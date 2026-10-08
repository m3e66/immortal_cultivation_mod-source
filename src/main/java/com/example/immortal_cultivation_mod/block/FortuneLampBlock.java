package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.fate.LuckSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** One spirit stone powers one day of local fortune gathering, capped at 65. */
public final class FortuneLampBlock extends Block {
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(3.5, 0, 3.5, 12.5, 3.7, 12.5), Block.box(4.3, 3.7, 4.3, 11.7, 11.3, 11.7),
            Block.box(2.8, 11.3, 2.8, 13.2, 13.8, 13.2), Block.box(6.6, 13.8, 7.6, 9.4, 15.6, 8.4));
    public static final MapCodec<FortuneLampBlock> CODEC = simpleCodec(FortuneLampBlock::new);
    public FortuneLampBlock(Properties properties) {
        super(properties); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false).setValue(BlockStateProperties.AGE_15, 0));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return SHAPE; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.LIT, BlockStateProperties.AGE_15); }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get()) || state.getValue(BlockStateProperties.LIT))
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (!player.isCreative()) stack.shrink(1);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true).setValue(BlockStateProperties.AGE_15, 15), 3);
            level.scheduleTick(pos, this, 1500);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BlockStateProperties.LIT)) return;
        for (var player : level.players()) {
            var data = LuckSystem.data(player);
            if (player.distanceToSqr(pos.getCenter()) <= 16 * 16) {
                data.putLong("LampAnchorUntil", level.getGameTime() + 1600);
                if (LuckSystem.luck(player) < 65 && level.getGameTime() >= data.getLong("NextLampGain")) {
                    LuckSystem.change(player, 1);
                    data.putLong("NextLampGain", level.getGameTime() + LuckSystem.DAY);
                }
            }
        }
        int fuel = state.getValue(BlockStateProperties.AGE_15);
        level.setBlock(pos, fuel == 0 ? state.setValue(BlockStateProperties.LIT, false)
                : state.setValue(BlockStateProperties.AGE_15, fuel - 1), 3);
        if (fuel > 0) level.scheduleTick(pos, this, 1500);
    }
}
