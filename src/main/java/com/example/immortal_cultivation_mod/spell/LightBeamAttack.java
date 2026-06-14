package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.LightBeamProjectileEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class LightBeamAttack {
    private static final int CAST_COST = 90;
    private static final int SHOOT_COST = 30;

    private LightBeamAttack() {
    }

    public static boolean cast(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        List<LightBeamProjectileEntity> waiting = waitingProjectiles(level, player);
        int currentStacks = waiting.size() / LightBeamProjectileEntity.PROJECTILES_PER_STACK;
        if (currentStacks >= LightBeamProjectileEntity.MAX_STACKS) {
            return false;
        }

        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, CAST_COST)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }

        for (int slot = 0; slot < LightBeamProjectileEntity.PROJECTILES_PER_STACK; slot++) {
            level.addFreshEntity(new LightBeamProjectileEntity(level, player, currentStacks, slot));
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean shoot(ServerPlayer player, Vec3 target, boolean shootAll) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        List<LightBeamProjectileEntity> waiting = waitingProjectiles(level, player);
        if (waiting.isEmpty()) {
            return false;
        }

        int shotsToTry = shootAll ? waiting.size() : 1;
        int shotsFired = 0;
        Vec3 direction = player.getLookAngle().normalize();
        for (int i = 0; i < shotsToTry; i++) {
            var data = ModAttachments.getData(player);
            if (!ServerEvents.spendQiOrBlood(player, data, SHOOT_COST)) {
                player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                break;
            }
            waiting.get(i).fireInDirection(direction);
            shotsFired++;
        }

        if (shotsFired > 0) {
            ServerEvents.syncPlayerData(player);
        }
        return shotsFired > 0;
    }

    public static boolean hasWaiting(Player player) {
        return !waitingProjectiles(player.level(), player).isEmpty();
    }

    private static List<LightBeamProjectileEntity> waitingProjectiles(Level level, Player player) {
        return level.getEntitiesOfClass(LightBeamProjectileEntity.class,
                        new AABB(player.blockPosition()).inflate(48.0D),
                        projectile -> projectile.isWaiting() && player.getUUID().equals(projectile.getCasterId()))
                .stream()
                .sorted(Comparator.comparingInt(entity -> entity.getId()))
                .toList();
    }
}
