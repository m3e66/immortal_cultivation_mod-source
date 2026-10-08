package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.QiFireBlock;
import com.example.immortal_cultivation_mod.block.DingBlock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Shared flame render carrier; only special forge flames use the saved expiry deadline. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class SpiritFlameBlockEntity extends BlockEntity {
    public static final int LIFETIME_TICKS = 30 * 20;
    public static final int FORGE_LIFETIME_TICKS = 3 * 60 * 20;
    private static final int MIGRATION_CHUNKS_PER_TICK = 2;
    private static final int MIGRATION_RETRIES = 40;
    private static final Map<ServerLevel, LinkedHashMap<Long, Integer>> MIGRATIONS = new HashMap<>();
    private long expiresAt = -1;
    private long ignitedAt = Long.MIN_VALUE;

    public SpiritFlameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRIT_FLAME.get(), pos, state);
    }

    public FlameKind kind() {
        return getBlockState().getBlock() instanceof SpiritFlameBlock flame
                ? flame.kind() : FlameKind.MORTAL;
    }

    public boolean isQiFire() { return getBlockState().getBlock() instanceof QiFireBlock; }

    private static boolean hasAnimatedFlame(BlockState state) {
        return state.getBlock() instanceof SpiritFlameBlock || state.getBlock() instanceof QiFireBlock;
    }

    /** Absolute world game time; -1 denotes a new or legacy flame awaiting its first server tick. */
    public long expiresAt() { return expiresAt; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritFlameBlockEntity flame) {
        if (level.isClientSide || flame.isRemoved() || flame.getLevel() != level
                || !flame.getBlockPos().equals(pos) || level.getBlockEntity(pos) != flame) return;
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof SpiritFlameBlock)
                || current.getBlock() != flame.getBlockState().getBlock()) return;
        long now = level.getGameTime();
        if (flame.ignitedAt == Long.MIN_VALUE) {
            // Old saves used a fixed three-minute deadline. Preserve their elapsed age.
            flame.ignitedAt = flame.expiresAt >= 0 ? flame.expiresAt - FORGE_LIFETIME_TICKS : now;
            flame.setChanged();
        }
        int duration = level.getBlockState(pos.above()).getBlock() instanceof DingBlock
                ? FORGE_LIFETIME_TICKS : LIFETIME_TICKS;
        long deadline = flame.ignitedAt > Long.MAX_VALUE - duration ? Long.MAX_VALUE : flame.ignitedAt + duration;
        if (flame.expiresAt != deadline) {
            flame.expiresAt = deadline;
            flame.setChanged();
        }
        if (now >= flame.expiresAt) level.removeBlock(pos, false);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (expiresAt >= 0) tag.putLong("ExpiresAt", expiresAt);
        if (ignitedAt != Long.MIN_VALUE) tag.putLong("IgnitedAt", ignitedAt);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        expiresAt = tag.contains("ExpiresAt", Tag.TAG_LONG) ? Math.max(-1, tag.getLong("ExpiresAt")) : -1;
        ignitedAt = tag.contains("IgnitedAt", Tag.TAG_LONG)
                ? Math.max(-FORGE_LIFETIME_TICKS, tag.getLong("IgnitedAt")) : Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onChunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        // Queueing may happen before FULL promotion. Terrain access stays on the world server thread.
        Runnable enqueue = () -> {
            if (level.getServer().getLevel(level.dimension()) != level || !containsAnimatedFlames(chunk)) return;
            synchronized (MIGRATIONS) {
                MIGRATIONS.computeIfAbsent(level, ignored -> new LinkedHashMap<>()).putIfAbsent(chunk.getPos().toLong(), 0);
            }
        };
        if (level.getServer().isSameThread()) enqueue.run();
        else level.getServer().execute(enqueue);
    }

    private static boolean containsAnimatedFlames(LevelChunk chunk) {
        for (var section : chunk.getSections()) {
            if (!section.hasOnlyAir() && section.maybeHas(SpiritFlameBlockEntity::hasAnimatedFlame)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        var work = new LinkedHashMap<Long, Integer>();
        synchronized (MIGRATIONS) {
            var pending = MIGRATIONS.get(level);
            if (pending == null) return;
            var iterator = pending.entrySet().iterator();
            while (iterator.hasNext() && work.size() < MIGRATION_CHUNKS_PER_TICK) {
                var entry = iterator.next();
                work.put(entry.getKey(), entry.getValue());
                iterator.remove();
            }
            if (pending.isEmpty()) MIGRATIONS.remove(level);
        }
        for (var entry : work.entrySet()) {
            ChunkPos position = new ChunkPos(entry.getKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(position.x, position.z);
            if ((chunk == null || !migrateLoadedChunk(level, chunk)) && entry.getValue() < MIGRATION_RETRIES) {
                synchronized (MIGRATIONS) {
                    MIGRATIONS.computeIfAbsent(level, ignored -> new LinkedHashMap<>())
                            .putIfAbsent(entry.getKey(), entry.getValue() + 1);
                }
            }
        }
    }

    /** Migrates only palette-positive sections of an already-loaded FULL chunk; never forces terrain loading. */
    public static boolean migrateLoadedChunk(ServerLevel level, LevelChunk chunk) {
        ChunkPos position = chunk.getPos();
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level
                || level.getChunkSource().getChunkNow(position.x, position.z) != chunk
                || !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) return false;
        var sections = chunk.getSections();
        boolean repaired = false;
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(SpiritFlameBlockEntity::hasAnimatedFlame)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (!hasAnimatedFlame(section.getBlockState(x, y, z))) continue;
                BlockPos pos = new BlockPos(position.getMinBlockX() + x, baseY + y, position.getMinBlockZ() + z);
                if (chunk.getBlockEntity(pos) == null) {
                    if (chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof SpiritFlameBlockEntity) {
                        repaired = true;
                        chunk.setUnsaved(true);
                    }
                }
            }
        }
        if (repaired) {
            // ChunkMap excludes pending first-time sends. Existing viewers need a complete
            // chunk packet: a standalone BE update cannot create their missing render carrier.
            var viewers = level.getChunkSource().chunkMap.getPlayers(position, false);
            if (!viewers.isEmpty()) {
                var packet = new ClientboundLevelChunkWithLightPacket(
                        chunk, level.getChunkSource().getLightEngine(), null, null);
                for (var viewer : viewers) {
                    if (viewer.serverLevel() == level) viewer.connection.send(packet);
                }
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onLevelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) synchronized (MIGRATIONS) { MIGRATIONS.remove(level); }
    }
}
