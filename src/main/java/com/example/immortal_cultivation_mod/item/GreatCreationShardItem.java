package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GreatCreationShardItem extends Item {
    public GreatCreationShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        var data = ModAttachments.getData(serverPlayer);
        String next = CultivationLevels.getNextStage(data.cultivationLevel());
        if (next == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.immortal_cultivation_mod.great_creation_shard.max"), true);
            return InteractionResultHolder.fail(stack);
        }

        ModAttachments.setData(serverPlayer,
                data.withCultivationLevel(next)
                        .withQi(0)
                        .withCultivationProgress(0)
                        .withMeditating(false)
                        .withAddedSkillPoints(10));
        ServerEvents.syncPlayerData(serverPlayer);
        serverPlayer.setHealth(serverPlayer.getMaxHealth());
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverPlayer.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.great_creation_shard.used", next), false);
        return InteractionResultHolder.success(stack);
    }
}
