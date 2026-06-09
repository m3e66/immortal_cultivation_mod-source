package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
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
            int cultivationProgress,
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
                            ByteBufCodecs.VAR_INT.encode(buf, data.cultivationProgress());
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

    public record ServerboundCastSpellPayload(String spellName) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCastSpellPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerboundCastSpellPayload::spellName, ServerboundCastSpellPayload::new);
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
            registrar.playToServer(ServerboundCastSpellPayload.TYPE, ServerboundCastSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleCastSpellOnServer);
            registrar.playToServer(ServerboundMeditatePayload.TYPE, ServerboundMeditatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditateOnServer);
            registrar.playToServer(ServerboundDebugAdjustStatPayload.TYPE, ServerboundDebugAdjustStatPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugAdjustStatOnServer);
        }

        private static void handleDebugAdjustStatOnServer(ServerboundDebugAdjustStatPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var data = ModAttachments.getData(sp);
                    var updated = switch (payload.stat()) {
                        case "cultivation_level" -> data.withCultivationLevel(nextCultivationLevel(data.cultivationLevel(), payload.delta()));
                        case "body_type" -> data.withBodyType(nextBodyType(data.bodyType(), payload.delta()));
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

        private static String nextBodyType(String current, int direction) {
            List<String> values = List.of(
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_MORTAL,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_LIANQI,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_ZHUJI,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_JINDAN,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_YUANYING
            );
            int index = values.indexOf(current);
            if (index < 0) {
                index = 0;
            }
            int next = Math.floorMod(index + (direction < 0 ? -1 : 1), values.size());
            return values.get(next);
        }

        private static String nextCultivationLevel(String current, int direction) {
            List<String> values = List.of(
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_MORTAL,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_LIANQI + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_LIANQI + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_MID,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_LIANQI + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_LATE,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_ZHUJI + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_ZHUJI + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_MID,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_ZHUJI + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_LATE,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_JINDAN + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_JINDAN + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_MID,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_JINDAN + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_LATE,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_YUANYING + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_YUANYING + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_MID,
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_YUANYING + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_LATE
            );
            int index = values.indexOf(current);
            if (index < 0) {
                index = 0;
            }
            int next = Math.floorMod(index + (direction < 0 ? -1 : 1), values.size());
            return values.get(next);
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
                    if (sp.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(spellId)) {
                        return;
                    }
                    if (!data.knownSpells().stream().map(ModSpells::normalizeId).toList().contains(spellId)) {
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
                    }
                }
            });
        }

        private static void castCleanse(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (data.qi() < spell.qiCost()) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            ModAttachments.setData(sp, data.withQi(data.qi() - spell.qiCost()));
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
            if (data.qi() < spell.qiCost()) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            ModAttachments.setData(sp, data.withQi(data.qi() - spell.qiCost()));
            var projectile = new com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.8f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castEarthEscape(ServerPlayer sp, ModAttachments.CultivationData data) {
            if (sp.hasEffect(ModEffects.EARTH_ESCAPE)) {
                return;
            }
            if (data.qi() < 5) {
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
            if (data.qi() < spell.qiCost()) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            ModAttachments.setData(sp, data.withQi(data.qi() - spell.qiCost()));
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
            if (data.qi() < spell.qiCost()) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            ModAttachments.setData(sp, data.withQi(data.qi() - spell.qiCost()));
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                PhotonEffects.beamSpell(serverLevel, sp, SpiritRoots.effectDuration(data, spell, 20 * 10));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castRegeneration(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (data.qi() < spell.qiCost()) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            ModAttachments.setData(sp, data.withQi(data.qi() - spell.qiCost()));
            sp.addEffect(new MobEffectInstance(MobEffects.HEAL, 20 * 1, 0));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castFireball(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (data.qi() < spell.qiCost()) {
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                return;
            }
            ModAttachments.setData(sp, data.withQi(data.qi() - spell.qiCost()));
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
