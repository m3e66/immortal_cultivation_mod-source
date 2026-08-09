package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.LiusanEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class LiusanModel extends PlayerModel<LiusanEntity> {
    public LiusanModel(ModelPart root) {
        super(root, true);
    }

    @Override
    public void setupAnim(LiusanEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.getCastingTicks() > 0) {
            rightArm.xRot = -2.15F;
            rightArm.yRot = -0.22F;
            rightArm.zRot = 0.18F;
            rightSleeve.copyFrom(rightArm);
        }
    }
}
