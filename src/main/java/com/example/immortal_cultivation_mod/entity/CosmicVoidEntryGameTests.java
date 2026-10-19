package com.example.immortal_cultivation_mod.entity;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_void_entry")
@PrefixGameTestTemplate(false)
public final class CosmicVoidEntryGameTests {
    private static BlockPos search(ServerLevel level, int x, int z) throws Exception {
        Method method = CosmicVoidPortalEntity.class.getDeclaredMethod("findSafeSurface", ServerLevel.class, int.class, int.class);
        method.setAccessible(true);
        return (BlockPos) method.invoke(null, level, x, z);
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void unloadedLandingSearchDoesNotGenerateChunks(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        int x = 10_000_008, z = 10_000_008;
        helper.assertTrue(level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null, "Remote fixture must be unloaded");
        helper.assertTrue(search(level, x, z) == null, "Unloaded void must choose fallback, not force terrain generation");
        for (int cx = (x - 64) >> 4; cx <= (x + 64) >> 4; cx++) {
            for (int cz = (z - 64) >> 4; cz <= (z + 64) >> 4; cz++) {
                helper.assertTrue(level.getChunkSource().getChunkNow(cx, cz) == null, "Landing search generated a remote chunk");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void loadedLandingUsesCorrectHeightmapOffset(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        BlockPos floor = helper.absolutePos(new BlockPos(2, 0, 2)).atY(200);
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        for (int y = 1; y <= 3; y++) level.setBlockAndUpdate(floor.above(y), Blocks.AIR.defaultBlockState());
        helper.assertTrue(floor.above().equals(search(level, floor.getX(), floor.getZ())), "Loaded landing must stand above the floor");
        level.removeBlock(floor, false);
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void returnMapsSignedDistanceFromActualVoidEntry(GameTestHelper helper) {
        var data = new CompoundTag();
        data.putDouble("ImmortalCultivationCosmicVoidReturnX", 1000.25);
        data.putDouble("ImmortalCultivationCosmicVoidReturnZ", -2000.75);
        data.putDouble("ImmortalCultivationCosmicVoidEntryX", 208.5);
        data.putDouble("ImmortalCultivationCosmicVoidEntryZ", -391.5);
        Vec3 spawn = new Vec3(0, 70, 0);
        Vec3 mapped = CosmicVoidPortalEntity.mapReturnPosition(data, new Vec3(308.5, 150, -431.5), spawn, 1);
        helper.assertTrue(mapped.equals(new Vec3(1500.25, 70, -2200.75)), "100 east and 40 north in void maps to 500 east and 200 north");
        mapped = CosmicVoidPortalEntity.mapReturnPosition(data, new Vec3(208.5, 400, -391.5), spawn, 1);
        helper.assertTrue(mapped.equals(new Vec3(1000.25, 70, -2000.75)), "No horizontal movement returns to origin despite vertical flight");
        mapped = CosmicVoidPortalEntity.mapReturnPosition(data, new Vec3(208.5, 400, -391.5), spawn, 8);
        helper.assertTrue(mapped.x == 8002 && mapped.z == -16006, "Nether entry origin converts to Overworld scale");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacyReturnAndMissingDataRemainUsable(GameTestHelper helper) {
        var data = new CompoundTag();
        data.putDouble("ImmortalCultivationCosmicVoidReturnX", -100);
        data.putDouble("ImmortalCultivationCosmicVoidReturnZ", 200);
        data.putInt("ImmortalCultivationCosmicVoidAnchorX", -24);
        data.putInt("ImmortalCultivationCosmicVoidAnchorZ", 40);
        Vec3 spawn = new Vec3(30, 70, 30);
        helper.assertTrue(CosmicVoidPortalEntity.mapReturnPosition(data, new Vec3(-13.5, 100, 20.5), spawn, 1)
                .equals(new Vec3(-50, 70, 100)), "Legacy anchor center supports travelled-distance exit");
        helper.assertTrue(CosmicVoidPortalEntity.mapReturnPosition(new CompoundTag(), new Vec3(1, 100, 2), spawn, 1)
                .equals(spawn), "Missing entry data falls back to spawn");
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void exitAvoidsHazardAndAlwaysTargetsOverworld(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos floor = helper.absolutePos(new BlockPos(2, 0, 2)).atY(200);
        level.setBlock(floor, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
        for (int i = 1; i <= 3; i++) level.setBlock(floor.above(i), Blocks.AIR.defaultBlockState(), 2);
        BlockPos safe = CosmicVoidPortalEntity.findSafeOverworldLanding(level, floor.getX(), floor.getZ());
        helper.assertTrue(!safe.equals(floor.above()), "Magma floor is rejected");
        helper.assertTrue(level.getBlockState(safe).getFluidState().isEmpty()
                && level.getBlockState(safe.above()).getCollisionShape(level, safe.above()).isEmpty(), "Landing dry with clear headroom");
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "VoidReturn"), ClientInformation.createDefault());
        var data = player.getPersistentData();
        data.putString("ImmortalCultivationCosmicVoidReturnDimension", "minecraft:the_nether");
        data.putDouble("ImmortalCultivationCosmicVoidReturnX", safe.getX() / 8.0);
        data.putDouble("ImmortalCultivationCosmicVoidReturnZ", safe.getZ() / 8.0);
        helper.assertTrue(CosmicVoidPortalEntity.readReturnPoint(player).level() == level.getServer().overworld(),
                "Exit targets Overworld even if entry was from Nether");
        level.removeBlock(floor, false);
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void oceanExitBuildsDryPlatformWithoutLoadingSearchArea(GameTestHelper helper) {
        var level = helper.getLevel();
        int x = 1_000_008, z = 1_000_008;
        level.getChunk(x >> 4, z >> 4);
        var loaded = new java.util.HashSet<Long>();
        for (int cx = (x - 64) >> 4; cx <= (x + 64) >> 4; cx++) {
            for (int cz = (z - 64) >> 4; cz <= (z + 64) >> 4; cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) continue;
                loaded.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    level.setBlock(new BlockPos((cx << 4) + dx, 200, (cz << 4) + dz), Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
        var safe = CosmicVoidPortalEntity.findSafeOverworldLanding(level, x, z);
        helper.assertTrue(safe.getY() > 201 && level.getBlockState(safe.below()).is(Blocks.STONE),
                "Fallback platform above ocean surface: " + safe + ", floor=" + level.getBlockState(safe.below()));
        helper.assertTrue(level.getBlockState(safe).isAir() && level.getBlockState(safe.above()).isAir(), "Fallback clears body and headroom");
        for (int cx = (x - 64) >> 4; cx <= (x + 64) >> 4; cx++) {
            for (int cz = (z - 64) >> 4; cz <= (z + 64) >> 4; cz++) {
                if (loaded.contains(net.minecraft.world.level.ChunkPos.asLong(cx, cz))) continue;
                helper.assertTrue(level.getChunkSource().getChunkNow(cx, cz) == null, "Exit generated neighboring search chunk");
            }
        }
        helper.succeed();
    }
}
