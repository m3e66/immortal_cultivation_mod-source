package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Shared world-space constellation and impact clock for server damage and client art. */
public final class SevenStarPattern {
    public static final int COUNT = 7;
    public static final int DESCENT = 10;
    public static final int INTERVAL = 6;
    public static final int FINISH = DESCENT + COUNT * INTERVAL;
    public static final int LIFETIME = FINISH + 10;
    private static final double[][] POINTS = {
        {-1.0, .4}, {-.65, .05}, {-.25, -.2}, {.1, -.75},
        {.85, -.85}, {1.05, -.1}, {.35, .15}
    };
    private SevenStarPattern() {}
    public static int charges(ItemStack stack) {
        return Math.clamp(HigherWeaponItem.data(stack).getInt("Stars"), 0, COUNT);
    }
    public static net.minecraft.network.chat.Component tooltip(ItemStack stack) {
        return net.minecraft.network.chat.Component.translatable(
                "tooltip.immortal_cultivation_mod.seven_star_sword.stars", charges(stack))
                .withStyle(net.minecraft.ChatFormatting.GOLD);
    }
    public static void showCharges(net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        player.displayClientMessage(tooltip(stack), true);
    }
    public static boolean requireCharges(net.minecraft.server.level.ServerPlayer player, ItemStack stack, int required) {
        if (charges(stack) >= required) return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.immortal_cultivation_mod.seven_star.need_charges", charges(stack), required)
                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
        return false;
    }
    public static Vec3 point(Vec3 center, int index) {
        return center.add(POINTS[index][0], 0, POINTS[index][1]);
    }
    public static int impactTick(int index) { return DESCENT + index * INTERVAL; }
    public static double progress(double age, int index) {
        return Math.clamp((age - index * INTERVAL) / DESCENT, 0, 1);
    }
}
