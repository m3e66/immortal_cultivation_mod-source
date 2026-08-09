package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "immortal_cultivation_mod");
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAGIC_RITUAL_TAB = CREATIVE_TABS.register(
      "magic_ritual_tab",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.immortal_cultivation_mod.rituals"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.COMBAT})
            .icon(() -> ((Item)ModItems.RITUAL_DAGGER.get()).getDefaultInstance())
            .displayItems((itemDisplayParameters, output) -> {
               output.accept((ItemLike)ModItems.RITUAL_DAGGER.get());
               output.accept((ItemLike)ModItems.SPIRIT_STONE.get());
               output.accept((ItemLike)ModItems.MEDIUM_SPIRIT_STONE.get());
               output.accept((ItemLike)ModItems.HIGH_SPIRIT_STONE.get());
               output.accept((ItemLike)ModItems.INFINITE_SPIRIT_STONE.get());
               output.accept((ItemLike)ModBlocks.MAGIC_CENTER_BLOCK.get());
               output.accept((ItemLike)ModBlocks.MAGIC_CIRCLE_BLOCK.get());
               output.accept((ItemLike)ModBlocks.SPIRIT_ORE.get());
               output.accept((ItemLike)ModItems.SPIRIT_MAP.get());
               output.accept((ItemLike)ModItems.ZHENPAN.get());
               output.accept((ItemLike)ModItems.ZHENWEN_JADE.get());
               output.accept((ItemLike)ModItems.PATTERNED_CLOTH.get());
               output.accept((ItemLike)ModItems.SPIRIT_SILKWORM_SILK.get());
               output.accept((ItemLike)ModItems.SPIRIT_GUIDING_THREAD.get());
               output.accept((ItemLike)ModItems.SPIRIT_STORAGE_CRYSTAL.get());
               output.accept((ItemLike)ModItems.FLOWING_SPIRIT_CRYSTAL.get());
               output.accept((ItemLike)ModItems.LINGMU_FLAGPOLE.get());
               output.accept((ItemLike)ModItems.BLANK_FORMATION_FLAG.get());
               output.accept((ItemLike)ModItems.DURATION_FLAG.get());
               output.accept((ItemLike)ModItems.AMPLIFIER_FLAG.get());
               output.accept((ItemLike)ModItems.SPIRIT_GATHERING_FLAG.get());
               output.accept((ItemLike)ModItems.HASTE_FLAG.get());
               output.accept((ItemLike)ModItems.QI_RETURN_FLAG.get());
            })
            .build()
   );
}
