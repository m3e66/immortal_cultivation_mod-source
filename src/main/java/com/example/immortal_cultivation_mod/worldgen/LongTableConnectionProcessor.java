package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.function.UnaryOperator;

/**
 * Corrects Ultramarine long-table connections after a structure transform.
 *
 * <p>Ultramarine rotates the table axis but does not correctly rotate its
 * left/right connection flags for every rotation. That places the raised end
 * pieces inside the table instead of at its outer ends. Structure processors
 * receive the original state and placement transform, so the correct state can
 * be rebuilt without relying on the broken block transform.</p>
 */
public final class LongTableConnectionProcessor extends StructureProcessor {
    public static final LongTableConnectionProcessor INSTANCE =
            new LongTableConnectionProcessor();
    public static final MapCodec<LongTableConnectionProcessor> CODEC =
            MapCodec.unit(() -> INSTANCE);

    private static final ResourceLocation LONG_TABLE =
            ResourceLocation.fromNamespaceAndPath(
                    "ultramarine", "long_table");

    private LongTableConnectionProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos structureOrigin,
            BlockPos pieceOrigin,
            StructureTemplate.StructureBlockInfo originalInfo,
            StructureTemplate.StructureBlockInfo currentInfo,
            StructurePlaceSettings settings) {
        if (!LONG_TABLE.equals(BuiltInRegistries.BLOCK.getKey(
                originalInfo.state().getBlock()))) {
            return currentInfo;
        }

        UnaryOperator<Direction> transform = direction ->
                settings.getRotation().rotate(
                        settings.getMirror().mirror(direction));
        BlockState corrected = transformConnections(
                originalInfo.state(), transform);
        return new StructureTemplate.StructureBlockInfo(
                currentInfo.pos(), corrected, currentInfo.nbt());
    }

    private static BlockState transformConnections(
            BlockState source,
            UnaryOperator<Direction> directionTransform) {
        Property<?> axisProperty = source.getBlock().getStateDefinition()
                .getProperty("axis");
        Property<?> leftProperty = source.getBlock().getStateDefinition()
                .getProperty("left");
        Property<?> rightProperty = source.getBlock().getStateDefinition()
                .getProperty("right");
        if (axisProperty == null || leftProperty == null
                || rightProperty == null) {
            return source;
        }

        Object axisValue = source.getValues().get(axisProperty);
        if (!(axisValue instanceof Direction.Axis sourceAxis)) {
            return source;
        }

        Direction sourceLeft = sourceAxis == Direction.Axis.X
                ? Direction.EAST : Direction.NORTH;
        Direction sourceRight = sourceAxis == Direction.Axis.X
                ? Direction.WEST : Direction.SOUTH;
        Direction targetAxisDirection = directionTransform.apply(sourceLeft);

        BlockState transformed = setValue(
                source, axisProperty, targetAxisDirection.getAxis());
        transformed = setValue(transformed, leftProperty, false);
        transformed = setValue(transformed, rightProperty, false);

        if (Boolean.TRUE.equals(source.getValues().get(leftProperty))) {
            transformed = addConnection(transformed,
                    directionTransform.apply(sourceLeft),
                    leftProperty, rightProperty);
        }
        if (Boolean.TRUE.equals(source.getValues().get(rightProperty))) {
            transformed = addConnection(transformed,
                    directionTransform.apply(sourceRight),
                    leftProperty, rightProperty);
        }
        return transformed;
    }

    private static BlockState addConnection(
            BlockState state,
            Direction direction,
            Property<?> leftProperty,
            Property<?> rightProperty) {
        boolean isLeft = direction == Direction.EAST
                || direction == Direction.NORTH;
        return setValue(state,
                isLeft ? leftProperty : rightProperty, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setValue(
            BlockState state, Property<?> property, Comparable value) {
        return state.setValue((Property) property, value);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModFeatures.LONG_TABLE_CONNECTION_PROCESSOR.get();
    }
}
