package com.example.immortal_cultivation_mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Additive, depth-tested light around sigil strokes; soft alpha never gets cut off. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "immortal_cultivation_mod",
        value = net.neoforged.api.distmarker.Dist.CLIENT,
        bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class RestrictionSigilGlow {
    private static ShaderInstance shader;
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();
    private static final RenderStateShard.TransparencyStateShard ADDITIVE =
            new RenderStateShard.TransparencyStateShard("restriction_sigil_additive", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private RestrictionSigilGlow() {}

    @net.neoforged.bus.api.SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "restriction_sigil_glow"),
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> shader = loaded);
    }

    public static RenderType type(ResourceLocation layer) {
        return TYPES.computeIfAbsent(layer, texture -> {
            var halo = texture.withPath(texture.getPath().replace(".png", "_glow.png"));
            return RenderType.create("restriction_sigil_glow_" + texture.getPath(),
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 2048, false, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                            .setTextureState(new RenderStateShard.TextureStateShard(halo, true, false))
                            .setTransparencyState(ADDITIVE)
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .createCompositeState(false));
        });
    }
}
