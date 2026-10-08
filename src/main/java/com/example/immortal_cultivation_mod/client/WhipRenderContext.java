package com.example.immortal_cultivation_mod.client;

import net.minecraft.world.entity.LivingEntity;

/** Rendering is on the client thread; the mixin restores the previous context in finally. */
public final class WhipRenderContext {
    private static LivingEntity holder;
    public static LivingEntity holder(){return holder;}
    public static LivingEntity set(LivingEntity value){var old=holder;holder=value;return old;}
    private WhipRenderContext(){}
}
