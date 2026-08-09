package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TechniqueDescriptionScreen extends Screen {
    protected enum Kind {
        SPELL,
        METHOD
    }

    private static final int DESIGN_WIDTH = 720;
    private static final int DESIGN_HEIGHT = 576;
    private static final int CONTENT_Y_OFFSET = 18;
    private static final ResourceLocation POEM_FONT =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "aidian_full_numbers");
    private static final String SCREEN_PREFIX =
            "screen." + ImmortalCultivationMod.MODID + ".description.";

    protected final Kind kind;
    protected final String techniqueId;
    @Nullable
    private final Screen parent;

    protected TechniqueDescriptionScreen(Kind kind, String techniqueId, @Nullable Screen parent) {
        super(resolveName(kind, techniqueId));
        this.kind = kind;
        this.techniqueId = techniqueId;
        this.parent = parent;
    }

    public static TechniqueDescriptionScreen spell(Screen parent, String spellId) {
        return new TechniqueDescriptionScreen(Kind.SPELL, spellId, parent);
    }

    public static TechniqueDescriptionScreen method(Screen parent, String methodId) {
        return new TechniqueDescriptionScreen(Kind.METHOD, methodId, parent);
    }

    @Override
    protected void init() {
        Component primary = primaryActionLabel();
        int buttonY = panelY() + panelHeight() - sy(48);
        int buttonWidth = sx(150);
        int buttonHeight = Math.max(18, sy(28));
        if (primary == null) {
            addRenderableWidget(new CultivationGuiButton(
                    panelX() + (panelWidth() - buttonWidth) / 2,
                    buttonY,
                    buttonWidth,
                    buttonHeight,
                    Component.translatable("gui.done"),
                    this::onClose));
            return;
        }

        int gap = sx(18);
        int totalWidth = buttonWidth * 2 + gap;
        int firstX = panelX() + (panelWidth() - totalWidth) / 2;
        addRenderableWidget(new CultivationGuiButton(
                firstX,
                buttonY,
                buttonWidth,
                buttonHeight,
                primary,
                this::performPrimaryAction));
        addRenderableWidget(new CultivationGuiButton(
                firstX + buttonWidth + gap,
                buttonY,
                buttonWidth,
                buttonHeight,
                Component.translatable("gui.back"),
                this::onClose));
    }

    @Nullable
    protected Component primaryActionLabel() {
        return null;
    }

    protected void performPrimaryAction() {
    }

    @Nullable
    protected Component statusText() {
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelX();
        int top = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(graphics, left, top, panelWidth, panelHeight);

        Component name = resolveName(kind, techniqueId);
        drawCenteredFitted(graphics, name, left + panelWidth / 2, top + sy(24),
                panelWidth - sx(190), 0xFF38352E);

        boolean poem = ClientData.chinesePoemDescriptions();
        if (poem) {
            renderPoemLayout(graphics, left, top, panelWidth, panelHeight);
        } else {
            renderStandardLayout(graphics, left, top, panelWidth, panelHeight);
        }

        Component status = statusText();
        if (status != null) {
            drawCenteredFitted(graphics, status, left + panelWidth / 2,
                    top + panelHeight - sy(69), panelWidth - sx(120), 0xFF4B7958);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderStandardLayout(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth,
            int panelHeight) {
        ResourceLocation icon = resolveIcon();
        int iconSlot = sx(92);
        int iconX = left + sx(54);
        int iconY = top + sy(83 + CONTENT_Y_OFFSET);
        graphics.fill(iconX, iconY, iconX + iconSlot, iconY + iconSlot, 0x249A865B);
        graphics.renderOutline(iconX, iconY, iconSlot, iconSlot, 0x887F6B3D);
        TechniqueIcons.blitCentered(graphics, icon, iconX, iconY, iconSlot, sx(82));

        renderMetadata(graphics, left + sx(170), top + sy(78 + CONTENT_Y_OFFSET), panelWidth - sx(220));

        Component descriptionLabel = Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".description_label");
        graphics.drawString(font, descriptionLabel, left + sx(54), top + sy(203 + CONTENT_Y_OFFSET),
                0xFF665A46, false);
        int descriptionTop = top + sy(224 + CONTENT_Y_OFFSET);
        int descriptionBottom = top + panelHeight - sy(79);
        CultivationGuiText.drawFittedWrapped(
                graphics,
                font,
                resolveDescription(),
                left + sx(54),
                descriptionTop,
                panelWidth - sx(108),
                Math.max(24, descriptionBottom - descriptionTop),
                0xFF3F3A32,
                false);
    }

    private void renderPoemLayout(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth,
            int panelHeight) {
        int iconSlot = sx(72);
        int iconX = left + panelWidth - sx(54) - iconSlot;
        int iconY = top + sy(100);
        graphics.fill(iconX, iconY, iconX + iconSlot, iconY + iconSlot, 0x249A865B);
        graphics.renderOutline(iconX, iconY, iconSlot, iconSlot, 0x887F6B3D);
        TechniqueIcons.blitCentered(graphics, resolveIcon(), iconX, iconY, iconSlot, sx(64));

        int columnStep = sx(40);
        int columnX = iconX - sx(30);
        int textTop = top + sy(133);
        int textBottom = top + panelHeight - sy(92);
        List<String> poemLines = poemLines(resolveDescription().getString());
        List<Metadata> metadata = metadata();
        List<String> metadataLines = new ArrayList<>();
        for (Metadata value : metadata) {
            metadataLines.add(sanitizePoemText(
                    value.label().getString() + "：" + poemMetadataValue(value)));
        }
        int longestColumn = 1;
        for (String line : poemLines) {
            longestColumn = Math.max(
                    longestColumn, line.codePointCount(0, line.length()));
        }
        for (String line : metadataLines) {
            longestColumn = Math.max(
                    longestColumn, line.codePointCount(0, line.length()));
        }
        int availableHeight = Math.max(1, textBottom - textTop);
        int characterStep = Math.max(
                1, Math.min(sy(40), availableHeight / longestColumn));

        for (int i = 0; i < poemLines.size(); i++) {
            drawVerticalColumn(graphics, poemLines.get(i), columnX, textTop, characterStep,
                    longestColumn, 0xFF3F3A32);
            columnX -= columnStep;
            if (kind == Kind.METHOD && (i + 1) % 2 == 0 && i + 1 < poemLines.size()) {
                columnX -= sx(12);
            }
        }

        columnX -= sx(12);
        for (int i = 0; i < metadata.size(); i++) {
            drawVerticalColumn(
                    graphics,
                    metadataLines.get(i),
                    columnX,
                    textTop,
                    characterStep,
                    longestColumn,
                    metadata.get(i).color());
            columnX -= columnStep;
        }
    }

    private static List<String> poemLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String line = sanitizePoemText(rawLine).strip();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String sanitizePoemText(String text) {
        return text.replace(',', ' ')
                .replace(':', ' ')
                .replace('，', ' ')
                .replace('：', ' ');
    }

    private String poemMetadataValue(Metadata metadata) {
        String value = metadata.value().getString();
        if ("qi_cost_label".equals(metadata.key())) {
            value = value.replaceAll("(?i)max\\s+qi\\b", "最大气")
                    .replaceAll("(?i)\\s*\\bqi\\b", "气")
                    .replaceAll("(?i)\\s*/\\s*sec\\b", "/秒");
        }
        if ("cooldown_label".equals(metadata.key())) {
            value = value.replaceFirst("(?i)(\\d+(?:\\.\\d+)?)s$", "$1");
        }
        if ("method_proficiency_label".equals(metadata.key())) {
            value = value.replace("（", "")
                    .replace("）", "")
                    .replace("(", "")
                    .replace(")", "");
        }
        return value;
    }

    private void drawVerticalColumn(
            GuiGraphics graphics,
            String text,
            int centerX,
            int startY,
            int characterStep,
            int maxCharacters,
            int color) {
        int[] codePoints = text.codePoints().limit(maxCharacters).toArray();
        for (int i = 0; i < codePoints.length; i++) {
            Component glyph = withPoemFont(Component.literal(
                    new String(Character.toChars(codePoints[i]))));
            int glyphWidth = Math.max(1, font.width(glyph));
            int targetSize = Math.max(
                    1, characterStep - Math.max(1, sy(3)));
            float glyphScale = targetSize / (float) glyphWidth;
            graphics.pose().pushPose();
            graphics.pose().translate(
                    centerX,
                    startY + i * characterStep,
                    0.0F);
            graphics.pose().scale(glyphScale, glyphScale, 1.0F);
            graphics.drawString(
                    font,
                    glyph,
                    -glyphWidth / 2,
                    0,
                    color,
                    false);
            graphics.pose().popPose();
        }
    }

    private void renderMetadata(GuiGraphics graphics, int x, int y, int availableWidth) {
        List<Metadata> metadata = metadata();
        int gap = sx(10);
        int cellWidth = Math.max(62, (availableWidth - gap * 2) / 3);
        int rowHeight = sy(58);
        for (int i = 0; i < metadata.size(); i++) {
            int column = i % 3;
            int row = i / 3;
            int cellX = x + column * (cellWidth + gap);
            int cellY = y + row * rowHeight;
            Metadata value = metadata.get(i);
            graphics.drawString(font, value.label(), cellX, cellY, 0xFF786D5B, false);
            CultivationGuiText.drawFittedSingle(
                    graphics,
                    font,
                    value.value(),
                    cellX,
                    cellY + sy(18),
                    cellWidth,
                    value.color(),
                    false);
        }
    }

    private List<Metadata> metadata() {
        List<Metadata> result = new ArrayList<>();
        if (kind == Kind.SPELL) {
            ModSpells.SpellDef spell = ModSpells.get(techniqueId);
            if (spell == null) {
                return result;
            }
            result.add(metadata("requirement_label", literalOrNone(spell.requiredLevel()), 0xFF725D23));
            result.add(metadata("element_label", TechniqueDescriptions.element(spell.element()), 0xFF326A55));
            result.add(metadata("method_tier_label",
                    Component.translatable("method_tier." + ImmortalCultivationMod.MODID + "." + spell.tier()),
                    0xFF7A5A18));
            result.add(metadata("qi_cost_label", Component.literal(spellCost(spell)), 0xFF275A6A));
            result.add(metadata("method_proficiency_label",
                    ModSpells.spellProficiencyLevel(ClientData.cultivationData, spell.id()), 0xFF70428B));
            result.add(metadata("cooldown_label", Component.literal(ModSpells.cooldownText(spell.id())), 0xFF6B5037));
            return result;
        }

        CultivationMethods.MethodDef method = CultivationMethods.get(techniqueId);
        if (method == null) {
            return result;
        }
        result.add(metadata("method_limit_label", literalOrNone(method.limitLevel()), 0xFF725D23));
        result.add(metadata("element_label", TechniqueDescriptions.element(method.element()), 0xFF326A55));
        result.add(metadata("method_tier_label",
                Component.translatable("method_tier." + ImmortalCultivationMod.MODID + "." + method.tier()),
                0xFF7A5A18));
        result.add(metadata("method_proficiency_label",
                CultivationMethods.methodProficiencyDescription(ClientData.cultivationData, method.id()),
                0xFF70428B));
        return result;
    }

    private Metadata metadata(String labelKey, Component value, int color) {
        return new Metadata(
                labelKey,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + labelKey),
                value,
                color);
    }

    private Component literalOrNone(String value) {
        return value == null || value.isBlank()
                ? Component.translatable(SCREEN_PREFIX + "none")
                : Component.literal(value);
    }

    private Component resolveDescription() {
        if (kind == Kind.SPELL) {
            ModSpells.SpellDef spell = ModSpells.get(techniqueId);
            return spell == null
                    ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".unknown_spell")
                    : TechniqueDescriptions.spell(spell);
        }
        return TechniqueDescriptions.method(techniqueId);
    }

    private ResourceLocation resolveIcon() {
        if (kind == Kind.SPELL) {
            ModSpells.SpellDef spell = ModSpells.get(techniqueId);
            return spell == null ? TechniqueIcons.UNKNOWN_ICON : TechniqueIcons.spell(spell);
        }
        CultivationMethods.MethodDef method = CultivationMethods.get(techniqueId);
        return method == null ? TechniqueIcons.UNKNOWN_ICON : TechniqueIcons.method(method);
    }

    private static Component resolveName(Kind kind, String techniqueId) {
        if (kind == Kind.SPELL) {
            ModSpells.SpellDef spell = ModSpells.get(techniqueId);
            return spell == null
                    ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".unknown_spell")
                    : spell.name();
        }
        CultivationMethods.MethodDef method = CultivationMethods.get(techniqueId);
        return method == null
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".unknown_method")
                : Component.translatable(method.nameKey());
    }

    private static Component withPoemFont(Component component) {
        return component.copy().withStyle(style -> style.withFont(POEM_FONT));
    }

    private static String spellCost(ModSpells.SpellDef spell) {
        if (ModSpells.WEIYA.equals(spell.id())) {
            return "1% qi / sec";
        }
        if (ModSpells.ZIBAO.equals(spell.id())) {
            return "all age";
        }
        if (ModSpells.LIANSHEN_SHU.equals(spell.id())) {
            return "75% max qi";
        }
        if (ModSpells.SUOKONG_SHU.equals(spell.id())) {
            return "100 / 700 / 2000 qi";
        }
        return spell.qiCost() + " qi";
    }

    private void drawCenteredFitted(
            GuiGraphics graphics,
            Component text,
            int centerX,
            int y,
            int maxWidth,
            int color) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            graphics.drawString(font, text, centerX - textWidth / 2, y, color, false);
            return;
        }
        float scale = Math.max(0.45F, maxWidth / (float) Math.max(1, textWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -textWidth / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private int panelWidth() {
        int maxWidth = Math.max(320, width - 16);
        int maxHeight = Math.max(256, height - 16);
        int panelWidth = Math.min(DESIGN_WIDTH, maxWidth);
        int panelHeight = Math.round(panelWidth * (DESIGN_HEIGHT / (float) DESIGN_WIDTH));
        if (panelHeight > maxHeight) {
            panelHeight = maxHeight;
            panelWidth = Math.round(panelHeight * (DESIGN_WIDTH / (float) DESIGN_HEIGHT));
        }
        return panelWidth;
    }

    private int panelHeight() {
        return Math.round(panelWidth() * (DESIGN_HEIGHT / (float) DESIGN_WIDTH));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int sx(int value) {
        return Math.max(1, Math.round(value * (panelWidth() / (float) DESIGN_WIDTH)));
    }

    private int sy(int value) {
        return Math.max(1, Math.round(value * (panelHeight() / (float) DESIGN_HEIGHT)));
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Metadata(String key, Component label, Component value, int color) {
    }
}
