package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.GeoRockBlockItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ImmortalCultivationMod.MODID);

    public static final DeferredBlock<Block> QINGLING_GRASS = registerPlant("qingling_grass");
    public static final DeferredBlock<Block> CHIYAN_FLOWER = registerPlant("chiyan_flower");
    public static final DeferredBlock<Block> NINGLU_ZHI = registerPlant("ninglu_zhi");
    public static final DeferredBlock<Block> XUANBING_LOTUS = registerPlant("xuanbing_lotus");
    public static final DeferredBlock<Block> ZIYUN_VINE = registerPlant("ziyun_vine");
    public static final DeferredBlock<Block> SHIHUN_GRASS = registerPlant("shihun_grass");
    public static final DeferredBlock<Block> JIUYE_XIANLAN = registerPlant("jiuye_xianlan");
    public static final DeferredBlock<Block> NUOMI_PLANT = registerBlock("nuomi_plant",
            () -> new GlutinousRiceCropBlock(BlockBehaviour.Properties.of()
                    .strength(0.0F)
                    .sound(SoundType.CROP)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()));

    public static final DeferredBlock<Block> NUOMI_DUST = registerBlockOnly("nuomi_dust",
            () -> new NuomiDustBlock(BlockBehaviour.Properties.of()
                    .strength(0.0F)
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()));

    public static final DeferredBlock<Block> MODOU_LINE = registerBlockOnly("modou_line",
            () -> new ModouLineBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()));

    public static final DeferredBlock<Block> LING_FIRE = registerBlockOnly("ling_fire",
            () -> new LingFireBlock(BlockBehaviour.Properties.of()
                    .strength(0.0F)
                    .lightLevel(state -> 12)
                    .sound(SoundType.WOOL)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()));

    public static final DeferredBlock<Block> FUGU_TAI = registerBlock("fugu_tai",
            () -> new CultivationVineBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .sound(SoundType.VINE)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()));

    public static final DeferredBlock<Block> HUANXIA_SI = registerBlock("huanxia_si",
            () -> new CultivationVineBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .sound(SoundType.VINE)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()));

    public static final DeferredBlock<Block> JINWEN_FRUIT = registerBlock("jinwen_fruit",
            () -> new GeoRockBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()),
            true);

    public static final DeferredBlock<Block> QIANNIAN_LINGMU_HEART =
            registerMaterial("qiannian_lingmu_heart", 2.0F, SoundType.WOOD);

    public static final DeferredBlock<Block> JINGTIE_MOTHERSTONE =
            registerOre("jingtie_motherstone", 3.0F);

    public static final DeferredBlock<Block> YUNWEN_SILVER_CRYSTAL =
            registerOre("yunwen_silver_crystal", 2.0F);

    public static final DeferredBlock<Block> CHITONG_SUI =
            registerOre("chitong_sui", 3.0F);

    public static final DeferredBlock<Block> HANPO_JADE =
            registerOre("hanpo_jade", 2.5F);

    public static final DeferredBlock<Block> MOXUAN_IRON =
            registerOre("moxuan_iron", 5.0F, false);

    public static final DeferredBlock<Block> LIUJIN_SAND = registerBlock("liujin_sand",
            () -> new FlowingGoldSandBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .sound(SoundType.SAND)
                    .noOcclusion()
                    .replaceable()));

    public static final DeferredBlock<Block> BISUI_STONE =
            registerOre("bisui_stone", 2.5F);

    public static final DeferredBlock<Block> STAR_DUST_STONE =
            registerOre("star_dust_stone", 2.0F);

    public static final DeferredBlock<Block> CU_TIE_XIAODING = registerBlock("cu_tie_xiaoding",
            () -> new DingBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F, 4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()), true);

    public static final DeferredBlock<Block> QINGTONG_YINHUO_DING = registerBlock("qingtong_yinhuo_ding",
            () -> new DingBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()), true);

    private static DeferredBlock<Block> registerPlant(String name) {
        return registerBlock(name, () -> new CultivationPlantBlock(BlockBehaviour.Properties.of()
                .strength(0.1F)
                .sound(SoundType.GRASS)
                .noCollission()
                .noOcclusion()));
    }

    private static DeferredBlock<Block> registerOre(String name, float strength) {
        return registerOre(name, strength, true);
    }

    private static DeferredBlock<Block> registerOre(String name, float strength, boolean geoInventoryModel) {
        return registerBlock(name, () -> new GeoRockBlock(BlockBehaviour.Properties.of()
                .strength(strength, strength + 2.0F)
                .sound(SoundType.STONE)
                .noOcclusion()
                .requiresCorrectToolForDrops()), geoInventoryModel);
    }

    private static DeferredBlock<Block> registerMaterial(String name, float strength, SoundType soundType) {
        return registerBlock(name, () -> new GeoRockBlock(BlockBehaviour.Properties.of()
                .strength(strength, strength + 2.0F)
                .noOcclusion()
                .sound(soundType)), true);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        return registerBlock(name, blockSupplier, false);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier, boolean geoInventoryModel) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> geoInventoryModel
                ? new GeoRockBlockItem(block.get(), new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static <T extends Block> DeferredBlock<T> registerBlockOnly(String name, Supplier<T> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }
}
