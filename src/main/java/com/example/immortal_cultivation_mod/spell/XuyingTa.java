package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.XuyingShadowEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class XuyingTa {
    private static final int COST = 50;
    private static final int REFRESH_TICKS = 30;
    private static final int SHADOW_INTERVAL_TICKS = 20;

    private XuyingTa() {
    }

    public static boolean toggle(ServerPlayer player) {
        if (player.hasEffect(ModEffects.XUYING_TA)) {
            player.removeEffect(ModEffects.XUYING_TA);
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            return true;
        }
        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, COST)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        player.addEffect(new MobEffectInstance(ModEffects.XUYING_TA, REFRESH_TICKS, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, REFRESH_TICKS, 1, false, false, true));
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.XUYING_TA)) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.XUYING_TA, REFRESH_TICKS, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, REFRESH_TICKS, 1, false, false, true));
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % SHADOW_INTERVAL_TICKS != 0) {
            return;
        }
        if (!player.isSprinting() && player.getDeltaMovement().horizontalDistanceSqr() < 0.08D) {
            return;
        }
        XuyingShadowEntity shadow = new XuyingShadowEntity(level, player);
        level.addFreshEntity(shadow);
    }
}
