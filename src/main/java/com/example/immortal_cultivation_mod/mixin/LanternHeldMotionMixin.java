package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.LanternHeldMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class LanternHeldMotionMixin {
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/client/ClientHooks;handleCameraTransforms(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemDisplayContext;Z)Lnet/minecraft/client/resources/model/BakedModel;",shift=At.Shift.AFTER))
    private void immortal$lanternMotion(ItemStack stack,ItemDisplayContext display,boolean left,PoseStack poses,
                                       MultiBufferSource source,int light,int overlay,BakedModel model,CallbackInfo ci) {
        LanternHeldMotion.apply(stack,display,poses);
    }
}
