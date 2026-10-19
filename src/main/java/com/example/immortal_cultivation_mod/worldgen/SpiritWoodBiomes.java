package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/** A narrow portion of temperate forest climates forms natural spiritwood groves. */
public final class SpiritWoodBiomes {
    public static final ResourceKey<Biome> GROVE = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "spiritwood_grove"));
    public static final TagKey<Biome> NATURAL_TREES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "has_feature/lingmu"));
    private static final long GROVE_MIN = Climate.quantizeCoord(.10F);
    private static final long GROVE_MAX = Climate.quantizeCoord(.24F);

    private SpiritWoodBiomes() {}

    public static boolean allowsNaturalTrees(LevelReader level, BlockPos position) {
        return level.getBiome(position).is(NATURAL_TREES);
    }

    public static boolean groveClimate(Climate.TargetPoint point) {
        return point.weirdness() >= GROVE_MIN && point.weirdness() <= GROVE_MAX;
    }

    /** Registry-time lookup is cached; no registry searches occur per sampled block. */
    public interface GrovePreset {
        Holder<Biome> immortalCultivation$grove();
    }
}
