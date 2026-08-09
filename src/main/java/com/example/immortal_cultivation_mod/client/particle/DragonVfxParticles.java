package com.example.immortal_cultivation_mod.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/** Client-only particles used by the Crimson Sky Flame Flood Dragon's abilities. */
public final class DragonVfxParticles {
    private DragonVfxParticles() {
    }

    private static final class Ember extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float initialSize;
        private final float spinSpeed;

        private Ember(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            lifetime = 16 + random.nextInt(10);
            hasPhysics = false;
            gravity = -0.015F;
            friction = 0.94F;
            initialSize = 0.24F + random.nextFloat() * 0.24F;
            quadSize = initialSize;
            spinSpeed = (random.nextBoolean() ? 1.0F : -1.0F)
                    * (0.08F + random.nextFloat() * 0.13F);
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            rCol = 1.0F;
            gCol = 0.12F + random.nextFloat() * 0.24F;
            bCol = 0.015F;
            alpha = 0.96F;
            xd = xSpeed;
            yd = ySpeed + 0.008D;
            zd = zSpeed;
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
            alpha = 0.96F * Mth.clamp((1.0F - life) * 1.35F, 0.0F, 1.0F);
            quadSize = initialSize * (0.72F + Mth.sin(life * Mth.PI) * 0.72F);
            oRoll = roll;
            roll += spinSpeed;

            // A slight curl keeps streams from looking like straight vanilla particle lines.
            double oldX = xd;
            xd = oldX * 0.985D - zd * 0.045D;
            zd = zd * 0.985D + oldX * 0.045D;
            yd += 0.0025D;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private static final class ScaleSpark extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float initialSize;
        private final float spinSpeed;

        private ScaleSpark(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            lifetime = 9 + random.nextInt(8);
            hasPhysics = false;
            gravity = 0.18F;
            friction = 0.91F;
            initialSize = 0.13F + random.nextFloat() * 0.13F;
            quadSize = initialSize;
            spinSpeed = (random.nextBoolean() ? 1.0F : -1.0F)
                    * (0.28F + random.nextFloat() * 0.32F);
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            rCol = 1.0F;
            gCol = 0.58F + random.nextFloat() * 0.30F;
            bCol = 0.05F;
            alpha = 1.0F;
            xd = xSpeed;
            yd = ySpeed;
            zd = zSpeed;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            setSpriteFromAge(sprites);
            float remaining = 1.0F - (float) age / lifetime;
            alpha = Mth.clamp(remaining * 1.45F, 0.0F, 1.0F);
            quadSize = initialSize * (0.25F + remaining * 1.35F);
            oRoll = roll;
            roll += spinSpeed;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /**
     * A ground-facing rune. Exact-particle velocity fields encode radius, spin,
     * and lifetime so no custom network payload is required.
     */
    private static final class FireSigil extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float initialSize;
        private final float spinSpeed;

        private FireSigil(
                ClientLevel level,
                double x,
                double y,
                double z,
                double encodedSize,
                double encodedSpin,
                double encodedLifetime,
                SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            initialSize = Mth.clamp((float) Math.abs(encodedSize), 0.75F, 16.0F);
            spinSpeed = Mth.clamp((float) encodedSpin, -0.5F, 0.5F);
            lifetime = Mth.clamp((int) Math.round(Math.abs(encodedLifetime)), 10, 240);
            hasPhysics = false;
            quadSize = initialSize;
            rCol = 1.0F;
            gCol = 0.11F;
            bCol = 0.015F;
            alpha = 0.0F;
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            setSpriteFromAge(sprites);
            float life = (float) age / lifetime;
            float fadeIn = Mth.clamp(life / 0.12F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - life) / 0.24F, 0.0F, 1.0F);
            alpha = 0.78F * Math.min(fadeIn, fadeOut);
            quadSize = initialSize * (0.96F + 0.04F * Mth.sin(age * 0.35F));
            oRoll = roll;
            roll += spinSpeed;
        }

        @Override
        public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
            Quaternionf rotation = new Quaternionf()
                    .rotationX(-Mth.HALF_PI)
                    .rotateZ(Mth.lerp(partialTicks, oRoll, roll));
            renderRotatedQuad(buffer, camera, rotation, partialTicks);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static final class EmberProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public EmberProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed) {
            return new Ember(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }

    public static final class ScaleSparkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public ScaleSparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed) {
            return new ScaleSpark(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }

    public static final class FireSigilProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FireSigilProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed) {
            return new FireSigil(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
