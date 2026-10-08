package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class TeleportCenterBehavior implements MagicCenterBehavior {
   public static final int BASE_CHANNEL_TICKS = 100;
   public static final int HASTE_REDUCTION_TICKS = 40;
   public static final int MIN_CHANNEL_TICKS = 10;
   public static final int QI_COST_PER_ENTITY = 100;
   public static final long ENERGY_PER_BLOCK_PER_ENTITY = 10L;
   private static final int AUTOMATIC_RETRY_TICKS = 20;
   private static final double TELEPORT_HEIGHT_LIMIT = (double)5.0F;
   private static final Map<ServerLevel, Map<UUID, AutomaticTeleportState>> AUTOMATIC_TELEPORT_STATES = new WeakHashMap();

   public void serverTick(CenterContext context) {
      if (context.center().isTemporary()) {
         tickAutomaticTeleport(context);
      } else {
         tickChannel(context);
      }

   }

   public int getFuelCostPerMinute(CenterContext context) {
      return 0;
   }

   public static boolean beginChannel(MagicCenterBlockEntity center, ServerPlayer player) {
      Level var3 = center.getLevel();
      if (var3 instanceof ServerLevel level) {
         if (MagicCenterTypeRegistry.TELEPORT.equals(center.getCenterTypeId()) && !center.isTemporary()) {
            if (!CenterTargetPolicy.canEdit(center, player)) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.not_authorized"), true);
               return false;
            }

            if (!center.isEnabled()) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.disabled"), true);
               return false;
            }

            if (!isInsideTeleportArea(center, player)) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.outside_radius"), true);
               return false;
            }

            if (!center.canTeleportOut()) {
               player.displayClientMessage(Component.translatable(center.hasTeleportLink() ? "message.immortal_cultivation_mod.teleport.one_way_blocked" : "message.immortal_cultivation_mod.teleport.not_linked"), true);
               return false;
            }

            if (center.resolveTeleportTarget() == null) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.destination_missing"), true);
               return false;
            }

            if (center.hasTeleportChannel()) {
               player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.busy"), true);
               return false;
            }

            int duration = Math.max(10, 100 - center.countHasteFlags() * 40);
            center.beginTeleportChannel(player.getUUID(), level.getGameTime(), duration, player.getHealth(), player.hurtTime);
            level.playSound((Player)null, center.getBlockPos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.45F);
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.channel_started", new Object[]{String.format(Locale.ROOT, "%.1f", (double)duration / (double)20.0F)}), true);
            return true;
         }
      }

      return false;
   }

   private static void tickAutomaticTeleport(CenterContext context) {
      MagicCenterBlockEntity source = context.center();
      if (source.isEnabled() && source.canTeleportOut()) {
         MagicCenterBlockEntity.ResolvedTeleportTarget resolved = source.resolveTeleportTarget();
         if (resolved == null) {
            clearAutomaticState(context.level(), source.getCenterId());
         } else {
            List<Entity> roots = collectTeleportRoots(context.level(), source);
            Set<UUID> currentRoots = new HashSet();

            for(Entity root : roots) {
               currentRoots.add(root.getUUID());
            }

            AutomaticTeleportState state = automaticState(context.level(), source.getCenterId());
            state.insideRoots.retainAll(currentRoots);
            List<Entity> entrants = roots.stream().filter((rootx) -> !state.insideRoots.contains(rootx.getUUID())).toList();
            if (!entrants.isEmpty()) {
               long gameTime = context.level().getGameTime();
               if (gameTime >= state.nextAttemptTime) {
                  state.nextAttemptTime = gameTime + 20L;
                  UUID ownerId = source.getOwnerUuid();
                  ServerPlayer payer = ownerId == null ? null : context.level().getServer().getPlayerList().getPlayer(ownerId);
                  if (payer != null && payer.isAlive()) {
                     if (completeTeleport(context, payer, entrants, true)) {
                        Set<UUID> movedRoots = new HashSet();

                        for(Entity entrant : entrants) {
                           movedRoots.add(entrant.getUUID());
                        }

                        state.insideRoots.addAll(movedRoots);
                        automaticState(resolved.level(), resolved.center().getCenterId()).insideRoots.addAll(movedRoots);
                     }
                  }
               }
            }
         }
      } else {
         clearAutomaticState(context.level(), source.getCenterId());
      }
   }

   private static void tickChannel(CenterContext context) {
      MagicCenterBlockEntity center = context.center();
      if (center.hasTeleportChannel()) {
         ServerPlayer player = context.level().getServer().getPlayerList().getPlayer(center.getTeleportChannelPlayer());
         if (player != null && player.serverLevel() == context.level() && player.isAlive()) {
            if (center.isEnabled() && CenterTargetPolicy.canEdit(center, player) && isInsideTeleportArea(center, player)) {
               if (!(player.getHealth() < center.getTeleportChannelStartHealth()) && player.hurtTime <= center.getTeleportChannelObservedHurtTime()) {
                  center.observeTeleportHurtTime(player.hurtTime);
                  if (center.canTeleportOut() && center.resolveTeleportTarget() != null) {
                     long elapsed = context.level().getGameTime() - center.getTeleportChannelStartTime();
                     if (elapsed < (long)center.getTeleportChannelDurationTicks()) {
                        if ((context.level().getGameTime() & 3L) == 0L) {
                           double progress = Math.min((double)1.0F, (double)elapsed / (double)Math.max(1, center.getTeleportChannelDurationTicks()));
                           context.level().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + (double)player.getBbHeight() * progress, player.getZ(), 5, 0.45, 0.08, 0.45, 0.015);
                        }

                     } else {
                        completeTeleport(context, player, collectTeleportRoots(context.level(), context.center()), false);
                     }
                  } else {
                     cancel(center, player, "message.immortal_cultivation_mod.teleport.destination_missing");
                  }
               } else {
                  cancel(center, player, "message.immortal_cultivation_mod.teleport.channel_damaged");
               }
            } else {
               cancel(center, player, "message.immortal_cultivation_mod.teleport.channel_moved");
            }
         } else {
            center.clearTeleportChannel();
         }
      }
   }

   private static boolean completeTeleport(CenterContext context, ServerPlayer activator, List<Entity> roots, boolean automatic) {
      MagicCenterBlockEntity source = context.center();
      MagicCenterBlockEntity.ResolvedTeleportTarget resolved = source.resolveTeleportTarget();
      if (resolved == null) {
         reject(source, activator, "message.immortal_cultivation_mod.teleport.destination_missing", automatic);
         return false;
      } else {
         int entityCount = roots.stream().mapToInt(TeleportCenterBehavior::entityTreeSize).sum();
         if (entityCount <= 0) {
            reject(source, activator, "message.immortal_cultivation_mod.teleport.no_entities", automatic);
            return false;
         } else {
            long qiCost = saturatingMultiply(100L, (long)entityCount);
            long energyCost = saturatingMultiply(energyCostPerEntity(source, resolved.center().getBlockPos()), (long)entityCount);
            if (!activator.getAbilities().instabuild) {
               int availableQi = Math.max(0, ModAttachments.getData(activator).qi());
               if (qiCost > (long)availableQi) {
                  reject(source, activator, "message.immortal_cultivation_mod.teleport.insufficient_qi", automatic);
                  return false;
               }

               if (!hasAvailableTeleportEnergy(source, activator, energyCost)) {
                  reject(source, activator, "message.immortal_cultivation_mod.teleport.insufficient_energy", automatic);
                  return false;
               }

               ModAttachments.CultivationData data = ModAttachments.getData(activator);
               ModAttachments.setData(activator, data.withQi(data.qi() - (int)qiCost));
               ServerEvents.syncPlayerData(activator);
               if (!consumeAvailableTeleportEnergy(source, activator, energyCost)) {
                  ModAttachments.setData(activator, data);
                  ServerEvents.syncPlayerData(activator);
                  reject(source, activator, "message.immortal_cultivation_mod.teleport.insufficient_energy", automatic);
                  return false;
               }
            }

            Vec3 sourcePoint = source.centerPoint();
            Vec3 destinationPoint = resolved.center().centerPoint();
            source.clearTeleportChannel();
            context.level().sendParticles(ParticleTypes.PORTAL, sourcePoint.x, sourcePoint.y + 0.8, sourcePoint.z, 70, Math.max((double)0.5F, (double)source.getRadius() * 0.35), 0.8, Math.max((double)0.5F, (double)source.getRadius() * 0.35), 0.15);
            context.level().playSound((Player)null, source.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2F, 0.75F);
            int movedCount = 0;

            for(int index = 0; index < roots.size(); ++index) {
               Entity root = (Entity)roots.get(index);
               if (!root.isRemoved()) {
                  Vec3 offset = root.position().subtract(sourcePoint);
                  double horizontalOffset = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
                  double maxTargetOffset = Math.max((double)0.5F, (double)resolved.center().getRadius() - (double)0.75F);
                  if (horizontalOffset > maxTargetOffset) {
                     double scale = maxTargetOffset / horizontalOffset;
                     offset = new Vec3(offset.x * scale, offset.y, offset.z * scale);
                  }

                  Vec3 preferred = destinationPoint.add(offset);
                  Vec3 destination = findSafeDestination(resolved.level(), root, preferred, resolved.center().getBlockPos(), index);
                  DimensionTransition transition = new DimensionTransition(resolved.level(), destination, Vec3.ZERO, root.getYRot(), root.getXRot(), (moved) -> {
                     moved.setDeltaMovement(Vec3.ZERO);
                     moved.resetFallDistance();
                  });
                  int treeSize = entityTreeSize(root);
                  if (root.changeDimension(transition) != null) {
                     movedCount += treeSize;
                  }
               }
            }

            resolved.level().sendParticles(ParticleTypes.REVERSE_PORTAL, destinationPoint.x, destinationPoint.y + 0.8, destinationPoint.z, 70, Math.max((double)0.5F, (double)resolved.center().getRadius() * 0.35), 0.8, Math.max((double)0.5F, (double)resolved.center().getRadius() * 0.35), 0.15);
            resolved.level().playSound((Player)null, resolved.center().getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2F, 1.25F);
            activator.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.complete", new Object[]{movedCount, qiCost, energyCost}), true);
            return true;
         }
      }
   }

   private static List<Entity> collectTeleportRoots(ServerLevel level, MagicCenterBlockEntity center) {
      double radius = (double)center.getRadius();
      Vec3 centerPoint = center.centerPoint();
      AABB bounds = new AABB(centerPoint.x - radius, centerPoint.y - (double)5.0F, centerPoint.z - radius, centerPoint.x + radius, centerPoint.y + (double)5.0F, centerPoint.z + radius);
      Set<Entity> roots = Collections.newSetFromMap(new IdentityHashMap());

      for(Entity entity : level.getEntities((Entity)null, bounds, (candidate) -> {
         boolean var10000;
         label25: {
            if (candidate.isAlive() && isInsideTeleportArea(center, candidate)) {
               if (!(candidate instanceof Player)) {
                  break label25;
               }

               Player player = (Player)candidate;
               if (!player.isSpectator()) {
                  break label25;
               }
            }

            var10000 = false;
            return var10000;
         }

         var10000 = true;
         return var10000;
      })) {
         Entity root = resolveMultipartParent(entity).getRootVehicle();
         if (isTeleportTreeFullyInside(center, root)) {
            roots.add(root);
         }
      }

      return new ArrayList(roots);
   }

   private static Entity resolveMultipartParent(Entity entity) {
      Entity resolved;
      PartEntity<?> part;
      for(resolved = entity; resolved instanceof PartEntity; resolved = part.getParent()) {
         part = (PartEntity)resolved;
      }

      return resolved;
   }

   private static boolean isTeleportTreeFullyInside(MagicCenterBlockEntity center, Entity root) {
      return root != null && root.getSelfAndPassengers().allMatch((member) -> {
         if (member.isAlive()) {
            if (member instanceof Player) {
               Player player = (Player)member;
               if (player.isSpectator()) {
                  return false;
               }
            }

            if (isInsideTeleportArea(center, member)) {
               if (member.isMultipartEntity() && member.getParts() != null) {
                  for(PartEntity<?> part : member.getParts()) {
                     if (part == null || !part.isAlive() || !isInsideTeleportArea(center, part)) {
                        return false;
                     }
                  }

                  return true;
               }

               return true;
            }
         }

         return false;
      });
   }

   private static boolean isInsideTeleportArea(MagicCenterBlockEntity center, Entity entity) {
      if (center != null && entity != null) {
         Vec3 centerPoint = center.centerPoint();
         AABB box = entity.getBoundingBox();
         double farthestX = Math.max(Math.abs(box.minX - centerPoint.x), Math.abs(box.maxX - centerPoint.x));
         double farthestZ = Math.max(Math.abs(box.minZ - centerPoint.z), Math.abs(box.maxZ - centerPoint.z));
         double radius = (double)center.getRadius();
         return box.minY >= centerPoint.y - (double)5.0F && box.maxY <= centerPoint.y + (double)5.0F && farthestX * farthestX + farthestZ * farthestZ <= radius * radius;
      } else {
         return false;
      }
   }

   private static int entityTreeSize(Entity root) {
      return (int)Math.min(2147483647L, root.getSelfAndPassengers().count());
   }

   public static long energyCostPerEntity(MagicCenterBlockEntity center, BlockPos destinationPos) {
      long distanceBlocks = (long)Math.ceil(center.centerPoint().distanceTo(Vec3.atCenterOf(destinationPos)));
      return adjustedEnergyCost(center, saturatingMultiply(10L, distanceBlocks));
   }

   private static long adjustedEnergyCost(MagicCenterBlockEntity center, long baseCost) {
      double cost = (double)Math.max(0L, baseCost);
      int gathering = center.countSpiritGatheringFlags();
      int haste = center.countHasteFlags();
      int ordinary = Math.max(0, center.countFormationFlags() - gathering - haste);

      for(int i = 0; i < ordinary; ++i) {
         cost *= (double)1.5F;
      }

      for(int i = 0; i < gathering; ++i) {
         cost *= (double)0.75F;
      }

      for(int i = 0; i < haste; ++i) {
         cost *= 1.3;
      }

      return cost >= (double)Long.MAX_VALUE ? Long.MAX_VALUE : (long)Math.ceil(cost);
   }

   private static boolean hasAvailableTeleportEnergy(MagicCenterBlockEntity source, ServerPlayer activator, long requestedEnergy) {
      long cost = Math.max(0L, requestedEnergy);
      if (source.hasTeleportEnergy(cost)) {
         return true;
      } else if (!source.isTemporary()) {
         return false;
      } else {
         long available = sourceAvailableEnergy(source);

         for(int slot = 0; slot < activator.getInventory().getContainerSize(); ++slot) {
            ItemStack stack = activator.getInventory().getItem(slot);
            if (stack.is((Item)ModItems.INFINITE_SPIRIT_STONE.get())) {
               return true;
            }

            int value = CenterFuelController.fuelValue(stack);
            if (value > 0) {
               available = saturatingAdd(available, saturatingMultiply((long)value, (long)stack.getCount()));
               if (available >= cost) {
                  return true;
               }
            }
         }

         return available >= cost;
      }
   }

   private static boolean consumeAvailableTeleportEnergy(MagicCenterBlockEntity source, ServerPlayer activator, long requestedEnergy) {
      long cost = Math.max(0L, requestedEnergy);
      if (source.consumeTeleportEnergy(cost)) {
         return true;
      } else if (source.isTemporary() && hasAvailableTeleportEnergy(source, activator, cost)) {
         for(int slot = 0; slot < activator.getInventory().getContainerSize(); ++slot) {
            if (activator.getInventory().getItem(slot).is((Item)ModItems.INFINITE_SPIRIT_STONE.get())) {
               return true;
            }
         }

         transferTemporaryFuelTier(source, activator, (Item)ModItems.SPIRIT_STONE.get(), 1, cost);
         transferTemporaryFuelTier(source, activator, (Item)ModItems.MEDIUM_SPIRIT_STONE.get(), 10, cost);
         transferTemporaryFuelTier(source, activator, (Item)ModItems.HIGH_SPIRIT_STONE.get(), 100, cost);
         activator.getInventory().setChanged();
         return source.consumeTeleportEnergy(cost);
      } else {
         return false;
      }
   }

   private static void transferTemporaryFuelTier(MagicCenterBlockEntity source, ServerPlayer activator, Item stone, int value, long requestedEnergy) {
      for(int slot = 0; slot < activator.getInventory().getContainerSize(); ++slot) {
         long missing = Math.max(0L, requestedEnergy - sourceAvailableEnergy(source));
         if (missing == 0L) {
            return;
         }

         ItemStack stack = activator.getInventory().getItem(slot);
         if (stack.is(stone)) {
            long neededItems = 1L + (missing - 1L) / (long)value;
            int consumed = (int)Math.min((long)stack.getCount(), neededItems);
            if (consumed > 0) {
               stack.shrink(consumed);
               source.addEnergy(saturatingMultiply((long)value, (long)consumed));
            }
         }
      }

   }

   private static long sourceAvailableEnergy(MagicCenterBlockEntity source) {
      ItemStack storedFuel = source.getFuelSlot().getStackInSlot(0);
      int storedValue = CenterFuelController.fuelValue(storedFuel);
      return saturatingAdd(source.getEnergy(), saturatingMultiply((long)storedValue, (long)storedFuel.getCount()));
   }

   private static Vec3 findSafeDestination(ServerLevel level, Entity entity, Vec3 preferred, BlockPos centerPos, int index) {
      for(int vertical = 0; vertical <= 5; ++vertical) {
         for(int ring = 0; ring <= 4; ++ring) {
            int samples = ring == 0 ? 1 : ring * 8;

            for(int sample = 0; sample < samples; ++sample) {
               double angle = samples == 1 ? (double)0.0F : (Math.PI * 2D) * ((double)sample + (double)index * 0.37) / (double)samples;
               Vec3 candidate = preferred.add(Math.cos(angle) * (double)ring, (double)vertical, Math.sin(angle) * (double)ring);
               BlockPos candidatePos = BlockPos.containing(candidate);
               if (level.getWorldBorder().isWithinBounds(candidatePos)) {
                  level.getChunkAt(candidatePos);
                  AABB movedBox = entity.getBoundingBox().move(candidate.subtract(entity.position()));
                  BlockState below = level.getBlockState(candidatePos.below());
                  if (level.noCollision(entity, movedBox) && (entity.isNoGravity() || below.isFaceSturdy(level, candidatePos.below(), Direction.UP))) {
                     return candidate;
                  }
               }
            }
         }
      }

      return Vec3.atBottomCenterOf(centerPos.above());
   }

   private static void cancel(MagicCenterBlockEntity center, ServerPlayer player, String translationKey) {
      center.clearTeleportChannel();
      Level var4 = center.getLevel();
      if (var4 instanceof ServerLevel level) {
         level.playSound((Player)null, center.getBlockPos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 0.8F);
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + (double)1.0F, player.getZ(), 12, 0.35, 0.45, 0.35, 0.015);
      }

      player.displayClientMessage(Component.translatable(translationKey), true);
   }

   private static void reject(MagicCenterBlockEntity center, ServerPlayer player, String translationKey, boolean automatic) {
      if (automatic) {
         player.displayClientMessage(Component.translatable(translationKey), true);
      } else {
         cancel(center, player, translationKey);
      }

   }

   private static AutomaticTeleportState automaticState(ServerLevel level, UUID centerId) {
      return (AutomaticTeleportState)((Map)AUTOMATIC_TELEPORT_STATES.computeIfAbsent(level, (ignored) -> new HashMap())).computeIfAbsent(centerId, (ignored) -> new AutomaticTeleportState());
   }

   private static void clearAutomaticState(ServerLevel level, UUID centerId) {
      Map<UUID, AutomaticTeleportState> states = (Map)AUTOMATIC_TELEPORT_STATES.get(level);
      if (states != null) {
         states.remove(centerId);
         if (states.isEmpty()) {
            AUTOMATIC_TELEPORT_STATES.remove(level);
         }

      }
   }

   public static void forgetAutomaticCenter(MagicCenterBlockEntity center) {
      Level var2 = center.getLevel();
      if (var2 instanceof ServerLevel level) {
         clearAutomaticState(level, center.getCenterId());
      }

   }

   private static long saturatingMultiply(long left, long right) {
      if (left > 0L && right > 0L) {
         return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
      } else {
         return 0L;
      }
   }

   private static long saturatingAdd(long left, long right) {
      long safeLeft = Math.max(0L, left);
      long safeRight = Math.max(0L, right);
      return safeLeft > Long.MAX_VALUE - safeRight ? Long.MAX_VALUE : safeLeft + safeRight;
   }

   private static final class AutomaticTeleportState {
      private final Set<UUID> insideRoots = new HashSet();
      private long nextAttemptTime;
   }
}
