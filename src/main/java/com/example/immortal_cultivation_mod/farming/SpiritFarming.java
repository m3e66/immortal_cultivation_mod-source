package com.example.immortal_cultivation_mod.farming;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.*;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class SpiritFarming {
    public static final int FARMLAND_STONE_COST = 8;
    public static final EnumMap<SpiritPlantSpecies,DeferredBlock<SpiritCropBlock>> CROPS=new EnumMap<>(SpiritPlantSpecies.class);
    public static final EnumMap<SpiritPlantSpecies,DeferredItem<Item>> SEEDS=new EnumMap<>(SpiritPlantSpecies.class);
    public static final DeferredBlock<SpiritFarmlandBlock> FARMLAND=ModBlocks.BLOCKS.register("spirit_farmland",
            ()->new SpiritFarmlandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND).lightLevel(s->2)));
    public static final DeferredItem<BlockItem> FARMLAND_ITEM=ModItems.ITEMS.register("spirit_farmland",
            ()->new BlockItem(FARMLAND.get(),new Item.Properties()));
    static {
        for(var species:SpiritPlantSpecies.values()) {
            var crop=ModBlocks.BLOCKS.register(species.id+"_crop",()->new SpiritCropBlock(species,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).noOcclusion()));
            CROPS.put(species,crop);
            SEEDS.put(species,ModItems.ITEMS.register(species.id+"_seeds",
                    ()->new SpiritSeedItem(species,crop.get(),new Item.Properties())));
        }
    }
    private SpiritFarming() {}
    public static void init() {}

    @SubscribeEvent public static void enrich(PlayerInteractEvent.RightClickBlock event) {
        var level=event.getLevel(); var pos=event.getPos(); var player=event.getEntity();
        if(!level.getBlockState(pos).is(Blocks.FARMLAND)
                ||!event.getItemStack().is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get())
                ||(!player.getAbilities().instabuild&&event.getItemStack().getCount()<FARMLAND_STONE_COST)
                ||!player.mayBuild()||!level.mayInteract(player,pos))return;
        if(!level.isClientSide&&level.setBlock(pos,FARMLAND.get().defaultBlockState(),Block.UPDATE_ALL)) {
            if(!player.getAbilities().instabuild)event.getItemStack().shrink(FARMLAND_STONE_COST);
            level.playSound(null,pos,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,.65F,1.3F);
            level.gameEvent(player,GameEvent.BLOCK_CHANGE,pos);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    /** Update the block before spawning loot: repeat/offhand packets cannot harvest it twice. */
    public static InteractionResult harvest(BlockState state,Level level,BlockPos pos,Player player,boolean regrow) {
        if(!player.mayBuild()||!level.mayInteract(player,pos))return InteractionResult.PASS;
        if(level instanceof ServerLevel server) {
            if(!level.getBlockState(pos).equals(state))return InteractionResult.PASS;
            var drops=Block.getDrops(state,server,pos,null,player,player.getMainHandItem());
            BlockState next=regrow?state.getBlock().defaultBlockState():Blocks.AIR.defaultBlockState();
            if(!level.setBlock(pos,next,Block.UPDATE_ALL))return InteractionResult.PASS;
            // The original planted seed stays in the living root. Breaking instead returns that seed.
            if(regrow&&state.getBlock() instanceof SpiritCropBlock crop) {
                Item seed=SEEDS.get(crop.species).get();
                for(var stack:drops)if(stack.is(seed)){stack.shrink(1);break;}
            }
            drops.forEach(stack->Block.popResource(level,pos,stack));
            level.playSound(null,pos,SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,SoundSource.BLOCKS,.7F,1.1F);
            level.gameEvent(player,GameEvent.BLOCK_CHANGE,pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
