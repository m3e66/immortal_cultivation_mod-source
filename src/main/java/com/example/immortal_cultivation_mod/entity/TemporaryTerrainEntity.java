package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.event.DeadRuinProtection;
import com.example.immortal_cultivation_mod.event.SmallSectProtection;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** Owns destructible temporary terrain and restores only blocks that still match its placements. */
public final class TemporaryTerrainEntity extends Entity {
    private static final int MAX_AIR_PLACEMENTS = 256;
    private final Map<BlockPos, TrackedBlock> trackedBlocks = new LinkedHashMap<>();
    private long expireGameTime;
    private java.util.UUID contactOwnerId;
    private java.lang.ref.WeakReference<LivingEntity> contactOwner = new java.lang.ref.WeakReference<>(null);
    private float contactDamage;
    private net.minecraft.world.phys.AABB contactArea;
    private boolean woodWithering;
    public void enableWoodWithering(){woodWithering=true;}
    public static int woodWitherTick(BlockPos pos){return 1+Math.floorMod(Long.hashCode(pos.asLong()),10);}

    /** Only explicitly armed terrain is harmful; ordinary temporary terrain keeps its existing behavior. */
    public void enableContactDamage(LivingEntity owner, float damage) {
        if (owner == null || owner.level() != level() || !Float.isFinite(damage) || damage <= 0) return;
        contactOwnerId = owner.getUUID();
        contactOwner = new java.lang.ref.WeakReference<>(owner);
        contactDamage = damage;
        if (level() instanceof ServerLevel server) tickContactDamage(server);
    }

