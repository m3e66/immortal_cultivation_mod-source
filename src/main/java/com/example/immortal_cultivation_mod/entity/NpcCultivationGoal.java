package com.example.immortal_cultivation_mod.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Stops random strolling during meditation; danger and commands immediately release the pose. */
public final class NpcCultivationGoal extends Goal {
    private final PathfinderMob mob;
    public NpcCultivationGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }
    @Override public boolean canUse() {
        return mob instanceof CultivationNpc npc && npc.npcCultivationState() > 0
                && NpcCultivation.blocker(mob).isEmpty();
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { mob.getNavigation().stop(); }
    @Override public void tick() { mob.getNavigation().stop(); }
    @Override public void stop() { NpcCultivation.interrupt(mob, "interrupted"); }
}
