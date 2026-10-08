package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Persistent world-space VFX and mechanics for ward talismans. */
public final class TalismanWardEntity extends Entity {
    /** Gameplay dimensions shared with the client renderer for the ancient Jinkong field. */
    public static final double FORBIDDEN_AIR_RADIUS = 24.0D;
    public static final double FORBIDDEN_AIR_HEIGHT = 48.0D;
    public static final double FORBIDDEN_AIR_BELOW = 4.0D;
    private static final double FORBIDDEN_AIR_RADIUS_SQR =
            FORBIDDEN_AIR_RADIUS * FORBIDDEN_AIR_RADIUS;
    private static final DustParticleOptions DIM_GOLD =
            new DustParticleOptions(new Vector3f(0.72F, 0.55F, 0.18F), 0.7F);
    private static final DustParticleOptions DARK_STAIN =
            new DustParticleOptions(new Vector3f(0.06F, 0.07F, 0.08F), 0.85F);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(
                    TalismanWardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(
                    TalismanWardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(
                    TalismanWardEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);

    private int ageTicks;

    public TalismanWardEntity(
            EntityType<? extends TalismanWardEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static void spawnSuppression(
            ServerLevel level, ServerPlayer owner) {
        TalismanWardEntity ward = create(
                level, Kind.SUPPRESSION, owner, 25 * 20);
        if (ward == null) return;
        ward.setPos(owner.getX(), owner.getY() + 0.02D, owner.getZ());
        level.addFreshEntity(ward);
    }

    public static void spawnMountains(
            ServerLevel level, ServerPlayer owner, int lifetime) {
        TalismanWardEntity ward = create(
                level, Kind.FIVE_MOUNTAINS, owner, lifetime);
        if (ward == null) return;
        ward.setPos(owner.getX(), owner.getY() + 0.02D, owner.getZ());
        level.addFreshEntity(ward);
    }

    public static void spawnLightningStain(
            ServerLevel level, net.minecraft.world.phys.Vec3 position) {
        TalismanWardEntity ward = create(
                level, Kind.LIGHTNING_STAIN, null, 5 * 20);
        if (ward == null) return;
        ward.setPos(position.x, position.y + 0.025D, position.z);
        level.addFreshEntity(ward);
    }

    public static void spawnForbiddenAir(
            ServerLevel level, Player owner, Vec3 position) {
        TalismanWardEntity ward = create(
                level, Kind.FORBIDDEN_AIR,
                owner instanceof ServerPlayer serverPlayer ? serverPlayer : null,
                6 + 8 * 20);
        if (ward == null) return;
        ward.setPos(position.x, position.y, position.z);
        level.addFreshEntity(ward);
    }

    @Nullable
    private static TalismanWardEntity create(
            ServerLevel level, Kind kind,
            @Nullable ServerPlayer owner, int lifetime) {
        TalismanWardEntity ward =
                ModEntities.TALISMAN_WARD.get().create(level);
        if (ward == null) return null;
        ward.entityData.set(DATA_KIND, kind.ordinal());
        ward.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
        ward.entityData.set(DATA_OWNER,
                owner == null ? Optional.empty()
                        : Optional.of(owner.getUUID()));
        return ward;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.SUPPRESSION.ordinal());
        builder.define(DATA_LIFETIME, 20);
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        ageTicks++;
        if (level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level();
        if (ageTicks >= lifetime()) {
            discard();
            return;
        }

        if (kind() == Kind.FIVE_MOUNTAINS) {
            ServerPlayer owner = owner(serverLevel);
            if (owner == null || !owner.isAlive()) {
                discard();
                return;
            }
            setPos(owner.getX(), owner.getY() + 0.02D, owner.getZ());
            return;
        }
        if (kind() == Kind.SUPPRESSION) {
            tickSuppression(serverLevel);
        } else if (kind() == Kind.FORBIDDEN_AIR) {
            tickForbiddenAir(serverLevel);
        } else if (tickCount % 5 == 0) {
            serverLevel.sendParticles(DARK_STAIN,
                    getX(), getY() + 0.035D, getZ(),
                    3, 0.8D, 0.015D, 0.8D, 0.0D);
        }
    }

    private void tickForbiddenAir(ServerLevel level) {
        if (ageTicks < 6) {
            emitForbiddenActivation(level);
            return;
        }
        AABB area = new AABB(
                getX() - FORBIDDEN_AIR_RADIUS,
                getY() - FORBIDDEN_AIR_BELOW,
                getZ() - FORBIDDEN_AIR_RADIUS,
                getX() + FORBIDDEN_AIR_RADIUS,
                getY() + FORBIDDEN_AIR_HEIGHT,
                getZ() + FORBIDDEN_AIR_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class, area,
                target -> target.isAlive()
                        && horizontalDistanceSqr(target) <= FORBIDDEN_AIR_RADIUS_SQR)) {
            double altitude = Math.max(0.0D, living.getY() - getY());
            double resistance = realmResistance(level, living);
            Vec3 movement = living.getDeltaMovement();
            if (altitude < 1.15D && movement.y > 0.0D) {
                movement = new Vec3(movement.x, movement.y * 0.60D, movement.z);
            }
            if (!living.onGround() || altitude > 1.15D) {
                double downward = altitude <= 3.0D ? 0.025D
                        : altitude <= 7.0D ? 0.065D : 0.12D;
                double horizontal = altitude <= 3.0D ? 0.95D
                        : altitude <= 7.0D ? 0.90D : 0.84D;
                movement = new Vec3(
                        movement.x * Mth.lerp(resistance, 1.0D, horizontal),
                        movement.y - downward * resistance,
                        movement.z * Mth.lerp(resistance, 1.0D, horizontal));
            }
            living.setDeltaMovement(movement);
            living.hurtMarked = true;
            if (tickCount % 4 == 0 && altitude > 0.6D) {
                emitDescendingPressure(level, living, altitude);
            }
        }
        for (FlyingSwordEntity sword : level.getEntitiesOfClass(
                FlyingSwordEntity.class, area,
                sword -> horizontalDistanceSqr(sword) <= FORBIDDEN_AIR_RADIUS_SQR)) {
            Vec3 movement = sword.getDeltaMovement();
            sword.setDeltaMovement(
                    movement.x * 0.86D,
                    movement.y * 0.75D - 0.045D,
                    movement.z * 0.86D);
            sword.hurtMarked = true;
        }
        if (tickCount % 4 == 0) emitForbiddenBoundary(level);
    }

    private double horizontalDistanceSqr(Entity entity) {
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    private double realmResistance(ServerLevel level, LivingEntity target) {
        int targetRealm = realm(target);
        int ownerRealm = entityData.get(DATA_OWNER)
                .map(level.getServer().getPlayerList()::getPlayer)
                .map(TalismanWardEntity::realm)
                .orElse(0);
        int advantage = targetRealm - ownerRealm;
        return advantage >= 2 ? 0.30D : advantage == 1 ? 0.60D : 1.0D;
    }

    private static int realm(LivingEntity target) {
        if (target instanceof Player player) {
            return CultivationLevels.getRealmIndex(
                    ModAttachments.getData(player).cultivationLevel());
        }
        if (target instanceof CultivationNpc npc) {
            return CultivationLevels.getRealmIndex(npc.npcRealm());
        }
        return 0;
    }

    private void emitForbiddenActivation(ServerLevel level) {
        double progress = ageTicks / 6.0D;
        int points = 72;
        for (int i = 0; i < points; i++) {
            double angle = Mth.TWO_PI * i / points;
            double radius = FORBIDDEN_AIR_RADIUS * progress;
            level.sendParticles(DIM_GOLD,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.04D,
                    getZ() + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (ageTicks == 5) {
            level.sendParticles(ParticleTypes.POOF,
                    getX(), getY() + 0.08D, getZ(),
                    72, FORBIDDEN_AIR_RADIUS * 0.50D, 0.18D,
                    FORBIDDEN_AIR_RADIUS * 0.50D, 0.12D);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                    getX(), getY() + 0.14D, getZ(),
                    56, FORBIDDEN_AIR_RADIUS * 0.50D, 0.10D,
                    FORBIDDEN_AIR_RADIUS * 0.50D, 0.15D);
        }
    }

    private void emitForbiddenBoundary(ServerLevel level) {
        int boundaryPoints = 96;
        for (int i = 0; i < boundaryPoints; i++) {
            double angle = Mth.TWO_PI * i / boundaryPoints;
            level.sendParticles(DIM_GOLD,
                    getX() + Math.cos(angle) * FORBIDDEN_AIR_RADIUS,
                    getY() + 0.035D,
                    getZ() + Math.sin(angle) * FORBIDDEN_AIR_RADIUS,
                    1, 0.006D, 0.002D, 0.006D, 0.0D);
        }
        for (int line = 0; line < 4; line++) {
            double angle = line * Mth.HALF_PI + tickCount * 0.006D;
            for (double step = 3.0D;
                 step < FORBIDDEN_AIR_RADIUS;
                 step += 2.5D) {
                level.sendParticles(DIM_GOLD,
                        getX() + Math.cos(angle) * step,
                        getY() + 0.04D,
                        getZ() + Math.sin(angle) * step,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void emitDescendingPressure(
            ServerLevel level, LivingEntity target, double altitude) {
        int rings = altitude > 7.0D ? 3 : altitude > 3.0D ? 2 : 1;
        for (int ring = 0; ring < rings; ring++) {
            double y = target.getY() + target.getBbHeight()
                    + 0.65D + ring * 0.75D;
            double radius = Math.max(0.65D, target.getBbWidth() * 0.85D)
                    + ring * 0.22D;
            for (int i = 0; i < 18; i++) {
                double angle = Mth.TWO_PI * i / 18.0D;
                level.sendParticles(DIM_GOLD,
                        target.getX() + Math.cos(angle) * radius,
                        y, target.getZ() + Math.sin(angle) * radius,
                        1, 0.0D, -0.018D, 0.0D, 0.0D);
            }
        }
        level.sendParticles(DIM_GOLD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(), 3, 0.08D, 0.40D, 0.08D, 0.01D);
    }

    public static boolean isInsideForbiddenAir(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        AABB search = entity.getBoundingBox().inflate(
                FORBIDDEN_AIR_RADIUS + 0.5D,
                FORBIDDEN_AIR_HEIGHT + FORBIDDEN_AIR_BELOW,
                FORBIDDEN_AIR_RADIUS + 0.5D);
        return !entity.level().getEntitiesOfClass(
                TalismanWardEntity.class, search,
                ward -> ward.kind() == Kind.FORBIDDEN_AIR
                        && ward.ageTicks >= 6
                        && entity.getY() >= ward.getY() - FORBIDDEN_AIR_BELOW
                        && entity.getY() <= ward.getY() + FORBIDDEN_AIR_HEIGHT
                        && ward.horizontalDistanceSqr(entity)
                        <= FORBIDDEN_AIR_RADIUS_SQR)
                .isEmpty();
    }

    public static int adjustFlightQiCost(Entity entity, int cost) {
        return isInsideForbiddenAir(entity)
                ? Mth.ceil(Math.max(0, cost) * 1.75D)
                : Math.max(0, cost);
    }

    private void tickSuppression(ServerLevel level) {
        if (tickCount % 5 == 0) {
            AABB area = getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
            for (LivingEntity living : level.getEntitiesOfClass(
                    LivingEntity.class, area,
                    target -> target.isAlive()
                            && target instanceof Enemy)) {
                living.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 14, 1,
                        false, false, true));
                living.setDeltaMovement(living.getDeltaMovement()
                        .multiply(0.86D, 0.82D, 0.86D));
            }
        }
        if (tickCount % 3 == 0) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = 5.7D * Math.sqrt(random.nextDouble());
            level.sendParticles(DIM_GOLD,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.05D + random.nextDouble() * 1.8D,
                    getZ() + Math.sin(angle) * radius,
                    1, 0.0D, -0.035D, 0.0D, 0.0D);
        }
    }

    @Nullable
    private ServerPlayer owner(ServerLevel level) {
        return entityData.get(DATA_OWNER)
                .map(level.getServer().getPlayerList()::getPlayer)
                .orElse(null);
    }

    public Kind kind() {
        return Kind.byId(entityData.get(DATA_KIND));
    }

    public int lifetime() {
        return entityData.get(DATA_LIFETIME);
    }

    public float ageProgress(float partialTick) {
        return Mth.clamp((ageTicks + partialTick)
                / Math.max(1.0F, lifetime()), 0.0F, 1.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_KIND, tag.getInt("Kind"));
        entityData.set(DATA_LIFETIME,
                Math.max(1, tag.getInt("Lifetime")));
        entityData.set(DATA_OWNER,
                tag.hasUUID("Owner")
                        ? Optional.of(tag.getUUID("Owner"))
                        : Optional.empty());
        ageTicks = Math.max(0, tag.getInt("Age"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind().ordinal());
        tag.putInt("Lifetime", lifetime());
        entityData.get(DATA_OWNER)
                .ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt("Age", ageTicks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public enum Kind {
        SUPPRESSION,
        FIVE_MOUNTAINS,
        LIGHTNING_STAIN,
        FORBIDDEN_AIR;

        public static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }
}
