package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public record CenterContext(ServerLevel level, BlockPos pos, BlockState state, MagicCenterBlockEntity center) {
}
