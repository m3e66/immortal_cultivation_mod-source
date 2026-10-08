package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public final class TeleportCodeRegistry extends SavedData {
   private static final String DATA_NAME = "immortal_cultivation_teleport_codes";
   private static final SavedData.Factory<TeleportCodeRegistry> FACTORY = new SavedData.Factory<>(TeleportCodeRegistry::new, TeleportCodeRegistry::load);
   private static final String PREFIX = "TP-";
   private static final int HEX_LENGTH = 32;
   private final Map<String, TargetRef> targets = new HashMap();

   private static TeleportCodeRegistry get(MinecraftServer server) {
      return (TeleportCodeRegistry)server.overworld().getDataStorage().computeIfAbsent(FACTORY, "immortal_cultivation_teleport_codes");
   }

   private static TeleportCodeRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
      TeleportCodeRegistry registry = new TeleportCodeRegistry();
      ListTag entries = tag.getList("Entries", 10);

      for(int index = 0; index < entries.size(); ++index) {
         CompoundTag entry = entries.getCompound(index);
         String code = normalize(entry.getString("Code"));
         ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
         if (isValid(code) && dimension != null && entry.contains("Pos", 4) && entry.hasUUID("CenterId")) {
            registry.targets.put(code, new TargetRef(dimension, BlockPos.of(entry.getLong("Pos")), entry.getUUID("CenterId"), entry.getString("Label")));
         }
      }

      return registry;
   }

   public static String codeFor(UUID centerId) {
      String hex = centerId.toString().replace("-", "").toUpperCase(Locale.ROOT);
      String var10000 = hex.substring(0, 8);
      return "TP-" + var10000 + "-" + hex.substring(8, 16) + "-" + hex.substring(16, 24) + "-" + hex.substring(24, 32);
   }

   public static String normalize(String rawCode) {
      if (rawCode == null) {
         return "";
      } else {
         String value = rawCode.trim().toUpperCase(Locale.ROOT).replace(" ", "");
         if (value.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
            value = value.replace("-", "");
         } else if (value.startsWith("TP-")) {
            value = value.substring("TP-".length()).replace("-", "");
         }

         if (!value.matches("[0-9A-F]{32}")) {
            return "";
         } else {
            String var10000 = value.substring(0, 8);
            return "TP-" + var10000 + "-" + value.substring(8, 16) + "-" + value.substring(16, 24) + "-" + value.substring(24, 32);
         }
      }
   }

   public static boolean isValid(String code) {
      return code != null && !code.isEmpty() && code.equals(normalize(code));
   }

   public static void register(MagicCenterBlockEntity center) {
      Level var2 = center.getLevel();
      if (var2 instanceof ServerLevel level) {
         if (MagicCenterTypeRegistry.TELEPORT.equals(center.getCenterTypeId())) {
            String code = codeFor(center.getCenterId());
            TargetRef next = new TargetRef(level.dimension().location(), center.getBlockPos().immutable(), center.getCenterId(), center.getDisplayName().getString());
            TeleportCodeRegistry registry = get(level.getServer());
            if (!next.equals(registry.targets.put(code, next))) {
               registry.setDirty();
            }

            return;
         }
      }

   }

   public static void unregister(MagicCenterBlockEntity center) {
      Level var2 = center.getLevel();
      if (var2 instanceof ServerLevel level) {
         String var5 = codeFor(center.getCenterId());
         TeleportCodeRegistry registry = get(level.getServer());
         TargetRef current = (TargetRef)registry.targets.get(var5);
         if (current != null && current.centerId().equals(center.getCenterId()) && current.dimension().equals(level.dimension().location()) && current.pos().equals(center.getBlockPos())) {
            registry.targets.remove(var5);
            registry.setDirty();
         }

      }
   }

   public static @Nullable TargetRef lookup(MinecraftServer server, String rawCode) {
      String code = normalize(rawCode);
      return code.isEmpty() ? null : (TargetRef)get(server).targets.get(code);
   }

   public static @Nullable ResolvedTarget resolve(MinecraftServer server, String rawCode) {
      String code = normalize(rawCode);
      if (code.isEmpty()) {
         return null;
      } else {
         TeleportCodeRegistry registry = get(server);
         TargetRef ref = (TargetRef)registry.targets.get(code);
         if (ref == null) {
            return null;
         } else {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ref.dimension()));
            if (level == null) {
               return null;
            } else {
               level.getChunkAt(ref.pos());
               BlockEntity var7 = level.getBlockEntity(ref.pos());
               if (var7 instanceof MagicCenterBlockEntity) {
                  MagicCenterBlockEntity center = (MagicCenterBlockEntity)var7;
                  if (MagicCenterTypeRegistry.TELEPORT.equals(center.getCenterTypeId()) && ref.centerId().equals(center.getCenterId()) && code.equals(codeFor(center.getCenterId()))) {
                     register(center);
                     return new ResolvedTarget(level, center);
                  }
               }

               if (ref.equals(registry.targets.remove(code))) {
                  registry.setDirty();
               }

               return null;
            }
         }
      }
   }

   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
      ListTag entries = new ListTag();
      this.targets.forEach((code, ref) -> {
         CompoundTag entry = new CompoundTag();
         entry.putString("Code", code);
         entry.putString("Dimension", ref.dimension().toString());
         entry.putLong("Pos", ref.pos().asLong());
         entry.putUUID("CenterId", ref.centerId());
         entry.putString("Label", ref.label());
         entries.add(entry);
      });
      tag.put("Entries", entries);
      return tag;
   }

   public static record TargetRef(ResourceLocation dimension, BlockPos pos, UUID centerId, String label) {
   }

   public static record ResolvedTarget(ServerLevel level, MagicCenterBlockEntity center) {
   }
}
