package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.system.OuterSystemType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SystemBootScreen extends Screen {
    private static final int WINDOW_W = 460;
    private static final int WINDOW_H = 290;
    private static final int TITLE_HEIGHT = 35;
    private static final int CHARACTER_TICKS = 1;
    private static final int LINE_PAUSE_TICKS = 6;

    private final String systemType;
    private final List<String> lines;
    private int lineIndex;
    private int characterIndex;
    private int pauseTicks;
    private boolean complete;

    public SystemBootScreen(String systemType) {
        super(Component.translatable("screen.immortal_cultivation_mod.system.boot_title"));
        this.systemType = systemType;
        this.lines = bootLines(systemType);
    }

    @Override
    protected void init() {
        lineIndex = 0;
        characterIndex = 0;
        pauseTicks = 0;
        complete = false;
    }

    @Override
    public void tick() {
        if (complete) {
            return;
        }
        if (pauseTicks > 0) {
            pauseTicks--;
            return;
        }
        if (lineIndex >= lines.size()) {
            complete = true;
            return;
        }
        String line = lines.get(lineIndex);
        characterIndex = Math.min(line.length(), characterIndex + CHARACTER_TICKS);
        if (characterIndex >= line.length()) {
            lineIndex++;
            characterIndex = 0;
            pauseTicks = LINE_PAUSE_TICKS;
            if (lineIndex >= lines.size()) {
                complete = true;
            }
        }
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xEE020711);
        int windowW = Math.min(WINDOW_W, width - 24);
        int windowH = Math.min(WINDOW_H, height - 24);
        int x = (width - windowW) / 2;
        int y = (height - windowH) / 2;
        SystemGuiTextures.drawWindow(
                graphics, x, y, windowW, windowH);
        SystemGuiTextures.drawInsetPanel(
                graphics,
                x + 7,
                y + TITLE_HEIGHT,
                windowW - 14,
                windowH - TITLE_HEIGHT - 8,
                0xFF010713);
        graphics.fill(
                x + 9, y + TITLE_HEIGHT + 2,
                x + windowW - 9, y + windowH - 10,
                0xFF010713);
        graphics.drawString(
                font,
                Component.translatable("screen.immortal_cultivation_mod.system.boot_header"),
                x + 13,
                y + 13,
                0xFFFFFFFF,
                true);

        int textX = x + 18;
        int textTop = y + TITLE_HEIGHT + 14;
        int textBottom = y + windowH - 28;
        int maxVisible = Math.max(1, (textBottom - textTop) / 11);
        int finishedLines = Math.min(lineIndex, lines.size());
        int first = Math.max(0, finishedLines - maxVisible + 1);
        int drawY = textTop;
        for (int index = first; index < finishedLines; index++) {
            graphics.drawString(
                    font, lines.get(index), textX, drawY,
                    colorFor(lines.get(index)), false);
            drawY += 11;
        }
        if (!complete && lineIndex < lines.size() && drawY <= textBottom) {
            String line = lines.get(lineIndex);
            String visible = line.substring(
                    0, Math.min(characterIndex, line.length()));
            graphics.drawString(
                    font, visible + "_", textX, drawY,
                    colorFor(line), false);
        } else if (complete) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.immortal_cultivation_mod.system.continue"),
                    textX,
                    y + windowH - 23,
                    0xFF78D6FF,
                    false);
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX, double mouseY, int button) {
        int windowW = Math.min(WINDOW_W, width - 24);
        int windowH = Math.min(WINDOW_H, height - 24);
        int x = (width - windowW) / 2;
        int y = (height - windowH) / 2;
        if (complete
                && button == 0
                && SystemGuiTextures.isOverClose(
                        mouseX, mouseY, x, y, windowW)) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (complete && (keyCode == 256 || keyCode == 257 || keyCode == 32)) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return complete;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int colorFor(String line) {
        if (line.contains("WARN") || line.contains("\u8b66\u544a")) {
            return 0xFFFFCF54;
        }
        if (line.contains("SUCCESS") || line.contains("\u7ed1\u5b9a\u5b8c\u6210")
                || line.contains("OK") || line.contains("\u901a\u8fc7")) {
            return 0xFF64FF91;
        }
        return 0xFF72CFFF;
    }

    private static List<String> bootLines(String typeId) {
        boolean chinese = net.minecraft.client.Minecraft.getInstance()
                .getLanguageManager().getSelected().startsWith("zh");
        String typeName = Component.translatable(
                OuterSystemType.byId(typeId).translationKey()).getString();
        List<String> result = new ArrayList<>();
        if (chinese) {
            result.addAll(List.of(
                    "\u3010\u672a\u77e5\u534f\u8bae\u3011\u68c0\u6d4b\u5230\u53ef\u627f\u8f7d\u4e2a\u4f53\u3002",
                    "\u3010\u672a\u77e5\u534f\u8bae\u3011\u5f00\u59cb\u5efa\u7acb\u5929\u5916\u8fde\u63a5\u2026\u2026",
                    "",
                    "\u3010\u68c0\u5b9a\u4e2d\u3011\u626b\u63cf\u795e\u9b42\u6ce2\u52a8\u2026\u2026",
                    "\u3010\u68c0\u5b9a\u4e2d\u3011\u626b\u63cf\u547d\u683c\u7ed3\u6784\u2026\u2026",
                    "\u3010\u68c0\u5b9a\u4e2d\u3011\u626b\u63cf\u8089\u8eab\u627f\u8f7d\u2026\u2026",
                    "\u3010\u68c0\u5b9a\u4e2d\u3011\u626b\u63cf\u7075\u6c14\u56de\u8def\u2026\u2026",
                    "",
                    "\u3010\u901a\u8fc7\u3011\u5bbf\u4e3b\u6ee1\u8db3\u6700\u4f4e\u9002\u914d\u8981\u6c42\u3002",
                    "\u3010\u901a\u8fc7\u3011\u5141\u8bb8\u63a5\u5165\u5916\u5e8f\u5217\u3002",
                    "",
                    "\u3010\u8fde\u63a5\u4e2d\u3011\u6b63\u5728\u5f00\u542f\u5929\u5916\u68c0\u7d22\u2026\u2026",
                    "\u3010\u8fde\u63a5\u4e2d\u3011\u6b63\u5728\u8bfb\u53d6\u65e0\u4e3b\u7cfb\u7edf\u5e93\u2026\u2026",
                    "\u3010\u8fde\u63a5\u4e2d\u3011\u6b63\u5728\u63d0\u53d6\u53ef\u9002\u914d\u6a21\u5757\u2026\u2026",
                    "",
                    "\u3010\u8b66\u544a\u3011\u5f53\u524d\u4f4d\u9762\u6cd5\u5219\u51fa\u73b0\u8f7b\u5fae\u6392\u65a5\u3002",
                    "\u3010\u8b66\u544a\u3011\u68c0\u6d4b\u5230\u5f02\u5e38\u56e0\u679c\u6270\u52a8\u3002",
                    "\u3010\u8b66\u544a\u3011\u68c0\u6d4b\u5230\u672a\u77e5\u9ad8\u7ef4\u6ce8\u89c6\u3002",
                    "",
                    "\u3010\u62bd\u53d6\u4e2d\u3011\u5f00\u59cb\u968f\u673a\u62bd\u53d6\u7cfb\u7edf\u2026\u2026",
                    "\u3010\u62bd\u53d6\u4e2d\u3011\u6b63\u5728\u6f14\u7b97\u547d\u683c\u5206\u652f\u2026\u2026",
                    "\u3010\u62bd\u53d6\u4e2d\u3011\u6b63\u5728\u79fb\u9664\u51b2\u7a81\u7cfb\u7edf\u2026\u2026",
                    "\u3010\u62bd\u53d6\u4e2d\u3011\u6b63\u5728\u7a33\u5b9a\u6700\u7ec8\u7ed3\u679c\u2026\u2026",
                    "",
                    "\u3010\u9501\u5b9a\u4e2d\u3011\u7ed3\u679c\u5373\u5c06\u786e\u5b9a\u2026\u2026",
                    "\u3010\u9501\u5b9a\u4e2d\u3011\u5bbf\u4e3b\u552f\u4e00\u8bc6\u522b\u5b8c\u6210\u2026\u2026",
                    "\u3010\u9501\u5b9a\u4e2d\u3011\u7cfb\u7edf\u6743\u9650\u5199\u5165\u4e2d\u2026\u2026",
                    "",
                    "\u3010\u7ed1\u5b9a\u5b8c\u6210\u3011\u7cfb\u7edf\u63a5\u5165\u6210\u529f\u3002",
                    "\u3010\u7ed1\u5b9a\u5b8c\u6210\u3011\u4f60\u5df2\u83b7\u5f97\uff1a" + typeName));
        } else {
            result.addAll(List.of(
                    "[BOOT] Unknown protocol detected...",
                    "[BOOT] Establishing outer-link...",
                    "",
                    "[SCAN] Host soul signature detected.",
                    "[SCAN] Verifying body compatibility...",
                    "[SCAN] Verifying fate deviation...",
                    "[SCAN] Verifying spiritual vessel...",
                    "",
                    "[OK] Minimum conditions satisfied.",
                    "[OK] Host accepted.",
                    "",
                    "[LINK] Accessing outer sequence...",
                    "[LINK] Reading unbound system archive...",
                    "[LINK] Extracting candidate modules...",
                    "",
                    "[WARN] Local reality instability detected.",
                    "[WARN] Non-native authority entering current world.",
                    "",
                    "[ROLL] Beginning system randomization...",
                    "[ROLL] Locking host identity...",
                    "[ROLL] Calculating destiny branch...",
                    "[ROLL] Filtering incompatible modules...",
                    "[ROLL] Stabilizing sequence...",
                    "",
                    "[......]",
                    "[......]",
                    "[......]",
                    "",
                    "[RESULT] Candidate selected.",
                    "[RESULT] Preparing final bind...",
                    "[BIND] Writing system authority into host...",
                    "[BIND] Sealing connection...",
                    "[BIND] Preventing manual removal...",
                    "[BIND] Finalizing...",
                    "",
                    "[SUCCESS] System bound successfully.",
                    "[SUCCESS] Granted system: " + typeName));
        }
        return result;
    }
}
