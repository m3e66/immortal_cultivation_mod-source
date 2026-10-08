package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import net.minecraft.client.model.PlayerModel;

/** The same seated pose as players, driven by the NPC's synchronized cultivation state. */
final class NpcCultivationPose {
    private NpcCultivationPose() {}
    static boolean apply(PlayerModel<?> model,CultivationNpc npc) {
        if (npc.asNpcMob().hasEffect(com.example.immortal_cultivation_mod.effect.ModEffects.NPC_WEAPON_GUARD)) {
            model.rightArm.xRot = -1.25F;
            model.rightArm.yRot = -.55F;
            model.rightArm.zRot = -.3F;
            model.leftArm.xRot = -1.15F;
            model.leftArm.yRot = .55F;
            model.leftArm.zRot = .3F;
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
            return true;
        }
        if(npc.npcCultivationState()==0)return false;
        MeditationPose.apply(model,model.head.yRot);
        return true;
    }
    static void reset(PlayerModel<?> model) { MeditationPose.reset(model); }
}
