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
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.phys.Vec3;

public final class CenterEffectSupport {
   private static final String THUNDER_SUPPRESSION_UNTIL = "MagicCenterThunderSuppressionUntil";
   private static final String LAST_SPELL_ELEMENT = "MagicCenterLastSpellElement";
   private static final String LAST_SPELL_TIME = "MagicCenterLastSpellTime";
   private static final Map<ServerLevel, Map<Long, ActiveCenter>> ACTIVE_CENTERS = new WeakHashMap<>();
   private static final ThreadLocal<SpellCastContext> SPELL_CAST = new ThreadLocal<>();

   private CenterEffectSupport() {
   }

   public static void track(MagicCenterBlockEntity center) {
      if (!(center.getLevel() instanceof ServerLevel level)) {
         return;
      }
      Map<Long, ActiveCenter> centers = ACTIVE_CENTERS.computeIfAbsent(level, ignored -> new HashMap<>());
      long key = center.getBlockPos().asLong();
      if (center.isActive()) {
         centers.put(key, new ActiveCenter(center.getBlockPos().immutable(), center.getCenterTypeId(),
               center.getRadius(), level.getGameTime()));
      } else {
         centers.remove(key);
      }
      if (level.getGameTime() % 200L == 0L) {
         centers.values().removeIf(entry -> entry.lastSeen() < level.getGameTime() - 2L);
      }
   }

   public static boolean isInside(LivingEntity entity, ResourceLocation type) {
      return findCenter(entity, type, center -> center.isAffectedByCircle(entity)) != null;
   }

   public static boolean isInsideHarmful(LivingEntity entity, ResourceLocation type) {
      return findCenter(entity, type,
            center -> CenterTargetPolicy.receivesHarmfulEffect(center, entity)) != null;
   }

   public static boolean isInsideBeneficial(LivingEntity entity, ResourceLocation type) {
      return findCenter(entity, type,
            center -> CenterTargetPolicy.receivesBeneficialEffect(center, entity)) != null;
   }

   public static List<MagicCenterBlockEntity> centersContaining(Entity entity) {
      return centersContaining(entity, null);
   }

