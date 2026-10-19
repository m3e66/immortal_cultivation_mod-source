package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.network.HigherWeaponPayloads;
import com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class HigherWeaponCombat {
    private record Strike(long tick,boolean charged,boolean critical,boolean sprinting) {}
    private record Control(long until,boolean bind,boolean hooked,Vec3 anchor) {}
    private record TimedBuff(long until,double speed,double attackSpeed) {}
    private static final Map<ServerPlayer,Strike> STRIKES=new WeakHashMap<>();
    private static final Map<LivingEntity,Control> CONTROLS=new WeakHashMap<>();
    private static final Map<ServerPlayer,TimedBuff> BUFFS=new WeakHashMap<>();
    private static final List<Wave> WAVES=new ArrayList<>();
    private static final List<Starfall> STARFALLS=new ArrayList<>();
    private record SilkBind(ServerPlayer owner,LivingEntity target,ItemStack stack,
                            com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity plume,
                            WeaponSkillAttack attack,long started,long until) {}
    private static final Map<LivingEntity,SilkBind> SILK_BINDS=new WeakHashMap<>();
    private static LivingEntity bindingPulseTarget;
    private record PlacedFire(ServerPlayer owner, long expires) { }
    private static final Map<ServerLevel,Map<BlockPos,PlacedFire>> FIRES=new WeakHashMap<>();
    private static boolean skillDamage;
    private HigherWeaponCombat() {}

    private static boolean enemy(ServerPlayer player,LivingEntity target) {
        return target!=player && target.isAlive() && !target.isSpectator()
                && !com.example.immortal_cultivation_mod.entity.NpcProfile.areCombatAllies(player,target)
                && !(target instanceof net.minecraft.world.entity.player.Player other && !player.canHarmPlayer(other));
    }
    static boolean boss(LivingEntity target) {
        var id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        return target instanceof com.example.immortal_cultivation_mod.entity.VaultGuardianEntity guardian && guardian.beastRealm() >= 3
                || target instanceof EnderDragon || target instanceof WitherBoss
                || target.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","bosses")))
                || id.getNamespace().equals("immortal_cultivation_mod") && Set.of("blood_ape_demon_general",
                        "crimson_sky_flame_flood_dragon").contains(id.getPath());
    }
    private static boolean visible(ServerPlayer player,LivingEntity target) {
        return player.hasLineOfSight(target);
    }
    private static List<LivingEntity> nearby(ServerPlayer p,Vec3 center,double radius) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),
                e->enemy(p,e)&&e.distanceToSqr(center)<=radius*radius&&visible(p,e));
    }
    private static LivingEntity aimed(ServerPlayer p,double range) {
        Vec3 start=p.getEyePosition(),direction=p.getLookAngle();
        var block=p.level().clip(new ClipContext(start,start.add(direction.scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        double length=block.getLocation().distanceTo(start);
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().expandTowards(direction.scale(length)).inflate(1),
                e->enemy(p,e)&&e.getBoundingBox().inflate(.35).clip(start,start.add(direction.scale(length))).isPresent())
                .stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    private static boolean damage(ServerPlayer p,LivingEntity target,float amount) {
        return damage(p,target,p.damageSources().playerAttack(p),amount);
    }
    private static boolean damage(ServerPlayer p,LivingEntity target,net.minecraft.world.damagesource.DamageSource source,float amount) {
        if(!enemy(p,target)||!visible(p,target))return false;
        if(com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior.interceptAttackPath(p,p.getEyePosition(),target.getBoundingBox().getCenter(),amount))return false;
        boolean previous=skillDamage;skillDamage=true;
        try {return target.hurt(source,amount);}
        finally {skillDamage=previous;}
    }
    private static boolean damage(ServerPlayer p,LivingEntity target,WeaponSkillAttack attack,float percent) {
        var source=p.damageSources().playerAttack(p);
        return attack.withWeapon(p,source,()->damage(p,target,source,attack.damage(percent)));
    }
    private static void push(ServerPlayer p,LivingEntity target,double power,double up) {
        Vec3 direction=target.position().subtract(p.position()).normalize();
        target.push(direction.x*power,up,direction.z*power);target.hurtMarked=true;
    }
    private static boolean control(ServerPlayer owner,LivingEntity target,int ticks,boolean bind,boolean hooked) {
        if(bind && com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.resistForDuration(target,"higher_weapon_bind",owner,ticks))return false;
        boolean immobilize=bind&&!boss(target);
        CONTROLS.put(target,new Control(target.level().getGameTime()+ticks,immobilize,hooked,target.position()));
        modifier(target,Attributes.MOVEMENT_SPEED,"higher_weapon_control",immobilize?-1:hooked?-.1:-.35);
        target.stopUsingItem();
        if(target instanceof ServerPlayer p)CombatStateManager.interruptSpellPreparation(p);
        if(target instanceof Mob mob)mob.getNavigation().stop();
        return true;
    }
    public static void clearControl(LivingEntity target) {
        CONTROLS.remove(target);modifier(target,Attributes.MOVEMENT_SPEED,"higher_weapon_control",0);
        SilkBind silk=SILK_BINDS.remove(target);
        if(silk!=null)finishSilkBinding(silk);
    }
    private static void modifier(LivingEntity entity,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,String name,double value) {
        var instance=entity.getAttribute(attribute);
        if(instance==null)return;
        var id=HigherWeaponItem.id(name);
        var current=instance.getModifier(id);
        if(current!=null && current.amount()==value)return;
        instance.removeModifier(id);
        if(value!=0)instance.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private static void line(ServerLevel level,Vec3 from,Vec3 to,ParticleOptions particle,int count) {
        for(int i=0;i<count;i++) {
            Vec3 at=from.lerp(to,i/(double)Math.max(1,count-1));
            level.sendParticles(particle,at.x,at.y,at.z,1,0,0,0,0);
        }
    }
    public static void cast(ServerPlayer p,ItemStack stack,HigherWeaponItem item,boolean charged) {
        if(!WeaponBinding.checkUse(stack,p))return;
        var spec=item.spec;LivingEntity target;
        WeaponSkillAttack attack=WeaponSkillAttack.capture(stack);
        switch(spec) {
            case BLACK_IRON_JIAN -> {
                target=aimed(p,5);if(target==null||!spend(p,stack,item,attack,25,60))return;
                modifier(target,Attributes.ARMOR,"piercing_thrust",-.1);
                try {damage(p,target,attack,150);} finally {modifier(target,Attributes.ARMOR,"piercing_thrust",0);}
                HigherWeaponPayloads.send(p,Visual.THRUST,p.getEyePosition().add(0,-.25,0),target.getBoundingBox().getCenter(),.4);
            }
            case BLACK_IRON_GREATSWORD -> {
                if(!spend(p,stack,item,attack,45,120))return;
                for(var e:nearby(p,p.position(),3))if(e.position().subtract(p.position()).normalize().dot(p.getLookAngle())>.35) {
                    if(damage(p,e,attack,3200F/21F)){push(p,e,1.2,.25);control(p,e,12,true,false);}
                }
                HigherWeaponPayloads.send(p,Visual.CLEAVE,p.position(),p.position().add(p.getLookAngle()),3);
            }
            case MOON_CRESCENT_HALBERD -> {
                if(!spend(p,stack,item,attack,50,100))return;
                if(p.getDeltaMovement().dot(p.getLookAngle().multiply(1,0,1))>.015){Vec3 forward=p.getLookAngle().multiply(1,0,1).normalize();p.push(forward.x*.8,.05,forward.z*.8);p.hurtMarked=true;}
                for(var e:nearby(p,p.position(),4))if(damage(p,e,attack,2200F/19F))push(p,e,.9,.2);
                HigherWeaponPayloads.send(p,Visual.SWEEP,p.position(),p.position().add(p.getLookAngle()),4);
            }
            case SPIRIT_SILK_WHIP -> {
                target=aimed(p,8);if(target==null||!spend(p,stack,item,attack,30,120))return;
                if(!com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior.interceptAttackPath(p,p.getEyePosition(),target.getBoundingBox().getCenter(),attack.damage(20)))startSilkBinding(p,stack,target,attack);
                HigherWeaponPayloads.send(p,Visual.SILK,p.getEyePosition().add(0,-.4,0),target.getBoundingBox().getCenter(),.3);
            }
            case SOUL_HOOK -> {
                if(!spend(p,stack,item,attack,35,100))return;
                launch(p,attack,12,80,1,ParticleTypes.SOUL,.25,WaveKind.HOOK);
            }
            case WIND_FEATHER_FAN -> {
                if(p.isSprinting()) {
                    if(!spend(p,stack,item,attack,45,140))return;
                    for(var e:nearby(p,p.position(),5)) { damage(p,e,attack,50);push(p,e,1.8,.35); }
                    HigherWeaponPayloads.send(p,Visual.GUST,p.position(),p.position().add(p.getLookAngle()),5);
                } else {
                    if(!spend(p,stack,item,attack,20,50))return;
                    launch(p,attack,14,1500F/11F,2,ParticleTypes.CLOUD,.7,WaveKind.WIND);
                }
            }
            case THUNDERWOOD_SWORD -> {
                target=aimed(p,15);if(target==null)return;
                boolean rain=p.serverLevel().isRainingAt(p.blockPosition());
                if(!spend(p,stack,item,attack,rain?45:55,120))return;
                damage(p,target,attack,(rain?3800F:2800F)/17F);
                HigherWeaponPayloads.send(p,Visual.THUNDER_SEAL,p.position().add(0,.025,0),p.position().add(p.getLookAngle()),1.6);
                p.serverLevel().playSound(null,target.blockPosition(),net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,net.minecraft.sounds.SoundSource.PLAYERS,.5F,1.2F);
            }
            case FLAME_PATTERN_DAO -> {
                if(!spend(p,stack,item,attack,charged?70:40,80))return;
                launch(p,attack,charged?13:9,(charged?3500F:2400F)/19F,8,ParticleTypes.FLAME,charged?1.5:.9,charged?WaveKind.FLAME_CHARGED:WaveKind.FLAME);
            }
            case EARTHBREAKER_HAMMER -> {
                var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(5)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                if(hit.getType()!=HitResult.Type.BLOCK||p.getLookAngle().y>=-.1||!spend(p,stack,item,attack,70,160))return;
                for(var e:nearby(p,hit.getLocation(),5))if(damage(p,e,attack,3500F/27F)){push(p,e,.25,.85);control(p,e,10,true,false);}
                com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.send(p,hit.getLocation(),false);
                var state=p.level().getBlockState(((net.minecraft.world.phys.BlockHitResult)hit).getBlockPos());
                p.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),hit.getLocation().x,hit.getLocation().y+.2,hit.getLocation().z,45,2,.15,2,.2);
                p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.PLAYERS,.7F,.7F);
                // No terrain mutation: the hammer only damages and displaces entities.
            }
            case SEVEN_STAR_SWORD -> {
                CompoundTag tag=HigherWeaponItem.data(stack);int stars=SevenStarPattern.charges(stack);
                if(p.isShiftKeyDown()) {
                    if(!SevenStarPattern.requireCharges(p,stack,7))return;
                    target=aimed(p,20);
                    if(target==null) {
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.seven_star.need_target"),true);
                        return;
                    }
                    if(!spend(p,stack,item,attack,250,400))return;
                    tag=HigherWeaponItem.data(stack);tag.putInt("Stars",0);HigherWeaponItem.save(stack,tag);
                    STARFALLS.add(new Starfall(p,p.serverLevel(),target.position(),p.serverLevel().getGameTime(),attack));
                    com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.send(p,target.position(),true);
                } else {
                    if(!SevenStarPattern.requireCharges(p,stack,3)||!spend(p,stack,item,attack,80,80))return;
                    tag=HigherWeaponItem.data(stack);tag.putInt("Stars",stars-3);HigherWeaponItem.save(stack,tag);
                    launch(p,attack,20,150,16,ParticleTypes.END_ROD,.45,WaveKind.STAR);
                }
            }
        }
        if(spec==HigherWeaponSpec.SEVEN_STAR_SWORD)SevenStarPattern.showCharges(p,stack);
        HigherWeaponPayloads.animate(p,spec,charged);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        if(item instanceof HigherWeaponGeoItem geo)geo.triggerAnim(p,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,p.serverLevel()),"main","attack");
    }
    private static boolean spend(ServerPlayer player,ItemStack stack,HigherWeaponItem item,WeaponSkillAttack attack,int qi,int cooldown) {
        if(!item.spend(player,stack,qi,cooldown))return false;
        attack.consumeInjectedQi();
        return true;
    }
    private static void startSilkBinding(ServerPlayer owner,ItemStack stack,LivingEntity target,WeaponSkillAttack attack) {
        for(var previous:new ArrayList<>(SILK_BINDS.values()))
            if(previous.owner==owner||previous.target==target)clearControl(previous.target);
        if(!control(owner,target,30,true,false))return;
        var plume=com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity.create(owner.serverLevel(),owner,target,31);
        if(plume!=null)owner.serverLevel().addFreshEntity(plume);
        long now=owner.level().getGameTime();
        SILK_BINDS.put(target,new SilkBind(owner,target,stack,plume,attack,now,now+30));
        CompoundTag tag=HigherWeaponItem.data(stack);
        tag.putInt("SilkBindingTarget",target.getId());tag.putLong("SilkBindingUntil",now+30);
        HigherWeaponItem.save(stack,tag);
        bindingPose(owner,true);
    }
    private static void finishSilkBinding(SilkBind silk) {
        if(silk.plume!=null)silk.plume.discard();
        CompoundTag tag=HigherWeaponItem.data(silk.stack);
        tag.remove("SilkBindingTarget");tag.remove("SilkBindingUntil");HigherWeaponItem.save(silk.stack,tag);
        bindingPose(silk.owner,false);
    }
    private static void bindingPose(ServerPlayer player,boolean active) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new com.example.immortal_cultivation_mod.network.CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(),active?com.example.immortal_cultivation_mod.network.CombatPayloads.FUCHEN_BIND_ANIMATION_START
                                :com.example.immortal_cultivation_mod.network.CombatPayloads.FUCHEN_BIND_ANIMATION_STOP));
    }
    private static void tickSilkBindings() {
        for(var silk:new ArrayList<>(SILK_BINDS.values())) {
            long now=silk.owner.level().getGameTime();
            if(!silk.owner.isAlive()||silk.owner.isRemoved()||!enemy(silk.owner,silk.target)||silk.target.isRemoved()||silk.target.level()!=silk.owner.level()
                    ||silk.owner.getMainHandItem()!=silk.stack||now>=silk.until
                    ||silk.owner.distanceToSqr(silk.target)>100||!visible(silk.owner,silk.target)
                    ||com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.hasProtection(silk.target)
                    ||!CONTROLS.containsKey(silk.target)) { clearControl(silk.target);continue; }
            if(now-silk.started==20) {
                LivingEntity previous=bindingPulseTarget;bindingPulseTarget=silk.target;
                try { damage(silk.owner,silk.target,silk.attack,20); }
                finally { bindingPulseTarget=previous; }
            }
        }
    }
    private static void ring(ServerPlayer p,double radius,ParticleOptions particle) {
        for(int i=0;i<24;i++) {
            double angle=i*Math.PI/12;
            Vec3 end=p.position().add(Math.cos(angle)*radius,.9,Math.sin(angle)*radius);
            var hit=p.level().clip(new ClipContext(p.position().add(0,.9,0),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            Vec3 at=hit.getLocation();p.serverLevel().sendParticles(particle,at.x,at.y,at.z,1,0,0,0,0);
        }
    }
    private enum WaveKind { WIND,HOOK,FLAME,FLAME_CHARGED,STAR }
    private static final class Wave {
        final ServerPlayer owner;final ServerLevel level;final Vec3 direction;final float percent;final WeaponSkillAttack attack;
        final ParticleOptions particle;final double width;final WaveKind kind;final Set<UUID> hit=new HashSet<>();
        Vec3 position;double remaining;int pierces;
        Wave(ServerPlayer p,WeaponSkillAttack attack,double range,float percent,int pierces,ParticleOptions particle,double width,WaveKind kind) {
            owner=p;level=p.serverLevel();position=p.getEyePosition().add(0,-.2,0);direction=p.getLookAngle();
            remaining=range;this.attack=attack;this.percent=percent;this.pierces=pierces;this.particle=particle;this.width=width;this.kind=kind;
        }
    }
    private record Starfall(ServerPlayer owner,ServerLevel level,Vec3 center,long start,WeaponSkillAttack attack) {}
    private static void launch(ServerPlayer p,WeaponSkillAttack attack,double range,float percent,int pierces,ParticleOptions particle,double width,WaveKind kind) {
        if(WAVES.size()<256)WAVES.add(new Wave(p,attack,range,percent,pierces,particle,width,kind));
    }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST) public static void attack(AttackEntityEvent event) {
        if(event.getEntity() instanceof ServerPlayer p && p.getMainHandItem().getItem() instanceof HigherWeaponItem) {
            STRIKES.put(p,new Strike(p.level().getGameTime(),p.getAttackStrengthScale(.5F)>.9F,
                    p.fallDistance>0&&!p.onGround()&&!p.isInWater()&&!p.isSprinting(),p.isSprinting()));
        }
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event) {
        if(!(bindingPulseTarget==event.getEntity()&&WeaponSkillAttack.isCurrentSource(event.getSource())) && CONTROLS.containsKey(event.getEntity()) && CONTROLS.get(event.getEntity()).bind)clearControl(event.getEntity());
        if(skillDamage || !(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity()!=p
                || !(p.getMainHandItem().getItem() instanceof HigherWeaponItem item))return;
        Strike strike=STRIKES.get(p);if(strike==null||strike.tick!=p.level().getGameTime())return;
        float amount=event.getAmount();var target=event.getEntity();
        switch(item.spec) {
            case BLACK_IRON_GREATSWORD -> {if(strike.sprinting){amount+=4;p.causeFoodExhaustion(.6F);}}
            case SPIRIT_SILK_WHIP -> {if(p.distanceToSqr(target)>=9)amount*=14F/11F;}
            case EARTHBREAKER_HAMMER -> amount*=1+Math.min(.2F,target.getArmorValue()*.01F);
            case FLAME_PATTERN_DAO -> {if(target.isOnFire())amount+=3;if(strike.charged)target.igniteForSeconds(3);}
            case SEVEN_STAR_SWORD -> amount*=1+.02F*SevenStarPattern.charges(p.getMainHandItem());
            default -> {}
        }
        event.setAmount(amount);
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        if(skillDamage||event.getNewDamage()<=0||!(event.getSource().getEntity() instanceof ServerPlayer p)
                ||event.getSource().getDirectEntity()!=p||!(p.getMainHandItem().getItem() instanceof HigherWeaponItem item))return;
        Strike strike=STRIKES.remove(p);if(strike==null||strike.tick!=p.level().getGameTime())return;
        ItemStack stack=p.getMainHandItem();CompoundTag tag=HigherWeaponItem.data(stack);LivingEntity target=event.getEntity();long now=p.level().getGameTime();
        switch(item.spec) {
            case BLACK_IRON_JIAN -> {
                int hits=now-tag.getLong("FlowStart")<=80?tag.getInt("FlowHits")+1:1;
                if(hits==1)tag.putLong("FlowStart",now);
                tag.putInt("FlowHits",hits>=3?0:hits);
                if(hits>=3)BUFFS.put(p,new TimedBuff(now+60,.15,.1));
            }
            case SOUL_HOOK -> {if(strike.critical)control(p,target,80,false,true);}
            case MOON_CRESCENT_HALBERD -> {
                if(strike.charged)nearby(p,target.position(),2).stream().filter(e->e!=target).limit(3).forEach(e->damage(p,e,item.spec.damage*.7F));
            }
            case WIND_FEATHER_FAN -> {
                if(strike.charged)nearby(p,target.position(),1.5).stream().filter(e->e!=target).limit(2).forEach(e->damage(p,e,5.5F));
            }
            case THUNDERWOOD_SWORD -> {
                int charges=tag.getInt("ThunderCharges");
                if(charges>=5) {
                    tag.putInt("ThunderCharges",0);
                    target.invulnerableTime=0;damage(p,target,12);
                    nearby(p,target.position(),4).stream().filter(e->e!=target).limit(2).forEach(e->{damage(p,e,12);line(p.serverLevel(),target.getBoundingBox().getCenter(),e.getBoundingBox().getCenter(),ParticleTypes.ELECTRIC_SPARK,10);});
                } else tag.putInt("ThunderCharges",charges+1);
            }
            case SEVEN_STAR_SWORD -> {
                int before=SevenStarPattern.charges(stack);
                tag.putInt("Stars",Math.min(SevenStarPattern.COUNT,before+1));
                if(before==6) {
                    p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.45F,1.4F);
                    p.serverLevel().sendParticles(ParticleTypes.END_ROD,p.getX(),p.getY()+1.2,p.getZ(),7,.3,.3,.3,.01);
                }
            }
            default -> {}
        }
        HigherWeaponItem.save(stack,tag);
        if(item.spec==HigherWeaponSpec.SEVEN_STAR_SWORD) {
            SevenStarPattern.showCharges(p,stack);
            p.serverLevel().sendParticles(ParticleTypes.END_ROD,target.getX(),target.getY()+target.getBbHeight()*.6,target.getZ(),2,.15,.15,.15,.01);
        }
        if(item instanceof HigherWeaponGeoItem geo)geo.triggerAnim(p,software.bernie.geckolib.animatable.GeoItem.getOrAssignId(stack,p.serverLevel()),"main","attack");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for(var p:event.getServer().getPlayerList().getPlayers()) {
            var buff=BUFFS.get(p);long now=p.level().getGameTime();
            if(buff!=null&&buff.until<=now){BUFFS.remove(p);buff=null;}
            double stars=p.getMainHandItem().getItem() instanceof HigherWeaponItem i&&i.spec==HigherWeaponSpec.SEVEN_STAR_SWORD?SevenStarPattern.charges(p.getMainHandItem())*.01:0;
            modifier(p,Attributes.MOVEMENT_SPEED,"flowing_blade",buff==null?0:buff.speed);
            modifier(p,Attributes.ATTACK_SPEED,"flowing_blade",buff==null?0:buff.attackSpeed);
            modifier(p,Attributes.MOVEMENT_SPEED,"seven_stars",stars);
        }
        var controls=CONTROLS.entrySet().iterator();
        while(controls.hasNext()) {
            var entry=controls.next();LivingEntity e=entry.getKey();Control c=entry.getValue();
            if(!e.isAlive()||e.level().getGameTime()>=c.until || c.bind && com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.hasProtection(e)){modifier(e,Attributes.MOVEMENT_SPEED,"higher_weapon_control",0);controls.remove();continue;}
            if(c.bind){
                e.setDeltaMovement(0,e.getDeltaMovement().y,0);
                if(e instanceof Mob m)m.getNavigation().stop();
                if(e instanceof ServerPlayer && e.position().subtract(c.anchor).horizontalDistanceSqr()>.01)
                    e.teleportTo(c.anchor.x,e.getY(),c.anchor.z);
            }
            if(c.hooked){Vec3 velocity=e.getDeltaMovement();double horizontal=velocity.horizontalDistance();if(horizontal>.35)e.setDeltaMovement(velocity.x*.35/horizontal,velocity.y,velocity.z*.35/horizontal);}
            if(e.level() instanceof ServerLevel level && e.tickCount%5==0)level.sendParticles(c.hooked?ParticleTypes.SOUL:ParticleTypes.END_ROD,e.getX(),e.getY()+.8,e.getZ(),2,.3,.3,.3,.01);
        }
        tickSilkBindings();
        var waves=WAVES.iterator();
        while(waves.hasNext()) {
            Wave w=waves.next();
            if(!w.owner.isAlive()||w.owner.level()!=w.level){waves.remove();continue;}
            Vec3 end=w.position.add(w.direction.scale(Math.min(1.5,w.remaining)));
            var block=w.level.clip(new ClipContext(w.position,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,w.owner));
            end=block.getLocation();final Vec3 endPoint=end;
            var targets=w.level.getEntitiesOfClass(LivingEntity.class,new AABB(w.position,end).inflate(w.width),
                    e->enemy(w.owner,e)&&!w.hit.contains(e.getUUID())&&e.getBoundingBox().inflate(w.width).clip(w.position,endPoint).isPresent());
            targets.sort(Comparator.comparingDouble(e->e.distanceToSqr(w.position)));
            for(var e:targets) {
                if(w.pierces<=0)break;
                w.hit.add(e.getUUID());w.pierces--;
                if(damage(w.owner,e,w.attack,w.percent)) {
                    if(w.kind==WaveKind.HOOK) {
                        if(boss(e))control(w.owner,e,40,false,false);
                        else if(e.getBbHeight()>2.5||e.getBbWidth()>1.5){Vec3 v=e.position().subtract(w.owner.position()).normalize().scale(1.2);w.owner.setDeltaMovement(v);w.owner.hurtMarked=true;}
                        else{e.setDeltaMovement(w.owner.position().subtract(e.position()).normalize().scale(1.4).add(0,.15,0));e.hurtMarked=true;}
                    }
                    if(w.kind==WaveKind.FLAME||w.kind==WaveKind.FLAME_CHARGED)e.igniteForSeconds(3);
                }
            }
            HigherWeaponPayloads.send(w.owner,Visual.valueOf(w.kind.name()),w.position,end,w.width);
            if(w.kind==WaveKind.FLAME_CHARGED
                    && BlockBreakingSettings.enabled(w.owner,
                    BlockBreakingSettings.Category.WEAPONS)) {
                BlockPos ground=BlockPos.containing(end).below();
                for(int i=0;i<3&&w.level.getBlockState(ground).isAir();i++)ground=ground.below();
                BlockPos fire=ground.above();
                if(w.level.getBlockState(fire).isAir()&&w.level.getBlockState(ground).isSolidRender(w.level,ground)
                        && w.level.mayInteract(w.owner, fire)
                        && w.owner.mayUseItemAt(fire, net.minecraft.core.Direction.UP, w.owner.getMainHandItem())
                        &&!com.example.immortal_cultivation_mod.event.SmallSectProtection.isLocked(w.level,fire)
                        &&!com.example.immortal_cultivation_mod.event.DeadRuinProtection.isLocked(w.level,fire)) {
                    w.level.setBlock(fire,Blocks.FIRE.defaultBlockState(),3);
                    FIRES.computeIfAbsent(w.level,k->new HashMap<>()).put(fire.immutable(),new PlacedFire(w.owner,w.level.getGameTime()+30));
                }
            }
            w.remaining-=1.5;w.position=end;
            if(w.remaining<=0||w.pierces<=0||block.getType()==HitResult.Type.BLOCK)waves.remove();
        }
        var falls=STARFALLS.iterator();
        while(falls.hasNext()) {
            Starfall f=falls.next();if(!f.owner.isAlive()||f.owner.isRemoved()||f.owner.level()!=f.level){falls.remove();continue;}
            long age=f.owner.level().getGameTime()-f.start;
            if(age>=SevenStarPattern.DESCENT && age<SevenStarPattern.FINISH && (age-SevenStarPattern.DESCENT)%SevenStarPattern.INTERVAL==0) {
                int index=(int)(age-SevenStarPattern.DESCENT)/SevenStarPattern.INTERVAL;
                Vec3 at=SevenStarPattern.point(f.center,index);
                f.level.sendParticles(ParticleTypes.END_ROD,at.x,at.y+.15,at.z,5,.12,.15,.12,.025);
                f.level.playSound(null,at.x,at.y,at.z,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT,net.minecraft.sounds.SoundSource.PLAYERS,.35F,1.0F+index*.08F);
                for(var e:nearby(f.owner,at,2)){e.invulnerableTime=0;damage(f.owner,e,f.attack,2200F/30F);}
            }
            if(age>=SevenStarPattern.FINISH){for(var e:nearby(f.owner,f.center,5)){e.invulnerableTime=0;damage(f.owner,e,f.attack,5500F/30F);}f.owner.serverLevel().sendParticles(ParticleTypes.EXPLOSION,f.center.x,f.center.y+1,f.center.z,1,0,0,0,0);falls.remove();}
        }
        for(var entry:FIRES.entrySet())entry.getValue().entrySet().removeIf(f->{
            if(entry.getKey().getGameTime()<f.getValue().expires()
                    && BlockBreakingSettings.enabled(f.getValue().owner(),
                    BlockBreakingSettings.Category.WEAPONS))return false;
            if(entry.getKey().getBlockState(f.getKey()).is(Blocks.FIRE))entry.getKey().setBlock(f.getKey(),Blocks.AIR.defaultBlockState(),3);
            return true;
        });
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        // Clear saved stack endpoint tags before the server saves inventories.
        for(var silk:new ArrayList<>(SILK_BINDS.values()))clearControl(silk.target);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {STRIKES.clear();CONTROLS.clear();BUFFS.clear();WAVES.clear();STARFALLS.clear();FIRES.clear();SILK_BINDS.clear();}

    @SubscribeEvent public static void knockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if(skillDamage)return;
        var attacker=event.getEntity().getLastHurtByMob();
        if(attacker instanceof ServerPlayer player && player.isSprinting()
                && player.getMainHandItem().is(ModItems.BLACK_IRON_GREATSWORD.get()))event.setStrength(event.getStrength()*1.3F);
    }
}
