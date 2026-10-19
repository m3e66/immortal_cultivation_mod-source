package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ModHotkeyGuard;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class ModHotkeyKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void immortalCultivation$rememberChord(long window, int key, int scanCode,
                                                  int action, int modifiers, CallbackInfo ci) {
        ModHotkeyGuard.onKey(key, action, modifiers);
    }
}
