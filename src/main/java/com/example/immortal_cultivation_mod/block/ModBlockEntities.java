package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoRockBlockEntity>> GEO_ROCK =
            BLOCK_ENTITIES.register("geo_rock", () -> BlockEntityType.Builder.of(
                    GeoRockBlockEntity::new,
                    ModBlocks.QIANNIAN_LINGMU_HEART.get(),
                    ModBlocks.JINGTIE_MOTHERSTONE.get(),
                    ModBlocks.YUNWEN_SILVER_CRYSTAL.get(),
                    ModBlocks.CHITONG_SUI.get(),
                    ModBlocks.HANPO_JADE.get(),
                    ModBlocks.MOXUAN_IRON.get(),
                    ModBlocks.BISUI_STONE.get(),
                    ModBlocks.STAR_DUST_STONE.get(),
                    ModBlocks.JINWEN_FRUIT.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DingBlockEntity>> DING =
            BLOCK_ENTITIES.register("ding", () -> BlockEntityType.Builder.of(
                    DingBlockEntity::new,
                    ModBlocks.CU_TIE_XIAODING.get(),
                    ModBlocks.QINGTONG_YINHUO_DING.get()
            ).build(null));
}
