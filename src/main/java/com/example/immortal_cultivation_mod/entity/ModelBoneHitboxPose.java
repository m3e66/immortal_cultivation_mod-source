package com.example.immortal_cultivation_mod.entity;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Vector3d;

/**
 * Evaluates the same model-space hierarchy used by GeckoLib for authoritative hitboxes.
 * Input positions and rotations use the original Blockbench JSON coordinate convention.
 */
final class ModelBoneHitboxPose {
    private final Vec3 pivot;
    private final Matrix3d transform;
    private final double modelUnit;

    private ModelBoneHitboxPose(Vec3 pivot, Matrix3d transform, double modelUnit) {
        this.pivot = pivot;
        this.transform = transform;
        this.modelUnit = modelUnit;
    }

    static ModelBoneHitboxPose root(Vec3 entityPosition,
                                    float entityYaw,
                                    float rendererYawOffset,
                                    float modelScale) {
        Matrix3d transform = new Matrix3d().rotationY(
                Math.toRadians(180.0D - entityYaw + rendererYawOffset));
        return new ModelBoneHitboxPose(entityPosition, transform, modelScale / 16.0D);
    }

    ModelBoneHitboxPose child(double modelX, double modelY, double modelZ,
                              float rotationX, float rotationY, float rotationZ) {
        return child(modelX, modelY, modelZ,
                rotationX, rotationY, rotationZ, 1.0F, 1.0F, 1.0F);
    }

    ModelBoneHitboxPose child(double modelX, double modelY, double modelZ,
                              float rotationX, float rotationY, float rotationZ,
                              float scaleX, float scaleY, float scaleZ) {
        Vec3 childPivot = point(modelX, modelY, modelZ);
        Matrix3d childTransform = new Matrix3d(transform)
                // GeckoLib negates Blockbench X/Y rotations but preserves Z.
                .rotateZ(Math.toRadians(rotationZ))
                .rotateY(Math.toRadians(-rotationY))
                .rotateX(Math.toRadians(-rotationX))
                .scale(scaleX, scaleY, scaleZ);
        return new ModelBoneHitboxPose(childPivot, childTransform, modelUnit);
    }

    ModelBoneHitboxPose animatedChild(BoneHitboxAnimation.Motion motion,
                                      float time,
                                      double pivotX, double pivotY, double pivotZ,
                                      float baseRotationX,
                                      float baseRotationY,
                                      float baseRotationZ) {
        return child(
                pivotX + motion.positionX().sample(time),
                pivotY + motion.positionY().sample(time),
                pivotZ + motion.positionZ().sample(time),
                baseRotationX + motion.rotationX().sample(time),
                baseRotationY + motion.rotationY().sample(time),
                baseRotationZ + motion.rotationZ().sample(time),
                motion.scaleX().sample(time),
                motion.scaleY().sample(time),
                motion.scaleZ().sample(time));
    }

    Vec3 point(double modelX, double modelY, double modelZ) {
        // GeckoLib mirrors Blockbench X while baking model geometry and pivots.
        Vector3d offset = new Vector3d(
                -modelX * modelUnit,
                modelY * modelUnit,
                modelZ * modelUnit);
        transform.transform(offset);
        return pivot.add(offset.x, offset.y, offset.z);
    }
}
