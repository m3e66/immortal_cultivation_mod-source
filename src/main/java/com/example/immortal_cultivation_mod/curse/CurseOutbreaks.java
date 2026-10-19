package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Breaking a seal leaves a saved source even if no player is currently nearby. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class CurseOutbreaks extends SavedData {
    public static final int MIN_TRAVEL_TICKS=80,MAX_TRAVEL_TICKS=400;
    private static final Factory<CurseOutbreaks> FACTORY=new Factory<>(CurseOutbreaks::new,CurseOutbreaks::load);
    private final ArrayList<CompoundTag> pending=new ArrayList<>();
    public static CurseOutbreaks get(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"immortal_annihilation_outbreaks");}
    public static void release(ServerLevel level,BlockPos origin) {
        var data=get(level);if(data.pending.size()>=64)return;
        var tag=new CompoundTag();tag.putLong("Origin",origin.asLong());data.pending.add(tag);data.setDirty();
    }
    private static CurseOutbreaks load(CompoundTag root,HolderLookup.Provider provider) {
        var data=new CurseOutbreaks();var entries=root.getList("Pending",10);
        for(int i=0;i<Math.min(entries.size(),64);i++)data.pending.add(entries.getCompound(i).copy());return data;
    }
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider provider) {
        var entries=new ListTag();for(var entry:pending)entries.add(entry.copy());root.put("Pending",entries);return root;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for(var player:event.getServer().getPlayerList().getPlayers())AnnihilationCurse.onlineTick(player);
        for(var level:event.getServer().getAllLevels())get(level).tick(level);
    }
    public void tick(ServerLevel level) {
        if(pending.isEmpty())return;
        var iterator=pending.iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var pos=BlockPos.of(entry.getLong("Origin"));
            ServerPlayer target=entry.hasUUID("Target")?level.getServer().getPlayerList().getPlayer(entry.getUUID("Target")):null;
            if(target==null||!target.isAlive()||target.isSpectator()||target.level()!=level) {
                target=level.players().stream().filter(p->p.isAlive()&&!p.isSpectator())
                        .min(Comparator.comparingDouble(p->p.distanceToSqr(pos.getCenter()))).orElse(null);
                if(target==null)continue;
                int duration=(int)Math.clamp(80+target.distanceToSqr(pos.getCenter())/50,MIN_TRAVEL_TICKS,MAX_TRAVEL_TICKS);
                entry.putUUID("Target",target.getUUID());entry.putInt("Remaining",duration);
                var packet=new CursePayloads.Outbreak(level.dimension().location(),pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5,
                        target.getId(),target.getUUID(),duration);
                for(var viewer:level.players())if(viewer==target||viewer.distanceToSqr(pos.getCenter())<128*128
                        ||viewer.distanceToSqr(target)<128*128)PacketDistributor.sendToPlayer(viewer,packet);
            }
            int remaining=entry.getInt("Remaining")-1;entry.putInt("Remaining",remaining);
            if(remaining<=0){AnnihilationCurse.acquire(target);iterator.remove();}
            setDirty();
        }
    }
}
