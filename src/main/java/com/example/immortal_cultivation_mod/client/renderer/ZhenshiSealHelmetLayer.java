package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.joml.Matrix4f;

public class ZhenshiSealHelmetLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
   private static final ResourceLocation ZHENSHI_TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/talisman.png");
   private static final ResourceLocation FENGYAN_TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/talisman.png");

   public ZhenshiSealHelmetLayer(RenderLayerParent<T, M> renderer) {
      super(renderer);
   }

   public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      if (entity.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.ZHENSHI_SEAL_HELMET.get())) {
         poseStack.pushPose();
         ((HumanoidModel)this.getParentModel()).head.translateAndRotate(poseStack);
         poseStack.translate((double)0.0F, (double)0.0F, -0.34);
         float width = 0.2F;
         float height = 0.32F;
         Matrix4f matrix = poseStack.last().pose();
         ResourceLocation texture = UndeadControl.isFengyanSealed(entity) ? FENGYAN_TEXTURE : ZHENSHI_TEXTURE;
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
         addVertex(vertices, matrix, -width, -height, 0.0F, 0.0F, 0.0F, packedLight);
         addVertex(vertices, matrix, width, -height, 0.0F, 1.0F, 0.0F, packedLight);
         addVertex(vertices, matrix, width, height, 0.0F, 1.0F, 1.0F, packedLight);
         addVertex(vertices, matrix, -width, height, 0.0F, 0.0F, 1.0F, packedLight);
         poseStack.popPose();
      }
   }

   private static void addVertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z, float u, float v, int packedLight) {
      vertices.addVertex(matrix, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 0.0F, -1.0F);
   }
}
