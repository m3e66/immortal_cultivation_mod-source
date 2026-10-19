package com.example.immortal_cultivation_mod.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;

/** Captures animated pivots after GeckoLib applies bone transforms; never uses a shared model's stale entity. */
public final class BeastVfxAnchors {
    private static final Map<UUID, Frame> FRAMES = new HashMap<>();
    private static Object world;
    private record Frame(long tick, Map<String, Vec3> bones) {}
    private BeastVfxAnchors() {}
    public static boolean relevant(String name) {
        return name.equals("body") || name.equals("torso") || name.equals("head") || name.equals("jaw")
                || name.equals("hand_l") || name.equals("hand_r") || name.equals("foot_l") || name.equals("foot_r")
                || name.equals("horn") || name.equals("stinger") || name.equals("left_mandible") || name.equals("right_mandible")
                || name.equals("tail_tip") || name.equals("tail_5") || name.equals("tail_3")
                || name.equals("tail_0_tip") || name.equals("tail_1_tip")
                || name.equals("pincer_left") || name.equals("pincer_right") || name.equals("shell_front")
                || name.equals("shell_rear") || name.equals("left_shell") || name.equals("right_shell")
                || name.equals("wing_left_hand") || name.equals("wing_right_hand")
                || name.equals("fore_hoof_l") || name.equals("fore_hoof_r")
                || name.equals("antler_l_s0") || name.equals("antler_r_s0")
                || name.equals("front_leg_left") || name.equals("front_leg_right")
                || name.equals("horn_left") || name.equals("horn_right");
    }
    public static void capture(Entity entity, GeoBone bone, float partial) {
        if (world != entity.level()) { FRAMES.clear(); world = entity.level(); }
        long tick = entity.level().getGameTime();
        if (FRAMES.size() >= 64 && !FRAMES.containsKey(entity.getUUID())) {
            FRAMES.entrySet().removeIf(e -> tick - e.getValue().tick > 2);
            if (FRAMES.size() >= 64) return;
        }
        Frame frame = FRAMES.get(entity.getUUID());
        if (frame == null || frame.tick != tick) { frame = new Frame(tick, new HashMap<>()); FRAMES.put(entity.getUUID(),frame); }
        var pivot = bone.getWorldPosition();
        Vec3 value = new Vec3(pivot.x,pivot.y,pivot.z).add(entity.getPosition(partial).subtract(entity.position()));
        if (Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z)) frame.bones.put(bone.getName(),value);
    }
    static Vec3 point(Entity entity, Vec3 fallback, String... names) {
        if (world != Minecraft.getInstance().level) { FRAMES.clear(); world = Minecraft.getInstance().level; }
        Frame frame=FRAMES.get(entity.getUUID());
        if (frame != null && entity.level().getGameTime()-frame.tick <= 1)
            for(String name:names) { Vec3 p=frame.bones.get(name); if(p!=null) return p; }
        return fallback;
    }
}
