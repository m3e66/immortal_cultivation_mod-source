package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.mojang.datafixers.types.Type;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES;
   public static final Supplier<BlockEntityType<MagicCircleBlockEntity>> MAGIC_CIRCLE_BE;
   public static final Supplier<BlockEntityType<MagicCenterBlockEntity>> MAGIC_CENTER_BE;
   public static final Supplier<BlockEntityType<SpiritVeinCenterBlockEntity>> SPIRIT_VEIN_CENTER_BE;
   public static final Supplier<BlockEntityType<ComboBlockEntity>> COMBO_BE;
   public static final Supplier<BlockEntityType<FogEscapeBlockEntity>> FOG_ESCAPE_BE;

   public static void register(IEventBus eventBus) {
      BLOCK_ENTITIES.register(eventBus);
   }

   static {
      BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "immortal_cultivation_mod");
      MAGIC_CIRCLE_BE = BLOCK_ENTITIES.register("magic_circle_be", () -> Builder.of(MagicCircleBlockEntity::new, new Block[]{(Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get()}).build((Type)null));
      MAGIC_CENTER_BE = BLOCK_ENTITIES.register("magic_center_be", () -> Builder.of(MagicCenterBlockEntity::new, new Block[]{(Block)ModBlocks.MAGIC_CENTER_BLOCK.get()}).build((Type)null));
      SPIRIT_VEIN_CENTER_BE = BLOCK_ENTITIES.register("spirit_vein_center_be", () -> Builder.of(SpiritVeinCenterBlockEntity::new, new Block[]{(Block)ModBlocks.SPIRIT_VEIN_CENTER.get()}).build((Type)null));
      COMBO_BE = BLOCK_ENTITIES.register("combo_be", () -> Builder.of(ComboBlockEntity::new, new Block[]{(Block)ModBlocks.COMBO_BLOCK.get()}).build((Type)null));
      FOG_ESCAPE_BE = BLOCK_ENTITIES.register("fog_escape_be", () -> Builder.of(FogEscapeBlockEntity::new, new Block[]{(Block)ModBlocks.FOG_ESCAPE_BLOCK.get()}).build((Type)null));
   }
}
