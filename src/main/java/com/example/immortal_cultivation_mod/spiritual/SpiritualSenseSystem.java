package com.example.immortal_cultivation_mod.spiritual;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.compat.UltramarineClayDollCompat;
import com.example.immortal_cultivation_mod.entity.BloodApeDemonGeneralEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.entity.KongmingBeetleEntity;
import com.example.immortal_cultivation_mod.entity.FrostspineCaveBearEntity;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import com.example.immortal_cultivation_mod.entity.ThunderhornRockBadgerEntity;
import com.example.immortal_cultivation_mod.entity.SoulEntity;
import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
import com.example.immortal_cultivation_mod.spiritual.TalismanMarkSystem;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.SpiritVeinCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.poi.ModPoiTypes;
import com.example.immortal_cultivation_mod.network.SpiritualSensePayloads;
import com.example.immortal_cultivation_mod.network.SpiritualSensePayloads.Signature;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Realm-scaled spiritual perception. It deliberately sends spiritual signatures,
 * rather than Minecraft's glowing flag or exact entity data for every target.
 */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class SpiritualSenseSystem {
    public static final String TRANSLATED_NAME_PREFIX = "@translation:";
    public static final int KIND_LIVING = 0;
    public static final int KIND_HOSTILE = 1;
    public static final int KIND_CULTIVATOR = 2;
    public static final int KIND_TREASURE = 3;
    public static final int KIND_VEIN = 4;
    public static final int KIND_FORMATION = 5;
    public static final int KIND_SOUL = 6;
    public static final int KIND_FORMATION_NODE = 7;

    public static final int TIER_PRESENCE = 1;
    public static final int TIER_IDENTIFICATION = 2;
    public static final int TIER_INSIGHT = 3;

    public static final int FLAG_HOSTILE = 1;
    public static final int FLAG_MOVING = 1 << 1;
    public static final int FLAG_INJURED = 1 << 2;
    public static final int FLAG_CONCEALED = 1 << 3;
    public static final int FLAG_ACTIVE = 1 << 4;
    public static final int FLAG_CASTING = 1 << 5;
    public static final int FLAG_INTERFERENCE = 1 << 6;
    public static final int FLAG_DENSE_INTERFERENCE = 1 << 7;
    public static final int FLAG_PRESSURE = 1 << 8;
    public static final int FLAG_SENSE_ACTIVE = 1 << 9;
    public static final int FLAG_COUNTER_PRESSURE = 1 << 10;
    /** A cultivator's circulating qi is perceptible even without world particles. */
    public static final int FLAG_QI_FLOW = 1 << 11;
    /** Automatic Qi-use particles are hidden from ordinary vision. */
    public static final int FLAG_SUPPRESSED_QI_PARTICLES = 1 << 12;
    /** Owner-specific persistent seal created by a 标记符. */
    public static final int FLAG_TALISMAN_MARK = 1 << 13;

    private static final int ENTITY_SCAN_INTERVAL = 4;
    private static final int ENVIRONMENT_SCAN_INTERVAL = 20;
    private static final int MAX_ENVIRONMENT_RADIUS = 256;
    private static final String CONCEALMENT_BONUS_TAG =
            "ImmortalCultivationSpiritualConcealment";
    private static final String SENSE_BONUS_TAG =
            "ImmortalCultivationSpiritualSenseBonus";

    private static final Map<UUID, SenseState> ACTIVE = new ConcurrentHashMap<>();

    private SpiritualSenseSystem() {
    }

    public static void setActive(
            ServerPlayer player, boolean active, boolean directionalFocus) {
        if (!active) {
            stop(player, false);
            return;
        }
        if (!player.isAlive() || player.isSpectator()
                || CultivationLevels.isMortal(
                        ModAttachments.getData(player).cultivationLevel())) {
            ACTIVE.remove(player.getUUID());
            sendInactive(player, true);
            return;
        }

        ACTIVE.computeIfAbsent(player.getUUID(), ignored ->
                new SenseState(player.serverLevel().getGameTime()));
        SenseState state = ACTIVE.get(player.getUUID());
        if (state != null) {
            state.directionalFocus = directionalFocus;
            state.lastEntityScan = Long.MIN_VALUE;
            state.lastEnvironmentScan = Long.MIN_VALUE;
            sendScan(player, state);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.ACTIVATE_SENSE);
            if (CultivationLevels.getStageIndex(
                    ModAttachments.getData(player).cultivationLevel())
                    >= CultivationLevels.getStageIndex(
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY)) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                        player, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.JINDAN_SENSE);
            }
        }
    }

    public static void stop(ServerPlayer player, boolean unavailable) {
        ACTIVE.remove(player.getUUID());
        sendInactive(player, unavailable);
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    private static void sendInactive(ServerPlayer player, boolean unavailable) {
        PacketDistributor.sendToPlayer(player,
                new SpiritualSensePayloads.ClientboundSpiritualSensePayload(
                        false, unavailable, 0.0F, 0.0F,
                        0.0F, false, 0.0F, 0, false, List.of()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SenseState state = ACTIVE.get(player.getUUID());
        if (state == null) return;
        if (!player.isAlive() || player.isSpectator()
                || CultivationLevels.isMortal(
                        ModAttachments.getData(player).cultivationLevel())) {
            stop(player, true);
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now - state.lastEntityScan >= ENTITY_SCAN_INTERVAL) {
            sendScan(player, state);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ACTIVE.remove(player.getUUID()) != null) {
            sendInactive(player, false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ACTIVE.remove(player.getUUID()) != null) {
            sendInactive(player, false);
        }
    }

    private static void sendScan(ServerPlayer player, SenseState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        SenseProfile profile = profile(data.cultivationLevel());
        float age = Math.max(0.0F, now - state.startedAt);
        float focus = smoothStep(Mth.clamp(age / profile.expansionTicks(), 0.0F, 1.0F));
        float radius = Mth.lerp(focus, 3.0F, profile.maxRange());
        double basePower = baseDetectionPower(player, data, focus);
        List<Detected> detections = new ArrayList<>();

        scanLivingSignatures(player, data, profile, radius, focus,
                state.directionalFocus, basePower, detections);
        scanDistantTalismanMarks(player, radius, detections);
        scanDroppedTreasure(player, radius, focus,
                state.directionalFocus, basePower, detections);

        if (state.lastEnvironmentScan == Long.MIN_VALUE
                || now - state.lastEnvironmentScan >= ENVIRONMENT_SCAN_INTERVAL
                || radius > state.environmentRadius + 6.0F) {
            state.environment = scanEnvironment(
                    player, profile, radius, focus,
                    state.directionalFocus, basePower);
            state.environmentRadius = radius;
            state.lastEnvironmentScan = now;
        }
        detections.addAll(state.environment);

        detections.sort(Comparator.comparingDouble(Detected::score).reversed());
        int limit = Math.min(SpiritualSensePayloads.MAX_SIGNATURES,
                profile.maxTargets());
        List<Signature> signatures = detections.stream()
                .limit(limit)
                .map(Detected::signature)
                .toList();
        PacketDistributor.sendToPlayer(player,
                new SpiritualSensePayloads.ClientboundSpiritualSensePayload(
                        true, false, radius, profile.maxRange(), focus,
                        state.directionalFocus,
                        (float) basePower,
                        com.example.immortal_cultivation_mod.event.ServerEvents
                                .ambientQiValue(player),
                        com.example.immortal_cultivation_mod.event.ServerEvents
                                .isDrawingOrUsingQi(player),
                        signatures));
        state.lastEntityScan = now;
    }

    private static void scanLivingSignatures(
            ServerPlayer viewer,
            ModAttachments.CultivationData viewerData,
            SenseProfile profile,
            float radius,
            float focus,
            boolean directionalFocus,
            double basePower,
            List<Detected> output) {
        AABB bounds = viewer.getBoundingBox().inflate(radius);
        for (LivingEntity target : viewer.serverLevel().getEntitiesOfClass(
                LivingEntity.class, bounds,
                living -> living != viewer && living.isAlive()
                        && !(living instanceof ArmorStand))) {
            double distance = viewer.distanceTo(target);
            if (distance > radius) continue;
            boolean talismanMarked = TalismanMarkSystem
                    .isMarkedBy(viewer, target);
            TargetInfo info = targetInfo(target);
            int kind = livingKind(target, info);
            int viewerStage = CultivationLevels.getStageIndex(
                    viewerData.cultivationLevel());
            int targetStage = CultivationLevels.getStageIndex(info.realm());
            boolean hasCultivatorQiFlow = kind == KIND_CULTIVATOR
                    && targetStage > 0;
            Vec3 targetCenter = target.position().add(
                    0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double concealment = livingConcealment(viewer, target, info, kind);
            Interference interference = materialInterference(viewer, targetCenter);
            double detection = powerAtDistance(basePower, focus, distance, radius)
                    * detectionTargetMultiplier(kind)
                    * detectionRealmMultiplier(
                            viewerStage, targetStage, info.realm())
                    * interference.multiplier()
                    * directionalFocusMultiplier(
                            viewer, targetCenter, directionalFocus);
            double ratio = detection / Math.max(1.0D, concealment);
            int tier = detectionTier(ratio);
            // Comparable cultivators cannot erase every trace of circulating
            // qi merely by suppressing their ordinary world particles. This
            // grants only an uncertain presence, never identity or an exact
            // position, and dense spirit-blocking matter still wins.
            boolean comparableQiFlow = hasCultivatorQiFlow
                    && Math.abs(viewerStage - targetStage) <= 2
                    && (interference.flags() & FLAG_DENSE_INTERFERENCE) == 0;
            if (tier == 0 && comparableQiFlow) {
                tier = TIER_PRESENCE;
            }
            if (tier == 0 && talismanMarked) {
                tier = TIER_PRESENCE;
            }
            if (tier == 0) {
                if (ratio >= 0.45D && (target.isInvisible()
                        || !viewer.hasLineOfSight(target))) {
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.hint(
                            viewer, "sense_resisted");
                }
                continue;
            }
            if (!viewer.hasLineOfSight(target)) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                        viewer,
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.DETECT_HIDDEN_LIVING);
            }

            Vec3 perceived = talismanMarked ? targetCenter
                    : perceivedPosition(viewer, targetCenter,
                            target.getUUID().hashCode(), tier, distance);
            int flags = livingFlags(viewer, target, kind) | interference.flags();
            if (talismanMarked) flags |= FLAG_TALISMAN_MARK;
            if (hasCultivatorQiFlow) {
                flags |= FLAG_QI_FLOW;
            }
            if (ratio < 1.18D || targetStage > viewerStage) {
                flags |= FLAG_PRESSURE;
            }
            if (target instanceof ServerPlayer targetPlayer
                    && ACTIVE.containsKey(targetPlayer.getUUID())) {
                flags |= FLAG_SENSE_ACTIVE;
                ModAttachments.CultivationData targetData =
                        ModAttachments.getData(targetPlayer);
                if (CultivationLevels.getStageIndex(
                        targetData.cultivationLevel()) >= viewerStage
                        || targetData.mentalAttack()
                                >= viewerData.mentalAttack()) {
                    flags |= FLAG_COUNTER_PRESSURE;
                }
            } else if (target instanceof Mob mob
                    && mob.getTarget() == viewer
                    && targetStage >= viewerStage) {
                flags |= FLAG_COUNTER_PRESSURE;
            }
            String realm = tier >= TIER_IDENTIFICATION
                    ? approximateRealm(info.realm(), tier) : "";
            int qiPercent = tier >= TIER_INSIGHT
                    ? percentage(info.qi(), info.maxQi(), 10) : -1;
            int healthPercent = tier >= TIER_INSIGHT
                    ? percentage(Math.round(target.getHealth()),
                            Math.max(1, Math.round(target.getMaxHealth())), 5)
                    : -1;
            String artifact = tier >= TIER_INSIGHT && ratio >= 2.1D
                    ? sensedArtifact(target) : "";
            Signature signature = new Signature(
                    target.getId(), 0, kind, tier,
                    perceived.x, perceived.y, perceived.z,
                    approximateDistance(distance, tier),
                    spiritualIntensity(target, info, kind), flags,
                    tier >= TIER_INSIGHT ? sensedEntityName(viewer, target) : "",
                    realm, qiPercent, healthPercent,
                    tier >= TIER_INSIGHT ? info.elements() : "",
                    tier >= TIER_INSIGHT ? info.method() : "",
                    artifact);
            output.add(new Detected(signature,
                    scoreFor(signature, ratio, distance)));
        }
    }

    /**
     * Tracking marks remain readable beyond the cultivator's normal sense
     * radius, but only transmit the seal's position—not free insight data.
     */
    private static void scanDistantTalismanMarks(
            ServerPlayer viewer, float normalRadius,
            List<Detected> output) {
        ServerLevel level = viewer.serverLevel();
        double maximumDistanceSqr = TalismanMarkSystem.RANGE
                * TalismanMarkSystem.RANGE;
        for (UUID targetUuid : TalismanMarkSystem.targetsMarkedBy(viewer)) {
            Entity entity = level.getEntity(targetUuid);
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive() || target == viewer
                    || !TalismanMarkSystem.isMarkedBy(viewer, target)) {
                continue;
            }
            double distanceSqr = viewer.distanceToSqr(target);
            if (distanceSqr <= normalRadius * normalRadius
                    || distanceSqr > maximumDistanceSqr) {
                continue;
            }
            double distance = Math.sqrt(distanceSqr);
            TargetInfo info = targetInfo(target);
            int kind = livingKind(target, info);
            Vec3 targetCenter = target.position().add(
                    0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Interference interference = materialInterference(
                    viewer, targetCenter);
            int flags = livingFlags(viewer, target, kind)
                    | interference.flags() | FLAG_TALISMAN_MARK;
            if (kind == KIND_CULTIVATOR
                    && CultivationLevels.getStageIndex(info.realm()) > 0) {
                flags |= FLAG_QI_FLOW;
            }
            Signature signature = new Signature(
                    target.getId(), 0, kind, TIER_PRESENCE,
                    targetCenter.x, targetCenter.y, targetCenter.z,
                    approximateDistance(distance, TIER_PRESENCE),
                    spiritualIntensity(target, info, kind), flags,
                    "", "", -1, -1, "", "", "");
            // A deliberately applied seal must not be pushed out of the packet
            // by harmless nearby signatures.
            output.add(new Detected(signature, 250_000.0D - distance));
        }
    }

    private static void scanDroppedTreasure(
            ServerPlayer viewer, float radius, float focus,
            boolean directionalFocus,
            double basePower, List<Detected> output) {
        AABB bounds = viewer.getBoundingBox().inflate(radius);
        for (ItemEntity itemEntity : viewer.serverLevel().getEntitiesOfClass(
                ItemEntity.class, bounds, ItemEntity::isAlive)) {
            ItemStack stack = itemEntity.getItem();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (stack.isEmpty() || itemId == null
                    || !ImmortalCultivationMod.MODID.equals(itemId.getNamespace())
                    || itemId.getPath().endsWith("spawn_egg")) {
                continue;
            }
            double distance = viewer.distanceTo(itemEntity);
            if (distance > radius) continue;
            Vec3 targetCenter = itemEntity.position().add(0.0D, 0.25D, 0.0D);
            Interference interference = materialInterference(viewer, targetCenter);
            double detection = powerAtDistance(basePower, focus, distance, radius)
                    * SpiritualSenseMultipliers.detection(
                            SpiritualSenseMultipliers.DetectionTarget.DROPPED_TREASURE)
                    * interference.multiplier()
                    * directionalFocusMultiplier(
                            viewer, targetCenter, directionalFocus);
            double ratio = detection / 55.0D;
            int tier = detectionTier(ratio);
            if (tier == 0) continue;
            Vec3 perceived = perceivedPosition(viewer, targetCenter,
                    itemEntity.getUUID().hashCode(), tier, distance);
            Signature signature = new Signature(
                    itemEntity.getId(), 0, KIND_TREASURE, tier,
                    perceived.x, perceived.y, perceived.z,
                    approximateDistance(distance, tier),
                    Mth.clamp(0.65F + stack.getCount() * 0.025F, 0.65F, 1.6F),
                    interference.flags(),
                    tier >= TIER_INSIGHT ? stack.getHoverName().getString() : "",
                    "", -1, -1, "", "", "");
            output.add(new Detected(signature,
                    scoreFor(signature, ratio, distance)));
        }
    }

    private static List<Detected> scanEnvironment(
            ServerPlayer viewer, SenseProfile profile, float radius,
            float focus, boolean directionalFocus, double basePower) {
        ServerLevel level = viewer.serverLevel();
        int environmentRadius = Math.min(MAX_ENVIRONMENT_RADIUS,
                Math.max(4, Mth.ceil(radius)));
        List<BlockEntity> blockEntities =
                MagicCenterBlockEntity.getLoadedBlockEntitiesNear(
                        level, viewer.blockPosition(), environmentRadius);
        List<Detected> result = new ArrayList<>();
        Set<Long> veinPositions = new HashSet<>();
        List<BlockEntity> formationNodes = blockEntities.stream()
                .filter(MagicCircleBlockEntity.class::isInstance).toList();

        for (BlockEntity blockEntity : blockEntities) {
            if (!(blockEntity instanceof MagicCenterBlockEntity center)) continue;
            double distance = Vec3.atCenterOf(center.getBlockPos())
                    .distanceTo(viewer.position());
            if (distance > radius) continue;
            double concealment = (center.isActive() ? 82.0D : 118.0D)
                    + center.getRadius() * (center.isActive() ? 0.7D : 1.1D);
            Vec3 targetCenter = Vec3.atCenterOf(center.getBlockPos());
            Interference interference = materialInterference(viewer, targetCenter);
            double detection = powerAtDistance(basePower, focus, distance, radius)
                    * SpiritualSenseMultipliers.detection(
                            SpiritualSenseMultipliers.DetectionTarget.FORMATION)
                    * interference.multiplier()
                    * directionalFocusMultiplier(
                            viewer, targetCenter, directionalFocus);
            double ratio = detection / concealment;
            int tier = detectionTier(ratio);
            if (tier == 0) continue;
            BlockPos centerPos = center.getBlockPos();
            int group = centerPos.hashCode();
            Vec3 perceived = perceivedPosition(viewer,
                    targetCenter, group, tier, distance);
            int flags = (center.isActive() ? FLAG_ACTIVE : 0)
                    | interference.flags();
            Signature signature = new Signature(
                    blockId(centerPos), group, KIND_FORMATION, tier,
                    perceived.x, perceived.y, perceived.z,
                    approximateDistance(distance, tier),
                    Mth.clamp(0.75F + center.getRadius() / 30.0F,
                            0.75F, 2.4F), flags,
                    tier >= TIER_INSIGHT
                            ? center.getCenterTypeId().getPath() : "",
                    "", -1, -1, "", "", "");
            double score = scoreFor(signature, ratio, distance) + 420.0D;
            result.add(new Detected(signature, score));

            double nodeRatio = ratio
                    * SpiritualSenseMultipliers.detection(
                            SpiritualSenseMultipliers.DetectionTarget.FORMATION_NODE)
                    / Math.max(0.01D, SpiritualSenseMultipliers.detection(
                            SpiritualSenseMultipliers.DetectionTarget.FORMATION));
            if (detectionTier(nodeRatio) >= TIER_INSIGHT) {
                int nodes = 0;
                double centerRadiusSquared = center.getRadius()
                        * (double) center.getRadius();
                for (BlockEntity candidate : formationNodes) {
                    if (nodes >= 28) break;
                    BlockPos nodePos = candidate.getBlockPos();
                    if (nodePos.distToCenterSqr(
                            centerPos.getX() + 0.5D,
                            centerPos.getY() + 0.5D,
                            centerPos.getZ() + 0.5D) > centerRadiusSquared) {
                        continue;
                    }
                    nodes++;
                    Vec3 node = Vec3.atCenterOf(nodePos);
                    Signature nodeSignature = new Signature(
                            blockId(nodePos), group,
                            KIND_FORMATION_NODE, TIER_INSIGHT,
                            node.x, node.y, node.z,
                            (float) node.distanceTo(viewer.position()),
                            0.62F, flags,
                            "", "", -1, -1, "", "", "");
                    result.add(new Detected(nodeSignature, score - 8.0D - nodes));
                }
            }
        }

        for (BlockEntity blockEntity : blockEntities) {
            BlockPos position = blockEntity.getBlockPos();
            double distance = Vec3.atCenterOf(position).distanceTo(viewer.position());
            if (distance > radius) continue;
            if (blockEntity instanceof SpiritVeinCenterBlockEntity) {
                veinPositions.add(position.asLong());
                addVeinSignature(viewer, position, distance, radius,
                        focus, directionalFocus, basePower, result);
            } else if (!(blockEntity instanceof MagicCenterBlockEntity)
                    && !(blockEntity instanceof MagicCircleBlockEntity)
                    && isSpiritualTreasure(blockEntity)) {
                addTreasureBlockSignature(viewer, blockEntity, distance,
                        radius, focus, directionalFocus, basePower, result);
            }
        }

        level.getPoiManager().getInRange(
                        holder -> holder.value()
                                == ModPoiTypes.SPIRIT_VEIN_CENTER_POI.get(),
                        viewer.blockPosition(), environmentRadius,
                        Occupancy.ANY)
                .limit(Math.max(8, profile.maxTargets() / 3L))
                .forEach(record -> {
                    BlockPos position = record.getPos();
                    if (!veinPositions.add(position.asLong())) return;
                    double distance = Vec3.atCenterOf(position)
                            .distanceTo(viewer.position());
                    if (distance <= radius) {
                        addVeinSignature(viewer, position, distance, radius,
                                focus, directionalFocus, basePower, result);
                    }
                });
        addEnchantedClayDollSignatures(
                viewer, environmentRadius, radius, result);
        return result;
    }

    /** Enchanted Ultramarine dolls have no block entity, so add their saved
     * spiritual signatures explicitly without changing their normal model. */
    private static void addEnchantedClayDollSignatures(
            ServerPlayer viewer, int environmentRadius, float senseRadius,
            List<Detected> output) {
        ServerLevel level = viewer.serverLevel();
        for (BlockPos position : UltramarineClayDollCompat.enchantedDollsNear(
                level, viewer.blockPosition(), environmentRadius)) {
            Vec3 center = Vec3.atCenterOf(position);
            double distance = center.distanceTo(viewer.position());
            if (distance > senseRadius) {
                continue;
            }
            BlockState state = level.getBlockState(position);
            Signature signature = new Signature(
                    blockId(position), 0, KIND_TREASURE,
                    TIER_IDENTIFICATION,
                    center.x, center.y, center.z,
                    approximateDistance(distance, TIER_IDENTIFICATION),
                    1.15F, FLAG_ACTIVE,
                    TRANSLATED_NAME_PREFIX
                            + state.getBlock().getDescriptionId(),
                    "", -1, -1, "", "", "");
            // The requested glow is a defining property of the enchantment,
            // so nearby combat signatures must not push it out of the packet.
            output.add(new Detected(signature, 100_000.0D - distance));
        }
    }

    private static void addVeinSignature(
            ServerPlayer viewer, BlockPos position, double distance,
            float radius, float focus, boolean directionalFocus,
            double basePower,
            List<Detected> output) {
        Vec3 targetCenter = Vec3.atCenterOf(position);
        Interference interference = materialInterference(viewer, targetCenter);
        double detection = powerAtDistance(basePower, focus, distance, radius)
                * SpiritualSenseMultipliers.detection(
                        SpiritualSenseMultipliers.DetectionTarget.SPIRIT_VEIN)
                * interference.multiplier()
                * directionalFocusMultiplier(
                        viewer, targetCenter, directionalFocus);
        double ratio = detection / 42.0D;
        int tier = detectionTier(ratio);
        if (tier == 0) return;
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                viewer, com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.DETECT_VEIN);
        // A vein is a fixed environmental source. Its information can remain
        // vague at lower tiers, but its HUD anchor must stay on the real core
        // instead of inheriting the living-target positional uncertainty.
        Vec3 perceived = targetCenter;
        Signature signature = new Signature(
                blockId(position), position.hashCode(), KIND_VEIN, tier,
                perceived.x, perceived.y, perceived.z,
                approximateDistance(distance, tier), 1.65F,
                FLAG_ACTIVE | interference.flags(),
                tier >= TIER_INSIGHT ? "spirit_vein_center" : "",
                "", -1, -1, "", "", "");
        output.add(new Detected(signature,
                scoreFor(signature, ratio, distance) + 360.0D));
    }

    private static void addTreasureBlockSignature(
            ServerPlayer viewer, BlockEntity blockEntity, double distance,
            float radius, float focus, boolean directionalFocus,
            double basePower,
            List<Detected> output) {
        Vec3 targetCenter = Vec3.atCenterOf(blockEntity.getBlockPos());
        Interference interference = materialInterference(viewer, targetCenter);
        double detection = powerAtDistance(basePower, focus, distance, radius)
                * SpiritualSenseMultipliers.detection(
                        SpiritualSenseMultipliers.DetectionTarget.SPIRITUAL_TREASURE_BLOCK)
                * interference.multiplier()
                * directionalFocusMultiplier(
                        viewer, targetCenter, directionalFocus);
        double ratio = detection / 68.0D;
        int tier = detectionTier(ratio);
        if (tier == 0) return;
        BlockPos position = blockEntity.getBlockPos();
        Vec3 perceived = perceivedPosition(viewer, targetCenter,
                position.hashCode(), tier, distance);
        Signature signature = new Signature(
                blockId(position), 0, KIND_TREASURE, tier,
                perceived.x, perceived.y, perceived.z,
                approximateDistance(distance, tier), 1.05F,
                interference.flags(),
                tier >= TIER_INSIGHT
                        ? blockEntity.getBlockState().getBlock()
                                .getName().getString() : "",
                "", -1, -1, "", "", "");
        output.add(new Detected(signature,
                scoreFor(signature, ratio, distance) + 170.0D));
    }

    private static boolean isSpiritualTreasure(BlockEntity blockEntity) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                blockEntity.getBlockState().getBlock());
        if (id == null || !ImmortalCultivationMod.MODID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.contains("ding")
                || path.contains("restriction")
                || path.contains("stele")
                || path.contains("motherstone")
                || path.contains("crystal")
                || path.contains("jade")
                || path.contains("ore")
                || path.contains("spirit")
                || path.contains("heart");
    }

    private static TargetInfo targetInfo(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            int maxQi = Math.max(1,
                    data.maxQi());
            return new TargetInfo(data.cultivationLevel(), data.qi(), maxQi,
                    String.join("/", data.spiritRoots()),
                    data.activeCultivationMethod(), data.mentalAttack());
        }
        if (target instanceof CultivationNpc npc) {
            return new TargetInfo(npc.npcRealm(), npc.npcQi(), npc.npcMaxQi(),
                    npc.npcSpiritRoots(), npc.npcCultivationMethod(), 0);
        }
        String beastRealm = switch (target) {
            case com.example.immortal_cultivation_mod.entity.RealmBeast beast -> beast.beastCultivationLevel();
            case EscapedYuanyingCoreEntity core -> com.example.immortal_cultivation_mod.entity.BeastRealms.level(core.coreRealm());
            case JindanEntity ignored ->
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
            default -> "";
        };
        return new TargetInfo(beastRealm, -1, -1, "", "", 0);
    }

    private static int livingKind(LivingEntity target, TargetInfo info) {
        if (target instanceof SoulEntity
                || target instanceof EscapedYuanyingCoreEntity
                || target instanceof JindanEntity) {
            return KIND_SOUL;
        }
        if (target instanceof Player || target instanceof CultivationNpc) {
            return KIND_CULTIVATOR;
        }
        return target instanceof Enemy ? KIND_HOSTILE : KIND_LIVING;
    }

    /**
     * Default entity names must be translated by the viewing client. Resolving
     * them on a dedicated server would force its language onto every player.
     * Player names and deliberately assigned NPC names remain literal.
     */
    private static String sensedEntityName(
            ServerPlayer viewer, LivingEntity target) {
        if (target instanceof Player) {
            return target.getName().getString();
        }
        if (target.hasCustomName() && target.getCustomName() != null) {
            return target.getCustomName().getString();
        }
        return TRANSLATED_NAME_PREFIX + target.getType().getDescriptionId();
    }

    private static double livingConcealment(
            ServerPlayer viewer, LivingEntity target,
            TargetInfo info, int kind) {
        if (target instanceof ServerPlayer hidingPlayer) {
            return playerHidingValue(viewer, hidingPlayer);
        }
        int stage = CultivationLevels.getStageIndex(info.realm());
        double concealment = switch (kind) {
            case KIND_CULTIVATOR -> 58.0D + stage * 30.0D
                    + info.mentalResistance() * 15.0D;
            case KIND_SOUL -> 76.0D + stage * 26.0D;
            case KIND_HOSTILE -> 58.0D
                    + Math.log10(Math.max(2.0D, target.getMaxHealth())) * 14.0D;
            default -> 36.0D
                    + Math.log10(Math.max(2.0D, target.getMaxHealth())) * 5.0D;
        };
        if (target.isInvisible()) concealment *= SpiritualSenseMultipliers.hiding(
                SpiritualSenseMultipliers.HidingSource.INVISIBLE);
        if (target.isCrouching()) concealment *= SpiritualSenseMultipliers.hiding(
                SpiritualSenseMultipliers.HidingSource.SNEAKING);
        if (!target.level().canSeeSky(target.blockPosition())) {
            concealment *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.UNDER_COVER);
        }
        if (target.isInWater()) concealment *= SpiritualSenseMultipliers.hiding(
                SpiritualSenseMultipliers.HidingSource.IN_WATER);
        if (disguiseMaskConcealsFrom(viewer, target)) {
            concealment *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.DISGUISE_MASK);
        }
        if (com.example.immortal_cultivation_mod.event.ServerEvents
                .isDrawingOrUsingQi(target)) {
            concealment *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.ACTIVELY_USING_QI);
        }
        if (target.getDeltaMovement().lengthSqr() > 0.08D
                || target.swinging || target.isUsingItem()
                || target.getLastHurtByMob() == viewer) {
            concealment *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.EXPOSED_BY_ACTION);
        }
        double persistentBonus = target.getPersistentData()
                .getDouble(CONCEALMENT_BONUS_TAG);
        return concealment * Math.max(0.1D, 1.0D + persistentBonus
                * SpiritualSenseMultipliers.PERSISTENT_BONUS_PER_POINT);
    }

    /** Current raw detection value before target, distance, and realm scaling. */
    public static double playerDetectionValue(ServerPlayer player) {
        return baseDetectionPower(
                player, ModAttachments.getData(player), 1.0F);
    }

    /**
     * Current hiding value against a specific detector. Mental stat forms the
     * base; active hiding sources, realm difference, and luck then multiply it.
     */
    public static double playerHidingValue(
            ServerPlayer detector, ServerPlayer hidingPlayer) {
        ModAttachments.CultivationData detectorData =
                ModAttachments.getData(detector);
        ModAttachments.CultivationData hidingData =
                ModAttachments.getData(hidingPlayer);
        double hiding = SpiritualSenseMultipliers.HIDING_BASE
                + Math.max(0, hidingData.mentalAttack())
                * SpiritualSenseMultipliers.HIDING_PER_MENTAL;

        int detectorStage = CultivationLevels.getStageIndex(
                detectorData.cultivationLevel());
        int hidingStage = CultivationLevels.getStageIndex(
                hidingData.cultivationLevel());
        hiding *= SpiritualSenseMultipliers.realmMultiplier(
                hidingStage - detectorStage,
                SpiritualSenseMultipliers.HIDING_REALM_MULTIPLIER_PER_STAGE);
        hiding *= SpiritualSenseMultipliers.luckMultiplier(hidingData.luck());

        if (hidingPlayer.isInvisible()) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.INVISIBLE);
        }
        if (hidingPlayer.isCrouching()) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.SNEAKING);
        }
        if (!hidingPlayer.level().canSeeSky(hidingPlayer.blockPosition())) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.UNDER_COVER);
        }
        if (hidingPlayer.isInWater()) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.IN_WATER);
        }
        if (disguiseMaskConcealsFrom(detector, hidingPlayer)) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.DISGUISE_MASK);
        }
        if (com.example.immortal_cultivation_mod.item.UtilityTalismanItem
                .isMuffleActive(hidingPlayer)) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.MUFFLE_TALISMAN);
        }
        if (com.example.immortal_cultivation_mod.event.ServerEvents
                .isDrawingOrUsingQi(hidingPlayer)) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.ACTIVELY_USING_QI);
        }
        if (hidingPlayer.getDeltaMovement().lengthSqr() > 0.08D
                || hidingPlayer.swinging || hidingPlayer.isUsingItem()
                || hidingPlayer.getLastHurtByMob() == detector) {
            hiding *= SpiritualSenseMultipliers.hiding(
                    SpiritualSenseMultipliers.HidingSource.EXPOSED_BY_ACTION);
        }
        double persistentBonus = hidingPlayer.getPersistentData()
                .getDouble(CONCEALMENT_BONUS_TAG);
        return Math.max(1.0D, hiding * Math.max(0.1D, 1.0D
                + persistentBonus
                * SpiritualSenseMultipliers.PERSISTENT_BONUS_PER_POINT));
    }

    private static double detectionTargetMultiplier(int kind) {
        SpiritualSenseMultipliers.DetectionTarget target = switch (kind) {
            case KIND_HOSTILE ->
                    SpiritualSenseMultipliers.DetectionTarget.HOSTILE;
            case KIND_CULTIVATOR ->
                    SpiritualSenseMultipliers.DetectionTarget.CULTIVATOR;
            case KIND_SOUL -> SpiritualSenseMultipliers.DetectionTarget.SOUL;
            default -> SpiritualSenseMultipliers.DetectionTarget.NORMAL_LIVING;
        };
        return SpiritualSenseMultipliers.detection(target);
    }

    private static double detectionRealmMultiplier(
            int detectorStage, int targetStage, String targetRealm) {
        if (targetRealm == null || targetRealm.isBlank()) return 1.0D;
        return SpiritualSenseMultipliers.realmMultiplier(
                detectorStage - targetStage,
                SpiritualSenseMultipliers.DETECTION_REALM_MULTIPLIER_PER_STAGE);
    }

    private static double baseDetectionPower(
            ServerPlayer player,
            ModAttachments.CultivationData data,
            float focus) {
        double power = SpiritualSenseMultipliers.DETECTION_BASE
                + Math.max(0, data.mentalAttack())
                * SpiritualSenseMultipliers.DETECTION_PER_MENTAL;
        String methodId = data.activeCultivationMethod();
        if (CultivationMethods.get(methodId) != null) {
            int proficiency = Math.max(0, data.methodProficiency(methodId));
            power *= 1.0D + Math.min(
                    SpiritualSenseMultipliers.METHOD_PROFICIENCY_MAX_BONUS,
                    Math.log10(proficiency + 1.0D)
                            * SpiritualSenseMultipliers.METHOD_PROFICIENCY_LOG_SCALE);
        }
        if (CultivationMethods.CLEAR_HEART.equals(methodId)) {
            power *= SpiritualSenseMultipliers.CLEAR_HEART_METHOD;
        }
        power *= SpiritualSenseMultipliers.EXPANSION_FOCUS_MIN
                + focus * SpiritualSenseMultipliers.EXPANSION_FOCUS_RANGE;
        double persistentBonus = player.getPersistentData()
                .getDouble(SENSE_BONUS_TAG);
        return power * Math.max(0.1D, 1.0D + persistentBonus
                * SpiritualSenseMultipliers.PERSISTENT_BONUS_PER_POINT);
    }

    /**
     * Sneaking while holding the sense key compresses awareness into the view
     * direction. The server applies the gain, so the client cannot manufacture
     * more detailed signatures by drawing a marker locally.
     */
    private static double directionalFocusMultiplier(
            ServerPlayer viewer, Vec3 target, boolean directionalFocus) {
        if (!directionalFocus) return 1.0D;
        Vec3 direction = target.subtract(viewer.getEyePosition());
        if (direction.lengthSqr() < 1.0E-6D) {
            return SpiritualSenseMultipliers.DIRECTIONAL_AT_SAME_POINT;
        }
        double alignment = viewer.getLookAngle().dot(direction.normalize());
        if (alignment >= 0.94D) {
            return SpiritualSenseMultipliers.DIRECTIONAL_TIGHT_AIM;
        }
        if (alignment >= 0.80D) {
            return SpiritualSenseMultipliers.DIRECTIONAL_LOOSE_AIM;
        }
        return SpiritualSenseMultipliers.DIRECTIONAL_OUTSIDE_VIEW;
    }

    /**
     * Spiritual perception can pass through matter, but matter weakens it. A
     * first solid obstruction supplies a cheap, deterministic approximation of
     * wall density without walking hundreds of blocks or loading new chunks.
     */
    private static Interference materialInterference(
            ServerPlayer viewer, Vec3 target) {
        Vec3 origin = viewer.getEyePosition();
        if (origin.distanceToSqr(target) < 1.0D) return Interference.NONE;
        LoadedSightView terrain = new LoadedSightView(viewer.serverLevel());
        BlockHitResult hit = terrain.clip(new ClipContext(
                origin, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, viewer));
        if (hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(target) < 0.36D) {
            return Interference.NONE;
        }

        BlockPos position = hit.getBlockPos();
        BlockState state = terrain.getBlockState(position);
        if (state.isAir()) return Interference.NONE;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath();
        boolean spiritBlocking = id != null
                && ImmortalCultivationMod.MODID.equals(id.getNamespace())
                && (path.contains("restriction")
                        || path.contains("forcefield")
                        || path.contains("force_field")
                        || path.contains("magic_circle")
                        || path.contains("magic_center"));
        float resistance = state.getBlock().getExplosionResistance();
        float destroySpeed = state.getDestroySpeed(terrain, position);
        boolean dense = spiritBlocking || destroySpeed < 0.0F
                || resistance >= 6.0F;
        int flags = FLAG_INTERFERENCE
                | (dense ? FLAG_DENSE_INTERFERENCE : 0);
        return new Interference(
                spiritBlocking
                        ? SpiritualSenseMultipliers.SPIRIT_BLOCKING_INTERFERENCE
                        : dense
                                ? SpiritualSenseMultipliers.DENSE_SOLID_INTERFERENCE
                                : SpiritualSenseMultipliers.NORMAL_SOLID_INTERFERENCE,
                flags);
    }

    private static double powerAtDistance(
            double basePower, float focus, double distance, float radius) {
        double normalized = Mth.clamp(distance / Math.max(1.0F, radius), 0.0D, 1.0D);
        double efficiency = SpiritualSenseMultipliers.DISTANCE_EFFICIENCY_MIN
                + SpiritualSenseMultipliers.DISTANCE_EFFICIENCY_RANGE
                * (1.0D - Math.pow(normalized,
                        SpiritualSenseMultipliers.DISTANCE_CURVE_POWER));
        double focusMultiplier = SpiritualSenseMultipliers.SCAN_FOCUS_MIN
                + focus * SpiritualSenseMultipliers.SCAN_FOCUS_RANGE;
        return basePower * efficiency * focusMultiplier;
    }

    private static int detectionTier(double ratio) {
        if (ratio < 0.72D) return 0;
        if (ratio < 1.05D) return TIER_PRESENCE;
        if (ratio < 1.65D) return TIER_IDENTIFICATION;
        return TIER_INSIGHT;
    }

    private static Vec3 perceivedPosition(
            ServerPlayer viewer, Vec3 target, int seed,
            int tier, double distance) {
        Vec3 origin = viewer.getEyePosition();
        if (tier == TIER_PRESENCE) {
            Vec3 delta = target.subtract(origin);
            double horizontal = Math.max(0.001D,
                    Math.sqrt(delta.x * delta.x + delta.z * delta.z));
            double angle = Math.atan2(delta.z, delta.x);
            double snapped = Math.round(angle / (Math.PI / 4.0D))
                    * (Math.PI / 4.0D);
            return origin.add(
                    Math.cos(snapped) * distance,
                    Mth.clamp(delta.y, -4.0D, 4.0D),
                    Math.sin(snapped) * distance)
                    .add(0.0D, horizontal * 0.0D, 0.0D);
        }
        if (tier == TIER_IDENTIFICATION) {
            double angle = Math.floorMod(seed * 1103515245L + 12345L,
                    3600L) * Math.PI / 1800.0D;
            double error = Mth.clamp(distance * 0.045D, 0.65D, 3.25D);
            double vertical = (Math.floorMod(seed >>> 5, 9) - 4) * 0.12D;
            return target.add(Math.cos(angle) * error, vertical,
                    Math.sin(angle) * error);
        }
        return target;
    }

    private static float approximateDistance(double distance, int tier) {
        double step = tier == TIER_PRESENCE ? 5.0D
                : tier == TIER_IDENTIFICATION ? 2.0D : 0.5D;
        return (float) (Math.max(step, Math.round(distance / step) * step));
    }

    private static boolean disguiseMaskConcealsFrom(
            ServerPlayer viewer, LivingEntity target) {
        ItemStack mask = DisguiseMaskItem.getEquippedMask(target);
        return !mask.isEmpty()
                && !DisguiseMaskItem.canQiSenseReveal(
                        ModAttachments.getData(viewer).cultivationLevel(),
                        mask);
    }

    private static int livingFlags(
            ServerPlayer viewer, LivingEntity target, int kind) {
        int flags = kind == KIND_HOSTILE ? FLAG_HOSTILE : 0;
        if (target.getDeltaMovement().lengthSqr() > 0.012D) flags |= FLAG_MOVING;
        if (target.getHealth() < target.getMaxHealth() * 0.72F) flags |= FLAG_INJURED;
        if (target.isInvisible() || target.isCrouching()
                || disguiseMaskConcealsFrom(viewer, target)) flags |= FLAG_CONCEALED;
        if (target.isUsingItem()
                || target instanceof ServerPlayer player
                        && CombatStateManager.isPreparingSpell(player)
                || target instanceof CultivationNpc npc
                && npc.npcCastingTicks() > 0) flags |= FLAG_CASTING;
        if (com.example.immortal_cultivation_mod.event.ServerEvents
                .isDrawingOrUsingQi(target)) {
            flags |= FLAG_ACTIVE;
            if (target instanceof ServerPlayer player
                    && !MethodQiParticleEffects.showQiUseParticles(player)) {
                flags |= FLAG_SUPPRESSED_QI_PARTICLES;
            }
        }
        return flags;
    }

    private static float spiritualIntensity(
            LivingEntity target, TargetInfo info, int kind) {
        int realm = CultivationLevels.getRealmIndex(info.realm());
        float healthWeight = (float) Math.log10(
                Math.max(2.0D, target.getMaxHealth())) * 0.14F;
        float kindWeight = kind == KIND_CULTIVATOR || kind == KIND_SOUL
                ? realm * 0.17F : kind == KIND_HOSTILE ? 0.25F : 0.0F;
        return Mth.clamp(0.55F + healthWeight + kindWeight, 0.55F, 2.8F);
    }

    private static String sensedArtifact(LivingEntity target) {
        ItemStack stack = !target.getMainHandItem().isEmpty()
                ? target.getMainHandItem() : target.getOffhandItem();
        if (stack.isEmpty()) return "";
        return stack.getHoverName().getString();
    }

    private static String approximateRealm(String fullRealm, int tier) {
        if (fullRealm == null || fullRealm.isBlank()) return "";
        return tier >= TIER_INSIGHT
                ? fullRealm
                : CultivationLevels.getLevelDef(fullRealm).realm();
    }

    private static int percentage(int value, int maximum, int step) {
        if (value < 0 || maximum <= 0) return -1;
        int percent = Mth.clamp(Math.round(value * 100.0F / maximum), 0, 100);
        return Mth.clamp(Math.round(percent / (float) step) * step, 0, 100);
    }

    private static double scoreFor(
            Signature signature, double ratio, double distance) {
        double kindPriority = switch (signature.kind()) {
            case KIND_CULTIVATOR -> 520.0D;
            case KIND_HOSTILE -> 480.0D;
            case KIND_SOUL -> 450.0D;
            case KIND_FORMATION -> 410.0D;
            case KIND_VEIN -> 380.0D;
            case KIND_TREASURE -> 260.0D;
            default -> 80.0D;
        };
        if ((signature.flags() & FLAG_CASTING) != 0) kindPriority += 220.0D;
        if ((signature.flags() & FLAG_TALISMAN_MARK) != 0) {
            kindPriority += 200_000.0D;
        }
        return signature.tier() * 10_000.0D + kindPriority
                + Math.min(4.0D, ratio) * 100.0D - distance;
    }

    private static int blockId(BlockPos position) {
        int hash = position.hashCode();
        return hash == Integer.MIN_VALUE ? Integer.MIN_VALUE + 1
                : -Math.max(1, Math.abs(hash));
    }

    private static SenseProfile profile(String cultivationLevel) {
        int realm = CultivationLevels.getRealmIndex(cultivationLevel);
        int stageIndex = CultivationLevels.getStageIndex(cultivationLevel);
        int stage = stageIndex <= 0 ? 0 : (stageIndex - 1) % 3;
        float maxRange = switch (realm) {
            case 1 -> new float[]{8.0F, 12.0F, 15.0F}[stage];
            case 2 -> new float[]{20.0F, 28.0F, 35.0F}[stage];
            case 3 -> new float[]{40.0F, 52.0F, 65.0F}[stage];
            case 4 -> new float[]{80.0F, 100.0F, 120.0F}[stage];
            case 5 -> new float[]{150.0F, 180.0F, 210.0F}[stage];
            case 6 -> new float[]{240.0F, 270.0F, 300.0F}[stage];
            case 7 -> new float[]{340.0F, 380.0F, 420.0F}[stage];
            case 8 -> new float[]{480.0F, 540.0F, 600.0F}[stage];
            case 9 -> new float[]{700.0F, 800.0F, 900.0F}[stage];
            default -> 0.0F;
        };
        int expansionTicks = Math.max(7, 34 - realm * 4 - stage * 2);
        int maxTargets = Math.min(SpiritualSensePayloads.MAX_SIGNATURES,
                8 + realm * 10 + stage * 4);
        return new SenseProfile(maxRange, expansionTicks, maxTargets);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private record TargetInfo(
            String realm, int qi, int maxQi,
            String elements, String method,
            int mentalResistance) {
    }

    private record SenseProfile(
            float maxRange, int expansionTicks, int maxTargets) {
    }

    private record Detected(Signature signature, double score) {
    }

    private record Interference(double multiplier, int flags) {
        private static final Interference NONE = new Interference(1.0D, 0);
    }

    private static final class SenseState {
        private final long startedAt;
        private boolean directionalFocus;
        private long lastEntityScan = Long.MIN_VALUE;
        private long lastEnvironmentScan = Long.MIN_VALUE;
        private float environmentRadius;
        private List<Detected> environment = List.of();

        private SenseState(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
