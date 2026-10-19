package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.HanlanNingyuanJing;
import com.example.immortal_cultivation_mod.spell.ChangqingJue;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real moving/domain hitboxes used by the 玄岳 and 寒澜 cultivation methods. */
public final class MethodTechniqueAreaEntity extends Entity
        implements ForcefieldInterceptable, DefensePiercingProjectile {
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(MethodTechniqueAreaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
            SynchedEntityData.defineId(MethodTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
            SynchedEntityData.defineId(MethodTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(MethodTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_POWER =
            SynchedEntityData.defineId(MethodTechniqueAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(MethodTechniqueAreaEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);

    public enum Kind {
        JINYAN_SPIKES,
        SHANSUO_DOMAIN,
        WANFENG_BURIAL,
        CANGYUAN_FLOOD,
        HANYUAN_DOMAIN,
        XUANYUE_WALL,
        CHANGQING_GROWTH,
        CHANGQING_VINES;

        private static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private final Set<UUID> hitEntities = new HashSet<>();
    private final Map<UUID, Integer> insideTicks = new HashMap<>();
    private final List<UUID> terrainAnchors = new ArrayList<>();
    private UUID ownerId;
    private int lifePenalty;

    public MethodTechniqueAreaEntity(
            EntityType<? extends MethodTechniqueAreaEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static MethodTechniqueAreaEntity create(
            ServerLevel level, LivingEntity owner, Kind kind, Vec3 position,
            Vec3 direction, float chargeScale, float power) {
        MethodTechniqueAreaEntity area = ModEntities.METHOD_TECHNIQUE_AREA.get().create(level);
        if (area == null || owner == null) return null;
        area.setKind(kind);
        area.ownerId = owner.getUUID();
        area.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        area.setDirection(direction);
        area.entityData.set(DATA_CHARGE, SpellCharge.clamp(chargeScale));
        area.entityData.set(DATA_POWER, power);
        area.moveTo(position.x, position.y, position.z,
                (float) Math.toDegrees(Math.atan2(-area.direction().x, area.direction().z)), 0.0F);
        NpcProfile.markNpcOwnedEffect(area, owner);
        return area;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.JINYAN_SPIKES.ordinal());
        builder.define(DATA_DIRECTION_X, 0.0F);
        builder.define(DATA_DIRECTION_Z, 1.0F);
        builder.define(DATA_CHARGE, SpellCharge.NORMAL);
        builder.define(DATA_POWER, 0.0F);
        builder.define(DATA_OWNER, Optional.empty());
    }

    public Kind kind() {
        return Kind.byId(entityData.get(DATA_KIND));
    }

    public void setKind(Kind kind) {
        entityData.set(DATA_KIND, (kind == null ? Kind.JINYAN_SPIKES : kind).ordinal());
    }

    public float chargeScale() {
        return SpellCharge.clamp(entityData.get(DATA_CHARGE));
    }

    public float power() {
        return entityData.get(DATA_POWER);
    }

    public Vec3 direction() {
        Vec3 value = new Vec3(entityData.get(DATA_DIRECTION_X), 0.0D,
                entityData.get(DATA_DIRECTION_Z));
        return value.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : value.normalize();
    }

    private void setDirection(Vec3 value) {
        Vec3 horizontal = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0D, value.z);
        if (horizontal.lengthSqr() < 1.0E-6D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize();
        entityData.set(DATA_DIRECTION_X, (float) horizontal.x);
        entityData.set(DATA_DIRECTION_Z, (float) horizontal.z);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        if (kind() == Kind.XUANYUE_WALL) {
            // The wall is independent terrain once raised; its VFX must outlive a dead or unloaded caster.
            tickWallRise(level);
        } else {
            LivingEntity owner = resolveOwner();
            if (owner == null || !owner.isAlive() || NpcProfile.isOrphanedNpcEffect(this, owner)) {
                if (kind() == Kind.CANGYUAN_FLOOD) {
                    collapseTerrain(level);
                }
                discard();
                return;
            }
            NpcProfile.markNpcOwnedEffect(this, owner);
            BlockBreakingSettings.runWithSource(
                    BlockBreakingSettings.Category.TECHNIQUES, owner, () -> {
                switch (kind()) {
                    case JINYAN_SPIKES -> tickJinyan(level, owner);
                    case SHANSUO_DOMAIN -> tickShansuo(level, owner);
                    case WANFENG_BURIAL -> tickWanfeng(level, owner);
                    case CANGYUAN_FLOOD -> tickFlood(level, owner);
                    case HANYUAN_DOMAIN -> tickFrozenDomain(level, owner);
                    case CHANGQING_GROWTH -> tickChangqingGrowth(level, owner);
                    case CHANGQING_VINES -> tickChangqingVines(level, owner);
                    case XUANYUE_WALL -> {
                    }
                }
            });
        }
        if (tickCount + lifePenalty >= duration()) {
            if (kind() == Kind.WANFENG_BURIAL || kind() == Kind.HANYUAN_DOMAIN
                    || kind() == Kind.CANGYUAN_FLOOD) {
                collapseTerrain(level);
            }
            endingVfx(level);
            discard();
        }
    }

    private void tickJinyan(ServerLevel level, LivingEntity owner) {
        if (tickCount == 1) {
            ring(level, ModParticles.METHOD_QI_METAL.get(), 4.0D, 38, 0.16D);
            level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 0.9F, 1.55F);
        }
        if (tickCount % 5 != 0) return;
        for (LivingEntity target : enemies(level, owner, 4.5D, 3.5D)) {
            if (!hitEntities.add(target.getUUID()) || blocked(owner, target, 360.0F)) continue;
            if (damage(owner, target, 360.0F, ModSpells.JINYAN_SHENGFENG)) {
                target.addEffect(new MobEffectInstance(ModEffects.DAZE, 12, 0,
                        false, true, true), owner);
                target.push(0.0D, 0.45D, 0.0D);
                level.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        18, 0.28D, 0.45D, 0.28D, 0.14D);
            }
        }
    }

    private void tickWallRise(ServerLevel level) {
        int realm = Math.max(0, Math.round(power()));
        int width = 5 + realm * 2;
        int wingLength = realm + 2;
        Vec3 side = new Vec3(-direction().z, 0.0D, direction().x);

        if (tickCount <= 24 && tickCount % 2 == 0) {
            int column = Math.floorMod(tickCount / 2, width);
            double lateral = column - (width - 1) * 0.5D;
            Vec3 point = position().add(side.scale(lateral));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.POLISHED_DEEPSLATE.defaultBlockState()),
                    point.x, point.y + 0.45D + tickCount * 0.12D, point.z,
                    10, 0.38D, 0.35D, 0.38D, 0.10D);
            level.sendParticles(column % 3 == 0
                            ? ModParticles.METHOD_QI_METAL.get()
                            : ModParticles.METHOD_QI_EARTH.get(),
                    point.x, point.y + 0.25D + tickCount * 0.13D, point.z,
                    5, 0.24D, 0.28D, 0.24D, 0.045D);

            if (realm >= 1) {
                int depth = Math.min(wingLength, 1 + tickCount / 6);
                for (int sign : new int[]{-1, 1}) {
                    Vec3 wing = position()
                            .add(side.scale(sign * width * 0.5D))
                            .subtract(direction().scale(depth));
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                    Blocks.POLISHED_DEEPSLATE.defaultBlockState()),
                            wing.x, wing.y + 0.55D + tickCount * 0.10D, wing.z,
                            8, 0.30D, 0.32D, 0.30D, 0.085D);
                    level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                            wing.x, wing.y + 0.35D + tickCount * 0.11D, wing.z,
                            4, 0.18D, 0.25D, 0.18D, 0.035D);
                }
            }
            if (tickCount % 6 == 0) {
                ring(level, ModParticles.METHOD_QI_EARTH.get(),
                        0.8D + tickCount * 0.10D, 24, 0.09D);
            }
        }

        // Keep the full front and both side walls spiritually outlined for their entire lifetime.
        if (tickCount % 8 == 0) {
            for (int column = 0; column < width; column += 2) {
                double lateral = column - (width - 1) * 0.5D;
                Vec3 point = position().add(side.scale(lateral));
                level.sendParticles((column + tickCount / 8) % 3 == 0
                                ? ModParticles.METHOD_QI_METAL.get()
                                : ModParticles.METHOD_QI_EARTH.get(),
                        point.x, point.y + 1.0D + (column % 3) * 0.72D, point.z,
                        2, 0.10D, 0.42D, 0.10D, 0.018D);
            }
            if (realm >= 1) {
                for (int sign : new int[]{-1, 1}) {
                    for (int depth = 1; depth <= wingLength; depth++) {
                        Vec3 point = position()
                                .add(side.scale(sign * width * 0.5D))
                                .subtract(direction().scale(depth));
                        level.sendParticles((depth + tickCount / 8) % 3 == 0
                                        ? ModParticles.METHOD_QI_METAL.get()
                                        : ModParticles.METHOD_QI_EARTH.get(),
                                point.x, point.y + 1.0D + (depth % 3) * 0.68D, point.z,
                                1, 0.08D, 0.34D, 0.08D, 0.014D);
                    }
                }
            }
        }
    }

    private void tickShansuo(ServerLevel level, LivingEntity owner) {
        double radius = 6.0D + Math.max(0.0F, power() - 1.0F) * 1.5D;
        if (tickCount % 4 == 0) ring(level, ModParticles.METHOD_QI_EARTH.get(),
                radius, 32, 0.035D);
        for (LivingEntity target : enemies(level, owner, radius, 8.0D)) {
            if (blocked(owner, target, 80.0F)) continue;
            target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW,
                    12, 39, false, true, true), owner);
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 0.78D,
                    Math.min(motion.y, target.onGround() ? 0.0D : -0.22D),
                    motion.z * 0.78D);
            target.hurtMarked = true;
            if (tickCount % 8 == 0) {
                level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                        target.getX(), target.getY() + target.getBbHeight() * 0.72D,
                        target.getZ(), 5,
                        target.getBbWidth() * 0.25D,
                        target.getBbHeight() * 0.34D,
                        target.getBbWidth() * 0.25D, 0.045D);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                Blocks.COBBLED_DEEPSLATE.defaultBlockState()),
                        target.getX(), target.getY() + 0.04D, target.getZ(),
                        target.onGround() ? 6 : 2,
                        target.getBbWidth() * 0.28D, 0.04D,
                        target.getBbWidth() * 0.28D, 0.045D);
            }
        }
    }

    private void tickWanfeng(ServerLevel level, LivingEntity owner) {
        if (tickCount == 1) {
            ring(level, ModParticles.METHOD_QI_EARTH.get(), 14.5D, 108, 0.035D);
            level.playSound(null, blockPosition(), SoundEvents.STONE_PLACE,
                    SoundSource.PLAYERS, 1.1F, 0.52F);
        } else if (tickCount == 14) {
            createBurialPillars(level);
            phaseDamage(level, owner, 340.0F, 0.42D);
            level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.1F, 0.72F);
        } else if (tickCount == 42) {
            createBurialBlades(level);
            phaseDamage(level, owner, 520.0F, 0.25D);
            ring(level, ModParticles.METHOD_QI_METAL.get(), 14.2D, 108, 0.22D);
            level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 1.15F, 1.42F);
        } else if (tickCount == 92) {
            collapseTerrain(level);
            phaseDamage(level, owner, 820.0F, -0.65D);
            ring(level, ModParticles.METHOD_QI_EARTH.get(), 15.0D, 132, 0.30D);
            ring(level, ModParticles.METHOD_QI_METAL.get(), 13.8D, 108, 0.26D);
            level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.5F, 0.58F);
        }
        if (tickCount >= 14 && tickCount < 92) {
            for (LivingEntity target : enemies(level, owner, 15.0D, 12.0D)) {
                if (blocked(owner, target, 100.0F)) continue;
                target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW,
                        12, 24, false, true, true), owner);
                Vec3 toward = position().subtract(target.position());
                if (toward.lengthSqr() > 0.01D) {
                    toward = toward.normalize().scale(0.025D);
                    target.push(toward.x, 0.0D, toward.z);
                }
            }
        }
        if (tickCount % 4 == 0) burialVfx(level);
    }

    private void tickFlood(ServerLevel level, LivingEntity owner) {
        // Clear a saved water front from older versions before advancing the wave.
        collapseTerrain(level);
        double environment = Mth.clamp(power(), 0.0F, 1.0F);
        double speed = 0.50D;
        Vec3 forward = direction().scale(speed);
        setPos(getX() + forward.x, getY(), getZ() + forward.z);
        hasImpulse = true;
        double halfWidth = 4.5D + environment;
        Vec3 side = new Vec3(-direction().z, 0.0D, direction().x);
        floodVfx(level, side, halfWidth);
        AABB bounds = getBoundingBox().inflate(halfWidth, 3.5D, halfWidth)
                .move(direction().scale(0.45D));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != owner && NpcProfile.isCombatEnemy(owner, target))) {
            Vec3 relative = target.position().subtract(position());
            if (Math.abs(relative.dot(side)) > halfWidth
                    || Math.abs(relative.dot(direction())) > 1.7D
                    || blocked(owner, target, 420.0F)) continue;
            HanlanNingyuanJing.applyDamp(target, 180, owner);
            if (hitEntities.add(target.getUUID())) {
                damage(owner, target, 420.0F, ModSpells.CANGYUAN_HONGLIU);
            }
            target.setDeltaMovement(direction().scale(0.70D + environment * 0.12D)
                    .add(0.0D, 0.16D, 0.0D));
            target.hurtMarked = true;
            target.clearFire();
        }
        extinguishFire(level, halfWidth);
        // The wave is visual terrain: vanilla fluid ticks can outlive even a
        // two-tick temporary source and create untracked, permanent water.
    }

    private void tickChangqingGrowth(ServerLevel level, LivingEntity owner) {
        double radius = 9.0D;
        if (tickCount == 1) {
            createTemporaryGrowth(level, radius);
            ring(level, ModParticles.METHOD_QI_WOOD.get(), radius, 72, 0.16D);
            ring(level, ModParticles.METHOD_QI_YANG.get(), radius * 0.72D, 48, 0.11D);
        }
        if (tickCount % 5 == 0) {
            double expanding = Math.min(radius, 1.0D + tickCount * 0.15D);
            ring(level, ModParticles.METHOD_QI_WOOD.get(), expanding, 42, 0.035D);
            branchingRootVfx(level, radius, tickCount * 0.025D);
        }
        if (tickCount % 20 == 0 && owner instanceof ServerPlayer player
                && player.distanceToSqr(this) <= radius * radius) {
            ChangqingJue.nourishFromGrowthField(player, 1.8F);
        }
        if (tickCount % 20 == 0) {
            nourishExistingPlants(level, radius, owner);
        }
    }

    private void tickChangqingVines(ServerLevel level, LivingEntity owner) {
        double radius = 5.0D;
        if (tickCount == 1) {
            ring(level, ModParticles.METHOD_QI_WOOD.get(), radius, 52, 0.14D);
        }
        if (tickCount % 4 == 0) {
            ring(level, ModParticles.METHOD_QI_WOOD.get(),
                    radius * (0.78D + 0.08D * Math.sin(tickCount * 0.22D)),
                    34, 0.028D);
            branchingRootVfx(level, radius, -tickCount * 0.018D);
        }
        Set<UUID> present = new HashSet<>();
        for (LivingEntity target : enemies(level, owner, radius, 4.0D)) {
            if (blocked(owner, target, 80.0F)) continue;
            present.add(target.getUUID());
            int stayed = insideTicks.merge(target.getUUID(), 1, Integer::sum);
            target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW,
                    12, 44, false, true, true), owner);
            if (stayed >= 40) {
                target.addEffect(new MobEffectInstance(ModEffects.DINGSHEN,
                        10, 0, false, true, true), owner);
            }
            if (tickCount % 4 == 0) vineWrapVfx(level, target, stayed >= 40);
        }
        insideTicks.keySet().retainAll(present);
    }

    private void createTemporaryGrowth(ServerLevel level, double radius) {
        Map<BlockPos, BlockState> plants = new LinkedHashMap<>();
        int samples = 96;
        for (int index = 0; index < samples; index++) {
            double angle = index * 2.399963229728653D;
            double distance = radius * Math.sqrt((index + 0.5D) / samples);
            BlockPos surface = findSurface(level, BlockPos.containing(
                    getX() + Math.cos(angle) * distance,
                    getY() + 3.0D,
                    getZ() + Math.sin(angle) * distance), 7);
            BlockPos plantPos = surface.above();
            BlockState ground = level.getBlockState(surface);
            if (!TemporaryTerrainEntity.canReplace(level, plantPos)
                    || !(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT)
                    || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.ROOTED_DIRT)
                    || ground.is(Blocks.MOSS_BLOCK))) {
                continue;
            }
            BlockState plant = switch (index % 11) {
                case 0 -> ModBlocks.QINGLING_GRASS.get().defaultBlockState();
                case 1 -> Blocks.DANDELION.defaultBlockState();
                case 2 -> Blocks.POPPY.defaultBlockState();
                case 3 -> Blocks.FERN.defaultBlockState();
                default -> Blocks.SHORT_GRASS.defaultBlockState();
            };
            if (plant.canSurvive(level, plantPos)) plants.put(plantPos, plant);
        }
        track(TemporaryTerrainEntity.place(level, plants, duration()));
    }

    private void nourishExistingPlants(
            ServerLevel level, double radius, LivingEntity owner) {
        int pulses = 0;
        BlockPos center = blockPosition();
        for (int attempt = 0; attempt < 20 && pulses < 5; attempt++) {
            BlockPos pos = center.offset(
                    level.random.nextInt((int)radius * 2 + 1) - (int)radius,
                    level.random.nextInt(5) - 2,
                    level.random.nextInt((int)radius * 2 + 1) - (int)radius);
            if (pos.distSqr(center) > radius * radius) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock growable)
                    || !growable.isValidBonemealTarget(level, pos, state)
                    || !growable.isBonemealSuccess(level, level.random, pos, state)) continue;
            growable.performBonemeal(level, level.random, pos, state);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                    6, 0.25D, 0.25D, 0.25D, 0.02D);
            pulses++;
        }
    }

    private void branchingRootVfx(ServerLevel level, double radius, double phase) {
        for (int branch = 0; branch < 9; branch++) {
            double baseAngle = Math.PI * 2.0D * branch / 9.0D + phase;
            for (int segment = 1; segment <= 6; segment++) {
                double t = segment / 6.0D;
                double bend = Math.sin(t * Math.PI * 2.0D + branch * 0.73D) * 0.24D;
                double angle = baseAngle + bend;
                Vec3 point = position().add(
                        Math.cos(angle) * radius * t,
                        0.08D + Math.sin(t * Math.PI) * 0.07D,
                        Math.sin(angle) * radius * t);
                level.sendParticles(segment % 3 == 0
                                ? ModParticles.METHOD_QI_YANG.get()
                                : ModParticles.METHOD_QI_WOOD.get(),
                        point.x, point.y, point.z, 1,
                        0.0D, 0.008D, 0.0D, 0.0D);
            }
        }
    }

    private static void vineWrapVfx(
            ServerLevel level, LivingEntity target, boolean rooted) {
        int strands = rooted ? 5 : 3;
        int segments = rooted ? 10 : 7;
        for (int strand = 0; strand < strands; strand++) {
            double phase = Math.PI * 2.0D * strand / strands + target.tickCount * 0.07D;
            for (int segment = 0; segment < segments; segment++) {
                double t = segment / (double)Math.max(1, segments - 1);
                double angle = phase + t * Math.PI * (rooted ? 2.8D : 1.8D);
                double radius = target.getBbWidth() * (0.48D - t * 0.12D);
                level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                        target.getX() + Math.cos(angle) * radius,
                        target.getY() + 0.06D + t * target.getBbHeight() * 0.78D,
                        target.getZ() + Math.sin(angle) * radius,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void tickFrozenDomain(ServerLevel level, LivingEntity owner) {
        double radius = 40.0D;
        if (tickCount <= 16) freezeExpandingRing(level, tickCount * 2.6D, 3.2D);
        if (tickCount == 1) createIceBoundary(level, radius);
        if (tickCount % 4 == 0) {
            ring(level, ModParticles.METHOD_QI_ICE.get(),
                    Math.min(radius, 3.0D + tickCount * 0.45D), 52, 0.045D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2.0D, getZ(),
                    30, radius * 0.72D, 2.0D, radius * 0.72D, 0.025D);
        }
        for (LivingEntity target : enemies(level, owner, radius, 12.0D)) {
            if (blocked(owner, target, 75.0F)) continue;
            boolean water = HanlanNingyuanJing.isStandingInWater(target);
            boolean damp = HanlanNingyuanJing.isDamp(target) || water;
            int accumulated = insideTicks.merge(target.getUUID(), 1, Integer::sum);
            target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW,
                    12, damp ? 49 : 34, false, true, true), owner);
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 20,
                    target.getTicksFrozen() + (water ? 8 : damp ? 5 : 2)));
            int trigger = water ? 20 : damp ? 40 : 80;
            if (accumulated >= trigger && accumulated % trigger == 0) {
                target.addEffect(new MobEffectInstance(ModEffects.FROZEN,
                        water || damp ? 30 : 20, 0, false, true, true), owner);
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        32, 0.35D, 0.65D, 0.35D, 0.07D);
            }
            if (tickCount % 20 == 0) {
                damage(owner, target, damp ? 90.0F : 60.0F, ModSpells.HANYUAN_FENGJIE);
            }
        }
        if (tickCount % 10 == 0 && nearbyFire(level, radius)) lifePenalty += 7;
    }

    private void phaseDamage(ServerLevel level, LivingEntity owner, float base, double vertical) {
        for (LivingEntity target : enemies(level, owner, 15.0D, 12.0D)) {
            if (blocked(owner, target, base)) continue;
            if (damage(owner, target, base, ModSpells.WANFENG_YUEZANG)) {
                Vec3 toward = position().subtract(target.position());
                if (toward.lengthSqr() < 0.01D) toward = new Vec3(0.0D, 0.0D, 1.0D);
                toward = toward.normalize();
                target.push(toward.x * 0.28D, vertical, toward.z * 0.28D);
                target.hurtMarked = true;
            }
        }
    }

    private boolean damage(LivingEntity owner, LivingEntity target, float base, String spellId) {
        return target.hurt(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.spellSource(damageSources().indirectMagic(this, owner), spellId),
                SpellDamageHelper.damage(owner, SpellCharge.damage(base, chargeScale()),
                        ModSpells.get(spellId)));
    }

    private boolean blocked(LivingEntity owner, LivingEntity target, float power) {
        return ForcefieldCenterBehavior.interceptAttackPath(owner,
                position().add(0.0D, 0.6D, 0.0D),
                target.getBoundingBox().getCenter(),
                SpellCharge.damage(power, chargeScale()));
    }

    private List<LivingEntity> enemies(
            ServerLevel level, LivingEntity owner, double radius, double height) {
        AABB bounds = new AABB(position(), position()).inflate(radius, height, radius);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != owner && NpcProfile.isCombatEnemy(owner, target)
                        && horizontalDistanceSqr(target.position(), position()) <= radiusSqr);
    }

    private void createBurialPillars(ServerLevel level) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        int columns = 18 + Math.max(0, Math.round(power())) * 3;
        for (int i = 0; i < columns; i++) {
            double angle = Math.PI * 2.0D * i / columns;
            double radius = 11.8D + (i & 1) * 1.7D;
            BlockPos surface = findSurface(level, BlockPos.containing(
                    getX() + Math.cos(angle) * radius, getY() + 2.0D,
                    getZ() + Math.sin(angle) * radius), 8);
            int height = 4 + (i % 3);
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    int edge = Math.abs(offsetX) + Math.abs(offsetZ);
                    if (edge > 1 && i % 3 != 0) continue;
                    int columnHeight = Math.max(2, height - (edge > 1 ? 1 : 0)
                            - (Math.floorMod(i + offsetX * 3 + offsetZ * 5, 4) == 0 ? 1 : 0));
                    for (int y = 1; y <= columnHeight; y++) {
                        BlockPos pos = surface.offset(offsetX, y, offsetZ);
                        BlockState state = y == columnHeight || (i + y + edge) % 5 == 0
                                ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                                : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                        blocks.put(pos, state);
                    }
                }
            }
        }
        track(TemporaryTerrainEntity.place(level, blocks, 120));
    }

    private void createBurialBlades(ServerLevel level) {
        for (int i = 0; i < 54; i++) {
            double angle = Math.PI * 2.0D * i / 54.0D + (i % 3) * 0.11D;
            double radius = 2.2D + (i % 9) * 1.45D;
            BlockPos surface = findSurface(level, BlockPos.containing(
                    getX() + Math.cos(angle) * radius, getY() + 3.0D,
                    getZ() + Math.sin(angle) * radius), 10);
            level.sendParticles(i % 3 == 0
                            ? ModParticles.METHOD_QI_EARTH.get()
                            : ModParticles.METHOD_QI_METAL.get(),
                    surface.getX() + 0.5D, surface.getY() + 1.1D,
                    surface.getZ() + 0.5D,
                    5, 0.18D, 0.42D, 0.18D, 0.065D);
        }
    }

    private void collapseTerrain(ServerLevel level) {
        for (UUID id : List.copyOf(terrainAnchors)) {
            if (level.getEntity(id) instanceof TemporaryTerrainEntity terrain) {
                terrain.cleanupNow();
            }
        }
        terrainAnchors.clear();
    }

    private void track(TemporaryTerrainEntity terrain) {
        if (terrain != null) terrainAnchors.add(terrain.getUUID());
    }

    private void createIceBoundary(ServerLevel level, double radius) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            BlockPos surface = findSurface(level, BlockPos.containing(
                    getX() + Math.cos(angle) * radius, getY() + 3.0D,
                    getZ() + Math.sin(angle) * radius), 10);
            int height = 1 + i % 4;
            for (int y = 1; y <= height; y++) {
                blocks.put(surface.above(y), y == height
                        ? Blocks.BLUE_ICE.defaultBlockState()
                        : Blocks.PACKED_ICE.defaultBlockState());
            }
        }
        track(TemporaryTerrainEntity.place(level, blocks, duration()));
    }

    private void freezeExpandingRing(ServerLevel level, double ringRadius, double thickness) {
        LivingEntity owner = resolveOwner();
        if (owner == null || !BlockBreakingSettings.enabled(owner,
                BlockBreakingSettings.Category.TECHNIQUES)) return;
        int samples = Math.max(24, (int) Math.ceil(Math.PI * 2.0D * ringRadius));
        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2.0D * i / samples;
            for (double radial = Math.max(0.0D, ringRadius - thickness);
                 radial <= Math.min(40.0D, ringRadius); radial += 1.0D) {
                BlockPos center = BlockPos.containing(
                        getX() + Math.cos(angle) * radial, getY(),
                        getZ() + Math.sin(angle) * radial);
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = center.offset(0, y, 0);
                    if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) {
                        com.example.immortal_cultivation_mod.spell.TerrainTransforms.replace(level, pos,
                                Blocks.FROSTED_ICE.defaultBlockState(), owner,
                                BlockBreakingSettings.Category.TECHNIQUES);
                    }
                }
            }
        }
    }

    private void extinguishFire(ServerLevel level, double halfWidth) {
        LivingEntity owner = resolveOwner();
        if (owner == null || !BlockBreakingSettings.enabled(owner,
                BlockBreakingSettings.Category.TECHNIQUES)) return;
        Vec3 side = new Vec3(-direction().z, 0.0D, direction().x);
        for (double lateral = -halfWidth; lateral <= halfWidth; lateral += 1.0D) {
            BlockPos base = BlockPos.containing(position().add(side.scale(lateral)));
            for (int y = -1; y <= 3; y++) {
                BlockPos pos = base.offset(0, y, 0);
                if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                    BlockBreakingSettings.runWithSource(
                            BlockBreakingSettings.Category.TECHNIQUES, owner,
                            () -> com.example.immortal_cultivation_mod.spell.SpellDestruction.breakBlock(level, pos, false, owner));
                }
            }
        }
    }

    private boolean nearbyFire(ServerLevel level, double radius) {
        if (!level.getEntitiesOfClass(LivingEntity.class,
                new AABB(position(), position()).inflate(radius, 6.0D, radius),
                LivingEntity::isOnFire).isEmpty()) {
            return true;
        }
        int step = 4;
        int limit = (int) radius;
        for (int x = -limit; x <= limit; x += step) {
            for (int z = -limit; z <= limit; z += step) {
                if (x * x + z * z > radius * radius) continue;
                for (int y = -1; y <= 3; y++) {
                    if (level.getBlockState(blockPosition().offset(x, y, z)).is(BlockTags.FIRE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void burialVfx(ServerLevel level) {
        for (int i = 0; i < 12; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 2.0D + level.random.nextDouble() * 13.0D;
            ParticleOptions particle = i % 3 == 0
                    ? ModParticles.METHOD_QI_METAL.get()
                    : new BlockParticleOption(ParticleTypes.BLOCK,
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            level.sendParticles(particle,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.4D + level.random.nextDouble() * 5.5D,
                    getZ() + Math.sin(angle) * radius,
                    1, 0.08D, 0.08D, 0.08D, 0.07D);
        }
    }

    private void floodVfx(ServerLevel level, Vec3 side, double halfWidth) {
        for (int i = 0; i < 18; i++) {
            double lateral = (level.random.nextDouble() * 2.0D - 1.0D) * halfWidth;
            double height = 0.25D + level.random.nextDouble() * 3.8D;
            Vec3 point = position().add(side.scale(lateral)).add(direction().scale(0.45D))
                    .add(0.0D, height, 0.0D);
            level.sendParticles(i % 4 == 0 ? ParticleTypes.SPLASH
                            : ModParticles.METHOD_QI_WATER.get(),
                    point.x, point.y, point.z, 1,
                    direction().x * 0.12D, 0.06D, direction().z * 0.12D, 0.02D);
        }
        level.sendParticles(ParticleTypes.CLOUD,
                getX(), getY() + 3.8D, getZ(),
                6, halfWidth * 0.55D, 0.25D, 0.5D, 0.04D);
        level.sendParticles(ModParticles.METHOD_QI_ICE.get(),
                getX(), getY() + 1.9D, getZ(),
                3, halfWidth * 0.55D, 1.4D, 0.45D, 0.025D);
    }

    public static void damageFireSensitiveAreas(
            ServerLevel level, Vec3 center, double radius, int lifetimeDamage) {
        AABB bounds = new AABB(center, center).inflate(radius);
        for (MethodTechniqueAreaEntity area : level.getEntitiesOfClass(
                MethodTechniqueAreaEntity.class, bounds,
                area -> area.kind() == Kind.HANYUAN_DOMAIN)) {
            area.lifePenalty += Math.max(1, lifetimeDamage);
            level.sendParticles(ParticleTypes.CLOUD,
                    center.x, center.y + 0.35D, center.z,
                    30, Math.max(0.55D, radius * 0.35D), 0.65D,
                    Math.max(0.55D, radius * 0.35D), 0.12D);
            level.sendParticles(ParticleTypes.SMOKE,
                    center.x, center.y + 0.45D, center.z,
                    18, Math.max(0.45D, radius * 0.25D), 0.40D,
                    Math.max(0.45D, radius * 0.25D), 0.07D);
            level.sendParticles(ModParticles.METHOD_QI_ICE.get(),
                    center.x, center.y + 0.18D, center.z,
                    20, Math.max(0.65D, radius * 0.45D), 0.18D,
                    Math.max(0.65D, radius * 0.45D), 0.09D);
        }
    }

    private void endingVfx(ServerLevel level) {
        switch (kind()) {
            case JINYAN_SPIKES -> ring(level, ModParticles.METHOD_QI_METAL.get(),
                    4.0D, 30, 0.12D);
            case SHANSUO_DOMAIN -> ring(level,
                    ModParticles.METHOD_QI_EARTH.get(), 7.0D, 42, 0.14D);
            case WANFENG_BURIAL -> ring(level,
                    ModParticles.METHOD_QI_EARTH.get(), 15.0D, 96, 0.14D);
            case CANGYUAN_FLOOD -> level.sendParticles(ParticleTypes.SPLASH,
                    getX(), getY() + 0.8D, getZ(), 36, 3.0D, 0.7D, 3.0D, 0.11D);
            case HANYUAN_DOMAIN -> ring(level, ModParticles.METHOD_QI_ICE.get(),
                    40.0D, 90, 0.18D);
            case XUANYUE_WALL -> ring(level, ModParticles.METHOD_QI_EARTH.get(),
                    3.0D, 28, 0.10D);
            case CHANGQING_GROWTH -> ring(level, ModParticles.METHOD_QI_WOOD.get(),
                    9.0D, 72, 0.12D);
            case CHANGQING_VINES -> ring(level, ModParticles.METHOD_QI_WOOD.get(),
                    5.0D, 46, 0.10D);
        }
    }

    private void ring(
            ServerLevel level, ParticleOptions particle, double radius, int count, double speed) {
        double phase = tickCount * 0.13D;
        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2.0D * i / count;
            level.sendParticles(particle,
                    getX() + Math.cos(angle) * radius, getY() + 0.12D,
                    getZ() + Math.sin(angle) * radius,
                    1, Math.cos(angle) * speed, 0.025D,
                    Math.sin(angle) * speed, 0.0D);
        }
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos origin, int search) {
        for (int offset = search; offset >= -search; offset--) {
            BlockPos ground = origin.offset(0, offset, 0);
            BlockState state = level.getBlockState(ground);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && state.isFaceSturdy(level, ground, Direction.UP)
                    && TemporaryTerrainEntity.canReplace(level, ground.above())) {
                return ground;
            }
        }
        return origin.below();
    }

    private LivingEntity resolveOwner() {
        return ownerId != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(ownerId) instanceof LivingEntity living ? living : null;
    }

    private int duration() {
        return switch (kind()) {
            case JINYAN_SPIKES -> 60;
            case SHANSUO_DOMAIN, HANYUAN_DOMAIN -> 160;
            case WANFENG_BURIAL -> 130;
            case CANGYUAN_FLOOD -> 36;
            case XUANYUE_WALL -> 300;
            case CHANGQING_GROWTH -> 200;
            case CHANGQING_VINES -> 100;
        };
    }

    public int visualDuration() {
        return duration();
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    @Override
    public Entity forcefieldOwner() {
        return resolveOwner();
    }

    @Override
    public Vec3 forcefieldMovement() {
        return kind() == Kind.CANGYUAN_FLOOD ? direction().scale(0.56D) : Vec3.ZERO;
    }

    @Override
    public float forcefieldBarrierDamage() {
        float base = switch (kind()) {
            case JINYAN_SPIKES -> 360.0F;
            case SHANSUO_DOMAIN -> 100.0F;
            case WANFENG_BURIAL -> 820.0F;
            case CANGYUAN_FLOOD -> 420.0F;
            case HANYUAN_DOMAIN -> 180.0F;
            case XUANYUE_WALL -> 0.0F;
            case CHANGQING_GROWTH, CHANGQING_VINES -> 0.0F;
        };
        return SpellCharge.damage(base, chargeScale());
    }

    @Override
    public float defenseBypass() {
        return switch (kind()) {
            case JINYAN_SPIKES -> 0.35F;
            case WANFENG_BURIAL -> 0.25F;
            default -> 0.0F;
        };
    }

    @Override
    public float shieldDamageMultiplier() {
        return switch (kind()) {
            case JINYAN_SPIKES -> 1.50F;
            case WANFENG_BURIAL -> 1.35F;
            default -> 1.0F;
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(Kind.byId(tag.getInt("Kind")));
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
            entityData.set(DATA_OWNER, Optional.of(ownerId));
        }
        setDirection(new Vec3(tag.getDouble("DirectionX"), 0.0D,
                tag.getDouble("DirectionZ")));
        entityData.set(DATA_CHARGE, SpellCharge.clamp(
                tag.contains("Charge") ? tag.getFloat("Charge") : SpellCharge.NORMAL));
        entityData.set(DATA_POWER, tag.getFloat("Power"));
        lifePenalty = Math.max(0, tag.getInt("LifePenalty"));
        terrainAnchors.clear();
        for (int index = 0; index < tag.getInt("TerrainAnchorCount"); index++) {
            String key = "TerrainAnchor" + index;
            if (tag.hasUUID(key)) terrainAnchors.add(tag.getUUID(key));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind().ordinal());
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        Vec3 direction = direction();
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionZ", direction.z);
        tag.putFloat("Charge", chargeScale());
        tag.putFloat("Power", power());
        tag.putInt("LifePenalty", lifePenalty);
        tag.putInt("TerrainAnchorCount", terrainAnchors.size());
        for (int index = 0; index < terrainAnchors.size(); index++) {
            tag.putUUID("TerrainAnchor" + index, terrainAnchors.get(index));
        }
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
