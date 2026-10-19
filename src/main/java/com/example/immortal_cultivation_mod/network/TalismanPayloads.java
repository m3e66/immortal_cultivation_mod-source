package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.TalismanMessageClient;
import com.example.immortal_cultivation_mod.entity.TalismanMessageEntity;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import com.example.immortal_cultivation_mod.screen.TalismanDrawingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class TalismanPayloads {
    private TalismanPayloads() {
    }

    private static <T extends CustomPacketPayload>
            CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, path));
    }

    public record ClientboundOpenMessage(boolean longRange)
            implements CustomPacketPayload {
        public static final Type<ClientboundOpenMessage> TYPE =
                TalismanPayloads.type("talisman_message_open");
        public static final StreamCodec<ByteBuf, ClientboundOpenMessage>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ClientboundOpenMessage::longRange,
                ClientboundOpenMessage::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSendMessage(
            String targetName, String message, boolean longRange)
            implements CustomPacketPayload {
        public static final Type<ServerboundSendMessage> TYPE =
                TalismanPayloads.type("talisman_message_send");
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundSendMessage> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.targetName(), 16);
                    buffer.writeUtf(payload.message(), 200);
                    buffer.writeBoolean(payload.longRange());
                },
                buffer -> new ServerboundSendMessage(
                        buffer.readUtf(16), buffer.readUtf(200),
                        buffer.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundDrawTalisman(
            String formulaId, int qiControl, List<Integer> trace, boolean instant)
            implements CustomPacketPayload {
        private static final int MAX_TRACE_VALUES = 2_050;
        public static final Type<ServerboundDrawTalisman> TYPE =
                TalismanPayloads.type("talisman_draw_submit");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDrawTalisman>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.formulaId(), 64);
                    buffer.writeVarInt(payload.qiControl());
                    int size = Math.min(MAX_TRACE_VALUES, payload.trace().size());
                    buffer.writeVarInt(size);
                    for (int i = 0; i < size; i++) buffer.writeInt(payload.trace().get(i));
                    buffer.writeBoolean(payload.instant());
                },
                buffer -> {
                    String formula = buffer.readUtf(64);
                    int control = buffer.readVarInt();
                    int size = buffer.readVarInt();
                    if (size < 0 || size > MAX_TRACE_VALUES) {
                        throw new IllegalArgumentException("Invalid talisman trace size: " + size);
                    }
                    List<Integer> trace = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) trace.add(buffer.readInt());
                    return new ServerboundDrawTalisman(formula, control, List.copyOf(trace),
                            buffer.readBoolean());
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Starts or abandons a manual drawing attempt before final submission. */
    public record ServerboundDrawingAttempt(String formulaId, boolean begin)
            implements CustomPacketPayload {
        public static final Type<ServerboundDrawingAttempt> TYPE =
                TalismanPayloads.type("talisman_drawing_attempt");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDrawingAttempt>
                STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.formulaId(), 64);
                    buffer.writeBoolean(payload.begin());
                },
                buffer -> new ServerboundDrawingAttempt(
                        buffer.readUtf(64), buffer.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void openMessageComposer(
            ServerPlayer player, boolean longRange) {
        PacketDistributor.sendToPlayer(
                player, new ClientboundOpenMessage(longRange));
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
                    ClientboundOpenMessage.TYPE,
                    ClientboundOpenMessage.STREAM_CODEC,
                    Handler::handleOpen);
            registrar.playToServer(
                    ServerboundSendMessage.TYPE,
                    ServerboundSendMessage.STREAM_CODEC,
                    Handler::handleSend);
            registrar.playToServer(
                    ServerboundDrawTalisman.TYPE,
                    ServerboundDrawTalisman.STREAM_CODEC,
                    Handler::handleDrawing);
            registrar.playToServer(
                    ServerboundDrawingAttempt.TYPE,
                    ServerboundDrawingAttempt.STREAM_CODEC,
                    Handler::handleDrawingAttempt);
        }

        private static void handleOpen(
                ClientboundOpenMessage payload, IPayloadContext context) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() ->
                        TalismanMessageClient.open(payload.longRange()));
            }
        }

        private static void handleSend(
                ServerboundSendMessage payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer sender) {
                    send(sender, payload);
                }
            });
        }

        private static void handleDrawing(
                ServerboundDrawTalisman payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof TalismanDrawingMenu menu) {
                    menu.submit(player, payload.formulaId(), payload.trace(), payload.qiControl(),
                            payload.instant());
                }
            });
        }

        private static void handleDrawingAttempt(
                ServerboundDrawingAttempt payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof TalismanDrawingMenu menu) {
                    if (payload.begin()) menu.beginDrawing(player, payload.formulaId());
                    else menu.cancelDrawing(player);
                }
            });
        }

        private static void send(
                ServerPlayer sender, ServerboundSendMessage payload) {
            String name = payload.targetName().trim();
            String message = payload.message().trim();
            if (name.isEmpty() || message.isEmpty()) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman_message.required"), true);
                return;
            }
            UtilityTalismanItem.Kind expected = payload.longRange()
                    ? UtilityTalismanItem.Kind.THOUSAND_LI_MESSAGE
                    : UtilityTalismanItem.Kind.VOICE_MESSAGE;
            InteractionHand hand = matchingHand(sender, expected);
            if (hand == null) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman_message.missing"), true);
                return;
            }
            ServerPlayer target = sender.server.getPlayerList()
                    .getPlayerByName(name);
            if (target == null || target == sender) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman_message.target_missing"), true);
                return;
            }
            double range = payload.longRange() ? 1_000.0D : 100.0D;
            if (target.serverLevel() != sender.serverLevel()
                    || target.distanceToSqr(sender) > range * range) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman_message.out_of_range"), true);
                return;
            }
            if (!TalismanMessageEntity.spawn(
                    sender.serverLevel(), sender, target,
                    message, payload.longRange())) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.talisman_message.activation_failed"), true);
                return;
            }
            ItemStack stack = sender.getItemInHand(hand);
            sender.getCooldowns().addCooldown(stack.getItem(), 10);
            if (!sender.getAbilities().instabuild) stack.shrink(1);
        }

        private static InteractionHand matchingHand(
                ServerPlayer player, UtilityTalismanItem.Kind kind) {
            for (InteractionHand hand : InteractionHand.values()) {
                if (player.getItemInHand(hand).getItem()
                        instanceof UtilityTalismanItem talisman
                        && talisman.kind() == kind) {
                    return hand;
                }
            }
            return null;
        }
    }
}
