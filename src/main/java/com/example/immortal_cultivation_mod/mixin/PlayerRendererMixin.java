package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true)
    private void immortalCultivation$disguiseSkin(
            AbstractClientPlayer player,
            CallbackInfoReturnable<ResourceLocation> cir) {
        ItemStack mask =
                DisguiseMaskItem.getEquippedMask(player);

        if (mask.isEmpty()
                || immortalCultivation$canReveal(player)) {
            return;
        }

        ResourceLocation disguiseTexture =
                DisguiseMaskItem.disguiseTexture(mask);

        if (disguiseTexture != null) {
            cir.setReturnValue(disguiseTexture);
        }
    }

    @Inject(
            method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void immortalCultivation$hideDisguisedNameTag(
            AbstractClientPlayer player,
            Component name,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            float partialTick,
            CallbackInfo ci) {
        if (DisguiseMaskItem.isWearing(player)
                && !immortalCultivation$canReveal(player)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void immortalCultivation$jindanFastFlightRotation(
            AbstractClientPlayer player,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float scale,
            CallbackInfo ci) {
        if (!JindanCreativeFlight
                .shouldUseElytraPose(player)) {
            return;
        }

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        -90.0F - player.getXRot()));

        Vec3 look =
                player.getViewVector(partialTick);

        Vec3 movement =
                player.getDeltaMovement();

        double movementHorizontal =
                movement.horizontalDistanceSqr();

        double lookHorizontal =
                look.horizontalDistanceSqr();

        if (movementHorizontal <= 0.0D
                || lookHorizontal <= 0.0D) {
            return;
        }

        double dot =
                (movement.x * look.x
                        + movement.z * look.z)
                        / Math.sqrt(
                        movementHorizontal
                                * lookHorizontal);

        double cross =
                movement.x * look.z
                        - movement.z * look.x;

        double angle =
                Math.signum(cross)
                        * Math.acos(
                        Math.max(
                                -1.0D,
                                Math.min(1.0D, dot)));

        poseStack.mulPose(
                Axis.YP.rotation((float)angle));
    }

    private static boolean
    immortalCultivation$canReveal(
            AbstractClientPlayer target) {
        var viewer =
                Minecraft.getInstance().player;

        return viewer != null
                && viewer.hasEffect(
                ModEffects.SPIRIT_SIGHT)
                && viewer.distanceToSqr(target)
                <= 64.0D;
    }
}
