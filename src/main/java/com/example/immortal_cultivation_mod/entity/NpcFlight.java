package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-owned flight upkeep and travel-facing shared by cultivation NPCs. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class NpcFlight {
    private static final String PAID_UNTIL = "FlightQiPaidUntil";
    private NpcFlight() {}

    public static int qiPerSecond(PathfinderMob mob) {
        return NpcSpellcasting.hasCreativeFlight(mob) ? 100 : 30;
    }

    private static boolean paid(PathfinderMob mob) {
        long remaining = NpcProfile.cultivationProfile(mob).getLong(PAID_UNTIL) - mob.level().getGameTime();
        return remaining > 0 && remaining <= 20;
    }

    public static boolean canSustain(PathfinderMob mob) {
        return mob instanceof CultivationNpc npc && (paid(mob)
                || NpcProfile.qi(mob, npc.npcArchetype()) >= qiPerSecond(mob));
    }

    /** Prepay one second, including hovering; repeated calls cannot charge twice. */
    public static boolean payUpkeep(PathfinderMob mob) {
        if (mob.level().isClientSide || !(mob instanceof CultivationNpc npc)) return false;
        if (paid(mob)) return true;
        if (!NpcProfile.spendQi(mob, npc.npcArchetype(), qiPerSecond(mob))) return false;
        NpcProfile.cultivationProfile(mob).putLong(PAID_UNTIL, mob.level().getGameTime() + 20);
        return true;
    }

    @SubscribeEvent
    public static void faceTravel(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof PathfinderMob mob && mob instanceof CultivationNpc
                && !mob.level().isClientSide && mob.isAlive() && !mob.isPassenger()) {
            faceMovement(mob, mob.position().subtract(mob.xo, mob.yo, mob.zo));
        }
    }

    static void faceMovement(PathfinderMob mob, Vec3 movement) {
        // Ignore standing still, vertical travel and teleport discontinuities.
        if (!Double.isFinite(movement.lengthSqr()) || movement.lengthSqr() > 100
                || movement.horizontalDistanceSqr() < 0.00001) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-movement.x, movement.z));
        // Vanilla look/move controls have already run. Start from the previous tick's
        // rendered angles so their intermediate snaps cannot bypass the turn limit.
        float turn = mob.isNoGravity() ? 12 : 18;
        mob.setYRot(turnToward(mob.yRotO, yaw, turn));
        mob.setYBodyRot(turnToward(mob.yBodyRotO, yaw, turn));
        mob.setYHeadRot(turnToward(mob.yHeadRotO, yaw, turn));
    }

    private static float turnToward(float previous, float wanted, float limit) {
        return previous + Mth.clamp(Mth.wrapDegrees(wanted - previous), -limit, limit);
    }

    /** Bounded steering preserves momentum through turns without instant reversals. */
    static Vec3 approachVelocity(Vec3 current, Vec3 desired, double acceleration) {
        Vec3 change = desired.subtract(current);
        double length = change.length();
        return length <= acceleration ? desired : current.add(change.scale(acceleration / length));
    }

    static void brake(PathfinderMob mob) {
        double deceleration = NpcSpellcasting.hasCreativeFlight(mob) ? .22 : .065;
        mob.setDeltaMovement(approachVelocity(mob.getDeltaMovement(), Vec3.ZERO, deceleration));
        // Normal entity tracking synchronizes steering. Do not mark each tick as a
        // damage impulse, which sends redundant motion packets between position updates.
    }
}
