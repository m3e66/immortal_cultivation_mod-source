package com.example.immortal_cultivation_mod.physics;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared server/client elastic sphere geometry. Contact planes partition actual influence. */
public record DomainVolume(Vec3 center,List<Cut> cuts) {
    public static final double RADIUS=20;
    public static final int CLASH_DURATION_TICKS=80;
    public static final int CLASH_RECOIL_PERIOD_TICKS=40;
    public record Cut(Vec3 normal,double offset) { }
    public DomainVolume {cuts=cuts.stream().sorted(java.util.Comparator.comparingDouble((Cut c)->c.normal.x)
            .thenComparingDouble(c->c.normal.y).thenComparingDouble(c->c.normal.z).thenComparingDouble(Cut::offset)).toList();}

    /** The two sides use one common plane. A damped spring gives compression a soft recoil. */
    public static Cut contact(Vec3 own,Vec3 rival,double age,int advantage,int stableSide) {
        Vec3 delta=rival.subtract(own);
        double distance=delta.length();
        Vec3 normal=distance>1E-6?delta.scale(1/distance):new Vec3(stableSide,0,0);
        double t=Math.clamp(age/CLASH_DURATION_TICKS,0,1);
        double spring=1-Math.exp(-5*t)*(Math.cos(9*t)+5.0/9*Math.sin(9*t));
        double bias=Math.min(distance*.38,Math.max(0,RADIUS-distance*.5));
        // Opposite stable sides give both owners exactly the same moving contact plane.
        // The contest keeps recoiling instead of becoming motionless after its first squeeze.
        double onset=Math.clamp(age/12,0,1);onset=onset*onset*(3-2*onset);
        double recoil=onset*(Math.sin(age*Math.PI/4)*.22+Math.sin(age*Math.PI/10)*.20);
        double offset=distance*.5+Math.signum(advantage)*bias*Math.clamp(spring,0,1)+stableSide*recoil;
        return new Cut(normal,Math.max(0,Math.min(RADIUS,offset)));
    }

    /** Keep tied contests synchronized indefinitely without enlarging the one-byte age packet. */
    public static int clashVisualAge(long age) {
        age=Math.max(0,age);
        return age<CLASH_DURATION_TICKS?(int)age:CLASH_DURATION_TICKS+(int)((age-CLASH_DURATION_TICKS)%CLASH_RECOIL_PERIOD_TICKS);
    }

    public double radius(Vec3 direction) {
        double radius=RADIUS;
        for(Cut cut:cuts) {
            double dot=direction.dot(cut.normal);
            if(dot<=1E-8)continue;
            double plane=Math.max(0,cut.offset-.025)/dot;
            // Round the cap's shoulder without ever crossing the shared contact plane.
            double h=Math.max(.45-Math.abs(radius-plane),0)/.45;
            radius=Math.max(0,Math.min(radius,plane)-h*h*.45*.25);
        }
        return radius;
    }
    public Vec3 surface(Vec3 direction) {return center.add(direction.scale(radius(direction)));}

    /** Travelling membrane shudders stay inside the authoritative compressed volume. */
    public Vec3 vibratingSurface(Vec3 direction,double time) {
        if(cuts.isEmpty())return surface(direction);
        double inset=0;
        for(var cut:cuts) {
            double angle=Math.acos(Math.clamp(direction.dot(cut.normal),-1,1));
            double wave=.5+.5*Math.sin(time*1.7-angle*13);
            double beat=.65+.35*Math.sin(time*.43);
            inset=Math.max(inset,(.12+.48*wave)*beat*(.45+.55*(1-angle/Math.PI)));
        }
        return center.add(direction.scale(Math.max(0,radius(direction)-inset)));
    }
    public boolean contains(Vec3 point) {
        Vec3 delta=point.subtract(center);double length=delta.length();
        return length<1E-8||length<=radius(delta.scale(1/length))+1E-7;
    }
    /** Clip a visible segment to the same sphere and contact half-spaces as gameplay. */
    public Vec3[] clip(Vec3 from,Vec3 to) {
        Vec3 direction=to.subtract(from),relative=from.subtract(center);
        double a=direction.lengthSqr();if(a<1E-12)return contains(from)?new Vec3[]{from,to}:null;
        double b=relative.dot(direction),c=relative.lengthSqr()-RADIUS*RADIUS,disc=b*b-a*c;
        if(disc<0)return null;
        double low=Math.max(0,(-b-Math.sqrt(disc))/a),high=Math.min(1,(-b+Math.sqrt(disc))/a);
        for(var cut:cuts) {
            double speed=direction.dot(cut.normal),distance=cut.offset-.025-relative.dot(cut.normal);
            if(Math.abs(speed)<1E-10){if(distance<0)return null;continue;}
            if(speed>0)high=Math.min(high,distance/speed);else low=Math.max(low,distance/speed);
        }
        if(low>high)return null;
        double middle=(low+high)*.5;
        if(!contains(from.lerp(to,middle)))return null;
        if(!contains(from.lerp(to,low))) {double l=low,r=middle;for(int i=0;i<12;i++){double t=(l+r)*.5;if(contains(from.lerp(to,t)))r=t;else l=t;}low=r;}
        if(!contains(from.lerp(to,high))) {double l=middle,r=high;for(int i=0;i<12;i++){double t=(l+r)*.5;if(contains(from.lerp(to,t)))l=t;else r=t;}high=l;}
        return new Vec3[]{from.lerp(to,low),from.lerp(to,high)};
    }
    public boolean intersects(AABB box) {
        Vec3 nearest=new Vec3(Math.clamp(center.x,box.minX,box.maxX),Math.clamp(center.y,box.minY,box.maxY),Math.clamp(center.z,box.minZ,box.maxZ));
        if(contains(nearest)||contains(box.getCenter()))return true;
        for(int i=0;i<8;i++)if(contains(new Vec3((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ)))return true;
        return false;
    }
}
