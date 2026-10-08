package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.DeadSkeletonBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/** Runtime setup and block lock for the generated dead ruin. */
public final class DeadRuinProtection {
    private static final ResourceLocation DEAD_RUIN_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "dead_ruin1");
    private static final ResourceKey<LootTable> DEAD_RUIN_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "chests/dead_ruin1"));
    private static final String GUARDED_MESSAGE =
            "message.immortal_cultivation_mod.dead_ruin_guarded";
    private static final String UNLOCKED_MESSAGE =
            "message.immortal_cultivation_mod.dead_ruin_unlocked";

    /* Supplied structure-block reference: top-left (-311,-60,-91). */
    private static final int TEMPLATE_SIZE_X = 40;
    private static final int TEMPLATE_SIZE_Z = 41;
    /* Keep the skeleton exactly where it is stored in the supplied template. */
    private static final BlockPos SKELETON_LOCAL = new BlockPos(18, 4, 21);
    /* Requested center coordinate (-294,-59,-71), directly below the skeleton. */
    private static final BlockPos MAGIC_CENTER_LOCAL = new BlockPos(18, 1, 21);
    private static final int DISCOVERY_CHUNK_RADIUS = 2;
    private static final int TRAP_RADIUS = 21;
    private static final int AMPLIFIER_FLAGS = 4;
    private static final List<ResourceLocation> TRAP_TYPES = List.of(
            MagicCenterTypeRegistry.FROST,
            MagicCenterTypeRegistry.DECAY,
            MagicCenterTypeRegistry.SCORCHING,
            MagicCenterTypeRegistry.THUNDER);
    private static final ThreadLocal<Boolean> SETTING_UP =
            ThreadLocal.withInitial(() -> false);

    private DeadRuinProtection() {
    }

    /** Finds nearby generated starts early enough to configure them before entry. */
    public static void ensureNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Structure structure = deadRuinStructure(level);
        if (structure == null) {
            return;
        }
        ChunkPos centerChunk = new ChunkPos(player.blockPosition());
        Set<String> visited = new HashSet<>();
        for (int chunkX = centerChunk.x - DISCOVERY_CHUNK_RADIUS;
             chunkX <= centerChunk.x + DISCOVERY_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunk.z - DISCOVERY_CHUNK_RADIUS;
                 chunkZ <= centerChunk.z + DISCOVERY_CHUNK_RADIUS; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (StructureStart start : level.structureManager().startsForStructure(
                        new ChunkPos(chunkX, chunkZ), candidate -> candidate == structure)) {
                    String key = instanceKey(level, start);
                    if (start.isValid() && visited.add(key)) {
                        ensureConfigured(level, start);
                    }
                }
            }
        }
    }

    /** Returns whether this original template block is still sealed. */
    public static boolean isLocked(ServerLevel level, BlockPos pos) {
        if (SETTING_UP.get()) {
            return false;
        }
        StructureStart start = findDeadRuin(level, pos);
        if (start == null) {
            return false;
        }
        DeadRuinData data = DeadRuinData.get(level);
        String key = instanceKey(level, start);
        return !data.isUnlocked(key) && !data.isPlaced(key, pos);
    }

    /** Player/mob-placed blocks inside a sealed ruin remain normally breakable. */
    public static void recordPlacedBlock(ServerLevel level, BlockPos pos) {
        StructureStart start = findDeadRuin(level, pos);
        if (start == null || level.getBlockState(pos).isAir()) {
            return;
        }
        DeadRuinData data = DeadRuinData.get(level);
        String key = instanceKey(level, start);
        if (!data.isUnlocked(key)) {
            data.markPlaced(key, pos);
        }
    }

    public static boolean preventBreaking(ServerLevel level, BlockPos pos,
                                          ServerPlayer player) {
        StructureStart start = findDeadRuin(level, pos);
        if (start == null) {
            return false;
        }
        ensureConfigured(level, start);
        if (!isLocked(level, pos)) {
            return false;
        }
        player.displayClientMessage(Component.translatable(GUARDED_MESSAGE), true);
        return true;
    }

    /** Keeps the trap and reward closed while still allowing the unlocking corpse to open. */
    public static boolean preventProtectedInteraction(ServerLevel level, BlockPos pos,
                                                      ServerPlayer player) {
        StructureStart start = findDeadRuin(level, pos);
        if (start == null) {
            return false;
        }
        ensureConfigured(level, start);
        DeadRuinPlacement placement = placement(start);
        if (placement == null
                || DeadRuinData.get(level).isUnlocked(instanceKey(level, start))
                || pos.equals(placement.skeleton())) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean protectedInteraction = blockEntity instanceof Container
                || state.is(com.example.immortal_cultivation_mod.magicritual.block.ModBlocks
                .MAGIC_CENTER_BLOCK.get());
        if (!protectedInteraction) {
            return false;
        }
        player.displayClientMessage(Component.translatable(GUARDED_MESSAGE), true);
        return true;
    }

    /** The first successful inventory open of the specified skeleton drops the seal. */
    public static void skeletonOpened(ServerLevel level, BlockPos pos,
                                      ServerPlayer player) {
        StructureStart start = findDeadRuin(level, pos);
        if (start == null) {
            return;
        }
        ensureConfigured(level, start);
        DeadRuinPlacement placement = placement(start);
        if (placement == null || !placement.skeleton().equals(pos)
                || !(level.getBlockEntity(pos) instanceof DeadSkeletonBlockEntity)) {
            return;
        }
        String key = instanceKey(level, start);
        boolean firstUnlock = DeadRuinData.get(level).markUnlocked(key);
        breakMagicCenter(level, placement.magicCenter());
        if (firstUnlock) {
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.BLOCKS, 1.0F, 0.75F);
            player.displayClientMessage(Component.translatable(UNLOCKED_MESSAGE), true);
        }
    }

    private static boolean ensureConfigured(ServerLevel level, StructureStart start) {
        String key = instanceKey(level, start);
        DeadRuinData data = DeadRuinData.get(level);
        DeadRuinPlacement placement = placement(start);
        if (data.isInitialized(key)) {
            if (!data.isUnlocked(key)
                    && placement != null
                    && positionsLoaded(level, placement)
                    && (!(level.getBlockEntity(placement.magicCenter())
                    instanceof MagicCenterBlockEntity center)
                    || !center.isStructureTrap()
                    || center.getRadius() != TRAP_RADIUS)) {
                SETTING_UP.set(true);
                try {
                    configureMagicCenter(level, placement.magicCenter(), key);
                } finally {
                    SETTING_UP.set(false);
                }
            }
            return true;
        }
        if (placement == null || !positionsLoaded(level, placement)) {
            return false;
        }

        boolean configured = false;
        SETTING_UP.set(true);
        try {
            configureMagicCenter(level, placement.magicCenter(), key);
            configureLoot(level, placement.skeleton(), key);
            configured = level.getBlockEntity(placement.skeleton())
                    instanceof DeadSkeletonBlockEntity
                    && level.getBlockEntity(placement.magicCenter())
                    instanceof MagicCenterBlockEntity;
        } finally {
            SETTING_UP.set(false);
        }
        if (configured) {
            data.markInitialized(key);
        }
        return configured;
    }

    private static void configureMagicCenter(ServerLevel level, BlockPos pos, String key) {
        BlockState state = com.example.immortal_cultivation_mod.magicritual.block.ModBlocks
                .MAGIC_CENTER_BLOCK.get().defaultBlockState()
                .setValue(MagicCenterBlock.RADIUS, TRAP_RADIUS);
        if (!level.getBlockState(pos).is(state.getBlock())) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        } else if (!level.getBlockState(pos).equals(state)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        if (level.getBlockEntity(pos) instanceof MagicCenterBlockEntity center) {
            ResourceLocation type = TRAP_TYPES.get(Math.floorMod(key.hashCode(), TRAP_TYPES.size()));
            center.configureStructureTrap(type, TRAP_RADIUS, AMPLIFIER_FLAGS);
        }
    }

    private static void configureLoot(ServerLevel level, BlockPos pos, String key) {
        if (!(level.getBlockEntity(pos) instanceof DeadSkeletonBlockEntity skeleton)) {
            return;
        }
        skeleton.setLootTable(null);
        skeleton.clearContent();
        skeleton.setLootTable(DEAD_RUIN_LOOT);
        skeleton.setLootTableSeed(mixSeed(level.getSeed(), key));
        skeleton.setChanged();
    }

    private static void breakMagicCenter(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MagicCenterBlockEntity center
                && center.isStructureTrap()) {
            level.destroyBlock(pos, false);
        }
    }

    private static long mixSeed(long worldSeed, String key) {
        long hash = key.hashCode();
        hash ^= hash << 21;
        hash ^= hash >>> 35;
        hash ^= hash << 4;
        return worldSeed ^ hash;
    }

    private static boolean positionsLoaded(ServerLevel level, DeadRuinPlacement placement) {
        return level.hasChunkAt(placement.skeleton())
                && level.hasChunkAt(placement.magicCenter());
    }

    @Nullable
    private static DeadRuinPlacement placement(StructureStart start) {
        PoolElementStructurePiece poolPiece = null;
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece candidate) {
                poolPiece = candidate;
                break;
            }
        }
        if (poolPiece == null) {
            return null;
        }
        BoundingBox bounds = poolPiece.getBoundingBox();
        Rotation rotation = poolPiece.getRotation();
        return new DeadRuinPlacement(
                transform(bounds, rotation, SKELETON_LOCAL),
                transform(bounds, rotation, MAGIC_CENTER_LOCAL));
    }

    private static BlockPos transform(BoundingBox bounds, Rotation rotation, BlockPos local) {
        int offsetX;
        int offsetZ;
        switch (rotation) {
            case CLOCKWISE_90 -> {
                offsetX = TEMPLATE_SIZE_Z - 1 - local.getZ();
                offsetZ = local.getX();
            }
            case CLOCKWISE_180 -> {
                offsetX = TEMPLATE_SIZE_X - 1 - local.getX();
                offsetZ = TEMPLATE_SIZE_Z - 1 - local.getZ();
            }
            case COUNTERCLOCKWISE_90 -> {
                offsetX = local.getZ();
                offsetZ = TEMPLATE_SIZE_X - 1 - local.getX();
            }
            default -> {
                offsetX = local.getX();
                offsetZ = local.getZ();
            }
        }
        return new BlockPos(bounds.minX() + offsetX,
                bounds.minY() + local.getY(), bounds.minZ() + offsetZ);
    }

    @Nullable
    private static StructureStart findDeadRuin(ServerLevel level, BlockPos pos) {
        Structure structure = deadRuinStructure(level);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(pos, structure);
        return start.isValid() ? start : null;
    }

    @Nullable
    private static Structure deadRuinStructure(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(DEAD_RUIN_ID);
    }

    private static String instanceKey(ServerLevel level, StructureStart start) {
        BoundingBox bounds = start.getBoundingBox();
        return level.dimension().location() + "|"
                + bounds.minX() + "," + bounds.minY() + "," + bounds.minZ()
                + "," + bounds.maxX() + "," + bounds.maxY() + "," + bounds.maxZ();
    }

    private record DeadRuinPlacement(BlockPos skeleton, BlockPos magicCenter) {
    }

    private static final class DeadRuinData extends SavedData {
        private static final String DATA_NAME = "immortal_cultivation_dead_ruins";
        private static final SavedData.Factory<DeadRuinData> FACTORY =
                new SavedData.Factory<>(DeadRuinData::new, DeadRuinData::load);
        private final Set<String> initialized = new HashSet<>();
        private final Set<String> unlocked = new HashSet<>();
        private final Set<String> placedBlocks = new HashSet<>();

        private static DeadRuinData get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(FACTORY, DATA_NAME);
        }

        private static DeadRuinData load(CompoundTag tag, HolderLookup.Provider registries) {
            DeadRuinData data = new DeadRuinData();
            loadSet(tag.getCompound("Initialized"), data.initialized);
            loadSet(tag.getCompound("Unlocked"), data.unlocked);
            loadSet(tag.getCompound("PlacedBlocks"), data.placedBlocks);
            return data;
        }

        private static void loadSet(CompoundTag tag, Set<String> target) {
            for (String key : tag.getAllKeys()) {
                if (tag.getBoolean(key)) {
                    target.add(key);
                }
            }
        }

        private boolean isInitialized(String key) {
            return initialized.contains(key);
        }

        private boolean isUnlocked(String key) {
            return unlocked.contains(key);
        }

        private boolean isPlaced(String instanceKey, BlockPos pos) {
            return placedBlocks.contains(placedKey(instanceKey, pos));
        }

        private void markInitialized(String key) {
            if (initialized.add(key)) {
                setDirty();
            }
        }

        private void markPlaced(String instanceKey, BlockPos pos) {
            if (placedBlocks.add(placedKey(instanceKey, pos))) {
                setDirty();
            }
        }

        private boolean markUnlocked(String key) {
            boolean changed = unlocked.add(key);
            changed |= placedBlocks.removeIf(placed -> placed.startsWith(key + "@"));
            if (changed) {
                setDirty();
            }
            return changed;
        }

        private static String placedKey(String instanceKey, BlockPos pos) {
            return instanceKey + "@" + pos.asLong();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("Initialized", saveSet(initialized));
            tag.put("Unlocked", saveSet(unlocked));
            tag.put("PlacedBlocks", saveSet(placedBlocks));
            return tag;
        }

        private static CompoundTag saveSet(Set<String> values) {
            CompoundTag tag = new CompoundTag();
            values.forEach(value -> tag.putBoolean(value, true));
            return tag;
        }
    }
}
