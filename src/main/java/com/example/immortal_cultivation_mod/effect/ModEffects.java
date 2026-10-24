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

    public static final DeferredHolder<MobEffect, MobEffect> NPC_WEAPON_GUARD =
            MOB_EFFECTS.register("npc_weapon_guard", NpcWeaponGuardEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SPRING_RECOVERY =
            MOB_EFFECTS.register("spring_recovery", SpringRecoveryEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> LINGBENG =
            MOB_EFFECTS.register("lingbeng", LingbengEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> EARTH_ESCAPE =
            MOB_EFFECTS.register("earth_escape", EarthEscapeEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> QI_GATHERING =
            MOB_EFFECTS.register("qi_gathering", QiGatheringEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> HUTI_QI =
            MOB_EFFECTS.register("huti_qi", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xBFEFFF) {});

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

    public static final DeferredHolder<MobEffect, MobEffect> SPIRIT_FLAME_BURN =
            MOB_EFFECTS.register("spirit_flame_burn", com.example.immortal_cultivation_mod.flame.SpiritFlameBurnEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> QI_FIRE_BURN =
            MOB_EFFECTS.register("qi_fire_burn", QiFireBurnEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> EMBER_BURN =
            MOB_EFFECTS.register("ember_burn", EmberBurnEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> TECHNIQUE_SLOW =
            MOB_EFFECTS.register("technique_slow", TechniqueSlowEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> BONE_BELL =
            MOB_EFFECTS.register("bone_bell", BoneBellEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> MULING_SHENGXI =
            MOB_EFFECTS.register("muling_shengxi", () -> new CultivationHealingEffect(
                    CultivationHealingEffect.Kind.WOOD_BREATH, 0x42B85A));

    public static final DeferredHolder<MobEffect, MobEffect> XUE_GUIYUAN_RECOVERY =
            MOB_EFFECTS.register("xue_guiyuan_recovery", () -> new CultivationHealingEffect(
                    CultivationHealingEffect.Kind.BLOOD_RETURN, 0xB83E45));

    public static final DeferredHolder<MobEffect, MobEffect> STABILIZED =
            MOB_EFFECTS.register("stabilized", StabilizedEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> XUYING_TA =
            MOB_EFFECTS.register("xuying_ta", XuyingTaEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> XINLIAO =
            MOB_EFFECTS.register("xinliao", XinliaoEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SHIXU_CHILIU =
            MOB_EFFECTS.register("shixu_chiliu", ShixuChiliuEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SOLAR_BODY =
            MOB_EFFECTS.register("solar_body", SolarBodyEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> XUYANG =
            MOB_EFFECTS.register("xuyang", XuyangEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> DAMP =
            MOB_EFFECTS.register("damp", DampEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SPIRIT_SEAL =
            MOB_EFFECTS.register("spirit_seal", SpiritSealEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> JINGSHI =
            MOB_EFFECTS.register("jingshi", () ->
                    new CorpseStatusEffect("jingshi", 0x344832, 0.0D));

    public static final DeferredHolder<MobEffect, MobEffect> MERIDIAN_CORPSE_STAGNATION =
            MOB_EFFECTS.register("meridian_corpse_stagnation", () ->
                    new CorpseStatusEffect("meridian_corpse_stagnation", 0x294A31, -0.20D));

    public static final DeferredHolder<MobEffect, MobEffect> CORPSE_MIGHT =
            MOB_EFFECTS.register("corpse_might", () ->
                    new CorpseStatusEffect("corpse_might", 0x26332A, -0.25D, -0.20D));

    public static final DeferredHolder<MobEffect, MobEffect> CORPSE_FIRE =
            MOB_EFFECTS.register("corpse_fire", () ->
                    new CorpseBurnEffect(0x194F48, 25.0F));

    public static final DeferredHolder<MobEffect, MobEffect> BA_FIRE =
            MOB_EFFECTS.register("ba_fire", () ->
                    new CorpseBurnEffect(0x8E240E, 300_000.0F));

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
