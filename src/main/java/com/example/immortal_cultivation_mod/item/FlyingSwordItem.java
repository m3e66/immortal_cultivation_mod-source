package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class FlyingSwordItem extends SwordItem {
    public FlyingSwordItem(Properties properties) {
        super(Tiers.NETHERITE, properties.durability(Tiers.NETHERITE.getUses())
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6.0F, -2.4F)));
    }
}
