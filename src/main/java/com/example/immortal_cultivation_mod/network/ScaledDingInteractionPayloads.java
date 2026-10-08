package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ScaledDingInteractionSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Network bridge for interactions with model parts outside a ding's anchor block. */
public final class ScaledDingInteractionPayloads {
    private ScaledDingInteractionPayloads() {
    }

    public record ServerboundUseScaledDingPayload(
            BlockPos anchor,
            double hitX,
            double hitY,
            double hitZ) implements CustomPacketPayload {
        public static final Type<ServerboundUseScaledDingPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID,
                        "use_scaled_ding"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundUseScaledDingPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeBlockPos(payload.anchor());
                    buffer.writeDouble(payload.hitX());
                    buffer.writeDouble(payload.hitY());
                    buffer.writeDouble(payload.hitZ());
                },
                buffer -> new ServerboundUseScaledDingPayload(
                        buffer.readBlockPos(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()));

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
            event.registrar("1").playToServer(
                    ServerboundUseScaledDingPayload.TYPE,
                    ServerboundUseScaledDingPayload.STREAM_CODEC,
                    Handler::handleUse);
        }

        private static void handleUse(
                ServerboundUseScaledDingPayload payload,
                IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    ScaledDingInteractionSupport.interact(
                            player, payload.anchor(),
                            new Vec3(payload.hitX(),
                                    payload.hitY(), payload.hitZ()));
                }
            });
        }
    }
}
