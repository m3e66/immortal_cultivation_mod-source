package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.network.DebugTimePayloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Opens the forward-only clock used to preview accelerated celestial motion. */
public final class DebugTimeControllerItem extends Item {
    public DebugTimeControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new DebugTimePayloads.ClientboundOpenTimeScreen(level.getDayTime()));
        }
        return InteractionResultHolder.sidedSuccess(
                player.getItemInHand(hand), level.isClientSide);
    }
}
