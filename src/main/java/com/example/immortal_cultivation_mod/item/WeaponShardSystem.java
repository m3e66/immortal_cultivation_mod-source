package com.example.immortal_cultivation_mod.item;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class WeaponShardSystem {
    public static final String TAG="immortal_cultivation_mod.weapon_shard";
    public static final String REFORGE="weapon_shard_reforge";
    private WeaponShardSystem(){}
    public static CompoundTag data(ItemStack stack){
        return stack.getItem() instanceof WeaponShardItem?stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(TAG):new CompoundTag();
    }
    public static boolean populated(ItemStack stack){
        var d=data(stack);long layers=0;
        for(var type:WeaponRestrictionSystem.RestrictionType.values()){
            int amount=d.getCompound("layers").getInt(type.id());if(amount<0||amount>72)return false;layers+=amount;
        }
        return d.hasUUID("source")&&d.getInt("parts")>=1&&d.getInt("parts")<=WeaponRestrictionSystem.MAX_TIER&&d.getInt("index")>=0&&d.getInt("index")<d.getInt("parts")&&d.getInt("layer_capacity")==72&&layers<=72&&d.contains("weapon");
    }
    public static List<ItemStack> split(ItemStack weapon,HolderLookup.Provider registries){
        int rank=WeaponRestrictionSystem.tier(weapon);
        if(!WeaponRestrictionSystem.isWeapon(weapon)||rank<=0)return List.of();
        var original=weapon.copyWithCount(1);
        var clean=original.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        clean.remove(WeaponTribulation.TAG);original.set(DataComponents.CUSTOM_DATA,CustomData.of(clean));
        var retained=new CompoundTag();int totalCapacity=rank*72;
        for(var type:WeaponRestrictionSystem.RestrictionType.values()){
            int keep=Math.min(totalCapacity,WeaponRestrictionSystem.mergedCount(weapon,type));
            if(keep>0)retained.putInt(type.id(),keep);totalCapacity-=keep;
        }
        WeaponRestrictionSystem.retainShardLayers(original,retained);
        var saved=original.save(registries);
        UUID source=UUID.randomUUID();var result=new ArrayList<ItemStack>();
        var remaining=retained.copy();
        for(int i=0;i<rank;i++){
            var payload=new CompoundTag();payload.putUUID("source",source);payload.putInt("index",i);payload.putInt("parts",rank);payload.putInt("tier",rank);
            payload.putString("name",weapon.getItem().getDescriptionId());payload.putString("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString());
            payload.putString("display_name",weapon.getHoverName().getString());payload.putDouble("attack",QiInfusedWeapon.baseAttackDamage(original));payload.putInt("max_durability",original.getMaxDamage());
            payload.putBoolean("custom_name",weapon.has(DataComponents.CUSTOM_NAME));
            payload.put("weapon",saved.copy());
            var layers=new CompoundTag();int capacity=72;
            for(var type:WeaponRestrictionSystem.RestrictionType.values()){
                int count=Math.min(capacity,Math.max(0,remaining.getInt(type.id())));
                if(count>0){layers.putInt(type.id(),count);remaining.putInt(type.id(),remaining.getInt(type.id())-count);capacity-=count;}
            }
            payload.put("layers",layers);payload.putInt("layer_capacity",72);
            var root=new CompoundTag();root.put(TAG,payload);
            var shard=new ItemStack(ModItems.WEAPON_SHARD.get());shard.set(DataComponents.CUSTOM_DATA,CustomData.of(root));result.add(shard);
        }
        return result;
    }
    public static void dropBroken(ItemStack original,ServerLevel level,LivingEntity holder){
        if(holder==null)return;
        for(var shard:split(original,level.registryAccess())){
            var drop=new ItemEntity(level,holder.getX(),holder.getY()+.5,holder.getZ(),shard);drop.setDefaultPickUpDelay();level.addFreshEntity(drop);
        }
    }
    /** Complete sets require every unique index, equal snapshots, and 128 finite Qi stones. */
    public static boolean canReforge(List<ItemStack> inputs){
        CompoundTag first=null;var indexes=new HashSet<Integer>();int stones=0;
        for(var stack:inputs){
            if(stack.isEmpty())continue;
            if(ForgingSystem.isRecipeSpiritStone(stack)){stones+=stack.getCount();continue;}
            if(!populated(stack)||stack.getCount()!=1)return false;
            var d=data(stack);
            if(first==null)first=d;
            if(!first.getUUID("source").equals(d.getUUID("source"))||first.getInt("parts")!=d.getInt("parts")||!first.get("weapon").equals(d.get("weapon"))||!indexes.add(d.getInt("index")))return false;
        }
        return first!=null&&indexes.size()==first.getInt("parts")&&stones>=128;
    }
    public static ItemStack reforge(List<ItemStack> inputs,HolderLookup.Provider registries){
        if(!canReforge(inputs))return ItemStack.EMPTY;
        var shard=inputs.stream().filter(WeaponShardSystem::populated).findFirst().orElseThrow();
        var restored=ItemStack.parse(registries,data(shard).getCompound("weapon")).orElse(ItemStack.EMPTY);
        if(!WeaponRestrictionSystem.isWeapon(restored))return ItemStack.EMPTY;
        var retained=new CompoundTag();
        for(var input:inputs)if(populated(input)){
            var portion=data(input).getCompound("layers");
            for(var type:WeaponRestrictionSystem.RestrictionType.values()){
                int amount=portion.getInt(type.id());
                if(amount>0)retained.putInt(type.id(),retained.getInt(type.id())+amount);
            }
        }
        WeaponRestrictionSystem.retainShardLayers(restored,retained);
        restored.setCount(1);restored.setDamageValue(0);return restored;
    }
    public static boolean validAdditives(List<ItemStack> inputs){
        var seen=new HashSet<String>();int count=0;
        for(var stack:inputs)if(stack.getItem() instanceof WeaponShardItem){
            if(!populated(stack)||stack.getCount()!=1)return false;
            var d=data(stack);if(!seen.add(d.getUUID("source")+":"+d.getInt("index")))return false;
            if(++count>WeaponRestrictionSystem.MAX_TIER)return false;
        }
        return true;
    }
    public static void inherit(ItemStack result,List<ItemStack> inputs){
        if(result.isEmpty()||!validAdditives(inputs))return;
        int pieces=0;var layers=new CompoundTag();
        for(var shard:inputs)if(populated(shard)){
            pieces++;var portion=data(shard).getCompound("layers");
            for(var type:WeaponRestrictionSystem.RestrictionType.values())layers.putInt(type.id(),layers.getInt(type.id())+Math.clamp(portion.getInt(type.id()),0,72));
        }
        if(pieces>0)WeaponRestrictionSystem.inheritCompletedLayers(result,pieces,layers);
    }
}
