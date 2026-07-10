package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.CultivatorCorpseModel;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class CultivatorCorpseRenderer extends LivingEntityRenderer<CultivatorCorpseEntity, CultivatorCorpseModel> {
    public CultivatorCorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new CultivatorCorpseModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.0F);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        addLayer(new ZhenshiSealHelmetLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CultivatorCorpseEntity entity) {
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(entity.getGameProfile()).texture();
    }

    @Override
    protected float getBob(CultivatorCorpseEntity livingBase, float partialTicks) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(CultivatorCorpseEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
        if (entity.isRaised()) {
            super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);
            return;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getCorpseYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.0D, -1.05D, 0.0D);
    }
}
