package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;

public final class ChangqingJue {
    private ChangqingJue() {
    }

    public static void tick(ServerPlayer player) {
        if (!CultivationMethods.isChangqingJue(ModAttachments.getData(player).activeCultivationMethod())
                || !(player.level() instanceof ServerLevel level)
                || player.tickCount % 80 != 0) {
            return;
        }

        BlockPos center = player.blockPosition();
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 2, 5))) {
            if (grown >= 12 || pos.distSqr(center) > 30.0D) {
                continue;
            }
            var state = level.getBlockState(pos);
            if (state.is(Blocks.GRASS_BLOCK) || !(state.getBlock() instanceof BonemealableBlock growable)) {
                continue;
            }
            if (growable.isValidBonemealTarget(level, pos, state) && growable.isBonemealSuccess(level, level.random, pos, state)) {
                growable.performBonemeal(level, level.random, pos, state);
                grown++;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                        6, 0.25D, 0.25D, 0.25D, 0.02D);
            }
        }
    }
}
