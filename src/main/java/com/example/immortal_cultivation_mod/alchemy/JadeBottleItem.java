package com.example.immortal_cultivation_mod.alchemy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import java.util.List;

public final class JadeBottleItem extends Item {
    public static final int CAPACITY=32;
    public JadeBottleItem(){super(new Properties().stacksTo(1));}
    public static ItemStack contents(ItemStack bottle){
        return bottle.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream()
                .findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY);
    }
    public static void store(ItemStack bottle,ItemStack pills){
        if(!pills.isEmpty()&&(!(pills.getItem() instanceof AlchemyPillItem)||pills.getCount()>CAPACITY))
            throw new IllegalArgumentException("Invalid jade bottle contents");
        if(pills.isEmpty())bottle.remove(DataComponents.CONTAINER);
        else bottle.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(pills.copy())));
    }
    public static boolean accepts(ItemStack existing,ItemStack incoming){
        return incoming.getItem() instanceof AlchemyPillItem
                && (existing.isEmpty()||ItemStack.isSameItemSameComponents(existing,incoming));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack bottle=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            if(player.isShiftKeyDown()){
                int slot=hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40;
                server.openMenu(new SimpleMenuProvider((id,inv,p)->new JadeBottleMenu(id,inv,slot),
                        Component.translatable("item.immortal_cultivation_mod.jade_pill_bottle")),buf->buf.writeVarInt(slot));
            } else {
                ItemStack pills=contents(bottle);
                if(!pills.isEmpty()&&AlchemyEffects.consume(server,pills)){
                    pills.shrink(1);store(bottle,pills);
                    player.getInventory().setChanged();
                    player.inventoryMenu.broadcastChanges();
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(bottle,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        ItemStack pills=contents(stack);
        if(pills.isEmpty())lines.add(Component.translatable("alchemy.empty").withStyle(ChatFormatting.GRAY));
        else {
            lines.add(pills.getHoverName().copy().append(" ×"+pills.getCount()).withStyle(ChatFormatting.GOLD));
            PillData.tooltip(pills,lines);
        }
        lines.add(Component.translatable("alchemy.capacity",pills.getCount(),CAPACITY).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("alchemy.bottle.controls").withStyle(ChatFormatting.GRAY));
    }
}

