package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;

/** Common particle lookup; providers and the supplied bitmap live exclusively on the client. */
public final class FlameParticles {
    private FlameParticles() { }
    public static SimpleParticleType type(FlameKind kind) {
        return switch (kind) {
            case EARTH -> ModParticles.VESSEL_FLAME_EARTH.get();
            case WOOD -> ModParticles.VESSEL_FLAME_WOOD.get();
            case SOLAR -> ModParticles.VESSEL_FLAME_SOLAR.get();
            case NETHER -> ModParticles.VESSEL_FLAME_NETHER.get();
            case THUNDER -> ModParticles.VESSEL_FLAME_THUNDER.get();
            case DRAGON -> ModParticles.VESSEL_FLAME_DRAGON.get();
            case HEAVENLY -> ModParticles.VESSEL_FLAME_HEAVENLY.get();
            default -> ModParticles.VESSEL_FLAME_CRIMSON.get();
        };
    }
}
