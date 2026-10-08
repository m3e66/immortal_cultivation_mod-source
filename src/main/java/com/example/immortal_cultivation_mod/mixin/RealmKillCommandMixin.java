package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.combat.RealmKillProtection;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KillCommand.class)
public abstract class RealmKillCommandMixin {
    @Shadow private static int kill(CommandSourceStack source, Collection<? extends Entity> targets) {
        throw new AssertionError();
    }

    @Inject(method="kill",at=@At("HEAD"),cancellable=true)
    private static void immortalCultivation$protectHighRealms(CommandSourceStack source,
            Collection<? extends Entity> targets, CallbackInfoReturnable<Integer> result) {
        var allowed = targets.stream().filter(entity -> !RealmKillProtection.protects(entity)).toList();
        int protectedCount = targets.size() - allowed.size();
        if (protectedCount == 0) return;
        source.sendFailure(Component.translatable("commands.immortal_cultivation_mod.kill.realm_protected", protectedCount));
        // Re-enter only with unprotected targets; vanilla preserves success messages and counts.
        result.setReturnValue(allowed.isEmpty() ? 0 : kill(source, allowed));
    }
}
