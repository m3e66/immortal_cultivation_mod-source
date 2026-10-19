package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.block.LingFireBlock;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Server acquisition sources. Never changes blocks, forces chunks, or scans the world each tick. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FlameAcquisition {
    static final String SOURCE_KEY = "icmFlameSource";
    static final String SOURCE_KIND = "icmFlameSourceKind";
    static final int SOLAR_RUIN_CHANCE = 3000;
    private static final String DEATH_CHECKED = "icmFlameDeathChecked";
    private static final int MAX_PENDING = 256;
    private static final int CHUNKS_PER_TICK = 2;
    private static final Map<ServerLevel, Pending> PENDING = new HashMap<>();

    private static final class Pending {
        final LinkedHashMap<Long, Integer> chunks = new LinkedHashMap<>();
        final ArrayDeque<Vec3> lightning = new ArrayDeque<>();
    }

    private FlameAcquisition() {}

    static FlameKind deathKind(LivingEntity victim) {
        if (victim instanceof com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity) return FlameKind.DRAGON;
        if (victim instanceof CrimsonScaleFlameLizardEntity) return FlameKind.CRIMSON;
        if (victim instanceof CultivationNpc npc
                && CultivationLevels.getRealmIndex(npc.npcRealm()) >= 3) {
            var roots = NpcProfile.spiritRootIds(npc.asNpcMob(), npc.npcArchetype());
            if (roots.contains(SpiritRoots.METAL) || roots.contains(SpiritRoots.FIRE)) return FlameKind.SOLAR;
        }
        if (UndeadControl.isUndeadServantType(victim)
                && CultivationLevels.getRealmIndex(UndeadControl.levelFor(victim)) >= 3) return FlameKind.NETHER;
        return null;
    }

    static int deathChance(LivingEntity victim, FlameKind kind) {
        if (kind == FlameKind.DRAGON) return 64;
        if (kind == FlameKind.CRIMSON) return 20;
        if (kind == FlameKind.NETHER && UndeadControl.isAtLeastRank(victim, "\u98de\u50f5")) return 10;
        return 40;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) return;
        onDefeated(event.getEntity(), level);
    }

    /** Capture defeats the spirit without firing a death event; it gets the same single flame roll. */
    public static void onDragonSpiritCaptured(com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity spirit) {
        if (spirit.level() instanceof ServerLevel level) onDefeated(spirit, level);
    }

    private static void onDefeated(LivingEntity victim, ServerLevel level) {
        FlameKind kind = deathKind(victim);
        if (kind == null || victim.getPersistentData().getBoolean(DEATH_CHECKED)) return;
        victim.getPersistentData().putBoolean(DEATH_CHECKED, true);
        String key = "death:" + kind.name() + ":" + victim.getUUID();
        if (roll(level.getSeed(), key, deathChance(victim, kind))) spawn(level, victim.position().add(0, .3, 0), kind, key);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLightningJoined(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LightningBolt)) return;
        // Entity/chunk join hooks can precede FULL chunk promotion. Check terrain next level tick.
        synchronized (PENDING) {
            Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
            if (pending.lightning.size() < MAX_PENDING) pending.lightning.add(event.getEntity().position());
        }
    }

    @SubscribeEvent
    public static void onChunkLoaded(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || !naturalDimension(level)) return;
        ChunkPos pos = event.getChunk().getPos();
        if (!roll(level.getSeed(), chunkKey("earth", pos), 64) && event.getChunk().getAllStarts().isEmpty()) return;
        synchronized (PENDING) {
            Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
            if (pending.chunks.size() < MAX_PENDING) pending.chunks.putIfAbsent(pos.toLong(), 0);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunks = new LinkedHashMap<Long, Integer>();
        var strikes = new ArrayDeque<Vec3>();
        synchronized (PENDING) {
            Pending pending = PENDING.get(level);
            if (pending == null) return;
            var iterator = pending.chunks.entrySet().iterator();
            while (iterator.hasNext() && chunks.size() < CHUNKS_PER_TICK) {
                var entry = iterator.next();
                chunks.put(entry.getKey(), entry.getValue());
                iterator.remove();
            }
            while (!pending.lightning.isEmpty() && strikes.size() < 4) strikes.add(pending.lightning.remove());
            if (pending.chunks.isEmpty() && pending.lightning.isEmpty()) PENDING.remove(level);
        }
        for (var entry : chunks.entrySet()) {
            ChunkPos pos = new ChunkPos(entry.getKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) {
                if (entry.getValue() < 40) synchronized (PENDING) {
                    Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
                    if (pending.chunks.size() < MAX_PENDING) pending.chunks.putIfAbsent(entry.getKey(), entry.getValue() + 1);
                }
            } else inspectNewChunk(level, chunk);
        }
        strikes.forEach(strike -> onWoodLightning(level, strike));
    }

    @SubscribeEvent
    public static void onLevelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) synchronized (PENDING) { PENDING.remove(level); }
    }

    private static boolean naturalDimension(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD) || level.dimension().equals(Level.NETHER);
    }

    static void inspectNewChunk(ServerLevel level, LevelChunk chunk) {
        if (!naturalDimension(level)) return;
        String earthKey = chunkKey("earth", chunk.getPos());
        if (roll(level.getSeed(), earthKey, 64) && FlameAcquisitionData.get(level).claim(earthKey)) {
            BlockPos vent = findEarthVent(level, chunk);
            if (vent != null) spawn(level, Vec3.atBottomCenterOf(vent).add(0, .2, 0), FlameKind.EARTH, earthKey);
        }
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (var entry : chunk.getAllStarts().entrySet()) {
            StructureStart start = entry.getValue();
            if (!start.isValid()) continue;
            ResourceLocation id = structures.getKey(entry.getKey());
            if (!isSolarRuin(id)) continue;
            String key = "ruin:" + id + ":" + start.getChunkPos().toLong();
            if (!FlameAcquisitionData.get(level).claim(key) || !roll(level.getSeed(), key, SOLAR_RUIN_CHANCE)) continue;
            // Cultivation ruins are valid spiritual sources in their own right.
            // Their shipped templates contain no vanilla metal or fire blocks.
            // Ordinary ruined portals still need their actual metal and heat.
            boolean requireMetalFire = !id.getNamespace().equals(ImmortalCultivationMod.MODID);
            BlockPos room = findStructureRoom(chunk, start.getBoundingBox(), requireMetalFire);
            if (room != null) spawn(level, Vec3.atBottomCenterOf(room).add(0, .2, 0), FlameKind.SOLAR, key);
        }
    }

    static boolean isSolarRuin(ResourceLocation id) {
        return id != null && (id.getNamespace().equals("minecraft") && id.getPath().startsWith("ruined_portal")
                || id.getNamespace().equals(ImmortalCultivationMod.MODID)
                && (id.getPath().equals("dead_ruin1") || id.getPath().equals("daoist_temple")));
    }

    /** At most 32 columns and 112 blocks per column, all inside this already-loaded chunk. */
    private static BlockPos findEarthVent(ServerLevel level, LevelChunk chunk) {
        int low = level.getMinBuildHeight() + 8;
        int high = Math.min(low + 111, level.getSeaLevel() - 8);
        for (int column = 0; column < 32; column++) {
            int x = chunk.getPos().getMinBlockX() + (column * 5 & 15);
            int z = chunk.getPos().getMinBlockZ() + (column * 7 / 16 + column * 3 & 15);
            for (int y = low; y <= high; y++) {
                BlockPos support = new BlockPos(x, y, z);
                if ((chunk.getBlockState(support).is(Blocks.MAGMA_BLOCK)
                        || chunk.getBlockState(support).getFluidState().is(FluidTags.LAVA))
                        && chunk.getBlockState(support.above()).isAir()) return support.above();
            }
            // Mountain vents are exposed magma above sea level, never arbitrary warm terrain.
            // ChunkAccess#getHeight already returns the occupied surface block, not the air above it.
            int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            BlockPos support = new BlockPos(x, surface, z);
            if (surface >= level.getSeaLevel() + 24 && chunk.getBlockState(support).is(Blocks.MAGMA_BLOCK)
                    && chunk.getBlockState(support.above()).isAir()) return support.above();
        }
        return null;
    }

    private static BlockPos findStructureRoom(LevelChunk chunk, BoundingBox bounds, boolean requireMetalFire) {
        int minX = Math.max(bounds.minX(), chunk.getPos().getMinBlockX());
        int maxX = Math.min(bounds.maxX(), chunk.getPos().getMaxBlockX());
        int minZ = Math.max(bounds.minZ(), chunk.getPos().getMinBlockZ());
        int maxZ = Math.min(bounds.maxZ(), chunk.getPos().getMaxBlockZ());
        int minY = Math.max(bounds.minY(), chunk.getMinBuildHeight());
        int maxY = Math.min(Math.min(bounds.maxY(), minY + 47), chunk.getMaxBuildHeight() - 2);
        BlockPos room = null;
        boolean metal = false, heat = false;
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            metal |= state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.IRON_BLOCK) || state.is(Blocks.COPPER_BLOCK);
            heat |= isLightningHeat(state) || state.is(Blocks.MAGMA_BLOCK);
            if (room == null && state.isAir() && chunk.getBlockState(pos.above()).isAir()
                    && chunk.getBlockState(pos.below()).isFaceSturdy(chunk, pos.below(), net.minecraft.core.Direction.UP)) room = pos;
        }
        return !requireMetalFire || metal && heat ? room : null;
    }

    /** Ordinary lightning can make Wood Flame only, never Thunder Flame. */
    static void onWoodLightning(ServerLevel level, Vec3 strike) {
        if (!naturalDimension(level)) return;
        BlockPos origin = BlockPos.containing(strike);
        for (int dy = 2; dy >= -12; dy--) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos log = origin.offset(dx, dy, dz);
            if (!loaded(level, log) || !level.getBlockState(log).is(ModBlocks.LINGMU_LOG.get()) || !matureSpiritwood(level, log)) continue;
            BlockPos open = findOpenAirNear(level, log);
            if (open == null) return;
            String key = chunkKey("wood", new ChunkPos(log));
            if (!FlameAcquisitionData.get(level).claim(key)) return;
            if (roll(level.getSeed(), key, 12)) spawn(level, Vec3.atBottomCenterOf(open).add(0, .2, 0), FlameKind.WOOD, key);
            return;
        }
    }

    static boolean matureSpiritwood(ServerLevel level, BlockPos log) {
        int below = 0, above = 0;
        while (below < 12 && loaded(level, log.below(below + 1))
                && level.getBlockState(log.below(below + 1)).is(ModBlocks.LINGMU_LOG.get())) below++;
        while (above < 12 && loaded(level, log.above(above + 1))
                && level.getBlockState(log.above(above + 1)).is(ModBlocks.LINGMU_LOG.get())) above++;
        if (below + above + 1 < 6) return false;
        BlockPos crown = log.above(above);
        int leaves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(crown.offset(-2, -1, -2), crown.offset(2, 2, 2))) {
            if (!loaded(level, pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.LINGMU_LEAVES.get()) && !state.getValue(LeavesBlock.PERSISTENT) && ++leaves >= 6) return true;
        }
        return false;
    }

    /** Called only by a real weapon-tribulation strike; other lightning has no Thunder path. */
    public static void onWeaponLightning(ServerLevel level, Vec3 strike) {
        BlockPos origin = BlockPos.containing(strike);
        for (int dy = -2; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (!loaded(level, pos) || !isLightningHeat(level.getBlockState(pos))) continue;
            String key = "thunder:" + pos.asLong();
            var data = FlameAcquisitionData.get(level);
            if (data.contains(key)) return;
            BlockPos open = findOpenAirNear(level, pos);
            if (open != null && spawn(level, Vec3.atBottomCenterOf(open).add(0, .2, 0), FlameKind.THUNDER, key)) data.claim(key);
            return;
        }
    }

    /** One very rare opportunity after a powerful successful trial, never from a failed or fatal trial. */
    public static void onPowerfulTribulationSurvived(ServerPlayer owner, Vec3 strike, UUID trialId,
                                                    int originalTier, double weaponPower) {
        if (owner == null || !owner.isAlive() || owner.isSpectator() || trialId == null
                || originalTier < 3 || !Double.isFinite(weaponPower) || weaponPower < 300) return;
        ServerLevel level = owner.serverLevel();
        String key = "heavenly:" + trialId;
        var data = FlameAcquisitionData.get(level);
        if (!data.claim(key) || !roll(level.getSeed(), key, 512)) return;
        BlockPos open = findOpenAirNear(level, BlockPos.containing(strike));
        if (open != null) spawn(level, Vec3.atBottomCenterOf(open).add(0, .35, 0), FlameKind.HEAVENLY, key);
    }

    /** Keep the essence outside solid trunks, leaves and fluid so the bottle has line of sight. */
    static BlockPos findOpenAirNear(ServerLevel level, BlockPos origin) {
        BlockPos above = origin.above();
        if (openAir(level, above)) return above;
        for (int radius = 1; radius <= 3; radius++) for (int dy = -1; dy <= 3; dy++)
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                BlockPos candidate = origin.offset(dx, dy, dz);
                if (openAir(level, candidate)) return candidate;
            }
        return null;
    }

    private static boolean openAir(ServerLevel level, BlockPos pos) {
        return loaded(level, pos) && loaded(level, pos.above())
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    static boolean isLightningHeat(BlockState state) {
        return state.is(BlockTags.FIRE) || state.getBlock() instanceof LingFireBlock || state.getFluidState().is(FluidTags.LAVA);
    }

    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static boolean spawn(ServerLevel level, Vec3 position, FlameKind kind, String key) {
        if (!loaded(level, BlockPos.containing(position))
                || !level.getEntitiesOfClass(SpiritFlameEntity.class, new AABB(position, position).inflate(8),
                        flame -> flame.kind() == kind).isEmpty()) return false;
        SpiritFlameEntity flame = SpiritFlameEntity.spawn(level, position, kind);
        if (flame != null) {
            flame.getPersistentData().putString(SOURCE_KEY, key);
            flame.getPersistentData().putString(SOURCE_KIND, kind.name());
            return true;
        }
        return false;
    }

    static String chunkKey(String kind, ChunkPos chunk) { return kind + ":" + chunk.toLong(); }

    /** Stable rare chances cannot be rerolled by unloading a chunk or restarting the server. */
    static boolean roll(long seed, String key, int denominator) {
        long value = seed ^ 0x9e3779b97f4a7c15L;
        for (int i = 0; i < key.length(); i++) value = (value ^ key.charAt(i)) * 0x100000001b3L;
        value ^= value >>> 30; value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27; value *= 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return Long.remainderUnsigned(value, denominator) == 0;
    }
}
