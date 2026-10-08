package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.RestrictionType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class EquipmentRestrictionEvents {
    private static final java.util.Map<net.neoforged.neoforge.common.damagesource.DamageContainer, Float> ARMOR_DEFENSE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private EquipmentRestrictionEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureArmorDefense(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        // Armor and Curios can break before LivingDamageEvent.Pre. Preserve defense for this hit,
        // keyed by its damage container so nested/cancelled attacks cannot overwrite another hit.
        ARMOR_DEFENSE.put(event.getContainer(),
                WeaponRestrictionSystem.armorDamageMultiplier(event.getEntity(), event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onProjectileDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) return;
        // Use the projectile's launch weapon, so switching held items cannot change the hit.
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !WeaponRestrictionSystem.isWeapon(weapon)) return;
        float damage = event.getAmount();
        damage *= 1.0F + WeaponRestrictionSystem.effectiveCount(weapon, RestrictionType.SHARPNESS) / 100.0F;
        if (event.getEntity().getArmorCoverPercentage() > 0.0F) {
            damage *= 1.0F + WeaponRestrictionSystem.effectiveCount(weapon, RestrictionType.ARMOR_BREAKING) / 100.0F;
        }
        event.setAmount(damage);
        WeaponRestrictionSystem.applyWeaponHitEffects(weapon, event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onArmorDamage(LivingDamageEvent.Pre event) {
        Float defense = ARMOR_DEFENSE.remove(event.getContainer());
        if (event.getEntity().level().isClientSide || event.getNewDamage() <= 0) return;
        event.setNewDamage(event.getNewDamage()
                * (defense != null ? defense : WeaponRestrictionSystem.armorDamageMultiplier(event.getEntity(), event.getSource())));
    }
}
