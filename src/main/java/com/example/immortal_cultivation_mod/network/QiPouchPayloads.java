package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.screen.QiPouchMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Network input used by the Qi pouch's combined chest search. */
public final class QiPouchPayloads {
    private QiPouchPayloads() {
    }

    public record ServerboundChestSearch(int containerId, String query)
            implements CustomPacketPayload {
        public static final Type<ServerboundChestSearch> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "qi_pouch_chest_search"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundChestSearch> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId());
                    buffer.writeUtf(payload.query(), 64);
                },
                buffer -> new ServerboundChestSearch(
                        buffer.readVarInt(), buffer.readUtf(64)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(
            modid = ImmortalCultivationMod.MODID,
            bus = EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        private Handler() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("qi-pouch-1").playToServer(
                    ServerboundChestSearch.TYPE,
                    ServerboundChestSearch.STREAM_CODEC,
                    Handler::handleSearch);
        }

        private static void handleSearch(
                ServerboundChestSearch payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof QiPouchMenu menu
                        && menu.containerId == payload.containerId()) {
                    menu.setChestSearch(payload.query());
                }
            });
        }
    }
}
