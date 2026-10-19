package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.DebugShentongPayloads;
import com.example.immortal_cultivation_mod.network.ShentongPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class ShentongClient {
    private static Object connection;
    private static DebugShentongPayloads.State state;
    private ShentongClient() {}
    private static void session() {
        var current = Minecraft.getInstance().getConnection();
        if (connection != current) { connection=current; state=null; }
        var player = Minecraft.getInstance().player;
        if (state != null && (player == null || !state.targetId().equals(player.getUUID()))) state=null;
    }
    public static DebugShentongPayloads.State state() { session(); return state; }
    public static boolean hasShentong() { return state() != null && !state.abilities().isEmpty(); }
    public static void request() {
        session();
        if (connection != null) PacketDistributor.sendToServer(new ShentongPayloads.Request());
    }
    public static void accept(DebugShentongPayloads.State snapshot) {
        session();
        var player = Minecraft.getInstance().player;
        if (player != null && player.getUUID().equals(snapshot.targetId())) state=snapshot;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { session(); }
}
