package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.client.QiAuraRenderer;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Ash returning to the heart, crystalline ridges, physical bell waves and solar fire. */
public final class AdditionalMethodVisuals {
    public static long ashFrames,iceFrames,boneFrames,boneDomainFrames,boneStatusFrames,netherFrames,solarFrames,vitalityFrames,pillarFrames;
    private AdditionalMethodVisuals() { }
    public static void aura(VertexConsumer out,Matrix4f m,LivingEntity entity,Vec3 p,Vec3 camera,QiElementLook look,double time,float alpha) {
        int art=look.art();
        if(!QiAuraRenderer.bodyRingsVisible(entity,art))return;
        if(art==9) {
            ashFrames++;
            for(int j=0;j<10;j++) {
                double t=((time*.018+j*.13)%1),a=j*2.399+time*.025;
                Vec3 ember=p.add(Math.cos(a)*(.7-t*.4),.1+t*entity.getBbHeight(),Math.sin(a)*(.7-t*.4));
                QiPressureMesh.ribbon(out,m,ember,ember.add(0,.07,0),camera,.022,0xFFD478,alpha*(float)Math.sin(t*Math.PI));
            }
        }else if(art==10) {
            iceFrames++;
            for(int j=0;j<9;j++) {
                double a=j*2.399,r=.63+(j%2)*.1,y=((j*.27-time*.014)%1+1)%1*entity.getBbHeight();
                shard(out,m,p.add(Math.cos(a)*r,y,Math.sin(a)*r),camera,.08,look.highlight(),alpha*.6F);
            }
        }else if(art==11) {
            boneFrames++;
            for(int j=0;j<3;j++)band(out,m,p.add(0,entity.getBbHeight()*(.58+j*.11),0),camera,.68+j*.06,.05,time*.005+j,look.color(),alpha*.4F);
        }else if(art==12) {
            netherFrames++;
            for(int j=0;j<4;j++) {
                double a=j*Math.PI/2-time*.015;
                Vec3 start=p.add(Math.cos(a)*.7,.3+j*.3,Math.sin(a)*.7);
                Vec3 end=start.add(Math.sin(time*.06+j)*.3,.5,Math.cos(time*.04+j)*.3);
                QiPressureMesh.ribbon(out,m,start,end,camera,.035,0x93E9AA,alpha*.23F);
            }
        }else if(art==13) {
            solarFrames++;
            for(int j=0;j<7;j++) {
                double a=j*2.399+time*.018;
                Vec3 ember=p.add(Math.cos(a)*.8,.2+(j%4)*.37,Math.sin(a)*.8);
                shard(out,m,ember,camera,.045,0xFFF3BD,alpha*.65F);
            }
        }
        float swing=entity.getAttackAnim(1);
        if(swing>0&&swing<1&&(art==9||art==11||art==13)) {
            Vec3 normal=entity.getLookAngle();
            for(int j=0;j<2;j++)QiPressureMesh.wave(out,m,p.add(0,entity.getBbHeight()*.6,0).add(normal.scale(.5+swing*1.5+j*.15)),
                    normal,camera,.12+swing*.55,alpha*(1-swing)*.45F,look.highlight());
        }
    }
    public static void domain(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,QiElementLook look,double time,float alpha) {
        int art=look.art();
        if(art==12)return;
        if(art==11) {
            if(!QiAuraRenderer.bodyRingsVisible(owner,art))return;
            boneDomainFrames++;
            double phase=(time+owner.getId()*7)%55;
            if(phase<45)QiPressureMesh.wave(out,m,p.add(0,owner.getBbHeight()*.50,0),new Vec3(0,1,0),camera,
                    .6+phase*.43,alpha*.12F*(float)Math.sin(phase/45*Math.PI),look.color());
            return;
        }
        int count=QiVfxQuality.level()==0?8:16;
        for(int j=0;j<count;j++) {
            double a=j*2.399+time*.002,r=3+(j%4)*4.8;
            Vec3 base=p.add(Math.cos(a)*r,.04,Math.sin(a)*r);
            if(art==9) {
                double age=(time+j*13)%50;
                if(age<23)plume(out,m,base,camera,look,age,1.0,alpha*.28F);
                Vec3 ember=base.add(Math.sin(time*.03+j)*.5,.2+((time+j*9)%45)*.045,Math.cos(time*.03+j)*.5);
                shard(out,m,ember,camera,.025,0xF8CD71,alpha*.6F);
            }else if(art==10) {
                double fall=((time+j*7)%90)/90;
                shard(out,m,base.add(Math.sin(j)*.6,3-fall*3,Math.cos(j)*.6),camera,.05,look.highlight(),alpha*.45F);
            }else if(art==13) {
                float sun=QiAuraRenderer.sunEnergy(owner)/100F;
                if(j>=4+Math.round(sun*(count-4)))continue;
                Vec3 point=base.add(Math.sin(time*.025+j)*.4,.3+(j%4)*.45,Math.cos(time*.025+j)*.4);
                shard(out,m,point,camera,.04+sun*.04,0xFFF2B0,alpha*(.2F+sun*.65F));
            }
        }
    }
    public static void status(VertexConsumer out,Matrix4f m,LivingEntity victim,Vec3 p,Vec3 camera,QiElementLook look,
                              int kind,int strength,double age,double time,float alpha) {
        if(kind==QiAuraPayloads.DomainEffect.FIRE_PLUME) {
            for(int j=0;j<4;j++) {
                double angle=j*Math.PI/2;
                Vec3 edge=p.add(Math.cos(angle)*victim.getBbWidth()*.55,0,Math.sin(angle)*victim.getBbWidth()*.55);
                plume(out,m,edge,camera,look,age,Math.max(1,victim.getBbHeight()*1.15),alpha*.38F);
            }
            if(age<18)for(int j=0;j<4;j++) {
                double t=age/18,a=j*Math.PI/2+t*3;
                Vec3 ember=p.add(Math.cos(a)*.5,.2+t*victim.getBbHeight(),Math.sin(a)*.5);
                shard(out,m,ember,camera,.035,0xFFD16C,alpha*(float)(1-t));
            }
        }else if(kind==QiAuraPayloads.DomainEffect.BONE) {
            if(!QiAuraRenderer.bodyRingsVisible(victim,11))return;
            boneStatusFrames++;
            double beat=(time+victim.getId())%22;
            QiPressureMesh.wave(out,m,p.add(0,.04,0),new Vec3(0,1,0),camera,.15+beat*.035,
                    alpha*.13F*(float)(1-beat/22),0xD5CDB2);
            Vec3 shoulder=p.add(0,victim.getBbHeight()*.72,0);
            for(int j=0;j<3;j++) {
                Vec3 from=shoulder.add((j-1)*victim.getBbWidth()*.4,0,0);
                QiPressureMesh.ribbon(out,m,from,from.add(0,-.35,0),camera,.008,0xE6DFCD,alpha*.12F);
            }
        }else if(kind==QiAuraPayloads.DomainEffect.SOLAR) {
            pillarFrames++;float energy=strength/100F,fade=(float)Math.max(0,1-age/20);
            plume(out,m,p,camera,look,age,2+energy*2,alpha*(.45F+energy*.5F));
            Vec3 top=p.add(0,1,0);
            QiPressureMesh.wave(out,m,p.add(0,.08,0),new Vec3(0,1,0),camera,.15+age*.16,fade*.42F,0xFFF6D4);
            if(age<5)for(int j=0;j<3;j++)QiPressureMesh.ribbon(out,m,p.add((j-1)*.1,0,0),top.add((j-1)*.1,1,0),camera,.08,0xFFFCED,fade*.6F);
        }
    }
    public static void vitality(VertexConsumer out,Matrix4f m,Vec3 from,Vec3 chest,Vec3 camera,double time,double age,float alpha) {
        vitalityFrames++;Vec3 direction=chest.subtract(from),side=direction.cross(new Vec3(0,1,0)).normalize();
        for(int j=0;j<9;j++) {
            double t=(age*.055+j*.11)%1,spiral=time*.12+j*2.399,r=Math.sin(t*Math.PI)*.38;
            Vec3 p=from.lerp(chest,t).add(side.scale(Math.cos(spiral)*r)).add(0,Math.sin(spiral)*r+.18*Math.sin(t*Math.PI),0);
            Vec3 tail=p.subtract(direction.normalize().scale(.09));
            QiPressureMesh.ribbon(out,m,tail,p,camera,.022,0xFFDA7B,alpha*.9F);
        }
    }
    public static void impact(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 normal,Vec3 camera,QiElementLook look,double age,float power) {
        float fade=(float)Math.max(0,1-age/16);
        if(look.art()==10) {
            Vec3 u=normal.cross(Math.abs(normal.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=normal.cross(u);
            for(int j=0;j<6;j++) {
                double a=j*Math.PI/3;Vec3 branch=u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
                Vec3 end=p.add(branch.scale(.2+age*.06));
                QiPressureMesh.ribbon(out,m,p,end,camera,.014,look.highlight(),fade*.8F);
                QiPressureMesh.ribbon(out,m,end,end.add(branch.scale(.2)).add(u.scale(.12)),camera,.008,look.highlight(),fade*.6F);
            }
        }else if(look.art()==12) {
            for(int j=0;j<5;j++)QiPressureMesh.ribbon(out,m,p.add(Math.cos(j)*age*.025,0,Math.sin(j)*age*.025),
                    p.add(Math.cos(j)*age*.035,.2+age*.04,Math.sin(j)*age*.035),camera,.06,look.color(),fade*.28F);
        }else for(int j=0;j<3;j++)QiPressureMesh.wave(out,m,p.add(normal.scale(j*.12)),normal,camera,
                .12+age*(look.art()==11?.09:.12)+j*.12,fade*(look.art()==11?.6F:.45F),look.highlight());
    }
    private static void plume(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,QiElementLook look,double age,double height,float alpha) {
        double life=Math.max(0,1-age/24),rise=Math.sin(Math.min(1,age/5)*Math.PI/2);
        for(int j=0;j<6;j++)for(int i=0;i<7;i++) {
            double t=i/7.0,u=(i+1)/7.0,a=j*Math.PI/3+age*.06;
            Vec3 from=p.add(Math.cos(a)*.28*(1-t),t*height*rise,Math.sin(a)*.28*(1-t));
            Vec3 to=p.add(Math.cos(a+.22)*.28*(1-u),u*height*rise,Math.sin(a+.22)*.28*(1-u));
            QiPressureMesh.ribbon(out,m,from,to,camera,.09*(1-t)+.02,j%2==0?look.color():look.highlight(),alpha*(float)life*(float)(1-t*.6));
        }
    }
    private static void band(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double radius,double width,double phase,int color,float alpha) {
        for(int i=0;i<32;i++) {
            double a=phase+i*Math.PI*2/32,b=phase+(i+1)*Math.PI*2/32;
            QiPressureMesh.ribbon(out,m,p.add(Math.cos(a)*radius,Math.sin(a*2)*.04,Math.sin(a)*radius),
                    p.add(Math.cos(b)*radius,Math.sin(b*2)*.04,Math.sin(b)*radius),camera,width,color,alpha);
        }
    }
    private static void shard(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double size,int color,float alpha) {
        QiPressureMesh.ribbon(out,m,p.add(0,-size,0),p.add(0,size,0),camera,size*.35,color,alpha);
        QiPressureMesh.ribbon(out,m,p.add(-size*.5,0,0),p.add(size*.5,0,0),camera,size*.2,color,alpha*.7F);
    }
}
