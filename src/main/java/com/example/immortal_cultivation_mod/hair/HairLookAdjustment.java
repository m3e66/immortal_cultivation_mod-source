package com.example.immortal_cultivation_mod.hair;

import com.google.gson.JsonObject;
import java.util.*;
import org.joml.*;

/** Back hair stays downward as the head rotates; no spring or inertia. */
public final class HairLookAdjustment {
    private HairLookAdjustment() {}
    public static boolean isBackHair(String name){
        return name.startsWith("hair_back_") || name.equals("ponytail")
                || name.startsWith("ponytail_") || name.equals("braid");
    }
    public record Collider(Matrix4f inverse,Vector3f min,Vector3f max) {}
    public static Vector3f hangingRotation(float pitch,float yaw,float roll,Vector3f bind){
        return new Quaternionf().rotationZYX(roll,yaw,pitch).invert()
                .mul(new Quaternionf().rotationY(yaw))
                .mul(new Quaternionf().rotationZYX(bind.z,bind.y,bind.x))
                .getEulerAnglesZYX(new Vector3f());
    }
    /** The root is fixed to the head. Clearance may rotate a lock, never translate it. */
    public static Vector3f attachedRotation(List<Vector3f[]> boxes,Vector3f pivot,Vector3f hanging,Collider chest){
        var colliders=List.of(chest);
        var best=new Vector3f(hanging);
        float bestScore=penetration(boxes,transform(pivot.x,pivot.y,pivot.z,best.x,best.y,best.z),colliders);
        if(bestScore<.0001F)return best;
        // Bend backward in chest space, about the original attachment point.
        var axis=new Matrix4f(chest.inverse).invert().transformDirection(new Vector3f(1,0,0)).normalize();
        var base=new Quaternionf().rotationZYX(hanging.z,hanging.y,hanging.x);
        for(int degrees=2;degrees<=90;degrees+=2){
            var candidate=new Quaternionf().rotationAxis((float)java.lang.Math.toRadians(degrees),axis)
                    .mul(base).getEulerAnglesZYX(new Vector3f());
            float score=penetration(boxes,transform(pivot.x,pivot.y,pivot.z,candidate.x,candidate.y,candidate.z),colliders);
            if(score<bestScore){bestScore=score;best.set(candidate);}
            if(bestScore<.0001F)break;
        }
        return best;
    }
    public static Matrix4f transform(float x,float y,float z,float rx,float ry,float rz){
        return new Matrix4f().translation(x,y,z).rotateZYX(rz,ry,rx);
    }
    private static float number(JsonObject o,String key,int index){return o.getAsJsonArray(key).get(index).getAsFloat();}
    public static List<Vector3f[]> boxes(JsonObject part){
        var result=new ArrayList<Vector3f[]>();
        for(var value:part.getAsJsonArray("boxes")){
            var b=value.getAsJsonObject();var matrix=new Matrix4f();
            if(b.has("pivot"))matrix=transform(number(b,"pivot",0),number(b,"pivot",1),number(b,"pivot",2),
                    (float)java.lang.Math.toRadians(number(b,"rotation",0)),(float)java.lang.Math.toRadians(number(b,"rotation",1)),(float)java.lang.Math.toRadians(number(b,"rotation",2)));
            var points=new Vector3f[8];
            for(int i=0;i<8;i++)points[i]=matrix.transformPosition(new Vector3f(number(b,"from",0)+((i&1)==0?0:number(b,"size",0)),
                    number(b,"from",1)+((i&2)==0?0:number(b,"size",1)),number(b,"from",2)+((i&4)==0?0:number(b,"size",2))));
            result.add(points);
        }
        return result;
    }
    private record Hit(float depth) {}
    private static Hit hit(Vector3f[] box,Matrix4f part,Collider collider){
        var matrix=new Matrix4f(collider.inverse).mul(part);
        var center=matrix.transformPosition(new Vector3f(box[0]).lerp(box[7],.5F));
        Vector3f[] edges={matrix.transformDirection(new Vector3f(box[1]).sub(box[0]).mul(.5F)),
                matrix.transformDirection(new Vector3f(box[2]).sub(box[0]).mul(.5F)),
                matrix.transformDirection(new Vector3f(box[4]).sub(box[0]).mul(.5F))};
        var half=new Vector3f(collider.max).sub(collider.min).mul(.5F);
        center.sub(new Vector3f(collider.min).lerp(collider.max,.5F));
        float minimum=Float.POSITIVE_INFINITY;
        // Exact separating-axis test: a diagonal lock's bounding box is not its volume.
        for(int i=0;i<15;i++){
            Vector3f axis;
            if(i<3)axis=new Vector3f().setComponent(i,1);
            else if(i<6)axis=new Vector3f(edges[i-3]).cross(edges[(i-2)%3]);
            else axis=new Vector3f(edges[(i-6)/3]).cross(new Vector3f().setComponent((i-6)%3,1));
            if(axis.lengthSquared()<.000001F)continue;axis.normalize();
            float radius=java.lang.Math.abs(edges[0].dot(axis))+java.lang.Math.abs(edges[1].dot(axis))+java.lang.Math.abs(edges[2].dot(axis));
            float body=half.x*java.lang.Math.abs(axis.x)+half.y*java.lang.Math.abs(axis.y)+half.z*java.lang.Math.abs(axis.z);
            float overlap=radius+body-java.lang.Math.abs(center.dot(axis));
            if(overlap<=.001F)return null;minimum=java.lang.Math.min(minimum,overlap);
        }
        return new Hit(minimum);
    }
    public static float penetration(List<Vector3f[]> boxes,Matrix4f part,List<Collider> colliders){
        float result=0;for(var c:colliders)for(var box:boxes){var h=hit(box,part,c);if(h!=null)result+=h.depth*h.depth;}return result;
    }
}
