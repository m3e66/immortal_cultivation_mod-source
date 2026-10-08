package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.RealmPressureClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class RealmPressureParticleMixin {
    @Inject(method = "tickParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$pressureDrift(Particle particle, CallbackInfo ci) {
        // Exact class: SplashParticle inherits WaterDropParticle, but splashing water isn't rain.
        if (particle.getClass() == WaterDropParticle.class && RealmPressureClient.suppressRainAt(particle.getPos())) {
            // Tick lifetime normally, but hold the drop at its exact world position.
            Vec3 held = particle.getPos();
            particle.tick();
            particle.setPos(held.x, held.y, held.z);
            particle.setParticleSpeed(0, 0, 0);
            ci.cancel(); return;
        }
        RealmPressureClient.pushParticle(particle);
    }
}
