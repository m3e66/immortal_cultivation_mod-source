package com.example.immortal_cultivation_mod.client.curse;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.curse.AnnihilationCurse;
import java.lang.reflect.Proxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Uses AsteorBar's public layer API. No required dependency, layout replacement or duplicate HP bar. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class CurseHealthBarCompat {
    private static boolean attempted;
    public static boolean installed;
    public static long draws;
    private CurseHealthBarCompat() { }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(attempted)return;attempted=true;if(!ModList.get().isLoaded("asteorbar"))return;
        try {
            var loader=CurseHealthBarCompat.class.getClassLoader();
            Class<?> overlays=Class.forName("com.afoxxvi.asteorbar.overlay.Overlays",true,loader);
            Object health=overlays.getField("PLAYER_HEALTH").get(null);
            Class<?> type=Class.forName("com.afoxxvi.asteorbar.overlay.parts.SimpleBarOverlay$Layer",true,loader);
            var configuration=Class.forName("com.afoxxvi.asteorbar.AsteorBar",true,loader).getField("config");
            Class<?> adapter=Class.forName("com.afoxxvi.asteorbar.config.ConfigAdapter",true,loader);
            var stacked=adapter.getMethod("enableStackHealthBar");var fullHealth=adapter.getMethod("fullHealthValue");
            Object layer=Proxy.newProxyInstance(loader,new Class<?>[]{type},(proxy,method,args)->{
                if(method.getName().equals("drawLayer")) {
                    Player player=(Player)args[0];float black=AnnihilationCurse.blackHp(player);if(black<=0)return null;
                    GuiGraphics graphics=(GuiGraphics)args[1];int x=(int)args[2],y=(int)args[3],right=(int)args[4],bottom=(int)args[5];
                    Object parameters=args[6];Class<?> p=parameters.getClass();
                    int shift=p.getField("verticalShift").getInt(parameters);y+=shift;bottom+=shift;
                    int width=Math.max(0,right-x-2);boolean flipped=(boolean)args[7];
                    double capacity=Math.max(.0001,p.getField("capacity").getDouble(parameters));
                    double value=Math.max(0,p.getField("value").getDouble(parameters));
                    Object config=configuration.get(null);
                    if((boolean)stacked.invoke(config)) {
                        double unit=Math.max(1,((Number)fullHealth.invoke(config)).doubleValue());
                        double hp=player.getHealth(),normal=Math.max(0,hp-black),base=Math.floor(hp/unit)*unit,part=(hp-base)/unit;
                        // AsteorBar shows the current partial stack over the preceding full stack.
                        if(base>0)fill(graphics,x,y,right,bottom,flipped,Math.max(part,(normal-base+unit)/unit),1);
                        if(part>0)fill(graphics,x,y,right,bottom,flipped,Math.max(0,(normal-base)/unit),part);
                        return null;
                    }
                    // AsteorBar passes absolute corners, not dimensions, and can mirror the fill.
                    int end=(int)Math.round(width*Math.clamp(value/capacity,0,1));
                    // Parameters.value is normalized by AsteorBar, not an HP count.
                    double amount=value*Math.min(black,player.getHealth())/Math.max(.0001,player.getHealth());
                    int start=(int)Math.round(width*Math.clamp((value-amount)/capacity,0,1));
                    fill(graphics,x,y,right,bottom,flipped,start/(double)Math.max(1,width),end/(double)Math.max(1,width));
                    return null;
                }
                if(method.getName().equals("toString"))return "ImmortalCultivationCurseHealthLayer";
                if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                if(method.getName().equals("equals"))return proxy==args[0];
                return null;
            });
            health.getClass().getMethod("addLayer",String.class,type).invoke(health,"immortal_annihilation_black_hp",layer);installed=true;
        } catch(ReflectiveOperationException|LinkageError error) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not attach cursed HP to optional AsteorBar layer API",error);
        }
    }
    private static void fill(GuiGraphics graphics,int x,int y,int right,int bottom,boolean flipped,double from,double to) {
        int width=Math.max(0,right-x-2),start=(int)Math.round(width*Math.clamp(from,0,1)),end=(int)Math.round(width*Math.clamp(to,0,1));
        if(end<=start||bottom-y<=2)return;
        int left=flipped?right-1-end:x+1+start,endX=flipped?right-1-start:x+1+end;
        graphics.fill(left,y+1,endX,bottom-1,0xFF0B080E);draws++;
    }
}
