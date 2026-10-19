package com.example.immortal_cultivation_mod.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** All newly committed rolls in one action, excluding historical rolls in actor snapshots. */
public final class LifeDiceSequence {
    public record Pool(String kind,CompoundTag roll) {}
    private LifeDiceSequence() {}

    public static List<Pool> pools(CompoundTag frame) {
        List<Pool> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        add(result,seen,"main",frame.getCompound("Roll"));
        add(result,seen,"attack",frame.getCompound("Attack"));
        add(result,seen,"damage",frame.getCompound("DamageRoll"));
        for(var tag:frame.getList("Hits",Tag.TAG_COMPOUND)) {
            CompoundTag hit=(CompoundTag)tag;
            add(result,seen,"attack",hit.getCompound("Attack"));
            add(result,seen,"main",hit.getCompound("Roll"));
            add(result,seen,"damage",hit.getCompound("DamageRoll"));
            add(result,seen,"final_will",hit.getCompound("FinalWillRoll"));
        }
        add(result,seen,"final_will",frame.getCompound("FinalWillRoll"));
        return List.copyOf(result);
    }

    private static void add(List<Pool> result,Set<String> seen,String kind,CompoundTag roll) {
        if(roll.isEmpty()||roll.getInt("Sides")<1||roll.getIntArray("Dice").length==0&&!roll.contains("Selected"))return;
        // Shared roll copies are one physical throw; identical-valued independent rolls keep their UUIDs.
        if(!roll.hasUUID("Id")||seen.add(roll.getUUID("Id").toString()))result.add(new Pool(kind,roll));
    }

    public static long rollMillis(int mode) { return mode==0?1600:650; }
    /** Keep each settled battle throw visible for one extra second in every speed mode. */
    public static long poolMillis(int mode) { return rollMillis(mode)+(mode==0?200:50)+1000; }
    public static long sequenceMillis(List<Pool> pools,int mode) { return poolMillis(mode)*Math.max(1,pools.size()); }
    public static int index(List<Pool> pools,long elapsed,int mode) {
        return Math.min(Math.max(0,pools.size()-1),(int)Math.max(0,elapsed/poolMillis(mode)));
    }
}
