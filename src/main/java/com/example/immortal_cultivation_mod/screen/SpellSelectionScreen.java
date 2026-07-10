package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.ClientSpellCombos;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellSelectionScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_selection.png");
    private static final ResourceLocation COMBO_ICON =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/spell_icon_combo.png");
    private static final ResourceLocation UNKNOWN_ICON =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png");
    private static final ResourceLocation SPELL_ROW_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_background.png");
    private static final int TEXTURE_W = 1567;
    private static final int TEXTURE_H = 1004;
    private static final int ROW_TEXTURE_W = 782;
    private static final int ROW_TEXTURE_H = 83;
    private static final int DESIGN_W = 1567;
    private static final int DESIGN_H = 1004;
    private static final Map<ResourceLocation, IconSize> ICON_SIZES = new HashMap<>();
    private static final Map<String, String> SPELL_EFFECT_ICONS = Map.ofEntries(
            Map.entry(ModSpells.DINGSHEN, "dishen"),
            Map.entry(ModSpells.SUISHUANG_LINGXIAO, "frost_flight"),
            Map.entry(ModSpells.HANJING_SUOZHUA, "frozen_qi"),
            Map.entry(ModSpells.SHUANGTIAN_QI, "frozen"),
            Map.entry(ModSpells.LIGHT_BEAM_ATTACK, "spell_damage_boost")
    );

    private final List<SelectionEntry> allEntries;
    private SpellListWidget listWidget;
    private Button toggleButton;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button yuqiModeButton;
    private Button closeButton;
    private SelectionEntry selectedEntry;

    public SpellSelectionScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_selection"));
        this.allEntries = getKnownEntries();
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();

        listWidget = new SpellListWidget(minecraft, sx(835), sy(618), y + sy(165), sy(85));
        listWidget.setX(x + sx(78));
        populateList();
        addRenderableWidget(listWidget);

        toggleButton = addRenderableWidget(new StyledButton(
                x + sx(594),
                y + sy(900),
                sx(420),
                sy(54),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".toggle_in_wheel"),
                btn -> {
                    if (selectedEntry != null) {
                        toggleEntry(selectedEntry);
                        updateToggleButton();
                    }
                }
        ));
        updateToggleButton();

        moveUpButton = addRenderableWidget(new StyledButton(
                x + sx(280),
                y + sy(900),
                sx(136),
                sy(54),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".wheel_move_up"),
                btn -> moveSelectedWheelSpell(-1)
        ));
        moveDownButton = addRenderableWidget(new StyledButton(
                x + sx(430),
                y + sy(900),
                sx(146),
                sy(54),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".wheel_move_down"),
                btn -> moveSelectedWheelSpell(1)
        ));
        updateMoveButtons();

        yuqiModeButton = addRenderableWidget(new TextStyledButton(
                x + sx(985),
                y + sy(690),
                sx(360),
                sy(46),
                Component.empty(),
                btn -> {
                    boolean allMode = ClientData.toggleYuqiControlAllMode();
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundYuqiModePayload(allMode));
                    updateYuqiModeButton();
                }
        ));
        updateYuqiModeButton();

        addRenderableWidget(new StyledButton(
                x + sx(1106),
                y + sy(900),
                sx(234),
                sy(54),
                Component.translatable("gui.done"),
                btn -> onClose()
        ));

        closeButton = addRenderableWidget(new StyledButton(
                x + sx(1490),
                y + sy(18),
                sx(58),
                sy(58),
                Component.empty(),
                btn -> onClose()
        ));
    }

    private int panelW() {
        return Math.max(320, Math.min(width - 8, Math.round((height - 8) * (DESIGN_W / (float) DESIGN_H))));
    }

    private int panelH() {
        return Math.max(205, Math.round(panelW() * (DESIGN_H / (float) DESIGN_W)));
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    private int panelY() {
        return (height - panelH()) / 2;
    }

    private int sx(int designX) {
        return Math.round(designX * (panelW() / (float) DESIGN_W));
    }

    private int sy(int designY) {
        return Math.round(designY * (panelH() / (float) DESIGN_H));
    }

    private static List<SelectionEntry> getKnownEntries() {
        ArrayList<SelectionEntry> entries = new ArrayList<>();
        for (ModSpells.SpellDef spell : ClientSpellCombos.knownSpells(ClientData.cultivationData)) {
            entries.add(SelectionEntry.spell(spell));
        }
        for (ClientSpellCombos.SpellCombo combo : ClientSpellCombos.combos()) {
            entries.add(SelectionEntry.combo(combo));
        }
        return entries;
    }

    private void populateList() {
        listWidget.children().clear();
        for (SelectionEntry entry : allEntries) {
            listWidget.children().add(new SpellListEntry(entry));
        }
    }

    private void toggleEntry(SelectionEntry entry) {
        ClientData.toggleSpellVisibleInWheel(entry.id());
        updateMoveButtons();
    }

    private void selectEntry(SelectionEntry entry) {
        selectedEntry = entry;
        updateToggleButton();
        updateMoveButtons();
        updateYuqiModeButton();
    }

    private void moveSelectedWheelSpell(int offset) {
        if (selectedEntry == null) {
            return;
        }
        ClientData.moveWheelSpell(selectedEntry.id(), offset);
        updateMoveButtons();
    }

    private void updateToggleButton() {
        if (toggleButton == null) {
            return;
        }

        toggleButton.active = selectedEntry != null;

        if (selectedEntry == null) {
            toggleButton.setMessage(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".toggle_in_wheel"));
            return;
        }

        String key = ClientData.isSpellVisibleInWheel(selectedEntry.id()) ? "remove_from_wheel" : "add_to_wheel";
        toggleButton.setMessage(Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key));
    }

    private void updateMoveButtons() {
        if (moveUpButton == null || moveDownButton == null) {
            return;
        }
        boolean selectedVisible = selectedEntry != null && ClientData.isSpellVisibleInWheel(selectedEntry.id());
        moveUpButton.active = selectedVisible && ClientData.canMoveWheelSpell(selectedEntry.id(), -1);
        moveDownButton.active = selectedVisible && ClientData.canMoveWheelSpell(selectedEntry.id(), 1);
    }

    private void updateYuqiModeButton() {
        if (yuqiModeButton == null) {
            return;
        }
        boolean visible = selectedEntry != null && ModSpells.YUQI_SHU.equals(ModSpells.normalizeId(selectedEntry.id()));
        yuqiModeButton.visible = visible;
        yuqiModeButton.active = visible;
        if (!visible) {
            return;
        }
        String key = ClientData.yuqiControlAllMode() ? "yuqi_mode_all" : "yuqi_mode_single";
        yuqiModeButton.setMessage(Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = panelX();
        int y = panelY();
        int panelWidth = panelW();
        int panelHeight = panelH();

        guiGraphics.blit(BACKGROUND, x, y, panelWidth, panelHeight,
                0.0F, 0.0F, TEXTURE_W, TEXTURE_H, TEXTURE_W, TEXTURE_H);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Component wheelCount = Component.literal(ClientData.selectedWheelSpellCount() + "/" + ClientData.MAX_WHEEL_SPELLS);
        guiGraphics.drawString(font, wheelCount, x + sx(870), y + sy(45), 0xFF3D3A32, false);

        renderActiveToggleSpells(guiGraphics, x + sx(250), y + sy(110), sx(1120));
        renderSpellDetails(guiGraphics, x + sx(978), y + sy(230), sx(390));

        if (allEntries.isEmpty()) {
            Component none = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spells");
            guiGraphics.drawString(font, none, (width - font.width(none)) / 2, height / 2 - 4, 0xCCCCCC, true);
        }
    }

    private void renderActiveToggleSpells(GuiGraphics guiGraphics, int x, int y, int maxWidth) {
        String names = activeToggleSpells().stream()
                .map(spell -> spell.name().getString())
                .reduce((a, b) -> a + ", " + b)
                .orElse(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".none_active_toggles").getString());

        Component line = Component.literal(names);

        if (font.width(line) > maxWidth) {
            names = font.plainSubstrByWidth(names, Math.max(20, maxWidth)) + "...";
            line = Component.literal(names);
        }

        guiGraphics.drawString(font, line, x, y, 0xFF3D3A32, false);
    }

    private List<ModSpells.SpellDef> activeToggleSpells() {
        if (minecraft == null || minecraft.player == null) {
            return List.of();
        }

        List<ModSpells.SpellDef> active = new ArrayList<>();

        addIfActive(active, ModSpells.EARTH_ESCAPE, ModEffects.EARTH_ESCAPE);
        addIfActive(active, ModSpells.QI_GATHERING, ModEffects.QI_GATHERING);
        addIfActive(active, ModSpells.SPIRIT_SIGHT, ModEffects.SPIRIT_SIGHT);
        addIfActive(active, ModSpells.WIND_STEP, ModEffects.WIND_STEP);
        addIfActive(active, ModSpells.YUFENG_JUE, ModEffects.YUFENG_JUE);
        addIfActive(active, ModSpells.WEIYA, ModEffects.WEIYA);
        addIfActive(active, ModSpells.FENGYA, ModEffects.FENGYA);
        addIfActive(active, ModSpells.SUISHUANG_LINGXIAO, ModEffects.FROST_FLIGHT);

        return active;
    }

    private void addIfActive(List<ModSpells.SpellDef> active, String spellId, Holder<MobEffect> effect) {
        if (minecraft != null && minecraft.player != null && minecraft.player.hasEffect(effect)) {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            if (spell != null) {
                active.add(spell);
            }
        }
    }

    private void renderSpellDetails(GuiGraphics guiGraphics, int x, int y, int maxWidth) {
        if (selectedEntry == null) {
            return;
        }

        int detailSlotSize = sx(86);
        int detailIconSize = sx(80);
        blitIconCentered(guiGraphics, selectedEntry.icon(), x - sx(3), y - sy(4), detailSlotSize, detailIconSize);
        guiGraphics.drawString(font, selectedEntry.name(), x + sx(98), y + sy(8), selectedEntry.nameColor(), false);

        Component desc = selectedEntry.description();
        drawFittedWrapped(guiGraphics, desc, x, y + sy(96), maxWidth, sy(110), 0xFF4A443A);

        int attrY = y + sy(250);
        guiGraphics.drawString(font, Component.literal(selectedEntry.costText()), x + sx(134), attrY + sy(5), 0xFF4A443A, false);
        guiGraphics.drawString(font, Component.literal(selectedEntry.requirementText()), x + sx(134), attrY + sy(50), 0xFF4A443A, false);
        guiGraphics.drawString(font, Component.literal(selectedEntry.proficiencyText()), x + sx(115), attrY + sy(100), 0xFF4A443A, false);
        guiGraphics.drawString(font, Component.literal(selectedEntry.cooldownText()), x + sx(137), attrY + sy(147), 0xFF4A443A, false);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class StyledButton extends Button {
        StyledButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            if (isHoveredOrFocused() && active) {
                g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0x1AFFFFFF);
            }
        }
    }

    private static class TextStyledButton extends Button {
        TextStyledButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int fill = isHoveredOrFocused() && active ? 0xCC52685B : 0xBB3F5047;
            int border = active ? 0xFFE6DDC4 : 0xFF8A8375;
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xAA2B302D);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            g.fill(getX(), getY(), getX() + width, getY() + 1, border);
            g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
            g.fill(getX(), getY(), getX() + 1, getY() + height, border);
            g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
            g.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    getX() + width / 2,
                    getY() + (height - 8) / 2,
                    active ? 0xFFFFF7DF : 0xFF9A9388
            );
        }
    }

    private class SpellListWidget extends ObjectSelectionList<SpellListEntry> {
        public SpellListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(width - 6, sx(782));
        }
    }

    private class SpellListEntry extends ObjectSelectionList.Entry<SpellListEntry> {
        private final SelectionEntry entry;

        SpellListEntry(SelectionEntry entry) {
            this.entry = entry;
        }

        @Override
        public Component getNarration() {
            return entry.name();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean visible = ClientData.isSpellVisibleInWheel(entry.id());
            int rowHeight = Math.round(width * (ROW_TEXTURE_H / (float) ROW_TEXTURE_W));
            int rowY = top + (height - rowHeight) / 2;
            guiGraphics.blit(SPELL_ROW_BACKGROUND, left, rowY, width, rowHeight,
                    0.0F, 0.0F, ROW_TEXTURE_W, ROW_TEXTURE_H, ROW_TEXTURE_W, ROW_TEXTURE_H);
            if (selectedEntry == entry) {
                guiGraphics.fill(left, rowY, left + width, rowY + rowHeight, 0x6638FF88);
            } else if (hovering) {
                guiGraphics.fill(left, rowY, left + width, rowY + rowHeight, 0x2238FF88);
            }

            boolean maxed = !visible && ClientData.selectedWheelSpellCount() >= ClientData.MAX_WHEEL_SPELLS;
            int wheelIndex = ClientData.selectedWheelSpellIndex(entry.id());
            String marker = visible && wheelIndex >= 0 ? "[" + (wheelIndex + 1) + "]" : maxed ? "[8]" : "[ ]";
            int markerColor = visible ? 0xFF2E6E50 : 0xFF8A8375;

            int slotX = left + Math.round(width * (25 / (float) ROW_TEXTURE_W));
            int slotY = rowY + Math.round(rowHeight * (7 / (float) ROW_TEXTURE_H));
            int slotSize = Math.round(rowHeight * (68 / (float) ROW_TEXTURE_H));
            int iconSize = Math.round(slotSize * 0.94F);
            int markerX = slotX + slotSize + 8;
            int textX = markerX + 24;

            blitIconCentered(guiGraphics, entry.icon(), slotX, slotY, slotSize, iconSize);
            guiGraphics.drawString(font, marker, markerX, rowY + Math.round(rowHeight * 0.25F), markerColor, false);
            drawClippedString(guiGraphics, entry.name(), textX, rowY + Math.round(rowHeight * 0.22F),
                    left + width - sx(18), entry.nameColor());
            drawClippedString(guiGraphics, Component.literal(entry.detail()), textX, rowY + Math.round(rowHeight * 0.52F),
                    left + width - sx(18), 0xFF6E685C);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1) {
                toggleEntry(entry);
                updateToggleButton();
                return true;
            }

            selectEntry(entry);
            return true;
        }
    }

    private record SelectionEntry(String id, Component name, String detail, Component description, int nameColor,
                                  ResourceLocation icon, String requirementText, String costText, String proficiencyText,
                                  String cooldownText) {
        static SelectionEntry spell(ModSpells.SpellDef spell) {
            String requirementText = spell.requiredLevel();
            String costText;
            if (ModSpells.WEIYA.equals(spell.id())) {
                costText = "1% qi per second";
            } else if (ModSpells.ZIBAO.equals(spell.id())) {
                costText = "all age";
            } else {
                costText = spell.qiCost() + " qi";
            }
            String proficiencyText = ModSpells.spellProficiencyLevel(ClientData.cultivationData, spell.id()).getString();
            String cooldownText = ModSpells.cooldownText(spell.id());
            String requirement = requirementText + " / " + costText + " / " + proficiencyText;
            return new SelectionEntry(
                    spell.id(),
                    spell.name(),
                    requirement,
                    Component.translatable("spell." + ImmortalCultivationMod.MODID + "." + spell.translationKey() + ".description"),
                    0xFF3D3A32,
                    iconForSpell(spell),
                    requirementText,
                    costText,
                    proficiencyText,
                    cooldownText);
        }

        static SelectionEntry combo(ClientSpellCombos.SpellCombo combo) {
            String elements = ClientSpellCombos.topElements(combo);
            String order = combo.spellIds().stream()
                    .map(ModSpells::get)
                    .filter(spell -> spell != null)
                    .map(spell -> spell.name().getString())
                    .reduce((a, b) -> a + " -> " + b)
                    .orElse(elements);
            Component description = Component.empty().append(ClientSpellCombos.castModeDescription(combo))
                    .append("\n")
                    .append(Component.literal(order));
            return new SelectionEntry(
                    combo.castId(),
                    Component.literal(combo.name()),
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_detail", elements, combo.spellIds().size()).getString(),
                    description,
                    0xFF5D4633,
                    COMBO_ICON,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo").getString(),
                    "-",
                    "-",
                    "-");
        }

        private static ResourceLocation iconForSpell(ModSpells.SpellDef spell) {
            if (UNKNOWN_ICON.equals(spell.icon()) && Minecraft.getInstance().getResourceManager().getResource(UNKNOWN_ICON).isPresent()) {
                return UNKNOWN_ICON;
            }
            String iconName = SPELL_EFFECT_ICONS.getOrDefault(spell.id(), spell.id());
            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/mob_effect/" + iconName + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(icon).isPresent() ? icon : UNKNOWN_ICON;
        }
    }

    private static void blitIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y, int size) {
        IconSize iconSize = iconSize(icon);
        guiGraphics.blit(icon, x, y, size, size, 0.0F, 0.0F,
                iconSize.width(), iconSize.height(), iconSize.width(), iconSize.height());
    }

    private static void blitIconCentered(GuiGraphics guiGraphics, ResourceLocation icon, int slotX, int slotY, int slotSize, int maxSize) {
        IconSize source = iconSize(icon);
        float scale = Math.min(maxSize / (float) source.width(), maxSize / (float) source.height());
        int drawW = Math.max(1, Math.round(source.width() * scale));
        int drawH = Math.max(1, Math.round(source.height() * scale));
        int drawX = slotX + (slotSize - drawW) / 2;
        int drawY = slotY + (slotSize - drawH) / 2;
        guiGraphics.blit(icon, drawX, drawY, drawW, drawH, 0.0F, 0.0F,
                source.width(), source.height(), source.width(), source.height());
    }

    private void drawClippedString(GuiGraphics guiGraphics, Component text, int x, int y, int maxX, int color) {
        int maxWidth = Math.max(12, maxX - x);
        Component rendered = text;
        if (font.width(rendered) > maxWidth) {
            rendered = Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(4, maxWidth - font.width("..."))) + "...");
        }
        guiGraphics.drawString(font, rendered, x, y, color, false);
    }

    private void drawFittedWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int maxHeight, int color) {
        float scale = 1.0F;
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, maxWidth);
        while ((lines.size() * Math.ceil(10.0F * scale) > maxHeight || widest(lines) * scale > maxWidth) && scale > 0.55F) {
            scale -= 0.05F;
            lines = font.split(text, Math.max(20, Math.round(maxWidth / scale)));
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        int lineHeight = Math.max(7, Math.round(10.0F / scale * scale));
        int maxLines = Math.max(1, (int) Math.floor(maxHeight / (10.0F * scale)));
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            guiGraphics.drawString(font, lines.get(i), 0, i * lineHeight, color, false);
        }
        guiGraphics.pose().popPose();
    }

    private int widest(List<net.minecraft.util.FormattedCharSequence> lines) {
        int widest = 0;
        for (var line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        return widest;
    }

    private static IconSize iconSize(ResourceLocation icon) {
        return ICON_SIZES.computeIfAbsent(icon, location -> Minecraft.getInstance().getResourceManager()
                .getResource(location)
                .map(resource -> {
                    try (var input = resource.open(); NativeImage image = NativeImage.read(input)) {
                        return new IconSize(image.getWidth(), image.getHeight());
                    } catch (Exception ignored) {
                        return new IconSize(16, 16);
                    }
                })
                .orElse(new IconSize(16, 16)));
    }

    private record IconSize(int width, int height) {
    }
}
