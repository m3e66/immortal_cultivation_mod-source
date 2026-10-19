package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_replacement_stories")
@PrefixGameTestTemplate(false)
public final class LifeStoryLootGameTests {
    private static CompoundTag definition(GameTestHelper h) {
        return LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager()).stream()
                .filter(d -> d.getString("id").equals("life_story_courier_cache")).findFirst().orElseThrow();
    }

    private static void choose(CompoundTag life, String id) {
        var story=life.getCompound("Story");
        LifeSimulationStories.choose(life,story.getUUID("Instance")+"/"+story.getInt("Revision")+"/"+id,RandomSource.create(42));
    }

    @GameTest(template="fate_test")
    public static void actualStoryChoicesGrantNewPillsSpellsAndMethodsOnce(GameTestHelper h) {
        for (String kind : java.util.List.of("pill", "spell", "method")) {
            var life = LifeReplacementStoryGameTests.person(4, 0);
            life.putInt("Comprehension", 100); life.putInt("Medical", 120);
            h.assertTrue(LifeSimulationStories.start(life, definition(h)), "Reward encounter can start through normal story engine");
            choose(life, "recover"); LifeSimulationStories.choose(life, "reveal", RandomSource.create(12));
            h.assertTrue(life.getCompound("Story").getString("Node").equals("cache"), "Successful search actually reaches the reward choice");
            var offers = life.getCompound("Story").getCompound("LootOffers").copy();
            h.assertTrue(!offers.getCompound(kind).isEmpty(), "Eligible simulated character has a " + kind + " offer");
            var saved = life.copy(); LifeSimulationStories.openNode(life, "cache");
            h.assertTrue(life.getCompound("Story").getCompound("LootOffers").equals(saved.getCompound("Story").getCompound("LootOffers")), "Reopening/saving cannot reroll offers");
            String id = offers.getCompound(kind).getString("Id");
            int before = LifeSimulationBattle.itemCount(life, id);
            choose(life, kind);
            h.assertTrue(life.getCompound("CurrentEvent").getCompound("LootReward").equals(offers.getCompound(kind)), "Ending previews exact saved reward");
            choose(life, "finish");
            switch (kind) {
                case "pill" -> h.assertTrue(LifeSimulationBattle.itemCount(life, id) == before + 2, "Two registered new pills enter simulated inventory");
                case "spell" -> h.assertTrue(life.getList("DiscoveredSpells", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(id)), "New spell enters simulated knowledge");
                case "method" -> h.assertTrue(life.getList("LearnedMethods", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(id))
                        && life.getString("ActiveMethod").equals(id), "New method is learned and used for training");
            }
            h.assertTrue(life.getCompound("StoryJournal").getCompound("life_story_courier_cache").getCompound("LootReceipt").equals(offers.getCompound(kind)), "Reward receipt survives in story journal");
            var after = life.copy(); LifeSimulationStories.complete(life, "E_" + kind);
            h.assertTrue(after.equals(life) && !LifeSimulationStories.eligible(life, definition(h)), "Duplicate finish and same-realm replay never duplicate loot");
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void rootlessStoryCharacterCanTakeMedicineButCannotLearnTexts(GameTestHelper h) {
        var life = LifeReplacementStoryGameTests.person(0, 0);
        LifeSimulationStories.start(life, definition(h)); LifeSimulationStories.openNode(life, "cache");
        h.assertTrue(LifeStoryLoot.offer(life,"spell").isEmpty() && LifeStoryLoot.offer(life,"method").isEmpty(), "Rootless mortal has no invented spell or method offer");
        var choices = life.getCompound("CurrentEvent").getList("Choices", Tag.TAG_COMPOUND);
        h.assertTrue(choices.getCompound(0).getString("Illegal").isBlank()
                && !choices.getCompound(1).getString("Illegal").isBlank()
                && !choices.getCompound(2).getString("Illegal").isBlank(), "Medicine enabled; impossible learning choices greyed out");
        choose(life,"pill"); choose(life,"finish");
        h.assertTrue(life.getInt("Realm") == 0 && life.getList("DiscoveredSpells",Tag.TAG_STRING).isEmpty(), "Pills do not invent roots or cultivation");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void explorationPoolIncludesLootAndOffersRespectSimulatedRealm(GameTestHelper h) {
        java.util.Set<String> pillTypes = new java.util.HashSet<>();
        for (int seed = 0; seed < 128; seed++) {
            var life = LifeReplacementStoryGameTests.person(1,0); life.putLong("Seed",seed);
            LifeSimulationStories.start(life,definition(h));
            pillTypes.add(LifeStoryLoot.offer(life,"pill").getString("Id"));
            h.assertTrue(LifeSimulationInheritance.canLearnSpell(life,LifeStoryLoot.offer(life,"spell").getString("Id")), "Spell is learnable by simulated realm and roots");
            var method = CultivationMethods.get(LifeStoryLoot.offer(life,"method").getString("Id"));
            h.assertTrue(method != null && LifeStoryLoot.methodTier(method.tier()) <= 1, "Early realm never receives earth/heaven method");
        }
        h.assertTrue(pillTypes.size() >= 10,"Stories can find many actual new pill types, not only old Qi pills");
        boolean found = false;
        for(int seed=0;seed<40;seed++) {
            var life=LifeReplacementStoryGameTests.person(4,0);
            if(LifeSimulationStories.select(life,h.getLevel().getServer().getResourceManager(),"adventure",RandomSource.create(seed))
                    &&life.getCompound("Story").getString("Id").equals("life_story_courier_cache")){found=true;break;}
        }
        h.assertTrue(found,"New loot story is obtainable through actual random exploration selection");
        h.succeed();
    }
}
