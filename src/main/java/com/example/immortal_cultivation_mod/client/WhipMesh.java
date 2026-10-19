package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.physics.WhipRope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/** Connected six-sided leather tube; joint rings are shared, so bending opens no gaps. */
public final class WhipMesh {
    private static final double LASH_THICKNESS_SCALE = .8;
    public static void render(WhipRope rope,Vec3 origin,Vec3 socket,PoseStack poses,VertexConsumer buffer,int light,int overlay,int color) {
        Vec3[] points=new Vec3[WhipRope.SEGMENTS+1];
        Vec3 shift=socket.subtract(rope.point(0));
        for(int i=0;i<points.length;i++)points[i]=rope.point(i).add(shift.scale(Math.exp(-i*.7)));
        render(rope,points,origin,poses,buffer,light,overlay,color);
    }
    public static void render(WhipRope rope,Vec3[] points,Vec3 origin,PoseStack poses,VertexConsumer buffer,int light,int overlay,int color) {
        Vec3[][] ring=new Vec3[WhipRope.SEGMENTS+1][6],normals=new Vec3[WhipRope.SEGMENTS+1][6];
        Vec3 side=new Vec3(1,0,0);
        for(int i=0;i<=WhipRope.SEGMENTS;i++) {
            Vec3 tangent=points[Math.min(WhipRope.SEGMENTS,i+1)].subtract(points[Math.max(0,i-1)]).normalize();
            side=side.subtract(tangent.scale(side.dot(tangent)));
            if(side.lengthSqr()<1e-8)side=tangent.cross(Math.abs(tangent.y)<.9?new Vec3(0,1,0):new Vec3(0,0,1));
            side=side.normalize();Vec3 up=tangent.cross(side).normalize();
            Vec3 center=points[i].subtract(origin);
            for(int j=0;j<6;j++) {
                double angle=j*Math.PI/3;
                normals[i][j]=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                ring[i][j]=center.add(normals[i][j].scale(rope.radius(i) * LASH_THICKNESS_SCALE));
            }
        }
        for(int i=0;i<WhipRope.SEGMENTS;i++)for(int j=0;j<6;j++) {
            int next=(j+1)%6;
            vertex(buffer,poses,ring[i][j],normals[i][j],88/190F,40/190F,light,overlay,color);
            vertex(buffer,poses,ring[i+1][j],normals[i+1][j],88/190F,43/190F,light,overlay,color);
            vertex(buffer,poses,ring[i+1][next],normals[i+1][next],91/190F,43/190F,light,overlay,color);
            vertex(buffer,poses,ring[i][next],normals[i][next],91/190F,40/190F,light,overlay,color);
        }
    }
    private static void vertex(VertexConsumer b,PoseStack p,Vec3 v,Vec3 n,float u,float w,int light,int overlay,int color) {
        b.addVertex(p.last(),(float)v.x,(float)v.y,(float)v.z).setColor(color).setUv(u,w).setOverlay(overlay).setLight(light)
                .setNormal(p.last(),(float)n.x,(float)n.y,(float)n.z);
    }
    private WhipMesh(){}
}
