package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Extends only the template's foundation columns down to terrain; never fills rooms. */
public final class SolidFoundationProcessor extends StructureProcessor {
    public static final MapCodec<SolidFoundationProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 8).optionalFieldOf("max_base_y", 0).forGetter(p -> p.maxBaseY),
            Codec.INT.optionalFieldOf("open_span_start", -1).forGetter(p -> p.openSpanStart),
            Codec.INT.optionalFieldOf("open_span_end", -1).forGetter(p -> p.openSpanEnd)
    ).apply(instance, SolidFoundationProcessor::new));
    public static final SolidFoundationProcessor INSTANCE = new SolidFoundationProcessor(0, -1, -1);
    private final int maxBaseY, openSpanStart, openSpanEnd;

    public SolidFoundationProcessor(int maxBaseY, int openSpanStart, int openSpanEnd) {
        this.maxBaseY = maxBaseY;
        this.openSpanStart = openSpanStart;
        this.openSpanEnd = openSpanEnd;
    }

    @Override
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pieceOrigin,
            List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings) {
        Map<Long, StructureBlockInfo> bases = new HashMap<>();
        var box = settings.getBoundingBox();
        for (int i = 0; i < Math.min(original.size(), processed.size()); i++) {
            BlockPos local = original.get(i).pos();
            if (local.getY() < 0 || local.getY() > maxBaseY) continue;
            // Bridge foundations belong on the banks; preserve the navigable water span in every rotation.
            if (openSpanStart >= 0 && local.getZ() >= openSpanStart && local.getZ() < openSpanEnd) continue;
            StructureBlockInfo block = processed.get(i);
            BlockPos pos = block.pos();
            if (box != null && (pos.getX() < box.minX() || pos.getX() > box.maxX()
                    || pos.getZ() < box.minZ() || pos.getZ() > box.maxZ())) continue;
            BlockState state = block.state();
            if (!(state.isCollisionShapeFullBlock(level, pos)
                    || state.getBlock() instanceof SlabBlock || state.getBlock() instanceof StairBlock
                    || state.hasBlockEntity() && !state.getCollisionShape(level, pos).isEmpty())) continue;
            long column = BlockPos.asLong(pos.getX(), 0, pos.getZ());
            StructureBlockInfo previous = bases.get(column);
            if (previous == null || pos.getY() < previous.pos().getY()) bases.put(column, block);
        }
        List<StructureBlockInfo> result = new ArrayList<>(processed);
        for (StructureBlockInfo base : bases.values()) {
            result.addAll(columnBelow(level, base.pos(), base.state(), box));
        }
        return result;
    }

    static List<StructureBlockInfo> columnBelow(ServerLevelAccessor level, BlockPos base, BlockState floor, BoundingBox box) {
        BlockState fill = foundationMaterial(floor);
        List<StructureBlockInfo> column = new ArrayList<>();
        BlockPos.MutableBlockPos below = base.below().mutable();
        // Bounds are checked before world reads: neighboring chunks generate independently.
        while (below.getY() >= level.getMinBuildHeight() && (box == null || box.isInside(below))) {
            BlockState existing = level.getBlockState(below);
            if (existing.hasBlockEntity() || (!existing.isAir() && !existing.canBeReplaced())) return column;
            column.add(new StructureBlockInfo(below.immutable(), fill, null));
            below.move(0, -1, 0);
        }
        // There is no foundation-to-terrain gap if the column opens into the bottomless void.
        return List.of();
    }

    public static void fillBelow(WorldGenLevel level, BlockPos base, BlockState floor, BoundingBox box) {
        for (StructureBlockInfo block : columnBelow(level, base, floor, box)) level.setBlock(block.pos(), block.state(), 2);
    }

    private static BlockState foundationMaterial(BlockState floor) {
        // A base-level altar or cabinet needs support, but must never be duplicated along with its inventory.
        if (floor.hasBlockEntity()) return Blocks.COBBLESTONE.defaultBlockState();
        if (floor.is(BlockTags.DIRT) || floor.is(Blocks.MOSS_BLOCK) || floor.is(Blocks.DIRT_PATH)
                || floor.is(Blocks.FARMLAND)) return Blocks.DIRT.defaultBlockState();
        if (floor.getBlock() instanceof FallingBlock) return Blocks.STONE.defaultBlockState();
        if (floor.getBlock() instanceof SlabBlock || floor.getBlock() instanceof StairBlock) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(floor.getBlock());
            String path = id.getPath().replaceFirst("_(slab|stairs)$", "");
            var material = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path));
            return material == Blocks.AIR ? Blocks.COBBLESTONE.defaultBlockState() : material.defaultBlockState();
        }
        return floor.hasProperty(BlockStateProperties.WATERLOGGED)
                ? floor.setValue(BlockStateProperties.WATERLOGGED, false) : floor;
    }

    @Override protected StructureProcessorType<?> getType() {
        return ModFeatures.SOLID_FOUNDATION_PROCESSOR.get();
    }
}
