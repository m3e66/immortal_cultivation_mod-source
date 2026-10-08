package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.pagoda.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid="immortal_cultivation_mod",value=Dist.CLIENT)
public final class GoldenGourdHud {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var minecraft=Minecraft.getInstance();var player=minecraft.player;
        if(player==null||minecraft.options.hideGui||player.isSpectator())return;
        ItemStack stack=player.getMainHandItem();
        if(!(stack.getItem() instanceof GoldenGourdItem))stack=player.getOffhandItem();
        if(!(stack.getItem() instanceof GoldenGourdItem))stack=CuriosApi.getCuriosInventory(player)
                .flatMap(handler->handler.findFirstCurio(candidate->candidate.getItem() instanceof GoldenGourdItem))
                .map(result->result.stack()).orElse(ItemStack.EMPTY);
        if(stack.isEmpty())return;
        var graphics=event.getGuiGraphics();int water=GourdSystem.shownWater(stack),x=8,y=graphics.guiHeight()-58;
        graphics.fill(x-2,y-2,x+104,y+21,0xA0202628);
        graphics.drawString(minecraft.font,Component.translatable("tooltip.immortal_cultivation_mod.gourd.water",water),x,y,0x70E5E8,true);
        graphics.fill(x,y+13,x+100,y+18,0xFF33484C);
        graphics.fill(x,y+13,x+water,y+18,0xFF4BCBCD);
    }
    private GoldenGourdHud(){}
}
