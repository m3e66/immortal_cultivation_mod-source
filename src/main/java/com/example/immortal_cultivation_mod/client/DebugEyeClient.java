package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.client.hud.HudLayout;
import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics;
import com.example.immortal_cultivation_mod.network.DebugEyePayloads;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class DebugEyeClient {
    private static DebugEyePayloads.Snapshot latest;
    private static long receivedAt;
    public static DebugEyePayloads.Snapshot latest() { return latest; }
    public static void accept(DebugEyePayloads.Snapshot snapshot) {
        if (!ClientData.debugEyeEnabled()) return;
        latest = snapshot;
        receivedAt = System.nanoTime();
    }
    public static void clear() { latest = null; receivedAt = 0; }
    public static boolean stale() { return latest != null && System.nanoTime() - receivedAt > 3_000_000_000L; }
    public static Component hint() { return DebugEyeDiagnostics.text("hint", ModKeyMappings.DEBUG_EYE.get().getTranslatedKeyMessage()); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.setDebugEyeEnabled(false);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!ClientData.debugEyeEnabled() || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        var graphics = event.getGuiGraphics();
        int width = Math.min(325, graphics.guiWidth() - 16);
        int x = graphics.guiWidth() - width - 6, y = 6;
        var lines = new ArrayList<FormattedCharSequence>();
        lines.addAll(mc.font.split(DebugEyeDiagnostics.text("title"), width - 10));
        lines.addAll(mc.font.split(hint(), width - 10));
        if (latest == null) lines.addAll(mc.font.split(DebugEyeDiagnostics.text("waiting"), width - 10));
        else {
            if (stale()) lines.addAll(mc.font.split(DebugEyeDiagnostics.text("stale"), width - 10));
            for (Component line : latest.hud()) lines.addAll(mc.font.split(line, width - 10));
        }
        int count = Math.min(lines.size(), Math.max(3, (graphics.guiHeight() - 65) / 10));
        var position = HudLayout.place(graphics, HudElement.DEBUG_EYE, x, y, width, count * 10 + 8);
        x = position.x(); y = position.y();
        graphics.fill(x, y, x + width, y + count * 10 + 8, 0xD010151B);
        for (int i = 0; i < count; i++) graphics.drawString(mc.font, lines.get(i), x + 5, y + 4 + i * 10,
                i == 0 ? 0xFFE5C888 : i == 1 ? 0xFF9AAABA : 0xFFE0E5EA, false);
    }
}
