package com.example.immortal_cultivation_mod.entity;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_dragon_flight")
@PrefixGameTestTemplate(false)
public final class DragonFlightGameTests {
    @GameTest(template="fate_test")
    public static void flightReversesClimbAndSettlesAtLowerDestination(GameTestHelper h) {
        var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        try {
            dragon.setNoAi(true);
            Vec3 destination=h.absoluteVec(new Vec3(4,40,4));
            dragon.setPos(destination.add(0,30,0));dragon.setDeltaMovement(0,1,0);
            for(int i=0;i<100;i++) {
                dragon.getMoveControl().setWantedPosition(destination.x,destination.y,destination.z,1);
                dragon.getMoveControl().tick();
                dragon.move(MoverType.SELF,dragon.getDeltaMovement());
            }
            h.assertTrue(Math.abs(dragon.getY()-destination.y)<.5,"Climbing dragon descends and settles without runaway overshoot: "+dragon.position());
            h.assertTrue(Math.abs(dragon.getDeltaMovement().y)<.05,"Vertical momentum settles near destination");
        }finally{dragon.discard();}h.succeed();
    }
    @GameTest(template="fate_test")
    public static void stoppedNavigationBrakesVerticalMomentum(GameTestHelper h) {
        var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        try {
            dragon.setDeltaMovement(0,1,0);
            for(int i=0;i<20;i++)dragon.getMoveControl().tick();
            h.assertTrue(dragon.getDeltaMovement().length()<.001,"Idle controller stops leftover ascent");
        }finally{dragon.discard();}h.succeed();
    }
    @GameTest(template="fate_test")
    public static void actualCombatAiReturnsTowardTargetBelow(GameTestHelper h) {
        var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        var target=EntityType.COW.create(h.getLevel());
        try {
            target.setNoAi(true);target.setNoGravity(true);target.setInvulnerable(true);
            target.setPos(h.absoluteVec(new Vec3(4,45,4)));
            dragon.setPos(target.position().add(0,35,0));dragon.setDeltaMovement(0,.8,0);dragon.setTarget(target);
            double initial=dragon.getY();
            for(int i=0;i<160;i++)dragon.tick();
            h.assertTrue(dragon.getY()<initial-15,"Real combat AI descends toward lower target: y="+dragon.getY()+", initial="+initial);
            h.assertTrue(dragon.getY()>target.getY()-8&&dragon.getY()<target.getY()+18,"Combat flight stays near target height");
        }finally{dragon.discard();target.discard();}h.succeed();
    }
}
