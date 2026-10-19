package com.example.immortal_cultivation_mod.client.qi;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Opposing living currents, echoing strikes and open black-white wheels of rebirth. */
public final class ReincarnationVisuals {
    public static long auraFrames,domainFrames,impactFrames,procFrames;
    private static final int VIOLET=0xA976EF,JADE=0xC1F6D8,BLACK=0x1B1729,WHITE=0xF2F4EB;
    private static final double TAU=Math.PI*2;
    private ReincarnationVisuals() { }
    public static QiElementLook particle(boolean jade) {
        return new QiElementLook(jade?JADE:VIOLET,jade?0xECFFF2:0xE0C6FF,QiElementLook.WIND,.8F,5);
    }

    public static void aura(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,double time,float alpha) {
        var mc=Minecraft.getInstance();
        if(owner==mc.getCameraEntity()&&mc.options.getCameraType().isFirstPerson())return;
        auraFrames++;
        float partial=(float)(time-Math.floor(time));
        Vec3 center=p.add(QiAuraPose.bodyOffset(owner,partial));
        double height=owner.getDimensions(Pose.STANDING).height()*1.12;
        double radius=Math.max(.54,owner.getBbWidth()*.95);
        int steps=QiVfxQuality.level()==0?24:36;
        for(int current=0;current<2;current++) {
            int sign=current==0?1:-1,color=current==0?VIOLET:JADE;
            for(int i=0;i<steps;i++) {
                double t=i/(double)steps,u=(i+1.0)/steps;
                Vec3 a=deform(owner,center.add(currentPoint(t,time,sign,radius,height)),partial);
                Vec3 b=deform(owner,center.add(currentPoint(u,time,sign,radius,height)),partial);
                double width=.012+.036*Math.sin(t*Math.PI);
                float fade=alpha*(float)Math.pow(Math.sin(t*Math.PI),.7);
                QiPressureMesh.ribbon(out,m,a,b,camera,width,color,fade*.35F);
                QiPressureMesh.ribbon(out,m,a,b,camera,width*.22,current==0?0xDEC4FF:0xEBFFF0,fade*.48F);
            }
            for(int j=0;j<3;j++) {
                double t=((time*.011+j/3.0)%1+1)%1;
                Vec3 a=deform(owner,center.add(currentPoint(t,time,sign,radius,height)),partial);
                Vec3 b=deform(owner,center.add(currentPoint(Math.max(0,t-.045),time,sign,radius,height)),partial);
                QiPressureMesh.ribbon(out,m,b,a,camera,.017,color,alpha*.65F*(float)Math.sin(t*Math.PI));
            }
        }
    }
    private static Vec3 deform(LivingEntity owner,Vec3 point,float partial){return com.example.immortal_cultivation_mod.client.QiAuraRenderer.deformBodyPoint(owner,point,partial);}

    /** Bounded phase; each current circles in the opposite direction at every world age. */
    public static Vec3 currentPoint(double t,double time,int sign,double radius,double height) {
        double phase=(time%480)*TAU/480,angle=sign*(phase*3+t*4.2)+(sign<0?Math.PI:0);
        double r=radius*(.87+.13*Math.sin(t*Math.PI));
        return new Vec3(Math.cos(angle)*r,(t-.5)*height,Math.sin(angle)*r);
    }

    public static void domain(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double time,float alpha) {
        domainFrames++;
        int count=QiVfxQuality.level()==0?3:5;
        for(int j=0;j<count;j++) {
            double orbit=j*2.399+(time%1200)*TAU/1200,r=5+j*3.0;
            Vec3 center=p.add(Math.cos(orbit)*r,.20+(j%2)*.75,Math.sin(orbit)*r);
            Vec3 normal=new Vec3(Math.cos(orbit)*.32,1,Math.sin(orbit)*.32).normalize();
            float breath=(float)(.68+.32*Math.sin(time*.022+j));
            wheel(out,m,center,normal,camera,.66+j*.095,(time%480)*TAU/240+j,alpha*breath*.37F);
        }
    }