    private void tickContactDamage(ServerLevel level) {
        if (contactOwnerId == null || contactDamage <= 0 || trackedBlocks.isEmpty()) return;
        LivingEntity owner = contactOwner.get();
        if (owner == null && level.getEntity(contactOwnerId) instanceof LivingEntity loaded) {
            owner = loaded; contactOwner = new java.lang.ref.WeakReference<>(owner);
        }
        if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.level() != level) return;
        if (contactArea == null) {
            for (BlockPos pos : trackedBlocks.keySet()) {
                var cell = new net.minecraft.world.phys.AABB(pos);
                contactArea = contactArea == null ? cell : contactArea.minmax(cell);
            }
        }
        int checked = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, contactArea.inflate(.12))) {
            if (++checked > 96) break;
            if (!com.example.immortal_cultivation_mod.combat.QiDomainSystem.canAffect(owner, target)) continue;
            var touch = target.getBoundingBox().inflate(.12);
            for (var entry : trackedBlocks.entrySet()) {
                // Standing against a solid spike counts as contact; rejected or later-edited cells do not.
                if (level.hasChunkAt(entry.getKey()) && level.getBlockState(entry.getKey()) == entry.getValue().expected()
                        && new net.minecraft.world.phys.AABB(entry.getKey()).intersects(touch)) {
                    com.example.immortal_cultivation_mod.combat.QiDomainDamage.hurt(owner, target, contactDamage, false);
                    break;
                }
            }
        }
    }

    public TemporaryTerrainEntity(
            EntityType<? extends TemporaryTerrainEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static TemporaryTerrainEntity place(
            ServerLevel level, Map<BlockPos, BlockState> placements, int lifetimeTicks) {
        return place(level, placements, lifetimeTicks, false);
    }

    /** Temporarily replaces existing simple blocks and restores their prior state on expiry. */
    public static TemporaryTerrainEntity replaceAndRestore(
            ServerLevel level, Map<BlockPos, BlockState> placements, int lifetimeTicks) {
        return place(level, placements, lifetimeTicks, true);
    }

    /**
     * Places at most 256 temporary simple blocks in empty cells of one loaded chunk.
     * Callers must group a larger shape by chunk and exclude friendly entities' occupied cells.
     * The anchor shares its blocks' chunk so its saved expiry survives chunk unloads.
     */
    public static TemporaryTerrainEntity placeAir(
            ServerLevel level, LivingEntity owner, Map<BlockPos, BlockState> placements,
            int lifetimeTicks) {
        if (placements == null || placements.isEmpty() || !canPlaceAir(level, owner)) return null;
        Map<BlockPos, BlockState> candidates = new LinkedHashMap<>();
        BlockPos first = null;
        int checked = 0;
        for (Map.Entry<BlockPos, BlockState> entry : placements.entrySet()) {
            if (checked++ >= MAX_AIR_PLACEMENTS) break;
            if (entry.getKey() == null) continue;
            BlockPos pos = entry.getKey().immutable();
            if (first != null && ((pos.getX() >> 4) != (first.getX() >> 4)
                    || (pos.getZ() >> 4) != (first.getZ() >> 4))) return null;
            if (!canPlaceAirAt(level, owner, pos, entry.getValue())) continue;
            if (first == null) first = pos;
            candidates.put(pos, entry.getValue());
        }
        if (candidates.isEmpty()) return null;
        TemporaryTerrainEntity anchor = ModEntities.TEMPORARY_TERRAIN.get().create(level);
        if (anchor == null) return null;
        anchor.setPos(Vec3.atCenterOf(first));
        anchor.expireGameTime = level.getGameTime() + Math.max(1, lifetimeTicks);
        if (!level.addFreshEntity(anchor)) {
            anchor.discard();
            return null;
        }
        try {
            BlockBreakingSettings.runWithSource(BlockBreakingSettings.Category.TECHNIQUES, owner, () -> {
                for (Map.Entry<BlockPos, BlockState> entry : candidates.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = entry.getValue();
                    if (!canPlaceAir(level, owner) || !canPlaceAirAt(level, owner, pos, state)) continue;
                    BlockState original = level.getBlockState(pos);
                    BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
                    if (!level.setBlock(pos, state, Block.UPDATE_CLIENTS)) continue;
                    anchor.trackedBlocks.put(pos, new TrackedBlock(state, original, true));
                    if (EventHooks.onBlockPlace(owner, snapshot, Direction.UP)) {
                        snapshot.restore();
                        anchor.trackedBlocks.remove(pos);
                        continue;
                    }
                    if (level.getBlockState(pos) != state) {
                        anchor.trackedBlocks.remove(pos);
                        continue;
                    }
                    level.updateNeighborsAt(pos, state.getBlock());
                }
            });
        } catch (RuntimeException | Error exception) {
            anchor.cleanupNow();
            throw exception;
        }
        if (anchor.trackedBlocks.isEmpty()) {
            anchor.discard();
            return null;
        }
        return anchor;
    }

    private static boolean canPlaceAir(ServerLevel level, LivingEntity owner) {
        if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.level() != level
                || !BlockBreakingSettings.enabled(owner, BlockBreakingSettings.Category.TECHNIQUES)
                || owner instanceof Mob && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
            return false;
        Player player = BlockBreakingSettings.playerOwner(owner);
        if (player == null && owner instanceof Player directPlayer) player = directPlayer;
        return player == null || player.isAlive() && !player.isSpectator()
                && player.level() == level && player.getAbilities().mayBuild;
    }

    private static boolean canPlaceAirAt(
            ServerLevel level, LivingEntity owner, BlockPos pos, BlockState state) {
        if (state == null || state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                || !level.hasChunkAt(pos) || !level.isInWorldBounds(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()
                || SmallSectProtection.isLocked(level, pos) || DeadRuinProtection.isLocked(level, pos)
                || !state.canSurvive(level, pos)
                || CenterEffectSupport.isForcefieldRouteBlocked(owner, owner.position(), Vec3.atCenterOf(pos)))
            return false;
        Player player = BlockBreakingSettings.playerOwner(owner);
        if (player == null && owner instanceof Player directPlayer) player = directPlayer;
        return player == null || level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }

    /** Exact accepted placements, for collision/damage checks and lifecycle verification. */
    public Map<BlockPos, BlockState> placedStates() {
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        trackedBlocks.forEach((pos, tracked) -> states.put(pos, tracked.expected()));
        return Map.copyOf(states);
    }

    private static TemporaryTerrainEntity place(
            ServerLevel level, Map<BlockPos, BlockState> placements, int lifetimeTicks,
            boolean replaceExisting) {
        if (placements == null || placements.isEmpty()) {
            return null;
        }
        TemporaryTerrainEntity anchor = ModEntities.TEMPORARY_TERRAIN.get().create(level);
        if (anchor == null) {
            return null;
        }
        double centerX = 0.0D;
        double centerY = 0.0D;
        double centerZ = 0.0D;
        int placed = 0;
        var source = BlockBreakingSettings.source(
                BlockBreakingSettings.Category.TECHNIQUES, null);
        for (Map.Entry<BlockPos, BlockState> entry : placements.entrySet()) {
            BlockPos pos = entry.getKey().immutable();
            BlockState state = entry.getValue();
            BlockState original = level.getBlockState(pos);
            if (state == null
                    || replaceExisting && original.hasBlockEntity()
                    || !replaceExisting && !canReplace(level, pos)
                    || original.is(state.getBlock())) {
                continue;
            }
            boolean accepted = !original.isAir() && source.actor() instanceof net.minecraft.world.entity.LivingEntity actor
                    ? com.example.immortal_cultivation_mod.spell.TerrainTransforms.replace(level, pos, state, actor, source.category())
                    : level.setBlock(pos, state, Block.UPDATE_ALL);
            if (!accepted) {
                continue;
            }
            anchor.trackedBlocks.put(pos, new TrackedBlock(state, original, false));
            centerX += pos.getX() + 0.5D;
            centerY += pos.getY() + 0.5D;
            centerZ += pos.getZ() + 0.5D;
            placed++;
        }
        if (placed == 0) {
            return null;
        }
        anchor.setPos(centerX / placed, centerY / placed, centerZ / placed);
        anchor.expireGameTime = level.getGameTime() + Math.max(1, lifetimeTicks);
        level.addFreshEntity(anchor);
        return anchor;
    }

    public static boolean canReplace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (!level().isClientSide && level().getGameTime() >= expireGameTime) {
            cleanupNow();
        } else if (level() instanceof ServerLevel server && tickCount % 2 == 0) {
            if(woodWithering&&expireGameTime-server.getGameTime()<=10) {
                int phase=(int)(10-(expireGameTime-server.getGameTime()));
                var iterator=trackedBlocks.entrySet().iterator();
                while(iterator.hasNext()) {
                    var entry=iterator.next();
                    if(woodWitherTick(entry.getKey())>phase)continue;
                    var tracked=entry.getValue();var current=server.getBlockState(entry.getKey());
                    if(server.hasChunkAt(entry.getKey())&&(tracked.exact()?current==tracked.expected():current.is(tracked.expected().getBlock())))
                        server.setBlock(entry.getKey(),tracked.original(),Block.UPDATE_ALL);
                    iterator.remove();
                }
            }
            tickContactDamage(server);
        }
    }

    public void cleanupNow() {
        if (level() instanceof ServerLevel level) {
            for (Map.Entry<BlockPos, TrackedBlock> entry : trackedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                TrackedBlock tracked = entry.getValue();
                if (level.hasChunkAt(pos) && (tracked.exact()
                        ? level.getBlockState(pos) == tracked.expected()
                        : level.getBlockState(pos).is(tracked.expected().getBlock()))) {
                    level.setBlock(pos, tracked.original(), Block.UPDATE_ALL);
                }
            }
        }
        trackedBlocks.clear();
        discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        expireGameTime = tag.getLong("ExpireGameTime");
        woodWithering=tag.getBoolean("WoodWithering");
        contactOwnerId = tag.hasUUID("ContactOwner") ? tag.getUUID("ContactOwner") : null;
        contactOwner = new java.lang.ref.WeakReference<>(null);
        contactDamage = tag.getFloat("ContactDamage");
        if (!Float.isFinite(contactDamage) || contactDamage < 0) contactDamage = 0;
        contactArea = null;
        trackedBlocks.clear();
        ListTag blocks = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < blocks.size(); index++) {
            CompoundTag entry = blocks.getCompound(index);
            BlockState expected = readState(entry, "State", "Block");
            BlockState original = readState(entry, "OriginalState", "OriginalBlock");
            trackedBlocks.put(BlockPos.of(entry.getLong("Pos")),
                    new TrackedBlock(expected, original, entry.getBoolean("ExactState")));
        }
    }

    private BlockState readState(CompoundTag entry, String stateKey, String legacyKey) {
        if (entry.contains(stateKey, Tag.TAG_COMPOUND))
            return NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(Registries.BLOCK),
                    entry.getCompound(stateKey));
        ResourceLocation id = ResourceLocation.tryParse(entry.getString(legacyKey));
        return id == null ? Blocks.AIR.defaultBlockState() : BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("ExpireGameTime", expireGameTime);
        if(woodWithering)tag.putBoolean("WoodWithering",true);
        if (contactOwnerId != null && contactDamage > 0) {
            tag.putUUID("ContactOwner", contactOwnerId);
            tag.putFloat("ContactDamage", contactDamage);
        }
        ListTag blocks = new ListTag();
        for (Map.Entry<BlockPos, TrackedBlock> tracked : trackedBlocks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", tracked.getKey().asLong());
            entry.put("State", NbtUtils.writeBlockState(tracked.getValue().expected()));
            entry.put("OriginalState", NbtUtils.writeBlockState(tracked.getValue().original()));
            entry.putBoolean("ExactState", tracked.getValue().exact());
            entry.putString("Block", BuiltInRegistries.BLOCK.getKey(
                    tracked.getValue().expected().getBlock()).toString());
            entry.putString("OriginalBlock", BuiltInRegistries.BLOCK.getKey(
                    tracked.getValue().original().getBlock()).toString());
            blocks.add(entry);
        }
        tag.put("Blocks", blocks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private record TrackedBlock(BlockState expected, BlockState original, boolean exact) {
    }
}
