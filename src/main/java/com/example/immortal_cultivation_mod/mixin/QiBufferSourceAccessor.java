package com.example.immortal_cultivation_mod.mixin;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.SequencedMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MultiBufferSource.BufferSource.class)
public interface QiBufferSourceAccessor {
    @Accessor("fixedBuffers") SequencedMap<RenderType,ByteBufferBuilder> immortal$fixedBuffers();
}
