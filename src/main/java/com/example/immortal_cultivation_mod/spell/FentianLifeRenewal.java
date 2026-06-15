package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FentianLifeRenewal {
    private static final int REVIVE_TICKS = 20 * 60;
    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> USED = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LAST_VALID_POS = new ConcurrentHashMap<>();

    private FentianLifeRenewal() {
    }

    public static boolean tryCatchDeath(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!CultivationMethods.isFentianLifeRenewal(ModAttachments.getData(player).activeCultivationMethod())
                || PENDING.containsKey(id)
                || USED.containsKey(id)) {
            return false;
        }

        USED.put(id, true);
        PENDING.put(id, REVIVE_TICKS);
        LAST_VALID_POS.put(id, player.position());
        player.setHealth(1.0F);
        player.setGameMode(GameType.SPECTATOR);
        return true;
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        Integer ticks = PENDING.get(id);
        if (ticks != null) {
            preventSpectatorTeleport(player, id);
            fieryTrail(player);
            if (ticks <= 0) {
                PENDING.remove(id);
                LAST_VALID_POS.remove(id);
                player.setGameMode(GameType.SURVIVAL);
                player.setHealth(2.0F);
                player.fallDistance = 0.0F;
                player.resetFallDistance();
            } else {
                PENDING.put(id, ticks - 1);
            }
            return;
        }

        if (USED.containsKey(id) && player.isAlive() && player.getHealth() >= player.getMaxHealth()) {
            USED.remove(id);
        }
    }

    public static void clear(ServerPlayer player) {
        UUID id = player.getUUID();
        PENDING.remove(id);
        LAST_VALID_POS.remove(id);
        if (player.isAlive() && player.getHealth() >= player.getMaxHealth()) {
            USED.remove(id);
        }
    }

    public static boolean isPending(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    private static void preventSpectatorTeleport(ServerPlayer player, UUID id) {
        Vec3 previous = LAST_VALID_POS.get(id);
        Vec3 current = player.position();
        if (previous != null && current.distanceToSqr(previous) > 100.0D) {
            player.teleportTo(previous.x, previous.y, previous.z);
            current = previous;
        }
        LAST_VALID_POS.put(id, current);
    }

    private static void fieryTrail(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 2 != 0) {
            return;
        }
        level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                10, 0.35D, 0.55D, 0.35D, 0.03D);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.5D, player.getZ(),
                5, 0.25D, 0.35D, 0.25D, 0.02D);
    }
}
