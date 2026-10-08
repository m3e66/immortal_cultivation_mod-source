package com.example.immortal_cultivation_mod.hair;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.ModTabs;
import com.example.immortal_cultivation_mod.screen.ModScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.*;

@EventBusSubscriber(modid="immortal_cultivation_mod", bus=EventBusSubscriber.Bus.MOD)
public final class HairContent {
    public static final List<DeferredItem<HairItem>> HAIR = Arrays.stream(HairStyle.values())
            .map(s -> ModItems.ITEMS.register(s.id(), () -> new HairItem(s.ordinal(), false))).toList();
    public static final List<String> ACCESSORY_IDS = List.of("cultivation_hairpin", "hair_ribbon", "hair_crown", "jade_hair_ornament", "decorative_hair_beads");
    public static final List<DeferredItem<HairItem>> ACCESSORIES = java.util.stream.IntStream.range(0, 5)
            .mapToObj(i -> ModItems.ITEMS.register(ACCESSORY_IDS.get(i), () -> new HairItem(i, true))).toList();
    public static final DeferredBlock<HairStylingTableBlock> TABLE = ModBlocks.BLOCKS.register("hair_styling_table",
            () -> new HairStylingTableBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ModItems.ITEMS.register("hair_styling_table",
            () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<MenuType<?>, MenuType<HairMenu>> MENU = ModScreens.MENUS.register("hair_styling",
            () -> new MenuType<>(HairMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static void init() { }
    @SubscribeEvent public static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModTabs.IMMORTAL_ACCESSORIES_TAB.get()) {
            HAIR.forEach(event::accept);
            ACCESSORIES.forEach(event::accept);
        }
        if (event.getTab() == ModTabs.IMMORTAL_BLOCKS_TAB.get()) {
            event.accept(TABLE_ITEM);
        }
    }
}
