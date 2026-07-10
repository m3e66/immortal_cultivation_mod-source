package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.DuanliuKongdunDomeEntity;
import com.example.immortal_cultivation_mod.entity.DuanliuKongdunProjectileEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class DuanliuKongdun {
    private static final int COST = 1200;

    private DuanliuKongdun() {
    }

    public static boolean cast(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, COST)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        DuanliuKongdunProjectileEntity projectile = new DuanliuKongdunProjectileEntity(player.level(), player);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.25F, 0.6F);
        player.level().addFreshEntity(projectile);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean isInsideAny(Player player) {
        return !player.level().getEntitiesOfClass(DuanliuKongdunDomeEntity.class,
                new AABB(player.blockPosition()).inflate(DuanliuKongdunDomeEntity.RADIUS + 2.0D),
                dome -> dome.position().distanceTo(player.position()) < DuanliuKongdunDomeEntity.RADIUS).isEmpty();
    }
}
