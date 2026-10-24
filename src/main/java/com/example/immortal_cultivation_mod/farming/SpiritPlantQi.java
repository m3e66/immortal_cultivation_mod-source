package com.example.immortal_cultivation_mod.farming;

import com.example.immortal_cultivation_mod.config.ModServerConfig;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Bounded, short-lived ambient cache. Farmland contributes once, only to the plant rooted in it. */
public final class SpiritPlantQi {
    private record Sample(long tick,int value) {}
    private static final Map<Level,LinkedHashMap<Long,Sample>> CACHE=new WeakHashMap<>();
    private SpiritPlantQi() {}
    public static int amount(Level level,BlockPos pos) {
        var cache=CACHE.computeIfAbsent(level,l->new LinkedHashMap<>());
        long now=level.getGameTime(),key=pos.asLong();var sample=cache.get(key);
        if(sample==null||now<sample.tick||now-sample.tick>=100) {
            sample=new Sample(now,ServerEvents.ambientQiAt(level,pos));
            if(cache.size()>=4096)cache.remove(cache.keySet().iterator().next());
            cache.put(key,sample);
        }
        int soil=level.getBlockState(pos.below()).is(SpiritFarming.FARMLAND.get())?ModServerConfig.SPIRIT_FARMLAND_QI.get():0;
        return (int)Math.min(Integer.MAX_VALUE,(long)sample.value+soil);
    }
    public static double speed(int qi) {
        var thresholds=ModServerConfig.SPIRIT_PLANT_QI_THRESHOLDS.get();
        return qi<thresholds.get(0)?.5:qi<thresholds.get(1)?.75:qi<thresholds.get(2)?1:qi<thresholds.get(3)?1.5:2;
    }
}
