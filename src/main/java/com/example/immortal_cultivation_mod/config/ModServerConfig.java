package com.example.immortal_cultivation_mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PILL_EXPLOSION_BASE_DAMAGE;
    public static final ModConfigSpec.BooleanValue CULTIVATOR_NATURAL_SPAWNING;
    public static final ModConfigSpec.IntValue CULTIVATOR_LOCAL_CAP;
    public static final ModConfigSpec.IntValue SPIRIT_FARMLAND_QI;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> SPIRIT_PLANT_QI_THRESHOLDS;
    public static final ModConfigSpec.BooleanValue ZHENPAN_QI_FIRST;
    public static final ModConfigSpec.BooleanValue SHOW_QI_USE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_DROUGHT_ZOMBIE_DOMAIN;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> LIFE_CARD_DICE_COUNT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> LIFE_CARD_DICE_SIDES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("cultivatorSpawning");
        CULTIVATOR_NATURAL_SPAWNING = builder.comment("Allow ambient village residents, cultivators and wandering merchants, including small parties. Eggs, commands and fixed structure NPCs are unaffected.")
                .define("enabled", true);
        CULTIVATOR_LOCAL_CAP = builder.comment("Stop ambient NPC spawning when this many living mod residents/cultivators/merchants already occupy a horizontal 128-block radius, across all heights. Every party member counts, including persistent and previously spawned NPCs. Zero disables natural spawning; existing NPCs are not removed.")
                .defineInRange("localCap", 4, 0, 32);
        builder.pop();
        builder.push("alchemy");
        PILL_EXPLOSION_BASE_DAMAGE = builder.comment("Pill explosion maximum HP damage per block of radius; linear falloff to zero at the edge.")
                .defineInRange("pillExplosionBaseDamage", 10D, 0D, 1000000D);
        builder.pop();
        builder.push("spiritFarming");
        SPIRIT_FARMLAND_QI=builder.comment("Local Qi supplied once to the crop rooted in spirit farmland. A natural spirit vein contributes 25 ambient Qi.")
                .defineInRange("farmlandQi",25,0,10000);
        SPIRIT_PLANT_QI_THRESHOLDS=builder.comment("Four strictly increasing Qi thresholds for 0.5x / 0.75x / 1x / 1.5x / 2x crop growth. Normal growth durations assume 25-49 Qi and vanilla randomTickSpeed=3.")
                .define("qiThresholds",java.util.List.of(10,25,50,75),value->{
                    if(!(value instanceof java.util.List<?> list)||list.size()!=4)return false;
                    int previous=-1;
                    for(Object entry:list){if(!(entry instanceof Integer n)||n<=previous||n>100000)return false;previous=n;}
                    return true;
                });
        builder.pop();
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
