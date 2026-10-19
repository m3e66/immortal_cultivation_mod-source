package com.example.immortal_cultivation_mod.system;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Shared data-driven authored scenes, checks, persistent cast, and delayed endings. */
public final class LifeSimulationStories {
    private LifeSimulationStories() {}

    public static CompoundTag breakthroughTrial(ResourceManager resources) {
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","life_simulation/trials/tribulation.json");
        var resource=resources.getResource(id);
        if(resource.isEmpty())return null;
        try(var reader=resource.get().openAsReader()) {
            return (CompoundTag)JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE,JsonParser.parseReader(reader));
        }catch(Exception exception){com.mojang.logging.LogUtils.getLogger().error("Invalid breakthrough trial",exception);return null;}
    }

    public static List<CompoundTag> definitions(ResourceManager resources) {
        List<CompoundTag> result = new ArrayList<>();
        resources.listResources("life_simulation/stories", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (var reader = resource.openAsReader()) {
                CompoundTag root = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, JsonParser.parseReader(reader));
                for (var tag : root.getList("stories", Tag.TAG_COMPOUND)) result.add(((CompoundTag) tag).copy());
            } catch (Exception exception) { com.mojang.logging.LogUtils.getLogger().error("Invalid life story {}", id, exception); }
        });
        return List.copyOf(result);
    }

    /** One-time repair of authored NPCs in old lives and archived relationship records. */
    static void normalizeCastGenders(CompoundTag life, ResourceManager resources) {
        List<CompoundTag> lives=new ArrayList<>();lives.add(life);
        for(var tag:life.getList("Memories",Tag.TAG_COMPOUND))lives.add((CompoundTag)tag);
        if(lives.stream().noneMatch(l->!l.getString("Name").isBlank()&&l.getInt("SpriteGenderVersion")<1))return;
        java.util.Map<String,String> genders=new java.util.HashMap<>();
        for(var definition:definitions(resources))for(var tag:definition.getList("cast",Tag.TAG_COMPOUND)) {
            var rule=(CompoundTag)tag;String gender=rule.getString("gender");
            if(!List.of("female","male").contains(gender))continue;
            if(!rule.getString("name_key").isBlank())genders.put(rule.getString("name_key"),gender);
            if(!rule.getString("name").isBlank())genders.put(rule.getString("name"),gender);
        }
        for(var saved:lives) {
            if(saved.getString("Name").isBlank()||saved.getInt("SpriteGenderVersion")>=1)continue;
            List<CompoundTag> people=new ArrayList<>();
            for(var tag:saved.getList("Relationships",Tag.TAG_COMPOUND))people.add((CompoundTag)tag);
            var memorials=saved.getCompound("NpcMemorials");
            for(String id:memorials.getAllKeys())people.add(memorials.getCompound(id));
            for(var person:people) {
                String gender=genders.getOrDefault(person.getString("NameKey"),genders.get(person.getString("Name")));
                if(gender!=null)person.putString("Gender",gender);
            }
            saved.putInt("SpriteGenderVersion",1);
        }
    }

    public static boolean select(CompoundTag life, ResourceManager resources, String category, RandomSource random) {
        if (life.contains("Story", Tag.TAG_COMPOUND) || LifeSimulationBattle.active(life)) return false;
        List<CompoundTag> eligible = definitions(resources).stream().filter(d -> (d.getString("category").equals(category)
                || d.getBoolean("replacement") && !category.equals("danger")) && eligible(life, d)).toList();
        if (eligible.isEmpty()) return false;
        return start(life, eligible.get(random.nextInt(eligible.size())));
    }

    public static boolean eligible(CompoundTag life, CompoundTag definition) {
        if (definition.getBoolean("replacement") && (!LifeAuthoredStories.conscious(life)
                || life.getInt("Age") < definition.getInt("minimum_age"))) return false;
        String id = definition.getString("id");
        CompoundTag completed = life.getCompound("StoryJournal").getCompound(id);
        boolean retry = id.equals("life_danger_tribulation") && !completed.getString("Ending").equals("a");
        if (!completed.isEmpty() && (!definition.getBoolean("repeat") || !retry && completed.getInt("Realm") >= life.getInt("Realm"))) return false;
        int realm = life.getInt("Realm");
        if (realm < definition.getInt("minimum_realm") || realm > definition.getInt("maximum_realm")) return false;
        if (life.getInt("Age") - life.getInt("LastMajorCrisis") < definition.getInt("cooldown")) return false;
        if (definition.getBoolean("sect") && (definition.getBoolean("replacement")
                ?!LifeAuthoredStories.sectAffiliated(life):"none".equals(life.getString("Sect")))) return false;
        for (var tag : definition.getList("conditions", Tag.TAG_STRING)) if (!LifeSimulationSystem.matchesCondition(life, tag.getAsString())) return false;
        String idType = definition.getString("trigger");
        return switch (idType) {
            case "rival" -> livingRelation(life, true) != null;
            case "friend" -> livingRelation(life, false) != null;
            case "strain" -> life.getInt("TrainingStrain") >= 60;
            case "oath" -> life.getCompound("SpiritualOath").getBoolean("Broken");
            case "pill" -> life.getBoolean("SuspiciousPillTaken");
            case "war" -> life.getBoolean("RegionalWar");
            case "vein" -> life.getBoolean("VeinDamaged") || life.getBoolean("NaturalVeinWarning");
            case "dynasty" -> life.getBoolean("RegionalCrisis");
            case "breakthrough" -> life.getInt("Realm") >= 6 && (life.getInt("Realm") + 1) % 3 == 1
                    && LifeSimulationBalance.supported(life, life.getInt("Realm") + 1)
                    && life.getLong("CultivationProgress") >= com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(
                            com.example.immortal_cultivation_mod.attachment.CultivationLevels.allLevels().get(life.getInt("Realm"))).qiNeeded();
            default -> true;
        };
    }

    public static CompoundTag livingRelation(CompoundTag life, boolean rival) {
        return life.getList("Relationships", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t)
                .filter(p -> p.getBoolean("Alive") && p.hasUUID("Id") && (!p.contains("Observed") || p.getBoolean("Observed")) && !p.getBoolean("Captive")
                        && (rival ? p.getBoolean("Nemesis") && !p.getString("Grievance").isBlank() : p.getBoolean("Friend") && p.getInt("Trust") >= 40))
                .findFirst().orElse(null);
    }

    /** Updates only authored rules in old pending saves, retaining their identities, resources and dice. */
    public static void upgradePendingDefinition(CompoundTag life, ResourceManager resources) {
        CompoundTag story = life.getCompound("Story"), old = story.getCompound("Definition");
        // New encounters use the replacement pool. Already committed old choices
        // and dice finish from their saved definition; history is never discarded.
        if (story.isEmpty() || old.getInt("version") >= 4) return;
        CompoundTag updated = definitions(resources).stream().filter(d -> d.getString("id").equals(story.getString("Id"))).findFirst().orElse(null);
        if (updated == null || updated.getInt("version") <= old.getInt("version")) return;
        CompoundTag cast = story.getCompound("Cast");
        for (var tag : updated.getList("cast", Tag.TAG_COMPOUND)) {
            CompoundTag rule = (CompoundTag) tag;
            if (cast.hasUUID(rule.getString("role")) || !rule.getString("existing").isBlank()) continue;
            CompoundTag npc = new CompoundTag(); npc.putUUID("Id", UUID.randomUUID());
            npc.putString("Name", rule.getString("name")); npc.putString("NameKey", rule.getString("name_key"));
            npc.putString("Relation", "acquaintance"); npc.putInt("Age", rule.getInt("age"));
            npc.putInt("BirthYear", life.getInt("Age") - rule.getInt("age")); npc.putString("Kind", rule.getString("kind"));
            boolean living = !List.of("memory", "remnant", "hazard").contains(rule.getString("kind"));
            npc.putInt("Realm", rule.getInt("realm")); npc.putBoolean("Alive", living); npc.putBoolean("Available", false); npc.putBoolean("Observed", false);
            npc.putInt("Health", living ? 80 : 0); npc.putString("Occupation", "sect_disciple"); npc.putString("Personality", "steady");
            life.getList("Relationships", Tag.TAG_COMPOUND).add(npc); cast.putUUID(rule.getString("role"), npc.getUUID("Id"));
        }
        ListTag path = story.getList("Path", Tag.TAG_STRING);
        String last = path.isEmpty() ? "" : path.getString(path.size() - 1);
        String[] selection = last.split(":", 2);
        if (selection.length == 2) {
            CompoundTag before = choiceById(old, selection[0], selection[1]);
            CompoundTag after = choiceById(updated, selection[0], selection[1]);
            if (!before.isEmpty() && !after.isEmpty()) {
                String current = story.getString("Node");
                if (!current.equals(selection[0])) {
                    if (current.equals(before.getString("next"))) story.putString("Node", after.getString("next"));
                    else if (current.equals(before.getString("failure"))) story.putString("Node", after.getString("failure"));
                }
                if (story.contains("PendingChoice")) story.put("PendingChoice", after.copy());
                CompoundTag battle = life.getCompound(LifeSimulationBattle.ROOT);
                if (!battle.isEmpty() && after.contains("battle")) {
                    CompoundTag rules = after.getCompound("battle");
                    battle.putString("NextWin", after.getString("next")); battle.putString("NextLoss", after.getString("failure"));
                    battle.putString("NextEscape", rules.getString("escape").isBlank() ? "withdraw" : rules.getString("escape"));
                    battle.putString("NextSurrender", rules.getString("surrender").isBlank() ? "withdraw" : rules.getString("surrender"));
                    battle.putBoolean("AbandonedEnding", rules.getBoolean("abandoned_ending"));
                    battle.putBoolean("ObjectiveEnabled", rules.getBoolean("objective_enabled") || rules.getBoolean("disaster"));
                }
            }
        }
        story.put("Definition", updated.copy());
        if (story.getString("MemoryKey").isBlank()) recordStoryHistory(life, story);
        if (story.getString("SourceLocation").isBlank()) story.putString("SourceLocation", LifeSimulationWorldMap.state(life).getString(LifeSimulationWorldMap.CURRENT_LOCATION));
        if (!story.contains("PendingRoll") && !LifeSimulationBattle.active(life)) refreshNode(life, story.getString("Node"));
        com.mojang.logging.LogUtils.getLogger().info("Updated pending life story {} to definition version {}", story.getString("Id"), updated.getInt("version"));
    }

    private static CompoundTag choiceById(CompoundTag definition, String node, String id) {
        for (var tag : node(definition, node).getList("choices", Tag.TAG_COMPOUND)) if (((CompoundTag) tag).getString("id").equals(id)) return (CompoundTag) tag;
        return new CompoundTag();
    }

    public static boolean start(CompoundTag life, CompoundTag definition) {
        if (life.contains("Story", Tag.TAG_COMPOUND) || LifeSimulationBattle.active(life)) return false;
        if (definition.getBoolean("replacement") && !eligible(life, definition)) return false;
        CompoundTag story = new CompoundTag();
        ListTag relationships = life.getList("Relationships", Tag.TAG_COMPOUND); life.put("Relationships", relationships);
        story.putString("Id", definition.getString("id")); story.putUUID("Instance", UUID.randomUUID());
        story.put("Definition", definition.copy()); story.putInt("Revision", 1);
        story.putInt("StartedYear", life.getInt("Age")); story.putString("Node", "start");
        LifeSimulationWorldMap.normalize(life);
        story.putString("SourceLocation", LifeSimulationWorldMap.state(life).getString(LifeSimulationWorldMap.CURRENT_LOCATION));
        CompoundTag cast = new CompoundTag();
        for (var tag : definition.getList("cast", Tag.TAG_COMPOUND)) {
            CompoundTag rule = (CompoundTag) tag;
            CompoundTag existing = switch (rule.getString("existing")) {
                case "rival" -> livingRelation(life, true);
                case "friend" -> livingRelation(life, false);
                default -> null;
            };
            if (existing == null && !rule.getString("existing").isBlank()) return false;
            if (existing == null) {
                existing = new CompoundTag(); existing.putUUID("Id", UUID.randomUUID());
                existing.putString("Name", rule.getString("name")); existing.putString("NameKey", rule.getString("name_key"));
                if(List.of("female","male").contains(rule.getString("gender")))existing.putString("Gender",rule.getString("gender"));
                existing.putString("Relation", "acquaintance"); existing.putInt("Age", rule.getInt("age"));
                existing.putInt("BirthYear", life.getInt("Age") - rule.getInt("age"));
                boolean living = !List.of("memory", "remnant", "hazard").contains(rule.getString("kind"));
                int realm = definition.getString("id").equals("life_danger_oath_collection") && rule.getString("role").equals("lead")
                        && life.getCompound("SpiritualOath").contains("Rank") ? life.getCompound("SpiritualOath").getInt("Rank") : rule.getInt("realm");
                existing.putInt("Realm", realm); existing.putBoolean("Alive", living);
                existing.putBoolean("Available", false); existing.putBoolean("Observed", false); existing.putInt("Health", living ? 80 : 0);
                if (!living) existing.putString("DeathCause", "memory");
                existing.putString("Kind", rule.getString("kind")); existing.putString("Occupation", rule.getString("occupation").isBlank()?"sect_disciple":rule.getString("occupation"));
                if (rule.getBoolean("rootless")) { existing.putString("SpiritualRoot","none"); existing.putInt("SpiritualRootPotential",0); }
                if (definition.getBoolean("replacement")) existing.putBoolean("StoryNonCombatant",true);
                if(rule.contains("health"))existing.putInt("Health",Math.max(1,Math.min(100,rule.getInt("health"))));
                if(rule.getInt("injury_years")>0)existing.putInt("InjuredUntil",life.getInt("Age")+rule.getInt("injury_years"));
                if(rule.getBoolean("same_sect"))existing.putString("Sect",LifeAuthoredStories.sectIdentity(life));
                existing.putString("Personality", "steady");
                life.getList("Relationships", Tag.TAG_COMPOUND).add(existing);
            }
            cast.putUUID(rule.getString("role"), existing.getUUID("Id"));
        }
        story.put("Cast", cast); life.put("Story", story);
        LifeStoryLoot.prepare(life);
        recordStoryHistory(life, story);
        if (definition.getString("category").equals("danger")) life.putInt("LastMajorCrisis", life.getInt("Age"));
        openNode(life, definition.getString("start_node").isBlank()?"start":definition.getString("start_node")); return true;
    }

    private static void recordStoryHistory(CompoundTag life, CompoundTag story) {
        String id = story.getString("Id"), memory = "life.battle.history.fictional_trial", aspect = "fear";
        ListTag history = life.getList("History", Tag.TAG_COMPOUND);
        for (int index = history.size() - 1; index >= 0; index--) {
            CompoundTag record = history.getCompound(index); String key = record.getString("Key");
            if (key.contains("breakthrough_failure")) { memory = key; aspect = "fear"; story.putInt("MemoryYear", record.getInt("Age")); break; }
            if (key.contains("death") || key.contains("relationship")) { memory = key; aspect = "regret"; story.putInt("MemoryYear", record.getInt("Age")); break; }
        }
        if (memory.equals("life.battle.history.fictional_trial")) for (String completed : life.getCompound("StoryJournal").getAllKeys()) {
            CompoundTag record = life.getCompound("StoryJournal").getCompound(completed);
            if (!record.getString("TextKey").isBlank()) { memory = record.getString("TextKey"); aspect = "regret"; story.putInt("MemoryYear", record.getInt("Year")); break; }
        }
        if (id.equals("life_danger_inner_demon") && memory.equals("life.battle.history.fictional_trial") && life.getInt("TrainingStrain") >= 60) { memory = "life.battle.history.training_strain"; aspect = "hunger"; }
        story.putString("MemoryKey", memory); story.putString("MemoryAspect", aspect);
        if (id.equals("life_danger_friend_betrayal")) {
            CompoundTag cast = story.getCompound("Cast"), pressure = new CompoundTag();
            if (!cast.hasUUID("lead") || !cast.hasUUID("captor") || !cast.hasUUID("hostage")) return;
            pressure.putUUID("FriendId", cast.getUUID("lead")); pressure.putUUID("CaptorId", cast.getUUID("captor")); pressure.putUUID("HostageId", cast.getUUID("hostage"));
            pressure.putInt("EstablishedYear", life.getInt("Age")); pressure.putString("State", "captivity_claimed"); pressure.putString("ReasonKey", "life.battle.history.coerced_friend");
            for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                CompoundTag npc = (CompoundTag)tag;
                if (npc.hasUUID("Id") && npc.getUUID("Id").equals(cast.getUUID("lead"))) {
                    pressure.putInt("PreviousTrust", npc.getInt("Trust")); pressure.putInt("PreviousAffinity", npc.getInt("Affinity"));
                    npc.put("CoercionHistory", pressure.copy());
                }
                if (npc.hasUUID("Id") && npc.getUUID("Id").equals(cast.getUUID("hostage"))) { npc.putBoolean("Captive", true); npc.putUUID("ProtectedBy", cast.getUUID("lead")); }
            }
            story.put("Coercion", pressure);
        }
    }

    public static void observePerson(CompoundTag life, UUID id) {
        for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
            CompoundTag npc = (CompoundTag)tag;
            if (npc.hasUUID("Id") && npc.getUUID("Id").equals(id)) {
                npc.putBoolean("Observed", true); npc.putBoolean("Available", npc.getBoolean("Alive") && !npc.getBoolean("Captive"));
                if (!npc.contains("FirstObservedYear")) npc.putInt("FirstObservedYear", life.getInt("Age"));
            }
        }
    }

    private static void observeRole(CompoundTag life, String role) {
        CompoundTag cast = life.getCompound("Story").getCompound("Cast");
        if (cast.hasUUID(role)) observePerson(life, cast.getUUID(role));
    }

    public static void openNode(CompoundTag life, String id) {
        if (LifeAuthoredStories.active(life)) {
            if (!LifeAuthoredStories.conscious(life) && !id.equals("E4")) id="E4";
            for(var role:life.getCompound("Story").getCompound("Definition").getList("required_living_cast",Tag.TAG_STRING))
                if(!LifeAuthoredStories.matches(life,"cast_alive:"+role.getAsString()))id="E4";
            if(life.getCompound("Story").getString("Id").equals("life_event_rain_sword")
                    &&!LifeAuthoredStories.matches(life,"cast_alive:ferryman"))id="E4";
            LifeAuthoredStories.enter(life,node(life.getCompound("Story").getCompound("Definition"),id));
        }
        refreshNode(life, id);
        if (life.contains("Story")) LifeSimulationSystem.addLog(life, node(life.getCompound("Story").getCompound("Definition"), id).getString("text_key"));
    }

    private static void refreshNode(CompoundTag life, String id) {
        CompoundTag story = life.getCompound("Story"), definition = story.getCompound("Definition");
        CompoundTag node = node(definition, id);
        if (node.isEmpty()) { complete(life, "withdraw"); return; }
        for (var role : node.getList("seen_roles", Tag.TAG_STRING)) observeRole(life, role.getAsString());
        story.putString("Node", id); story.putInt("Revision", story.getInt("Revision") + 1);
        CompoundTag event = new CompoundTag(); event.putBoolean("Story", true);
        event.putString("Id", definition.getString("id") + ":" + id);
        event.putString("TitleKey", definition.getString("title_key")); event.putString("TextKey", node.getString("text_key"));
        event.putString("ObjectiveKey", node.getString("objective_key")); event.putString("LocationKey", definition.getString("location_key"));
        if (!node.getString("loot_kind").isBlank()) event.put("LootReward", LifeStoryLoot.offer(life, node.getString("loot_kind")).copy());
        if (definition.getBoolean("replacement")) event.put("TextParts",LifeAuthoredStories.textParts(life,node));
        ListTag choices = new ListTag();
        for (var tag : node.getList("choices", Tag.TAG_COMPOUND)) {
            CompoundTag choice = (CompoundTag) tag;
            if (!available(life, choice)) continue;
            CompoundTag visible = choice.copy();
            visible.putString("Id", story.getUUID("Instance") + "/" + story.getInt("Revision") + "/" + choice.getString("id"));
            visible.putString("TextKey", choice.getString("text_key"));
            visible.putString("Illegal", choiceIllegal(life, choice)); choices.add(visible);
        }
        event.put("Choices", choices); life.put("CurrentEvent", event);
    }

    private static boolean available(CompoundTag life, CompoundTag choice) {
        for (var tag : choice.getList("conditions", Tag.TAG_STRING)) if (!LifeSimulationSystem.matchesCondition(life, tag.getAsString())) return false;
        return true;
    }
    /** Shared UI/server validation; visible resource costs never fail as a silent no-op. */
    public static String choiceIllegal(CompoundTag life, CompoundTag choice) {
        if (!available(life, choice)) return "condition";
        if (!choice.getString("loot_kind").isBlank() && LifeStoryLoot.offer(life, choice.getString("loot_kind")).isEmpty()) return "condition";
        if (LifeAuthoredStories.active(life)) {
            String illegal=LifeAuthoredStories.illegal(life,choice);
            if(!illegal.isBlank())return illegal;
        }
        if (life.getLong("Wealth") < Math.max(0, choice.getLong("wealth_cost"))) return "wealth";
        String item = choice.getString("item_cost");
        if (!item.isBlank() && LifeSimulationBattle.itemCount(life, item) < 1) return "item";
        return "";
    }
    public static CompoundTag node(CompoundTag definition, String id) {
        for (var tag : definition.getList("nodes", Tag.TAG_COMPOUND)) if (((CompoundTag) tag).getString("id").equals(id)) return (CompoundTag) tag;
        return new CompoundTag();
    }

    public static void choose(CompoundTag life, String payload, RandomSource random) {
        if (!life.contains("Story", Tag.TAG_COMPOUND) || LifeSimulationBattle.active(life)) return;
        CompoundTag story = life.getCompound("Story");
        if (payload.equals("reveal")) {
            if (!story.contains("PendingRoll", Tag.TAG_COMPOUND)) return;
            CompoundTag roll = story.getCompound("PendingRoll");
            if (roll.getBoolean("Revealed")) return;
            roll.putBoolean("Revealed", true); roll.putBoolean("Applied", true);
            LifeSimulationDice.remember(life, roll);
            int degree = roll.getInt("Degree");
            CompoundTag choice = story.getCompound("PendingChoice");
            if (degree == 1 && !choice.getBoolean("no_strain")) {
                life.putInt("TrainingStrain", Math.min(100, life.getInt("TrainingStrain") + 6));
                story.putInt("Meter", story.getInt("Meter") + 1);
            } else if (degree == 0) story.putInt("Meter", story.getInt("Meter") + 2);
            story.remove("PendingRoll"); story.remove("PendingChoice");
            if (degree == 0) choice.putBoolean("prepare", false);
            if (choice.getBoolean("extended")) {
                story.putInt("Successes", story.getInt("Successes") + (degree >= 1 ? 1 : 0));
                story.putInt("Failures", story.getInt("Failures") + (degree == 0 ? 1 : 0));
                if (story.getInt("Successes") < 3 && story.getInt("Failures") < 2) {
                    openNode(life, story.getString("Node")); return;
                }
                applyChoice(life, choice, story.getInt("Successes") >= 3 ? choice.getString("next") : choice.getString("failure"), random);
            } else {
                if(LifeAuthoredStories.active(life))LifeAuthoredStories.effects(life,
                        choice.getList(degree>=1?"success_story_effects":"failure_story_effects",Tag.TAG_STRING));
                applyChoice(life, choice, degree >= 1 ? choice.getString("next") : choice.getString("failure"), random);
            }
            return;
        }
        String[] identity = payload.split("/", 3);
        if (identity.length != 3 || !identity[0].equals(story.getUUID("Instance").toString())
                || !identity[1].equals(Integer.toString(story.getInt("Revision"))) || story.contains("PendingRoll")) return;
        CompoundTag current = node(story.getCompound("Definition"), story.getString("Node"));
        CompoundTag selected = null;
        for (var tag : current.getList("choices", Tag.TAG_COMPOUND)) if (((CompoundTag) tag).getString("id").equals(identity[2])) selected = (CompoundTag) tag;
        if (selected == null || !choiceIllegal(life, selected).isBlank()) return;
        if (LifeAuthoredStories.active(life)) LifeAuthoredStories.pay(life,selected);
        String neededItem = selected.getString("item_cost");
        if (!neededItem.isBlank() && LifeSimulationBattle.itemCount(life, neededItem) < 1) return;
        life.putLong("Wealth", life.getLong("Wealth") - selected.getLong("wealth_cost"));
        if (!neededItem.isBlank()) for (var tag : life.getList("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) tag;
            if (neededItem.equals(item.getString("Id"))) { item.putInt("Count", item.getInt("Count") - 1); break; }
        }
        ListTag path = story.getList("Path", Tag.TAG_STRING); path.add(net.minecraft.nbt.StringTag.valueOf(story.getString("Node") + ":" + selected.getString("id"))); story.put("Path", path);
        if (selected.contains("check", Tag.TAG_COMPOUND)) {
            CompoundTag check = selected.getCompound("check");
            boolean futurePreparation = life.getBoolean("FuturePrepared");
            CompoundTag roll = LifeSimulationDice.check(random, check.getString("purpose"), LifeStoryChecks.modifier(life, check),
                    check.getInt("dc"), LifeStoryChecks.advantage(life), LifeStoryChecks.disadvantage(life));
            roll.putBoolean("FuturePreparation", futurePreparation);
            if (futurePreparation) life.putBoolean("FuturePrepared", false);
            story.put("PendingRoll", roll); story.put("PendingChoice", selected.copy());
            story.putInt("Revision", story.getInt("Revision") + 1); life.getCompound("CurrentEvent").put("Choices", new ListTag());
        } else applyChoice(life, selected, selected.getString("next"), random);
    }

    private static void applyChoice(CompoundTag life, CompoundTag choice, String next, RandomSource random) {
        CompoundTag story = life.getCompound("Story");
        if (LifeAuthoredStories.active(life)) {
            LifeAuthoredStories.effects(life,choice.getList("story_effects",Tag.TAG_STRING));
            if(life.getInt("Realm")>=4&&!choice.getString("high_realm_next").isBlank())next=choice.getString("high_realm_next");
        }
        for (var tag : choice.getList("effects", Tag.TAG_STRING)) effect(life, tag.getAsString());
        if (choice.getBoolean("prepare")) story.putBoolean("Prepared", true);
        if (choice.contains("battle", Tag.TAG_COMPOUND)) {
            CompoundTag definition = choice.getCompound("battle").copy(); definition.putString("story", story.getString("Id"));
            definition.putString("win", next); definition.putString("loss", choice.getString("failure"));
            if (story.getString("Id").equals("life_danger_oath_collection") && life.getCompound("SpiritualOath").contains("Rank"))
                for (var enemy : definition.getList("enemies", Tag.TAG_COMPOUND)) ((CompoundTag) enemy).putInt("realm", life.getCompound("SpiritualOath").getInt("Rank"));
            if (!definition.contains("objective_enabled")) definition.putBoolean("objective_enabled", List.of(
                    "life_event_rain_sword", "life_event_missing_disciple", "life_event_flooded_mine", "life_event_silent_elder",
                    "life_danger_tribulation", "life_danger_inner_demon", "life_danger_spirit_mist",
                    "life_danger_spirit_vein_collapse", "life_danger_pill_soul", "life_danger_dynasty_end").contains(story.getString("Id")));
            LifeSimulationBattle.start(life, definition, random); life.remove("CurrentEvent");
            return;
        }
        if (choice.getBoolean("ending")) complete(life, next);
        else openNode(life, next.isBlank() ? "withdraw" : next);
    }

    public static void finishBattle(CompoundTag life) {
        CompoundTag battle = life.getCompound(LifeSimulationBattle.ROOT);
        if (LifeSimulationBattle.pending(battle) || battle.getString("Result").isBlank()) return;
        CompoundTag player = LifeSimulationBattle.find(battle, "player");
        boolean cards=LifeCardBattle.enabled(battle);
        if(cards)LifeCardBattle.sync(life,battle);
        else life.putInt("Health", Math.max(1, (int) (100 * player.getDouble("Hp") / player.getDouble("MaxHp"))));
        String result = battle.getString("Result");
        boolean victory = result.equals("victory");
        if (victory && life.getCompound("Story").getCompound("Definition").getString("category").equals("danger")) life.putInt("DangersSurvived", life.getInt("DangersSurvived") + 1);
        if (!victory) life.putInt("TrainingStrain", Math.min(100, life.getInt("TrainingStrain") + 15));
        if (!cards && result.equals("defeat") && battle.getString("Stakes").equals("lethal")) life.putInt("Health", 0);
        if (battle.getInt("CiviliansHarmed") > 0) {
            for (String role : List.of("lead", "worker")) {
                CompoundTag cast = life.getCompound("Story").getCompound("Cast"); if (!cast.hasUUID(role)) continue;
                for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                    CompoundTag npc = (CompoundTag)tag;
                    if (npc.hasUUID("Id") && npc.getUUID("Id").equals(cast.getUUID(role)) && npc.getBoolean("Alive")) {
                        npc.putInt("Health", Math.max(1, npc.getInt("Health") - battle.getInt("CiviliansHarmed") * 10)); npc.putInt("InjuredUntil", life.getInt("Age") + 2);
                    }
                }
            }
            life.putInt("Reputation", life.getInt("Reputation") - Math.min(12, battle.getInt("CiviliansHarmed") * 2));
        }
        for (var actorTag : battle.getList("Actors", Tag.TAG_COMPOUND)) {
            CompoundTag actor = (CompoundTag) actorTag;
            if (!actor.hasUUID("NpcId") || actor.getString("Kind").equals("anchor")) continue;
            for (var npcTag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                CompoundTag npc = (CompoundTag) npcTag;
                if (!npc.hasUUID("Id") || !npc.getUUID("Id").equals(actor.getUUID("NpcId"))) continue;
                npc.put("LastEncounterTactics", battle.getList("PlayerActions", Tag.TAG_STRING).copy());
                npc.putInt("LastEncounterYear", life.getInt("Age"));
                if(cards&&!actor.getBoolean("Enemy"))continue;
                if (actor.getDouble("Hp") > 0) {
                    npc.putInt("Health", Math.max(1, (int)(100 * actor.getDouble("Hp") / actor.getDouble("MaxHp"))));
                    if (npc.getInt("Health") < 25) npc.putInt("InjuredUntil", life.getInt("Age") + 2);
                    continue;
                }
                if (battle.getString("Stakes").equals("lethal")) {
                    npc.putBoolean("Alive", false); npc.putBoolean("Available", false); npc.putInt("DeathYear", life.getInt("Age")); npc.putString("DeathCause", "battle");
                } else { npc.putInt("Health", 20); npc.putInt("InjuredUntil", life.getInt("Age") + 2); }
            }
        }
        String next = victory ? battle.getString("NextWin") : battle.getString("NextLoss");
        if (victory && battle.getString("VictoryBy").equals("objective") && !battle.getString("NextObjectiveWin").isBlank()) next = battle.getString("NextObjectiveWin");
        if (result.equals("escape")) next = battle.getString("NextEscape").isBlank() ? "withdraw" : battle.getString("NextEscape");
        if (result.equals("surrender")) next = battle.getString("NextSurrender").isBlank() ? "withdraw" : battle.getString("NextSurrender");
        if ((result.equals("escape") || result.equals("surrender")) && battle.getBoolean("AbandonedEnding")) life.getCompound("Story").putBoolean("Abandoned", true);
        if (battle.getString("Mechanic").equals("coerced_friend")) {
            CompoundTag pressure = battle.getCompound("Coercion");
            if (victory) pressure.putString("State", "hostage_released");
            life.getCompound("Story").put("Coercion", pressure.copy());
            for (var npcTag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                CompoundTag npc = (CompoundTag)npcTag;
                if (!npc.hasUUID("Id")) continue;
                if (pressure.hasUUID("FriendId") && npc.getUUID("Id").equals(pressure.getUUID("FriendId"))) npc.put("CoercionHistory", pressure.copy());
                if (victory && pressure.hasUUID("HostageId") && npc.getUUID("Id").equals(pressure.getUUID("HostageId"))) npc.putBoolean("Captive", false);
                if (pressure.hasUUID("CaptorId") && npc.getUUID("Id").equals(pressure.getUUID("CaptorId"))) {
                    npc.putBoolean("Nemesis", true); npc.putInt("Affinity", -50); npc.putString("Grievance", "life_danger_friend_betrayal");
                }
            }
        }
        if (LifeAuthoredStories.active(life)) {
            String effects= victory?"battle_win_effects":result.equals("escape")?"battle_escape_effects":"battle_loss_effects";
            LifeAuthoredStories.effects(life,life.getCompound("Story").getCompound("Definition").getList(effects,Tag.TAG_STRING));
            CompoundTag state=life.getCompound("Story").getCompound("State");
            state.putString("BattleResult",result);state.put("BattleActors",battle.getList("Actors",Tag.TAG_COMPOUND).copy());
            if(life.getBoolean("CombatDead") || !LifeAuthoredStories.conscious(life))next="E4";
        }
        life.remove(LifeSimulationBattle.ROOT); openNode(life, next.isBlank() ? "withdraw" : next);
        if(cards&&life.getBoolean("CombatDead")){life.putBoolean("Alive",false);life.putInt("Health",0);}
        if(cards&&!victory&&!life.getBoolean("CombatDead")&&life.getInt("Health")<=0)life.putInt("Health",1);
        if (life.getInt("Health") <= 0) complete(life, next.isBlank() ? "withdraw" : next);
    }

    public static void effect(CompoundTag life, String raw) {
        if (raw.startsWith("storyworld:")) {
            LifeSimulationWorldMap.applyStoryEffect(life, life.getCompound("Story").getString("SourceLocation"), raw.substring(11));
            return;
        }
        if (raw.startsWith("oath:")) {
            CompoundTag oath = new CompoundTag(); oath.putBoolean("Accepted", true);
            oath.putString("TermsKey", "life.expansion.oath.terms"); oath.putInt("Deadline", life.getInt("Age") + Integer.parseInt(raw.substring(5)));
            oath.putInt("Rank", life.getInt("Realm")); oath.putString("SourceStory", life.getCompound("Story").getString("Id"));
            life.put("SpiritualOath", oath); return;
        }
        if (raw.equals("breakthrough")) {
            int next = Math.min(27, life.getInt("Realm") + 1);
            life.putInt("Realm", next); life.putInt("HighestRealm", Math.max(next, life.getInt("HighestRealm")));
            life.putInt("MaximumLifespan", Math.max(life.getInt("MaximumLifespan"), com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(
                    com.example.immortal_cultivation_mod.attachment.CultivationLevels.allLevels().get(next)).maxAge()));
            life.putLong("CultivationProgress", 0); return;
        }
        if (raw.startsWith("strain")) { life.putInt("TrainingStrain", Mth.clamp(life.getInt("TrainingStrain") + Integer.parseInt(raw.substring(6)), 0, 100)); return; }
        if (raw.startsWith("clear:")) {
            life.putBoolean(raw.substring(6), false);
            if (raw.equals("clear:OathBroken")) {
                life.getCompound("SpiritualOath").putBoolean("Broken", false);
                life.getCompound("SpiritualOath").putBoolean("Fulfilled", true);
            }
            return;
        }
        if (raw.startsWith("state:")) { life.putBoolean(raw.substring(6), true); return; }
        if (raw.startsWith("grief:")) {
            CompoundTag cast = life.getCompound("Story").getCompound("Cast"); String role = raw.substring(6);
            if (!cast.hasUUID(role)) return;
            for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                CompoundTag npc = (CompoundTag) tag;
                if (!npc.hasUUID("Id") || !npc.getUUID("Id").equals(cast.getUUID(role)) || npc.getBoolean("GriefApplied")) continue;
                int grief = npc.getInt("Affinity") > 0 ? Math.min(8, (npc.getInt("Affinity") + Math.max(0, npc.getInt("Trust"))) / 25) : 0;
                life.putInt("Mentality", Math.max(0, life.getInt("Mentality") - grief)); npc.putBoolean("GriefApplied", true);
            }
            return;
        }
        if (raw.startsWith("relation:")) {
            String[] p = raw.split(":", 4); if (p.length < 3) return;
            CompoundTag cast = life.getCompound("Story").getCompound("Cast"); if (!cast.hasUUID(p[1])) return;
            observeRole(life, p[1]);
            for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                CompoundTag npc = (CompoundTag) tag;
                if (!npc.hasUUID("Id") || !npc.getUUID("Id").equals(cast.getUUID(p[1])) || !npc.getBoolean("Alive")) continue;
                if (p[2].equals("kill")) { npc.putBoolean("Alive", false); npc.putBoolean("Available", false); npc.putInt("DeathYear", life.getInt("Age")); npc.putString("DeathCause", "story"); }
                else if (p[2].equals("enemy")) { npc.putBoolean("Nemesis", true); npc.putInt("Affinity", -50); npc.putString("Grievance", life.getCompound("Story").getString("Id")); }
                else if (p[2].equals("neutral")) { npc.putBoolean("Friend", false); npc.putBoolean("Protector", false); npc.putBoolean("Nemesis", false); npc.putInt("Affinity", 0); }
                else {
                    boolean reconciliation = life.getCompound("Story").getString("Id").equals("life_danger_friend_betrayal");
                    npc.putBoolean("Friend", true); npc.putInt("Affinity", Math.min(100, npc.getInt("Affinity") + (reconciliation ? 10 : 45)));
                    npc.putInt("Trust", Math.min(100, npc.getInt("Trust") + (reconciliation ? 5 : 45)));
                }
            }
            return;
        }
        if (raw.startsWith("continuation:")) {
            life.putBoolean("MethodContinuation", true); life.putInt("MethodContinuationLimit", Math.min(27, Integer.parseInt(raw.substring(13)))); return;
        }
        LifeSimulationSystem.applyEffect(life, raw);
    }

    public static void complete(CompoundTag life, String ending) {
        CompoundTag story = life.getCompound("Story"); if (story.isEmpty()) return;
        String id = story.getString("Id"); CompoundTag journal = life.getCompound("StoryJournal");
        boolean retry = id.equals("life_danger_tribulation") && !journal.getCompound(id).getString("Ending").equals("a");
        if (!journal.contains(id) || story.getCompound("Definition").getBoolean("repeat") && (retry || journal.getCompound(id).getInt("Realm") < life.getInt("Realm"))) {
            CompoundTag record = new CompoundTag(); record.putString("Ending", ending);
            record.put("Path", story.getList("Path", Tag.TAG_STRING).copy()); record.put("Cast", story.getCompound("Cast").copy());
            record.putInt("Year", life.getInt("Age")); record.putInt("Realm", life.getInt("Realm")); record.putBoolean("Rewarded", !ending.equals("withdraw") && !story.getBoolean("Abandoned"));
            if (story.getCompound("Definition").getBoolean("replacement")) {
                record.put("State",story.getCompound("State").copy());
                record.put("TextParts",LifeAuthoredStories.textParts(life,node(story.getCompound("Definition"),ending)));
                record.putString("TitleKey",story.getCompound("Definition").getString("title_key"));
                for(var npcTag:life.getList("Relationships",Tag.TAG_COMPOUND)) {
                    CompoundTag npc=(CompoundTag)npcTag;
                    if(!npc.hasUUID("Id"))continue;
                    for(String role:story.getCompound("Cast").getAllKeys())if(story.getCompound("Cast").hasUUID(role)&&story.getCompound("Cast").getUUID(role).equals(npc.getUUID("Id"))) {
                        CompoundTag history=npc.getCompound("AuthoredStoryHistory");history.put(id,story.getCompound("State").copy());npc.put("AuthoredStoryHistory",history);
                    }
                }
            }
            record.putString("SourceLocation", story.getString("SourceLocation")); record.putBoolean("Abandoned", story.getBoolean("Abandoned"));
            CompoundTag end = node(story.getCompound("Definition"), ending);
            for (var tag : end.getList("effects", Tag.TAG_STRING)) effect(life, tag.getAsString());
            if (story.contains("LootReceipt")) record.put("LootReceipt", story.getCompound("LootReceipt").copy());
            record.putString("TextKey", end.getString("text_key")); journal.put(id, record); life.put("StoryJournal", journal);
            ListTag delayed = life.getList("DelayedStories", Tag.TAG_COMPOUND); CompoundTag follow = new CompoundTag();
            follow.putInt("Year", life.getInt("Age") + 3); follow.putString("TextKey", end.getString("followup_key"));
            follow.put("Cast", story.getCompound("Cast").copy()); delayed.add(follow); life.put("DelayedStories", delayed);
        }
        if (!ending.equals("withdraw") && !story.getBoolean("Abandoned")) LifeSimulationBalance.rewardEncounter(life);
        life.remove("Story"); life.remove("CurrentEvent");
    }

    public static void advanceYear(CompoundTag life) {
        LifeAuthoredStories.settlePendingFees(life);
        CompoundTag oath = life.getCompound("SpiritualOath");
        if (oath.getBoolean("Accepted") && !oath.getBoolean("Fulfilled") && !oath.getBoolean("Broken") && life.getInt("Age") > oath.getInt("Deadline")) {
            oath.putBoolean("Broken", true); LifeSimulationSystem.addLog(life, "life.expansion.oath.broken");
        }
        RandomSource world = RandomSource.create(life.getLong("Seed") ^ life.getInt("Age") * 93187L);
        if (!"none".equals(life.getString("Sect")) && life.getInt("Reputation") < -15 && life.getInt("Age") >= 20 && !life.getBoolean("RegionalWar") && world.nextInt(25) == 0) {
            life.putBoolean("RegionalWar", true); LifeSimulationSystem.addLog(life, "life.expansion.warning.war");
        }
        if (life.getInt("Realm") >= 4 && life.getInt("Age") % 2 == 0 && !life.getBoolean("NaturalVeinWarning") && world.nextInt(600) == 0) {
            life.putBoolean("NaturalVeinWarning", true); LifeSimulationSystem.addLog(life, "life.expansion.warning.vein");
        }
        ListTag delayed = life.getList("DelayedStories", Tag.TAG_COMPOUND);
        for (var tag : delayed) {
            CompoundTag follow = (CompoundTag) tag;
            if (!follow.getBoolean("Applied") && life.getInt("Age") >= follow.getInt("Year")) {
                follow.putBoolean("Applied", true);
                boolean dead = false;
                CompoundTag cast = follow.getCompound("Cast");
                for (String role : cast.getAllKeys()) if (cast.hasUUID(role) && life.getCompound("NpcMemorials").contains(cast.getUUID(role).toString())) {
                    CompoundTag memorial = life.getCompound("NpcMemorials").getCompound(cast.getUUID(role).toString());
                    if (!List.of("memory", "remnant", "hazard").contains(memorial.getString("Kind"))) dead = true;
                }
                for (var person : life.getList("Relationships", Tag.TAG_COMPOUND)) {
                    CompoundTag npc = (CompoundTag) person;
                    if (npc.getBoolean("Alive") || !npc.hasUUID("Id") || List.of("memory", "remnant", "hazard").contains(npc.getString("Kind"))) continue;
                    for (String role : cast.getAllKeys()) if (cast.hasUUID(role) && cast.getUUID(role).equals(npc.getUUID("Id"))) dead = true;
                }
                if (!follow.getString("TextKey").isBlank()) LifeSimulationSystem.addLog(life, dead ? "life.expansion.memorial_followup" : follow.getString("TextKey"));
            }
        }
        // Expired cast references become memorials; never regenerate their original living identity.
    }
}
