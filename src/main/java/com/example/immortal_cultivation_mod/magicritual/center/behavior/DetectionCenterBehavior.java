package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.entity.SoulEntity;
import com.example.immortal_cultivation_mod.item.IdentityItems;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class DetectionCenterBehavior implements MagicCenterBehavior {
   private static final DustParticleOptions SOUL_MARK = new DustParticleOptions(new Vector3f(0.35F, 0.85F, 1.0F), 0.9F);
   private static final DustParticleOptions EVIL_MARK = new DustParticleOptions(new Vector3f(0.65F, 0.08F, 0.7F), 0.9F);
   private static final DustParticleOptions CORPSE_MARK = new DustParticleOptions(new Vector3f(0.35F, 0.8F, 0.28F), 0.9F);
   private static final DustParticleOptions DEMONIC_MARK = new DustParticleOptions(new Vector3f(0.7F, 0.05F, 0.12F), 0.9F);
   private final Map<ServerLevel, Map<Long, Set<UUID>>> presentByCenter = new WeakHashMap();

   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         AABB bounds = CenterBehaviorUtil.effectColumn(context);
         Set<UUID> nowPresent = new HashSet();
         ServerPlayer owner = context.center().getOwnerUuid() == null ? null : context.level().getServer().getPlayerList().getPlayer(context.center().getOwnerUuid());

         for(LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
            if (entity instanceof ServerPlayer) {
               ServerPlayer player = (ServerPlayer)entity;
               if (context.center().isOwner(player) || context.center().isPlayerExcluded(player)) {
                  player.addEffect(new MobEffectInstance(ModEffects.SPIRIT_SIGHT, 50, 0, true, false, true));
                  continue;
               }
            }

            if (context.center().isAffectedByCircle(entity)) {
               if (entity instanceof ServerPlayer) {
                  ServerPlayer player = (ServerPlayer)entity;
                  if (player.isInvisible() && owner != null && CultivationLevels.getRealmIndex(CenterEffectSupport.realmName(player)) >= CultivationLevels.getRealmIndex(CenterEffectSupport.realmName(owner)) + 2) {
                     continue;
                  }
               }

               entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false, true));
               if (entity instanceof ServerPlayer) {
                  ServerPlayer player = (ServerPlayer)entity;
                  nowPresent.add(player.getUUID());
               }

               DustParticleOptions marker = identityMarker(entity);
               if (marker != null || entity instanceof Enemy) {
                  context.level().sendParticles(marker == null ? EVIL_MARK : marker, entity.getX(), entity.getY() + (double)entity.getBbHeight(), entity.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
               }
            }
         }

         for(Entity entity : context.level().getEntities((Entity)null, bounds, (candidate) -> (candidate instanceof SoulEntity || candidate instanceof CultivatorCorpseEntity) && CenterBehaviorUtil.insideEffectColumn(context, candidate))) {
            context.level().sendParticles(entity instanceof SoulEntity ? SOUL_MARK : CORPSE_MARK, entity.getX(), entity.getY() + 0.6, entity.getZ(), 4, 0.2, (double)0.25F, 0.2, 0.01);
         }

         Map<Long, Set<UUID>> levelPresence = (Map)this.presentByCenter.computeIfAbsent(context.level(), (ignored) -> new HashMap());
         Set<UUID> previous = (Set)levelPresence.getOrDefault(context.pos().asLong(), Set.of());
         if (owner != null) {
            for(UUID id : nowPresent) {
               if (!previous.contains(id)) {
                  ServerPlayer intruder = context.level().getServer().getPlayerList().getPlayer(id);
                  if (intruder != null) {
                     owner.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center_intruder", new Object[]{intruder.getName(), CultivationLevels.displayName(CenterEffectSupport.realmName(intruder))}), false);
                  }
               }
            }
         }

         levelPresence.put(context.pos().asLong(), nowPresent);
      }
   }

   private static DustParticleOptions identityMarker(LivingEntity entity) {
      String var10000;
      if (entity instanceof ServerPlayer player) {
         var10000 = IdentityItems.ensurePlayerIdentity(player);
      } else if (entity instanceof CultivationNpc npc) {
         var10000 = npc.npcIdentity();
      } else {
         var10000 = "";
      }

      DustParticleOptions var5;
      switch (var10000) {
         case "corpse_cultivator":
            var5 = CORPSE_MARK;
            break;
         case "demonic_cultivator":
         case "evil_cultivator":
            var5 = DEMONIC_MARK;
            break;
         default:
            var5 = null;
      }

      return var5;
   }

   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 2;
   }

   public boolean supportsHostileMobSetting() {
      return true;
   }

   public boolean supportsPassiveMobSetting() {
      return true;
   }
}
