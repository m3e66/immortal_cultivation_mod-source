package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.RealmPressureClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class RealmPressureHumanoidMixin {
    @Shadow public ModelPart head;
    @Shadow public ModelPart body;
    @Shadow public ModelPart leftLeg;
    @Shadow public ModelPart rightLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void immortalCultivation$bendUnderPressure(LivingEntity entity, float limb, float amount,
            float age, float yaw, float pitch, CallbackInfo ci) {
        float strength = RealmPressureClient.suppression(entity);
        if (strength <= .4F || !entity.onGround() || entity.isPassenger() || entity.isSleeping()) return;
        // Vanilla resets these rotations each frame, so the next rendered entity inherits no pose.
        body.xRot += strength * .32F;
        head.xRot += strength * .24F;
        leftLeg.xRot -= strength * .72F;
        rightLeg.xRot -= strength * .72F;
    }
}
