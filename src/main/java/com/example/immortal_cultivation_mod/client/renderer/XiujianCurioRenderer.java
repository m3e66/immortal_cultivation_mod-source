package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.XiujianItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class XiujianCurioRenderer implements ICurioRenderer {
   private final XiujianItemRenderer renderer = new XiujianItemRenderer();

   public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer, int packedLight, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
      LivingEntity entity = slotContext.entity();
      EntityModel var15 = renderLayerParent.getModel();
      if (var15 instanceof HumanoidModel<?> humanoidModel) {
         poseStack.pushPose();
         ICurioRenderer.translateIfSneaking(poseStack, entity);
         XiujianArmRenderHelper.render(this.renderer, stack, humanoidModel, HumanoidArm.RIGHT, poseStack, buffer, packedLight);
         poseStack.popPose();
      }
   }
}
