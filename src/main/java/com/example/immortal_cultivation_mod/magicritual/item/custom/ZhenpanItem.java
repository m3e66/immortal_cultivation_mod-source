package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.config.ModServerConfig;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    /** Temporary two-slot format retained so old plates can recover both centers one at a time. */
    private static final String STORED_CENTERS_TAG = "StoredMagicCenters";
    private static final String TELEPORT_LINK_RECORD_TAG = "TeleportLinkRecord";
    private static final String ACTIVE_TEMPORARY_CENTERS_TAG = "ActiveTemporaryMagicCenters";
    /** Legacy single-center tag retained for migration. */
    private static final String ACTIVE_TEMPORARY_CENTER_TAG = "ActiveTemporaryMagicCenter";
    private static final String ACTIVE_TEMPORARY_EXPIRES_AT_TAG = "ExpiresAt";
    private static final String ACTIVE_TEMPORARY_DIMENSION_TAG = "Dimension";
    private static final String ACTIVE_TEMPORARY_POS_TAG = "Pos";
    private static final String ACTIVE_TEMPORARY_ID_TAG = "CenterId";
    private static final String QI_FIRST_PAYMENT_TAG =
            ImmortalCultivationMod.MODID + ".formation_plate_qi_first";
    private static final int PORTABLE_RADIUS = 5;
    private static final int MAX_ACTIVE_TEMPORARY_TELEPORT_CENTERS = 2;
    private static final int TEMPORARY_DURATION_TICKS = 20 * 20;
    private static final int TEMPORARY_COST_MULTIPLIER = 10;
    private static final double TEMPORARY_REACH_MULTIPLIER = 2.0D;
    private static final RawAnimation SPIN_ANIMATION =
            RawAnimation.begin().thenLoop("animation.zhenpan.spin");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ZhenpanItem(Properties properties) {
        super(properties);
    }

    public static void setQiFirst(Player player, boolean qiFirst) {
        if (player != null) {
            player.getPersistentData().putBoolean(QI_FIRST_PAYMENT_TAG, qiFirst);
        }
    }

    public static void copyPersistentData(Player original, Player copy) {
        if (original == null || copy == null) {
            return;
        }
        CompoundTag source = original.getPersistentData();
        if (source.contains(QI_FIRST_PAYMENT_TAG)) {
            copy.getPersistentData().putBoolean(
                    QI_FIRST_PAYMENT_TAG, source.getBoolean(QI_FIRST_PAYMENT_TAG));
        }
    }

    private static boolean qiFirst(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(QI_FIRST_PAYMENT_TAG)
                ? data.getBoolean(QI_FIRST_PAYMENT_TAG)
                : ModServerConfig.zhenpanQiFirst();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = (ServerLevel) context.getLevel();
        ItemStack zhenpan = context.getItemInHand();
        if (!player.isShiftKeyDown()
                && level.getBlockEntity(context.getClickedPos())
                instanceof MagicCenterBlockEntity clickedCenter) {
            if (clickedCenter.isTemporary()) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID
                                + ".zhenpan.temporary_locked"), true);
                return InteractionResult.SUCCESS;
            }
            if (MagicCenterTypeRegistry.TELEPORT.equals(clickedCenter.getCenterTypeId())) {
                if (teleportLinkRecord(zhenpan) != null) {
                    linkRecordedTeleportCenter(level, player, zhenpan, clickedCenter);
                } else if (!hasStoredCenter(zhenpan)) {
                    recordTeleportCenter(level, player, zhenpan, clickedCenter);
                }
            }
            player.openMenu(clickedCenter, context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(context.getClickedPos())
                    instanceof MagicCenterBlockEntity) {
                return packCenter(level, player, zhenpan, context.getClickedPos())
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            if (hasStoredCenter(zhenpan)) {
                BlockPos placement = resolvePlacementPos(
                        level, context.getClickedPos(), context.getClickedFace());
                return placeStoredCenter(level, player, zhenpan, placement)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.FAIL;
            }
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.no_center"), true);
            return InteractionResult.FAIL;
        }

        BlockPos placement = resolvePlacementPos(
                level, context.getClickedPos(), context.getClickedFace());
        return summonTemporaryCenter(level, player, zhenpan, placement)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack zhenpan = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(zhenpan);
        }

        double temporaryReach = Math.max(1.0D,
                player.blockInteractionRange() * TEMPORARY_REACH_MULTIPLIER);
        HitResult target = player.pick(temporaryReach, 0.0F, false);
        if (!(target instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".zhenpan.look_at_block"), true);
            }
            return InteractionResultHolder.fail(zhenpan);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(zhenpan);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos placement = resolvePlacementPos(serverLevel, hit.getBlockPos(), hit.getDirection());
        boolean summoned = summonTemporaryCenter(serverLevel, player, zhenpan, placement);
        return summoned
                ? InteractionResultHolder.success(zhenpan)
                : InteractionResultHolder.fail(zhenpan);
    }

    private static boolean packCenter(
            ServerLevel level, Player player, ItemStack zhenpan, BlockPos centerPos) {
        if (hasStoredCenter(zhenpan)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.full"), true);
            return false;
        }
        if (!(level.getBlockEntity(centerPos) instanceof MagicCenterBlockEntity center)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.no_center"), true);
            return false;
        }
        if (center.isTemporary()) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.temporary_locked"), true);
            return false;
        }
        if (!CenterTargetPolicy.canManageControllers(center, player)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".center.not_authorized"), true);
            return false;
        }
        if (!level.mayInteract(player, centerPos)
                || !player.mayUseItemAt(centerPos, Direction.UP, zhenpan)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.blocked"), true);
            return false;
        }

        center.setRadius(Math.min(PORTABLE_RADIUS, center.getRadius()));
        CompoundTag portable = center.createPortableConfigurationTag(level.registryAccess());
        portable.putInt("Radius", center.getRadius());
        if (!setStoredCenter(zhenpan, portable)) {
            return false;
        }
        center.removeForPacking();
        level.playSound(null, centerPos, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 0.7F, 1.35F);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".zhenpan.packed"), true);
        return true;
    }

    private static boolean placeStoredCenter(
            ServerLevel level, Player player, ItemStack zhenpan, BlockPos placement) {
        CompoundTag portable = storedCenter(zhenpan);
        if (portable == null) {
            return false;
        }
        int storedRadius = MagicCenterBlockEntity.normalizeRadius(
                Math.min(PORTABLE_RADIUS, portable.getInt("Radius")));
        MagicCenterBlockEntity center = createCenter(
                level, player, zhenpan, placement, portable, storedRadius);
        if (center == null) {
            return false;
        }

        center.reconcileTeleportLinkAfterPlacement();
        clearStoredCenter(zhenpan);
        level.playSound(null, placement, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 0.8F, 0.85F);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".zhenpan.placed"), true);
        return true;
    }

    private static boolean summonTemporaryCenter(
            ServerLevel level, Player player, ItemStack zhenpan, BlockPos placement) {
        CompoundTag portable = storedCenter(zhenpan);
        if (portable == null) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.empty"), true);
            return false;
        }
        List<CompoundTag> activeTemporaryCenters = activeTemporaryCenters(
                zhenpan, level.getGameTime());
        boolean teleportTemporary = isTeleportPortable(portable);
        int activeLimit = teleportTemporary
                ? MAX_ACTIVE_TEMPORARY_TELEPORT_CENTERS : 1;
        if (activeTemporaryCenters.size() >= activeLimit) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.temporary_active"), true);
            return false;
        }

        MagicCenterBlockEntity existingTemporaryTeleport = null;
        if (teleportTemporary && !activeTemporaryCenters.isEmpty()) {
            existingTemporaryTeleport = resolveTemporaryTeleportCenter(
                    level, activeTemporaryCenters.getFirst());
            if (existingTemporaryTeleport == null) {
                activeTemporaryCenters.clear();
                writeActiveTemporaryCenters(zhenpan, activeTemporaryCenters);
            }
        }

        MagicCenterBlockEntity center = createCenter(
                level, player, zhenpan, placement, portable, PORTABLE_RADIUS);
        if (center == null) {
            return false;
        }

        int durationFlagCount = center.countDurationFlags();
        long durationTicks = TEMPORARY_DURATION_TICKS
                + (long) durationFlagCount * DurationFlagItem.TEMPORARY_DURATION_BONUS_TICKS;
        int costPerMinute = Math.max(0, center.getCurrentFuelCostPerMinute());
        long originalCost =
                ((long) costPerMinute * TEMPORARY_DURATION_TICKS + 1_199L) / 1_200L;
        int cost = (int) Math.min(Integer.MAX_VALUE,
                originalCost * TEMPORARY_COST_MULTIPLIER);
        PaymentResult payment = paySummonCost(player, cost);
        if (payment == null) {
            center.removeWithoutDrops();
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.insufficient", cost), true);
            return false;
        }

        long expiresAt = level.getGameTime() + durationTicks;
        center.markTemporary(expiresAt);
        if (existingTemporaryTeleport != null) {
            existingTemporaryTeleport.unlinkTeleportPair();
            center.unlinkTeleportPair();
            existingTemporaryTeleport.linkTeleportPair(center);
        }
        activeTemporaryCenters.add(activeTemporaryCenterRecord(level, center, expiresAt));
        writeActiveTemporaryCenters(zhenpan, activeTemporaryCenters);
        level.playSound(null, placement, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 0.9F, 1.2F);
        player.getCooldowns().addCooldown(zhenpan.getItem(), 10);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".zhenpan.summoned",
                durationTicks / 20L, payment.stoneValueSpent(), payment.qiSpent()), true);
        return true;
    }

    private static MagicCenterBlockEntity createCenter(
            ServerLevel level, Player player, ItemStack zhenpan,
            BlockPos placement, CompoundTag portable, int requestedRadius) {
        int radius = MagicCenterBlockEntity.normalizeRadius(
                Math.min(PORTABLE_RADIUS, requestedRadius));
        BlockState state = ModBlocks.MAGIC_CENTER_BLOCK.get().defaultBlockState()
                .setValue(MagicCenterBlock.RADIUS, radius);
        if (!canPlace(level, player, zhenpan, placement, state)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".zhenpan.blocked"), true);
            return null;
        }
        if (!level.setBlock(placement, state, Block.UPDATE_ALL)) {
            return null;
        }
        if (!(level.getBlockEntity(placement) instanceof MagicCenterBlockEntity center)) {
            level.removeBlock(placement, false);
            return null;
        }

        CompoundTag sanitized = portable.copy();
        sanitized.putInt("Radius", radius);
        center.applyPortableConfigurationTag(sanitized, level.registryAccess());
        return center;
    }

    private static boolean canPlace(
            ServerLevel level, Player player, ItemStack zhenpan,
            BlockPos placement, BlockState state) {
        return level.isInWorldBounds(placement)
                && level.getBlockState(placement).canBeReplaced()
                && level.mayInteract(player, placement)
                && player.mayUseItemAt(placement, Direction.UP, zhenpan)
                && level.isUnobstructed(state, placement, CollisionContext.empty());
    }

    private static BlockPos resolvePlacementPos(
            Level level, BlockPos clickedPos, Direction clickedFace) {
        return level.getBlockState(clickedPos).canBeReplaced()
                ? clickedPos
                : clickedPos.relative(clickedFace);
    }

    private static PaymentResult paySummonCost(Player player, int requestedCost) {
        int cost = Math.max(0, requestedCost);
        if (cost == 0 || player.getAbilities().instabuild) {
            return new PaymentResult(0, 0);
        }

        StoneInventory stones = countStones(player.getInventory());
        if (stones.infinite()) {
            return new PaymentResult(0, 0);
        }

        int remaining = cost;
        int qiSpent = 0;
        StonePlan stonePlan;
        int availableQi = Math.max(0, ModAttachments.getData(player).qi());
        if (qiFirst(player)) {
            qiSpent = Math.min(availableQi, remaining);
            remaining -= qiSpent;
            stonePlan = planStones(stones, remaining);
            remaining = Math.max(0, remaining - stonePlan.value());
        } else {
            stonePlan = planStones(stones, remaining);
            remaining = Math.max(0, remaining - stonePlan.value());
            qiSpent = Math.min(availableQi, remaining);
            remaining -= qiSpent;
        }

        if (remaining > 0) {
            return null;
        }

        consumeStone(player.getInventory(), ModItems.HIGH_SPIRIT_STONE.get(), stonePlan.high());
        consumeStone(player.getInventory(), ModItems.MEDIUM_SPIRIT_STONE.get(), stonePlan.medium());
        consumeStone(player.getInventory(), ModItems.SPIRIT_STONE.get(), stonePlan.low());
        if (qiSpent > 0) {
            var data = ModAttachments.getData(player);
            ModAttachments.setData(player, data.withQi(data.qi() - qiSpent));
            if (player instanceof ServerPlayer serverPlayer) {
                ServerEvents.syncPlayerData(serverPlayer);
            }
        }
        return new PaymentResult(stonePlan.value(), qiSpent);
    }

    private static StoneInventory countStones(Inventory inventory) {
        int low = 0;
        int medium = 0;
        int high = 0;
        boolean infinite = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.INFINITE_SPIRIT_STONE.get())) {
                infinite = true;
            } else if (stack.is(ModItems.HIGH_SPIRIT_STONE.get())) {
                high += stack.getCount();
            } else if (stack.is(ModItems.MEDIUM_SPIRIT_STONE.get())) {
                medium += stack.getCount();
            } else if (stack.is(ModItems.SPIRIT_STONE.get())) {
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
                medium++;
            } else if (high < stones.high()) {
                high++;
            }
        }
        return new StonePlan(low, medium, high, low + medium * 10 + high * 100);
    }

    private static void consumeStone(Inventory inventory, Item item, int requestedCount) {
        int remaining = requestedCount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
        }
        inventory.setChanged();
    }

    private static boolean recordTeleportCenter(ServerLevel level, Player player,
                                                ItemStack zhenpan,
                                                MagicCenterBlockEntity center) {
        if (center.isTemporary() || !center.isOwner(player)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".teleport.link_owner_only"), true);
            return false;
        }
        CompoundTag record = new CompoundTag();
        record.putString("Dimension", level.dimension().location().toString());
        record.putLong("Pos", center.getBlockPos().asLong());
        record.putUUID("Owner", player.getUUID());
        record.putUUID("CenterId", center.getCenterId());
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(TELEPORT_LINK_RECORD_TAG, record);
        zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        level.playSound(null, center.getBlockPos(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, 0.8F, 1.25F);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".teleport.link_recorded",
                center.getBlockPos().getX(), center.getBlockPos().getY(),
                center.getBlockPos().getZ()), true);
        return true;
    }

    private static boolean linkRecordedTeleportCenter(ServerLevel secondLevel, Player player,
                                                      ItemStack zhenpan,
                                                      MagicCenterBlockEntity second) {
        CompoundTag record = teleportLinkRecord(zhenpan);
        if (record == null || !record.hasUUID("Owner") || !record.hasUUID("CenterId")
                || !record.contains("Pos", Tag.TAG_LONG)) {
            return false;
        }
        if (!record.getUUID("Owner").equals(player.getUUID())
                || second.isTemporary() || !second.isOwner(player)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".teleport.link_owner_only"), true);
            return false;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(
                record.getString("Dimension"));
        if (dimensionId == null) {
            return false;
        }
        ServerLevel firstLevel = secondLevel.getServer().getLevel(ResourceKey.create(
                Registries.DIMENSION, dimensionId));
        BlockPos firstPos = BlockPos.of(record.getLong("Pos"));
        if (firstLevel == null) {
            clearTeleportLinkRecord(zhenpan);
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".teleport.first_missing"), true);
            return false;
        }
        firstLevel.getChunkAt(firstPos);
        if (!(firstLevel.getBlockEntity(firstPos) instanceof MagicCenterBlockEntity first)
                || !MagicCenterTypeRegistry.TELEPORT.equals(first.getCenterTypeId())
                || !record.getUUID("CenterId").equals(first.getCenterId())
                || !first.isOwner(player)
                || first.getCenterId().equals(second.getCenterId())) {
            clearTeleportLinkRecord(zhenpan);
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".teleport.first_missing"), true);
            return false;
        }

        first.unlinkTeleportPair();
        second.unlinkTeleportPair();
        first.linkTeleportPair(second);
        clearTeleportLinkRecord(zhenpan);
        firstLevel.playSound(null, firstPos, SoundEvents.END_PORTAL_FRAME_FILL,
                SoundSource.BLOCKS, 1.0F, 1.15F);
        secondLevel.playSound(null, second.getBlockPos(), SoundEvents.END_PORTAL_FRAME_FILL,
                SoundSource.BLOCKS, 1.0F, 1.15F);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".teleport.link_complete"), true);
        return true;
    }

    private static CompoundTag teleportLinkRecord(ItemStack zhenpan) {
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(TELEPORT_LINK_RECORD_TAG, Tag.TAG_COMPOUND)
                ? root.getCompound(TELEPORT_LINK_RECORD_TAG) : null;
    }

    private static void clearTeleportLinkRecord(ItemStack zhenpan) {
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(TELEPORT_LINK_RECORD_TAG);
        zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static boolean hasStoredCenter(ItemStack zhenpan) {
        return storedCenter(zhenpan) != null;
    }

    private static CompoundTag storedCenter(ItemStack zhenpan) {
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(STORED_CENTER_TAG, Tag.TAG_COMPOUND)) {
            return root.getCompound(STORED_CENTER_TAG).copy();
        }
        if (root.contains(STORED_CENTERS_TAG, Tag.TAG_LIST)) {
            ListTag list = root.getList(STORED_CENTERS_TAG, Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                return list.getCompound(0).copy();
            }
        }
        return null;
    }

    private static boolean setStoredCenter(ItemStack zhenpan, CompoundTag centerTag) {
        if (hasStoredCenter(zhenpan)) {
            return false;
        }
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(STORED_CENTERS_TAG);
        root.put(STORED_CENTER_TAG, centerTag.copy());
        zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }

    private static boolean isTeleportPortable(CompoundTag portable) {
        ResourceLocation type = ResourceLocation.tryParse(portable.getString("CenterType"));
        return MagicCenterTypeRegistry.TELEPORT.equals(type);
    }

    private static List<CompoundTag> activeTemporaryCenters(
            ItemStack zhenpan, long gameTime) {
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<CompoundTag> activeCenters = new ArrayList<>();
        if (root.contains(ACTIVE_TEMPORARY_CENTERS_TAG, Tag.TAG_LIST)) {
            ListTag list = root.getList(ACTIVE_TEMPORARY_CENTERS_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                CompoundTag active = list.getCompound(index);
                if (active.getLong(ACTIVE_TEMPORARY_EXPIRES_AT_TAG) > gameTime) {
                    activeCenters.add(active.copy());
                }
            }
        }
        if (activeCenters.isEmpty()
                && root.contains(ACTIVE_TEMPORARY_CENTER_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag legacy = root.getCompound(ACTIVE_TEMPORARY_CENTER_TAG);
            if (legacy.getLong(ACTIVE_TEMPORARY_EXPIRES_AT_TAG) > gameTime) {
                activeCenters.add(legacy.copy());
            }
        }
        writeActiveTemporaryCenters(zhenpan, activeCenters);
        return activeCenters;
    }

    private static CompoundTag activeTemporaryCenterRecord(
            ServerLevel level, MagicCenterBlockEntity center, long expiresAt) {
        CompoundTag active = new CompoundTag();
        active.putLong(ACTIVE_TEMPORARY_EXPIRES_AT_TAG, expiresAt);
        active.putString(ACTIVE_TEMPORARY_DIMENSION_TAG,
                level.dimension().location().toString());
        active.putLong(ACTIVE_TEMPORARY_POS_TAG, center.getBlockPos().asLong());
        active.putUUID(ACTIVE_TEMPORARY_ID_TAG, center.getCenterId());
        return active;
    }

    private static MagicCenterBlockEntity resolveTemporaryTeleportCenter(
            ServerLevel currentLevel, CompoundTag active) {
        if (!active.contains(ACTIVE_TEMPORARY_POS_TAG, Tag.TAG_LONG)
                || !active.hasUUID(ACTIVE_TEMPORARY_ID_TAG)) {
            return null;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(
                active.getString(ACTIVE_TEMPORARY_DIMENSION_TAG));
        if (dimension == null) {
            return null;
        }
        ServerLevel targetLevel = currentLevel.getServer().getLevel(ResourceKey.create(
                Registries.DIMENSION, dimension));
        if (targetLevel == null) {
            return null;
        }
        BlockPos targetPos = BlockPos.of(active.getLong(ACTIVE_TEMPORARY_POS_TAG));
        targetLevel.getChunkAt(targetPos);
        if (targetLevel.getBlockEntity(targetPos)
                instanceof MagicCenterBlockEntity target
                && target.isTemporary()
                && MagicCenterTypeRegistry.TELEPORT.equals(target.getCenterTypeId())
                && active.getUUID(ACTIVE_TEMPORARY_ID_TAG).equals(target.getCenterId())) {
            return target;
        }
        return null;
    }

    private static void writeActiveTemporaryCenters(
            ItemStack zhenpan, List<CompoundTag> activeCenters) {
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(ACTIVE_TEMPORARY_CENTER_TAG);
        if (activeCenters.isEmpty()) {
            root.remove(ACTIVE_TEMPORARY_CENTERS_TAG);
        } else {
            ListTag list = new ListTag();
            for (CompoundTag active : activeCenters) {
                list.add(active.copy());
            }
            root.put(ACTIVE_TEMPORARY_CENTERS_TAG, list);
        }
        zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static void clearStoredCenter(ItemStack zhenpan) {
        CompoundTag root = zhenpan.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag legacySecond = null;
        if (!root.contains(STORED_CENTER_TAG, Tag.TAG_COMPOUND)
                && root.contains(STORED_CENTERS_TAG, Tag.TAG_LIST)) {
            ListTag legacy = root.getList(STORED_CENTERS_TAG, Tag.TAG_COMPOUND);
            if (legacy.size() > 1) {
                legacySecond = legacy.getCompound(1).copy();
            }
        }
        root.remove(STORED_CENTER_TAG);
        root.remove(STORED_CENTERS_TAG);
        if (legacySecond != null) {
            root.put(STORED_CENTER_TAG, legacySecond);
        }
        zhenpan.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ZhenpanItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ZhenpanItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this, "zhenpan_spin", 0,
                state -> state.setAndContinue(SPIN_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private record StoneInventory(int low, int medium, int high, boolean infinite) {
    }

    private record StonePlan(int low, int medium, int high, int value) {
    }

    private record PaymentResult(int stoneValueSpent, int qiSpent) {
    }
}
