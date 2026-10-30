package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.example.immortal_cultivation_mod.ring.RingAiConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.LinkedHashMap;
import java.util.Map;

public class CultivationSettingsScreen extends FittedMenuScreen {
    private static final String TRANSLATION_PREFIX =
            "screen." + ImmortalCultivationMod.MODID + ".settings.";
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 385;
    private static final int BUTTON_HEIGHT = 22;
    private static final int MIN_BUTTON_HEIGHT = 18;
    private static final int ROW_HEIGHT = 26;
    private static final int ROW_GAP = 2;
    // Vertical edges of the inner frame in cultivation_background.png.
    private static final int CONTENT_TOP_TEXTURE_Y = 198;
    private static final int CONTENT_BOTTOM_TEXTURE_Y = 949;
    private int scroll;
    private boolean showModSky;
    private final Map<AbstractWidget, Integer> rowWidgets = new LinkedHashMap<>();
    private Component status = Component.empty();
    private CultivationGuiButton autoChannelButton;
    private CultivationGuiButton outwardQiButton;
    private CultivationGuiButton outwardQiAutoFitButton;
    private CultivationGuiButton outwardQiSizeScrollButton;
    private CultivationGuiButton creativeFlightButton;
    private CultivationGuiButton combatStateButton;
    private CultivationGuiButton autoCombatOnHitButton;
    private CultivationGuiButton chinesePoemButton;
    private CultivationGuiButton formationPlatePaymentButton;
    private CultivationGuiButton qiUseParticlesButton;
    private CultivationGuiButton spiritualSenseQiCurrentsButton;
    private CultivationGuiButton droughtDomainButton;

