package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.CrimsonScaleFlameLizardModel;
import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CrimsonScaleFlameLizardRenderer extends GeoEntityRenderer<CrimsonScaleFlameLizardEntity> {
   public CrimsonScaleFlameLizardRenderer(EntityRendererProvider.Context context) {
      super(context, new CrimsonScaleFlameLizardModel());
      this.withScale(1.18F);
      this.shadowRadius = 1.05F;
   }

   protected void applyRotations(CrimsonScaleFlameLizardEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
      super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
      poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
   }
}
