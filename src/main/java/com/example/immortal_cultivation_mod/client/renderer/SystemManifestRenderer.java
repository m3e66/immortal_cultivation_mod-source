package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.entity.SystemManifestEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public class SystemManifestRenderer extends EntityRenderer<SystemManifestEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/gui/system/manifest_window.png");
   private static final float WIDTH=2.3F, HEIGHT=WIDTH*366F/509F;

   public SystemManifestRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   public void render(SystemManifestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (CultivationLevels.getRealmIndex(ClientData.cultivationData.cultivationLevel()) >= 5) {
         poseStack.pushPose();
         poseStack.translate((double)0.0F, 0.9, (double)0.0F);
         poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.scale(WIDTH, HEIGHT, 1.0F);
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
         // Standalone window texture, with no unrelated GUI artwork.
         quad(vertices,pose,-.5F,-.5F,.5F,.5F,0,0,509,366,0);
         // Replace the baked English title without altering the frame or its controls.
         quad(vertices,pose,.5F-349F/509F,.5F-63F/366F,.5F-14F/509F,.5F-9F/366F,260,9,340,63,-.002F);
         poseStack.popPose();
         if(buffer instanceof MultiBufferSource.BufferSource source)source.endBatch(RenderType.entityTranslucent(TEXTURE));
         drawLabels(entity,poseStack,buffer);
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      }
   }

   private static void drawLabels(SystemManifestEntity entity,PoseStack poses,MultiBufferSource buffer){
      var mc=Minecraft.getInstance();poses.pushPose();poses.translate(0,.9,.0);poses.mulPose(mc.gameRenderer.getMainCamera().rotation());
      poses.translate(0,0,.008);poses.scale(.007F,-.007F,.007F);
      draw(mc.font,buffer,poses,Component.translatable(entity.systemType().translationKey()),-145,-108,0xFFF7FAFF,205);
      draw(mc.font,buffer,poses,Component.translatable("screen.immortal_cultivation_mod.system.manifest.linked"),-132,-53,0xFF276448,265);
      String owner=entity.ownerId().map(id->{var player=entity.level().getPlayerByUUID(id);return player==null?id.toString().substring(0,8):player.getName().getString();}).orElse("");
      draw(mc.font,buffer,poses,Component.translatable("screen.immortal_cultivation_mod.system.manifest.owner",owner),-132,-30,0xFF304961,265);
      poses.popPose();
   }
   private static void draw(Font font,MultiBufferSource buffer,PoseStack poses,Component text,int x,int y,int color,int width){
      font.drawInBatch(font.plainSubstrByWidth(text.getString(),width),x,y,color,false,poses.last().pose(),buffer,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);
   }
   private static void quad(VertexConsumer vertices,Matrix4f pose,float left,float bottom,float right,float top,float u0,float v0,float u1,float v1,float z){
      vertex(vertices,pose,left,bottom,z,u1/509F,v1/366F);vertex(vertices,pose,right,bottom,z,u0/509F,v1/366F);
      vertex(vertices,pose,right,top,z,u0/509F,v0/366F);vertex(vertices,pose,left,top,z,u1/509F,v0/366F);
   }
   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y,float z, float u, float v) {
      vertices.addVertex(pose, x, y, z).setColor(255, 255, 255, 242).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0,0,1);
   }

   public ResourceLocation getTextureLocation(SystemManifestEntity entity) {
      return TEXTURE;
   }
}
