package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ClientData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
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

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void immortalCultivation$meditatingPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player) || !ClientData.isPlayerMeditating(player.getUUID())) {
            if (immortalCultivation$poseApplied) {
                resetDefaultPlayerPose();
                immortalCultivation$poseApplied = false;
            }
            return;
        }

        immortalCultivation$poseApplied = true;
        crouching = false;
        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);

        body.x = 0.0F;
        body.y = 8.5F;
        body.z = 0.8F;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        body.zRot = 0.0F;

        head.x = 0.0F;
        head.y = 8.5F;
        head.z = -0.4F;
        head.xRot = -0.16F;
        head.yRot = yaw;
        head.zRot = 0.0F;
        hat.copyFrom(head);

        rightArm.x = -4.8F;
        rightArm.y = 10.5F;
        rightArm.z = -0.7F;
        rightArm.xRot = -0.28F;
        rightArm.yRot = -0.18F;
        rightArm.zRot = -0.82F;

        leftArm.x = 4.8F;
        leftArm.y = 10.5F;
        leftArm.z = -0.7F;
        leftArm.xRot = -0.28F;
        leftArm.yRot = 0.18F;
        leftArm.zRot = 0.82F;

        rightLeg.x = -5.45F;
        rightLeg.y = 21.3F;
        rightLeg.z = 2.45F;
        rightLeg.xRot = -2.0F;
        rightLeg.yRot = 0.0F;
        rightLeg.zRot = 1.42F;

        leftLeg.x = 5.45F;
        leftLeg.y = 21.3F;
        leftLeg.z = 2.45F;
        leftLeg.xRot = -2.0F;
        leftLeg.yRot = 0.0F;
        leftLeg.zRot = -1.42F;

        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    private void resetDefaultPlayerPose() {
        head.x = 0.0F;
        head.y = 0.0F;
        head.z = 0.0F;
        hat.copyFrom(head);

        body.x = 0.0F;
        body.y = 0.0F;
        body.z = 0.0F;
        jacket.copyFrom(body);

        rightArm.x = -5.0F;
        rightArm.y = 2.0F;
        rightArm.z = 0.0F;
        leftArm.x = 5.0F;
        leftArm.y = 2.0F;
        leftArm.z = 0.0F;
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);

        rightLeg.x = -1.9F;
        rightLeg.y = 12.0F;
        rightLeg.z = 0.0F;
        leftLeg.x = 1.9F;
        leftLeg.y = 12.0F;
        leftLeg.z = 0.0F;
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }
}
