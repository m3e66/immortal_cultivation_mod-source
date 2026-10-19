package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class DongtianJieling {
    private DongtianJieling() {
    }

    public static boolean cast(ServerPlayer player) {
        int chunks = JindanEntity.generatedChunkCount(player);
        if (chunks <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dongtian_jieling_no_chunks"), true);
            return false;
        }

        var data = ModAttachments.getData(player);
        int maxQi = Math.max(1, data.maxQi());
        if (data.qi() >= maxQi) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dongtian_jieling_full"), true);
            return false;
        }

        long calculated = Math.max(1L, (long) maxQi * chunks / 100L);
        int restored = (int) Math.min(maxQi - (long) data.qi(), calculated);
        if (restored <= 0) {
            return false;
        }

        ModAttachments.setData(player, data.withQi(data.qi() + restored));
        ServerEvents.syncPlayerData(player);
        spawnParticles(player.serverLevel(), player);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".dongtian_jieling_restored",
                restored,
                chunks), true);
        return true;
    }

    private static void spawnParticles(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                90, 0.9D, 1.1D, 0.9D, 0.18D);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                45, 0.65D, 0.9D, 0.65D, 0.045D);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                120, 1.1D, 0.7D, 1.1D, 0.2D);
    }
}
