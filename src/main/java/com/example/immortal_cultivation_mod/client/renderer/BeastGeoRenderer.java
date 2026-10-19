package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.BeastVfxAnchors;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Small selected set of animated anatomy anchors shared by beast combat VFX. */
public class BeastGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    public BeastGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model) { super(context,model); }
    @Override public void renderRecursively(PoseStack poses,T entity,GeoBone bone,RenderType type,
            MultiBufferSource source,VertexConsumer buffer,boolean reRender,float partial,int light,int overlay,int color) {
        boolean track=BeastVfxAnchors.relevant(bone.getName()) && !reRender && !ShaderPackCompatibility.isRenderingShadowPass();
        if(track) bone.setTrackingMatrices(true);
        super.renderRecursively(poses,entity,bone,type,source,buffer,reRender,partial,light,overlay,color);
        if(track) BeastVfxAnchors.capture(entity,bone,partial);
    }
}
