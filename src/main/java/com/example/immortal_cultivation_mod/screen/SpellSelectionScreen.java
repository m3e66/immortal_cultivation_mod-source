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
import com.example.immortal_cultivation_mod.shentong.ShentongRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpellSelectionScreen extends FittedMenuScreen {
    private static final int PANEL_W = 600, PANEL_H = 392;
    private static final int DESIGN_W = 1567;
    private static final int DESIGN_H = 1004;
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
    private Button previousMethodsButton,nextMethodsButton;
    private int methodPage,methodPages=1;
    private List<String> displayedSpellIds=List.of();
    private String displayedMethod="";
    private SelectionEntry selectedEntry;
    private SpellListEntry draggedWheelEntry;
    private String draggedCircleSpellId;
    private int dragTargetWheelIndex = -1;
    private long lastSpellClickMillis;
    private String lastSpellClickId = "";
    private long lastMethodClickMillis;
    private String lastMethodClickId = "";

    public SpellSelectionScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_selection"), 748, 416);
        this.allEntries = getKnownEntries();
    }

    @Override
    protected void initMenu() {
        if (ClientData.cultivationData != null)
            ClientData.reconcileWheelSelection(ClientData.cultivationData.knownSpells());
        int x = panelX(), y = panelY();
        listWidget = new SpellListWidget(minecraft, 316, 192, y + 101, 34);
        listWidget.setX(x + 30);
        populateList();
        addRenderableWidget(listWidget);
        toggleButton = addRenderableWidget(new StyledButton(x + 146, y + 354, 252, 22,
                text("toggle_in_wheel"), btn -> {
                    if (selectedEntry != null) { toggleEntry(selectedEntry); updateToggleButton(); }
                }));
        moveUpButton = addRenderableWidget(new StyledButton(x + 24, y + 354, 54, 22,
                text("wheel_move_up"), btn -> moveSelectedWheelSpell(-1)));
        moveDownButton = addRenderableWidget(new StyledButton(x + 84, y + 354, 54, 22,
                text("wheel_move_down"), btn -> moveSelectedWheelSpell(1)));
        yuqiModeButton = addRenderableWidget(new StyledButton(x + 376, y + 321, 188, 18,
                Component.empty(), btn -> {
                    boolean allMode = ClientData.toggleYuqiControlAllMode();
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundYuqiModePayload(allMode));
                    updateYuqiModeButton();
                }));
        previousMethodsButton=addRenderableWidget(new StyledButton(x+272,y+306,18,14,Component.literal("‹"),btn->changeMethodPage(-1)));
        nextMethodsButton=addRenderableWidget(new StyledButton(x+324,y+306,18,14,Component.literal("›"),btn->changeMethodPage(1)));
        previousMethodsButton.visible=nextMethodsButton.visible=false;
        addRenderableWidget(new StyledButton(x + 470, y + 354, 106, 22,
                Component.translatable("gui.done"), btn -> onClose()));
        addRenderableWidget(new StyledButton(x + 555, y + 15, 22, 22,
                Component.literal("×"), btn -> onClose()));
        updateToggleButton();
        updateMoveButtons();
        updateYuqiModeButton();
    }

    private static Component text(String key) {
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key);
    }

    private int panelW() { return PANEL_W; }
    private int panelH() { return PANEL_H; }
    private int panelX() {
        int wheelWidth = Math.round((2 * WHEEL_RADIUS + WHEEL_SLOT_SIZE) * (PANEL_W / (float) DESIGN_W));
        // Center the wheel and panel together instead of pinning the panel to the right edge.
        return (width - PANEL_W + wheelWidth + WHEEL_PANEL_GAP) / 2;
    }
    private int panelY() { return (height - PANEL_H) / 2; }
    private int listTop() { return panelY() + 101; }
    private int listBottom() { return listTop() + 192; }

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
                // Registered shentong have their own controls, not ordinary spell-list rows.
                .filter(spell -> ShentongRegistry.get(spell.id()) == null)
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
    protected void renderMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = panelX();
        int y = panelY();
        int panelWidth = panelW();
        int panelHeight = panelH();

        guiGraphics.fill(0, 0, width, height, 0x90101716);
        CultivationGuiTextures.drawLandscape(guiGraphics, x, y, panelWidth, panelHeight);
        guiGraphics.drawString(font, title, x + (panelWidth - font.width(title)) / 2,
                y + 19, 0xFF3D3A32, false);
        CultivationGuiTextures.drawInnerBox(guiGraphics, x + 24, y + 58, 552, 27);
        CultivationGuiTextures.drawInnerBox(guiGraphics, x + 24, y + 95, 328, 204);
        CultivationGuiTextures.drawInnerBox(guiGraphics, x + 364, y + 95, 212, 250);
        CultivationGuiTextures.drawInnerBox(guiGraphics, x + 24, y + 305, 328, 46);

        if (listWidget != null) {
            for (SpellListEntry entry : listWidget.children()) {
                entry.rendered = false;
            }
        }

        super.renderMenu(guiGraphics, mouseX, mouseY, partialTick);
        renderWheelOrderCircle(guiGraphics, mouseX, mouseY);

        Component wheelCount = Component.literal(ClientData.selectedWheelSpellCount() + "/" + ClientData.MAX_WHEEL_SPELLS);
        CultivationGuiTextures.drawInnerBox(guiGraphics, x + 496, y + 15, 50, 22);
        guiGraphics.drawString(font, wheelCount, x + 521 - font.width(wheelCount) / 2,
                y + 22, 0xFF3D3A32, false);

        renderActiveToggleSpells(guiGraphics, x + 34, y + 67, 532);
        renderSpellDetails(guiGraphics, x + 376, y + 104, 188);
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
            guiGraphics.drawString(font, none, x + 188 - font.width(none) / 2, y + 191, 0xFF756F63, false);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double menuX = toMenuCoordinate(mouseX), menuY = toMenuCoordinate(mouseY);
        if (button == 0 && draggedCircleSpellId != null) {
            int target = wheelCircleTarget(menuX, menuY);
            if (target >= 0) {
                dragTargetWheelIndex = target;
            }
            return true;
        }
        if (button == 0 && draggedWheelEntry != null) {
            int listTop = listTop();
            int listBottom = listBottom();
            if (menuY < listTop + sy(24)) {
                listWidget.setScrollAmount(listWidget.getScrollAmount() - sy(12));
            } else if (menuY > listBottom - sy(24)) {
                listWidget.setScrollAmount(listWidget.getScrollAmount() + sy(12));
            }
            int target = wheelDragTarget(menuY);
            if (target >= 0) {
                dragTargetWheelIndex = target;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double menuX = toMenuCoordinate(mouseX), menuY = toMenuCoordinate(mouseY);
        if (button == 0 && isWheelOrderDragging()) {
            boolean circleDrag = draggedCircleSpellId != null;
            String draggedId = circleDrag ? draggedCircleSpellId : draggedWheelEntry.entry.id();
            int current = ClientData.selectedWheelSpellIndex(draggedId);
            int target = circleDrag ? wheelCircleTarget(menuX, menuY) : wheelDragTarget(menuY);
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
        int listTop = listTop();
        int listBottom = listBottom();
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
        int extent = wheelRadius() + (wheelSlotSize() + 1) / 2;
        return Math.max(SCREEN_MARGIN + extent, panelX() - WHEEL_PANEL_GAP - extent);
    }

    private int wheelCenterY() {
        return panelY() + 204;
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

        Component line = text("active_toggle_spells").copy().append(" ").append(names);

        drawClippedString(guiGraphics, line, x, y, x + maxWidth, 0xFF3D3A32);
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
        addIfActive(active, ModSpells.HUTI_QI, ModEffects.HUTI_QI);
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

    private void renderSpellDetails(GuiGraphics g, int x, int y, int maxWidth) {
        g.drawString(font, text("description_label"), x, y, 0xFF9B7C43, false);
        g.fill(x, y + 12, x + maxWidth, y + 13, 0x669B8A65);
        if (selectedEntry == null) {
            CultivationGuiText.drawFittedWrapped(g, font, text("select_spell_hint"),
                    x, y + 28, maxWidth, 55, 0xFF756F63, false);
            return;
        }
        CultivationGuiTextures.drawInnerBox(g, x, y + 18, 30, 30);
        TechniqueIcons.blitCentered(g, selectedEntry.icon(), x + 3, y + 21, 24, 24);
        CultivationGuiText.drawFittedSingle(g, font, selectedEntry.name(),
                x + 36, y + 29, maxWidth - 36, selectedEntry.nameColor(), false);
        CultivationGuiText.drawFittedWrapped(g, font, selectedEntry.description(),
                x, y + 55, maxWidth, 57, 0xFF4A443A, false);
        g.fill(x, y + 119, x + maxWidth, y + 120, 0x669B8A65);
        renderAttribute(g, text("qi_cost"), selectedEntry.costText(), x, y + 132, maxWidth);
        renderAttribute(g, text("realm_requirement"), selectedEntry.requirementText(), x, y + 152, maxWidth);
        renderAttribute(g, text("proficiency"), selectedEntry.proficiencyText(), x, y + 172, maxWidth);
        renderAttribute(g, text("cooldown_label"), selectedEntry.cooldownText(), x, y + 192, maxWidth);
    }

    private void renderAttribute(GuiGraphics g, Component label, String value, int x, int y, int width) {
        CultivationGuiText.drawFittedSingle(g, font, label, x, y, 89, 0xFF756F63, false);
        CultivationGuiText.drawFittedSingle(g, font, Component.literal(value),
                x + 96, y, width - 96, 0xFF465A52, false);
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

    private void changeMethodPage(int delta) {
        methodPage=Math.clamp(methodPage+delta,0,methodPages-1);
        methodHitboxes.clear();lastMethodClickId="";
    }

    private List<List<MethodHitbox>> methodLayout() {
        List<List<MethodHitbox>> pages=new ArrayList<>();pages.add(new ArrayList<>());
        int left=panelX()+32,right=panelX()+344,x=left,row=0;
        for(var method:learnedMethods()) {
            int itemWidth=Math.min(right-left,font.width(Component.translatable(method.nameKey()))+18);
            if(x+itemWidth>right){x=left;row++;}
            if(row>=2){pages.add(new ArrayList<>());row=0;x=left;}
            pages.getLast().add(new MethodHitbox(method.id(),x,panelY()+323+row*13,itemWidth,13));
            x+=itemWidth+4;
        }
        return pages;
    }

    private void renderLearnedMethods(GuiGraphics graphics,int mouseX,int mouseY) {
        methodHitboxes.clear();var pages=methodLayout();methodPages=pages.size();
        methodPage=Math.clamp(methodPage,0,methodPages-1);
        previousMethodsButton.visible=nextMethodsButton.visible=methodPages>1;
        previousMethodsButton.active=methodPage>0;nextMethodsButton.active=methodPage<methodPages-1;
        CultivationGuiText.drawFittedSingle(graphics,font,text("learned_methods"),panelX()+32,panelY()+310,
                methodPages>1?230:304,0xFF665A46,false);
        if(methodPages>1)graphics.drawCenteredString(font,Component.literal((methodPage+1)+"/"+methodPages),panelX()+307,panelY()+310,0xFF665A46);
        methodHitboxes.addAll(pages.get(methodPage));
        if(methodHitboxes.isEmpty())graphics.drawString(font,text("debug_no_learned_methods"),panelX()+32,panelY()+329,0xFF756F63,false);
        List<Component> hoveredTooltip=null;
        for(var hitbox:methodHitboxes) {
            var method=CultivationMethods.get(hitbox.methodId());Component name=Component.translatable(method.nameKey());
            boolean active=method.id().equals(ClientData.cultivationData.activeCultivationMethod()),hovered=hitbox.contains(mouseX,mouseY);
            graphics.fill(hitbox.x(),hitbox.y(),hitbox.x()+hitbox.width(),hitbox.y()+hitbox.height(),active?0x78588968:hovered?0x44588968:0x247E765F);
            graphics.renderOutline(hitbox.x(),hitbox.y(),hitbox.width(),hitbox.height(),active?0xFF416C4A:0x55725F3A);
            Component label=active?Component.literal("✓ ").append(name):name;
            CultivationGuiText.drawFittedSingle(graphics,font,label,hitbox.x()+5,hitbox.y()+2,hitbox.width()-10,active?0xFF294D3A:0xFF453F35,false);
            if(hovered){
                List<Component> tooltip=new ArrayList<>();tooltip.add(name);
                if(active)tooltip.add(text("method_active"));
                tooltip.add(text("method_switch_hint"));
                hoveredTooltip=tooltip;
            }
        }
        if(hoveredTooltip!=null)graphics.renderTooltip(font,hoveredTooltip,java.util.Optional.empty(),mouseX,mouseY);
    }

    @Override public void tick() {
        super.tick();if(ClientData.cultivationData==null||listWidget==null)return;
        var spells=knownSpells().stream().map(ModSpells.SpellDef::id).toList();String method=ClientData.cultivationData.activeCultivationMethod();
        if(spells.equals(displayedSpellIds)&&method.equals(displayedMethod))return;
        displayedSpellIds=spells;displayedMethod=method;
        ClientData.reconcileWheelSelection(ClientData.cultivationData.knownSpells());
        String selected=selectedEntry==null?"":selectedEntry.id();allEntries.clear();allEntries.addAll(getKnownEntries());
        selectedEntry=allEntries.stream().filter(entry->entry.id().equals(selected)).findFirst().orElse(null);
        populateList();updateToggleButton();updateMoveButtons();updateYuqiModeButton();
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        double x=toMenuCoordinate(mouseX),y=toMenuCoordinate(mouseY);
        if(methodPages>1&&vertical!=0&&x>=panelX()+24&&x<panelX()+352&&y>=panelY()+305&&y<panelY()+351){changeMethodPage(vertical>0?-1:1);return true;}
        return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double menuX = toMenuCoordinate(mouseX), menuY = toMenuCoordinate(mouseY);
        if(button==0||button==1)for(var hitbox:methodHitboxes) {
            if(!hitbox.contains(menuX,menuY))continue;
            long now=System.currentTimeMillis();
            if(button==1||hitbox.methodId().equals(lastMethodClickId)&&now-lastMethodClickMillis<=350L) {
                minecraft.setScreen(TechniqueDescriptionScreen.method(this,hitbox.methodId()));
                lastMethodClickId="";
            }else{
                if(!hitbox.methodId().equals(ClientData.cultivationData.activeCultivationMethod()))
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundActivateMethodPayload(hitbox.methodId()));
                lastMethodClickId=hitbox.methodId();lastMethodClickMillis=now;
            }
            return true;
        }
        if (button == 0) {
            int circleIndex = wheelCircleTarget(menuX, menuY);
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
                    if (row.isHandleHovered(menuX, menuY)
                            && ClientData.isSpellVisibleInWheel(row.entry.id())) {
                        draggedWheelEntry = row;
                        dragTargetWheelIndex = ClientData.selectedWheelSpellIndex(row.entry.id());
                        selectEntry(row.entry);
                        return true;
                    }
                }
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
            CultivationGuiTextures.drawButton(g, getX(), getY(), width, height, active, isHoveredOrFocused());
            CultivationGuiTextures.drawButtonText(g, getMessage(), getX(), getY(), width, height, active);
        }
    }

    private class SpellListWidget extends ObjectSelectionList<SpellListEntry> {
        public SpellListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return width - 8;
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {
        }

        @Override
        protected void enableScissor(GuiGraphics graphics) {
            var pose = graphics.pose().last().pose();
            var topLeft = new org.joml.Vector3f(getX(), getY(), 0).mulPosition(pose);
            var bottomRight = new org.joml.Vector3f(getRight(), getBottom(), 0).mulPosition(pose);
            graphics.enableScissor((int)Math.floor(topLeft.x()), (int)Math.floor(topLeft.y()),
                    (int)Math.ceil(bottomRight.x()), (int)Math.ceil(bottomRight.y()));
        }

        @Override
        protected boolean isValidMouseClick(int button) {
            return button == 0 || button == 1;
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
            int rowHeight = 32;
            int rowY = top + (height - rowHeight) / 2;
            this.rendered = true;
            this.rowY = rowY;
            this.rowHeight = rowHeight;
            CultivationGuiTextures.drawInnerBox(guiGraphics, left, rowY, width, rowHeight);
            if (selectedEntry == entry) {
                guiGraphics.fill(left, rowY, left + width, rowY + rowHeight, 0x6638FF88);
            } else if (hovering) {
                guiGraphics.fill(left, rowY, left + width, rowY + rowHeight, 0x2238FF88);
            }

            int wheelIndex = ClientData.selectedWheelSpellIndex(entry.id());
            String marker = visible && wheelIndex >= 0 ? "[" + (wheelIndex + 1) + "]" : "[ ]";
            int markerColor = visible ? 0xFF2E6E50 : 0xFF8A8375;

            int slotX = left + 5;
            int slotY = rowY + 4;
            int slotSize = 24;
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
            int listTop = listTop();
            int listBottom = listBottom();
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
