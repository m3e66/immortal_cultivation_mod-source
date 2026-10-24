package com.example.immortal_cultivation_mod.spell;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** Wider spell contacts; does not change damage, explosions, or non-spell projectiles. */
public final class SpellProjectileCollision {
    private static final Set<String> SPELL_PROJECTILES = Set.of(
            "fireball_projectile",
            "ignite_flare_projectile",
            "light_beam_projectile",
            "lingzhi_bullet_projectile",
            "jinmang_chuansha_projectile",
            "elemental_technique_projectile",
            "spell_trigger_projectile",
            "wind_blade_projectile",
            "smoke_projectile",
            "sliding_water_projectile",
            "absorb_cultivation_projectile",
            "dingshen_projectile",
            "kongshi_projectile",
            "lightning_projectile",
            "frost_cry_projectile",
            "liuguang_jianying_projectile",
            "duanliu_kongdun_projectile",
            "yihen_ci_projectile",
            "suokong_projectile",
            "xinliao_fireball",
            "starfall_projectile",
            "sword_technique_projectile");
    private SpellProjectileCollision() {}

    public static float margin(Entity projectile, float original) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType());
        if (!"immortal_cultivation_mod".equals(id.getNamespace()) || !SPELL_PROJECTILES.contains(id.getPath())) {
            return original;
        }
        return Math.max(original, Math.max(projectile.getBbWidth(), projectile.getBbHeight()) * 0.5F);
    }
}
