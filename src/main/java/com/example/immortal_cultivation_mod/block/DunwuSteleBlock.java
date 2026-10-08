package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DunwuSteleBlock extends GeoRockBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SHAPE = createNorthShape();
    private static final VoxelShape EAST_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.EAST);
    private static final VoxelShape SOUTH_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.SOUTH);
    private static final VoxelShape WEST_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.WEST);
    private static final VoxelShape[][] SCALED_SHAPES = {
            createScaledVariants(NORTH_SHAPE),
            createScaledVariants(EAST_SHAPE),
            createScaledVariants(SOUTH_SHAPE),
            createScaledVariants(WEST_SHAPE)
    };

    public DunwuSteleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SCALE, MIN_SCALE));
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
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return shapeFor(state.getValue(FACING), state.getValue(SCALE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return shapeFor(state.getValue(FACING), state.getValue(SCALE));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (tryCycleScale(state, level, pos, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
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
                    CultivationLevels.displayName(spell.requiredLevel())), true);
            return InteractionResult.CONSUME;
        }
        if (!ModPayloads.beginDunwuSteleLearning(serverPlayer, spellId)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".dunwu_stele.busy"), true);
        }
        return InteractionResult.CONSUME;
    }

    private static VoxelShape shapeFor(Direction facing, int scale) {
        int direction = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SCALED_SHAPES[direction][scale - MIN_SCALE];
    }

    private static VoxelShape createNorthShape() {
        return SteleModelShape.north();
    }

    /** Rotates around the block center using the same positive-Y angles as the renderer. */
    private static VoxelShape rotateHorizontal(VoxelShape source, Direction facing) {
        if (facing == Direction.NORTH) {
            return source;
        }

        VoxelShape[] rotated = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                case EAST -> Shapes.box(
                        minZ, minY, 1.0D - maxX,
                        maxZ, maxY, 1.0D - minX);
                case SOUTH -> Shapes.box(
                        1.0D - maxX, minY, 1.0D - maxZ,
                        1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> Shapes.box(
                        1.0D - maxZ, minY, minX,
                        1.0D - minZ, maxY, maxX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0].optimize();
    }
}
