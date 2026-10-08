package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.WantengVineModel;
import com.example.immortal_cultivation_mod.entity.WantengVineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class WantengVineRenderer extends GeoEntityRenderer<WantengVineEntity> {
   private static final float[] SIZE_VARIATION = new float[]{1.05F, 0.94F, 1.1F, 0.98F, 1.03F, 0.91F};

   public WantengVineRenderer(EntityRendererProvider.Context context) {
      super(context, new WantengVineModel());
      this.shadowRadius = 0.0F;
   }

   public void render(WantengVineEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float age = (float)entity.tickCount + partialTick;
      float growth = Mth.clamp(age / 8.0F, 0.03F, 1.0F);
      float size = SIZE_VARIATION[entity.vineIndex()];
      poseStack.pushPose();
      poseStack.scale(size, growth * size, size);
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
