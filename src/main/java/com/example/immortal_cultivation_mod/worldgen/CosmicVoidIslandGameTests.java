package com.example.immortal_cultivation_mod.worldgen;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises loaded datapack codecs and seeded terrain, without generating remote chunks. */
@GameTestHolder("immortal_cultivation_void_islands")
@PrefixGameTestTemplate(false)
public final class CosmicVoidIslandGameTests {
    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void loadedTerrainIsExactlyThreeTimesWiderAcrossSeeds(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var scaled = access.registryOrThrow(Registries.NOISE_SETTINGS)
                .get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "cosmic_void"));
        helper.assertTrue(scaled != null, "Cosmic void noise settings did not load");
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        String json = NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, scaled).getOrThrow().toString();
        var original = NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, JsonParser.parseString(json
                .replace("immortal_cultivation_mod:scaled_void_islands", "minecraft:end_islands")
                .replace("immortal_cultivation_mod:cosmic_void/sloped_cheese", "minecraft:end/sloped_cheese")))
                .getOrThrow();
        var noises = access.lookupOrThrow(Registries.NOISE);
        for (long seed : new long[]{0L, 20261012L, -987654321L}) {
            var before = RandomState.create(original, noises, seed).router();
            var after = RandomState.create(scaled, noises, seed).router();
            for (int x : new int[]{0, 31, -31, 512, -512, 4096, -4096, 12000, -12000}) {
                for (int z : new int[]{0, 97, -97, 777, -777}) {
                    for (int y : new int[]{32, 48, 64, 80}) {
                        var oldPoint = new SinglePointContext(x, y, z);
                        var newPoint = new SinglePointContext(x * 3, y, z * 3);
                        double expected = before.finalDensity().compute(oldPoint);
                        double actual = after.finalDensity().compute(newPoint);
                        helper.assertTrue(Math.abs(expected - actual) < 0.0000001,
                                "3x horizontal terrain differs at " + x + "," + y + "," + z + " seed " + seed);
                        helper.assertTrue(before.erosion().compute(oldPoint) == after.erosion().compute(newPoint),
                                "Island outline did not scale with final terrain");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void enlargedTerrainStillUsesTheWorldSeed(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS)
                .get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "cosmic_void"));
        var a = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), 1234).router().finalDensity();
        var b = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), 5678).router().finalDensity();
        boolean differs = false;
        for (int x = 0; x <= 12000 && !differs; x += 237) {
            var point = new SinglePointContext(x, 48, -x / 2);
            differs = Math.abs(a.compute(point) - b.compute(point)) > 0.0001;
        }
        helper.assertTrue(differs, "Coordinate scaling lost world-seed wiring");
        helper.succeed();
    }
}
