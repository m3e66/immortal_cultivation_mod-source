package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.FuchenItem;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ZhujiCultivatorEntity extends PathfinderMob implements CultivationNpc {
    private static final EntityDataAccessor<Integer> CULTIVATION_STATE =
            SynchedEntityData.defineId(ZhujiCultivatorEntity.class, EntityDataSerializers.INT);
    @Override public int npcCultivationState() { return entityData.get(CULTIVATION_STATE); }
    @Override public void setNpcCultivationState(int state) { entityData.set(CULTIVATION_STATE, state); }
    private static final EntityDataAccessor<Integer> CAST_TICKS =
            SynchedEntityData.defineId(ZhujiCultivatorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> NPC_SKIN =
            SynchedEntityData.defineId(ZhujiCultivatorEntity.class, EntityDataSerializers.STRING);

    private final NpcCombatBrain combatBrain = new NpcCombatBrain(this);

    public ZhujiCultivatorEntity(EntityType<? extends ZhujiCultivatorEntity> entityType, Level level) {
        super(entityType, level);
        moveControl = new NpcMoveControl(this);
        xpReward = 20;
        setPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new NpcCultivationGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NpcHurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAST_TICKS, 0);
        builder.define(NPC_SKIN, "");
        builder.define(CULTIVATION_STATE, 0);
    }

    @Override
    public String npcArchetype() {
        return NpcProfile.ZHUJI;
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
        if (getCastingTicks() > 0) {
            entityData.set(CAST_TICKS, getCastingTicks() - 1);
        }
        if (!level().isClientSide) {
            DielangShield.tick(this);
            tickNpcFlight(0.48D);
        }
    }

    private void tickNpcFlight(double speed) {
        combatBrain.tickFlightMotion(speed);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        combatBrain.tick();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (NpcProfile.handleDebugEditorInteraction(this, player, hand)) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (getTarget() == player
                || getLastHurtByMob() == player && getLastHurtByMobTimestamp() + 200 > tickCount) {
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
        NpcProfile.ensure(this, npcArchetype());
        if (getMainHandItem().isEmpty()) {
            NpcProfile.equipCombatWeapon(this);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt && entity instanceof LivingEntity target && hasEffect(ModEffects.LINGBENG)) {
            NpcSpellcasting.releaseLingbeng(this, target);
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F
                && !NpcProfile.isNonCombatNpcDamage(source, this)) {
            combatBrain.recordDamage(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            DielangShield.clear(this);
            FuchenItem.clear(this);
        }
        super.remove(reason);
    }

    @Override
    public void beginNpcCast(int ticks) {
        entityData.set(CAST_TICKS, Math.max(1, ticks));
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 1.2D, getZ(),
                    22, 0.4D, 0.5D, 0.4D, 0.04D);
        }
    }

    @Override
    public void endNpcCast() {
        entityData.set(CAST_TICKS, 0);
    }

    @Override
    public int npcCastingTicks() {
        return getCastingTicks();
    }

    public int getCastingTicks() {
        return entityData.get(CAST_TICKS);
    }
}
