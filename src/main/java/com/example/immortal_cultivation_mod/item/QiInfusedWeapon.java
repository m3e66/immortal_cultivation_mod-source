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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;

public final class QiInfusedWeapon {
    public static final double QI_PER_BONUS_DAMAGE = 20.0D;
    private static final int AUTO_CHANNEL_INTERVAL_TICKS = 5;
    private static final String TAG_QI = ImmortalCultivationMod.MODID + ".weapon_qi";
    private static final String AUTO_CHANNEL_TAG = "ImmortalCultivationAutoChannelWeaponQi";
    private static final String OUTWARD_QI_AUTO_FIT_TAG = "ImmortalCultivationOutwardQiAutoFit";
    private static final String OUTWARD_QI_TAG = "ImmortalCultivationOutwardQiRelease";
    private static final String OUTWARD_QI_BLOCK_BREAKING_TAG =
            "ImmortalCultivationOutwardQiBlockBreaking";

    private QiInfusedWeapon() {
    }

    public static void inject(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".qi_inject_need_weapon"), true);
            return;
        }
        if (!isWeapon(stack)) {
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

        double added = cost;
        if (stack.getItem() instanceof TieguShanItem) {
            added = TieguShanItem.storeInjectedQi(stack, cost);
            if (added <= 0.001D) {
                player.displayClientMessage(Component.translatable(
                        "message." + ImmortalCultivationMod.MODID + ".qi_storage_full"), true);
                return;
            }
        } else {
            setQi(stack, getQi(stack) + cost);
        }

        int spent = Math.min(data.qi(), Math.max(1, (int) Math.ceil(added)));
        ModAttachments.setData(player, data.withQi(data.qi() - spent));
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".qi_injected", format(added)), true);
        ServerEvents.syncPlayerData(player);
    }

    public static void setAutoChannel(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(AUTO_CHANNEL_TAG, enabled);
    }

    public static void setOutwardQiRelease(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(OUTWARD_QI_TAG, enabled);
    }

    public static boolean outwardQiReleaseEnabled(ServerPlayer player) {
        return player.getPersistentData().getBoolean(OUTWARD_QI_TAG);
    }

    public static void setOutwardQiBlockBreaking(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(OUTWARD_QI_BLOCK_BREAKING_TAG, enabled);
    }

    public static boolean outwardQiBlockBreakingEnabled(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return !data.contains(OUTWARD_QI_BLOCK_BREAKING_TAG)
                || data.getBoolean(OUTWARD_QI_BLOCK_BREAKING_TAG);
    }

    public static void setOutwardQiAutoFit(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(OUTWARD_QI_AUTO_FIT_TAG, enabled);
    }

    public static boolean outwardQiAutoFitEnabled(ServerPlayer player) {
        var data = player.getPersistentData();
        return !data.contains(OUTWARD_QI_AUTO_FIT_TAG) || data.getBoolean(OUTWARD_QI_AUTO_FIT_TAG);
    }

    public static void copyPersistentData(Player original, Player copy) {
        if (original.getPersistentData().contains(OUTWARD_QI_AUTO_FIT_TAG)) {
            copy.getPersistentData().putBoolean(OUTWARD_QI_AUTO_FIT_TAG,
                    original.getPersistentData().getBoolean(OUTWARD_QI_AUTO_FIT_TAG));
        }
        if (original.getPersistentData().getBoolean(AUTO_CHANNEL_TAG)) {
            copy.getPersistentData().putBoolean(AUTO_CHANNEL_TAG, true);
        }
        if (original.getPersistentData().getBoolean(OUTWARD_QI_TAG)) {
            copy.getPersistentData().putBoolean(OUTWARD_QI_TAG, true);
        }
        if (original.getPersistentData().contains(OUTWARD_QI_BLOCK_BREAKING_TAG)) {
            copy.getPersistentData().putBoolean(
                    OUTWARD_QI_BLOCK_BREAKING_TAG,
                    original.getPersistentData().getBoolean(OUTWARD_QI_BLOCK_BREAKING_TAG));
        }
    }

    public static void tickAutoChannel(ServerPlayer player) {
        if (player.tickCount % AUTO_CHANNEL_INTERVAL_TICKS != 0
                || !player.getPersistentData().getBoolean(AUTO_CHANNEL_TAG)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!canAutoChannel(stack)) {
            return;
        }

        boolean isFan = stack.getItem() instanceof TieguShanItem;
        double currentQi = getQi(stack);
        double limit = storageLimit(stack);
        double room = isFan ? TieguShanItem.remainingQiStorage(stack)
                : limit - currentQi;
        if (room <= 0.001D) {
            return;
        }

        var data = ModAttachments.getData(player);
        int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        int transfer = Math.min(data.qi(), Math.max(1, maxQi / 100));
        if (transfer <= 0) {
            return;
        }

        double added = Math.min(room, transfer);
        int spent = Math.min(data.qi(), Math.max(1, (int) Math.ceil(added)));
        if (isFan) {
            added = TieguShanItem.storeInjectedQi(stack, added);
        } else {
            setQi(stack, Math.min(limit, currentQi + added));
        }
        if (added <= 0.001D) {
            return;
        }
        ModAttachments.setData(player, data.withQi(data.qi() - spent));
        ServerEvents.syncPlayerData(player);
    }

    public static void strike(ServerPlayer player, Entity target) {
        ItemStack stack = player.getMainHandItem();
        if (outwardQiReleaseEnabled(player) && supportsOutwardQi(stack)) {
            return;
        }
        float bonusDamage = injectedQiDamage(stack);
        if (bonusDamage <= 0.0F) {
            return;
        }
        if (target.hurt(player.damageSources().playerAttack(player), bonusDamage)) {
            consumeInjectedQiDamage(stack, bonusDamage);
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

        double threshold = storageLimit(stack);
        if (qi <= threshold) {
            return;
        }

        int durabilityLoss = Math.max(1, (int) Math.ceil((qi - threshold) / 10.0D));
        int nextDamage = stack.getDamageValue() + durabilityLoss;
        if (nextDamage >= stack.getMaxDamage()) {
            WeaponShardSystem.dropBroken(stack.copyWithCount(1),player.serverLevel(),player);
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

    public static double baseAttackDamage(ItemStack stack) {
        double addValue = 0.0D;
        double addMultipliedBase = 0.0D;
        double multipliedTotal = 1.0D;
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
            switch (modifier.operation()) {
                case ADD_VALUE -> addValue += modifier.amount();
                case ADD_MULTIPLIED_BASE -> addMultipliedBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> multipliedTotal *= 1.0D + modifier.amount();
            }
        }
        double damage = (1.0D + addValue) * (1.0D + addMultipliedBase) * multipliedTotal;
        return hasAttackDamage ? Math.max(1.0D, damage) : 0.0D;
    }

    public static double attackDamageAddCoefficient(ItemStack stack) {
        double addMultipliedBase = 0.0D;
        double multipliedTotal = 1.0D;
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.attribute().is(Attributes.ATTACK_DAMAGE)
                    || (entry.slot() != EquipmentSlotGroup.ANY
                    && entry.slot() != EquipmentSlotGroup.MAINHAND)) {
                continue;
            }
            switch (entry.modifier().operation()) {
                case ADD_MULTIPLIED_BASE -> addMultipliedBase += entry.modifier().amount();
                case ADD_MULTIPLIED_TOTAL -> multipliedTotal *= 1.0D + entry.modifier().amount();
                default -> {
                }
            }
        }
        return Math.max(0.0001D, (1.0D + addMultipliedBase) * multipliedTotal);
    }

    public static boolean isWeapon(ItemStack stack) {
        return baseAttackDamage(stack) > 0.0D;
    }

    private static boolean canAutoChannel(ItemStack stack) {
        return isWeapon(stack) && !(stack.getItem() instanceof DiggerItem);
    }

    public static boolean isSwordOrDao(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof ThrowingKnifeItem)
                && !HeavyIronBracerItem.isRealStack(stack)
                && !HeavyIronBracerItem.isDebugStack(stack);
    }

    public static boolean isFistWeapon(ItemStack stack) {
        return HeavyIronBracerItem.isRealStack(stack) || stack.getItem() instanceof com.example.immortal_cultivation_mod.pagoda.GoldenGourdItem;
    }

    public static boolean supportsOutwardQi(ItemStack stack) {
        return isSwordOrDao(stack) || isFistWeapon(stack);
    }

    public static boolean isSpear(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof JingtieSpearItem;
    }

    public static double storageLimit(ItemStack stack) {
        return Math.max(1.0D, baseAttackDamage(stack)) * 100.0D
                * WeaponRestrictionSystem.qiStorageMultiplier(stack);
    }

    public static double getQi(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0.0D, tag.getDouble(TAG_QI));
    }

    public static float injectedQiDamage(ItemStack stack) {
        double qi = getQi(stack);
        if (qi <= 0.0D) {
            return 0.0F;
        }
        return ForgingSystem.adjustInjectedQiDamage(
                stack,
                (float) (qi / QI_PER_BONUS_DAMAGE));
    }

    public static void consumeInjectedQiDamage(
            ItemStack stack,
            float injectedQiDamage) {
        // Preserve the existing 10% charge drain independently of the damage conversion.
        // Forging's Qi-efficiency modifiers continue to affect both bonus and payment.
        consumeQi(stack, Math.max(0.0F, injectedQiDamage) * QI_PER_BONUS_DAMAGE * 0.1D);
    }

    public static void consumeQi(ItemStack stack, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        setQi(stack, Math.max(0.0D, getQi(stack) - amount));
    }

    /** Appended by ItemTooltipEvent, after vanilla attributes and advanced item information. */
    public static void appendStoredQiTooltips(ItemStack stack, java.util.List<Component> lines) {
        if (getQi(stack) > 0.0D) {
            lines.add(tooltip(stack));
        }
    }

    public static Component tooltip(ItemStack stack) {
        double qi = getQi(stack);
        double damage = injectedQiDamage(stack);
        return Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".weapon_qi",
                format(qi),
                format(damage)
        ).withStyle(ChatFormatting.AQUA);
    }

    static void setQi(ItemStack stack, double qi) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (qi <= 0.001D) {
            tag.remove(TAG_QI);
        } else {
            tag.putDouble(TAG_QI, qi);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
