package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.center.behavior.BlankCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.DecayCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.DetectionCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.FogCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.FrostCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.GrowthCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.InvertedCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ReforgeBladeCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.SanctuaryCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ScorchingCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.TeleportCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ThunderCenterBehavior;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class MagicCenterTypeRegistry {
   public static final ResourceLocation BLANK = id("blank");
   public static final ResourceLocation FOG = id("fog");
   public static final ResourceLocation FORCEFIELD = id("forcefield");
   public static final ResourceLocation INVERTED = id("inverted");
   public static final ResourceLocation SCORCHING = id("scorching");
   public static final ResourceLocation SANCTUARY = id("sanctuary");
   public static final ResourceLocation GROWTH = id("growth");
   public static final ResourceLocation FROST = id("frost");
   public static final ResourceLocation DECAY = id("decay");
   public static final ResourceLocation THUNDER = id("thunder");
   public static final ResourceLocation DETECTION = id("detection");
   public static final ResourceLocation REFORGE_BLADE = id("reforge_blade");
   public static final ResourceLocation TELEPORT = id("teleport");
   private static final Map<ResourceLocation, MagicCenterType> TYPES = new LinkedHashMap();

   private MagicCenterTypeRegistry() {
   }

   private static void register(ResourceLocation id, String visualPattern, MagicCenterBehavior behavior) {
      String path = id.getPath();
      TYPES.put(id, new MagicCenterType(id, "center_type.immortal_cultivation_mod." + path, "center_type.immortal_cultivation_mod." + path + ".description", visualPattern, behavior));
   }

   public static Optional<MagicCenterType> get(ResourceLocation id) {
      return Optional.ofNullable((MagicCenterType)TYPES.get(id));
   }

   public static MagicCenterType getOrBlank(ResourceLocation id) {
      return (MagicCenterType)TYPES.getOrDefault(id, (MagicCenterType)TYPES.get(BLANK));
   }

   public static boolean contains(ResourceLocation id) {
      return TYPES.containsKey(id);
   }

   public static Collection<MagicCenterType> values() {
      return TYPES.values();
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
   }

   static {
      register(BLANK, "normal", new BlankCenterBehavior());
      register(FOG, "fog", new FogCenterBehavior());
      register(FORCEFIELD, "forcefield", new ForcefieldCenterBehavior());
      register(INVERTED, "inverted", new InvertedCenterBehavior());
      register(SCORCHING, "normal", new ScorchingCenterBehavior());
      register(SANCTUARY, "normal", new SanctuaryCenterBehavior());
      register(GROWTH, "normal", new GrowthCenterBehavior());
      register(FROST, "normal", new FrostCenterBehavior());
      register(DECAY, "normal", new DecayCenterBehavior());
      register(THUNDER, "normal", new ThunderCenterBehavior());
      register(DETECTION, "normal", new DetectionCenterBehavior());
      register(REFORGE_BLADE, "normal", new ReforgeBladeCenterBehavior());
      register(TELEPORT, "normal", new TeleportCenterBehavior());
   }
}
