package com.example.immortal_cultivation_mod.farming;

import com.example.immortal_cultivation_mod.block.CultivationVineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Preserve all existing wild vine faces and placement rules; farmed forms need only farmland. */
public final class WildSpiritVineBlock extends CultivationVineBlock {
    public WildSpiritVineBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(WildSpiritPlantBlock.NATURAL,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(WildSpiritPlantBlock.NATURAL);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var s=super.getStateForPlacement(context);return s==null?null:s.setValue(WildSpiritPlantBlock.NATURAL,false);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return SpiritFarming.harvest(state,level,pos,player,false);}
}
