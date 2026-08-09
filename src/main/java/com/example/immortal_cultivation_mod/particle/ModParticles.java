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

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_METAL =
            registerSimple("method_qi_metal");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_WOOD =
            registerSimple("method_qi_wood");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_WATER =
            registerSimple("method_qi_water");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_FIRE =
            registerSimple("method_qi_fire");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_EARTH =
            registerSimple("method_qi_earth");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_THUNDER =
            registerSimple("method_qi_thunder");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_ICE =
            registerSimple("method_qi_ice");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_WIND =
            registerSimple("method_qi_wind");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_LIGHT =
            registerSimple("method_qi_light");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_DARK =
            registerSimple("method_qi_dark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_BLOOD =
            registerSimple("method_qi_blood");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_NEUTRAL =
            registerSimple("method_qi_neutral");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON_EMBER =
            registerSimple("dragon_ember");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON_SCALE_SPARK =
            registerSimple("dragon_scale_spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON_FIRE_SIGIL =
            registerSimple("dragon_fire_sigil");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> registerSimple(String id) {
        return PARTICLE_TYPES.register(id, () -> new SimpleParticleType(false));
    }

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
