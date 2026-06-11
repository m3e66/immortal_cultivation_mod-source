package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WindStep {
    private static final int QI_PER_SECOND = 5;
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> USED_DOUBLE_JUMP = new ConcurrentHashMap<>();

    private WindStep() {
    }

    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE.remove(id) != null) {
            stop(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, QI_PER_SECOND)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ACTIVE.put(id, true);
        USED_DOUBLE_JUMP.remove(id);
        refreshEffects(player);
        ServerEvents.syncPlayerData(player);
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!ACTIVE.containsKey(id)) {
            return;
        }

        refreshEffects(player);
        if (player.onGround()) {
            USED_DOUBLE_JUMP.remove(id);
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, QI_PER_SECOND)) {
            stop(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ServerEvents.syncPlayerData(player);
    }

    public static void doubleJump(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!ACTIVE.containsKey(id) || USED_DOUBLE_JUMP.getOrDefault(id, false)) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, 0.65D, movement.z);
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.fallDistance = 0.0F;
        player.resetFallDistance();
        USED_DOUBLE_JUMP.put(id, true);
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        USED_DOUBLE_JUMP.remove(player.getUUID());
        player.removeEffect(ModEffects.WIND_STEP);
    }

    private static void stop(ServerPlayer player) {
        clear(player);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        ServerEvents.syncPlayerData(player);
    }

    private static void refreshEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.WIND_STEP, 40, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
    }
}
