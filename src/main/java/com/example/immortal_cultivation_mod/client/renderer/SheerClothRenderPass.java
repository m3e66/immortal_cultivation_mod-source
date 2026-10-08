package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** Flush sheer cloth after the wearer's opaque layers, without occluding later models. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class SheerClothRenderPass {
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(262144));
    private static final java.util.Map<ResourceLocation, RenderType> TYPES = new java.util.HashMap<>();
    private static RenderType createType(ResourceLocation texture) {
        return RenderType.create("cultivation_sheer_cloth",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture,false,false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false));
    }
    private static boolean pending;

    private SheerClothRenderPass() {}
    public static RenderType type(ResourceLocation texture) { return TYPES.computeIfAbsent(texture, SheerClothRenderPass::createType); }
    public static MultiBufferSource buffers() { pending=true; return BUFFERS; }

    @SubscribeEvent
    public static void afterLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!pending) return;
        pending=false;
        if(event.getMultiBufferSource() instanceof MultiBufferSource.BufferSource source) source.endBatch();
        var main=Minecraft.getInstance().renderBuffers().bufferSource();
        if(main!=event.getMultiBufferSource()) main.endBatch();
        BUFFERS.endBatch();
    }
}
