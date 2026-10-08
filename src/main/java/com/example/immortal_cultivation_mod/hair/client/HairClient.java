package com.example.immortal_cultivation_mod.hair.client;

import com.example.immortal_cultivation_mod.hair.*;
import com.example.immortal_cultivation_mod.mixin.HairLayerAccessor;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="immortal_cultivation_mod",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class HairClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(HairContent.MENU.get(),HairScreen::new);}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){
        HairContent.HAIR.forEach(item->e.register((stack,index)->0xFF000000|HairData.color(stack),item.get()));
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){
        e.registerReloadListener((ResourceManagerReloadListener) manager->HairGeometry.reload());
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
        for(var skin:e.getSkins()){
            var candidate=e.getSkin(skin);
            if(!(candidate instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer))continue;
            var layers=((HairLayerAccessor)renderer).hairLayers();int index=0;
            while(index<layers.size() && !(layers.get(index) instanceof HumanoidArmorLayer))index++;
            layers.add(index,new HairLayer(renderer));
        }
    }
}
