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
    // Normalized imported hand cube bounds (including bone rotations), origin XYZ and size XYZ.
    public static final double[][] BOXES = {
        {-5.55905, -12.00000, -2.43215, 23.71351, 7.29646, 7.29646},
        {-8.59924, -6.80616, -2.49597, 25.53762, 17.32714, 8.03214},
        {10.90745, -5.55330, -2.51777, 8.40932, 19.17905, 8.11300},
        {-16.28453, -9.03752, -4.58027, 15.43499, 15.76596, 9.98981},
        {-20.37930, 2.63512, -3.04019, 9.91689, 9.79925, 7.29646},
        {-21.67848, -6.81824, -4.34501, 8.58962, 13.44739, 8.58562},
        {-8.59924, 9.56680, -1.09621, 29.18585, 12.75043, 8.32858},
        {-10.01623, 18.30315, -1.23175, 9.13894, 9.98112, 9.24474},
        {-8.69903, 24.39070, -1.20752, 10.15627, 12.98435, 10.76232},
        {-6.62717, 30.89477, 2.48532, 8.33708, 11.93099, 11.02687},
        {-1.19629, 19.46553, -1.23413, 7.66989, 9.96532, 9.04996},
        {-1.54761, 26.87129, -0.68538, 8.15228, 11.73370, 10.27710},
        {-0.84540, 31.32644, 4.99463, 6.83625, 12.67356, 12.28481},
        {14.68015, 16.72645, -0.76606, 6.99833, 8.04054, 8.51471},
        {13.49915, 21.68460, -0.90630, 7.46813, 8.76304, 8.97765},
        {13.95231, 23.63422, 1.82789, 6.29141, 10.18344, 9.83883},
        {7.11381, 18.55860, -1.13216, 7.99524, 9.31662, 9.10355},
        {5.88634, 24.70213, -0.89737, 8.69315, 12.33458, 10.60234},
        {6.62248, 29.23828, 4.58382, 7.04503, 12.03935, 11.51762}
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
