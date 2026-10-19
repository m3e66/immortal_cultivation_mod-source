package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_life_simulation")
@PrefixGameTestTemplate(false)
public final class LifeExpansionGameTests {
    @GameTest(template="fate_test")
    public static void plannerSharesEveryActivityAndCurrentAvailability(GameTestHelper h){
        var state=life(0);state.putInt("Age",8);state.putString("Sect","none");state.remove("ActiveMethod");
        h.assertTrue(LifeSimulationSystem.ACTIVITIES.size()==14&&LifeSimulationSystem.ACTIVITIES.containsAll(java.util.List.of("exercise","family","keep_oath")),"Planner contains all fourteen manual activities");
        h.assertTrue(LifeSimulationSystem.ROUTINE_CONDITIONS.contains("none"),"None is a supported no-condition option");
        for(String action:java.util.List.of("study","exercise","family","rest","socialize","explore","consolidate"))h.assertTrue(LifeSimulationSystem.activityUnavailable(state,action).isBlank(),"Accessible activity remains available: "+action);
        for(String action:java.util.List.of("cultivate","breathing","practice","seclusion"))h.assertTrue(LifeSimulationSystem.activityUnavailable(state,action).equals("awakening"),"Realm-dependent activity is disabled before awakening");
        h.assertTrue(LifeSimulationSystem.activityUnavailable(state,"work").equals("work_age")&&LifeSimulationSystem.activityUnavailable(state,"sect_mission").equals("sect")&&LifeSimulationSystem.activityUnavailable(state,"keep_oath").equals("oath"),"Age, sect and oath restrictions explain disabled choices");
        state.putInt("Realm",4);state.putString("ActiveMethod",CultivationMethods.BASIC_BREATHING);state.putBoolean("SeclusionThisYear",true);
        h.assertTrue(LifeSimulationSystem.activityUnavailable(state,"seclusion").equals("seclusion_once"),"Already-used seclusion is disabled");state.putBoolean("SeclusionThisYear",false);state.putInt("AnnualActionPoints",1);
        h.assertTrue(LifeSimulationSystem.activityUnavailable(state,"seclusion").equals("seclusion_actions"),"Two-action cost is validated");h.succeed();
    }
    private static CompoundTag life(int realm) {
        CompoundTag life=new CompoundTag();
        life.putString("Name","test"); life.putBoolean("Alive",true); life.putBoolean("Active",true);
        life.putInt("Realm",realm); life.putInt("HighestRealm",realm); life.putInt("Age",40);
        life.putInt("Health",100); life.putInt("Mentality",70); life.putInt("Comprehension",70);
        life.putInt("Charisma",70); life.putInt("Combat",60); life.putInt("Alchemy",60);
        life.putInt("AnnualActionPoints",3); life.putInt("SpiritualRootPotential",70);
        life.putString("Sect","sect"); life.putString("ActiveMethod",CultivationMethods.BASIC_BREATHING);
        life.putLong("Wealth",10_000); life.put("Relationships",new ListTag()); life.put("Inventory",new ListTag());
        life.put("History",new ListTag()); return life;
    }
    private static CompoundTag battleDefinition(int realm,String profile) {
        CompoundTag d=new CompoundTag(); d.putString("arena","temple"); d.putInt("goal",3);d.putInt("limit",8);
        d.putString("stakes","retreat"); ListTag enemies=new ListTag(); CompoundTag enemy=new CompoundTag();
        enemy.putString("name_key","life.battle.title");enemy.putInt("realm",realm);enemy.putString("profile",profile); enemies.add(enemy);d.put("enemies",enemies);return d;
    }
    private static String payload(CompoundTag life,String action) {
        CompoundTag battle=life.getCompound("Battle");return battle.getUUID("Id")+":"+battle.getInt("Revision")+":"+action;
    }
    private static void reveal(CompoundTag life) {
        int limit=20;
        while(LifeSimulationBattle.pending(life.getCompound("Battle"))&&limit-->0) LifeSimulationBattle.handle(life,payload(life,"reveal"),RandomSource.create(1));
    }

