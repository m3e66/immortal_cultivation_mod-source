package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime size, nine-tail native geometry, and gameplay animation compatibility. */
@GameTestHolder("immortal_cultivation_fox_model")
@PrefixGameTestTemplate(false)
public final class PhantomFoxModelGameTests {
    private static final String[] SIDES = {"left", "right"};
    private static final String[] LIMBS = {"front", "rear"};

    @GameTest(template = "fate_test")
    public static void doubledFoxAndCloneKeepIllusionState(GameTestHelper h) {
        var fox = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        h.assertTrue(fox != null, "Registered fox creates a real entity");
        fox.setNoAi(true);
        assertDoubledSize(h, fox.getBbWidth(), fox.getBbHeight());
        fox.setBeastRealm(2);
        var tag = new CompoundTag();
        fox.saveWithoutId(tag);
        tag.putInt("Masks", 3);
        tag.putInt("HundredFacesTicks", 25);
        tag.putBoolean("HundredFacesTriggered", true);
        var restored = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        restored.load(tag);
        restored.refreshDimensions();
        assertDoubledSize(h, restored.getBbWidth(), restored.getBbHeight());
        h.assertTrue(restored.maskCount() == 3 && restored.isSpiritMaskVisible(),
                "Loading the new model retains saved masks and Hundred Faces visibility");

        var clone = ModEntities.PHANTOM_VEIL_FOX_CLONE.get().create(h.getLevel());
        clone.configure(restored, null, 80);
        assertDoubledSize(h, clone.getBbWidth(), clone.getBbHeight());
        var cloneTag = new CompoundTag();
        clone.saveWithoutId(cloneTag);
        var restoredClone = ModEntities.PHANTOM_VEIL_FOX_CLONE.get().create(h.getLevel());
        restoredClone.load(cloneTag);
        restoredClone.refreshDimensions();
        assertDoubledSize(h, restoredClone.getBbWidth(), restoredClone.getBbHeight());
        h.assertTrue(restoredClone.ownerUuid().filter(restored.getUUID()::equals).isPresent(),
                "Illusion clones retain their owner after loading");
        h.assertTrue(Math.abs(restoredClone.renderScale() - restored.realmTier().renderScale()) < .00001,
                "Doubled clones retain the owner's independent realm scale");
        fox.discard();
        restored.discard();
        clone.discard();
        restoredClone.discard();
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void nineTailsUseNativeHybridPixelGeometry(GameTestHelper h) throws Exception {
        var geometry = asset("geo/entity/phantom_veil_fox.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        java.awt.image.BufferedImage texture;
        try (var stream = PhantomFoxModelGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/textures/entity/phantom_veil_fox.png")) {
            if (stream == null) throw new AssertionError("Missing fox pixel atlas");
            texture = ImageIO.read(stream);
        }
        h.assertTrue(texture != null, "Packaged fox pixel atlas is readable");
        var description = geometry.getAsJsonObject("description");
        h.assertTrue(texture.getWidth() == description.get("texture_width").getAsInt()
                        && texture.getHeight() == description.get("texture_height").getAsInt(),
                "Native geometry and packaged atlas dimensions agree");
        var bones = new HashMap<String, JsonObject>();
        var checkedTiles = new HashSet<String>();
        int cubes = 0, facets = 0, solids = 0, diagonalSolids = 0;
        for (var element : geometry.getAsJsonArray("bones")) {
            var bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            h.assertTrue(bones.put(name, bone) == null, "Animation bones have unique names");
            h.assertTrue(!bone.has("poly_mesh"), "Fox geometry exports as native Cube elements");
            if (!bone.has("cubes")) continue;
            for (var cubeElement : bone.getAsJsonArray("cubes")) {
                var cube = cubeElement.getAsJsonObject();
                cubes++;
                var uv = cube.getAsJsonObject("uv");
                boolean facet = uv.size() == 2 && uv.has("up") && uv.has("down");
                var dimensions = cube.getAsJsonArray("size");
                h.assertTrue(dimensions.size() == 3, "Native geometry dimensions have three axes");
                int zeroAxes = 0;
                for (var dimension : dimensions) {
                    double size = dimension.getAsDouble();
                    h.assertTrue(Double.isFinite(size) && size >= 0,
                            "Native dimensions are finite and nonnegative");
                    if (size == 0) zeroAxes++;
                    else h.assertTrue(size > .001, "Nonzero native dimensions retain finite thickness");
                }
                h.assertTrue(zeroAxes <= 1, "An authored plane can collapse only one axis");
                boolean plane = zeroAxes == 1;
                h.assertTrue(!plane || dimensions.get(1).getAsDouble() == 0
                                && uv.size() >= 1 && uv.size() <= 2
                                && uv.keySet().stream().allMatch(face -> face.equals("up") || face.equals("down")),
                        "Authored flat planes use only their up/down texture faces");
                boolean surface = facet || plane;
                h.assertTrue(surface || uv.size() == 6,
                        "Native anatomy uses source surfaces, authored planes and complete solid cuboids");
                if (surface) facets++; else solids++;
                if (cube.has("inflate")) {
                    h.assertTrue(Double.isFinite(cube.get("inflate").getAsDouble()),
                            "Signed native cube inflation remains finite");
                }
                if (cube.has("rotation")) {
                    boolean diagonal = false;
                    for (var angle : cube.getAsJsonArray("rotation")) {
                        h.assertTrue(Double.isFinite(angle.getAsDouble()), "Native rotations are finite");
                        diagonal |= Math.abs(angle.getAsDouble()) > .01;
                    }
                    if (diagonal && !surface) diagonalSolids++;
                }
                if (!surface) {
                    for (String face : new String[] {"north", "south", "east", "west", "up", "down"}) {
                        h.assertTrue(uv.has(face), "Solid cuboid face has texture UVs: " + face);
                    }
                }
                for (var face : uv.entrySet()) {
                    var tile = face.getValue().getAsJsonObject();
                    var origin = tile.getAsJsonArray("uv");
                    var size = tile.getAsJsonArray("uv_size");
                    double u = origin.get(0).getAsDouble(), v = origin.get(1).getAsDouble();
                    double width = size.get(0).getAsDouble(), height = size.get(1).getAsDouble();
                    h.assertTrue(Double.isFinite(u) && Double.isFinite(v) && Double.isFinite(width)
                                    && Double.isFinite(height) && width != 0 && height != 0,
                            "Every native face has nonempty finite pixel coordinates");
                    int x0 = (int) Math.floor(Math.min(u, u + width));
                    int y0 = (int) Math.floor(Math.min(v, v + height));
                    int x1 = (int) Math.ceil(Math.max(u, u + width));
                    int y1 = (int) Math.ceil(Math.max(v, v + height));
                    h.assertTrue(x0 >= 0 && y0 >= 0 && x1 <= texture.getWidth()
                                    && y1 <= texture.getHeight(), "Every UV region remains inside the atlas");
                    if (checkedTiles.add(surface + ":" + x0 + ":" + y0 + ":" + x1 + ":" + y1)) {
                        boolean visible = false;
                        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                            int alpha = texture.getRGB(x, y) >>> 24;
                            h.assertTrue(alpha == 255 || surface && alpha == 0,
                                    "Solid cubes are opaque; source cutouts use binary pixel alpha");
                            visible |= alpha == 255;
                        }
                        h.assertTrue(visible, "Every exported face references visible texture pixels");
                    }
                }
            }
        }
        h.assertTrue(cubes >= 200 && cubes <= 5000, "Native detail remains within a practical cube budget");
        h.assertTrue(facets >= 100 && solids >= 20 && diagonalSolids >= 10,
                "Fox blends source facets with sloping solid anatomy");
        for (String name : new String[] {"root", "body", "neck", "head", "jaw", "ear_left", "ear_right"}) {
            h.assertTrue(bones.containsKey(name), "Core animation bone exists: " + name);
        }
        h.assertTrue(tailRoots(bones).size() == 9, "Nine independently articulated fox tails are packaged");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sixControllerClipsAnimateBodyAndNineTails(GameTestHelper h) throws Exception {
        var geometry = asset("geo/entity/phantom_veil_fox.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var bones = new HashMap<String, JsonObject>();
        for (var entry : geometry.getAsJsonArray("bones")) {
            var bone = entry.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }
        var tails = tailRoots(bones);
        var clips = asset("animations/entity/phantom_veil_fox.animation.json").getAsJsonObject("animations");
        String[] names = {"idle", "walk", "run", "pounce", "vanish", "death"};
        double[] durations = {3.2, .82, .5, .78, .72, 1.45};
        h.assertTrue(clips.size() == names.length, "All six real fox controller clips are packaged");
        for (int i = 0; i < names.length; i++) {
            var clip = clips.getAsJsonObject("animation.phantom_veil_fox." + names[i]);
            h.assertTrue(clip != null, "Gameplay clip exists: " + names[i]);
            h.assertTrue(Math.abs(clip.get("animation_length").getAsDouble() - durations[i]) < .00001,
                    "Combat and movement timing remain compatible: " + names[i]);
            h.assertTrue(i < 3 ? clip.get("loop").getAsBoolean()
                            : clip.get("loop").getAsString().equals("hold_on_last_frame"),
                    "Controller loop and one-shot behavior remain compatible: " + names[i]);
            var tracks = clip.getAsJsonObject("bones");
            for (String name : new String[] {"root", "body", "neck", "head", "jaw"}) {
                h.assertTrue(tracks.has(name), "Supporting full-body track exists: " + name);
            }
            for (String side : SIDES) for (String limb : LIMBS) {
                h.assertTrue(tracks.has(limb + "_leg_" + side)
                                && tracks.has(limb + "_lower_" + side)
                                && tracks.has(limb + "_paw_" + side),
                        "Every leg is articulated through its paw: " + limb + " " + side);
            }
            for (String tail : tails) {
                h.assertTrue(tracks.has(tail), "Every tail has independent supporting motion: " + tail);
            }
            for (var track : tracks.entrySet()) {
                h.assertTrue(bones.containsKey(track.getKey()), "Every animation targets an exported native bone");
                assertFiniteValues(h, track.getValue());
            }
        }
        h.succeed();
    }

    private static Set<String> tailRoots(HashMap<String, JsonObject> bones) {
        var roots = new HashSet<String>();
        for (var entry : bones.entrySet()) {
            var bone = entry.getValue();
            if (entry.getKey().startsWith("tail_") && bone.has("parent")
                    && !bone.get("parent").getAsString().startsWith("tail_")) {
                roots.add(entry.getKey());
            }
        }
        return roots;
    }

    private static void assertDoubledSize(GameTestHelper h, float width, float height) {
        h.assertTrue(Math.abs(width - 2.5) < .00001 && Math.abs(height - 2.8) < .00001,
                "Fox and illusion clone collision dimensions both double");
    }

    private static void assertFiniteValues(GameTestHelper h, JsonElement value) {
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) assertFiniteValues(h, entry.getValue());
        } else if (value.isJsonArray()) {
            for (var item : value.getAsJsonArray()) assertFiniteValues(h, item);
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            h.assertTrue(Double.isFinite(value.getAsDouble()), "Animation keys contain finite values");
        }
    }

    private static JsonObject asset(String path) throws Exception {
        try (var stream = PhantomFoxModelGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/" + path)) {
            if (stream == null) throw new AssertionError("Missing fox asset " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
