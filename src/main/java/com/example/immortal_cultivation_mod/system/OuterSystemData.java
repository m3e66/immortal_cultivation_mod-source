package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.OuterSystemItem;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class OuterSystemData {
    public static final String ROOT_TAG = "ImmortalCultivationOuterSystem";
    public static final String EVER_BOUND = "EverBound";
    public static final String DESTROYED = "Destroyed";
    public static final String TYPE = "Type";
    public static final String PREVIOUS_TYPE = "PreviousType";
    public static final String BOOT_SHOWN = "BootShown";
    public static final String QUEST = "Quest";
    private static final ResourceLocation SYSTEM_SLOT_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "outer_system_slot");

    private OuterSystemData() {
    }

    public static CompoundTag read(Player player) {
        return player.getPersistentData().contains(ROOT_TAG)
                ? player.getPersistentData().getCompound(ROOT_TAG).copy()
                : new CompoundTag();
    }

    public static void save(Player player, CompoundTag state) {
        player.getPersistentData().put(ROOT_TAG, state.copy());
    }

    public static void copy(Player original, Player replacement) {
        if (original.getPersistentData().contains(ROOT_TAG)) {
            replacement.getPersistentData().put(
                    ROOT_TAG,
                    original.getPersistentData().getCompound(ROOT_TAG).copy());
        }
    }

    public static Optional<ItemStack> equipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(ModItems.SYSTEM.get()))
                .filter(result -> OuterSystemItem.SLOT.equals(
                        result.slotContext().identifier()))
                .map(result -> result.stack());
    }

    public static void ensureSlot(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            boolean present = handler.getModifiers().get(OuterSystemItem.SLOT)
                    .stream()
                    .anyMatch(modifier -> SYSTEM_SLOT_MODIFIER.equals(
                            modifier.id()));
            if (!present) {
                handler.addPermanentSlotModifier(
                        OuterSystemItem.SLOT,
                        SYSTEM_SLOT_MODIFIER,
                        1.0D,
                        AttributeModifier.Operation.ADD_VALUE);
                handler.processSlots();
            }
        });
    }

    public static boolean equip(Player player, ItemStack stack) {
        final boolean[] equipped = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getStacksHandler(OuterSystemItem.SLOT).ifPresent(stacksHandler -> {
                    if (stacksHandler.getStacks().getSlots() > 0
                            && stacksHandler.getStacks().getStackInSlot(0).isEmpty()) {
                        handler.setEquippedCurio(
                                OuterSystemItem.SLOT, 0, stack.copyWithCount(1));
                        equipped[0] = true;
                    }
                }));
        return equipped[0];
    }

    public static void removeEquipped(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getStacksHandler(OuterSystemItem.SLOT).ifPresent(stacksHandler -> {
                    for (int slot = 0;
                         slot < stacksHandler.getStacks().getSlots();
                         slot++) {
                        if (stacksHandler.getStacks().getStackInSlot(slot)
                                .is(ModItems.SYSTEM.get())) {
                            handler.setEquippedCurio(
                                    OuterSystemItem.SLOT, slot, ItemStack.EMPTY);
                        }
                    }
                }));
    }

    public static void removeSlot(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            boolean present = handler.getModifiers().get(OuterSystemItem.SLOT)
                    .stream()
                    .anyMatch(modifier -> SYSTEM_SLOT_MODIFIER.equals(
                            modifier.id()));
            if (present) {
                handler.removeSlotModifier(
                        OuterSystemItem.SLOT, SYSTEM_SLOT_MODIFIER);
                handler.processSlots();
            }
        });
    }

    public static OuterSystemType type(Player player) {
        Optional<ItemStack> equipped = equipped(player);
        if (equipped.isPresent()) {
            return OuterSystemItem.getType(equipped.get());
        }
        return OuterSystemType.byId(read(player).getString(TYPE));
    }
}
