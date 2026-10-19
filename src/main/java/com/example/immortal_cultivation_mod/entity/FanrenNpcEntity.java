package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FanrenNpcEntity extends PathfinderMob implements CultivationNpc {
    private static final EntityDataAccessor<String> NPC_SKIN =
            SynchedEntityData.defineId(FanrenNpcEntity.class, EntityDataSerializers.STRING);

    public FanrenNpcEntity(EntityType<? extends FanrenNpcEntity> entityType, Level level) {
        super(entityType, level);
        moveControl = new NpcMoveControl(this);
        xpReward = 0;
        setPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC_SKIN, "");
    }

    @Override
    public String npcArchetype() {
        return NpcProfile.FANREN;
    }

    @Override
    public String getNpcSkinId() {
        return entityData.get(NPC_SKIN);
    }

    @Override
    public void setNpcSkinId(String skinId) {
        entityData.set(NPC_SKIN, skinId == null ? "" : skinId);
    }

    @Override
    public void tick() {
        super.tick();
        NpcProfile.tickCultivation(this, npcArchetype());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (NpcProfile.handleDebugEditorInteraction(this, player, hand)) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (getLastHurtByMob() == player && getLastHurtByMobTimestamp() + 200 > tickCount) {
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NpcProfile.openInteraction(this, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        NpcProfile.save(this, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NpcProfile.load(this, tag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && !NpcProfile.isSparring(this)) {
            setTarget(null);
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity) {
                fleeFrom(attacker.position());
            }
        }
        return hurt;
    }

    private void fleeFrom(Vec3 danger) {
        Vec3 away = position().subtract(danger);
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
        }
        Vec3 destination = position().add(away.normalize().scale(10.0D));
        getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35D);
    }
}
