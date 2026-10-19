package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.item.IdentityItems;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.spell.ChangqingJue;
import com.example.immortal_cultivation_mod.spell.HanlanTechniques;
import com.example.immortal_cultivation_mod.spell.HealingTechniques;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.XuanyueSuifengJue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class CenterEffectSupport {
   private static final String THUNDER_SUPPRESSION_UNTIL = "MagicCenterThunderSuppressionUntil";
   private static final String LAST_SPELL_ELEMENT = "MagicCenterLastSpellElement";
   private static final String LAST_SPELL_TIME = "MagicCenterLastSpellTime";
   private static final Map<ServerLevel, Map<Long, ActiveCenter>> ACTIVE_CENTERS = new WeakHashMap();
   private static final ThreadLocal<SpellCastContext> SPELL_CAST = new ThreadLocal();

   private CenterEffectSupport() {
   }

   public static void track(MagicCenterBlockEntity center) {
      Level var2 = center.getLevel();
      if (var2 instanceof ServerLevel level) {
         Map<Long, ActiveCenter> centers = ACTIVE_CENTERS.computeIfAbsent(level, ignored -> new HashMap<>());
         long key = center.getBlockPos().asLong();
         if (center.isActive()) {
            centers.put(key, new ActiveCenter(center.getBlockPos().immutable(), center.getCenterTypeId(), center.getRadius(), level.getGameTime()));
         } else {
            centers.remove(key);
         }

         if (level.getGameTime() % 200L == 0L) {
            centers.values().removeIf(entry -> entry.lastSeen() < level.getGameTime() - 2L);
         }

      }
   }

   public static boolean isInside(LivingEntity entity, ResourceLocation type) {
      return findCenter(entity, type, (center) -> center.isAffectedByCircle(entity)) != null;
   }

   public static boolean isInsideHarmful(LivingEntity entity, ResourceLocation type) {
      return findCenter(entity, type, (center) -> CenterTargetPolicy.receivesHarmfulEffect(center, entity)) != null;
   }

   public static boolean isInsideBeneficial(LivingEntity entity, ResourceLocation type) {
      return findCenter(entity, type, (center) -> CenterTargetPolicy.receivesBeneficialEffect(center, entity)) != null;
   }

   public static List<MagicCenterBlockEntity> centersContaining(Entity entity) {
      return centersContaining(entity, (ResourceLocation)null);
   }

   public static boolean isDangerousPosition(LivingEntity entity, Vec3 position) {
      Level var3 = entity.level();
      if (var3 instanceof ServerLevel level) {
         Map<Long, ActiveCenter> centers = (Map)ACTIVE_CENTERS.get(level);
         if (centers != null && !centers.isEmpty()) {
            Iterator<ActiveCenter> iterator = centers.values().iterator();

            while(iterator.hasNext()) {
               ActiveCenter entry = (ActiveCenter)iterator.next();
               if (entry.lastSeen() < level.getGameTime() - 2L) {
                  iterator.remove();
               } else if (isDangerousType(entry.type()) && !(position.y < (double)entry.pos().getY())) {
                  double dx = position.x - ((double)entry.pos().getX() + (double)0.5F);
                  double dz = position.z - ((double)entry.pos().getZ() + (double)0.5F);
                  if (!(dx * dx + dz * dz > (double)(entry.radius() * entry.radius()))) {
                     BlockEntity var11 = level.getBlockEntity(entry.pos());
                     if (var11 instanceof MagicCenterBlockEntity) {
                        MagicCenterBlockEntity center = (MagicCenterBlockEntity)var11;
                        if (center.isActive() && CenterTargetPolicy.receivesHarmfulEffect(center, entity)) {
                           return true;
                        }
                     }
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isForcefieldRouteBlocked(LivingEntity entity, Vec3 start, Vec3 end) {
      Level var4 = entity.level();
      if (!(var4 instanceof ServerLevel level)) {
         return false;
      } else {
         Map<Long, ActiveCenter> centers = (Map)ACTIVE_CENTERS.get(level);
         if (centers != null && !centers.isEmpty()) {
            Iterator<ActiveCenter> iterator = centers.values().iterator();

            while(iterator.hasNext()) {
               ActiveCenter entry = (ActiveCenter)iterator.next();
               if (entry.lastSeen() >= level.getGameTime() - 2L) {
                  if (MagicCenterTypeRegistry.FORCEFIELD.equals(entry.type())) {
                     BlockEntity var8 = level.getBlockEntity(entry.pos());
                     if (var8 instanceof MagicCenterBlockEntity) {
                        MagicCenterBlockEntity center = (MagicCenterBlockEntity)var8;
                        if (center.isActive() && !CenterTargetPolicy.mayCrossForcefield(center, entity)) {
                           Vec3 centerPoint = Vec3.atCenterOf(entry.pos());
                           double radiusSqr = (double)entry.radius() * (double)entry.radius();
                           boolean startInside = start.distanceToSqr(centerPoint) <= radiusSqr;
                           boolean endInside = end.distanceToSqr(centerPoint) <= radiusSqr;
                           if (startInside != endInside || !startInside && intersectsSphere(start, end, centerPoint, (double)entry.radius())) {
                              return true;
                           }
                        }
                     }
                  }
               } else {
                  iterator.remove();
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   public static boolean isHostileTarget(MagicCenterBlockEntity center, LivingEntity entity) {
      if (entity instanceof Player player) {
         if (center.isOwner(player) || center.isPlayerExcluded(player)) {
            return false;
         }
      }

      return center.isAffectedByCircle(entity);
   }

   public static void beginSpellCast(ServerPlayer player, String spellId) {
      ModSpells.SpellDef spell = ModSpells.get(spellId);
      String element = spell == null ? "" : spell.element();
      SPELL_CAST.set(new SpellCastContext(player.getUUID(), spellId, element));
      player.getPersistentData().putString("MagicCenterLastSpellElement", element);
      player.getPersistentData().putLong("MagicCenterLastSpellTime", player.level().getGameTime());
      if (SpiritRoots.elementMatches(element, "\u706b") && isInside(player, MagicCenterTypeRegistry.FROST)) {
         MobEffectInstance frozenQi = player.getEffect(ModEffects.FROZEN_QI);
         if (frozenQi != null) {
            int remaining = frozenQi.getDuration() - 40;
            int amplifier = frozenQi.getAmplifier();
            player.removeEffect(ModEffects.FROZEN_QI);
            if (remaining > 0) {
               player.addEffect(new MobEffectInstance(ModEffects.FROZEN_QI, remaining, amplifier, false, true, true));
            }
         }
      }

   }

   public static void endSpellCast() {
      SPELL_CAST.remove();
   }

   /** Exact element of a synchronous cast, before its damage source loses spell metadata. */
   public static String currentSpellElement(Entity caster) {
      SpellCastContext cast = SPELL_CAST.get();
      return cast != null && caster != null && cast.playerId().equals(caster.getUUID()) ? cast.element() : "";
   }

   public static boolean hasSpellContext(Entity caster) {
      SpellCastContext cast = SPELL_CAST.get();
      return cast != null && caster != null && cast.playerId().equals(caster.getUUID());
   }

   public static int adjustSpellQiCost(Player player, int cost) {
      SpellCastContext cast = (SpellCastContext)SPELL_CAST.get();
      return cast != null && cast.playerId().equals(player.getUUID()) ? adjustSpellQiCost(player, cost, cast.spellId()) : Math.max(0, cost);
   }

   public static int adjustSpellQiCost(Player player, int cost, String spellId) {
      int adjusted = Math.max(0, cost);
      if (adjusted != 0 && player instanceof ServerPlayer serverPlayer) {
         ModSpells.SpellDef spell = ModSpells.get(spellId);
         String element = spell == null ? "" : spell.element();
         double multiplier = com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.spellCostMultiplier(player, element);
         if ("regeneration".equals(spellId) || HealingTechniques.isTechnique(spellId)) {
            if (isInsideBeneficial(serverPlayer, MagicCenterTypeRegistry.SANCTUARY)) {
               multiplier *= (double)0.75F;
            }

            if (isInsideHarmful(serverPlayer, MagicCenterTypeRegistry.DECAY)) {
               multiplier *= (double)1.25F;
            }
         }

         if (SpiritRoots.elementMatches(element, "\u51b0") && isInsideHarmful(serverPlayer, MagicCenterTypeRegistry.SCORCHING)) {
            multiplier *= 1.15;
         }

         if (isInsideHarmful(serverPlayer, MagicCenterTypeRegistry.FROST)) {
            multiplier *= hasMethodElement(serverPlayer, "\u51b0") ? 1.1 : 1.2;
         }

         ModAttachments.CultivationData data = ModAttachments.getData(serverPlayer);
         if (CultivationMethods.isXuanyueSuifengJue(data.activeCultivationMethod()) && SpiritRoots.elementMatches(element, "\u571f") && XuanyueSuifengJue.isGroundConnected(serverPlayer)) {
            multiplier *= 0.9;
         }

         if (CultivationMethods.isHanlanNingyuanJing(data.activeCultivationMethod()) && SpiritRoots.elementMatches(element, "\u51b0")) {
            multiplier *= (double)HanlanTechniques.qiCostMultiplier(serverPlayer);
         }

         if (CultivationMethods.isChangqingJue(data.activeCultivationMethod()) && SpiritRoots.elementMatches(element, "\u6728")) {
            multiplier *= (double)ChangqingJue.woodQiCostMultiplier(serverPlayer);
         }

         return Math.max(1, (int)Math.ceil((double)adjusted * multiplier));
      } else {
         return adjusted;
      }
   }

   public static int adjustNaturalQiRegen(LivingEntity player, int amount) {
      int adjusted = Math.max(0, amount);
      if (isInsideBeneficial(player, MagicCenterTypeRegistry.SANCTUARY)) {
         adjusted = Math.max(1, adjusted * 125 / 100);
      }

      if (isInsideHarmful(player, MagicCenterTypeRegistry.FROST)) {
         adjusted /= 2;
      }

      if (hasThunderSuppression(player)) {
         adjusted /= 2;
      }

      return Math.max(0, adjusted);
   }

   public static float adjustIncomingDamage(LivingEntity target, DamageSource source, float amount) {
      return !(amount <= 0.0F) && isInsideHarmful(target, MagicCenterTypeRegistry.SCORCHING) && (isRecentSpellElement(source, "\u706b") || source.is(DamageTypeTags.IS_FIRE)) ? amount * 1.15F : amount;
   }

   public static float adjustHealing(LivingEntity entity, float amount) {
      float adjusted = amount;
      if (isInsideHarmful(entity, MagicCenterTypeRegistry.SCORCHING)) {
         adjusted = amount * (hasMethodElement(entity, "\u706b") ? 0.75F : 0.5F);
      }

      if (isInsideHarmful(entity, MagicCenterTypeRegistry.DECAY)) {
         adjusted *= hasBloodOrCorpseAffinity(entity) ? 0.75F : 0.5F;
      }

      return Math.max(0.0F, adjusted);
   }

   public static int maxQi(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         ModAttachments.CultivationData data = ModAttachments.getData(player);
         return Math.max(0, data.maxQi());
      } else if (entity instanceof CultivationNpc npc) {
         return npc.npcMaxQi();
      } else {
         return 0;
      }
   }

   public static int currentQi(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         return ModAttachments.getData(player).qi();
      } else if (entity instanceof CultivationNpc npc) {
         return npc.npcQi();
      } else {
         return 0;
      }
   }

   public static int drainQi(LivingEntity entity, int amount) {
      int requested = Math.max(0, amount);
      if (requested == 0) {
         return 0;
      } else if (entity instanceof ServerPlayer) {
         ServerPlayer player = (ServerPlayer)entity;
         ModAttachments.CultivationData data = ModAttachments.getData(player);
         int drained = Math.min(requested, data.qi());
         ModAttachments.setData(player, data.withQi(data.qi() - drained));
         return drained;
      } else if (entity instanceof CultivationNpc) {
         CultivationNpc npc = (CultivationNpc)entity;
         int drained = Math.min(requested, npc.npcQi());
         NpcProfile.drainQi(npc.asNpcMob(), npc.npcArchetype(), drained);
         return drained;
      } else {
         return 0;
      }
   }

   public static void restoreQi(LivingEntity entity, int amount) {
      int restored = Math.max(0, amount);
      if (restored != 0) {
         if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            ModAttachments.setData(player, data.withQi(Math.min(maxQi(player), data.qi() + restored)));
         } else if (entity instanceof CultivationNpc) {
            CultivationNpc npc = (CultivationNpc)entity;
            NpcProfile.restoreQi(npc.asNpcMob(), npc.npcArchetype(), restored);
         }

      }
   }

   public static boolean hasMethodElement(LivingEntity entity, String element) {
      CultivationMethods.MethodDef method = CultivationMethods.get(methodId(entity));
      if (method == null) {
         return false;
      } else {
         for(String methodElement : method.element().split(",")) {
            if (element.equals(methodElement)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean hasBloodOrCorpseAffinity(LivingEntity entity) {
      String methodId = methodId(entity);
      if (!CultivationMethods.isBloodDemon(methodId) && !CultivationMethods.isJiuyouLianshiJue(methodId)) {
         if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            return "corpse_cultivator".equals(IdentityItems.ensurePlayerIdentity(player));
         } else {
            boolean var10000;
            if (entity instanceof CultivationNpc) {
               CultivationNpc npc = (CultivationNpc)entity;
               if ("corpse_cultivator".equals(npc.npcIdentity())) {
                  var10000 = true;
                  return var10000;
               }
            }

            var10000 = false;
            return var10000;
         }
      } else {
         return true;
      }
   }

   public static void suppressQiRegeneration(LivingEntity entity, int ticks) {
      entity.getPersistentData().putLong("MagicCenterThunderSuppressionUntil", entity.level().getGameTime() + (long)Math.max(1, ticks));
   }

   public static boolean hasThunderSuppression(LivingEntity entity) {
      return entity.getPersistentData().getLong("MagicCenterThunderSuppressionUntil") > entity.level().getGameTime();
   }

   public static String realmName(LivingEntity entity) {
      if (entity instanceof com.example.immortal_cultivation_mod.entity.RealmBeast beast) return beast.beastCultivationLevel();
      if (entity instanceof ServerPlayer player) {
         return ModAttachments.getData(player).cultivationLevel();
      } else if (entity instanceof CultivationNpc npc) {
         return npc.npcRealm();
      } else {
         return "\u51e1\u4eba";
      }
   }

   private static String methodId(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         return ModAttachments.getData(player).activeCultivationMethod();
      } else if (entity instanceof CultivationNpc npc) {
         return NpcProfile.methodId(npc.asNpcMob(), npc.npcArchetype());
      } else {
         return "";
      }
   }

   private static boolean isRecentSpellElement(DamageSource source, String element) {
      Entity attacker = source.getEntity();
      if (!(attacker instanceof ServerPlayer player)) {
         return false;
      } else {
         SpellCastContext cast = (SpellCastContext)SPELL_CAST.get();
         if (cast != null && cast.playerId().equals(player.getUUID()) && SpiritRoots.elementMatches(cast.element(), element)) {
            return true;
         } else if (source.getDirectEntity() == attacker) {
            return false;
         } else {
            return SpiritRoots.elementMatches(player.getPersistentData().getString("MagicCenterLastSpellElement"), element) && player.level().getGameTime() - player.getPersistentData().getLong("MagicCenterLastSpellTime") <= 100L;
         }
      }
   }

   private static MagicCenterBlockEntity findCenter(LivingEntity entity, ResourceLocation type, Predicate<MagicCenterBlockEntity> predicate) {
      for(MagicCenterBlockEntity center : centersContaining(entity, type)) {
         if (predicate.test(center)) {
            return center;
         }
      }

      return null;
   }

   private static List<MagicCenterBlockEntity> centersContaining(Entity entity, ResourceLocation requiredType) {
      Level var3 = entity.level();
      if (!(var3 instanceof ServerLevel level)) {
         return List.of();
      } else {
         Map<Long, ActiveCenter> centers = (Map)ACTIVE_CENTERS.get(level);
         if (centers != null && !centers.isEmpty()) {
            List<MagicCenterBlockEntity> matches = new ArrayList();
            Iterator<ActiveCenter> iterator = centers.values().iterator();

            while(iterator.hasNext()) {
               ActiveCenter entry = (ActiveCenter)iterator.next();
               if (entry.lastSeen() < level.getGameTime() - 2L) {
                  iterator.remove();
               } else {
                  double dx = entity.getX() - ((double)entry.pos().getX() + (double)0.5F);
                  double dz = entity.getZ() - ((double)entry.pos().getZ() + (double)0.5F);
                  if ((requiredType == null || requiredType.equals(entry.type())) && !(entity.getY() < (double)entry.pos().getY()) && !(dx * dx + dz * dz > (double)(entry.radius() * entry.radius()))) {
                     BlockEntity var12 = level.getBlockEntity(entry.pos());
                     if (var12 instanceof MagicCenterBlockEntity) {
                        MagicCenterBlockEntity center = (MagicCenterBlockEntity)var12;
                        if (center.isActive()) {
                           matches.add(center);
                        }
                     }
                  }
               }
            }

            return matches;
         } else {
            return List.of();
         }
      }
   }

   private static boolean isDangerousType(ResourceLocation type) {
      return MagicCenterTypeRegistry.SCORCHING.equals(type) || MagicCenterTypeRegistry.FROST.equals(type) || MagicCenterTypeRegistry.DECAY.equals(type) || MagicCenterTypeRegistry.THUNDER.equals(type);
   }

   private static boolean intersectsSphere(Vec3 start, Vec3 end, Vec3 center, double radius) {
      Vec3 segment = end.subtract(start);
      double lengthSquared = segment.lengthSqr();
      if (lengthSquared < 1.0E-7) {
         return start.distanceToSqr(center) <= radius * radius;
      } else {
         double t = Math.max((double)0.0F, Math.min((double)1.0F, center.subtract(start).dot(segment) / lengthSquared));
         return start.add(segment.scale(t)).distanceToSqr(center) <= radius * radius;
      }
   }

   private static record ActiveCenter(BlockPos pos, ResourceLocation type, int radius, long lastSeen) {
   }

   private static record SpellCastContext(UUID playerId, String spellId, String element) {
   }
}
