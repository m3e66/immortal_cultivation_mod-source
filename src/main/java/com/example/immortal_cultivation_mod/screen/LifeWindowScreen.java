package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared legacy desktop frame and matching input coordinates for life subwindows. */
abstract class LifeWindowScreen extends Screen {
    private static final int WINDOW_W = 620, WINDOW_H = 390, BODY_X = 14, BODY_Y = 42;
    private final SystemWindowState window = new SystemWindowState(WINDOW_W, WINDOW_H);

    protected LifeWindowScreen(Component title) { super(title); }
    protected final int canvasWidth() { return WINDOW_W - BODY_X * 2; }
    protected final int canvasHeight() { return WINDOW_H - BODY_Y - 8; }
    protected abstract void initContent();
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    protected boolean scrollContent(double x, double y, double horizontal, double vertical) { return false; }
    protected boolean clickContent(double x, double y, int button) { return false; }
    protected boolean dragContent(double x, double y, int button) { return false; }
    protected void releaseContent(double x, double y, int button) { }
    protected final void scissorCanvas(GuiGraphics graphics,int x,int y,int right,int bottom) {
        var matrix=graphics.pose().last().pose();
        var start=matrix.transformPosition(x,y,0,new org.joml.Vector3f());
        var end=matrix.transformPosition(right,bottom,0,new org.joml.Vector3f());
        graphics.enableScissor((int)Math.floor(start.x()),(int)Math.floor(start.y()),(int)Math.ceil(end.x()),(int)Math.ceil(end.y()));
    }

    @Override protected final void init() {
        window.initializeCentered(desktopWidth(), desktopHeight());
        if (!window.isMinimized()) initContent();
    }

    protected final SystemGuiButton button(int x, int y, int w, int h, Component label, Runnable action) {
        var layout = layout();
        var button = new SystemGuiButton(0, 0, 1, 1, label, action);
        button.setScaledBounds(layout.mapX(window.x() + BODY_X + x), layout.mapY(window.y() + BODY_Y + y),
                layout.mapWidth(w), layout.mapHeight(h), layout.scale());
        return addRenderableWidget(button);
    }

    protected final SystemGuiButton wrappedButton(int x, int y, int w, int h, Component label, Runnable action) {
        var button = button(x, y, w, h, label, action);
        button.setWrapped(true);
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
        return button;
    }

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB4000711);
        float scale = uiScale();
        int mx = (int)Math.floor(mouseX / scale), my = (int)Math.floor(mouseY / scale);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1);
        SystemGuiTextures.drawWindow(graphics, window.x(), window.y(), window.width(), window.renderedHeight());
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), Math.max(1, window.width() - 105)),
                window.x() + 13, window.y() + 13, 0xFFFFFFFF, true);
        if (!window.isMinimized()) {
            if (!window.isMaximized()) SystemGuiTextures.drawResizeGrip(graphics, window.x(), window.y(), window.width(), window.height());
            var layout = layout();
            graphics.pose().pushPose();
            graphics.pose().translate(layout.originX() + BODY_X * layout.scale(), layout.originY() + BODY_Y * layout.scale(), 0);
            graphics.pose().scale(layout.scale(), layout.scale(), 1);
            renderContent(graphics, (int)(layout.virtualX(mx) - window.x() - BODY_X),
                    (int)(layout.virtualY(my) - window.y() - BODY_Y), partialTick);
            graphics.pose().popPose();
            for (var renderable : renderables) renderable.render(graphics, mx, my, partialTick);
        }
        graphics.pose().popPose();
        SystemGuiButton.keepTooltipOnScreen(this);
    }

    @Override public final boolean mouseClicked(double x, double y, int button) {
        x /= uiScale(); y /= uiScale();
        if (!window.contains(x, y)) return false;
        if (button == 0 && SystemGuiTextures.isOverClose(x,y,window.x(),window.y(),window.width())) { onClose(); return true; }
        if (button == 0 && SystemGuiTextures.isOverMinimize(x,y,window.x(),window.y(),window.width())) {
            window.toggleMinimize(); rebuildWidgets(); return true;
        }
        if (button == 0 && SystemGuiTextures.isOverMaximize(x,y,window.x(),window.y(),window.width())) {
            window.toggleMaximize(desktopWidth(),desktopHeight()); rebuildWidgets(); return true;
        }
        if (window.beginDrag(x,y,button,desktopWidth(),desktopHeight())) return true;
        var layout=layout();
        if (!window.isMinimized() && clickContent(layout.virtualX(x)-window.x()-BODY_X,layout.virtualY(y)-window.y()-BODY_Y,button)) return true;
        return !window.isMinimized() && super.mouseClicked(x,y,button);
    }

    @Override public final boolean mouseReleased(double x, double y, int button) {
        if (window.isDragging()) { window.endDrag(); return true; }
        var layout=layout();
        releaseContent(layout.virtualX(x/uiScale())-window.x()-BODY_X,layout.virtualY(y/uiScale())-window.y()-BODY_Y,button);
        return super.mouseReleased(x/uiScale(),y/uiScale(),button);
    }

    @Override public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (window.dragTo(x/uiScale(),y/uiScale(),desktopWidth(),desktopHeight())) { rebuildWidgets(); return true; }
        var layout=layout();
        if (dragContent(layout.virtualX(x/uiScale())-window.x()-BODY_X,layout.virtualY(y/uiScale())-window.y()-BODY_Y,button)) return true;
        return super.mouseDragged(x/uiScale(),y/uiScale(),button,dx/uiScale(),dy/uiScale());
    }

    @Override public final boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        double mx=x/uiScale(),my=y/uiScale();
        if (window.isMinimized() || !window.contains(mx,my)) return false;
        var layout=layout();
        return scrollContent(layout.virtualX(mx)-window.x()-BODY_X,layout.virtualY(my)-window.y()-BODY_Y,horizontal,vertical);
    }

    @Override public void removed() {
        if (!OuterSystemClient.isTransitioning() && minecraft != null && minecraft.getConnection() != null)
            PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundClose());
    }
    @Override public final boolean isPauseScreen() { return false; }
    private SystemWindowState.ContentLayout layout() { return window.contentLayout(WINDOW_W,WINDOW_H); }
    private float uiScale() { return Math.max(.1F,Math.min(.82F,Math.min((width-12F)/WINDOW_W,(height-12F)/WINDOW_H))); }
    private int desktopWidth() { return Math.max(WINDOW_W,(int)Math.floor(width/uiScale())); }
    private int desktopHeight() { return Math.max(WINDOW_H,(int)Math.floor(height/uiScale())); }
}
