package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem;
import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3f;

public final class CenterTransformationService {
   private static final DustParticleOptions ERROR_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.08F, 0.08F), 1.25F);

   private CenterTransformationService() {
   }

   public static void activate(ServerPlayer player, BlockPos centerPos, ItemStack dagger) {
      ServerLevel level = player.serverLevel();
      BlockEntity var5 = level.getBlockEntity(centerPos);
      if (var5 instanceof MagicCenterBlockEntity center) {
         if (!center.isBlank()) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.already_transformed", new Object[]{center.getDisplayName()}), true);
         } else if (!CenterTargetPolicy.canEdit(center, player)) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.not_owner"), true);
         } else {
            if (center.getOwnerUuid() == null) {
               center.setOwnerUuid(player.getUUID());
            }

            CenterRitualMatcher.Result result = CenterRitualMatcher.match(level, centerPos, CenterRitualManager.INSTANCE.definitions());
            if (result.status() == CenterRitualMatcher.Status.SUCCESS && result.match() != null) {
               CenterRitualMatcher.Match match = result.match();
               CenterRitualMatcher.Result recheck = CenterRitualMatcher.match(level, centerPos, List.of(match.definition()));
               if (recheck.status() == CenterRitualMatcher.Status.SUCCESS && recheck.match() != null && recheck.match().definition().output().equals(match.definition().output())) {
                  ResourceLocation output = match.definition().output();
                  List<BlockPos> consumed = match.consumedPositions();

                  for(BlockPos patternPos : consumed) {
                     if (!level.getBlockState(patternPos).is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                        player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.pattern_changed"), true);
                        return;
                     }
                  }

                  center.beginPendingTransformation(output, consumed);

                  for(BlockPos patternPos : consumed) {
                     level.sendParticles(ParticleTypes.ENCHANT, (double)patternPos.getX() + (double)0.5F, (double)patternPos.getY() + 0.2, (double)patternPos.getZ() + (double)0.5F, 18, (double)(centerPos.getX() - patternPos.getX()) * 0.035, 0.08, (double)(centerPos.getZ() - patternPos.getZ()) * 0.035, 0.03);
                     level.setBlock(patternPos, Blocks.AIR.defaultBlockState(), 3);
                  }

                  center.completeTransformation(output);
                  com.example.immortal_cultivation_mod.event.CraftProficiency.award(player, com.example.immortal_cultivation_mod.event.CraftProficiency.Skill.FORMATION, 1);
                  dagger.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                  level.playSound((Player)null, centerPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.3F, 0.7F);
                  level.sendParticles(ParticleTypes.END_ROD, (double)centerPos.getX() + (double)0.5F, (double)centerPos.getY() + (double)0.75F, (double)centerPos.getZ() + (double)0.5F, 70, 0.8, (double)0.5F, 0.8, 0.08);
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.center.transformed", new Object[]{center.getDisplayName()}), true);
                  DaoGuidanceSystem.record(player, "build_pattern");
                  DaoGuidanceSystem.record(player, "activate_formation");
               } else {
                  fail(player, level, recheck);
               }
            } else {
               fail(player, level, result);
            }
         }
      }
   }

   private static void fail(ServerPlayer player, ServerLevel level, CenterRitualMatcher.Result result) {
      DaoGuidanceSystem.hint(player, "formation_mismatch");
      String var10000;
      switch (result.status()) {
         case AMBIGUOUS -> var10000 = "message.immortal_cultivation_mod.center.ambiguous";
         case UNLOADED -> var10000 = "message.immortal_cultivation_mod.center.unloaded";
         default -> var10000 = "message.immortal_cultivation_mod.center.no_pattern";
      }

      String key = var10000;
      player.displayClientMessage(Component.translatable(key), true);

      for(BlockPos error : result.errorPositions()) {
         level.sendParticles(ERROR_PARTICLE, (double)error.getX() + (double)0.5F, (double)error.getY() + 0.35, (double)error.getZ() + (double)0.5F, 10, 0.18, 0.18, 0.18, 0.01);
      }

   }
}
