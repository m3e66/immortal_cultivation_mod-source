package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.TimeFastForwardClientState;
import com.example.immortal_cultivation_mod.screen.DebugTimeScreen;
import com.example.immortal_cultivation_mod.world.DebugTimeController;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Packets for the debug clock and its synchronized fast-forward animation. */
public final class DebugTimePayloads {
    private DebugTimePayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(
            String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, path));
    }

    public record ClientboundOpenTimeScreen(long dayTime)
            implements CustomPacketPayload {
        public static final Type<ClientboundOpenTimeScreen> TYPE =
                DebugTimePayloads.type("open_debug_time_screen");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenTimeScreen>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeLong(payload.dayTime),
                buffer -> new ClientboundOpenTimeScreen(buffer.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundStartFastForward(
            int targetTimeOfDay, int speedMultiplier)
            implements CustomPacketPayload {
        public static final Type<ServerboundStartFastForward> TYPE =
                DebugTimePayloads.type("start_debug_time_fast_forward");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundStartFastForward>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.targetTimeOfDay);
                    buffer.writeVarInt(payload.speedMultiplier);
                },
                buffer -> new ServerboundStartFastForward(
                        buffer.readVarInt(), buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundFastForwardState(
            long dayTime, int stepTicks, boolean active)
            implements CustomPacketPayload {
        public static final Type<ClientboundFastForwardState> TYPE =
                DebugTimePayloads.type("debug_time_fast_forward_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundFastForwardState>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.dayTime);
                    buffer.writeVarInt(payload.stepTicks);
                    buffer.writeBoolean(payload.active);
                },
                buffer -> new ClientboundFastForwardState(
                        buffer.readLong(), buffer.readVarInt(), buffer.readBoolean()));

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
            registrar.playToClient(
                    ClientboundOpenTimeScreen.TYPE,
                    ClientboundOpenTimeScreen.STREAM_CODEC,
                    Handler::handleOpenScreen);
            registrar.playToServer(
                    ServerboundStartFastForward.TYPE,
                    ServerboundStartFastForward.STREAM_CODEC,
                    Handler::handleStart);
            registrar.playToClient(
                    ClientboundFastForwardState.TYPE,
                    ClientboundFastForwardState.STREAM_CODEC,
                    Handler::handleState);
        }

        private static void handleOpenScreen(
                ClientboundOpenTimeScreen payload, IPayloadContext context) {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            context.enqueueWork(() -> DebugTimeScreen.open(payload.dayTime));
        }

        private static void handleStart(
                ServerboundStartFastForward payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    DebugTimeController.start(
                            player, payload.targetTimeOfDay,
                            payload.speedMultiplier);
                }
            });
        }

        private static void handleState(
                ClientboundFastForwardState payload, IPayloadContext context) {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            context.enqueueWork(() -> TimeFastForwardClientState.accept(
                    payload.dayTime, payload.stepTicks, payload.active));
        }
    }
}
