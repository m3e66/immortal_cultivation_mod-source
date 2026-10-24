package com.example.immortal_cultivation_mod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.example.immortal_cultivation_mod.spell.ModSpells;

/** Solid, world-oriented spell cores; collision and travel remain owned by the actual projectile. */
public final class TechniqueBoltRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    public TechniqueBoltRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius=0; }
    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
    @Override public void render(T entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        draw(entity,BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath(),partial,pose,buffers);
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    public static void draw(Entity entity,String id,float partial,PoseStack pose,MultiBufferSource buffers) {
        var spell=ModSpells.get(id);
        String style=id+","+(spell==null?"":spell.element());
        int color=color(style);
        Vec3 direction=entity.getDeltaMovement();
        if(direction.lengthSqr()<1e-6)direction=entity.getLookAngle();
        pose.pushPose();
        float charge = entity instanceof com.example.immortal_cultivation_mod.entity.FireballProjectileEntity fire ? fire.chargeScale()
                : entity instanceof com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity trigger ? trigger.chargeScale() : 1F;
        float size = (style.contains("fireball") ? 1.7F : 1F) * Math.clamp(charge,1F,2F);
        pose.scale(size,size,size);
        pose.mulPose(Axis.YP.rotation((float)Math.atan2(direction.x,direction.z)));
        pose.mulPose(Axis.XP.rotation((float)-Math.atan2(direction.y,direction.horizontalDistance())));
        float age=entity.tickCount+partial;
        float radius=.19F+.02F*(float)Math.sin(age*.9);
        boolean blade=style.contains("wind")||style.contains("jian")||style.contains("metal");
        if(blade)pose.scale(2.1F,.4F,1);
        pose.mulPose(Axis.ZP.rotationDegrees(age*(blade?8:15)));
        VertexConsumer v=buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        // Twelve-sided solid core, with an elongated trailing tip and bright leading tip.
        for(int i=0;i<12;i++) {
            float a=(float)(i*Math.PI/6),b=(float)((i+1)*Math.PI/6);
            float x=(float)Math.cos(a)*radius,y=(float)Math.sin(a)*radius;
            float x2=(float)Math.cos(b)*radius,y2=(float)Math.sin(b)*radius;
            tri(v,pose,0,0,.38F,x,y,0,x2,y2,0,0xffffff,235);
            tri(v,pose,x2,y2,0,x,y,0,0,0,-.85F,color,210);
        }
        // Two travelling hoops read as motion in world space, never camera-facing sprites.
        for(int ring=0;ring<2;ring++) {
            float z=-.25F-ring*.36F-(age*.045F% .36F);
            float r=radius*(1.4F+ring*.35F);
            for(int i=0;i<16;i++) {
                double a=i*Math.PI/8,b=(i+1)*Math.PI/8;
                vertex(v,pose,(float)Math.cos(a)*r,(float)Math.sin(a)*r,z,color,140);
                vertex(v,pose,(float)Math.cos(b)*r,(float)Math.sin(b)*r,z,color,140);
                vertex(v,pose,(float)Math.cos(b)*(r+.025F),(float)Math.sin(b)*(r+.025F),z,color,140);
                vertex(v,pose,(float)Math.cos(a)*(r+.025F),(float)Math.sin(a)*(r+.025F),z,color,140);
            }
        }
        pose.popPose();
    }
    public static int color(String style) {
        if(style.contains("fire")||style.contains("flare")||style.contains("yan"))return 0xff7c30;
        if(style.contains("frost")||style.contains("ice")||style.contains("bing"))return 0x92e9ff;
        if(style.contains("lightning")||style.contains("thunder")||style.contains("lei"))return 0xb195ff;
        if(style.contains("water")||style.contains("hanlan")||style.contains("liu"))return 0x40a8ef;
        if(style.contains("wood")||style.contains("changqing"))return 0x72dc91;
        if(style.contains("earth")||style.contains("xuanyue"))return 0xdfb563;
        if(style.contains("kong")||style.contains("smoke"))return 0xaf85dc;
        if(style.contains("jin")||style.contains("metal"))return 0xffd876;
        return 0x81ffe6;
    }
    private static void tri(VertexConsumer v,PoseStack p,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,int color,int alpha) {
        vertex(v,p,ax,ay,az,color,alpha);vertex(v,p,bx,by,bz,color,alpha);
        vertex(v,p,cx,cy,cz,color,alpha);vertex(v,p,cx,cy,cz,color,alpha);
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float z,int color,int alpha) {
        v.addVertex(p.last().pose(),x,y,z).setColor((color>>16)&255,(color>>8)&255,color&255,alpha)
          .setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(p.last(),0,1,0);
    }
}
