package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class UndeadRepel {
    private UndeadRepel() {
    }

    static void bounceFrom(LivingEntity entity, BlockPos pos) {
        Vec3 away = entity.position().subtract(Vec3.atCenterOf(pos));
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = Vec3.directionFromRotation(0.0F, entity.getYRot()).reverse();
        }
        Vec3 push = horizontal.normalize().scale(0.85D);
        entity.setDeltaMovement(push.x, 0.48D, push.z);
        entity.hurtMarked = true;
        entity.fallDistance = 0.0F;
    }
}
