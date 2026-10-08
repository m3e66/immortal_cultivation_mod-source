package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.pagoda.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class PagodaRenderer extends GeoEntityRenderer<PagodaEntity> {
    public static boolean assetsReady(){
        var resources=net.minecraft.client.Minecraft.getInstance().getResourceManager();
        return resources.getResource(PagodaWorlds.id("geo/item/baota.geo.json")).isPresent()
                &&resources.getResource(PagodaWorlds.id("textures/item/baota_atlas.png")).isPresent();
    }
    public PagodaRenderer(EntityRendererProvider.Context context){super(context,new GeoModel<>(){
        @Override public ResourceLocation getModelResource(PagodaEntity e){return PagodaWorlds.id(e.gourd()?(e.core()?"geo/item/jin_hulu.geo.json":"geo/item/jin_hulu_summoned.geo.json"):e.core()?"geo/item/baota_core.geo.json":"geo/item/baota.geo.json");}
        @Override public ResourceLocation getTextureResource(PagodaEntity e){return PagodaWorlds.id(e.gourd()?"textures/item/jin_hulu_atlas.png":"textures/item/baota_atlas.png");}
        @Override public ResourceLocation getAnimationResource(PagodaEntity e){return PagodaWorlds.id(e.gourd()?"animations/item/jin_hulu.animation.json":e.core()?"animations/item/baota_core.animation.json":"animations/item/baota.animation.json");}
        @Override public void setCustomAnimations(PagodaEntity entity,long instance,software.bernie.geckolib.animation.AnimationState<PagodaEntity> state){
            super.setCustomAnimations(entity,instance,state);
            getBone("stopper").ifPresent(bone->bone.setHidden(entity.gourd()&&!entity.core()));
            getBone("breakout_cracks").ifPresent(bone->{bone.setHidden(entity.core()||entity.breakout()<50);bone.setScaleX(1);bone.setScaleY(1);bone.setScaleZ(1);});
        }
    });}
    @Override public void render(PagodaEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(entity.gourd()||assetsReady()){super.render(entity,yaw,partial,pose,buffers,light);return;}
        // Keep incomplete/resource-pack installs safe while the user's geometry is absent.
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float size=entity.core()?1.6F:14*entity.visualScale();pose.scale(size,size,size);pose.translate(-.5,0,-.5);
        com.example.immortal_cultivation_mod.item.WeaponInventorySprite.render("baota",pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    @Override public void preRender(PoseStack pose,PagodaEntity entity,BakedGeoModel model,MultiBufferSource buffers,VertexConsumer vertices,
            boolean reRender,float partial,int light,int overlay,int color){
        float scale=entity.core()?.75F:entity.visualScale()*(entity.gourd()?3:2);
        pose.scale(scale,scale,scale);
        if(entity.breakout()>=25){double time=entity.tickCount+partial;double amount=.025+entity.breakout()*.0007;pose.translate(Math.sin(time*2)*amount,0,Math.cos(time*2.4)*amount);}
        if(entity.core())pose.translate(0,Math.sin((entity.tickCount+partial)*.08)*.15,0);
        super.preRender(pose,entity,model,buffers,vertices,reRender,partial,light,overlay,color);
    }
}
