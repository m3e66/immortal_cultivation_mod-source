package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/** Triples horizontal island dimensions while preserving vanilla world-seed wiring. */
public final class ScaledVoidIslands implements DensityFunction.SimpleFunction {
    public static final int HORIZONTAL_SCALE = 3;
    public static final MapCodec<ScaledVoidIslands> MAP_CODEC = MapCodec.unit(
            new ScaledVoidIslands(DensityFunctions.endIslands(0)));
    private static final KeyDispatchDataCodec<ScaledVoidIslands> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);
    private final DensityFunction islands;

    private ScaledVoidIslands(DensityFunction islands) {
        this.islands = islands;
    }

    @Override public double compute(FunctionContext context) {
        return islands.compute(new SinglePointContext(context.blockX() / HORIZONTAL_SCALE,
                context.blockY(), context.blockZ() / HORIZONTAL_SCALE));
    }

    @Override public DensityFunction mapAll(Visitor visitor) {
        // Visit the vanilla terminal so RandomState supplies this world's seed.
        // Cache/interpolation markers stay outside the coordinate transform.
        return visitor.apply(new ScaledVoidIslands(islands.mapAll(visitor)));
    }

    @Override public double minValue() { return islands.minValue(); }
    @Override public double maxValue() { return islands.maxValue(); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
