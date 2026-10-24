package com.example.immortal_cultivation_mod.farming;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Five visible stages, with eight saved substeps between stages. No per-crop block entity. */
public final class SpiritCropBlock extends CropBlock {
    public static final IntegerProperty AGE=BlockStateProperties.AGE_4;
    public static final IntegerProperty PROGRESS=IntegerProperty.create("progress",0,7);
    public static final BooleanProperty RICH=BooleanProperty.create("rich");
    public static final int TOTAL_STEPS=32;
    public final SpiritPlantSpecies species;
    public SpiritCropBlock(SpiritPlantSpecies species,Properties properties) {
        super(properties);this.species=species;
        registerDefaultState(defaultBlockState().setValue(PROGRESS,0).setValue(RICH,false));
    }
    @Override public MapCodec<? extends CropBlock> codec(){return simpleCodec(p->new SpiritCropBlock(species,p));}
    @Override protected IntegerProperty getAgeProperty(){return AGE;}
    @Override public int getMaxAge(){return 4;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE,PROGRESS,RICH);}
    @Override protected ItemLike getBaseSeedId(){return SpiritFarming.SEEDS.get(species).get();}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).getBlock() instanceof FarmBlock;}
    @Override protected boolean isRandomlyTicking(BlockState state){return true;}
    public static int progress(BlockState state){return state.getValue(AGE)*8+state.getValue(PROGRESS);}
    public static BlockState withProgress(BlockState state,int progress){
        int p=Math.clamp(progress,0,TOTAL_STEPS);
        return state.setValue(AGE,p/8).setValue(PROGRESS,p%8);
    }
    /** Vanilla default randomTickSpeed=3 selects a particular block once per 4096/3 ticks on average. */
    public double stepsPerRandomTick(double multiplier){return TOTAL_STEPS*(4096.0/3)/species.growthTicks()*multiplier;}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!state.canSurvive(level,pos))return;
        double speed=SpiritPlantQi.speed(SpiritPlantQi.amount(level,pos));
        var next=state.setValue(RICH,speed>=1.5);
        if(!isMaxAge(state)) {
            double expected=stepsPerRandomTick(speed);
            int steps=(int)expected+(random.nextDouble()<expected-(int)expected?1:0);
            next=withProgress(next,progress(state)+steps);
        }
        if(!next.equals(state))level.setBlock(pos,next,Block.UPDATE_CLIENTS);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        return isMaxAge(state)?SpiritFarming.harvest(state,level,pos,player,true):InteractionResult.PASS;
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state){return false;}
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state){return false;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state){}
    @Override public void growCrops(Level level,BlockPos pos,BlockState state){}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        int age=state.getValue(AGE);double height=species==SpiritPlantSpecies.FUGU?2+age:3+age*3;
        return box(4-age*.5,0,4-age*.5,12+age*.5,height,12+age*.5);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        SpiritPlantParticles.emit(species,level,pos,random,state.getValue(AGE)==4,state.getValue(RICH));
    }
}
