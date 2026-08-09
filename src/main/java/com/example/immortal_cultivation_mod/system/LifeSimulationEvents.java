package com.example.immortal_cultivation_mod.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

/** Loads life events from data packs each time a deterministic yearly event is selected. */
public final class LifeSimulationEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "life_simulation/events";

    private LifeSimulationEvents() {
    }

    public static Definition select(
            ResourceManager resources,
            CompoundTag life,
            RandomSource random) {
        List<Definition> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (Definition definition : load(resources)) {
            if (definition.matches(life)) {
                eligible.add(definition);
                totalWeight += definition.weight();
            }
        }
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

    private static List<Definition> load(ResourceManager resources) {
        List<Definition> result = new ArrayList<>();
        Map<ResourceLocation, Resource> files = resources.listResources(
                DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                result.add(parse(entry.getKey(), json));
            } catch (Exception exception) {
                LOGGER.error(
                        "Unable to load life simulation event {}",
                        entry.getKey(),
                        exception);
            }
        }
        return result;
    }

    private static Definition parse(
            ResourceLocation resourceId, JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(
                json,
                "id",
                resourceId.toString()
                        .replace("life_simulation/events/", "life_event/")
                        .replace(".json", "")));
        int minimumAge = GsonHelper.getAsInt(json, "minimum_age", 0);
        int maximumAge = GsonHelper.getAsInt(
                json, "maximum_age", Integer.MAX_VALUE);
        int weight = Math.max(1, GsonHelper.getAsInt(json, "weight", 1));
        String textKey = GsonHelper.getAsString(
                json,
                "text_key",
                "life.immortal_cultivation_mod.event.unknown");
        List<String> conditions = strings(json.getAsJsonArray("conditions"));
        List<String> effects = strings(json.getAsJsonArray("effects"));
        List<Choice> choices = new ArrayList<>();
        JsonArray choiceArray = json.getAsJsonArray("choices");
        if (choiceArray != null) {
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
                                "life.immortal_cultivation_mod.event.unknown.result"),
                        strings(choice.getAsJsonArray("conditions")),
                        strings(choice.getAsJsonArray("effects"))));
            }
        }
        return new Definition(
                id,
                minimumAge,
                maximumAge,
                weight,
                textKey,
                List.copyOf(conditions),
                List.copyOf(effects),
                List.copyOf(choices));
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            result.add(element.getAsString());
        }
        return result;
    }

    public record Definition(
            ResourceLocation id,
            int minimumAge,
            int maximumAge,
            int weight,
            String textKey,
            List<String> conditions,
            List<String> effects,
            List<Choice> choices) {
        private boolean matches(CompoundTag life) {
            int age = life.getInt("Age");
            if (age < minimumAge || age > maximumAge) {
                return false;
            }
            for (String condition : conditions) {
                if (!LifeSimulationSystem.matchesCondition(life, condition)) {
                    return false;
                }
            }
            return true;
        }
    }

    public record Choice(
            String id,
            String textKey,
            String resultKey,
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
