package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Finite scroll stock, saved with the merchant and replenished at the next in-game dawn. */
public final class SpellMerchantStock {
    public static final int OFFER_COUNT=12;
    private static final int VERSION=1;
    public static final String SKIN="violet_robed_cultivator";
    private record Scroll(Item item,int realm) { }
    private static List<Scroll> scrolls;
    private SpellMerchantStock() { }

    static void ensure(SpellMerchantEntity mob,CompoundTag profile) {
        int realm=CultivationLevels.getRealmIndex(profile.getString("Realm"));
        long day=Math.floorDiv(mob.level().getDayTime(),24_000L);
        if(profile.getInt("SpellMerchantStockVersion")==VERSION&&profile.getInt("SpellMerchantStockRealm")==realm
                &&profile.getLong("SpellMerchantStockDay")==day)return;
        var inventory=NpcProfile.inventory(mob);int first=inventory.size()-OFFER_COUNT;
        for(int slot=first;slot<inventory.size();slot++) {
            var existing=inventory.get(slot);
            if(!existing.isEmpty()&&!isSpell(existing)) {
                var retained=existing.copy();
                if(!NpcProfile.insertInventory(inventory.subList(0,first),retained))mob.spawnAtLocation(retained);
            }
        }
        var selected=select(realm,mob.getUUID().getMostSignificantBits()^mob.getUUID().getLeastSignificantBits()^day*31L^realm*1009L);
        for(int i=0;i<OFFER_COUNT;i++)inventory.set(first+i,selected.get(i));
        NpcProfile.writeInventory(mob,profile,inventory);
        profile.putInt("SpellMerchantStockVersion",VERSION);profile.putInt("SpellMerchantStockRealm",realm);
        profile.putLong("SpellMerchantStockDay",day);profile.putString("Skin",SKIN);profile.putString("Gender",NpcSkinCatalog.MALE);
    }

    public static boolean isSpell(ItemStack stack) {
        return !stack.isEmpty()&&stack.getItem() instanceof ModItems.SpellScrollItem scroll&&ModSpells.get(scroll.spellId())!=null;
    }
    public static int grade(ItemStack stack) {
        if(!isSpell(stack))return Integer.MAX_VALUE;
        var spell=ModSpells.get(((ModItems.SpellScrollItem)stack.getItem()).spellId());
        return Math.max(1,CultivationLevels.getRealmIndex(spell.requiredLevel()));
    }
    static List<ItemStack> select(int realm,long seed) {
        if(scrolls==null) {
            Map<String,Scroll> unique=new LinkedHashMap<>();
            BuiltInRegistries.ITEM.stream().filter(item->isSpell(item.getDefaultInstance()))
                    .sorted(Comparator.comparing(item->BuiltInRegistries.ITEM.getKey(item).toString()))
                    .forEach(item->unique.putIfAbsent(((ModItems.SpellScrollItem)item).spellId(),new Scroll(item,grade(item.getDefaultInstance()))));
            scrolls=List.copyOf(unique.values());
        }
        var pool=new ArrayList<>(scrolls.stream().filter(scroll->scroll.realm<=Math.max(1,realm)).toList());
        if(pool.size()<OFFER_COUNT)throw new IllegalStateException("Not enough usable spell scrolls for merchant realm "+realm);
        int best=pool.stream().mapToInt(Scroll::realm).max().orElse(1);var random=new Random(seed);
        List<ItemStack> result=new ArrayList<>();
        while(result.size()<OFFER_COUNT) {
            int total=pool.stream().mapToInt(scroll->weight(scroll,best)).sum(),roll=random.nextInt(total),chosen=0;
            while((roll-=weight(pool.get(chosen),best))>=0)chosen++;
            result.add(new ItemStack(pool.remove(chosen).item));
        }
        return List.copyOf(result);
    }
    private static int weight(Scroll scroll,int best) {int gap=best-scroll.realm;return gap==0?32:gap==1?8:1;}
}
