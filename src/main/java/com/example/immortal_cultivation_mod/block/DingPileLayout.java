package com.example.immortal_cultivation_mod.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Spreads native-sized material in four directions, allowing overlap instead of tall overflow. */
public final class DingPileLayout {
    private static final double GAP = .008;
    private final double floorRadius;
    private final double rimRadius;
    private final double floor;
    private final double rim;
    private final DingBasinVolume volume;
    private final List<AABB> placed = new ArrayList<>();

    public DingPileLayout(double floorRadius, double rimRadius, double floor, double rim) {
        this.floorRadius = floorRadius;
        this.rimRadius = rimRadius;
        this.floor = floor;
        this.rim = rim;
        volume = new DingBasinVolume(floorRadius, rimRadius, floor, rim);
    }

    public static DingPileLayout forModel(String name, int scale) {
        boolean iron = "cu_tie_xiaoding".equals(name);
        boolean sanyang = "chi_jin_sanyang_ding".equals(name);
        double floor = (iron ? .42 : sanyang ? .63 : .65) * scale;
        double fullRim = (iron ? 1.05 : sanyang ? 1.73 : 1.33) * scale;
        // Stay well below the lip and inside the authored walls at every height.
        return new DingPileLayout((iron ? .51 : sanyang ? .72 : .58) * scale,
                (iron ? .51 : sanyang ? .72 : .684) * scale, floor, floor + (fullRim - floor) * .8);
    }

    public static int visibleCopies(int count) {
        return Math.clamp(count, 0, 5);
    }

    public Vec3 place(AABB modelBounds, long seed) {
        RandomSource random = RandomSource.create(seed);
        double footprint = Math.hypot(modelBounds.getXsize(), modelBounds.getZsize()) / 2;
        double maximumBottom = Math.max(floor, rim - modelBounds.getYsize() - GAP);
        double layer = Math.min((rim - floor) * .22,
                (placed.size() / 12) * Math.min(modelBounds.getYsize() * .2, .05));
        double bottom = Math.min(floor + layer, maximumBottom);
        double radius = Math.min(radiusAt(bottom), radiusAt(Math.min(rim, bottom + modelBounds.getYsize())));
        double available = Math.max(0, radius * Math.cos(Math.PI / 16) - footprint - GAP);
        double angle = (placed.size() % 4) * Math.PI / 2 + (random.nextDouble() - .5) * .7;
        double distance = available * (.35 + random.nextDouble() * .6);
        Vec3 offset = new Vec3(Math.cos(angle) * distance - modelBounds.getCenter().x,
                bottom - modelBounds.minY, Math.sin(angle) * distance - modelBounds.getCenter().z);
        placed.add(modelBounds.move(offset));
        return offset;
    }

    private double radiusAt(double height) {
        return floorRadius + (rimRadius - floorRadius) * Math.clamp((height - floor) / (rim - floor), 0, 1);
    }

    public DingBasinVolume volume() { return volume; }

    public AABB bounds() {
        AABB bounds = new AABB(0, floor, 0, 0, floor, 0);
        for (AABB item : placed) bounds = bounds.minmax(item.intersect(volume.bounds()));
        return bounds;
    }
}
