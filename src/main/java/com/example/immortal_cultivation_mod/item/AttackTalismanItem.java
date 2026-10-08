package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity.AttackType;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared real projectile logic for offensive talismans. Every variant uses the
 * fireball talisman's curved launch and renderer trail, while its element
 * controls color, shot count, damage, and impact behavior.
 */
public final class AttackTalismanItem extends TalismanItem {
    private static final double TOP_ARC_SPAWN_RADIUS = 1.25D;
    private static final double OUTWARD_CURVE_BLOCKS = 8.0D;
    private static final int RANDOM_LAUNCH_DELAY_TICKS = 10;
    private static final int COOLDOWN_TICKS = 8;

    private final AttackType attackType;

    public AttackTalismanItem(
            Properties properties, AttackType attackType, Tier tier) {
        super(properties, tier);
        this.attackType = attackType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            launchVolley(level, player);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Fires this exact registered talisman attack for an NPC. This deliberately
     * shares the player projectile, damage, trail, impact, and targeting data
     * instead of creating a visually similar NPC-only attack.
     */
    public boolean launchForNpc(PathfinderMob user, LivingEntity targetEntity) {
        if (user.level().isClientSide
                || !user.isAlive()
                || !targetEntity.isAlive()
                || user.level() != targetEntity.level()
                || !user.hasLineOfSight(targetEntity)
                || user.distanceToSqr(targetEntity) > range() * range()) {
            return false;
        }
        Vec3 target = targetEntity.position().add(
                0.0D, targetEntity.getBbHeight() * 0.55D, 0.0D);
        Vec3 look = target.subtract(user.getEyePosition());
        if (look.lengthSqr() < 0.001D) {
            return false;
        }
        return launchVolley(user.level(), user, look.normalize(), targetEntity, target);
    }

    private void launchVolley(Level level, Player player) {
        double range = range();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        HitResult hit = player.pick(range, 0.0F, false);
        Vec3 blockTarget = hit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(range)) : hit.getLocation();
        EntityHitResult entityHit = pickEntity(player, eye, blockTarget, range);
        LivingEntity targetEntity = entityHit == null
                ? null : ModelBonePartEntity.resolveLiving(entityHit.getEntity());
        Vec3 target = targetEntity == null
                ? blockTarget
                : targetEntity.position().add(
                        0.0D, targetEntity.getBbHeight() * 0.55D, 0.0D);

        launchVolley(level, player, look, targetEntity, target);
    }

    private boolean launchVolley(
            Level level,
            LivingEntity shooter,
            Vec3 look,
            @Nullable LivingEntity targetEntity,
            Vec3 target) {
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        if (flatLook.lengthSqr() < 0.001D) {
            flatLook = Vec3.directionFromRotation(0.0F, shooter.getYRot());
        } else {
            flatLook = flatLook.normalize();
        }
        Vec3 origin = shooter.position()
                .add(0.0D, shooter.getBbHeight() * 0.65D, 0.0D)
                .add(flatLook.scale(1.5D));
        if (attackType != AttackType.BLAST_WIND
                && attackType != AttackType.SPIRIT_SEAL
                && targetEntity == null
                && target.distanceToSqr(origin) < 16.0D) {
            target = origin.add(look.scale(16.0D));
        }

        Vec3 forward = target.subtract(origin);
        if (forward.lengthSqr() < 0.001D) {
            return false;
        }
        forward = forward.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 0.001D
                ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        int count = projectileCount();
        for (int index = 0; index < count; index++) {
            double arcAngle = count == 1
                    ? Math.PI * 0.5D
                    : Math.PI * index / (count - 1.0D);
            Vec3 arcDirection = right.scale(Math.cos(arcAngle))
                    .add(0.0D, Math.sin(arcAngle), 0.0D)
                    .normalize();
            Vec3 outward = arcDirection.scale(OUTWARD_CURVE_BLOCKS);
            Vec3 projectileOrigin = origin.add(
                    arcDirection.scale(TOP_ARC_SPAWN_RADIUS));
            int launchDelay = attackType == AttackType.SPIRIT_SEAL
                    || attackType == AttackType.BLAST_WIND
                    ? 0 : level.random.nextInt(RANDOM_LAUNCH_DELAY_TICKS + 1);
            FireballTalismanProjectileEntity projectile =
                    new FireballTalismanProjectileEntity(level, shooter);
            projectile.setPos(projectileOrigin.x,
                    projectileOrigin.y, projectileOrigin.z);
            projectile.configure(target, outward, speed(), launchDelay,
                    targetEntity, attackType);
            projectile.setDeltaMovement(outward.normalize().scale(speed()));
            level.addFreshEntity(projectile);
        }
        return true;
    }

    private EntityHitResult pickEntity(
            Player player, Vec3 eye, Vec3 blockTarget, double range) {
        double maxDistance = Math.min(range, eye.distanceTo(blockTarget));
        Vec3 end = eye.add(player.getLookAngle().normalize().scale(maxDistance));
        AABB search = player.getBoundingBox()
                .expandTowards(end.subtract(eye)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end, search,
                entity -> ModelBonePartEntity.resolveLiving(entity) != null
                        && entity.isPickable()
                        && ModelBonePartEntity.resolveParent(entity) != player
                        && !entity.isSpectator());
    }

    private int projectileCount() {
        return switch (attackType) {
            case FIREBALL -> 8;
            case SPARK -> 5;
            case WIND_BLADE -> 3;
            case BINDING_SILK -> 6;
            default -> 1;
        };
    }

    private double range() {
        return switch (attackType) {
            case FIREBALL -> 64.0D;
            case SPARK -> 16.0D;
            case WIND_BLADE -> 15.0D;
            case BINDING_SILK -> 18.0D;
            case FIVE_THUNDER -> 24.0D;
            case FIRE_PRISON -> 24.0D;
            case BARRIER_BREAKER -> 20.0D;
            case BLAST_WIND -> 14.0D;
            case SPIRIT_SEAL -> 18.0D;
            case BLACK_FLAME -> 30.0D;
        };
    }

    private double speed() {
        return switch (attackType) {
            case WIND_BLADE -> 2.35D;
            case FIVE_THUNDER -> 2.65D;
            case BINDING_SILK -> 1.65D;
            case SPIRIT_SEAL -> 2.40D;
            case BLACK_FLAME -> 1.85D;
            default -> 2.0D;
        };
    }
}
