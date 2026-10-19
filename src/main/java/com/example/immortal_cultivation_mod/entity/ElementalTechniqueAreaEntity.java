package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ElementalSpellVfx;
import com.example.immortal_cultivation_mod.spell.GangtingTechniques;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Persistent and telegraphed portions of the elemental technique set. */
public final class ElementalTechniqueAreaEntity extends Entity implements ForcefieldInterceptable {
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(ElementalTechniqueAreaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
            SynchedEntityData.defineId(ElementalTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
            SynchedEntityData.defineId(ElementalTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(
                    ElementalTechniqueAreaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE =
            SynchedEntityData.defineId(
                    ElementalTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOMENTUM_SNAPSHOT =
            SynchedEntityData.defineId(
                    ElementalTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);

    public enum Kind {
        JINGJI_TUSHENG,
        WANTENG_JIAOSHA,
        CHIYAN_LONGJUAN,
        DIYAN_QIANG,
        SHANBENG_YIN,
        CANGLANG_CHONG,
        WULEI_HONGDING,
        BINGZHUI_YU,
        HANYU_FENGDI,
        TIANGUANG_GUANSHA,
        TIANFENG_ZHUI,
        JIUXIAO_FENGLEI_YU;

        static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private final Set<UUID> hitEntities = new HashSet<>();
    private final Map<UUID, Integer> insideTicks = new HashMap<>();
    private final Map<UUID, Double> forcedFallStart = new HashMap<>();
    private UUID ownerId;
    private UUID targetId;
    private int lifePenalty;
    private int terrainBlocksDestroyed;
    private boolean primaryTriggered;
    private boolean targetLifted;

    public ElementalTechniqueAreaEntity(EntityType<? extends ElementalTechniqueAreaEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static ElementalTechniqueAreaEntity create(
            ServerLevel level,
            LivingEntity owner,
            Kind kind,
            Vec3 position,
            Vec3 direction,
            LivingEntity target) {
        return create(level, owner, kind, position, direction, target, SpellCharge.NORMAL);
    }

    public static ElementalTechniqueAreaEntity create(
            ServerLevel level,
            LivingEntity owner,
            Kind kind,
            Vec3 position,
            Vec3 direction,
            LivingEntity target,
            float chargeScale) {
        ElementalTechniqueAreaEntity effect = ModEntities.ELEMENTAL_TECHNIQUE_AREA.get().create(level);
        if (effect == null) {
            return null;
        }
        effect.setKind(kind);
        effect.ownerId = owner.getUUID();
        effect.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        effect.targetId = target == null ? null : target.getUUID();
        effect.setChargeScale(chargeScale);
        effect.setDirection(direction);
        effect.moveTo(position.x, position.y, position.z,
                (float) Math.toDegrees(Math.atan2(-effect.direction().x, effect.direction().z)), 0.0F);
        NpcProfile.markNpcOwnedEffect(effect, owner);
        return effect;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.JINGJI_TUSHENG.ordinal());
        builder.define(DATA_DIRECTION_X, 0.0F);
        builder.define(DATA_DIRECTION_Z, 1.0F);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_CHARGE_SCALE, SpellCharge.NORMAL);
        builder.define(DATA_MOMENTUM_SNAPSHOT, 0.0F);
    }

    public Kind kind() {
        return Kind.byId(entityData.get(DATA_KIND));
    }

    public void setKind(Kind kind) {
        entityData.set(DATA_KIND, (kind == null ? Kind.JINGJI_TUSHENG : kind).ordinal());
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

    public void setMomentumSnapshot(float momentum) {
        entityData.set(DATA_MOMENTUM_SNAPSHOT,
                Mth.clamp(momentum, 0.0F, 100.0F));
    }

    public float momentumSnapshot() {
        return Mth.clamp(entityData.get(DATA_MOMENTUM_SNAPSHOT), 0.0F, 100.0F);
    }

    public Vec3 direction() {
        Vec3 direction = new Vec3(
                entityData.get(DATA_DIRECTION_X), 0.0D, entityData.get(DATA_DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    public void setDirection(Vec3 direction) {
        Vec3 horizontal = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        entityData.set(DATA_DIRECTION_X, (float) horizontal.x);
        entityData.set(DATA_DIRECTION_Z, (float) horizontal.z);
    }

    public ItemStack modelStack() {
        Item item = switch (kind()) {
            case JINGJI_TUSHENG -> ModItems.SPELL_MODEL_THORNS.get();
            case DIYAN_QIANG -> ModItems.SPELL_MODEL_EARTH_SPEAR.get();
            case CANGLANG_CHONG -> ModItems.SPELL_MODEL_WAVE.get();
            default -> null;
        };
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    public float modelScale() {
        return switch (kind()) {
            case JINGJI_TUSHENG -> 2.5F;
            case DIYAN_QIANG -> 2.4F;
            case CANGLANG_CHONG -> 5.0F;
            default -> 1.0F;
        };
    }

    public float modelGrowth(float partialTick) {
        float age = tickCount + partialTick;
        float growth = switch (kind()) {
            case JINGJI_TUSHENG -> clamp01((age - 2.0F) / 5.0F);
            case DIYAN_QIANG -> clamp01((age - 2.0F) / 5.0F);
            case CANGLANG_CHONG -> clamp01(age / 4.0F);
            default -> 1.0F;
        };
        int remaining = duration() - tickCount;
        if ((kind() == Kind.JINGJI_TUSHENG || kind() == Kind.WANTENG_JIAOSHA)
                && remaining < 8) {
            growth *= clamp01(remaining / 8.0F);
        }
        return Math.max(0.02F, growth);
    }

    public int visualDuration() {
        return duration();
    }

    public double modelYOffset(float partialTick) {
        float age = tickCount + partialTick;
        return switch (kind()) {
            case JINGJI_TUSHENG -> -0.9D + Math.min(0.9D, age * 0.18D);
            case DIYAN_QIANG -> -1.8D + Math.min(1.8D, age * 0.38D);
            default -> 0.0D;
        };
    }

    @Override
    public Entity forcefieldOwner() {
        return resolveOwner();
    }

    @Override
    public Vec3 forcefieldMovement() {
        return switch (kind()) {
            case CANGLANG_CHONG -> direction().scale(fullyCharged() ? 0.90D : 0.70D);
            case CHIYAN_LONGJUAN -> direction().scale(fullyCharged() ? 0.12D : 0.08D);
            default -> Vec3.ZERO;
        };
    }

    @Override
    public float forcefieldBarrierDamage() {
        float base = switch (kind()) {
            case SHANBENG_YIN -> 900.0F;
            case TIANGUANG_GUANSHA -> 950.0F;
            case TIANFENG_ZHUI -> 600.0F;
            case JIUXIAO_FENGLEI_YU -> 650.0F;
            default -> 180.0F;
        };
        return SpellCharge.damage(base, chargeScale());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) {
            tickClientVfx();
            return;
        }
        LivingEntity owner = resolveOwner();
        if (!(level() instanceof ServerLevel level) || owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        NpcProfile.markNpcOwnedEffect(this, owner);
        if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
            discard();
            return;
        }

        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.TECHNIQUES, owner, () -> {
            switch (kind()) {
                case JINGJI_TUSHENG -> tickThorns(level, owner);
                case WANTENG_JIAOSHA -> tickVines(level, owner);
                case CHIYAN_LONGJUAN -> tickFireTornado(level, owner);
                case DIYAN_QIANG -> tickEarthSpear(level, owner);
                case SHANBENG_YIN -> tickMountainSeal(level, owner);
                case CANGLANG_CHONG -> tickWave(level, owner);
                case WULEI_HONGDING -> tickFiveLightning(level, owner);
                case BINGZHUI_YU -> tickIceRain(level, owner);
                case HANYU_FENGDI -> tickIcePrison(level, owner);
                case TIANGUANG_GUANSHA -> tickLightPillar(level, owner);
                case TIANFENG_ZHUI -> tickWindDrop(level, owner);
                case JIUXIAO_FENGLEI_YU -> tickFengleiDomain(level, owner);
            }
        });

        if (tickCount + lifePenalty >= duration()) {
            if (kind() == Kind.CHIYAN_LONGJUAN) {
                throwTornadoTargets(level, owner);
            } else if (kind() == Kind.CANGLANG_CHONG) {
                finishWave(level);
            }
            emitEndingVfx(level);
            discard();
        }
    }

    private void tickClientVfx() {
        switch (kind()) {
            case JINGJI_TUSHENG -> clientThornVfx();
            case WANTENG_JIAOSHA -> clientVineVfx();
            case CHIYAN_LONGJUAN -> clientTornadoVfx();
            case DIYAN_QIANG -> clientEarthSpearVfx();
            case SHANBENG_YIN -> clientMountainVfx();
            case CANGLANG_CHONG -> clientWaveVfx();
            case WULEI_HONGDING -> clientLightningVfx();
            case BINGZHUI_YU -> clientIceRainVfx();
            case HANYU_FENGDI -> clientFrozenDomainVfx();
            case TIANGUANG_GUANSHA -> clientLightPillarVfx();
            case TIANFENG_ZHUI -> clientWindDropVfx();
            case JIUXIAO_FENGLEI_YU -> clientFengleiDomainVfx();
        }
    }

    private void clientThornVfx() {
        double radiusScale = fullyCharged() ? 1.20D : 1.0D;
        if (tickCount <= 6) {
            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = (0.4D + random.nextDouble() * 1.8D) * radiusScale;
                clientParticle(ModParticles.METHOD_QI_WOOD.get(),
                        position().add(Math.cos(angle) * radius, 0.16D,
                                Math.sin(angle) * radius),
                        new Vec3(-Math.cos(angle) * 0.025D, -0.07D,
                                -Math.sin(angle) * 0.025D));
            }
        }
        if (tickCount == 6) {
            for (int i = 0; i < 22; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
                clientParticle(i % 3 == 0 ? ParticleTypes.CRIT : ModParticles.METHOD_QI_WOOD.get(),
                        position().add(0.0D, 0.25D, 0.0D),
                        radial.scale(0.10D + random.nextDouble() * 0.09D)
                                .add(0.0D, 0.10D + random.nextDouble() * 0.15D, 0.0D));
            }
        }
        if (tickCount > 6 && tickCount % 3 == 0) {
            forEachVisualTarget(fullyCharged() ? 3.1D : 2.6D, 3.2D, target -> {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                clientParticle(ModParticles.METHOD_QI_WOOD.get(),
                        target.position().add(Math.cos(angle) * 0.34D,
                                0.08D + random.nextDouble() * 0.28D,
                                Math.sin(angle) * 0.34D),
                        new Vec3(0.0D, 0.015D, 0.0D));
            });
        }
    }

    private void clientVineVfx() {
        double domainScale = fullyCharged() ? 1.20D : 1.0D;
        if (tickCount < 10) {
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = (1.5D + random.nextDouble() * 3.5D) * domainScale;
                Vec3 point = position().add(Math.cos(angle) * radius, 0.14D,
                        Math.sin(angle) * radius);
                clientParticle(i % 3 == 0 ? ParticleTypes.COMPOSTER
                                : ModParticles.METHOD_QI_WOOD.get(),
                        point, position().add(0.0D, 0.08D, 0.0D)
                                .subtract(point).normalize().scale(0.065D));
            }
            return;
        }
        for (int i = 0; i < 9; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * 4.8D * domainScale;
            Vec3 point = position().add(Math.cos(angle) * radius,
                    random.nextDouble() * 2.3D, Math.sin(angle) * radius);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.07D, Math.cos(angle)).scale(0.045D);
            clientParticle(i % 4 == 0 ? ParticleTypes.COMPOSTER
                            : ModParticles.METHOD_QI_WOOD.get(), point, tangent);
        }
        if (tickCount % 3 == 0) {
            forEachVisualTarget(fullyCharged() ? 6.0D : 5.0D, 4.5D, target -> {
                Vec3 from = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
                Vec3 to = position().add(0.0D, 0.35D, 0.0D);
                clientLine(from, to, ModParticles.METHOD_QI_WOOD.get(), 0.55D);
            });
        }
    }

    private void clientTornadoVfx() {
        double sizeScale = fullyCharged() ? 1.25D : 1.0D;
        double build = Math.min(1.0D, tickCount / 8.0D);
        int layers = Math.max(2, (int) Math.ceil(10.0D * build));
        for (int layer = 0; layer < layers; layer++) {
            double y = layer * 0.82D * sizeScale;
            double radius = (0.75D + layer * 0.82D * 0.27D) * build * sizeScale;
            double angle = tickCount * (0.42D + layer * 0.012D)
                    + layer * 0.82D + random.nextDouble() * 0.22D;
            for (int arm = 0; arm < 4; arm++) {
                double a = angle + arm * Math.PI * 0.5D;
                Vec3 point = position().add(Math.cos(a) * radius, y, Math.sin(a) * radius);
                Vec3 tangent = new Vec3(-Math.sin(a), 0.10D, Math.cos(a)).scale(0.13D);
                clientParticle((layer + arm) % 4 == 0 ? ParticleTypes.FLAME
                                : ModParticles.METHOD_QI_FIRE.get(), point, tangent);
            }
        }

        // Visible suction streams enter from outside the damaging core.
        for (int stream = 0; stream < 7; stream++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = (3.8D + random.nextDouble() * 2.2D) * sizeScale;
            double height = (0.15D + random.nextDouble() * 3.2D) * sizeScale;
            Vec3 point = position().add(
                    Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            Vec3 inward = position().add(0.0D, 1.8D, 0.0D).subtract(point).normalize();
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle));
            clientParticle(stream % 3 == 0 ? ParticleTypes.ASH
                            : ModParticles.METHOD_QI_FIRE.get(),
                    point, inward.scale(0.13D).add(tangent.scale(0.055D)));
        }

        for (int core = 0; core < 3; core++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.15D + random.nextDouble() * 0.65D;
            clientParticle(core == 0 ? ParticleTypes.SMALL_FLAME
                            : ModParticles.METHOD_QI_FIRE.get(),
                    position().add(Math.cos(angle) * radius,
                            (0.3D + random.nextDouble() * 6.5D) * sizeScale,
                            Math.sin(angle) * radius),
                    new Vec3(-Math.sin(angle) * 0.075D,
                            0.08D, Math.cos(angle) * 0.075D));
        }

        if (tickCount % 3 == 0) {
            forEachVisualTarget(fullyCharged() ? 7.5D : 6.0D,
                    fullyCharged() ? 10.0D : 9.0D, target -> clientLine(
                    target.getBoundingBox().getCenter(),
                    position().add(0.0D, 2.4D, 0.0D),
                    ModParticles.METHOD_QI_FIRE.get(), 0.58D));
        }
        if (tickCount % 12 == 0) {
            clientShockwave(ModParticles.METHOD_QI_FIRE.get(), 1.4D, 24, 0.18D);
        }
        if (tickCount % 2 == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.5D + random.nextDouble() * 2.4D;
            clientParticle(random.nextBoolean() ? ParticleTypes.SMOKE : ParticleTypes.LAVA,
                    position().add(Math.cos(angle) * radius,
                            4.0D + random.nextDouble() * 4.2D, Math.sin(angle) * radius),
                    new Vec3(-Math.sin(angle) * 0.04D, 0.08D, Math.cos(angle) * 0.04D));
        }
        // The moving base visibly drags embers rather than blinking between positions.
        if (tickCount > 8) {
            Vec3 tail = position().subtract(direction().scale(0.55D));
            clientParticle(ParticleTypes.FLAME, tail,
                    new Vec3(-direction().x * 0.025D, 0.025D, -direction().z * 0.025D));
        }
    }

    private void clientEarthSpearVfx() {
        if (tickCount < 6) {
            for (int i = 0; i < 4; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 0.25D + random.nextDouble() * 1.1D;
                clientParticle(i % 2 == 0 ? ParticleTypes.POOF
                                : ModParticles.METHOD_QI_EARTH.get(),
                        position().add(Math.cos(angle) * radius, 0.08D,
                                Math.sin(angle) * radius),
                        new Vec3(0.0D, 0.025D, 0.0D));
            }
        } else if (tickCount == 6) {
            for (int i = 0; i < 18; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
                clientParticle(ModParticles.METHOD_QI_EARTH.get(),
                        position().add(0.0D, 0.15D, 0.0D),
                        radial.scale(0.10D).add(0.0D, 0.18D + random.nextDouble() * 0.18D, 0.0D));
            }
        }
    }

    private void clientMountainVfx() {
        int impactTick = fullyCharged() ? 40 : 30;
        double radiusScale = fullyCharged() ? 8.0D / 6.0D : 1.0D;
        if (tickCount < impactTick) {
            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = Math.sqrt(random.nextDouble()) * 5.8D * radiusScale;
                Vec3 point = position().add(Math.cos(angle) * radius,
                        0.15D + random.nextDouble() * 2.8D, Math.sin(angle) * radius);
                clientParticle(i % 2 == 0 ? ParticleTypes.POOF
                                : ModParticles.METHOD_QI_EARTH.get(),
                        point, new Vec3(0.0D, 0.025D + random.nextDouble() * 0.025D, 0.0D));
            }
            if (tickCount >= impactTick - 6) {
                for (int i = 0; i < 6; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double radius = random.nextDouble() * 4.0D * radiusScale;
                    clientParticle(ParticleTypes.POOF,
                            position().add(Math.cos(angle) * radius, 7.0D,
                                    Math.sin(angle) * radius),
                            new Vec3(0.0D, -0.35D, 0.0D));
                }
            }
        } else if (tickCount == impactTick) {
            clientShockwave(ModParticles.METHOD_QI_EARTH.get(), 0.6D, 36, 0.28D);
            clientShockwave(ParticleTypes.POOF, 0.9D, 40, 0.34D);
        }
    }

    private void clientWaveVfx() {
        Vec3 side = new Vec3(-direction().z, 0.0D, direction().x);
        double widthScale = fullyCharged() ? 9.0D / 7.0D : 1.0D;
        for (int lateral = -6; lateral <= 6; lateral++) {
            double edge = Math.abs(lateral) / 6.0D;
            Vec3 point = position().add(side.scale(lateral * 0.55D * widthScale))
                    .add(direction().scale(0.10D + random.nextDouble() * 0.30D))
                    .add(0.0D, 0.45D + random.nextDouble() * (3.8D - edge * 0.65D), 0.0D);
            clientParticle(lateral % 3 == 0 ? ParticleTypes.SPLASH
                            : ModParticles.METHOD_QI_WATER.get(),
                    point, direction().scale(0.11D).add(side.scale(lateral * 0.003D))
                            .add(0.0D, 0.055D, 0.0D));
            if ((lateral & 1) == 0) {
                double crest = 4.55D - edge * 0.45D + random.nextDouble() * 0.35D;
                clientParticle(lateral % 4 == 0 ? ParticleTypes.CLOUD : ParticleTypes.SPLASH,
                        position().add(side.scale(lateral * 0.55D * widthScale))
                                .add(direction().scale(0.42D)).add(0.0D, crest, 0.0D),
                        direction().scale(0.14D).add(side.scale(lateral * 0.006D))
                                .add(0.0D, 0.025D, 0.0D));
            }
        }
        if ((tickCount & 1) == 0) {
            for (int edge = -1; edge <= 1; edge += 2) {
                clientParticle(ParticleTypes.SPLASH,
                        position().add(side.scale(edge * 3.55D * widthScale))
                                .add(0.0D, 0.65D, 0.0D),
                        direction().scale(0.12D).add(side.scale(edge * 0.16D))
                                .add(0.0D, 0.10D, 0.0D));
            }
            clientParticle(ParticleTypes.FALLING_WATER,
                    position().subtract(direction().scale(1.25D)).add(0.0D, 0.14D, 0.0D),
                    new Vec3(0.0D, -0.01D, 0.0D));
        }
    }

    private void clientLightningVfx() {
        clientRing(position().add(0.0D, 0.08D, 0.0D),
                ModParticles.METHOD_QI_THUNDER.get(), fullyCharged() ? 2.85D : 1.85D, 18,
                tickCount * 0.18D);
        if (tickCount < 16) {
            clientLine(position().add(0.0D, 0.2D, 0.0D),
                    position().add(0.0D, 8.5D, 0.0D),
                    ModParticles.METHOD_QI_THUNDER.get(), 0.62D);
            if ((tickCount & 1) == 0) {
                clientParticle(ParticleTypes.LARGE_SMOKE,
                        position().add((random.nextDouble() - 0.5D) * 2.8D,
                                7.0D + random.nextDouble() * 2.0D,
                                (random.nextDouble() - 0.5D) * 2.8D),
                        new Vec3(0.0D, 0.015D, 0.0D));
            }
        }
        int[] strikes = {16, 27, 38, 49, 60};
        for (int strike : strikes) {
            if (tickCount == strike) {
                for (int i = 0; i < 34; i++) {
                    double y = random.nextDouble() * 11.0D;
                    double wobble = (random.nextDouble() - 0.5D) * (0.18D + y * 0.025D);
                    clientParticle(i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK
                                    : ModParticles.METHOD_QI_THUNDER.get(),
                            position().add(wobble, y, (random.nextDouble() - 0.5D) * 0.35D),
                            Vec3.ZERO);
                }
                clientShockwave(ParticleTypes.ELECTRIC_SPARK, 0.35D, 26, 0.20D);
                break;
            }
        }
    }

    private void clientIceRainVfx() {
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * (fullyCharged() ? 5.0D : 4.0D);
            Vec3 point = position().add(Math.cos(angle) * radius,
                    5.5D + random.nextDouble() * 1.8D, Math.sin(angle) * radius);
            clientParticle(i % 3 == 0 ? ParticleTypes.SNOWFLAKE
                            : ModParticles.METHOD_QI_ICE.get(),
                    point, new Vec3(0.0D, -0.025D, 0.0D));
        }
    }

    private void clientFrozenDomainVfx() {
        double domainRadius = fullyCharged() ? 9.0D : 7.0D;
        for (int i = 0; i < 7; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * (domainRadius - 0.1D);
            Vec3 point = position().add(Math.cos(angle) * radius,
                    0.08D + random.nextDouble() * 0.35D, Math.sin(angle) * radius);
            clientParticle(i % 3 == 0 ? ParticleTypes.SNOWFLAKE
                            : ModParticles.METHOD_QI_ICE.get(),
                    point, new Vec3(0.0D, 0.008D, 0.0D));
        }
        if (tickCount % 3 == 0) {
            forEachVisualTarget(domainRadius, 4.0D, target -> clientParticle(
                    ParticleTypes.SNOWFLAKE,
                    target.position().add(
                            (random.nextDouble() - 0.5D) * target.getBbWidth(),
                            random.nextDouble() * target.getBbHeight() * 0.7D,
                            (random.nextDouble() - 0.5D) * target.getBbWidth()),
                    new Vec3(0.0D, -0.015D, 0.0D)));
        }
    }

    private void clientLightPillarVfx() {
        int warningTicks = fullyCharged() ? 30 : 20;
        if (tickCount < warningTicks) {
            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = (0.2D + random.nextDouble() * 1.8D)
                        * (fullyCharged() ? 1.5D : 1.0D);
                clientParticle(ModParticles.METHOD_QI_YANG.get(),
                        position().add(Math.cos(angle) * radius,
                                random.nextDouble() * 2.2D, Math.sin(angle) * radius),
                        new Vec3(-Math.cos(angle) * 0.015D, 0.055D,
                                -Math.sin(angle) * 0.015D));
            }
        } else {
            for (int i = 0; i < 14; i++) {
                double y = random.nextDouble() * 24.0D;
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = random.nextDouble() * (fullyCharged() ? 2.7D : 1.7D);
                clientParticle(i % 4 == 0 ? ParticleTypes.END_ROD
                                : ModParticles.METHOD_QI_YANG.get(),
                        position().add(Math.cos(angle) * radius, y,
                                Math.sin(angle) * radius),
                        new Vec3(0.0D, -0.32D, 0.0D));
            }
        }
    }

    private void clientWindDropVfx() {
        double radiusScale = fullyCharged() ? 7.0D / 5.0D : 1.0D;
        if (tickCount < 20) {
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = Math.sqrt(random.nextDouble()) * 4.8D * radiusScale;
                Vec3 point = position().add(Math.cos(angle) * radius, 0.1D,
                        Math.sin(angle) * radius);
                clientParticle(i % 2 == 0 ? ParticleTypes.CLOUD
                                : ModParticles.METHOD_QI_WIND.get(),
                        point, new Vec3(-Math.sin(angle) * 0.035D,
                                0.10D + random.nextDouble() * 0.08D,
                                Math.cos(angle) * 0.035D));
            }
        } else if (tickCount == 20) {
            for (int i = 0; i < 34; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = Math.sqrt(random.nextDouble()) * 4.8D * radiusScale;
                clientParticle(i % 3 == 0 ? ParticleTypes.CLOUD
                                : ModParticles.METHOD_QI_WIND.get(),
                        position().add(Math.cos(angle) * radius,
                                8.0D + random.nextDouble() * 3.0D,
                                Math.sin(angle) * radius),
                        new Vec3(0.0D, -0.62D, 0.0D));
            }
            clientShockwave(ModParticles.METHOD_QI_WIND.get(), 0.5D, 36, 0.34D);
            clientShockwave(ParticleTypes.CLOUD, 0.8D, 32, 0.28D);
        } else if (tickCount < 42 && tickCount % 2 == 0) {
            clientParticle(ParticleTypes.CLOUD,
                    position().add((random.nextDouble() - 0.5D) * 7.0D * radiusScale,
                            0.15D, (random.nextDouble() - 0.5D) * 7.0D * radiusScale),
                    new Vec3(0.0D, 0.015D, 0.0D));
        }
    }

