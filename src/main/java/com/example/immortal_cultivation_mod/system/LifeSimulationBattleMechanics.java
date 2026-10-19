package com.example.immortal_cultivation_mod.system;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Authored encounter rules, operating only on the saved simulated battle. */
final class LifeSimulationBattleMechanics {
    private LifeSimulationBattleMechanics() {}

    static boolean combatant(CompoundTag actor) {
        return !actor.getString("Kind").equals("anchor") && !actor.getBoolean("Hidden") && !actor.getBoolean("Withdrawn")
                && !actor.getBoolean("Reserve") && !actor.getBoolean("KnockedOut");
    }

    static void initialize(CompoundTag life, CompoundTag battle, CompoundTag definition) {
        String mechanic = definition.getString("mechanic");
        battle.putString("Mechanic", mechanic); battle.putString("VictoryRule", definition.getString("victory_rule"));
        battle.putString("NextObjectiveWin", definition.getString("objective_win"));
        battle.putInt("Phase", 0); battle.putString("PhaseKey", "life.battle.phase." + (mechanic.equals("tribulation") ? "body" : "encounter"));
        battle.putInt("StageGoal", mechanic.equals("tribulation") ? 3 : 1);
        battle.putInt("HazardLimit", Math.max(4, definition.getInt("hazard_limit") == 0 ? battle.getInt("Limit") : definition.getInt("hazard_limit")));
        battle.putString("HistoryKey", life.getCompound("Story").getString("MemoryKey"));
        battle.putString("HazardKey", definition.getString("hazard_key"));
        ListTag actors = battle.getList("Actors", Tag.TAG_COMPOUND);
        List<CompoundTag> enemies = actors.stream().map(tag -> (CompoundTag) tag).filter(a -> a.getBoolean("Enemy")).toList();
        int index = 0;
        for (var tag : definition.getList("anchors", Tag.TAG_COMPOUND)) {
            CompoundTag data = (CompoundTag) tag;
            CompoundTag owner = enemies.isEmpty() ? LifeSimulationBattle.find(battle, "player") : enemies.get(Mth.clamp(data.getInt("owner"), 0, enemies.size() - 1));
            CompoundTag anchor = new CompoundTag();
            anchor.putString("Id", "anchor" + index); anchor.putString("Name", data.getString("name_key"));
            anchor.putString("Kind", "anchor"); anchor.putString("OwnerId", owner.getString("Id"));
            anchor.putString("Sprite", "storm_front"); anchor.putBoolean("Enemy", !data.getBoolean("protected")); anchor.putInt("Realm", owner.getInt("Realm"));
            anchor.putDouble("MaxHp", Math.max(10, owner.getDouble("MaxHp") * Mth.clamp(data.getDouble("hp_fraction"), .02, .3)));
            anchor.putDouble("Hp", anchor.getDouble("MaxHp")); anchor.putInt("Defense", 10); anchor.putDouble("ArmorFraction", .10);
            anchor.putString("AnchorPurpose", data.getString("purpose")); anchor.putInt("CheckDC", Mth.clamp(data.getInt("dc"), 8, 26));
            anchor.putInt("Order", data.contains("order") ? data.getInt("order") : -1);
            anchor.putBoolean("Protected", data.getBoolean("protected")); anchor.putBoolean("InspectionRequired", data.getBoolean("inspection_required"));
            anchor.putBoolean("ExplicitBypass", data.getBoolean("explicit_bypass"));
            anchor.putBoolean("Identified", life.getCompound("Story").getBoolean("Prepared") && data.getBoolean("preparation_identifies"));
            anchor.putBoolean("Locked", mechanic.equals("tribulation") && index != 0);
            if (data.getBoolean("hidden")) anchor.putBoolean("Hidden", true);
            String castRole = data.getString("cast_role"); anchor.putString("CastRole", castRole);
            if (!castRole.isBlank() && life.getCompound("Story").getCompound("Cast").hasUUID(castRole)) anchor.putUUID("NpcId", life.getCompound("Story").getCompound("Cast").getUUID(castRole));
            if (!data.getString("memory_key").isBlank()) anchor.putString("MemoryKey", data.getString("memory_key"));
            else if (data.getString("purpose").equals("memory")) anchor.putString("MemoryKey", battle.getString("HistoryKey"));
            actors.add(anchor); index++;
        }
        battle.putInt("AnchorCount", index);
        if (index > 0) { battle.putBoolean("ObjectiveEnabled", true); battle.putInt("Goal", index); }
        for (CompoundTag enemy : enemies) if (enemy.getString("Profile").equals("mirror")) enemy.putDouble("Shield", enemy.getDouble("MaxHp") * .25);
        if (mechanic.equals("tribulation") && !enemies.isEmpty()) {
            CompoundTag storm = enemies.getFirst(); storm.putDouble("ReferenceMaxHp", storm.getDouble("MaxHp"));
            storm.putDouble("PhaseMaxHp", storm.getDouble("MaxHp") * .32);
            storm.putDouble("Hp", storm.getDouble("PhaseMaxHp")); storm.putDouble("MaxHp", storm.getDouble("PhaseMaxHp"));
            storm.putString("Profile", "tribulation");
        }
        if (mechanic.equals("coerced_friend")) {
            battle.putString("PhaseKey", "life.battle.phase.restrained_duel");
            battle.putString("HistoryKey", "life.battle.history.coerced_friend");
            for (CompoundTag enemy : enemies) if (enemy.getString("Profile").equals("coerced_friend")) {
                enemy.putBoolean("Restrained", true); enemy.putDouble("Power", enemy.getDouble("Power") * .30);
                battle.putString("CoercedActor", enemy.getString("Id"));
            }
            battle.put("Coercion", life.getCompound("Story").getCompound("Coercion").copy());
        }
    }

