package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Read-only server snapshots, sent only while the operator's Debug Eye is enabled. */
public final class DebugEyePayloads {
    public record Snapshot(long tick, List<Component> hud, List<Component> luck, List<Component> player,
                           List<Component> target, List<Component> events, List<Component> raw) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "debug_eye_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = StreamCodec.of(
                (buf, data) -> {
                    buf.writeLong(data.tick);
                    for (var page : List.of(data.hud, data.luck, data.player, data.target, data.events, data.raw)) {
                        buf.writeVarInt(page.size());
                        for (Component line : page) ComponentSerialization.STREAM_CODEC.encode(buf, line);
                    }
                }, buf -> new Snapshot(buf.readLong(), read(buf), read(buf), read(buf), read(buf), read(buf), read(buf)));
        private static List<Component> read(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > 256) throw new IllegalArgumentException("Debug page too large");
            List<Component> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) result.add(ComponentSerialization.STREAM_CODEC.decode(buf));
            return List.copyOf(result);
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(Snapshot.TYPE, Snapshot.STREAM_CODEC, (payload, ctx) -> {
                if (FMLEnvironment.dist == Dist.CLIENT) ctx.enqueueWork(() ->
                        com.example.immortal_cultivation_mod.client.DebugEyeClient.accept(payload));
            });
        }
    }
}
