package com.example.immortal_cultivation_mod.magicritual.datagen;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
   public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
      super(output, "immortal_cultivation_mod", existingFileHelper);
   }

   protected void registerStatesAndModels() {
      this.simpleBlockWithItem((Block)ModBlocks.SPIRIT_ORE.get(), this.models().cubeAll("spirit_ore", this.modLoc("block/spirit_ore")));
      ModelFile attackModel = this.makeCircleModel(CircleType.ATTACK, this.modLoc("block/magic_circle_attack"));
      ModelFile defenseModel = this.makeCircleModel(CircleType.DEFENSE, this.modLoc("block/magic_circle_defense"));
      ModelFile regenModel = this.makeCircleModel(CircleType.REGENERATION, this.modLoc("block/magic_circle_regeneration"));
      ((VariantBlockStateBuilder)((VariantBlockStateBuilder)this.getVariantBuilder((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get()).partialState().with(MagicCircleBlock.CIRCLE_TYPE, CircleType.ATTACK).modelForState().modelFile(attackModel).addModel()).partialState().with(MagicCircleBlock.CIRCLE_TYPE, CircleType.DEFENSE).modelForState().modelFile(defenseModel).addModel()).partialState().with(MagicCircleBlock.CIRCLE_TYPE, CircleType.REGENERATION).modelForState().modelFile(regenModel).addModel();
      this.simpleBlockItem((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get(), attackModel);
   }

   private ModelFile makeCircleModel(CircleType type, ResourceLocation texture) {
      return this.models().singleTexture("block/magic_circle_block_" + type.getSerializedName(), this.mcLoc("block/carpet"), "wool", texture);
   }
}
