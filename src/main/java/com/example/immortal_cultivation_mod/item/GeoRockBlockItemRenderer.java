package com.example.immortal_cultivation_mod.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;

public class GeoRockBlockItemRenderer extends GeoItemRenderer<GeoRockBlockItem> {
    private static final double ISOMETRIC_X_FACTOR = Math.sqrt(0.5D);
    private static final double ISOMETRIC_Y_DEPTH_FACTOR = Math.sqrt(0.125D);
    private static final double ISOMETRIC_Y_HEIGHT_FACTOR = Math.sqrt(0.75D);
    private static final double VANILLA_BLOCK_PROJECTED_WIDTH =
            ISOMETRIC_X_FACTOR * 2.0D;
    private static final double VANILLA_BLOCK_PROJECTED_HEIGHT =
            ISOMETRIC_Y_DEPTH_FACTOR * 2.0D
                    + ISOMETRIC_Y_HEIGHT_FACTOR;

    private final Map<String, GuiBounds> guiBounds = new HashMap<>();

    public GeoRockBlockItemRenderer() {
        super(new GeoRockBlockItemModel());
    }

    @Override
    public void setupLightingForGuiRender() {
        if (animatable != null
                && usesSpecialIsometricView(animatable.modelName())) {
            Lighting.setupForEntityInInventory();
        } else {
            Lighting.setupForFlatItems();
        }
    }

    @Override
    public void preRender(PoseStack poseStack, GeoRockBlockItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        if (!isReRender
                && renderPerspective == ItemDisplayContext.GUI
                && usesSpecialIsometricView(animatable.modelName())) {
            GuiBounds bounds = guiBounds.computeIfAbsent(
                    animatable.modelName(), ignored -> calculateBounds(model));
            if (bounds.valid()) {
                float fit = (float) bounds.isometricFit();
                // Fit around the same 0.5/0.5/0.5 pivot used by vanilla
                // block-item models, then offset GeckoLib's baked geometry so
                // both -8..8 and 0..16 authored models share that pivot.
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.scale(fit, fit, fit);
                poseStack.translate(-0.5D, -0.5D, -0.5D);
                poseStack.translate(
                        -bounds.centerX(),
                        -bounds.centerY() - 0.01D,
                        -bounds.centerZ());
            }
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }

    private static boolean usesSpecialIsometricView(String modelName) {
        if (modelName.endsWith("_spell_scroll_block")) {
            return true;
        }
        return switch (modelName) {
            case "qiannian_lingmu_heart", "yunwen_silver_crystal",
                    "jingtie_motherstone", "chitong_sui", "hanpo_jade",
                    "bisui_stone", "star_dust_stone", "moxuan_iron",
                    "cu_tie_xiaoding", "qingtong_yinhuo_ding", "chi_jin_sanyang_ding",
                    "talisman_drawing_table", "restriction_table",
                    "dunwu_stele" -> true;
            default -> false;
        };
    }

    private static GuiBounds calculateBounds(BakedGeoModel model) {
        GuiBounds bounds = new GuiBounds();
        PoseStack modelPose = new PoseStack();
        for (GeoBone bone : model.topLevelBones()) {
            includeBone(bounds, modelPose, bone);
        }
        return bounds;
    }

    private static void includeBone(
            GuiBounds bounds, PoseStack poseStack, GeoBone bone) {
        poseStack.pushPose();
        RenderUtil.prepMatrixForBone(poseStack, bone);

        if (!bone.isHidden()) {
            for (GeoCube cube : bone.getCubes()) {
                poseStack.pushPose();
                RenderUtil.translateToPivotPoint(poseStack, cube);
                RenderUtil.rotateMatrixAroundCube(poseStack, cube);
                RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
                for (GeoQuad quad : cube.quads()) {
                    if (quad == null) continue;
                    for (GeoVertex vertex : quad.vertices()) {
                        Vector4f point = poseStack.last().pose().transform(
                                new Vector4f(vertex.position(), 1.0F));
                        bounds.include(point.x(), point.y(), point.z());
                    }
                }
                poseStack.popPose();
            }
        }

        if (!bone.isHidingChildren()) {
            for (GeoBone child : bone.getChildBones()) {
                includeBone(bounds, poseStack, child);
            }
        }
        poseStack.popPose();
    }

    private static final class GuiBounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(double x, double y, double z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        private boolean valid() {
            return minX <= maxX && minY <= maxY && minZ <= maxZ;
        }

        private double centerX() {
            return (minX + maxX) * 0.5D;
        }

        private double centerY() {
            return (minY + maxY) * 0.5D;
        }

        private double centerZ() {
            return (minZ + maxZ) * 0.5D;
        }

        private double isometricFit() {
            double depthWidth = (maxX - minX) + (maxZ - minZ);
            double projectedWidth = ISOMETRIC_X_FACTOR * depthWidth;
            double projectedHeight = ISOMETRIC_Y_DEPTH_FACTOR * depthWidth
                    + ISOMETRIC_Y_HEIGHT_FACTOR * (maxY - minY);
            if (projectedWidth <= 0.0D || projectedHeight <= 0.0D) {
                return 1.0D;
            }
            return Math.max(0.05D, Math.min(2.0D, Math.min(
                    VANILLA_BLOCK_PROJECTED_WIDTH / projectedWidth,
                    VANILLA_BLOCK_PROJECTED_HEIGHT / projectedHeight)));
        }
    }
}
