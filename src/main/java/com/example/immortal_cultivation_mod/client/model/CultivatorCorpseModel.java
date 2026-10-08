package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public class CultivatorCorpseModel extends PlayerModel<CultivatorCorpseEntity> {
   public CultivatorCorpseModel(ModelPart root) {
      super(root, true);
   }

   public void setupAnim(CultivatorCorpseEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      if (entity.isRaised() && !entity.isSealed()) {
         super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
         AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, entity.isAggressive(), this.attackTime, ageInTicks);
         this.hat.copyFrom(this.head);
         this.jacket.copyFrom(this.body);
         this.leftSleeve.copyFrom(this.leftArm);
         this.rightSleeve.copyFrom(this.rightArm);
         this.leftPants.copyFrom(this.leftLeg);
         this.rightPants.copyFrom(this.rightLeg);
      } else {
         super.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
         this.head.xRot = 0.0F;
         this.head.yRot = 0.0F;
         this.body.xRot = 0.0F;
         this.body.yRot = 0.0F;
         this.leftArm.xRot = entity.isRaised() ? -1.55F : 0.0F;
         this.leftArm.yRot = 0.0F;
         this.rightArm.xRot = entity.isRaised() ? -1.55F : 0.0F;
         this.rightArm.yRot = 0.0F;
         this.leftLeg.xRot = 0.0F;
         this.leftLeg.yRot = 0.0F;
         this.rightLeg.xRot = 0.0F;
         this.rightLeg.yRot = 0.0F;
         this.hat.copyFrom(this.head);
         this.jacket.copyFrom(this.body);
         this.leftSleeve.copyFrom(this.leftArm);
         this.rightSleeve.copyFrom(this.rightArm);
         this.leftPants.copyFrom(this.leftLeg);
         this.rightPants.copyFrom(this.rightLeg);
      }
   }
}
