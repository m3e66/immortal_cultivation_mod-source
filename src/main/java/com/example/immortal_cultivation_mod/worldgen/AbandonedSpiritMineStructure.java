package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import net.minecraft.world.level.storage.loot.LootTable;

/** A vanilla-shaped mineshaft whose pieces are marked for spirit-ore decoration. */
public final class AbandonedSpiritMineStructure extends MineshaftStructure {
    public static final MapCodec<AbandonedSpiritMineStructure> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    settingsCodec(instance),
                    MineshaftStructure.Type.CODEC.fieldOf("mineshaft_type")
                            .forGetter(structure -> structure.mineType))
                    .apply(instance, AbandonedSpiritMineStructure::new));

    public static final ResourceKey<LootTable> MINECART_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "chests/abandoned_spirit_mine"));

    private final MineshaftStructure.Type mineType;

    public AbandonedSpiritMineStructure(Structure.StructureSettings settings,
                                        MineshaftStructure.Type mineType) {
        super(settings, mineType);
        this.mineType = mineType;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        AbandonedSpiritMineGeneration.begin();
        try {
            return super.findGenerationPoint(context).map(original -> {
                // Vanilla mineshafts provide an already-built generator on the
                // right side of the Either. Do not assume it is a deferred
                // consumer: doing so crashes chunk generation with an empty
                // Optional. Reuse the finished pieces and raise them safely.
                StructurePiecesBuilder pieces = original.getPiecesBuilder();
                pieces.offsetPiecesVertically(2);
                return new Structure.GenerationStub(
                        original.position().above(2), Either.right(pieces));
            });
        } finally {
            AbandonedSpiritMineGeneration.end();
        }
    }

    @Override
    public StructureType<?> type() {
        return ModFeatures.ABANDONED_SPIRIT_MINE.get();
    }
}
