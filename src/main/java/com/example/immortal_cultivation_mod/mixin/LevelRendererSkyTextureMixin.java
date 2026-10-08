package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.CosmicVoidSkyShaders;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hides vanilla's flat Sun and Moon only when their custom Overworld replacements are ready. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyTextureMixin {
    private static final ResourceLocation IMMORTAL_CULTIVATION_TRANSPARENT_SKY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "textures/item/empty.png");

    @Shadow
    private ClientLevel level;

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
                    ordinal = 0))
    private void immortalCultivation$replaceOverworldSun(int sampler, ResourceLocation texture) {
        RenderSystem.setShaderTexture(sampler, replacementTexture(texture));
    }

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
                    ordinal = 1))
    private void immortalCultivation$replaceOverworldMoon(int sampler, ResourceLocation texture) {
        RenderSystem.setShaderTexture(sampler, replacementTexture(texture));
    }

    private ResourceLocation replacementTexture(ResourceLocation vanillaTexture) {
        return level != null && level.dimension().equals(Level.OVERWORLD)
                && CosmicVoidSkyShaders.celestialShader() != null
                && !ShaderPackCompatibility.isShaderPackActive()
                ? IMMORTAL_CULTIVATION_TRANSPARENT_SKY_TEXTURE
                : vanillaTexture;
    }
}
