package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.flame.SpiritFlameBurnEffect;
import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Quaternionf;

/** Recolors vanilla animated fire, preserving its silhouette and animation frames. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BurningFlameVisuals {
    private static ShaderInstance shader;
    private static final RenderType TYPE = RenderType.create("cultivation_burning_flame",
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 2048, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(BurningFlameVisuals::shader))
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));
    private BurningFlameVisuals() { }

    public static ShaderInstance shader() { return shader != null ? shader : GameRenderer.getPositionTexColorShader(); }

    @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "burning_flame"),
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> shader = loaded);
    }

    public static int color(LivingEntity entity) {
        var kind = SpiritFlameBurnEffect.kind(entity);
        if (kind != null) return kind.color();
        var qiBurn = entity.getEffect(ModEffects.QI_FIRE_BURN);
        return qiBurn == null ? -1 : qiBurn.getAmplifier() > 0 ? 0xFF492A : 0x39BFCF;
    }

    public static void render(PoseStack poses, MultiBufferSource buffers, LivingEntity entity, Quaternionf rotation) {
        int tint = 0xFF000000 | color(entity);
        var first = ModelBakery.FIRE_0.sprite();
        var second = ModelBakery.FIRE_1.sprite();
        poses.pushPose();
        float scale = entity.getBbWidth() * 1.4F;
        poses.scale(scale, scale, scale);
        float remaining = entity.getBbHeight() / scale, halfWidth = .5F, offset = 0, depth = 0;
        poses.mulPose(rotation);
        poses.translate(0, 0, .3F - (int)remaining * .02F);
        var out = buffers.getBuffer(TYPE);
        for (int layer = 0; remaining > 0; layer++) {
            var sprite = layer % 2 == 0 ? first : second;
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            if (layer / 2 % 2 == 0) { float swap = u0; u0 = u1; u1 = swap; }
            var pose = poses.last().pose();
            out.addVertex(pose, -halfWidth, -offset, depth).setUv(u1, sprite.getV1()).setColor(tint);
            out.addVertex(pose, halfWidth, -offset, depth).setUv(u0, sprite.getV1()).setColor(tint);
            out.addVertex(pose, halfWidth, 1.4F-offset, depth).setUv(u0, sprite.getV0()).setColor(tint);
            out.addVertex(pose, -halfWidth, 1.4F-offset, depth).setUv(u1, sprite.getV0()).setColor(tint);
            remaining -= .45F; offset -= .45F; halfWidth *= .9F; depth -= .03F;
        }
        poses.popPose();
    }
}
