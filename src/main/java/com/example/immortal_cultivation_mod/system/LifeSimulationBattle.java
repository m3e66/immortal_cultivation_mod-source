package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Pure simulated combat. No world entities, real inventory, or real player damage. */
public final class LifeSimulationBattle {
    public static final String ROOT = "Battle";
    private LifeSimulationBattle() {}

    public record Move(String id, String name, String element, double potency, int accuracy,
                       int priority, double qiFraction, int cooldown, String effect, boolean area) {}
    public static final List<Move> BASE_MOVES = List.of(
            move("strike", 1, 0, 0, 0, 0, "", false), move("heavy", 1.5, -2, -1, .04, 1, "", false),
            move("quick", .65, 1, 2, .02, 0, "", false), move("pierce", .85, 0, 0, .06, 1, "pierce", false),
            move("shield", 0, 0, 1, .08, 2, "shield", false), move("heal", 0, 0, 0, .12, 3, "heal", false));
    private static Move move(String id, double potency, int accuracy, int priority, double qi, int cd, String effect, boolean area) {
        return new Move(id, "life.battle.move." + id, "physical", potency, accuracy, priority, qi, cd, effect, area);
    }

    public static List<Move> moves(CompoundTag life) {
        List<Move> moves = new ArrayList<>(BASE_MOVES);
        for (var entry : life.getList("DiscoveredSpells", Tag.TAG_STRING)) {
            var spell = ModSpells.get(entry.getAsString());
            if (spell == null) continue;
            String element = spell.element();
            String effect = switch (element) { case "fire" -> "burn"; case "ice" -> "slow"; case "thunder" -> "disrupt"; default -> ""; };
            moves.add(new Move("spell_" + spell.id(), spell.translationKey(), element,
                    element.equals("ice") ? .8 : 1.1, 0, 0, .08, 1, effect,
                    spell.id().contains("storm") || spell.id().contains("wulei")));
        }
        return List.copyOf(moves);
    }

    public static List<Move> actorMoves(CompoundTag life, CompoundTag actor) {
        if (actor.getString("Id").equals("player") && actor.getList("Moves", Tag.TAG_COMPOUND).isEmpty()) return moves(life);
        List<Move> result = new ArrayList<>();
        for (var entry : actor.getList("Moves", Tag.TAG_COMPOUND)) {
            CompoundTag move = (CompoundTag) entry;
            result.add(new Move(move.getString("id"), move.getString("name_key"), move.getString("element"),
                    Mth.clamp(move.getDouble("potency"), 0, 2), move.getInt("accuracy"), Mth.clamp(move.getInt("priority"), -2, 2),
                    Mth.clamp(move.getDouble("qi_fraction"), 0, 1), Mth.clamp(move.getInt("cooldown"), 0, 10), move.getString("effect"), move.getBoolean("area")));
        }
        result.addAll(LifeSimulationBattleMechanics.additionalMoves(actor));
        result.addAll(BASE_MOVES); return List.copyOf(result);
    }

    public static boolean active(CompoundTag life) { return life.contains(ROOT, Tag.TAG_COMPOUND); }
    public static boolean pending(CompoundTag battle) { return battle.getInt("Reveal") < battle.getList("Frames", Tag.TAG_COMPOUND).size(); }

    public static void start(CompoundTag life, CompoundTag definition, RandomSource random) {
        startLegacy(life,definition,random);
        LifeCardBattle.initialize(life);
    }

