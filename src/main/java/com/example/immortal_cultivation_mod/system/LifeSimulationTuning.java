package com.example.immortal_cultivation_mod.system;

import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Data-pack balance defaults are validated and replaced atomically on server reload. */
@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class LifeSimulationTuning implements PreparableReloadListener {
    private static final Map<String,Double> DEFAULTS = Map.ofEntries(
            Map.entry("raw_training",25.0), Map.entry("method_growth",6.0), Map.entry("earth_method",2.5), Map.entry("heaven_method",4.0),
            Map.entry("passive_training",.15),Map.entry("breathing_training",.45),Map.entry("seclusion_training",1.7),Map.entry("cultivate_training",1.25),
            Map.entry("realm_score",1800.0),Map.entry("story_score",1500.0),Map.entry("danger_score",900.0),
            Map.entry("insight_divisor",250.0),Map.entry("insight_cap",400.0),Map.entry("crisis_chance",.18));
    private static volatile Map<String,Double> values=DEFAULTS;
    private static final LifeSimulationTuning INSTANCE=new LifeSimulationTuning();
    private LifeSimulationTuning() {}
    public static double get(String key) { return values.getOrDefault(key,DEFAULTS.getOrDefault(key,0.0)); }
    @SubscribeEvent public static void reloadListener(AddReloadListenerEvent event) { event.addListener(INSTANCE); }
    @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparationProfiler,
            ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> load(manager),backgroundExecutor)
                .thenCompose(barrier::wait).thenAcceptAsync(loaded -> values=loaded,gameExecutor);
    }
    private static Map<String,Double> load(ResourceManager manager) {
        Map<String,Double> loaded=new HashMap<>(DEFAULTS);
        var resource=manager.getResource(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","life_simulation/tuning.json"));
        if(resource.isEmpty())return Map.copyOf(loaded);
        try(var reader=resource.get().openAsReader()) {
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            for(String key:DEFAULTS.keySet())if(json.has(key)) {
                double value=json.get(key).getAsDouble();
                double limit=key.equals("crisis_chance")?1:key.equals("method_growth")?10:1_000_000;
                double minimum=key.equals("insight_divisor")?1:0;
                if(!Double.isFinite(value)||value<minimum||value>limit)throw new IllegalArgumentException("Invalid life tuning: "+key);
                loaded.put(key,value);
            }
        } catch(Exception error) { com.mojang.logging.LogUtils.getLogger().error("Invalid life simulation tuning; retaining defaults",error);return DEFAULTS; }
        return Map.copyOf(loaded);
    }
}
