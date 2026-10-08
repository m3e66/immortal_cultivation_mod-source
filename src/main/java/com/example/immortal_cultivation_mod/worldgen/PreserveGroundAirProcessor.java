package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Leaves natural terrain in empty cells on a template's foundation layer.
 * Air above the foundation is still placed so terrain cannot clip through the
 * structure itself.
 */
public final class PreserveGroundAirProcessor extends StructureProcessor {
    public static final PreserveGroundAirProcessor INSTANCE =
            new PreserveGroundAirProcessor();
    public static final MapCodec<PreserveGroundAirProcessor> CODEC =
            MapCodec.unit(() -> INSTANCE);

    private PreserveGroundAirProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos structureOrigin,
            BlockPos pieceOrigin,
            StructureTemplate.StructureBlockInfo originalInfo,
            StructureTemplate.StructureBlockInfo currentInfo,
            StructurePlaceSettings settings) {
        if (originalInfo.pos().getY() == 0 && currentInfo.state().isAir()) {
            return null;
        }
        return currentInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModFeatures.PRESERVE_GROUND_AIR_PROCESSOR.get();
    }
}
