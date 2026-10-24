package com.example.immortal_cultivation_mod.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Client animate ticks only, globally limited to four motes per game tick even in a huge garden. */
public final class SpiritPlantParticles {
    private static java.lang.ref.WeakReference<Level> lastLevel=new java.lang.ref.WeakReference<>(null);
    private static long tick=-1;
    private static int emitted;
    private SpiritPlantParticles() {}
    public static void emit(SpiritPlantSpecies species,Level level,BlockPos pos,RandomSource random,boolean mature,boolean rich) {
        if(!level.isClientSide||random.nextInt(rich?50:mature?100:250)!=0)return;
        if(lastLevel.get()!=level||tick!=level.getGameTime()){lastLevel=new java.lang.ref.WeakReference<>(level);tick=level.getGameTime();emitted=0;}
        if(emitted>=4)return;emitted++;
        int color=species.color;
        if(species==SpiritPlantSpecies.HUANXIA)color=new int[]{0xF1A5DA,0xA8D8FA,0xEBDD9A}[random.nextInt(3)];
        level.addParticle(new DustParticleOptions(new Vector3f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F),.45F),
                pos.getX()+.25+random.nextDouble()*.5,pos.getY()+.5+random.nextDouble()*.4,pos.getZ()+.25+random.nextDouble()*.5,0,.015,0);
    }
}
