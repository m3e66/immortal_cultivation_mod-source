package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Visible threats and brief, directional guards; no invulnerability or automatic hit cancellation. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class NpcCombatReactions {
    private NpcCombatReactions() {}

    public static boolean disabled(LivingEntity entity) {
        return entity.hasEffect(ModEffects.DAZE) || entity.hasEffect(ModEffects.DINGSHEN)
                || entity.hasEffect(ModEffects.FROZEN) || entity.hasEffect(ModEffects.FROZEN_QI);
    }

    public static boolean incoming(AABB body, Vec3 position, Vec3 relativeVelocity) {
        return relativeVelocity.lengthSqr() > .01
                && (body.inflate(.35).contains(position)
                || body.inflate(.35).clip(position, position.add(relativeVelocity.scale(5))).isPresent());
    }

    public static Entity projectile(PathfinderMob defender) {
        Entity nearest = null;
        double impactTime = Double.MAX_VALUE;
        int inspected = 0;
        for (Entity projectile : defender.level().getEntities(defender,
                defender.getBoundingBox().inflate(10), entity -> !entity.isRemoved()
                        && (entity instanceof Projectile || entity instanceof ForcefieldInterceptable attack
                        && attack.forcefieldBarrierDamage() > 0))) {
            if (++inspected > 48) break;
            ForcefieldInterceptable attack = projectile instanceof ForcefieldInterceptable supported ? supported : null;
            Entity owner = attack != null ? attack.forcefieldOwner() : ((Projectile)projectile).getOwner();
            if (owner == defender || owner instanceof LivingEntity living && NpcProfile.areCombatAllies(defender, living)) continue;
            Vec3 center = attack == null ? projectile.position() : attack.forcefieldCenter();
            Vec3 velocity = (attack == null ? projectile.getDeltaMovement() : attack.forcefieldMovement())
                    .subtract(defender.getDeltaMovement());
            // External Qi and other non-Projectile attacks expose their real oriented shape.
            // Axis support avoids treating a wide blade as a point, without a spherical threat radius.
            AABB body = defender.getBoundingBox();
            if (attack != null) body = body.inflate(attack.forcefieldSupport(new Vec3(1,0,0)),
                    attack.forcefieldSupport(new Vec3(0,1,0)), attack.forcefieldSupport(new Vec3(0,0,1)));
            if (!defender.hasLineOfSight(projectile)
                    || center.subtract(defender.getBoundingBox().getCenter()).dot(velocity) >= 0
                    || !incoming(body, center, velocity)) continue;
            var intersection = body.inflate(.35).clip(center, center.add(velocity.scale(5)));
            double candidate = body.contains(center) ? 0 : intersection.map(point -> point.distanceTo(center)
                    / velocity.length()).orElse(Double.MAX_VALUE);
            if (candidate < impactTime) { nearest = projectile; impactTime = candidate; }
        }
        return nearest;
    }

    public static boolean startGuard(PathfinderMob defender) {
        if (!(defender instanceof CultivationNpc npc) || defender.getMainHandItem().isEmpty()
                || disabled(defender) || npc.npcCastingTicks() > 0
                || defender.getPersistentData().getLong("NpcGuardReady") > defender.level().getGameTime()
                || !NpcProfile.spendQi(defender, npc.npcArchetype(), 2)) return false;
        long now = defender.level().getGameTime();
        int startup = NpcSpellcasting.hasCreativeFlight(defender) ? 1 : 2;
        defender.getPersistentData().putLong("NpcGuardActiveAfter", now + startup);
        defender.getPersistentData().putLong("NpcGuardRecoveryUntil", now + startup + 8 + 5);
        defender.getPersistentData().putLong("NpcGuardReady", now + 32);
        defender.addEffect(new MobEffectInstance(ModEffects.NPC_WEAPON_GUARD, startup + 8, 0, false, false, false));
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void guard(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob defender) || !(defender instanceof CultivationNpc npc)
                || !defender.hasEffect(ModEffects.NPC_WEAPON_GUARD) || defender.level().isClientSide
                || defender.isNoAi() || NpcProfile.isInteracting(defender) || disabled(defender)
                || npc.npcCastingTicks() > 0 || defender.getMainHandItem().isEmpty() || event.getAmount() <= 0) return;
        if (defender.level().getGameTime() < defender.getPersistentData().getLong("NpcGuardActiveAfter")) return;
        var source = event.getSource();
        boolean physical = source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.PLAYER_ATTACK);
        if (!physical || source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypeTags.IS_EXPLOSION)) return;
        Vec3 position = source.getSourcePosition();
        if (position == null) return;
        Vec3 towardHit = position.subtract(defender.position()).multiply(1, 0, 1).normalize();
        Vec3 facing = defender.getLookAngle().multiply(1, 0, 1).normalize();
        if (towardHit.dot(facing) < .25) return;
        int cost = Math.max(1, (int)Math.ceil(event.getAmount() * .2));
        if (!NpcProfile.spendQi(defender, npc.npcArchetype(), cost)) {
            defender.removeEffect(ModEffects.NPC_WEAPON_GUARD);
            return;
        }
        float preventedDamage = event.getAmount() * .55F;
        event.setAmount(event.getAmount() * .45F);
        com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.blocked(defender, source, preventedDamage);
        defender.getMainHandItem().hurtAndBreak(1, defender, EquipmentSlot.MAINHAND);
    }
}
