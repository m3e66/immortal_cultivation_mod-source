package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Weiya {
    private static final int REFRESH_TICKS = 40;
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TIMED_UNTIL = new ConcurrentHashMap<>();

    private Weiya() {
    }

    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();

        if (ACTIVE.remove(id) != null) {
            stop(player);
            return;
        }

        var data = ModAttachments.getData(player);

        if (!player.isAlive() || CultivationLevels.isMortal(data.cultivationLevel())) {
            return;
        }

        int cost = qiCostPerSecond(data);

        if (!ServerEvents.spendSpellQiOrBlood(player, data, cost)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ACTIVE.put(id, true);
        refreshCaster(player);


        ServerEvents.syncPlayerData(player);
    }

    public static void tick(ServerPlayer player) {
        if (!ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        Long timedUntil = TIMED_UNTIL.get(player.getUUID());
        if (timedUntil != null && player.level().getGameTime() >= timedUntil) {
            stop(player);
            return;
        }

        var data = ModAttachments.getData(player);

        if (!player.isAlive() || CultivationLevels.isMortal(data.cultivationLevel())) {
            stop(player);
            return;
        }

        refreshCaster(player);
        if (player.tickCount % 5 == 0) applyPressure(player, data.cultivationLevel());

        if (timedUntil != null || player.tickCount % 20 != 0) {
            return;
        }

        if (!ServerEvents.spendSustainedQiOrBlood(
                player, data, qiCostPerSecond(data))) {
            stop(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ServerEvents.syncPlayerData(player);
    }

    public static boolean isSuppressed(Player player) {
        return player.hasEffect(ModEffects.DAZE);
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        TIMED_UNTIL.remove(player.getUUID());
        player.removeEffect(ModEffects.WEIYA);
    }

    public static void activateTimed(ServerPlayer player, int durationTicks) {
        var data = ModAttachments.getData(player);
        if (CultivationLevels.isMortal(data.cultivationLevel())) {
            return;
        }
        UUID id = player.getUUID();
        ACTIVE.put(id, true);
        long now = player.level().getGameTime();
        long current = TIMED_UNTIL.getOrDefault(id, now);
        TIMED_UNTIL.put(id, Math.max(now, current) + durationTicks);
        refreshCaster(player);
        ServerEvents.syncPlayerData(player);
    }

    private static void stop(ServerPlayer player) {
        clear(player);
        ServerEvents.syncPlayerData(player);
    }

    private static void refreshCaster(ServerPlayer player) {
        int stage = CultivationLevels.getStageIndex(ModAttachments.getData(player).cultivationLevel());
        var old = player.getEffect(ModEffects.WEIYA);
        if (old != null && old.getAmplifier() == stage && old.getDuration() > 25) return;
        if (old != null && old.getAmplifier() != stage) player.removeEffect(ModEffects.WEIYA);
        player.addEffect(new MobEffectInstance(ModEffects.WEIYA, REFRESH_TICKS, stage, false, false, true));
    }

    private static void applyPressure(ServerPlayer caster, String casterLevel) {
        double radius = radius(casterLevel);
        List<LivingEntity> targets = caster.level().getEntitiesOfClass(
                LivingEntity.class,
                caster.getBoundingBox().inflate(radius),
                target -> target != caster && target.isAlive()
                        && contains(caster.position(), radius, target.position())
        );

        for (LivingEntity target : targets) {
            applyPressureEffects(casterLevel, target);
        }
    }

    /**
     * Applies the same realm comparison as active 威压 without requiring a
     * living caster or spending Qi.  Persistent relic blocks use this to keep
     * their pressure active while their chunk is loaded.
     */
    public static void applyAmbientPressure(ServerLevel level, BlockPos source,
                                            String sourceLevel) {
        double radius = radius(sourceLevel);
        Vec3 center = Vec3.atBottomCenterOf(source);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(source).inflate(radius),
                target -> target.isAlive() && contains(center, radius, target.position())
        );
        for (LivingEntity target : targets) {
            applyPressureEffects(sourceLevel, target);
        }
    }

    public static void applyPressureEffects(String sourceLevel, LivingEntity target) {
        PressureStrength strength = pressureStrength(sourceLevel, targetLevel(target));

        if (strength == PressureStrength.NONE) {
            return;
        }

        target.addEffect(new MobEffectInstance(ModEffects.WEIYA_SUPPRESSED, REFRESH_TICKS,
                strength.ordinal() - 1, false, false, true));

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, REFRESH_TICKS,
                strength.slownessAmplifier(), false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, REFRESH_TICKS,
                strength.weaknessAmplifier(), false, false, true));

        if (strength.suppressesSpells()) {
            target.addEffect(new MobEffectInstance(ModEffects.DAZE, REFRESH_TICKS,
                    0, false, false, true));
        }
    }

    private static String targetLevel(LivingEntity target) {
        if (target instanceof Player player) {
            return ModAttachments.getData(player).cultivationLevel();
        }
        if (target instanceof CultivationNpc npc) {
            return npc.npcRealm();
        }

        return CultivationLevels.REALM_MORTAL;
    }

    private static PressureStrength pressureStrength(String casterLevel, String targetLevel) {
        int casterStage = CultivationLevels.getStageIndex(casterLevel);
        int targetStage = CultivationLevels.getStageIndex(targetLevel);

        if (targetStage >= casterStage) {
            return PressureStrength.NONE;
        }

        int realmDiff = CultivationLevels.getRealmIndex(casterLevel) - CultivationLevels.getRealmIndex(targetLevel);

        if (realmDiff == 1) {
            return PressureStrength.BIG_LEVEL;
        }

        if (realmDiff == 0 && casterStage - targetStage == 1) {
            return PressureStrength.SMALL_LEVEL;
        }

        return PressureStrength.OVERWHELMING;
    }

    public static double radius(String level) {
        return radiusForStage(CultivationLevels.getStageIndex(level));
    }

    public static double radiusForStage(int stage) {
        return stage <= 0 ? 0 : Math.min(64, 12 + (stage - 1) * 2.0D);
    }

    public static boolean contains(Vec3 center, double radius, Vec3 point) {
        return radius > 0 && center.distanceToSqr(point) <= radius * radius;
    }

    /** Rain stops at the top of the pressure dome, without changing world weather. */
    public static double rainCeiling(Vec3 center, double radius, double x, double z) {
        double dx = x - center.x, dz = z - center.z;
        double remaining = radius * radius - dx * dx - dz * dz;
        return radius <= 0 || remaining < 0 ? Double.NEGATIVE_INFINITY : center.y + Math.sqrt(remaining);
    }

    public static int qiCostPerSecond(ModAttachments.CultivationData data) {
        long maxQi = Math.max(1L, (long) CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) Math.ceil(maxQi * 0.02D)));
    }

    private enum PressureStrength {
        NONE(0, 0, false),
        SMALL_LEVEL(2, 2, false),
        BIG_LEVEL(4, 4, false),
        OVERWHELMING(9, 9, true);

        private final int slownessAmplifier;
        private final int weaknessAmplifier;
        private final boolean suppressesSpells;

        PressureStrength(int slownessAmplifier, int weaknessAmplifier, boolean suppressesSpells) {
            this.slownessAmplifier = slownessAmplifier;
            this.weaknessAmplifier = weaknessAmplifier;
            this.suppressesSpells = suppressesSpells;
        }

        private int slownessAmplifier() {
            return slownessAmplifier;
        }

        private int weaknessAmplifier() {
            return weaknessAmplifier;
        }

        private boolean suppressesSpells() {
            return suppressesSpells;
        }
    }
}
