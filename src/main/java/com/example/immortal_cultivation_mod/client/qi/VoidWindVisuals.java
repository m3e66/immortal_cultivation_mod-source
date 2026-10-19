package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.client.QiDomainDistortion;
import com.example.immortal_cultivation_mod.combat.MethodDomainStyle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Brief mint-white crossing cuts, emitted only by confirmed server hit events. */
public final class VoidWindVisuals {
    public static long domainFrames,slashFrames,afterimageFrames,impactFrames,auraFrames;
    private static final int MINT=MethodDomainStyle.WIND.color,WHITE=MethodDomainStyle.WIND.secondary;
    private VoidWindVisuals() { }

    /** No ambient ribbons: keeping the aura open is not a cutting event. */
    public static void aura(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,double time,float alpha) { }

    /** The ordinary mint domain border is separate; empty space has no wind currents or floor haze. */
    public static void domain(VertexConsumer out,Matrix4f m,LivingEntity owner,Vec3 p,Vec3 camera,double time,float alpha) { }

    private static void slash(VertexConsumer out,Matrix4f m,Vec3 a,Vec3 b,Vec3 camera,float alpha,double scale) {
        // Taper each confirmed cut to a razor tip, with a pale core and the original mint edge.
        for(int i=0;i<6;i++) {
            double t=i/6.,u=(i+1)/6.,taper=Math.pow(Math.sin((t+u)*.5*Math.PI),.65);
            Vec3 p=a.lerp(b,t),q=a.lerp(b,u);
            QiPressureMesh.ribbon(out,m,p,q,camera,.095*taper*scale,MINT,alpha*.16F);
            QiPressureMesh.ribbon(out,m,p,q,camera,.045*taper*scale,MINT,alpha*.42F);
            QiPressureMesh.ribbon(out,m,p,q,camera,.013*taper*scale,WHITE,alpha*.92F);
        }
    }
    public static void impact(VertexConsumer out,Matrix4f m,Vec3 p,Vec3 normal,Vec3 camera,double age,float power) {
        impactFrames++;Vec3 n=normal.lengthSqr()<.01?new Vec3(0,1,0):normal.normalize();
        Vec3 u=n.cross(Math.abs(n.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=n.cross(u).normalize();
        // Confirmed hits receive a rapid sequence of crossing cuts rather than one lingering mark.
        for(int j=0;j<6;j++) {
            double t=age-j*2;if(t<0||t>5)continue;
            double angle=Math.PI*.16+j*2.399,length=(.65+power*.5)*(1+t*.045);
            Vec3 axis=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle))).scale(length);
            Vec3 center=p.add(n.scale(j*.035)).add(v.scale(Math.sin(j*2.4)*.30));
            float fade=(float)Math.pow(1-t/5,1.2);
            Vec3 start=center.subtract(axis),end=center.add(axis);
            slash(out,m,start,end,camera,fade,1.25);
            if(j%2==0)QiDomainDistortion.windSeam(start,end,fade*.7F);
            slashFrames++;
        }
    }
}
