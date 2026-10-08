package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import software.bernie.geckolib.animatable.GeoItem;

/** Persistent identity, purity, artifact linking, and recall for the qi pouch. */
public final class QiPouchStorage {
    public static final int LEGACY_SLOTS = 108;
    public static final double RECALL_RANGE = 50.0D;
    public static final int RECALL_DELAY_TICKS = 200;

    private static final String POUCH_ID_TAG = "immortal_cultivation_mod:qi_pouch_id";
    private static final String POUCH_OWNER_TAG = "immortal_cultivation_mod:qi_pouch_owner";
    private static final String LATENT_PURITY_TAG =
            "immortal_cultivation_mod:qi_pouch_latent_purity";
    private static final String LAST_CHEST_TAB_TAG =
            "immortal_cultivation_mod:qi_pouch_last_chest_tab";
    private static final String LINK_TAG = "immortal_cultivation_mod:qi_pouch_link";
    private static final String LINK_POUCH_TAG = "Pouch";
    private static final String LINK_OWNER_TAG = "Owner";

    private QiPouchStorage() {
    }

    public static UUID ensureIdentity(ItemStack pouch, ServerPlayer holder) {
        if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())) {
            return null;
        }
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID pouchId = root.hasUUID(POUCH_ID_TAG)
                ? root.getUUID(POUCH_ID_TAG) : UUID.randomUUID();
        boolean changed = !root.hasUUID(POUCH_ID_TAG)
                || !root.hasUUID(POUCH_OWNER_TAG)
                || !holder.getUUID().equals(root.getUUID(POUCH_OWNER_TAG));
        root.putUUID(POUCH_ID_TAG, pouchId);
        root.putUUID(POUCH_OWNER_TAG, holder.getUUID());
        if (changed) {
            pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return pouchId;
    }

    public static UUID pouchId(ItemStack pouch) {
        if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())) return null;
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.hasUUID(POUCH_ID_TAG) ? root.getUUID(POUCH_ID_TAG) : null;
    }

    public static boolean playerHasPouch(ServerPlayer player, UUID requestedPouchId) {
        return requestedPouchId != null && !findPouch(player, requestedPouchId).isEmpty();
    }

    public static boolean opensOnChestTab(ItemStack pouch) {
        if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())) return false;
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.getBoolean(LAST_CHEST_TAB_TAG);
    }

    public static void setOpensOnChestTab(ItemStack pouch, boolean chestTab) {
        if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())) return;
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (chestTab) {
            root.putBoolean(LAST_CHEST_TAB_TAG, true);
        } else {
            root.remove(LAST_CHEST_TAB_TAG);
        }
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static boolean hasPurity(ItemStack pouch) {
        if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())) return false;
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(ForgingSystem.TAG_PURITY, net.minecraft.nbt.Tag.TAG_INT);
    }

    public static int purity(ItemStack pouch) {
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(1, Math.min(100,
                root.getInt(ForgingSystem.TAG_PURITY)));
    }

    public static int ensurePurity(ItemStack pouch, ServerPlayer holder) {
        UUID pouchId = ensureIdentity(pouch, holder);
        if (pouchId == null) return 1;
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ForgingSystem.TAG_PURITY, net.minecraft.nbt.Tag.TAG_INT)) {
            CompoundTag forging = root.getCompound(ForgingSystem.TAG_FORGING);
            int forgedPurity = forging.contains("average_purity", Tag.TAG_ANY_NUMERIC)
                    ? Mth.clamp((int) Math.round(forging.getDouble("average_purity")), 1, 100)
                    : 0;
            int revealed;
            if (root.contains(LATENT_PURITY_TAG, Tag.TAG_INT)) {
                revealed = root.getInt(LATENT_PURITY_TAG);
            } else if (forgedPurity > 0) {
                revealed = forgedPurity;
            } else {
                revealed = 1 + Math.floorMod(pouchId.hashCode() * 31
                        + holder.getUUID().hashCode() * 17, 100);
            }
            root.putInt(ForgingSystem.TAG_PURITY,
                    Math.max(1, Math.min(100, revealed)));
            root.remove(LATENT_PURITY_TAG);
            pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return purity(pouch);
    }

    public static void setLatentPurity(ItemStack pouch, int purity) {
        if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())) return;
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ForgingSystem.TAG_PURITY, net.minecraft.nbt.Tag.TAG_INT)) {
            root.putInt(LATENT_PURITY_TAG,
                    Math.max(1, Math.min(100, purity)));
            pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    public static boolean isFloorItem(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(ModItems.QI_POUCH.get())
                && (stack.getItem() instanceof BlockItem
                || stack.getItem() instanceof GeoItem
                || FlyingSwordEntity.isFlyingWeapon(stack));
    }

    public static void linkArtifact(
            ItemStack pouch, ItemStack artifact, UUID ownerId) {
        UUID pouchId = pouchId(pouch);
        if (pouchId == null || ownerId == null || artifact.isEmpty()
                || !FlyingSwordEntity.isFlyingWeapon(artifact)) {
            return;
        }
        CompoundTag root = artifact.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag link = new CompoundTag();
        link.putUUID(LINK_POUCH_TAG, pouchId);
        link.putUUID(LINK_OWNER_TAG, ownerId);
        root.put(LINK_TAG, link);
        artifact.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static Link linkedPouch(ItemStack artifact) {
        if (artifact.isEmpty()) return null;
        CompoundTag root = artifact.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(LINK_TAG)) return null;
        CompoundTag link = root.getCompound(LINK_TAG);
        if (!link.hasUUID(LINK_POUCH_TAG) || !link.hasUUID(LINK_OWNER_TAG)) {
            return null;
        }
        return new Link(link.getUUID(LINK_POUCH_TAG), link.getUUID(LINK_OWNER_TAG));
    }

    public static boolean insertRecalled(
            ServerPlayer owner, UUID requestedPouchId, ItemStack artifact) {
        if (requestedPouchId == null || artifact.isEmpty()
                || !FlyingSwordEntity.isFlyingWeapon(artifact)) {
            return false;
        }
        if (owner.containerMenu instanceof OpenPouchAccess open
                && requestedPouchId.equals(open.pouchId())) {
            return open.insertRecalled(artifact.copyWithCount(1), owner);
        }
        ItemStack pouch = findPouch(owner, requestedPouchId);
        if (pouch.isEmpty()) return false;
        ItemStack stored = artifact.copyWithCount(1);
        linkArtifact(pouch, stored, owner.getUUID());
        return QiPouchWorld.insertArtifact(pouch, owner, stored);
    }

    public static boolean tryRecallFlyingSword(FlyingSwordEntity sword) {
        if (!(sword.level() instanceof ServerLevel level)) return false;
        ItemStack weapon = sword.pouchRecallStack();
        Link link = linkedPouch(weapon);
        if (link == null || sword.getOwnerUuid() == null
                || !link.ownerId().equals(sword.getOwnerUuid())) {
            return false;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(link.ownerId());
        if (owner == null || owner.level() != level
                || sword.distanceToSqr(owner) > RECALL_RANGE * RECALL_RANGE
                || !insertRecalled(owner, link.pouchId(), weapon)) {
            return false;
        }
        recallVfx(level, sword.getX(), sword.getY(), sword.getZ());
        return true;
    }

    public static boolean tickLinkedItemEntity(ItemEntity itemEntity) {
        if (!(itemEntity.level() instanceof ServerLevel level)
                || itemEntity.getAge() < RECALL_DELAY_TICKS
                || itemEntity.tickCount % 20 != 0) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        Link link = linkedPouch(stack);
        if (link == null) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(link.ownerId());
        if (owner == null || owner.level() != level
                || itemEntity.distanceToSqr(owner) > RECALL_RANGE * RECALL_RANGE
                || !insertRecalled(owner, link.pouchId(), stack)) {
            return false;
        }
        stack.shrink(1);
        if (stack.isEmpty()) itemEntity.discard();
        else itemEntity.setItem(stack);
        recallVfx(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        return true;
    }

    public static ItemStack findPouch(ServerPlayer player, UUID requestedPouchId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.is(ModItems.QI_POUCH.get())
                    && requestedPouchId.equals(pouchId(candidate))) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remaining = stack.copy();
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) player.drop(remaining, false);
    }

    private static void recallVfx(
            ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y + 0.3D, z,
                24, 0.32D, 0.35D, 0.32D, 0.22D);
        level.playSound(null, x, y, z, SoundEvents.ENDER_CHEST_CLOSE,
                SoundSource.PLAYERS, 0.65F, 1.45F);
    }

    public interface OpenPouchAccess {
        UUID pouchId();

        boolean insertRecalled(ItemStack artifact, ServerPlayer owner);
    }

    public record Link(UUID pouchId, UUID ownerId) {
    }
}
