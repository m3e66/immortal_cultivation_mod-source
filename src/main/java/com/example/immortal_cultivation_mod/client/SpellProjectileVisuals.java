package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.PoseStack;

/** World spell visuals only; never changes projectile physics or damage radii. */
public final class SpellProjectileVisuals {
    public static final float SCALE = 2.0F;
    private SpellProjectileVisuals() {}

    public static void enlarge(PoseStack pose) { pose.scale(SCALE, SCALE, SCALE); }
}
