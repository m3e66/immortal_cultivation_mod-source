package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SoulEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class SoulRenderer extends HumanoidMobRenderer<SoulEntity, PlayerModel<SoulEntity>> {
    private static final int SOUL_COLOR = 0x26FFFFFF;
    private static final PlayerSkin STEVE_SKIN = new PlayerSkin(
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"),
            null, null, null, PlayerSkin.Model.WIDE, true);
    private final PlayerModel<SoulEntity> slimModel;
    private final PlayerModel<SoulEntity> wideModel;

    public SoulRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulPlayerModel(
                context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.0F);
        slimModel = model;
        wideModel = new SoulPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(SoulEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.textureId().isBlank()) {
            PlayerSkin skin = resolvePlayerSkin(entity);
            model = skin.model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
        } else {
            model = slimModel;
        }
        model.young = false;
        model.attackTime = 0.0F;
        model.riding = false;
        model.crouching = false;
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    protected RenderType getRenderType(SoulEntity entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    @Override
    public ResourceLocation getTextureLocation(SoulEntity entity) {
        String textureId = entity.textureId();
        if (!textureId.isBlank()) {
            ResourceLocation texture = ResourceLocation.tryParse(textureId);
            if (texture != null && !texture.getPath().isBlank()) {
                return texture;
            }
        }
        return resolvePlayerSkin(entity).texture();
    }

    private static PlayerSkin resolvePlayerSkin(SoulEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.getConnection() != null && entity.ownerUuid().isPresent()) {
                var playerInfo = minecraft.getConnection().getPlayerInfo(entity.ownerUuid().get());
                if (playerInfo != null) {
                    PlayerSkin skin = playerInfo.getSkin();
                    if (hasValidTexture(skin)) {
                        return skin;
                    }
                }
            }

            GameProfile profile = entity.getGameProfile();
            if (profile.getProperties().containsKey("textures")) {
                PlayerSkin skin = minecraft.getSkinManager().getInsecureSkin(profile);
                if (hasValidTexture(skin)) {
                    return skin;
                }
            }
        } catch (RuntimeException ignored) {
            return STEVE_SKIN;
        }
        return STEVE_SKIN;
    }

    private static boolean hasValidTexture(PlayerSkin skin) {
        return skin != null
                && skin.texture() != null
                && !skin.texture().getPath().isBlank();
    }

    private static final class SoulPlayerModel extends PlayerModel<SoulEntity> {
        private SoulPlayerModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                   int packedOverlay, int color) {
            super.renderToBuffer(
                    poseStack, buffer, packedLight, packedOverlay, SOUL_COLOR);
        }
    }
}
