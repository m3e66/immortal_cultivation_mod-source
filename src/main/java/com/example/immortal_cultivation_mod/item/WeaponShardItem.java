package com.example.immortal_cultivation_mod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Blank shards carry no attributes or restrictions; broken weapons supply their payload. */
public final class WeaponShardItem extends Item {
    public WeaponShardItem(Properties properties){super(properties.stacksTo(1));}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        var data=WeaponShardSystem.data(stack);
        if(data.isEmpty()){
            lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.source",data.getBoolean("custom_name")?Component.literal(data.getString("display_name")):Component.translatable(data.getString("name"))).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(data.getString("item")).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("weapon_tier.immortal_cultivation_mod."+data.getInt("tier")).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.part",data.getInt("index")+1,data.getInt("parts")).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.layers",72).withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.stats",String.format(java.util.Locale.ROOT,"%.1f",data.getDouble("attack")),data.getInt("max_durability")).withStyle(ChatFormatting.GRAY));
        var restrictions=data.getCompound("layers");
        for(var type:WeaponRestrictionSystem.RestrictionType.values())if(restrictions.getInt(type.id())>0)
            lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.restriction",Component.translatable("restriction.immortal_cultivation_mod."+type.id()),restrictions.getInt(type.id())).withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.reforge").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.weapon_shard.additive").withStyle(ChatFormatting.GRAY));
    }
}
