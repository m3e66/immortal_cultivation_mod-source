package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.FanrenNpcModel;
import com.example.immortal_cultivation_mod.entity.FanrenNpcEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FanrenNpcRenderer extends HumanoidMobRenderer<FanrenNpcEntity, FanrenNpcModel> {
    public FanrenNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new FanrenNpcModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FanrenNpcEntity entity) {
        return NpcProfile.skinTexture(entity, NpcProfile.FANREN);
    }
}
