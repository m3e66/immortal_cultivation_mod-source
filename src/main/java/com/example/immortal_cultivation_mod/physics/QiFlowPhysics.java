package com.example.immortal_cultivation_mod.physics;

import net.minecraft.world.phys.Vec3;

/** Bounded spring filaments: inertia, upward convection, curl and owner-motion drag. */
public final class QiFlowPhysics {
    public static final int NODES=11;
    private final Vec3[] position=new Vec3[NODES],previous=new Vec3[NODES],velocity=new Vec3[NODES];
    private Vec3 anchor;
    public QiFlowPhysics(Vec3 anchor) { reset(anchor); }
    private void reset(Vec3 origin) {
        anchor=origin;
        for(int i=0;i<NODES;i++){position[i]=previous[i]=origin.add(0,i*.23,0);velocity[i]=Vec3.ZERO;}
    }
    public void tick(Vec3 origin,double phase,double height,double spread,Vec3 movement) {
        if(!finite(origin)||!finite(movement)||!Double.isFinite(phase)||!Double.isFinite(height)||!Double.isFinite(spread))return;
        height=Math.clamp(height,.1,12);spread=Math.clamp(spread,0,4);
        if(anchor.distanceToSqr(origin)>64)reset(origin);
        Vec3 translation=origin.subtract(anchor);anchor=origin;
        for(int i=0;i<NODES;i++) {
            previous[i]=position[i];double t=i/(double)(NODES-1);
            double angle=phase+Math.sin(t*7-phase*1.3)*(.18+t*.36);
            double radius=spread*(.65+Math.sin(t*Math.PI)*.3)*(1-.4*t);
            Vec3 target=origin.add(Math.cos(angle)*radius,t*height,
                    Math.sin(angle)*radius);
            Vec3 curl=new Vec3(Math.sin(phase*1.4+t*9),.15,Math.cos(phase*1.2+t*8)).scale(.012*t);
            velocity[i]=velocity[i].scale(.69).add(target.subtract(position[i]).scale(.24)).add(curl)
                    .subtract(movement.scale(.035*t));
            if(velocity[i].lengthSqr()>.36)velocity[i]=velocity[i].normalize().scale(.6);
            position[i]=position[i].add(translation.scale(.42)).add(velocity[i]);
            if(position[i].distanceToSqr(target)>9)position[i]=target;
        }
    }
    public Vec3 point(int index,float partial){return previous[index].lerp(position[index],Math.clamp(partial,0,1));}
    public static boolean finite(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
}
