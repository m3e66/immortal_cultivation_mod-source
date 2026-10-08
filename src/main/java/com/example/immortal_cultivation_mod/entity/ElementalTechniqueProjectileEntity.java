package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ElementalSpellVfx;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared real projectile implementation for the elemental technique set. */
public final class ElementalTechniqueProjectileEntity extends ThrowableItemProjectile
        implements DefensePiercingProjectile {
    private static final int MAX_LIFETIME_TICKS = 20 * 30;
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(ElementalTechniqueProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE =
            SynchedEntityData.defineId(
                    ElementalTechniqueProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final Map<UUID, Mark> BLADE_MARKS = new ConcurrentHashMap<>();

    public enum Kind {
        JINFENG_ZHI,
        WANREN_CHUANXIN,
        YANBAO_DAN,
        SHUIREN_LIANZHAN,
        BINGZHUI_YU;

        static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private UUID targetId;
    private UUID castId;
    private int launchDelay;
    private boolean detonated;
    private final Set<UUID> hitEntities = new HashSet<>();
    private int penetrationsUsed;

    public ElementalTechniqueProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ElementalTechniqueProjectileEntity(Level level, LivingEntity owner, Kind kind) {
        super(ModEntities.ELEMENTAL_TECHNIQUE_PROJECTILE.get(), owner, level);
        setKind(kind);
        setNoGravity(true);
        NpcProfile.markNpcOwnedEffect(this, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, Kind.JINFENG_ZHI.ordinal());
        builder.define(DATA_CHARGE_SCALE, SpellCharge.NORMAL);
    }

    public Kind kind() {
        // ThrowableItemProjectile asks for its default item while Entity is still
        // constructing, before this.entityData has been assigned.
        return entityData == null
                ? Kind.JINFENG_ZHI
                : Kind.byId(entityData.get(DATA_KIND));
    }

    public void setKind(Kind kind) {
        Kind resolved = kind == null ? Kind.JINFENG_ZHI : kind;
        entityData.set(DATA_KIND, resolved.ordinal());
        setItem(visualItem(resolved).getDefaultInstance());
    }

    public void setChargeScale(float chargeScale) {
        entityData.set(DATA_CHARGE_SCALE, SpellCharge.clamp(chargeScale));
    }

    public float chargeScale() {
        return SpellCharge.clamp(entityData.get(DATA_CHARGE_SCALE));
    }

    public boolean fullyCharged() {
        return SpellCharge.isFull(chargeScale());
    }

    public void configureBlade(UUID sharedCastId, LivingEntity target, int delayTicks) {
        castId = sharedCastId;
        targetId = target == null ? null : target.getUUID();
        launchDelay = Math.max(0, delayTicks);
        setDeltaMovement(Vec3.ZERO);
    }

    public int launchDelay() {
        return launchDelay;
    }

    @Override
    protected Item getDefaultItem() {
        // This method is invoked from the superclass constructor, so it must not
        // depend on synchronized entity data. setKind supplies the real visual.
        return visualItem(Kind.JINFENG_ZHI);
    }

    private static Item visualItem(Kind kind) {
        return switch (kind) {
            case JINFENG_ZHI -> ModItems.SPELL_MODEL_METAL_NEEDLE.get();
            case WANREN_CHUANXIN -> ModItems.SPELL_MODEL_METAL_BLADE.get();
            case SHUIREN_LIANZHAN -> ModItems.SPELL_MODEL_WATER_CRESCENT.get();
            case BINGZHUI_YU -> ModItems.SPELL_MODEL_ICE_SPIKE.get();
            case YANBAO_DAN -> Items.FIRE_CHARGE;
        };
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

        if (launchDelay > 0) {
            launchDelay--;
            setDeltaMovement(Vec3.ZERO);
            super.tick();
            if (!level().isClientSide && launchDelay == 0) {
                launchBlade();
            }
            spawnTrail();
            return;
        }

        if (!level().isClientSide && kind() == Kind.WANREN_CHUANXIN) {
            homeBlade();
        }
        super.tick();
        if (!level().isClientSide && isAlive()
                && ForcefieldCenterBehavior.interceptProjectileImpact(this, position())) {
            return;
        }
        spawnTrail();
        if (!level().isClientSide && tickCount > MAX_LIFETIME_TICKS) {
            impactParticles(position());
            discard();
        }
    }

    private void launchBlade() {
        LivingEntity target = resolveTarget();
        Vec3 aim = target == null
                ? (getOwner() == null ? new Vec3(0.0D, 0.0D, 1.0D) : getOwner().getLookAngle())
                : target.getEyePosition().subtract(position()).normalize();
        shoot(aim.x, aim.y, aim.z, fullyCharged() ? 2.65F : 2.25F, 0.0F);
    }

    private void homeBlade() {
        if (castId == null) {
            return;
        }
        Mark mark = BLADE_MARKS.get(castId);
        if (mark == null || mark.expiresAt() < level().getGameTime()) {
            BLADE_MARKS.remove(castId);
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(mark.targetId()) instanceof LivingEntity target)
                || !target.isAlive()) {
            return;
        }
        double speed = fullyCharged() ? 2.65D : 2.25D;
        double homing = fullyCharged() ? 0.38D : 0.22D;
        Vec3 desired = target.getEyePosition().subtract(position()).normalize().scale(speed);
        setDeltaMovement(getDeltaMovement().scale(1.0D - homing)
                .add(desired.scale(homing)).normalize().scale(speed));
    }

    private LivingEntity resolveTarget() {
        return targetId != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(targetId) instanceof LivingEntity living ? living : null;
    }

    private void spawnTrail() {
        if (!level().isClientSide) {
            return;
        }
        ParticleOptions qi = switch (kind()) {
            case JINFENG_ZHI, WANREN_CHUANXIN -> ModParticles.METHOD_QI_METAL.get();
            case YANBAO_DAN -> ModParticles.METHOD_QI_FIRE.get();
            case SHUIREN_LIANZHAN -> ModParticles.METHOD_QI_WATER.get();
            case BINGZHUI_YU -> ModParticles.METHOD_QI_ICE.get();
        };
        Vec3 motion = getDeltaMovement();
        if (launchDelay > 0 && kind() == Kind.WANREN_CHUANXIN) {
            Vec3 inward = position().subtract(getOwner() == null
                    ? position() : getOwner().getEyePosition()).normalize().scale(-0.035D);
            level().addParticle(qi,
                    getX() + (random.nextDouble() - 0.5D) * 0.35D,
                    getY() + (random.nextDouble() - 0.5D) * 0.35D,
                    getZ() + (random.nextDouble() - 0.5D) * 0.35D,
                    inward.x, inward.y, inward.z);
            return;
        }
        if (motion.lengthSqr() < 1.0E-5D) {
            return;
        }
        Vec3 backward = motion.normalize().scale(-1.0D);
        int points = switch (kind()) {
            case JINFENG_ZHI -> 4;
            case WANREN_CHUANXIN -> 7;
            case YANBAO_DAN -> 5;
            case SHUIREN_LIANZHAN -> 4;
            case BINGZHUI_YU -> 5;
        };
        double spacing = switch (kind()) {
            case JINFENG_ZHI -> 0.12D;
            case WANREN_CHUANXIN -> 0.22D;
            case YANBAO_DAN -> 0.16D;
            case SHUIREN_LIANZHAN -> 0.18D;
            case BINGZHUI_YU -> 0.16D;
        };
        for (int i = 0; i < points; i++) {
            Vec3 point = position().add(backward.scale(i * spacing));
            level().addParticle(qi, point.x, point.y, point.z,
                    -motion.x * 0.008D, -motion.y * 0.008D, -motion.z * 0.008D);
        }
        switch (kind()) {
            case JINFENG_ZHI -> {
                if ((tickCount & 1) == 0) {
                    Vec3 point = position().add(backward.scale(0.25D));
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            point.x, point.y, point.z,
                            (random.nextDouble() - 0.5D) * 0.025D,
                            (random.nextDouble() - 0.5D) * 0.025D,
                            (random.nextDouble() - 0.5D) * 0.025D);
                }
            }
            case WANREN_CHUANXIN -> level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX() + (random.nextDouble() - 0.5D) * 0.08D,
                    getY() + (random.nextDouble() - 0.5D) * 0.08D,
                    getZ() + (random.nextDouble() - 0.5D) * 0.08D,
                    backward.x * 0.025D, backward.y * 0.025D, backward.z * 0.025D);
            case YANBAO_DAN -> {
                level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(),
                        -motion.x * 0.035D, -motion.y * 0.035D, -motion.z * 0.035D);
                if ((tickCount & 1) == 0) {
                    level().addParticle(ParticleTypes.SMOKE,
                            getX() + backward.x * 0.25D,
                            getY() + backward.y * 0.25D,
                            getZ() + backward.z * 0.25D,
                            backward.x * 0.015D, 0.02D, backward.z * 0.015D);
                }
            }
            case SHUIREN_LIANZHAN -> level().addParticle(ParticleTypes.FALLING_WATER,
                    getX(), getY(), getZ(), backward.x * 0.02D, -0.02D, backward.z * 0.02D);
            case BINGZHUI_YU -> level().addParticle(ParticleTypes.SNOWFLAKE,
                    getX(), getY(), getZ(), backward.x * 0.015D, -0.015D, backward.z * 0.015D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (launchDelay > 0) {
            return false;
        }
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        return resolved instanceof LivingEntity
                && !hitEntities.contains(resolved.getUUID())
                && NpcProfile.canNpcEffectHit(this, getOwner(), resolved)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity resolved = ModelBonePartEntity.resolveParent(result.getEntity());
        if (!(resolved instanceof LivingEntity target)) {
            return;
        }
        boolean wasBurning = target.isOnFire();
        boolean guarded = target.isBlocking() || target.getArmorValue() > 0;
        float chargedBaseDamage = SpellCharge.damage(baseDamage(), chargeScale());
        if (kind() == Kind.JINFENG_ZHI && guarded) {
            chargedBaseDamage *= fullyCharged() ? 1.50F : 1.25F;
        }
        float damage = SpellDamageHelper.damage(
                getOwner(), chargedBaseDamage, ModSpells.get(spellId()));
        boolean independentMultiHit = isIndependentMultiHit();
        int previousInvulnerability = target.invulnerableTime;
        if (independentMultiHit) {
            // These techniques explicitly deal damage once per blade/spike. Vanilla's
            // short hurt-invulnerability window would otherwise swallow most of them.
            target.invulnerableTime = 0;
        }
        boolean hurt = target.hurt(damageSources().thrown(this, getOwner()), damage);
        if (!hurt) {
            if (independentMultiHit) {
                target.invulnerableTime = previousInvulnerability;
            }
            return;
        }
        hitEntities.add(target.getUUID());
        switch (kind()) {
            case JINFENG_ZHI -> emitNeedleImpact(target, guarded);
            case WANREN_CHUANXIN -> {
                if (castId != null) {
                    BLADE_MARKS.entrySet().removeIf(
                            entry -> entry.getValue().expiresAt() < level().getGameTime());
                    Mark mark = BLADE_MARKS.compute(castId, (ignored, previous) ->
                            new Mark(target.getUUID(), level().getGameTime() + 100L,
                                    previous == null ? 1 : previous.hits() + 1));
                    emitBladeImpact(target, mark.hits());
                }
            }
            case YANBAO_DAN -> target.igniteForSeconds(fullyCharged() ? 5 : 3);
            case SHUIREN_LIANZHAN -> {
                target.clearFire();
                if (getOwner() instanceof LivingEntity caster) {
                    com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                            .applyDampFromWater(caster, target, 140);
                }
                if (wasBurning && level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                            18, 0.35D, 0.45D, 0.35D, 0.035D);
                }
            }
            case BINGZHUI_YU -> {
                applyIceSlow(target);
                if (fullyCharged()) {
                    applyChargedIceImpact(target);
                }
            }
            default -> {
            }
        }
    }

    private void emitNeedleImpact(LivingEntity target, boolean guarded) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 motion = getDeltaMovement().normalize();
        Vec3 side = motion.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 up = side.cross(motion).normalize();
        ElementalSpellVfx.line(serverLevel, center.subtract(side.scale(0.34D)),
                center.add(side.scale(0.34D)), ModParticles.METHOD_QI_METAL.get(), 0.07D);
        ElementalSpellVfx.line(serverLevel, center.subtract(up.scale(0.34D)),
                center.add(up.scale(0.34D)), ParticleTypes.END_ROD, 0.07D);
        ElementalSpellVfx.line(serverLevel, center.subtract(motion.scale(0.35D)),
                center.add(motion.scale(0.85D)), ModParticles.METHOD_QI_METAL.get(), 0.10D);
        if (guarded) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    center.x, center.y, center.z, 14, 0.28D, 0.28D, 0.28D, 0.16D);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    center.x, center.y, center.z, 10, 0.22D, 0.22D, 0.22D, 0.13D);
        }
    }

    private void emitBladeImpact(LivingEntity target, int hits) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = target.getBoundingBox().getCenter();
        serverLevel.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                center.x, center.y, center.z, 13, 0.30D, 0.36D, 0.30D, 0.11D);
        if (hits == 1) {
            for (int i = 0; i < 18; i++) {
                double angle = Math.PI * 2.0D * i / 18.0D;
                Vec3 point = center.add(Math.cos(angle) * 0.68D,
                        target.getBbHeight() * 0.16D, Math.sin(angle) * 0.68D);
                serverLevel.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                        point.x, point.y, point.z, 1, 0.005D, 0.005D, 0.005D, 0.0D);
            }
            ElementalSpellVfx.line(serverLevel,
                    center.add(-0.26D, -0.44D, 0.0D), center.add(0.26D, 0.44D, 0.0D),
                    ParticleTypes.END_ROD, 0.10D);
        } else if (hits >= 6) {
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2.0D * i / 6.0D;
                Vec3 start = center.add(Math.cos(angle) * 1.8D,
                        (i % 2 == 0 ? 0.8D : -0.4D), Math.sin(angle) * 1.8D);
                ElementalSpellVfx.line(serverLevel, start, center,
                        ModParticles.METHOD_QI_METAL.get(), 0.16D);
            }
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void applyIceSlow(LivingEntity target) {
        MobEffectInstance current = target.getEffect(ModEffects.TECHNIQUE_SLOW);
        boolean dampSynergy = getOwner() instanceof LivingEntity caster
                && com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing.hasMethod(caster)
                && (com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing.isDamp(target)
                || com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                .isStandingInWater(target));
        int perHit = dampSynergy ? (fullyCharged() ? 14 : 10)
                : fullyCharged() ? 8 : 5;
        int maximum = dampSynergy ? 50 : fullyCharged() ? 40 : 30;
        int currentPercent = current == null ? 0 : current.getAmplifier() + 1;
        int amplifier = Math.min(maximum, currentPercent + perHit) - 1;
        int duration = current == null ? 60 : Math.max(60, current.getDuration());
        target.addEffect(new MobEffectInstance(
                ModEffects.TECHNIQUE_SLOW, duration, amplifier, false, true, true));
        if (dampSynergy) {
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 20,
                    target.getTicksFrozen() + 35));
        }
    }

    private void applyChargedIceImpact(LivingEntity directTarget) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = directTarget.getBoundingBox().getCenter();
        AABB area = new AABB(center, center).inflate(1.35D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class, area,
                candidate -> candidate != directTarget && candidate.isAlive()
                        && NpcProfile.canNpcEffectHit(this, getOwner(), candidate))) {
            int previousInvulnerability = target.invulnerableTime;
            target.invulnerableTime = 0;
            if (target.hurt(damageSources().thrown(this, getOwner()),
                    SpellDamageHelper.damage(getOwner(),
                            SpellCharge.damage(baseDamage(), chargeScale()),
                            ModSpells.get(spellId())))) {
                applyIceSlow(target);
            } else {
                target.invulnerableTime = previousInvulnerability;
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (kind() == Kind.YANBAO_DAN) {
            detonate(result.getLocation());
        } else if (result.getType() == HitResult.Type.ENTITY && canPenetrateAnotherTarget()) {
            penetrationsUsed++;
            Vec3 motion = getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-8D) {
                setPos(position().add(motion.normalize().scale(0.20D)));
            }
            impactParticles(result.getLocation());
        } else {
            impactParticles(result.getLocation());
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
    }

    private void detonate(Vec3 center) {
        if (detonated || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        detonated = true;
        Entity owner = getOwner();
        double radius = fullyCharged() ? 4.0D : 3.0D;
        AABB bounds = new AABB(center, center).inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != owner && target.isAlive()
                        && NpcProfile.canNpcEffectHit(this, owner, target))) {
            double distance = target.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float splash = (float) (35.0D * Math.max(0.0D, 1.0D - distance / radius));
            if (splash > 0.0F) {
                // The direct-hit target must also receive the advertised additional
                // explosion damage during the same tick.
                int previousInvulnerability = target.invulnerableTime;
                target.invulnerableTime = 0;
                if (target.hurt(damageSources().indirectMagic(this, owner),
                        SpellDamageHelper.damage(owner,
                                SpellCharge.damage(splash, chargeScale()),
                                ModSpells.get(ModSpells.YANBAO_DAN)))) {
                    target.igniteForSeconds(fullyCharged() ? 5 : 3);
                    if (fullyCharged()) {
                        Vec3 away = target.getBoundingBox().getCenter().subtract(center);
                        if (away.lengthSqr() > 1.0E-5D) {
                            away = away.normalize();
                            target.push(away.x * 1.15D, 0.28D, away.z * 1.15D);
                        }
                    }
                } else {
                    target.invulnerableTime = previousInvulnerability;
                }
            }
        }
        ElementalTechniqueAreaEntity.damageFireSensitiveAreas(
                serverLevel, center, radius + 1.0D, 30);
        com.example.immortal_cultivation_mod.network.SpellRibbonPayload.send(serverLevel,"fire_burst",center,new Vec3(0,0,1),-1,24,(float)Math.max(.8,radius/3));
        SpellImpactParticles.fireExplosion(serverLevel, center, radius);
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.15F);
        discard();
    }

    private void impactParticles(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (kind()) {
            case JINFENG_ZHI, WANREN_CHUANXIN -> {
                serverLevel.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                        center.x, center.y, center.z, 16, 0.18D, 0.18D, 0.18D, 0.08D);
                serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.7F, 1.8F);
            }
            case SHUIREN_LIANZHAN -> SpellImpactParticles.waterSlash(
                    serverLevel, center, getDeltaMovement());
            case BINGZHUI_YU -> {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        center.x, center.y, center.z, 22, 0.32D, 0.28D, 0.32D, 0.09D);
                SpellImpactParticles.shockRing(serverLevel,
                        center.add(0.0D, 0.06D, 0.0D), ModParticles.METHOD_QI_ICE.get(),
                        0.9D, 18, 0.07D);
            }
            default -> {
            }
        }
    }

    private float baseDamage() {
        return switch (kind()) {
            case JINFENG_ZHI -> 28.0F;
            case WANREN_CHUANXIN -> 110.0F;
            case YANBAO_DAN -> 65.0F;
            case SHUIREN_LIANZHAN -> 12.0F;
            case BINGZHUI_YU -> 30.0F;
        };
    }

    private boolean isIndependentMultiHit() {
        return kind() == Kind.WANREN_CHUANXIN
                || kind() == Kind.SHUIREN_LIANZHAN
                || kind() == Kind.BINGZHUI_YU;
    }

    public String spellId() {
        return switch (kind()) {
            case JINFENG_ZHI -> ModSpells.JINFENG_ZHI;
            case WANREN_CHUANXIN -> ModSpells.WANREN_CHUANXIN;
            case YANBAO_DAN -> ModSpells.YANBAO_DAN;
            case SHUIREN_LIANZHAN -> ModSpells.SHUIREN_LIANZHAN;
            case BINGZHUI_YU -> ModSpells.BINGZHUI_YU;
        };
    }

    @Override
    public float defenseBypass() {
        return switch (kind()) {
            case JINFENG_ZHI -> fullyCharged() ? 0.20F : 0.10F;
            case WANREN_CHUANXIN -> fullyCharged() ? 0.25F : 0.15F;
            default -> 0.0F;
        };
    }

    @Override
    public float shieldDamageMultiplier() {
        return kind() == Kind.JINFENG_ZHI
                ? (fullyCharged() ? 1.50F : 1.25F) : 1.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TechniqueKind", kind().ordinal());
        tag.putInt("LaunchDelay", launchDelay);
        tag.putBoolean("Detonated", detonated);
        tag.putFloat("ChargeScale", chargeScale());
        tag.putInt("PenetrationsUsed", penetrationsUsed);
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        if (castId != null) {
            tag.putUUID("Cast", castId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(Kind.byId(tag.getInt("TechniqueKind")));
        launchDelay = Math.max(0, tag.getInt("LaunchDelay"));
        detonated = tag.getBoolean("Detonated");
        setChargeScale(tag.contains("ChargeScale")
                ? tag.getFloat("ChargeScale") : SpellCharge.NORMAL);
        penetrationsUsed = Math.max(0, tag.getInt("PenetrationsUsed"));
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        castId = tag.hasUUID("Cast") ? tag.getUUID("Cast") : null;
    }

    private record Mark(UUID targetId, long expiresAt, int hits) {
    }

    private boolean canPenetrateAnotherTarget() {
        return fullyCharged() && penetrationsUsed < 1
                && (kind() == Kind.JINFENG_ZHI || kind() == Kind.SHUIREN_LIANZHAN);
    }
}
