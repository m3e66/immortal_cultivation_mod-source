package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SpiritualLightEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SpiritualLightRenderer extends EntityRenderer<SpiritualLightEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public SpiritualLightRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(SpiritualLightEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float age = (float)entity.tickCount + partialTick;
      float pulse = 1.0F + Mth.sin(age * 0.11F) * 0.055F;
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      sphere(vertices, pose, 0.58F * pulse, 1.0F, 0.91F, 0.66F, 0.045F, Vec3.ZERO);
      sphere(vertices, pose, 0.34F * pulse, 1.0F, 0.95F, 0.78F, 0.13F, Vec3.ZERO);
      sphere(vertices, pose, 0.145F * pulse, 1.0F, 0.98F, 0.88F, 0.48F, Vec3.ZERO);
      sphere(vertices, pose, 0.072F * pulse, 1.0F, 1.0F, 0.96F, 0.96F, Vec3.ZERO);

      for(int mote = 0; mote < 3; ++mote) {
         double angle = (double)age * (0.022 + (double)mote * 0.004) + (double)((float)mote * ((float)Math.PI * 2F)) / (double)3.0F;
         double radius = 0.23 + (double)mote * 0.035;
         Vec3 offset = new Vec3(Math.cos(angle) * radius, Math.sin(angle * 1.7 + (double)mote) * 0.095, Math.sin(angle) * radius);
         sphere(vertices, pose, 0.018F, 1.0F, 0.96F, 0.78F, 0.72F, offset);
      }

      super.render(entity, entityYaw, partialTick, poseStack, buffer, 15728880);
   }

   private static void sphere(VertexConsumer vertices, Matrix4f pose, float radius, float red, float green, float blue, float alpha, Vec3 offset) {
      int longitudes = 16;
      int latitudes = 8;

      for(int latitude = 0; latitude < latitudes; ++latitude) {
         float lat0 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)latitude / (float)latitudes;
         float lat1 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)(latitude + 1) / (float)latitudes;

         for(int longitude = 0; longitude < longitudes; ++longitude) {
            float lon0 = ((float)Math.PI * 2F) * (float)longitude / (float)longitudes;
            float lon1 = ((float)Math.PI * 2F) * (float)(longitude + 1) / (float)longitudes;
            sphereVertex(vertices, pose, radius, lat0, lon0, red, green, blue, alpha, offset);
            sphereVertex(vertices, pose, radius, lat1, lon0, red, green, blue, alpha, offset);
            sphereVertex(vertices, pose, radius, lat1, lon1, red, green, blue, alpha, offset);
            sphereVertex(vertices, pose, radius, lat0, lon1, red, green, blue, alpha, offset);
         }
      }

   }

   private static void sphereVertex(VertexConsumer vertices, Matrix4f pose, float radius, float latitude, float longitude, float red, float green, float blue, float alpha, Vec3 offset) {
      float cosLatitude = Mth.cos(latitude);
      Vec3 point = offset.add((double)(cosLatitude * Mth.cos(longitude) * radius), (double)(Mth.sin(latitude) * radius), (double)(cosLatitude * Mth.sin(longitude) * radius));
      vertices.addVertex(pose, (float)point.x, (float)point.y, (float)point.z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(SpiritualLightEntity entity) {
      return WHITE_TEXTURE;
   }
}
