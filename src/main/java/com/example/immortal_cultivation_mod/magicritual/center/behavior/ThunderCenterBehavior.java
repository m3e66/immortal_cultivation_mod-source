package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

public final class ThunderCenterBehavior implements MagicCenterBehavior {
   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 60)) {
         List<LivingEntity> targets = CenterBehaviorUtil.livingEntities(context).stream().filter((entity) -> CenterBehaviorUtil.hostile(context, entity)).sorted(Comparator.comparingInt(CenterEffectSupport::currentQi).reversed()).toList();
         if (!targets.isEmpty()) {
            int attackCount = context.center().getAttackCount();
            Map<LivingEntity, Integer> hitsByTarget = new LinkedHashMap();

            for(int attack = 0; attack < attackCount; ++attack) {
               LivingEntity target = (LivingEntity)targets.get(attack % targets.size());
               hitsByTarget.merge(target, 1, Integer::sum);
            }

            for(Map.Entry<LivingEntity, Integer> entry : hitsByTarget.entrySet()) {
               strike(context, (LivingEntity)entry.getKey(), (Integer)entry.getValue());
            }

         }
      }
   }

   private static void strike(CenterContext context, LivingEntity target, int hitCount) {
      for(int hit = 0; hit < hitCount; ++hit) {
         LightningBolt lightning = (LightningBolt)EntityType.LIGHTNING_BOLT.create(context.level());
         if (lightning != null) {
            lightning.moveTo(target.position());
            lightning.setVisualOnly(true);
            context.level().addFreshEntity(lightning);
         }
      }

      float damage = 20.0F + target.getMaxHealth() * 0.01F;
      if (target.isInWaterRainOrBubble() || target.getTicksFrozen() > 0 || target.hasEffect(ModEffects.FROZEN) || target.hasEffect(ModEffects.FROZEN_QI)) {
         damage *= 1.25F;
      }

      if (CenterEffectSupport.hasMethodElement(target, "\u96f7")) {
         damage *= 0.5F;
      }

      target.hurt(target.damageSources().magic(), damage * (float)hitCount);
      CenterEffectSupport.suppressQiRegeneration(target, 100);
      SpellImpactParticles.lightning(context.level(), target.position());
      context.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + (double)target.getBbHeight() * (double)0.5F, target.getZ(), Math.min(144, 36 * hitCount), 0.55, 0.8, 0.55, 0.12);
   }

   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 4;
   }

   public boolean supportsHostileMobSetting() {
      return true;
   }

   public boolean supportsPassiveMobSetting() {
      return true;
   }
}
