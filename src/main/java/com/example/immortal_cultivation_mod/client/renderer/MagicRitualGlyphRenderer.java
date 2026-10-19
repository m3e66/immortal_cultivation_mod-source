package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.FormationLayers;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MagicRitualGlyphRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   private static final double ORPHAN_RENDER_DISTANCE = (double)96.0F;
   private static final float ENTRY_EXPANSION_TICKS = 30.0F;

   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
   private static final int FORCEFIELD_MIN_LONGITUDE_SEGMENTS = 32;
   private static final int FORCEFIELD_MAX_LONGITUDE_SEGMENTS = 64;
   private static final float FORMATION_FLAG_INNER_RADIUS_FACTOR = 0.34F;
   private static final float FORMATION_FLAG_OUTER_MARGIN = 0.75F;
   private static final float FORMATION_FLAG_MIN_INNER_RADIUS = 0.9F;
   private static final float FORMATION_FLAG_MIN_OUTER_RADIUS = 1.9F;
   private static final float FORMATION_FLAG_SCALE = 0.72F;
   private static final double FORMATION_FLAG_BASE_HEIGHT = 0.22;
   private static final float FORMATION_FLAG_HOVER_HEIGHT = 0.2F;
   private static final float FORMATION_FLAG_SPIN_SPEED = 2.1F;
   private final ItemRenderer itemRenderer;

   public MagicRitualGlyphRenderer(BlockEntityRendererProvider.Context context) {
      this.itemRenderer = context.getItemRenderer();
   }

   public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      MagicCenterBlockEntity var10000;
      if (blockEntity instanceof MagicCenterBlockEntity centerEntity) {
         var10000 = centerEntity;
      } else {
         var10000 = null;
      }

      MagicCenterBlockEntity renderedCenter = var10000;
      boolean forcefieldCenter = renderedCenter != null && MagicCenterTypeRegistry.FORCEFIELD.equals(renderedCenter.getCenterTypeId());
      ItemStack reforgeWeapon = renderedCenter != null && MagicCenterTypeRegistry.REFORGE_BLADE.equals(renderedCenter.getCenterTypeId()) ? renderedCenter.getReforgeWeapon() : ItemStack.EMPTY;
      boolean hasReforgeWeapon = !reforgeWeapon.isEmpty();
      boolean hasFormationFlags = renderedCenter != null && renderedCenter.hasFormationFlags();
      boolean hasTeleportChannel = renderedCenter != null && renderedCenter.hasTeleportChannel();
      boolean temporaryCenter = renderedCenter != null && renderedCenter.isTemporary();
      boolean glyphEnabled = renderedCenter == null || renderedCenter.isParticlesEnabled();
      boolean authorizedReveal = renderedCenter != null && renderedCenter.hasAuthorizedPlayerInside();
      boolean entryReveal = authorizedReveal && !glyphEnabled;
      if (glyphEnabled || forcefieldCenter || authorizedReveal || hasReforgeWeapon || hasFormationFlags || hasTeleportChannel || temporaryCenter) {
         MagicCenterBlockEntity owningCenter = findOwningCenter(blockEntity);
         if (!(blockEntity instanceof MagicCircleBlockEntity) || owningCenter == null || owningCenter.isParticlesEnabled()) {
            boolean center = renderedCenter != null;
            Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 renderOrigin = owningCenter == null ? Vec3.atCenterOf(blockEntity.getBlockPos()) : owningCenter.centerPoint();
            double renderDistance = getRenderDistance(owningCenter);
            double distanceSquared = renderOrigin.distanceToSqr(cameraPosition);
            if (!(distanceSquared > renderDistance * renderDistance)) {
               boolean active = owningCenter != null && owningCenter.isActive();
               float alpha = active ? 1.0F : (distanceSquared > (double)2304.0F ? 0.45F : 1.0F);
               if (owningCenter != null && !owningCenter.isActive()) {
                  alpha *= 0.35F;
               }

               int color = owningCenter == null ? 16777215 : owningCenter.getGlyphColor();
               float red = (float)(color >> 16 & 255) / 255.0F;
               float green = (float)(color >> 8 & 255) / 255.0F;
               float blue = (float)(color & 255) / 255.0F;
               float halfSize = center ? (float)((MagicCenterBlockEntity)blockEntity).getRadius() : 0.78F;
               float pulse = 1.0F + 0.012F * (float)Math.sin((double)(blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime()) * 0.12);
               halfSize *= pulse;
               long gameTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime();
               float time = (float)gameTime + partialTick;
               if (!center) {
                  renderFormation(poseStack, bufferSource, FormationLayers.circle(blockEntity.getBlockState().getValue(MagicCircleBlock.CIRCLE_TYPE)),
                          halfSize, (double)gameTime + partialTick, 0.09F, red, green, blue, alpha, active);
               } else {
                  var textures = FormationLayers.center(renderedCenter.getCenterTypeId());
                  if (glyphEnabled || authorizedReveal) {
                     float revealScale = entryReveal ? entryExpansionScale(renderedCenter, time) : 1.0F;
                     float glyphSize = halfSize * revealScale;
                     float glyphAlpha = entryReveal ? Math.max(alpha, 0.7F) : alpha;
                     renderFormation(poseStack, bufferSource, textures, glyphSize, (double)gameTime + partialTick,
                             0.08F, red, green, blue, glyphAlpha, active);
                  }

                  if (forcefieldCenter) {
                     renderForcefieldSphere(poseStack, bufferSource, halfSize, time, red, green, blue, alpha * (active ? 1.0F : 0.28F));
                  }

                  if (hasTeleportChannel) {
                     renderTeleportChannel(renderedCenter, time, poseStack, bufferSource, red, green, blue, Math.max(0.82F, alpha));
                  }

                  if (hasReforgeWeapon) {
                     this.renderReforgeWeapon(renderedCenter, reforgeWeapon, time, poseStack, bufferSource, packedLight);
                  }

                  if (hasFormationFlags) {
                     this.renderFormationFlags(renderedCenter, time, poseStack, bufferSource, packedLight);
                  }

                  if (temporaryCenter) {
                     renderTemporaryCountdown(renderedCenter, partialTick, poseStack, bufferSource);
                  }

               }
            }
         }
      }
   }

   private void renderFormationFlags(MagicCenterBlockEntity center, float time, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      float outerRadius = Math.max(1.9F, (float)center.getRadius() - 0.75F);
      float innerRadius = Math.max(0.9F, outerRadius * 0.34F);
      float ringStep = (outerRadius - innerRadius) / 2.0F;

      for(int slot = 0; slot < 24; ++slot) {
         ItemStack flag = center.getFormationFlagForRender(slot);
         if (!flag.isEmpty()) {
            int layer = slot / 8;
            int point = slot % 8;
            double angleDegrees = (double)-90.0F + (double)point * (double)45.0F;
            double angleRadians = Math.toRadians(angleDegrees);
            double ringRadius = (double)(innerRadius + ringStep * (float)layer);
            double offsetX = Math.cos(angleRadians) * ringRadius;
            double offsetZ = Math.sin(angleRadians) * ringRadius;
            float bob = 0.2F * (0.5F + 0.5F * (float)Math.sin((double)(time * 0.12F + (float)slot * 0.65F)));
            float spin = (time * 2.1F + (float)slot * 11.25F) % 360.0F;
            poseStack.pushPose();
            poseStack.translate((double)0.5F + offsetX, 0.22 + (double)bob, (double)0.5F + offsetZ);
            poseStack.mulPose(Axis.YP.rotationDegrees((float)(-angleDegrees) + spin));
            poseStack.scale(0.72F, 0.72F, 0.72F);
            this.itemRenderer.renderStatic(flag, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, center.getLevel(), (int)(center.getBlockPos().asLong() ^ (long)slot * 31L));
            poseStack.popPose();
         }
      }

   }

   private void renderReforgeWeapon(MagicCenterBlockEntity center, ItemStack weapon, float time, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      float bob = 0.04F * (float)Math.sin((double)(time * 0.1F));
      poseStack.pushPose();
      poseStack.translate((double)0.5F, (double)1.25F + (double)bob, (double)0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(time));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
      poseStack.scale(1.35F, 1.35F, 1.35F);
      this.itemRenderer.renderStatic(weapon, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, center.getLevel(), (int)center.getBlockPos().asLong());
      poseStack.popPose();
   }

   private static void renderTemporaryCountdown(MagicCenterBlockEntity center, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
      if (center.getLevel() != null && center.getTemporaryExpiresAt() >= 0L) {
         double remainingTicks = (double)((float)center.getTemporaryExpiresAt() - ((float)center.getLevel().getGameTime() + partialTick));
         int remainingSeconds = Math.max(0, (int)Math.ceil(remainingTicks / (double)20.0F));
         if (remainingSeconds > 0) {
            Component text = Component.translatable("label.immortal_cultivation_mod.temporary_center_countdown", new Object[]{remainingSeconds});
            Font font = Minecraft.getInstance().font;
            poseStack.pushPose();
            poseStack.translate((double)0.5F, (double)2.0F, (double)0.5F);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(0.025F, -0.025F, 0.025F);
            float textX = (float)(-font.width(text)) / 2.0F;
            font.drawInBatch(text, textX, 0.0F, -798614, false, poseStack.last().pose(), bufferSource, DisplayMode.NORMAL, 1879048192, 15728880);
            poseStack.popPose();
         }
      }
   }

   private static void renderTeleportChannel(MagicCenterBlockEntity center, float time, PoseStack poseStack, MultiBufferSource bufferSource, float red, float green, float blue, float alpha) {
      if (center.getLevel() != null && center.getTeleportChannelPlayer() != null) {
         Player player = center.getLevel().getPlayerByUUID(center.getTeleportChannelPlayer());
         if (player != null) {
            Vec3 playerPos = player.getPosition(time - (float)Math.floor((double)time));
            Vec3 blockOrigin = Vec3.atLowerCornerOf(center.getBlockPos());
            Vec3 relative = playerPos.subtract(blockOrigin);
            float duration = Math.max(1.0F, (float)center.getTeleportChannelDurationTicks());
            float progress = Math.max(0.0F, Math.min(1.0F, (time - (float)center.getTeleportChannelStartTime()) / duration));
            float bodyHeight = player.getBbHeight();
            float travel = bodyHeight * 0.55F;
            float lowerHeight = 0.05F + progress * travel;
            float upperHeight = bodyHeight + 0.45F - progress * travel;
            float glyphRadius = Math.max(1.05F, player.getBbWidth() + 0.65F);
            poseStack.pushPose();
            poseStack.translate(relative.x - (double)0.5F, relative.y, relative.z - (double)0.5F);
            var textures = FormationLayers.center(MagicCenterTypeRegistry.TELEPORT);
            double ticks = center.getLevel().getGameTime() + (double)(time - (float)Math.floor(time));
            renderFormation(poseStack, bufferSource, textures, glyphRadius, ticks * 6.0,
                    lowerHeight, red, green, blue, alpha, true);
            renderFormation(poseStack, bufferSource, textures, glyphRadius, -ticks * 6.0,
                    upperHeight, red, green, blue, alpha, true);
            poseStack.popPose();
         }
      }
   }

   private static float entryExpansionScale(MagicCenterBlockEntity center, float time) {
      long entryTime = center.getAuthorizedEntryGameTime();
      if (entryTime < 0L) {
         return 1.0F;
      } else {
         float progress = Math.max(0.0F, Math.min(1.0F, (time - (float)entryTime) / 30.0F));
         float remaining = 1.0F - progress;
         return 1.0F - remaining * remaining * remaining;
      }
   }

   private static void renderFormation(PoseStack poseStack, MultiBufferSource bufferSource,
                                       java.util.List<ResourceLocation> textures, float halfSize, double ticks,
                                       float height, float red, float green, float blue, float alpha, boolean active) {
      for (int layer = 0; layer < FormationLayers.COUNT; layer++) {
         renderGlyph(poseStack, bufferSource, textures.get(layer), halfSize, FormationLayers.angle(ticks, layer),
                 height + layer * 0.006F, red, green, blue, alpha, active);
      }
   }

   private static void renderGlyph(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, float halfSize, float spin, float height, float red, float green, float blue, float alpha, boolean active) {
      renderLayer(poseStack, bufferSource, texture, halfSize, spin, height, red, green, blue, alpha, active);
      if (active) {
         float glowRed = red;
         float glowGreen = green;
         float glowBlue = blue;

         for(int layer = 1; layer <= 7; ++layer) {
            float progress = (float)layer / 7.0F;
            float glowSize = halfSize * (1.0F + progress * 0.055F);
            float glowHeight = height + (float)layer * 0.026F;
            float glowAlpha = alpha * (0.19F * (1.0F - progress) + 0.025F);
            renderLayer(poseStack, bufferSource, texture, glowSize, spin, glowHeight, glowRed, glowGreen, glowBlue, glowAlpha, false);
         }

         renderLayer(poseStack, bufferSource, texture, halfSize * 1.085F, spin, height + 0.008F, glowRed, glowGreen, glowBlue, alpha * 0.1F, false);
      }
   }

   private static void renderLayer(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, float halfSize, float spin, float height, float red, float green, float blue, float alpha, boolean solid) {
      poseStack.pushPose();
      poseStack.translate((double)0.5F, (double)height, (double)0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(spin));
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = bufferSource.getBuffer(solid ? RenderType.entityCutoutNoCull(texture) : RenderType.entityTranslucent(texture));
      int light = 15728880;
      vertex(vertices, pose, -halfSize, 0.0F, -halfSize, 0.0F, 0.0F, light, red, green, blue, alpha);
      vertex(vertices, pose, -halfSize, 0.0F, halfSize, 0.0F, 1.0F, light, red, green, blue, alpha);
      vertex(vertices, pose, halfSize, 0.0F, halfSize, 1.0F, 1.0F, light, red, green, blue, alpha);
      vertex(vertices, pose, halfSize, 0.0F, -halfSize, 1.0F, 0.0F, light, red, green, blue, alpha);
      poseStack.popPose();
   }

   private static void renderForcefieldSphere(PoseStack poseStack, MultiBufferSource bufferSource, float radius, float time, float red, float green, float blue, float alpha) {
      float shimmer = 0.94F + 0.06F * (float)Math.sin((double)(time * 0.09F));
      renderSphereShell(poseStack, bufferSource, radius, red, green, blue, alpha * 0.1F * shimmer);
      renderSphereShell(poseStack, bufferSource, radius * 1.025F, red, green, blue, alpha * 0.035F * shimmer);
   }

   private static void renderSphereShell(PoseStack poseStack, MultiBufferSource bufferSource, float radius, float red, float green, float blue, float alpha) {
      poseStack.pushPose();
      poseStack.translate((double)0.5F, 0.08, (double)0.5F);
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      int light = 15728880;
      int longitudeSegments = Math.max(32, Math.min(64, (int)Math.ceil((double)(radius / 14.0F)) * 8));
      int latitudeSegments = longitudeSegments / 2;

      for(int latitudeIndex = 0; latitudeIndex < latitudeSegments; ++latitudeIndex) {
         float latitude0 = (float)((-Math.PI / 2D) + Math.PI * (double)latitudeIndex / (double)latitudeSegments);
         float latitude1 = (float)((-Math.PI / 2D) + Math.PI * (double)(latitudeIndex + 1) / (double)latitudeSegments);
         float v0 = (float)latitudeIndex / (float)latitudeSegments;
         float v1 = (float)(latitudeIndex + 1) / (float)latitudeSegments;

         for(int longitudeIndex = 0; longitudeIndex < longitudeSegments; ++longitudeIndex) {
            float longitude0 = (float)((Math.PI * 2D) * (double)longitudeIndex / (double)longitudeSegments);
            float longitude1 = (float)((Math.PI * 2D) * (double)(longitudeIndex + 1) / (double)longitudeSegments);
            float u0 = (float)longitudeIndex / (float)longitudeSegments;
            float u1 = (float)(longitudeIndex + 1) / (float)longitudeSegments;
            sphereVertex(vertices, pose, radius, latitude0, longitude0, u0, v0, light, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, latitude1, longitude0, u0, v1, light, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, latitude1, longitude1, u1, v1, light, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, latitude0, longitude1, u1, v0, light, red, green, blue, alpha);
         }
      }

      poseStack.popPose();
   }

   public boolean shouldRenderOffScreen(T blockEntity) {
      return true;
   }

   public AABB getRenderBoundingBox(T blockEntity) {
      return AABB.INFINITE;
   }

   public int getViewDistance() {
      return 431;
   }

   public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
      MagicCenterBlockEntity owningCenter = findOwningCenter(blockEntity);
      Vec3 renderOrigin = owningCenter == null ? Vec3.atCenterOf(blockEntity.getBlockPos()) : owningCenter.centerPoint();
      double renderDistance = getRenderDistance(owningCenter);
      return renderOrigin.distanceToSqr(cameraPosition) <= renderDistance * renderDistance;
   }

   private static double getRenderDistance(MagicCenterBlockEntity center) {
      return center == null ? (double)96.0F : (double)(center.getRadius() + 128);
   }


   private static MagicCenterBlockEntity findOwningCenter(BlockEntity blockEntity) {
      if (blockEntity instanceof MagicCenterBlockEntity center) {
         return center;
      } else {
         if (blockEntity instanceof MagicCircleBlockEntity circle) {
            if (circle.getLevel() != null) {
               BlockPos centerPos = circle.findOwningCenter();
               if (centerPos != null) {
                  BlockEntity var4 = circle.getLevel().getBlockEntity(centerPos);
                  if (var4 instanceof MagicCenterBlockEntity) {
                     MagicCenterBlockEntity center = (MagicCenterBlockEntity)var4;
                     return center;
                  }
               }
            }
         }

         return null;
      }
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, int light, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
   }

   private static void sphereVertex(VertexConsumer vertices, Matrix4f pose, float radius, float latitude, float longitude, float u, float v, int light, float red, float green, float blue, float alpha) {
      float cosLatitude = (float)Math.cos((double)latitude);
      float normalX = cosLatitude * (float)Math.cos((double)longitude);
      float normalY = (float)Math.sin((double)latitude);
      float normalZ = cosLatitude * (float)Math.sin((double)longitude);
      vertices.addVertex(pose, normalX * radius, normalY * radius, normalZ * radius).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normalX, normalY, normalZ);
   }
}
