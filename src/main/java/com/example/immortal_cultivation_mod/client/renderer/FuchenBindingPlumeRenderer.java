package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
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

public class FuchenBindingPlumeRenderer
        extends GeoEntityRenderer<FuchenBindingPlumeEntity> {
    private static final ResourceLocation PLUME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/entity/fuchen_binding_plume.png");
    private static final float TEXTURE_SIZE = 123.0F;

    public FuchenBindingPlumeRenderer(EntityRendererProvider.Context context) {
        super(context, new FuchenBindingPlumeModel());
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            FuchenBindingPlumeEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        Entity target = entity.getTargetEntity();
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        float horizontalScale = Mth.clamp(livingTarget.getBbWidth() / 0.81F, 0.7F, 2.25F);
        float verticalScale = Mth.clamp(livingTarget.getBbHeight() / 0.77F, 1.0F, 3.25F);

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.22D, 0.0D);
        poseStack.scale(horizontalScale, verticalScale, horizontalScale);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();

        Entity owner = entity.getOwnerEntity();
        if (owner instanceof LivingEntity livingOwner) {
            renderPlumeTether(
                    entity,
                    livingOwner,
                    livingTarget,
                    partialTick,
                    poseStack,
                    buffer,
                    packedLight);
        }
    }

    private static void renderPlumeTether(
            FuchenBindingPlumeEntity plume,
            LivingEntity owner,
            LivingEntity target,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        Vec3 plumeOrigin = interpolatedPosition(plume, partialTick);
        Vec3 ownerPosition = interpolatedPosition(owner, partialTick);
        Vec3 targetPosition = interpolatedPosition(target, partialTick);
        float bodyYaw = Mth.lerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 forward = new Vec3(
                -Math.sin(yawRadians),
                0.0D,
                Math.cos(yawRadians));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double armSide = owner.getMainArm() == HumanoidArm.RIGHT ? 0.28D : -0.28D;
        Vec3 start = ownerPosition
                .add(0.0D, owner.getBbHeight() * 1.10D, 0.0D)
                .add(forward.scale(0.70D))
                .add(side.scale(armSide))
                .subtract(plumeOrigin);
        Vec3 end = targetPosition
                .add(0.0D, target.getBbHeight() * 0.58D, 0.0D)
                .subtract(plumeOrigin);

        double tetherLength = start.distanceTo(end);
        int segmentCount = Mth.clamp(
                Mth.ceil(tetherLength * 12.0D),
                8,
                24);
        Vec3 control = start.lerp(end, 0.5D)
                .add(side.scale(0.06D))
                .add(0.0D, -0.08D, 0.0D);
        VertexConsumer vertices = buffer.getBuffer(
                RenderType.entityCutoutNoCull(PLUME_TEXTURE));
        Vec3 previous = start;
        for (int i = 1; i <= segmentCount; i++) {
            double progress = i / (double) segmentCount;
            Vec3 point = quadraticBezier(start, control, end, progress);
            float width = 0.065F
                    + (float) Math.sin(progress * Math.PI) * 0.025F;
            renderPlumeSegment(
                    poseStack,
                    vertices,
                    previous,
                    point,
                    width,
                    packedLight);
            previous = point;
        }
    }

    private static Vec3 quadraticBezier(
            Vec3 start,
            Vec3 control,
            Vec3 end,
            double progress) {
        double inverse = 1.0D - progress;
        return start.scale(inverse * inverse)
                .add(control.scale(2.0D * inverse * progress))
                .add(end.scale(progress * progress));
    }

    private static void renderPlumeSegment(
            PoseStack poseStack,
            VertexConsumer vertices,
            Vec3 start,
            Vec3 end,
            float width,
            int packedLight) {
        Vec3 delta = end.subtract(start);
        float length = (float) delta.length();
        if (length < 1.0E-4F) {
            return;
        }
        Vector3f direction = new Vector3f(
                (float) delta.x,
                (float) delta.y,
                (float) delta.z).normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                new Vector3f(0.0F, 1.0F, 0.0F),
                direction);

        poseStack.pushPose();
        Vec3 middle = start.add(end).scale(0.5D);
        poseStack.translate(middle.x, middle.y, middle.z);
        poseStack.mulPose(rotation);
        renderTexturedPlumeCuboid(
                poseStack,
                vertices,
                width,
                length + 0.018F,
                packedLight);
        poseStack.popPose();
    }

    private static void renderTexturedPlumeCuboid(
            PoseStack poseStack,
            VertexConsumer vertices,
            float width,
            float length,
            int packedLight) {
        float x = width;
        float y = length * 0.5F;
        float z = width;
        PoseStack.Pose pose = poseStack.last();

        face(vertices, pose,
                -x, -y, -z, x, -y, -z, x, y, -z, -x, y, -z,
                94, 4, 103, 9, 0, 0, -1, packedLight);
        face(vertices, pose,
                x, -y, z, -x, -y, z, -x, y, z, x, y, z,
                107, 4, 116, 9, 0, 0, 1, packedLight);
        face(vertices, pose,
                -x, -y, z, -x, -y, -z, -x, y, -z, -x, y, z,
                90, 4, 94, 9, -1, 0, 0, packedLight);
        face(vertices, pose,
                x, -y, -z, x, -y, z, x, y, z, x, y, -z,
                103, 4, 107, 9, 1, 0, 0, packedLight);
        face(vertices, pose,
                -x, y, -z, x, y, -z, x, y, z, -x, y, z,
                94, 0, 103, 4, 0, 1, 0, packedLight);
        face(vertices, pose,
                -x, -y, z, x, -y, z, x, -y, -z, -x, -y, -z,
                103, 0, 112, 4, 0, -1, 0, packedLight);
    }

    private static void face(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float minU, float minV,
            float maxU, float maxV,
            float normalX, float normalY, float normalZ,
            int packedLight) {
        vertex(vertices, pose, x1, y1, z1,
                minU, maxV, normalX, normalY, normalZ, packedLight);
        vertex(vertices, pose, x2, y2, z2,
                maxU, maxV, normalX, normalY, normalZ, packedLight);
        vertex(vertices, pose, x3, y3, z3,
                maxU, minV, normalX, normalY, normalZ, packedLight);
        vertex(vertices, pose, x4, y4, z4,
                minU, minV, normalX, normalY, normalZ, packedLight);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ,
            int packedLight) {
        vertices.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u / TEXTURE_SIZE, v / TEXTURE_SIZE)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }
}
