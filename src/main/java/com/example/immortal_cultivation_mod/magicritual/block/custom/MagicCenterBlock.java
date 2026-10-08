package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.TeleportCodeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.TeleportCenterBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MagicCenterBlock extends Block implements EntityBlock {
   public static final IntegerProperty RADIUS = IntegerProperty.create("radius", 3, 303);
   private static final VoxelShape SHAPE = Block.box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)16.0F);

   public MagicCenterBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(RADIUS, 5));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{RADIUS});
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
      if (!level.isClientSide) {
         BlockEntity var7 = level.getBlockEntity(pos);
         if (var7 instanceof MagicCenterBlockEntity) {
            MagicCenterBlockEntity be = (MagicCenterBlockEntity)var7;
            if (be.isStructureTrap()) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.dead_ruin_guarded"), true);
               return InteractionResult.SUCCESS;
            }

            if (be.isTemporary()) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.temporary_locked"), true);
               return InteractionResult.SUCCESS;
            }

            if (player.isShiftKeyDown() && player instanceof ServerPlayer) {
               ServerPlayer serverPlayer = (ServerPlayer)player;
               if (MagicCenterTypeRegistry.TELEPORT.equals(be.getCenterTypeId())) {
                  TeleportCenterBehavior.beginChannel(be, serverPlayer);
                  return InteractionResult.SUCCESS;
               }
            }

            player.openMenu(be, pos);
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && placer instanceof Player player) {
         BlockEntity var8 = level.getBlockEntity(pos);
         if (var8 instanceof MagicCenterBlockEntity center) {
            center.setOwnerUuid(player.getUUID());
            if (player instanceof ServerPlayer serverPlayer) {
               DaoGuidanceSystem.record(serverPlayer, "place_center");
            }
         }
      }

   }

   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
      BlockEntity var8 = level.getBlockEntity(pos);
      if (var8 instanceof MagicCenterBlockEntity center) {
         if (center.isStructureTrap()) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.dead_ruin_guarded"), true);
            return false;
         }

         if (center.isTemporary()) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.temporary_locked"), true);
            return false;
         }

         if (level instanceof ServerLevel && !CenterTargetPolicy.canManageControllers(center, player)) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.not_authorized"), true);
            return false;
         }

         if (!center.isBlank()) {
            if (level instanceof ServerLevel) {
               ServerLevel serverLevel = (ServerLevel)level;
               center.breakTransformation(serverLevel);
            }

            return false;
         }
      }

      return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
   }

   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.isClientSide && !state.is(newState.getBlock())) {
         BlockEntity var7 = level.getBlockEntity(pos);
         if (var7 instanceof MagicCenterBlockEntity) {
            MagicCenterBlockEntity center = (MagicCenterBlockEntity)var7;
            TeleportCodeRegistry.unregister(center);
            center.dropContents();
         }
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new MagicCenterBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlockEntities.MAGIC_CENTER_BE.get(), MagicCenterBlockEntity::tick);
   }

   private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
      return expectedType == type ? (BlockEntityTicker<A>)ticker : null;
   }
}
