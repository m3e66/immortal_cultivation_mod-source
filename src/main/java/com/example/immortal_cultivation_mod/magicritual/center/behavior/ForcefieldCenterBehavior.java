package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.entity.LightningProjectileEntity;
import com.example.immortal_cultivation_mod.item.FuchenItem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.ShihunfanItem;
import com.example.immortal_cultivation_mod.item.ThrowingKnifeItem;
import com.example.immortal_cultivation_mod.item.WhipItem;
import com.example.immortal_cultivation_mod.item.XiujianItem;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ForcefieldCenterBehavior implements MagicCenterBehavior {
   private static final DustParticleOptions IMPACT_PARTICLE =
         new DustParticleOptions(new Vector3f(0.2F, 0.65F, 1.0F), 1.2F);
   private static final double CONTACT_EPSILON = 0.055D;
   private static final double BASE_PUSH = 0.12D;
   private static final double BOUNCE_SPEED_MULTIPLIER = 1.5D;
   private static final double STATE_KEEP_DISTANCE = 4.0D;
   private static final long STATE_TIMEOUT = 3L;

   private static final Map<ServerLevel, Map<Long, FieldState>> FIELDS_BY_LEVEL =
         new WeakHashMap<>();
   private static final Map<ServerLevel, Map<CollisionKey, CollisionState>> COLLISIONS_BY_LEVEL =
         new WeakHashMap<>();
   private static final Map<ServerLevel, Map<CollisionKey, Long>> LAST_MELEE_HITS_BY_LEVEL =
         new WeakHashMap<>();
   private static final Map<ServerLevel, Map<CollisionKey, MeleeCredit>> MELEE_CREDITS_BY_LEVEL =
         new WeakHashMap<>();
   private static final Map<ServerLevel, Map<DamagePulseKey, DamagePulseState>> DAMAGE_PULSES_BY_LEVEL =
         new WeakHashMap<>();

   @Override
   public void serverTick(CenterContext context) {
      ServerLevel level = context.level();
      long gameTime = level.getGameTime();
      long centerKey = context.pos().asLong();
      double radius = context.center().getRadius();
      Map<Long, FieldState> fields = FIELDS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      Map<CollisionKey, CollisionState> collisions = COLLISIONS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      Map<CollisionKey, Long> lastMeleeHits = LAST_MELEE_HITS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());

      FieldState prior = fields.get(centerKey);
      boolean continuous = prior != null && prior.lastSeen() == gameTime - 1L;
      double previousRadius = continuous ? prior.radius() : radius;
      if (!continuous) {
         purgeCenterState(centerKey, collisions, lastMeleeHits);
      }
      fields.put(centerKey, new FieldState(context.pos().immutable(), radius, gameTime));

      if (gameTime % 20L == 0L) {
         Iterator<Map.Entry<Long, FieldState>> fieldIterator = fields.entrySet().iterator();
         while (fieldIterator.hasNext()) {
            Map.Entry<Long, FieldState> entry = fieldIterator.next();
            if (entry.getValue().lastSeen() < gameTime - STATE_TIMEOUT) {
               fieldIterator.remove();
               purgeCenterState(entry.getKey(), collisions, lastMeleeHits);
            }
         }
         collisions.values().removeIf(state -> state.lastSeen() < gameTime - 40L);
         lastMeleeHits.values().removeIf(hitTime -> hitTime < gameTime - 40L);
         Map<CollisionKey, MeleeCredit> meleeCredits = MELEE_CREDITS_BY_LEVEL.get(level);
         if (meleeCredits != null) {
            meleeCredits.values().removeIf(credit -> credit.gameTime() < gameTime - 2L);
            if (meleeCredits.isEmpty()) {
               MELEE_CREDITS_BY_LEVEL.remove(level);
            }
         }
         Map<DamagePulseKey, DamagePulseState> damagePulses = DAMAGE_PULSES_BY_LEVEL.get(level);
         if (damagePulses != null) {
            damagePulses.values().removeIf(pulse -> pulse.gameTime() < gameTime - 40L);
            if (damagePulses.isEmpty()) {
               DAMAGE_PULSES_BY_LEVEL.remove(level);
            }
         }
      }

      if (Math.abs(radius - previousRadius) > 1.0E-6D
            && !sweepRadiusChange(context, previousRadius, radius, collisions)) {
         return;
      }
      handleMeleeImpacts(context, lastMeleeHits);
   }

   /** Predicts the coming movement before projectile hit logic or ordinary movement. */
   public static boolean enforceEntityPreTick(Entity entity) {
      return enforceEntity(entity, tickPrediction(entity));
   }

   /** Sweeps the actual movement after the tick as a fallback for late movement changes. */
   public static void enforceEntityPostTick(Entity entity) {
      enforceEntity(entity, tickPrediction(entity));
   }

   /** Rechecks a projectile only as far as the first vanilla impact for this tick. */
   public static boolean interceptProjectileImpact(Projectile projectile, Vec3 impactLocation) {
      return enforceEntity(projectile,
            impactLocation.subtract(projectile.position()));
   }

   /** Stops a real hitscan or control action when its path crosses a rejecting field. */
   public static boolean interceptAttackPath(Entity sourceEntity, Vec3 sourcePosition,
                                             Vec3 targetPosition, float amount) {
      if (!(sourceEntity.level() instanceof ServerLevel level)) {
         return false;
      }
      return interceptAttackPath(level, sourceEntity, sourcePosition,
            targetPosition, amount, null);
   }

   /** Stops local lightning effects from reaching an entity across a field boundary. */
   public static boolean interceptLightningStrike(Entity target, LightningBolt lightning) {
      if (!(target.level() instanceof ServerLevel level)) {
         return false;
      }
      return interceptAttackPath(level, null, lightning.position(),
            collisionBox(target).getCenter(), lightning.getDamage(), lightning.getUUID());
   }

   private static boolean interceptAttackPath(ServerLevel level, Entity policySource,
                                              Vec3 sourcePosition, Vec3 targetPosition,
                                              float amount, Object pulseToken) {
      if (sourcePosition.distanceToSqr(targetPosition) < 1.0E-8D) {
         return false;
      }
      Map<Long, FieldState> fields = FIELDS_BY_LEVEL.get(level);
      if (fields == null || fields.isEmpty()) {
         return false;
      }

      long gameTime = level.getGameTime();
      List<DamageContact> contacts = new ArrayList<>();
      for (Map.Entry<Long, FieldState> entry : fields.entrySet()) {
         FieldState field = entry.getValue();
         if (field.lastSeen() < gameTime - STATE_TIMEOUT
               || !level.hasChunkAt(field.pos())
               || !(level.getBlockEntity(field.pos())
                     instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())
               || (policySource != null && mayCross(center, policySource))) {
            continue;
         }
         Vec3 fieldCenter = Vec3.atCenterOf(field.pos());
         boolean sourceInside = sourcePosition.distanceToSqr(fieldCenter)
               < field.radius() * field.radius();
         Contact contact = findContact(sourcePosition, targetPosition, fieldCenter,
               field.radius(), sourceInside);
         if (contact.hit()) {
            contacts.add(new DamageContact(
                  segmentParameter(sourcePosition, targetPosition, contact.point()),
                  entry.getKey(), center, contact));
         }
      }

      contacts.sort(Comparator.comparingDouble(DamageContact::parameter)
            .thenComparingLong(DamageContact::centerKey));
      Map<DamagePulseKey, DamagePulseState> damagePulses = pulseToken == null
            ? null : DAMAGE_PULSES_BY_LEVEL.computeIfAbsent(
                  level, ignored -> new HashMap<>());
      for (DamageContact candidate : contacts) {
         MagicCenterBlockEntity center = candidate.center();
         if (!center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            continue;
         }
         DamagePulseKey pulseKey = pulseToken == null ? null
               : new DamagePulseKey(candidate.centerKey(), pulseToken);
         if (pulseKey != null) {
            DamagePulseState prior = damagePulses.get(pulseKey);
            if (prior != null && prior.gameTime() == gameTime) {
               return true;
            }
         }
         float barrierDamage = Math.max(1.0F, amount);
         if (!center.absorbBarrierDamage(barrierDamage)) {
            continue;
         }
         if (pulseKey != null) {
            damagePulses.put(pulseKey,
                  new DamagePulseState(gameTime, barrierDamage));
         }
         spawnImpact(level, candidate.contact().point());
         return true;
      }
      return false;
   }

   /** Stops real damage whose source-to-target path crosses a rejecting field. */
   public static boolean interceptDamage(LivingEntity target, DamageSource source,
                                         float amount) {
      if (!(target.level() instanceof ServerLevel level)) {
         return false;
      }
      Vec3 sourcePosition = source.getSourcePosition();
      if (sourcePosition == null) {
         return false;
      }
      Entity sourceEntity = source.getEntity() != null
            ? source.getEntity() : source.getDirectEntity();
      Entity directEntity = source.getDirectEntity();
      Entity policyEntity = directEntity != null && isAbsorbableAttack(directEntity)
            ? directEntity : sourceEntity;
      if (sourceEntity == target) {
         return false;
      }
      Map<Long, FieldState> fields = FIELDS_BY_LEVEL.get(level);
      if (fields == null || fields.isEmpty()) {
         return false;
      }

      long gameTime = level.getGameTime();
      Vec3 targetPosition = collisionBox(target).getCenter();
      List<DamageContact> contacts = new ArrayList<>();
      for (Map.Entry<Long, FieldState> entry : fields.entrySet()) {
         FieldState field = entry.getValue();
         if (field.lastSeen() < gameTime - STATE_TIMEOUT
               || !level.hasChunkAt(field.pos())
               || !(level.getBlockEntity(field.pos())
                     instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())
               || (policyEntity != null && mayCross(center, policyEntity))) {
            continue;
         }
         Vec3 fieldCenter = Vec3.atCenterOf(field.pos());
         boolean sourceInside = sourcePosition.distanceToSqr(fieldCenter)
               < field.radius() * field.radius();
         Contact contact = findContact(sourcePosition, targetPosition, fieldCenter,
               field.radius(), sourceInside);
         if (contact.hit()) {
            contacts.add(new DamageContact(
                  segmentParameter(sourcePosition, targetPosition, contact.point()),
                  entry.getKey(), center, contact));
         }
      }

      contacts.sort(Comparator.comparingDouble(DamageContact::parameter)
            .thenComparingLong(DamageContact::centerKey));
      if (contacts.isEmpty()) {
         return false;
      }
      Map<CollisionKey, Long> lastMeleeHits = LAST_MELEE_HITS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      Map<CollisionKey, MeleeCredit> meleeCredits = MELEE_CREDITS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      Map<DamagePulseKey, DamagePulseState> damagePulses =
            DAMAGE_PULSES_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      boolean sameLivingSource = sourceEntity instanceof LivingEntity
            && source.getDirectEntity() == sourceEntity;
      boolean directMeleeAttack = sameLivingSource
            && source.sourcePositionRaw() == null
            && (source.is(DamageTypes.PLAYER_ATTACK)
                  || source.is(DamageTypes.MOB_ATTACK)
                  || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
      Object pulseToken = source.getDirectEntity() == null || sameLivingSource
            ? new DamageSourceToken(
                  source.getDirectEntity() == null
                        ? null : source.getDirectEntity().getUUID(),
                  new DamageSourceIdentity(source), source.getMsgId())
            : source.getDirectEntity().getUUID();
      for (DamageContact candidate : contacts) {
         MagicCenterBlockEntity center = candidate.center();
         if (!center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            continue;
         }
         CollisionKey meleeKey = sourceEntity == null ? null
               : new CollisionKey(candidate.centerKey(), sourceEntity.getUUID());
         DamagePulseKey pulseKey = new DamagePulseKey(
               candidate.centerKey(), pulseToken);
         float pulseDamage = Math.max(1.0F, amount);
         DamagePulseState priorPulse = damagePulses.get(pulseKey);
         float chargeAmount = pulseDamage;
         if (priorPulse != null && priorPulse.gameTime() == gameTime) {
            chargeAmount -= priorPulse.maxDamage();
         }
         if (chargeAmount <= 1.0E-4F) {
            return true;
         }

         boolean coveredByMeleeCredit = false;
         if (directMeleeAttack && meleeKey != null) {
            MeleeCredit credit = meleeCredits.get(meleeKey);
            if (credit != null && gameTime - credit.gameTime() <= 1L) {
               float usedCredit = Math.min(chargeAmount, credit.remainingDamage());
               chargeAmount -= usedCredit;
               float remainingCredit = credit.remainingDamage() - usedCredit;
               if (remainingCredit > 1.0E-4F) {
                  meleeCredits.put(meleeKey,
                        new MeleeCredit(credit.gameTime(), remainingCredit));
               } else {
                  meleeCredits.remove(meleeKey);
               }
               coveredByMeleeCredit = chargeAmount <= 1.0E-4F;
            }
         }
         if (!coveredByMeleeCredit
               && !center.absorbBarrierDamage(Math.max(1.0F, chargeAmount))) {
            continue;
         }
         damagePulses.put(pulseKey,
               new DamagePulseState(gameTime, pulseDamage));
         if (directMeleeAttack && meleeKey != null) {
            lastMeleeHits.put(meleeKey, gameTime);
         }
         if (!coveredByMeleeCredit) {
            spawnImpact(level, candidate.contact().point());
         }
         return true;
      }
      return false;
   }

   /** Keeps an outside explosion from destroying terrain inside an active field. */
   public static void protectBlocksFromExplosion(ServerLevel level, Vec3 explosionCenter,
                                                 List<BlockPos> affectedBlocks) {
      Map<Long, FieldState> fields = FIELDS_BY_LEVEL.get(level);
      if (affectedBlocks.isEmpty() || fields == null || fields.isEmpty()) {
         return;
      }

      long gameTime = level.getGameTime();
      List<ExplosionProtection> protections = new ArrayList<>();
      for (FieldState field : fields.values()) {
         if (field.lastSeen() < gameTime - STATE_TIMEOUT
               || !level.hasChunkAt(field.pos())
               || !(level.getBlockEntity(field.pos())
                     instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            continue;
         }
         Vec3 fieldCenter = center.centerPoint();
         double radius = center.getRadius();
         double radiusSquared = radius * radius;
         if (explosionCenter.distanceToSqr(fieldCenter) >= radiusSquared) {
            protections.add(new ExplosionProtection(fieldCenter, radiusSquared));
         }
      }
      if (protections.isEmpty()) {
         return;
      }

      affectedBlocks.removeIf(block -> {
         for (ExplosionProtection protection : protections) {
            if (blockIntersectsSphere(block, protection)) {
               return true;
            }
         }
         return false;
      });
   }

   private static boolean blockIntersectsSphere(BlockPos block,
                                                ExplosionProtection protection) {
      Vec3 center = protection.center();
      double closestX = Mth.clamp(center.x, block.getX(), block.getX() + 1.0D);
      double closestY = Mth.clamp(center.y, block.getY(), block.getY() + 1.0D);
      double closestZ = Mth.clamp(center.z, block.getZ(), block.getZ() + 1.0D);
      return center.distanceToSqr(closestX, closestY, closestZ)
            <= protection.radiusSquared();
   }

   private static boolean enforceEntity(Entity entity, Vec3 movement) {
      if (!(entity.level() instanceof ServerLevel level) || !isRelevant(entity)) {
         return false;
      }
      Map<Long, FieldState> fields = FIELDS_BY_LEVEL.get(level);
      if (fields == null || fields.isEmpty()) {
         FIELDS_BY_LEVEL.remove(level);
         clearCompanionState(level);
         return false;
      }
      long gameTime = level.getGameTime();
      Map<CollisionKey, CollisionState> collisions = COLLISIONS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      Map<CollisionKey, Long> lastMeleeHits = LAST_MELEE_HITS_BY_LEVEL.computeIfAbsent(
            level, ignored -> new HashMap<>());
      Vec3 current = forcefieldCenter(entity);
      Vec3 fallbackStart = centerPointAt(entity, new Vec3(entity.xo, entity.yo, entity.zo));
      double broadSupport = radialSupportUpperBound(entity);
      if (isAbsorbableAttack(entity)
            && absorbAtFirstCrossedField(entity, movement, fields, collisions,
                  current, fallbackStart, broadSupport, gameTime)) {
         return true;
      }
      Iterator<Map.Entry<Long, FieldState>> iterator = fields.entrySet().iterator();
      while (iterator.hasNext() && entity.isAlive()) {
         Map.Entry<Long, FieldState> entry = iterator.next();
         FieldState field = entry.getValue();
         if (field.lastSeen() < gameTime - STATE_TIMEOUT) {
            iterator.remove();
            purgeCenterState(entry.getKey(), collisions, lastMeleeHits);
            continue;
         }

         CollisionKey key = new CollisionKey(entry.getKey(), entity.getUUID());
         CollisionState remembered = collisions.get(key);
         boolean rememberedSide = remembered != null
               && remembered.lastSeen() >= gameTime - STATE_TIMEOUT;
         Vec3 start = rememberedSide ? remembered.lastPosition() : fallbackStart;
         Vec3 fieldCenter = Vec3.atCenterOf(field.pos());
         double broadRadius = field.radius() + broadSupport + STATE_KEEP_DISTANCE;
         if (!intersectsSphere(start, current, fieldCenter, broadRadius)
               && !intersectsSphere(current, current.add(movement),
                     fieldCenter, broadRadius)) {
            collisions.remove(key);
            continue;
         }
         if (!level.hasChunkAt(field.pos())
               || !(level.getBlockEntity(field.pos()) instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            iterator.remove();
            purgeCenterState(entry.getKey(), collisions, lastMeleeHits);
            continue;
         }

         if (mayCross(center, entity)) {
            collisions.remove(key);
            continue;
         }
         boolean keepInside = rememberedSide
               ? remembered.keepInside()
               : classifyInitialSide(entity, start, fieldCenter, field.radius(),
                     entry.getKey(), collisions, gameTime);

         Contact contact = findBodyContact(entity, start, current, fieldCenter,
               field.radius(), keepInside);
         Vec3 predictionStart = contact.hit()
               ? boundaryPoint(fieldCenter, contact.normal(), contact.radius(), keepInside)
               : current;
         Contact predicted = findBodyContact(entity, predictionStart,
               predictionStart.add(movement), fieldCenter,
               field.radius(), keepInside);
         if (predicted.hit() && !isAbsorbableAttack(entity)
               && !hasClearPath(level, entity, predictionStart, predicted.point())) {
            predicted = Contact.NONE;
         }
         Contact blockingContact = contact.hit() ? contact : predicted;

         if (blockingContact.hit() && isAbsorbableAttack(entity)
               && isMovingIntoWall(entity, movement, blockingContact.normal(), keepInside)) {
            if (!center.absorbBarrierDamage(barrierDamage(entity))) {
               collisions.remove(key);
               continue;
            }
            Vec3 bounceNormal = blockingContact.normal();
            bounceVelocity(entity, movement, bounceNormal, keepInside, 0.0D);
            double outgoingRadius = bodyBoundaryRadius(
                  entity, field.radius(), bounceNormal, keepInside);
            Vec3 target = boundaryPoint(
                  fieldCenter, bounceNormal, outgoingRadius, keepInside);
            moveEntityCenter(level, entity, target, fieldCenter);
            current = forcefieldCenter(entity);
            spawnImpact(level, blockingContact.point());
            collisions.put(key, new CollisionState(
                  current, keepInside, gameTime, gameTime));
            return true;
         }

         long lastImpact = remembered == null ? Long.MIN_VALUE / 2L : remembered.lastImpact();
         Vec3 responseNormal = blockingContact.normal();
         if (contact.hit()) {
            Vec3 target = boundaryPoint(fieldCenter, contact.normal(),
                  contact.radius(), keepInside);
            moveEntityCenter(level, entity, target, fieldCenter);
            current = forcefieldCenter(entity);
            responseNormal = radialDirection(current, current, fieldCenter);
         }
         if (blockingContact.hit()) {
            bounceVelocity(entity, movement, responseNormal, keepInside, 0.0D);
            movement = forcefieldMovement(entity);
            if (gameTime - lastImpact >= 5L) {
               spawnImpact(level, blockingContact.point());
               lastImpact = gameTime;
            }
         }

         double distance = current.distanceTo(fieldCenter);
         double support = radialSupportUpperBound(entity);
         if (!keepInside && !blockingContact.hit()
               && distance > field.radius() + support + STATE_KEEP_DISTANCE) {
            collisions.remove(key);
         } else {
            collisions.put(key, new CollisionState(current, keepInside, gameTime, lastImpact));
         }
      }
      if (fields.isEmpty()) {
         FIELDS_BY_LEVEL.remove(level);
         clearCompanionState(level);
      }
      return false;
   }

   private static boolean absorbAtFirstCrossedField(
         Entity entity, Vec3 movement, Map<Long, FieldState> fields,
         Map<CollisionKey, CollisionState> collisions, Vec3 current,
         Vec3 fallbackStart, double broadSupport, long gameTime) {
      List<AttackContact> contacts = new ArrayList<>();
      for (Map.Entry<Long, FieldState> entry : fields.entrySet()) {
         FieldState field = entry.getValue();
         if (field.lastSeen() < gameTime - STATE_TIMEOUT
               || !entity.level().hasChunkAt(field.pos())
               || !(entity.level().getBlockEntity(field.pos())
                     instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())
               || mayCross(center, entity)) {
            continue;
         }

         CollisionKey key = new CollisionKey(entry.getKey(), entity.getUUID());
         CollisionState remembered = collisions.get(key);
         boolean rememberedSide = remembered != null
               && remembered.lastSeen() >= gameTime - STATE_TIMEOUT;
         Vec3 start = rememberedSide ? remembered.lastPosition() : fallbackStart;
         Vec3 fieldCenter = Vec3.atCenterOf(field.pos());
         double broadRadius = field.radius() + broadSupport + STATE_KEEP_DISTANCE;
         if (!intersectsSphere(start, current, fieldCenter, broadRadius)
               && !intersectsSphere(current, current.add(movement),
                     fieldCenter, broadRadius)) {
            continue;
         }

         boolean keepInside = rememberedSide
               ? remembered.keepInside()
               : classifyInitialSide(entity, start, fieldCenter, field.radius(),
                     entry.getKey(), collisions, gameTime);
         Contact past = findBodyContact(entity, start, current, fieldCenter,
               field.radius(), keepInside);
         if (past.hit()) {
            contacts.add(new AttackContact(0,
                  segmentParameter(start, current, past.point()), entry.getKey(),
                  center, key, past, keepInside));
            continue;
         }
         Vec3 futureEnd = current.add(movement);
         Contact future = findBodyContact(entity, current, futureEnd, fieldCenter,
               field.radius(), keepInside);
         if (future.hit()) {
            contacts.add(new AttackContact(1,
                  segmentParameter(current, futureEnd, future.point()), entry.getKey(),
                  center, key, future, keepInside));
         }
      }

      contacts.sort(Comparator.comparingInt(AttackContact::phase)
            .thenComparingDouble(AttackContact::parameter)
            .thenComparingLong(AttackContact::centerKey));
      for (AttackContact candidate : contacts) {
         MagicCenterBlockEntity center = candidate.center();
         if (!center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            collisions.remove(candidate.collisionKey());
            continue;
         }
         if (!isMovingIntoWall(entity, movement, candidate.contact().normal(),
               candidate.keepInside())) {
            continue;
         }
         if (!center.absorbBarrierDamage(barrierDamage(entity))) {
            collisions.remove(candidate.collisionKey());
            continue;
         }
         ServerLevel level = (ServerLevel)entity.level();
         Vec3 fieldCenter = center.centerPoint();
         Vec3 bounceNormal = candidate.contact().normal();
         bounceVelocity(entity, movement, bounceNormal,
               candidate.keepInside(), 0.0D);
         double outgoingRadius = bodyBoundaryRadius(
               entity, center.getRadius(), bounceNormal, candidate.keepInside());
         Vec3 target = boundaryPoint(fieldCenter, bounceNormal,
               outgoingRadius, candidate.keepInside());
         moveEntityCenter(level, entity, target, fieldCenter);
         Vec3 corrected = forcefieldCenter(entity);
         spawnImpact(level, candidate.contact().point());
         collisions.put(candidate.collisionKey(), new CollisionState(
               corrected, candidate.keepInside(), gameTime, gameTime));
         return true;
      }
      return false;
   }

   private static double segmentParameter(Vec3 start, Vec3 end, Vec3 point) {
      Vec3 segment = end.subtract(start);
      double lengthSquared = segment.lengthSqr();
      if (lengthSquared < 1.0E-9D) {
         return 0.0D;
      }
      return Math.max(0.0D, Math.min(1.0D,
            point.subtract(start).dot(segment) / lengthSquared));
   }

   private static boolean sweepRadiusChange(CenterContext context, double oldRadius,
                                            double newRadius,
                                            Map<CollisionKey, CollisionState> collisions) {
      ServerLevel level = context.level();
      Vec3 fieldCenter = context.center().centerPoint();
      double radiusChange = newRadius - oldRadius;
      double scanRadius = Math.max(oldRadius, newRadius) + 4.0D;
      AABB bounds = new AABB(context.pos()).inflate(scanRadius);
      long gameTime = level.getGameTime();
      long centerKey = context.pos().asLong();
      for (Entity entity : level.getEntities((Entity)null, bounds,
            ForcefieldCenterBehavior::isRelevant)) {
         if (mayCross(context.center(), entity)) {
            continue;
         }
         CollisionKey key = new CollisionKey(centerKey, entity.getUUID());
         CollisionState remembered = collisions.get(key);
         boolean rememberedSide = remembered != null
               && remembered.lastSeen() >= gameTime - STATE_TIMEOUT;
         Vec3 current = forcefieldCenter(entity);
         boolean keepInside = rememberedSide
               ? remembered.keepInside()
               : classifyInitialSide(entity, current, fieldCenter, oldRadius,
                     centerKey, collisions, gameTime);
         Vec3 normal = radialDirection(current, current, fieldCenter);
         double support = radialSupportUpperBound(entity);
         double distance = current.distanceTo(fieldCenter);
         double targetDistance = distance;
         boolean swept = false;
         if (radiusChange > 0.0D && !keepInside) {
            double newLimit = newRadius + support;
            if (distance < newLimit + CONTACT_EPSILON) {
               targetDistance = Math.max(newLimit + CONTACT_EPSILON,
                     distance + radiusChange);
               swept = true;
            }
         } else if (radiusChange < 0.0D && keepInside) {
            double newLimit = Math.max(0.2D, newRadius - support);
            if (distance > newLimit - CONTACT_EPSILON) {
               targetDistance = Math.max(0.2D,
                     Math.min(newLimit - CONTACT_EPSILON, distance + radiusChange));
               swept = true;
            }
         }
         if (!swept) {
            collisions.put(key, new CollisionState(current, keepInside, gameTime,
                  remembered == null ? Long.MIN_VALUE / 2L : remembered.lastImpact()));
            continue;
         }
         if (isAbsorbableAttack(entity)
               && !context.center().absorbBarrierDamage(barrierDamage(entity))) {
            return false;
         }
         Vec3 target = fieldCenter.add(normal.scale(targetDistance));
         moveEntityCenter(level, entity, target, fieldCenter);
         Vec3 corrected = forcefieldCenter(entity);
         Vec3 correctedNormal = radialDirection(corrected, corrected, fieldCenter);
         bounceVelocity(entity, forcefieldMovement(entity), correctedNormal,
               keepInside, radiusChange);
         spawnImpact(level, corrected);
         collisions.put(key, new CollisionState(corrected, keepInside, gameTime, gameTime));
      }
      return true;
   }

   private static void handleMeleeImpacts(CenterContext context,
                                          Map<CollisionKey, Long> lastMeleeHits) {
      long gameTime = context.level().getGameTime();
      double radius = context.center().getRadius();
      Vec3 center = context.center().centerPoint();
      double playerScanRadiusSquared = (radius + 6.0D) * (radius + 6.0D);
      for (ServerPlayer player : context.level().players()) {
         if (!player.isAlive() || mayCross(context.center(), player)
               || player.position().distanceToSqr(center) > playerScanRadiusSquared) {
            continue;
         }
         CollisionKey key = new CollisionKey(context.pos().asLong(), player.getUUID());
         if (!player.swinging || player.swingTime > 2
               || player.swingingArm != InteractionHand.MAIN_HAND
               || !isStandaloneMeleeSwing(player)
               || gameTime - lastMeleeHits.getOrDefault(key, Long.MIN_VALUE / 2L) < 5L) {
            continue;
         }
         Vec3 start = player.getEyePosition();
         Vec3 end = start.add(player.getLookAngle().scale(5.5D));
         MeleeContact firstContact = findFirstMeleeContact(
               context.level(), player, start, end);
         if (firstContact == null
               || firstContact.centerKey() != context.pos().asLong()) {
            continue;
         }
         lastMeleeHits.put(key, gameTime);
         float damage = (float)Math.max(1.0D,
               player.getAttributeValue(Attributes.ATTACK_DAMAGE));
         if (!context.center().absorbBarrierDamage(damage)) {
            return;
         }
         MELEE_CREDITS_BY_LEVEL.computeIfAbsent(
               context.level(), ignored -> new HashMap<>())
               .put(key, new MeleeCredit(gameTime, damage));
         spawnImpact(context.level(), firstContact.point());
      }
   }

   private static boolean isStandaloneMeleeSwing(ServerPlayer player) {
      var stack = player.getMainHandItem();
      if (stack.getItem() instanceof FuchenItem
            || stack.getItem() instanceof ShihunfanItem
            || stack.getItem() instanceof WhipItem
            || stack.getItem() instanceof ThrowingKnifeItem
            || stack.getItem() instanceof XiujianItem
            || XiujianItem.isEquipped(player)) {
         return false;
      }
      return stack.isEmpty() || QiInfusedWeapon.isWeapon(stack);
   }

   private static MeleeContact findFirstMeleeContact(ServerLevel level,
                                                      ServerPlayer player,
                                                      Vec3 start, Vec3 end) {
      Map<Long, FieldState> fields = FIELDS_BY_LEVEL.get(level);
      if (fields == null || fields.isEmpty()) {
         return null;
      }
      long gameTime = level.getGameTime();
      MeleeContact first = null;
      for (Map.Entry<Long, FieldState> entry : fields.entrySet()) {
         FieldState field = entry.getValue();
         if (field.lastSeen() < gameTime - STATE_TIMEOUT
               || !level.hasChunkAt(field.pos())
               || !(level.getBlockEntity(field.pos())
                     instanceof MagicCenterBlockEntity center)
               || !center.isActive()
               || !MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())
               || mayCross(center, player)) {
            continue;
         }
         Vec3 fieldCenter = Vec3.atCenterOf(field.pos());
         double parameter = firstSphereIntersectionParameter(
               start, end, fieldCenter, field.radius());
         if (Double.isNaN(parameter)) {
            continue;
         }
         Vec3 point = start.add(end.subtract(start).scale(parameter));
         if (!hasClearPath(level, player, start, point)
               || first != null && (parameter > first.parameter() + 1.0E-7D
                     || Math.abs(parameter - first.parameter()) <= 1.0E-7D
                     && entry.getKey() >= first.centerKey())) {
            continue;
         }
         first = new MeleeContact(parameter, entry.getKey(), point);
      }
      return first;
   }

   private static Contact findBodyContact(Entity entity, Vec3 start, Vec3 end,
                                          Vec3 center, double fieldRadius,
                                          boolean keepInside) {
      double maximumSupport = radialSupportUpperBound(entity);
      if (keepInside && maximumSupport >= fieldRadius - CONTACT_EPSILON) {
         Vec3 normal = radialDirection(end, start, center);
         return new Contact(true, end, normal, 0.2D);
      }
      if (!keepInside && entity instanceof ForcefieldInterceptable) {
         Vec3 closest = segmentClosestPoint(start, end, center);
         Vec3 normal = radialDirection(closest, start, center);
         return findContact(start, end, center,
               fieldRadius + radialSupport(entity, normal), false);
      }
      return findContact(start, end, center,
            effectiveRadius(fieldRadius, maximumSupport, keepInside), keepInside);
   }

   private static Contact findContact(Vec3 start, Vec3 end, Vec3 center,
                                      double radius, boolean keepInside) {
      double startDistanceSquared = start.distanceToSqr(center);
      double endDistanceSquared = end.distanceToSqr(center);
      double radiusSquared = radius * radius;
      boolean endpointViolation = keepInside
            ? endDistanceSquared > radiusSquared
            : endDistanceSquared < radiusSquared;
      boolean tunneled = !keepInside
            && startDistanceSquared >= radiusSquared
            && endDistanceSquared >= radiusSquared
            && segmentMinimumDistanceSquared(start, end, center)
                  < radiusSquared - 1.0E-8D;
      if (!endpointViolation && !tunneled) {
         return Contact.NONE;
      }
      Vec3 hit = sphereImpact(start, end, center, radius);
      Vec3 normal = radialDirection(hit, end, center);
      return new Contact(true, hit, normal, radius);
   }

   private static Vec3 sphereImpact(Vec3 start, Vec3 end, Vec3 center, double radius) {
      Vec3 segment = end.subtract(start);
      double parameter = firstSphereIntersectionParameter(start, end, center, radius);
      if (Double.isNaN(parameter)) {
         return end;
      }
      return start.add(segment.scale(parameter));
   }

   private static double firstSphereIntersectionParameter(Vec3 start, Vec3 end,
                                                           Vec3 center, double radius) {
      Vec3 segment = end.subtract(start);
      double a = segment.lengthSqr();
      if (a < 1.0E-9D) {
         return Double.NaN;
      }
      Vec3 offset = start.subtract(center);
      double b = 2.0D * offset.dot(segment);
      double c = offset.lengthSqr() - radius * radius;
      double discriminant = b * b - 4.0D * a * c;
      if (discriminant < 0.0D) {
         return Double.NaN;
      }
      double root = Math.sqrt(discriminant);
      double first = (-b - root) / (2.0D * a);
      double second = (-b + root) / (2.0D * a);
      if (first >= -1.0E-7D && first <= 1.0D + 1.0E-7D) {
         return Math.max(0.0D, Math.min(1.0D, first));
      }
      if (second >= -1.0E-7D && second <= 1.0D + 1.0E-7D) {
         return Math.max(0.0D, Math.min(1.0D, second));
      }
      return Double.NaN;
   }

   private static boolean intersectsSphere(Vec3 start, Vec3 end, Vec3 center,
                                           double radius) {
      return segmentMinimumDistanceSquared(start, end, center) <= radius * radius;
   }

   private static double segmentMinimumDistanceSquared(Vec3 start, Vec3 end,
                                                        Vec3 center) {
      return segmentClosestPoint(start, end, center).distanceToSqr(center);
   }

   private static Vec3 segmentClosestPoint(Vec3 start, Vec3 end, Vec3 center) {
      Vec3 segment = end.subtract(start);
      double lengthSquared = segment.lengthSqr();
      if (lengthSquared < 1.0E-9D) {
         return start;
      }
      double t = Math.max(0.0D, Math.min(1.0D,
            center.subtract(start).dot(segment) / lengthSquared));
      return start.add(segment.scale(t));
   }

   private static boolean classifySide(Entity entity, Vec3 point, Vec3 center,
                                       double radius) {
      Vec3 normal = radialDirection(point, point, center);
      double support = radialSupport(entity, normal);
      double distance = point.distanceTo(center);
      if (distance < radius - support) {
         return true;
      }
      if (distance > radius + support) {
         return false;
      }
      double radialVelocity = entity.getDeltaMovement().dot(normal);
      if (radialVelocity > 1.0E-4D) {
         return true;
      }
      if (radialVelocity < -1.0E-4D) {
         return false;
      }
      // At an ambiguous cold-start contact, keep the entity on the secure/outside side.
      return false;
   }

   private static boolean classifyInitialSide(Entity entity, Vec3 point, Vec3 center,
                                              double radius, long centerKey,
                                              Map<CollisionKey, CollisionState> collisions,
                                              long gameTime) {
      Entity attackOwner = forcefieldOwner(entity);
      if (attackOwner != null && attackOwner.level() == entity.level()) {
         Entity source = attackOwner.getRootVehicle();
         CollisionState sourceState = collisions.get(
               new CollisionKey(centerKey, source.getUUID()));
         if (sourceState != null && sourceState.lastSeen() >= gameTime - STATE_TIMEOUT) {
            return sourceState.keepInside();
         }
         return classifySide(source, collisionBox(source).getCenter(), center, radius);
      }
      return classifySide(entity, point, center, radius);
   }

   private static double radialSupport(Entity entity, Vec3 normal) {
      if (entity instanceof ForcefieldInterceptable interceptable) {
         return Math.max(0.12D, interceptable.forcefieldSupport(normal)) + 0.05D;
      }
      AABB box = collisionBox(entity);
      double halfX = box.getXsize() * 0.5D;
      double halfY = box.getYsize() * 0.5D;
      double halfZ = box.getZsize() * 0.5D;
      return Math.max(0.12D, Math.abs(normal.x) * halfX
            + Math.abs(normal.y) * halfY + Math.abs(normal.z) * halfZ) + 0.05D;
   }

   private static double radialSupportUpperBound(Entity entity) {
      if (entity instanceof ForcefieldInterceptable interceptable) {
         return Math.max(0.12D, interceptable.forcefieldMaximumSupport()) + 0.05D;
      }
      AABB box = collisionBox(entity);
      double halfX = box.getXsize() * 0.5D;
      double halfY = box.getYsize() * 0.5D;
      double halfZ = box.getZsize() * 0.5D;
      return Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ) + 0.05D;
   }

   private static AABB collisionBox(Entity entity) {
      AABB box = entity.getBoundingBox();
      if (entity.isVehicle()) {
         double extraWidth = 0.0D;
         double extraHeight = 0.0D;
         for (Entity passenger : entity.getIndirectPassengers()) {
            extraWidth = Math.max(extraWidth, passenger.getBbWidth());
            extraHeight = Math.max(extraHeight, passenger.getBbHeight());
         }
         if (extraWidth > 0.0D || extraHeight > 0.0D) {
            box = new AABB(box.minX - extraWidth, box.minY, box.minZ - extraWidth,
                  box.maxX + extraWidth, box.maxY + extraHeight,
                  box.maxZ + extraWidth);
         }
      }
      return box;
   }

   private static Vec3 forcefieldCenter(Entity entity) {
      return entity instanceof ForcefieldInterceptable interceptable
            ? interceptable.forcefieldCenter() : collisionBox(entity).getCenter();
   }

   private static Vec3 forcefieldMovement(Entity entity) {
      return entity instanceof ForcefieldInterceptable interceptable
            ? interceptable.forcefieldMovement() : entity.getDeltaMovement();
   }

   private static Vec3 tickPrediction(Entity entity) {
      return entity instanceof Projectile && isAbsorbableAttack(entity)
            ? Vec3.ZERO : forcefieldMovement(entity);
   }

   private static double effectiveRadius(double radius, double support,
                                         boolean keepInside) {
      return keepInside ? Math.max(0.2D, radius - support) : radius + support;
   }

   private static double bodyBoundaryRadius(Entity entity, double fieldRadius,
                                            Vec3 normal, boolean keepInside) {
      if (!keepInside && entity instanceof ForcefieldInterceptable) {
         return fieldRadius + radialSupport(entity, normal);
      }
      return effectiveRadius(
            fieldRadius, radialSupportUpperBound(entity), keepInside);
   }

   private static Vec3 boundaryPoint(Vec3 center, Vec3 normal, double radius,
                                     boolean keepInside) {
      double offsetRadius = keepInside
            ? Math.max(0.2D, radius - CONTACT_EPSILON)
            : radius + CONTACT_EPSILON;
      return center.add(normal.scale(offsetRadius));
   }

   private static Vec3 radialDirection(Vec3 preferred, Vec3 fallback, Vec3 center) {
      Vec3 radial = preferred.subtract(center);
      if (radial.lengthSqr() < 1.0E-8D) {
         radial = fallback.subtract(center);
      }
      return radial.lengthSqr() < 1.0E-8D
            ? new Vec3(1.0D, 0.0D, 0.0D) : radial.normalize();
   }

   private static Vec3 centerPointAt(Entity entity, Vec3 feetPosition) {
      Vec3 currentCenter = forcefieldCenter(entity);
      return feetPosition.add(currentCenter.subtract(entity.position()));
   }

   private static void moveEntityCenter(ServerLevel level, Entity entity, Vec3 targetCenter,
                                        Vec3 fieldCenter) {
      Vec3 currentCenter = forcefieldCenter(entity);
      Vec3 requestedMove = targetCenter.subtract(currentCenter);
      Vec3 safeMove = collisionSafeTranslation(level, entity, requestedMove);
      if (safeMove.distanceToSqr(requestedMove) > 1.0E-6D) {
         Vec3 routedMove = findClearBoundaryRoute(
               level, entity, currentCenter, targetCenter, fieldCenter);
         if (routedMove != null) {
            safeMove = routedMove;
         }
      }
      Vec3 targetFeet = entity.position().add(safeMove);
      if (entity instanceof ServerPlayer player) {
         player.teleportTo(level, targetFeet.x, targetFeet.y, targetFeet.z,
               player.getYRot(), player.getXRot());
      } else {
         entity.teleportTo(targetFeet.x, targetFeet.y, targetFeet.z);
      }
   }

   private static Vec3 findClearBoundaryRoute(ServerLevel level, Entity entity,
                                               Vec3 currentCenter, Vec3 targetCenter,
                                               Vec3 fieldCenter) {
      Vec3 normal = radialDirection(targetCenter, currentCenter, fieldCenter);
      Vec3 tangent = new Vec3(-normal.z, 0.0D, normal.x);
      if (tangent.lengthSqr() < 1.0E-8D) {
         tangent = new Vec3(1.0D, 0.0D, 0.0D);
      } else {
         tangent = tangent.normalize();
      }
      Vec3 bitangent = normal.cross(tangent).normalize();
      double targetRadius = targetCenter.distanceTo(fieldCenter);
      for (int degrees = 10; degrees <= 90; degrees += 10) {
         double radians = Math.toRadians(degrees);
         double cos = Math.cos(radians);
         double sin = Math.sin(radians);
         Vec3[] directions = {
               normal.scale(cos).add(tangent.scale(sin)),
               normal.scale(cos).subtract(tangent.scale(sin)),
               normal.scale(cos).add(bitangent.scale(sin)),
               normal.scale(cos).subtract(bitangent.scale(sin))
         };
         for (Vec3 direction : directions) {
            Vec3 candidateCenter = fieldCenter.add(direction.normalize().scale(targetRadius));
            Vec3 candidateMove = candidateCenter.subtract(currentCenter);
            Vec3 safeMove = collisionSafeTranslation(level, entity, candidateMove);
            if (safeMove.distanceToSqr(candidateMove) <= 1.0E-6D) {
               return candidateMove;
            }
         }
      }
      return null;
   }

   private static Vec3 collisionSafeTranslation(ServerLevel level, Entity entity,
                                                Vec3 requestedMove) {
      double distance = requestedMove.length();
      if (distance < 1.0E-7D) {
         return requestedMove;
      }
      AABB startBox = collisionBox(entity).deflate(1.0E-5D);
      int steps = Math.max(1, (int)Math.ceil(distance / 0.125D));
      Vec3 accepted = Vec3.ZERO;
      for (int step = 1; step <= steps; step++) {
         Vec3 candidate = requestedMove.scale(step / (double)steps);
         if (!level.noCollision(entity, startBox.move(candidate))) {
            break;
         }
         accepted = candidate;
      }
      return accepted;
   }

   private static boolean isMovingIntoWall(Entity entity, Vec3 fallbackMovement,
                                                Vec3 normal, boolean keepInside) {
      Vec3 velocity = forcefieldMovement(entity);
      if (velocity.lengthSqr() < 1.0E-9D) {
         velocity = fallbackMovement;
      }
      double radialVelocity = velocity.dot(normal);
      return keepInside ? radialVelocity > 1.0E-5D : radialVelocity < -1.0E-5D;
   }

   private static void bounceVelocity(Entity entity, Vec3 fallbackMovement,
                                      Vec3 normal, boolean keepInside,
                                      double radiusChange) {
      Vec3 velocity = forcefieldMovement(entity);
      if (velocity.lengthSqr() < 1.0E-9D) {
         velocity = fallbackMovement;
      }
      double speed = velocity.length();
      double radialVelocity = velocity.dot(normal);
      boolean movingIntoWall = keepInside
            ? radialVelocity > 1.0E-5D
            : radialVelocity < -1.0E-5D;
      boolean sweptByWall = keepInside
            ? radiusChange < 0.0D
            : radiusChange > 0.0D;
      if (!movingIntoWall && !sweptByWall) {
         return;
      }

      Vec3 reflected;
      if (movingIntoWall) {
         reflected = velocity.subtract(normal.scale(radialVelocity * 2.0D));
      } else {
         Vec3 tangential = velocity.subtract(normal.scale(radialVelocity));
         double wallSpeed = Math.max(BASE_PUSH, Math.abs(radiusChange));
         reflected = tangential.add(normal.scale(keepInside ? -wallSpeed : wallSpeed));
      }

      double targetSpeed = speed > 1.0E-6D
            ? speed * BOUNCE_SPEED_MULTIPLIER
            : BASE_PUSH;
      if (sweptByWall) {
         targetSpeed = Math.max(targetSpeed,
               Math.abs(radiusChange) * BOUNCE_SPEED_MULTIPLIER);
      }
      if (reflected.lengthSqr() < 1.0E-9D) {
         reflected = normal.scale(keepInside ? -targetSpeed : targetSpeed);
      } else {
         reflected = reflected.normalize().scale(targetSpeed);
      }

      if (entity instanceof ForcefieldInterceptable interceptable) {
         interceptable.forcefieldBounce(reflected);
      } else {
         entity.setDeltaMovement(reflected);
      }
      entity.hurtMarked = true;
      entity.hasImpulse = true;
      if (entity instanceof ServerPlayer player) {
         player.connection.send(new ClientboundSetEntityMotionPacket(player));
      }
   }

   private static boolean hasClearPath(ServerLevel level, Entity entity,
                                       Vec3 start, Vec3 contact) {
      if (start.distanceToSqr(contact) < 1.0E-8D) {
         return true;
      }
      HitResult blockHit = level.clip(new ClipContext(start, contact,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
      return blockHit.getType() == HitResult.Type.MISS
            || blockHit.getLocation().distanceToSqr(start) + 1.0E-4D
            >= contact.distanceToSqr(start);
   }

   private static void purgeCenterState(long centerKey,
                                        Map<CollisionKey, CollisionState> collisions,
                                        Map<CollisionKey, Long> lastMeleeHits) {
      collisions.keySet().removeIf(key -> key.centerPosition() == centerKey);
      lastMeleeHits.keySet().removeIf(key -> key.centerPosition() == centerKey);
   }

   private static void clearCompanionState(ServerLevel level) {
      COLLISIONS_BY_LEVEL.remove(level);
      LAST_MELEE_HITS_BY_LEVEL.remove(level);
      MELEE_CREDITS_BY_LEVEL.remove(level);
      DAMAGE_PULSES_BY_LEVEL.remove(level);
   }

   private static boolean mayCross(MagicCenterBlockEntity center, Entity entity) {
      if (entity instanceof LightningProjectileEntity) {
         return false;
      }
      if (isAbsorbableAttack(entity)) {
         Entity attackOwner = forcefieldOwner(entity);
         return attackOwner != null
               && CenterTargetPolicy.mayCrossForcefield(center, attackOwner);
      }
      boolean hasPassengers = false;
      for (Entity passenger : entity.getIndirectPassengers()) {
         hasPassengers = true;
         if (!CenterTargetPolicy.mayCrossForcefield(center, passenger)) {
            return false;
         }
      }
      if (hasPassengers) {
         return true;
      }
      Entity attackOwner = forcefieldOwner(entity);
      if (attackOwner != null) {
         return CenterTargetPolicy.mayCrossForcefield(center, attackOwner);
      }
      return CenterTargetPolicy.mayCrossForcefield(center, entity);
   }

   private static boolean isAbsorbableAttack(Entity entity) {
      return entity instanceof ForcefieldInterceptable
            || entity instanceof Projectile
            && !entity.isVehicle();
   }

   private static Entity forcefieldOwner(Entity entity) {
      if (entity instanceof ForcefieldInterceptable interceptable) {
         return interceptable.forcefieldOwner();
      }
      return entity instanceof Projectile projectile ? projectile.getOwner() : null;
   }

   private static boolean isRelevant(Entity entity) {
      if (!entity.isAlive() || entity.isPassenger()
            || entity instanceof ItemEntity
            || entity instanceof ExperienceOrb
            || entity instanceof FallingBlockEntity) {
         return false;
      }
      if (entity instanceof Boat boat && boat.getPassengers().isEmpty()) {
         return false;
      }
      if (entity instanceof AbstractMinecart minecart && minecart.getPassengers().isEmpty()) {
         return false;
      }
      return true;
   }

   private static float barrierDamage(Entity entity) {
      if (entity instanceof ForcefieldInterceptable interceptable) {
         return Math.max(1.0F, interceptable.forcefieldBarrierDamage());
      }
      if (entity instanceof AbstractArrow arrow) {
         return (float)Math.max(1.0D, arrow.getBaseDamage());
      }
      return (float)Math.max(1.0D, entity.getDeltaMovement().length() * 4.0D);
   }

   private static void spawnImpact(ServerLevel level, Vec3 position) {
      level.sendParticles(IMPACT_PARTICLE, position.x, position.y, position.z,
            18, 0.35D, 0.55D, 0.35D, 0.01D);
   }

   @Override
   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 3;
   }

   @Override
   public boolean supportsHostileMobSetting() {
      return true;
   }

   @Override
   public boolean supportsPassiveMobSetting() {
      return true;
   }

   private record FieldState(BlockPos pos, double radius, long lastSeen) {
   }

   private record CollisionState(Vec3 lastPosition, boolean keepInside,
                                 long lastSeen, long lastImpact) {
   }

   private record CollisionKey(long centerPosition, UUID entityId) {
   }

   private record Contact(boolean hit, Vec3 point, Vec3 normal, double radius) {
      private static final Contact NONE = new Contact(false, Vec3.ZERO, Vec3.ZERO, 0.0D);
   }

   private record AttackContact(int phase, double parameter, long centerKey,
                                MagicCenterBlockEntity center,
                                CollisionKey collisionKey, Contact contact,
                                boolean keepInside) {
   }

   private record DamageContact(double parameter, long centerKey,
                                MagicCenterBlockEntity center, Contact contact) {
   }

   private record DamagePulseKey(long centerKey, Object sourceToken) {
   }

   private record DamageSourceToken(UUID directEntityId, DamageSourceIdentity identity,
                                    String messageId) {
   }

   private static final class DamageSourceIdentity {
      private final WeakReference<DamageSource> source;
      private final int hash;

      private DamageSourceIdentity(DamageSource source) {
         this.source = new WeakReference<>(source);
         this.hash = System.identityHashCode(source);
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         }
         if (!(other instanceof DamageSourceIdentity identity)) {
            return false;
         }
         DamageSource current = source.get();
         return current != null && current == identity.source.get();
      }

      @Override
      public int hashCode() {
         return hash;
      }
   }

   private record DamagePulseState(long gameTime, float maxDamage) {
   }

   private record ExplosionProtection(Vec3 center, double radiusSquared) {
   }

   private record MeleeContact(double parameter, long centerKey, Vec3 point) {
   }

   private record MeleeCredit(long gameTime, float remainingDamage) {
   }
}
