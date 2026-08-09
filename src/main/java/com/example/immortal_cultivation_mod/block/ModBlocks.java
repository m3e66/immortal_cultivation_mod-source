package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.GeoRockBlockItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ImmortalCultivationMod.MODID);

    private static final ResourceKey<ConfiguredFeature<?, ?>> LINGMU_TREE_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "lingmu"));
    private static final TreeGrower LINGMU_TREE_GROWER = new TreeGrower(
            ImmortalCultivationMod.MODID + ":lingmu",
            Optional.empty(), Optional.of(LINGMU_TREE_FEATURE), Optional.empty());

    public static final DeferredBlock<Block> QINGLING_GRASS = registerPlant("qingling_grass");
    public static final DeferredBlock<Block> CHIYAN_FLOWER = registerPlant("chiyan_flower");
    public static final DeferredBlock<Block> NINGLU_ZHI = registerPlant("ninglu_zhi");
    public static final DeferredBlock<Block> XUANBING_LOTUS = registerPlant("xuanbing_lotus");
    public static final DeferredBlock<Block> ZIYUN_VINE = registerPlant("ziyun_vine");
    public static final DeferredBlock<Block> SHIHUN_GRASS = registerPlant("shihun_grass");
    public static final DeferredBlock<Block> JIUYE_XIANLAN = registerPlant("jiuye_xianlan");
    public static final DeferredBlock<Block> LINGMU = registerBlock("lingmu",
            () -> new SaplingBlock(LINGMU_TREE_GROWER, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> LINGMU_LOG = registerBlock("lingmu_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));
    public static final DeferredBlock<Block> LINGMU_LEAVES = registerBlock("lingmu_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));
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

    public static final DeferredBlock<Block> SUOKONG_BARRIER = registerBlockOnly("suokong_barrier",
            () -> new SuokongBarrierBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .noLootTable()
                    .replaceable()));

    public static final DeferredBlock<Block> STAR_MARK = registerBlockOnly("star_mark",
            () -> new StarMarkBlock(BlockBehaviour.Properties.of()
                    .strength(0.2F)
                    .lightLevel(state -> 12)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .randomTicks()
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

    public static final DeferredBlock<Block> RESTRICTION_TABLE = registerBlock("restriction_table",
            () -> new RestrictionTableBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DUNWU_STELE = registerBlock("dunwu_stele",
            () -> new DunwuSteleBlock(BlockBehaviour.Properties.of()
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()), true);

    public static void registerVanillaInteractions() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        fire.setFlammable(LINGMU_LOG.get(), 5, 5);
        fire.setFlammable(LINGMU_LEAVES.get(), 30, 60);

        ComposterBlock.COMPOSTABLES.put(LINGMU.get(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(LINGMU_LEAVES.get(), 0.3F);
    }

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
