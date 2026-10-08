package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.worldgen.AbandonedSpiritMineGeneration;
import com.example.immortal_cultivation_mod.worldgen.AbandonedSpiritMinePiece;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces$MineShaftPiece")
public abstract class MineshaftPieceMixin implements AbandonedSpiritMinePiece {
    @Unique
    private static final String IMMORTAL_CULTIVATION_SPIRIT_MINE_TAG = "ICMSpiritMine";

    @Unique
    private boolean immortalCultivation$abandonedSpiritMine;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePieceType;ILnet/minecraft/world/level/levelgen/structure/structures/MineshaftStructure$Type;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)V",
            at = @At("RETURN"))
    private void immortalCultivation$markGeneratedPiece(StructurePieceType pieceType, int generationDepth,
                                                        MineshaftStructure.Type mineType,
                                                        BoundingBox boundingBox, CallbackInfo callback) {
        this.immortalCultivation$abandonedSpiritMine = AbandonedSpiritMineGeneration.isActive();
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePieceType;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("RETURN"))
    private void immortalCultivation$loadMarkedPiece(StructurePieceType pieceType, CompoundTag tag,
                                                     CallbackInfo callback) {
        this.immortalCultivation$abandonedSpiritMine =
                tag.getBoolean(IMMORTAL_CULTIVATION_SPIRIT_MINE_TAG);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void immortalCultivation$saveMarkedPiece(StructurePieceSerializationContext context,
                                                     CompoundTag tag, CallbackInfo callback) {
        if (this.immortalCultivation$abandonedSpiritMine) {
            tag.putBoolean(IMMORTAL_CULTIVATION_SPIRIT_MINE_TAG, true);
        }
    }

    @Override
    public boolean immortalCultivation$isAbandonedSpiritMine() {
        return this.immortalCultivation$abandonedSpiritMine;
    }
}
