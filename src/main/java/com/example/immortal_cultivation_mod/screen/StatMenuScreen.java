package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class StatMenuScreen extends Screen {
    private static final ResourceLocation STAT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/stat_menu.png");
    private static final ResourceLocation JINDAN_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/jindan_button.png");
    private static final int TEXTURE_W = 1402;
    private static final int TEXTURE_H = 1122;
    private static final int DESIGN_W = 520;
    private static final int DESIGN_H = 416;
    private static final int WINDOW_W = 430;
    private static final int WINDOW_H = 344;

    private static final int TEXT_COLOR = 0x3F3A35;
    private static final int VALUE_COLOR = 0x465A52;
    private static final int ACCENT_COLOR = 0x5F4A82;

    private static final int SKILL_BUTTON_X = 198;
    private static final int SKILL_BUTTON_Y = 96;
    private static final int SKILL_BUTTON_SPACING = 25;
    private static final int SKILL_BUTTON_SIZE = 24;

    private static final int SKILL_TEXT_X = 230;
    private static final int SKILL_TEXT_Y = 104;
    private static final int SKILL_TEXT_SPACING = 25;
    private static final int SKILL_TEXT_WIDTH = 245;

    private static final int SKILL_POINTS_X = 428;
    private static final int SKILL_POINTS_Y = 47;
    private static final int SKILL_POINTS_WIDTH = 88;

    private static final int LEVEL_X = 49;
    private static final int LEVEL_Y = 95;
    private static final int LEVEL_WIDTH = 112;

    private static final int LEFT_VALUE_X = 105;
    private static final int LEFT_VALUE_WIDTH = 65;
    private static final int BODY_Y = 223;
    private static final int MORAL_Y = 250;
    private static final int LUCK_Y = 277;
    private static final int SOUL_Y = 304;
    private static final int THOUGHTS_Y = 331;

    private static final int ROOT_X = 196;
    private static final int ROOT_Y = 282;
    private static final int ROOT_WIDTH = 146;

    private static final int METHOD_X = 194;
    private static final int METHOD_Y = 322;
    private static final int METHOD_WIDTH = 150;

    private static final int AGE_X = 395;
    private static final int AGE_Y = 286;
    private static final int AGE_WIDTH = 68;

    private static final int PROGRESS_X = 196;
    private static final int PROGRESS_Y = 367;
    private static final int PROGRESS_WIDTH = 166;

    private static final int BREAKTHROUGH_X = 379;
    private static final int BREAKTHROUGH_Y = 353;
    private static final int BREAKTHROUGH_WIDTH = 91;
    private static final int BREAKTHROUGH_HEIGHT = 27;
    private static final int JINDAN_BUTTON_SIZE = 36;
    private final List<Button> skillButtons = new ArrayList<>();
    private Button breakthroughBtn;
    private Button proficiencyBtn;
    private Button jindanBtn;

    public StatMenuScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".stat_menu"));
    }

    @Override
    protected void init() {
        skillButtons.clear();
        int x = left();
        int y = top();

        addSkillButton("hp", x + sx(SKILL_BUTTON_X), y + sy(SKILL_BUTTON_Y));
        addSkillButton("qi", x + sx(SKILL_BUTTON_X), y + sy(SKILL_BUTTON_Y + SKILL_BUTTON_SPACING));
        addSkillButton("energy", x + sx(SKILL_BUTTON_X), y + sy(SKILL_BUTTON_Y + SKILL_BUTTON_SPACING * 2));
        addSkillButton("physical", x + sx(SKILL_BUTTON_X), y + sy(SKILL_BUTTON_Y + SKILL_BUTTON_SPACING * 3));
        addSkillButton("magic", x + sx(SKILL_BUTTON_X), y + sy(SKILL_BUTTON_Y + SKILL_BUTTON_SPACING * 4));
        addSkillButton("mental", x + sx(SKILL_BUTTON_X), y + sy(SKILL_BUTTON_Y + SKILL_BUTTON_SPACING * 5));

        proficiencyBtn = new InvisibleButton(
                x + tx(30),
                y + ty(300),
                tx(110),
                ty(210),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".proficiency_menu"),
                b -> minecraft.setScreen(new ProficiencyMenuScreen())
        );
        addRenderableWidget(proficiencyBtn);

        breakthroughBtn = new InvisibleButton(
                x + sx(BREAKTHROUGH_X),
                y + sy(BREAKTHROUGH_Y),
                sx(BREAKTHROUGH_WIDTH),
                sy(BREAKTHROUGH_HEIGHT),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".breakthrough_button"),
                b -> PacketDistributor.sendToServer(new ModPayloads.ServerboundRequestBreakthroughPayload())
        );
        addRenderableWidget(breakthroughBtn);

        jindanBtn = new TexturedIconButton(
                x + tx(958),
                y + ty(820),
                sx(JINDAN_BUTTON_SIZE),
                sy(JINDAN_BUTTON_SIZE),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".enter_jindan"),
                b -> {
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundEnterJindanPayload());
                    minecraft.setScreen(null);
                },
                JINDAN_BUTTON_TEXTURE
        );
        addRenderableWidget(jindanBtn);
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int top() {
        return (height - WINDOW_H) / 2;
    }

    private static int sx(int designX) {
        return Math.round(designX * (WINDOW_W / (float) DESIGN_W));
    }

    private static int sy(int designY) {
        return Math.round(designY * (WINDOW_H / (float) DESIGN_H));
    }

    private static int tx(int textureX) {
        return Math.round(textureX * (WINDOW_W / (float) TEXTURE_W));
    }

    private static int ty(int textureY) {
        return Math.round(textureY * (WINDOW_H / (float) TEXTURE_H));
    }

    private void addSkillButton(String stat, int x, int y) {
        Button button = new InvisibleButton(
                x,
                y,
                sx(SKILL_BUTTON_SIZE),
                sy(SKILL_BUTTON_SIZE),
                Component.literal("+"),
                b -> PacketDistributor.sendToServer(new ModPayloads.ServerboundSpendSkillPointPayload(stat))
        );
        skillButtons.add(button);
        addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int x = left();
        int y = top();

        g.blit(STAT_TEXTURE, x, y, WINDOW_W, WINDOW_H,
                0.0F, 0.0F, TEXTURE_W, TEXTURE_H, TEXTURE_W, TEXTURE_H);

        Minecraft mc = Minecraft.getInstance();
        var data = ClientData.cultivationData;

        if (data == null) {
            setWidgetsVisible(false);
            drawFitted(g, mc, Component.literal("No data"), x + sx(221), y + sy(198), sx(80), 0xAA3333, false);
            super.render(g, mx, my, pt);
            return;
        }

        String level = CultivationLevels.isMortal(data.cultivationLevel()) ? CultivationLevels.REALM_MORTAL : data.cultivationLevel();
        var levelDef = CultivationLevels.getLevelDef(level);
        int maxHp = Math.max(1, levelDef.maxHp() + data.maxHpBonus());
        int maxQi = levelDef.maxQi() + data.maxQiBonus();
        int maxAge = Math.max(1, levelDef.maxAge() - data.agePenalty());

        drawCentered(g, mc, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".skill_points", data.skillPoints()),
                x + sx(SKILL_POINTS_X), y + sy(SKILL_POINTS_Y), sx(SKILL_POINTS_WIDTH), 0xEFE8D4, false);
        drawCentered(g, mc, Component.literal(level), x + sx(LEVEL_X), y + sy(LEVEL_Y), sx(LEVEL_WIDTH), VALUE_COLOR, false);

        drawSkillLine(g, mc, x, y, 0, "hp", 4, maxHp, data.maxHpBonus() / 4, TEXT_COLOR);
        drawSkillLine(g, mc, x, y, 1, "qi", 40, maxQi, data.maxQiBonus() / 40, 0x355F6C);
        drawSkillLine(g, mc, x, y, 2, "energy", 40, data.maxEnergyBonus(), data.maxEnergyBonus() / 40, 0x735F2D);
        drawSkillLine(g, mc, x, y, 3, "physical", 1, data.physicalAttack(), data.physicalAttack(), 0x794D39);
        drawSkillLine(g, mc, x, y, 4, "magic", 1, data.magicAttack(), data.magicAttack(), ACCENT_COLOR);
        drawSkillLine(g, mc, x, y, 5, "mental", 1, data.mentalAttack(), data.mentalAttack(), 0x3B6A57);

        drawFitted(g, mc, Component.literal(data.bodyType()), x + sx(LEFT_VALUE_X), y + sy(BODY_Y), sx(LEFT_VALUE_WIDTH), VALUE_COLOR, false);
        drawFitted(g, mc, Component.literal(String.valueOf(data.moral())), x + sx(LEFT_VALUE_X), y + sy(MORAL_Y), sx(LEFT_VALUE_WIDTH), VALUE_COLOR, false);
        drawFitted(g, mc, Component.literal(String.valueOf(data.luck())), x + sx(LEFT_VALUE_X), y + sy(LUCK_Y), sx(LEFT_VALUE_WIDTH), VALUE_COLOR, false);
        drawFitted(g, mc, Component.literal(String.valueOf(data.soul())), x + sx(LEFT_VALUE_X), y + sy(SOUL_Y), sx(LEFT_VALUE_WIDTH), ACCENT_COLOR, false);
        drawFitted(g, mc, Component.literal(String.valueOf(data.thoughts())), x + sx(LEFT_VALUE_X), y + sy(THOUGHTS_Y), sx(LEFT_VALUE_WIDTH), VALUE_COLOR, false);

        drawCentered(g, mc, Component.literal(SpiritRoots.format(data.spiritRoots(), data.spiritRootGrade())),
                x + sx(ROOT_X), y + sy(ROOT_Y), sx(ROOT_WIDTH), VALUE_COLOR, false);

        var method = CultivationMethods.get(data.activeCultivationMethod());
        Component methodName = method == null
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".none")
                : Component.translatable(method.nameKey());
        MutableComponent methodLine = methodName.copy();
        if (method != null) {
            methodLine = methodLine.append(" / ").append(CultivationMethods.methodProficiencyLevel(data, data.activeCultivationMethod()));
        }
        drawCentered(g, mc, methodLine, x + sx(METHOD_X), y + sy(METHOD_Y), sx(METHOD_WIDTH), VALUE_COLOR, false);
        drawCentered(g, mc, Component.literal(String.valueOf(maxAge)), x + sx(AGE_X), y + sy(AGE_Y), sx(AGE_WIDTH), VALUE_COLOR, false);

        long progressNeeded = CultivationLevels.getTotalQiNeeded(level);
        drawCentered(g, mc, Component.literal(data.cultivationProgress() + "/" + progressNeeded),
                x + sx(PROGRESS_X), y + sy(PROGRESS_Y), sx(PROGRESS_WIDTH), 0xEEE7D4, false);

        String nextStage = CultivationLevels.getNextStage(level);
        boolean fullProgress = data.cultivationProgress() >= progressNeeded;
        boolean needsPill = CultivationLevels.needsBreakthroughPill(level);
        boolean hasPill = hasBreakthroughPill(mc);
        boolean canBreakthrough = nextStage != null && fullProgress && (!needsPill || hasPill);

        for (Button button : skillButtons) {
            button.active = data.skillPoints() > 0;
            button.visible = true;
        }
        if (breakthroughBtn != null) {
            breakthroughBtn.active = canBreakthrough;
            breakthroughBtn.visible = nextStage != null;
        }
        if (jindanBtn != null) {
            boolean canEnterJindan = CultivationLevels.getRealmIndex(level) >= CultivationLevels.getRealmIndex(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
            jindanBtn.active = canEnterJindan;
            jindanBtn.visible = canEnterJindan;
        }

        super.render(g, mx, my, pt);
    }

    private void setWidgetsVisible(boolean visible) {
        skillButtons.forEach(button -> button.visible = visible);
        if (breakthroughBtn != null) {
            breakthroughBtn.visible = visible;
        }
        if (proficiencyBtn != null) {
            proficiencyBtn.visible = visible;
        }
        if (jindanBtn != null) {
            jindanBtn.visible = visible;
        }
    }

    private boolean hasBreakthroughPill(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        for (var stack : mc.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == com.example.immortal_cultivation_mod.item.ModItems.BREAKTHROUGH_PILL.get()) {
                return true;
            }
        }
        return false;
    }

    private void drawSkillLine(GuiGraphics g, Minecraft mc, int x, int y, int row, String key, int increase, int total, int invested, int color) {
        Component label = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".skill_" + key, increase, total, invested);
        drawFitted(g, mc, label, x + sx(SKILL_TEXT_X), y + sy(SKILL_TEXT_Y + row * SKILL_TEXT_SPACING), sx(SKILL_TEXT_WIDTH), color, false);
    }

    private void drawCentered(GuiGraphics g, Minecraft mc, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
        Component fitted = fittedText(mc, text, maxWidth);
        g.drawString(mc.font, fitted, x + (maxWidth - mc.font.width(fitted)) / 2, y, color, shadow);
    }

    private void drawFitted(GuiGraphics g, Minecraft mc, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
        g.drawString(mc.font, fittedText(mc, text, maxWidth), x, y, color, shadow);
    }

    private Component fittedText(Minecraft mc, Component text, int maxWidth) {
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = mc.font.width(suffix);
        String clipped = mc.font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - suffixWidth));
        return Component.literal(clipped + suffix);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class InvisibleButton extends Button {
        InvisibleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        }
    }

    private static class TexturedIconButton extends Button {
        private final ResourceLocation texture;

        TexturedIconButton(int x, int y, int width, int height, Component message, OnPress onPress, ResourceLocation texture) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.texture = texture;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int color = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFEFE6C8;
            g.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
            g.blit(texture, getX(), getY(), getWidth(), getHeight(), 0.0F, 0.0F, 138, 139, 138, 139);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
