package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.DingBlock;
import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.block.DunwuSteleBlock;
import com.example.immortal_cultivation_mod.block.GeoRockBlock;
import com.example.immortal_cultivation_mod.block.GeoRockBlockEntity;
import com.example.immortal_cultivation_mod.block.RealmSpellScrollBlock;
import com.example.immortal_cultivation_mod.client.model.GeoRockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GeoRockBlockRenderer extends GeoBlockRenderer<GeoRockBlockEntity> {
   private final DingItemPileRenderer dingItems;
   public GeoRockBlockRenderer(BlockEntityRendererProvider.Context context) {
      super(new GeoRockModel());
      dingItems = new DingItemPileRenderer(context.getItemRenderer());
   }

   @Override
   public void renderRecursively(PoseStack poseStack, GeoRockBlockEntity animatable, GeoBone bone,
           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
           boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
      if ("chi_jin_sanyang_ding".equals(animatable.modelName()) && bone.getName().startsWith("sun_glow_")) {
         packedLight = LightTexture.FULL_BRIGHT;
      }
      super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
              isReRender, partialTick, packedLight, packedOverlay, colour);
   }

   public void render(GeoRockBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (animatable.getBlockState().getBlock() instanceof DingBlock) {
         float scale = animatable.getBlockState().getValue(GeoRockBlock.SCALE);
         poseStack.pushPose();
         poseStack.translate(0.5, 0, 0.5);
         poseStack.scale(scale, scale, scale);
         poseStack.translate(-0.5, 0, -0.5);
         super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         poseStack.popPose();
         if (animatable instanceof DingBlockEntity ding && (!ding.hasLid() || ding.isLidOpen())) {
            dingItems.render(ding, poseStack, bufferSource, packedLight, packedOverlay);
         }
         return;
      }
      if (usesModelKitCoordinates(animatable.modelName())) {
         poseStack.pushPose();
         if (animatable.modelName().endsWith("_spell_scroll_block")) {
            poseStack.translate((double)0.5F, (double)0.0F, (double)0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(modelKitRotationDegrees((Direction)animatable.getBlockState().getValue(RealmSpellScrollBlock.FACING))));
            poseStack.translate((double)0.0F, (double)0.0F, (double)-1.0F);
         } else {
            float scale = (float)(Integer)animatable.getBlockState().getValue(GeoRockBlock.SCALE);
            poseStack.translate((double)0.5F, (double)0.0F, (double)0.5F);
            poseStack.scale(scale, scale, scale);
            poseStack.translate((double)0.0F, (double)0.0F, (double)-1.0F);
         }

         super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         poseStack.popPose();
      } else if (!"dunwu_stele".equals(animatable.modelName()) && animatable.getBlockState().hasProperty(GeoRockBlock.SCALE)) {
         float scale = (float)(Integer)animatable.getBlockState().getValue(GeoRockBlock.SCALE);
         poseStack.pushPose();
         poseStack.translate((double)0.5F, (double)0.0F, (double)0.5F);
         poseStack.scale(scale, scale, scale);
         poseStack.translate((double)-0.5F, (double)0.0F, (double)-0.5F);
         super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         poseStack.popPose();
      } else if (!"dunwu_stele".equals(animatable.modelName())) {
         super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      } else {
         poseStack.pushPose();
         poseStack.translate((double)0.5F, (double)0.0F, (double)0.5F);
         poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees((Direction)animatable.getBlockState().getValue(DunwuSteleBlock.FACING))));
         float scale = 1.5F * (float)(Integer)animatable.getBlockState().getValue(GeoRockBlock.SCALE);
         poseStack.scale(scale, scale, scale);
         poseStack.translate((double)0.0F, (double)0.0F, (double)-1.0F);
         super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         poseStack.popPose();
      }
   }

   protected Direction getFacing(GeoRockBlockEntity blockEntity) {
      if (blockEntity.getBlockState().getBlock() instanceof DingBlock) {
         return blockEntity.getBlockState().getValue(DingBlock.FACING);
      }
      return !"dunwu_stele".equals(blockEntity.modelName()) && !blockEntity.modelName().endsWith("_spell_scroll_block") ? super.getFacing(blockEntity) : Direction.NORTH;
   }

   public AABB getRenderBoundingBox(GeoRockBlockEntity blockEntity) {
      if (blockEntity.getBlockState().hasProperty(GeoRockBlock.SCALE)) {
         int scale = (Integer)blockEntity.getBlockState().getValue(GeoRockBlock.SCALE);
         double centerX = (double)blockEntity.getBlockPos().getX() + (double)0.5F;
         double centerZ = (double)blockEntity.getBlockPos().getZ() + (double)0.5F;
         double minY = (double)blockEntity.getBlockPos().getY();
         if (blockEntity.getBlockState().getBlock() instanceof DingBlock) {
            AABB bounds = new AABB(centerX - 1.3 * scale, minY, centerZ - 1.3 * scale,
                    centerX + 1.3 * scale, minY + 2.5 * scale, centerZ + 1.3 * scale);
            return bounds;
         }
         return new AABB(centerX - (double)scale, minY, centerZ - (double)scale, centerX + (double)scale, minY + (double)2.0F * (double)scale, centerZ + (double)scale);
      } else {
         return super.getRenderBoundingBox(blockEntity);
      }
   }

   private static float rotationDegrees(Direction facing) {
      float var10000;
      switch (facing) {
         case EAST -> var10000 = 90.0F;
         case SOUTH -> var10000 = 180.0F;
         case WEST -> var10000 = 270.0F;
         default -> var10000 = 0.0F;
      }

      return var10000;
   }

   private static float modelKitRotationDegrees(Direction facing) {
      float var10000;
      switch (facing) {
         case EAST -> var10000 = 270.0F;
         case SOUTH -> var10000 = 180.0F;
         case WEST -> var10000 = 90.0F;
         default -> var10000 = 0.0F;
      }

      return var10000;
   }

   private static boolean usesModelKitCoordinates(String modelName) {
      if (modelName.endsWith("_spell_scroll_block")) {
         return true;
      } else {
         boolean var10000;
         switch (modelName) {
            case "qiannian_lingmu_heart":
            case "yunwen_silver_crystal":
            case "jingtie_motherstone":
            case "chitong_sui":
            case "hanpo_jade":
            case "bisui_stone":
            case "star_dust_stone":
            case "moxuan_iron":
               var10000 = true;
               break;
            default:
               var10000 = false;
         }

         return var10000;
      }
   }
}
