package com.example.immortal_cultivation_mod.particle;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ImmortalCultivationMod.MODID);


    public static void register(IEventBus bus) { PARTICLE_TYPES.register(bus); }
}
