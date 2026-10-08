package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.WhipRenderContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemRenderer.class)
public abstract class WhipItemRenderContextMixin {
    @WrapMethod(method="renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V")
    private void immortal$holder(LivingEntity entity,ItemStack stack,ItemDisplayContext display,boolean left,
                                PoseStack pose,MultiBufferSource buffers,Level level,int light,int overlay,int seed,Operation<Void> original) {
        var previous=WhipRenderContext.set(entity);
        try {original.call(entity,stack,display,left,pose,buffers,level,light,overlay,seed);}
        finally {WhipRenderContext.set(previous);}
    }
}
