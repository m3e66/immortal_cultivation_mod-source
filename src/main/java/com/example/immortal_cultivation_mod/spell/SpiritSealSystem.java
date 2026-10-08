package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Real Qi, charging, recovery, cleansing, and VFX behavior for 封灵符. */
public final class SpiritSealSystem {
    private static final int FULL_DURATION_TICKS = 15 * 20;
    private static final int RESISTED_DURATION_TICKS = 8 * 20;
    private static final int HEAVILY_RESISTED_DURATION_TICKS = 3 * 20;
    private static final String ACTIVE_TAG =
            ImmortalCultivationMod.MODID + ":spirit_seal_active";
    private static final DustParticleOptions DARK_GOLD =
            new DustParticleOptions(new Vector3f(0.53F, 0.36F, 0.08F), 0.85F);
    private static final DustParticleOptions CRACK_GOLD =
            new DustParticleOptions(new Vector3f(0.95F, 0.70F, 0.18F), 0.75F);

    private SpiritSealSystem() {
    }

    public static void apply(LivingEntity caster, LivingEntity target) {
        int difference = realm(target) - realm(caster);
        int duration = difference >= 2
                ? HEAVILY_RESISTED_DURATION_TICKS
                : difference == 1
                ? RESISTED_DURATION_TICKS
                : FULL_DURATION_TICKS;
        if (!target.addEffect(new MobEffectInstance(
                ModEffects.SPIRIT_SEAL, duration, 0,
                false, true, false), caster)) return;
        target.getPersistentData().putBoolean(ACTIVE_TAG, true);
        if (target instanceof ServerPlayer player) {
            CombatStateManager.interruptSpellPreparation(player);
        } else if (target instanceof CultivationNpc npc) {
            npc.endNpcCast();
        }
        if (target.level() instanceof ServerLevel level) {
            emitSeal(level, target, true);
        }
    }

    public static int adjustSpellQiCost(Player player, int cost) {
        return player.hasEffect(ModEffects.SPIRIT_SEAL)
                ? Mth.ceil(Math.max(0, cost) * 1.30D) : Math.max(0, cost);
    }

    public static int adjustSustainQiCost(Player player, int cost) {
        return player.hasEffect(ModEffects.SPIRIT_SEAL)
                ? Mth.ceil(Math.max(0, cost) * 1.20D) : Math.max(0, cost);
    }

    public static int adjustNaturalQiRegen(Player player, int amount) {
        return player.hasEffect(ModEffects.SPIRIT_SEAL)
                ? Math.max(0, Mth.floor(Math.max(0, amount) * 0.30D))
                : Math.max(0, amount);
    }

    /** A deliberate burst spending at least 25% max Qi tears the seal open. */
    public static void onQiSpent(Player player, int amount) {
        if (!player.hasEffect(ModEffects.SPIRIT_SEAL) || amount <= 0) return;
        var data = ModAttachments.getData(player);
        int maxQi = Math.max(1,
                CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi()
                        + data.maxQiBonus());
        if (amount * 4 >= maxQi) player.removeEffect(ModEffects.SPIRIT_SEAL);
        else if (player.level() instanceof ServerLevel level) {
            emitSeal(level, player, true);
        }
    }

    public static void tick(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        boolean active = target.hasEffect(ModEffects.SPIRIT_SEAL);
        boolean wasActive = target.getPersistentData().getBoolean(ACTIVE_TAG);
        if (!active) {
            if (wasActive) {
                target.getPersistentData().remove(ACTIVE_TAG);
                emitBreak(level, target);
            }
            return;
        }
        target.getPersistentData().putBoolean(ACTIVE_TAG, true);
        if ((target.tickCount & 1) == 0) emitSeal(level, target, false);
    }

    private static int realm(LivingEntity entity) {
        if (entity instanceof Player player) {
            return CultivationLevels.getRealmIndex(
                    ModAttachments.getData(player).cultivationLevel());
        }
        if (entity instanceof CultivationNpc npc) {
            return CultivationLevels.getRealmIndex(npc.npcRealm());
        }
        return 0;
    }

    private static void emitSeal(
            ServerLevel level, LivingEntity target, boolean burst) {
        Vec3 center = target.position().add(
                0.0D, target.getBbHeight() * 0.58D, 0.0D);
        Vec3[] ends = {
                center.add(-target.getBbWidth() * 0.72D, 0.12D, 0.0D),
                center.add(target.getBbWidth() * 0.72D, 0.12D, 0.0D),
                center.add(0.0D, -target.getBbHeight() * 0.48D, 0.0D),
                center.add(0.0D, target.getBbHeight() * 0.40D, 0.0D)
        };
        level.sendParticles(DARK_GOLD, center.x, center.y, center.z,
                burst ? 8 : 2, 0.09D, 0.12D, 0.09D, 0.006D);
        int segments = burst ? 11 : 5;
        for (Vec3 end : ends) {
            for (int i = 1; i <= segments; i++) {
                Vec3 point = center.lerp(end, i / (double) segments);
                level.sendParticles(DARK_GOLD, point.x, point.y, point.z,
                        1, 0.006D, 0.006D, 0.006D, 0.0D);
            }
        }
        double time = target.tickCount * 0.17D;
        int motes = burst ? 12 : 3;
        for (int i = 0; i < motes; i++) {
            double angle = time + i * Mth.TWO_PI / motes;
            double radius = 0.75D + (i % 3) * 0.18D;
            level.sendParticles(CRACK_GOLD,
                    center.x + Math.cos(angle) * radius,
                    center.y + Math.sin(angle * 0.63D) * 0.42D,
                    center.z + Math.sin(angle) * radius,
                    1, 0.025D, 0.025D, 0.025D, 0.002D);
        }
    }

    private static void emitBreak(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(
                0.0D, target.getBbHeight() * 0.58D, 0.0D);
        level.sendParticles(CRACK_GOLD, center.x, center.y, center.z,
                28, target.getBbWidth() * 0.55D,
                target.getBbHeight() * 0.45D,
                target.getBbWidth() * 0.55D, 0.11D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.x, center.y, center.z,
                9, 0.30D, 0.45D, 0.30D, 0.08D);
    }
}
