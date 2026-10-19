package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.BeastRealms;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** A species ingredient retains its actual drop realm without duplicating item IDs. */
public class BeastMaterialItem extends Item {
    private final int defaultRealm;
    public BeastMaterialItem(Properties properties, int defaultRealm) {
        super(properties);
        this.defaultRealm = defaultRealm;
    }
    @Override public Component getName(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int realm = tag.contains(BeastRealms.NBT_KEY) ? tag.getInt(BeastRealms.NBT_KEY) : defaultRealm;
        return BeastRealms.prefix(realm, super.getName(stack));
    }
}
