package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SlidingWater {
    private static final int PUDDLE_DURATION_TICKS = 20 * 12;
    private static final int INVERTED_CONTROL_TICKS = 20 * 5;
    private static final ResourceLocation[] INVERTED_CONTROL_IDS = {
            ResourceLocation.fromNamespaceAndPath("magic_ritual_mod", "inverted_control"),
            ResourceLocation.fromNamespaceAndPath("magic_ritual_mod", "inverted_controls"),
            ResourceLocation.fromNamespaceAndPath("magic_ritual_mod", "invertedcontrol")
    };
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PUDDLES = new ConcurrentHashMap<>();

    private SlidingWater() {
    }

    public static void createPuddle(ServerLevel level, BlockPos pos) {
        createPuddle(level, pos, 1.0F);
    }

    public static void createPuddle(ServerLevel level, BlockPos pos, float chargeScale) {
        int radius = Math.max(0, Math.round(Math.max(1.0F, Math.min(2.0F, chargeScale)) - 1.0F));
        for (BlockPos puddlePos : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius), pos.offset(radius, 0, radius))) {
            PUDDLES.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>())
                    .put(puddlePos.immutable(), level.getGameTime() + PUDDLE_DURATION_TICKS);
            PhotonEffects.puddle(level, puddlePos.getX() + 0.5D, puddlePos.getY(), puddlePos.getZ() + 0.5D);
        }
    }

    public static void tick(ServerPlayer player) {
        Map<BlockPos, Long> puddles = PUDDLES.get(player.level().dimension());
        if (puddles == null || puddles.isEmpty()) {
            return;
        }

        long now = player.level().getGameTime();
        for (var entry : puddles.entrySet()) {
            if (entry.getValue() <= now) {
                puddles.remove(entry.getKey());
                PhotonEffects.removePuddle(player.serverLevel(), entry.getKey());
            }
        }
        if (puddles.isEmpty()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (player.tickCount % 5 == 0) {
            for (BlockPos pos : puddles.keySet()) {
                level.sendParticles(ParticleTypes.SPLASH,
                        pos.getX() + 0.5D, pos.getY() + 0.04D, pos.getZ() + 0.5D,
                        10, 0.35D, 0.01D, 0.35D, 0.02D);
                level.sendParticles(ParticleTypes.FALLING_WATER,
                        pos.getX() + 0.5D, pos.getY() + 0.08D, pos.getZ() + 0.5D,
                        4, 0.35D, 0.01D, 0.35D, 0.0D);
            }
        }

        for (BlockPos pos : puddles.keySet()) {
            AABB puddleBox = new AABB(pos).inflate(0.45D, 0.08D, 0.45D);
            var targets = level.getEntitiesOfClass(LivingEntity.class, puddleBox, entity -> entity.isAlive());
            if (!targets.isEmpty()) {
                applyInvertedControl(targets.getFirst());
                puddles.remove(pos);
                PhotonEffects.removePuddle(level, pos);
                return;
            }
        }
    }

    private static void applyInvertedControl(LivingEntity entity) {
        Optional<? extends net.minecraft.core.Holder<MobEffect>> effect = invertedControlEffect();
        effect.ifPresent(holder -> entity.addEffect(new MobEffectInstance(holder, INVERTED_CONTROL_TICKS, 0, false, false, true)));
    }

    private static Optional<? extends net.minecraft.core.Holder<MobEffect>> invertedControlEffect() {
        for (ResourceLocation id : INVERTED_CONTROL_IDS) {
            var key = ResourceKey.create(Registries.MOB_EFFECT, id);
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(key);
            if (holder.isPresent()) {
                return holder;
            }
        }
        return Optional.empty();
    }
}
