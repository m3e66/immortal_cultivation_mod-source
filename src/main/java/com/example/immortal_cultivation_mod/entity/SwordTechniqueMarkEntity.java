package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Attached delayed cut line or 追命剑印 marker. */
public final class SwordTechniqueMarkEntity extends Entity implements DefensePiercingProjectile {
    public enum Mode {
        CUT,
        FATE;

        static Mode byId(int id) {
            Mode[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(
                    SwordTechniqueMarkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET =
            SynchedEntityData.defineId(
                    SwordTechniqueMarkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BASE_DAMAGE =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHIELD_MULTIPLIER =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DAMAGE_DELAY =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HIT_COUNT =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FINAL =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPELL_KIND =
            SynchedEntityData.defineId(SwordTechniqueMarkEntity.class, EntityDataSerializers.INT);

    private UUID castId;
    private boolean damageApplied;
    private int finalStart = -1;

    public SwordTechniqueMarkEntity(
            EntityType<? extends SwordTechniqueMarkEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static SwordTechniqueMarkEntity createCut(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity target,
            SwordTechniqueProjectileEntity.Kind spellKind,
            float baseDamage,
            float chargeScale,
            float roll,
            int damageDelay,
            float shieldMultiplier) {
        SwordTechniqueMarkEntity mark = ModEntities.SWORD_TECHNIQUE_MARK.get().create(level);
        if (mark == null) {
            return null;
        }
        mark.setMode(Mode.CUT);
        mark.setOwner(owner);
        mark.setTarget(target);
        mark.entityData.set(DATA_SPELL_KIND, spellKind.ordinal());
        mark.entityData.set(DATA_BASE_DAMAGE, Math.max(0.0F, baseDamage));
        mark.entityData.set(DATA_CHARGE, SpellCharge.clamp(chargeScale));
        mark.entityData.set(DATA_ROLL, roll);
        mark.entityData.set(DATA_DAMAGE_DELAY, Math.max(0, damageDelay));
        mark.entityData.set(DATA_SHIELD_MULTIPLIER, Math.max(1.0F, shieldMultiplier));
        mark.moveToTarget(target);
        NpcProfile.markNpcOwnedEffect(mark, owner);
        return mark;
    }

    public static SwordTechniqueMarkEntity createFate(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity target,
            UUID castId) {
        SwordTechniqueMarkEntity mark = ModEntities.SWORD_TECHNIQUE_MARK.get().create(level);
        if (mark == null) {
            return null;
        }
        mark.setMode(Mode.FATE);
        mark.setOwner(owner);
        mark.setTarget(target);
        mark.castId = castId;
        mark.entityData.set(DATA_HIT_COUNT, 1);
        mark.moveToTarget(target);
        NpcProfile.markNpcOwnedEffect(mark, owner);
        return mark;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, Mode.CUT.ordinal());
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_TARGET, Optional.empty());
        builder.define(DATA_ROLL, 0.0F);
        builder.define(DATA_BASE_DAMAGE, 0.0F);
        builder.define(DATA_CHARGE, SpellCharge.NORMAL);
        builder.define(DATA_SHIELD_MULTIPLIER, 1.0F);
        builder.define(DATA_DAMAGE_DELAY, 0);
        builder.define(DATA_HIT_COUNT, 0);
        builder.define(DATA_FINAL, false);
        builder.define(DATA_SPELL_KIND, SwordTechniqueProjectileEntity.Kind.POKONG_JIANHEN.ordinal());
    }

    private void setMode(Mode mode) {
        entityData.set(DATA_MODE, (mode == null ? Mode.CUT : mode).ordinal());
    }

    public Mode mode() {
        return Mode.byId(entityData.get(DATA_MODE));
    }

    private void setOwner(LivingEntity owner) {
        entityData.set(DATA_OWNER, owner == null
                ? Optional.empty() : Optional.of(owner.getUUID()));
    }

    private void setTarget(LivingEntity target) {
        entityData.set(DATA_TARGET, target == null
                ? Optional.empty() : Optional.of(target.getUUID()));
    }

    public float roll() {
        return entityData.get(DATA_ROLL);
    }

    public int hitCount() {
        return entityData.get(DATA_HIT_COUNT);
    }

    public boolean finalBurst() {
        return entityData.get(DATA_FINAL);
    }

    public UUID castId() {
        return castId;
    }

    public void setHitCount(int hitCount) {
        int resolved = Math.max(1, Math.min(3, hitCount));
        entityData.set(DATA_HIT_COUNT, resolved);
        if (resolved >= 3 && !entityData.get(DATA_FINAL)) {
            entityData.set(DATA_FINAL, true);
            finalStart = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = resolve(level, DATA_OWNER);
        LivingEntity target = resolve(level, DATA_TARGET);
        if (owner == null || target == null || !target.isAlive()
                || NpcProfile.isOrphanedNpcEffect(this, owner)) {
            discard();
            return;
        }
        NpcProfile.markNpcOwnedEffect(this, owner);
        moveToTarget(target);

        if (mode() == Mode.CUT) {
            if (!damageApplied && tickCount >= entityData.get(DATA_DAMAGE_DELAY)) {
                damageApplied = true;
                applyDelayedCut(level, owner, target);
            }
            if (tickCount >= 7) {
                discard();
            }
            return;
        }

        if (finalBurst()) {
            if (finalStart < 0) {
                finalStart = tickCount;
            }
            if (tickCount == finalStart) {
                emitFinalBurst(level, target);
            }
            if (tickCount - finalStart >= 9) {
                discard();
            }
        } else if (tickCount >= 65) {
            discard();
        }
    }

    private void applyDelayedCut(
            ServerLevel level, LivingEntity owner, LivingEntity target) {
        float baseDamage = entityData.get(DATA_BASE_DAMAGE);
        if (baseDamage > 0.0F) {
            String spellId = spellKind() == SwordTechniqueProjectileEntity.Kind.POKONG_JIANHEN
                    ? ModSpells.POKONG_JIANHEN : ModSpells.SANJIAN_ZHUIMING;
            float damage = SpellDamageHelper.damage(owner,
                    SpellCharge.damage(baseDamage, entityData.get(DATA_CHARGE)),
                    ModSpells.get(spellId));
            target.hurt(damageSources().indirectMagic(this, owner), damage);
        }
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                22, 0.34D, 0.38D, 0.34D, 0.13D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                16, 0.30D, 0.34D, 0.30D, 0.16D);
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT_GROUND,
                SoundSource.PLAYERS, 0.85F, 1.85F);
    }

    private void emitFinalBurst(ServerLevel level, LivingEntity target) {
        Vec3Helper.emitThreeSwordLines(level, target.getBoundingBox().getCenter());
        level.sendParticles(ParticleTypes.FLASH,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                2, 0.08D, 0.08D, 0.08D, 0.0D);
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                52, 0.85D, 0.80D, 0.85D, 0.24D);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.35F, 1.18F);
    }

