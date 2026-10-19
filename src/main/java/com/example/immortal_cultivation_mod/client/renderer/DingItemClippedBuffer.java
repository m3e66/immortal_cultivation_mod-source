package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.DingBasinVolume;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Crops item faces at the interior walls, preserving native transforms and interpolated textures. */
final class DingItemClippedBuffer implements MultiBufferSource, AutoCloseable {
    private final MultiBufferSource parent;
    private final Matrix4f toBasin;
    private final DingBasinVolume volume;
    private final List<CroppedConsumer> consumers = new ArrayList<>();

    DingItemClippedBuffer(MultiBufferSource parent, Matrix4f toBasin, DingBasinVolume volume) {
        this.parent = parent;
        this.toBasin = toBasin;
        this.volume = volume;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        // Complete pending faces before a buffer switch can end the parent's current batch.
        for (CroppedConsumer consumer : consumers) consumer.flush();
        CroppedConsumer consumer = new CroppedConsumer(parent.getBuffer(type), type.mode());
        consumers.add(consumer);
        return consumer;
    }

    @Override
    public void close() {
        for (CroppedConsumer consumer : consumers) consumer.flush();
    }

    private static final class Vertex {
        float x, y, z, u, v, nx, ny, nz;
        int red = 255, green = 255, blue = 255, alpha = 255;
        int overlayU, overlayV, lightU, lightV;
        Vec3 basin = Vec3.ZERO;

        static Vertex interpolate(Vertex a, Vertex b, double fraction) {
            float t = (float) fraction;
            Vertex result = new Vertex();
            result.x = lerp(a.x, b.x, t); result.y = lerp(a.y, b.y, t); result.z = lerp(a.z, b.z, t);
            result.u = lerp(a.u, b.u, t); result.v = lerp(a.v, b.v, t);
            result.nx = lerp(a.nx, b.nx, t); result.ny = lerp(a.ny, b.ny, t); result.nz = lerp(a.nz, b.nz, t);
            result.red = Math.round(lerp(a.red, b.red, t)); result.green = Math.round(lerp(a.green, b.green, t));
            result.blue = Math.round(lerp(a.blue, b.blue, t)); result.alpha = Math.round(lerp(a.alpha, b.alpha, t));
            result.overlayU = a.overlayU; result.overlayV = a.overlayV;
            result.lightU = Math.round(lerp(a.lightU, b.lightU, t));
            result.lightV = Math.round(lerp(a.lightV, b.lightV, t));
            result.basin = a.basin.lerp(b.basin, fraction);
            return result;
        }

        private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

        void emit(VertexConsumer target) {
            target.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v)
                    .setUv1(overlayU, overlayV).setUv2(lightU, lightV).setNormal(nx, ny, nz);
        }
    }

    private final class CroppedConsumer implements VertexConsumer {
        private final VertexConsumer target;
        private final boolean quads;
        private final int primitiveSize;
        private final List<Vertex> face = new ArrayList<>(4);
        private Vertex current;

        CroppedConsumer(VertexConsumer target, VertexFormat.Mode mode) {
            this.target = target;
            quads = mode == VertexFormat.Mode.QUADS;
            primitiveSize = quads ? 4 : mode == VertexFormat.Mode.TRIANGLES ? 3 : 0;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (face.size() == primitiveSize) flush();
            current = new Vertex();
            current.x = x; current.y = y; current.z = z;
            Vector3f local = toBasin.transformPosition(new Vector3f(x, y, z));
            current.basin = new Vec3(local.x, local.y, local.z);
            if (primitiveSize != 0) face.add(current);
            return this;
        }

        void flush() {
            if (primitiveSize == 0 || face.isEmpty()) return;
            if (face.size() == primitiveSize) {
                List<Vertex> cropped = volume.clip(face, vertex -> vertex.basin, Vertex::interpolate);
                if (cropped == face) {
                    for (Vertex vertex : face) vertex.emit(target);
                } else {
                    for (int i = 1; i + 1 < cropped.size(); i++) {
                        cropped.getFirst().emit(target);
                        cropped.get(i).emit(target);
                        cropped.get(i + 1).emit(target);
                        // The item pipeline expects quads; repeat the last triangle vertex.
                        if (quads) cropped.get(i + 1).emit(target);
                    }
                }
            }
            face.clear();
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            current.red = r; current.green = g; current.blue = b; current.alpha = a; return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { current.u = u; current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { current.overlayU = u; current.overlayV = v; return this; }
        @Override public VertexConsumer setUv2(int u, int v) { current.lightU = u; current.lightV = v; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            current.nx = x; current.ny = y; current.nz = z; return this;
        }
    }
}
