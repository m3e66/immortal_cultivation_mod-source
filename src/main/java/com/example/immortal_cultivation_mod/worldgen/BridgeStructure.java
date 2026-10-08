package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Places bridge1 only where its two outer thirds are over land and its middle
 * third crosses water. The terrain checks use the template's actual rotation.
 */
public final class BridgeStructure extends Structure {
    public static final MapCodec<BridgeStructure> CODEC =
            simpleCodec(BridgeStructure::new);

    private static final ResourceLocation TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "bridge1");
    private static final ResourceKey<StructureTemplatePool> START_POOL =
            ResourceKey.create(Registries.TEMPLATE_POOL,
                    ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID,
                            "bridge1/start_pool"));
    private static final Rotation[] ROTATIONS = {
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90
    };
    /*
     * Keep generation-point lookup bounded. /locate invokes this method for
     * many candidate chunks; taking the Cartesian product of five X offsets,
     * five Z offsets, four rotations, and every row in the template made one
     * locate command request thousands of complete noise columns per chunk.
     * These five positions still let a random-spread candidate snap from the
     * river bank toward the channel without turning locate into chunkgen.
     */
    private static final int[][] SEARCH_OFFSETS = {
            {0, 0}, {-6, 0}, {6, 0}, {0, -6}, {0, 6}
    };
    private static final int REQUIRED_SIDE_PERCENT = 80;

    public BridgeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(
            GenerationContext context) {
        Holder<StructureTemplatePool> pool = context.registryAccess()
                .registryOrThrow(Registries.TEMPLATE_POOL)
                .getHolderOrThrow(START_POOL);
        StructurePoolElement element = pool.value()
                .getRandomTemplate(context.random());
        if (element == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        }

        StructureTemplate template = context.structureTemplateManager()
                .getOrCreate(TEMPLATE_ID);
        Vec3i size = template.getSize();
        if (size.getX() < 3 || size.getZ() < 6) {
            return Optional.empty();
        }

        ChunkPos chunk = context.chunkPos();
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        int rotationStart = context.random().nextInt(ROTATIONS.length);
        Map<Long, TerrainSample> terrainCache = new HashMap<>();

        for (int[] offset : SEARCH_OFFSETS) {
            int candidateX = centerX + offset[0];
            int candidateZ = centerZ + offset[1];
            // Adjacent rotations cover both possible bridge axes. The two
            // opposite rotations test effectively the same terrain footprint.
            for (int index = 0; index < ROTATIONS.length / 2; index++) {
                Rotation rotation = ROTATIONS[
                        (rotationStart + index) % ROTATIONS.length];
                Candidate candidate = candidate(
                        context, element, size, rotation,
                        candidateX, candidateZ, terrainCache);
                if (candidate == null
                        || !isValidBiome(context, candidate)) {
                    continue;
                }

                PoolElementStructurePiece piece =
                        new PoolElementStructurePiece(
                                context.structureTemplateManager(),
                                element,
                                candidate.templatePosition(),
                                element.getGroundLevelDelta(),
                                rotation,
                                candidate.boundingBox(),
                                JigsawStructure.DEFAULT_LIQUID_SETTINGS);
                int placementHeight = candidate.surfaceHeight() + 2;
                int verticalMove = placementHeight
                        - (piece.getBoundingBox().minY()
                        + piece.getGroundLevelDelta());
                piece.move(0, verticalMove, 0);
                BlockPos generationCenter = new BlockPos(
                        candidateX, placementHeight,
                        candidateZ);
                return Optional.of(new GenerationStub(
                        generationCenter, builder ->
                        builder.addPiece(piece)));
            }
        }
        return Optional.empty();
    }

    private static Candidate candidate(
            GenerationContext context,
            StructurePoolElement element,
            Vec3i size,
            Rotation rotation,
            int candidateX,
            int candidateZ,
            Map<Long, TerrainSample> cache) {
        BoundingBox zeroBox = element.getBoundingBox(
                context.structureTemplateManager(), BlockPos.ZERO,
                rotation);
        int zeroCenterX = (zeroBox.minX() + zeroBox.maxX()) / 2;
        int zeroCenterZ = (zeroBox.minZ() + zeroBox.maxZ()) / 2;
        BlockPos templatePosition = new BlockPos(
                candidateX - zeroCenterX, 0,
                candidateZ - zeroCenterZ);
        BoundingBox box = element.getBoundingBox(
                context.structureTemplateManager(), templatePosition,
                rotation);

        int width = size.getX();
        int length = size.getZ();
        int centerLocalX = width / 2;
        int waterStart = length / 3;
        int waterEnd = length * 2 / 3;
        int sideMatches = 0;
        int sideChecks = 0;
        List<Integer> waterHeights = new ArrayList<>();
        List<LandProbe> landProbes = new ArrayList<>();

        for (int localZ : probeRows(length, waterStart, waterEnd)) {
            boolean waterSpan = localZ >= waterStart
                    && localZ < waterEnd;
            TerrainKind expected = waterSpan
                    ? TerrainKind.WATER : TerrainKind.LAND;
            BlockPos center = transform(templatePosition, rotation,
                    centerLocalX, localZ);
            TerrainSample centerSample = sample(context, center, cache);
            if (centerSample.kind() != expected) {
                return null;
            }
            if (waterSpan) {
                waterHeights.add(centerSample.surfaceHeight());
            } else {
                landProbes.add(new LandProbe(
                        localZ, centerSample.surfaceHeight()));
            }

            for (int localX : new int[]{0, width - 1}) {
                BlockPos side = transform(templatePosition, rotation,
                        localX, localZ);
                TerrainSample sideSample = sample(context, side, cache);
                if (sideSample.kind() == expected) {
                    sideMatches++;
                }
                sideChecks++;
            }
        }

        if (waterHeights.isEmpty()
                || sideMatches * 100
                < sideChecks * REQUIRED_SIDE_PERCENT) {
            return null;
        }
        int waterMinimum = waterHeights.stream()
                .min(Comparator.naturalOrder()).orElseThrow();
        int waterMaximum = waterHeights.stream()
                .max(Comparator.naturalOrder()).orElseThrow();
        if (waterMaximum - waterMinimum > 1) {
            return null;
        }
        int surfaceHeight = median(waterHeights);

        // The banks may vary slightly, but may not rise through the bridge or
        // drop away far enough that the blue-marked spans visibly float.
        for (LandProbe probe : landProbes) {
            int deckY = deckHeight(probe.localZ(), length);
            int deckBlockY = surfaceHeight - 1 + deckY;
            if (probe.surfaceHeight() > deckBlockY + 1
                    || probe.surfaceHeight() < surfaceHeight - 2) {
                return null;
            }
        }

        return new Candidate(templatePosition, box, surfaceHeight);
    }

    /**
     * Representative rows for both complete land spans and the water span.
     * Sampling the boundaries, centers, and ends preserves the intended
     * placement test while keeping structure lookup constant-time.
     */
    private static int[] probeRows(
            int length, int waterStart, int waterEnd) {
        return new int[]{
                0,
                waterStart / 2,
                waterStart - 1,
                waterStart,
                (waterStart + waterEnd - 1) / 2,
                waterEnd - 1,
                waterEnd,
                waterEnd + (length - waterEnd) / 2,
                length - 1
        };
    }

    private static BlockPos transform(
            BlockPos templatePosition, Rotation rotation,
            int localX, int localZ) {
        return StructureTemplate.transform(
                        new BlockPos(localX, 0, localZ),
                        Mirror.NONE, rotation, BlockPos.ZERO)
                .offset(templatePosition);
    }

    private static TerrainSample sample(
            GenerationContext context, BlockPos pos,
            Map<Long, TerrainSample> cache) {
        long key = BlockPos.asLong(pos.getX(), 0, pos.getZ());
        TerrainSample cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        int surfaceHeight = context.chunkGenerator().getFirstFreeHeight(
                pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        NoiseColumn column = context.chunkGenerator().getBaseColumn(
                pos.getX(), pos.getZ(), context.heightAccessor(),
                context.randomState());
        BlockState top = column.getBlock(surfaceHeight - 1);
        TerrainKind kind;
        if (top.getFluidState().is(FluidTags.WATER)
                || top.is(Blocks.ICE)
                || top.is(Blocks.FROSTED_ICE)) {
            kind = TerrainKind.WATER;
        } else if (top.isAir() || !top.getFluidState().isEmpty()) {
            kind = TerrainKind.OTHER;
        } else {
            kind = TerrainKind.LAND;
        }
        TerrainSample result = new TerrainSample(kind, surfaceHeight);
        cache.put(key, result);
        return result;
    }

    private static boolean isValidBiome(
            GenerationContext context, Candidate candidate) {
        BoundingBox box = candidate.boundingBox();
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(candidate.surfaceHeight()),
                QuartPos.fromBlock(centerZ),
                context.randomState().sampler());
        return context.validBiome().test(biome);
    }

    private static int median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }

    /** Lowest occupied bridge level for each length segment in bridge1. */
    private static int deckHeight(int localZ, int length) {
        int waterStart = length / 3;
        int waterEnd = length * 2 / 3;
        int ramp = Math.max(1, length / 6 - 1);
        if (localZ >= waterStart && localZ < waterEnd) {
            return 2;
        }
        if (localZ >= waterStart - ramp
                && localZ < waterEnd + ramp) {
            return 1;
        }
        return 0;
    }

    @Override
    public StructureType<?> type() {
        return ModFeatures.BRIDGE.get();
    }

    private enum TerrainKind {
        LAND,
        WATER,
        OTHER
    }

    private record TerrainSample(
            TerrainKind kind, int surfaceHeight) {
    }

    private record LandProbe(
            int localZ, int surfaceHeight) {
    }

    private record Candidate(
            BlockPos templatePosition,
            BoundingBox boundingBox,
            int surfaceHeight) {
    }
}
