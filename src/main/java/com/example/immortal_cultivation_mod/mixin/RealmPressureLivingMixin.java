package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.RealmPressureClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class RealmPressureLivingMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void immortalCultivation$oppressiveTremble(LivingEntity entity, PoseStack pose,
            float age, float yaw, float partial, float scale, CallbackInfo ci) {
        float strength = RealmPressureClient.suppression(entity);
        if (strength <= 0 || entity.isSleeping() || entity.isPassenger()) return;
        double time = entity.tickCount + partial;
        pose.translate(Math.sin(time * 2.6 + entity.getId()) * .012 * strength, 0,
                Math.cos(time * 1.9 + entity.getId()) * .009 * strength);
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 2.1) * .75F * strength));
        if (entity.onGround()) {
            pose.translate(0, -.035 * strength, 0);
            pose.scale(1, 1 - .08F * strength, 1);
        }
    }
}
