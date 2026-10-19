package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.CultivationFlightVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class NpcFlightGameTests {
    private static LianqiCultivatorEntity flyer(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        var profile = NpcProfile.cultivationProfile(npc);
        profile.putString("Realm", CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        profile.putInt("MaxQi", 3000);
        profile.putInt("Qi", 1000);
        npc.setOnGround(false);
        npc.setNoGravity(true);
        npc.setNoAi(true);
        npc.setPos(h.absoluteVec(new Vec3(6.5, 7, 6.5)));
        return npc;
    }

    @GameTest(template = "npc_combat_arena")
    public static void movementTurnsSmoothlyWithoutChangingStationaryAim(GameTestHelper h) {
        var npc = flyer(h);
        npc.setYRot(0);
        npc.setYBodyRot(0);
        npc.setYHeadRot(0);
        for (Vec3 direction : new Vec3[]{new Vec3(.1, 0, 0), new Vec3(0, .1, -.1), new Vec3(-.1, 0, 0)}) {
            npc.xo = npc.getX() - direction.x;
            npc.yo = npc.getY() - direction.y;
            npc.zo = npc.getZ() - direction.z;
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            for (int tick = 0; tick < 20; tick++) {
                npc.yRotO = npc.getYRot();
                npc.yBodyRotO = npc.yBodyRot;
                npc.yHeadRotO = npc.getYHeadRot();
                // Emulate vanilla controls snapping before the post-tick correction.
                npc.setYRot(yaw);
                npc.setYHeadRot(yaw);
                NpcFlight.faceTravel(new EntityTickEvent.Post(npc));
                h.assertTrue(Math.abs(Mth.wrapDegrees(npc.getYRot() - npc.yRotO)) <= 12.001
                        && Math.abs(Mth.wrapDegrees(npc.yBodyRot - npc.yBodyRotO)) <= 12.001
                        && Math.abs(Mth.wrapDegrees(npc.getYHeadRot() - npc.yHeadRotO)) <= 12.001,
                        "Travel turns are bounded even if vanilla controls snap first");
            }
            h.assertTrue(Math.abs(Mth.wrapDegrees(npc.getYRot() - yaw)) < .001
                    && Math.abs(Mth.wrapDegrees(npc.yBodyRot - yaw)) < .001
                    && Math.abs(Mth.wrapDegrees(npc.getYHeadRot() - yaw)) < .001,
                    "Turns finish aligned with travel, including a reversal");
        }
        npc.yRotO = npc.yBodyRotO = npc.yHeadRotO = 179;
        NpcFlight.faceMovement(npc, Vec3.directionFromRotation(0, -179).scale(.1));
        h.assertTrue(Math.abs(npc.getYRot() - 181) < .001, "Wraparound takes the short two-degree turn");
        npc.setYHeadRot(17);
        NpcFlight.faceMovement(npc, Vec3.ZERO);
        NpcFlight.faceMovement(npc, new Vec3(0, .12, 0));
        NpcFlight.faceMovement(npc, new Vec3(50, 0, 0));
        h.assertTrue(npc.getYHeadRot() == 17, "Standing, vertical movement and teleports preserve aim");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void walkingRampsSpeedAndSlowsForCornersWithoutSlidingAfterStop(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        var control = npc.getMoveControl();
        h.assertTrue(control instanceof NpcMoveControl, "Cultivators use the walking controller");
        npc.setYRot(0);
        float previous = 0;
        for (int tick = 0; tick < 6; tick++) {
            control.setWantedPosition(npc.getX(), npc.getY(), npc.getZ() + 8, 1);
            control.tick();
            h.assertTrue(npc.getSpeed() >= previous && npc.getSpeed() - previous <= .061,
                    "Walking builds speed over several ticks");
            previous = npc.getSpeed();
        }
        control.setWantedPosition(npc.getX() + 8, npc.getY(), npc.getZ(), 1);
        control.tick();
        h.assertTrue(Math.abs(Mth.wrapDegrees(npc.getYRot())) <= 18.001 && npc.getSpeed() < previous,
                "A sharp path corner turns gradually and reduces speed");
        control.tick();
        h.assertTrue(npc.zza == 0, "Stopped navigation cannot leave forward input behind");
        control.setWantedPosition(npc.getX(), npc.getY(), npc.getZ() + 8, 1);
        control.tick();
        h.assertTrue(npc.getSpeed() < previous * .5, "Restarting a path accelerates from rest");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void flightAcceleratesAndReversesWithoutVelocitySnaps(GameTestHelper h) {
        var npc = flyer(h);
        Vec3 destination = npc.position().add(50, 12, 0);
        for (int tick = 0; tick < 24; tick++) {
            Vec3 before = npc.getDeltaMovement();
            NpcSpellcasting.tickCreativeFlight(npc, destination);
            h.assertTrue(npc.getDeltaMovement().distanceTo(before) <= .220001,
                    "Flight acceleration stays bounded");
        }
        h.assertTrue(Math.abs(npc.getDeltaMovement().length() - NpcSpellcasting.CREATIVE_FLIGHT_SPEED) < .0001,
                "Smoothing retains full realm flight speed");
        destination = npc.position().add(-50, -12, 0);
        NpcSpellcasting.tickCreativeFlight(npc, destination);
        h.assertTrue(npc.getDeltaMovement().x > 0, "Reversing a destination first brakes existing momentum");
        for (int tick = 0; tick < 24; tick++) {
            Vec3 before = npc.getDeltaMovement();
            NpcSpellcasting.tickCreativeFlight(npc, destination);
            h.assertTrue(npc.getDeltaMovement().distanceTo(before) <= .220001,
                    "Reversal cannot snap velocity");
        }
        h.assertTrue(npc.getDeltaMovement().x < 0 && npc.getDeltaMovement().y < 0,
                "Flight completes the reversal in three dimensions");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void flightArrivesWithoutOrbitingOrRestarting(GameTestHelper h) {
        for (boolean innate : new boolean[]{true, false}) {
            var npc = flyer(h);
            Vec3 destination = npc.position().add(12, 2, 3);
            double previousDistance = npc.position().distanceTo(destination);
            for (int tick = 0; tick < 110; tick++) {
                if (innate) NpcSpellcasting.tickCreativeFlight(npc, destination);
                else NpcSpellcasting.tickYufengFlight(npc, destination, .48);
                // Integrate unobstructed travel with vanilla airborne drag.
                Vec3 motion = npc.getDeltaMovement();
                npc.setPos(npc.position().add(motion));
                npc.setDeltaMovement(motion.multiply(.91, .98, .91));
                double distance = npc.position().distanceTo(destination);
                h.assertTrue(distance <= previousDistance + .001, "Arrival should not overshoot and orbit its target");
                previousDistance = distance;
            }
            h.assertTrue(previousDistance < .03 && npc.getDeltaMovement().length() < .005,
                    "Both flight modes settle at their destination");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void idleLandingEasesOutOfClimb(GameTestHelper h) {
        var npc = flyer(h);
        h.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        npc.setDeltaMovement(.4, .15, 0);
        var brain = new NpcCombatBrain(npc);
        brain.tickFlightMotion(.48);
        h.assertTrue(npc.getDeltaMovement().y > 0 && npc.getDeltaMovement().y < .15,
                "Ending combat while climbing eases into descent rather than snapping downward");
        for (int tick = 0; tick < 20; tick++) {
            Vec3 before = npc.getDeltaMovement();
            brain.tickFlightMotion(.48);
            h.assertTrue(Math.abs(npc.getDeltaMovement().y - before.y) <= .025001,
                    "Landing has bounded vertical acceleration");
        }
        h.assertTrue(npc.getDeltaMovement().y >= -.120001 && npc.getDeltaMovement().y < -.1
                && npc.getDeltaMovement().horizontalDistanceSqr() == 0, "Descent settles at a slow vertical glide");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 140, batch = "npc_smooth_walking")
    public static void realWalkingStillReachesPathTarget(GameTestHelper h) {
        var npc = ModEntities.FANREN_NPC.get().create(h.getLevel());
        npc.goalSelector.removeAllGoals(goal -> true);
        npc.targetSelector.removeAllGoals(goal -> true);
        for (int x = 2; x <= 12; x++) for (int z = 3; z <= 8; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        npc.setPos(h.absoluteVec(new Vec3(3.5, 2, 4.5)));
        npc.setYRot(180);
        h.getLevel().addFreshEntity(npc);
        Vec3 destination = h.absoluteVec(new Vec3(11.5, 2, 6.5));
        double[] remaining = {npc.position().distanceTo(destination)};
        h.runAfterDelay(2, () -> {
            // moveTo(x,y,z) uses accuracy 1 and may intentionally end a block short.
            // Request the exact node so the endpoint assertion measures our controller.
            var path = npc.getNavigation().createPath(BlockPos.containing(destination), 0);
            h.assertTrue(path != null && path.canReach() && npc.getNavigation().moveTo(path, 1),
                    "Walking fixture has a complete path to the exact destination");
        });
        for (int tick : new int[]{45, 90}) {
            h.runAfterDelay(tick, () -> {
                double distance = npc.position().distanceTo(destination);
                h.assertTrue(distance < remaining[0] - 1, "Path steering keeps making progress after the initial turn");
                remaining[0] = distance;
            });
        }
        // Mortal walking speed is profile-dependent; allow the initial 180-degree turn
        // and acceleration before checking arrival, without relaxing the endpoint distance.
        h.runAfterDelay(125, () -> {
            h.assertTrue(npc.position().distanceTo(destination) < 1,
                    "Gradual turns still follow the path and reach its endpoint: " + npc.position()
                            + ", destination=" + destination + ", done=" + npc.getNavigation().isDone());
            npc.discard();
            h.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena")
    public static void innateNpcFlightNeverEmitsTrails(GameTestHelper h) {
        var npc = flyer(h);
        h.assertTrue(!npc.hasEffect(ModEffects.YUFENG_JUE), "Fixture uses innate flight without a spell");
        for (double speed : new double[]{0, .001, .04, .12, 1.62}) {
            h.assertTrue(!CultivationFlightVisuals.shouldEmit(npc, speed), "Innate NPC flight never emits trails at any speed");
        }
        h.assertTrue(!CultivationFlightVisuals.shouldEmit(npc, 11)
                && !CultivationFlightVisuals.shouldEmit(npc, Double.NaN), "Invalid movement and teleports cannot make trail segments");
        npc.setNoGravity(false);
        h.assertTrue(!CultivationFlightVisuals.eligible(npc), "An innate flight ability alone does not decorate falling");
        npc.setNoGravity(true);
        npc.setOnGround(true);
        h.assertTrue(!CultivationFlightVisuals.eligible(npc), "Landing ends emission");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 50)
    public static void hoveringPaysOncePerSecondAndKeepsCreditAcrossSave(GameTestHelper h) {
        var npc = flyer(h);
        var brain = new NpcCombatBrain(npc);
        var enemy = EntityType.ZOMBIE.create(h.getLevel());
        npc.setTarget(enemy); // Hold position rather than selecting an idle landing.
        for (int i = 0; i < 8; i++) brain.tickFlightMotion(.48);
        h.assertTrue(NpcProfile.qi(npc, npc.npcArchetype()) == 900,
                "Hovering prepays 100 Qi once, not once per steering call");
        var saved = new CompoundTag();
        NpcProfile.save(npc, saved);
        var restored = flyer(h);
        NpcProfile.load(restored, saved);
        h.assertTrue(NpcFlight.payUpkeep(restored) && NpcProfile.qi(restored, restored.npcArchetype()) == 900,
                "Reload keeps the paid second without a second charge");
        h.runAfterDelay(21, () -> {
            brain.tickFlightMotion(.48);
            h.assertTrue(NpcProfile.qi(npc, npc.npcArchetype()) == 800, "A new second of hovering pays again");
            h.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena")
    public static void windFlightPaysAndCannotContinueWithoutQi(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        npc.setPos(h.absoluteVec(new Vec3(6.5, 7, 6.5)));
        h.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        npc.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE, 200));
        npc.setOnGround(false);
        new NpcCombatBrain(npc).tickFlightMotion(.48);
        h.assertTrue(NpcProfile.qi(npc, npc.npcArchetype()) == 70 && npc.isNoGravity(),
                "Wind flight spends its own 30 Qi upkeep and remains supported");
        var empty = flyer(h);
        NpcProfile.cultivationProfile(empty).putInt("Qi", 99);
        empty.addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE, 200));
        new NpcCombatBrain(empty).tickFlightMotion(.48);
        h.assertTrue(!empty.isNoGravity() && !empty.hasEffect(ModEffects.YUFENG_JUE)
                && !NpcSpellcasting.canFly(empty), "Unaffordable innate flight cannot become free wind flight or hover");
        h.assertTrue(!CultivationFlightVisuals.eligible(empty), "Exhaustion stops the flight visual");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void groundedNpcDoesNotHoverOrSpendQi(GameTestHelper h) {
        var npc = flyer(h);
        npc.setOnGround(true);
        new NpcCombatBrain(npc).tickFlightMotion(.48);
        h.assertTrue(!npc.isNoGravity() && NpcProfile.qi(npc, npc.npcArchetype()) == 1000,
                "An innate flight-capable NPC can walk without paying flight upkeep");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void combatHoversThenIdleLandsAndAvoidsHotGround(GameTestHelper h) {
        var npc = flyer(h);
        h.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        var brain = new NpcCombatBrain(npc);
        var enemy = EntityType.ZOMBIE.create(h.getLevel());
        npc.setTarget(enemy);
        brain.tickFlightMotion(.48);
        h.assertTrue(npc.getDeltaMovement().y == 0, "A live combat target prevents idle descent");
        enemy.discard();
        brain.tickFlightMotion(.48);
        h.assertTrue(npc.getDeltaMovement().y < 0 && npc.getDeltaMovement().y >= -.12,
                "A dead target permits slow landing without waiting for target cleanup");
        h.setBlock(new BlockPos(6, 1, 6), Blocks.MAGMA_BLOCK);
        npc.setDeltaMovement(Vec3.ZERO);
        new NpcCombatBrain(npc).tickFlightMotion(.48);
        h.assertTrue(npc.getDeltaMovement().y == 0, "Idle landing avoids a dangerous support block");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 110, batch = "npc_flight_landing")
    public static void realTicksLandOnPartialBlocksAndStopPaying(GameTestHelper h) {
        var npc = flyer(h);
        // NoAI disables vanilla travel itself. Keep physics running, but remove wandering
        // goals so this fixture measures descent instead of choosing a new walking target.
        npc.setNoAi(false);
        npc.goalSelector.removeAllGoals(goal -> true);
        npc.targetSelector.removeAllGoals(goal -> true);
        for (int x = 5; x <= 7; x++) for (int z = 5; z <= 7; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE_SLAB);
        }
        npc.setPos(h.absoluteVec(new Vec3(6.5, 5, 6.5)));
        h.getLevel().addFreshEntity(npc);
        h.runAfterDelay(75, () -> {
            h.assertTrue(npc.onGround() && !npc.isNoGravity(), "Real NPC ticks finish descent and restore gravity: y="
                    + npc.getY() + ", expected=" + h.absoluteVec(new Vec3(6.5, 1.5, 6.5)).y
                    + ", motion=" + npc.getDeltaMovement() + ", onGround=" + npc.onGround()
                    + ", noGravity=" + npc.isNoGravity() + ", qi=" + NpcProfile.qi(npc, npc.npcArchetype()));
            h.assertTrue(Math.abs(npc.getY() - h.absoluteVec(new Vec3(6.5, 1.5, 6.5)).y) < .01,
                    "Landing respects slab height rather than floating or clipping through it");
            int qi = NpcProfile.qi(npc, npc.npcArchetype());
            h.assertTrue(qi < 1000 && qi > 0, "Live flying consumes finite Qi while landing");
            for (int delay = 1; delay <= 20; delay++) {
                h.runAfterDelay(delay, () -> h.assertTrue(!npc.isNoGravity(),
                        "Touchdown cannot restart flight on alternate ticks"));
            }
            h.runAfterDelay(21, () -> {
                h.assertTrue(NpcProfile.qi(npc, npc.npcArchetype()) >= qi,
                        "Grounded NPC stops paying and can regenerate");
                npc.discard();
                h.succeed();
            });
        });
    }
}
