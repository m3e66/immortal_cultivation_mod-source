package com.example.immortal_cultivation_mod.system;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Focused delivery and source validation checks for the direct reward picker. */
@GameTestHolder("immortal_cultivation_inheritance_picker")
@PrefixGameTestTemplate(false)
public final class LifeInheritancePickerGameTests {
    @GameTest(template = "fate_test")
    public static void directChoicesAreCanonicalAndDeliverExactlySelectedItem(GameTestHelper helper) {
        LifeSimulationGameTests.inheritanceSelectorsUseOnlySimulatedInventoryAndSpells(helper);
    }
    @GameTest(template = "fate_test")
    public static void cultivationStillCarriesAcrossAllStagesOnce(GameTestHelper helper) {
        LifeSimulationGameTests.inheritanceCarriesExactUserExampleAcrossRealmsOnce(helper);
    }
    @GameTest(template = "fate_test")
    public static void spellRequiresActualSimulatedLearning(GameTestHelper helper) {
        LifeSimulationGameTests.spellInheritanceRequiresActuallyLearnedSpellAndKeepsProficiency(helper);
    }
    @GameTest(template = "fate_test")
    public static void rootlessLivesCannotInventSpellRewards(GameTestHelper helper) {
        LifeSimulationGameTests.rootlessMortalCannotLearnOrInheritSpellsOrHostCultivation(helper);
    }
    @GameTest(template = "fate_test")
    public static void legacyPendingInventedRewardsAreRepaired(GameTestHelper helper) {
        LifeSimulationGameTests.legacyPendingInventedRewardsAreRepairedBeforeDelivery(helper);
    }
    @GameTest(template = "fate_test")
    public static void fullInventoryPreservesExactlyOneSelectedReward(GameTestHelper helper) throws Exception {
        LifeSimulationGameTests.fullInventoryReservesOneChoiceAndRestoresAtomicDelivery(helper);
    }
    @GameTest(template = "fate_test")
    public static void failedCheckpointNeverGrantsOrLocksReward(GameTestHelper helper) throws Exception {
        LifeSimulationGameTests.failedInheritanceCheckpointCannotGrantOrLockAChoice(helper);
    }
    @GameTest(template = "fate_test")
    public static void clientOnlyReceivesSourceBackedRewards(GameTestHelper helper) {
        LifeSimulationGameTests.clientReceivesMemoriesAndOnlySimulatedOwnedItems(helper);
    }
}
