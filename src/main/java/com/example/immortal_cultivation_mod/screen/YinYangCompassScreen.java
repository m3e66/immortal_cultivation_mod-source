package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class YinYangCompassScreen extends Screen {
    private static final int PANEL_COLOR = 0xD0101010;
    private static final int HIDDEN_DOT_COLOR = 0xFF101010;
    private static final float REVEAL_SPEED = 1.35F;
    private static final float REVEAL_DELAY_PER_DOT = 1.65F;
    private static final float FLIP_DURATION = 6.0F;

    private final int centerX;
    private final int centerZ;
    private final int step;
    private final int size;
    private final List<Integer> values;
    private final List<Integer> qiValues;
    private Mode mode = Mode.YIN_YANG;
    private Button modeButton;
    private int openTicks;

    public YinYangCompassScreen(int centerX, int centerZ, int step, int size, List<Integer> values, List<Integer> qiValues) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".yinyang_compass"));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.step = step;
        this.size = Math.max(1, size);
        this.values = values == null ? List.of() : List.copyOf(values);
        this.qiValues = qiValues == null ? List.of() : List.copyOf(qiValues);
        this.mode = ClientData.yinYangCompassQiMode() ? Mode.QI : Mode.YIN_YANG;
    }

    @Override
    protected void init() {
        int buttonWidth = 92;
        openTicks = 0;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
                    mode = mode == Mode.YIN_YANG ? Mode.QI : Mode.YIN_YANG;
                    ClientData.setYinYangCompassQiMode(mode == Mode.QI);
                    openTicks = 0;
                    button.setMessage(modeLabel());
                })
                .bounds(this.width / 2 - buttonWidth / 2, 18, buttonWidth, 18)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        openTicks++;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int spacing = 5;
        int dot = 3;
        int gridPixels = (size - 1) * spacing + dot;
        int left = (this.width - gridPixels) / 2;
        int top = (this.height - gridPixels) / 2 + 8;
        int panelPad = 14;

        guiGraphics.fill(left - panelPad, top - panelPad - 24, left + gridPixels + panelPad, top + gridPixels + panelPad + 18, PANEL_COLOR);
        guiGraphics.fill(left - panelPad, top - panelPad - 24, left + gridPixels + panelPad, top - panelPad - 23, mode == Mode.QI ? 0xFF1F6FE5 : 0xFF8A1B1B);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top - panelPad - 17, 0xFFECECEC);

        float animationTime = openTicks + partialTick;
        int centerDot = size / 2;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int value = valueAt(x, z);
                int color = colorFor(value);
                int px = left + x * spacing;
                int py = top + z * spacing;
                drawAnimatedDot(guiGraphics, px, py, dot, color, revealProgress(x, z, centerDot, animationTime));
            }
        }

        int cx = left + centerDot * spacing;
        int cz = top + centerDot * spacing;
        float centerProgress = revealProgress(centerDot, centerDot, centerDot, animationTime);
        guiGraphics.fill(cx - 1, cz - 1, cx + dot + 1, cz + dot + 1, centerProgress >= 0.5F ? 0xFF44AAFF : HIDDEN_DOT_COLOR);
        drawAnimatedDot(guiGraphics, cx, cz, dot, colorFor(valueAt(centerDot, centerDot)), centerProgress);

        int current = valueAt(centerDot, centerDot);
        String footer = Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + (mode == Mode.QI ? "spirit_qi_here" : "yin_qi_here"), current).getString()
                + "  "
                + Component.translatable("screen." + ImmortalCultivationMod.MODID + ".yin_qi_range", centerX, centerZ, step).getString();
        guiGraphics.drawCenteredString(this.font, footer, this.width / 2, top + gridPixels + panelPad + 5, 0xFFECECEC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float revealProgress(int x, int z, int centerDot, float animationTime) {
        float dx = x - centerDot;
        float dz = z - centerDot;
        float distance = Mth.sqrt(dx * dx + dz * dz);
        return Mth.clamp((animationTime * REVEAL_SPEED - distance * REVEAL_DELAY_PER_DOT) / FLIP_DURATION, 0.0F, 1.0F);
    }

    private void drawAnimatedDot(GuiGraphics guiGraphics, int x, int y, int size, int color, float progress) {
        if (progress <= 0.0F) {
            guiGraphics.fill(x, y, x + size, y + size, HIDDEN_DOT_COLOR);
            return;
        }
        if (progress >= 1.0F) {
            guiGraphics.fill(x, y, x + size, y + size, color);
            return;
        }
        float flip = Math.abs(progress - 0.5F) * 2.0F;
        int width = Math.max(1, Mth.ceil(size * flip));
        int offset = (size - width) / 2;
        int activeColor = progress < 0.5F ? HIDDEN_DOT_COLOR : color;
        guiGraphics.fill(x, y, x + size, y + size, HIDDEN_DOT_COLOR);
        guiGraphics.fill(x + offset, y, x + offset + width, y + size, activeColor);
    }

    private int valueAt(int x, int z) {
        int index = z * size + x;
        List<Integer> activeValues = mode == Mode.QI ? qiValues : values;
        if (index < 0 || index >= activeValues.size()) {
            return 0;
        }
        int min = mode == Mode.QI ? 0 : -15;
        return Mth.clamp(activeValues.get(index), min, 15);
    }

    private int colorFor(int value) {
        if (mode == Mode.QI) {
            int clamped = Mth.clamp(value, 0, 15);
            if (clamped == 0) {
                return 0xFFFFFFFF;
            }
            float t = 0.30F + 0.70F * clamped / 15.0F;
            int red = (int) (255.0F * (1.0F - t));
            int green = (int) (255.0F * (1.0F - t * 0.45F));
            return argb(255, red, green, 255);
        }
        int clamped = Mth.clamp(value, -15, 15);
        if (clamped == 0) {
            return 0xFFFFFFFF;
        }
        if (clamped > 0) {
            float t = 0.35F + 0.65F * clamped / 15.0F;
            int channel = (int) (255.0F * (1.0F - t));
            return argb(255, 255, channel, channel);
        }
        float t = 0.35F + 0.65F * -clamped / 15.0F;
        int red = (int) (255.0F - 35.0F * t);
        int green = (int) (255.0F - 70.0F * t);
        int blue = (int) (255.0F * (1.0F - t));
        return argb(255, red, green, blue);
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
    }

    private Component modeLabel() {
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + (mode == Mode.QI ? "mode_spirit_qi" : "mode_yin_yang_qi"));
    }

    private enum Mode {
        YIN_YANG,
        QI
    }
}
