package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class SpiritSilkwormSilkItem extends Item {
    private static final int REFINE_QI_COST = 20;

    public SpiritSilkwormSilkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        var data = ModAttachments.getData(serverPlayer);
        if (!ServerEvents.spendQiOrBlood(serverPlayer, data, REFINE_QI_COST)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"), true);
            return InteractionResultHolder.fail(stack);
        }
        int puritySeed = (int) (serverPlayer.getUUID().getLeastSignificantBits()
                ^ level.getGameTime());
        ForgingSystem.ensurePurity(stack, puritySeed);
        ItemStack refined = new ItemStack(ModItems.SPIRIT_GUIDING_THREAD.get());
        ForgingSystem.copyPurity(stack, refined);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!serverPlayer.getInventory().add(refined)) {
            serverPlayer.drop(refined, false);
        }
        ServerEvents.syncPlayerData(serverPlayer);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                    18, 0.35D, 0.45D, 0.35D, 0.08D);
        }
        serverPlayer.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".spirit_silk.refined"), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".spirit_silkworm_silk",
                REFINE_QI_COST).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
