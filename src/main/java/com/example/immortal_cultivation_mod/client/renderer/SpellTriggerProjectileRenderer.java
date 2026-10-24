package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SpellTriggerProjectileRenderer extends EntityRenderer<SpellTriggerProjectileEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public SpellTriggerProjectileRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(SpellTriggerProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      // The travelling carrier is invisible; activated solar impact animation remains visible.
      if (!entity.activated()) return;
      String spellId = entity.spellId();
      if ("leixiao_guanyun".equals(spellId)) {
         renderLeixiao(entity, partialTick, poseStack, buffer);
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      } else {
         boolean fallingSun = "dari_zhen".equals(spellId);
         boolean sunWheel = "rilun".equals(spellId);
         if (!fallingSun && !sunWheel) {
            if (!entity.activated()) TechniqueBoltRenderer.draw(entity,spellId,partialTick,poseStack,buffer);
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
         } else if (fallingSun && entity.activated() && entity.solarImpactTicks() > 4) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
         } else {
            float age = (float)entity.tickCount + partialTick;
            float pulse = 0.96F + 0.06F * Mth.sin(age * 0.65F);
            float scale = (fallingSun ? 0.74F : 0.62F) * (entity.chargeScale() >= 1.99F ? 1.22F : 1.0F) * pulse * com.example.immortal_cultivation_mod.client.SpellProjectileVisuals.SCALE;
            if (fallingSun && entity.activated()) {
               float collapse = 1.0F - Mth.clamp(((float)entity.solarImpactTicks() + partialTick) / 4.0F, 0.0F, 1.0F);
               scale *= 0.12F + collapse * 0.88F;
            }

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 7.5F));
            poseStack.mulPose(Axis.XP.rotationDegrees(18.0F + age * 2.1F));
            poseStack.scale(scale, scale, scale);
            renderModel(ModItems.SPELL_MODEL_SOLAR_ORB.toStack(), entity, poseStack, buffer);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F + age * 5.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-age * 9.0F));
            poseStack.scale(scale * 0.82F, scale * 0.82F, scale * 0.82F);
            renderModel(ModItems.SPELL_MODEL_SOLAR_HALO.toStack(), entity, poseStack, buffer);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
         }
      }
   }

   private static void renderLeixiao(SpellTriggerProjectileEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
      Vec3 facing = entity.getDeltaMovement();
      if (!(facing.lengthSqr() < 1.0E-6)) {
         double horizontal = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
         float yaw = (float)(Mth.atan2(-facing.x, facing.z) * (180D / Math.PI));
         float pitch = (float)(Mth.atan2(facing.y, horizontal) * (180D / Math.PI));
         float age = (float)entity.tickCount + partialTick;
         float momentumGlow = Mth.clamp(entity.momentumSnapshot() / 100.0F, 0.0F, 1.0F);
         float pulse = 0.88F + 0.12F * Mth.sin(age * 2.4F);
         poseStack.pushPose();
         com.example.immortal_cultivation_mod.client.SpellProjectileVisuals.enlarge(poseStack);
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
         renderCorridor(poseStack, buffer, 0.34F, -1.1F, 4.8F, 0.12F, 0.92F, 1.0F, (0.2F + momentumGlow * 0.12F) * pulse, 24);
         renderBolt(poseStack, buffer, -1.25F, 5.15F, 0.038F + momentumGlow * 0.015F, 0.82F, 0.96F, 1.0F, 0.98F);
         renderBolt(poseStack, buffer, -0.9F, 4.7F, 0.095F + momentumGlow * 0.025F, 0.18F, 0.7F, 1.0F, 0.44F);

         for(int ring = 0; ring < 5; ++ring) {
            float z = 0.35F + (float)ring * 0.86F;
            float radius = 0.43F + 0.035F * Mth.sin(age * 1.7F + (float)ring);
            renderAxisRing(poseStack, buffer, z, radius - 0.025F, radius + 0.025F, 0.16F, 0.94F, 1.0F, 0.48F - (float)ring * 0.045F, 28, age * (5.0F - (float)ring * 0.45F));
         }

         renderLightningBranches(poseStack, buffer, age, momentumGlow);
         poseStack.popPose();
      }
   }

   private static void renderCorridor(PoseStack poseStack, MultiBufferSource buffer, float radius, float frontZ, float backZ, float red, float green, float blue, float alpha, int segments) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));

      for(int i = 0; i < segments; ++i) {
         float a0 = ((float)Math.PI * 2F) * (float)i / (float)segments;
         float a1 = ((float)Math.PI * 2F) * (float)(i + 1) / (float)segments;
         float x0 = Mth.cos(a0) * radius;
         float y0 = Mth.sin(a0) * radius;
         float x1 = Mth.cos(a1) * radius;
         float y1 = Mth.sin(a1) * radius;
         glowVertex(vertices, pose, x0, y0, frontZ, 0.0F, 0.0F, red, green, blue, alpha * 0.68F);
         glowVertex(vertices, pose, x1, y1, frontZ, 1.0F, 0.0F, red, green, blue, alpha * 0.68F);
         glowVertex(vertices, pose, x1, y1, backZ, 1.0F, 1.0F, red, green, blue, 0.0F);
         glowVertex(vertices, pose, x0, y0, backZ, 0.0F, 1.0F, red, green, blue, 0.0F);
      }

   }

   private static void renderBolt(PoseStack poseStack, MultiBufferSource buffer, float frontZ, float backZ, float halfWidth, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      glowQuad(vertices, pose, -halfWidth, 0.0F, frontZ, halfWidth, 0.0F, frontZ, halfWidth, 0.0F, backZ, -halfWidth, 0.0F, backZ, red, green, blue, alpha);
      glowQuad(vertices, pose, 0.0F, -halfWidth, frontZ, 0.0F, halfWidth, frontZ, 0.0F, halfWidth, backZ, 0.0F, -halfWidth, backZ, red, green, blue, alpha);
   }

   private static void renderLightningBranches(PoseStack poseStack, MultiBufferSource buffer, float age, float momentum) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      int branches = momentum >= 0.75F ? 7 : 4;

      for(int branch = 0; branch < branches; ++branch) {
         float angle = (float)branch * 2.39996F + age * 0.19F;
         float z = -0.45F + (float)branch * 0.68F;
         float reach = 0.18F + (float)(branch % 3) * 0.09F + momentum * 0.12F;
         float x = Mth.cos(angle) * reach;
         float y = Mth.sin(angle) * reach;
         float width = 0.018F;
         glowQuad(vertices, pose, -width, 0.0F, z, width, 0.0F, z, x + width, y, z + 0.28F, x - width, y, z + 0.28F, 0.62F, 0.92F, 1.0F, 0.78F);
      }

   }

   private static void renderAxisRing(PoseStack poseStack, MultiBufferSource buffer, float z, float innerRadius, float outerRadius, float red, float green, float blue, float alpha, int segments, float rotationDegrees) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float rotation = rotationDegrees * ((float)Math.PI / 180F);

      for(int i = 0; i < segments; ++i) {
         float a0 = rotation + ((float)Math.PI * 2F) * (float)i / (float)segments;
         float a1 = rotation + ((float)Math.PI * 2F) * (float)(i + 1) / (float)segments;
         glowVertex(vertices, pose, Mth.cos(a0) * innerRadius, Mth.sin(a0) * innerRadius, z, 0.0F, 1.0F, red, green, blue, alpha);
         glowVertex(vertices, pose, Mth.cos(a1) * innerRadius, Mth.sin(a1) * innerRadius, z, 1.0F, 1.0F, red, green, blue, alpha);
         glowVertex(vertices, pose, Mth.cos(a1) * outerRadius, Mth.sin(a1) * outerRadius, z, 1.0F, 0.0F, red, green, blue, alpha * 0.62F);
         glowVertex(vertices, pose, Mth.cos(a0) * outerRadius, Mth.sin(a0) * outerRadius, z, 0.0F, 0.0F, red, green, blue, alpha * 0.62F);
      }

   }

   private static void glowQuad(VertexConsumer vertices, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
      glowVertex(vertices, pose, x0, y0, z0, 0.0F, 0.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, x1, y1, z1, 1.0F, 0.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, x2, y2, z2, 1.0F, 1.0F, red, green, blue, 0.0F);
      glowVertex(vertices, pose, x3, y3, z3, 0.0F, 1.0F, red, green, blue, 0.0F);
   }

   private static void glowVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   private static void renderModel(ItemStack stack, SpellTriggerProjectileEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
      Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
   }

   public ResourceLocation getTextureLocation(SpellTriggerProjectileEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
