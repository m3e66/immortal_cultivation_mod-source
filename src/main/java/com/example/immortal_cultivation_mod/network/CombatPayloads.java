package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientPayloadHandlers;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
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

import java.util.UUID;

public final class CombatPayloads {
    public static final int ATTACK_BEGIN = 0;
    public static final int ATTACK_NORMAL = 1;
    public static final int ATTACK_HEAVY = 2;
    public static final int ATTACK_CANCEL = 3;
    public static final int SPEAR_HEAVY_THRUST_ANIMATION = -1;
    public static final int BLOCK_ANIMATION_START = -2;
    public static final int BLOCK_ANIMATION_STOP = -3;
    public static final int FUCHEN_HEAVY_SPIN_ANIMATION = -4;
    public static final int FUCHEN_BIND_ANIMATION_START = -5;
    public static final int FUCHEN_BIND_ANIMATION_STOP = -6;
    public static final int XIUJIAN_FIRE_RIGHT_ANIMATION = -7;
    public static final int XIUJIAN_FIRE_LEFT_ANIMATION = -8;
    public static final int WHIP_HEAVY_ANIMATION = -9;

    private CombatPayloads() {
    }

    public record ClientboundCombatStatePayload(
            boolean active,
            float posture,
            float maxPosture,
            int remainingTicks,
            boolean broken
    ) implements CustomPacketPayload {
        public static final Type<ClientboundCombatStatePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "combat_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCombatStatePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeBoolean(data.active());
                            buf.writeFloat(data.posture());
                            buf.writeFloat(data.maxPosture());
                            buf.writeVarInt(data.remainingTicks());
                            buf.writeBoolean(data.broken());
                        },
                        buf -> new ClientboundCombatStatePayload(
                                buf.readBoolean(),
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readVarInt(),
                                buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundSwordComboAnimationPayload(UUID playerId, int slashIndex)
            implements CustomPacketPayload {
        public static final Type<ClientboundSwordComboAnimationPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "sword_combo_animation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSwordComboAnimationPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeLong(data.playerId().getMostSignificantBits());
                            buf.writeLong(data.playerId().getLeastSignificantBits());
                            buf.writeVarInt(data.slashIndex());
                        },
                        buf -> new ClientboundSwordComboAnimationPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundCombatAttackPayload(
            int action, int entityId, long packedBlockPos) implements CustomPacketPayload {
        public static final Type<ServerboundCombatAttackPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "combat_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCombatAttackPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeVarInt(data.action());
                            buf.writeVarInt(data.entityId());
                            buf.writeLong(data.packedBlockPos());
                        },
                        buf -> new ServerboundCombatAttackPayload(
                                buf.readVarInt(), buf.readVarInt(), buf.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetBlockingPayload(boolean blocking)
            implements CustomPacketPayload {
        public static final Type<ServerboundSetBlockingPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_combat_blocking"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetBlockingPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL,
                        ServerboundSetBlockingPayload::blocking,
                        ServerboundSetBlockingPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetCombatEnabledPayload(boolean enabled)
            implements CustomPacketPayload {
        public static final Type<ServerboundSetCombatEnabledPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_combat_enabled"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetCombatEnabledPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL,
                        ServerboundSetCombatEnabledPayload::enabled,
                        ServerboundSetCombatEnabledPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundToggleManualCombatPayload() implements CustomPacketPayload {
        public static final Type<ServerboundToggleManualCombatPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "toggle_manual_combat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleManualCombatPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundToggleManualCombatPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetOutwardQiPayload(boolean enabled, boolean blockBreaking, boolean autoFit)
            implements CustomPacketPayload {
        public static final Type<ServerboundSetOutwardQiPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_outward_qi"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetOutwardQiPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeBoolean(data.enabled());
                            buf.writeBoolean(data.blockBreaking());
                            buf.writeBoolean(data.autoFit());
                        },
                        buf -> new ServerboundSetOutwardQiPayload(
                                buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetBlockBreakingPayload(int disabledMask) implements CustomPacketPayload {
        public static final Type<ServerboundSetBlockBreakingPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "set_block_breaking"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetBlockBreakingPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT,
                        ServerboundSetBlockBreakingPayload::disabledMask,
                        ServerboundSetBlockBreakingPayload::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundQiSizePayload(int steps) implements CustomPacketPayload {
        public static final Type<ServerboundQiSizePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "external_qi_size"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundQiSizePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ServerboundQiSizePayload::steps, ServerboundQiSizePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundWeaponQiSlashPayload() implements CustomPacketPayload {
        public static final Type<ServerboundWeaponQiSlashPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "weapon_qi_slash"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundWeaponQiSlashPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundWeaponQiSlashPayload());

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
            var registrar = event.registrar("2");
            registrar.playToServer(ServerboundQiSizePayload.TYPE, ServerboundQiSizePayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player)
                            com.example.immortal_cultivation_mod.combat.ExternalQiSize.adjust(player, payload.steps());
                    }));
            registrar.playToClient(
                    ClientboundCombatStatePayload.TYPE,
                    ClientboundCombatStatePayload.STREAM_CODEC,
                    Handler::handleCombatStateClient);
            registrar.playToClient(
                    ClientboundSwordComboAnimationPayload.TYPE,
                    ClientboundSwordComboAnimationPayload.STREAM_CODEC,
                    Handler::handleSwordComboClient);
            registrar.playToServer(
                    ServerboundCombatAttackPayload.TYPE,
                    ServerboundCombatAttackPayload.STREAM_CODEC,
                    Handler::handleCombatAttackServer);
            registrar.playToServer(
                    ServerboundSetBlockingPayload.TYPE,
                    ServerboundSetBlockingPayload.STREAM_CODEC,
                    Handler::handleSetBlockingServer);
            registrar.playToServer(
                    ServerboundSetCombatEnabledPayload.TYPE,
                    ServerboundSetCombatEnabledPayload.STREAM_CODEC,
                    Handler::handleCombatEnabledServer);
            registrar.playToServer(
                    ServerboundToggleManualCombatPayload.TYPE,
                    ServerboundToggleManualCombatPayload.STREAM_CODEC,
                    Handler::handleToggleManualCombatServer);
            registrar.playToServer(
                    ServerboundSetBlockBreakingPayload.TYPE,
                    ServerboundSetBlockBreakingPayload.STREAM_CODEC,
                    Handler::handleBlockBreakingServer);
            registrar.playToServer(
                    ServerboundSetOutwardQiPayload.TYPE,
                    ServerboundSetOutwardQiPayload.STREAM_CODEC,
                    Handler::handleOutwardQiEnabledServer);
            registrar.playToServer(
                    ServerboundWeaponQiSlashPayload.TYPE,
                    ServerboundWeaponQiSlashPayload.STREAM_CODEC,
                    Handler::handleWeaponQiSlashServer);
        }

        private static void handleCombatStateClient(
                ClientboundCombatStatePayload payload, IPayloadContext context) {
            if (FMLEnvironment.dist.isClient()) {
                ClientPayloadHandlers.handleCombatState(payload, context);
            }
        }

        private static void handleSwordComboClient(
                ClientboundSwordComboAnimationPayload payload, IPayloadContext context) {
            if (FMLEnvironment.dist.isClient()) {
                ClientPayloadHandlers.handleSwordComboAnimation(payload, context);
            }
        }

        private static void handleCombatAttackServer(
                ServerboundCombatAttackPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                switch (payload.action()) {
                    case ATTACK_BEGIN -> CombatStateManager.beginHeavyCharge(player);
                    case ATTACK_NORMAL -> CombatStateManager.performNormalAttack(
                            player, payload.entityId());
                    case ATTACK_HEAVY -> CombatStateManager.performHeavyAttack(
                            player, payload.entityId(), payload.packedBlockPos());
                    case ATTACK_CANCEL -> CombatStateManager.cancelHeavyCharge(player);
                    default -> {
                    }
                }
            });
        }

        private static void handleSetBlockingServer(
                ServerboundSetBlockingPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    CombatStateManager.setBlocking(player, payload.blocking());
                }
            });
        }

        private static void handleCombatEnabledServer(
                ServerboundSetCombatEnabledPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    CombatStateManager.setEnabled(player, payload.enabled());
                }
            });
        }

        private static void handleToggleManualCombatServer(
                ServerboundToggleManualCombatPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    CombatStateManager.toggleManualCombat(player);
                }
            });
        }

        private static void handleBlockBreakingServer(
                ServerboundSetBlockBreakingPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    int disabledMask = payload.disabledMask() & BlockBreakingSettings.validMask();
                    BlockBreakingSettings.setDisabledMask(player, disabledMask);
                }
            });
        }

        private static void handleOutwardQiEnabledServer(
                ServerboundSetOutwardQiPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.example.immortal_cultivation_mod.item.QiInfusedWeapon
                            .setOutwardQiRelease(player, payload.enabled());
                    com.example.immortal_cultivation_mod.item.QiInfusedWeapon
                            .setOutwardQiBlockBreaking(player, payload.blockBreaking());
                    com.example.immortal_cultivation_mod.item.QiInfusedWeapon
                            .setOutwardQiAutoFit(player, payload.autoFit());
                }
            });
        }

        private static void handleWeaponQiSlashServer(
                ServerboundWeaponQiSlashPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    CombatStateManager.broadcastSwordCombo(player);
                }
            });
        }
    }
}