    /** Compatibility path for already committed command battles and their persistence regressions. */
    public static void startLegacy(CompoundTag life, CompoundTag definition, RandomSource random) {
        if (active(life)) return;
        CompoundTag battle = new CompoundTag();
        battle.putUUID("Id", UUID.randomUUID());
        battle.putInt("Revision", 1);
        battle.putBoolean("LegacyCompatibility",true);
        battle.putInt("Round", 1);
        battle.putString("Arena", definition.getString("arena"));
        battle.putString("ObjectiveKey", definition.getString("objective_key"));
        battle.putString("Stakes", definition.contains("stakes") ? definition.getString("stakes") : "retreat");
        battle.putInt("Goal", Math.max(1, definition.getInt("goal")));
        battle.putInt("Limit", Math.max(6, definition.getInt("limit")));
        battle.putBoolean("Disaster", definition.getBoolean("disaster"));
        battle.putString("NextWin", definition.getString("win"));
        battle.putString("NextLoss", definition.getString("loss"));
        battle.putString("NextEscape", definition.getString("escape").isBlank() ? "withdraw" : definition.getString("escape"));
        battle.putString("NextSurrender", definition.getString("surrender").isBlank() ? "withdraw" : definition.getString("surrender"));
        battle.putBoolean("ObjectiveEnabled", definition.getBoolean("objective_enabled") || definition.getBoolean("disaster"));
        battle.putBoolean("AbandonedEnding", definition.getBoolean("abandoned_ending"));
        battle.putString("Story", definition.getString("story"));
        ListTag actors = new ListTag();
        java.util.Set<UUID> opposingNpcs = new java.util.HashSet<>();
        CompoundTag player = actor("player", life.getString("Name"), life.getInt("Realm"), false,
                LifeSimulationSprites.assignPerson(life), "player");
        player.putDouble("Hp", player.getDouble("MaxHp") * Mth.clamp(life.getInt("Health"), 1, 100) / 100.0);
        player.putInt("Accuracy", LifeSimulationDice.attribute(life.getInt("Comprehension")));
        player.putInt("Proficiency", Mth.clamp(life.getInt("Combat") / 30, 0, 4));
        actors.add(player);
        for (var tag : definition.getList("enemies", Tag.TAG_COMPOUND)) {
            CompoundTag enemy = (CompoundTag) tag;
            int realm = enemy.getInt("realm");
            if (battle.getBoolean("Disaster") && realm < 0) realm = life.getInt("Realm");
            CompoundTag linked = new CompoundTag();
            String role = enemy.getString("cast_role");
            CompoundTag cast = life.getCompound("Story").getCompound("Cast");
            if (!role.isBlank() && cast.hasUUID(role)) {
                for (var npcTag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                    CompoundTag npc = (CompoundTag) npcTag;
                    if (npc.hasUUID("Id") && npc.getUUID("Id").equals(cast.getUUID(role)) && npc.getBoolean("Alive")) linked = npc;
                }
                if (linked.isEmpty()) continue;
                realm = linked.getInt("Realm"); opposingNpcs.add(linked.getUUID("Id"));
            }
            String name = linked.isEmpty() ? enemy.getString("name_key") : linked.getString("NameKey").isBlank() ? linked.getString("Name") : linked.getString("NameKey");
            CompoundTag enemyActor = actor("enemy" + actors.size(), name, realm, true, enemy.getString("sprite"), enemy.getString("profile"));
            if(List.of("female","male").contains(enemy.getString("gender")))enemyActor.putString("Gender",enemy.getString("gender"));
            enemyActor.put("Moves", enemy.getList("moves", Tag.TAG_COMPOUND).copy());
            enemyActor.putBoolean("Hidden", enemy.getBoolean("hidden"));
            enemyActor.putString("CastRole", role);
            if (!linked.isEmpty()) {
                enemyActor.putUUID("NpcId", linked.getUUID("Id")); enemyActor.put("KnownTactics", linked.getList("LastEncounterTactics", Tag.TAG_STRING).copy());
                if (enemyActor.getString("Profile").equals("assassin") && !enemyActor.getList("KnownTactics", Tag.TAG_STRING).isEmpty()) enemyActor.putString("Profile", "rival_memory");
            }
            enemyActor.putString("Aspect", life.getCompound("Story").getString("MemoryAspect"));
            LifeSimulationSprites.assignActor(life, enemyActor, linked);
            actors.add(enemyActor);
            if (!linked.isEmpty() && !enemy.getBoolean("hidden")) LifeSimulationStories.observePerson(life, linked.getUUID("Id"));
        }
        battle.put("Actors", actors);
        LifeSimulationBattleMechanics.initialize(life, battle, definition);
        int allyCount = 0;
        for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
            CompoundTag p = (CompoundTag) tag;
            if (!eligibleAlly(p) || opposingNpcs.contains(p.getUUID("Id")) || p.getInt("Age") < 16 || p.getInt("InjuredUntil") > life.getInt("Age") || actors.size() >= 7 || allyCount >= 3) continue;
            allyCount++;
            CompoundTag ally = actor(p.getUUID("Id").toString(), p.getString("NameKey").isBlank() ? p.getString("Name") : p.getString("NameKey"), p.getInt("Realm"), false,
                    LifeSimulationSprites.assignPerson(p), "ally");
            ally.putUUID("NpcId", p.getUUID("Id"));
            ally.putBoolean("Protect", p.getBoolean("Protector"));
            ally.putInt("InsightModifier", LifeSimulationDice.attribute(p.contains("Comprehension") ? p.getInt("Comprehension") : 50));
            ally.putInt("Proficiency", Mth.clamp(p.getInt("Combat") / 30, 0, 4));
            actors.add(ally);
        }
        battle.put("Actors", actors);
        battle.put("Frames", new ListTag());
        battle.putString("Reaction", "brace");
        planEnemies(battle);
        life.put(ROOT, battle);
    }

    public static boolean eligibleAlly(CompoundTag p) {
        return !p.getBoolean("StoryNonCombatant") && p.getBoolean("Alive") && p.hasUUID("Id") && p.getInt("Affinity") >= 40
                && p.getInt("Trust") >= 40 && (p.getBoolean("Protector") || p.getBoolean("Friend"))
                && (!p.contains("Observed") || p.getBoolean("Observed")) && !p.getBoolean("Captive");
    }

    static CompoundTag actor(String id, String name, int realm, boolean enemy, String sprite, String profile) {
        realm = Mth.clamp(realm, 0, CultivationLevels.allLevels().size() - 1);
        var level = CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(realm));
        CompoundTag a = new CompoundTag();
        a.putString("Id", id); a.putString("Name", name); a.putInt("Realm", realm);
        a.putBoolean("Enemy", enemy); a.putString("Sprite", sprite.isBlank() ? "cultivator_front" : sprite);
        a.putString("Profile", profile); a.putDouble("MaxHp", Math.max(20, level.maxHp()));
        a.putDouble("Hp", a.getDouble("MaxHp")); a.putDouble("MaxQi", Math.max(30, level.maxQi()));
        a.putDouble("Qi", a.getDouble("MaxQi")); a.putDouble("Power", a.getDouble("MaxHp") * .19);
        a.putInt("Speed", Mth.clamp(realm / 3, 0, 5)); a.putInt("Accuracy", 2);
        a.putInt("Defense", 12); a.putDouble("ArmorFraction", .20);
        return a;
    }

    private static void planEnemies(CompoundTag b) {
        for (var tag : b.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag a = (CompoundTag) tag;
            a.putBoolean("ReactionAvailable", true);
            a.putInt("CurrentRound", b.getInt("Round"));
            if (!a.getBoolean("Enemy") || !LifeSimulationBattleMechanics.combatant(a)) continue;
            String action = switch (a.getString("Profile")) {
                case "defensive" -> b.getInt("Round") % 3 == 1 ? "shield" : "strike";
                case "assassin" -> b.getInt("Round") % 2 == 1 ? "quick" : "pierce";
                case "formation", "disaster" -> b.getInt("Round") % 2 == 1 ? "channel" : "heavy";
                case "soul" -> b.getInt("Round") % 3 == 0 ? "shield" : "pierce";
                case "beast" -> b.getInt("Round") % 3 == 0 ? "heavy" : "strike";
                default -> b.getInt("Round") % 2 == 0 ? "heavy" : "strike";
            };
            for (var moveTag : a.getList("Moves", Tag.TAG_COMPOUND)) {
                CompoundTag move = (CompoundTag) moveTag;
                if (move.getString("kind").equals(action)) { action = move.getString("id"); break; }
            }
            action = LifeSimulationBattleMechanics.plan(b, a, action);
            Move planned = lookup(new CompoundTag(), a, action);
            if (planned != null && (a.getDouble("Qi") < cost(a, planned)
                    || a.getCompound("Cooldowns").getInt(planned.id()) > b.getInt("Round"))) action = "strike";
            a.putString("Plan", action); // Committed before the player selects an action.
        }
    }

    public static String illegal(CompoundTag life, String moveId, String targetId) {
        CompoundTag b = life.getCompound(ROOT);
        if (!active(life) || pending(b) || !b.getString("Result").isBlank()) return "busy";
        CompoundTag player = find(b, "player");
        CompoundTag target = find(b, targetId);
        if (target.isEmpty() || target.getDouble("Hp") <= 0 || target.getBoolean("Withdrawn") || target.getBoolean("Hidden")) return "target";
        if (target.getBoolean("Locked")) return "stage";
        Move move = moves(life).stream().filter(m -> m.id().equals(moveId)).findFirst().orElse(null);
        if (move == null) return "move";
        boolean support = move.effect().equals("heal") || move.effect().equals("shield");
        if (target.getBoolean("Protected") && !support) return "protected";
        if (target.getBoolean("Enemy") == support) return "target";
        if (player.getDouble("Qi") < cost(player, move)) return "qi";
        if (player.getCompound("Cooldowns").getInt(move.id()) > b.getInt("Round")) return "cooldown";
        return "";
    }

    public static CompoundTag preview(CompoundTag life, Move move, String targetId) {
        CompoundTag b = life.getCompound(ROOT), a = find(b, "player"), target = find(b, targetId);
        CompoundTag preview = new CompoundTag();
        preview.putString("Illegal", illegal(life, move.id(), targetId));
        int modifier = attackModifier(a, target, move);
        boolean possible = plausible(a, target);
        preview.putDouble("Hit", LifeSimulationDice.hitChance(modifier, target.getInt("Defense"), possible));
        preview.putDouble("Low", damage(a, target, move, .75, false));
        preview.putDouble("High", damage(a, target, move, 1.25, false));
        preview.putDouble("Qi", cost(a, move));
        return preview;
    }

    public static void handle(CompoundTag life, String payload, RandomSource random) {
        if(LifeCardBattle.enabled(life.getCompound(ROOT))){LifeCardBattle.handle(life,payload,random);return;}
        CompoundTag b = life.getCompound(ROOT);
        String[] p = payload.split(":", 5);
        if (!active(life) || p.length < 3 || !b.getUUID("Id").toString().equals(p[0])) return;
        int revision;
        try { revision = Integer.parseInt(p[1]); } catch (NumberFormatException e) { return; }
        if (revision != b.getInt("Revision")) return;
        if ((p[2].equals("reveal") || p[2].equals("skip")) && pending(b)) {
            b.putInt("Reveal", p[2].equals("skip") ? b.getList("Frames", Tag.TAG_COMPOUND).size() : b.getInt("Reveal") + 1);
            if (!pending(b) && b.getString("Result").isBlank()) {
                b.putInt("Round", b.getInt("Round") + 1);
                planEnemies(b);
            }
        } else if (p[2].equals("reaction") && !pending(b) && p.length == 4
                && List.of("brace", "dodge", "counter", "disrupt", "preserve").contains(p[3])) {
            b.putString("Reaction", p[3]);
        } else if (p[2].equals("ally") && !pending(b) && p.length == 5) {
            CompoundTag ally = find(b, p[3]);
            if (ally.isEmpty() || ally.getString("Kind").equals("anchor") || ally.getBoolean("Hidden") || ally.getBoolean("Enemy") || ally.getString("Id").equals("player") || ally.getDouble("Hp") <= 0) return;
            if (p[4].startsWith("objective@")) {
                String anchor = p[4].substring(10);
                if (!LifeSimulationBattleMechanics.objectiveIllegalFor(life, ally, anchor).isBlank()) return;
                ally.putString("ObjectiveTarget", anchor); ally.putBoolean("Withdrawn", false); ally.putBoolean("Protect", false);
            } else {
                if (!List.of("protect", "attack", "withdraw", "return").contains(p[4])) return;
                ally.remove("ObjectiveTarget"); ally.putBoolean("Protect", p[4].equals("protect"));
                ally.putBoolean("Withdrawn", p[4].equals("withdraw"));
            }
        } else if (p[2].equals("plan") && p.length == 5 && !pending(b) && b.getString("Result").isBlank()) {
            if (!legalAction(life, p[3], p[4])) return;
            commitRound(life, p[3], p[4], random);
        } else return;
        b.putInt("Revision", revision + 1);
        life.put(ROOT, b);
    }

    private static boolean legalAction(CompoundTag life, String action, String target) {
        CompoundTag player = find(life.getCompound(ROOT), "player");
        if (action.equals("objective")) return supportsObjective(life.getCompound(ROOT)) && objectiveIllegal(life, target).isBlank();
        if (action.startsWith("bag_")) return itemCount(life, action.substring(4).replace('~', ':')) > 0 && consumable(action.substring(4).replace('~', ':'));
        if (List.of("guard", "reposition", "recover", "inspect", "escape", "surrender", "objective").contains(action)) return true;
        return illegal(life, action, target).isBlank() && player.getDouble("Hp") > 0;
    }

    /** Only an authored objective can finish combat without defeating its opponents. */
    public static boolean supportsObjective(CompoundTag battle) { return battle.getBoolean("ObjectiveEnabled"); }

    public static String objectiveIllegal(CompoundTag life, String target) { return LifeSimulationBattleMechanics.objectiveIllegal(life, target); }
    public static String allyObjectiveIllegal(CompoundTag life, String ally, String target) {
        CompoundTag actor = find(life.getCompound(ROOT), ally);
        if (actor.isEmpty() || actor.getBoolean("Enemy") || actor.getString("Kind").equals("anchor") || actor.getBoolean("Hidden") || actor.getDouble("Hp") <= 0) return "target";
        return LifeSimulationBattleMechanics.objectiveIllegalFor(life, actor, target);
    }
    public static List<CompoundTag> activeTargets(CompoundTag battle) {
        return battle.getList("Actors", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                .filter(a -> a.getDouble("Hp") > 0 && !a.getBoolean("Withdrawn") && !a.getBoolean("Hidden") && !a.getBoolean("Locked")).toList();
    }

    private static void commitRound(CompoundTag life, String action, String targetId, RandomSource random) {
        CompoundTag b = life.getCompound(ROOT);
        ListTag actors = b.getList("Actors", Tag.TAG_COMPOUND), frames = new ListTag();
        b.put("Frames", frames); b.putInt("Reveal", 0);
        b.putString("ReservedAction", action); b.putString("ReservedTarget", targetId);
        List<CompoundTag> queue = new ArrayList<>();
        LifeSimulationBattleMechanics.startRound(life, b, random);
        b.put("RoundStartActors", actors.copy());
        b.putInt("RoundStartPhase", b.getInt("Phase")); b.putString("RoundStartPhaseKey", b.getString("PhaseKey"));
        b.putInt("RoundStartObjective", b.getInt("Objective")); b.putInt("RoundStartHazard", b.getInt("Hazard"));
        for (var tag : actors) {
            CompoundTag a = (CompoundTag) tag;
            if (a.getDouble("Hp") <= 0 || !LifeSimulationBattleMechanics.combatant(a)) continue;
            if (a.getString("Id").equals("player")) a.putString("Plan", action);
            else if (!a.getBoolean("Enemy")) a.putString("Plan", a.getString("ObjectiveTarget").isBlank() ? a.getBoolean("Protect") ? "guard" : "strike" : "objective");
            Move move = lookup(life, a, a.getString("Plan"));
            CompoundTag initiative = LifeSimulationDice.check(random, "initiative", a.getInt("Speed") + (move == null ? 0 : move.priority())
                    - (a.getInt("SlowUntil") > b.getInt("Round") ? 2 : 0), 0, false, false);
            a.putInt("Initiative", initiative.getInt("Total")); a.put("InitiativeRoll", initiative);
            LifeSimulationDice.remember(life, initiative);
            a.putBoolean("Guard", a.getString("Plan").equals("rival_counter") && a.getList("KnownTactics", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(action)));
            a.putBoolean("ReactionAvailable", true);
            queue.add(a);
        }
        queue.sort(Comparator.<CompoundTag>comparingInt(a -> a.getInt("Initiative")).reversed()
                .thenComparing(Comparator.<CompoundTag>comparingInt(a -> a.getInt("Speed")).reversed())
                .thenComparing(a -> a.getString("Id")));
        ListTag order = new ListTag();
        for (CompoundTag a : queue) order.add(StringTag.valueOf(a.getString("Id")));
        b.put("Queue", order);
        for (CompoundTag a : queue) {
            if (a.getDouble("Hp") <= 0 || !b.getString("Result").isBlank()) continue;
            CompoundTag target = a.getBoolean("Enemy") ? weakest(b, false) : a.getString("Id").equals("player") ? find(b, targetId)
                    : a.getString("Plan").equals("objective") ? find(b, a.getString("ObjectiveTarget")) : weakest(b, true);
            resolve(life, b, a, target, random, false);
            LifeSimulationBattleMechanics.afterAction(life, b, a, random);
            frame(b, a.getString("Id"), a.getString("Plan"));
            checkEnd(b);
        }
        // Status duration is a global round expiry, so a fresh two-round status survives this round.
        for (var tag : actors) {
            CompoundTag a = (CompoundTag) tag;
            if (b.getString("Result").isBlank() && a.getDouble("Hp") > 0 && a.getInt("BurnUntil") > b.getInt("Round")) {
                a.putDouble("Hp", Math.max(0, a.getDouble("Hp") - a.getDouble("MaxHp") * .03));
            }
        }
        if (b.getString("Result").isBlank()) LifeSimulationBattleMechanics.endRound(life, b);
        if (b.getBoolean("Disaster") && b.getInt("Objective") < b.getInt("Goal") && b.getString("Mechanic").isBlank()) b.putInt("Hazard", b.getInt("Hazard") + 1);
        checkEnd(b);
        if (b.getString("Result").isBlank() && b.getInt("Round") >= b.getInt("Limit")) {
            b.putBoolean("TimedOut", true); b.putString("Result", "escape");
        }
        b.put("LastDetail", new CompoundTag());
        frame(b, "", "round_end");
        b.remove("ReservedAction"); b.remove("ReservedTarget");
    }

    private static void resolve(CompoundTag life, CompoundTag b, CompoundTag a, CompoundTag target, RandomSource random, boolean counter) {
        String action = a.getString("Plan");
        CompoundTag detail = new CompoundTag();
        detail.putString("Actor", a.getString("Id")); detail.putString("Target", target.getString("Id"));
        detail.putString("Action", action); detail.putString("Outcome", "success");
        b.put("LastDetail", detail);
        if (action.equals("guard")) { a.putBoolean("Guard", true); return; }
        if (action.equals("channel")) { a.putBoolean("Channelled", true); return; }
        if (action.equals("recover")) {
            CompoundTag roll = LifeSimulationDice.damage(random, 1, 4); detail.put("Roll", roll);
            a.putDouble("Qi", Math.min(a.getDouble("MaxQi"), a.getDouble("Qi") + a.getDouble("MaxQi") * (.06 + roll.getInt("Total") * .02)));
            LifeSimulationDice.remember(life, roll); return;
        }
        if (action.equals("reposition")) { a.putInt("CoverUntil", b.getInt("Round") + 2); return; }
        if (action.equals("inspect")) { LifeSimulationBattleMechanics.inspect(life, b, target); return; }
        if (action.equals("surrender")) { b.putString("Result", "surrender"); return; }
        if (action.equals("escape")) {
            CompoundTag enemy = weakest(b, true);
            boolean overwhelming = a.getInt("Realm") - enemy.getInt("Realm") >= 6;
            CompoundTag roll = LifeSimulationDice.check(random, "escape", a.getInt("Speed") - enemy.getInt("Speed") + 2, 12 + Math.min(5, b.getInt("Hazard")), a.getInt("CoverUntil") > b.getInt("Round"), false);
            detail.put("Roll", roll); LifeSimulationDice.remember(life, roll);
            if (overwhelming || roll.getInt("Degree") >= 2) b.putString("Result", "escape"); else detail.putString("Outcome", "failure");
            return;
        }
        if (action.equals("objective")) {
            if (LifeSimulationBattleMechanics.objective(life, b, a, target, random, detail)) return;
            CompoundTag roll = LifeSimulationDice.check(random, "objective", LifeSimulationDice.attribute(life.getInt("Comprehension")) + a.getInt("Proficiency"), 14, a.getInt("CoverUntil") > b.getInt("Round"), false);
            detail.put("Roll", roll); LifeSimulationDice.remember(life, roll);
            if (roll.getInt("Degree") >= 2) { b.putInt("Objective", b.getInt("Objective") + 1); b.putInt("Hazard", Math.max(0, b.getInt("Hazard") - 1)); }
            else if (roll.getInt("Degree") == 1) { b.putInt("Objective", b.getInt("Objective") + 1); b.putInt("Hazard", b.getInt("Hazard") + 1); }
            else b.putInt("Hazard", b.getInt("Hazard") + 1);
            return;
        }
        if (action.startsWith("bag_")) {
            String id = action.substring(4).replace('~', ':');
            if (!consume(life, id)) return;
            if (id.contains("qi")) a.putDouble("Qi", Math.min(a.getDouble("MaxQi"), a.getDouble("Qi") + a.getDouble("MaxQi") * .25));
            else if (id.contains("escape")) b.putString("Result", "escape");
            else if (id.contains("honey") || id.contains("qingdu")) { a.remove("BurnUntil"); a.remove("SlowUntil"); }
            else a.putDouble("Hp", Math.min(a.getDouble("MaxHp"), a.getDouble("Hp") + a.getDouble("MaxHp") * .20));
            return;
        }
        if (target.isEmpty() || target.getDouble("Hp") <= 0 || target.getBoolean("Withdrawn")) { a.putBoolean("Guard", true); detail.putString("Outcome", "retarget_guard"); return; }
        Move move = lookup(life, a, action);
        if (move == null || a.getDouble("Qi") < cost(a, move)) { a.putBoolean("Guard", true); detail.putString("Outcome", "qi"); return; }
        if (a.getCompound("Cooldowns").getInt(move.id()) > b.getInt("Round")) { a.putBoolean("Guard", true); detail.putString("Outcome", "retarget_guard"); return; }
        a.putDouble("Qi", a.getDouble("Qi") - cost(a, move));
        CompoundTag cooldowns = a.getCompound("Cooldowns"); cooldowns.putInt(move.id(), b.getInt("Round") + move.cooldown() + 1); a.put("Cooldowns", cooldowns);
        if (LifeSimulationBattleMechanics.specialSupport(b, a, move, random, detail)) { LifeSimulationDice.remember(life, detail.getCompound("DamageRoll")); return; }
        if (move.effect().equals("shield") || move.effect().equals("heal")) {
            if (a.getBoolean("Enemy")) target = a;
            detail.putString("Target", target.getString("Id"));
            CompoundTag roll = LifeSimulationDice.damage(random, 2, 6); detail.put("DamageRoll", roll); LifeSimulationDice.remember(life, roll);
            double amount = a.getDouble("Power") * (move.effect().equals("heal") ? .7 : .8) * roll.getDouble("Factor");
            if (move.effect().equals("heal")) target.putDouble("Hp", Math.min(target.getDouble("MaxHp"), target.getDouble("Hp") + amount));
            else target.putDouble("Shield", Math.min(target.getDouble("MaxHp") * .4, target.getDouble("Shield") + amount));
            detail.putDouble("Amount", amount); return;
        }
        if (a.getBoolean("Enemy") && !move.area()) {
            for (var tag : b.getList("Actors", Tag.TAG_COMPOUND)) {
                CompoundTag ally = (CompoundTag) tag;
                if (!ally.getBoolean("Enemy") && ally.getBoolean("Protect") && ally.getDouble("Hp") > 0
                        && !ally.getBoolean("Withdrawn") && ally.getBoolean("ReactionAvailable")) {
                    target = ally; target.putBoolean("ReactionAvailable", false); detail.putString("Target", ally.getString("Id")); break;
                }
            }
        }
        ListTag hits = new ListTag();
        if (move.area()) {
            for (var tag : b.getList("Actors", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) tag;
                if (t.getBoolean("Enemy") != a.getBoolean("Enemy") && t.getDouble("Hp") > 0 && !t.getBoolean("Withdrawn")
                        && !t.getBoolean("Hidden") && !t.getBoolean("Locked") && !t.getBoolean("Protected")) hit(life, b, a, t, move, random, hits, counter);
            }
        } else hit(life, b, a, target, move, random, hits, counter);
        detail.put("Hits", hits);
    }

    private static void hit(CompoundTag life, CompoundTag b, CompoundTag a, CompoundTag target, Move move, RandomSource random, ListTag hits, boolean counter) {
        CompoundTag hit = new CompoundTag(); hit.putString("Target", target.getString("Id")); hits.add(hit);
        LifeSimulationBattleMechanics.beforeHit(b, a, target, hit);
        if (!plausible(a, target)) { hit.putString("Outcome", "realm_block"); return; }
        String reaction = target.getString("Id").equals("player") ? b.getString("Reaction") : target.getString("Profile").equals("rival_memory") ? "counter" : "brace";
        boolean react = target.getBoolean("ReactionAvailable") && !counter;
        if (react) target.putBoolean("ReactionAvailable", false);
        if (react && reaction.equals("disrupt") && a.getBoolean("Channelled")) {
            CompoundTag disrupt = LifeSimulationDice.check(random, "disrupt", target.getInt("Accuracy"), 14, false, false);
            hit.put("Resistance", disrupt); LifeSimulationDice.remember(life, disrupt);
            if (disrupt.getInt("Degree") >= 2) { a.remove("Channelled"); hit.putString("Outcome", "disrupt"); return; }
        }
        int defense = target.getInt("Defense") + (target.getInt("CoverUntil") > b.getInt("Round") ? 2 : 0)
                + (react && reaction.equals("dodge") ? 4 : 0);
        CompoundTag roll = LifeSimulationDice.check(random, "attack", attackModifier(a, target, move), defense,
                a.getBoolean("Channelled"), a.getInt("SlowUntil") > b.getInt("Round"));
        hit.put("Attack", roll); LifeSimulationDice.remember(life, roll);
        int die = roll.getInt("Selected");
        if (die == 1 || die != 20 && roll.getInt("Total") < defense) { a.remove("Channelled"); hit.putString("Outcome", "miss"); return; }
        CompoundTag damageRoll = LifeSimulationDice.damage(random, 2, 6); LifeSimulationDice.remember(life, damageRoll);
        hit.put("DamageRoll", damageRoll);
        double raw = damage(a, target, move, damageRoll.getDouble("Factor"), die == 20);
        hit.putDouble("Raw", a.getDouble("Power") * move.potency() * damageRoll.getDouble("Factor") * (die == 20 ? 1.5 : 1));
        hit.putDouble("Armor", target.getDouble("ArmorFraction") * (move.effect().equals("pierce") ? .5 : 1));
        if (target.getBoolean("Guard") || react && reaction.equals("brace")) raw *= .7; // Stronger only; no multiplicative guard stack.
        double shield = Math.min(raw, target.getDouble("Shield"));
        target.putDouble("Shield", target.getDouble("Shield") - shield);
        target.putDouble("Hp", Math.max(0, target.getDouble("Hp") - (raw - shield)));
        hit.putDouble("Shield", shield); hit.putDouble("HpDamage", raw - shield);
        hit.putString("Outcome", die == 20 ? "critical" : "hit"); hit.putBoolean("Applied", true);
        if (!move.effect().isBlank() && !move.effect().equals("pierce")) {
            CompoundTag resistance = LifeSimulationDice.check(random, "resistance", target.getInt("Speed"), 14, false, false);
            hit.put("Resistance", resistance); LifeSimulationDice.remember(life, resistance);
            if (resistance.getInt("Degree") < 2) {
                if (move.effect().equals("burn")) target.putInt("BurnUntil", b.getInt("Round") + 3);
                if (move.effect().equals("slow")) target.putInt("SlowUntil", b.getInt("Round") + 3);
                if (move.effect().equals("disrupt")) target.remove("Channelled");
                if (move.effect().equals("obscure")) target.putInt("ObscuredUntil", b.getInt("Round") + 3);
                if (move.effect().equals("pressure")) target.putInt("PressureUntil", b.getInt("Round") + 3);
            }
        }
        a.remove("Channelled");
        if (react && reaction.equals("counter") && target.getDouble("Hp") > 0 && a.getDouble("Hp") > 0) {
            // One bounded counter, never a recursive reaction chain.
            CompoundTag responseRoll = LifeSimulationDice.damage(random, 2, 6);
            LifeSimulationDice.remember(life, responseRoll); hit.put("CounterRoll", responseRoll);
            double response = damage(target, a, move("counter", .5, 0, 0, 0, 0, "", false), responseRoll.getDouble("Factor"), false);
            double absorbed = Math.min(response, a.getDouble("Shield"));
            a.putDouble("Shield", a.getDouble("Shield") - absorbed);
            a.putDouble("Hp", Math.max(0, a.getDouble("Hp") - (response - absorbed))); hit.putDouble("Counter", response - absorbed);
        }
    }

    public static boolean plausible(CompoundTag a, CompoundTag target) { return a.getInt("Realm") + 6 > target.getInt("Realm"); }
    private static int attackModifier(CompoundTag a, CompoundTag target, Move move) {
        return a.getInt("Accuracy") + a.getInt("Proficiency") + move.accuracy() + Mth.clamp(a.getInt("Realm") - target.getInt("Realm"), -4, 4)
                - (a.getInt("ObscuredUntil") > a.getInt("CurrentRound") ? 2 : 0) - (a.getInt("PressureUntil") > a.getInt("CurrentRound") ? 1 : 0);
    }
    public static double damage(CompoundTag a, CompoundTag target, Move move, double factor, boolean critical) {
        if (!plausible(a, target)) return 0;
        double armor = target.getDouble("ArmorFraction") * (move.effect().equals("pierce") ? .5 : 1);
        return a.getDouble("Power") * move.potency() * factor * (critical ? 1.5 : 1) * (1 - armor);
    }
    public static double cost(CompoundTag actor, Move move) { return actor.getDouble("MaxQi") * move.qiFraction(); }
    private static Move lookup(CompoundTag life, String id) { return moves(life).stream().filter(m -> m.id().equals(id)).findFirst().orElse(null); }
    private static Move lookup(CompoundTag life, CompoundTag actor, String id) { return actorMoves(life, actor).stream().filter(m -> m.id().equals(id)).findFirst().orElse(null); }
    public static CompoundTag find(CompoundTag b, String id) {
        for (var tag : b.getList("Actors", Tag.TAG_COMPOUND)) if (((CompoundTag) tag).getString("Id").equals(id)) return (CompoundTag) tag;
        return new CompoundTag();
    }
    private static CompoundTag weakest(CompoundTag b, boolean enemy) {
        return b.getList("Actors", Tag.TAG_COMPOUND).stream().map(tag -> (CompoundTag) tag)
                .filter(a -> a.getBoolean("Enemy") == enemy && a.getDouble("Hp") > 0 && LifeSimulationBattleMechanics.combatant(a))
                .min(Comparator.comparingDouble(a -> a.getDouble("Hp") / a.getDouble("MaxHp"))).orElse(new CompoundTag());
    }
    private static void checkEnd(CompoundTag b) {
        if (!b.getString("Result").isBlank()) return;
        if (find(b, "player").getDouble("Hp") <= 0) b.putString("Result", "defeat");
        else if (b.getInt("Hazard") >= (b.contains("HazardLimit") ? b.getInt("HazardLimit") : b.getInt("Limit"))) {
            boolean immune = b.getString("Mechanic").equals("soul_control") && b.getList("Actors", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                    .filter(a -> a.getBoolean("Enemy") && LifeSimulationBattleMechanics.combatant(a)).noneMatch(a -> plausible(a, find(b, "player")));
            b.putString("Result", immune ? "escape" : "defeat");
        }
        else if (LifeSimulationBattleMechanics.victory(b, weakest(b, true).isEmpty())) b.putString("Result", "victory");
    }
    private static void frame(CompoundTag b, String actor, String action) {
        CompoundTag frame = b.getCompound("LastDetail").copy();
        frame.putString("Actor", actor); frame.putString("Action", action);
        frame.put("Actors", b.getList("Actors", Tag.TAG_COMPOUND).copy());
        frame.putInt("Phase", b.getInt("Phase")); frame.putString("PhaseKey", b.getString("PhaseKey"));
        frame.putInt("Objective", b.getInt("Objective")); frame.putInt("Hazard", b.getInt("Hazard"));
        frame.putBoolean("Applied", true); b.getList("Frames", Tag.TAG_COMPOUND).add(frame);
    }
    public static boolean consumable(String id) {
        return id.contains("qi_recovery") || id.contains("healing") || id.contains("rejuvenation")
                || id.contains("escape_talisman") || id.equals("minecraft:honey_bottle") || id.contains("qingdu");
    }
    public static int itemCount(CompoundTag life, String id) {
        for (var tag : life.getList("Inventory", Tag.TAG_COMPOUND)) if (((CompoundTag) tag).getString("Id").equals(id)) return ((CompoundTag) tag).getInt("Count");
        return 0;
    }
    private static boolean consume(CompoundTag life, String id) {
        for (var tag : life.getList("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) tag;
            if (item.getString("Id").equals(id) && item.getInt("Count") > 0) { item.putInt("Count", item.getInt("Count") - 1); return true; }
        }
        return false;
    }
}
