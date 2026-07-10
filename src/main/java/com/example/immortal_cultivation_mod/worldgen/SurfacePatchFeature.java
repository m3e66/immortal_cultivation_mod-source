package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class SurfacePatchFeature extends Feature<SurfacePatchConfiguration> {
    public SurfacePatchFeature(Codec<SurfacePatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SurfacePatchConfiguration> context) {
        SurfacePatchConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int placed = 0;

        for (int i = 0; i < Math.max(1, config.tries()); i++) {
            BlockPos probe = origin.offset(
                    random.nextInt(config.xzSpread() * 2 + 1) - config.xzSpread(),
                    random.nextInt(config.ySpread() * 2 + 1) - config.ySpread(),
                    random.nextInt(config.xzSpread() * 2 + 1) - config.xzSpread());
            BlockPos target = findPlacementPos(level, probe, Math.max(6, config.ySpread() + 8));
            if (target != null && placeState(level, target, config, random)) {
                placed++;
            }
        }

        return placed > 0;
    }

    private static BlockPos findPlacementPos(WorldGenLevel level, BlockPos start, int scan) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i <= scan && cursor.getY() > level.getMinBuildHeight() + 1; i++) {
            if (canPlaceAt(level, cursor)) {
                return cursor.immutable();
            }
            cursor.move(0, -1, 0);
        }

        cursor.set(start);
        for (int i = 0; i <= scan && cursor.getY() < level.getMaxBuildHeight() - 1; i++) {
            if (canPlaceAt(level, cursor)) {
                return cursor.immutable();
            }
            cursor.move(0, 1, 0);
        }
        return null;
    }

    private static boolean canPlaceAt(WorldGenLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        return (current.isAir() || current.canBeReplaced())
                && !below.isAir()
                && below.getFluidState().isEmpty()
                && below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    private static boolean placeState(WorldGenLevel level, BlockPos pos, SurfacePatchConfiguration config, RandomSource random) {
        BlockState state = config.state();
        if (state.hasProperty(SnowLayerBlock.LAYERS)) {
            return placeLayered(level, pos, state, config, random);
        }
        if (!canPlaceAt(level, pos)) {
            return false;
        }
        return level.setBlock(pos, state, 3);
    }

    private static boolean placeLayered(WorldGenLevel level, BlockPos pos, BlockState state, SurfacePatchConfiguration config, RandomSource random) {
        BlockState current = level.getBlockState(pos);
        int minLayers = Math.max(1, Math.min(8, config.minLayers()));
        int maxLayers = Math.max(minLayers, Math.min(8, config.maxLayers()));
        int add = minLayers + random.nextInt(maxLayers - minLayers + 1);

        if (current.is(state.getBlock()) && current.hasProperty(SnowLayerBlock.LAYERS)) {
            int layers = current.getValue(SnowLayerBlock.LAYERS);
            if (layers >= 8) {
                return false;
            }
            return level.setBlock(pos, current.setValue(SnowLayerBlock.LAYERS, Math.min(8, layers + add)), 3);
        }

        if (!canPlaceAt(level, pos)) {
            return false;
        }
        return level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, add), 3);
    }
}
