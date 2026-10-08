package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class CombatImpactGameTests {
    @GameTest(template = "fate_test")
    public static void impactRemainsAtContactAndEnergyTravelsBehindTarget(GameTestHelper helper) {
        Vec3 origin = new Vec3(230.3, 80.7, -145.2);
        for (Vec3 incoming : new Vec3[]{Vec3.ZERO, new Vec3(0, 1, 0), new Vec3(0, -1, 0),
                new Vec3(1, .4, -.8), new Vec3(Double.NaN, 0, 0)}) {
            var basis = CombatImpactShapes.basis(incoming);
            helper.assertTrue(CombatImpactShapes.finite(basis.forward())
                            && Math.abs(basis.forward().length() - 1) < 1e-8
                            && Math.abs(basis.right().length() - 1) < 1e-8
                            && Math.abs(basis.up().length() - 1) < 1e-8
                            && Math.abs(basis.right().dot(basis.up())) < 1e-8,
                    "Zero, vertical and diagonal attacks have stable orthonormal impact planes");
            for (int i = 0; i < 32; i++) {
                Vec3 point = CombatImpactShapes.ringPoint(origin, basis, 3, i * Math.PI / 16);
                helper.assertTrue(Math.abs(point.distanceTo(origin) - 3) < 1e-8
                                && Math.abs(point.subtract(origin).dot(basis.forward())) < 1e-8,
                        "Shockwave stays centered at confirmed contact in the attack plane");
            }
            Vec3 behind = CombatImpactShapes.behind(origin, basis, .7, 1.2).subtract(origin);
            helper.assertTrue(Math.abs(behind.dot(basis.forward()) - 1.9) < 1e-8,
                    "Directional burst passes behind target along incoming attack trajectory");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void massiveImpactStagesWaitForFlashThenShockwave(GameTestHelper helper) {
        var stages = CombatImpactShapes.Stage.values();
        double previous = -1;
        for (var stage : stages) {
            double delay = CombatImpactShapes.delay(stage, 1);
            helper.assertTrue(delay > previous, "Massive flash, shock, cracks, debris, dust and falling energy are staged");
            if (stage != CombatImpactShapes.Stage.FLASH) {
                helper.assertTrue(CombatImpactShapes.elapsed(stage, 1, delay - .01) < 0,
                        "Future stage cannot render before its scheduled tick");
            }
            previous = delay;
        }
        helper.assertTrue(CombatImpactShapes.delay(CombatImpactShapes.Stage.SHOCKWAVE, 1) == 1,
                "Massive shockwave waits exactly 0.05 seconds after impact point");
        helper.assertTrue(CombatImpactShapes.delay(CombatImpactShapes.Stage.SHOCKWAVE, .6F) == 0,
                "Regular heavy hit responds immediately");
        double first = CombatImpactShapes.variation(123, 4);
        helper.assertTrue(first == CombatImpactShapes.variation(123, 4)
                        && first != CombatImpactShapes.variation(123, 5) && first >= 0 && first <= 1,
                "Seeded cracks and debris cannot jitter between rendered frames");
        helper.succeed();
    }
}
