package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.combat.QiDomainSystem;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** World-space thunder branches, mountain strata and abyssal currents. No terrain mutation. */
public final class ElementalDomainVisuals {
    public static long stormFrames,mountainFrames,tideFrames,arcFrames,waterStackFrames,rippleFrames;
    private record Seen(long tick,List<LivingEntity> targets) { }
    private static final Map<UUID,Seen> NEARBY=new HashMap<>();
    private static Level world;
    private ElementalDomainVisuals() { }
    public static void clear() {NEARBY.clear();world=null;}

    private static List<LivingEntity> nearby(LivingEntity owner) {
        if(world!=owner.level()){clear();world=owner.level();}
        long now=world.getGameTime();var seen=NEARBY.get(owner.getUUID());
        if(seen==null||now-seen.tick>=10||now<seen.tick) {
            var targets=world.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(20),
                    e->e.distanceToSqr(owner)<=400&&QiDomainSystem.canAffect(owner,e));
            targets.sort(Comparator.comparingDouble((LivingEntity e)->e.distanceToSqr(owner)));
            if(targets.size()>24)targets=new ArrayList<>(targets.subList(0,24));
            if(NEARBY.size()>=64)NEARBY.clear();
            seen=new Seen(now,targets);NEARBY.put(owner.getUUID(),seen);
        }
        return seen.targets;
    }

    public static void aura(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,
                            QiElementLook look,double time,float alpha) {
        if(look.art()==14) {
            for(int i=0;i<4;i++) {
                double a=i*Math.PI/2+Math.floor(time/4)*.9;
                Vec3 from=p.add(Math.cos(a)*.65,.3,Math.sin(a)*.65);
                lightning(out,m,from,from.add(-Math.sin(a)*.5,1.4,Math.cos(a)*.5),camera,time+i,look,alpha*.7F);
            }
        }else if(look.art()==15) {
            for(int j=0;j<3;j++)orbit(out,m,p.add(0,.35+j*.5,0),camera,1.0+j*.08,.13,time*.009+j,look.color(),alpha*.22F);
            for(int i=0;i<6;i++) {
                double a=i*2.399+time*.007;
                rock(out,m,p.add(Math.cos(a)*.95,.3+i*.20+Math.sin(time*.035+i)*.16,Math.sin(a)*.95),camera,
                        .055+(i%3)*.018,time*.015+i,look,alpha*.8F);
            }
        }else {
            for(int j=0;j<3;j++)current(out,m,p.add(0,.2+j*.52,0),camera,.72,.045,-time*.018+j*2.1,look.color(),alpha*.45F);
            for(int j=0;j<9;j++) {
                double a=j*2.399-time*.017;
                Vec3 drop=p.add(Math.cos(a)*.87,.3+(j%4)*.34+Math.sin(time*.035+j)*.08,Math.sin(a)*.87);
                QiPressureMesh.ribbon(out,m,drop,drop.add(0,.07,0),camera,.024,look.highlight(),alpha*.75F);
            }
        }
        float swing=owner.getAttackAnim(1);
        if(look.art()==14&&swing>0&&swing<1) {
            Vec3 hand=p.add(0,owner.getBbHeight()*.65,0),forward=owner.getLookAngle();
            lightning(out,m,hand,hand.add(forward.scale(.5+swing*2.2)),camera,time,look,alpha*(1-swing));
        }
    }

    public static void domain(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,
                              QiElementLook look,double time,float alpha) {
        int count=nearby(owner).size(),quality=QiVfxQuality.level()==0?1:2;
        if(look.art()==14) {
            stormFrames++;
            int branches=Math.min(20,5+count/2)*quality/2;
            long phase=(long)time/7;
            for(int i=0;i<branches;i++) {
                double a=hash(i+phase*37)*Math.PI*2,r=4+hash(i*7+phase)*15;
                Vec3 from=p.add(Math.cos(a)*r,.2+hash(i*13+phase)*5,Math.sin(a)*r);
                Vec3 to=from.add(Math.cos(a+1)*2,hash(i*5+phase)>.5?-from.y+p.y+.12:1.2,Math.sin(a+1)*2);
                float flash=(float)Math.pow(Math.sin((time%7)/7*Math.PI),2);
                lightning(out,m,from,to,camera,phase+i,look,alpha*flash*.65F);
            }
            // Slowly rotating, layered cloud currents above the caster, with brief violet flashes.
            for(int j=0;j<3;j++)orbit(out,m,p.add(0,4.5+j*.3,0),camera,3+j*1.2,.30,
                    time*.006+j,look.color(),alpha*.10F);
            double pulse=(time+owner.getId()*3)%63;
            if(pulse<4)orbit(out,m,p.add(0,4.7,0),camera,4.3,.45,time*.006,look.color(),alpha*.22F*(float)(1-pulse/4));
        }else if(look.art()==15) {
            mountainFrames++;
            for(int i=0;i<8*quality;i++) {
                double a=i*2.399+time*.002,r=3+(i%4)*4.8;
                Vec3 base=ground(owner,p.add(Math.cos(a)*r,0,Math.sin(a)*r));
                rock(out,m,base.add(0,.7+Math.sin(time*.025+i)*.45,0),camera,.12+(i%3)*.06,time*.008+i,look,alpha*.65F);
                Vec3 end=base.add(Math.cos(a)*1.5,0,Math.sin(a)*1.5);
                crack(out,m,base,end,camera,time*.015+i,look,alpha*(.15F+Math.min(12,count)*.012F));
            }
        }else {
            tideFrames++;
            for(int j=0;j<4*quality;j++) {
                double a=j*2.399-time*.004,r=5+(j%4)*4.3;
                Vec3 center=p.add(Math.cos(a)*r,.3+(j%3)*.7,Math.sin(a)*r);
                current(out,m,center,camera,2.4,.12,-time*.014+j,look.color(),alpha*.22F);
                current(out,m,center.add(0,.08,0),camera,2.45,.025,-time*.014+j,look.highlight(),alpha*.18F);
            }
            double phase=(time+owner.getId()*2)%100;
            if(phase<70) {
                double radius=phase*.28;
                QiPressureMesh.wave(out,m,p.add(0,.11,0),new Vec3(0,1,0),camera,radius,
                        alpha*.25F*(float)Math.sin(phase/70*Math.PI),look.highlight());
                waterRing(out,m,p.add(0,.055,0),camera,Math.max(.05,radius-.25),.06,look.color(),alpha*.16F*(float)Math.sin(phase/70*Math.PI));
            }
        }
    }

    public static void status(VertexConsumer out,Matrix4f m,LivingEntity victim,Vec3 p,Vec3 camera,
                              QiElementLook look,int kind,int strength,double age,double time,float alpha) {
        if(kind==QiAuraPayloads.DomainEffect.MOUNTAIN) {
            double phase=(time+victim.getId()*7)%50;
            if(phase<32) {
                float fade=alpha*(float)Math.sin(phase/32*Math.PI);
                Vec3 base=p.add(0,victim.getBbHeight()+.45,0);
                double size=.55+strength*.035;
                for(int j=0;j<3;j++) {
                    Vec3 center=base.add((j-1)*size*.65,j==1?0:-.12,(j-1)*size*.24);
                    mountain(out,m,center,camera,size*(j==1?1:.65),size*(j==1?1.3:.9),look,fade*.22F);
                }
                for(int i=0;i<3;i++)QiPressureMesh.ribbon(out,m,base.add((i-1)*.4,-.1,0),base.add((i-1)*.4,-.5,0),camera,.008,look.color(),fade*.16F);
            }
            Vec3 delta=victim.position().subtract(new Vec3(victim.xo,victim.yo,victim.zo));
            if(delta.horizontalDistanceSqr()>.0001) {
                double beat=(time+victim.getId())%12;
                QiPressureMesh.wave(out,m,p.add(0,.035,0),new Vec3(0,1,0),camera,.15+beat*.065,
                        alpha*.35F*(float)(1-beat/12),look.color());
                for(int j=0;j<3;j++)crack(out,m,p.add(0,.04,0),p.add(Math.cos(j*2.1)*.7,.04,Math.sin(j*2.1)*.7),camera,j,look,alpha*.4F);
            }
        }else if(kind==QiAuraPayloads.DomainEffect.FROST) {
            for(int j=0;j<2+strength/2;j++) {
                double y=.15+j*.22,a=time*.018+j;
                current(out,m,p.add(0,y,0),camera,victim.getBbWidth()*.6+.08,.018,-a,look.highlight(),alpha*(.14F+strength*.02F));
                Vec3 ice=p.add(Math.cos(a)*victim.getBbWidth()*.65,y,Math.sin(a)*victim.getBbWidth()*.65);
                crystal(out,m,ice,camera,.035+strength*.004,time*.04+j,look,alpha*.6F);
            }
            for(int j=0;j<3;j++)current(out,m,p.add(0,.22+j*.25,0),camera,.75+j*.1,.12,
                    -time*.01+j,look.color(),alpha*.055F*strength);
        }else if(kind==QiAuraPayloads.DomainEffect.TIDE_WATER) {
            waterStackFrames++;
            int bands=2+Math.min(3,strength/2);
            for(int j=0;j<bands;j++) {
                double height=.12+(j+.4)/bands*victim.getBbHeight()*.75;
                current(out,m,p.add(0,height,0),camera,victim.getBbWidth()*.58+.06,.018+strength*.002,
                        -time*.018+j*1.7,look.color(),alpha*(.18F+strength*.025F));
                current(out,m,p.add(0,height+.035,0),camera,victim.getBbWidth()*.60+.06,.009,
                        -time*.018+j*1.7,look.highlight(),alpha*.27F);
            }
        }else if(kind==QiAuraPayloads.DomainEffect.HEAL) {
            for(int j=0;j<6;j++) {
                double t=Math.clamp(age/24,0,1),a=j*Math.PI/3+time*.025,r=(1-t)*1.5;
                Vec3 drop=p.add(Math.cos(a)*r,.4+t*.3,Math.sin(a)*r);
                QiPressureMesh.ribbon(out,m,drop,drop.add(0,.09,0),camera,.035,look.highlight(),alpha*.8F);
            }
        }
    }
    public static Vec3 rippleOrigin(LivingEntity victim){return ground(victim,victim.position());}
    /** These rings use the recorded step position, so they remain in the water after the actor moves. */
    public static void waterRipple(VertexConsumer out,Matrix4f m,Vec3 origin,Vec3 camera,QiElementLook look,int strength,double age) {
        if(age<0||age>=32)return;rippleFrames++;
        float fade=(float)Math.sin(Math.clamp(age/32,0,1)*Math.PI);
        double radius=.12+age*(.055+strength*.004);
        waterRing(out,m,origin,camera,radius,.045+strength*.002,look.highlight(),fade*.58F);
        waterRing(out,m,origin.add(0,-.008,0),camera,Math.max(.03,radius-.13),.065,look.color(),fade*.33F);
        if(age>6)waterRing(out,m,origin.add(0,.005,0),camera,Math.max(.03,radius-.38),.02,look.highlight(),fade*.26F);
    }
    private static void waterRing(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double radius,double width,int color,float alpha) {
        int segments=QiVfxQuality.level()==0?24:48;double inner=Math.max(.01,radius-width),outer=radius+width;
        for(int i=0;i<segments;i++) {
            double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            face(out,m,camera,color,alpha,p.add(Math.cos(a)*inner,0,Math.sin(a)*inner),p.add(Math.cos(b)*inner,0,Math.sin(b)*inner),
                    p.add(Math.cos(b)*outer,0,Math.sin(b)*outer),p.add(Math.cos(a)*outer,0,Math.sin(a)*outer));
        }
    }

    public static void impact(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 normal,Vec3 camera,QiElementLook look,double age,float power) {
        float fade=(float)Math.max(0,1-age/16);
        if(look.art()==14) {
            for(int i=0;i<5;i++) {
                double a=i*2.399;
                lightning(out,m,p,p.add(Math.cos(a)*(1+power),Math.sin(a*2),Math.sin(a)*(1+power)),camera,age+i,look,fade);
            }
        }else {
            for(int j=0;j<3;j++)QiPressureMesh.wave(out,m,p.add(normal.scale(j*.08)),normal,camera,
                    .2+age*.08+j*.15,fade*.5F,j==0?look.highlight():look.color());
            if(look.art()==15)for(int i=0;i<5;i++)rock(out,m,p.add(Math.cos(i*2.4)*age*.04,.15+Math.sin(age*.15)*.4,Math.sin(i*2.4)*age*.04),
                    camera,.06,age*.1+i,look,fade);
        }
    }

    public static void lightning(VertexConsumer out,Matrix4f m,Vec3 from,Vec3 to,Vec3 camera,double seed,QiElementLook look,float alpha) {
        arcFrames++;Vec3 last=from,axis=to.subtract(from),side=axis.cross(new Vec3(0,1,0)).normalize();
        if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        for(int i=1;i<=10;i++) {
            double t=i/10.0;
            Vec3 next=from.lerp(to,t).add(side.scale(Math.sin(t*Math.PI)*(hash(i*13+Math.floor(seed)*7)-.5)*.65))
                    .add(0,Math.sin(t*Math.PI)*(hash(i+Math.floor(seed)*3)-.5)*.5,0);
            QiPressureMesh.ribbon(out,m,last,next,camera,.035,look.color(),alpha*.45F);
            QiPressureMesh.ribbon(out,m,last,next,camera,.010,look.highlight(),alpha);
            if(i==4||i==7)QiPressureMesh.ribbon(out,m,next,next.add(side.scale(i==4?.55:-.45)).add(0,.3,0),camera,.009,look.highlight(),alpha*.7F);
            last=next;
        }
    }
    private static void orbit(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double radius,double width,double phase,int color,float alpha) {
        for(int i=0;i<24;i++) {
            double a=phase+i*Math.PI*2/24,b=phase+(i+1)*Math.PI*2/24;
            Vec3 from=p.add(Math.cos(a)*radius,Math.sin(a*3+phase)*.16,Math.sin(a)*radius);
            Vec3 to=p.add(Math.cos(b)*radius,Math.sin(b*3+phase)*.16,Math.sin(b)*radius);
            QiPressureMesh.ribbon(out,m,from,to,camera,width,color,alpha*(float)(.35+.65*Math.pow(.5+.5*Math.sin(a-phase),2)));
        }
    }
    private static void current(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double radius,double width,double phase,int color,float alpha) {
        for(int i=0;i<24;i++) {
            double t=i/24.0,u=(i+1)/24.0,a=phase+t*4.7,b=phase+u*4.7;
            Vec3 from=p.add(Math.cos(a)*radius,Math.sin(a*2+phase)*.22+t*.16,Math.sin(a)*radius);
            Vec3 to=p.add(Math.cos(b)*radius,Math.sin(b*2+phase)*.22+u*.16,Math.sin(b)*radius);
            QiPressureMesh.ribbon(out,m,from,to,camera,width,color,alpha*(float)Math.sin(t*Math.PI));
        }
    }
    private static void mountain(VertexConsumer out,Matrix4f m,Vec3 base,Vec3 camera,double size,double height,QiElementLook look,float alpha) {
        Vec3 top=base.add(size*.12,height,0);
        Vec3[] feet={base.add(-size,0,-size*.5),base.add(size*.8,0,-size*.5),base.add(size,0,size*.5),base.add(-size*.7,0,size*.5)};
        for(int i=0;i<4;i++) {
            face(out,m,camera,i%2==0?look.color():look.highlight(),alpha,feet[i],feet[(i+1)%4],top,top);
            QiPressureMesh.ribbon(out,m,feet[i],top,camera,.008,look.highlight(),alpha*.6F);
        }
    }
    private static void crystal(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double size,double spin,QiElementLook look,float alpha) {
        Vec3 top=p.add(0,size*2.4,0),bottom=p.add(0,-size*1.5,0);
        for(int i=0;i<4;i++) {
            double a=spin+i*Math.PI/2,b=spin+(i+1)*Math.PI/2;
            Vec3 first=p.add(Math.cos(a)*size,0,Math.sin(a)*size),second=p.add(Math.cos(b)*size,0,Math.sin(b)*size);
            face(out,m,camera,i%2==0?look.highlight():look.color(),alpha,first,second,top,top);
            face(out,m,camera,look.color(),alpha,second,first,bottom,bottom);
        }
    }
    private static void face(VertexConsumer out,Matrix4f m,Vec3 camera,int color,float alpha,Vec3... points) {
        for(var point:points) {
            Vec3 v=point.subtract(camera);
            out.addVertex(m,(float)v.x,(float)v.y,(float)v.z).setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha*QiVfxQuality.opacity());
        }
    }
    private static void crack(VertexConsumer out,Matrix4f m,Vec3 from,Vec3 to,Vec3 camera,double seed,QiElementLook look,float alpha) {
        Vec3 last=from;
        for(int j=1;j<=5;j++) {
            Vec3 next=from.lerp(to,j/5.0).add(j==5?0:Math.sin(j*7+seed)*.12,0,j==5?0:Math.cos(j*5+seed)*.12);
            QiPressureMesh.ribbon(out,m,last,next,camera,.018,look.highlight(),alpha);last=next;
        }
    }
    private static Vec3 ground(LivingEntity owner,Vec3 p) {
        BlockPos block=BlockPos.containing(p.add(0,2,0));
        if(!owner.level().hasChunkAt(block))return p.add(0,.04,0);
        for(int i=0;i<7&&owner.level().getBlockState(block).isAir();i++)block=block.below();
        return new Vec3(p.x,owner.level().getBlockState(block).isAir()?p.y+.04:block.getY()+1.025,p.z);
    }
    private static void rock(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 camera,double size,double spin,QiElementLook look,float alpha) {
        Vec3[] v=new Vec3[8];
        for(int i=0;i<8;i++)v[i]=new Vec3((i&1)==0?-size:size,(i&2)==0?-size*.7:size*.7,(i&4)==0?-size:size)
                .yRot((float)spin).add(p).subtract(camera);
        int[][] faces={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
        for(int f=0;f<faces.length;f++) {
            int color=f%2==0?look.color():look.highlight();
            for(int index:faces[f])out.addVertex(m,(float)v[index].x,(float)v[index].y,(float)v[index].z)
                    .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha*QiVfxQuality.opacity()*.65F);
        }
    }
    private static double hash(double value) {double v=Math.sin(value*127.1+311.7)*43758.5453;return v-Math.floor(v);}
}
