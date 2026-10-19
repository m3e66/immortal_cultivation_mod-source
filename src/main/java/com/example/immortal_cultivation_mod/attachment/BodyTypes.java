package com.example.immortal_cultivation_mod.attachment;

import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class BodyTypes {
    public static final String MORTAL_BODY = "\u51e1\u4f53";
    public static final String VOID_BODY = "\u865a\u7a7a\u4f53";
    public static final String OUTER_DEMON_BODY = "\u5929\u5916\u4e4b\u9b54";

    public static final String CRIMSON_FLAME = "赤炎灵体";
    public static final String PROFOUND_ICE = "玄冰灵体";
    public static final String VIOLET_THUNDER = "紫霄雷体";
    public static final String EVERGREEN = "青木长生体";
    public static final String PROFOUND_EARTH = "厚土玄体";
    public static final String GENG_METAL = "庚金剑体";
    public static final String VAST_SEA = "沧海灵体";
    public static final String WINDCHASER = "逐风灵体";
    public static final String INNATE_DAO = "先天道体";
    public static final String IMMACULATE = "无垢灵体";
    public static final String NINE_ORIFICE = "九窍玲珑体";
    public static final String MYRIAD_POISON = "万毒之体";
    public static final String BLOOD_FIEND = "血煞魔体";
    public static final String GOLDEN_BODY = "不灭金身";
    public static final String SPIRIT_DEVOURING = "吞灵之体";
    public static final String ANCIENT_SACRED = "荒古圣体";
    public static final String SUPREME_YIN = "太阴神体";
    public static final String SOLAR_DIVINE = "太阳神体";

    public enum Rarity { MORTAL, ADVANCED, RARE, LEGENDARY }
    public record Definition(String id, String key, Rarity rarity) {}

    public static final List<Definition> DEFINITIONS = List.of(
        new Definition(MORTAL_BODY, "mortal", Rarity.MORTAL),
        new Definition(CRIMSON_FLAME, "crimson_flame", Rarity.ADVANCED),
        new Definition(PROFOUND_ICE, "profound_ice", Rarity.ADVANCED),
        new Definition(VIOLET_THUNDER, "violet_thunder", Rarity.ADVANCED),
        new Definition(EVERGREEN, "evergreen", Rarity.ADVANCED),
        new Definition(PROFOUND_EARTH, "profound_earth", Rarity.ADVANCED),
        new Definition(GENG_METAL, "geng_metal", Rarity.ADVANCED),
        new Definition(VAST_SEA, "vast_sea", Rarity.ADVANCED),
        new Definition(WINDCHASER, "windchaser", Rarity.ADVANCED),
        new Definition(INNATE_DAO, "innate_dao", Rarity.RARE),
        new Definition(IMMACULATE, "immaculate", Rarity.RARE),
        new Definition(NINE_ORIFICE, "nine_orifice", Rarity.RARE),
        new Definition(MYRIAD_POISON, "myriad_poison", Rarity.RARE),
        new Definition(BLOOD_FIEND, "blood_fiend", Rarity.RARE),
        new Definition(GOLDEN_BODY, "golden_body", Rarity.RARE),
        new Definition(SPIRIT_DEVOURING, "spirit_devouring", Rarity.RARE),
        new Definition(VOID_BODY, "void", Rarity.RARE),
        new Definition(OUTER_DEMON_BODY, "outer_demon", Rarity.RARE),
        new Definition(ANCIENT_SACRED, "ancient_sacred", Rarity.LEGENDARY),
        new Definition(SUPREME_YIN, "supreme_yin", Rarity.LEGENDARY),
        new Definition(SOLAR_DIVINE, "solar_divine", Rarity.LEGENDARY)
    );
    public static final List<String> VALUES = DEFINITIONS.stream().map(Definition::id).toList();
    private static final java.util.Map<String, Definition> BY_ID = DEFINITIONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Definition::id, d -> d));
    private static final List<String> ADVANCED = group(Rarity.ADVANCED);
    private static final List<String> RARE = group(Rarity.RARE);
    private static final List<String> LEGENDARY = group(Rarity.LEGENDARY);

    private BodyTypes() {}

    private static List<String> group(Rarity rarity) {
        return DEFINITIONS.stream().filter(d -> d.rarity() == rarity).map(Definition::id).toList();
    }

    /** 90% mortal; 8% advanced (1% each); 1.8% rare (0.2% each); 0.2% legendary (~0.0667% each). */
    public static String random(RandomSource random) {
        return bodyForRoll(random.nextInt(15000));
    }

    static String bodyForRoll(int roll) {
        if (roll < 0 || roll >= 15000) throw new IllegalArgumentException("Body roll outside weighted pool");
        if (roll < 13500) return MORTAL_BODY;
        if (roll < 14700) return ADVANCED.get((roll - 13500) / 150);
        if (roll < 14970) return RARE.get((roll - 14700) / 30);
        return LEGENDARY.get((roll - 14970) / 10);
    }

    public static String sanitize(String bodyType) {
        bodyType = normalize(bodyType);
        return VALUES.contains(bodyType) ? bodyType : MORTAL_BODY;
    }

    public static String normalize(String bodyType) {
        // Migrate retired elemental bodies without rerolling a saved character.
        if (bodyType != null) switch (bodyType) {
            case "金灵体", "木灵体", "水灵体", "火灵体", "土灵体", "雷灵体",
                    "风灵体", "冰灵体", "阳灵体", "阴灵体", "光灵体", "暗灵体" -> {
                return MORTAL_BODY;
            }
        }
        return bodyType;
    }

    public static Definition definition(String bodyType) {
        return BY_ID.get(sanitize(bodyType));
    }

    public static Component displayName(String bodyType) {
        return Component.translatable("cultivation.immortal_cultivation_mod.body." + definition(bodyType).key());
    }

    public static Component description(String bodyType) {
        return Component.translatable("cultivation.immortal_cultivation_mod.body." + definition(bodyType).key() + ".description");
    }

    public static Component rarityName(String bodyType) {
        return Component.translatable("cultivation.immortal_cultivation_mod.body.rarity."
                + definition(bodyType).rarity().name().toLowerCase(java.util.Locale.ROOT));
    }

    public static String next(String current, int direction) {
        current = normalize(current);
        int index = VALUES.indexOf(current);
        if (index < 0) {
            index = 0;
        }
        return VALUES.get(Math.floorMod(index + (direction < 0 ? -1 : 1), VALUES.size()));
    }
}
