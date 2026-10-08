package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Terrain replacement obeys the same preferences and protection events as terrain removal. */
public final class TerrainTransforms {
    private TerrainTransforms() { }

    public static boolean replace(ServerLevel level, BlockPos pos, BlockState replacement,
            LivingEntity actor, BlockBreakingSettings.Category category) {
        if (!BlockBreakingSettings.enabled(actor, category)) return false;
        return BlockBreakingSettings.withSource(category, actor, () -> {
            if (actor == null || !SpellDestruction.canBreakBlock(level, pos, actor)) return false;
            BlockState expected = level.getBlockState(pos);
            if (expected == replacement) return false;
            ServerPlayer player = BlockBreakingSettings.playerOwner(actor);
            if (player != null && (!level.mayInteract(player, pos)
                    || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, expected, player)).isCanceled()))
                return false;
            if (level.getBlockState(pos) != expected) return false;
            return level.setBlockAndUpdate(pos, replacement);
        });
    }
}
