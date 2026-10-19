package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ImmortalCultivationMod.MODID);
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, ImmortalCultivationMod.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ImmortalCultivationMod.MODID);
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ImmortalCultivationMod.MODID);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<ScaledVoidIslands>>
            SCALED_VOID_ISLANDS = DENSITY_FUNCTION_TYPES.register("scaled_void_islands", () -> ScaledVoidIslands.MAP_CODEC);

    public static final DeferredHolder<Feature<?>, SurfacePatchFeature> SURFACE_PATCH =
            FEATURES.register("surface_patch", () -> new SurfacePatchFeature(SurfacePatchConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, VoidRuinFeature> VOID_RUIN =
            FEATURES.register("void_ruin",
                    () -> new VoidRuinFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<StructureProcessorType<CaveRailingProcessor>> CAVE_RAILING_PROCESSOR =
            STRUCTURE_PROCESSORS.register("cave_railing",
                    () -> () -> CaveRailingProcessor.CODEC);

    public static final Supplier<StructureProcessorType<PreserveGroundAirProcessor>>
            PRESERVE_GROUND_AIR_PROCESSOR =
            STRUCTURE_PROCESSORS.register("preserve_ground_air",
                    () -> () -> PreserveGroundAirProcessor.CODEC);

    public static final Supplier<StructureProcessorType<LongTableConnectionProcessor>>
            LONG_TABLE_CONNECTION_PROCESSOR =
            STRUCTURE_PROCESSORS.register("long_table_connection",
                    () -> () -> LongTableConnectionProcessor.CODEC);

    public static final Supplier<StructureProcessorType<SolidFoundationProcessor>> SOLID_FOUNDATION_PROCESSOR =
            STRUCTURE_PROCESSORS.register("solid_foundation", () -> () -> SolidFoundationProcessor.CODEC);

    public static final Supplier<StructureType<AbandonedSpiritMineStructure>> ABANDONED_SPIRIT_MINE =
            STRUCTURE_TYPES.register("abandoned_spirit_mine",
                    () -> () -> AbandonedSpiritMineStructure.CODEC);

    public static final Supplier<StructureType<BridgeStructure>> BRIDGE =
            STRUCTURE_TYPES.register("bridge",
                    () -> () -> BridgeStructure.CODEC);

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
        STRUCTURE_PROCESSORS.register(eventBus);
        STRUCTURE_TYPES.register(eventBus);
        DENSITY_FUNCTION_TYPES.register(eventBus);
    }
}
