package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.SpiritualSenseClient;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.IOException;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class SpiritualReconstructionRenderer {
   private static final ResourceLocation SHADER_ID = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "spiritual_reconstruction");
   private static ShaderInstance shader;
   private static final Function<ResourceLocation, RenderType> RENDER_TYPES = Util.memoize(SpiritualReconstructionRenderer::createRenderType);
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   private static final RenderType FALLBACK_RENDER_TYPE = createFallbackRenderType();

   private SpiritualReconstructionRenderer() {
   }

   public static void registerShader(RegisterShadersEvent event) throws IOException {
      event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER_ID, DefaultVertexFormat.NEW_ENTITY), (loaded) -> shader = loaded);
   }

   public static boolean isReady() {
      return shader != null && ShaderPackCompatibility.useCustomCoreShaders()
              && !ShaderPackCompatibility.isRenderingShadowPass();
   }

   public static RenderType renderType(ResourceLocation texture) {
      return (RenderType)RENDER_TYPES.apply(texture);
   }

   public static RenderType fallbackRenderType() {
      return FALLBACK_RENDER_TYPE;
   }

   public static int fallbackArgb(SpiritualSenseClient.Reconstruction reconstruction) {
      int original = reconstruction.argb();
      int originalAlpha = original >>> 24 & 255;
      int alpha = Mth.clamp(Math.round(42.0F + (float)originalAlpha * 0.42F), 48, 132);
      return alpha << 24 | original & 16777215;
   }

   public static int encodedLight(SpiritualSenseClient.Reconstruction reconstruction) {
      return packControls(reconstruction.interference(), reconstruction.pressure());
   }

   public static int encodedOverlay(SpiritualSenseClient.Reconstruction reconstruction) {
      return packControls(reconstruction.concealment(), reconstruction.signatureClass());
   }

   private static int packControls(float low, float high) {
      int lowValue = Mth.clamp(Math.round(low * 255.0F), 0, 255);
      int highValue = Mth.clamp(Math.round(high * 255.0F), 0, 255);
      return lowValue | highValue << 16;
   }

   private static RenderType createRenderType(ResourceLocation texture) {
      RenderType.CompositeState state = CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(() -> shader)).setTextureState(new RenderStateShard.TextureStateShard(texture, false, false)).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL).setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false);
      return RenderType.create("immortal_cultivation_mod:spiritual_reconstruction", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, false, true, state);
   }

   private static RenderType createFallbackRenderType() {
      RenderType.CompositeState state = CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader)).setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false)).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL).setWriteMaskState(RenderStateShard.COLOR_WRITE).setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(false);
      return RenderType.create("immortal_cultivation_mod:spiritual_reconstruction_fallback", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, false, true, state);
   }

   public static void onGeoEntityPost(GeoRenderEvent.Entity.Post event) {
      Entity entity = event.getEntity();
      SpiritualSenseClient.Reconstruction reconstruction = SpiritualSenseClient.reconstructionFor(entity.getId());
      if (reconstruction != null && entity instanceof GeoAnimatable animatable) {
         GeoEntityRenderer renderer = event.getRenderer();
         ResourceLocation texture = renderer.getTextureLocation(entity);
         RenderType fallback = fallbackRenderType();
         VertexConsumer fallbackVertices = event.getBufferSource().getBuffer(fallback);
         renderer.reRender(event.getModel(), event.getPoseStack(), event.getBufferSource(), animatable, fallback, fallbackVertices, event.getPartialTick(), 15728880, OverlayTexture.NO_OVERLAY, fallbackArgb(reconstruction));
         if (isReady()) {
            RenderType renderType = renderType(texture);
            VertexConsumer vertices = event.getBufferSource().getBuffer(renderType);
            renderer.reRender(event.getModel(), event.getPoseStack(), event.getBufferSource(), animatable, renderType, vertices, event.getPartialTick(), encodedLight(reconstruction), encodedOverlay(reconstruction), reconstruction.argb());
         }
      }
   }
}
