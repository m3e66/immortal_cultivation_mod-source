package com.example.immortal_cultivation_mod.externalpower;

import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

/** Reloads atomically; a malformed edit keeps the last valid set rather than half applying it. */
public final class ExternalPowerOverrides {
    public record Override(int powerTier, Integer penetrationTier) {}
    private static Map<ResourceLocation,Override> entries=Map.of();
    private static long checkedAt=Long.MIN_VALUE, modified=Long.MIN_VALUE;
    public static Path path(){return FMLPaths.CONFIGDIR.get().resolve("external_power_overrides.json");}
    public static Map<ResourceLocation,Override> parse(Reader reader) {
        var result=new HashMap<ResourceLocation,Override>();
        var root=JsonParser.parseReader(reader).getAsJsonObject();
        if(root.size()>4096)throw new IllegalArgumentException("Too many override entries (maximum 4096)");
        for(var entry:root.entrySet()) {
            var id=ResourceLocation.parse(entry.getKey());var value=entry.getValue().getAsJsonObject();
            int power=tier(value.get("powerTier"));
            Integer penetration=value.has("penetrationTier")?tier(value.get("penetrationTier")):null;
            result.put(id,new Override(power,penetration));
        }
        return Map.copyOf(result);
    }
    private static int tier(com.google.gson.JsonElement value) {
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Tier must be an integer");
        double number=value.getAsDouble();
        if(!Double.isFinite(number)||number<0||number>128||number!=Math.rint(number))throw new IllegalArgumentException("Tier must be an integer from 0 to 128");
        return (int)number;
    }
    public static void reload(boolean force) {
        long now=System.nanoTime();
        if(!force && checkedAt!=Long.MIN_VALUE && now-checkedAt<5_000_000_000L)return;
        checkedAt=now;
        try {
            Path path=path();
            if(!Files.exists(path)) {Files.createDirectories(path.getParent());Files.writeString(path,"{}\n",StandardCharsets.UTF_8);}
            long stamp=Files.getLastModifiedTime(path).toMillis();
            if(!force&&stamp==modified)return;
            modified=stamp;
            if(Files.size(path)>1024*1024)throw new IllegalArgumentException("Override file exceeds 1 MiB");
            try(var reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){entries=parse(reader);}
        } catch(Exception error) {ExternalPowerSystem.LOG.warn("External power overrides unchanged: {}",error.toString());}
    }
    public static Override get(ResourceLocation type){return entries.get(type);}
    public static void clear(){entries=Map.of();checkedAt=modified=Long.MIN_VALUE;}
}
