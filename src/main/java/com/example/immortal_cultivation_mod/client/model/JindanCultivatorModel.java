package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.JindanCultivatorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class JindanCultivatorModel extends PlayerModel<JindanCultivatorEntity> {
   public JindanCultivatorModel(ModelPart root) {
      super(root, true);
   }

   public void setupAnim(JindanCultivatorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      NpcCultivationPose.reset(this);
      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (NpcCultivationPose.apply(this, entity)) return;
      if (entity.getCastingTicks() > 0) {
         this.rightArm.xRot = -2.15F;
         this.rightArm.yRot = -0.22F;
         this.rightArm.zRot = 0.18F;
         this.rightSleeve.copyFrom(this.rightArm);
      }

   }
}
