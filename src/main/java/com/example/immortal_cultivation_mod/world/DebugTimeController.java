package com.example.immortal_cultivation_mod.world;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.DebugTimePayloads;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-owned, forward-only world time acceleration. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class DebugTimeController {
    public static final int DAY_TICKS = 24_000;
    public static final int MINIMUM_FORWARD_TICKS = 500;
    public static final int MAX_SPEED_MULTIPLIER = 5;
    private static final int BASE_TIME_STEP = 10;
    private static final int DECELERATION_TICKS = 40;
    private static final int SETTLING_TICKS = 12;
    private static final Map<ServerLevel, FastForward> ACTIVE = new IdentityHashMap<>();

    private DebugTimeController() {
    }

    public static void start(
            ServerPlayer player, int targetTimeOfDay, int speedMultiplier) {
        if (targetTimeOfDay < 0 || targetTimeOfDay >= DAY_TICKS
                || (!player.getMainHandItem().is(ModItems.DEBUG_TIME_CONTROLLER.get())
                && !player.getOffhandItem().is(ModItems.DEBUG_TIME_CONTROLLER.get()))) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long current = level.getDayTime();
        long currentTimeOfDay = Math.floorMod(current, (long) DAY_TICKS);
        long delta = Math.floorMod(
                (long) targetTimeOfDay - currentTimeOfDay, (long) DAY_TICKS);
        if (delta < MINIMUM_FORWARD_TICKS) {
            delta += DAY_TICKS;
        }

        int validatedSpeed = Math.max(
                1, Math.min(MAX_SPEED_MULTIPLIER, speedMultiplier));
        int stepTicks = BASE_TIME_STEP * validatedSpeed;
        FastForward fastForward = new FastForward(
                current, current + delta, stepTicks);
        ACTIVE.put(level, fastForward);
        broadcast(level, current,
                effectiveStep(fastForward, delta), true);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        FastForward fastForward = ACTIVE.get(level);
        if (fastForward == null) {
            return;
        }

        long remaining = fastForward.targetDayTime - fastForward.currentDayTime;
        if (remaining <= 0L) {
            finish(level, fastForward);
            return;
        }

        int currentStep = effectiveStep(fastForward, remaining);
        long next = fastForward.currentDayTime
                + Math.min(remaining, currentStep);
        fastForward.currentDayTime = next;
        level.setDayTime(next);
        if (next >= fastForward.targetDayTime) {
            finish(level, fastForward);
        } else {
            broadcast(level, next, effectiveStep(
                    fastForward, fastForward.targetDayTime - next), true);
        }
    }

    private static int effectiveStep(FastForward fastForward, long remaining) {
        int maximumStep = fastForward.stepTicks;
        if (maximumStep <= 1 || remaining <= SETTLING_TICKS) {
            return 1;
        }
        double brakingDistance = (maximumStep + 1.0D)
                * DECELERATION_TICKS * 0.5D;
        if (remaining >= brakingDistance) {
            return maximumStep;
        }
        double ratio = Math.max(0.0D, remaining / brakingDistance);
        double squaredSpeed = 1.0D
                + (maximumStep * (double) maximumStep - 1.0D) * ratio;
        int curvedStep = Math.max(1, Math.min(maximumStep,
                (int) Math.round(Math.sqrt(squaredSpeed))));
        return (int) Math.max(1L, Math.min(
                curvedStep, remaining - SETTLING_TICKS));
    }

    private static void finish(ServerLevel level, FastForward fastForward) {
        level.setDayTime(fastForward.targetDayTime);
        ACTIVE.remove(level);
        broadcast(level, fastForward.targetDayTime, 0, false);
    }

    private static void broadcast(
            ServerLevel level, long dayTime, int stepTicks, boolean active) {
        DebugTimePayloads.ClientboundFastForwardState payload =
                new DebugTimePayloads.ClientboundFastForwardState(
                        dayTime, stepTicks, active);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static final class FastForward {
        private long currentDayTime;
        private final long targetDayTime;
        private final int stepTicks;

        private FastForward(
                long currentDayTime, long targetDayTime, int stepTicks) {
            this.currentDayTime = currentDayTime;
            this.targetDayTime = targetDayTime;
            this.stepTicks = stepTicks;
        }
    }
}
