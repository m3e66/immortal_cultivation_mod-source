package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.physics.CultivationFlightTrail;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Shared additive material and soft geometry for flight wakes and Photon spell effects. */
final class FilamentVfx {
    private FilamentVfx() {}
    private static final RenderStateShard.TransparencyStateShard ADDITIVE = new RenderStateShard.TransparencyStateShard(
            "cultivation_additive_preserve_alpha", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });
    // Used only by AFTER_PARTICLES world passes, while the scene still has terrain depth.
    // Fabulous starts that stage on a transparent particle framebuffer. Its compositor
    // discards pixels with zero alpha, so alpha-preserving additive light must instead
    // blend into the scene. MAIN_TARGET alone is a no-op and does not switch buffers.
    // Leave the current target alone in Fast/Fancy and shader-pack rendering; those
    // pipelines own the active world framebuffer. Restore Fabulous's particle target
    // afterwards so later stage subscribers see the target they expect.
    static final RenderStateShard.OutputStateShard WORLD_TARGET = new RenderStateShard.OutputStateShard(
            "cultivation_world_depth", () -> {
                if (useFabulousSceneTarget()) {
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                }
            }, () -> {
                if (useFabulousSceneTarget()) {
                    RenderStateShard.PARTICLES_TARGET.setupRenderState();
                }
            });
    static final RenderType TYPE = createType("cultivation_glow_filaments", WORLD_TARGET);
    // Entity renderers draw before the particle target is populated in Fabulous mode.
    static final RenderType ENTITY_TYPE = createType("cultivation_entity_glow", RenderStateShard.MAIN_TARGET);
    private static boolean useFabulousSceneTarget() {
        return Minecraft.useShaderTransparency() && !ShaderPackCompatibility.isShaderPackActive();
    }
    private static RenderType createType(String name, RenderStateShard.OutputStateShard target) {
        return RenderType.create(name,
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    // Preserve destination alpha as well as color; later screen passes
                    // must not treat the soft edges as holes in the world framebuffer.
                    .setTransparencyState(ADDITIVE)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(target)
                    .createCompositeState(false));
    }

    static int ribbon(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 camera, double width,
                               float r, float g, float blue, float alphaA, float alphaB) {
        double tx = b.x - a.x, ty = b.y - a.y, tz = b.z - a.z;
        double tangentLengthSquared = tx * tx + ty * ty + tz * tz;
        if (tangentLengthSquared < .000001 || alphaA + alphaB <= .001) return 0;
        double mx = a.x + .5 * tx - camera.x;
        double my = a.y + .5 * ty - camera.y;
        double mz = a.z + .5 * tz - camera.z;
        double sx = ty * mz - tz * my, sy = tz * mx - tx * mz, sz = tx * my - ty * mx;
        if (sx * sx + sy * sy + sz * sz < .000001) {
            Vec3 fallback = CultivationFlightTrail.side(new Vec3(tx, ty, tz), null);
            sx = fallback.x; sy = fallback.y; sz = fallback.z;
        }
        double sideLength = Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx = sx / sideLength * width; sy = sy / sideLength * width; sz = sz / sideLength * width;
        double ax = a.x - camera.x, ay = a.y - camera.y, az = a.z - camera.z;
        double bx = b.x - camera.x, by = b.y - camera.y, bz = b.z - camera.z;
        // Emit the same six quads, including width fades and overlapping bend caps.
        vertex(out, matrix, ax - sx, ay - sy, az - sz, r, g, blue, 0);
        vertex(out, matrix, ax, ay, az, r, g, blue, alphaA);
        vertex(out, matrix, bx, by, bz, r, g, blue, alphaB);
        vertex(out, matrix, bx - sx, by - sy, bz - sz, r, g, blue, 0);
        vertex(out, matrix, ax, ay, az, r, g, blue, alphaA);
        vertex(out, matrix, ax + sx, ay + sy, az + sz, r, g, blue, 0);
        vertex(out, matrix, bx + sx, by + sy, bz + sz, r, g, blue, 0);
        vertex(out, matrix, bx, by, bz, r, g, blue, alphaB);
        double tangentLength = Math.sqrt(tangentLengthSquared);
        double ex = tx / tangentLength * width, ey = ty / tangentLength * width, ez = tz / tangentLength * width;
        for (int end = 0; end < 2; end++) {
            double cx = end == 0 ? ax : bx, cy = end == 0 ? ay : by, cz = end == 0 ? az : bz;
            double signEnd = end == 0 ? -1 : 1;
            float alpha = end == 0 ? alphaA : alphaB;
            for (int sign = -1; sign <= 1; sign += 2) {
                vertex(out, matrix, cx, cy, cz, r, g, blue, alpha);
                vertex(out, matrix, cx + sx * sign, cy + sy * sign, cz + sz * sign, r, g, blue, 0);
                vertex(out, matrix, cx + ex * signEnd, cy + ey * signEnd, cz + ez * signEnd, r, g, blue, 0);
                vertex(out, matrix, cx, cy, cz, r, g, blue, alpha);
            }
        }
        return 6;
    }

    /** Connected blade strip: fixed world-space thickness, without overlapping round segment caps. */
    static int bladeStrip(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 camera,
                          Vec3 perpendicular, double widthA, double widthB,
                          float r, float g, float blue, float alphaA, float alphaB) {
        double ax = a.x - camera.x, ay = a.y - camera.y, az = a.z - camera.z;
        double bx = b.x - camera.x, by = b.y - camera.y, bz = b.z - camera.z;
        double sax = perpendicular.x * widthA, say = perpendicular.y * widthA, saz = perpendicular.z * widthA;
        double sbx = perpendicular.x * widthB, sby = perpendicular.y * widthB, sbz = perpendicular.z * widthB;
        vertex(out, matrix, ax - sax, ay - say, az - saz, r, g, blue, 0);
        vertex(out, matrix, ax, ay, az, r, g, blue, alphaA);
        vertex(out, matrix, bx, by, bz, r, g, blue, alphaB);
        vertex(out, matrix, bx - sbx, by - sby, bz - sbz, r, g, blue, 0);
        vertex(out, matrix, ax, ay, az, r, g, blue, alphaA);
        vertex(out, matrix, ax + sax, ay + say, az + saz, r, g, blue, 0);
        vertex(out, matrix, bx + sbx, by + sby, bz + sbz, r, g, blue, 0);
        vertex(out, matrix, bx, by, bz, r, g, blue, alphaB);
        return 2;
    }

    static int halo(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera, Vec3 right, Vec3 up,
                             double size, float r, float g, float b, float alpha) {
        double cx = center.x - camera.x, cy = center.y - camera.y, cz = center.z - camera.z;
        for (int i = 0; i < 16; i++) {
            double px = HALO_COS[i] * size, py = HALO_SIN[i] * size;
            double qx = HALO_COS[i + 1] * size, qy = HALO_SIN[i + 1] * size;
            vertex(out, matrix, cx, cy, cz, r, g, b, alpha);
            vertex(out, matrix, cx + right.x * px + up.x * py,
                    cy + right.y * px + up.y * py, cz + right.z * px + up.z * py, r, g, b, 0);
            vertex(out, matrix, cx + right.x * qx + up.x * qy,
                    cy + right.y * qx + up.y * qy, cz + right.z * qx + up.z * qy, r, g, b, 0);
            vertex(out, matrix, cx, cy, cz, r, g, b, alpha);
        }
        return 16;
    }
    private static final double[] HALO_COS = new double[17], HALO_SIN = new double[17];
    static {
        for (int i = 0; i <= 16; i++) {
            double angle = i * Math.PI / 8;
            HALO_COS[i] = Math.cos(angle); HALO_SIN[i] = Math.sin(angle);
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                               float r, float g, float b, float alpha) {
        VfxVertexWriter.vertex(out, matrix, (float) x, (float) y, (float) z).setColor(r, g, b, Mth.clamp(alpha, 0, 1));
    }
}
