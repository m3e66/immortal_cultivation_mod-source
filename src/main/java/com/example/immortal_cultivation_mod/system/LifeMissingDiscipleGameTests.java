package com.example.immortal_cultivation_mod.system;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_replacement_stories")
@PrefixGameTestTemplate(false)
public final class LifeMissingDiscipleGameTests {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static CompoundTag definition(GameTestHelper h){return LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager()).stream()
            .filter(d->d.getString("id").equals("life_event_missing_disciple")).findFirst().orElseThrow();}
    static CompoundTag member(int realm){var life=LifeReplacementStoryGameTests.person(realm,0);life.putString("Sect","qingyun");return life;}
    private static CompoundTag start(GameTestHelper h){var life=member(0);check(LifeSimulationStories.start(life,definition(h)),"Eligible rootless member starts");return life;}
    private static CompoundTag state(CompoundTag life){return life.getCompound("Story").getCompound("State");}
    private static void path(CompoundTag life,String choices){for(String c:choices.split(" "))LifeReplacementStoryGameTests.choose(life,c);}
    private static CompoundTag person(CompoundTag life,String role){var id=life.getCompound("Story").getCompound("Cast").getUUID(role);return life.getList("Relationships",Tag.TAG_COMPOUND).stream()
            .map(t->(CompoundTag)t).filter(n->n.getUUID("Id").equals(id)).findFirst().orElseThrow();}
    @GameTest(template="fate_test")
    public static void affiliationAndAgeAreRequiredWithoutInventingMembership(GameTestHelper h){
        var life=LifeReplacementStoryGameTests.person(0,0);var before=life.copy();
        check(!LifeSimulationStories.start(life,definition(h))&&life.equals(before),"Outsider gains no sect or cast");
        life.putString("Sect","qingyun");life.putInt("Age",15);check(!LifeSimulationStories.start(life,definition(h)),"Age below sixteen blocked");
        life.putInt("Age",16);check(LifeSimulationStories.start(life,definition(h)),"Rootless registered sect person allowed");
        life=LifeReplacementStoryGameTests.person(0,0);var work=new CompoundTag();work.putString("Sect","qingyun");work.putBoolean("Active",true);work.putBoolean("Registered",true);life.put("SectEmployment",work);
        check(LifeSimulationStories.start(life,definition(h))&&life.getString("Sect").equals("none"),"Actual employment qualifies without granting disciple membership");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void pennilessMortalCanPreserveTechnicalEvidenceAndRescueLin(GameTestHelper h){
        var life=start(h);var inventory=life.getList("Inventory",Tag.TAG_COMPOUND).copy();path(life,"B C A A A B A A A");
        check(life.getCompound("Story").getString("Node").equals("E1"),"Technical-evidence mortal route");
        var s=state(life);check(s.getBoolean("lin_freed")&&s.getBoolean("batch_lost")&&s.getBoolean("worker_private_confirmation"),"Actual rescue and privately corroborated evidence");
        check(!s.getBoolean("confidentiality_broken")&&s.getBoolean("confidentiality_kept"),"Private corroboration honors privacy promise");
        check(person(life,"lead").getInt("Health")==25&&person(life,"worker").getInt("Health")==55,"Neither NPC automatically healed");
        check(person(life,"worker").getInt("Realm")==0&&person(life,"worker").getString("SpiritualRoot").equals("none"),"Worker stays rootless");
        check(LifeCardBattle.companions(life).isEmpty(),"Named cast are not added to combat support");
        path(life,"finish");check(life.getList("Inventory",Tag.TAG_COMPOUND).equals(inventory)&&life.getLong("Wealth")==0,"No project stones, cloak or compensation enters player inventory");
        check(life.getInt("Realm")==0&&life.getList("DiscoveredSpells",Tag.TAG_STRING).isEmpty(),"No invented spell or cultivation");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void stewardSubstitutionSpendsProjectReserveExactlyOnce(GameTestHelper h){
        var life=start(h);path(life,"C A B B B");
        var choices=LifeSimulationStories.node(definition(h),"S12").getList("choices",Tag.TAG_COMPOUND);
        check(choices.size()==2&&choices.stream().noneMatch(c->((CompoundTag)c).getString("id").equals("A")),"Personal formation option removed as requested");
        path(life,"B");var s=state(life);
        check(life.getCompound("Story").getString("Node").equals("E2")&&s.getBoolean("lin_freed"),"Steward successfully replaces human source");
        check(s.getInt("ProjectReserve")==0&&s.getInt("ProjectStonesSpent")==24&&s.getInt("ElapsedHours")==8,"Project spends its actual twenty-four stones over four double-hours");
        check(s.getBoolean("crop_preserved")&&!s.getBoolean("batch_lost")&&!s.getBoolean("public_accountability"),"Sealed compromise preserves crop without public conviction");
        check(life.getLong("Wealth")==0&&person(life,"steward").getBoolean("AuthorityOverLinRevoked"),"No personal windfall; actual authority restriction persists");
        LifeSimulationStories.openNode(life,"S12");check(!LifeSimulationStories.choiceIllegal(life,choices.getCompound(0)).isBlank(),"Reserve cannot be consumed twice");
        LifeSimulationStories.openNode(life,"E2");path(life,"finish");check(life.getCompound("MissingDiscipleState").getInt("ProjectStonesSpent")==24,"Saved ending retains single expenditure");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void formalProtectionRescuesOnceWithoutPunishingTruthfulReporter(GameTestHelper h){
        var life=start(h);path(life,"C C A A");var s=state(life);
        check(life.getCompound("Story").getString("Node").equals("E3")&&s.getBoolean("protective_order"),"Early report reaches provisional protection");
        check(s.getInt("ShutdownCount")==1&&!s.getBoolean("censure_pending")&&!s.getBoolean("lin_seen"),"No invented personal eyewitness knowledge or censure");
        check(life.getInt("Reputation")==0&&life.getLong("Wealth")==0,"Truthful reporting carries no invented reputation cost");
        life=start(h);path(life,"C A B A A A B A A");
        check(state(life).getInt("ShutdownCount")==1,"Formal route after prior rescue does not shut down or destroy batch twice");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void privacyBreachAndUnauthorizedRecordsRemainReviewable(GameTestHelper h){
        var life=start(h);path(life,"B B A C B B");var s=state(life);
        check(life.getCompound("Story").getString("Node").equals("E3"),"Disclosure route reaches provisional ending");
        check(s.getBoolean("confidentiality_broken")&&s.getBoolean("unauthorized_records")&&s.getBoolean("censure_pending"),"Actual misconduct recorded");
        check(s.getBoolean("review_requested")&&s.getBoolean("archive_access_restricted"),"Appeal does not erase pending censure");
        check(person(life,"worker").getBoolean("RefusesDocumentCustody")&&person(life,"worker").getInt("Trust")==0,"Rescue never restores breached trust automatically");
        path(life,"finish");check(life.getCompound("MissingDiscipleState").getBoolean("confidentiality_broken"),"Leak remains saved");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void withdrawalPreservesActualRescueAndEntrustedProperty(GameTestHelper h){
        var life=start(h);path(life,"D B");check(state(life).getBoolean("lin_connected")&&!state(life).getBoolean("lin_freed"),"Early report is not a completed rescue");
        life=start(h);path(life,"A A B B B A B B");
        check(life.getCompound("Story").getString("Node").equals("E4")&&state(life).getBoolean("lin_freed"),"Withdrawal after rescue never reconnects Lin");
        check(state(life).getBoolean("cloak_returned_to_lin")&&state(life).getBoolean("key_returned")&&!state(life).getBoolean("cloak_held"),"Entrusted cloak, ticket and key returned");
        check(life.getList("Inventory",Tag.TAG_COMPOUND).size()==1,"Entrusted property was never saleable loot");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void insightSeventeenPersistsAndFailureAllowsAnotherEvidenceRoute(GameTestHelper h){
        for(int degree=0;degree<4;degree++){
            var life=start(h);path(life,"B A");var roll=life.getCompound("Story").getCompound("PendingRoll");check(roll.getInt("DC")==17,"Original insight target");
            roll.putInt("Degree",degree);var restored=life.copy();LifeSimulationStories.choose(life,"reveal",RandomSource.create(2));LifeSimulationStories.choose(restored,"reveal",RandomSource.create(999));
            check(life.getCompound("Story").getString("Node").equals("S04")&&state(life).equals(state(restored)),"Saved check branches identically after reload");
            check(state(life).getBoolean("accounting_evidence")== (degree>=1),"Only successful reading grants complete accounts");
            var before=life.copy();LifeSimulationStories.choose(life,"reveal",RandomSource.create(7));check(life.equals(before),"No reroll or duplicate effects");
            check(life.getInt("Health")==100&&life.getInt("TrainingStrain")==0,"Evidence failure causes no damage or extra strain");
            if(degree==0){path(life,"B A A A B A A A");check(life.getCompound("Story").getString("Node").equals("E1"),"Failed accounts still allow technical-evidence ending");}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void guardStandDownAndDeadWorkerDoNotInventCombatOrDialogue(GameTestHelper h){
        var life=member(1);LifeSimulationStories.start(life,definition(h));path(life,"C A B C B");
        check(!life.contains("Battle")&&!state(life).getBoolean("battle_started")&&life.getInt("KnockoutStreak")==0,"Stand down before combat adds no assault or knockout");
        life=start(h);person(life,"worker").putBoolean("Alive",false);LifeSimulationStories.openNode(life,"S05");
        check(life.getCompound("Story").getString("Node").equals("E4")&&life.getCompound("CurrentEvent").getList("TextParts",Tag.TAG_STRING).size()==1,"Dead worker leaves surviving account only");h.succeed();
    }
}
