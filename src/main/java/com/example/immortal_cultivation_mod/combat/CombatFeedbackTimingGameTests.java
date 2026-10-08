package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class CombatFeedbackTimingGameTests {
    @GameTest(template = "fate_test")
    public static void rapidHitsCannotSustainCameraShake(GameTestHelper h) {
        var timing = new CombatFeedbackTiming();
        h.assertTrue(timing.value(0, 0) == 0, "Fresh world has no camera impulse");
        h.assertTrue(timing.trigger(10), "First hit starts feedback");
        for (int tick = 11; tick < 26; tick++) {
            h.assertTrue(!timing.trigger(tick), "Rapid hits must not extend the original impulse");
            if (tick >= 17) h.assertTrue(timing.value(tick, .5) == 0, "Mandatory quiet interval after hit");
        }
        h.assertTrue(timing.trigger(26), "Later distinct impact can be felt");
        timing.clear();
        h.assertTrue(timing.value(0, 0) == 0 && timing.trigger(0), "World change clears cooldown and previous shake");
        h.assertTrue(CombatFeedbackTiming.distanceWeight(50, 24) == 0, "Distant impacts cannot shake camera");
        h.assertTrue(CombatFeedbackTiming.decay(Double.NaN, 7) == 0, "Invalid timing cannot enter camera transforms");
        h.succeed();
    }
}
