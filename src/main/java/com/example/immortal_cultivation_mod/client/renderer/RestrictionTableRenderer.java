package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity;
import com.example.immortal_cultivation_mod.client.model.RestrictionTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class RestrictionTableRenderer extends GeoBlockRenderer<RestrictionTableBlockEntity> {
   public RestrictionTableRenderer(BlockEntityRendererProvider.Context context) {
      super(new RestrictionTableModel());
   }

   public void render(RestrictionTableBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      poseStack.pushPose();
      poseStack.translate((double)0.5F, (double)0.0F, (double)-0.5F);
      super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      poseStack.popPose();
   }

   public AABB getRenderBoundingBox(RestrictionTableBlockEntity blockEntity) {
      return (new AABB(blockEntity.getBlockPos())).inflate(0.15);
   }
}
