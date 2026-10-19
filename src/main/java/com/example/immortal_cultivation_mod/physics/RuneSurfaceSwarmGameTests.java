package com.example.immortal_cultivation_mod.physics;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_curse")
@PrefixGameTestTemplate(false)
public final class RuneSurfaceSwarmGameTests {
    private static RuneSurfaceSwarm pair() {
        var sheet=new RuneSurfaceSwarm(.6,.4);sheet.sync(.6,.4,List.of(new RuneSurfaceSwarm.Seed(1,.22,.2,.03,.04,.007,0),new RuneSurfaceSwarm.Seed(2,.32,.2,.03,.04,-.007,0)));return sheet;
    }
    @GameTest(template="fate_test") public static void independentRunesBumpAndSeparateInsteadOfPassingThrough(GameTestHelper h) {
        var sheet=pair();for(int t=0;t<12;t++)sheet.tick(t);
        var a=sheet.sample(1,1);var b=sheet.sample(2,1);
        h.assertTrue(sheet.collisions>0&&a.vx()<0&&b.vx()>0,"Head-on glyphs exchange normal momentum and bounce");
        h.assertTrue(b.x()-a.x()>=.05999,"Colliding glyph footprints separate");h.succeed();
    }
    @GameTest(template="fate_test") public static void manyRunesMoveIndividuallyWithBoundedDeterministicCollisions(GameTestHelper h) {
        var seeds=new ArrayList<RuneSurfaceSwarm.Seed>();
        for(int y=0;y<6;y++)for(int x=0;x<8;x++){double angle=(x+y*8)*2.399;seeds.add(new RuneSurfaceSwarm.Seed(x+y*8,(x+.5)*.1,(y+.5)*.12,.035,.046,Math.cos(angle)*.007,Math.sin(angle)*.007));}
        var a=new RuneSurfaceSwarm(.8,.72);var b=new RuneSurfaceSwarm(.8,.72);a.sync(.8,.72,seeds);Collections.reverse(seeds);b.sync(.8,.72,seeds);
        long initial=a.fingerprint();
        for(int t=0;t<360;t++) {
            a.tick(t);b.tick(t);h.assertTrue(a.fingerprint()==b.fingerprint(),"Registration order and additional views do not change the physical solve");
            for(var seed:seeds){var p=a.sample(seed.id(),1);h.assertTrue(Double.isFinite(p.x()+p.y()+p.vx()+p.vy())&&p.x()>=.03499&&p.x()<=.76501&&p.y()>=.04599&&p.y()<=.67401,"Dense moving ink stays on its model face");}
        }
        h.assertTrue(a.fingerprint()!=initial&&a.collisions>50&&a.wallBounces>0,"A crowded face visibly moves, collides and rebounds");h.succeed();
    }
    @GameTest(template="fate_test") public static void redrawsDoNotResetPositionsOrAdvancePhysics(GameTestHelper h) {
        var sheet=pair();sheet.tick(1);var before=sheet.sample(1,1);long hash=sheet.fingerprint();
        for(int i=0;i<120;i++){sheet.tick(1);sheet.sync(.6,.4,List.of(new RuneSurfaceSwarm.Seed(1,.22,.2,.03,.04,.007,0),new RuneSurfaceSwarm.Seed(2,.32,.2,.03,.04,-.007,0)));}
        h.assertTrue(sheet.fingerprint()==hash&&sheet.sample(1,1).equals(before),"Repeated frames neither stamp nodes back on a grid nor simulate faster");
        sheet.tick(2);var half=sheet.sample(1,.5F);var next=sheet.sample(1,1);h.assertTrue(Math.abs(half.x()-(before.x()+next.x())*.5)<1E-9,"Frame interpolation stays smooth");h.succeed();
    }
    @GameTest(template="fate_test") public static void growthRemovalResizeAndInactiveTimeRemainSafe(GameTestHelper h) {
        var sheet=pair();sheet.tick(1);sheet.sync(.02,.01,List.of(new RuneSurfaceSwarm.Seed(1,.01,.005,.05,.04,.007,0)));
        h.assertTrue(sheet.size()==1&&sheet.sample(2,1)==null,"Cleansed glyphs are removed from collisions");
        for(int i=0;i<=20;i++){var p=sheet.sample(1,i/20F);h.assertTrue(p.x()>=0&&p.x()<=.02&&p.y()>=0&&p.y()<=.01,"Every interpolated point remains on tiny resized faces");}
        long before=sheet.fingerprint();sheet.tick(10000);h.assertTrue(sheet.fingerprint()==before,"Returning to a hidden entity cannot integrate a giant time step");
        sheet.sync(.02,.01,List.of(new RuneSurfaceSwarm.Seed(3,Double.NaN,0,.01,.01,0,0)));h.assertTrue(sheet.size()==0,"Invalid bodies never poison the solver");h.succeed();
    }
}
