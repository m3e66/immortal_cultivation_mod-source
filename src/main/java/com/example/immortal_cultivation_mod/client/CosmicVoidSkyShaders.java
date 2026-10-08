package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Reload-safe custom shaders used only by the cosmic-void sky renderer. */
public final class CosmicVoidSkyShaders {
    private static final ResourceLocation SKY_SHADER_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cosmic_void_sky");
    private static final ResourceLocation NEBULA_SHADER_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cosmic_void_nebula");
    private static final ResourceLocation PLANET_SHADER_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cosmic_void_planet");
    private static final ResourceLocation CELESTIAL_SHADER_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cosmic_void_celestial");
    private static final ResourceLocation COMET_SHADER_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cosmic_void_comet");
    private static final ResourceLocation BLOOM_SHADER_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "cosmic_void_bloom");

    private static ShaderInstance skyShader;
    private static ShaderInstance nebulaShader;
    private static ShaderInstance planetShader;
    private static ShaderInstance celestialShader;
    private static ShaderInstance cometShader;
    private static ShaderInstance bloomShader;

    private CosmicVoidSkyShaders() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(), SKY_SHADER_ID,
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> skyShader = loaded);
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(), NEBULA_SHADER_ID,
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> nebulaShader = loaded);
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(), PLANET_SHADER_ID,
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> planetShader = loaded);
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(), CELESTIAL_SHADER_ID,
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> celestialShader = loaded);
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(), COMET_SHADER_ID,
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> cometShader = loaded);
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(), BLOOM_SHADER_ID,
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> bloomShader = loaded);
    }

    public static ShaderInstance skyShader() {
        return skyShader;
    }

    public static ShaderInstance nebulaShader() {
        return nebulaShader;
    }

    public static ShaderInstance planetShader() {
        return planetShader;
    }

    public static ShaderInstance celestialShader() {
        return celestialShader;
    }

    public static ShaderInstance cometShader() {
        return cometShader;
    }

    public static ShaderInstance bloomShader() {
        return bloomShader;
    }
}