    static List<LifeSimulationBattle.Move> additionalMoves(CompoundTag actor) {
        List<LifeSimulationBattle.Move> moves = new ArrayList<>();
        String profile = actor.getString("Profile");
        if (profile.equals("mirror")) {
            moves.add(move("mirror_reflect", .70, 0, 0, .03, 0, "", "physical"));
            moves.add(move("mirror_face", .45, 1, 1, .05, 1, "obscure", "yin"));
        }
        if (profile.equals("clock")) {
            moves.add(move("clock_borrowed", .70, 1, 2, .04, 0, "slow", "physical"));
            moves.add(move("clock_morning", 0, 0, 1, .08, 2, "clock_morning", "physical"));
            CompoundTag previous = actor.getCompound("PreviousAttack");
            moves.add(move("clock_repeat", previous.isEmpty() ? .70 : Mth.clamp(previous.getDouble("Potency"), .3, 1.2), 0, 0, .04, 0,
                    previous.getString("Effect"), previous.getString("Element").isBlank() ? "physical" : previous.getString("Element")));
        }
        if (List.of("future_echo", "certainty_echo", "inner_shadow").contains(profile)) {
            moves.add(move("echo_strike", .85, 0, 0, .04, 0, "", "physical"));
            moves.add(move("shadow_pressure", .65, 1, 0, .06, 1, "pressure", "yin"));
            CompoundTag copy = actor.getCompound("EchoMove");
            if (!copy.isEmpty()) moves.add(move("future_copy", Mth.clamp(copy.getDouble("Potency"), .3, 1.05), copy.getInt("Accuracy"),
                    Mth.clamp(copy.getInt("Priority"), -1, 2), .06, 1, copy.getString("Effect"), copy.getString("Element")));
        }
        if (profile.equals("tribulation")) {
            moves.add(move("tribulation_body", 1.00, 0, -1, 0, 0, "", "thunder"));
            moves.add(move("tribulation_meridians", .90, 1, 0, 0, 0, "burn", "thunder"));
            moves.add(move("tribulation_will", .75, 1, 1, 0, 0, "pressure", "thunder"));
        }
        if (profile.equals("rival_memory")) moves.add(move("rival_counter", .85, 0, 1, .04, 1, "", "physical"));
        return moves;
    }

    private static LifeSimulationBattle.Move move(String id, double potency, int accuracy, int priority, double qi, int cooldown, String effect, String element) {
        return new LifeSimulationBattle.Move(id, "life.battle.move." + id, element, potency, accuracy, priority, qi, cooldown, effect, false);
    }

