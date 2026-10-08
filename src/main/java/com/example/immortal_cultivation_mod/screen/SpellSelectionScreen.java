package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
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
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpellSelectionScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_selection.png");
    private static final ResourceLocation SPELL_ROW_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_background.png");
    private static final int TEXTURE_W = 1567;
    private static final int TEXTURE_H = 1004;
    private static final int ROW_TEXTURE_W = 782;
    private static final int ROW_TEXTURE_H = 83;
    private static final int DESIGN_W = 1567;
    private static final int DESIGN_H = 1004;
    private static final int WHEEL_CENTER_Y = 470;
    private static final int WHEEL_RADIUS = 92;
    private static final int WHEEL_SLOT_SIZE = 46;
    private static final int SCREEN_MARGIN = 4;
    private static final int WHEEL_PANEL_GAP = 8;
    private final List<SelectionEntry> allEntries;
    private final List<MethodHitbox> methodHitboxes = new ArrayList<>();
    private SpellListWidget listWidget;
    private Button toggleButton;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button yuqiModeButton;
    private Button closeButton;
    private SelectionEntry selectedEntry;
    private SpellListEntry draggedWheelEntry;
    private String draggedCircleSpellId;
    private int dragTargetWheelIndex = -1;
    private long lastSpellClickMillis;
    private String lastSpellClickId = "";
    private long lastMethodClickMillis;
    private String lastMethodClickId = "";

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
        return Math.max(SCREEN_MARGIN, width - panelW() - SCREEN_MARGIN);
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
        for (ModSpells.SpellDef spell : knownSpells()) {
            entries.add(SelectionEntry.spell(spell));
        }
        return entries;
    }

    private static List<ModSpells.SpellDef> knownSpells() {
        var data = ClientData.cultivationData;
        if (data == null) {
            return List.of();
        }
        ArrayList<String> spellIds = new ArrayList<>(
                data.knownSpells() == null ? List.of() : data.knownSpells());
        for (String spellId : ModSpells.innateSpellIds()) {
            if (ClientData.isInnateSpellKnown(spellId)) {
                spellIds.add(spellId);
            }
        }
        return spellIds.stream()
                .map(ModSpells::normalizeId)
                .map(ModSpells::get)
                .filter(spell -> spell != null)
                .distinct()
                .toList();
    }

    private void populateList() {
        listWidget.children().clear();

        // Keep wheel spells together in their real radial order so drag-and-drop is unambiguous.
        for (SelectionEntry entry : wheelEntriesInOrder()) {
            listWidget.children().add(new SpellListEntry(entry));
        }
        for (SelectionEntry entry : allEntries) {
            if (!ClientData.isSpellVisibleInWheel(entry.id())) {
                listWidget.children().add(new SpellListEntry(entry));
            }
        }
    }

    private List<SelectionEntry> wheelEntriesInOrder() {
        ArrayList<SelectionEntry> ordered = new ArrayList<>();
        for (int wheelIndex = 0; wheelIndex < ClientData.selectedWheelSpellCount(); wheelIndex++) {
            for (SelectionEntry entry : allEntries) {
                if (ClientData.selectedWheelSpellIndex(entry.id()) == wheelIndex) {
                    ordered.add(entry);
                    break;
                }
            }
        }
        return ordered;
    }

    private void toggleEntry(SelectionEntry entry) {
        ClientData.toggleSpellVisibleInWheel(entry.id());
        if (ClientData.isSpellVisibleInWheel(entry.id())) {
            PacketDistributor.sendToServer(
                    new DaoGuidancePayloads.ServerboundDaoGuidanceActionPayload(
                            DaoGuidanceContent.EQUIP_SPELL));
        }
        populateList();
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
        populateList();
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

        if (listWidget != null) {
            for (SpellListEntry entry : listWidget.children()) {
                entry.rendered = false;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderWheelOrderCircle(guiGraphics, mouseX, mouseY);

        Component wheelCount = Component.literal(ClientData.selectedWheelSpellCount() + "/" + ClientData.MAX_WHEEL_SPELLS);
        guiGraphics.drawString(font, wheelCount, x + sx(870), y + sy(45), 0xFF3D3A32, false);

        renderActiveToggleSpells(guiGraphics, x + sx(250), y + sy(110), sx(1120));
        renderSpellDetails(guiGraphics, x + sx(978), y + sy(230), sx(390));
        renderLearnedMethods(guiGraphics, mouseX, mouseY);

        if (toggleButton != null
                && DaoGuidanceClient.isCurrentAction(
                        DaoGuidanceContent.EQUIP_SPELL)) {
            int pulse = 145 + (int) (Math.sin(System.currentTimeMillis() / 180.0D) * 65.0D);
            int color = (Math.max(70, Math.min(230, pulse)) << 24) | 0xF0C66B;
            guiGraphics.renderOutline(toggleButton.getX() - 3,
                    toggleButton.getY() - 3,
                    toggleButton.getWidth() + 6,
                    toggleButton.getHeight() + 6, color);
            guiGraphics.drawCenteredString(font, Component.literal("↓"),
                    toggleButton.getX() + toggleButton.getWidth() / 2,
                    toggleButton.getY() - 13, color);
        }

        if (allEntries.isEmpty()) {
            Component none = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spells");
            guiGraphics.drawString(font, none, (width - font.width(none)) / 2, height / 2 - 4, 0xCCCCCC, true);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggedCircleSpellId != null) {
            int target = wheelCircleTarget(mouseX, mouseY);
            if (target >= 0) {
                dragTargetWheelIndex = target;
            }
            return true;
        }
        if (button == 0 && draggedWheelEntry != null) {
            int listTop = panelY() + sy(165);
            int listBottom = listTop + sy(618);
            if (mouseY < listTop + sy(24)) {
                listWidget.setScrollAmount(listWidget.getScrollAmount() - sy(12));
            } else if (mouseY > listBottom - sy(24)) {
                listWidget.setScrollAmount(listWidget.getScrollAmount() + sy(12));
            }
            int target = wheelDragTarget(mouseY);
            if (target >= 0) {
                dragTargetWheelIndex = target;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isWheelOrderDragging()) {
            boolean circleDrag = draggedCircleSpellId != null;
            String draggedId = circleDrag ? draggedCircleSpellId : draggedWheelEntry.entry.id();
            int current = ClientData.selectedWheelSpellIndex(draggedId);
            int target = circleDrag ? wheelCircleTarget(mouseX, mouseY) : wheelDragTarget(mouseY);
            if (target < 0) {
                target = dragTargetWheelIndex;
            }
            draggedWheelEntry = null;
            draggedCircleSpellId = null;
            dragTargetWheelIndex = -1;
            if (current >= 0 && target >= 0 && current != target) {
                ClientData.moveWheelSpell(draggedId, target - current);
                populateList();
                updateMoveButtons();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isWheelOrderDragging() {
        return draggedWheelEntry != null || draggedCircleSpellId != null;
    }

    private int wheelDragTarget(double mouseY) {
        int listTop = panelY() + sy(165);
        int listBottom = listTop + sy(618);
        if (mouseY < listTop || mouseY > listBottom || listWidget == null) {
            return -1;
        }

        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        for (SpellListEntry row : listWidget.children()) {
            int wheelIndex = ClientData.selectedWheelSpellIndex(row.entry.id());
            if (!row.rendered || wheelIndex < 0) {
                continue;
            }
            double distance = Math.abs(mouseY - (row.rowY + row.rowHeight / 2.0));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = wheelIndex;
            }
        }
        return closestIndex;
    }

    private void renderWheelOrderCircle(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = wheelCenterX();
        int centerY = wheelCenterY();
        int radius = wheelRadius();
        int slotSize = wheelSlotSize();

        for (int point = 0; point < 72; point++) {
            double angle = Math.PI * 2.0 * point / 72.0;
            int ringX = centerX + (int) Math.round(Math.sin(angle) * radius);
            int ringY = centerY - (int) Math.round(Math.cos(angle) * radius);
            graphics.fill(ringX - 1, ringY - 1, ringX + 1, ringY + 1, 0x997D6C48);
        }

        int coreSize = Math.max(18, Math.round(36 * wheelScale()));
        graphics.fill(centerX - coreSize / 2, centerY - coreSize / 2,
                centerX + coreSize / 2, centerY + coreSize / 2, 0xDDE8DFC8);
        graphics.renderOutline(centerX - coreSize / 2, centerY - coreSize / 2,
                coreSize, coreSize, 0xFF8C7951);
        Component count = Component.literal(ClientData.selectedWheelSpellCount() + "/" + ClientData.MAX_WHEEL_SPELLS);
        graphics.drawCenteredString(font, count, centerX, centerY - font.lineHeight / 2, 0xFF4A4337);

        List<SelectionEntry> ordered = wheelEntriesInOrder();
        for (int index = 0; index < ordered.size(); index++) {
            double angle = Math.PI * 2.0 * index / Math.max(1, ordered.size());
            int slotCenterX = centerX + (int) Math.round(Math.sin(angle) * radius);
            int slotCenterY = centerY - (int) Math.round(Math.cos(angle) * radius);
            int slotX = slotCenterX - slotSize / 2;
            int slotY = slotCenterY - slotSize / 2;
            boolean hovered = mouseX >= slotX && mouseX < slotX + slotSize
                    && mouseY >= slotY && mouseY < slotY + slotSize;
            boolean dragged = ordered.get(index).id().equals(draggedCircleSpellId);
            boolean target = isWheelOrderDragging() && dragTargetWheelIndex == index;

            int fill = dragged ? 0xDD9FCFB5 : hovered ? 0xDDF5EEDC : 0xDDE8DFC8;
            graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, fill);
            graphics.renderOutline(slotX, slotY, slotSize, slotSize,
                    target ? 0xFF2E8B57 : 0xFF8C7951);
            TechniqueIcons.blitCentered(graphics, ordered.get(index).icon(), slotX + 2, slotY + 2,
                    slotSize - 4, slotSize - 6);

            Component order = Component.literal("[" + (index + 1) + "]");
            int orderX = slotCenterX - font.width(order) / 2;
            int orderY = slotY + slotSize + Math.max(1, Math.round(2 * wheelScale()));
            graphics.drawString(font, order, orderX, orderY, 0xFF2E6E50, false);
        }
    }

    private float wheelScale() {
        float panelScale = panelW() / (float) DESIGN_W;
        int availableWidth = Math.max(1, panelX() - SCREEN_MARGIN - WHEEL_PANEL_GAP);
        float fitScale = availableWidth / (2.0F * (WHEEL_RADIUS + WHEEL_SLOT_SIZE / 2.0F));
        return Math.max(0.35F, Math.min(panelScale, fitScale));
    }

    private int wheelRadius() {
        return Math.max(20, Math.round(WHEEL_RADIUS * wheelScale()));
    }

    private int wheelSlotSize() {
        return Math.max(14, Math.round(WHEEL_SLOT_SIZE * wheelScale()));
    }

    private int wheelCenterX() {
        int left = SCREEN_MARGIN;
        int right = Math.max(left, panelX() - WHEEL_PANEL_GAP);
        int extent = wheelRadius() + (wheelSlotSize() + 1) / 2;
        return Math.max(left + extent, (left + right) / 2);
    }

    private int wheelCenterY() {
        return panelY() + sy(WHEEL_CENTER_Y);
    }

    private int wheelCircleTarget(double mouseX, double mouseY) {
        List<SelectionEntry> ordered = wheelEntriesInOrder();
        if (ordered.isEmpty()) {
            return -1;
        }
        int centerX = wheelCenterX();
        int centerY = wheelCenterY();
        int radius = wheelRadius();
        int slotSize = wheelSlotSize();
        for (int index = 0; index < ordered.size(); index++) {
            double angle = Math.PI * 2.0 * index / ordered.size();
            int slotCenterX = centerX + (int) Math.round(Math.sin(angle) * radius);
            int slotCenterY = centerY - (int) Math.round(Math.cos(angle) * radius);
            if (mouseX >= slotCenterX - slotSize / 2.0 && mouseX < slotCenterX + slotSize / 2.0
                    && mouseY >= slotCenterY - slotSize / 2.0 && mouseY < slotCenterY + slotSize / 2.0) {
                return index;
            }
        }
        return -1;
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
        addIfActive(active, ModSpells.SHIXU_CHILIU, ModEffects.SHIXU_CHILIU);
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
        TechniqueIcons.blitCentered(guiGraphics, selectedEntry.icon(), x - sx(3), y - sy(4),
                detailSlotSize, detailIconSize);
        guiGraphics.drawString(font, selectedEntry.name(), x + sx(98), y + sy(8), selectedEntry.nameColor(), false);

        Component desc = selectedEntry.description();
        CultivationGuiText.drawFittedWrapped(guiGraphics, font, desc, x, y + sy(96),
                maxWidth, sy(110), 0xFF4A443A, false);

        int attrY = y + sy(250);
        guiGraphics.drawString(font, Component.literal(selectedEntry.costText()), x + sx(134), attrY + sy(5), 0xFF4A443A, false);
        guiGraphics.drawString(font, Component.literal(selectedEntry.requirementText()), x + sx(134), attrY + sy(50), 0xFF4A443A, false);
        guiGraphics.drawString(font, Component.literal(selectedEntry.proficiencyText()), x + sx(115), attrY + sy(100), 0xFF4A443A, false);
        guiGraphics.drawString(font, Component.literal(selectedEntry.cooldownText()), x + sx(137), attrY + sy(147), 0xFF4A443A, false);
    }

    private List<CultivationMethods.MethodDef> learnedMethods() {
        if (ClientData.cultivationData == null) {
            return List.of();
        }
        Set<String> learned = ClientData.cultivationData.methodProficiencies().keySet();
        String active = ClientData.cultivationData.activeCultivationMethod();
        return CultivationMethods.allMethods().stream()
                .filter(method -> learned.contains(method.id()) || method.id().equals(active))
                .toList();
    }

    private void renderLearnedMethods(GuiGraphics graphics, int mouseX, int mouseY) {
        methodHitboxes.clear();
        List<CultivationMethods.MethodDef> methods = learnedMethods();
        if (methods.isEmpty()) {
            return;
        }

        int left = panelX() + sx(82);
        int right = panelX() + sx(900);
        int y = panelY() + sy(796);
        graphics.drawString(font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learned_methods"),
                left, y, 0xFF665A46, false);
        y += sy(25);
        int x = left;
        int rowHeight = Math.max(14, sy(25));
        int gap = Math.max(3, sx(7));
        for (CultivationMethods.MethodDef method : methods) {
            Component name = Component.translatable(method.nameKey());
            int itemWidth = Math.min(right - left, font.width(name) + sx(20));
            if (x + itemWidth > right) {
                x = left;
                y += rowHeight;
            }
            if (y + rowHeight > panelY() + sy(898)) {
                break;
            }
            boolean hovered = mouseX >= x && mouseX < x + itemWidth
                    && mouseY >= y && mouseY < y + rowHeight;
            graphics.fill(x, y, x + itemWidth, y + rowHeight,
                    hovered ? 0x44588968 : 0x247E765F);
            graphics.renderOutline(x, y, itemWidth, rowHeight, 0x55725F3A);
            CultivationGuiText.drawFittedSingle(
                    graphics, font, name, x + sx(7), y + Math.max(2, sy(6)),
                    itemWidth - sx(14), 0xFF453F35, false);
            methodHitboxes.add(new MethodHitbox(method.id(), x, y, itemWidth, rowHeight));
            x += itemWidth + gap;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int circleIndex = wheelCircleTarget(mouseX, mouseY);
            List<SelectionEntry> wheelEntries = wheelEntriesInOrder();
            if (circleIndex >= 0 && circleIndex < wheelEntries.size()) {
                SelectionEntry entry = wheelEntries.get(circleIndex);
                draggedCircleSpellId = entry.id();
                dragTargetWheelIndex = circleIndex;
                selectEntry(entry);
                return true;
            }
            if (listWidget != null) {
                for (SpellListEntry row : listWidget.children()) {
                    if (row.isHandleHovered(mouseX, mouseY)
                            && ClientData.isSpellVisibleInWheel(row.entry.id())) {
                        draggedWheelEntry = row;
                        dragTargetWheelIndex = ClientData.selectedWheelSpellIndex(row.entry.id());
                        selectEntry(row.entry);
                        return true;
                    }
                }
            }
            for (MethodHitbox hitbox : methodHitboxes) {
                if (!hitbox.contains(mouseX, mouseY)) {
                    continue;
                }
                long now = System.currentTimeMillis();
                if (hitbox.methodId().equals(lastMethodClickId)
                        && now - lastMethodClickMillis <= 350L
                        && minecraft != null) {
                    minecraft.setScreen(TechniqueDescriptionScreen.method(this, hitbox.methodId()));
                }
                lastMethodClickId = hitbox.methodId();
                lastMethodClickMillis = now;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        private boolean rendered;
        private int rowY;
        private int rowHeight;
        private int handleX;
        private int handleY;
        private int handleWidth;

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
            this.rendered = true;
            this.rowY = rowY;
            this.rowHeight = rowHeight;
            guiGraphics.blit(SPELL_ROW_BACKGROUND, left, rowY, width, rowHeight,
                    0.0F, 0.0F, ROW_TEXTURE_W, ROW_TEXTURE_H, ROW_TEXTURE_W, ROW_TEXTURE_H);
            if (selectedEntry == entry) {
                guiGraphics.fill(left, rowY, left + width, rowY + rowHeight, 0x6638FF88);
            } else if (hovering) {
                guiGraphics.fill(left, rowY, left + width, rowY + rowHeight, 0x2238FF88);
            }

            int wheelIndex = ClientData.selectedWheelSpellIndex(entry.id());
            String marker = visible && wheelIndex >= 0 ? "[" + (wheelIndex + 1) + "]" : "[ ]";
            int markerColor = visible ? 0xFF2E6E50 : 0xFF8A8375;

            int slotX = left + Math.round(width * (25 / (float) ROW_TEXTURE_W));
            int slotY = rowY + Math.round(rowHeight * (7 / (float) ROW_TEXTURE_H));
            int slotSize = Math.round(rowHeight * (68 / (float) ROW_TEXTURE_H));
            int iconSize = Math.round(slotSize * 0.94F);
            int handleX = slotX + slotSize + 7;
            int markerX = handleX;
            int textX = markerX + font.width("[8]") + 8;
            int textY = rowY + Math.round(rowHeight * 0.25F);

            this.handleX = handleX;
            this.handleY = rowY;
            this.handleWidth = font.width("[8]");

            if (draggedWheelEntry == this) {
                guiGraphics.fill(left + 2, rowY + 2, left + width - 2, rowY + rowHeight - 2, 0x443C8C68);
            } else if (isWheelOrderDragging() && visible && dragTargetWheelIndex == wheelIndex) {
                guiGraphics.renderOutline(left + 2, rowY + 2, width - 4, rowHeight - 4, 0xFF2E8B57);
            }

            TechniqueIcons.blitCentered(guiGraphics, entry.icon(), slotX, slotY, slotSize, iconSize);
            guiGraphics.drawString(font, marker, markerX, textY, markerColor, false);
            drawClippedString(guiGraphics, entry.name(), textX, rowY + Math.round(rowHeight * 0.22F),
                    left + width - sx(18), entry.nameColor());
            drawClippedString(guiGraphics, Component.literal(entry.detail()), textX, rowY + Math.round(rowHeight * 0.52F),
                    left + width - sx(18), 0xFF6E685C);
        }

        private boolean isHandleHovered(double mouseX, double mouseY) {
            int listTop = panelY() + sy(165);
            int listBottom = listTop + sy(618);
            return rendered
                    && mouseX >= handleX - 3
                    && mouseX <= handleX + handleWidth + 3
                    && mouseY >= Math.max(handleY, listTop)
                    && mouseY <= Math.min(handleY + rowHeight, listBottom);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1) {
                toggleEntry(entry);
                updateToggleButton();
                return true;
            }

            selectEntry(entry);
            long now = System.currentTimeMillis();
            if (entry.id().equals(lastSpellClickId)
                    && now - lastSpellClickMillis <= 350L
                    && minecraft != null) {
                minecraft.setScreen(TechniqueDescriptionScreen.spell(SpellSelectionScreen.this, entry.id()));
            }
            lastSpellClickId = entry.id();
            lastSpellClickMillis = now;
            return true;
        }
    }

    private record SelectionEntry(String id, Component name, String detail, Component description, int nameColor,
                                  ResourceLocation icon, String requirementText, String costText, String proficiencyText,
                                  String cooldownText) {
        static SelectionEntry spell(ModSpells.SpellDef spell) {
            String requirementText = CultivationLevels.displayName(spell.requiredLevel()).getString();
            String costText;
            if (ModSpells.WEIYA.equals(spell.id())) {
                costText = Component.translatable("spell.immortal_cultivation_mod.weiya.qi_cost").getString();
            } else if (ModSpells.ZIBAO.equals(spell.id())) {
                costText = "all age";
            } else if (ModSpells.LIANSHEN_SHU.equals(spell.id())) {
                costText = "75% max qi";
            } else if (ModSpells.SUOKONG_SHU.equals(spell.id())) {
                costText = "100 / 700 / 2000 qi";
            } else {
                costText = spell.qiCost() + " qi";
            }
            String proficiencyText = ModSpells.spellProficiencyLevel(ClientData.cultivationData, spell.id()).getString();
            String cooldownText = ModSpells.cooldownText(spell.id());
            String requirement = java.util.stream.Stream.of(
                            requirementText, costText, proficiencyText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(java.util.stream.Collectors.joining(" / "));
            return new SelectionEntry(
                    spell.id(),
                    spell.name(),
                    requirement,
                    TechniqueDescriptions.spell(spell),
                    0xFF3D3A32,
                    TechniqueIcons.spell(spell),
                    requirementText,
                    costText,
                    proficiencyText,
                    cooldownText);
        }

    }

    private void drawClippedString(GuiGraphics guiGraphics, Component text, int x, int y, int maxX, int color) {
        int maxWidth = Math.max(12, maxX - x);
        Component rendered = text;
        if (font.width(rendered) > maxWidth) {
            rendered = Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(4, maxWidth - font.width("..."))) + "...");
        }
        guiGraphics.drawString(font, rendered, x, y, color, false);
    }

    private record MethodHitbox(String methodId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
