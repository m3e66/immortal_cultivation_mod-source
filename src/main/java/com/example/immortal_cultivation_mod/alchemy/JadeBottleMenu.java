package com.example.immortal_cultivation_mod.alchemy;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class JadeBottleMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int bottleSlot;
    private final ItemStack bottle;
    private final SimpleContainer contents;
    public JadeBottleMenu(int id,Inventory inventory,int bottleSlot){
        super(AlchemyContent.BOTTLE_MENU.get(),id);
        if(bottleSlot<0||bottleSlot>=inventory.getContainerSize())throw new IllegalArgumentException("Invalid bottle slot");
        this.inventory=inventory;this.bottleSlot=bottleSlot;this.bottle=inventory.getItem(bottleSlot);
        contents=new SimpleContainer(1){
            @Override public int getMaxStackSize(){return JadeBottleItem.CAPACITY;}
            // Bottle storage is pooled; loose pills still use their item stack limit.
            @Override public int getMaxStackSize(ItemStack stack){return JadeBottleItem.CAPACITY;}
            @Override public void setChanged(){
                super.setChanged();
                if(!inventory.player.level().isClientSide&&inventory.getItem(bottleSlot)==bottle&&bottle.getItem() instanceof JadeBottleItem){
                    JadeBottleItem.store(bottle,getItem(0));inventory.setChanged();
                }
            }
        };
        contents.setItem(0,JadeBottleItem.contents(bottle));
        addSlot(new Slot(contents,0,80,32){
            @Override public boolean mayPlace(ItemStack incoming){return JadeBottleItem.accepts(getItem(),incoming);}
            @Override public int getMaxStackSize(){return JadeBottleItem.CAPACITY;}
            @Override public int getMaxStackSize(ItemStack s){return JadeBottleItem.CAPACITY;}
            @Override public ItemStack remove(int amount){
                return super.remove(Math.min(amount,getItem().getMaxStackSize()));
            }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addPlayerSlot(col+row*9+9,8+col*18,84+row*18);
        for(int col=0;col<9;col++)addPlayerSlot(col,8+col*18,142);
    }
    private void addPlayerSlot(int index,int x,int y){
        addSlot(new Slot(inventory,index,x,y){
            @Override public boolean mayPickup(Player p){return index!=bottleSlot;}
            @Override public boolean mayPlace(ItemStack s){return index!=bottleSlot;}
        });
    }
    @Override public boolean stillValid(Player p){
        return p==inventory.player && bottle.getItem() instanceof JadeBottleItem
                && (p.level().isClientSide||inventory.getItem(bottleSlot)==bottle);
    }
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!stillValid(p)||type==ClickType.SWAP&&button==bottleSlot)return;
        if(slot>=1&&slot<slots.size()&&slots.get(slot).getContainerSlot()==bottleSlot)return;
        if(slot==0&&type==ClickType.SWAP&&(button>=0&&button<9||button==40)){
            Slot storedSlot=slots.get(0);ItemStack stored=storedSlot.getItem();
            if(!stored.isEmpty()&&stored.getCount()>stored.getMaxStackSize()){
                // A full bottle cannot be swapped into one normal inventory slot.
                if(inventory.getItem(button).isEmpty()){
                    ItemStack taken=storedSlot.safeTake(stored.getMaxStackSize(),stored.getMaxStackSize(),p);
                    inventory.setItem(button,taken);inventory.setChanged();
                }
                return;
            }
        }
        super.clicked(slot,button,type,p);
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){
        return slot.container!=inventory||slot.getContainerSlot()!=bottleSlot;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index==0){
            if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY;
        }else if(JadeBottleItem.accepts(contents.getItem(0),stack)){
            if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        }else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(p,stack);contents.setChanged();
        return copy;
    }
}

