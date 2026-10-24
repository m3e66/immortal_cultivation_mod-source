package com.example.immortal_cultivation_mod.farming;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_farming")
@PrefixGameTestTemplate(false)
public final class SpiritFarmingGameTests {
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",s);}
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"gardener"));p.setGameMode(GameType.SURVIVAL);return p;}
    private static BlockPos bed(GameTestHelper h,int y){return h.absolutePos(new BlockPos(2,y,2));}
    private static BlockHitResult hit(BlockPos pos){return new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false);}
    private static int count(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private static List<ItemStack> drops(GameTestHelper h,BlockState state,BlockPos pos){return Block.getDrops(state,h.getLevel(),pos,null);}

    @GameTest(template="fate_test") public static void everySeedPlantsOnBothSoilsWithoutRealmOrWater(GameTestHelper h){
        var p=player(h);var level=h.getLevel();var soil=bed(h,120);var pos=soil.above();
        int qi=ModAttachments.getData(p).qi();
        for(var species:SpiritPlantSpecies.values())for(var base:List.of(Blocks.FARMLAND.defaultBlockState(),SpiritFarming.FARMLAND.get().defaultBlockState())){
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(soil,base);
            var item=SpiritFarming.SEEDS.get(species).get();var stack=new ItemStack(item,3);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            item.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(soil)));
            var crop=SpiritFarming.CROPS.get(species).get();
            h.assertTrue(level.getBlockState(pos).is(crop)&&SpiritCropBlock.progress(level.getBlockState(pos))==0,"Fresh "+species+" plants on either soil");
            h.assertTrue(stack.getCount()==2,"Exactly one seed consumed");
            base.randomTick(level,soil,RandomSource.create(12));
            h.assertTrue(level.getBlockState(soil).getBlock() instanceof FarmBlock,"Seeded dry farmland stays farmland");
            h.assertTrue(ModAttachments.getData(p).qi()==qi,"No personal Qi used");
        }
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(soil,Blocks.STONE.defaultBlockState());
        var stack=p.getMainHandItem();int before=stack.getCount();stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(soil)));
        h.assertTrue(level.getBlockState(pos).isAir()&&stack.getCount()==before,"Bad ground neither plants nor consumes seed");
        p.discard();h.succeed();
    }

    @GameTest(template="fate_test") public static void naturalHerbsGiveExistingIngredientsAndRaritySeeds(GameTestHelper h){
        var pos=bed(h,120);var fortune=new ItemStack(Items.DIAMOND_HOE);
        fortune.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);
        for(var species:SpiritPlantSpecies.values()){
            var wild=BuiltInRegistries.BLOCK.get(id(species.id)).defaultBlockState();int seeds=0,plainHerbs=0,enchanted=0;
            h.assertTrue(wild.getValue(WildSpiritPlantBlock.NATURAL),"Old untagged worldgen defaults to a natural mature herb");
            var legacy=NbtUtils.writeBlockState(wild);legacy.getCompound("Properties").remove("natural");
            h.assertTrue(NbtUtils.readBlockState(h.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK),legacy)
                    .getValue(WildSpiritPlantBlock.NATURAL),"Old saved wild plants without new property remain natural");
            for(int i=0;i<400;i++){
                var loot=drops(h,wild,pos);seeds+=count(loot,SpiritFarming.SEEDS.get(species).get());plainHerbs+=count(loot,species.herb());
                h.assertTrue(count(loot,species.herb())>=1,"Every natural harvest yields original alchemy herb");
                enchanted+=count(Block.getDrops(wild,h.getLevel(),pos,null,null,fortune),species.herb());
            }
            h.assertTrue(seeds>400*(species.seedChance-.09)&&seeds<400*(species.seedChance+.09),"Rarity seed probability for "+species+": "+seeds);
            h.assertTrue(enchanted>plainHerbs&&enchanted<plainHerbs+200,"Fortune provides modest herb improvement");
            var placed=wild.setValue(WildSpiritPlantBlock.NATURAL,false);
            for(int i=0;i<20;i++){
                var loot=Block.getDrops(placed,h.getLevel(),pos,null,null,fortune);
                h.assertTrue(loot.size()==1&&count(loot,species.herb())==1,"Placed ingredients never multiply or produce seeds, including Fortune");
            }
        }
        h.succeed();
    }

    @GameTest(template="fate_test") public static void immatureCropsNeverDropMatureHerbs(GameTestHelper h){
        var pos=bed(h,120);
        for(var species:SpiritPlantSpecies.values())for(int progress=0;progress<32;progress++){
            var state=SpiritCropBlock.withProgress(SpiritFarming.CROPS.get(species).get().defaultBlockState(),progress);
            var loot=drops(h,state,pos);
            h.assertTrue(loot.size()==1&&count(loot,SpiritFarming.SEEDS.get(species).get())==1,"Immature "+species+" returns only original seed");
        }
        h.succeed();
    }

    @GameTest(template="fate_test") public static void harvestResetsRootsAndRepeatedPacketsCannotDuplicate(GameTestHelper h){
        var level=h.getLevel();var p=player(h);var soil=bed(h,120);var pos=soil.above();
        level.setBlockAndUpdate(soil,SpiritFarming.FARMLAND.get().defaultBlockState());
        for(var species:SpiritPlantSpecies.values()){
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
            var crop=SpiritFarming.CROPS.get(species).get();var mature=crop.getStateForAge(4);
            level.setBlockAndUpdate(pos,mature);mature.useWithoutItem(level,p,hit(pos));
            int before=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).stream().mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(before>=1&&SpiritCropBlock.progress(level.getBlockState(pos))==0,"Mature harvest gives items and resets root");
            // Exercise both a fresh duplicate use and a stale captured mature-state request.
            level.getBlockState(pos).useWithoutItem(level,p,hit(pos));SpiritFarming.harvest(mature,level,pos,p,true);
            int after=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).stream().mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(after==before&&level.getBlockState(soil).is(SpiritFarming.FARMLAND.get()),"Repeated use has no loot and preserves farmland");
            h.assertTrue(!crop.isValidBonemealTarget(level,pos,level.getBlockState(pos)),"Bonemeal cannot bypass Qi-only growth");
            crop.performBonemeal(level,level.random,pos,level.getBlockState(pos));
            h.assertTrue(SpiritCropBlock.progress(level.getBlockState(pos))==0,"Direct bonemeal does not advance crop");
        }
        p.discard();h.succeed();
    }

    @GameTest(template="fate_test") public static void qiSourcesShareExistingValuesAndFarmlandCountsOnlyOnce(GameTestHelper h){
        var level=h.getLevel();var soil=bed(h,250);var pos=soil.above();
        level.setBlockAndUpdate(soil,SpiritFarming.FARMLAND.get().defaultBlockState());
        var vein=com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_VEIN_CENTER.get();
        level.setBlockAndUpdate(pos.offset(3,0,0),vein.defaultBlockState());level.setBlockAndUpdate(pos.offset(-3,0,0),vein.defaultBlockState());
        h.assertTrue(ServerEvents.ambientQiAt(level,pos)==50,"Existing ambient source exposes exactly two veins");
        h.assertTrue(SpiritPlantQi.amount(level,pos)==75,"Two veins plus one rooted farmland contribution");
        level.setBlockAndUpdate(soil.east(),SpiritFarming.FARMLAND.get().defaultBlockState());
        h.assertTrue(SpiritPlantQi.amount(level,pos)==75,"Adjacent farmland is not counted again");
        level.setBlockAndUpdate(soil,Blocks.FARMLAND.defaultBlockState());
        h.assertTrue(SpiritPlantQi.amount(level,pos)==50,"Removing spirit soil immediately removes its Qi");
        int[] amounts={0,9,10,24,25,49,50,74,75,10000};double[] expected={.5,.5,.75,.75,1,1,1.5,1.5,2,2};
        for(int i=0;i<amounts.length;i++)h.assertTrue(SpiritPlantQi.speed(amounts[i])==expected[i],"Qi threshold "+amounts[i]);
        h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=400) public static void realRandomTicksGrowInDarkDryBedsAndHighQiIsFaster(GameTestHelper h){
        var level=h.getLevel();var low=bed(h,120).above();var high=bed(h,250).above();
        // One test's private horizontal offset separates its high-Qi veins from the source-count test.
        high=high.offset(150,0,0);
        level.setBlockAndUpdate(low.below(),Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(high.below(),SpiritFarming.FARMLAND.get().defaultBlockState());
        var vein=com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_VEIN_CENTER.get();
        level.setBlockAndUpdate(high.east(3),vein.defaultBlockState());level.setBlockAndUpdate(high.west(3),vein.defaultBlockState());
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(low.offset(x,2,z),Blocks.STONE.defaultBlockState());
        for(var species:SpiritPlantSpecies.values()){
            var crop=SpiritFarming.CROPS.get(species).get();double lowTotal=0,highTotal=0;
            for(int trial=0;trial<12;trial++)for(int kind=0;kind<2;kind++){
                var pos=kind==0?low:high;level.setBlockAndUpdate(pos,crop.defaultBlockState());
                var random=RandomSource.create(1971+trial);int calls=0;
                while(!crop.isMaxAge(level.getBlockState(pos))&&calls<3000){crop.randomTick(level.getBlockState(pos),level,pos,random);calls++;}
                h.assertTrue(calls>1&&calls<3000,"Gradual growth succeeds for "+species);
                if(kind==0)lowTotal+=calls;else highTotal+=calls;
            }
            h.assertTrue(lowTotal>highTotal*2.8&&lowTotal<highTotal*5.5,"Very high Qi grows about four times faster than very low Qi: "+species);
            double expectedCalls=species.growthTicks()*2/(4096.0/3);
            h.assertTrue(lowTotal/12>expectedCalls*.65&&lowTotal/12<expectedCalls*1.35,"Expected species-specific duration holds: "+species);
        }
        h.succeed();
    }

    @GameTest(template="fate_test") public static void partialGrowthRoundTripsThroughWorldBlockStateCodec(GameTestHelper h){
        for(var species:SpiritPlantSpecies.values()){
            var crop=SpiritFarming.CROPS.get(species).get();var state=SpiritCropBlock.withProgress(crop.defaultBlockState(),19).setValue(SpiritCropBlock.RICH,true);
            var nbt=NbtUtils.writeBlockState(state);
            var restored=NbtUtils.readBlockState(h.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK),nbt);
            h.assertTrue(restored.equals(state)&&SpiritCropBlock.progress(restored)==19,"Full stage and substage survives world NBT: "+species);
            h.assertTrue(Block.stateById(Block.getId(restored)).equals(state),"Standard client block-state protocol round trip: "+species);
        }
        h.succeed();
    }

    @GameTest(template="fate_test") public static void oldAlchemyIngredientsAndWildGenerationRemainLoaded(GameTestHelper h){
        for(var species:SpiritPlantSpecies.values()){
            h.assertTrue(species.herb() instanceof BlockItem,"Harvest is the pre-existing placeable herb ingredient");
            h.assertTrue(com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.PLANTS.containsKey(species.id),"Original alchemy ingredient remains registered: "+species);
            h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(id(species.id))!=null,"Original worldgen feature remains loaded: "+species);
        }
        h.succeed();
    }
}
