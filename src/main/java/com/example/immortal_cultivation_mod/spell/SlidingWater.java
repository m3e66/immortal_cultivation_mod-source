package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.effect.NativeSpellEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlidingWater {
    private static final int PUDDLE_DURATION_TICKS = 20 * 12;
    private static final int INVERTED_CONTROL_TICKS = 20 * 5;
    private static final ResourceLocation[] INVERTED_CONTROL_IDS = {
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "inverted_control"),
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "inverted_controls"),
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "invertedcontrol")
    };
    private static final Map<ResourceKey<Level>, Map<BlockPos, Puddle>> PUDDLES = new ConcurrentHashMap<>();

    private SlidingWater() {
    }

    public static void createPuddle(ServerLevel level, BlockPos pos) {
        createPuddle(level, pos, 1.0F, null);
    }

    public static void createPuddle(ServerLevel level, BlockPos pos, float chargeScale) {
        createPuddle(level, pos, chargeScale, null);
    }

    public static void createPuddle(
            ServerLevel level,
            BlockPos pos,
            float chargeScale,
            Entity owner) {
        int radius = Math.max(0, Math.round(Math.max(1.0F, Math.min(2.0F, chargeScale)) - 1.0F));
        Puddle puddle = new Puddle(
                level.getGameTime() + PUDDLE_DURATION_TICKS,
                owner == null ? null : owner.getUUID(),
                owner instanceof CultivationNpc);
        for (BlockPos puddlePos : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius), pos.offset(radius, 0, radius))) {
            PUDDLES.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>())
                    .put(puddlePos.immutable(), puddle);
            NativeSpellEffects.puddle(level, puddlePos.getX() + 0.5D, puddlePos.getY(), puddlePos.getZ() + 0.5D);
        }
    }

    /** Advances puddles once per dimension, including NPC-only combat with no players present. */
    public static void tick(ServerLevel level) {
        Map<BlockPos, Puddle> puddles = PUDDLES.get(level.dimension());
        if (puddles == null || puddles.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        for (var entry : puddles.entrySet()) {
            if (entry.getValue().expiresAt() <= now) {
                puddles.remove(entry.getKey());
                NativeSpellEffects.removePuddle(level, entry.getKey());
            }
        }
        if (puddles.isEmpty()) {
            PUDDLES.remove(level.dimension(), puddles);
            return;
        }

        if (now % 5L == 0L) {
            for (BlockPos pos : puddles.keySet()) {
                if (now % 20L == 0L) {
                    NativeSpellEffects.puddle(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                }
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
            Puddle puddle = puddles.get(pos);
            if (puddle == null) {
                continue;
            }
            LivingEntity owner = puddle.ownerId() == null
                    || !(level.getEntity(puddle.ownerId()) instanceof LivingEntity living)
                    ? null : living;
            var targets = level.getEntitiesOfClass(
                    LivingEntity.class,
                    puddleBox,
                    entity -> entity.isAlive()
                            && (!puddle.npcOwned()
                            || owner != null && NpcProfile.isCombatEnemy(owner, entity)));
            if (!targets.isEmpty()) {
                if (owner != null) {
                    HanlanNingyuanJing.applyDampFromWater(
                            owner, targets.getFirst(), 140);
                }
                applyInvertedControl(targets.getFirst());
                puddles.remove(pos);
                NativeSpellEffects.removePuddle(level, pos);
            }
        }
        if (puddles.isEmpty()) {
            PUDDLES.remove(level.dimension(), puddles);
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

    private record Puddle(long expiresAt, UUID ownerId, boolean npcOwned) {
    }
}