    private static CompoundTag startAuthoredBattle(GameTestHelper helper, CompoundTag state, String storyId, String choiceId) {
        var definition=LifeSimulationStories.definitions(helper.getLevel().getServer().getResourceManager()).stream()
                .filter(d->d.getString("id").equals(storyId)).findFirst().orElseThrow();
        LifeSimulationStories.start(state,definition);LifeSimulationStories.openNode(state,"decision");
        String packet="";for(var tag:state.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND))
            if(((CompoundTag)tag).getString("id").equals(choiceId))packet=((CompoundTag)tag).getString("Id");
        LifeSimulationStories.choose(state,packet,RandomSource.create(1));
        // These tests exercise the still-supported saved command-battle format.
        // New card battles and authored story integration have their own acceptance suite.
        state.getCompound("Battle").remove("CardBattleVersion");
        helper.assertTrue(LifeSimulationBattle.active(state),"The authored choice starts its actual playable encounter");return state;
    }

    private static CompoundTag successfulObjective(GameTestHelper helper,CompoundTag state,String target) {
        int objective=state.getCompound("Battle").getInt("Objective"),phase=state.getCompound("Battle").getInt("Phase");
        for(int seed=0;seed<200;seed++) {
            var trial=state.copy();LifeSimulationBattle.handle(trial,payload(trial,"plan:objective:"+target),RandomSource.create(seed));
            if(trial.getCompound("Battle").getInt("Objective")>objective||trial.getCompound("Battle").getInt("Phase")>phase) return trial;
        }
        helper.fail("Expected at least one successful legal objective check in controlled fixture seeds");return state;
    }
    @GameTest(template="fate_test")
    public static void diceAreBoundedAndAdvantageCancels(GameTestHelper h) {
        h.assertTrue(LifeSimulationDice.attribute(0)==-4&&LifeSimulationDice.attribute(50)==0&&LifeSimulationDice.attribute(100)==5,"Bounded attribute modifiers");
        RandomSource random=RandomSource.create(170);
        for(int i=0;i<500;i++) {
            var advantage=LifeSimulationDice.check(random,"test",0,14,true,false);
            int[] dice=advantage.getIntArray("Dice");
            h.assertTrue(dice.length==2&&advantage.getInt("Selected")==Math.max(dice[0],dice[1]),"Advantage chooses higher d20");
            var cancelled=LifeSimulationDice.check(random,"test",0,14,true,true);
            h.assertTrue(cancelled.getIntArray("Dice").length==1,"Opposed advantage cancels");
            var damage=LifeSimulationDice.damage(random,2,6);
            h.assertTrue(damage.getDouble("Factor")>=.75&&damage.getDouble("Factor")<=1.25,"2d6 factor stays bounded");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void staleRoundCannotRerollOrConsumeTwice(GameTestHelper h) {
        var life=life(4); LifeSimulationBattle.startLegacy(life,battleDefinition(4,"aggressive"),RandomSource.create(4));
        String action=payload(life,"plan:heavy:enemy1");
        LifeSimulationBattle.handle(life,action,RandomSource.create(5));
        var committed=life.copy(); LifeSimulationBattle.handle(life,action,RandomSource.create(6));
        h.assertTrue(life.equals(committed),"Duplicate packet cannot replay damage, Qi, rolls or reservations");
        var restored=life.copy(); reveal(restored);
        h.assertTrue(LifeSimulationBattle.find(restored.getCompound("Battle"),"player").getDouble("Qi")==LifeSimulationBattle.find(committed.getCompound("Battle"),"player").getDouble("Qi"),"Reconnect reveals saved frames without re-paying Qi");
        h.assertTrue(!LifeSimulationBattle.illegal(restored,"heavy","enemy1").isBlank(),"Cooldown survives the reveal round");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void skippingAnimationsPreservesCommittedCombat(GameTestHelper h) {
        var life=life(4); LifeSimulationBattle.startLegacy(life,battleDefinition(4,"defensive"),RandomSource.create(4));
        LifeSimulationBattle.handle(life,payload(life,"plan:heavy:enemy1"),RandomSource.create(5));
        var actors=life.getCompound("Battle").getList("Actors",Tag.TAG_COMPOUND).copy();
        var frames=life.getCompound("Battle").getList("Frames",Tag.TAG_COMPOUND).copy();
        var rolls=life.getList("DiceHistory",Tag.TAG_COMPOUND).copy();
        String skip=payload(life,"skip");
        LifeSimulationBattle.handle(life,skip,RandomSource.create(99));
        h.assertTrue(!LifeSimulationBattle.pending(life.getCompound("Battle")),"Skip reveals the saved round");
        h.assertTrue(life.getCompound("Battle").getList("Frames",Tag.TAG_COMPOUND).equals(frames),"Skip never replays damage frames");
        h.assertTrue(life.getList("DiceHistory",Tag.TAG_COMPOUND).equals(rolls),"Skip never rerolls combat");
        for(var tag:actors) {
            var actor=(CompoundTag)tag;var after=LifeSimulationBattle.find(life.getCompound("Battle"),actor.getString("Id"));
            h.assertTrue(after.getDouble("Hp")==actor.getDouble("Hp")&&after.getDouble("Qi")==actor.getDouble("Qi"),"Skip cannot alter committed HP or Qi");
        }
        var complete=life.copy();LifeSimulationBattle.handle(life,skip,RandomSource.create(100));
        h.assertTrue(life.equals(complete),"Duplicate skip cannot advance another round");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void authoredEnemyMoveUsesItsOwnNameAndEffect(GameTestHelper h) {
        var life=life(4);var definition=battleDefinition(4,"defensive");
        var move=new CompoundTag();move.putString("id","test_ward");move.putString("kind","shield");
        move.putString("name_key","life.story.rain_sword.move.shield");move.putString("element","physical");
        move.putString("effect","shield");move.putDouble("qi_fraction",.08);move.putInt("cooldown",2);
        var moves=new ListTag();moves.add(move);definition.getList("enemies",Tag.TAG_COMPOUND).getCompound(0).put("moves",moves);
        LifeSimulationBattle.startLegacy(life,definition,RandomSource.create(1));
        var enemy=LifeSimulationBattle.find(life.getCompound("Battle"),"enemy1");
        h.assertTrue(enemy.getString("Plan").equals("test_ward"),"Enemy commits its authored move before the player's action");
        h.assertTrue(LifeSimulationBattle.actorMoves(life,enemy).stream().anyMatch(m->m.id().equals("test_ward")&&m.name().equals(move.getString("name_key"))),"Saved custom move retains bilingual name key");
        LifeSimulationBattle.handle(life,payload(life,"plan:guard:player"),RandomSource.create(12));
        h.assertTrue(enemy.getDouble("Shield")>0&&enemy.getDouble("Qi")<enemy.getDouble("MaxQi"),"Authored ward applies shield and pays Qi once");
        h.assertTrue(LifeSimulationBattle.find(life.getCompound("Battle"),"player").getDouble("Shield")==0,"Authored enemy ward cannot shield the opponent");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void defensiveEnemyShieldsItself(GameTestHelper h) {
        var life=life(4); LifeSimulationBattle.startLegacy(life,battleDefinition(4,"defensive"),RandomSource.create(1));
        LifeSimulationBattle.handle(life,payload(life,"plan:guard:player"),RandomSource.create(12));
        var battle=life.getCompound("Battle");
        h.assertTrue(LifeSimulationBattle.find(battle,"enemy1").getDouble("Shield")>0,"Enemy shield applies to enemy");
        h.assertTrue(LifeSimulationBattle.find(battle,"player").getDouble("Shield")==0,"Enemy cannot accidentally shield the player");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void namespacedBagItemsConsumeOnce(GameTestHelper h) {
        var life=life(4); CompoundTag item=new CompoundTag();item.putString("Id","immortal_cultivation_mod:low_qi_recovery_pill");item.putInt("Count",2);life.getList("Inventory",Tag.TAG_COMPOUND).add(item);
        LifeSimulationBattle.startLegacy(life,battleDefinition(4,"defensive"),RandomSource.create(1));
        LifeSimulationBattle.find(life.getCompound("Battle"),"player").putDouble("Qi",0);
        String action=payload(life,"plan:bag_immortal_cultivation_mod~low_qi_recovery_pill:player");
        LifeSimulationBattle.handle(life,action,RandomSource.create(9));LifeSimulationBattle.handle(life,action,RandomSource.create(99));
        h.assertTrue(item.getInt("Count")==1,"Namespace-safe action uses one simulated pill");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void criticalCannotBypassOverwhelmingRealm(GameTestHelper h) {
        var life=life(1);LifeSimulationBattle.startLegacy(life,battleDefinition(10,"defensive"),RandomSource.create(1));
        var b=life.getCompound("Battle");var a=LifeSimulationBattle.find(b,"player");var enemy=LifeSimulationBattle.find(b,"enemy1");
        for(var move:LifeSimulationBattle.moves(life))h.assertTrue(LifeSimulationBattle.damage(a,enemy,move,1.25,true)==0,"Even a natural critical cannot ignore two-major-realm gap");
        h.assertTrue(LifeSimulationBattle.preview(life,LifeSimulationBattle.BASE_MOVES.getFirst(),"enemy1").getDouble("Hit")==0,"Preview reflects impossible attack");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void allTwentyStoriesAndEndingsPersist(GameTestHelper h) {
        var definitions=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager());
        h.assertTrue(definitions.size()==20,"Twenty complete stories are loaded on the dedicated server");
        for(var definition:definitions)for(String ending:new String[]{"a","b","c","withdraw"}) {
            var life=life(Math.max(10,definition.getInt("minimum_realm")));
            if(definition.getString("trigger").equals("rival")||definition.getString("trigger").equals("friend")) {
                var npc=new CompoundTag();npc.putUUID("Id",UUID.randomUUID());npc.putBoolean("Alive",true);npc.putBoolean("Friend",true);npc.putBoolean("Nemesis",true);npc.putString("Grievance","history");npc.putInt("Trust",60);npc.putInt("Affinity",60);life.getList("Relationships",Tag.TAG_COMPOUND).add(npc);
            }
            h.assertTrue(LifeSimulationStories.start(life,definition),"Start story "+definition.getString("id"));
            for(String node:new String[]{"start","evidence","decision",ending}) {
                LifeSimulationStories.openNode(life,node);
                var event=life.getCompound("CurrentEvent");
                h.assertTrue(!event.getString("TextKey").isBlank()&&!event.getList("Choices",Tag.TAG_COMPOUND).isEmpty(),"Connected scene has narrative and usable choices");
            }
            LifeSimulationStories.complete(life,ending);
            var completed=life.copy();LifeSimulationStories.complete(life,ending);
            h.assertTrue(life.equals(completed),"Ending rewards remain idempotent");
            var record=life.getCompound("StoryJournal").getCompound(definition.getString("id"));
            h.assertTrue(record.getString("Ending").equals(ending)&&record.getBoolean("Rewarded")==!ending.equals("withdraw"),"Ending/withdrawal is persisted");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void checksPauseWithoutRerollsAndFailuresContinue(GameTestHelper h) {
        var d=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager()).stream().filter(s->s.getString("id").equals("life_event_rain_sword")).findFirst().orElseThrow();
        for(int degree=0;degree<4;degree++) {
            var life=life(4);LifeSimulationStories.start(life,d);LifeSimulationStories.openNode(life,"evidence");
            String choice=life.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND).getCompound(0).getString("Id");
            LifeSimulationStories.choose(life,choice,RandomSource.create(4));var pending=life.copy();
            LifeSimulationStories.choose(life,choice,RandomSource.create(7));h.assertTrue(life.equals(pending),"Pending roll cannot be re-selected");
            life.getCompound("Story").getCompound("PendingRoll").putInt("Degree",degree);
            LifeSimulationStories.choose(life,"reveal",RandomSource.create(1));
            h.assertTrue(life.getCompound("Story").getString("Node").equals("decision"),"Every evidence outcome progresses, including failure");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void deadCastNeverBecomesAllyAndFollowupIsMemorial(GameTestHelper h) {
        var life=life(7);var npc=new CompoundTag();npc.putUUID("Id",UUID.randomUUID());npc.putBoolean("Alive",false);npc.putBoolean("Friend",true);npc.putInt("Trust",100);npc.putInt("Affinity",100);
        life.getList("Relationships",Tag.TAG_COMPOUND).add(npc);
        h.assertTrue(!LifeSimulationBattle.eligibleAlly(npc)&&LifeSimulationStories.livingRelation(life,false)==null,"Dead people cannot support or be cast as living friends");
        var follow=new CompoundTag();follow.putInt("Year",39);follow.putString("TextKey","life.story.rain_sword.a.later");var cast=new CompoundTag();cast.putUUID("lead",npc.getUUID("Id"));follow.put("Cast",cast);var delayed=new ListTag();delayed.add(follow);life.put("DelayedStories",delayed);
        LifeSimulationStories.advanceYear(life);int history=life.getList("History",Tag.TAG_COMPOUND).size();LifeSimulationStories.advanceYear(life);
        h.assertTrue(history==1&&life.getList("History",Tag.TAG_COMPOUND).size()==1,"Followup delivered once");
        h.assertTrue(life.getList("History",Tag.TAG_COMPOUND).getCompound(0).getString("Key").equals("life.expansion.memorial_followup"),"Dead cast communicates through memorial, not resurrection");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void trainingNeverScalesWithCurrentRequirement(GameTestHelper h) {
        var life=life(1);long early=LifeSimulationBalance.gain(life,1);life.putInt("Realm",4);long later=LifeSimulationBalance.gain(life,1);
        h.assertTrue(early==later,"Same method and preparation yield raw gain independent of realm requirement");
        life.putInt("Realm",20);h.assertTrue(LifeSimulationBalance.gain(life,1)==0,"Low method cannot support arbitrary high realms");
        life.putInt("Realm",7);life.putBoolean("MethodContinuation",true);life.putInt("MethodContinuationLimit",9);h.assertTrue(LifeSimulationBalance.gain(life,1)>0,"Explicit compatible continuation unlocks its limit");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void dangerousStoriesRequireTheirRecordedCause(GameTestHelper h) {
        var definitions=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager());
        var state=life(6);state.putBoolean("MethodContinuation",true);state.putInt("MethodContinuationLimit",9);
        var storm=definitions.stream().filter(d->d.getString("trigger").equals("breakthrough")).findFirst().orElseThrow();
        h.assertTrue(!LifeSimulationStories.eligible(state,storm),"Random danger cannot summon a breakthrough before cultivation is ready");
        state.putLong("CultivationProgress",Long.MAX_VALUE);
        h.assertTrue(LifeSimulationStories.eligible(state,storm),"Prepared major breakthrough is eligible");
        for(String trigger:new String[]{"war","oath","pill","dynasty","vein","rival","friend"}) {
            var d=definitions.stream().filter(s->s.getString("trigger").equals(trigger)).findFirst().orElseThrow();
            state.putInt("Realm",Math.max(10,d.getInt("minimum_realm")));
            h.assertTrue(!LifeSimulationStories.eligible(state,d),"Unrecorded cause cannot start danger: "+trigger);
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void linkedRivalUsesExistingRealmAndCannotJoinBothTeams(GameTestHelper h) {
        var state=life(4);var npc=new CompoundTag();UUID id=UUID.randomUUID();npc.putUUID("Id",id);npc.putBoolean("Alive",true);npc.putBoolean("Friend",true);npc.putInt("Trust",60);npc.putInt("Affinity",60);npc.putInt("Age",30);npc.putInt("Realm",1);state.getList("Relationships",Tag.TAG_COMPOUND).add(npc);
        var story=new CompoundTag();var cast=new CompoundTag();cast.putUUID("lead",id);story.put("Cast",cast);state.put("Story",story);
        var definition=battleDefinition(15,"assassin");definition.getList("enemies",Tag.TAG_COMPOUND).getCompound(0).putString("cast_role","lead");
        LifeSimulationBattle.startLegacy(state,definition,RandomSource.create(1));var b=state.getCompound("Battle");
        h.assertTrue(LifeSimulationBattle.find(b,"enemy1").getInt("Realm")==1&&b.getList("Actors",Tag.TAG_COMPOUND).size()==2,"Stored rival's actual realm is respected and identity appears on one team only");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void ordinaryDuelCannotBeWonByInventingAnObjective(GameTestHelper h) {
        var state=life(1);LifeSimulationBattle.startLegacy(state,battleDefinition(15,"defensive"),RandomSource.create(1));
        var before=state.copy();
        LifeSimulationBattle.handle(state,payload(state,"plan:objective:enemy1"),RandomSource.create(2));
        h.assertTrue(state.equals(before),"An ordinary duel cannot bypass an overwhelming opponent through an unauthored objective");
        var objective=battleDefinition(1,"formation");objective.putBoolean("objective_enabled",true);
        var prepared=life(1);LifeSimulationBattle.startLegacy(prepared,objective,RandomSource.create(3));
        h.assertTrue(LifeSimulationBattle.supportsObjective(prepared.getCompound("Battle")),"An explicitly authored formation objective remains available");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void withdrawnAlliesAreOutsideAreaAttacks(GameTestHelper h) {
        var state=life(4);var npc=new CompoundTag();UUID id=UUID.randomUUID();
        npc.putUUID("Id",id);npc.putString("Name","ally");npc.putBoolean("Alive",true);npc.putBoolean("Friend",true);
        npc.putInt("Trust",60);npc.putInt("Affinity",60);npc.putInt("Age",30);npc.putInt("Realm",4);state.getList("Relationships",Tag.TAG_COMPOUND).add(npc);
        var definition=battleDefinition(4,"aggressive");var moves=new ListTag();var move=new CompoundTag();
        move.putString("id","area_test");move.putString("kind","strike");move.putString("name_key","life.battle.move.strike");
        move.putString("element","physical");move.putDouble("potency",1);move.putBoolean("area",true);moves.add(move);
        definition.getList("enemies",Tag.TAG_COMPOUND).getCompound(0).put("moves",moves);
        LifeSimulationBattle.startLegacy(state,definition,RandomSource.create(1));var battle=state.getCompound("Battle");
        var ally=LifeSimulationBattle.find(battle,id.toString());double health=ally.getDouble("Hp");
        LifeSimulationBattle.handle(state,payload(state,"ally:"+id+":withdraw"),RandomSource.create(2));
        LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(3));
        h.assertTrue(ally.getDouble("Hp")==health,"Retreated ally takes no area damage");
        for(var frameTag:battle.getList("Frames",Tag.TAG_COMPOUND))for(var hitTag:((CompoundTag)frameTag).getList("Hits",Tag.TAG_COMPOUND))
            h.assertTrue(!((CompoundTag)hitTag).getString("Target").equals(id.toString()),"Area attack does not roll against a withdrawn ally");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void enemyPlanCannotUseATechniqueStillOnCooldown(GameTestHelper h) {
        var state=life(4);var definition=battleDefinition(4,"defensive");var moves=new ListTag();var move=new CompoundTag();
        move.putString("id","long_ward");move.putString("kind","shield");move.putString("name_key","life.battle.move.shield");
        move.putString("element","physical");move.putString("effect","shield");move.putDouble("qi_fraction",.08);move.putInt("cooldown",10);moves.add(move);
        definition.getList("enemies",Tag.TAG_COMPOUND).getCompound(0).put("moves",moves);
        LifeSimulationBattle.startLegacy(state,definition,RandomSource.create(1));var battle=state.getCompound("Battle");
        var player=LifeSimulationBattle.find(battle,"player");player.putDouble("MaxHp",1_000_000);player.putDouble("Hp",1_000_000);
        for(int round=1;round<=3;round++) {
            LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(20+round));reveal(state);
        }
        var enemy=LifeSimulationBattle.find(battle,"enemy1");
        h.assertTrue(battle.getInt("Round")==4&&enemy.getCompound("Cooldowns").getInt("long_ward")>4,"Long ward cooldown persists across rounds");
        h.assertTrue(enemy.getString("Plan").equals("strike"),"Enemy chooses a legal fallback instead of reusing its ward early");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void abandoningBattleDoesNotGrantAnotherEndingReward(GameTestHelper h) {
        var definitions=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager());
        for(String result:new String[]{"escape","surrender"}) {
            var definition=definitions.stream().filter(d->d.getString("id").equals("life_danger_tribulation")).findFirst().orElseThrow();
            var state=life(6);LifeSimulationStories.start(state,definition);var encounter=battleDefinition(6,"disaster");
            encounter.putString("win","a");encounter.putString("loss","c");encounter.putString("stakes","lethal");
            LifeSimulationBattle.startLegacy(state,encounter,RandomSource.create(1));state.getCompound("Battle").putString("Result",result);
            LifeSimulationStories.finishBattle(state);
            h.assertTrue(state.getCompound("Story").getString("Node").equals("withdraw"),"Abandonment records withdrawal instead of a favorable alternate ending");
            LifeSimulationStories.complete(state,"withdraw");
            h.assertTrue(!state.getCompound("StoryJournal").getCompound(definition.getString("id")).getBoolean("Rewarded")&&state.getInt("DangersSurvived")==0,"Abandoning danger grants no ending or survived-danger reward");
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void storyResourceFailuresAreVisibleAndNeverCharge(GameTestHelper h) {
        var definition=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager()).stream()
                .filter(d->d.getString("id").equals("life_event_borrowed_faces")).findFirst().orElseThrow();
        var state=life(4);state.putLong("Wealth",0);LifeSimulationStories.start(state,definition);LifeSimulationStories.openNode(state,"decision");
        CompoundTag option=null;for(var tag:state.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND))
            if(((CompoundTag)tag).getString("id").equals("alternative"))option=(CompoundTag)tag;
        h.assertTrue(option!=null&&option.getString("Illegal").equals("wealth")&&LifeSimulationStories.choiceIllegal(state,option).equals("wealth"),"Unaffordable story choice exposes a resource reason to the GUI");
        var before=state.copy();LifeSimulationStories.choose(state,option.getString("Id"),RandomSource.create(1));
        h.assertTrue(state.equals(before),"Rejected unaffordable choice cannot alter resources, rolls or story position");
        var oath=new CompoundTag();oath.putBoolean("Accepted",true);oath.putBoolean("Broken",true);oath.putInt("Deadline",10);state.put("SpiritualOath",oath);
        LifeSimulationStories.effect(state,"clear:OathBroken");LifeSimulationStories.advanceYear(state);
        h.assertTrue(oath.getBoolean("Fulfilled")&&!oath.getBoolean("Broken"),"Resolved oath remains resolved after its old deadline");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void storyConsequencesRemainAtTheirSavedSourceLocation(GameTestHelper h) {
        var state=life(4);LifeSimulationWorldMap.initialize(state);var map=LifeSimulationWorldMap.state(state);
        String source=map.getString(LifeSimulationWorldMap.CURRENT_LOCATION);
        var definition=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager()).stream()
                .filter(d->d.getString("id").equals("life_event_flooded_mine")).findFirst().orElseThrow();
        LifeSimulationStories.start(state,definition);
        LifeSimulationWorldMap.state(state).putString(LifeSimulationWorldMap.CURRENT_LOCATION,"yunhai_market");
        LifeSimulationStories.complete(state,"b");
        var closed=LifeSimulationWorldMap.locationState(LifeSimulationWorldMap.state(state),source);
        h.assertTrue(closed.getCompound("StorySites").getString("mine").equals("closed")&&!closed.getBoolean("MineAccessible"),"Closing a mine updates its original region");
        h.assertTrue(LifeSimulationWorldMap.locationState(LifeSimulationWorldMap.state(state),"yunhai_market").getCompound("StorySites").isEmpty(),"Travelling before an ending cannot damage the new destination instead");
        var persisted=state.copy();for(int year=0;year<20;year++)LifeSimulationWorldMap.advanceYear(persisted,RandomSource.create(year));
        h.assertTrue(LifeSimulationWorldMap.locationState(LifeSimulationWorldMap.state(persisted),source).getCompound("StorySites").getString("mine").equals("closed"),"Closed site remains closed through annual map events and save/load copies");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void governanceAndAnchorUpkeepApplyOncePerYear(GameTestHelper h) {
        var state=life(10);LifeSimulationWorldMap.initialize(state);String source=LifeSimulationWorldMap.state(state).getString(LifeSimulationWorldMap.CURRENT_LOCATION);
        LifeSimulationWorldMap.applyStoryEffect(state,source,"sect:anchor_upkeep");
        LifeSimulationWorldMap.applyStoryEffect(state,source,"government:governor");
        long wealth=state.getLong("Wealth");state.putInt("Age",41);
        LifeSimulationWorldMap.advanceYear(state,RandomSource.create(1));
        h.assertTrue(state.getLong("Wealth")==wealth-50,"Known annual obligations use only simulated wealth");
        LifeSimulationWorldMap.advanceYear(state,RandomSource.create(2));
        h.assertTrue(state.getLong("Wealth")==wealth-50,"Repeated processing at the same age cannot charge obligations twice");
        state.putLong("Wealth",0);state.putInt("Age",42);
        int danger=LifeSimulationWorldMap.locationState(LifeSimulationWorldMap.state(state),source).getInt("Danger");
        LifeSimulationWorldMap.advanceYear(state,RandomSource.create(3));
        h.assertTrue(LifeSimulationWorldMap.locationState(LifeSimulationWorldMap.state(state),source).getInt("Danger")>danger,"Unpaid obligations have a bounded local consequence");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void oldPendingStoryRulesMigrateWithoutReplayingCombat(GameTestHelper h) {
        var resources=h.getLevel().getServer().getResourceManager();
        var current=LifeSimulationStories.definitions(resources).stream().filter(d->d.getString("id").equals("life_danger_dynasty_end")).findFirst().orElseThrow();
        var old=current.copy();old.putInt("version",0);
        for(var tag:LifeSimulationStories.node(old,"decision").getList("choices",Tag.TAG_COMPOUND))if(((CompoundTag)tag).getString("id").equals("resolve"))((CompoundTag)tag).putString("next","a");
        var state=life(10);LifeSimulationStories.start(state,old);LifeSimulationStories.openNode(state,"decision");
        String choice=state.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND).getCompound(0).getString("Id");
        LifeSimulationStories.choose(state,choice,RandomSource.create(1));var story=state.getCompound("Story");UUID instance=story.getUUID("Instance");
        var actors=state.getCompound("Battle").getList("Actors",Tag.TAG_COMPOUND).copy();
        LifeSimulationStories.upgradePendingDefinition(state,resources);
        h.assertTrue(story.getUUID("Instance").equals(instance)&&story.getCompound("Definition").getInt("version")==4,"Migration retains the existing event identity");
        h.assertTrue(state.getCompound("Battle").getString("NextWin").equals("b")&&state.getCompound("Battle").getList("Actors",Tag.TAG_COMPOUND).equals(actors),"Force route ends in force outcome without resetting actors, HP, Qi or combat");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void inspectedFormationAnchorsHaveIndependentTargetsAndSafeOrder(GameTestHelper h) {
        var state=startAuthoredBattle(h,life(15),"life_event_missing_disciple","shutdown");var battle=state.getCompound("Battle");
        h.assertTrue(battle.getInt("AnchorCount")==3&&LifeSimulationBattle.find(battle,"anchor1").getString("Kind").equals("anchor"),"Feeding seals are actual separately targetable actors");
        var unchanged=state.copy();LifeSimulationBattle.handle(state,payload(state,"plan:objective:anchor1"),RandomSource.create(1));
        h.assertTrue(state.equals(unchanged)&&LifeSimulationBattle.objectiveIllegal(state,"anchor1").equals("inspect"),"Uninspected shutdown is rejected before costs or dice");
        LifeSimulationBattle.handle(state,payload(state,"plan:inspect:anchor1"),RandomSource.create(2));reveal(state);
        h.assertTrue(LifeSimulationBattle.objectiveIllegal(state,"anchor0").equals("order")&&LifeSimulationBattle.objectiveIllegal(state,"anchor1").isBlank(),"Inspection exposes an authoritative nontrivial shutdown order");
        LifeSimulationBattle.find(battle,"enemy1").putDouble("Hp",0);
        LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(3));
        h.assertTrue(!battle.getString("Result").equals("victory"),"Defeating an enforcer does not automatically stop the living formation");reveal(state);
        state=successfulObjective(h,state,"anchor1");
        h.assertTrue(LifeSimulationBattle.find(state.getCompound("Battle"),"anchor1").getBoolean("Resolved")&&!LifeSimulationBattle.find(state.getCompound("Battle"),"anchor0").getBoolean("Resolved"),"One seal can be shut down without silently resolving another");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void tribulationRequiresThreePhasesAndSavedFinalWillCheck(GameTestHelper h) {
        var state=life(6);state.putInt("Mentality",100);state.putInt("Comprehension",100);state.putInt("Alchemy",100);state.putInt("Combat",100);
        state=startAuthoredBattle(h,state,"life_danger_tribulation","resolve");
        state.getCompound("Story").putBoolean("Prepared",true);
        LifeSimulationBattle.find(state.getCompound("Battle"),"enemy1").putDouble("Power",0);
        h.assertTrue(LifeSimulationBattle.objectiveIllegal(state,"anchor1").equals("stage"),"Meridians cannot be completed before the body phase");
        for(int phase=0;phase<3;phase++) {
            state=successfulObjective(h,state,"anchor"+phase);var battle=state.getCompound("Battle");
            h.assertTrue(battle.getInt("Phase")==phase+1,"Each completed knot advances exactly one actual phase");
            h.assertTrue(battle.getInt("RoundStartPhase")==phase,"Unrevealed animations retain the start phase instead of displaying a future result");
            if(phase<2)h.assertTrue(battle.getString("Result").isBlank(),"An early phase cannot declare overall victory");
            var saved=state.getList("DiceHistory",Tag.TAG_COMPOUND).copy();reveal(state);
            h.assertTrue(state.getList("DiceHistory",Tag.TAG_COMPOUND).equals(saved),"Phase reveal does not reroll the committed checks");
        }
        var complete=state.getCompound("Battle");
        h.assertTrue(complete.getString("Result").equals("victory")&&complete.getInt("Objective")==3,"All three phases and their knots must be complete");
        h.assertTrue(complete.getList("Frames",Tag.TAG_COMPOUND).stream().anyMatch(t->((CompoundTag)t).contains("FinalWillRoll")),"Final will pressure has a distinct saved d20 result");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void clockWraithRepeatsOnlyItsRecordedAttackAndHealsOnce(GameTestHelper h) {
        var state=startAuthoredBattle(h,life(15),"life_event_pawned_tomorrow","force");var battle=state.getCompound("Battle");
        var enemy=LifeSimulationBattle.find(battle,"enemy1");enemy.putDouble("Hp",enemy.getDouble("MaxHp")*.5);
        LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(1));reveal(state);
        h.assertTrue(enemy.getCompound("PreviousAttack").getString("Id").equals("clock_borrowed"),"Clock's prior action is saved rather than fabricated");
        LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(2));reveal(state);
        h.assertTrue(enemy.getInt("DelayedHealRound")==3&&enemy.getString("Plan").equals("clock_repeat"),"Clock telegraphs its delayed recovery and recorded repetition");
        double health=enemy.getDouble("Hp");LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(3));
        h.assertTrue(enemy.getDouble("Hp")>health&&enemy.getInt("DelayedHealRound")==0,"Delayed morning healing executes once at the next round start");
        h.assertTrue(battle.getList("Frames",Tag.TAG_COMPOUND).stream().filter(t->((CompoundTag)t).getString("Action").equals("clock_delayed_heal")).count()==1,"Recovery produces one actual visible animation frame");
        h.assertTrue(LifeSimulationBattle.actorMoves(state,enemy).stream().filter(m->m.id().equals("clock_repeat")).findFirst().orElseThrow().potency()<=1.2,"Repeated hour has bounded power");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void mirrorContractsControlTheirOwnAttendantsShield(GameTestHelper h) {
        var state=startAuthoredBattle(h,life(15),"life_event_borrowed_faces","force");var battle=state.getCompound("Battle");
        h.assertTrue(battle.getInt("AnchorCount")==2&&LifeSimulationBattle.find(battle,"enemy2").getString("Profile").equals("mirror"),"Both authored mirror attendants and their contracts exist");
        LifeSimulationBattle.handle(state,payload(state,"plan:inspect:anchor0"),RandomSource.create(1));reveal(state);
        state=successfulObjective(h,state,"anchor0");battle=state.getCompound("Battle");
        h.assertTrue(LifeSimulationBattle.find(battle,"enemy1").getDouble("Shield")==0&&LifeSimulationBattle.find(battle,"enemy2").getDouble("Shield")>0,"Exposing one contract removes only its associated attendant's ward");
        h.assertTrue(battle.getString("Result").isBlank(),"One exposed contract cannot resolve the entire ledger");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void coercedFriendKeepsIdentityAndChangesSidesOnlyAfterExposure(GameTestHelper h) {
        var state=life(7);var friend=new CompoundTag();UUID id=UUID.randomUUID();friend.putUUID("Id",id);friend.putString("Name","old friend");
        friend.putBoolean("Alive",true);friend.putBoolean("Friend",true);friend.putInt("Trust",60);friend.putInt("Affinity",60);friend.putInt("Realm",4);friend.putInt("Age",30);
        state.getList("Relationships",Tag.TAG_COMPOUND).add(friend);state=startAuthoredBattle(h,state,"life_danger_friend_betrayal","resolve");
        var battle=state.getCompound("Battle");var actor=LifeSimulationBattle.find(battle,battle.getString("CoercedActor"));
        h.assertTrue(actor.getUUID("NpcId").equals(id)&&actor.getInt("Realm")==4&&actor.getBoolean("Enemy")&&friend.getCompound("CoercionHistory").hasUUID("CaptorId"),"Coercion uses the real persistent friend and a saved living captor");
        actor.putDouble("Hp",actor.getDouble("MaxHp")*.2);LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(1));
        h.assertTrue(!actor.getBoolean("Enemy")&&battle.getBoolean("FriendSwitched")&&!LifeSimulationBattle.find(battle,"enemy2").getBoolean("Hidden"),"Controlled sparring exposes the ambush and changes the same friend to an ally");
        h.assertTrue(!friend.getBoolean("Nemesis"),"Entering an enemy combat side never permanently marks a coerced friend hostile");reveal(state);
        h.assertTrue(LifeSimulationBattle.illegal(state,"strike","anchor0").equals("protected")&&LifeSimulationBattle.illegal(state,"shield","anchor0").isBlank(),"Hostage goal supports defense but rejects direct attacks");
        var unchanged=state.copy();LifeSimulationBattle.handle(state,payload(state,"ally:anchor0:attack"),RandomSource.create(2));
        h.assertTrue(state.equals(unchanged),"Protected anchors cannot masquerade as recruitable ally IDs");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void futureEchoCopiesObservedMovesAndShadowUsesActualStrain(GameTestHelper h) {
        var future=startAuthoredBattle(h,life(15),"life_event_future_letter","echo");var echo=LifeSimulationBattle.find(future.getCompound("Battle"),"enemy1");
        LifeSimulationBattle.find(future.getCompound("Battle"),"player").putDouble("Power",20);
        LifeSimulationBattle.handle(future,payload(future,"plan:heavy:enemy1"),RandomSource.create(1));reveal(future);
        h.assertTrue(echo.getInt("Realm")==7&&echo.getString("Plan").equals("future_copy"),"Echo retains its established realm and copies only an observed action");
        h.assertTrue(LifeSimulationBattle.actorMoves(future,echo).stream().filter(m->m.id().equals("future_copy")).findFirst().orElseThrow().potency()<=1.05,"Copy cannot inherit an unbounded player ultimate");
        var strained=life(10);strained.putInt("TrainingStrain",70);strained=startAuthoredBattle(h,strained,"life_danger_inner_demon","alternative");
        var shadow=LifeSimulationBattle.find(strained.getCompound("Battle"),"enemy1");shadow.putDouble("BasePower",100);shadow.putDouble("Power",100);
        h.assertTrue(strained.getCompound("Battle").getString("HistoryKey").equals("life.battle.history.training_strain"),"Shadow pressure is grounded in actual recorded strain, not invented family tragedy");
        LifeSimulationBattle.handle(strained,payload(strained,"plan:heavy:enemy1"),RandomSource.create(2));reveal(strained);
        LifeSimulationBattle.handle(strained,payload(strained,"plan:guard:player"),RandomSource.create(3));
        h.assertTrue(shadow.getDouble("Power")>100,"Hunger reacts to a recorded strong attack on the following round");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void roundLimitCannotLetAWeakEnemyKillAnUnharmedImmortal(GameTestHelper h) {
        var state=life(15);var definition=battleDefinition(4,"aggressive");definition.putInt("limit",6);definition.putString("stakes","lethal");
        LifeSimulationBattle.startLegacy(state,definition,RandomSource.create(1));double health=LifeSimulationBattle.find(state.getCompound("Battle"),"player").getDouble("Hp");
        for(int round=0;round<6;round++) { LifeSimulationBattle.handle(state,payload(state,"plan:guard:player"),RandomSource.create(round));reveal(state); }
        h.assertTrue(state.getCompound("Battle").getString("Result").equals("escape")&&state.getCompound("Battle").getBoolean("TimedOut"),"Unresolved time limit abandons the objective instead of inventing a fatal defeat");
        h.assertTrue(LifeSimulationBattle.find(state.getCompound("Battle"),"player").getDouble("Hp")==health,"Blocked lower-realm attacks cannot kill through a hidden timeout rule");h.succeed();
    }
}
