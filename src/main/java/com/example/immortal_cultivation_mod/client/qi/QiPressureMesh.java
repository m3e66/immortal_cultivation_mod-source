package com.example.immortal_cultivation_mod.client.qi;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Pressure ribbons; method-domain traces clip to the authoritative compressed volume. */
public final class QiPressureMesh {
    private static com.example.immortal_cultivation_mod.physics.DomainVolume domainClip;
    public static void clipDomain(com.example.immortal_cultivation_mod.physics.DomainVolume volume){domainClip=volume;}
    private QiPressureMesh(){}
    public static void ribbon(VertexConsumer out,Matrix4f m,Vec3 a,Vec3 b,Vec3 camera,double width,int color,float alpha){
        if(alpha<=0)return;
        if(domainClip!=null){var clipped=domainClip.clip(a,b);if(clipped==null)return;a=clipped[0];b=clipped[1];}
        Vec3 side=b.subtract(a).cross(a.add(b).scale(.5).subtract(camera)).normalize().scale(width);
        if(side.lengthSqr()<1E-10)return;
        for(Vec3 v:new Vec3[]{a.subtract(side),a.add(side),b.add(side),b.subtract(side)}){
            Vec3 p=v.subtract(camera);
            out.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha*QiVfxQuality.opacity());
        }
    }
    public static void wave(VertexConsumer out,Matrix4f m,Vec3 center,Vec3 normal,Vec3 camera,double radius,float alpha,int color){
        Vec3 u=normal.cross(Math.abs(normal.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
        Vec3 v=normal.cross(u).normalize();
        for(int i=0;i<48;i++){
            double a=i*Math.PI*2/48,b=(i+1)*Math.PI*2/48;
            ribbon(out,m,center.add(u.scale(Math.cos(a)*radius)).add(v.scale(Math.sin(a)*radius)),
                center.add(u.scale(Math.cos(b)*radius)).add(v.scale(Math.sin(b)*radius)),camera,.025,color,alpha);
        }
    }
    public static void domain(VertexConsumer out,Matrix4f m,Vec3 center,Vec3 camera,double time,int color,float power){
        int count=QiVfxQuality.level()==0?5:10;
        for(int j=0;j<count;j++){
            double phase=(time+j*19)%130;
            if(phase>46)continue;
            float opacity=(float)(Math.sin(phase/46*Math.PI)*(.13+.18*power));
            double a=j*2.399+Math.floor((time+j*19)/130)*.61;
            for(int i=0;i<12;i++){
                double t=i/12.0,u=(i+1)/12.0;
                Vec3 p=boundary(center,a,t,j,time),q=boundary(center,a,u,j,time);
                ribbon(out,m,p,q,camera,.018,color,opacity);
            }
        }
    }
    private static Vec3 boundary(Vec3 center,double a,double t,int j,double time){
        double theta=a+(j%3==0?t*.42:Math.sin(t*9+j)*.018);
        double y=j%3==0?.8+Math.sin(t*Math.PI)*2:t*6.5+.2;
        return center.add(Math.cos(theta)*20,y+Math.sin(t*17+time*.06)*.16,Math.sin(theta)*20);
    }
}
