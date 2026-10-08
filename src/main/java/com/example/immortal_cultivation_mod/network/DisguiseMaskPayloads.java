package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Network flow for the server-authoritative disguise-mask skin picker. */
public final class DisguiseMaskPayloads {
    private static final int MAX_SKIN_ID_LENGTH = 32;

    private DisguiseMaskPayloads() {
    }

    private static <T extends CustomPacketPayload>
            CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, path));
    }

    public record ClientboundOpenSkinPicker(
            boolean mainHand, String selectedSkin)
            implements CustomPacketPayload {
        public static final Type<ClientboundOpenSkinPicker> TYPE =
                DisguiseMaskPayloads.type("disguise_mask_open_skin_picker");
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ClientboundOpenSkinPicker> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeBoolean(payload.mainHand());
                    buffer.writeUtf(
                            payload.selectedSkin(), MAX_SKIN_ID_LENGTH);
                },
                buffer -> new ClientboundOpenSkinPicker(
                        buffer.readBoolean(),
                        buffer.readUtf(MAX_SKIN_ID_LENGTH)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSelectSkin(
            boolean mainHand, String skin)
            implements CustomPacketPayload {
        public static final Type<ServerboundSelectSkin> TYPE =
                DisguiseMaskPayloads.type("disguise_mask_select_skin");
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundSelectSkin> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeBoolean(payload.mainHand());
                    buffer.writeUtf(payload.skin(), MAX_SKIN_ID_LENGTH);
                },
                buffer -> new ServerboundSelectSkin(
                        buffer.readBoolean(),
                        buffer.readUtf(MAX_SKIN_ID_LENGTH)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void openSkinPicker(
            ServerPlayer player,
            InteractionHand hand,
            String selectedSkin) {
        PacketDistributor.sendToPlayer(
                player,
                new ClientboundOpenSkinPicker(
                        hand == InteractionHand.MAIN_HAND,
                        selectedSkin));
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
                    ClientboundOpenSkinPicker.TYPE,
                    ClientboundOpenSkinPicker.STREAM_CODEC,
                    (payload, context) ->
                            com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                                    .handleOpenDisguiseMaskPicker(payload, context));
            registrar.playToServer(
                    ServerboundSelectSkin.TYPE,
                    ServerboundSelectSkin.STREAM_CODEC,
                    Handler::handleSelection);
        }

        private static void handleSelection(
                ServerboundSelectSkin payload,
                IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }

                InteractionHand hand = payload.mainHand()
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND;
                ItemStack mask = player.getItemInHand(hand);
                if (!mask.is(ModItems.DISGUISE_MASK.get())) {
                    return;
                }

                if (DisguiseMaskItem.setDisguiseSkin(mask, payload.skin())) {
                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();
                }
            });
        }
    }
}
