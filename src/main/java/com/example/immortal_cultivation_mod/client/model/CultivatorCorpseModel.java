package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class CultivatorCorpseModel extends PlayerModel<CultivatorCorpseEntity> {
    public CultivatorCorpseModel(ModelPart root) {
        super(root, true);
    }

    @Override
    public void setupAnim(CultivatorCorpseEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isRaised() && !entity.isSealed()) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            AnimationUtils.animateZombieArms(leftArm, rightArm, entity.isAggressive(), attackTime, ageInTicks);
            hat.copyFrom(head);
            jacket.copyFrom(body);
            leftSleeve.copyFrom(leftArm);
            rightSleeve.copyFrom(rightArm);
            leftPants.copyFrom(leftLeg);
            rightPants.copyFrom(rightLeg);
            return;
        }
        super.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        head.xRot = 0.0F;
        head.yRot = 0.0F;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        leftArm.xRot = entity.isRaised() ? -1.55F : 0.0F;
        leftArm.yRot = 0.0F;
        rightArm.xRot = entity.isRaised() ? -1.55F : 0.0F;
        rightArm.yRot = 0.0F;
        leftLeg.xRot = 0.0F;
        leftLeg.yRot = 0.0F;
        rightLeg.xRot = 0.0F;
        rightLeg.yRot = 0.0F;
        hat.copyFrom(head);
        jacket.copyFrom(body);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
    }
}
