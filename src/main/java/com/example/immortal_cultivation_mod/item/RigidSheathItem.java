package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** A persistent rigid sheath that only accepts one weapon family. */
public final class RigidSheathItem extends Item implements GeoItem, ICurioItem {
    private static final String BACK_ACCESSORY = "back_accessory";
    private static final String STORED_WEAPON = ImmortalCultivationMod.MODID + ".rigid_sheath_weapon";

    public enum Kind {
        SWORD,
        DAO
    }

    private final Kind kind;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RigidSheathItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean acceptsWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return kind == Kind.DAO ? isDaoWeapon(stack) : isSwordWeapon(stack);
    }

    public static boolean isDaoWeapon(ItemStack stack) {
        return stack.is(ModItems.DAO.get()) || stack.is(ModItems.FLAME_PATTERN_DAO.get()) || "dao".equals(forgingRecipe(stack));
    }

    public static boolean isSwordWeapon(ItemStack stack) {
        return stack.is(ModItems.BLACK_IRON_JIAN.get()) || stack.is(ModItems.BLACK_IRON_GREATSWORD.get())
                || stack.is(ModItems.THUNDERWOOD_SWORD.get()) || stack.is(ModItems.SEVEN_STAR_SWORD.get())
                || stack.is(ModItems.FLYING_SWORD.get())
                || stack.is(ModItems.TAOMU_SWORD.get())
                || stack.is(ModItems.COPPER_COIN_SWORD.get())
                || "flying_sword".equals(forgingRecipe(stack));
    }

    private static String forgingRecipe(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ForgingSystem.TAG_FORGING)) {
            return "";
        }
        return root.getCompound(ForgingSystem.TAG_FORGING).getString("recipe");
    }

    public static ItemStack getStoredWeapon(ItemStack sheath, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag root = sheath.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(STORED_WEAPON)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, root.getCompound(STORED_WEAPON));
    }

    static void setStoredWeapon(ItemStack sheath, ItemStack weapon,
                                net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag root = sheath.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (weapon.isEmpty()) {
            root.remove(STORED_WEAPON);
        } else {
            root.put(STORED_WEAPON, weapon.saveOptional(registries));
        }
        sheath.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
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
