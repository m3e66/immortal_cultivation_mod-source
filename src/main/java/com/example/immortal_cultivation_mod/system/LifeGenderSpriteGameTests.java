package com.example.immortal_cultivation_mod.system;

import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Gender resolution, legacy migration and existing appearance guarantees. */
@GameTestHolder("immortal_cultivation_gender_sprites")
@PrefixGameTestTemplate(false)
public final class LifeGenderSpriteGameTests {
    @GameTest(template="fate_test")
    public static void genderOverridesEveryOccupationAndMismatchedCachedPortrait(GameTestHelper h){
        for(String gender:List.of("female","male"))for(String occupation:List.of("physician","healer","herbalist","alchemist","smith","merchant","scholar","guard","guardian","soldier","monk","musician","hermit","sect_elder","cultivator")){
            var pool=gender.equals("female")?LifeSimulationSprites.FEMALE:LifeSimulationSprites.MALE;
            var person=new CompoundTag();person.putUUID("Id",new UUID(2,occupation.hashCode()));person.putString("Gender",gender);person.putString("Occupation",occupation);
            person.putString("PortraitSprite",gender.equals("female")?LifeSimulationSprites.MALE.getFirst():LifeSimulationSprites.FEMALE.getFirst());
            var before=person.copy();String id=LifeSimulationSprites.person(person).id();
            h.assertTrue(pool.contains(id)&&before.equals(person),"Read-only gender-correct portrait for "+gender+" "+occupation);
            LifeSimulationSprites.assignPerson(person);person.putString("Occupation","traveler");
            h.assertTrue(LifeSimulationSprites.person(person).id().equals(id),"Correct saved face survives occupation changes");
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void playerNpcAndAnimationFramesRepairWrongGenderWithoutChangingCombat(GameTestHelper h){
        var life=new CompoundTag();life.putString("Name","Female smith");life.putString("Gender","female");life.putString("Occupation","smith");life.putString("PortraitSprite",LifeSimulationSprites.MALE.getFirst());life.putLong("Seed",381);
        var npc=new CompoundTag();npc.putUUID("Id",new UUID(3,4));npc.putString("Gender","female");npc.putString("Occupation","guard");npc.putString("PortraitSprite",LifeSimulationSprites.MALE.get(1));
        var people=new ListTag();people.add(npc);life.put("Relationships",people);
        var actors=new ListTag();var player=new CompoundTag();player.putString("Id","player");player.putString("Sprite",LifeSimulationSprites.MALE.getFirst());player.putDouble("Hp",71.25);actors.add(player);
        var enemy=new CompoundTag();enemy.putString("Id","enemy1");enemy.putUUID("NpcId",npc.getUUID("Id"));enemy.putString("Sprite",LifeSimulationSprites.MALE.getFirst());enemy.putDouble("Hp",53.75);actors.add(enemy);
        var male=new CompoundTag();male.putString("Id","enemy2");male.putString("Gender","male");male.putString("Sprite",LifeSimulationSprites.FEMALE.getFirst());actors.add(male);
        var battle=new CompoundTag();battle.putUUID("Id",new UUID(5,6));battle.putInt("Revision",9);battle.putInt("Reveal",1);battle.put("Actors",actors);battle.put("RoundStartActors",actors.copy());
        var frame=new CompoundTag();frame.put("Actors",actors.copy());frame.putInt("Total",19);var frames=new ListTag();frames.add(frame);battle.put("Frames",frames);life.put("Battle",battle);
        LifeSimulationSprites.normalize(life);
        for(var roster:List.of(actors,battle.getList("RoundStartActors",Tag.TAG_COMPOUND),frame.getList("Actors",Tag.TAG_COMPOUND))){
            h.assertTrue(LifeSimulationSprites.FEMALE.contains(roster.getCompound(0).getString("Sprite")),"Female simulated player always uses female sprite");
            h.assertTrue(roster.getCompound(1).getString("Sprite").equals(npc.getString("PortraitSprite"))&&LifeSimulationSprites.FEMALE.contains(npc.getString("PortraitSprite")),"Linked female NPC matches journal during every frame");
            h.assertTrue(LifeSimulationSprites.MALE.contains(roster.getCompound(2).getString("Sprite")),"Unlinked male opponent corrects female cached sprite");
        }
        h.assertTrue(player.getDouble("Hp")==71.25&&enemy.getDouble("Hp")==53.75&&battle.getInt("Revision")==9&&battle.getInt("Reveal")==1&&frame.getInt("Total")==19&&life.getLong("Seed")==381,"Gender migration never changes HP, saved rolls, revision or randomness");
        var saved=life.copy();LifeSimulationSprites.normalize(life);h.assertTrue(life.equals(saved),"Gender migration is idempotent");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void authoredAndArchivedWomenRepairFromStoryMetadata(GameTestHelper h){
        var life=new CompoundTag();life.putString("Name","Old life");
        var npc=new CompoundTag();npc.putString("Name","杜芬");npc.putString("Gender","male");npc.putString("PortraitSprite",LifeSimulationSprites.MALE.getFirst());
        var people=new ListTag();people.add(npc);life.put("Relationships",people);
        var old=life.copy();var memories=new ListTag();memories.add(old);life.put("Memories",memories);
        LifeSimulationStories.normalizeCastGenders(life,h.getLevel().getServer().getResourceManager());LifeSimulationSprites.normalize(life);
        h.assertTrue(npc.getString("Gender").equals("female")&&LifeSimulationSprites.FEMALE.contains(npc.getString("PortraitSprite")),"Authored woman repairs previously inferred male identity");
        h.assertTrue(LifeSimulationSprites.FEMALE.contains(old.getList("Relationships",Tag.TAG_COMPOUND).getCompound(0).getString("PortraitSprite")),"Archived relationship portrait repairs too");
        var mother=new CompoundTag();mother.putString("Relation","mother");mother.putString("PortraitSprite",LifeSimulationSprites.MALE.getFirst());
        h.assertTrue(LifeSimulationSprites.FEMALE.contains(LifeSimulationSprites.person(mother).id()),"Old mother without gender uses female sprite");h.succeed();
    }

    @GameTest(template="fate_test") public static void existingNpcAppearanceGuarantees(GameTestHelper h){LifeSimulationSpriteGameTests.journalAppearanceFollowsNpcAcrossBattleSides(h);}
    @GameTest(template="fate_test") public static void existingLegacyMigrationGuarantees(GameTestHelper h){LifeSimulationSpriteGameTests.legacyBattleMigrationPreservesCombatAndIsIdempotent(h);}
    @GameTest(template="fate_test") public static void existingStoryAnimationGuarantees(GameTestHelper h){LifeSimulationSpriteGameTests.storyEnemiesAndSavedAnimationsUseNewCharacters(h);}
    @GameTest(template="fate_test") public static void existingThirtySpriteCoverage(GameTestHelper h){LifeSimulationSpriteGameTests.allThirtyNewAppearancesAreReachable(h);}
}
