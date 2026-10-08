package com.example.immortal_cultivation_mod.entity;

import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** Shared palm silhouette and flight frame. Model front is -Z, fingers point +Y. */
public final class ZhenshanPalmShape {
    private ZhenshanPalmShape() {}
    public static final float MODEL_SCALE = 14.601F / 3.5F;
    public static final double UNITS_TO_BLOCKS = MODEL_SCALE / 16.0;
    // Native Blockbench origin XYZ and size XYZ; keep in sync with the structural model bones.
    public static final double[][] BOXES = {
        {-5, -12, -2, 10, 12, 4},
        {-8, -2, -2.5, 16, 10, 5},
        {-10, 5, -2.5, 20, 15, 5},
        {-9, 20, -2.2, 18, 2, 4.4},
        {-15, 1, -2.2, 7, 8, 4.4},
        {-18, 5, -2.1, 5, 7, 4.2},
        {-20, 10, -1.8, 4.5, 5, 3.6},
        {-10.0, 20, -2.1, 4.3, 8.6, 4.2},
        {-9.8, 28.8, -1.9, 3.9, 6.6, 3.8},
        {-9.6, 35.6, -1.7, 3.5, 4.4, 3.4},
        {-4.8, 21, -2.1, 4.6, 9.89, 4.2},
        {-4.6, 31.09, -1.9, 4.2, 7.62, 3.8},
        {-4.4, 38.91, -1.7, 3.8, 5.09, 3.4},
        {0.9, 21, -2.1, 4.3, 8.17, 4.2},
        {1.1, 29.37, -1.9, 3.9, 6.26, 3.8},
        {1.3, 35.83, -1.7, 3.5, 4.17, 3.4},
        {6.2, 19, -2.1, 3.8, 6.45, 4.2},
        {6.4, 25.65, -1.9, 3.4, 4.9, 3.8},
        {6.6, 30.75, -1.7, 3.0, 3.25, 3.4}
    };
    public static double[][] collisionPoints() {
        var points = new ArrayList<double[]>();
        for (double[] b : BOXES) {
            int nx = (int)Math.ceil(b[3]), ny = (int)Math.ceil(b[4]);
            for (int x = 0; x <= nx; x++) for (int y = 0; y <= ny; y++)
                points.add(new double[]{-(b[0] + b[3] * x / nx), b[1] + b[4] * y / ny});
        }
        return points.toArray(double[][]::new);
    }
    public static Vec3 right(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        return (right.lengthSqr() < .001 ? forward.cross(new Vec3(0, 0, 1)) : right).normalize();
    }
    public static Quaternionf rotation(Vec3 direction) {
        Vec3 forward = direction.lengthSqr() < .001 ? new Vec3(0, 0, 1) : direction.normalize();
        Vec3 right = right(forward);
        Vec3 up = right.cross(forward).normalize();
        // Recompute right to keep the frame orthogonal near vertical shots.
        right = forward.cross(up).normalize();
        return new Matrix3f((float)right.x, (float)right.y, (float)right.z,
                (float)up.x, (float)up.y, (float)up.z,
                (float)-forward.x, (float)-forward.y, (float)-forward.z)
                .getNormalizedRotation(new Quaternionf());
    }
}
