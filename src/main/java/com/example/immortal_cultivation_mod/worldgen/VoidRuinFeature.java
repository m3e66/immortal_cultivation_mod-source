package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Places small, broken cultivation platforms without turning the void into a normal terrain world. */
public final class VoidRuinFeature extends Feature<NoneFeatureConfiguration> {
    private static final Object PLACEMENT_LOCK = new Object();
    private static final Map<Long, List<BlockPos>> PLACED_RUINS_BY_SEED = new HashMap<>();
    private static final int RUIN_SEARCH_GRID = 4;
    private static final int MIN_RUIN_SPACING = 500;
    private static final BlockState FOUNDATION = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState RUNE = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState CORE = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final FormationCell[] TELEPORT_PATTERN = {
            new FormationCell(-1, -2, CircleType.ATTACK),
            new FormationCell(1, -2, CircleType.ATTACK),
            new FormationCell(-2, -1, CircleType.DEFENSE),
            new FormationCell(0, -1, CircleType.REGENERATION),
            new FormationCell(2, -1, CircleType.DEFENSE),
            new FormationCell(-1, 0, CircleType.REGENERATION),
            new FormationCell(1, 0, CircleType.REGENERATION),
            new FormationCell(-2, 1, CircleType.DEFENSE),
            new FormationCell(0, 1, CircleType.REGENERATION),
            new FormationCell(2, 1, CircleType.DEFENSE),
            new FormationCell(-1, 2, CircleType.ATTACK),
            new FormationCell(1, 2, CircleType.ATTACK)
    };

