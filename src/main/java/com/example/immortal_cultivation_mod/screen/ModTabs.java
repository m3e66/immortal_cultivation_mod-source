package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
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
                        output.accept(ModItems.ENLIGHTENMENT_PILL.get());
                        output.accept(ModItems.BREAKTHROUGH_PILL.get());
                        output.accept(ModItems.SMALL_LEVEL_UP.get());
                        output.accept(ModItems.BIG_LEVEL_UP.get());
                        output.accept(ModItems.QI_POUCH.get());

                    }).build());
}
