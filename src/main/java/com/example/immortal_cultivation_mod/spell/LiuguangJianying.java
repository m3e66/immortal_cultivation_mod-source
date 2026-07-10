package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.entity.LiuguangJianyingProjectileEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class LiuguangJianying {
    private static final int BASE_PROJECTILE_COUNT = 50;
    private static final int SCALED_PROJECTILE_COUNT = 80;
    private static final double SPEED = 2.45D;

    private LiuguangJianying() {
    }

    public static void cast(ServerPlayer player, float chargeScale) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        float clampedScale = Math.max(1.0F, Math.min(2.0F, chargeScale));
        int projectileCount = Math.round(BASE_PROJECTILE_COUNT
                + (SCALED_PROJECTILE_COUNT - BASE_PROJECTILE_COUNT) * (clampedScale - 1.0F));
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);

        for (int i = 0; i < projectileCount; i++) {
            double t = (i + 0.5D) / projectileCount;
            double y = 1.0D - 2.0D * t;
            double radius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = i * Math.PI * (3.0D - Math.sqrt(5.0D));
            Vec3 direction = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius).normalize();
            LiuguangJianyingProjectileEntity projectile = new LiuguangJianyingProjectileEntity(level, player);
            projectile.setPos(origin.x + direction.x * 0.65D, origin.y + direction.y * 0.45D, origin.z + direction.z * 0.65D);
            projectile.shoot(direction.x, direction.y, direction.z, (float) SPEED, 0.0F);
            level.addFreshEntity(projectile);
        }
    }
}
