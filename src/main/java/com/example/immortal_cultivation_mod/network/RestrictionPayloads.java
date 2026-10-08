package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity;
import com.example.immortal_cultivation_mod.screen.RestrictionTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class RestrictionPayloads {
    public static final int RESULT_MOVED = 0;
    public static final int RESULT_NOT_ENOUGH_QI = 1;
    public static final int RESULT_INVALID_MOVE = 2;
    public static final int RESULT_SOLVED = 3;
    public static final int RESULT_FAILED_MINOR = 4;
    public static final int RESULT_FAILED_MAJOR = 5;
    public static final int RESULT_FAILED_CATASTROPHIC = 6;
    public static final int RESULT_FAILED_LAYER_DAMAGED = 7;
    public static final int RESULT_NO_HINTS = 8;
    public static final int RESULT_NO_HINT_MOVE = 9;

    private RestrictionPayloads() {
    }

    public record ServerboundStartCarving(List<Integer> additions) implements CustomPacketPayload {
        public static final Type<ServerboundStartCarving> TYPE = RestrictionPayloads.type("restriction_start");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundStartCarving> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> writeInts(buf, payload.additions()),
                        buf -> new ServerboundStartCarving(readInts(buf, com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.RestrictionType.values().length)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundEvolve() implements CustomPacketPayload {
        public static final Type<ServerboundEvolve> TYPE = RestrictionPayloads.type("restriction_evolve");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundEvolve> STREAM_CODEC =
                StreamCodec.unit(new ServerboundEvolve());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundOpenPuzzle(BlockPos tablePos, long token, int firstLayer,
                                        int attemptedLayers, int difficulty, int moveLimit,
                                        int instabilityInterval, ResourceLocation artifact, int hintsRemaining,
                                        List<Integer> values, List<Integer> targets,
                                        List<Integer> flags, List<Integer> links)
            implements CustomPacketPayload {
        public static final Type<ClientboundOpenPuzzle> TYPE = RestrictionPayloads.type("restriction_open_puzzle");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenPuzzle> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> {
                    buf.writeBlockPos(payload.tablePos());
                    buf.writeLong(payload.token());
                    buf.writeVarInt(payload.firstLayer());
                    buf.writeVarInt(payload.attemptedLayers());
                    buf.writeVarInt(payload.difficulty());
                    buf.writeVarInt(payload.moveLimit());
                    buf.writeVarInt(payload.instabilityInterval());
                    buf.writeResourceLocation(payload.artifact());
                    buf.writeVarInt(payload.hintsRemaining());
                    writeInts(buf, payload.values());
                    writeInts(buf, payload.targets());
                    writeInts(buf, payload.flags());
                    writeInts(buf, payload.links());
                }, buf -> new ClientboundOpenPuzzle(
                        buf.readBlockPos(),
                        buf.readLong(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readResourceLocation(),
                        buf.readVarInt(),
                        readInts(buf, 16),
                        readInts(buf, 16),
                        readInts(buf, 16),
                        readInts(buf, 72)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundPuzzleMove(BlockPos tablePos, long token, int node,
                                        int direction) implements CustomPacketPayload {
        public static final Type<ServerboundPuzzleMove> TYPE = RestrictionPayloads.type("restriction_puzzle_move");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundPuzzleMove> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> {
                    buf.writeBlockPos(payload.tablePos());
                    buf.writeLong(payload.token());
                    buf.writeVarInt(payload.node());
                    buf.writeByte(payload.direction());
                }, buf -> new ServerboundPuzzleMove(
                        buf.readBlockPos(), buf.readLong(), buf.readVarInt(), buf.readByte()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundPuzzleHint(BlockPos tablePos, long token) implements CustomPacketPayload {
        public static final Type<ServerboundPuzzleHint> TYPE = RestrictionPayloads.type("restriction_puzzle_hint");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundPuzzleHint> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> {
                    buf.writeBlockPos(payload.tablePos());
                    buf.writeLong(payload.token());
                }, buf -> new ServerboundPuzzleHint(buf.readBlockPos(), buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundCancelPuzzle(BlockPos tablePos, long token) implements CustomPacketPayload {
        public static final Type<ServerboundCancelPuzzle> TYPE = RestrictionPayloads.type("restriction_cancel_puzzle");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCancelPuzzle> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> {
                    buf.writeBlockPos(payload.tablePos());
                    buf.writeLong(payload.token());
                }, buf -> new ServerboundCancelPuzzle(buf.readBlockPos(), buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundPuzzleState(long token, List<Integer> values, int movesUsed,
                                         int result, boolean instabilityShifted,
                                         int hintsRemaining, boolean hintUsed)
            implements CustomPacketPayload {
        public static final Type<ClientboundPuzzleState> TYPE = RestrictionPayloads.type("restriction_puzzle_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPuzzleState> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> {
                    buf.writeLong(payload.token());
                    writeInts(buf, payload.values());
                    buf.writeVarInt(payload.movesUsed());
                    buf.writeVarInt(payload.result());
                    buf.writeBoolean(payload.instabilityShifted());
                    buf.writeVarInt(payload.hintsRemaining());
                    buf.writeBoolean(payload.hintUsed());
                }, buf -> new ClientboundPuzzleState(
                        buf.readLong(),
                        readInts(buf, 16),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        private Handler() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("2");
            registrar.playToServer(ServerboundStartCarving.TYPE, ServerboundStartCarving.STREAM_CODEC,
                    Handler::handleStart);
            registrar.playToServer(ServerboundEvolve.TYPE, ServerboundEvolve.STREAM_CODEC,
                    Handler::handleEvolve);
            registrar.playToServer(ServerboundPuzzleMove.TYPE, ServerboundPuzzleMove.STREAM_CODEC,
                    Handler::handleMove);
            registrar.playToServer(ServerboundPuzzleHint.TYPE, ServerboundPuzzleHint.STREAM_CODEC,
                    Handler::handleHint);
            registrar.playToServer(ServerboundCancelPuzzle.TYPE, ServerboundCancelPuzzle.STREAM_CODEC,
                    Handler::handleCancel);
            registrar.playToClient(ClientboundOpenPuzzle.TYPE, ClientboundOpenPuzzle.STREAM_CODEC,
                    Handler::handleOpenClient);
            registrar.playToClient(ClientboundPuzzleState.TYPE, ClientboundPuzzleState.STREAM_CODEC,
                    Handler::handleStateClient);
        }

        private static void handleStart(ServerboundStartCarving payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof RestrictionTableMenu menu) {
                    menu.startPuzzle(player, payload.additions().stream().mapToInt(Integer::intValue).toArray());
                }
            });
        }

        private static void handleEvolve(ServerboundEvolve payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof RestrictionTableMenu menu) {
                    menu.evolve(player);
                }
            });
        }

        private static void handleMove(ServerboundPuzzleMove payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.level().getBlockEntity(payload.tablePos())
                            instanceof RestrictionTableBlockEntity table) {
                    table.movePuzzle(player, payload.token(), payload.node(), payload.direction());
                }
            });
        }

        private static void handleHint(ServerboundPuzzleHint payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.level().getBlockEntity(payload.tablePos())
                        instanceof RestrictionTableBlockEntity table) {
                    table.hintPuzzle(player, payload.token());
                }
            });
        }

        private static void handleCancel(ServerboundCancelPuzzle payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.level().getBlockEntity(payload.tablePos())
                        instanceof RestrictionTableBlockEntity table) {
                    table.cancelPuzzle(player, payload.token());
                }
            });
        }

        private static void handleOpenClient(ClientboundOpenPuzzle payload, IPayloadContext context) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleOpenRestrictionPuzzle(payload, context);
            }
        }

        private static void handleStateClient(ClientboundPuzzleState payload, IPayloadContext context) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleRestrictionPuzzleState(payload, context);
            }
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path));
    }

    private static void writeInts(RegistryFriendlyByteBuf buffer, List<Integer> values) {
        buffer.writeVarInt(values.size());
        for (int value : values) {
            buffer.writeVarInt(value);
        }
    }

    private static List<Integer> readInts(RegistryFriendlyByteBuf buffer, int maxSize) {
        int size = Math.max(0, Math.min(maxSize, buffer.readVarInt()));
        List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buffer.readVarInt());
        }
        return List.copyOf(values);
    }
}
