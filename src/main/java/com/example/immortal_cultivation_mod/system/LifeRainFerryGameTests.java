package com.example.immortal_cultivation_mod.system;

import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real simulator commands and card actions, rather than fabricated combat results. */
@GameTestHolder("immortal_cultivation_replacement_stories")
@PrefixGameTestTemplate(false)
public final class LifeRainFerryGameTests {
    private static final RandomSource RNG=RandomSource.create(712);
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static List<CompoundTag> definitions(GameTestHelper h){return LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager());}
    private static CompoundTag rain(GameTestHelper h){return definitions(h).stream().filter(d->d.getString("id").equals("life_event_rain_sword")).findFirst().orElseThrow();}
    private static CompoundTag person(int realm){return LifeReplacementStoryGameTests.person(realm,0);}
    private static CompoundTag state(CompoundTag life){return life.getCompound("Story").getCompound("State");}
    private static String combatNode(CompoundTag definition){return definition.getList("nodes",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
            .filter(n->n.getList("choices",Tag.TAG_COMPOUND).stream().anyMatch(c->((CompoundTag)c).contains("battle")))
            .findFirst().orElseThrow().getString("id");}
    private static void path(CompoundTag life,String path){for(String c:path.split(" "))LifeReplacementStoryGameTests.choose(life,c);}
    private static void action(CompoundTag life,String action){
        var b=life.getCompound("Battle");int revision=b.getInt("Revision");
        LifeSimulationBattle.handle(life,b.getUUID("Id")+":"+revision+":"+action,RNG);
        check(b.getInt("Revision")>revision,"Accepted real card command "+action);
    }
    private static void skip(CompoundTag life){if(LifeSimulationBattle.pending(life.getCompound("Battle")))action(life,"skip");}
    private static void fight(CompoundTag life,boolean attack){
        action(life,"ready");
        for(int steps=0;steps<400&&life.getCompound("Battle").getString("Result").isBlank();steps++){
            skip(life);var b=life.getCompound("Battle");if(!b.getString("Result").isBlank())break;
            var enemy=b.getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
                    .filter(a->a.getBoolean("Enemy")&&LifeCardBattle.active(a)).findFirst().orElseThrow();
            var card=b.getList("Hand",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
                    .filter(c->attack&&LifeCardCatalog.get(life,LifeCardBattle.player(life),c.getString("Card")).effect().equals("attack")
                            &&LifeCardBattle.illegal(life,c.getString("Instance"),enemy.getString("Id")).isBlank()).findFirst();
            if(card.isPresent())action(life,"play:"+card.get().getString("Instance")+":"+enemy.getString("Id"));else action(life,"end");
        }
        skip(life);check(!life.getCompound("Battle").getString("Result").isBlank(),"Battle terminates through real card engine");
    }
    @GameTest(template="fate_test",timeoutTicks=600)
    public static void eachStoryTriggersFromRealExploreMapAndSectActions(GameTestHelper h){
        try(var fixture=new LifeSimulationGameTests.Player(h)){
            fixture.equip(h,OuterSystemType.LIFE_SIMULATION);
            for(var target:definitions(h))for(String command:List.of("activity:explore","map:quest","activity:sect_mission")){
                var life=person(4);life.putUUID("SimulationId",UUID.randomUUID());life.putString("Sect","qingyun");
                var journal=new CompoundTag();for(var d:definitions(h))if(!d.getString("id").equals(target.getString("id"))){var record=new CompoundTag();record.putString("Ending","E4");record.putInt("Realm",life.getInt("Realm"));journal.put(d.getString("id"),record);}
                life.put("StoryJournal",journal);LifeSimulationWorldMap.normalize(life);
                var outer=OuterSystemData.read(fixture.player);outer.put("LifeSimulation",life);OuterSystemData.save(fixture.player,outer);
                LifeSimulationSystem.handleAction(fixture.player,command);
                var actual=OuterSystemData.read(fixture.player).getCompound("LifeSimulation");
                check(actual.getCompound("Story").getString("Id").equals(target.getString("id")),command+" triggers "+target.getString("id"));
                check(actual.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND).size()>=2,"Actual choice payload exists");
                var c=actual.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND).getCompound(0);
                LifeSimulationSystem.handleAction(fixture.player,"choice:"+c.getString("Id"));
                var after=OuterSystemData.read(fixture.player).getCompound("LifeSimulation");
                check(!after.getCompound("Story").getString("Node").equals("S01"),"Uppercase authored choice works through real dispatcher");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=600)
    public static void everyStoryCanTriggerDuringAnOrdinarySimulatedYear(GameTestHelper h){
        try(var fixture=new LifeSimulationGameTests.Player(h)){
            fixture.equip(h,OuterSystemType.LIFE_SIMULATION);
            for(var target:definitions(h)){
                boolean triggered=false;
                for(int seed=0;seed<40&&!triggered;seed++){
                    var life=person(0);life.putUUID("SimulationId",UUID.randomUUID());life.putLong("Seed",seed);
                    if(target.getBoolean("sect"))life.putString("Sect","qingyun");
                    var flags=new ListTag();flags.add(StringTag.valueOf("root_tested"));life.put("Flags",flags);
                    var journal=new CompoundTag();for(var d:definitions(h))if(!d.getString("id").equals(target.getString("id"))){var record=new CompoundTag();record.putString("Ending","E4");record.putInt("Realm",life.getInt("Realm"));journal.put(d.getString("id"),record);}
                    life.put("StoryJournal",journal);
                    var outer=OuterSystemData.read(fixture.player);outer.put("LifeSimulation",life);OuterSystemData.save(fixture.player,outer);
                    LifeSimulationSystem.handleAction(fixture.player,"advance");
                    var actual=OuterSystemData.read(fixture.player).getCompound("LifeSimulation");
                    triggered=actual.getCompound("Story").getString("Id").equals(target.getString("id"));
                }
                check(triggered,"Normal annual simulation can trigger "+target.getString("id"));
            }
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void finalStoryKeepsQuestAvailableAndDeadFerrymanOnlyLeavesRecords(GameTestHelper h){
        var life=person(0);var journal=new CompoundTag();
        for(var d:definitions(h))if(!d.getString("id").equals("life_event_rain_sword"))journal.put(d.getString("id"),new CompoundTag());
        life.put("StoryJournal",journal);check(LifeAuthoredStories.remaining(life),"Quest button remains for fifth story");
        LifeSimulationStories.start(life,rain(h));
        var ferryman=life.getCompound("Story").getCompound("Cast").getUUID("ferryman");
        for(var t:life.getList("Relationships",Tag.TAG_COMPOUND)){var n=(CompoundTag)t;if(n.getUUID("Id").equals(ferryman))n.putBoolean("Alive",false);}
        LifeSimulationStories.openNode(life,"S02");check(life.getCompound("Story").getString("Node").equals("E4"),"Dead ferryman cannot hold new conversations");
        check(life.getCompound("CurrentEvent").getList("TextParts",Tag.TAG_STRING).size()==1,"Only surviving record variant shown");
        path(life,"finish");check(!LifeAuthoredStories.remaining(life),"Completed fifth story closes quest availability");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void mortalFerryAndOwnBoatRoutesTransferExactlyOneExistingSword(GameTestHelper h){
        var d=rain(h);var life=person(0);LifeSimulationStories.start(life,d);path(life,"B C A A A A A A");
        check(life.getCompound("Story").getString("Node").equals("E1"),"Rootless mortal completes safe crossing");
        check(state(life).getBoolean("ferry_dismantled")&&state(life).getBoolean("promise_fulfilled"),"Real ferry cost and promise recorded");
        check(!state(life).getBoolean("own_boat")&&state(life).getInt("HalfDays")>=4,"Preparation/building consume time");
        check(LifeSimulationBattle.itemCount(life,d.getString("sword_item"))==1,"One existing sword transferred");
        LifeSimulationStories.openNode(life,"E1");path(life,"finish");
        check(LifeSimulationBattle.itemCount(life,d.getString("sword_item"))==1&&!LifeSimulationStories.start(life,d),"No replay reward or event reset");
        check(life.getInt("Realm")==0&&life.getString("SpiritualRoot").equals("none")&&life.getList("DiscoveredSpells",Tag.TAG_STRING).isEmpty(),"No fabricated magic");
        life=person(0);LifeSimulationStories.start(life,d);path(life,"B A B B A");
        var choice=LifeSimulationStories.node(d,"S12").getList("choices",Tag.TAG_COMPOUND).getCompound(1);
        check(!LifeSimulationStories.choiceIllegal(life,choice).isBlank(),"Cannot conjure a boat");
        LifeSimulationSystem.addSimulationItem(life,"minecraft:oak_boat",1);path(life,"B");
        check(state(life).getBoolean("own_boat")&&!state(life).getBoolean("ferry_dismantled"),"Owned boat preserves ferry");
        check(LifeSimulationBattle.itemCount(life,"minecraft:oak_boat")==1,"Boat neither consumed nor duplicated");
        check(life.getCompound("RainFerryState").getBoolean("river_protection_ended"),"Ending persists lost river protection");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void agreedReleaseDiffersFromCrossingAndAwardsNoWeapon(GameTestHelper h){
        var life=person(0);LifeSimulationStories.start(life,rain(h));path(life,"B C B B B A B");
        check(life.getCompound("Story").getString("Node").equals("E2"),"Release ending reachable without spirit root");
        check(state(life).getBoolean("promise_replaced_and_discharged")&&!state(life).getBoolean("crossing_completed"),"Mutual release never claims a crossing");
        check(state(life).getBoolean("names_copy")&&state(life).getBoolean("token_to_river")&&!state(life).getBoolean("token_held"),"Evidence and entrusted token settled");
        check(!state(life).getBoolean("sword_transferred")&&!state(life).getBoolean("ferry_dismantled"),"No weapon or rebuilt ferry fabricated");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void insightUsesOneSavedCheckWithoutDamageOrRerolls(GameTestHelper h)throws Exception{
        for(int degree=0;degree<4;degree++){
            var life=person(0);LifeSimulationStories.start(life,rain(h));path(life,"A B");
            var roll=life.getCompound("Story").getCompound("PendingRoll");check(roll.getInt("DC")==11||roll.getInt("Target")==11,"Original insight target");
            roll.putInt("Degree",degree);var copy=life.copy();
            LifeSimulationStories.choose(life,"reveal",RNG);LifeSimulationStories.choose(copy,"reveal",RandomSource.create(999));
            check(life.getCompound("Story").getString("Node").equals(degree>=1?"S04":"S05"),"Saved result branches correctly");
            check(life.getCompound("Story").getString("Node").equals(copy.getCompound("Story").getString("Node")),"Reload cannot reroll");
            var before=life.copy();LifeSimulationStories.choose(life,"reveal",RNG);check(before.equals(life),"Reveal is idempotent");
            check(life.getInt("Health")==100&&life.getInt("TrainingStrain")==0,"Reading failure never damages character");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=600)
    public static void allStoryBattlesReachVictoryThroughActualCardDamage(GameTestHelper h){
        for(var d:definitions(h).stream().filter(d->!d.getBoolean("loot_encounter")).toList()){
            var life=person(10);if(d.getBoolean("sect"))life.putString("Sect","qingyun");
            LifeSimulationStories.start(life,d);LifeSimulationStories.openNode(life,combatNode(d));path(life,"A");
            check(LifeCardBattle.enabled(life.getCompound("Battle")),"Actual card preparation "+d.getString("id"));
            if(d.getString("id").equals("life_event_rain_sword")){
                var b=life.getCompound("Battle");check(b.getList("Actors",Tag.TAG_COMPOUND).size()==2&&!b.getBoolean("ObjectiveEnabled"),"Only witness and sword-remnant, no passenger targets");
                var enemy=LifeSimulationBattle.find(b,"enemy1");check(enemy.getInt("Realm")==3&&enemy.getList("Moves",Tag.TAG_COMPOUND).size()==3,"Original remnant realm and three moves");
            }
            fight(life,true);check(life.getCompound("Battle").getString("Result").equals("victory"),"Real combat victory "+d.getString("id"));
            LifeSimulationStories.finishBattle(life);check(!life.contains("Battle")&&!life.getCompound("CurrentEvent").isEmpty(),"Victory resumes playable story");
            if(d.getString("id").equals("life_event_missing_disciple")) {
                check(life.getCompound("Story").getString("Node").equals("S10")&&!state(life).getBoolean("lin_freed"),"Guard victory opens passage without freeing Lin");
                path(life,"A");check(state(life).getBoolean("lin_freed"),"Separate steward shutdown releases Lin");
            }
            if(d.getString("id").equals("life_event_rain_sword")){
                check(life.getCompound("Story").getString("Node").equals("S11")&&state(life).getBoolean("remnant_weakened"),"Victory is not crossing completion");
                path(life,"A");check(life.getCompound("Story").getString("Node").equals("E3")&&state(life).getBoolean("unresolved_oath"),"Actual victory permits unsettled sword ending");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=600)
    public static void allStoryDefeatsUseActualEnemyTurnsAndKeepKnockout(GameTestHelper h){
        for(var d:definitions(h).stream().filter(d->!d.getBoolean("loot_encounter")).toList()){
            var life=person(1);if(d.getBoolean("sect"))life.putString("Sect","qingyun");
            LifeSimulationStories.start(life,d);LifeSimulationStories.openNode(life,combatNode(d));path(life,"A");
            fight(life,false);check(life.getCompound("Battle").getString("Result").equals("defeat"),"Enemy actions produce real defeat "+d.getString("id"));
            int streak=life.getInt("KnockoutStreak");check(streak==1&&LifeCardInjuries.hp(life)==0,"One real knockout, no free healing");
            LifeSimulationStories.finishBattle(life);check(life.getCompound("Story").getString("Node").equals("E4"),"Unconscious gets recorded aftermath");
            check(life.getBoolean("Alive")&&life.getInt("KnockoutStreak")==streak,"No extra narrative death or knockout");path(life,"finish");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=600)
    public static void retreatRollFailureStaysInBattleAndSuccessResumesStory(GameTestHelper h){
        boolean success=false,failure=false;
        for(int seed=0;seed<100&&!(success&&failure);seed++){
            var life=person(10);LifeSimulationStories.start(life,rain(h));LifeSimulationStories.openNode(life,"S10");path(life,"A");
            var b=life.getCompound("Battle");LifeSimulationBattle.handle(life,b.getUUID("Id")+":"+b.getInt("Revision")+":ready",RandomSource.create(seed));
            var run=b.getList("Hand",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(c->c.getString("Card").equals("run")).findFirst();if(run.isEmpty())continue;
            String payload=b.getUUID("Id")+":"+b.getInt("Revision")+":play:"+run.get().getString("Instance")+":player";
            LifeSimulationBattle.handle(life,payload,RandomSource.create(seed));var saved=life.copy();LifeSimulationBattle.handle(life,payload,RandomSource.create(999));
            check(life.equals(saved),"Retreat packet cannot reroll saved outcome");
            if(b.getString("Result").equals("escape")){success=true;skip(life);LifeSimulationStories.finishBattle(life);check(life.getCompound("Story").getString("Node").equals("S15"),"Successful retreat returns to account");}
            else{failure=true;check(b.getString("Result").isBlank()&&life.getCompound("Story").getString("Node").equals("S10"),"Failed retreat stays in current battle");}
        }
        check(success&&failure,"Both retreat outcomes exercised");h.succeed();
    }
}
