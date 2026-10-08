package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem;
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

/** Payloads for the persistent, server-authoritative 引道 state. */
public final class DaoGuidancePayloads {
    private static final int MAX_CHAPTERS = 64;

    private DaoGuidancePayloads() {
    }

    public record ChapterState(
            String id,
            boolean discovered,
            boolean complete,
            int objectiveIndex,
            int progress) {
        public ChapterState {
            id = safe(id, 64);
            objectiveIndex = Math.max(0, objectiveIndex);
            progress = Math.max(0, progress);
        }
    }

    public record ClientboundDaoGuidanceSyncPayload(
            String trackedChapter,
            List<ChapterState> chapters,
            String caveTargetDimension,
            int caveTargetX,
            int caveTargetZ) implements CustomPacketPayload {
        public static final Type<ClientboundDaoGuidanceSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "dao_guidance_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ClientboundDaoGuidanceSyncPayload> STREAM_CODEC = StreamCodec.of(
                DaoGuidancePayloads::writeSync,
                DaoGuidancePayloads::readSync);

        public ClientboundDaoGuidanceSyncPayload {
            trackedChapter = safe(trackedChapter, 64);
            chapters = chapters == null ? List.of()
                    : List.copyOf(chapters.subList(
                            0, Math.min(MAX_CHAPTERS, chapters.size())));
            caveTargetDimension = safe(caveTargetDimension, 128);
            if (caveTargetDimension.isBlank()) {
                caveTargetX = 0;
                caveTargetZ = 0;
            }
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundDaoGuidanceNoticePayload(
            String kind,
            String id,
            String nextId) implements CustomPacketPayload {
        public static final Type<ClientboundDaoGuidanceNoticePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "dao_guidance_notice"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ClientboundDaoGuidanceNoticePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        ClientboundDaoGuidanceNoticePayload::kind,
                        ByteBufCodecs.STRING_UTF8,
                        ClientboundDaoGuidanceNoticePayload::id,
                        ByteBufCodecs.STRING_UTF8,
                        ClientboundDaoGuidanceNoticePayload::nextId,
                        ClientboundDaoGuidanceNoticePayload::new);

        public ClientboundDaoGuidanceNoticePayload {
            kind = safe(kind, 32);
            id = safe(id, 96);
            nextId = safe(nextId, 64);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundTrackDaoGuidancePayload(String chapterId)
            implements CustomPacketPayload {
        public static final Type<ServerboundTrackDaoGuidancePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "track_dao_guidance"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundTrackDaoGuidancePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        ServerboundTrackDaoGuidancePayload::chapterId,
                        ServerboundTrackDaoGuidancePayload::new);

        public ServerboundTrackDaoGuidancePayload {
            chapterId = safe(chapterId, 64);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundDaoGuidanceActionPayload(String action)
            implements CustomPacketPayload {
        public static final Type<ServerboundDaoGuidanceActionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "dao_guidance_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,
                ServerboundDaoGuidanceActionPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        ServerboundDaoGuidanceActionPayload::action,
                        ServerboundDaoGuidanceActionPayload::new);

        public ServerboundDaoGuidanceActionPayload {
            action = safe(action, 64);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void writeSync(
            RegistryFriendlyByteBuf buffer,
            ClientboundDaoGuidanceSyncPayload payload) {
        buffer.writeUtf(payload.trackedChapter(), 64);
        buffer.writeVarInt(payload.chapters().size());
        for (ChapterState chapter : payload.chapters()) {
            buffer.writeUtf(chapter.id(), 64);
            buffer.writeBoolean(chapter.discovered());
            buffer.writeBoolean(chapter.complete());
            buffer.writeVarInt(chapter.objectiveIndex());
            buffer.writeVarInt(chapter.progress());
        }
        buffer.writeUtf(payload.caveTargetDimension(), 128);
        buffer.writeInt(payload.caveTargetX());
        buffer.writeInt(payload.caveTargetZ());
    }

    private static ClientboundDaoGuidanceSyncPayload readSync(
            RegistryFriendlyByteBuf buffer) {
        String tracked = buffer.readUtf(64);
        int count = Math.min(MAX_CHAPTERS, Math.max(0, buffer.readVarInt()));
        List<ChapterState> chapters = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            chapters.add(new ChapterState(
                    buffer.readUtf(64),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt()));
        }
        return new ClientboundDaoGuidanceSyncPayload(
                tracked, chapters,
                buffer.readUtf(128), buffer.readInt(), buffer.readInt());
    }

    private static String safe(String value, int maximum) {
        if (value == null || value.isBlank()) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
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
                    ClientboundDaoGuidanceSyncPayload.TYPE,
                    ClientboundDaoGuidanceSyncPayload.STREAM_CODEC,
                    Handler::handleSync);
            registrar.playToClient(
                    ClientboundDaoGuidanceNoticePayload.TYPE,
                    ClientboundDaoGuidanceNoticePayload.STREAM_CODEC,
                    Handler::handleNotice);
            registrar.playToServer(
                    ServerboundTrackDaoGuidancePayload.TYPE,
                    ServerboundTrackDaoGuidancePayload.STREAM_CODEC,
                    Handler::handleTrack);
            registrar.playToServer(
                    ServerboundDaoGuidanceActionPayload.TYPE,
                    ServerboundDaoGuidanceActionPayload.STREAM_CODEC,
                    Handler::handleAction);
        }

        private static void handleSync(
                ClientboundDaoGuidanceSyncPayload payload,
                IPayloadContext context) {
            if (FMLEnvironment.dist.isClient()) {
                context.enqueueWork(() -> DaoGuidanceClient.accept(payload));
            }
        }

        private static void handleNotice(
                ClientboundDaoGuidanceNoticePayload payload,
                IPayloadContext context) {
            if (FMLEnvironment.dist.isClient()) {
                context.enqueueWork(() -> DaoGuidanceClient.notice(payload));
            }
        }

        private static void handleTrack(
                ServerboundTrackDaoGuidancePayload payload,
                IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    DaoGuidanceSystem.track(player, payload.chapterId());
                }
            });
        }

        private static void handleAction(
                ServerboundDaoGuidanceActionPayload payload,
                IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                if (com.example.immortal_cultivation_mod.guidance.DaoGuidanceExpansion.isReading(payload.action())) {
                    DaoGuidanceSystem.acknowledge(player, payload.action());
                    return;
                }
                if (DaoGuidanceContent.OPEN_STATS.equals(payload.action())
                        || (DaoGuidanceContent.EQUIP_SPELL.equals(payload.action())
                        && !com.example.immortal_cultivation_mod.attachment.ModAttachments
                        .getData(player).knownSpells().isEmpty())
                        || (DaoGuidanceContent.OPEN_FLAG_GUI.equals(payload.action())
                        && player.containerMenu instanceof
                        com.example.immortal_cultivation_mod.magicritual.ui.CenterFuelMenu)) {
                    DaoGuidanceSystem.record(player, payload.action());
                }
            });
        }
    }
}
