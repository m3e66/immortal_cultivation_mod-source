package com.example.immortal_cultivation_mod.alchemy;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_alchemy")
@PrefixGameTestTemplate(false)
public final class AlchemyPlantGameTests {
    @GameTest(template="fate_test")
    public static void newHerbsPlaceHarvestAndRequireSoil(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"herb-test"));
        p.setGameMode(GameType.SURVIVAL);
        BlockPos soil=h.absolutePos(new BlockPos(2,1,2)),plant=soil.above();
        for(var entry:AlchemyContent.EXTRA_PLANTS.entrySet()) {
            var item=entry.getValue().get();var block=AlchemyContent.PLANT_BLOCKS.get(entry.getKey()).get();
            h.assertTrue(item instanceof BlockItem,"Herb uses its own placeable block");
            h.getLevel().setBlockAndUpdate(soil,Blocks.DIRT.defaultBlockState());
            h.getLevel().setBlockAndUpdate(plant,Blocks.AIR.defaultBlockState());
            var stack=new ItemStack(item,3);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(soil).add(0,.5,0),Direction.UP,soil,false));
            item.useOn(context);
            h.assertTrue(h.getLevel().getBlockState(plant).is(block),"Right-click places "+entry.getKey());
            h.assertTrue(stack.getCount()==2,"Placement consumes exactly one herb");
            var drops=Block.getDrops(block.defaultBlockState(),h.getLevel(),plant,null);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(item)&&drops.getFirst().getCount()==1,"Harvest returns exactly the same alchemy ingredient");
            h.getLevel().setBlockAndUpdate(soil,Blocks.AIR.defaultBlockState());
            h.assertTrue(h.getLevel().getBlockState(plant).isAir(),"Removing soil removes unsupported herb");
            h.getLevel().setBlockAndUpdate(soil,Blocks.STONE.defaultBlockState());
            item.useOn(context);
            h.assertTrue(h.getLevel().getBlockState(plant).isAir()&&stack.getCount()==2,"Invalid soil neither places nor consumes herb");
        }
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void herbsHaveLoadedNaturalFeaturesWithoutCraftingRecipes(GameTestHelper h) {
        for(String name:AlchemyContent.EXTRA_PLANTS.keySet()) {
            var id=ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",name);
            h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(id)!=null,"Configured patch loads: "+name);
            h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(id)!=null,"Placed patch loads: "+name);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isEmpty(),"Herb is collected from the world, not crafted: "+name);
            var tag=net.minecraft.tags.TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","has_feature/"+name));
            h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getTag(tag).isPresent(),"Natural habitat tag loads: "+name);
            var habitats=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getTag(tag).orElseThrow();
            h.assertTrue(habitats.size()>0,"Herb has at least one natural habitat");
            for(var biome:habitats)h.assertTrue(biome.value().getGenerationSettings().features().stream()
                    .anyMatch(set->set.stream().anyMatch(feature->feature.unwrapKey().map(key->key.location().equals(id)).orElse(false))),
                    "Herb patch is actually injected into its biome generation: "+name);
        }
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","jade_pill_bottle")).orElseThrow().value().getResultItem(h.getLevel().registryAccess()).is(AlchemyContent.BOTTLE_SLIP.get()),"Bottle recipe creates its reusable forge slip");
        h.succeed();
    }
}
