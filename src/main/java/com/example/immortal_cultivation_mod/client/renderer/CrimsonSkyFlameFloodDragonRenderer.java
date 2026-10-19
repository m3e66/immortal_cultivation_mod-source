package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.CrimsonSkyFlameFloodDragonModel;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.GeoBone;
import com.example.immortal_cultivation_mod.physics.DragonLookPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public final class CrimsonSkyFlameFloodDragonRenderer
        extends BeastGeoRenderer<CrimsonSkyFlameFloodDragonEntity> {
    public CrimsonSkyFlameFloodDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonSkyFlameFloodDragonModel());
        withScale(CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE);
        shadowRadius = 2.4F * CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE;
    }
    @Override public void renderRecursively(PoseStack poses,CrimsonSkyFlameFloodDragonEntity dragon,GeoBone bone,
                                            RenderType type,MultiBufferSource source,VertexConsumer buffer,boolean reRender,
                                            float partialTick,int light,int overlay,int color) {
        float offset=DragonLookPose.rotation(bone.getName(),dragon.getLookPitch(partialTick))*Mth.DEG_TO_RAD;
        if(offset==0) {
            super.renderRecursively(poses,dragon,bone,type,source,buffer,reRender,partialTick,light,overlay,color);return;
        }
        float rotation=bone.getRotX();
        bone.setRotX(rotation+offset);
        try {super.renderRecursively(poses,dragon,bone,type,source,buffer,reRender,partialTick,light,overlay,color);}
        finally {bone.setRotX(rotation);}
    }
}
