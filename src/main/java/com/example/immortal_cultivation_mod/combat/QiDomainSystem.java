package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import com.example.immortal_cultivation_mod.physics.DomainVolume;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server authority for release, upkeep, method domains, timed effects and contact hazards. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class QiDomainSystem {
    public static final double DOMAIN_RADIUS=20;
    public static final int GREAT_MASTERY=500,MAX_ACTORS=64,MOTES=12;
    public static final int MOTE=1,FIST=2,WEAPON=3,BODY=4;
    public static final int NPC_BLAST_TICKS=200,NPC_BLAST_COOLDOWN_TICKS=200;
    public static final int NPC_BLAST_OPENING_TICKS=100;
    public static final String NPC_COMBAT_READY_AT="ImmortalCultivationNpcQiCombatReadyAt";
    private static final String PERCENT="ImmortalCultivationQiReleasePercent";
    private static final String NPC_READY_AT="ImmortalCultivationNpcQiBlastReadyAt";
    private static final String DOMAIN_READY_AT="ImmortalCultivationDomainReadyAt";
    private static final ResourceLocation MOBILITY=ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID,"qi_blast_mobility");
    private static final Map<LivingEntity,Aura> ACTIVE=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> INPUT_TICK=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> TOGGLE_TICK=new WeakHashMap<>();
    private QiDomainSystem() {}
    public static final class Aura {
        public final LivingEntity owner;
        public boolean blasting;
        public int percent=100,attackMode,opponent=-1;
        public float power=1;
        public long until,attackUntil,npcBlastUntil,lastSend=Long.MIN_VALUE,lastUpkeep=Long.MIN_VALUE;
        long lastMethodPulse=Long.MIN_VALUE;
        String pulseMethod="";
        final Map<UUID,Integer> domainExposure=new HashMap<>();
        public String method="";
        final List<DomainVolume.Cut> contacts=new ArrayList<>();
        public final long[] moteCooldown=new long[MOTES];
        public final Vec3[] motePrevious=new Vec3[MOTES];
        private final Map<UUID,Long> hitUntil=new HashMap<>();
        private Aura(LivingEntity owner){this.owner=owner;}
        public boolean domain(){return blasting&&percent>=200;}
    }
    private static Aura state(LivingEntity owner) {
        var a=ACTIVE.get(owner);
        if(a==null&&ACTIVE.size()<MAX_ACTORS){a=new Aura(owner);ACTIVE.put(owner,a);}
        return a;
    }
    public static Aura view(LivingEntity owner){return ACTIVE.get(owner);}
    /** Forget accumulated exposure, including state held by other active domain owners. */
    public static void clearTargetDebuffs(LivingEntity target) {
        if(target.level().isClientSide)return;
        ColdTideFreeze.clear(target);TideWaterStacks.clear(target);BloodLilyBloom.clear(target);
        for(var aura:ACTIVE.values())aura.domainExposure.remove(target.getUUID());
    }
    public static DomainVolume volume(LivingEntity owner) {
        var aura=view(owner);return new DomainVolume(owner.position(),aura==null?List.of():aura.contacts);
    }
    public static boolean contains(LivingEntity owner,Entity target) {return volume(owner).intersects(target.getBoundingBox());}
    public static boolean contains(LivingEntity owner,Vec3 point) {return volume(owner).contains(point);}
    public static int percent(LivingEntity owner){var a=ACTIVE.get(owner);return a==null?100:a.percent;}
    public static boolean blasting(LivingEntity owner){var a=ACTIVE.get(owner);return a!=null&&a.blasting;}
    public static int stage(LivingEntity owner) {
        return CultivationLevels.getStageIndex(level(owner));
    }
    public static String level(LivingEntity owner) {
        if(owner instanceof ServerPlayer p)return ModAttachments.getData(p).cultivationLevel();
        if(owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc npc)return NpcProfile.realm(mob,npc.npcArchetype());
        return CultivationLevels.REALM_MORTAL;
    }
    public static String method(LivingEntity owner) {
        if(owner instanceof ServerPlayer p)return ModAttachments.getData(p).activeCultivationMethod();
        if(owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc npc)return NpcProfile.methodId(mob,npc.npcArchetype());
        return "";
    }
    public static int mastery(LivingEntity owner) {
        if(owner instanceof ServerPlayer p)return ModAttachments.getData(p).methodProficiency(method(p));
        if(owner instanceof PathfinderMob mob) {
            var profile=mob.getPersistentData().getCompound("ImmortalCultivationNpc");
            return Math.max(profile.getInt("MethodProficiency"),profile.getCompound("MethodProficiencies").getInt(method(owner)));
        }
        return 0;
    }
    public static boolean eligible(LivingEntity owner) {
        return owner.isAlive()&&!owner.isRemoved()&&!owner.isSpectator()&&!ColdTideFreeze.isFrozen(owner)
                && CultivationLevels.getRealmIndex(level(owner))>=3;
    }
    public static void toggle(ServerPlayer player) {
        long tick=player.level().getGameTime();if(TOGGLE_TICK.getOrDefault(player,Long.MIN_VALUE)==tick)return;
        TOGGLE_TICK.put(player,tick);
        if(blasting(player)){stop(player);message(player,"off");return;}
        if(!eligible(player)){message(player,"realm");return;}
        if(!CombatStateManager.isInCombat(player)){message(player,"combat");return;}
        int desired=Math.clamp(player.getPersistentData().getInt(PERCENT),100,200);
        if(desired==200&&tick<player.getPersistentData().getLong(DOMAIN_READY_AT))desired=100;
        if(!ServerEvents.canAffordQiOrBlood(ModAttachments.getData(player),upkeep(player,desired))){message(player,"empty");return;}
        var a=state(player);if(a==null)return;
        a.blasting=true;a.percent=desired;a.method=method(player);
        a.until=player.level().getGameTime()+20;send(a,true);activation(a);message(player,"on");
    }
    public static void adjust(ServerPlayer player,int steps) {
        var a=ACTIVE.get(player);
        if(a==null||!a.blasting||!eligible(player)||!CombatStateManager.isInCombat(player)||!player.isShiftKeyDown())return;
        long now=player.level().getGameTime();if(INPUT_TICK.getOrDefault(player,Long.MIN_VALUE)==now)return;
        INPUT_TICK.put(player,now);
        int desired=Math.clamp(a.percent+Math.clamp(steps,-4,4)*10,100,200);
        if(desired==200&&now<player.getPersistentData().getLong(DOMAIN_READY_AT)){message(player,"recovering");return;}
        a.percent=desired;
        player.getPersistentData().putInt(PERCENT,a.percent);
        send(a,true);
        player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.qi_release.intensity",a.percent),true);
    }
    public static void qiUsed(LivingEntity player,String method,int amount) {
        if(amount<=0||player.level().isClientSide||player instanceof ServerPlayer p&&!MethodQiParticleEffects.showQiUseParticles(p))return;
        var a=state(player);if(a==null)return;
        a.method=method;a.until=player.level().getGameTime()+18;send(a,false);
    }
    public static void attack(ServerPlayer player) {
        if(!CombatStateManager.isInCombat(player))return;
        var data=ModAttachments.getData(player);
        if(data.qi()<=0&&data.blood()<=0)return;
        var a=state(player);if(a==null)return;
        a.method=method(player);a.attackMode=QiInfusedWeaponBridge.isWeapon(player)?WEAPON:FIST;
        a.attackUntil=player.level().getGameTime()+14;a.until=a.attackUntil;send(a,true);
    }
    // Avoid treating bracers (a fist weapon) as a long held weapon.
    private static final class QiInfusedWeaponBridge {
        static boolean isWeapon(ServerPlayer p){return !p.getMainHandItem().isEmpty()
                && !com.example.immortal_cultivation_mod.item.QiInfusedWeapon.isFistWeapon(p.getMainHandItem());}
    }
    public static void stop(LivingEntity owner) {
        DomainClashes.remove(owner);
        mobility(owner,false);
        var a=ACTIVE.remove(owner);if(a==null)return;
        if(a.blasting&&owner instanceof CultivationNpc)
            owner.getPersistentData().putLong(NPC_READY_AT,owner.level().getGameTime()+NPC_BLAST_COOLDOWN_TICKS);
        a.blasting=false;a.until=0;send(a,true);
    }
    static void breakDomain(Aura aura) {
        if(!aura.domain())return;
        if(aura.owner instanceof TrainingDummyEntity dummy)dummy.domainRequested=false;
        aura.percent=100;aura.power=1;aura.opponent=-1;aura.contacts.clear();aura.domainExposure.clear();
        aura.owner.getPersistentData().putLong(DOMAIN_READY_AT,aura.owner.level().getGameTime()+DomainClashes.RECOVERY_TICKS);
        if(aura.owner instanceof ServerPlayer p){p.getPersistentData().putInt(PERCENT,100);message(p,"shattered");}
        send(aura,true);
    }
    private static void activation(Aura a) {
        if(!(a.owner.level() instanceof ServerLevel level))return;
        var owner=a.owner;
        mobility(owner,true);
        // Activation is visual only; launch requires a confirmed punch.
        PacketDistributor.sendToPlayersNear(level,null,owner.getX(),owner.getY(),owner.getZ(),96,
                new QiAuraPayloads.Activation(level.dimension().location(),owner.getX(),owner.getY(),owner.getZ(),
                        MethodDomainStyle.of(a.method).ordinal(),a.percent,owner.getId(),owner.getUUID()));
    }
    /** Separate transient modifiers leave potion strength, duration and particles untouched. */
    private static void mobility(LivingEntity owner,boolean enabled) {
        var speed=owner.getAttribute(Attributes.MOVEMENT_SPEED);
        var jump=owner.getAttribute(Attributes.JUMP_STRENGTH);
        var fall=owner.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if(enabled) {
            if(speed!=null&&!speed.hasModifier(MOBILITY))speed.addTransientModifier(new AttributeModifier(
                    MOBILITY,.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if(jump!=null&&!jump.hasModifier(MOBILITY))jump.addTransientModifier(new AttributeModifier(
                    MOBILITY,.1,AttributeModifier.Operation.ADD_VALUE));
            // Jump Boost I also grants one additional block of safe fall distance.
            if(fall!=null&&!fall.hasModifier(MOBILITY))fall.addTransientModifier(new AttributeModifier(
                    MOBILITY,1,AttributeModifier.Operation.ADD_VALUE));
        }else {
            if(speed!=null)speed.removeModifier(MOBILITY);
            if(jump!=null)jump.removeModifier(MOBILITY);
            if(fall!=null)fall.removeModifier(MOBILITY);
        }
    }
    private static void message(ServerPlayer p,String key){p.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.qi_release."+key),true);}
    public static int upkeep(LivingEntity owner,int percent) {
        int max=owner instanceof ServerPlayer p?ModAttachments.getData(p).maxQi():
                owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc npc?NpcProfile.maxQi(mob,npc.npcArchetype()):100;
        return Math.max(1,(int)Math.ceil(max*.0025*Math.pow(Math.clamp(percent,100,200)/100.0,2)));
    }
    /** Same stage: stalemate. One/two stages below: partial suppression; a full realm below: suppressed. */
    public static float pressure(int ownStage,int rivalStage) {
        int gap=rivalStage-ownStage;return gap<=0?1:gap==1?.72F:gap==2?.42F:0;
    }
    public static boolean canAffect(LivingEntity owner,Entity target) {
        if(target==owner||!target.isAlive()||target.isSpectator()||target.isAlliedTo(owner)||owner.isAlliedTo(target))return false;
        if(target instanceof net.minecraft.world.entity.TamableAnimal pet&&owner.getUUID().equals(pet.getOwnerUUID()))return false;
        if(target instanceof Projectile projectile&&(projectile.getOwner()==owner||projectile.getOwner()!=null&&projectile.getOwner().isAlliedTo(owner)))return false;
        if(target instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()))return false;
        return !(owner instanceof ServerPlayer p&&target instanceof net.minecraft.world.entity.player.Player other)||p.canHarmPlayer(other);
    }
    /** Live passive targeting is separate from eligibility for an already confirmed attack. */
    public static boolean canDomainAffect(LivingEntity owner,Entity target) {
        return canAffect(owner,target)&&(!(owner instanceof CultivationNpc)||!(target instanceof LivingEntity living)
                ||NpcProfile.isCombatEnemy(owner,living));
    }
    @SubscribeEvent public static void npcTick(EntityTickEvent.Post event) {
        if(!(event.getEntity() instanceof PathfinderMob mob)||!(mob instanceof CultivationNpc npc)||mob.level().isClientSide)return;
        if(mob instanceof TrainingDummyEntity dummy) {
            var aura=ACTIVE.get(dummy);
            if(!dummy.domainRequested||!eligible(dummy)){if(aura!=null&&aura.blasting)stop(dummy);return;}
            long now=dummy.level().getGameTime();
            if(now<dummy.getPersistentData().getLong(DOMAIN_READY_AT))return;
            if(aura==null||!aura.blasting){aura=state(dummy);if(aura==null)return;aura.blasting=true;aura.percent=200;aura.method=method(dummy);aura.npcBlastUntil=Long.MAX_VALUE;send(aura,true);activation(aura);}
            return;
        }
        long now=mob.level().getGameTime();var a=ACTIVE.get(mob);
        if(!npcCombatTarget(mob)) {
            mob.getPersistentData().remove(NPC_COMBAT_READY_AT);
            if(a!=null&&a.blasting)stop(mob);
            return;
        }
        if(!mob.getPersistentData().contains(NPC_COMBAT_READY_AT)) {
            mob.getPersistentData().putLong(NPC_COMBAT_READY_AT,now+NPC_BLAST_OPENING_TICKS);
        }
        if(a!=null&&a.blasting&&(now>=a.npcBlastUntil||!npcCombatTarget(mob)
                ||NpcProfile.qi(mob,npc.npcArchetype())<upkeep(mob,a.percent))) {
            stop(mob);return;
        }
        // Give the opening exchange to spells instead of automatically releasing Qi on target acquisition.
        if(mob.tickCount<5||now<mob.getPersistentData().getLong(NPC_COMBAT_READY_AT))return;
        if(a==null||!a.blasting) {
            if(now<mob.getPersistentData().getLong(NPC_READY_AT)||mob.tickCount%20!=0)return;
            int percent=mastery(mob)>=GREAT_MASTERY&&now>=mob.getPersistentData().getLong(DOMAIN_READY_AT)?200:100;
            long required=(long)npcSpellReserve(mob,npc)+(long)upkeep(mob,percent)*(NPC_BLAST_TICKS/20);
            if(NpcProfile.qi(mob,npc.npcArchetype())<required)return;
            a=state(mob);if(a==null)return;
            a.blasting=true;a.percent=percent;
            a.method=method(mob);a.npcBlastUntil=now+NPC_BLAST_TICKS;
            // Reserve the cooldown immediately as well, so unloading/reloading cannot reset it.
            mob.getPersistentData().putLong(NPC_READY_AT,a.npcBlastUntil+NPC_BLAST_COOLDOWN_TICKS);
            send(a,true);activation(a);
        }
        if(mob.tickCount%20!=0)return;
        var p=mob.getPersistentData().getCompound("ImmortalCultivationNpc");
        var uses=p.getCompound("MethodProficiencies");uses.putInt(method(mob),Math.min(10000,uses.getInt(method(mob))+1));p.put("MethodProficiencies",uses);
    }
    private static boolean npcCombatTarget(PathfinderMob mob) {
        var target=mob.getTarget();
        return eligible(mob)&&target!=null&&target.level()==mob.level()&&canAffect(mob,target)
                &&mob.distanceToSqr(target)<=32*32&&NpcProfile.isCombatEnemy(mob,target);
    }
    /** Protect a useful spell cast as well as a general reserve; only aura upkeep observes this floor. */
    public static int npcSpellReserve(PathfinderMob mob,CultivationNpc npc) {
        int max=NpcProfile.maxQi(mob,npc.npcArchetype());
        int reserve=(int)Math.ceil(max*.40);
        for(String id:NpcProfile.learnedSpellIds(mob,npc.npcArchetype())) {
            int cost=NpcSpellcasting.qiCost(mob,id);
            if(cost<=max)reserve=Math.max(reserve,cost);
        }
        return reserve;
    }
    @SubscribeEvent public static void serverTick(ServerTickEvent.Post event){tickAll();}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){stop(event.getEntity());}
    @SubscribeEvent public static void changedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){stop(event.getEntity());}
    @SubscribeEvent public static void serverStop(ServerStoppedEvent event){
        for(var aura:new ArrayList<>(ACTIVE.values()))mobility(aura.owner,false);
        ACTIVE.clear();INPUT_TICK.clear();TOGGLE_TICK.clear();QiDomainDamage.clear();DomainClashes.clear();
    }
    public static void tickAll() {
        var actors=new ArrayList<>(ACTIVE.values());
        for(var a:actors) {
            var owner=a.owner;long now=owner.level().getGameTime();
            if(!owner.isAlive()||owner.isRemoved()||owner.isSpectator()||a.blasting&&(!eligible(owner)
                    ||owner instanceof ServerPlayer p&&!CombatStateManager.isInCombat(p))) {stop(owner);continue;}
            if(a.blasting&&owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc&&!(owner instanceof TrainingDummyEntity)
                    &&(now>=a.npcBlastUntil||!npcCombatTarget(mob))){stop(owner);continue;}
            if(!a.blasting&&now>a.until){stop(owner);continue;}
            a.method=method(owner);a.power=1;a.opponent=-1;
            if(a.domain()&&owner instanceof CultivationNpc&&!(owner instanceof TrainingDummyEntity)&&mastery(owner)<GREAT_MASTERY) {
                a.percent=100;a.contacts.clear();a.domainExposure.clear();send(a,true);
            }
            if(a.attackUntil<now)a.attackMode=0;
            if(a.blasting&&(a.lastUpkeep==Long.MIN_VALUE||now-a.lastUpkeep>=20)) {
                a.lastUpkeep=now;int cost=upkeep(owner,a.percent);
                if(owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc npc
                        &&!(owner instanceof TrainingDummyEntity)
                        &&(long)NpcProfile.qi(mob,npc.npcArchetype())-cost<npcSpellReserve(mob,npc)) {
                    stop(owner);continue;
                }
                boolean paid=owner instanceof ServerPlayer p?ServerEvents.spendSustainedQiOrBlood(p,ModAttachments.getData(p),cost):
                        owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc npc&&NpcProfile.spendQi(mob,npc.npcArchetype(),cost);
                if(!paid){stop(owner);if(owner instanceof ServerPlayer p)message(p,"empty");continue;}
                if(owner instanceof ServerPlayer p)ServerEvents.syncPlayerData(p);
            }
        }
        actors=new ArrayList<>(ACTIVE.values());
        DomainClashes.tick(actors);
        for(var a:actors) {
            long now=a.owner.level().getGameTime();
            if(a.domain()&&a.power>0&&mastery(a.owner)>=GREAT_MASTERY) {
                MethodDomainEffects.tick(a,now,a.owner.getRandom());
                if(now%2==0)tickMotes(a,now);
            }else {a.domainExposure.clear();}
            if(now%5==0)send(a,false);
        }
    }
    /** Deterministic moving pollen/wisps follow the owner. Previous positions sweep contacts while moving. */
    public static Vec3 motePosition(LivingEntity owner,int index,long tick) {
        return motePosition(owner,index,tick,MethodDomainStyle.of(method(owner))==MethodDomainStyle.EVERGREEN);
    }
    public static Vec3 pollenPosition(LivingEntity owner,int index,long tick){return motePosition(owner,index,tick,true);}
    private static Vec3 motePosition(LivingEntity owner,int index,long tick,boolean wood) {
        double angle=index*Math.PI*2/MOTES+tick*.012;
        // Equal-area bands cover the full twenty-block domain instead of bunching pollen near the body.
        double radius=DOMAIN_RADIUS*Math.sqrt((index%4+1)/4.0);
        if(wood) {
            angle+=Math.sin(tick*.009+index*1.71)*.09;
            if(index%4!=3)radius=Math.clamp(radius+Math.sin(tick*.013+index*2.37)*.65,10,DOMAIN_RADIUS);
        }
        return owner.position().add(Math.cos(angle)*radius, .7+(index%3)*.7+Math.sin(tick*.045+index)*.35, Math.sin(angle)*radius);
    }
    private static void tickMotes(Aura a,long now) {
        var owner=a.owner;var level=(ServerLevel)owner.level();var style=MethodDomainStyle.of(a.method);
        // These arts are visual-only or use whole-domain rolls / moving wind seams instead of motes.
        if(style.kind==MethodDomainStyle.Kind.AIR||style.kind==MethodDomainStyle.Kind.CLEAR
                ||AdvancedMethodDomains.handles(style)
                ||style.kind==MethodDomainStyle.Kind.REINCARNATION||style.kind==MethodDomainStyle.Kind.DEVOUR
                ||style.kind==MethodDomainStyle.Kind.STORM||style.kind==MethodDomainStyle.Kind.MOUNTAIN
                ||style.kind==MethodDomainStyle.Kind.TIDE||BloodLilyBloom.blood(style))return;
        ownerWard(owner,style,now);
        if(style.kind==MethodDomainStyle.Kind.WIND){MethodDomainEffects.windContacts(a,now);return;}
        var volume=volume(owner);
        var targets=level.getEntities(owner,owner.getBoundingBox().inflate(DOMAIN_RADIUS),e->
                (e instanceof LivingEntity||e instanceof Projectile)&&canDomainAffect(owner,e)&&volume.intersects(e.getBoundingBox()));
        if(targets.size()>96)targets=targets.subList(0,96);
        for(int i=0;i<MOTES;i++) {
            Vec3 point=motePosition(owner,i,now),before=a.motePrevious[i];a.motePrevious[i]=point;
            if(now<a.moteCooldown[i])continue;
            if(before==null||before.distanceToSqr(point)>16)before=point;
            var sweep=new net.minecraft.world.phys.AABB(before,point).inflate(.38);
            for(var target:targets)if(sweep.intersects(target.getBoundingBox())&&a.hitUntil.getOrDefault(target.getUUID(),0L)<=now) {
                a.moteCooldown[i]=now+50;a.hitUntil.put(target.getUUID(),now+20);
                burst(a,style,point,target,targets);break;
            }
        }
        a.hitUntil.entrySet().removeIf(e->e.getValue()<now);
    }
    private static void ownerWard(LivingEntity owner,MethodDomainStyle style,long now) {
        if(now%20!=0)return;
        switch(style.kind) {
            case BONE,MOUNTAIN -> owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,30,0,false,false));
            case STORM -> owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,30,0,false,false));
            case FIRE,SOLAR -> owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,30,0,false,false));
            case TIDE -> {owner.heal(Math.min(4,owner.getMaxHealth()*.002F));owner.clearFire();}
            default -> { }
        }
    }
    private static void burst(Aura a,MethodDomainStyle style,Vec3 point,Entity contact,List<Entity> nearby) {
        var owner=a.owner;var level=(ServerLevel)owner.level();
        PacketDistributor.sendToPlayersNear(level,null,point.x,point.y,point.z,64,
                new QiAuraPayloads.Burst(level.dimension().location(),point.x,point.y,point.z,style.ordinal()));
        float damage=QiDomainDamage.amount(owner,a.power);
        if(style.kind==MethodDomainStyle.Kind.WOOD) {
            QiWoodSpikes.eruptDomain(level,owner,point,damage);
            if(contact instanceof Projectile projectile)projectile.discard();
            return;
        }
        if(contact instanceof Projectile projectile) {projectile.discard();return;}
        if(!(contact instanceof LivingEntity victim))return;
        float multiplier=switch(style.kind) {
            case DRAIN,STORM -> .7F;
            case CROWN -> 1.2F;
            case BONE,SOLAR -> 1.15F;
            case TIDE -> .8F;
            default -> 1;
        };
        float before=victim.getHealth();
        if(!QiDomainDamage.hurt(owner,victim,damage*multiplier,
                style.kind==MethodDomainStyle.Kind.BONE||style.kind==MethodDomainStyle.Kind.MOUNTAIN))return;
        float dealt=Math.max(0,before-victim.getHealth());
        switch(style.kind) {
            case BLOOD,DRAIN,CROWN -> MethodDomainEffects.mark(a,victim);
            case FIRE -> {victim.igniteForSeconds(3);owner.heal(dealt*.05F);}
            case ICE -> {victim.setTicksFrozen(Math.max(victim.getTicksFrozen(),160));victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,1));}
            case BONE -> victim.push(0,.15,0);
            case NETHER -> {victim.addEffect(new MobEffectInstance(MobEffects.WITHER,60,0));victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0));}
            case SOLAR -> {victim.igniteForSeconds(4);victim.addEffect(new MobEffectInstance(MobEffects.GLOWING,80,0));}
            case STORM -> {
                int chained=0;
                for(var entity:nearby)if(entity!=victim&&entity instanceof LivingEntity living
                        &&living.distanceToSqr(point)<9&&canDomainAffect(owner,living)&&chained<2
                        &&QiDomainDamage.hurt(owner,living,damage*.7F,false)) {
                    chained++;a.hitUntil.put(living.getUUID(),level.getGameTime()+20);
                }
            }
            case MOUNTAIN -> {victim.setDeltaMovement(victim.getDeltaMovement().multiply(.4,0,.4).add(0,-.35,0));victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1));}
            case TIDE -> {victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,1));victim.setTicksFrozen(100);owner.heal(dealt*.08F);}
            default -> { }
        }
    }
    static int drain(LivingEntity victim,LivingEntity owner,int amount) {
        if(amount<=0)return 0;int used=0;
        if(victim instanceof ServerPlayer p) {
            var data=ModAttachments.getData(p);used=Math.min(data.qi(),amount);
            ModAttachments.setData(p,data.withQi(data.qi()-used));ServerEvents.syncPlayerData(p);
        } else if(victim instanceof PathfinderMob mob&&victim instanceof CultivationNpc npc) {
            used=Math.min(NpcProfile.qi(mob,npc.npcArchetype()),amount);NpcProfile.spendQi(mob,npc.npcArchetype(),used);
        }
        if(used<=0)return 0;
        if(owner instanceof ServerPlayer receiver){var own=ModAttachments.getData(receiver);
            ModAttachments.setData(receiver,own.withQi((int)Math.min(own.maxQi(),(long)own.qi()+used)));ServerEvents.syncPlayerData(receiver);}
        else if(owner instanceof PathfinderMob mob&&owner instanceof CultivationNpc npc)
            NpcProfile.restoreQi(mob,npc.npcArchetype(),Math.min(used,Math.max(0,NpcProfile.maxQi(mob,npc.npcArchetype())-NpcProfile.qi(mob,npc.npcArchetype()))));
        return used;
    }
    private static void send(Aura a,boolean force) {
        if(!(a.owner.level() instanceof ServerLevel level))return;
        long now=level.getGameTime();if(!force&&a.lastSend!=Long.MIN_VALUE&&now-a.lastSend<5)return;
        a.lastSend=now;int mode=a.blasting?BODY:a.attackMode!=0?a.attackMode:MOTE;
        if(a.until==0&&!a.blasting)mode=0;
        if(!a.blasting&&a.owner instanceof ServerPlayer p&&!MethodQiParticleEffects.showQiUseParticles(p))mode=0;
        var packet=new QiAuraPayloads.State(level.dimension().location(),a.owner.getId(),a.owner.getUUID(),a.method,
                a.percent,mode,a.attackMode,a.power,a.opponent,mastery(a.owner)>=GREAT_MASTERY,a.blasting?12:20,
                MethodDomainStyle.of(a.method)==MethodDomainStyle.SOLAR?AdvancedMethodDomains.sunEnergy(a.owner):0);
        PacketDistributor.sendToPlayersNear(level,null,a.owner.getX(),a.owner.getY(),a.owner.getZ(),96,packet);
    }
}
