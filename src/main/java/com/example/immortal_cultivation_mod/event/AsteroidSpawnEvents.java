package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.AsteroidEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Maintains a small population of naturally drifting asteroids in the cosmic void. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class AsteroidSpawnEvents {
    public static final ResourceKey<Level> COSMIC_VOID = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cosmic_void"));

    public static final int MAX_NATURAL_ASTEROIDS = 12;
    public static final int SPAWN_INTERVAL_TICKS = 100;
    public static final float SPAWN_CHANCE = 0.35F;
    public static final double MIN_SPAWN_DISTANCE = 72.0D;
    public static final double MAX_SPAWN_DISTANCE = 128.0D;
    public static final double MAX_VERTICAL_OFFSET = 48.0D;
    public static final int SPAWN_ATTEMPTS = 4;
    private static final int CLEANUP_INTERVAL_TICKS = 20;
    private static final int BUILD_HEIGHT_MARGIN = 32;

    private AsteroidSpawnEvents() {
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(COSMIC_VOID)) {
            return;
        }

        if (level.getGameTime() % CLEANUP_INTERVAL_TICKS == 0L) {
            discardInactiveNaturalAsteroids(level);
        }
        if (level.getGameTime() % SPAWN_INTERVAL_TICKS != 0L
                || countAsteroids(level) >= MAX_NATURAL_ASTEROIDS) {
            return;
        }

        List<ServerPlayer> players = activePlayers(level);
        if (players.isEmpty()) {
            return;
        }

        RandomSource random = players.get(0).getRandom();
        if (random.nextFloat() >= SPAWN_CHANCE) {
            return;
        }
        ServerPlayer anchor = players.get(random.nextInt(players.size()));

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            Vec3 spawnPosition = randomSpawnPosition(level, anchor, random);
            BlockPos spawnBlock = BlockPos.containing(spawnPosition);
            if (!level.hasChunkAt(spawnBlock)
                    || !level.isPositionEntityTicking(spawnBlock)) {
                continue;
            }

            AsteroidEntity asteroid = AsteroidEntity.createRandom(level, spawnPosition, random);
            if (asteroid != null && level.noCollision(asteroid)) {
                level.addFreshEntity(asteroid);
                return;
            }
        }
    }

    public static int countAsteroids(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AsteroidEntity && ++count >= MAX_NATURAL_ASTEROIDS) {
                return count;
            }
        }
        return count;
    }

    private static void discardInactiveNaturalAsteroids(ServerLevel level) {
        List<AsteroidEntity> inactive = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AsteroidEntity asteroid
                    && asteroid.isNaturalSpawn()
                    && !level.isPositionEntityTicking(asteroid.blockPosition())) {
                inactive.add(asteroid);
            }
        }
        inactive.forEach(Entity::discard);
    }

    private static List<ServerPlayer> activePlayers(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) {
                players.add(player);
            }
        }
        return players;
    }

    public static Vec3 randomSpawnPosition(
            ServerLevel level, ServerPlayer anchor, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = Mth.lerp(random.nextDouble(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
        double x = anchor.getX() + Math.cos(angle) * distance;
        double z = anchor.getZ() + Math.sin(angle) * distance;
        double requestedY = anchor.getY()
                + (random.nextDouble() * 2.0D - 1.0D) * MAX_VERTICAL_OFFSET;
        double minY = level.getMinBuildHeight() + BUILD_HEIGHT_MARGIN;
        double maxY = level.getMaxBuildHeight() - BUILD_HEIGHT_MARGIN;
        double y = Mth.clamp(requestedY, minY, maxY);
        return new Vec3(x, y, z);
    }
}
