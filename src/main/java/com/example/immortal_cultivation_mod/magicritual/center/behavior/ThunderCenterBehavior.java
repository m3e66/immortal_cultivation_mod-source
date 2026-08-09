package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
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
   @Override
   public void serverTick(CenterContext context) {
      if (!context.center().shouldTriggerEvery(context.level().getGameTime(), 60)) {
         return;
      }
      List<LivingEntity> targets = CenterBehaviorUtil.livingEntities(context).stream()
            .filter(entity -> CenterBehaviorUtil.hostile(context, entity))
            .sorted(Comparator.comparingInt(CenterEffectSupport::currentQi).reversed())
            .toList();
      if (targets.isEmpty()) {
         return;
      }

      int attackCount = context.center().getAttackCount();
      Map<LivingEntity, Integer> hitsByTarget = new LinkedHashMap<>();
      for (int attack = 0; attack < attackCount; attack++) {
         LivingEntity target = targets.get(attack % targets.size());
         hitsByTarget.merge(target, 1, Integer::sum);
      }
      for (Map.Entry<LivingEntity, Integer> entry : hitsByTarget.entrySet()) {
         strike(context, entry.getKey(), entry.getValue());
      }
   }

   private static void strike(CenterContext context, LivingEntity target, int hitCount) {
      for (int hit = 0; hit < hitCount; hit++) {
         LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(context.level());
         if (lightning != null) {
            lightning.moveTo(target.position());
            lightning.setVisualOnly(true);
            context.level().addFreshEntity(lightning);
         }
      }

      float damage = 20.0F + target.getMaxHealth() * 0.01F;
      if (target.isInWaterRainOrBubble() || target.getTicksFrozen() > 0
            || target.hasEffect(ModEffects.FROZEN)
            || target.hasEffect(ModEffects.FROZEN_QI)) {
         damage *= 1.25F;
      }
      if (CenterEffectSupport.hasMethodElement(target, SpiritRoots.THUNDER)) {
         damage *= 0.5F;
      }
      target.hurt(target.damageSources().magic(), damage * hitCount);
      CenterEffectSupport.suppressQiRegeneration(target, 100);
      SpellImpactParticles.lightning(context.level(), target.position());
      context.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(),
            target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
            Math.min(144, 36 * hitCount), 0.55D, 0.8D, 0.55D, 0.12D);
   }

   @Override
   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 4;
   }

   @Override public boolean supportsHostileMobSetting() { return true; }
   @Override public boolean supportsPassiveMobSetting() { return true; }
}
