package com.example.immortal_cultivation_mod.farming;

import com.example.immortal_cultivation_mod.block.CultivationPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Old worldgen defaults to natural=true; placed herb items cannot be harvested for extra herbs/seeds. */
public final class WildSpiritPlantBlock extends CultivationPlantBlock {
    public static final BooleanProperty NATURAL=BooleanProperty.create("natural");
    public WildSpiritPlantBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(NATURAL,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(NATURAL);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(NATURAL,false);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return SpiritFarming.harvest(state,level,pos,player,false);}
}
