package com.example.immortal_cultivation_mod.magicritual.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class RitualDaggerItem extends SwordItem {
   public RitualDaggerItem(Tier tier, Item.Properties properties) {
      super(tier, properties.attributes(SwordItem.createAttributes(tier, 1, -2.4F)));
   }
}