    public CultivationSettingsScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings"), PANEL_WIDTH + 20, PANEL_HEIGHT + 20);
    }

    @Override
    protected void initMenu() {
        rowWidgets.clear();
        showModSky = ShaderPackCompatibility.isShaderModPresent();
        boolean showCreativeFlight = showCreativeFlightOption();
        int left = panelX();
        int top = panelY();
        int buttonX = left + 50;
        int buttonWidth = panelWidth() - (buttonX - left) * 2;
        int rowButtonHeight = buttonHeight(showCreativeFlight);

        addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(0, showCreativeFlight), buttonWidth, rowButtonHeight,
                Component.translatable(TRANSLATION_PREFIX + "block_breaking"),
                () -> minecraft.setScreen(new BlockBreakingSettingsScreen(this))))
                .setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable(TRANSLATION_PREFIX + "block_breaking.description")));
        autoChannelButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(1, showCreativeFlight), buttonWidth, rowButtonHeight,
                autoChannelLabel(), () -> {
                    ClientData.setAutoChannelWeaponQi(!ClientData.autoChannelWeaponQi());
                    autoChannelButton.setMessage(autoChannelLabel());
                }));
        outwardQiButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(2, showCreativeFlight), buttonWidth, rowButtonHeight,
                outwardQiLabel(), () -> {
                    ClientData.setOutwardQiRelease(!ClientData.outwardQiRelease());
                    outwardQiButton.setMessage(outwardQiLabel());
                }));
        outwardQiAutoFitButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(3, showCreativeFlight), buttonWidth, rowButtonHeight,
                outwardQiAutoFitLabel(), () -> {
                    ClientData.setOutwardQiAutoFit(!ClientData.outwardQiAutoFit());
                    outwardQiAutoFitButton.setMessage(outwardQiAutoFitLabel());
                }));
        outwardQiAutoFitButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(TRANSLATION_PREFIX + "outward_qi_auto_fit.description")));
        outwardQiSizeScrollButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(4, showCreativeFlight), buttonWidth, rowButtonHeight,
                outwardQiSizeScrollLabel(), () -> {
                    ModClientConfig.setExternalQiSizeMouseWheel(!ModClientConfig.enableExternalQiSizeMouseWheel());
                    outwardQiSizeScrollButton.setMessage(outwardQiSizeScrollLabel());
                }));
        outwardQiSizeScrollButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(TRANSLATION_PREFIX + "outward_qi_size_scroll.description")));
        combatStateButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(5, showCreativeFlight), buttonWidth, rowButtonHeight,
                combatStateLabel(), () -> {
                    ClientData.setCombatStateEnabled(!ClientData.combatStateEnabled());
                    combatStateButton.setMessage(combatStateLabel());
                }));
        autoCombatOnHitButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(6, showCreativeFlight), buttonWidth, rowButtonHeight,
                autoCombatOnHitLabel(), () -> {
                    ClientData.setAutoCombatOnHit(!ClientData.autoCombatOnHit());
                    autoCombatOnHitButton.setMessage(autoCombatOnHitLabel());
                }));
        autoCombatOnHitButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(TRANSLATION_PREFIX + "auto_combat_on_hit.description")));
        chinesePoemButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(7, showCreativeFlight), buttonWidth, rowButtonHeight,
                chinesePoemLabel(), () -> {
                    ClientData.setChinesePoemDescriptions(!ClientData.chinesePoemDescriptions());
                    chinesePoemButton.setMessage(chinesePoemLabel());
                }));
        formationPlatePaymentButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(8, showCreativeFlight), buttonWidth, rowButtonHeight,
                formationPlatePaymentLabel(), () -> {
                    ClientData.setFormationPlateQiFirst(!ClientData.formationPlateQiFirst());
                    formationPlatePaymentButton.setMessage(formationPlatePaymentLabel());
                }));
        qiUseParticlesButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(9, showCreativeFlight), buttonWidth, rowButtonHeight,
                qiUseParticlesLabel(), () -> {
                    ClientData.setShowQiUseParticles(!ClientData.showQiUseParticles());
                    qiUseParticlesButton.setMessage(qiUseParticlesLabel());
                }));
        spiritualSenseQiCurrentsButton = addRenderableWidget(
                new CultivationGuiButton(
                        buttonX, buttonY(10, showCreativeFlight),
                        buttonWidth, rowButtonHeight,
                        spiritualSenseQiCurrentsLabel(), () -> {
                            ClientData.setShowSpiritualSenseQiCurrents(
                                    !ClientData.showSpiritualSenseQiCurrents());
                            spiritualSenseQiCurrentsButton.setMessage(
                                    spiritualSenseQiCurrentsLabel());
                        }));
        droughtDomainButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(11, showCreativeFlight), buttonWidth, rowButtonHeight,
                droughtDomainLabel(), () -> {
                    ClientData.setDroughtZombieDomainEnabled(
                            !ClientData.droughtZombieDomainEnabled());
                    droughtDomainButton.setMessage(droughtDomainLabel());
                }));
        creativeFlightButton = null;
        if (showCreativeFlight) {
            creativeFlightButton = addRenderableWidget(new CultivationGuiButton(
                    buttonX, buttonY(12, true), buttonWidth, rowButtonHeight,
                    creativeFlightLabel(), () -> {
                        ClientData.setJindanCreativeFlight(!ClientData.jindanCreativeFlight());
                        creativeFlightButton.setMessage(creativeFlightLabel());
                    }));
        }
        int next = showCreativeFlight ? 13 : 12;
        var recallButton=addRenderableWidget(new CultivationGuiButton(buttonX,buttonY(next,showCreativeFlight),buttonWidth,rowButtonHeight,
                swordRecallLabel(),()->{ClientData.setFlyingSwordAutoRecall(!ClientData.flyingSwordAutoRecall());rebuildWidgets();}));
        recallButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(TRANSLATION_PREFIX+"flying_sword_auto_recall.description")));
        next++;
        var aiButton = addRenderableWidget(new CultivationGuiButton(buttonX, buttonY(next, showCreativeFlight),
                buttonWidth, rowButtonHeight, aiLabel(), () -> {
                    if (!RingAiSettingsScreen.hostMayConfigure()) return;
                    var old = RingAiConfig.get();
                    try {
                        RingAiConfig.save(new RingAiConfig.Settings(!old.enabled(), old.endpoint(), old.model(), old.apiKey(), old.protocol()));
                        rebuildWidgets();
                    } catch (Exception ignored) { status = Component.translatable("ring.ai.config_error"); }
                }));
        aiButton.active = RingAiSettingsScreen.hostMayConfigure();
        var apiButton = addRenderableWidget(new CultivationGuiButton(buttonX, buttonY(next + 1, showCreativeFlight),
                buttonWidth, rowButtonHeight, Component.translatable("ring.ai.settings"),
                () -> minecraft.setScreen(new RingAiSettingsScreen(this))));
        apiButton.active = RingAiSettingsScreen.hostMayConfigure();
        addRenderableWidget(new CultivationGuiButton(buttonX, buttonY(next + 2, showCreativeFlight),
                buttonWidth, rowButtonHeight, Component.translatable("toxicity.config.title"),
                () -> minecraft.setScreen(new PillToxicitySettingsScreen(this))))
                .setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("toxicity.config.description")));
        addRenderableWidget(new CultivationGuiButton(buttonX, buttonY(next + 3, showCreativeFlight),
                buttonWidth, rowButtonHeight, Component.translatable("hud.layout.title"),
                () -> minecraft.setScreen(new HudLayoutScreen(this))));
        addRenderableWidget(new CultivationGuiButton(buttonX, buttonY(next + 4, showCreativeFlight),
                buttonWidth, rowButtonHeight, Component.translatable("qi.vfx.settings"),
                () -> minecraft.setScreen(new BlastQiSettingsScreen(this))));
        if (showModSky) {
            addRenderableWidget(new CultivationGuiButton(buttonX, buttonY(next + 5, showCreativeFlight),
                    buttonWidth, rowButtonHeight, modSkyLabel(), () -> {
                        ModClientConfig.setModSky(!ModClientConfig.enableModSky());
                        rebuildWidgets();
                    })).setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable(TRANSLATION_PREFIX + "mod_sky.description")));
        }
        for (var child : children()) if (child instanceof AbstractWidget widget) {
            if (widget instanceof CultivationGuiButton button) button.setTextScale(1.0F);
            rowWidgets.put(widget, widget.getY() + scroll);
        }
        addRenderableWidget(new CultivationGuiButton(
                left + panelWidth() / 2 - 55, top + panelHeight() - 29, 110, BUTTON_HEIGHT,
                Component.translatable(TRANSLATION_PREFIX + "done"), this::onClose).setTextScale(1.0F));
        updateScroll();
    }

    private Component autoChannelLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.auto_channel",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.autoChannelWeaponQi() ? "on" : "off")));
    }

    private Component creativeFlightLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.jindan_flight",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.jindanCreativeFlight() ? "on" : "off")));
    }

    private Component outwardQiLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.outward_qi",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.outwardQiRelease() ? "on" : "off")));
    }

    private Component outwardQiAutoFitLabel() {
        return Component.translatable(TRANSLATION_PREFIX + "outward_qi_auto_fit",
                Component.translatable(TRANSLATION_PREFIX + (ClientData.outwardQiAutoFit() ? "on" : "off")));
    }

    private Component outwardQiSizeScrollLabel() {
        return Component.translatable(TRANSLATION_PREFIX + "outward_qi_size_scroll",
                Component.translatable(TRANSLATION_PREFIX + (ModClientConfig.enableExternalQiSizeMouseWheel() ? "on" : "off")));
    }

    private Component combatStateLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.combat_state",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.combatStateEnabled() ? "on" : "off")));
    }

    private Component autoCombatOnHitLabel() {
        return Component.translatable(TRANSLATION_PREFIX + "auto_combat_on_hit",
                Component.translatable(TRANSLATION_PREFIX + (ClientData.autoCombatOnHit() ? "on" : "off")));
    }

    private Component chinesePoemLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "chinese_poems",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.chinesePoemDescriptions() ? "on" : "off")));
    }

    private Component formationPlatePaymentLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "formation_plate_payment",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.formationPlateQiFirst()
                        ? "payment_qi_spirit_stone"
                        : "payment_spirit_stone_qi")));
    }

    private Component qiUseParticlesLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "qi_use_particles",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.showQiUseParticles() ? "on" : "off")));
    }

    private Component spiritualSenseQiCurrentsLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "spiritual_sense_qi_currents",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.showSpiritualSenseQiCurrents()
                                ? "on" : "off")));
    }

    private Component droughtDomainLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "drought_zombie_domain",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.droughtZombieDomainEnabled() ? "on" : "off")));
    }

    private boolean showCreativeFlightOption() {
        return CultivationLevels.getRealmIndex(ClientData.cultivationData.cultivationLevel()) >= 3;
    }

    @Override
    protected void renderMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean showCreativeFlight = showCreativeFlightOption();
        int left = panelX();
        int top = panelY();

        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(graphics, left, top, panelWidth(), panelHeight());
        drawCenteredFitted(graphics, title, left + panelWidth() / 2, top + 20,
                panelWidth() - 150, 0xFF38352E);

        graphics.enableScissor(left + 20, contentTop(), left + panelWidth() - 20, contentBottom());
        for (int i = 0; i < rowCount(showCreativeFlight); i++) drawSettingRow(graphics, i, showCreativeFlight);
        graphics.disableScissor();
        int viewport = contentHeight();
        int full = contentExtent(showCreativeFlight);
        if (full > viewport) {
            int barHeight = Math.max(18, viewport * viewport / full);
            int barY = contentTop() + scroll * (viewport - barHeight) / (full - viewport);
            graphics.fill(left + panelWidth() - 22, barY, left + panelWidth() - 18, barY + barHeight, 0xAA897451);
        }
        if (!status.getString().isEmpty()) drawCenteredFitted(graphics, status, left + panelWidth() / 2,
                contentBottom() + 3, panelWidth() - 45, 0xCC552222);
        super.renderMenu(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSettingRow(GuiGraphics graphics, int index, boolean showCreativeFlight) {
        int left = panelX();
        int rowTop = rowTop(index, showCreativeFlight);
        int inset = Math.max(25, panelWidth() / 11);
        int rowHeight = rowVisualHeight(showCreativeFlight);
        graphics.fill(left + inset, rowTop, left + panelWidth() - inset,
                rowTop + rowHeight, 0x22A89B7D);
        graphics.renderOutline(left + inset, rowTop, panelWidth() - inset * 2,
                rowHeight, 0x2C897451);
    }

    private void drawCenteredFitted(GuiGraphics graphics, Component text, int centerX, int y,
                                    int availableWidth, int color) {
        int textWidth = font.width(text);
        if (textWidth <= availableWidth) {
            graphics.drawString(font, text, centerX - textWidth / 2, y, color, false);
            return;
        }

        float scale = Math.max(0.7F, availableWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledX = Math.round(centerX / scale - textWidth / 2.0F);
        int scaledY = Math.round(y / scale);
        graphics.drawString(font, text, scaledX, scaledY, color, false);
        graphics.pose().popPose();
    }

    private int buttonY(int index, boolean showCreativeFlight) {
        return rowTop(index, showCreativeFlight)
                + (rowVisualHeight(showCreativeFlight) - buttonHeight(showCreativeFlight)) / 2;
    }

    private int rowTop(int index, boolean showCreativeFlight) {
        int span = contentExtent(showCreativeFlight) - rowVisualHeight(showCreativeFlight);
        return contentTop() + (int) Math.round(index * span / (double) (rowCount(showCreativeFlight) - 1)) - scroll;
    }

    private int buttonHeight(boolean showCreativeFlight) {
        int count = rowCount(showCreativeFlight);
        return Math.min(BUTTON_HEIGHT, Math.max(MIN_BUTTON_HEIGHT,
                (contentHeight() - ROW_GAP * (count - 1)) / count));
    }

    private int rowVisualHeight(boolean showCreativeFlight) {
        return Math.min(ROW_HEIGHT, buttonHeight(showCreativeFlight) + ROW_GAP);
    }

    private int contentExtent(boolean showCreativeFlight) {
        return Math.max(contentHeight(), rowCount(showCreativeFlight) * rowVisualHeight(showCreativeFlight));
    }

    private int rowPitch(boolean showCreativeFlight) {
        return Math.max(1, (contentExtent(showCreativeFlight) - rowVisualHeight(showCreativeFlight))
                / (rowCount(showCreativeFlight) - 1));
    }

    private int rowCount(boolean showCreativeFlight) {
        return (showCreativeFlight ? 19 : 18) + (showModSky ? 1 : 0);
    }
    private Component modSkyLabel() {
        return Component.translatable(TRANSLATION_PREFIX + "mod_sky",
                Component.translatable(TRANSLATION_PREFIX + (ModClientConfig.enableModSky() ? "on" : "off")));
    }
    private Component swordRecallLabel(){return Component.translatable(TRANSLATION_PREFIX+"flying_sword_auto_recall."+(ClientData.flyingSwordAutoRecall()?"on":"off"));}

    private int contentHeight() {
        return Math.max(1, contentBottom() - contentTop());
    }

    private int contentTop() {
        return panelY() + Math.round(panelHeight() * CONTENT_TOP_TEXTURE_Y
                / (float) CultivationGuiTextures.LANDSCAPE_HEIGHT);
    }

    private int contentBottom() {
        return panelY() + Math.round(panelHeight() * CONTENT_BOTTOM_TEXTURE_Y
                / (float) CultivationGuiTextures.LANDSCAPE_HEIGHT);
    }

    private Component aiLabel() {
        if (!RingAiSettingsScreen.hostMayConfigure()) return Component.translatable("ring.ai.host_only");
        return Component.translatable("ring.ai.toggle", Component.translatable(RingAiConfig.get().enabled() ? "options.on" : "options.off"));
    }

    private void updateScroll() {
        boolean showCreativeFlight = showCreativeFlightOption();
        scroll = net.minecraft.util.Mth.clamp(scroll, 0, Math.max(0,
                contentExtent(showCreativeFlight) - contentHeight()));
        int top = contentTop(), bottom = contentBottom();
        rowWidgets.forEach((widget, baseY) -> {
            widget.setY(baseY - scroll);
            widget.visible = widget.getY() >= top && widget.getY() + widget.getHeight() <= bottom;
            if (!widget.visible && getFocused() == widget) setFocused(null);
        });
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll -= (int)(dy * rowPitch(showCreativeFlightOption()));
        updateScroll();
        return true;
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 266 || key == 267 || key == 268 || key == 269) {
            scroll = key == 268 ? 0 : key == 269 ? Integer.MAX_VALUE
                    : scroll + (key == 266 ? -1 : 1) * contentHeight();
            updateScroll(); return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(280, width - 20));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(248, height - 20));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
