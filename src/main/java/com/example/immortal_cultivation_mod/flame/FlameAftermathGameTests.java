package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_flame_aftermath")
@PrefixGameTestTemplate(false)
public final class FlameAftermathGameTests {
    @GameTest(template = "fate_test")
    public static void largestCraterMeshKeepsEveryFaceAndMergesNeighbors(GameTestHelper h) {
        Vec3 center = new Vec3(-.17, 20.81, -.93);
        var candidates = new FlameAftermathFaces(center, 60, 71);
        var mesh = new FlameAftermathMesh();
        var expected = new java.util.HashSet<FlameAftermathFaces.Face>();
        var tiles = new java.util.HashSet<FlameAftermathMesh.Tile>();
        while (candidates.hasNext()) {
            var face = candidates.next();
            expected.add(face);
            tiles.add(mesh.add(face));
        }
        h.assertTrue(mesh.faceCount() == expected.size() && expected.size() < FlameAftermathMesh.MAX_FACES,
                "Largest supported crater fits in the cache with no random skips or truncation");
        var actual = new java.util.HashSet<FlameAftermathFaces.Face>();
        int quads = 0;
        for (var tile : tiles) for (var quad : tile.quads()) {
            quads++;
            for (int u = quad.u(); u < quad.u() + quad.width(); u++)
                for (int v = quad.v(); v < quad.v() + quad.height(); v++) {
                    var face = new FlameAftermathFaces.Face(tile.position(u + v * FlameAftermathMesh.TILE_SIZE),
                            tile.direction());
                    h.assertTrue(actual.add(face), "Merged mesh must never overlap faces");
                }
        }
        h.assertTrue(actual.equals(expected), "Every exposed face survives mesh merging, including negative coordinates");
        h.assertTrue(quads < expected.size(), "Coplanar neighbors reduce the rendered quad count");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void tintPatchesMergeAndBlockEditsDoNotBridgeHoles(GameTestHelper h) {
        for (Direction direction : Direction.values()) {
            var mesh = new FlameAftermathMesh();
            FlameAftermathMesh.Tile tile = null;
            for (int u = 0; u < 4; u++) for (int v = 0; v < 4; v++) {
                var pos = switch (direction.getAxis()) {
                    case X -> new BlockPos(0, v, u);
                    case Y -> new BlockPos(u, 0, v);
                    case Z -> new BlockPos(u, v, 0);
                };
                tile = mesh.add(new FlameAftermathFaces.Face(pos, direction));
            }
            h.assertTrue(tile.quads().size() == 1, "Sixteen coplanar faces share one quad in every direction");
            tile.remove(5);
            int cells = 0;
            for (var quad : tile.quads()) for (int u = quad.u(); u < quad.u() + quad.width(); u++)
                for (int v = quad.v(); v < quad.v() + quad.height(); v++) {
                    h.assertTrue(u + v * 4 != 5, "Edited block holes must stay untinted");
                    cells++;
                }
            h.assertTrue(cells == 15, "Removing one cell keeps all remaining coverage");
        }
        for (FlameKind kind : FlameKind.values()) {
            var origin = new BlockPos(-4, 12, 4);
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                var pos = origin.offset(x, y, z);
                h.assertTrue(FlameAftermathStyle.color(kind, pos, 71) == FlameAftermathStyle.color(kind, origin, 71)
                                && FlameAftermathStyle.opacity(100, pos, 71) == FlameAftermathStyle.opacity(100, origin, 71),
                        "Adjacent blocks share a coherent color and opacity patch instead of isolated speckles");
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void coatingsCoverEveryExposedCraterFaceWithoutDuplicates(GameTestHelper h) {
        for (Vec3 center : new Vec3[]{new Vec3(.5, 20.5, .5), new Vec3(.17, 20.81, .93)}) {
            int radius = 5;
            long seed = 71;
            var removed = new java.util.HashSet<BlockPos>();
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                var column = new BlockPos(x, 0, z);
                double reach = FlameCraterShape.reach(center, column, radius, seed);
                if (!Double.isFinite(reach)) continue;
                int bottom = (int)Math.ceil(center.y - reach - .5);
                int top = (int)Math.floor(center.y + reach - .5);
                for (int y = bottom; y <= top; y++) removed.add(column.atY(y));
            }
            var expected = new java.util.HashSet<FlameAftermathFaces.Face>();
            for (BlockPos air : removed) for (Direction direction : Direction.values()) {
                BlockPos wall = air.relative(direction);
                if (!removed.contains(wall)) expected.add(new FlameAftermathFaces.Face(wall, direction.getOpposite()));
            }
            var actual = new java.util.HashSet<FlameAftermathFaces.Face>();
            var directions = java.util.EnumSet.noneOf(Direction.class);
            var faces = new FlameAftermathFaces(center, radius, seed);
            while (faces.hasNext()) {
                var face = faces.next();
                h.assertTrue(actual.add(face), "A face must not receive overlapping tint quads");
                h.assertTrue(!removed.contains(face.pos()) && removed.contains(face.exposedNeighbor()),
                        "Each tint faces crater air and belongs to a surviving block");
                directions.add(face.direction());
            }
            h.assertTrue(actual.equals(expected), "Tint must include every floor, wall, and ceiling face");
            h.assertTrue(directions.size() == 6, "All six face orientations must be supported");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void everyFlamePreservesOriginalSurvivingCraterFloor(GameTestHelper h) {
        var level = h.getLevel();
        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "flame-aftermath"));
        owner.getAbilities().mayBuild = true;
        int index = 0;
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            var center = h.absolutePos(new BlockPos(index++ * 12, 20, 0));
            var floor = center.below(4);
            owner.setPos(Vec3.atCenterOf(center));
            for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(floor.above(y), Blocks.STONE.defaultBlockState());
            var protectedFloor = floor.east();
            level.setBlockAndUpdate(protectedFloor, Blocks.BEDROCK.defaultBlockState());
            var shot = ModEntities.FLAME_SHOT.get().create(level);
            shot.setOwner(owner);
            shot.setKind(kind);
            FlameTerrainDestruction.destroy(level, owner, shot, Vec3.atCenterOf(center), 3);
            h.assertTrue(level.getBlockState(floor).is(Blocks.STONE),
                    "Every flame preserves the original surviving floor instead of replacing it: " + kind);
            h.assertTrue(level.getBlockState(center).isAir(), "Aftermath leaves crater interior empty");
            h.assertTrue(level.getBlockState(protectedFloor).is(Blocks.BEDROCK), "Unbreakable lining is preserved");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void temporaryColorsHaveVariantsAndExpireWithoutFlickering(GameTestHelper h) {
        for (FlameKind kind : FlameKind.values()) {
            var variants = new java.util.HashSet<Integer>();
            for (int i = 0; i < 32; i++) {
                var pos = new BlockPos(i, 12, i * 3);
                variants.add(FlameAftermathStyle.color(kind, pos, 71));
                h.assertTrue(FlameAftermathStyle.opacity(20, pos, 71) > 0
                                && FlameAftermathStyle.opacity(599, pos, 71) < .001
                                && FlameAftermathStyle.opacity(600, pos, 71) == 0,
                        "Coating appears, fades smoothly, and disappears after 30 seconds");
            }
            h.assertTrue(variants.size() == 3, "Each flame has three stable scorch color variants");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void craterIsIrregularSeededAndAlwaysWithinRealmRadius(GameTestHelper h) {
        Vec3 center = Vec3.atCenterOf(BlockPos.ZERO);
        for (int radius : new int[]{3, 5, 15, 60}) {
            int accepted = 0, rejected = 0, changed = 0;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                var pos = new BlockPos(x, 0, z);
                double reach = FlameCraterShape.reach(center, pos, radius, 71);
                double other = FlameCraterShape.reach(center, pos, radius, 99);
                if (Double.isFinite(reach)) {
                    accepted++;
                    h.assertTrue(x*x + z*z + reach*reach <= radius*radius + .0001,
                            "Irregular crater cannot destroy blocks beyond its realm radius");
                } else if (x*x + z*z < radius*radius) rejected++;
                if (Double.isFinite(reach) != Double.isFinite(other)) changed++;
            }
            h.assertTrue(accepted > 0 && rejected > 0 && changed > 0,
                    "Each realm has an uneven, impact-specific footprint, rather than a perfect circle");
        }
        h.succeed();
    }
}
