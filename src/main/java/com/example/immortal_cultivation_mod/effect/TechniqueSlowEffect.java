package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One-percent movement reduction per amplifier level for exact spell percentages. */
public final class TechniqueSlowEffect extends MobEffect {
    public TechniqueSlowEffect() {
        super(MobEffectCategory.HARMFUL, 0x5C8A74);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "technique_slow"),
                -0.01D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
