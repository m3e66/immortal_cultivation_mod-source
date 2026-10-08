package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Synchronizes the brief NPC guard pose to clients. Damage handling lives in NpcCombatReactions. */
public final class NpcWeaponGuardEffect extends MobEffect {
    public NpcWeaponGuardEffect() { super(MobEffectCategory.NEUTRAL, 0x86BEC9); }
}
