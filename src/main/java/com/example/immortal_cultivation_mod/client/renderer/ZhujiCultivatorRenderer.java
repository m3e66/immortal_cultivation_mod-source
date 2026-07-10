package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.model.ZhujiCultivatorModel;
import com.example.immortal_cultivation_mod.entity.ZhujiCultivatorEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ZhujiCultivatorRenderer extends HumanoidMobRenderer<ZhujiCultivatorEntity, ZhujiCultivatorModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/entity/zhuji1.png");

    public ZhujiCultivatorRenderer(EntityRendererProvider.Context context) {
        super(context, new ZhujiCultivatorModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ZhujiCultivatorEntity entity) {
        return TEXTURE;
    }
}
