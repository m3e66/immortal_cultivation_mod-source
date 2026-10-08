package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.QiFireBlock;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.flame.SpiritFlameBlock;
import com.example.immortal_cultivation_mod.flame.SpiritFlameEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** All ambient accents, capture bursts and vessel trails share distance, density and live limits. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class FlameClientEffects {
    static final ParticleGroup PARTICLE_GROUP = new ParticleGroup(384);
    private static final int MAX_EMISSIONS_PER_TICK = 24;
    private static final int MAX_TRACKED_BLOCKS = 96;
    private static final int MAX_SOURCES_PER_TICK = 64;
    private static final double NEAR_DISTANCE_SQUARED = 48 * 48;
    private static final Map<Long, ObservedBlock> BLOCKS = new LinkedHashMap<>();
    private static ClientLevel activeLevel;
    private static long budgetTick = Long.MIN_VALUE;
    private static int admitted;
    private static int densitySequence;

    private FlameClientEffects() { }

    /** Called only by visible block renderers; never scans chunks for hidden fire blocks. */
    public static void observeBlock(BlockPos position, FlameKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        resetForLevel(level);
        if (level == null || minecraft.player == null || minecraft.isPaused()
                || !level.hasChunkAt(position)
                || minecraft.gameRenderer.getMainCamera().getPosition()
                        .distanceToSqr(Vec3.atCenterOf(position)) > NEAR_DISTANCE_SQUARED) return;
        long key = position.asLong();
        // Refresh insertion order so actively rendered sources survive bounded-cache eviction.
        BLOCKS.remove(key);
        BLOCKS.put(key, new ObservedBlock(position.immutable(), kind, level.getGameTime()));
        if (BLOCKS.size() > MAX_TRACKED_BLOCKS) BLOCKS.remove(BLOCKS.keySet().iterator().next());
    }

    /** Also used by network-created vessel particles, which otherwise bypass the ambient budget. */
    static boolean admitParticle(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        resetForLevel(level);
        if (level == null || minecraft.player == null || minecraft.isPaused()
                || minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z)
                        > NEAR_DISTANCE_SQUARED) return false;
        long time = level.getGameTime();
        if (time != budgetTick) {
            budgetTick = time;
            admitted = 0;
        }
        ParticleStatus density = minecraft.options.particles().get();
        int stride = density == ParticleStatus.MINIMAL ? 6 : density == ParticleStatus.DECREASED ? 2 : 1;
        if (Math.floorMod(densitySequence++, stride) != 0 || admitted >= MAX_EMISSIONS_PER_TICK) return false;
        admitted++;
        return true;
    }

    private static void resetForLevel(ClientLevel level) {
        if (activeLevel == level) return;
        activeLevel = level;
        BLOCKS.clear();
        budgetTick = Long.MIN_VALUE;
        admitted = 0;
        densitySequence = 0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        resetForLevel(level);
        if (level == null || minecraft.player == null || minecraft.isPaused()) return;
        long time = level.getGameTime();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        List<Source> sources = new ArrayList<>(MAX_SOURCES_PER_TICK + MAX_TRACKED_BLOCKS);
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof SpiritFlameEntity flame) || !flame.isAlive()
                    || camera.distanceToSqr(flame.position()) > NEAR_DISTANCE_SQUARED) continue;
            sources.add(new Source(flame.position().add(0, 0.65, 0), flame.kind(), flame.getId()));
            if (sources.size() >= MAX_SOURCES_PER_TICK) break;
        }
        BLOCKS.values().removeIf(block -> time - block.lastSeen() > 10
                || time < block.lastSeen() || !level.hasChunkAt(block.position())
                || !matchesBlock(level, block)
                || camera.distanceToSqr(Vec3.atCenterOf(block.position())) > NEAR_DISTANCE_SQUARED);
        for (ObservedBlock block : BLOCKS.values()) {
            sources.add(new Source(Vec3.atLowerCornerOf(block.position()).add(0.5, 0.6, 0.5),
                    block.kind(), block.position().asLong()));
        }
        if (sources.isEmpty()) return;
        // Rotate through the bounded candidate list so clustered blocks and entities get fair turns.
        int start = Math.floorMod(time * 7, sources.size());
        for (int i = 0; i < Math.min(MAX_SOURCES_PER_TICK, sources.size()); i++) {
            if (budgetTick == time && admitted >= MAX_EMISSIONS_PER_TICK) break;
            Source source = sources.get((start + i) % sources.size());
            emitSource(level, source, time);
        }
    }

    private static void emitSource(ClientLevel level, Source source, long time) {
        long phase = time + Math.floorMod(source.seed(), 157);
        switch (source.kind()) {
            case MORTAL, CRIMSON -> {
                if (phase % 3 == 0) spawn(level, source, FlameAccentParticle.Shape.EMBER);
                if (phase % 31 == 0) {
                    spawn(level, source, FlameAccentParticle.Shape.BURST);
                    spawn(level, source, FlameAccentParticle.Shape.BURST);
                }
            }
            case EARTH -> {
                if (phase % 7 == 0) spawn(level, source, FlameAccentParticle.Shape.MOLTEN);
                if (phase % 17 == 0) spawn(level, source, FlameAccentParticle.Shape.SMOKE);
            }
            case WOOD -> {
                if (phase % 5 == 0) spawn(level, source, FlameAccentParticle.Shape.MOTE);
                if (phase % 23 == 0) spawn(level, source, FlameAccentParticle.Shape.LEAF);
            }
            case SOLAR -> {
                if (phase % 13 == 0) {
                    for (int i = 0; i < 4; i++) spawn(level, source, FlameAccentParticle.Shape.SOLAR_SPARK);
                }
            }
            case NETHER -> {
                if (phase % 9 == 0) spawn(level, source, FlameAccentParticle.Shape.GHOST);
                if (phase % 17 == 0) spawn(level, source, FlameAccentParticle.Shape.SOUL);
                if (phase % 29 == 0) spawn(level, source, FlameAccentParticle.Shape.MIST);
            }
            case THUNDER -> {
                if (phase % 4 == 0) spawn(level, source, FlameAccentParticle.Shape.CHARGED);
                if (phase % 11 == 0) spawn(level, source, FlameAccentParticle.Shape.ELECTRIC);
            }
            case DRAGON -> {
                if (phase % 4 == 0) spawn(level, source, FlameAccentParticle.Shape.SCALE);
                if (phase % 7 == 0) spawn(level, source, FlameAccentParticle.Shape.EMBER);
                if (phase % 37 == 0) spawn(level, source, FlameAccentParticle.Shape.BURST);
            }
            case HEAVENLY -> {
                if (phase % 7 == 0) spawn(level, source, FlameAccentParticle.Shape.STAR);
                if (phase % 43 == 0) spawn(level, source, FlameAccentParticle.Shape.PULSE);
            }
        }
    }

    private static boolean matchesBlock(ClientLevel level, ObservedBlock source) {
        var block = level.getBlockState(source.position()).getBlock();
        return block instanceof SpiritFlameBlock flame ? flame.kind() == source.kind()
                : block instanceof QiFireBlock && source.kind() == FlameKind.MORTAL;
    }

    private static void spawn(ClientLevel level, Source source, FlameAccentParticle.Shape shape) {
        Vec3 center = source.center();
        if (!admitParticle(center.x, center.y, center.z)) return;
        Minecraft.getInstance().particleEngine.add(
                new FlameAccentParticle(level, center, source.kind(), shape,
                        source.kind() == FlameKind.MORTAL ? 0x73EDFF : -1));
    }

    private record ObservedBlock(BlockPos position, FlameKind kind, long lastSeen) { }
    private record Source(Vec3 center, FlameKind kind, long seed) { }
}
