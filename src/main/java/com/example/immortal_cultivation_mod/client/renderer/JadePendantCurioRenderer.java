package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.JadePendantArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class JadePendantCurioRenderer implements ICurioRenderer {
   private final JadePendantArmorRenderer armorRenderer = new JadePendantArmorRenderer();

   public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer, int packedLight, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
      LivingEntity entity = slotContext.entity();
      EntityModel var15 = renderLayerParent.getModel();
      if (var15 instanceof HumanoidModel<?> humanoidModel) {
         poseStack.pushPose();
         ICurioRenderer.followBodyRotations(entity, new HumanoidModel[]{this.armorRenderer});
         this.armorRenderer.prepForRender(entity, stack, EquipmentSlot.CHEST, humanoidModel, buffer, partialTicks, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
         this.armorRenderer.renderToBuffer(poseStack, (VertexConsumer)null, packedLight, OverlayTexture.NO_OVERLAY, -1);
         poseStack.popPose();
      }

   }
}
