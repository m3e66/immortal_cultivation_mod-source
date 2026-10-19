package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Minecraft's exact matrix transforms, with one scratch vector per rendering thread. */
public final class VfxVertexWriter {
    private static final ThreadLocal<Vector3f> POSITION = ThreadLocal.withInitial(Vector3f::new);
    private VfxVertexWriter() {}

    public static VertexConsumer vertex(VertexConsumer out, Matrix4f matrix, float x, float y, float z) {
        // Preserve custom consumers' matrix overloads (including shader-pack wrappers).
        if (out.getClass() != BufferBuilder.class) return out.addVertex(matrix, x, y, z);
        Vector3f position = matrix.transformPosition(x, y, z, POSITION.get());
        return out.addVertex(position.x(), position.y(), position.z());
    }

    /** Use Minecraft's complete-vertex writer for NEW_ENTITY instead of six attribute mask lookups. */
    public static void entity(VertexConsumer out, Matrix4f matrix, float x, float y, float z,
            int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        Vector3f position = matrix.transformPosition(x, y, z, POSITION.get());
        out.addVertex(position.x(), position.y(), position.z(), color, u, v, overlay, light, nx, ny, nz);
    }

    /** Complete-vertex writer with the exact original pose normal transform. */
    public static void entity(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
            int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        Vector3f scratch = POSITION.get();
        pose.pose().transformPosition(x, y, z, scratch);
        float px = scratch.x(), py = scratch.y(), pz = scratch.z();
        pose.transformNormal(nx, ny, nz, scratch);
        out.addVertex(px, py, pz, color, u, v, overlay, light, scratch.x(), scratch.y(), scratch.z());
    }

    public static VertexConsumer normal(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z) {
        if (out.getClass() != BufferBuilder.class) return out.setNormal(pose, x, y, z);
        Vector3f normal = pose.transformNormal(x, y, z, POSITION.get());
        return out.setNormal(normal.x(), normal.y(), normal.z());
    }
}
