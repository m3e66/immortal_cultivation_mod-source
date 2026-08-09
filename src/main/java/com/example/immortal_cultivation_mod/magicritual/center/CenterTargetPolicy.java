package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class CenterTargetPolicy {
    private CenterTargetPolicy() {
    }

    public static boolean canEdit(MagicCenterBlockEntity center, Player player) {
        return center.getOwnerUuid() == null
                || center.isOwner(player)
                || center.isController(player)
                || player.hasPermissions(2);
    }

    public static boolean canManageControllers(MagicCenterBlockEntity center, Player player) {
        return center.isOwner(player)
                || player.hasPermissions(2);
    }

    public static boolean canWithdrawFuel(MagicCenterBlockEntity center, Player player) {
        return canEdit(center, player);
    }

    public static boolean receivesBlankBenefit(MagicCenterBlockEntity center, LivingEntity entity) {
        if (entity instanceof Player player) {
            return center.isOwner(player) || center.isPlayerExcluded(player);
        }
        return center.isAffectedByCircle(entity);
    }

    public static boolean receivesHarmfulEffect(MagicCenterBlockEntity center, LivingEntity entity) {
        if (entity instanceof Player player
                && (center.isOwner(player) || center.isPlayerExcluded(player))) {
            return false;
        }
        return center.isAffectedByCircle(entity);
    }

    public static boolean receivesBeneficialEffect(MagicCenterBlockEntity center, LivingEntity entity) {
        return receivesBlankBenefit(center, entity);
    }

    public static boolean isAuthorizedPlayer(MagicCenterBlockEntity center, Player player) {
        return !center.isPlayerExcluded(player);
    }

    public static boolean mayCrossForcefield(MagicCenterBlockEntity center, Entity entity) {
        if (entity instanceof Player player) {
            return !center.isPlayerExcluded(player);
        }
        if (entity instanceof LivingEntity living) {
            return living instanceof Enemy
                    ? !center.isAllowHostileMobs()
                    : !center.isAllowPassiveMobs();
        }
        return false;
    }
}