    public static void impact(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 normal,Vec3 camera,double age,float power) {
        impactFrames++;
        Vec3 n=normal.lengthSqr()<.01?new Vec3(0,1,0):normal.normalize();
        Vec3 u=perpendicular(n),v=n.cross(u).normalize();
        for(int image=0;image<3;image++) {
            double local=age-image*2.5;if(local<0||local>=15)continue;
            double expand=.16+local*.085+image*.06;
            Vec3 center=p.add(n.scale(local*.028+image*.10)).add(u.scale(Math.sin(image*2.1)*.075)).add(v.scale(Math.cos(image*2.1)*.075));
            float fade=(float)Math.pow(1-local/15,1.4)*(.68F+power*.20F);
            Vec3 axis=n.add(u.scale((image-1)*.12)).normalize();
            arc(out,m,center,axis,camera,expand,local*.075*(image%2==0?1:-1)+image*.8,TAU*.89,
                    .022+power*.010,image%2==0?VIOLET:JADE,fade);
        }
        if(age<10)for(int j=0;j<6;j++) {
            double angle=j*TAU/6+age*.04,r=.14+age*.06;
            Vec3 a=p.add(u.scale(Math.cos(angle)*r)).add(v.scale(Math.sin(angle)*r));
            QiPressureMesh.ribbon(out,m,a,a.add(n.scale(.10)).add(u.scale(Math.cos(angle)*.05)),camera,.012,j%2==0?VIOLET:JADE,(float)(.55*(1-age/10)));
        }
    }

    public static void status(VertexConsumer out,Matrix4f m,LivingEntity victim,Vec3 p,Vec3 camera,double time,double age,float alpha) {
        procFrames++;
        float end=(float)Math.clamp((200-age)/12,0,1);
        if(age<34)wheel(out,m,p.add(0,.055,0),new Vec3(0,1,0),camera,
                Math.max(.62,victim.getBbWidth()*.85),time*.025,alpha*.70F*(float)(1-age/34));
        double radius=(victim.getBbWidth()*.67+.10)*(.75+.25*end);
        int steps=QiVfxQuality.level()==0?20:32;
        Vec3 center=p.add(0,victim.getBbHeight()*.5,0);
        for(int strand=0;strand<2;strand++)for(int i=0;i<steps;i++) {
            double t=i/(double)steps,u=(i+1.0)/steps;
            int sign=strand==0?1:-1,color=strand==0?BLACK:WHITE;
            Vec3 a=center.add(currentPoint(t,time,sign,radius,victim.getBbHeight()*.94));
            Vec3 b=center.add(currentPoint(u,time,sign,radius,victim.getBbHeight()*.94));
            float fade=alpha*end*(float)Math.sin(t*Math.PI);
            QiPressureMesh.ribbon(out,m,a,b,camera,.022,color,fade*.50F);
            if(i%8==0)QiPressureMesh.ribbon(out,m,a,a.add(0,.055,0),camera,.015,strand==0?VIOLET:JADE,fade*.70F);
        }
    }

    private static void wheel(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 normal,Vec3 camera,double radius,double phase,float alpha) {
        arc(out,m,p,normal,camera,radius,phase,Math.PI*.95,.023,WHITE,alpha);
        arc(out,m,p,normal,camera,radius,phase+Math.PI,Math.PI*.95,.027,BLACK,alpha*.95F);
        Vec3 u=perpendicular(normal),v=normal.cross(u).normalize();
        // Two opposing inner comma curves form the wheel without a filled disc.
        for(int half=0;half<2;half++) {
            double angle=phase+half*Math.PI;
            Vec3 offset=u.scale(Math.cos(angle)*radius*.40).add(v.scale(Math.sin(angle)*radius*.40));
            arc(out,m,p.add(offset),normal,camera,radius*.40,phase+half*Math.PI,Math.PI,.018,half==0?WHITE:BLACK,alpha*.8F);
            Vec3 eye=p.add(offset.scale(1.15));
            QiPressureMesh.ribbon(out,m,eye.subtract(v.scale(.022)),eye.add(v.scale(.022)),camera,.021,half==0?JADE:VIOLET,alpha);
        }
    }
    private static void arc(VertexConsumer out,Matrix4f m,Vec3 center,Vec3 normal,Vec3 camera,double radius,
                            double phase,double sweep,double width,int color,float alpha) {
        Vec3 u=perpendicular(normal),v=normal.cross(u).normalize();
        int steps=QiVfxQuality.level()==0?24:40;
        for(int i=0;i<steps;i++) {
            double a=phase+sweep*i/steps,b=phase+sweep*(i+1)/steps;
            Vec3 p=center.add(u.scale(Math.cos(a)*radius)).add(v.scale(Math.sin(a)*radius));
            Vec3 q=center.add(u.scale(Math.cos(b)*radius)).add(v.scale(Math.sin(b)*radius));
            QiPressureMesh.ribbon(out,m,p,q,camera,width,color,alpha);
        }
    }
    private static Vec3 perpendicular(Vec3 normal) {
        return normal.cross(Math.abs(normal.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
    }
}
