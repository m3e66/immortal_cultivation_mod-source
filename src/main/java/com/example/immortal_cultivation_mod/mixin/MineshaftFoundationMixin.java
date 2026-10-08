package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.worldgen.AbandonedSpiritMinePiece;
import com.example.immortal_cultivation_mod.worldgen.SolidFoundationProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MineshaftPieces.MineShaftCorridor.class, MineshaftPieces.MineShaftCrossing.class,
        MineshaftPieces.MineShaftRoom.class, MineshaftPieces.MineShaftStairs.class})
public abstract class MineshaftFoundationMixin {
    @Inject(method = "postProcess", at = @At("TAIL"))
    private void immortalCultivation$supportMineFloor(WorldGenLevel level, StructureManager structures,
            ChunkGenerator generator, RandomSource random, BoundingBox generationBox, ChunkPos chunk,
            BlockPos origin, CallbackInfo ci) {
        if (!((Object)this instanceof AbandonedSpiritMinePiece marked) || !marked.immortalCultivation$isAbandonedSpiritMine()) return;
        BoundingBox piece = ((StructurePiece)(Object)this).getBoundingBox();
        for (int x = Math.max(piece.minX(), generationBox.minX()); x <= Math.min(piece.maxX(), generationBox.maxX()); x++) {
            for (int z = Math.max(piece.minZ(), generationBox.minZ()); z <= Math.min(piece.maxZ(), generationBox.maxZ()); z++) {
                BlockPos floor = new BlockPos(x, piece.minY() - 1, z);
                if (!generationBox.isInside(floor)) continue;
                var state = level.getBlockState(floor);
                if (!state.hasBlockEntity() && state.isCollisionShapeFullBlock(level, floor))
                    SolidFoundationProcessor.fillBelow(level, floor, state, generationBox);
            }
        }
    }
}
