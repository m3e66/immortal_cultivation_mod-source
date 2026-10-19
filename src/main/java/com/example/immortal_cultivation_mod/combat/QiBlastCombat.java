package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Qi augments the original melee hit, so armor, shields and cancellation still apply once. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class QiBlastCombat {
    public static final double FIST_SPLASH_RADIUS = 2;
    public static final float FIST_SPLASH_DAMAGE = .35F;
    private static final ThreadLocal<DamageSource> SPLASH_SOURCE = new ThreadLocal<>();

    public static int currentQi(LivingEntity owner) {
        return Math.max(0,owner instanceof ServerPlayer p?ModAttachments.getData(p).qi():
                owner instanceof CultivationNpc npc?npc.npcQi():0);
    }
    public static boolean isFistImpact(LivingEntity owner,DamageSource source) {
        if(SPLASH_SOURCE.get()==source||QiDomainDamage.isContactDamage(source)||source.getEntity()!=owner
                ||source.getDirectEntity()!=owner||!(source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)
                ||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)))return false;
        if(owner instanceof ServerPlayer&&!source.is(DamageTypes.PLAYER_ATTACK))return false;
        var held=owner instanceof ServerPlayer p?
                com.example.immortal_cultivation_mod.item.WeaponSkillAttack.sourceWeapon(p,source):owner.getMainHandItem();
        return held.isEmpty()||QiInfusedWeapon.isFistWeapon(held);
    }
    public static float fistBonus(LivingEntity owner,DamageSource source) {
        if(!QiDomainSystem.blasting(owner)||!QiDomainSystem.eligible(owner)||!isFistImpact(owner,source))return 0;
        // One percent of remaining Qi at 100% release; two percent at 200%.
        return (float)(currentQi(owner)*.01*Math.clamp(QiDomainSystem.percent(owner),100,200)/100.0);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if(event.getEntity().level().isClientSide||!Float.isFinite(event.getAmount())||event.getAmount()<=0)return;
        if(event.getSource().getEntity() instanceof LivingEntity owner)
            event.setAmount(event.getAmount()+fistBonus(owner,event.getSource()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(LivingDamageEvent.Post event) {
        var target=event.getEntity();var source=event.getSource();
        if(!(target.level() instanceof ServerLevel level)||!Float.isFinite(event.getNewDamage())||event.getNewDamage()<=0
                ||!(source.getEntity() instanceof LivingEntity owner)||owner==target||!QiDomainSystem.blasting(owner)||!isFistImpact(owner,source))return;
        var direction=CombatFeedbackEvents.incomingDirection(target,source);
        var point=CombatFeedbackEvents.contactPoint(target,source,direction);
        float power=(float)Math.clamp(QiDomainSystem.percent(owner)/200.0,.5,1);
        PacketDistributor.sendToPlayersNear(level,null,point.x,point.y,point.z,80,
                new QiAuraPayloads.Hit(level.dimension().location(),target.getId(),target.getUUID(),
                        point.x,point.y,point.z,MethodDomainStyle.of(QiDomainSystem.method(owner)).ordinal(),power));
        if (fistBonus(owner, source) > 0) fistSplash(level, owner, target, source, event.getNewDamage());
    }

    /** Only a confirmed fist hit splashes; secondary hits cannot add the Qi bonus or splash again. */
    private static void fistSplash(ServerLevel level, LivingEntity owner, LivingEntity primary,
                                   DamageSource original, float dealt) {
        var center = primary.getBoundingBox().getCenter();
        var targets = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(FIST_SPLASH_RADIUS),
                other -> other != primary && QiDomainSystem.canAffect(owner, other)
                        && !com.example.immortal_cultivation_mod.entity.NpcProfile.areCombatAllies(owner, other)
                        && other.getBoundingBox().getCenter().distanceToSqr(center) <= FIST_SPLASH_RADIUS * FIST_SPLASH_RADIUS
                        && owner.hasLineOfSight(other)
                        && level.clip(new net.minecraft.world.level.ClipContext(center, other.getBoundingBox().getCenter(),
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                net.minecraft.world.level.ClipContext.Fluid.NONE, owner)).getType()
                                == net.minecraft.world.phys.HitResult.Type.MISS);
        targets.sort(java.util.Comparator.comparingDouble(other -> other.getBoundingBox().getCenter().distanceToSqr(center)));
        var source = new DamageSource(original.typeHolder(), owner);
        var previous = SPLASH_SOURCE.get();
        SPLASH_SOURCE.set(source);
        try {
            for (int i = 0; i < Math.min(8, targets.size()); i++)
                targets.get(i).hurt(source, dealt * FIST_SPLASH_DAMAGE);
        } finally {
            if (previous == null) SPLASH_SOURCE.remove(); else SPLASH_SOURCE.set(previous);
        }
    }
    private QiBlastCombat() {}
}
