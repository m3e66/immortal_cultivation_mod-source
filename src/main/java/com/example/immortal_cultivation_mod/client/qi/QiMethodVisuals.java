package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.combat.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Art-specific world-space currents, ripples, blood flowers and cutting wind sheets. */
public final class QiMethodVisuals {
    public static long auraFrames,impactFrames,windFrames;
    private QiMethodVisuals() { }

    public static void aura(VertexConsumer out,Matrix4f matrix,LivingEntity owner,Vec3 origin,Vec3 camera,
                            QiElementLook look,double time,float alpha) {
        int art=look.art();
        if(art==5) {
            auraFrames++;ReincarnationVisuals.aura(out,matrix,owner,origin,camera,time,alpha);return;
        }
        if(art==7) {
            auraFrames++;VoidWindVisuals.aura(out,matrix,owner,origin,camera,time,alpha);return;
        }
        if(art>=9&&art<=13) {
            AdditionalMethodVisuals.aura(out,matrix,owner,origin,camera,look,time,alpha);auraFrames++;return;
        }
        if(art>=14&&art<=16) {
            ElementalDomainVisuals.aura(out,matrix,owner,origin,camera,look,time,alpha);auraFrames++;return;
        }
        if(art<0||art>7)return;
        auraFrames++;
        if(art==0||art==1||art==6) {
            int streams=3;
            double height=owner.getBbHeight()*1.18;
            for(int j=0;j<streams;j++)for(int i=0;i<18;i++) {
                double t=i/18.0,u=(i+1)/18.0;
                double direction=art==6?-1:1;
                double phase=time*(art==1?.035:.075)*direction+j*Math.PI*2/streams;
                double r=owner.getBbWidth()*.70+.13;
                Vec3 p=stream(origin,t,phase,r,height,art),q=stream(origin,u,phase,r,height,art);
                int color=look.color();
                QiPressureMesh.ribbon(out,matrix,p,q,camera,.018,color,
                        alpha*(art==0?.20F:art==1?.18F:.35F)*(float)Math.sin(t*Math.PI));
            }
        }
        if(art==1)for(int j=0;j<7;j++) {
            double angle=j*2.399+Math.sin(time*.018+j)*.15;
            Vec3 p=origin.add(Math.cos(angle)*.65,.3+(j%4)*.4+Math.sin(time*.035+j)*.035,Math.sin(angle)*.65);
            QiPressureMesh.ribbon(out,matrix,p.add(0,-.025,0),p.add(0,.045,0),camera,.026,look.highlight(),alpha*.75F);
        }
        if(art>=2&&art<=4)FoundationDomainVisuals.bloodAura(out,matrix,owner,origin,camera,look,time,alpha);
        float swing=owner.getAttackAnim(1);
        if(swing>0&&swing<1&&(art==0||art==1)) {
            Vec3 normal=owner.getLookAngle(),center=origin.add(0,owner.getBbHeight()*.63,0);
            for(int j=0;j<2;j++) {
                double advance=swing*1.8+j*.22;
                QiPressureMesh.wave(out,matrix,center.add(normal.scale(.5+advance)),normal,camera,
                        .12+advance*.30,alpha*(1-swing)*.6F,j%2==0?look.color():look.highlight());
            }
        }
    }
    private static Vec3 stream(Vec3 p,double t,double phase,double radius,double height,int art) {
        double angle=phase+t*(art==6?-7:5);
        double r=radius*(art==6?1.2-.65*t:1);
        return p.add(Math.cos(angle)*r,.08+t*height,Math.sin(angle)*r);
    }

    /** True means this method supplies its own hit shape instead of the generic attached flame. */
    public static boolean impact(VertexConsumer out,Matrix4f matrix,Vec3 point,Vec3 normal,Vec3 camera,
                                 QiElementLook look,double age,float power) {
        int art=look.art();
        if(art==5) {
            impactFrames++;ReincarnationVisuals.impact(out,matrix,point,normal,camera,age,power);return true;
        }
        if(art==7) {
            impactFrames++;VoidWindVisuals.impact(out,matrix,point,normal,camera,age,power);return true;
        }
        if(art>=9&&art<=13) {
            impactFrames++;AdditionalMethodVisuals.impact(out,matrix,point,normal,camera,look,age,power);return true;
        }
        if(art>=14&&art<=16) {
            impactFrames++;ElementalDomainVisuals.impact(out,matrix,point,normal,camera,look,age,power);return true;
        }
        if(art<0||art>7||art==6)return false;
        impactFrames++;
        float fade=(float)Math.max(0,1-age/16);
        double expansion=.16+age*.11;
        if(art>=2&&art<=4) {
            Vec3 u=normal.cross(Math.abs(normal.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
            Vec3 v=normal.cross(u).normalize();
            for(int petal=0;petal<6;petal++)for(int i=0;i<12;i++) {
                double a=i*Math.PI*2/12,b=(i+1)*Math.PI*2/12,turn=petal*Math.PI/3;
                Vec3 p=petal(point,u,v,a,turn,expansion*(.8+power*.6)),q=petal(point,u,v,b,turn,expansion*(.8+power*.6));
                QiPressureMesh.ribbon(out,matrix,p,q,camera,.035,petal%2==0?look.color():look.highlight(),fade*.85F);
            }
        }else {
            int rings=3;
            for(int j=0;j<rings;j++) {
                Vec3 offset=normal.scale(j*.06);
                QiPressureMesh.wave(out,matrix,point.add(offset),normal,camera,expansion+j*.13,
                        fade*(art==0?.42F:.60F),j%2==0?look.color():look.highlight());
            }
        }
        return true;
    }
    private static Vec3 petal(Vec3 center,Vec3 u,Vec3 v,double phase,double turn,double size) {
        double x=(.5+.5*Math.cos(phase))*size,y=Math.sin(phase)*size*.28;
        return center.add(u.scale(x*Math.cos(turn)-y*Math.sin(turn))).add(v.scale(x*Math.sin(turn)+y*Math.cos(turn)));
    }

    public static void domain(VertexConsumer out,Matrix4f matrix,LivingEntity owner,Vec3 origin,Vec3 camera,
                              QiElementLook look,double time,float alpha) {
        if(look.art()>=9&&look.art()<=13) {
            AdditionalMethodVisuals.domain(out,matrix,owner,origin,camera,look,time,alpha);return;
        }
        if(look.art()>=14&&look.art()<=16) {
            ElementalDomainVisuals.domain(out,matrix,owner,origin,camera,look,time,alpha);return;
        }
        if(look.art()==7) {
            windFrames++;
            VoidWindVisuals.domain(out,matrix,owner,origin,camera,time,alpha);
        } else if(look.art()>=2&&look.art()<=4) {
            FoundationDomainVisuals.bloodDomain(out,matrix,owner,origin,camera,look,time,alpha);
        }else if(look.art()==5)FoundationDomainVisuals.rebirthDomain(out,matrix,origin,camera,time,alpha);
        else if(look.art()==6)FoundationDomainVisuals.devourDomain(out,matrix,origin,camera,time,alpha);
    }
}
