package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.UUID;

public class JindanEntity extends PathfinderMob {
    private static final String JINDAN_ANCHOR_TAG = "ImmortalCultivationJindanAnchor";
    private static final int REPLICATE_CHUNK_QI_COST = 2000;
    private static final int SOURCE_RANDOM_RADIUS_BLOCKS = 120_000;
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(JindanEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private boolean hasReplicaSource;
    private int sourceOriginChunkX;
    private int sourceOriginChunkZ;
    private int destOriginChunkX;
    private int destOriginChunkZ;
    private int copiedChunkCount;

    public JindanEntity(EntityType<? extends JindanEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        setNoAi(true);
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
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
                || !ModPayloads.JINDAN_DIMENSION.equals(jindanLevel.dimension())) {
            return;
        }

        var data = ModAttachments.getData(player);
        if (data.qi() < REPLICATE_CHUNK_QI_COST) {
            player.displayClientMessage(Component.literal("Need 2000 qi"), true);
            return;
        }

        ModAttachments.setData(player, data.withQi(data.qi() - REPLICATE_CHUNK_QI_COST));
        ServerEvents.syncPlayerData(player);

        if (!hasReplicaSource) {
            pickReplicaSource(player);
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

        spawnInjectParticles(jindanLevel);

        ChunkPos entityChunk = new ChunkPos(blockPosition());
        if (entityChunk.x == destinationChunkX && entityChunk.z == destinationChunkZ) {
            moveEntityAndAnchorToSurface(player, jindanLevel);
        }
    }

    private void pickReplicaSource(ServerPlayer player) {
        int sourceX = player.getRandom().nextInt(SOURCE_RANDOM_RADIUS_BLOCKS * 2 + 1)
                - SOURCE_RANDOM_RADIUS_BLOCKS;
        int sourceZ = player.getRandom().nextInt(SOURCE_RANDOM_RADIUS_BLOCKS * 2 + 1)
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putBoolean("HasReplicaSource", hasReplicaSource);
        tag.putInt("ReplicaSourceChunkX", sourceOriginChunkX);
        tag.putInt("ReplicaSourceChunkZ", sourceOriginChunkZ);
        tag.putInt("ReplicaDestChunkX", destOriginChunkX);
        tag.putInt("ReplicaDestChunkZ", destOriginChunkZ);
        tag.putInt("ReplicaCopiedChunks", copiedChunkCount);
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
        setNoAi(true);
        setPersistenceRequired();
        setCustomName(Component.literal("金丹"));
        setCustomNameVisible(true);
    }
}
