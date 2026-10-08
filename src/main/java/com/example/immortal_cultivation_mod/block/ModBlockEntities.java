package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.immortal_cultivation_mod.flame.SpiritFlameBlockEntity>>
            SPIRIT_FLAME = BLOCK_ENTITIES.register("spirit_flame", () -> BlockEntityType.Builder.of(
                    com.example.immortal_cultivation_mod.flame.SpiritFlameBlockEntity::new,
                    ModBlocks.LING_FIRE.get(),
                    ModBlocks.CRIMSON_FIRE.get(), ModBlocks.EARTH_FIRE.get(),
                    ModBlocks.WOOD_FIRE.get(), ModBlocks.SOLAR_FIRE.get(),
                    ModBlocks.NETHER_FIRE.get(), ModBlocks.THUNDER_FIRE.get(),
                    ModBlocks.DRAGON_FIRE.get(), ModBlocks.HEAVENLY_FIRE.get()).build(null));

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
                    ModBlocks.DUNWU_STELE.get(),
                    ModBlocks.LIANQI_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.ZHUJI_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.JINDAN_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.YUANYING_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.HUASHEN_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.LIANXU_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.HETI_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.DACHENG_SPELL_SCROLL_BLOCK.get(),
                    ModBlocks.DUJIE_SPELL_SCROLL_BLOCK.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DingBlockEntity>> DING =
            BLOCK_ENTITIES.register("ding", () -> BlockEntityType.Builder.of(
                    DingBlockEntity::new,
                    ModBlocks.CU_TIE_XIAODING.get(),
                    ModBlocks.QINGTONG_YINHUO_DING.get(),
                    ModBlocks.CHI_JIN_SANYANG_DING.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RestrictionTableBlockEntity>> RESTRICTION_TABLE =
            BLOCK_ENTITIES.register("restriction_table", () -> BlockEntityType.Builder.of(
                    RestrictionTableBlockEntity::new,
                    ModBlocks.RESTRICTION_TABLE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TalismanDrawingTableBlockEntity>>
            TALISMAN_DRAWING_TABLE = BLOCK_ENTITIES.register("talisman_drawing_table",
            () -> BlockEntityType.Builder.of(TalismanDrawingTableBlockEntity::new,
                    ModBlocks.TALISMAN_DRAWING_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MedicineCabinetBlockEntity>>
            MEDICINE_CABINET = BLOCK_ENTITIES.register("medicine_cabinet",
            () -> BlockEntityType.Builder.of(MedicineCabinetBlockEntity::new,
                    ModBlocks.MEDICINE_CABINET_SMALL.get(),
                    ModBlocks.MEDICINE_CABINET_LARGE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeadSkeletonBlockEntity>>
            DEAD_SKELETON = BLOCK_ENTITIES.register("dead_skeleton",
            () -> BlockEntityType.Builder.of(DeadSkeletonBlockEntity::new,
                    ModBlocks.DEAD_SKELETON.get()).build(null));

}
