package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;

public final class QiInfusedWeapon {
    private static final String TAG_QI = ImmortalCultivationMod.MODID + ".weapon_qi";

    private QiInfusedWeapon() {
    }

    public static void inject(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".qi_inject_need_weapon"), true);
            return;
        }
        if (!hasBaseAttackDamage(stack)) {
            player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".qi_inject_need_damage"), true);
            return;
        }

        var data = ModAttachments.getData(player);
        int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        int cost = Math.max(1, maxQi / 100);
        if (data.qi() < cost) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ModAttachments.setData(player, data.withQi(data.qi() - cost));
        setQi(stack, getQi(stack) + cost);
        player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".qi_injected", cost), true);
        ServerEvents.syncPlayerData(player);
    }

    public static void strike(ServerPlayer player, Entity target) {
        ItemStack stack = player.getMainHandItem();
        double qi = getQi(stack);
        if (qi <= 0.0D) {
            return;
        }

        float bonusDamage = ForgingSystem.adjustInjectedQiDamage(stack, (float) (qi / 10.0D));
        if (bonusDamage <= 0.0F) {
            return;
        }

        if (target.hurt(player.damageSources().playerAttack(player), bonusDamage)) {
            setQi(stack, Math.max(0.0D, qi - bonusDamage));
        }
    }

    public static void tickOverflow(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            tickOverflow(player, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            tickOverflow(player, stack);
        }
        ForgingSystem.tickForgedInventory(player);
    }

    private static void tickOverflow(ServerPlayer player, ItemStack stack) {
        double qi = getQi(stack);
        if (qi <= 0.0D || !stack.isDamageableItem()) {
            return;
        }

        double threshold = Math.max(1.0D, baseAttackDamage(stack)) * 100.0D;
        if (qi <= threshold) {
            return;
        }

        int durabilityLoss = Math.max(1, (int) Math.ceil((qi - threshold) / 10.0D));
        int nextDamage = stack.getDamageValue() + durabilityLoss;
        if (nextDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
            stack.setDamageValue(0);
            player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".qi_overflow_broke"), true);
        } else {
            stack.setDamageValue(nextDamage);
        }
    }

    public static boolean hasQi(ItemStack stack) {
        return getQi(stack) > 0.0D;
    }

    private static double baseAttackDamage(ItemStack stack) {
        double damage = 1.0D;
        boolean hasAttackDamage = false;
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                continue;
            }
            if (entry.slot() != EquipmentSlotGroup.ANY && entry.slot() != EquipmentSlotGroup.MAINHAND) {
                continue;
            }
            hasAttackDamage = true;
            AttributeModifier modifier = entry.modifier();
            damage = switch (modifier.operation()) {
                case ADD_VALUE -> damage + modifier.amount();
                case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> damage * (1.0D + modifier.amount());
            };
        }
        return hasAttackDamage ? Math.max(1.0D, damage) : 0.0D;
    }

    private static boolean hasBaseAttackDamage(ItemStack stack) {
        return baseAttackDamage(stack) > 0.0D;
    }

    public static double getQi(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0.0D, tag.getDouble(TAG_QI));
    }

    public static Component tooltip(ItemStack stack) {
        double qi = getQi(stack);
        if (qi <= 0.0D) {
            return Component.empty();
        }
        double damage = qi / 10.0D;
        return Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".weapon_qi",
                format(qi),
                format(damage)
        ).withStyle(ChatFormatting.AQUA);
    }

    private static void setQi(ItemStack stack, double qi) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (qi <= 0.001D) {
            tag.remove(TAG_QI);
        } else {
            tag.putDouble(TAG_QI, qi);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
