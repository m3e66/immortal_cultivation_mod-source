package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundCenterSettingsPayload;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterType;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.example.immortal_cultivation_mod.screen.CultivationGuiTextures;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
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
   private static final ResourceLocation CENTER_GLYPH = glyph("magic_center_glyph");
   private static final ResourceLocation FOG_GLYPH_1 = glyph("magic_center_fog_1");
   private static final ResourceLocation FOG_GLYPH_2 = glyph("magic_center_fog_2");
   private static final ResourceLocation INVERTED_GLYPH_1 = glyph("magic_center_inverted_1");
   private static final ResourceLocation INVERTED_GLYPH_2 = glyph("magic_center_inverted_2");
   private static final ResourceLocation FORCEFIELD_GLYPH_1 = glyph("magic_center_forcefield_1");
   private static final ResourceLocation FORCEFIELD_GLYPH_2 = glyph("magic_center_forcefield_2");
   private static final ResourceLocation SCORCHING_GLYPH_1 = glyph("magic_center_scorching_1");
   private static final ResourceLocation SCORCHING_GLYPH_2 = glyph("magic_center_scorching_2");
   private static final ResourceLocation SANCTUARY_GLYPH_1 = glyph("magic_center_sanctuary_1");
   private static final ResourceLocation SANCTUARY_GLYPH_2 = glyph("magic_center_sanctuary_2");
   private static final ResourceLocation FROST_GLYPH_1 = glyph("magic_center_frost_1");
   private static final ResourceLocation FROST_GLYPH_2 = glyph("magic_center_frost_2");
   private static final ResourceLocation DECAY_GLYPH_1 = glyph("magic_center_decay_1");
   private static final ResourceLocation DECAY_GLYPH_2 = glyph("magic_center_decay_2");
   private static final ResourceLocation THUNDER_GLYPH_1 = glyph("magic_center_thunder_1");
   private static final ResourceLocation THUNDER_GLYPH_2 = glyph("magic_center_thunder_2");
   private static final ResourceLocation DETECTION_GLYPH_1 = glyph("magic_center_detection_1");
   private static final ResourceLocation DETECTION_GLYPH_2 = glyph("magic_center_detection_2");
   private static final ResourceLocation REFORGE_BLADE_GLYPH_1 = glyph("magic_center_reforge_blade_1");
   private static final ResourceLocation REFORGE_BLADE_GLYPH_2 = glyph("magic_center_reforge_blade_2");
   private static final ResourceLocation TELEPORT_GLYPH_1 = glyph("magic_center_teleport_1");
   private static final ResourceLocation TELEPORT_GLYPH_2 = glyph("magic_center_teleport_2");
   private static final int BORDER = 5;
   private static final int FUEL_ROW_H = 18;
   private static final int TOGGLE_H = 18;
   private static final int LABEL_H = 8;
   private static final int[] GLYPH_COLORS = {
         0xFFFFFF, 0xFF4040, 0xFFD45A, 0x55DD77,
         0x55E6E6, 0x5590FF, 0xB968FF, 0xFF70C8
   };
   private static final String[] GLYPH_COLOR_KEYS = {
         "white", "red", "gold", "green", "cyan", "blue", "purple", "pink"
   };
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
   private final List<Button> removeButtons = new ArrayList<>();
   private final List<Button> controllerRemoveButtons = new ArrayList<>();
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
   private final List<ColorSwatchButton> colorButtons = new ArrayList<>();
   private Button settingsTabBtn;
   private Button octagonTabBtn;
   private boolean octagonTab;

   private static ResourceLocation tex(String name) {
      return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/" + name + ".png");
   }

   private static ResourceLocation glyph(String name) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",
            "textures/particle/" + name + ".png");
   }

   private String labelParticles() {
      return "Particles:" + (((CenterFuelMenu)this.menu).particlesEnabled ? "ON" : "OFF");
   }

   public CenterFuelScreen(CenterFuelMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = CenterFuelMenu.GUI_WIDTH;
      this.imageHeight = CenterFuelMenu.GUI_HEIGHT;
      this.inventoryLabelX = CenterFuelMenu.PLAYER_INVENTORY_X;
      this.inventoryLabelY = 253;
   }

   protected void init() {
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      this.entryStartY = y + 204;
      int inputY = y + 226;
      this.splitY = y + 249;
      this.playerInvTop = y + CenterFuelMenu.PLAYER_INVENTORY_Y;
      int toggleW = 88;
      int toggleGap = 6;
      int toggleX = x + (this.imageWidth - toggleW * 4 - toggleGap * 3) / 2;
      int toggleY = y + 142;
      this.hostileBtn = this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(toggleX, toggleY, toggleW, 18,
               Component.translatable("gui.immortal_cultivation_mod.hostile_toggle", this.stateLabel(((CenterFuelMenu)this.menu).allowHostileMobs)),
               b -> {
                  PacketDistributor.sendToServer(
                     new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_hostile", ""), new CustomPacketPayload[0]
                  );
                  ((CenterFuelMenu)this.menu).allowHostileMobs = !((CenterFuelMenu)this.menu).allowHostileMobs;
                  b.setMessage(Component.translatable("gui.immortal_cultivation_mod.hostile_toggle", this.stateLabel(((CenterFuelMenu)this.menu).allowHostileMobs)));
               }
            )
      );
      this.passiveBtn = this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(toggleX + toggleW + toggleGap, toggleY, toggleW, 18,
               Component.translatable("gui.immortal_cultivation_mod.passive_toggle", this.stateLabel(((CenterFuelMenu)this.menu).allowPassiveMobs)),
               b -> {
                  PacketDistributor.sendToServer(
                     new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_passive", ""), new CustomPacketPayload[0]
                  );
                  ((CenterFuelMenu)this.menu).allowPassiveMobs = !((CenterFuelMenu)this.menu).allowPassiveMobs;
                  b.setMessage(Component.translatable("gui.immortal_cultivation_mod.passive_toggle", this.stateLabel(((CenterFuelMenu)this.menu).allowPassiveMobs)));
               }
            )
      );
      this.particlesBtn = this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(toggleX + (toggleW + toggleGap) * 2, toggleY, toggleW, 18,
               Component.translatable("gui.immortal_cultivation_mod.glyph_toggle", this.stateLabel(((CenterFuelMenu)this.menu).particlesEnabled)),
               b -> {
                  PacketDistributor.sendToServer(
                     new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "toggle_particles", ""), new CustomPacketPayload[0]
                  );
                  ((CenterFuelMenu)this.menu).particlesEnabled = !((CenterFuelMenu)this.menu).particlesEnabled;
                  b.setMessage(Component.translatable("gui.immortal_cultivation_mod.glyph_toggle", this.stateLabel(((CenterFuelMenu)this.menu).particlesEnabled)));
               }
            )
      );
      this.enabledBtn = this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(toggleX + (toggleW + toggleGap) * 3, toggleY, toggleW, 18,
               Component.translatable("gui.immortal_cultivation_mod.center_enabled_toggle",
                     this.stateLabel(this.menu.displayedEnabled())),
               b -> {
                  PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(
                        this.menu.centerPos, "toggle_enabled", ""), new CustomPacketPayload[0]);
                  this.menu.enabled = !this.menu.displayedEnabled();
                  b.setMessage(Component.translatable(
                        "gui.immortal_cultivation_mod.center_enabled_toggle",
                        this.stateLabel(this.menu.enabled)));
               }));
      this.teleportModeBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(
                  toggleX, toggleY, toggleW * 2 + toggleGap, 18,
                  this.teleportModeLabel(this.menu.displayedTeleportLinkMode()),
                  button -> {
                     int current = this.menu.displayedTeleportLinkMode();
                     if (current < 0) {
                        return;
                     }
                     int next = current == 0 ? 1 : current == 1 ? 2 : 0;
                     this.menu.setDisplayedTeleportLinkMode(next);
                     PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(
                           this.menu.centerPos, "cycle_teleport_mode", ""),
                           new CustomPacketPayload[0]);
                     button.setMessage(this.teleportModeLabel(next));
                  }));
      this.radiusDownBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(
                  x + 226,
                  y + 169,
                  28,
                  18,
                  Component.literal("-"),
                  button -> this.changeRadius(-2)));

      this.radiusInput = this.addRenderableWidget(
            new EditBox(
                  this.font,
                  x + 260,
                  y + 169,
                  54,
                  18,
                  Component.translatable(
                        "gui.immortal_cultivation_mod.center_radius",
                        this.menu.displayedRadius())));

      this.radiusInput.setMaxLength(3);
      this.radiusInput.setFilter(value ->
            value.isEmpty()
                  || value.chars().allMatch(Character::isDigit));
      this.radiusInput.setValue(
            Integer.toString(this.menu.displayedRadius()));
      this.radiusInput.setHint(Component.literal("3-303"));

      this.radiusApplyBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(
                  x + 320,
                  y + 169,
                  38,
                  18,
                  Component.literal("OK"),
                  button -> this.applyTypedRadius()));

      this.radiusUpBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(
                  x + 364,
                  y + 169,
                  28,
                  18,
                  Component.literal("+"),
                  button -> this.changeRadius(2)));
      MagicCenterType centerType = MagicCenterTypeRegistry.getOrBlank(this.menu.centerTypeId);
      this.hostileBtn.visible = centerType.behavior().supportsHostileMobSetting();
      this.passiveBtn.visible = centerType.behavior().supportsPassiveMobSetting();
      this.colorButtons.clear();
      int swatchX = x + 24;
      for (int i = 0; i < GLYPH_COLORS.length; i++) {
         final int color = GLYPH_COLORS[i];
         ColorSwatchButton button = this.addRenderableWidget(
               new ColorSwatchButton(swatchX + i * 24, y + 171, 19, 14, color, () -> this.selectColor(color)));
         this.colorButtons.add(button);
      }
      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int inputW = columnW - 66;
      int inputX = x + 17;
      int controllerX = inputX + columnW + columnGap;
      this.nameInput = (EditBox)this.addRenderableWidget(new EditBox(this.font, inputX, inputY, inputW, 16, Component.literal("")));
      this.nameInput.setMaxLength(16);
      this.blacklistAddBtn = this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(inputX + inputW + 6, inputY - 2, 60, 20,
               Component.translatable("gui.immortal_cultivation_mod.add"),
               b -> {
                  String name = this.nameInput.getValue().trim();
                  if (isValidPlayerName(name)
                        && this.menu.blacklistedPlayers.size()
                              < MagicCenterBlockEntity.MAX_BLACKLISTED_PLAYERS
                        && !containsIgnoreCase(this.menu.blacklistedPlayers, name)) {
                     PacketDistributor.sendToServer(
                        new ServerboundCenterSettingsPayload(((CenterFuelMenu)this.menu).centerPos, "add_blacklist", name), new CustomPacketPayload[0]
                     );
                     this.nameInput.setValue("");
                  }
               }
            )
      );
      this.controllerInput = this.addRenderableWidget(
            new EditBox(this.font, controllerX, inputY, inputW, 16, Component.literal("")));
      this.controllerInput.setMaxLength(16);
      this.controllerAddBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(controllerX + inputW + 6, inputY - 2, 60, 20,
                  Component.translatable("gui.immortal_cultivation_mod.add"),
                  b -> {
                     String name = this.controllerInput.getValue().trim();
                     if (isValidPlayerName(name)
                           && this.menu.controllerPlayers.size()
                                 < MagicCenterBlockEntity.MAX_CONTROLLER_PLAYERS
                           && !containsIgnoreCase(this.menu.controllerPlayers, name)) {
                        PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(
                              this.menu.centerPos, "add_controller", name),
                              new CustomPacketPayload[0]);
                        this.controllerInput.setValue("");
                     }
                  }));
      boolean canControl = this.menu.canControlCenter();
      boolean canManageControllers = this.menu.canManageControllers();
      this.hostileBtn.active = canControl;
      this.passiveBtn.active = canControl;
      this.particlesBtn.active = canControl;
      this.enabledBtn.active = canControl;
      this.teleportModeBtn.active = canManageControllers
            && this.menu.displayedTeleportLinkMode() >= 0;
      this.radiusDownBtn.active = canControl;
      this.radiusUpBtn.active = canControl;
      this.radiusInput.active = canControl;
      this.radiusApplyBtn.active = canControl;
      this.colorButtons.forEach(button -> button.active = canControl);
      this.nameInput.active = canControl;
      this.blacklistAddBtn.active = canControl;
      this.controllerInput.active = canManageControllers;
      this.controllerAddBtn.active = canManageControllers;
      this.refreshRemoveButtons();
      this.refreshControllerRemoveButtons();
      this.seenAccessRevision = this.menu.accessRevision();

      int tabX = x + this.imageWidth - 4;
      int tabY = y + 46;
      this.settingsTabBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(
                  tabX, tabY, 70, 20,
                  Component.translatable(
                        "gui.immortal_cultivation_mod.center_tab_settings"),
                  button -> this.switchTab(false)));
      this.octagonTabBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(
                  tabX, tabY + 23, 70, 20,
                  Component.translatable(
                        "gui.immortal_cultivation_mod.center_tab_octagon"),
                  button -> this.switchTab(true)));
      this.switchTab(this.octagonTab);
   }

   private void switchTab(boolean octagon) {
      this.octagonTab = octagon;
      this.menu.setOctagonTabActive(octagon);

      boolean settingsVisible = !octagon;
      MagicCenterType centerType =
            MagicCenterTypeRegistry.getOrBlank(this.menu.centerTypeId);
      this.hostileBtn.visible = settingsVisible
            && centerType.behavior().supportsHostileMobSetting();
      this.passiveBtn.visible = settingsVisible
            && centerType.behavior().supportsPassiveMobSetting();
      this.particlesBtn.visible = settingsVisible;
      this.enabledBtn.visible = settingsVisible;
      this.teleportModeBtn.visible = settingsVisible
            && MagicCenterTypeRegistry.TELEPORT.equals(this.menu.centerTypeId);
      this.radiusDownBtn.visible = settingsVisible;
      this.radiusUpBtn.visible = settingsVisible;
      this.radiusInput.visible = settingsVisible;
      this.radiusApplyBtn.visible = settingsVisible;
      this.colorButtons.forEach(button -> button.visible = settingsVisible);
      this.nameInput.visible = settingsVisible;
      this.blacklistAddBtn.visible = settingsVisible;
      this.controllerInput.visible = settingsVisible;
      this.controllerAddBtn.visible = settingsVisible;
      this.removeButtons.forEach(button -> button.visible = settingsVisible);
      this.controllerRemoveButtons.forEach(button -> button.visible = settingsVisible);

      if (octagon) {
         this.nameInput.setFocused(false);
         this.controllerInput.setFocused(false);
         this.radiusInput.setFocused(false);
      }

      if (this.settingsTabBtn != null) {
         this.settingsTabBtn.visible = true;
         this.settingsTabBtn.active = octagon;
      }
      if (this.octagonTabBtn != null) {
         this.octagonTabBtn.visible = true;
         this.octagonTabBtn.active = !octagon;
      }
   }

   @Override
   protected void containerTick() {
      super.containerTick();

      if (this.seenAccessRevision != this.menu.accessRevision()) {
         this.seenAccessRevision = this.menu.accessRevision();
         this.refreshRemoveButtons();
         this.refreshControllerRemoveButtons();
      }

      if (this.radiusInput != null && !this.radiusInput.isFocused()) {
         String currentValue = Integer.toString(this.menu.displayedRadius());

         if (!this.radiusInput.getValue().equals(currentValue)) {
            this.radiusInput.setValue(currentValue);
         }
      }

      boolean canControl = this.menu.canControlCenter();

      if (this.teleportModeBtn != null) {
         int mode = this.menu.displayedTeleportLinkMode();
         this.teleportModeBtn.active = this.menu.canManageControllers() && mode >= 0;
         this.teleportModeBtn.setMessage(this.teleportModeLabel(mode));
      }

      if (this.radiusDownBtn != null) {
         this.radiusDownBtn.active = canControl
               && this.menu.displayedRadius() > MagicCenterBlockEntity.MIN_RADIUS;
      }

      if (this.radiusUpBtn != null) {
         this.radiusUpBtn.active = canControl
               && this.menu.displayedRadius() < MagicCenterBlockEntity.MAX_RADIUS;
      }
   }

   private void changeRadius(int amount) {
      commitRadius(this.menu.displayedRadius() + amount);
   }

   private void applyTypedRadius() {
      String text = this.radiusInput.getValue().trim();

      if (text.isEmpty()) {
         this.radiusInput.setValue(
               Integer.toString(this.menu.displayedRadius()));
         return;
      }

      try {
         commitRadius(Integer.parseInt(text));
      } catch (NumberFormatException ignored) {
         this.radiusInput.setValue(
               Integer.toString(this.menu.displayedRadius()));
      }
   }

   private void commitRadius(int requestedRadius) {
      int radius = MagicCenterBlockEntity.normalizeRadius(requestedRadius);

      this.menu.setDisplayedRadius(radius);
      this.radiusInput.setValue(Integer.toString(radius));

      PacketDistributor.sendToServer(
            new ServerboundCenterSettingsPayload(
                  this.menu.centerPos,
                  "set_radius",
                  Integer.toString(radius)),
            new CustomPacketPayload[0]);
   }

   private void refreshRemoveButtons() {
      this.removeButtons.forEach(x$0 -> this.removeWidget(x$0));
      this.removeButtons.clear();
      int x = this.leftPos;
      int btnW = 60;
      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int columnX = x + 17;
      int btnX = x + 17 + columnW - btnW;
      List<String> names = sortedNames(this.menu.blacklistedPlayers);
      if (names.isEmpty()) {
         this.blacklistEntryIndex = 0;
         return;
      }
      this.blacklistEntryIndex = Math.min(this.blacklistEntryIndex, names.size() - 1);
      String playerName = names.get(this.blacklistEntryIndex);
      if (names.size() > 1) {
         addAccessNavigationButtons(this.removeButtons, columnX, btnX,
               () -> {
                  this.blacklistEntryIndex = Math.floorMod(
                        this.blacklistEntryIndex - 1, names.size());
                  this.refreshRemoveButtons();
               },
               () -> {
                  this.blacklistEntryIndex = (this.blacklistEntryIndex + 1) % names.size();
                  this.refreshRemoveButtons();
               });
      }
      Button removeBtn = this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(btnX, this.entryStartY + 1, btnW, 18,
               Component.translatable("gui.immortal_cultivation_mod.remove"),
               b -> {
                  PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(
                        this.menu.centerPos, "remove_blacklist", playerName),
                        new CustomPacketPayload[0]);
               }));
      removeBtn.active = this.menu.canControlCenter();
      removeBtn.visible = !this.octagonTab;
      this.removeButtons.add(removeBtn);
   }

   private void refreshControllerRemoveButtons() {
      this.controllerRemoveButtons.forEach(this::removeWidget);
      this.controllerRemoveButtons.clear();
      int x = this.leftPos;
      int btnW = 60;
      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int controllerX = x + 17 + columnW + columnGap;
      int btnX = controllerX + columnW - btnW;
      List<String> names = sortedNames(this.menu.controllerPlayers);
      if (names.isEmpty()) {
         this.controllerEntryIndex = 0;
         return;
      }
      this.controllerEntryIndex = Math.min(this.controllerEntryIndex, names.size() - 1);
      String playerName = names.get(this.controllerEntryIndex);
      if (names.size() > 1) {
         addAccessNavigationButtons(this.controllerRemoveButtons, controllerX, btnX,
               () -> {
                  this.controllerEntryIndex = Math.floorMod(
                        this.controllerEntryIndex - 1, names.size());
                  this.refreshControllerRemoveButtons();
               },
               () -> {
                  this.controllerEntryIndex = (this.controllerEntryIndex + 1) % names.size();
                  this.refreshControllerRemoveButtons();
               });
      }
      Button removeBtn = this.addRenderableWidget(
            new MagicRitualGuiStyle.RitualButton(btnX, this.entryStartY + 1, btnW, 18,
                  Component.translatable("gui.immortal_cultivation_mod.remove"),
                  b -> {
                     PacketDistributor.sendToServer(new ServerboundCenterSettingsPayload(
                           this.menu.centerPos, "remove_controller", playerName),
                           new CustomPacketPayload[0]);
                  }));
      removeBtn.active = this.menu.canManageControllers();
      removeBtn.visible = !this.octagonTab;
      this.controllerRemoveButtons.add(removeBtn);
   }

   private void addAccessNavigationButtons(List<Button> buttons, int columnX, int removeX,
                                           Runnable previous, Runnable next) {
      Button previousBtn = this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(
            columnX, this.entryStartY + 1, 16, 18, Component.literal("<"),
            button -> previous.run()));
      Button nextBtn = this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(
            removeX - 18, this.entryStartY + 1, 16, 18, Component.literal(">"),
            button -> next.run()));
      previousBtn.visible = !this.octagonTab;
      nextBtn.visible = !this.octagonTab;
      buttons.add(previousBtn);
      buttons.add(nextBtn);
   }

   private static List<String> sortedNames(java.util.Set<String> names) {
      List<String> sorted = new ArrayList<>(names);
      sorted.sort(String.CASE_INSENSITIVE_ORDER);
      return sorted;
   }

   private static boolean isValidPlayerName(String name) {
      return name != null && name.matches("[A-Za-z0-9_]{1,16}");
   }

   private static boolean containsIgnoreCase(java.util.Set<String> names, String name) {
      return names.stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
   }

   private String labelHostile() {
      return "Hostile:" + (((CenterFuelMenu)this.menu).allowHostileMobs ? "ON" : "OFF");
   }

   private Component stateLabel(boolean enabled) {
      return Component.translatable(enabled
            ? "gui.immortal_cultivation_mod.enabled"
            : "gui.immortal_cultivation_mod.disabled");
   }

   private Component teleportModeLabel(int mode) {
      String suffix = switch (mode) {
         case 0 -> "two_way";
         case 1 -> "this_source";
         case 2 -> "linked_source";
         default -> "unlinked";
      };
      return Component.translatable(
            "gui.immortal_cultivation_mod.teleport_mode." + suffix);
   }

   private void selectColor(int color) {
      ((CenterFuelMenu)this.menu).glyphColor = color;
      PacketDistributor.sendToServer(
            new ServerboundCenterSettingsPayload(
                  ((CenterFuelMenu)this.menu).centerPos,
                  "set_glyph_color",
                  String.format("%06X", color)),
            new CustomPacketPayload[0]);
   }

   private String labelPassive() {
      return "Passive:" + (((CenterFuelMenu)this.menu).allowPassiveMobs ? "ON" : "OFF");
   }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers) {
        if (this.radiusInput.isFocused()
                && (keyCode == 257 || keyCode == 335)) {
            this.applyTypedRadius();
            this.radiusInput.setFocused(false);
            return true;
        }

        EditBox focusedInput =
                this.nameInput.isFocused()
                        ? this.nameInput
                        : this.controllerInput.isFocused()
                        ? this.controllerInput
                        : this.radiusInput.isFocused()
                        ? this.radiusInput
                        : null;

        if (focusedInput != null) {
            if (focusedInput.keyPressed(
                    keyCode,
                    scanCode,
                    modifiers)) {
                return true;
            }

            if (keyCode == 256) {
                this.onClose();
                return true;
            }

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers);
    }

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers) {
        if (this.nameInput.isFocused()) {
            return this.nameInput.charTyped(
                    codePoint,
                    modifiers);
        }

        if (this.controllerInput.isFocused()) {
            return this.controllerInput.charTyped(
                    codePoint,
                    modifiers);
        }

        if (this.radiusInput.isFocused()) {
            return this.radiusInput.charTyped(
                    codePoint,
                    modifiers);
        }

        return super.charTyped(
                codePoint,
                modifiers);
    }

   @Override
   protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
      int x = this.leftPos;
      int y = this.topPos;
      int W = this.imageWidth;
      int H = this.imageHeight;
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      CultivationGuiTextures.drawLandscape(g, x, y, W, H);

      if (this.octagonTab) {
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 30, W - 24, 218);
         CultivationGuiTextures.drawInnerBox(g, x + 12, y + 263, W - 24, 84);
         renderOctagonGrid(g, x, y);
         g.blit(TEX_SPLIT, x + 5, this.splitY, 0.0F, 0.0F, W - 10, 14, 1, 14);
         int playerInvLeft = x + CenterFuelMenu.PLAYER_INVENTORY_X;
         g.blit(TEX_PSLOT, playerInvLeft - 1, this.playerInvTop - 1,
               0.0F, 0.0F, 162, 76, 162, 76);
         return;
      }

      CultivationGuiTextures.drawInnerBox(g, x + 12, y + 30, W - 24, 104);
      CultivationGuiTextures.drawInnerBox(g, x + 12, y + 136, W - 24, 55);
      CultivationGuiTextures.drawInnerBox(g, x + 12, y + 193, W - 24, 55);
      CultivationGuiTextures.drawInnerBox(g, x + 12, y + 263, W - 24, 84);
      CultivationGuiTextures.drawInnerBox(g, x + 19, y + 37, 82, 88);
      renderCenterGlyph(g, x + 60, y + 80, 68, partialTick);

      int centerSlotCount = this.menu.isReforgeBlade() || this.menu.getSlot(1).hasItem() ? 2 : 1;
      for (int i = 0; i < centerSlotCount; i++) {
         Slot slot = (Slot)((CenterFuelMenu)this.menu).slots.get(i);
         g.blit(TEX_SLOT, x + slot.x - 1, y + slot.y - 1, 0.0F, 0.0F, 18, 18, 18, 18);
      }

      g.blit(TEX_SPLIT, x + 5, this.splitY, 0.0F, 0.0F, W - 10, 14, 1, 14);
      int playerInvLeft = x + CenterFuelMenu.PLAYER_INVENTORY_X;
      g.blit(TEX_PSLOT, playerInvLeft - 1, this.playerInvTop - 1, 0.0F, 0.0F, 162, 76, 162, 76);
      MagicCenterType centerType = MagicCenterTypeRegistry.getOrBlank(this.menu.centerTypeId);
      Component status = Component.translatable(this.menu.displayedActive()
            ? "gui.immortal_cultivation_mod.center_status.active"
            : !this.menu.displayedEnabled()
                  ? "gui.immortal_cultivation_mod.center_status.disabled"
                  : "gui.immortal_cultivation_mod.center_status.no_fuel");
      long energy = this.menu.displayedEnergy();
      int fuelCost = this.menu.displayedFuelCost();
      ItemStack fuelStack = this.menu.getSlot(0).getItem();
      boolean infiniteFuel = fuelStack.is(ModItems.INFINITE_SPIRIT_STONE.get());
      long availableFuel = energy + (long)CenterFuelController.fuelValue(fuelStack)
            * fuelStack.getCount();
      double stones = availableFuel / (double)CenterFuelController.ENERGY_PER_STONE;
      long runtimeSeconds = fuelCost <= 0 ? 0L
            : (long)Math.floor(availableFuel * 60.0D / fuelCost);
      g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_type",
            Component.translatable(centerType.translationKey())), x + 111, y + 38, 4210752, false);
      g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_status", status),
            x + 270, y + 38, 4210752, false);
