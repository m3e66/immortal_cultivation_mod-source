package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.mixin.MinecraftServerAccessor;
import java.util.List;
import java.util.UUID;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class PagodaWorlds {
    private static final net.minecraft.server.level.TicketType<UUID> OUTSIDE_TICKET=net.minecraft.server.level.TicketType.create("immortal_pagoda",java.util.Comparator.comparing(UUID::toString));
    public static final ResourceKey<Level> TEMPLATE=ResourceKey.create(Registries.DIMENSION,id("pagoda_interior"));
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",path);}
    public static ResourceKey<Level> key(UUID id){return ResourceKey.create(Registries.DIMENSION,id("pagoda_instances/"+id));}
    public static boolean isInterior(ResourceKey<Level> key){return key.location().getNamespace().equals("immortal_cultivation_mod")&&key.location().getPath().startsWith("pagoda_instances/");}
    public static void holdOutside(ServerLevel level,PagodaData.Tower tower){level.getChunkSource().addRegionTicket(OUTSIDE_TICKET,new net.minecraft.world.level.ChunkPos(tower.position),2,tower.id);}
    public static void releaseOutside(ServerLevel level,PagodaData.Tower tower){level.getChunkSource().removeRegionTicket(OUTSIDE_TICKET,new net.minecraft.world.level.ChunkPos(tower.position),2,tower.id);}
    public static ServerLevel getOrCreate(MinecraftServer server,PagodaData.Tower tower) {
        var key=key(tower.id);var existing=server.getLevel(key);if(existing!=null){ensureLayout(existing,tower);return existing;}
        LevelStem stem=server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(TEMPLATE.location());
        // Existing saves retain their original dimension-stem registry. Decode the newly
        // installed template against current dynamic registries rather than requiring a new world.
        if(stem==null){
            var resource=server.getResourceManager().getResource(id("dimension/pagoda_interior.json"))
                    .orElseThrow(()->new IllegalStateException("Missing pagoda interior template"));
            try(var reader=resource.openAsReader()){
                stem=LevelStem.CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,server.registryAccess()),
                        com.google.gson.JsonParser.parseReader(reader)).getOrThrow();
            }catch(java.io.IOException ex){throw new IllegalStateException("Cannot read pagoda dimension template",ex);}
        }
        var accessor=(MinecraftServerAccessor)server;
        var level=new ServerLevel(server,accessor.immortalCultivation$getExecutor(),accessor.immortalCultivation$getStorageSource(),
                new DerivedLevelData(server.getWorldData(),server.getWorldData().overworldData()),key,stem,
                LoggerChunkProgressListener.createCompleted(),false,BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()),List.of(),false,server.overworld().getRandomSequences());
        level.getWorldBorder().setCenter(0,0);level.getWorldBorder().setSize(40);
        server.forgeGetWorldMap().put(key,level);server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)level.setChunkForced(x,z,true);
        ensureLayout(level,tower);
        return level;
    }
    private static void ensureLayout(ServerLevel level,PagodaData.Tower tower){
        if(!tower.generated||!tower.gourd&&tower.layoutVersion<2){
            if(tower.gourd)generateGourd(level);else {
                if(tower.generated&&tower.layoutVersion<2){
                    tower.unlockedFloors=Math.clamp(Math.max(tower.unlockedFloors,1+(int)Math.round(Math.log(Math.max(50,tower.ownerPower)/50)/Math.log(4))),1,9);
                    int i=0;for(var prisoner:tower.prisoners.values())if(!prisoner.visitor)prisoner.floor=Math.min(8,i++/10);
                }
                generate(level);
            }
            tower.generated=true;tower.layoutVersion=2;tower.lockedFloors=-1;PagodaData.get(level.getServer()).setDirty();
        }
    }
    public static void updateFloorLocks(ServerLevel level,PagodaData.Tower tower){
        if(tower.gourd||tower.lockedFloors==tower.unlockedFloors)return;
        for(int floor=0;floor<8;floor++){
            int y=73+floor*9,z=floor%2==0?-4:4;
            for(int h=0;h<3;h++)level.setBlock(new BlockPos(2,y+h,z),
                    (floor+1>=tower.unlockedFloors?Blocks.BARRIER:Blocks.AIR).defaultBlockState(),2);
        }
        tower.lockedFloors=tower.unlockedFloors;
    }
    private static void generate(ServerLevel l){
        for(int floor=0;floor<9;floor++){
            int y=64+floor*9;
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++){
                int radius=x*x+z*z;if(radius>81)continue;
                if(radius<65)for(int h=1;h<=8;h++)l.setBlock(new BlockPos(x,y+h,z),Blocks.AIR.defaultBlockState(),2);
                l.setBlock(new BlockPos(x,y,z),(radius>=65?Blocks.GOLD_BLOCK:Blocks.DARK_OAK_PLANKS).defaultBlockState(),2);
                l.setBlock(new BlockPos(x,145,z),Blocks.BARRIER.defaultBlockState(),2);
                if(radius>=65)for(int h=1;h<=8;h++){
                    boolean window=h>=3&&h<=5&&(Math.abs(x)<3||Math.abs(z)<3);
                    l.setBlock(new BlockPos(x,y+h,z),(window?Blocks.BARRIER:Blocks.DARK_OAK_LOG).defaultBlockState(),2);
                }
            }
            for(int x:new int[]{-5,5})for(int z:new int[]{-5,5}){
                for(int h=1;h<=8;h++)l.setBlock(new BlockPos(x,y+h,z),Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),2);
                l.setBlock(new BlockPos(x+1,y+6,z),Blocks.YELLOW_BANNER.defaultBlockState(),2);
                l.setBlock(new BlockPos(x,y+7,z+1),Blocks.SOUL_LANTERN.defaultBlockState(),2);
            }
            l.setBlock(new BlockPos(0,y,0),Blocks.GLOWSTONE.defaultBlockState(),2);
            for(int side:new int[]{-1,1})for(int k=-3;k<=3;k++){
                l.setBlock(new BlockPos(k,y+1,side*6),Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(),2);
                if(k%2==0)l.setBlock(new BlockPos(side*6,y+6,k),Blocks.YELLOW_BANNER.defaultBlockState(),2);
            }
            // Each chamber bears its cultivation-floor number in golden blocks.
            for(int k=0;k<=floor;k++)l.setBlock(new BlockPos(-4+k,y+1,7),Blocks.GOLD_BLOCK.defaultBlockState(),2);
        }
        // Staircase links circular floors; deeper floors apply stronger pressure.
        for(int floor=0;floor<8;floor++)for(int step=0;step<9;step++){
            int x=-6+step,z=floor%2==0?-4:4,y=64+floor*9+step;
            l.setBlock(new BlockPos(x,y,z),Blocks.DARK_OAK_STAIRS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.EAST),2);
            for(int h=1;h<=3;h++)l.setBlock(new BlockPos(x,y+h,z),Blocks.AIR.defaultBlockState(),2);
        }
    }
    public static int gourdRadius(int y){
        double lower=81-Math.pow((y-71)*1.05,2),upper=36-Math.pow((y-86)*.9,2);
        return Math.max(2,(int)Math.round(Math.sqrt(Math.max(0,Math.max(lower,upper)))));
    }
    private static void generateGourd(ServerLevel level){
        for(int y=64;y<=94;y++){int radius=gourdRadius(y);
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){
                int r=x*x+z*z;if(r>radius*radius)continue;
                boolean shell=r>Math.pow(radius-1,2)||y==64||y==94;
                level.setBlock(new BlockPos(x,y,z),(shell?(y%9==0?Blocks.GOLD_BLOCK:Blocks.SMOOTH_SANDSTONE):Blocks.AIR).defaultBlockState(),2);
            }
        }
        // Spiral staircase joins the upper bulb through the narrowed waist.
        for(int y=65;y<=92;y++){
            double angle=(y-65)*Math.PI/3;int radius=Math.max(1,gourdRadius(y)-2);
            int x=(int)Math.round(Math.cos(angle)*radius),z=(int)Math.round(Math.sin(angle)*radius);
            level.setBlock(new BlockPos(x,y,z),Blocks.CUT_SANDSTONE_SLAB.defaultBlockState(),2);
        }
        for(int y:new int[]{66,75,84,93})level.setBlock(new BlockPos(0,y,0),Blocks.SEA_LANTERN.defaultBlockState(),2);
    }
    public static void updateWater(ServerLevel level,PagodaData.Tower tower,int meter){
        int height=waterHeight(meter);if(height==tower.waterLevel)return;
        for(int y=65;y<=92;y++){int radius=gourdRadius(y);
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++){
                if(x*x+z*z>Math.pow(radius-1,2))continue;
                var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
                if(state.isAir()||state.is(Blocks.WATER))level.setBlock(pos,(y<=height?Blocks.WATER:Blocks.AIR).defaultBlockState(),2);
                else if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
                    level.setBlock(pos,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,y<=height),2);
            }
        }
        tower.waterLevel=height;
    }
    public static int waterHeight(int meter){return 65+Math.round(Math.clamp(meter,0,100)*27/100F);}
    /** Only retire a world after every survivor has left. Never follow links outside its UUID directory. */
    public static boolean retire(MinecraftServer server,PagodaData.Tower tower){
        var key=key(tower.id);var level=server.getLevel(key);
        if(level!=null){
            for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(x,z)))return false;
            if(!level.players().isEmpty())return false;
            for(var entity:level.getAllEntities())if(!entity.isRemoved()&&!(entity instanceof PagodaEntity))return false;
            try {
                NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
                server.forgeGetWorldMap().remove(key);server.markWorldsDirty();level.close();
            } catch(Exception ex){return false;}
        }
        Path path=((MinecraftServerAccessor)server).immortalCultivation$getStorageSource().getDimensionPath(key).toAbsolutePath().normalize();
        if(!path.getFileName().toString().equals(tower.id.toString())||!path.getParent().getFileName().toString().equals("pagoda_instances"))return false;
        try {
            if(Files.exists(path)){
                Path worldRoot=((MinecraftServerAccessor)server).immortalCultivation$getStorageSource()
                        .getLevelPath(net.minecraft.world.level.storage.LevelResource.ROOT).toRealPath();
                if(!path.toRealPath().startsWith(worldRoot))throw new IllegalStateException("Pagoda cleanup escapes save directory");
                try(var walk=Files.walk(path)){
                    var paths=walk.sorted(java.util.Comparator.reverseOrder()).toList();
                    for(var child:paths){if(!child.toAbsolutePath().normalize().startsWith(path))throw new IllegalStateException("Invalid pagoda cleanup path");Files.deleteIfExists(child);}
                }
            }
            return true;
        } catch(Exception ex){org.slf4j.LoggerFactory.getLogger(PagodaWorlds.class).warn("Pagoda cleanup deferred: {}",tower.id,ex);return false;}
    }
    private PagodaWorlds(){}
}
