package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.item.ShihunfanItem;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ShihunfanSoulWaveEntity extends Entity implements ForcefieldInterceptable {
    private static final int LIFETIME_TICKS = 24;
    private static final double SPEED = 1.0D;
    private static final double HALF_WIDTH = 6.0D;
    private static final double HALF_HEIGHT = 2.5D;
    private static final DustParticleOptions BLACK_MIST =
            new DustParticleOptions(new Vector3f(0.025F, 0.02F, 0.035F), 1.8F);
    private static final DustParticleOptions CURSED_PURPLE =
            new DustParticleOptions(new Vector3f(0.18F, 0.025F, 0.28F), 1.45F);
    private static final DustParticleOptions GHOST_CYAN =
            new DustParticleOptions(new Vector3f(0.12F, 0.45F, 0.48F), 1.1F);
    private static final DustParticleOptions GHOST_GREEN =
            new DustParticleOptions(new Vector3f(0.38F, 0.68F, 0.47F), 0.95F);

    private final Set<UUID> hitEntities = new HashSet<>();
    private final Set<BlockPos> brokenBlocks = new HashSet<>();
    private net.minecraft.world.item.ItemStack bindingWeapon = net.minecraft.world.item.ItemStack.EMPTY;
    private UUID ownerUuid;
    private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
    private double speed = SPEED;
    private float damage = 1.0F;
    private float soulDensity;

    public ShihunfanSoulWaveEntity(EntityType<? extends ShihunfanSoulWaveEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    public Entity forcefieldOwner() {
        return ownerUuid != null && level() instanceof ServerLevel serverLevel
                ? serverLevel.getServer().getPlayerList().getPlayer(ownerUuid)
                : null;
    }

    @Override
    public Vec3 forcefieldMovement() {
        return direction.scale(speed);
    }

    @Override
    public float forcefieldBarrierDamage() {
        return damage;
    }

    @Override
    public void forcefieldBounce(Vec3 reflectedMovement) {
        Vec3 horizontal = new Vec3(reflectedMovement.x, 0.0D, reflectedMovement.z);
        if (horizontal.lengthSqr() < 1.0E-9D) {
            horizontal = direction.scale(-1.0D);
        }
        direction = horizontal.normalize();
        speed = Math.max(0.01D, reflectedMovement.length());
        setDeltaMovement(direction.scale(speed));
        hurtMarked = true;
        hasImpulse = true;
    }

    @Override
    public double forcefieldSupport(Vec3 normal) {
        Vec3 right = new Vec3(-direction.z, 0.0D, direction.x).normalize();
        return Math.abs(normal.dot(direction)) * 1.15D
                + Math.abs(normal.dot(right)) * HALF_WIDTH
                + Math.abs(normal.y) * HALF_HEIGHT;
    }

    @Override
    public double forcefieldMaximumSupport() {
        return Math.sqrt(1.15D * 1.15D + HALF_WIDTH * HALF_WIDTH
                + HALF_HEIGHT * HALF_HEIGHT);
    }

    public void setBindingWeapon(net.minecraft.world.item.ItemStack stack) {
        com.example.immortal_cultivation_mod.item.WeaponBinding.prepare(stack);
        bindingWeapon = stack.copy();
    }
    @Override public net.minecraft.world.item.ItemStack getWeaponItem() { return bindingWeapon; }

    public static ShihunfanSoulWaveEntity create(
            ServerLevel level, ServerPlayer owner, Vec3 direction, float damage, float soulDensity) {
        ShihunfanSoulWaveEntity wave = ModEntities.SHIHUNFAN_SOUL_WAVE.get().create(level);
        if (wave == null) {
            return null;
        }
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        wave.ownerUuid = owner.getUUID();
        wave.direction = horizontal.normalize();
        wave.damage = Math.max(1.0F, damage);
        wave.soulDensity = Math.max(0.0F, Math.min(1.0F, soulDensity));
        Vec3 start = owner.position().add(0.0D, 1.15D, 0.0D).add(wave.direction.scale(1.4D));
        wave.moveTo(start.x, start.y, start.z, owner.getYRot(), 0.0F);
        return wave;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
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
        ServerPlayer owner = ownerUuid == null ? null : serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null || !owner.isAlive() || !com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(bindingWeapon, owner) || tickCount > LIFETIME_TICKS) {
            discard();
            return;
        }

        setPos(position().add(direction.scale(speed)));
        renderWave(serverLevel);
        hitTargets(serverLevel, owner);
        breakWaveFront(serverLevel, owner);
    }

    private void hitTargets(ServerLevel level, ServerPlayer owner) {
        AABB search = getBoundingBox().inflate(HALF_WIDTH + 1.0D, HALF_HEIGHT + 1.0D, HALF_WIDTH + 1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> isEnemy(owner, entity) && !hitEntities.contains(entity.getUUID()))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(position());
            double forwardDistance = offset.dot(direction);
            Vec3 lateral = offset.subtract(direction.scale(forwardDistance));
            if (Math.abs(forwardDistance) > 1.15D
                    || Math.abs(offset.y) > HALF_HEIGHT
                    || Math.sqrt(lateral.x * lateral.x + lateral.z * lateral.z) > HALF_WIDTH) {
                continue;
            }

            hitEntities.add(target.getUUID());
            boolean alreadyExecutable = target.getHealth() <= target.getMaxHealth() * 0.10F;
            if (alreadyExecutable) {
                executeAndCapture(owner, target);
            } else {
                damageAndCaptureIfKilled(owner, target, damage);
            }
            if (target.isAlive()) {
                target.push(direction.x * 1.8D, 0.34D, direction.z * 1.8D);
                target.hasImpulse = true;
            }
            ShihunfanItem.renderSoulImpact(level, target.getBoundingBox().getCenter(), soulDensity, true);
            releaseSecondarySoul(level, owner, target);

            if (!alreadyExecutable && target.isAlive() && target.getHealth() <= target.getMaxHealth() * 0.10F) {
                executeAndCapture(owner, target);
            }
        }
    }

    private void releaseSecondarySoul(ServerLevel level, ServerPlayer owner, LivingEntity source) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                        LivingEntity.class, source.getBoundingBox().inflate(8.0D),
                        target -> target != source && isEnemy(owner, target))
                .stream()
                .sorted(Comparator.comparingDouble(source::distanceToSqr))
                .toList();
        if (nearby.isEmpty()) {
            return;
        }
        LivingEntity secondaryTarget = nearby.getFirst();
        Vec3 start = source.getBoundingBox().getCenter();
        Vec3 end = secondaryTarget.getBoundingBox().getCenter();
        ShihunfanItem.renderSoulTrail(level, start, end, soulDensity);
        damageAndCaptureIfKilled(owner, secondaryTarget, damage * 0.35F);
        ShihunfanItem.renderSoulImpact(level, end, soulDensity, false);
    }

    private void executeAndCapture(ServerPlayer owner, LivingEntity target) {
        damageAndCaptureIfKilled(owner, target, Float.MAX_VALUE);
    }

    private boolean damageAndCaptureIfKilled(ServerPlayer owner, LivingEntity target, float amount) {
        double capturedSouls = SoulEntity.valueFor(target);
        target.getPersistentData().putBoolean(ShihunfanItem.CAPTURED_SOUL_TAG, true);
        boolean damaged = target.hurt(owner.damageSources().indirectMagic(this, owner), amount);
        if (target.isAlive()) {
            target.getPersistentData().remove(ShihunfanItem.CAPTURED_SOUL_TAG);
            return false;
        }
        ShihunfanItem.renderSoulTrail(
                owner.serverLevel(), target.getBoundingBox().getCenter(),
                owner.getBoundingBox().getCenter(), soulDensity);
        ShihunfanItem.storeCapturedSouls(owner, capturedSouls);
        return damaged;
    }

    private void breakWaveFront(ServerLevel level, ServerPlayer owner) {
        if (!BlockBreakingSettings.enabled(owner,
                BlockBreakingSettings.Category.WEAPONS)) return;
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        for (int lateral = -6; lateral <= 6; lateral++) {
            for (int vertical = -2; vertical <= 2; vertical++) {
                if (Math.abs(lateral) / HALF_WIDTH + Math.abs(vertical) / 3.5D > 1.45D) {
                    continue;
                }
                Vec3 sample = position().add(side.scale(lateral)).add(0.0D, vertical, 0.0D);
                BlockPos pos = BlockPos.containing(sample);
                if (!brokenBlocks.add(pos.immutable())) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                    continue;
                }
                BlockBreakingSettings.runWithSource(
                        BlockBreakingSettings.Category.WEAPONS, owner,
                        () -> com.example.immortal_cultivation_mod.spell.SpellDestruction.breakBlock(level,pos, false, owner));
            }
        }
    }

    private void renderWave(ServerLevel level) {
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        int layers = 2 + Math.round(soulDensity * 3.0F);
        for (int lateral = -6; lateral <= 6; lateral++) {
            double curve = 1.0D - Math.abs(lateral) / HALF_WIDTH;
            for (int layer = 0; layer < layers; layer++) {
                double y = -1.5D + layer * (3.0D / Math.max(1, layers - 1));
                Vec3 point = position().add(side.scale(lateral)).add(0.0D, y + curve * 0.65D, 0.0D);
                level.sendParticles(layer % 2 == 0 ? BLACK_MIST : CURSED_PURPLE,
                        point.x, point.y, point.z, 2, 0.28D, 0.22D, 0.28D, 0.015D);
                level.sendParticles(layer % 3 == 0 ? GHOST_CYAN : GHOST_GREEN,
                        point.x, point.y, point.z, 1, 0.18D, 0.18D, 0.18D, 0.01D);
                if ((lateral + layer + tickCount) % 3 == 0) {
                    level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                            1 + Math.round(soulDensity * 2.0F), 0.22D, 0.22D, 0.22D, 0.02D);
                }
                if ((lateral + layer + tickCount) % 4 == 0) {
                    level.sendParticles(ParticleTypes.ASH, point.x, point.y, point.z,
                            2, 0.34D, 0.26D, 0.34D, 0.02D);
                }
            }
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(),
                10 + Math.round(soulDensity * 18.0F), HALF_WIDTH * 0.75D, 1.6D, 0.35D, 0.05D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(),
                4 + Math.round(soulDensity * 10.0F), HALF_WIDTH * 0.7D, 1.4D, 0.35D, 0.025D);
    }

    private static boolean isEnemy(ServerPlayer owner, LivingEntity target) {
        return target.isAlive() && target != owner && !owner.isAlliedTo(target) && owner.canAttack(target);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        bindingWeapon = net.minecraft.world.item.ItemStack.parseOptional(registryAccess(), tag.getCompound("BindingWeapon"));
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        direction = new Vec3(
                tag.getDouble("DirectionX"),
                tag.contains("DirectionY") ? tag.getDouble("DirectionY") : 0.0D,
                tag.getDouble("DirectionZ"));
        if (direction.lengthSqr() < 1.0E-5D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            direction = direction.normalize();
        }
        speed = tag.contains("Speed")
                ? Math.max(0.05D, tag.getDouble("Speed"))
                : SPEED;
        damage = Math.max(1.0F, tag.getFloat("Damage"));
        soulDensity = Math.max(0.0F, Math.min(1.0F, tag.getFloat("SoulDensity")));
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!bindingWeapon.isEmpty()) tag.put("BindingWeapon", bindingWeapon.saveOptional(registryAccess()));
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        tag.putDouble("Speed", speed);
        tag.putFloat("Damage", damage);
        tag.putFloat("SoulDensity", soulDensity);
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
