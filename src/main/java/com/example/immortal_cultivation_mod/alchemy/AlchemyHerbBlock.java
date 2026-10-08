package com.example.immortal_cultivation_mod.alchemy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Harvestable wild medicinal herbs, with vanilla soil support and break behavior. */
public final class AlchemyHerbBlock extends BushBlock {
    public static final MapCodec<AlchemyHerbBlock> CODEC=simpleCodec(AlchemyHerbBlock::new);
    private static final VoxelShape SHAPE=box(3,0,3,13,14,13);
    public AlchemyHerbBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BushBlock> codec(){return CODEC;}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos){
        return super.mayPlaceOn(state,level,pos)||state.is(Blocks.MOSS_BLOCK);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
}
