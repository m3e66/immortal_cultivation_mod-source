package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.world.CosmicVoidGravity;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A short-lived, owner-only rift between the normal world and the Cosmic Void. */
public final class CosmicVoidPortalEntity extends Entity {
    public static final int LIFETIME_TICKS = 10 * 20;
    private static final int ACTIVATION_DELAY_TICKS = 8;
    private static final int SEARCH_RADIUS = 64;
    private static final int SEARCH_STEP = 8;
    private static final int FALLBACK_PLATFORM_Y = 80;
    private static final double COSMIC_COORDINATE_SCALE = 0.2D;
    private static final double PORTAL_HEIGHT = 4.5D;
    private static final DustParticleOptions RIFT_DUST =
            new DustParticleOptions(
                    new Vector3f(0.58F, 0.20F, 1.0F), 1.15F);

    private static final String RETURN_DIMENSION_TAG =
            "ImmortalCultivationCosmicVoidReturnDimension";
    private static final String RETURN_X_TAG =
            "ImmortalCultivationCosmicVoidReturnX";
    private static final String RETURN_Y_TAG =
            "ImmortalCultivationCosmicVoidReturnY";
    private static final String RETURN_Z_TAG =
            "ImmortalCultivationCosmicVoidReturnZ";
    private static final String RETURN_YAW_TAG =
            "ImmortalCultivationCosmicVoidReturnYaw";
    private static final String RETURN_PITCH_TAG =
            "ImmortalCultivationCosmicVoidReturnPitch";
    private static final String ANCHOR_X_TAG =
            "ImmortalCultivationCosmicVoidAnchorX";
    private static final String ANCHOR_Y_TAG =
            "ImmortalCultivationCosmicVoidAnchorY";
    private static final String ANCHOR_Z_TAG =
            "ImmortalCultivationCosmicVoidAnchorZ";

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(
                    CosmicVoidPortalEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);

