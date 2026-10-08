package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(
   modid = "immortal_cultivation_mod"
)
public final class MagicCenterEvents {
   private MagicCenterEvents() {
   }

   @SubscribeEvent
   public static void onReload(AddReloadListenerEvent event) {
      event.addListener(CenterRitualManager.INSTANCE);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onLeftClickCenter(PlayerInteractEvent.LeftClickBlock event) {
      if (event.getEntity().getMainHandItem().is((Item)ModItems.RITUAL_DAGGER.get()) && event.getLevel().getBlockEntity(event.getPos()) instanceof MagicCenterBlockEntity) {
         event.setCanceled(true);
         if (!event.getLevel().isClientSide) {
            Player var2 = event.getEntity();
            if (var2 instanceof ServerPlayer) {
               ServerPlayer player = (ServerPlayer)var2;
               if (event.getAction() == Action.START) {
                  CenterTransformationService.activate(player, event.getPos(), player.getMainHandItem());
               }
            }
         }

      }
   }

   @SubscribeEvent
   public static void onLivingHeal(LivingHealEvent event) {
      event.setAmount(CenterEffectSupport.adjustHealing(event.getEntity(), event.getAmount()));
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (ForcefieldCenterBehavior.interceptDamage(event.getEntity(), event.getSource(), event.getAmount())) {
         event.setCanceled(true);
      } else {
         event.setAmount(CenterEffectSupport.adjustIncomingDamage(event.getEntity(), event.getSource(), event.getAmount()));
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
      Level var2 = event.getLevel();
      if (var2 instanceof ServerLevel level) {
         ForcefieldCenterBehavior.protectBlocksFromExplosion(level, event.getExplosion().center(), event.getAffectedBlocks());
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
      if (ForcefieldCenterBehavior.interceptLightningStrike(event.getEntity(), event.getLightning())) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public static void onEntityTickPre(EntityTickEvent.Pre event) {
      if (ForcefieldCenterBehavior.enforceEntityPreTick(event.getEntity())) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      if (ForcefieldCenterBehavior.interceptProjectileImpact(event.getProjectile(), event.getRayTraceResult().getLocation())) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public static void onEntityTickPost(EntityTickEvent.Post event) {
      ForcefieldCenterBehavior.enforceEntityPostTick(event.getEntity());
   }
}
