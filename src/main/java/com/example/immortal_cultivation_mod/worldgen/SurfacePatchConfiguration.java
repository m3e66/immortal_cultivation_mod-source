package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SurfacePatchConfiguration(
        BlockState state,
        int tries,
        int xzSpread,
        int ySpread,
        int minLayers,
        int maxLayers
) implements FeatureConfiguration {
    public static final Codec<SurfacePatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(SurfacePatchConfiguration::state),
            Codec.INT.optionalFieldOf("tries", 16).forGetter(SurfacePatchConfiguration::tries),
            Codec.INT.optionalFieldOf("xz_spread", 4).forGetter(SurfacePatchConfiguration::xzSpread),
            Codec.INT.optionalFieldOf("y_spread", 4).forGetter(SurfacePatchConfiguration::ySpread),
            Codec.INT.optionalFieldOf("min_layers", 1).forGetter(SurfacePatchConfiguration::minLayers),
            Codec.INT.optionalFieldOf("max_layers", 1).forGetter(SurfacePatchConfiguration::maxLayers)
    ).apply(instance, SurfacePatchConfiguration::new));
}
