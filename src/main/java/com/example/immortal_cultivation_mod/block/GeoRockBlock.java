package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeoRockBlock extends Block implements EntityBlock {
    public static final int MIN_SCALE = 1;
    public static final int MAX_SCALE = 5;
    protected static final int MAX_SIMPLIFIED_HITBOX_SCALE = 2;
    private static final int MODERATE_HITBOX_BOX_LIMIT = 12;
    public static final IntegerProperty SCALE = IntegerProperty.create("scale", MIN_SCALE, MAX_SCALE);
    private static final Map<Block, VoxelShape[]> SCALED_SHAPES = new ConcurrentHashMap<>();

    public GeoRockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SCALE, MIN_SCALE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SCALE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeoRockBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return scaledShapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return scaledShapeFor(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return tryCycleScale(state, level, pos, player)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        return tryCycleScale(state, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected final boolean tryCycleScale(
            BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        // Acknowledge the predicted click client-side. The server is authoritative
        // about Debug Eye access and the resulting block state.
        if (level.isClientSide) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !ModPayloads.isDebugEyeEnabled(serverPlayer)) {
            return false;
        }

        int currentScale = state.getValue(SCALE);
        int nextScale = currentScale >= MAX_SCALE ? MIN_SCALE : currentScale + 1;
        level.setBlock(pos, state.setValue(SCALE, nextScale), Block.UPDATE_ALL);
        serverPlayer.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".block_scale", nextScale), true);
        return true;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        int scale = state.getValue(SCALE);
        if (scale <= MIN_SCALE || !multipliesDropsWithScale(state)) {
            return drops;
        }

        for (ItemStack drop : drops) {
            if (drop.is(state.getBlock().asItem())) {
                drop.setCount(drop.getCount() * scale);
            }
        }
        return drops;
    }

    private static boolean multipliesDropsWithScale(BlockState state) {
        return state.is(ModBlocks.QIANNIAN_LINGMU_HEART.get())
                || state.is(ModBlocks.YUNWEN_SILVER_CRYSTAL.get())
                || state.is(ModBlocks.JINGTIE_MOTHERSTONE.get())
                || state.is(ModBlocks.CHITONG_SUI.get())
                || state.is(ModBlocks.HANPO_JADE.get())
                || state.is(ModBlocks.BISUI_STONE.get())
                || state.is(ModBlocks.STAR_DUST_STONE.get())
                || state.is(ModBlocks.MOXUAN_IRON.get());
    }

    private static VoxelShape scaledShapeFor(BlockState state) {
        int scale = state.getValue(SCALE);
        VoxelShape baseShape = GeneratedGeoRockShapes.shapeFor(state);
        VoxelShape[] variants = SCALED_SHAPES.computeIfAbsent(
                state.getBlock(), ignored -> createScaledVariants(baseShape));
        return variants[scale - MIN_SCALE];
    }

    protected static VoxelShape[] createScaledVariants(VoxelShape source) {
        VoxelShape[] variants = new VoxelShape[MAX_SCALE];
        for (int scale = MIN_SCALE; scale <= MAX_SCALE; scale++) {
            VoxelShape detailedShape = scale == MIN_SCALE
                    ? source
                    : scaleShapeFromCenter(source, scale);
            variants[scale - MIN_SCALE] = scale <= MAX_SIMPLIFIED_HITBOX_SCALE
                    ? simplifyToModerateDetail(detailedShape)
                    : detailedShape;
        }
        return variants;
    }

    /**
     * Keeps a recognizable, moderately detailed outline at 1x and 2x while
     * avoiding the full model-cube collision cost. Nearby boxes are merged
     * first, preserving feet, rims, branches, and crystal tiers much better
     * than a single bounding box.
     */
    protected static VoxelShape simplifyToModerateDetail(VoxelShape source) {
        return simplifyCollisionShape(source, MODERATE_HITBOX_BOX_LIMIT);
    }

    protected static VoxelShape simplifyCollisionShape(VoxelShape source, int boxLimit) {
        if (source.isEmpty()) {
            return Shapes.empty();
        }

        List<AABB> boxes = new ArrayList<>();
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ)));
        while (boxes.size() > boxLimit) {
            int bestFirst = 0;
            int bestSecond = 1;
            double bestCost = Double.POSITIVE_INFINITY;
            AABB bestMerged = null;

            for (int first = 0; first < boxes.size() - 1; first++) {
                AABB a = boxes.get(first);
                for (int second = first + 1; second < boxes.size(); second++) {
                    AABB b = boxes.get(second);
                    AABB merged = enclosingBox(a, b);
                    double cost = volume(merged) - volume(a) - volume(b) + intersectionVolume(a, b);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestFirst = first;
                        bestSecond = second;
                        bestMerged = merged;
                    }
                }
            }

            boxes.set(bestFirst, bestMerged);
            boxes.remove(bestSecond);
        }

        VoxelShape simplified = Shapes.empty();
        for (AABB box : boxes) {
            simplified = Shapes.or(simplified, Shapes.box(
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ));
        }
        return simplified.optimize();
    }

    private static AABB enclosingBox(AABB a, AABB b) {
        return new AABB(
                Math.min(a.minX, b.minX), Math.min(a.minY, b.minY), Math.min(a.minZ, b.minZ),
                Math.max(a.maxX, b.maxX), Math.max(a.maxY, b.maxY), Math.max(a.maxZ, b.maxZ));
    }

    private static double volume(AABB box) {
        return (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
    }

    private static double intersectionVolume(AABB a, AABB b) {
        double x = Math.max(0.0D, Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX));
        double y = Math.max(0.0D, Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY));
        double z = Math.max(0.0D, Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ));
        return x * y * z;
    }

    /** Scales X/Z around the block center and Y upward from the floor. */
    protected static VoxelShape scaleShapeFromCenter(VoxelShape source, int scale) {
        VoxelShape[] scaled = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double scaledMinX = 0.5D + (minX - 0.5D) * scale;
            double scaledMaxX = 0.5D + (maxX - 0.5D) * scale;
            double scaledMinZ = 0.5D + (minZ - 0.5D) * scale;
            double scaledMaxZ = 0.5D + (maxZ - 0.5D) * scale;
            scaled[0] = Shapes.or(scaled[0], Shapes.box(
                    scaledMinX, minY * scale, scaledMinZ,
                    scaledMaxX, maxY * scale, scaledMaxZ));
        });
        return scaled[0].optimize();
    }

    @Override
    protected boolean triggerEvent(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType pathComputationType) {
        return false;
    }
}
