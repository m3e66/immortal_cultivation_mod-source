package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** World-space wake. Old samples stay where they were emitted, including through turns and stops. */
public final class CultivationFlightTrail {
    public static final int MAX_POINTS = 42;
    public static final int LIFETIME = 40;
    public static final double MAX_LENGTH = 42;
    public static final double MAX_STEP = 10;
    private record Sample(Vec3 position, long tick) {}
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private long lastMovement = Long.MIN_VALUE;

    public void sample(Vec3 position, long tick) {
        if (!finite(position)) { clear(); return; }
        Sample previous = samples.peekFirst();
        if (previous != null) {
            if (tick < previous.tick || position.distanceToSqr(previous.position) > MAX_STEP * MAX_STEP) clear();
            else if (tick == previous.tick || position.distanceToSqr(previous.position) < .0025) { prune(tick); return; }
        }
        samples.addFirst(new Sample(position, tick));
        lastMovement = tick;
        prune(tick);
    }

    public void prune(long tick) {
        while (!samples.isEmpty() && (samples.size() > MAX_POINTS || tick - samples.peekLast().tick > LIFETIME)) samples.removeLast();
        while (samples.size() > 2 && length() > MAX_LENGTH) samples.removeLast();
    }

    public List<Vec3> points(Vec3 interpolatedHead) {
        List<Vec3> result = new ArrayList<>(samples.size() + 1);
        if (samples.isEmpty()) return result;
        boolean replaceHead = connectedTo(interpolatedHead);
        boolean first = true;
        for (Sample sample : samples) {
            Vec3 point = first && replaceHead ? interpolatedHead : sample.position;
            if (result.isEmpty() || result.getLast().distanceToSqr(point) > .0001) result.add(point);
            first = false;
        }
        return result;
    }

    /** A teleport cannot reuse the old wake while waiting for the next client sample. */
    public boolean connectedTo(Vec3 position) {
        Sample newest = samples.peekFirst();
        return newest != null && finite(position) && position.distanceToSqr(newest.position) <= MAX_STEP * MAX_STEP;
    }

    /** Matches points() without allocating a list during player visibility checks. */
    public boolean hasVisibleSegment(Vec3 interpolatedHead) {
        var iterator = samples.iterator();
        if (!iterator.hasNext()) return false;
        Vec3 head = iterator.next().position;
        if (connectedTo(interpolatedHead)) head = interpolatedHead;
        while (iterator.hasNext()) {
            if (head.distanceToSqr(iterator.next().position) > .0001) return true;
        }
        return false;
    }

    public double length() {
        double length = 0;
        Sample previous = null;
        for (Sample sample : samples) {
            if (previous != null) length += previous.position.distanceTo(sample.position);
            previous = sample;
        }
        return length;
    }

    public float opacity(double tick) {
        return samples.size() < 2 || lastMovement == Long.MIN_VALUE ? 0 : (float) Mth.clamp(1 - (tick - lastMovement) / 16, 0, 1);
    }
    public int size() { return samples.size(); }
    public void clear() { samples.clear(); lastMovement = Long.MIN_VALUE; }
    public static boolean finite(Vec3 value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    /** Parallel transport avoids a sudden ribbon flip when the flight path becomes vertical. */
    public static Vec3 side(Vec3 tangent, Vec3 previous) {
        Vec3 forward = finite(tangent) && tangent.lengthSqr() > .00001 ? tangent.normalize() : new Vec3(0, 0, 1);
        Vec3 side = previous == null ? Vec3.ZERO : previous.subtract(forward.scale(previous.dot(forward)));
        if (side.lengthSqr() < .0001) side = forward.cross(Math.abs(forward.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0));
        return side.normalize();
    }

    public static int color(String element) {
        if (SpiritRoots.elementMatches(element, SpiritRoots.ICE) || SpiritRoots.elementMatches(element, SpiritRoots.WATER)) return 0x91E9FF;
        if (SpiritRoots.elementMatches(element, SpiritRoots.THUNDER) || SpiritRoots.elementMatches(element, SpiritRoots.DARK)) return 0xD898FF;
        if (SpiritRoots.elementMatches(element, SpiritRoots.FIRE)) return 0xFFAD70;
        if (SpiritRoots.elementMatches(element, SpiritRoots.EARTH)) return 0xFFE19A;
        if (SpiritRoots.elementMatches(element, SpiritRoots.METAL) || SpiritRoots.elementMatches(element, SpiritRoots.LIGHT)) return 0xEFFFF0;
        return 0x8AFF77;
    }
}
