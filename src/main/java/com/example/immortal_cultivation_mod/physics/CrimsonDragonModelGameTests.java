package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Focused integration checks for the native Blockbench replacement and server rig. */
@GameTestHolder("immortal_cultivation_dragon_model")
@PrefixGameTestTemplate(false)
public final class CrimsonDragonModelGameTests {
    @GameTest(template = "fate_test")
    public static void verticalLookRemainsSmooth(GameTestHelper h) {
        DragonLookGameTests.verticalLookEasesAndReturnsToRest(h);
    }

    @GameTest(template = "fate_test")
    public static void newNeckMovesRealHeadAndJawCollision(GameTestHelper h) {
        DragonLookGameTests.lookingMovesHeadAndJawHitboxes(h);
    }

    @GameTest(template = "fate_test")
    @SuppressWarnings("unchecked")
    public static void collisionAnchorsMatchPackagedNativeModel(GameTestHelper h) throws Exception {
        var geometry = asset("geo/entity/crimson_sky_flame_flood_dragon.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var bones = geometry.getAsJsonArray("bones");
        h.assertTrue(bones.size() == 46, "Rig includes flexible torso, two uncoiling neck hinges and articulated chin/brow whiskers");
        int cubes = 0;
        int pixelPlanes = 0;
        int legFrillPlanes = 0;
        int anteriorChestPlates = 0;
        for (var element : bones) {
            var bone = element.getAsJsonObject();
            if (bone.has("cubes")) {
                cubes += bone.getAsJsonArray("cubes").size();
                String name = bone.get("name").getAsString();
                if (name.endsWith("_foot")) {
                    h.assertTrue(bone.getAsJsonArray("cubes").size() >= 240,
                            "Each GLB paw retains separate toe contours and overlapping wrist skin");
                    for (var footElement : bone.getAsJsonArray("cubes")) {
                        double footThickness = footElement.getAsJsonObject().getAsJsonArray("size").get(1).getAsDouble();
                        h.assertTrue(Math.abs(footThickness - 0.08) < 0.00002,
                                "Claw carriers stay thin instead of forming oversized toe wedges");
                    }
                }
                for (var elementCube : bone.getAsJsonArray("cubes")) {
                    var cube = elementCube.getAsJsonObject();
                    // These plates are the front of the curled lower neck.
                    // A former region-classifier gap left them on body_core,
                    // causing visible chest detachment in every animation.
                    var origin = cube.getAsJsonArray("origin");
                    var cubeSize = cube.getAsJsonArray("size");
                    double centerY = origin.get(1).getAsDouble() + cubeSize.get(1).getAsDouble() / 2;
                    double centerZ = origin.get(2).getAsDouble() + cubeSize.get(2).getAsDouble() / 2;
                    if (centerZ > -44 && centerZ < -40 && centerY > 13 && centerY < 25
                            && !name.equals("head") && !name.equals("jaw")) {
                        h.assertTrue(name.equals("neck_1") || name.equals("neck_2") || name.equals("chest_uncoil"),
                                "Anterior chest skin must inherit the lower neck, including all look and attack poses");
                        anteriorChestPlates++;
                    }
                    double thickness = cube.getAsJsonArray("size").get(1).getAsDouble();
                    if (thickness > 0 && thickness < 0.002) {
                        pixelPlanes++;
                        if (name.endsWith("_upper") || name.endsWith("_lower")) legFrillPlanes++;
                        var uv = cube.getAsJsonObject("uv");
                        h.assertTrue(uv.size() == 2 && uv.has("up") && uv.has("down"),
                                "Every head, back, side and underside trail is a two-sided plane");
                        for (var face : uv.entrySet()) {
                            var size = face.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                            h.assertTrue(Math.abs(size.get(0).getAsDouble()) == 64
                                            && Math.abs(size.get(1).getAsDouble()) == 64,
                                    "All trail cutouts include the requested additional pixel detail");
                        }
                    }
                }
                if (name.equals("body_core") || name.equals("neck_1") || name.startsWith("tail_")
                        || name.endsWith("_upper") || name.endsWith("_lower") || name.endsWith("_foot")) {
                    for (var cube : bone.getAsJsonArray("cubes")) {
                        h.assertTrue(cube.getAsJsonObject().getAsJsonObject("uv").size() == 2,
                                "Body uses the accepted head-style surface facets on both sides");
                    }
                }
                if (name.startsWith("horn_") || name.startsWith("whisker_")) {
                    for (var elementCube : bone.getAsJsonArray("cubes")) {
                        var cube = elementCube.getAsJsonObject();
                        h.assertTrue(cube.getAsJsonObject("uv").size() == 2,
                                "Trail planes have textured front and back faces");
                        double thickness = cube.getAsJsonArray("size").get(1).getAsDouble();
                        h.assertTrue(thickness > 0 && thickness < 0.002,
                                "Trails are thin planes rather than stacked blocks");
                        for (var face : cube.getAsJsonObject("uv").entrySet()) {
                            var size = face.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                            h.assertTrue(Math.abs(size.get(0).getAsDouble()) == 64
                                            && Math.abs(size.get(1).getAsDouble()) == 64,
                                    "Trail cutouts use 64 by 64 pixel textures");
                        }
                    }
                }
            }
        }
        h.assertTrue(cubes == 5408, "Dragon retains solid legs and flat ornaments, with source-colored collars covering both uncoiling neck hinges and the skull connection");
        h.assertTrue(pixelPlanes == 52, "Pixel planes cover body trails, whiskers and protruding ornaments around all four legs");
        h.assertTrue(legFrillPlanes == 6, "Only source ornaments become planes; solid lower rear legs stay intact");
        h.assertTrue(anteriorChestPlates >= 26, "Packaged model includes the formerly detached anterior chest plates");
        h.assertTrue(geometry.getAsJsonObject("description").get("texture_width").getAsInt() == 4096,
                "The GLB color atlas is packaged with the matching UV resolution");
        h.assertTrue(geometry.getAsJsonObject("description").get("texture_height").getAsInt() == 4096,
                "Shared texture patches include flexible skin and generated whiskers");
        var clips = asset("animations/entity/crimson_sky_flame_flood_dragon.animation.json")
                .getAsJsonObject("animations");
        h.assertTrue(clips.size() == 8, "Seven accepted gameplay clips plus the airborne flight loop are available");
        var flightClip = clips.getAsJsonObject("animation.crimson_sky_flame_flood_dragon.fly");
        h.assertTrue(flightClip.get("loop").getAsBoolean()
                        && Math.abs(flightClip.get("animation_length").getAsDouble() - 1.6) < .00001,
                "Flight continuously loops through the full-body swimming motion");
        for (var entry : clips.entrySet()) {
            var torso = entry.getValue().getAsJsonObject().getAsJsonObject("bones").getAsJsonObject("body_core");
            h.assertTrue(trackSpan(torso.getAsJsonObject("rotation"), 1) > 5,
                    "Every remade clip turns the torso visibly: " + entry.getKey());
            h.assertTrue(trackSpan(torso.getAsJsonObject("rotation"), 2) > 2,
                    "Every remade clip transfers weight through torso roll: " + entry.getKey());
            h.assertTrue(trackSpan(torso.getAsJsonObject("position"), 1) > .8,
                    "Every remade clip moves the whole body vertically: " + entry.getKey());
            h.assertTrue(trackSpan(torso.getAsJsonObject("scale"), 1) > .015,
                    "Every clip includes visible inherited squash/stretch: " + entry.getKey());
            for (String segment : new String[]{"body_mid", "body_rear"}) {
                var channels = entry.getValue().getAsJsonObject().getAsJsonObject("bones").getAsJsonObject(segment);
                h.assertTrue(trackSpan(channels.getAsJsonObject("rotation"), 1) > 4,
                        "Torso segments bend independently in " + entry.getKey());
            }
        }
        Map<String, JsonObject> byName = new HashMap<>();
        for (var element : bones) {
            var bone = element.getAsJsonObject();
            byName.put(bone.get("name").getAsString(), bone);
        }
        var flightTracks = flightClip.getAsJsonObject("bones");
        h.assertTrue(byName.get("neck_2").get("parent").getAsString().equals("chest_uncoil")
                        && byName.get("head").get("parent").getAsString().equals("upper_uncoil"),
                "Both bends of the source neck unfold before the attached skull");
        double lowestBack = Double.POSITIVE_INFINITY, highestBack = Double.NEGATIVE_INFINITY;
        String[] neckHosts = {"neck_1", "neck_1", "chest_uncoil", "chest_uncoil", "neck_2", "upper_uncoil"};
        double[][] neckSource = {{0, 14, -25}, {0, 14, -36}, {0, 20.846607, -38.994109},
                {0, 25, -38}, {0, 30, -39}, {0, 34, -39}};
        for (int sample = 0; sample <= 32; sample++) {
            double time = 1.6 * sample / 32;
            Vector3d[] rendered = new Vector3d[neckHosts.length];
            for (int index = 0; index < neckHosts.length; index++) {
                var pivot = byName.get(neckHosts[index]).getAsJsonArray("pivot");
                rendered[index] = nativeJoint(neckHosts[index], byName, flightTracks, time).transformPosition(
                        new Vector3d(neckSource[index][0] - pivot.get(0).getAsDouble(),
                                neckSource[index][1] - pivot.get(1).getAsDouble(),
                                neckSource[index][2] - pivot.get(2).getAsDouble()));
            }
            Vector3d start = rendered[0], end = rendered[rendered.length - 1];
            for (Vector3d point : rendered) {
                double fraction = (point.z - start.z) / (end.z - start.z);
                h.assertTrue(Math.abs(point.y - (start.y + fraction * (end.y - start.y))) < 3.5,
                        "Visible source neck centerline is straight in flight, including its former chest bend");
            }
            double backY = nativeJoint("tail_1", byName, flightTracks, time).transformPosition(new Vector3d()).y;
            lowestBack = Math.min(lowestBack, backY);
            highestBack = Math.max(highestBack, backY);
        }
        h.assertTrue(highestBack - lowestBack > 14,
                "Flight carries a pronounced vertical traveling wave through the back");
        var jawPivot = byName.get("jaw").getAsJsonArray("pivot");
        h.assertTrue(jawPivot.get(0).getAsDouble() == 0
                        && jawPivot.get(1).getAsDouble() == 29.3
                        && jawPivot.get(2).getAsDouble() == -59,
                "Lower jaw pivots at the source model's mouth corner");
        h.assertTrue(byName.get("jaw").get("parent").getAsString().equals("head"),
                "Every jaw facet inherits the complete head animation and look pose");
        h.assertTrue(byName.get("jaw").getAsJsonArray("cubes").size() == 316,
                "Source lower lip and teeth remain; hanging chin ornament becomes pixel whiskers");
        for (String side : new String[]{"left", "right"}) {
            for (String kind : new String[]{"chin", "brow"}) {
                String root = kind + "_" + side;
                h.assertTrue(byName.get(root).get("parent").getAsString().equals(kind.equals("chin") ? "jaw" : "head"),
                        "Whisker root follows its anatomical attachment");
                h.assertTrue(byName.get(root + "_mid").get("parent").getAsString().equals(root)
                                && byName.get(root + "_tip").get("parent").getAsString().equals(root + "_mid"),
                        "Whisker ends inherit flowing joint motion");
            }
        }
        for (var entry : clips.entrySet()) {
            var jaw = entry.getValue().getAsJsonObject().getAsJsonObject("bones").getAsJsonObject("jaw");
            h.assertTrue(!jaw.has("position") && !jaw.has("scale"),
                    "Jaw rotates about its fixed hinge without translating apart: " + entry.getKey());
        }
        var breathJaw = clips.getAsJsonObject("animation.crimson_sky_flame_flood_dragon.yan_jiao_tu_xi")
                .getAsJsonObject("bones").getAsJsonObject("jaw").getAsJsonObject("rotation");
        double maximumOpening = breathJaw.entrySet().stream()
                .mapToDouble(frame -> frame.getValue().getAsJsonArray().get(0).getAsDouble()).max().orElse(0);
        h.assertTrue(maximumOpening > 10 && maximumOpening <= 16.001,
                "Breath opens the already posed GLB jaw without excessive rotation");
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.setPos(h.absoluteVec(new Vec3(4, 30, 4)));
        dragon.setYRot(0);
        var update = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("updateBoneHitboxes");
        update.setAccessible(true);
        var begin = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("beginAction", byte.class, int.class);
        begin.setAccessible(true);
        var finish = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("finishAction");
        finish.setAccessible(true);
        var ticksField = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredField("ACTION_TICKS");
        ticksField.setAccessible(true);
        var ticks = (EntityDataAccessor<Integer>) ticksField.get(null);
        String[] names = {"idle", "walk", "yan_jiao_tu_xi", "chi_yan_shuai_wei",
                "yan_lin_bao", "jiao_huo_dun", "fen_tian_nu", "fly"};
        int[] durations = {60, 30, 52, 30, 29, 22, 160, 32};
        for (int index = 0; index < names.length; index++) {
            var clip = clips.getAsJsonObject("animation.crimson_sky_flame_flood_dragon." + names[index]);
            var tracks = clip.getAsJsonObject("bones");
            boolean locomotion = index < 2 || index == 7;
            if (locomotion) finish.invoke(dragon);
            else begin.invoke(dragon, (byte) (index - 1), durations[index]);
            dragon.setOnGround(index < 2);
            dragon.setDeltaMovement(index == 1 ? new Vec3(0, 0, .07) : Vec3.ZERO);
            int lengthTicks = (int) Math.round(clip.get("animation_length").getAsDouble() * 20);
            for (int sample = 0; sample <= 8; sample++) {
                int elapsed = Math.min(lengthTicks - 1, lengthTicks * sample / 8);
                if (locomotion) dragon.tickCount = elapsed;
                else dragon.getEntityData().set(ticks, durations[index] - elapsed);
                double time = elapsed / 20.0;
                update.invoke(dragon);
                checkCollisionPose(h, dragon, byName, tracks, time, names[index]);
                var breath = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("breathOrigin", Vec3.class);
                breath.setAccessible(true);
                Vector3d muzzle = nativeJoint("head", byName, tracks, time)
                        .transformPosition(new Vector3d(0, -7, -17.3));
                double unit = CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE / 16.0;
                Vec3 expected = dragon.position().add(-muzzle.x * unit, muzzle.y * unit, -muzzle.z * unit + .15);
                Vec3 actual = (Vec3) breath.invoke(dragon, new Vec3(0, 0, 1));
                h.assertTrue(actual.distanceTo(expected) < .001,
                        "Breath origin follows full-body motion in " + names[index] + " at " + time);
            }
        }
        dragon.discard();
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void flightUsesAirborneLoopAndDoubledSpeed(GameTestHelper h) throws Exception {
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.setPos(h.absoluteVec(new Vec3(4, 30, 4)));
        var current = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("currentSkeletonAnimation");
        current.setAccessible(true);
        dragon.setOnGround(true);
        dragon.setDeltaMovement(Vec3.ZERO);
        assertSkeleton(h, dragon, current, "IDLE", "Grounded resting dragon uses idle");
        dragon.setDeltaMovement(0, 0, .07);
        assertSkeleton(h, dragon, current, "WALK", "Ground movement still uses walk");
        dragon.setOnGround(false);
        assertSkeleton(h, dragon, current, "FLY", "Airborne travel uses flight rather than walking");
        dragon.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(dragon.isFlying(), "Stationary hovering is airborne locomotion");
        assertSkeleton(h, dragon, current, "FLY", "Hovering also plays the continuous flight loop");
        var begin = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("beginAction", byte.class, int.class);
        begin.setAccessible(true);
        begin.invoke(dragon, (byte) 1, 52);
        assertSkeleton(h, dragon, current, "BREATH", "Attacks keep priority over airborne locomotion");

        var flight = dragon.getAttribute(Attributes.FLYING_SPEED);
        h.assertTrue(Math.abs(flight.getBaseValue() - 3.24) < .000001, "New dragon flies at twice the former 1.62 base");
        dragon.getMoveControl().setWantedPosition(dragon.getX() + 10, dragon.getY() + 2, dragon.getZ(), 1);
        dragon.getMoveControl().tick();
        float faster = dragon.getSpeed();
        flight.setBaseValue(1.62);
        dragon.getMoveControl().setWantedPosition(dragon.getX() + 10, dragon.getY() + 2, dragon.getZ(), 1);
        dragon.getMoveControl().tick();
        float previous = dragon.getSpeed();
        h.assertTrue(previous > 0 && Math.abs(faster / previous - 2) < .00001,
                "Actual FlyingMoveControl consumes the doubled speed during airborne travel");
        var loaded = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        loaded.readAdditionalSaveData(dragon.saveWithoutId(new CompoundTag()));
        h.assertTrue(Math.abs(loaded.getAttribute(Attributes.FLYING_SPEED).getBaseValue() - 3.24) < .000001,
                "Existing saved dragons using the former default upgrade to the new speed");
        flight.setBaseValue(2.1);
        loaded.readAdditionalSaveData(dragon.saveWithoutId(new CompoundTag()));
        h.assertTrue(Math.abs(loaded.getAttribute(Attributes.FLYING_SPEED).getBaseValue() - 2.1) < .000001,
                "Custom saved flying speeds are preserved");
        loaded.discard();
        dragon.discard();
        h.succeed();
    }

    private static void assertSkeleton(GameTestHelper h, CrimsonSkyFlameFloodDragonEntity dragon,
                                       java.lang.reflect.Method current, String expected, String message) throws Exception {
        var field = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredField(expected + "_SKELETON");
        field.setAccessible(true);
        h.assertTrue(current.invoke(dragon) == field.get(null), message);
    }

    private static void checkCollisionPose(GameTestHelper h, CrimsonSkyFlameFloodDragonEntity dragon,
                                          Map<String, JsonObject> bones, JsonObject tracks,
                                          double time, String clip) {
        for (String name : new String[]{"body_mid", "body_rear", "chest_uncoil", "neck_2", "head", "jaw", "tail_3", "tail_tip"}) {
            double[] offset = switch (name) {
                case "body_mid" -> new double[]{0, -1.5, 3};
                case "body_rear" -> new double[]{0, -3.57, 3};
                case "chest_uncoil" -> new double[]{0, 6, -1};
                case "neck_2" -> new double[]{0, 7, -4};
                case "head" -> new double[]{0, -1, -6};
                case "jaw" -> new double[]{0, -4.5, -1.7};
                case "tail_3" -> new double[]{0, 10.7, 9.5};
                default -> new double[]{0, 15, 10};
            };
            double unit = CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE / 16.0;
            Vector3d point = nativeJoint(name, bones, tracks, time)
                    .transformPosition(new Vector3d(offset[0], offset[1], offset[2]));
            Vec3 expected = dragon.position().add(-point.x * unit, point.y * unit, -point.z * unit);
            ModelBonePartEntity<?> found = null;
            String partName = switch (name) {
                case "body_mid" -> "body_core_middle";
                case "body_rear" -> "body_core_rear";
                case "chest_uncoil" -> "neck_1";
                default -> name.startsWith("tail_") ? name + "_middle" : name;
            };
            for (var part : dragon.getParts()) {
                if (part instanceof ModelBonePartEntity<?> candidate && candidate.boneName().equals(partName))
                    found = candidate;
            }
            h.assertTrue(found != null && found.getBoundingBox().getCenter().distanceTo(expected) < .001,
                    "Server collision follows " + name + " in " + clip + " at " + time);
        }
    }

    private static double trackSpan(JsonObject track, int axis) {
        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
        for (var frame : track.entrySet()) {
            double value = frame.getValue().getAsJsonArray().get(axis).getAsDouble();
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        return maximum - minimum;
    }

    /** Read packaged animation tracks independently of the server's generated curves. */
    private static Matrix4d nativeJoint(String name, Map<String, JsonObject> bones, JsonObject animation, double time) {
        var bone = bones.get(name);
        var pivot = bone.getAsJsonArray("pivot");
        Matrix4d transform = new Matrix4d();
        double[] parentPivot = {0, 0, 0};
        if (bone.has("parent")) {
            String parent = bone.get("parent").getAsString();
            transform = nativeJoint(parent, bones, animation, time);
            var value = bones.get(parent).getAsJsonArray("pivot");
            for (int i = 0; i < 3; i++) parentPivot[i] = value.get(i).getAsDouble();
        }
        var channels = animation.has(name) ? animation.getAsJsonObject(name) : new JsonObject();
        double[] position = sampleFrame(channels, "position", time), rotation = sampleFrame(channels, "rotation", time);
        if (bone.has("rotation")) {
            for (int i = 0; i < 3; i++) rotation[i] += bone.getAsJsonArray("rotation").get(i).getAsDouble();
        }
        // Bedrock mirrors animation X translations, just like its X/Y rotations.
        transform.translate(pivot.get(0).getAsDouble() - parentPivot[0] - position[0],
                pivot.get(1).getAsDouble() - parentPivot[1] + position[1],
                pivot.get(2).getAsDouble() - parentPivot[2] + position[2]);
        double[] scale = sampleFrame(channels, "scale", time);
        return transform.rotateZ(Math.toRadians(rotation[2]))
                .rotateY(Math.toRadians(-rotation[1])).rotateX(Math.toRadians(-rotation[0]))
                .scale(scale[0], scale[1], scale[2]);
    }

    private static double[] sampleFrame(JsonObject channels, String channel, double time) {
        if (!channels.has(channel)) return channel.equals("scale") ? new double[]{1, 1, 1} : new double[]{0, 0, 0};
        var value = channels.get(channel);
        JsonArray frame;
        if (value.isJsonArray()) frame = value.getAsJsonArray();
        else {
            var frames = value.getAsJsonObject();
            var keys = frames.keySet().stream().sorted(java.util.Comparator.comparingDouble(Double::parseDouble)).toList();
            frame = frames.getAsJsonArray(keys.get(0));
            double before = Double.parseDouble(keys.get(0));
            for (int index = 1; index < keys.size(); index++) {
                double after = Double.parseDouble(keys.get(index));
                JsonArray next = frames.getAsJsonArray(keys.get(index));
                if (time <= after) {
                    double fraction = Math.max(0, Math.min(1, (time - before) / (after - before)));
                    return new double[]{
                            frame.get(0).getAsDouble() + fraction * (next.get(0).getAsDouble() - frame.get(0).getAsDouble()),
                            frame.get(1).getAsDouble() + fraction * (next.get(1).getAsDouble() - frame.get(1).getAsDouble()),
                            frame.get(2).getAsDouble() + fraction * (next.get(2).getAsDouble() - frame.get(2).getAsDouble())};
                }
                frame = next;
                before = after;
            }
        }
        return new double[]{frame.get(0).getAsDouble(), frame.get(1).getAsDouble(), frame.get(2).getAsDouble()};
    }

    @GameTest(template = "fate_test")
    public static void breathStartsAtAnimatedMouth(GameTestHelper h) throws Exception {
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setNoAi(true);
        dragon.setNoGravity(true);
        dragon.setPos(h.absoluteVec(new Vec3(4, 30, 4)));
        dragon.setYRot(0);
        dragon.setXRot(0);
        var update = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("updateBoneHitboxes");
        update.setAccessible(true);
        update.invoke(dragon);
        var breath = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("breathOrigin", Vec3.class);
        breath.setAccessible(true);
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 origin = (Vec3) breath.invoke(dragon, forward);
        ModelBonePartEntity<?> head = null;
        for (var part : dragon.getParts()) {
            if (part instanceof ModelBonePartEntity<?> bone && bone.boneName().equals("head")) head = bone;
        }
        double unit = CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE / 16.0;
        h.assertTrue(head != null, "Animated head has a collision anchor");
        Map<String, JsonObject> bones = new HashMap<>();
        for (var element : asset("geo/entity/crimson_sky_flame_flood_dragon.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            var bone = element.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }
        var tracks = asset("animations/entity/crimson_sky_flame_flood_dragon.animation.json")
                .getAsJsonObject("animations").getAsJsonObject("animation.crimson_sky_flame_flood_dragon.fly")
                .getAsJsonObject("bones");
        // Airborne flight reaches forward from its first frame. Rotate the
        // head-to-muzzle offset instead of assuming a motionless, level head.
        Vector3d offset = nativeJoint("head", bones, tracks, 0)
                .transformDirection(new Vector3d(0, -6, -11.3));
        Vec3 expected = head.getBoundingBox().getCenter().add(-offset.x * unit,
                offset.y * unit, -offset.z * unit + .15);
        h.assertTrue(origin.distanceTo(expected) < .001, "Breath starts at the animated model's muzzle");
        dragon.setXRot(-40);
        for (int i = 0; i < 40; i++) dragon.tick();
        Vec3 lifted = (Vec3) breath.invoke(dragon, forward);
        h.assertTrue(lifted.y > origin.y + 1, "Breath origin follows the animated neck when looking up");
        dragon.discard();
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void flameBreathReachesAheadOfRemadeMuzzle(GameTestHelper h) throws Exception {
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setNoAi(true); dragon.setNoGravity(true);
        dragon.setPos(h.absoluteVec(new Vec3(4, 30, 4))); dragon.setYRot(0);
        var update = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("updateBoneHitboxes");
        update.setAccessible(true); update.invoke(dragon);
        var breath = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod("breathOrigin", Vec3.class);
        breath.setAccessible(true);
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 origin = (Vec3) breath.invoke(dragon, forward);
        var target = net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(origin.add(0, -target.getBbHeight() * .5, 10));
        h.getLevel().addFreshEntity(target);
        var damage = CrimsonSkyFlameFloodDragonEntity.class.getDeclaredMethod(
                "damageBreathCone", net.minecraft.server.level.ServerLevel.class, Vec3.class);
        damage.setAccessible(true);
        h.runAfterDelay(2, () -> {
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(origin, origin).inflate(14)).contains(target),
                    "The forward test target is registered in the muzzle search area");
            h.assertTrue(dragon.hasLineOfSight(target), "The test target has an unobstructed flame path");
            float health = target.getHealth();
            try { damage.invoke(dragon, h.getLevel(), forward); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
            h.assertTrue(target.getHealth() < health,
                    "Breath finds targets in front of the muzzle rather than only near the torso");
            target.discard(); dragon.discard(); h.succeed();
        });
    }

    private static JsonObject asset(String relative) throws Exception {
        var input = CrimsonDragonModelGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/" + relative);
        if (input == null) throw new AssertionError("Missing dragon asset: " + relative);
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private CrimsonDragonModelGameTests() {}
}
