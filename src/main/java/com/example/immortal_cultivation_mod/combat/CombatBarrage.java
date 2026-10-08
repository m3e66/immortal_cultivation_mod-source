package com.example.immortal_cultivation_mod.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-timed normal attacks; clients send clicks, never the ten individual strikes. */
public final class CombatBarrage {
    public static final int ATTACK_COUNT = 10;
    public static final int INTERVAL_TICKS = 6;
    public static final int DOUBLE_CLICK_TICKS = 6;
    private static final Map<UUID, Click> CLICKS = new HashMap<>();
    private static final Map<UUID, Burst> BURSTS = new HashMap<>();

    private record Click(long at, ResourceKey<Level> dimension, int slot, ItemStack weapon) { }
    private static final class Burst {
        final Click origin;
        long next;
        int remaining = ATTACK_COUNT;

        Burst(Click origin) {
            this.origin = origin;
            next = origin.at + INTERVAL_TICKS;
        }
    }

    private CombatBarrage() { }

    /** @return true if the click starts or is consumed by an already active barrage. */
    static boolean onNormalClick(ServerPlayer player) {
        if (!eligible(player)) {
            clear(player);
            return false;
        }
        if (BURSTS.containsKey(player.getUUID())) return true;
        long now = player.serverLevel().getGameTime();
        Click click = new Click(now, player.level().dimension(), player.getInventory().selected,
                player.getMainHandItem());
        Click previous = CLICKS.put(player.getUUID(), click);
        if (previous == null || now < previous.at || now - previous.at > DOUBLE_CLICK_TICKS
                || !sameWeapon(player, previous)) return false;
        CLICKS.remove(player.getUUID());
        BURSTS.put(player.getUUID(), new Burst(click));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return BURSTS.containsKey(player.getUUID());
    }

    static void tick(ServerPlayer player) {
        Burst burst = BURSTS.get(player.getUUID());
        if (burst == null) {
            Click click = CLICKS.get(player.getUUID());
            if (click != null && (player.serverLevel().getGameTime() - click.at > DOUBLE_CLICK_TICKS
                    || !eligible(player) || !sameWeapon(player, click))) CLICKS.remove(player.getUUID());
            return;
        }
        if (!eligible(player) || !sameWeapon(player, burst.origin)) {
            clear(player);
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now < burst.next) return;
        // Never catch up several hits in one tick after a delayed tick or dimension transition.
        burst.next = now + INTERVAL_TICKS;
        CombatStateManager.performBarrageAttack(player, aimedEntity(player));
        if (--burst.remaining == 0) clear(player);
    }

    private static int aimedEntity(ServerPlayer player) {
        double range = player.entityInteractionRange() + 1.0D;
        var start = player.getEyePosition();
        var direction = player.getLookAngle().scale(range);
        var end = player.pick(range, 1, false).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(direction).inflate(1),
                entity -> entity != player && entity.isAlive() && entity.isPickable() && !entity.isSpectator(),
                start.distanceToSqr(end));
        return hit == null ? -1 : hit.getEntity().getId();
    }

    private static boolean sameWeapon(ServerPlayer player, Click click) {
        return player.level().dimension().equals(click.dimension)
                && player.getInventory().selected == click.slot && player.getMainHandItem() == click.weapon;
    }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu
                && CombatStateManager.isInCombat(player) && !CombatStateManager.isPostureBroken(player)
                && !CombatStateManager.isBlocking(player)
                && !CombatStateManager.isSpellcastingBlocked(player)
                && (CombatStateManager.isCombatWeapon(player.getMainHandItem())
                    || com.example.immortal_cultivation_mod.item.XiujianItem.canFire(player));
    }

    public static void clear(ServerPlayer player) {
        CLICKS.remove(player.getUUID());
        BURSTS.remove(player.getUUID());
    }
}
