package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ClothSheathItem extends Item implements GeoItem, ICurioItem {
    private static final String BACK_ACCESSORY = "back_accessory";
    private static final String STORED_WEAPON = ImmortalCultivationMod.MODID + ".cloth_sheath_weapon";
    private static final String SHEATH_COLOR = ImmortalCultivationMod.MODID + ".cloth_sheath_color";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ClothSheathItem(Properties properties) {
        super(properties);
    }

    public static boolean swapWeapon(ServerPlayer player) {
        ItemStack hand = player.getMainHandItem();
        if (!hand.isEmpty() && !QiInfusedWeapon.isWeapon(hand)) {
            return false;
        }
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler(BACK_ACCESSORY)
                        .map(back -> swapWeapon(player, handler, back.getStacks(), hand)))
                .orElse(false);
    }

    private static boolean swapWeapon(ServerPlayer player,
                                      top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler,
                                      top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler backStacks,
                                      ItemStack hand) {
        boolean foundEmptySheath = false;
        for (int slot = 0; slot < backStacks.getSlots(); slot++) {
            ItemStack equipped = backStacks.getStackInSlot(slot);
            boolean cloth = equipped.is(ModItems.CLOTH_SHEATH.get());
            RigidSheathItem rigid = equipped.getItem() instanceof RigidSheathItem item ? item : null;
            if (!cloth && rigid == null) {
                continue;
            }

            if (!hand.isEmpty() && rigid != null && !rigid.acceptsWeapon(hand)) {
                continue;
            }

            ItemStack sheath = equipped.copy();
            ItemStack stored = cloth
                    ? getStoredWeapon(sheath, player.registryAccess())
                    : RigidSheathItem.getStoredWeapon(sheath, player.registryAccess());
            if (hand.isEmpty() && stored.isEmpty()) {
                foundEmptySheath = true;
                continue;
            }
            if (cloth) {
                setStoredWeapon(sheath, hand, player.registryAccess());
            } else {
                RigidSheathItem.setStoredWeapon(sheath, hand, player.registryAccess());
            }
            handler.setEquippedCurio(BACK_ACCESSORY, slot, sheath);
            player.setItemInHand(InteractionHand.MAIN_HAND, stored);
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + (cloth ? ".cloth_sheath_swapped" : ".rigid_sheath_swapped")), true);
            return true;
        }
        return foundEmptySheath;
    }

    public static ItemStack getStoredWeapon(ItemStack sheath, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag root = sheath.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(STORED_WEAPON)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, root.getCompound(STORED_WEAPON));
    }

    private static void setStoredWeapon(ItemStack sheath, ItemStack weapon,
                                        net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag root = sheath.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (weapon.isEmpty()) {
            root.remove(STORED_WEAPON);
        } else {
            root.put(STORED_WEAPON, weapon.saveOptional(registries));
        }
        sheath.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static boolean isWideWeapon(ItemStack weapon) {
        if (weapon.is(ModItems.DAO.get()) || weapon.is(ModItems.FLAME_PATTERN_DAO.get()) || weapon.is(ModItems.BLACK_IRON_GREATSWORD.get())) {
            return true;
        }
        CompoundTag root = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return "dao".equals(root.getCompound(ForgingSystem.TAG_FORGING).getString("recipe"));
    }

    public static DyeColor color(ItemStack sheath) {
        CompoundTag root = sheath.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return DyeColor.byName(root.getString(SHEATH_COLOR), DyeColor.WHITE);
    }

    public static void setColor(ItemStack sheath, DyeColor color) {
        CompoundTag root = sheath.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.putString(SHEATH_COLOR, color.getName());
        sheath.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        sheath.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(color.getId() + 1));
    }

    public static ResourceLocation woolTexture(ItemStack sheath) {
        return ResourceLocation.withDefaultNamespace("textures/block/" + color(sheath).getName() + "_wool.png");
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
