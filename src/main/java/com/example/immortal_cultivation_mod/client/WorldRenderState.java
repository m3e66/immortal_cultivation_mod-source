package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

/** Restores both Minecraft's shader selection and the actual GPU state around world overlays. */
final class WorldRenderState {
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int activeBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    private final int drawTarget = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int readTarget = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final ShaderInstance shader = RenderSystem.getShader();
    private final float[] color = RenderSystem.getShaderColor().clone();
    private final int[] shaderTextures = new int[12];
    // World overlays use the diffuse, overlay and lightmap texture units.
    private final int[] bindings = new int[3];
    private final BlendTarget[] blendTargets = captureBlendTargets();

    private record BlendTarget(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha,
                               int rgbEquation, int alphaEquation) {}

    private static BlendTarget[] captureBlendTargets() {
        var capabilities = GL.getCapabilities();
        if (!capabilities.OpenGL40 && !capabilities.GL_ARB_draw_buffers_blend) return new BlendTarget[0];
        BlendTarget[] targets = new BlendTarget[GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS)];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new BlendTarget(GL30.glIsEnabledi(GL11.GL_BLEND, i),
                    GL30.glGetIntegeri(GL14.GL_BLEND_SRC_RGB, i), GL30.glGetIntegeri(GL14.GL_BLEND_DST_RGB, i),
                    GL30.glGetIntegeri(GL14.GL_BLEND_SRC_ALPHA, i), GL30.glGetIntegeri(GL14.GL_BLEND_DST_ALPHA, i),
                    GL30.glGetIntegeri(GL20.GL_BLEND_EQUATION_RGB, i), GL30.glGetIntegeri(GL20.GL_BLEND_EQUATION_ALPHA, i));
        }
        return targets;
    }

    private WorldRenderState() {
        for (int i = 0; i < shaderTextures.length; i++) shaderTextures[i] = RenderSystem.getShaderTexture(i);
        try {
            for (int i = 0; i < bindings.length; i++) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                bindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
        } finally {
            GlStateManager._activeTexture(activeTexture);
        }
    }

    static WorldRenderState capture() { return new WorldRenderState(); }

    void restore() {
        RenderSystem.depthMask(depthWrite);
        RenderSystem.depthFunc(depthFunction);
        RenderSystem.lineWidth(lineWidth);
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        // Iris can change indexed blend state without changing Minecraft's cached
        // global blend factors. Force the real state, then restore every MRT slot.
        GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        for (int i = 0; i < blendTargets.length; i++) {
            BlendTarget target = blendTargets[i];
            if (target.enabled) GL30.glEnablei(GL11.GL_BLEND, i); else GL30.glDisablei(GL11.GL_BLEND, i);
            if (GL.getCapabilities().OpenGL40) {
                GL40.glBlendFuncSeparatei(i, target.srcRgb, target.dstRgb, target.srcAlpha, target.dstAlpha);
                GL40.glBlendEquationSeparatei(i, target.rgbEquation, target.alphaEquation);
            } else {
                ARBDrawBuffersBlend.glBlendFuncSeparateiARB(i, target.srcRgb, target.dstRgb, target.srcAlpha, target.dstAlpha);
                ARBDrawBuffersBlend.glBlendEquationSeparateiARB(i, target.rgbEquation, target.alphaEquation);
            }
        }
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        for (int i = 0; i < shaderTextures.length; i++) RenderSystem.setShaderTexture(i, shaderTextures[i]);
        RenderSystem.setShader(() -> shader);
        for (int i = 0; i < bindings.length; i++) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
            GlStateManager._bindTexture(bindings[i]);
        }
        GlStateManager._activeTexture(activeTexture);
        GlStateManager._bindTexture(activeBinding);
        // setShader alone does not rebind the program; ShaderInstance.clear() may have bound 0.
        GlStateManager._glUseProgram(program);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget);
    }
}
