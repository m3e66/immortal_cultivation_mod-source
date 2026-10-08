package com.example.immortal_cultivation_mod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Retained as the cave pool's compatibility processor. Railing directions are
 * transformed by UltramarineRailingTransformMixin at the same stage Minecraft
 * transforms every other block state, including direct template placement.
 */
public final class CaveRailingProcessor extends StructureProcessor {
    public static final CaveRailingProcessor INSTANCE = new CaveRailingProcessor();
    public static final MapCodec<CaveRailingProcessor> CODEC =
            MapCodec.unit(() -> INSTANCE);

    private CaveRailingProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos structureOrigin,
            BlockPos pieceOrigin,
            StructureTemplate.StructureBlockInfo originalInfo,
            StructureTemplate.StructureBlockInfo currentInfo,
            StructurePlaceSettings settings) {
        return currentInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModFeatures.CAVE_RAILING_PROCESSOR.get();
    }
}
