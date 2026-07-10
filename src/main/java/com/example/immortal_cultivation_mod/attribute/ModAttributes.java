package com.example.immortal_cultivation_mod.attribute;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<Attribute, Attribute> GRUDGE = ATTRIBUTES.register("grudge",
            () -> new RangedAttribute(
                    "attribute.name." + ImmortalCultivationMod.MODID + ".grudge",
                    0.0D,
                    0.0D,
                    ImmortalCultivationMod.MAX_CULTIVATION_HEALTH)
                    .setSyncable(true));
}
