package com.example.immortal_cultivation_mod.magicritual.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SpiritVeinCenterBlock extends Block implements EntityBlock {
   private static final VoxelShape SHAPE = Block.box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)16.0F);

   public SpiritVeinCenterBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
      return 1.0F;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      InteractionResult var10000;
      if (!level.isClientSide) {
         BlockEntity var7 = level.getBlockEntity(pos);
         if (var7 instanceof SpiritVeinCenterBlockEntity) {
            SpiritVeinCenterBlockEntity be = (SpiritVeinCenterBlockEntity)var7;
            if (be.releaseStoredSpiritStones()) {
               var10000 = InteractionResult.SUCCESS;
               return var10000;
            }
         }
      }

      var10000 = InteractionResult.sidedSuccess(level.isClientSide);
      return var10000;
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SpiritVeinCenterBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlockEntities.SPIRIT_VEIN_CENTER_BE.get(), SpiritVeinCenterBlockEntity::tick);
   }

   private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
      return expectedType == type ? (BlockEntityTicker<A>)ticker : null;
   }
}
