package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Builds tapered great-circle ribbons for accelerated celestial motion. */
final class CosmicTrailGeometry {
    private static final float EPSILON = 0.000001F;

    private CosmicTrailGeometry() {
    }

    static void addCurvedRibbon(
            BufferBuilder buffer, Matrix4f matrix,
            Vector3f tail, Vector3f head, float halfWidth,
            float red, float green, float blue, float alpha) {
        if (new Vector3f(head).sub(tail).lengthSquared() < EPSILON) {
            return;
        }

        float tailRadius = tail.length();
        float headRadius = head.length();
        if (tailRadius < EPSILON || headRadius < EPSILON) {
            return;
        }

        Vector3f tailDirection = new Vector3f(tail).div(tailRadius);
        Vector3f headDirection = new Vector3f(head).div(headRadius);
        float dot = Math.max(-1.0F, Math.min(1.0F, tailDirection.dot(headDirection)));
        float arcAngle = (float) Math.acos(dot);
        float sinArc = (float) Math.sin(arcAngle);
        int segments = Math.max(6, Math.min(18, 6 + (int) (arcAngle * 12.0F)));
        Vector3f[] points = new Vector3f[segments + 1];
        Vector3f[] widths = new Vector3f[segments + 1];

        for (int index = 0; index <= segments; index++) {
            float progress = index / (float) segments;
            Vector3f direction;
            if (Math.abs(sinArc) > 0.0001F) {
                float tailWeight = (float) Math.sin((1.0F - progress) * arcAngle) / sinArc;
                float headWeight = (float) Math.sin(progress * arcAngle) / sinArc;
                direction = new Vector3f(tailDirection).mul(tailWeight)
                        .fma(headWeight, headDirection)
                        .normalize();
            } else {
                direction = new Vector3f(tailDirection)
                        .lerp(headDirection, progress)
                        .normalize();
            }
            float radius = tailRadius + (headRadius - tailRadius) * progress;
            points[index] = direction.mul(radius);
        }

        for (int index = 0; index <= segments; index++) {
            Vector3f before = points[Math.max(0, index - 1)];
            Vector3f after = points[Math.min(segments, index + 1)];
            Vector3f tangent = new Vector3f(after).sub(before);
            Vector3f radial = new Vector3f(points[index]).normalize();
            Vector3f width = radial.cross(tangent, new Vector3f());
            if (width.lengthSquared() < EPSILON) {
                Vector3f reference = Math.abs(radial.y) > 0.88F
                        ? new Vector3f(1.0F, 0.0F, 0.0F)
                        : new Vector3f(0.0F, 1.0F, 0.0F);
                width = radial.cross(reference, new Vector3f());
            }
            float progress = index / (float) segments;
            float eased = progress * progress * (3.0F - 2.0F * progress);
            widths[index] = width.normalize().mul(halfWidth * (0.34F + eased * 0.66F));
        }

        for (int index = 0; index < segments; index++) {
            float startProgress = index / (float) segments;
            float endProgress = (index + 1) / (float) segments;
            float startFade = startProgress * startProgress
                    * (3.0F - 2.0F * startProgress);
            float endFade = endProgress * endProgress
                    * (3.0F - 2.0F * endProgress);
            Vector3f startLeft = new Vector3f(points[index]).sub(widths[index]);
            Vector3f startRight = new Vector3f(points[index]).add(widths[index]);
            Vector3f endRight = new Vector3f(points[index + 1]).add(widths[index + 1]);
            Vector3f endLeft = new Vector3f(points[index + 1]).sub(widths[index + 1]);
            float startU = startProgress * 0.5F;
            float endU = endProgress * 0.5F;

            buffer.addVertex(matrix, startLeft.x, startLeft.y, startLeft.z)
                    .setUv(startU, 1.0F).setColor(red, green, blue, alpha * startFade);
            buffer.addVertex(matrix, startRight.x, startRight.y, startRight.z)
                    .setUv(startU, 0.0F).setColor(red, green, blue, alpha * startFade);
            buffer.addVertex(matrix, endRight.x, endRight.y, endRight.z)
                    .setUv(endU, 0.0F).setColor(red, green, blue, alpha * endFade);
            buffer.addVertex(matrix, endLeft.x, endLeft.y, endLeft.z)
                    .setUv(endU, 1.0F).setColor(red, green, blue, alpha * endFade);
        }
    }
}
