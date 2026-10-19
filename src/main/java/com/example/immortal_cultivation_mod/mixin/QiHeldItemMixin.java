package com.example.immortal_cultivation_mod.mixin;
import com.example.immortal_cultivation_mod.client.qi.QiCoatingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemRenderer.class)
public abstract class QiHeldItemMixin {
    @Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void immortal$coating(ItemStack stack,ItemDisplayContext display,boolean left,PoseStack pose,MultiBufferSource buffers,int light,int overlay,BakedModel model,CallbackInfo ci){
        QiCoatingRenderer.baked(pose,buffers,stack,display,model);
    }
}
