package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Layered skybox: a stable equirectangular panorama, parallax stars, and sparse local void currents. */
public final class CosmicVoidSkyRenderer {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2.0F;
    private static final int COMPATIBILITY_LATITUDE_SEGMENTS = 18;
    private static final int COMPATIBILITY_LONGITUDE_SEGMENTS = 36;
    private static final ResourceLocation SKYBOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/environment/cosmic_void_sky.png");
    private static final ResourceLocation STAR_GLOW = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/environment/void_star_glow.png");
    private static final List<Star> NEAR_STARS = createStars();
    private CosmicVoidSkyRenderer() {
    }

    public static void render(ClientLevel level, int ticks, float partialTick,
                              Matrix4f modelViewMatrix, Camera camera) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) {
            return;
        }
        float time = ticks + partialTick;
        Vec3 cameraPos = camera.getPosition();

        beginSkyPass();
        try {
            float dayTime = TimeFastForwardClientState.interpolatedTimeOfDay(
                    level.getDayTime(), partialTick);
            boolean earthNight = dayTime >= 13_000.0F && dayTime < 23_000.0F;
            float dayOrbitAngle = (dayTime / 24_000.0F) * TAU;
            renderSkyboxWithTrails(modelViewMatrix, 1.0F, dayOrbitAngle);

            Matrix4f starMatrix = new Matrix4f(modelViewMatrix)
                    .translate(
                            (float) -Math.sin(cameraPos.x * 0.003D) * 0.85F,
                            (float) -Math.sin(cameraPos.y * 0.004D) * 0.45F,
                            (float) -Math.sin(cameraPos.z * 0.003D) * 0.85F);
            renderNearStars(starMatrix, time, dayOrbitAngle, 1.0F);

            Matrix4f currentMatrix = new Matrix4f(modelViewMatrix)
                    .translate(
                            (float) -Math.sin(cameraPos.x * 0.009D) * 1.75F,
                            (float) -Math.sin(cameraPos.y * 0.011D) * 0.95F,
                            (float) -Math.sin(cameraPos.z * 0.009D) * 1.75F);
            renderVoidCurrents(currentMatrix, time, cameraPos);
            CosmicVoidDynamicVfxRenderer.render(
                    modelViewMatrix, time, cameraPos, 0.0F, 0.0F,
                    earthNight, dayOrbitAngle);
        } finally {
            endSkyPass();
        }
    }

    /** Draws only the distant panorama so normal dimensions can reuse it as a night sky. */
    public static void renderPanorama(
            Matrix4f matrix, float alpha, float time, float dayOrbitAngle) {
        if (alpha <= 0.001F) {
            return;
        }
        if (ShaderPackCompatibility.isRenderingShadowPass()) {
            return;
        }
        beginSkyPass();
        try {
            renderSkyboxWithTrails(matrix, alpha, dayOrbitAngle);
            renderNearStars(matrix, time, dayOrbitAngle, alpha * 0.72F);
        } finally {
            endSkyPass();
        }
    }

    private static void beginSkyPass() {
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void endSkyPass() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderSkyboxWithTrails(
            Matrix4f matrix, float alpha, float dayOrbitAngle) {
        float trailLength = TimeFastForwardClientState.trailLengthMultiplier();
        float trailSpan = trailLength > 0.0F
                ? 0.16F * trailLength
                : 0.0F;
        renderSkybox(matrix, alpha, dayOrbitAngle, trailSpan);
    }

    private static void renderSkybox(
            Matrix4f matrix, float alpha, float sampleRotation,
            float trailSpan) {
        // A skybox follows camera rotation, but never camera translation or animated drift.
        Matrix4f skyMatrix = new Matrix4f(matrix)
                .m30(0.0F)
                .m31(0.0F)
                .m32(0.0F);
        if (!ShaderPackCompatibility.useCustomCoreShaders()
                || CosmicVoidSkyShaders.skyShader() == null) {
            renderCompatibleSkySphere(skyMatrix, alpha, sampleRotation);
            return;
        }
        RenderSystem.setShader(CosmicVoidSkyShaders::skyShader);
        CosmicVoidTextureFiltering.bindSkybox(0, SKYBOX_TEXTURE);
        // The cube is wound inward. Cull its rear faces so the opposite side cannot
        // overwrite the world-facing side while depth testing is disabled.
        RenderSystem.enableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        // This layer has depth testing disabled and follows camera rotation only,
        // so it does not need to sit near the far plane. Keeping every cube corner
        // inside 28 blocks prevents boosted flight FOV from clipping faces into
        // the large black boxes/triangles seen in the panorama.
        float radius = 16.0F;
        addSkyboxFace(buffer, skyMatrix, radius, alpha, sampleRotation, trailSpan,
                1, 1, 1, 1, 1, -1, 1, -1, -1, 1, -1, 1); // +X
        addSkyboxFace(buffer, skyMatrix, radius, alpha, sampleRotation, trailSpan,
                -1, 1, -1, -1, 1, 1, -1, -1, 1, -1, -1, -1); // -X
        addSkyboxFace(buffer, skyMatrix, radius, alpha, sampleRotation, trailSpan,
                -1, 1, -1, 1, 1, -1, 1, 1, 1, -1, 1, 1); // +Y
        addSkyboxFace(buffer, skyMatrix, radius, alpha, sampleRotation, trailSpan,
                -1, -1, 1, 1, -1, 1, 1, -1, -1, -1, -1, -1); // -Y
        addSkyboxFace(buffer, skyMatrix, radius, alpha, sampleRotation, trailSpan,
                -1, 1, 1, 1, 1, 1, 1, -1, 1, -1, -1, 1); // +Z
        addSkyboxFace(buffer, skyMatrix, radius, alpha, sampleRotation, trailSpan,
                1, 1, -1, -1, 1, -1, -1, -1, -1, 1, -1, -1); // -Z
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableCull();
    }

    /**
     * Shader packs cannot safely consume the custom panorama shader's overloaded
     * color/UV channels. This tessellated sphere performs the equirectangular mapping
     * in Java and submits ordinary textured vertices through Minecraft's shader path.
     */
    private static void renderCompatibleSkySphere(
            Matrix4f matrix, float alpha, float sampleRotation) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bindSkybox(0, SKYBOX_TEXTURE);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float radius = 16.0F;
        for (int latitude = 0;
             latitude < COMPATIBILITY_LATITUDE_SEGMENTS; latitude++) {
            float latitude0 = -PI * 0.5F
                    + PI * latitude / COMPATIBILITY_LATITUDE_SEGMENTS;
            float latitude1 = -PI * 0.5F
                    + PI * (latitude + 1) / COMPATIBILITY_LATITUDE_SEGMENTS;
            float v0 = 1.0F - latitude / (float) COMPATIBILITY_LATITUDE_SEGMENTS;
            float v1 = 1.0F
                    - (latitude + 1) / (float) COMPATIBILITY_LATITUDE_SEGMENTS;
            for (int longitude = 0;
                 longitude < COMPATIBILITY_LONGITUDE_SEGMENTS; longitude++) {
                float baseLongitude0 = -PI
                        + TAU * longitude / COMPATIBILITY_LONGITUDE_SEGMENTS;
                float baseLongitude1 = -PI
                        + TAU * (longitude + 1) / COMPATIBILITY_LONGITUDE_SEGMENTS;
                float longitude0 = baseLongitude0 - sampleRotation;
                float longitude1 = baseLongitude1 - sampleRotation;
                float u0 = longitude / (float) COMPATIBILITY_LONGITUDE_SEGMENTS;
                float u1 = (longitude + 1)
                        / (float) COMPATIBILITY_LONGITUDE_SEGMENTS;
                addPanoramaVertex(buffer, matrix, radius, latitude0, longitude0,
                        u0, v0, alpha);
                addPanoramaVertex(buffer, matrix, radius, latitude0, longitude1,
                        u1, v0, alpha);
                addPanoramaVertex(buffer, matrix, radius, latitude1, longitude1,
                        u1, v1, alpha);
                addPanoramaVertex(buffer, matrix, radius, latitude1, longitude0,
                        u0, v1, alpha);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addPanoramaVertex(
            BufferBuilder buffer, Matrix4f matrix, float radius,
            float latitude, float longitude, float u, float v, float alpha) {
        float horizontal = (float) Math.cos(latitude);
        float x = horizontal * (float) Math.cos(longitude);
        float y = (float) Math.sin(latitude);
        float z = horizontal * (float) Math.sin(longitude);
        buffer.addVertex(matrix, x * radius, y * radius, z * radius)
                .setUv(u, v)
                .setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    private static void addSkyboxFace(BufferBuilder buffer, Matrix4f matrix, float radius,
                                      float alpha, float sampleRotation, float trailSpan,
                                      float x0, float y0, float z0,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float x3, float y3, float z3) {
        addSkyboxVertex(buffer, matrix, radius, alpha,
                sampleRotation, trailSpan, x0, y0, z0);
        addSkyboxVertex(buffer, matrix, radius, alpha,
                sampleRotation, trailSpan, x1, y1, z1);
        addSkyboxVertex(buffer, matrix, radius, alpha,
                sampleRotation, trailSpan, x2, y2, z2);
        addSkyboxVertex(buffer, matrix, radius, alpha,
                sampleRotation, trailSpan, x3, y3, z3);
    }

    private static void addSkyboxVertex(BufferBuilder buffer, Matrix4f matrix, float radius,
                                        float alpha, float sampleRotation, float trailSpan,
                                        float x, float y, float z) {
        buffer.addVertex(matrix, x * radius, y * radius, z * radius)
                // Constant per-face values carry rotation and motion-blur distance.
                .setUv(sampleRotation, trailSpan)
                // Keep projection direction fixed; the shader scrolls only longitude.
                .setColor(x * 0.5F + 0.5F, y * 0.5F + 0.5F,
                        z * 0.5F + 0.5F, alpha);
    }

    private static void renderNearStars(
            Matrix4f matrix, float time, float dayOrbitAngle, float layerAlpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float trailLength = TimeFastForwardClientState.trailLengthMultiplier();
        float trailSpan = trailLength > 0.0F
                ? 0.16F * trailLength
                : 0.0F;
        float trailAlpha = Math.min(1.0F, trailLength);
        for (Star star : NEAR_STARS) {
            float pulse = 0.78F + 0.22F * (float) Math.sin(time * star.twinkleSpeed + star.phase);
            if (!star.moving) {
                pulse = 0.94F + pulse * 0.06F;
            }
            float alpha = star.alpha * pulse * layerAlpha;
            float size = star.size * (0.96F + pulse * 0.04F);
            StarFrame current = starFrame(star, time, dayOrbitAngle);
            if (trailSpan > 0.0F) {
                StarFrame previous = starFrame(
                        star, time, dayOrbitAngle - trailSpan);
                addTrailRibbon(buffer, matrix, previous.center, current.center,
                        Math.max(0.10F, size * 1.10F),
                        star.red, star.green, star.blue,
                        alpha * 0.90F * trailAlpha);
            }
            float movingScale = star.moving ? 1.25F : 1.0F;
            addGlowingStar(buffer, matrix, current.center, current.right, current.up,
                    size * movingScale,
                    star.red, star.green, star.blue,
                    Math.min(1.0F, alpha * (star.moving ? 1.38F : 1.0F)));
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static StarFrame starFrame(Star star, float time, float dayOrbitAngle) {
        Vector3f direction = new Vector3f(star.x, star.y, star.z)
                .normalize()
                .rotateY(dayOrbitAngle);
        Vector3f reference = Math.abs(direction.y) > 0.88F
                ? new Vector3f(1.0F, 0.0F, 0.0F)
                : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = direction.cross(reference, new Vector3f()).normalize();
        Vector3f up = right.cross(direction, new Vector3f()).normalize();
        Vector3f center = new Vector3f(direction).mul(star.distance);
        if (star.moving) {
            float orbit = time * star.driftSpeed + star.phase;
            center.fma((float) Math.sin(orbit) * star.driftRadius, right)
                    .fma((float) Math.cos(orbit * 0.83F)
                            * star.driftRadius * 0.58F, up);
        }
        return new StarFrame(center, right, up);
    }

    /** Layered halos keep sparse stars luminous without adding more stars to the sky. */
    private static void addGlowingStar(BufferBuilder buffer, Matrix4f matrix,
                                       Vector3f center, Vector3f right, Vector3f up,
                                       float size, float red, float green, float blue, float alpha) {
        addBillboard(buffer, matrix, center, right, up, size * 3.85F,
                red * 0.72F, green * 0.78F, Math.min(1.0F, blue * 1.06F), alpha * 0.36F);
        addBillboard(buffer, matrix, center, right, up, size * 1.98F,
                red, green, blue, alpha * 0.76F);
        addBillboard(buffer, matrix, center, right, up, size * 0.58F,
                1.0F, 0.98F, 1.0F, Math.min(1.0F, alpha * 2.05F));
    }

    private static void addBillboard(BufferBuilder buffer, Matrix4f matrix,
                                     Vector3f center, Vector3f right, Vector3f up,
                                     float size, float red, float green, float blue, float alpha) {
        Vector3f lowerLeft = new Vector3f(center).sub(new Vector3f(right).mul(size))
                .sub(new Vector3f(up).mul(size));
        Vector3f lowerRight = new Vector3f(center).add(new Vector3f(right).mul(size))
                .sub(new Vector3f(up).mul(size));
        Vector3f upperRight = new Vector3f(center).add(new Vector3f(right).mul(size))
                .add(new Vector3f(up).mul(size));
        Vector3f upperLeft = new Vector3f(center).sub(new Vector3f(right).mul(size))
                .add(new Vector3f(up).mul(size));
        buffer.addVertex(matrix, lowerLeft.x, lowerLeft.y, lowerLeft.z)
                .setUv(0.0F, 1.0F).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, lowerRight.x, lowerRight.y, lowerRight.z)
                .setUv(1.0F, 1.0F).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, upperRight.x, upperRight.y, upperRight.z)
                .setUv(1.0F, 0.0F).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, upperLeft.x, upperLeft.y, upperLeft.z)
                .setUv(0.0F, 0.0F).setColor(red, green, blue, alpha);
    }

    private static void addTrailRibbon(
            BufferBuilder buffer, Matrix4f matrix,
            Vector3f tail, Vector3f head, float halfWidth,
            float red, float green, float blue, float alpha) {
        CosmicTrailGeometry.addCurvedRibbon(
                buffer, matrix, tail, head, halfWidth,
                red, green, blue, alpha);
    }

    private static void renderVoidCurrents(Matrix4f matrix, float time, Vec3 cameraPos) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(1.4F);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int current = 0; current < 8; current++) {
            double seed = current * 19.731D;
            float distance = 18.0F + current % 6 * 4.2F;
            float yaw = (float) (seed + time * (0.00042D + (current % 3) * 0.00009D)
                    + Math.sin(cameraPos.x * 0.0008D) * 0.06D);
            float pitch = (float) (Math.sin(seed * 0.71D + time * 0.00031D) * 0.72D);
            Vector3f direction = direction(yaw, pitch);
            Vector3f tangent = new Vector3f(-direction.z, 0.15F + current % 2 * 0.08F,
                    direction.x).normalize();
            Vector3f center = new Vector3f(direction).mul(distance);
            for (int segment = 0; segment < 5; segment++) {
                float start = (segment - 2.5F) * 0.94F;
                float end = start + 0.68F;
                float curve0 = (float) Math.sin(seed + segment * 0.8D + time * 0.008D) * 0.18F;
                float curve1 = (float) Math.sin(seed + (segment + 1) * 0.8D + time * 0.008D) * 0.18F;
                Vector3f a = new Vector3f(center).fma(start, tangent).fma(curve0, direction);
                Vector3f b = new Vector3f(center).fma(end, tangent).fma(curve1, direction);
                float alpha = 0.075F + (current % 4) * 0.012F;
                buffer.addVertex(matrix, a.x, a.y, a.z).setColor(0.45F, 0.58F, 0.82F, alpha);
                buffer.addVertex(matrix, b.x, b.y, b.z).setColor(0.62F, 0.48F, 0.88F, 0.0F);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static Vector3f direction(float yaw, float pitch) {
        float cosPitch = (float) Math.cos(pitch);
        return new Vector3f(
                cosPitch * (float) Math.cos(yaw),
                (float) Math.sin(pitch),
                cosPitch * (float) Math.sin(yaw));
    }

    private static List<Star> createStars() {
        Random random = new Random(0x564F4944534B594CL);
        List<Star> stars = new ArrayList<>();
        for (int index = 0; index < 1152; index++) {
            double yaw = random.nextDouble() * Math.PI * 2.0D;
            double y = random.nextDouble() * 1.96D - 0.98D;
            double horizontal = Math.sqrt(1.0D - y * y);
            float blueShift = random.nextFloat();
            float sizeRoll = random.nextFloat();
            boolean large = sizeRoll > 0.985F;
            boolean medium = !large && sizeRoll > 0.84F;
            boolean moving = index % 37 == 0;
            float size = large
                    ? 0.17F + random.nextFloat() * 0.19F
                    : medium
                    ? 0.060F + random.nextFloat() * 0.095F
                    : 0.014F + random.nextFloat() * 0.044F;
            float alpha = large
                    ? 0.42F + random.nextFloat() * 0.28F
                    : medium
                    ? 0.22F + random.nextFloat() * 0.25F
                    : 0.09F + random.nextFloat() * 0.20F;
            stars.add(new Star(
                    (float) (Math.cos(yaw) * horizontal),
                    (float) y,
                    (float) (Math.sin(yaw) * horizontal),
                    69.0F + random.nextFloat() * 18.0F,
                    size,
                    alpha,
                    0.78F + blueShift * 0.16F,
                    0.82F + blueShift * 0.12F,
                    0.90F + blueShift * 0.10F,
                    0.018F + random.nextFloat() * 0.022F,
                    random.nextFloat() * ((float) Math.PI * 2.0F),
                    moving,
                    moving ? 0.20F + random.nextFloat() * 0.42F : 0.0F,
                    moving ? 0.0012F + random.nextFloat() * 0.0014F : 0.0F));
        }
        return List.copyOf(stars);
    }

    private record Star(float x, float y, float z, float distance, float size,
                        float alpha, float red, float green, float blue,
                        float twinkleSpeed, float phase, boolean moving,
                        float driftRadius, float driftSpeed) {
    }

    private record StarFrame(Vector3f center, Vector3f right, Vector3f up) {
    }
}
