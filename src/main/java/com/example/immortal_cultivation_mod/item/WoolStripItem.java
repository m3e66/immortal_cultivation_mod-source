package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

public class WoolStripItem extends Item {
    private final DyeColor color;

    public WoolStripItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor color() {
        return color;
    }
}
