package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.TalismanDrawingTableBlock;
import com.example.immortal_cultivation_mod.block.TalismanDrawingTableBlockEntity;
import com.example.immortal_cultivation_mod.client.model.TalismanDrawingTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class TalismanDrawingTableRenderer extends GeoBlockRenderer<TalismanDrawingTableBlockEntity> {
   public TalismanDrawingTableRenderer(BlockEntityRendererProvider.Context context) {
      super(new TalismanDrawingTableModel());
   }

   public void render(TalismanDrawingTableBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      Direction facing = (Direction)animatable.getBlockState().getValue(TalismanDrawingTableBlock.FACING);
      poseStack.pushPose();
      poseStack.translate((double)0.5F, (double)0.0F, (double)0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(facing)));
      poseStack.translate((double)0.0F, (double)0.0F, (double)-1.0F);
      super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      poseStack.popPose();
   }

   protected Direction getFacing(TalismanDrawingTableBlockEntity blockEntity) {
      return Direction.NORTH;
   }

   private static float rotationDegrees(Direction facing) {
      float var10000;
      switch (facing) {
         case EAST -> var10000 = 270.0F;
         case SOUTH -> var10000 = 180.0F;
         case WEST -> var10000 = 90.0F;
         default -> var10000 = 0.0F;
      }

      return var10000;
   }

   public AABB getRenderBoundingBox(TalismanDrawingTableBlockEntity blockEntity) {
      return (new AABB(blockEntity.getBlockPos())).inflate((double)0.25F);
   }
}
