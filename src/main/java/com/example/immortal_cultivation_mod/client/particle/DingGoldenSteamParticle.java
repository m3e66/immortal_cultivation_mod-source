package com.example.immortal_cultivation_mod.client.particle;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Fine, translucent vapor drifting from the heated Three-Sun Ding's lid seam. */
public final class DingGoldenSteamParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;

    private DingGoldenSteamParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        lifetime = 30 + random.nextInt(16);
        initialSize = .045F + random.nextFloat() * .025F;
        quadSize = initialSize;
        setColor(1F, .78F, .32F);
        alpha = 0;
        xd = vx; yd = vy; zd = vz;
        friction = .992F;
        gravity = 0;
        hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float progress = age / (float) lifetime;
        alpha = Math.min(1F, age / 6F) * (1F - progress) * .32F;
        quadSize = initialSize * (1F + progress * 1.6F);
        yd += .00015;
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DING_GOLDEN_STEAM.get(),
                s -> (t, l, x, y, z, vx, vy, vz) -> new DingGoldenSteamParticle(l, x, y, z, vx, vy, vz, s));
    }
}
