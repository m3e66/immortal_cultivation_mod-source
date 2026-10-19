package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;

/** Ability and presentation profiles; health and damage follow the shared cultivation realm definitions. */
public enum PhantomVeilFoxTier {
    ZHUJI(
            2,
            12.0D, 3.0D, 0.48D, 0.12D,
            3, 2, 1, 220, 0.78F),
    JINDAN(
            3,
            22.0D, 6.0D, 0.55D, 0.24D,
            4, 4, 2, 650, 0.90F),
    YUANYING(
            4,
            32.0D, 8.0D, 0.62D, 0.35D,
            5, 6, 2, 1_800, 1.0F);

    private final int realm;
    private final double armor;
    private final double armorToughness;
    private final double movementSpeed;
    private final double knockbackResistance;
    private final int maxMasks;
    private final int sevenAspectsClones;
    private final int exchangeClones;
    private final int experienceReward;
    private final float renderScale;

    PhantomVeilFoxTier(int realm,
                       double armor,
                       double armorToughness,
                       double movementSpeed,
                       double knockbackResistance,
                       int maxMasks,
                       int sevenAspectsClones,
                       int exchangeClones,
                       int experienceReward,
                       float renderScale) {
        this.realm = realm;
        this.armor = armor;
        this.armorToughness = armorToughness;
        this.movementSpeed = movementSpeed;
        this.knockbackResistance = knockbackResistance;
        this.maxMasks = maxMasks;
        this.sevenAspectsClones = sevenAspectsClones;
        this.exchangeClones = exchangeClones;
        this.experienceReward = experienceReward;
        this.renderScale = renderScale;
    }

    public String cultivationRealm() {
        return BeastRealms.level(realm);
    }

    public double maxHealth() {
        return CultivationLevels.getLevelDef(cultivationRealm()).maxHp();
    }

    public double attackDamage() {
        return 520 * BeastRealms.powerScale(realm, 4);
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
        return (float)(yuanyingDamage * BeastRealms.powerScale(realm, 4));
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
