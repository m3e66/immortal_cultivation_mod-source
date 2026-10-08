package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.network.ForgingVisualPayload;
import com.example.immortal_cultivation_mod.screen.DingScreen;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Short leases recover cleanly from unloaded forges, disconnects and tracking changes. */
public final class ForgingVisualState {
    private record Source(UUID player,ResourceLocation dimension,BlockPos pos) {}
    private static final Map<Source,Long> ACTIVE=new HashMap<>();
    private static ClientLevel world;
    private ForgingVisualState(){}
    private static ClientLevel level(){
        var current=Minecraft.getInstance().level;
        if(current!=world){ACTIVE.clear();world=current;}
        if(current!=null)ACTIVE.values().removeIf(until->until<=current.getGameTime());
        return current;
    }
    public static void receive(ForgingVisualPayload payload){
        var current=level();
        if(current==null||!current.dimension().location().equals(payload.dimension()))return;
        var key=new Source(payload.player(),payload.forgeDimension(),payload.forge());
        if(payload.active())ACTIVE.put(key,current.getGameTime()+30);
        else ACTIVE.remove(key);
    }
    public static boolean isForging(Player player){
        var current=level();
        var minecraft=Minecraft.getInstance();
        // Menu data may arrive after the active packet; suppress the pose until
        // the batch is active, but retain its lease so it can start immediately.
        if(player==minecraft.player && (!(minecraft.screen instanceof DingScreen screen)
                || !screen.getMenu().forging()))return false;
        return current!=null&&player.level()==current&&player.isAlive()
                && ACTIVE.keySet().stream().anyMatch(key->key.player.equals(player.getUUID()));
    }

    public static void clear(Player player){
        if(player!=null)ACTIVE.keySet().removeIf(key->key.player.equals(player.getUUID()));
    }
}
