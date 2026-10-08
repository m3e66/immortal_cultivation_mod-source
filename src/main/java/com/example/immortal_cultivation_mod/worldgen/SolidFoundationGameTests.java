package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class SolidFoundationGameTests {
    private static StructureBlockInfo block(int x, int y, int z, net.minecraft.world.level.block.Block block) {
        return new StructureBlockInfo(new BlockPos(x,y,z),block.defaultBlockState(),null);
    }

    @GameTest(template="fate_test")
    public static void foundationFillsDownToTerrainAndLeavesRoomsAndDecorationsOpen(GameTestHelper h) {
        BlockPos base=h.absolutePos(new BlockPos(1,5,1));
        h.setBlock(1,1,1,Blocks.STONE);
        for(int y=2;y<5;y++)h.setBlock(1,y,1,Blocks.AIR);
        var original=List.of(block(0,0,0,Blocks.GRASS_BLOCK),block(0,1,0,Blocks.AIR),
                block(1,0,0,Blocks.SHORT_GRASS),block(2,4,0,Blocks.OAK_PLANKS));
        var settings=new StructurePlaceSettings().addProcessor(SolidFoundationProcessor.INSTANCE);
        var placed=StructureTemplate.processBlockInfos(h.getLevel(),base,base,settings,original,null);
        h.assertTrue(placed.size()==original.size()+3,"Exactly the three empty blocks below the floor receive support");
        for(int y=2;y<5;y++) {
            BlockPos p=h.absolutePos(new BlockPos(1,y,1));
            h.assertTrue(placed.stream().anyMatch(b->b.pos().equals(p)&&b.state().is(Blocks.DIRT)),"Grass foundations extend as solid dirt");
        }
        h.assertTrue(placed.stream().anyMatch(b->b.pos().equals(base.above())&&b.state().isAir()),"Room air is preserved");
        h.assertTrue(placed.stream().filter(b->b.pos().getX()!=base.getX()).count()==2,"Plants and roof overhangs create no support columns");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void bridgeBanksAreSupportedAndTheWaterPassageSurvivesEveryTransform(GameTestHelper h) {
        for(Rotation rotation:Rotation.values())for(Mirror mirror:Mirror.values()) {
            BlockPos origin=h.absolutePos(new BlockPos(3,5,3));
            var settings=new StructurePlaceSettings().setRotation(rotation).setMirror(mirror)
                    .addProcessor(new SolidFoundationProcessor(1,1,2));
            var original=List.of(block(0,0,0,Blocks.STONE),block(0,1,1,Blocks.STONE),block(0,1,2,Blocks.STONE));
            var terrain=new ArrayList<BlockPos>();
            for(var local:original) {
                BlockPos world=StructureTemplate.calculateRelativePosition(settings,local.pos()).offset(origin);
                for(int depth=1;depth<=3;depth++)h.getLevel().setBlock(world.below(depth),Blocks.AIR.defaultBlockState(),2);
                h.getLevel().setBlock(world.below(4),Blocks.STONE.defaultBlockState(),2);terrain.add(world);
            }
            var placed=StructureTemplate.processBlockInfos(h.getLevel(),origin,origin,settings,original,null);
            h.assertTrue(placed.size()==9,"Only two land-bank columns are extended, through rotation and mirroring");
            for(int depth=1;depth<=3;depth++) {
                BlockPos open=terrain.get(1).below(depth);
                h.assertTrue(placed.stream().noneMatch(b->b.pos().equals(open)),"Bridge water span is never filled");
            }
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void foundationRespectsChunkBoundsAndExistingContainers(GameTestHelper h) {
        BlockPos base=h.absolutePos(new BlockPos(1,5,1));
        h.getLevel().setBlock(base.below(),Blocks.AIR.defaultBlockState(),2);
        h.getLevel().setBlock(base.below(2),Blocks.CHEST.defaultBlockState(),2);
        var box=new BoundingBox(base.getX(),h.getLevel().getMinBuildHeight(),base.getZ(),base.getX(),base.getY()+4,base.getZ());
        var settings=new StructurePlaceSettings().setBoundingBox(box).addProcessor(SolidFoundationProcessor.INSTANCE);
        var original=List.of(block(0,0,0,Blocks.STONE_BRICKS),block(1,0,0,Blocks.STONE_BRICKS));
        var placed=StructureTemplate.processBlockInfos(h.getLevel(),base,base,settings,original,null);
        h.assertTrue(placed.size()==3,"Only the in-chunk foundation column is read and extended");
        h.assertTrue(placed.getLast().pos().equals(base.below()),"Filling stops before an existing container");
        h.assertTrue(h.getLevel().getBlockState(base.below(2)).is(Blocks.CHEST),"The existing chest is preserved");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void allTemplateProcessorListsIncludeSolidFoundations(GameTestHelper h) {
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        for(String id:List.of("bridge1_ignore_air","cave_railing","daoist_temple_ignore_air","dead_ruin1_clear_terrain",
                "hotel1_clear_terrain","house1_clear_terrain","small_sect1_ignore_air","small_shrine_clear_terrain",
                "tomb1_ignore_air","tomb2_preserve_ground")) {
            var processors=registry.get(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,id));
            h.assertTrue(processors!=null&&processors.list().getLast() instanceof SolidFoundationProcessor,
                    "Every structure family finishes placement with solid foundations: "+id);
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void realTombTemplatePlacesSolidFoundationsUnderSlabsAndStairs(GameTestHelper h) {
        var id=ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"tomb1");
        var template=h.getLevel().getStructureManager().getOrCreate(id);
        var settings=new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90);
        var processors=h.getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                .get(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"tomb1_ignore_air"));
        processors.list().forEach(settings::addProcessor);
        BlockPos origin=h.absolutePos(new BlockPos(5,6,1));
        var nbt=template.save(new net.minecraft.nbt.CompoundTag());
        var blocks=nbt.getList("blocks",10);
        var palette=nbt.getList("palette",10);
        var bases=new ArrayList<BlockPos>();
        for(int i=0;i<blocks.size();i++) {
            var b=blocks.getCompound(i);var coordinates=b.getList("pos",3);
            if(coordinates.getInt(1)!=0)continue;
            String name=palette.getCompound(b.getInt("state")).getString("Name");
            if(name.equals("minecraft:air"))continue;
            var local=new BlockPos(coordinates.getInt(0),0,coordinates.getInt(2));
            BlockPos base=StructureTemplate.calculateRelativePosition(settings,local).offset(origin);
            bases.add(base);
            for(int d=0;d<5;d++)h.getLevel().setBlock(base.below(d),Blocks.AIR.defaultBlockState(),2);
            h.getLevel().setBlock(base.below(5),Blocks.STONE.defaultBlockState(),2);
        }
        h.assertTrue(bases.size()>20,"The actual tomb includes its slab and stair foundation cells");
        h.assertTrue(template.placeInWorld(h.getLevel(),origin,origin,settings,h.getLevel().random,2),"Real template placement succeeds");
        for(BlockPos base:bases)for(int d=1;d<5;d++) {
            BlockPos below=base.below(d);
            h.assertTrue(h.getLevel().getBlockState(below).isCollisionShapeFullBlock(h.getLevel(),below),
                    "Actual floor "+h.getLevel().getBlockState(base)+" needs continuous support at "+below);
        }
        h.succeed();
    }
}
