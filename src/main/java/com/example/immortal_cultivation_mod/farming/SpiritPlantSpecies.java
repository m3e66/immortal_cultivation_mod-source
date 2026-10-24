package com.example.immortal_cultivation_mod.farming;

import java.util.Arrays;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Existing herb IDs remain the alchemy ingredients; only seeds and crops are new. */
public enum SpiritPlantSpecies {
    QINGLING("qingling_grass", 3, 4, "common", .65, 0x80E5A3),
    CHIYAN("chiyan_flower", 5, 7, "uncommon", .35, 0xFF823D),
    NINGLU("ninglu_zhi", 5, 7, "uncommon", .35, 0xB2E9FF),
    ZIYUN("ziyun_vine", 6, 9, "uncommon", .35, 0xB58BEB),
    XUANBING("xuanbing_lotus", 8, 12, "rare", .12, 0x90D9FF),
    SHIHUN("shihun_grass", 7, 10, "rare", .12, 0x633D8B),
    FUGU("fugu_tai", 5, 8, "uncommon", .35, 0x8EAE65),
    HUANXIA("huanxia_si", 10, 14, "rare", .12, 0xEAB4E9);

    public final String id, rarity;
    public final int minDays, maxDays, color;
    public final double seedChance;
    SpiritPlantSpecies(String id, int minDays, int maxDays, String rarity, double seedChance, int color) {
        this.id=id; this.minDays=minDays; this.maxDays=maxDays; this.rarity=rarity;
        this.seedChance=seedChance; this.color=color;
    }
    public double growthTicks() { return (minDays+maxDays)*.5*24000; }
    public Item herb() { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",id)); }
    public static SpiritPlantSpecies find(String id) {
        return Arrays.stream(values()).filter(s->s.id.equals(id)).findFirst().orElse(null);
    }
}
