package com.example.immortal_cultivation_mod.magicritual.center;

import net.minecraft.resources.ResourceLocation;

public record MagicCenterType(ResourceLocation id, String translationKey, String descriptionKey, String visualPattern, MagicCenterBehavior behavior) {
}
