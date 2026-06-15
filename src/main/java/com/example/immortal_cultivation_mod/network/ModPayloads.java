package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.UUID;

public class ModPayloads {

    public record ClientboundSyncPlayerDataPayload(
            int qi,
            int maxQi,
            int maxHp,
            int maxAge,
            String cultivationLevel,
            int luck,
            int moral,
            String bodyType,
            int soul,
            int thoughts,
            List<String> spiritRoots,
            String spiritRootGrade,
            int agePenalty,
            long cultivationProgress,
            String activeCultivationMethod,
            int blood,
            List<String> knownSpells,
            boolean isMeditating,
            int skillPoints,
            int maxHpBonus,
            int maxQiBonus,
            int maxEnergyBonus,
            int physicalAttack,
            int magicAttack,
            int mentalAttack
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundSyncPlayerDataPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "sync_player_data"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncPlayerDataPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            ByteBufCodecs.VAR_INT.encode(buf, data.qi());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxQi());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxHp());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxAge());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.cultivationLevel());
                            ByteBufCodecs.VAR_INT.encode(buf, data.luck());
                            ByteBufCodecs.VAR_INT.encode(buf, data.moral());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.bodyType());
                            ByteBufCodecs.VAR_INT.encode(buf, data.soul());
                            ByteBufCodecs.VAR_INT.encode(buf, data.thoughts());
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, data.spiritRoots());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.spiritRootGrade());
                            ByteBufCodecs.VAR_INT.encode(buf, data.agePenalty());
                            buf.writeVarLong(data.cultivationProgress());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.activeCultivationMethod());
                            ByteBufCodecs.VAR_INT.encode(buf, data.blood());
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, data.knownSpells());
                            ByteBufCodecs.BOOL.encode(buf, data.isMeditating());
                            ByteBufCodecs.VAR_INT.encode(buf, data.skillPoints());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxHpBonus());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxQiBonus());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxEnergyBonus());
                            ByteBufCodecs.VAR_INT.encode(buf, data.physicalAttack());
                            ByteBufCodecs.VAR_INT.encode(buf, data.magicAttack());
                            ByteBufCodecs.VAR_INT.encode(buf, data.mentalAttack());
                        },
                        buf -> new ClientboundSyncPlayerDataPayload(
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                buf.readVarLong(),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                ByteBufCodecs.BOOL.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf)
                        ));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundRequestBreakthroughPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundRequestBreakthroughPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "request_breakthrough"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRequestBreakthroughPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundRequestBreakthroughPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundMeditationStatePayload(UUID playerId, boolean meditating) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundMeditationStatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "meditation_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMeditationStatePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeLong(data.playerId().getMostSignificantBits());
                            buf.writeLong(data.playerId().getLeastSignificantBits());
                            ByteBufCodecs.BOOL.encode(buf, data.meditating());
                        },
                        buf -> new ClientboundMeditationStatePayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.BOOL.decode(buf)
                        ));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundSpendSkillPointPayload(String stat) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSpendSkillPointPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "spend_skill_point"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSpendSkillPointPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerboundSpendSkillPointPayload::stat, ServerboundSpendSkillPointPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundLearnSpellPayload(String spellName) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundLearnSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "learn_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundLearnSpellPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerboundLearnSpellPayload::spellName, ServerboundLearnSpellPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundActivateMethodPayload(String methodId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundActivateMethodPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "activate_method"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundActivateMethodPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerboundActivateMethodPayload::methodId, ServerboundActivateMethodPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCastSpellPayload(String spellName) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCastSpellPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerboundCastSpellPayload::spellName, ServerboundCastSpellPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundLightBeamAirPunchPayload(boolean shootAll) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundLightBeamAirPunchPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "light_beam_air_punch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundLightBeamAirPunchPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundLightBeamAirPunchPayload::shootAll, ServerboundLightBeamAirPunchPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundWindStepJumpPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundWindStepJumpPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "wind_step_jump"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundWindStepJumpPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundWindStepJumpPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundMeditatePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundMeditatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "meditate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundMeditatePayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundMeditatePayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDebugAdjustStatPayload(String stat, int delta) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDebugAdjustStatPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "debug_adjust_stat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugAdjustStatPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ServerboundDebugAdjustStatPayload::stat,
                        ByteBufCodecs.VAR_INT, ServerboundDebugAdjustStatPayload::delta,
                        ServerboundDebugAdjustStatPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDebugSetSpiritRootsPayload(List<String> roots, String grade) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDebugSetSpiritRootsPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "debug_set_spirit_roots"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugSetSpiritRootsPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, data.roots());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.grade());
                        },
                        buf -> new ServerboundDebugSetSpiritRootsPayload(
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf)
                        ));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCompleteEnlightenmentPayload(List<String> roots, String grade, String bodyType) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCompleteEnlightenmentPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "complete_enlightenment"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCompleteEnlightenmentPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, data.roots());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.grade());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.bodyType());
                        },
                        buf -> new ServerboundCompleteEnlightenmentPayload(
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf)
                        ));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModPayloadsHandler {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(ClientboundSyncPlayerDataPayload.TYPE, ClientboundSyncPlayerDataPayload.STREAM_CODEC, ModPayloadsHandler::handleSyncPlayerDataOnClient);
            registrar.playToClient(ClientboundMeditationStatePayload.TYPE, ClientboundMeditationStatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditationStateOnClient);
            registrar.playToServer(ServerboundRequestBreakthroughPayload.TYPE, ServerboundRequestBreakthroughPayload.STREAM_CODEC, ModPayloadsHandler::handleBreakthroughOnServer);
            registrar.playToServer(ServerboundSpendSkillPointPayload.TYPE, ServerboundSpendSkillPointPayload.STREAM_CODEC, ModPayloadsHandler::handleSpendSkillPointOnServer);
            registrar.playToServer(ServerboundLearnSpellPayload.TYPE, ServerboundLearnSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleLearnSpellOnServer);
            registrar.playToServer(ServerboundActivateMethodPayload.TYPE, ServerboundActivateMethodPayload.STREAM_CODEC, ModPayloadsHandler::handleActivateMethodOnServer);
            registrar.playToServer(ServerboundCastSpellPayload.TYPE, ServerboundCastSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleCastSpellOnServer);
            registrar.playToServer(ServerboundLightBeamAirPunchPayload.TYPE, ServerboundLightBeamAirPunchPayload.STREAM_CODEC, ModPayloadsHandler::handleLightBeamAirPunchOnServer);
            registrar.playToServer(ServerboundWindStepJumpPayload.TYPE, ServerboundWindStepJumpPayload.STREAM_CODEC, ModPayloadsHandler::handleWindStepJumpOnServer);
            registrar.playToServer(ServerboundMeditatePayload.TYPE, ServerboundMeditatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditateOnServer);
            registrar.playToServer(ServerboundDebugAdjustStatPayload.TYPE, ServerboundDebugAdjustStatPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugAdjustStatOnServer);
            registrar.playToServer(ServerboundDebugSetSpiritRootsPayload.TYPE, ServerboundDebugSetSpiritRootsPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugSetSpiritRootsOnServer);
            registrar.playToServer(ServerboundCompleteEnlightenmentPayload.TYPE, ServerboundCompleteEnlightenmentPayload.STREAM_CODEC, ModPayloadsHandler::handleCompleteEnlightenmentOnServer);
        }

        private static void handleActivateMethodOnServer(ServerboundActivateMethodPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var def = CultivationMethods.get(payload.methodId());
                    if (def == null) {
                        return;
                    }
                    var data = ModAttachments.getData(sp).withActiveCultivationMethod(payload.methodId());
                    ModAttachments.setData(sp, data);
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".method_activated",
                            Component.translatable(def.nameKey())));
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                }
            });
        }

        private static void handleDebugAdjustStatOnServer(ServerboundDebugAdjustStatPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var data = ModAttachments.getData(sp);
                    var updated = switch (payload.stat()) {
                        case "cultivation_level" -> data.withCultivationLevel(nextCultivationLevel(data.cultivationLevel(), payload.delta()));
                        case "body_type" -> data.withBodyType(BodyTypes.next(data.bodyType(), payload.delta()));
                        case "age" -> data.withAgePenalty(clamp(
                                data.agePenalty() - payload.delta(),
                                0,
                                Math.max(0, com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge() - 1)
                        ));
                        case "moral" -> data.withMoral(clamp(data.moral() + payload.delta(), 0, 100));
                        case "luck" -> data.withLuck(clamp(data.luck() + payload.delta(), 0, 100));
                        case "soul" -> data.withSoul(clamp(data.soul() + payload.delta(), 0, 1000));
                        case "thoughts" -> data.withThoughts(clamp(data.thoughts() + payload.delta(), 0, 1000));
                        case "spirit_roots" -> data.withSpiritRoots(SpiritRoots.nextRootSet(data.spiritRoots(), payload.delta()), data.spiritRootGrade());
                        case "spirit_root_grade" -> data.withSpiritRoots(data.spiritRoots(), SpiritRoots.nextGrade(data.spiritRootGrade(), payload.delta()));
                        case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental" -> data.debugAdjustStat(payload.stat(), payload.delta());
                        default -> data;
                    };
                    if (updated != data) {
                        ModAttachments.setData(sp, updated);
                        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                    }
                }
            });
        }

        private static void handleDebugSetSpiritRootsOnServer(ServerboundDebugSetSpiritRootsPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var data = ModAttachments.getData(sp);
                    var roots = SpiritRoots.sanitizeRootList(payload.roots());
                    String grade = SpiritRoots.sanitizeGrade(payload.grade());
                    ModAttachments.setData(sp, data.withSpiritRoots(roots, grade));
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                }
            });
        }

        private static String nextCultivationLevel(String current, int direction) {
            List<String> values = com.example.immortal_cultivation_mod.attachment.CultivationLevels.allLevels();
            int index = values.indexOf(current);
            if (index < 0) {
                index = 0;
            }
            int next = Math.floorMod(index + (direction < 0 ? -1 : 1), values.size());
            return values.get(next);
        }

        private static void handleCompleteEnlightenmentOnServer(ServerboundCompleteEnlightenmentPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var data = ModAttachments.getData(sp);
                    if (!CultivationLevels.isMortal(data.cultivationLevel())) {
                        sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_already_cultivating"));
                        return;
                    }
                    if (!consumeEnlightenmentPill(sp)) {
                        return;
                    }

                    var roots = SpiritRoots.sanitizeRootList(payload.roots());
                    if (roots.isEmpty()) {
                        roots = List.of(SpiritRoots.METAL);
                    }
                    String grade = SpiritRoots.sanitizeGrade(payload.grade());
                    String bodyType = BodyTypes.sanitize(payload.bodyType());
                    var updated = data
                            .withCultivationLevel(CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY)
                            .withSpiritRoots(roots, grade)
                            .withBodyType(bodyType);
                    ModAttachments.setData(sp, updated);
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_success"));
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                }
            });
        }

        private static boolean consumeEnlightenmentPill(ServerPlayer sp) {
            if (sp.getAbilities().instabuild) {
                return true;
            }
            var pill = ModItems.ENLIGHTENMENT_PILL.get();
            if (sp.getMainHandItem().is(pill)) {
                sp.getMainHandItem().shrink(1);
                return true;
            }
            if (sp.getOffhandItem().is(pill)) {
                sp.getOffhandItem().shrink(1);
                return true;
            }
            for (var stack : sp.getInventory().items) {
                if (stack.is(pill)) {
                    stack.shrink(1);
                    return true;
                }
            }
            return false;
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private static void handleLearnSpellOnServer(ServerboundLearnSpellPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    String spellId = ModSpells.normalizeId(payload.spellName());
                    var spell = ModSpells.get(spellId);
                    if (spell == null) {
                        return;
                    }
                    var data = ModAttachments.getData(sp);
                    if (!ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                        sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", spell.requiredLevel()));
                        return;
                    }
                    var known = new java.util.ArrayList<>(data.knownSpells());
                    if (!known.contains(spellId)) {
                        known.add(spellId);
                        ModAttachments.setData(sp, data.withKnownSpells(known));
                        consumeLearnedScroll(sp, spellId);
                        sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_learned", spell.name()));
                        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                    }
                }
            });
        }

        private static void consumeLearnedScroll(ServerPlayer sp, String spellId) {
            if (sp.getAbilities().instabuild) {
                return;
            }

            var scroll = switch (spellId) {
                case ModSpells.FIREBALL -> ModItems.FIREBALL_SCROLL.get();
                case ModSpells.LINGBENG -> ModItems.LINGBENG_SCROLL.get();
                case ModSpells.REGENERATION -> ModItems.REGENERATION_SCROLL.get();
                case ModSpells.BEAM -> ModItems.BEAM_SCROLL.get();
                case ModSpells.EARTH_ESCAPE -> ModItems.EARTH_ESCAPE_SCROLL.get();
                case ModSpells.CLEANSE -> ModItems.CLEANSE_SCROLL.get();
                case ModSpells.QI_GATHERING -> ModItems.QI_GATHERING_SCROLL.get();
                case ModSpells.IGNITE_FLARE -> ModItems.IGNITE_FLARE_SCROLL.get();
                case ModSpells.SPIRIT_SIGHT -> ModItems.SPIRIT_SIGHT_SCROLL.get();
                case ModSpells.ZHENSHAN_PALM -> ModItems.ZHENSHAN_PALM_SCROLL.get();
                case ModSpells.LIGHT_BEAM_ATTACK -> ModItems.LIGHT_BEAM_ATTACK_SCROLL.get();
                case ModSpells.DIELANG_SHIELD -> ModItems.DIELANG_SHIELD_SCROLL.get();
                case ModSpells.LINGZHI_BULLET -> ModItems.LINGZHI_BULLET_SCROLL.get();
                case ModSpells.WIND_BLADE -> ModItems.WIND_BLADE_SCROLL.get();
                case ModSpells.WIND_STEP -> ModItems.WIND_STEP_SCROLL.get();
                case ModSpells.YUFENG_JUE -> ModItems.YUFENG_JUE_SCROLL.get();
                case ModSpells.SMOKE_ART -> ModItems.SMOKE_ART_SCROLL.get();
                case ModSpells.SLIDING_WATER -> ModItems.SLIDING_WATER_SCROLL.get();
                case ModSpells.DINGSHEN -> ModItems.DINGSHEN_SCROLL.get();
                case ModSpells.YINLEI_JUE -> ModItems.YINLEI_JUE_SCROLL.get();
                case ModSpells.WULEI_ZHENGFA -> ModItems.WULEI_ZHENGFA_SCROLL.get();
                default -> null;
            };
            if (scroll == null) {
                return;
            }

            if (sp.getMainHandItem().is(scroll)) {
                sp.getMainHandItem().shrink(1);
                return;
            }
            if (sp.getOffhandItem().is(scroll)) {
                sp.getOffhandItem().shrink(1);
                return;
            }
            for (var stack : sp.getInventory().items) {
                if (stack.is(scroll)) {
                    stack.shrink(1);
                    return;
                }
            }
        }

        private static void handleSyncPlayerDataOnClient(ClientboundSyncPlayerDataPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                var data = new ModAttachments.CultivationData(
                        payload.qi(),
                        payload.cultivationLevel(),
                        payload.luck(),
                        payload.moral(),
                        payload.bodyType(),
                        payload.soul(),
                        payload.thoughts(),
                        payload.spiritRoots(),
                        payload.spiritRootGrade(),
                        payload.agePenalty(),
                        payload.cultivationProgress(),
                        payload.activeCultivationMethod(),
                        payload.blood(),
                        payload.knownSpells(),
                        payload.isMeditating(),
                        payload.skillPoints(),
                        payload.maxHpBonus(),
                        payload.maxQiBonus(),
                        payload.maxEnergyBonus(),
                        payload.physicalAttack(),
                        payload.magicAttack(),
                        payload.mentalAttack()
                );
                com.example.immortal_cultivation_mod.client.ClientData.cultivationData = data;
                com.example.immortal_cultivation_mod.client.ClientData.reconcileWheelSelection(payload.knownSpells());
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    com.example.immortal_cultivation_mod.client.ClientData.updateMeditationState(player.getUUID(), payload.isMeditating());
                }
            });
        }

        private static void handleMeditationStateOnClient(ClientboundMeditationStatePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                com.example.immortal_cultivation_mod.client.ClientData.updateMeditationState(payload.playerId(), payload.meditating());
            });
        }

        private static void handleBreakthroughOnServer(ServerboundRequestBreakthroughPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.event.ServerEvents.tryBreakthrough(sp);
                }
            });
        }

        private static void handleSpendSkillPointOnServer(ServerboundSpendSkillPointPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var data = ModAttachments.getData(sp);
                    var updated = data.spendSkillPoint(payload.stat());
                    if ("qi".equals(payload.stat()) && updated != data) {
                        updated = updated.withQi(updated.qi() + 40);
                    }
                    ModAttachments.setData(sp, updated);
                    if ("hp".equals(payload.stat()) && updated != data) {
                        sp.heal(4.0F);
                    }
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                }
            });
        }

        private static void handleCastSpellOnServer(ServerboundCastSpellPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    String spellId = ModSpells.normalizeId(payload.spellName());
                    var spell = ModSpells.get(spellId);
                    if (spell == null) {
                        return;
                    }
                    var data = ModAttachments.getData(sp);
                    if (com.example.immortal_cultivation_mod.spell.Weiya.isSuppressed(sp)) {
                        return;
                    }
                    if (sp.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(spellId)) {
                        return;
                    }
                    boolean innateKnown = ModSpells.isInnateKnown(spellId, data);
                    if (!innateKnown && !data.knownSpells().stream().map(ModSpells::normalizeId).toList().contains(spellId)) {
                        sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_not_learned"));
                        return;
                    }
                    if (!ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                        sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", spell.requiredLevel()));
                        return;
                    }
                    if (ModSpells.FIREBALL.equals(spellId)) {
                        castFireball(sp, data, spell);
                    } else if (ModSpells.LINGBENG.equals(spellId)) {
                        castLingbeng(sp, data, spell);
                    } else if (ModSpells.REGENERATION.equals(spellId)) {
                        castRegeneration(sp, data, spell);
                    } else if (ModSpells.BEAM.equals(spellId)) {
                        castBeam(sp, data, spell);
                    } else if (ModSpells.EARTH_ESCAPE.equals(spellId)) {
                        castEarthEscape(sp, data);
                    } else if (ModSpells.CLEANSE.equals(spellId)) {
                        castCleanse(sp, data, spell);
                    } else if (ModSpells.QI_GATHERING.equals(spellId)) {
                        castQiGathering(sp);
                    } else if (ModSpells.IGNITE_FLARE.equals(spellId)) {
                        castIgniteFlare(sp, data, spell);
                    } else if (ModSpells.SPIRIT_SIGHT.equals(spellId)) {
                        com.example.immortal_cultivation_mod.event.ServerEvents.toggleSpiritSight(sp);
                    } else if (ModSpells.ZHENSHAN_PALM.equals(spellId)) {
                        castZhenshanPalm(sp, data, spell);
                    } else if (ModSpells.LIGHT_BEAM_ATTACK.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.LightBeamAttack.cast(sp);
                    } else if (ModSpells.DIELANG_SHIELD.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.DielangShield.cast(sp);
                    } else if (ModSpells.LINGZHI_BULLET.equals(spellId)) {
                        castLingzhiBullet(sp, data, spell);
                    } else if (ModSpells.WIND_BLADE.equals(spellId)) {
                        castWindBlade(sp, data, spell);
                    } else if (ModSpells.WIND_STEP.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.WindStep.toggle(sp);
                    } else if (ModSpells.YUFENG_JUE.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.YufengJue.toggle(sp);
                    } else if (ModSpells.SMOKE_ART.equals(spellId)) {
                        castSmokeArt(sp, data, spell);
                    } else if (ModSpells.SLIDING_WATER.equals(spellId)) {
                        castSlidingWater(sp, data, spell);
                    } else if (ModSpells.WEIYA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.Weiya.toggle(sp);
                    } else if (ModSpells.ABSORB_CULTIVATION.equals(spellId)) {
                        castAbsorbCultivation(sp, data, spell);
                    } else if (ModSpells.TUNTIAN.equals(spellId)) {
                        castTuntian(sp, data);
                    } else if (ModSpells.FENGYA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.Fengya.toggle(sp);
                    } else if (ModSpells.DINGSHEN.equals(spellId)) {
                        castDingshen(sp, data, spell);
                    } else if (ModSpells.YINLEI_JUE.equals(spellId)) {
                        castLightning(sp, data, spell, false);
                    } else if (ModSpells.WULEI_ZHENGFA.equals(spellId)) {
                        castLightning(sp, data, spell, true);
                    }
                }
            });
        }

        private static void handleLightBeamAirPunchOnServer(ServerboundLightBeamAirPunchPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp && !sp.hasEffect(ModEffects.QI_GATHERING) && !sp.hasEffect(ModEffects.EARTH_ESCAPE)) {
                    Vec3 target = sp.getEyePosition().add(sp.getLookAngle().scale(64.0D));
                    if (!com.example.immortal_cultivation_mod.spell.LightBeamAttack.shoot(sp, target, payload.shootAll())) {
                        com.example.immortal_cultivation_mod.spell.Fengya.release(
                                sp,
                                sp.getEyePosition().add(sp.getLookAngle().scale(8.0D)),
                                payload.shootAll());
                    }
                }
            });
        }

        private static void handleWindStepJumpOnServer(ServerboundWindStepJumpPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.spell.WindStep.doubleJump(sp);
                }
            });
        }

        private static void castLingzhiBullet(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.LingzhiBulletProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 2.1f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castWindBlade(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.9f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castSmokeArt(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.SmokeProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.4f, 0.2f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castSlidingWater(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.SlidingWaterProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.6f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castAbsorbCultivation(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isReincarnationTrueArt(data.activeCultivationMethod())) {
                return;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.AbsorbCultivationProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.8f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castTuntian(ServerPlayer sp, ModAttachments.CultivationData data) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isTuntianDemonArt(data.activeCultivationMethod())) {
                return;
            }
            if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                return;
            }

            final int radius = 5;
            int consumedBlocks = 0;
            net.minecraft.core.BlockPos center = sp.blockPosition();
            net.minecraft.world.phys.Vec3 centerVec = sp.position().add(0.0D, sp.getBbHeight() * 0.5D, 0.0D);
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                if (pos.distSqr(center) > radius * radius) {
                    continue;
                }
                var state = serverLevel.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(serverLevel, pos) < 0.0F) {
                    continue;
                }
                if (serverLevel.destroyBlock(pos, false, sp)) {
                    consumedBlocks++;
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                            3, 0.25D, 0.25D, 0.25D, 0.05D);
                }
            }

            long progressGain = consumedBlocks * 10L;
            var targets = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(centerVec, centerVec).inflate(radius),
                    entity -> entity.isAlive() && !entity.getUUID().equals(sp.getUUID()));
            for (var target : targets) {
                if (target.distanceToSqr(centerVec) > radius * radius) {
                    continue;
                }
                float maxHealth = target.getMaxHealth();
                target.hurt(sp.damageSources().magic(), 100.0F);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        24, 0.35D, 0.35D, 0.35D, 0.08D);
                if (!target.isAlive() || target.isDeadOrDying()) {
                    progressGain += Math.max(0L, (long) (maxHealth / 50.0F)) * 20L;
                }
            }

            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                    centerVec.x, centerVec.y, centerVec.z, 70, radius * 0.45D, 1.4D, radius * 0.45D, 0.12D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    centerVec.x, centerVec.y, centerVec.z, 45, radius * 0.35D, 0.8D, radius * 0.35D, 0.04D);

            if (progressGain > 0L) {
                long need = Math.max(1L, com.example.immortal_cultivation_mod.attachment.CultivationLevels.getTotalQiNeeded(data.cultivationLevel()));
                ModAttachments.setData(sp, data.withCultivationProgress(Math.min(need, data.cultivationProgress() + progressGain)));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castDingshen(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.DingshenProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.9f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castLightning(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, boolean greater) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.LightningProjectileEntity(sp.level(), sp, greater);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, greater ? 3.2F : 2.4F, greater ? 0.2F : 0.4F);
            sp.level().addFreshEntity(projectile);
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.example.immortal_cultivation_mod.entity.SpellImpactParticles.lightning(serverLevel, sp.getEyePosition().add(sp.getLookAngle().scale(0.9D)));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castZhenshanPalm(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var palm = new com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity(sp.level(), sp);
            sp.level().addFreshEntity(palm);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castCleanse(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var harmful = sp.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                    .map(MobEffectInstance::getEffect)
                    .toList();
            for (var effect : harmful) {
                sp.removeEffect(effect);
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castQiGathering(ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(ModEffects.QI_GATHERING, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                PhotonEffects.meditatingStart(serverLevel, sp);
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.startQiGathering(sp);
        }

        private static void castIgniteFlare(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.8f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castEarthEscape(ServerPlayer sp, ModAttachments.CultivationData data) {
            if (sp.hasEffect(ModEffects.EARTH_ESCAPE)) {
                return;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, 8)) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            sp.addEffect(new MobEffectInstance(ModEffects.EARTH_ESCAPE, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            sp.teleportTo(sp.getX(), sp.getY() - 1.25D, sp.getZ());
            sp.noPhysics = true;
            sp.setNoGravity(true);
        }

        private static void castLingbeng(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            data = ModAttachments.getData(sp);
            var current = sp.getEffect(ModEffects.LINGBENG);
            int amplifier = current == null ? 0 : Math.min(9, current.getAmplifier() + 1);
            int duration = SpiritRoots.effectDuration(data, spell, 20 * (45 + data.mentalAttack()));
            if (current != null) {
                duration += current.getDuration();
            }
            sp.addEffect(new MobEffectInstance(ModEffects.LINGBENG, duration, amplifier, false, false, true));
            sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".lingbeng_active"));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castBeam(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            data = ModAttachments.getData(sp);
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                PhotonEffects.beamSpell(serverLevel, sp, SpiritRoots.effectDuration(data, spell, 20 * 10));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castRegeneration(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            sp.addEffect(new MobEffectInstance(MobEffects.HEAL, 20 * 1, 0));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castFireball(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.FireballProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.5f, 1.0f);
            sp.level().addFreshEntity(projectile);
            PhotonEffects.fireballProjectile(projectile);
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    com.example.immortal_cultivation_mod.sound.ModSounds.FIREBALL_CAST.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void handleMeditateOnServer(ServerboundMeditatePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.event.ServerEvents.handleToggleMeditate(sp);
                }
            });
        }
    }
}