    private void clientFengleiDomainVfx() {
        double radius = momentumSnapshot() >= 100.0F ? 12.0D : 10.0D;
        double phase = tickCount * 0.18D;
        if (tickCount % 2 == 0) {
            clientRing(position().add(0.0D, 0.10D, 0.0D),
                    ModParticles.METHOD_QI_WIND.get(), radius, 38, phase);
            clientRing(position().add(0.0D, 0.13D, 0.0D),
                    ModParticles.METHOD_QI_THUNDER.get(), radius * 0.58D,
                    24, -phase * 1.35D);
        }
        for (int stream = 0; stream < 8; stream++) {
            double angle = phase * (1.0D + stream * 0.025D)
                    + Math.PI * 2.0D * stream / 8.0D;
            double streamRadius = radius * (0.22D + (stream % 4) * 0.19D);
            double y = 0.25D + (stream * 1.37D + tickCount * 0.12D) % 7.5D;
            Vec3 point = position().add(
                    Math.cos(angle) * streamRadius, y,
                    Math.sin(angle) * streamRadius);
            Vec3 inward = position().add(0.0D, 2.1D, 0.0D)
                    .subtract(point).normalize().scale(0.07D);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.04D, Math.cos(angle))
                    .scale(0.07D);
            clientParticle(stream % 3 == 0
                            ? ModParticles.METHOD_QI_THUNDER.get()
                            : ModParticles.METHOD_QI_WIND.get(),
                    point, inward.add(tangent));
        }
        for (int cloud = 0; cloud < 3; cloud++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double cloudRadius = Math.sqrt(random.nextDouble()) * radius;
            clientParticle(cloud == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CLOUD,
                    position().add(Math.cos(angle) * cloudRadius,
                            7.0D + random.nextDouble() * 3.0D,
                            Math.sin(angle) * cloudRadius),
                    new Vec3(-Math.sin(angle) * 0.025D, -0.015D,
                            Math.cos(angle) * 0.025D));
        }
        int strikeInterval = momentumSnapshot() >= 100.0F ? 10 : 20;
        if (tickCount % strikeInterval == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double strikeRadius = Math.sqrt(random.nextDouble()) * radius * 0.82D;
            Vec3 ground = position().add(
                    Math.cos(angle) * strikeRadius, 0.15D,
                    Math.sin(angle) * strikeRadius);
            Vec3 top = ground.add(0.0D, 11.0D, 0.0D);
            clientLine(top, ground, ParticleTypes.ELECTRIC_SPARK, 0.34D);
            clientLine(top.add(0.18D, 0.0D, -0.12D), ground,
                    ModParticles.METHOD_QI_THUNDER.get(), 0.48D);
        }
        if (tickCount == 160) {
            clientShockwave(ModParticles.METHOD_QI_WIND.get(), 0.7D, 52, 0.44D);
            clientShockwave(ModParticles.METHOD_QI_THUNDER.get(), 1.0D, 44, 0.36D);
        }
    }

    private void clientParticle(ParticleOptions particle, Vec3 point, Vec3 velocity) {
        level().addParticle(particle, point.x, point.y, point.z,
                velocity.x, velocity.y, velocity.z);
    }

    private void clientLine(
            Vec3 start, Vec3 end, ParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) {
            return;
        }
        Vec3 direction = delta.normalize();
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            clientParticle(particle, start.add(direction.scale(distance)), Vec3.ZERO);
        }
    }

    private void clientRing(
            Vec3 center, ParticleOptions particle, double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2.0D * i / count;
            clientParticle(particle,
                    center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius),
                    Vec3.ZERO);
        }
    }

    private void clientShockwave(
            ParticleOptions particle, double startRadius, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            clientParticle(particle,
                    position().add(radial.scale(startRadius)).add(0.0D, 0.12D, 0.0D),
                    radial.scale(speed));
        }
    }

    private void forEachVisualTarget(
            double radius, double height, java.util.function.Consumer<LivingEntity> action) {
        UUID owner = entityData.get(DATA_OWNER).orElse(null);
        AABB bounds = new AABB(position(), position()).inflate(radius, height, radius);
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, bounds,
                living -> living.isAlive() && !living.getUUID().equals(owner)
                        && horizontalDistance(living.position(), position()) <= radius)) {
            action.accept(living);
        }
    }

    private void tickThorns(ServerLevel level, LivingEntity owner) {
        if (!primaryTriggered && tickCount >= 6) {
            primaryTriggered = true;
            double radius = fullyCharged() ? 3.0D : 2.5D;
            for (LivingEntity target : enemies(level, owner,
                    new AABB(position(), position()).inflate(radius, 3.2D, radius))) {
                if (target.getY() > getY() + 3.0D) {
                    continue;
                }
                if (damage(owner, target, 40.0F, ModSpells.JINGJI_TUSHENG)) {
                    target.addEffect(new MobEffectInstance(
                            ModEffects.TECHNIQUE_SLOW,
                            fullyCharged() ? 60 : 40,
                            fullyCharged() ? 29 : 19, false, true, true));
                    target.push(0.0D, fullyCharged() ? 0.62D : 0.42D, 0.0D);
                }
            }
            SpellImpactParticles.wood(level, position());
        }
    }

    private void tickVines(ServerLevel level, LivingEntity owner) {
        if (tickCount == 1 || tickCount % 20 == 0) {
            ensureWantengVines(level);
        }
        if (tickCount < 10) {
            return;
        }
        double radius = fullyCharged() ? 6.0D : 5.0D;
        for (LivingEntity target : radialEnemies(level, owner, radius, 4.5D)) {
            if (!permitsContinuousEffect(owner, target, 90.0F, ModSpells.WANTENG_JIAOSHA)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(
                    ModEffects.TECHNIQUE_SLOW, 8,
                    fullyCharged() ? 69 : 59, false, false, true));
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() > 0.2D) {
                Vec3 force = pull.normalize().scale(fullyCharged() ? 0.14D : 0.10D);
                target.push(force.x, Math.max(-0.02D, force.y), force.z);
            }
            if (tickCount % 20 == 0) {
                level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                        target.getX(), target.getY() + 0.35D, target.getZ(),
                        8, 0.22D, 0.30D, 0.22D, 0.045D);
            }
        }
        weakenFromNearbyFire(level, radius);
    }

    private void ensureWantengVines(ServerLevel level) {
        boolean[] present = new boolean[WantengVineEntity.VINE_COUNT];
        for (WantengVineEntity vine : level.getEntitiesOfClass(
                WantengVineEntity.class,
                getBoundingBox().inflate(8.0D, 7.0D, 8.0D),
                candidate -> candidate.belongsTo(getUUID()))) {
            int index = vine.vineIndex();
            if (present[index]) {
                vine.discard();
            } else {
                present[index] = true;
            }
        }
        for (int index = 0; index < present.length; index++) {
            if (present[index]) {
                continue;
            }
            WantengVineEntity vine = WantengVineEntity.create(level, this, index);
            if (vine != null) {
                level.addFreshEntity(vine);
            }
        }
    }

    private void tickFireTornado(ServerLevel level, LivingEntity owner) {
        if (tickCount < 8) {
            return;
        }
        double coreRadius = fullyCharged() ? 5.0D : 4.0D;
        double pullRadius = fullyCharged() ? 7.5D : 6.0D;
        double height = fullyCharged() ? 10.0D : 8.0D;
        setPos(position().add(direction().scale(fullyCharged() ? 0.12D : 0.08D)));
        for (LivingEntity target : radialEnemies(level, owner, pullRadius, height)) {
            double distance = horizontalDistance(target.position(), position());
            boolean insideDamageCore = distance <= coreRadius;
            if (insideDamageCore
                    ? !permitsContinuousEffect(owner, target, 180.0F, ModSpells.CHIYAN_LONGJUAN)
                    : ForcefieldCenterBehavior.interceptAttackPath(
                            owner, position().add(0.0D, 2.5D, 0.0D),
                            target.getBoundingBox().getCenter(), 1.0F)) {
                continue;
            }
            pullLivingIntoTornado(target, distance, pullRadius);
            if (insideDamageCore && tickCount % 20 == 0) {
                target.igniteForSeconds(2);
                level.sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        12, 0.30D, 0.45D, 0.30D, 0.08D);
            }
        }
        pullLooseTornadoEntities(level, owner);
        if ((tickCount - 8) % 4 == 0) {
            destroyTornadoTerrain(level, owner, height);
        }
        if (tickCount % 4 == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.0D + random.nextDouble() * 2.8D;
            level.sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.3D + random.nextDouble() * 6.8D,
                    getZ() + Math.sin(angle) * radius,
                    3, 0.08D, 0.18D, 0.08D, 0.035D);
        }
        damageFireSensitiveAreas(level, position(), coreRadius + 0.5D, 4);
    }

    private void pullLivingIntoTornado(
            LivingEntity target, double horizontalDistance, double pullRadius) {
        Vec3 radial = position().subtract(target.position());
        radial = new Vec3(radial.x, 0.0D, radial.z);
        if (radial.lengthSqr() < 1.0E-4D) {
            radial = direction();
        } else {
            radial = radial.normalize();
        }
        double closeness = 1.0D - Math.min(1.0D, horizontalDistance / pullRadius);
        double strengthScale = fullyCharged() ? 1.35D : 1.0D;
        double inwardStrength = (0.18D + closeness * 0.16D) * strengthScale;
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x)
                .scale((0.055D + closeness * 0.055D) * strengthScale);
        double targetHeight = position().y + 2.6D;
        double verticalCorrection = Mth.clamp(
                (targetHeight - target.getBoundingBox().getCenter().y) * 0.025D,
                -0.035D, 0.075D);
        double lift = (target.onGround() ? 0.10D : 0.045D) + closeness * 0.055D;
        target.push(
                radial.x * inwardStrength + tangent.x,
                lift + verticalCorrection,
                radial.z * inwardStrength + tangent.z);
        target.hurtMarked = true;
    }

    private void pullLooseTornadoEntities(ServerLevel level, LivingEntity owner) {
        double pullRadius = fullyCharged() ? 7.5D : 6.0D;
        double height = fullyCharged() ? 10.0D : 9.0D;
        AABB bounds = getBoundingBox().inflate(pullRadius, height, pullRadius);
        for (Entity target : level.getEntities(this, bounds,
                entity -> entity != owner
                        && (entity instanceof ItemEntity
                        || entity instanceof ExperienceOrb
                        || entity instanceof Projectile))) {
            Vec3 radial = position().add(0.0D, 2.2D, 0.0D)
                    .subtract(target.getBoundingBox().getCenter());
            Vec3 horizontal = new Vec3(radial.x, 0.0D, radial.z);
            double distance = horizontal.length();
            if (distance > pullRadius || distance < 1.0E-4D) {
                continue;
            }
            Vec3 inward = horizontal.scale(1.0D / distance);
            Vec3 tangent = new Vec3(-inward.z, 0.0D, inward.x);
            double closeness = 1.0D - distance / pullRadius;
            double strengthScale = fullyCharged() ? 1.35D : 1.0D;
            Vec3 suction = inward.scale((0.12D + closeness * 0.10D) * strengthScale)
                    .add(tangent.scale((0.04D + closeness * 0.035D) * strengthScale))
                    .add(0.0D, Mth.clamp(radial.y * 0.018D, -0.03D, 0.08D), 0.0D);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.82D).add(suction));
            target.hasImpulse = true;
        }
    }

    private void throwTornadoTargets(ServerLevel level, LivingEntity owner) {
        double radius = fullyCharged() ? 5.5D : 4.5D;
        for (LivingEntity target : radialEnemies(level, owner, radius, fullyCharged() ? 10.0D : 8.0D)) {
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    owner, position().add(0.0D, 2.5D, 0.0D),
                    target.getBoundingBox().getCenter(), 1.0F)) {
                continue;
            }
            Vec3 away = target.position().subtract(position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 0.01D) {
                away = direction();
            }
            away = away.normalize();
            double force = fullyCharged() ? 1.65D : 1.25D;
            target.push(away.x * force, fullyCharged() ? 0.75D : 0.55D, away.z * force);
        }
    }

    private void tickEarthSpear(ServerLevel level, LivingEntity owner) {
        if (!primaryTriggered && tickCount >= 6) {
            primaryTriggered = true;
            LivingEntity preferred = resolveTarget();
            LivingEntity target = preferred != null && preferred.distanceToSqr(this) <= 6.25D
                    ? preferred : radialEnemies(level, owner, 1.5D, 3.0D).stream().findFirst().orElse(null);
            if (target != null && damage(owner, target, 55.0F, ModSpells.DIYAN_QIANG)) {
                target.setDeltaMovement(target.getDeltaMovement().x,
                        fullyCharged() ? 1.05D : 0.82D, target.getDeltaMovement().z);
                target.hurtMarked = true;
                targetId = target.getUUID();
                targetLifted = true;
            }
            SpellImpactParticles.earth(level, position());
        }
        LivingEntity target = resolveTarget();
        if (targetLifted && target != null && tickCount > 7 && target.onGround()) {
            if (damage(owner, target, fullyCharged() ? 40.0F : 20.0F, ModSpells.DIYAN_QIANG)) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                level.getBlockState(target.blockPosition().below())),
                        target.getX(), target.getY(), target.getZ(),
                        18, 0.35D, 0.18D, 0.35D, 0.12D);
                SpellImpactParticles.shockRing(level, target.position().add(0.0D, 0.08D, 0.0D),
                        ParticleTypes.POOF, 0.8D, 20, 0.10D);
            }
            targetLifted = false;
        }
    }

    private void tickMountainSeal(ServerLevel level, LivingEntity owner) {
        if (tickCount == 1 || tickCount % 10 == 0) {
            ensureShanbengSeal(level);
        }
        int impactTick = fullyCharged() ? 40 : 30;
        if (tickCount < impactTick) {
            if (tickCount % 6 == 0) {
                level.playSound(null, blockPosition(), SoundEvents.STONE_PLACE,
                        SoundSource.PLAYERS, 0.7F, 0.55F + tickCount * 0.012F);
            }
            return;
        }
        if (primaryTriggered) {
            return;
        }
        primaryTriggered = true;
        double radius = fullyCharged() ? 8.0D : 6.0D;
        for (LivingEntity target : radialEnemies(level, owner, radius, 10.0D)) {
            double distance = horizontalDistance(target.position(), position());
            float base = (float) (900.0D * Math.max(0.30D,
                    1.0D - 0.70D * distance / radius));
            if (target.onGround()) {
                base *= fullyCharged() ? 1.75F : 1.50F;
            }
            if (damage(owner, target, base, ModSpells.SHANBENG_YIN)) {
                target.setDeltaMovement(target.getDeltaMovement().x * 0.2D,
                        Math.min(fullyCharged() ? -1.8D : -1.2D,
                                target.getDeltaMovement().y),
                        target.getDeltaMovement().z * 0.2D);
                target.hurtMarked = true;
            }
        }
        destroyMountainBlocks(level, owner);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                getX(), getY() + 0.5D, getZ(), 2, 1.0D, 0.5D, 1.0D, 0.0D);
        SpellImpactParticles.earth(level, position());
        SpellImpactParticles.shockRing(level, position().add(0.0D, 0.12D, 0.0D),
                ModParticles.METHOD_QI_EARTH.get(), 1.1D, 40, 0.30D);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.5F, 0.55F);
    }

    private void ensureShanbengSeal(ServerLevel level) {
        boolean found = false;
        for (ShanbengSealEntity seal : level.getEntitiesOfClass(
                ShanbengSealEntity.class,
                getBoundingBox().inflate(10.0D, 14.0D, 10.0D),
                candidate -> candidate.belongsTo(getUUID()))) {
            if (found) {
                seal.discard();
            } else {
                found = true;
            }
        }
        if (!found) {
            ShanbengSealEntity seal = ShanbengSealEntity.create(level, this);
            if (seal != null) {
                level.addFreshEntity(seal);
            }
        }
    }

    private void destroyMountainBlocks(ServerLevel level, LivingEntity owner) {
        int destroyed = destroyCraterTerrain(
                level, owner, position(),
                fullyCharged() ? 10.5D : 8.0D,
                fullyCharged() ? 10 : 7,
                fullyCharged() ? 14 : 11,
                fullyCharged() ? 85.0F : 55.0F,
                fullyCharged() ? 0.86D : 0.76D,
                fullyCharged() ? 3_200 : 1_600);
        emitTerrainRupture(level, position(), destroyed,
                ModParticles.METHOD_QI_EARTH.get(), Blocks.DEEPSLATE.defaultBlockState());
    }

    /**
     * Sweeps a widening destructive core through the moving tornado without creating
     * dozens of real explosions. The total cap keeps the five-second spell bounded.
     */
    private void destroyTornadoTerrain(
            ServerLevel level, LivingEntity owner, double tornadoHeight) {
        int totalCap = fullyCharged() ? 3_600 : 2_000;
        int remaining = totalCap - terrainBlocksDestroyed;
        if (remaining <= 0 || !terrainDamageAllowed(level, owner)) {
            return;
        }
        BlockPos center = blockPosition();
        int height = Mth.ceil(tornadoHeight);
        int horizontal = fullyCharged() ? 6 : 5;
        int pulseBudget = Math.min(remaining, fullyCharged() ? 360 : 220);
        Vec3 origin = position();
        boolean charged = fullyCharged();
        AABB bounds = new AABB(origin.x-horizontal,origin.y-1,origin.z-horizontal,
                origin.x+horizontal,origin.y+height,origin.z+horizontal);
        int destroyed = com.example.immortal_cultivation_mod.spell.SpellDestruction.area(level,owner,
                bounds,origin,pulseBudget,charged?68:44,pos -> {
                    double heightProgress=Mth.clamp((pos.getY()+.5-origin.y)/Math.max(1,tornadoHeight),0,1);
                    double radius=(charged?3.5:2.8)+heightProgress*(charged?1.8:1.4);
                    double x=pos.getX()+.5-origin.x,z=pos.getZ()+.5-origin.z;
                    double edge=.86+.1*Math.sin(x*1.9+pos.getY()*.4)+.08*Math.cos(z*2.3);
                    return x*x+z*z<radius*radius*edge*edge;
                },null,broken -> terrainBlocksDestroyed -= pulseBudget-broken);
        terrainBlocksDestroyed += destroyed;
        if (destroyed > 0) {
            level.sendParticles(new BlockParticleOption(
                            ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState()),
                    getX(), getY() + Math.min(3.0D, tornadoHeight * 0.35D), getZ(),
                    Math.min(70, 12 + destroyed / 3),
                    fullyCharged() ? 2.4D : 1.8D,
                    fullyCharged() ? 3.8D : 2.8D,
                    fullyCharged() ? 2.4D : 1.8D, 0.30D);
        }
    }

    /** Explosion-shaped crater patterned after the destructive 天陨焚城 impact. */
    private int destroyCraterTerrain(
            ServerLevel level,
            LivingEntity owner,
            Vec3 center,
            double horizontalRadius,
            int depth,
            int height,
            float maximumHardness,
            double edgeChance,
            int budget) {
        if (!terrainDamageAllowed(level, owner) || budget <= 0) return 0;
        long seed = level.random.nextLong();
        AABB bounds = new AABB(center.x-horizontalRadius, center.y-depth, center.z-horizontalRadius,
                center.x+horizontalRadius, center.y+height, center.z+horizontalRadius);
        return com.example.immortal_cultivation_mod.spell.SpellDestruction.area(level, owner,
                bounds, center, budget, maximumHardness, pos ->
                        com.example.immortal_cultivation_mod.spell.SpellDestruction.fractured(pos, center,
                                horizontalRadius, pos.getY()+.5<center.y?Math.max(1,depth):Math.max(1,height), seed), null);
    }

    /** Carves the vertical channel traversed by 天光贯杀 instead of only its floor. */
    private int destroyLightColumnTerrain(
            ServerLevel level, LivingEntity owner, Vec3 center) {
        if (!terrainDamageAllowed(level, owner)) return 0;
        double radius = fullyCharged() ? 4.5 : 3.25;
        int depth = fullyCharged() ? 26 : 18, height = fullyCharged() ? 72 : 48;
        AABB bounds = new AABB(center.x-radius,center.y-depth,center.z-radius,
                center.x+radius,center.y+height,center.z+radius);
        return com.example.immortal_cultivation_mod.spell.SpellDestruction.area(level, owner,
                bounds, center, fullyCharged()?3600:1800, fullyCharged()?95:60, pos -> {
                    double x=pos.getX()+.5-center.x,z=pos.getZ()+.5-center.z;
                    double edge=.88+.1*Math.sin(x*1.7+pos.getY()*.3)+.08*Math.sin(z*2.1);
                    return x*x+z*z<radius*radius*edge*edge;
                }, null);
    }

    private boolean terrainDamageAllowed(ServerLevel level, LivingEntity owner) {
        return BlockBreakingSettings.enabled(owner,
                BlockBreakingSettings.Category.TECHNIQUES)
                && (owner instanceof Player || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));
    }

    private void emitTerrainRupture(
            ServerLevel level,
            Vec3 center,
            int destroyed,
            ParticleOptions qiParticle,
            BlockState debrisState) {
        if (destroyed <= 0) {
            return;
        }
        int debris = Math.min(220, 30 + destroyed / 5);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, debrisState),
                center.x, center.y + 0.8D, center.z,
                debris, 3.8D, 2.8D, 3.8D, 0.48D);
        level.sendParticles(qiParticle,
                center.x, center.y + 0.35D, center.z,
                Math.min(100, 18 + destroyed / 12), 3.4D, 1.0D, 3.4D, 0.26D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.5D, center.z,
                Math.min(4, 1 + destroyed / 500), 1.0D, 0.5D, 1.0D, 0.0D);
    }

    private void tickWave(ServerLevel level, LivingEntity owner) {
        Vec3 direction = direction();
        setPos(position().add(direction.scale(fullyCharged() ? 0.90D : 0.70D)));
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        AABB search = new AABB(position(), position()).inflate(
                fullyCharged() ? 5.0D : 4.0D, 2.4D, fullyCharged() ? 5.0D : 4.0D);
        for (LivingEntity target : enemies(level, owner, search)) {
            Vec3 offset = target.position().subtract(position());
            double forward = offset.dot(direction);
            double lateral = Math.abs(offset.dot(side));
            if (Math.abs(forward) > 1.35D || lateral > (fullyCharged() ? 4.5D : 3.5D)) {
                continue;
            }
            if (hitEntities.add(target.getUUID())) {
                if (!damage(owner, target, 180.0F, ModSpells.CANGLANG_CHONG)) {
                    hitEntities.remove(target.getUUID());
                    continue;
                }
            }
            com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                    .applyDampFromWater(owner, target, 180);
            target.clearFire();
            double carry = fullyCharged() ? 0.95D : 0.72D;
            target.setDeltaMovement(direction.x * carry,
                    fullyCharged() ? 0.18D : 0.12D, direction.z * carry);
            target.hurtMarked = true;
        }
        extinguishFireBlocks(level, search);
    }

    private void finishWave(ServerLevel level) {
        Vec3 direction = direction();
        for (UUID uuid : hitEntities) {
            if (level.getEntity(uuid) instanceof LivingEntity target && target.isAlive()) {
                double force = fullyCharged() ? 1.75D : 1.35D;
                target.setDeltaMovement(direction.x * force,
                        fullyCharged() ? 0.50D : 0.38D, direction.z * force);
                target.hurtMarked = true;
            }
        }
        SpellImpactParticles.water(level, position());
    }

    private void tickFiveLightning(ServerLevel level, LivingEntity owner) {
        LivingEntity target = resolveTarget();
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        setPos(target.getX(), target.getY(), target.getZ());
        int[] strikes = {16, 27, 38, 49, 60};
        for (int index = 0; index < strikes.length; index++) {
            if (tickCount == strikes[index]) {
                strikeLightning(level, owner, target, index);
                break;
            }
        }
    }

    private void strikeLightning(ServerLevel level, LivingEntity owner, LivingEntity marked, int index) {
        Vec3 center = marked.position();
        double visualAngle = index * 2.17D;
        Vec3 visualCenter = center.add(Math.cos(visualAngle) * index * 0.055D,
                0.0D, Math.sin(visualAngle) * index * 0.055D);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(visualCenter.x, visualCenter.y, visualCenter.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        for (LivingEntity target : radialEnemiesAt(
                level, owner, center, fullyCharged() ? 3.0D : 2.0D, 5.0D)) {
            float amount = 240.0F;
            if (target == marked) {
                amount *= 1.0F + index * (fullyCharged() ? 0.15F : 0.10F);
            }
            float dealt = SpellDamageHelper.damage(owner,
                    SpellCharge.damage(amount, chargeScale()),
                    ModSpells.get(ModSpells.WULEI_HONGDING));
            if (target.hurt(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.spellSource(damageSources().indirectMagic(this, owner), ModSpells.WULEI_HONGDING), dealt)) {
                com.example.immortal_cultivation_mod.spell.GangtingXiaokongLu
                        .maybeChainLightning(owner, target, dealt);
            }
        }
        level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                center.x, center.y + 1.0D, center.z, 32, 0.9D, 1.2D, 0.9D, 0.20D);
        SpellImpactParticles.lightning(level, center.add(0.0D, 0.15D, 0.0D));
        int destroyed = destroyCraterTerrain(
                level, owner, center,
                index == 4 ? (fullyCharged() ? 5.5D : 4.0D)
                        : (fullyCharged() ? 3.0D : 2.2D),
                index == 4 ? (fullyCharged() ? 4 : 3) : (fullyCharged() ? 2 : 1),
                index == 4 ? (fullyCharged() ? 6 : 4) : (fullyCharged() ? 3 : 2),
                index == 4 ? (fullyCharged() ? 65.0F : 40.0F)
                        : (fullyCharged() ? 30.0F : 18.0F),
                index == 4 ? (fullyCharged() ? 0.90D : 0.80D)
                        : (fullyCharged() ? 0.80D : 0.70D),
                index == 4 ? (fullyCharged() ? 550 : 300)
                        : (fullyCharged() ? 120 : 70));
        if (index == 4) {
            emitTerrainRupture(level, center, destroyed,
                    ModParticles.METHOD_QI_THUNDER.get(), Blocks.DEEPSLATE.defaultBlockState());
        }
        if (index == 4) {
            for (int branch = 0; branch < 4; branch++) {
                double angle = Math.PI * 2.0D * branch / 4.0D + 0.45D;
                Vec3 start = center.add(Math.cos(angle) * 3.2D,
                        10.0D + (branch & 1), Math.sin(angle) * 3.2D);
                ElementalSpellVfx.line(level, start, center.add(0.0D, 0.8D, 0.0D),
                        branch % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK
                                : ModParticles.METHOD_QI_THUNDER.get(), 0.24D);
            }
            level.sendParticles(ParticleTypes.FLASH,
                    center.x, center.y + 0.8D, center.z,
                    3, 0.28D, 0.50D, 0.28D, 0.0D);
            SpellImpactParticles.shockRing(level, center.add(0.0D, 0.12D, 0.0D),
                    ModParticles.METHOD_QI_THUNDER.get(), 0.55D, 36, 0.30D);
        }
    }

    private void tickIceRain(ServerLevel level, LivingEntity owner) {
        if (tickCount >= 4 && tickCount <= 32 && tickCount % 4 == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * (fullyCharged() ? 5.0D : 4.0D);
            Vec3 spawn = position().add(Math.cos(angle) * radius, 8.0D,
                    Math.sin(angle) * radius);
            ElementalTechniqueProjectileEntity spike = new ElementalTechniqueProjectileEntity(
                    level, owner, ElementalTechniqueProjectileEntity.Kind.BINGZHUI_YU);
            spike.setPos(spawn.x, spawn.y, spawn.z);
            spike.setDeltaMovement(0.0D, -2.1D, 0.0D);
            spike.setChargeScale(chargeScale());
            level.addFreshEntity(spike);
        }
    }

    private void tickIcePrison(ServerLevel level, LivingEntity owner) {
        Set<UUID> present = new HashSet<>();
        double radius = fullyCharged() ? 9.0D : 7.0D;
        for (LivingEntity target : radialEnemies(level, owner, radius, 4.0D)) {
            if (!permitsContinuousEffect(owner, target, 100.0F, ModSpells.HANYU_FENGDI)) {
                continue;
            }
            present.add(target.getUUID());
            int time = insideTicks.merge(target.getUUID(), 1, Integer::sum);
            boolean dampSynergy = com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                    .hasMethod(owner)
                    && (com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                    .isDamp(target)
                    || com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                    .isStandingInWater(target));
            target.addEffect(new MobEffectInstance(
                    ModEffects.TECHNIQUE_SLOW, 8,
                    dampSynergy ? 59 : fullyCharged() ? 49 : 34,
                    false, false, true));
            int freezeThreshold = dampSynergy
                    ? (com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing
                    .isStandingInWater(target) ? 30 : 50)
                    : fullyCharged() ? 60 : 80;
            if (time >= freezeThreshold && !target.hasEffect(ModEffects.FROZEN)) {
                target.addEffect(new MobEffectInstance(ModEffects.FROZEN,
                        fullyCharged() ? 30 : 20, 0, false, true, true));
                insideTicks.put(target.getUUID(), 0);
                level.sendParticles(ModParticles.METHOD_QI_ICE.get(),
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        34, target.getBbWidth() * 0.65D,
                        target.getBbHeight() * 0.55D,
                        target.getBbWidth() * 0.65D, 0.045D);
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        22, target.getBbWidth() * 0.55D,
                        target.getBbHeight() * 0.50D,
                        target.getBbWidth() * 0.55D, 0.055D);
            }
        }
        insideTicks.keySet().removeIf(uuid -> !present.contains(uuid));
        weakenFromNearbyFire(level, radius);
    }

    private void tickLightPillar(ServerLevel level, LivingEntity owner) {
        int impactTick = fullyCharged() ? 30 : 20;
        if (tickCount < impactTick) {
            return;
        }
        if (!primaryTriggered) {
            primaryTriggered = true;
            double radius = fullyCharged() ? 3.0D : 2.0D;
            AABB beam = new AABB(getX() - radius, level.getMinBuildHeight(), getZ() - radius,
                    getX() + radius, level.getMaxBuildHeight(), getZ() + radius);
            for (LivingEntity target : enemies(level, owner, beam)) {
                if (horizontalDistance(target.position(), position()) > radius) {
                    continue;
                }
                float amount = isDark(target)
                        ? 950.0F * (fullyCharged() ? 1.50F : 1.25F) : 950.0F;
                if (damage(owner, target, amount, ModSpells.TIANGUANG_GUANSHA)
                        && isDark(target)) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE,
                            target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            22, target.getBbWidth() * 0.55D,
                            target.getBbHeight() * 0.45D,
                            target.getBbWidth() * 0.55D, 0.05D);
                    level.sendParticles(ModParticles.METHOD_QI_YANG.get(),
                            target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            20, target.getBbWidth() * 0.50D,
                            target.getBbHeight() * 0.42D,
                            target.getBbWidth() * 0.50D, 0.08D);
                }
            }
            int destroyed = destroyLightColumnTerrain(level, owner, position());
            emitTerrainRupture(level, position(), destroyed,
                    ModParticles.METHOD_QI_YANG.get(), Blocks.QUARTZ_BLOCK.defaultBlockState());
            level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.3F, 1.55F);
        }
    }

    private void tickWindDrop(ServerLevel level, LivingEntity owner) {
        if (tickCount < 20) {
            return;
        }
        if (!primaryTriggered) {
            primaryTriggered = true;
            double radius = fullyCharged() ? 7.0D : 5.0D;
            for (LivingEntity target : radialEnemies(level, owner, radius, 12.0D)) {
                boolean airborne = !target.onGround() || target.getY() > getY() + 1.5D;
                if (!damage(owner, target, 600.0F, ModSpells.TIANFENG_ZHUI)) {
                    continue;
                }
                if (airborne) {
                    forcedFallStart.put(target.getUUID(), target.getY());
                    target.setDeltaMovement(target.getDeltaMovement().x * 0.15D,
                            fullyCharged() ? -3.2D : -2.4D,
                            target.getDeltaMovement().z * 0.15D);
                } else {
                    boolean controlled = target.addEffect(new MobEffectInstance(
                            ModEffects.DAZE, fullyCharged() ? 24 : 12,
                            0, false, true, true), owner);
                    if (controlled) target.setDeltaMovement(Vec3.ZERO);
                }
                target.hurtMarked = true;
            }
            int destroyed = destroyCraterTerrain(
                    level, owner, position(),
                    fullyCharged() ? 9.5D : 7.0D,
                    fullyCharged() ? 9 : 6,
                    fullyCharged() ? 14 : 10,
                    fullyCharged() ? 90.0F : 60.0F,
                    fullyCharged() ? 0.88D : 0.78D,
                    fullyCharged() ? 3_200 : 1_600);
            emitTerrainRupture(level, position(), destroyed,
                    ModParticles.METHOD_QI_WIND.get(), Blocks.STONE.defaultBlockState());
        }
        forcedFallStart.entrySet().removeIf(entry -> {
            if (!(level.getEntity(entry.getKey()) instanceof LivingEntity target) || !target.isAlive()) {
                return true;
            }
            if (!target.onGround()) {
                return false;
            }
            float impact = (float) Math.min(fullyCharged() ? 600.0D : 300.0D,
                    Math.max(0.0D, entry.getValue() - target.getY()) * 30.0D);
            if (impact > 0.0F) {
                damage(owner, target, impact, ModSpells.TIANFENG_ZHUI);
                SpellImpactParticles.wind(level, target.position());
            }
            return true;
        });
    }

    private void tickFengleiDomain(ServerLevel level, LivingEntity owner) {
        boolean harmony = momentumSnapshot() >= 100.0F;
        double radius = harmony ? 12.0D : 10.0D;
        double windForceScale = momentumSnapshot() >= 100.0F ? 1.35D
                : momentumSnapshot() >= 75.0F ? 1.22D
                : momentumSnapshot() >= 25.0F ? 1.10D : 1.0D;
        Vec3 center = position().add(0.0D, 1.0D, 0.0D);
        for (LivingEntity target : radialEnemies(level, owner, radius, 8.0D)) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            if (CenterEffectSupport.isForcefieldRouteBlocked(owner, center, targetCenter)) {
                continue;
            }
            Vec3 inward = position().subtract(target.position());
            inward = new Vec3(inward.x, 0.0D, inward.z);
            double distance = Math.max(0.35D, inward.length());
            inward = inward.scale(1.0D / distance);
            Vec3 tangent = new Vec3(-inward.z, 0.0D, inward.x);
            double closeness = 1.0D - Math.min(1.0D, distance / radius);
            double force = (0.055D + closeness * 0.065D)
                    * windForceScale;
            Vec3 current = target.getDeltaMovement();
            double downward = target.onGround() ? 0.0D
                    : harmony ? -0.095D : -0.060D;
            target.setDeltaMovement(current.scale(0.92D)
                    .add(inward.scale(force))
                    .add(tangent.scale(0.018D))
                    .add(0.0D, downward, 0.0D));
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(
                    ModEffects.TECHNIQUE_SLOW, 8,
                    harmony ? 24 : 14, false, false, true));
        }

        AABB projectileBounds = new AABB(position(), position())
                .inflate(radius, 9.0D, radius);
        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class, projectileBounds,
                Entity::isAlive)) {
            Entity projectileOwner = projectile.getOwner();
            if (projectileOwner == owner
                    || projectileOwner instanceof LivingEntity living
                    && !NpcProfile.isCombatEnemy(owner, living)) {
                continue;
            }
            if (CenterEffectSupport.isForcefieldRouteBlocked(
                    owner, center, projectile.getBoundingBox().getCenter())) {
                continue;
            }
            Vec3 radial = position().subtract(projectile.position());
            radial = new Vec3(radial.x, 0.0D, radial.z);
            if (radial.lengthSqr() < 1.0E-5D) continue;
            radial = radial.normalize();
            Vec3 swirl = new Vec3(-radial.z, 0.0D, radial.x)
                    .scale((harmony ? 0.085D : 0.055D) * windForceScale);
            projectile.setDeltaMovement(projectile.getDeltaMovement()
                    .scale(harmony ? 0.90D : 0.94D)
                    .add(radial.scale(0.025D)).add(swirl)
                    .add(0.0D, -0.018D, 0.0D));
            projectile.hasImpulse = true;
        }

        int strikeInterval = harmony ? 10 : 20;
        if (tickCount % strikeInterval == 0 && tickCount < 160) {
            strikeFenglei(level, owner, radius, harmony);
        }
        if (harmony && tickCount == 20) {
            GangtingTechniques.thunderGust(
                    level, owner, position().add(0.0D, 0.2D, 0.0D),
                    radius, 0.52D, 80.0F);
        }
        if (tickCount == 160) {
            finishFengleiDomain(level, owner, radius, harmony);
        }
    }

    private void strikeFenglei(
            ServerLevel level, LivingEntity owner, double radius, boolean harmony) {
        java.util.List<LivingEntity> targets = new ArrayList<>(
                radialEnemies(level, owner, radius, 10.0D));
        LivingEntity marked = targets.isEmpty()
                ? null : targets.get(random.nextInt(targets.size()));
        Vec3 strike = marked == null
                ? position().add(
                (random.nextDouble() - 0.5D) * radius * 1.5D,
                0.0D,
                (random.nextDouble() - 0.5D) * radius * 1.5D)
                : marked.position();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(strike.x, strike.y, strike.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        ElementalSpellVfx.line(level, strike.add(0.0D, 13.0D, 0.0D),
                strike.add(0.0D, 0.25D, 0.0D),
                ModParticles.METHOD_QI_THUNDER.get(), 0.32D);
        ElementalSpellVfx.line(level,
                strike.add(0.35D, 11.5D, -0.20D),
                strike.add(0.0D, 0.25D, 0.0D),
                ParticleTypes.ELECTRIC_SPARK, 0.26D);
        for (LivingEntity target : radialEnemiesAt(
                level, owner, strike, harmony ? 2.4D : 1.8D, 5.5D)) {
            float barrierDamage = SpellCharge.damage(260.0F, chargeScale());
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    owner, position().add(0.0D, 1.0D, 0.0D),
                    target.getBoundingBox().getCenter(), barrierDamage)) {
                continue;
            }
            float amount = GangtingTechniques.damageWithSnapshot(
                    owner, 260.0F, ModSpells.JIUXIAO_FENGLEI_YU,
                    chargeScale(), momentumSnapshot());
            if (target.hurt(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.spellSource(damageSources().indirectMagic(this, owner), ModSpells.JIUXIAO_FENGLEI_YU), amount)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 24, 1,
                        false, true, true));
                target.push(0.0D, target.onGround() ? 0.18D : -0.25D, 0.0D);
            }
        }
        level.sendParticles(ModParticles.METHOD_QI_WIND.get(),
                strike.x, strike.y + 0.22D, strike.z,
                22, 0.9D, 0.18D, 0.9D, 0.16D);
        level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                strike.x, strike.y + 0.55D, strike.z,
                28, 0.70D, 0.75D, 0.70D, 0.20D);
        level.playSound(null, BlockPos.containing(strike),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                0.85F, harmony ? 1.30F : 1.12F);
    }

    private void finishFengleiDomain(
            ServerLevel level, LivingEntity owner, double radius, boolean harmony) {
        double finalRadius = harmony ? radius * 1.25D : radius;
        Vec3 center = position().add(0.0D, 0.25D, 0.0D);
        if (harmony) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(position().x, position().y, position().z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            ElementalSpellVfx.line(level, center.add(0.0D, 16.0D, 0.0D),
                    center, ModParticles.METHOD_QI_THUNDER.get(), 0.26D);
            ElementalSpellVfx.line(level, center.add(-0.35D, 14.0D, 0.25D),
                    center, ParticleTypes.ELECTRIC_SPARK, 0.22D);
            for (LivingEntity target : radialEnemies(
                    level, owner, 4.0D, 8.0D)) {
                float barrierDamage = SpellCharge.damage(700.0F, chargeScale());
                if (ForcefieldCenterBehavior.interceptAttackPath(
                        owner, center, target.getBoundingBox().getCenter(),
                        barrierDamage)) {
                    continue;
                }
                float amount = GangtingTechniques.damageWithSnapshot(
                        owner, 700.0F, ModSpells.JIUXIAO_FENGLEI_YU,
                        chargeScale(), momentumSnapshot());
                target.hurt(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.spellSource(damageSources().indirectMagic(this, owner), ModSpells.JIUXIAO_FENGLEI_YU), amount);
            }
            GangtingTechniques.thunderGust(
                    level, owner, center, finalRadius, 1.05D, 0.0F);
        }
        SpellImpactParticles.shockRing(level, center,
                ModParticles.METHOD_QI_WIND.get(), 1.0D, 64, 0.46D);
        SpellImpactParticles.shockRing(level, center,
                ModParticles.METHOD_QI_THUNDER.get(), 1.4D, 52, 0.38D);
        level.sendParticles(ParticleTypes.FLASH,
                center.x, center.y + 1.0D, center.z,
                harmony ? 5 : 2, 1.0D, 1.2D, 1.0D, 0.0D);
        level.playSound(null, blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 1.5F, 0.72F);
    }

    private void weakenFromNearbyFire(ServerLevel level, double radius) {
        if (tickCount % 5 != 0) {
            return;
        }
        AABB bounds = new AABB(position(), position()).inflate(radius, 3.0D, radius);
        boolean burningEntity = !level.getEntitiesOfClass(LivingEntity.class, bounds,
                LivingEntity::isOnFire).isEmpty();
        boolean fireBlock = false;
        BlockPos center = blockPosition();
        int horizontal = Math.min(7, (int) Math.ceil(radius));
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-horizontal, -1, -horizontal),
                center.offset(horizontal, 2, horizontal))) {
            if (level.getBlockState(cursor).is(BlockTags.FIRE)) {
                fireBlock = true;
                break;
            }
        }
        if (burningEntity || fireBlock) {
            lifePenalty += 12;
        }
    }

    private void emitEndingVfx(ServerLevel level) {
        switch (kind()) {
            case JINGJI_TUSHENG -> {
                level.sendParticles(new BlockParticleOption(
                                ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()),
                        getX(), getY() + 0.35D, getZ(),
                        18, 0.75D, 0.35D, 0.75D, 0.06D);
                level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                        getX(), getY() + 0.35D, getZ(),
                        12, 0.65D, 0.28D, 0.65D, 0.035D);
            }
            case WANTENG_JIAOSHA -> {
                SpellImpactParticles.shockRing(level, position().add(0.0D, 0.15D, 0.0D),
                        ModParticles.METHOD_QI_WOOD.get(), 1.0D, 36, 0.22D);
                level.sendParticles(ParticleTypes.COMPOSTER,
                        getX(), getY() + 0.8D, getZ(),
                        30, 3.4D, 0.8D, 3.4D, 0.04D);
            }
            case CHIYAN_LONGJUAN -> SpellImpactParticles.fireExplosion(level, position(), 4.0D);
            case DIYAN_QIANG -> level.sendParticles(new BlockParticleOption(
                            ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState()),
                    getX(), getY() + 0.7D, getZ(),
                    22, 0.45D, 0.75D, 0.45D, 0.08D);
            case HANYU_FENGDI -> {
                SpellImpactParticles.shockRing(level, position().add(0.0D, 0.10D, 0.0D),
                        ModParticles.METHOD_QI_ICE.get(), 1.0D, 42, 0.26D);
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        getX(), getY() + 0.18D, getZ(),
                        45, 4.8D, 0.22D, 4.8D, 0.07D);
            }
            case TIANGUANG_GUANSHA -> {
                SpellImpactParticles.shockRing(level, position().add(0.0D, 0.10D, 0.0D),
                        ModParticles.METHOD_QI_YANG.get(), 0.8D, 32, 0.23D);
                level.sendParticles(ParticleTypes.END_ROD,
                        getX(), getY() + 0.2D, getZ(),
                        24, 1.5D, 0.25D, 1.5D, 0.05D);
            }
            case TIANFENG_ZHUI -> SpellImpactParticles.shockRing(
                    level, position().add(0.0D, 0.10D, 0.0D),
                    ModParticles.METHOD_QI_WIND.get(), 1.0D, 34, 0.20D);
            case JIUXIAO_FENGLEI_YU -> {
                level.sendParticles(ModParticles.METHOD_QI_WIND.get(),
                        getX(), getY() + 0.5D, getZ(),
                        42, 4.0D, 1.2D, 4.0D, 0.18D);
                level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                        getX(), getY() + 0.8D, getZ(),
                        36, 3.2D, 1.5D, 3.2D, 0.20D);
            }
            default -> {
            }
        }
    }

    public static void damageFireSensitiveAreas(
            ServerLevel level, Vec3 center, double radius, int lifetimeDamage) {
        MethodTechniqueAreaEntity.damageFireSensitiveAreas(
                level, center, radius, lifetimeDamage);
        AABB bounds = new AABB(center, center).inflate(radius);
        for (ElementalTechniqueAreaEntity effect : level.getEntitiesOfClass(
                ElementalTechniqueAreaEntity.class, bounds,
                effect -> effect.kind() == Kind.WANTENG_JIAOSHA
                        || effect.kind() == Kind.HANYU_FENGDI)) {
            effect.lifePenalty += Math.max(1, lifetimeDamage);
            level.sendParticles(ParticleTypes.SMOKE,
                    effect.getX(), effect.getY() + 0.4D, effect.getZ(),
                    16, 0.7D, 0.35D, 0.7D, 0.06D);
        }
    }

    private void extinguishFireBlocks(ServerLevel level, AABB bounds) {
        LivingEntity owner = resolveOwner();
        if (owner == null || !terrainDamageAllowed(level, owner)) return;
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(cursor).is(BlockTags.FIRE)) {
                com.example.immortal_cultivation_mod.spell.SpellDestruction.breakBlock(level, cursor, false, owner);
            }
        }
    }

    private void ringParticles(
            ServerLevel level, ParticleOptions particle, double radius, int count, double speed) {
        ringParticlesAt(level, position(), particle, radius, count, speed);
    }

    private void ringParticlesAt(
            ServerLevel level, Vec3 center, ParticleOptions particle,
            double radius, int count, double speed) {
        if (tickCount % 2 != 0) {
            return;
        }
        double phase = tickCount * 0.13D;
        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2.0D * i / count;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * radius,
                    center.y + 0.12D,
                    center.z + Math.sin(angle) * radius,
                    1, 0.02D, 0.03D, 0.02D, speed);
        }
    }

    private java.util.List<LivingEntity> radialEnemies(
            ServerLevel level, LivingEntity owner, double radius, double height) {
        return radialEnemiesAt(level, owner, position(), radius, height);
    }

    private java.util.List<LivingEntity> radialEnemiesAt(
            ServerLevel level, LivingEntity owner, Vec3 center, double radius, double height) {
        AABB bounds = new AABB(center, center).inflate(radius, height, radius);
        return enemies(level, owner, bounds).stream()
                .filter(target -> horizontalDistance(target.position(), center) <= radius)
                .toList();
    }

    private java.util.List<LivingEntity> enemies(
            ServerLevel level, LivingEntity owner, AABB bounds) {
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != owner && NpcProfile.isCombatEnemy(owner, target));
    }

    private boolean damage(LivingEntity owner, LivingEntity target, float base, String spellId) {
        return target.hurt(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.spellSource(damageSources().indirectMagic(this, owner), spellId),
                SpellDamageHelper.damage(owner,
                        SpellCharge.damage(base, chargeScale()), ModSpells.get(spellId)));
    }

    private boolean permitsContinuousEffect(
            LivingEntity owner, LivingEntity target, float pulseDamage, String spellId) {
        if (tickCount % 20 == 0) {
            return damage(owner, target, pulseDamage, spellId);
        }
        return !ForcefieldCenterBehavior.interceptAttackPath(
                owner, position().add(0.0D, 0.5D, 0.0D),
                target.getBoundingBox().getCenter(), 1.0F);
    }

    private LivingEntity resolveOwner() {
        return ownerId != null && level() instanceof ServerLevel level
                && level.getEntity(ownerId) instanceof LivingEntity owner ? owner : null;
    }

    private LivingEntity resolveTarget() {
        return targetId != null && level() instanceof ServerLevel level
                && level.getEntity(targetId) instanceof LivingEntity target ? target : null;
    }

    private boolean isDark(LivingEntity target) {
        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            return true;
        }
        if (target instanceof Player player) {
            return ModAttachments.getData(player).spiritRoots().contains(SpiritRoots.YIN);
        }
        return target instanceof net.minecraft.world.entity.PathfinderMob mob
                && target instanceof CultivationNpc npc
                && NpcProfile.spiritRootIds(mob, npc.npcArchetype())
                .contains(SpiritRoots.YIN);
    }

    private int duration() {
        return switch (kind()) {
            case JINGJI_TUSHENG -> 24;
            case WANTENG_JIAOSHA, CHIYAN_LONGJUAN -> 100;
            case DIYAN_QIANG -> 60;
            case SHANBENG_YIN -> fullyCharged() ? 54 : 44;
            case CANGLANG_CHONG -> 20;
            case WULEI_HONGDING -> 64;
            case BINGZHUI_YU -> 45;
            case HANYU_FENGDI -> 120;
            case TIANGUANG_GUANSHA -> fullyCharged() ? 38 : 28;
            case TIANFENG_ZHUI -> 100;
            case JIUXIAO_FENGLEI_YU -> 160;
        };
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(Kind.byId(tag.getInt("TechniqueKind")));
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
            entityData.set(DATA_OWNER, Optional.of(ownerId));
        }
        if (tag.hasUUID("Target")) {
            targetId = tag.getUUID("Target");
        }
        setDirection(new Vec3(tag.getDouble("DirectionX"), 0.0D, tag.getDouble("DirectionZ")));
        setChargeScale(tag.contains("ChargeScale")
                ? tag.getFloat("ChargeScale") : SpellCharge.NORMAL);
        setMomentumSnapshot(tag.contains("FengleiMomentumSnapshot")
                ? tag.getFloat("FengleiMomentumSnapshot") : 0.0F);
        lifePenalty = Math.max(0, tag.getInt("LifePenalty"));
        terrainBlocksDestroyed = Math.max(0, tag.getInt("TerrainBlocksDestroyed"));
        primaryTriggered = tag.getBoolean("PrimaryTriggered");
        targetLifted = tag.getBoolean("TargetLifted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TechniqueKind", kind().ordinal());
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        Vec3 direction = direction();
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionZ", direction.z);
        tag.putFloat("ChargeScale", chargeScale());
        tag.putFloat("FengleiMomentumSnapshot", momentumSnapshot());
        tag.putInt("LifePenalty", lifePenalty);
        tag.putInt("TerrainBlocksDestroyed", terrainBlocksDestroyed);
        tag.putBoolean("PrimaryTriggered", primaryTriggered);
        tag.putBoolean("TargetLifted", targetLifted);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
