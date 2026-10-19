package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Procedural feather, stone, crystal, silk and foxfire bodies; no ordinary item/fire-charge sprite. */
public final class BeastProjectileRenderer<T extends Projectile> extends EntityRenderer<T> {
    public BeastProjectileRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public void render(T entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        var style=BeastAttackVfx.style(entity);
        if(style!=null && !ShaderPackCompatibility.isRenderingShadowPass()){
            Vec3 right=new Vec3(new Vector3f(1,0,0).rotate(camera.rotation()));
            Vec3 up=new Vec3(new Vector3f(0,1,0).rotate(camera.rotation()));
            BeastVfxGeometry.object(1000);
            var g=new BeastVfxGeometry(buffers.getBuffer(FilamentVfx.ENTITY_TYPE),buffers.getBuffer(BeastVfxGeometry.ENTITY_MATTER),
                    poses.last().pose(),Vec3.ZERO,right,up);
            BeastMoveVfx.projectile(g,entity,style,Vec3.ZERO,java.util.List.of(),entity.level().getGameTime()+partial,0x736958,true);
        }
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(T entity){return TextureAtlas.LOCATION_BLOCKS;}
}
