package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.combat.ColdTideFreeze;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks spell hotkeys and other custom actions without blocking chat, login or keepalive packets. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ColdTidePlayerPayloadMixin {
    @Inject(method="handleCustomPayload",at=@At("HEAD"),cancellable=true)
    private void immortal$coldTidePayload(ServerboundCustomPayloadPacket packet,CallbackInfo ci) {
        if((Object)this instanceof ServerGamePacketListenerImpl listener) {
            PacketUtils.ensureRunningOnSameThread(packet,listener,listener.player.serverLevel());
            if(ColdTideFreeze.isFrozen(listener.player))ci.cancel();
        }
    }
}
