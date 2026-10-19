package com.example.immortal_cultivation_mod.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Authoritative card combat. Every mutation is protected by battle UUID and revision. */
public final class LifeCardBattle {
    public static final int VERSION=1,DECK_SIZE=15,CONSUMABLE_SLOTS=5,HAND_SIZE=5;
    private LifeCardBattle() {}
    public static boolean enabled(CompoundTag battle){return battle.getInt("CardBattleVersion")>0;}
    public static CompoundTag player(CompoundTag life){return LifeSimulationBattle.find(life.getCompound("Battle"),"player");}
    public static ListTag starter(){
        ListTag deck=new ListTag();for(int i=0;i<5;i++)deck.add(StringTag.valueOf("strike"));
        for(int i=0;i<5;i++)deck.add(StringTag.valueOf("block"));
        for(int i=0;i<2;i++)deck.add(StringTag.valueOf("dodge"));
        for(int i=0;i<2;i++)deck.add(StringTag.valueOf("recover"));deck.add(StringTag.valueOf("run"));return deck;
    }
    public static String deckIllegal(CompoundTag life,ListTag deck){
        if(deck.size()!=DECK_SIZE)return "deck_size";
        Map<String,Integer> count=new HashMap<>();CompoundTag actor=player(life);
        for(var tag:deck){var card=LifeCardCatalog.get(life,actor,tag.getAsString());
            if(card==null||card.consumable())return "unknown_card";
            if(count.merge(card.id(),1,Integer::sum)>card.copies())return card.id().startsWith("spell_")?"spell_copies":"copies";}
        return "";
    }

    private static ListTag uniqueSpellDeck(ListTag original) {
        if(original.size()!=DECK_SIZE)return original;
        ListTag repaired=original.copy();var counts=new HashMap<String,Integer>();
        List<Integer> duplicates=new ArrayList<>();
        for(int i=0;i<repaired.size();i++){
            String id=repaired.getString(i);
            if(id.startsWith("spell_")&&counts.containsKey(id))duplicates.add(i);
            else counts.merge(id,1,Integer::sum);
        }
        for(int index:duplicates){
            String filler=LifeCardCatalog.BASICS.stream().filter(id->counts.getOrDefault(id,0)<5).findFirst().orElseThrow();
            repaired.set(index,StringTag.valueOf(filler));counts.merge(filler,1,Integer::sum);
        }
        return repaired;
    }

