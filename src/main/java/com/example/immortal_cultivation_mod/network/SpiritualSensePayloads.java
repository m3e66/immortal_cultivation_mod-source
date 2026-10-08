package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.SpiritualSenseClient;
import com.example.immortal_cultivation_mod.spiritual.SpiritualSenseSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Network messages for the server-authoritative spiritual-perception scan. */
public final class SpiritualSensePayloads {
    public static final int MAX_SIGNATURES = 160;

    private SpiritualSensePayloads() {
    }

    public record ServerboundSetSpiritualSensePayload(
            boolean active,
            boolean focused)
            implements CustomPacketPayload {
        public static final Type<ServerboundSetSpiritualSensePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_spiritual_sense"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundSetSpiritualSensePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                ServerboundSetSpiritualSensePayload::active,
                ByteBufCodecs.BOOL,
                ServerboundSetSpiritualSensePayload::focused,
                ServerboundSetSpiritualSensePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Signature(
            int id,
            int groupId,
            int kind,
            int tier,
            double x,
            double y,
            double z,
            float distance,
            float intensity,
            int flags,
            String name,
            String realm,
            int qiPercent,
            int healthPercent,
            String element,
            String method,
            String artifact) {
        public Signature {
            name = safe(name, 96);
            realm = safe(realm, 48);
            element = safe(element, 48);
            method = safe(method, 96);
            artifact = safe(artifact, 96);
        }
    }

    public record ClientboundSpiritualSensePayload(
            boolean active,
            boolean unavailable,
            float radius,
            float maxRadius,
            float focus,
            boolean directionalFocus,
            float detectionPower,
            int ambientQi,
            boolean selfUsingQi,
            List<Signature> signatures) implements CustomPacketPayload {
        public static final Type<ClientboundSpiritualSensePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "spiritual_sense_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ClientboundSpiritualSensePayload> STREAM_CODEC = StreamCodec.of(
                SpiritualSensePayloads::writeState,
                SpiritualSensePayloads::readState);

        public ClientboundSpiritualSensePayload {
            signatures = signatures == null
                    ? List.of()
                    : List.copyOf(signatures.subList(
                            0, Math.min(MAX_SIGNATURES, signatures.size())));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void writeState(
            RegistryFriendlyByteBuf buffer,
            ClientboundSpiritualSensePayload payload) {
        buffer.writeBoolean(payload.active());
        buffer.writeBoolean(payload.unavailable());
        buffer.writeFloat(payload.radius());
        buffer.writeFloat(payload.maxRadius());
        buffer.writeFloat(payload.focus());
        buffer.writeBoolean(payload.directionalFocus());
        buffer.writeFloat(payload.detectionPower());
        buffer.writeVarInt(Math.max(0, payload.ambientQi()));
        buffer.writeBoolean(payload.selfUsingQi());
        buffer.writeVarInt(payload.signatures().size());
        for (Signature signature : payload.signatures()) {
            buffer.writeVarInt(signature.id());
            buffer.writeInt(signature.groupId());
            buffer.writeByte(signature.kind());
            buffer.writeByte(signature.tier());
            buffer.writeDouble(signature.x());
            buffer.writeDouble(signature.y());
            buffer.writeDouble(signature.z());
            buffer.writeFloat(signature.distance());
            buffer.writeFloat(signature.intensity());
            buffer.writeVarInt(signature.flags());
            buffer.writeUtf(signature.name(), 96);
            buffer.writeUtf(signature.realm(), 48);
            buffer.writeVarInt(signature.qiPercent() + 1);
            buffer.writeVarInt(signature.healthPercent() + 1);
            buffer.writeUtf(signature.element(), 48);
            buffer.writeUtf(signature.method(), 96);
            buffer.writeUtf(signature.artifact(), 96);
        }
    }

    private static ClientboundSpiritualSensePayload readState(
            RegistryFriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        boolean unavailable = buffer.readBoolean();
        float radius = buffer.readFloat();
        float maxRadius = buffer.readFloat();
        float focus = buffer.readFloat();
        boolean directionalFocus = buffer.readBoolean();
        float detectionPower = buffer.readFloat();
        int ambientQi = Math.max(0, buffer.readVarInt());
        boolean selfUsingQi = buffer.readBoolean();
        int count = Math.min(MAX_SIGNATURES, Math.max(0, buffer.readVarInt()));
        List<Signature> signatures = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            signatures.add(new Signature(
                    buffer.readVarInt(),
                    buffer.readInt(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readUtf(96),
                    buffer.readUtf(48),
                    buffer.readVarInt() - 1,
                    buffer.readVarInt() - 1,
                    buffer.readUtf(48),
                    buffer.readUtf(96),
                    buffer.readUtf(96)));
        }
        return new ClientboundSpiritualSensePayload(
                active, unavailable, radius, maxRadius,
                focus, directionalFocus, detectionPower,
                ambientQi, selfUsingQi, signatures);
    }

    private static String safe(String value, int maxLength) {
        if (value == null || value.isBlank()) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID,
            bus = EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        private Handler() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToServer(
                    ServerboundSetSpiritualSensePayload.TYPE,
                    ServerboundSetSpiritualSensePayload.STREAM_CODEC,
                    Handler::handleSetActive);
            registrar.playToClient(
                    ClientboundSpiritualSensePayload.TYPE,
                    ClientboundSpiritualSensePayload.STREAM_CODEC,
                    Handler::handleState);
        }

        private static void handleSetActive(
                ServerboundSetSpiritualSensePayload payload,
                IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    SpiritualSenseSystem.setActive(
                            player, payload.active(), payload.focused());
                }
            });
        }

        private static void handleState(
                ClientboundSpiritualSensePayload payload,
                IPayloadContext context) {
            if (FMLEnvironment.dist.isClient()) {
                context.enqueueWork(() -> SpiritualSenseClient.accept(payload));
            }
        }
    }
}
