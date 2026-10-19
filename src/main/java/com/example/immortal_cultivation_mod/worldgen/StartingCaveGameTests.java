package com.example.immortal_cultivation_mod.worldgen;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_starting_cave")
@PrefixGameTestTemplate(false)
public final class StartingCaveGameTests {
    private StartingCaveGameTests() {
    }

    @GameTest(template = "fate_test")
    public static void candidatesStayNearActualSpawnAcrossSeedsAndNegativeCoordinates(GameTestHelper h) {
        for (BlockPos spawn : new BlockPos[]{BlockPos.ZERO, new BlockPos(-9321, 70, 5407),
                new BlockPos(15997, 150, -24001)}) {
            for (long seed = -20; seed <= 20; seed++) {
                var candidates = StartingCaveManager.candidateChunks(spawn, seed);
                h.assertTrue(candidates.size() == 24 && new HashSet<>(candidates).size() == 24,
                        "A bounded set of distinct terrain sites is sampled");
                h.assertTrue(candidates.equals(StartingCaveManager.candidateChunks(spawn, seed)),
                        "World seed selects stable sites rather than depending on global coordinates");
                for (var chunk : candidates) {
                    var bounds = new BoundingBox(chunk.getMinBlockX() - 80, -64, chunk.getMinBlockZ() - 80,
                            chunk.getMaxBlockX() + 80, 319, chunk.getMaxBlockZ() + 80);
                    h.assertTrue(StartingCaveManager.withinRange(spawn, bounds),
                            "Entire possible cave footprint fits inside 1,000 horizontal blocks of actual spawn");
                }
            }
        }
        h.assertTrue(!StartingCaveManager.withinRange(BlockPos.ZERO, new BoundingBox(1000, 0, 1, 1030, 16, 29)),
                "A cave partly outside the requested range is rejected");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 200)
    public static void realCaveIsPlacedRegisteredAndNeverRebuiltAfterReload(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos spawn = h.absolutePos(new BlockPos(3, 2, 3));
        var state = new StartingCaveManager.State();
        state.pending = true;
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(StartingCaveManager.CAVE);
        var generator = level.getChunkSource().getGenerator();
        var probe = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                StartingCaveManager.candidateChunks(spawn, level.getSeed()).getFirst(), 0, level, biome -> true);
        h.assertTrue(probe.isValid(), "Real cave layout can be generated from the loaded template pool");
        var probeBounds = probe.getBoundingBox();
        StartingCaveManager.ensure(level, spawn, state);
        h.assertTrue(state.placed && !state.pending, "Pending new-world cave completes real structure placement; bounds="
                + probeBounds + ", range=" + StartingCaveManager.withinRange(spawn, probeBounds)
                + ", worldHeight=" + level.getMinBuildHeight() + ".." + level.getMaxBuildHeight());
        var cave = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(StartingCaveManager.CAVE);
        var start = level.structureManager().getStructureAt(state.position, cave);
        h.assertTrue(start.isValid() && StartingCaveManager.withinRange(spawn, start.getBoundingBox()),
                "Actual cave footprint is registered and stays within the requested spawn radius");
        long solid = BlockPos.betweenClosedStream(start.getBoundingBox())
                .filter(pos -> !level.getBlockState(pos).isAir()).count();
        h.assertTrue(solid > 100, "Real saved cave template placed its structural blocks");
        BlockPos changed = state.position;
        level.setBlock(changed, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        CompoundTag saved = state.save(new CompoundTag(), level.registryAccess());
        var restored = StartingCaveManager.State.load(saved, level.registryAccess());
        StartingCaveManager.ensure(level, spawn.offset(5000, 0, 5000), restored);
        h.assertTrue(restored.placed && restored.position.equals(changed)
                        && level.getBlockState(changed).is(Blocks.DIAMOND_BLOCK),
                "Reload and later spawn changes never duplicate or rebuild a completed cave");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void existingWorldWithoutNewWorldMarkerIsUntouched(GameTestHelper h) {
        var state = new StartingCaveManager.State();
        StartingCaveManager.ensure(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)), state);
        h.assertTrue(!state.pending && !state.placed && state.position.equals(BlockPos.ZERO),
                "Loading an existing world does not retroactively place structures over player builds");
        var pending = new StartingCaveManager.State();
        pending.pending = true;
        var restored = StartingCaveManager.State.load(pending.save(new CompoundTag(), h.getLevel().registryAccess()),
                h.getLevel().registryAccess());
        h.assertTrue(restored.pending && !restored.placed,
                "Interrupted new-world startup retains pending placement for the next startup");
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void tutorialUsesSavedForcedCaveAndReplacesRemoteCachedTargets(GameTestHelper h) {
        var level = h.getLevel();
        var savedState = StartingCaveManager.state(level);
        CompoundTag original = savedState.save(new CompoundTag(), level.registryAccess());
        BlockPos forced = h.absolutePos(new BlockPos(3, 2, 3));
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "forced-cave-guide"),
                net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server, connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(net.minecraft.network.protocol.Packet<?> packet) {
            }
        };
        try {
            savedState.placed = false;
            h.assertTrue(StartingCaveManager.position(level).isEmpty(),
                    "Unplaced caves never expose a bogus zero-coordinate tutorial target");
            savedState.complete(forced);
            h.assertTrue(StartingCaveManager.position(level).orElseThrow().equals(forced),
                    "Locator reads the actual saved forced cave position");
            var reloaded = StartingCaveManager.State.load(
                    savedState.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            h.assertTrue(reloaded.placed && reloaded.position.equals(forced),
                    "Forced cave position survives save/load for later tutorial sessions");
            player.setPos(forced.getX() + 20_000, forced.getY(), forced.getZ() - 20_000);
            for (String scenario : new String[]{"fresh", "cached_remote_cave", "retry_cooldown"}) {
                CompoundTag guide = new CompoundTag();
                CompoundTag discovered = new CompoundTag();
                discovered.putBoolean("enlightenment", true);
                guide.put("Discovered", discovered);
                if (scenario.equals("cached_remote_cave")) {
                    guide.putString("CaveTargetDimension", "minecraft:overworld");
                    guide.putInt("CaveTargetX", forced.getX() + 11_927);
                    guide.putInt("CaveTargetZ", forced.getZ() + 11_927);
                }
                guide.putInt("CaveSearchAttempt", 42);
                guide.putLong("CaveSearchAfter", Long.MAX_VALUE);
                player.getPersistentData().put(
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.ROOT_TAG, guide);
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.onLogin(
                        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
                CompoundTag updated = player.getPersistentData().getCompound(
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.ROOT_TAG);
                h.assertTrue(updated.getString("CaveTargetDimension").equals("minecraft:overworld")
                                && updated.getInt("CaveTargetX") == forced.getX()
                                && updated.getInt("CaveTargetZ") == forced.getZ(),
                        "Tutorial points to the forced cave regardless of player distance, old target or cooldown: " + scenario);
                h.assertTrue(updated.getInt("CaveSearchAttempt") == 42,
                        "Reading forced cave metadata bypasses the natural-structure locator entirely");
            }
        } finally {
            var restored = StartingCaveManager.State.load(original, level.registryAccess());
            savedState.pending = restored.pending;
            savedState.placed = restored.placed;
            savedState.position = restored.position;
            savedState.setDirty();
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.onLogout(
                    new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            player.discard();
            channel.close();
        }
        h.succeed();
    }

}
