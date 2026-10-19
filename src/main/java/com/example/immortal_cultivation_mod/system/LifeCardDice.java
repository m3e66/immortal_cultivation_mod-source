package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** One server-generated pool per action, saved before the animation is revealed. */
public final class LifeCardDice {
    private LifeCardDice() {}
    public static int majorRealm(int level) {
        return CultivationLevels.getRealmIndex(CultivationLevels.allLevels().get(
                Mth.clamp(level,0,CultivationLevels.allLevels().size()-1)));
    }
    public static int[] pool(CompoundTag life,int level) {
        int major=majorRealm(level);
        CompoundTag config=life.getCompound("CardDiceRules").getCompound(Integer.toString(major));
        var counts=com.example.immortal_cultivation_mod.config.ModServerConfig.LIFE_CARD_DICE_COUNT.get();
        var sizes=com.example.immortal_cultivation_mod.config.ModServerConfig.LIFE_CARD_DICE_SIDES.get();
        int count=config.contains("Count")?Mth.clamp(config.getInt("Count"),1,12):counts.get(Math.min(major,counts.size()-1));
        int sides=config.contains("Sides")?Mth.clamp(config.getInt("Sides"),4,100):sizes.get(Math.min(major,sizes.size()-1));
        return new int[]{count,sides};
    }
    public static double variation(int total,int count,int sides) {
        return -.05+.10*Mth.clamp((total-count)/(double)Math.max(1,count*(sides-1)),0,1);
    }
    public static CompoundTag roll(CompoundTag life,CompoundTag actor,String purpose,RandomSource random) {
        int[] pool=pool(life,actor.getInt("Realm")),dice=new int[pool[0]];int total=0;
        for(int i=0;i<dice.length;i++){dice[i]=1+random.nextInt(pool[1]);total+=dice[i];}
        CompoundTag result=new CompoundTag();result.putUUID("Id",UUID.randomUUID());
        result.putString("Purpose",purpose);result.putInt("Sides",pool[1]);result.putIntArray("Dice",dice);
        result.putInt("Total",total);result.putDouble("Variation",variation(total,pool[0],pool[1]));
        result.putDouble("Factor",1+result.getDouble("Variation"));
        LifeSimulationDice.remember(life,result);return result;
    }
    public static boolean binary(CompoundTag roll,double successFraction) {
        int count=roll.getIntArray("Dice").length,sides=roll.getInt("Sides");
        int requirement=(int)Math.ceil(count+(1-Mth.clamp(successFraction,0,1))*count*(sides-1));
        roll.putInt("Requirement",requirement);roll.putBoolean("Success",roll.getInt("Total")>=requirement);
        return roll.getBoolean("Success");
    }
    public static int requirement(CompoundTag life,CompoundTag actor,double successFraction){int[] pool=pool(life,actor.getInt("Realm"));return (int)Math.ceil(pool[0]+(1-Mth.clamp(successFraction,0,1))*pool[0]*(pool[1]-1));}
}
