package com.example.immortal_cultivation_mod.item;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class DaofuChestplateItem extends ArmorItem
        implements GeoItem, ICurioItem {
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public DaofuChestplateItem(Properties properties) {
        super(
                ModArmorMaterials.DAOFU,
                Type.CHESTPLATE,
                properties.durability(1800));
    }

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DaofuArmorRenderer renderer;

            @Override
            public <T extends LivingEntity>
            HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer = new DaofuArmorRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public boolean canEquip(
            SlotContext slotContext,
            ItemStack stack) {
        return LayeredClothingSlots.OUTER_CLOTHES.equals(
                slotContext.identifier());
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
