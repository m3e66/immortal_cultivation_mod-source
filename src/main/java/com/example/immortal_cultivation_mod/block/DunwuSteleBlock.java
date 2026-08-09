package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DunwuSteleBlock extends GeoRockBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DunwuSteleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (!(level.getBlockEntity(pos) instanceof GeoRockBlockEntity stele)) {
            return InteractionResult.CONSUME;
        }
        String spellId = ModSpells.normalizeId(stele.steleSpellId());
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        if (spell == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dunwu_stele.empty"), true);
            return InteractionResult.CONSUME;
        }
        var data = ModAttachments.getData(serverPlayer);
        if (data.knownSpells().contains(spellId) || ModSpells.isInnateKnown(spellId, data)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dunwu_stele.already_learned",
                    spell.name()), true);
            return InteractionResult.CONSUME;
        }
        if (!ModSpells.meetsRequirement(data.cultivationLevel(), spell)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".spell_requirement",
                    spell.requiredLevel()), true);
            return InteractionResult.CONSUME;
        }
        if (!ModPayloads.beginDunwuSteleLearning(serverPlayer, spellId)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dunwu_stele.busy"), true);
        }
        return InteractionResult.CONSUME;
    }
}
