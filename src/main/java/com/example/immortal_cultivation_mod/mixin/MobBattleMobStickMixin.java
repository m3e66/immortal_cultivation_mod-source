package com.example.immortal_cultivation_mod.mixin;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.github.flemmli97.mobbattle.common.items.MobStick", remap = false)
public abstract class MobBattleMobStickMixin {
    @Redirect(
            method = "appendHoverText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 0
    )
    private MutableComponent immortalCultivation$unwrapOptionalTooltipArgument(String key, Object... arguments) {
        Object[] safeArguments = Arrays.stream(arguments)
                .map(argument -> argument instanceof Optional<?> optional
                        ? (optional.isPresent() ? optional.get() : "")
                        : argument)
                .toArray(Object[]::new);
        return Component.translatable(key, safeArguments);
    }
}
