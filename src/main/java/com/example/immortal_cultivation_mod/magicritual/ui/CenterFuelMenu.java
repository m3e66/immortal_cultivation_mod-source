package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
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
   public static final int OCTAGON_SLOT_COUNT = OCTAGON_LAYER_SIZE * OCTAGON_LAYER_COUNT;
   public static final int[][] OCTAGON_SLOT_POSITIONS = {
      // Layer 1 - inner octagon, clockwise from the top.
      {202, 102}, {223, 111}, {232, 132}, {223, 153},
      {202, 162}, {181, 153}, {172, 132}, {181, 111},
      // Layer 2 - middle octagon.
      {202, 76}, {242, 92}, {258, 132}, {242, 172},
      {202, 188}, {162, 172}, {146, 132}, {162, 92},
      // Layer 3 - outer octagon.
      {202, 50}, {260, 74}, {284, 132}, {260, 190},
      {202, 214}, {144, 190}, {120, 132}, {144, 74}
   };
   public static final int PLAYER_INVENTORY_X = (GUI_WIDTH - 162) / 2;
   public static final int PLAYER_INVENTORY_Y = 266;
   private final ContainerLevelAccess access;
   private final IItemHandler fuelHandler;
   private final IItemHandler reforgeWeaponHandler;
   private final IItemHandler octagonGridHandler;
   private boolean octagonTabActive;
   public final BlockPos centerPos;
   public boolean allowHostileMobs;
   public boolean allowPassiveMobs;
   public final Set<String> blacklistedPlayers = new HashSet<>();
   public final Set<String> controllerPlayers = new HashSet<>();
   private static final int FUEL_SLOT = 0;
   private static final int WEAPON_SLOT = 1;
   public static final int OCTAGON_SLOT_START = 2;
   public static final int OCTAGON_SLOT_END = OCTAGON_SLOT_START + OCTAGON_SLOT_COUNT;
   private static final int CONTAINER_END = OCTAGON_SLOT_END;
   private static final int PLAYER_INV_START = CONTAINER_END;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;
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
   public String ownerLabel = "-";
   public final Set<UUID> excludedPlayerUuids = new HashSet<>();
   private boolean canControlCenter;
   private boolean canManageControllers;
   public int teleportLinkMode = -1;
   public String teleportDestinationLabel = "-";
   private int accessRevision;
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
      this(
         id,
         playerInv,
         new ItemStackHandler(1),
         new ItemStackHandler(1),
         new ItemStackHandler(OCTAGON_SLOT_COUNT),
         ContainerLevelAccess.NULL,
         extraData.readBlockPos(),
         extraData.readBoolean(),
         extraData.readBoolean(),
         (Set<String>)extraData.readCollection(HashSet::new, buf -> buf.readUtf()),
         0xFFFFFF
      );
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
      for (int index = 0; index < excludedCount; index++) {
         this.excludedPlayerUuids.add(extraData.readUUID());
      }
      this.controllerPlayers.addAll((Set<String>)extraData.readCollection(
            HashSet::new, buf -> buf.readUtf()));
      this.canControlCenter = extraData.readBoolean();
      this.canManageControllers = extraData.readBoolean();
      this.teleportLinkMode = extraData.readVarInt();
      this.teleportDestinationLabel = extraData.readUtf();
      seedDataFromFields();
   }

   public CenterFuelMenu(
      int id,
      Inventory playerInv,
      IItemHandler fuelHandler,
      IItemHandler reforgeWeaponHandler,
      IItemHandler octagonGridHandler,
      ContainerLevelAccess access,
      BlockPos centerPos,
      boolean allowHostileMobs,
      boolean allowPassiveMobs,
      Set<String> blacklistedPlayers,
      int glyphColor
   ) {
      this(id, playerInv, fuelHandler, reforgeWeaponHandler, octagonGridHandler,
            access, centerPos, allowHostileMobs,
            allowPassiveMobs, blacklistedPlayers, glyphColor,
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "blank"),
            5, 0L, 0.0F, true, false, 16,
            MagicCenterBlockEntity.CircleCounts.ZERO, null, Set.of(), Set.of(),
            false, false, -1, "-");
   }

   public CenterFuelMenu(
      int id,
      Inventory playerInv,
      IItemHandler fuelHandler,
      IItemHandler reforgeWeaponHandler,
      IItemHandler octagonGridHandler,
      ContainerLevelAccess access,
      BlockPos centerPos,
      boolean allowHostileMobs,
      boolean allowPassiveMobs,
      Set<String> blacklistedPlayers,
      int glyphColor,
      ResourceLocation centerTypeId,
      int radius,
      long energy,
      float barrierHealth,
      boolean enabled,
      boolean active,
      int fuelCostPerMinute,
      MagicCenterBlockEntity.CircleCounts circleCounts,
      UUID ownerUuid,
      Set<UUID> excludedPlayerUuids,
      Set<String> controllerPlayers,
      boolean canControlCenter,
      boolean canManageControllers,
      int teleportLinkMode,
      String teleportDestinationLabel
   ) {
      super((MenuType)ModMenuTypes.CENTER_FUEL.get(), id);
      this.access = access;
      this.fuelHandler = fuelHandler;
      this.reforgeWeaponHandler = reforgeWeaponHandler;
      this.octagonGridHandler = octagonGridHandler;
      this.centerPos = centerPos;
      this.allowHostileMobs = allowHostileMobs;
      this.allowPassiveMobs = allowPassiveMobs;
      this.blacklistedPlayers.addAll(blacklistedPlayers);
      this.glyphColor = glyphColor & 0xFFFFFF;
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
      this.centerData = new SimpleContainerData(DATA_COUNT);
      seedDataFromFields();
      addDataSlots(centerData);
      this.addSlot(new SlotItemHandler(fuelHandler, 0, FUEL_SLOT_X, FUEL_SLOT_Y) {
         @Override
         public boolean mayPickup(Player player) {
            return mayWithdrawFuel(player);
         }

         @Override
         public boolean isActive() {
            return centerRef != null || !octagonTabActive;
         }
      });
      this.addSlot(new SlotItemHandler(reforgeWeaponHandler, 0, WEAPON_SLOT_X, WEAPON_SLOT_Y) {
         @Override
         public boolean mayPickup(Player player) {
            return mayWithdrawFuel(player);
         }

         @Override
         public boolean mayPlace(ItemStack stack) {
            return isReforgeBlade() && stack.isDamageableItem();
         }

         @Override
         public boolean isActive() {
            return (centerRef != null || !octagonTabActive)
                  && (isReforgeBlade() || hasItem());
         }
      });

      for (int slotIndex = 0; slotIndex < OCTAGON_SLOT_COUNT; slotIndex++) {
         int[] position = OCTAGON_SLOT_POSITIONS[slotIndex];
         this.addSlot(new SlotItemHandler(
               octagonGridHandler, slotIndex, position[0], position[1]) {
            @Override
            public boolean mayPickup(Player player) {
               return mayEditOctagonGrid(player);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
               return canControlCenter && ModItems.isFormationFlag(stack.getItem());
            }

            @Override
            public boolean isActive() {
               return centerRef != null || octagonTabActive;
            }
         });
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, 9 + row * 9 + col,
                  PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInv, col,
               PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + 58));
      }
   }

   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack quickMovedStack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack rawStack = slot.getItem();
         quickMovedStack = rawStack.copy();
         if (slotIndex < CONTAINER_END) {
            if (!slot.mayPickup(player)) {
               return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(rawStack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (ModItems.isSpiritStone(rawStack.getItem())) {
               if (!this.moveItemStackTo(rawStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (isReforgeBlade() && rawStack.isDamageableItem()) {
               if (!this.moveItemStackTo(rawStack, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (ModItems.isFormationFlag(rawStack.getItem())) {
               if (!this.moveItemStackTo(rawStack, OCTAGON_SLOT_START, OCTAGON_SLOT_END, false)) {
                  return ItemStack.EMPTY;
               }
            } else {
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
      return MagicCenterTypeRegistry.REFORGE_BLADE.equals(centerTypeId);
   }

   public boolean canControlCenter() {
      return canControlCenter;
   }

   public boolean canManageControllers() {
      return canManageControllers;
   }

   public boolean isOctagonTabActive() {
      return octagonTabActive;
   }

   public void setOctagonTabActive(boolean active) {
      this.octagonTabActive = active;
   }

   public int accessRevision() {
      return accessRevision;
   }

   public void replaceAccessLists(Collection<String> blacklisted,
                                  Collection<String> controllers) {
      blacklistedPlayers.clear();
      blacklistedPlayers.addAll(blacklisted);
      controllerPlayers.clear();
      controllerPlayers.addAll(controllers);
      accessRevision++;
   }

   private boolean mayWithdrawFuel(Player player) {
      return centerRef == null
            ? canControlCenter
            : CenterTargetPolicy.canWithdrawFuel(centerRef, player);
   }

   private boolean mayEditOctagonGrid(Player player) {
      return centerRef == null
            ? canControlCenter
            : CenterTargetPolicy.canEdit(centerRef, player);
   }

   public void bindCenter(MagicCenterBlockEntity center) {
      this.centerRef = center;
      this.ownerLabel = center.getOwnerDisplayLabel();
      syncDataFromCenter();
   }

   @Override
   public void broadcastChanges() {
      syncDataFromCenter();
      super.broadcastChanges();
   }

   private void syncDataFromCenter() {
      if (centerRef == null) {
         return;
      }
      radius = centerRef.getRadius();
      energy = centerRef.getEnergy();
      barrierHealth = centerRef.getBarrierHealth();
      enabled = centerRef.isEnabled();
      active = centerRef.isActive();
      fuelCostPerMinute = centerRef.getCurrentFuelCostPerMinute();
      MagicCenterBlockEntity.CircleCounts counts = centerRef.getPassiveCircleCounts();
      attackCircles = counts.attack();
      defenseCircles = counts.defense();
      regenerationCircles = counts.regeneration();
      teleportLinkMode = centerRef.getTeleportLinkMode();
      teleportDestinationLabel = centerRef.getTeleportDestinationLabel();
      seedDataFromFields();
   }

   private void seedDataFromFields() {
      centerData.set(DATA_RADIUS, radius);
      centerData.set(DATA_ENERGY_LOW, (int)energy);
      centerData.set(DATA_ENERGY_HIGH, (int)(energy >>> 32));
      centerData.set(DATA_ENABLED, enabled ? 1 : 0);
      centerData.set(DATA_ACTIVE, active ? 1 : 0);
      centerData.set(DATA_COST, fuelCostPerMinute);
      centerData.set(DATA_ATTACK, attackCircles);
      centerData.set(DATA_DEFENSE, defenseCircles);
      centerData.set(DATA_REGENERATION, regenerationCircles);
      centerData.set(DATA_BARRIER_HEALTH, Float.floatToIntBits(barrierHealth));
      centerData.set(DATA_TELEPORT_LINK_MODE, teleportLinkMode);
   }

   public int displayedRadius() {
      return centerData.get(DATA_RADIUS);
   }

   public void setDisplayedRadius(int value) {
      radius = value;
      centerData.set(DATA_RADIUS, value);
   }

   public long displayedEnergy() {
      return (centerData.get(DATA_ENERGY_LOW) & 0xFFFFFFFFL)
            | ((long)centerData.get(DATA_ENERGY_HIGH) << 32);
   }

   public boolean displayedEnabled() {
      return centerData.get(DATA_ENABLED) != 0;
   }

   public boolean displayedActive() {
      return centerData.get(DATA_ACTIVE) != 0;
   }

   public int displayedFuelCost() {
      return centerData.get(DATA_COST);
   }

   public int displayedAttackCircles() {
      return centerData.get(DATA_ATTACK);
   }

   public int displayedDefenseCircles() {
      return centerData.get(DATA_DEFENSE);
   }

   public int displayedRegenerationCircles() {
      return centerData.get(DATA_REGENERATION);
   }

   public float displayedBarrierHealth() {
      return Float.intBitsToFloat(centerData.get(DATA_BARRIER_HEALTH));
   }

   public int displayedTeleportLinkMode() {
      return centerData.get(DATA_TELEPORT_LINK_MODE);
   }

   public void setDisplayedTeleportLinkMode(int mode) {
      teleportLinkMode = mode;
      centerData.set(DATA_TELEPORT_LINK_MODE, mode);
   }
}
