package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Weiya {
    private static final double RADIUS = 16.0D;
    private static final int REFRESH_TICKS = 40;
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();

    private Weiya() {
    }

    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE.remove(id) != null) {
            stop(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (CultivationLevels.isMortal(data.cultivationLevel())) {
            return;
        }

        int cost = qiCostPerSecond(data);
        if (!ServerEvents.spendQiOrBlood(player, data, cost)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ACTIVE.put(id, true);
        refreshCaster(player);
        PhotonEffects.weiyaStart(player);
        ServerEvents.syncPlayerData(player);
    }

    public static void tick(ServerPlayer player) {
        if (!ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        var data = ModAttachments.getData(player);
        if (CultivationLevels.isMortal(data.cultivationLevel())) {
            stop(player);
            return;
        }

        refreshCaster(player);
        applyPressure(player, data.cultivationLevel());

        if (player.tickCount % REFRESH_TICKS == 0) {
            PhotonEffects.weiyaStart(player);
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        if (!ServerEvents.spendQiOrBlood(player, data, qiCostPerSecond(data))) {
            stop(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ServerEvents.syncPlayerData(player);
    }

    public static boolean isSuppressed(Player player) {
        return player.hasEffect(ModEffects.WEIYA_SUPPRESSED);
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        player.removeEffect(ModEffects.WEIYA);
        PhotonEffects.weiyaStop(player);
    }

    private static void stop(ServerPlayer player) {
        clear(player);
        ServerEvents.syncPlayerData(player);
    }

    private static void refreshCaster(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.WEIYA, REFRESH_TICKS, 0, false, false, true));
    }

    private static void applyPressure(ServerPlayer caster, String casterLevel) {
        List<LivingEntity> targets = caster.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(caster.blockPosition()).inflate(RADIUS),
                target -> target != caster && target.isAlive());

        for (LivingEntity target : targets) {
            PressureStrength strength = pressureStrength(casterLevel, targetLevel(target));
            if (strength == PressureStrength.NONE) {
                continue;
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, REFRESH_TICKS, strength.slownessAmplifier(), false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, REFRESH_TICKS, strength.weaknessAmplifier(), false, false, true));
            if (strength.suppressesSpells() && target instanceof Player player) {
                player.addEffect(new MobEffectInstance(ModEffects.WEIYA_SUPPRESSED, REFRESH_TICKS, 0, false, false, true));
            }
        }
    }

    private static String targetLevel(LivingEntity target) {
        if (target instanceof Player player) {
            return ModAttachments.getData(player).cultivationLevel();
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

    private static int qiCostPerSecond(ModAttachments.CultivationData data) {
        int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        return Math.max(1, (int) Math.ceil(maxQi * 0.01D));
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
