package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class MagicPatternLoader {
   private static final Gson GSON = new Gson();
   private static final Logger LOGGER = LogUtils.getLogger();

   public static double[][] load(ResourceManager manager, ResourceLocation id) {
      try {
         Resource resource = (Resource)manager.getResource(id).orElseThrow();

         double[][] var11;
         try (InputStreamReader reader = new InputStreamReader(resource.open())) {
            Type type = (new TypeToken<List<List<Double>>>() {
            }).getType();
            List<List<Double>> raw = (List<List<Double>>)GSON.fromJson(reader, type);
            double[][] result = new double[raw.size()][2];

            for (int i = 0; i < raw.size(); i++) {
               result[i][0] = raw.get(i).get(0);
               result[i][1] = raw.get(i).get(1);
            }

            var11 = result;
         }

         return var11;
      } catch (Exception var10) {
         LOGGER.warn("Unable to load optional Magic Center visual pattern {}: {}",
               id, var10.getMessage());
         return new double[0][0];
      }
   }
}
