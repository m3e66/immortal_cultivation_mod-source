package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Broad luminous threads plus non-additive mist, dust, chips and feather silhouettes. */
final class BeastVfxGeometry {
    static final double SIZE_MULTIPLIER = 1.65;
    private static final double TRAIL_WIDTH_MULTIPLIER = 9.9;
    private static boolean reducedDetail;
    static int detail(int count) {
        return reducedDetail ? count : (int)Math.ceil(count * 2.6);
    }
    static final net.minecraft.client.renderer.RenderType ENTITY_MATTER = net.minecraft.client.renderer.RenderType.create(
            "beast_entity_matter", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 32768, false, true,
            net.minecraft.client.renderer.RenderType.CompositeState.builder()
                    .setShaderState(new net.minecraft.client.renderer.RenderStateShard.ShaderStateShard(
                            net.minecraft.client.renderer.GameRenderer::getPositionColorShader))
                    .setTransparencyState(new net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard("beast_entity_alpha", () -> {
                        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(770,771,0,1);
                    }, () -> { com.mojang.blaze3d.systems.RenderSystem.disableBlend(); com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc(); }))
                    .setDepthTestState(net.minecraft.client.renderer.RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE)
                    .setCullState(net.minecraft.client.renderer.RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static int quads;
    private static int limit,objectLimit;
    final VertexConsumer light, matter;
    final Matrix4f matrix;
    final Vec3 camera, right, up;
    static void registerBuffers(net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent event) {
        // Both consumers stay live while geometry alternates between glow and matter.
        // Unregistered types share one builder; acquiring the second ends the first.
        event.registerRenderBuffer(FilamentVfx.TYPE);
        event.registerRenderBuffer(CombatImpactVfx.MATTER);
        event.registerRenderBuffer(FilamentVfx.ENTITY_TYPE);
        event.registerRenderBuffer(ENTITY_MATTER);
    }
    BeastVfxGeometry(VertexConsumer light,VertexConsumer matter,Matrix4f matrix,Vec3 camera,Vec3 right,Vec3 up) {
        this.light=light;this.matter=matter;this.matrix=matrix;this.camera=camera;this.right=right;this.up=up;
    }
    static void beginEntities() {
        reducedDetail = net.minecraft.client.Minecraft.getInstance().options.particles().get()
                == net.minecraft.client.ParticleStatus.MINIMAL;
        quads=0;limit=6000;objectLimit=limit;
    }
    static void begin(boolean minimal) { reducedDetail=minimal;quads=0;limit=minimal?4000:14000;objectLimit=limit; }
    static void object(int budget){objectLimit=Math.min(limit,quads+budget);}
    static boolean exhausted(){return quads>=limit;}
    static boolean full(){return quads>=objectLimit;}
    void line(Vec3 a,Vec3 b,double width,int color,float alpha) {
        if(full() || alpha<.002)return;
        width *= TRAIL_WIDTH_MULTIPLIER;
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,blue=(color&255)/255F;
        quads+=FilamentVfx.ribbon(light,matrix,a,b,camera,width*4,r,g,blue,alpha*.07F,alpha*.07F);
        quads+=FilamentVfx.ribbon(light,matrix,a,b,camera,width*1.8,r,g,blue,alpha*.2F,alpha*.2F);
        quads+=FilamentVfx.ribbon(light,matrix,a,b,camera,width,r,g,blue,alpha*.8F,alpha*.8F);
    }
    void streak(Vec3 a, Vec3 b, double width, int color, float alpha, double time) {
        line(a, b, width, color, alpha);
        if (reducedDetail || full()) return;
        Vec3 tangent = b.subtract(a);
        if (tangent.lengthSqr() < .000001) return;
        Vec3 side = tangent.cross(a.lerp(b, .5).subtract(camera));
        if (side.lengthSqr() < .000001) side = com.example.immortal_cultivation_mod.physics.CultivationFlightTrail.side(tangent, null);
        side = side.normalize();
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 offsetA = side.scale(sign * width * (2.6 + Math.sin(time * .19) * .5));
            Vec3 offsetB = side.scale(sign * width * (2.6 + Math.sin(time * .19 + 1.4) * .5));
            line(a.add(offsetA), b.add(offsetB), width * .55, color, alpha * .6F);
        }
    }
    void curve(Vec3 a,Vec3 control,Vec3 b,double width,int color,float alpha) {
        Vec3 last=a;
        for(int i=1;i<=12 && !full();i++){
            double t=i/12.0;
            Vec3 p=a.scale((1-t)*(1-t)).add(control.scale(2*t*(1-t))).add(b.scale(t*t));
            line(last,p,width,color,alpha*(float)Math.sin(t*Math.PI));last=p;
        }
    }
    void mist(Vec3 center,double size,int color,float alpha,double stretch) {
        if(full() || alpha<.002 || size<=0)return;
        size *= SIZE_MULTIPLIER;
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        // Ordinary alpha blending prevents smoke, shadow and soil from becoming glowing balls.
        quads+=FilamentVfx.halo(matter,matrix,center,camera,right.scale(stretch),up,size,r,g,b,alpha);
    }
    void mote(Vec3 center,double size,int color,float alpha) {
        if(full() || alpha<.002)return;
        size *= SIZE_MULTIPLIER;
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        quads+=FilamentVfx.halo(light,matrix,center,camera,right,up,size,r,g,b,alpha*.4F);
    }
    void chip(Vec3 center,double size,int color,float alpha,double rotation) {
        if(full() || alpha<.002)return;
        size *= SIZE_MULTIPLIER;
        Vec3 x=right.scale(Math.cos(rotation)).add(up.scale(Math.sin(rotation))).scale(size);
        Vec3 y=up.scale(Math.cos(rotation)).subtract(right.scale(Math.sin(rotation))).scale(size*.6);
        Vec3 depth=right.cross(up).scale(size*.55);
        Vec3 top=center.add(y),bottom=center.subtract(y),left=center.subtract(x),r=center.add(x),front=center.add(depth);
        triangle(top,left,front,color,alpha);triangle(left,bottom,front,shade(color,.72),alpha);
        triangle(bottom,r,front,shade(color,.6),alpha);triangle(r,top,front,shade(color,.9),alpha);
    }
    void feather(Vec3 center,Vec3 axis,double length,int color,float alpha,double twist,boolean burning) {
        length *= SIZE_MULTIPLIER;
        Vec3 tangent=axis.lengthSqr()<.001?up:axis.normalize();
        Vec3 side=tangent.cross(right).normalize();
        if(side.lengthSqr()<.001)side=up;
        side=side.scale(Math.cos(twist)).add(tangent.cross(side).scale(Math.sin(twist)));
        Vec3 a=center.subtract(tangent.scale(length*.5)),b=center.add(tangent.scale(length*.5));
        line(a,b,.008,color,alpha);
        for(int i=1;i<=5;i++) {
            double u=i/6.0,width=Math.sin(u*Math.PI)*length*.16;
            Vec3 spine=a.lerp(b,u);
            for(int sign=-1;sign<=1;sign+=2) {
                Vec3 tip=spine.add(side.scale(sign*width)).subtract(tangent.scale(length*.12));
                if(!full()) {
                    float r=(color>>16&255)/255F,g=(color>>8&255)/255F,blue=(color&255)/255F;
                    quads+=FilamentVfx.ribbon(light,matrix,spine,tip,camera,.004,r,g,blue,alpha*.45F,alpha*.45F);
                }
                if(burning && i%2==0)mote(tip.add(0,.025*Math.sin(twist+i),0),.025,color,alpha);
            }
        }
    }
    void crack(Vec3 center,Vec3 axis,double length,int color,float alpha,int seed) {
        Vec3 side=new Vec3(-axis.z,.03,axis.x).normalize();
        Vec3 last=center;
        for(int i=1;i<=7;i++) {
            double u=i/7.0;
            Vec3 p=center.add(axis.scale(length*u)).add(side.scale(Math.sin(seed+i*6.7)*length*.08));
            line(last,p,.012,color,alpha*(float)(1-u*.6));
            if(i==3 || i==5)line(p,p.add(side.scale((i%2==0?-1:1)*length*.18)).add(axis.scale(length*.08)),.008,color,alpha*.5F);
            last=p;
        }
    }
    void triangle(Vec3 a,Vec3 b,Vec3 c,int color,float alpha) {
        if(full())return;
        vertex(a,color,alpha);vertex(b,color,alpha);vertex(c,color,alpha);vertex(c,color,alpha);quads++;
    }
    private void vertex(Vec3 p,int color,float alpha) {
        p=p.subtract(camera);
        VfxVertexWriter.vertex(matter, matrix,(float)p.x,(float)p.y,(float)p.z).setColor((color>>16&255)/255F,
                (color>>8&255)/255F,(color&255)/255F,Mth.clamp(alpha,0,1));
    }
    static int shade(int color,double factor) {
        return (int)((color>>16&255)*factor)<<16 | (int)((color>>8&255)*factor)<<8 | (int)((color&255)*factor);
    }
}
