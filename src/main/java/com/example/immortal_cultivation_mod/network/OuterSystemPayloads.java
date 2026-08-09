package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.system.OuterSystemManager;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

public final class OuterSystemPayloads {
    private OuterSystemPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(
            String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, path));
    }

    public record ClientboundSync(String systemType, boolean equipped)
            implements CustomPacketPayload {
        public static final Type<ClientboundSync> TYPE =
                OuterSystemPayloads.type("outer_system_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSync>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ClientboundSync::systemType,
                ByteBufCodecs.BOOL,
                ClientboundSync::equipped,
                ClientboundSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundBoot(String systemType)
            implements CustomPacketPayload {
        public static final Type<ClientboundBoot> TYPE =
                OuterSystemPayloads.type("outer_system_boot");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBoot>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ClientboundBoot::systemType,
                ClientboundBoot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundOpen(CompoundTag state)
            implements CustomPacketPayload {
        public static final Type<ClientboundOpen> TYPE =
                OuterSystemPayloads.type("outer_system_open");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpen>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeNbt(payload.state()),
                buffer -> new ClientboundOpen(Optional.ofNullable(
                        buffer.readNbt()).orElseGet(CompoundTag::new)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundOpen() implements CustomPacketPayload {
        public static final Type<ServerboundOpen> TYPE =
                OuterSystemPayloads.type("outer_system_open_request");
        public static final StreamCodec<ByteBuf, ServerboundOpen> STREAM_CODEC =
                StreamCodec.unit(new ServerboundOpen());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundClose() implements CustomPacketPayload {
        public static final Type<ServerboundClose> TYPE =
                OuterSystemPayloads.type("outer_system_close");
        public static final StreamCodec<ByteBuf, ServerboundClose> STREAM_CODEC =
                StreamCodec.unit(new ServerboundClose());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundClaimQuest() implements CustomPacketPayload {
        public static final Type<ServerboundClaimQuest> TYPE =
                OuterSystemPayloads.type("outer_system_claim_quest");
        public static final StreamCodec<ByteBuf, ServerboundClaimQuest> STREAM_CODEC =
                StreamCodec.unit(new ServerboundClaimQuest());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundShopBuy(String itemId)
            implements CustomPacketPayload {
        public static final Type<ServerboundShopBuy> TYPE =
                OuterSystemPayloads.type("outer_system_shop_buy");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundShopBuy>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ServerboundShopBuy::itemId,
                ServerboundShopBuy::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundShopRecycle(int inventorySlot)
            implements CustomPacketPayload {
        public static final Type<ServerboundShopRecycle> TYPE =
                OuterSystemPayloads.type("outer_system_shop_recycle");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundShopRecycle>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ServerboundShopRecycle::inventorySlot,
                ServerboundShopRecycle::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundShopRequest(int requestIndex)
            implements CustomPacketPayload {
        public static final Type<ServerboundShopRequest> TYPE =
                OuterSystemPayloads.type("outer_system_shop_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundShopRequest>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ServerboundShopRequest::requestIndex,
                ServerboundShopRequest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundGachaDraw() implements CustomPacketPayload {
        public static final Type<ServerboundGachaDraw> TYPE =
                OuterSystemPayloads.type("outer_system_gacha_draw");
        public static final StreamCodec<ByteBuf, ServerboundGachaDraw>
                STREAM_CODEC = StreamCodec.unit(new ServerboundGachaDraw());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundGachaClaim() implements CustomPacketPayload {
        public static final Type<ServerboundGachaClaim> TYPE =
                OuterSystemPayloads.type("outer_system_gacha_claim");
        public static final StreamCodec<ByteBuf, ServerboundGachaClaim>
                STREAM_CODEC = StreamCodec.unit(new ServerboundGachaClaim());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundLifeAction(String action)
            implements CustomPacketPayload {
        public static final Type<ServerboundLifeAction> TYPE =
                OuterSystemPayloads.type("outer_system_life_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundLifeAction>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ServerboundLifeAction::action,
                ServerboundLifeAction::new);

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
            var registrar = event.registrar("1");
            registrar.playToClient(
                    ClientboundSync.TYPE,
                    ClientboundSync.STREAM_CODEC,
                    Handler::handleSync);
            registrar.playToClient(
                    ClientboundBoot.TYPE,
                    ClientboundBoot.STREAM_CODEC,
                    Handler::handleBoot);
            registrar.playToClient(
                    ClientboundOpen.TYPE,
                    ClientboundOpen.STREAM_CODEC,
                    Handler::handleOpen);
            registrar.playToServer(
                    ServerboundOpen.TYPE,
                    ServerboundOpen.STREAM_CODEC,
                    Handler::handleOpenRequest);
            registrar.playToServer(
                    ServerboundClose.TYPE,
                    ServerboundClose.STREAM_CODEC,
                    Handler::handleCloseRequest);
            registrar.playToServer(
                    ServerboundClaimQuest.TYPE,
                    ServerboundClaimQuest.STREAM_CODEC,
                    Handler::handleClaimRequest);
            registrar.playToServer(
                    ServerboundShopBuy.TYPE,
                    ServerboundShopBuy.STREAM_CODEC,
                    Handler::handleShopBuy);
            registrar.playToServer(
                    ServerboundShopRecycle.TYPE,
                    ServerboundShopRecycle.STREAM_CODEC,
                    Handler::handleShopRecycle);
            registrar.playToServer(
                    ServerboundShopRequest.TYPE,
                    ServerboundShopRequest.STREAM_CODEC,
                    Handler::handleShopRequest);
            registrar.playToServer(
                    ServerboundGachaDraw.TYPE,
                    ServerboundGachaDraw.STREAM_CODEC,
                    Handler::handleGachaDraw);
            registrar.playToServer(
                    ServerboundGachaClaim.TYPE,
                    ServerboundGachaClaim.STREAM_CODEC,
                    Handler::handleGachaClaim);
            registrar.playToServer(
                    ServerboundLifeAction.TYPE,
                    ServerboundLifeAction.STREAM_CODEC,
                    Handler::handleLifeAction);
        }

        private static void handleSync(
                ClientboundSync payload, IPayloadContext context) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() -> OuterSystemClient.sync(
                        payload.systemType(), payload.equipped()));
            }
        }

        private static void handleBoot(
                ClientboundBoot payload, IPayloadContext context) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() ->
                        OuterSystemClient.openBoot(payload.systemType()));
            }
        }

        private static void handleOpen(
                ClientboundOpen payload, IPayloadContext context) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() ->
                        OuterSystemClient.openOrUpdate(payload.state()));
            }
        }

        private static void handleOpenRequest(
                ServerboundOpen payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.openScreen(player);
                }
            });
        }

        private static void handleCloseRequest(
                ServerboundClose payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.closeScreen(player);
                }
            });
        }

        private static void handleClaimRequest(
                ServerboundClaimQuest payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.claimQuest(player);
                }
            });
        }

        private static void handleShopBuy(
                ServerboundShopBuy payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.buyShopItem(
                            player, payload.itemId());
                }
            });
        }

        private static void handleShopRecycle(
                ServerboundShopRecycle payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.recycleShopItem(
                            player, payload.inventorySlot());
                }
            });
        }

        private static void handleShopRequest(
                ServerboundShopRequest payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.submitShopRequest(
                            player, payload.requestIndex());
                }
            });
        }

        private static void handleGachaDraw(
                ServerboundGachaDraw payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.drawGacha(player);
                }
            });
        }

        private static void handleGachaClaim(
                ServerboundGachaClaim payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.claimGachaRewards(player);
                }
            });
        }

        private static void handleLifeAction(
                ServerboundLifeAction payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    OuterSystemManager.handleLifeAction(
                            player, payload.action());
                }
            });
        }
    }
}
