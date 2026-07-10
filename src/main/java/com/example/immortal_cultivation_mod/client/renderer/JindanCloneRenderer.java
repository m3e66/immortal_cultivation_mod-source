package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.JindanCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class JindanCloneRenderer extends HumanoidMobRenderer<JindanCloneEntity, PlayerModel<JindanCloneEntity>> {
    public JindanCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(JindanCloneEntity entity) {
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(entity.getGameProfile()).texture();
    }
}
