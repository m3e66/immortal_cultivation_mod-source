package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
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
    private static final String NURTURE_TICKS =
            ImmortalCultivationMod.MODID + ".sword_case_nurture_ticks";
    private static final String NURTURE_QI =
            ImmortalCultivationMod.MODID + ".sword_case_nurture_qi";
    private static final String NURTURE_GRADE =
            ImmortalCultivationMod.MODID + ".sword_case_nurture_grade";
    private static final int[] NURTURE_TIME = {0, 24_000, 72_000, 168_000};
    private static final double[] NURTURE_QI_REQUIRED =
            {0.0D, 100.0D, 2_500.0D, 25_000.0D};
    private static final double[] NURTURE_QI_PER_SECOND =
            {0.0D, 1.0D, 5.0D, 20.0D};
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
        // Older cases can still contain a Dao. Remove only the exact eligible entries that were released.
        int remaining = released;
        for (var iterator = stored.iterator(); iterator.hasNext() && remaining > 0;) {
            ItemStack weapon = iterator.next();
            if (FlyingSwordEntity.isFlyingWeapon(weapon) && WeaponBinding.canUse(weapon, player)) {
                iterator.remove();
                remaining--;
            }
        }
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

    @Override
    public void curioTick(SlotContext slotContext, ItemStack swordCase) {
        if (!(slotContext.entity() instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }
        tickNurturing(player, swordCase);
    }

    private static void tickNurturing(ServerPlayer player, ItemStack swordCase) {
        List<ItemStack> stored = getStoredWeapons(swordCase, player.registryAccess());
        if (stored.stream().noneMatch(FlyingSwordEntity::isFlyingWeapon)) {
            return;
        }

        CompoundTag current = swordCase.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int grade = Math.max(0, Math.min(3, current.getInt(NURTURE_GRADE)));
        int availableGrade = availableNurtureGrade(player, stored);
        if (grade == 0) {
            grade = availableGrade;
        }
        if (grade <= 0 || availableGrade < grade) {
            return;
        }

        int ticks = Math.max(0, current.getInt(NURTURE_TICKS));
        double nurturedQi = Math.max(0.0D, current.getDouble(NURTURE_QI));
        double qiStillNeeded = Math.max(
                0.0D, NURTURE_QI_REQUIRED[grade] - nurturedQi);
        if (qiStillNeeded > 0.0D) {
            double drained = consumeStoredWeaponQi(
                    stored,
                    Math.min(NURTURE_QI_PER_SECOND[grade], qiStillNeeded));
            if (drained <= 0.0D) {
                return;
            }
            nurturedQi += drained;
            setStoredWeapons(swordCase, stored, player.registryAccess());
        }

        ticks += 20;
        CompoundTag updated = swordCase.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        updated.putInt(NURTURE_GRADE, grade);
        updated.putInt(NURTURE_TICKS, ticks);
        updated.putDouble(NURTURE_QI, nurturedQi);

        if (ticks < NURTURE_TIME[grade]
                || nurturedQi + 0.0001D < NURTURE_QI_REQUIRED[grade]) {
            swordCase.set(DataComponents.CUSTOM_DATA, CustomData.of(updated));
            return;
        }

        updated.remove(NURTURE_GRADE);
        updated.remove(NURTURE_TICKS);
        updated.remove(NURTURE_QI);
        swordCase.set(DataComponents.CUSTOM_DATA, CustomData.of(updated));

        ItemStack result = new ItemStack(nurturingStone(grade));
        Component resultName = result.getHoverName();
        if (!player.getInventory().add(result) && !result.isEmpty()) {
            player.drop(result, false);
        }
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + ".sword_case_nurturing_complete",
                resultName), false);
    }

    private static int availableNurtureGrade(
            ServerPlayer player, List<ItemStack> stored) {
        int realmIndex = CultivationLevels.getRealmIndex(
                ModAttachments.getData(player).cultivationLevel());
        int realmGrade = realmIndex >= 3 ? 3 : realmIndex >= 2 ? 2 : 1;
        int weaponGrade = 1;
        for (ItemStack weapon : stored) {
            if (!FlyingSwordEntity.isFlyingWeapon(weapon)) continue;
            int tier = WeaponRestrictionSystem.tier(weapon);
            weaponGrade = Math.max(
                    weaponGrade, tier >= 4 ? 3 : tier >= 2 ? 2 : 1);
        }
        return Math.min(realmGrade, weaponGrade);
    }

    private static double consumeStoredWeaponQi(
            List<ItemStack> stored, double amount) {
        double remaining = Math.max(0.0D, amount);
        double consumed = 0.0D;
        for (ItemStack weapon : stored) {
            if (!FlyingSwordEntity.isFlyingWeapon(weapon)) continue;
            if (remaining <= 0.0001D) {
                break;
            }
            double available = QiInfusedWeapon.getQi(weapon);
            double used = Math.min(available, remaining);
            if (used <= 0.0D) {
                continue;
            }
            QiInfusedWeapon.consumeQi(weapon, used);
            consumed += used;
            remaining -= used;
        }
        return consumed;
    }

    private static Item nurturingStone(int grade) {
        return grade >= 3
                ? ModItems.HIGH_YANGJIAN_STONE.get()
                : grade == 2
                        ? ModItems.MIDDLE_YANGJIAN_STONE.get()
                        : ModItems.LOW_YANGJIAN_STONE.get();
    }

    private static List<ItemStack> getStoredWeapons(ItemStack swordCase, HolderLookup.Provider registries) {
        CompoundTag root = swordCase.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = root.getList(STORED_WEAPONS, Tag.TAG_COMPOUND);
        List<ItemStack> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            // Preserve legacy non-swords so the owner can cycle them back out safely.
            if (!stack.isEmpty()) {
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
            if (!weapon.isEmpty()) {
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

        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int grade = Math.max(0, Math.min(3, root.getInt(NURTURE_GRADE)));
        if (grade > 0) {
            int ticks = Math.max(0, root.getInt(NURTURE_TICKS));
            double nurturedQi = Math.max(0.0D, root.getDouble(NURTURE_QI));
            int timePercent = Math.min(
                    100, ticks * 100 / NURTURE_TIME[grade]);
            int qiPercent = Math.min(
                    100,
                    (int) Math.floor(
                            nurturedQi * 100.0D
                                    / NURTURE_QI_REQUIRED[grade]));
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID
                            + ".sword_case_nurturing",
                    new ItemStack(nurturingStone(grade)).getHoverName(),
                    timePercent,
                    qiPercent));
        } else {
            tooltip.add(Component.translatable(
                    "tooltip." + ImmortalCultivationMod.MODID
                            + ".sword_case_nurturing_idle"));
        }
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID
                        + ".sword_case_nurturing_cost"));
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
