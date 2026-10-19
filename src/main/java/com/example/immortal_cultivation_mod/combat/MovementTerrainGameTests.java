package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_movement_terrain")
@PrefixGameTestTemplate(false)
public final class MovementTerrainGameTests {
    @GameTest(template = "npc_combat_arena", timeoutTicks = 70)
    public static void fallingPlayerKeepsItsLandingGround(GameTestHelper h) {
        CombatFeedbackGameTests.fallingPlayerKeepsItsLandingGround(h);
    }

    @GameTest(template = "npc_combat_arena")
    public static void confirmedWallImpactStillWorks(GameTestHelper h) {
        CombatKnockbackGameTests.launchedPlayerBreaksContactButNotBedrock(h);
    }

    @GameTest(template = "npc_combat_arena")
    public static void walkingCannotBreakTerrain(GameTestHelper h) {
        CombatKnockbackGameTests.movementWithoutAttackCannotBreakTerrain(h);
    }

    @GameTest(template = "npc_combat_arena")
    public static void protectedTerrainSurvivesAttacks(GameTestHelper h) {
        com.example.immortal_cultivation_mod.event.VaultProtectionGameTests.guardianAttackCannotExcavateGuardedSect(h);
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 70)
    public static void fallingNpcKeepsItsLandingGround(GameTestHelper h) {
        checkFall(h, ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel()));
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 70)
    public static void fallingBeastKeepsItsLandingGround(GameTestHelper h) {
        checkFall(h, ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get().create(h.getLevel()));
    }

    static void checkFall(GameTestHelper h, LivingEntity mob) {
        BlockPos floor = h.absolutePos(new BlockPos(12, 6, 12));
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            h.getLevel().setBlockAndUpdate(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            // The arena template is only seven blocks tall; remove its test-only barrier roof
            // over this drop lane so the mob reaches the stone rather than landing on that roof.
            for (int y = 1; y <= 13; y++)
                h.getLevel().setBlockAndUpdate(floor.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        Vec3 feet = Vec3.atBottomCenterOf(floor.above());
        // Use real collision resolution, without AI or unrelated combat moving the fixture.
        mob.setPos(feet.add(0, 9, 0));
        mob.setOnGround(true);
        CombatFeedbackEvents.tickMovement(mob);
        for (int tick = 1; tick <= 45; tick++) {
            int step = tick;
            h.runAfterDelay(tick, () -> {
                mob.setDeltaMovement(0, -1, 0);
                mob.move(MoverType.SELF, new Vec3(0, -1, 0));
                CombatFeedbackEvents.tickMovement(mob);
                if (step == 45) {
                    try {
                        h.assertTrue(CombatFeedbackEvents.emittedCount(mob) > 0,
                                "A real heavy landing still produces visible feedback");
                        h.assertTrue(mob.onGround() && Math.abs(mob.getY() - feet.y) < .001,
                                "Beasts and NPCs remain on their original landing surface: feet=" + feet
                                        + ", actual=" + mob.position() + ", grounded=" + mob.onGround()
                                        + ", floor=" + h.getLevel().getBlockState(floor));
                        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
                            h.assertTrue(h.getLevel().getBlockState(floor.offset(x, 0, z)).is(Blocks.STONE),
                                    "Ordinary falling cannot excavate ground or start a shaft");
                        h.succeed();
                    } finally { mob.discard(); }
                }
            });
        }
    }
}
