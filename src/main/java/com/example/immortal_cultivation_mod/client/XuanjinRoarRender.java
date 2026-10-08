package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.companion.XuanjinRules;
import com.example.immortal_cultivation_mod.network.XuanjinPayloads;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Expanding wavefronts perpendicular to the cast direction, using vanilla sonic-boom frames. */
public final class XuanjinRoarRender {
    private static final RenderType[] FRAMES=new RenderType[16];
    static {
        for(int i=0;i<16;i++)FRAMES[i]=RenderType.entityTranslucentEmissive(
                ResourceLocation.withDefaultNamespace("textures/particle/sonic_boom_"+i+".png"),false);
    }
    public static void render(XuanjinPayloads.Effect effect,float age,RenderLevelStageEvent event) {
        Vec3 forward=effect.target().subtract(effect.origin()).normalize();
        if(forward.lengthSqr()<.5)return;
        Vec3 side=forward.cross(new Vec3(0,1,0)).normalize();
        if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        Vec3 up=side.cross(forward).normalize(),camera=event.getCamera().getPosition();
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        Matrix4f matrix=event.getPoseStack().last().pose();
        // Furthest wavefront first. All rings share the cast-time direction, even if the caster turns.
        for(int ring=0;ring<XuanjinRules.ROAR_RINGS;ring++) {
            double waveAge=age-ring*XuanjinRules.ROAR_RING_DELAY;
            if(waveAge<0 || waveAge>=XuanjinRules.ROAR_TRAVEL_TICKS+XuanjinRules.ROAR_FADE_TICKS)continue;
            double distance=Math.min(1,waveAge/XuanjinRules.ROAR_TRAVEL_TICKS)*XuanjinRules.ROAR_RANGE;
            double radius=XuanjinRules.roarRadius(distance);
            // The later vanilla frames are hollow shock rings, keeping the cone readable through its center.
            int frame=Math.clamp(10+(int)(waveAge*.22),10,15);
            // Each ring reaches full range and size before fading, including the last emitted ring.
            float alpha=(float)(.85*Math.min(1,waveAge/2)*Math.min(1,
                    (XuanjinRules.ROAR_TRAVEL_TICKS+XuanjinRules.ROAR_FADE_TICKS-waveAge)/XuanjinRules.ROAR_FADE_TICKS));
            if(alpha<=0)continue;
            Vec3 center=effect.origin().add(forward.scale(distance)).subtract(camera);
            Vec3 horizontal=side.scale(radius),vertical=up.scale(radius);
            RenderType type=FRAMES[frame];var vertices=buffers.getBuffer(type);
            vertex(vertices,matrix,center.subtract(horizontal).subtract(vertical),0,1,alpha,forward);
            vertex(vertices,matrix,center.add(horizontal).subtract(vertical),1,1,alpha,forward);
            vertex(vertices,matrix,center.add(horizontal).add(vertical),1,0,alpha,forward);
            vertex(vertices,matrix,center.subtract(horizontal).add(vertical),0,0,alpha,forward);
            buffers.endBatch(type);
        }
    }
    private static void vertex(VertexConsumer vertices,Matrix4f matrix,Vec3 p,float u,float v,float alpha,Vec3 normal) {
        vertices.addVertex(matrix,(float)p.x,(float)p.y,(float)p.z).setColor(.75F,.95F,1F,alpha)
                .setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float)normal.x,(float)normal.y,(float)normal.z);
    }
}
