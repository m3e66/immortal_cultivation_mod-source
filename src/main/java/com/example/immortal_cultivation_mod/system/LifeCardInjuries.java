package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/** Absolute simulated HP and consecutive knockouts, independent of real player health. */
public final class LifeCardInjuries {
    private LifeCardInjuries() {}
    public static double maxHp(CompoundTag person) {
        int realm=Mth.clamp(person.getInt("Realm"),0,CultivationLevels.allLevels().size()-1);
        return Math.max(20,CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(realm)).maxHp());
    }
    public static double hp(CompoundTag person) {
        return person.contains("CombatHp")?Mth.clamp(person.getDouble("CombatHp"),0,maxHp(person))
                :maxHp(person)*Mth.clamp(person.contains("Health")?person.getInt("Health"):100,0,100)/100.;
    }
    public static void save(CompoundTag person,CompoundTag actor,int year) {
        double health=Mth.clamp(actor.getDouble("Hp"),0,actor.getDouble("MaxHp"));
        person.putDouble("CombatHp",health);person.putDouble("CombatQi",actor.getDouble("Qi"));
        person.putInt("RecoveryYear",year);
        person.put("CombatEffects",actor.getList("CardEffects",Tag.TAG_COMPOUND).copy());
        CompoundTag buffs=actor.getCompound("CardBuffs").copy();for(String key:buffs.getAllKeys()){CompoundTag effect=buffs.getCompound(key);effect.putInt("Remaining",Math.max(0,effect.getInt("Until")-actor.getInt("CurrentRound")));}
        person.put("CombatBuffs",buffs);
        person.putInt("Health",Math.max(person.getBoolean("CombatDead")?0:1,(int)Math.ceil(100*health/actor.getDouble("MaxHp"))));
        if(health>=actor.getDouble("MaxHp")-.000001)person.putInt("KnockoutStreak",0);
    }
    public static void knockout(CompoundTag person,int year) {
        person.putDouble("CombatHp",0);person.putInt("RecoveryYear",year);
        int streak=person.getInt("KnockoutStreak")+1;person.putInt("KnockoutStreak",streak);
        if(streak>=3){person.putBoolean("CombatDead",true);person.putBoolean("Alive",false);
            person.putBoolean("Available",false);person.putInt("Health",0);person.putInt("DeathYear",year);
            person.putString("DeathCause","third_knockout");}
        else person.putInt("Health",1);
    }
    public static void recover(CompoundTag person,int year) {
        if(!person.contains("CombatHp")||person.getBoolean("CombatDead")||!person.getBoolean("Alive"))return;
        int elapsed=Math.max(0,year-person.getInt("RecoveryYear"));
        if(elapsed==0)return;
        double ongoing=0;for(var tag:person.getList("CombatEffects",Tag.TAG_COMPOUND)){CompoundTag effect=(CompoundTag)tag;if(effect.getString("Kind").equals("Hp"))ongoing+=effect.getDouble("Amount")*effect.getInt("Remaining");}
        person.remove("CombatEffects");person.remove("CombatBuffs");
        double health=Math.min(maxHp(person),hp(person)+100.*elapsed+ongoing);
        person.putDouble("CombatHp",health);person.putInt("RecoveryYear",year);
        person.putInt("Health",Math.max(1,(int)Math.ceil(100*health/maxHp(person))));
        if(health>=maxHp(person))person.putInt("KnockoutStreak",0);
    }
    public static void advanceYear(CompoundTag life) {
        int year=life.getInt("Age");recover(life,year);
        for(var tag:life.getList("Relationships",Tag.TAG_COMPOUND))recover((CompoundTag)tag,year);
    }
}
