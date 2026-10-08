package com.example.immortal_cultivation_mod.pagoda;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Entity UUIDs are authoritative. Never re-create a prisoner from a snapshot. */
public final class PagodaData extends SavedData {
    private static final Factory<PagodaData> FACTORY=new Factory<>(PagodaData::new,PagodaData::load);
    public final Map<UUID,Integer> gourdWater=new LinkedHashMap<>();
    public final Map<UUID,Tower> towers=new LinkedHashMap<>();
    public static final class Prisoner {
        public UUID id; public ResourceLocation origin; public BlockPos position;
        public boolean player,visitor; public int floor; public double progress; public long lastAttack; public long forcedSince=-1;
        public Prisoner(UUID id,ResourceLocation origin,BlockPos pos,boolean player){this.id=id;this.origin=origin;position=pos;this.player=player;}
        CompoundTag save(){var t=new CompoundTag();t.putUUID("id",id);t.putString("origin",origin.toString());t.putLong("pos",position.asLong());t.putBoolean("player",player);t.putBoolean("visitor",visitor);t.putInt("floor",floor);t.putDouble("progress",progress);t.putLong("attack",lastAttack);t.putLong("forcedSince",forcedSince);return t;}
        static Prisoner load(CompoundTag t){var p=new Prisoner(t.getUUID("id"),ResourceLocation.parse(t.getString("origin")),BlockPos.of(t.getLong("pos")),t.getBoolean("player"));p.visitor=t.getBoolean("visitor");p.floor=Math.clamp(t.getInt("floor"),0,8);p.progress=t.getDouble("progress");p.lastAttack=t.getLong("attack");p.forcedSince=t.contains("forcedSince")?t.getLong("forcedSince"):-1;return p;}
    }
    public static final class Tower {
        public UUID id,owner,entity,container;
        public boolean gourd;
        public int waterLevel=-1,unlockedFloors=1,layoutVersion;
        public String ownerRealm=com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_MORTAL;
        public ResourceLocation origin;
        public BlockPos position;
        public long age;
        public int lifetime;
        public double ownerPower,injectedDamage,health=500,maxHealth=500,pressure=1;
        public boolean closing,generated,ownerDead,enterRequested;
        public int lockedFloors=-1,inactiveTicks;
        public final Map<UUID,Prisoner> prisoners=new LinkedHashMap<>();
        public Tower(UUID id,UUID owner,ResourceLocation origin,BlockPos position,int lifetime){this.id=id;this.owner=owner;this.origin=origin;this.position=position;this.lifetime=lifetime;}
        public double progress(){return prisoners.values().stream().mapToDouble(p->p.progress).max().orElse(0);}
        CompoundTag save(){var t=new CompoundTag();t.putUUID("id",id);t.putUUID("owner",owner);t.putBoolean("gourd",gourd);t.putInt("floors",unlockedFloors);t.putInt("layout",layoutVersion);t.putInt("inactive",inactiveTicks);t.putString("realm",ownerRealm);if(container!=null)t.putUUID("container",container);if(entity!=null)t.putUUID("entity",entity);t.putString("origin",origin.toString());t.putLong("pos",position.asLong());t.putLong("age",age);t.putInt("lifetime",lifetime);t.putDouble("power",ownerPower);t.putDouble("qiDamage",injectedDamage);t.putDouble("health",health);t.putDouble("maxHealth",maxHealth);t.putDouble("pressure",pressure);t.putBoolean("closing",closing);t.putBoolean("enterRequested",enterRequested);t.putBoolean("ownerDead",ownerDead);t.putBoolean("generated",generated);var list=new ListTag();prisoners.values().forEach(p->list.add(p.save()));t.put("prisoners",list);return t;}
        static Tower load(CompoundTag t){var v=new Tower(t.getUUID("id"),t.getUUID("owner"),ResourceLocation.parse(t.getString("origin")),BlockPos.of(t.getLong("pos")),Math.max(1,t.getInt("lifetime")));v.gourd=t.getBoolean("gourd");v.unlockedFloors=Math.clamp(t.getInt("floors"),1,9);v.layoutVersion=t.getInt("layout");v.inactiveTicks=t.getInt("inactive");if(t.contains("realm"))v.ownerRealm=t.getString("realm");if(t.hasUUID("container"))v.container=t.getUUID("container");if(t.hasUUID("entity"))v.entity=t.getUUID("entity");v.age=t.getLong("age");v.ownerPower=t.getDouble("power");v.injectedDamage=t.getDouble("qiDamage");v.health=t.getDouble("health");v.maxHealth=Math.max(1,t.getDouble("maxHealth"));v.pressure=t.getDouble("pressure");v.closing=t.getBoolean("closing");v.enterRequested=t.getBoolean("enterRequested");v.ownerDead=t.getBoolean("ownerDead");v.generated=t.getBoolean("generated");for(var p:t.getList("prisoners",Tag.TAG_COMPOUND)){var prisoner=Prisoner.load((CompoundTag)p);v.prisoners.put(prisoner.id,prisoner);}return v;}
    }
    public static PagodaData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(FACTORY,"immortal_pagodas");}
    public static PagodaData load(CompoundTag tag,HolderLookup.Provider registries){var data=new PagodaData();for(var water:tag.getList("gourdWater",Tag.TAG_COMPOUND)){var t=(CompoundTag)water;if(t.hasUUID("id"))data.gourdWater.put(t.getUUID("id"),Math.clamp(t.getInt("water"),0,100));}for(var entry:tag.getList("towers",Tag.TAG_COMPOUND)){var tower=Tower.load((CompoundTag)entry);data.towers.put(tower.id,tower);}return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();towers.values().forEach(t->list.add(t.save()));tag.put("towers",list);var water=new ListTag();gourdWater.forEach((id,amount)->{var t=new CompoundTag();t.putUUID("id",id);t.putInt("water",amount);water.add(t);});tag.put("gourdWater",water);return tag;}
}
