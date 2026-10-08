package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.config.ModServerConfig;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ZhenpanItem extends Item implements GeoItem {
   private static final String STORED_CENTER_TAG = "StoredMagicCenter";
   private static final String STORED_CENTERS_TAG = "StoredMagicCenters";
   private static final String TELEPORT_LINK_RECORD_TAG = "TeleportLinkRecord";
   private static final String ACTIVE_TEMPORARY_CENTERS_TAG = "ActiveTemporaryMagicCenters";
   private static final String ACTIVE_TEMPORARY_CENTER_TAG = "ActiveTemporaryMagicCenter";
   private static final String ACTIVE_TEMPORARY_EXPIRES_AT_TAG = "ExpiresAt";
   private static final String ACTIVE_TEMPORARY_DIMENSION_TAG = "Dimension";
   private static final String ACTIVE_TEMPORARY_POS_TAG = "Pos";
   private static final String ACTIVE_TEMPORARY_ID_TAG = "CenterId";
   private static final String QI_FIRST_PAYMENT_TAG = "immortal_cultivation_mod.formation_plate_qi_first";
   private static final int PORTABLE_RADIUS = 5;
   private static final int MAX_ACTIVE_TEMPORARY_TELEPORT_CENTERS = 2;
   private static final int TEMPORARY_DURATION_TICKS = 400;
   private static final int TEMPORARY_COST_MULTIPLIER = 10;
   private static final double TEMPORARY_REACH_MULTIPLIER = (double)2.0F;
   private static final RawAnimation SPIN_ANIMATION = RawAnimation.begin().thenLoop("animation.zhenpan.spin");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ZhenpanItem(Item.Properties properties) {
      super(properties);
   }

   public static void setQiFirst(Player player, boolean qiFirst) {
      if (player != null) {
         player.getPersistentData().putBoolean("immortal_cultivation_mod.formation_plate_qi_first", qiFirst);
      }

   }

   public static void copyPersistentData(Player original, Player copy) {
      if (original != null && copy != null) {
         CompoundTag source = original.getPersistentData();
         if (source.contains("immortal_cultivation_mod.formation_plate_qi_first")) {
            copy.getPersistentData().putBoolean("immortal_cultivation_mod.formation_plate_qi_first", source.getBoolean("immortal_cultivation_mod.formation_plate_qi_first"));
         }

      }
   }

   private static boolean qiFirst(Player player) {
      CompoundTag data = player.getPersistentData();
      return data.contains("immortal_cultivation_mod.formation_plate_qi_first") ? data.getBoolean("immortal_cultivation_mod.formation_plate_qi_first") : ModServerConfig.zhenpanQiFirst();
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else if (context.getLevel().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         ServerLevel level = (ServerLevel)context.getLevel();
         ItemStack zhenpan = context.getItemInHand();
         if (!player.isShiftKeyDown()) {
            BlockEntity var6 = level.getBlockEntity(context.getClickedPos());
            if (var6 instanceof MagicCenterBlockEntity) {
               MagicCenterBlockEntity clickedCenter = (MagicCenterBlockEntity)var6;
               if (clickedCenter.isTemporary()) {
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.temporary_locked"), true);
                  return InteractionResult.SUCCESS;
               }

               if (MagicCenterTypeRegistry.TELEPORT.equals(clickedCenter.getCenterTypeId())) {
                  clearTeleportLinkRecord(zhenpan);
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.use_code_menu"), true);
               }

               return InteractionResult.SUCCESS;
            }
         }

         if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(context.getClickedPos()) instanceof MagicCenterBlockEntity) {
               return packCenter(level, player, zhenpan, context.getClickedPos()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            } else if (hasStoredCenter(zhenpan)) {
               BlockPos placement = resolvePlacementPos(level, context.getClickedPos(), context.getClickedFace());
               return placeStoredCenter(level, player, zhenpan, placement) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            } else {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.no_center"), true);
               return InteractionResult.FAIL;
            }
         } else {
            BlockPos placement = resolvePlacementPos(level, context.getClickedPos(), context.getClickedFace());
            return summonTemporaryCenter(level, player, zhenpan, placement) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack zhenpan = player.getItemInHand(hand);
      if (player.isShiftKeyDown()) {
         return InteractionResultHolder.pass(zhenpan);
      } else {
         double temporaryReach = Math.max((double)1.0F, player.blockInteractionRange() * (double)2.0F);
         HitResult target = player.pick(temporaryReach, 0.0F, false);
         if (target instanceof BlockHitResult) {
            BlockHitResult hit = (BlockHitResult)target;
            if (hit.getType() == Type.BLOCK) {
               if (level.isClientSide) {
                  return InteractionResultHolder.success(zhenpan);
               }

               ServerLevel serverLevel = (ServerLevel)level;
               BlockPos placement = resolvePlacementPos(serverLevel, hit.getBlockPos(), hit.getDirection());
               boolean summoned = summonTemporaryCenter(serverLevel, player, zhenpan, placement);
               return summoned ? InteractionResultHolder.success(zhenpan) : InteractionResultHolder.fail(zhenpan);
            }
         }

         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.look_at_block"), true);
         }

         return InteractionResultHolder.fail(zhenpan);
      }
   }

   private static boolean packCenter(ServerLevel level, Player player, ItemStack zhenpan, BlockPos centerPos) {
      if (hasStoredCenter(zhenpan)) {
         player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.full"), true);
         return false;
      } else {
         BlockEntity var5 = level.getBlockEntity(centerPos);
         if (var5 instanceof MagicCenterBlockEntity) {
            MagicCenterBlockEntity center = (MagicCenterBlockEntity)var5;
            if (center.isTemporary()) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.temporary_locked"), true);
               return false;
            } else if (!CenterTargetPolicy.canManageControllers(center, player)) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.not_authorized"), true);
               return false;
            } else if (level.mayInteract(player, centerPos) && player.mayUseItemAt(centerPos, Direction.UP, zhenpan)) {
               center.setRadius(Math.min(5, center.getRadius()));
               CompoundTag portable = center.createPortableConfigurationTag(level.registryAccess());
               portable.putInt("Radius", center.getRadius());
               if (!setStoredCenter(zhenpan, portable)) {
                  return false;
               } else {
                  center.removeForPacking();
                  level.playSound((Player)null, centerPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.7F, 1.35F);
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.packed"), true);
                  return true;
               }
            } else {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.blocked"), true);
               return false;
            }
         } else {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.no_center"), true);
            return false;
         }
      }
   }

   private static boolean placeStoredCenter(ServerLevel level, Player player, ItemStack zhenpan, BlockPos placement) {
      CompoundTag portable = storedCenter(zhenpan);
      if (portable == null) {
         return false;
      } else {
         int storedRadius = MagicCenterBlockEntity.normalizeRadius(Math.min(5, portable.getInt("Radius")));
         MagicCenterBlockEntity center = createCenter(level, player, zhenpan, placement, portable, storedRadius);
         if (center == null) {
            return false;
         } else {
            center.reconcileTeleportLinkAfterPlacement();
            clearStoredCenter(zhenpan);
            level.playSound((Player)null, placement, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8F, 0.85F);
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.placed"), true);
            return true;
         }
      }
   }

   private static boolean summonTemporaryCenter(ServerLevel level, Player player, ItemStack zhenpan, BlockPos placement) {
      CompoundTag portable = storedCenter(zhenpan);
      if (portable == null) {
         player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.empty"), true);
         return false;
      } else {
         List<CompoundTag> activeTemporaryCenters = activeTemporaryCenters(zhenpan, level.getGameTime());
         boolean teleportTemporary = isTeleportPortable(portable);
         int activeLimit = teleportTemporary ? 2 : 1;
         if (activeTemporaryCenters.size() >= activeLimit) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.temporary_active"), true);
            return false;
         } else {
            MagicCenterBlockEntity existingTemporaryTeleport = null;
            if (teleportTemporary && !activeTemporaryCenters.isEmpty()) {
               existingTemporaryTeleport = resolveTemporaryTeleportCenter(level, (CompoundTag)activeTemporaryCenters.getFirst());
               if (existingTemporaryTeleport == null) {
                  activeTemporaryCenters.clear();
                  writeActiveTemporaryCenters(zhenpan, activeTemporaryCenters);
               }
            }

            MagicCenterBlockEntity center = createCenter(level, player, zhenpan, placement, portable, 5);
            if (center == null) {
               return false;
            } else {
               int durationFlagCount = center.countDurationFlags();
               long durationTicks = 400L + (long)durationFlagCount * 60L;
               int costPerMinute = Math.max(0, center.getCurrentFuelCostPerMinute());
               long originalCost = ((long)costPerMinute * 400L + 1199L) / 1200L;
               int cost = (int)Math.min(2147483647L, originalCost * 10L);
               PaymentResult payment = paySummonCost(player, cost);
               if (payment == null) {
                  center.removeWithoutDrops();
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.insufficient", new Object[]{cost}), true);
                  return false;
               } else {
                  long expiresAt = level.getGameTime() + durationTicks;
                  center.markTemporary(expiresAt);
                  if (existingTemporaryTeleport != null) {
                     existingTemporaryTeleport.unlinkTeleportPair();
                     center.unlinkTeleportPair();
                     existingTemporaryTeleport.linkTeleportPair(center);
                  }

                  activeTemporaryCenters.add(activeTemporaryCenterRecord(level, center, expiresAt));
                  writeActiveTemporaryCenters(zhenpan, activeTemporaryCenters);
                  level.playSound((Player)null, placement, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.2F);
                  player.getCooldowns().addCooldown(zhenpan.getItem(), 10);
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.summoned", new Object[]{durationTicks / 20L, payment.stoneValueSpent(), payment.qiSpent()}), true);
                  return true;
               }
            }
         }
      }
   }

   private static MagicCenterBlockEntity createCenter(ServerLevel level, Player player, ItemStack zhenpan, BlockPos placement, CompoundTag portable, int requestedRadius) {
      int radius = MagicCenterBlockEntity.normalizeRadius(Math.min(5, requestedRadius));
      BlockState state = (BlockState)((Block)ModBlocks.MAGIC_CENTER_BLOCK.get()).defaultBlockState().setValue(MagicCenterBlock.RADIUS, radius);
      if (!canPlace(level, player, zhenpan, placement, state)) {
         player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.zhenpan.blocked"), true);
         return null;
      } else if (!level.setBlock(placement, state, 3)) {
         return null;
      } else {
         BlockEntity var9 = level.getBlockEntity(placement);
         if (var9 instanceof MagicCenterBlockEntity) {
            MagicCenterBlockEntity center = (MagicCenterBlockEntity)var9;
            CompoundTag sanitized = portable.copy();
            sanitized.putInt("Radius", radius);
            center.applyPortableConfigurationTag(sanitized, level.registryAccess());
            return center;
         } else {
            level.removeBlock(placement, false);
            return null;
         }
      }
   }

   private static boolean canPlace(ServerLevel level, Player player, ItemStack zhenpan, BlockPos placement, BlockState state) {
      return level.isInWorldBounds(placement) && level.getBlockState(placement).canBeReplaced() && level.mayInteract(player, placement) && player.mayUseItemAt(placement, Direction.UP, zhenpan) && level.isUnobstructed(state, placement, CollisionContext.empty());
   }

   private static BlockPos resolvePlacementPos(Level level, BlockPos clickedPos, Direction clickedFace) {
      return level.getBlockState(clickedPos).canBeReplaced() ? clickedPos : clickedPos.relative(clickedFace);
   }

   private static PaymentResult paySummonCost(Player player, int requestedCost) {
      int cost = Math.max(0, requestedCost);
      if (cost != 0 && !player.getAbilities().instabuild) {
         StoneInventory stones = countStones(player.getInventory());
         if (stones.infinite()) {
            return new PaymentResult(0, 0);
         } else {
            int qiSpent = 0;
            int availableQi = Math.max(0, ModAttachments.getData(player).qi());
            StonePlan stonePlan;
            int var10;
            if (qiFirst(player)) {
               qiSpent = Math.min(availableQi, cost);
               var10 = cost - qiSpent;
               stonePlan = planStones(stones, var10);
               var10 = Math.max(0, var10 - stonePlan.value());
            } else {
               stonePlan = planStones(stones, cost);
               var10 = Math.max(0, cost - stonePlan.value());
               qiSpent = Math.min(availableQi, var10);
               var10 -= qiSpent;
            }

            if (var10 > 0) {
               return null;
            } else {
               consumeStone(player.getInventory(), (Item)ModItems.HIGH_SPIRIT_STONE.get(), stonePlan.high());
               consumeStone(player.getInventory(), (Item)ModItems.MEDIUM_SPIRIT_STONE.get(), stonePlan.medium());
               consumeStone(player.getInventory(), (Item)ModItems.SPIRIT_STONE.get(), stonePlan.low());
               if (qiSpent > 0) {
                  ModAttachments.CultivationData data = ModAttachments.getData(player);
                  ModAttachments.setData(player, data.withQi(data.qi() - qiSpent));
                  if (player instanceof ServerPlayer) {
                     ServerPlayer serverPlayer = (ServerPlayer)player;
                     ServerEvents.syncPlayerData(serverPlayer);
                  }
               }

               return new PaymentResult(stonePlan.value(), qiSpent);
            }
         }
      } else {
         return new PaymentResult(0, 0);
      }
   }

   private static StoneInventory countStones(Inventory inventory) {
      int low = 0;
      int medium = 0;
      int high = 0;
      boolean infinite = false;

      for(int slot = 0; slot < inventory.getContainerSize(); ++slot) {
         ItemStack stack = inventory.getItem(slot);
         if (stack.is((Item)ModItems.INFINITE_SPIRIT_STONE.get())) {
            infinite = true;
         } else if (stack.is((Item)ModItems.HIGH_SPIRIT_STONE.get())) {
            high += stack.getCount();
         } else if (stack.is((Item)ModItems.MEDIUM_SPIRIT_STONE.get())) {
            medium += stack.getCount();
         } else if (stack.is((Item)ModItems.SPIRIT_STONE.get())) {
            low += stack.getCount();
         }
      }

      return new StoneInventory(low, medium, high, infinite);
   }

   private static StonePlan planStones(StoneInventory stones, int requestedValue) {
      int remaining = Math.max(0, requestedValue);
      int high = Math.min(stones.high(), remaining / 100);
      remaining -= high * 100;
      int medium = Math.min(stones.medium(), remaining / 10);
      remaining -= medium * 10;
      int low = Math.min(stones.low(), remaining);
      remaining -= low;
      if (remaining > 0) {
         if (medium < stones.medium()) {
            ++medium;
         } else if (high < stones.high()) {
            ++high;
         }
      }

      return new StonePlan(low, medium, high, low + medium * 10 + high * 100);
   }

   private static void consumeStone(Inventory inventory, Item item, int requestedCount) {
      int remaining = requestedCount;

      for(int slot = 0; slot < inventory.getContainerSize() && remaining > 0; ++slot) {
         ItemStack stack = inventory.getItem(slot);
         if (stack.is(item)) {
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
         }
      }

      inventory.setChanged();
   }

   private static void clearTeleportLinkRecord(ItemStack zhenpan) {
      CompoundTag root = ((CustomData)zhenpan.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      root.remove("TeleportLinkRecord");
      zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
   }

   public static boolean hasStoredCenter(ItemStack zhenpan) {
      return storedCenter(zhenpan) != null;
   }

   private static CompoundTag storedCenter(ItemStack zhenpan) {
      CompoundTag root = ((CustomData)zhenpan.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (root.contains("StoredMagicCenter", 10)) {
         return root.getCompound("StoredMagicCenter").copy();
      } else {
         if (root.contains("StoredMagicCenters", 9)) {
            ListTag list = root.getList("StoredMagicCenters", 10);
            if (!list.isEmpty()) {
               return list.getCompound(0).copy();
            }
         }

         return null;
      }
   }

   private static boolean setStoredCenter(ItemStack zhenpan, CompoundTag centerTag) {
      if (hasStoredCenter(zhenpan)) {
         return false;
      } else {
         CompoundTag root = ((CustomData)zhenpan.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         root.remove("StoredMagicCenters");
         root.put("StoredMagicCenter", centerTag.copy());
         zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
         return true;
      }
   }

   private static boolean isTeleportPortable(CompoundTag portable) {
      ResourceLocation type = ResourceLocation.tryParse(portable.getString("CenterType"));
      return MagicCenterTypeRegistry.TELEPORT.equals(type);
   }

   private static List<CompoundTag> activeTemporaryCenters(ItemStack zhenpan, long gameTime) {
      CompoundTag root = ((CustomData)zhenpan.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      List<CompoundTag> activeCenters = new ArrayList();
      if (root.contains("ActiveTemporaryMagicCenters", 9)) {
         ListTag list = root.getList("ActiveTemporaryMagicCenters", 10);

         for(int index = 0; index < list.size(); ++index) {
            CompoundTag active = list.getCompound(index);
            if (active.getLong("ExpiresAt") > gameTime) {
               activeCenters.add(active.copy());
            }
         }
      }

      if (activeCenters.isEmpty() && root.contains("ActiveTemporaryMagicCenter", 10)) {
         CompoundTag legacy = root.getCompound("ActiveTemporaryMagicCenter");
         if (legacy.getLong("ExpiresAt") > gameTime) {
            activeCenters.add(legacy.copy());
         }
      }

      writeActiveTemporaryCenters(zhenpan, activeCenters);
      return activeCenters;
   }

   private static CompoundTag activeTemporaryCenterRecord(ServerLevel level, MagicCenterBlockEntity center, long expiresAt) {
      CompoundTag active = new CompoundTag();
      active.putLong("ExpiresAt", expiresAt);
      active.putString("Dimension", level.dimension().location().toString());
      active.putLong("Pos", center.getBlockPos().asLong());
      active.putUUID("CenterId", center.getCenterId());
      return active;
   }

   private static MagicCenterBlockEntity resolveTemporaryTeleportCenter(ServerLevel currentLevel, CompoundTag active) {
      if (active.contains("Pos", 4) && active.hasUUID("CenterId")) {
         ResourceLocation dimension = ResourceLocation.tryParse(active.getString("Dimension"));
         if (dimension == null) {
            return null;
         } else {
            ServerLevel targetLevel = currentLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (targetLevel == null) {
               return null;
            } else {
               BlockPos targetPos = BlockPos.of(active.getLong("Pos"));
               targetLevel.getChunkAt(targetPos);
               BlockEntity var6 = targetLevel.getBlockEntity(targetPos);
               if (var6 instanceof MagicCenterBlockEntity) {
                  MagicCenterBlockEntity target = (MagicCenterBlockEntity)var6;
                  if (target.isTemporary() && MagicCenterTypeRegistry.TELEPORT.equals(target.getCenterTypeId()) && active.getUUID("CenterId").equals(target.getCenterId())) {
                     return target;
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static void writeActiveTemporaryCenters(ItemStack zhenpan, List<CompoundTag> activeCenters) {
      CompoundTag root = ((CustomData)zhenpan.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      root.remove("ActiveTemporaryMagicCenter");
      if (activeCenters.isEmpty()) {
         root.remove("ActiveTemporaryMagicCenters");
      } else {
         ListTag list = new ListTag();

         for(CompoundTag active : activeCenters) {
            list.add(active.copy());
         }

         root.put("ActiveTemporaryMagicCenters", list);
      }

      zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
   }

   private static void clearStoredCenter(ItemStack zhenpan) {
      CompoundTag root = ((CustomData)zhenpan.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      CompoundTag legacySecond = null;
      if (!root.contains("StoredMagicCenter", 10) && root.contains("StoredMagicCenters", 9)) {
         ListTag legacy = root.getList("StoredMagicCenters", 10);
         if (legacy.size() > 1) {
            legacySecond = legacy.getCompound(1).copy();
         }
      }

      root.remove("StoredMagicCenter");
      root.remove("StoredMagicCenters");
      if (legacySecond != null) {
         root.put("StoredMagicCenter", legacySecond);
      }

      zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ZhenpanItemRenderer renderer;

         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new ZhenpanItemRenderer();
            }

            return this.renderer;
         }
      });
   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController(this, "zhenpan_spin", 0, (state) -> state.setAndContinue(SPIN_ANIMATION)));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private static record StoneInventory(int low, int medium, int high, boolean infinite) {
   }

   private static record StonePlan(int low, int medium, int high, int value) {
   }

   private static record PaymentResult(int stoneValueSpent, int qiSpent) {
   }
}
