package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_card_battle")
@PrefixGameTestTemplate(false)
public final class LifeCardBattleGameTests {
    private LifeCardBattleGameTests() {}
    private static CompoundTag life(){
        CompoundTag life=new CompoundTag();life.putUUID("SimulationId",UUID.randomUUID());life.putString("Name","Card test");
        life.putInt("Realm",4);life.putInt("Age",30);life.putInt("Health",100);life.putBoolean("Alive",true);life.putBoolean("Active",true);life.putInt("Combat",90);
        CompoundTag definition=new CompoundTag();definition.putInt("limit",100);definition.putString("stakes","lethal");
        CompoundTag enemy=new CompoundTag();enemy.putString("name_key","Test opponent");enemy.putInt("realm",4);enemy.putString("profile","aggressive");
        ListTag enemies=new ListTag();enemies.add(enemy);definition.put("enemies",enemies);
        LifeSimulationBattle.start(life,definition,RandomSource.create(12));
        CompoundTag opponent=LifeSimulationBattle.find(life.getCompound("Battle"),"enemy1");opponent.putDouble("Power",0);opponent.putDouble("Hp",100000);opponent.putDouble("MaxHp",100000);
        return life;
    }
    private static void action(CompoundTag life,String command){CompoundTag b=life.getCompound("Battle");LifeSimulationBattle.handle(life,b.getUUID("Id")+":"+b.getInt("Revision")+":"+command,RandomSource.create(14));}
    private static void skip(CompoundTag life){if(LifeSimulationBattle.pending(life.getCompound("Battle")))action(life,"skip");}
    private static CompoundTag takeCard(CompoundTag life,String id){
        CompoundTag b=life.getCompound("Battle");ListTag hand=b.getList("Hand",Tag.TAG_COMPOUND);
        for(var tag:hand)if(((CompoundTag)tag).getString("Card").equals(id))return (CompoundTag)tag;
        for(String name:java.util.List.of("DrawPile","DiscardPile")){
            ListTag pile=b.getList(name,Tag.TAG_COMPOUND);for(int i=0;i<pile.size();i++)if(pile.getCompound(i).getString("Card").equals(id)){
                CompoundTag card=(CompoundTag)pile.remove(i);if(hand.size()>=5)b.getList("DiscardPile",Tag.TAG_COMPOUND).add(hand.remove(0));hand.add(card);return card;}}
        throw new AssertionError("Missing fixture card "+id);
    }
    private static void play(CompoundTag life,String id,String target){CompoundTag card=takeCard(life,id);action(life,"play:"+card.getString("Instance")+":"+target);skip(life);}

    private static void valueBuff(CompoundTag actor,String id,double amount){
        CompoundTag buffs=actor.getCompound("CardBuffs"),entry=new CompoundTag();entry.putDouble("Amount",amount);entry.putInt("Until",100);buffs.put(id,entry);actor.put("CardBuffs",buffs);
    }
    @GameTest(template="fate_test")
    public static void cardDamagePreviewsMatchRealCastsWithoutConsumingDefenses(GameTestHelper h){
        for(String id:java.util.List.of("strike","spell_fireball","spell_jinfeng_zhi","spell_wanren_chuanxin","spell_bingzhui_yu")){
            CompoundTag state=life(),b=state.getCompound("Battle"),p=LifeCardBattle.player(state),enemy=LifeSimulationBattle.find(b,"enemy1");
            ListTag spells=new ListTag();for(String spell:java.util.List.of("fireball","jinfeng_zhi","wanren_chuanxin","bingzhui_yu"))spells.add(StringTag.valueOf(spell));state.put("DiscoveredSpells",spells);
            action(state,"deck:0:"+id);p.putDouble("Qi",100000);p.putDouble("MaxQi",100000);p.putDouble("Power",80);
            enemy.putDouble("Hp",1000);enemy.putDouble("MaxHp",1000);enemy.putDouble("ArmorFraction",.4);enemy.putDouble("Shield",13);
            valueBuff(p,"FIRE_DAMAGE",50);valueBuff(p,"NEXT_MELEE",40);valueBuff(enemy,"DEFENSE",20);valueBuff(enemy,"FIRE_RESIST",10);valueBuff(enemy,"COLD_RESIST",50);
            action(state,"ready");ListTag queue=new ListTag();
            for(String kind:java.util.List.of("block","dodge","block")){var defense=new CompoundTag();defense.putString("Kind",kind);defense.putDouble("Strength",.3);defense.putBoolean("Success",true);queue.add(defense);}enemy.put("DefenseQueue",queue);
            var card=LifeCardCatalog.get(state,p,id);var before=state.copy();var preview=LifeCardBattle.preview(p,enemy,card);
            h.assertTrue(state.equals(before),"Preview never rolls dice, spends buffs or consumes defenses");
            if(id.equals("spell_fireball"))h.assertTrue(Math.abs(preview.minimum()-.209475)<1e-9&&Math.abs(preview.maximum()-.231525)<1e-9,"Fire preview includes damage bonus, armor, resistance, Block and finite shield");
            if(id.equals("spell_bingzhui_yu")){
                var bare=enemy.copy();bare.remove("Shield");bare.remove("DefenseQueue");var ice=LifeCardBattle.preview(p,bare,card);
                h.assertTrue(Math.abs(ice.minimum()-41.04)<1e-9&&Math.abs(ice.maximum()-45.36)<1e-9,"Chinese ice element applies cold resistance to all eight hits");
            }
            for(int seed=0;seed<12;seed++){
                var trial=state.copy();var battle=trial.getCompound("Battle");var victim=LifeSimulationBattle.find(battle,"enemy1");double hp=victim.getDouble("Hp");
                var instance=takeCard(trial,id);LifeSimulationBattle.handle(trial,battle.getUUID("Id")+":"+battle.getInt("Revision")+":play:"+instance.getString("Instance")+":enemy1",RandomSource.create(seed));
                var frame=battle.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);double damage=hp-victim.getDouble("Hp");
                h.assertTrue(damage>=preview.minimum()-1e-9&&damage<=preview.maximum()+1e-9,"Actual cast stays inside target-aware range: "+id);
                h.assertTrue(Math.abs(frame.getDouble("AppliedAmount")-damage)<1e-9,"Committed damage reports actual HP loss: "+id);
            }
            enemy.putDouble("Hp",.01);enemy.remove("Shield");enemy.remove("DefenseQueue");var lethal=LifeCardBattle.preview(p,enemy,card);
            h.assertTrue(lethal.maximum()<=.01+1e-9,"Damage preview caps overkill at remaining HP");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void recoveryValuesSeparateCappedHealingAndTimedTotals(GameTestHelper h){
        CompoundTag state=life(),b=state.getCompound("Battle"),p=LifeCardBattle.player(state);
        ListTag spells=new ListTag();spells.add(StringTag.valueOf("regeneration"));state.put("DiscoveredSpells",spells);
        action(state,"deck:0:spell_regeneration");action(state,"ready");p.putDouble("Hp",p.getDouble("MaxHp")-1);
        var heal=LifeCardCatalog.get(state,p,"spell_regeneration");var preview=LifeCardBattle.preview(p,p,heal);
        h.assertTrue(heal.amount()==4&&preview.minimum()==1&&preview.maximum()==1,"Original healing baseline stays four; display caps restoration to one missing HP");
        play(state,"spell_regeneration","player");var frame=b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);
        h.assertTrue(p.getDouble("Hp")==p.getDouble("MaxHp")&&frame.getDouble("AppliedAmount")==1&&frame.getDouble("Amount")>3,"Saved healing separates rolled potential from capped actual recovery");

        state=life();b=state.getCompound("Battle");p=LifeCardBattle.player(state);action(state,"ready");p.putDouble("Qi",p.getDouble("MaxQi")-1);
        preview=LifeCardBattle.preview(p,p,LifeCardCatalog.get(state,p,"recover"));h.assertTrue(preview.minimum()==1&&preview.maximum()==1,"Qi preview caps at missing Qi");
        play(state,"recover","player");h.assertTrue(b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0).getDouble("AppliedAmount")==1,"Actual Qi recovery reports only restored amount");

