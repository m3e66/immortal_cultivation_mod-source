package com.example.immortal_cultivation_mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ZHENPAN_QI_FIRST;
    public static final ModConfigSpec.BooleanValue SHOW_QI_USE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_DROUGHT_ZOMBIE_DOMAIN;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> LIFE_CARD_DICE_COUNT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> LIFE_CARD_DICE_SIDES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("magicRitual");
        ZHENPAN_QI_FIRST = builder
                .comment("When true, temporary Formation Plate centers spend player qi before inventory spirit stones. The default spends spirit stones first.")
                .define("formationPlateQiFirst", false);
        builder.pop();
        builder.push("visuals");
        SHOW_QI_USE_PARTICLES = builder
                .comment("Show the automatic cultivation-method particles around a player whenever that player spends qi. Spell-specific VFX are unaffected.")
                .define("showQiUseParticles", true);
        builder.pop();
        builder.push("undead");
        ENABLE_DROUGHT_ZOMBIE_DOMAIN = builder
                .comment("Allow Hanba drought terrain drying and the Scorched Earth domain. Players can additionally disable their own drought domain in the in-game settings screen.")
                .define("enableDroughtZombieDomain", true);
        builder.pop();
        builder.push("lifeCardBattle");
        LIFE_CARD_DICE_COUNT=builder.comment("Dice count by major realm, starting with Mortal. Each card's numerical variation stays within +/-5%.")
                .defineList("realmDiceCount",java.util.List.of(1,1,2,3,4,5,6,7,8,9),value->value instanceof Integer n&&n>=1&&n<=12);
        LIFE_CARD_DICE_SIDES=builder.comment("Dice sides by major realm: Mortal, Qi Refining, Foundation, Golden Core, Nascent Soul, and onward.")
                .defineList("realmDiceSides",java.util.List.of(4,4,6,8,10,12,14,16,18,20),value->value instanceof Integer n&&n>=4&&n<=100);
        builder.pop();
        SPEC = builder.build();
    }

    private ModServerConfig() {
    }

    public static boolean zhenpanQiFirst() {
        return ZHENPAN_QI_FIRST.get();
    }

    public static boolean showQiUseParticles() {
        return SHOW_QI_USE_PARTICLES.get();
    }

    public static boolean droughtZombieDomainEnabled() {
        return ENABLE_DROUGHT_ZOMBIE_DOMAIN.get();
    }
}
