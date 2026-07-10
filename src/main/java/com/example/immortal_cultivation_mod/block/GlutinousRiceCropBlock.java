package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GlutinousRiceCropBlock extends CropBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            box(3.0D, 0.0D, 3.0D, 13.0D, 3.0D, 13.0D),
            box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D),
            box(3.0D, 0.0D, 3.0D, 13.0D, 7.0D, 13.0D),
            box(3.0D, 0.0D, 3.0D, 13.0D, 9.0D, 13.0D),
            box(3.0D, 0.0D, 3.0D, 13.0D, 11.0D, 13.0D),
            box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D),
            box(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D),
            box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)
    };

    public GlutinousRiceCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.NUOMI_SEED.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }
}
