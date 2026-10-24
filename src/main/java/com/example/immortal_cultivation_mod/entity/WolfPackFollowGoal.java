package com.example.immortal_cultivation_mod.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.goal.Goal;

/** Existing natural packs stay together; the highest-realm member leads without teleporting. */
final class WolfPackFollowGoal extends Goal {
    private final Wolf wolf;
    private Wolf leader;
    private long nextSearch;
    WolfPackFollowGoal(Wolf wolf) { this.wolf = wolf; setFlags(EnumSet.of(Flag.MOVE)); }
    @Override public boolean canUse() {
        long now = wolf.level().getGameTime();
        if (now < nextSearch || !WolfPackBehavior.wildAdult(wolf) || wolf.getTarget() != null
                || wolf.isInLove() || BeastDazeControl.blocksMoves(wolf)) return false;
        nextSearch = now + 40;
        var tag = wolf.getPersistentData();
        if (!tag.hasUUID(WolfPackBehavior.PACK_ID)) return false;
        var id = tag.getUUID(WolfPackBehavior.PACK_ID);
        var members = wolf.level().getEntitiesOfClass(Wolf.class, wolf.getBoundingBox().inflate(24),
                other -> WolfPackBehavior.wildAdult(other)
                        && other.getPersistentData().hasUUID(WolfPackBehavior.PACK_ID)
                        && id.equals(other.getPersistentData().getUUID(WolfPackBehavior.PACK_ID)));
        leader = WolfPackBehavior.leader(members);
        return leader != null && leader != wolf && wolf.distanceToSqr(leader) > 36;
    }
    @Override public boolean canContinueToUse() {
        return leader != null && WolfPackBehavior.wildAdult(wolf) && WolfPackBehavior.wildAdult(leader)
                && wolf.getTarget() == null && !wolf.isInLove() && !BeastDazeControl.blocksMoves(wolf)
                && wolf.distanceToSqr(leader) > 16 && wolf.distanceToSqr(leader) < 32 * 32;
    }
    @Override public void tick() {
        if (wolf.tickCount % 10 == 0) wolf.getNavigation().moveTo(leader, 1.05);
    }
    @Override public void stop() { leader = null; wolf.getNavigation().stop(); }
}
