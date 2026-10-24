package com.example.immortal_cultivation_mod.farming;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

public final class SpiritSeedItem extends ItemNameBlockItem {
    private final SpiritPlantSpecies species;
    public SpiritSeedItem(SpiritPlantSpecies species,Block crop,Properties properties){super(crop,properties);this.species=species;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.spirit_seed.days",species.minDays,species.maxDays).withStyle(ChatFormatting.AQUA));
    }
}
