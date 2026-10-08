package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/** Mortal innkeeper with a fixed appearance and hotel-food trading stock. */
public class HotelNpcEntity extends FanrenNpcEntity {
    private static final String HOTEL_BOUND_TAG = "HotelStructureBound";
    private static final String HOTEL_KEY_TAG = "HotelStructureKey";

    private String hotelInstanceKey = "";
    private BlockPos hotelHome = BlockPos.ZERO;
    private int hotelMinX;
    private int hotelMinY;
    private int hotelMinZ;
    private int hotelMaxX;
    private int hotelMaxY;
    private int hotelMaxZ;
    private boolean hotelBound;

    public HotelNpcEntity(
            EntityType<? extends HotelNpcEntity> entityType,
            Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            NpcProfile.ensureHotelkeeper(this);
            maintainHotelBoundary();
        }
    }

    public void bindToHotel(
            String instanceKey, BoundingBox bounds, BlockPos home) {
        hotelInstanceKey = instanceKey == null ? "" : instanceKey;
        hotelHome = home.immutable();
        hotelMinX = bounds.minX();
        hotelMinY = bounds.minY();
        hotelMinZ = bounds.minZ();
        hotelMaxX = bounds.maxX();
        hotelMaxY = bounds.maxY();
        hotelMaxZ = bounds.maxZ();
        hotelBound = true;
        applyHotelRestriction();
        setPersistenceRequired();
    }

    public boolean isHotelBound() {
        return hotelBound;
    }

    public boolean isBoundToHotel(String instanceKey) {
        return hotelBound && hotelInstanceKey.equals(instanceKey);
    }

    private void applyHotelRestriction() {
        if (!hotelBound) {
            return;
        }
        int width = hotelMaxX - hotelMinX + 1;
        int depth = hotelMaxZ - hotelMinZ + 1;
        int radius = Math.max(2, Math.min(width, depth) / 2 - 2);
        restrictTo(new BlockPos(
                (hotelMinX + hotelMaxX) / 2,
                hotelHome.getY(),
                (hotelMinZ + hotelMaxZ) / 2), radius);
    }

    private void maintainHotelBoundary() {
        if (!hotelBound) {
            return;
        }
        if (!hasRestriction()) {
            applyHotelRestriction();
        }

        BlockPos target = getNavigation().getTargetPos();
        if (target != null && !insideHotel(target, 1)) {
            getNavigation().stop();
            getNavigation().moveTo(
                    hotelHome.getX() + 0.5D,
                    hotelHome.getY(),
                    hotelHome.getZ() + 0.5D,
                    0.8D);
        }

        if (!insideHotel(blockPosition(), 0)
                || getY() < hotelMinY - 1.0D
                || getY() > hotelMaxY + 1.0D) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            teleportTo(
                    Mth.clamp(hotelHome.getX() + 0.5D,
                            hotelMinX + 0.5D, hotelMaxX + 0.5D),
                    hotelHome.getY(),
                    Mth.clamp(hotelHome.getZ() + 0.5D,
                            hotelMinZ + 0.5D, hotelMaxZ + 0.5D));
        }
    }

    private boolean insideHotel(BlockPos pos, int inset) {
        int minX = Math.min(hotelMaxX, hotelMinX + inset);
        int minZ = Math.min(hotelMaxZ, hotelMinZ + inset);
        int maxX = Math.max(hotelMinX, hotelMaxX - inset);
        int maxZ = Math.max(hotelMinZ, hotelMaxZ - inset);
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!hotelBound) {
            return;
        }
        tag.putBoolean(HOTEL_BOUND_TAG, true);
        tag.putString(HOTEL_KEY_TAG, hotelInstanceKey);
        tag.putLong("HotelHome", hotelHome.asLong());
        tag.putInt("HotelMinX", hotelMinX);
        tag.putInt("HotelMinY", hotelMinY);
        tag.putInt("HotelMinZ", hotelMinZ);
        tag.putInt("HotelMaxX", hotelMaxX);
        tag.putInt("HotelMaxY", hotelMaxY);
        tag.putInt("HotelMaxZ", hotelMaxZ);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hotelBound = tag.getBoolean(HOTEL_BOUND_TAG);
        if (!hotelBound) {
            return;
        }
        hotelInstanceKey = tag.getString(HOTEL_KEY_TAG);
        hotelHome = BlockPos.of(tag.getLong("HotelHome"));
        hotelMinX = tag.getInt("HotelMinX");
        hotelMinY = tag.getInt("HotelMinY");
        hotelMinZ = tag.getInt("HotelMinZ");
        hotelMaxX = tag.getInt("HotelMaxX");
        hotelMaxY = tag.getInt("HotelMaxY");
        hotelMaxZ = tag.getInt("HotelMaxZ");
        applyHotelRestriction();
    }
}
