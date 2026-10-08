package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Status marker used by the corpse-path abilities. */
public final class CorpseStatusEffect extends MobEffect {
    public CorpseStatusEffect(String id, int color, double movementMultiplier) {
        this(id, color, movementMultiplier, 0.0D);
    }

    public CorpseStatusEffect(String id, int color, double movementMultiplier,
                              double attackSpeedMultiplier) {
        super(MobEffectCategory.HARMFUL, color);
        if (movementMultiplier != 0.0D) {
            addAttributeModifier(
                    Attributes.MOVEMENT_SPEED,
                    ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID, id + "_movement"),
                    movementMultiplier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if (attackSpeedMultiplier != 0.0D) {
            addAttributeModifier(
                    Attributes.ATTACK_SPEED,
                    ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID, id + "_attack_speed"),
                    attackSpeedMultiplier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }
}
