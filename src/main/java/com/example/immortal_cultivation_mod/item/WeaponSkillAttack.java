package com.example.immortal_cultivation_mod.item;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;

/** One cast's weapon power, retained by every delayed hit after the held item changes. */
public final class WeaponSkillAttack {
    private record Context(WeaponSkillAttack attack, net.minecraft.world.entity.LivingEntity owner, DamageSource source) {}
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private final ItemStack original;
    private final ItemStack weapon;
    private final float power;
    private final float injectedQi;
    private boolean paid;

    private WeaponSkillAttack(ItemStack stack) {
        WeaponBinding.prepare(stack);
        original = stack;
        weapon = stack.copy();
        injectedQi = QiInfusedWeapon.injectedQiDamage(stack);
        power = (float) Math.max(0.0D, QiInfusedWeapon.baseAttackDamage(stack)) + injectedQi;
    }

    public static WeaponSkillAttack capture(ItemStack stack) {
        return new WeaponSkillAttack(stack);
    }

    /** Percent applies to the forged weapon damage together with its injected Qi bonus. */
    public float damage(float percent) {
        return power * Math.max(0.0F, percent) / 100.0F;
    }

    /** Called only after cast validation/payment succeeds, never once per target or pulse. */
    public void consumeInjectedQi() {
        if (paid) return;
        paid = true;
        QiInfusedWeapon.consumeInjectedQiDamage(original, injectedQi);
    }

    public boolean withWeapon(net.minecraft.world.entity.LivingEntity owner, DamageSource source, BooleanSupplier hit) {
        Context previous = ACTIVE.get();
        ACTIVE.set(new Context(this, owner, source));
        try { return WeaponBinding.withWeapon(weapon, source, hit); }
        finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static boolean isCurrentSource(DamageSource source) {
        Context context = ACTIVE.get();
        return context != null && context.source == source;
    }

    public static ItemStack sourceWeapon(ServerPlayer player, DamageSource source) {
        Context context = ACTIVE.get();
        return context != null && context.owner == player && context.source == source
                ? context.attack.weapon : player.getMainHandItem();
    }
}
