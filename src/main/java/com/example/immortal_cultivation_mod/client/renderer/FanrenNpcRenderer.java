package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.model.FanrenNpcModel;
import com.example.immortal_cultivation_mod.entity.FanrenNpcEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FanrenNpcRenderer extends HumanoidMobRenderer<FanrenNpcEntity, FanrenNpcModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/entity/fanren_npc.png");

    public FanrenNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new FanrenNpcModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FanrenNpcEntity entity) {
        return TEXTURE;
    }
}
