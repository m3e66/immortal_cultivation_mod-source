package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_life_progress")
@PrefixGameTestTemplate(false)
public final class LifeProgressGameTests {
    private static CompoundTag read(ServerPlayer player) {
        return OuterSystemData.read(player).getCompound("LifeSimulation").copy();
    }

    private static void write(ServerPlayer player, CompoundTag life) {
        CompoundTag outer = OuterSystemData.read(player);
        outer.put("LifeSimulation", life.copy());
        OuterSystemData.save(player, outer);
    }

    private static CompoundTag ready(LifeSimulationGameTests.Player fixture, GameTestHelper helper) {
        CompoundTag life = fixture.start(helper);
        life.putInt("Age", 22); life.putInt("Realm", 1); life.putInt("HighestRealm", 1);
        life.putInt("Health", 80); life.putInt("Mentality", 80); life.putInt("TrainingStrain", 0);
        life.putInt("AnnualActionPoints", 3); life.putInt("RoutineCursor", 0);
        life.putString("ActiveMethod", CultivationMethods.BASIC_BREATHING);
        life.putString("SpiritualRoot", "single"); life.putLong("CultivationProgress", 0);
        life.remove("CurrentEvent"); life.remove("Story"); life.remove("Battle");
        life.put("History", new ListTag());
        return life;
    }

