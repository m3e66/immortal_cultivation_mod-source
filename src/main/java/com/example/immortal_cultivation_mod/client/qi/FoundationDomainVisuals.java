package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.client.QiAuraRenderer;
import com.example.immortal_cultivation_mod.combat.QiDomainSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Circulation, reincarnation and devouring use distinct silhouettes and directional motion. */
public final class FoundationDomainVisuals {
    public static long bloodFrames,bloodFlowFrames,thornFrames,rebirthFrames,devourFrames,collapseFrames;
    private FoundationDomainVisuals() { }
    public static int bloodTier(int art){return Math.clamp(art-1,1,3);}
    public static void bloodAura(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,QiElementLook look,double time,float alpha) {
        if(BloodLilyClient.hidden(owner))return;
        bloodFrames++;int tier=bloodTier(look.art());
        for(int j=0;j<3+tier*2;j++) {
            double a=j*Math.PI*2/(3+tier*2)+time*.015,r=.55;
            Vec3 from=p.add(Math.cos(a)*r,owner.getBbHeight()*.69,Math.sin(a)*r);
            Vec3 tip=p.add(Math.cos(a)*(.73+tier*.025),owner.getBbHeight()*(.93+(j%2)*.10),Math.sin(a)*(.73+tier*.025));
            spike(out,m,from,tip,.045+tier*.01,look.highlight(),alpha*(.23F+tier*.08F));
        }
        for(int j=0;j<tier*3;j++) {
            double a=j*2.399+time*.025,t=(time*.016+j*.17)%1;
            Vec3 drop=p.add(Math.cos(a)*(.65+tier*.015),.18+t*owner.getBbHeight(),Math.sin(a)*(.65+tier*.015));
            QiPressureMesh.ribbon(out,m,drop,drop.add(0,.045,0),camera,.012+tier*.004,look.highlight(),alpha*.6F);
        }
    }
    public static void bloodDomain(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,QiElementLook look,double time,float alpha) {
        int tier=bloodTier(look.art()),marks=Math.min(12,QiAuraRenderer.bloodMarks(owner));
        int count=8+tier*4+marks;
        for(int j=0;j<count;j++) {
            double a=j*2.399+time*.004,r=3+Math.sqrt((j+.5)/count)*16;
            Vec3 base=p.add(Math.cos(a)*r,.3+(j%4)*.5+Math.sin(time*.018+j)*.14,Math.sin(a)*r);
            QiPressureMesh.ribbon(out,m,base.add(0,-.06,0),base.add(0,.06,0),camera,.016,look.color(),alpha*(.3F+tier*.1F));
            if(j%3==0)for(int i=0;i<8;i++) {
                double t=i/8.0,u=(i+1)/8.0;
                Vec3 from=base.add(t*1.2,Math.sin(t*5+time*.04+j)*.12,Math.cos(t*4+j)*.2);
                Vec3 to=base.add(u*1.2,Math.sin(u*5+time*.04+j)*.12,Math.cos(u*4+j)*.2);
                QiPressureMesh.ribbon(out,m,from,to,camera,.008+tier*.002,look.highlight(),alpha*.23F);
            }
        }
        for(int i=0;i<QiDomainSystem.MOTES;i++) {
            Vec3 point=QiDomainSystem.motePosition(owner,i,(long)time).add(p.subtract(owner.position()));
            spike(out,m,point.add(0,-.10,0),point.add(0,.15,0),.025,look.highlight(),alpha*.42F);
        }
    }
    public static void bloodFlow(VertexConsumer out,Matrix4f m,Vec3 from,Vec3 chest,Vec3 camera,QiElementLook look,double age,float alpha,int marks) {
        bloodFlowFrames++;int tier=bloodTier(look.art()),streams=tier*2;
        Vec3 axis=chest.subtract(from),side=axis.cross(new Vec3(0,1,0)).normalize();
        double width=.007+tier*.005;
        for(int j=0;j<streams;j++)for(int i=0;i<24;i++) {
            double t=i/24.0,u=(i+1)/24.0;
            Vec3 a=vessel(from,chest,side,t,j,age,tier),b=vessel(from,chest,side,u,j,age,tier);
            double pulse=(t-age*.07-j*.2)%1;if(pulse<0)pulse++;
            float fade=alpha*(float)(.12+.7*Math.exp(-Math.pow((pulse-.5)/.13,2)));
            QiPressureMesh.ribbon(out,m,a,b,camera,width,j%2==0?look.color():look.highlight(),fade);
            if(tier==3&&i==15)QiPressureMesh.ribbon(out,m,a,a.lerp(chest.add(side.scale((j%2==0?1:-1)*.25)).add(0,.3,0),.85),camera,
                    width*.55,look.highlight(),fade*.65F);
        }
        for(int j=0;j<Math.min(16,3+marks*tier);j++) {
            double a=j*2.399+age*.15,r=.25+Math.sin(age*.08+j)*.07;
            Vec3 end=chest.add(Math.cos(a)*r,Math.sin(a)*.18,Math.sin(a)*r);
            QiPressureMesh.ribbon(out,m,end,end.lerp(chest,.35),camera,.014,look.highlight(),alpha*.6F);
        }
    }
    private static Vec3 vessel(Vec3 from,Vec3 chest,Vec3 side,double t,int stream,double age,int tier) {
        double curve=Math.sin(t*Math.PI),wave=Math.sin(t*8+stream*1.8-age*.10);
        return from.lerp(chest,t).add(side.scale(wave*(.10+tier*.035)*curve))
                .add(0,curve*(.12+stream*.035+Math.cos(t*7+stream-age*.08)*.045),0);
    }
    public static void thorns(VertexConsumer out,Matrix4f m,LivingEntity victim,Vec3 p,QiElementLook look,double time,double age,float alpha) {
        if(BloodLilyClient.hidden(victim))return;
        thornFrames++;int tier=bloodTier(look.art());
        double phase=(time+victim.getId()*7)%32;float fade=alpha*(float)Math.sin(phase/32*Math.PI);
        for(int j=0;j<2+tier*2;j++) {
            double a=j*2.399+time*.008;Vec3 root=p.add(Math.cos(a)*(victim.getBbWidth()*.65+.1),.15+(j%3)*victim.getBbHeight()*.27,
                    Math.sin(a)*(victim.getBbWidth()*.65+.1));
            spike(out,m,root,root.add(Math.cos(a)*.09,.20+tier*.055,Math.sin(a)*.09),.03+tier*.008,look.color(),fade*.7F);
        }
    }
    public static void rebirthDomain(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double time,float alpha) {
        ReincarnationVisuals.domain(out,m,p,camera,time,alpha);
    }
    public static void rebirth(VertexConsumer out,Matrix4f m,LivingEntity victim,Vec3 p,Vec3 camera,double time,double age,float alpha) {
        rebirthFrames++;
        ReincarnationVisuals.status(out,m,victim,p,camera,time,age,alpha);
    }
    public static void devourDomain(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double time,float alpha) {
        for(int j=0;j<6;j++) {
            double a=j*2.399-time*.003,r=4+j*2.4;
            Vec3 center=p.add(Math.cos(a)*r,.5+(j%3)*.6,Math.sin(a)*r);
            for(int i=0;i<6;i++) {
                double t=i/6.0,u=(i+1)/6.0;
                QiPressureMesh.ribbon(out,m,center.add(t*.8,Math.sin(t*5+time*.02)*.08,0),center.add(u*.8,Math.sin(u*5+time*.02)*.08,0),
                        camera,.015,0x20102D,alpha*.20F);
            }
        }
    }
    public static void devour(VertexConsumer out,Matrix4f m,Vec3 center,Vec3 camera,QiElementLook look,double age,float alpha) {
        devourFrames++;double contract=Math.pow(Math.max(0,1-age/22),.6);
        if(age>17)collapseFrames++;
        for(int j=0;j<3;j++)for(int i=0;i<24;i++) {
            double t=i/24.0,u=(i+1)/24.0,phase=age*.40+j*Math.PI*2/3;
            double radius=contract*(.45+.65*(1-t));
            Vec3 from=center.add(Math.cos(phase+t*9)*radius,(t-.5)*2.2*contract,Math.sin(phase+t*9)*radius);
            Vec3 to=center.add(Math.cos(phase+u*9)*radius,(u-.5)*2.2*contract,Math.sin(phase+u*9)*radius);
            QiPressureMesh.ribbon(out,m,from,to,camera,.045,j==0?0x09070E:look.color(),alpha*.65F);
        }
        if(age>17&&age<24) {
            double size=.10*(24-age)/7;
            QiPressureMesh.ribbon(out,m,center.add(0,-size,0),center.add(0,size,0),camera,size,0x050307,.95F);
        }
    }
    public static void windLift(VertexConsumer out,Matrix4f m,LivingEntity victim,Vec3 p,Vec3 camera,double age,float alpha) {
        Vec3 motion=victim.position().subtract(new Vec3(victim.xo,victim.yo,victim.zo));
        Vec3 back=motion.lengthSqr()>.0001?motion.normalize().scale(-.9):new Vec3(0,-.65,0);
        for(int j=0;j<3;j++) {
            Vec3 from=p.add((j-1)*victim.getBbWidth()*.25,victim.getBbHeight()*.5,0);
            QiPressureMesh.ribbon(out,m,from,from.add(back.scale(1-age/20)),camera,.025,com.example.immortal_cultivation_mod.combat.MethodDomainStyle.WIND.secondary,alpha*.25F);
        }
    }
    private static void spike(VertexConsumer out,Matrix4f m,Vec3 root,Vec3 tip,double size,int color,float alpha) {
        Vec3 u=tip.subtract(root).cross(new Vec3(0,0,1)).normalize().scale(size),v=tip.subtract(root).normalize().cross(u).normalize().scale(size);
        Vec3[] base={root.add(u),root.add(v),root.subtract(u),root.subtract(v)};
        Vec3 camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for(int i=0;i<4;i++)for(var point:new Vec3[]{base[i],base[(i+1)%4],tip,tip}) {
            Vec3 p=point.subtract(camera);float shade=i%2==0?1:.65F;
            out.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor((color>>16&255)/255F*shade,(color>>8&255)/255F*shade,(color&255)/255F*shade,alpha*QiVfxQuality.opacity());
        }
    }
}
