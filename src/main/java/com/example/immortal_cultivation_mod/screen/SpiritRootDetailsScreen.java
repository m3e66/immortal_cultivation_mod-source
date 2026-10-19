package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Two read-only root charts, using saved roots and grade rather than invented individual rolls. */
public final class SpiritRootDetailsScreen extends FittedMenuScreen {
    private static final int WINDOW_W = 460, WINDOW_H = 326;
    private static final float RADIUS = 56;
    private static final List<String> FIVE_ELEMENTS = List.of(SpiritRoots.METAL, SpiritRoots.EARTH,
            SpiritRoots.WATER, SpiritRoots.WOOD, SpiritRoots.FIRE);
    private static final List<String> SPECIAL_ELEMENTS = List.of(SpiritRoots.THUNDER, SpiritRoots.WIND,
            SpiritRoots.ICE, SpiritRoots.YANG, SpiritRoots.YIN);
    private static final int[] FIVE_COLORS = {0xFF96751D, 0xFF86653E, 0xFF2D78AA, 0xFF348350, 0xFFBB463F};
    private static final int[] SPECIAL_COLORS = {0xFF7850A0, 0xFF288B79, 0xFF397F97, 0xFFA47A22, 0xFF615A83};
    private final Screen parent;

    public SpiritRootDetailsScreen(Screen parent) {
        super(text("root_details"), WINDOW_W + 12, WINDOW_H + 12);
        this.parent = parent;
    }

    private static Component text(String suffix) {
        return Component.translatable("screen.immortal_cultivation_mod." + suffix);
    }

    @Override
    protected void initMenu() {
        addRenderableWidget(new CultivationGuiButton((width - 110) / 2,
                (height - WINDOW_H) / 2 + 297, 110, 20,
                Component.translatable("gui.back"), this::onClose));
    }

    @Override
    protected void renderMenu(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2, y = (height - WINDOW_H) / 2;
        g.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(g, x, y, WINDOW_W, WINDOW_H);
        g.drawCenteredString(font, title, x + WINDOW_W / 2, y + 17, 0xFF38352E);
        var data = ClientData.cultivationData;
        if (data != null) {
            g.drawCenteredString(font, SpiritRoots.displayName(data.spiritRoots(), data.spiritRootGrade()),
                    x + WINDOW_W / 2, y + 36, 0xFF4F645B);
            chart(g, x + 16, y + 58, FIVE_ELEMENTS, FIVE_COLORS, text("root_chart.five"),
                    data.spiritRoots(), data.spiritRootGrade());
            chart(g, x + 238, y + 58, SPECIAL_ELEMENTS, SPECIAL_COLORS, text("root_chart.special"),
                    data.spiritRoots(), data.spiritRootGrade());
            g.drawCenteredString(font, text("root_chart.explanation"), x + WINDOW_W / 2, y + 277, 0xFF625A4A);
        } else {
            g.drawCenteredString(font, text("no_data"), x + WINDOW_W / 2, y + 150, 0xFFAA3333);
        }
        super.renderMenu(g, mouseX, mouseY, partialTick);
    }

    private void chart(GuiGraphics g, int x, int y, List<String> elements, int[] colors,
                       Component heading, List<String> roots, String grade) {
        g.drawCenteredString(font, heading, x + 103, y + 8, 0xFF64573E);
        float cx = x + 103, cy = y + 108;
        float[] px = new float[5], py = new float[5];
        for (int i = 0; i < 5; i++) {
            double angle = -Math.PI / 2 + i * Math.PI * 2 / 5;
            float strength = SpiritRoots.elementStrength(roots, grade, elements.get(i));
            px[i] = cx + (float)Math.cos(angle) * RADIUS * strength;
            py[i] = cy + (float)Math.sin(angle) * RADIUS * strength;
        }

        // One small triangle batch per chart; no raster assets or per-frame texture uploads.
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = g.pose().last().pose();
        for (int i = 0; i < 96; i++) {
            double a = i * Math.PI * 2 / 96, b = (i + 1) * Math.PI * 2 / 96;
            line(buffer, matrix, cx + (float)Math.cos(a) * 64, cy + (float)Math.sin(a) * 64,
                    cx + (float)Math.cos(b) * 64, cy + (float)Math.sin(b) * 64, 1.2F, 0x997F8658);
        }
        for (int i = 0; i < 5; i++) {
            int next = (i + 1) % 5;
            double a = -Math.PI / 2 + i * Math.PI * 2 / 5;
            double b = -Math.PI / 2 + next * Math.PI * 2 / 5;
            float ax = (float)Math.cos(a), ay = (float)Math.sin(a);
            float bx = (float)Math.cos(b), by = (float)Math.sin(b);
            line(buffer, matrix, cx, cy, cx + ax * RADIUS, cy + ay * RADIUS, .7F, 0x604B7884);
            for (int ring = 1; ring <= 4; ring++) {
                float r = RADIUS * ring / 4;
                line(buffer, matrix, cx + ax * r, cy + ay * r, cx + bx * r, cy + by * r, .7F, 0x604B7884);
            }
            vertex(buffer, matrix, cx, cy, 0x183EA3C7);
            vertex(buffer, matrix, px[i], py[i], 0x50468CB1);
            vertex(buffer, matrix, px[next], py[next], 0x50468CB1);
            line(buffer, matrix, px[i], py[i], px[next], py[next], 1.2F, 0xB388B9CF);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        boolean missing = false;
        for (int i = 0; i < 5; i++) {
            boolean present = SpiritRoots.elementStrength(roots, grade, elements.get(i)) > 0;
            if (present) {
                int dotX = Math.round(px[i]), dotY = Math.round(py[i]);
                g.fill(dotX - 5, dotY - 5, dotX + 5, dotY + 5, (colors[i] & 0xFFFFFF) | 0x18000000);
                g.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, (colors[i] & 0xFFFFFF) | 0x60000000);
                g.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, colors[i]);
                g.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, 0xFFEAF5EE);
            } else missing = true;
            double a = -Math.PI / 2 + i * Math.PI * 2 / 5;
            int lx = Math.round(cx + (float)Math.cos(a) * 77);
            int ly = Math.round(cy + (float)Math.sin(a) * 77) - 8;
            g.drawCenteredString(font, SpiritRoots.elementDisplayName(elements.get(i)), lx, ly,
                    present ? colors[i] : 0xFF8A8980);
            g.drawCenteredString(font, present ? SpiritRoots.gradeDisplayName(grade) : text("root_chart.absent"),
                    lx, ly + 11, present ? 0xFF5F5748 : 0xFF8A8980);
        }
        if (missing) g.fill((int)cx - 1, (int)cy - 1, (int)cx + 2, (int)cy + 2, 0xFF85999C);
    }

    private static void vertex(BufferBuilder b, Matrix4f matrix, float x, float y, int color) {
        b.addVertex(matrix, x, y, 0).setColor(color);
    }

    private static void line(BufferBuilder b, Matrix4f matrix, float x1, float y1, float x2, float y2,
                             float width, int color) {
        float dx = x2 - x1, dy = y2 - y1, length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < .001F) return;
        float ox = -dy / length * width / 2, oy = dx / length * width / 2;
        vertex(b, matrix, x1 + ox, y1 + oy, color);
        vertex(b, matrix, x2 + ox, y2 + oy, color);
        vertex(b, matrix, x2 - ox, y2 - oy, color);
        vertex(b, matrix, x1 + ox, y1 + oy, color);
        vertex(b, matrix, x2 - ox, y2 - oy, color);
        vertex(b, matrix, x1 - ox, y1 - oy, color);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
