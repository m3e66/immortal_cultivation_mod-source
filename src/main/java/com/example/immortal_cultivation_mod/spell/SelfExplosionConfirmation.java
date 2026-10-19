package com.example.immortal_cultivation_mod.spell;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned, short-lived confirmation. Never saved or copied on respawn. */
public final class SelfExplosionConfirmation {
    private static final Map<ServerPlayer, Sequence> PENDING = new WeakHashMap<>();
    private SelfExplosionConfirmation() {}

    public static final class Sequence {
        private long firstTick = Long.MIN_VALUE;
        private long lastTick = Long.MIN_VALUE;
        private int presses;

        /** Returns remaining presses; repeated packets in the same tick cannot advance. */
        public int press(long tick) {
            if (presses == 0 || tick < firstTick || tick - firstTick > 100) {
                firstTick = tick;
                lastTick = Long.MIN_VALUE;
                presses = 0;
            }
            if (tick == lastTick) return 3 - presses;
            lastTick = tick;
            if (++presses < 3) return 3 - presses;
            presses = 0;
            return 0;
        }
    }

    public static boolean confirm(ServerPlayer player) {
        int remaining = PENDING.computeIfAbsent(player, ignored -> new Sequence())
                .press(player.serverLevel().getGameTime());
        if (remaining == 0) {
            PENDING.remove(player);
            return true;
        }
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.zibao_confirm", remaining), false);
        return false;
    }

    public static void cancel(ServerPlayer player) {
        PENDING.remove(player);
    }
}
