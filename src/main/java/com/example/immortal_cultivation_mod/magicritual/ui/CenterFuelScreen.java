package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.client.FormationLayers;

import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterType;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.TeleportDestinationInfo;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundCenterSettingsPayload;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
import com.example.immortal_cultivation_mod.screen.CultivationGuiTextures;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CenterFuelScreen extends AbstractContainerScreen<CenterFuelMenu> {
   private static final String NS = "minecraft";
   private static final ResourceLocation TEX_SLOT = tex("slot_default");
   private static final ResourceLocation TEX_PSLOT = tex("player_slots_9x4");
   private static final ResourceLocation TEX_FILL = tex("fill_white");
   private static final ResourceLocation TEX_SPLIT = tex("split_bar");
   private static final ResourceLocation TEX_TL = tex("border_corner_tl");
   private static final ResourceLocation TEX_TR = tex("border_corner_tr");
   private static final ResourceLocation TEX_BL = tex("border_corner_bl");
   private static final ResourceLocation TEX_BR = tex("border_corner_br");
   private static final ResourceLocation TEX_ET = tex("border_edge_top");
   private static final ResourceLocation TEX_EB = tex("border_edge_bottom");
   private static final ResourceLocation TEX_EL = tex("border_edge_left");
   private static final ResourceLocation TEX_ER = tex("border_edge_right");

   private static final int BORDER = 5;
   private static final int FUEL_ROW_H = 18;
   private static final int TOGGLE_H = 18;
   private static final int LABEL_H = 8;
   private static final int[] GLYPH_COLORS = new int[]{16777215, 16728128, 16766042, 5627255, 5629670, 5607679, 12151039, 16740552};
   private static final String[] GLYPH_COLOR_KEYS = new String[]{"white", "red", "gold", "green", "cyan", "blue", "purple", "pink"};
   private static final int INPUT_H = 20;
   private static final int SPLIT_H = 14;
   private static final int PLAYER_INV_H = 76;
   private int entryStartY;
   private int splitY;
   private int playerInvTop;
   private Button hostileBtn;
   private Button passiveBtn;
   private EditBox nameInput;
   private EditBox controllerInput;
   private Button blacklistAddBtn;
   private Button controllerAddBtn;
   private final List<Button> removeButtons = new ArrayList();
   private final List<Button> controllerRemoveButtons = new ArrayList();
   private int blacklistEntryIndex;
   private int controllerEntryIndex;
   private int seenAccessRevision;
   private Button particlesBtn;
   private Button enabledBtn;
   private Button teleportModeBtn;
   private Button radiusDownBtn;
   private Button radiusUpBtn;
   private EditBox radiusInput;
   private Button radiusApplyBtn;
   private final List<ColorSwatchButton> colorButtons = new ArrayList();
   private Button settingsTabBtn;
   private Button octagonTabBtn;
   private Button teleportTabBtn;
   private boolean octagonTab;
   private boolean teleportTab;
   private EditBox teleportCodeInput;
   private Button teleportCodeAddBtn;
   private Button teleportCodeCopyBtn;
   private Button teleportActivateBtn;
   private final List<Button> teleportDestinationButtons = new ArrayList();
   private final List<Button> teleportRemoveButtons = new ArrayList();
   private int teleportDestinationScroll;
   private int seenTeleportRevision;
   private static final int TELEPORT_ROWS = 5;

   private static ResourceLocation tex(String name) {
      return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/" + name + ".png");
   }


   private String labelParticles() {
      return "Particles:" + (((CenterFuelMenu)this.menu).particlesEnabled ? "ON" : "OFF");
   }

   public CenterFuelScreen(CenterFuelMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = 420;
      this.imageHeight = 354;
      this.inventoryLabelX = 129;
      this.inventoryLabelY = 253;
   }

   protected void init() {
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      this.entryStartY = y + 204;
      int inputY = y + 226;
      this.splitY = y + 249;
      this.playerInvTop = y + 266;
      int toggleW = 88;
      int toggleGap = 6;
      int toggleX = x + (this.imageWidth - toggleW * 4 - toggleGap * 3) / 2;
      int toggleY = y + 142;
      this.hostileBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(toggleX, toggleY, toggleW, 18, Component.translatable("gui.immortal_cultivation_mod.hostile_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).allowHostileMobs)}), (b) -> {
         PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_hostile", ""), new CustomPacketPayload[0]);
         ((CenterFuelMenu)this.menu).allowHostileMobs = !((CenterFuelMenu)this.menu).allowHostileMobs;
         b.setMessage(Component.translatable("gui.immortal_cultivation_mod.hostile_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).allowHostileMobs)}));
      }));
      this.passiveBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(toggleX + toggleW + toggleGap, toggleY, toggleW, 18, Component.translatable("gui.immortal_cultivation_mod.passive_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).allowPassiveMobs)}), (b) -> {
         PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_passive", ""), new CustomPacketPayload[0]);
         ((CenterFuelMenu)this.menu).allowPassiveMobs = !((CenterFuelMenu)this.menu).allowPassiveMobs;
         b.setMessage(Component.translatable("gui.immortal_cultivation_mod.passive_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).allowPassiveMobs)}));
      }));
      this.particlesBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(toggleX + (toggleW + toggleGap) * 2, toggleY, toggleW, 18, Component.translatable("gui.immortal_cultivation_mod.glyph_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).particlesEnabled)}), (b) -> {
         PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_particles", ""), new CustomPacketPayload[0]);
         ((CenterFuelMenu)this.menu).particlesEnabled = !((CenterFuelMenu)this.menu).particlesEnabled;
         b.setMessage(Component.translatable("gui.immortal_cultivation_mod.glyph_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).particlesEnabled)}));
      }));
      this.enabledBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(toggleX + (toggleW + toggleGap) * 3, toggleY, toggleW, 18, Component.translatable("gui.immortal_cultivation_mod.center_enabled_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).displayedEnabled())}), (b) -> {
         PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_enabled", ""), new CustomPacketPayload[0]);
         ((CenterFuelMenu)this.menu).enabled = !((CenterFuelMenu)this.menu).displayedEnabled();
         b.setMessage(Component.translatable("gui.immortal_cultivation_mod.center_enabled_toggle", new Object[]{this.stateLabel(((CenterFuelMenu)this.menu).enabled)}));
      }));
      this.teleportModeBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(toggleX, toggleY, toggleW * 2 + toggleGap, 18, Component.translatable("gui.immortal_cultivation_mod.center_tab_teleport"), (buttonx) -> this.switchTab(false, true)));
      this.radiusDownBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(x + 226, y + 169, 28, 18, Component.literal("-"), (buttonx) -> this.changeRadius(-2)));
      this.radiusInput = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 260, y + 169, 54, 18, Component.translatable("gui.immortal_cultivation_mod.center_radius", new Object[]{((CenterFuelMenu)this.menu).displayedRadius()})));
      this.radiusInput.setMaxLength(3);
      this.radiusInput.setFilter((value) -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
      this.radiusInput.setValue(Integer.toString(((CenterFuelMenu)this.menu).displayedRadius()));
      this.radiusInput.setHint(Component.literal("3-303"));
      this.radiusApplyBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(x + 320, y + 169, 38, 18, Component.translatable("gui.ok"), (buttonx) -> this.applyTypedRadius()));
      this.radiusUpBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(x + 364, y + 169, 28, 18, Component.literal("+"), (buttonx) -> this.changeRadius(2)));
      MagicCenterType centerType = MagicCenterTypeRegistry.getOrBlank(((CenterFuelMenu)this.menu).centerTypeId);
      this.hostileBtn.visible = centerType.behavior().supportsHostileMobSetting();
      this.passiveBtn.visible = centerType.behavior().supportsPassiveMobSetting();
      this.colorButtons.clear();
      int swatchX = x + 24;

      for(int i = 0; i < GLYPH_COLORS.length; ++i) {
         int color = GLYPH_COLORS[i];
         ColorSwatchButton button = (ColorSwatchButton)this.addRenderableWidget(new ColorSwatchButton(swatchX + i * 24, y + 171, 19, 14, color, () -> this.selectColor(color)));
         this.colorButtons.add(button);
      }

      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int inputW = columnW - 66;
      int inputX = x + 17;
      int controllerX = inputX + columnW + columnGap;
      this.nameInput = (EditBox)this.addRenderableWidget(new EditBox(this.font, inputX, inputY, inputW, 16, Component.literal("")));
      this.nameInput.setMaxLength(16);
      this.blacklistAddBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(inputX + inputW + 6, inputY - 2, 60, 20, Component.translatable("gui.immortal_cultivation_mod.add"), (b) -> {
         String name = this.nameInput.getValue().trim();
         if (isValidPlayerName(name) && ((CenterFuelMenu)this.menu).blacklistedPlayers.size() < 64 && !containsIgnoreCase(((CenterFuelMenu)this.menu).blacklistedPlayers, name)) {
            PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "add_blacklist", name), new CustomPacketPayload[0]);
            this.nameInput.setValue("");
         }

      }));
      this.controllerInput = (EditBox)this.addRenderableWidget(new EditBox(this.font, controllerX, inputY, inputW, 16, Component.literal("")));
      this.controllerInput.setMaxLength(16);
      this.controllerAddBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(controllerX + inputW + 6, inputY - 2, 60, 20, Component.translatable("gui.immortal_cultivation_mod.add"), (b) -> {
         String name = this.controllerInput.getValue().trim();
         if (isValidPlayerName(name) && ((CenterFuelMenu)this.menu).controllerPlayers.size() < 16 && !containsIgnoreCase(((CenterFuelMenu)this.menu).controllerPlayers, name)) {
            PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "add_controller", name), new CustomPacketPayload[0]);
            this.controllerInput.setValue("");
         }

      }));
      this.teleportCodeCopyBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(x + 326, y + 40, 72, 18, Component.translatable("gui.immortal_cultivation_mod.teleport_code.copy"), (buttonx) -> {
         Minecraft.getInstance().keyboardHandler.setClipboard(((CenterFuelMenu)this.menu).teleportCode);
         buttonx.setMessage(Component.translatable("gui.immortal_cultivation_mod.teleport_code.copied"));
      }));
      this.teleportCodeInput = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 18, y + 75, 298, 18, Component.translatable("gui.immortal_cultivation_mod.teleport_code.input")));
      this.teleportCodeInput.setMaxLength(64);
      this.teleportCodeInput.setFilter((value) -> value.isEmpty() || value.matches("[A-Za-z0-9\\- ]*"));
      this.teleportCodeInput.setHint(Component.translatable("gui.immortal_cultivation_mod.teleport_code.input_hint"));
      this.teleportCodeAddBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(x + 322, y + 74, 76, 20, Component.translatable("gui.immortal_cultivation_mod.teleport_code.add"), (buttonx) -> this.addTeleportDestination()));
      this.teleportActivateBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(x + 286, y + 221, 112, 20, Component.translatable("gui.immortal_cultivation_mod.teleport_code.teleport"), (buttonx) -> PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "begin_teleport", ""), new CustomPacketPayload[0])));
      boolean canControl = ((CenterFuelMenu)this.menu).canControlCenter();
      boolean canManageControllers = ((CenterFuelMenu)this.menu).canManageControllers();
      this.hostileBtn.active = canControl;
      this.passiveBtn.active = canControl;
      this.particlesBtn.active = canControl;
      this.enabledBtn.active = canControl;
      this.teleportModeBtn.active = canControl;
      this.radiusDownBtn.active = canControl;
      this.radiusUpBtn.active = canControl;
      this.radiusInput.active = canControl;
      this.radiusApplyBtn.active = canControl;
      this.colorButtons.forEach((buttonx) -> buttonx.active = canControl);
      this.nameInput.active = canControl;
      this.blacklistAddBtn.active = canControl;
      this.controllerInput.active = canManageControllers;
      this.controllerAddBtn.active = canManageControllers;
      this.teleportCodeAddBtn.active = canManageControllers;
      this.teleportActivateBtn.active = canControl && this.hasAvailableSelectedDestination();
      this.refreshRemoveButtons();
      this.refreshControllerRemoveButtons();
      this.refreshTeleportDestinationButtons();
      this.seenAccessRevision = ((CenterFuelMenu)this.menu).accessRevision();
      this.seenTeleportRevision = ((CenterFuelMenu)this.menu).teleportRevision();
      int tabX = x + this.imageWidth - 4;
      int tabY = y + 46;
      this.settingsTabBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(tabX, tabY, 70, 20, Component.translatable("gui.immortal_cultivation_mod.center_tab_settings"), (buttonx) -> this.switchTab(false)));
      this.octagonTabBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(tabX, tabY + 23, 70, 20, Component.translatable("gui.immortal_cultivation_mod.center_tab_octagon"), (buttonx) -> {
         this.switchTab(true);
         PacketDistributor.sendToServer(new DaoGuidancePayloads.ServerboundDaoGuidanceActionPayload("open_flag_gui"), new CustomPacketPayload[0]);
      }));
      this.teleportTabBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(tabX, tabY + 46, 70, 20, Component.translatable("gui.immortal_cultivation_mod.center_tab_teleport"), (buttonx) -> this.switchTab(false, true)));
      this.switchTab(this.octagonTab, this.teleportTab);
   }

   private void switchTab(boolean octagon) {
      this.switchTab(octagon, false);
   }

   private void switchTab(boolean octagon, boolean teleport) {
      boolean wasTeleportTab = this.teleportTab;
      this.octagonTab = octagon;
      this.teleportTab = teleport && MagicCenterTypeRegistry.TELEPORT.equals(((CenterFuelMenu)this.menu).centerTypeId);
      ((CenterFuelMenu)this.menu).setOctagonTabActive(octagon);
      ((CenterFuelMenu)this.menu).setTeleportTabActive(this.teleportTab);
      boolean settingsVisible = !octagon && !this.teleportTab;
      MagicCenterType centerType = MagicCenterTypeRegistry.getOrBlank(((CenterFuelMenu)this.menu).centerTypeId);
      this.hostileBtn.visible = settingsVisible && centerType.behavior().supportsHostileMobSetting();
      this.passiveBtn.visible = settingsVisible && centerType.behavior().supportsPassiveMobSetting();
      this.particlesBtn.visible = settingsVisible;
      this.enabledBtn.visible = settingsVisible;
      this.teleportModeBtn.visible = settingsVisible && MagicCenterTypeRegistry.TELEPORT.equals(((CenterFuelMenu)this.menu).centerTypeId);
      this.radiusDownBtn.visible = settingsVisible;
      this.radiusUpBtn.visible = settingsVisible;
      this.radiusInput.visible = settingsVisible;
      this.radiusApplyBtn.visible = settingsVisible;
      this.colorButtons.forEach((button) -> button.visible = settingsVisible);
      this.nameInput.visible = settingsVisible;
      this.blacklistAddBtn.visible = settingsVisible;
      this.controllerInput.visible = settingsVisible;
      this.controllerAddBtn.visible = settingsVisible;
      this.removeButtons.forEach((button) -> button.visible = settingsVisible);
      this.controllerRemoveButtons.forEach((button) -> button.visible = settingsVisible);
      this.teleportCodeInput.visible = this.teleportTab;
      this.teleportCodeAddBtn.visible = this.teleportTab;
      this.teleportCodeCopyBtn.visible = this.teleportTab;
      this.teleportActivateBtn.visible = this.teleportTab;
      this.teleportDestinationButtons.forEach((button) -> button.visible = this.teleportTab);
      this.teleportRemoveButtons.forEach((button) -> button.visible = this.teleportTab);
      if (octagon || this.teleportTab) {
         this.nameInput.setFocused(false);
         this.controllerInput.setFocused(false);
         this.radiusInput.setFocused(false);
      }

      if (!this.teleportTab) {
         this.teleportCodeInput.setFocused(false);
      }

      if (this.settingsTabBtn != null) {
         this.settingsTabBtn.visible = true;
         this.settingsTabBtn.active = octagon || this.teleportTab;
      }

      if (this.octagonTabBtn != null) {
         this.octagonTabBtn.visible = true;
         this.octagonTabBtn.active = !octagon;
      }

      if (this.teleportTabBtn != null) {
         this.teleportTabBtn.visible = MagicCenterTypeRegistry.TELEPORT.equals(((CenterFuelMenu)this.menu).centerTypeId);
         this.teleportTabBtn.active = !this.teleportTab;
      }

      if (this.teleportTab && !wasTeleportTab) {
         PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "refresh_teleport_destinations", ""), new CustomPacketPayload[0]);
      }

   }

   protected void containerTick() {
      super.containerTick();
      if (this.seenAccessRevision != ((CenterFuelMenu)this.menu).accessRevision()) {
         this.seenAccessRevision = ((CenterFuelMenu)this.menu).accessRevision();
         this.refreshRemoveButtons();
         this.refreshControllerRemoveButtons();
      }

      if (this.seenTeleportRevision != ((CenterFuelMenu)this.menu).teleportRevision()) {
         this.seenTeleportRevision = ((CenterFuelMenu)this.menu).teleportRevision();
         this.refreshTeleportDestinationButtons();
         this.switchTab(this.octagonTab, this.teleportTab);
      }

      if (this.radiusInput != null && !this.radiusInput.isFocused()) {
         String currentValue = Integer.toString(((CenterFuelMenu)this.menu).displayedRadius());
         if (!this.radiusInput.getValue().equals(currentValue)) {
            this.radiusInput.setValue(currentValue);
         }
      }

      boolean canControl = ((CenterFuelMenu)this.menu).canControlCenter();
      if (this.teleportModeBtn != null) {
         this.teleportModeBtn.active = canControl;
         this.teleportModeBtn.setMessage(Component.translatable("gui.immortal_cultivation_mod.center_tab_teleport"));
      }

      if (this.teleportCodeAddBtn != null) {
         this.teleportCodeAddBtn.active = ((CenterFuelMenu)this.menu).canManageControllers();
      }

      if (this.teleportActivateBtn != null) {
         this.teleportActivateBtn.active = canControl && this.hasAvailableSelectedDestination();
      }

      if (this.radiusDownBtn != null) {
         this.radiusDownBtn.active = canControl && ((CenterFuelMenu)this.menu).displayedRadius() > 3;
      }

      if (this.radiusUpBtn != null) {
         this.radiusUpBtn.active = canControl && ((CenterFuelMenu)this.menu).displayedRadius() < 303;
      }

   }

   private void changeRadius(int amount) {
      this.commitRadius(((CenterFuelMenu)this.menu).displayedRadius() + amount);
   }

   private void applyTypedRadius() {
      String text = this.radiusInput.getValue().trim();
      if (text.isEmpty()) {
         this.radiusInput.setValue(Integer.toString(((CenterFuelMenu)this.menu).displayedRadius()));
      } else {
         try {
            this.commitRadius(Integer.parseInt(text));
         } catch (NumberFormatException var3) {
            this.radiusInput.setValue(Integer.toString(((CenterFuelMenu)this.menu).displayedRadius()));
         }

      }
   }

   private void commitRadius(int requestedRadius) {
      int radius = MagicCenterBlockEntity.normalizeRadius(requestedRadius);
      ((CenterFuelMenu)this.menu).setDisplayedRadius(radius);
      this.radiusInput.setValue(Integer.toString(radius));
      PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "set_radius", Integer.toString(radius)), new CustomPacketPayload[0]);
   }

   private void addTeleportDestination() {
      String code = this.teleportCodeInput.getValue().trim();
      if (!code.isEmpty()) {
         PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "add_teleport_destination", code), new CustomPacketPayload[0]);
         this.teleportCodeInput.setValue("");
         this.teleportCodeInput.setFocused(false);
      }
   }

   private void refreshTeleportDestinationButtons() {
      this.teleportDestinationButtons.forEach((x$0) -> this.removeWidget(x$0));
      this.teleportDestinationButtons.clear();
      this.teleportRemoveButtons.forEach((x$0) -> this.removeWidget(x$0));
      this.teleportRemoveButtons.clear();
      int size = ((CenterFuelMenu)this.menu).teleportDestinations.size();
      int maxScroll = Math.max(0, size - 5);
      this.teleportDestinationScroll = Math.max(0, Math.min(this.teleportDestinationScroll, maxScroll));
      int first = this.teleportDestinationScroll;
      int last = Math.min(size, first + 5);

      for(int index = first; index < last; ++index) {
         TeleportDestinationInfo destination = (TeleportDestinationInfo)((CenterFuelMenu)this.menu).teleportDestinations.get(index);
         boolean selected = destination.code().equals(((CenterFuelMenu)this.menu).selectedTeleportDestinationCode);
         int row = index - first;
         int rowY = this.topPos + 101 + row * 23;
         String name = this.font.plainSubstrByWidth(destination.label(), 202);
         Component cost = destination.available() ? Component.translatable("gui.immortal_cultivation_mod.teleport_code.cost", new Object[]{destination.qiCost(), formatNumber(destination.energyCost())}) : Component.translatable("gui.immortal_cultivation_mod.teleport_code.unavailable");
         Component label = Component.literal(selected ? "\u25b6 " : "  ").append(Component.literal(name + "  ")).append(cost);
         Button destinationButton = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(this.leftPos + 18, rowY, 340, 20, label, (button) -> {
            if (destination.available()) {
               ((CenterFuelMenu)this.menu).selectedTeleportDestinationCode = destination.code();
               PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "select_teleport_destination", destination.code()), new CustomPacketPayload[0]);
               this.refreshTeleportDestinationButtons();
               this.switchTab(false, true);
            }
         }));
         destinationButton.active = destination.available() && ((CenterFuelMenu)this.menu).canControlCenter();
         destinationButton.visible = this.teleportTab;
         this.teleportDestinationButtons.add(destinationButton);
         Button removeButton = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(this.leftPos + 363, rowY, 35, 20, Component.literal("\u00d7"), (button) -> PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "remove_teleport_destination", destination.code()), new CustomPacketPayload[0])));
         removeButton.active = ((CenterFuelMenu)this.menu).canManageControllers();
         removeButton.visible = this.teleportTab;
         this.teleportRemoveButtons.add(removeButton);
      }

   }

   private static String formatNumber(long value) {
      return String.format(Locale.ROOT, "%,d", Math.max(0L, value));
   }

   private boolean hasAvailableSelectedDestination() {
      return ((CenterFuelMenu)this.menu).teleportDestinations.stream().anyMatch((destination) -> destination.available() && destination.code().equals(((CenterFuelMenu)this.menu).selectedTeleportDestinationCode));
   }

   private void refreshRemoveButtons() {
      this.removeButtons.forEach((x$0) -> this.removeWidget(x$0));
      this.removeButtons.clear();
      int x = this.leftPos;
      int btnW = 60;
      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int columnX = x + 17;
      int btnX = x + 17 + columnW - btnW;
      List<String> names = sortedNames(((CenterFuelMenu)this.menu).blacklistedPlayers);
      if (names.isEmpty()) {
         this.blacklistEntryIndex = 0;
      } else {
         this.blacklistEntryIndex = Math.min(this.blacklistEntryIndex, names.size() - 1);
         String playerName = (String)names.get(this.blacklistEntryIndex);
         if (names.size() > 1) {
            this.addAccessNavigationButtons(this.removeButtons, columnX, btnX, () -> {
               this.blacklistEntryIndex = Math.floorMod(this.blacklistEntryIndex - 1, names.size());
               this.refreshRemoveButtons();
            }, () -> {
               this.blacklistEntryIndex = (this.blacklistEntryIndex + 1) % names.size();
               this.refreshRemoveButtons();
            });
         }

         Button removeBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(btnX, this.entryStartY + 1, btnW, 18, Component.translatable("gui.immortal_cultivation_mod.remove"), (b) -> PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "remove_blacklist", playerName), new CustomPacketPayload[0])));
         removeBtn.active = ((CenterFuelMenu)this.menu).canControlCenter();
         removeBtn.visible = !this.octagonTab && !this.teleportTab;
         this.removeButtons.add(removeBtn);
      }
   }

   private void refreshControllerRemoveButtons() {
      this.controllerRemoveButtons.forEach((x$0) -> this.removeWidget(x$0));
      this.controllerRemoveButtons.clear();
      int x = this.leftPos;
      int btnW = 60;
      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int controllerX = x + 17 + columnW + columnGap;
      int btnX = controllerX + columnW - btnW;
      List<String> names = sortedNames(((CenterFuelMenu)this.menu).controllerPlayers);
      if (names.isEmpty()) {
         this.controllerEntryIndex = 0;
      } else {
         this.controllerEntryIndex = Math.min(this.controllerEntryIndex, names.size() - 1);
         String playerName = (String)names.get(this.controllerEntryIndex);
         if (names.size() > 1) {
            this.addAccessNavigationButtons(this.controllerRemoveButtons, controllerX, btnX, () -> {
               this.controllerEntryIndex = Math.floorMod(this.controllerEntryIndex - 1, names.size());
               this.refreshControllerRemoveButtons();
            }, () -> {
               this.controllerEntryIndex = (this.controllerEntryIndex + 1) % names.size();
               this.refreshControllerRemoveButtons();
            });
         }

         Button removeBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(btnX, this.entryStartY + 1, btnW, 18, Component.translatable("gui.immortal_cultivation_mod.remove"), (b) -> PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "remove_controller", playerName), new CustomPacketPayload[0])));
         removeBtn.active = ((CenterFuelMenu)this.menu).canManageControllers();
         removeBtn.visible = !this.octagonTab && !this.teleportTab;
         this.controllerRemoveButtons.add(removeBtn);
      }
   }

   private void addAccessNavigationButtons(List<Button> buttons, int columnX, int removeX, Runnable previous, Runnable next) {
      Button previousBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(columnX, this.entryStartY + 1, 16, 18, Component.literal("<"), (button) -> previous.run()));
      Button nextBtn = (Button)this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(removeX - 18, this.entryStartY + 1, 16, 18, Component.literal(">"), (button) -> next.run()));
      previousBtn.visible = !this.octagonTab && !this.teleportTab;
      nextBtn.visible = !this.octagonTab && !this.teleportTab;
      buttons.add(previousBtn);
      buttons.add(nextBtn);
   }

   private static List<String> sortedNames(Set<String> names) {
      List<String> sorted = new ArrayList(names);
      sorted.sort(String.CASE_INSENSITIVE_ORDER);
      return sorted;
   }

   private static boolean isValidPlayerName(String name) {
      return name != null && name.matches("[A-Za-z0-9_]{1,16}");
   }

   private static boolean containsIgnoreCase(Set<String> names, String name) {
      return names.stream().anyMatch((existing) -> existing.equalsIgnoreCase(name));
   }

   private String labelHostile() {
      return "Hostile:" + (((CenterFuelMenu)this.menu).allowHostileMobs ? "ON" : "OFF");
   }

   private Component stateLabel(boolean enabled) {
      return Component.translatable(enabled ? "gui.immortal_cultivation_mod.enabled" : "gui.immortal_cultivation_mod.disabled");
   }

   private Component teleportModeLabel(int mode) {
      String var10000;
      switch (mode) {
         case 0 -> var10000 = "two_way";
         case 1 -> var10000 = "this_source";
         case 2 -> var10000 = "linked_source";
         default -> var10000 = "unlinked";
      }

      String suffix = var10000;
      return Component.translatable("gui.immortal_cultivation_mod.teleport_mode." + suffix);
   }

   private void selectColor(int color) {
      ((CenterFuelMenu)this.menu).glyphColor = color;
      PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "set_glyph_color", String.format("%06X", color)), new CustomPacketPayload[0]);
   }

   private String labelPassive() {
      return "Passive:" + (((CenterFuelMenu)this.menu).allowPassiveMobs ? "ON" : "OFF");
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.radiusInput.isFocused() || keyCode != 257 && keyCode != 335) {
         if (!this.teleportCodeInput.isFocused() || keyCode != 257 && keyCode != 335) {
            EditBox focusedInput = this.nameInput.isFocused() ? this.nameInput : (this.controllerInput.isFocused() ? this.controllerInput : (this.radiusInput.isFocused() ? this.radiusInput : (this.teleportCodeInput.isFocused() ? this.teleportCodeInput : null)));
            if (focusedInput != null) {
               if (focusedInput.keyPressed(keyCode, scanCode, modifiers)) {
                  return true;
               } else if (keyCode == 256) {
                  this.onClose();
                  return true;
               } else {
                  return true;
               }
            } else {
               return super.keyPressed(keyCode, scanCode, modifiers);
            }
         } else {
            this.addTeleportDestination();
            return true;
         }
      } else {
         this.applyTypedRadius();
         this.radiusInput.setFocused(false);
         return true;
      }
   }

   public boolean charTyped(char codePoint, int modifiers) {
      if (this.nameInput.isFocused()) {
         return this.nameInput.charTyped(codePoint, modifiers);
      } else if (this.controllerInput.isFocused()) {
         return this.controllerInput.charTyped(codePoint, modifiers);
      } else if (this.radiusInput.isFocused()) {
         return this.radiusInput.charTyped(codePoint, modifiers);
      } else {
         return this.teleportCodeInput.isFocused() ? this.teleportCodeInput.charTyped(codePoint, modifiers) : super.charTyped(codePoint, modifiers);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.teleportTab && scrollY != (double)0.0F && ((CenterFuelMenu)this.menu).teleportDestinations.size() > 5) {
         int maxScroll = ((CenterFuelMenu)this.menu).teleportDestinations.size() - 5;
         int nextScroll = this.teleportDestinationScroll + (scrollY < (double)0.0F ? 1 : -1);
         nextScroll = Math.max(0, Math.min(nextScroll, maxScroll));
         if (nextScroll != this.teleportDestinationScroll) {
            this.teleportDestinationScroll = nextScroll;
            this.refreshTeleportDestinationButtons();
            this.switchTab(false, true);
         }

         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
      int x = this.leftPos;
      int y = this.topPos;
      int W = this.imageWidth;
      int H = this.imageHeight;
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      CultivationGuiTextures.drawLandscape(g, x, y, W, H);
      if (this.teleportTab) {
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 30, W - 24, 218);
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 263, W - 24, 84);
         g.blit(TEX_SPLIT, x + 5, this.splitY, 0.0F, 0.0F, W - 10, 14, 1, 14);
         int playerInvLeft = x + 129;
         g.blit(TEX_PSLOT, playerInvLeft - 1, this.playerInvTop - 1, 0.0F, 0.0F, 162, 76, 162, 76);
         this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.teleport_code.local", new Object[]{((CenterFuelMenu)this.menu).teleportCode}), x + 18, y + 45, 300);
         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.teleport_code.add_prompt"), x + 18, y + 64, 4210752, false);
         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.teleport_code.destinations"), x + 18, y + 94, 4210752, false);
         if (((CenterFuelMenu)this.menu).teleportDestinations.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.teleport_code.empty"), x + 20, y + 119, 8417119, false);
         }

         int size = ((CenterFuelMenu)this.menu).teleportDestinations.size();
         if (size > 5) {
            int first = this.teleportDestinationScroll + 1;
            int last = Math.min(size, this.teleportDestinationScroll + 5);
            g.drawString(this.font, Component.literal(first + "-" + last + "/" + size), x + 18, y + 228, 8417119, false);
         }

      } else if (this.octagonTab) {
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 30, W - 24, 218);
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 263, W - 24, 84);
         this.renderOctagonGrid(g, x, y);
         g.blit(TEX_SPLIT, x + 5, this.splitY, 0.0F, 0.0F, W - 10, 14, 1, 14);
         int playerInvLeft = x + 129;
         g.blit(TEX_PSLOT, playerInvLeft - 1, this.playerInvTop - 1, 0.0F, 0.0F, 162, 76, 162, 76);
      } else {
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 30, W - 24, 104);
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 136, W - 24, 55);
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 193, W - 24, 55);
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 263, W - 24, 84);
         CultivationGuiTextures.drawInnerBox(g, x + 19, y + 37, 82, 88);
         this.renderCenterGlyph(g, x + 60, y + 80, 68, partialTick);
         int centerSlotCount = !((CenterFuelMenu)this.menu).isReforgeBlade() && !((CenterFuelMenu)this.menu).getSlot(1).hasItem() ? 1 : 2;

         for(int i = 0; i < centerSlotCount; ++i) {
            Slot slot = (Slot)((CenterFuelMenu)this.menu).slots.get(i);
            g.blit(TEX_SLOT, x + slot.x - 1, y + slot.y - 1, 0.0F, 0.0F, 18, 18, 18, 18);
         }

         g.blit(TEX_SPLIT, x + 5, this.splitY, 0.0F, 0.0F, W - 10, 14, 1, 14);
         int playerInvLeft = x + 129;
         g.blit(TEX_PSLOT, playerInvLeft - 1, this.playerInvTop - 1, 0.0F, 0.0F, 162, 76, 162, 76);
         MagicCenterType centerType = MagicCenterTypeRegistry.getOrBlank(((CenterFuelMenu)this.menu).centerTypeId);
         Component status = Component.translatable(((CenterFuelMenu)this.menu).displayedActive() ? "gui.immortal_cultivation_mod.center_status.active" : (!((CenterFuelMenu)this.menu).displayedEnabled() ? "gui.immortal_cultivation_mod.center_status.disabled" : "gui.immortal_cultivation_mod.center_status.no_fuel"));
         long energy = ((CenterFuelMenu)this.menu).displayedEnergy();
         int fuelCost = ((CenterFuelMenu)this.menu).displayedFuelCost();
         ItemStack fuelStack = ((CenterFuelMenu)this.menu).getSlot(0).getItem();
         boolean infiniteFuel = fuelStack.is((Item)ModItems.INFINITE_SPIRIT_STONE.get());
         long availableFuel = energy + (long)CenterFuelController.fuelValue(fuelStack) * (long)fuelStack.getCount();
         double stones = (double)availableFuel / (double)1.0F;
         long runtimeSeconds = fuelCost <= 0 ? 0L : (long)Math.floor((double)availableFuel * (double)60.0F / (double)fuelCost);
         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_type", new Object[]{Component.translatable(centerType.translationKey())}), x + 111, y + 38, 4210752, false);
         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_status", new Object[]{status}), x + 270, y + 38, 4210752, false);
         boolean teleportCenter = MagicCenterTypeRegistry.TELEPORT.equals(((CenterFuelMenu)this.menu).centerTypeId);
         g.drawString(this.font, teleportCenter ? Component.translatable("gui.immortal_cultivation_mod.teleport_cost_on_use") : Component.translatable("gui.immortal_cultivation_mod.center_cost", new Object[]{fuelCost}), x + 270, y + 51, 4210752, false);
         g.drawString(this.font, infiniteFuel ? Component.translatable("gui.immortal_cultivation_mod.center_energy_infinite") : Component.translatable("gui.immortal_cultivation_mod.center_energy", new Object[]{String.format("%.2f", stones)}), x + 111, y + 64, 4210752, false);
         g.drawString(this.font, teleportCenter ? Component.translatable("gui.immortal_cultivation_mod.teleport_runtime_on_demand") : (infiniteFuel ? Component.translatable("gui.immortal_cultivation_mod.center_runtime_infinite") : Component.translatable("gui.immortal_cultivation_mod.center_runtime", new Object[]{formatTime(runtimeSeconds)})), x + 270, y + 64, 4210752, false);
         this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.center_owner", new Object[]{((CenterFuelMenu)this.menu).ownerLabel}), x + 111, y + 77, 260);
         if (MagicCenterTypeRegistry.BLANK.equals(((CenterFuelMenu)this.menu).centerTypeId)) {
            g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_circles", new Object[]{((CenterFuelMenu)this.menu).displayedAttackCircles(), ((CenterFuelMenu)this.menu).displayedDefenseCircles(), ((CenterFuelMenu)this.menu).displayedRegenerationCircles()}), x + 111, y + 90, 4210752, false);
            this.drawWrapped(g, Component.translatable("gui.immortal_cultivation_mod.center_blank_hint"), x + 111, y + 103, 258, 2);
         } else {
            this.drawWrapped(g, Component.translatable(centerType.descriptionKey()), x + 111, y + 90, 258, 2);
            if (MagicCenterTypeRegistry.FORCEFIELD.equals(((CenterFuelMenu)this.menu).centerTypeId)) {
               this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.center_barrier_health", new Object[]{infiniteFuel ? Component.translatable("gui.immortal_cultivation_mod.infinite") : Math.round(((CenterFuelMenu)this.menu).displayedBarrierHealth())}), x + 111, y + 116, 258);
            } else if (teleportCenter) {
               this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.teleport_destination", new Object[]{((CenterFuelMenu)this.menu).teleportDestinationLabel}), x + 111, y + 116, 258);
            } else {
               this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.center_pattern", new Object[]{Component.translatable(centerType.translationKey())}), x + 111, y + 116, 258);
            }
         }

         this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.fuel"), x + 342, y + 109, 36);
         if (!((CenterFuelMenu)this.menu).canControlCenter()) {
            this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.fuel_locked"), x + 337, y + 122, 66);
         }

         if (centerSlotCount == 2) {
            this.drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.reforge_weapon"), x + 337, y + 76, 42);
         }

         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.glyph_color_title"), x + 18, y + 162, 4210752, false);
         int columnGap = 12;
         int columnW = (this.imageWidth - 34 - columnGap) / 2;
         int controllerX = x + 17 + columnW + columnGap;
         List<String> names = sortedNames(((CenterFuelMenu)this.menu).blacklistedPlayers);
         List<String> controllers = sortedNames(((CenterFuelMenu)this.menu).controllerPlayers);
         if (!names.isEmpty()) {
            this.blacklistEntryIndex = Math.min(this.blacklistEntryIndex, names.size() - 1);
         }

         if (!controllers.isEmpty()) {
            this.controllerEntryIndex = Math.min(this.controllerEntryIndex, controllers.size() - 1);
         }

         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.blacklist").append(names.isEmpty() ? "" : " " + (this.blacklistEntryIndex + 1) + "/" + names.size()), x + 18, y + 197, 4210752, false);
         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.controller_whitelist").append(controllers.isEmpty() ? "" : " " + (this.controllerEntryIndex + 1) + "/" + controllers.size()), controllerX + 1, y + 197, 4210752, false);
         if (!names.isEmpty()) {
            int nameX = names.size() > 1 ? x + 36 : x + 20;
            int nameWidth = names.size() > 1 ? columnW - 116 : columnW - 68;
            Object var10002 = names.get(this.blacklistEntryIndex);
            this.drawTrimmed(g, Component.literal(" - " + (String)var10002), nameX, this.entryStartY + 5, nameWidth);
         }

         if (!controllers.isEmpty()) {
            int nameX = controllers.size() > 1 ? controllerX + 18 : controllerX + 2;
            int nameWidth = controllers.size() > 1 ? columnW - 116 : columnW - 68;
            Object var41 = controllers.get(this.controllerEntryIndex);
            this.drawTrimmed(g, Component.literal(" - " + (String)var41), nameX, this.entryStartY + 5, nameWidth);
         }

      }
   }

   private void renderOctagonGrid(GuiGraphics graphics, int x, int y) {
      for(int layer = 0; layer < 3; ++layer) {
         int start = 2 + layer * 8;

         for(int point = 0; point < 8; ++point) {
            Slot first = ((CenterFuelMenu)this.menu).getSlot(start + point);
            Slot second = ((CenterFuelMenu)this.menu).getSlot(start + (point + 1) % 8);
            drawPixelLine(graphics, x + first.x + 8, y + first.y + 8, x + second.x + 8, y + second.y + 8, -1603575213);
         }
      }

      for(int slotIndex = 2; slotIndex < 26; ++slotIndex) {
         Slot slot = ((CenterFuelMenu)this.menu).getSlot(slotIndex);
         graphics.blit(TEX_SLOT, x + slot.x - 1, y + slot.y - 1, 0.0F, 0.0F, 18, 18, 18, 18);
      }

   }

   private static void drawPixelLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
      int dx = Math.abs(x1 - x0);
      int sx = x0 < x1 ? 1 : -1;
      int dy = -Math.abs(y1 - y0);
      int sy = y0 < y1 ? 1 : -1;
      int error = dx + dy;

      while(true) {
         graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
         if (x0 == x1 && y0 == y1) {
            return;
         }

         int twiceError = error * 2;
         if (twiceError >= dy) {
            error += dy;
            x0 += sx;
         }

         if (twiceError <= dx) {
            error += dx;
            y0 += sy;
         }
      }
   }

   private void renderCenterGlyph(GuiGraphics graphics, int centerX, int centerY, int size, float partialTick) {
      int color = ((CenterFuelMenu)this.menu).glyphColor & 16777215;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      graphics.setColor(red, green, blue, 1.0F);
      double time = Minecraft.getInstance().level == null ? System.nanoTime() / 50_000_000.0
              : (double)Minecraft.getInstance().level.getGameTime() + partialTick;
      var textures = FormationLayers.center(((CenterFuelMenu)this.menu).centerTypeId);
      for (int layer = 0; layer < FormationLayers.COUNT; layer++) {
         drawGlyphLayer(graphics, textures.get(layer), centerX, centerY, size, FormationLayers.angle(time, layer));
      }

      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }


   private static void drawGlyphLayer(GuiGraphics graphics, ResourceLocation texture, int centerX, int centerY, int size, float angle) {
      graphics.pose().pushPose();
      graphics.pose().translate((float)centerX, (float)centerY, 120.0F);
      graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
      graphics.blit(texture, -size / 2, -size / 2, size, size, 0.0F, 0.0F, 128, 128, 128, 128);
      graphics.pose().popPose();
   }

   private static String formatTime(long seconds) {
      long minutes = seconds / 60L;
      return String.format("%02d:%02d", minutes, seconds % 60L);
   }

   private void drawTrimmed(GuiGraphics graphics, Component text, int x, int y, int width) {
      graphics.drawString(this.font, this.font.plainSubstrByWidth(text.getString(), width), x, y, 4210752, false);
   }

   private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int maxLines) {
      List<FormattedCharSequence> lines = this.font.split(text, width);
      int count = Math.min(maxLines, lines.size());

      for(int i = 0; i < count; ++i) {
         graphics.drawString(this.font, (FormattedCharSequence)lines.get(i), x, y + i * 10, 4210752, false);
      }

   }

   protected void renderLabels(GuiGraphics g, int mx, int my) {
      g.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 17, -13617617, false);
      g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -13617617, false);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      super.render(graphics, mouseX, mouseY, partialTick);
      int pulse = 145 + (int)(Math.sin((double)System.currentTimeMillis() / (double)180.0F) * (double)65.0F);
      int color = Math.max(70, Math.min(230, pulse)) << 24 | 15779435;
      if (DaoGuidanceClient.isCurrentAction("open_flag_gui") && this.octagonTabBtn != null) {
         graphics.renderOutline(this.octagonTabBtn.getX() - 3, this.octagonTabBtn.getY() - 3, this.octagonTabBtn.getWidth() + 6, this.octagonTabBtn.getHeight() + 6, color);
      } else if (DaoGuidanceClient.isCurrentAction("insert_flag")) {
         for(int slot = 2; slot < 26; ++slot) {
            Slot target = ((CenterFuelMenu)this.menu).getSlot(slot);
            graphics.renderOutline(this.leftPos + target.x - 2, this.topPos + target.y - 2, 20, 20, color);
         }
      }

      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private final class ColorSwatchButton extends Button {
      private final int color;

      private ColorSwatchButton(int x, int y, int width, int height, int color, Runnable onPress) {
         super(x, y, width, height, Component.empty(), (button) -> onPress.run(), DEFAULT_NARRATION);
         this.color = color;
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         boolean selected = (((CenterFuelMenu)CenterFuelScreen.this.menu).glyphColor & 16777215) == this.color;
         int border = selected ? -1 : (this.isHoveredOrFocused() ? -1516113 : -11710133);
         graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, border);
         graphics.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, -16777216 | this.color);
      }
   }
}
