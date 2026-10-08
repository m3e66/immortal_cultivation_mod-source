package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.ShihunfanGhostEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class ShihunfanGhostModel extends PlayerModel<ShihunfanGhostEntity> {
   public ShihunfanGhostModel(ModelPart root) {
      super(root, true);
   }

   public void setupAnim(ShihunfanGhostEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
      this.leftArm.xRot = (float)Math.PI;
      this.rightArm.xRot = (float)Math.PI;
      this.leftArm.yRot = 0.0F;
      this.rightArm.yRot = 0.0F;
      this.leftArm.zRot = -0.12F;
      this.rightArm.zRot = 0.12F;
      this.leftSleeve.copyFrom(this.leftArm);
      this.rightSleeve.copyFrom(this.rightArm);
   }
}
