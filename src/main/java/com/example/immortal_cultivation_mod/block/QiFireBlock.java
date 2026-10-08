package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.flame.SpiritFlameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The original Qi fire gameplay with the shared animated flame presentation. */
public final class QiFireBlock extends LingFireBlock implements EntityBlock {
    public QiFireBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritFlameBlockEntity(pos, state);
    }

    @Override protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Visible renderers schedule the same bounded accents used by other spirit flames.
    }
}
