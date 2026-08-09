package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class FlyingNeedleItem extends Item {
    public static final float BASE_DAMAGE = 4.0F;
    public static final int MAX_DURABILITY = 1000;
    public static final int DURABILITY_PER_SHOT = 10;

    public FlyingNeedleItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .durability(MAX_DURABILITY)
                .attributes(SwordItem.createAttributes(
                        Tiers.WOOD, 3.0F, -2.4F)));
    }
}
