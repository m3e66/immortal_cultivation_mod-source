package com.example.immortal_cultivation_mod.physics;

import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Rest geometry and release/return timing shared by held views. */
public final class WhipHeldPose {
    public static double deployment(double sinceStrike,double releaseFrom) {
        if(sinceStrike<0 || sinceStrike>=1.05)return 0;
        if(sinceStrike<.12)return releaseFrom+(1-releaseFrom)*smooth(sinceStrike/.12);
        if(sinceStrike<=.65)return 1;
        return 1-smooth((sinceStrike-.65)/.4);
    }

    /** Resample the authored coil by arc length and match the physical lash's length. */
    public static Vec3[] coil(List<Vec3> authored) {
        return coil(authored,WhipRope.LENGTH);
    }
    public static Vec3[] coil(List<Vec3> authored,double length) {
        double[] distances=new double[authored.size()];
        for(int i=1;i<distances.length;i++)distances[i]=distances[i-1]+authored.get(i).distanceTo(authored.get(i-1));
        double total=distances[distances.length-1];
        Vec3 root=authored.getFirst();
        Vec3[] result=new Vec3[WhipRope.SEGMENTS+1];
        int edge=1;
        for(int i=0;i<result.length;i++) {
            double d=total*i/WhipRope.SEGMENTS;
            while(edge<distances.length-1 && distances[edge]<d)edge++;
            double fraction=(d-distances[edge-1])/Math.max(1e-9,distances[edge]-distances[edge-1]);
            result[i]=root.add(authored.get(edge-1).lerp(authored.get(edge),fraction).subtract(root).scale(length/total));
        }
        return result;
    }
    private static double smooth(double x){return x*x*(3-2*x);}
    private WhipHeldPose(){}
}
