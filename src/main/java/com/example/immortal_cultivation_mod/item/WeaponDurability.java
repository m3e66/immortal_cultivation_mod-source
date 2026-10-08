package com.example.immortal_cultivation_mod.item;

import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = "immortal_cultivation_mod", bus = EventBusSubscriber.Bus.MOD)
public final class WeaponDurability {
    private WeaponDurability() {}

    @SubscribeEvent
    public static void modifyDefaults(ModifyDefaultComponentsEvent event) {
        for(var spec:HigherWeaponSpec.values())event.modify(HigherWeaponRecipes.item(spec.id), builder -> builder.set(DataComponents.MAX_DAMAGE,spec.durability));
        // Apply after TieredItem initializes its iron-tier durability.
        event.modify(ModItems.TIEGU_SHAN.get(), builder -> builder.set(DataComponents.MAX_DAMAGE, 2031));
        event.modify(ModItems.FUCHEN.get(), builder -> builder.set(DataComponents.MAX_DAMAGE, 2031));
        event.modify(ModItems.WHIP.get(), builder -> builder.set(DataComponents.MAX_DAMAGE, 2031));
    }
}
