package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared wear thresholds for worn clothing and vanilla damage-based item icons. */
public final class ClothingWear {
    private static final String[] SUFFIXES = {"", "_worn", "_damaged", "_tattered"};

    private ClothingWear() {
    }

    public static int stage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int maximum = stack.getMaxDamage();
        if (maximum <= 0) {
            return 0;
        }

        // Long arithmetic also supports datapacks that raise an item's maximum durability.
        return (int)Math.clamp((long)stack.getDamageValue() * 4 / maximum, 0, 3);
    }

    public static ResourceLocation[] textures(String name) {
        ResourceLocation[] textures = new ResourceLocation[SUFFIXES.length];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/item/armor/" + name + SUFFIXES[i] + ".png");
        }
        return textures;
    }
}
