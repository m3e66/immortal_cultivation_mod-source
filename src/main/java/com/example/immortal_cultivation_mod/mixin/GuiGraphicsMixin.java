package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.screen.NpcDataScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    private static final String MOD_SCREEN_PACKAGE = "com.example.immortal_cultivation_mod.screen.";
    private static final String PATCHOULI_BOOK_SCREEN_PACKAGE = "vazkii.patchouli.client.book.gui.";

    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
            at = @At("HEAD"),
            argsOnly = true)
    private boolean immortalCultivation$applyStringShadow(boolean dropShadow, Font font,
                                                           String text, float x, float y,
                                                           int color) {
        return useDropShadow(dropShadow, color);
    }

    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I",
            at = @At("HEAD"),
            argsOnly = true)
    private boolean immortalCultivation$applyFormattedShadow(boolean dropShadow, Font font,
                                                              FormattedCharSequence text,
                                                              float x, float y, int color) {
        return useDropShadow(dropShadow, color);
    }

    private static boolean useDropShadow(boolean requested, int color) {
        Screen screen = Minecraft.getInstance().screen;
        if (isPatchouliBookScreen(screen)) {
            // Patchouli colors body text through the Component style while passing
            // white as the fallback color, so its explicit shadow flag must win.
            return requested && !isDarkText(color);
        }
        if (isWhiteText(color)) {
            return true;
        }
        return requested && (screen == null
                || screen instanceof NpcDataScreen
                || !screen.getClass().getName().startsWith(MOD_SCREEN_PACKAGE));
    }

    private static boolean isPatchouliBookScreen(Screen screen) {
        return screen != null
                && screen.getClass().getName().startsWith(PATCHOULI_BOOK_SCREEN_PACKAGE);
    }

    private static boolean isDarkText(int color) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        return red * 3 + green * 6 + blue < 1_280;
    }

    private static boolean isWhiteText(int color) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        return red >= 210 && green >= 210 && blue >= 210;
    }
}
