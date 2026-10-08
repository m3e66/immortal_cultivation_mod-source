package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.FuchenBindingPlumeModel;
import com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FuchenBindingPlumeRenderer extends GeoEntityRenderer<FuchenBindingPlumeEntity> {
   private static final ResourceLocation PLUME_TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/fuchen_binding_plume.png");
   private static final float TEXTURE_SIZE = 123.0F;

   public FuchenBindingPlumeRenderer(EntityRendererProvider.Context context) {
      super(context, new FuchenBindingPlumeModel());
      this.shadowRadius = 0.0F;
   }

   public void render(FuchenBindingPlumeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Entity target = entity.getTargetEntity();
      if (target instanceof LivingEntity livingTarget) {
         float horizontalScale = Mth.clamp(livingTarget.getBbWidth() / 0.81F, 0.7F, 2.25F);
         float verticalScale = Mth.clamp(livingTarget.getBbHeight() / 0.77F, 1.0F, 3.25F);
         poseStack.pushPose();
         poseStack.translate((double)0.0F, -0.22, (double)0.0F);
         poseStack.scale(horizontalScale, verticalScale, horizontalScale);
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
         poseStack.popPose();
         Entity owner = entity.getOwnerEntity();
         if (owner instanceof LivingEntity livingOwner) {
            renderPlumeTether(entity, livingOwner, livingTarget, partialTick, poseStack, buffer, packedLight);
         }

      }
   }

   private static void renderPlumeTether(FuchenBindingPlumeEntity plume, LivingEntity owner, LivingEntity target, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Vec3 plumeOrigin = interpolatedPosition(plume, partialTick);
      Vec3 ownerPosition = interpolatedPosition(owner, partialTick);
      Vec3 targetPosition = interpolatedPosition(target, partialTick);
      float bodyYaw = Mth.lerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
      double yawRadians = Math.toRadians((double)bodyYaw);
      Vec3 forward = new Vec3(-Math.sin(yawRadians), (double)0.0F, Math.cos(yawRadians));
      Vec3 side = new Vec3(-forward.z, (double)0.0F, forward.x);
      double armSide = owner.getMainArm() == HumanoidArm.RIGHT ? 0.28 : -0.28;
      Vec3 start = ownerPosition.add((double)0.0F, (double)owner.getBbHeight() * 1.1, (double)0.0F).add(forward.scale(0.7)).add(side.scale(armSide)).subtract(plumeOrigin);
      Vec3 end = targetPosition.add((double)0.0F, (double)target.getBbHeight() * 0.58, (double)0.0F).subtract(plumeOrigin);
      double tetherLength = start.distanceTo(end);
      int segmentCount = Mth.clamp(Mth.ceil(tetherLength * (double)12.0F), 8, 24);
      Vec3 control = start.lerp(end, (double)0.5F).add(side.scale(0.06)).add((double)0.0F, -0.08, (double)0.0F);
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(PLUME_TEXTURE));
      Vec3 previous = start;

      for(int i = 1; i <= segmentCount; ++i) {
         double progress = (double)i / (double)segmentCount;
         Vec3 point = quadraticBezier(start, control, end, progress);
         float width = 0.065F + (float)Math.sin(progress * Math.PI) * 0.025F;
         renderPlumeSegment(poseStack, vertices, previous, point, width, packedLight);
         previous = point;
      }

   }

   private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, double progress) {
      double inverse = (double)1.0F - progress;
      return start.scale(inverse * inverse).add(control.scale((double)2.0F * inverse * progress)).add(end.scale(progress * progress));
   }

   private static void renderPlumeSegment(PoseStack poseStack, VertexConsumer vertices, Vec3 start, Vec3 end, float width, int packedLight) {
      Vec3 delta = end.subtract(start);
      float length = (float)delta.length();
      if (!(length < 1.0E-4F)) {
         Vector3f direction = (new Vector3f((float)delta.x, (float)delta.y, (float)delta.z)).normalize();
         Quaternionf rotation = (new Quaternionf()).rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), direction);
         poseStack.pushPose();
         Vec3 middle = start.add(end).scale((double)0.5F);
         poseStack.translate(middle.x, middle.y, middle.z);
         poseStack.mulPose(rotation);
         renderTexturedPlumeCuboid(poseStack, vertices, width, length + 0.018F, packedLight);
         poseStack.popPose();
      }
   }

   private static void renderTexturedPlumeCuboid(PoseStack poseStack, VertexConsumer vertices, float width, float length, int packedLight) {
      float y = length * 0.5F;
      PoseStack.Pose pose = poseStack.last();
      face(vertices, pose, -width, -y, -width, width, -y, -width, width, y, -width, -width, y, -width, 94.0F, 4.0F, 103.0F, 9.0F, 0.0F, 0.0F, -1.0F, packedLight);
      face(vertices, pose, width, -y, width, -width, -y, width, -width, y, width, width, y, width, 107.0F, 4.0F, 116.0F, 9.0F, 0.0F, 0.0F, 1.0F, packedLight);
      face(vertices, pose, -width, -y, width, -width, -y, -width, -width, y, -width, -width, y, width, 90.0F, 4.0F, 94.0F, 9.0F, -1.0F, 0.0F, 0.0F, packedLight);
      face(vertices, pose, width, -y, -width, width, -y, width, width, y, width, width, y, -width, 103.0F, 4.0F, 107.0F, 9.0F, 1.0F, 0.0F, 0.0F, packedLight);
      face(vertices, pose, -width, y, -width, width, y, -width, width, y, width, -width, y, width, 94.0F, 0.0F, 103.0F, 4.0F, 0.0F, 1.0F, 0.0F, packedLight);
      face(vertices, pose, -width, -y, width, width, -y, width, width, -y, -width, -width, -y, -width, 103.0F, 0.0F, 112.0F, 4.0F, 0.0F, -1.0F, 0.0F, packedLight);
   }

   private static void face(VertexConsumer vertices, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float minU, float minV, float maxU, float maxV, float normalX, float normalY, float normalZ, int packedLight) {
      vertex(vertices, pose, x1, y1, z1, minU, maxV, normalX, normalY, normalZ, packedLight);
      vertex(vertices, pose, x2, y2, z2, maxU, maxV, normalX, normalY, normalZ, packedLight);
      vertex(vertices, pose, x3, y3, z3, maxU, minV, normalX, normalY, normalZ, packedLight);
      vertex(vertices, pose, x4, y4, z4, minU, minV, normalX, normalY, normalZ, packedLight);
   }

   private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int packedLight) {
      vertices.addVertex(pose.pose(), x, y, z).setColor(255, 255, 255, 255).setUv(u / 123.0F, v / 123.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, normalX, normalY, normalZ);
   }

   private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
      return new Vec3(Mth.lerp((double)partialTick, entity.xo, entity.getX()), Mth.lerp((double)partialTick, entity.yo, entity.getY()), Mth.lerp((double)partialTick, entity.zo, entity.getZ()));
   }
}
