package com.example.immortal_cultivation_mod.farming;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SpiritFarmlandBlock extends FarmBlock {
    public static final MapCodec<FarmBlock> CODEC=simpleCodec(SpiritFarmlandBlock::new);
    public SpiritFarmlandBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(MOISTURE,7));}
    @Override public MapCodec<FarmBlock> codec(){return CODEC;}
    @Override protected boolean isRandomlyTicking(BlockState state){return false;}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(level.getBlockState(pos.above()).isAir())SpiritPlantParticles.emit(SpiritPlantSpecies.QINGLING,level,pos,random,false,true);
    }
}