    private static long required(int realm) {
        return CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(realm)).qiNeeded();
    }

    @GameTest(template = "fate_test")
    public static void healthConditionSkipsAtAndAboveSixty(GameTestHelper helper) {
        for (int health : new int[]{59, 60, 80}) try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            CompoundTag life = ready(fixture, helper); life.putInt("Realm", 0); life.putInt("Health", health);
            write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "routine:slot:0:rest:health");
            LifeSimulationSystem.handleAction(fixture.player, "routine:slot:1:family:none");
            LifeSimulationSystem.handleAction(fixture.player, "routine:slot:2:study:none");
            LifeSimulationSystem.handleAction(fixture.player, "routine:run");
            CompoundTag result = read(fixture.player);
            long rests = result.getList("History", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                    .filter(log -> log.getString("Key").equals("life.immortal_cultivation_mod.activity.rest")).count();
            helper.assertTrue(rests == (health < 60 ? 1 : 0), "Rest must follow the strict health threshold: " + health);
            helper.assertTrue(result.getInt("Age") == 23, "Skipped condition must not stall the planned year");
            for (String action : new String[]{"family", "study"}) helper.assertTrue(
                    result.getList("History", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                            .anyMatch(log -> log.getString("Key").equals("life.immortal_cultivation_mod.activity." + action)),
                    "Later eligible slots still execute after a skip");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void repeatedRestRechecksHealthAfterEachSlot(GameTestHelper helper) {
        try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            CompoundTag life = ready(fixture, helper); life.putInt("Realm", 0); life.putInt("Health", 59);
            write(fixture.player, life);
            for (int slot = 0; slot < 3; slot++) LifeSimulationSystem.handleAction(fixture.player, "routine:slot:" + slot + ":rest:health");
            LifeSimulationSystem.handleAction(fixture.player, "routine:run");
            helper.assertTrue(read(fixture.player).getList("History", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                    .filter(log -> log.getString("Key").equals("life.immortal_cultivation_mod.activity.rest")).count() == 1,
                    "Rest once from 59 to 63, then skip both remaining rest slots");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void cultivationActivityGainsTwentyFivePercentMore(GameTestHelper helper) {
        try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            CompoundTag life = ready(fixture, helper);
            life.putInt("Realm", 2); life.putInt("HighestRealm", 2);
            long oldGain = LifeSimulationBalance.gain(life, 1);
            long expected = Math.min(required(2), LifeSimulationBalance.gain(life, 1.25));
            helper.assertTrue(expected > oldGain, "Cultivation must improve over the previous activity multiplier");
            write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "activity:cultivate");
            CompoundTag result = read(fixture.player);
            helper.assertTrue(result.getLong("CultivationProgress") == expected,
                    "Real cultivation action uses the increased gain: expected " + expected + ", actual " + result.getLong("CultivationProgress"));
            helper.assertTrue(result.getInt("AnnualActionPoints") == 2 && result.getInt("TrainingStrain") == 6,
                    "Existing cultivation action cost and strain are preserved");
        }
        helper.succeed();
    }

    private static CompoundTag event(String next) {
        CompoundTag event = new CompoundTag(), choice = new CompoundTag();
        event.putString("Id", "progress_test"); choice.putString("Id", "finish");
        choice.putString("NextNode", next); choice.putString("ResultKey", "life.immortal_cultivation_mod.activity.explore");
        ListTag choices = new ListTag(); choices.add(choice); event.put("Choices", choices);
        return event;
    }

    @GameTest(template = "fate_test")
    public static void eventCompletionRewardsOnceAndCapsProgress(GameTestHelper helper) {
        try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            CompoundTag life = ready(fixture, helper); life.put("CurrentEvent", event("")); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "choice:finish");
            long expected = Math.max(1, required(1) / 20);
            helper.assertTrue(read(fixture.player).getLong("CultivationProgress") == expected, "Completed encounter grants five percent of maximum progress");
            LifeSimulationSystem.handleAction(fixture.player, "choice:finish");
            helper.assertTrue(read(fixture.player).getLong("CultivationProgress") == expected, "Replayed completion cannot grant another bonus");
            life = read(fixture.player); life.putLong("CultivationProgress", required(1) - 1);
            life.put("CurrentEvent", event("")); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "choice:finish");
            helper.assertTrue(read(fixture.player).getLong("CultivationProgress") == required(1), "Encounter bonus caps at full progress");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void intermediateQuestNodeDoesNotGrantCompletionBonus(GameTestHelper helper) {
        try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            CompoundTag life = ready(fixture, helper), event = event("second");
            event.putString("QuestId", "immortal_cultivation_mod:progress_fixture");
            event.putString("QuestNode", "first"); life.put("CurrentEvent", event); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "choice:finish");
            helper.assertTrue(read(fixture.player).getLong("CultivationProgress") == 0, "Intermediate decision is not encounter completion");
            life = read(fixture.player); event = event("");
            event.putString("QuestId", "immortal_cultivation_mod:progress_fixture"); event.putString("QuestNode", "second");
            life.put("CurrentEvent", event); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "choice:finish");
            helper.assertTrue(read(fixture.player).getLong("CultivationProgress") == Math.max(1, required(1) / 20),
                    "Final quest decision grants one encounter bonus");
        }
        helper.succeed();
    }

    private static CompoundTag story() {
        CompoundTag story = new CompoundTag(), definition = new CompoundTag(), ending = new CompoundTag();
        story.putString("Id", "progress_story"); story.putUUID("Instance", UUID.randomUUID());
        ending.putString("id", "ending"); ListTag nodes = new ListTag(); nodes.add(ending);
        definition.put("nodes", nodes); story.put("Definition", definition);
        return story;
    }

    @GameTest(template = "fate_test")
    public static void storyBonusUsesCurrentRealmAndRejectsAbandonment(GameTestHelper helper) {
        for (int realm : new int[]{1, 4, 10}) {
            CompoundTag life = LifeReplacementStoryGameTests.person(realm, 0);
            life.put("Story", story()); LifeSimulationStories.complete(life, "ending");
            long expected = Math.max(1, required(realm) / 20);
            helper.assertTrue(life.getLong("CultivationProgress") == expected, "Story reward scales with realm " + realm);
            LifeSimulationStories.complete(life, "ending");
            helper.assertTrue(life.getLong("CultivationProgress") == expected, "Repeated finish without active story cannot pay twice");
            life.put("Story", story()); LifeSimulationStories.complete(life, "withdraw");
            helper.assertTrue(life.getLong("CultivationProgress") == expected, "Manual withdrawal grants no completion bonus");
            CompoundTag abandoned = story(); abandoned.putBoolean("Abandoned", true); life.put("Story", abandoned);
            LifeSimulationStories.complete(life, "ending");
            helper.assertTrue(life.getLong("CultivationProgress") == expected, "Abandonment grants no completion bonus");
        }
        for (int realm : new int[]{0, 1}) {
            CompoundTag life = LifeReplacementStoryGameTests.person(realm, 0); life.putInt("Health", 0);
            LifeSimulationBalance.rewardEncounter(life);
            helper.assertTrue(life.getLong("CultivationProgress") == 0, "Dead or mortal life cannot gain cultivation from encounters");
        }
        helper.succeed();
    }
}
