package com.example.immortal_cultivation_mod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** One permanent fan form; old folded stacks retain all their injected Qi. */
public final class WindFanQi {
    private static final String RESERVED_QI = "WindFanReservedQi";
    private WindFanQi() {}

    public static boolean isFan(ItemStack stack) {
        return stack.getItem() instanceof HigherWeaponItem item
                && item.spec == HigherWeaponSpec.WIND_FEATHER_FAN;
    }

    public static void migrateLegacyStack(ItemStack stack) {
        if (!isFan(stack)) return;
        var tag = HigherWeaponItem.data(stack);
        var attributes = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        boolean closedStats = attributes.modifiers().stream().anyMatch(entry ->
                entry.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID) && entry.modifier().amount() == 7);
        if (!tag.contains("WindFanOpen") && !tag.contains(RESERVED_QI) && !closedStats) return;
        if (closedStats) {
            var builder = ItemAttributeModifiers.builder();
            for (var entry : attributes.modifiers()) {
                if (!entry.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)
                        && !entry.modifier().id().equals(Item.BASE_ATTACK_SPEED_ID)
                        && !entry.modifier().id().equals(HigherWeaponItem.id("higher_weapon_reach"))) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }
            for (var entry : HigherWeaponItem.attributes(HigherWeaponSpec.WIND_FEATHER_FAN).modifiers()) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        }
        double reserve = Math.max(0, tag.getDouble(RESERVED_QI));
        tag.remove("WindFanOpen");
        tag.remove(RESERVED_QI);
        HigherWeaponItem.save(stack, tag);
        if (reserve > 0) QiInfusedWeapon.setQi(stack, QiInfusedWeapon.getQi(stack) + reserve);
    }

}
