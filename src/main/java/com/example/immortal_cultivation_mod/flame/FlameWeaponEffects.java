package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FlyingNeedleEntity;
import com.example.immortal_cultivation_mod.entity.ThrowingKnifeEntity;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Applies cultivation flame procs only after the actual server damage pipeline accepts a hit. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FlameWeaponEffects {
    private record Hit(ItemStack weapon, Entity target) { }
    private static final ThreadLocal<Hit> PROJECTILE_HIT = new ThreadLocal<>();

    private FlameWeaponEffects() { }

    /** Flying swords use a player-attack damage source, so preserve their own weapon during that call. */
    public static boolean hurtWithWeapon(ItemStack weapon, Entity target, DamageSource source, float damage) {
        Hit previous = PROJECTILE_HIT.get();
        PROJECTILE_HIT.set(new Hit(weapon, target));
        try {
            return target.hurt(source, damage);
        } finally {
            if (previous == null) PROJECTILE_HIT.remove();
            else PROJECTILE_HIT.set(previous);
        }
    }

    @SubscribeEvent
    public static void onAcceptedHit(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !Float.isFinite(event.getNewDamage()) || event.getNewDamage() <= 0) return;
        ItemStack weapon = ItemStack.EMPTY;
        Hit scoped = PROJECTILE_HIT.get();
        Entity direct = event.getSource().getDirectEntity();
        if (scoped != null && scoped.target() == target) {
            weapon = scoped.weapon();
        } else if ((direct instanceof FlyingNeedleEntity || direct instanceof ThrowingKnifeEntity)
                && direct instanceof ThrowableItemProjectile projectile) {
            weapon = projectile.getItem();
        } else if (event.getSource().getEntity() instanceof LivingEntity attacker && direct == attacker
                && (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK))) {
            weapon = attacker.getMainHandItem();
        }
        ForgingSystem.applyAcceptedWeaponHitEffects(weapon, target);
    }
}
