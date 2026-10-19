package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.HigherWeaponItem;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Weapon-level spread, with a server-owned optional size preference. */
public final class ExternalQiSize {
    private static final String KEY = "ExternalQiSizePercent";
    private static final java.util.Map<ServerPlayer, Long> LAST_SCROLL = new java.util.WeakHashMap<>();
    private static final double[] LEVEL_RADII = {2, 3, 4.5, 6.5, 9, 13, 19, 26};
    private ExternalQiSize() {}
    public static int clampPercent(int value) { return Math.clamp(value, 10, 200); }
    public static double scale(Player player) {
        return (player.getPersistentData().contains(KEY)
                ? clampPercent(player.getPersistentData().getInt(KEY)) : 100) / 100.0;
    }

    /** Intrinsic Foundation/Golden Core gear keeps its starting level even without restrictions. */
    public static int weaponLevel(ItemStack weapon) {
        int intrinsic = weapon.getItem() instanceof HigherWeaponItem higher ? higher.spec.realm : 0;
        return Math.clamp(Math.max(intrinsic, WeaponRestrictionSystem.tier(weapon)), 0, LEVEL_RADII.length - 1);
    }

    public static double radius(ItemStack weapon, double preference, boolean cone, boolean heavy, boolean whip) {
        double factor = Double.isFinite(preference) ? Math.clamp(preference, .1, 2) : 1;
        boolean fist = QiInfusedWeapon.isFistWeapon(weapon);
        double shape = fist ? (heavy ? 1.5 : .75) : whip ? .45 : cone ? .6 : 1;
        double maximum = fist ? (heavy ? 12 : 7) : ExternalQiVfxShape.MAX_RADIUS;
        return Math.clamp(LEVEL_RADII[weaponLevel(weapon)] * shape * factor, .2, maximum);
    }
    public static void adjust(ServerPlayer player, int steps) {
        if (steps == 0 || !player.isAlive() || player.isSpectator() || !player.isShiftKeyDown()
                || !QiInfusedWeapon.isWeapon(player.getMainHandItem())) return;
        long now = player.serverLevel().getGameTime();
        if (LAST_SCROLL.getOrDefault(player, Long.MIN_VALUE) == now) return;
        LAST_SCROLL.put(player, now);
        int percent = clampPercent((int) Math.round(scale(player) * 100) + Math.clamp(steps, -4, 4) * 10);
        player.getPersistentData().putInt(KEY, percent);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.immortal_cultivation_mod.external_qi_size", percent), true);
    }
}