    private SwordTechniqueProjectileEntity.Kind spellKind() {
        return SwordTechniqueProjectileEntity.Kind.byId(entityData.get(DATA_SPELL_KIND));
    }

    private LivingEntity resolve(
            ServerLevel level, EntityDataAccessor<Optional<UUID>> accessor) {
        Optional<UUID> uuid = entityData.get(accessor);
        return uuid.isPresent() && level.getEntity(uuid.get()) instanceof LivingEntity living
                ? living : null;
    }

    private void moveToTarget(LivingEntity target) {
        double y = mode() == Mode.FATE
                ? target.getY() + target.getBbHeight() + 0.65D
                : target.getY() + target.getBbHeight() * 0.55D;
        setPos(target.getX(), y, target.getZ());
    }

    @Override
    public float defenseBypass() {
        return mode() == Mode.CUT ? 0.10F : 0.0F;
    }

    @Override
    public float shieldDamageMultiplier() {
        return entityData.get(DATA_SHIELD_MULTIPLIER);
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
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", mode().ordinal());
        entityData.get(DATA_OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        entityData.get(DATA_TARGET).ifPresent(uuid -> tag.putUUID("Target", uuid));
        tag.putFloat("Roll", entityData.get(DATA_ROLL));
        tag.putFloat("BaseDamage", entityData.get(DATA_BASE_DAMAGE));
        tag.putFloat("Charge", entityData.get(DATA_CHARGE));
        tag.putFloat("ShieldMultiplier", entityData.get(DATA_SHIELD_MULTIPLIER));
        tag.putInt("DamageDelay", entityData.get(DATA_DAMAGE_DELAY));
        tag.putInt("HitCount", entityData.get(DATA_HIT_COUNT));
        tag.putBoolean("Final", entityData.get(DATA_FINAL));
        tag.putInt("SpellKind", entityData.get(DATA_SPELL_KIND));
        tag.putBoolean("DamageApplied", damageApplied);
        tag.putInt("FinalStart", finalStart);
        if (castId != null) {
            tag.putUUID("CastId", castId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setMode(Mode.byId(tag.getInt("Mode")));
        entityData.set(DATA_OWNER, tag.hasUUID("Owner")
                ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(DATA_TARGET, tag.hasUUID("Target")
                ? Optional.of(tag.getUUID("Target")) : Optional.empty());
        entityData.set(DATA_ROLL, tag.getFloat("Roll"));
        entityData.set(DATA_BASE_DAMAGE, tag.getFloat("BaseDamage"));
        entityData.set(DATA_CHARGE, tag.contains("Charge")
                ? tag.getFloat("Charge") : SpellCharge.NORMAL);
        entityData.set(DATA_SHIELD_MULTIPLIER, tag.contains("ShieldMultiplier")
                ? tag.getFloat("ShieldMultiplier") : 1.0F);
        entityData.set(DATA_DAMAGE_DELAY, tag.getInt("DamageDelay"));
        entityData.set(DATA_HIT_COUNT, tag.getInt("HitCount"));
        entityData.set(DATA_FINAL, tag.getBoolean("Final"));
        entityData.set(DATA_SPELL_KIND, tag.getInt("SpellKind"));
        damageApplied = tag.getBoolean("DamageApplied");
        finalStart = tag.getInt("FinalStart");
        castId = tag.hasUUID("CastId") ? tag.getUUID("CastId") : null;
    }

    /** Keeps the mark entity small while reusing the existing server-side line helper. */
    private static final class Vec3Helper {
        private static void emitThreeSwordLines(ServerLevel level, net.minecraft.world.phys.Vec3 center) {
            for (int line = 0; line < 3; line++) {
                double angle = Math.PI * 2.0D * line / 3.0D + 0.35D;
                net.minecraft.world.phys.Vec3 direction = new net.minecraft.world.phys.Vec3(
                        Math.cos(angle), line == 1 ? 0.42D : -0.22D, Math.sin(angle)).normalize();
                com.example.immortal_cultivation_mod.spell.ElementalSpellVfx.line(
                        level, center.subtract(direction.scale(1.7D)),
                        center.add(direction.scale(1.7D)),
                        ModParticles.METHOD_QI_METAL.get(), 0.09D);
            }
        }
    }
}
