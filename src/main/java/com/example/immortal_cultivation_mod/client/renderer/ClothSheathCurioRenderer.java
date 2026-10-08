package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.ClothSheathItem;
import com.example.immortal_cultivation_mod.item.ClothSheathThinArmorRenderer;
import com.example.immortal_cultivation_mod.item.ClothSheathWideArmorRenderer;
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
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class ClothSheathCurioRenderer implements ICurioRenderer {
   private final ClothSheathThinArmorRenderer thinRenderer = new ClothSheathThinArmorRenderer();
   private final ClothSheathWideArmorRenderer wideRenderer = new ClothSheathWideArmorRenderer();

   public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer, int packedLight, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
      LivingEntity entity = slotContext.entity();
      ItemStack weapon = ClothSheathItem.getStoredWeapon(stack, entity.registryAccess());
      if (!weapon.isEmpty()) {
         EntityModel var16 = renderLayerParent.getModel();
         if (var16 instanceof HumanoidModel) {
            HumanoidModel<?> humanoidModel = (HumanoidModel)var16;
            GeoArmorRenderer<ClothSheathItem> renderer = (GeoArmorRenderer<ClothSheathItem>)(ClothSheathItem.isWideWeapon(weapon) ? this.wideRenderer : this.thinRenderer);
            poseStack.pushPose();
            ICurioRenderer.translateIfSneaking(poseStack, entity);
            ICurioRenderer.followBodyRotations(entity, new HumanoidModel[]{renderer});
            renderer.prepForRender(entity, stack, EquipmentSlot.CHEST, humanoidModel, buffer, partialTicks, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
            renderer.renderToBuffer(poseStack, (VertexConsumer)null, packedLight, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
            return;
         }
      }

   }
}
