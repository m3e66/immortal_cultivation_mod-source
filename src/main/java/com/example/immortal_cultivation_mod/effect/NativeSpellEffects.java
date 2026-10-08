package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.entity.IceFxAnchorEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.network.SpellVfxPayloads;
import com.example.immortal_cultivation_mod.network.SpellVfxPayloads.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Native VFX only. No commands, external emitters, or persistent server-side visual state. */
public final class NativeSpellEffects {
    private NativeSpellEffects() {}

    public static void waterShield(ServerLevel level, Entity entity, int stack) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new SpellVfxPayloads.Visual(
                level.dimension().location(), Kind.WATER_SHIELD, entity.getId(), entity.position(),
                Math.clamp(stack, 1, 3), 30));
    }

    public static void removeWaterShield(ServerLevel level, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new SpellVfxPayloads.Visual(
                level.dimension().location(), Kind.WATER_SHIELD, entity.getId(), entity.position(), 1, 0));
    }

    public static void puddle(ServerLevel level, double x, double y, double z) {
        at(level, Kind.PUDDLE, new Vec3(x, y, z), 1, 30);
    }

    public static void removePuddle(ServerLevel level, BlockPos pos) {
        at(level, Kind.PUDDLE, new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D), 1, 0);
    }

    public static void deadSkeletonPressure(ServerLevel level, BlockPos pos) {
        deadSkeletonPressure(level, pos, 1);
    }

    public static void deadSkeletonPressure(ServerLevel level, BlockPos pos, int stage) {
        at(level, Kind.PRESSURE, Vec3.atBottomCenterOf(pos), Math.clamp(stage, 1, 27), 30);
    }

    public static void lingbengExplosion(ServerLevel level, Vec3 pos, float radius) {
        at(level, Kind.LINGBENG, pos, 1, 6);
    }

    public static void iceImpact(ServerLevel level, double x, double y, double z) {
        IceFxAnchorEntity anchor = ModEntities.ICE_FX_ANCHOR.get().create(level);
        if (anchor == null) return;
        anchor.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(anchor);
    }

    public static void beamSpell(ServerLevel level, ServerPlayer caster, int durationTicks) {
        var targets = level.getEntities(caster, new AABB(caster.blockPosition()).inflate(128.0D),
                entity -> !entity.isRemoved());
        targets.addFirst(caster);
        PacketDistributor.sendToPlayer(caster, new SpellVfxPayloads.BeamMarkers(
                level.dimension().location(), targets.stream().limit(SpellVfxPayloads.BeamMarkers.MAX_TARGETS)
                        .mapToInt(Entity::getId).toArray(), durationTicks));
    }

    private static void at(ServerLevel level, Kind kind, Vec3 pos, int strength, int duration) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, 64.0D,
                new SpellVfxPayloads.Visual(level.dimension().location(), kind, -1, pos, strength, duration));
    }
}
