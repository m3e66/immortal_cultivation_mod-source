package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

/** Fullbright rendering preserves each flame kind's own colored artwork. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FlameRenderTypes {
    private static final EnumMap<FlameKind, ResourceLocation> TEXTURES = new EnumMap<>(FlameKind.class);
    private static final EnumMap<FlameKind, RenderType> ENTITIES = new EnumMap<>(FlameKind.class);
    private static ShaderInstance shader;
    private static ShaderInstance heatHazeShader;
    private static final RenderType ENERGY = createEnergyType("cultivation_flame_shell", false);
    private static final RenderType CORE = createEnergyType("cultivation_flame_core", true);

    static {
        for (FlameKind kind : FlameKind.values()) {
            String element = kind.name().toLowerCase(Locale.ROOT);
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                    kind.special() ? "textures/entity/flame_seed_" + element + ".png"
                            : "textures/entity/spirit_flame.png");
            TEXTURES.put(kind, texture);
            ENTITIES.put(kind, createEntityType(element, texture));
        }
    }

    private static RenderType createEntityType(String element, ResourceLocation texture) {
        return RenderType.create("cultivation_spirit_flame_" + element,
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 2048, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(FlameRenderTypes::shader))
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));
    }

    private static RenderType createEnergyType(String name, boolean core) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                65536, false, !core, RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(core ? RenderStateShard.COLOR_DEPTH_WRITE : RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    /** Texture-free, fullbright world geometry, with normal terrain depth testing. */
    public static RenderType energy() { return ENERGY; }
    public static RenderType core() { return CORE; }

    /** The scene-copy shader keeps arbitrarily faint haze edges; vanilla has a 0.1 alpha test. */
    public static ShaderInstance heatHazeShader() { return heatHazeShader; }

    public static ResourceLocation texture(FlameKind kind) {
        return TEXTURES.get(kind == null ? FlameKind.MORTAL : kind);
    }

    public static RenderType entity(FlameKind kind) {
        return ENTITIES.get(kind == null ? FlameKind.MORTAL : kind);
    }

    // A separate vertex format avoids vanilla particle lightmap modulation: flames are fullbright.
    public static final ParticleRenderType PARTICLE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(FlameRenderTypes::shader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }

        @Override
        public String toString() {
            return "CULTIVATION_VESSEL_FLAME";
        }
    };

    private FlameRenderTypes() { }

    private static ShaderInstance shader() {
        return shader != null ? shader : GameRenderer.getPositionTexColorShader();
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "vessel_flame"),
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> shader = loaded);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "flame_heat_haze"),
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> heatHazeShader = loaded);
    }

    public static void billboard(VertexConsumer buffer, Matrix4f pose, float size, float alpha) {
        float half = size * 0.5F;
        buffer.addVertex(pose, -half, -half, 0).setUv(0, 1).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(pose, half, -half, 0).setUv(1, 1).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(pose, half, half, 0).setUv(1, 0).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(pose, -half, half, 0).setUv(0, 0).setColor(1.0F, 1.0F, 1.0F, alpha);
    }
}
