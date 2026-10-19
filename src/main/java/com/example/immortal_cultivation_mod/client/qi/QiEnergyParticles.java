package com.example.immortal_cultivation_mod.client.qi;

import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Small elemental forms, not fire sprites. One shared budget covers fragments and actual block dust. */
public final class QiEnergyParticles {
    private record Fragment(Vec3 point,Vec3 velocity,QiElementLook look,long start,int life,double seed){}
    private static final ArrayList<Fragment> FRAGMENTS=new ArrayList<>();
    private static ClientLevel world;
    private static long tick=Long.MIN_VALUE;
    private static int spent;
    private QiEnergyParticles(){}
    public static void clear(){FRAGMENTS.clear();world=null;tick=Long.MIN_VALUE;spent=0;}
    private static boolean take(){
        var level=Minecraft.getInstance().level;if(level==null)return false;
        if(level!=world){clear();world=level;}
        if(tick!=level.getGameTime()){tick=level.getGameTime();spent=0;}
        return spent++<QiVfxQuality.particleBudget();
    }
    public static void energy(Vec3 p,Vec3 velocity,QiElementLook look,int life){
        if(!take()||FRAGMENTS.size()>=512)return;
        FRAGMENTS.add(new Fragment(p,velocity,look,world.getGameTime(),Math.clamp(life,1,20),world.random.nextDouble()*6.283));
    }
    public static void debris(Vec3 p,Vec3 velocity,BlockState state){
        if(state.isAir()||!take())return;
        var particle=Minecraft.getInstance().particleEngine.createParticle(new BlockParticleOption(ParticleTypes.BLOCK,state),
                p.x,p.y,p.z,velocity.x,velocity.y,velocity.z);
        if(particle!=null){particle.setParticleSpeed(velocity.x,velocity.y,velocity.z);particle.setLifetime(12);particle.scale(.55F);}
    }
    public static void render(PoseStack pose,MultiBufferSource buffers,Vec3 camera,double time){
        if(world!=Minecraft.getInstance().level){clear();return;}
        FRAGMENTS.removeIf(f->time-f.start>=f.life||time<f.start);
        if(FRAGMENTS.isEmpty())return;
        var out=buffers.getBuffer(QiAuraMaterial.GLOW);var matrix=pose.last().pose();
        for(var f:FRAGMENTS){
            double age=time-f.start,t=age/f.life;
            Vec3 p=f.point.add(f.velocity.scale((1-Math.exp(-age*.10))/.10));
            if(p.distanceToSqr(camera)>64*64)continue;
            double nearFade=Math.clamp((p.distanceTo(camera)-.65)/1.2,0,1);
            if(nearFade<=0)continue;
            float alpha=(float)(.58*(1-t)*nearFade*QiVfxQuality.opacity());
            double spin=f.seed+age*.13;
            Vec3 u=new Vec3(Math.cos(spin),0,Math.sin(spin)),v=new Vec3(0,1,0);
            switch(f.look.family()){
                case QiElementLook.WOOD -> {
                    // A solid folded leaf stays readable when its narrow face turns toward the viewer.
                    leaf(out,matrix,camera,p,u,v,.16,.075,f.look,(float)(.85*(1-t)*nearFade*QiVfxQuality.opacity()));
                }
                case QiElementLook.LIGHTNING -> {
                    Vec3 last=p.subtract(v.scale(.13));
                    for(int j=0;j<3;j++){Vec3 next=p.add(v.scale(-.05+j*.08)).add(u.scale((j%2==0?1:-1)*.035));
                        QiPressureMesh.ribbon(out,matrix,last,next,camera,.008,f.look.highlight(),alpha);last=next;}
                }
                case QiElementLook.WIND,QiElementLook.FIRE,QiElementLook.DARK -> {
                    Vec3 axis=f.velocity.lengthSqr()>.0001?f.velocity.normalize():v;
                    QiPressureMesh.ribbon(out,matrix,p.subtract(axis.scale(.12)),p.add(axis.scale(.08)),camera,.014,f.look.color(),alpha);
                }
                case QiElementLook.METAL,QiElementLook.ICE -> {
                    Vec3 top=p.add(v.scale(.09)),bottom=p.subtract(v.scale(.09));
                    polygon(out,matrix,camera,f.look.highlight(),alpha,top,p.add(u.scale(.035)),bottom,p);
                    polygon(out,matrix,camera,f.look.color(),alpha,top,p,bottom,p.subtract(u.scale(.035)));
                }
                case QiElementLook.EARTH -> {
                    p=p.add(0,-age*age*.003,0);
                    polygon(out,matrix,camera,f.look.color(),alpha,p.add(u.scale(.035)).add(v.scale(.025)),p.add(u.scale(.035)).subtract(v.scale(.025)),
                            p.subtract(u.scale(.035)).subtract(v.scale(.025)),p.subtract(u.scale(.035)).add(v.scale(.025)));
                }
                default -> polygon(out,matrix,camera,f.look.highlight(),alpha,p.add(v.scale(.065)),p.add(u.scale(.025)),p.subtract(v.scale(.04)),p.subtract(u.scale(.025)));
            }
        }
    }
    /** Large luminous pollen marks the real wood-spike contact positions, even at low particle settings. */
    public static boolean woodMote(PoseStack pose,MultiBufferSource buffers,Vec3 camera,Vec3 point,Vec3 tail,
                                   QiElementLook look,double time,float fade){
        double distance=point.distanceTo(camera);
        if(distance>48)return false;
        float alpha=(float)(fade*Math.clamp((distance-.35)/.6,0,1)*Math.clamp((48-distance)/12,0,1));
        if(alpha<=0||QiVfxQuality.opacity()<=0)return false;
        var out=buffers.getBuffer(QiAuraMaterial.GLOW);var matrix=pose.last().pose();
        double spin=time*.07;
        Vec3 u=new Vec3(Math.cos(spin),0,Math.sin(spin));
        Vec3 v=new Vec3(-Math.sin(spin)*.25,1,Math.cos(spin)*.25).normalize();
        QiPressureMesh.ribbon(out,matrix,tail,point,camera,.045,look.color(),.52F*alpha);
        // Faint outer leaf, bright solid facets and a pale vein separate it from green terrain.
        leaf(out,matrix,camera,point,u,v,.43,.22,look,.16F*alpha*QiVfxQuality.opacity());
        leaf(out,matrix,camera,point,u,v,.30,.14,look,.94F*alpha*QiVfxQuality.opacity());
        QiPressureMesh.ribbon(out,matrix,point.subtract(v.scale(.24)),point.add(v.scale(.27)),camera,
                .022,0xF3FFD0,alpha);
        return true;
    }
    private static void leaf(VertexConsumer out,Matrix4f matrix,Vec3 camera,Vec3 p,Vec3 u,Vec3 v,
                             double height,double width,QiElementLook look,float alpha){
        Vec3 normal=u.cross(v).normalize();
        Vec3 top=p.add(v.scale(height)),bottom=p.subtract(v.scale(height*.8));
        Vec3[] rim={p.add(u.scale(width)),p.add(normal.scale(width*.55)),
                p.subtract(u.scale(width)),p.subtract(normal.scale(width*.55))};
        for(int i=0;i<4;i++){
            int color=i%2==0?look.color():look.highlight();
            polygon(out,matrix,camera,color,alpha,top,rim[i],bottom,rim[(i+1)%4]);
        }
    }
    private static void polygon(VertexConsumer out,Matrix4f matrix,Vec3 camera,int color,float alpha,Vec3... vertices){
        for(Vec3 v:vertices){Vec3 p=v.subtract(camera);out.addVertex(matrix,(float)p.x,(float)p.y,(float)p.z)
            .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha);}
    }
}
