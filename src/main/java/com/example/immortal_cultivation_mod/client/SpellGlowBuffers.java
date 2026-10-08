package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Flight-style light is the primary layer; original quads remain as faint supporting detail. */
public final class SpellGlowBuffers {
    private SpellGlowBuffers() {}
    private static boolean supporting;
    public static SupportScope support(boolean enabled) { return new SupportScope(enabled); }
    public static final class SupportScope implements AutoCloseable {
        private final boolean previous;
        private SupportScope(boolean enabled) { previous=supporting; supporting=enabled; }
        @Override public void close() { supporting=previous; }
    }

    public static VertexConsumer get(MultiBufferSource buffers) {
        return new GlowConsumer(buffers.getBuffer(FilamentVfx.ENTITY_TYPE), supporting);
    }

    // These procedural VFX all finish each vertex with setNormal. Keep the original
    // positions/colors, then expand only the cross-section for the two fading halos.
    private static final class GlowConsumer implements VertexConsumer {
        private final VertexConsumer out;
        private final Vec3[] positions = new Vec3[4];
        private final float[][] colors = new float[4][4];
        private int index;
        private final boolean support;
        GlowConsumer(VertexConsumer out, boolean support) { this.out = out; this.support = support; }
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            positions[index] = new Vec3(x,y,z); return this;
        }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) {
            colors[index] = new float[]{r/255F,g/255F,b/255F,a/255F}; return this;
        }
        @Override public VertexConsumer setUv(float u,float v) { return this; }
        @Override public VertexConsumer setUv1(int u,int v) { return this; }
        @Override public VertexConsumer setUv2(int u,int v) { return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) {
            if (++index == 4) { emit(); index = 0; }
            return this;
        }
        private void emit() {
            if (support) { for(int i=0;i<4;i++) vertex(positions[i],colors[i],.28F,0); return; }
            Vec3 start=positions[0].lerp(positions[1],.5),end=positions[3].lerp(positions[2],.5);
            // Limit expansion on broad discs/walls; narrow ribbons can have a wider halo.
            double width=Math.max(positions[0].distanceTo(positions[1]),positions[3].distanceTo(positions[2]));
            if(width>.00001 && start.distanceToSqr(end)>.000001) {
                // Same broad halo / colored glow / pale core hierarchy as the flight wake.
                // Cap widening for large walls and discs to retain their authored silhouette.
                double core = Math.max(.40, Math.min(2, .04 / width));
                halo(start,end,Math.min(core*9,1+.48/width),.11F,0);
                halo(start,end,core*3.3,.36F,0);
                halo(start,end,core,1F,.40F);
            }
            for(int i=0;i<4;i++) vertex(positions[i],colors[i],.20F,0);
        }
        private void halo(Vec3 start,Vec3 end,double scale,float strength,float pale) {
            Vec3 a=start.add(positions[0].subtract(start).scale(scale));
            Vec3 b=start.add(positions[1].subtract(start).scale(scale));
            Vec3 c=end.add(positions[2].subtract(end).scale(scale));
            Vec3 d=end.add(positions[3].subtract(end).scale(scale));
            vertex(a,colors[0],0,pale);vertex(start,colors[0],strength,pale);
            vertex(end,colors[3],strength,pale);vertex(d,colors[3],0,pale);
            vertex(start,colors[1],strength,pale);vertex(b,colors[1],0,pale);
            vertex(c,colors[2],0,pale);vertex(end,colors[2],strength,pale);
        }
        private void vertex(Vec3 p,float[] color,float strength,float pale) {
            out.addVertex((float)p.x,(float)p.y,(float)p.z)
                    .setColor(pale+color[0]*(1-pale),pale+color[1]*(1-pale),pale+color[2]*(1-pale),color[3]*strength);
        }
    }
}
