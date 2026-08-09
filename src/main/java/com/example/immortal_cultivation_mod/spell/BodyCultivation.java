package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class BodyCultivation {
    public static final String BODY_TEMPERING_PILL_USES = "ImmortalCultivationBodyTemperingPillUses";
    private static final String LAST_COOLDOWN_TICKS = "ImmortalCultivationBodyTemperingLastCooldownTicks";
    private static final String CAST_PROGRESS_PREFIX = "ImmortalCultivationBodyTemperingProgress_";
    private static final String HP_GAINS_PREFIX = "ImmortalCultivationBodyTemperingGains_";
    private static final int MAX_PILL_USES = 10;
    private static final int MAX_GAINS_PER_REALM = 3;

    private BodyCultivation() {
    }

    public static boolean useBodyTemperingPill(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        int uses = tag.getInt(BODY_TEMPERING_PILL_USES);
        if (uses >= MAX_PILL_USES) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".body_tempering_pill_limit"), true);
            return false;
        }
        tag.putInt(BODY_TEMPERING_PILL_USES, uses + 1);
        ModAttachments.setData(player, ModAttachments.getData(player).withAddedPhysicalAttack(1));
        ServerEvents.syncPlayerData(player);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".body_tempering_pill_used", uses + 1, MAX_PILL_USES), true);
        return true;
    }

    public static boolean cast(ServerPlayer player) {
        int realm = Math.max(0, CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel()));
        CompoundTag tag = player.getPersistentData();
        String gainsKey = HP_GAINS_PREFIX + realm;
        int gains = tag.getInt(gainsKey);
        if (gains >= MAX_GAINS_PER_REALM) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".cuigu_realm_limit"), true);
            return false;
        }

        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float selfDamage = maxHealth * 0.05F;
        int required = requiredCasts(maxHealth);
        tag.putInt(LAST_COOLDOWN_TICKS, cooldownTicksForRequiredCasts(required));
        player.hurt(player.damageSources().magic(), selfDamage);

        String progressKey = CAST_PROGRESS_PREFIX + realm;
        int progress = tag.getInt(progressKey) + 1;
        if (progress >= required) {
            int hpGain = Math.max(1, (int) Math.ceil(maxHealth * 0.05D));
            tag.putInt(progressKey, 0);
            tag.putInt(gainsKey, gains + 1);
            ModAttachments.setData(player, ModAttachments.getData(player).withAddedMaxHpBonus(hpGain));
            ServerEvents.syncPlayerData(player);
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".cuigu_hp_gained", hpGain, gains + 1, MAX_GAINS_PER_REALM), true);
        } else {
            tag.putInt(progressKey, progress);
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".cuigu_progress", progress, required), true);
        }
        return true;
    }

    public static int cooldownTicks(ServerPlayer player) {
        int stored = player.getPersistentData().getInt(LAST_COOLDOWN_TICKS);
        return stored > 0 ? stored : cooldownTicksForRequiredCasts(requiredCasts(player.getMaxHealth()));
    }

    public static void copyPersistentData(Player original, Player copy) {
        CompoundTag source = original.getPersistentData();
        CompoundTag target = copy.getPersistentData();
        if (source.contains(BODY_TEMPERING_PILL_USES)) {
            target.putInt(BODY_TEMPERING_PILL_USES, source.getInt(BODY_TEMPERING_PILL_USES));
        }
        copyInt(source, target, LAST_COOLDOWN_TICKS);
        for (int realm = 0; realm <= 9; realm++) {
            copyInt(source, target, CAST_PROGRESS_PREFIX + realm);
            copyInt(source, target, HP_GAINS_PREFIX + realm);
        }
    }

    private static int requiredCasts(float maxHealth) {
        return Math.max(1, (int) Math.ceil((maxHealth * 0.05D) / 10.0D));
    }

    private static int cooldownTicksForRequiredCasts(int required) {
        long ticks = (long) required * 60L * 20L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, ticks));
    }

    private static void copyInt(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key)) {
            target.putInt(key, source.getInt(key));
        }
    }
}
