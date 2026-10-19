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

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_CRIMSON = registerSimple("vessel_flame_crimson");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_EARTH = registerSimple("vessel_flame_earth");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_WOOD = registerSimple("vessel_flame_wood");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_SOLAR = registerSimple("vessel_flame_solar");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_NETHER = registerSimple("vessel_flame_nether");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_THUNDER = registerSimple("vessel_flame_thunder");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_DRAGON = registerSimple("vessel_flame_dragon");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VESSEL_FLAME_HEAVENLY = registerSimple("vessel_flame_heavenly");

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
    // Keep registry IDs stable for saved particle settings and existing resource packs.
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_YANG =
            registerSimple("method_qi_light");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METHOD_QI_YIN =
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
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOLAR_EMBER =
            registerSimple("solar_ember");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOLAR_CORE =
            registerSimple("solar_core");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOLAR_SIGIL =
            registerSimple("solar_sigil");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLACK_FLAME_CORE =
            registerSimple("black_flame_core");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLACK_FLAME_EDGE =
            registerSimple("black_flame_edge");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLACK_FLAME_ASH =
            registerSimple("black_flame_ash");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLACK_FLAME_SPARK =
            registerSimple("black_flame_spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JINDAN_SONIC_RING =
            registerSimple("jindan_sonic_ring");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JINDAN_SMASH_RING =
            registerSimple("jindan_smash_ring");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> registerSimple(String id) {
        return PARTICLE_TYPES.register(id, () -> new SimpleParticleType(false));
    }

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AMBIENT_ASH = registerSimple("ambient_ash");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DING_GOLDEN_STEAM = registerSimple("ding_golden_steam");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AMBIENT_LEAF = registerSimple("ambient_leaf");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AMBIENT_FROST = registerSimple("ambient_frost");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AMBIENT_EMBER = registerSimple("ambient_ember");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AMBIENT_SPIRIT = registerSimple("ambient_spirit");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPELL_SPARK = registerSimple("spell_library_spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPELL_VOID = registerSimple("spell_library_void");

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
