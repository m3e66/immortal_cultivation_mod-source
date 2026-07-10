package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<Feature<?>, SurfacePatchFeature> SURFACE_PATCH =
            FEATURES.register("surface_patch", () -> new SurfacePatchFeature(SurfacePatchConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
