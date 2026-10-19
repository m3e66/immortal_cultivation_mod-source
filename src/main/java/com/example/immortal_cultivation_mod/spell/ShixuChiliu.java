package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShixuChiliu {
    public static final double DOMAIN_RADIUS = 30.0D;
    private static final double RADIUS_SQUARED = DOMAIN_RADIUS * DOMAIN_RADIUS;
    private static final int QI_PER_SECOND = 500;
    private static final int EFFECT_REFRESH_TICKS = 40;
    private static final double CASTER_TIME_MULTIPLIER = 2.0D;
    private static final double ENEMY_TIME_MULTIPLIER = -0.95D;
    private static final ResourceLocation CASTER_MOVEMENT_MODIFIER = id("shixu_chiliu_caster_movement");
    private static final ResourceLocation CASTER_FLYING_MODIFIER = id("shixu_chiliu_caster_flying");
    private static final ResourceLocation CASTER_ATTACK_MODIFIER = id("shixu_chiliu_caster_attack");
    private static final ResourceLocation ENEMY_MOVEMENT_MODIFIER = id("shixu_chiliu_enemy_movement");
    private static final ResourceLocation ENEMY_FLYING_MODIFIER = id("shixu_chiliu_enemy_flying");
    private static final ResourceLocation ENEMY_ATTACK_MODIFIER = id("shixu_chiliu_enemy_attack");

    private static final Map<UUID, Boolean> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> TIMED_UNTIL = new HashMap<>();
    private static final Map<UUID, SlowedTarget> SLOWED_TARGETS = new HashMap<>();
    private static final Map<UUID, FrozenProjectile> FROZEN_PROJECTILES = new HashMap<>();

    private ShixuChiliu() {
    }

    public static boolean toggle(ServerPlayer player) {
        UUID casterId = player.getUUID();
        if (ACTIVE.remove(casterId) != null) {
            stop(player);
            return true;
        }

        var data = ModAttachments.getData(player);
        if (!meetsRequirement(data.cultivationLevel())) {
            return false;
        }
        if (!ServerEvents.spendSpellQiOrBlood(player, data, QI_PER_SECOND)) {
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }

        TIMED_UNTIL.remove(casterId);
        ACTIVE.put(casterId, true);
        refreshCaster(player);
        applyDomain(player);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        Long timedUntil = TIMED_UNTIL.get(player.getUUID());
        if (!player.isAlive() || (timedUntil != null && player.level().getGameTime() >= timedUntil)) {
            stop(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (!meetsRequirement(data.cultivationLevel())) {
            stop(player);
            return;
        }

        refreshCaster(player);
        applyDomain(player);
        if (timedUntil != null || player.tickCount % 20 != 0) {
            return;
        }
        if (!ServerEvents.spendSustainedSpellQiOrBlood(player, data, QI_PER_SECOND, ModSpells.SHIXU_CHILIU)) {
            stop(player);
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }
        ServerEvents.syncPlayerData(player);
    }

    public static void activateTimed(ServerPlayer player, int durationTicks) {
        var data = ModAttachments.getData(player);
        if (!meetsRequirement(data.cultivationLevel())) {
            return;
        }

        UUID casterId = player.getUUID();
        ACTIVE.put(casterId, true);
        long now = player.level().getGameTime();
        long current = TIMED_UNTIL.getOrDefault(casterId, now);
        TIMED_UNTIL.put(casterId, Math.max(now, current) + Math.max(1, durationTicks));
        refreshCaster(player);
        applyDomain(player);
        ServerEvents.syncPlayerData(player);
    }

    public static void clear(ServerPlayer player) {
        UUID casterId = player.getUUID();
        ACTIVE.remove(casterId);
        TIMED_UNTIL.remove(casterId);
        player.removeEffect(ModEffects.SHIXU_CHILIU);
        removeCasterModifiers(player);
        releaseCasterTargets(casterId, Set.of(), Set.of());
    }

    public static void holdIfFrozen(Projectile projectile) {
        FrozenProjectile frozen = FROZEN_PROJECTILES.get(projectile.getUUID());
        if (frozen == null || frozen.controllers.isEmpty() || projectile.isRemoved()) {
            return;
        }
        projectile.setPos(frozen.anchor.x, frozen.anchor.y, frozen.anchor.z);
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.setNoGravity(true);
    }

    private static void stop(ServerPlayer player) {
        clear(player);
        ServerEvents.syncPlayerData(player);
    }

    private static void refreshCaster(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                ModEffects.SHIXU_CHILIU, EFFECT_REFRESH_TICKS, 0, false, false, true));
        addModifier(player, Attributes.MOVEMENT_SPEED, CASTER_MOVEMENT_MODIFIER, CASTER_TIME_MULTIPLIER);
        addModifier(player, Attributes.FLYING_SPEED, CASTER_FLYING_MODIFIER, CASTER_TIME_MULTIPLIER);
        addModifier(player, Attributes.ATTACK_SPEED, CASTER_ATTACK_MODIFIER, CASTER_TIME_MULTIPLIER);
    }

    private static void applyDomain(ServerPlayer caster) {
        UUID casterId = caster.getUUID();
        AABB bounds = caster.getBoundingBox().inflate(DOMAIN_RADIUS);
        Set<UUID> currentTargets = new HashSet<>();
        Set<UUID> currentProjectiles = new HashSet<>();

        for (LivingEntity target : caster.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                target -> target.isAlive()
                        && target.distanceToSqr(caster) <= RADIUS_SQUARED
                        && isEnemy(caster, target))) {
            currentTargets.add(target.getUUID());
            slowTarget(casterId, target);
        }

        for (Projectile projectile : caster.serverLevel().getEntitiesOfClass(
                Projectile.class,
                bounds,
                projectile -> !projectile.isRemoved()
                        && projectile.distanceToSqr(caster) <= RADIUS_SQUARED
                        && isEnemyProjectile(caster, projectile))) {
            currentProjectiles.add(projectile.getUUID());
            freezeProjectile(casterId, projectile);
        }

        releaseCasterTargets(casterId, currentTargets, currentProjectiles);
    }

    private static void slowTarget(UUID casterId, LivingEntity target) {
        SlowedTarget slowed = SLOWED_TARGETS.computeIfAbsent(
                target.getUUID(), ignored -> new SlowedTarget(target));
        slowed.controllers.add(casterId);
        addModifier(target, Attributes.MOVEMENT_SPEED, ENEMY_MOVEMENT_MODIFIER, ENEMY_TIME_MULTIPLIER);
        addModifier(target, Attributes.FLYING_SPEED, ENEMY_FLYING_MODIFIER, ENEMY_TIME_MULTIPLIER);
        addModifier(target, Attributes.ATTACK_SPEED, ENEMY_ATTACK_MODIFIER, ENEMY_TIME_MULTIPLIER);
    }

    private static void freezeProjectile(UUID casterId, Projectile projectile) {
        FrozenProjectile frozen = FROZEN_PROJECTILES.computeIfAbsent(
                projectile.getUUID(),
                ignored -> new FrozenProjectile(
                        projectile,
                        projectile.position(),
                        projectile.getDeltaMovement(),
                        projectile.isNoGravity()));
        frozen.controllers.add(casterId);
        holdIfFrozen(projectile);
    }

    private static void releaseCasterTargets(UUID casterId, Set<UUID> currentTargets,
                                             Set<UUID> currentProjectiles) {
        Iterator<Map.Entry<UUID, SlowedTarget>> targetIterator = SLOWED_TARGETS.entrySet().iterator();
        while (targetIterator.hasNext()) {
            Map.Entry<UUID, SlowedTarget> entry = targetIterator.next();
            SlowedTarget slowed = entry.getValue();
            if (slowed.target.isRemoved()) {
                targetIterator.remove();
                continue;
            }
            if (!currentTargets.contains(entry.getKey())) {
                slowed.controllers.remove(casterId);
            }
            if (slowed.controllers.isEmpty()) {
                removeEnemyModifiers(slowed.target);
                targetIterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, FrozenProjectile>> projectileIterator =
                FROZEN_PROJECTILES.entrySet().iterator();
        while (projectileIterator.hasNext()) {
            Map.Entry<UUID, FrozenProjectile> entry = projectileIterator.next();
            FrozenProjectile frozen = entry.getValue();
            if (frozen.projectile.isRemoved()) {
                projectileIterator.remove();
                continue;
            }
            if (!currentProjectiles.contains(entry.getKey())) {
                frozen.controllers.remove(casterId);
            }
            if (frozen.controllers.isEmpty()) {
                restoreProjectile(frozen);
                projectileIterator.remove();
            }
        }
    }

    private static boolean isEnemy(ServerPlayer caster, LivingEntity target) {
        if (target == caster || caster.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable
                && caster.getUUID().equals(tamable.getOwnerUUID())) {
            return false;
        }
        if (UndeadControl.isOwnedByPlayer(target, caster)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isSpectator();
        }
        if (target instanceof Enemy) {
            return true;
        }
        if (target instanceof Mob mob && mob.getTarget() == caster) {
            return true;
        }
        return target.getLastHurtByMob() == caster || caster.getLastHurtMob() == target;
    }

    private static boolean isEnemyProjectile(ServerPlayer caster, Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (owner == caster || (owner != null && caster.isAlliedTo(owner))) {
            return false;
        }
        if (owner instanceof LivingEntity livingOwner) {
            return isEnemy(caster, livingOwner);
        }
        return owner == null;
    }

    private static void restoreProjectile(FrozenProjectile frozen) {
        Projectile projectile = frozen.projectile;
        projectile.setPos(frozen.anchor.x, frozen.anchor.y, frozen.anchor.z);
        projectile.setNoGravity(frozen.noGravity);
        projectile.setDeltaMovement(frozen.velocity);
    }

    private static void removeCasterModifiers(LivingEntity entity) {
        removeModifier(entity, Attributes.MOVEMENT_SPEED, CASTER_MOVEMENT_MODIFIER);
        removeModifier(entity, Attributes.FLYING_SPEED, CASTER_FLYING_MODIFIER);
        removeModifier(entity, Attributes.ATTACK_SPEED, CASTER_ATTACK_MODIFIER);
    }

    private static void removeEnemyModifiers(LivingEntity entity) {
        removeModifier(entity, Attributes.MOVEMENT_SPEED, ENEMY_MOVEMENT_MODIFIER);
        removeModifier(entity, Attributes.FLYING_SPEED, ENEMY_FLYING_MODIFIER);
        removeModifier(entity, Attributes.ATTACK_SPEED, ENEMY_ATTACK_MODIFIER);
    }

    private static void addModifier(LivingEntity entity, Holder<Attribute> attribute,
                                    ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeModifier(LivingEntity entity, Holder<Attribute> attribute,
                                       ResourceLocation id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }

    private static boolean meetsRequirement(String cultivationLevel) {
        String required = CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE;
        return CultivationLevels.getStageIndex(cultivationLevel)
                >= CultivationLevels.getStageIndex(required);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path);
    }

    private static final class SlowedTarget {
        private final LivingEntity target;
        private final Set<UUID> controllers = new HashSet<>();

        private SlowedTarget(LivingEntity target) {
            this.target = target;
        }
    }

    private static final class FrozenProjectile {
        private final Projectile projectile;
        private final Vec3 anchor;
        private final Vec3 velocity;
        private final boolean noGravity;
        private final Set<UUID> controllers = new HashSet<>();

        private FrozenProjectile(Projectile projectile, Vec3 anchor, Vec3 velocity, boolean noGravity) {
            this.projectile = projectile;
            this.anchor = anchor;
            this.velocity = velocity;
            this.noGravity = noGravity;
        }
    }
}
