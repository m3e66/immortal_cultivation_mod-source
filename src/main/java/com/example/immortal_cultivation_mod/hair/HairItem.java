package com.example.immortal_cultivation_mod.hair;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class HairItem extends Item implements ICurioItem {
    public final int style;
    public final boolean accessory;
    public HairItem(int style, boolean accessory) {
        super(new Properties().stacksTo(1)); this.style = style; this.accessory = accessory;
    }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return context.identifier().equals(accessory ? "hair_accessory" : "hair");
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<Component> lines, TooltipFlag flag) {
        if (!accessory) lines.add(Component.literal(String.format("#%06X", HairData.color(stack))));
    }
}
