package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Stable, server-safe descriptions shared by flame vessels, acquisition and forging. */
public enum FlameKind {
    MORTAL("normal_fire", 100, 45, 0, 4000, 10, 0xFF8C3A),
    // Foundation weapons need 4500, Golden Core 6500, and Nascent Soul 8500.
    // Later-stage flames add headroom without reaching the next realm's target.
    CRIMSON("crimson_fire", 320, 78, 0, 5000, 20, 0xFF5A32),
    EARTH("earth_fire", 280, 90, 0, 6000, 18, 0xD78B3C),
    WOOD("wood_fire", 240, 92, 0, 7000, 16, 0x68DC74),
    SOLAR("solar_fire", 560, 85, 0, 7500, 40, 0xFFD45A),
    NETHER("nether_fire", 420, 70, 0, 8000, 30, 0xA25DDD),
    THUNDER("thunder_fire", 500, 65, 0, 9000, 35, 0x7F95FF),
    DRAGON("dragon_fire", 650, 75, 0, 10000, 45, 0xFF492A),
    HEAVENLY("heavenly_fire", 800, 95, 0, 12000, 55, 0xBF72FF);

    private final String id;
    private final int power, stability, minimum, maximum, qiCost, color;

    FlameKind(String id, int power, int stability, int minimum, int maximum, int qiCost, int color) {
        this.id = id; this.power = power; this.stability = stability;
        this.minimum = minimum; this.maximum = maximum; this.qiCost = qiCost; this.color = color;
    }

    public String id() { return id; }
    public int power() { return power; }
    public int stability() { return stability; }
    public int minTemperature() { return minimum; }
    public int maxTemperature() { return maximum; }
    public int qiCost() { return qiCost; }
    public int color() { return color; }
    public boolean special() { return this != MORTAL; }

    /** Combat potency follows the flame's forging grade, independent of its owner or target. */
    public String damageCultivationLevel() {
        return switch (this) {
            case MORTAL -> CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY;
            case CRIMSON -> CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY;
            case EARTH -> CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE;
            case WOOD -> CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
            case SOLAR -> CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID;
            case NETHER -> CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE;
            case THUNDER -> CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY;
            case DRAGON -> CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_MID;
            case HEAVENLY -> CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE;
        };
    }

    public float shotDamage() {
        return CultivationLevels.getLevelDef(damageCultivationLevel()).maxHp() * .15F;
    }

    public float contactDamagePerSecond() {
        return CultivationLevels.getLevelDef(damageCultivationLevel()).maxHp() * .05F;
    }

    /** Full-charge terrain radius, determined by the flame's cultivation grade. */
    public int destructionRadius() {
        return switch (CultivationLevels.getRealmIndex(damageCultivationLevel())) {
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 60;
            default -> 3;
        };
    }

    /** Shared heat capacity for recipe eligibility and the actual furnace source. */
    public static int temperatureCeiling(String fireType) {
        if ("ling_fire".equals(fireType)) return 4000;
        if ("zhen_fire".equals(fireType)) return 10000;
        return fromId(fireType).maxTemperature();
    }

    /** Stable item-model values; persisted flame IDs remain independent of enum order. */
    public int modelIndex() {
        return switch (this) {
            case MORTAL -> 0;
            case CRIMSON -> 1;
            case EARTH -> 2;
            case WOOD -> 3;
            case SOLAR -> 4;
            case NETHER -> 5;
            case THUNDER -> 6;
            case DRAGON -> 7;
            case HEAVENLY -> 8;
        };
    }
    public Component displayName() { return Component.translatable(key()); }
    public Component affinity() { return Component.translatable(key() + ".affinity"); }
    public Component maxGrade() { return Component.translatable(key() + ".max_grade"); }
    private String key() { return "flame." + ImmortalCultivationMod.MODID + "." + id; }

    public static FlameKind fromId(String id) {
        if (id != null) for (FlameKind kind : values()) if (kind.id.equals(id)) return kind;
        return MORTAL;
    }

    public Block block() {
        return switch (this) {
            case MORTAL -> Blocks.FIRE;
            case CRIMSON -> ModBlocks.CRIMSON_FIRE.get();
            case EARTH -> ModBlocks.EARTH_FIRE.get();
            case WOOD -> ModBlocks.WOOD_FIRE.get();
            case SOLAR -> ModBlocks.SOLAR_FIRE.get();
            case NETHER -> ModBlocks.NETHER_FIRE.get();
            case THUNDER -> ModBlocks.THUNDER_FIRE.get();
            case DRAGON -> ModBlocks.DRAGON_FIRE.get();
            case HEAVENLY -> ModBlocks.HEAVENLY_FIRE.get();
        };
    }
}
