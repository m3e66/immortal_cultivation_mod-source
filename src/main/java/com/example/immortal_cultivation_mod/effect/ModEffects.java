package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> LINGBENG =
            MOB_EFFECTS.register("lingbeng", LingbengEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> EARTH_ESCAPE =
            MOB_EFFECTS.register("earth_escape", EarthEscapeEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> QI_GATHERING =
            MOB_EFFECTS.register("qi_gathering", QiGatheringEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> WIND_STEP =
            MOB_EFFECTS.register("wind_step", WindStepEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> YUFENG_JUE =
            MOB_EFFECTS.register("yufeng_jue", YufengJueEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SPIRIT_SIGHT =
            MOB_EFFECTS.register("spirit_sight", SpiritSightEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SPELL_DAMAGE_BOOST =
            MOB_EFFECTS.register("spell_damage_boost", SpellDamageBoostEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> WEIYA =
            MOB_EFFECTS.register("weiya", WeiyaEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> WEIYA_SUPPRESSED =
            MOB_EFFECTS.register("weiya_suppressed", WeiyaSuppressedEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> DAZE =
            MOB_EFFECTS.register("daze", DazeEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> DINGSHEN =
            MOB_EFFECTS.register("dingshen", DingshenEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FENGYA =
            MOB_EFFECTS.register("fengya", FengyaEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FROZEN_QI =
            MOB_EFFECTS.register("frozen_qi", FrozenQiEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FROZEN =
            MOB_EFFECTS.register("frozen", FrozenEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FROST_FLIGHT =
            MOB_EFFECTS.register("frost_flight", FrostFlightEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SHIDU =
            MOB_EFFECTS.register("shidu", ShiduEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> QI_FIRE_BURN =
            MOB_EFFECTS.register("qi_fire_burn", QiFireBurnEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> XUYING_TA =
            MOB_EFFECTS.register("xuying_ta", XuyingTaEffect::new);

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
