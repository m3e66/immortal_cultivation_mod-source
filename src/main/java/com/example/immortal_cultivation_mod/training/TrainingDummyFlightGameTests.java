package com.example.immortal_cultivation_mod.training;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.TrainingDummyEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_dummy_flight")
@PrefixGameTestTemplate(false)
public final class TrainingDummyFlightGameTests {
    private static TrainingDummyEntity dummy(GameTestHelper h, Vec3 offset, String realm) {
        var dummy = ModEntities.TRAINING_DUMMY.get().create(h.getLevel());
        dummy.setPos(h.absoluteVec(offset));
        dummy.ensureProfile();
        dummy.profile().putString("Realm", realm);
        h.getLevel().addFreshEntity(dummy);
        return dummy;
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void goldenCoreAndHigherHoverButFoundationFalls(GameTestHelper h) {
        var foundation = dummy(h, new Vec3(2, 5000, 2), CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE);
        var golden = dummy(h, new Vec3(4, 5000, 2), CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        var nascent = dummy(h, new Vec3(6, 5000, 2), CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY);
        var spirit = dummy(h, new Vec3(8, 5000, 2), CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_EARLY);
        double start = golden.getY();
        h.runAfterDelay(20, () -> {
            try {
                h.assertTrue(!foundation.isNoGravity() && foundation.getY() < start - 3, "Foundation dummy falls normally");
                for (var flyer : new TrainingDummyEntity[]{golden, nascent, spirit}) {
                    h.assertTrue(flyer.isNoGravity() && Math.abs(flyer.getY() - start) < .3,
                            "Golden Core and higher hold altitude without learning a movement spell");
                    h.assertTrue(!flyer.noPhysics, "Hovering keeps normal block collisions enabled");
                }
                h.succeed();
            } finally {
                foundation.discard();golden.discard();nascent.discard();spirit.discard();
            }
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 110)
    public static void mobileDummyPursuesAirborneTargetThenHoversWhenStopped(GameTestHelper h) {
        var hunter = dummy(h, new Vec3(2, 5000, 2), CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        var target = dummy(h, new Vec3(8, 5012, 2), CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        hunter.mobile = true;hunter.combatEnabled = true;hunter.setTarget(target);
        double start = hunter.getY();
        h.runAfterDelay(55, () -> {
            h.assertTrue(hunter.isNoGravity() && hunter.getY() > start + 8 && hunter.distanceToSqr(target) < 9,
                    "Real dummy ticks climb and reach an airborne opponent");
            hunter.halt();hunter.mobile = false;Vec3 stopped = hunter.position();
            h.runAfterDelay(20, () -> {
                try {
                    h.assertTrue(hunter.isNoGravity() && hunter.position().distanceTo(stopped) < .4,
                            "Stop and stationary mode hold the dummy in the air");
                    h.succeed();
                } finally {hunter.discard();target.discard();}
            });
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void controllerDestinationFliesAndLoweringRealmRestoresGravity(GameTestHelper h) {
        var dummy = dummy(h, new Vec3(2, 5000, 2), CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        Vec3 destination = dummy.position().add(6, 8, 0);
        dummy.moveToPoint(destination);
        h.runAfterDelay(50, () -> {
            h.assertTrue(dummy.isNoGravity() && dummy.position().distanceTo(destination) < .4,
                    "Controller destination uses actual flight rather than ground-only navigation");
            dummy.profile().putString("Realm", CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE);
            double before = dummy.getY();
            h.runAfterDelay(10, () -> {
                try {
                    h.assertTrue(!dummy.isNoGravity() && dummy.getY() < before - 1,
                            "Changing to a lower realm ends flight and restores gravity");
                    h.succeed();
                } finally {dummy.discard();}
            });
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 80)
    public static void finiteQiPaysOncePerSecondAndExhaustionEndsHover(GameTestHelper h) {
        var dummy = dummy(h, new Vec3(2, 5000, 2), CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        dummy.unlimitedQi = false;dummy.profile().putInt("Qi", 200);double start = dummy.getY();
        h.runAfterDelay(5, () -> h.assertTrue(dummy.isNoGravity() && dummy.profile().getInt("Qi") == 100,
                "First second of hovering costs normal 100 Qi"));
        h.runAfterDelay(15, () -> h.assertTrue(dummy.profile().getInt("Qi") == 100, "Hover upkeep is not charged every tick"));
        h.runAfterDelay(25, () -> h.assertTrue(dummy.isNoGravity() && dummy.profile().getInt("Qi") == 0,
                "Prepaid second remains airborne after spending the last Qi"));
        h.runAfterDelay(50, () -> {
            try {
                h.assertTrue(!dummy.isNoGravity() && dummy.getY() < start - 1, "Exhausted dummy falls after paid flight expires");
                h.succeed();
            } finally {dummy.discard();}
        });
    }
}
