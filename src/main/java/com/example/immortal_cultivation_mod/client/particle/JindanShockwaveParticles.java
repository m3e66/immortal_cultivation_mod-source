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
import org.joml.Vector3f;

public final class JindanShockwaveParticles {
   private static final float MIN_INTENSITY = 0.75F;
   private static final float MAX_INTENSITY = 2.25F;

   private JindanShockwaveParticles() {
   }

   private static Vector3f normal(double x, double y, double z) {
      Vector3f normal = new Vector3f((float)x, (float)y, (float)z);
      if (normal.lengthSquared() < 1.0E-5F) {
         normal.set(0.0F, 0.0F, 1.0F);
      }

      return normal;
   }

   private static float magnitude(double x, double y, double z) {
      return (float)Math.sqrt(x * x + y * y + z * z);
   }

   private abstract static class ShockwaveParticle extends TextureSheetParticle {
      protected final SpriteSet sprites;
      protected final Quaternionf orientation;
      protected final float intensity;

      protected ShockwaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, Vector3f normal, float intensity, int lifetime) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.intensity = Mth.clamp(intensity, 0.75F, 2.25F);
         this.orientation = (new Quaternionf()).rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), normal.normalize());
         this.lifetime = lifetime;
         this.hasPhysics = false;
         this.gravity = 0.0F;
         this.xd = (double)0.0F;
         this.yd = (double)0.0F;
         this.zd = (double)0.0F;
         this.rCol = 1.0F;
         this.gCol = 1.0F;
         this.bCol = 1.0F;
         this.alpha = 1.0F;
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
         }
      }

      protected float progress(float partialTicks) {
         return Mth.clamp(((float)this.age + partialTicks) / (float)this.lifetime, 0.0F, 1.0F);
      }

      protected static float easeOutCubic(float value) {
         float inverse = 1.0F - value;
         return 1.0F - inverse * inverse * inverse;
      }

      protected void renderDoubleSided(VertexConsumer buffer, Camera camera, float partialTicks) {
         this.renderRotatedQuad(buffer, camera, this.orientation, partialTicks);
         this.renderRotatedQuad(buffer, camera, (new Quaternionf(this.orientation)).rotateY((float)Math.PI), partialTicks);
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   private static final class SonicRingParticle extends ShockwaveParticle {
      private final float startSize;
      private final float endSize;

      private SonicRingParticle(ClientLevel level, double x, double y, double z, double encodedX, double encodedY, double encodedZ, SpriteSet sprites) {
         super(level, x, y, z, sprites, JindanShockwaveParticles.normal(encodedX, encodedY, encodedZ), JindanShockwaveParticles.magnitude(encodedX, encodedY, encodedZ), 14);
         this.startSize = 0.42F + this.intensity * 0.16F;
         this.endSize = 2.75F + this.intensity * 1.15F;
         this.quadSize = this.startSize;
      }

      public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
         float life = this.progress(partialTicks);
         this.quadSize = Mth.lerp(easeOutCubic(life), this.startSize, this.endSize);
         this.alpha = 0.92F * Mth.clamp((1.0F - life) / 0.58F, 0.0F, 1.0F);
         this.renderDoubleSided(buffer, camera, partialTicks);
      }
   }

   private static final class SmashRingParticle extends ShockwaveParticle {
      private final float maximumSize;

      private SmashRingParticle(ClientLevel level, double x, double y, double z, double encodedIntensity, SpriteSet sprites) {
         super(level, x, y, z, sprites, new Vector3f(0.0F, 1.0F, 0.0F), (float)Math.abs(encodedIntensity), 22);
         this.maximumSize = 5.5F + this.intensity * 2.65F;
         this.quadSize = 0.65F;
      }

      public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
         float life = this.progress(partialTicks);
         this.quadSize = Mth.lerp(easeOutCubic(life), 0.55F, this.maximumSize);
         this.alpha = 0.92F * Mth.clamp((1.0F - life) / 0.68F, 0.0F, 1.0F);
         this.renderDoubleSided(buffer, camera, partialTicks);
      }
   }

   public static final class SonicRingProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SonicRingProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SonicRingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }

   public static final class SmashRingProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SmashRingProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SmashRingParticle(level, x, y, z, xSpeed, this.sprites);
      }
   }
}
