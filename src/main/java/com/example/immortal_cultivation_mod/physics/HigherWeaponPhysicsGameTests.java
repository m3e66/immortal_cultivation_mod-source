package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class HigherWeaponPhysicsGameTests {
    @GameTest(template="fate_test") public static void silkWhipKeepsLengthAndRespondsToHand(GameTestHelper helper) {
        WhipRope rope=new WhipRope(2.2,.024,.0096,1.99,1.9,5);
        Vec3 anchor=new Vec3(0,10,0),up=new Vec3(0,1,0);
        rope.reset(0,anchor,up);
        for(int i=1;i<=360;i++)rope.advance(i/120D,anchor,up,WhipRope.AIR);
        helper.assertTrue(rope.point(32).y<anchor.y-1.4,"Silk lash falls under gravity");
        helper.assertTrue(rope.maximumStretch()<.1,"Silk lash cannot stretch into disconnected pieces");
        Vec3 before=rope.point(32);rope.advance(3+1/120D,anchor.add(.1,0,0),up,WhipRope.AIR);
        helper.assertTrue(Math.abs(rope.point(32).x-before.x)<.08,"Tip lags behind the hand instead of moving rigidly");
        helper.succeed();
    }
    @GameTest(template="fate_test") public static void soulHookUsesWeightedChain(GameTestHelper helper) {
        WhipRope rope=new WhipRope(.55,.036,.056,1.7,1.4,.12);
        Vec3 anchor=new Vec3(0,10,0),up=new Vec3(0,1,0);
        rope.reset(0,anchor,up);
        for(int i=1;i<=480;i++)rope.advance(i/120D,anchor,up,WhipRope.AIR);
        helper.assertTrue(rope.point(32).y<anchor.y-.3,"Heavy hook hangs below its chain socket");
        helper.assertTrue(rope.maximumStretch()<.12,"Metal links retain their connected length");
        Vec3 before=rope.point(32);rope.advance(4+1/120D,anchor.add(.08,0,0),up,WhipRope.AIR);
        helper.assertTrue(Math.abs(rope.point(32).x-before.x)<.06,"Heavy hook keeps inertia when the hand moves");
        rope.advance(5,anchor.add(30,0,0),up,WhipRope.AIR);
        helper.assertTrue(rope.point(0).distanceTo(anchor.add(30,0,0))<1e-8,"Teleport resets chain without an explosive impulse");
        helper.succeed();
    }
}
