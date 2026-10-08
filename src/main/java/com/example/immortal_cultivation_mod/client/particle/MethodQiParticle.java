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

   @Override
   public void render(com.mojang.blaze3d.vertex.VertexConsumer vertices,
                      net.minecraft.client.Camera camera, float partialTick) {
      float size = this.quadSize;
      float opacity = this.alpha;
      // Keep each particle's original sprite, path and lifetime; add only a faint outer glow.
      this.quadSize = size * 2.4F;
      this.alpha = opacity * 0.12F;
      super.render(vertices, camera, partialTick);
      this.quadSize = size;
      this.alpha = opacity;
      super.render(vertices, camera, partialTick);
   }

   protected MethodQiParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, int color) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed);
      this.sprites = sprites;
      this.lifetime = 14 + this.random.nextInt(9);
      this.gravity = -0.018F;
      this.hasPhysics = false;
      this.initialSize = 0.1F + this.random.nextFloat() * 0.09F;
      this.quadSize = this.initialSize;
      this.alpha = 0.92F;
      this.rCol = (float)(color >> 16 & 255) / 255.0F;
      this.gCol = (float)(color >> 8 & 255) / 255.0F;
      this.bCol = (float)(color & 255) / 255.0F;
      this.xd = xSpeed + (this.random.nextDouble() - (double)0.5F) * 0.025;
      this.yd = Math.abs(ySpeed) * 0.35 + 0.025 + this.random.nextDouble() * 0.025;
      this.zd = zSpeed + (this.random.nextDouble() - (double)0.5F) * 0.025;
      this.setSpriteFromAge(sprites);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         float life = (float)this.age / (float)this.lifetime;
         this.alpha = 0.92F * (1.0F - life);
         this.quadSize = this.initialSize * (0.85F + life * 0.45F);
         this.xd *= 0.96;
         this.yd *= 0.985;
         this.zd *= 0.96;
      }
   }

   protected int getLightColor(float partialTick) {
      return 15728880;
   }

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

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new MethodQiParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.color);
      }
   }
}
