package com.example.immortal_cultivation_mod.particle;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.config.ModServerConfig;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MethodQiParticleEffects {
    private static final int PULSE_INTERVAL_TICKS = 2;
    private static final int PULSES_PER_QI_USE = 3;
    private static final String SHOW_QI_USE_PARTICLES_KEY =
            "immortal_cultivation_mod:show_qi_use_particles";
    private static final Map<UUID, PendingEmission> PENDING = new HashMap<>();

    private MethodQiParticleEffects() {
    }

    public static void emit(ServerPlayer player, String methodId, int qiSpent) {
        if (!showQiUseParticles(player)) {
            PENDING.remove(player.getUUID());
            return;
        }
        CultivationMethods.MethodDef method = CultivationMethods.get(methodId);
        if (method == null || qiSpent <= 0) {
            return;
        }

        List<DeferredHolder<ParticleType<?>, SimpleParticleType>> particleTypes =
                method.bloodDemon()
                        ? List.of(ModParticles.METHOD_QI_BLOOD)
                        : particlesFor(method.element());
        if (particleTypes.isEmpty()) {
            return;
        }
        int countPerPulse = Mth.clamp(
                4 + (int) Math.floor(Math.log10(qiSpent + 1.0D) * 3.0D),
                4,
                14);
        PendingEmission pending = PENDING.get(player.getUUID());
        if (pending != null && pending.particleTypes.equals(particleTypes)) {
            pending.countPerPulse = Math.max(pending.countPerPulse, countPerPulse);
            pending.pulsesRemaining = Math.min(
                    PULSES_PER_QI_USE, pending.pulsesRemaining + 1);
            return;
        }

        emitPulse(player, particleTypes, countPerPulse);
        PENDING.put(player.getUUID(), new PendingEmission(
                particleTypes,
                countPerPulse,
                PULSES_PER_QI_USE - 1,
                player.tickCount + PULSE_INTERVAL_TICKS));
    }

    public static void tick(ServerPlayer player) {
        if (!showQiUseParticles(player)) {
            PENDING.remove(player.getUUID());
            return;
        }
        PendingEmission pending = PENDING.get(player.getUUID());
        if (pending == null || player.tickCount < pending.nextPulseTick) {
            return;
        }
        if (!player.isAlive() || pending.pulsesRemaining <= 0) {
            PENDING.remove(player.getUUID());
            return;
        }

        emitPulse(player, pending.particleTypes, pending.countPerPulse);
        pending.pulsesRemaining--;
        if (pending.pulsesRemaining <= 0) {
            PENDING.remove(player.getUUID());
        } else {
            pending.nextPulseTick = player.tickCount + PULSE_INTERVAL_TICKS;
        }
    }

    public static List<SimpleParticleType> particlesForPlayer(ServerPlayer player) {
        CultivationMethods.MethodDef method = CultivationMethods.get(
                ModAttachments.getData(player).activeCultivationMethod());
        List<DeferredHolder<ParticleType<?>, SimpleParticleType>> holders;
        if (method == null) {
            holders = List.of(ModParticles.METHOD_QI_NEUTRAL);
        } else if (method.bloodDemon()) {
            holders = List.of(ModParticles.METHOD_QI_BLOOD);
        } else {
            holders = particlesFor(method.element());
        }
        return resolveParticles(holders);
    }

    public static void setQiUseParticlesEnabled(
            ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(
                SHOW_QI_USE_PARTICLES_KEY, enabled);
        if (!enabled) {
            PENDING.remove(player.getUUID());
        }
    }

    /**
     * Whether ordinary vision receives this player's automatic Qi-use
     * particles. Spiritual sense deliberately queries the same state so a
     * hidden emission can remain spiritually perceptible without respawning
     * the normal world particles.
     */
    public static boolean showQiUseParticles(ServerPlayer player) {
        if (!ModServerConfig.showQiUseParticles()) {
            return false;
        }
        return !player.getPersistentData().contains(SHOW_QI_USE_PARTICLES_KEY)
                || player.getPersistentData().getBoolean(
                        SHOW_QI_USE_PARTICLES_KEY);
    }

    public static List<SimpleParticleType> particlesForOwner(
            LivingEntity owner,
            String elementText) {
        if (owner instanceof ServerPlayer player) {
            return particlesForPlayer(player);
        }
        return particlesForElement(elementText);
    }

    public static List<SimpleParticleType> particlesForElement(String elementText) {
        return resolveParticles(particlesFor(elementText));
    }

    private static List<SimpleParticleType> resolveParticles(
            List<DeferredHolder<ParticleType<?>, SimpleParticleType>> holders) {
        List<SimpleParticleType> result = new ArrayList<>();
        for (DeferredHolder<ParticleType<?>, SimpleParticleType> holder : holders) {
            result.add(holder.get());
        }
        return List.copyOf(result);
    }

    private static void emitPulse(
            ServerPlayer player,
            List<DeferredHolder<ParticleType<?>, SimpleParticleType>> particleTypes,
            int total) {
        int remaining = Math.max(total, particleTypes.size());
        for (int i = 0; i < particleTypes.size(); i++) {
            int count = i == particleTypes.size() - 1
                    ? remaining
                    : Math.max(1, remaining / (particleTypes.size() - i));
            remaining -= count;
            player.serverLevel().sendParticles(
                    particleTypes.get(i).get(),
                    player.getX(),
                    player.getY() + player.getBbHeight() * 0.55D,
                    player.getZ(),
                    count,
                    0.42D,
                    player.getBbHeight() * 0.42D,
                    0.42D,
                    0.035D);
        }
    }

    private static final class PendingEmission {
        private final List<DeferredHolder<ParticleType<?>, SimpleParticleType>> particleTypes;
        private int countPerPulse;
        private int pulsesRemaining;
        private int nextPulseTick;

        private PendingEmission(
                List<DeferredHolder<ParticleType<?>, SimpleParticleType>> particleTypes,
                int countPerPulse,
                int pulsesRemaining,
                int nextPulseTick) {
            this.particleTypes = particleTypes;
            this.countPerPulse = countPerPulse;
            this.pulsesRemaining = pulsesRemaining;
            this.nextPulseTick = nextPulseTick;
        }
    }

    private static List<DeferredHolder<ParticleType<?>, SimpleParticleType>> particlesFor(
            String elementText) {
        if ("any".equals(elementText)) {
            return List.of(ModParticles.METHOD_QI_NEUTRAL);
        }
        if (elementText == null || elementText.isBlank() || "none".equals(elementText)) {
            return List.of(ModParticles.METHOD_QI_NEUTRAL);
        }

        List<DeferredHolder<ParticleType<?>, SimpleParticleType>> result = new ArrayList<>();
        for (String element : elementText.split(",")) {
            result.add(particleFor(element.trim()));
        }
        return result.isEmpty() ? List.of(ModParticles.METHOD_QI_NEUTRAL) : result;
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> particleFor(String element) {
        return switch (element) {
            case "blood" -> ModParticles.METHOD_QI_BLOOD;
            case SpiritRoots.METAL -> ModParticles.METHOD_QI_METAL;
            case SpiritRoots.WOOD -> ModParticles.METHOD_QI_WOOD;
            case SpiritRoots.WATER -> ModParticles.METHOD_QI_WATER;
            case SpiritRoots.FIRE -> ModParticles.METHOD_QI_FIRE;
            case SpiritRoots.EARTH -> ModParticles.METHOD_QI_EARTH;
            case SpiritRoots.THUNDER -> ModParticles.METHOD_QI_THUNDER;
            case SpiritRoots.ICE -> ModParticles.METHOD_QI_ICE;
            case SpiritRoots.WIND -> ModParticles.METHOD_QI_WIND;
            case SpiritRoots.LIGHT -> ModParticles.METHOD_QI_LIGHT;
            case SpiritRoots.DARK -> ModParticles.METHOD_QI_DARK;
            default -> ModParticles.METHOD_QI_NEUTRAL;
        };
    }
}
