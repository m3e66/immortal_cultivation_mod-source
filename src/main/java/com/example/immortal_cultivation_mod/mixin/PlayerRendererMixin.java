package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.client.BlackFlameClient;
import com.example.immortal_cultivation_mod.client.JindanFlightEffects;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.SpiritualSenseClient;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Unique private double immortalCultivation$lastFlightYawCorrection;
    @Unique private float immortalCultivation$lastFlightMovementPitch;

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"))
    private void immortalCultivation$blackFlameCollapse(
            AbstractClientPlayer player,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float scale,
            CallbackInfo ci) {
        BlackFlameClient.DeathAppearance appearance =
                BlackFlameClient.deathAppearance(player, partialTick);
        if (appearance != null && appearance.scale() < 0.999F) {
            poseStack.scale(appearance.scale(), appearance.scale(), appearance.scale());
        }
    }

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"))
    private void immortalCultivation$solarBodyScale(
            AbstractClientPlayer player,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float scale,
            CallbackInfo ci) {
        var solarBody = player.getEffect(ModEffects.SOLAR_BODY);
        if (solarBody == null) {
            return;
        }
        float bodyScale = switch (solarBody.getAmplifier()) {
            case 1 -> 1.15F;
            case 2 -> 1.25F;
            default -> 1.12F;
        };
        poseStack.scale(bodyScale, bodyScale, bodyScale);
    }

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
                || immortalCultivation$canReveal(player, mask)) {
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
        ItemStack mask = DisguiseMaskItem.getEquippedMask(player);
        if (!mask.isEmpty()
                && !immortalCultivation$canReveal(player, mask)) {
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
        float flightPoseProgress = com.example.immortal_cultivation_mod.client.SwordComboAnimations.isPlaying(player)
                ? 0.0F : JindanFlightEffects.fastFlightPoseProgress(player, partialTick);
        if (flightPoseProgress <= 0.001F) {
            immortalCultivation$lastFlightYawCorrection = 0.0D;
            immortalCultivation$lastFlightMovementPitch = 0.0F;
            return;
        }

        Vec3 movement =
                player.getDeltaMovement();

        double movementHorizontal =
                movement.horizontalDistanceSqr();

        // First turn the rendered body toward its actual horizontal movement,
        // instead of the camera direction or the player's last walking yaw.
        if (movementHorizontal > 1.0E-7D) {
            double yawRadians = Math.toRadians(rotationYaw);
            double renderedForwardX = -Math.sin(yawRadians);
            double renderedForwardZ = Math.cos(yawRadians);
            double horizontalLength = Math.sqrt(movementHorizontal);
            double dot = (movement.x * renderedForwardX
                    + movement.z * renderedForwardZ) / horizontalLength;
            double cross = movement.x * renderedForwardZ
                    - movement.z * renderedForwardX;
            immortalCultivation$lastFlightYawCorrection = Math.signum(cross) * Math.acos(
                    Math.max(-1.0D, Math.min(1.0D, dot)));
        }
        poseStack.mulPose(Axis.YP.rotation(
                (float)immortalCultivation$lastFlightYawCorrection
                        * flightPoseProgress));

        // Match the supplied model's 82.5-degree root tilt, then carry that
        // complete straight-body pose through the velocity pitch.
        if (movement.lengthSqr() > 1.0E-7D) {
            double horizontalSpeed = Math.sqrt(movementHorizontal);
            immortalCultivation$lastFlightMovementPitch = (float)Math.toDegrees(
                    Math.atan2(-movement.y, horizontalSpeed));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (-82.5F - immortalCultivation$lastFlightMovementPitch)
                        * flightPoseProgress));
    }

    private static boolean
    immortalCultivation$canReveal(
            AbstractClientPlayer target, ItemStack mask) {
        var viewer = Minecraft.getInstance().player;
        if (viewer == null) {
            return false;
        }

        boolean spiritSight = viewer.hasEffect(ModEffects.SPIRIT_SIGHT)
                && viewer.distanceToSqr(target) <= 64.0D;
        boolean strongerQiSense = SpiritualSenseClient.isActive()
                && SpiritualSenseClient.isSensingEntity(target.getId())
                && DisguiseMaskItem.canQiSenseReveal(
                        ClientData.cultivationData.cultivationLevel(),
                        mask);
        return spiritSight || strongerQiSense;
    }
}
