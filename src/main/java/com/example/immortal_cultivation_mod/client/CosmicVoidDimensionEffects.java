package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Client effects for the blockless expanse and its sparse floating islands. */
public final class CosmicVoidDimensionEffects extends DimensionSpecialEffects {
    public CosmicVoidDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return new Vec3(0.002D, 0.001D, 0.006D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick,
                             Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy,
                             Runnable setupFog) {
        // Returning true bypasses LevelRenderer's normal sky path, including its fog
        // callback. Run it here so shader packs receive the expected sky fog state.
        setupFog.run();
        CosmicVoidSkyRenderer.render(level, ticks, partialTick, modelViewMatrix, camera);
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick,
                                PoseStack poseStack, double camX, double camY, double camZ,
                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick,
                                     LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }
}
