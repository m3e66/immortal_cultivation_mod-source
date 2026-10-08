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
        Vec3 tangent = b.subtract(a);
        if (tangent.lengthSqr() < .000001 || alphaA + alphaB <= .001) return 0;
        Vec3 side = tangent.cross(a.lerp(b, .5).subtract(camera));
        if (side.lengthSqr() < .000001) side = CultivationFlightTrail.side(tangent, null);
        side = side.normalize().scale(width);
        // Fade across the width too: flat alpha bands produce visible rectangular edges between filaments.
        Vec3 ac = a.subtract(camera), bc = b.subtract(camera);
        vertex(out, matrix, ac.subtract(side), r, g, blue, 0);
        vertex(out, matrix, ac, r, g, blue, alphaA);
        vertex(out, matrix, bc, r, g, blue, alphaB);
        vertex(out, matrix, bc.subtract(side), r, g, blue, 0);
        vertex(out, matrix, ac, r, g, blue, alphaA);
        vertex(out, matrix, ac.add(side), r, g, blue, 0);
        vertex(out, matrix, bc.add(side), r, g, blue, 0);
        vertex(out, matrix, bc, r, g, blue, alphaB);
        // Soft caps overlap at sharp bends instead of leaving hard wedge-shaped gaps.
        Vec3 extension=tangent.normalize().scale(width);
        for(int end=0;end<2;end++){
            Vec3 center=end==0?ac:bc,tip=center.add(extension.scale(end==0?-1:1));
            float alpha=end==0?alphaA:alphaB;
            for(int sign=-1;sign<=1;sign+=2){
                vertex(out,matrix,center,r,g,blue,alpha);
                vertex(out,matrix,center.add(side.scale(sign)),r,g,blue,0);
                vertex(out,matrix,tip,r,g,blue,0);
                vertex(out,matrix,center,r,g,blue,alpha);
            }
        }
        return 6;
    }

    /** Connected blade strip: fixed world-space thickness, without overlapping round segment caps. */
    static int bladeStrip(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 camera,
                          Vec3 perpendicular, double widthA, double widthB,
                          float r, float g, float blue, float alphaA, float alphaB) {
        Vec3 ac = a.subtract(camera), bc = b.subtract(camera);
        Vec3 sideA = perpendicular.scale(widthA), sideB = perpendicular.scale(widthB);
        vertex(out, matrix, ac.subtract(sideA), r, g, blue, 0);
        vertex(out, matrix, ac, r, g, blue, alphaA);
        vertex(out, matrix, bc, r, g, blue, alphaB);
        vertex(out, matrix, bc.subtract(sideB), r, g, blue, 0);
        vertex(out, matrix, ac, r, g, blue, alphaA);
        vertex(out, matrix, ac.add(sideA), r, g, blue, 0);
        vertex(out, matrix, bc.add(sideB), r, g, blue, 0);
        vertex(out, matrix, bc, r, g, blue, alphaB);
        return 2;
    }

    static int halo(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera, Vec3 right, Vec3 up,
                             double size, float r, float g, float b, float alpha) {
        Vec3 c = center.subtract(camera);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8, z = (i + 1) * Math.PI / 8;
            Vec3 p = c.add(right.scale(Math.cos(a) * size)).add(up.scale(Math.sin(a) * size));
            Vec3 q = c.add(right.scale(Math.cos(z) * size)).add(up.scale(Math.sin(z) * size));
            vertex(out, matrix, c, r, g, b, alpha); vertex(out, matrix, p, r, g, b, 0);
            vertex(out, matrix, q, r, g, b, 0); vertex(out, matrix, c, r, g, b, alpha);

        }
        return 16;
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 point, float r, float g, float b, float alpha) {
        out.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z).setColor(r, g, b, Mth.clamp(alpha, 0, 1));
    }
}
