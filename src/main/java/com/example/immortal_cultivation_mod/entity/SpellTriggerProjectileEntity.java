package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.spell.ElementalTechniques;
import com.example.immortal_cultivation_mod.spell.ChangqingTechniques;
import com.example.immortal_cultivation_mod.spell.GangtingTechniques;
import com.example.immortal_cultivation_mod.spell.HanlanTechniques;
import com.example.immortal_cultivation_mod.spell.HealingTechniques;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SolarTechniques;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import com.example.immortal_cultivation_mod.spell.SuiguiXingyun;
import com.example.immortal_cultivation_mod.spell.XuanyueTechniques;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Invisible real projectile that activates an existing spell at its impact point. */
public final class SpellTriggerProjectileEntity extends ThrowableItemProjectile
        implements ForcefieldInterceptable {
    /** Safety cleanup only; spell reach is determined by projectile collision, not distance. */
    private static final int MAX_LIFETIME_TICKS = 20 * 30;
    private static final EntityDataAccessor<String> DATA_SPELL_ID =
            SynchedEntityData.defineId(SpellTriggerProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE =
            SynchedEntityData.defineId(SpellTriggerProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVATED =
            SynchedEntityData.defineId(SpellTriggerProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SOLAR_IMPACT_TICKS =
            SynchedEntityData.defineId(SpellTriggerProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MOMENTUM_SNAPSHOT =
            SynchedEntityData.defineId(SpellTriggerProjectileEntity.class, EntityDataSerializers.FLOAT);
    private String spellId = "";
    private boolean activated;
    private float chargeScale = SpellCharge.NORMAL;
    private int solarImpactTicks;
    private float solarPowerSnapshot = 50.0F;
    private float momentumSnapshot;
    private final Set<Integer> piercedEntities = new HashSet<>();

    public SpellTriggerProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SpellTriggerProjectileEntity(
            Level level, LivingEntity owner, String spellId) {
        this(level, owner, spellId, SpellCharge.NORMAL);
    }

    public SpellTriggerProjectileEntity(
            Level level, LivingEntity owner, String spellId, float chargeScale) {
        super(ModEntities.SPELL_TRIGGER_PROJECTILE.get(), owner, level);
        this.spellId = ModSpells.normalizeId(spellId);
        this.chargeScale = SpellCharge.clamp(chargeScale);
        entityData.set(DATA_SPELL_ID, this.spellId);
        entityData.set(DATA_CHARGE_SCALE, this.chargeScale);
        setNoGravity(true);
        NpcProfile.markNpcOwnedEffect(this, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPELL_ID, "");
        builder.define(DATA_CHARGE_SCALE, SpellCharge.NORMAL);
        builder.define(DATA_ACTIVATED, false);
        builder.define(DATA_SOLAR_IMPACT_TICKS, 0);
        builder.define(DATA_MOMENTUM_SNAPSHOT, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        Entity owner = getOwner();
        if (!level().isClientSide) {
            NpcProfile.markNpcOwnedEffect(this, owner);
            if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
                discard();
                return;
            }
        }

        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (activated && ModSpells.DARI_ZHEN.equals(spellId)) {
            solarImpactTicks++;
            entityData.set(DATA_SOLAR_IMPACT_TICKS, solarImpactTicks);
            SolarTechniques.tickRiyunImpact(this);
            return;
        }
        if (ForcefieldCenterBehavior.interceptProjectileImpact(this, position())) {
            finishLeixiao();
            return;
        }
        SolarTechniques.tickTriggerProjectile(this);
        GangtingTechniques.tickTriggerProjectile(this);
        XuanyueTechniques.tickTriggerProjectile(this);
        HanlanTechniques.tickTriggerProjectile(this);
        ChangqingTechniques.tickTriggerProjectile(this);
        if (tickCount > MAX_LIFETIME_TICKS) {
            finishLeixiao();
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        boolean validTarget = resolved instanceof LivingEntity living
                && (HealingTechniques.isTechnique(spellId)
                ? getOwner() instanceof LivingEntity caster
                        && HealingTechniques.canProjectileHit(caster, living)
                : NpcProfile.canNpcEffectHit(this, getOwner(), resolved));
        return resolved instanceof LivingEntity
                && validTarget
                && (!GangtingTechniques.isPiercingProjectile(spellId)
                || !piercedEntities.contains(resolved.getId()))
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide || activated || isRemoved()) {
            return;
        }
        if (GangtingTechniques.isPiercingProjectile(spellId)
                && result instanceof EntityHitResult entityHit
                && getOwner() instanceof LivingEntity caster
                && level() instanceof ServerLevel serverLevel) {
            Entity resolved = ModelBonePartEntity.resolveParent(entityHit.getEntity());
            if (resolved instanceof LivingEntity target
                    && piercedEntities.add(target.getId())) {
                GangtingTechniques.hitPiercingProjectile(
                        serverLevel, caster, this, target, piercedEntities.size() - 1);
                if (piercedEntities.size()
                        >= GangtingTechniques.maximumPiercedTargets(momentumSnapshot)) {
                    finishLeixiao();
                    discard();
                }
            }
            return;
        }
        if (GangtingTechniques.isPiercingProjectile(spellId)) {
            finishLeixiao();
            discard();
            return;
        }
        activated = true;
        if (ModSpells.DARI_ZHEN.equals(spellId)) {
            beginRiyunImpact(result.getLocation());
            return;
        }
        LivingEntity hitTarget = null;
        if (result instanceof EntityHitResult entityHit) {
            Entity resolved = ModelBonePartEntity.resolveParent(entityHit.getEntity());
            if (resolved instanceof LivingEntity living) {
                hitTarget = living;
            }
        }
        if (level() instanceof ServerLevel serverLevel && getOwner() instanceof LivingEntity caster) {
            if (SolarTechniques.isTechnique(spellId)) {
                SolarTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, result.getLocation(), hitTarget, chargeScale);
            } else if (GangtingTechniques.isTechnique(spellId)) {
                GangtingTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, result.getLocation(), hitTarget,
                        chargeScale, momentumSnapshot);
            } else if (XuanyueTechniques.isTechnique(spellId)) {
                XuanyueTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, result.getLocation(), hitTarget,
                        chargeScale);
            } else if (HanlanTechniques.isTechnique(spellId)) {
                HanlanTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, result.getLocation(), hitTarget,
                        chargeScale);
            } else if (ChangqingTechniques.isTechnique(spellId)) {
                ChangqingTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, result.getLocation(), hitTarget,
                        chargeScale);
            } else if (HealingTechniques.isTechnique(spellId)) {
                HealingTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, hitTarget);
            } else if (ModSpells.SUIGUI_XINGYUN.equals(spellId)) {
                SuiguiXingyun.activateTriggeredSpell(
                        serverLevel, caster, result.getLocation(), chargeScale);
            } else {
                ElementalTechniques.activateTriggeredSpell(
                        serverLevel, caster, spellId, result.getLocation(), hitTarget, chargeScale);
            }
        }
        discard();
    }

    private void finishLeixiao() {
        if (GangtingTechniques.isPiercingProjectile(spellId)
                && level() instanceof ServerLevel serverLevel) {
            Vec3 movement = getDeltaMovement();
            Vec3 direction = movement.lengthSqr() < 1.0E-6D
                    ? new Vec3(0.0D, 0.0D, 1.0D) : movement.normalize();
            GangtingTechniques.finishLeixiao(
                    serverLevel, position(), direction, momentumSnapshot());
        }
    }

    private void beginRiyunImpact(Vec3 impact) {
        activated = true;
        entityData.set(DATA_ACTIVATED, true);
        setPos(impact.x, impact.y, impact.z);
        setDeltaMovement(Vec3.ZERO);
        noPhysics = true;
        hasImpulse = true;
        solarImpactTicks = 0;
        if (getOwner() instanceof LivingEntity caster) {
            solarPowerSnapshot = SolarTechniques.solarPowerFor(caster);
        }
    }

    @Override
    public Entity forcefieldOwner() {
        return getOwner();
    }

    @Override
    public Vec3 forcefieldMovement() {
        return getDeltaMovement();
    }

    @Override
    public float forcefieldBarrierDamage() {
        return SolarTechniques.isTechnique(spellId)
                ? SolarTechniques.triggerBarrierDamage(spellId, chargeScale)
                : GangtingTechniques.isTechnique(spellId)
                ? GangtingTechniques.triggerBarrierDamage(spellId, chargeScale)
                : XuanyueTechniques.isTechnique(spellId)
                ? XuanyueTechniques.triggerBarrierDamage(spellId, chargeScale)
                : HanlanTechniques.isTechnique(spellId)
                ? HanlanTechniques.triggerBarrierDamage(spellId, chargeScale)
                : ChangqingTechniques.isTechnique(spellId)
                ? ChangqingTechniques.triggerBarrierDamage(spellId, chargeScale)
                : ModSpells.SUIGUI_XINGYUN.equals(spellId)
                ? SpellCharge.damage(800.0F, chargeScale)
                : ElementalTechniques.triggerBarrierDamage(spellId, chargeScale);
    }

    public String spellId() {
        String synced = entityData.get(DATA_SPELL_ID);
        return synced.isBlank() ? spellId : synced;
    }

    public float chargeScale() {
        return entityData.get(DATA_CHARGE_SCALE);
    }

    public boolean activated() {
        return entityData.get(DATA_ACTIVATED);
    }

    public int solarImpactTicks() {
        return entityData.get(DATA_SOLAR_IMPACT_TICKS);
    }

    public float solarPowerSnapshot() {
        return solarPowerSnapshot;
    }

    public void setMomentumSnapshot(float momentum) {
        momentumSnapshot = Math.max(0.0F, Math.min(100.0F, momentum));
        entityData.set(DATA_MOMENTUM_SNAPSHOT, momentumSnapshot);
    }

    public float momentumSnapshot() {
        return entityData.get(DATA_MOMENTUM_SNAPSHOT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SpellId", spellId);
        tag.putBoolean("Activated", activated);
        tag.putFloat("ChargeScale", chargeScale);
        tag.putInt("SolarImpactTicks", solarImpactTicks);
        tag.putFloat("SolarPowerSnapshot", solarPowerSnapshot);
        tag.putFloat("FengleiMomentumSnapshot", momentumSnapshot);
        tag.putIntArray("PiercedEntities", piercedEntities.stream()
                .mapToInt(Integer::intValue).toArray());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellId = ModSpells.normalizeId(tag.getString("SpellId"));
        entityData.set(DATA_SPELL_ID, spellId);
        activated = tag.getBoolean("Activated");
        chargeScale = tag.contains("ChargeScale")
                ? SpellCharge.clamp(tag.getFloat("ChargeScale")) : SpellCharge.NORMAL;
        entityData.set(DATA_CHARGE_SCALE, chargeScale);
        solarImpactTicks = Math.max(0, tag.getInt("SolarImpactTicks"));
        entityData.set(DATA_SOLAR_IMPACT_TICKS, solarImpactTicks);
        solarPowerSnapshot = tag.contains("SolarPowerSnapshot")
                ? Math.max(0.0F, Math.min(100.0F, tag.getFloat("SolarPowerSnapshot")))
                : 50.0F;
        momentumSnapshot = tag.contains("FengleiMomentumSnapshot")
                ? Math.max(0.0F, Math.min(100.0F,
                tag.getFloat("FengleiMomentumSnapshot"))) : 0.0F;
        entityData.set(DATA_MOMENTUM_SNAPSHOT, momentumSnapshot);
        piercedEntities.clear();
        for (int entityId : tag.getIntArray("PiercedEntities")) {
            piercedEntities.add(entityId);
        }
        if (activated && ModSpells.DARI_ZHEN.equals(spellId)) {
            entityData.set(DATA_ACTIVATED, true);
            setDeltaMovement(Vec3.ZERO);
            noPhysics = true;
        }
    }
}
