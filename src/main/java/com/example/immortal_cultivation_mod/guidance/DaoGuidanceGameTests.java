package com.example.immortal_cultivation_mod.guidance;

import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_guidance")
@PrefixGameTestTemplate(false)
public final class DaoGuidanceGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "guidance-test"));
    }
    private static CompoundTag state(FakePlayer player) {
        return player.getPersistentData().getCompound(DaoGuidanceSystem.ROOT_TAG);
    }
    private static int index(FakePlayer player, String chapter) {
        return state(player).getCompound("ObjectiveIndex").getInt(chapter);
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void allBranchesRegisteredWithUniqueIds(GameTestHelper helper) {
        helper.assertTrue(DaoGuidanceContent.chapters().size() == 42, "All existing chapters and nine current-feature readings are registered");
        var ids = new HashSet<String>();
        for (var chapter : DaoGuidanceContent.chapters()) {
            helper.assertTrue(ids.add(chapter.id()) && DaoGuidanceContent.chapter(chapter.id()) == chapter,
                    "Unique chapters resolve through the shared content registry");
        }
        helper.assertTrue(DaoGuidanceExpansion.chapters().stream().allMatch(DaoGuidanceContent.Chapter::secret),
                "Side chapters remain hidden until relevant gameplay");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void acknowledgementsCannotSkipGameplayOrLockedReadings(GameTestHelper helper) {
        var player = player(helper);
        DaoGuidanceSystem.acknowledge(player, "read:weapon_growth:step_1");
        helper.assertTrue(!state(player).getCompound("Discovered").getBoolean("weapon_growth"), "Forged reading does not discover a locked chapter");
        DaoGuidanceSystem.discover(player, "weapon_growth");
        DaoGuidanceSystem.acknowledge(player, "read:weapon_growth:step_1");
        helper.assertTrue(index(player, "weapon_growth") == 1, "Current reading can be acknowledged");
        DaoGuidanceSystem.acknowledge(player, "guide_restriction_added");
        DaoGuidanceSystem.acknowledge(player, "read:weapon_growth:step_1");
        helper.assertTrue(index(player, "weapon_growth") == 1, "Gameplay and stale readings cannot be acknowledged");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void earlyCaptureSurvivesLaterReadingAndRepeatedObservation(GameTestHelper helper) {
        var player = player(helper);
        DaoGuidanceObservations.record(player, "flame_captured");
        DaoGuidanceObservations.record(player, "vessel_obtained");
        helper.assertTrue(index(player, "flame_vessel") == 2, "Early real capture remains credited after obtaining the vessel");
        DaoGuidanceObservations.record(player, "flame_captured");
        helper.assertTrue(index(player, "flame_vessel") == 2, "Repeated observation cannot skip the tooltip reading");
        DaoGuidanceSystem.acknowledge(player, "read:flame_vessel:step_3");
        helper.assertTrue(index(player, "flame_vessel") == 3, "Only acknowledgement advances the reading");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void oldSaveIndicesMigrateOnceAndCompletedChaptersStayComplete(GameTestHelper helper) {
        var player = player(helper);
        var old = new CompoundTag(); old.putBoolean("Initialized", true); old.putBoolean("TrackingEnabled", true);
        var indices = new CompoundTag(); indices.putInt("storage_equipment", 2); indices.putInt("forging", 4);
        old.put("ObjectiveIndex", indices);
        var complete = new CompoundTag(); complete.putBoolean("breakthrough", true); old.put("Completed", complete);
        player.getPersistentData().put(DaoGuidanceSystem.ROOT_TAG, old);
        DaoGuidanceSystem.discover(player, "hair_styling");
        helper.assertTrue(index(player, "storage_equipment") == 3 && index(player, "forging") == 6,
                "Old indices follow their original objectives after inserted lessons");
        DaoGuidanceSystem.discover(player, "hair_styling");
        helper.assertTrue(index(player, "storage_equipment") == 3 && index(player, "forging") == 6,
                "Migration never runs twice");
        helper.assertTrue(state(player).getCompound("Completed").getBoolean("breakthrough"), "Completed main chapters remain complete");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void oneChargeDiscoversBothRelevantBranches(GameTestHelper helper) {
        var player = player(helper);
        DaoGuidanceObservations.record(player, "flame_charged");
        helper.assertTrue(state(player).getCompound("Discovered").getBoolean("flame_vessel")
                && state(player).getCompound("Discovered").getBoolean("giant_fireball"),
                "A shared gameplay action discovers every relevant side chapter");
        helper.assertTrue(index(player, "giant_fireball") == 1, "Charging credits the genuine action");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void simulationNeedsMultipleEventsAndRewardClaim(GameTestHelper helper) {
        var player = player(helper);
        DaoGuidanceObservations.record(player, "life_started");
        DaoGuidanceObservations.record(player, "life_choice");
        DaoGuidanceObservations.record(player, "life_event");
        DaoGuidanceObservations.record(player, "life_event");
        helper.assertTrue(index(player, "life_simulation") == 2, "Two events cannot satisfy the three-event objective");
        DaoGuidanceObservations.record(player, "life_event");
        helper.assertTrue(index(player, "life_simulation") == 3, "Three events unlock the reward explanation");
        DaoGuidanceSystem.acknowledge(player, "read:life_simulation:step_4");
        DaoGuidanceObservations.record(player, "life_settled");
        helper.assertTrue(!state(player).getCompound("Completed").getBoolean("life_simulation"), "Settlement alone cannot claim rewards");
        DaoGuidanceObservations.record(player, "life_finished");
        helper.assertTrue(state(player).getCompound("Completed").getBoolean("life_simulation"), "Actual reward claim completes the lesson");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void currentReadingsUnlockWithoutCompletingThemselves(GameTestHelper helper) {
        var player = player(helper);
        DaoGuidanceObservations.record(player, "life_started");
        for (String id : java.util.List.of("annual_plans", "life_card_battle", "life_inheritance")) {
            helper.assertTrue(state(player).getCompound("Discovered").getBoolean(id) && index(player, id) == 0,
                    "Starting a life discovers its new readings without acknowledging them");
        }
        DaoGuidanceSystem.acknowledge(player, "read:life_card_battle:step_3");
        helper.assertTrue(index(player, "life_card_battle") == 0, "An out-of-order acknowledgement cannot skip the first reading");
        DaoGuidanceSystem.acknowledge(player, "read:life_card_battle:step_1");
        DaoGuidanceObservations.record(player, "life_started");
        helper.assertTrue(index(player, "life_card_battle") == 1, "Repeated saturated gameplay observations retain reading progress");
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void everyCurrentReadingCanBeAcknowledgedInOrder(GameTestHelper helper) {
        var player = player(helper);
        helper.assertTrue(DaoGuidanceUpdates.chapters().size() == 9, "All nine current topics are present");
        for (var chapter : DaoGuidanceUpdates.chapters()) {
            DaoGuidanceSystem.discover(player, chapter.id());
            for (var objective : chapter.objectives()) {
                helper.assertTrue(DaoGuidanceExpansion.isReading(objective.action())
                                && DaoGuidanceExpansion.chapterFor(objective.action()).equals(chapter.id()),
                        "Reading actions resolve to their actual chapter");
                DaoGuidanceSystem.acknowledge(player, objective.action());
            }
            helper.assertTrue(state(player).getCompound("Completed").getBoolean(chapter.id()),
                    "Every new chapter can complete through its real acknowledgement path");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test", templateNamespace = "immortal_cultivation_guidance")
    public static void existingPlayersDiscoverEquipmentAndRealmReadings(GameTestHelper helper) {
        var player = player(helper);
        com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player,
                com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player)
                        .withCultivationLevel(com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_JINDAN
                                + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY));
        player.getInventory().items.set(0, new net.minecraft.world.item.ItemStack(
                com.example.immortal_cultivation_mod.item.ModItems.YINYANG_COMPASS.get()));
        DaoGuidanceObservations.tick(player);
        helper.assertTrue(state(player).getCompound("Discovered").getBoolean("qi_blast")
                        && state(player).getCompound("Discovered").getBoolean("exploration_tools"),
                "Existing Golden Core players and compass owners discover relevant readings without repeating old quests");
        helper.succeed();
    }
}
