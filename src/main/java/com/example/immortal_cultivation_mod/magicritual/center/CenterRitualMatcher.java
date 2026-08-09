package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class CenterRitualMatcher {
   private CenterRitualMatcher() {
   }

   public static Result match(ServerLevel level, BlockPos origin,
                              List<CenterRitualDefinition> definitions) {
      List<Match> matches = new ArrayList<>();
      Diagnostic bestDiagnostic = null;
      for (CenterRitualDefinition definition : definitions) {
         int rotations = definition.allowRotations() ? 4 : 1;
         int mirrors = definition.allowMirror() ? 2 : 1;
         for (int mirror = 0; mirror < mirrors; mirror++) {
            for (int rotation = 0; rotation < rotations; rotation++) {
               Validation validation = validate(
                     level, origin, definition, rotation, mirror == 1);
               if (validation.unloaded()) {
                  return Result.unloaded(validation.errors());
               }
               if (validation.errors().isEmpty()) {
                  matches.add(new Match(definition, rotation, mirror == 1,
                        validation.consumedPositions()));
               } else if (bestDiagnostic == null
                     || validation.errors().size() < bestDiagnostic.positions().size()) {
                  bestDiagnostic = new Diagnostic(definition, validation.errors());
               }
            }
         }
      }
      if (matches.isEmpty()) {
         return Result.none(bestDiagnostic == null ? List.of() : bestDiagnostic.positions());
      }
      matches.sort(Comparator.comparingInt(
                  (Match match) -> match.definition().priority()).reversed()
            .thenComparing(Comparator.comparingInt(
                  (Match match) -> match.definition().pattern().size()).reversed()));
      int topPriority = matches.getFirst().definition().priority();
      int topSize = matches.getFirst().definition().pattern().size();
      Map<ResourceLocation, Match> topOutputs = new LinkedHashMap<>();
      for (Match match : matches) {
         if (match.definition().priority() != topPriority
               || match.definition().pattern().size() != topSize) {
            continue;
         }
         topOutputs.putIfAbsent(match.definition().output(), match);
      }
      if (topOutputs.size() != 1) {
         return Result.ambiguous();
      }
      return Result.success(topOutputs.values().iterator().next());
   }

   private static Validation validate(ServerLevel level, BlockPos origin,
                                      CenterRitualDefinition definition,
                                      int rotation, boolean mirrored) {
      Map<BlockPos, CenterRitualDefinition.Requirement> expected = new HashMap<>();
      List<BlockPos> errors = new ArrayList<>();
      boolean unloaded = false;
      for (CenterRitualDefinition.Requirement requirement : definition.pattern()) {
         BlockPos offset = transform(requirement.offset(), rotation, mirrored);
         BlockPos absolute = origin.offset(offset);
         expected.put(offset, requirement);
         if (!level.hasChunkAt(absolute)) {
            unloaded = true;
            addError(errors, absolute);
            continue;
         }
         if (!matchesRequirement(level.getBlockState(absolute), requirement)) {
            addError(errors, absolute);
         }
      }

      Bounds bounds = Bounds.of(expected.keySet());
      for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
         for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
               BlockPos offset = new BlockPos(x, y, z);
               if (expected.containsKey(offset)) {
                  continue;
               }
               BlockPos absolute = origin.offset(offset);
               if (!level.hasChunkAt(absolute)) {
                  unloaded = true;
                  addError(errors, absolute);
               } else if (level.getBlockState(absolute).is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                  addError(errors, absolute);
               }
            }
         }
      }
      List<BlockPos> consumed = expected.keySet().stream().map(origin::offset).toList();
      return new Validation(errors, consumed, unloaded);
   }

   private static boolean matchesRequirement(BlockState state,
                                             CenterRitualDefinition.Requirement requirement) {
      ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
      if (!requirement.block().equals(blockId)) {
         return false;
      }
      for (Map.Entry<String, String> expected : requirement.properties().entrySet()) {
         Property<?> property = state.getProperties().stream()
               .filter(candidate -> candidate.getName().equals(expected.getKey()))
               .findFirst().orElse(null);
         if (property == null || !expected.getValue().equals(propertyValue(state, property))) {
            return false;
         }
      }
      return true;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static String propertyValue(BlockState state, Property property) {
      return property.getName(state.getValue(property));
   }

   private static BlockPos transform(BlockPos offset, int rotation, boolean mirrored) {
      int x = mirrored ? -offset.getX() : offset.getX();
      int z = offset.getZ();
      return switch (rotation & 3) {
         case 1 -> new BlockPos(-z, offset.getY(), x);
         case 2 -> new BlockPos(-x, offset.getY(), -z);
         case 3 -> new BlockPos(z, offset.getY(), -x);
         default -> new BlockPos(x, offset.getY(), z);
      };
   }

   private static void addError(List<BlockPos> errors, BlockPos pos) {
      if (errors.size() < 8 && !errors.contains(pos)) {
         errors.add(pos.immutable());
      }
   }

   public enum Status {
      SUCCESS,
      NONE,
      AMBIGUOUS,
      UNLOADED
   }

   public record Result(Status status, Match match, List<BlockPos> errorPositions) {
      private static Result success(Match match) {
         return new Result(Status.SUCCESS, match, List.of());
      }

      private static Result none(List<BlockPos> errors) {
         return new Result(Status.NONE, null, List.copyOf(errors));
      }

      private static Result ambiguous() {
         return new Result(Status.AMBIGUOUS, null, List.of());
      }

      private static Result unloaded(List<BlockPos> errors) {
         return new Result(Status.UNLOADED, null, List.copyOf(errors));
      }
   }

   public record Match(CenterRitualDefinition definition, int rotation,
                       boolean mirrored, List<BlockPos> consumedPositions) {
   }

   private record Validation(List<BlockPos> errors, List<BlockPos> consumedPositions,
                             boolean unloaded) {
   }

   private record Diagnostic(CenterRitualDefinition definition, List<BlockPos> positions) {
   }

   private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
      private static Bounds of(Set<BlockPos> positions) {
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;
         for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
         }
         return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
      }
   }
}
