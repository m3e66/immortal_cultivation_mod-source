package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.flame.SpiritFlameBlock;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DingBlockEntity extends GeoRockBlockEntity implements Container, MenuProvider {
    public static final int YUJIAN_SLOT = 0;
    public static final int MATERIAL_START = 1;
    public static final int MATERIAL_COUNT = 30;
    public static final int SMALL_FORGE_SLOTS = 10;
    public static final int LARGE_FORGE_SLOTS = 15;
    public static final int GOLDEN_CORE_FORGE_SLOTS = 20;
    public static final int RESULT_SLOT = MATERIAL_START + MATERIAL_COUNT;
    public static final int SLOT_COUNT = RESULT_SLOT + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final NonNullList<ItemStack> forgingSnapshot = NonNullList.withSize(MATERIAL_COUNT, ItemStack.EMPTY);
    private final int[] initialMaterialCounts = new int[MATERIAL_COUNT];
    private final int[] meltedMaterialCounts = new int[MATERIAL_COUNT];
    private String activeRecipeId = "";
    private String activeFireType = "normal_fire";
    private int temperature;
    private int targetTemperature;
    private int progressTicks;
    private int maxProgressTicks = 20;
    private boolean forging;
    private long temperatureSamples;
    private long temperatureTotal;
    private UUID forgingPlayerId;
    private ServerPlayer visualPlayer;
    private final java.util.Set<UUID> meshViewers = new java.util.HashSet<>();
    private boolean meshLidOpen;
    private final NonNullList<ItemStack> displayItems = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private long finishAnimationUntil;

    public boolean hasLid() {
        return getBlockState().is(ModBlocks.QINGTONG_YINHUO_DING.get())
                || getBlockState().is(ModBlocks.CHI_JIN_SANYANG_DING.get());
    }

    public boolean isLidOpen() { return meshLidOpen; }
    public ItemStack getDisplayItem(int slot) { return displayItems.get(slot); }

    /** A physical lid stays where the player leaves it, independently of menu viewers. */
    public void setLidOpen(boolean open) {
        if (!hasLid() || level == null || level.isClientSide || meshLidOpen == open) return;
        meshLidOpen = open;
        if (!open) {
            for (UUID id : List.copyOf(meshViewers)) {
                if (level.getPlayerByUUID(id) instanceof ServerPlayer viewer
                        && viewer.containerMenu instanceof DingMenu menu && menu.usesForge(this)) {
                    viewer.closeContainer();
                }
            }
            meshViewers.clear();
        }
        setChanged();
        syncAnimationState();
    }
    public boolean isForging() { return forging; }
    public boolean isFinishingAnimation() {
        return level != null && level.getGameTime() < finishAnimationUntil;
    }

    private void syncAnimationState() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public void startOpen(Player player) {
        if (player instanceof ServerPlayer server && hasFireBelow()) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "forge_fire"); }
        if (level != null && !level.isClientSide && meshViewers.add(player.getUUID())) syncMeshLid();
    }
    @Override public void stopOpen(Player player) {
        if (level != null && !level.isClientSide && meshViewers.remove(player.getUUID())) syncMeshLid();
    }
    private void syncMeshLid() {
        if (hasLid()) return;
        boolean open = !meshViewers.isEmpty();
        if (meshLidOpen == open) return;
        meshLidOpen = open;
        syncAnimationState();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("MeshLidOpen", meshLidOpen);
        tag.putBoolean("Forging", forging);
        tag.putLong("FinishAnimationUntil", finishAnimationUntil);
        CompoundTag displayTag = new CompoundTag();
        ContainerHelper.saveAllItems(displayTag, displayItems, registries);
        tag.put("DisplayItems", displayTag);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        meshLidOpen = tag.getBoolean("MeshLidOpen");
        forging = tag.getBoolean("Forging");
        finishAnimationUntil = tag.getLong("FinishAnimationUntil");
        if (tag.contains("DisplayItems")) {
            displayItems.clear();
            ContainerHelper.loadAllItems(tag.getCompound("DisplayItems"), displayItems, registries);
        }
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public DingBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DING.get(), pos, blockState);
    }

    public int materialSlotCount() {
        if (getBlockState().is(ModBlocks.CHI_JIN_SANYANG_DING.get())) return GOLDEN_CORE_FORGE_SLOTS;
        return getBlockState().is(ModBlocks.CU_TIE_XIAODING.get())
                ? SMALL_FORGE_SLOTS
                : LARGE_FORGE_SLOTS;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, DingBlockEntity ding) {
        if (level.getGameTime() % 20L == 0L && !ding.meshViewers.isEmpty()) {
            ding.meshViewers.removeIf(id -> {
                Player viewer = level.getPlayerByUUID(id);
                return viewer == null || !(viewer.containerMenu instanceof DingMenu menu) || !menu.usesForge(ding);
            });
            ding.syncMeshLid();
        }
        if (!ding.forging) {
            return;
        }
        if (!ding.hasFireBelow() || !ding.canContinue()) {
            ding.finishForging(true);
            return;
        }
        // Loaded heat must obey the current flame catalog before it enters quality samples.
        ding.temperature = Math.clamp(ding.temperature, ding.lowestFireTemp(), ding.highestFireTemp());
        if (level.getGameTime() % 20L == 0L) {
            ServerPlayer player = ding.nearestPlayer();
            if (player == null || !ding.chargeQi(player, 100)) {
                ding.finishForging(true);
                return;
            }
            if (ding.progressTicks >= 20) {
                ding.temperatureSamples++;
                ding.temperatureTotal += ding.temperature;
            }
        }
        ding.refreshForgingVisual(false);
        if (ding.temperature > ding.lowestFireTemp()) {
            ding.temperature = Math.max(ding.lowestFireTemp(), ding.temperature - 35);
        }
        ding.progressTicks++;
        ding.meltVisibleMaterials();
        if (ding.progressTicks == 20 && ding.level instanceof ServerLevel serverLevel
                && ding.forgingPlayerId != null) {
            ServerPlayer forgingPlayer = serverLevel.getServer().getPlayerList()
                    .getPlayer(ding.forgingPlayerId);
            if (forgingPlayer != null) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                        forgingPlayer,
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.FORGE_MELT);
            }
        }
        if (ding.progressTicks >= ding.maxProgressTicks) {
            ding.finishForging(false);
        }
        ding.setChanged();
    }

    public void startForging(Player player) {
        if (forging || level == null || level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return;
        }
        ForgingSystem.ForgingRecipe recipe = getItem(YUJIAN_SLOT).isEmpty()
                &&materialStacks().stream().anyMatch(com.example.immortal_cultivation_mod.item.WeaponShardSystem::populated)
                ?ForgingSystem.recipeById(com.example.immortal_cultivation_mod.item.WeaponShardSystem.REFORGE)
                :ForgingSystem.recipe(getItem(YUJIAN_SLOT));
        if (recipe == null || !hasFireBelow()
                || !ForgingSystem.canUseFire(recipe, currentFireType(), materialStacks())
                || !ForgingSystem.hasRequiredMaterials(recipe, materialStacks())
                || !getItem(RESULT_SLOT).isEmpty()) {
            return;
        }
        if (!chargeQi(sp, 100)) {
            return;
        }
        captureForgingSnapshot(recipe);
        forgingPlayerId = sp.getUUID();
        activeRecipeId = recipe.id();
        activeFireType = currentFireType();
        targetTemperature = ForgingSystem.targetTemperature(recipe, snapshotStacks());
        temperature = lowestFireTemp();
        maxProgressTicks = ForgingSystem.meltTicks(snapshotStacks());
        progressTicks = 0;
        temperatureSamples = 0;
        temperatureTotal = 0;
        forging = true;
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(sp, "forge_fire");
        finishAnimationUntil = 0;
        syncAnimationState();
        refreshForgingVisual(true);
        setChanged();
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.FORGE_MATERIAL);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.FORGE_MOLD);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.READ_RECIPE_SLIP);
    }

    public void heatPulse(Player player) {
        if (!forging || level == null || level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!chargeQi(sp, fireCost())) {
            return;
        }
        // A higher fire ceiling must not make each pulse too coarse to hold the recipe's target.
        int heatStep = Math.max(1, Math.min(250, (highestFireTemp() - lowestFireTemp()) / 10));
        temperature = Math.min(highestFireTemp(),
                Math.clamp(temperature, lowestFireTemp(), highestFireTemp()) + heatStep);
        setChanged();
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.FORGE_HEAT);
    }

    private boolean chargeQi(ServerPlayer player, int cost) {
        var data = ModAttachments.getData(player);
        if (ServerEvents.spendQiOrBlood(player, data, cost)) {
            ServerEvents.syncPlayerData(player);
            return true;
        }
        int remaining = cost;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int restore = ServerEvents.magicRitualSpiritStoneRestoreAmount(key);
            if (restore <= 0) {
                continue;
            }
            if (ServerEvents.isInfiniteMagicRitualSpiritStone(key)) {
                return true;
            }
            remaining -= restore;
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        if (remaining <= 0) {
            return true;
        }
        return ServerEvents.spendQiOrBlood(player, ModAttachments.getData(player), remaining);
    }

    private boolean canContinue() {
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipeById(activeRecipeId);
        return recipe != null
                && (!ForgingSystem.requiresHeavenlyFire(recipe, snapshotStacks())
                    || FlameKind.fromId(currentFireType()) == FlameKind.HEAVENLY)
                && (!com.example.immortal_cultivation_mod.item.WeaponShardSystem.REFORGE.equals(activeRecipeId)
                    || targetTemperature <= highestFireTemp())
                && (!FlameKind.fromId(activeFireType).special()
                    || activeFireType.equals(currentFireType()) && targetTemperature <= highestFireTemp())
                && (activeFireType.equals(currentFireType())
                    || targetTemperature <= highestFireTemp()
                        && ForgingSystem.canUseFire(recipe, currentFireType(), snapshotStacks()))
                && getItem(RESULT_SLOT).isEmpty();
    }

    private void finishForging(boolean failedEarly) {
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipeById(activeRecipeId);
        if (recipe == null) {
            stopForging();
            return;
        }
        List<ItemStack> materials = snapshotStacks();
        if(com.example.immortal_cultivation_mod.item.WeaponShardSystem.REFORGE.equals(activeRecipeId)){
            // Reforging uses the whole set, never a random purity failure. An
            // interrupted reforge leaves its still-unmelted ingredients intact.
            if(!failedEarly&&level!=null){
                var restored=com.example.immortal_cultivation_mod.item.WeaponShardSystem.reforge(materials,level.registryAccess());
                if(!restored.isEmpty()){meltAllRemainingRequiredMaterials();setItem(RESULT_SLOT,restored);awardForgingProficiency();}
            }
            stopForging();return;
        }
        meltAllRemainingRequiredMaterials();
        double successChance = ForgingSystem.forgingSuccessChancePercent(materials);
        boolean succeeded = !failedEarly && level != null
                && level.random.nextDouble() * 100.0D < successChance;
        if (!succeeded) {
            salvageWoodFlameMaterials(recipe);
            setItem(RESULT_SLOT, new ItemStack(ModItems.ASHES.get()));
            stopForging();
            return;
        }
        double avg = temperatureSamples <= 0 ? temperature : (double) temperatureTotal / (double) temperatureSamples;
        ItemStack result = ForgingSystem.createMold(
                recipe, materials, avg, activeFireType, targetTemperature, level.random);
        if (level instanceof ServerLevel serverLevel && forgingPlayerId != null) {
            ServerPlayer crafter = serverLevel.getServer().getPlayerList().getPlayer(forgingPlayerId);
            if (crafter != null) com.example.immortal_cultivation_mod.fate.FateCrafting.afterForge(
                    crafter, result, !recipe.directOutput());
        }
        setItem(RESULT_SLOT, result);
        awardForgingProficiency();
        stopForging();
    }

    private void awardForgingProficiency() {
        if (!(level instanceof ServerLevel serverLevel) || forgingPlayerId == null) {
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(forgingPlayerId);
        if (player != null) {
            if (FlameKind.fromId(activeFireType).special()) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "special_forge"); }
            if (com.example.immortal_cultivation_mod.item.WeaponShardSystem.REFORGE.equals(activeRecipeId)) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "weapon_reforged"); }
            else if (snapshotStacks().stream().anyMatch(com.example.immortal_cultivation_mod.item.WeaponShardSystem::populated)) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "shard_forge"); }
            com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.recordSuccessfulBatch(player, activeRecipeId);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.FORGE_COMPLETE);
        }
    }

    private void meltVisibleMaterials() {
        if(com.example.immortal_cultivation_mod.item.WeaponShardSystem.REFORGE.equals(activeRecipeId))return;
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            ItemStack snapshot = forgingSnapshot.get(i);
            if (snapshot.isEmpty() || ForgingSystem.isReusableAdditive(snapshot)) {
                continue;
            }
            int initial = initialMaterialCounts[i];
            if (initial <= 0) {
                continue;
            }
            int meltTicks = Math.max(1, ForgingSystem.meltTicksForStack(snapshot));
            int shouldBeMelted = Math.min(initial, (int) ((long) initial * progressTicks / meltTicks));
            int delta = shouldBeMelted - meltedMaterialCounts[i];
            if (delta <= 0) {
                continue;
            }
            ItemStack visible = getItem(MATERIAL_START + i);
            if (!visible.isEmpty() && ItemStack.isSameItemSameComponents(visible, snapshot)) {
                int consumed = Math.min(delta, visible.getCount());
                visible.shrink(consumed);
                meltedMaterialCounts[i] += consumed;
            }
        }
    }

    private void meltAllRemainingRequiredMaterials() {
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            ItemStack snapshot = forgingSnapshot.get(i);
            if (snapshot.isEmpty() || ForgingSystem.isReusableAdditive(snapshot)) {
                continue;
            }
            int remaining = Math.max(0, initialMaterialCounts[i] - meltedMaterialCounts[i]);
            ItemStack visible = getItem(MATERIAL_START + i);
            if (remaining > 0 && !visible.isEmpty()
                    && ItemStack.isSameItemSameComponents(visible, snapshot)) {
                int consumed = Math.min(remaining, visible.getCount());
                visible.shrink(consumed);
                meltedMaterialCounts[i] += consumed;
            }
        }
    }

    private void salvageWoodFlameMaterials(ForgingSystem.ForgingRecipe recipe) {
        if (level == null) return;
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            ItemStack consumed = forgingSnapshot.get(i).copyWithCount(meltedMaterialCounts[i]);
            int count = ForgingSystem.woodFlameSalvageCount(recipe, activeFireType, consumed);
            if (count <= 0) continue;
            ItemStack refund = consumed.copyWithCount(count);
            ItemStack visible = getItem(MATERIAL_START + i);
            if (visible.isEmpty()) {
                setItem(MATERIAL_START + i, refund);
            } else if (ItemStack.isSameItemSameComponents(visible, refund)
                    && visible.getCount() + count <= visible.getMaxStackSize()) {
                visible.grow(count);
            } else {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + .5,
                        worldPosition.getY() + .5, worldPosition.getZ() + .5, refund);
            }
        }
    }

    private void stopForging() {
        clearForgingVisual();
        finishAnimationUntil = forging && level != null ? level.getGameTime() + 18 : 0;
        forging = false;
        progressTicks = 0;
        activeRecipeId = "";
        activeFireType = "normal_fire";
        forgingPlayerId = null;
        clearForgingSnapshot();
        setChanged();
        syncAnimationState();
    }

    private void sendForgingVisual(ServerPlayer player, boolean active) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new com.example.immortal_cultivation_mod.network.ForgingVisualPayload(
                        player.getUUID(),player.level().dimension().location(),level.dimension().location(),worldPosition,active));
    }

    private void refreshForgingVisual(boolean force) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner=forgingPlayerId==null?null:serverLevel.getServer().getPlayerList().getPlayer(forgingPlayerId);
        if(owner!=null && (!forging || !owner.isAlive() || owner.isRemoved()
                || !(owner.containerMenu instanceof DingMenu menu) || !menu.usesForge(this)
                || !(owner.level()==level || com.example.immortal_cultivation_mod.item.QiPouchWorld.isRemoteInteraction(owner,level,worldPosition))
                || !stillValid(owner))) owner=null;
        if(owner!=visualPlayer){clearForgingVisual();visualPlayer=owner;force=true;}
        if(visualPlayer!=null && (force || level.getGameTime()%10==0))sendForgingVisual(visualPlayer,true);
    }

    /** Closing the menu ends its pose and effects without cancelling the batch. */
    public void stopViewing(Player player) {
        if (visualPlayer == player) clearForgingVisual();
    }

    private void clearForgingVisual() {
        if(visualPlayer!=null && level!=null && !level.isClientSide)sendForgingVisual(visualPlayer,false);
        visualPlayer=null;
    }

    @Override public void setRemoved() {
        clearForgingVisual();
        super.setRemoved();
    }

    private void captureForgingSnapshot(ForgingSystem.ForgingRecipe recipe) {
        clearForgingSnapshot();
        int puritySeed = (int) (worldPosition.asLong()
                ^ (level == null ? 0L : level.getGameTime()));
        Map<net.minecraft.world.item.Item, Integer> remaining =
                new LinkedHashMap<>(recipe.required());
        int remainingSpiritStones = recipe.spiritStoneCount();
        for (int i = 0; i < materialSlotCount(); i++) {
            ItemStack stack = getItem(MATERIAL_START + i);
            if (stack.isEmpty()) {
                continue;
            }
            int needed = remaining.getOrDefault(stack.getItem(), 0);
            int available = ForgingSystem.canStackInForgingRecipe(stack)
                    ? stack.getCount()
                    : 1;
            int requiredUsed = Math.min(needed, available);
            int spiritStoneUsed = 0;
            if (remainingSpiritStones > 0
                    && ForgingSystem.isRecipeSpiritStone(stack)
                    && available > requiredUsed) {
                spiritStoneUsed = Math.min(
                        remainingSpiritStones, available - requiredUsed);
            }
            int used = requiredUsed + spiritStoneUsed;
            if(com.example.immortal_cultivation_mod.item.WeaponShardSystem.populated(stack))used=1;
            if(com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(recipe.id()) && com.example.immortal_cultivation_mod.alchemy.AlchemyMenu.plantId(stack)!=null)
                used=available;
            if (used <= 0 && !recipe.directOutput()
                    && ForgingSystem.isForgeMaterial(stack)) {
                // The old mold-based system still permits extra forge additives.
                used = 1;
            }
            if (used <= 0) {
                continue;
            }
            ForgingSystem.ensurePurity(stack, puritySeed + i * 9973);
            ItemStack snapshot = stack.copyWithCount(used);
            forgingSnapshot.set(i, snapshot);
            initialMaterialCounts[i] = used;
            if (requiredUsed > 0) {
                remaining.put(stack.getItem(), needed - requiredUsed);
            }
            remainingSpiritStones -= spiritStoneUsed;
        }
    }

    private void clearForgingSnapshot() {
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            forgingSnapshot.set(i, ItemStack.EMPTY);
            initialMaterialCounts[i] = 0;
            meltedMaterialCounts[i] = 0;
        }
    }

    public boolean hasFireBelow() {
        if (level == null) {
            return false;
        }
        BlockState below = level.getBlockState(worldPosition.below());
        return below.is(BlockTags.FIRE)
                || below.getBlock() instanceof BaseFireBlock
                || CampfireBlock.isLitCampfire(below)
                || below.is(Blocks.FIRE)
                || below.is(Blocks.SOUL_FIRE)
                || below.is(ModBlocks.LING_FIRE.get())
                || below.is(ModBlocks.ZHEN_FIRE.get())
                || below.getBlock() instanceof SpiritFlameBlock
                || below.is(Blocks.LAVA)
                || below.getBlock() instanceof AbstractFurnaceBlock
                    && below.hasProperty(BlockStateProperties.LIT) && below.getValue(BlockStateProperties.LIT);
    }

    public int lowestFireTemp() {
        return 0;
    }

    public int highestFireTemp() {
        return FlameKind.temperatureCeiling(currentFireType());
    }

    public int fireCost() {
        if (specialFire() != null) return specialFire().qiCost();
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.ZHEN_FIRE.get())) {
            return 40;
        }
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.LING_FIRE.get())) {
            return 20;
        }
        return 10;
    }

    private String currentFireType() {
        if (specialFire() != null) return specialFire().id();
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.ZHEN_FIRE.get())) {
            return "zhen_fire";
        }
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.LING_FIRE.get())) {
            return "ling_fire";
        }
        return "normal_fire";
    }

    private FlameKind specialFire() {
        return level != null && level.getBlockState(worldPosition.below()).getBlock() instanceof SpiritFlameBlock flame
                ? flame.kind() : null;
    }

    private ServerPlayer nearestPlayer() {
        if (level == null) {
            return null;
        }
        Player player = level.getNearestPlayer(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                functionalRange(), false);
        return player instanceof ServerPlayer sp ? sp : null;
    }

    /** Keeps every visible part of a scaled ding within its menu/forging range. */
    private double functionalRange() {
        int scale = getBlockState().hasProperty(GeoRockBlock.SCALE)
                ? getBlockState().getValue(GeoRockBlock.SCALE)
                : GeoRockBlock.MIN_SCALE;
        return Math.max(8.0D, scale * 3.0D);
    }

    private List<ItemStack> materialStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = MATERIAL_START;
             slot < MATERIAL_START + materialSlotCount();
             slot++) {
            stacks.add(getItem(slot));
        }
        return stacks;
    }

    private List<ItemStack> snapshotStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            ItemStack stack = forgingSnapshot.get(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return stacks;
    }

    public int data(int index) {
        return switch (index) {
            case 0 -> temperature;
            case 1 -> targetTemperature;
            case 2 -> progressTicks;
            case 3 -> maxProgressTicks;
            case 4 -> forging ? 1 : 0;
            case 5 -> lowestFireTemp();
            case 6 -> highestFireTemp();
            case 7 -> fireCost();
            case 8 -> materialSlotCount();
            default -> 0;
        };
    }

    @Override
    public Component getDisplayName() {
        if (getBlockState().is(ModBlocks.CHI_JIN_SANYANG_DING.get())) {
            return getBlockState().getBlock().getName();
        }
        return Component.translatable("container." + ImmortalCultivationMod.MODID + ".ding");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (hasLid() && !meshLidOpen) return null;
        return new DingMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    private boolean refreshDisplayItems() {
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack next = items.get(slot).copy();
            if (!ItemStack.matches(next, displayItems.get(slot))) {
                displayItems.set(slot, next);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // Progress and temperature changes do not resend identical display stacks.
        if (displayItems != null && refreshDisplayItems()) syncAnimationState();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (forging) {
            return false;
        }
        if (slot == YUJIAN_SLOT) {
            return ForgingSystem.recipe(stack) != null;
        }
        if (slot >= MATERIAL_START
                && slot < MATERIAL_START + MATERIAL_COUNT) {
            ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipe(getItem(YUJIAN_SLOT));
            return slot < MATERIAL_START + materialSlotCount()
                    && ForgingSystem.isIngredientForRecipe(recipe, stack);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        double range = functionalRange();
        return (!hasLid() || meshLidOpen) && level != null && level.getBlockEntity(worldPosition) == this
                && (player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= range * range
                || com.example.immortal_cultivation_mod.item.QiPouchWorld
                .isRemoteInteraction(player, level, worldPosition));
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("ManualLidOpen", hasLid() && meshLidOpen);
        tag.putInt("materialSlotVersion", 2);
        tag.putInt("temperature", temperature);
        tag.putInt("targetTemperature", targetTemperature);
        tag.putInt("progressTicks", progressTicks);
        tag.putInt("maxProgressTicks", maxProgressTicks);
        tag.putBoolean("forging", forging);
        tag.putString("activeRecipeId", activeRecipeId);
        tag.putString("activeFireType", activeFireType);
        tag.putLong("temperatureSamples", temperatureSamples);
        tag.putLong("temperatureTotal", temperatureTotal);
        if (forgingPlayerId != null) {
            tag.putUUID("forgingPlayer", forgingPlayerId);
        }
        CompoundTag snapshotTag = new CompoundTag();
        ContainerHelper.saveAllItems(snapshotTag, forgingSnapshot, registries);
        tag.put("forgingSnapshot", snapshotTag);
        tag.putIntArray("initialMaterialCounts", initialMaterialCounts);
        tag.putIntArray("meltedMaterialCounts", meltedMaterialCounts);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.getInt("materialSlotVersion") < 2) {
            // Preserve the old result slot when upgrading a ten-material forge.
            items.set(RESULT_SLOT, items.get(11));
            items.set(11, ItemStack.EMPTY);
        }
        meshViewers.clear();
        meshLidOpen = hasLid() && tag.getBoolean("ManualLidOpen");
        refreshDisplayItems();
        temperature = tag.getInt("temperature");
        targetTemperature = tag.getInt("targetTemperature");
        progressTicks = tag.getInt("progressTicks");
        maxProgressTicks = Math.max(20, tag.getInt("maxProgressTicks"));
        forging = tag.getBoolean("forging");
        activeRecipeId = tag.getString("activeRecipeId");
        activeFireType = tag.getString("activeFireType");
        if (activeFireType.isBlank()) {
            activeFireType = "normal_fire";
        }
        temperatureSamples = tag.getLong("temperatureSamples");
        temperatureTotal = tag.getLong("temperatureTotal");
        forgingPlayerId = tag.hasUUID("forgingPlayer") ? tag.getUUID("forgingPlayer") : null;
        clearForgingSnapshot();
        if (tag.contains("forgingSnapshot")) {
            ContainerHelper.loadAllItems(tag.getCompound("forgingSnapshot"), forgingSnapshot, registries);
        }
        int[] initial = tag.getIntArray("initialMaterialCounts");
        int[] melted = tag.getIntArray("meltedMaterialCounts");
        for (int i = 0; i < MATERIAL_COUNT; i++) {
            initialMaterialCounts[i] = i < initial.length ? initial[i] : 0;
            meltedMaterialCounts[i] = i < melted.length ? melted[i] : 0;
        }
    }
}
