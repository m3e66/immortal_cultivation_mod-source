package com.example.immortal_cultivation_mod.magicritual.center;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class CenterRitualManager implements PreparableReloadListener {
   public static final CenterRitualManager INSTANCE = new CenterRitualManager();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();
   private static final String DIRECTORY = "center_rituals";
   private volatile List<CenterRitualDefinition> definitions = List.of();

   private CenterRitualManager() {
   }

   public List<CenterRitualDefinition> definitions() {
      return this.definitions;
   }

   public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
      CompletableFuture<List<CenterRitualDefinition>> loadedFuture = CompletableFuture.supplyAsync(() -> loadDefinitions(manager), backgroundExecutor);
      Objects.requireNonNull(barrier);
      return loadedFuture.thenCompose(barrier::wait).thenAcceptAsync((loaded) -> this.definitions = loaded, gameExecutor);
   }

   private static List<CenterRitualDefinition> loadDefinitions(ResourceManager manager) {
      List<CenterRitualDefinition> loaded = new ArrayList();
      Map<ResourceLocation, Resource> resources = manager.listResources("center_rituals", (id) -> id.getPath().endsWith(".json"));

      for(Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
         try {
            InputStreamReader reader = new InputStreamReader(((Resource)entry.getValue()).open(), StandardCharsets.UTF_8);

            try {
               CenterRitualDefinition definition = parseDefinition((ResourceLocation)entry.getKey(), JsonParser.parseReader(reader).getAsJsonObject());
               loaded.add(definition);
            } catch (Throwable var9) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            reader.close();
         } catch (Exception exception) {
            LOGGER.error("Disabling invalid Magic Center ritual {}: {}", entry.getKey(), exception.getMessage());
         }
      }

      loaded.sort(Comparator.comparingInt(CenterRitualDefinition::priority).reversed().thenComparing(Comparator.comparingInt((CenterRitualDefinition definitionx) -> definitionx.pattern().size()).reversed()).thenComparing((definitionx) -> definitionx.id().toString()));
      LOGGER.info("Loaded {} Magic Center ritual definitions", loaded.size());
      return List.copyOf(loaded);
   }

   private static CenterRitualDefinition parseDefinition(ResourceLocation resourceId, JsonObject root) {
      ResourceLocation ritualId = definitionId(resourceId);
      ResourceLocation output = requiredId(root, "output");
      if (MagicCenterTypeRegistry.contains(output) && !MagicCenterTypeRegistry.BLANK.equals(output)) {
         int priority = root.has("priority") ? root.get("priority").getAsInt() : 0;
         boolean rotations = !root.has("allow_rotations") || root.get("allow_rotations").getAsBoolean();
         boolean mirror = root.has("allow_mirror") && root.get("allow_mirror").getAsBoolean();
         JsonArray patternJson = root.getAsJsonArray("pattern");
         if (patternJson != null && !patternJson.isEmpty()) {
            List<CenterRitualDefinition.Requirement> pattern = new ArrayList();

            for(JsonElement element : patternJson) {
               JsonObject requirement = element.getAsJsonObject();
               BlockPos offset = new BlockPos(requirement.get("x").getAsInt(), requirement.get("y").getAsInt(), requirement.get("z").getAsInt());
               ResourceLocation block = requiredId(requirement, "block");
               Map<String, String> properties = new LinkedHashMap();
               if (requirement.has("properties")) {
                  for(Map.Entry<String, JsonElement> property : requirement.getAsJsonObject("properties").entrySet()) {
                     properties.put((String)property.getKey(), ((JsonElement)property.getValue()).getAsString());
                  }
               }

               pattern.add(new CenterRitualDefinition.Requirement(offset, block, properties));
            }

            long uniqueOffsets = pattern.stream().map(CenterRitualDefinition.Requirement::offset).distinct().count();
            if (uniqueOffsets != (long)pattern.size()) {
               throw new IllegalArgumentException("Pattern contains duplicate offsets");
            } else {
               return new CenterRitualDefinition(ritualId, output, priority, rotations, mirror, pattern);
            }
         } else {
            throw new IllegalArgumentException("Pattern must contain at least one block");
         }
      } else {
         throw new IllegalArgumentException("Unknown or invalid output center type " + String.valueOf(output));
      }
   }

   private static ResourceLocation definitionId(ResourceLocation resourceId) {
      String path = resourceId.getPath();
      path = path.substring("center_rituals".length() + 1, path.length() - ".json".length());
      return ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), path);
   }

   private static ResourceLocation requiredId(JsonObject root, String key) {
      if (!root.has(key)) {
         throw new IllegalArgumentException("Missing " + key);
      } else {
         ResourceLocation id = ResourceLocation.tryParse(root.get(key).getAsString());
         if (id == null) {
            throw new IllegalArgumentException("Invalid resource location for " + key);
         } else {
            return id;
         }
      }
   }
}
