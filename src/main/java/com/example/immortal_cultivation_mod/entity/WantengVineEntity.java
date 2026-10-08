package com.example.immortal_cultivation_mod.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** One independently animated vine belonging to a 万藤绞杀 area. */
public final class WantengVineEntity extends Entity implements GeoEntity {
    public static final int VINE_COUNT = 6;

    private static final double[][] OFFSETS = {
            {-3.25D, -1.35D},
            {-1.55D, 2.75D},
            {1.65D, 3.15D},
            {3.35D, 0.55D},
            {2.05D, -2.85D},
            {-0.85D, -3.45D}
    };

    private static final RawAnimation[] SWING_ANIMATIONS = {
            RawAnimation.begin().thenLoop("animation.wanteng_vine.swing_0"),
            RawAnimation.begin().thenLoop("animation.wanteng_vine.swing_1"),
            RawAnimation.begin().thenLoop("animation.wanteng_vine.swing_2"),
            RawAnimation.begin().thenLoop("animation.wanteng_vine.swing_3"),
            RawAnimation.begin().thenLoop("animation.wanteng_vine.swing_4"),
            RawAnimation.begin().thenLoop("animation.wanteng_vine.swing_5")
    };

    private static final EntityDataAccessor<Optional<UUID>> DATA_PARENT =
            SynchedEntityData.defineId(
                    WantengVineEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_VINE_INDEX =
            SynchedEntityData.defineId(
                    WantengVineEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WantengVineEntity(EntityType<? extends WantengVineEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static WantengVineEntity create(
            ServerLevel level,
            ElementalTechniqueAreaEntity parent,
            int vineIndex) {
        WantengVineEntity vine = ModEntities.WANTENG_VINE.get().create(level);
        if (vine == null) {
            return null;
        }
        vine.entityData.set(DATA_PARENT, Optional.of(parent.getUUID()));
        vine.entityData.set(DATA_VINE_INDEX, Math.floorMod(vineIndex, VINE_COUNT));
        vine.updateAnchorPosition(parent);
        return vine;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PARENT, Optional.empty());
        builder.define(DATA_VINE_INDEX, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID parentId = parentId();
        Entity parent = parentId == null ? null : serverLevel.getEntity(parentId);
        if (!(parent instanceof ElementalTechniqueAreaEntity area)
                || !area.isAlive()
                || area.kind() != ElementalTechniqueAreaEntity.Kind.WANTENG_JIAOSHA) {
            discard();
            return;
        }
        updateAnchorPosition(area);
    }

    private void updateAnchorPosition(ElementalTechniqueAreaEntity parent) {
        double[] offset = OFFSETS[vineIndex()];
        double scale = parent.fullyCharged() ? 1.18D : 1.0D;
        moveTo(
                parent.getX() + offset[0] * scale,
                parent.getY() + 0.03D,
                parent.getZ() + offset[1] * scale,
                (float) Math.toDegrees(Math.atan2(-offset[0], offset[1])),
                0.0F);
    }

    public UUID parentId() {
        return entityData.get(DATA_PARENT).orElse(null);
    }

    public boolean belongsTo(UUID parentId) {
        return parentId != null && parentId.equals(parentId());
    }

    public int vineIndex() {
        return Math.floorMod(entityData.get(DATA_VINE_INDEX), VINE_COUNT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID parentId = parentId();
        if (parentId != null) {
            tag.putUUID("Parent", parentId);
        }
        tag.putInt("VineIndex", vineIndex());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_PARENT,
                tag.hasUUID("Parent") ? Optional.of(tag.getUUID("Parent")) : Optional.empty());
        entityData.set(DATA_VINE_INDEX, Math.floorMod(tag.getInt("VineIndex"), VINE_COUNT));
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> state.setAndContinue(SWING_ANIMATIONS[vineIndex()])));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
