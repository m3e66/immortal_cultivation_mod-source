package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.entity.JindanCloneEntity;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModPayloads {
    public static final ResourceKey<Level> JINDAN_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "jindan"));
    private static final String JINDAN_RETURN_DIMENSION_TAG = "ImmortalCultivationJindanReturnDimension";
    private static final String JINDAN_RETURN_X_TAG = "ImmortalCultivationJindanReturnX";
    private static final String JINDAN_RETURN_Y_TAG = "ImmortalCultivationJindanReturnY";
    private static final String JINDAN_RETURN_Z_TAG = "ImmortalCultivationJindanReturnZ";
    private static final String JINDAN_RETURN_YAW_TAG = "ImmortalCultivationJindanReturnYaw";
    private static final String JINDAN_RETURN_PITCH_TAG = "ImmortalCultivationJindanReturnPitch";
    private static final String JINDAN_CLONE_UUID_TAG = "ImmortalCultivationJindanCloneUuid";
    private static final String JINDAN_ENTITY_UUID_TAG = "ImmortalCultivationJindanEntityUuid";

    public static void returnFromJindan(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ResourceKey<Level> returnDimension = Level.OVERWORLD;
        if (data.contains(JINDAN_RETURN_DIMENSION_TAG)) {
            returnDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(data.getString(JINDAN_RETURN_DIMENSION_TAG)));
        }
        ServerLevel target = player.server.getLevel(returnDimension);
        if (target == null) {
            target = player.server.overworld();
        }

        double x = data.contains(JINDAN_RETURN_X_TAG) ? data.getDouble(JINDAN_RETURN_X_TAG) : target.getSharedSpawnPos().getX() + 0.5D;
        double y = data.contains(JINDAN_RETURN_Y_TAG) ? data.getDouble(JINDAN_RETURN_Y_TAG) : target.getSharedSpawnPos().getY();
        double z = data.contains(JINDAN_RETURN_Z_TAG) ? data.getDouble(JINDAN_RETURN_Z_TAG) : target.getSharedSpawnPos().getZ() + 0.5D;
        float yaw = data.contains(JINDAN_RETURN_YAW_TAG) ? data.getFloat(JINDAN_RETURN_YAW_TAG) : player.getYRot();
        float pitch = data.contains(JINDAN_RETURN_PITCH_TAG) ? data.getFloat(JINDAN_RETURN_PITCH_TAG) : player.getXRot();
        removeNormalWorldClone(player, target, x, y, z);
        player.teleportTo(target, x, y, z, Set.of(), yaw, pitch);
    }

    private static void removeNormalWorldClone(ServerPlayer player, ServerLevel target, double x, double y, double z) {
        CompoundTag data = player.getPersistentData();
        if (data.hasUUID(JINDAN_CLONE_UUID_TAG) && target.getEntity(data.getUUID(JINDAN_CLONE_UUID_TAG)) instanceof JindanCloneEntity clone) {
            clone.discard();
            data.remove(JINDAN_CLONE_UUID_TAG);
        }

        BlockPos pos = BlockPos.containing(x, y, z);
        target.getChunk(pos);
        AABB scanBox = new AABB(pos).inflate(64.0D);
        target.getEntitiesOfClass(JindanCloneEntity.class, scanBox,
                entity -> entity.playerUuid().map(player.getUUID()::equals).orElse(false)
        ).forEach(JindanCloneEntity::discard);
    }

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
            Map<String, Integer> spellProficiencies,
            Map<String, Integer> methodProficiencies,
            boolean isMeditating,
            int skillPoints,
            int maxHpBonus,
            int maxQiBonus,
            int maxEnergyBonus,
            int physicalAttack,
            int magicAttack,
            int mentalAttack,
            boolean yuqiControlAllMode
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
                            writeSpellProficiencies(buf, data.spellProficiencies());
                            writeSpellProficiencies(buf, data.methodProficiencies());
                            ByteBufCodecs.BOOL.encode(buf, data.isMeditating());
                            ByteBufCodecs.VAR_INT.encode(buf, data.skillPoints());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxHpBonus());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxQiBonus());
                            ByteBufCodecs.VAR_INT.encode(buf, data.maxEnergyBonus());
                            ByteBufCodecs.VAR_INT.encode(buf, data.physicalAttack());
                            ByteBufCodecs.VAR_INT.encode(buf, data.magicAttack());
                            ByteBufCodecs.VAR_INT.encode(buf, data.mentalAttack());
                            ByteBufCodecs.BOOL.encode(buf, data.yuqiControlAllMode());
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
                                readSpellProficiencies(buf),
                                readSpellProficiencies(buf),
                                ByteBufCodecs.BOOL.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.BOOL.decode(buf)
                        ));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void writeSpellProficiencies(RegistryFriendlyByteBuf buf, Map<String, Integer> proficiencies) {
        Map<String, Integer> values = proficiencies == null ? Map.of() : proficiencies;
        buf.writeVarInt(values.size());
        for (var entry : values.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(Math.max(0, entry.getValue()));
        }
    }

    private static Map<String, Integer> readSpellProficiencies(RegistryFriendlyByteBuf buf) {
        int size = Math.min(4096, Math.max(0, buf.readVarInt()));
        Map<String, Integer> proficiencies = new HashMap<>();
        for (int i = 0; i < size; i++) {
            proficiencies.put(buf.readUtf(), Math.max(0, buf.readVarInt()));
        }
        return proficiencies;
    }

    public record ServerboundRequestBreakthroughPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundRequestBreakthroughPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "request_breakthrough"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRequestBreakthroughPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundRequestBreakthroughPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundEnterJindanPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundEnterJindanPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "enter_jindan"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundEnterJindanPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundEnterJindanPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundOpenJindanChallengePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundOpenJindanChallengePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "open_jindan_challenge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenJindanChallengePayload> STREAM_CODEC =
                StreamCodec.unit(new ClientboundOpenJindanChallengePayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCompleteJindanChallengePayload(float averageScale, int lessThanOne, int atOne) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCompleteJindanChallengePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "complete_jindan_challenge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCompleteJindanChallengePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeFloat(data.averageScale());
                            buf.writeVarInt(data.lessThanOne());
                            buf.writeVarInt(data.atOne());
                        },
                        buf -> new ServerboundCompleteJindanChallengePayload(buf.readFloat(), buf.readVarInt(), buf.readVarInt())
                );
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

    public record ClientboundCastAnimationPayload(UUID playerId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundCastAnimationPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_animation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCastAnimationPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeLong(data.playerId().getMostSignificantBits());
                            buf.writeLong(data.playerId().getLeastSignificantBits());
                        },
                        buf -> new ClientboundCastAnimationPayload(new UUID(buf.readLong(), buf.readLong()))
                );
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundShieldDataPayload(float amount, float max) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundShieldDataPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "shield_data"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundShieldDataPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeFloat(data.amount());
                            buf.writeFloat(data.max());
                        },
                        buf -> new ClientboundShieldDataPayload(buf.readFloat(), buf.readFloat())
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundSpellCooldownPayload(String spellId, int ticks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundSpellCooldownPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "spell_cooldown"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSpellCooldownPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ClientboundSpellCooldownPayload::spellId,
                        ByteBufCodecs.VAR_INT, ClientboundSpellCooldownPayload::ticks,
                        ClientboundSpellCooldownPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundYinYangCompassPayload(int centerX, int centerZ, int step, int size,
                                             List<Integer> values, List<Integer> qiValues) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundYinYangCompassPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "yinyang_compass"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundYinYangCompassPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeVarInt(data.centerX());
                            buf.writeVarInt(data.centerZ());
                            buf.writeVarInt(data.step());
                            buf.writeVarInt(data.size());
                            writeIntList(buf, data.values());
                            writeIntList(buf, data.qiValues());
                        },
                        buf -> {
                            int centerX = buf.readVarInt();
                            int centerZ = buf.readVarInt();
                            int step = buf.readVarInt();
                            int size = buf.readVarInt();
                            List<Integer> values = readIntList(buf);
                            List<Integer> qiValues = readIntList(buf);
                            return new ClientboundYinYangCompassPayload(centerX, centerZ, step, size, values, qiValues);
                        }
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

        private static void writeIntList(RegistryFriendlyByteBuf buf, List<Integer> values) {
            List<Integer> payloadValues = values == null ? List.of() : values;
            buf.writeVarInt(payloadValues.size());
            for (int value : payloadValues) {
                buf.writeVarInt(value);
            }
        }

        private static List<Integer> readIntList(RegistryFriendlyByteBuf buf) {
            int count = Math.min(4225, Math.max(0, buf.readVarInt()));
            List<Integer> values = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                values.add(buf.readVarInt());
            }
            return values;
        }
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

    public record ServerboundCastSpellPayload(String spellName, float chargeScale) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCastSpellPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ServerboundCastSpellPayload::spellName,
                        ByteBufCodecs.FLOAT, ServerboundCastSpellPayload::chargeScale,
                        ServerboundCastSpellPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCastComboPayload(List<String> spellNames, boolean sequential, float chargeScale) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCastComboPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_combo"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCastComboPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ServerboundCastComboPayload::spellNames,
                        ByteBufCodecs.BOOL, ServerboundCastComboPayload::sequential,
                        ByteBufCodecs.FLOAT, ServerboundCastComboPayload::chargeScale,
                        ServerboundCastComboPayload::new);
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

    public record ServerboundReleaseWeaponPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundReleaseWeaponPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "release_weapon"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundReleaseWeaponPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundReleaseWeaponPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundFlyingSwordPunchPayload(boolean shootSix) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundFlyingSwordPunchPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "flying_sword_punch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundFlyingSwordPunchPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundFlyingSwordPunchPayload::shootSix, ServerboundFlyingSwordPunchPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundFlyingSwordInteractPayload(boolean ride) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundFlyingSwordInteractPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "flying_sword_interact"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundFlyingSwordInteractPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundFlyingSwordInteractPayload::ride, ServerboundFlyingSwordInteractPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundYuqiModePayload(boolean allMode) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundYuqiModePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "yuqi_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundYuqiModePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundYuqiModePayload::allMode, ServerboundYuqiModePayload::new);
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

    public record ServerboundInjectQiPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundInjectQiPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "inject_qi"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundInjectQiPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundInjectQiPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDingActionPayload(boolean heatPulse) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDingActionPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "ding_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDingActionPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundDingActionPayload::heatPulse, ServerboundDingActionPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundFrostFlightCameraPayload(boolean firstPerson) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundFrostFlightCameraPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "frost_flight_camera"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundFrostFlightCameraPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundFrostFlightCameraPayload::firstPerson, ServerboundFrostFlightCameraPayload::new);
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
        private static final int COMBO_CAST_INTERVAL_TICKS = 6;
        private static final String METHOD_PROFICIENCY_PREFIX = "method_proficiency:";
        private static final Map<UUID, QueuedCombo> QUEUED_COMBOS = new HashMap<>();
        private static final Map<UUID, Map<String, Long>> SPELL_COOLDOWNS = new HashMap<>();
        private static final Map<UUID, Map<String, Long>> MESSAGE_COOLDOWNS = new HashMap<>();
        private static final ThreadLocal<Boolean> IGNORE_SPELL_COOLDOWN = ThreadLocal.withInitial(() -> false);
        private static final ThreadLocal<Vec3> COMBO_PROJECTILE_OFFSET = new ThreadLocal<>();
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(ClientboundSyncPlayerDataPayload.TYPE, ClientboundSyncPlayerDataPayload.STREAM_CODEC, ModPayloadsHandler::handleSyncPlayerDataOnClient);
            registrar.playToClient(ClientboundMeditationStatePayload.TYPE, ClientboundMeditationStatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditationStateOnClient);
            registrar.playToClient(ClientboundCastAnimationPayload.TYPE, ClientboundCastAnimationPayload.STREAM_CODEC, ModPayloadsHandler::handleCastAnimationOnClient);
            registrar.playToClient(ClientboundShieldDataPayload.TYPE, ClientboundShieldDataPayload.STREAM_CODEC, ModPayloadsHandler::handleShieldDataOnClient);
            registrar.playToClient(ClientboundSpellCooldownPayload.TYPE, ClientboundSpellCooldownPayload.STREAM_CODEC, ModPayloadsHandler::handleSpellCooldownOnClient);
            registrar.playToClient(ClientboundYinYangCompassPayload.TYPE, ClientboundYinYangCompassPayload.STREAM_CODEC, ModPayloadsHandler::handleYinYangCompassOnClient);
            registrar.playToClient(ClientboundOpenJindanChallengePayload.TYPE, ClientboundOpenJindanChallengePayload.STREAM_CODEC, ModPayloadsHandler::handleOpenJindanChallengeOnClient);
            registrar.playToServer(ServerboundRequestBreakthroughPayload.TYPE, ServerboundRequestBreakthroughPayload.STREAM_CODEC, ModPayloadsHandler::handleBreakthroughOnServer);
            registrar.playToServer(ServerboundEnterJindanPayload.TYPE, ServerboundEnterJindanPayload.STREAM_CODEC, ModPayloadsHandler::handleEnterJindanOnServer);
            registrar.playToServer(ServerboundCompleteJindanChallengePayload.TYPE, ServerboundCompleteJindanChallengePayload.STREAM_CODEC, ModPayloadsHandler::handleCompleteJindanChallengeOnServer);
            registrar.playToServer(ServerboundSpendSkillPointPayload.TYPE, ServerboundSpendSkillPointPayload.STREAM_CODEC, ModPayloadsHandler::handleSpendSkillPointOnServer);
            registrar.playToServer(ServerboundLearnSpellPayload.TYPE, ServerboundLearnSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleLearnSpellOnServer);
            registrar.playToServer(ServerboundActivateMethodPayload.TYPE, ServerboundActivateMethodPayload.STREAM_CODEC, ModPayloadsHandler::handleActivateMethodOnServer);
            registrar.playToServer(ServerboundCastSpellPayload.TYPE, ServerboundCastSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleCastSpellOnServer);
            registrar.playToServer(ServerboundCastComboPayload.TYPE, ServerboundCastComboPayload.STREAM_CODEC, ModPayloadsHandler::handleCastComboOnServer);
            registrar.playToServer(ServerboundLightBeamAirPunchPayload.TYPE, ServerboundLightBeamAirPunchPayload.STREAM_CODEC, ModPayloadsHandler::handleLightBeamAirPunchOnServer);
            registrar.playToServer(ServerboundReleaseWeaponPayload.TYPE, ServerboundReleaseWeaponPayload.STREAM_CODEC, ModPayloadsHandler::handleReleaseWeaponOnServer);
            registrar.playToServer(ServerboundFlyingSwordPunchPayload.TYPE, ServerboundFlyingSwordPunchPayload.STREAM_CODEC, ModPayloadsHandler::handleFlyingSwordPunchOnServer);
            registrar.playToServer(ServerboundFlyingSwordInteractPayload.TYPE, ServerboundFlyingSwordInteractPayload.STREAM_CODEC, ModPayloadsHandler::handleFlyingSwordInteractOnServer);
            registrar.playToServer(ServerboundYuqiModePayload.TYPE, ServerboundYuqiModePayload.STREAM_CODEC, ModPayloadsHandler::handleYuqiModeOnServer);
            registrar.playToServer(ServerboundWindStepJumpPayload.TYPE, ServerboundWindStepJumpPayload.STREAM_CODEC, ModPayloadsHandler::handleWindStepJumpOnServer);
            registrar.playToServer(ServerboundMeditatePayload.TYPE, ServerboundMeditatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditateOnServer);
            registrar.playToServer(ServerboundInjectQiPayload.TYPE, ServerboundInjectQiPayload.STREAM_CODEC, ModPayloadsHandler::handleInjectQiOnServer);
            registrar.playToServer(ServerboundDingActionPayload.TYPE, ServerboundDingActionPayload.STREAM_CODEC, ModPayloadsHandler::handleDingActionOnServer);
            registrar.playToServer(ServerboundFrostFlightCameraPayload.TYPE, ServerboundFrostFlightCameraPayload.STREAM_CODEC, ModPayloadsHandler::handleFrostFlightCameraOnServer);
            registrar.playToServer(ServerboundDebugAdjustStatPayload.TYPE, ServerboundDebugAdjustStatPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugAdjustStatOnServer);
            registrar.playToServer(ServerboundDebugSetSpiritRootsPayload.TYPE, ServerboundDebugSetSpiritRootsPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugSetSpiritRootsOnServer);
            registrar.playToServer(ServerboundCompleteEnlightenmentPayload.TYPE, ServerboundCompleteEnlightenmentPayload.STREAM_CODEC, ModPayloadsHandler::handleCompleteEnlightenmentOnServer);
        }

        private static void handleDingActionOnServer(ServerboundDingActionPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof com.example.immortal_cultivation_mod.screen.DingMenu menu) {
                    if (payload.heatPulse()) {
                        menu.heatPulse(sp);
                    } else {
                        menu.startForging(sp);
                    }
                }
            });
        }

        private static void handleEnterJindanOnServer(ServerboundEnterJindanPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    enterJindan(sp);
                }
            });
        }

        private static void enterJindan(ServerPlayer player) {
            ServerLevel target = player.server.getLevel(JINDAN_DIMENSION);
            if (target == null) {
                player.displayClientMessage(Component.literal("Jindan dimension is not loaded."), true);
                return;
            }
            storeJindanReturnPoint(player);
            BlockPos anchor = getOrCreateJindanAnchor(player, target);
            ensureNormalWorldClone(player);
            ensureJindanReturnEntity(player, target, anchor);
            player.teleportTo(target,
                    anchor.getX() + 0.5D,
                    anchor.getY(),
                    anchor.getZ() + 0.5D,
                    Set.of(),
                    player.getYRot(),
                    player.getXRot());
        }

        private static BlockPos getOrCreateJindanAnchor(ServerPlayer player, ServerLevel target) {
            CompoundTag data = player.getPersistentData();
            if (data.contains("ImmortalCultivationJindanAnchor")) {
                CompoundTag anchor = data.getCompound("ImmortalCultivationJindanAnchor");
                return new BlockPos(anchor.getInt("x"), anchor.getInt("y"), anchor.getInt("z"));
            }

            int radius = 12_000;
            int x = player.getRandom().nextInt(radius * 2 + 1) - radius;
            int z = player.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = Math.max(target.getMinBuildHeight() + 2, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
            BlockPos anchorPos = new BlockPos(x, y, z);
            target.getChunk(anchorPos);

            CompoundTag anchor = new CompoundTag();
            anchor.putInt("x", anchorPos.getX());
            anchor.putInt("y", anchorPos.getY());
            anchor.putInt("z", anchorPos.getZ());
            data.put("ImmortalCultivationJindanAnchor", anchor);
            return anchorPos;
        }

        private static void storeJindanReturnPoint(ServerPlayer player) {
            if (JINDAN_DIMENSION.equals(player.level().dimension())) {
                return;
            }
            CompoundTag data = player.getPersistentData();
            data.putString(JINDAN_RETURN_DIMENSION_TAG, player.level().dimension().location().toString());
            data.putDouble(JINDAN_RETURN_X_TAG, player.getX());
            data.putDouble(JINDAN_RETURN_Y_TAG, player.getY());
            data.putDouble(JINDAN_RETURN_Z_TAG, player.getZ());
            data.putFloat(JINDAN_RETURN_YAW_TAG, player.getYRot());
            data.putFloat(JINDAN_RETURN_PITCH_TAG, player.getXRot());
        }

        private static void ensureJindanReturnEntity(ServerPlayer player, ServerLevel target, BlockPos anchor) {
            CompoundTag data = player.getPersistentData();
            target.getChunk(anchor);

            if (data.hasUUID(JINDAN_ENTITY_UUID_TAG)
                    && target.getEntity(data.getUUID(JINDAN_ENTITY_UUID_TAG)) instanceof JindanEntity existing) {
                existing.setOwner(player.getUUID());
                return;
            }

            AABB scanBox = new AABB(anchor).inflate(8.0D);
            List<JindanEntity> existingNearAnchor = target.getEntitiesOfClass(JindanEntity.class, scanBox,
                    entity -> entity.ownerUuid().map(player.getUUID()::equals).orElse(false));
            if (!existingNearAnchor.isEmpty()) {
                data.putUUID(JINDAN_ENTITY_UUID_TAG, existingNearAnchor.getFirst().getUUID());
                return;
            }

            JindanEntity jindan = ModEntities.JINDAN.get().create(target);
            if (jindan != null) {
                jindan.setOwner(player.getUUID());
                jindan.moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 2.5D, 180.0F, 0.0F);
                target.addFreshEntity(jindan);
                data.putUUID(JINDAN_ENTITY_UUID_TAG, jindan.getUUID());
            }
        }

        private static void ensureNormalWorldClone(ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            ResourceKey<Level> returnDimension = Level.OVERWORLD;
            if (data.contains(JINDAN_RETURN_DIMENSION_TAG)) {
                returnDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(data.getString(JINDAN_RETURN_DIMENSION_TAG)));
            }
            ServerLevel returnLevel = player.server.getLevel(returnDimension);
            if (returnLevel == null) {
                returnLevel = player.server.overworld();
            }

            double x = data.contains(JINDAN_RETURN_X_TAG) ? data.getDouble(JINDAN_RETURN_X_TAG) : player.getX();
            double y = data.contains(JINDAN_RETURN_Y_TAG) ? data.getDouble(JINDAN_RETURN_Y_TAG) : player.getY();
            double z = data.contains(JINDAN_RETURN_Z_TAG) ? data.getDouble(JINDAN_RETURN_Z_TAG) : player.getZ();
            float yaw = data.contains(JINDAN_RETURN_YAW_TAG) ? data.getFloat(JINDAN_RETURN_YAW_TAG) : player.getYRot();
            BlockPos clonePos = BlockPos.containing(x, y, z);
            returnLevel.getChunk(clonePos);

            AABB scanBox = new AABB(clonePos).inflate(8.0D);
            List<JindanCloneEntity> existingClones = returnLevel.getEntitiesOfClass(JindanCloneEntity.class, scanBox,
                    entity -> entity.playerUuid().map(player.getUUID()::equals).orElse(false));
            if (!existingClones.isEmpty()) {
                data.putUUID(JINDAN_CLONE_UUID_TAG, existingClones.getFirst().getUUID());
            } else {
                JindanCloneEntity clone = ModEntities.JINDAN_CLONE.get().create(returnLevel);
                if (clone != null) {
                    clone.moveTo(x, y, z, yaw, 0.0F);
                    clone.copyFrom(player);
                    returnLevel.addFreshEntity(clone);
                    data.putUUID(JINDAN_CLONE_UUID_TAG, clone.getUUID());
                }
            }
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
                        case "thoughts" -> data.withThoughts(clamp(data.thoughts() + payload.delta(), 0, ModAttachments.MAX_THOUGHTS));
                        case "spirit_roots" -> data.withSpiritRoots(SpiritRoots.nextRootSet(data.spiritRoots(), payload.delta()), data.spiritRootGrade());
                        case "spirit_root_grade" -> data.withSpiritRoots(data.spiritRoots(), SpiritRoots.nextGrade(data.spiritRootGrade(), payload.delta()));
                        case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental" -> data.debugAdjustStat(payload.stat(), payload.delta());
                        default -> data;
                    };
                    if (payload.stat().startsWith(METHOD_PROFICIENCY_PREFIX)) {
                        updated = adjustMethodProficiency(data, payload.stat().substring(METHOD_PROFICIENCY_PREFIX.length()), payload.delta());
                    }
                    if (updated != data) {
                        ModAttachments.setData(sp, updated);
                        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                    }
                }
            });
        }

        private static ModAttachments.CultivationData adjustMethodProficiency(ModAttachments.CultivationData data, String methodId, int delta) {
            if (CultivationMethods.get(methodId) == null || delta == 0) {
                return data;
            }
            Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
            long value = (long) data.methodProficiency(methodId) + delta;
            if (value <= 0L) {
                updated.remove(methodId);
            } else {
                updated.put(methodId, (int) Math.min(Integer.MAX_VALUE, value));
            }
            return data.withMethodProficiencies(updated);
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
                case ModSpells.HUTI_QI -> ModItems.HUTI_QI_SCROLL.get();
                case ModSpells.MICHEN_ZHANG -> ModItems.MICHEN_ZHANG_SCROLL.get();
                case ModSpells.SLIDING_WATER -> ModItems.SLIDING_WATER_SCROLL.get();
                case ModSpells.DINGSHEN -> ModItems.DINGSHEN_SCROLL.get();
                case ModSpells.YINLEI_JUE -> ModItems.YINLEI_JUE_SCROLL.get();
                case ModSpells.WULEI_ZHENGFA -> ModItems.WULEI_ZHENGFA_SCROLL.get();
                case ModSpells.LIUGUANG_JIANYING -> ModItems.LIUGUANG_JIANYING_SCROLL.get();
                case ModSpells.SIFANG_JIE -> ModItems.SIFANG_JIE_SCROLL.get();
                case ModSpells.GUSHI_SHIELD -> ModItems.GUSHI_SHIELD_SCROLL.get();
                case ModSpells.KONGSHI_SHU -> ModItems.KONGSHI_SHU_SCROLL.get();
                case ModSpells.XUYING_TA -> ModItems.XUYING_TA_SCROLL.get();
                case ModSpells.DUANLIU_KONGDUN -> ModItems.DUANLIU_KONGDUN_SCROLL.get();
                case ModSpells.YIHEN_CI -> ModItems.YIHEN_CI_SCROLL.get();
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
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleSyncPlayerData(payload, ctx);
            }
        }

        private static void handleMeditationStateOnClient(ClientboundMeditationStatePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleMeditationState(payload, ctx);
            }
        }

        private static void handleCastAnimationOnClient(ClientboundCastAnimationPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleCastAnimation(payload, ctx);
            }
        }

        private static void handleShieldDataOnClient(ClientboundShieldDataPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleShieldData(payload, ctx);
            }
        }

        private static void handleSpellCooldownOnClient(ClientboundSpellCooldownPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleSpellCooldown(payload, ctx);
            }
        }

        private static void handleYinYangCompassOnClient(ClientboundYinYangCompassPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleYinYangCompass(payload, ctx);
            }
        }

        private static void handleOpenJindanChallengeOnClient(ClientboundOpenJindanChallengePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleOpenJindanChallenge(payload, ctx);
            }
        }

        private static void handleBreakthroughOnServer(ServerboundRequestBreakthroughPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.event.ServerEvents.tryBreakthrough(sp);
                }
            });
        }

        private static void handleCompleteJindanChallengeOnServer(ServerboundCompleteJindanChallengePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.event.ServerEvents.completeJindanBreakthrough(
                            sp,
                            payload.averageScale(),
                            payload.lessThanOne(),
                            payload.atOne()
                    );
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
                    castValidatedSpell(sp, spellId, payload.chargeScale());
                }
            });
        }

        private static void handleCastComboOnServer(ServerboundCastComboPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    java.util.Map<String, Integer> timedToggles = new java.util.HashMap<>();
                    java.util.List<String> normalSpells = new java.util.ArrayList<>();
                    for (String rawSpell : payload.spellNames()) {
                        String spellId = ModSpells.normalizeId(rawSpell);
                        if (!ModSpells.isComboEligible(spellId)) {
                            continue;
                        }
                        if (ModSpells.isComboTimedToggle(spellId)) {
                            timedToggles.merge(spellId, 1, Integer::sum);
                        } else {
                            normalSpells.add(spellId);
                        }
                    }

                    if (payload.sequential()) {
                        if (!normalSpells.isEmpty()) {
                            QUEUED_COMBOS.put(sp.getUUID(), new QueuedCombo(normalSpells, 0, sp.tickCount, payload.chargeScale()));
                        }
                    } else {
                        int projectileIndex = 0;
                        IGNORE_SPELL_COOLDOWN.set(true);
                        try {
                            for (String spellId : normalSpells) {
                                if (isProjectileSpell(spellId)) {
                                    castValidatedSpellWithProjectileOffset(sp, spellId, comboHexOffset(sp, projectileIndex++), payload.chargeScale());
                                } else {
                                    castValidatedSpell(sp, spellId, payload.chargeScale());
                                }
                            }
                        } finally {
                            IGNORE_SPELL_COOLDOWN.remove();
                        }
                    }
                    java.util.List<java.util.Map.Entry<String, Integer>> applicableToggles = new java.util.ArrayList<>();
                    for (var entry : timedToggles.entrySet()) {
                        if (canCastSpell(sp, entry.getKey())) {
                            applicableToggles.add(entry);
                        }
                    }
                    if (!applicableToggles.isEmpty()) {
                        sendCastAnimation(sp);
                    }
                    for (var entry : applicableToggles) {
                        applyTimedToggle(sp, entry.getKey(), entry.getValue() * 20 * 10);
                        addSpellProficiency(sp, entry.getKey(), entry.getValue());
                    }
                }
            });
        }

        public static void tickQueuedComboCasts(ServerPlayer player) {
            QueuedCombo combo = QUEUED_COMBOS.get(player.getUUID());
            if (combo == null || player.tickCount < combo.nextCastTick()) {
                return;
            }
            castValidatedSpell(player, combo.spellIds().get(combo.nextSpellIndex()), combo.chargeScale());
            int nextIndex = combo.nextSpellIndex() + 1;
            if (nextIndex >= combo.spellIds().size()) {
                QUEUED_COMBOS.remove(player.getUUID());
            } else {
                QUEUED_COMBOS.put(player.getUUID(), new QueuedCombo(combo.spellIds(), nextIndex,
                        player.tickCount + COMBO_CAST_INTERVAL_TICKS, combo.chargeScale()));
            }
        }

        private record QueuedCombo(List<String> spellIds, int nextSpellIndex, int nextCastTick, float chargeScale) {
            private QueuedCombo {
                spellIds = List.copyOf(spellIds);
                chargeScale = Math.max(1.0F, Math.min(2.0F, chargeScale));
            }
        }

        private static void castValidatedSpell(ServerPlayer sp, String spellId, float requestedChargeScale) {
                    float chargeScale = chargeScaleFor(sp, spellId, requestedChargeScale);
                    var spell = ModSpells.get(spellId);
                    if (spell == null) {
                        return;
                    }
                    var data = ModAttachments.getData(sp);
                    if (sp.hasEffect(ModEffects.DAZE)) {
                        return;
                    }
                    if (sp.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(spellId)) {
                        return;
                    }
                    boolean innateKnown = ModSpells.isInnateKnown(spellId, data);
                    if (!innateKnown && !data.knownSpells().stream().map(ModSpells::normalizeId).toList().contains(spellId)) {
                        sendThrottledSystemMessage(sp, "spell_not_learned",
                                Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_not_learned"));
                        return;
                    }
                    if (!hasRequiredHantiBingqinProficiency(sp, spellId, data)) {
                        return;
                    }
                    if (!ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                        sendThrottledSystemMessage(sp, "spell_requirement:" + spell.requiredLevel(),
                                Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", spell.requiredLevel()));
                        return;
                    }
                    if (!IGNORE_SPELL_COOLDOWN.get() && isSpellOnCooldown(sp, spellId)) {
                        return;
                    }
                    sendCastAnimation(sp);
                    boolean castSucceeded = true;
                    if (ModSpells.FIREBALL.equals(spellId)) {
                        castFireball(sp, data, spell, chargeScale);
                    } else if (ModSpells.LINGBENG.equals(spellId)) {
                        castLingbeng(sp, data, spell);
                    } else if (ModSpells.REGENERATION.equals(spellId)) {
                        castSucceeded = castRegeneration(sp, data, spell);
                    } else if (ModSpells.BEAM.equals(spellId)) {
                        castBeam(sp, data, spell);
                    } else if (ModSpells.EARTH_ESCAPE.equals(spellId)) {
                        castEarthEscape(sp, data);
                    } else if (ModSpells.CLEANSE.equals(spellId)) {
                        castCleanse(sp, data, spell);
                    } else if (ModSpells.QI_GATHERING.equals(spellId)) {
                        castQiGathering(sp);
                    } else if (ModSpells.IGNITE_FLARE.equals(spellId)) {
                        castIgniteFlare(sp, data, spell, chargeScale);
                    } else if (ModSpells.SPIRIT_SIGHT.equals(spellId)) {
                        com.example.immortal_cultivation_mod.event.ServerEvents.toggleSpiritSight(sp);
                    } else if (ModSpells.ZHENSHAN_PALM.equals(spellId)) {
                        castZhenshanPalm(sp, data, spell, chargeScale);
                    } else if (ModSpells.LIGHT_BEAM_ATTACK.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.LightBeamAttack.cast(sp);
                    } else if (ModSpells.DIELANG_SHIELD.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.DielangShield.cast(sp);
                    } else if (ModSpells.LINGZHI_BULLET.equals(spellId)) {
                        castLingzhiBullet(sp, data, spell, chargeScale);
                    } else if (ModSpells.WIND_BLADE.equals(spellId)) {
                        castWindBlade(sp, data, spell, chargeScale);
                    } else if (ModSpells.WIND_STEP.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.WindStep.toggle(sp);
                    } else if (ModSpells.YUFENG_JUE.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.YufengJue.toggle(sp);
                    } else if (ModSpells.SMOKE_ART.equals(spellId)) {
                        castSmokeArt(sp, data, spell, chargeScale);
                    } else if (ModSpells.HUTI_QI.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.HutiQi.cast(sp);
                    } else if (ModSpells.MICHEN_ZHANG.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.MichenZhang.cast(sp, chargeScale);
                    } else if (ModSpells.SLIDING_WATER.equals(spellId)) {
                        castSlidingWater(sp, data, spell, chargeScale);
                    } else if (ModSpells.WEIYA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.Weiya.toggle(sp);
                    } else if (ModSpells.ZIBAO.equals(spellId)) {
                        castZibao(sp, data);
                    } else if (ModSpells.ABSORB_CULTIVATION.equals(spellId)) {
                        castAbsorbCultivation(sp, data, spell, chargeScale);
                    } else if (ModSpells.TUNTIAN.equals(spellId)) {
                        castTuntian(sp, data, chargeScale);
                    } else if (ModSpells.FENGYA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.Fengya.toggle(sp);
                    } else if (ModSpells.DINGSHEN.equals(spellId)) {
                        castDingshen(sp, data, spell, chargeScale);
                    } else if (ModSpells.YINLEI_JUE.equals(spellId)) {
                        castLightning(sp, data, spell, false, chargeScale);
                    } else if (ModSpells.WULEI_ZHENGFA.equals(spellId)) {
                        castLightning(sp, data, spell, true, chargeScale);
                    } else if (ModSpells.LIUGUANG_JIANYING.equals(spellId)) {
                        castSucceeded = castLiuguangJianying(sp, data, spell, chargeScale);
                    } else if (ModSpells.SIFANG_JIE.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.SifangJie.cast(sp);
                    } else if (ModSpells.GUSHI_SHIELD.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.GushiShield.cast(sp);
                    } else if (ModSpells.KONGSHI_SHU.equals(spellId)) {
                        castKongshiShu(sp, data, spell, chargeScale);
                    } else if (ModSpells.HANJING_SUOZHUA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.HantiBingqin.castClaw(sp, chargeScale);
                    } else if (ModSpells.SHUANGTIAN_QI.equals(spellId)) {
                        castFrostCry(sp, data, chargeScale);
                    } else if (ModSpells.SUISHUANG_LINGXIAO.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.HantiBingqin.toggleFrostFlight(sp);
                    } else if (ModSpells.YUQI_SHU.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.controlLookedAt(sp);
                    } else if (ModSpells.XUYING_TA.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.XuyingTa.toggle(sp);
                    } else if (ModSpells.DUANLIU_KONGDUN.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.DuanliuKongdun.cast(sp);
                    } else if (ModSpells.YIHEN_CI.equals(spellId)) {
                        castSucceeded = castYihenCi(sp, data, spell);
                    }
                    if (castSucceeded) {
                        startSpellCooldown(sp, spellId);
                        addSpellProficiency(sp, spellId, 1);
                    }
        }

        private static void sendThrottledSystemMessage(ServerPlayer player, String key, Component message) {
            long gameTime = player.serverLevel().getGameTime();
            Map<String, Long> playerCooldowns = MESSAGE_COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
            long nextAllowed = playerCooldowns.getOrDefault(key, 0L);
            if (gameTime < nextAllowed) {
                return;
            }
            playerCooldowns.put(key, gameTime + 20L);
            player.sendSystemMessage(message);
        }

        private static void sendNotEnoughQi(ServerPlayer player) {
            sendThrottledSystemMessage(player, "not_enough_qi",
                    Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
        }

        private static void castZibao(ServerPlayer sp, ModAttachments.CultivationData data) {
            float radius = zibaoRadius(data.cultivationLevel());
            if (radius <= 0.0F) {
                return;
            }
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
            ModAttachments.setData(sp, data.withAgePenalty(levelDef.maxAge()));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            sp.level().explode(sp, sp.getX(), sp.getY(), sp.getZ(), zibaoExplosionPower(radius), Level.ExplosionInteraction.BLOCK);
            sp.hurt(sp.damageSources().genericKill(), Float.MAX_VALUE);
            if (sp.isAlive()) {
                sp.kill();
            }
        }

        private static float zibaoExplosionPower(float requestedRadius) {
            return requestedRadius * 2.0F;
        }

        private static float zibaoRadius(String cultivationLevel) {
            return switch (CultivationLevels.getLevelDef(cultivationLevel).realm()) {
                case CultivationLevels.REALM_JINDAN -> 50.0F;
                case CultivationLevels.REALM_YUANYING -> 80.0F;
                case CultivationLevels.REALM_HUASHEN -> 100.0F;
                case CultivationLevels.REALM_LIANXU -> 200.0F;
                case CultivationLevels.REALM_HETI -> 300.0F;
                case CultivationLevels.REALM_DACHENG -> 400.0F;
                case CultivationLevels.REALM_DUJIE -> 500.0F;
                default -> 0.0F;
            };
        }

        private static boolean isSpellOnCooldown(ServerPlayer player, String spellId) {
            int ticks = spellCooldownTicks(spellId);
            if (ticks <= 0) {
                return false;
            }
            Map<String, Long> cooldowns = SPELL_COOLDOWNS.get(player.getUUID());
            if (cooldowns == null) {
                return false;
            }
            long now = player.level().getGameTime();
            Long until = cooldowns.get(spellId);
            if (until == null || until <= now) {
                cooldowns.remove(spellId);
                if (cooldowns.isEmpty()) {
                    SPELL_COOLDOWNS.remove(player.getUUID());
                }
                return false;
            }
            return true;
        }

        private static void startSpellCooldown(ServerPlayer player, String spellId) {
            int ticks = spellCooldownTicks(spellId);
            if (ticks <= 0) {
                return;
            }
            SPELL_COOLDOWNS
                    .computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(spellId, player.level().getGameTime() + ticks);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new ClientboundSpellCooldownPayload(spellId, ticks));
        }

        private static int spellCooldownTicks(String spellId) {
            return ModSpells.cooldownTicks(spellId);
        }

        private static float chargeScaleFor(ServerPlayer player, String spellId, float requestedChargeScale) {
            float scale = Math.max(1.0F, Math.min(2.0F, requestedChargeScale));
            int baseCost = baseChargeCost(player, spellId, ModAttachments.getData(player));
            if (baseCost <= 0) {
                return scale;
            }
            while (scale > 1.0F && ModAttachments.getData(player).qi() < chargedCost(baseCost, scale)) {
                scale = Math.max(1.0F, scale - 0.025F);
            }
            return scale;
        }

        private static int baseChargeCost(ServerPlayer player, String spellId, ModAttachments.CultivationData data) {
            if (ModSpells.HANJING_SUOZHUA.equals(spellId)) {
                int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
                return Math.max(1, maxQi * 3 / 100);
            }
            if (ModSpells.SHUANGTIAN_QI.equals(spellId)) {
                int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
                return Math.max(1, maxQi * 5 / 100);
            }
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            return spell == null ? 0 : Math.max(0, spell.qiCost());
        }

        private static int chargedCost(int baseCost, float chargeScale) {
            float clamped = Math.max(1.0F, Math.min(2.0F, chargeScale));
            return Math.max(1, Math.round(baseCost * (1.0F + 4.0F * (clamped - 1.0F))));
        }

        private static float chargedAreaScale(float chargeScale) {
            return Math.max(1.0F, Math.min(2.0F, chargeScale));
        }

        private static void addSpellProficiency(ServerPlayer player, String spellId, int amount) {
            var data = ModAttachments.getData(player);
            ModAttachments.setData(player, data.withAddedSpellProficiency(spellId, amount));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
        }

        private static void castValidatedSpellWithProjectileOffset(ServerPlayer sp, String spellId, Vec3 offset, float chargeScale) {
            COMBO_PROJECTILE_OFFSET.set(offset);
            try {
                castValidatedSpell(sp, spellId, chargeScale);
            } finally {
                COMBO_PROJECTILE_OFFSET.remove();
            }
        }

        private static boolean isProjectileSpell(String spellId) {
            return ModSpells.FIREBALL.equals(spellId)
                    || ModSpells.IGNITE_FLARE.equals(spellId)
                    || ModSpells.ZHENSHAN_PALM.equals(spellId)
                    || ModSpells.LINGZHI_BULLET.equals(spellId)
                    || ModSpells.WIND_BLADE.equals(spellId)
                    || ModSpells.SMOKE_ART.equals(spellId)
                    || ModSpells.SLIDING_WATER.equals(spellId)
                    || ModSpells.ABSORB_CULTIVATION.equals(spellId)
                    || ModSpells.DINGSHEN.equals(spellId)
                    || ModSpells.YINLEI_JUE.equals(spellId)
                    || ModSpells.WULEI_ZHENGFA.equals(spellId)
                    || ModSpells.LIUGUANG_JIANYING.equals(spellId)
                    || ModSpells.SHUANGTIAN_QI.equals(spellId)
                    || ModSpells.DUANLIU_KONGDUN.equals(spellId)
                    || ModSpells.YIHEN_CI.equals(spellId);
        }

        private static Vec3 comboHexOffset(ServerPlayer player, int index) {
            int ring = 0;
            int ringStart = 0;
            while (index >= ringStart + Math.max(1, ring * 6)) {
                ringStart += Math.max(1, ring * 6);
                ring++;
            }

            int axialQ = 0;
            int axialR = 0;
            if (ring > 0) {
                int step = index - ringStart;
                int side = step / ring;
                int sideStep = step % ring;
                int[][] directions = {{-1, 1}, {-1, 0}, {0, -1}, {1, -1}, {1, 0}, {0, 1}};
                axialQ = ring;
                axialR = 0;
                for (int i = 0; i < side; i++) {
                    axialQ += directions[i][0] * ring;
                    axialR += directions[i][1] * ring;
                }
                axialQ += directions[side][0] * sideStep;
                axialR += directions[side][1] * sideStep;
            }

            double spacing = 0.18D;
            double sideways = Math.sqrt(3.0D) * spacing * (axialQ + axialR * 0.5D);
            double vertical = 1.5D * spacing * axialR;
            Vec3 forward = player.getLookAngle().normalize();
            Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            if (right.lengthSqr() < 0.001D) {
                right = new Vec3(1.0D, 0.0D, 0.0D);
            }
            return forward.scale(0.55D).add(right.scale(sideways)).add(0.0D, vertical, 0.0D);
        }

        private static void applyComboProjectileOffset(net.minecraft.world.entity.Entity projectile) {
            Vec3 offset = COMBO_PROJECTILE_OFFSET.get();
            if (offset != null) {
                projectile.setPos(projectile.position().add(offset));
            }
        }

        private static void sendCastAnimation(ServerPlayer sp) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new ClientboundCastAnimationPayload(sp.getUUID()));
        }

        private static boolean canCastSpell(ServerPlayer sp, String spellId) {
            var spell = ModSpells.get(spellId);
            if (spell == null) {
                return false;
            }
            var data = ModAttachments.getData(sp);
            if (sp.hasEffect(ModEffects.DAZE)) {
                return false;
            }
            if (sp.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(spellId)) {
                return false;
            }
            boolean innateKnown = ModSpells.isInnateKnown(spellId, data);
            if (!innateKnown && !data.knownSpells().stream().map(ModSpells::normalizeId).toList().contains(spellId)) {
                sendThrottledSystemMessage(sp, "spell_not_learned",
                        Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_not_learned"));
                return false;
            }
            if (!hasRequiredHantiBingqinProficiency(sp, spellId, data)) {
                return false;
            }
            if (!ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                sendThrottledSystemMessage(sp, "spell_requirement:" + spell.requiredLevel(),
                        Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", spell.requiredLevel()));
                return false;
            }
            return true;
        }

        private static boolean hasRequiredHantiBingqinProficiency(ServerPlayer sp, String spellId, ModAttachments.CultivationData data) {
            int required = ModSpells.requiredHantiBingqinProficiency(spellId);
            if (required < 0) {
                return true;
            }
            if (ModSpells.isHantiBingqinSpellUnlocked(spellId, data)) {
                return true;
            }
            return false;
        }

        private static void applyTimedToggle(ServerPlayer sp, String spellId, int durationTicks) {
            int stackedDuration = stackedEffectDuration(sp, effectForTimedToggle(spellId), durationTicks);
            if (ModSpells.EARTH_ESCAPE.equals(spellId)) {
                sp.addEffect(new MobEffectInstance(ModEffects.EARTH_ESCAPE, stackedDuration, 0, false, false, true));
            } else if (ModSpells.QI_GATHERING.equals(spellId)) {
                sp.addEffect(new MobEffectInstance(ModEffects.QI_GATHERING, stackedDuration, 0, false, false, true));
                com.example.immortal_cultivation_mod.event.ServerEvents.startQiGathering(sp);
            } else if (ModSpells.SPIRIT_SIGHT.equals(spellId)) {
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, stackedDuration, 0, false, false, true));
                sp.addEffect(new MobEffectInstance(ModEffects.SPIRIT_SIGHT, stackedDuration, 0, false, false, true));
            } else if (ModSpells.WIND_STEP.equals(spellId)) {
                com.example.immortal_cultivation_mod.spell.WindStep.activateTimed(sp, durationTicks);
                return;
            } else if (ModSpells.YUFENG_JUE.equals(spellId)) {
                com.example.immortal_cultivation_mod.spell.YufengJue.activateTimed(sp, durationTicks);
                return;
            } else if (ModSpells.WEIYA.equals(spellId)) {
                com.example.immortal_cultivation_mod.spell.Weiya.activateTimed(sp, durationTicks);
                return;
            } else if (ModSpells.FENGYA.equals(spellId)) {
                sp.addEffect(new MobEffectInstance(ModEffects.FENGYA, stackedEffectDuration(sp, ModEffects.FENGYA, durationTicks), 0, false, false, true));
            } else if (ModSpells.SUISHUANG_LINGXIAO.equals(spellId)) {
                sp.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT, stackedEffectDuration(sp, ModEffects.FROST_FLIGHT, durationTicks), 0, false, false, true));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static Holder<net.minecraft.world.effect.MobEffect> effectForTimedToggle(String spellId) {
            if (ModSpells.EARTH_ESCAPE.equals(spellId)) {
                return ModEffects.EARTH_ESCAPE;
            }
            if (ModSpells.QI_GATHERING.equals(spellId)) {
                return ModEffects.QI_GATHERING;
            }
            if (ModSpells.SPIRIT_SIGHT.equals(spellId)) {
                return ModEffects.SPIRIT_SIGHT;
            }
            return null;
        }

        private static int stackedEffectDuration(ServerPlayer sp, Holder<net.minecraft.world.effect.MobEffect> effect, int durationTicks) {
            if (effect == null) {
                return durationTicks;
            }
            var current = sp.getEffect(effect);
            if (current == null || current.getDuration() <= 0) {
                return durationTicks;
            }
            return current.getDuration() + durationTicks;
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

        private static void handleReleaseWeaponOnServer(ServerboundReleaseWeaponPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.releaseFromHand(sp);
                }
            });
        }

        private static void handleFlyingSwordPunchOnServer(ServerboundFlyingSwordPunchPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.shootControlled(sp, payload.shootSix());
                }
            });
        }

        private static void handleFlyingSwordInteractOnServer(ServerboundFlyingSwordInteractPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.interactLookingAt(sp, payload.ride());
                }
            });
        }

        private static void handleYuqiModeOnServer(ServerboundYuqiModePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.setControlAllMode(sp, payload.allMode());
                    var data = ModAttachments.getData(sp);
                    ModAttachments.setData(sp, data.withYuqiControlAllMode(payload.allMode()));
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
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

        private static void castLingzhiBullet(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.LingzhiBulletProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 2.1f, 0.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castWindBlade(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.9f, 0.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castSmokeArt(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.SmokeProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.4f, 0.2f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castSlidingWater(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.SlidingWaterProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.6f, 0.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castAbsorbCultivation(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isReincarnationTrueArt(data.activeCultivationMethod())) {
                return;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.AbsorbCultivationProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.8f, 0.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castTuntian(ServerPlayer sp, ModAttachments.CultivationData data, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isTuntianDemonArt(data.activeCultivationMethod())) {
                return;
            }
            if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                return;
            }

            final int radius = Math.max(1, Math.round(5.0F * chargedAreaScale(chargeScale)));
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

        private static void castDingshen(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.DingshenProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.9f, 0.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castKongshiShu(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.KongshiProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.8F, 0.1F);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castLightning(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, boolean greater, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.LightningProjectileEntity(sp.level(), sp, greater);
            projectile.setChargeScale(chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, greater ? 3.2F : 2.4F, greater ? 0.2F : 0.4F);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.example.immortal_cultivation_mod.entity.SpellImpactParticles.lightning(serverLevel, sp.getEyePosition().add(sp.getLookAngle().scale(0.9D)));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static boolean castLiuguangJianying(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            com.example.immortal_cultivation_mod.spell.LiuguangJianying.cast(sp, chargeScale);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return true;
        }

        private static boolean castYihenCi(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.adjustThoughts(sp, -10);
            var projectile = new com.example.immortal_cultivation_mod.entity.YihenCiProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.4F, 0.4F);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            return true;
        }

        private static void castZhenshanPalm(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var palm = new com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity(sp.level(), sp, chargedAreaScale(chargeScale));
            applyComboProjectileOffset(palm);
            sp.level().addFreshEntity(palm);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castCleanse(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
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

        private static void castIgniteFlare(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.8f, 0.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castEarthEscape(ServerPlayer sp, ModAttachments.CultivationData data) {
            if (sp.hasEffect(ModEffects.EARTH_ESCAPE)) {
                return;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, 8)) {
                sendNotEnoughQi(sp);
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
                sendNotEnoughQi(sp);
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
                sendNotEnoughQi(sp);
                return;
            }
            data = ModAttachments.getData(sp);
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                PhotonEffects.beamSpell(serverLevel, sp, SpiritRoots.effectDuration(data, spell, 20 * 10));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static boolean castRegeneration(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            sp.addEffect(new MobEffectInstance(MobEffects.HEAL, 20 * 1, 0));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return true;
        }

        private static void castFireball(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.FireballProjectileEntity(sp.level(), sp);
            projectile.setChargeScale(chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.5f, 1.0f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            PhotonEffects.fireballProjectile(projectile);
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    com.example.immortal_cultivation_mod.sound.ModSounds.FIREBALL_CAST.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void castFrostCry(ServerPlayer sp, ModAttachments.CultivationData data, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isHantiBingqin(data.activeCultivationMethod())) {
                return;
            }
            int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
            int cost = Math.max(1, maxQi * 5 / 100);
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, chargedCost(cost, chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.FrostCryProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.75f, 0.1f);
            applyComboProjectileOffset(projectile);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static void handleMeditateOnServer(ServerboundMeditatePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.event.ServerEvents.handleToggleMeditate(sp);
                }
            });
        }

        private static void handleInjectQiOnServer(ServerboundInjectQiPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    if (!com.example.immortal_cultivation_mod.item.CorpseControlToolCycler.cycle(sp)) {
                        com.example.immortal_cultivation_mod.item.QiInfusedWeapon.inject(sp);
                    }
                }
            });
        }

        private static void handleFrostFlightCameraOnServer(ServerboundFrostFlightCameraPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    PhotonEffects.updateFrostFlightSelfView(sp, payload.firstPerson());
                }
            });
        }
    }
}

