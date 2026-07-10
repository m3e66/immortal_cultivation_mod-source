package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class CorpseControlToolCycler {
    private static final String WAIST_ACCESSORY = "waist_accessory";

    private CorpseControlToolCycler() {
    }

    public static boolean cycle(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler(WAIST_ACCESSORY)
                        .map(waist -> cycle(player, handler, waist.getStacks())))
                .orElse(false);
    }

    private static boolean cycle(ServerPlayer player, top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler,
                                 top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler waistStacks) {
        ItemStack hand = player.getMainHandItem();
        boolean handIsTool = isTool(hand);
        if (!hand.isEmpty() && !handIsTool) {
            return false;
        }

        int toolSlot = firstToolSlot(waistStacks);
        if (toolSlot < 0) {
            if (handIsTool) {
                int emptySlot = firstEmptySlot(waistStacks);
                if (emptySlot >= 0) {
                    handler.setEquippedCurio(WAIST_ACCESSORY, emptySlot, hand.copy());
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".cycled_tool"), true);
                    return true;
                }
            }
            return false;
        }

        ItemStack nextTool = waistStacks.getStackInSlot(toolSlot).copy();
        handler.setEquippedCurio(WAIST_ACCESSORY, toolSlot, handIsTool ? hand.copy() : ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, nextTool);
        player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".cycled_tool"), true);
        return true;
    }

    private static int firstToolSlot(top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (isTool(stacks.getStackInSlot(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int firstEmptySlot(top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isTool(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItemTags.CORPSE_CONTROL_TOOLS);
    }
}
