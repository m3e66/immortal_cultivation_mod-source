package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Applies the appropriate sky-texture sampler again after resource reloads. */
final class CosmicVoidTextureFiltering {
    private static final Map<ResourceLocation, FilterState> FILTER_STATES = new HashMap<>();

    private CosmicVoidTextureFiltering() {
    }

    static void bind(int sampler, ResourceLocation textureId) {
        bind(sampler, textureId, true);
    }

    static void bindCrisp(int sampler, ResourceLocation textureId) {
        bind(sampler, textureId, false);
    }

    /** Applies linear sampling directly to the uploaded OpenGL sky texture. */
    static void bindSkybox(int sampler, ResourceLocation textureId) {
        // Shader packs own more of the texture/render pipeline. Avoid mutating raw GL
        // sampler state in that path; the compatibility sky mesh never samples outside
        // the ordinary 0..1 texture range.
        if (ShaderPackCompatibility.isShaderPackActive()) {
            bind(sampler, textureId, true);
            return;
        }
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
        FilterState previous = FILTER_STATES.get(textureId);
        if (previous == null || previous.texture() != texture || !previous.linear()) {
            texture.setFilter(true, false);
            RenderSystem.bindTexture(texture.getId());
            RenderSystem.texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            RenderSystem.texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            RenderSystem.texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            RenderSystem.texParameter(
                    GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                    GL12.GL_CLAMP_TO_EDGE);
            FILTER_STATES.put(textureId, new FilterState(texture, true));
        }
        RenderSystem.setShaderTexture(sampler, texture.getId());
    }

    private static void bind(int sampler, ResourceLocation textureId, boolean linear) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
        FilterState previous = FILTER_STATES.get(textureId);
        if (previous == null || previous.texture() != texture || previous.linear() != linear) {
            texture.setFilter(linear, false);
            FILTER_STATES.put(textureId, new FilterState(texture, linear));
        }
        RenderSystem.setShaderTexture(sampler, textureId);
    }

    private record FilterState(AbstractTexture texture, boolean linear) {
    }
}
