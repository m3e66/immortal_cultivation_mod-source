package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FlowingGoldSandBlock extends SnowLayerBlock {
    public FlowingGoldSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        int layers = state.getValue(LAYERS);

        if (context.getItemInHand().is(this.asItem()) && layers < 8) {
            return context.replacingClickedOnBlock()
                    ? context.getClickedFace().getAxis().isVertical()
                    : true;
        }

        return layers == 1;
    }
}