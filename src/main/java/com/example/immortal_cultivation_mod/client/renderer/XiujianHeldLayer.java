package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.XiujianItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class XiujianHeldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private final XiujianItemRenderer renderer = new XiujianItemRenderer();

   public XiujianHeldLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      ItemStack stack = player.getMainHandItem();
      if (stack.is((Item)ModItems.XIUJIAN.get())) {
         poseStack.pushPose();
         XiujianArmRenderHelper.render(this.renderer, stack, (HumanoidModel)this.getParentModel(), player.getMainArm(), poseStack, buffer, packedLight);
         poseStack.popPose();
      }
   }
}
