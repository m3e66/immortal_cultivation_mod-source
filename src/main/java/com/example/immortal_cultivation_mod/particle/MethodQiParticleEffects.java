package com.example.immortal_cultivation_mod.particle;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.config.ModServerConfig;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public final class MethodQiParticleEffects {
    private static final String SHOW_QI_USE_PARTICLES_KEY =
            "immortal_cultivation_mod:show_qi_use_particles";

    private MethodQiParticleEffects() {
    }

    public static void emit(ServerPlayer player, String methodId, int qiSpent) {
        com.example.immortal_cultivation_mod.combat.QiDomainSystem.qiUsed(player, methodId, qiSpent);
    }

    public static void tick(ServerPlayer player) {
        // The unified flowing-Qi renderer owns automatic consumption visuals now.
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
        return switch (SpiritRoots.normalizeElement(element)) {
            case "blood" -> ModParticles.METHOD_QI_BLOOD;
            case SpiritRoots.METAL -> ModParticles.METHOD_QI_METAL;
            case SpiritRoots.WOOD -> ModParticles.METHOD_QI_WOOD;
            case SpiritRoots.WATER -> ModParticles.METHOD_QI_WATER;
            case SpiritRoots.FIRE -> ModParticles.METHOD_QI_FIRE;
            case SpiritRoots.EARTH -> ModParticles.METHOD_QI_EARTH;
            case SpiritRoots.THUNDER -> ModParticles.METHOD_QI_THUNDER;
            case SpiritRoots.ICE -> ModParticles.METHOD_QI_ICE;
            case SpiritRoots.WIND -> ModParticles.METHOD_QI_WIND;
            case SpiritRoots.YANG -> ModParticles.METHOD_QI_YANG;
            case SpiritRoots.YIN -> ModParticles.METHOD_QI_YIN;
            default -> ModParticles.METHOD_QI_NEUTRAL;
        };
    }
}
