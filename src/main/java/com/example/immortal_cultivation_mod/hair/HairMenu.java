package com.example.immortal_cultivation_mod.hair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Applies drafts as one server transaction; the UI never mutates equipment. */
public final class HairMenu extends AbstractContainerMenu {
    private final Player owner;
    private final BlockPos table;
    private final ContainerData palettes = new SimpleContainerData(34);
    public HairMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public HairMenu(int id, Inventory inventory, BlockPos table) {
        super(HairContent.MENU.get(), id); this.owner = inventory.player; this.table = table;
        addDataSlots(palettes); if (table != null) refreshColors();
    }
    @Override public boolean stillValid(Player p) {
        return p == owner && (p.level().isClientSide || table != null
                && p.level().getBlockState(table).is(HairContent.TABLE.get())
                && p.distanceToSqr(table.getX() + .5, table.getY() + .5, table.getZ() + .5) <= 64);
    }
    @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    public int[] colors(boolean saved) {
        int count = palettes.get(saved ? 1 : 0); int offset = saved ? 18 : 2;
        int[] result = new int[Math.clamp(count, 0, 8)];
        for (int i=0;i<result.length;i++) result[i] = (palettes.get(offset+i*2)&65535) | ((palettes.get(offset+i*2+1)&255)<<16);
        return result;
    }
    private void refreshColors() {
        for (boolean saved : new boolean[]{false,true}) {
            int[] colors=HairData.colors(owner,saved); palettes.set(saved?1:0,Math.min(8,colors.length));
            int offset=saved?18:2;
            for(int i=0;i<Math.min(8,colors.length);i++) {
                palettes.set(offset+i*2,colors[i]&65535); palettes.set(offset+i*2+1,colors[i]>>>16);
            }
        }
        broadcastChanges();
    }
    public boolean apply(Player player, int style, int color, int helmet, boolean saveColor) {
        if (player.level().isClientSide || !stillValid(player) || style < -1 || style >= HairStyle.values().length
                || color < 0 || color > 0xFFFFFF || helmet < 0 || helmet > 2) return false;
        var hair = HairData.slot(player,"hair");
        if (hair == null) return false;
        ItemStack oldHair=hair.getStackInSlot(0);
        // Do not overwrite unrelated items inserted by another mod's slot rules.
        if (!oldHair.isEmpty() && HairData.style(oldHair)<0) return false;
        hair.setStackInSlot(0,style<0?ItemStack.EMPTY:HairData.create(style,color,helmet));
        HairData.remember(player,color,false); if(saveColor) HairData.remember(player,color,true);
        refreshColors(); player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        return true;
    }
}
