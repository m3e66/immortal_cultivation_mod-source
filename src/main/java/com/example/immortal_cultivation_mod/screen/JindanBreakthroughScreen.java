package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JindanBreakthroughScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/gui/breakthrough.png"
            );

    private static final int TEXTURE_W = 1404;
    private static final int TEXTURE_H = 1120;

    private static final int WINDOW_W = 430;
    private static final int WINDOW_H = 343;

    private static final int CORE_X = 701;
    private static final int CORE_Y = 543;
    private static final int CORE_SIZE = 130;

    private static final int PROGRESS_X = 450;
    private static final int PROGRESS_Y = 1014;
    private static final int PROGRESS_W = 504;
    private static final int PROGRESS_H = 20;

    private static final int START_BUTTON_X = 600;
    private static final int START_BUTTON_Y = 936;
    private static final int START_BUTTON_W = 204;
    private static final int START_BUTTON_H = 48;

    private static final int ATTEMPTS = 20;
    private static final int START_DELAY_TICKS = 20;
    private static final int SHRINK_TICKS = 60;

    private static final float MAX_SCALE = 2.0F;
    private static final float GOLDEN_SCALE = 1.0F;
    private static final float MIN_SCALE = 0.1F;

    private final List<Float> samples = new ArrayList<>();
    private final List<ScreenParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private int ticks;
    private int cycleTicks;
    private boolean preparing;
    private boolean started;
    private boolean sent;
    private Button startButton;

    public JindanBreakthroughScreen() {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".jindan_breakthrough"
        ));
    }

    @Override
    protected void init() {
        ticks = 0;
        cycleTicks = 0;
        preparing = false;
        started = false;
        sent = false;
        samples.clear();
        particles.clear();

        int x = left();
        int y = top();
        startButton = addRenderableWidget(Button.builder(
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".jindan_start"),
                button -> {
                    preparing = true;
                    ticks = 0;
                    cycleTicks = 0;
                    button.visible = false;
                    button.active = false;
                }
        ).bounds(
                x + px(START_BUTTON_X),
                y + py(START_BUTTON_Y),
                px(START_BUTTON_W),
                Math.max(20, py(START_BUTTON_H))
        ).build());
    }

    @Override
    public void tick() {
        if (!preparing && !started) {
            tickParticles();
            return;
        }

        ticks++;
        spawnBreakthroughParticles();
        tickParticles();

        if (!started) {
            if (ticks >= START_DELAY_TICKS) {
                started = true;
                preparing = false;
                cycleTicks = 0;
            }
            return;
        }

        if (samples.size() < ATTEMPTS) {
            cycleTicks++;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == InputConstants.KEY_SPACE) {
            if (started && !sent) {
                recordCurrentScale();
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void recordCurrentScale() {
        if (samples.size() >= ATTEMPTS) {
            return;
        }

        samples.add(currentScale());
        cycleTicks = 0;

        if (samples.size() >= ATTEMPTS) {
            finish();
        }
    }

    private void finish() {
        if (sent) {
            return;
        }

        sent = true;

        float sum = 0.0F;

        for (float sample : samples) {
            sum += sample;
        }

        float average = samples.isEmpty()
                ? GOLDEN_SCALE
                : sum / samples.size();

        PacketDistributor.sendToServer(
                new ModPayloads.ServerboundCompleteJindanChallengePayload(
                        average,
                        0,
                        0
                )
        );

        Minecraft.getInstance().setScreen(null);
    }

    private float currentScale() {
        if (!started) {
            return MAX_SCALE;
        }

        int shrinkTicks = Math.max(
                40,
                SHRINK_TICKS - samples.size()
        );

        float progress = Math.min(
                1.0F,
                cycleTicks / (float) shrinkTicks
        );

        return Math.max(
                MIN_SCALE,
                MAX_SCALE - (MAX_SCALE - MIN_SCALE) * progress
        );
    }

    @Override
    public void render(
            GuiGraphics g,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderTransparentBackground(g);

        int x = left();
        int y = top();

        g.blit(
                TEXTURE,
                x,
                y,
                WINDOW_W,
                WINDOW_H,
                0.0F,
                0.0F,
                TEXTURE_W,
                TEXTURE_H,
                TEXTURE_W,
                TEXTURE_H
        );

        int centerX = x + px(CORE_X);
        int centerY = y + py(CORE_Y);
        int coreSize = px(CORE_SIZE);

        if (preparing || started) {
            float scale = started
                    ? currentScale()
                    : MAX_SCALE;

            drawParticles(g);
            drawContestCircle(
                    g,
                    centerX,
                    centerY,
                    Math.max(1, Math.round(coreSize * scale))
            );
        }

        drawProgress(
                g,
                x + px(PROGRESS_X),
                y + py(PROGRESS_Y),
                px(PROGRESS_W),
                Math.max(4, py(PROGRESS_H))
        );

        if (preparing && !started) {
            Component waiting = Component.translatable(
                    "screen."
                            + ImmortalCultivationMod.MODID
                            + ".jindan_prepare"
            );

            g.drawString(
                    font,
                    waiting,
                    centerX - font.width(waiting) / 2,
                    centerY + coreSize / 2 + py(58),
                    0x8F6C24,
                    false
            );
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawContestCircle(GuiGraphics g, int centerX, int centerY, int size) {
        drawCircleLine(g, centerX, centerY, size / 2, 0xFFD18A00);
    }

    private void spawnBreakthroughParticles() {
        int centerX = left() + px(CORE_X);
        int centerY = top() + py(CORE_Y);
        float baseRadius = px(CORE_SIZE) * 0.65F;

        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            float radius = baseRadius * (0.6F + random.nextFloat() * 1.25F);
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            float speed = 0.35F + random.nextFloat() * 0.45F;
            float vx = (centerX - x) / Math.max(1.0F, radius) * speed;
            float vy = (centerY - y) / Math.max(1.0F, radius) * speed;
            particles.add(new ScreenParticle(
                    x,
                    y,
                    vx,
                    vy,
                    22 + random.nextInt(18),
                    1.0F + random.nextFloat() * 2.0F
            ));
        }

        if (particles.size() > 220) {
            particles.subList(0, particles.size() - 220).clear();
        }
    }

    private void tickParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            ScreenParticle particle = particles.get(i);
            particle.tick();
            if (particle.dead()) {
                particles.remove(i);
            }
        }
    }

    private void drawParticles(GuiGraphics g) {
        for (ScreenParticle particle : particles) {
            int alpha = Math.max(0, Math.min(210, Math.round(210.0F * particle.alpha())));
            int color = (alpha << 24) | 0xD18A00;
            int size = Math.max(1, Math.round(particle.size));
            g.fill(
                    Math.round(particle.x),
                    Math.round(particle.y),
                    Math.round(particle.x) + size,
                    Math.round(particle.y) + size,
                    color
            );
        }
    }

    private void drawCircleLine(GuiGraphics g, int centerX, int centerY, int radius, int color) {
        int segments = Math.max(72, radius * 4);

        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;

            int px = centerX + (int) Math.round(Math.cos(angle) * radius);
            int py = centerY + (int) Math.round(Math.sin(angle) * radius);

            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private void drawProgress(
            GuiGraphics g,
            int x,
            int y,
            int barW,
            int barH
    ) {
        g.fill(
                x - 2,
                y - 2,
                x + barW + 2,
                y + barH + 2,
                0x8059462A
        );

        g.fill(
                x,
                y,
                x + barW,
                y + barH,
                0x66382E20
        );

        int filled = Math.round(
                barW * (samples.size() / (float) ATTEMPTS)
        );

        g.fill(
                x,
                y,
                x + filled,
                y + barH,
                0xD8D69B34
        );

        for (int i = 1; i < ATTEMPTS; i++) {
            int progressX = x + Math.round(
                    barW * (i / (float) ATTEMPTS)
            );

            g.fill(
                    progressX,
                    y,
                    progressX + 1,
                    y + barH,
                    0x8059462A
            );
        }
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int top() {
        return (height - WINDOW_H) / 2;
    }

    private static int px(int sourceX) {
        return Math.round(
                sourceX * (WINDOW_W / (float) TEXTURE_W)
        );
    }

    private static int py(int sourceY) {
        return Math.round(
                sourceY * (WINDOW_H / (float) TEXTURE_H)
        );
    }

    @Override
    public void renderBackground(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static class ScreenParticle {
        private float x;
        private float y;
        private final float vx;
        private final float vy;
        private final int lifetime;
        private final float size;
        private int age;

        private ScreenParticle(float x, float y, float vx, float vy, int lifetime, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifetime = lifetime;
            this.size = size;
        }

        private void tick() {
            x += vx;
            y += vy;
            age++;
        }

        private float alpha() {
            return 1.0F - age / (float) lifetime;
        }

        private boolean dead() {
            return age >= lifetime;
        }
    }
}
