package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.config.ModClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Adds the cosmic panorama and spherical custom celestial bodies to the Overworld sky. */
public final class OverworldCosmicSkyRenderer {
    private OverworldCosmicSkyRenderer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(OverworldCosmicSkyRenderer::onRenderLevel);
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || ShaderPackCompatibility.isRenderingShadowPass()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)
                || !ModClientConfig.enableModSky()) {
            return;
        }
        FogType fog = event.getCamera().getFluidInCamera();
        if (fog == FogType.LAVA || fog == FogType.POWDER_SNOW
                || minecraft.player.hasEffect(MobEffects.BLINDNESS)
                || minecraft.player.hasEffect(MobEffects.DARKNESS)) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float dayTime = TimeFastForwardClientState.interpolatedTimeOfDay(
                minecraft.level.getDayTime(), partialTick);
        float weatherAlpha = Math.max(0.0F,
                1.0F - minecraft.level.getRainLevel(partialTick));
        float panoramaAlpha = nightAlpha(dayTime) * weatherAlpha;
        float orbitAngle = dayTime / 24_000.0F * (float) (Math.PI * 2.0D);
        CosmicVoidSkyRenderer.renderPanorama(
                event.getModelViewMatrix(), panoramaAlpha,
                minecraft.level.getGameTime() + partialTick, orbitAngle);
        // Active packs frequently own their Sun/Moon passes. Keep the compatible
        // panorama overlay, but leave those pack-controlled celestial bodies alone.
        if (!ShaderPackCompatibility.isShaderPackActive()
                && CosmicVoidSkyShaders.celestialShader() != null) {
            var state=CosmicVoidSkyRenderer.SkyPassState.capture();
            try {
                CosmicVoidDynamicVfxRenderer.renderOverworldCelestials(
                        event.getModelViewMatrix(), dayTime / 24_000.0F,
                        weatherAlpha);
            } finally {
                state.restore();
            }
        }
    }

    private static float nightAlpha(float dayTime) {
        if (dayTime >= 13_000.0F && dayTime < 14_000.0F) {
            return smoothStep((dayTime - 13_000.0F) / 1_000.0F);
        }
        if (dayTime >= 14_000.0F && dayTime <= 22_000.0F) {
            return 1.0F;
        }
        if (dayTime > 22_000.0F && dayTime < 23_000.0F) {
            return 1.0F - smoothStep((dayTime - 22_000.0F) / 1_000.0F);
        }
        return 0.0F;
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
