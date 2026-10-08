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

public final class DragonVfxParticles {
   private DragonVfxParticles() {
   }

   private static final class Ember extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float initialSize;
      private final float spinSpeed;

      private Ember(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.lifetime = 16 + this.random.nextInt(10);
         this.hasPhysics = false;
         this.gravity = -0.015F;
         this.friction = 0.94F;
         this.initialSize = 0.24F + this.random.nextFloat() * 0.24F;
         this.quadSize = this.initialSize;
         this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.08F + this.random.nextFloat() * 0.13F);
         this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
         this.oRoll = this.roll;
         this.rCol = 1.0F;
         this.gCol = 0.12F + this.random.nextFloat() * 0.24F;
         this.bCol = 0.015F;
         this.alpha = 0.96F;
         this.xd = xSpeed;
         this.yd = ySpeed + 0.008;
         this.zd = zSpeed;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            float life = (float)this.age / (float)this.lifetime;
            this.alpha = 0.96F * Mth.clamp((1.0F - life) * 1.35F, 0.0F, 1.0F);
            this.quadSize = this.initialSize * (0.72F + Mth.sin(life * (float)Math.PI) * 0.72F);
            this.oRoll = this.roll;
            this.roll += this.spinSpeed;
            double oldX = this.xd;
            this.xd = oldX * 0.985 - this.zd * 0.045;
            this.zd = this.zd * 0.985 + oldX * 0.045;
            this.yd += 0.0025;
         }
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   private static final class ScaleSpark extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float initialSize;
      private final float spinSpeed;

      private ScaleSpark(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.lifetime = 9 + this.random.nextInt(8);
         this.hasPhysics = false;
         this.gravity = 0.18F;
         this.friction = 0.91F;
         this.initialSize = 0.13F + this.random.nextFloat() * 0.13F;
         this.quadSize = this.initialSize;
         this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.28F + this.random.nextFloat() * 0.32F);
         this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
         this.oRoll = this.roll;
         this.rCol = 1.0F;
         this.gCol = 0.58F + this.random.nextFloat() * 0.3F;
         this.bCol = 0.05F;
         this.alpha = 1.0F;
         this.xd = xSpeed;
         this.yd = ySpeed;
         this.zd = zSpeed;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            float remaining = 1.0F - (float)this.age / (float)this.lifetime;
            this.alpha = Mth.clamp(remaining * 1.45F, 0.0F, 1.0F);
            this.quadSize = this.initialSize * (0.25F + remaining * 1.35F);
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

   private static final class FireSigil extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float initialSize;
      private final float spinSpeed;

      private FireSigil(ClientLevel level, double x, double y, double z, double encodedSize, double encodedSpin, double encodedLifetime, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.initialSize = Mth.clamp((float)Math.abs(encodedSize), 0.75F, 16.0F);
         this.spinSpeed = Mth.clamp((float)encodedSpin, -0.5F, 0.5F);
         this.lifetime = Mth.clamp((int)Math.round(Math.abs(encodedLifetime)), 10, 240);
         this.hasPhysics = false;
         this.quadSize = this.initialSize;
         this.rCol = 1.0F;
         this.gCol = 0.11F;
         this.bCol = 0.015F;
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
            float fadeIn = Mth.clamp(life / 0.12F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - life) / 0.24F, 0.0F, 1.0F);
            this.alpha = 0.78F * Math.min(fadeIn, fadeOut);
            this.quadSize = this.initialSize * (0.96F + 0.04F * Mth.sin((float)this.age * 0.35F));
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

   public static final class EmberProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public EmberProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new Ember(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }

   public static final class ScaleSparkProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public ScaleSparkProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new ScaleSpark(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }

   public static final class FireSigilProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public FireSigilProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new FireSigil(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }
}
