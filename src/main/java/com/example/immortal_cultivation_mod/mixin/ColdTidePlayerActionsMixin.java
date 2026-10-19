package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.combat.ColdTideFreeze;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla queues these handlers onto the server thread; frozen input cannot move or act there. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ColdTidePlayerActionsMixin {
    @Inject(method={"handleMovePlayer","handleMoveVehicle","handleUseItem","handleUseItemOn","handleInteract","handlePlayerAction"},at=@At("HEAD"),cancellable=true)
    private void immortal$coldTideActions(CallbackInfo ci) {
        var player=((ServerGamePacketListenerImpl)(Object)this).player;
        if(player.getServer().isSameThread()&&ColdTideFreeze.isFrozen(player))ci.cancel();
    }
}
