package com.example.immortal_cultivation_mod.physics;

import net.minecraft.world.phys.Vec3;

/** Fixed-step, tapered-mass rope. Only the handle socket and its tangent are driven. */
public final class WhipRope {
    public static final int SEGMENTS=32;
    public static final double LENGTH=4.60,STEP=1.0/120.0,SEGMENT=LENGTH/SEGMENTS;
    @FunctionalInterface public interface Contact { Vec3 project(Vec3 from,Vec3 to,double radius); }
    public static final Contact AIR=(from,to,radius)->to;
    private final double segmentLength,rootRadius,tipRadius,bendRoot,bendTip,tipWeight;
    private final Vec3[] points=new Vec3[SEGMENTS+1],previous=new Vec3[SEGMENTS+1];
    private Vec3 lastAnchor,lastDirection;
    private double clock=Double.NaN,accumulator;
    public Vec3 point(int i){return points[i];}
    public Vec3 predictedPoint(int i){return points[i].add(points[i].subtract(previous[i]).scale(12));}
    public boolean initialized(){return lastAnchor!=null;}
    public WhipRope(){this(LENGTH,.058725,.014175,1.997,1.912,7);}
    public static WhipRope spiritSilk(double scale) {
        return new WhipRope(44/16D*scale,.03*scale,.012*scale,1.99,1.9,5);
    }
    /** The same fixed-step solver also drives silk lashes and weighted metal chains. */
    public WhipRope(double length,double rootRadius,double tipRadius,double bendRoot,double bendTip,double tipWeight) {
        this.segmentLength=length/SEGMENTS;this.rootRadius=rootRadius;this.tipRadius=tipRadius;
        this.bendRoot=bendRoot;this.bendTip=bendTip;this.tipWeight=tipWeight;
    }
    public double length(){return segmentLength*SEGMENTS;}
    public double radius(int i){return rootRadius+(tipRadius-rootRadius)*Math.pow(i/(double)SEGMENTS,.75);}
    private double weight(int i){return i<2?0:1+(tipWeight-1)*Math.pow(i/(double)SEGMENTS,2);}

    /** The hand supports the stored coil, so idle movement must not accumulate free-lash velocity. */
    public void hold(double time,Vec3[] coil) {
        System.arraycopy(coil,0,points,0,points.length);
        System.arraycopy(coil,0,previous,0,previous.length);
        clock=time;accumulator=0;lastAnchor=points[0];lastDirection=points[1].subtract(points[0]).normalize();
    }

    public void reset(double time,Vec3 anchor,Vec3 direction) {
        direction=direction.normalize();clock=time;accumulator=0;lastAnchor=anchor;lastDirection=direction;
        points[0]=anchor;points[1]=anchor.add(direction.scale(segmentLength));
        // Start slack above the floor, rather than spawning a six-block rod through terrain.
        for(int i=2;i<=SEGMENTS;i++) {
            double angle=(i-2)*.23;
            points[i]=points[i-1].add(new Vec3(Math.cos(angle),-.035,Math.sin(angle)).normalize().scale(segmentLength));
        }
        System.arraycopy(points,0,previous,0,points.length);
    }

    public void advance(double time,Vec3 anchor,Vec3 direction,Contact contact) {
        if(!finite(anchor) || !finite(direction) || !Double.isFinite(time))return;
        direction=direction.lengthSqr()<1e-8?new Vec3(0,1,0):direction.normalize();
        double elapsed=time-clock;
        if(!initialized() || elapsed<0 || elapsed>.25 || anchor.distanceToSqr(lastAnchor)>36) {
            reset(time,anchor,direction);return;
        }
        if(elapsed<=0)return;
        double pending=accumulator+elapsed;
        double first=STEP-accumulator;
        for(double offset=first;offset<=elapsed+1e-9;offset+=STEP) {
            double alpha=Math.clamp(offset/elapsed,0,1);
            Vec3 socket=lastAnchor.lerp(anchor,alpha),tangent=lastDirection.lerp(direction,alpha).normalize();
            step(socket,tangent,contact);
            pending-=STEP;
        }
        accumulator=Math.max(0,pending);clock=time;lastAnchor=anchor;lastDirection=direction;
    }

    private void step(Vec3 anchor,Vec3 direction,Contact contact) {
        Vec3[] starts=points.clone();
        for(int i=2;i<=SEGMENTS;i++) {
            Vec3 velocity=points[i].subtract(previous[i]).scale(Math.exp(-4.6*STEP));
            if(velocity.lengthSqr()>Math.pow(80*STEP,2))velocity=velocity.normalize().scale(80*STEP);
            previous[i]=points[i];points[i]=points[i].add(velocity).add(0,-9.81*STEP*STEP,0);
        }
        points[0]=anchor;points[1]=anchor.add(direction.scale(segmentLength));
        // Short silk segments and a compact chain need more tension passes than a long lash.
        // Keep the same timestep and contact projection so its extra mass cannot stretch the links.
        int iterations=tipWeight<1?96:Math.clamp((int)Math.ceil(40*LENGTH/length()),40,96);
        for(int iteration=0;iteration<iterations;iteration++) {
            if((iteration&1)==0)for(int i=1;i<=SEGMENTS;i++)distance(i-1,i,segmentLength,false);
            else for(int i=SEGMENTS;i>=1;i--)distance(i-1,i,segmentLength,false);
            // Thick leather resists tight folds, especially near the handle; the tip stays supple.
            if(iteration%2==0)for(int i=1;i<SEGMENTS;i++) {
                double bendSpan=bendRoot+(bendTip-bendRoot)*i/(double)SEGMENTS;
                distance(i-1,i+1,segmentLength*bendSpan,true);
            }
            if(iteration%8==7)for(int i=2;i<=SEGMENTS;i++) {
                Vec3 resolved=contact.project(starts[i],points[i],radius(i));
                if(!resolved.equals(points[i])) {
                    points[i]=resolved;
                    previous[i]=resolved.subtract(resolved.subtract(starts[i]).scale(.25));
                }
            }
        }
        previous[0]=points[0];previous[1]=points[1];
        for(Vec3 point:points)if(!finite(point)){reset(clock,anchor,direction);break;}
    }
    private void distance(int a,int b,double rest,boolean minimumOnly) {
        Vec3 delta=points[b].subtract(points[a]);double length=delta.length(),wa=weight(a),wb=weight(b);
        if(length<1e-9 || wa+wb==0 || minimumOnly && length>=rest)return;
        Vec3 correction=delta.scale((length-rest)/(length*(wa+wb)));
        if(wa>0)points[a]=points[a].add(correction.scale(wa));
        if(wb>0)points[b]=points[b].subtract(correction.scale(wb));
    }
    public double maximumStretch() {
        if(!initialized())return 0;
        double max=0;for(int i=1;i<=SEGMENTS;i++)max=Math.max(max,Math.abs(points[i].distanceTo(points[i-1])/segmentLength-1));return max;
    }
    private static boolean finite(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
}
