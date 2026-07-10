package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class CopperCoinSwordItem extends SwordItem {
    public CopperCoinSwordItem(Properties properties) {
        super(Tiers.IRON, properties.durability(Tiers.IRON.getUses())
                .attributes(SwordItem.createAttributes(Tiers.IRON, 4.0F, -2.4F)));
    }
}
