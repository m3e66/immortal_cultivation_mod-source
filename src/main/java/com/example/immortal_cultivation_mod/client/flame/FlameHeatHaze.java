package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/** Local heat refraction uses a separate scene snapshot, never a framebuffer feedback loop. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class FlameHeatHaze {
    private record Source(Vec3 center, float strength, float size) { }
    private static final Map<BlockPos, Source> SOURCES = new LinkedHashMap<>();
    private static ClientLevel world;
    private static TextureTarget snapshot;
    private static long lastActive;

    private FlameHeatHaze() { }

    public static void observe(FlameKind kind, Vec3 center) {
        float strength = switch (kind) {
            case CRIMSON -> .18F;
            case EARTH -> .22F;
            case DRAGON -> .25F;
            default -> 0;
        };
        observe(center, strength, 1);
    }

    public static void observeCharge(FlameKind kind, Vec3 center, float strength, float diameter) {
        observe(center, (kind == FlameKind.EARTH ? .5F : .3F) * strength, Math.max(1, diameter));
    }

    private static void observe(Vec3 center, float strength, float size) {
        if (strength == 0 || ShaderPackCompatibility.isShaderPackActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) { SOURCES.clear(); world = mc.level; }
        if (world == null || mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(center) > 24 * 24)
            return;
        if (SOURCES.size() < 48) SOURCES.put(BlockPos.containing(center), new Source(center, strength, size));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        SOURCES.clear(); world = null; release();
    }

    private static void release() {
        if (snapshot == null) return;
        int read = GL11.glGetInteger(36010), draw = GL11.glGetInteger(36006);
        snapshot.destroyBuffers(); snapshot = null;
        GlStateManager._glBindFramebuffer(36008, read);
        GlStateManager._glBindFramebuffer(36009, draw);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) { SOURCES.clear(); world = mc.level; release(); }
        if (world == null || ShaderPackCompatibility.isShaderPackActive()) {
            SOURCES.clear(); release(); return;
        }
        if (SOURCES.isEmpty()) {
            if (world.getGameTime() - lastActive > 60) release();
            return;
        }
        var hazeShader = FlameRenderTypes.heatHazeShader();
        if (hazeShader == null) { SOURCES.clear(); return; }
        lastActive = world.getGameTime();
        var main = mc.getMainRenderTarget();
        int width = Math.min(960, main.viewWidth);
        int height = Math.max(1, main.viewHeight * width / Math.max(1, main.viewWidth));
        if (width <= 0) { SOURCES.clear(); return; }
        if (snapshot == null) snapshot = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        else if (snapshot.width != width || snapshot.height != height)
            snapshot.resize(width, height, Minecraft.ON_OSX);
        snapshot.setFilterMode(9729);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        var previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        // AFTER_LEVEL fires after vanilla restores its model-view stack. Match the view used by
        // screen-space UV projection so the card and sampled scene align for every camera angle.
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.mul(event.getModelViewMatrix());
            RenderSystem.applyModelViewMatrix();
            GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
            GlStateManager._glBindFramebuffer(36009, snapshot.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.viewWidth, main.viewHeight,
                    0, 0, width, height, 16384, 9729);
            main.bindWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.enableBlend(); RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            RenderSystem.setShader(() -> hazeShader);
            RenderSystem.setShaderTexture(0, snapshot.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            int patches = 0;
            float time = world.getGameTime() % 240 + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Vec3 camera = event.getCamera().getPosition();
            for (Source source : SOURCES.values()) {
                if (patches >= 12 || !event.getFrustum().isVisible(
                        new net.minecraft.world.phys.AABB(source.center(), source.center()).inflate(source.size()))) continue;
                Vec3 relative = source.center().subtract(camera);
                Vector4f clip = projection.transform(new Vector4f((float) relative.x, (float) relative.y,
                        (float) relative.z, 1));
                if (clip.w <= .05F) continue;
                patches++;
                for (int row = 0; row < 10; row++) for (int column = 0; column < 8; column++) {
                    for (int corner = 0; corner < 4; corner++) {
                        float u = (column + (corner == 1 || corner == 2 ? 1 : 0)) / 8F;
                        float v = (row + (corner >= 2 ? 1 : 0)) / 10F;
                        Vector3f offset = new Vector3f((u - .5F) * .95F * source.size(), (.1F + v) * source.size(), 0)
                                .rotate(event.getCamera().rotation());
                        Vector3f position = new Vector3f((float) relative.x, (float) relative.y,
                                (float) relative.z).add(offset);
                        Vector4f screen = projection.transform(new Vector4f(position, 1));
                        // Fade smoothly to zero at all four boundaries; no rectangular haze card.
                        float fade = Mth.sin(u * Mth.PI) * Mth.sin(v * Mth.PI);
                        float distanceFade = Mth.clamp((24F - (float) relative.length()) / 18, 0, 1);
                        float warp = .0065F * source.strength() * fade * distanceFade;
                        float screenU = screen.w <= 0 ? .5F : screen.x / screen.w * .5F + .5F;
                        float screenV = screen.w <= 0 ? .5F : screen.y / screen.w * .5F + .5F;
                        float sampleU = Mth.clamp(screenU + Mth.sin(v * 23 + time * .38F + u * 9) * warp, 0, 1);
                        float sampleV = Mth.clamp(screenV + Mth.cos(u * 17 + time * .21F) * warp * .4F, 0, 1);
                        buffer.addVertex(position.x, position.y, position.z).setUv(sampleU, sampleV)
                                .setColor(1F, 1F, 1F, source.strength() * fade * distanceFade);
                    }
                }
            }
            if (patches > 0) BufferUploader.drawWithShader(buffer.buildOrThrow());
            else buffer.build();
        } finally {
            SOURCES.clear();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.depthMask(true);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.defaultBlendFunc();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            main.bindWrite(true);
        }
    }
}