    public CosmicVoidPortalEntity(
            EntityType<? extends CosmicVoidPortalEntity> type,
            Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    /** Validates the unlock and replaces this player's previous rift with a fresh one. */
    public static void openFor(ServerPlayer player) {
        int playerStage = CultivationLevels.getStageIndex(
                ModAttachments.getData(player).cultivationLevel());
        int huashenStage = CultivationLevels.getStageIndex(
                CultivationLevels.REALM_HUASHEN
                        + CultivationLevels.STAGE_EARLY);
        if (playerStage < huashenStage) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".cosmic_void_portal.requires_huashen"), true);
            return;
        }

        for (ServerLevel loadedLevel : player.server.getAllLevels()) {
            for (Entity entity : loadedLevel.getAllEntities()) {
                if (entity instanceof CosmicVoidPortalEntity portal
                        && portal.isOwnedBy(player)) {
                    portal.discard();
                }
            }
        }

        ServerLevel level = player.serverLevel();
        CosmicVoidPortalEntity portal =
                ModEntities.COSMIC_VOID_PORTAL.get().create(level);
        if (portal == null) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".cosmic_void_portal.unavailable"), true);
            return;
        }

        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-5D) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        } else {
            forward = forward.normalize();
        }
        Vec3 position = player.position().add(forward.scale(3.35D));
        portal.entityData.set(DATA_OWNER, Optional.of(player.getUUID()));
        portal.setPos(position.x, player.getY(), position.z);
        portal.setYRot(player.getYRot());
        level.addFreshEntity(portal);

        level.playSound(null, portal.blockPosition(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS,
                1.15F, 1.18F);
        portal.emitOpeningVfx(level);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + ".cosmic_void_portal.opened"), true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) {
            return;
        }

        if (tickCount >= LIFETIME_TICKS) {
            discard();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();
        emitAmbientVfx(serverLevel);

        ServerPlayer owner = owner(serverLevel);
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (tickCount < ACTIVATION_DELAY_TICKS
                || !owner.getBoundingBox().intersects(
                        getBoundingBox().inflate(0.28D, 0.0D, 0.28D))) {
            return;
        }

        travel(owner);
        discard();
    }

    private void travel(ServerPlayer player) {
        ServerLevel source = player.serverLevel();
        boolean returning = source.dimension().equals(
                CosmicVoidGravity.DIMENSION);
        ServerLevel target;
        Vec3 destination;
        float yaw;
        float pitch;

        if (returning) {
            ReturnPoint returnPoint = readReturnPoint(player);
            target = returnPoint.level();
            destination = returnPoint.position();
            yaw = returnPoint.yaw();
            pitch = returnPoint.pitch();
        } else {
            target = player.server.getLevel(CosmicVoidGravity.DIMENSION);
            if (target == null) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID
                                + ".cosmic_void_portal.unavailable"), true);
                return;
            }
            BlockPos anchor = getOrCreateCosmicAnchor(player, target);
            storeReturnPoint(player);
            destination = Vec3.atBottomCenterOf(anchor);
            yaw = player.getYRot();
            pitch = player.getXRot();
        }

        source.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                0.8F, 0.72F);
        emitCrossingVfx(source);

        player.teleportTo(target,
                destination.x, destination.y, destination.z,
                Set.of(), yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.hurtMarked = true;

        target.playSound(null, BlockPos.containing(destination),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                0.9F, 1.18F);
        target.sendParticles(ParticleTypes.PORTAL,
                destination.x, destination.y + 0.9D, destination.z,
                72, 0.68D, 1.25D, 0.68D, 0.075D);
        target.sendParticles(RIFT_DUST,
                destination.x, destination.y + 1.0D, destination.z,
                28, 0.62D, 1.15D, 0.62D, 0.018D);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID
                        + (returning
                        ? ".cosmic_void_portal.returned"
                        : ".cosmic_void_portal.entered")), true);
    }

    private void emitOpeningVfx(ServerLevel level) {
        Vec3 right = riftRight();
        Vec3 normal = riftNormal();
        for (int index = 0; index < 72; index++) {
            double lateral = (random.nextDouble() - 0.5D) * 2.15D;
            double height = 0.06D + random.nextDouble()
                    * (PORTAL_HEIGHT - 0.12D);
            double depth = (random.nextDouble() - 0.5D) * 0.32D;
            Vec3 point = riftPoint(right, normal, lateral, height, depth);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y, point.z, 1,
                    right.x * (random.nextDouble() - 0.5D) * 0.20D,
                    (random.nextDouble() - 0.35D) * 0.12D,
                    right.z * (random.nextDouble() - 0.5D) * 0.20D,
                    0.045D);
        }
        for (int index = 0; index < 32; index++) {
            double side = random.nextBoolean() ? -1.0D : 1.0D;
            double lateral = side * (0.72D + random.nextDouble() * 0.34D);
            double height = 0.08D + random.nextDouble()
                    * (PORTAL_HEIGHT - 0.16D);
            Vec3 point = riftPoint(right, normal, lateral, height,
                    (random.nextDouble() - 0.5D) * 0.20D);
            level.sendParticles(index % 3 == 0
                            ? ParticleTypes.END_ROD : RIFT_DUST,
                    point.x, point.y, point.z, 1,
                    0.0D, 0.025D, 0.0D, 0.01D);
        }
    }

    private void emitAmbientVfx(ServerLevel level) {
        if (tickCount % 2 != 0) {
            return;
        }
        Vec3 right = riftRight();
        Vec3 normal = riftNormal();
        double phase = tickCount * 0.17D;
        for (int strand = 0; strand < 3; strand++) {
            double angle = phase + strand * Mth.TWO_PI / 3.0D;
            double lateral = Math.sin(angle)
                    * (0.78D + strand * 0.08D);
            double height = 2.25D
                    + Math.cos(angle * 0.73D + strand) * 1.95D;
            double depth = Math.sin(angle * 1.31D) * 0.10D;
            Vec3 point = riftPoint(right, normal, lateral, height, depth);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y, point.z, 1,
                    normal.x * 0.025D, 0.018D,
                    normal.z * 0.025D, 0.012D);
            if (tickCount % 6 == 0) {
                level.sendParticles(RIFT_DUST,
                        point.x, point.y, point.z, 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (tickCount % 10 == 0) {
            for (int spark = 0; spark < 4; spark++) {
                double lateral = (random.nextDouble() - 0.5D) * 1.75D;
                double height = 0.12D + random.nextDouble()
                        * (PORTAL_HEIGHT - 0.24D);
                Vec3 point = riftPoint(right, normal, lateral, height,
                        (random.nextDouble() - 0.5D) * 0.16D);
                level.sendParticles(ParticleTypes.END_ROD,
                        point.x, point.y, point.z, 1,
                        0.0D, 0.018D, 0.0D, 0.005D);
            }
        }
        if (tickCount % 40 == 0) {
            level.playSound(null, blockPosition(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS,
                    0.34F, 0.78F + random.nextFloat() * 0.12F);
        }
    }

    private void emitCrossingVfx(ServerLevel level) {
        Vec3 right = riftRight();
        Vec3 normal = riftNormal();
        for (int index = 0; index < 88; index++) {
            double lateral = (random.nextDouble() - 0.5D) * 2.15D;
            double height = random.nextDouble() * PORTAL_HEIGHT;
            double depth = (random.nextDouble() - 0.5D) * 0.46D;
            Vec3 point = riftPoint(right, normal, lateral, height, depth);
            level.sendParticles(index % 4 == 0
                            ? RIFT_DUST : ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y, point.z, 1,
                    normal.x * (random.nextDouble() - 0.5D) * 0.28D,
                    (random.nextDouble() - 0.35D) * 0.15D,
                    normal.z * (random.nextDouble() - 0.5D) * 0.28D,
                    0.055D);
        }
    }

    private Vec3 riftRight() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
    }

    private Vec3 riftNormal() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
    }

    private Vec3 riftPoint(
            Vec3 right, Vec3 normal,
            double lateral, double height, double depth) {
        return new Vec3(getX(), getY() + height, getZ())
                .add(right.scale(lateral))
                .add(normal.scale(depth));
    }

    private static void storeReturnPoint(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putString(RETURN_DIMENSION_TAG,
                player.level().dimension().location().toString());
        data.putDouble(RETURN_X_TAG, player.getX());
        data.putDouble(RETURN_Y_TAG, player.getY());
        data.putDouble(RETURN_Z_TAG, player.getZ());
        data.putFloat(RETURN_YAW_TAG, player.getYRot());
        data.putFloat(RETURN_PITCH_TAG, player.getXRot());
    }

    private static ReturnPoint readReturnPoint(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ServerLevel target = player.server.overworld();
        if (data.contains(RETURN_DIMENSION_TAG)) {
            ResourceLocation location = ResourceLocation.tryParse(
                    data.getString(RETURN_DIMENSION_TAG));
            if (location != null) {
                ServerLevel savedLevel = player.server.getLevel(
                        ResourceKey.create(Registries.DIMENSION, location));
                if (savedLevel != null
                        && !savedLevel.dimension().equals(
                        CosmicVoidGravity.DIMENSION)) {
                    target = savedLevel;
                }
            }
        }

        BlockPos spawn = target.getSharedSpawnPos();
        Vec3 position = new Vec3(
                data.contains(RETURN_X_TAG)
                        ? data.getDouble(RETURN_X_TAG)
                        : spawn.getX() + 0.5D,
                data.contains(RETURN_Y_TAG)
                        ? data.getDouble(RETURN_Y_TAG)
                        : spawn.getY(),
                data.contains(RETURN_Z_TAG)
                        ? data.getDouble(RETURN_Z_TAG)
                        : spawn.getZ() + 0.5D);
        float yaw = data.contains(RETURN_YAW_TAG)
                ? data.getFloat(RETURN_YAW_TAG) : player.getYRot();
        float pitch = data.contains(RETURN_PITCH_TAG)
                ? data.getFloat(RETURN_PITCH_TAG) : player.getXRot();
        return new ReturnPoint(target, position, yaw, pitch);
    }

    private static BlockPos getOrCreateCosmicAnchor(
            ServerPlayer player, ServerLevel level) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(ANCHOR_X_TAG)
                && data.contains(ANCHOR_Y_TAG)
                && data.contains(ANCHOR_Z_TAG)) {
            BlockPos saved = new BlockPos(
                    data.getInt(ANCHOR_X_TAG),
                    data.getInt(ANCHOR_Y_TAG),
                    data.getInt(ANCHOR_Z_TAG));
            level.getChunk(saved.getX() >> 4, saved.getZ() >> 4);
            if (isSafe(level, saved)) {
                return saved;
            }
        }

        int centerX = Mth.floor(Mth.clamp(
                player.getX() * COSMIC_COORDINATE_SCALE,
                -29_999_000.0D, 29_999_000.0D));
        int centerZ = Mth.floor(Mth.clamp(
                player.getZ() * COSMIC_COORDINATE_SCALE,
                -29_999_000.0D, 29_999_000.0D));
        BlockPos anchor = findSafeSurface(level, centerX, centerZ);
        if (anchor == null) {
            anchor = buildFallbackPlatform(level, centerX, centerZ);
        }
        data.putInt(ANCHOR_X_TAG, anchor.getX());
        data.putInt(ANCHOR_Y_TAG, anchor.getY());
        data.putInt(ANCHOR_Z_TAG, anchor.getZ());
        return anchor;
    }

    @Nullable
    private static BlockPos findSafeSurface(
            ServerLevel level, int centerX, int centerZ) {
        for (int radius = 0; radius <= SEARCH_RADIUS;
             radius += SEARCH_STEP) {
            int minimum = radius == 0 ? 0 : -radius;
            int maximum = radius == 0 ? 0 : radius;
            for (int xOffset = minimum; xOffset <= maximum;
                 xOffset += SEARCH_STEP) {
                for (int zOffset = minimum; zOffset <= maximum;
                     zOffset += SEARCH_STEP) {
                    if (radius > 0
                            && Math.abs(xOffset) != radius
                            && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    int x = centerX + xOffset;
                    int z = centerZ + zOffset;
                    level.getChunk(x >> 4, z >> 4);
                    int y = level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x, z);
                    BlockPos feet = new BlockPos(x, y, z);
                    if (isSafe(level, feet)) {
                        return feet;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos buildFallbackPlatform(
            ServerLevel level, int centerX, int centerZ) {
        int feetY = Mth.clamp(FALLBACK_PLATFORM_Y,
                level.getMinBuildHeight() + 3,
                level.getMaxBuildHeight() - 4);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int xOffset = -2; xOffset <= 2; xOffset++) {
            for (int zOffset = -2; zOffset <= 2; zOffset++) {
                cursor.set(centerX + xOffset, feetY - 1,
                        centerZ + zOffset);
                level.setBlockAndUpdate(cursor,
                        ModBlocks.VOID_STONE.get().defaultBlockState());
            }
        }
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                for (int yOffset = 0; yOffset <= 2; yOffset++) {
                    cursor.set(centerX + xOffset, feetY + yOffset,
                            centerZ + zOffset);
                    level.destroyBlock(cursor, false);
                }
            }
        }
        return new BlockPos(centerX, feetY, centerZ);
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)
                || feet.getY() <= level.getMinBuildHeight() + 1
                || feet.getY() >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        BlockPos floorPos = feet.below();
        BlockState floor = level.getBlockState(floorPos);
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        return floor.isFaceSturdy(level, floorPos, Direction.UP)
                && floor.getFluidState().isEmpty()
                && body.getFluidState().isEmpty()
                && head.getFluidState().isEmpty()
                && body.getCollisionShape(level, feet).isEmpty()
                && head.getCollisionShape(level, feet.above()).isEmpty();
    }

    private boolean isOwnedBy(ServerPlayer player) {
        return entityData.get(DATA_OWNER)
                .map(player.getUUID()::equals)
                .orElse(false);
    }

    @Nullable
    private ServerPlayer owner(ServerLevel level) {
        return entityData.get(DATA_OWNER)
                .map(level.getServer().getPlayerList()::getPlayer)
                .orElse(null);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            entityData.set(DATA_OWNER,
                    Optional.of(tag.getUUID("Owner")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(DATA_OWNER)
                .ifPresent(owner -> tag.putUUID("Owner", owner));
    }

    private record ReturnPoint(
            ServerLevel level, Vec3 position, float yaw, float pitch) {
    }
}
