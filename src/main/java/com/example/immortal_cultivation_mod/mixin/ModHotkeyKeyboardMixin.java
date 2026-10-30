package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ModHotkeyGuard;
import com.example.immortal_cultivation_mod.client.ThoughtInputGuard;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
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
        if (window == Minecraft.getInstance().getWindow().getWindow())
            ThoughtInputGuard.onKeyEvent(InputConstants.getKey(key, scanCode), action);
    }
}
