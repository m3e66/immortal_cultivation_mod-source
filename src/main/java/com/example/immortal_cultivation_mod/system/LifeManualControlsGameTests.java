package com.example.immortal_cultivation_mod.system;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Focused integration cases for manual plans and parent relationship controls. */
@GameTestHolder("immortal_cultivation_life_manual_controls")
@PrefixGameTestTemplate(false)
public final class LifeManualControlsGameTests {
    @GameTest(template="fate_test") public static void parentFriendshipIsRejected(GameTestHelper h) {
        LifeSimulationGameTests.parentsCannotBeBefriendedButOrdinaryPeopleCan(h);
    }
    @GameTest(template="fate_test") public static void annualRunCannotReplayAnotherYear(GameTestHelper h) {
        LifeSimulationGameTests.routinePacketCannotAdvanceASecondYear(h);
    }
    @GameTest(template="fate_test") public static void annualPlanStillExecutesManualActivities(GameTestHelper h) {
        LifeSimulationGameTests.routineRunsFamilyExerciseAndNoConditionThroughRealActions(h);
    }
    @GameTest(template="fate_test") public static void annualEditsStillAcknowledge(GameTestHelper h) {
        LifeSimulationGameTests.routineEditsAcknowledgeEveryRequestInAnOpenSession(h);
    }
    @GameTest(template="fate_test") public static void deceasedFriendsRemainNonInteractive(GameTestHelper h) {
        LifeSimulationGameTests.deadPeopleRejectEveryRelationshipAction(h);
    }
}
