package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.DebugTimePayloads;
import com.example.immortal_cultivation_mod.world.DebugTimeController;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** A forward-only, 24-hour radial clock inspired by Genshin's time selector. */
public final class DebugTimeScreen extends Screen {
    private static final int WINDOW_WIDTH = 270;
    private static final int WINDOW_HEIGHT = 244;
    private static final int CLOCK_RADIUS = 71;
    private static final int DAY_TICKS = DebugTimeController.DAY_TICKS;
    private final long openedDayTime;
    private int selectedTimeOfDay;
    private int left;
    private int top;
    private int clockX;
    private int clockY;
    private int speedMultiplier = 1;
    private SystemGuiButton speedButton;
    private boolean draggingClock;

    public DebugTimeScreen(long dayTime) {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".debug_time.title"));
        openedDayTime = dayTime;
        selectedTimeOfDay = Math.floorMod((int) dayTime + 1_000, DAY_TICKS);
    }

    public static void open(long dayTime) {
        Minecraft.getInstance().setScreen(new DebugTimeScreen(dayTime));
    }

    @Override
    protected void init() {
        left = (width - WINDOW_WIDTH) / 2;
        top = (height - WINDOW_HEIGHT) / 2;
        clockX = left + 92;
        clockY = top + 124;

        speedButton = addRenderableWidget(new SystemGuiButton(
                left + 53, top + 207, 78, 20,
                speedLabel(), this::cycleSpeed));
        addRenderableWidget(new SystemGuiButton(
                left + 174, top + 170, 78, 20,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".debug_time.confirm"),
                this::confirm));
        addRenderableWidget(new SystemGuiButton(
                left + 174, top + 198, 78, 20,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".debug_time.cancel"),
                this::onClose));
    }

    private void confirm() {
        PacketDistributor.sendToServer(
                new DebugTimePayloads.ServerboundStartFastForward(
                        selectedTimeOfDay, speedMultiplier));
        onClose();
    }

    private void cycleSpeed() {
        speedMultiplier = speedMultiplier
                % DebugTimeController.MAX_SPEED_MULTIPLIER + 1;
        speedButton.setMessage(speedLabel());
    }

    private Component speedLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID
                        + ".debug_time.speed",
                speedMultiplier);
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
        SystemGuiTextures.drawWindow(
                graphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT);
        graphics.drawCenteredString(font, title,
                left + WINDOW_WIDTH / 2, top + 13, 0xFFFFFFFF);

        drawClock(graphics);

        int textX = left + 174;
        graphics.drawString(font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".debug_time.current",
                        formatTime(Math.floorMod(openedDayTime, (long) DAY_TICKS))),
                textX, top + 63, 0xFF303030, false);
        graphics.drawString(font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".debug_time.target",
                        formatTime(selectedTimeOfDay)),
                textX, top + 86, 0xFF8A5B08, false);
        graphics.drawString(font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".debug_time.forward",
                        formatDuration(forwardDelta())),
                textX, top + 109, 0xFF303030, false);
        graphics.drawWordWrap(font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".debug_time.instructions"),
                textX, top + 133, 78, 0xFF4A4A4A);

        // Screen.render() reapplies Minecraft's framebuffer blur after this
        // foreground has been drawn. Render the widgets directly so the clock,
        // text, and window stay crisp.
        for (net.minecraft.client.gui.components.Renderable renderable
                : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawClock(GuiGraphics graphics) {
        fillCircle(graphics, clockX, clockY, CLOCK_RADIUS + 4, 0xFF4C3B25);
        fillCircle(graphics, clockX, clockY, CLOCK_RADIUS + 1, 0xFFE0C483);
        fillCircle(graphics, clockX, clockY, CLOCK_RADIUS - 4, 0xFF16243A);
        fillCircle(graphics, clockX, clockY, CLOCK_RADIUS - 10, 0xFF0B1424);

        for (int hour = 0; hour < 24; hour++) {
            double angle = hour * Math.PI * 2.0D / 24.0D;
            int radius = hour % 6 == 0 ? CLOCK_RADIUS - 16 : CLOCK_RADIUS - 13;
            int x = clockX + (int) Math.round(Math.sin(angle) * radius);
            int y = clockY - (int) Math.round(Math.cos(angle) * radius);
            int size = hour % 6 == 0 ? 3 : 2;
            int color = hour >= 6 && hour < 18 ? 0xFFFFD66B : 0xFF84A9E8;
            graphics.fill(x - size / 2, y - size / 2,
                    x + (size + 1) / 2, y + (size + 1) / 2, color);
        }

        graphics.drawCenteredString(font, "0", clockX, clockY - 52, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "6", clockX + 53, clockY - 4, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "12", clockX, clockY + 45, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "18", clockX - 53, clockY - 4, 0xFFFFFFFF);

        drawHand(graphics,
                Math.floorMod(openedDayTime, (long) DAY_TICKS),
                CLOCK_RADIUS - 23, 0xFF8B99AD, 1);
        drawHand(graphics, selectedTimeOfDay,
                CLOCK_RADIUS - 18, 0xFFFFCB52, 2);
        fillCircle(graphics, clockX, clockY, 5, 0xFF3A2914);
        fillCircle(graphics, clockX, clockY, 3, 0xFFFFD875);
    }

    private void drawHand(
            GuiGraphics graphics, long dayTime, int length, int color, int thickness) {
        int displayMinutes = Math.floorMod(
                (int) (dayTime * 1_440L / DAY_TICKS) + 360, 1_440);
        double angle = displayMinutes * Math.PI * 2.0D / 1_440.0D;
        int endX = clockX + (int) Math.round(Math.sin(angle) * length);
        int endY = clockY - (int) Math.round(Math.cos(angle) * length);
        drawLine(graphics, clockX, clockY, endX, endY, color, thickness);
        fillCircle(graphics, endX, endY, thickness + 2, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideClock(mouseX, mouseY)) {
            draggingClock = true;
            selectFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (draggingClock && button == 0) {
            selectFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingClock = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsideClock(double mouseX, double mouseY) {
        double dx = mouseX - clockX;
        double dy = mouseY - clockY;
        return dx * dx + dy * dy <= (CLOCK_RADIUS + 7) * (CLOCK_RADIUS + 7);
    }

    private void selectFromMouse(double mouseX, double mouseY) {
        double angle = Math.atan2(mouseX - clockX, -(mouseY - clockY));
        if (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }
        int displayMinutes = (int) Math.round(angle * 1_440.0D
                / (Math.PI * 2.0D));
        displayMinutes = Math.floorMod(
                (int) Math.round(displayMinutes / 6.0D) * 6, 1_440);
        selectedTimeOfDay = Math.floorMod(displayMinutes - 360, 1_440)
                / 6 * 100;
    }

    private long forwardDelta() {
        long current = Math.floorMod(openedDayTime, (long) DAY_TICKS);
        long delta = Math.floorMod(
                (long) selectedTimeOfDay - current, (long) DAY_TICKS);
        if (delta < DebugTimeController.MINIMUM_FORWARD_TICKS) {
            delta += DAY_TICKS;
        }
        return delta;
    }

    private static String formatTime(long dayTime) {
        int minutes = Math.floorMod(
                (int) (dayTime * 1_440L / DAY_TICKS) + 360, 1_440);
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private static String formatDuration(long ticks) {
        int minutes = Math.max(1, (int) Math.round(ticks * 1_440.0D / DAY_TICKS));
        return String.format(Locale.ROOT, "%dh %02dm", minutes / 60, minutes % 60);
    }

    private static void fillCircle(
            GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.floor(Math.sqrt(radius * radius - y * y));
            graphics.fill(centerX - halfWidth, centerY + y,
                    centerX + halfWidth + 1, centerY + y + 1, color);
        }
    }

    private static void drawLine(
            GuiGraphics graphics, int x0, int y0, int x1, int y1,
            int color, int thickness) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int stepX = x0 < x1 ? 1 : -1;
        int stepY = y0 < y1 ? 1 : -1;
        int error = dx - dy;
        while (true) {
            graphics.fill(x0 - thickness / 2, y0 - thickness / 2,
                    x0 + (thickness + 1) / 2,
                    y0 + (thickness + 1) / 2, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int doubled = error * 2;
            if (doubled > -dy) {
                error -= dy;
                x0 += stepX;
            }
            if (doubled < dx) {
                error += dx;
                y0 += stepY;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
