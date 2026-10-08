package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.network.FlameImpactPayload;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Immediate damage/knockback and complete terrain destruction only for an accepted explosion. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class ChargedFlameImpact extends ExplosionDamageCalculator {
    private final FlameShotEntity shot;
    private final ServerPlayer owner;
    private final ServerLevel level;
    private final Vec3 center;
    private final Entity directTarget;
    private final Set<UUID> damaged = new HashSet<>();
    private boolean started;

    private ChargedFlameImpact(FlameShotEntity shot, ServerPlayer owner, ServerLevel level, HitResult hit) {
        this.shot = shot;
        this.owner = owner;
        this.level = level;
        // Keep the shared damage, terrain, and visual origin just outside the struck surface.
        this.center = hit instanceof BlockHitResult block
                ? hit.getLocation().add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.05))
                : hit.getLocation();
        this.directTarget = hit instanceof EntityHitResult entity
                ? ModelBonePartEntity.resolveParent(entity.getEntity()) : null;
    }

    static void explode(FlameShotEntity shot, HitResult hit) {
        if (!(shot.level() instanceof ServerLevel level) || !(shot.getOwner() instanceof ServerPlayer owner)
                || !owner.isAlive() || owner.isSpectator() || owner.level() != level) return;
        var impact = new ChargedFlameImpact(shot, owner, level, hit);
        shot.activeImpact = impact;
        try {
            // Vanilla explosion events can cancel the whole impact or filter affected blocks/entities.
            // No fire placement: an entity impact must never create a ground fire block.
            level.explode(shot, level.damageSources().source(DamageTypes.FIREBALL, shot, owner), impact,
                    // Vanilla doubles explosion strength for its entity radius.
                    impact.center, shot.blastRadius() * .5F, false, Level.ExplosionInteraction.NONE);
            if (impact.started) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(owner, "giant_impact");
                FlameImpactPayload.prepare(shot, hit, impact.center).ifPresent(payload -> FlameImpactPayload.send(level, payload));
                FlameTerrainDestruction.destroy(level, owner, shot, impact.center, shot.destructionRadius());
            }
        } finally {
            shot.activeImpact = null;
        }
    }

    private float damageScale(Entity target) {
        if (target == directTarget) return 1;
        double radius = shot.blastRadius();
        return target.distanceToSqr(center) <= radius * radius ? 1 : 0;
    }

    @Override public boolean shouldDamageEntity(Explosion explosion, Entity target) {
        // Only the real parent receives damage; multipart collision boxes must not multiply it.
        return ModelBonePartEntity.resolveParent(target) == target && shot.canDamageTarget(target)
                && !target.fireImmune() && damageScale(target) > 0;
    }

    @Override public float getEntityDamageAmount(Explosion explosion, Entity target) {
        return shot.kind().shotDamage() * shot.powerMultiplier() * damageScale(target);
    }

    @Override public float getKnockbackMultiplier(Entity target) {
        // A denied/absorbed fire hit must not push an ally, immune target, or protected player.
        return damaged.contains(target.getUUID()) ? .8F : 0;
    }

    @Override public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter blocks,
            BlockPos pos, BlockState state, FluidState fluid) {
        // This callback runs only after ExplosionEvent.Start accepts the blast. Stop vanilla terrain
        // ray marching immediately; terrain is cleared once by the exact-radius crater traversal.
        started = true;
        return Optional.of(Float.MAX_VALUE);
    }

    @SubscribeEvent public static void onAcceptedBlastDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0 || !event.getSource().is(DamageTypes.FIREBALL)
                || !(event.getSource().getDirectEntity() instanceof FlameShotEntity shot)
                || shot.activeImpact == null) return;
        shot.activeImpact.damaged.add(event.getEntity().getUUID());
        SpiritFlameBurnEffect.ignite(event.getEntity(), shot.kind());
    }
}
