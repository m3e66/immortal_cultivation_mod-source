package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

public final class AlchemyMenu extends AbstractContainerMenu {
    public final SimpleContainer ingredients = new SimpleContainer(10);
    public final String formulaId;
    private final BlockPos furnace;
    private final Player owner;
    public AlchemyMenu(int id,Inventory inventory,BlockPos furnace,String formula) {
        super(AlchemyContent.MENU.get(),id);
        this.owner=inventory.player;this.furnace=furnace.immutable();this.formulaId=formula;
        AlchemyChemistry.formula(formula);
        for(int i=0;i<9;i++)addSlot(new Slot(ingredients,i,28+(i%3)*18,42+(i/3)*18){
            @Override public boolean mayPlace(ItemStack stack){return plantId(stack)!=null;}
        });
        addSlot(new Slot(ingredients,9,132,60){
            @Override public boolean mayPlace(ItemStack stack){return false;}
        });
        addInventory(inventory,8,164);
    }
    private void addInventory(Inventory inventory,int x,int y) {
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inventory,col+row*9+9,x+col*18,y+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,x+col*18,y+58));
    }
    public static String plantId(ItemStack stack){
        if(stack.isEmpty())return null;
        var key=BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals("immortal_cultivation_mod")&&AlchemyChemistry.PLANTS.containsKey(key.getPath())?key.getPath():null;
    }
    public static boolean isFurnace(Level level,BlockPos pos){return level.getBlockState(pos).is(Blocks.FURNACE);}
    @Override public boolean stillValid(Player p){
        return p==owner && (p.level().isClientSide || isFurnace(p.level(),furnace)&&p.distanceToSqr(furnace.getCenter())<=64);
    }
    public Map<String,Integer> inputs(){
        Map<String,Integer> counts=new TreeMap<>();
        for(int i=0;i<9;i++){
            ItemStack stack=ingredients.getItem(i);if(stack.isEmpty())continue;
            String key=plantId(stack);if(key==null)return Map.of();
            counts.merge(key,stack.getCount(),Integer::sum);
        }
        return counts;
    }
    @Override public boolean clickMenuButton(Player p,int button){
        // Brewing is exclusively processed by DingBlockEntity using reusable recipe slips.
        return false;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size()||!stillValid(p))return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<10){if(!moveItemStackTo(stack,10,slots.size(),true))return ItemStack.EMPTY;}
        else if(plantId(stack)!=null){if(!moveItemStackTo(stack,0,9,false))return ItemStack.EMPTY;}
        else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(p,stack);return copy;
    }
    @Override public void removed(Player p){
        super.removed(p);
        if(!p.level().isClientSide)clearContainer(p,ingredients);
    }
}

