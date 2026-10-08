package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem;
import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.TeleportDestinationInfo;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CenterFuelMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 420;
   public static final int GUI_HEIGHT = 354;
   public static final int FUEL_SLOT_X = 381;
   public static final int FUEL_SLOT_Y = 105;
   public static final int WEAPON_SLOT_X = 381;
   public static final int WEAPON_SLOT_Y = 72;
   public static final int OCTAGON_LAYER_SIZE = 8;
   public static final int OCTAGON_LAYER_COUNT = 3;
   public static final int OCTAGON_SLOT_COUNT = 24;
   public static final int[][] OCTAGON_SLOT_POSITIONS = new int[][]{{202, 102}, {223, 111}, {232, 132}, {223, 153}, {202, 162}, {181, 153}, {172, 132}, {181, 111}, {202, 76}, {242, 92}, {258, 132}, {242, 172}, {202, 188}, {162, 172}, {146, 132}, {162, 92}, {202, 50}, {260, 74}, {284, 132}, {260, 190}, {202, 214}, {144, 190}, {120, 132}, {144, 74}};
   public static final int PLAYER_INVENTORY_X = 129;
   public static final int PLAYER_INVENTORY_Y = 266;
   private final ContainerLevelAccess access;
   private final IItemHandler fuelHandler;
   private final IItemHandler reforgeWeaponHandler;
   private final IItemHandler octagonGridHandler;
   private boolean octagonTabActive;
   private boolean teleportTabActive;
   public final BlockPos centerPos;
   public boolean allowHostileMobs;
   public boolean allowPassiveMobs;
   public final Set<String> blacklistedPlayers;
   public final Set<String> controllerPlayers;
   private static final int FUEL_SLOT = 0;
   private static final int WEAPON_SLOT = 1;
   public static final int OCTAGON_SLOT_START = 2;
   public static final int OCTAGON_SLOT_END = 26;
   private static final int CONTAINER_END = 26;
   private static final int PLAYER_INV_START = 26;
   private static final int PLAYER_INV_END = 53;
   private static final int HOTBAR_END = 62;
   public boolean particlesEnabled;
   public int glyphColor;
   public ResourceLocation centerTypeId;
   public int radius;
   public long energy;
   public float barrierHealth;
   public boolean enabled;
   public boolean active;
   public int fuelCostPerMinute;
   public int attackCircles;
   public int defenseCircles;
   public int regenerationCircles;
   public UUID ownerUuid;
   public String ownerLabel;
   public final Set<UUID> excludedPlayerUuids;
   private boolean canControlCenter;
   private boolean canManageControllers;
   public int teleportLinkMode;
   public String teleportDestinationLabel;
   public String teleportCode;
   public String selectedTeleportDestinationCode;
   public final List<TeleportDestinationInfo> teleportDestinations;
   private int accessRevision;
   private int teleportRevision;
   private static final int DATA_RADIUS = 0;
   private static final int DATA_ENERGY_LOW = 1;
   private static final int DATA_ENERGY_HIGH = 2;
   private static final int DATA_ENABLED = 3;
   private static final int DATA_ACTIVE = 4;
   private static final int DATA_COST = 5;
   private static final int DATA_ATTACK = 6;
   private static final int DATA_DEFENSE = 7;
   private static final int DATA_REGENERATION = 8;
   private static final int DATA_BARRIER_HEALTH = 9;
   private static final int DATA_TELEPORT_LINK_MODE = 10;
   private static final int DATA_COUNT = 11;
   private final ContainerData centerData;
   private MagicCenterBlockEntity centerRef;

   public CenterFuelMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
      this(id, playerInv, new ItemStackHandler(1), new ItemStackHandler(1), new ItemStackHandler(24), ContainerLevelAccess.NULL, extraData.readBlockPos(), extraData.readBoolean(), extraData.readBoolean(), (Set)extraData.readCollection(HashSet::new, (buf) -> buf.readUtf()), 16777215);
      this.particlesEnabled = extraData.readBoolean();
      this.glyphColor = extraData.readInt();
      this.centerTypeId = extraData.readResourceLocation();
      this.radius = extraData.readVarInt();
      this.energy = extraData.readVarLong();
      this.barrierHealth = extraData.readFloat();
      this.enabled = extraData.readBoolean();
      this.active = extraData.readBoolean();
      this.fuelCostPerMinute = extraData.readVarInt();
      this.attackCircles = extraData.readVarInt();
      this.defenseCircles = extraData.readVarInt();
      this.regenerationCircles = extraData.readVarInt();
      if (extraData.readBoolean()) {
         this.ownerUuid = extraData.readUUID();
      }

      this.ownerLabel = extraData.readUtf();
      int excludedCount = extraData.readVarInt();

      for(int index = 0; index < excludedCount; ++index) {
         this.excludedPlayerUuids.add(extraData.readUUID());
      }

      this.controllerPlayers.addAll((Set)extraData.readCollection(HashSet::new, (buf) -> buf.readUtf()));
      this.canControlCenter = extraData.readBoolean();
      this.canManageControllers = extraData.readBoolean();
      this.teleportLinkMode = extraData.readVarInt();
      this.teleportDestinationLabel = extraData.readUtf();
      this.teleportCode = extraData.readUtf();
      this.selectedTeleportDestinationCode = extraData.readUtf();
      int teleportDestinationCount = Math.min(32, extraData.readVarInt());

      for(int index = 0; index < teleportDestinationCount; ++index) {
         this.teleportDestinations.add(new TeleportDestinationInfo(extraData.readUtf(), extraData.readUtf(), extraData.readVarInt(), extraData.readVarLong(), extraData.readBoolean()));
      }

      this.seedDataFromFields();
   }

   public CenterFuelMenu(int id, Inventory playerInv, IItemHandler fuelHandler, IItemHandler reforgeWeaponHandler, IItemHandler octagonGridHandler, ContainerLevelAccess access, BlockPos centerPos, boolean allowHostileMobs, boolean allowPassiveMobs, Set<String> blacklistedPlayers, int glyphColor) {
      this(id, playerInv, fuelHandler, reforgeWeaponHandler, octagonGridHandler, access, centerPos, allowHostileMobs, allowPassiveMobs, blacklistedPlayers, glyphColor, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "blank"), 5, 0L, 0.0F, true, false, 16, MagicCenterBlockEntity.CircleCounts.ZERO, (UUID)null, Set.of(), Set.of(), false, false, -1, "-", "", "", List.of());
   }

   public CenterFuelMenu(int id, final Inventory playerInv, IItemHandler fuelHandler, IItemHandler reforgeWeaponHandler, IItemHandler octagonGridHandler, ContainerLevelAccess access, BlockPos centerPos, boolean allowHostileMobs, boolean allowPassiveMobs, Set<String> blacklistedPlayers, int glyphColor, ResourceLocation centerTypeId, int radius, long energy, float barrierHealth, boolean enabled, boolean active, int fuelCostPerMinute, MagicCenterBlockEntity.CircleCounts circleCounts, UUID ownerUuid, Set<UUID> excludedPlayerUuids, Set<String> controllerPlayers, final boolean canControlCenter, boolean canManageControllers, int teleportLinkMode, String teleportDestinationLabel, String teleportCode, String selectedTeleportDestinationCode, List<TeleportDestinationInfo> teleportDestinations) {
      super((MenuType)ModMenuTypes.CENTER_FUEL.get(), id);
      this.blacklistedPlayers = new HashSet();
      this.controllerPlayers = new HashSet();
      this.ownerLabel = "-";
      this.excludedPlayerUuids = new HashSet();
      this.teleportLinkMode = -1;
      this.teleportDestinationLabel = "-";
      this.teleportCode = "";
      this.selectedTeleportDestinationCode = "";
      this.teleportDestinations = new ArrayList();
      this.access = access;
      this.fuelHandler = fuelHandler;
      this.reforgeWeaponHandler = reforgeWeaponHandler;
      this.octagonGridHandler = octagonGridHandler;
      this.centerPos = centerPos;
      this.allowHostileMobs = allowHostileMobs;
      this.allowPassiveMobs = allowPassiveMobs;
      this.blacklistedPlayers.addAll(blacklistedPlayers);
      this.glyphColor = glyphColor & 16777215;
      this.centerTypeId = centerTypeId;
      this.radius = radius;
      this.energy = energy;
      this.barrierHealth = barrierHealth;
      this.enabled = enabled;
      this.active = active;
      this.fuelCostPerMinute = fuelCostPerMinute;
      this.attackCircles = circleCounts.attack();
      this.defenseCircles = circleCounts.defense();
      this.regenerationCircles = circleCounts.regeneration();
      this.ownerUuid = ownerUuid;
      this.ownerLabel = ownerUuid == null ? "-" : ownerUuid.toString();
      this.excludedPlayerUuids.addAll(excludedPlayerUuids);
      this.controllerPlayers.addAll(controllerPlayers);
      this.canControlCenter = canControlCenter;
      this.canManageControllers = canManageControllers;
      this.teleportLinkMode = teleportLinkMode;
      this.teleportDestinationLabel = teleportDestinationLabel;
      this.teleportCode = teleportCode;
      this.selectedTeleportDestinationCode = selectedTeleportDestinationCode;
      this.teleportDestinations.addAll(teleportDestinations);
      this.centerData = new SimpleContainerData(11);
      this.seedDataFromFields();
      this.addDataSlots(this.centerData);
      this.addSlot(new SlotItemHandler(fuelHandler, 0, 381, 105) {
         public boolean mayPickup(Player player) {
            return CenterFuelMenu.this.mayWithdrawFuel(player);
         }

         public boolean isActive() {
            return CenterFuelMenu.this.centerRef != null || !CenterFuelMenu.this.octagonTabActive && !CenterFuelMenu.this.teleportTabActive;
         }
      });
      this.addSlot(new SlotItemHandler(reforgeWeaponHandler, 0, 381, 72) {
         public boolean mayPickup(Player player) {
            return CenterFuelMenu.this.mayWithdrawFuel(player);
         }

         public boolean mayPlace(ItemStack stack) {
            return CenterFuelMenu.this.isReforgeBlade() && stack.isDamageableItem();
         }

         public boolean isActive() {
            return (CenterFuelMenu.this.centerRef != null || !CenterFuelMenu.this.octagonTabActive && !CenterFuelMenu.this.teleportTabActive) && (CenterFuelMenu.this.isReforgeBlade() || this.hasItem());
         }
      });

      for(int slotIndex = 0; slotIndex < 24; ++slotIndex) {
         int[] position = OCTAGON_SLOT_POSITIONS[slotIndex];
         this.addSlot(new SlotItemHandler(octagonGridHandler, slotIndex, position[0], position[1]) {
            public void set(ItemStack stack) {
               ItemStack previous = this.getItem().copy();
               super.set(stack);
               boolean inserted = ModItems.isFormationFlag(stack.getItem()) && (!ModItems.isFormationFlag(previous.getItem()) || stack.getCount() > previous.getCount());
               if (inserted) {
                  Player var5 = playerInv.player;
                  if (var5 instanceof ServerPlayer) {
                     ServerPlayer serverPlayer = (ServerPlayer)var5;
                     DaoGuidanceSystem.record(serverPlayer, "insert_flag");
                  }
               }

            }

            public boolean mayPickup(Player player) {
               return CenterFuelMenu.this.mayEditOctagonGrid(player);
            }

            public boolean mayPlace(ItemStack stack) {
               return canControlCenter && ModItems.isFormationFlag(stack.getItem());
            }

            public boolean isActive() {
               return CenterFuelMenu.this.centerRef != null || CenterFuelMenu.this.octagonTabActive && !CenterFuelMenu.this.teleportTabActive;
            }
         });
      }

      for(int row = 0; row < 3; ++row) {
         for(int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, 9 + row * 9 + col, 129 + col * 18, 266 + row * 18));
         }
      }

      for(int col = 0; col < 9; ++col) {
         this.addSlot(new Slot(playerInv, col, 129 + col * 18, 324));
      }

   }

   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack quickMovedStack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack rawStack = slot.getItem();
         quickMovedStack = rawStack.copy();
         if (slotIndex < 26) {
            if (!slot.mayPickup(player)) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(rawStack, 26, 62, true)) {
               return ItemStack.EMPTY;
            }
         } else if (ModItems.isSpiritStone(rawStack.getItem())) {
            if (!this.moveItemStackTo(rawStack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.isReforgeBlade() && rawStack.isDamageableItem()) {
            if (!this.moveItemStackTo(rawStack, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (!ModItems.isFormationFlag(rawStack.getItem())) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(rawStack, 2, 26, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (rawStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (rawStack.getCount() == quickMovedStack.getCount()) {
            return ItemStack.EMPTY;
         } else {
            slot.onTake(player, rawStack);
            return quickMovedStack;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean stillValid(Player player) {
      return stillValid(this.access, player, (Block)ModBlocks.MAGIC_CENTER_BLOCK.get());
   }

   public boolean isReforgeBlade() {
      return MagicCenterTypeRegistry.REFORGE_BLADE.equals(this.centerTypeId);
   }

   public boolean canControlCenter() {
      return this.canControlCenter;
   }

   public boolean canManageControllers() {
      return this.canManageControllers;
   }

   public boolean isOctagonTabActive() {
      return this.octagonTabActive;
   }

   public void setOctagonTabActive(boolean active) {
      this.octagonTabActive = active;
   }

   public boolean isTeleportTabActive() {
      return this.teleportTabActive;
   }

   public void setTeleportTabActive(boolean active) {
      this.teleportTabActive = active;
   }

   public int accessRevision() {
      return this.accessRevision;
   }

   public void replaceAccessLists(Collection<String> blacklisted, Collection<String> controllers) {
      this.blacklistedPlayers.clear();
      this.blacklistedPlayers.addAll(blacklisted);
      this.controllerPlayers.clear();
      this.controllerPlayers.addAll(controllers);
      ++this.accessRevision;
   }

   public int teleportRevision() {
      return this.teleportRevision;
   }

   public void replaceTeleportDestinations(String localCode, String selectedCode, Collection<TeleportDestinationInfo> destinations) {
      this.teleportCode = localCode;
      this.selectedTeleportDestinationCode = selectedCode;
      this.teleportDestinations.clear();
      this.teleportDestinations.addAll(destinations);
      this.teleportDestinationLabel = (String)this.teleportDestinations.stream().filter((destination) -> destination.code().equals(selectedCode)).map(TeleportDestinationInfo::label).findFirst().orElse(selectedCode != null && !selectedCode.isBlank() ? selectedCode : "-");
      this.teleportLinkMode = selectedCode != null && !selectedCode.isBlank() ? 0 : -1;
      this.centerData.set(10, this.teleportLinkMode);
      ++this.teleportRevision;
   }

   private boolean mayWithdrawFuel(Player player) {
      return this.centerRef == null ? this.canControlCenter : CenterTargetPolicy.canWithdrawFuel(this.centerRef, player);
   }

   private boolean mayEditOctagonGrid(Player player) {
      return this.centerRef == null ? this.canControlCenter : CenterTargetPolicy.canEdit(this.centerRef, player);
   }

   public void bindCenter(MagicCenterBlockEntity center) {
      this.centerRef = center;
      this.ownerLabel = center.getOwnerDisplayLabel();
      this.syncDataFromCenter();
   }

   public void broadcastChanges() {
      this.syncDataFromCenter();
      super.broadcastChanges();
   }

   private void syncDataFromCenter() {
      if (this.centerRef != null) {
         this.radius = this.centerRef.getRadius();
         this.energy = this.centerRef.getEnergy();
         this.barrierHealth = this.centerRef.getBarrierHealth();
         this.enabled = this.centerRef.isEnabled();
         this.active = this.centerRef.isActive();
         this.fuelCostPerMinute = this.centerRef.getCurrentFuelCostPerMinute();
         MagicCenterBlockEntity.CircleCounts counts = this.centerRef.getPassiveCircleCounts();
         this.attackCircles = counts.attack();
         this.defenseCircles = counts.defense();
         this.regenerationCircles = counts.regeneration();
         this.teleportLinkMode = this.centerRef.getTeleportLinkMode();
         this.teleportDestinationLabel = this.centerRef.getTeleportDestinationLabel();
         this.teleportCode = this.centerRef.getTeleportCode();
         this.selectedTeleportDestinationCode = this.centerRef.getSelectedTeleportDestinationCode();
         this.seedDataFromFields();
      }
   }

   private void seedDataFromFields() {
      this.centerData.set(0, this.radius);
      this.centerData.set(1, (int)this.energy);
      this.centerData.set(2, (int)(this.energy >>> 32));
      this.centerData.set(3, this.enabled ? 1 : 0);
      this.centerData.set(4, this.active ? 1 : 0);
      this.centerData.set(5, this.fuelCostPerMinute);
      this.centerData.set(6, this.attackCircles);
      this.centerData.set(7, this.defenseCircles);
      this.centerData.set(8, this.regenerationCircles);
      this.centerData.set(9, Float.floatToIntBits(this.barrierHealth));
      this.centerData.set(10, this.teleportLinkMode);
   }

   public int displayedRadius() {
      return this.centerData.get(0);
   }

   public void setDisplayedRadius(int value) {
      this.radius = value;
      this.centerData.set(0, value);
   }

   public long displayedEnergy() {
      return (long)this.centerData.get(1) & 4294967295L | (long)this.centerData.get(2) << 32;
   }

   public boolean displayedEnabled() {
      return this.centerData.get(3) != 0;
   }

   public boolean displayedActive() {
      return this.centerData.get(4) != 0;
   }

   public int displayedFuelCost() {
      return this.centerData.get(5);
   }

   public int displayedAttackCircles() {
      return this.centerData.get(6);
   }

   public int displayedDefenseCircles() {
      return this.centerData.get(7);
   }

   public int displayedRegenerationCircles() {
      return this.centerData.get(8);
   }

   public float displayedBarrierHealth() {
      return Float.intBitsToFloat(this.centerData.get(9));
   }

   public int displayedTeleportLinkMode() {
      return this.centerData.get(10);
   }

   public void setDisplayedTeleportLinkMode(int mode) {
      this.teleportLinkMode = mode;
      this.centerData.set(10, mode);
   }
}
