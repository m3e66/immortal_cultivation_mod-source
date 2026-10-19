package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.entity.BloodApeDemonGeneralEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import javax.imageio.ImageIO;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration checks for the restored original white-maned general. */
@GameTestHolder("immortal_cultivation_ape_model")
@PrefixGameTestTemplate(false)
public final class CrimsonApeModelGameTests {
    @GameTest(template = "fate_test")
    public static void originalSizeSurvivesSaveAndReload(GameTestHelper h) {
        var ape = ModEntities.BLOOD_APE_DEMON_GENERAL.get().create(h.getLevel());
        ape.setNoAi(true);
        ape.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(4, 12, 4)));
        assertSize(h, ape);
        CompoundTag tag = new CompoundTag();
        ape.saveWithoutId(tag);
        var restored = ModEntities.BLOOD_APE_DEMON_GENERAL.get().create(h.getLevel());
        restored.load(tag);
        restored.refreshDimensions();
        assertSize(h, restored);
        ape.discard();
        restored.discard();
        h.succeed();
    }

    private static void assertSize(GameTestHelper h, BloodApeDemonGeneralEntity ape) {
        h.assertTrue(BloodApeDemonGeneralEntity.MODEL_SCALE == 1.0F,
                "Renderer and physical dimensions share the restored original scale");
        h.assertTrue(Math.abs(ape.getBbWidth() - 1.15) < .0001,
                "Original collision width is 1.15 blocks");
        h.assertTrue(Math.abs(ape.getBbHeight() - 2.8) < .0001,
                "Original collision height is 2.8 blocks");
        h.assertTrue(Math.abs(ape.getBoundingBox().getYsize() - 2.8) < .0001,
                "The actual collision AABB uses the original height");
    }

    @GameTest(template = "fate_test")
    public static void packagedOriginalModelKeepsTextureAndRig(GameTestHelper h) throws Exception {
        var geometry = asset("geo/entity/xueyuan_yaojiang.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        java.awt.image.BufferedImage texture;
        try (var stream = CrimsonApeModelGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/textures/entity/xueyuan_yaojiang.png")) {
            if (stream == null) throw new AssertionError("Missing solid ape texture");
            texture = ImageIO.read(stream);
        }
        h.assertTrue(texture != null, "The packaged pixel atlas is readable");
        var description = geometry.getAsJsonObject("description");
        h.assertTrue(texture.getWidth() == 64 && texture.getHeight() == 64,
                "The original white-mane and gold-armor atlas is 64 by 64 pixels");
        h.assertTrue(texture.getWidth() == description.get("texture_width").getAsInt()
                        && texture.getHeight() == description.get("texture_height").getAsInt(),
                "Native face UVs use the packaged atlas dimensions");
        var names = new HashSet<String>();
        var checkedTiles = new HashSet<String>();
        int cubes = 0, headCubes = 0, slopedLimbs = 0, slopedTorsoHead = 0;
        for (var entry : geometry.getAsJsonArray("bones")) {
            var bone = entry.getAsJsonObject();
            String boneName = bone.get("name").getAsString();
            names.add(boneName);
            h.assertTrue(!bone.has("poly_mesh"), "Every source part exports as native solid cuboids");
            if (!bone.has("cubes")) continue;
            for (var element : bone.getAsJsonArray("cubes")) {
                var cube = element.getAsJsonObject();
                cubes++;
                if (boneName.equals("head") || boneName.equals("jaw")) headCubes++;
                for (var dimension : cube.getAsJsonArray("size")) {
                    double size = dimension.getAsDouble();
                    h.assertTrue(Double.isFinite(size) && size >= .18 - .00002,
                            "Original anatomy and armor details retain their solid thickness");
                }
                var uv = cube.getAsJsonObject("uv");
                h.assertTrue(uv.size() == 6, "Every solid cuboid textures all six faces");
                for (String face : new String[] {"north", "south", "east", "west", "up", "down"}) {
                    h.assertTrue(uv.has(face), "Solid cube face has UVs: " + face);
                    var tile = uv.getAsJsonObject(face);
                    var start = tile.getAsJsonArray("uv");
                    var span = tile.getAsJsonArray("uv_size");
                    double u = start.get(0).getAsDouble(), v = start.get(1).getAsDouble();
                    double width = span.get(0).getAsDouble(), height = span.get(1).getAsDouble();
                    h.assertTrue(Double.isFinite(u) && Double.isFinite(v) && Double.isFinite(width)
                                    && Double.isFinite(height) && width != 0 && height != 0,
                            "Every solid face has finite, nonempty UV bounds");
                    int x0 = (int) Math.floor(Math.min(u, u + width));
                    int y0 = (int) Math.floor(Math.min(v, v + height));
                    int x1 = (int) Math.ceil(Math.max(u, u + width));
                    int y1 = (int) Math.ceil(Math.max(v, v + height));
                    h.assertTrue(x0 >= 0 && y0 >= 0 && x1 <= texture.getWidth()
                                    && y1 <= texture.getHeight(), "Solid face stays inside the pixel atlas");
                    if (checkedTiles.add(x0 + ":" + y0 + ":" + x1 + ":" + y1)) {
                        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                            h.assertTrue((texture.getRGB(x, y) >>> 24) == 255,
                                    "All six solid faces use opaque pixels, with no triangle cutout masks");
                        }
                    }
                }
                boolean diagonal = false;
                if (cube.has("rotation")) {
                    for (var angle : cube.getAsJsonArray("rotation")) {
                        h.assertTrue(Double.isFinite(angle.getAsDouble()), "Native rotations are finite");
                        diagonal |= Math.abs(angle.getAsDouble()) > .01;
                    }
                }
                if (diagonal && (boneName.startsWith("uarm_") || boneName.startsWith("farm_")
                        || boneName.startsWith("hand_") || boneName.startsWith("thigh_")
                        || boneName.startsWith("shin_") || boneName.startsWith("foot_"))) slopedLimbs++;
                if (diagonal && (boneName.equals("pelvis") || boneName.equals("torso")
                        || boneName.equals("head"))) slopedTorsoHead++;
            }
        }
        h.assertTrue(cubes == 415,
                "The original general has 415 editable solid cubes");
        h.assertTrue(headCubes == 73,
                "The original white mane, muzzle and brows retain their 73 head cubes");
        h.assertTrue(slopedLimbs == 40, "The original arms and legs retain their angled geometry");
        h.assertTrue(slopedTorsoHead == 67, "The original torso and head retain their angled geometry");
        h.assertTrue(names.size() == 34, "All 34 gameplay animation bones remain available");
        for (String bone : new String[] {"root", "fighter", "pelvis", "torso", "head", "jaw",
                "uarm_l", "farm_l", "hand_l", "thigh_l", "shin_l", "foot_l", "skirt_l",
                "uarm_r", "farm_r", "hand_r", "thigh_r", "shin_r", "foot_r", "skirt_r",
                "skirt_center", "skirt_back", "body_vein_l", "body_vein_r", "crest_0", "crest_1",
                "crest_2", "crest_3", "crest_4", "skull_trophy", "fist_aura", "berserk_flames",
                "shockwave", "afterimage"}) {
            h.assertTrue(names.contains(bone), "Gameplay animation bone remains available: " + bone);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void originalClipsKeepGameplayTiming(GameTestHelper h) throws Exception {
        var clips = asset("animations/entity/xueyuan_yaojiang.animation.json")
                .getAsJsonObject("animations");
        String[] names = {"idle", "walk", "run", "xuesha_zhongquan", "liedi_ta",
                "yaoyuan_qinna", "xueying_yuesha", "ranxue_kuanghua", "berserk_idle"};
        double[] durations = {3, 1.2, .8, 1.4, 1.55, 2.55, 2.3, 2.2, 2};
        var boneNames = new HashSet<String>();
        for (var entry : asset("geo/entity/xueyuan_yaojiang.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            boneNames.add(entry.getAsJsonObject().get("name").getAsString());
        }
        var anatomy = java.util.Set.of("pelvis", "torso", "head", "uarm_l", "farm_l", "uarm_r", "farm_r",
                "thigh_l", "shin_l", "thigh_r", "shin_r");
        h.assertTrue(clips.size() == names.length, "All nine gameplay clips are packaged");
        for (int i = 0; i < names.length; i++) {
            var clip = clips.getAsJsonObject("animation.xueyuan_yaojiang_remake." + names[i]);
            h.assertTrue(clip != null, "Combat controller clip exists: " + names[i]);
            h.assertTrue(Math.abs(clip.get("animation_length").getAsDouble() - durations[i]) < .00001,
                    "Attack cues retain existing durations: " + names[i]);
            String loop = i < 3 || i == 8 ? "true" : "hold_on_last_frame";
            h.assertTrue(clip.has("loop") && clip.get("loop").getAsString().equals(loop),
                    "Original clips preserve their gameplay loop modes: " + names[i]);
            var tracks = clip.getAsJsonObject("bones");
            double anatomyRange = 0;
            h.assertTrue(tracks.size() > 0, "Original animation contains bone tracks: " + names[i]);
            for (var track : tracks.entrySet()) {
                String bone = track.getKey();
                h.assertTrue(boneNames.contains(bone),
                        "Original animation targets an existing bone: " + names[i] + " " + bone);
                var channels = track.getValue().getAsJsonObject();
                for (var channel : channels.entrySet()) {
                    h.assertTrue(java.util.Set.of("rotation", "position", "scale").contains(channel.getKey()),
                            "Animation uses a supported transform channel");
                    double range = channelRange(h, channel.getValue(), durations[i],
                            names[i] + " " + bone + " " + channel.getKey());
                    if (anatomy.contains(bone) && channel.getKey().equals("rotation")) anatomyRange += range;
                }
            }
            h.assertTrue(anatomyRange >= .5,
                    "Original clip has changing anatomy poses: " + names[i]);
        }
        h.succeed();
    }

    private static double channelRange(GameTestHelper h, JsonElement channel, double duration, String name) {
        h.assertTrue(channel.isJsonObject(), "Original transform uses timed poses: " + name);
        var keys = channel.getAsJsonObject();
        var times = new HashSet<Double>();
        var points = new ArrayList<double[]>();
        for (var entry : keys.entrySet()) {
            double time = Double.parseDouble(entry.getKey());
            h.assertTrue(Double.isFinite(time) && time >= 0 && time <= duration + .00001,
                    "Original transform keys stay within gameplay duration: " + name);
            h.assertTrue(times.add(time), "Transform key times are unique: " + name);
            addTransformPoints(h, entry.getValue(), points, name);
        }
        h.assertTrue(!points.isEmpty(), "Transform channel contains numeric poses: " + name);
        double range = 0;
        for (int axis = 0; axis < 3; axis++) {
            double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
            for (var point : points) {
                minimum = Math.min(minimum, point[axis]);
                maximum = Math.max(maximum, point[axis]);
            }
            range = Math.max(range, maximum - minimum);
        }
        return range;
    }

    private static void addTransformPoints(GameTestHelper h, JsonElement value,
                                         ArrayList<double[]> points, String name) {
        if (value.isJsonObject()) {
            var frame = value.getAsJsonObject();
            h.assertTrue(frame.has("pre") || frame.has("post"), "Transform key has a numeric pose: " + name);
            if (frame.has("pre")) addTransformPoints(h, frame.get("pre"), points, name);
            if (frame.has("post")) addTransformPoints(h, frame.get("post"), points, name);
            return;
        }
        h.assertTrue(value.isJsonArray() && value.getAsJsonArray().size() == 3,
                "Transform vectors have three components: " + name);
        var vector = value.getAsJsonArray();
        double[] point = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            point[axis] = vector.get(axis).getAsDouble();
            h.assertTrue(Double.isFinite(point[axis]), "Original transforms stay finite: " + name);
        }
        points.add(point);
    }

    private static JsonObject asset(String path) throws Exception {
        try (var stream = CrimsonApeModelGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/" + path)) {
            if (stream == null) throw new AssertionError("Missing runtime asset " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
