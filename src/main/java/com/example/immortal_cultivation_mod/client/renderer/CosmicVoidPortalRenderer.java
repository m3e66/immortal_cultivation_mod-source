package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.CosmicVoidPortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class CosmicVoidPortalRenderer extends EntityRenderer<CosmicVoidPortalEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/break_void_portal.png");

   public CosmicVoidPortalRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(CosmicVoidPortalEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      float age = (float)entity.tickCount + partialTick;
      float remaining = 200.0F - age;
      float appear = Mth.clamp(age / 7.0F, 0.0F, 1.0F);
      float fade = Mth.clamp(remaining / 14.0F, 0.0F, 1.0F);
      float pulse = 1.0F + Mth.sin(age * 0.13F) * 0.045F;
      float alpha = appear * fade;
      poseStack.pushPose();
      poseStack.translate((double)0.0F, (double)2.25F, (double)0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.045F) * 1.25F));
      renderLayer(poseStack, buffers, 4.85F * pulse, 0.03F, 0.48F, 0.16F, 1.0F, alpha * 0.22F);
      renderLayer(poseStack, buffers, 4.45F * pulse, 0.01F, 0.72F, 0.42F, 1.0F, alpha * 0.52F);
      renderLayer(poseStack, buffers, 4.1F * pulse, -0.01F, 1.0F, 1.0F, 1.0F, alpha);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffers, 15728880);
   }

   private static void renderLayer(PoseStack poseStack, MultiBufferSource buffers, float scale, float depth, float red, float green, float blue, float alpha) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, depth);
      poseStack.scale(scale, scale, scale);
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
      vertex(vertices, pose, -0.5F, -0.5F, 0.0F, 1.0F, red, green, blue, alpha, 1.0F);
      vertex(vertices, pose, 0.5F, -0.5F, 1.0F, 1.0F, red, green, blue, alpha, 1.0F);
      vertex(vertices, pose, 0.5F, 0.5F, 1.0F, 0.0F, red, green, blue, alpha, 1.0F);
      vertex(vertices, pose, -0.5F, 0.5F, 0.0F, 0.0F, red, green, blue, alpha, 1.0F);
      vertex(vertices, pose, -0.5F, 0.5F, 0.0F, 0.0F, red, green, blue, alpha, -1.0F);
      vertex(vertices, pose, 0.5F, 0.5F, 1.0F, 0.0F, red, green, blue, alpha, -1.0F);
      vertex(vertices, pose, 0.5F, -0.5F, 1.0F, 1.0F, red, green, blue, alpha, -1.0F);
      vertex(vertices, pose, -0.5F, -0.5F, 0.0F, 1.0F, red, green, blue, alpha, -1.0F);
      poseStack.popPose();
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float u, float v, float red, float green, float blue, float alpha, float normalZ) {
      vertices.addVertex(pose, x, y, 0.0F).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 0.0F, normalZ);
   }

   public ResourceLocation getTextureLocation(CosmicVoidPortalEntity entity) {
      return TEXTURE;
   }
}
