package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class SwordCaseItem extends Item implements GeoItem, ICurioItem {
    private static final String BACK_ACCESSORY = "back_accessory";
    private static final String STORED_WEAPONS = ImmortalCultivationMod.MODID + ".sword_case_weapons";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SwordCaseItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isEquipped(net.minecraft.world.entity.player.Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler(BACK_ACCESSORY)
                        .map(back -> {
                            for (int slot = 0; slot < back.getStacks().getSlots(); slot++) {
                                if (back.getStacks().getStackInSlot(slot).is(ModItems.SWORD_CASE.get())) {
                                    return true;
                                }
                            }
                            return false;
                        }))
                .orElse(false);
    }

    public static boolean handleAction(ServerPlayer player, boolean releaseAll) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler(BACK_ACCESSORY)
                        .map(back -> {
                            for (int slot = 0; slot < back.getStacks().getSlots(); slot++) {
                                ItemStack equipped = back.getStacks().getStackInSlot(slot);
                                if (!equipped.is(ModItems.SWORD_CASE.get())) {
                                    continue;
                                }
                                ItemStack swordCase = equipped.copy();
                                boolean handled = releaseAll
                                        ? releaseOrRecallAll(player, swordCase)
                                        : cycleWeapon(player, swordCase);
                                if (handled) {
                                    handler.setEquippedCurio(BACK_ACCESSORY, slot, swordCase);
                                }
                                return handled;
                            }
                            return false;
                        }))
                .orElse(false);
    }

    private static boolean cycleWeapon(ServerPlayer player, ItemStack swordCase) {
        ItemStack hand = player.getMainHandItem();
        if (!hand.isEmpty() && !FlyingSwordEntity.isFlyingWeapon(hand)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".sword_case_weapon_only"), true);
            return false;
        }

        List<ItemStack> stored = getStoredWeapons(swordCase, player.registryAccess());
        if (hand.isEmpty()) {
            if (stored.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".sword_case_empty"), true);
                return false;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, stored.removeFirst());
        } else {
            ItemStack next = stored.isEmpty() ? ItemStack.EMPTY : stored.removeFirst();
            stored.add(hand.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, next);
        }
        setStoredWeapons(swordCase, stored, player.registryAccess());
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".sword_case_cycled", stored.size()), true);
        return true;
    }

    private static boolean releaseOrRecallAll(ServerPlayer player, ItemStack swordCase) {
        List<ItemStack> stored = getStoredWeapons(swordCase, player.registryAccess());
        if (stored.isEmpty()) {
            List<ItemStack> recalled = FlyingSwordEntity.collectControlledForCase(player);
            if (recalled.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".sword_case_empty"), true);
                return false;
            }
            setStoredWeapons(swordCase, recalled, player.registryAccess());
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".sword_case_recalled", recalled.size()), true);
            return true;
        }

        int released = FlyingSwordEntity.releaseControlledFromCase(player, stored);
        if (released <= 0) {
            return false;
        }
        stored.subList(0, released).clear();
        setStoredWeapons(swordCase, stored, player.registryAccess());
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".sword_case_released", released), true);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack swordCase, ItemStack carried, Slot slot,
                                             ClickAction action, net.minecraft.world.entity.player.Player player,
                                             SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || !FlyingSwordEntity.isFlyingWeapon(carried)) {
            return false;
        }

        List<ItemStack> stored = getStoredWeapons(swordCase, player.registryAccess());
        stored.add(carried.copyWithCount(1));
        carried.shrink(1);
        carriedAccess.set(carried);
        setStoredWeapons(swordCase, stored, player.registryAccess());
        slot.setChanged();
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".sword_case_stored", stored.size()), true);
        }
        return true;
    }

    public static int storedWeaponCount(ItemStack swordCase) {
        CompoundTag root = swordCase.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(STORED_WEAPONS, Tag.TAG_LIST)
                ? root.getList(STORED_WEAPONS, Tag.TAG_COMPOUND).size() : 0;
    }

    private static List<ItemStack> getStoredWeapons(ItemStack swordCase, HolderLookup.Provider registries) {
        CompoundTag root = swordCase.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = root.getList(STORED_WEAPONS, Tag.TAG_COMPOUND);
        List<ItemStack> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (FlyingSwordEntity.isFlyingWeapon(stack)) {
                result.add(stack);
            }
        }
        return result;
    }

    private static void setStoredWeapons(ItemStack swordCase, List<ItemStack> weapons,
                                         HolderLookup.Provider registries) {
        CompoundTag root = swordCase.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = new ListTag();
        for (ItemStack weapon : weapons) {
            if (!weapon.isEmpty() && FlyingSwordEntity.isFlyingWeapon(weapon)) {
                list.add(weapon.copyWithCount(1).saveOptional(registries));
            }
        }
        if (list.isEmpty()) {
            root.remove(STORED_WEAPONS);
        } else {
            root.put(STORED_WEAPONS, list);
        }
        swordCase.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".sword_case_count",
                storedWeaponCount(stack)));
        tooltip.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".sword_case_controls"));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return BACK_ACCESSORY.equals(slotContext.identifier());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
