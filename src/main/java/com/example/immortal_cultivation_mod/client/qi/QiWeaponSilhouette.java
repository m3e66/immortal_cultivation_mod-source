package com.example.immortal_cultivation_mod.client.qi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fits the flame to textured weapon surfaces instead of transparent item-card corners. */
public final class QiWeaponSilhouette {
    public record Fit(Vec3 start, Vec3 end, double width, double depth) {}

    private static final Map<List<BakedQuad>, Fit> CACHE = new LinkedHashMap<>(32, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<List<BakedQuad>, Fit> entry) {
            return size() > 128;
        }
    };

    private QiWeaponSilhouette() {}

    public static void clearCache() { CACHE.clear(); }

    public static Fit fit(List<BakedQuad> quads) {
        Fit cached = CACHE.get(quads);
        if (cached != null || CACHE.containsKey(quads)) return cached;
        List<Vec3> points = new ArrayList<>();
        for (BakedQuad quad : quads) visiblePoints(quad, points);
        Fit result = fitPoints(points);
        CACHE.put(List.copyOf(quads), result);
        return result;
    }

    private static void visiblePoints(BakedQuad quad, List<Vec3> points) {
        int[] data = quad.getVertices();
        int stride = data.length / 4;
        if (stride < 6) return;
        Vec3[] positions = new Vec3[4];
        double[] u = new double[4], v = new double[4];
        TextureAtlasSprite sprite = quad.getSprite();
        var contents = sprite.contents();
        double uSpan = sprite.getU1() - sprite.getU0(), vSpan = sprite.getV1() - sprite.getV0();
        if (uSpan <= 0 || vSpan <= 0) return;
        for (int i = 0; i < 4; i++) {
            positions[i] = new Vec3(Float.intBitsToFloat(data[i * stride]),
                    Float.intBitsToFloat(data[i * stride + 1]), Float.intBitsToFloat(data[i * stride + 2]));
            u[i] = (Float.intBitsToFloat(data[i * stride + 4]) - sprite.getU0()) / uSpan;
            v[i] = (Float.intBitsToFloat(data[i * stride + 5]) - sprite.getV0()) / vSpan;
        }
        int columns = steps(u[1] - u[0], v[1] - v[0], contents.width(), contents.height());
        int rows = steps(u[3] - u[0], v[3] - v[0], contents.width(), contents.height());
        int[] frames = contents.getUniqueFrames().toArray();
        for (int row = 0; row < rows; row++) {
            int run = -1;
            for (int column = 0; column <= columns; column++) {
                boolean visible = false;
                if (column < columns) {
                    double s = (column + .5) / columns, t = (row + .5) / rows;
                    int x = Math.clamp((int) Math.floor(interpolate(u, s, t) * contents.width()), 0, contents.width() - 1);
                    int y = Math.clamp((int) Math.floor(interpolate(v, s, t) * contents.height()), 0, contents.height() - 1);
                    for (int frame : frames) {
                        if (!contents.isTransparent(frame, x, y)) { visible = true; break; }
                    }
                }
                if (visible && run < 0) run = column;
                if (!visible && run >= 0) {
                    // Four corners per opaque run, never the full front/back card.
                    double s0 = run / (double) columns, s1 = column / (double) columns;
                    double t0 = row / (double) rows, t1 = (row + 1D) / rows;
                    points.add(interpolate(positions, s0, t0));
                    points.add(interpolate(positions, s1, t0));
                    points.add(interpolate(positions, s1, t1));
                    points.add(interpolate(positions, s0, t1));
                    run = -1;
                }
            }
        }
    }

    private static int steps(double du, double dv, int width, int height) {
        return Math.max(1, (int) Math.ceil(Math.max(Math.abs(du) * width, Math.abs(dv) * height)));
    }

    private static double interpolate(double[] values, double s, double t) {
        return values[0] * (1 - s) * (1 - t) + values[1] * s * (1 - t)
                + values[2] * s * t + values[3] * (1 - s) * t;
    }

    private static Vec3 interpolate(Vec3[] values, double s, double t) {
        return values[0].scale((1 - s) * (1 - t)).add(values[1].scale(s * (1 - t)))
                .add(values[2].scale(s * t)).add(values[3].scale((1 - s) * t));
    }

    private static Fit fitPoints(List<Vec3> points) {
        if (points.isEmpty()) return null;
        Vec3 center = Vec3.ZERO;
        for (Vec3 point : points) center = center.add(point);
        center = center.scale(1D / points.size());
        double xx = 0, xy = 0, xz = 0, yy = 0, yz = 0, zz = 0;
        for (Vec3 point : points) {
            Vec3 p = point.subtract(center);
            xx += p.x * p.x; xy += p.x * p.y; xz += p.x * p.z;
            yy += p.y * p.y; yz += p.y * p.z; zz += p.z * p.z;
        }
        Vec3 axis = yy >= xx && yy >= zz ? new Vec3(0, 1, 0)
                : xx >= zz ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        for (int i = 0; i < 32; i++) {
            Vec3 next = new Vec3(xx * axis.x + xy * axis.y + xz * axis.z,
                    xy * axis.x + yy * axis.y + yz * axis.z,
                    xz * axis.x + yz * axis.y + zz * axis.z);
            if (next.lengthSqr() < 1.0E-16) break;
            axis = next.normalize();
        }
        if (axis.y < -1.0E-6 || Math.abs(axis.y) < 1.0E-6 && axis.x < 0) axis = axis.scale(-1);
        Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0),
                new Vector3f((float) axis.x, (float) axis.y, (float) axis.z));
        Vec3 side = new Vec3(new Vector3f(1, 0, 0).rotate(rotation));
        Vec3 depthAxis = new Vec3(new Vector3f(0, 0, 1).rotate(rotation));
        double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
        double minSide = low, maxSide = high, minDepth = low, maxDepth = high;
        for (Vec3 point : points) {
            double along = point.dot(axis), across = point.dot(side), deep = point.dot(depthAxis);
            low = Math.min(low, along); high = Math.max(high, along);
            minSide = Math.min(minSide, across); maxSide = Math.max(maxSide, across);
            minDepth = Math.min(minDepth, deep); maxDepth = Math.max(maxDepth, deep);
        }
        if (high - low < .005) return null;
        Vec3 offset = side.scale((minSide + maxSide) * .5).add(depthAxis.scale((minDepth + maxDepth) * .5));
        return new Fit(offset.add(axis.scale(low)), offset.add(axis.scale(high)),
                (maxSide - minSide) * .5, (maxDepth - minDepth) * .5);
    }
}
