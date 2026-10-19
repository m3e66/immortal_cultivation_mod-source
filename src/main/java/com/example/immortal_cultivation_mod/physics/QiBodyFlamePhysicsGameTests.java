package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.client.qi.QiEnergyShape;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class QiBodyFlamePhysicsGameTests {
    private static final Vec3 BACK=new Vec3(0,0,-1);
    private static QiBodyFlamePhysics settle(Vec3 motion,double flight) {
        var physics=new QiBodyFlamePhysics();
        for(int i=0;i<60;i++)physics.tick(motion.scale(i),motion,BACK,flight,2,i);
        return physics;
    }
    @GameTest(template="fate_test") public static void movingFlameBendsInsteadOfRigidlyRotatingItsBase(GameTestHelper h) {
        var physics=settle(new Vec3(.8,0,0),1);var shape=physics.snapshot(1);
        h.assertTrue(shape.offset(0).equals(Vec3.ZERO)&&shape.offset(.16).equals(Vec3.ZERO),"Bottom emitter rows remain pinned in world axes");
        Vec3 crown=shape.deform(new Vec3(0,2,0),1),torso=shape.deform(new Vec3(0,.9,0),.45);
        h.assertTrue(crown.x<-.8&&Math.abs(crown.y-.9)<.2,"Free crown bends behind fast flight: "+crown);
        h.assertTrue(Math.abs(torso.x)<Math.abs(crown.x)*.4,"Torso holds its position while crown deforms");
        h.assertTrue(Math.abs(crown.distanceTo(torso)-1.1)>.05,"Changing surface distances prove deformation rather than a rigid rotation");h.succeed();
    }
    @GameTest(template="fate_test") public static void turnRetainsInertiaThenSettlesDownstream(GameTestHelper h) {
        var physics=settle(new Vec3(.8,0,0),1);var before=physics.snapshot(1).offset(1);
        physics.tick(new Vec3(.8*59,0,0),new Vec3(0,0,.8),BACK,1,2,60);var first=physics.snapshot(1).offset(1);
        h.assertTrue(first.x<-.3&&Math.abs(first.z)<1,"Turn does not instantly snap whole mesh onto new travel direction");
        for(int i=1;i<=45;i++)physics.tick(new Vec3(.8*59,0,i*.8),new Vec3(0,0,.8),BACK,1,2,60+i);
        var after=physics.snapshot(1).offset(1);
        h.assertTrue(after.z<-1&&Math.abs(after.x)<.03,"Elastic wake settles behind new movement: "+after);
        h.assertTrue(before.distanceTo(after)>1,"The displacement field really changes across the turn");h.succeed();
    }
    @GameTest(template="fate_test") public static void climbsAndDivesDeformInAllThreeAxes(GameTestHelper h) {
        for(var motion:new Vec3[]{new Vec3(.7,.6,0),new Vec3(.7,-.6,0),new Vec3(0,.8,0),new Vec3(0,-.8,0)}) {
            var shape=settle(motion,1).snapshot(1);Vec3 tail=new Vec3(0,1.1,0).add(shape.offset(1));
            h.assertTrue(tail.dot(motion)<-.2,"Crown follows three-dimensional airflow, including pure vertical travel: "+motion+" / "+tail);
            h.assertTrue(shape.offset(.1).equals(Vec3.ZERO),"Diving never rotates or detaches the emitter");
            if(motion.y>0)h.assertTrue(shape.deform(new Vec3(0,1.5,0),.75).y>1.6,"Body pressure preserves the upper envelope while its free crown trails downward");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void stoppingRecoversWithDampingAndSnapshotsDoNotAdvanceSimulation(GameTestHelper h) {
        var physics=settle(new Vec3(.8,0,0),1);Vec3 place=new Vec3(.8*59,0,0),old=physics.snapshot(1).offset(1);
        physics.tick(place,Vec3.ZERO,BACK,0,2,60);var saved=physics.snapshot(1).offset(.8);
        for(int i=0;i<500;i++)physics.snapshot(i/500F).deform(new Vec3(.2,1.6,.3),.8);
        h.assertTrue(saved.equals(physics.snapshot(1).offset(.8)),"Rendering many interpolation samples never advances simulation");
        h.assertTrue(physics.snapshot(1).offset(1).x<-.5,"Stopping retains a brief physical wake");
        for(int i=1;i<=80;i++)physics.tick(place,Vec3.ZERO,BACK,0,2,60+i);
        var after=physics.snapshot(1).offset(1);
        h.assertTrue(Math.abs(after.x)<.005&&Math.abs(after.y)<.005&&Math.abs(after.z+.2)<.005,"Damping returns flame to quiet rearward curl: "+after);
        h.assertTrue(old.distanceTo(after)>1,"Airflow wake disappears after recovery");h.succeed();
    }
    @GameTest(template="fate_test") public static void allMethodSurfacesStayClosedAndFiniteWhileDeforming(GameTestHelper h) {
        var shape=settle(new Vec3(.8,-.45,.25),1).snapshot(.7F);
        for(int art=0;art<17;art++)for(double t:new double[]{0,.1,.3,.5,.7,.9,1}) {
            Vec3 first=null;
            for(int j=0;j<40;j++) {
                Vec3 source=QiEnergyShape.point(j*Math.PI/20,t,137,.3F,Vec3.ZERO,art).multiply(.8,2,.8);
                Vec3 point=shape.deform(source,t);
                h.assertTrue(QiFlowPhysics.finite(point)&&point.length()<7,"Every art remains finite and body-sized");
                if(first==null)first=point;else if(t==0||t==1)h.assertTrue(first.distanceTo(point)<1E-7,"Deformation preserves both closed poles for every art");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void teleportAndMalformedMotionCannotCreateStretchedOrNanFlames(GameTestHelper h) {
        var physics=settle(new Vec3(.8,0,0),1);
        physics.tick(new Vec3(10000,120,-7000),Vec3.ZERO,BACK,0,2,80);
        h.assertTrue(physics.snapshot(1).offset(1).length()<.1,"Teleport discards old wake rather than stretching a long connection");
        var clean=physics.snapshot(1).offset(1);physics.tick(new Vec3(Double.NaN,0,0),Vec3.ZERO,BACK,0,2,81);
        h.assertTrue(physics.snapshot(1).offset(1).equals(clean),"Invalid input does not poison saved physics state");
        for(int i=0;i<2000;i++) {
            double sign=i%2==0?1:-1;physics.tick(new Vec3(i*.25,120,0),new Vec3(sign*2.5,sign*2,.8),BACK,1,2,i);
            for(int node=0;node<QiBodyFlamePhysics.NODES;node++) {
                var offset=physics.snapshot(1).offset(node/(double)(QiBodyFlamePhysics.NODES-1));
                h.assertTrue(QiFlowPhysics.finite(offset)&&offset.length()<=3.00001,"Extreme repeated reversals stay bounded");
            }
        }h.succeed();
    }
}
