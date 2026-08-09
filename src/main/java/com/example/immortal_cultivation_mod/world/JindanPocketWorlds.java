package com.example.immortal_cultivation_mod.world;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.mixin.MinecraftServerAccessor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class JindanPocketWorlds {
    public static final ResourceKey<Level> TEMPLATE_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "jindan"));

    private static final String POCKET_PREFIX = "jindan_pockets/";
    private static final String GENERATION_TAG = "ImmortalCultivationJindanAnchorGeneration";

    private JindanPocketWorlds() {
    }

    public static ResourceKey<Level> keyFor(ServerPlayer player) {
        return keyFor(player.getUUID(), generation(player));
    }

    public static ResourceKey<Level> keyFor(UUID owner, int generation) {
        return ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID,
                        POCKET_PREFIX + owner + "/" + Math.max(0, generation)));
    }

    public static boolean isPocketDimension(ResourceKey<Level> dimension) {
        ResourceLocation id = dimension.location();
        return ImmortalCultivationMod.MODID.equals(id.getNamespace())
                && id.getPath().startsWith(POCKET_PREFIX);
    }

    public static boolean isJindanDimension(ResourceKey<Level> dimension) {
        return TEMPLATE_DIMENSION.equals(dimension) || isPocketDimension(dimension);
    }

    public static boolean isOwnersPocket(ServerPlayer player, ResourceKey<Level> dimension) {
        return keyFor(player).equals(dimension);
    }

    public static ServerLevel getOrCreate(ServerPlayer player) {
        int generation = generation(player);
        JindanPocketWorldData.get(player.server).setActiveGeneration(player.getUUID(), generation);
        return getOrCreate(player.server, keyFor(player.getUUID(), generation));
    }

    public static void setActiveGeneration(ServerPlayer player, int generation) {
        JindanPocketWorldData.get(player.server)
                .setActiveGeneration(player.getUUID(), Math.max(0, generation));
    }

    public static void loadSavedPockets(MinecraftServer server) {
        for (Map.Entry<UUID, Integer> entry
                : JindanPocketWorldData.get(server).activeGenerations().entrySet()) {
            getOrCreate(server, keyFor(entry.getKey(), entry.getValue()));
        }
    }

    @SuppressWarnings("deprecation")
    public static ServerLevel getOrCreate(MinecraftServer server, ResourceKey<Level> key) {
        ServerLevel existing = server.getLevel(key);
        if (existing != null) {
            return existing;
        }
        if (!isPocketDimension(key)) {
            throw new IllegalArgumentException("Not a Jindan pocket dimension: " + key.location());
        }

        Registry<LevelStem> stems = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        LevelStem template = stems.get(TEMPLATE_DIMENSION.location());
        if (template == null) {
            throw new IllegalStateException("Missing Jindan level stem " + TEMPLATE_DIMENSION.location());
        }

        MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
        ServerLevel level = new ServerLevel(
                server,
                accessor.immortalCultivation$getExecutor(),
                accessor.immortalCultivation$getStorageSource(),
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
                key,
                template,
                LoggerChunkProgressListener.createCompleted(),
                server.getWorldData().isDebugWorld(),
                BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()),
                List.of(),
                false,
                server.overworld().getRandomSequences());

        server.overworld().getWorldBorder().addListener(
                new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        synchronized (server) {
            ServerLevel raced = server.getLevel(key);
            if (raced != null) {
                try {
                    level.close();
                } catch (Exception ignored) {
                }
                return raced;
            }
            server.forgeGetWorldMap().put(key, level);
            server.markWorldsDirty();
        }
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }

    public static void migrateLegacyPocket(
            ServerPlayer player,
            ServerLevel privateLevel,
            BlockPos anchor,
            boolean hadLegacyAnchor
    ) {
        JindanPocketWorldData savedData = JindanPocketWorldData.get(player.server);
        String pocketId = privateLevel.dimension().location().toString();
        if (savedData.isMigrated(pocketId)) {
            return;
        }

        ServerLevel legacy = player.server.getLevel(TEMPLATE_DIMENSION);
        if (hadLegacyAnchor && legacy != null && legacy != privateLevel) {
            int chunkCount = Math.max(1, JindanEntity.generatedChunkCount(player));
            ChunkPos origin = new ChunkPos(anchor);
            for (int index = 0; index < chunkCount; index++) {
                ChunkOffset offset = spiralOffset(index);
                copyChunk(
                        legacy,
                        privateLevel,
                        origin.x + offset.x(),
                        origin.z + offset.z(),
                        origin.x + offset.x(),
                        origin.z + offset.z());
            }
        }
        savedData.markMigrated(pocketId);
    }

    private static int generation(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(GENERATION_TAG));
    }

    private static void copyChunk(
            ServerLevel source,
            ServerLevel destination,
            int sourceChunkX,
            int sourceChunkZ,
            int destinationChunkX,
            int destinationChunkZ
    ) {
        source.getChunk(sourceChunkX, sourceChunkZ);
        destination.getChunk(destinationChunkX, destinationChunkZ);

        int minY = Math.max(source.getMinBuildHeight(), destination.getMinBuildHeight());
        int maxY = Math.min(source.getMaxBuildHeight(), destination.getMaxBuildHeight());
        BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos destinationPos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int sourceX = (sourceChunkX << 4) + localX;
            int destinationX = (destinationChunkX << 4) + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int sourceZ = (sourceChunkZ << 4) + localZ;
                int destinationZ = (destinationChunkZ << 4) + localZ;
                for (int y = minY; y < maxY; y++) {
                    sourcePos.set(sourceX, y, sourceZ);
                    destinationPos.set(destinationX, y, destinationZ);
                    BlockState state = source.getBlockState(sourcePos);
                    destination.setBlock(destinationPos, state, 2);

                    BlockEntity sourceEntity = source.getBlockEntity(sourcePos);
                    if (sourceEntity == null) {
                        continue;
                    }
                    CompoundTag entityTag = sourceEntity.saveWithFullMetadata(source.registryAccess());
                    entityTag.putInt("x", destinationX);
                    entityTag.putInt("y", y);
                    entityTag.putInt("z", destinationZ);
                    BlockEntity copied = BlockEntity.loadStatic(
                            destinationPos.immutable(), state, entityTag, destination.registryAccess());
                    if (copied != null) {
                        destination.setBlockEntity(copied);
                    }
                }
            }
        }
    }

    private static ChunkOffset spiralOffset(int index) {
        if (index <= 0) {
            return new ChunkOffset(0, 0);
        }
        int ring = (int) Math.ceil((Math.sqrt(index + 1.0D) - 1.0D) / 2.0D);
        int side = ring * 2;
        int max = (ring * 2 + 1) * (ring * 2 + 1) - 1;
        int offset = max - index;
        if (offset < side) {
            return new ChunkOffset(ring - offset, -ring);
        }
        offset -= side;
        if (offset < side) {
            return new ChunkOffset(-ring, -ring + offset);
        }
        offset -= side;
        if (offset < side) {
            return new ChunkOffset(-ring + offset, ring);
        }
        offset -= side;
        return new ChunkOffset(ring, ring - offset);
    }

    private record ChunkOffset(int x, int z) {
    }
}
