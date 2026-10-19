package com.example.immortal_cultivation_mod.physics;

import java.util.Arrays;
import net.minecraft.world.phys.Vec3;

/** Local elastic displacement field in world axes. The emitter is pinned; the free crown has inertia. */
public final class QiBodyFlamePhysics {
    public static final int NODES=13;
    private final Vec3[] offset=new Vec3[NODES],previous=new Vec3[NODES],velocity=new Vec3[NODES];
    private final Vec3[] target=new Vec3[NODES];
    private Vec3 anchor,movement=Vec3.ZERO;
    private double squeeze,oldSqueeze;
    public QiBodyFlamePhysics(){clear();}
    private void clear(){Arrays.fill(offset,Vec3.ZERO);Arrays.fill(previous,Vec3.ZERO);Arrays.fill(velocity,Vec3.ZERO);squeeze=oldSqueeze=0;}
    public void tick(Vec3 origin,Vec3 motion,Vec3 back,double airborne,double height,double time) {
        if(!QiFlowPhysics.finite(origin)||!QiFlowPhysics.finite(motion)||!QiFlowPhysics.finite(back)
                ||!Double.isFinite(airborne)||!Double.isFinite(height)||!Double.isFinite(time))return;
        height=Math.clamp(height,.1,12);airborne=Math.clamp(airborne,0,1);
        Vec3 translation=anchor==null?Vec3.ZERO:origin.subtract(anchor);
        if(anchor==null||translation.lengthSqr()>64){clear();translation=Vec3.ZERO;movement=motion;}
        anchor=origin;
        if(motion.lengthSqr()>9)motion=motion.normalize().scale(3);
        Vec3 acceleration=motion.subtract(movement);movement=motion;
        double wind=Math.clamp(motion.length()*.85+airborne*.5,0,1);
        Vec3 downstream=motion.lengthSqr()>1E-8?motion.normalize().scale(-1):Vec3.ZERO;
        Vec3 rear=back.lengthSqr()>1E-8?back.normalize():Vec3.ZERO;
        oldSqueeze=squeeze;squeeze+=(wind*.10-squeeze)*.32;
        for(int i=0;i<NODES;i++) {
            previous[i]=offset[i];double t=i/(double)(NODES-1),free=smooth(.18,1,t);
            // Crown loses upward buoyancy under fast airflow, while lower rows stay attached.
            Vec3 airflow=downstream.scale(height*wind*(.22+.55*airborne)).add(rear.scale(height*.10*(1-wind)));
            Vec3 convection=new Vec3(Math.sin(time*.13+t*6)*.012,Math.sin(time*.09+t*4)*.006,Math.cos(time*.11+t*5)*.012).scale(height*free*Math.sin(t*Math.PI));
            target[i]=airflow.scale(free).add(0,-height*Math.max(0,t-.45)*airborne*smooth(.42,.92,t),0).add(convection);
            // Hot gas piles up around the body instead of collapsing the entire cap below it.
            // Pressure vanishes at the free pole, so a climb still leaves a downward wake.
            double pressure=-Math.min(0,target[i].y)*1.5*Math.sqrt(Math.max(0,Math.sin(t*Math.PI)))*airborne;
            target[i]=target[i].add(0,pressure,0);
            offset[i]=offset[i].subtract(translation.scale(.11*free));
            velocity[i]=velocity[i].subtract(acceleration.scale(.22*free));
        }
        // Fixed substeps: frame rate never changes the spring response. Neighbour springs resist kinks.
        for(int sub=0;sub<3;sub++) {
            for(int i=0;i<NODES;i++) {
                if(i<=2){offset[i]=velocity[i]=Vec3.ZERO;continue;}
                Vec3 error=target[i].subtract(offset[i]);
                Vec3 curve=Vec3.ZERO;
                if(i<NODES-1)curve=offset[i-1].subtract(target[i-1]).add(offset[i+1].subtract(target[i+1])).subtract(offset[i].subtract(target[i]).scale(2));
                velocity[i]=velocity[i].add(error.scale(.28/3)).add(curve.scale(.12/3)).scale(Math.pow(.62,1.0/3));
            }
            for(int i=3;i<NODES;i++) {
                offset[i]=offset[i].add(velocity[i].scale(1.0/3));
                double limit=height*1.5*smooth(.15,1,i/(double)(NODES-1));
                if(offset[i].lengthSqr()>limit*limit){offset[i]=offset[i].normalize().scale(limit);velocity[i]=velocity[i].scale(.3);}
            }
        }
    }
    public Snapshot snapshot(float partial) {
        double blend=Float.isFinite(partial)?Math.clamp(partial,0,1):1;var points=new Vec3[NODES];
        for(int i=0;i<NODES;i++)points[i]=previous[i].lerp(offset[i],blend);
        return new Snapshot(points,oldSqueeze+(squeeze-oldSqueeze)*blend);
    }
    /** A frame-local view shared by every shell layer, particle and attached current. */
    public static final class Snapshot {
        private final Vec3[] offset;
        private final double squeeze;
        private Snapshot(Vec3[] offset,double squeeze){this.offset=offset;this.squeeze=squeeze;}
        public Vec3 offset(double t) {
            double row=Math.clamp(t,0,1)*(NODES-1);int i=Math.min(NODES-2,(int)row);
            return offset[i].lerp(offset[i+1],row-i);
        }
        public Vec3 deform(Vec3 point,double t) {
            double pinch=1-squeeze*smooth(.18,.85,t);
            return new Vec3(point.x*pinch,point.y,point.z*pinch).add(offset(t));
        }
    }
    private static double smooth(double from,double to,double t){t=Math.clamp((t-from)/(to-from),0,1);return t*t*(3-2*t);}
}
