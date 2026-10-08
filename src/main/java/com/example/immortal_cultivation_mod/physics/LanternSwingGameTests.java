package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class LanternSwingGameTests {
    private static final Vec3 RIGHT=new Vec3(1,0,0),FORWARD=new Vec3(0,0,1);
    @GameTest(template="fate_test") public static void movementSwingsThenSettles(GameTestHelper h) {
        var swing=new LanternSwing();double maximum=0;
        for(int i=0;i<=120;i++) {
            double t=i/120D;swing.advance(t,new Vec3(Math.sin(t*Math.PI*2)*.4,2,0),RIGHT,FORWARD);
            maximum=Math.max(maximum,Math.abs(swing.roll()));
        }
        h.assertTrue(maximum>1 && maximum<=LanternSwing.MAX_ANGLE,"Acceleration produces a bounded physical swing");
        for(int i=121;i<=600;i++)swing.advance(i/120D,new Vec3(0,2,0),RIGHT,FORWARD);
        h.assertTrue(Math.abs(swing.roll())<.1 && Math.abs(swing.pitch())<.1,"A stationary hand settles without idle wobble");h.succeed();
    }
    @GameTest(template="fate_test") public static void pauseAndTeleportDoNotKickLantern(GameTestHelper h) {
        var swing=new LanternSwing();swing.advance(0,Vec3.ZERO,RIGHT,FORWARD);
        swing.advance(.05,new Vec3(.3,0,0),RIGHT,FORWARD);double roll=swing.roll();
        swing.advance(.05,new Vec3(.3,0,0),RIGHT,FORWARD);
        h.assertTrue(swing.roll()==roll,"Paused renders do not advance physics");
        swing.advance(.1,new Vec3(100,70,0),RIGHT,FORWARD);
        h.assertTrue(swing.roll()==0 && swing.pitch()==0,"Teleport resets instead of whipping the lantern around");h.succeed();
    }
}
