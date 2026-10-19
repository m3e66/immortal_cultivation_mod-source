package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.achievement.NoobAchievement;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.NativeSpellEffects;
import com.example.immortal_cultivation_mod.entity.CosmicVoidPortalEntity;
import com.example.immortal_cultivation_mod.entity.JindanCloneEntity;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.MysteriousRingItem;
import com.example.immortal_cultivation_mod.item.RestrictionInsightItem;
import com.example.immortal_cultivation_mod.item.TalismanRecipes;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.ShieldDisplay;
import com.example.immortal_cultivation_mod.system.OuterSystemManager;
import com.example.immortal_cultivation_mod.system.OuterSystemType;
import com.example.immortal_cultivation_mod.world.JindanPocketWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModPayloads {
    public static final ResourceKey<Level> JINDAN_DIMENSION = JindanPocketWorlds.TEMPLATE_DIMENSION;
    private static final String JINDAN_RETURN_DIMENSION_TAG = "ImmortalCultivationJindanReturnDimension";
    private static final String JINDAN_RETURN_X_TAG = "ImmortalCultivationJindanReturnX";
    private static final String JINDAN_RETURN_Y_TAG = "ImmortalCultivationJindanReturnY";
    private static final String JINDAN_RETURN_Z_TAG = "ImmortalCultivationJindanReturnZ";
    private static final String JINDAN_RETURN_YAW_TAG = "ImmortalCultivationJindanReturnYaw";
    private static final String JINDAN_RETURN_PITCH_TAG = "ImmortalCultivationJindanReturnPitch";
    private static final String JINDAN_CLONE_UUID_TAG = "ImmortalCultivationJindanCloneUuid";
    private static final String JINDAN_ENTITY_UUID_TAG = "ImmortalCultivationJindanEntityUuid";
    private static final String JINDAN_ANCHOR_TAG = "ImmortalCultivationJindanAnchor";
    private static final String JINDAN_ANCHOR_VERSION_TAG = "ImmortalCultivationJindanAnchorVersion";
    private static final String JINDAN_ANCHOR_GENERATION_TAG = "ImmortalCultivationJindanAnchorGeneration";
    private static final String JINDAN_LEVEL_TRACKED_TAG = "ImmortalCultivationJindanLevelTracked";
    private static final String JINDAN_WAS_LEVEL_TAG = "ImmortalCultivationWasJindanLevel";
    private static final String JINDAN_RESET_PENDING_TAG = "ImmortalCultivationJindanResetPending";
    private static final String JINDAN_RESET_ENTITY_UUID_TAG = "ImmortalCultivationJindanResetEntityUuid";
    private static final String JINDAN_RESET_ANCHOR_TAG = "ImmortalCultivationJindanResetAnchor";
    private static final String JINDAN_RESET_DIMENSION_TAG = "ImmortalCultivationJindanResetDimension";
    private static final int JINDAN_ANCHOR_VERSION = 2;
    private static final int JINDAN_ANCHOR_RADIUS = 100_000;
    private static final long JINDAN_ANCHOR_RANDOM_SALT = 0x4A494E44414E5EEDL;
    private static final long JINDAN_REPLICA_RANDOM_SALT = 0x5245504C4943415EL;
    private static final String DEBUG_EYE_ENABLED_TAG = "ImmortalCultivationDebugEyeEnabled";

    public static boolean isDebugEyeEnabled(ServerPlayer player) {
        return player.hasPermissions(2)
                && player.getPersistentData().getBoolean(DEBUG_EYE_ENABLED_TAG);
    }

    public static void syncDebugEyeState(ServerPlayer player) {
        boolean enabled = isDebugEyeEnabled(player);
        PacketDistributor.sendToPlayer(player, new ClientboundDebugEyeStatePayload(enabled));
        com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics.sync(player);
    }

    public static void syncJindanFlightControlState(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayer(player, new ClientboundJindanFlightControlPayload(active));
    }

    public static void syncJindanFlightSpeed(
            ServerPlayer player, float selectedSpeed, int changedLayers) {
        PacketDistributor.sendToPlayer(player,
                new ClientboundJindanFlightSpeedPayload(selectedSpeed, changedLayers));
    }

    public static void syncJindanFlightDive(
            ServerPlayer player, boolean impact, float intensity) {
        PacketDistributor.sendToPlayer(player,
                new ClientboundJindanFlightDivePayload(impact, intensity));
    }

    public static void openDebugStatEditor(ServerPlayer viewer, ServerPlayer target) {
        openDebugStatEditor(viewer, target, 0);
    }

    public static void openDebugStatEditor(ServerPlayer viewer, ServerPlayer target, int page) {
        PacketDistributor.sendToPlayer(viewer, new ClientboundOpenDebugStatEditorPayload(
                target.getUUID(), target.getGameProfile().getName(),
                OuterSystemManager.debugSystemType(target).id(), Math.clamp(page, 0, 8)));
        com.example.immortal_cultivation_mod.event.ServerEvents
                .syncPlayerDataTo(target, viewer);
        PacketDistributor.sendToPlayer(viewer, new ClientboundDebugHintCountPayload(
                RestrictionInsightItem.hintCount(target)));
    }

    public static ServerPlayer resolveDebugTarget(ServerPlayer editor, UUID targetId) {
        boolean holdingEditor = editor.getMainHandItem().is(ModItems.DEBUG_STAT_EDITOR.get())
                || editor.getOffhandItem().is(ModItems.DEBUG_STAT_EDITOR.get());
        if (!holdingEditor || editor.getServer() == null) return null;
        return editor.getServer().getPlayerList().getPlayer(targetId);
    }

    public static void beginNpcLearning(ServerPlayer player, int npcEntityId, String learningType,
                                        String targetId, String teacherLevel) {
        ModPayloadsHandler.beginLearningSession(
                player, "npc", learningType, targetId, npcEntityId, teacherLevel);
    }

    public static boolean beginRestrictionInsightLearning(ServerPlayer player) {
        return ModPayloadsHandler.beginRestrictionInsightLearning(player);
    }

    public static boolean beginDunwuSteleLearning(ServerPlayer player, String spellId) {
        return ModPayloadsHandler.beginDunwuSteleLearning(player, spellId);
    }

    public static void openDunwuSteleEditor(ServerPlayer player, BlockPos pos, String spellId) {
        PacketDistributor.sendToPlayer(player, new ClientboundOpenSteleSpellEditorPayload(
                pos, spellId == null ? "" : spellId));
    }

    public static void clearPendingLearning(ServerPlayer player) {
        ModPayloadsHandler.PENDING_LEARNING.remove(player.getUUID());
    }

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
        removeNormalWorldClone(player, target, x, y, z);
        ServerLevel finalTarget = target;
        player.server.execute(() -> removeNormalWorldClone(player, finalTarget, x, y, z));
    }

    public static void cleanupJindanClones(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ResourceKey<Level> returnDimension = Level.OVERWORLD;
        if (data.contains(JINDAN_RETURN_DIMENSION_TAG)) {
            returnDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(data.getString(JINDAN_RETURN_DIMENSION_TAG)));
        }
        ServerLevel target = player.server.getLevel(returnDimension);
        if (target == null) {
            target = player.server.overworld();
        }
        double x = data.contains(JINDAN_RETURN_X_TAG)
                ? data.getDouble(JINDAN_RETURN_X_TAG)
                : target.getSharedSpawnPos().getX() + 0.5D;
        double y = data.contains(JINDAN_RETURN_Y_TAG)
                ? data.getDouble(JINDAN_RETURN_Y_TAG)
                : target.getSharedSpawnPos().getY();
        double z = data.contains(JINDAN_RETURN_Z_TAG)
                ? data.getDouble(JINDAN_RETURN_Z_TAG)
                : target.getSharedSpawnPos().getZ() + 0.5D;
        removeNormalWorldClone(player, target, x, y, z);
    }

    public static void trackJindanRealmState(ServerPlayer player, String cultivationLevel) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(JINDAN_RESET_PENDING_TAG)
                && !JindanPocketWorlds.isJindanDimension(player.level().dimension())) {
            removeOwnedJindanEntities(player);
        }
        int currentStage = CultivationLevels.getStageIndex(cultivationLevel);
        int jindanStage = CultivationLevels.getStageIndex(
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        boolean isJindanLevel = currentStage >= jindanStage;
        boolean tracked = data.getBoolean(JINDAN_LEVEL_TRACKED_TAG);
        boolean wasJindanLevel = data.getBoolean(JINDAN_WAS_LEVEL_TAG);
        boolean legacyLoweredPlayer = !tracked && !isJindanLevel && data.contains(JINDAN_ANCHOR_TAG);

        if ((tracked && wasJindanLevel && !isJindanLevel) || legacyLoweredPlayer) {
            if (JindanPocketWorlds.isJindanDimension(player.level().dimension())) {
                data.putBoolean(JINDAN_RESET_PENDING_TAG, true);
                data.putString(JINDAN_RESET_DIMENSION_TAG, player.level().dimension().location().toString());
                if (data.hasUUID(JINDAN_ENTITY_UUID_TAG)) {
                    data.putUUID(JINDAN_RESET_ENTITY_UUID_TAG, data.getUUID(JINDAN_ENTITY_UUID_TAG));
                }
                if (data.contains(JINDAN_ANCHOR_TAG)) {
                    data.put(JINDAN_RESET_ANCHOR_TAG, data.getCompound(JINDAN_ANCHOR_TAG).copy());
                }
            } else {
                removeOwnedJindanEntities(player);
            }
            int nextGeneration = data.getInt(JINDAN_ANCHOR_GENERATION_TAG) + 1;
            data.putInt(JINDAN_ANCHOR_GENERATION_TAG, nextGeneration);
            JindanPocketWorlds.setActiveGeneration(player, nextGeneration);
            data.remove(JINDAN_ANCHOR_TAG);
            data.remove(JINDAN_ENTITY_UUID_TAG);
            JindanEntity.resetGeneratedChunkCount(player);
            data.putInt(JINDAN_ANCHOR_VERSION_TAG, JINDAN_ANCHOR_VERSION);
        }
        data.putBoolean(JINDAN_LEVEL_TRACKED_TAG, true);
        data.putBoolean(JINDAN_WAS_LEVEL_TAG, isJindanLevel);
    }

    public static void finishPendingJindanReset(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(JINDAN_RESET_PENDING_TAG)) {
            removeOwnedJindanEntities(player);
        }
    }

    private static void removeOwnedJindanEntities(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ResourceKey<Level> expectedDimension = data.contains(JINDAN_RESET_DIMENSION_TAG)
                ? ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(data.getString(JINDAN_RESET_DIMENSION_TAG)))
                : JindanPocketWorlds.keyFor(player);
        ServerLevel expectedLevel = player.server.getLevel(expectedDimension);

        CompoundTag anchor = data.contains(JINDAN_ANCHOR_TAG)
                ? data.getCompound(JINDAN_ANCHOR_TAG)
                : data.getCompound(JINDAN_RESET_ANCHOR_TAG);
        if (expectedLevel != null && !anchor.isEmpty()) {
            expectedLevel.getChunk(new BlockPos(
                    anchor.getInt("x"),
                    anchor.getInt("y"),
                    anchor.getInt("z")));
        }

        UUID trackedEntity = data.hasUUID(JINDAN_ENTITY_UUID_TAG)
                ? data.getUUID(JINDAN_ENTITY_UUID_TAG)
                : data.hasUUID(JINDAN_RESET_ENTITY_UUID_TAG)
                ? data.getUUID(JINDAN_RESET_ENTITY_UUID_TAG)
                : null;
        List<JindanEntity> owned = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            if (!JindanPocketWorlds.isJindanDimension(level.dimension())) {
                continue;
            }
            for (var entity : level.getAllEntities()) {
                if (entity instanceof JindanEntity jindan
                        && ((trackedEntity != null && jindan.getUUID().equals(trackedEntity))
                        || jindan.ownerUuid().map(player.getUUID()::equals).orElse(false))) {
                    owned.add(jindan);
                }
            }
        }
        owned.forEach(JindanEntity::discard);
        data.remove(JINDAN_ENTITY_UUID_TAG);
        data.remove(JINDAN_RESET_ENTITY_UUID_TAG);
        data.remove(JINDAN_RESET_ANCHOR_TAG);
        data.remove(JINDAN_RESET_DIMENSION_TAG);
        data.remove(JINDAN_RESET_PENDING_TAG);
    }

    private static void removeNormalWorldClone(ServerPlayer player, ServerLevel target, double x, double y, double z) {
        CompoundTag data = player.getPersistentData();
        BlockPos pos = BlockPos.containing(x, y, z);
        target.getChunk(pos);
        UUID trackedClone = data.hasUUID(JINDAN_CLONE_UUID_TAG)
                ? data.getUUID(JINDAN_CLONE_UUID_TAG)
                : null;
        List<JindanCloneEntity> clonesToRemove = new ArrayList<>();

        for (ServerLevel level : player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof JindanCloneEntity clone
                        && ((trackedClone != null && clone.getUUID().equals(trackedClone))
                        || clone.playerUuid().map(player.getUUID()::equals).orElse(false)
                        || clone.playerUuid().isEmpty())) {
                    clonesToRemove.add(clone);
                }
            }
        }

        AABB scanBox = new AABB(pos).inflate(128.0D);
        for (JindanCloneEntity clone : target.getEntitiesOfClass(
                JindanCloneEntity.class,
                scanBox,
                entity -> entity.playerUuid().map(player.getUUID()::equals).orElse(true))) {
            if (!clonesToRemove.contains(clone)) {
                clonesToRemove.add(clone);
            }
        }
        clonesToRemove.forEach(JindanCloneEntity::discard);
        data.remove(JINDAN_CLONE_UUID_TAG);
    }

    public record ClientboundSyncPlayerDataPayload(
            int qi,
            int maxQi,
            int maxHp,
            int maxAge,
            String cultivationLevel,
            String jindanQuality,
            int luck,
            int moral,
            String bodyType,
            int soul,
            int thoughts,
            float talent,
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
            boolean yuqiControlAllMode,
            String playerIdentity,
            boolean livingCorpse
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
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.jindanQuality());
                            ByteBufCodecs.VAR_INT.encode(buf, data.luck());
                            ByteBufCodecs.VAR_INT.encode(buf, data.moral());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.bodyType());
                            ByteBufCodecs.VAR_INT.encode(buf, data.soul());
                            ByteBufCodecs.VAR_INT.encode(buf, data.thoughts());
                            ByteBufCodecs.FLOAT.encode(buf, data.talent());
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
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.playerIdentity());
                            ByteBufCodecs.BOOL.encode(buf, data.livingCorpse());
                        },
                        buf -> new ClientboundSyncPlayerDataPayload(
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.FLOAT.decode(buf),
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
                                ByteBufCodecs.BOOL.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
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

    public record ServerboundOpenCosmicVoidPortalPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundOpenCosmicVoidPortalPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "open_cosmic_void_portal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenCosmicVoidPortalPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundOpenCosmicVoidPortalPayload());
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundOpenJindanChallengePayload(boolean assisted) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundOpenJindanChallengePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "open_jindan_challenge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenJindanChallengePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ClientboundOpenJindanChallengePayload::assisted,
                        ClientboundOpenJindanChallengePayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundJindanTribulationPayload(int event, int durationTicks, float intensity)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundJindanTribulationPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "jindan_tribulation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundJindanTribulationPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeVarInt(data.event());
                            buf.writeVarInt(data.durationTicks());
                            buf.writeFloat(data.intensity());
                        },
                        buf -> new ClientboundJindanTribulationPayload(
                                buf.readVarInt(), buf.readVarInt(), buf.readFloat()));

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

    public record ClientboundCastAnimationPayload(UUID playerId, String spellId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundCastAnimationPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_animation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCastAnimationPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeLong(data.playerId().getMostSignificantBits());
                            buf.writeLong(data.playerId().getLeastSignificantBits());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.spellId());
                        },
                        buf -> new ClientboundCastAnimationPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.STRING_UTF8.decode(buf))
                );
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundShieldDataPayload(float amount, float max,List<ShieldDisplay.Layer> layers) implements CustomPacketPayload {
        public ClientboundShieldDataPayload(float amount,float max){this(amount,max,List.of());}
        public ClientboundShieldDataPayload {
            layers=List.copyOf(layers);
            if(layers.size()>ShieldDisplay.Kind.values().length)throw new IllegalArgumentException("Too many shield layers");
            var kinds=java.util.EnumSet.noneOf(ShieldDisplay.Kind.class);
            for(var layer:layers)if(!kinds.add(layer.kind()))throw new IllegalArgumentException("Duplicate shield layer");
            if(!layers.isEmpty()){amount=ShieldDisplay.total(layers,false);max=ShieldDisplay.total(layers,true);}
            if(!Float.isFinite(amount)||!Float.isFinite(max)||amount<0||max<amount)throw new IllegalArgumentException("Invalid shield totals");
        }
        public static final CustomPacketPayload.Type<ClientboundShieldDataPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "shield_data"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundShieldDataPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeFloat(data.amount());
                            buf.writeFloat(data.max());
                            buf.writeVarInt(data.layers().size());
                            for(var layer:data.layers()){buf.writeByte(layer.kind().ordinal());buf.writeFloat(layer.amount());buf.writeFloat(layer.max());}
                        },
                        buf -> {
                            float amount=buf.readFloat(),max=buf.readFloat();int count=buf.readVarInt();var kinds=ShieldDisplay.Kind.values();
                            if(count<0||count>kinds.length)throw new IllegalArgumentException("Too many shield layers");
                            var layers=new ArrayList<ShieldDisplay.Layer>(count);
                            for(int i=0;i<count;i++){
                                int kind=buf.readUnsignedByte();if(kind>=kinds.length)throw new IllegalArgumentException("Unknown shield layer");
                                layers.add(new ShieldDisplay.Layer(kinds[kind],buf.readFloat(),buf.readFloat()));
                            }
                            return new ClientboundShieldDataPayload(amount,max,layers);
                        }
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

    public record ClientboundInterruptSpellPreparationPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundInterruptSpellPreparationPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "interrupt_spell_preparation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundInterruptSpellPreparationPayload>
                STREAM_CODEC = StreamCodec.unit(new ClientboundInterruptSpellPreparationPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundRingDialoguePayload(
            Component message, boolean elder, int durationTicks)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundRingDialoguePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "mysterious_ring_dialogue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRingDialoguePayload>
                STREAM_CODEC = StreamCodec.of(
                        (buf, data) -> {
                            ComponentSerialization.STREAM_CODEC.encode(buf, data.message());
                            ByteBufCodecs.BOOL.encode(buf, data.elder());
                            ByteBufCodecs.VAR_INT.encode(buf, data.durationTicks());
                        },
                        buf -> new ClientboundRingDialoguePayload(
                                ComponentSerialization.STREAM_CODEC.decode(buf),
                                ByteBufCodecs.BOOL.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
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

    public record ClientboundNpcProfilePayload(CompoundTag data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundNpcProfilePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "npc_profile"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundNpcProfilePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeNbt(payload.data()),
                        buf -> {
                            CompoundTag data = buf.readNbt();
                            return new ClientboundNpcProfilePayload(data == null ? new CompoundTag() : data);
                        });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundNpcActionPayload(int entityId, String action, int index) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundNpcActionPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "npc_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundNpcActionPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ServerboundNpcActionPayload::entityId,
                        ByteBufCodecs.STRING_UTF8, ServerboundNpcActionPayload::action,
                        ByteBufCodecs.VAR_INT, ServerboundNpcActionPayload::index,
                        ServerboundNpcActionPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundDebugEyeStatePayload(boolean enabled) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundDebugEyeStatePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "debug_eye_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugEyeStatePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ClientboundDebugEyeStatePayload::enabled,
                        ClientboundDebugEyeStatePayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundJindanFlightControlPayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundJindanFlightControlPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "jindan_flight_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundJindanFlightControlPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ClientboundJindanFlightControlPayload::active,
                        ClientboundJindanFlightControlPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundJindanFlightSpeedPayload(float selectedSpeed, int changedLayers)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundJindanFlightSpeedPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "jindan_flight_speed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundJindanFlightSpeedPayload>
                STREAM_CODEC = StreamCodec.of(
                        (buf, data) -> {
                            buf.writeFloat(data.selectedSpeed());
                            buf.writeVarInt(data.changedLayers());
                        },
                        buf -> new ClientboundJindanFlightSpeedPayload(
                                buf.readFloat(), buf.readVarInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundJindanFlightDivePayload(boolean impact, float intensity)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundJindanFlightDivePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "jindan_flight_dive"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundJindanFlightDivePayload>
                STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.BOOL, ClientboundJindanFlightDivePayload::impact,
                        ByteBufCodecs.FLOAT, ClientboundJindanFlightDivePayload::intensity,
                        ClientboundJindanFlightDivePayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundDebugHintCountPayload(int hints) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundDebugHintCountPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "debug_hint_count"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugHintCountPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ClientboundDebugHintCountPayload::hints,
                        ClientboundDebugHintCountPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientboundOpenDebugStatEditorPayload(
            UUID targetId, String targetName, String systemType, int page)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundOpenDebugStatEditorPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "open_debug_stat_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenDebugStatEditorPayload>
                STREAM_CODEC = StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeLong(payload.targetId().getMostSignificantBits());
                            buf.writeLong(payload.targetId().getLeastSignificantBits());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.targetName());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.systemType());
                            buf.writeVarInt(payload.page());
                        },
                        buf -> new ClientboundOpenDebugStatEditorPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf), buf.readVarInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundOpenWantedOrderPayload(String skin, String name, String realm,
                                                     String rewardKey, int rewardCount) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundOpenWantedOrderPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "open_wanted_order"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenWantedOrderPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.skin());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.name());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.realm());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.rewardKey());
                            ByteBufCodecs.VAR_INT.encode(buf, payload.rewardCount());
                        },
                        buf -> new ClientboundOpenWantedOrderPayload(
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundProtectivePendantActivationPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundProtectivePendantActivationPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "protective_pendant_activation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundProtectivePendantActivationPayload> STREAM_CODEC =
                StreamCodec.unit(new ClientboundProtectivePendantActivationPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundToggleDebugEyePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundToggleDebugEyePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "toggle_debug_eye"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleDebugEyePayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundToggleDebugEyePayload());

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

    public record ClientboundOpenSteleSpellEditorPayload(
            BlockPos pos,
            String spellId
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundOpenSteleSpellEditorPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "open_stele_spell_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenSteleSpellEditorPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeBlockPos(data.pos());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.spellId());
                        },
                        buf -> new ClientboundOpenSteleSpellEditorPayload(
                                buf.readBlockPos(),
                                ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetSteleSpellPayload(
            BlockPos pos,
            String spellId
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSetSteleSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_stele_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetSteleSpellPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeBlockPos(data.pos());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.spellId());
                        },
                        buf -> new ServerboundSetSteleSpellPayload(
                                buf.readBlockPos(),
                                ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundOpenLearningGamePayload(
            String learningType,
            String targetId,
            int requiredPoints,
            float difficulty,
            float talent,
            boolean assisted,
            String playerLevel,
            long token
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientboundOpenLearningGamePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "open_learning_game"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenLearningGamePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.learningType());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.targetId());
                            ByteBufCodecs.VAR_INT.encode(buf, data.requiredPoints());
                            ByteBufCodecs.FLOAT.encode(buf, data.difficulty());
                            ByteBufCodecs.FLOAT.encode(buf, data.talent());
                            ByteBufCodecs.BOOL.encode(buf, data.assisted());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.playerLevel());
                            buf.writeLong(data.token());
                        },
                        buf -> new ClientboundOpenLearningGamePayload(
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf),
                                ByteBufCodecs.FLOAT.decode(buf),
                                ByteBufCodecs.FLOAT.decode(buf),
                                ByteBufCodecs.BOOL.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                buf.readLong()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCompleteLearningGamePayload(long token, int score) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCompleteLearningGamePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "complete_learning_game"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCompleteLearningGamePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_LONG, ServerboundCompleteLearningGamePayload::token,
                        ByteBufCodecs.VAR_INT, ServerboundCompleteLearningGamePayload::score,
                        ServerboundCompleteLearningGamePayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCancelLearningGamePayload(long token) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCancelLearningGamePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "cancel_learning_game"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCancelLearningGamePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_LONG, ServerboundCancelLearningGamePayload::token,
                        ServerboundCancelLearningGamePayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCastSpellPayload(
            String spellName,
            float chargeScale,
            boolean selfCast) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cast_spell"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCastSpellPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ServerboundCastSpellPayload::spellName,
                        ByteBufCodecs.FLOAT, ServerboundCastSpellPayload::chargeScale,
                        ByteBufCodecs.BOOL, ServerboundCastSpellPayload::selfCast,
                        ServerboundCastSpellPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundSpellPreparationPayload(
            String spellName,
            boolean preparing) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSpellPreparationPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "spell_preparation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSpellPreparationPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        ServerboundSpellPreparationPayload::spellName,
                        ByteBufCodecs.BOOL,
                        ServerboundSpellPreparationPayload::preparing,
                        ServerboundSpellPreparationPayload::new);

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

    public record ServerboundShihunfanSlashPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundShihunfanSlashPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "shihunfan_slash"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundShihunfanSlashPayload> STREAM_CODEC =
                StreamCodec.unit(new ServerboundShihunfanSlashPayload());
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

    public record ServerboundSwordCaseActionPayload(boolean releaseAll) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSwordCaseActionPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "sword_case_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSwordCaseActionPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundSwordCaseActionPayload::releaseAll,
                        ServerboundSwordCaseActionPayload::new);
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

    public record ServerboundSetAutoChannelQiPayload(boolean enabled) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSetAutoChannelQiPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_auto_channel_qi"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetAutoChannelQiPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ServerboundSetAutoChannelQiPayload::enabled,
                        ServerboundSetAutoChannelQiPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundFlyingSwordAutoRecallPayload(boolean enabled) implements CustomPacketPayload {
        public static final Type<ServerboundFlyingSwordAutoRecallPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"flying_sword_auto_recall"));
        public static final StreamCodec<RegistryFriendlyByteBuf,ServerboundFlyingSwordAutoRecallPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,ServerboundFlyingSwordAutoRecallPayload::enabled,ServerboundFlyingSwordAutoRecallPayload::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record ServerboundSetQiUseParticlesPayload(boolean enabled)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSetQiUseParticlesPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_qi_use_particles"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetQiUseParticlesPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ServerboundSetQiUseParticlesPayload::enabled,
                        ServerboundSetQiUseParticlesPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetDroughtDomainPayload(boolean enabled)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSetDroughtDomainPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_drought_domain"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetDroughtDomainPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ServerboundSetDroughtDomainPayload::enabled,
                        ServerboundSetDroughtDomainPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetFormationPlatePaymentOrderPayload(boolean qiFirst)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSetFormationPlatePaymentOrderPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_formation_plate_payment_order"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetFormationPlatePaymentOrderPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ServerboundSetFormationPlatePaymentOrderPayload::qiFirst,
                        ServerboundSetFormationPlatePaymentOrderPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundSetJindanFlightPayload(boolean enabled) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundSetJindanFlightPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "set_jindan_flight"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetJindanFlightPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ServerboundSetJindanFlightPayload::enabled,
                        ServerboundSetJindanFlightPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundAdjustJindanFlightSpeedPayload(int steps) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundAdjustJindanFlightSpeedPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "adjust_jindan_flight_speed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundAdjustJindanFlightSpeedPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ServerboundAdjustJindanFlightSpeedPayload::steps,
                        ServerboundAdjustJindanFlightSpeedPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundDingActionPayload(boolean heatPulse) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDingActionPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "ding_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDingActionPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ServerboundDingActionPayload::heatPulse, ServerboundDingActionPayload::new);
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDebugAdjustStatPayload(UUID targetId, String stat, int delta)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDebugAdjustStatPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "debug_adjust_stat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugAdjustStatPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeLong(payload.targetId().getMostSignificantBits());
                            buf.writeLong(payload.targetId().getLeastSignificantBits());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.stat());
                            ByteBufCodecs.VAR_INT.encode(buf, payload.delta());
                        },
                        buf -> new ServerboundDebugAdjustStatPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.VAR_INT.decode(buf)));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDebugSetSpiritRootsPayload(
            UUID targetId, List<String> roots, String grade) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDebugSetSpiritRootsPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "debug_set_spirit_roots"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugSetSpiritRootsPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            buf.writeLong(data.targetId().getMostSignificantBits());
                            buf.writeLong(data.targetId().getLeastSignificantBits());
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, data.roots());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.grade());
                        },
                        buf -> new ServerboundDebugSetSpiritRootsPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf)
                        ));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDebugSetJindanQualityPayload(UUID targetId, String quality)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDebugSetJindanQualityPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "debug_set_jindan_quality"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugSetJindanQualityPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeLong(payload.targetId().getMostSignificantBits());
                            buf.writeLong(payload.targetId().getLeastSignificantBits());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.quality());
                        },
                        buf -> new ServerboundDebugSetJindanQualityPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundDebugSetOuterSystemPayload(UUID targetId, String systemType)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundDebugSetOuterSystemPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "debug_set_outer_system"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugSetOuterSystemPayload>
                STREAM_CODEC = StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeLong(payload.targetId().getMostSignificantBits());
                            buf.writeLong(payload.targetId().getLeastSignificantBits());
                            ByteBufCodecs.STRING_UTF8.encode(buf, payload.systemType());
                        },
                        buf -> new ServerboundDebugSetOuterSystemPayload(
                                new UUID(buf.readLong(), buf.readLong()),
                                ByteBufCodecs.STRING_UTF8.decode(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ServerboundCloseDebugStatEditorPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCloseDebugStatEditorPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "close_debug_stat_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCloseDebugStatEditorPayload>
                STREAM_CODEC = StreamCodec.unit(new ServerboundCloseDebugStatEditorPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundCompleteEnlightenmentPayload(List<String> roots, String grade, String bodyType,
                                                           String gender) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ServerboundCompleteEnlightenmentPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "complete_enlightenment"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCompleteEnlightenmentPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, data) -> {
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, data.roots());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.grade());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.bodyType());
                            ByteBufCodecs.STRING_UTF8.encode(buf, data.gender());
                        },
                        buf -> new ServerboundCompleteEnlightenmentPayload(
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf)
                        ));
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModPayloadsHandler {
        private static final String METHOD_PROFICIENCY_PREFIX = "method_proficiency:";
        private static final String CRAFT_PROFICIENCY_PREFIX = "craft_proficiency:";
        private static final String TALISMAN_MASTERY_PREFIX = "talisman_mastery:";
        private static final Set<String> CRAFT_PROFICIENCY_IDS = Set.of(
                "forging",
                "alchemy",
                "talisman",
                "beast_taming",
                "puppet",
                "corpse_control",
                "gu",
                "formation",
                "divination",
                "ghost_control");
        private static final Map<UUID, Map<String, Long>> SPELL_COOLDOWNS = new HashMap<>();
        private static final Map<UUID, Map<String, Long>> MESSAGE_COOLDOWNS = new HashMap<>();
        private static final Map<UUID, PendingLearning> PENDING_LEARNING = new HashMap<>();
        private static final int BASE_LEARNING_POINTS = 5_000;
        private static final long LEARNING_SESSION_TICKS = 20L * 60L * 15L;
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("3");
            registrar.playToClient(ClientboundSyncPlayerDataPayload.TYPE, ClientboundSyncPlayerDataPayload.STREAM_CODEC, ModPayloadsHandler::handleSyncPlayerDataOnClient);
            registrar.playToClient(ClientboundMeditationStatePayload.TYPE, ClientboundMeditationStatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditationStateOnClient);
            registrar.playToClient(ClientboundCastAnimationPayload.TYPE, ClientboundCastAnimationPayload.STREAM_CODEC, ModPayloadsHandler::handleCastAnimationOnClient);
            registrar.playToClient(ClientboundShieldDataPayload.TYPE, ClientboundShieldDataPayload.STREAM_CODEC, ModPayloadsHandler::handleShieldDataOnClient);
            registrar.playToClient(ClientboundSpellCooldownPayload.TYPE, ClientboundSpellCooldownPayload.STREAM_CODEC, ModPayloadsHandler::handleSpellCooldownOnClient);
            registrar.playToClient(ClientboundInterruptSpellPreparationPayload.TYPE,
                    ClientboundInterruptSpellPreparationPayload.STREAM_CODEC,
                    (payload, context) -> com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                            .handleInterruptSpellPreparation(payload, context));
            registrar.playToClient(ClientboundRingDialoguePayload.TYPE,
                    ClientboundRingDialoguePayload.STREAM_CODEC,
                    ModPayloadsHandler::handleRingDialogueOnClient);
            registrar.playToClient(ClientboundYinYangCompassPayload.TYPE, ClientboundYinYangCompassPayload.STREAM_CODEC, ModPayloadsHandler::handleYinYangCompassOnClient);
            registrar.playToClient(ClientboundOpenJindanChallengePayload.TYPE, ClientboundOpenJindanChallengePayload.STREAM_CODEC, ModPayloadsHandler::handleOpenJindanChallengeOnClient);
            registrar.playToClient(ClientboundJindanTribulationPayload.TYPE, ClientboundJindanTribulationPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleJindanTribulationOnClient);
            registrar.playToClient(ClientboundNpcProfilePayload.TYPE, ClientboundNpcProfilePayload.STREAM_CODEC, ModPayloadsHandler::handleNpcProfileOnClient);
            registrar.playToClient(ClientboundOpenLearningGamePayload.TYPE, ClientboundOpenLearningGamePayload.STREAM_CODEC,
                    ModPayloadsHandler::handleOpenLearningGameOnClient);
            registrar.playToClient(ClientboundOpenSteleSpellEditorPayload.TYPE,
                    ClientboundOpenSteleSpellEditorPayload.STREAM_CODEC,
                    (payload, context) -> com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                            .handleOpenSteleSpellEditor(payload, context));
            registrar.playToClient(ClientboundDebugEyeStatePayload.TYPE, ClientboundDebugEyeStatePayload.STREAM_CODEC, ModPayloadsHandler::handleDebugEyeStateOnClient);
            registrar.playToClient(ClientboundJindanFlightControlPayload.TYPE,
                    ClientboundJindanFlightControlPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleJindanFlightControlOnClient);
            registrar.playToClient(ClientboundJindanFlightSpeedPayload.TYPE,
                    ClientboundJindanFlightSpeedPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleJindanFlightSpeedOnClient);
            registrar.playToClient(ClientboundJindanFlightDivePayload.TYPE,
                    ClientboundJindanFlightDivePayload.STREAM_CODEC,
                    ModPayloadsHandler::handleJindanFlightDiveOnClient);
            registrar.playToClient(ClientboundDebugHintCountPayload.TYPE, ClientboundDebugHintCountPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleDebugHintCountOnClient);
            registrar.playToClient(ClientboundOpenDebugStatEditorPayload.TYPE,
                    ClientboundOpenDebugStatEditorPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleOpenDebugStatEditorOnClient);
            registrar.playToClient(ClientboundOpenWantedOrderPayload.TYPE, ClientboundOpenWantedOrderPayload.STREAM_CODEC,
                    (payload, context) -> com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleWantedOrder(payload, context));
            registrar.playToClient(ClientboundProtectivePendantActivationPayload.TYPE,
                    ClientboundProtectivePendantActivationPayload.STREAM_CODEC,
                    (payload, context) -> com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                            .handleProtectivePendantActivation(payload, context));
            registrar.playToServer(ServerboundRequestBreakthroughPayload.TYPE, ServerboundRequestBreakthroughPayload.STREAM_CODEC, ModPayloadsHandler::handleBreakthroughOnServer);
            registrar.playToServer(ServerboundEnterJindanPayload.TYPE, ServerboundEnterJindanPayload.STREAM_CODEC, ModPayloadsHandler::handleEnterJindanOnServer);
            registrar.playToServer(ServerboundOpenCosmicVoidPortalPayload.TYPE,
                    ServerboundOpenCosmicVoidPortalPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleOpenCosmicVoidPortalOnServer);
            registrar.playToServer(ServerboundCompleteJindanChallengePayload.TYPE, ServerboundCompleteJindanChallengePayload.STREAM_CODEC, ModPayloadsHandler::handleCompleteJindanChallengeOnServer);
            registrar.playToServer(ServerboundSpendSkillPointPayload.TYPE, ServerboundSpendSkillPointPayload.STREAM_CODEC, ModPayloadsHandler::handleSpendSkillPointOnServer);
            registrar.playToServer(ServerboundLearnSpellPayload.TYPE, ServerboundLearnSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleLearnSpellOnServer);
            registrar.playToServer(ServerboundActivateMethodPayload.TYPE, ServerboundActivateMethodPayload.STREAM_CODEC, ModPayloadsHandler::handleActivateMethodOnServer);
            registrar.playToServer(ServerboundSetSteleSpellPayload.TYPE, ServerboundSetSteleSpellPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSetSteleSpellOnServer);
            registrar.playToServer(ServerboundCompleteLearningGamePayload.TYPE, ServerboundCompleteLearningGamePayload.STREAM_CODEC,
                    ModPayloadsHandler::handleCompleteLearningGameOnServer);
            registrar.playToServer(ServerboundCancelLearningGamePayload.TYPE, ServerboundCancelLearningGamePayload.STREAM_CODEC,
                    ModPayloadsHandler::handleCancelLearningGameOnServer);
            registrar.playToServer(ServerboundCastSpellPayload.TYPE, ServerboundCastSpellPayload.STREAM_CODEC, ModPayloadsHandler::handleCastSpellOnServer);
            registrar.playToServer(ServerboundSpellPreparationPayload.TYPE,
                    ServerboundSpellPreparationPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSpellPreparationOnServer);
            registrar.playToServer(ServerboundLightBeamAirPunchPayload.TYPE, ServerboundLightBeamAirPunchPayload.STREAM_CODEC, ModPayloadsHandler::handleLightBeamAirPunchOnServer);
            registrar.playToServer(ServerboundShihunfanSlashPayload.TYPE, ServerboundShihunfanSlashPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleShihunfanSlashOnServer);
            registrar.playToServer(ServerboundReleaseWeaponPayload.TYPE, ServerboundReleaseWeaponPayload.STREAM_CODEC, ModPayloadsHandler::handleReleaseWeaponOnServer);
            registrar.playToServer(ServerboundSwordCaseActionPayload.TYPE, ServerboundSwordCaseActionPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSwordCaseActionOnServer);
            registrar.playToServer(ServerboundFlyingSwordPunchPayload.TYPE, ServerboundFlyingSwordPunchPayload.STREAM_CODEC, ModPayloadsHandler::handleFlyingSwordPunchOnServer);
            registrar.playToServer(ServerboundFlyingSwordInteractPayload.TYPE, ServerboundFlyingSwordInteractPayload.STREAM_CODEC, ModPayloadsHandler::handleFlyingSwordInteractOnServer);
            registrar.playToServer(ServerboundYuqiModePayload.TYPE, ServerboundYuqiModePayload.STREAM_CODEC, ModPayloadsHandler::handleYuqiModeOnServer);
            registrar.playToServer(ServerboundWindStepJumpPayload.TYPE, ServerboundWindStepJumpPayload.STREAM_CODEC, ModPayloadsHandler::handleWindStepJumpOnServer);
            registrar.playToServer(ServerboundMeditatePayload.TYPE, ServerboundMeditatePayload.STREAM_CODEC, ModPayloadsHandler::handleMeditateOnServer);
            registrar.playToServer(ServerboundInjectQiPayload.TYPE, ServerboundInjectQiPayload.STREAM_CODEC, ModPayloadsHandler::handleInjectQiOnServer);
            registrar.playToServer(ServerboundSetAutoChannelQiPayload.TYPE, ServerboundSetAutoChannelQiPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSetAutoChannelQiOnServer);
            registrar.playToServer(ServerboundSetQiUseParticlesPayload.TYPE,
                    ServerboundSetQiUseParticlesPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSetQiUseParticlesOnServer);
            registrar.playToServer(ServerboundFlyingSwordAutoRecallPayload.TYPE,ServerboundFlyingSwordAutoRecallPayload.STREAM_CODEC,
                    (payload,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer p)com.example.immortal_cultivation_mod.entity.FlyingSwordRecovery.setAutoRecall(p,payload.enabled());}));
            registrar.playToServer(ServerboundSetDroughtDomainPayload.TYPE,
                    ServerboundSetDroughtDomainPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSetDroughtDomainOnServer);
            registrar.playToServer(ServerboundSetFormationPlatePaymentOrderPayload.TYPE,
                    ServerboundSetFormationPlatePaymentOrderPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSetFormationPlatePaymentOrderOnServer);
            registrar.playToServer(ServerboundSetJindanFlightPayload.TYPE, ServerboundSetJindanFlightPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleSetJindanFlightOnServer);
            registrar.playToServer(ServerboundAdjustJindanFlightSpeedPayload.TYPE,
                    ServerboundAdjustJindanFlightSpeedPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleAdjustJindanFlightSpeedOnServer);
            registrar.playToServer(ServerboundDingActionPayload.TYPE, ServerboundDingActionPayload.STREAM_CODEC, ModPayloadsHandler::handleDingActionOnServer);
            registrar.playToServer(ServerboundDebugAdjustStatPayload.TYPE, ServerboundDebugAdjustStatPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugAdjustStatOnServer);
            registrar.playToServer(ServerboundDebugSetSpiritRootsPayload.TYPE, ServerboundDebugSetSpiritRootsPayload.STREAM_CODEC, ModPayloadsHandler::handleDebugSetSpiritRootsOnServer);
            registrar.playToServer(ServerboundDebugSetJindanQualityPayload.TYPE,
                    ServerboundDebugSetJindanQualityPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleDebugSetJindanQualityOnServer);
            registrar.playToServer(ServerboundDebugSetOuterSystemPayload.TYPE,
                    ServerboundDebugSetOuterSystemPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleDebugSetOuterSystemOnServer);
            registrar.playToServer(ServerboundCloseDebugStatEditorPayload.TYPE,
                    ServerboundCloseDebugStatEditorPayload.STREAM_CODEC,
                    ModPayloadsHandler::handleCloseDebugStatEditorOnServer);
            registrar.playToServer(ServerboundCompleteEnlightenmentPayload.TYPE, ServerboundCompleteEnlightenmentPayload.STREAM_CODEC, ModPayloadsHandler::handleCompleteEnlightenmentOnServer);
            registrar.playToServer(ServerboundNpcActionPayload.TYPE, ServerboundNpcActionPayload.STREAM_CODEC, ModPayloadsHandler::handleNpcActionOnServer);
            registrar.playToServer(ServerboundToggleDebugEyePayload.TYPE, ServerboundToggleDebugEyePayload.STREAM_CODEC, ModPayloadsHandler::handleToggleDebugEyeOnServer);
        }

        private static void handleToggleDebugEyeOnServer(ServerboundToggleDebugEyePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (!player.hasPermissions(2)) {
                    player.getPersistentData().putBoolean(DEBUG_EYE_ENABLED_TAG, false);
                    syncDebugEyeState(player);
                    player.displayClientMessage(Component.translatable(
                            "message." + ImmortalCultivationMod.MODID + ".debug_eye_no_permission"), true);
                    return;
                }
                boolean enabled = !player.getPersistentData().getBoolean(DEBUG_EYE_ENABLED_TAG);
                player.getPersistentData().putBoolean(DEBUG_EYE_ENABLED_TAG, enabled);
                syncDebugEyeState(player);
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID
                                + (enabled ? ".debug_eye_enabled" : ".debug_eye_disabled")), true);
            });
        }

        private static void handleNpcActionOnServer(ServerboundNpcActionPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player)) {
                    return;
                }
                var entity = player.level().getEntity(payload.entityId());
                if (entity instanceof net.minecraft.world.entity.PathfinderMob mob
                        && entity instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc) {
                    com.example.immortal_cultivation_mod.entity.NpcProfile.handleAction(
                            mob, player, payload.action(), payload.index());
                }
            });
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

        private static void handleOpenCosmicVoidPortalOnServer(
                ServerboundOpenCosmicVoidPortalPayload payload,
                IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    CosmicVoidPortalEntity.openFor(sp);
                }
            });
        }

        private static void enterJindan(ServerPlayer player) {
            finishPendingJindanReset(player);
            storeJindanReturnPoint(player);
            ServerLevel target = JindanPocketWorlds.getOrCreate(player);
            boolean hadLegacyAnchor = player.getPersistentData().contains(JINDAN_ANCHOR_TAG);
            BlockPos anchor = getOrCreateJindanAnchor(player, target);
            JindanPocketWorlds.migrateLegacyPocket(player, target, anchor, hadLegacyAnchor);
            ensureNormalWorldClone(player);
            ensureJindanReturnEntity(player, target, anchor);
            player.teleportTo(target,
                    anchor.getX() + 0.5D,
                    anchor.getY(),
                    anchor.getZ() + 0.5D,
                    Set.of(),
                    player.getYRot(),
                    player.getXRot());
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.ENTER_INNER_SPACE);
        }

        private static BlockPos getOrCreateJindanAnchor(ServerPlayer player, ServerLevel target) {
            CompoundTag data = player.getPersistentData();
            if (data.getInt(JINDAN_ANCHOR_VERSION_TAG) == JINDAN_ANCHOR_VERSION
                    && data.contains(JINDAN_ANCHOR_TAG)) {
                CompoundTag saved = data.getCompound(JINDAN_ANCHOR_TAG);
                BlockPos savedPos = new BlockPos(saved.getInt("x"), saved.getInt("y"), saved.getInt("z"));
                target.getChunk(savedPos);
                return savedPos;
            }
            if (data.contains(JINDAN_ANCHOR_TAG) || data.hasUUID(JINDAN_ENTITY_UUID_TAG)) {
                removeOwnedJindanEntities(player);
            }

            UUID playerId = player.getUUID();
            long generation = data.getInt(JINDAN_ANCHOR_GENERATION_TAG);
            long playerSeed = JINDAN_ANCHOR_RANDOM_SALT
                    ^ playerId.getMostSignificantBits()
                    ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 29)
                    ^ generation * 0x9E3779B97F4A7C15L;
            RandomSource random = RandomSource.create(playerSeed);
            int x = random.nextInt(JINDAN_ANCHOR_RADIUS * 2 + 1) - JINDAN_ANCHOR_RADIUS;
            int z = random.nextInt(JINDAN_ANCHOR_RADIUS * 2 + 1) - JINDAN_ANCHOR_RADIUS;
            int y = Math.max(target.getMinBuildHeight() + 2, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
            BlockPos anchorPos = new BlockPos(x, y, z);
            target.getChunk(anchorPos);

            CompoundTag anchor = new CompoundTag();
            anchor.putInt("x", anchorPos.getX());
            anchor.putInt("y", anchorPos.getY());
            anchor.putInt("z", anchorPos.getZ());
            data.put(JINDAN_ANCHOR_TAG, anchor);
            data.putInt(JINDAN_ANCHOR_VERSION_TAG, JINDAN_ANCHOR_VERSION);
            return anchorPos;
        }

        private static void storeJindanReturnPoint(ServerPlayer player) {
            if (JindanPocketWorlds.isJindanDimension(player.level().dimension())) {
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
            long replicaSourceSeed = jindanReplicaSourceSeed(player);

            List<JindanEntity> loadedJindanEntities = new ArrayList<>();
            List<JindanEntity> unownedLegacyEntities = new ArrayList<>();
            for (var entity : target.getAllEntities()) {
                if (entity instanceof JindanEntity jindan) {
                    if (jindan.ownerUuid().map(player.getUUID()::equals).orElse(false)) {
                        loadedJindanEntities.add(jindan);
                    } else if (jindan.ownerUuid().isEmpty()) {
                        unownedLegacyEntities.add(jindan);
                    }
                }
            }
            unownedLegacyEntities.forEach(JindanEntity::discard);

            JindanEntity singleton = null;
            if (data.hasUUID(JINDAN_ENTITY_UUID_TAG)
                    && target.getEntity(data.getUUID(JINDAN_ENTITY_UUID_TAG)) instanceof JindanEntity existing
                    && existing.ownerUuid().map(player.getUUID()::equals).orElse(false)) {
                singleton = existing;
            }

            if (singleton == null && !loadedJindanEntities.isEmpty()) {
                double anchorX = anchor.getX() + 0.5D;
                double anchorY = anchor.getY();
                double anchorZ = anchor.getZ() + 0.5D;
                double closestDistance = Double.MAX_VALUE;
                for (JindanEntity candidate : loadedJindanEntities) {
                    double distance = candidate.distanceToSqr(anchorX, anchorY, anchorZ);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        singleton = candidate;
                    }
                }
            }

            if (singleton != null) {
                for (JindanEntity candidate : loadedJindanEntities) {
                    if (candidate != singleton) {
                        candidate.discard();
                    }
                }

                double anchorX = anchor.getX() + 0.5D;
                double anchorY = anchor.getY();
                double anchorZ = anchor.getZ() + 0.5D;
                singleton.moveTo(anchorX, anchorY, anchorZ, 180.0F, 0.0F);
                singleton.setOwner(player.getUUID());
                singleton.setReplicaSourceSeed(replicaSourceSeed);
                singleton.syncGeneratedChunkCount(player);
                singleton.setDeltaMovement(Vec3.ZERO);
                data.putUUID(JINDAN_ENTITY_UUID_TAG, singleton.getUUID());
                return;
            }

            JindanEntity jindan = ModEntities.JINDAN.get().create(target);
            if (jindan != null) {
                jindan.moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 180.0F, 0.0F);
                jindan.setOwner(player.getUUID());
                jindan.setReplicaSourceSeed(replicaSourceSeed);
                jindan.syncGeneratedChunkCount(player);
                target.addFreshEntity(jindan);
                data.putUUID(JINDAN_ENTITY_UUID_TAG, jindan.getUUID());
            }
        }

        private static long jindanReplicaSourceSeed(ServerPlayer player) {
            UUID playerId = player.getUUID();
            long generation = player.getPersistentData().getInt(JINDAN_ANCHOR_GENERATION_TAG);
            long seed = JINDAN_REPLICA_RANDOM_SALT
                    ^ Long.rotateLeft(playerId.getMostSignificantBits(), 17)
                    ^ Long.rotateRight(playerId.getLeastSignificantBits(), 11)
                    ^ generation * 0xD1B54A32D192ED03L;
            return seed == 0L ? JINDAN_REPLICA_RANDOM_SALT : seed;
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

            removeNormalWorldClone(player, returnLevel, x, y, z);
            JindanCloneEntity clone = ModEntities.JINDAN_CLONE.get().create(returnLevel);
            if (clone != null) {
                clone.moveTo(x, y, z, yaw, 0.0F);
                clone.copyFrom(player);
                returnLevel.addFreshEntity(clone);
                data.putUUID(JINDAN_CLONE_UUID_TAG, clone.getUUID());
            }
        }

        private static void handleActivateMethodOnServer(ServerboundActivateMethodPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    var def = CultivationMethods.get(payload.methodId());
                    if (def == null) {
                        return;
                    }
                    var current = ModAttachments.getData(sp);
                    boolean learned = payload.methodId().equals(current.activeCultivationMethod())
                            || current.methodProficiencies().containsKey(payload.methodId());
                    if (learned) {
                        activateMethod(sp, payload.methodId());
                    } else {
                        beginLearningSession(sp, "method_item", "method", payload.methodId(), -1,
                                def.limitLevel());
                    }
                }
            });
        }

        private static void handleDebugAdjustStatOnServer(ServerboundDebugAdjustStatPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer editor) {
                    ServerPlayer target = resolveDebugTarget(editor, payload.targetId());
                    if (target == null) {
                        return;
                    }
                    if ("restriction_hints".equals(payload.stat())) {
                        int hints = RestrictionInsightItem.adjustHintCount(target, payload.delta());
                        syncDebugHintCount(target, editor, hints);
                        return;
                    }
                    if("age".equals(payload.stat())) {
                        com.example.immortal_cultivation_mod.event.ServerEvents.adjustDebugRemainingAge(target,payload.delta());
                        syncDebugTarget(target,editor);
                        return;
                    }
                    var data = ModAttachments.getData(target);
                    var updated = switch (payload.stat()) {
                        case "cultivation_level" -> data.withCultivationLevel(nextCultivationLevel(data.cultivationLevel(), payload.delta()));
                        case "body_type" -> data.withBodyType(BodyTypes.next(data.bodyType(), payload.delta()));
                        case "moral" -> data.withMoral(clamp(data.moral() + payload.delta(), 0, 100));
                        case "luck" -> data.withLuck(clamp(data.luck() + payload.delta(), 0, 100));
                        case "soul" -> data.withSoul(clamp(data.soul() + payload.delta(), 0, 1000));
                        case "thoughts" -> data.withThoughts(clamp(data.thoughts() + payload.delta(), 0, data.maxThoughts()));
                        case "spirit_roots" -> data.withSpiritRoots(SpiritRoots.nextRootSet(data.spiritRoots(), payload.delta()), data.spiritRootGrade());
                        case "spirit_root_grade" -> data.withSpiritRoots(data.spiritRoots(), SpiritRoots.nextGrade(data.spiritRootGrade(), payload.delta()));
                        case "skill_points", "max_hp", "max_qi", "max_energy", "physical", "magic", "mental", "talent" ->
                                data.debugAdjustStat(payload.stat(), payload.delta());
                        default -> data;
                    };
                    if (payload.stat().startsWith(METHOD_PROFICIENCY_PREFIX)) {
                        updated = adjustMethodProficiency(data, payload.stat().substring(METHOD_PROFICIENCY_PREFIX.length()), payload.delta());
                    } else if (payload.stat().startsWith(CRAFT_PROFICIENCY_PREFIX)) {
                        updated = adjustCraftProficiency(data,
                                payload.stat().substring(CRAFT_PROFICIENCY_PREFIX.length()), payload.delta());
                    } else if (payload.stat().startsWith(TALISMAN_MASTERY_PREFIX)) {
                        updated = adjustTalismanMastery(data,
                                payload.stat().substring(TALISMAN_MASTERY_PREFIX.length()), payload.delta());
                    }
                    if (updated != data) {
                        ModAttachments.setData(target, updated);
                        syncDebugTarget(target, editor);
                    }
                }
            });
        }

        private static void syncDebugTarget(ServerPlayer target, ServerPlayer editor) {
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(target);
            if (target != editor) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerDataTo(target, editor);
            }
        }

        private static void syncDebugHintCount(ServerPlayer target, ServerPlayer editor, int hints) {
            PacketDistributor.sendToPlayer(editor, new ClientboundDebugHintCountPayload(hints));
            if (target != editor) {
                PacketDistributor.sendToPlayer(target, new ClientboundDebugHintCountPayload(hints));
            }
        }

        private static ModAttachments.CultivationData adjustMethodProficiency(ModAttachments.CultivationData data, String methodId, int delta) {
            if (CultivationMethods.get(methodId) == null || delta == 0) {
                return data;
            }
            Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
            long value = (long) data.methodProficiency(methodId) + delta;
            updated.put(methodId, (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value)));
            return data.withMethodProficiencies(updated);
        }

        private static ModAttachments.CultivationData adjustCraftProficiency(
                ModAttachments.CultivationData data, String proficiencyId, int delta) {
            if (!CRAFT_PROFICIENCY_IDS.contains(proficiencyId) || delta == 0) {
                return data;
            }
            if ("corpse_control".equals(proficiencyId)) {
                Map<String, Integer> updated = new HashMap<>(data.spellProficiencies());
                long value = (long) data.spellProficiency(ModSpells.KONGSHI_SHU) + delta;
                updated.put(ModSpells.KONGSHI_SHU,
                        (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value)));
                return data.withSpellProficiencies(updated);
            }
            String storageKey = "craft_" + proficiencyId;
            Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
            long value = (long) data.methodProficiency(storageKey) + delta;
            updated.put(storageKey, (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value)));
            return data.withMethodProficiencies(updated);
        }

        private static ModAttachments.CultivationData adjustTalismanMastery(
                ModAttachments.CultivationData data, String recipeId, int delta) {
            if (TalismanRecipes.byId(recipeId) == null || delta == 0) {
                return data;
            }
            String storageKey = TalismanRecipes.successKey(recipeId);
            Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
            long value = (long) TalismanRecipes.successCount(data, recipeId) + delta;
            updated.put(storageKey, (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value)));
            return data.withMethodProficiencies(updated);
        }

        private static void handleDebugSetSpiritRootsOnServer(ServerboundDebugSetSpiritRootsPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer editor) {
                    ServerPlayer target = resolveDebugTarget(editor, payload.targetId());
                    if (target == null) {
                        return;
                    }
                    var data = ModAttachments.getData(target);
                    var roots = SpiritRoots.sanitizeRootList(payload.roots());
                    String grade = SpiritRoots.sanitizeGrade(payload.grade());
                    ModAttachments.setData(target, data.withSpiritRoots(roots, grade));
                    syncDebugTarget(target, editor);
                }
            });
        }

        private static void handleDebugSetJindanQualityOnServer(ServerboundDebugSetJindanQualityPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer editor) {
                    ServerPlayer target = resolveDebugTarget(editor, payload.targetId());
                    if (target == null) {
                        return;
                    }
                    com.example.immortal_cultivation_mod.event.ServerEvents.setJindanQuality(
                            target, payload.quality());
                    if (target != editor) {
                        com.example.immortal_cultivation_mod.event.ServerEvents
                                .syncPlayerDataTo(target, editor);
                    }
                }
            });
        }

        private static void handleDebugSetOuterSystemOnServer(
                ServerboundDebugSetOuterSystemPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer editor) {
                    ServerPlayer target = resolveDebugTarget(editor, payload.targetId());
                    if (target != null) {
                        OuterSystemManager.setDebugSystem(
                                target, OuterSystemType.byId(payload.systemType()));
                    }
                }
            });
        }

        private static void handleCloseDebugStatEditorOnServer(
                ServerboundCloseDebugStatEditorPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer editor) {
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(editor);
                    PacketDistributor.sendToPlayer(editor, new ClientboundDebugHintCountPayload(
                            RestrictionInsightItem.hintCount(editor)));
                }
            });
        }

        private static String nextCultivationLevel(String current, int direction) {
            if (com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_HALF_JINDAN.equals(current)) {
                return direction < 0 ? com.example.immortal_cultivation_mod.attachment.CultivationLevels.getPreviousStage(current) : com.example.immortal_cultivation_mod.attachment.CultivationLevels.getNextStage(current);
            }
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
                    sp.getPersistentData().putString(
                            com.example.immortal_cultivation_mod.entity.NpcProfile.PLAYER_GENDER_TAG,
                            com.example.immortal_cultivation_mod.entity.NpcProfile.normalizePlayerGender(payload.gender()));
                    var updated = data
                            .withCultivationLevel(CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY)
                            .withSpiritRoots(roots, grade)
                            .withBodyType(bodyType);
                    ModAttachments.setData(sp, updated);
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_success"));
                    if (sp.getRandom().nextInt(1000) == 0) {
                        sp.getInventory().placeItemBackInInventory(
                                new net.minecraft.world.item.ItemStack(ModItems.MYSTERIOUS_RING.get()));
                    }
                    com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                            sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.ENTER_LIANQI);
                }
            });
        }

        private static boolean consumeEnlightenmentPill(ServerPlayer sp) {
            if (sp.getAbilities().instabuild) {
                return true;
            }
            var pill = ModItems.ENLIGHTENMENT_PILL.get();
            if (sp.getMainHandItem().is(pill)) {
                com.example.immortal_cultivation_mod.alchemy.PillToxicity.consumed(sp,sp.getMainHandItem());
                sp.getMainHandItem().shrink(1);
                return true;
            }
            if (sp.getOffhandItem().is(pill)) {
                com.example.immortal_cultivation_mod.alchemy.PillToxicity.consumed(sp,sp.getOffhandItem());
                sp.getOffhandItem().shrink(1);
                return true;
            }
            for (var stack : sp.getInventory().items) {
                if (stack.is(pill)) {
                    com.example.immortal_cultivation_mod.alchemy.PillToxicity.consumed(sp,stack);
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
                        sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", CultivationLevels.displayName(spell.requiredLevel())));
                        return;
                    }
                    if (data.knownSpells().contains(spellId) || ModSpells.isInnateKnown(spellId, data)) {
                        return;
                    }
                    beginLearningSession(sp, "scroll", "spell", spellId, -1, spell.requiredLevel());
                }
            });
        }

        private static void handleSetSteleSpellOnServer(ServerboundSetSteleSpellPayload payload,
                                                         IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player)
                        || player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0D
                        || (!player.getMainHandItem().is(ModItems.DEBUG_STAT_EDITOR.get())
                        && !player.getOffhandItem().is(ModItems.DEBUG_STAT_EDITOR.get()))
                        || !player.level().getBlockState(payload.pos()).is(
                        com.example.immortal_cultivation_mod.block.ModBlocks.DUNWU_STELE.get())
                        || !(player.level().getBlockEntity(payload.pos())
                        instanceof com.example.immortal_cultivation_mod.block.GeoRockBlockEntity stele)) {
                    return;
                }
                String spellId = payload.spellId().isBlank()
                        ? ""
                        : ModSpells.normalizeId(payload.spellId());
                ModSpells.SpellDef spell = spellId.isBlank() ? null : ModSpells.get(spellId);
                if (!spellId.isBlank() && spell == null) {
                    return;
                }
                stele.setSteleSpellId(spellId);
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".dunwu_stele.set",
                        spell == null
                                ? Component.translatable("screen." + ImmortalCultivationMod.MODID
                                + ".stele_spell_editor.empty")
                                : spell.name()), true);
            });
        }

        private static boolean beginRestrictionInsightLearning(ServerPlayer player) {
            if (!player.isAlive()) return false;
            PENDING_LEARNING.remove(player.getUUID());
            long token = player.getRandom().nextLong() ^ player.level().getGameTime();
            PendingLearning pending = new PendingLearning(
                    "restriction_insight",
                    "restriction_insight",
                    "restriction_insight",
                    -1,
                    RestrictionInsightItem.LEARNING_GOAL,
                    token,
                    player.level().getGameTime() + LEARNING_SESSION_TICKS);
            PENDING_LEARNING.put(player.getUUID(), pending);
            PacketDistributor.sendToPlayer(player, new ClientboundOpenLearningGamePayload(
                    pending.learningType(), pending.targetId(), pending.requiredPoints(),
                    1.0F, 1.0F, NoobAchievement.hasReward(player), ModAttachments.getData(player).cultivationLevel(), token));
            MysteriousRingItem.onLearningStarted(player);
            return true;
        }

        private static boolean beginDunwuSteleLearning(ServerPlayer player, String requestedSpellId) {
            if (!player.isAlive()) return false;
            PENDING_LEARNING.remove(player.getUUID());
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            String spellId = ModSpells.normalizeId(requestedSpellId);
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            if (spell == null || data.knownSpells().contains(spellId)
                    || ModSpells.isInnateKnown(spellId, data)
                    || !ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                return false;
            }
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.discover(player, "learning_puzzles");
            if (player.getAbilities().instabuild) {
                completeDunwuSteleLearning(player, spellId);
                return true;
            }
            int maxQi = Math.max(1, data.maxQi());
            int requiredPoints = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, maxQi * 75L));
            long token = player.getRandom().nextLong() ^ player.level().getGameTime();
            PendingLearning pending = new PendingLearning(
                    "dunwu_stele",
                    "spell",
                    spellId,
                    -1,
                    requiredPoints,
                    token,
                    player.level().getGameTime() + LEARNING_SESSION_TICKS);
            PENDING_LEARNING.put(player.getUUID(), pending);
            PacketDistributor.sendToPlayer(player, new ClientboundOpenLearningGamePayload(
                    pending.learningType(), pending.targetId(), pending.requiredPoints(),
                    1.0F, 1.0F, NoobAchievement.hasReward(player), data.cultivationLevel(), token));
            MysteriousRingItem.onLearningStarted(player);
            return true;
        }

        private static void beginLearningSession(ServerPlayer player, String source, String learningType,
                                                 String targetId, int npcEntityId, String teacherLevel) {
            if (!player.isAlive() || targetId == null || targetId.isBlank()
                    || (!("spell".equals(learningType)) && !("method".equals(learningType)))) {
                return;
            }
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.discover(player, "learning_puzzles");
            if (player.getAbilities().instabuild) {
                switch (source) {
                    case "scroll" -> completeScrollSpellLearning(player, targetId);
                    case "method_item" -> activateMethod(player, targetId);
                    case "npc" -> NpcProfile.completeNpcLearning(
                            player, npcEntityId, learningType, targetId);
                    default -> {
                    }
                }
                return;
            }
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            float talent = ModAttachments.CultivationData.sanitizeTalent(data.talent());
            float difficulty = learningDifficulty(data.cultivationLevel(), teacherLevel);
            int requiredPoints = Math.max(1, (int) Math.ceil(BASE_LEARNING_POINTS * difficulty / talent));
            long token = player.getRandom().nextLong() ^ player.level().getGameTime();
            PendingLearning pending = new PendingLearning(
                    source,
                    learningType,
                    targetId,
                    npcEntityId,
                    requiredPoints,
                    token,
                    player.level().getGameTime() + LEARNING_SESSION_TICKS);
            PENDING_LEARNING.put(player.getUUID(), pending);
            PacketDistributor.sendToPlayer(player, new ClientboundOpenLearningGamePayload(
                    learningType, targetId, requiredPoints, difficulty, talent,
                    NoobAchievement.hasReward(player), data.cultivationLevel(), token));
            MysteriousRingItem.onLearningStarted(player);
        }

        private static float learningDifficulty(String playerLevel, String teacherLevel) {
            if (teacherLevel == null || teacherLevel.isBlank()) {
                return 1.0F;
            }
            int playerStage = CultivationLevels.getStageIndex(playerLevel);
            int teacherStage = CultivationLevels.getStageIndex(teacherLevel);
            if (teacherStage <= 0 || playerStage <= teacherStage) {
                return 1.0F;
            }
            if (playerStage - teacherStage == 1) {
                return 0.9F;
            }
            int realmDifference = CultivationLevels.getRealmIndex(playerLevel)
                    - CultivationLevels.getRealmIndex(teacherLevel);
            return realmDifference == 1 ? 0.5F : 0.1F;
        }

        private static void handleCompleteLearningGameOnServer(
                ServerboundCompleteLearningGamePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player)) {
                    return;
                }
                PendingLearning pending = PENDING_LEARNING.get(player.getUUID());
                if (!player.isAlive() || pending == null
                        || pending.token() != payload.token()
                        || player.level().getGameTime() > pending.expiresAt()
                        || payload.score() < pending.requiredPoints()) {
                    return;
                }
                PENDING_LEARNING.remove(player.getUUID());
                switch (pending.source()) {
                    case "scroll" -> completeScrollSpellLearning(player, pending.targetId());
                    case "method_item" -> activateMethod(player, pending.targetId());
                    case "npc" -> NpcProfile.completeNpcLearning(
                            player, pending.npcEntityId(), pending.learningType(), pending.targetId());
                    case "restriction_insight" -> RestrictionInsightItem.completeLearning(player);
                    case "dunwu_stele" -> completeDunwuSteleLearning(player, pending.targetId());
                    case "lianshen_spell" -> completeLianshenSpellPuzzle(player);
                    default -> {
                    }
                }
                MysteriousRingItem.onLearningSucceeded(player);
            });
        }

        private static void handleCancelLearningGameOnServer(
                ServerboundCancelLearningGamePayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player)) {
                    return;
                }
                PendingLearning pending = PENDING_LEARNING.get(player.getUUID());
                if (pending != null && pending.token() == payload.token()) {
                    PENDING_LEARNING.remove(player.getUUID());
                    MysteriousRingItem.onLearningFailed(player);
                }
            });
        }

        private static void completeScrollSpellLearning(ServerPlayer player, String spellId) {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            if (spell == null
                    || data.knownSpells().contains(spellId)
                    || ModSpells.isInnateKnown(spellId, data)
                    || !ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                return;
            }
            List<String> known = new ArrayList<>(data.knownSpells());
            known.add(spellId);
            ModAttachments.setData(player, data.withKnownSpells(known));
            consumeLearnedScroll(player, spellId);
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".spell_learned", spell.name()));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.LEARN_SPELL);
        }

        private static void completeDunwuSteleLearning(ServerPlayer player, String spellId) {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            if (spell == null
                    || data.knownSpells().contains(spellId)
                    || ModSpells.isInnateKnown(spellId, data)
                    || !ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
                return;
            }
            List<String> known = new ArrayList<>(data.knownSpells());
            known.add(spellId);
            ModAttachments.setData(player, data.withKnownSpells(known));
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dunwu_stele.learned",
                    spell.name()));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.LEARN_SPELL);
        }

        private static boolean beginLianshenSpellPuzzle(ServerPlayer player, ModAttachments.CultivationData data) {
            if (!player.isAlive()) return false;
            if (!data.knownSpells().contains(ModSpells.LIANSHEN_SHU) || data.hasLianshenThoughtBonus()) {
                return false;
            }
            int maxQi = Math.max(1, data.maxQi());
            int qiCost = Math.max(1, (int)((long)maxQi * 75 / 100));
            if (data.qi() < qiCost) {
                sendNotEnoughQi(player);
                return false;
            }
            ModAttachments.setData(player, data.withQi(data.qi() - qiCost));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);

            PENDING_LEARNING.remove(player.getUUID());
            int requiredPoints = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, maxQi * 75L));
            long token = player.getRandom().nextLong() ^ player.level().getGameTime();
            PendingLearning pending = new PendingLearning(
                    "lianshen_spell",
                    "spell",
                    ModSpells.LIANSHEN_SHU,
                    -1,
                    requiredPoints,
                    token,
                    player.level().getGameTime() + LEARNING_SESSION_TICKS);
            PENDING_LEARNING.put(player.getUUID(), pending);
            PacketDistributor.sendToPlayer(player, new ClientboundOpenLearningGamePayload(
                    pending.learningType(), pending.targetId(), pending.requiredPoints(),
                    1.0F, 1.0F, NoobAchievement.hasReward(player), data.cultivationLevel(), token));
            MysteriousRingItem.onLearningStarted(player);
            return true;
        }

        private static void completeLianshenSpellPuzzle(ServerPlayer player) {
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            if (!data.knownSpells().contains(ModSpells.LIANSHEN_SHU) || data.hasLianshenThoughtBonus()) {
                return;
            }
            ModAttachments.setData(player, data.withLianshenThoughtBonus());
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".lianshen_shu.completed"));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
        }

        private static void activateMethod(ServerPlayer player, String methodId) {
            CultivationMethods.MethodDef def = CultivationMethods.get(methodId);
            if (def == null) {
                return;
            }
            ModAttachments.CultivationData current = ModAttachments.getData(player);
            Map<String, Integer> learnedMethods = new HashMap<>(current.methodProficiencies());
            if (CultivationMethods.get(current.activeCultivationMethod()) != null) {
                learnedMethods.putIfAbsent(current.activeCultivationMethod(), 0);
            }
            learnedMethods.putIfAbsent(methodId, 0);
            ModAttachments.setData(player, current.withMethodProficiencies(learnedMethods)
                    .withActiveCultivationMethod(methodId));
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".method_activated",
                    Component.translatable(def.nameKey())));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.ACTIVATE_METHOD);
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
                case ModSpells.JINMANG_CHUANSHA -> ModItems.JINMANG_CHUANSHA_SCROLL.get();
                case ModSpells.JINFENG_ZHI -> ModItems.JINFENG_ZHI_SCROLL.get();
                case ModSpells.WANREN_CHUANXIN -> ModItems.WANREN_CHUANXIN_SCROLL.get();
                case ModSpells.JINGJI_TUSHENG -> ModItems.JINGJI_TUSHENG_SCROLL.get();
                case ModSpells.WANTENG_JIAOSHA -> ModItems.WANTENG_JIAOSHA_SCROLL.get();
                case ModSpells.YANBAO_DAN -> ModItems.YANBAO_DAN_SCROLL.get();
                case ModSpells.CHIYAN_LONGJUAN -> ModItems.CHIYAN_LONGJUAN_SCROLL.get();
                case ModSpells.TIANYUN_FENCHENG -> ModItems.TIANYUN_FENCHENG_SCROLL.get();
                case ModSpells.LUOHUA_CHANSHEN -> ModItems.LUOHUA_CHANSHEN_SCROLL.get();
                case ModSpells.BAIHUA_ZANG -> ModItems.BAIHUA_ZANG_SCROLL.get();
                case ModSpells.POKONG_JIANHEN -> ModItems.POKONG_JIANHEN_SCROLL.get();
                case ModSpells.SANJIAN_ZHUIMING -> ModItems.SANJIAN_ZHUIMING_SCROLL.get();
                case ModSpells.DIYAN_QIANG -> ModItems.DIYAN_QIANG_SCROLL.get();
                case ModSpells.SHANBENG_YIN -> ModItems.SHANBENG_YIN_SCROLL.get();
                case ModSpells.SHUIREN_LIANZHAN -> ModItems.SHUIREN_LIANZHAN_SCROLL.get();
                case ModSpells.CANGLANG_CHONG -> ModItems.CANGLANG_CHONG_SCROLL.get();
                case ModSpells.LEISHAN -> ModItems.LEISHAN_SCROLL.get();
                case ModSpells.WULEI_HONGDING -> ModItems.WULEI_HONGDING_SCROLL.get();
                case ModSpells.BINGZHUI_YU -> ModItems.BINGZHUI_YU_SCROLL.get();
                case ModSpells.HANYU_FENGDI -> ModItems.HANYU_FENGDI_SCROLL.get();
                case ModSpells.YAOGUANG_SHAN -> ModItems.YAOGUANG_SHAN_SCROLL.get();
                case ModSpells.TIANGUANG_GUANSHA -> ModItems.TIANGUANG_GUANSHA_SCROLL.get();
                case ModSpells.YINGXI -> ModItems.YINGXI_SCROLL.get();
                case ModSpells.TIANFENG_ZHUI -> ModItems.TIANFENG_ZHUI_SCROLL.get();
                case ModSpells.MULING_SHENGXI -> ModItems.MULING_SHENGXI_SCROLL.get();
                case ModSpells.XUE_GUIYUAN_SHU -> ModItems.XUE_GUIYUAN_SHU_SCROLL.get();
                case ModSpells.SHENGXI_BAOFA -> ModItems.SHENGXI_BAOFA_SCROLL.get();
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
                case ModSpells.XUANYAN_HUTI -> ModItems.XUANYAN_HUTI_SCROLL.get();
                case ModSpells.LIUFENG_XIEJIN -> ModItems.LIUFENG_XIEJIN_SCROLL.get();
                case ModSpells.XUESHA_HUSHEN -> ModItems.XUESHA_HUSHEN_SCROLL.get();
                case ModSpells.JINGLING_FANZHAO -> ModItems.JINGLING_FANZHAO_SCROLL.get();
                case ModSpells.YUANGANG_HUJIE -> ModItems.YUANGANG_HUJIE_SCROLL.get();
                case ModSpells.KONGSHI_SHU -> ModItems.KONGSHI_SHU_SCROLL.get();
                case ModSpells.XUYING_TA -> ModItems.XUYING_TA_SCROLL.get();
                case ModSpells.DUANLIU_KONGDUN -> ModItems.DUANLIU_KONGDUN_SCROLL.get();
                case ModSpells.YIHEN_CI -> ModItems.YIHEN_CI_SCROLL.get();
                case ModSpells.JINHUO_XINLIAO -> ModItems.JINHUO_XINLIAO_SCROLL.get();
                case ModSpells.SUIGUI_XINGYUN -> ModItems.SUIGUI_XINGYUN_SCROLL.get();
                case ModSpells.SHIXU_CHILIU -> ModItems.SHIXU_CHILIU_SCROLL.get();
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

        private static void handleRingDialogueOnClient(
                ClientboundRingDialoguePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist
                    == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleRingDialogue(payload, ctx);
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

        private static void handleJindanTribulationOnClient(ClientboundJindanTribulationPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleJindanTribulation(payload, ctx);
            }
        }

        private static void handleNpcProfileOnClient(ClientboundNpcProfilePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleNpcProfile(payload, ctx);
            }
        }

        private static void handleOpenLearningGameOnClient(
                ClientboundOpenLearningGamePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleOpenLearningGame(payload, ctx);
            }
        }

        private static void handleDebugEyeStateOnClient(ClientboundDebugEyeStatePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleDebugEyeState(payload, ctx);
            }
        }

        private static void handleJindanFlightControlOnClient(
                ClientboundJindanFlightControlPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleJindanFlightControl(payload, ctx);
            }
        }

        private static void handleJindanFlightSpeedOnClient(
                ClientboundJindanFlightSpeedPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleJindanFlightSpeed(payload, ctx);
            }
        }

        private static void handleJindanFlightDiveOnClient(
                ClientboundJindanFlightDivePayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleJindanFlightDive(payload, ctx);
            }
        }

        private static void handleDebugHintCountOnClient(ClientboundDebugHintCountPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleDebugHintCount(payload, ctx);
            }
        }

        private static void handleOpenDebugStatEditorOnClient(
                ClientboundOpenDebugStatEditorPayload payload, IPayloadContext ctx) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers
                        .handleOpenDebugStatEditor(payload, ctx);
            }
        }

        private static void handleBreakthroughOnServer(ServerboundRequestBreakthroughPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                            sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.BREAKTHROUGH_ATTEMPT);
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
                    if (!ModSpells.ZIBAO.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.cancel(sp);
                    }
                    if (!CombatStateManager.finishSpellPreparation(
                            sp, spellId, payload.chargeScale())) {
                        return;
                    }
                    castValidatedSpell(sp, spellId, payload.chargeScale(), payload.selfCast());
                }
            });
        }

        private static void handleSpellPreparationOnServer(
                ServerboundSpellPreparationPayload payload,
                IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sp)) {
                    return;
                }
                if (payload.preparing()) {
                    if (!ModSpells.ZIBAO.equals(ModSpells.normalizeId(payload.spellName()))) {
                        com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.cancel(sp);
                    }
                    CombatStateManager.beginSpellPreparation(sp, payload.spellName());
                } else {
                    CombatStateManager.cancelSpellPreparation(sp);
                    com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.cancel(sp);
                }
            });
        }

        private record PendingLearning(
                String source,
                String learningType,
                String targetId,
                int npcEntityId,
                int requiredPoints,
                long token,
                long expiresAt
        ) {
        }

        private static void castValidatedSpell(ServerPlayer sp, String spellId, float requestedChargeScale) {
            castValidatedSpell(sp, spellId, requestedChargeScale, sp.isShiftKeyDown());
        }

        private static void castValidatedSpell(
                ServerPlayer sp,
                String spellId,
                float requestedChargeScale,
                boolean selfCast) {
                    float chargeScale = chargeScaleFor(sp, spellId, requestedChargeScale);
                    var spell = ModSpells.get(spellId);
                    if (spell == null) {
                        return;
                    }
                    if(ModSpells.YANMIE_WEIJIN_ZHIREN.equals(spellId)) {
                        com.example.immortal_cultivation_mod.curse.AnnihilationCurse.showStatus(sp);
                        return;
                    }
                    var data = ModAttachments.getData(sp);
                    if (CombatStateManager.isSpellcastingBlocked(sp)) {
                        return;
                    }
                    if (sp.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(spellId)) {
                        return;
                    }
                    boolean innateKnown = ModSpells.isInnateKnown(spellId, data)
                            || com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem
                            .canPlayerUseAbility(sp, spellId);
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
                                Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", CultivationLevels.displayName(spell.requiredLevel())));
                        return;
                    }
                    if (isSpellOnCooldown(sp, spellId)) {
                        return;
                    }
                    if (ModSpells.ZIBAO.equals(spellId)
                            && !com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.confirm(sp)) {
                        return;
                    }
                    com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport
                            .beginSpellCast(sp, spellId);
                    try {
                    net.minecraft.world.phys.Vec3 ribbonCastOrigin = sp.position();
                    boolean castSucceeded = true;
                    if (com.example.immortal_cultivation_mod.fate.FateTechniques.isTechnique(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.fate.FateTechniques.start(sp, spellId);
                    } else if (com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem
                            .isPlayerAbility(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem
                                .castPlayerAbility(sp, spellId);
                    } else if (ModSpells.FIREBALL.equals(spellId)) {
                        castSucceeded = castFireball(sp, data, spell, chargeScale);
                    } else if (ModSpells.LINGBENG.equals(spellId)) {
                        castSucceeded = castLingbeng(sp, data, spell);
                    } else if (ModSpells.REGENERATION.equals(spellId)) {
                        castSucceeded = castRegeneration(sp, data, spell);
                    } else if (ModSpells.BEAM.equals(spellId)) {
                        castBeam(sp, data, spell);
                    } else if (ModSpells.EARTH_ESCAPE.equals(spellId)) {
                        castEarthEscape(sp, data);
                    } else if (ModSpells.CLEANSE.equals(spellId)) {
                        castSucceeded = castCleanse(sp, data, spell);
                    } else if (ModSpells.QI_GATHERING.equals(spellId)) {
                        castSucceeded = castQiGathering(sp);
                    } else if (ModSpells.IGNITE_FLARE.equals(spellId)) {
                        castSucceeded = castIgniteFlare(sp, data, spell, chargeScale);
                    } else if (ModSpells.SPIRIT_SIGHT.equals(spellId)) {
                        com.example.immortal_cultivation_mod.event.ServerEvents.toggleSpiritSight(sp);
                    } else if (ModSpells.ZHENSHAN_PALM.equals(spellId)) {
                        castSucceeded = castZhenshanPalm(sp, data, spell, chargeScale);
                    } else if (ModSpells.LIGHT_BEAM_ATTACK.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.LightBeamAttack.cast(sp);
                    } else if (ModSpells.DIELANG_SHIELD.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.DielangShield.cast(sp);
                    } else if (ModSpells.LINGZHI_BULLET.equals(spellId)) {
                        castSucceeded = castLingzhiBullet(sp, data, spell, chargeScale);
                    } else if (ModSpells.JINMANG_CHUANSHA.equals(spellId)) {
                        castSucceeded = castJinmangChuansha(sp, data, spell);
                    } else if (com.example.immortal_cultivation_mod.spell.HealingTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castHealingTechnique(sp, data, spell, spellId, selfCast);
                    } else if (com.example.immortal_cultivation_mod.spell.SolarTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castSolarTechnique(
                                sp, data, spell, spellId, chargeScale);
                    } else if (com.example.immortal_cultivation_mod.spell.GangtingTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castGangtingTechnique(
                                sp, data, spell, spellId, chargeScale);
                    } else if (com.example.immortal_cultivation_mod.spell.XuanyueTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castXuanyueTechnique(
                                sp, data, spell, spellId, chargeScale);
                    } else if (com.example.immortal_cultivation_mod.spell.HanlanTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castHanlanTechnique(
                                sp, data, spell, spellId, chargeScale);
                    } else if (com.example.immortal_cultivation_mod.spell.ChangqingTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castChangqingTechnique(
                                sp, data, spell, spellId, chargeScale);
                    } else if (com.example.immortal_cultivation_mod.spell.ElementalTechniques
                            .isTechnique(spellId)) {
                        castSucceeded = castElementalTechnique(
                                sp, data, spell, spellId, chargeScale);
                    } else if (ModSpells.WIND_BLADE.equals(spellId)) {
                        castSucceeded = castWindBlade(sp, data, spell, chargeScale);
                    } else if (ModSpells.WIND_STEP.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.WindStep.toggle(sp);
                    } else if (ModSpells.YUFENG_JUE.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.YufengJue.toggle(sp);
                    } else if (ModSpells.SMOKE_ART.equals(spellId)) {
                        castSucceeded = castSmokeArt(sp, data, spell, chargeScale);
                    } else if (ModSpells.HUTI_QI.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.HutiQi.cast(sp);
                    } else if (ModSpells.MICHEN_ZHANG.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.MichenZhang.cast(sp, chargeScale);
                    } else if (ModSpells.SLIDING_WATER.equals(spellId)) {
                        castSucceeded = castSlidingWater(sp, data, spell, chargeScale);
                    } else if (ModSpells.WEIYA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.Weiya.toggle(sp);
                    } else if (ModSpells.ZIBAO.equals(spellId)) {
                        castZibao(sp, data);
                    } else if (ModSpells.ABSORB_CULTIVATION.equals(spellId)) {
                        castSucceeded = castAbsorbCultivation(sp, data, spell, chargeScale);
                    } else if (ModSpells.TUNTIAN.equals(spellId)) {
                        castSucceeded = castTuntian(sp, data, chargeScale);
                    } else if (ModSpells.FENGYA.equals(spellId)) {
                        com.example.immortal_cultivation_mod.spell.Fengya.toggle(sp);
                    } else if (ModSpells.DINGSHEN.equals(spellId)) {
                        castSucceeded = castDingshen(sp, data, spell, chargeScale);
                    } else if (ModSpells.YINLEI_JUE.equals(spellId)) {
                        castSucceeded = castLightning(sp, data, spell, false, chargeScale);
                    } else if (ModSpells.WULEI_ZHENGFA.equals(spellId)) {
                        castSucceeded = castLightning(sp, data, spell, true, chargeScale);
                    } else if (ModSpells.LIUGUANG_JIANYING.equals(spellId)) {
                        castSucceeded = castLiuguangJianying(sp, data, spell, chargeScale);
                    } else if (ModSpells.SIFANG_JIE.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.SifangJie.cast(sp);
                    } else if (ModSpells.GUSHI_SHIELD.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.GushiShield.cast(sp);
                    } else if (ModSpells.XUANYAN_HUTI.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.CultivationShieldSpells.castXuanyanHuti(sp);
                    } else if (ModSpells.LIUFENG_XIEJIN.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.CultivationShieldSpells.castLiufengXiejin(sp);
                    } else if (ModSpells.XUESHA_HUSHEN.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.CultivationShieldSpells.castXueshaHushen(sp);
                    } else if (ModSpells.JINGLING_FANZHAO.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.CultivationShieldSpells.castJinglingFanzhao(sp);
                    } else if (ModSpells.YUANGANG_HUJIE.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.CultivationShieldSpells.castYuangangHujie(sp);
                    } else if (ModSpells.KONGSHI_SHU.equals(spellId)) {
                        castKongshiShu(sp, data, spell, chargeScale);
                    } else if (ModSpells.HANJING_SUOZHUA.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.HantiBingqin.castClaw(sp, chargeScale);
                    } else if (ModSpells.SHUANGTIAN_QI.equals(spellId)) {
                        castSucceeded = castFrostCry(sp, data, chargeScale);
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
                    } else if (ModSpells.JINHUO_XINLIAO.equals(spellId)) {
                        castSucceeded = castJinhuoXinliao(sp, data, spell, chargeScale);
                    } else if (ModSpells.SUIGUI_XINGYUN.equals(spellId)) {
                        castSucceeded = castSuiguiXingyun(sp, data, spell, chargeScale);
                    } else if (ModSpells.DONGTIAN_JIELING.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.DongtianJieling.cast(sp);
                    } else if (ModSpells.SHIXU_CHILIU.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.ShixuChiliu.toggle(sp);
                    } else if (ModSpells.LIANSHEN_SHU.equals(spellId)) {
                        castSucceeded = beginLianshenSpellPuzzle(sp, data);
                    } else if (ModSpells.SUOKONG_SHU.equals(spellId)) {
                        castSucceeded = castSuokongShu(sp, data, chargeScale);
                    } else if (ModSpells.CUIGU_LIANSHEN.equals(spellId)) {
                        castSucceeded = com.example.immortal_cultivation_mod.spell.BodyCultivation.cast(sp);
                    }
                    if (castSucceeded) {
                        SpellRibbonPayload.cast(sp, spellId, ribbonCastOrigin);
                        com.example.immortal_cultivation_mod.spell.GangtingXiaokongLu
                                .onTechniqueCast(sp, spellId);
                        sendCastAnimation(sp, spellId);
                        com.example.immortal_cultivation_mod.shentong.ShentongActivations.announce(sp,spellId);
                        if (!com.example.immortal_cultivation_mod.fate.FateTechniques.isTechnique(spellId)
                                && !com.example.immortal_cultivation_mod.spell.CultivationShieldSpells
                                .handlesOwnCooldown(spellId)) {
                            startSpellCooldown(sp, spellId);
                        }
                        addSpellProficiency(sp, spellId, 1);
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                                sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.CAST_SPELL);
                        if (chargeScale >= 1.5F) {
                            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                                    sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.CHARGED_SPELL);
                        }
                        var guidanceData = ModAttachments.getData(sp);
                        int guidanceMaxQi = Math.max(1,
                                guidanceData.maxQi());
                        if (guidanceData.qi() * 5 >= guidanceMaxQi) {
                            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                                    sp, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.MANAGE_QI);
                        }
                    }
                    } finally {
                        com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport
                                .endSpellCast();
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
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.hint(
                    player, "qi_insufficient");
        }

        private static void castZibao(ServerPlayer sp, ModAttachments.CultivationData data) {
            float radius = zibaoRadius(data.cultivationLevel());
            if (radius <= 0.0F) {
                return;
            }
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
            ModAttachments.setData(sp, data.withAgePenalty(levelDef.maxAge()));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(sp.level(), sp, sp.getX(), sp.getY(), sp.getZ(), zibaoExplosionPower(radius), Level.ExplosionInteraction.BLOCK);
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

        public static Map<String, Long> debugSpellCooldowns(ServerPlayer player) {
            var result = new java.util.TreeMap<String, Long>();
            long now = player.level().getGameTime();
            SPELL_COOLDOWNS.getOrDefault(player.getUUID(), Map.of()).forEach((id, until) -> {
                if (until > now) result.put(id, until - now);
            });
            return result;
        }

        public static void reduceAttackCooldowns(ServerPlayer player, int ticks) {
            var cooldowns = SPELL_COOLDOWNS.get(player.getUUID());
            if (cooldowns == null) return;
            long now = player.level().getGameTime();
            cooldowns.replaceAll((id, until) -> {
                if (!ModSpells.isAttackSpell(id)) return until;
                long next = Math.max(now, until - ticks);
                PacketDistributor.sendToPlayer(player, new ClientboundSpellCooldownPayload(id, (int)(next - now)));
                return next;
            });
        }

        private static void startSpellCooldown(ServerPlayer player, String spellId) {
            int ticks = ModSpells.CUIGU_LIANSHEN.equals(spellId)
                    ? com.example.immortal_cultivation_mod.spell.BodyCultivation.cooldownTicks(player)
                    : spellCooldownTicks(spellId);
            ticks = com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cooldown(player, ticks);
            if (ticks <= 0) {
                return;
            }
            SPELL_COOLDOWNS
                    .computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(spellId, player.level().getGameTime() + ticks);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new ClientboundSpellCooldownPayload(spellId, ticks));
        }

        /** Starts cooldowns whose clock begins when a sustained spell deactivates. */
        public static void startExternalSpellCooldown(ServerPlayer player, String spellId) {
            startSpellCooldown(player, ModSpells.normalizeId(spellId));
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
            while (scale > 1.0F && !com.example.immortal_cultivation_mod.event.ServerEvents.canAffordQiOrBlood(
                    ModAttachments.getData(player),
                    com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport
                            .adjustSpellQiCost(player,
                                    chargedCost(baseCost, scale), spellId))) {
                scale = Math.max(1.0F, scale - 0.025F);
            }
            return scale;
        }

        private static int baseChargeCost(ServerPlayer player, String spellId, ModAttachments.CultivationData data) {
            if (ModSpells.HANJING_SUOZHUA.equals(spellId)) {
                int maxQi = Math.max(1, data.maxQi());
                return Math.max(1, (int)((long)maxQi * 3 / 100));
            }
            if (ModSpells.SHUANGTIAN_QI.equals(spellId)) {
                int maxQi = Math.max(1, data.maxQi());
                return Math.max(1, (int)((long)maxQi * 5 / 100));
            }
            if (ModSpells.SUOKONG_SHU.equals(spellId)) {
                return suokongQiCost(data);
            }
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            return spell == null ? 0 : Math.max(0, spell.qiCost());
        }

        private static int chargedCost(int baseCost, float chargeScale) {
            return com.example.immortal_cultivation_mod.spell.SpellCharge
                    .qiCost(baseCost, chargeScale);
        }

        private static float chargedAreaScale(float chargeScale) {
            return Math.max(1.0F, Math.min(2.0F, chargeScale));
        }

        private static void addSpellProficiency(ServerPlayer player, String spellId, int amount) {
            var data = ModAttachments.getData(player);
            int oldValue = data.spellProficiencies().getOrDefault(spellId, 0);
            int adjustedAmount = amount
                    * com.example.immortal_cultivation_mod.item.CultivationAccessoryItem
                    .proficiencyMultiplier(player);
            ModAttachments.setData(player,
                    data.withAddedSpellProficiency(spellId, adjustedAmount));
            int newValue = ModAttachments.getData(player)
                    .spellProficiencies().getOrDefault(spellId, oldValue);
            MysteriousRingItem.onSpellCast(player, spellId);
            MysteriousRingItem.onSpellProficiencyIncreased(player, oldValue, newValue);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(player);
        }

        private static void sendCastAnimation(ServerPlayer sp, String spellId) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    sp,
                    new ClientboundCastAnimationPayload(
                            sp.getUUID(), ModSpells.normalizeId(spellId)));
        }

        private static boolean canCastSpell(ServerPlayer sp, String spellId) {
            var spell = ModSpells.get(spellId);
            if (spell == null) {
                return false;
            }
            var data = ModAttachments.getData(sp);
            if (CombatStateManager.isSpellcastingBlocked(sp)) {
                return false;
            }
            if (sp.hasEffect(ModEffects.QI_GATHERING) && !ModSpells.QI_GATHERING.equals(spellId)) {
                return false;
            }
            boolean innateKnown = ModSpells.isInnateKnown(spellId, data)
                    || com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem
                    .canPlayerUseAbility(sp, spellId);
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
                        Component.translatable("message." + ImmortalCultivationMod.MODID + ".spell_requirement", CultivationLevels.displayName(spell.requiredLevel())));
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
            } else if (ModSpells.SHIXU_CHILIU.equals(spellId)) {
                com.example.immortal_cultivation_mod.spell.ShixuChiliu.activateTimed(sp, durationTicks);
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

        private static void handleShihunfanSlashOnServer(ServerboundShihunfanSlashPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) {
                    com.example.immortal_cultivation_mod.item.ShihunfanItem.soulWindSlash(player);
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

        private static void handleSwordCaseActionOnServer(ServerboundSwordCaseActionPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    boolean handled = com.example.immortal_cultivation_mod.item.SwordCaseItem.handleAction(
                            sp, payload.releaseAll());
                    if (!handled && !payload.releaseAll()) {
                        com.example.immortal_cultivation_mod.item.ClothSheathItem.swapWeapon(sp);
                    }
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

        private static boolean castLingzhiBullet(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .lingzhiBullet(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castJinmangChuansha(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .jinmangChuansha(sp, null, projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castElementalTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                float chargeScale) {
            if (!com.example.immortal_cultivation_mod.spell.ElementalTechniques
                    .canCast(sp, null, spellId, chargeScale)) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.ElementalTechniques
                    .cast(sp, null, spellId, chargeScale,
                            projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castSolarTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                float chargeScale) {
            if (!com.example.immortal_cultivation_mod.spell.SolarTechniques
                    .canCast(sp, spellId)) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.SolarTechniques
                    .cast(sp, spellId, chargeScale,
                            projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castGangtingTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                float chargeScale) {
            if (!com.example.immortal_cultivation_mod.spell.GangtingTechniques
                    .canCast(sp, spellId)) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.GangtingTechniques
                    .cast(sp, spellId, chargeScale,
                            projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castXuanyueTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                float chargeScale) {
            if (!com.example.immortal_cultivation_mod.spell.XuanyueTechniques
                    .canCast(sp, spellId)) {
                return false;
            }
            int cost = chargedCost(spell.qiCost(), chargeScale);
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, cost)) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.XuanyueTechniques
                    .cast(sp, spellId, chargeScale,
                            projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castHanlanTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                float chargeScale) {
            if (!com.example.immortal_cultivation_mod.spell.HanlanTechniques
                    .canCast(sp, spellId)) {
                return false;
            }
            int cost = chargedCost(spell.qiCost(), chargeScale);
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, cost)) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.HanlanTechniques
                    .cast(sp, spellId, chargeScale,
                            projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castChangqingTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                float chargeScale) {
            if (!com.example.immortal_cultivation_mod.spell.ChangqingTechniques
                    .canCast(sp, spellId)) {
                return false;
            }
            int cost = chargedCost(spell.qiCost(), chargeScale);
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, cost)) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.ChangqingTechniques
                    .cast(sp, spellId, chargeScale,
                            projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castHealingTechnique(
                ServerPlayer sp,
                ModAttachments.CultivationData data,
                ModSpells.SpellDef spell,
                String spellId,
                boolean selfCast) {
            if (!com.example.immortal_cultivation_mod.spell.HealingTechniques
                    .canCast(sp, selfCast ? sp : null, spellId)) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents
                    .spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.HealingTechniques
                    .cast(sp, null, spellId, projectile -> { }, selfCast);
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castWindBlade(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .windBlade(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castSmokeArt(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .smokeArt(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castSlidingWater(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .slidingWater(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castAbsorbCultivation(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isReincarnationTrueArt(data.activeCultivationMethod())) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.AbsorbCultivationProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.8f, 0.0f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return true;
        }

        private static boolean castTuntian(ServerPlayer sp, ModAttachments.CultivationData data, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isTuntianDemonArt(data.activeCultivationMethod())) {
                return false;
            }
            if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                return false;
            }

            final int radius = Math.max(1, Math.round(5.0F * chargedAreaScale(chargeScale)));
            net.minecraft.world.phys.Vec3 centerVec = sp.position().add(0.0D, sp.getBbHeight() * 0.5D, 0.0D);
            long terrainSeed = serverLevel.random.nextLong();
            com.example.immortal_cultivation_mod.spell.SpellDestruction.area(serverLevel, sp,
                    new net.minecraft.world.phys.AABB(centerVec, centerVec).inflate(radius), centerVec,
                    131072, Float.MAX_VALUE,
                    pos -> com.example.immortal_cultivation_mod.spell.SpellDestruction.fractured(
                            pos, centerVec, radius, radius, terrainSeed), null, consumed -> {
                        if (consumed <= 0 || sp.isRemoved()) return;
                        var current = ModAttachments.getData(sp);
                        long need = Math.max(1L, CultivationLevels.getTotalQiNeeded(current.cultivationLevel()));
                        ModAttachments.setData(sp, current.withCultivationProgress(
                                Math.min(need, current.cultivationProgress() + com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cultivationGain(sp, consumed * 10L))));
                        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                    });
            long progressGain = 0;
            var damageSource = new net.minecraft.world.damagesource.DamageSource(
                    sp.damageSources().magic().typeHolder(), sp, sp, centerVec);
            var targets = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(centerVec, centerVec).inflate(radius),
                    entity -> entity.isAlive() && !entity.getUUID().equals(sp.getUUID()));
            for (var target : targets) {
                if (target.distanceToSqr(centerVec) > radius * radius) {
                    continue;
                }
                float maxHealth = target.getMaxHealth();
                target.hurt(damageSource, 100.0F);
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
                ModAttachments.setData(sp, data.withCultivationProgress(Math.min(need, data.cultivationProgress() + com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cultivationGain(sp, progressGain))));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return true;
        }

        private static boolean castDingshen(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .dingshen(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static void castKongshiShu(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.KongshiProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.8F, 0.1F);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static boolean castLightning(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, boolean greater, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .lightning(sp, null, greater, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castLiuguangJianying(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .liuguangJianying(sp, chargeScale);
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castYihenCi(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.adjustThoughts(sp, -10);
            var projectile = new com.example.immortal_cultivation_mod.entity.YihenCiProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.4F, 0.4F);
            sp.level().addFreshEntity(projectile);
            return true;
        }

        private static boolean castSuokongShu(ServerPlayer sp, ModAttachments.CultivationData data, float chargeScale) {
            int cost = chargedCost(suokongQiCost(data), chargeScale);
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, cost)) {
                sendNotEnoughQi(sp);
                return false;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.SuokongProjectileEntity(sp.level(), sp);
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 2.2F, 0.0F);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return true;
        }

        private static int suokongQiCost(ModAttachments.CultivationData data) {
            String realm = CultivationLevels.getLevelDef(data.cultivationLevel()).realm();
            if (CultivationLevels.REALM_JINDAN.equals(realm)
                    || CultivationLevels.REALM_YUANYING.equals(realm)
                    || CultivationLevels.REALM_HUASHEN.equals(realm)
                    || CultivationLevels.REALM_LIANXU.equals(realm)
                    || CultivationLevels.REALM_HETI.equals(realm)
                    || CultivationLevels.REALM_DACHENG.equals(realm)
                    || CultivationLevels.REALM_DUJIE.equals(realm)) {
                return 2000;
            }
            return CultivationLevels.REALM_ZHUJI.equals(realm) ? 700 : 100;
        }

        private static boolean castJinhuoXinliao(ServerPlayer sp, ModAttachments.CultivationData data,
                                                  ModSpells.SpellDef spell, float chargeScale) {
            LivingEntity target = sp.getLastHurtByMob();
            if (target == null || !target.isAlive() || target.level() != sp.level()) {
                target = null;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(
                    sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .jinhuoXinliao(sp, target, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castSuiguiXingyun(ServerPlayer sp, ModAttachments.CultivationData data,
                                                  ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(
                    sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.SuiguiXingyun.cast(
                    sp, chargeScale, projectile -> { });
            if (cast) {
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return cast;
        }

        private static boolean castZhenshanPalm(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .zhenshanPalm(sp, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castCleanse(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (sp.getActiveEffects().stream().noneMatch(effect ->
                    effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution.applyCleanse(sp);
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castQiGathering(ServerPlayer sp) {
            if (!com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution.applyQiGathering(sp)) {
                return false;
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.startQiGathering(sp);
            return true;
        }

        private static boolean castIgniteFlare(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .igniteFlare(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static void castEarthEscape(ServerPlayer sp, ModAttachments.CultivationData data) {
            if (sp.hasEffect(ModEffects.EARTH_ESCAPE)) {
                return;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, 8)) {
                sendNotEnoughQi(sp);
                return;
            }
            sp.addEffect(new MobEffectInstance(ModEffects.EARTH_ESCAPE, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            sp.teleportTo(sp.getX(), sp.getY() - 1.25D, sp.getZ());
            sp.noPhysics = true;
            sp.setNoGravity(true);
        }

        private static boolean castLingbeng(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            data = ModAttachments.getData(sp);
            int duration = SpiritRoots.effectDuration(data, spell, 20 * (45 + data.mentalAttack()));
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .applyLingbeng(sp, duration);
            sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".lingbeng_active"));
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static void castBeam(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return;
            }
            data = ModAttachments.getData(sp);
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                NativeSpellEffects.beamSpell(serverLevel, sp, SpiritRoots.effectDuration(data, spell, 20 * 10));
            }
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
        }

        private static boolean castRegeneration(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
            if (sp.getHealth() >= sp.getMaxHealth()) {
                return false;
            }
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, spell.qiCost())) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .applyRegeneration(sp);
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castFireball(ServerPlayer sp, ModAttachments.CultivationData data, ModSpells.SpellDef spell, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(spell.qiCost(), chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            boolean cast = com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution
                    .fireball(sp, null, chargeScale, projectile -> { });
            if (cast) com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return cast;
        }

        private static boolean castFrostCry(ServerPlayer sp, ModAttachments.CultivationData data, float chargeScale) {
            if (!com.example.immortal_cultivation_mod.attachment.CultivationMethods.isHantiBingqin(data.activeCultivationMethod())) {
                return false;
            }
            int maxQi = Math.max(1, data.maxQi());
            int cost = Math.max(1, (int)((long)maxQi * 5 / 100));
            if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(sp, data, chargedCost(cost, chargeScale))) {
                sendNotEnoughQi(sp);
                return false;
            }
            var projectile = new com.example.immortal_cultivation_mod.entity.FrostCryProjectileEntity(sp.level(), sp);
            projectile.getPersistentData().putFloat("ChargeScale", chargedAreaScale(chargeScale));
            projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0, 1.75f, 0.1f);
            sp.level().addFreshEntity(projectile);
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            return true;
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

        private static void handleSetAutoChannelQiOnServer(ServerboundSetAutoChannelQiPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.item.QiInfusedWeapon.setAutoChannel(sp, payload.enabled());
                }
            });
        }

        private static void handleSetQiUseParticlesOnServer(
                ServerboundSetQiUseParticlesPayload payload,
                IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects
                            .setQiUseParticlesEnabled(sp, payload.enabled());
                }
            });
        }

        private static void handleSetDroughtDomainOnServer(
                ServerboundSetDroughtDomainPayload payload,
                IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.spell.UndeadControl
                            .setDroughtDomainEnabled(sp, payload.enabled());
                }
            });
        }

        private static void handleSetFormationPlatePaymentOrderOnServer(
                ServerboundSetFormationPlatePaymentOrderPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.magicritual.item.custom.ZhenpanItem
                            .setQiFirst(sp, payload.qiFirst());
                }
            });
        }

        private static void handleSetJindanFlightOnServer(
                ServerboundSetJindanFlightPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.spell.JindanCreativeFlight.setEnabled(
                            sp, payload.enabled());
                }
            });
        }

        private static void handleAdjustJindanFlightSpeedOnServer(
                ServerboundAdjustJindanFlightSpeedPayload payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    com.example.immortal_cultivation_mod.spell.JindanCreativeFlight.adjustSpeed(
                            sp, payload.steps());
                }
            });
        }

    }
}
