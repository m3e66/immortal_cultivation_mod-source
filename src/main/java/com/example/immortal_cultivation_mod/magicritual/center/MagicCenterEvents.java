package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class MagicCenterEvents {
   private MagicCenterEvents() {
   }

   @SubscribeEvent
   public static void onReload(AddReloadListenerEvent event) {
      event.addListener(CenterRitualManager.INSTANCE);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLeftClickCenter(PlayerInteractEvent.LeftClickBlock event) {
      if (!event.getEntity().getMainHandItem().is(ModItems.RITUAL_DAGGER.get())
            || !(event.getLevel().getBlockEntity(event.getPos())
            instanceof MagicCenterBlockEntity)) {
         return;
      }
      event.setCanceled(true);
      if (!event.getLevel().isClientSide
            && event.getEntity() instanceof ServerPlayer player
            && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
         CenterTransformationService.activate(
               player, event.getPos(), player.getMainHandItem());
      }
   }

   @SubscribeEvent
   public static void onLivingHeal(LivingHealEvent event) {
      event.setAmount(CenterEffectSupport.adjustHealing(event.getEntity(), event.getAmount()));
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (ForcefieldCenterBehavior.interceptDamage(
            event.getEntity(), event.getSource(), event.getAmount())) {
         event.setCanceled(true);
         return;
      }
      event.setAmount(CenterEffectSupport.adjustIncomingDamage(
            event.getEntity(), event.getSource(), event.getAmount()));
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
      if (event.getLevel() instanceof ServerLevel level) {
         ForcefieldCenterBehavior.protectBlocksFromExplosion(
               level, event.getExplosion().center(), event.getAffectedBlocks());
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
      if (ForcefieldCenterBehavior.interceptLightningStrike(
            event.getEntity(), event.getLightning())) {
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
      if (ForcefieldCenterBehavior.interceptProjectileImpact(
            event.getProjectile(), event.getRayTraceResult().getLocation())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onEntityTickPost(EntityTickEvent.Post event) {
      ForcefieldCenterBehavior.enforceEntityPostTick(event.getEntity());
   }
}
