package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Continuous physical pressure, distinct from elemental Slowness and its cleanses. */
public final class BoneBellEffect extends MobEffect {
    public BoneBellEffect() {
        super(MobEffectCategory.HARMFUL,0xDDD5B9);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"bone_bell"),
                -.30D,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
