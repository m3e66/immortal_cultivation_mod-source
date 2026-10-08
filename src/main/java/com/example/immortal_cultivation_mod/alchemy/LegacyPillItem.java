package com.example.immortal_cultivation_mod.alchemy;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Original pills retain their use behavior and share the alchemy tooltip presentation. */
public class LegacyPillItem extends Item {
    public LegacyPillItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        PillData.tooltip(stack, lines);
    }
}
