package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.pagoda.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Bounded inward wind and rune emissions; model/entity sync drives the large tower. */
public final class PagodaVisuals {
    public static void tick(PagodaEntity entity){
        var mc=Minecraft.getInstance();if(mc.player==null||entity.distanceToSqr(mc.player)>9216||entity.tickCount%2!=0)return;
        var l=entity.level();double time=entity.tickCount*.09;
        if(entity.core()){
            for(int i=0;i<4;i++){
                double angle=time+i*Math.PI/2;double x=entity.getX()+Math.cos(angle)*1.1,z=entity.getZ()+Math.sin(angle)*1.1;
                l.addParticle(PagodaVfx.GOLD,x,entity.getY()+3,z,0,-.12,0);
            }
            return;
        }
        double radius=5*entity.visualScale();
        for(int i=0;i<12;i++){
            double a=time+i*Math.PI/6;double r=radius*(.5+(i%3)*.2);
            Vec3 p=entity.position().add(Math.cos(a)*r,(i%3)*.8+.15,Math.sin(a)*r);
            Vec3 inward=entity.position().add(0,.3,0).subtract(p).normalize().scale(.16);
            l.addParticle(entity.breakout()>=75&&entity.tickCount%4==0?ParticleTypes.REVERSE_PORTAL:PagodaVfx.GOLD,p.x,p.y,p.z,inward.x,inward.y,inward.z);
            if(i%3==0)l.addParticle(ParticleTypes.CLOUD,p.x,entity.getY()+.1,p.z,inward.x,0,inward.z);
            if(entity.breakout()<75)l.addParticle(ParticleTypes.ENCHANT,p.x,entity.getY()+.25,p.z,inward.x,.03,inward.z);
        }
        if(entity.breakout()>=50){
            for(int i=0;i<5;i++){
                double a=time+i*1.25;l.addParticle(ParticleTypes.REVERSE_PORTAL,entity.getX()+Math.cos(a)*2,entity.getY()+i*1.5,entity.getZ()+Math.sin(a)*2,0,.015,0);
            }
        }
    }
    private PagodaVisuals(){}
}
