package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.IdentityItems;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DingBlockEntity extends GeoRockBlockEntity implements Container, MenuProvider {
    public static final int YUJIAN_SLOT = 0;
    public static final int MATERIAL_START = 1;
    public static final int MATERIAL_COUNT = 10;
    public static final int RESULT_SLOT = 11;
    public static final int SLOT_COUNT = 12;

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

    public DingBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DING.get(), pos, blockState);
    }

    public int materialSlotCount() {
        return getBlockState().is(ModBlocks.CU_TIE_XIAODING.get())
                ? 5
                : MATERIAL_COUNT;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, DingBlockEntity ding) {
        if (!ding.forging) {
            return;
        }
        if (!ding.hasFireBelow() || !ding.canContinue()) {
            ding.finishForging(true);
            return;
        }
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
        if (ding.temperature > ding.lowestFireTemp()) {
            ding.temperature = Math.max(ding.lowestFireTemp(), ding.temperature - 35);
        }
        ding.progressTicks++;
        ding.meltVisibleMaterials();
        if (ding.progressTicks >= ding.maxProgressTicks) {
            ding.finishForging(false);
        }
        ding.setChanged();
    }

    public void startForging(Player player) {
        if (forging || level == null || level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return;
        }
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipe(getItem(YUJIAN_SLOT));
        if (recipe == null || !hasFireBelow() || !ForgingSystem.hasRequiredMaterials(recipe, materialStacks()) || !getItem(RESULT_SLOT).isEmpty()) {
            return;
        }
        if (!chargeQi(sp, 100)) {
            return;
        }
        captureForgingSnapshot(recipe);
        forgingPlayerId = sp.getUUID();
        activeRecipeId = recipe.id();
        activeFireType = currentFireType();
        targetTemperature = recipe.averageTemp();
        temperature = lowestFireTemp();
        maxProgressTicks = ForgingSystem.meltTicks(snapshotStacks());
        progressTicks = 0;
        temperatureSamples = 0;
        temperatureTotal = 0;
        forging = true;
        setChanged();
    }

    public void heatPulse(Player player) {
        if (!forging || level == null || level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!chargeQi(sp, fireCost())) {
            return;
        }
        temperature = Math.min(highestFireTemp(), temperature + Math.max(1, (highestFireTemp() - lowestFireTemp()) / 10));
        setChanged();
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
        return !activeRecipeId.isEmpty() && getItem(RESULT_SLOT).isEmpty();
    }

    private void finishForging(boolean failedEarly) {
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipeById(activeRecipeId);
        if (recipe == null) {
            stopForging();
            return;
        }
        meltAllRemainingRequiredMaterials();
        if (failedEarly && recipe.directOutput()) {
            stopForging();
            return;
        }
        double avg = temperatureSamples <= 0 ? temperature : (double) temperatureTotal / (double) temperatureSamples;
        if (failedEarly && temperatureSamples <= 0) {
            avg = temperature;
        }
        setItem(RESULT_SLOT, ForgingSystem.createMold(recipe, snapshotStacks(), avg, activeFireType));
        if (!failedEarly) {
            awardForgingProficiency();
        }
        stopForging();
    }

    private void awardForgingProficiency() {
        if (!(level instanceof ServerLevel serverLevel) || forgingPlayerId == null) {
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(forgingPlayerId);
        if (player != null) {
            IdentityItems.recordSuccessfulForging(player);
        }
    }

    private void meltVisibleMaterials() {
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
                visible.shrink(Math.min(delta, visible.getCount()));
            }
            meltedMaterialCounts[i] += delta;
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
                visible.shrink(Math.min(remaining, visible.getCount()));
            }
            meltedMaterialCounts[i] = initialMaterialCounts[i];
        }
    }

    private void stopForging() {
        forging = false;
        progressTicks = 0;
        activeRecipeId = "";
        activeFireType = "normal_fire";
        forgingPlayerId = null;
        clearForgingSnapshot();
        setChanged();
    }

    private void captureForgingSnapshot(ForgingSystem.ForgingRecipe recipe) {
        clearForgingSnapshot();
        int puritySeed = (int) (worldPosition.asLong()
                ^ (level == null ? 0L : level.getGameTime()));
        if (!recipe.directOutput()) {
            for (int i = 0; i < materialSlotCount(); i++) {
                ItemStack stack = getItem(MATERIAL_START + i);
                if (stack.isEmpty()) {
                    continue;
                }
                ForgingSystem.ensurePurity(stack, puritySeed + i * 9973);
                ItemStack snapshot = stack.copy();
                forgingSnapshot.set(i, snapshot);
                initialMaterialCounts[i] = snapshot.getCount();
            }
            return;
        }

        Map<net.minecraft.world.item.Item, Integer> remaining =
                new LinkedHashMap<>(recipe.required());
        int remainingSpiritStones = recipe.spiritStoneCount();
        for (int i = 0; i < materialSlotCount(); i++) {
            ItemStack stack = getItem(MATERIAL_START + i);
            if (stack.isEmpty()) {
                continue;
            }
            int needed = remaining.getOrDefault(stack.getItem(), 0);
            boolean spiritStoneIngredient = remainingSpiritStones > 0
                    && ForgingSystem.isRecipeSpiritStone(stack);
            if (needed <= 0 && !spiritStoneIngredient) {
                continue;
            }
            int used = needed > 0
                    ? Math.min(needed, stack.getCount())
                    : Math.min(remainingSpiritStones, stack.getCount());
            if (used <= 0) {
                continue;
            }
            ForgingSystem.ensurePurity(stack, puritySeed + i * 9973);
            ItemStack snapshot = stack.copyWithCount(used);
            forgingSnapshot.set(i, snapshot);
            initialMaterialCounts[i] = used;
            if (needed > 0) {
                remaining.put(stack.getItem(), needed - used);
            } else {
                remainingSpiritStones -= used;
            }
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
                || below.is(ModBlocks.LING_FIRE.get());
    }

    public int lowestFireTemp() {
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.LING_FIRE.get())) {
            return 1500;
        }
        return 600;
    }

    public int highestFireTemp() {
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.LING_FIRE.get())) {
            return 4000;
        }
        return 2000;
    }

    public int fireCost() {
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.LING_FIRE.get())) {
            return 20;
        }
        return 10;
    }

    private String currentFireType() {
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.LING_FIRE.get())) {
            return "ling_fire";
        }
        return "normal_fire";
    }

    private ServerPlayer nearestPlayer() {
        if (level == null) {
            return null;
        }
        Player player = level.getNearestPlayer(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 8.0D, false);
        return player instanceof ServerPlayer sp ? sp : null;
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
        return Component.translatable("container." + ImmortalCultivationMod.MODID + ".ding");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DingMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
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
        return ContainerHelper.takeItem(items, slot);
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
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
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
