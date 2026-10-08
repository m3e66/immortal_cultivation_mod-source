package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DingBlock extends GeoRockBlock {
    public static final int MIN_SCALE = GeoRockBlock.MIN_SCALE;
    public static final int MAX_SCALE = GeoRockBlock.MAX_SCALE;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty SCALE = GeoRockBlock.SCALE;
    private static final VoxelShape CU_TIE_NORTH_SHAPE = DingCubeShapes.iron();
    private static final VoxelShape CU_TIE_EAST_SHAPE = rotateHorizontal(CU_TIE_NORTH_SHAPE, Direction.EAST);
    private static final VoxelShape CU_TIE_SOUTH_SHAPE = rotateHorizontal(CU_TIE_NORTH_SHAPE, Direction.SOUTH);
    private static final VoxelShape CU_TIE_WEST_SHAPE = rotateHorizontal(CU_TIE_NORTH_SHAPE, Direction.WEST);
    // Small dings use one hull; enlarged dings retain the native model details.
    private static final VoxelShape QINGTONG_YINHUO_DING_SHAPE =
            DingCubeShapes.bronze();
    private static final VoxelShape QINGTONG_EAST_SHAPE =
            rotateHorizontal(QINGTONG_YINHUO_DING_SHAPE, Direction.EAST);
    private static final VoxelShape QINGTONG_SOUTH_SHAPE =
            rotateHorizontal(QINGTONG_YINHUO_DING_SHAPE, Direction.SOUTH);
    private static final VoxelShape QINGTONG_WEST_SHAPE =
            rotateHorizontal(QINGTONG_YINHUO_DING_SHAPE, Direction.WEST);
    private static final VoxelShape[][] CU_TIE_SHAPES = createScaledVariants(
            CU_TIE_NORTH_SHAPE, CU_TIE_EAST_SHAPE, CU_TIE_SOUTH_SHAPE, CU_TIE_WEST_SHAPE);
    private static final VoxelShape[][] QINGTONG_SHAPES = createDingDetailVariants(
            QINGTONG_YINHUO_DING_SHAPE, QINGTONG_EAST_SHAPE,
            QINGTONG_SOUTH_SHAPE, QINGTONG_WEST_SHAPE);
    private static final VoxelShape SANYANG_NORTH_SHAPE =
            SanyangDingShape.create();
    private static final VoxelShape[][] SANYANG_SHAPES = createDingDetailVariants(
            SANYANG_NORTH_SHAPE, rotateHorizontal(SANYANG_NORTH_SHAPE, Direction.EAST),
            rotateHorizontal(SANYANG_NORTH_SHAPE, Direction.SOUTH),
            rotateHorizontal(SANYANG_NORTH_SHAPE, Direction.WEST));

    public DingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SCALE, MIN_SCALE));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.is(ModBlocks.CHI_JIN_SANYANG_DING.get())
                || !(level.getBlockEntity(pos) instanceof DingBlockEntity ding)
                || !(ding.hasFireBelow() || ding.isForging() || ding.isFinishingAnimation())) return;
        double scale = state.getValue(SCALE);
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = .72 * scale;
        double height = (ding.isLidOpen() && !ding.isForging() ? 1.77 : 1.80) * scale;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            level.addParticle(ModParticles.DING_GOLDEN_STEAM.get(),
                    pos.getX() + .5 + Math.cos(a) * radius, pos.getY() + height,
                    pos.getZ() + .5 + Math.sin(a) * radius,
                    Math.cos(a) * .002, .026 + random.nextDouble() * .012, Math.sin(a) * .002);
        }
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
        builder.add(FACING, SCALE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return shapeFor(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DingBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (canScaleWithHeldTool(state, player) && tryCycleScale(state, level, pos, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        openDingMenu(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (canScaleWithHeldTool(state, player) && tryCycleScale(state, level, pos, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        openDingMenu(state, level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openDingMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DingBlockEntity ding = getOrRepairDingBlockEntity(level, pos, state);
        if (ding.hasLid()) {
            if (player.isShiftKeyDown()) {
                if (ding.isLidOpen()) {
                    ding.setLidOpen(false);
                    if (serverPlayer.containerMenu instanceof com.example.immortal_cultivation_mod.screen.DingMenu menu
                            && menu.usesForge(ding)) serverPlayer.closeContainer();
                    return;
                }
                ding.setLidOpen(true);
                return;
            } else if (!ding.isLidOpen()) {
                return;
            }
        }
        serverPlayer.openMenu(ding);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                serverPlayer,
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.OPEN_DING);
    }

    private boolean canScaleWithHeldTool(BlockState state, Player player) {
        // Lidded dings reserve ordinary sneak-use for their lid. Debug scaling remains
        // available by holding the stat editor while Debug Eye is enabled.
        return state.is(ModBlocks.CU_TIE_XIAODING.get())
                || player.getMainHandItem().is(com.example.immortal_cultivation_mod.item.ModItems.DEBUG_STAT_EDITOR.get());
    }

    private DingBlockEntity getOrRepairDingBlockEntity(Level level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DingBlockEntity ding) {
            return ding;
        }

        // Older placed dings can still contain the generic GeoRock block entity.
        level.removeBlockEntity(pos);
        DingBlockEntity ding = new DingBlockEntity(pos, state);
        level.setBlockEntity(ding);
        ding.setChanged();
        return ding;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof DingBlockEntity ding) {
                DingBlockEntity.serverTick(tickLevel, pos, tickState, ding);
            }
        };
    }

    private static VoxelShape shapeFor(BlockState state) {
        if (state.is(ModBlocks.CHI_JIN_SANYANG_DING.get())) {
            return SANYANG_SHAPES[directionIndex(state.getValue(FACING))][state.getValue(SCALE) - MIN_SCALE];
        }
        VoxelShape[][] variants = state.is(ModBlocks.QINGTONG_YINHUO_DING.get())
                ? QINGTONG_SHAPES
                : CU_TIE_SHAPES;
        return variants[directionIndex(state.getValue(FACING))][state.getValue(SCALE) - MIN_SCALE];
    }

    private static VoxelShape[][] createScaledVariants(
            VoxelShape north, VoxelShape east, VoxelShape south, VoxelShape west) {
        VoxelShape[] directions = {north, east, south, west};
        VoxelShape[][] variants = new VoxelShape[directions.length][MAX_SCALE];
        for (int direction = 0; direction < directions.length; direction++) {
            for (int scale = MIN_SCALE; scale <= MAX_SCALE; scale++) {
                VoxelShape detailedShape = scale == MIN_SCALE
                        ? directions[direction]
                        : scaleShapeFromCenter(directions[direction], scale);
                variants[direction][scale - MIN_SCALE] = scale <= MAX_SIMPLIFIED_HITBOX_SCALE
                        ? simplifyToModerateDetail(detailedShape)
                        : detailedShape;
            }
        }
        return variants;
    }

    /** Keep 1x/2x easy to click; restore feet, handles and body contours above 2x. */
    private static VoxelShape[][] createDingDetailVariants(
            VoxelShape north, VoxelShape east, VoxelShape south, VoxelShape west) {
        VoxelShape[][] variants = createScaledVariants(north, east, south, west);
        for (VoxelShape[] direction : variants) {
            for (int scale = MIN_SCALE; scale <= MAX_SIMPLIFIED_HITBOX_SCALE; scale++) {
                direction[scale - MIN_SCALE] = Shapes.create(direction[scale - MIN_SCALE].bounds());
            }
        }
        return variants;
    }

    private static int directionIndex(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    /** Rotates a north-facing model-kit shape around the center of its block. */
    private static VoxelShape rotateHorizontal(VoxelShape source, Direction facing) {
        if (facing == Direction.NORTH) {
            return source;
        }

        VoxelShape[] rotated = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                case EAST -> Shapes.box(
                        1.0D - maxZ, minY, minX,
                        1.0D - minZ, maxY, maxX);
                case SOUTH -> Shapes.box(
                        1.0D - maxX, minY, 1.0D - maxZ,
                        1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> Shapes.box(
                        minZ, minY, 1.0D - maxX,
                        maxZ, maxY, 1.0D - minX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0].optimize();
    }

    /** Converts an unrotated GeckoLib cube into the centered block-render coordinate space. */
    private static VoxelShape modelBox(
            double originX, double originY, double originZ,
            double sizeX, double sizeY, double sizeZ, double inflate) {
        return Block.box(
                8.0D - originX - sizeX - inflate,
                originY - inflate,
                8.0D + originZ - inflate,
                8.0D - originX + inflate,
                originY + sizeY + inflate,
                8.0D + originZ + sizeZ + inflate);
    }
}
