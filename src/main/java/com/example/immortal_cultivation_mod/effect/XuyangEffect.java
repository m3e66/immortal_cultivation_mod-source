package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The thirty-second solar exhaustion caused by ending 极阳一刻. */
public final class XuyangEffect extends MobEffect {
    public XuyangEffect() {
        super(MobEffectCategory.HARMFUL, 0x8E5A2D);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "xuyang_speed"),
                -0.20D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
