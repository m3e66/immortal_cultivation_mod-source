package com.example.immortal_cultivation_mod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class MethodQiParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;

    protected MethodQiParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed,
                               SpriteSet sprites, int color) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.lifetime = 14 + random.nextInt(9);
        this.gravity = -0.018F;
        this.hasPhysics = false;
        this.initialSize = 0.10F + random.nextFloat() * 0.09F;
        this.quadSize = initialSize;
        this.alpha = 0.92F;
        this.rCol = ((color >> 16) & 255) / 255.0F;
        this.gCol = ((color >> 8) & 255) / 255.0F;
        this.bCol = (color & 255) / 255.0F;
        this.xd = xSpeed + (random.nextDouble() - 0.5D) * 0.025D;
        this.yd = Math.abs(ySpeed) * 0.35D + 0.025D + random.nextDouble() * 0.025D;
        this.zd = zSpeed + (random.nextDouble() - 0.5D) * 0.025D;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        setSpriteFromAge(sprites);
        float life = (float) age / lifetime;
        alpha = 0.92F * (1.0F - life);
        quadSize = initialSize * (0.85F + life * 0.45F);
        xd *= 0.96D;
        yd *= 0.985D;
        zd *= 0.96D;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final int color;

        public Provider(SpriteSet sprites, int color) {
            this.sprites = sprites;
            this.color = color;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new MethodQiParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, color);
        }
    }
}
