package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative state machine for the Life Simulation System. */
public final class LifeSimulationSystem {
    private static final String ROOT = "LifeSimulation";
    private static final String ATTEMPTS = "Attempts";
    private static final String LAST_ATTEMPT_DAY = "LastAttemptDay";
    private static final String INSIGHT = "Insight";
    private static final String HISTORY = "History";
    private static final String RELATIONSHIPS = "Relationships";
    private static final String FLAGS = "Flags";
    private static final String UPGRADES = "Upgrades";
    private static final String CURRENT_EVENT = "CurrentEvent";
    private static final String REWARDS = "InheritanceRewards";
    private static final String ENDINGS = "EndingCollection";
    public static final int MAX_ATTEMPTS = 3;
    private static final int ATTEMPT_INTERVAL_DAYS = 4;
    private static final int MAX_HISTORY = 240;
    private static final int YEARLY_ACTIONS = 3;
    private static final int INHERITED_SPELL_PROFICIENCY = 100;
    private static final String SIMULATION_INVENTORY = "Inventory";
    private static final String DISCOVERED_SPELLS = "DiscoveredSpells";
    private static final String LEARNED_METHODS = "LearnedMethods";
    private static final String ACTIVE_METHOD = "ActiveMethod";
    private static final String QUEST_PROGRESS = "QuestProgress";
    private static final String LAST_EXPLORE_QUEST = "LastExploreQuest";
    private static final String LAST_SECT_QUEST = "LastSectQuest";

    private static final String[] SURNAMES = {
            "林", "沈", "顾", "陆", "苏", "江", "叶", "楚", "白", "萧",
            "韩", "秦", "宁", "谢", "温", "柳"
    };
    private static final String[] MALE_NAMES = {
            "云川", "清玄", "长风", "景行", "知远", "怀瑾", "星河", "明渊"
    };
    private static final String[] FEMALE_NAMES = {
            "清月", "若兰", "青璃", "灵犀", "疏影", "映雪", "云舒", "婉宁"
    };
    private static final String[] BIRTHPLACES = {
            "greenstone_village", "cloudriver_town", "northwild_farm",
            "redleaf_city", "whitewater_hamlet", "mistmountain_pass"
    };
    private static final String[] FAMILIES = {
            "poor_herbalists", "tenant_farmers", "village_smiths",
            "small_merchants", "fallen_clan", "wandering_healers"
    };

    private LifeSimulationSystem() {
    }

    public static int attempts(ServerPlayer player) {
        ensureDay(player);
        return read(player).getInt(ATTEMPTS);
    }

    public static int setAttempts(ServerPlayer player, int amount) {
        CompoundTag life = read(player);
        int clamped = Mth.clamp(amount, 0, MAX_ATTEMPTS);
        life.putInt(ATTEMPTS, clamped);
        life.putLong(LAST_ATTEMPT_DAY, currentDay(player));
        save(player, life);
        return clamped;
    }

    public static int adjustAttempts(ServerPlayer player, int delta) {
        return setAttempts(player, attempts(player) + delta);
    }

    public static void ensureDay(ServerPlayer player) {
        CompoundTag outer = OuterSystemData.read(player);
        CompoundTag life = outer.getCompound(ROOT).copy();
        long day = currentDay(player);
        boolean changed = false;
        if (life.getBoolean("Active") && !life.getString("Name").isBlank()
                && !life.hasUUID("SimulationId")) {
            // Existing saves predate annual request tokens. Persist the identity before
            // sending it to the editor, rather than inventing a different token per read.
            life.putUUID("SimulationId", UUID.randomUUID());
            changed = true;
        }
        if (!life.contains(LAST_ATTEMPT_DAY)) {
            life.putLong(LAST_ATTEMPT_DAY, day);
            life.putInt(ATTEMPTS, Math.max(1, life.getInt(ATTEMPTS)));
            changed = true;
        } else {
            long previous = life.getLong(LAST_ATTEMPT_DAY);
            if (day < previous) {
                life.putLong(LAST_ATTEMPT_DAY, day);
                changed = true;
            } else if (day - previous >= ATTEMPT_INTERVAL_DAYS) {
                long grants = (day - previous) / ATTEMPT_INTERVAL_DAYS;
                life.putInt(
                        ATTEMPTS,
                        Math.min(MAX_ATTEMPTS, life.getInt(ATTEMPTS)
                                + (int) Math.min(MAX_ATTEMPTS, grants)));
                life.putLong(
                        LAST_ATTEMPT_DAY,
                        previous + grants * ATTEMPT_INTERVAL_DAYS);
                changed = true;
            }
        }
        int attempts = Mth.clamp(life.getInt(ATTEMPTS), 0, MAX_ATTEMPTS);
        if (attempts != life.getInt(ATTEMPTS)) {
            life.putInt(ATTEMPTS, attempts);
            changed = true;
        }
        if (changed) {
            outer.put(ROOT, life);
            OuterSystemData.save(player, outer);
        }
    }

    public static void handleAction(ServerPlayer player, String rawAction) {
        if (!isActiveSystem(player) || rawAction == null) {
            return;
        }
        String action = rawAction.trim().toLowerCase(Locale.ROOT);
        if (action.equals("start")) {
            startLife(player);
        } else if (action.equals("reroll")) {
            rerollBirth(player);
        } else if (action.equals("advance")) {
            advanceYears(player, 1);
        } else if (action.equals("advance_5")) {
            advanceYears(player, 5);
        } else if (action.equals("settle")) {
            settleLife(player);
        } else if (action.startsWith("battle:")) {
            CompoundTag life = read(player);
            if (life.getBoolean("Finished") || !life.getBoolean("Alive") && !(life.getBoolean("CombatDead") && LifeSimulationBattle.active(life))) return;
            if (action.substring(7).equals("continue")) LifeSimulationStories.finishBattle(life);
            else LifeSimulationBattle.handle(life, action.substring(7), player.getRandom());
            if (life.getInt("Health") <= 0 && !LifeSimulationBattle.active(life)) finishLife(player, life, life.getBoolean("CombatDead") ? "third_knockout" : "event_death");
            save(player, life);
        } else if (action.startsWith("story:")) {
            CompoundTag life = read(player);
            if (!life.getBoolean("Alive") || life.getBoolean("Finished")) return;
            LifeSimulationStories.choose(life, action.substring(6), player.getRandom());
            if (life.getInt("Health") <= 0) finishLife(player, life, "event_death");
            save(player, life);
        } else if (action.startsWith("choice:")) {
            // Authored identifiers are opaque and case-sensitive. Normalize the
            // command name above, but never rewrite a saved choice's identity.
            chooseEvent(player, rawAction.trim().substring("choice:".length()));
        } else if (action.startsWith("activity:")) {
            performActivity(player, action.substring("activity:".length()));
        } else if (action.startsWith("routine:")) {
            routineAction(player, action.substring(8));
        } else if (action.startsWith("relationship:")) {
            relationshipAction(player, action.substring("relationship:".length()));
        } else if (action.startsWith("inherit_select:")) {
            selectInheritance(player, action.substring("inherit_select:".length()));
        } else if (action.startsWith("inherit:")) {
            claimInheritance(player, action.substring("inherit:".length()));
        } else if (action.startsWith("upgrade:")) {
            buyUpgrade(player, action.substring("upgrade:".length()));
        } else if (action.startsWith("map:")) {
            CompoundTag life = read(player);
            if (life.getBoolean("Active")
                    && life.getBoolean("Alive")
                    && !life.getBoolean("Finished")
                    && !LifeSimulationBattle.active(life) && !life.contains("Story")
                    && LifeSimulationWorldMap.handleAction(
                            player,
                            life,
                            action.substring("map:".length()),
                            yearlyRandom(life))) {
                save(player, life);
            }
        }
    }

    public static void writeClientState(
            ServerPlayer player, CompoundTag result) {
        ensureDay(player);
        CompoundTag life = read(player);
        CompoundTag client = life.copy();
        filterObservedRelationships(client);
        for (var tag : client.getList("Memories", Tag.TAG_COMPOUND)) filterObservedRelationships((CompoundTag)tag);
        client.remove("Seed");
        client.getCompound("Story").remove("Definition");
        // Send the entire bounded history, including settled lives.
        ListTag rewards = client.getList(REWARDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < rewards.size(); i++) {
            CompoundTag reward = rewards.getCompound(i);
            reward.put("Preview", inheritancePreview(player, life, reward));
            ListTag options = LifeSimulationInheritance.options(life, reward.getString("Id"));
            reward.putInt("ChoiceCount", options.size());
            reward.putInt("ChoiceIndex", LifeSimulationInheritance.optionIndex(options, reward));
            for (var option : options) {
                CompoundTag choice = (CompoundTag) option;
                choice.put("Preview", inheritancePreview(player, life, choice));
            }
            reward.put("Choices", options);
        }
        client.putLong(
                "CultivationProgressRequired",
                cultivationRequired(life.getInt("Realm")));
        CultivationLevels.LevelDef simulatedLevel = cultivationLevel(life.getInt("Realm"));
        client.putInt("RealmMaxHp", simulatedLevel.maxHp());
        client.putInt("RealmMaxQi", simulatedLevel.maxQi());
        client.putInt("RealmMaxAge", simulatedLevel.maxAge());
        client.putLong(
                "DaysUntilAttempt",
                Math.max(0L, ATTEMPT_INTERVAL_DAYS
                        - (currentDay(player) - life.getLong(LAST_ATTEMPT_DAY))));
        result.put("LifeSimulation", client);
    }

    private static void filterObservedRelationships(CompoundTag client) {
        ListTag people = client.getList(RELATIONSHIPS, Tag.TAG_COMPOUND), visible = new ListTag();
        for (int index = 0; index < people.size(); index++) {
            CompoundTag person = people.getCompound(index);
            if (person.contains("Observed") && !person.getBoolean("Observed")) continue;
            CompoundTag entry = person.copy();
            entry.putInt("ServerIndex", index);
            visible.add(entry);
        }
        client.put(RELATIONSHIPS, visible);
    }

