package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ElementalTechniqueProjectileRenderer extends EntityRenderer<ElementalTechniqueProjectileEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public ElementalTechniqueProjectileRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   public void render(ElementalTechniqueProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      try (var support = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.support(com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null)) {
      poseStack.pushPose();
      Vec3 facing = entity.getDeltaMovement();
      if (entity.kind() == ElementalTechniqueProjectileEntity.Kind.WANREN_CHUANXIN && entity.launchDelay() > 0 && entity.getOwner() != null) {
         facing = entity.getOwner().getLookAngle();
      }

      if (facing.lengthSqr() > 1.0E-5) {
         double horizontal = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
         float yaw = (float)(Mth.atan2(-facing.x, facing.z) * (180D / Math.PI));
         float pitch = (float)(Mth.atan2(facing.y, horizontal) * (180D / Math.PI));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      }

      renderAfterimage(entity, partialTick, poseStack, buffer);
      if (entity.kind() == ElementalTechniqueProjectileEntity.Kind.WANREN_CHUANXIN) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(((float)entity.tickCount + partialTick) * 18.0F));
      }

      float var10000;
      switch (entity.kind()) {
         case JINFENG_ZHI -> var10000 = 0.52F;
         case WANREN_CHUANXIN -> var10000 = 1.35F;
         case YANBAO_DAN -> var10000 = 1.2F;
         case SHUIREN_LIANZHAN -> var10000 = 1.25F;
         case BINGZHUI_YU -> var10000 = 1.35F;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      float scale = var10000 * (com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null ? .38F : .72F);
      poseStack.scale(scale, scale, scale);
      Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItem(), ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }
   }

   private static void renderAfterimage(ElementalTechniqueProjectileEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
      if (!(entity.getDeltaMovement().lengthSqr() < 1.0E-5) && entity.launchDelay() <= 0) {
         float pulse = 0.9F + 0.1F * Mth.sin(((float)entity.tickCount + partialTick) * 0.75F);
         switch (entity.kind()) {
            case JINFENG_ZHI:
               renderRibbon(poseStack, buffer, 1.05F, 0.012F, 1.0F, 0.86F, 0.28F, 0.92F);
               renderRibbon(poseStack, buffer, 0.62F, 0.025F, 1.0F, 0.98F, 0.78F, 0.42F);
               break;
            case WANREN_CHUANXIN:
               renderRibbon(poseStack, buffer, 2.15F, 0.05F, 1.0F, 0.63F, 0.08F, 0.64F);
               renderRibbon(poseStack, buffer, 1.35F, 0.085F, 1.0F, 0.93F, 0.55F, 0.24F);
               break;
            case YANBAO_DAN:
               renderRibbon(poseStack, buffer, 0.95F, 0.12F, 1.0F, 0.16F, 0.015F, 0.55F);
               renderOrbGlow(poseStack, buffer, 0.36F * pulse, 1.0F, 0.16F, 0.02F, 0.58F);
               renderOrbGlow(poseStack, buffer, 0.18F * pulse, 1.0F, 0.92F, 0.62F, 0.92F);
               break;
            case SHUIREN_LIANZHAN:
               renderRibbon(poseStack, buffer, 1.1F, 0.055F, 0.1F, 0.62F, 1.0F, 0.48F);
               renderRibbon(poseStack, buffer, 0.66F, 0.085F, 0.78F, 0.96F, 1.0F, 0.36F);
               break;
            case BINGZHUI_YU:
               renderRibbon(poseStack, buffer, 0.88F, 0.05F, 0.56F, 0.9F, 1.0F, 0.48F);
         }

      } else {
         if (entity.kind() == ElementalTechniqueProjectileEntity.Kind.YANBAO_DAN) {
            renderOrbGlow(poseStack, buffer, 0.34F, 1.0F, 0.18F, 0.025F, 0.48F);
         }

      }
   }

   private static void renderRibbon(PoseStack poseStack, MultiBufferSource buffer, float length, float halfWidth, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      ribbonFace(vertices, pose, -halfWidth, 0.0F, 0.0F, halfWidth, 0.0F, 0.0F, halfWidth * 0.12F, 0.0F, -length, -halfWidth * 0.12F, 0.0F, -length, red, green, blue, alpha);
      ribbonFace(vertices, pose, 0.0F, -halfWidth, 0.0F, 0.0F, halfWidth, 0.0F, 0.0F, halfWidth * 0.12F, -length, 0.0F, -halfWidth * 0.12F, -length, red, green, blue, alpha);
   }

   private static void renderOrbGlow(PoseStack poseStack, MultiBufferSource buffer, float radius, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      ribbonFace(vertices, pose, -radius, -radius, 0.0F, radius, -radius, 0.0F, radius, radius, 0.0F, -radius, radius, 0.0F, red, green, blue, alpha);
      ribbonFace(vertices, pose, -radius, 0.0F, -radius, radius, 0.0F, -radius, radius, 0.0F, radius, -radius, 0.0F, radius, red, green, blue, alpha * 0.82F);
      ribbonFace(vertices, pose, 0.0F, -radius, -radius, 0.0F, radius, -radius, 0.0F, radius, radius, 0.0F, -radius, radius, red, green, blue, alpha * 0.82F);
   }

   private static void ribbonFace(VertexConsumer vertices, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
      glowVertex(vertices, pose, x0, y0, z0, 0.0F, 0.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, x1, y1, z1, 1.0F, 0.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, x2, y2, z2, 1.0F, 1.0F, red, green, blue, 0.0F);
      glowVertex(vertices, pose, x3, y3, z3, 0.0F, 1.0F, red, green, blue, 0.0F);
   }

   private static void glowVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(ElementalTechniqueProjectileEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
