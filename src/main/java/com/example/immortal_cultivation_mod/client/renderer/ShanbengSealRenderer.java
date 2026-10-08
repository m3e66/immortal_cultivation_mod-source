package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.ShanbengSealModel;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.ShanbengSealEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class ShanbengSealRenderer extends GeoEntityRenderer<ShanbengSealEntity> {
   public ShanbengSealRenderer(EntityRendererProvider.Context context) {
      super(context, new ShanbengSealModel());
      this.shadowRadius = 0.0F;
   }

   public void render(ShanbengSealEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Entity parent;
      boolean var10000;
      label32: {
         parent = entity.parentEntity();
         if (parent instanceof ElementalTechniqueAreaEntity area) {
            if (area.fullyCharged()) {
               var10000 = true;
               break label32;
            }
         }

         var10000 = false;
      }

      boolean fullyCharged = var10000;
      float landingStart = fullyCharged ? 30.0F : 22.0F;
      float landingTick = fullyCharged ? 40.0F : 30.0F;
      float age = (float)(parent instanceof ElementalTechniqueAreaEntity ? parent.tickCount : entity.tickCount) + partialTick;
      float height;
      if (age < landingStart) {
         height = 9.7F + Mth.sin(age * 0.18F) * 0.16F;
      } else {
         float progress = Mth.clamp((age - landingStart) / (landingTick - landingStart), 0.0F, 1.0F);
         float eased = progress * progress * (3.0F - 2.0F * progress);
         height = Mth.lerp(eased, 9.7F, 0.0F);
      }

      float impactPulse = Mth.clamp(1.0F - Math.abs(age - (landingTick + 1.0F)) / 2.0F, 0.0F, 1.0F);
      float horizontalScale = 1.0F + impactPulse * 0.07F;
      float verticalScale = 1.0F - impactPulse * 0.13F;
      poseStack.pushPose();
      poseStack.translate((double)0.0F, (double)height, (double)0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(Math.min(age, landingTick) * 3.5F));
      poseStack.scale(horizontalScale, verticalScale, horizontalScale);
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
