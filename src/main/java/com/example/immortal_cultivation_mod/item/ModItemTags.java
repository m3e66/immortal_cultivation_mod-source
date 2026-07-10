package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> CORPSE_CONTROL_TOOLS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "corpse_control_tools")
    );

    private ModItemTags() {
    }
}
