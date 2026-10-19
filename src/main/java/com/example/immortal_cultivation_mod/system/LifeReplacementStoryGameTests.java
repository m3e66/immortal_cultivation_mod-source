package com.example.immortal_cultivation_mod.system;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_replacement_stories")
@PrefixGameTestTemplate(false)
public final class LifeReplacementStoryGameTests {
    private static final RandomSource RNG=RandomSource.create(1521);
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static CompoundTag person(int realm,int stones){
        CompoundTag life=new CompoundTag();life.putString("Name","Story test");life.putBoolean("Active",true);
        life.putBoolean("Alive",true);life.putInt("Age",28);life.putInt("MaximumLifespan",180);life.putInt("Health",100);
        life.putInt("Realm",realm);life.putInt("Comprehension",70);life.putInt("Combat",60);life.putInt("Mentality",80);
        life.putString("SpiritualRoot",realm>0?"heavenly":"none");life.putString("Sect","none");life.putLong("Seed",251);
        life.putLong("Wealth",stones);life.putDouble("CombatQi",100000);life.putInt("AnnualActionPoints",3);
        ListTag known=new ListTag();if(realm>0)for(String id:List.of("wind_step","yufeng_jue","yuqi_shu","gushi_shield","sifang_jie","fireball"))known.add(StringTag.valueOf(id));
        life.put("DiscoveredSpells",known);ListTag inventory=new ListTag();CompoundTag stack=new CompoundTag();
        stack.putString("Id","minecraft:wheat");stack.putInt("Count",5);inventory.add(stack);life.put("Inventory",inventory);
        life.put("Relationships",new ListTag());return life;
    }
    private static CompoundTag definition(GameTestHelper helper,String id){
        return LifeSimulationStories.definitions(helper.getLevel().getServer().getResourceManager()).stream()
                .filter(d->d.getString("id").equals("life_story_"+id)).findFirst().orElseThrow();
    }
    static void choose(CompoundTag life,String id){
        var story=life.getCompound("Story");
        var choice=LifeSimulationStories.node(story.getCompound("Definition"),story.getString("Node")).getList("choices",Tag.TAG_COMPOUND).stream()
                .map(t->(CompoundTag)t).filter(t->t.getString("id").equals(id)).findFirst().orElseThrow();
        check(LifeSimulationStories.choiceIllegal(life,choice).isBlank(),"Illegal "+story.getString("Id")+" "+story.getString("Node")+":"+id);
        LifeSimulationStories.choose(life,story.getUUID("Instance")+"/"+story.getInt("Revision")+"/"+id,RNG);
    }
    private static void path(CompoundTag life,String choices){for(String choice:choices.split(" "))choose(life,choice);}
    @GameTest(template="fate_test",timeoutTicks=1000)
    public static void allNewScenesChoicesAndEndingsAreReachable(GameTestHelper helper){
        var definitions=LifeSimulationStories.definitions(helper.getLevel().getServer().getResourceManager());
        check(definitions.stream().filter(d->!d.getBoolean("loot_encounter")).count()==6,"All six supplied replacement stories ship");
        check(definitions.stream().anyMatch(d->d.getBoolean("loot_encounter")),"Reward expedition is available alongside the supplied stories");
        Set<String> expected=new HashSet<>(),seen=new HashSet<>();
        for(var d:definitions)for(var t:d.getList("nodes",Tag.TAG_COMPOUND))for(var c:((CompoundTag)t).getList("choices",Tag.TAG_COMPOUND))
            expected.add(d.getString("id")+":"+((CompoundTag)t).getString("id")+":"+((CompoundTag)c).getString("id"));
        java.util.Random pick=new java.util.Random(932);
        for(var definition:definitions)for(int sample=0;sample<700;sample++){
            CompoundTag life=person(sample%3==0?0:sample%3==1?1:10,30);
            if(definition.getBoolean("sect"))life.putString("Sect","qingyun");
            if(sample%2==0)LifeSimulationSystem.addSimulationItem(life,"minecraft:oak_boat",1);
            check(LifeSimulationStories.start(life,definition),"Start eligible story");
            for(int step=0;step<30&&life.contains("Story");step++){
                if(life.contains("Battle")){
                    CompoundTag b=life.getCompound("Battle");String result=List.of("victory","defeat","escape").get(pick.nextInt(3));
                    b.putString("Result",result);b.put("Frames",new ListTag());b.putInt("Reveal",0);LifeSimulationStories.finishBattle(life);continue;
                }
                CompoundTag s=life.getCompound("Story");
                if(s.contains("PendingRoll")){LifeSimulationStories.choose(life,"reveal",RNG);continue;}
                var choices=LifeSimulationStories.node(s.getCompound("Definition"),s.getString("Node")).getList("choices",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
                        .filter(c->LifeSimulationStories.choiceIllegal(life,c).isBlank()).toList();
                check(!choices.isEmpty(),"No dead end: "+s.getString("Id")+":"+s.getString("Node"));
                var c=choices.get(pick.nextInt(choices.size()));seen.add(s.getString("Id")+":"+s.getString("Node")+":"+c.getString("id"));choose(life,c.getString("id"));
            }
            check(!life.contains("Story"),"Every route terminates");
        }
        // Exercise the long preparation route explicitly instead of relying on
        // random coverage to select this rare conjunction of map, tools and trust.
        var prepared=person(0,0);LifeSimulationStories.start(prepared,definition(helper,"ninth_spring"));
        for(String c:"A A A A B A A B A finish".split(" ")) {
            var s=prepared.getCompound("Story");seen.add(s.getString("Id")+":"+s.getString("Node")+":"+c);choose(prepared,c);
        }
        // Taking the sword is intentionally a long, optional route. Cover each
        // post-victory decision explicitly; separate tests win via real card damage.
        var rain=definitions.stream().filter(d->d.getString("id").equals("life_event_rain_sword")).findFirst().orElseThrow();
        for(String branch:List.of("A finish","B A A finish","C A finish")) {
            var life=person(10,0);LifeSimulationStories.start(life,rain);
            for(String c:"B B B B C A A".split(" ")) {
                var s=life.getCompound("Story");seen.add(s.getString("Id")+":"+s.getString("Node")+":"+c);choose(life,c);
            }
            var battle=life.getCompound("Battle");battle.putString("Result","victory");battle.put("Frames",new ListTag());LifeSimulationStories.finishBattle(life);
            for(String c:branch.split(" ")) {
                var s=life.getCompound("Story");seen.add(s.getString("Id")+":"+s.getString("Node")+":"+c);choose(life,c);
            }
        }
        expected.removeAll(seen);check(expected.isEmpty(),"Unreached choices: "+expected);helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void rootlessMortalCanReachPublicBellEndingWithoutRewards(GameTestHelper helper){
        var life=person(0,0);LifeSimulationStories.start(life,definition(helper,"third_bell"));
        path(life,"B A A A A B A A");check(life.getCompound("Story").getString("Node").equals("E1"),"Mortal public ending");
        choose(life,"finish");check(!life.contains("Story"),"Finish clears pending scene");
        check(life.getList("DiscoveredSpells",Tag.TAG_STRING).isEmpty()&&life.getInt("Realm")==0,"No invented roots or spells");
        check(LifeAuthoredStories.stones(life)==0&&life.getList("Inventory",Tag.TAG_COMPOUND).getCompound(0).getInt("Count")==5,"No invented riches or item changes");
        check(life.getCompound("StoryJournal").getCompound("life_story_third_bell").getList("TextParts",Tag.TAG_STRING).size()>5,"Full ending persists");helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void actualCostsStaleClicksAndSavedChecks(GameTestHelper helper){
        var life=person(0,12);LifeSimulationStories.start(life,definition(helper,"third_bell"));choose(life,"A");
        var story=life.getCompound("Story");String token=story.getUUID("Instance")+"/"+story.getInt("Revision")+"/B";
        LifeSimulationStories.choose(life,token,RNG);check(LifeAuthoredStories.stones(life)==0,"Twelve actual stones spent");
        LifeSimulationStories.choose(life,token,RNG);check(LifeAuthoredStories.stones(life)==0,"Duplicate payload not charged");
        life=person(0,0);LifeSimulationStories.start(life,definition(helper,"third_bell"));path(life,"B B B");
        var saved=life.copy();var roll=life.getCompound("Story").getCompound("PendingRoll").copy();
        LifeSimulationStories.choose(life,"reveal",RNG);LifeSimulationStories.choose(saved,"reveal",RNG);
        check(life.getCompound("Story").getString("Node").equals(saved.getCompound("Story").getString("Node")),"Reload uses saved result");
        check(life.getInt("Health")==100&&life.getInt("TrainingStrain")==0,"Environmental check adds no injuries or strain");
        check(roll.hasUUID("Id"),"Saved roll identity");helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void entrustedFreightAndBondNeverBecomePersonalWealth(GameTestHelper helper){
        var life=person(0,0);LifeSimulationStories.start(life,definition(helper,"salt_baskets"));path(life,"A B A B");
        check(life.getCompound("Story").getString("Node").equals("S08"),"Arrive for report");
        var state=life.getCompound("Story").getCompound("State");check(state.getInt("FreightReturned")==2,"Unused freight returned");
        check(state.getInt("RangBondReturned")==12&&state.getInt("VillagerCompensation")==6,"Bond belongs to Rang/villagers");
        check(LifeAuthoredStories.stones(life)==3,"Only commission enters inventory");
        LifeSimulationStories.openNode(life,"S08");check(LifeAuthoredStories.stones(life)==3,"Reopen cannot duplicate payment");
        path(life,"A B A A");check(life.getCompound("Story").getString("Node").equals("E1"),"Funded winter ending");
        choose(life,"finish");check(LifeAuthoredStories.stones(life)==3,"No mount or bond reward");helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void combatAndKnockoutKeepExistingRules(GameTestHelper helper){
        for(String id:List.of("third_bell","winter_coats","ninth_spring","salt_baskets")){
            var life=person(1,0);LifeSimulationStories.start(life,definition(helper,id));LifeSimulationStories.openNode(life,"S10");choose(life,"A");
            check(LifeCardBattle.enabled(life.getCompound("Battle")),"Real card battle: "+id);
            check(LifeCardBattle.companions(life).isEmpty(),"Named cast cannot become combat support");
            var b=life.getCompound("Battle");var p=LifeCardBattle.player(life);p.putDouble("Hp",0);life.putInt("KnockoutStreak",1);
            b.putString("Result","defeat");b.put("Frames",new ListTag());LifeSimulationStories.finishBattle(life);
            check(life.getBoolean("Alive")&&!life.getBoolean("CombatDead"),"No extra story death");
            check(LifeCardInjuries.hp(life)==0,"No free full healing");check(life.getCompound("Story").getString("Node").equals("E4"),"Unconscious receives written account");
            choose(life,"finish");check(!life.contains("Story"),"Unconscious aftermath can close");
        }helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void pendingFeeWaitsForRecoveryAndNeverPaysDeadCharacters(GameTestHelper helper){
        var life=person(0,0);LifeSimulationStories.start(life,definition(helper,"salt_baskets"));
        life.putDouble("CombatHp",0);life.putInt("Health",1);LifeSimulationStories.openNode(life,"S16");choose(life,"finish");
        check(LifeAuthoredStories.stones(life)==0,"Unconscious fee not collected");
        var dead=life.copy();dead.putBoolean("CombatDead",true);dead.putBoolean("Alive",false);LifeAuthoredStories.settlePendingFees(dead);
        check(LifeAuthoredStories.stones(dead)==0,"No fee added after death");
        life.putInt("RecoveryYear",life.getInt("Age"));life.putInt("Age",life.getInt("Age")+1);LifeCardInjuries.recover(life,life.getInt("Age"));
        LifeAuthoredStories.settlePendingFees(life);LifeAuthoredStories.settlePendingFees(life);
        check(LifeAuthoredStories.stones(life)==3,"Fee collected once after actual recovery");helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void newStoriesTriggerThroughExploreAndRespectAge(GameTestHelper helper){
        var resources=helper.getLevel().getServer().getResourceManager();var life=person(0,0);life.putInt("Age",15);
        check(!LifeSimulationStories.select(life,resources,"adventure",RNG),"Children cannot enter adult stories");
        life.putInt("Age",18);check(LifeSimulationStories.select(life,resources,"adventure",RNG),"Explore triggers replacement");
        check(life.getCompound("CurrentEvent").getList("TextParts",Tag.TAG_STRING).size()>10,"Full dialogue sent to encounter");
        var trial=LifeSimulationStories.breakthroughTrial(resources);check(trial!=null,"Mechanical breakthrough trial survives story removal");
        var cultivator=person(6,0);check(LifeSimulationStories.start(cultivator,trial),"Trial starts");choose(cultivator,"begin");
        check(LifeCardBattle.enabled(cultivator.getCompound("Battle")),"Trial uses existing combat");helper.succeed();
    }
}
