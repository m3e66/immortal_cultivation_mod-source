package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.client.CompassNavigation;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CompassOverlay {
    public static final ResourceLocation HUD_ID=ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","compass_navigation");
    public static final LayeredDraw.Layer INSTANCE=(g,delta)->{
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.options.hideGui||mc.screen!=null
                ||!mc.player.getMainHandItem().is(ModItems.YINYANG_COMPASS.get())&&!mc.player.getOffhandItem().is(ModItems.YINYANG_COMPASS.get()))return;
        var lines=new ArrayList<Component>();var reading=CompassNavigation.reading();
        lines.add(reading==null?Component.translatable("screen.immortal_cultivation_mod.compass.waiting"):
                Component.translatable("screen.immortal_cultivation_mod.compass.reading",reading.qi(),reading.yin()));
        lines.add(CompassNavigation.position());
        if(CompassNavigation.target()!=null){lines.add(CompassNavigation.targetLabel());lines.add(CompassNavigation.directions());}
        int width=lines.stream().mapToInt(mc.font::width).max().orElse(0);
        float scale=Math.min(1,(g.guiWidth()-24)/(float)Math.max(1,width));
        int right=(int)(g.guiWidth()/scale)-12,x=right-width,y=12;
        g.pose().pushPose();g.pose().scale(scale,scale,1);
        g.fill(x-5,y-5,right+5,y+lines.size()*12+1,0xBD101820);
        for(int i=0;i<lines.size();i++)g.drawString(mc.font,lines.get(i),x,y+i*12,i>1?0xFFFFD774:0xFFF0F4F7,false);
        g.pose().popPose();
    };
    private CompassOverlay() {}
}