   /** Candidate-position query used by NPC path selection before entering a harmful formation. */
   public static boolean isDangerousPosition(LivingEntity entity, Vec3 position) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }
      Map<Long, ActiveCenter> centers = ACTIVE_CENTERS.get(level);
      if (centers == null || centers.isEmpty()) {
         return false;
      }
      Iterator<ActiveCenter> iterator = centers.values().iterator();
      while (iterator.hasNext()) {
         ActiveCenter entry = iterator.next();
         if (entry.lastSeen() < level.getGameTime() - 2L) {
            iterator.remove();
            continue;
         }
         if (!isDangerousType(entry.type()) || position.y < entry.pos().getY()) {
            continue;
         }
         double dx = position.x - (entry.pos().getX() + 0.5D);
         double dz = position.z - (entry.pos().getZ() + 0.5D);
         if (dx * dx + dz * dz > entry.radius() * entry.radius()) {
            continue;
         }
         if (level.getBlockEntity(entry.pos()) instanceof MagicCenterBlockEntity center
               && center.isActive()
               && CenterTargetPolicy.receivesHarmfulEffect(center, entity)) {
            return true;
         }
      }
      return false;
   }

   /** Rejects routes that the live force-field behavior would bounce back. */
   public static boolean isForcefieldRouteBlocked(LivingEntity entity, Vec3 start, Vec3 end) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }
      Map<Long, ActiveCenter> centers = ACTIVE_CENTERS.get(level);
      if (centers == null || centers.isEmpty()) {
         return false;
      }
      Iterator<ActiveCenter> iterator = centers.values().iterator();
      while (iterator.hasNext()) {
         ActiveCenter entry = iterator.next();
         if (entry.lastSeen() < level.getGameTime() - 2L) {
            iterator.remove();
            continue;
         }
         if (!MagicCenterTypeRegistry.FORCEFIELD.equals(entry.type())
               || !(level.getBlockEntity(entry.pos()) instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || CenterTargetPolicy.mayCrossForcefield(center, entity)) {
            continue;
         }
         Vec3 centerPoint = Vec3.atCenterOf(entry.pos());
         double radiusSqr = entry.radius() * (double)entry.radius();
         boolean startInside = start.distanceToSqr(centerPoint) <= radiusSqr;
         boolean endInside = end.distanceToSqr(centerPoint) <= radiusSqr;
         if (startInside != endInside
               || !startInside && intersectsSphere(start, end, centerPoint, entry.radius())) {
            return true;
         }
      }
      return false;
   }

   public static boolean isHostileTarget(MagicCenterBlockEntity center, LivingEntity entity) {
      if (entity instanceof Player player && (center.isOwner(player) || center.isPlayerExcluded(player))) {
         return false;
      }
      return center.isAffectedByCircle(entity);
   }

   public static void beginSpellCast(ServerPlayer player, String spellId) {
      ModSpells.SpellDef spell = ModSpells.get(spellId);
      String element = spell == null ? "" : spell.element();
      SPELL_CAST.set(new SpellCastContext(player.getUUID(), spellId, element));
      player.getPersistentData().putString(LAST_SPELL_ELEMENT, element);
      player.getPersistentData().putLong(LAST_SPELL_TIME, player.level().getGameTime());
      if (SpiritRoots.FIRE.equals(element) && isInside(player, MagicCenterTypeRegistry.FROST)) {
         MobEffectInstance frozenQi = player.getEffect(ModEffects.FROZEN_QI);
         if (frozenQi != null) {
            int remaining = frozenQi.getDuration() - 40;
            int amplifier = frozenQi.getAmplifier();
            player.removeEffect(ModEffects.FROZEN_QI);
            if (remaining > 0) {
               player.addEffect(new MobEffectInstance(ModEffects.FROZEN_QI, remaining, amplifier,
                     false, true, true));
            }
         }
      }
   }

   public static void endSpellCast() {
      SPELL_CAST.remove();
   }

   public static int adjustSpellQiCost(Player player, int cost) {
      SpellCastContext cast = SPELL_CAST.get();
      return cast == null || !cast.playerId().equals(player.getUUID())
            ? Math.max(0, cost)
            : adjustSpellQiCost(player, cost, cast.spellId());
   }

   public static int adjustSpellQiCost(Player player, int cost, String spellId) {
      int adjusted = Math.max(0, cost);
      if (adjusted == 0 || !(player instanceof ServerPlayer serverPlayer)) {
         return adjusted;
      }
      ModSpells.SpellDef spell = ModSpells.get(spellId);
      String element = spell == null ? "" : spell.element();
      double multiplier = 1.0D;
      if (ModSpells.REGENERATION.equals(spellId)) {
         if (isInsideBeneficial(serverPlayer, MagicCenterTypeRegistry.SANCTUARY)) {
            multiplier *= 0.75D;
         }
         if (isInsideHarmful(serverPlayer, MagicCenterTypeRegistry.DECAY)) {
            multiplier *= 1.25D;
         }
      }
      if (SpiritRoots.ICE.equals(element)
            && isInsideHarmful(serverPlayer, MagicCenterTypeRegistry.SCORCHING)) {
         multiplier *= 1.15D;
      }
      if (isInsideHarmful(serverPlayer, MagicCenterTypeRegistry.FROST)) {
         multiplier *= hasMethodElement(serverPlayer, SpiritRoots.ICE) ? 1.10D : 1.20D;
      }
      return Math.max(1, (int)Math.ceil(adjusted * multiplier));
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
      if (amount <= 0.0F || !isInsideHarmful(target, MagicCenterTypeRegistry.SCORCHING)
            || !isRecentSpellElement(source, SpiritRoots.FIRE)
            && !source.is(DamageTypeTags.IS_FIRE)) {
         return amount;
      }
      return amount * 1.15F;
   }

   public static float adjustHealing(LivingEntity entity, float amount) {
      float adjusted = amount;
      if (isInsideHarmful(entity, MagicCenterTypeRegistry.SCORCHING)) {
         adjusted *= hasMethodElement(entity, SpiritRoots.FIRE) ? 0.75F : 0.5F;
      }
      if (isInsideHarmful(entity, MagicCenterTypeRegistry.DECAY)) {
         adjusted *= hasBloodOrCorpseAffinity(entity) ? 0.75F : 0.5F;
      }
      return Math.max(0.0F, adjusted);
   }

   public static int maxQi(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         ModAttachments.CultivationData data = ModAttachments.getData(player);
         return Math.max(0, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi()
               + data.maxQiBonus());
      }
      if (entity instanceof CultivationNpc npc) {
         return npc.npcMaxQi();
      }
      return 0;
   }

   public static int currentQi(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         return ModAttachments.getData(player).qi();
      }
      if (entity instanceof CultivationNpc npc) {
         return npc.npcQi();
      }
      return 0;
   }

   public static int drainQi(LivingEntity entity, int amount) {
      int requested = Math.max(0, amount);
      if (requested == 0) {
         return 0;
      }
      if (entity instanceof ServerPlayer player) {
         ModAttachments.CultivationData data = ModAttachments.getData(player);
         int drained = Math.min(requested, data.qi());
         ModAttachments.setData(player, data.withQi(data.qi() - drained));
         return drained;
      }
      if (entity instanceof CultivationNpc npc) {
         int drained = Math.min(requested, npc.npcQi());
         NpcProfile.drainQi(npc.asNpcMob(), npc.npcArchetype(), drained);
         return drained;
      }
      return 0;
   }

   public static void restoreQi(LivingEntity entity, int amount) {
      int restored = Math.max(0, amount);
      if (restored == 0) {
         return;
      }
      if (entity instanceof ServerPlayer player) {
         ModAttachments.CultivationData data = ModAttachments.getData(player);
         ModAttachments.setData(player, data.withQi(Math.min(maxQi(player), data.qi() + restored)));
      } else if (entity instanceof CultivationNpc npc) {
         NpcProfile.restoreQi(npc.asNpcMob(), npc.npcArchetype(), restored);
      }
   }

   public static boolean hasMethodElement(LivingEntity entity, String element) {
      CultivationMethods.MethodDef method = CultivationMethods.get(methodId(entity));
      if (method == null) {
         return false;
      }
      for (String methodElement : method.element().split(",")) {
         if (element.equals(methodElement)) {
            return true;
         }
      }
      return false;
   }

   public static boolean hasBloodOrCorpseAffinity(LivingEntity entity) {
      String methodId = methodId(entity);
      if (CultivationMethods.isBloodDemon(methodId)
            || CultivationMethods.isJiuyouLianshiJue(methodId)) {
         return true;
      }
      if (entity instanceof ServerPlayer player) {
         return "corpse_cultivator".equals(IdentityItems.ensurePlayerIdentity(player));
      }
      return entity instanceof CultivationNpc npc
            && "corpse_cultivator".equals(npc.npcIdentity());
   }

   public static void suppressQiRegeneration(LivingEntity entity, int ticks) {
      entity.getPersistentData().putLong(THUNDER_SUPPRESSION_UNTIL,
            entity.level().getGameTime() + Math.max(1, ticks));
   }

   public static boolean hasThunderSuppression(LivingEntity entity) {
      return entity.getPersistentData().getLong(THUNDER_SUPPRESSION_UNTIL)
            > entity.level().getGameTime();
   }

   public static String realmName(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         return ModAttachments.getData(player).cultivationLevel();
      }
      if (entity instanceof CultivationNpc npc) {
         return npc.npcRealm();
      }
      return "凡人";
   }

   private static String methodId(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         return ModAttachments.getData(player).activeCultivationMethod();
      }
      if (entity instanceof CultivationNpc npc) {
         return NpcProfile.methodId(npc.asNpcMob(), npc.npcArchetype());
      }
      return "";
   }

   private static boolean isRecentSpellElement(DamageSource source, String element) {
      Entity attacker = source.getEntity();
      if (!(attacker instanceof ServerPlayer player)) {
         return false;
      }
      SpellCastContext cast = SPELL_CAST.get();
      if (cast != null && cast.playerId().equals(player.getUUID())
            && element.equals(cast.element())) {
         return true;
      }
      if (source.getDirectEntity() == attacker) {
         return false;
      }
      return element.equals(player.getPersistentData().getString(LAST_SPELL_ELEMENT))
            && player.level().getGameTime() - player.getPersistentData().getLong(LAST_SPELL_TIME) <= 100L;
   }

   private static MagicCenterBlockEntity findCenter(LivingEntity entity, ResourceLocation type,
                                                      Predicate<MagicCenterBlockEntity> predicate) {
      for (MagicCenterBlockEntity center : centersContaining(entity, type)) {
         if (predicate.test(center)) {
            return center;
         }
      }
      return null;
   }

   private static List<MagicCenterBlockEntity> centersContaining(
         Entity entity, ResourceLocation requiredType) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return List.of();
      }
      Map<Long, ActiveCenter> centers = ACTIVE_CENTERS.get(level);
      if (centers == null || centers.isEmpty()) {
         return List.of();
      }
      List<MagicCenterBlockEntity> matches = new ArrayList<>();
      Iterator<ActiveCenter> iterator = centers.values().iterator();
      while (iterator.hasNext()) {
         ActiveCenter entry = iterator.next();
         if (entry.lastSeen() < level.getGameTime() - 2L) {
            iterator.remove();
            continue;
         }
         double dx = entity.getX() - (entry.pos().getX() + 0.5D);
         double dz = entity.getZ() - (entry.pos().getZ() + 0.5D);
         if ((requiredType != null && !requiredType.equals(entry.type()))
               || entity.getY() < entry.pos().getY()
               || dx * dx + dz * dz > entry.radius() * entry.radius()) {
            continue;
         }
         if (level.getBlockEntity(entry.pos()) instanceof MagicCenterBlockEntity center
               && center.isActive()) {
            matches.add(center);
         }
      }
      return matches;
   }

   private static boolean isDangerousType(ResourceLocation type) {
      return MagicCenterTypeRegistry.SCORCHING.equals(type)
            || MagicCenterTypeRegistry.FROST.equals(type)
            || MagicCenterTypeRegistry.DECAY.equals(type)
            || MagicCenterTypeRegistry.THUNDER.equals(type);
   }

   private static boolean intersectsSphere(Vec3 start, Vec3 end, Vec3 center, double radius) {
      Vec3 segment = end.subtract(start);
      double lengthSquared = segment.lengthSqr();
      if (lengthSquared < 1.0E-7D) {
         return start.distanceToSqr(center) <= radius * radius;
      }
      double t = Math.max(0.0D, Math.min(1.0D,
            center.subtract(start).dot(segment) / lengthSquared));
      return start.add(segment.scale(t)).distanceToSqr(center) <= radius * radius;
   }

   private record ActiveCenter(BlockPos pos, ResourceLocation type, int radius, long lastSeen) {
   }

   private record SpellCastContext(java.util.UUID playerId, String spellId, String element) {
   }
}
