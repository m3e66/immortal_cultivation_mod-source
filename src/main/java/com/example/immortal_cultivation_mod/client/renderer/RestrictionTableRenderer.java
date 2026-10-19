package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.FormationLayers;

import com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity;
import com.example.immortal_cultivation_mod.block.RestrictionTableSigils;
import com.example.immortal_cultivation_mod.client.model.RestrictionTableModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class RestrictionTableRenderer extends GeoBlockRenderer<RestrictionTableBlockEntity> {
   private static final List<List<ResourceLocation>> SIGILS = RestrictionTableSigils.ARTWORK.stream()
           .map(FormationLayers::restriction).toList();
   public RestrictionTableRenderer(BlockEntityRendererProvider.Context context) {
      super(new RestrictionTableModel());
   }

   public void render(RestrictionTableBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      poseStack.pushPose();
      poseStack.translate((double)0.5F, (double)0.0F, (double)-0.5F);
      super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      poseStack.popPose();
      if (animatable.getLevel() != null) {
         double ticks = animatable.getLevel().getGameTime() % 24000L + partialTick;
         for (int slot = 0; slot < RestrictionTableSigils.COUNT; slot++) {
            var sigil = RestrictionTableSigils.sample(animatable.getBlockPos().asLong(), slot, ticks);
            renderSigil(poseStack, bufferSource, sigil, ticks);
         }
      }
   }

   private static void renderSigil(PoseStack poses, MultiBufferSource buffers,
                                   RestrictionTableSigils.Pose sigil, double ticks) {
      int glowAlpha = (int)(225 + 30 * Math.sin(ticks * Math.PI * 2 / 100));
      for (int layer = 0; layer < FormationLayers.COUNT; layer++) {
         var texture = SIGILS.get(sigil.textureIndex()).get(layer);
         double angle = Math.toRadians(FormationLayers.angle(ticks, layer));
         double cos = Math.cos(angle), sin = Math.sin(angle);
         quad(buffers.getBuffer(RestrictionSigilGlow.type(texture)), poses.last(), sigil,
                 cos, sin, layer, 1.25, glowAlpha, true);
         quad(buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), poses.last(), sigil,
                 cos, sin, layer, 1, 255, false);
      }
   }

   private static void quad(VertexConsumer vertices, PoseStack.Pose pose, RestrictionTableSigils.Pose sigil,
                            double cos, double sin, int layer, double scale, int alpha, boolean glow) {
      vertex(vertices, pose, sigil, -1, -1, 0, 1, cos, sin, layer, scale, alpha, glow);
      vertex(vertices, pose, sigil, 1, -1, 1, 1, cos, sin, layer, scale, alpha, glow);
      vertex(vertices, pose, sigil, 1, 1, 1, 0, cos, sin, layer, scale, alpha, glow);
      vertex(vertices, pose, sigil, -1, 1, 0, 0, cos, sin, layer, scale, alpha, glow);
   }

   private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, RestrictionTableSigils.Pose sigil,
                              float horizontal, float vertical, float u, float v, double cos, double sin, int layer,
                              double scale, int alpha, boolean glow) {
      double x = horizontal * cos - vertical * sin;
      double y = horizontal * sin + vertical * cos;
      Vec3 point = sigil.center().add(sigil.tangent().scale(x * RestrictionTableSigils.HALF_SIZE * scale))
              .add(sigil.up().scale(y * RestrictionTableSigils.HALF_SIZE * scale))
              .add(sigil.inwardNormal().scale(layer * 0.001 + (glow ? 0.003 : 0)));
      Vec3 normal = sigil.inwardNormal();
      vertices.addVertex(pose.pose(), (float)point.x, (float)point.y, (float)point.z)
              .setColor(255, 255, 255, alpha).setUv(u, v);
      if (!glow) vertices.setOverlay(OverlayTexture.NO_OVERLAY)
              .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
   }

   public AABB getRenderBoundingBox(RestrictionTableBlockEntity blockEntity) {
      return (new AABB(blockEntity.getBlockPos())).inflate(0.7);
   }
}
