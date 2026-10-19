package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.network.CompassReadingsPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/** Compass state is scoped to a client world, so pins never leak across dimensions or saves. */
public final class CompassNavigation {
    public record Target(int x,int z,int value,boolean qi) {}
    private static ClientLevel world;
    private static CompassReadingsPayloads.Reading reading;
    private static long readAt;
    private static Target target;
    private CompassNavigation() {}
    private static boolean ready(){
        var level=Minecraft.getInstance().level;
        if(world!=level){world=level;reading=null;target=null;}
        return level!=null;
    }
    public static void accept(CompassReadingsPayloads.Reading value){
        if(ready()&&world.dimension().location().equals(value.dimension())){reading=value;readAt=world.getGameTime();}
    }
    public static CompassReadingsPayloads.Reading reading(){return ready()&&world.getGameTime()-readAt<=30?reading:null;}
    public static void pin(int x,int z,int value,boolean qi){if(ready())target=new Target(x,z,value,qi);}
    public static Target target(){return ready()?target:null;}
    public static void clear(){target=null;}
    public static Component position(){
        var player=Minecraft.getInstance().player;
        return player==null?Component.empty():Component.translatable("screen.immortal_cultivation_mod.compass.position",player.getBlockX(),player.getBlockY(),player.getBlockZ());
    }
    public static Component targetLabel(){
        var t=target();return t==null?Component.translatable("screen.immortal_cultivation_mod.compass.no_target"):
            Component.translatable("screen.immortal_cultivation_mod.compass.target",t.x,t.z,
                    Component.translatable("screen.immortal_cultivation_mod."+(t.qi?"spirit_qi_here":"yin_qi_here"),t.value));
    }
    public static Component directions(){
        var t=target();var player=Minecraft.getInstance().player;if(t==null||player==null)return Component.empty();
        int dx=t.x-player.getBlockX(),dz=t.z-player.getBlockZ();
        if(Math.hypot(dx,dz)<2)return Component.translatable("screen.immortal_cultivation_mod.compass.arrived");
        String[] names={"north","northeast","east","southeast","south","southwest","west","northwest"};
        int index=Math.floorMod((int)Math.round(Math.atan2(dx,-dz)/(Math.PI/4)),8);
        return Component.translatable("screen.immortal_cultivation_mod.compass.navigation",
                Component.translatable("screen.immortal_cultivation_mod.compass."+names[index]),(int)Math.ceil(Math.hypot(dx,dz)),String.format(java.util.Locale.ROOT,"%+d",dx),String.format(java.util.Locale.ROOT,"%+d",dz));
    }
}
