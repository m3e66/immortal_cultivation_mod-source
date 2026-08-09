package com.example.immortal_cultivation_mod.item;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

/** Internal wearable used to render the fist bones on both player arms. */
public class YaoyuanFistBoneChestplateItem extends HeavyIronBracerChestplateItem {
    public YaoyuanFistBoneChestplateItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private YaoyuanFistBoneChestplateRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer = new YaoyuanFistBoneChestplateRenderer();
                }
                return renderer;
            }
        });
    }
}
