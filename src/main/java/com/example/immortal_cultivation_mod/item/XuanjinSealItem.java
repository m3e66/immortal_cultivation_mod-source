package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.companion.XuanjinCompanion;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public final class XuanjinSealItem extends Item {
    public XuanjinSealItem(Properties properties) { super(properties.stacksTo(1).fireResistant()); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer p && XuanjinCompanion.bind(p,stack)) {
            if(player.isShiftKeyDown()) XuanjinCompanion.execute(p,"recall"); else XuanjinCompanion.menu(p);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("xuanjin.tooltip"));
        lines.add(Component.translatable("xuanjin.tooltip_controls"));
    }
}
