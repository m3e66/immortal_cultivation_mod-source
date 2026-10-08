package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.client.renderer.BlackFlameDeathRenderer;
import com.example.immortal_cultivation_mod.network.BlackFlamePayloads;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import software.bernie.geckolib.event.GeoRenderEvent;

/** Client presentation for progressive terrain scorching and 玄焰 deaths. */
public final class BlackFlameClient {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType SCORCH_RENDER_TYPE =
            RenderType.entityTranslucent(WHITE_TEXTURE);
    private static final int BLOCK_VISUAL_LIFETIME = 30;
    private static final int MAX_BLOCK_VISUALS = 768;
    private static final Map<Long, ScorchVisual> SCORCHED_BLOCKS = new HashMap<>();
    private static final Map<Integer, DeathVisual> DEATHS = new HashMap<>();

    private BlackFlameClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BlackFlameClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(BlackFlameClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(BlackFlameClient::onGeoEntityPre);
        NeoForge.EVENT_BUS.addListener(BlackFlameDeathRenderer::onGeoEntityPost);
    }

    public static void acceptBlockVisuals(List<BlackFlamePayloads.BlockVisual> visuals) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        for (BlackFlamePayloads.BlockVisual visual : visuals) {
            float progress = Mth.clamp(visual.progress(), 0.0F, 1.0F);
            SCORCHED_BLOCKS.put(visual.position(), new ScorchVisual(progress, now));
        }
        if (SCORCHED_BLOCKS.size() > MAX_BLOCK_VISUALS) {
            SCORCHED_BLOCKS.entrySet().removeIf(entry -> now - entry.getValue().updatedAt > 10);
            int excess = SCORCHED_BLOCKS.size() - MAX_BLOCK_VISUALS;
            var iterator = SCORCHED_BLOCKS.keySet().iterator();
            while (excess-- > 0 && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    public static void beginDeath(int entityId, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        DEATHS.put(entityId, new DeathVisual(
                minecraft.level.getGameTime(), Math.max(1, durationTicks)));
    }

    public static DeathAppearance deathAppearance(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity == null) return null;
        DeathVisual visual = DEATHS.get(entity.getId());
        if (visual == null) return null;
        float age = minecraft.level.getGameTime() + partialTick - visual.startedAt;
        if (age < 0.0F || age > visual.durationTicks + 4.0F) return null;

        float collapse = Mth.clamp((age - 6.0F)
                / Math.max(1.0F, visual.durationTicks - 6.0F), 0.0F, 1.0F);
        float scale = Mth.lerp(collapse * collapse, 1.0F, 0.08F);
        int argb;
        if (age < 2.0F) {
            argb = 0;
        } else if (age < 4.0F) {
            // At 0.10 seconds the original color is almost fully covered by
            // a flat grayscale body pass rather than merely tinted darker.
            int alpha = Mth.clamp(Math.round(220.0F + (age - 2.0F) * 8.0F),
                    220, 236);
            argb = alpha << 24 | 0x006B696E;
        } else if (age < 6.0F) {
            int alpha = Mth.clamp(Math.round(232.0F + (age - 4.0F) * 7.0F),
                    232, 246);
            argb = alpha << 24 | 0x00100A16;
        } else {
            int alpha = Mth.clamp(Math.round(210.0F + collapse * 38.0F), 210, 248);
            argb = alpha << 24 | 0x0009040F;
        }
        return new DeathAppearance(age, scale, argb);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            SCORCHED_BLOCKS.clear();
            DEATHS.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        SCORCHED_BLOCKS.entrySet().removeIf(entry ->
                now - entry.getValue().updatedAt > (entry.getValue().progress >= 1.0F
                        ? 8 : BLOCK_VISUAL_LIFETIME));
        DEATHS.entrySet().removeIf(entry ->
                now - entry.getValue().startedAt > entry.getValue().durationTicks + 5L);
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || SCORCHED_BLOCKS.isEmpty()
                || ShaderPackCompatibility.isRenderingShadowPass()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(SCORCH_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<Long, ScorchVisual> entry : SCORCHED_BLOCKS.entrySet()) {
            BlockPos block = BlockPos.of(entry.getKey());
            double distanceSquared = block.distToCenterSqr(
                    cameraPosition.x, cameraPosition.y, cameraPosition.z);
            if (distanceSquared > 128.0D * 128.0D) continue;
            ScorchVisual visual = entry.getValue();
            if (minecraft.level.getBlockState(block).isAir()) continue;
            boolean north = !minecraft.level.getBlockState(block.north()).canOcclude();
            boolean south = !minecraft.level.getBlockState(block.south()).canOcclude();
            boolean west = !minecraft.level.getBlockState(block.west()).canOcclude();
            boolean east = !minecraft.level.getBlockState(block.east()).canOcclude();
            boolean up = !minecraft.level.getBlockState(block.above()).canOcclude();
            boolean down = !minecraft.level.getBlockState(block.below()).canOcclude();
            if (!(north || south || west || east || up || down)) continue;
            float progress = visual.progress;
            float alpha = 0.10F + progress * 0.70F;
            float gray = Mth.lerp(progress, 0.62F, 0.018F);
            float red = gray;
            float green = gray * (1.0F - progress * 0.44F);
            float blue = Math.min(0.18F, gray * 1.06F + progress * 0.035F);
            float minX = (float) (block.getX() - cameraPosition.x) - 0.002F;
            float minY = (float) (block.getY() - cameraPosition.y) - 0.002F;
            float minZ = (float) (block.getZ() - cameraPosition.z) - 0.002F;
            renderCube(vertices, matrix, minX, minY, minZ,
                    1.004F, red, green, blue, alpha,
                    north, south, west, east, up, down);
        }
        buffers.endBatch(SCORCH_RENDER_TYPE);
    }

    private static void onGeoEntityPre(GeoRenderEvent.Entity.Pre event) {
        DeathAppearance appearance = deathAppearance(event.getEntity(), event.getPartialTick());
        if (appearance == null || appearance.scale >= 0.999F) return;
        event.getPoseStack().scale(appearance.scale, appearance.scale, appearance.scale);
    }

    private static void renderCube(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float z, float size,
            float red, float green, float blue, float alpha,
            boolean north, boolean south, boolean west, boolean east,
            boolean up, boolean down) {
        float x2 = x + size;
        float y2 = y + size;
        float z2 = z + size;
        if (north) face(vertices, matrix,
                x, y, z, x2, y, z, x2, y2, z, x, y2, z,
                red, green, blue, alpha, 0.0F, 0.0F, -1.0F);
        if (south) face(vertices, matrix,
                x2, y, z2, x, y, z2, x, y2, z2, x2, y2, z2,
                red, green, blue, alpha, 0.0F, 0.0F, 1.0F);
        if (west) face(vertices, matrix,
                x, y, z2, x, y, z, x, y2, z, x, y2, z2,
                red, green, blue, alpha, -1.0F, 0.0F, 0.0F);
        if (east) face(vertices, matrix,
                x2, y, z, x2, y, z2, x2, y2, z2, x2, y2, z,
                red, green, blue, alpha, 1.0F, 0.0F, 0.0F);
        if (up) face(vertices, matrix,
                x, y2, z, x2, y2, z, x2, y2, z2, x, y2, z2,
                red, green, blue, alpha, 0.0F, 1.0F, 0.0F);
        if (down) face(vertices, matrix,
                x, y, z2, x2, y, z2, x2, y, z, x, y, z,
                red, green, blue, alpha, 0.0F, -1.0F, 0.0F);
    }

    private static void face(
            VertexConsumer vertices, Matrix4f matrix,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float red, float green, float blue, float alpha,
            float normalX, float normalY, float normalZ) {
        vertex(vertices, matrix, ax, ay, az, 0.0F, 0.0F,
                red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(vertices, matrix, bx, by, bz, 1.0F, 0.0F,
                red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(vertices, matrix, cx, cy, cz, 1.0F, 1.0F,
                red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(vertices, matrix, dx, dy, dz, 0.0F, 1.0F,
                red, green, blue, alpha, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float z, float u, float v,
            float red, float green, float blue, float alpha,
            float normalX, float normalY, float normalZ) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(normalX, normalY, normalZ);
    }

    private record ScorchVisual(float progress, long updatedAt) {
    }

    private record DeathVisual(long startedAt, int durationTicks) {
    }

    public record DeathAppearance(float age, float scale, int argb) {
    }
}
