package com.example.immortal_cultivation_mod.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Convex interior used to crop display geometry without resizing the actual item models. */
public final class DingBasinVolume {
    public record Plane(double x, double y, double z, double offset) {
        public double distance(Vec3 point) {
            return x * point.x + y * point.y + z * point.z + offset;
        }
    }

    @FunctionalInterface
    public interface Interpolator<T> {
        T interpolate(T first, T second, double fraction);
    }

    private final List<Plane> planes = new ArrayList<>();
    private final AABB bounds;

    public DingBasinVolume(double floorRadius, double rimRadius, double floor, double rim) {
        planes.add(new Plane(0, 1, 0, -floor));
        planes.add(new Plane(0, -1, 0, rim));
        double slope = (rimRadius - floorRadius) / (rim - floor);
        double inset = Math.cos(Math.PI / 16);
        for (int side = 0; side < 16; side++) {
            double angle = side * Math.PI / 8;
            planes.add(new Plane(-Math.cos(angle), slope * inset, -Math.sin(angle),
                    (floorRadius - slope * floor) * inset));
        }
        double radius = Math.max(floorRadius, rimRadius);
        bounds = new AABB(-radius, floor, -radius, radius, rim, radius);
    }

    public AABB bounds() { return bounds; }

    public boolean contains(Vec3 point) {
        for (Plane plane : planes) if (plane.distance(point) < -1e-6) return false;
        return true;
    }

    public boolean contains(AABB box) {
        for (int corner = 0; corner < 8; corner++) {
            if (!contains(new Vec3((corner & 1) == 0 ? box.minX : box.maxX,
                    (corner & 2) == 0 ? box.minY : box.maxY,
                    (corner & 4) == 0 ? box.minZ : box.maxZ))) return false;
        }
        return true;
    }

    public <T> List<T> clip(List<T> polygon, Function<T, Vec3> position, Interpolator<T> interpolator) {
        for (Plane plane : planes) {
            if (polygon.isEmpty()) break;
            boolean allInside = true;
            boolean allOutside = true;
            for (T vertex : polygon) {
                boolean inside = plane.distance(position.apply(vertex)) >= 0;
                allInside &= inside;
                allOutside &= !inside;
            }
            if (allOutside) return List.of();
            if (allInside) continue;
            List<T> cropped = new ArrayList<>(polygon.size() + 2);
            T previous = polygon.getLast();
            double before = plane.distance(position.apply(previous));
            for (T current : polygon) {
                double after = plane.distance(position.apply(current));
                if ((before >= 0) != (after >= 0)) {
                    cropped.add(interpolator.interpolate(previous, current, before / (before - after)));
                }
                if (after >= 0) cropped.add(current);
                previous = current;
                before = after;
            }
            polygon = cropped;
        }
        return polygon;
    }
}
