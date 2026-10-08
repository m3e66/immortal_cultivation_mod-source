package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** The effect's synced, saved amplifier stores the stable FlameKind model index. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class SpiritFlameBurnEffect extends MobEffect {
    public static final String DAMAGE_TICK = "icmSpiritFlameContactTick";

    public SpiritFlameBurnEffect() { super(MobEffectCategory.HARMFUL, 0xFF5A32); }

    public static FlameKind kind(LivingEntity entity) {
        var burn = entity.getEffect(ModEffects.SPIRIT_FLAME_BURN);
        if (burn != null) for (var kind : FlameKind.values())
            if (kind.special() && kind.modelIndex() == burn.getAmplifier()) return kind;
        return null;
    }

    /** Called only after an accepted fire hit. One source replaces the previous burn. */
    public static void ignite(LivingEntity entity, FlameKind kind) {
        if (entity.level().isClientSide || !kind.special()) return;
        refresh(entity, kind);
        entity.getPersistentData().putLong(DAMAGE_TICK, entity.level().getGameTime());
    }

    public static void refresh(LivingEntity entity, FlameKind kind) {
        entity.igniteForSeconds(4);
        var current = entity.getEffect(ModEffects.SPIRIT_FLAME_BURN);
        if (current != null && current.getAmplifier() == kind.modelIndex() && current.getDuration() > 60) return;
        entity.removeEffect(ModEffects.SPIRIT_FLAME_BURN);
        entity.addEffect(new MobEffectInstance(ModEffects.SPIRIT_FLAME_BURN, 80,
                kind.modelIndex(), false, false, true));
        entity.addEffect(new MobEffectInstance(ModEffects.QI_FIRE_BURN, 90, 0, false, false, false));
    }

    public static boolean ready(LivingEntity entity) {
        long now = entity.level().getGameTime();
        var data = entity.getPersistentData();
        long previous = data.getLong(DAMAGE_TICK);
        if (data.contains(DAMAGE_TICK) && now >= previous && now - previous < 20) return false;
        data.putLong(DAMAGE_TICK, now);
        return true;
    }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        if (!entity.isAlive() || !entity.isOnFire() || entity.fireImmune() || entity.isSpectator()
                || entity instanceof Player player && player.isCreative()
                || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return false;
        FlameKind kind = kind(entity);
        if (kind == null) return false;
        if (ready(entity)) entity.hurt(entity.damageSources().inFire(), kind.contactDamagePerSecond());
        return true;
    }

    @SubscribeEvent public static void preventDuplicateVanillaBurn(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.ON_FIRE) && kind(event.getEntity()) != null)
            event.setCanceled(true);
    }
}
