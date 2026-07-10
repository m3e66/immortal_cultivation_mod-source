package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class TaomuSwordItem extends SwordItem {
    public TaomuSwordItem(Properties properties) {
        super(Tiers.WOOD, properties.durability(Tiers.WOOD.getUses())
                .attributes(SwordItem.createAttributes(Tiers.WOOD, 3.0F, -2.4F)));
    }
}
