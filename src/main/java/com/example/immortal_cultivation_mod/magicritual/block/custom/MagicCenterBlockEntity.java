package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterRitualDefinition;
import com.example.immortal_cultivation_mod.magicritual.center.CenterRitualManager;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterType;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.TeleportCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.ui.CenterFuelMenu;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class MagicCenterBlockEntity extends BlockEntity implements MenuProvider {
    private static final int DATA_VERSION = 3;
    private static final long LEGACY_ENERGY_PER_STONE = 1_200L;
    private static final int LEGACY_COMBO_MIGRATION_VERSION = 1;
    private static final int CACHE_VALIDATION_TICKS = 100;
    public static final int MAX_CONTROLLER_PLAYERS = 16;
    public static final int MAX_BLACKLISTED_PLAYERS = 64;
    public static final int MIN_RADIUS = 3;
    /** Large formations support every odd radius through 303 blocks. */
    public static final int MAX_RADIUS = 303;
    public static final int GLYPH_RENDER_MARGIN = 8 * 16;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static volatile double[][] FOG_POINTS = new double[0][];
    public static volatile double[][] INVERTED_POINTS = new double[0][];
    public static volatile double[][] FORCEFIELD_POINTS = new double[0][];
    public static volatile double[][] NORMAL_POINTS = new double[0][];

    private ResourceLocation centerTypeId = MagicCenterTypeRegistry.BLANK;
    private ResourceLocation unknownRecoveryType;
    private int radius = 5;
    private long energy;
    private long fuelCostRemainder;
    private float barrierHealth;
    private boolean enabled = true;
    private boolean active;
    private boolean particlesEnabled = true;
    private int glyphColor = 0xFFFFFF;
    private UUID ownerUuid;
    private UUID centerId = UUID.randomUUID();
    private ResourceLocation teleportLinkedDimension;
    private BlockPos teleportLinkedPos;
    private UUID teleportLinkedCenterId;
    /** Null means two-way; otherwise this is the only endpoint allowed to send. */
    private UUID teleportSourceCenterId;
    private UUID teleportChannelPlayer;
    private long teleportChannelStartTime = -1L;
    private int teleportChannelDurationTicks;
    private float teleportChannelStartHealth;
    private int teleportChannelObservedHurtTime;
    private final Map<UUID, String> controllerPlayers = new HashMap<>();
    private final Set<UUID> excludedPlayers = new HashSet<>();
    private final Set<String> legacyExcludedNames = new HashSet<>();
    private final Set<UUID> authorizedPlayersInside = new HashSet<>();
    private boolean authorizedPlayerInside;
    private long authorizedEntryGameTime = -1L;
    private boolean allowHostileMobs = true;
    private boolean allowPassiveMobs = true;
    private String customName = "";
    private CircleCounts passiveCircleCounts = CircleCounts.ZERO;
    private boolean passiveCacheDirty = true;
    private long lastPassiveScan = Long.MIN_VALUE;
    private PendingTransformation pendingTransformation;
    private List<ConsumedCircle> transformationCircles = List.of();
    private boolean legacyRadiusMigrated;
    private int legacyComboMigrationVersion;
    private boolean temporary;
    private long temporaryExpiresAt = -1L;
    private boolean suppressContentsDrop;
    private boolean suppressOctagonDrop;

    private final ItemStackHandler fuelSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            MagicCenterBlockEntity.this.setChangedAndSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ModItems.isSpiritStone(stack.getItem());
        }
    };

    private final ItemStackHandler reforgeWeaponSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            MagicCenterBlockEntity.this.setChangedAndSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MagicCenterTypeRegistry.REFORGE_BLADE.equals(centerTypeId)
                    && stack.isDamageableItem();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final ItemStackHandler octagonGrid = new ItemStackHandler(
            CenterFuelMenu.OCTAGON_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            MagicCenterBlockEntity.this.setChangedAndSync();
        }
    };

    public MagicCenterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_CENTER_BE.get(), pos, state);
        if (state.hasProperty(MagicCenterBlock.RADIUS)) {
            radius = normalizeRadius(state.getValue(MagicCenterBlock.RADIUS));
        }
    }

    public static void loadPatterns(ResourceManager manager) {
        FOG_POINTS = MagicPatternLoader.load(manager,
                ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/fog.json"));
        INVERTED_POINTS = MagicPatternLoader.load(manager,
                ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/inverted.json"));
        FORCEFIELD_POINTS = MagicPatternLoader.load(manager,
                ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/forcefield.json"));
        NORMAL_POINTS = MagicPatternLoader.load(manager,
                ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/normal.json"));
    }

    public static void spawnParticles(Level level, BlockPos pos, BlockState state,
                                      MagicCenterBlockEntity center, double[][] points,
                                      Vector3f color) {
        // The block entity renderer owns all projected-glyph rendering.
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            MagicCenterBlockEntity center) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (center.temporary && serverLevel.getGameTime() >= center.temporaryExpiresAt) {
            center.removeWithoutDrops();
            return;
        }
        center.recoverPendingTransformation();
        center.migrateLegacyRadius(state);
        center.migrateLegacyComboIfNeeded(serverLevel);
        center.updateAuthorizedPlayerPresence(serverLevel);

        MagicCenterType type = center.getCenterType();
        CenterContext context = new CenterContext(serverLevel, pos, state, center);
        if (!center.temporary && serverLevel.getGameTime() % 20L == 0L) {
            CenterFuelController.pullFromAdjacentInventories(context);
        }
        if (MagicCenterTypeRegistry.BLANK.equals(type.id())) {
            center.refreshPassiveCircleCacheIfNeeded(false);
        }

        int baseCostPerMinute = Math.max(0, type.behavior().getFuelCostPerMinute(context));
        int costPerMinute = center.applyFormationFlagFuelMultiplier(baseCostPerMinute);
        boolean wasActive = center.active;
        boolean teleportCenter = MagicCenterTypeRegistry.TELEPORT.equals(type.id());
        boolean teleportChannelPending = teleportCenter && center.hasTeleportChannel();
        boolean automaticTemporaryTeleport = teleportCenter && center.temporary
                && center.hasTeleportLink();
        center.active = teleportCenter
                ? center.enabled && center.hasFuel()
                        && center.hasPlayerInsideRadius(serverLevel)
                : center.temporary
                        ? center.enabled
                        : CenterFuelController.consumeActiveTick(context, costPerMinute);
        if (center.active || teleportChannelPending || automaticTemporaryTeleport) {
            type.behavior().serverTick(context);
        }
        CenterEffectSupport.track(center);
        if (wasActive != center.active) {
            center.setChangedAndSync();
        } else if (serverLevel.getGameTime() % 20L == 0L) {
            center.setChanged();
        }
    }

    private void updateAuthorizedPlayerPresence(ServerLevel serverLevel) {
        Set<UUID> currentPlayers = new HashSet<>();
        Vec3 center = centerPoint();
        double radiusSquared = radius * radius;
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isAlive() || player.getY() < worldPosition.getY()
                    || !CenterTargetPolicy.isAuthorizedPlayer(this, player)) {
                continue;
            }
            double dx = player.getX() - center.x;
            double dz = player.getZ() - center.z;
            if (dx * dx + dz * dz <= radiusSquared) {
                currentPlayers.add(player.getUUID());
            }
        }

        boolean playerEntered = currentPlayers.stream()
                .anyMatch(uuid -> !authorizedPlayersInside.contains(uuid));
        boolean playerInside = !currentPlayers.isEmpty();
        boolean presenceChanged = playerInside != authorizedPlayerInside;
        authorizedPlayersInside.clear();
        authorizedPlayersInside.addAll(currentPlayers);
        authorizedPlayerInside = playerInside;
        if (playerEntered) {
            authorizedEntryGameTime = serverLevel.getGameTime();
        }
        if (playerEntered || presenceChanged) {
            setChangedAndSync();
        }
    }

    private boolean hasPlayerInsideRadius(ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.players()) {
            if (player.isAlive() && !player.isSpectator() && isInsideRadius(player)) {
                return true;
            }
        }
        return false;
    }

    private void migrateLegacyRadius(BlockState state) {
        if (!legacyRadiusMigrated) {
            legacyRadiusMigrated = true;
            if (state.hasProperty(MagicCenterBlock.RADIUS) && radius == 5) {
                radius = normalizeRadius(state.getValue(MagicCenterBlock.RADIUS));
            }
            passiveCacheDirty = true;
            setChanged();
        }
    }

    private void migrateLegacyComboIfNeeded(ServerLevel serverLevel) {
        if (legacyComboMigrationVersion >= LEGACY_COMBO_MIGRATION_VERSION || !isBlank()) {
            return;
        }
        int minChunkX = Math.floorDiv(worldPosition.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(worldPosition.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(worldPosition.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(worldPosition.getZ() + radius, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!serverLevel.hasChunk(chunkX, chunkZ)) {
                    return;
                }
            }
        }

        List<LegacyComboMatch> matches = new ArrayList<>();
        for (BlockEntity blockEntity : getLoadedBlockEntitiesNear(serverLevel, worldPosition, radius)) {
            if (!(blockEntity instanceof ComboBlockEntity)
                    || Math.abs(blockEntity.getBlockPos().getY() - worldPosition.getY()) > 3) {
                continue;
            }
            BlockPos comboPos = blockEntity.getBlockPos();
            BlockState comboState = blockEntity.getBlockState();
            if (!comboState.is(ModBlocks.COMBO_BLOCK.get())) {
                continue;
            }
            LegacyComboMatch match = findLegacyComboMatch(serverLevel, comboPos,
                    comboState.getValue(ComboBlock.COMBO_TYPE));
            if (match != null) {
                matches.add(match);
            }
        }

        legacyComboMigrationVersion = LEGACY_COMBO_MIGRATION_VERSION;
        if (matches.size() > 1) {
            LOGGER.warn("Magic Center at {} has {} old combo structures; leaving it blank for manual repair",
                    worldPosition, matches.size());
            setChanged();
            return;
        }
        if (matches.isEmpty()) {
            setChanged();
            return;
        }

        LegacyComboMatch match = matches.getFirst();
        beginPendingTransformation(match.outputType(), match.circlePositions());
        serverLevel.setBlock(match.comboPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos circlePos : match.circlePositions()) {
            if (serverLevel.getBlockState(circlePos).is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                serverLevel.setBlock(circlePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        completeTransformation(match.outputType());
        LOGGER.info("Migrated old {} combo at {} into Magic Center at {}",
                match.outputType(), match.comboPos(), worldPosition);
    }

    private static LegacyComboMatch findLegacyComboMatch(ServerLevel level, BlockPos comboPos,
                                                         ComboType comboType) {
        ComboBlockEntity.PatternEntry[] entries = ComboBlockEntity.getPattern(comboType);
        for (int rotation = 0; rotation < 4; rotation++) {
            List<BlockPos> circles = new ArrayList<>();
            boolean valid = true;
            for (ComboBlockEntity.PatternEntry entry : entries) {
                int dx = switch (rotation) {
                    case 1 -> -entry.dz();
                    case 2 -> -entry.dx();
                    case 3 -> entry.dz();
                    default -> entry.dx();
                };
                int dz = switch (rotation) {
                    case 1 -> entry.dx();
                    case 2 -> -entry.dz();
                    case 3 -> -entry.dx();
                    default -> entry.dz();
                };
                BlockPos found = null;
                for (int y = -1; y <= 1; y++) {
                    BlockPos candidate = comboPos.offset(dx, y, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (state.is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())
                            && state.getValue(MagicCircleBlock.CIRCLE_TYPE) == entry.type()) {
                        found = candidate.immutable();
                        break;
                    }
                }
                if (found == null) {
                    valid = false;
                    break;
                }
                circles.add(found);
            }
            if (valid) {
                ResourceLocation output = switch (comboType) {
                    case FOG -> MagicCenterTypeRegistry.FOG;
                    case INVERTED -> MagicCenterTypeRegistry.INVERTED;
                    case FORCEFIELD -> MagicCenterTypeRegistry.FORCEFIELD;
                };
                return new LegacyComboMatch(comboPos.immutable(), output, List.copyOf(circles));
            }
        }
        return null;
    }

    private void recoverPendingTransformation() {
        if (pendingTransformation == null || level == null || level.isClientSide) {
            return;
        }
        if (!MagicCenterTypeRegistry.contains(pendingTransformation.outputType())) {
            unknownRecoveryType = pendingTransformation.outputType();
            pendingTransformation = null;
            setChangedAndSync();
            return;
        }
        if (transformationCircles.isEmpty()) {
            transformationCircles = snapshotCircles(pendingTransformation.consumedPositions());
        }
        for (BlockPos consumed : pendingTransformation.consumedPositions()) {
            if (level.getBlockState(consumed).is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                level.setBlock(consumed, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        centerTypeId = pendingTransformation.outputType();
        pendingTransformation = null;
        fuelCostRemainder = 0L;
        passiveCircleCounts = CircleCounts.ZERO;
        passiveCacheDirty = true;
        setChangedAndSync();
    }

    public void beginPendingTransformation(ResourceLocation outputType,
                                           List<BlockPos> consumedPositions) {
        transformationCircles = snapshotCircles(consumedPositions);
        pendingTransformation = new PendingTransformation(outputType,
                consumedPositions.stream().map(BlockPos::immutable).toList());
        setChangedAndSync();
    }

    public void completeTransformation(ResourceLocation outputType) {
        if (!MagicCenterTypeRegistry.contains(outputType)
                || MagicCenterTypeRegistry.BLANK.equals(outputType)) {
            return;
        }
        centerTypeId = outputType;
        pendingTransformation = null;
        fuelCostRemainder = 0L;
        passiveCircleCounts = CircleCounts.ZERO;
        passiveCacheDirty = true;
        setChangedAndSync();
    }

    private List<ConsumedCircle> snapshotCircles(List<BlockPos> positions) {
        if (level == null || positions.isEmpty()) {
            return List.of();
        }
        List<ConsumedCircle> snapshots = new ArrayList<>();
        for (BlockPos position : positions) {
            BlockState state = level.getBlockState(position);
            if (state.is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                snapshots.add(new ConsumedCircle(position.immutable(),
                        state.getValue(MagicCircleBlock.CIRCLE_TYPE)));
            }
        }
        return List.copyOf(snapshots);
    }

    public boolean breakTransformation(ServerLevel serverLevel) {
        if (isBlank()) {
            return false;
        }

        if (MagicCenterTypeRegistry.TELEPORT.equals(centerTypeId)) {
            unlinkTeleportPair();
        }

        List<ConsumedCircle> circles = transformationCircles.isEmpty()
                ? recoverCanonicalPattern() : transformationCircles;
        int brokenCount = circles.isEmpty() ? 0
                : Math.max(1, Math.round(circles.size() * 0.25F));
        Set<Integer> brokenIndices = new HashSet<>();
        while (brokenIndices.size() < brokenCount) {
            brokenIndices.add(serverLevel.random.nextInt(circles.size()));
        }

        centerTypeId = MagicCenterTypeRegistry.BLANK;
        pendingTransformation = null;
        transformationCircles = List.of();
        fuelCostRemainder = 0L;
        active = false;
        barrierHealth = 0.0F;
        passiveCircleCounts = CircleCounts.ZERO;
        passiveCacheDirty = true;
        lastPassiveScan = Long.MIN_VALUE;

        for (int index = 0; index < circles.size(); index++) {
            ConsumedCircle circle = circles.get(index);
            BlockState circleState = ModBlocks.MAGIC_CIRCLE_BLOCK.get().defaultBlockState()
                    .setValue(MagicCircleBlock.CIRCLE_TYPE, circle.type());
            BlockState currentState = serverLevel.getBlockState(circle.pos());
            boolean broken = brokenIndices.contains(index) || !currentState.isAir();
            if (!broken) {
                serverLevel.setBlock(circle.pos(), circleState, Block.UPDATE_ALL);
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        circle.pos().getX() + 0.5D, circle.pos().getY() + 0.2D,
                        circle.pos().getZ() + 0.5D, 8,
                        0.18D, 0.08D, 0.18D, 0.02D);
                continue;
            }

            if (currentState.is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                serverLevel.setBlock(circle.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    circle.pos().getX() + 0.5D, circle.pos().getY() + 0.15D,
                    circle.pos().getZ() + 0.5D, 20,
                    0.32D, 0.12D, 0.32D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    circle.pos().getX() + 0.5D, circle.pos().getY() + 0.18D,
                    circle.pos().getZ() + 0.5D, 8,
                    0.22D, 0.08D, 0.22D, 0.015D);
        }

        serverLevel.playSound(null, worldPosition, SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS, 1.2F, 0.65F);
        serverLevel.sendParticles(ParticleTypes.POOF,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.35D,
                worldPosition.getZ() + 0.5D, 28,
                0.55D, 0.25D, 0.55D, 0.04D);
        setChangedAndSync();
        serverLevel.getChunkSource().blockChanged(worldPosition);
        return true;
    }

    private List<ConsumedCircle> recoverCanonicalPattern() {
        for (CenterRitualDefinition definition : CenterRitualManager.INSTANCE.definitions()) {
            if (!definition.output().equals(centerTypeId)) {
                continue;
            }
            List<ConsumedCircle> recovered = new ArrayList<>();
            for (CenterRitualDefinition.Requirement requirement : definition.pattern()) {
                CircleType type = parseCircleType(requirement.properties().get("circle_type"));
                recovered.add(new ConsumedCircle(
                        worldPosition.offset(requirement.offset()).immutable(), type));
            }
            return List.copyOf(recovered);
        }
        return List.of();
    }

    private static CircleType parseCircleType(String name) {
        for (CircleType type : CircleType.values()) {
            if (type.getSerializedName().equals(name)) {
                return type;
            }
        }
        return CircleType.ATTACK;
    }

    public MagicCenterType getCenterType() {
        return MagicCenterTypeRegistry.getOrBlank(centerTypeId);
    }

    public ResourceLocation getCenterTypeId() {
        return centerTypeId;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public boolean hasTeleportLink() {
        return teleportLinkedDimension != null
                && teleportLinkedPos != null
                && teleportLinkedCenterId != null;
    }

    public boolean canTeleportOut() {
        return hasTeleportLink()
                && (teleportSourceCenterId == null || centerId.equals(teleportSourceCenterId));
    }

    /** -1 unlinked, 0 two-way, 1 this center is source, 2 linked center is source. */
    public int getTeleportLinkMode() {
        if (!hasTeleportLink()) {
            return -1;
        }
        if (teleportSourceCenterId == null) {
            return 0;
        }
        return centerId.equals(teleportSourceCenterId) ? 1 : 2;
    }

    public String getTeleportDestinationLabel() {
        if (!hasTeleportLink()) {
            return "-";
        }
        return teleportLinkedDimension + " @ "
                + teleportLinkedPos.getX() + ", "
                + teleportLinkedPos.getY() + ", "
                + teleportLinkedPos.getZ();
    }

    public void linkTeleportPair(MagicCenterBlockEntity other) {
        if (other == null || other == this || level == null || other.level == null
                || !MagicCenterTypeRegistry.TELEPORT.equals(centerTypeId)
                || !MagicCenterTypeRegistry.TELEPORT.equals(other.centerTypeId)) {
            return;
        }
        teleportLinkedDimension = other.level.dimension().location();
        teleportLinkedPos = other.worldPosition.immutable();
        teleportLinkedCenterId = other.centerId;
        teleportSourceCenterId = null;
        other.teleportLinkedDimension = level.dimension().location();
        other.teleportLinkedPos = worldPosition.immutable();
        other.teleportLinkedCenterId = centerId;
        other.teleportSourceCenterId = null;
        clearTeleportChannel();
        other.clearTeleportChannel();
        setChangedAndSync();
        other.setChangedAndSync();
    }

    public void cycleTeleportLinkMode() {
        ResolvedTeleportTarget target = resolveTeleportTarget();
        if (target == null) {
            return;
        }
        UUID nextSource;
        if (teleportSourceCenterId == null) {
            nextSource = centerId;
        } else if (centerId.equals(teleportSourceCenterId)) {
            nextSource = teleportLinkedCenterId;
        } else {
            nextSource = null;
        }
        teleportSourceCenterId = nextSource;
        target.center.teleportSourceCenterId = nextSource;
        clearTeleportChannel();
        target.center.clearTeleportChannel();
        setChangedAndSync();
        target.center.setChangedAndSync();
    }

    public ResolvedTeleportTarget resolveTeleportTarget() {
        if (!(level instanceof ServerLevel sourceLevel) || !hasTeleportLink()) {
            return null;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(
                Registries.DIMENSION, teleportLinkedDimension);
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(dimensionKey);
        if (targetLevel == null) {
            return null;
        }
        targetLevel.getChunkAt(teleportLinkedPos);
        if (!(targetLevel.getBlockEntity(teleportLinkedPos)
                instanceof MagicCenterBlockEntity target)
                || !MagicCenterTypeRegistry.TELEPORT.equals(target.centerTypeId)
                || !teleportLinkedCenterId.equals(target.centerId)
                || !centerId.equals(target.teleportLinkedCenterId)) {
            return null;
        }
        return new ResolvedTeleportTarget(targetLevel, target);
    }

    /** Repairs the remote endpoint coordinates after this center was moved by a 阵盘. */
    public void reconcileTeleportLinkAfterPlacement() {
        ResolvedTeleportTarget target = resolveTeleportTarget();
        if (target == null || level == null) {
            return;
        }
        target.center.teleportLinkedDimension = level.dimension().location();
        target.center.teleportLinkedPos = worldPosition.immutable();
        target.center.teleportLinkedCenterId = centerId;
        target.center.teleportSourceCenterId = teleportSourceCenterId;
        target.center.setChangedAndSync();
        setChangedAndSync();
    }

    public void unlinkTeleportPair() {
        ResolvedTeleportTarget target = resolveTeleportTarget();
        if (target != null) {
            target.center.clearTeleportLinkLocal();
            target.center.setChangedAndSync();
        }
        clearTeleportLinkLocal();
        setChangedAndSync();
    }

    private void clearTeleportLinkLocal() {
        teleportLinkedDimension = null;
        teleportLinkedPos = null;
        teleportLinkedCenterId = null;
        teleportSourceCenterId = null;
        clearTeleportChannel();
    }

    public boolean isInsideRadius(net.minecraft.world.entity.Entity entity) {
        return entity != null
                && entity.position().distanceToSqr(centerPoint()) <= (double)radius * radius;
    }

    public boolean hasTeleportChannel() {
        return teleportChannelPlayer != null;
    }

    public UUID getTeleportChannelPlayer() {
        return teleportChannelPlayer;
    }

    public long getTeleportChannelStartTime() {
        return teleportChannelStartTime;
    }

    public int getTeleportChannelDurationTicks() {
        return teleportChannelDurationTicks;
    }

    public float getTeleportChannelStartHealth() {
        return teleportChannelStartHealth;
    }

    public int getTeleportChannelObservedHurtTime() {
        return teleportChannelObservedHurtTime;
    }

    public void observeTeleportHurtTime(int hurtTime) {
        teleportChannelObservedHurtTime = Math.max(0, hurtTime);
    }

    public void beginTeleportChannel(UUID playerId, long startTime, int durationTicks,
                                     float startHealth, int observedHurtTime) {
        teleportChannelPlayer = playerId;
        teleportChannelStartTime = startTime;
        teleportChannelDurationTicks = Math.max(1, durationTicks);
        teleportChannelStartHealth = startHealth;
        teleportChannelObservedHurtTime = Math.max(0, observedHurtTime);
        setChangedAndSync();
    }

    public void clearTeleportChannel() {
        if (teleportChannelPlayer == null && teleportChannelStartTime < 0L
                && teleportChannelDurationTicks == 0) {
            return;
        }
        teleportChannelPlayer = null;
        teleportChannelStartTime = -1L;
        teleportChannelDurationTicks = 0;
        teleportChannelStartHealth = 0.0F;
        teleportChannelObservedHurtTime = 0;
        setChangedAndSync();
    }

    public boolean hasTeleportEnergy(long requestedEnergy) {
        long cost = Math.max(0L, requestedEnergy);
        if (cost == 0L || hasInfiniteFuel()) {
            return true;
        }
        ItemStack storedFuel = fuelSlot.getStackInSlot(0);
        long fuelValue = CenterFuelController.fuelValue(storedFuel);
        long itemEnergy = fuelValue <= 0L ? 0L
                : fuelValue > Long.MAX_VALUE / storedFuel.getCount()
                ? Long.MAX_VALUE : fuelValue * storedFuel.getCount();
        return energy >= cost || itemEnergy >= cost - energy;
    }

    public boolean consumeTeleportEnergy(long requestedEnergy) {
        long cost = Math.max(0L, requestedEnergy);
        if (cost == 0L || hasInfiniteFuel()) {
            return true;
        }
        if (!hasTeleportEnergy(cost)) {
            return false;
        }
        while (energy < cost) {
            ItemStack fuel = fuelSlot.getStackInSlot(0);
            int value = CenterFuelController.fuelValue(fuel);
            if (value <= 0) {
                return false;
            }
            long missing = cost - energy;
            int needed = (int)Math.min(Integer.MAX_VALUE,
                    Math.max(1L, (missing + value - 1L) / value));
            ItemStack consumed = fuelSlot.extractItem(0, needed, false);
            if (consumed.isEmpty()) {
                return false;
            }
            long added = (long)value * consumed.getCount();
            energy = added > Long.MAX_VALUE - energy ? Long.MAX_VALUE : energy + added;
        }
        energy -= cost;
        setChangedAndSync();
        return true;
    }

    public boolean isBlank() {
        return MagicCenterTypeRegistry.BLANK.equals(centerTypeId);
    }

    public int getCurrentFuelCostPerMinute() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return applyFormationFlagFuelMultiplier(CenterFuelController.baseRadiusCost(radius));
        }
        CenterContext context = new CenterContext(serverLevel, worldPosition, getBlockState(), this);
        int baseCost = Math.max(0, getCenterType().behavior().getFuelCostPerMinute(context));
        return applyFormationFlagFuelMultiplier(baseCost);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int requestedRadius) {
        int normalized = normalizeRadius(requestedRadius);
        if (radius != normalized) {
            radius = normalized;
            invalidatePassiveCache();
            setChangedAndSync();
        }
    }

    public static int normalizeRadius(int requestedRadius) {
        int clamped = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, requestedRadius));
        return (clamped & 1) == 0 ? clamped - 1 : clamped;
    }

    /** Returns loaded block entities without probing every block position in the search square. */
    public static List<BlockEntity> getLoadedBlockEntitiesNear(
            Level level, BlockPos origin, int horizontalRadius) {
        int radius = Math.max(0, horizontalRadius);
        int minChunkX = Math.floorDiv(origin.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + radius, 16);
        List<BlockEntity> blockEntities = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    BlockPos blockPos = blockEntity.getBlockPos();
                    if (Math.abs(blockPos.getX() - origin.getX()) <= radius
                            && Math.abs(blockPos.getZ() - origin.getZ()) <= radius) {
                        blockEntities.add(blockEntity);
                    }
                }
            }
        }
        return blockEntities;
    }

    /** Returns loaded centers without probing every block position in the search square. */
    public static List<MagicCenterBlockEntity> getLoadedCentersNear(
            Level level, BlockPos origin, int horizontalRadius) {
        List<MagicCenterBlockEntity> centers = new ArrayList<>();
        for (BlockEntity blockEntity : getLoadedBlockEntitiesNear(level, origin, horizontalRadius)) {
            if (blockEntity instanceof MagicCenterBlockEntity center) {
                centers.add(center);
            }
        }
        return centers;
    }

    public Vec3 centerPoint() {
        return Vec3.atCenterOf(worldPosition);
    }

    public long getEnergy() {
        return energy;
    }

    public void setEnergy(long energy) {
        this.energy = Math.max(0L, energy);
    }

    public long getFuelCostRemainder() {
        return fuelCostRemainder;
    }

    public void setFuelCostRemainder(long fuelCostRemainder) {
        this.fuelCostRemainder = Math.max(0L, fuelCostRemainder);
    }

    public void addEnergy(long energy) {
        if (energy > 0L) {
            this.energy = Math.min(Long.MAX_VALUE, this.energy + Math.min(energy, Long.MAX_VALUE - this.energy));
            setChanged();
        }
    }

    public float getBarrierHealth() {
        return barrierHealth;
    }

    public void addBarrierHealth(float amount) {
        if (amount > 0.0F) {
            barrierHealth = Math.min(Float.MAX_VALUE, barrierHealth + amount);
            setChangedAndSync();
        }
    }

    public boolean absorbBarrierDamage(float amount) {
        if (!(level instanceof ServerLevel serverLevel)
                || !MagicCenterTypeRegistry.FORCEFIELD.equals(centerTypeId)) {
            return false;
        }
        if (temporary) {
            return active;
        }
        if (hasInfiniteFuel()) {
            return true;
        }
        float remainingDamage = Math.max(1.0F, amount);
        while (barrierHealth < remainingDamage) {
            ItemStack reserveStone = fuelSlot.extractItem(0, 1, false);
            if (reserveStone.isEmpty()) {
                collapseBarrier(serverLevel);
                return false;
            }
            barrierHealth += CenterFuelController.barrierHealthFor(reserveStone);
        }
        barrierHealth = Math.max(0.0F, barrierHealth - remainingDamage);
        setChangedAndSync();
        return true;
    }

    private void collapseBarrier(ServerLevel serverLevel) {
        active = false;
        energy = 0L;
        barrierHealth = 0.0F;
        Vec3 center = centerPoint();
        serverLevel.explode(null, center.x, center.y, center.z, 3.0F,
                Level.ExplosionInteraction.NONE);
        breakTransformation(serverLevel);
    }

    public boolean hasFuel() {
        return energy > 0L || !fuelSlot.getStackInSlot(0).isEmpty();
    }

    public boolean hasInfiniteFuel() {
        return fuelSlot.getStackInSlot(0).is(ModItems.INFINITE_SPIRIT_STONE.get());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (!enabled) {
                active = false;
            }
            setChangedAndSync();
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public boolean hasAuthorizedPlayerInside() {
        return authorizedPlayerInside;
    }

    public long getAuthorizedEntryGameTime() {
        return authorizedEntryGameTime;
    }

    public void setParticlesEnabled(boolean particlesEnabled) {
        if (this.particlesEnabled != particlesEnabled) {
            this.particlesEnabled = particlesEnabled;
            setChangedAndSync();
        }
    }

    public int getGlyphColor() {
        return glyphColor;
    }

    public void setGlyphColor(int glyphColor) {
        int normalized = glyphColor & 0xFFFFFF;
        if (this.glyphColor != normalized) {
            this.glyphColor = normalized;
            setChangedAndSync();
        }
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerDisplayLabel() {
        if (ownerUuid == null) {
            return "-";
        }
        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner != null) {
                return owner.getGameProfile().getName();
            }
        }
        return ownerUuid.toString();
    }

    public void setOwnerUuid(UUID ownerUuid) {
        if (this.ownerUuid == null && ownerUuid != null) {
            this.ownerUuid = ownerUuid;
            controllerPlayers.clear();
            excludedPlayers.clear();
            legacyExcludedNames.clear();
            setChangedAndSync();
        }
    }

    public boolean isOwner(Player player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    public boolean isController(Player player) {
        UUID playerUuid = player.getUUID();
        if (isOwner(player)) {
            if (controllerPlayers.remove(playerUuid) != null) {
                setChanged();
            }
            return false;
        }
        String savedName = controllerPlayers.get(playerUuid);
        if (savedName == null) {
            return false;
        }
        String currentName = player.getGameProfile().getName();
        if (!currentName.equals(savedName)) {
            controllerPlayers.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(playerUuid)
                            && entry.getValue().equalsIgnoreCase(currentName))
                    .forEach(entry -> entry.setValue(entry.getKey().toString()));
            controllerPlayers.put(playerUuid, currentName);
            setChanged();
        }
        return true;
    }

    public Set<UUID> getControllerPlayers() {
        return Set.copyOf(controllerPlayers.keySet());
    }

    public Set<String> getControllerWhitelist() {
        if (level instanceof ServerLevel serverLevel) {
            refreshControllerNames(serverLevel);
        }
        Set<String> labels = new HashSet<>();
        controllerPlayers.forEach((uuid, name) -> labels.add(
                name == null || name.isBlank() ? uuid.toString() : name));
        return labels;
    }

    private void refreshControllerNames(ServerLevel serverLevel) {
        boolean changed = false;
        for (Map.Entry<UUID, String> entry : controllerPlayers.entrySet()) {
            ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
            String currentName = online == null
                    ? serverLevel.getServer().getProfileCache().get(entry.getKey())
                    .map(GameProfile::getName)
                    .orElse(entry.getValue())
                    : online.getGameProfile().getName();
            if (isValidPlayerName(currentName)
                    && !currentName.equals(entry.getValue())
                    && controllerPlayers.entrySet().stream().noneMatch(other ->
                    !other.getKey().equals(entry.getKey())
                            && other.getValue().equalsIgnoreCase(currentName))) {
                entry.setValue(currentName);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    public boolean addController(String name) {
        if (!isValidPlayerName(name)) {
            return false;
        }
        String normalizedName = name.trim();
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        refreshControllerNames(serverLevel);
        ServerPlayer online = serverLevel.getServer().getPlayerList()
                .getPlayerByName(normalizedName);
        GameProfile profile = online == null
                ? serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null)
                : online.getGameProfile();
        if (profile == null || profile.getId() == null || profile.getId().equals(ownerUuid)
                || controllerPlayers.containsKey(profile.getId())
                || controllerPlayers.values().stream()
                .anyMatch(savedName -> savedName.equalsIgnoreCase(normalizedName))
                || controllerPlayers.size() >= MAX_CONTROLLER_PLAYERS) {
            return false;
        }
        String profileName = profile.getName();
        controllerPlayers.put(profile.getId(),
                profileName == null || profileName.isBlank() ? normalizedName : profileName);
        setChangedAndSync();
        return true;
    }

    public boolean removeController(String name) {
        if (name == null || name.isBlank() || name.length() > 64) {
            return false;
        }
        String normalizedName = name.trim();
        UUID controllerUuid = null;
        try {
            controllerUuid = UUID.fromString(normalizedName);
        } catch (IllegalArgumentException ignored) {
        }
        if (controllerUuid == null && level instanceof ServerLevel serverLevel) {
            refreshControllerNames(serverLevel);
            ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayerByName(normalizedName);
            GameProfile profile = online == null
                    ? serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null)
                    : online.getGameProfile();
            if (profile != null && controllerPlayers.containsKey(profile.getId())) {
                controllerUuid = profile.getId();
            }
        }
        if (controllerUuid == null) {
            controllerUuid = controllerPlayers.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(normalizedName))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }
        boolean changed = controllerUuid != null && controllerPlayers.remove(controllerUuid) != null;
        if (changed) {
            setChangedAndSync();
        }
        return changed;
    }

    private static boolean isValidPlayerName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= 1
                && trimmed.length() <= 16
                && trimmed.matches("[A-Za-z0-9_]+");
    }

    private static String findIgnoreCase(Set<String> values, String target) {
        if (target == null) {
            return null;
        }
        return values.stream()
                .filter(value -> value.equalsIgnoreCase(target))
                .findFirst()
                .orElse(null);
    }

    private String resolvePlayerLabel(UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (online != null) {
                return online.getGameProfile().getName();
            }
            return serverLevel.getServer().getProfileCache().get(uuid)
                    .map(GameProfile::getName)
                    .filter(MagicCenterBlockEntity::isValidPlayerName)
                    .orElse(uuid.toString());
        }
        return uuid.toString();
    }

    private void saveAccessLists(CompoundTag tag) {
        ListTag controllers = new ListTag();
        ListTag controllerNames = new ListTag();
        for (Map.Entry<UUID, String> controller : controllerPlayers.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", controller.getKey());
            String name = controller.getValue();
            if (!isValidPlayerName(name)) {
                name = resolvePlayerLabel(controller.getKey());
            }
            if (isValidPlayerName(name)) {
                entry.putString("Name", name);
                controllerNames.add(StringTag.valueOf(name));
            }
            controllers.add(entry);
        }
        tag.put("ControllerPlayers", controllers);
        tag.put("ControllerWhitelist", controllerNames);

        ListTag excluded = new ListTag();
        ListTag blacklistedNames = new ListTag();
        Set<String> savedNames = new HashSet<>();
        for (UUID uuid : excludedPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            String name = resolvePlayerLabel(uuid);
            if (isValidPlayerName(name)) {
                entry.putString("Name", name);
                if (findIgnoreCase(savedNames, name) == null) {
                    savedNames.add(name);
                    blacklistedNames.add(StringTag.valueOf(name));
                }
            }
            excluded.add(entry);
        }
        tag.put("ExcludedPlayers", excluded);

        ListTag legacy = new ListTag();
        for (String name : legacyExcludedNames) {
            legacy.add(StringTag.valueOf(name));
            if (findIgnoreCase(savedNames, name) == null) {
                savedNames.add(name);
                blacklistedNames.add(StringTag.valueOf(name));
            }
        }
        tag.put("LegacyExcludedNames", legacy);
        tag.put("BlacklistedPlayerNames", blacklistedNames);
    }

    private void loadAccessLists(CompoundTag tag) {
        controllerPlayers.clear();
        ListTag controllers = tag.getList("ControllerPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < controllers.size()
                && controllerPlayers.size() < MAX_CONTROLLER_PLAYERS; i++) {
            CompoundTag entry = controllers.getCompound(i);
            if (!entry.hasUUID("UUID")) {
                continue;
            }
            UUID uuid = entry.getUUID("UUID");
            if (uuid.equals(ownerUuid) || controllerPlayers.containsKey(uuid)) {
                continue;
            }
            String name = entry.getString("Name");
            if (!isValidPlayerName(name)) {
                name = resolvePlayerLabel(uuid);
            }
            String proposedLabel = name;
            String label = controllerPlayers.values().stream()
                    .anyMatch(existing -> existing.equalsIgnoreCase(proposedLabel))
                    ? uuid.toString() : proposedLabel;
            controllerPlayers.put(uuid, label);
        }

        excludedPlayers.clear();
        legacyExcludedNames.clear();
        Set<String> loadedNames = new HashSet<>();
        ListTag excluded = tag.getList("ExcludedPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < excluded.size()
                && excludedPlayers.size() < MAX_BLACKLISTED_PLAYERS; i++) {
            CompoundTag entry = excluded.getCompound(i);
            if (entry.hasUUID("UUID")) {
                excludedPlayers.add(entry.getUUID("UUID"));
            }
            String name = entry.getString("Name");
            if (isValidPlayerName(name)) {
                loadedNames.add(name);
            }
        }

        readPlayerNameSet(tag.getList("LegacyExcludedNames", Tag.TAG_STRING),
                legacyExcludedNames, MAX_BLACKLISTED_PLAYERS - excludedPlayers.size(), loadedNames);
        readPlayerNameSet(tag.getList("blacklistedPlayers", Tag.TAG_STRING),
                legacyExcludedNames, MAX_BLACKLISTED_PLAYERS - excludedPlayers.size(), loadedNames);
        if (excluded.isEmpty()) {
            readPlayerNameSet(tag.getList("BlacklistedPlayerNames", Tag.TAG_STRING),
                    legacyExcludedNames, MAX_BLACKLISTED_PLAYERS, Set.of());
        }
    }

    public boolean isPlayerExcluded(Player player) {
        if (excludedPlayers.contains(player.getUUID())) {
            return true;
        }
        String legacyName = findIgnoreCase(
                legacyExcludedNames, player.getGameProfile().getName());
        if (legacyName != null) {
            legacyExcludedNames.remove(legacyName);
            excludedPlayers.add(player.getUUID());
            setChangedAndSync();
            return true;
        }
        return false;
    }

    public Set<UUID> getExcludedPlayers() {
        return Set.copyOf(excludedPlayers);
    }

    public Set<String> getBlacklistedPlayers() {
        Set<String> labels = new HashSet<>(legacyExcludedNames);
        for (UUID uuid : excludedPlayers) {
            labels.add(resolvePlayerLabel(uuid));
        }
        return labels;
    }

    public boolean addExcludedPlayer(UUID uuid) {
        boolean changed = uuid != null
                && excludedPlayers.size() + legacyExcludedNames.size()
                < MAX_BLACKLISTED_PLAYERS
                && excludedPlayers.add(uuid);
        if (changed) {
            setChangedAndSync();
        }
        return changed;
    }

    public boolean removeExcludedPlayer(UUID uuid) {
        boolean changed = uuid != null && excludedPlayers.remove(uuid);
        if (changed) {
            setChangedAndSync();
        }
        return changed;
    }

    public boolean addBlacklistedPlayer(String name) {
        if (!isValidPlayerName(name)
                || excludedPlayers.size() + legacyExcludedNames.size()
                >= MAX_BLACKLISTED_PLAYERS) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        String normalizedName = name.trim();
        ServerPlayer online = serverLevel.getServer().getPlayerList()
                .getPlayerByName(normalizedName);
        GameProfile profile = online == null
                ? serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null)
                : online.getGameProfile();
        if (profile == null || profile.getId() == null
                || excludedPlayers.contains(profile.getId())
                || findIgnoreCase(legacyExcludedNames, normalizedName) != null) {
            return false;
        }
        boolean changed = excludedPlayers.add(profile.getId());
        if (changed) {
            setChangedAndSync();
        }
        return changed;
    }

    public boolean removeBlacklistedPlayer(String name) {
        String normalizedName = name == null ? "" : name.trim();
        boolean changed = false;
        try {
            changed = excludedPlayers.remove(UUID.fromString(normalizedName));
        } catch (IllegalArgumentException ignored) {
        }
        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer online = serverLevel.getServer().getPlayerList()
                    .getPlayerByName(normalizedName);
            GameProfile profile = online == null && isValidPlayerName(normalizedName)
                    ? serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null)
                    : online == null ? null : online.getGameProfile();
            if (profile != null) {
                changed |= excludedPlayers.remove(profile.getId());
            }
        }
        String legacyName = findIgnoreCase(legacyExcludedNames, normalizedName);
        if (legacyName != null) {
            changed |= legacyExcludedNames.remove(legacyName);
        }
        if (changed) {
            setChangedAndSync();
        }
        return changed;
    }

    public boolean isAllowHostileMobs() {
        return allowHostileMobs;
    }

    public void setAllowHostileMobs(boolean allowHostileMobs) {
        if (this.allowHostileMobs != allowHostileMobs) {
            this.allowHostileMobs = allowHostileMobs;
            setChanged();
        }
    }

    public boolean isAllowPassiveMobs() {
        return allowPassiveMobs;
    }

    public void setAllowPassiveMobs(boolean allowPassiveMobs) {
        if (this.allowPassiveMobs != allowPassiveMobs) {
            this.allowPassiveMobs = allowPassiveMobs;
            setChanged();
        }
    }

    public boolean isAffectedByCircle(LivingEntity entity) {
        if (entity instanceof Player player) {
            return !isPlayerExcluded(player);
        }
        return entity instanceof Enemy ? allowHostileMobs : allowPassiveMobs;
    }

    public String getCustomNameText() {
        return customName;
    }

    public void setCustomNameText(String customName) {
        this.customName = customName == null ? "" : customName.strip();
        setChanged();
    }

    public CircleCounts getPassiveCircleCounts() {
        return passiveCircleCounts;
    }

    public void invalidatePassiveCache() {
        passiveCacheDirty = true;
    }

    public void refreshPassiveCircleCacheIfNeeded(boolean force) {
        if (!isBlank() || level == null) {
            passiveCircleCounts = CircleCounts.ZERO;
            return;
        }
        long gameTime = level.getGameTime();
        if (!force && !passiveCacheDirty
                && gameTime - lastPassiveScan < CACHE_VALIDATION_TICKS) {
            return;
        }
        int attack = 0;
        int defense = 0;
        int regeneration = 0;
        double radiusSquared = radius * radius;
        for (BlockEntity blockEntity : getLoadedBlockEntitiesNear(level, worldPosition, radius)) {
            BlockPos scanPos = blockEntity.getBlockPos();
            if (Math.abs(scanPos.getY() - worldPosition.getY()) > 3
                    || Vec3.atCenterOf(scanPos).distanceToSqr(centerPoint()) > radiusSquared) {
                continue;
            }
            BlockState state = blockEntity.getBlockState();
            if (!state.is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                continue;
            }
            switch (state.getValue(MagicCircleBlock.CIRCLE_TYPE)) {
                case ATTACK -> attack++;
                case DEFENSE -> defense++;
                case REGENERATION -> regeneration++;
            }
        }
        passiveCircleCounts = new CircleCounts(attack, defense, regeneration);
        passiveCacheDirty = false;
        lastPassiveScan = gameTime;
        setChanged();
    }

    public static void invalidateNearbyBlankCenters(Level level, BlockPos circlePos) {
        if (level.isClientSide) {
            return;
        }
        for (MagicCenterBlockEntity center : getLoadedCentersNear(level, circlePos, MAX_RADIUS)) {
            if (Math.abs(center.worldPosition.getY() - circlePos.getY()) <= 3
                    && center.isBlank()
                    && Vec3.atCenterOf(circlePos).distanceToSqr(center.centerPoint())
                    <= center.radius * center.radius) {
                center.invalidatePassiveCache();
            }
        }
    }

    public ItemStackHandler getFuelSlot() {
        return fuelSlot;
    }

    public ItemStackHandler getReforgeWeaponSlot() {
        return reforgeWeaponSlot;
    }

    public ItemStackHandler getOctagonGrid() {
        return octagonGrid;
    }

    public int countDurationFlags() {
        return countFlags(ModItems.DURATION_FLAG.get());
    }

    public int countAmplifierFlags() {
        return countFlags(ModItems.AMPLIFIER_FLAG.get());
    }

    public int countSpiritGatheringFlags() {
        return countFlags(ModItems.SPIRIT_GATHERING_FLAG.get());
    }

    public int countHasteFlags() {
        return countFlags(ModItems.HASTE_FLAG.get());
    }

    public int countQiReturnFlags() {
        return countFlags(ModItems.QI_RETURN_FLAG.get());
    }

    public int countFormationFlags() {
        int count = 0;
        for (int slot = 0; slot < octagonGrid.getSlots(); slot++) {
            ItemStack stack = octagonGrid.getStackInSlot(slot);
            if (ModItems.isFormationFlag(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int applyFormationFlagFuelMultiplier(int baseCost) {
        long adjustedCost = Math.max(0, baseCost);
        if (adjustedCost == 0L) {
            return 0;
        }

        int gatheringFlags = countSpiritGatheringFlags();
        int normalCostFlags = Math.max(0, countFormationFlags() - gatheringFlags);
        for (int i = 0; i < normalCostFlags; i++) {
            adjustedCost = (adjustedCost * 3L + 1L) / 2L;
            if (adjustedCost >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }

        for (int i = 0; i < countHasteFlags(); i++) {
            adjustedCost = (adjustedCost * 13L + 9L) / 10L;
            if (adjustedCost >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }

        for (int i = 0; i < gatheringFlags; i++) {
            adjustedCost = Math.max(1L, adjustedCost * 3L / 4L);
        }
        return (int)Math.min(Integer.MAX_VALUE, adjustedCost);
    }

    public int adjustedTriggerInterval(int baseTicks) {
        int interval = Math.max(1, baseTicks);
        for (int i = 0; i < countHasteFlags(); i++) {
            interval = Math.max(1, (int)Math.round(interval / 1.3D));
        }
        return interval;
    }

    public boolean shouldTriggerEvery(long gameTime, int baseTicks) {
        return gameTime % adjustedTriggerInterval(baseTicks) == 0L;
    }

    public int getQiReturnAmount() {
        return countQiReturnFlags() * 10;
    }

    public int getAttackCount() {
        return 1 + countAmplifierFlags();
    }

    public boolean hasFormationFlags() {
        return countFormationFlags() > 0;
    }

    public ItemStack getFormationFlagForRender(int slot) {
        if (slot < 0 || slot >= octagonGrid.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = octagonGrid.getStackInSlot(slot);
        return ModItems.isFormationFlag(stack.getItem()) ? stack : ItemStack.EMPTY;
    }

    private int countFlags(net.minecraft.world.item.Item flagItem) {
        int count = 0;
        for (int slot = 0; slot < octagonGrid.getSlots(); slot++) {
            ItemStack stack = octagonGrid.getStackInSlot(slot);
            if (stack.is(flagItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public ItemStack getReforgeWeapon() {
        return reforgeWeaponSlot.getStackInSlot(0);
    }

    public void repairReforgeWeapon(int amount) {
        ItemStack weapon = reforgeWeaponSlot.getStackInSlot(0);
        if (amount <= 0 || weapon.isEmpty() || !weapon.isDamageableItem()
                || !weapon.isDamaged()) {
            return;
        }
        ItemStack repaired = weapon.copy();
        repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - amount));
        reforgeWeaponSlot.setStackInSlot(0, repaired);
    }

    @Override
    public Component getDisplayName() {
        return customName.isBlank()
                ? Component.translatable(getCenterType().translationKey())
                : Component.literal(customName);
    }

    private void saveCenterIdentityAndTeleportLink(CompoundTag tag) {
        tag.putUUID("CenterId", centerId);
        if (!hasTeleportLink()) {
            return;
        }
        CompoundTag link = new CompoundTag();
        link.putString("Dimension", teleportLinkedDimension.toString());
        link.putLong("Pos", teleportLinkedPos.asLong());
        link.putUUID("CenterId", teleportLinkedCenterId);
        if (teleportSourceCenterId != null) {
            link.putUUID("SourceCenterId", teleportSourceCenterId);
        }
        tag.put("TeleportLink", link);
    }

    private void loadCenterIdentityAndTeleportLink(CompoundTag tag) {
        centerId = tag.hasUUID("CenterId") ? tag.getUUID("CenterId") : UUID.randomUUID();
        teleportLinkedDimension = null;
        teleportLinkedPos = null;
        teleportLinkedCenterId = null;
        teleportSourceCenterId = null;
        if (!tag.contains("TeleportLink", Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag link = tag.getCompound("TeleportLink");
        ResourceLocation dimension = ResourceLocation.tryParse(link.getString("Dimension"));
        if (dimension == null || !link.contains("Pos", Tag.TAG_LONG)
                || !link.hasUUID("CenterId")) {
            return;
        }
        teleportLinkedDimension = dimension;
        teleportLinkedPos = BlockPos.of(link.getLong("Pos"));
        teleportLinkedCenterId = link.getUUID("CenterId");
        teleportSourceCenterId = link.hasUUID("SourceCenterId")
                ? link.getUUID("SourceCenterId") : null;
    }

    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeBoolean(allowHostileMobs);
        buffer.writeBoolean(allowPassiveMobs);
        buffer.writeCollection(getBlacklistedPlayers(), (buf, value) -> buf.writeUtf(value));
        buffer.writeBoolean(particlesEnabled);
        buffer.writeInt(glyphColor);
        buffer.writeResourceLocation(centerTypeId);
        buffer.writeVarInt(radius);
        buffer.writeVarLong(energy);
        buffer.writeFloat(barrierHealth);
        buffer.writeBoolean(enabled);
        buffer.writeBoolean(active);
        buffer.writeVarInt(getCurrentFuelCostPerMinute());
        buffer.writeVarInt(passiveCircleCounts.attack());
        buffer.writeVarInt(passiveCircleCounts.defense());
        buffer.writeVarInt(passiveCircleCounts.regeneration());
        buffer.writeBoolean(ownerUuid != null);
        if (ownerUuid != null) {
            buffer.writeUUID(ownerUuid);
        }
        buffer.writeUtf(getOwnerDisplayLabel());
        buffer.writeCollection(excludedPlayers, (buf, value) -> buf.writeUUID(value));
        buffer.writeCollection(getControllerWhitelist(), (buf, value) -> buf.writeUtf(value));
        boolean canControl = menu instanceof CenterFuelMenu fuelMenu
                && fuelMenu.canControlCenter();
        boolean canManageControllers = menu instanceof CenterFuelMenu fuelMenu
                && fuelMenu.canManageControllers();
        buffer.writeBoolean(canControl);
        buffer.writeBoolean(canManageControllers);
        buffer.writeVarInt(getTeleportLinkMode());
        buffer.writeUtf(getTeleportDestinationLabel());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        CenterFuelMenu menu = new CenterFuelMenu(
                id, inventory, fuelSlot, reforgeWeaponSlot, octagonGrid,
                ContainerLevelAccess.create(level, worldPosition), worldPosition,
                allowHostileMobs, allowPassiveMobs, getBlacklistedPlayers(), glyphColor,
                centerTypeId, radius, energy, barrierHealth, enabled, active,
                getCurrentFuelCostPerMinute(), passiveCircleCounts,
                ownerUuid, excludedPlayers, getControllerWhitelist(),
                CenterTargetPolicy.canEdit(this, player),
                CenterTargetPolicy.canManageControllers(this, player),
                getTeleportLinkMode(), getTeleportDestinationLabel());
        menu.bindCenter(this);
        return menu;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DataVersion", DATA_VERSION);
        tag.putString("CenterType", centerTypeId.toString());
        if (unknownRecoveryType != null) {
            tag.putString("UnknownRecoveryType", unknownRecoveryType.toString());
        }
        tag.putInt("Radius", radius);
        tag.putLong("Energy", energy);
        tag.putLong("FuelCostRemainder", fuelCostRemainder);
        tag.putFloat("BarrierHealth", barrierHealth);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("Active", active);
        tag.putBoolean("ParticlesEnabled", particlesEnabled);
        tag.putInt("GlyphColor", glyphColor);
        tag.putBoolean("AllowHostileMobs", allowHostileMobs);
        tag.putBoolean("AllowPassiveMobs", allowPassiveMobs);
        tag.putString("CustomNameText", customName);
        tag.putInt("LegacyComboMigrationVersion", legacyComboMigrationVersion);
        tag.putBoolean("Temporary", temporary);
        tag.putLong("TemporaryExpiresAt", temporaryExpiresAt);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        saveCenterIdentityAndTeleportLink(tag);
        saveAccessLists(tag);
        tag.put("FuelInventory", fuelSlot.serializeNBT(registries));
        tag.put("ReforgeInventory", reforgeWeaponSlot.serializeNBT(registries));
        tag.put("OctagonGridInventory", octagonGrid.serializeNBT(registries));

        ListTag savedCircles = new ListTag();
        for (ConsumedCircle circle : transformationCircles) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", circle.pos().asLong());
            entry.putString("Type", circle.type().getSerializedName());
            savedCircles.add(entry);
        }
        tag.put("TransformationCircles", savedCircles);

        if (pendingTransformation != null) {
            CompoundTag pending = new CompoundTag();
            pending.putString("OutputType", pendingTransformation.outputType().toString());
            ListTag positions = new ListTag();
            for (BlockPos pos : pendingTransformation.consumedPositions()) {
                positions.add(LongTag.valueOf(pos.asLong()));
            }
            pending.put("ConsumedPositions", positions);
            tag.put("PendingTransformation", pending);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, Provider registries) {
        super.loadAdditional(tag, registries);
        int dataVersion = tag.getInt("DataVersion");
        ResourceLocation loadedType = ResourceLocation.tryParse(tag.getString("CenterType"));
        if (loadedType != null && MagicCenterTypeRegistry.contains(loadedType)) {
            centerTypeId = loadedType;
        } else if (loadedType != null) {
            unknownRecoveryType = loadedType;
            centerTypeId = MagicCenterTypeRegistry.BLANK;
        }
        if (tag.contains("UnknownRecoveryType", Tag.TAG_STRING)) {
            unknownRecoveryType = ResourceLocation.tryParse(tag.getString("UnknownRecoveryType"));
        }
        if (tag.contains("Radius", Tag.TAG_INT)) {
            radius = normalizeRadius(tag.getInt("Radius"));
        }
        if (tag.contains("Energy", Tag.TAG_LONG)) {
            long storedEnergy = Math.max(0L, tag.getLong("Energy"));
            energy = dataVersion < DATA_VERSION && storedEnergy > 0L
                    ? 1L + (storedEnergy - 1L) / LEGACY_ENERGY_PER_STONE
                    : storedEnergy;
        } else if (tag.contains("fuel", Tag.TAG_FLOAT)) {
            energy = Math.max(0L, (long)Math.ceil(tag.getFloat("fuel")));
        }
        fuelCostRemainder = Math.max(0L, tag.getLong("FuelCostRemainder")) % 1_200L;
        barrierHealth = Math.max(0.0F, tag.getFloat("BarrierHealth"));
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        active = tag.getBoolean("Active");
        particlesEnabled = tag.contains("ParticlesEnabled")
                ? tag.getBoolean("ParticlesEnabled")
                : !tag.contains("particlesEnabled") || tag.getBoolean("particlesEnabled");
        glyphColor = tag.contains("GlyphColor") ? tag.getInt("GlyphColor") & 0xFFFFFF
                : tag.contains("glyphColor") ? tag.getInt("glyphColor") & 0xFFFFFF : 0xFFFFFF;
        allowHostileMobs = tag.contains("AllowHostileMobs")
                ? tag.getBoolean("AllowHostileMobs")
                : !tag.contains("allowHostileMobs") || tag.getBoolean("allowHostileMobs");
        allowPassiveMobs = tag.contains("AllowPassiveMobs")
                ? tag.getBoolean("AllowPassiveMobs")
                : !tag.contains("allowPassiveMobs") || tag.getBoolean("allowPassiveMobs");
        customName = tag.getString("CustomNameText");
        legacyComboMigrationVersion = tag.getInt("LegacyComboMigrationVersion");
        temporary = tag.getBoolean("Temporary");
        temporaryExpiresAt = temporary
                ? tag.getLong("TemporaryExpiresAt")
                : -1L;
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        loadCenterIdentityAndTeleportLink(tag);

        loadAccessLists(tag);

        List<ConsumedCircle> loadedCircles = new ArrayList<>();
        ListTag savedCircles = tag.getList("TransformationCircles", Tag.TAG_COMPOUND);
        for (int i = 0; i < savedCircles.size(); i++) {
            CompoundTag entry = savedCircles.getCompound(i);
            if (entry.contains("Pos", Tag.TAG_LONG)) {
                loadedCircles.add(new ConsumedCircle(BlockPos.of(entry.getLong("Pos")),
                        parseCircleType(entry.getString("Type"))));
            }
        }
        transformationCircles = List.copyOf(loadedCircles);

        if (tag.contains("FuelInventory", Tag.TAG_COMPOUND)) {
            fuelSlot.deserializeNBT(registries, tag.getCompound("FuelInventory"));
        } else if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            fuelSlot.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("ReforgeInventory", Tag.TAG_COMPOUND)) {
            reforgeWeaponSlot.deserializeNBT(registries, tag.getCompound("ReforgeInventory"));
        }
        if (tag.contains("OctagonGridInventory", Tag.TAG_COMPOUND)) {
            octagonGrid.deserializeNBT(registries, tag.getCompound("OctagonGridInventory"));
        }

        pendingTransformation = null;
        if (tag.contains("PendingTransformation", Tag.TAG_COMPOUND)) {
            CompoundTag pending = tag.getCompound("PendingTransformation");
            ResourceLocation output = ResourceLocation.tryParse(pending.getString("OutputType"));
            if (output != null) {
                List<BlockPos> positions = new ArrayList<>();
                ListTag savedPositions = pending.getList("ConsumedPositions", Tag.TAG_LONG);
                for (int i = 0; i < savedPositions.size(); i++) {
                    positions.add(BlockPos.of(((LongTag)savedPositions.get(i)).getAsLong()));
                }
                pendingTransformation = new PendingTransformation(output, List.copyOf(positions));
            }
        }
        passiveCircleCounts = CircleCounts.ZERO;
        passiveCacheDirty = true;
        lastPassiveScan = Long.MIN_VALUE;
    }

    private static void readPlayerNameSet(ListTag list, Set<String> target,
                                          int maxSize, Set<String> ignoredNames) {
        for (int i = 0; i < list.size() && target.size() < maxSize; i++) {
            String value = list.getString(i);
            if (isValidPlayerName(value)
                    && findIgnoreCase(target, value) == null
                    && findIgnoreCase(ignoredNames, value) == null) {
                target.add(value);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("CenterType", centerTypeId.toString());
        tag.putInt("Radius", radius);
        tag.putLong("Energy", energy);
        tag.putFloat("BarrierHealth", barrierHealth);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("Active", active);
        tag.putBoolean("ParticlesEnabled", particlesEnabled);
        tag.putInt("GlyphColor", glyphColor);
        tag.putBoolean("AuthorizedPlayerInside", authorizedPlayerInside);
        tag.putLong("AuthorizedEntryGameTime", authorizedEntryGameTime);
        tag.putBoolean("Temporary", temporary);
        if (temporary) {
            tag.putLong("TemporaryExpiresAt", temporaryExpiresAt);
        }
        if (teleportChannelPlayer != null) {
            tag.putUUID("TeleportChannelPlayer", teleportChannelPlayer);
            tag.putLong("TeleportChannelStartTime", teleportChannelStartTime);
            tag.putInt("TeleportChannelDuration", teleportChannelDurationTicks);
        }
        tag.put("ReforgeInventory", reforgeWeaponSlot.serializeNBT(registries));
        tag.put("OctagonGridInventory", octagonGrid.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, Provider registries) {
        readClientTag(tag, registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             Provider registries) {
        readClientTag(packet.getTag(), registries);
    }

    private void readClientTag(CompoundTag tag, Provider registries) {
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("CenterType"));
        centerTypeId = type != null && MagicCenterTypeRegistry.contains(type)
                ? type : MagicCenterTypeRegistry.BLANK;
        radius = normalizeRadius(tag.getInt("Radius"));
        energy = Math.max(0L, tag.getLong("Energy"));
        barrierHealth = Math.max(0.0F, tag.getFloat("BarrierHealth"));
        enabled = tag.getBoolean("Enabled");
        active = tag.getBoolean("Active");
        particlesEnabled = tag.getBoolean("ParticlesEnabled");
        glyphColor = tag.getInt("GlyphColor") & 0xFFFFFF;
        authorizedPlayerInside = tag.getBoolean("AuthorizedPlayerInside");
        authorizedEntryGameTime = tag.contains("AuthorizedEntryGameTime", Tag.TAG_LONG)
                ? tag.getLong("AuthorizedEntryGameTime") : -1L;
        temporary = tag.getBoolean("Temporary");
        temporaryExpiresAt = temporary && tag.contains("TemporaryExpiresAt", Tag.TAG_LONG)
                ? tag.getLong("TemporaryExpiresAt") : -1L;
        teleportChannelPlayer = tag.hasUUID("TeleportChannelPlayer")
                ? tag.getUUID("TeleportChannelPlayer") : null;
        teleportChannelStartTime = teleportChannelPlayer == null ? -1L
                : tag.getLong("TeleportChannelStartTime");
        teleportChannelDurationTicks = teleportChannelPlayer == null ? 0
                : Math.max(1, tag.getInt("TeleportChannelDuration"));
        if (tag.contains("ReforgeInventory", Tag.TAG_COMPOUND)) {
            reforgeWeaponSlot.deserializeNBT(registries, tag.getCompound("ReforgeInventory"));
        } else {
            reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
        if (tag.contains("OctagonGridInventory", Tag.TAG_COMPOUND)) {
            octagonGrid.deserializeNBT(registries, tag.getCompound("OctagonGridInventory"));
        } else {
            clearOctagonGrid();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        // A temporary center uses cloned portable configuration. Never turn its
        // flags (or any other cloned contents) into real item drops, regardless
        // of whether it expired, was exploded, or was removed by another mod.
        if (suppressContentsDrop || temporary) {
            fuelSlot.setStackInSlot(0, ItemStack.EMPTY);
            reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
            clearOctagonGrid();
            energy = 0L;
            return;
        }
        for (int slot = 0; slot < fuelSlot.getSlots(); slot++) {
            ItemStack stack = fuelSlot.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), stack);
            }
        }
        for (int slot = 0; slot < reforgeWeaponSlot.getSlots(); slot++) {
            ItemStack stack = reforgeWeaponSlot.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), stack);
            }
        }
        if (suppressOctagonDrop) {
            clearOctagonGrid();
        } else {
            for (int slot = 0; slot < octagonGrid.getSlots(); slot++) {
                ItemStack stack = octagonGrid.extractItem(slot, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                            worldPosition.getZ(), stack);
                }
            }
        }
        long wholeStones = energy / CenterFuelController.ENERGY_PER_STONE;
        energy = 0L;
        while (wholeStones > 0L) {
            int count = (int)Math.min(64L, wholeStones);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                    worldPosition.getZ(), new ItemStack(ModItems.SPIRIT_STONE.get(), count));
            wholeStones -= count;
        }
    }

    public CompoundTag createPortableConfigurationTag(Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DATA_VERSION);
        tag.putString("CenterType", centerTypeId.toString());
        tag.putInt("Radius", radius);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("ParticlesEnabled", particlesEnabled);
        tag.putInt("GlyphColor", glyphColor);
        tag.putBoolean("AllowHostileMobs", allowHostileMobs);
        tag.putBoolean("AllowPassiveMobs", allowPassiveMobs);
        tag.putString("CustomNameText", customName);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        saveCenterIdentityAndTeleportLink(tag);
        saveAccessLists(tag);
        tag.put("OctagonGridInventory", octagonGrid.serializeNBT(registries));
        return tag;
    }

    public void applyPortableConfigurationTag(CompoundTag tag, Provider registries) {
        loadAdditional(tag.copy(), registries);
        radius = normalizeRadius(Math.min(5, radius));
        energy = 0L;
        fuelCostRemainder = 0L;
        barrierHealth = 0.0F;
        active = false;
        temporary = false;
        temporaryExpiresAt = -1L;
        suppressContentsDrop = false;
        suppressOctagonDrop = false;
        pendingTransformation = null;
        transformationCircles = List.of();
        fuelSlot.setStackInSlot(0, ItemStack.EMPTY);
        reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
        setChangedAndSync();
    }

    public boolean isTemporary() {
        return temporary;
    }

    public long getTemporaryExpiresAt() {
        return temporaryExpiresAt;
    }

    public void markTemporary(long expiresAt) {
        centerId = UUID.randomUUID();
        teleportLinkedDimension = null;
        teleportLinkedPos = null;
        teleportLinkedCenterId = null;
        teleportSourceCenterId = null;
        teleportChannelPlayer = null;
        teleportChannelStartTime = -1L;
        teleportChannelDurationTicks = 0;
        teleportChannelStartHealth = 0.0F;
        teleportChannelObservedHurtTime = 0;
        temporary = true;
        temporaryExpiresAt = Math.max(0L, expiresAt);
        active = enabled;
        energy = 0L;
        fuelCostRemainder = 0L;
        barrierHealth = 0.0F;
        pendingTransformation = null;
        transformationCircles = List.of();
        fuelSlot.setStackInSlot(0, ItemStack.EMPTY);
        reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
        setChangedAndSync();
    }

    private void clearOctagonGrid() {
        for (int slot = 0; slot < octagonGrid.getSlots(); slot++) {
            if (!octagonGrid.getStackInSlot(slot).isEmpty()) {
                octagonGrid.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public void removeForPacking() {
        suppressOctagonDrop = true;
        if (level != null && !level.isClientSide) {
            level.removeBlock(worldPosition, false);
        }
    }

    public void removeWithoutDrops() {
        suppressContentsDrop = true;
        if (level != null && !level.isClientSide) {
            if (temporary && MagicCenterTypeRegistry.TELEPORT.equals(centerTypeId)) {
                TeleportCenterBehavior.forgetAutomaticCenter(this);
                unlinkTeleportPair();
            }
            level.removeBlock(worldPosition, false);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public record CircleCounts(int attack, int defense, int regeneration) {
        public static final CircleCounts ZERO = new CircleCounts(0, 0, 0);
    }

    public record PendingTransformation(ResourceLocation outputType,
                                        List<BlockPos> consumedPositions) {
    }

    public record ConsumedCircle(BlockPos pos, CircleType type) {
    }

    public record ResolvedTeleportTarget(ServerLevel level,
                                         MagicCenterBlockEntity center) {
    }

    private record LegacyComboMatch(BlockPos comboPos, ResourceLocation outputType,
                                    List<BlockPos> circlePositions) {
    }
}
