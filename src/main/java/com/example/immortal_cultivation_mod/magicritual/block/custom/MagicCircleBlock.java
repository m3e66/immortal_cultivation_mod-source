package com.example.immortal_cultivation_mod.magicritual.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MagicCircleBlock extends CarpetBlock implements EntityBlock {
   private static final VoxelShape SHAPE = Block.box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)1.0F, (double)16.0F);
   public static final EnumProperty<CircleType> CIRCLE_TYPE = EnumProperty.create("circle_type", CircleType.class);

   public MagicCircleBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(CIRCLE_TYPE, CircleType.ATTACK));
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
      return false;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder builder) {
      builder.add(new Property[]{CIRCLE_TYPE});
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new MagicCircleBlockEntity(pos, state);
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, level, pos, oldState, movedByPiston);
      MagicCenterBlockEntity.invalidateNearbyBlankCenters(level, pos);
   }

   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      super.onRemove(state, level, pos, newState, movedByPiston);
      MagicCenterBlockEntity.invalidateNearbyBlankCenters(level, pos);
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? (lvl, pos, st, be) -> {
         if (be instanceof MagicCircleBlockEntity circle) {
            circle.clientTick(lvl, pos, st);
         }

      } : null;
   }
}
