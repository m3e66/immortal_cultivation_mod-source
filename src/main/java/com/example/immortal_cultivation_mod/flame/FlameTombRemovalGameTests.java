package com.example.immortal_cultivation_mod.flame;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_flame_tomb_removal")
@PrefixGameTestTemplate(false)
public final class FlameTombRemovalGameTests {
    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void smallTombCannotSpawnNetherFlame(GameTestHelper h) {
        FlameAcquisitionIntegrationGameTests.structureSource(h, "tomb1", FlameKind.NETHER);
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void largeTombCannotSpawnNetherFlame(GameTestHelper h) {
        FlameAcquisitionIntegrationGameTests.structureSource(h, "tomb2", FlameKind.NETHER);
    }

    @GameTest(template = "fate_test")
    public static void undeadAndOtherDeathSourcesStillWork(GameTestHelper h) {
        FlameAcquisitionIntegrationGameTests.allDeathSourcesProduceFlamesThatSurvivalPlayersCanCapture(h);
    }

    private FlameTombRemovalGameTests() {}
}
