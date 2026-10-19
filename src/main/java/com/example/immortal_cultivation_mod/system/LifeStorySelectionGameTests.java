package com.example.immortal_cultivation_mod.system;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_replacement_stories")
@PrefixGameTestTemplate(false)
public final class LifeStorySelectionGameTests {
    @GameTest(template = "fate_test", timeoutTicks = 600)
    public static void randomSelectionCanStartEveryEligibleStoryInDifferentOrders(GameTestHelper helper) {
        var resources = helper.getLevel().getServer().getResourceManager();
        var baseline = LifeReplacementStoryGameTests.person(10, 0);
        baseline.putString("Sect", "qingyun");
        Set<String> expected = new HashSet<>();
        for (var definition : LifeSimulationStories.definitions(resources)) {
            if (LifeSimulationStories.eligible(baseline, definition)) expected.add(definition.getString("id"));
        }
        helper.assertTrue(expected.size() >= 7, "All authored stories and the loot encounter are eligible");
        Set<String> seen = new HashSet<>(), firstStories = new HashSet<>(), orders = new HashSet<>();
        for (int seed = 0; seed < 32; seed++) {
            var life = baseline.copy();
            var random = RandomSource.create(seed);
            StringBuilder order = new StringBuilder();
            for (int index = 0; index < expected.size(); index++) {
                helper.assertTrue(LifeSimulationStories.select(life, resources, "adventure", random),
                        "Every remaining eligible story can start");
                String id = life.getCompound("Story").getString("Id");
                if (index == 0) firstStories.add(id);
                seen.add(id); order.append(id).append('/');
                // Model an existing completion receipt; selection must exclude it next time.
                var journal = life.getCompound("StoryJournal");
                helper.assertTrue(!journal.contains(id), "Completed stories never repeat in the same realm");
                CompoundTag receipt = new CompoundTag(); receipt.putInt("Realm", life.getInt("Realm"));
                journal.put(id, receipt); life.put("StoryJournal", journal);
                life.remove("Story"); life.remove("CurrentEvent");
            }
            orders.add(order.toString());
            var before = life.copy();
            helper.assertTrue(!LifeSimulationStories.select(life, resources, "adventure", random)
                    && life.equals(before), "Exhausted pool cannot restart completed stories or mutate the life");
        }
        helper.assertTrue(seen.equals(expected), "Random draws cover every eligible story");
        helper.assertTrue(firstStories.size() > 1 && orders.size() > 1, "Different seeds give different opening stories and story orders");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void randomSelectionRespectsEligibilityAndNeverRerollsSavedEncounters(GameTestHelper helper) {
        var resources = helper.getLevel().getServer().getResourceManager();
        var life = LifeReplacementStoryGameTests.person(0, 0);
        life.putInt("Age", 15);
        var child = life.copy();
        helper.assertTrue(!LifeSimulationStories.select(life, resources, "adventure", RandomSource.create(1))
                && child.equals(life), "Random selection cannot bypass minimum age");
        life.putInt("Age", 28);
        for (int seed = 0; seed < 32; seed++) {
            var attempt = life.copy();
            helper.assertTrue(LifeSimulationStories.select(attempt, resources, "adventure", RandomSource.create(seed)),
                    "Adult mortal has eligible stories");
            helper.assertTrue(!attempt.getCompound("Story").getCompound("Definition").getBoolean("sect"),
                    "Random selection cannot grant a sect-only story to an unaffiliated person");
            var saved = attempt.copy();
            helper.assertTrue(!LifeSimulationStories.select(attempt, resources, "adventure", RandomSource.create(seed + 1000))
                    && attempt.equals(saved), "Repeated requests cannot replace the committed story, cast, choices, or rewards");
            var reloaded = saved.copy();
            helper.assertTrue(!LifeSimulationStories.select(reloaded, resources, "adventure", RandomSource.create(seed + 2000))
                    && reloaded.equals(saved), "Reload preserves the randomly selected encounter");
        }
        helper.succeed();
    }
}
