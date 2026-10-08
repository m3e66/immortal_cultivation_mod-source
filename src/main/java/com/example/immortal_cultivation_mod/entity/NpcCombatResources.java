package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyEffects;
import com.example.immortal_cultivation_mod.alchemy.PillData;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Emergency actions consume the same saved inventory that trading, looting and equipment use. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class NpcCombatResources {
    public enum Purpose { ESCAPE, SHIELD, CLEANSE, QI, HEALTH }
    private NpcCombatResources() {}
    private static String archetype(PathfinderMob npc){return ((CultivationNpc)npc).npcArchetype();}
    private static boolean sealed(PathfinderMob npc) {
        return npc.hasEffect(ModEffects.DINGSHEN)||npc.hasEffect(ModEffects.SPIRIT_SEAL)
                ||npc.hasEffect(ModEffects.FROZEN)||npc.hasEffect(ModEffects.FROZEN_QI)||npc.hasEffect(ModEffects.DAZE);
    }
    public static boolean available(PathfinderMob npc,Purpose purpose,Vec3 escape) {
        if (sealed(npc))return false;
        for (ItemStack stack:NpcProfile.inventory(npc))if (usable(npc,stack,purpose,escape))return true;
        return false;
    }
    private static boolean usable(PathfinderMob npc,ItemStack stack,Purpose purpose,Vec3 escape) {
        if (stack.isEmpty())return false;
        var p=NpcProfile.cultivationProfile(npc);long now=npc.level().getGameTime();
        if (p.getLong("ResourceReady."+purpose)>now)return false;
        if (purpose==Purpose.HEALTH) {
            var dose=PillData.effects(stack).get(AlchemyChemistry.Effect.HEALTH);
            return dose!=null&&dose.amount()>0&&npc.getHealth()<npc.getMaxHealth()*.8
                    &&npc.getPersistentData().getLong("NextCultivationPill")<=now
                    &&!AlchemyEffects.hasActive(npc,AlchemyChemistry.Effect.HEALTH);
        }
        if (purpose==Purpose.QI)return PillData.effects(stack).containsKey(AlchemyChemistry.Effect.QI)
                &&npc.getPersistentData().getLong("NextCultivationPill")<=now
                &&!AlchemyEffects.hasActive(npc,AlchemyChemistry.Effect.QI)
                &&NpcProfile.qi(npc,archetype(npc))<NpcProfile.maxQi(npc,archetype(npc))*.5;
        if (!(stack.getItem() instanceof UtilityTalismanItem talisman))return false;
        return switch(purpose) {
            case ESCAPE -> talisman.kind()==UtilityTalismanItem.Kind.VOID_ESCAPE?blinkDestination(npc)!=null:
                    talisman.kind()==UtilityTalismanItem.Kind.SHORT_DIVINE_TRAVEL&&escape!=null&&!npc.hasEffect(MobEffects.MOVEMENT_SPEED);
            case SHIELD -> shield(npc)<=0&&(talisman.kind()==UtilityTalismanItem.Kind.THIN_GOLD_SHIELD
                    ||talisman.kind()==UtilityTalismanItem.Kind.GOLDEN_BODY_SHIELD);
            case CLEANSE -> talisman.kind()==UtilityTalismanItem.Kind.CLEAN_DUST
                    &&(npc.isOnFire()||npc.hasEffect(MobEffects.BLINDNESS)||npc.hasEffect(MobEffects.DARKNESS))
                    ||talisman.kind()==UtilityTalismanItem.Kind.CALM_SOUL
                    &&(npc.hasEffect(MobEffects.DARKNESS)||npc.hasEffect(MobEffects.CONFUSION));
            default -> false;
        };
    }
    /** Reserve a copy, not an item: interrupted preparation leaves the saved inventory intact. */
    static ItemStack healingPill(PathfinderMob npc) {
        if(sealed(npc))return ItemStack.EMPTY;
        for(ItemStack stack:NpcProfile.inventory(npc))
            if(usable(npc,stack,Purpose.HEALTH,null))return stack.copyWithCount(1);
        return ItemStack.EMPTY;
    }
    static boolean finishHealingPill(PathfinderMob npc,ItemStack reserved) {
        if(!(npc.level() instanceof ServerLevel)||sealed(npc)||reserved.isEmpty())return false;
        var inventory=NpcProfile.inventory(npc);
        for(ItemStack stack:inventory) {
            if(!ItemStack.isSameItemSameComponents(stack,reserved)||!usable(npc,stack,Purpose.HEALTH,null))continue;
            if(!AlchemyEffects.consume(npc,stack))return false;
            stack.shrink(1);
            var profile=NpcProfile.cultivationProfile(npc);NpcProfile.writeInventory(npc,profile,inventory);
            profile.putLong("ResourceReady.HEALTH",npc.level().getGameTime()+100);
            npc.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT,1,1);
            return true;
        }
        return false;
    }
    public static boolean use(PathfinderMob npc,Purpose purpose,Vec3 escape) {
        // Healing pills are released only by the brain's interruptible preparation action.
        if (purpose==Purpose.HEALTH)return false;
        if (!(npc.level() instanceof ServerLevel level)||sealed(npc))return false;
        var inventory=NpcProfile.inventory(npc);var p=NpcProfile.cultivationProfile(npc);long now=level.getGameTime();
        for (ItemStack stack:inventory) {
            if (!usable(npc,stack,purpose,escape))continue;
            if (purpose==Purpose.QI) {
                if(!AlchemyEffects.consume(npc,stack))return false;
            } else {
                var kind=((UtilityTalismanItem)stack.getItem()).kind();
                switch(kind) {
                    case VOID_ESCAPE -> {
                        Vec3 destination=blinkDestination(npc);if(destination==null)return false;
                        npc.getNavigation().stop();npc.teleportTo(destination.x,destination.y,destination.z);
                        npc.setDeltaMovement(Vec3.ZERO);npc.fallDistance=0;
                    }
                    case SHORT_DIVINE_TRAVEL -> {
                        npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,7));
                        npc.addEffect(new MobEffectInstance(MobEffects.JUMP,600,2));
                        npc.getNavigation().moveTo(escape.x,escape.y,escape.z,1.2);
                    }
                    case THIN_GOLD_SHIELD, GOLDEN_BODY_SHIELD -> {
                        p.putFloat("TalismanShield",(kind==UtilityTalismanItem.Kind.THIN_GOLD_SHIELD?200:600)+npc.getMaxHealth()*.01F);
                        p.putLong("ShieldUntil",now+(kind==UtilityTalismanItem.Kind.THIN_GOLD_SHIELD?300:500));
                    }
                    case CLEAN_DUST -> {npc.clearFire();npc.removeEffect(MobEffects.BLINDNESS);npc.removeEffect(MobEffects.DARKNESS);}
                    case CALM_SOUL -> {npc.removeEffect(MobEffects.DARKNESS);npc.removeEffect(MobEffects.CONFUSION);}
                    default -> {return false;}
                }
            }
            stack.shrink(1);NpcProfile.writeInventory(npc,p,inventory);
            p.putLong("ResourceReady."+purpose,now+40);
            level.sendParticles(purpose==Purpose.SHIELD?ParticleTypes.ENCHANTED_HIT:ParticleTypes.CLOUD,
                    npc.getX(),npc.getY()+1,npc.getZ(),24,.7,.8,.7,.03);
            return true;
        }
        return false;
    }

    /** Only loaded, unobstructed, supported destinations 12–20 blocks away; seals/forcefields still win. */
    static Vec3 blinkDestination(PathfinderMob npc) {
        if (sealed(npc))return null;
        LivingEntity enemy=npc.getTarget();
        Vec3 away=enemy!=null&&npc.hasLineOfSight(enemy)?npc.position().subtract(enemy.position()):npc.getLookAngle();
        away=new Vec3(away.x,0,away.z).normalize();if(away.lengthSqr()<.1)away=new Vec3(1,0,0);
        for(int i=0;i<9;i++) {
            double angle=(i-4)*Math.PI/9;Vec3 direction=new Vec3(away.x*Math.cos(angle)-away.z*Math.sin(angle),0,away.x*Math.sin(angle)+away.z*Math.cos(angle));
            for(int distance=20;distance>=12;distance-=4)for(int y=2;y>=-3;y--) {
                Vec3 point=npc.position().add(direction.scale(distance)).add(0,y,0);BlockPos feet=BlockPos.containing(point);
                if (!npc.level().hasChunkAt(feet)||!npc.level().getWorldBorder().isWithinBounds(feet))continue;
                if (!npc.level().getBlockState(feet.below()).isFaceSturdy(npc.level(),feet.below(),net.minecraft.core.Direction.UP)
                        ||!npc.level().getFluidState(feet).isEmpty()||!npc.level().getFluidState(feet.below()).isEmpty()
                        ||npc.level().getBlockState(feet.below()).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                        ||!npc.level().noCollision(npc,npc.getBoundingBox().move(point.subtract(npc.position())))
                        ||CenterEffectSupport.isDangerousPosition(npc,point)
                        ||CenterEffectSupport.isForcefieldRouteBlocked(npc,npc.position(),point))continue;
                if (npc.level().clip(new ClipContext(npc.getEyePosition(),point.add(0,npc.getEyeHeight(),0),
                        ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,npc)).getType()!=HitResult.Type.MISS)continue;
                return point;
            }
        }
        return null;
    }
    public static float shield(LivingEntity entity) {
        if (!(entity instanceof PathfinderMob npc)||!(entity instanceof CultivationNpc))return 0;
        var p=NpcProfile.cultivationProfile(npc);
        return p.getLong("ShieldUntil")>entity.level().getGameTime()?p.getFloat("TalismanShield"):0;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void absorb(LivingIncomingDamageEvent event) {
        float amount=shield(event.getEntity());if(amount<=0||event.getAmount()<=0)return;
        float absorbed=Math.min(amount,event.getAmount());
        var npc=(PathfinderMob)event.getEntity();NpcProfile.cultivationProfile(npc).putFloat("TalismanShield",amount-absorbed);
        event.setAmount(event.getAmount()-absorbed);
    }
    static void tick(PathfinderMob npc) {
        AlchemyEffects.tickLiving(npc);
        var p=NpcProfile.cultivationProfile(npc);long now=npc.level().getGameTime();
        if(p.getLong("PillUntil")>=now&&p.getLong("PillNext")>0&&p.getLong("PillNext")<=now) {
            NpcProfile.restoreQi(npc,archetype(npc),p.getInt("PillRate"));p.putLong("PillNext",now+20);
        }
        if(shield(npc)>0&&now%10==0&&npc.level() instanceof ServerLevel level)
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,npc.getX(),npc.getY()+1,npc.getZ(),6,.6,.8,.6,.01);
    }
    static boolean burnEssence(PathfinderMob npc) {
        if(NpcProfile.isSparring(npc)||sealed(npc))return false;
        var p=NpcProfile.cultivationProfile(npc);long now=npc.level().getGameTime();
        // Forbidden combustion requires actual blood training; it converts finite life/health into Qi.
        boolean trained=NpcProfile.learnedSpellIds(npc,archetype(npc)).contains(com.example.immortal_cultivation_mod.spell.ModSpells.XUE_GUIYUAN_SHU)
                ||p.getString("Method").equals(com.example.immortal_cultivation_mod.attachment.CultivationMethods.BLOOD_DEMON_HUASHEN);
        int max=NpcProfile.maxQi(npc,archetype(npc));float healthCost=Math.max(1,npc.getMaxHealth()*.04F);
        if(!trained||p.getLong("BloodBurnReady")>now||p.getInt("Age")<=5||npc.getHealth()<=healthCost
                ||NpcProfile.qi(npc,archetype(npc))>max*.35)return false;
        p.putInt("Age",p.getInt("Age")-5);npc.setHealth(npc.getHealth()-healthCost);
        NpcProfile.restoreQi(npc,archetype(npc),Math.max(1,(int)(max*.35)));
        p.putLong("BloodBurnReady",now+200);revealFrenzy(npc);return true;
    }
    static boolean detonateArtifact(PathfinderMob npc,LivingEntity target) {
        if(NpcProfile.isSparring(npc)||sealed(npc)||!npc.hasLineOfSight(target)||npc.distanceToSqr(target)>25)return false;
        ItemStack weapon=npc.getMainHandItem();double stored=QiInfusedWeapon.getQi(weapon);
        if(weapon.isEmpty()||!QiInfusedWeapon.supportsOutwardQi(weapon)||stored<=0)return false;
        if(!npc.level().getEntitiesOfClass(LivingEntity.class,npc.getBoundingBox().inflate(6),
                e->e!=npc&&e.isAlive()&&NpcProfile.areCombatAllies(npc,e)).isEmpty())return false;
        float damage=(float)Math.min(npc.getAttributeValue(Attributes.ATTACK_DAMAGE)*4,stored*.6);
        if(damage<=0||!NpcProfile.consumeEquippedArtifact(npc))return false;
        revealFrenzy(npc);
        ((ServerLevel)npc.level()).sendParticles(ParticleTypes.EXPLOSION_EMITTER,npc.getX(),npc.getY()+1,npc.getZ(),1,0,0,0,0);
        for(LivingEntity enemy:npc.level().getEntitiesOfClass(LivingEntity.class,npc.getBoundingBox().inflate(5),
                e->NpcProfile.isCombatEnemy(npc,e)&&npc.hasLineOfSight(e)))
            enemy.hurt(npc.damageSources().mobAttack(npc),damage*(float)Math.max(.25,1-npc.distanceTo(enemy)/7));
        npc.hurt(npc.damageSources().magic(),damage*.25F);
        return true;
    }
    private static void revealFrenzy(PathfinderMob npc) {
        npc.getPersistentData().putLong("NpcVisibleFrenzyUntil",npc.level().getGameTime()+80);
        ((ServerLevel)npc.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR,npc.getX(),npc.getY()+1,npc.getZ(),24,.7,.8,.7,.15);
    }
}
