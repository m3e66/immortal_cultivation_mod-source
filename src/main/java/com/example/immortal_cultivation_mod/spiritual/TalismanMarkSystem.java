package com.example.immortal_cultivation_mod.spiritual;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.joml.Vector3f;

/** Persistent owner-specific tracking marks created by the 标记符. */
public final class TalismanMarkSystem {
    public static final double RANGE = 300.0D;

    private static final String TARGET_MARK_OWNERS =
            ImmortalCultivationMod.MODID + ":talisman_mark_owners";
    private static final String CASTER_MARK_TARGETS =
            ImmortalCultivationMod.MODID + ":talisman_mark_targets";
    private static final DustParticleOptions MARK_GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.34F), 0.72F);
    private static final DustParticleOptions MARK_CYAN =
            new DustParticleOptions(new Vector3f(0.48F, 0.94F, 1.0F), 0.58F);

    private TalismanMarkSystem() {
    }

    public static void mark(ServerPlayer caster, LivingEntity target) {
        CompoundTag owners = target.getPersistentData()
                .getCompound(TARGET_MARK_OWNERS);
        owners.putBoolean(caster.getUUID().toString(), true);
        target.getPersistentData().put(TARGET_MARK_OWNERS, owners);

        CompoundTag targets = caster.getPersistentData()
                .getCompound(CASTER_MARK_TARGETS);
        targets.putBoolean(target.getUUID().toString(), true);
        caster.getPersistentData().put(CASTER_MARK_TARGETS, targets);

        // Vanilla's persistence flag is serialized with the mob, so this also
        // survives save/reload instead of merely resetting its despawn timer.
        if (target instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        emitAppliedSeal(caster.serverLevel(), target);
    }

    public static boolean isMarkedBy(
            ServerPlayer caster, LivingEntity target) {
        return target.getPersistentData()
                .getCompound(TARGET_MARK_OWNERS)
                .getBoolean(caster.getUUID().toString());
    }

    public static List<UUID> targetsMarkedBy(ServerPlayer caster) {
        CompoundTag targets = caster.getPersistentData()
                .getCompound(CASTER_MARK_TARGETS);
        List<UUID> result = new ArrayList<>();
        for (String key : targets.getAllKeys()) {
            try {
                result.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                // Ignore legacy/corrupt keys without breaking the sense scan.
            }
        }
        return result;
    }

    public static void removeMark(
            ServerPlayer caster, LivingEntity target) {
        CompoundTag owners = target.getPersistentData()
                .getCompound(TARGET_MARK_OWNERS);
        owners.remove(caster.getUUID().toString());
        if (owners.isEmpty()) {
            target.getPersistentData().remove(TARGET_MARK_OWNERS);
        } else {
            target.getPersistentData().put(TARGET_MARK_OWNERS, owners);
        }

        CompoundTag targets = caster.getPersistentData()
                .getCompound(CASTER_MARK_TARGETS);
        targets.remove(target.getUUID().toString());
        if (targets.isEmpty()) {
            caster.getPersistentData().remove(CASTER_MARK_TARGETS);
        } else {
            caster.getPersistentData().put(CASTER_MARK_TARGETS, targets);
        }
        emitBrokenSeal(caster.serverLevel(), target);
    }

    private static void emitAppliedSeal(
            ServerLevel level, LivingEntity target) {
        double centerY = target.getY() + target.getBbHeight() * 0.78D;
        for (int index = 0; index < 24; index++) {
            double angle = Mth.TWO_PI * index / 24.0D;
            double radius = 0.42D;
            level.sendParticles(index % 3 == 0 ? MARK_CYAN : MARK_GOLD,
                    target.getX() + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius,
                    target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                target.getX(), centerY, target.getZ(),
                10, 0.20D, 0.25D, 0.20D, 0.015D);
        level.playSound(null, target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.75F, 1.55F);
    }

    private static void emitBrokenSeal(
            ServerLevel level, LivingEntity target) {
        double y = target.getY() + target.getBbHeight() + 0.25D;
        level.sendParticles(MARK_GOLD,
                target.getX(), y, target.getZ(),
                18, 0.32D, 0.32D, 0.32D, 0.055D);
        level.sendParticles(ParticleTypes.WAX_OFF,
                target.getX(), y, target.getZ(),
                8, 0.22D, 0.22D, 0.22D, 0.04D);
    }
}