//      g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_radius", this.menu.displayedRadius()),
//            x + 111, y + 51, 4210752, false);
      boolean teleportCenter = MagicCenterTypeRegistry.TELEPORT.equals(this.menu.centerTypeId);
      g.drawString(this.font, teleportCenter
                  ? Component.translatable("gui.immortal_cultivation_mod.teleport_cost_on_use")
                  : Component.translatable("gui.immortal_cultivation_mod.center_cost", fuelCost),
            x + 270, y + 51, 4210752, false);
      g.drawString(this.font, infiniteFuel
                  ? Component.translatable("gui.immortal_cultivation_mod.center_energy_infinite")
                  : Component.translatable("gui.immortal_cultivation_mod.center_energy", String.format("%.2f", stones)),
            x + 111, y + 64, 4210752, false);
      g.drawString(this.font, teleportCenter
                  ? Component.translatable("gui.immortal_cultivation_mod.teleport_runtime_on_demand")
                  : infiniteFuel
                        ? Component.translatable("gui.immortal_cultivation_mod.center_runtime_infinite")
                        : Component.translatable("gui.immortal_cultivation_mod.center_runtime", formatTime(runtimeSeconds)),
            x + 270, y + 64, 4210752, false);
      drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.center_owner",
            this.menu.ownerLabel), x + 111, y + 77, 260);
      if (MagicCenterTypeRegistry.BLANK.equals(this.menu.centerTypeId)) {
         g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.center_circles",
               this.menu.displayedAttackCircles(), this.menu.displayedDefenseCircles(),
               this.menu.displayedRegenerationCircles()),
               x + 111, y + 90, 4210752, false);
         drawWrapped(g, Component.translatable("gui.immortal_cultivation_mod.center_blank_hint"),
               x + 111, y + 103, 258, 2);
      } else {
         drawWrapped(g, Component.translatable(centerType.descriptionKey()),
               x + 111, y + 90, 258, 2);
         if (MagicCenterTypeRegistry.FORCEFIELD.equals(this.menu.centerTypeId)) {
            drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.center_barrier_health",
                  infiniteFuel
                        ? Component.translatable("gui.immortal_cultivation_mod.infinite")
                        : Math.round(this.menu.displayedBarrierHealth())), x + 111, y + 116, 258);
         } else if (teleportCenter) {
            drawTrimmed(g, Component.translatable(
                  "gui.immortal_cultivation_mod.teleport_destination",
                  this.menu.teleportDestinationLabel), x + 111, y + 116, 258);
         } else {
            drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.center_pattern",
                  Component.translatable(centerType.translationKey())), x + 111, y + 116, 258);
         }
      }
      drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.fuel"),
            x + 342, y + 109, 36);
      if (!this.menu.canControlCenter()) {
         drawTrimmed(g, Component.translatable(
               "gui.immortal_cultivation_mod.fuel_locked"), x + 337, y + 122, 66);
      }
      if (centerSlotCount == 2) {
         drawTrimmed(g, Component.translatable("gui.immortal_cultivation_mod.reforge_weapon"),
               x + 337, y + 76, 42);
      }
      g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.glyph_color_title"),
            x + 18, y + 162, 4210752, false);
      int columnGap = 12;
      int columnW = (this.imageWidth - 34 - columnGap) / 2;
      int controllerX = x + 17 + columnW + columnGap;
      List<String> names = sortedNames(this.menu.blacklistedPlayers);
      List<String> controllers = sortedNames(this.menu.controllerPlayers);
      if (!names.isEmpty()) {
         this.blacklistEntryIndex = Math.min(this.blacklistEntryIndex, names.size() - 1);
      }
      if (!controllers.isEmpty()) {
         this.controllerEntryIndex = Math.min(
               this.controllerEntryIndex, controllers.size() - 1);
      }
      g.drawString(this.font, Component.translatable("gui.immortal_cultivation_mod.blacklist")
            .append(names.isEmpty() ? "" : " " + (this.blacklistEntryIndex + 1)
                  + "/" + names.size()), x + 18, y + 197, 4210752, false);
      g.drawString(this.font, Component.translatable(
            "gui.immortal_cultivation_mod.controller_whitelist")
            .append(controllers.isEmpty() ? "" : " " + (this.controllerEntryIndex + 1)
                  + "/" + controllers.size()), controllerX + 1, y + 197, 4210752, false);
      if (!names.isEmpty()) {
         int nameX = names.size() > 1 ? x + 36 : x + 20;
         int nameWidth = names.size() > 1 ? columnW - 116 : columnW - 68;
         drawTrimmed(g, Component.literal(" - " + names.get(this.blacklistEntryIndex)),
               nameX, this.entryStartY + 5, nameWidth);
      }
      if (!controllers.isEmpty()) {
         int nameX = controllers.size() > 1 ? controllerX + 18 : controllerX + 2;
         int nameWidth = controllers.size() > 1 ? columnW - 116 : columnW - 68;
         drawTrimmed(g, Component.literal(" - "
               + controllers.get(this.controllerEntryIndex)), nameX,
               this.entryStartY + 5, nameWidth);
      }
   }

   private void renderOctagonGrid(GuiGraphics graphics, int x, int y) {
      for (int layer = 0; layer < CenterFuelMenu.OCTAGON_LAYER_COUNT; layer++) {
         int start = CenterFuelMenu.OCTAGON_SLOT_START
               + layer * CenterFuelMenu.OCTAGON_LAYER_SIZE;
         for (int point = 0; point < CenterFuelMenu.OCTAGON_LAYER_SIZE; point++) {
            Slot first = this.menu.getSlot(start + point);
            Slot second = this.menu.getSlot(
                  start + (point + 1) % CenterFuelMenu.OCTAGON_LAYER_SIZE);
            drawPixelLine(
                  graphics,
                  x + first.x + 8,
                  y + first.y + 8,
                  x + second.x + 8,
                  y + second.y + 8,
                  0xA06B6253);
         }
      }

      for (int slotIndex = CenterFuelMenu.OCTAGON_SLOT_START;
           slotIndex < CenterFuelMenu.OCTAGON_SLOT_END;
           slotIndex++) {
         Slot slot = this.menu.getSlot(slotIndex);
         graphics.blit(
               TEX_SLOT,
               x + slot.x - 1,
               y + slot.y - 1,
               0.0F,
               0.0F,
               18,
               18,
               18,
               18);
      }
   }

   private static void drawPixelLine(
         GuiGraphics graphics,
         int x0,
         int y0,
         int x1,
         int y1,
         int color) {
      int dx = Math.abs(x1 - x0);
      int sx = x0 < x1 ? 1 : -1;
      int dy = -Math.abs(y1 - y0);
      int sy = y0 < y1 ? 1 : -1;
      int error = dx + dy;

      while (true) {
         graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
         if (x0 == x1 && y0 == y1) {
            break;
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

   private void renderCenterGlyph(GuiGraphics graphics, int centerX, int centerY,
                                  int size, float partialTick) {
      int color = this.menu.glyphColor & 0xFFFFFF;
      float red = ((color >> 16) & 255) / 255.0F;
      float green = ((color >> 8) & 255) / 255.0F;
      float blue = (color & 255) / 255.0F;
      graphics.setColor(red, green, blue, 1.0F);
      float time = (Minecraft.getInstance().level == null
            ? System.currentTimeMillis() / 50.0F
            : Minecraft.getInstance().level.getGameTime() + partialTick);
      ResourceLocation[] textures = centerGlyphTextures();
      if (textures.length == 1) {
         drawGlyphLayer(graphics, textures[0], centerX, centerY, size, time * 0.45F);
      } else {
         // GUI Y points down, so positive Z rotation is clockwise.
         drawGlyphLayer(graphics, textures[0], centerX, centerY, size, time * 0.65F);
         drawGlyphLayer(graphics, textures[1], centerX, centerY, size, -time * 0.52F);
      }
      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private ResourceLocation[] centerGlyphTextures() {
      if (MagicCenterTypeRegistry.FOG.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{FOG_GLYPH_1, FOG_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.INVERTED.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{INVERTED_GLYPH_1, INVERTED_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.FORCEFIELD.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{FORCEFIELD_GLYPH_1, FORCEFIELD_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.SCORCHING.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{SCORCHING_GLYPH_1, SCORCHING_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.SANCTUARY.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{SANCTUARY_GLYPH_1, SANCTUARY_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.FROST.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{FROST_GLYPH_1, FROST_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.DECAY.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{DECAY_GLYPH_1, DECAY_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.THUNDER.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{THUNDER_GLYPH_1, THUNDER_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.DETECTION.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{DETECTION_GLYPH_1, DETECTION_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.REFORGE_BLADE.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{REFORGE_BLADE_GLYPH_1, REFORGE_BLADE_GLYPH_2};
      }
      if (MagicCenterTypeRegistry.TELEPORT.equals(this.menu.centerTypeId)) {
         return new ResourceLocation[]{TELEPORT_GLYPH_1, TELEPORT_GLYPH_2};
      }
      return new ResourceLocation[]{CENTER_GLYPH};
   }

   private static void drawGlyphLayer(GuiGraphics graphics, ResourceLocation texture,
                                      int centerX, int centerY, int size, float angle) {
      graphics.pose().pushPose();
      graphics.pose().translate(centerX, centerY, 120.0F);
      graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
      graphics.blit(texture, -size / 2, -size / 2, size, size,
            0.0F, 0.0F, 128, 128, 128, 128);
      graphics.pose().popPose();
   }

   private static String formatTime(long seconds) {
      long minutes = seconds / 60L;
      return String.format("%02d:%02d", minutes, seconds % 60L);
   }

   private void drawTrimmed(GuiGraphics graphics, Component text, int x, int y, int width) {
      graphics.drawString(this.font, this.font.plainSubstrByWidth(text.getString(), width),
            x, y, 4210752, false);
   }

   private void drawWrapped(GuiGraphics graphics, Component text, int x, int y,
                            int width, int maxLines) {
      List<FormattedCharSequence> lines = this.font.split(text, width);
      int count = Math.min(maxLines, lines.size());
      for (int i = 0; i < count; i++) {
         graphics.drawString(this.font, lines.get(i), x, y + i * 10, 4210752, false);
      }
   }

   protected void renderLabels(GuiGraphics g, int mx, int my) {
      g.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2,
            17, MagicRitualGuiStyle.TEXT_COLOR, false);
      g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
            MagicRitualGuiStyle.TEXT_COLOR, false);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private final class ColorSwatchButton extends Button {
      private final int color;

      private ColorSwatchButton(
            int x,
            int y,
            int width,
            int height,
            int color,
            Runnable onPress) {
         super(
               x,
               y,
               width,
               height,
               Component.empty(),
               button -> onPress.run(),
               DEFAULT_NARRATION);
         this.color = color;
      }

      @Override
      protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
         boolean selected =
               (CenterFuelScreen.this.menu.glyphColor & 0xFFFFFF) == this.color;
         int border = selected
               ? 0xFFFFFFFF
               : isHoveredOrFocused()
                     ? 0xFFE8DDAF
                     : 0xFF4D514B;

         graphics.fill(
               getX(),
               getY(),
               getX() + width,
               getY() + height,
               border);
         graphics.fill(
               getX() + 2,
               getY() + 2,
               getX() + width - 2,
               getY() + height - 2,
               0xFF000000 | this.color);
      }
   }
}
