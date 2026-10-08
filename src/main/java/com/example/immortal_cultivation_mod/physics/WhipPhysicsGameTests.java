package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class WhipPhysicsGameTests {
    private static final Vec3 UP=new Vec3(0,1,0);
    @GameTest(template="fate_test") public static void silkLashSagsAndFollowsMovingHand(GameTestHelper h) {
        for(double scale:new double[]{.8,.82}) {
            var rope=WhipRope.spiritSilk(scale);
            Vec3 anchor=new Vec3(0,10,0);
            for(int i=0;i<=360;i++)rope.advance(i/120D,anchor,UP,WhipRope.AIR);
            h.assertTrue(rope.point(32).y<anchor.y-rope.length()*.6,
                    "Held silk hangs under gravity without an attack animation");
            Vec3 tip=rope.point(32);
            rope.advance(3+1/120D,anchor.add(.3,0,0),UP,WhipRope.AIR);
            h.assertTrue(rope.point(0).x>.29&&Math.abs(rope.point(32).x-tip.x)<.15,
                    "Silk tip lags behind hand motion instead of following a rigid coil");
            for(int i=2;i<=120;i++)rope.advance(3+i/120D,anchor.add(.3,0,0),UP,WhipRope.AIR);
            h.assertTrue(rope.maximumStretch()<.08,"Silk stays connected after the hand moves: "+rope.maximumStretch());
        }
        h.succeed();
    }
    @GameTest(template="fate_test") public static void heldCoilReleasesAndReturns(GameTestHelper h) {
        h.assertTrue(WhipHeldPose.deployment(10,0)==0,"Unused whip is stored in the hand");
        h.assertTrue(WhipHeldPose.deployment(0,0)==0 && WhipHeldPose.deployment(.06,0)>.4,
                "A strike smoothly releases the coil");
        h.assertTrue(WhipHeldPose.deployment(.3,0)==1,"The striking lash is fully physical");
        h.assertTrue(WhipHeldPose.deployment(.85,0)>.4 && WhipHeldPose.deployment(.85,0)<.6,
                "The lash gathers gradually after the swing");
        h.assertTrue(WhipHeldPose.deployment(1.05,0)==0,"Return ends in a supported coil");
        h.assertTrue(WhipHeldPose.deployment(0,.7)==.7,"Repeated attacks do not snap back to the stored pose");
        var points=new java.util.ArrayList<Vec3>();
        for(int i=0;i<=128;i++) {
            double angle=i*Math.PI*4/128;
            points.add(new Vec3(.35*Math.cos(angle),10+.35*Math.sin(angle),i*.001));
        }
        Vec3[] coil=WhipHeldPose.coil(points);var rope=new WhipRope();rope.hold(0,coil);
        Vec3 shift=new Vec3(2,1,-3);
        for(int i=0;i<coil.length;i++)coil[i]=coil[i].add(shift);
        rope.hold(.05,coil);
        for(int i=0;i<coil.length;i++) {
            h.assertTrue(rope.point(i).equals(coil[i]),"Every coil segment follows the hand while stored");
            h.assertTrue(rope.predictedPoint(i).equals(coil[i]),"Idle movement does not store lash velocity");
        }
        Vec3 anchor=coil[0],direction=coil[1].subtract(anchor).normalize();
        for(int i=1;i<=120;i++)rope.advance(.05+i/120D,anchor,direction,WhipRope.AIR);
        h.assertTrue(rope.point(32).distanceTo(coil[32])>.5,"Released coil responds to gravity and tension");
        h.assertTrue(rope.maximumStretch()<.08,"Releasing a tightly stored coil stays stable");h.succeed();
    }
    @GameTest(template="fate_test") public static void gravityAndTension(GameTestHelper h) {
        var rope=new WhipRope();Vec3 anchor=new Vec3(0,10,0);rope.reset(0,anchor,UP);
        for(int i=1;i<=360;i++)rope.advance(i/120D,anchor,UP,WhipRope.AIR);
        h.assertTrue(rope.point(32).y<anchor.y-3,"The unsupported lash falls under gravity");
        h.assertTrue(rope.point(0).distanceTo(anchor)<1e-8,"Handle socket stays pinned");
        h.assertTrue(rope.maximumStretch()<.08,"The lash resists stretching: "+rope.maximumStretch());h.succeed();
    }
    @GameTest(template="fate_test") public static void tipLagsBehindHandle(GameTestHelper h) {
        var rope=new WhipRope();Vec3 anchor=new Vec3(0,10,0);rope.reset(0,anchor,UP);
        for(int i=1;i<=240;i++)rope.advance(i/120D,anchor,UP,WhipRope.AIR);
        Vec3 tip=rope.point(32);rope.advance(2+1/120D,anchor.add(.3,0,0),UP,WhipRope.AIR);
        h.assertTrue(rope.point(0).x>.29,"Handle follows the player's motion immediately");
        h.assertTrue(Math.abs(rope.point(32).x-tip.x)<.15,"Tip keeps momentum instead of translating rigidly");h.succeed();
    }
    @GameTest(template="fate_test") public static void frameRateDoesNotChangePhysics(GameTestHelper h) {
        var a=run(20);var b=run(60);var c=run(144);
        h.assertTrue(a.point(32).distanceTo(b.point(32))<.2 && a.point(32).distanceTo(c.point(32))<.2,
                "20/60/144 FPS follow the same fixed-step physics: "+a.point(32).distanceTo(c.point(32)));h.succeed();
    }
    private static WhipRope run(int fps) {
        var rope=new WhipRope();rope.reset(0,new Vec3(0,10,0),UP);
        for(int i=1;i<=fps*3;i++) {
            double t=i/(double)fps;
            rope.advance(t,new Vec3(.35*Math.sin(t*3),10,0),UP,WhipRope.AIR);
        }
        return rope;
    }
    @GameTest(template="fate_test") public static void sweptWhipCannotSkipThinWalls(GameTestHelper h) {
        var contact=new WhipContacts(List.of(new AABB(.95,0,-1,1,3,1)));
        Vec3 hit=contact.project(new Vec3(0,1,0),new Vec3(3,1,0),.02);
        h.assertTrue(hit.x<.931 && hit.x>.92,"A fast segment is stopped at a thin wall: "+hit);
        var floor=new WhipContacts(List.of(new AABB(-10,-1,-10,10,0,10)));
        var rope=new WhipRope();Vec3 anchor=new Vec3(0,1.5,0);rope.reset(0,anchor,UP);
        for(int i=1;i<=360;i++)rope.advance(i/120D,anchor,UP,floor);
        for(int i=2;i<=32;i++)h.assertTrue(rope.point(i).y>=rope.radius(i)-.001,"Lash rests on the floor at node "+i);
        h.succeed();
    }
    @GameTest(template="fate_test") public static void teleportAndPauseStayStable(GameTestHelper h) {
        var rope=new WhipRope();rope.reset(0,new Vec3(0,1,0),UP);
        Vec3 anchor=new Vec3(10000,80,-12000);rope.advance(.05,anchor,UP,WhipRope.AIR);
        h.assertTrue(rope.point(0).equals(anchor),"Teleport reanchors the whip");
        for(int i=0;i<=32;i++)h.assertTrue(rope.point(i).distanceTo(anchor)<=WhipRope.LENGTH+.001,"No rope stretches across the world");
        Vec3 tip=rope.point(32);rope.advance(.05,anchor,UP,WhipRope.AIR);
        h.assertTrue(tip.equals(rope.point(32)),"Paused rendering advances no physics");
        rope.advance(5,anchor,UP,WhipRope.AIR);
        h.assertTrue(rope.maximumStretch()<.001,"Long render stalls reset instead of exploding");h.succeed();
    }
}
