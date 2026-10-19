package com.example.immortal_cultivation_mod.mixin;
import com.example.immortal_cultivation_mod.client.qi.QiCoatingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerRenderer.class)
public abstract class QiHandViewMixin {
    // Vanilla hands read PlayerSkin directly, bypassing the renderer's disguise/reveal rules.
    // Share the body's resolved texture for both the bare arm and its outer sleeve.
    @Redirect(method="renderHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/PlayerSkin;texture()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation immortal$handSkin(PlayerSkin skin,PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,ModelPart arm,ModelPart sleeve){
        return ((PlayerRenderer)(Object)this).getTextureLocation(player);
    }

    @Inject(method="renderHand",at=@At("TAIL"))
    private void immortal$hand(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,ModelPart arm,ModelPart sleeve,CallbackInfo ci){
        var renderer=(PlayerRenderer)(Object)this;
        QiCoatingRenderer.hand(pose,buffers,player,arm,arm==renderer.getModel().rightArm,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false),true);
        com.example.immortal_cultivation_mod.client.curse.CurseClient.hand(pose,buffers,player,arm,sleeve,arm==renderer.getModel().rightArm,renderer.getTextureLocation(player),light,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }
}
