package com.example.immortal_cultivation_mod.mixin;
import com.example.immortal_cultivation_mod.client.qi.QiAuraMaterial;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RenderBuffers.class)
public abstract class QiRenderBuffersMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void immortal$qiBuffers(CallbackInfo ci){
        var buffers=((QiBufferSourceAccessor)((RenderBuffers)(Object)this).bufferSource()).immortal$fixedBuffers();
        buffers.put(QiAuraMaterial.ENERGY,new ByteBufferBuilder(262144));
        buffers.put(QiAuraMaterial.HALO,new ByteBufferBuilder(262144));
        buffers.put(QiAuraMaterial.GLOW,new ByteBufferBuilder(65536));
    }
}
