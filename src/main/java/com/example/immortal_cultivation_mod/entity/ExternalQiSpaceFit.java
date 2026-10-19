package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A bounded, launch-time clearance check; no per-tick terrain scan or chunk loading. */
public final class ExternalQiSpaceFit {
    private static final double PROBE_DISTANCE = 6;
    private static final double MARGIN = .08;
    private ExternalQiSpaceFit() {}

    public static double fit(ServerPlayer player, Vec3 forward, int stage, double radius, boolean cone) {
        if (!QiInfusedWeapon.outwardQiAutoFitEnabled(player)) return radius;
        Vec3 center = player.getEyePosition().add(0, -.42, 0);
        Vec3[] directions = {new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)};
        double[] positive = new double[3], negative = new double[3];
        for (int i = 0; i < 3; i++) {
            positive[i] = clearance(player, center, directions[i]);
            negative[i] = clearance(player, center, directions[i].scale(-1));
        }
        // Ground alone is not an enclosure: outdoor casts retain their full weapon-level size.
        boolean enclosed = Double.isFinite(positive[1])
                || (Double.isFinite(positive[0]) && Double.isFinite(negative[0]))
                || (Double.isFinite(positive[2]) && Double.isFinite(negative[2]));
        if (!enclosed) return radius;
        double[] available = new double[3];
        for (int i = 0; i < 3; i++) available[i] = Math.max(0, Math.min(positive[i], negative[i]) - MARGIN);
        Vec3 axis = ExternalQiVfxShape.axis(forward, stage);
        Vec3 perpendicular = forward.cross(axis).normalize();
        if (fits(radius, forward, axis, perpendicular, available, cone)) return radius;
        double low = .05, high = radius;
        for (int iteration = 0; iteration < 14; iteration++) {
            double candidate = (low + high) * .5;
            if (fits(candidate, forward, axis, perpendicular, available, cone)) low = candidate;
            else high = candidate;
        }
        return Math.min(radius, low);
    }

    private static boolean fits(double radius, Vec3 forward, Vec3 axis, Vec3 perpendicular,
                                double[] available, boolean cone) {
        for (int i = 0; i < 3; i++) {
            double direction = component(forward, i);
            double extent = cone ? radius * Math.sqrt(Math.max(0, 1 - direction * direction))
                    : ExternalQiVfxShape.bladeReach(radius) * Math.abs(component(axis, i))
                    + ExternalQiVfxShape.halfThickness(radius) * Math.abs(component(perpendicular, i))
                    + Math.min(.9, radius * .22) * Math.abs(direction);
            if (extent > available[i]) return false;
        }
        return true;
    }

    private static double component(Vec3 vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    private static double clearance(ServerPlayer player, Vec3 from, Vec3 direction) {
        var level = player.serverLevel();
        for (int step = 0; step <= PROBE_DISTANCE; step++) {
            if (!level.hasChunkAt(BlockPos.containing(from.add(direction.scale(step))))) return Double.POSITIVE_INFINITY;
        }
        var hit = level.clip(new ClipContext(from, from.add(direction.scale(PROBE_DISTANCE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : from.distanceTo(hit.getLocation());
    }
}
