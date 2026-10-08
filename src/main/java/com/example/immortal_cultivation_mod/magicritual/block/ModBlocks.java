package com.example.immortal_cultivation_mod.magicritual.block;

import com.example.immortal_cultivation_mod.magicritual.block.custom.ComboBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.FogEscapeBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.SpiritVeinCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.item.MagicCenterBlockItem;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
   public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("immortal_cultivation_mod");
   public static final DeferredBlock<Block> MAGIC_CENTER_BLOCK = registerBlock("magic_center_block", () -> new MagicCenterBlock(Properties.of().mapColor(MapColor.NONE).strength(0.0F, 3600000.0F).sound(SoundType.STONE).noOcclusion().noTerrainParticles().noLootTable()));
   public static final DeferredBlock<Block> MAGIC_CIRCLE_BLOCK = registerBlock("magic_circle_block", () -> new MagicCircleBlock(Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1F).sound(SoundType.WOOL).noOcclusion().noTerrainParticles()));
   public static final DeferredBlock<Block> SPIRIT_ORE = registerBlock("spirit_ore", () -> new Block(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
   public static final DeferredBlock<Block> SPIRIT_VEIN_CENTER = registerBlock("spirit_vein_center", () -> new SpiritVeinCenterBlock(Properties.of().mapColor(MapColor.NONE).strength(0.5F).sound(SoundType.STONE).noOcclusion()));
   public static final DeferredBlock<Block> COMBO_BLOCK;
   public static final DeferredBlock<Block> FOG_ESCAPE_BLOCK;
   public static final DeferredItem<BlockItem> MAGIC_CENTER_BLOCK_ITEM;
   public static final DeferredItem<BlockItem> MAGIC_CIRCLE_BLOCK_ITEM;
   public static final DeferredItem<BlockItem> SPIRIT_ORE_ITEM;
   public static final DeferredItem<BlockItem> SPIRIT_VEIN_CENTER_ITEM;

   private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
      return BLOCKS.register(name, block);
   }

   static {
      COMBO_BLOCK = BLOCKS.register("combo_block", () -> new ComboBlock(Properties.of().mapColor(MapColor.NONE).strength(0.2F).sound(SoundType.STONE).noOcclusion().noLootTable()));
      FOG_ESCAPE_BLOCK = BLOCKS.register("fog_escape_block", () -> new FogEscapeBlock(Properties.of().mapColor(MapColor.NONE).strength(0.2F).sound(SoundType.STONE).noOcclusion().noLootTable()));
      MAGIC_CENTER_BLOCK_ITEM = ModItems.ITEMS.register("magic_center_block", () -> new MagicCenterBlockItem((Block)MAGIC_CENTER_BLOCK.get(), new Item.Properties()));
      MAGIC_CIRCLE_BLOCK_ITEM = ModItems.ITEMS.register("magic_circle_block", () -> new BlockItem((Block)MAGIC_CIRCLE_BLOCK.get(), new Item.Properties()));
      SPIRIT_ORE_ITEM = ModItems.ITEMS.register("spirit_ore", () -> new BlockItem((Block)SPIRIT_ORE.get(), new Item.Properties()));
      SPIRIT_VEIN_CENTER_ITEM = ModItems.ITEMS.register("spirit_vein_center", () -> new BlockItem((Block)SPIRIT_VEIN_CENTER.get(), new Item.Properties()));
   }
}
