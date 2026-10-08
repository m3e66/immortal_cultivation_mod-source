package com.example.immortal_cultivation_mod.externalpower;

import net.minecraft.resources.ResourceLocation;

/** Immutable observations: no retained entity, world, weapon stack, or DamageSource references. */
public record ExternalAttackProfile(
        String modId, ResourceLocation damageType, float rawDamage, float finalVanillaDamage,
        boolean explosion, boolean projectile, boolean melee, boolean magic, boolean fire, boolean lightning,
        boolean environmental, boolean bypassArmor, boolean bypassEffects, boolean bypassEnchantments,
        boolean bypassInvulnerability, boolean specialKill,
        ResourceLocation attackerEntity, ResourceLocation directEntity, ResourceLocation weaponItem,
        double attackerMaxHealth, double attackerAttackDamage,
        double explosionRadius, double explosionDistance, double observedBlockRadius,
        int burstHits, double burstDamage, double hitsPerSecond,
        double damageScore, double penetrationScore, int estimatedPowerTier, int penetrationTier,
        double confidence, boolean manualOverride) {
    public record Key(String modId, ResourceLocation damageType, ResourceLocation attackerEntity,
                      ResourceLocation directEntity, ResourceLocation weaponItem, ResourceLocation projectileType) {}
    public record Traits(boolean explosion, boolean projectile, boolean melee, boolean magic, boolean fire,
                         boolean lightning, boolean environmental, boolean bypassArmor, boolean bypassEffects,
                         boolean bypassEnchantments, boolean bypassInvulnerability, boolean specialKill) {}
    public record Observation(Key key, Traits traits, float raw, float reduced, double maxHealth, double attackDamage,
                              double explosionRadius, double explosionDistance, double blockRadius,
                              int burstHits, double burstDamage, double hitsPerSecond) {}
    public record Estimate(double damageScore, double penetrationScore, int powerTier, int penetrationTier, double confidence) {}
    public static ExternalAttackProfile from(Observation o, Estimate e, boolean manual) {
        var k=o.key(); var t=o.traits();
        return new ExternalAttackProfile(k.modId(),k.damageType(),o.raw(),o.reduced(),t.explosion(),t.projectile(),t.melee(),
                t.magic(),t.fire(),t.lightning(),t.environmental(),t.bypassArmor(),t.bypassEffects(),t.bypassEnchantments(),
                t.bypassInvulnerability(),t.specialKill(),k.attackerEntity(),k.directEntity(),k.weaponItem(),o.maxHealth(),
                o.attackDamage(),o.explosionRadius(),o.explosionDistance(),o.blockRadius(),o.burstHits(),o.burstDamage(),
                o.hitsPerSecond(),e.damageScore(),e.penetrationScore(),e.powerTier(),e.penetrationTier(),e.confidence(),manual);
    }
}
