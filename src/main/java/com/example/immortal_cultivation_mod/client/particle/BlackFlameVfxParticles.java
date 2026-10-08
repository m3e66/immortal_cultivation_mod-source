package com.example.immortal_cultivation_mod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class BlackFlameVfxParticles {
   private BlackFlameVfxParticles() {
   }

   private static enum Kind {
      CORE,
      EDGE,
      ASH,
      SPARK;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{CORE, EDGE, ASH, SPARK};
      }
   }

   private static final class BlackFlameParticle extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final Kind kind;
      private final float initialSize;
      private final float spinSpeed;

      private BlackFlameParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, Kind kind) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.kind = kind;
         this.hasPhysics = false;
         this.friction = kind == BlackFlameVfxParticles.Kind.SPARK ? 0.92F : 0.965F;
         this.gravity = kind == BlackFlameVfxParticles.Kind.ASH ? 0.018F : -0.012F;
         this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
         this.oRoll = this.roll;
         float var10001 = this.random.nextBoolean() ? 1.0F : -1.0F;
         float var10002;
         switch (kind.ordinal()) {
            case 0 -> var10002 = 0.045F + this.random.nextFloat() * 0.07F;
            case 1 -> var10002 = 0.08F + this.random.nextFloat() * 0.11F;
            case 2 -> var10002 = 0.025F + this.random.nextFloat() * 0.045F;
            case 3 -> var10002 = 0.28F + this.random.nextFloat() * 0.36F;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         this.spinSpeed = var10001 * var10002;
         switch (kind.ordinal()) {
            case 0 -> var10001 = 0.52F + this.random.nextFloat() * 0.42F;
            case 1 -> var10001 = 0.46F + this.random.nextFloat() * 0.44F;
            case 2 -> var10001 = 0.1F + this.random.nextFloat() * 0.16F;
            case 3 -> var10001 = 0.1F + this.random.nextFloat() * 0.13F;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         this.initialSize = var10001;
         int var18;
         switch (kind.ordinal()) {
            case 0 -> var18 = 20 + this.random.nextInt(13);
            case 1 -> var18 = 18 + this.random.nextInt(12);
            case 2 -> var18 = 25 + this.random.nextInt(18);
            case 3 -> var18 = 8 + this.random.nextInt(7);
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         this.lifetime = var18;
         this.quadSize = this.initialSize;
         this.alpha = kind == BlackFlameVfxParticles.Kind.ASH ? 0.74F : 0.98F;
         switch (kind.ordinal()) {
            case 0:
               this.rCol = 0.12F;
               this.gCol = 0.012F;
               this.bCol = 0.2F;
               break;
            case 1:
               this.rCol = 0.48F + this.random.nextFloat() * 0.16F;
               this.gCol = 0.018F;
               this.bCol = 0.82F + this.random.nextFloat() * 0.18F;
               break;
            case 2:
               float gray = 0.13F + this.random.nextFloat() * 0.16F;
               this.rCol = gray;
               this.gCol = gray * 0.92F;
               this.bCol = gray * 1.1F;
               break;
            case 3:
               this.rCol = 0.72F;
               this.gCol = 0.06F;
               this.bCol = 1.0F;
         }

         this.xd = xSpeed;
         this.yd = ySpeed + (kind != BlackFlameVfxParticles.Kind.CORE && kind != BlackFlameVfxParticles.Kind.EDGE ? (double)0.0F : 0.018 + this.random.nextDouble() * 0.018);
         this.zd = zSpeed;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            float life = (float)this.age / (float)this.lifetime;
            float fadeIn = Mth.clamp(life / 0.1F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - life) / 0.34F, 0.0F, 1.0F);
            this.alpha = (this.kind == BlackFlameVfxParticles.Kind.ASH ? 0.74F : 0.98F) * Math.min(fadeIn, fadeOut);
            float var10001;
            switch (this.kind.ordinal()) {
               case 0 -> var10001 = this.initialSize * (0.72F + Mth.sin(life * (float)Math.PI) * 0.72F);
               case 1 -> var10001 = this.initialSize * (0.62F + Mth.sin(life * (float)Math.PI) * 0.92F);
               case 2 -> var10001 = this.initialSize * (0.82F + life * 0.45F);
               case 3 -> var10001 = this.initialSize * (1.35F - life);
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            this.quadSize = var10001;
            this.oRoll = this.roll;
            this.roll += this.spinSpeed;
            if (this.kind == BlackFlameVfxParticles.Kind.CORE || this.kind == BlackFlameVfxParticles.Kind.EDGE) {
               double previousX = this.xd;
               double curl = this.kind == BlackFlameVfxParticles.Kind.EDGE ? 0.075 : 0.052;
               this.xd = previousX * 0.975 - this.zd * curl;
               this.zd = this.zd * 0.975 + previousX * curl;
               this.yd += 0.0018;
            }

         }
      }

      protected int getLightColor(float partialTick) {
         return this.kind == BlackFlameVfxParticles.Kind.ASH ? super.getLightColor(partialTick) : 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   private abstract static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;
      private final Kind kind;

      private Provider(SpriteSet sprites, Kind kind) {
         this.sprites = sprites;
         this.kind = kind;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new BlackFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.kind);
      }
   }

   public static final class CoreProvider extends Provider {
      public CoreProvider(SpriteSet sprites) {
         super(sprites, BlackFlameVfxParticles.Kind.CORE);
      }
   }

   public static final class EdgeProvider extends Provider {
      public EdgeProvider(SpriteSet sprites) {
         super(sprites, BlackFlameVfxParticles.Kind.EDGE);
      }
   }

   public static final class AshProvider extends Provider {
      public AshProvider(SpriteSet sprites) {
         super(sprites, BlackFlameVfxParticles.Kind.ASH);
      }
   }

   public static final class SparkProvider extends Provider {
      public SparkProvider(SpriteSet sprites) {
         super(sprites, BlackFlameVfxParticles.Kind.SPARK);
      }
   }
}