    private static void startLife(ServerPlayer player) {
        ensureDay(player);
        CompoundTag previous = read(player);
        if (previous.getInt(ATTEMPTS) <= 0
                || previous.getBoolean("Active")) {
            return;
        }
        int attempts = previous.getInt(ATTEMPTS) - 1;
        long seed = player.getRandom().nextLong()
                ^ player.getUUID().getMostSignificantBits()
                ^ player.serverLevel().getGameTime();
        CompoundTag life = generateBirth(previous, seed);
        life.putInt(ATTEMPTS, attempts);
        save(player, life);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "life_started");
    }

    private static void rerollBirth(ServerPlayer player) {
        CompoundTag current = read(player);
        if (!current.getBoolean("Active")
                || !current.getBoolean("Alive")
                || current.getInt("Age") != 0
                || current.getBoolean("BirthRerolled")
                || current.getInt(INSIGHT) < 30) {
            return;
        }
        int insight = current.getInt(INSIGHT) - 30;
        int attempts = current.getInt(ATTEMPTS);
        long seed = current.getLong("Seed")
                ^ 0x6A09E667F3BCC909L
                ^ player.serverLevel().getGameTime();
        CompoundTag rerolled = generateBirth(current, seed);
        rerolled.putInt(INSIGHT, insight);
        rerolled.putInt(ATTEMPTS, attempts);
        rerolled.putBoolean("BirthRerolled", true);
        save(player, rerolled);
    }

    private static void settleLife(ServerPlayer player) {
        CompoundTag life = read(player);
        if (!life.contains("Name", Tag.TAG_STRING)
                || !life.getBoolean("Active")
                || !life.getBoolean("Alive")
                || life.getBoolean("Finished")) {
            return;
        }
        if (LifeSimulationBattle.active(life) || life.getCompound("Story").contains("PendingRoll")) return;
        if (life.contains("Story")) LifeSimulationStories.complete(life, "withdraw");
        finishLife(player, life, "manual_settlement");
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "life_settled");
        CompoundTag map = life.getCompound(LifeSimulationWorldMap.ROOT);
        map.remove(LifeSimulationWorldMap.ACTIVE_JOURNEY);
        map.remove(LifeSimulationWorldMap.PENDING_ARRIVAL);
        life.put(LifeSimulationWorldMap.ROOT, map);
        save(player, life);
    }

    private static CompoundTag generateBirth(
            CompoundTag previous, long seed) {
        CompoundTag life = copyMeta(previous);
        RandomSource random = RandomSource.create(seed);
        String gender = random.nextBoolean() ? "male" : "female";
        String surname = pick(random, SURNAMES);
        String givenName = pick(
                random, "male".equals(gender) ? MALE_NAMES : FEMALE_NAMES);
        int health = attribute(random);
        int mentality = attribute(random);
        int comprehension = attribute(random);
        int charisma = attribute(random);
        int luck = attribute(random);
        String family = pick(random, FAMILIES);
        long wealth = switch (family) {
            case "small_merchants" -> random.nextInt(280, 620);
            case "fallen_clan" -> random.nextInt(120, 360);
            case "poor_herbalists", "tenant_farmers" -> random.nextInt(8, 70);
            default -> random.nextInt(50, 210);
        };
        if (hasUpgrade(life, "previous_luck")) {
            comprehension = Math.min(100, comprehension + 10);
        }
        if (hasUpgrade(life, "family_blessing")) {
            wealth = Math.max(wealth, 300L);
        }

        life.putBoolean("Active", true);
        life.putBoolean("Alive", true);
        life.putBoolean("Finished", false);
        life.putBoolean("RewardClaimed", false);
        life.putBoolean("BirthRerolled", false);
        life.putUUID("SimulationId", UUID.randomUUID());
        life.putLong("Seed", seed);
        life.putString("Name", surname + givenName);
        life.putString("Gender", gender);
        life.putString("Birthplace", pick(random, BIRTHPLACES));
        life.putString("Family", family);
        life.putString("Father", surname + pick(random, MALE_NAMES));
        life.putString("Mother", pick(random, SURNAMES) + pick(random, FEMALE_NAMES));
        life.putBoolean("HasSibling", random.nextFloat() < 0.58F);
        if (life.getBoolean("HasSibling")) {
            String siblingGender=random.nextBoolean()?"male":"female";
            life.putString("SiblingGender",siblingGender);
            life.putString("Sibling",surname+pick(random,siblingGender.equals("male")?MALE_NAMES:FEMALE_NAMES));
        }
        life.putInt("Age", 0);
        life.putInt("Health", health);
        life.putInt("Mentality", mentality);
        life.putInt("Comprehension", comprehension);
        life.putInt("Charisma", charisma);
        life.putInt("Luck", luck);
        life.putBoolean("LuckRevealed", false);
        life.putInt("Morality", 0);
        life.putInt("Reputation", 0);
        life.putLong("Wealth", wealth);
        life.putInt("Realm", 0);
        life.putInt("HighestRealm", 0);
        life.putLong("CultivationProgress", 0L);
        life.putBoolean("UsesExactProgress", true);
        life.putInt("CultivationStability", 50);
        life.putInt("Combat", 5);
        life.putInt("Medical", family.equals("wandering_healers") ? 25 : 5);
        life.putInt("Alchemy", family.equals("poor_herbalists") ? 15 : 0);
        life.putString(ACTIVE_METHOD, "");
        life.put(DISCOVERED_SPELLS, new ListTag());
        life.put(LEARNED_METHODS, new ListTag());
        life.put(SIMULATION_INVENTORY, new ListTag());
        seedFamilyInventory(life, family, random);
        int rootPotential = random.nextInt(101);
        if (hasUpgrade(life, "fate_retention")) {
            rootPotential = Math.max(
                    rootPotential, life.getInt("RetainedRootPotential"));
        }
        life.putInt("SpiritualRootPotential", rootPotential);
        life.putString("SpiritualRoot", "untested");
        life.putInt("MaximumLifespan", 52 + health / 2 + random.nextInt(8, 24));
        life.putInt("AnnualActionPoints", YEARLY_ACTIONS);
        life.putString("Occupation", "child");
        life.putString("Sect", "none");
        life.put(HISTORY, new ListTag());
        life.put(RELATIONSHIPS, initialRelationships(life, random));
        life.put(FLAGS, new ListTag());
        life.put(QUEST_PROGRESS, new CompoundTag());
        life.remove(CURRENT_EVENT);
        life.remove(REWARDS);
        LifeSimulationWorldMap.initialize(life);
        addLog(life, "life.immortal_cultivation_mod.log.birth");
        normalizeLifeState(life);
        return life;
    }

    private static CompoundTag copyMeta(CompoundTag source) {
        CompoundTag result = new CompoundTag();
        ListTag memories = source.getList("Memories", Tag.TAG_COMPOUND).copy();
        if (source.getBoolean("Finished") && source.hasUUID("SimulationId")) {
            CompoundTag memory = new CompoundTag();
            for (String key : new String[]{"SimulationId", "Name", "Age", "HighestRealm",
                    "Ending", "Score", HISTORY, "RewardClaimed", "InheritanceReceipt", "StoryJournal", "DiceHistory", "Relationships", "NpcMemorials"}) {
                if (source.contains(key)) {
                    memory.put(key, source.get(key).copy());
                }
            }
            memories.add(memory);
            while (memories.size() > 5) {
                memories.remove(0);
            }
        }
        result.put("Memories", memories);
        result.putInt(ATTEMPTS, source.getInt(ATTEMPTS));
        result.putLong(LAST_ATTEMPT_DAY, source.getLong(LAST_ATTEMPT_DAY));
        result.putInt(INSIGHT, source.getInt(INSIGHT));
        result.put(UPGRADES, source.getList(UPGRADES, Tag.TAG_STRING).copy());
        result.put(ENDINGS, source.getList(ENDINGS, Tag.TAG_STRING).copy());
        result.putInt(
                "RetainedRootPotential",
                source.getInt("RetainedRootPotential"));
        return result;
    }

    private static ListTag initialRelationships(
            CompoundTag life, RandomSource random) {
        ListTag list = new ListTag();
        list.add(person(
                life.getString("Father"),
                "father",
                random.nextInt(22, 39),
                random.nextInt(70, 96),
                "steady",
                "family_trade", "male"));
        list.add(person(
                life.getString("Mother"),
                "mother",
                random.nextInt(20, 37),
                random.nextInt(78, 100),
                "kind",
                "family_trade", "female"));
        if (life.getBoolean("HasSibling")) {
            list.add(person(
                    life.getString("Sibling"),
                    "sibling",
                    0,
                    random.nextInt(58, 90),
                    random.nextBoolean() ? "cheerful" : "proud",
                    "child", life.getString("SiblingGender")));
        }
        return list;
    }

    private static CompoundTag person(
            String name,
            String relation,
            int age,
            int affinity,
            String personality,
            String occupation, String gender) {
        CompoundTag person = new CompoundTag();
        person.putUUID("Id", UUID.randomUUID());
        person.putString("Name", name);
        person.putString("Relation", relation);
        person.putString("Gender",gender);
        person.putInt("Age", age);
        person.putInt("Health", 65 + Math.floorMod(name.hashCode(), 31));
        person.putInt("Affinity", affinity);
        person.putString("Personality", personality);
        person.putString("Occupation", occupation);
        person.putInt("Realm", 0);
        person.putBoolean("Alive", true);
        return person;
    }

    private static void advanceYears(ServerPlayer player, int requestedYears) {
        CompoundTag life = read(player);
        if (!canAdvance(life)) {
            return;
        }
        int years = requestedYears <= 1 ? 1 : 5;
        if (years > 1 && !hasUpgrade(life, "continuous_memory")) {
            return;
        }
        for (int i = 0; i < years && canAdvance(life); i++) {
            if (advanceOneYear(player, life)) {
                break;
            }
        }
        save(player, life);
    }

    /** Returns true when fast-forwarding should stop. */
    private static boolean advanceOneYear(
            ServerPlayer player, CompoundTag life) {
        int age = life.getInt("Age") + 1;
        life.putInt("Age", age);
        LifeCardInjuries.advanceYear(life);
        life.putInt("AnnualActionPoints", YEARLY_ACTIONS);
        life.putBoolean("SeclusionThisYear", false);
        life.putInt("RoutineCursor", 0);
        if (life.getBoolean("FutureActionDebt")) {
            life.putInt("AnnualActionPoints", YEARLY_ACTIONS - 1);
            life.putBoolean("FutureActionDebt", false);
        }
        life.putInt("TrainingStrain", Math.max(0, life.getInt("TrainingStrain") - 3));
        RandomSource random = yearlyRandom(life);
        LifeSimulationStories.advanceYear(life);
        LifeSimulationWorldMap.advanceYear(life, random);

        applyIncomeAndExpenses(life, random);
        boolean importantRelationshipChange = updateRelationships(life, random);
        applyAging(life, random);
        applyPassiveCultivation(life, random);

        if (LifeSimulationWorldMap.hasActiveJourney(life)) {
            LifeSimulationWorldMap.progressJourney(life);
            return true;
        }

        if (life.getInt("Health") <= 0
                || age > life.getInt("MaximumLifespan")) {
            finishLife(player, life, "natural_death");
            return true;
        }
        if (life.getInt("Mentality") <= 0) life.putInt("TrainingStrain", Math.min(100, life.getInt("TrainingStrain") + 8));
        if (!hasFlag(life, "root_tested") && age >= 10) {
            setRootTestEvent(life);
            return true;
        }
        if (isReadyForBreakthrough(life)) {
            setBreakthroughEvent(life);
            return true;
        }

        if (age % 2 == 0 && random.nextFloat() < LifeSimulationTuning.get("crisis_chance")
                && LifeSimulationStories.select(life, player.server.getResourceManager(), "danger", random)) return true;
        if (random.nextFloat() < 0.52F) {
            if (LifeSimulationStories.select(life, player.server.getResourceManager(), "adventure", random)) return true;
            LifeSimulationEvents.Definition event = LifeSimulationEvents.select(
                    player.server.getResourceManager(), life, random);
            if (event != null) {
                boolean requiresChoice = applyEvent(life, event);
                if (requiresChoice) {
                    return true;
                }
            } else {
                addLog(life, "life.immortal_cultivation_mod.log.quiet_year");
            }
        } else {
            addLog(life, "life.immortal_cultivation_mod.log.quiet_year");
        }
        return importantRelationshipChange;
    }

    private static void applyIncomeAndExpenses(
            CompoundTag life, RandomSource random) {
        long income = switch (life.getString("Occupation")) {
            case "herbalist" -> 70L;
            case "merchant" -> 130L;
            case "physician" -> 110L;
            case "smith" -> 95L;
            case "sect_disciple" -> 55L;
            case "sect_elder" -> 280L;
            default -> life.getInt("Age") >= 16 ? 25L : 0L;
        };
        long expenses = life.getInt("Age") >= 16 ? 20L + random.nextInt(20) : 0L;
        life.putLong("Wealth", Math.max(0L, life.getLong("Wealth") + income - expenses));
    }

    private static boolean updateRelationships(
            CompoundTag life, RandomSource random) {
        ListTag relationships = life.getList(RELATIONSHIPS, Tag.TAG_COMPOUND);
        boolean important = false;
        int yearlyGrief = 0;
        for (int i = 0; i < relationships.size(); i++) {
            CompoundTag person = relationships.getCompound(i);
            if (!person.getBoolean("Alive")) {
                continue;
            }
            person.putInt("Age", person.getInt("Age") + 1);
            if (person.getInt("Age") >= 16 && person.getString("Occupation").equals("child")) person.putString("Occupation", "laborer");
            if (person.getInt("Age") >= 18 && person.getString("Occupation").equals("sect_disciple") && person.getInt("Realm") == 0) person.putInt("Realm", 1);
            if (person.getInt("Realm") > 0 && person.getBoolean("Alive")) {
                if (person.getString("ActiveMethod").isBlank()) {
                    var method = CultivationMethods.allMethods().stream()
                            .filter(m -> CultivationLevels.getStageIndex(m.limitLevel()) >= person.getInt("Realm"))
                            .min(java.util.Comparator.comparingInt(m -> CultivationLevels.getStageIndex(m.limitLevel())));
                    method.ifPresent(m -> person.putString("ActiveMethod", m.id()));
                    person.putInt("SpiritualRootPotential", 50); person.putInt("Comprehension", 50); person.putString("Sect", "none");
                }
                LifeSimulationBalance.train(person, .35);
                int npcRealm = person.getInt("Realm");
                if (npcRealm < 27 && person.getLong("CultivationProgress") >= cultivationLevel(npcRealm).qiNeeded() && LifeSimulationBalance.supported(person, npcRealm + 1)) {
                    person.putInt("Realm", npcRealm + 1); person.putLong("CultivationProgress", 0);
                }
            }
            person.putInt("Affinity", Mth.clamp(
                    person.getInt("Affinity") - (person.getInt("Affinity") > 0 && random.nextFloat() < 0.22F ? 1 : 0),
                    -100,
                    100));
            int deathAge = person.getInt("Realm") > 0 ? cultivationLevel(person.getInt("Realm")).maxAge() : 78;
            if (person.getInt("Age") > deathAge
                    && random.nextFloat() < 0.08F) {
                person.putBoolean("Alive", false);
                person.putBoolean("Available", false);
                person.putInt("DeathYear", life.getInt("Age"));
                person.putString("DeathCause", "natural_death");
                int grief = person.getInt("Affinity") <= 0 ? 0 : Math.min(8, person.getInt("Affinity") / 15);
                int allowedGrief = Math.min(grief, Math.max(0, 12 - yearlyGrief));
                yearlyGrief += allowedGrief;
                life.putInt("Mentality", clampAttribute(life.getInt("Mentality") - allowedGrief));
                addLog(life, "life.immortal_cultivation_mod.log.relationship_death");
                important = true;
            }
        }
        if (relationships.size() >= 80) {
            CompoundTag memorials = life.getCompound("NpcMemorials");
            for (int i = relationships.size() - 1; i >= 0 && relationships.size() >= 70; i--) {
                CompoundTag old = relationships.getCompound(i);
                if (!old.getBoolean("Alive") && old.hasUUID("Id") && !old.getBoolean("Friend")
                        && !List.of("father", "mother", "child", "dao_companion").contains(old.getString("Relation"))) {
                    memorials.put(old.getUUID("Id").toString(), old.copy()); relationships.remove(i);
                }
            }
            life.put("NpcMemorials", memorials);
        }
        if (life.getInt("Age") % 12 == 0 && life.getInt("Age") >= 24 && relationships.size() < 80) {
            CompoundTag successor = person(pick(random, SURNAMES) + pick(random, MALE_NAMES), "acquaintance", 18, 10, "steady", "sect_disciple", "male");
            successor.putInt("BirthYear", life.getInt("Age") - 18);
            relationships.add(successor);
        }
        if (life.getInt("Age") >= 18
                && hasLivingRelation(relationships, "dao_companion")
                && countRelations(relationships, "child") < 3
                && random.nextFloat() < 0.08F) {
            String surname = life.getString("Name").isEmpty()
                    ? pick(random, SURNAMES)
                    : life.getString("Name").substring(0, 1);
            boolean male = random.nextBoolean();
            relationships.add(person(
                    surname + pick(random, male ? MALE_NAMES : FEMALE_NAMES),
                    "child",
                    0,
                    78,
                    random.nextBoolean() ? "cheerful" : "steady",
                    "child", male ? "male" : "female"));
            addLog(life, "life.immortal_cultivation_mod.log.child_birth");
            important = true;
        }
        life.put(RELATIONSHIPS, relationships);
        return important;
    }

    private static void applyAging(CompoundTag life, RandomSource random) {
        int age = life.getInt("Age");
        int maximum = Math.max(1, life.getInt("MaximumLifespan"));
        if (age > maximum * 0.70F) {
            life.putInt("Health", clampAttribute(
                    life.getInt("Health") - random.nextInt(1, 5)));
        } else if (random.nextFloat() < 0.24F) {
            life.putInt("Health", clampAttribute(life.getInt("Health") + 1));
        }
        if (age >= 16 && "child".equals(life.getString("Occupation"))) {
            life.putString("Occupation", "laborer");
        }
    }

    private static void applyPassiveCultivation(
            CompoundTag life, RandomSource random) {
        if (life.getInt("Realm") <= 0) {
            return;
        }
        LifeSimulationBalance.train(life, LifeSimulationTuning.get("passive_training"));
    }

    private static boolean applyEvent(
            CompoundTag life, LifeSimulationEvents.Definition event) {
        for (String effect : event.effects()) {
            applyEffect(life, effect);
        }
        List<LifeSimulationEvents.Choice> available = event.choices().stream()
                .filter(choice -> choice.available(life))
                .toList();
        if (available.isEmpty()) {
            addLog(life, event.textKey());
            return false;
        }
        CompoundTag current = new CompoundTag();
        current.putString("Id", event.id().toString());
        current.putString("TextKey", event.textKey());
        ListTag choices = new ListTag();
        for (LifeSimulationEvents.Choice definition : available) {
            CompoundTag choice = new CompoundTag();
            choice.putString("Id", definition.id());
            choice.putString("TextKey", definition.textKey());
            choice.putString("ResultKey", definition.resultKey());
            choice.put("Effects", stringList(definition.effects()));
            if (hasUpgrade(life, "danger_sense")) {
                choice.putInt("Danger", estimateDanger(definition.effects()));
            }
            choices.add(choice);
        }
        current.put("Choices", choices);
        life.put(CURRENT_EVENT, current);
        return true;
    }

    private static void setRootTestEvent(CompoundTag life) {
        CompoundTag event = new CompoundTag();
        event.putString("Id", "root_test");
        event.putString(
                "TextKey", "life.immortal_cultivation_mod.event.root_test");
        ListTag choices = new ListTag();
        choices.add(choice(
                "join_sect",
                "life.immortal_cultivation_mod.choice.join_sect",
                "life.immortal_cultivation_mod.result.join_sect",
                List.of("root_test:join_sect")));
        choices.add(choice(
                "stay_home",
                "life.immortal_cultivation_mod.choice.stay_home",
                "life.immortal_cultivation_mod.result.stay_home",
                List.of("root_test:stay_home")));
        choices.add(choice(
                "seek_master",
                "life.immortal_cultivation_mod.choice.seek_master",
                "life.immortal_cultivation_mod.result.seek_master",
                List.of("root_test:seek_master")));
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
    }

    private static void setBreakthroughEvent(CompoundTag life) {
        CompoundTag event = new CompoundTag();
        event.putString("Id", "breakthrough");
        event.putString(
                "TextKey", "life.immortal_cultivation_mod.event.breakthrough");
        ListTag choices = new ListTag();
        choices.add(choice(
                "attempt",
                "life.immortal_cultivation_mod.choice.breakthrough_attempt",
                "life.immortal_cultivation_mod.result.breakthrough_attempt",
                List.of("breakthrough:attempt")));
        choices.add(choice(
                "prepare",
                "life.immortal_cultivation_mod.choice.breakthrough_prepare",
                "life.immortal_cultivation_mod.result.breakthrough_prepare",
                List.of("breakthrough:prepare")));
        choices.add(choice(
                "seek_help",
                "life.immortal_cultivation_mod.choice.breakthrough_help",
                "life.immortal_cultivation_mod.result.breakthrough_help",
                List.of("breakthrough:help")));
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
    }

    private static void setSectMissionEvent(CompoundTag life) {
        CompoundTag event = new CompoundTag();
        event.putString("Id", "sect_mission_board");
        event.putString(
                "TextKey", "life.immortal_cultivation_mod.event.sect_mission_board");
        ListTag choices = new ListTag();
        choices.add(choice(
                "patrol",
                "life.immortal_cultivation_mod.choice.mission_patrol",
                "life.immortal_cultivation_mod.result.mission_patrol_accept",
                List.of("mission:patrol")));
        choices.add(choice(
                "escort",
                "life.immortal_cultivation_mod.choice.mission_escort",
                "life.immortal_cultivation_mod.result.mission_escort_accept",
                List.of("mission:escort")));
        choices.add(choice(
                "hunt",
                "life.immortal_cultivation_mod.choice.mission_hunt",
                "life.immortal_cultivation_mod.result.mission_hunt_accept",
                List.of("mission:hunt")));
        choices.add(choice(
                "records",
                "life.immortal_cultivation_mod.choice.mission_records",
                "life.immortal_cultivation_mod.result.mission_records",
                List.of("reputation+2", "wealth+60", "comprehension+1")));
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
    }

    private static void resolveSectMission(CompoundTag life, String route) {
        CompoundTag event = new CompoundTag();
        event.putString("Id", "sect_mission_" + route);
        event.putString(
                "TextKey",
                "life.immortal_cultivation_mod.event.sect_mission_" + route);
        ListTag choices = new ListTag();
        switch (route) {
            case "patrol" -> {
                choices.add(choice(
                        "follow_tracks",
                        "life.immortal_cultivation_mod.choice.mission_follow_tracks",
                        "life.immortal_cultivation_mod.result.mission_follow_tracks",
                        List.of("health-5", "combat+8", "reputation+7", "wealth+150",
                                "item:immortal_cultivation_mod:spirit_stone:8")));
                choices.add(choice(
                        "guard_village",
                        "life.immortal_cultivation_mod.choice.mission_guard_village",
                        "life.immortal_cultivation_mod.result.mission_guard_village",
                        List.of("morality+6", "reputation+5", "wealth+90",
                                "friend:village_friend")));
                choices.add(choice(
                        "report_tracks",
                        "life.immortal_cultivation_mod.choice.mission_report_tracks",
                        "life.immortal_cultivation_mod.result.mission_report_tracks",
                        List.of("reputation+3", "comprehension+2", "wealth+70")));
            }
            case "escort" -> {
                choices.add(choice(
                        "mountain_pass",
                        "life.immortal_cultivation_mod.choice.mission_mountain_pass",
                        "life.immortal_cultivation_mod.result.mission_mountain_pass",
                        List.of("health-8", "combat+6", "wealth+220", "reputation+6",
                                "item:immortal_cultivation_mod:medium_spirit_stone:2")));
                choices.add(choice(
                        "river_road",
                        "life.immortal_cultivation_mod.choice.mission_river_road",
                        "life.immortal_cultivation_mod.result.mission_river_road",
                        List.of("mentality-2", "wealth+150", "reputation+4",
                                "item:immortal_cultivation_mod:spirit_stone:10")));
                choices.add(choice(
                        "negotiate",
                        "life.immortal_cultivation_mod.choice.mission_negotiate",
                        "life.immortal_cultivation_mod.result.mission_negotiate",
                        List.of("charisma+4", "reputation+7", "wealth+180")));
            }
            case "hunt" -> {
                choices.add(choice(
                        "direct_hunt",
                        "life.immortal_cultivation_mod.choice.mission_direct_hunt",
                        "life.immortal_cultivation_mod.result.mission_direct_hunt",
                        List.of("health-12", "combat+12", "cultivation+500", "wealth+260",
                                "item:immortal_cultivation_mod:low_qi_recovery_pill:2")));
                choices.add(choice(
                        "set_trap",
                        "life.immortal_cultivation_mod.choice.mission_set_trap",
                        "life.immortal_cultivation_mod.result.mission_set_trap",
                        List.of("comprehension+4", "combat+6", "reputation+8", "wealth+230")));
                choices.add(choice(
                        "request_support",
                        "life.immortal_cultivation_mod.choice.mission_request_support",
                        "life.immortal_cultivation_mod.result.mission_request_support",
                        List.of("reputation+4", "wealth+120", "friend:sect_companion")));
            }
            default -> {
                return;
            }
        }
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
    }

    private static void setExplorationEvent(CompoundTag life) {
        CompoundTag event = new CompoundTag();
        event.putString("Id", "yearly_exploration");
        event.putString(
                "TextKey", "life.immortal_cultivation_mod.event.exploration_crossroads");
        ListTag choices = new ListTag();
        choices.add(choice(
                "herb_valley",
                "life.immortal_cultivation_mod.choice.explore_herb_valley",
                "life.immortal_cultivation_mod.result.explore_herb_valley",
                List.of("health+2", "alchemy+3", "wealth+55",
                        "item:immortal_cultivation_mod:bigu_pill:2")));
        choices.add(choice(
                "sealed_cave",
                "life.immortal_cultivation_mod.choice.explore_sealed_cave",
                "life.immortal_cultivation_mod.result.explore_sealed_cave",
                List.of("health-9", "cultivation+600", "combat+5",
                        "item:immortal_cultivation_mod:spirit_stone:14")));
        choices.add(choice(
                "traveling_caravan",
                "life.immortal_cultivation_mod.choice.explore_caravan",
                "life.immortal_cultivation_mod.result.explore_caravan",
                List.of("charisma+3", "wealth+120", "friend:caravan_friend")));
        choices.add(choice(
                "return_home",
                "life.immortal_cultivation_mod.choice.explore_return",
                "life.immortal_cultivation_mod.result.explore_return",
                List.of("mentality+3", "luck+1")));
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
    }

    private static CompoundTag choice(
            String id,
            String textKey,
            String resultKey,
            List<String> effects) {
        CompoundTag choice = new CompoundTag();
        choice.putString("Id", id);
        choice.putString("TextKey", textKey);
        choice.putString("ResultKey", resultKey);
        choice.put("Effects", stringList(effects));
        return choice;
    }

    private static void chooseEvent(ServerPlayer player, String choiceId) {
        CompoundTag life = read(player);
        if (!life.getBoolean("Active")
                || !life.getBoolean("Alive")
                || !life.contains(CURRENT_EVENT, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag event = life.getCompound(CURRENT_EVENT).copy();
        if (event.getBoolean("Story")) {
            LifeSimulationStories.choose(life, choiceId, player.getRandom());
            if (life.getInt("Health") <= 0) finishLife(player, life, "event_death");
            save(player, life);
            return;
        }
        ListTag choices = event.getList("Choices", Tag.TAG_COMPOUND);
        CompoundTag selected = null;
        for (int i = 0; i < choices.size(); i++) {
            CompoundTag candidate = choices.getCompound(i);
            if (candidate.getString("Id").equals(choiceId)) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return;
        }
        String questId = event.getString("QuestId");
        String questNode = event.getString("QuestNode");
        String nextNode = selected.getString("NextNode");
        life.remove(CURRENT_EVENT);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "life_choice");
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "life_event");
        for (String effect : readStrings(selected.getList("Effects", Tag.TAG_STRING))) {
            if (effect.startsWith("breakthrough:")) {
                resolveBreakthrough(player, life, effect.substring(13));
            } else if (effect.startsWith("root_test:")) {
                resolveRootTest(life, effect.substring(10));
            } else if (effect.startsWith("mission:")) {
                resolveSectMission(life, effect.substring(8));
            } else {
                applyEffect(life, effect);
            }
        }
        if (life.getBoolean("Alive")) {
            addLog(life, selected.getString("ResultKey"));
        }
        if (!questId.isBlank()) {
            recordQuestChoice(
                    life, questId, questNode, selected.getString("Id"), nextNode);
            if (life.getBoolean("Alive") && !nextNode.isBlank()) {
                LifeSimulationQuests.Definition quest = LifeSimulationQuests.find(
                        player.server.getResourceManager(), questId);
                if (quest != null) {
                    openQuestNode(life, quest, nextNode);
                }
            }
        }
        boolean cultivationDecision = readStrings(selected.getList("Effects", Tag.TAG_STRING)).stream()
                .anyMatch(effect -> effect.startsWith("breakthrough:") || effect.startsWith("root_test:"));
        if (!cultivationDecision && nextNode.isBlank()) LifeSimulationBalance.rewardEncounter(life);
        if (life.getInt("Health") <= 0) {
            finishLife(player, life, "event_death");
        }
        save(player, life);
    }

    private static void resolveRootTest(CompoundTag life, String route) {
        int potential = life.getInt("SpiritualRootPotential");
        String root = potential >= 90 ? "heavenly"
                : potential >= 72 ? "single"
                : potential >= 52 ? "dual"
                : potential >= 30 ? "triple"
                : potential >= 15 ? "pseudo"
                : "none";
        life.putString("SpiritualRoot", root);
        life.putBoolean("LuckRevealed", true);
        setFlag(life, "root_tested");
        if (potential >= 15) {
            life.putInt("Realm", 1);
            life.putInt("HighestRealm", Math.max(1, life.getInt("HighestRealm")));
            life.putInt("MaximumLifespan", Math.max(
                    life.getInt("MaximumLifespan"), cultivationLevel(1).maxAge()));
            life.putLong(
                    "CultivationProgress",
                    cultivationRequired(1) * (250L + potential * 8L) / 10_000L);
            learnMethod(life, CultivationMethods.BASIC_BREATHING);
            discoverCoreSpells(life, 1);
            if ("join_sect".equals(route)) {
                life.putString("Sect", "azure_cloud_sect");
                life.putString("Occupation", "sect_disciple");
                life.putInt("Reputation", life.getInt("Reputation") + 5);
                addSimulationItem(life, "immortal_cultivation_mod:spirit_stone", 12);
            } else if ("seek_master".equals(route)) {
                life.putString("Occupation", "wandering_cultivator");
                addCultivationBasisPoints(life, 500);
                learnMethod(life, CultivationMethods.CLEAR_HEART);
            }
        }
    }

    private static void resolveBreakthrough(
            ServerPlayer player, CompoundTag life, String route) {
        if ("prepare".equals(route)) {
            life.putLong("CultivationProgress",
                    cultivationRequired(life.getInt("Realm")) * 92L / 100L);
            life.putInt("CultivationStability", Mth.clamp(
                    life.getInt("CultivationStability") + 10, 0, 100));
            return;
        }
        if ("help".equals(route)) {
            if (life.getLong("Wealth") >= 150L) {
                life.putLong("Wealth", life.getLong("Wealth") - 150L);
                life.putInt("CultivationStability", Mth.clamp(
                        life.getInt("CultivationStability") + 18, 0, 100));
            }
            life.putLong("CultivationProgress",
                    cultivationRequired(life.getInt("Realm")) * 95L / 100L);
            return;
        }
        RandomSource random = yearlyRandom(life);
        if (!LifeSimulationBalance.supported(life, life.getInt("Realm") + 1)) {
            addLog(life, "life.balance.method_limit");
            return;
        }
        if (life.getInt("Realm") >= 6 && life.getInt("Realm") % 3 == 0) {
            if (life.contains("LastTribulationAttemptYear")
                    && life.getInt("LastTribulationAttemptYear") >= life.getInt("Age")) {
                addLog(life, "life.balance.tribulation_retry");
                return;
            }
            CompoundTag tribulation = LifeSimulationStories.breakthroughTrial(player.server.getResourceManager());
            if (tribulation != null && LifeSimulationStories.start(life, tribulation)) {
                // Explicit major breakthroughs require this encounter even after an
                // earlier withdrawal or another crisis. Random-event cooldowns never
                // turn the required trial into an ordinary percentage roll.
                life.putInt("LastTribulationAttemptYear", life.getInt("Age"));
            } else {
                addLog(life, "life.balance.tribulation_unavailable");
            }
            return;
        }
        float chance = 0.18F
                + life.getInt("Comprehension") * 0.0035F
                + life.getInt("Health") * 0.0015F
                + life.getInt("Luck") * 0.001F
                + life.getInt("CultivationStability") * 0.0015F;
        chance = Mth.clamp(chance, 0.12F, 0.92F);
        if (random.nextFloat() <= chance) {
            int realm = Math.min(27, life.getInt("Realm") + 1);
            life.putInt("Realm", realm);
            life.putInt("HighestRealm", Math.max(realm, life.getInt("HighestRealm")));
            life.putLong("CultivationProgress", 0L);
            life.putInt("CultivationStability", 48);
            life.putInt("MaximumLifespan", Math.max(
                    life.getInt("MaximumLifespan"), cultivationLevel(realm).maxAge()));
            discoverCoreSpells(life, realm);
            discoverSpell(life, random);
            grantBreakthroughSupplies(life, realm);
            addLog(life, "life.immortal_cultivation_mod.log.breakthrough_success");
        } else {
            int damage = random.nextInt(12, 34);
            life.putInt("Health", clampAttribute(life.getInt("Health") - damage));
            life.putInt("Mentality", clampAttribute(life.getInt("Mentality") - 8));
            life.putLong("CultivationProgress",
                    cultivationRequired(life.getInt("Realm")) * 62L / 100L);
            life.putInt("CultivationStability", Math.max(5,
                    life.getInt("CultivationStability") - 16));
            addLog(life, "life.immortal_cultivation_mod.log.breakthrough_failure");
            if (life.getInt("Health") <= 0) {
                finishLife(player, life, "breakthrough_death");
            }
        }
    }

    private static void discoverSpell(CompoundTag life, RandomSource random) {
        int realm = life.getInt("Realm");
        ListTag known = life.getList(DISCOVERED_SPELLS, Tag.TAG_STRING);
        List<ModSpells.SpellDef> eligible = ModSpells.all().stream()
                .filter(spell -> {
                    int required = CultivationLevels.getStageIndex(spell.requiredLevel());
                    return required > 0 && required <= realm
                            && !ModSpells.innateSpellIds().contains(spell.id())
                            && !contains(known, spell.id());
                })
                .toList();
        if (eligible.isEmpty()) {
            return;
        }
        String spell = eligible.get(random.nextInt(eligible.size())).id();
        learnSpell(life, spell);
    }

    private static void discoverCoreSpells(CompoundTag life, int realm) {
        String[] foundation = {
                ModSpells.QI_GATHERING,
                ModSpells.WIND_STEP,
                ModSpells.FIREBALL
        };
        for (String spellId : foundation) {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            if (spell != null
                    && CultivationLevels.getStageIndex(spell.requiredLevel()) <= realm) {
                learnSpell(life, spellId);
            }
        }
        if (realm >= CultivationLevels.getStageIndex(
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID)) {
            learnSpell(life, ModSpells.REGENERATION);
        }
    }

    private static void learnSpell(CompoundTag life, String spellId) {
        if (spellId == null || spellId.isBlank() || !LifeSimulationInheritance.canLearnSpell(life, spellId)) {
            return;
        }
        ListTag known = life.getList(DISCOVERED_SPELLS, Tag.TAG_STRING);
        if (!contains(known, spellId)) {
            known.add(StringTag.valueOf(spellId));
            life.put(DISCOVERED_SPELLS, known);
        }
    }

    private static void learnMethod(CompoundTag life, String methodId) {
        CultivationMethods.MethodDef method = CultivationMethods.get(methodId);
        if (method == null) {
            return;
        }
        ListTag known = life.getList(LEARNED_METHODS, Tag.TAG_STRING);
        if (!contains(known, methodId)) {
            known.add(StringTag.valueOf(methodId));
            life.put(LEARNED_METHODS, known);
        }
        String active = life.getString(ACTIVE_METHOD);
        CultivationMethods.MethodDef activeMethod = CultivationMethods.get(active);
        if (activeMethod == null
                || CultivationLevels.getStageIndex(activeMethod.limitLevel())
                        < CultivationLevels.getStageIndex(method.limitLevel())
                || activeMethod.limitLevel().equals(method.limitLevel())
                        && LifeStoryLoot.methodTier(activeMethod.tier()) < LifeStoryLoot.methodTier(method.tier())) {
            life.putString(ACTIVE_METHOD, methodId);
        }
    }

    private static void ensureMethodForRealm(
            CompoundTag life, RandomSource random) {
        int realm = life.getInt("Realm");
        CultivationMethods.MethodDef current = CultivationMethods.get(
                life.getString(ACTIVE_METHOD));
        if (current != null
                && CultivationLevels.getStageIndex(current.limitLevel()) >= realm) {
            return;
        }
        List<CultivationMethods.MethodDef> candidates = CultivationMethods.allMethods().stream()
                .filter(method -> CultivationLevels.getStageIndex(method.limitLevel()) >= realm)
                .filter(method -> methodForSimulatedRealm(method, realm))
                .toList();
        if (candidates.isEmpty()) {
            candidates = CultivationMethods.allMethods().stream()
                    .filter(method -> CultivationLevels.getStageIndex(method.limitLevel()) >= realm)
                    .toList();
        }
        if (!candidates.isEmpty()) {
            learnMethod(life, candidates.get(random.nextInt(candidates.size())).id());
        }
    }

    private static boolean methodForSimulatedRealm(
            CultivationMethods.MethodDef method, int realm) {
        int limit = CultivationLevels.getStageIndex(method.limitLevel());
        if (realm <= 6) {
            return CultivationMethods.BASIC_BREATHING.equals(method.id())
                    || CultivationMethods.CLEAR_HEART.equals(method.id());
        }
        if (realm <= 12) {
            return limit >= 9 && limit <= 15;
        }
        return limit >= realm;
    }

    private static void grantBreakthroughSupplies(
            CompoundTag life, int realm) {
        int major = Math.max(1, (realm - 1) / 3 + 1);
        if (major >= 5) {
            addSimulationItem(life, "immortal_cultivation_mod:high_spirit_stone", 2);
        } else if (major >= 2) {
            addSimulationItem(life, "immortal_cultivation_mod:medium_spirit_stone", 3);
        } else {
            addSimulationItem(life, "immortal_cultivation_mod:spirit_stone", 8);
        }
        String pill = major >= 4
                ? "immortal_cultivation_mod:high_qi_recovery_pill"
                : major >= 2
                        ? "immortal_cultivation_mod:middle_qi_recovery_pill"
                        : "immortal_cultivation_mod:low_qi_recovery_pill";
        addSimulationItem(life, pill, 2);
    }

    private static void performActivity(
            ServerPlayer player, String activity) {
        CompoundTag life = read(player);
        if (!activityUnavailable(life, activity).isBlank()) {
            return;
        }
        RandomSource random = yearlyRandom(life);
        int cost = "seclusion".equals(activity) ? 2 : 1;
        if (life.getInt("AnnualActionPoints") < cost || "seclusion".equals(activity) && life.getBoolean("SeclusionThisYear")) return;
        boolean valid = true;
        switch (activity) {
            case "study" -> {
                addAttribute(life, "Comprehension", random.nextInt(2, 6));
                addAttribute(life, "Mentality", -1);
            }
            case "exercise" -> {
                addAttribute(life, "Health", random.nextInt(2, 6));
                life.putInt("Combat", life.getInt("Combat") + random.nextInt(2, 5));
            }
            case "family" -> {
                addAttribute(life, "Mentality", 5);
                improveAllRelationships(life, 3);
            }
            case "explore" -> valid = LifeSimulationStories.select(life, player.server.getResourceManager(), "adventure", random) || setRandomQuestEvent(
                    player,
                    life,
                    random,
                    LifeSimulationQuests.Pool.EXPLORE);
            case "work" -> {
                if (life.getInt("Age") < 12) {
                    valid = false;
                } else {
                    life.putLong("Wealth", life.getLong("Wealth")
                            + random.nextInt(45, 141));
                    if (life.getInt("Age") >= 16
                            && "laborer".equals(life.getString("Occupation"))) {
                        String[] jobs = {"herbalist", "merchant", "physician", "smith"};
                        life.putString("Occupation", pick(random, jobs));
                    }
                }
            }
            case "socialize" -> {
                addAttribute(life, "Charisma", random.nextInt(2, 5));
                addAttribute(life, "Mentality", 3);
                improveAllRelationships(life, 2);
                maybeAddFriend(life, random);
            }
            case "rest" -> {
                addAttribute(life, "Health", 4);
                addAttribute(life, "Mentality", 4);
                life.putInt("TrainingStrain", Math.max(0, life.getInt("TrainingStrain") - 8));
            }
            case "consolidate" -> {
                life.putInt("CultivationStability", Math.min(100, life.getInt("CultivationStability") + 6));
                life.putInt("TrainingStrain", Math.max(0, life.getInt("TrainingStrain") - 4));
            }
            case "keep_oath" -> {
                CompoundTag oath = life.getCompound("SpiritualOath");
                if (!oath.getBoolean("Accepted") || oath.getBoolean("Fulfilled") || life.getLong("Wealth") < 100L) valid = false;
                else {
                    life.putLong("Wealth", life.getLong("Wealth") - 100L);
                    oath.putBoolean("Fulfilled", true); oath.putBoolean("Broken", false);
                }
            }
            case "breathing" -> {
                valid = life.getInt("Realm") > 0;
                if (valid) LifeSimulationBalance.train(life, LifeSimulationTuning.get("breathing_training"));
            }
            case "cultivate" -> {
                if (life.getInt("Realm") <= 0) {
                    valid = false;
                } else {
                    LifeSimulationBalance.train(life, LifeSimulationTuning.get("cultivate_training"));
                    life.putInt("TrainingStrain", Math.min(100, life.getInt("TrainingStrain") + 6));
                    addAttribute(life, "Mentality", -2);
                    life.putInt("CultivationStability", Mth.clamp(
                            life.getInt("CultivationStability") + 2, 0, 100));
                }
            }
            case "practice" -> {
                if (life.getInt("Realm") <= 0) {
                    valid = false;
                } else {
                    life.putInt("Combat", life.getInt("Combat")
                            + random.nextInt(4, 10));
                    addAttribute(life, "Health", -2);
                }
            }
            case "sect_mission" -> {
                if ("none".equals(life.getString("Sect"))) {
                    valid = false;
                } else {
                    valid = LifeSimulationStories.select(life, player.server.getResourceManager(), "sect", random) || setRandomQuestEvent(
                            player,
                            life,
                            random,
                            LifeSimulationQuests.Pool.SECT);
                }
            }
            case "seclusion" -> {
                if (life.getInt("Realm") <= 0) {
                    valid = false;
                } else {
                    LifeSimulationBalance.train(life, LifeSimulationTuning.get("seclusion_training"));
                    life.putBoolean("SeclusionThisYear", true);
                    life.putInt("TrainingStrain", Math.min(100, life.getInt("TrainingStrain") + 12));
                    addAttribute(life, "Mentality", -5);
                    improveAllRelationships(life, -3);
                }
            }
            default -> valid = false;
        }
        if (!valid) {
            return;
        }
        life.putInt("AnnualActionPoints",
                life.getInt("AnnualActionPoints") - cost);
        addLog(life, "life.immortal_cultivation_mod.activity." + activity);
        if (life.getInt("Health") <= 0) {
            finishLife(player, life, "activity_death");
        } else if (!life.contains(CURRENT_EVENT, Tag.TAG_COMPOUND)
                && isReadyForBreakthrough(life)) {
            setBreakthroughEvent(life);
        }
        save(player, life);
    }

    public static final List<String> ACTIVITIES = List.of("study", "exercise", "family", "explore", "work", "socialize", "rest", "cultivate", "practice", "sect_mission", "seclusion", "breathing", "consolidate", "keep_oath");
    public static final List<String> ROUTINE_CONDITIONS = List.of("none", "any", "health", "strain", "stability");

    /** Shared manual/planner validation; returns a localized reason suffix or an empty string. */
    public static String activityUnavailable(CompoundTag life,String activity) {
        if (!ACTIVITIES.contains(activity)) return "requirements";
        if (!life.getBoolean("Active") || !life.getBoolean("Alive") || life.getBoolean("Finished")) return "finished";
        if (life.contains(CURRENT_EVENT,Tag.TAG_COMPOUND) || life.contains("Story",Tag.TAG_COMPOUND) || LifeSimulationBattle.active(life)) return "encounter";
        if (LifeSimulationWorldMap.blocksActivities(life) || LifeSimulationWorldMap.blocksYearAdvance(life)) return "journey";
        if (life.getInt("AnnualActionPoints")<=0) return "no_actions";
        if (activity.equals("work") && life.getInt("Age")<12) return "work_age";
        if (activity.equals("sect_mission") && (life.getString("Sect").isBlank() || life.getString("Sect").equals("none"))) return "sect";
        if (activity.equals("keep_oath") && (!life.getCompound("SpiritualOath").getBoolean("Accepted") || life.getCompound("SpiritualOath").getBoolean("Fulfilled") || life.getLong("Wealth")<100)) return "oath";
        if (List.of("cultivate","breathing","seclusion","practice").contains(activity) && life.getInt("Realm")<=0) return "awakening";
        if (activity.equals("seclusion") && life.getBoolean("SeclusionThisYear")) return "seclusion_once";
        if (activity.equals("seclusion") && life.getInt("AnnualActionPoints")<2) return "seclusion_actions";
        if (List.of("cultivate","breathing","seclusion").contains(activity) && life.getString(ACTIVE_METHOD).isBlank()) return "method";
        return "";
    }

    public static boolean canRunPlannedYear(CompoundTag life) { return canAdvance(life) && life.hasUUID("SimulationId"); }

    /** One bounded annual routine. Every external decision stops this operation. */
    private static void routineAction(ServerPlayer player, String payload) {
        CompoundTag life = read(player);
        String[] fields = payload.split(":");
        if (fields.length == 4 && fields[0].equals("slot")) {
            int slot;
            try { slot = Integer.parseInt(fields[1]); } catch (NumberFormatException e) { return; }
            if (slot < 0 || slot >= 3 || !ACTIVITIES.contains(fields[2]) || !ROUTINE_CONDITIONS.contains(fields[3])) return;
            CompoundTag routine = life.getCompound("Routine");
            CompoundTag entry = new CompoundTag(); entry.putString("Action", fields[2]); entry.putString("Condition", fields[3]);
            routine.put(Integer.toString(slot), entry); life.put("Routine", routine); save(player, life); return;
        }
        boolean requestedRun = payload.equals("run");
        if (fields.length == 3 && fields[0].equals("run") && life.hasUUID("SimulationId")) {
            try {
                requestedRun = life.getUUID("SimulationId").equals(java.util.UUID.fromString(fields[1])) && life.getInt("Age") == Integer.parseInt(fields[2]);
            } catch (IllegalArgumentException e) { return; }
        }
        if (!requestedRun || !canAdvance(life)) return;
        for (int slot = life.getInt("RoutineCursor"); slot < 3 && canAct(life); slot++) {
            CompoundTag entry = life.getCompound("Routine").getCompound(Integer.toString(slot));
            String activity = entry.getString("Action"); if (activity.isBlank()) activity = "rest";
            boolean ready = switch (entry.getString("Condition")) {
                case "health" -> life.getInt("Health") < 60;
                case "strain" -> life.getInt("TrainingStrain") > 50;
                case "stability" -> life.getInt("CultivationStability") < 60;
                default -> true;
            };
            if (!ready) {
                life.putInt("RoutineCursor", slot + 1);
                save(player, life);
                continue;
            }
            if (life.getInt("Health") < 30 || life.getInt("TrainingStrain") > 85) activity = "rest";
            if (activity.equals("seclusion") && (life.getInt("AnnualActionPoints") < 2 || life.getBoolean("SeclusionThisYear"))) activity = "rest";
            if (List.of("cultivate", "breathing", "seclusion", "practice").contains(activity) && life.getInt("Realm") <= 0) activity = "study";
            if (activity.equals("sect_mission") && "none".equals(life.getString("Sect"))) activity = "work";
            if (activity.equals("work") && life.getInt("Age") < 12) activity = "study";
            if (!activityUnavailable(life,activity).isBlank()) activity = "rest";
            life.putInt("RoutineCursor", slot + 1); save(player, life);
            performActivity(player, activity); life = read(player);
            if (!canAdvance(life)) return;
        }
        if (canAdvance(life) && (life.getInt("RoutineCursor") >= 3 || life.getInt("AnnualActionPoints") == 0)) {
            advanceOneYear(player, life); save(player, life);
        }
    }

    private static boolean setRandomQuestEvent(
            ServerPlayer player,
            CompoundTag life,
            RandomSource random,
            LifeSimulationQuests.Pool pool) {
        String previousKey = pool == LifeSimulationQuests.Pool.EXPLORE
                ? LAST_EXPLORE_QUEST
                : LAST_SECT_QUEST;
        LifeSimulationQuests.Definition quest = LifeSimulationQuests.select(
                player.server.getResourceManager(),
                pool,
                life,
                random,
                life.getString(previousKey));
        if (quest == null) {
            return false;
        }
        if (!openQuestNode(life, quest, quest.startNode())) {
            return false;
        }
        life.getCompound(CURRENT_EVENT).putBoolean("RandomlyAssignedQuest", true);
        life.putString(previousKey, quest.id().toString());
        return true;
    }

    static boolean openQuestNode(
            CompoundTag life,
            LifeSimulationQuests.Definition quest,
            String nodeId) {
        LifeSimulationQuests.Node node = quest.node(nodeId);
        if (node == null) {
            return false;
        }
        List<LifeSimulationQuests.Choice> available = node.choices().stream()
                .filter(choice -> choice.available(life))
                .toList();
        if (available.isEmpty()) {
            return false;
        }
        CompoundTag event = new CompoundTag();
        event.putString("Id", quest.id() + ":" + node.id());
        event.putString("TextKey", node.textKey());
        event.putBoolean("QuestGraph", true);
        event.putString("QuestId", quest.id().toString());
        event.putString("QuestNode", node.id());
        event.putString("QuestPool", quest.pool().name().toLowerCase(Locale.ROOT));
        ListTag choices = new ListTag();
        for (LifeSimulationQuests.Choice definition : available) {
            CompoundTag choice = new CompoundTag();
            choice.putString("Id", definition.id());
            choice.putString("TextKey", definition.textKey());
            choice.putString("ResultKey", definition.resultKey());
            choice.putString("NextNode", definition.nextNode());
            choice.put("Effects", stringList(definition.effects()));
            if (hasUpgrade(life, "danger_sense")) {
                choice.putInt("Danger", estimateDanger(definition.effects()));
            }
            choices.add(choice);
        }
        event.put("Choices", choices);
        life.put(CURRENT_EVENT, event);
        CompoundTag progress = life.getCompound(QUEST_PROGRESS);
        CompoundTag questState = progress.getCompound(quest.id().toString());
        questState.putBoolean("Started", true);
        questState.putString("CurrentNode", node.id());
        progress.put(quest.id().toString(), questState);
        life.put(QUEST_PROGRESS, progress);
        addLog(life, node.textKey());
        return true;
    }

    private static void recordQuestChoice(
            CompoundTag life,
            String questId,
            String nodeId,
            String choiceId,
            String nextNode) {
        CompoundTag progress = life.getCompound(QUEST_PROGRESS);
        CompoundTag questState = progress.getCompound(questId);
        ListTag path = questState.getList("Path", Tag.TAG_STRING);
        path.add(StringTag.valueOf(nodeId + ":" + choiceId));
        questState.put("Path", path);
        if (nextNode.isBlank()) {
            questState.putBoolean("Completed", true);
            questState.remove("CurrentNode");
        } else {
            questState.putString("CurrentNode", nextNode);
        }
        progress.put(questId, questState);
        life.put(QUEST_PROGRESS, progress);
    }

    public static boolean isParent(CompoundTag person) {
        return "father".equals(person.getString("Relation")) || "mother".equals(person.getString("Relation"));
    }

    private static void relationshipAction(
            ServerPlayer player, String payload) {
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            return;
        }
        CompoundTag life = read(player);
        if (!canAct(life)) {
            return;
        }
        ListTag relationships = life.getList(RELATIONSHIPS, Tag.TAG_COMPOUND);
        int index = -1;
        try {
            index = Integer.parseInt(parts[0]);
        } catch (NumberFormatException numeric) {
            try {
                UUID identity = UUID.fromString(parts[0]);
                for (int candidate = 0; candidate < relationships.size(); candidate++) {
                    CompoundTag person = relationships.getCompound(candidate);
                    if (person.hasUUID("Id") && person.getUUID("Id").equals(identity)) { index = candidate; break; }
                }
            } catch (IllegalArgumentException malformed) { return; }
        }
        if (index < 0 || index >= relationships.size()) {
            return;
        }
        CompoundTag person = relationships.getCompound(index);
        if (!person.getBoolean("Alive") || person.getBoolean("Captive")
                || person.contains("Observed") && !person.getBoolean("Observed")) {
            return;
        }
        switch (parts[1]) {
            case "friend" -> {
                if (isParent(person) || person.getInt("Affinity") < 30) return;
                person.putBoolean("Friend", true); person.putBoolean("Nemesis", false);
                person.putInt("Trust", Math.max(40, person.getInt("Trust")));
            }
            case "breakup" -> {
                person.putBoolean("Friend", false); person.putBoolean("Protector", false);
            }
            case "protector" -> {
                if (!person.getBoolean("Friend") || person.getInt("Trust") < 40 || person.getInt("Age") < 16) return;
                person.putBoolean("Protector", !person.getBoolean("Protector"));
            }
            case "nemesis" -> {
                person.putBoolean("Nemesis", true); person.putBoolean("Friend", false); person.putBoolean("Protector", false);
                person.putInt("Affinity", -50); person.putString("Grievance", "declared_enmity");
            }
            case "talk" -> person.putInt("Affinity", Mth.clamp(
                    person.getInt("Affinity") + 4, -100, 100));
            case "gift" -> {
                if (life.getLong("Wealth") < 50L) {
                    return;
                }
                life.putLong("Wealth", life.getLong("Wealth") - 50L);
                person.putInt("Affinity", Mth.clamp(
                        person.getInt("Affinity") + 9, -100, 100));
            }
            case "help" -> {
                person.putInt("Affinity", Mth.clamp(
                        person.getInt("Affinity") + 7, -100, 100));
                life.putInt("Morality", Mth.clamp(
                        life.getInt("Morality") + 3, -100, 100));
            }
            default -> {
                return;
            }
        }
        relationships.set(index, person);
        if (!person.getBoolean("Nemesis") && !parts[1].equals("breakup")) person.putInt("Trust", Math.min(100, person.getInt("Trust") + 3));
        life.put(RELATIONSHIPS, relationships);
        life.putInt("AnnualActionPoints",
                life.getInt("AnnualActionPoints") - 1);
        addLog(life, "life.immortal_cultivation_mod.log.relationship_action");
        save(player, life);
    }

    private static void finishLife(
            ServerPlayer player, CompoundTag life, String cause) {
        if (life.getBoolean("Finished")) {
            return;
        }
        boolean manualSettlement = "manual_settlement".equals(cause);
        if (!manualSettlement
                && !"natural_death".equals(cause)
                && !life.getBoolean("CombatDead")
                && hasUpgrade(life, "fate_not_end")
                && !hasFlag(life, "fate_not_end_used")) {
            life.putBoolean("Alive", true);
            life.putInt("Health", Math.max(12, life.getInt("Health")));
            life.putInt(
                    "Mentality",
                    Math.max(1, life.getInt("Mentality") - 15));
            life.remove(CURRENT_EVENT);
            setFlag(life, "fate_not_end_used");
            addLog(life, "life.immortal_cultivation_mod.log.fate_saved");
            return;
        }
        life.putBoolean("Alive", false);
        life.putBoolean("Finished", true);
        life.putString("DeathCause", cause);
        String ending = ending(life, cause);
        life.putString("Ending", ending);
        long score = LifeSimulationBalance.score(life);
        life.putLong("Score", score);
        int insight = Mth.clamp((int) (score / LifeSimulationTuning.get("insight_divisor")) + 8, 8, Math.max(8,(int)LifeSimulationTuning.get("insight_cap")));
        life.putInt("InsightEarned", insight);
        life.putInt(INSIGHT, life.getInt(INSIGHT) + insight);
        ListTag endings = life.getList(ENDINGS, Tag.TAG_STRING);
        if (!contains(endings, ending)) {
            endings.add(StringTag.valueOf(ending));
            life.put(ENDINGS, endings);
        }
        life.putInt(
                "RetainedRootPotential",
                Math.max(
                        life.getInt("RetainedRootPotential"),
                        life.getInt("SpiritualRootPotential")));
        life.put(REWARDS, LifeSimulationInheritance.generate(life));
        life.putInt("InheritanceSourceVersion", LifeSimulationInheritance.VERSION);

        life.remove(CURRENT_EVENT);
        addLog(life, manualSettlement
                ? "life.immortal_cultivation_mod.log.manual_settlement"
                : "life.immortal_cultivation_mod.log.death");
    }

    private static void selectInheritance(ServerPlayer player, String selection) {
        CompoundTag life = read(player);
        if (!life.getBoolean("Finished") || life.getBoolean("RewardClaimed") || life.contains("PendingInheritance")) return;
        String[] parts = selection.split(":", -1);
        if (parts.length != 2) return;
        ListTag options = LifeSimulationInheritance.options(life, parts[0]);
        if (options.isEmpty()) return;
        int requested = -1;
        boolean cycle = parts[1].equals("next") || parts[1].equals("previous");
        if (!cycle) {
            try { requested = Integer.parseInt(parts[1]); }
            catch (NumberFormatException ignored) { return; }
            if (requested < 0 || requested >= options.size()) return;
        }
        ListTag rewards = life.getList(REWARDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < rewards.size(); i++) {
            CompoundTag reward = rewards.getCompound(i);
            if (!reward.getString("Id").equals(parts[0])) continue;
            int index = LifeSimulationInheritance.optionIndex(options, reward);
            int next = cycle ? Math.floorMod(index + (parts[1].equals("next") ? 1 : -1), options.size()) : requested;
            rewards.set(i, options.getCompound(next).copy());
            save(player, life);
            return;
        }
    }

    private static void claimInheritance(
            ServerPlayer player, String rewardId) {
        CompoundTag life = read(player);
        if (!life.getBoolean("Finished") || life.getBoolean("RewardClaimed")) {
            return;
        }
        ListTag rewards = life.getList(REWARDS, Tag.TAG_COMPOUND);
        CompoundTag selected = null;
        for (int i = 0; i < rewards.size(); i++) {
            CompoundTag reward = rewards.getCompound(i);
            if (reward.getString("Id").equals(rewardId)) {
                selected = reward;
                break;
            }
        }
        CompoundTag pending = life.getCompound("PendingInheritance");
        if ("none".equals(rewardId) && pending.isEmpty()) {
            boolean available = rewards.stream().anyMatch(t -> !"none".equals(inheritancePreview(player, life, (CompoundTag)t).getString("Kind")));
            if (available) return;
            selected = new CompoundTag();
            selected.putString("Id", "none");
        }
        if (selected == null && pending.isEmpty()) {
            return;
        }
        if (pending.isEmpty()) {
            CompoundTag previous = life.copy();
            pending.putString("RewardId", selected.getString("Id"));
            pending.put("Selected", selected.copy());
            CompoundTag preview = inheritancePreview(player, life, selected);
            if ("none".equals(preview.getString("Kind")) && !"none".equals(rewardId)) return;
            pending.put("Receipt", preview);
            life.putUUID("ClaimId", UUID.randomUUID());
            life.put("PendingInheritance", pending);
            life.putString("ClaimTransaction", "pending");
            save(player, life);
            if (!LifeInheritanceDelivery.checkpoint(player)) {
                save(player, previous);
                player.displayClientMessage(Component.translatable("life.expansion.inherit.save_failed"), false);
                return;
            }
        }
        // Retrying any visible claim cannot change a reserved choice or reroll its receipt.
        CompoundTag receipt = pending.getCompound("Receipt").copy();
        CompoundTag beforeDelivery = player.saveWithoutId(new CompoundTag());
        if (!deliverReceipt(player, receipt)) {
            player.displayClientMessage(Component.translatable("life.expansion.inherit.space"), false);
            return;
        }
        life.put("InheritanceReceipt", receipt);
        life.putBoolean("RewardClaimed", true);
        life.putString("ClaimTransaction", "delivered");
        life.remove("PendingInheritance");
        life.putBoolean("Active", false);
        save(player, life);
        if (!LifeInheritanceDelivery.checkpoint(player)) {
            player.load(beforeDelivery);
            ServerEvents.syncPlayerData(player);
            player.displayClientMessage(Component.translatable("life.expansion.inherit.save_failed"), false);
            return;
        }
        player.getInventory().setChanged();
        ServerEvents.syncPlayerData(player);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "life_finished");
    }

    private static boolean deliverReceipt(ServerPlayer player, CompoundTag receipt) {
        var data = ModAttachments.getData(player);
        switch (receipt.getString("Kind")) {
            case "materials" -> {
                var id = net.minecraft.resources.ResourceLocation.tryParse(receipt.getString("Item"));
                var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
                int count = receipt.getInt("Count");
                if (id == null || !registry.containsKey(id) || count < 1 || count > 4096) return false;
                ItemStack reward = new ItemStack(registry.get(id), count);
                int space = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.isEmpty()) space += reward.getMaxStackSize();
                    else if (ItemStack.isSameItemSameComponents(stack, reward)) space += Math.max(0, stack.getMaxStackSize() - stack.getCount());
                }
                if (space < count) return false;
                int remaining = count;
                for (ItemStack stack : player.getInventory().items) {
                    if (remaining == 0) break;
                    if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, reward)) {
                        int amount = Math.min(remaining, Math.max(0, stack.getMaxStackSize() - stack.getCount()));
                        stack.grow(amount); remaining -= amount;
                    }
                }
                for (int slot = 0; remaining > 0 && slot < player.getInventory().items.size(); slot++) {
                    if (player.getInventory().items.get(slot).isEmpty()) {
                        int amount = Math.min(remaining, reward.getMaxStackSize());
                        player.getInventory().items.set(slot, reward.copyWithCount(amount)); remaining -= amount;
                    }
                }
            }
            case "progress" -> {
                long gain = receipt.getLong("Gain");
                var carried = LifeSimulationInheritance.carry(data.cultivationLevel(), data.cultivationProgress(), gain);
                if (gain <= 0 || carried.gained() != gain) return false;
                int stages = Math.max(0, CultivationLevels.getStageIndex(carried.level()) - CultivationLevels.getStageIndex(data.cultivationLevel()));
                ModAttachments.setData(player, data.withCultivationLevel(carried.level())
                        .withCultivationProgress(carried.progress()).withAddedSkillPoints(stages * 10));
            }
            case "proficiency" -> {
                String spell = receipt.getString("Spell");
                int gain = receipt.getInt("Gain");
                if (!data.knownSpells().contains(spell) || gain < 0
                        || data.withAddedSpellProficiency(spell, gain).spellProficiency(spell) - data.spellProficiency(spell) != gain) return false;
                ModAttachments.setData(player, data.withAddedSpellProficiency(spell, gain));
            }
            case "spell" -> {
                String spell = receipt.getString("Spell");
                if (ModSpells.get(spell) == null) return false;
                ArrayList<String> known = new ArrayList<>(data.knownSpells());
                if (!known.contains(spell)) known.add(spell);
                ModAttachments.setData(player, data.withKnownSpells(List.copyOf(known)));
            }
            case "none" -> { /* A life with no eligible inheritance may still finish. */ }
            case "blessing" -> player.getPersistentData().putLong("LifeSimulationBlessingUntil",
                    player.serverLevel().getGameTime() + receipt.getInt("Minutes") * 1200L);
            default -> { return false; }
        }
        return true;
    }

    /** Source quantities come from the simulated life; only the final real-world stage limits cultivation delivery. */
    static CompoundTag inheritancePreview(ServerPlayer player, CompoundTag reward) {
        return inheritancePreview(player, read(player), reward);
    }

    private static CompoundTag inheritancePreview(ServerPlayer player, CompoundTag life, CompoundTag reward) {
        CompoundTag preview = new CompoundTag();
        String id = reward.getString("Id");
        preview.putString("Id", id);
        preview.putString("Kind", "none");
        preview.putString("Reason", switch (id) {
            case "progress" -> "no_cultivation";
            case "spell" -> "no_spell";
            case "materials" -> "no_items";
            default -> "empty";
        });
        if (!LifeSimulationInheritance.validSource(life, reward)) return preview;
        var data = ModAttachments.getData(player);
        switch (id) {
            case "progress" -> {
                long gain = LifeSimulationInheritance.progressGain(reward);
                var carried = LifeSimulationInheritance.carry(data.cultivationLevel(), data.cultivationProgress(), gain);
                long actual = carried.gained();
                if (actual > 0L) {
                    preview.putString("Kind", "progress");
                    preview.putLong("Gain", actual);
                    preview.putLong("SourceCultivation", reward.getLong("SourceCultivation"));
                    preview.putString("DestinationLevel", carried.level());
                    preview.putLong("DestinationProgress", carried.progress());
                } else {
                    if (gain > 0) preview.putString("Reason", "full_progress");
                }
            }
            case "spell" -> {
                String spell = reward.getString("Spell");
                boolean known = data.knownSpells().contains(spell);
                preview.putString("Kind", known ? "proficiency" : "spell");
                preview.putString("Spell", spell);
                preview.putInt("Gain", known ? (int)Math.max(0L, Math.min(INHERITED_SPELL_PROFICIENCY,
                        (long)Integer.MAX_VALUE - data.spellProficiency(spell))) : 0);
            }
            case "materials" -> {
                preview.putString("Kind", "materials");
                preview.putString("Item", reward.getString("Item"));
                preview.putInt("Count", reward.getInt("Count"));
            }
            case "blessing" -> {
                preview.putString("Kind", "blessing");
                preview.putInt("Minutes", 20);
            }
            default -> { }
        }
        return preview;
    }

    private static void buyUpgrade(ServerPlayer player, String id) {
        int cost = switch (id) {
            case "danger_sense" -> 40;
            case "previous_luck" -> 60;
            case "family_blessing" -> 50;
            case "continuous_memory" -> 80;
            case "inheritance_expansion" -> 100;
            case "fate_not_end" -> 120;
            case "fate_retention" -> 140;
            default -> -1;
        };
        CompoundTag life = read(player);
        if (cost < 0 || life.getInt(INSIGHT) < cost || hasUpgrade(life, id)) {
            return;
        }
        life.putInt(INSIGHT, life.getInt(INSIGHT) - cost);
        ListTag upgrades = life.getList(UPGRADES, Tag.TAG_STRING);
        upgrades.add(StringTag.valueOf(id));
        life.put(UPGRADES, upgrades);
        save(player, life);
    }

    static boolean matchesCondition(CompoundTag life, String rawCondition) {
        if (rawCondition == null || rawCondition.isBlank()) {
            return true;
        }
        String condition = rawCondition.trim().toLowerCase(Locale.ROOT);
        if (condition.equals("alive")) {
            return life.getBoolean("Alive");
        }
        if (condition.equals("spiritual_root")) {
            return life.getInt("Realm") > 0;
        }
        if (condition.startsWith("flag:")) {
            return hasFlag(life, condition.substring(5));
        }
        if (condition.startsWith("!flag:")) {
            return !hasFlag(life, condition.substring(6));
        }
        if (condition.startsWith("occupation:")) {
            return life.getString("Occupation").equals(condition.substring(11));
        }
        if (condition.startsWith("sect:")) {
            return life.getString("Sect").equals(condition.substring(5));
        }
        String[] operators = {">=", "<=", ">", "<", "="};
        for (String operator : operators) {
            int at = condition.indexOf(operator);
            if (at <= 0) {
                continue;
            }
            String field = condition.substring(0, at);
            int expected;
            try {
                expected = Integer.parseInt(condition.substring(at + operator.length()));
            } catch (NumberFormatException ignored) {
                return false;
            }
            int actual = numericField(life, field);
            return switch (operator) {
                case ">=" -> actual >= expected;
                case "<=" -> actual <= expected;
                case ">" -> actual > expected;
                case "<" -> actual < expected;
                default -> actual == expected;
            };
        }
        return false;
    }

    private static int numericField(CompoundTag life, String field) {
        return switch (field) {
            case "age" -> life.getInt("Age");
            case "health" -> life.getInt("Health");
            case "mentality" -> life.getInt("Mentality");
            case "comprehension" -> life.getInt("Comprehension");
            case "charisma" -> life.getInt("Charisma");
            case "luck" -> life.getInt("Luck");
            case "morality" -> life.getInt("Morality");
            case "reputation" -> life.getInt("Reputation");
            case "realm" -> life.getInt("Realm");
            case "medical" -> life.getInt("Medical");
            case "combat" -> life.getInt("Combat");
            default -> 0;
        };
    }

    static void applyEffect(CompoundTag life, String rawEffect) {
        if (rawEffect == null || rawEffect.isBlank()) {
            return;
        }
        String effect = rawEffect.trim();
        if (effect.startsWith("story_loot:")) {
            LifeStoryLoot.grant(life, effect.substring("story_loot:".length()));
            return;
        }
        if (LifeSimulationWorldMap.applyEventEffect(life, effect)) {
            return;
        }
        if (effect.startsWith("set_flag:")) {
            setFlag(life, effect.substring(9));
            return;
        }
        if (effect.startsWith("reveal_luck")) {
            life.putBoolean("LuckRevealed", true);
            return;
        }
        if (effect.startsWith("occupation:")) {
            life.putString("Occupation", effect.substring(11));
            return;
        }
        if (effect.startsWith("sect:")) {
            life.putString("Sect", effect.substring(5));
            return;
        }
        if (effect.startsWith("friend:")) {
            addGeneratedRelationship(life, effect.substring(7));
            return;
        }
        if (effect.startsWith("spell:")) {
            learnSpell(life, effect.substring(6));
            return;
        }
        if (effect.startsWith("method:")) {
            learnMethod(life, effect.substring(7));
            return;
        }
        if (effect.startsWith("item:")) {
            String payload = effect.substring(5);
            int separator = payload.lastIndexOf(':');
            if (separator > 0) {
                try {
                    addSimulationItem(
                            life,
                            payload.substring(0, separator),
                            Integer.parseInt(payload.substring(separator + 1)));
                } catch (NumberFormatException ignored) {
                    // Malformed optional data-pack rewards are ignored.
                }
            }
            return;
        }
        String[] fields = {
                "health", "mentality", "comprehension", "charisma", "luck",
                "morality", "reputation", "wealth", "cultivation", "medical",
                "alchemy", "combat", "lifespan"
        };
        for (String field : fields) {
            if (!effect.toLowerCase(Locale.ROOT).startsWith(field)) {
                continue;
            }
            String deltaText = effect.substring(field.length());
            int delta;
            try {
                delta = Integer.parseInt(deltaText);
            } catch (NumberFormatException ignored) {
                return;
            }
            applyNumericEffect(life, field, delta);
            return;
        }
    }

    private static int estimateDanger(List<String> effects) {
        int healthLoss = 0;
        for (String effect : effects) {
            if (effect.startsWith("health-")) {
                try {
                    healthLoss += Integer.parseInt(effect.substring(7));
                } catch (NumberFormatException ignored) {
                    // A malformed optional effect is ignored by the estimator.
                }
            }
        }
        if (healthLoss >= 15) {
            return 3;
        }
        if (healthLoss >= 7) {
            return 2;
        }
        return healthLoss > 0 ? 1 : 0;
    }

    private static void applyNumericEffect(
            CompoundTag life, String field, int delta) {
        switch (field) {
            case "health" -> addAttribute(life, "Health", delta);
            case "mentality" -> addAttribute(life, "Mentality", delta);
            case "comprehension" -> addAttribute(life, "Comprehension", delta);
            case "charisma" -> addAttribute(life, "Charisma", delta);
            case "luck" -> addAttribute(life, "Luck", delta);
            case "morality" -> life.putInt("Morality",
                    Mth.clamp(life.getInt("Morality") + delta, -100, 100));
            case "reputation" -> life.putInt("Reputation",
                    Math.max(0, life.getInt("Reputation") + delta));
            case "wealth" -> life.putLong("Wealth",
                    Math.max(0L, life.getLong("Wealth") + delta));
            case "cultivation" -> addCultivationBasisPoints(life, delta);
            case "medical" -> life.putInt("Medical",
                    Math.max(0, life.getInt("Medical") + delta));
            case "alchemy" -> life.putInt("Alchemy",
                    Math.max(0, life.getInt("Alchemy") + delta));
            case "combat" -> life.putInt("Combat",
                    Math.max(0, life.getInt("Combat") + delta));
            case "lifespan" -> life.putInt("MaximumLifespan",
                    Math.max(life.getInt("Age") + 1,
                            life.getInt("MaximumLifespan") + delta));
            default -> {
            }
        }
    }

    private static void maybeAddFriend(CompoundTag life, RandomSource random) {
        if (random.nextFloat() >= 0.28F) {
            return;
        }
        String gender = random.nextBoolean() ? "male" : "female";
        String name = pick(random, SURNAMES)
                + pick(random, "male".equals(gender) ? MALE_NAMES : FEMALE_NAMES);
        ListTag relationships = life.getList(RELATIONSHIPS, Tag.TAG_COMPOUND);
        relationships.add(person(
                name,
                "friend",
                life.getInt("Age") + random.nextInt(-2, 3),
                45,
                random.nextBoolean() ? "cheerful" : "steady",
                "traveler", gender));
        life.put(RELATIONSHIPS, relationships);
    }

    private static void addGeneratedRelationship(
            CompoundTag life, String relation) {
        RandomSource random = yearlyRandom(life);
        String name = pick(random, SURNAMES) + pick(random, MALE_NAMES);
        ListTag relationships = life.getList(RELATIONSHIPS, Tag.TAG_COMPOUND);
        relationships.add(person(
                name,
                relation,
                Math.max(8, life.getInt("Age") + random.nextInt(-4, 8)),
                38,
                "mysterious",
                "cultivator", "male"));
        life.put(RELATIONSHIPS, relationships);
    }

    private static void improveAllRelationships(CompoundTag life, int amount) {
        ListTag relationships = life.getList(RELATIONSHIPS, Tag.TAG_COMPOUND);
        for (int i = 0; i < relationships.size(); i++) {
            CompoundTag person = relationships.getCompound(i);
            if (person.getBoolean("Alive") && !person.getBoolean("Captive")
                    && (!person.contains("Observed") || person.getBoolean("Observed"))) {
                person.putInt("Affinity", Mth.clamp(
                        person.getInt("Affinity") + amount, -100, 100));
            }
        }
        life.put(RELATIONSHIPS, relationships);
    }

    private static boolean hasLivingRelation(
            ListTag relationships, String relation) {
        for (int i = 0; i < relationships.size(); i++) {
            CompoundTag person = relationships.getCompound(i);
            if (person.getBoolean("Alive")
                    && relation.equals(person.getString("Relation"))) {
                return true;
            }
        }
        return false;
    }

    private static int countRelations(
            ListTag relationships, String relation) {
        int count = 0;
        for (int i = 0; i < relationships.size(); i++) {
            if (relation.equals(
                    relationships.getCompound(i).getString("Relation"))) {
                count++;
            }
        }
        return count;
    }

    private static String ending(CompoundTag life, String cause) {
        if ("breakthrough_death".equals(cause)) {
            return "failed_breakthrough";
        }
        if (life.getInt("Morality") <= -70) {
            return "demonic_lord";
        }
        if (life.getInt("HighestRealm") >= 10) {
            return "renowned_cultivator";
        }
        if (life.getInt("Reputation") >= 80) {
            return "world_famous";
        }
        if (life.getLong("Wealth") >= 8_000L) {
            return "merchant_lord";
        }
        if (life.getInt("Age") >= life.getInt("MaximumLifespan") - 2) {
            return "peaceful_old_age";
        }
        return life.getInt("HighestRealm") > 0
                ? "wandering_cultivator" : "ordinary_life";
    }

    private static CultivationLevels.LevelDef cultivationLevel(int realmIndex) {
        List<String> levels = CultivationLevels.allLevels();
        int safeIndex = Mth.clamp(realmIndex, 0, levels.size() - 1);
        return CultivationLevels.getLevelDef(levels.get(safeIndex));
    }

    private static long cultivationRequired(int realmIndex) {
        return Math.max(1L, cultivationLevel(realmIndex).qiNeeded());
    }

    private static boolean isReadyForBreakthrough(CompoundTag life) {
        int realm = life.getInt("Realm");
        return realm > 0
                && realm < CultivationLevels.allLevels().size() - 1
                && life.getLong("CultivationProgress") >= cultivationRequired(realm);
    }

    private static void addCultivationBasisPoints(
            CompoundTag life, int basisPoints) {
        if (life.getInt("Realm") <= 0 || basisPoints == 0) {
            return;
        }
        long required = cultivationRequired(life.getInt("Realm"));
        long magnitude = Math.max(1L, (long) (LifeSimulationBalance.gain(life, 1) * Math.abs((long) basisPoints) / 650.0));
        long delta = basisPoints < 0 ? -magnitude : magnitude;
        life.putLong(
                "CultivationProgress",
                Math.max(0L, Math.min(required,
                        life.getLong("CultivationProgress") + delta)));
    }

    private static void seedFamilyInventory(
            CompoundTag life, String family, RandomSource random) {
        addSimulationItem(life, "minecraft:bread", 2 + random.nextInt(4));
        switch (family) {
            case "poor_herbalists" -> {
                addSimulationItem(life, "minecraft:wheat", 8 + random.nextInt(9));
                addSimulationItem(life, "immortal_cultivation_mod:bigu_pill", 2);
            }
            case "tenant_farmers" -> {
                addSimulationItem(life, "minecraft:wheat", 16 + random.nextInt(17));
                addSimulationItem(life, "immortal_cultivation_mod:nuomi_seed", 4);
            }
            case "village_smiths" -> {
                addSimulationItem(life, "minecraft:iron_ingot", 4 + random.nextInt(5));
                addSimulationItem(life, "minecraft:copper_ingot", 6 + random.nextInt(7));
            }
            case "small_merchants" ->
                    addSimulationItem(life, "immortal_cultivation_mod:copper_coin", 24);
            case "fallen_clan" -> {
                addSimulationItem(life, "immortal_cultivation_mod:spirit_stone", 3);
                addSimulationItem(life, "immortal_cultivation_mod:low_qi_recovery_pill", 1);
            }
            case "wandering_healers" -> {
                addSimulationItem(life, "minecraft:honey_bottle", 3);
                addSimulationItem(life, "immortal_cultivation_mod:low_qi_recovery_pill", 2);
            }
            default -> {
            }
        }
    }

    static void addSimulationItem(
            CompoundTag life, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        ListTag inventory = life.getList(SIMULATION_INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag entry = inventory.getCompound(i);
            if (itemId.equals(entry.getString("Id"))) {
                entry.putInt("Count", Math.min(999_999,
                        entry.getInt("Count") + amount));
                inventory.set(i, entry);
                life.put(SIMULATION_INVENTORY, inventory);
                return;
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", itemId);
        entry.putInt("Count", amount);
        inventory.add(entry);
        life.put(SIMULATION_INVENTORY, inventory);
    }

    private static void normalizeLifeState(CompoundTag life) {
        LifeSimulationBalance.normalize(life);
        LifeSimulationSprites.normalize(life);
        LifeCardBattle.migrateSpellDeck(life);
        CompoundTag savedBattle=life.getCompound("Battle");
        if(!savedBattle.isEmpty()&&!savedBattle.getBoolean("LegacyCompatibility")&&!LifeCardBattle.enabled(savedBattle)&&!LifeSimulationBattle.pending(savedBattle)
                &&savedBattle.getString("Result").isBlank())LifeCardBattle.initialize(life);
        if (!life.contains(SIMULATION_INVENTORY, Tag.TAG_LIST)) {
            life.put(SIMULATION_INVENTORY, new ListTag());
        }
        if (!life.contains(DISCOVERED_SPELLS, Tag.TAG_LIST)) {
            life.put(DISCOVERED_SPELLS, new ListTag());
        }
        if (!life.contains(LEARNED_METHODS, Tag.TAG_LIST)) {
            life.put(LEARNED_METHODS, new ListTag());
        }
        if (!life.contains(ACTIVE_METHOD, Tag.TAG_STRING)) {
            life.putString(ACTIVE_METHOD, "");
        }
        if (!life.contains(QUEST_PROGRESS, Tag.TAG_COMPOUND)) {
            life.put(QUEST_PROGRESS, new CompoundTag());
        }
        if (life.getBoolean("Active") && !life.getBoolean("UsesExactProgress")) {
            long legacy = Math.max(0L, life.getLong("CultivationProgress"));
            long required = cultivationRequired(life.getInt("Realm"));
            life.putLong("CultivationProgress",
                    Math.min(required, required * legacy / 10_000L));
            life.putBoolean("UsesExactProgress", true);
        }
        if (life.getInt("Realm") == 0 || "none".equals(life.getString("SpiritualRoot"))) {
            life.put(DISCOVERED_SPELLS, new ListTag());
        }
        if (life.getInt("Realm") > 0 && life.getString(ACTIVE_METHOD).isBlank()) {
            learnMethod(life, CultivationMethods.BASIC_BREATHING);
            discoverCoreSpells(life, life.getInt("Realm"));
        }
        if (life.getBoolean("Active")) {
            LifeSimulationWorldMap.normalize(life);
        }
        LifeSimulationInheritance.normalize(life);
    }

    private static boolean canAdvance(CompoundTag life) {
        return life.getBoolean("Active")
                && life.getBoolean("Alive")
                && !life.getBoolean("Finished")
                && !life.contains(CURRENT_EVENT, Tag.TAG_COMPOUND)
                && !LifeSimulationBattle.active(life)
                && !life.contains("Story", Tag.TAG_COMPOUND)
                && !LifeSimulationWorldMap.blocksYearAdvance(life);
    }

    private static boolean canAct(CompoundTag life) {
        return canAdvance(life)
                && !LifeSimulationWorldMap.blocksActivities(life)
                && life.getInt("AnnualActionPoints") > 0;
    }

    private static boolean isActiveSystem(ServerPlayer player) {
        return OuterSystemData.equipped(player).isPresent()
                && OuterSystemData.type(player) == OuterSystemType.LIFE_SIMULATION;
    }

    private static CompoundTag read(ServerPlayer player) {
        CompoundTag stored = OuterSystemData.read(player).getCompound(ROOT).copy();
        CompoundTag life = stored.copy();
        LifeSimulationStories.normalizeCastGenders(life, player.server.getResourceManager());
        normalizeLifeState(life);
        LifeSimulationStories.upgradePendingDefinition(life, player.server.getResourceManager());
        if (!stored.getString("Name").isBlank() && !stored.equals(life)) {
            // Legacy NPC identities and pending-story revisions must be stable between
            // the client snapshot and its subsequent UUID action. Retain fresh outer
            // fields, and do not overwrite a life changed by a reentrant observer.
            CompoundTag outer = OuterSystemData.read(player);
            if (outer.getCompound(ROOT).equals(stored)) {
                outer.put(ROOT, life);
                OuterSystemData.save(player, outer);
            }
        }
        return life;
    }

    private static void save(ServerPlayer player, CompoundTag life) {
        CompoundTag outer = OuterSystemData.read(player);
        outer.put(ROOT, life);
        OuterSystemData.save(player, outer);
    }

    private static long currentDay(ServerPlayer player) {
        return player.server.overworld().getDayTime() / 24_000L;
    }

    private static RandomSource yearlyRandom(CompoundTag life) {
        long seed = life.getLong("Seed")
                ^ ((long) life.getInt("Age") << 32)
                ^ life.getList(HISTORY, Tag.TAG_COMPOUND).size() * 0x9E3779B97F4A7C15L
                ^ life.getInt("AnnualActionPoints") * 0xC2B2AE3D27D4EB4FL;
        return RandomSource.create(seed);
    }

    private static int attribute(RandomSource random) {
        return Mth.clamp(
                (random.nextInt(31, 91) + random.nextInt(31, 91)) / 2,
                1,
                100);
    }

    private static int clampAttribute(int value) {
        return Mth.clamp(value, 0, 100);
    }

    private static void addAttribute(
            CompoundTag life, String key, int amount) {
        int minimum = "Health".equals(key) || "Mentality".equals(key) ? 0 : 1;
        life.putInt(key, Mth.clamp(life.getInt(key) + amount, minimum, 100));
    }

    static void addLog(CompoundTag life, String translationKey) {
        ListTag history = life.getList(HISTORY, Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.putInt("Age", life.getInt("Age"));
        entry.putString("Key", translationKey);
        history.add(entry);
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        life.put(HISTORY, history);
    }

    private static void setFlag(CompoundTag life, String flag) {
        ListTag flags = life.getList(FLAGS, Tag.TAG_STRING);
        if (!contains(flags, flag)) {
            flags.add(StringTag.valueOf(flag));
            life.put(FLAGS, flags);
        }
    }

    private static boolean hasFlag(CompoundTag life, String flag) {
        return contains(life.getList(FLAGS, Tag.TAG_STRING), flag);
    }

    private static boolean hasUpgrade(CompoundTag life, String upgrade) {
        return contains(life.getList(UPGRADES, Tag.TAG_STRING), upgrade);
    }

    private static boolean contains(ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private static ListTag stringList(List<String> values) {
        ListTag result = new ListTag();
        for (String value : values) {
            result.add(StringTag.valueOf(value));
        }
        return result;
    }

    private static List<String> readStrings(ListTag values) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            result.add(values.getString(i));
        }
        return result;
    }

    private static <T> T pick(RandomSource random, T[] values) {
        return values[random.nextInt(values.length)];
    }
}
