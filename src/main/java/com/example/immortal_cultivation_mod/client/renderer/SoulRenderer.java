package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SoulEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;

public class SoulRenderer extends HumanoidMobRenderer<SoulEntity, PlayerModel<SoulEntity>> {
   private static final int SOUL_COLOR = 654311423;
   private static final PlayerSkin STEVE_SKIN;
   private final PlayerModel<SoulEntity> slimModel;
   private final PlayerModel<SoulEntity> wideModel;

   public SoulRenderer(EntityRendererProvider.Context context) {
      super(context, new SoulPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.0F);
      this.slimModel = (PlayerModel)this.model;
      this.wideModel = new SoulPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
   }

   public void render(SoulEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (entity.textureId().isBlank()) {
         PlayerSkin skin = resolvePlayerSkin(entity);
         this.model = skin.model() == Model.SLIM ? this.slimModel : this.wideModel;
      } else {
         this.model = this.slimModel;
      }

      ((PlayerModel)this.model).young = false;
      ((PlayerModel)this.model).attackTime = 0.0F;
      ((PlayerModel)this.model).riding = false;
      ((PlayerModel)this.model).crouching = false;
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   protected RenderType getRenderType(SoulEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      return RenderType.entityTranslucent(this.getTextureLocation(entity));
   }

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
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo((UUID)entity.ownerUuid().get());
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
      } catch (RuntimeException var4) {
         return STEVE_SKIN;
      }

      return STEVE_SKIN;
   }

   private static boolean hasValidTexture(PlayerSkin skin) {
      return skin != null && skin.texture() != null && !skin.texture().getPath().isBlank();
   }

   static {
      STEVE_SKIN = new PlayerSkin(ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"), (String)null, (ResourceLocation)null, (ResourceLocation)null, Model.WIDE, true);
   }

   private static final class SoulPlayerModel extends PlayerModel<SoulEntity> {
      private SoulPlayerModel(ModelPart root, boolean slim) {
         super(root, slim);
      }

      public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
         super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, 654311423);
      }
   }
}
