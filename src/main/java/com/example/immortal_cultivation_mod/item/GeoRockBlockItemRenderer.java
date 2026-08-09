package com.example.immortal_cultivation_mod.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GeoRockBlockItemRenderer extends GeoItemRenderer<GeoRockBlockItem> {
    public GeoRockBlockItemRenderer() {
        super(new GeoRockBlockItemModel());
    }

    @Override
    public void preRender(PoseStack poseStack, GeoRockBlockItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        if (!isReRender
                && renderPerspective == ItemDisplayContext.GUI
                && "dunwu_stele".equals(animatable.modelName())) {
            poseStack.translate(0.5D, -0.3D, -0.5D);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
