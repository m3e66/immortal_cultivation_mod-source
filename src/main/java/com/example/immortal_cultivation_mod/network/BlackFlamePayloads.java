package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.BlackFlameClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Small, batched visual packets for 玄焰 terrain corrosion and staged deaths. */
public final class BlackFlamePayloads {
    private static final int MAX_BLOCK_VISUALS = 64;

    private BlackFlamePayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, path));
    }

    public record BlockVisual(long position, float progress) {
    }

    public record ClientboundBlockCorrosion(List<BlockVisual> visuals)
            implements CustomPacketPayload {
        public static final Type<ClientboundBlockCorrosion> TYPE =
                BlackFlamePayloads.type("black_flame_block_corrosion");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBlockCorrosion>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    int size = Math.min(MAX_BLOCK_VISUALS, payload.visuals.size());
                    buffer.writeVarInt(size);
                    for (int index = 0; index < size; index++) {
                        BlockVisual visual = payload.visuals.get(index);
                        buffer.writeLong(visual.position);
                        buffer.writeFloat(visual.progress);
                    }
                },
                buffer -> {
                    int size = buffer.readVarInt();
                    if (size < 0 || size > MAX_BLOCK_VISUALS) {
                        throw new IllegalArgumentException(
                                "Invalid black-flame block visual count: " + size);
                    }
                    List<BlockVisual> visuals = new ArrayList<>(size);
                    for (int index = 0; index < size; index++) {
                        visuals.add(new BlockVisual(buffer.readLong(), buffer.readFloat()));
                    }
                    return new ClientboundBlockCorrosion(List.copyOf(visuals));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundDeathStart(int entityId, int durationTicks)
            implements CustomPacketPayload {
        public static final Type<ClientboundDeathStart> TYPE =
                BlackFlamePayloads.type("black_flame_death_start");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDeathStart>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.entityId);
                    buffer.writeVarInt(payload.durationTicks);
                },
                buffer -> new ClientboundDeathStart(
                        buffer.readVarInt(), buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID,
            bus = EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        private Handler() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToClient(ClientboundBlockCorrosion.TYPE,
                    ClientboundBlockCorrosion.STREAM_CODEC, Handler::handleBlocks);
            registrar.playToClient(ClientboundDeathStart.TYPE,
                    ClientboundDeathStart.STREAM_CODEC, Handler::handleDeath);
        }

        private static void handleBlocks(
                ClientboundBlockCorrosion payload, IPayloadContext context) {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            context.enqueueWork(() -> BlackFlameClient.acceptBlockVisuals(payload.visuals));
        }

        private static void handleDeath(
                ClientboundDeathStart payload, IPayloadContext context) {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            context.enqueueWork(() -> BlackFlameClient.beginDeath(
                    payload.entityId, payload.durationTicks));
        }
    }
}
