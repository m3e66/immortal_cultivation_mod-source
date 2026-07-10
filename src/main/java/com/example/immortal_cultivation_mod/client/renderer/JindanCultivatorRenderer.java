package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.model.JindanCultivatorModel;
import com.example.immortal_cultivation_mod.entity.JindanCultivatorEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class JindanCultivatorRenderer extends HumanoidMobRenderer<JindanCultivatorEntity, JindanCultivatorModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/entity/jindan1.png");

    public JindanCultivatorRenderer(EntityRendererProvider.Context context) {
        super(context, new JindanCultivatorModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(JindanCultivatorEntity entity) {
        return TEXTURE;
    }
}
