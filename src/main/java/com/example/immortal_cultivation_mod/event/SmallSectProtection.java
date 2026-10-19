package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/** Links each generated small sect to its own vault guardian and mining lock. */
public final class SmallSectProtection {
    private static final ResourceLocation SMALL_SECT_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "small_sect1");
    private static final String MESSAGE_KEY =
            "message.immortal_cultivation_mod.small_sect_guarded";
    /** Entity position saved in the 88 x 33 x 96 small_sect1 template. */
    private static final BlockPos GUARDIAN_LOCAL_POS =
            new BlockPos(44, 5, 60);
    private static final int TEMPLATE_SIZE_X = 88;
    private static final int TEMPLATE_SIZE_Z = 96;

    private SmallSectProtection() {
    }

    /**
     * Registers a structure-spawned guardian after both its entity and the
     * structure start are available to the server.
     *
     * @return {@code true} once the guardian has been linked to a small sect
     */
    public static boolean registerGuardian(
            ServerLevel level, VaultGuardianEntity guardian) {
        if (!guardian.isAlive() || !guardian.isSmallSectGuardian()) {
            return false;
        }
        String savedKey = guardian.getSmallSectInstanceKey();
        if (!savedKey.isEmpty()) {
            GuardianStateData.get(level).markLivingGuardian(savedKey);
            return true;
        }
        StructureStart start = findSmallSect(level, guardian.blockPosition());
        if (start == null) {
            return false;
        }
        String key = instanceKey(level, start);
        guardian.setSmallSectInstanceKey(key);
        GuardianStateData.get(level).markLivingGuardian(key);
        return true;
    }

    /** Keeps the one guardian assigned to a generated sect at its vault. */
    public static void ensureGuardianNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        StructureStart start = findSmallSect(level, player.blockPosition());
        if (start != null) {
            ensureGuardian(level, start);
        }
    }

    private static boolean ensureGuardian(
            ServerLevel level, StructureStart start) {
        String key = instanceKey(level, start);
        GuardianStateData data = GuardianStateData.get(level);
        for (VaultGuardianEntity guardian : linkedGuardians(level, start)) {
            if (!guardian.isAlive()) continue;
            if (guardian.getSmallSectInstanceKey().isEmpty()) {
                guardian.setSmallSectInstanceKey(key);
            }
            if (!guardianArea(start).contains(guardian.position())) {
                guardian.returnToVaultHome();
            }
            data.markLivingGuardian(key);
            return true;
        }

        if (data.isDefeated(key)) {
            return false;
        }

        // Once a guardian has existed for this structure, never manufacture a
        // replacement. A linked guardian that leaves the vault is found by its
        // instance key above and teleported home instead.
        if (data.isActive(key)) {
            return false;
        }

        // One-time initialization for templates whose saved entity failed to
        // place. markActive prevents this path from ever becoming a respawn.
        GuardianPlacement placement = guardianPlacement(start);
        if (placement == null || !level.hasChunkAt(placement.position())) {
            return false;
        }
        VaultGuardianEntity guardian =
                ModEntities.VAULT_GUARDIAN.get().create(level);
        if (guardian == null) {
            return false;
        }
        guardian.configureAsSmallSectGuardian(key);
        BlockPos spawn = findOpenSpawn(level, guardian, placement);
        guardian.moveTo(spawn.getX() + 0.5D, spawn.getY(),
                spawn.getZ() + 0.5D, placement.yaw(), 0.0F);
        if (!level.addFreshEntity(guardian)) {
            return false;
        }
        data.markActive(key);
        return true;
    }

    @Nullable
    private static GuardianPlacement guardianPlacement(StructureStart start) {
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
                offsetX = TEMPLATE_SIZE_Z - 1 - GUARDIAN_LOCAL_POS.getZ();
                offsetZ = GUARDIAN_LOCAL_POS.getX();
                yaw = 90.0F;
            }
            case CLOCKWISE_180 -> {
                offsetX = TEMPLATE_SIZE_X - 1 - GUARDIAN_LOCAL_POS.getX();
                offsetZ = TEMPLATE_SIZE_Z - 1 - GUARDIAN_LOCAL_POS.getZ();
                yaw = 180.0F;
            }
            case COUNTERCLOCKWISE_90 -> {
                offsetX = GUARDIAN_LOCAL_POS.getZ();
                offsetZ = TEMPLATE_SIZE_X - 1 - GUARDIAN_LOCAL_POS.getX();
                yaw = 270.0F;
            }
            default -> {
                offsetX = GUARDIAN_LOCAL_POS.getX();
                offsetZ = GUARDIAN_LOCAL_POS.getZ();
                yaw = 0.0F;
            }
        }
        return new GuardianPlacement(new BlockPos(
                bounds.minX() + offsetX,
                bounds.minY() + GUARDIAN_LOCAL_POS.getY(),
                bounds.minZ() + offsetZ), yaw);
    }

    private static BlockPos findOpenSpawn(
            ServerLevel level, VaultGuardianEntity guardian,
            GuardianPlacement placement) {
        BlockPos expected = placement.position();
        for (int rise = 0; rise <= 4; rise++) {
            BlockPos candidate = expected.above(rise);
            guardian.moveTo(candidate.getX() + 0.5D, candidate.getY(),
                    candidate.getZ() + 0.5D, placement.yaw(), 0.0F);
            if (level.noCollision(guardian)) {
                return candidate;
            }
        }
        // Newly generated templates clear this exact location with their saved
        // air. Keeping the intended position is safer than placing the vault
        // guardian outside its protected room in a damaged legacy structure.
        return expected;
    }

    /** Permanently releases this generated sect when its linked guardian dies. */
    public static void guardianDefeated(
            ServerLevel level, VaultGuardianEntity guardian) {
        String savedKey = guardian.getSmallSectInstanceKey();
        // Legacy worlds can contain both a template guardian and a fallback.
        // The first death must not unlock a vault whose other guardian lives.
        if (!savedKey.isEmpty()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof VaultGuardianEntity other && other != guardian
                        && other.isAlive() && other.isSmallSectGuardian()
                        && savedKey.equals(other.getSmallSectInstanceKey())) {
                    GuardianStateData.get(level).markLivingGuardian(savedKey);
                    return;
                }
            }
        }
        if (!savedKey.isEmpty()) {
            GuardianStateData.get(level).markDefeated(savedKey);
            return;
        }
        StructureStart start = findSmallSect(level, guardian.blockPosition());
        if (start != null) {
            GuardianStateData.get(level).markDefeated(instanceKey(level, start));
        }
    }

    /** Returns whether the generated small-sect instance containing this block is locked. */
    public static boolean isLocked(ServerLevel level, BlockPos pos) {
        StructureStart start = findSmallSect(level, pos);
        return start != null
                && !isPlacedAfterGeneration(level, start, pos)
                && isLocked(level, start);
    }

    /**
     * Exempts a block added after structure generation from the guardian lock.
     * The original template blocks remain protected, while player/mob placed
     * blocks can be removed normally.
     */
    public static void recordPlacedBlock(ServerLevel level, BlockPos pos) {
        StructureStart start = findSmallSect(level, pos);
        if (start == null || level.getBlockState(pos).isAir()
                || !isLocked(level, start)) {
            return;
        }
        String key = instanceKey(level, start);
        GuardianStateData.get(level).markPlaced(key, pos);
    }

    /**
     * Cancels mining inside a guarded small sect, wakes its guardian, and
     * gives the player a concise reason.
     */
    public static boolean preventBreaking(
            ServerLevel level, BlockPos pos, ServerPlayer player) {
        StructureStart start = findSmallSect(level, pos);
        if (start == null || isPlacedAfterGeneration(level, start, pos)
                || !isLocked(level, start)) {
            return false;
        }
        awakenLinkedGuardian(level, start, player);
        player.displayClientMessage(Component.translatable(MESSAGE_KEY), true);
        return true;
    }

    /** Wakes the guardian tied to the small sect containing an interacted block. */
    public static void awakenForInteraction(
            ServerLevel level, BlockPos pos, ServerPlayer player) {
        StructureStart start = findSmallSect(level, pos);
        if (start != null && isLocked(level, start)) {
            awakenLinkedGuardian(level, start, player);
        }
    }

    private static boolean isLocked(ServerLevel level, StructureStart start) {
        String key = instanceKey(level, start);
        GuardianStateData data = GuardianStateData.get(level);
        if (data.isDefeated(key)) {
            return false;
        }
        if (data.isActive(key)) {
            return true;
        }
        ensureGuardian(level, start);
        // Generation/loading is not proof of defeat. In particular, a slash
        // can reach the structure before the guardian's chunk/entity loads.
        return true;
    }

    private static boolean isPlacedAfterGeneration(
            ServerLevel level, StructureStart start, BlockPos pos) {
        return GuardianStateData.get(level).isPlaced(
                instanceKey(level, start), pos);
    }

    private static void awakenLinkedGuardian(
            ServerLevel level, StructureStart start, ServerPlayer player) {
        for (VaultGuardianEntity guardian : linkedGuardians(level, start)) {
            if (guardian.isAlive()) {
                guardian.activateAgainst(player);
            }
        }
    }

    private static List<VaultGuardianEntity> linkedGuardians(
            ServerLevel level, StructureStart start) {
        String key = instanceKey(level, start);
        AABB area = guardianArea(start);
        List<VaultGuardianEntity> guardians = new ArrayList<>(
                level.getEntitiesOfClass(
                        VaultGuardianEntity.class, area,
                        guardian -> guardian.isAlive()
                                && guardian.isSmallSectGuardian()
                                && (guardian.getSmallSectInstanceKey().isEmpty()
                                || key.equals(guardian.getSmallSectInstanceKey()))));
        if (!guardians.isEmpty()) {
            return guardians;
        }

        // A guardian that crossed the old local search boundary still exists;
        // locate it by the persistent structure key instead of spawning again.
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof VaultGuardianEntity guardian)
                    || !guardian.isAlive()
                    || !guardian.isSmallSectGuardian()) {
                continue;
            }
            String guardianKey = guardian.getSmallSectInstanceKey();
            if (key.equals(guardianKey)) {
                guardians.add(guardian);
            }
        }
        return guardians;
    }

    private static AABB guardianArea(StructureStart start) {
        BoundingBox bounds = start.getBoundingBox();
        return new AABB(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0D, bounds.maxY() + 1.0D,
                bounds.maxZ() + 1.0D).inflate(2.0D);
    }

    @Nullable
    private static StructureStart findSmallSect(ServerLevel level, BlockPos pos) {
        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(SMALL_SECT_ID);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(pos, structure);
        return start.isValid() ? start : null;
    }

    private static String instanceKey(ServerLevel level, StructureStart start) {
        BoundingBox bounds = start.getBoundingBox();
        return level.dimension().location() + "|"
                + bounds.minX() + "," + bounds.minY() + "," + bounds.minZ()
                + "," + bounds.maxX() + "," + bounds.maxY() + ","
                + bounds.maxZ();
    }

    private record GuardianPlacement(BlockPos position, float yaw) {
    }

    /** Persistent state keeps an unloaded living guardian from unlocking its sect. */
    private static final class GuardianStateData extends SavedData {
        private static final String DATA_NAME =
                "immortal_cultivation_small_sect_guardians";
        private static final SavedData.Factory<GuardianStateData> FACTORY =
                new SavedData.Factory<>(GuardianStateData::new, GuardianStateData::load);

        private final Set<String> active = new HashSet<>();
        private final Set<String> defeated = new HashSet<>();
        private final Set<String> placedBlocks = new HashSet<>();

        private static GuardianStateData get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(FACTORY, DATA_NAME);
        }

        private static GuardianStateData load(
                CompoundTag tag, HolderLookup.Provider registries) {
            GuardianStateData data = new GuardianStateData();
            CompoundTag activeTag = tag.getCompound("Active");
            for (String key : activeTag.getAllKeys()) {
                if (activeTag.getBoolean(key)) {
                    data.active.add(key);
                }
            }
            CompoundTag defeatedTag = tag.getCompound("Defeated");
            for (String key : defeatedTag.getAllKeys()) {
                if (defeatedTag.getBoolean(key)) {
                    data.defeated.add(key);
                }
            }
            CompoundTag placedTag = tag.getCompound("PlacedBlocks");
            for (String key : placedTag.getAllKeys()) {
                if (placedTag.getBoolean(key)) {
                    data.placedBlocks.add(key);
                }
            }
            return data;
        }

        private boolean isActive(String key) {
            return active.contains(key);
        }

        private boolean isDefeated(String key) {
            return defeated.contains(key);
        }

        private boolean isPlaced(String instanceKey, BlockPos pos) {
            return placedBlocks.contains(placedKey(instanceKey, pos));
        }

        private void markActive(String key) {
            if (!defeated.contains(key) && active.add(key)) {
                setDirty();
            }
        }

        private void markLivingGuardian(String key) {
            // A loaded living, linked guardian overrides a stale death marker.
            boolean changed = defeated.remove(key);
            changed |= active.add(key);
            if (changed) {
                setDirty();
            }
        }

        private void markPlaced(String instanceKey, BlockPos pos) {
            if (placedBlocks.add(placedKey(instanceKey, pos))) {
                setDirty();
            }
        }

        private void markDefeated(String key) {
            boolean changed = active.remove(key);
            changed |= defeated.add(key);
            changed |= placedBlocks.removeIf(
                    placed -> placed.startsWith(key + "@"));
            if (changed) {
                setDirty();
            }
        }

        private static String placedKey(String instanceKey, BlockPos pos) {
            return instanceKey + "@" + pos.asLong();
        }

        @Override
        public CompoundTag save(
                CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag activeTag = new CompoundTag();
            active.forEach(key -> activeTag.putBoolean(key, true));
            tag.put("Active", activeTag);

            CompoundTag defeatedTag = new CompoundTag();
            defeated.forEach(key -> defeatedTag.putBoolean(key, true));
            tag.put("Defeated", defeatedTag);

            CompoundTag placedTag = new CompoundTag();
            placedBlocks.forEach(key -> placedTag.putBoolean(key, true));
            tag.put("PlacedBlocks", placedTag);
            return tag;
        }
    }
}
