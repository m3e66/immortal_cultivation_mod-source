package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.XuanjinPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class XuanjinLanternInput {
    private static boolean attackHeld;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.screen!=null || !mc.options.keyAttack.isDown()
                || !mc.player.getMainHandItem().is(ModItems.XUANJIN_SEAL.get()))attackHeld=false;
    }
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var mc=Minecraft.getInstance();
        if(!event.isAttack() || mc.screen!=null || mc.player==null || mc.player.isSpectator()
                || !mc.player.getMainHandItem().is(ModItems.XUANJIN_SEAL.get()))return;
        event.setCanceled(true);event.setSwingHand(false);
        ThoughtInputGuard.handled(event);
        // Minecraft repeats this event while mining: one physical press should cast once.
        if(attackHeld)return;
        attackHeld=true;
        PacketDistributor.sendToServer(new XuanjinPayloads.Cast());
    }
}
