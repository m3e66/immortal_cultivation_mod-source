package com.example.immortal_cultivation_mod.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_beast_spawns")
@PrefixGameTestTemplate(false)
public final class BeastSpawnGameTests {
    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void everyWildBeastHasReachableRegisteredRulesAndLoadedBiomeSpawns(GameTestHelper h) {
        long originalTime = h.getLevel().getDayTime();
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        h.assertTrue(BeastSpawnRules.rules().size() == 13
                        && !BeastSpawnRules.rules().containsKey(ModEntities.XUANJIN_ZHENG.get()),
                "All thirteen wild beast species have rules; Xuanjin stays excluded");
        try {
            for (var type : BeastSpawnRules.rules().keySet()) {
                var specimen = (Mob) type.create(h.getLevel());
                h.assertTrue(com.example.immortal_cultivation_mod.externalpower.ExternalPowerTargets.realm(specimen)
                                == BeastSpawnRules.rules().get(type).realm(),
                        "Spawn spacing uses the actual combat realm of " + type);
                specimen.discard();
                Holder<Biome> habitat = biomes.getTag(BeastSpawnRules.habitatTag(type)).orElseThrow().iterator().next();
                h.assertTrue(biomes.holders().filter(holder -> holder.is(BeastSpawnRules.habitatTag(type)))
                                .anyMatch(holder -> holder.value().getMobSettings().getMobs(type.getCategory()).unwrap()
                                        .stream().anyMatch(spawn -> spawn.type == type)),
                        "A loaded biome modifier actually adds " + type);
                boolean deer = type == ModEntities.WIND_FEATHER_CLIFF_DEER.get();
                boolean owl = type == ModEntities.EMBER_FEATHER_FIRE_OWL.get();
                var pos = h.absolutePos(new BlockPos(3, 2, 3)).atY(deer || owl ? 160 : 30);
                prepare(h, pos);
                var ground = type == ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get() ? Blocks.RED_SAND
                        : type == ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get() ? Blocks.SAND
                        : type == ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get()
                                || type == ModEntities.BLOOD_APE_DEMON_GENERAL.get() ? Blocks.DIRT : Blocks.STONE;
                h.getLevel().setBlockAndUpdate(pos.below(), ground.defaultBlockState());
                h.getLevel().setBlockAndUpdate(pos.offset(4, -1, 0), Blocks.LAVA.defaultBlockState());
                h.getLevel().setBlockAndUpdate(pos.offset(4, 0, 1), Blocks.OAK_LEAVES.defaultBlockState());
                h.getLevel().setDayTime(deer ? 6_000 : 18_000);
                h.getLevel().updateSkyBrightness();
                var view = view(h, habitat, deer || owl, 0, Difficulty.NORMAL);
                h.assertTrue(SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.NATURAL, pos, lucky()),
                        "Registered natural habitat is reachable for " + type);
                h.assertTrue(SpawnPlacements.checkSpawnRules(type,
                                view(h, habitat, deer || owl, 15, Difficulty.NORMAL), MobSpawnType.NATURAL, pos, lucky()),
                        "Maximum light does not block natural spawning in a valid habitat for " + type);
                var wrong = view(h, biomes.getHolderOrThrow(Biomes.THE_END), deer || owl, 0, Difficulty.NORMAL);
                h.assertTrue(!SpawnPlacements.checkSpawnRules(type, wrong, MobSpawnType.NATURAL, pos, lucky()),
                        "Wrong biomes cannot naturally spawn " + type);
                h.assertTrue(SpawnPlacements.checkSpawnRules(type, wrong, MobSpawnType.SPAWN_EGG, pos, lucky()),
                        "Eggs do not require natural habitat for " + type);
                h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                h.assertTrue(!SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.NATURAL, pos, lucky()),
                        "Solid blocks prevent spawning " + type);
                h.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                h.assertTrue(!SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.NATURAL, pos, lucky()),
                        "Water prevents spawning " + type);
                h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                if (type != ModEntities.AZURE_SPIRIT_SILKWORM.get()) {
                    h.assertTrue(!SpawnPlacements.checkSpawnRules(type,
                                    view(h, habitat, deer || owl, 0, Difficulty.PEACEFUL), MobSpawnType.NATURAL, pos, lucky()),
                            "Peaceful difficulty blocks hostile beast " + type);
                }
            }
        } finally {
            h.getLevel().setDayTime(originalTime);
            h.getLevel().updateSkyBrightness();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sameRealmAcrossSpeciesBlocksNaturalSpawnsButAllowsEggs(GameTestHelper h) {
        var type = ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get();
        var population = new BeastSpawnRules.SpawnRule(BeastRealms.naturalRealm(type, h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)).atY(30)), 1);
        var pos = h.absolutePos(new BlockPos(3, 2, 3)).atY(30);
        prepare(h, pos);
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.RED_SAND.defaultBlockState());
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var view = view(h, biomes.getHolderOrThrow(Biomes.BADLANDS), true, 0, Difficulty.NORMAL);
        var existing = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        existing.setBeastRealm(population.realm());
        try {
            h.assertTrue(SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.NATURAL, pos, lucky()),
                    "An empty lizard habitat permits spawning");
            existing.setNoAi(true);
            existing.setPos(pos.getX() + .5 + population.radius() - .1, pos.getY(), pos.getZ() + .5);
            h.getLevel().addFreshEntity(existing);
            h.assertTrue(!SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.NATURAL, pos, lucky()),
                    "A Golden Core fox prevents a Golden Core lizard from spawning within 27 blocks");
            existing.setPos(pos.getX() + .5 + population.radius() + .1, pos.getY(), pos.getZ() + .5);
            h.assertTrue(SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.NATURAL, pos, lucky()),
                    "An existing same-realm beast beyond the radius does not block natural spawning");
            existing.setPos(pos.getX() + .5 + population.radius() - .1, pos.getY(), pos.getZ() + .5);
            h.assertTrue(SpawnPlacements.checkSpawnRules(type, view, MobSpawnType.SPAWN_EGG, pos, lucky()),
                    "Realm spacing does not prevent explicit egg placement");
        } finally { existing.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void realmRadiiAreSphericalAndIgnoreOtherRealmsAndExcludedBeasts(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 2, 3)).atY(50);
        var pairs = java.util.List.of(
                java.util.List.of(ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(), ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get()),
                java.util.List.of(ModEntities.THUNDERHORN_ROCK_BADGER.get(), ModEntities.FROSTSPINE_CAVE_BEAR.get()),
                java.util.List.of(ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(), ModEntities.EMBER_FEATHER_FIRE_OWL.get()),
                java.util.List.of(ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(), ModEntities.BLOOD_APE_DEMON_GENERAL.get()));
        for (int index = 0; index < pairs.size(); index++) {
            var candidate = pairs.get(index).get(0);
            var existing = (Mob) pairs.get(index).get(1).create(h.getLevel());
            double radius = Math.pow(3, index + 1);
            h.assertTrue(BeastSpawnRules.rules().get(candidate).radius() == radius,
                    "Each successive major realm triples the previous radius");
            existing.setNoAi(true);
            h.getLevel().addFreshEntity(existing);
            try {
                existing.setPos(pos.getX() + .5 + radius, pos.getY(), pos.getZ() + .5);
                h.assertTrue(!BeastSpawnRules.hasRealmSeparation(candidate, h.getLevel(), pos),
                        "A same-realm beast exactly on the radius blocks spawning");
                existing.setPos(pos.getX() + .5 + radius + .1, pos.getY(), pos.getZ() + .5);
                h.assertTrue(BeastSpawnRules.hasRealmSeparation(candidate, h.getLevel(), pos),
                        "A beast just outside the radius permits spawning");
                existing.setPos(pos.getX() + .5 + radius * .8, pos.getY(), pos.getZ() + .5 + radius * .8);
                h.assertTrue(BeastSpawnRules.hasRealmSeparation(candidate, h.getLevel(), pos),
                        "The query box's corners are outside the actual spherical exclusion radius");
                existing.setPos(pos.getX() + .5, pos.getY() + radius - .1, pos.getZ() + .5);
                h.assertTrue(!BeastSpawnRules.hasRealmSeparation(candidate, h.getLevel(), pos),
                        "Realm spacing also applies to vertically separated beasts");
                var anotherRealm = pairs.get((index + 1) % pairs.size()).get(0);
                h.assertTrue(BeastSpawnRules.hasRealmSeparation(anotherRealm, h.getLevel(), pos),
                        "A different realm does not block spawning");
            } finally { existing.discard(); }
        }
        var excluded = ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
        excluded.setSeal(0);
        excluded.setNoAi(true);
        excluded.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        h.getLevel().addFreshEntity(excluded);
        try {
            h.assertTrue(BeastSpawnRules.hasRealmSeparation(ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(), h.getLevel(), pos),
                    "Xuanjin remains outside the wild-beast spacing rules even at a matching realm");
        } finally { excluded.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void owlSupportsNaturalPerchesAndExistingAirbornePositions(GameTestHelper h) {
        long originalTime = h.getLevel().getDayTime();
        var pos = h.absolutePos(new BlockPos(3, 2, 3)).atY(160);
        prepare(h, pos);
        var biome = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.JAGGED_PEAKS);
        var view = view(h, biome, true, 0, Difficulty.NORMAL);
        try {
            h.getLevel().setDayTime(18_000);
            h.getLevel().updateSkyBrightness();
            h.assertTrue(SpawnPlacements.checkSpawnRules(ModEntities.EMBER_FEATHER_FIRE_OWL.get(), view,
                            MobSpawnType.NATURAL, pos, lucky()), "Heightmap surface candidates permit owl perches");
            h.assertTrue(SpawnPlacements.checkSpawnRules(ModEntities.EMBER_FEATHER_FIRE_OWL.get(), view,
                            MobSpawnType.NATURAL, pos.above(12), lucky()), "Existing airborne owl habitat remains valid");
            h.getLevel().setDayTime(6_000);
            h.getLevel().updateSkyBrightness();
            h.assertTrue(!SpawnPlacements.checkSpawnRules(ModEntities.EMBER_FEATHER_FIRE_OWL.get(), view,
                            MobSpawnType.NATURAL, pos, lucky()), "Overworld fire owls remain nocturnal");
        } finally {
            h.getLevel().setDayTime(originalTime);
            h.getLevel().updateSkyBrightness();
        }
        h.succeed();
    }

    private static void prepare(GameTestHelper h, BlockPos pos) {
        for (var cursor : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 5, 3))) {
            h.getLevel().setBlockAndUpdate(cursor, cursor.getY() < pos.getY()
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    private static LegacyRandomSource lucky() {
        return new LegacyRandomSource(42) {
            @Override public int nextInt(int bound) { return 0; }
        };
    }

    private static ServerLevelAccessor view(GameTestHelper h, Holder<Biome> biome, boolean seesSky,
            int brightness, Difficulty difficulty) {
        // Use real world geometry/entities while isolating biome and lighting from random test terrain.
        return (ServerLevelAccessor) Proxy.newProxyInstance(ServerLevelAccessor.class.getClassLoader(),
                new Class<?>[]{ServerLevelAccessor.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getBiome" -> biome;
                        case "canSeeSky" -> seesSky;
                        case "getBrightness" -> args[0] == net.minecraft.world.level.LightLayer.SKY
                                ? seesSky ? 15 : 0 : brightness;
                        case "getMaxLocalRawBrightness" -> brightness;
                        case "getDifficulty" -> difficulty;
                        default -> {
                            try { yield method.invoke(h.getLevel(), args); }
                            catch (InvocationTargetException error) { throw error.getCause(); }
                        }
                    };
                });
    }
}
