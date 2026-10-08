package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Server-owned size preference. Damage and injected Qi cost do not change with this visual spread. */
public final class ExternalQiSize {
    private static final String KEY = "ExternalQiSizePercent";
    private static final java.util.Map<ServerPlayer, Long> LAST_SCROLL = new java.util.WeakHashMap<>();
    private ExternalQiSize() {}
    public static int clampPercent(int value) { return Math.clamp(value, 10, 200); }
    public static double scale(Player player) {
        return (player.getPersistentData().contains(KEY)
                ? clampPercent(player.getPersistentData().getInt(KEY)) : 100) / 100.0;
    }
    public static void adjust(ServerPlayer player, int steps) {
        if (steps == 0 || !player.isAlive() || player.isSpectator()
                || !QiInfusedWeapon.isWeapon(player.getMainHandItem())) return;
        long now = player.serverLevel().getGameTime();
        if (LAST_SCROLL.getOrDefault(player, Long.MIN_VALUE) == now) return;
        LAST_SCROLL.put(player, now);
        int percent = clampPercent((int) Math.round(scale(player) * 100) + Math.clamp(steps, -4, 4) * 10);
        player.getPersistentData().putInt(KEY, percent);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.external_qi_size", percent), true);
    }
}
