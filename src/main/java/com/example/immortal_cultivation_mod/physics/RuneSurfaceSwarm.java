package com.example.immortal_cultivation_mod.physics;

import java.util.*;

/** Small independent ink bodies move in model-face coordinates, not camera or world coordinates. */
public final class RuneSurfaceSwarm {
    public record Seed(int id,double x,double y,double halfWidth,double halfHeight,double vx,double vy) { }
    public record Sample(double x,double y,double vx,double vy) { }
    private static final class Body {
        final int id;
        double x,y,oldX,oldY,rx,ry,vx,vy;
        Body(Seed s){id=s.id;x=oldX=s.x;y=oldY=s.y;vx=s.vx;vy=s.vy;rx=s.halfWidth;ry=s.halfHeight;}
    }
    private final Map<Integer,Body> bodies=new TreeMap<>();
    private double width,height;
    private long tick=Long.MIN_VALUE;
    public long collisions,wallBounces;
    public RuneSurfaceSwarm(double width,double height){this.width=width;this.height=height;}
    public int size(){return bodies.size();}
    public long fingerprint(){long value=1;for(var b:bodies.values())value=value*31+Double.doubleToLongBits(b.x)+Double.doubleToLongBits(b.y);return value;}
    public void sync(double width,double height,List<Seed> seeds) {
        if(!Double.isFinite(width)||!Double.isFinite(height)||width<=0||height<=0)return;
        if(Math.abs(this.width-width)>.002||Math.abs(this.height-height)>.002) {
            for(var b:bodies.values()){b.x*=width/this.width;b.oldX*=width/this.width;b.y*=height/this.height;b.oldY*=height/this.height;}
            this.width=width;this.height=height;
        }
        var retained=new HashSet<Integer>();
        for(var s:seeds) {
            if(retained.size()>=144)break;
            if(!Double.isFinite(s.x+s.y+s.halfWidth+s.halfHeight+s.vx+s.vy)||s.halfWidth<=0||s.halfHeight<=0)continue;
            retained.add(s.id);var b=bodies.computeIfAbsent(s.id,ignored->new Body(s));
            b.rx=Math.min(width*.49,s.halfWidth);b.ry=Math.min(height*.49,s.halfHeight);bound(b);
            b.oldX=Math.clamp(b.oldX,b.rx,width-b.rx);b.oldY=Math.clamp(b.oldY,b.ry,height-b.ry);
        }
        bodies.keySet().retainAll(retained);
    }
    public Sample sample(int id,float partial) {
        var b=bodies.get(id);if(b==null)return null;
        double t=Math.clamp(partial,0,1);
        return new Sample(b.oldX+(b.x-b.oldX)*t,b.oldY+(b.y-b.oldY)*t,b.vx,b.vy);
    }
    /** A render-rate-independent update; repeated views of the same entity never advance time. */
    public void tick(long now) {
        if(tick==now)return;
        if(tick!=Long.MIN_VALUE&&(now<tick||now-tick>40)){for(var b:bodies.values()){b.oldX=b.x;b.oldY=b.y;}tick=now;return;}
        tick=now;
        var list=new ArrayList<>(bodies.values());
        for(var b:list){b.oldX=b.x;b.oldY=b.y;
            double phase=(b.id&65535)*.017;
            b.vx+=Math.sin(now*.13+phase)*.00010;b.vy+=Math.cos(now*.11-phase)*.00010;
            double speed=Math.hypot(b.vx,b.vy);if(speed>.009){b.vx*=.009/speed;b.vy*=.009/speed;}
        }
        double cell=.012;for(var b:list)cell=Math.max(cell,Math.max(b.rx,b.ry)*2+.0001);
        for(int substep=0;substep<2;substep++) {
            for(var b:list){b.x+=b.vx*.5;b.y+=b.vy*.5;bound(b);}
            for(int pass=0;pass<3;pass++) {
                var grid=new HashMap<Long,List<Integer>>();
                for(int i=0;i<list.size();i++){var b=list.get(i);grid.computeIfAbsent(key((int)Math.floor(b.x/cell),(int)Math.floor(b.y/cell)),ignored->new ArrayList<>()).add(i);}
                for(int i=0;i<list.size();i++) {
                    var a=list.get(i);int x=(int)Math.floor(a.x/cell),y=(int)Math.floor(a.y/cell);
                    for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++) {
                        var near=grid.get(key(x+dx,y+dy));if(near==null)continue;
                        for(int j:near)if(j>i)collide(a,list.get(j));
                    }
                }
                for(var b:list)bound(b);
            }
        }
    }
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
    private void collide(Body a,Body b) {
        double dx=b.x-a.x,dy=b.y-a.y,px=a.rx+b.rx-Math.abs(dx),py=a.ry+b.ry-Math.abs(dy);
        if(px<=0||py<=0)return;
        boolean horizontal=px<py;double normal=(horizontal?dx:dy)>=0?1:-1;
        double shift=((horizontal?px:py)+.000001)*.5;
        double relative=(horizontal?b.vx-a.vx:b.vy-a.vy)*normal;
        if(horizontal){a.x-=normal*shift;b.x+=normal*shift;}else{a.y-=normal*shift;b.y+=normal*shift;}
        if(relative<0){double impulse=-relative*.88;if(horizontal){a.vx-=normal*impulse;b.vx+=normal*impulse;}else{a.vy-=normal*impulse;b.vy+=normal*impulse;}collisions++;}
    }
    private void bound(Body b) {
        double x=Math.clamp(b.x,b.rx,width-b.rx),y=Math.clamp(b.y,b.ry,height-b.ry);
        if(x!=b.x){if(b.x<b.rx&&b.vx<0||b.x>width-b.rx&&b.vx>0)b.vx*=-.90;wallBounces++;b.x=x;}
        if(y!=b.y){if(b.y<b.ry&&b.vy<0||b.y>height-b.ry&&b.vy>0)b.vy*=-.90;wallBounces++;b.y=y;}
    }
}
