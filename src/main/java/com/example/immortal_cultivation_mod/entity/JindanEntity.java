package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.world.JindanPocketWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class JindanEntity extends PathfinderMob {
    private static final String JINDAN_ANCHOR_TAG = "ImmortalCultivationJindanAnchor";
    private static final String GENERATED_CHUNK_COUNT_TAG = "ImmortalCultivationJindanGeneratedChunks";
    private static final int SOURCE_RANDOM_RADIUS_BLOCKS = 120_000;
    private static final long FALLBACK_REPLICA_SOURCE_SEED = 0x4A494E44414E5EEDL;
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(JindanEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private boolean hasReplicaSource;
    private int sourceOriginChunkX;
    private int sourceOriginChunkZ;
    private int destOriginChunkX;
    private int destOriginChunkZ;
    private int copiedChunkCount;
    private long replicaSourceSeed;

    public JindanEntity(EntityType<? extends JindanEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        setCustomName(Component.literal("金丹"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setOwner(UUID owner) {
        entityData.set(OWNER_UUID, Optional.of(owner));
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(OWNER_UUID);
    }

    public void setReplicaSourceSeed(long seed) {
        long normalizedSeed = seed == 0L ? FALLBACK_REPLICA_SOURCE_SEED : seed;
        if (replicaSourceSeed == 0L) {
            replicaSourceSeed = normalizedSeed;
            return;
        }
        if (replicaSourceSeed == normalizedSeed) {
            return;
        }

        replicaSourceSeed = normalizedSeed;
        hasReplicaSource = false;
        sourceOriginChunkX = 0;
        sourceOriginChunkZ = 0;
        destOriginChunkX = 0;
        destOriginChunkZ = 0;
        copiedChunkCount = 0;
    }

    public static int generatedChunkCount(Player player) {
        return Math.max(0, player.getPersistentData().getInt(GENERATED_CHUNK_COUNT_TAG));
    }

    public static void resetGeneratedChunkCount(Player player) {
        player.getPersistentData().remove(GENERATED_CHUNK_COUNT_TAG);
    }

    public static void copyGeneratedChunkCount(Player original, Player copy) {
        int count = generatedChunkCount(original);
        if (count > 0) {
            copy.getPersistentData().putInt(GENERATED_CHUNK_COUNT_TAG, count);
        }
    }

    public void syncGeneratedChunkCount(ServerPlayer player) {
        int count = Math.max(generatedChunkCount(player), Math.max(0, copiedChunkCount));
        if (count > 0 && !hasReplicaSource) {
            pickReplicaSource();
        }
        copiedChunkCount = count;
        if (count > 0) {
            player.getPersistentData().putInt(GENERATED_CHUNK_COUNT_TAG, count);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount == 1
                && JindanPocketWorlds.isJindanDimension(level().dimension())) {
            Optional<UUID> owner = ownerUuid();
            for (var entity : ((ServerLevel) level()).getAllEntities()) {
                if (entity instanceof JindanEntity other
                        && other != this
                        && owner.isPresent()
                        && other.ownerUuid().equals(owner)
                        && other.getId() < getId()) {
                    discard();
                    return;
                }
            }
        }
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        Optional<UUID> owner = ownerUuid();
        if (owner.isPresent() && !owner.get().equals(serverPlayer.getUUID())) {
            return InteractionResult.CONSUME;
        }
        if (serverPlayer.isShiftKeyDown()) {
            injectQiAndReplicateChunk(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        ModPayloads.returnFromJindan(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    private void injectQiAndReplicateChunk(ServerPlayer player) {
        if (!(level() instanceof ServerLevel jindanLevel)
                || !JindanPocketWorlds.isOwnersPocket(player, jindanLevel.dimension())) {
            return;
        }

        var data = ModAttachments.getData(player);
        int qiCost = ServerEvents.getJindanChunkQiCost(player);
        if (data.qi() < qiCost) {
            player.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.jindan_chunk_need_qi", qiCost), true);
            return;
        }

        ModAttachments.setData(player, data.withQi(data.qi() - qiCost));
        ServerEvents.syncPlayerData(player);

        if (!hasReplicaSource) {
            pickReplicaSource();
        }

        ServerLevel overworld = player.server.overworld();
        ChunkOffset offset = spiralOffset(copiedChunkCount);
        int destinationChunkX = destOriginChunkX + offset.x();
        int destinationChunkZ = destOriginChunkZ + offset.z();
        copyChunk(
                overworld,
                jindanLevel,
                sourceOriginChunkX + offset.x(),
                sourceOriginChunkZ + offset.z(),
                destinationChunkX,
                destinationChunkZ
        );
        copiedChunkCount++;
        player.getPersistentData().putInt(
                GENERATED_CHUNK_COUNT_TAG,
                Math.max(generatedChunkCount(player) + 1, copiedChunkCount));

        spawnInjectParticles(jindanLevel);

        ChunkPos entityChunk = new ChunkPos(blockPosition());
        if (entityChunk.x == destinationChunkX && entityChunk.z == destinationChunkZ) {
            moveEntityAndAnchorToSurface(player, jindanLevel);
        }
    }

    private void pickReplicaSource() {
        long seed = replicaSourceSeed == 0L ? FALLBACK_REPLICA_SOURCE_SEED : replicaSourceSeed;
        RandomSource random = RandomSource.create(seed);
        int sourceX = random.nextInt(SOURCE_RANDOM_RADIUS_BLOCKS * 2 + 1)
                - SOURCE_RANDOM_RADIUS_BLOCKS;
        int sourceZ = random.nextInt(SOURCE_RANDOM_RADIUS_BLOCKS * 2 + 1)
                - SOURCE_RANDOM_RADIUS_BLOCKS;
        ChunkPos destination = new ChunkPos(blockPosition());

        sourceOriginChunkX = Math.floorDiv(sourceX, 16);
        sourceOriginChunkZ = Math.floorDiv(sourceZ, 16);
        destOriginChunkX = destination.x;
        destOriginChunkZ = destination.z;
        copiedChunkCount = 0;
        hasReplicaSource = true;
    }

    private void copyChunk(
            ServerLevel source,
            ServerLevel destination,
            int sourceChunkX,
            int sourceChunkZ,
            int destinationChunkX,
            int destinationChunkZ
    ) {
        source.getChunk(sourceChunkX, sourceChunkZ);
        destination.getChunk(destinationChunkX, destinationChunkZ);

        int minY = Math.max(source.getMinBuildHeight(), destination.getMinBuildHeight());
        int maxY = Math.min(source.getMaxBuildHeight(), destination.getMaxBuildHeight());
        BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos destinationPos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int sourceX = (sourceChunkX << 4) + localX;
            int destinationX = (destinationChunkX << 4) + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int sourceZ = (sourceChunkZ << 4) + localZ;
                int destinationZ = (destinationChunkZ << 4) + localZ;
                for (int y = minY; y < maxY; y++) {
                    sourcePos.set(sourceX, y, sourceZ);
                    destinationPos.set(destinationX, y, destinationZ);
                    BlockState state = source.getBlockState(sourcePos);
                    destination.setBlock(destinationPos, state, 2);
                }
            }
        }
    }

    private void moveEntityAndAnchorToSurface(ServerPlayer player, ServerLevel level) {
        int entityTop = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                getBlockX(),
                getBlockZ()
        );
        moveTo(getX(), entityTop, getZ(), getYRot(), getXRot());
        if (player.level() == level) {
            int playerTop = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.getBlockX(),
                    player.getBlockZ()
            );
            player.teleportTo(
                    level,
                    player.getX(),
                    playerTop,
                    player.getZ(),
                    Set.of(),
                    player.getYRot(),
                    player.getXRot()
            );
        }

        CompoundTag playerData = player.getPersistentData();
        CompoundTag anchor = playerData.contains(JINDAN_ANCHOR_TAG)
                ? playerData.getCompound(JINDAN_ANCHOR_TAG)
                : new CompoundTag();
        int anchorX = anchor.contains("x") ? anchor.getInt("x") : getBlockX();
        int anchorZ = anchor.contains("z") ? anchor.getInt("z") : getBlockZ();
        int anchorY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                anchorX,
                anchorZ
        );
        anchor.putInt("x", anchorX);
        anchor.putInt("y", anchorY);
        anchor.putInt("z", anchorZ);
        playerData.put(JINDAN_ANCHOR_TAG, anchor);
    }

    private void spawnInjectParticles(ServerLevel level) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                getX(),
                getY() + 0.9D,
                getZ(),
                70,
                0.85D,
                0.75D,
                0.85D,
                0.08D
        );
        level.sendParticles(
                ParticleTypes.ENCHANT,
                getX(),
                getY() + 1.0D,
                getZ(),
                120,
                1.1D,
                1.0D,
                1.1D,
                0.18D
        );
    }

    private static ChunkOffset spiralOffset(int index) {
        if (index <= 0) {
            return new ChunkOffset(0, 0);
        }

        int ring = (int) Math.ceil((Math.sqrt(index + 1.0D) - 1.0D) / 2.0D);
        int side = ring * 2;
        int max = (ring * 2 + 1) * (ring * 2 + 1) - 1;
        int offset = max - index;

        if (offset < side) {
            return new ChunkOffset(ring - offset, -ring);
        }
        offset -= side;
        if (offset < side) {
            return new ChunkOffset(-ring, -ring + offset);
        }
        offset -= side;
        if (offset < side) {
            return new ChunkOffset(-ring + offset, ring);
        }
        offset -= side;
        return new ChunkOffset(ring, ring - offset);
    }

    private record ChunkOffset(int x, int z) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putBoolean("HasReplicaSource", hasReplicaSource);
        tag.putInt("ReplicaSourceChunkX", sourceOriginChunkX);
        tag.putInt("ReplicaSourceChunkZ", sourceOriginChunkZ);
        tag.putInt("ReplicaDestChunkX", destOriginChunkX);
        tag.putInt("ReplicaDestChunkZ", destOriginChunkZ);
        tag.putInt("ReplicaCopiedChunks", copiedChunkCount);
        tag.putLong("ReplicaSourceSeed", replicaSourceSeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
        hasReplicaSource = tag.getBoolean("HasReplicaSource");
        sourceOriginChunkX = tag.getInt("ReplicaSourceChunkX");
        sourceOriginChunkZ = tag.getInt("ReplicaSourceChunkZ");
        destOriginChunkX = tag.getInt("ReplicaDestChunkX");
        destOriginChunkZ = tag.getInt("ReplicaDestChunkZ");
        copiedChunkCount = tag.getInt("ReplicaCopiedChunks");
        replicaSourceSeed = tag.getLong("ReplicaSourceSeed");
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        setCustomName(Component.literal("金丹"));
        setCustomNameVisible(true);
    }
}
