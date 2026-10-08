package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.HotelNpcEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Spawns and binds one persistent innkeeper to each generated hotel. */
public final class HotelStructureManager {
    private static final ResourceLocation HOTEL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "hotel1");

    /* The saved hotel is 40 x 20 x 42; this is beside its front counter. */
    private static final BlockPos KEEPER_LOCAL_POS = new BlockPos(30, 3, 12);
    private static final int TEMPLATE_SIZE_X = 40;
    private static final int TEMPLATE_SIZE_Z = 42;

    private HotelStructureManager() {
    }

    public static void ensureHotelkeeperNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        StructureStart start = findHotel(level, player.blockPosition());
        if (start != null) {
            ensureHotelkeeper(level, start);
        }
    }

    private static void ensureHotelkeeper(
            ServerLevel level, StructureStart start) {
        String key = instanceKey(level, start);
        KeeperStateData keeperState = KeeperStateData.get(level);
        BoundingBox bounds = start.getBoundingBox();
        AABB searchArea = new AABB(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0D, bounds.maxY() + 1.0D,
                bounds.maxZ() + 1.0D).inflate(2.0D);

        for (HotelNpcEntity keeper : level.getEntitiesOfClass(
                HotelNpcEntity.class, searchArea, HotelNpcEntity::isAlive)) {
            if (keeper.isBoundToHotel(key) || !keeper.isHotelBound()) {
                KeeperPlacement placement = keeperPlacement(start);
                if (placement != null) {
                    keeper.bindToHotel(key, bounds, placement.position());
                }
                keeperState.markCreated(key);
                return;
            }
        }

        // A generated hotel receives exactly one keeper. If that keeper has
        // died, been removed, or is temporarily unloaded, never create a
        // replacement.
        if (keeperState.wasCreated(key)) {
            return;
        }

        KeeperPlacement placement = keeperPlacement(start);
        if (placement == null || !level.hasChunkAt(placement.position())) {
            return;
        }
        HotelNpcEntity keeper = ModEntities.HOTEL_NPC.get().create(level);
        if (keeper == null) {
            return;
        }
        BlockPos spawn = findOpenSpawn(level, keeper, placement, bounds);
        keeper.bindToHotel(key, bounds, spawn);
        keeper.moveTo(
                spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                placement.yaw(), 0.0F);
        if (level.addFreshEntity(keeper)) {
            keeperState.markCreated(key);
        }
    }

    @Nullable
    private static KeeperPlacement keeperPlacement(StructureStart start) {
        PoolElementStructurePiece poolPiece = null;
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece candidate) {
                poolPiece = candidate;
                break;
            }
        }
        if (poolPiece == null) {
            return null;
        }

        Rotation rotation = poolPiece.getRotation();
        BoundingBox bounds = poolPiece.getBoundingBox();
        int offsetX;
        int offsetZ;
        float yaw;
        switch (rotation) {
            case CLOCKWISE_90 -> {
                offsetX = TEMPLATE_SIZE_Z - 1 - KEEPER_LOCAL_POS.getZ();
                offsetZ = KEEPER_LOCAL_POS.getX();
                yaw = 90.0F;
            }
            case CLOCKWISE_180 -> {
                offsetX = TEMPLATE_SIZE_X - 1 - KEEPER_LOCAL_POS.getX();
                offsetZ = TEMPLATE_SIZE_Z - 1 - KEEPER_LOCAL_POS.getZ();
                yaw = 180.0F;
            }
            case COUNTERCLOCKWISE_90 -> {
                offsetX = KEEPER_LOCAL_POS.getZ();
                offsetZ = TEMPLATE_SIZE_X - 1 - KEEPER_LOCAL_POS.getX();
                yaw = 270.0F;
            }
            default -> {
                offsetX = KEEPER_LOCAL_POS.getX();
                offsetZ = KEEPER_LOCAL_POS.getZ();
                yaw = 0.0F;
            }
        }
        return new KeeperPlacement(new BlockPos(
                bounds.minX() + offsetX,
                bounds.minY() + KEEPER_LOCAL_POS.getY(),
                bounds.minZ() + offsetZ), yaw);
    }

    private static BlockPos findOpenSpawn(
            ServerLevel level, HotelNpcEntity keeper,
            KeeperPlacement placement, BoundingBox bounds) {
        BlockPos expected = placement.position();
        for (int rise = 0; rise <= 3; rise++) {
            BlockPos candidate = expected.above(rise);
            keeper.moveTo(
                    candidate.getX() + 0.5D, candidate.getY(),
                    candidate.getZ() + 0.5D, placement.yaw(), 0.0F);
            if (bounds.isInside(candidate) && level.noCollision(keeper)) {
                return candidate;
            }
        }
        return expected;
    }

    @Nullable
    private static StructureStart findHotel(ServerLevel level, BlockPos pos) {
        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(HOTEL_ID);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(pos, structure);
        return start.isValid() ? start : null;
    }

    private static String instanceKey(
            ServerLevel level, StructureStart start) {
        BoundingBox bounds = start.getBoundingBox();
        return level.dimension().location() + "|"
                + bounds.minX() + "," + bounds.minY() + "," + bounds.minZ()
                + "," + bounds.maxX() + "," + bounds.maxY() + ","
                + bounds.maxZ();
    }

    private record KeeperPlacement(BlockPos position, float yaw) {
    }

    /** Persistent one-keeper history for every generated hotel. */
    private static final class KeeperStateData extends SavedData {
        private static final String DATA_NAME =
                "immortal_cultivation_hotel_keepers";
        private static final SavedData.Factory<KeeperStateData> FACTORY =
                new SavedData.Factory<>(KeeperStateData::new,
                        KeeperStateData::load);

        private final Set<String> createdHotels = new HashSet<>();

        private static KeeperStateData get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(FACTORY, DATA_NAME);
        }

        private static KeeperStateData load(
                CompoundTag tag, HolderLookup.Provider registries) {
            KeeperStateData data = new KeeperStateData();
            CompoundTag createdTag = tag.getCompound("CreatedHotels");
            for (String key : createdTag.getAllKeys()) {
                if (createdTag.getBoolean(key)) {
                    data.createdHotels.add(key);
                }
            }
            return data;
        }

        private boolean wasCreated(String key) {
            return createdHotels.contains(key);
        }

        private void markCreated(String key) {
            if (createdHotels.add(key)) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(
                CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag createdTag = new CompoundTag();
            createdHotels.forEach(key -> createdTag.putBoolean(key, true));
            tag.put("CreatedHotels", createdTag);
            return tag;
        }
    }
}
