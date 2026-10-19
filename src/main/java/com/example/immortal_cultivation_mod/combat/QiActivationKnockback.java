package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-only impulse for the one-shot release blast, with realm pressure and ordinary knockback resistance. */
public final class QiActivationKnockback {
    public static final double RADIUS = 18;

    public static void nearby(LivingEntity owner, int percent) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        var targets = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(RADIUS),
                target -> target != owner && target.distanceToSqr(owner) <= RADIUS * RADIUS);
        targets.sort(java.util.Comparator.comparingDouble(owner::distanceToSqr));
        int count = 0;
        for (var target : targets) {
            if (count++ >= 96) break;
            push(owner,target,percent);
        }
    }

    /** Same realm: full impulse; higher realms resist; two realms above cannot be displaced. */
    public static double realmFactor(LivingEntity owner, LivingEntity target) {
        int realmGap = CultivationLevels.getRealmIndex(QiDomainSystem.level(owner))
                - CultivationLevels.getRealmIndex(QiDomainSystem.level(target));
        if (realmGap <= -2) return 0;
        if (realmGap == -1) return .2;
        if (realmGap > 0) return Math.min(2.5,1 + realmGap * .6);
        int stageGap = QiDomainSystem.stage(owner) - QiDomainSystem.stage(target);
        return Math.clamp(1 + stageGap * .12,.76,1.24);
    }

    public static boolean push(LivingEntity owner, LivingEntity target, int percent) {
        if (owner.level().isClientSide || owner.level()!=target.level() || !owner.isAlive() || owner.isRemoved()
                || !QiDomainSystem.canAffect(owner,target) || target.isInvulnerable()
                || !NpcProfile.isCombatEnemy(owner,target) || !owner.hasLineOfSight(target)) return false;
        double distance = owner.distanceTo(target), factor = realmFactor(owner,target);
        if (distance > RADIUS || factor <= 0) return false;
        Vec3 away = target.position().subtract(owner.position()).multiply(1,0,1);
        if (away.horizontalDistanceSqr()<1E-8) away=owner.getLookAngle().multiply(1,0,1);
        if (away.horizontalDistanceSqr()<1E-8) away=new Vec3(1,0,0);
        away=away.normalize();
        double intensity = .75 + .25 * ((Math.clamp(percent,100,200)-100)/100.0);
        double falloff = Math.max(.2,1-distance/RADIUS);
        Vec3 before = target.getDeltaMovement();
        target.knockback(1.2 * factor * intensity * falloff,-away.x,-away.z);
        if (target.getDeltaMovement().distanceToSqr(before)<1E-12) return false;
        target.hasImpulse=true;
        target.hurtMarked=true;
        if (target instanceof ServerPlayer player)
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        if(owner.level() instanceof ServerLevel level){
            Vec3 point=target.position(),motion=target.getDeltaMovement();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level,null,point.x,point.y,point.z,80,
                    new com.example.immortal_cultivation_mod.network.QiAuraPayloads.Motion(level.dimension().location(),target.getId(),target.getUUID(),
                            point.x,point.y,point.z,(float)motion.x,(float)motion.y,(float)motion.z,
                            MethodDomainStyle.of(QiDomainSystem.method(owner)).ordinal(),
                            net.minecraft.world.level.block.Block.getId(level.getBlockState(target.blockPosition().below())),false));
        }
        return true;
    }

    private QiActivationKnockback() {}
}
