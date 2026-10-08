package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.TemporalSlowflowClient;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    private static final int MOD_PARTICLES_PER_LAYER = 65_536;
    private static final double FIRST_PERSON_SHIELD_RADIUS_SQR = 64.0D;
    private static final double FIRST_PERSON_SOLAR_RADIUS_SQR = 36.0D;
    private static int immortalCultivation$firstPersonDensitySequence;

    @ModifyConstant(method = "lambda$tick$11", constant = @Constant(intValue = 16_384))
    private static int immortalCultivation$increaseParticleCap(int original) {
        return MOD_PARTICLES_PER_LAYER;
    }

    @Inject(
            method = "tickParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void immortalCultivation$dragParticlesThroughSlowflow(
            Particle particle, CallbackInfo ci) {
        if (TemporalSlowflowClient.shouldDelayParticle(particle)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true)
    private void immortalCultivation$reduceOwnerFirstPersonParticles(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            CallbackInfoReturnable<Particle> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double distanceSqr = camera.distanceToSqr(x, y, z);
        ParticleType<?> type = options.getType();
        boolean personalShieldParticle = ClientData.shieldAmount() > 0.0F
                && distanceSqr <= FIRST_PERSON_SHIELD_RADIUS_SQR
                && immortalCultivation$isShieldParticle(type);
        boolean highSolarParticle = minecraft.player.hasEffect(ModEffects.SOLAR_BODY)
                && distanceSqr <= FIRST_PERSON_SOLAR_RADIUS_SQR
                && immortalCultivation$isSolarParticle(type);
        if (!personalShieldParticle && !highSolarParticle) {
            return;
        }

        // Preserve exactly three particles from every ten candidates. Cycling by seven
        // avoids repeatedly keeping adjacent points in circular particle formations.
        int sample = Math.floorMod(
                immortalCultivation$firstPersonDensitySequence++ * 7, 10);
        if (sample >= 3) {
            cir.setReturnValue(null);
        }
    }

    private static boolean immortalCultivation$isSolarParticle(ParticleType<?> type) {
        return type == ModParticles.METHOD_QI_LIGHT.get()
                || type == ModParticles.METHOD_QI_FIRE.get()
                || type == ModParticles.SOLAR_EMBER.get()
                || type == ModParticles.SOLAR_CORE.get()
                || type == ParticleTypes.DUST
                || type == ParticleTypes.FLAME
                || type == ParticleTypes.FLASH
                || type == ParticleTypes.ELECTRIC_SPARK
                || type == ParticleTypes.GUST
                || type == ParticleTypes.END_ROD
                || type == ParticleTypes.LARGE_SMOKE
                || type == ParticleTypes.BLOCK
                || type == ParticleTypes.POOF;
    }

    private static boolean immortalCultivation$isShieldParticle(ParticleType<?> type) {
        return type == ModParticles.METHOD_QI_EARTH.get()
                || type == ModParticles.METHOD_QI_WIND.get()
                || type == ModParticles.METHOD_QI_WATER.get()
                || type == ModParticles.METHOD_QI_BLOOD.get()
                || type == ModParticles.METHOD_QI_LIGHT.get()
                || type == ModParticles.METHOD_QI_NEUTRAL.get()
                || type == ParticleTypes.DUST
                || type == ParticleTypes.BLOCK
                || type == ParticleTypes.FALLING_DUST
                || type == ParticleTypes.GUST
                || type == ParticleTypes.GUST_EMITTER_LARGE
                || type == ParticleTypes.POOF
                || type == ParticleTypes.END_ROD
                || type == ParticleTypes.FLASH
                || type == ParticleTypes.DAMAGE_INDICATOR
                || type == ParticleTypes.CLOUD
                || type == ParticleTypes.SWEEP_ATTACK
                || type == ParticleTypes.CAMPFIRE_COSY_SMOKE
                || type == ParticleTypes.ENCHANT;
    }
}