    public VoidRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // Neighboring chunks may decorate concurrently. Serializing this rare feature lets the
        // embedded marker prevent two decoration passes from building on the same void island.
        synchronized (PLACEMENT_LOCK) {
            return placeLocked(context);
        }
    }

    private boolean placeLocked(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos initialSurface = findSurface(level, context.origin());
        BlockPos surface = initialSurface == null ? null : findAlignedSurface(level, initialSurface);
        if (surface == null || !level.getBlockState(surface.below()).is(ModBlocks.VOID_STONE.get())) {
            return false;
        }
        long worldSeed = level.getSeed();
        if (hasRememberedRuin(worldSeed, surface) || hasNearbyRuin(level, surface)) {
            return false;
        }

        int radius = 9 + random.nextInt(4);
        int rotation = random.nextInt(4);
        boolean placed = placePlatform(level, surface, radius, rotation, random);
        if (!placed) {
            return false;
        }

        prepareFormationDais(level, surface);
        placeHalfBuiltTeleportPattern(level, surface.above(), rotation, random);
        placeBrokenPillars(level, surface, radius, rotation, random);
        rememberRuin(worldSeed, surface);
        return true;
    }

    private static boolean hasRememberedRuin(long worldSeed, BlockPos center) {
        List<BlockPos> ruins = PLACED_RUINS_BY_SEED.get(worldSeed);
        if (ruins == null) {
            return false;
        }
        long radiusSquared = (long) MIN_RUIN_SPACING * MIN_RUIN_SPACING;
        for (BlockPos ruin : ruins) {
            long dx = (long) ruin.getX() - center.getX();
            long dz = (long) ruin.getZ() - center.getZ();
            if (dx * dx + dz * dz <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private static void rememberRuin(long worldSeed, BlockPos center) {
        PLACED_RUINS_BY_SEED.computeIfAbsent(worldSeed, ignored -> new ArrayList<>())
                .add(center.immutable());
    }

    private static boolean placePlatform(WorldGenLevel level, BlockPos center, int radius,
                                         int rotation, RandomSource random) {
        boolean placed = false;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distance = x * x + z * z;
                if (distance > radius * radius + radius) {
                    continue;
                }
                int innerRadius = Math.max(4, radius - 4);
                boolean outerRing = Math.abs(distance - radius * radius) <= radius * 2;
                boolean innerRing = Math.abs(distance - innerRadius * innerRadius) <= radius;
                boolean spokes = x == 0 || z == 0 || Math.abs(x) == Math.abs(z);
                boolean survivingFloor = distance < (radius - 1) * (radius - 1)
                        && random.nextFloat() < 0.58F;
                boolean broken = random.nextFloat() < (outerRing ? 0.19F : 0.11F);
                if ((!outerRing && !innerRing && !spokes && !survivingFloor) || broken) {
                    continue;
                }
                BlockPos pos = center.offset(x, 0, z);
                if (!level.getBlockState(pos).canBeReplaced()) {
                    continue;
                }
                BlockState state = (spokes || innerRing)
                        && ((Math.abs(x) + Math.abs(z) + rotation) % 3 == 0)
                        ? RUNE : FOUNDATION;
                level.setBlock(pos, state, 2);
                SolidFoundationProcessor.fillBelow(level, pos, state, null);
                placed = true;
            }
        }
        return placed;
    }

    private static void prepareFormationDais(WorldGenLevel level, BlockPos center) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int edge = Math.max(Math.abs(x), Math.abs(z));
                BlockState state = edge == 3 || x == 0 || z == 0 ? RUNE : FOUNDATION;
                BlockPos floor = center.offset(x, 0, z);
                level.setBlock(floor, state, 2);
                SolidFoundationProcessor.fillBelow(level, floor, state, null);
            }
        }
        // A buried, globally grid-aligned marker makes later chunk decoration passes recognize
        // that this island already owns a ruin.
        level.setBlock(center, CORE, 2);
    }

    private static void placeHalfBuiltTeleportPattern(WorldGenLevel level, BlockPos center,
                                                       int rotation, RandomSource random) {
        level.setBlock(center,
                com.example.immortal_cultivation_mod.magicritual.block.ModBlocks
                        .MAGIC_CENTER_BLOCK.get().defaultBlockState(), 2);

        int[] order = new int[TELEPORT_PATTERN.length];
        for (int index = 0; index < order.length; index++) {
            order[index] = index;
        }
        for (int index = order.length - 1; index > 0; index--) {
            int swapWith = random.nextInt(index + 1);
            int swapped = order[index];
            order[index] = order[swapWith];
            order[swapWith] = swapped;
        }

        // Six of the twelve required circle blocks survive: visibly and mechanically half-built.
        for (int index = 0; index < TELEPORT_PATTERN.length / 2; index++) {
            FormationCell cell = TELEPORT_PATTERN[order[index]];
            BlockPos offset = rotate(cell.x(), cell.z(), rotation);
            BlockState circle = com.example.immortal_cultivation_mod.magicritual.block.ModBlocks
                    .MAGIC_CIRCLE_BLOCK.get().defaultBlockState()
                    .setValue(MagicCircleBlock.CIRCLE_TYPE, cell.type());
            level.setBlock(center.offset(offset), circle, 2);
        }
    }

    private static BlockPos rotate(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 1 -> new BlockPos(-z, 0, x);
            case 2 -> new BlockPos(-x, 0, -z);
            case 3 -> new BlockPos(z, 0, -x);
            default -> new BlockPos(x, 0, z);
        };
    }

    private static BlockPos findSurface(WorldGenLevel level, BlockPos origin) {
        int top = Math.min(level.getMaxBuildHeight() - 2, Math.max(origin.getY(), 120));
        int bottom = level.getMinBuildHeight() + 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), top, origin.getZ());
        for (int y = top; y >= bottom; y--) {
            cursor.setY(y);
            if (!level.getBlockState(cursor).isAir()) {
                return cursor.above().immutable();
            }
        }
        return null;
    }

    private static BlockPos findAlignedSurface(WorldGenLevel level, BlockPos near) {
        int alignedX = Math.floorDiv(near.getX() + RUIN_SEARCH_GRID / 2, RUIN_SEARCH_GRID)
                * RUIN_SEARCH_GRID;
        int alignedZ = Math.floorDiv(near.getZ() + RUIN_SEARCH_GRID / 2, RUIN_SEARCH_GRID)
                * RUIN_SEARCH_GRID;
        for (int ring = 0; ring <= 3; ring++) {
            for (int gridX = -ring; gridX <= ring; gridX++) {
                for (int gridZ = -ring; gridZ <= ring; gridZ++) {
                    if (ring > 0 && Math.abs(gridX) != ring && Math.abs(gridZ) != ring) {
                        continue;
                    }
                    BlockPos candidate = findSurface(level, new BlockPos(
                            alignedX + gridX * RUIN_SEARCH_GRID,
                            near.getY(),
                            alignedZ + gridZ * RUIN_SEARCH_GRID));
                    if (candidate != null
                            && level.getBlockState(candidate.below()).is(ModBlocks.VOID_STONE.get())) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasNearbyRuin(WorldGenLevel level, BlockPos center) {
        int minX = Math.floorDiv(center.getX() - MIN_RUIN_SPACING, RUIN_SEARCH_GRID)
                * RUIN_SEARCH_GRID;
        int minZ = Math.floorDiv(center.getZ() - MIN_RUIN_SPACING, RUIN_SEARCH_GRID)
                * RUIN_SEARCH_GRID;
        int maxX = center.getX() + MIN_RUIN_SPACING;
        int maxZ = center.getZ() + MIN_RUIN_SPACING;
        int radiusSquared = MIN_RUIN_SPACING * MIN_RUIN_SPACING;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x += RUIN_SEARCH_GRID) {
            for (int z = minZ; z <= maxZ; z += RUIN_SEARCH_GRID) {
                int dx = x - center.getX();
                int dz = z - center.getZ();
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                probe.set(x, center.getY(), z);
                if (!level.hasChunkAt(probe)) {
                    continue;
                }
                int top = Math.min(level.getMaxBuildHeight() - 1,
                        level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
                int bottom = Math.max(level.getMinBuildHeight(), top - 8);
                for (int y = top; y >= bottom; y--) {
                    probe.set(x, y, z);
                    if (level.getBlockState(probe).is(CORE.getBlock())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void placeBrokenPillars(WorldGenLevel level, BlockPos center, int radius,
                                           int rotation, RandomSource random) {
        int edge = Math.max(5, radius - 2);
        int[][] anchors = {
                {edge, edge}, {-edge, edge}, {-edge, -edge}, {edge, -edge},
                {0, radius}, {radius, 0}, {0, -radius}, {-radius, 0}
        };
        for (int index = 0; index < anchors.length; index++) {
            if (random.nextFloat() < 0.32F) {
                continue;
            }
            int[] anchor = anchors[(index + rotation * 2) & 7];
            int height = 4 + random.nextInt(6);
            for (int y = 1; y <= height; y++) {
                BlockPos pos = center.offset(anchor[0], y, anchor[1]);
                if (!level.getBlockState(pos).canBeReplaced()) {
                    break;
                }
                level.setBlock(pos, y == height ? RUNE : FOUNDATION, 2);
            }
        }
    }

    private record FormationCell(int x, int z, CircleType type) {
    }
}
