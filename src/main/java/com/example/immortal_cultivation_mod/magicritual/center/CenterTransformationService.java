package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

public final class CenterTransformationService {
   private static final DustParticleOptions ERROR_PARTICLE =
         new DustParticleOptions(new Vector3f(1.0F, 0.08F, 0.08F), 1.25F);

   private CenterTransformationService() {
   }

   public static void activate(ServerPlayer player, BlockPos centerPos, ItemStack dagger) {
      ServerLevel level = player.serverLevel();
      if (!(level.getBlockEntity(centerPos) instanceof MagicCenterBlockEntity center)) {
         return;
      }
      if (!center.isBlank()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
               "message.immortal_cultivation_mod.center.already_transformed",
               center.getDisplayName()), true);
         return;
      }
      if (!CenterTargetPolicy.canEdit(center, player)) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
               "message.immortal_cultivation_mod.center.not_owner"), true);
         return;
      }
      if (center.getOwnerUuid() == null) {
         center.setOwnerUuid(player.getUUID());
      }

      CenterRitualMatcher.Result result = CenterRitualMatcher.match(
            level, centerPos, CenterRitualManager.INSTANCE.definitions());
      if (result.status() != CenterRitualMatcher.Status.SUCCESS || result.match() == null) {
         fail(player, level, result);
         return;
      }

      CenterRitualMatcher.Match match = result.match();
      CenterRitualMatcher.Result recheck = CenterRitualMatcher.match(
            level, centerPos, List.of(match.definition()));
      if (recheck.status() != CenterRitualMatcher.Status.SUCCESS
            || recheck.match() == null
            || !recheck.match().definition().output().equals(match.definition().output())) {
         fail(player, level, recheck);
         return;
      }

      ResourceLocation output = match.definition().output();
      List<BlockPos> consumed = match.consumedPositions();
      for (BlockPos patternPos : consumed) {
         if (!level.getBlockState(patternPos).is(ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                  "message.immortal_cultivation_mod.center.pattern_changed"), true);
            return;
         }
      }
      center.beginPendingTransformation(output, consumed);
      for (BlockPos patternPos : consumed) {
         level.sendParticles(ParticleTypes.ENCHANT,
               patternPos.getX() + 0.5D, patternPos.getY() + 0.2D,
               patternPos.getZ() + 0.5D, 18,
               (centerPos.getX() - patternPos.getX()) * 0.035D,
               0.08D,
               (centerPos.getZ() - patternPos.getZ()) * 0.035D,
               0.03D);
         level.setBlock(patternPos, Blocks.AIR.defaultBlockState(), 3);
      }
      center.completeTransformation(output);
      dagger.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
      level.playSound(null, centerPos, SoundEvents.ENCHANTMENT_TABLE_USE,
            SoundSource.BLOCKS, 1.3F, 0.7F);
      level.sendParticles(ParticleTypes.END_ROD,
            centerPos.getX() + 0.5D, centerPos.getY() + 0.75D,
            centerPos.getZ() + 0.5D, 70, 0.8D, 0.5D, 0.8D, 0.08D);
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
            "message.immortal_cultivation_mod.center.transformed",
            center.getDisplayName()), true);
   }

   private static void fail(ServerPlayer player, ServerLevel level,
                            CenterRitualMatcher.Result result) {
      String key = switch (result.status()) {
         case AMBIGUOUS -> "message.immortal_cultivation_mod.center.ambiguous";
         case UNLOADED -> "message.immortal_cultivation_mod.center.unloaded";
         default -> "message.immortal_cultivation_mod.center.no_pattern";
      };
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
      for (BlockPos error : result.errorPositions()) {
         level.sendParticles(ERROR_PARTICLE,
               error.getX() + 0.5D, error.getY() + 0.35D, error.getZ() + 0.5D,
               10, 0.18D, 0.18D, 0.18D, 0.01D);
      }
   }
}
