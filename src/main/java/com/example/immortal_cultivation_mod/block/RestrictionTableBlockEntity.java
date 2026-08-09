package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.achievement.NoobAchievement;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.RestrictionInsightItem;
import com.example.immortal_cultivation_mod.item.RestrictionPuzzleLogic;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.network.RestrictionPayloads;
import com.example.immortal_cultivation_mod.screen.RestrictionTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RestrictionTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int WEAPON_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    public static final int MOVE_QI_COST = 300;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private UUID activePlayer;
    private long activeToken;
    private int[] pendingAdditions = new int[WeaponRestrictionSystem.RestrictionType.values().length];
    private RestrictionPuzzleLogic.Puzzle activePuzzle;

    public RestrictionTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESTRICTION_TABLE.get(), pos, state);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
                                  RestrictionTableBlockEntity table) {
        if (table.activePlayer == null || level.getGameTime() % 20L != 0L) {
            return;
        }
        ServerPlayer player = level.getServer() == null
                ? null
                : level.getServer().getPlayerList().getPlayer(table.activePlayer);
        if (player == null || player.level() != level || player.distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            if (player != null) {
                table.failPuzzle(player);
            }
            table.clearSession();
        }
    }

    public void startPuzzle(ServerPlayer player, int[] additions) {
        if (level == null || level.isClientSide || !stillValid(player) || activePlayer != null
                || additions == null || additions.length != pendingAdditions.length) {
            return;
        }
        ItemStack weapon = getItem(WEAPON_SLOT);
        int total = Arrays.stream(additions).sum();
        if (!WeaponRestrictionSystem.isWeapon(weapon) || total <= 0
                || total > WeaponRestrictionSystem.remainingCapacity(weapon)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".restriction.invalid_selection"), true);
            return;
        }
        for (int addition : additions) {
            if (addition < 0) {
                return;
            }
        }

        activePlayer = player.getUUID();
        activeToken = player.getRandom().nextLong() ^ level.getGameTime() ^ worldPosition.asLong();
        pendingAdditions = Arrays.copyOf(additions, additions.length);
        activePuzzle = RestrictionPuzzleLogic.create(player.getRandom(),
                WeaponRestrictionSystem.totalLayers(weapon), total,
                NoobAchievement.hasReward(player));
        setChanged();
        PacketDistributor.sendToPlayer(player, new RestrictionPayloads.ClientboundOpenPuzzle(
                worldPosition,
                activeToken,
                activePuzzle.firstLayer(),
                activePuzzle.attemptedLayers(),
                activePuzzle.difficulty().ordinal(),
                activePuzzle.moveLimit(),
                activePuzzle.instabilityInterval(),
                BuiltInRegistries.ITEM.getKey(weapon.getItem()),
                RestrictionInsightItem.hintCount(player),
                activePuzzle.valuesList(),
                activePuzzle.targetsList(),
                activePuzzle.flagsList(),
                flattenLinks(activePuzzle.links())));
    }

    public void movePuzzle(ServerPlayer player, long token, int node, int direction) {
        if (!isActiveFor(player, token) || activePuzzle == null) {
            return;
        }
        if (!activePuzzle.canMove(node, direction)) {
            sendState(player, RestrictionPayloads.RESULT_INVALID_MOVE, false, false);
            return;
        }
        if (!chargeQi(player, MOVE_QI_COST)) {
            sendState(player, RestrictionPayloads.RESULT_NOT_ENOUGH_QI, false, false);
            return;
        }
        RestrictionPuzzleLogic.MoveOutcome outcome = activePuzzle.move(node, direction);
        resolveMove(player, outcome, false);
    }

    public void hintPuzzle(ServerPlayer player, long token) {
        if (!isActiveFor(player, token) || activePuzzle == null) {
            return;
        }
        if (RestrictionInsightItem.hintCount(player) <= 0) {
            sendState(player, RestrictionPayloads.RESULT_NO_HINTS, false, false);
            return;
        }
        RestrictionPuzzleLogic.HintMove hint = activePuzzle.hintMove();
        if (hint == null) {
            sendState(player, RestrictionPayloads.RESULT_NO_HINT_MOVE, false, false);
            return;
        }
        if (!RestrictionInsightItem.consumeHint(player)) {
            sendState(player, RestrictionPayloads.RESULT_NO_HINTS, false, false);
            return;
        }
        resolveMove(player, hint.outcome(), true);
    }

    private void resolveMove(ServerPlayer player, RestrictionPuzzleLogic.MoveOutcome outcome,
                             boolean hintUsed) {
        if (outcome.solved()) {
            if (!WeaponRestrictionSystem.addRestrictions(getItem(WEAPON_SLOT), pendingAdditions)) {
                int failure = failPuzzle(player);
                sendState(player, failure, false, hintUsed);
                clearSession();
                return;
            }
            setChanged();
            playSuccessEffects();
            sendState(player, RestrictionPayloads.RESULT_SOLVED,
                    outcome.instabilityShifted(), hintUsed);
            clearSession();
            return;
        }
        if (outcome.exhausted()) {
            int failure = failPuzzle(player);
            sendState(player, failure, outcome.instabilityShifted(), hintUsed);
            clearSession();
            return;
        }
        sendState(player, RestrictionPayloads.RESULT_MOVED,
                outcome.instabilityShifted(), hintUsed);
    }

    public void cancelPuzzle(ServerPlayer player, long token) {
        if (isActiveFor(player, token)) {
            failPuzzle(player);
            clearSession();
        }
    }

    public void evolveWeapon(ServerPlayer player) {
        if (activePlayer != null || !stillValid(player)) {
            return;
        }
        if (WeaponRestrictionSystem.evolve(getItem(WEAPON_SLOT))) {
            setChanged();
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".restriction.evolved"), true);
        }
    }

    public int data(int index) {
        ItemStack weapon = getItem(WEAPON_SLOT);
        return switch (index) {
            case 0 -> WeaponRestrictionSystem.tier(weapon);
            case 1 -> WeaponRestrictionSystem.tierProgress(weapon);
            case 2 -> activePlayer == null ? 0 : 1;
            default -> 0;
        };
    }

    private boolean isActiveFor(ServerPlayer player, long token) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && activePlayer != null && activePlayer.equals(player.getUUID())
                && activeToken == token && player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    private boolean chargeQi(ServerPlayer player, int cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        var data = ModAttachments.getData(player);
        long available = data.qi();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && available < cost; slot++) {
            ItemStack stack = inventory.getItem(slot);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int restore = ServerEvents.magicRitualSpiritStoneRestoreAmount(key);
            if (restore > 0) {
                if (ServerEvents.isInfiniteMagicRitualSpiritStone(key)) {
                    available = cost;
                    break;
                }
                available += (long) restore * stack.getCount();
            }
        }
        if (available < cost) {
            return false;
        }

        int qiSpent = Math.min(cost, data.qi());
        if (qiSpent > 0) {
            ModAttachments.setData(player, data.withQi(data.qi() - qiSpent));
        }
        int remaining = cost - qiSpent;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int restore = ServerEvents.magicRitualSpiritStoneRestoreAmount(key);
            if (ServerEvents.isInfiniteMagicRitualSpiritStone(key) && remaining > 0) {
                remaining = 0;
                break;
            }
            while (restore > 0 && !stack.isEmpty() && remaining > 0) {
                stack.shrink(1);
                remaining -= restore;
            }
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    private void sendState(ServerPlayer player, int result, boolean instabilityShifted,
                           boolean hintUsed) {
        if (activePuzzle == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new RestrictionPayloads.ClientboundPuzzleState(
                activeToken,
                activePuzzle.valuesList(),
                activePuzzle.movesUsed(),
                result,
                instabilityShifted,
                RestrictionInsightItem.hintCount(player),
                hintUsed));
    }

    private int failPuzzle(ServerPlayer player) {
        if (activePuzzle == null || player.getAbilities().instabuild) {
            return RestrictionPayloads.RESULT_FAILED_MINOR;
        }
        double mismatch = activePuzzle.mismatchRatio();
        int result;
        int qiLoss;
        int stones;
        float healthFraction;
        double durabilityFraction;
        if (mismatch < 0.20D) {
            result = RestrictionPayloads.RESULT_FAILED_MINOR;
            qiLoss = 150;
            stones = 1;
            healthFraction = 0.0F;
            durabilityFraction = 0.0D;
        } else if (mismatch < 0.48D) {
            result = RestrictionPayloads.RESULT_FAILED_MAJOR;
            qiLoss = 600;
            stones = 2;
            healthFraction = 0.05F;
            durabilityFraction = 0.03D;
        } else {
            result = RestrictionPayloads.RESULT_FAILED_CATASTROPHIC;
            qiLoss = 1200;
            stones = 3;
            healthFraction = 0.15F;
            durabilityFraction = 0.10D;
        }

        drainAvailableQi(player, qiLoss);
        consumeSpiritStones(player, stones);
        if (healthFraction > 0.0F) {
            float wanted = Math.max(2.0F, player.getMaxHealth() * healthFraction);
            float safeDamage = Math.min(wanted, Math.max(0.0F, player.getHealth() - 1.0F));
            if (safeDamage > 0.0F) {
                player.hurt(player.damageSources().magic(), safeDamage);
            }
        }
        damageArtifact(durabilityFraction);
        if (result == RestrictionPayloads.RESULT_FAILED_CATASTROPHIC
                && activePuzzle.difficulty() == RestrictionPuzzleLogic.Difficulty.EXTREME
                && player.getRandom().nextFloat() < 0.20F
                && WeaponRestrictionSystem.removeOneRestrictionLayer(
                getItem(WEAPON_SLOT), player.getRandom())) {
            result = RestrictionPayloads.RESULT_FAILED_LAYER_DAMAGED;
        }
        playFailureEffects(result);
        player.displayClientMessage(Component.translatable(resultMessage(result)), true);
        setChanged();
        return result;
    }

    private void drainAvailableQi(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        var data = ModAttachments.getData(player);
        int spent = Math.min(amount, data.qi());
        if (spent > 0) {
            ModAttachments.setData(player, data.withQi(data.qi() - spent));
            ServerEvents.syncPlayerData(player);
        }
    }

    private void consumeSpiritStones(ServerPlayer player, int count) {
        Inventory inventory = player.getInventory();
        for (int consumed = 0; consumed < count; consumed++) {
            int bestSlot = -1;
            int lowestRestore = Integer.MAX_VALUE;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                int restore = ServerEvents.magicRitualSpiritStoneRestoreAmount(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (restore > 0
                        && !ServerEvents.isInfiniteMagicRitualSpiritStone(
                                BuiltInRegistries.ITEM.getKey(stack.getItem()))
                        && restore < lowestRestore) {
                    bestSlot = slot;
                    lowestRestore = restore;
                }
            }
            if (bestSlot < 0) {
                return;
            }
            inventory.getItem(bestSlot).shrink(1);
        }
    }

    private void damageArtifact(double fraction) {
        ItemStack weapon = getItem(WEAPON_SLOT);
        if (fraction <= 0.0D || !weapon.isDamageableItem()) {
            return;
        }
        int damage = Math.max(1, (int) Math.ceil(weapon.getMaxDamage() * fraction));
        weapon.setDamageValue(Math.min(weapon.getMaxDamage() - 1,
                weapon.getDamageValue() + damage));
    }

    private void playSuccessEffects() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 1.1D;
        double z = worldPosition.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z,
                60, 0.8D, 0.55D, 0.8D, 0.15D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z,
                24, 0.45D, 0.5D, 0.45D, 0.04D);
        serverLevel.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 1.15F);
    }

    private void playFailureEffects(int result) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 1.0D;
        double z = worldPosition.getZ() + 0.5D;
        int count = result == RestrictionPayloads.RESULT_FAILED_MINOR ? 12 : 36;
        serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z,
                count, 0.55D, 0.45D, 0.55D, 0.04D);
        if (result != RestrictionPayloads.RESULT_FAILED_MINOR) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
                    18, 0.45D, 0.45D, 0.45D, 0.04D);
            serverLevel.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS, 0.65F, 1.4F);
        }
    }

    private static String resultMessage(int result) {
        String suffix = switch (result) {
            case RestrictionPayloads.RESULT_FAILED_MAJOR -> "failed_major";
            case RestrictionPayloads.RESULT_FAILED_CATASTROPHIC -> "failed_catastrophic";
            case RestrictionPayloads.RESULT_FAILED_LAYER_DAMAGED -> "failed_layer";
            default -> "failed_minor";
        };
        return "message." + ImmortalCultivationMod.MODID + ".restriction." + suffix;
    }

    private void clearSession() {
        activePlayer = null;
        activeToken = 0L;
        Arrays.fill(pendingAdditions, 0);
        activePuzzle = null;
        setChanged();
    }

    private static List<Integer> flattenLinks(List<RestrictionPuzzleLogic.Link> links) {
        List<Integer> values = new ArrayList<>(links.size() * 3);
        for (RestrictionPuzzleLogic.Link link : links) {
            values.add(link.first());
            values.add(link.second());
            values.add(link.polarity());
        }
        return List.copyOf(values);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ImmortalCultivationMod.MODID + ".restriction_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RestrictionTableMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.getFirst().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (activePlayer != null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (activePlayer != null) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (activePlayer != null) {
            return;
        }
        items.set(slot, stack);
        if (stack.getCount() > 1) {
            stack.setCount(1);
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        if (activePlayer == null) {
            items.clear();
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        clearSession();
    }
}
