package com.example.immortal_cultivation_mod.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

/** Data-pack-backed pools for randomly assigned life-simulation quests. */
public final class LifeSimulationQuests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "life_simulation/quests";

    private LifeSimulationQuests() {
    }

    public static Definition select(
            ResourceManager resources,
            Pool pool,
            CompoundTag life,
            RandomSource random,
            String previousQuest) {
        List<Definition> eligible = load(resources).stream()
                .filter(definition -> definition.pool() == pool)
                .filter(definition -> definition.matches(life))
                .toList();
        if (eligible.size() > 1 && previousQuest != null
                && !previousQuest.isBlank()) {
            List<Definition> withoutRepeat = eligible.stream()
                    .filter(definition -> !definition.id().toString()
                            .equals(previousQuest))
                    .toList();
            if (!withoutRepeat.isEmpty()) {
                eligible = withoutRepeat;
            }
        }
        int totalWeight = eligible.stream()
                .mapToInt(Definition::weight)
                .sum();
        if (eligible.isEmpty() || totalWeight <= 0) {
            return null;
        }
        int roll = random.nextInt(totalWeight);
        for (Definition definition : eligible) {
            roll -= definition.weight();
            if (roll < 0) {
                return definition;
            }
        }
        return eligible.getLast();
    }

    public static Definition find(ResourceManager resources, String questId) {
        if (questId == null || questId.isBlank()) {
            return null;
        }
        return load(resources).stream()
                .filter(definition -> definition.id().toString().equals(questId))
                .findFirst()
                .orElse(null);
    }

    private static List<Definition> load(ResourceManager resources) {
        List<Definition> result = new ArrayList<>();
        Map<ResourceLocation, Resource> files = resources.listResources(
                DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray quests = root.getAsJsonArray("quests");
                if (quests == null) {
                    result.add(parse(entry.getKey(), root));
                    continue;
                }
                for (JsonElement element : quests) {
                    result.add(parse(entry.getKey(), element.getAsJsonObject()));
                }
            } catch (Exception exception) {
                LOGGER.error(
                        "Unable to load life simulation quests from {}",
                        entry.getKey(),
                        exception);
            }
        }
        result.sort(Comparator.comparing(definition -> definition.id().toString()));
        return result;
    }

    private static Definition parse(
            ResourceLocation resourceId, JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(
                json, "id", resourceId.toString()));
        Pool pool = Pool.fromId(GsonHelper.getAsString(json, "pool"));
        int minimumRealm = Math.max(0,
                GsonHelper.getAsInt(json, "minimum_realm", 0));
        int maximumRealm = Math.max(
                minimumRealm,
                GsonHelper.getAsInt(json, "maximum_realm", 27));
        int weight = Math.max(1, GsonHelper.getAsInt(json, "weight", 1));
        List<String> conditions = strings(json.getAsJsonArray("conditions"));
        Map<String, Node> nodes = new LinkedHashMap<>();
        JsonArray nodeArray = json.getAsJsonArray("nodes");
        if (nodeArray != null) {
            for (JsonElement element : nodeArray) {
                Node node = parseNode(element.getAsJsonObject());
                if (nodes.put(node.id(), node) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate node " + node.id() + " in quest " + id);
                }
            }
        }
        String startNode = GsonHelper.getAsString(json, "start_node", "start");
        if (nodes.isEmpty()) {
            String textKey = GsonHelper.getAsString(
                    json,
                    "text_key",
                    "life.immortal_cultivation_mod.quest.unknown");
            nodes.put(startNode, new Node(
                    startNode,
                    textKey,
                    parseChoices(json.getAsJsonArray("choices"))));
        }
        if (!nodes.containsKey(startNode)) {
            throw new IllegalArgumentException(
                    "Missing start node " + startNode + " in quest " + id);
        }
        return new Definition(
                id,
                pool,
                minimumRealm,
                maximumRealm,
                weight,
                List.copyOf(conditions),
                startNode,
                Map.copyOf(nodes));
    }

    private static Node parseNode(JsonObject json) {
        String id = GsonHelper.getAsString(json, "id");
        return new Node(
                id,
                GsonHelper.getAsString(
                        json,
                        "text_key",
                        "life.immortal_cultivation_mod.quest.unknown"),
                parseChoices(json.getAsJsonArray("choices")));
    }

    private static List<Choice> parseChoices(JsonArray choiceArray) {
        if (choiceArray == null) {
            return List.of();
        }
        List<Choice> choices = new ArrayList<>();
        for (JsonElement element : choiceArray) {
            JsonObject choice = element.getAsJsonObject();
            choices.add(new Choice(
                    GsonHelper.getAsString(choice, "id"),
                    GsonHelper.getAsString(
                            choice,
                            "text_key",
                            "life.immortal_cultivation_mod.choice.unknown"),
                    GsonHelper.getAsString(
                            choice,
                            "result_key",
                            "life.immortal_cultivation_mod.result.unknown"),
                    GsonHelper.getAsString(choice, "next_node", ""),
                    strings(choice.getAsJsonArray("conditions")),
                    strings(choice.getAsJsonArray("effects"))));
        }
        return List.copyOf(choices);
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    public enum Pool {
        EXPLORE("explore"),
        SECT("sect"),
        LOCATION("location");

        private final String id;

        Pool(String id) {
            this.id = id;
        }

        static Pool fromId(String id) {
            for (Pool pool : values()) {
                if (pool.id.equalsIgnoreCase(id)) {
                    return pool;
                }
            }
            throw new IllegalArgumentException("Unknown quest pool: " + id);
        }
    }

    public record Definition(
            ResourceLocation id,
            Pool pool,
            int minimumRealm,
            int maximumRealm,
            int weight,
            List<String> conditions,
            String startNode,
            Map<String, Node> nodes) {
        private boolean matches(CompoundTag life) {
            int realm = life.getInt("Realm");
            if (realm < minimumRealm || realm > maximumRealm) {
                return false;
            }
            CompoundTag progress = life.getCompound("QuestProgress")
                    .getCompound(id.toString());
            if (progress.getBoolean("Completed")) {
                return false;
            }
            for (String condition : conditions) {
                if (!LifeSimulationSystem.matchesCondition(life, condition)) {
                    return false;
                }
            }
            return true;
        }

        public Node start() {
            return nodes.get(startNode);
        }

        public Node node(String nodeId) {
            return nodes.get(nodeId);
        }
    }

    public record Node(String id, String textKey, List<Choice> choices) {
    }

    public record Choice(
            String id,
            String textKey,
            String resultKey,
            String nextNode,
            List<String> conditions,
            List<String> effects) {
        public boolean available(CompoundTag life) {
            for (String condition : conditions) {
                if (!LifeSimulationSystem.matchesCondition(life, condition)) {
                    return false;
                }
            }
            return true;
        }
    }
}
