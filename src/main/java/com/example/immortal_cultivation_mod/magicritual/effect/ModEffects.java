package com.example.immortal_cultivation_mod.magicritual.effect;

import com.example.immortal_cultivation_mod.magicritual.effect.custom.BleedingEffect;
import com.example.immortal_cultivation_mod.magicritual.effect.custom.FogEffect;
import com.example.immortal_cultivation_mod.magicritual.effect.custom.InvertedControlEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "immortal_cultivation_mod");
   public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register(
      "bleeding", () -> new BleedingEffect(MobEffectCategory.HARMFUL, 9109504)
   );
   public static final DeferredHolder<MobEffect, MobEffect> FOG = EFFECTS.register("fog", () -> new FogEffect(MobEffectCategory.HARMFUL, 16777215));
   public static final DeferredHolder<MobEffect, MobEffect> INVERTED_CONTROL = EFFECTS.register(
      "inverted_control", () -> new InvertedControlEffect(MobEffectCategory.HARMFUL, 8388736)
   );

   public static void register(IEventBus eventBus) {
      EFFECTS.register(eventBus);
   }
}
