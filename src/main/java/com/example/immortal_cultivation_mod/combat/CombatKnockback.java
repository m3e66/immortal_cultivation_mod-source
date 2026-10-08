package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.spell.SpellDestruction;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Server-confirmed impacts; no terrain damage from ordinary walking, falling or status ticks. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class CombatKnockback {
    private static final Map<LivingEntity, Impact> IMPACTS = new WeakHashMap<>();
    private static final class Impact {
        Entity attacker;
        Vec3 direction, previous;
        ServerLevel level;
        float damage;
        int blocks, age;
        boolean pending = true;
    }
    private CombatKnockback() {}

    public static double strength(float damage) {
        return Float.isFinite(damage) && damage > 0 ? Math.min(2.8, .35 + Math.sqrt(damage) * .075) : 0;
    }
    public static int blockBudget(float damage) {
        return Float.isFinite(damage) && damage > 0 ? (int) Math.min(256, Math.ceil(damage * .8)) : 0;
    }

    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        var source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(target.level() instanceof ServerLevel level) || attacker == null || attacker == target
                || !target.isAlive() || strength(event.getNewDamage()) == 0
                || (target instanceof ServerPlayer player && (player.isSpectator() || player.isCreative()))) return;
        // Ongoing burns and poison have no attacker; damage that carries an attacker is an impact.
        Vec3 direction = CombatFeedbackEvents.incomingDirection(target, source).multiply(1, 0, 1);
        if (direction.lengthSqr() < .0001) direction = attacker.getLookAngle().multiply(1, 0, 1);
        if (direction.lengthSqr() < .0001) direction = new Vec3(0, 0, 1);
        Impact impact = IMPACTS.get(target);
        if (impact != null && impact.pending && impact.damage >= event.getNewDamage()) return;
        impact = new Impact();
        impact.attacker = attacker;
        impact.direction = direction.normalize();
        impact.previous = target.position();
        impact.level = level;
        impact.damage = event.getNewDamage();
        impact.blocks = blockBudget(impact.damage);
        if (!BlockBreakingSettings.enabled(attacker,
                BlockBreakingSettings.Category.COMBAT_IMPACTS)) impact.blocks = 0;
        if (source.getDirectEntity() instanceof com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity slash
                && !slash.allowsTerrainBreaking()) impact.blocks = 0;
        else if (attacker instanceof ServerPlayer player
                && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && source.getDirectEntity() == player
                && (!CombatStateManager.isHeavyAttack(source)
                || !com.example.immortal_cultivation_mod.item.QiInfusedWeapon.outwardQiBlockBreakingEnabled(player)))
            impact.blocks = 0;
        IMPACTS.put(target, impact);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        Impact impact = IMPACTS.get(target);
        if (impact == null) return;
        if (!target.isAlive() || target.isRemoved() || target.level() != impact.level || ++impact.age > 20
                || target.position().distanceToSqr(impact.previous) > 64
                || impact.attacker.isRemoved() || impact.attacker.level() != impact.level) {
            IMPACTS.remove(target); return;
        }
        if (impact.pending) {
            // Apply after the attack finishes: vanilla player attacks can restore pre-hit motion.
            Vec3 before = target.getDeltaMovement();
            target.knockback(strength(impact.damage), -impact.direction.x, -impact.direction.z);
            if (target.getDeltaMovement().equals(before)) { IMPACTS.remove(target); return; }
            target.hasImpulse = true;
            target.hurtMarked = true;
            impact.pending = false;
            if (target instanceof ServerPlayer player)
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        }
        Vec3 velocity = target.getDeltaMovement();
        if (!BlockBreakingSettings.enabled(impact.attacker,
                BlockBreakingSettings.Category.COMBAT_IMPACTS)) impact.blocks = 0;
        Vec3 travelled = target.position().subtract(impact.previous);
        impact.previous = target.position();
        double forwardSpeed = Math.max(velocity.dot(impact.direction), travelled.dot(impact.direction));
        if (!(target instanceof ServerPlayer) || impact.blocks <= 0) {
            if (impact.age > 2) IMPACTS.remove(target);
            return;
        }
        if (forwardSpeed < .08) { if (impact.age > 2) IMPACTS.remove(target); return; }
        // A narrow swept body first proves contact. Only then widen the impact around that contact.
        AABB sweep = target.getBoundingBox().deflate(.025)
                .expandTowards(impact.direction.scale(Math.min(2.8, forwardSpeed)));
        BlockPos contact = null;
        double nearest = Double.MAX_VALUE;
        int probes = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(sweep.minX, sweep.minY, sweep.minZ),
                BlockPos.containing(sweep.maxX, sweep.maxY, sweep.maxZ))) {
            if (++probes > 128) break;
            if (!impact.level.hasChunkAt(pos)) continue;
            var shape = impact.level.getBlockState(pos).getCollisionShape(impact.level, pos);
            boolean intersects = shape.toAabbs().stream().anyMatch(box -> box.move(pos).intersects(sweep));
            double distance = Vec3.atCenterOf(pos).distanceToSqr(target.getBoundingBox().getCenter());
            if (intersects && distance < nearest) { contact = pos.immutable(); nearest = distance; }
        }
        if (contact == null) return;
        double radius = Math.min(2.5, .5 + Math.sqrt(impact.damage) * .1);
        Vec3 center = Vec3.atCenterOf(contact);
        int extent = (int) Math.ceil(radius), broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(contact.offset(-extent, -extent, -extent),
                contact.offset(extent, extent, extent))) {
            if (broken >= 32 || impact.blocks <= 0 || !SpellDestruction.hasBudget(impact.level)) break;
            if (Vec3.atCenterOf(pos).distanceToSqr(center) > radius * radius || !impact.level.hasChunkAt(pos)) continue;
            if (BlockBreakingSettings.withSource(
                    BlockBreakingSettings.Category.COMBAT_IMPACTS, impact.attacker,
                    () -> SpellDestruction.breakNow(impact.level, pos, false, impact.attacker))) {
                broken++; impact.blocks--;
            }
        }
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) { IMPACTS.clear(); }
}
