package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Loaded registries, real climate sampling and real natural/sapling generation. */
@GameTestHolder("immortal_cultivation_spiritwood_biome")
@PrefixGameTestTemplate(false)
public final class SpiritWoodBiomeGameTests {
    private static final ResourceLocation TREE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "lingmu");
    private static final ResourceKey<MultiNoiseBiomeSourceParameterList> OVERWORLD = ResourceKey.create(
            Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld"));

    @GameTest(template="fate_test")
    public static void groveIsInPlayableOverworldSourceAndTreeFeatureIsExclusive(GameTestHelper h) {
        var access=h.getLevel().registryAccess();
        var registry=access.registryOrThrow(Registries.BIOME);
        var grove=registry.getHolderOrThrow(SpiritWoodBiomes.GROVE);
        var preset=access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(OVERWORLD);
        var source=MultiNoiseBiomeSource.createFromPreset(preset);
        h.assertTrue(source.possibleBiomes().contains(grove),"Playable Overworld actually includes the custom biome, not just its JSON registration");
        h.assertTrue(grove.is(SpiritWoodBiomes.NATURAL_TREES)&&hasTrees(grove),"Loaded grove has the spiritwood placed feature");
        for(var key:List.of(Biomes.PLAINS,Biomes.FOREST,Biomes.FLOWER_FOREST,Biomes.BIRCH_FOREST,Biomes.DARK_FOREST,
                Biomes.TAIGA,Biomes.OLD_GROWTH_PINE_TAIGA,Biomes.OLD_GROWTH_SPRUCE_TAIGA,Biomes.JUNGLE,Biomes.SPARSE_JUNGLE,Biomes.BAMBOO_JUNGLE)) {
            var biome=registry.getHolderOrThrow(key);
            h.assertTrue(!biome.is(SpiritWoodBiomes.NATURAL_TREES)&&!hasTrees(biome),"Ordinary biome has no natural spiritwood feature: "+key.location());
        }
        h.assertTrue(source.possibleBiomes().stream().filter(holder->holder.is(SpiritWoodBiomes.GROVE)).count()==1,"Grove is advertised once for feature ordering and locating");
        var nether=access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).get(
                ResourceLocation.withDefaultNamespace("nether"));
        h.assertTrue(nether.parameters().values().stream().noneMatch(e->e.getSecond().is(SpiritWoodBiomes.GROVE)),"Nether preset is unaffected");
        h.succeed();
    }

    private static boolean hasTrees(Holder<Biome> biome) {
        return biome.value().getGenerationSettings().features().stream().flatMap(set->set.stream()).anyMatch(feature->feature.is(TREE));
    }

    @GameTest(template="fate_test")
    public static void forestClimateCoverageProducesRareGrovesWithoutReplacingOtherRegions(GameTestHelper h) {
        var baseline=MultiNoiseBiomeSourceParameterList.knownPresets().get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);
        var preset=h.getLevel().registryAccess().registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(OVERWORLD);
        var patched=MultiNoiseBiomeSource.createFromPreset(preset);
        int groves=0,forests=0;RandomSource random=RandomSource.create(942716L);
        for(int i=0;i<6000;i++) {
            float weirdness=random.nextFloat()*2-1;
            var point=Climate.target(random.nextFloat()*2-1,random.nextFloat()*2-1,
                    random.nextFloat()*2-1,random.nextFloat()*2-1,0,weirdness);
            var before=baseline.findValue(point);var after=patched.getNoiseBiome(point).unwrapKey().orElseThrow();
            if(after.equals(SpiritWoodBiomes.GROVE)) {
                groves++;
                h.assertTrue(before.equals(Biomes.FOREST),"Grove replaces only a forest climate, never oceans, deserts or other regions");
                h.assertTrue(weirdness>=.0999F&&weirdness<=.2401F,"Grove uses a limited climate band");
            } else if(after.equals(Biomes.FOREST)) forests++;
            if(weirdness<.0999F||weirdness>.2401F) h.assertTrue(before.equals(after),"Outside the grove band, existing biome sampling stays the same");
        }
        h.assertTrue(groves>0&&forests>groves&&groves<300,"Groves are naturally reachable and less common than ordinary forests: groves="+groves+", forests="+forests);
        h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=200)
    public static void realOreVeinsPlantOnlyInsideGrovesIncludingAtBiomeBorders(GameTestHelper h) {
        ServerLevel level=h.getLevel();
        BlockPos center=h.absolutePos(new BlockPos(1000,0,1000));center=new BlockPos(center.getX(),64,center.getZ());
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var forest=registry.getHolderOrThrow(Biomes.FOREST);var grove=registry.getHolderOrThrow(SpiritWoodBiomes.GROVE);
        long seed=0;while(RandomSource.create(seed).nextInt(101)!=70)seed++;
        var feature=com.example.immortal_cultivation_mod.magicritual.worldgen.ModFeatures.SPIRIT_ORE_VEIN.get();
        for(int mode=0;mode<3;mode++) {
            fillBiome(level,center,forest,grove,mode);
            for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++) {
                level.setBlockAndUpdate(center.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState());
                for(int y=0;y<12;y++)level.setBlockAndUpdate(center.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)for(int y=-39;y<=-21;y++)
                level.setBlockAndUpdate(new BlockPos(center.getX()+x,y,center.getZ()+z),Blocks.STONE.defaultBlockState());
            h.assertTrue(feature.place(NoneFeatureConfiguration.INSTANCE,level,level.getChunkSource().getGenerator(),RandomSource.create(seed),center),"Real underground vein successfully places ore");
            int saplings=0;
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++) {
                BlockPos pos=center.offset(x,0,z);
                if(level.getBlockState(pos).is(ModBlocks.LINGMU.get())) {
                    saplings++;
                    h.assertTrue(level.getBiome(pos).is(SpiritWoodBiomes.GROVE),"Every vein-fed sapling stays in grove even where vein straddles biome border");
                }
            }
            h.assertTrue(mode==0?saplings==0:saplings>=1,"Vein sapling rule follows actual surface biome: mode="+mode+", saplings="+saplings);
        }
        h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=200)
    public static void plantedSaplingCanStillGrowOutsideNaturalHabitat(GameTestHelper h) {
        ServerLevel level=h.getLevel();BlockPos center=h.absolutePos(new BlockPos(-1000,0,1000));center=new BlockPos(center.getX(),64,center.getZ());
        var forest=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST);
        fillBiome(level,center,forest,forest,0);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) {
            level.setBlockAndUpdate(center.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState());
            for(int y=0;y<12;y++)level.setBlockAndUpdate(center.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var state=ModBlocks.LINGMU.get().defaultBlockState().setValue(SaplingBlock.STAGE,1);
        level.setBlockAndUpdate(center,state);
        ((SaplingBlock)ModBlocks.LINGMU.get()).advanceTree(level,center,state,RandomSource.create(7));
        h.assertTrue(level.getBlockState(center).is(ModBlocks.LINGMU_LOG.get()),"Player-grown saplings retain real tree growth outside grove");
        h.succeed();
    }

    private static void fillBiome(ServerLevel level,BlockPos center,Holder<Biome> forest,Holder<Biome> grove,int mode) {
        var sampler=level.getChunkSource().randomState().sampler();
        int boundary=center.getX()>>2;
        for(int x=(center.getX()-20)>>4;x<=(center.getX()+20)>>4;x++)for(int z=(center.getZ()-20)>>4;z<=(center.getZ()+20)>>4;z++) {
            var chunk=level.getChunk(x,z);
            chunk.fillBiomesFromNoise((qx,qy,qz,ignored)->mode==0?forest:mode==1?grove:qx<boundary?forest:grove,sampler);
            chunk.setUnsaved(true);
        }
    }
}
