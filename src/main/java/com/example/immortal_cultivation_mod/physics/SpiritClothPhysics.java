package com.example.immortal_cultivation_mod.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Pinned Verlet chains for the puppet's cloth, independent of the renderer. */
public final class SpiritClothPhysics {
    public static final double STEP=1.0/120;
    public record Point(double x,double y,double z) {
        public Point add(Point p){return new Point(x+p.x,y+p.y,z+p.z);}
        public Point subtract(Point p){return new Point(x-p.x,y-p.y,z-p.z);}
        public Point scale(double s){return new Point(x*s,y*s,z*s);}
        public double dot(Point p){return x*p.x+y*p.y+z*p.z;}
        public double length(){return Math.sqrt(dot(this));}
        public boolean finite(){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z);}
        public Point mix(Point p,double t){return scale(1-t).add(p.scale(t));}
    }
    public record Capsule(Point start,Point end,double radius) {
        public Point project(Point p,double clothRadius) {
            Point axis=end.subtract(start);
            double t=Math.clamp(p.subtract(start).dot(axis)/Math.max(1e-12,axis.dot(axis)),0,1);
            Point closest=start.add(axis.scale(t)),offset=p.subtract(closest);
            double distance=offset.length(),minimum=radius+clothRadius;
            if(distance>=minimum)return p;
            return closest.add((distance<1e-8?new Point(0,0,1):offset.scale(1/distance)).scale(minimum));
        }
    }
    private static final class Chain {
        final Point[] positions,previous;
        final double[] lengths;
        final double radius;
        Chain(Point[] rest,double radius) {
            positions=rest.clone();previous=rest.clone();lengths=new double[rest.length-1];this.radius=radius;
            for(int i=0;i<lengths.length;i++)lengths[i]=rest[i+1].subtract(rest[i]).length();
        }
    }
    private final List<Chain> chains=new ArrayList<>();
    private double accumulator;
    public SpiritClothPhysics(List<Point[]> rest,List<Double> radii) {
        if(rest.size()!=radii.size())throw new IllegalArgumentException("Cloth layout mismatch");
        for(int i=0;i<rest.size();i++)chains.add(new Chain(rest.get(i),radii.get(i)));
    }
    public void reset(List<Point[]> rest) {
        for(int c=0;c<chains.size();c++)for(int i=0;i<chains.get(c).positions.length;i++)
            chains.get(c).positions[i]=chains.get(c).previous[i]=rest.get(c)[i];
        accumulator=0;
    }
    /** Preserve world-space inertia when the entity moves or turns. */
    public void rebase(UnaryOperator<Point> oldToNew) {
        for(Chain c:chains)for(int i=0;i<c.positions.length;i++) {
            c.positions[i]=oldToNew.apply(c.positions[i]);c.previous[i]=oldToNew.apply(c.previous[i]);
        }
    }
    public void impulse(Point velocity) {
        for(Chain c:chains)for(int i=1;i<c.positions.length;i++)
            c.previous[i]=c.previous[i].subtract(velocity.scale(STEP*i/(c.positions.length-1)));
    }
    public void advance(double seconds,List<Point[]> rest,List<Capsule> bodies,Point wind,boolean submerged) {
        if(!Double.isFinite(seconds)||seconds<0||seconds>.35){reset(rest);return;}
        accumulator+=seconds;
        int steps=Math.min(42,(int)Math.floor((accumulator+1e-12)/STEP));accumulator-=steps*STEP;
        double damping=Math.exp(-(submerged?18:3.2)*STEP);
        for(int step=0;step<steps;step++)for(int ci=0;ci<chains.size();ci++) {
            Chain c=chains.get(ci);Point[] target=rest.get(ci);
            Point anchor=c.positions[0].mix(target[0],1.0/(steps-step));c.positions[0]=c.previous[0]=anchor;
            for(int i=1;i<c.positions.length;i++) {
                Point p=c.positions[i],velocity=p.subtract(c.previous[i]).scale(damping);
                Point force=new Point(0,submerged?-35:-190,0).add(wind).add(target[i].subtract(p).scale(2));
                c.previous[i]=p;c.positions[i]=p.add(velocity).add(force.scale(STEP*STEP));
            }
            for(int iteration=0;iteration<20;iteration++) {
                c.positions[0]=anchor;
                for(int i=0;i<c.lengths.length;i++) {
                    Point a=c.positions[i],b=c.positions[i+1],delta=b.subtract(a);
                    if(delta.length()<1e-9)delta=target[i+1].subtract(target[i]);
                    double length=Math.max(1e-9,delta.length());Point correction=delta.scale((length-c.lengths[i])/length);
                    if(i==0)c.positions[i+1]=b.subtract(correction);
                    else {c.positions[i]=a.add(correction.scale(.5));c.positions[i+1]=b.subtract(correction.scale(.5));}
                }
                for(int i=1;i<c.positions.length;i++) {
                    Point p=c.positions[i];for(Capsule body:bodies)p=body.project(p,c.radius);
                    c.positions[i]=p.y<.12?new Point(p.x,.12,p.z):p;
                }
            }
            c.positions[0]=anchor;
            if(!finite(c))for(int i=0;i<target.length;i++)c.positions[i]=c.previous[i]=target[i];
        }
    }
    private static boolean finite(Chain c){for(Point p:c.positions)if(!p.finite())return false;return true;}
    public Point point(int chain,int node){return chains.get(chain).positions[node];}
    public double maximumLengthError() {
        double worst=0;for(Chain c:chains)for(int i=0;i<c.lengths.length;i++)
            worst=Math.max(worst,Math.abs(c.positions[i+1].subtract(c.positions[i]).length()-c.lengths[i]));return worst;
    }
    public boolean finite(){return chains.stream().allMatch(SpiritClothPhysics::finite);}
}
