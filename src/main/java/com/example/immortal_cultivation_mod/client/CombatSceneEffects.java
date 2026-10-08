package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;

/** Small scene-only blur/refraction pass. Never samples the framebuffer being written. */
final class CombatSceneEffects {
    private static TextureTarget snapshot;
    private static double lastUse;
    private CombatSceneEffects() {}

    static void release() {
        if (snapshot == null) return;
        int read = org.lwjgl.opengl.GL11.glGetInteger(36010);
        int draw = org.lwjgl.opengl.GL11.glGetInteger(36006);
        snapshot.destroyBuffers();
        snapshot = null;
        GlStateManager._glBindFramebuffer(36008, read);
        GlStateManager._glBindFramebuffer(36009, draw);
    }

    static void draw(GuiGraphics gui, float speed, float pressure, float spatial, double time) {
        // Iris owns a different render graph. Leave it intact; vignette, FOV and geometry still work.
        if (ShaderPackCompatibility.isShaderPackActive()) { release(); return; }
        if (speed + pressure + spatial < .003F) {
            if (time - lastUse > 60) release();
            return;
        }
        lastUse = time;
        var main = Minecraft.getInstance().getMainRenderTarget();
        int width = Math.min(960, main.viewWidth);
        int height = Math.max(1, main.viewHeight * width / Math.max(1, main.viewWidth));
        if (width <= 0) return;
        gui.flush();
        if (snapshot == null) snapshot = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        else if (snapshot.width != width || snapshot.height != height) snapshot.resize(width, height, Minecraft.ON_OSX);
        snapshot.setFilterMode(9729);
        try {
            GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
            GlStateManager._glBindFramebuffer(36009, snapshot.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.viewWidth, main.viewHeight,
                    0, 0, width, height, 16384, 9729);
            main.bindWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, snapshot.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            // Three faint radial samples blur only the outer scene; crosshair and HUD render later.
            for (int tap = 1; tap <= 3 && speed > .001F; tap++)
                mesh(gui, speed * .075F, tap * .012F * speed, 0, 0, time);
            if (pressure > .001F) {
                mesh(gui, pressure * .32F, 0, pressure * .004F, 0, time);
                for (int tap = 1; tap <= 2; tap++)
                    mesh(gui, pressure * .04F, tap * .003F * pressure, 0, 0, time);
            }
            if (spatial > .001F) {
                RenderSystem.colorMask(true, false, false, false);
                mesh(gui, spatial * .6F, 0, 0, .003F * spatial, time);
                RenderSystem.colorMask(false, true, true, false);
                mesh(gui, spatial * .6F, 0, 0, -.003F * spatial, time);
            }
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            main.bindWrite(true);
        }
    }

    private static void mesh(GuiGraphics gui, float alpha, float zoom, float warp, float shift, double time) {
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        var matrix = gui.pose().last().pose();
        for (int y = 0; y < 10; y++) for (int x = 0; x < 16; x++) {
            for (int corner = 0; corner < 4; corner++) {
                float u = (x + (corner == 1 || corner == 2 ? 1 : 0)) / 16F;
                float v = (y + (corner >= 2 ? 1 : 0)) / 10F;
                float nx = u - .5F, ny = v - .5F;
                float edge = Mth.clamp((nx * nx + ny * ny) * 3, 0, 1);
                float wave = (float) Math.sin(Math.sqrt(nx*nx+ny*ny) * 36 + time * .10) * warp;
                float sampleU = Mth.clamp(u - nx * zoom + shift + nx * wave, 0, 1);
                float sampleV = Mth.clamp(v - ny * zoom + ny * wave, 0, 1);
                buffer.addVertex(matrix, u * gui.guiWidth(), v * gui.guiHeight(), 0)
                        .setUv(sampleU, 1 - sampleV).setColor(1F, 1F, 1F, alpha * edge);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