    static String plan(CompoundTag battle, CompoundTag actor, String fallback) {
        int round = battle.getInt("Round");
        return switch (actor.getString("Profile")) {
            case "mirror" -> round % 3 == 0 ? "mirror_reflect" : round % 3 == 2 ? "mirror_face" : fallback;
            case "clock" -> round % 3 == 1 ? "clock_borrowed" : round % 3 == 2 ? "clock_morning" : "clock_repeat";
            case "future_echo" -> actor.getCompound("EchoMove").isEmpty() ? "echo_strike" : "future_copy";
            case "certainty_echo" -> battle.getInt("RepeatCount") >= 2 ? "shadow_pressure" : "echo_strike";
            case "inner_shadow" -> actor.getString("Aspect").equals("fear") && battle.getInt("DefensiveStreak") >= 2 ? "shadow_pressure" : "echo_strike";
            case "tribulation" -> switch (battle.getInt("Phase")) { case 0 -> "tribulation_body"; case 1 -> "tribulation_meridians"; default -> "tribulation_will"; };
            case "coerced_friend" -> "strike";
            case "rival_memory" -> !actor.getList("KnownTactics", Tag.TAG_STRING).isEmpty() ? "rival_counter" : fallback;
            default -> fallback;
        };
    }

    static void startRound(CompoundTag life, CompoundTag battle, RandomSource random) {
        int round = battle.getInt("Round");
        if (battle.getInt("ProcessedRound") == round) return;
        battle.putInt("ProcessedRound", round);
        for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag actor = (CompoundTag) tag;
            if (actor.getDouble("Hp") <= 0 || !combatant(actor)) continue;
            if (actor.getInt("DelayedHealRound") == round) {
                double amount = Math.min(actor.getDouble("MaxHp") - actor.getDouble("Hp"), actor.getDouble("DelayedHeal"));
                actor.putDouble("Hp", actor.getDouble("Hp") + amount); actor.remove("DelayedHealRound"); actor.remove("DelayedHeal");
                CompoundTag frame = new CompoundTag(); frame.putString("Actor", actor.getString("Id")); frame.putString("Target", actor.getString("Id"));
                frame.putString("Action", "clock_delayed_heal"); frame.putDouble("Amount", amount); frame.putString("Outcome", "success");
                frame.putInt("Phase", battle.getInt("Phase")); frame.putString("PhaseKey", battle.getString("PhaseKey"));
                frame.putInt("Objective", battle.getInt("Objective")); frame.putInt("Hazard", battle.getInt("Hazard"));
                frame.put("Actors", battle.getList("Actors", Tag.TAG_COMPOUND).copy()); frame.putBoolean("Applied", true); battle.getList("Frames", Tag.TAG_COMPOUND).add(frame);
            }
            if (actor.getString("Profile").equals("mirror") && !ownerAnchors(battle, actor.getString("Id")).isEmpty())
                actor.putDouble("Shield", Math.max(actor.getDouble("Shield"), actor.getDouble("MaxHp") * .25));
            if (actor.getString("Profile").equals("certainty_echo")) actor.putDouble("ArmorFraction", battle.getInt("RepeatCount") >= 2 ? .45 : .15);
            if (actor.getString("Profile").equals("inner_shadow")) {
                double base = actor.contains("BasePower") ? actor.getDouble("BasePower") : actor.getDouble("Power"); actor.putDouble("BasePower", base);
                double boost = actor.getString("Aspect").equals("hunger") ? Math.min(.6, battle.getInt("HeavyStreak") * .15) : 0;
                actor.putDouble("Power", base * (1 + boost) * Math.max(.55, 1 - actor.getInt("WeaknessStacks") * .15));
            }
        }
    }

    static String objectiveIllegal(CompoundTag life, String targetId) {
        return objectiveIllegalFor(life, LifeSimulationBattle.find(life.getCompound("Battle"), "player"), targetId);
    }

    static String objectiveIllegalFor(CompoundTag life, CompoundTag actor, String targetId) {
        CompoundTag battle = life.getCompound("Battle"), anchor = LifeSimulationBattle.find(battle, targetId);
        if (battle.getInt("AnchorCount") == 0) return "";
        if (!anchor.getString("Kind").equals("anchor") || anchor.getDouble("Hp") <= 0 || anchor.getBoolean("Resolved") || anchor.getBoolean("Hidden")) return "target";
        if (anchor.getBoolean("Locked")) return "stage";
        if (anchor.getBoolean("InspectionRequired") && !anchor.getBoolean("Identified")) return "inspect";
        if (anchor.getInt("Order") >= 0 && anchor.getInt("Order") != battle.getInt("Objective")) return "order";
        if (!anchor.getBoolean("ExplicitBypass") && !LifeSimulationBattle.plausible(actor, anchor)) return "realm";
        return "";
    }

    static boolean objective(CompoundTag life, CompoundTag battle, CompoundTag actor, CompoundTag anchor, RandomSource random, CompoundTag detail) {
        if (battle.getInt("AnchorCount") == 0) return false;
        String illegal = objectiveIllegalFor(life, actor, anchor.getString("Id"));
        if (!illegal.isBlank()) { actor.putBoolean("Guard", true); detail.putString("Outcome", illegal); return true; }
        boolean mental = anchor.getString("AnchorPurpose").equals("memory") || anchor.getString("AnchorPurpose").equals("will");
        int modifier = actor.getString("Id").equals("player") ? LifeSimulationDice.attribute(life.getInt(mental ? "Mentality" : "Comprehension"))
                + Mth.clamp(life.getInt(mental ? "Combat" : "Alchemy") / 30, 0, 4) : actor.getInt("InsightModifier") + actor.getInt("Proficiency");
        CompoundTag roll = LifeSimulationDice.check(random, mental ? "composure" : "formation", modifier, anchor.getInt("CheckDC"),
                anchor.getBoolean("Identified") || life.getCompound("Story").getBoolean("Prepared"), life.getInt("TrainingStrain") >= 80);
        detail.put("Roll", roll); LifeSimulationDice.remember(life, roll);
        if (roll.getInt("Degree") >= 1) {
            resolveAnchor(battle, anchor); detail.putString("Outcome", roll.getInt("Degree") == 1 ? "complication" : "anchor_resolved");
            if (roll.getInt("Degree") == 1) battle.putInt("Hazard", battle.getInt("Hazard") + 1);
            if (anchor.getString("AnchorPurpose").equals("memory")) for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
                CompoundTag enemy = (CompoundTag) tag;
                if (enemy.getBoolean("Enemy") && combatant(enemy)) {
                    enemy.putInt("WeaknessStacks", Math.min(3, enemy.getInt("WeaknessStacks") + 1));
                    enemy.putDouble("Power", enemy.getDouble("Power") * .85); enemy.putDouble("Shield", 0);
                }
            }
            if (anchor.getString("AnchorPurpose").equals("hostage")) {
                battle.getCompound("Coercion").putString("State", "hostage_released"); switchFriend(battle);
            }
        } else { battle.putInt("Hazard", battle.getInt("Hazard") + 1); detail.putString("Outcome", "failure"); }
        return true;
    }

    static void inspect(CompoundTag life, CompoundTag battle, CompoundTag selected) {
        for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag actor = (CompoundTag) tag;
            // Inspection is a meaningful action: expose the marked anchors, not a new die each paragraph.
            actor.putBoolean("Identified", true);
            if (battle.getString("Mechanic").equals("coerced_friend")) actor.putBoolean("Hidden", false);
            if (actor.hasUUID("NpcId") && !actor.getBoolean("Hidden")) LifeSimulationStories.observePerson(life, actor.getUUID("NpcId"));
        }
        if (battle.getString("Mechanic").equals("coerced_friend")) {
            battle.putInt("Phase", 1); battle.putString("PhaseKey", "life.battle.phase.ambush_exposed"); battle.getCompound("Coercion").putString("State", "captor_exposed");
        }
    }

    static void beforeHit(CompoundTag battle, CompoundTag actor, CompoundTag target, CompoundTag detail) {
        if (target.getString("Kind").equals("anchor") && target.getBoolean("InspectionRequired") && !target.getBoolean("Identified")) {
            battle.putInt("Hazard", battle.getInt("Hazard") + 2);
            if (List.of("barrier", "flood", "siege").contains(battle.getString("Mechanic"))) battle.putInt("CiviliansHarmed", battle.getInt("CiviliansHarmed") + 1);
            detail.putString("WarningKey", "life.battle.history.wrong_anchor");
        }
        if (actor.getString("Plan").equals("rival_counter") && actor.getList("KnownTactics", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(battle.getString("ReservedAction")))) actor.putBoolean("Guard", true);
    }

    static void afterAction(CompoundTag life, CompoundTag battle, CompoundTag actor, RandomSource random) {
        CompoundTag detail = battle.getCompound("LastDetail");
        for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag target = (CompoundTag) tag;
            if (target.getString("Kind").equals("anchor") && target.getDouble("Hp") <= 0 && !target.getBoolean("Resolved")) resolveAnchor(battle, target);
        }
        if (battle.getString("Mechanic").equals("coerced_friend") && !battle.getString("Stakes").equals("lethal")) {
            CompoundTag friend = LifeSimulationBattle.find(battle, battle.getString("CoercedActor"));
            if (!friend.isEmpty() && friend.getBoolean("Enemy") && friend.getDouble("Hp") <= friend.getDouble("MaxHp") * .25) {
                friend.putDouble("Hp", Math.max(1, friend.getDouble("Hp"))); inspect(life, battle, friend); switchFriend(battle);
                detail.putString("StoryMessageKey", "life.battle.history.friend_switch");
            }
        }
        if (battle.getString("Mechanic").equals("tribulation")) advanceTribulation(life, battle, random, detail);
        if (actor.getString("Id").equals("player")) recordPlayerAction(life, battle, actor);
        if (actor.getBoolean("Enemy")) {
            LifeSimulationBattle.Move move = LifeSimulationBattle.actorMoves(life, actor).stream().filter(m -> m.id().equals(actor.getString("Plan"))).findFirst().orElse(null);
            if (move != null && move.potency() > 0 && !move.id().equals("clock_repeat")) actor.put("PreviousAttack", moveRecord(move));
        }
    }

    private static void recordPlayerAction(CompoundTag life, CompoundTag battle, CompoundTag actor) {
        String action = actor.getString("Plan"), previous = battle.getString("LastPlayerAction");
        battle.putInt("RepeatCount", action.equals(previous) ? battle.getInt("RepeatCount") + 1 : 1); battle.putString("LastPlayerAction", action);
        boolean defensive = List.of("guard", "shield", "block", "dodge", "heal", "recover").contains(action);
        battle.putInt("DefensiveStreak", defensive ? battle.getInt("DefensiveStreak") + 1 : 0);
        LifeSimulationBattle.Move move = LifeSimulationBattle.moves(life).stream().filter(m -> m.id().equals(action)).findFirst().orElse(null);
        battle.putInt("HeavyStreak", move != null && move.potency() >= 1.1 ? battle.getInt("HeavyStreak") + 1 : 0);
        ListTag history = battle.getList("PlayerActions", Tag.TAG_STRING); history.add(StringTag.valueOf(action)); battle.put("PlayerActions", history);
        if (move != null && move.potency() > 0) for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag enemy = (CompoundTag) tag;
            if (enemy.getString("Profile").equals("future_echo")) enemy.put("EchoMove", moveRecord(move));
        }
    }

    private static CompoundTag moveRecord(LifeSimulationBattle.Move move) {
        CompoundTag copy = new CompoundTag(); copy.putString("Id", move.id()); copy.putDouble("Potency", move.potency());
        copy.putInt("Accuracy", move.accuracy()); copy.putInt("Priority", move.priority()); copy.putString("Element", move.element()); copy.putString("Effect", move.effect()); return copy;
    }

    static boolean specialSupport(CompoundTag battle, CompoundTag actor, LifeSimulationBattle.Move move, RandomSource random, CompoundTag detail) {
        if (!move.effect().equals("clock_morning")) return false;
        CompoundTag roll = LifeSimulationDice.damage(random, 2, 6); detail.put("DamageRoll", roll);
        double amount = actor.getDouble("Power") * .8 * roll.getDouble("Factor");
        actor.putDouble("Shield", Math.min(actor.getDouble("MaxHp") * .4, actor.getDouble("Shield") + amount));
        actor.putDouble("DelayedHeal", actor.getDouble("Power") * .5 * roll.getDouble("Factor")); actor.putInt("DelayedHealRound", battle.getInt("Round") + 1);
        detail.putString("Target", actor.getString("Id")); detail.putDouble("Amount", amount); return true;
    }

    static void endRound(CompoundTag life, CompoundTag battle) {
        String mechanic = battle.getString("Mechanic"); int round = battle.getInt("Round");
        CompoundTag player = LifeSimulationBattle.find(battle, "player");
        if (player.getDouble("Hp") <= 0) return;
        if (List.of("vein_collapse", "rift").contains(mechanic) && round % 2 == 0 && countResolved(battle) < battle.getInt("AnchorCount")) battle.putInt("Hazard", battle.getInt("Hazard") + 1);
        if (mechanic.equals("mist")) {
            boolean protectedBreathing = player.getBoolean("Guard") || player.getDouble("Shield") > 0 || player.getInt("CoverUntil") > round;
            if (!protectedBreathing) battle.putInt("Hazard", battle.getInt("Hazard") + 1);
            int rank = battle.getList("Actors", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t).filter(a -> a.getBoolean("Enemy") && combatant(a)).mapToInt(a -> a.getInt("Realm")).max().orElse(7);
            if (battle.getInt("Hazard") >= 3 && player.getInt("Realm") < rank + 6) player.putDouble("Hp", Math.max(0, player.getDouble("Hp") - player.getDouble("MaxHp") * .06));
        }
        if (mechanic.equals("inner_shadow") && battle.getInt("DefensiveStreak") >= 3) battle.putInt("Hazard", battle.getInt("Hazard") + 1);
        if (mechanic.equals("siege") && (LifeCardBattle.enabled(battle) ? player.getInt("DefendedRound")==round : player.getBoolean("Guard"))) battle.putInt("HoldRounds", battle.getInt("HoldRounds") + 1);
        if (mechanic.equals("siege") && battle.getInt("HoldRounds") >= 6) { battle.putString("Result", "victory"); battle.putString("VictoryBy", "combat"); }
        if (!mechanic.isBlank() && battle.getInt("Hazard") > 0) {
            CompoundTag detail = new CompoundTag(); detail.putString("Actor", ""); detail.putString("Action", "hazard_pulse");
            detail.putInt("Hazard", battle.getInt("Hazard")); detail.putString("StoryMessageKey", "life.battle.history.hazard_pulse");
            detail.putInt("Phase", battle.getInt("Phase")); detail.putString("PhaseKey", battle.getString("PhaseKey"));
            detail.putInt("Objective", battle.getInt("Objective"));
            detail.put("Actors", battle.getList("Actors", Tag.TAG_COMPOUND).copy()); detail.putBoolean("Applied", true);
            battle.getList("Frames", Tag.TAG_COMPOUND).add(detail);
        }
    }

    private static void advanceTribulation(CompoundTag life, CompoundTag battle, RandomSource random, CompoundTag detail) {
        if (!battle.getString("Result").isBlank() || LifeSimulationBattle.find(battle, "player").getDouble("Hp") <= 0) return;
        int phase = battle.getInt("Phase"); if (phase >= 3) return;
        CompoundTag storm = battle.getList("Actors", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t).filter(a -> a.getString("Profile").equals("tribulation")).findFirst().orElse(new CompoundTag());
        CompoundTag anchor = LifeSimulationBattle.find(battle, "anchor" + phase);
        if (!anchor.getBoolean("Resolved") && storm.getDouble("Hp") > 0) return;
        if (phase == 2) {
            CompoundTag will = LifeSimulationDice.check(random, "composure", LifeSimulationDice.attribute(life.getInt("Mentality"))
                    + Mth.clamp(life.getInt("Combat") / 30, 0, 4), 14, life.getCompound("Story").getBoolean("Prepared"), life.getInt("TrainingStrain") >= 80);
            detail.put("FinalWillRoll", will); LifeSimulationDice.remember(life, will);
            if (will.getInt("Degree") == 0) {
                CompoundTag player = LifeSimulationBattle.find(battle, "player");
                player.putDouble("Hp", Math.max(0, player.getDouble("Hp") - player.getDouble("MaxHp") * .12));
                battle.putInt("Hazard", battle.getInt("Hazard") + 1); anchor.putBoolean("Resolved", false); anchor.putDouble("Hp", anchor.getDouble("MaxHp"));
                storm.putDouble("Hp", storm.getDouble("PhaseMaxHp"));
                battle.putInt("Objective", countResolved(battle)); detail.putString("StoryMessageKey", "life.battle.history.will_failed"); return;
            }
            if (!anchor.getBoolean("Resolved")) resolveAnchor(battle, anchor);
            battle.putInt("Phase", 3); battle.putString("PhaseKey", "life.battle.phase.complete"); battle.putString("Result", "victory"); battle.putString("VictoryBy", "objective"); return;
        }
        if (!anchor.getBoolean("Resolved")) resolveAnchor(battle, anchor);
        battle.putInt("Phase", phase + 1); battle.putString("PhaseKey", "life.battle.phase." + (phase == 0 ? "meridians" : "will"));
        LifeSimulationBattle.find(battle, "anchor" + (phase + 1)).putBoolean("Locked", false);
        storm.putDouble("Hp", storm.getDouble("PhaseMaxHp")); detail.putString("StoryMessageKey", "life.battle.history.phase_advanced"); detail.putInt("Phase", phase + 1);
    }

    private static void switchFriend(CompoundTag battle) {
        CompoundTag friend = LifeSimulationBattle.find(battle, battle.getString("CoercedActor"));
        if (friend.isEmpty() || friend.getDouble("Hp") <= 0) return;
        friend.putBoolean("Enemy", false); friend.putBoolean("Restrained", false); friend.putString("Profile", "ally");
        friend.putDouble("Power", friend.getDouble("MaxHp") * .19); friend.putString("Plan", "guard"); friend.putBoolean("Protect", true);
        battle.putInt("Phase", 1); battle.putString("PhaseKey", "life.battle.phase.ambush_exposed"); battle.putBoolean("FriendSwitched", true);
        for (var tag : battle.getList("Actors", Tag.TAG_COMPOUND)) ((CompoundTag) tag).putBoolean("Hidden", false);
    }

    private static void resolveAnchor(CompoundTag battle, CompoundTag anchor) {
        anchor.putBoolean("Resolved", true); anchor.putDouble("Hp", 0); battle.putInt("Objective", countResolved(battle));
        CompoundTag owner = LifeSimulationBattle.find(battle, anchor.getString("OwnerId"));
        if (ownerAnchors(battle, owner.getString("Id")).isEmpty()) owner.putDouble("Shield", 0);
    }

    private static int countResolved(CompoundTag battle) {
        return (int) battle.getList("Actors", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t).filter(a -> a.getString("Kind").equals("anchor") && a.getBoolean("Resolved")).count();
    }

    private static List<CompoundTag> ownerAnchors(CompoundTag battle, String owner) {
        return battle.getList("Actors", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t)
                .filter(a -> a.getString("Kind").equals("anchor") && a.getString("OwnerId").equals(owner) && !a.getBoolean("Resolved")).toList();
    }

    static boolean victory(CompoundTag battle, boolean enemiesDefeated) {
        if (battle.getString("Mechanic").equals("tribulation")) return battle.getInt("Phase") >= 3;
        boolean anchors = battle.getInt("AnchorCount") > 0 && countResolved(battle) == battle.getInt("AnchorCount");
        if (battle.getString("Mechanic").equals("coerced_friend")) return enemiesDefeated && anchors;
        if (battle.getString("VictoryRule").equals("anchors")) { if (anchors) battle.putString("VictoryBy", "objective"); return anchors; }
        if (anchors) { battle.putString("VictoryBy", "objective"); return true; }
        return enemiesDefeated || battle.getInt("AnchorCount") == 0 && LifeSimulationBattle.supportsObjective(battle) && battle.getInt("Objective") >= battle.getInt("Goal");
    }
}
