package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** State and resource rules for the replacement manuscript. No real-player mutations. */
public final class LifeAuthoredStories {
    private LifeAuthoredStories() {}
    public static boolean remaining(CompoundTag life) {
        return List.of("life_story_third_bell","life_story_winter_coats","life_story_ninth_spring",
                       "life_story_salt_baskets","life_event_rain_sword").stream()
                .anyMatch(id->!life.getCompound("StoryJournal").contains(id))
                || sectAffiliated(life)&&!life.getCompound("StoryJournal").contains("life_event_missing_disciple");
    }
    public static boolean sectAffiliated(CompoundTag life) {return !sectIdentity(life).isBlank();}
    public static String sectIdentity(CompoundTag life) {
        String sect=life.getString("Sect");if(!sect.isBlank()&&!sect.equals("none"))return sect;
        CompoundTag employment=life.getCompound("SectEmployment");sect=employment.getString("Sect");
        return employment.getBoolean("Active")&&employment.getBoolean("Registered")&&!sect.isBlank()&&!sect.equals("none")?sect:"";
    }
    public static boolean active(CompoundTag life) {
        return life.getCompound("Story").getCompound("Definition").getBoolean("replacement");
    }
    private static CompoundTag state(CompoundTag life) {
        CompoundTag story = life.getCompound("Story");
        if (!story.contains("State")) story.put("State", new CompoundTag());
        return story.getCompound("State");
    }
    public static boolean conscious(CompoundTag life) {
        return life.getBoolean("Alive") && !life.getBoolean("CombatDead") && LifeCardInjuries.hp(life) > 0;
    }
    public static boolean matches(CompoundTag life, String expression) {
        if (expression.isBlank()) return true;
        for (String alternative : expression.split("\\|")) {
            boolean all = true;
            for (String term : alternative.split("&")) all &= term(life, term.trim());
            if (all) return true;
        }
        return false;
    }
    private static boolean term(CompoundTag life, String term) {
        if (term.startsWith("!")) return !term(life, term.substring(1));
        if (term.equals("conscious")) return conscious(life);
        if (term.equals("mobile")) return conscious(life) && life.getInt("Health") >= 25 && life.getInt("InjuredUntil") <= life.getInt("Age");
        if (term.equals("combat")) return life.getInt("Realm") >= 1 && term(life,"mobile") && usableAttack(life);
        if (term.equals("boat")) return !boat(life).isBlank();
        if (term.equals("project_reserve")) return state(life).getInt("ProjectReserve")==24&&!state(life).getBoolean("project_spent");
        if (term.startsWith("cast_alive:")) {
            CompoundTag cast=life.getCompound("Story").getCompound("Cast");String role=term.substring(11);
            if(!cast.hasUUID(role))return false;
            return life.getList("Relationships",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t)
                    .anyMatch(n->n.hasUUID("Id")&&n.getUUID("Id").equals(cast.getUUID(role))&&n.getBoolean("Alive"));
        }
        if (term.startsWith("realm>=")) return life.getInt("Realm") >= Integer.parseInt(term.substring(7));
        if (term.startsWith("freight>=")) return state(life).getInt("Freight") >= Integer.parseInt(term.substring(9));
        if (term.startsWith("ability:")) return !ability(life,term.substring(8)).isBlank();
        return state(life).getBoolean(term);
    }
    private static String boat(CompoundTag life) {
        // These inventory possessions are available with the simulated traveller.
        // Chest boats and unavailable/stored possessions are not shallow ferries.
        List<String> boats=List.of("oak_boat","spruce_boat","birch_boat","jungle_boat","acacia_boat",
                "dark_oak_boat","mangrove_boat","cherry_boat","bamboo_raft");
        for(var tag:life.getList("Inventory",Tag.TAG_COMPOUND)) {
            CompoundTag item=(CompoundTag)tag;
            if(item.getInt("Count")>0&&!item.getBoolean("Unavailable")&&!item.getBoolean("StoredAway")
                    &&boats.stream().anyMatch(id->item.getString("Id").equals("minecraft:"+id)))return item.getString("Id");
        }
        return "";
    }
    private static CompoundTag actor(CompoundTag life) {
        CompoundTag actor = LifeSimulationBattle.actor("player",life.getString("Name"),life.getInt("Realm"),false,"","player");
        if (life.contains("CombatQi")) actor.putDouble("Qi",life.getDouble("CombatQi"));
        return actor;
    }
    private static boolean usableAttack(CompoundTag life) {
        CompoundTag actor=actor(life);
        ListTag deck=life.contains("CardDeck")?life.getList("CardDeck",Tag.TAG_STRING):LifeCardBattle.starter();
        for (var tag:deck) {
            String id=tag.getAsString();
            var card=LifeCardCatalog.get(life,actor,id);
            if (card!=null && card.effect().equals("attack") && card.qi()<=actor.getDouble("Qi")
                    && (!id.startsWith("spell_") || LifeSimulationInheritance.canLearnSpell(life,id.substring(6)))) return true;
        }
        return false;
    }
    /** Only utility abilities suitable for this physical task, already learned and affordable. */
    private static String ability(CompoundTag life,String kind) {
        if (!term(life,"mobile") || life.getInt("Realm")<1 || "none".equals(life.getString("SpiritualRoot"))) return "";
        List<String> suitable=switch(kind) {
            case "movement", "movement2" -> List.of("wind_step","yufeng_jue","xuying_ta");
            case "retrieve" -> List.of("yuqi_shu");
            case "barrier" -> List.of("gushi_shield","sifang_jie");
            default -> List.of();
        };
        double qi=actor(life).getDouble("Qi"); int trips=kind.equals("movement2")?2:1;
        for (String id:suitable) {
            if (life.getList("DiscoveredSpells",Tag.TAG_STRING).stream().noneMatch(t->t.getAsString().equals(id))) continue;
            var spell=ModSpells.get(id);
            if (spell!=null && CultivationLevels.getStageIndex(spell.requiredLevel())<=life.getInt("Realm")
                    && qi>=spell.qiCost()*trips) return id;
        }
        return "";
    }
    public static String illegal(CompoundTag life,CompoundTag choice) {
        if (!matches(life,choice.getString("requires"))) return "condition";
        if (stones(life)<choice.getInt("stone_cost")) return "wealth";
        String utility=choice.getString("ability");
        if (!utility.isBlank() && !bypassAbility(life,choice) && ability(life,utility).isBlank()) return "condition";
        return "";
    }
    private static boolean bypassAbility(CompoundTag life,CompoundTag choice) {
        return !choice.getString("ability_unless").isBlank() && matches(life,choice.getString("ability_unless"));
    }
    public static int stones(CompoundTag life) {
        // The existing simulator wallet is denominated in low-grade stones.
        // Inventory stacks remain possessions, not a second copy of that wallet.
        return (int)Math.min(Integer.MAX_VALUE,Math.max(0L,life.getLong("Wealth")));
    }
    public static void pay(CompoundTag life,CompoundTag choice) {
        life.putLong("Wealth",life.getLong("Wealth")-Math.max(0,choice.getInt("stone_cost")));
        String kind=choice.getString("ability");
        if (!kind.isBlank() && !bypassAbility(life,choice)) {
            String id=ability(life,kind); var spell=ModSpells.get(id);
            if (spell==null) throw new IllegalStateException("Validated story ability disappeared");
            life.putDouble("CombatQi",actor(life).getDouble("Qi")-spell.qiCost()*(kind.equals("movement2")?2:1));
            state(life).putString("LastUtilitySpell",id);
        }
    }
    public static void effects(CompoundTag life,ListTag effects) {
        for(var effect:effects) effect(life,effect.getAsString());
    }
    private static void effect(CompoundTag life,String raw) {
        CompoundTag state=state(life);
        if(raw.startsWith("md:")){discipleEffect(life,state,raw.substring(3));return;}
        if(raw.startsWith("rain:")) {
            switch(raw.substring(5)) {
                case "prepare" -> {if(!state.getBoolean("prepared")){effect(life,"time:2");state.putBoolean("prepared",true);}}
                case "copy_names" -> {state.putBoolean("names_copy",true);state.putBoolean("document:names_copy",true);}
                case "own_boat" -> {state.putBoolean("own_boat",true);state.putString("BoatUsed",boat(life));}
                case "cross" -> {
                    state.putBoolean("crossing_completed",true);state.putBoolean("oath_ended",true);
                    state.putBoolean("river_protection_ended",true);state.putBoolean("night_service_ended",true);
                    state.putBoolean("promise_fulfilled",state.getBoolean("crossing_promise"));
                }
                case "release" -> {
                    state.putBoolean("oath_ended",true);state.putBoolean("sword_dissolved",true);
                    state.putBoolean("river_protection_ended",true);state.putBoolean("night_service_ended",true);
                    state.putBoolean("promise_replaced_and_discharged",state.getBoolean("crossing_promise"));
                }
                case "take_sword" -> {
                    if(!state.getBoolean("sword_transferred")&&!state.getBoolean("sword_dissolved")) {
                        String item=life.getCompound("Story").getCompound("Definition").getString("sword_item");
                        if(!item.isBlank())LifeSimulationSystem.addSimulationItem(life,item,1);
                        state.putBoolean("sword_transferred",true);state.putString("SwordItem",item);
                    }
                }
                case "world" -> {
                    if(state.getBoolean("sword_transferred")) {
                        state.putBoolean("river_protection_ended",true);state.putBoolean("night_service_ended",true);
                    }
                    state.putBoolean("promise_unfulfilled",state.getBoolean("crossing_promise")
                            &&!state.getBoolean("promise_fulfilled")&&!state.getBoolean("promise_replaced_and_discharged"));
                    life.put("RainFerryState",state.copy());
                }
                default -> throw new IllegalArgumentException("Unknown Rain Ferry effect "+raw);
            }
            return;
        }
        if (raw.startsWith("document:")) { state.putBoolean(raw,true); return; }
        if (raw.startsWith("breach:")) { String promise=raw.substring(7); if(state.getBoolean(promise))state.putBoolean(promise+"_broken",true); return; }
        if (raw.startsWith("time:")) {
            int halfDays=Integer.parseInt(raw.substring(5));
            state.putInt("HalfDays",state.getInt("HalfDays")+halfDays);
            life.putInt("StoryElapsedHalfDays",life.getInt("StoryElapsedHalfDays")+halfDays);
            return; // A fraction of a year never invokes annual free recovery.
        }
        if (raw.startsWith("freight:")) { state.putInt("Freight",state.getInt("Freight")+Integer.parseInt(raw.substring(8))); return; }
        if (raw.startsWith("fee:")) {
            if (state.getBoolean("fee_settled")) return;
            if (!conscious(life)) {state.putInt("PendingFee",Integer.parseInt(raw.substring(4)));return;}
            LifeSimulationSystem.applyEffect(life,"wealth+"+raw.substring(4));
            state.putBoolean("fee_settled",true);state.remove("PendingFee");return;
        }
        if (raw.equals("privacy_check")) {if(state.getBoolean("privacy")&&!state.getBoolean("informed"))state.putBoolean("privacy_broken",true);return;}
        if (raw.equals("settle_salt")) {
            if(state.getBoolean("bond_settled"))return;
            state.putInt("SaltRecovered",state.getBoolean("salt")?2:0);
            state.putInt("VillagerCompensation",state.getBoolean("salt")?6:18);
            state.putInt("RangBondReturned",state.getBoolean("salt")?12:0);
            state.putInt("FreightReturned",state.getInt("Freight"));state.putInt("Freight",0);
            state.putBoolean("bond_settled",true);return;
        }
        if(raw.equals("report_done")) {
            state.putBoolean("report_done",true);
            state.putBoolean("ReportLate",state.getInt("HalfDays")>6 || life.getInt("Age")>life.getCompound("Story").getInt("StartedYear"));
            return;
        }
        if(raw.startsWith("!"))state.putBoolean(raw.substring(1),false);else state.putBoolean(raw,true);
    }
    private static CompoundTag castPerson(CompoundTag life,String role) {
        CompoundTag cast=life.getCompound("Story").getCompound("Cast");
        if(cast.hasUUID(role))for(var tag:life.getList("Relationships",Tag.TAG_COMPOUND)) {
            var npc=(CompoundTag)tag;if(npc.hasUUID("Id")&&npc.getUUID("Id").equals(cast.getUUID(role)))return npc;
        }
        return new CompoundTag();
    }
    private static void discipleEffect(CompoundTag life,CompoundTag state,String effect) {
        switch(effect) {
            case "initialize" -> {state.putInt("ProjectReserve",24);state.putBoolean("lin_connected",true);castPerson(life,"lead").putBoolean("FormationBound",true);}
            case "worker_trust" -> {var worker=castPerson(life,"worker");worker.putInt("Trust",Math.min(100,worker.getInt("Trust")+20));state.putBoolean("worker_trust",true);}
            case "privacy_breach" -> {
                state.putBoolean("confidentiality_broken",true);state.putBoolean("worker_trust_damaged",true);
                var worker=castPerson(life,"worker");worker.putInt("Trust",Math.max(0,worker.getInt("Trust")-25));worker.putBoolean("RefusesDocumentCustody",true);
            }
            case "shutdown" -> {
                if(!state.getBoolean("lin_freed")) {
                    state.putBoolean("batch_lost",true);state.putInt("ShutdownCount",state.getInt("ShutdownCount")+1);
                    freeDisciple(life,state);
                }
            }
            case "substitute" -> {
                if(state.getInt("ProjectReserve")!=24||state.getBoolean("project_spent"))throw new IllegalStateException("Validated project reserve unavailable");
                state.putInt("ProjectReserve",0);state.putInt("ProjectStonesSpent",24);state.putBoolean("project_spent",true);
                state.putBoolean("crop_preserved",true);state.putBoolean("donor_fittings_removed",true);
                state.putInt("ElapsedHours",state.getInt("ElapsedHours")+8);life.putInt("StoryElapsedHours",life.getInt("StoryElapsedHours")+8);
                freeDisciple(life,state);
            }
            case "return_cloak" -> {
                if(state.getBoolean("cloak_held"))state.putBoolean("cloak_returned_to_lin",true);
                state.putBoolean("cloak_held",false);state.putBoolean("meal_ticket_held",false);
            }
            case "return_entrusted" -> {
                if(state.getBoolean("key_borrowed"))state.putBoolean("key_returned",true);
                if(state.getBoolean("cloak_held"))state.putBoolean(state.getBoolean("lin_freed")?"cloak_returned_to_lin":"cloak_returned_to_care",true);
                state.putBoolean("key_held",false);state.putBoolean("cloak_held",false);state.putBoolean("meal_ticket_held",false);state.putBoolean("note_held",false);
                state.putBoolean("evidence_deposited",true);
            }
            case "publish_evidence" -> {
                state.putBoolean("authorization_verified",true);
                if(!state.getBoolean("accounting_evidence"))state.putBoolean("worker_private_confirmation",true);
                state.putBoolean("other_victim_identities_sealed",true);state.putBoolean("lin_public_name_consent",true);
            }
            case "protect" -> {state.putBoolean("protective_order",true);state.putBoolean("steward_operations_suspended",true);}
            case "censure" -> {
                if(state.getBoolean("unauthorized_records")||state.getBoolean("battle_started")||state.getBoolean("confidentiality_broken")) {
                    state.putBoolean("censure_pending",true);state.putBoolean("archive_access_restricted",true);
                }
            }
            case "world" -> {
                state.putBoolean("confidentiality_kept",state.getBoolean("confidentiality")&&!state.getBoolean("confidentiality_broken"));
                var steward=castPerson(life,"steward");
                if(state.getBoolean("sealed_agreement"))steward.putBoolean("AuthorityOverLinRevoked",true);
                if(state.getBoolean("public_accountability"))steward.putBoolean("NightDutyAuthorityRevoked",true);
                if(state.getBoolean("steward_operations_suspended"))steward.putBoolean("HumanSupplyOperationsSuspended",true);
                if(state.getBoolean("identity_restored"))castPerson(life,"lead").putBoolean("DesertionAccusationWithdrawn",true);
                life.put("MissingDiscipleState",state.copy());
            }
            default -> throw new IllegalArgumentException("Unknown disciple story effect "+effect);
        }
    }
    private static void freeDisciple(CompoundTag life,CompoundTag state) {
        state.putBoolean("lin_connected",false);state.putBoolean("lin_freed",true);state.putBoolean("medical_handover",true);
        state.putBoolean("human_supply_stopped",true);
        // Removing the connection is not healing, cultivation, or resurrection.
        var lin=castPerson(life,"lead");lin.putBoolean("FormationBound",false);lin.putBoolean("MedicalCare",true);
    }
    public static void enter(CompoundTag life,CompoundTag node) {
        CompoundTag story=life.getCompound("Story");
        CompoundTag entered=story.getCompound("EnteredNodes");String id=node.getString("id");
        if(entered.getBoolean(id))return;
        entered.putBoolean(id,true);story.put("EnteredNodes",entered);
        effects(life,node.getList("enter_effects",Tag.TAG_STRING));
    }
    public static ListTag textParts(CompoundTag life,CompoundTag node) {
        ListTag parts=new ListTag();
        for(var tag:node.getList("parts",Tag.TAG_COMPOUND)) {
            CompoundTag part=(CompoundTag)tag;
            if(matches(life,part.getString("requires")))parts.add(StringTag.valueOf(part.getString("text_key")));
        }
        return parts;
    }
    public static Component text(CompoundTag event) {
        var result=Component.translatable(event.getString("TextKey")).copy();
        for(var tag:event.getList("TextParts",Tag.TAG_STRING)) {
            Component part=Component.translatable(tag.getAsString());
            if(!part.getString().isBlank())result.append("\n\n").append(part);
        }
        if (!event.getCompound("LootReward").isEmpty()) result.append("\n\n").append(LifeStoryLoot.description(event.getCompound("LootReward")));
        return result;
    }
    public static void settlePendingFees(CompoundTag life) {
        if(!conscious(life))return;
        CompoundTag journal=life.getCompound("StoryJournal");
        for(String id:journal.getAllKeys()) {
            CompoundTag state=journal.getCompound(id).getCompound("State");
            int fee=state.getInt("PendingFee");
            if(fee<=0||state.getBoolean("fee_settled"))continue;
            LifeSimulationSystem.applyEffect(life,"wealth+"+fee);
            state.putBoolean("fee_settled",true);state.remove("PendingFee");
        }
    }
}
