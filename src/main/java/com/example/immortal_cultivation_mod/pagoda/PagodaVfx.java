package com.example.immortal_cultivation_mod.pagoda;

import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PagodaVfx {
    public static final DustParticleOptions GOLD=new DustParticleOptions(new Vector3f(.9F,.65F,.18F),1);
    public static void chain(ServerLevel level,Vec3 target,Vec3 center,boolean failed){
        if(level.getGameTime()%4!=0)return;
        for(int i=0;i<8;i++){
            double a=i*.85+level.getGameTime()*.1;Vec3 p=target.add(Math.cos(a)*.8,i*.18,Math.sin(a)*.8);
            level.sendParticles(failed?ParticleTypes.ENCHANT:GOLD,p.x,p.y,p.z,1,0,0,0,0);
        }
    }
    public static void ripple(ServerLevel l,Vec3 p){
        for(int i=0;i<24;i++){double a=i*Math.PI/12;Vec3 point=p.add(Math.cos(a),1,Math.sin(a));l.sendParticles(GOLD,point.x,point.y,point.z,1,0,0,0,0);}
        l.playSound(null,p.x,p.y,p.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.65F,.65F);
    }
    public static void pressure(ServerLevel l,Vec3 p,double strength){
        for(int i=0;i<6;i++)l.sendParticles(GOLD,p.x,p.y+3-i*.5,p.z,1,.15,0,.15,0);
    }
    public static void field(ServerLevel l,Vec3 p,double strength){
        if(strength<=0)return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(l,null,p.x,p.y,p.z,64,
                new com.example.immortal_cultivation_mod.network.SpellVfxPayloads.Visual(l.dimension().location(),
                        com.example.immortal_cultivation_mod.network.SpellVfxPayloads.Kind.PRESSURE,-1,p,Math.clamp((int)Math.ceil(strength),1,3),30));
    }
    public static void pulse(ServerLevel l,Vec3 p,float strength){
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(l,null,p.x,p.y,p.z,32,
                new com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Effect(l.dimension().location(),
                        com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Kind.SHIELD_BREAK,-1,p,
                        new Vec3(0,-1,0),strength,com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.SPATIAL,l.random.nextInt()));
    }
    public static void burst(ServerLevel l,Vec3 p,boolean broken){
        for(int i=0;i<48;i++){double a=i*Math.PI/24;Vec3 point=p.add(Math.cos(a)*4,.25,Math.sin(a)*4);l.sendParticles(GOLD,point.x,point.y,point.z,1,0,0,0,0);}
        l.playSound(null,p.x,p.y,p.z,broken?SoundEvents.GENERIC_EXPLODE.value():SoundEvents.BEACON_DEACTIVATE,SoundSource.PLAYERS,1,.6F);
        if(broken)pulse(l,p,.7F);
    }
    private PagodaVfx(){}
}
