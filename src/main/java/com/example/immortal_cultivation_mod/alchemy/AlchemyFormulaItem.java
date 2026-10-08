package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.item.ForgingYujianItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

/** Reusable formula slip placed in the old ding's jade-slip slot. */
public final class AlchemyFormulaItem extends ForgingYujianItem {
    public final String formula;
    public AlchemyFormulaItem(String id){super(new Properties().stacksTo(1));formula=id;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("alchemy.formula.use").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("alchemy.extra_hint").withStyle(ChatFormatting.DARK_AQUA));
    }
}
