package com.example.immortal_cultivation_mod.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

final class NpcHurtByTargetGoal extends HurtByTargetGoal {
    private final PathfinderMob npc;

    NpcHurtByTargetGoal(PathfinderMob npc) {
        super(npc);
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        LivingEntity attacker = npc.getLastHurtByMob();
        return !(attacker instanceof ServerPlayer player) || NpcProfile.shouldAttackPlayer(npc, player);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = npc.getTarget();
        if (target instanceof ServerPlayer player && !NpcProfile.shouldAttackPlayer(npc, player)) {
            npc.setTarget(null);
            return false;
        }
        return super.canContinueToUse();
    }
}
