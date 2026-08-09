package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.ShihunfanGhostEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class ShihunfanGhostModel extends PlayerModel<ShihunfanGhostEntity> {
    public ShihunfanGhostModel(ModelPart root) {
        super(root, true);
    }

    @Override
    public void setupAnim(ShihunfanGhostEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
        leftArm.xRot = (float) Math.PI;
        rightArm.xRot = (float) Math.PI;
        leftArm.yRot = 0.0F;
        rightArm.yRot = 0.0F;
        leftArm.zRot = -0.12F;
        rightArm.zRot = 0.12F;
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
