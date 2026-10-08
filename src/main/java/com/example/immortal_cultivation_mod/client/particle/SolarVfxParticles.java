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

public final class SolarVfxParticles {
   private SolarVfxParticles() {
   }

   private static final class SolarEmber extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float initialSize;
      private final float spinSpeed;

      private SolarEmber(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.lifetime = 18 + this.random.nextInt(13);
         this.hasPhysics = false;
         this.gravity = -0.012F;
         this.friction = 0.94F;
         this.initialSize = 0.18F + this.random.nextFloat() * 0.24F;
         this.quadSize = this.initialSize;
         this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.08F + this.random.nextFloat() * 0.16F);
         this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
         this.oRoll = this.roll;
         this.rCol = 1.0F;
         this.gCol = 0.62F + this.random.nextFloat() * 0.3F;
         this.bCol = 0.04F + this.random.nextFloat() * 0.18F;
         this.alpha = 0.98F;
         this.xd = xSpeed;
         this.yd = ySpeed;
         this.zd = zSpeed;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            float life = (float)this.age / (float)this.lifetime;
            float fadeIn = Mth.clamp(life / 0.1F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - life) / 0.42F, 0.0F, 1.0F);
            this.alpha = 0.98F * Math.min(fadeIn, fadeOut);
            this.quadSize = this.initialSize * (0.72F + Mth.sin(life * (float)Math.PI) * 1.15F);
            this.oRoll = this.roll;
            this.roll += this.spinSpeed;
            double previousX = this.xd;
            this.xd = previousX * 0.982 - this.zd * 0.052;
            this.zd = this.zd * 0.982 + previousX * 0.052;
            this.yd += 0.0018;
         }
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   private static final class SolarCore extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float baseSize;
      private final float spinSpeed;

      private SolarCore(ClientLevel level, double x, double y, double z, double encodedSize, double encodedSpin, double encodedLifetime, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.baseSize = Mth.clamp((float)Math.abs(encodedSize), 0.18F, 4.0F);
         this.spinSpeed = Mth.clamp((float)encodedSpin, -0.5F, 0.5F);
         this.lifetime = Mth.clamp((int)Math.round(Math.abs(encodedLifetime)), 3, 24);
         this.hasPhysics = false;
         this.quadSize = this.baseSize;
         this.rCol = 1.0F;
         this.gCol = 0.96F;
         this.bCol = 0.68F;
         this.alpha = 0.98F;
         this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
         this.oRoll = this.roll;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         this.xo = this.x;
         this.yo = this.y;
         this.zo = this.z;
         if (this.age++ >= this.lifetime) {
            this.remove();
         } else {
            this.setSpriteFromAge(this.sprites);
            float life = (float)this.age / (float)this.lifetime;
            this.alpha = 0.98F * Mth.clamp((1.0F - life) / 0.38F, 0.0F, 1.0F);
            this.quadSize = this.baseSize * (0.94F + 0.1F * Mth.sin((float)this.age * 0.72F));
            this.oRoll = this.roll;
            this.roll += this.spinSpeed;
         }
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   private static final class SolarSigil extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float targetSize;
      private final float spinSpeed;
      private final boolean expanding;

      private SolarSigil(ClientLevel level, double x, double y, double z, double encodedSize, double encodedSpin, double encodedLifetime, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.expanding = encodedSize < (double)0.0F;
         this.targetSize = Mth.clamp((float)Math.abs(encodedSize), 0.45F, 24.0F);
         this.spinSpeed = Mth.clamp((float)encodedSpin, -0.5F, 0.5F);
         this.lifetime = Mth.clamp((int)Math.round(Math.abs(encodedLifetime)), 6, 240);
         this.hasPhysics = false;
         this.quadSize = this.expanding ? this.targetSize * 0.08F : this.targetSize;
         this.rCol = 1.0F;
         this.gCol = this.expanding ? 0.9F : 0.72F;
         this.bCol = this.expanding ? 0.38F : 0.08F;
         this.alpha = 0.0F;
         this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
         this.oRoll = this.roll;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         this.xo = this.x;
         this.yo = this.y;
         this.zo = this.z;
         if (this.age++ >= this.lifetime) {
            this.remove();
         } else {
            this.setSpriteFromAge(this.sprites);
            float life = (float)this.age / (float)this.lifetime;
            float fadeIn = Mth.clamp(life / 0.08F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - life) / (this.expanding ? 0.72F : 0.3F), 0.0F, 1.0F);
            this.alpha = (this.expanding ? 0.88F : 0.82F) * Math.min(fadeIn, fadeOut);
            if (this.expanding) {
               float eased = 1.0F - (float)Math.pow((double)(1.0F - life), (double)3.0F);
               this.quadSize = this.targetSize * (0.08F + eased * 0.92F);
            } else {
               this.quadSize = this.targetSize * (0.965F + 0.035F * Mth.sin((float)this.age * 0.32F));
            }

            this.oRoll = this.roll;
            this.roll += this.spinSpeed;
         }
      }

      public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
         Quaternionf rotation = (new Quaternionf()).rotationX((-(float)Math.PI / 2F)).rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
         this.renderRotatedQuad(buffer, camera, rotation, partialTicks);
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   public static final class SolarEmberProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SolarEmberProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SolarEmber(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }

   public static final class SolarSigilProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SolarSigilProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SolarSigil(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }

   public static final class SolarCoreProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SolarCoreProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SolarCore(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }
}
