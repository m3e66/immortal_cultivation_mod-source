package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.JindanFlightEffects;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<LivingEntity> {
    @Shadow private ModelPart leftSleeve;
    @Shadow private ModelPart rightSleeve;
    @Shadow private ModelPart leftPants;
    @Shadow private ModelPart rightPants;
    @Shadow private ModelPart jacket;
    private boolean immortalCultivation$poseApplied;

    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void immortalCultivation$resetCustomPose(
            LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        // Player renderers reuse their model. Clear our previous custom pose before
        // vanilla computes this player's stance, including the crouching hip offsets.
        if (immortalCultivation$poseApplied) {
            com.example.immortal_cultivation_mod.client.model.MeditationPose.reset(
                    (PlayerModel<?>)(Object)this);
            immortalCultivation$poseApplied = false;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void immortalCultivation$meditatingPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (com.example.immortal_cultivation_mod.client.flame.ChargedFlameRenderer.charging(player)) {
            boolean right = (player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND)
                    == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            ModelPart arm = right ? rightArm : leftArm;
            ModelPart other = right ? leftArm : rightArm;
            arm.xRot = -Mth.HALF_PI + head.xRot * .75F;
            arm.yRot = head.yRot + (right ? -.08F : .08F);
            arm.zRot = right ? .06F : -.06F;
            other.xRot = -.22F; other.yRot = 0; other.zRot = right ? -.12F : .12F;
            rightSleeve.copyFrom(rightArm); leftSleeve.copyFrom(leftArm);
            return;
        }

        // Player Animator applies its pose before this tail injection. Yield for ordinary
        // swings and single-weapon guards too, not only skills and crossed guards.
        if (com.example.immortal_cultivation_mod.client.SwordComboAnimations.isPlaying(player)) {
            return;
        }

        boolean forgingVisual=com.example.immortal_cultivation_mod.client.ForgingVisualState.isForging(player);
        boolean meditationVisual=ClientData.isPlayerMeditating(player.getUUID()) || forgingVisual;
        float flightPoseProgress = JindanFlightEffects.fastFlightPoseProgress(
                player,
                Mth.clamp(ageInTicks - player.tickCount, 0.0F, 1.0F));
        if (flightPoseProgress > 0.001F && !forgingVisual) {
            immortalCultivation$poseApplied = true;
            applyJindanReferenceFlightPose(flightPoseProgress);
            return;
        }

        if (ClientData.isPlayerCasting(player.getUUID()) && !meditationVisual) {
            rightArm.xRot = -2.15F;
            rightArm.yRot = -0.22F;
            rightArm.zRot = 0.18F;
            rightSleeve.copyFrom(rightArm);
            return;
        }

        if (!meditationVisual) {
            return;
        }

        immortalCultivation$poseApplied = true;
        com.example.immortal_cultivation_mod.client.model.MeditationPose.apply(
                (PlayerModel<?>)(Object)this,netHeadYaw*((float)Math.PI/180F));
    }

    private void applyJindanReferenceFlightPose(float progress) {
        crouching = false;

        blendPart(body, progress,
                0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F);

        // Match the supplied model.geo.json reference: the body stays straight,
        // while the head pitches back 62.5 degrees to face the travel direction.
        blendPart(head, progress,
                0.0F, 0.0F, 0.0F,
                (float)Math.toRadians(-62.5D), 0.0F, 0.0F);
        hat.copyFrom(head);

        blendPart(rightArm, progress,
                -5.0F, 2.0F, 0.0F,
                0.0F, 0.0F, 0.0F);
        blendPart(leftArm, progress,
                5.0F, 2.0F, 0.0F,
                0.0F, 0.0F, 0.0F);
        blendPart(rightLeg, progress,
                -1.9F, 12.0F, 0.0F,
                0.0F, 0.0F, 0.0F);
        blendPart(leftLeg, progress,
                1.9F, 12.0F, 0.0F,
                0.0F, 0.0F, 0.0F);

        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    private static void blendPart(
            ModelPart part,
            float progress,
            float targetX,
            float targetY,
            float targetZ,
            float targetXRot,
            float targetYRot,
            float targetZRot) {
        part.x = Mth.lerp(progress, part.x, targetX);
        part.y = Mth.lerp(progress, part.y, targetY);
        part.z = Mth.lerp(progress, part.z, targetZ);
        part.xRot = Mth.lerp(progress, part.xRot, targetXRot);
        part.yRot = Mth.lerp(progress, part.yRot, targetYRot);
        part.zRot = Mth.lerp(progress, part.zRot, targetZRot);
    }

}
