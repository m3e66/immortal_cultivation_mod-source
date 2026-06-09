package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> LINGBENG =
            MOB_EFFECTS.register("lingbeng", LingbengEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> EARTH_ESCAPE =
            MOB_EFFECTS.register("earth_escape", EarthEscapeEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> QI_GATHERING =
            MOB_EFFECTS.register("qi_gathering", QiGatheringEffect::new);

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
