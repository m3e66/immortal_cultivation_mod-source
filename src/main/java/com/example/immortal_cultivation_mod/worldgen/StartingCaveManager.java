package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** One additional cave near the final world spawn, created only for new worlds. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class StartingCaveManager {
    static final int RANGE = 1000;
    static final ResourceLocation CAVE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cave");
    private static final TagKey<Biome> CAVE_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "has_structure/cave"));
    private static final String DATA_NAME = "immortal_cultivation_starting_cave";
    private static final SavedData.Factory<State> FACTORY = new SavedData.Factory<>(State::new, State::load);

    private StartingCaveManager() {
    }

    @SubscribeEvent
    public static void onInitialSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && level.structureManager().shouldGenerateStructures()) {
            State state = state(level);
            state.pending = true;
            state.setDirty();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        if (level != null && level.structureManager().shouldGenerateStructures()) {
            ensure(level, level.getSharedSpawnPos(), state(level));
        }
    }

    static State state(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /** Reads the guaranteed cave's saved position without locating or generating chunks. */
    public static Optional<BlockPos> position(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return Optional.empty();
        State state = state(level);
        return state.placed ? Optional.of(state.position) : Optional.empty();
    }

    static List<ChunkPos> candidateChunks(BlockPos spawn, long seed) {
        var result = new ArrayList<ChunkPos>();
        double phase = RandomSource.create(seed ^ 0x5354415254434156L).nextDouble() * Math.PI * 2;
        for (int radius : new int[]{192, 448, 768}) {
            for (int direction = 0; direction < 8; direction++) {
                double angle = phase + direction * Math.PI / 4;
                result.add(new ChunkPos(new BlockPos(
                        spawn.getX() + (int)Math.round(Math.cos(angle) * radius), 0,
                        spawn.getZ() + (int)Math.round(Math.sin(angle) * radius))));
            }
        }
        return List.copyOf(result);
    }

    static boolean withinRange(BlockPos spawn, BoundingBox bounds) {
        for (int x : new int[]{bounds.minX(), bounds.maxX()}) {
            for (int z : new int[]{bounds.minZ(), bounds.maxZ()}) {
                long dx = (long)x - spawn.getX(), dz = (long)z - spawn.getZ();
                if (dx * dx + dz * dz > (long)RANGE * RANGE) return false;
            }
        }
        return true;
    }

    static void ensure(ServerLevel level, BlockPos spawn, State state) {
        if (!state.pending || state.placed) return;
        Structure cave = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(CAVE);
        if (cave == null) {
            LogUtils.getLogger().warn("Starting cave could not be placed: cave structure is missing");
            return;
        }
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        var candidates = new ArrayList<Candidate>();
        // Generate candidate layouts from terrain noise without loading a thousand-block area.
        for (ChunkPos chunk : candidateChunks(spawn, level.getSeed())) {
            StructureStart start = cave.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    randomState, level.getStructureManager(), level.getSeed(), chunk, 0, level, biome -> true);
            if (!start.isValid()) continue;
            BoundingBox bounds = start.getBoundingBox();
            // Terrain-adaptation padding may extend below the world without any cave block doing so.
            if (!withinRange(spawn, bounds) || start.getPieces().stream().anyMatch(piece ->
                    piece.getBoundingBox().minY() < level.getMinBuildHeight()
                            || piece.getBoundingBox().maxY() >= level.getMaxBuildHeight())
                    || !level.getWorldBorder().isWithinBounds(new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()))
                    || !level.getWorldBorder().isWithinBounds(new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()))) continue;
            int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
            for (int x : new int[]{bounds.minX(), bounds.maxX()}) {
                for (int z : new int[]{bounds.minZ(), bounds.maxZ()}) {
                    int height = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                    lowest = Math.min(lowest, height);
                    highest = Math.max(highest, height);
                }
            }
            BlockPos center = bounds.getCenter();
            boolean mountain = generator.getBiomeSource().getNoiseBiome(center.getX() >> 2,
                    center.getY() >> 2, center.getZ() >> 2, randomState.sampler()).is(CAVE_BIOMES);
            int score = (lowest < generator.getSeaLevel() - 1 ? 10000 : 0)
                    + (mountain ? 0 : 100) + (highest - lowest) * 4;
            candidates.add(new Candidate(start, score));
        }
        candidates.sort(Comparator.comparingInt(Candidate::score));
        for (int index = 0; index < candidates.size(); index++) {
            Candidate candidate = candidates.get(index);
            StructureStart start = candidate.start();
            BoundingBox bounds = start.getBoundingBox();
            List<ChunkPos> chunks = ChunkPos.rangeClosed(new ChunkPos(bounds.minX() >> 4, bounds.minZ() >> 4),
                    new ChunkPos(bounds.maxX() >> 4, bounds.maxZ() >> 4)).toList();
            // Load every affected chunk first, so later terrain generation cannot overwrite the cave.
            for (ChunkPos chunk : chunks) level.getChunk(chunk.x, chunk.z);
            StructureStart existing = level.getChunk(start.getChunkPos().x, start.getChunkPos().z)
                    .getStartForStructure(cave);
            if (existing != null && existing.isValid() && withinRange(spawn, existing.getBoundingBox())) {
                state.complete(existing.getBoundingBox().getCenter());
                return;
            }
            boolean occupied = chunks.stream().anyMatch(chunk -> level.getChunk(chunk.x, chunk.z)
                    .getAllStarts().values().stream().anyMatch(other -> other.isValid()
                            && other.getBoundingBox().intersects(bounds)));
            // Prefer a clear site; for new-world terrain the final fallback still guarantees a cave.
            if (occupied && index + 1 < candidates.size()) continue;
            for (ChunkPos chunk : chunks) {
                start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                        new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ()), chunk);
            }
            ChunkPos origin = start.getChunkPos();
            level.structureManager().setStartForStructure(SectionPos.of(origin, 0), cave, start,
                    level.getChunk(origin.x, origin.z));
            for (ChunkPos chunk : chunks) {
                level.structureManager().addReferenceForStructure(SectionPos.of(chunk, 0), cave, origin.toLong(),
                        level.getChunk(chunk.x, chunk.z));
            }
            state.complete(bounds.getCenter());
            LogUtils.getLogger().info("Starting cave placed within {} blocks of world spawn at {}", RANGE, state.position);
            return;
        }
        LogUtils.getLogger().warn("No safe starting cave site within {} blocks of world spawn; placement remains pending", RANGE);
    }

    private record Candidate(StructureStart start, int score) {
    }

    static final class State extends SavedData {
        boolean pending;
        boolean placed;
        BlockPos position = BlockPos.ZERO;

        void complete(BlockPos position) {
            this.position = position.immutable();
            placed = true;
            pending = false;
            setDirty();
        }

        static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State state = new State();
            state.pending = tag.getBoolean("Pending");
            state.placed = tag.getBoolean("Placed");
            state.position = BlockPos.of(tag.getLong("Position"));
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("Pending", pending);
            tag.putBoolean("Placed", placed);
            tag.putLong("Position", position.asLong());
            return tag;
        }
    }
}
