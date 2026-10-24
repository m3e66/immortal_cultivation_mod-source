package com.example.immortal_cultivation_mod.entity;

import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/** Wild adults hunt vulnerable players; pets, pups, creative and spectator players are excluded. */
final class WolfPackHuntGoal extends NearestAttackableTargetGoal<Player> {
    private final Wolf wolf;
    WolfPackHuntGoal(Wolf wolf) {
        super(wolf, Player.class, 10, true, false, target -> WolfPackBehavior.validTarget(wolf, target));
        this.wolf = wolf;
    }
    @Override public boolean canUse() {
        return WolfPackBehavior.wildAdult(wolf) && !BeastDazeControl.blocksMoves(wolf)
                && (wolf.getTarget() == null || !wolf.getTarget().isAlive()) && super.canUse();
    }
    @Override public boolean canContinueToUse() {
        return WolfPackBehavior.wildAdult(wolf) && !BeastDazeControl.blocksMoves(wolf)
                && WolfPackBehavior.validTarget(wolf, wolf.getTarget()) && super.canContinueToUse();
    }
    @Override public void start() {
        super.start();
        if (wolf.getTarget() != null) {
            wolf.setPersistentAngerTarget(wolf.getTarget().getUUID());
            wolf.startPersistentAngerTimer();
        }
    }
}
