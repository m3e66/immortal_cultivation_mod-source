package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.item.HigherWeaponItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HigherWeaponItemProperties {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->ItemProperties.register(ModItems.SEVEN_STAR_SWORD.get(),HigherWeaponItem.id("stars"),
                (stack,level,entity,seed)->com.example.immortal_cultivation_mod.item.SevenStarPattern.charges(stack)));
    }
}
