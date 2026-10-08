package com.example.immortal_cultivation_mod.pagoda;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;

@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class PagodaEvents {
    @SubscribeEvent public static void started(ServerStartedEvent event){
        for(var tower:PagodaData.get(event.getServer()).towers.values())if(tower.generated||!tower.prisoners.isEmpty())PagodaWorlds.getOrCreate(event.getServer(),tower);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){PagodaSystem.clear();}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){PagodaSystem.tick(event.getServer());for(var player:event.getServer().getPlayerList().getPlayers())GourdSystem.updateBuff(player);}
    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event){
        if(PagodaSystem.transferring()||PagodaCommandScope.admin())return;
        if(PagodaWorlds.isInterior(event.getEntity().level().dimension())
                &&!(event.getEntity() instanceof ServerPlayer p&&p.hasPermissions(2)))event.setCanceled(true);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(!(event.getEntity().level() instanceof ServerLevel level))return;
        if(event.getEntity() instanceof ServerPlayer owner){for(var state:PagodaData.get(level.getServer()).towers.values())if(state.owner.equals(owner.getUUID()))state.ownerDead=true;}
        var tower=PagodaSystem.interior(level.getServer(),level.dimension());
        if(tower!=null){GourdSystem.death(level.getServer(),tower,event.getEntity().getUUID());tower.prisoners.remove(event.getEntity().getUUID());PagodaData.get(level.getServer()).setDirty();}
        if(event.getEntity() instanceof PagodaEntity entity&&entity.towerId!=null&&!entity.core()){
            var state=PagodaSystem.towerFor(level.getServer(),entity.towerId);if(state!=null)state.closing=true;
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        for(var tower:PagodaData.get(player.server).towers.values())if(tower.closing&&tower.prisoners.containsKey(player.getUUID()))PagodaSystem.release(player.server,tower,tower.prisoners.get(player.getUUID()));
    }
    @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||PagodaSystem.transferring())return;
        var state=PagodaSystem.interior(player.server,event.getFrom());
        if(state==null||!state.prisoners.containsKey(player.getUUID()))return;
        if(PagodaCommandScope.admin()||player.hasPermissions(2)){state.prisoners.remove(player.getUUID());PagodaData.get(player.server).setDirty();return;}
        PagodaSystem.returnInside(player,state);
    }
    @SubscribeEvent public static void join(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof net.minecraft.world.entity.LivingEntity living)||living instanceof PagodaEntity)return;
        var tower=PagodaSystem.interior(level.getServer(),level.dimension());
        if(tower==null||tower.gourd||living.getUUID().equals(tower.owner))return;
        int floor=PagodaFloors.floor(living.getY());
        // Saved entities must always reload safely; only new joins are capped.
        if(event.loadedFromDisk())return;
        var room=new net.minecraft.world.phys.AABB(-10,64+floor*9,-10,10,73+floor*9,10);
        int count=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,room,
                e->e.isAlive()&&!(e instanceof PagodaEntity)&&!e.getUUID().equals(tower.owner)&&!e.getUUID().equals(living.getUUID())).size();
        if(floor>=tower.unlockedFloors||count>=10)event.setCanceled(true);
    }
    @SubscribeEvent public static void grief(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent event){
        if(PagodaWorlds.isInterior(event.getEntity().level().dimension()))event.setCanGrief(false);
    }
    @SubscribeEvent public static void explosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event){
        if(PagodaWorlds.isInterior(event.getLevel().dimension()))event.getAffectedBlocks().clear();
    }
    @SubscribeEvent public static void seal(PlayerInteractEvent.LeftClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!PagodaWorlds.isInterior(player.level().dimension()))return;
        if(player.serverLevel().getBlockState(event.getPos()).is(Blocks.GOLD_BLOCK)){
            var core=player.serverLevel().getEntitiesOfClass(PagodaEntity.class,player.getBoundingBox().inflate(12),PagodaEntity::core)
                    .stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if(core!=null&&player.getAttackStrengthScale(0)>=.9F){player.resetAttackStrengthTicker();core.hurt(player.damageSources().playerAttack(player),(float)PagodaSystem.attack(player));}
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event){
        if(!PagodaWorlds.isInterior(event.getLevel().dimension()))return;
        if(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BedBlock
                ||event.getItemStack().is(net.minecraft.world.item.Items.FLINT_AND_STEEL)
                ||event.getItemStack().is(net.minecraft.world.item.Items.FIRE_CHARGE)
                ||event.getLevel().getBlockState(event.getPos()).is(Blocks.RESPAWN_ANCHOR))event.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event){if(PagodaWorlds.isInterior(event.getPlayer().level().dimension())&&!event.getPlayer().hasPermissions(2))event.setCanceled(true);}
    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent event){if(event.getLevel() instanceof ServerLevel level&&PagodaWorlds.isInterior(level.dimension())&&!(event.getEntity() instanceof ServerPlayer p&&p.hasPermissions(2)))event.setCanceled(true);}
}
