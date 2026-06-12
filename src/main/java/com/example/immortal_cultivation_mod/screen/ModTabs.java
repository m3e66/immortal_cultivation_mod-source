package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_TAB =
            CREATIVE_TABS.register("immortal_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.BREAKTHROUGH_PILL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.FIREBALL_SCROLL.get());
                        output.accept(ModItems.LINGBENG_SCROLL.get());
                        output.accept(ModItems.REGENERATION_SCROLL.get());
                        output.accept(ModItems.BEAM_SCROLL.get());
                        output.accept(ModItems.EARTH_ESCAPE_SCROLL.get());
                        output.accept(ModItems.CLEANSE_SCROLL.get());
                        output.accept(ModItems.QI_GATHERING_SCROLL.get());
                        output.accept(ModItems.IGNITE_FLARE_SCROLL.get());
                        output.accept(ModItems.SPIRIT_SIGHT_SCROLL.get());
                        output.accept(ModItems.ZHENSHAN_PALM_SCROLL.get());
                        output.accept(ModItems.LIGHT_BEAM_ATTACK_SCROLL.get());
                        output.accept(ModItems.DIELANG_SHIELD_SCROLL.get());
                        output.accept(ModItems.LINGZHI_BULLET_SCROLL.get());
                        output.accept(ModItems.WIND_BLADE_SCROLL.get());
                        output.accept(ModItems.WIND_STEP_SCROLL.get());
                        output.accept(ModItems.SMOKE_ART_SCROLL.get());
                        output.accept(ModItems.SLIDING_WATER_SCROLL.get());
                        output.accept(ModItems.ENLIGHTENMENT_PILL.get());
                        output.accept(ModItems.BREAKTHROUGH_PILL.get());
                        output.accept(ModItems.SMALL_LEVEL_UP.get());
                        output.accept(ModItems.BIG_LEVEL_UP.get());
                        output.accept(ModItems.QI_POUCH.get());
                        output.accept(ModItems.DEBUG_STAT_EDITOR.get());
                        output.accept(ModItems.FOG_REVEALING_MIRROR.get());
                        output.accept(ModItems.BLOOD.get());
                        output.accept(ModItems.BASIC_BREATHING_METHOD.get());
                        output.accept(ModItems.CLEAR_HEART_METHOD.get());
                        output.accept(ModItems.BLOOD_DEMON_JINDAN_METHOD.get());
                        output.accept(ModItems.BLOOD_DEMON_YUANYING_METHOD.get());
                        output.accept(ModItems.BLOOD_DEMON_HUASHEN_METHOD.get());
                        output.accept(ModItems.JIUHU.get());

                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_BLOCKS_TAB =
            CREATIVE_TABS.register("immortal_blocks_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".blocks"))
                    .withTabsBefore(IMMORTAL_TAB.getKey())
                    .icon(() -> ModBlocks.QINGLING_GRASS.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.QINGLING_GRASS.get());
                        output.accept(ModBlocks.CHIYAN_FLOWER.get());
                        output.accept(ModBlocks.NINGLU_ZHI.get());
                        output.accept(ModBlocks.XUANBING_LOTUS.get());
                        output.accept(ModBlocks.ZIYUN_VINE.get());
                        output.accept(ModBlocks.JINWEN_FRUIT.get());
                        output.accept(ModBlocks.SHIHUN_GRASS.get());
                        output.accept(ModBlocks.JIUYE_XIANLAN.get());
                        output.accept(ModBlocks.QIANNIAN_LINGMU_HEART.get());
                        output.accept(ModBlocks.FUGU_TAI.get());
                        output.accept(ModBlocks.HUANXIA_SI.get());
                        output.accept(ModBlocks.JINGTIE_MOTHERSTONE.get());
                        output.accept(ModBlocks.YUNWEN_SILVER_CRYSTAL.get());
                        output.accept(ModBlocks.CHITONG_SUI.get());
                        output.accept(ModBlocks.HANPO_JADE.get());
                        output.accept(ModBlocks.MOXUAN_IRON.get());
                        output.accept(ModBlocks.LIUJIN_SAND.get());
                        output.accept(ModBlocks.BISUI_STONE.get());
                        output.accept(ModItems.LINGJIAO_SCALE.get());
                        output.accept(ModBlocks.STAR_DUST_STONE.get());

                    }).build());
}
