package com.example.immortal_cultivation_mod.client.qi;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The body's layered flame, fitted to a held weapon or a moving whip segment. */
public final class QiWeaponFlames {
    public static long draws;
    private QiWeaponFlames() {}

    public static void wrap(PoseStack pose, MultiBufferSource buffers, Vec3 start, Vec3 end,
                            double width, double depth, QiElementLook look, double time, float surge, double seed) {
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < .005) return;
        axis = axis.scale(1 / length);
        Vec3 base = start.subtract(axis.scale(length * .10));
        pose.pushPose();
        pose.translate(base.x, base.y, base.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0),
                new Vector3f((float) axis.x, (float) axis.y, (float) axis.z)));
        float pulse = 1 + surge * .18F;
        pose.scale((float) width * pulse, (float) length * (1.28F + surge * .12F), (float) depth * pulse);
        if (QiEnergyMesh.drawWeapon(pose, buffers, look, time + seed, surge, .78F, length < .45) > 0) draws++;
        pose.popPose();
    }

    /** Bounds are in the animated bone's local space, including authored cube rotations. */
    public static void bounds(PoseStack pose, MultiBufferSource buffers, Vec3 min, Vec3 max,
                              QiElementLook look, double time, float surge, double seed) {
        Vec3 center = min.add(max).scale(.5), size = max.subtract(min);
        if (size.y >= size.x && size.y >= size.z) {
            wrap(pose, buffers, new Vec3(center.x, min.y, center.z), new Vec3(center.x, max.y, center.z),
                    size.x * .55 + .055, size.z * .55 + .055, look, time, surge, seed);
        } else if (size.x >= size.z) {
            wrap(pose, buffers, new Vec3(min.x, center.y, center.z), new Vec3(max.x, center.y, center.z),
                    size.y * .55 + .055, size.z * .55 + .055, look, time, surge, seed);
        } else {
            wrap(pose, buffers, new Vec3(center.x, center.y, min.z), new Vec3(center.x, center.y, max.z),
                    size.x * .55 + .055, size.y * .55 + .055, look, time, surge, seed);
        }
    }
}
