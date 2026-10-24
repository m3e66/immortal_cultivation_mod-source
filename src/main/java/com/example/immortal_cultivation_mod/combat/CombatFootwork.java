package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Short collision-checked steps. No teleportation or invulnerability. */
public final class CombatFootwork {
    private CombatFootwork() {}
    public static boolean disabled(LivingEntity actor) {
        return !actor.isAlive() || actor.isPassenger() || actor.isInWaterOrBubble()
            || ColdTideFreeze.isFrozen(actor) || actor.hasEffect(ModEffects.DAZE)
            || actor.hasEffect(ModEffects.DINGSHEN) || actor.hasEffect(ModEffects.FROZEN)
            || actor.hasEffect(ModEffects.FROZEN_QI)
            || actor instanceof ServerPlayer player && (player.isSpectator() || CombatStateManager.isPostureBroken(player));
    }
    public static boolean backstep(ServerPlayer player) {
        if (!CombatStateManager.isInCombat(player) || disabled(player)
            || (!player.onGround() && (player.getDeltaMovement().y <= 0 || player.fallDistance > 0))) return false;
        // The input packet may arrive immediately after the ordinary jump packet.
        if (!player.onGround() && !player.level().getBlockState(player.blockPosition().below()).blocksMotion()) return false;
        if (!step(player, player.getLookAngle().reverse(), .66)) return false;
        CombatStateManager.cancelStrike(player);
        CombatStateManager.cancelHeavyCharge(player);
        CombatStateManager.interruptSpellPreparation(player);
        CombatStateManager.setBlocking(player, false);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
            new CombatPayloads.ClientboundSwordComboAnimationPayload(player.getUUID(), CombatPayloads.BACKSTEP_ANIMATION));
        return true;
    }
    public static boolean step(LivingEntity actor, Vec3 direction, double speed) {
        if (!(actor.level() instanceof ServerLevel level) || disabled(actor)) return false;
        var data = actor.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLong("CombatStepReady")) return false;
        int chain = now < data.getLong("CombatStepWindow") ? data.getInt("CombatStepChain") : 0;
        if (chain >= 3) return false;
        Vec3 unit = direction.multiply(1,0,1).normalize();
        if (unit.lengthSqr() < .5) return false;
        Vec3 travel = unit.scale(1.8);
        for (int i=1;i<=6;i++) {
            Vec3 offset = travel.scale(i/6D);
            Vec3 point = actor.position().add(offset);
            BlockPos floor = BlockPos.containing(point).below();
            if (!level.hasChunkAt(floor) || !level.getWorldBorder().isWithinBounds(floor)
                || !level.getBlockState(floor).blocksMotion() || !level.getFluidState(floor).isEmpty()
                || !level.noCollision(actor, actor.getBoundingBox().move(offset).deflate(.02))
                || CenterEffectSupport.isForcefieldRouteBlocked(actor, actor.position(), point)) return false;
        }
        if (actor instanceof PathfinderMob mob) mob.getNavigation().stop();
        actor.setDeltaMovement(unit.x*speed, Math.max(.08, Math.min(.42,actor.getDeltaMovement().y)),unit.z*speed);
        actor.hurtMarked = true;
        data.putLong("CombatStepReady",now+12+chain*8);
        data.putLong("CombatStepWindow",now+100);
        data.putInt("CombatStepChain",chain+1);
        CombatExchange.reset(actor);
        CombatFeedbackEvents.dashStarted(actor,unit,.35F,8);
        if (actor instanceof ServerPlayer player) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(actor));
        return true;
    }
}
