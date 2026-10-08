package com.example.immortal_cultivation_mod.client.model;

import net.minecraft.client.model.PlayerModel;

/** The player's seated cultivation pose, shared with every cultivator NPC. */
public final class MeditationPose {
    public static void apply(PlayerModel<?> model,float yaw) {
        model.crouching = false;

        model.body.x = 0.0F;
        model.body.y = 8.5F;
        model.body.z = 0.8F;
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;

        model.head.x = 0.0F;
        model.head.y = 8.5F;
        model.head.z = -0.4F;
        model.head.xRot = -0.16F;
        model.head.yRot = yaw;
        model.head.zRot = 0.0F;
        model.hat.copyFrom(model.head);

        model.rightArm.x = -4.8F;
        model.rightArm.y = 10.5F;
        model.rightArm.z = -0.7F;
        model.rightArm.xRot = -0.28F;
        model.rightArm.yRot = -0.18F;
        model.rightArm.zRot = -0.82F;

        model.leftArm.x = 4.8F;
        model.leftArm.y = 10.5F;
        model.leftArm.z = -0.7F;
        model.leftArm.xRot = -0.28F;
        model.leftArm.yRot = 0.18F;
        model.leftArm.zRot = 0.82F;

        model.rightLeg.x = -5.45F;
        model.rightLeg.y = 21.3F;
        model.rightLeg.z = 2.45F;
        model.rightLeg.xRot = -2.0F;
        model.rightLeg.yRot = 0.0F;
        model.rightLeg.zRot = 1.42F;

        model.leftLeg.x = 5.45F;
        model.leftLeg.y = 21.3F;
        model.leftLeg.z = 2.45F;
        model.leftLeg.xRot = -2.0F;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = -1.42F;

        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }
    public static void reset(PlayerModel<?> model) {
        for(var part:java.util.List.of(model.head,model.hat,model.body,model.rightArm,model.leftArm,
                model.rightLeg,model.leftLeg,model.jacket,model.rightSleeve,model.leftSleeve,model.rightPants,model.leftPants))
            part.loadPose(part.getInitialPose());
    }
    private MeditationPose(){}
}
