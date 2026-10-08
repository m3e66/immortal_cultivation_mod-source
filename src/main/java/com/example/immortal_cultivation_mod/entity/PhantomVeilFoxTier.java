package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;

/** Realm-specific combat and presentation values shared by every Phantom Veil Fox variant. */
public enum PhantomVeilFoxTier {
    ZHUJI(
            CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
            1_200.0D, 90.0D, 12.0D, 3.0D, 0.48D, 0.12D,
            0.18F, 3, 2, 1, 220, 0.78F),
    JINDAN(
            CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
            4_500.0D, 250.0D, 22.0D, 6.0D, 0.55D, 0.24D,
            0.45F, 4, 4, 2, 650, 0.90F),
    YUANYING(
            CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_MID,
            15_000.0D, 520.0D, 32.0D, 8.0D, 0.62D, 0.35D,
            1.0F, 5, 6, 2, 1_800, 1.0F);

    private final String cultivationRealm;
    private final double maxHealth;
    private final double attackDamage;
    private final double armor;
    private final double armorToughness;
    private final double movementSpeed;
    private final double knockbackResistance;
    private final float techniqueDamageScale;
    private final int maxMasks;
    private final int sevenAspectsClones;
    private final int exchangeClones;
    private final int experienceReward;
    private final float renderScale;

    PhantomVeilFoxTier(String cultivationRealm,
                       double maxHealth,
                       double attackDamage,
                       double armor,
                       double armorToughness,
                       double movementSpeed,
                       double knockbackResistance,
                       float techniqueDamageScale,
                       int maxMasks,
                       int sevenAspectsClones,
                       int exchangeClones,
                       int experienceReward,
                       float renderScale) {
        this.cultivationRealm = cultivationRealm;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.armor = armor;
        this.armorToughness = armorToughness;
        this.movementSpeed = movementSpeed;
        this.knockbackResistance = knockbackResistance;
        this.techniqueDamageScale = techniqueDamageScale;
        this.maxMasks = maxMasks;
        this.sevenAspectsClones = sevenAspectsClones;
        this.exchangeClones = exchangeClones;
        this.experienceReward = experienceReward;
        this.renderScale = renderScale;
    }

    public String cultivationRealm() {
        return cultivationRealm;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public double armor() {
        return armor;
    }

    public double armorToughness() {
        return armorToughness;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public double knockbackResistance() {
        return knockbackResistance;
    }

    public float scaleTechniqueDamage(float yuanyingDamage) {
        return yuanyingDamage * techniqueDamageScale;
    }

    public int maxMasks() {
        return maxMasks;
    }

    public int sevenAspectsClones() {
        return sevenAspectsClones;
    }

    public int exchangeClones() {
        return exchangeClones;
    }

    public int experienceReward() {
        return experienceReward;
    }

    public float renderScale() {
        return renderScale;
    }
}
