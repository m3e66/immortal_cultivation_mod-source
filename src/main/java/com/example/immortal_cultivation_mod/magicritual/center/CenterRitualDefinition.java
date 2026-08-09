package com.example.immortal_cultivation_mod.magicritual.center;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record CenterRitualDefinition(
      ResourceLocation id,
      ResourceLocation output,
      int priority,
      boolean allowRotations,
      boolean allowMirror,
      List<Requirement> pattern
) {
   public CenterRitualDefinition {
      pattern = List.copyOf(pattern);
   }

   public record Requirement(
         BlockPos offset,
         ResourceLocation block,
         Map<String, String> properties
   ) {
      public Requirement {
         properties = Map.copyOf(properties);
      }
   }
}
