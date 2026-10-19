package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.entity.FrostspineCaveBearEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Asset compatibility, doubled collision dimensions, and ridge persistence. */
@GameTestHolder("immortal_cultivation_bear_model")
@PrefixGameTestTemplate(false)
public final class FrostspineBearModelGameTests {
    @GameTest(template = "fate_test")
    public static void doubledSizeAndBrokenRidgesPersist(GameTestHelper h) {
        var bear = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
        bear.setNoAi(true);
        h.assertTrue(FrostspineCaveBearEntity.MODEL_SCALE == 2, "Shared visual and physical scale doubles");
        h.assertTrue(Math.abs(bear.getBbWidth() - 5.6) < .0001, "Physical width doubles");
        h.assertTrue(Math.abs(bear.getBbHeight() - 4.4) < .0001, "Physical height doubles");
        var tag = new CompoundTag(); bear.saveWithoutId(tag);
        tag.putInt("RidgeMask", 0b1010);
        var restored = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
        restored.load(tag); restored.refreshDimensions();
        h.assertTrue(restored.getRidgeMask() == 0b1010 && restored.ridgeCount() == 2,
                "The four independently breakable ridge flags survive loading");
        h.assertTrue(Math.abs(restored.getBoundingBox().getYsize() - 4.4) < .0001,
                "Saved bears retain the doubled collision height");
        bear.discard(); restored.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sourceGeometryUsesNativeSlopingCubes(GameTestHelper h) throws Exception {
        var geometry = asset("geo/entity/frostspine_cave_bear.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        java.awt.image.BufferedImage texture;
        try (var stream = FrostspineBearModelGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/textures/entity/frostspine_cave_bear.png")) {
            if (stream == null) throw new AssertionError("Missing hybrid bear texture");
            texture = ImageIO.read(stream);
        }
        h.assertTrue(texture != null, "The packaged hybrid pixel atlas is readable");
        var description = geometry.getAsJsonObject("description");
        h.assertTrue(texture.getWidth() == description.get("texture_width").getAsInt()
                        && texture.getHeight() == description.get("texture_height").getAsInt(),
                "The hybrid model uses the packaged pixel atlas dimensions");
        var organicBones = Set.of("body", "head", "jaw", "front_leg_left", "front_leg_right",
                "rear_leg_left", "rear_leg_right");
        var names = new HashSet<String>();
        var checkedTiles = new HashSet<String>();
        int cubes = 0, facets = 0, solids = 0, slopedLeg = 0, slopedTorsoHead = 0, headCubes = 0;
        for (var entry : geometry.getAsJsonArray("bones")) {
            var b = entry.getAsJsonObject();
            String boneName = b.get("name").getAsString();
            names.add(boneName);
            h.assertTrue(!b.has("poly_mesh"), "Every source part exports as native cuboids");
            if (!b.has("cubes")) continue;
            for (var element : b.getAsJsonArray("cubes")) {
                var q = element.getAsJsonObject(); cubes++;
                if (boneName.equals("head") || boneName.equals("jaw")) headCubes++;
                var uv = q.getAsJsonObject("uv");
                boolean facet = uv.size() == 2 && uv.has("up") && uv.has("down");
                h.assertTrue(facet || uv.size() == 6, "Native cubes use six solid faces or two source masks");
                if (facet) {
                    facets++;
                    h.assertTrue(organicBones.contains(boneName), "Source facets belong to chosen organic regions");
                    h.assertTrue(Math.abs(q.getAsJsonArray("size").get(1).getAsDouble() - .08) < .00002,
                            "Source triangle surfaces retain their original thin carrier depth");
                } else {
                    solids++;
                    for (String face : new String[] {"north", "south", "east", "west", "up", "down"}) {
                        h.assertTrue(uv.has(face), "Solid cuboid face has UVs: " + face);
                    }
                }
                for (var dimension : q.getAsJsonArray("size")) {
                    double size = dimension.getAsDouble();
                    h.assertTrue(Double.isFinite(size) && size >= (facet ? .079 : .25) - .00002,
                            "Native source carriers and solid cubes retain valid thickness");
                }
                for (var face : uv.entrySet()) {
                    var tile = face.getValue().getAsJsonObject();
                    var start = tile.getAsJsonArray("uv");
                    var span = tile.getAsJsonArray("uv_size");
                    double u = start.get(0).getAsDouble(), v = start.get(1).getAsDouble();
                    double width = span.get(0).getAsDouble(), height = span.get(1).getAsDouble();
                    h.assertTrue(Double.isFinite(u) && Double.isFinite(v) && Double.isFinite(width)
                                    && Double.isFinite(height) && width != 0 && height != 0,
                            "Hybrid cube faces have finite, nonempty UV bounds");
                    int x0 = (int) Math.floor(Math.min(u, u + width));
                    int y0 = (int) Math.floor(Math.min(v, v + height));
                    int x1 = (int) Math.ceil(Math.max(u, u + width));
                    int y1 = (int) Math.ceil(Math.max(v, v + height));
                    h.assertTrue(x0 >= 0 && y0 >= 0 && x1 <= texture.getWidth()
                                    && y1 <= texture.getHeight(), "Hybrid face stays inside its atlas");
                    if (checkedTiles.add(facet + ":" + x0 + ":" + y0 + ":" + x1 + ":" + y1)) {
                        boolean clear = false, opaque = false;
                        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                            int alpha = texture.getRGB(x, y) >>> 24;
                            h.assertTrue(alpha == 255 || facet && alpha == 0,
                                    "Solid faces are opaque; original triangle masks use binary alpha");
                            clear |= alpha == 0;
                            opaque |= alpha == 255;
                        }
                        h.assertTrue(opaque && (!facet || clear),
                                "Source triangle masks contain visible pixels and transparent cutout corners");
                    }
                }
                boolean diagonal = false;
                if (q.has("rotation")) {
                    for (var angle : q.getAsJsonArray("rotation")) if (Math.abs(angle.getAsDouble()) > .01) {
                        diagonal = true; break;
                    }
                }
                if (!facet && diagonal && (boneName.startsWith("front_") || boneName.startsWith("rear_"))) slopedLeg++;
                if (diagonal && (boneName.equals("body") || boneName.equals("head"))) slopedTorsoHead++;
            }
        }
        h.assertTrue(cubes >= 300 && cubes <= 4000, "Hybrid geometry stays within the native cube budget");
        h.assertTrue(facets >= 300 && solids >= 100, "Hybrid geometry includes source surfaces and solid anatomy");
        h.assertTrue(slopedLeg >= 80, "Diagonal solid cubes shape the lower legs and paws");
        h.assertTrue(slopedTorsoHead >= 20, "Angular source surfaces shape the torso and detailed head");
        h.assertTrue(headCubes <= 300, "Head and jaw retain the requested lower detail budget");
        for (int i = 0; i < 4; i++) h.assertTrue(names.contains("ridge_" + i), "Ridge renderer bone exists");
        h.assertTrue(names.contains("ridge_0_head") && names.contains("ridge_0_neck"),
                "Forehead plates move with the head and neck while retaining ridge-zero visibility");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void twelveClipsKeepCombatTiming(GameTestHelper h) throws Exception {
        var clips = asset("animations/entity/frostspine_cave_bear.animation.json").getAsJsonObject("animations");
        String[] names = {"idle", "walk", "run", "paw_slam", "frost_roar", "ice_spine_line",
                "avalanche_leap", "fog_stalk", "ridge_burst", "frenzy", "stagger", "death"};
        double[] durations = {2.8, 1.05, .66, 1.15, 1.6, 2.1, 2.45, 1.25, 1.45, 1.6, 1, 2.2};
        h.assertTrue(clips.size() == 12, "All twelve controller clips are packaged");
        for (int i = 0; i < names.length; i++) {
            var clip = clips.getAsJsonObject("animation.frostspine_cave_bear." + names[i]);
            h.assertTrue(clip != null, "Gameplay animation exists: " + names[i]);
            h.assertTrue(Math.abs(clip.get("animation_length").getAsDouble() - durations[i]) < .00001,
                    "Existing attack timing is retained: " + names[i]);
            var tracks = clip.getAsJsonObject("bones");
            boolean torsoMotion = tracks.has("body")
                    || tracks.has("root") && tracks.getAsJsonObject("root").has("rotation");
            h.assertTrue(tracks.has("root") && torsoMotion && tracks.has("front_paw_left")
                    && tracks.has("rear_paw_right"), "Whole-body supporting motion is packaged");
        }
        h.succeed();
    }

    private static JsonObject asset(String path) throws Exception {
        try (var stream = FrostspineBearModelGameTests.class.getResourceAsStream("/assets/immortal_cultivation_mod/" + path)) {
            if (stream == null) throw new AssertionError("Missing bear asset " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