        state=life();b=state.getCompound("Battle");p=LifeCardBattle.player(state);
        String pill=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(com.example.immortal_cultivation_mod.alchemy.AlchemyContent.PILLS.get("huichun").get()).toString();
        ListTag inventory=new ListTag();var item=new CompoundTag();item.putString("Id",pill);item.putInt("Count",1);inventory.add(item);state.put("Inventory",inventory);
        action(state,"slot:0:"+pill.replace(':','~'));action(state,"ready");p.putDouble("MaxHp",1000);p.putDouble("Hp",998);
        var timed=LifeCardCatalog.item(p,pill);preview=LifeCardBattle.preview(p,p,timed);
        h.assertTrue(timed.amount()==60&&preview.turns()==6&&preview.minimum()==57&&preview.maximum()==63&&preview.initialMinimum()==2&&preview.initialMaximum()==2,"Timed pill preview distinguishes total, six turns and capped immediate recovery");
        play(state,timed.id(),"player");frame=b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);double factor=frame.getCompound("Roll").getDouble("Factor");
        h.assertTrue(frame.getDouble("AppliedAmount")==2&&Math.abs(frame.getDouble("PerTurn")-10*factor)<1e-9,"Pill result reports capped first heal and real per-turn rate");
        var effect=p.getList("CardEffects",Tag.TAG_COMPOUND).getCompound(0);
        h.assertTrue(effect.getInt("Remaining")==5&&Math.abs(effect.getDouble("Amount")*6-frame.getDouble("Amount"))<1e-9,"Immediate tick is part of total; five future ticks preserve original total");
        action(state,"end");skip(state);
        h.assertTrue(effect.getInt("Remaining")==5,"Timed pill cannot tick again at the end of its casting round");
        action(state,"end");skip(state);
        h.assertTrue(effect.getInt("Remaining")==4,"Next installment arrives once in the following round");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void legalDeckEnergyAndPersistentQi(GameTestHelper h){
        CompoundTag life=life(),b=life.getCompound("Battle");h.assertTrue(b.getList("RegularDeck",Tag.TAG_STRING).size()==15,"Fifteen regular cards");
        ListTag learned=new ListTag();learned.add(StringTag.valueOf("jinfeng_zhi"));learned.add(StringTag.valueOf("wanren_chuanxin"));life.put("DiscoveredSpells",learned);
        action(life,"deck:0:spell_jinfeng_zhi");int revision=b.getInt("Revision");action(life,"deck:1:spell_jinfeng_zhi");
        h.assertTrue(b.getInt("Revision")==revision,"Ordinary spells limited to one copy on server");
        action(life,"deck:2:spell_wanren_chuanxin");revision=b.getInt("Revision");action(life,"deck:3:spell_wanren_chuanxin");h.assertTrue(b.getInt("Revision")==revision,"Signature spells limited to one copy");
        action(life,"ready");h.assertTrue(b.getInt("Energy")==3&&b.getList("Hand",Tag.TAG_COMPOUND).size()==5,"Three Energy and five drawn cards");
        double qi=LifeCardBattle.player(life).getDouble("Qi");play(life,"spell_jinfeng_zhi","enemy1");
        h.assertTrue(LifeCardBattle.player(life).getDouble("Qi")==qi-ModSpells.get("jinfeng_zhi").qiCost(),"Actual world-spell Qi cost");
        var frame=b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);
        h.assertTrue(frame.getDouble("CardBase")==28&&Math.abs(frame.getDouble("Base")-35)<1e-9&&Math.abs(frame.getCompound("Roll").getDouble("Variation"))<=.05,"Original 28 damage, original 25 percent bonus against armor, bounded dice");
        h.assertTrue(Math.abs(frame.getDouble("Amount")/frame.getDouble("Base")-frame.getCompound("Roll").getDouble("Factor"))<1e-9,"Roll preview isolates dice variation from existing conditional spell bonuses");
        action(life,"end");skip(life);
        h.assertTrue(b.getInt("Energy")==3&&b.getList("Hand",Tag.TAG_COMPOUND).size()==5,"New turn restores Energy and hand");
        h.assertTrue(LifeCardBattle.player(life).getDouble("Qi")==qi-30,"Qi is not refilled every turn");
        var ids=new HashSet<String>();int count=0;for(String pile:java.util.List.of("Hand","DrawPile","DiscardPile","Exhausted"))for(var tag:b.getList(pile,Tag.TAG_COMPOUND)){count++;h.assertTrue(ids.add(((CompoundTag)tag).getString("Instance")),"No duplicated card instances");}
        h.assertTrue(count==15,"Regular cards remain conserved across piles");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void consumablesDiscardWithoutUseAndReplaySafeConsumption(GameTestHelper h){
        CompoundTag life=life();CompoundTag item=new CompoundTag();item.putString("Id","immortal_cultivation_mod:low_qi_recovery_pill");item.putInt("Count",2);
        ListTag inventory=new ListTag();inventory.add(item);life.put("Inventory",inventory);
        action(life,"slot:0:immortal_cultivation_mod~low_qi_recovery_pill");action(life,"slot:1:immortal_cultivation_mod~low_qi_recovery_pill");
        int revision=life.getCompound("Battle").getInt("Revision");action(life,"slot:2:immortal_cultivation_mod~low_qi_recovery_pill");
        h.assertTrue(life.getCompound("Battle").getInt("Revision")==revision,"Cannot fabricate a third pill");
        action(life,"ready");var card=takeCard(life,"item_immortal_cultivation_mod~low_qi_recovery_pill");
        h.assertTrue(item.getInt("Count")==2,"Drawing never consumes a pill");action(life,"end");skip(life);
        h.assertTrue(item.getInt("Count")==2,"Discarding never consumes a pill");card=takeCard(life,"item_immortal_cultivation_mod~low_qi_recovery_pill");
        CompoundTag b=life.getCompound("Battle");String token=b.getUUID("Id")+":"+b.getInt("Revision")+":play:"+card.getString("Instance")+":player";
        LifeSimulationBattle.handle(life,token,RandomSource.create(1));CompoundTag committed=life.copy();
        LifeSimulationBattle.handle(life,token,RandomSource.create(99));h.assertTrue(life.equals(committed)&&item.getInt("Count")==1,"Replay cannot reroll or consume twice");
        h.assertTrue(b.getList("Exhausted",Tag.TAG_COMPOUND).size()==1,"Played consumable exhausts permanently");
        skip(life);for(int i=0;i<4;i++){action(life,"end");skip(life);}h.assertTrue(item.getInt("Count")==1,"No automatic consumable refill");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void defensesResolveEachHitInPlayedOrder(GameTestHelper h){
        CompoundTag target=new CompoundTag();target.putDouble("Hp",1000);ListTag queue=new ListTag();
        for(String kind:java.util.List.of("block","dodge","block")){CompoundTag d=new CompoundTag();d.putString("Kind",kind);d.putDouble("Strength",.3);d.putBoolean("Success",true);queue.add(d);}target.put("DefenseQueue",queue);
        double[] hits={60,300,80},expected={42,0,56};for(int i=0;i<3;i++)h.assertTrue(Math.abs(LifeCardBattle.applyHit(target,hits[i],false).getDouble("HpDamage")-expected[i])<.00001,"Each hit consumes its own FIFO defense");
        h.assertTrue(queue.isEmpty()&&target.getDouble("Hp")==902,"One Dodge does not avoid an entire technique");
        CompoundTag failed=new CompoundTag();failed.putString("Kind","dodge");failed.putBoolean("Success",false);queue.add(failed);
        h.assertTrue(LifeCardBattle.applyHit(target,100,false).getDouble("HpDamage")==100,"Failed Dodge provides no reduction");
        CompoundTag block=new CompoundTag();block.putString("Kind","block");block.putDouble("Strength",1);queue.add(block);
        h.assertTrue(LifeCardBattle.applyHit(target,100,false).getDouble("HpDamage")>0,"Even malformed 100 percent Block cannot grant immunity");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void committedIntentAndUnusedDefenseExpiry(GameTestHelper h){
        CompoundTag life=life(),b=life.getCompound("Battle"),enemy=LifeSimulationBattle.find(b,"enemy1");enemy.putString("Profile","defensive");action(life,"ready");
        CompoundTag intent=enemy.getCompound("Intent").copy();play(life,"block","player");
        h.assertTrue(intent.equals(enemy.getCompound("Intent")),"Enemy cannot change intent after a counter is selected");
        h.assertTrue(LifeCardBattle.player(life).getList("DefenseQueue",Tag.TAG_COMPOUND).size()==1,"Block prepares one defensive hit");
        action(life,"end");skip(life);h.assertTrue(LifeCardBattle.player(life).getList("DefenseQueue",Tag.TAG_COMPOUND).isEmpty(),"Unused defenses expire on the next player turn");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void actualKnockoutsRecoverAndThirdEndsOnlySimulatedLife(GameTestHelper h){
        CompoundTag life=life();int year=life.getInt("Age");
        for(int knockout=1;knockout<=3;knockout++){
            if(knockout>1){life.remove("Battle");CompoundTag definition=new CompoundTag();definition.putInt("limit",30);CompoundTag enemy=new CompoundTag();enemy.putInt("realm",4);ListTag enemies=new ListTag();enemies.add(enemy);definition.put("enemies",enemies);LifeSimulationBattle.start(life,definition,RandomSource.create(8));}
            CompoundTag b=life.getCompound("Battle");LifeSimulationBattle.find(b,"enemy1").putDouble("Power",100000);action(life,"ready");action(life,"end");
            h.assertTrue(life.getInt("KnockoutStreak")==knockout&&LifeCardBattle.player(life).getDouble("Hp")==0,"Actual enemy action records exactly one knockout");
            if(knockout<3){h.assertTrue(life.getBoolean("Alive")&&!life.getBoolean("CombatDead"),"First and second knockouts survive lethal stakes");life.putInt("Age",++year);LifeCardInjuries.advanceYear(life);h.assertTrue(life.getDouble("CombatHp")==100,"Exactly 100 HP per simulated year");}
        }
        h.assertTrue(life.getBoolean("CombatDead")&&!life.getBoolean("Alive")&&life.getInt("Health")==0,"Third consecutive knockout permanently ends simulated life");
        CompoundTag recovered=new CompoundTag();recovered.putInt("Realm",4);recovered.putBoolean("Alive",true);recovered.putDouble("CombatHp",0);recovered.putInt("RecoveryYear",10);recovered.putInt("KnockoutStreak",2);
        LifeCardInjuries.recover(recovered,13);h.assertTrue(recovered.getDouble("CombatHp")==250&&recovered.getInt("KnockoutStreak")==0,"Recovery caps at max HP and resets streak only at full HP");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void diceNormalizeEveryRealmAndRoundTripState(GameTestHelper h){
        for(int realm=0;realm<28;realm++){CompoundTag life=life(),actor=LifeCardBattle.player(life);actor.putInt("Realm",realm);int[] pool=LifeCardDice.pool(life,realm);
            h.assertTrue(Math.abs(LifeCardDice.variation(pool[0],pool[0],pool[1])+.05)<1e-12&&Math.abs(LifeCardDice.variation(pool[0]*pool[1],pool[0],pool[1])-.05)<1e-12,"Every realm normalizes both extreme rolls to five percent");}
        CompoundTag life=life();action(life,"ready");play(life,"dodge","player");var saved=life.copy();var restored=saved.copy();
        h.assertTrue(restored.equals(life),"Piles, Energy, defense rolls and committed intent survive state round-trip");
        action(restored,"end");h.assertTrue(restored.getCompound("Battle").getList("Frames",Tag.TAG_COMPOUND).size()>0,"Restored battle remains playable");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void companionTechniquesResourcesAndThreeKnockouts(GameTestHelper h){
        CompoundTag life=life(),npc=new CompoundTag();npc.putUUID("Id",UUID.randomUUID());npc.putString("Name","Companion");npc.putInt("Realm",4);npc.putInt("Age",35);
        npc.putBoolean("Alive",true);npc.putBoolean("Friend",true);npc.putInt("Affinity",80);npc.putInt("Trust",80);npc.putInt("Health",80);npc.putDouble("CombatHp",200);
        ListTag spells=new ListTag();spells.add(StringTag.valueOf("fireball"));npc.put("DiscoveredSpells",spells);ListTag people=new ListTag();people.add(npc);life.put("Relationships",people);
        action(life,"companion:"+npc.getUUID("Id"));action(life,"ready");
        CompoundTag b=life.getCompound("Battle"),ally=LifeSimulationBattle.find(b,npc.getUUID("Id").toString());double qi=ally.getDouble("Qi"),playerQi=LifeCardBattle.player(life).getDouble("Qi");
        action(life,"end");
        String allyId=ally.getString("Id");h.assertTrue(b.getList("Frames",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(f->f.getString("Actor").equals(allyId)).count()==1,"Companion takes exactly one independent action");
        h.assertTrue(ally.getString("Plan").equals("spell_fireball")&&ally.getDouble("Qi")==qi-ModSpells.get("fireball").qiCost(),"Companion selects its own learned technique and spends its own Qi");
        h.assertTrue(LifeCardBattle.player(life).getDouble("Qi")==playerQi,"Companion never spends player Qi");skip(life);
        for(int ko=1;ko<=3;ko++){
            life.remove("Battle");life.remove("CardDeck");CompoundTag definition=new CompoundTag();definition.putInt("limit",30);ListTag enemies=new ListTag();CompoundTag enemy=new CompoundTag();enemy.putInt("realm",4);enemies.add(enemy);definition.put("enemies",enemies);
            npc.putDouble("CombatHp",20);npc.putInt("Health",8);LifeSimulationBattle.start(life,definition,RandomSource.create(2));
            LifeSimulationBattle.find(life.getCompound("Battle"),"enemy1").putDouble("Power",100000);action(life,"ready");action(life,"end");
            h.assertTrue(npc.getInt("KnockoutStreak")==ko,"Each companion knockout increments its own streak once");
            ally=LifeSimulationBattle.find(life.getCompound("Battle"),npc.getUUID("Id").toString());h.assertTrue(ally.getBoolean("KnockedOut"),"Knocked-out companion cannot act again this battle");
            if(ko<3)h.assertTrue(npc.getBoolean("Alive")&&LifeCardBattle.companions(life).isEmpty(),"Survives first two knockouts, but zero HP prevents immediate recruitment");
        }
        h.assertTrue(!npc.getBoolean("Alive")&&npc.getBoolean("CombatDead")&&LifeCardBattle.companions(life).isEmpty(),"Third companion knockout kills only its simulated NPC and prevents recruitment");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void reinforcementWaveAndAuthoredMultihitIntent(GameTestHelper h){
        CompoundTag life=life(),b=life.getCompound("Battle"),enemy=LifeSimulationBattle.find(b,"enemy1");
        for(int i=2;i<=4;i++){var reserve=enemy.copy();reserve.putString("Id","enemy"+i);b.getList("Actors",Tag.TAG_COMPOUND).add(reserve);}
        CompoundTag move=new CompoundTag();move.putString("id","three_strikes");move.putString("kind","strike");move.putString("name_key","Three strikes");move.putString("element","physical");move.putDouble("potency",1);move.putInt("cooldown",3);
        ListTag hits=new ListTag();for(double value:new double[]{60,300,80})hits.add(DoubleTag.valueOf(value));move.put("hits",hits);ListTag moves=new ListTag();moves.add(move);enemy.put("Moves",moves);
        action(life,"ready");h.assertTrue(b.getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(a->a.getBoolean("Enemy")&&LifeCardBattle.active(a)).count()==2,"Only two active enemies");
        h.assertTrue(enemy.getCompound("Intent").getList("Hits",Tag.TAG_DOUBLE).equals(hits),"Intent retains separately authored damage values");
        ListTag defenses=new ListTag();CompoundTag dodge=new CompoundTag();dodge.putString("Kind","dodge");dodge.putBoolean("Success",true);defenses.add(dodge);LifeCardBattle.player(life).put("DefenseQueue",defenses);LifeCardBattle.player(life).putDouble("Hp",1000);
        action(life,"end");h.assertTrue(enemy.getCompound("Cooldowns").getInt("three_strikes")==5,"Enemy action records its real cooldown");
        var frame=b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);var resolved=frame.getList("Hits",Tag.TAG_COMPOUND);
        h.assertTrue(resolved.size()==3&&resolved.getCompound(0).getDouble("HpDamage")==0&&resolved.getCompound(1).getDouble("HpDamage")>0,"One Dodge protects exactly one authored strike");
        for(var tag:resolved){CompoundTag hit=(CompoundTag)tag;h.assertTrue(Math.abs(hit.getDouble("Raw")/hit.getDouble("Base")-frame.getCompound("Roll").getDouble("Factor"))<1e-9,"Every strike shares one authoritative dice adjustment");}
        skip(life);enemy.putDouble("Hp",0);play(life,"block","player");h.assertTrue(LifeCardBattle.active(LifeSimulationBattle.find(b,"enemy3"))&&!LifeCardBattle.active(LifeSimulationBattle.find(b,"enemy4")),"One reinforcement enters when a slot opens");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void serializedPendingConsumptionAndBinaryRollCannotReplay(GameTestHelper h)throws Exception{
        CompoundTag life=life();action(life,"ready");var entry=takeCard(life,"dodge");String packet=life.getCompound("Battle").getUUID("Id")+":"+life.getCompound("Battle").getInt("Revision")+":play:"+entry.getString("Instance")+":player";
        LifeSimulationBattle.handle(life,packet,RandomSource.create(123));CompoundTag roll=life.getCompound("Battle").getList("Frames",Tag.TAG_COMPOUND).getCompound(0).getCompound("Roll");
        h.assertTrue(roll.contains("Requirement")&&roll.getBoolean("Success")== (roll.getInt("Total")>=roll.getInt("Requirement")),"One visible server pool decides binary Dodge");
        var bytes=new java.io.ByteArrayOutputStream();net.minecraft.nbt.NbtIo.writeCompressed(life,bytes);
        CompoundTag restored=net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()),net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        h.assertTrue(restored.equals(life),"Actual compressed save/load preserves piles, energy, intent, frames and defense roll");
        LifeSimulationBattle.handle(restored,packet,RandomSource.create(999));h.assertTrue(restored.equals(life),"Pre-restart packet cannot reroll or apply twice after restart");
        skip(restored);h.assertTrue(restored.getCompound("Battle").getList("Frames",Tag.TAG_COMPOUND).getCompound(0).getCompound("Roll").equals(roll),"Skipping keeps exactly the saved binary roll");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void sourcePillDoseAndGatheringNeverRefillInstantly(GameTestHelper h){
        CompoundTag life=life(),b=life.getCompound("Battle"),p=LifeCardBattle.player(life);ListTag learned=new ListTag();learned.add(StringTag.valueOf("qi_gathering"));life.put("DiscoveredSpells",learned);
        action(life,"deck:0:spell_qi_gathering");CompoundTag item=new CompoundTag();item.putString("Id","immortal_cultivation_mod:qi_recovery_pill");item.putInt("Count",1);ListTag inventory=new ListTag();inventory.add(item);life.put("Inventory",inventory);action(life,"slot:0:immortal_cultivation_mod~qi_recovery_pill");action(life,"ready");p.putDouble("Qi",0);
        double max=p.getDouble("MaxQi");play(life,"item_immortal_cultivation_mod~qi_recovery_pill","player");var detail=b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);double dose=max*.05*detail.getCompound("Roll").getDouble("Factor");
        h.assertTrue(Math.abs(p.getDouble("Qi")-dose/10)<1e-9,"Huiling's original five percent dose is spread over its original ten seconds");
        action(life,"end");skip(life);h.assertTrue(Math.abs(p.getDouble("Qi")-dose/10)<1e-9,"Casting round does not grant a second pill installment");
        for(int i=0;i<9;i++){action(life,"end");skip(life);}h.assertTrue(Math.abs(p.getDouble("Qi")-dose)<1e-9,"Total pill recovery matches source formula, without unlimited refills");
        p.putDouble("Qi",max*.5);play(life,"spell_qi_gathering","player");double before=p.getDouble("Qi"),gather=p.getDouble("GatherQi");action(life,"end");skip(life);
        h.assertTrue(Math.abs(p.getDouble("Qi")-before-gather)<1e-9&&p.getDouble("Qi")<max,"Explicit gathering recovers bounded Qi instead of auto-refilling");play(life,"dodge","player");h.assertTrue(!p.contains("GatherQi"),"Movement ends stationary gathering");
        p.putDouble("Hp",p.getDouble("MaxHp")-2);p.putInt("KnockoutStreak",2);life.putInt("KnockoutStreak",2);CompoundTag healing=new CompoundTag();healing.putString("Kind","Hp");healing.putDouble("Amount",2);healing.putInt("Remaining",1);p.getList("CardEffects",Tag.TAG_COMPOUND).add(healing);
        action(life,"end");skip(life);h.assertTrue(p.getDouble("Hp")==p.getDouble("MaxHp")&&p.getInt("KnockoutStreak")==0&&life.getInt("KnockoutStreak")==0,"Timed healing to full HP clears actor and persisted person knockout streaks");h.succeed();
    }
    private static CompoundTag authoredLife(){
        var state=life();state.remove("Battle");state.remove("CardDeck");state.remove("CombatHp");state.remove("CombatQi");state.putInt("Realm",15);state.putInt("Health",100);state.putInt("Mentality",100);state.putInt("Comprehension",100);state.putInt("Alchemy",100);state.putInt("Combat",100);state.putLong("Wealth",10000);state.putString("Sect","sect");state.putBoolean("CardNoCompanion",true);
        ListTag people=new ListTag();for(boolean rival:new boolean[]{false,true}){CompoundTag npc=new CompoundTag();npc.putUUID("Id",UUID.randomUUID());npc.putString("Name","Story relation");npc.putBoolean("Alive",true);npc.putInt("Realm",7);npc.putInt("Age",35);npc.putInt("Health",100);npc.putInt("Trust",80);npc.putInt("Affinity",80);npc.putBoolean("Friend",!rival);npc.putBoolean("Nemesis",rival);npc.putString("Grievance",rival?"old_duel":"");people.add(npc);}state.put("Relationships",people);return state;
    }
    private static CompoundTag authoredBattle(GameTestHelper h,CompoundTag definition){
        CompoundTag state=authoredLife();h.assertTrue(LifeSimulationStories.start(state,definition),"Story cast and life initialize: "+definition.getString("id"));LifeSimulationStories.openNode(state,"decision");
        var choice=state.getCompound("CurrentEvent").getList("Choices",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(c->c.contains("battle")).findFirst().orElseThrow();
        LifeSimulationStories.choose(state,choice.getString("Id"),RandomSource.create(5));h.assertTrue(LifeCardBattle.enabled(state.getCompound("Battle")),"Authored choice starts card engine");return state;
    }
    @GameTest(template="fate_test")
    public static void allTwentyStoriesKeepAuthoredConsequencesAndDeathRule(GameTestHelper h){
        var definitions=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager());h.assertTrue(definitions.size()==20,"Ten adventures and ten dangers remain present");
        for(var definition:definitions){
            CompoundTag state=authoredBattle(h,definition),b=state.getCompound("Battle");String expected=b.getString("NextLoss");
            for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){var enemy=(CompoundTag)tag;if(enemy.getBoolean("Enemy")&&!enemy.getString("Kind").equals("anchor")){enemy.putString("Profile","aggressive");enemy.putDouble("Power",1e12);}}
            action(state,"ready");action(state,"end");skip(state);h.assertTrue(b.getString("Result").equals("defeat")&&state.getInt("KnockoutStreak")==1,"Real enemy defeat records first knockout: "+definition.getString("id"));
            LifeSimulationStories.finishBattle(state);h.assertTrue(state.getBoolean("Alive")&&!state.getBoolean("CombatDead")&&state.getInt("Health")>0,"Authored failure cannot bypass three-knockout rule");
            h.assertTrue(!state.contains("Battle")&&state.getCompound("Story").getString("Node").equals(expected),"Defeat reaches authored branch: "+definition.getString("id"));var completed=state.copy();LifeSimulationStories.finishBattle(state);h.assertTrue(state.equals(completed),"Story consequence cannot replay");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void cardObjectivesRequireInspectionAndSurviveSixTurnGateDefense(GameTestHelper h){
        var definitions=LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager());var definition=definitions.stream().filter(d->d.getString("id").equals("life_event_missing_disciple")).findFirst().orElseThrow();
        var state=authoredBattle(h,definition);var b=state.getCompound("Battle");action(state,"ready");var objective=takeCard(state,"objective");h.assertTrue(LifeCardBattle.illegal(state,objective.getString("Instance"),"anchor1").equals("inspect"),"Card target validation preserves required inspection");
        play(state,"inspect","anchor1");h.assertTrue(LifeCardBattle.illegal(state,objective.getString("Instance"),"anchor0").equals("order")&&LifeCardBattle.illegal(state,objective.getString("Instance"),"anchor1").isBlank(),"Card inspection exposes authored shutdown order");
        boolean resolved=false;for(int seed=0;seed<50&&!resolved;seed++){var trial=state.copy();var card=takeCard(trial,"objective");var battle=trial.getCompound("Battle");LifeSimulationBattle.handle(trial,battle.getUUID("Id")+":"+battle.getInt("Revision")+":play:"+card.getString("Instance")+":anchor1",RandomSource.create(seed));resolved=LifeSimulationBattle.find(battle,"anchor1").getBoolean("Resolved");if(resolved)h.assertTrue(battle.getList("Frames",Tag.TAG_COMPOUND).getCompound(0).getCompound("Roll").contains("DC"),"Discrete authored objective check remains visible and saved");}h.assertTrue(resolved,"Actual Interact card can resolve an ordered formation seal");
        state=authoredBattle(h,definitions.stream().filter(d->d.getString("id").equals("life_danger_sect_invasion")).findFirst().orElseThrow());b=state.getCompound("Battle");for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){var enemy=(CompoundTag)tag;if(enemy.getBoolean("Enemy")){enemy.putDouble("Power",0);enemy.putDouble("Hp",1e12);enemy.putDouble("MaxHp",1e12);}}action(state,"ready");
        for(int turn=0;turn<6;turn++){play(state,"block","player");action(state,"end");skip(state);}h.assertTrue(b.getString("Result").equals("victory")&&b.getInt("HoldRounds")==6,"Prepared Block supports authored six-turn sect-gate defense");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void allTwentyStoriesCanWinThroughActualCards(GameTestHelper h){
        for(var definition:LifeSimulationStories.definitions(h.getLevel().getServer().getResourceManager())){
            CompoundTag state=authoredBattle(h,definition),b=state.getCompound("Battle");state.getCompound("Story").putBoolean("Prepared",true);
            for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){var enemy=(CompoundTag)tag;if(enemy.getBoolean("Enemy")&&!enemy.getString("Kind").equals("anchor")){enemy.putDouble("Power",0);enemy.putDouble("Hp",1);enemy.putDouble("Shield",0);}}
            action(state,"ready");
            if(b.getInt("AnchorCount")>0)play(state,"inspect","player");
            for(int step=0;step<24&&b.getString("Result").isBlank();step++){
                if(b.getInt("Energy")==0){action(state,"end");skip(state);continue;}
                String target="";for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){var anchor=(CompoundTag)tag;if(anchor.getString("Kind").equals("anchor")&&LifeSimulationBattle.objectiveIllegal(state,anchor.getString("Id")).isBlank()){target=anchor.getString("Id");break;}}
                if(!target.isBlank()){
                    boolean success=false;int before=b.getInt("Objective"),phase=b.getInt("Phase");
                    for(int seed=0;seed<80&&!success;seed++){var trial=state.copy();var entry=takeCard(trial,"objective");var battle=trial.getCompound("Battle");LifeSimulationBattle.handle(trial,battle.getUUID("Id")+":"+battle.getInt("Revision")+":play:"+entry.getString("Instance")+":"+target,RandomSource.create(seed));
                        success=battle.getInt("Objective")>before||battle.getInt("Phase")>phase||battle.getString("Result").equals("victory");if(success){state=trial;b=state.getCompound("Battle");skip(state);}}
                    h.assertTrue(success,"Legal Interact card can advance "+definition.getString("id"));
                }else{
                    var enemy=b.getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(a->a.getBoolean("Enemy")&&LifeCardBattle.active(a)).findFirst().orElseThrow();
                    enemy.putDouble("Hp",1);enemy.putDouble("Shield",0);play(state,"strike",enemy.getString("Id"));
                }
            }
            h.assertTrue(b.getString("Result").equals("victory"),"Actual card objective/attack wins "+definition.getString("id"));String expected=b.getString("VictoryBy").equals("objective")&&!b.getString("NextObjectiveWin").isBlank()?b.getString("NextObjectiveWin"):b.getString("NextWin");
            LifeSimulationStories.finishBattle(state);h.assertTrue(state.getCompound("Story").getString("Node").equals(expected),"Victory reaches authored consequence "+definition.getString("id"));var saved=state.copy();LifeSimulationStories.finishBattle(state);h.assertTrue(state.equals(saved),"Victory consequences apply once");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void clockIntentPreservesShieldAndOneDelayedHeal(GameTestHelper h){
        var state=life();CompoundTag b=state.getCompound("Battle"),enemy=LifeSimulationBattle.find(b,"enemy1");enemy.putString("Profile","clock");enemy.putDouble("Power",10);action(state,"ready");action(state,"end");skip(state);
        h.assertTrue(enemy.getCompound("Intent").getString("Card").equals("clock_morning"),"Clock commits its original support ability");enemy.putDouble("Hp",80000);double before=enemy.getDouble("Hp");action(state,"end");
        var detail=b.getList("Frames",Tag.TAG_COMPOUND).getCompound(0);double factor=detail.getCompound("Roll").getDouble("Factor");h.assertTrue(Math.abs(enemy.getDouble("Shield")-8*factor)<1e-9,"Clock shield uses original Power times 0.8 with one bounded roll");
        skip(state);h.assertTrue(Math.abs(enemy.getDouble("Hp")-before-5*factor)<1e-9,"Clock heals original Power times 0.5 once next turn");skip(state);h.assertTrue(Math.abs(enemy.getDouble("Hp")-before-5*factor)<1e-9,"Revealing delayed frame cannot duplicate healing");h.succeed();
    }

    private static CompoundTag oddsPerson() {
        var state=new CompoundTag();state.putInt("Comprehension",70);state.putInt("Alchemy",60);state.put("Story",new CompoundTag());return state;
    }
    private static CompoundTag oddsChoice(boolean extended) {
        var choice=new CompoundTag();var check=new CompoundTag();check.putString("purpose","insight");
        check.putString("attribute","Comprehension");check.putString("proficiency","Alchemy");check.putInt("dc",17);
        choice.put("check",check);choice.putBoolean("extended",extended);return choice;
    }
    private static void chance(GameTestHelper h,double actual,double expected,String message){h.assertTrue(Math.abs(actual-expected)<1e-9,message+" actual="+actual);}

    @GameTest(template="fate_test")
    public static void exactStoryOddsUseSimulatedStatsAndActualRollRules(GameTestHelper h) {
        var person=oddsPerson();var choice=oddsChoice(false);var original=person.copy();
        var odds=LifeStoryChecks.odds(person,choice);
        chance(h,odds.success(),.60,"Actual +4 bonus gives 60 percent passing including costly successes");
        chance(h,odds.clean(),.40,"Clean or exceptional outcomes");chance(h,odds.cost(),.20,"Costly outcomes");chance(h,odds.failure(),.40,"Failure outcomes");
        h.assertTrue(person.equals(original),"Preview never consumes preparation or commits dice");
        person.getCompound("Story").putBoolean("Prepared",true);chance(h,LifeStoryChecks.odds(person,choice).success(),.84,"Prepared higher-of-two dice");
        person.putInt("TrainingStrain",70);chance(h,LifeStoryChecks.odds(person,choice).success(),.60,"Preparation and fatigue cancel");
        person.getCompound("Story").putBoolean("Prepared",false);chance(h,LifeStoryChecks.odds(person,choice).success(),.36,"Fatigue lower-of-two dice");
        person.putInt("TrainingStrain",0);person.putBoolean("FuturePrepared",true);chance(h,LifeStoryChecks.odds(person,choice).success(),.84,"Future preparation applies");
        person.putBoolean("FuturePrepared",false);person.putInt("Comprehension",40);person.putInt("Alchemy",0);
        chance(h,LifeStoryChecks.odds(person,choice).success(),.35,"Weaker simulated person has lower chance");
        person.putInt("Comprehension",0);chance(h,LifeStoryChecks.odds(person,choice).success(),.20,"Natural twenty still improves a below-target outcome");
        h.assertTrue(LifeSimulationDice.degree(20,-4,17)==2&&LifeSimulationDice.degree(1,9,11)==0,"Natural twenty and one retain their special outcome shifts");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void extendedStoryOddsAccountForFutureFatigueAndCurrentProgress(GameTestHelper h) {
        var person=oddsPerson();var choice=oddsChoice(true);
        chance(h,LifeStoryChecks.odds(person,choice).success(),.4752,"Three passes before two failures, not just one-roll chance");
        person.putInt("TrainingStrain",64);chance(h,LifeStoryChecks.odds(person,choice).success(),.3509376,"Costly successes can introduce disadvantage mid-challenge");
        person.putBoolean("FuturePrepared",true);var original=person.copy();
        chance(h,LifeStoryChecks.odds(person,choice).success(),.453696,"Future preparation helps first roll only");
        h.assertTrue(person.equals(original),"Extended preview leaves saved progress and preparation untouched");
        person.getCompound("Story").putInt("Successes",2);person.getCompound("Story").putInt("Failures",1);
        chance(h,LifeStoryChecks.odds(person,choice).success(),.84,"At two successes and one failure only the next roll is decisive");
        person.putBoolean("FuturePrepared",false);person.putInt("TrainingStrain",70);
        chance(h,LifeStoryChecks.odds(person,choice).success(),.36,"Current progress retains current fatigue");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void savedDuplicateSpellDecksUpgradeWithoutResettingOtherCards(GameTestHelper h) {
        var state=life();var known=new ListTag();known.add(StringTag.valueOf("fireball"));known.add(StringTag.valueOf("wind_blade"));state.put("DiscoveredSpells",known);
        var old=LifeCardBattle.starter();old.set(0,StringTag.valueOf("spell_fireball"));old.set(1,StringTag.valueOf("spell_fireball"));
        old.set(2,StringTag.valueOf("spell_wind_blade"));old.set(3,StringTag.valueOf("spell_wind_blade"));old.set(4,StringTag.valueOf("heavy"));
        state.put("CardDeck",old.copy());var battle=state.getCompound("Battle");battle.put("RegularDeck",old.copy());battle.putString("CardPhase","player");battle.putInt("Energy",2);
        var hand=new ListTag();var discard=new ListTag();var draw=new ListTag();var identities=new HashSet<String>();
        for(int n=0;n<old.size();n++){var card=new CompoundTag();String id=UUID.randomUUID().toString();identities.add(id);card.putString("Instance",id);card.putString("Card",old.getString(n));if(n<3)hand.add(card);else if(n==3)discard.add(card);else draw.add(card);}
        battle.put("Hand",hand);battle.put("DiscardPile",discard);battle.put("DrawPile",draw);battle.put("Exhausted",new ListTag());
        var frames=new ListTag();var frame=new CompoundTag();frame.put("Roll",LifeSimulationDice.check(RandomSource.create(15),"attack",4,17,false,false));frames.add(frame);battle.put("Frames",frames);var committed=frames.copy();
        int revision=battle.getInt("Revision");h.assertTrue(LifeCardBattle.migrateSpellDeck(state),"Old duplicate spell selection migrates");
        var repaired=battle.getList("RegularDeck",Tag.TAG_STRING);
        h.assertTrue(repaired.size()==15&&LifeCardBattle.deckIllegal(state,repaired).isBlank(),"Upgrade retains legal fifteen-card deck");
        for(String id:java.util.List.of("spell_fireball","spell_wind_blade","heavy"))h.assertTrue(repaired.stream().filter(t->t.getAsString().equals(id)).count()==1,"Retain first spell and unrelated advanced attack");
        var afterIds=new HashSet<String>();var spellCounts=new java.util.HashMap<String,Integer>();
        for(String pile:java.util.List.of("Hand","DiscardPile","DrawPile","Exhausted"))for(var tag:battle.getList(pile,Tag.TAG_COMPOUND)){
            var card=(CompoundTag)tag;afterIds.add(card.getString("Instance"));if(card.getString("Card").startsWith("spell_"))spellCounts.merge(card.getString("Card"),1,Integer::sum);}
        h.assertTrue(afterIds.equals(identities)&&spellCounts.values().stream().allMatch(n->n==1),"Active piles retain all identities and one copy per spell");
        h.assertTrue(battle.getInt("Energy")==2&&battle.getList("Frames",Tag.TAG_COMPOUND).equals(committed)&&battle.getInt("Revision")==revision+1,"Committed rolls and energy untouched; stale input revision invalidated");
        var saved=state.copy();h.assertTrue(!LifeCardBattle.migrateSpellDeck(state)&&state.equals(saved),"Migration is idempotent");
        h.assertTrue(LifeCardCatalog.get(state,LifeCardBattle.player(state),"spell_fireball").copies()==1&&LifeCardCatalog.get(state,LifeCardBattle.player(state),"spell_fireball").energy()==1,"One spell copy never increases its original Energy cost");
        var fresh=state.copy();fresh.remove("Battle");fresh.put("CardDeck",old.copy());var definition=new CompoundTag();var enemy=new CompoundTag();enemy.putInt("realm",4);var enemies=new ListTag();enemies.add(enemy);definition.put("enemies",enemies);
        LifeSimulationBattle.start(fresh,definition,RandomSource.create(3));h.assertTrue(fresh.getList("CardDeck",Tag.TAG_STRING).stream().filter(t->t.getAsString().equals("spell_fireball")).count()==1,"Loading saved duplicate deck retains spell instead of resetting to starter");
        h.succeed();
    }
}
