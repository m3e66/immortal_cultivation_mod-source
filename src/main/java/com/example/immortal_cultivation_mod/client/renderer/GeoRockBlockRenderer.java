package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.DunwuSteleBlock;
import com.example.immortal_cultivation_mod.block.GeoRockBlockEntity;
import com.example.immortal_cultivation_mod.client.model.GeoRockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GeoRockBlockRenderer extends GeoBlockRenderer<GeoRockBlockEntity> {
    public GeoRockBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new GeoRockModel());
    }

    @Override
    public void render(GeoRockBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!"dunwu_stele".equals(animatable.modelName())) {
            super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(
                animatable.getBlockState().getValue(DunwuSteleBlock.FACING))));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.translate(0.0D, 0.0D, -1.0D);
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    protected Direction getFacing(GeoRockBlockEntity blockEntity) {
        return "dunwu_stele".equals(blockEntity.modelName())
                ? Direction.NORTH
                : super.getFacing(blockEntity);
    }

    private static float rotationDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
