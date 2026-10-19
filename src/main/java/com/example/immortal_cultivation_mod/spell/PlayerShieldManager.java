package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerShieldManager {
    private static final Map<UUID, Map<String, ShieldValue>> SHIELDS = new ConcurrentHashMap<>();

    private PlayerShieldManager() {
    }

    public static void set(ServerPlayer player, String source, float amount, float max) {
        UUID playerId = player.getUUID();
        if (!Float.isFinite(amount) || !Float.isFinite(max) || amount <= 0.0F || max <= 0.0F) {
            clear(player, source);
            return;
        }
        var value=new ShieldValue(Math.min(amount,max),max);
        var previous=SHIELDS.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(source,value);
        if(value.equals(previous))return;
        sync(player);
    }

    public static void clear(ServerPlayer player, String source) {
        Map<String, ShieldValue> playerShields = SHIELDS.get(player.getUUID());
        if (playerShields != null) {
            if(playerShields.remove(source)==null)return;
            if (playerShields.isEmpty()) {
                SHIELDS.remove(player.getUUID());
            }
        } else return;
        sync(player);
    }

    public static void clearAll(ServerPlayer player) {
        SHIELDS.remove(player.getUUID());
        sync(player);
    }

    public static float totalAmount(ServerPlayer player) {
        var shields = SHIELDS.get(player.getUUID());
        if (shields == null) return 0;
        double total = 0;
        for (ShieldValue value : shields.values()) total += Math.max(0, value.amount());
        return (float)Math.min(Float.MAX_VALUE,total);
    }

    public static void sync(ServerPlayer player) {
        List<ShieldDisplay.Layer> layers=layers(player);
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundShieldDataPayload(
                ShieldDisplay.total(layers,false),ShieldDisplay.total(layers,true),layers));
    }

    public static List<ShieldDisplay.Layer> layers(ServerPlayer player) {
        Map<String, ShieldValue> playerShields = SHIELDS.get(player.getUUID());
        var kinds=ShieldDisplay.Kind.values();double[] amounts=new double[kinds.length],maxima=new double[kinds.length];
        if (playerShields != null) {
            for (var entry : playerShields.entrySet()) {
                int kind=ShieldDisplay.Kind.source(entry.getKey()).ordinal();var shield=entry.getValue();
                amounts[kind]+=shield.amount();maxima[kind]+=shield.max();
            }
        }
        var layers=new ArrayList<ShieldDisplay.Layer>();
        for(int i=0;i<kinds.length;i++)if(amounts[i]>0&&maxima[i]>0)
            layers.add(new ShieldDisplay.Layer(kinds[i],(float)Math.min(Float.MAX_VALUE,amounts[i]),(float)Math.min(Float.MAX_VALUE,maxima[i])));
        return List.copyOf(layers);
    }

    private record ShieldValue(float amount, float max) {
    }
}
