package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.client.server.IntegratedPlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reincarnation Jade keeps inactive bodies in the integrated server as fake
 * ServerPlayers. An old body can legitimately keep the singleplayer owner's
 * account name, but vanilla's IntegratedPlayerList treats any same-name player
 * as a second login and rejects the real owner before Reincarnation Jade gets a
 * login event.
 *
 * Only hide a same-name Reincarnation Jade body from that one owner-name check.
 * The body itself remains loaded and untouched, and every other login check is
 * still handled by vanilla/NeoForge normally.
 */
@Mixin(IntegratedPlayerList.class)
public abstract class IntegratedPlayerListMixin {
    private static final String REINCARNATED_PLAYER_CLASS =
            "cn.reincarnationjade.entity.ReincarnatedPlayer";

    @Redirect(
            method = "canPlayerLogin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/server/IntegratedPlayerList;getPlayerByName(Ljava/lang/String;)Lnet/minecraft/server/level/ServerPlayer;"
            )
    )
    private ServerPlayer immortalCultivation$ignoreReincarnationBodyNameCollision(
            IntegratedPlayerList playerList, String name) {
        ServerPlayer existing = playerList.getPlayerByName(name);
        return immortalCultivation$isReincarnatedBody(existing) ? null : existing;
    }

    @Unique
    private static boolean immortalCultivation$isReincarnatedBody(ServerPlayer player) {
        if (player == null || !player.isFakePlayer()) {
            return false;
        }

        Class<?> type = player.getClass();
        while (type != null) {
            if (REINCARNATED_PLAYER_CLASS.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }
}
