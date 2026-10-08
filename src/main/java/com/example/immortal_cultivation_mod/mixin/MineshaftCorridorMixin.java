package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.worldgen.AbandonedSpiritMineDecorator;
import com.example.immortal_cultivation_mod.worldgen.AbandonedSpiritMinePiece;
import com.example.immortal_cultivation_mod.worldgen.AbandonedSpiritMineStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MineshaftPieces.MineShaftCorridor.class)
public abstract class MineshaftCorridorMixin {
    @ModifyVariable(method = "createChest", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ResourceKey<LootTable> immortalCultivation$useSpiritMineLoot(
            ResourceKey<LootTable> originalLoot) {
        if ((Object) this instanceof AbandonedSpiritMinePiece piece
                && piece.immortalCultivation$isAbandonedSpiritMine()) {
            return AbandonedSpiritMineStructure.MINECART_LOOT;
        }
        return originalLoot;
    }

    @Inject(method = "postProcess", at = @At("TAIL"))
    private void immortalCultivation$addSpiritOreWalls(WorldGenLevel level,
                                                       StructureManager structureManager,
                                                       ChunkGenerator chunkGenerator,
                                                       RandomSource random,
                                                       BoundingBox generationBox,
                                                       ChunkPos chunkPos,
                                                       BlockPos structureOrigin,
                                                       CallbackInfo callback) {
        if ((Object) this instanceof AbandonedSpiritMinePiece piece
                && piece.immortalCultivation$isAbandonedSpiritMine()) {
            AbandonedSpiritMineDecorator.decorateCorridor(
                    level, (StructurePiece) (Object) this, generationBox);
        }
    }
}
