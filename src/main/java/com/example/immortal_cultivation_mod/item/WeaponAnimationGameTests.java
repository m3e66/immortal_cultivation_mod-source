package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.network.WeaponUseAnimations;
import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.GeckoLibSerializer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class WeaponAnimationGameTests {
    @GameTest(template = "fate_test")
    public static void animationLayersRecoverAcrossPlayerReplacement(GameTestHelper helper) throws Exception {
        var guard = loadCombatClip("block", "weapon_block");
        var attack = loadCombatClip("sword", "sword_1");
        var oldStack = new dev.kosmx.playerAnim.api.layered.AnimationStack();
        var newStack = new dev.kosmx.playerAnim.api.layered.AnimationStack();
        var oldState = new com.example.immortal_cultivation_mod.combat.CombatAnimationLayers();
        var newState = new com.example.immortal_cultivation_mod.combat.CombatAnimationLayers();
        oldState.attach(oldStack);
        oldState.blockPose = true;
        oldState.setAnimation(guard.playAnimation());
        newState.attach(newStack);
        newState.setAnimation(attack.playAnimation());
        oldState.detach();
        helper.assertTrue(!oldStack.isActive() && newStack.isActive(),
                "Discarding the previous player clears only its layers, not replacement player's attack");
        // Repeated registration must not multiply ticking speed or leave an older looping layer above attacks.
        for (int i = 0; i < 20; i++) newState.attach(newStack);
        var reference = attack.playAnimation();
        for (int i = 0; i < 40; i++) {
            helper.assertTrue(newStack.isActive() == reference.isActive(),
                    "One animation tick per player tick after repeated registration");
            newStack.tick();
            reference.tick();
        }
        newState.binding.setAnimation(guard.playAnimation());
        newState.skill.setAnimation(guard.playAnimation());
        newState.detach();
        helper.assertTrue(!newStack.isActive() && !newState.playing(),
                "Disconnect clears attack, binding, and skill layers together");
        newState.attach(newStack);
        newState.setAnimation(attack.playAnimation());
        helper.assertTrue(newStack.isActive(), "Animation can play again after reconnect cleanup");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lateGuardReleaseDoesNotCancelNewAttack(GameTestHelper helper) throws Exception {
        var guard = loadCombatClip("block", "weapon_block");
        var attack = loadCombatClip("sword", "sword_1");
        var stack = new dev.kosmx.playerAnim.api.layered.AnimationStack();
        var state = new com.example.immortal_cultivation_mod.combat.CombatAnimationLayers();
        state.attach(stack);
        for (int repeat = 0; repeat < 20; repeat++) {
            state.blockPose = false;
            state.setAnimation(guard.playAnimation());
            state.beginAttack();
            var playback = attack.playAnimation();
            state.setAnimation(playback);
            state.stopBlock();
            helper.assertTrue(state.getAnimation() == playback && stack.isActive(),
                    "Guard-stop arriving after a swing does not erase the newer animation");
            for (int i = 0; i < 60; i++) stack.tick();
            helper.assertTrue(!stack.isActive(), "Finished attack leaves no stale guard hiding the next swing");
            state.blockPose = true;
            state.setAnimation(guard.playAnimation());
            helper.assertTrue(stack.isActive(), "Guard still starts after repeated attacks");
            state.stopBlock();
            helper.assertTrue(!stack.isActive(), "An actual guard release still clears its pose");
        }
        helper.succeed();
    }

    private static dev.kosmx.playerAnim.api.IPlayable loadCombatClip(String file, String name) throws Exception {
        try (var stream = WeaponAnimationGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/player_animations/" + file + ".json")) {
            return dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs.deserialize("json", stream)
                    .stream().filter(clip -> name.equals(clip.getName())).findFirst().orElseThrow();
        }
    }

    @GameTest(template = "fate_test")
    public static void combatPlaybackLoadsMovesAndReleases(GameTestHelper helper) throws Exception {
        var clips = new HashMap<String, dev.kosmx.playerAnim.api.IPlayable>();
        for (String file : new String[]{"sword", "punch", "block", "dual_block", "spear", "fuchen", "whip"}) {
            try (var stream = WeaponAnimationGameTests.class.getResourceAsStream(
                    "/assets/immortal_cultivation_mod/player_animations/" + file + ".json")) {
                helper.assertTrue(stream != null, "Packaged combat resource: " + file);
                for (var clip : dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs.deserialize("json", stream)) {
                    clips.put(clip.getName(), clip);
                }
            }
        }
        for (String name : new String[]{"sword_1", "sword_2", "sword_3", "punch_1", "punch_2", "punch_3",
                "weapon_block", "weapon_dual_block", "spear_heavy_thrust", "fuchen_heavy_spin", "whip_heavy"}) {
            var clip = clips.get(name);
            helper.assertTrue(clip != null, "Runtime codec finds combat clip: " + name);
            var stack = new dev.kosmx.playerAnim.api.layered.AnimationStack();
            var layer = new dev.kosmx.playerAnim.api.layered.ModifierLayer<>();
            var firstPerson = new dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier();
            firstPerson.setCurrentFirstPersonMode(dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL);
            layer.addModifierLast(firstPerson);
            stack.addAnimLayer(1000, layer);
            layer.setAnimation(clip.playAnimation());
            boolean movedArm = false;
            for (int tick = 0; tick < 80; tick++) {
                stack.setupAnim(0);
                var arm = stack.get3DTransform("rightArm", TransformType.ROTATION, 0, Vec3f.ZERO);
                movedArm |= Math.abs(arm.getX()) + Math.abs(arm.getY()) + Math.abs(arm.getZ()) > .1F;
                if (tick == 2) {
                    helper.assertTrue(stack.isActive() && layer.isActive(), "Combat layer owns pose during " + name);
                    helper.assertTrue(stack.getFirstPersonMode(0).isEnabled(), "Combat visible in first person: " + name);
                }
                stack.tick();
            }
            helper.assertTrue(movedArm, "Loaded clip moves player arm: " + name);
            boolean guard = name.equals("weapon_block") || name.equals("weapon_dual_block");
            helper.assertTrue(stack.isActive() == guard, "Only guards remain active until released: " + name);
            layer.setAnimation(null);
            helper.assertTrue(!stack.isActive(), "Released combat layer gives pose back: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void dualBlockBladesCrossInFrontAndHold(GameTestHelper helper) throws Exception {
        try (var reader = new InputStreamReader(WeaponAnimationGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/player_animations/dual_block.json"), StandardCharsets.UTF_8)) {
            var clip = GeckoLibSerializer.deserialize(JsonParser.parseReader(reader).getAsJsonObject()).getFirst();
            var playback = new KeyframeAnimationPlayer(clip);
            for (int tick = 0; tick < 80; tick++) playback.tick();
            playback.setupAnim(0);
            helper.assertTrue(playback.isActive(), "Crossed guard stays raised until block release");

            // Compose the same arm, vanilla hand, and Player Animator item transforms used by ItemInHandLayer.
            // Weapon models' +/-90 degree Y rotations leave their upright blade axis unchanged.
            var right = blockingHandTransform(playback, "right", 1);
            var left = blockingHandTransform(playback, "left", -1);
            var rightGrip = right.transformPosition(new org.joml.Vector3f());
            var leftGrip = left.transformPosition(new org.joml.Vector3f());
            var rightBlade = right.transformDirection(new org.joml.Vector3f(0, 1, 0));
            var leftBlade = left.transformDirection(new org.joml.Vector3f(0, 1, 0));
            helper.assertTrue(rightGrip.x < 0 && leftGrip.x > 0, "Grips stay on opposite sides of the chest");
            helper.assertTrue(rightBlade.x > .5F && leftBlade.x < -.5F
                    && rightBlade.y < -.5F && leftBlade.y < -.5F, "Both blades rise inward, forming an X instead of parallel bars");
            var rightCross = new org.joml.Vector3f(rightGrip).fma(-rightGrip.x / rightBlade.x, rightBlade);
            var leftCross = new org.joml.Vector3f(leftGrip).fma(-leftGrip.x / leftBlade.x, leftBlade);
            helper.assertTrue(Math.abs(rightCross.y - leftCross.y) < .01F, "Blade axes meet at chest center in front view");
            helper.assertTrue(rightCross.z < -6 && leftCross.z < -6, "Crossing stays in front of the torso");
            helper.assertTrue(Math.abs(rightCross.z - leftCross.z) > .5F
                    && Math.abs(rightCross.z - leftCross.z) < 2, "Small depth gap avoids coplanar blade flicker");
        }
        helper.succeed();
    }

    private static org.joml.Matrix4f blockingHandTransform(KeyframeAnimationPlayer playback, String side, int sign) {
        var armPosition = playback.get3DTransform(side + "Arm", TransformType.POSITION, 0, Vec3f.ZERO);
        var armRotation = playback.get3DTransform(side + "Arm", TransformType.ROTATION, 0, Vec3f.ZERO);
        var itemPosition = playback.get3DTransform(side + "Item", TransformType.POSITION, 0, Vec3f.ZERO);
        var itemRotation = playback.get3DTransform(side + "Item", TransformType.ROTATION, 0, Vec3f.ZERO);
        return new org.joml.Matrix4f().translate(armPosition.getX(), armPosition.getY(), armPosition.getZ())
                .rotateZYX(armRotation.getZ(), armRotation.getY(), armRotation.getX())
                .rotateX(-(float)Math.PI / 2).rotateY((float)Math.PI)
                .translate(sign, 2, -10)
                .translate(itemPosition.getX(), itemPosition.getY(), itemPosition.getZ())
                .rotateZYX(itemRotation.getZ(), itemRotation.getY(), itemRotation.getX());
    }

    @GameTest(template = "fate_test")
    public static void skillClipsDecodeAnimateFullBodyAndFinish(GameTestHelper helper) throws Exception {
        String path = "/assets/immortal_cultivation_mod/player_animations/weapon_skills.json";
        var stream = WeaponAnimationGameTests.class.getResourceAsStream(path);
        helper.assertTrue(stream != null, "Weapon skill resource is packaged");
        var clips = new HashMap<String, KeyframeAnimation>();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            // The actual Player Animator decoder, not a parallel JSON approximation.
            for (var clip : GeckoLibSerializer.deserialize(JsonParser.parseReader(reader).getAsJsonObject())) {
                helper.assertTrue(clips.put(clip.getName(), clip) == null, "Clip names are unique");
            }
        }
        var ids = new HashSet<Integer>();
        var names = new HashSet<String>();
        for (var type : WeaponUseAnimations.Type.values()) {
            helper.assertTrue(ids.add(type.packetId), "Packet IDs are unique");
            helper.assertTrue(WeaponUseAnimations.fromPacket(type.packetId) == type, "Packet selects correct family");
            helper.assertTrue(names.add(type.clip), "Each family has its own new clip");
            var clip = clips.get(type.clip);
            helper.assertTrue(clip != null, "Player Animator can decode " + type.clip);
            helper.assertTrue(!clip.isInfinite, "Skill must finish without looping: " + type.clip);
            var playback = new KeyframeAnimationPlayer(clip);
            var moving = new HashSet<String>();
            for (int tick = 0; tick < 40; tick++) {
                playback.setupAnim(0);
                for (String bone : new String[]{"body", "head", "rightArm", "leftArm", "rightLeg", "leftLeg"}) {
                    var rotation = playback.get3DTransform(bone, TransformType.ROTATION, 0, new Vec3f(0, 0, 0));
                    if (Math.abs(rotation.getX()) + Math.abs(rotation.getY()) + Math.abs(rotation.getZ()) > .01F) moving.add(bone);
                }
                playback.tick();
            }
            helper.assertTrue(moving.size() == 6, "All six player parts move: " + type.clip);
            helper.assertTrue(!playback.isActive(), "Skill returns control to normal player animation: " + type.clip);
        }
        for (var spec : HigherWeaponSpec.values()) {
            helper.assertTrue(clips.containsKey(WeaponUseAnimations.type(spec).clip), "Every higher weapon has a player clip: " + spec);
        }
        helper.assertTrue(WeaponUseAnimations.type(HigherWeaponSpec.FLAME_PATTERN_DAO) == WeaponUseAnimations.Type.SABER,
                "Saber variants share the saber clip");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void skillMirroringSwapsPlayerArms(GameTestHelper helper) throws Exception {
        try (var reader = new InputStreamReader(WeaponAnimationGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/player_animations/weapon_skills.json"), StandardCharsets.UTF_8)) {
            var clip = GeckoLibSerializer.deserialize(JsonParser.parseReader(reader).getAsJsonObject()).getFirst();
            var playback = new KeyframeAnimationPlayer(clip, 4);
            playback.setupAnim(0);
            var mirror = new MirrorModifier(true);
            mirror.setAnim(playback);
            var right = playback.get3DTransform("rightArm", TransformType.ROTATION, 0, new Vec3f(0, 0, 0));
            var left = mirror.get3DTransform("leftArm", TransformType.ROTATION, 0, new Vec3f(0, 0, 0));
            helper.assertTrue(Math.abs(right.getX() - left.getX()) < .001F
                    && Math.abs(right.getY() + left.getY()) < .001F
                    && Math.abs(right.getZ() + left.getZ()) < .001F, "Left-handed stance mirrors actual clip transforms");
        }
        helper.succeed();
    }
}
