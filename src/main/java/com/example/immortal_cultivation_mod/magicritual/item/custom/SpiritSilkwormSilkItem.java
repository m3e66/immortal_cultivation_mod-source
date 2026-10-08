package com.example.immortal_cultivation_mod.magicritual.item.custom;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public final class SpiritSilkwormSilkItem extends Item {
   private static final int REFINE_QI_COST = 20;

   public SpiritSilkwormSilkItem(Item.Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player instanceof ServerPlayer serverPlayer) {
         ModAttachments.CultivationData data = ModAttachments.getData(serverPlayer);
         if (!ServerEvents.spendQiOrBlood(serverPlayer, data, 20)) {
            serverPlayer.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.not_enough_qi"), true);
            return InteractionResultHolder.fail(stack);
         } else {
            int puritySeed = (int)(serverPlayer.getUUID().getLeastSignificantBits() ^ level.getGameTime());
            ForgingSystem.ensurePurity(stack, puritySeed);
            ItemStack refined = new ItemStack((ItemLike)ModItems.SPIRIT_GUIDING_THREAD.get());
            ForgingSystem.copyPurity(stack, refined);
            if (!serverPlayer.getAbilities().instabuild) {
               stack.shrink(1);
            }

            if (!serverPlayer.getInventory().add(refined)) {
               serverPlayer.drop(refined, false);
            }

            ServerEvents.syncPlayerData(serverPlayer);
            if (level instanceof ServerLevel) {
               ServerLevel serverLevel = (ServerLevel)level;
               serverLevel.sendParticles(ParticleTypes.ENCHANT, serverPlayer.getX(), serverPlayer.getY() + (double)1.0F, serverPlayer.getZ(), 18, 0.35, 0.45, 0.35, 0.08);
            }

            serverPlayer.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.spirit_silk.refined"), true);
            return InteractionResultHolder.success(stack);
         }
      } else {
         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
      }
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.spirit_silkworm_silk", new Object[]{20}).withStyle(ChatFormatting.AQUA));
      super.appendHoverText(stack, context, tooltip, flag);
   }
}