    /** Upgrade saved decks without losing other selections, card identities, or committed results. */
    public static boolean migrateSpellDeck(CompoundTag life) {
        boolean changed=false;
        ListTag saved=life.getList("CardDeck",Tag.TAG_STRING),fixed=uniqueSpellDeck(saved);
        if(!saved.equals(fixed)){life.put("CardDeck",fixed);changed=true;}
        CompoundTag battle=life.getCompound("Battle");
        if(!enabled(battle))return changed;
        ListTag regular=battle.getList("RegularDeck",Tag.TAG_STRING);
        if(regular.size()!=DECK_SIZE)return changed;
        ListTag repaired=uniqueSpellDeck(regular);
        boolean battleChanged=!regular.equals(repaired);
        var counts=new HashMap<String,Integer>();var wanted=new HashMap<String,Integer>();
        for(var tag:repaired)wanted.merge(tag.getAsString(),1,Integer::sum);
        List<CompoundTag> extras=new ArrayList<>();
        // Keep a spell already in hand first. Replacing an excess copy never rerolls or reshuffles.
        for(String pile:List.of("Hand","DiscardPile","DrawPile","Exhausted"))for(var tag:battle.getList(pile,Tag.TAG_COMPOUND)){
            CompoundTag card=(CompoundTag)tag;String id=card.getString("Card");
            if(id.startsWith("item_"))continue;
            if(id.startsWith("spell_")&&counts.containsKey(id))extras.add(card);
            else counts.merge(id,1,Integer::sum);
        }
        for(var card:extras){
            String filler=LifeCardCatalog.BASICS.stream().filter(id->counts.getOrDefault(id,0)<wanted.getOrDefault(id,0))
                    .findFirst().orElseGet(()->LifeCardCatalog.BASICS.stream().filter(id->counts.getOrDefault(id,0)<5).findFirst().orElseThrow());
            card.putString("Card",filler);counts.merge(filler,1,Integer::sum);battleChanged=true;
        }
        if(battleChanged){
            battle.put("RegularDeck",repaired);life.put("CardDeck",repaired.copy());
            battle.putInt("Revision",battle.getInt("Revision")+1);
        }
        return changed||battleChanged;
    }
    public static void initialize(CompoundTag life){
        CompoundTag b=life.getCompound("Battle");if(b.isEmpty())return;
        if(enabled(b)){migrateSpellDeck(life);return;}
        b.putInt("CardBattleVersion",VERSION);b.putString("CardPhase","prepare");b.putInt("Energy",0);
        CompoundTag p=player(life);
        if(b.getInt("Round")>1&&!life.contains("CombatHp")){life.putDouble("CombatHp",p.getDouble("Hp"));life.putDouble("CombatQi",p.getDouble("Qi"));}
        restoreActor(life,p);
        migrateSpellDeck(life);
        ListTag deck=life.getList("CardDeck",Tag.TAG_STRING).copy();
        if(!deckIllegal(life,deck).isBlank())deck=starter();
        if(b.getBoolean("ObjectiveEnabled")&&!life.contains("CardDeck")){
            deck.set(8,StringTag.valueOf("inspect"));deck.set(9,StringTag.valueOf("objective"));
        }
        b.put("RegularDeck",deck);life.put("CardDeck",deck.copy());
        ListTag selection=new ListTag();for(int i=0;i<CONSUMABLE_SLOTS;i++)selection.add(StringTag.valueOf(""));
        List<String> available=consumables(life);int slot=0;
        for(String id:available)for(int n=0;n<LifeSimulationBattle.itemCount(life,id)&&slot<CONSUMABLE_SLOTS;n++)selection.set(slot++,StringTag.valueOf(id));
        b.put("ConsumableSlots",selection);
        for(String pile:List.of("DrawPile","Hand","DiscardPile","Exhausted"))b.put(pile,new ListTag());
        b.put("Frames",new ListTag());b.putInt("Reveal",0);
        // Keep authored opponents and anchors in saved state; excess opponents enter as waves.
        ListTag actors=b.getList("Actors",Tag.TAG_COMPOUND);
        for(int i=actors.size()-1;i>=0;i--){CompoundTag a=actors.getCompound(i);
            if(!a.getBoolean("Enemy")&&!a.getString("Id").equals("player")&&!a.getString("Kind").equals("anchor"))actors.remove(i);}
        selectCompanion(life,life.getBoolean("CardNoCompanion")?"none":life.hasUUID("CardCompanion")?life.getUUID("CardCompanion").toString():"");
        limitActors(b);sync(life,b);
    }
    private static void restoreActor(CompoundTag person,CompoundTag actor){
        actor.putDouble("Hp",Math.min(actor.getDouble("MaxHp"),LifeCardInjuries.hp(person)));
        if(person.contains("CombatQi"))actor.putDouble("Qi",Mth.clamp(person.getDouble("CombatQi"),0,actor.getDouble("MaxQi")));
        actor.putInt("KnockoutStreak",person.getInt("KnockoutStreak"));actor.put("DefenseQueue",new ListTag());
        actor.putInt("CurrentRound",1);actor.put("CardEffects",person.getList("CombatEffects",Tag.TAG_COMPOUND).copy());
        CompoundTag buffs=person.getCompound("CombatBuffs").copy();for(String key:buffs.getAllKeys()){CompoundTag effect=buffs.getCompound(key);effect.putInt("Until",1+effect.getInt("Remaining"));}
        actor.put("CardBuffs",buffs);
    }
    public static List<CompoundTag> companions(CompoundTag life){
        CompoundTag b=life.getCompound("Battle");List<UUID> opponents=new ArrayList<>();
        for(var t:b.getList("Actors",Tag.TAG_COMPOUND)){CompoundTag a=(CompoundTag)t;if(a.getBoolean("Enemy")&&a.hasUUID("NpcId"))opponents.add(a.getUUID("NpcId"));}
        return life.getList("Relationships",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
            .filter(p->LifeSimulationBattle.eligibleAlly(p)&&p.getInt("Age")>=16&&!p.getBoolean("CombatDead")&&LifeCardInjuries.hp(p)>0&&!opponents.contains(p.getUUID("Id"))).toList();
    }
    private static void selectCompanion(CompoundTag life,String id){
        CompoundTag b=life.getCompound("Battle");ListTag actors=b.getList("Actors",Tag.TAG_COMPOUND);
        for(int i=actors.size()-1;i>=0;i--){CompoundTag a=actors.getCompound(i);if(a.getBoolean("Companion"))actors.remove(i);}
        if(id.equals("none")){life.remove("CardCompanion");life.putBoolean("CardNoCompanion",true);return;}
        var choices=companions(life);CompoundTag selected=choices.stream().filter(p->p.getUUID("Id").toString().equals(id)).findFirst().orElse(choices.isEmpty()?new CompoundTag():choices.getFirst());
        if(selected.isEmpty())return;
        CompoundTag actor=LifeSimulationBattle.actor(selected.getUUID("Id").toString(),selected.getString("NameKey").isBlank()?selected.getString("Name"):selected.getString("NameKey"),selected.getInt("Realm"),false,LifeSimulationSprites.assignPerson(selected),"ally");
        actor.putUUID("NpcId",selected.getUUID("Id"));actor.putBoolean("Companion",true);
        actor.put("Moves",selected.getList("Moves",Tag.TAG_COMPOUND).copy());
        actor.put("DiscoveredSpells",selected.getList("DiscoveredSpells",Tag.TAG_STRING).copy());
        actor.putInt("Combat",selected.getInt("Combat"));actor.putInt("InsightModifier",LifeSimulationDice.attribute(selected.getInt("Comprehension")));
        actor.putInt("Proficiency",Mth.clamp(selected.getInt("Combat")/30,0,4));life.remove("CardNoCompanion");
        actor.putString("Behavior",life.getString("CardCompanionBehavior").isBlank()?"offensive":life.getString("CardCompanionBehavior"));
        restoreActor(selected,actor);actors.add(actor);life.putUUID("CardCompanion",selected.getUUID("Id"));
    }
    public static List<String> consumables(CompoundTag life){
        List<String> result=new ArrayList<>();for(var tag:life.getList("Inventory",Tag.TAG_COMPOUND)){
            CompoundTag entry=(CompoundTag)tag;String id=entry.getString("Id");
            if(entry.getInt("Count")>0&&LifeCardCatalog.item(player(life),id)!=null&&!result.contains(id))result.add(id);}
        return List.copyOf(result);
    }
    public static boolean active(CompoundTag actor){return actor.getDouble("Hp")>0&&!actor.getBoolean("KnockedOut")&&LifeSimulationBattleMechanics.combatant(actor);}
    private static void limitActors(CompoundTag b){
        int enemies=0,companions=0;
        for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){
            CompoundTag a=(CompoundTag)tag;if(a.getString("Kind").equals("anchor")||a.getString("Id").equals("player"))continue;
            if(a.getDouble("Hp")<=0||a.getBoolean("KnockedOut")||a.getBoolean("Hidden"))continue;
            if(a.getBoolean("Enemy"))a.putBoolean("Reserve",enemies++>=2);
            else a.putBoolean("Reserve",companions++>=1);
        }
    }
    public static void handle(CompoundTag life,String payload,RandomSource random){
        CompoundTag b=life.getCompound("Battle");if(!enabled(b)||!b.hasUUID("Id"))return;
        migrateSpellDeck(life);
        String[] parts=payload.split(":",5);if(parts.length<3||!b.getUUID("Id").toString().equals(parts[0]))return;
        int revision;try{revision=Integer.parseInt(parts[1]);}catch(NumberFormatException e){return;}
        if(revision!=b.getInt("Revision"))return;
        String command=parts[2];boolean accepted=false;
        if((command.equals("reveal")||command.equals("skip"))&&LifeSimulationBattle.pending(b)){
            b.putInt("Reveal",command.equals("skip")?b.getList("Frames",Tag.TAG_COMPOUND).size():b.getInt("Reveal")+1);accepted=true;
            if(!LifeSimulationBattle.pending(b)&&b.getString("CardPhase").equals("enemy")&&b.getString("Result").isBlank())beginTurn(life,b,random);
        }else if(!LifeSimulationBattle.pending(b)&&b.getString("Result").isBlank()){
            if(b.getString("CardPhase").equals("prepare"))accepted=prepare(life,b,parts,random);
            else if(b.getString("CardPhase").equals("player")){
                if(command.equals("play")&&parts.length==5&&illegal(life,parts[3],parts[4]).isBlank()){
                    play(life,b,parts[3],parts[4],random);accepted=true;
                }else if(command.equals("end")){endTurn(life,b,random);accepted=true;}
            }
        }
        if(accepted){b.putInt("Revision",revision+1);sync(life,b);}
    }
    private static boolean prepare(CompoundTag life,CompoundTag b,String[] p,RandomSource random){
        if(p[2].equals("ready")){
            if(player(life).getDouble("Hp")<=0||!deckIllegal(life,b.getList("RegularDeck",Tag.TAG_STRING)).isBlank())return false;
            ListTag pile=b.getList("DrawPile",Tag.TAG_COMPOUND);
            for(var tag:b.getList("RegularDeck",Tag.TAG_STRING))pile.add(instance(tag.getAsString()));
            Map<String,Integer> counts=new HashMap<>();
            for(var tag:b.getList("ConsumableSlots",Tag.TAG_STRING)){
                String id=tag.getAsString();if(id.isBlank())continue;
                if(LifeCardCatalog.item(player(life),id)!=null&&counts.merge(id,1,Integer::sum)<=LifeSimulationBattle.itemCount(life,id))pile.add(instance("item_"+id.replace(':','~')));
            }
            shuffle(pile,random);b.putString("CardPhase","player");b.putInt("Round",1);beginTurn(life,b,random);return true;
        }
        if(p[2].equals("withdraw")){b.putString("Result","escape");return true;}
        if(p[2].equals("deck")&&p.length==5){
            int slot=integer(p[3]);ListTag deck=b.getList("RegularDeck",Tag.TAG_STRING).copy();
            if(slot<0||slot>=DECK_SIZE)return false;deck.set(slot,StringTag.valueOf(p[4]));
            if(!deckIllegal(life,deck).isBlank())return false;b.put("RegularDeck",deck);life.put("CardDeck",deck.copy());return true;
        }
        if(p[2].equals("slot")&&p.length==5){
            int slot=integer(p[3]);String id=p[4].replace('~',':');if(slot<0||slot>=5)return false;
            ListTag selected=b.getList("ConsumableSlots",Tag.TAG_STRING).copy();
            if(!id.equals("none")&&(!consumables(life).contains(id)))return false;
            selected.set(slot,StringTag.valueOf(id.equals("none")?"":id));
            long count=selected.stream().filter(t->t.getAsString().equals(id)).count();
            if(!id.equals("none")&&count>LifeSimulationBattle.itemCount(life,id))return false;
            b.put("ConsumableSlots",selected);return true;
        }
        if(p[2].equals("companion")&&p.length==4){
            if(!p[3].equals("none")&&companions(life).stream().noneMatch(n->n.getUUID("Id").toString().equals(p[3])))return false;
            selectCompanion(life,p[3]);return true;
        }
        if(p[2].equals("behavior")&&p.length==4&&List.of("offensive","defensive","supportive").contains(p[3])){
            life.putString("CardCompanionBehavior",p[3]);for(var tag:b.getList("Actors",Tag.TAG_COMPOUND))if(((CompoundTag)tag).getBoolean("Companion"))((CompoundTag)tag).putString("Behavior",p[3]);return true;
        }
        return false;
    }
    private static int integer(String text){try{return Integer.parseInt(text);}catch(NumberFormatException e){return -1;}}
    private static CompoundTag instance(String id){CompoundTag card=new CompoundTag();card.putString("Instance",UUID.randomUUID().toString());card.putString("Card",id);return card;}
    private static void shuffle(ListTag pile,RandomSource random){for(int i=pile.size()-1;i>0;i--){int j=random.nextInt(i+1);Tag a=pile.get(i);pile.set(i,pile.get(j));pile.set(j,a);}}
    private static void draw(CompoundTag b,RandomSource random){
        ListTag draw=b.getList("DrawPile",Tag.TAG_COMPOUND),discard=b.getList("DiscardPile",Tag.TAG_COMPOUND),hand=b.getList("Hand",Tag.TAG_COMPOUND);
        while(hand.size()<HAND_SIZE){if(draw.isEmpty()){if(discard.isEmpty())break;draw.addAll(discard);discard.clear();shuffle(draw,random);}hand.add(draw.remove(0));}
    }
    private static void beginTurn(CompoundTag life,CompoundTag b,RandomSource random){
        b.putString("CardPhase","player");b.putInt("Energy",3);b.putInt("CardsPlayed",0);
        limitActors(b);for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){
            CompoundTag a=(CompoundTag)tag;a.put("DefenseQueue",new ListTag());a.remove("Guard");a.putInt("CurrentRound",b.getInt("Round"));}
        LifeSimulationBattleMechanics.startRound(life,b,random);
        commitIntents(life,b);draw(b,random);
    }
    public static CompoundTag handCard(CompoundTag b,String instance){
        for(var tag:b.getList("Hand",Tag.TAG_COMPOUND))if(((CompoundTag)tag).getString("Instance").equals(instance))return (CompoundTag)tag;return new CompoundTag();
    }
    public static String illegal(CompoundTag life,String instance,String targetId){
        CompoundTag b=life.getCompound("Battle"),p=player(life),entry=handCard(b,instance),target=LifeSimulationBattle.find(b,targetId);
        if(!enabled(b)||LifeSimulationBattle.pending(b)||!b.getString("Result").isBlank()||!b.getString("CardPhase").equals("player"))return "busy";
        if(entry.isEmpty())return "unknown_card";var card=LifeCardCatalog.get(life,p,entry.getString("Card"));if(card==null)return "unknown_card";
        if(b.getInt("CardsPlayed")>=20)return "turn_limit";
        if(b.getInt("Energy")<card.energy())return "energy";
        if(p.getDouble("Qi")<card.qi())return "qi";
        if(card.effect().equals("gather")&&p.getDouble("GatherQi")>0)return "active";
        if(p.getCompound("Cooldowns").getInt(card.id())>b.getInt("Round"))return "cooldown";
        if(card.consumable()&&LifeSimulationBattle.itemCount(life,card.item())<=0)return "item";
        if(card.target().equals("self"))return targetId.equals("player")?"":"target";
        if(target.isEmpty()||target.getDouble("Hp")<=0||target.getBoolean("Reserve")||target.getBoolean("Hidden")||target.getBoolean("KnockedOut")||target.getBoolean("Withdrawn"))return "target";
        if(target.getBoolean("Locked"))return "stage";
        if(card.target().equals("enemy")&&(!target.getBoolean("Enemy")||target.getBoolean("Protected")))return "target";
        if(card.target().equals("friend")&&target.getBoolean("Enemy"))return "target";
        if(card.effect().equals("objective"))return LifeSimulationBattle.objectiveIllegal(life,targetId);
        return "";
    }
    private static void play(CompoundTag life,CompoundTag b,String instance,String targetId,RandomSource random){
        CompoundTag entry=handCard(b,instance),p=player(life),target=LifeSimulationBattle.find(b,targetId);
        var card=LifeCardCatalog.get(life,p,entry.getString("Card"));
        if(card.consumable()&&!consume(life,card.item()))return;
        resetFrames(b);b.putInt("Energy",b.getInt("Energy")-card.energy());b.putInt("CardsPlayed",b.getInt("CardsPlayed")+1);
        p.putDouble("Qi",p.getDouble("Qi")-card.qi());
        if(card.id().startsWith("spell_"))p.getCompound("CardBuffs").remove("NEXT_QI_SPELL");
        if(card.id().startsWith("spell_")){int ticks=com.example.immortal_cultivation_mod.spell.ModSpells.cooldownTicks(card.id().substring(6));
            CompoundTag cooldowns=p.getCompound("Cooldowns");cooldowns.putInt(card.id(),b.getInt("Round")+(int)Math.ceil(ticks/20.));p.put("Cooldowns",cooldowns);}
        p.putString("Plan",card.id());b.putString("ReservedAction",card.id());
        resolve(life,b,p,target,card,random);
        LifeSimulationBattleMechanics.afterAction(life,b,p,random);checkEnd(life,b);frame(b);
        b.getList("Hand",Tag.TAG_COMPOUND).remove(entry);
        b.getList(card.consumable()?"Exhausted":"DiscardPile",Tag.TAG_COMPOUND).add(entry);
    }
    private static boolean consume(CompoundTag life,String id){
        for(var tag:life.getList("Inventory",Tag.TAG_COMPOUND)){CompoundTag item=(CompoundTag)tag;if(item.getString("Id").equals(id)&&item.getInt("Count")>0){item.putInt("Count",item.getInt("Count")-1);return true;}}
        return false;
    }
    private static void resetFrames(CompoundTag b){b.put("Frames",new ListTag());b.putInt("Reveal",0);b.put("RoundStartActors",b.getList("Actors",Tag.TAG_COMPOUND).copy());}
    private static void frame(CompoundTag b){
        CompoundTag f=b.getCompound("LastDetail").copy();f.put("Actors",b.getList("Actors",Tag.TAG_COMPOUND).copy());
        f.putInt("Phase",b.getInt("Phase"));f.putString("PhaseKey",b.getString("PhaseKey"));f.putInt("Objective",b.getInt("Objective"));f.putInt("Hazard",b.getInt("Hazard"));f.putBoolean("Applied",true);
        b.getList("Frames",Tag.TAG_COMPOUND).add(f);
    }
    /** Read-only numerical preview; never rolls dice or consumes the target's prepared defenses. */
    public record CardPreview(double base,double minimum,double maximum,double initialMinimum,double initialMaximum,int turns,double beforeDefense) {}
    public static CardPreview preview(CompoundTag actor,CompoundTag target,LifeCardCatalog.Card card){
        double base=card.effect().equals("attack")?card.baseTotal():card.amount();
        double low=base*.95,high=base*1.05,firstLow=low,firstHigh=high;
        int turns=0;
        switch(card.effect()){
            case "attack" -> {low=previewDamage(actor,target,card,.95);high=previewDamage(actor,target,card,1.05);}
            case "heal","qi","shield","clock_morning" -> {
                double room=restoreRoom(actor,target,card);
                low=Math.min(room,low);high=Math.min(room,high);
            }
            case "heal_over_time","qi_over_time" -> {
                turns=Math.max(1,card.duration());double room=restoreRoom(actor,target,card);
                firstLow=Math.min(room,low/turns);firstHigh=Math.min(room,high/turns);
            }
            case "block" -> {low=Mth.clamp(low,0,.95)*100;high=Mth.clamp(high,0,.95)*100;}
            default -> { }
        }
        if(turns==0){firstLow=low;firstHigh=high;}
        double beforeDefense=card.effect().equals("attack")?base*(1+targetAttackBonus(attackBonus(actor,card),card,target)/100.):base;
        return new CardPreview(base,low,high,firstLow,firstHigh,turns,beforeDefense);
    }
    private static double restoreRoom(CompoundTag actor,CompoundTag target,LifeCardCatalog.Card card){
        if(card.effect().equals("shield")||card.effect().equals("clock_morning"))return Math.max(0,target.getDouble("MaxHp")*.4-target.getDouble("Shield"));
        String kind=card.effect().startsWith("qi")?"Qi":"Hp";
        double current=target.getDouble(kind);
        // Spell costs are paid before resolving a self-targeted recovery card.
        if(kind.equals("Qi")&&actor.getString("Id").equals(target.getString("Id")))current-=card.qi();
        return Math.max(0,target.getDouble("Max"+kind)-current);
    }
    private static double previewDamage(CompoundTag actor,CompoundTag target,LifeCardCatalog.Card card,double factor){
        CompoundTag copy=target.copy();double hp=copy.getDouble("Hp");
        double bonus=targetAttackBonus(attackBonus(actor,card),card,copy);
        for(double base:card.hits()){
            if(copy.getDouble("Hp")<=0||copy.getBoolean("KnockedOut"))break;
            applyHit(copy,base*factor*(1+bonus/100.),armorBypass(card),element(card));
        }
        return Math.max(0,hp-copy.getDouble("Hp"));
    }
    private static String element(LifeCardCatalog.Card card){
        if(card.id().startsWith("spell_")){
            var spell=com.example.immortal_cultivation_mod.spell.ModSpells.get(card.id().substring(6));
            if(spell!=null)return spell.element()
                    .replace(com.example.immortal_cultivation_mod.attachment.SpiritRoots.FIRE,"fire")
                    .replace(com.example.immortal_cultivation_mod.attachment.SpiritRoots.ICE,"ice");
        }
        return card.id().contains("fireball")?"fire":"physical";
    }
    private static double attackBonus(CompoundTag actor,LifeCardCatalog.Card card){
        String element=element(card);
        return (element.contains("fire")?buff(actor,"FIRE_DAMAGE"):0)+(element.equals("physical")?buff(actor,"NEXT_MELEE"):0);
    }
    private static double targetAttackBonus(double bonus,LifeCardCatalog.Card card,CompoundTag target){
        return bonus+(card.id().equals("spell_jinfeng_zhi")&&(target.getDouble("ArmorFraction")>0||!target.getList("DefenseQueue",Tag.TAG_COMPOUND).isEmpty())?25:0);
    }
    private static double armorBypass(LifeCardCatalog.Card card){
        return card.id().equals("spell_jinfeng_zhi")?.10:card.id().equals("spell_wanren_chuanxin")?.15:card.status().equals("pierce")?.5:0;
    }
    private static void resolve(CompoundTag life,CompoundTag b,CompoundTag actor,CompoundTag target,LifeCardCatalog.Card card,RandomSource random){
        CompoundTag detail=new CompoundTag();detail.putString("Actor",actor.getString("Id"));detail.putString("Target",target.getString("Id"));detail.putString("Action",card.id());detail.putString("Outcome","success");b.put("LastDetail",detail);
        if(card.effect().equals("objective")&&b.getInt("AnchorCount")>0){LifeSimulationBattleMechanics.objective(life,b,actor,target,random,detail);return;}
        CompoundTag roll=LifeCardDice.roll(life,actor,card.effect(),random);detail.put("Roll",roll);
        double factor=roll.getDouble("Factor"),amount=card.amount()*factor;
        detail.putString("Effect",card.effect());
        detail.putBoolean("Quantitative",List.of("attack","block","heal","qi","heal_over_time","qi_over_time","buff","burn","gather","shield","clock_morning").contains(card.effect()));
        detail.putDouble("CardBase",card.effect().equals("attack")?card.baseTotal():card.amount());
        detail.putDouble("Base",card.effect().equals("attack")?card.baseTotal():card.amount());detail.putDouble("Amount",card.effect().equals("attack")?card.baseTotal()*factor:amount);
        double previousHp=target.getDouble("Hp"),previousQi=target.getDouble("Qi"),previousShield=target.getDouble("Shield");
        switch(card.effect()){
            case "block","dodge" -> {
                if(card.effect().equals("block")){actor.putBoolean("Guard",true);actor.putInt("DefendedRound",b.getInt("Round"));}
                CompoundTag defense=new CompoundTag();defense.putString("Kind",card.effect());defense.put("Roll",roll.copy());
                if(card.effect().equals("dodge"))defense.putBoolean("Success",LifeCardDice.binary(roll,card.amount()));
                else defense.putDouble("Strength",Mth.clamp(amount,0,.95));
                actor.getList("DefenseQueue",Tag.TAG_COMPOUND).add(defense);
                if(!actor.contains("DefenseQueue")){ListTag queue=new ListTag();queue.add(defense);actor.put("DefenseQueue",queue);}
            }
            case "run" -> {if(LifeCardDice.binary(roll,card.amount()))b.putString("Result","escape");else detail.putString("Outcome","failure");}
            case "heal" -> target.putDouble("Hp",Math.min(target.getDouble("MaxHp"),target.getDouble("Hp")+amount));
            case "qi" -> target.putDouble("Qi",Math.min(target.getDouble("MaxQi"),target.getDouble("Qi")+amount));
            case "heal_over_time","qi_over_time" -> {
                String kind=card.effect().equals("heal_over_time")?"Hp":"Qi";
                double perTurn=amount/Math.max(1,card.duration());target.putDouble(kind,Math.min(target.getDouble("Max"+kind),target.getDouble(kind)+perTurn));
                CompoundTag effect=new CompoundTag();effect.putString("Kind",kind);effect.putDouble("Amount",perTurn);effect.putInt("Remaining",Math.max(0,card.duration()-1));
                effect.putInt("NextRound",b.getInt("Round")+1);
                ListTag effects=target.getList("CardEffects",Tag.TAG_COMPOUND);effects.add(effect);target.put("CardEffects",effects);
            }
            case "buff" -> {
                CompoundTag buffs=target.getCompound("CardBuffs"),buff=new CompoundTag();buff.putDouble("Amount",amount);buff.putInt("Until",b.getInt("Round")+card.duration());buffs.put(card.status(),buff);target.put("CardBuffs",buffs);
                if(card.status().equals("FIRE_RESIST"))target.remove("BurnUntil");
            }
            case "burn" -> {target.putInt("BurnUntil",b.getInt("Round")+card.duration());target.putDouble("BurnDamage",amount);}
            case "gather" -> actor.putDouble("GatherQi",amount);
            case "shield","clock_morning" -> {
                target.putDouble("Shield",Math.min(target.getDouble("MaxHp")*.4,target.getDouble("Shield")+amount));
                if(card.effect().equals("clock_morning")){target.putDouble("DelayedHeal",actor.getDouble("Power")*.5*factor);target.putInt("DelayedHealRound",b.getInt("Round")+1);detail.putDouble("DelayedHeal",target.getDouble("DelayedHeal"));}
            }
            case "cleanse" -> {target.remove("BurnUntil");target.remove("SlowUntil");target.remove("PressureUntil");target.remove("ObscuredUntil");}
            case "inspect" -> LifeSimulationBattleMechanics.inspect(life,b,target);
            case "objective" -> {
                // Authored checks are discrete outcomes; do not replace their DC with damage dice.
                if(!LifeSimulationBattleMechanics.objective(life,b,actor,target,random,detail)){
                    boolean success=LifeCardDice.binary(roll,.65);
                    if(success)b.putInt("Objective",b.getInt("Objective")+1);else b.putInt("Hazard",b.getInt("Hazard")+1);
                }
            }
            case "attack" -> {
                String element=element(card);double bonus=attackBonus(actor,card);
                if(element.equals("physical"))actor.getCompound("CardBuffs").remove("NEXT_MELEE");
                double bypass=armorBypass(card);
                List<CompoundTag> targets=card.area()?b.getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(a->active(a)&&a.getBoolean("Enemy")!=actor.getBoolean("Enemy")&&!a.getBoolean("Protected")).toList():List.of(target);
                ListTag hits=new ListTag();for(var victim:targets){LifeSimulationBattleMechanics.beforeHit(b,actor,victim,detail);
                    double targetBonus=targetAttackBonus(bonus,card,victim);
                    for(double base:card.hits()){
                    if(victim.getDouble("Hp")<=0||victim.getBoolean("KnockedOut"))break;
                    CompoundTag hit=applyHit(victim,base*factor*(1+targetBonus/100.),bypass,element);hit.putDouble("Base",base);hits.add(hit);
                    if(hit.getDouble("HpDamage")>0){
                        if(card.status().equals("burn")){victim.putInt("BurnUntil",b.getInt("Round")+card.duration());victim.putDouble("BurnDamage",factor);}
                        if(card.status().equals("slow"))victim.putInt("SlowUntil",b.getInt("Round")+card.duration());
                        if(card.status().equals("pressure"))victim.putInt("PressureUntil",b.getInt("Round")+card.duration());
                        if(card.status().equals("obscure"))victim.putInt("ObscuredUntil",b.getInt("Round")+card.duration());
                        if(card.status().equals("disrupt"))victim.putBoolean("Interrupted",true);
                    }
                    knockout(life,b,victim);
                }}detail.put("Hits",hits);double rolled=hits.stream().map(t->(CompoundTag)t).filter(t->t.getString("Target").equals(target.getString("Id"))).mapToDouble(t->t.getDouble("Raw")).sum();detail.putDouble("Amount",rolled);detail.putDouble("Base",rolled/factor);
            }
            case "channel" -> actor.putBoolean("Channelled",true);
            default -> { }
        }
        switch(card.effect()){
            case "attack" -> detail.putDouble("AppliedAmount",detail.getList("Hits",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(t->t.getString("Target").equals(target.getString("Id"))).mapToDouble(t->t.getDouble("HpDamage")).sum());
            case "heal","heal_over_time" -> detail.putDouble("AppliedAmount",Math.max(0,target.getDouble("Hp")-previousHp));
            case "qi","qi_over_time" -> detail.putDouble("AppliedAmount",Math.max(0,target.getDouble("Qi")-previousQi));
            case "shield","clock_morning" -> detail.putDouble("AppliedAmount",Math.max(0,target.getDouble("Shield")-previousShield));
            default -> { }
        }
        if(List.of("heal_over_time","qi_over_time").contains(card.effect()))detail.putDouble("PerTurn",amount/Math.max(1,card.duration()));
        if(List.of("dodge","run").contains(card.effect())||List.of("spell_leishan","spell_canglang_chong").contains(card.id()))actor.remove("GatherQi");
        if(card.consumable())pillExtras(life,b,actor,card,roll.getDouble("Factor"));
        if(actor.getDouble("Hp")>=actor.getDouble("MaxHp")-.000001)actor.putInt("KnockoutStreak",0);
        if(!target.isEmpty()&&target.getDouble("Hp")>=target.getDouble("MaxHp")-.000001)target.putInt("KnockoutStreak",0);
    }
    /** One queue entry per hit. Failed dodges consume their entry and mitigate nothing. */
    public static CompoundTag applyHit(CompoundTag target,double raw,boolean pierce){
        return applyHit(target,raw,pierce?.5:0,"physical");
    }
    private static CompoundTag applyHit(CompoundTag target,double raw,double bypass,String element){
        CompoundTag hit=new CompoundTag();hit.putString("Target",target.getString("Id"));hit.putDouble("Raw",raw);
        double damage=Math.max(0,raw)*(1-Mth.clamp(target.getDouble("ArmorFraction")*(1-bypass),0,.95));
        double reduction=buff(target,"DEFENSE")+buff(target,"IRON_BONES")+(element.contains("fire")?buff(target,"FIRE_RESIST"):element.contains("ice")?buff(target,"COLD_RESIST"):0);
        damage*=1-Mth.clamp(reduction/100.,0,.95);
        ListTag queue=target.getList("DefenseQueue",Tag.TAG_COMPOUND);
        if(!queue.isEmpty()){
            CompoundTag defense=(CompoundTag)queue.remove(0);hit.put("PreparedDefense",defense.copy());
            if(defense.getString("Kind").equals("dodge")&&defense.getBoolean("Success")){damage=0;hit.putString("Outcome","dodged");}
            else if(defense.getString("Kind").equals("block")){damage*=1-Mth.clamp(defense.getDouble("Strength"),0,.95);hit.putString("Outcome","blocked");}
            if(queue.stream().map(t->(CompoundTag)t).noneMatch(d->d.getString("Kind").equals("block")))target.remove("Guard");
        }
        // Existing narrative shields remain finite; they never turn Block into full immunity.
        double absorbed=Math.min(target.getDouble("Shield"),damage*.95);target.putDouble("Shield",Math.max(0,target.getDouble("Shield")-absorbed));damage-=absorbed;
        double previousHp=target.getDouble("Hp");
        target.putDouble("Hp",Math.max(0,previousHp-damage));hit.putDouble("HpDamage",Math.min(Math.max(0,previousHp),damage));hit.putDouble("Shield",absorbed);
        if(target.getDouble("Hp")>0&&target.getDouble("Hp")<target.getDouble("MaxHp")*.2&&buff(target,"EMERGENCY_REGEN")>0){
            CompoundTag regen=target.getCompound("CardBuffs").getCompound("EMERGENCY_REGEN");
            double total=target.getDouble("MaxHp")*regen.getDouble("Amount")/100.;
            CompoundTag healing=new CompoundTag();healing.putString("Kind","Hp");healing.putDouble("Amount",total/45);healing.putInt("Remaining",45);
            ListTag effects=target.getList("CardEffects",Tag.TAG_COMPOUND);effects.add(healing);target.put("CardEffects",effects);target.getCompound("CardBuffs").remove("EMERGENCY_REGEN");
        }
        if(hit.getString("Outcome").isBlank())hit.putString("Outcome","hit");hit.putBoolean("Applied",true);return hit;
    }
    public static double buff(CompoundTag actor,String id){CompoundTag buff=actor.getCompound("CardBuffs").getCompound(id);return buff.getInt("Until")>actor.getInt("CurrentRound")?buff.getDouble("Amount"):0;}
    private static void pillExtras(CompoundTag life,CompoundTag b,CompoundTag actor,LifeCardCatalog.Card card,double factor){
        var id=net.minecraft.resources.ResourceLocation.tryParse(LifeCardCatalog.itemTextureId(card.item()));if(id==null)return;
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem pill))return;
        var doses=com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.formula(pill.formula).effects();
        CompoundTag buffs=actor.getCompound("CardBuffs");
        for(var entry:doses.entrySet()){
            String name=entry.getKey().name();
            if(List.of("DEFENSE","FIRE_DAMAGE","FIRE_RESIST","COLD_RESIST","NEXT_MELEE","NEXT_QI_SPELL","IRON_BONES","EMERGENCY_REGEN","WITHER_SHIELD").contains(name)){
                CompoundTag effect=new CompoundTag();effect.putDouble("Amount",(name.equals("IRON_BONES")?60:entry.getValue().amount())*(name.equals("WITHER_SHIELD")?1:factor));
                effect.putInt("Until",b.getInt("Round")+(int)Math.ceil(entry.getValue().ticks()/20.));buffs.put(name,effect);
            }
            if(name.equals("CLEAR_FIRE"))actor.remove("BurnUntil");
            if(name.equals("CULTIVATION")){
                var level=com.example.immortal_cultivation_mod.attachment.CultivationLevels.allLevels().get(Mth.clamp(life.getInt("Realm"),0,27));
                long required=com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(level).qiNeeded();
                life.putLong("CultivationProgress",Math.min(required,life.getLong("CultivationProgress")+(long)(required*entry.getValue().amount()*factor/100.)));
            }
        }
        actor.put("CardBuffs",buffs);
    }
    private static CompoundTag person(CompoundTag life,CompoundTag actor){
        if(actor.getString("Id").equals("player"))return life;
        if(actor.hasUUID("NpcId"))for(var t:life.getList("Relationships",Tag.TAG_COMPOUND)){
            CompoundTag p=(CompoundTag)t;if(p.hasUUID("Id")&&p.getUUID("Id").equals(actor.getUUID("NpcId")))return p;}
        return new CompoundTag();
    }
    private static void knockout(CompoundTag life,CompoundTag b,CompoundTag actor){
        if(actor.getDouble("Hp")>0||actor.getBoolean("KnockedOut")||actor.getString("Kind").equals("anchor"))return;
        actor.putBoolean("KnockedOut",true);
        if(!actor.getBoolean("Enemy")){
            CompoundTag person=person(life,actor);if(!person.isEmpty()){
                LifeCardInjuries.knockout(person,life.getInt("Age"));actor.putInt("KnockoutStreak",person.getInt("KnockoutStreak"));}
        }
    }
    public static void sync(CompoundTag life,CompoundTag b){
        for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){
            CompoundTag actor=(CompoundTag)tag;
            if(actor.getBoolean("Enemy")||actor.getString("Kind").equals("anchor"))continue;
            CompoundTag person=person(life,actor);if(person.isEmpty())continue;
            if(actor.getDouble("Hp")>=actor.getDouble("MaxHp")-.000001)actor.putInt("KnockoutStreak",0);
            LifeCardInjuries.save(person,actor,life.getInt("Age"));person.putInt("KnockoutStreak",actor.getInt("KnockoutStreak"));
            if(person.getBoolean("CombatDead"))person.putInt("Health",0);
        }
    }
    private static LifeCardCatalog.Card aiCard(CompoundTag life,CompoundTag actor,String action){
        boolean hasAuthored=actor.getList("Moves",Tag.TAG_COMPOUND).stream().anyMatch(t->((CompoundTag)t).getString("id").equals(action));
        CompoundTag knowledge=actor.getString("Id").equals("player")?life:actor;
        var basic=LifeCardCatalog.get(knowledge,actor,action);if(basic!=null&&!hasAuthored)return basic;
        var move=LifeSimulationBattle.actorMoves(life,actor).stream().filter(m->m.id().equals(action)).findFirst().orElse(null);
        if(move==null)return LifeCardCatalog.get(life,actor,"strike");
        String effect=move.effect();double amount=0;String target="enemy";double[] hits={actor.getDouble("Power")*move.potency()};
        if(effect.equals("heal")){amount=actor.getDouble("Power")*.7;target="friend";hits=new double[0];}
        else if(effect.equals("shield")||effect.equals("clock_morning")){amount=actor.getDouble("Power")*.8;target="self";hits=new double[0];}
        else effect="attack";
        // Authored individual hit values take precedence over a one-hit potency definition.
        for(var tag:actor.getList("Moves",Tag.TAG_COMPOUND)){
            CompoundTag authored=(CompoundTag)tag;if(!authored.getString("id").equals(action))continue;
            Tag value=authored.get("hits");
            if(value instanceof ListTag separate&&!separate.isEmpty()){hits=new double[separate.size()];for(int i=0;i<hits.length;i++)hits[i]=separate.get(i) instanceof net.minecraft.nbt.NumericTag number?Math.max(0,number.getAsDouble()):0;}
        }
        return new LifeCardCatalog.Card(action,move.name(),effect,1,LifeSimulationBattle.cost(actor,move),1,target,hits,amount,2,move.effect(),false,"",move.area());
    }
    private static CompoundTag record(LifeCardCatalog.Card card,String target){
        CompoundTag n=new CompoundTag();n.putString("Card",card.id());n.putString("Name",card.name());n.putString("Effect",card.effect());n.putString("Target",target);
        n.putDouble("Qi",card.qi());n.putDouble("Amount",card.amount());n.putInt("Duration",card.duration());n.putString("Status",card.status());
        n.putBoolean("Area",card.area());
        ListTag hits=new ListTag();for(double hit:card.hits())hits.add(DoubleTag.valueOf(hit));n.put("Hits",hits);return n;
    }
    private static LifeCardCatalog.Card recorded(CompoundTag n){
        ListTag list=n.getList("Hits",Tag.TAG_DOUBLE);double[] hits=new double[list.size()];for(int i=0;i<hits.length;i++)hits[i]=list.getDouble(i);
        return new LifeCardCatalog.Card(n.getString("Card"),n.getString("Name"),n.getString("Effect"),1,n.getDouble("Qi"),1,"enemy",hits,n.getDouble("Amount"),n.getInt("Duration"),n.getString("Status"),false,"",n.getBoolean("Area"));
    }
    private static CompoundTag weakest(CompoundTag b,boolean enemy){
        return b.getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
            .filter(a->a.getBoolean("Enemy")==enemy&&active(a)).min(Comparator.comparingDouble(a->a.getDouble("Hp")/a.getDouble("MaxHp"))).orElse(new CompoundTag());
    }
    private static void commitIntents(CompoundTag life,CompoundTag b){
        for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){
            CompoundTag a=(CompoundTag)tag;if(!a.getBoolean("Enemy")||!active(a))continue;
            String action=switch(a.getString("Profile")){
                case "defensive" -> b.getInt("Round")%3==1?"shield":"strike";
                case "assassin","rival_memory" -> b.getInt("Round")%2==1?"quick":"pierce";
                case "formation","disaster" -> b.getInt("Round")%2==1?"heavy":"strike";
                default -> b.getInt("Round")%2==0?"heavy":"strike";
            };
            for(var moveTag:a.getList("Moves",Tag.TAG_COMPOUND)){
                CompoundTag move=(CompoundTag)moveTag;if(move.getString("kind").equals(action))action=move.getString("id");}
            action=LifeSimulationBattleMechanics.plan(b,a,action);
            if(a.getDouble("Hp")<a.getDouble("MaxHp")*.3)action="heal";
            var card=aiCard(life,a,action);
            if(a.getDouble("Qi")<card.qi()||a.getCompound("Cooldowns").getInt(card.id())>b.getInt("Round"))card=LifeCardCatalog.get(life,a,"strike");
            CompoundTag target=List.of("heal","block","shield","clock_morning").contains(card.effect())?a:weakest(b,false);
            a.putString("Plan",card.id());a.put("Intent",record(card,target.getString("Id")));
        }
    }
    private static void endTurn(CompoundTag life,CompoundTag b,RandomSource random){
        b.getList("DiscardPile",Tag.TAG_COMPOUND).addAll(b.getList("Hand",Tag.TAG_COMPOUND));b.getList("Hand",Tag.TAG_COMPOUND).clear();
        b.putString("CardPhase","enemy");b.putInt("Energy",0);resetFrames(b);
        for(var tag:new ArrayList<>(b.getList("Actors",Tag.TAG_COMPOUND))){
            CompoundTag a=(CompoundTag)tag;if(a.getBoolean("Enemy")||a.getString("Id").equals("player")||!active(a)||!b.getString("Result").isBlank())continue;
            String behavior=a.getString("Behavior");var card=LifeCardCatalog.get(life,a,behavior.equals("defensive")?"block":"strike");
            CompoundTag target=card.effect().equals("block")?a:weakest(b,true);
            if(behavior.equals("supportive")&&player(life).getDouble("Hp")<player(life).getDouble("MaxHp")*.65){card=aiCard(life,a,"heal");target=player(life);}
            else if(!behavior.equals("defensive")){
                var learned=LifeCardCatalog.available(a,a).stream().filter(c->c.id().startsWith("spell_")&&c.effect().equals("attack")&&c.qi()<=a.getDouble("Qi")&&a.getCompound("Cooldowns").getInt(c.id())<=b.getInt("Round"))
                    .max(Comparator.comparingDouble(LifeCardCatalog.Card::baseTotal)).orElse(null);
                if(learned!=null)card=learned;
                else for(var move:a.getList("Moves",Tag.TAG_COMPOUND)){var candidate=aiCard(life,a,((CompoundTag)move).getString("id"));if(candidate.effect().equals("attack")&&candidate.qi()<=a.getDouble("Qi")&&a.getCompound("Cooldowns").getInt(candidate.id())<=b.getInt("Round")){card=candidate;break;}}
            }
            if(a.getCompound("Cooldowns").getInt(card.id())>b.getInt("Round")){card=LifeCardCatalog.get(life,a,"strike");target=weakest(b,true);}
            if(a.getDouble("Qi")<card.qi()){card=LifeCardCatalog.get(life,a,"recover");target=a;}
            a.putDouble("Qi",a.getDouble("Qi")-card.qi());a.putString("Plan",card.id());
            spendCooldown(a,card,b.getInt("Round"));
            resolve(life,b,a,target,card,random);LifeSimulationBattleMechanics.afterAction(life,b,a,random);checkEnd(life,b);frame(b);
        }
        for(var tag:new ArrayList<>(b.getList("Actors",Tag.TAG_COMPOUND))){
            CompoundTag a=(CompoundTag)tag;if(!a.getBoolean("Enemy")||!active(a)||!b.getString("Result").isBlank())continue;
            CompoundTag intent=a.getCompound("Intent");if(intent.isEmpty())continue;
            var card=recorded(intent);CompoundTag target=LifeSimulationBattle.find(b,intent.getString("Target"));
            if(!active(target))target=List.of("heal","block","shield","clock_morning").contains(card.effect())?a:weakest(b,false);
            if(a.getBoolean("Interrupted")){a.remove("Interrupted");CompoundTag detail=new CompoundTag();detail.putString("Actor",a.getString("Id"));detail.putString("Action",card.id());detail.putString("Outcome","interrupted");b.put("LastDetail",detail);frame(b);continue;}
            if(a.getDouble("Qi")<card.qi())continue; // Committed intent cannot be swapped for a new counter.
            a.putDouble("Qi",a.getDouble("Qi")-card.qi());spendCooldown(a,card,b.getInt("Round"));
            resolve(life,b,a,target,card,random);LifeSimulationBattleMechanics.afterAction(life,b,a,random);checkEnd(life,b);frame(b);
        }
        for(var tag:b.getList("Actors",Tag.TAG_COMPOUND)){
            CompoundTag a=(CompoundTag)tag;if(a.getDouble("Hp")>0&&a.getInt("BurnUntil")>b.getInt("Round")){
                a.putDouble("Hp",Math.max(0,a.getDouble("Hp")-(a.contains("BurnDamage")?a.getDouble("BurnDamage"):1)));knockout(life,b,a);}
            if(a.getDouble("Hp")>0&&!a.getBoolean("KnockedOut")){
                if(a.getDouble("GatherQi")>0)a.putDouble("Qi",Math.min(a.getDouble("MaxQi"),a.getDouble("Qi")+a.getDouble("GatherQi")));
                ListTag effects=a.getList("CardEffects",Tag.TAG_COMPOUND);
                for(int i=effects.size()-1;i>=0;i--){CompoundTag effect=effects.getCompound(i);String kind=effect.getString("Kind");
                    if(effect.getInt("Remaining")>0&&(!effect.contains("NextRound")||b.getInt("Round")>=effect.getInt("NextRound"))){a.putDouble(kind,Math.min(a.getDouble("Max"+kind),a.getDouble(kind)+effect.getDouble("Amount")));effect.putInt("Remaining",effect.getInt("Remaining")-1);}
                    if(effect.getInt("Remaining")<=0)effects.remove(i);}
            }
        }
        if(b.getString("Result").isBlank())LifeSimulationBattleMechanics.endRound(life,b);
        checkEnd(life,b);b.putInt("Round",b.getInt("Round")+1);
        for(var tag:b.getList("Actors",Tag.TAG_COMPOUND))((CompoundTag)tag).putInt("CurrentRound",b.getInt("Round"));
        if(b.getString("Result").isBlank()&&b.getInt("Round")>b.getInt("Limit")){b.putString("Result","escape");b.putBoolean("TimedOut",true);}
        b.put("LastDetail",new CompoundTag());frame(b);
    }
    private static void spendCooldown(CompoundTag actor,LifeCardCatalog.Card card,int round){
        int duration=card.id().startsWith("spell_")?(int)Math.ceil(com.example.immortal_cultivation_mod.spell.ModSpells.cooldownTicks(card.id().substring(6))/20.)
            :1+LifeSimulationBattle.actorMoves(new CompoundTag(),actor).stream().filter(m->m.id().equals(card.id())).mapToInt(LifeSimulationBattle.Move::cooldown).findFirst().orElse(0);
        CompoundTag cooldowns=actor.getCompound("Cooldowns");cooldowns.putInt(card.id(),round+duration);actor.put("Cooldowns",cooldowns);
    }
    private static void checkEnd(CompoundTag life,CompoundTag b){
        for(var tag:b.getList("Actors",Tag.TAG_COMPOUND))knockout(life,b,(CompoundTag)tag);
        limitActors(b);if(!b.getString("Result").isBlank())return;
        if(player(life).getDouble("Hp")<=0){b.putString("Result","defeat");return;}
        if(b.getInt("Hazard")>=b.getInt("HazardLimit")){b.putString("Result","defeat");return;}
        boolean allEnemies=b.getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).noneMatch(a->a.getBoolean("Enemy")&&!a.getString("Kind").equals("anchor")&&a.getDouble("Hp")>0);
        if(LifeSimulationBattleMechanics.victory(b,allEnemies))b.putString("Result","victory");
    }
}
