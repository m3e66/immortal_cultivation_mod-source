package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class DisguiseMaskItem extends ArmorItem implements GeoItem, ICurioItem {
    private static final String DISGUISE_SKIN =
            ImmortalCultivationMod.MODID + ".disguise_skin";
    private static final String HELMET_EQUIPPED =
            ImmortalCultivationMod.MODID + ".disguise_helmet_equipped";

    private static final List<String> SKINS =
            List.of(
                    "zhuji1",
                    "zhuji2",
                    "zhuji3",
                    "jindan1",
                    "jindan2");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public DisguiseMaskItem(Properties properties) {
        super(
                ModArmorMaterials.DAOFU,
                Type.HELMET,
                properties.stacksTo(1).durability(220));
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected) {
        super.inventoryTick(
                stack,
                level,
                entity,
                slotId,
                isSelected);

        if (level.isClientSide
                || !(entity instanceof LivingEntity living)) {
            return;
        }

        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        boolean equippedAsHelmet =
                living.getItemBySlot(EquipmentSlot.HEAD) == stack;

        boolean wasEquippedAsHelmet =
                tag.getBoolean(HELMET_EQUIPPED);

        if (equippedAsHelmet && !wasEquippedAsHelmet) {
            String currentSkin =
                    tag.getString(DISGUISE_SKIN);

            int selectedIndex;

            if (SKINS.contains(currentSkin)
                    && SKINS.size() > 1) {
                int currentIndex =
                        SKINS.indexOf(currentSkin);

                selectedIndex =
                        living.getRandom()
                                .nextInt(SKINS.size() - 1);

                if (selectedIndex >= currentIndex) {
                    selectedIndex++;
                }
            } else {
                selectedIndex =
                        living.getRandom()
                                .nextInt(SKINS.size());
            }

            tag.putString(
                    DISGUISE_SKIN,
                    SKINS.get(selectedIndex));

            tag.putBoolean(
                    HELMET_EQUIPPED,
                    true);

            stack.set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.of(tag));

            return;
        }

        if (!equippedAsHelmet && wasEquippedAsHelmet) {
            tag.remove(HELMET_EQUIPPED);

            stack.set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.of(tag));

            return;
        }

        ensureDisguiseSkin(stack, living);
    }

    @Override
    public boolean canEquip(
            SlotContext slotContext,
            ItemStack stack) {
        return LayeredClothingSlots.HEAD_ACCESSORY.equals(
                slotContext.identifier());
    }

    @Override
    public void onEquip(
            SlotContext slotContext,
            ItemStack previousStack,
            ItemStack stack) {
        LivingEntity wearer =
                slotContext.entity();

        if (wearer.level().isClientSide
                || !LayeredClothingSlots.HEAD_ACCESSORY.equals(
                slotContext.identifier())) {
            return;
        }

        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        String currentSkin =
                tag.getString(DISGUISE_SKIN);

        int selectedIndex;

        if (SKINS.contains(currentSkin)
                && SKINS.size() > 1) {
            int currentIndex =
                    SKINS.indexOf(currentSkin);

            selectedIndex =
                    wearer.getRandom()
                            .nextInt(SKINS.size() - 1);

            if (selectedIndex >= currentIndex) {
                selectedIndex++;
            }
        } else {
            selectedIndex =
                    wearer.getRandom()
                            .nextInt(SKINS.size());
        }

        tag.putString(
                DISGUISE_SKIN,
                SKINS.get(selectedIndex));

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(tag));
    }

    public static boolean ensureDisguiseSkin(
            ItemStack stack,
            LivingEntity wearer) {
        if (!stack.is(ModItems.DISGUISE_MASK.get())
                || wearer.level().isClientSide) {
            return false;
        }

        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        String currentSkin =
                tag.getString(DISGUISE_SKIN);

        if (SKINS.contains(currentSkin)) {
            return false;
        }

        tag.putString(
                DISGUISE_SKIN,
                SKINS.get(
                        wearer.getRandom()
                                .nextInt(SKINS.size())));

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(tag));

        return true;
    }

    public static ItemStack getEquippedMask(
            LivingEntity entity) {
        ItemStack helmet =
                entity.getItemBySlot(EquipmentSlot.HEAD);

        if (helmet.is(ModItems.DISGUISE_MASK.get())) {
            ensureDisguiseSkin(
                    helmet,
                    entity);

            return helmet;
        }

        ItemStack[] result = {ItemStack.EMPTY};

        CuriosApi.getCuriosInventory(entity).ifPresent(handler ->
                handler.getStacksHandler(
                        LayeredClothingSlots.HEAD_ACCESSORY)
                        .ifPresent(stacksHandler -> {
                            var stacks =
                                    stacksHandler.getStacks();

                            for (int slot = 0;
                                 slot < stacks.getSlots();
                                 slot++) {
                                ItemStack stack =
                                        stacks.getStackInSlot(slot);

                                if (stack.is(
                                        ModItems.DISGUISE_MASK.get())) {
                                    ensureDisguiseSkin(
                                            stack,
                                            entity);

                                    result[0] = stack;
                                    return;
                                }
                            }
                        }));

        return result[0];
    }

    public static boolean isWearing(
            LivingEntity entity) {
        return !getEquippedMask(entity).isEmpty();
    }

    public static ResourceLocation disguiseTexture(
            ItemStack stack) {
        CompoundTag tag = stack
                .getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY)
                .copyTag();

        String skin =
                tag.getString(DISGUISE_SKIN);

        if (!SKINS.contains(skin)) {
            return null;
        }

        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/entity/" + skin + ".png");
    }

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DisguiseMaskArmorRenderer renderer;

            @Override
            public <T extends LivingEntity>
            HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer =
                            new DisguiseMaskArmorRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return cache;
    }
}
