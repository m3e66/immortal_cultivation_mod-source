package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.MedicineCabinetBlock;
import com.example.immortal_cultivation_mod.block.MedicineCabinetBlockEntity;
import com.example.immortal_cultivation_mod.client.model.MedicineCabinetModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class MedicineCabinetRenderer extends GeoBlockRenderer<MedicineCabinetBlockEntity> {
   public MedicineCabinetRenderer(BlockEntityRendererProvider.Context context) {
      super(new MedicineCabinetModel());
   }

   public void render(MedicineCabinetBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      Direction facing = (Direction)animatable.getBlockState().getValue(MedicineCabinetBlock.FACING);
      poseStack.pushPose();
      poseStack.translate((double)0.5F, (double)0.0F, (double)0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(facing)));
      poseStack.translate((double)0.0F, (double)0.0F, (double)-1.0F);
      super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      poseStack.popPose();
   }

   protected Direction getFacing(MedicineCabinetBlockEntity blockEntity) {
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

   public AABB getRenderBoundingBox(MedicineCabinetBlockEntity blockEntity) {
      MedicineCabinetBlock.Variant variant = blockEntity.variant();
      BlockPos controller = blockEntity.getBlockPos();
      Direction facing = (Direction)blockEntity.getBlockState().getValue(MedicineCabinetBlock.FACING);
      BlockPos farCorner = controller.relative(facing.getCounterClockWise(), variant.blockWidth() - 1).above(variant.blockHeight() - 1);
      return (new AABB((double)Math.min(controller.getX(), farCorner.getX()), (double)controller.getY(), (double)Math.min(controller.getZ(), farCorner.getZ()), (double)Math.max(controller.getX(), farCorner.getX()) + (double)1.0F, (double)farCorner.getY() + (double)1.0F, (double)Math.max(controller.getZ(), farCorner.getZ()) + (double)1.0F)).inflate((double)0.75F);
   }
}
