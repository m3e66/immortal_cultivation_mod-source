package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.XuanjinZhengModel;
import com.example.immortal_cultivation_mod.companion.XuanjinRules;
import com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class XuanjinZhengRenderer extends GeoEntityRenderer<XuanjinZhengEntity> {
    public XuanjinZhengRenderer(EntityRendererProvider.Context context) {
        super(context,new XuanjinZhengModel()); addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    @Override public net.minecraft.client.renderer.RenderType getRenderType(XuanjinZhengEntity entity,net.minecraft.resources.ResourceLocation texture,
            MultiBufferSource source,float partialTick) {
        return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
    }
    @Override public void preRender(PoseStack poses,XuanjinZhengEntity e,BakedGeoModel model,MultiBufferSource source,VertexConsumer buffer,
            boolean reRender,float partial,int light,int overlay,int color) {
        float scale=XuanjinRules.stage(e.seal()).scale(); shadowRadius=1.2F*scale;
        poses.scale(scale,scale,scale);
        super.preRender(poses,e,model,source,buffer,reRender,partial,light,overlay,color);
    }
}
