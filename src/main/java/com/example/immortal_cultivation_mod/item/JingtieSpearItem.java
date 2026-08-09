package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class JingtieSpearItem extends SwordItem {

    public JingtieSpearItem(Properties properties) {
        super(Tiers.NETHERITE, properties.durability(Tiers.NETHERITE.getUses())
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 10.0F, -2.4F)));
    }
}
