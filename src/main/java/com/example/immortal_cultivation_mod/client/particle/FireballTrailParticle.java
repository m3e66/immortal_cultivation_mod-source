package com.example.immortal_cultivation_mod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class FireballTrailParticle extends TextureSheetParticle {
   private final SpriteSet spriteSet;

   public FireballTrailParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed);
      this.spriteSet = spriteSet;
      this.lifetime = 10;
      this.gravity = 0.0F;
      this.hasPhysics = false;
      this.quadSize = 0.15F;
      this.rCol = 1.0F;
      this.gCol = 0.5F;
      this.bCol = 0.0F;
      this.alpha = 0.8F;
      this.setSpriteFromAge(spriteSet);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.spriteSet);
         this.alpha = 1.0F - (float)this.age / (float)this.lifetime;
         this.quadSize *= 0.95F;
         this.xd *= 0.9;
         this.yd *= 0.9;
         this.zd *= 0.9;
         this.move(this.xd, this.yd, this.zd);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteSet;

      public Provider(SpriteSet spriteSet) {
         this.spriteSet = spriteSet;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new FireballTrailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
      }
   }
}
