package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ThrowingKnifeEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class ThrowingKnifeItem extends Item {
    public static final int MAX_KNIVES_PER_STACK = 6;
    private static final String STACKED_KNIVES_TAG =
            "ImmortalCultivationStackedThrowingKnives";

    public ThrowingKnifeItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .attributes(SwordItem.createAttributes(Tiers.IRON, 1.0F, -2.2F)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !throwFromHand(serverPlayer, hand, false)) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public static boolean throwHeavy(ServerPlayer player) {
        return throwFromHand(player, InteractionHand.MAIN_HAND, true);
    }

    private static boolean throwFromHand(
            ServerPlayer player, InteractionHand hand, boolean heavy) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof ThrowingKnifeItem)
                || player.getCooldowns().isOnCooldown(ModItems.THROWING_KNIFE.get())) {
            return false;
        }

        ItemStack thrownStack = player.getAbilities().instabuild
                ? copyKnifeForThrow(held, player.registryAccess())
                : takeKnife(held, player.registryAccess());
        if (thrownStack.isEmpty()) {
            return false;
        }
        ThrowingKnifeEntity projectile =
                new ThrowingKnifeEntity(player.serverLevel(), player, thrownStack, heavy);
        projectile.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                heavy ? 2.8F : 2.25F,
                0.0F);
        player.serverLevel().addFreshEntity(projectile);
        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                0.85F,
                heavy ? 0.82F : 1.15F);

        if (!player.getAbilities().instabuild) {
            player.getInventory().setChanged();
        }
        player.getCooldowns().addCooldown(ModItems.THROWING_KNIFE.get(), heavy ? 10 : 4);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack knife,
            ItemStack carried,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY
                || !(carried.getItem() instanceof ThrowingKnifeItem)) {
            return false;
        }
        normalizeLegacyStack(knife, player.registryAccess());
        normalizeLegacyStack(carried, player.registryAccess());
        if (knifeCount(knife) >= MAX_KNIVES_PER_STACK) {
            return true;
        }

        ItemStack stored = takeKnife(carried, player.registryAccess());
        if (!stored.isEmpty()) {
            List<ItemStack> knives = getStoredKnives(knife, player.registryAccess());
            knives.add(stored);
            setStoredKnives(knife, knives, player.registryAccess());
            carriedAccess.set(carried);
            slot.setChanged();
        }
        return true;
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        normalizeLegacyStack(stack, level.registryAccess());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        int count = knifeCount(stack);
        if (count > 1) {
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID
                            + ".throwing_knife_bundle",
                    count,
                    MAX_KNIVES_PER_STACK));
        }
    }

    public static int knifeCount(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ThrowingKnifeItem)) {
            return 0;
        }
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int stored = root.contains(STACKED_KNIVES_TAG, Tag.TAG_LIST)
                ? root.getList(STACKED_KNIVES_TAG, Tag.TAG_COMPOUND).size()
                : 0;
        return Math.min(
                MAX_KNIVES_PER_STACK,
                Math.max(1, stack.getCount()) + stored);
    }

    public static boolean tryStoreKnife(
            ItemStack bundle,
            ItemStack knife,
            HolderLookup.Provider registries) {
        if (bundle.isEmpty()
                || !(bundle.getItem() instanceof ThrowingKnifeItem)
                || knife.isEmpty()
                || !(knife.getItem() instanceof ThrowingKnifeItem)) {
            return false;
        }
        normalizeLegacyStack(bundle, registries);
        if (knifeCount(bundle) >= MAX_KNIVES_PER_STACK) {
            return false;
        }
        List<ItemStack> stored = getStoredKnives(bundle, registries);
        stored.add(standaloneKnife(knife));
        setStoredKnives(bundle, stored, registries);
        return true;
    }

    private static ItemStack takeKnife(
            ItemStack bundle,
            HolderLookup.Provider registries) {
        if (bundle.isEmpty()
                || !(bundle.getItem() instanceof ThrowingKnifeItem)) {
            return ItemStack.EMPTY;
        }
        normalizeLegacyStack(bundle, registries);
        List<ItemStack> stored = getStoredKnives(bundle, registries);
        if (!stored.isEmpty()) {
            ItemStack knife = stored.remove(stored.size() - 1);
            setStoredKnives(bundle, stored, registries);
            return standaloneKnife(knife);
        }

        ItemStack knife = standaloneKnife(bundle);
        bundle.shrink(1);
        return knife;
    }

    private static ItemStack copyKnifeForThrow(
            ItemStack bundle,
            HolderLookup.Provider registries) {
        List<ItemStack> stored = getStoredKnives(bundle, registries);
        return stored.isEmpty()
                ? standaloneKnife(bundle)
                : standaloneKnife(stored.get(stored.size() - 1));
    }

    private static void normalizeLegacyStack(
            ItemStack stack,
            HolderLookup.Provider registries) {
        if (stack.isEmpty()
                || !(stack.getItem() instanceof ThrowingKnifeItem)
                || stack.getCount() <= 1) {
            return;
        }
        int legacyExtras = Math.min(
                stack.getCount() - 1,
                MAX_KNIVES_PER_STACK - 1);
        ItemStack template = standaloneKnife(stack);
        List<ItemStack> stored = getStoredKnives(stack, registries);
        int capacity = MAX_KNIVES_PER_STACK - 1 - stored.size();
        for (int index = 0; index < Math.min(legacyExtras, capacity); index++) {
            stored.add(template.copy());
        }
        stack.setCount(1);
        setStoredKnives(stack, stored, registries);
    }

    private static List<ItemStack> getStoredKnives(
            ItemStack bundle,
            HolderLookup.Provider registries) {
        CompoundTag root = bundle.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = root.getList(STACKED_KNIVES_TAG, Tag.TAG_COMPOUND);
        List<ItemStack> knives = new ArrayList<>(list.size());
        for (int index = 0;
                index < list.size() && knives.size() < MAX_KNIVES_PER_STACK - 1;
                index++) {
            ItemStack knife = ItemStack.parseOptional(
                    registries, list.getCompound(index));
            if (knife.getItem() instanceof ThrowingKnifeItem) {
                knives.add(standaloneKnife(knife));
            }
        }
        return knives;
    }

    private static void setStoredKnives(
            ItemStack bundle,
            List<ItemStack> knives,
            HolderLookup.Provider registries) {
        CompoundTag root = bundle.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = new ListTag();
        for (ItemStack knife : knives) {
            if (list.size() >= MAX_KNIVES_PER_STACK - 1) {
                break;
            }
            if (!knife.isEmpty()
                    && knife.getItem() instanceof ThrowingKnifeItem) {
                list.add(standaloneKnife(knife).saveOptional(registries));
            }
        }
        if (list.isEmpty()) {
            root.remove(STACKED_KNIVES_TAG);
        } else {
            root.put(STACKED_KNIVES_TAG, list);
        }
        if (root.isEmpty()) {
            bundle.remove(DataComponents.CUSTOM_DATA);
        } else {
            bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    private static ItemStack standaloneKnife(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack knife = stack.copyWithCount(1);
        CompoundTag root = knife.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(STACKED_KNIVES_TAG);
        if (root.isEmpty()) {
            knife.remove(DataComponents.CUSTOM_DATA);
        } else {
            knife.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return knife;
    }
}
