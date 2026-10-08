package com.example.immortal_cultivation_mod.magicritual.center;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record CenterRitualDefinition(ResourceLocation id, ResourceLocation output, int priority, boolean allowRotations, boolean allowMirror, List<Requirement> pattern) {
   public CenterRitualDefinition(ResourceLocation id, ResourceLocation output, int priority, boolean allowRotations, boolean allowMirror, List<Requirement> pattern) {
      pattern = List.copyOf(pattern);
      this.id = id;
      this.output = output;
      this.priority = priority;
      this.allowRotations = allowRotations;
      this.allowMirror = allowMirror;
      this.pattern = pattern;
   }

   public static record Requirement(BlockPos offset, ResourceLocation block, Map<String, String> properties) {
      public Requirement(BlockPos offset, ResourceLocation block, Map<String, String> properties) {
         properties = Map.copyOf(properties);
         this.offset = offset;
         this.block = block;
         this.properties = properties;
      }
   }
}
