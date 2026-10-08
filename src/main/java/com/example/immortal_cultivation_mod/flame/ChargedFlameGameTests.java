package com.example.immortal_cultivation_mod.flame;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class ChargedFlameGameTests {
    @GameTest(template="fate_test")
    public static void chargeGrowsThenCompressesAndExposesItsCore(GameTestHelper h) {
        h.assertTrue(ChargedFlameShape.radius(0)==.125F,"Initial core is 0.25 blocks wide");
        float grown=ChargedFlameShape.radius(70), compressed=ChargedFlameShape.radius(87);
        h.assertTrue(grown*2>=2 && grown*2<=3 && compressed/grown>=.8 && compressed/grown<=.85,
                "Growth reaches giant size before contracting fifteen to twenty percent");
        h.assertTrue(ChargedFlameShape.shell(86)==1 && ChargedFlameShape.shell(87)<.1
                && ChargedFlameShape.shell(89)<.1,"Last three pre-launch ticks expose the core");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void everyChargedFlameHasDistinctFiniteBoundedGeometry(GameTestHelper h) {
        var identities=new java.util.HashSet<Long>();
        for(var kind:FlameKind.values()) {
            if(!kind.special())continue;
            long fingerprint=0;
            for(boolean flying:new boolean[]{false,true}) for(int detail:new int[]{1,2})
                for(float age:new float[]{0,29,30,50,70,74,77,86,87,89,90}) {
                    int[] counts={0,0,0};long[] hash={7};
                    var sink=new ChargedFlameShape.Sink() {
                        private void point(Vec3 p) {
                            h.assertTrue(Double.isFinite(p.lengthSqr())&&p.length()<7,"Geometry stays finite and local: "+kind);
                            hash[0]=hash[0]*31+p.hashCode();
                        }
                        private void style(float size,float alpha) {
                            h.assertTrue(Float.isFinite(size)&&size>0&&size<2&&Float.isFinite(alpha)&&alpha>=0&&alpha<=1,
                                    "Valid widths, radii and opacity: "+kind);
                        }
                        public void line(Vec3 a,Vec3 b,float width,int color,float alpha) {
                            point(a);point(b);style(width,alpha);counts[0]++;hash[0]=hash[0]*31+color;
                        }
                        public void core(Vec3 p,float radius,int color,float alpha) {
                            point(p);style(radius,alpha);counts[1]++;hash[0]=hash[0]*31+color;
                        }
                        public void leaf(Vec3 p,Vec3 direction,float size,int color,float alpha) {
                            point(p);point(direction);style(size,alpha);counts[2]++;
                        }
                    };
                    ChargedFlameShape.emit(kind,age,1,flying,detail,sink);
                    h.assertTrue(counts[0]>100 && counts[0]<1300 && counts[1]>=2 && counts[1]<=30 && counts[2]<=120,
                            "All phases keep their layered geometry within render budgets: "+kind);
                    if(!flying&&age==50&&detail==2) fingerprint=hash[0];
                }
            h.assertTrue(identities.add(fingerprint),"Each element has a distinct charge presentation: "+kind);
        }
        h.succeed();
    }
}
