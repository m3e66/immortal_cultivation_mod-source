package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Small fullbright shapes built directly from vertices; no flame bitmap or vanilla fire texture. */
final class FlameAccentParticle extends Particle {
    enum Shape {
        EMBER, BURST, MOLTEN, SMOKE, MOTE, LEAF, SOLAR_SPARK,
        GHOST, SOUL, MIST, ELECTRIC, CHARGED, SCALE, STAR, PULSE
    }

    private static final float[][] DIAMOND = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
    private static final float[][] LEAF = {
            {0, 1}, {-0.48F, 0.5F}, {-0.65F, -0.1F}, {-0.35F, -0.65F},
            {0, -1}, {0.35F, -0.65F}, {0.65F, -0.1F}, {0.48F, 0.5F}};
    private static final float[][] STAR = {
            {0, 1}, {-0.18F, 0.18F}, {-1, 0}, {-0.18F, -0.18F},
            {0, -1}, {0.18F, -0.18F}, {1, 0}, {0.18F, 0.18F}};
    private static final float[][] SCALE = {{0, 0.65F}, {-0.72F, 0.1F}, {-0.45F, -0.8F},
            {0, -1}, {0.45F, -0.8F}, {0.72F, 0.1F}};
    private static final float[][] HALO = new float[12][2];
    private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override public String toString() { return "CULTIVATION_FLAME_ACCENT"; }
    };

    static {
        for (int i = 0; i < HALO.length; i++) {
            float angle = (float) (Math.PI * 2 * i / HALO.length);
            HALO[i][0] = Mth.cos(angle);
            HALO[i][1] = Mth.sin(angle);
        }
    }

    private final Vec3 center;
    private final Shape shape;
    private final float phase;
    private final float radius;
    private final float travel;
    private final float size;
    private final int color;

    FlameAccentParticle(ClientLevel level, Vec3 center, FlameKind kind, Shape shape) {
        this(level, center, kind, shape, -1);
    }

    FlameAccentParticle(ClientLevel level, Vec3 center, FlameKind kind, Shape shape, int tint) {
        super(level, center.x, center.y, center.z);
        this.center = center;
        this.shape = shape;
        hasPhysics = false;
        gravity = 0;
        phase = random.nextFloat() * Mth.TWO_PI;
        radius = 0.14F + random.nextFloat() * 0.18F;
        travel = 0.5F + random.nextFloat() * 0.7F;
        size = switch (shape) {
            case SMOKE, MIST -> 0.065F + random.nextFloat() * 0.025F;
            case LEAF -> 0.055F + random.nextFloat() * 0.025F;
            case GHOST -> 0.035F + random.nextFloat() * 0.015F;
            case SCALE -> 0.032F + random.nextFloat() * 0.018F;
            case PULSE -> 0.024F;
            default -> 0.013F + random.nextFloat() * 0.018F;
        };
        lifetime = switch (shape) {
            case ELECTRIC -> 7 + random.nextInt(4);
            case BURST, SOLAR_SPARK -> 12 + random.nextInt(7);
            case PULSE -> 16;
            case MOLTEN -> 34 + random.nextInt(7);
            case LEAF, GHOST, MIST -> 38 + random.nextInt(12);
            default -> 26 + random.nextInt(12);
        };
        color = tint >= 0 ? tint : switch (shape) {
            case EMBER -> kind == FlameKind.DRAGON && random.nextBoolean() ? 0xFFD264 : 0xFFA047;
            case BURST -> 0xFF602B;
            case MOLTEN -> 0xFF921F;
            case SMOKE -> 0x302126;
            case MOTE -> 0xB4FFD3;
            case LEAF -> 0x61ECA0;
            case SOLAR_SPARK -> 0xFFE7A0;
            case GHOST -> 0x7C85FF;
            case SOUL -> 0xBA8AFF;
            case MIST -> 0x261238;
            case ELECTRIC -> 0xD9E8FF;
            case CHARGED -> 0x9C98FF;
            case SCALE -> random.nextBoolean() ? 0xFFE078 : 0xFF6C3A;
            case STAR -> random.nextBoolean() ? 0xFFE397 : 0xC09AFF;
            case PULSE -> 0xD8B7FF;
        };
        updatePosition(0);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime || !level.hasChunkAt(BlockPos.containing(center))) {
            remove();
            return;
        }
        updatePosition((float) age / lifetime);
        // Molten fragments dissolve above the support instead of passing through the ground.
        if (shape == Shape.MOLTEN && y < center.y - 0.49) remove();
    }

    private void updatePosition(float progress) {
        float angle = phase + age * 0.09F;
        double dx = 0;
        double dy = 0;
        double dz = 0;
        switch (shape) {
            case EMBER -> {
                dx = Mth.cos(angle) * radius * (0.4 + progress * 0.5);
                dz = Mth.sin(angle) * radius * (0.4 + progress * 0.5);
                dy = progress * travel;
            }
            case BURST, SOLAR_SPARK -> {
                double outward = radius * 0.3 + progress * (shape == Shape.BURST ? 0.65 : 0.5);
                dx = Mth.cos(phase) * outward;
                dz = Mth.sin(phase) * outward;
                dy = progress * 0.24 - progress * progress * 0.12;
            }
            case MOLTEN -> {
                angle = phase + age * 0.045F;
                dx = Mth.cos(angle) * radius;
                dz = Mth.sin(angle) * radius;
                double fall = Math.max(0, progress - 0.45) / 0.55;
                dy = 0.08 + Mth.sin(angle * 1.2F) * 0.055 - fall * fall * 0.63;
            }
            case SMOKE -> {
                dx = Mth.cos(angle * 0.4F) * (radius + progress * 0.16);
                dz = Mth.sin(angle * 0.4F) * radius;
                dy = 0.12 + progress * 0.64;
            }
            case MOTE, LEAF -> {
                angle = phase + age * (shape == Shape.LEAF ? 0.065F : 0.12F);
                double spiral = radius * (1 - progress * 0.35);
                dx = Mth.cos(angle) * spiral;
                dz = Mth.sin(angle) * spiral;
                dy = progress * 0.78 + Mth.sin(angle) * 0.025;
            }
            case GHOST -> {
                angle = phase - age * 0.035F;
                double orbit = (radius + 0.26) * (1 - progress * progress);
                dx = Mth.cos(angle) * orbit;
                dz = Mth.sin(angle) * orbit;
                dy = Mth.sin(angle * 1.5F) * 0.16 * (1 - progress);
            }
            case SOUL, MIST -> {
                angle = phase - age * 0.045F;
                dx = Mth.cos(angle) * (radius + 0.08);
                dz = Mth.sin(angle) * (radius + 0.08);
                dy = Mth.sin(phase + age * 0.038F) * 0.13 - progress * 0.24;
            }
            case ELECTRIC -> {
                dx = Mth.cos(phase) * radius;
                dz = Mth.sin(phase) * radius;
                dy = Mth.sin(phase * 2) * 0.14;
            }
            case CHARGED -> {
                angle = phase + age * 0.22F;
                dx = Mth.cos(angle) * 0.36;
                dz = Mth.sin(angle) * 0.36;
                dy = Mth.sin(angle * 2) * 0.025;
            }
            case SCALE -> {
                angle = phase + age * 0.15F;
                double spiral = radius * (0.7 + progress * 0.75);
                dx = Mth.cos(angle) * spiral;
                dz = Mth.sin(angle) * spiral;
                dy = progress * 0.68;
            }
            case STAR -> {
                angle = phase + age * 0.018F;
                double outward = radius + progress * 0.48;
                dx = Mth.cos(angle) * outward;
                dz = Mth.sin(angle) * outward;
                dy = Mth.sin(phase) * progress * 0.36;
            }
            case PULSE -> { }
        }
        setPos(center.x + dx, center.y + dy, center.z + dz);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0, 1);
        float fade = Math.min(progress * 5, 1) * (1 - progress);
        if (shape == Shape.BURST || shape == Shape.ELECTRIC) fade = 1 - progress;
        float opacity = switch (shape) {
            case SMOKE, MIST -> fade * 0.28F;
            case LEAF -> fade * 0.7F;
            case GHOST -> fade * 0.56F;
            default -> fade * 0.95F;
        };
        Vec3 view = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - view.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - view.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - view.z);
        Quaternionf rotation = new Quaternionf(camera.rotation());
        if (shape == Shape.LEAF || shape == Shape.SCALE || shape == Shape.STAR)
            rotation.rotateZ(phase + (age + partialTick) * 0.065F);
        float displaySize = size * (1 - progress * 0.35F);
        if (shape == Shape.SMOKE || shape == Shape.MIST) displaySize = size * (0.65F + progress * 1.4F);
        switch (shape) {
            case SMOKE, MIST -> fan(buffer, rotation, px, py, pz, HALO,
                    displaySize, displaySize * 0.8F, color, opacity, 0);
            case LEAF -> {
                fan(buffer, rotation, px, py, pz, LEAF, displaySize, displaySize,
                        color, opacity, opacity * 0.5F);
                line(buffer, rotation, px, py, pz, 0, -displaySize * 0.7F,
                        0, displaySize * 0.75F, displaySize * 0.035F, 0xD0FFE0, opacity);
            }
            case SCALE -> {
                fan(buffer, rotation, px, py, pz, SCALE, displaySize, displaySize,
                        0xFFE4A6, opacity, opacity * 0.45F);
                fan(buffer, rotation, px, py, pz, SCALE, displaySize * 0.68F,
                        displaySize * 0.62F, color, opacity, opacity * 0.6F);
            }
            case GHOST -> {
                float bend = Mth.sin(phase + (age + partialTick) * 0.08F) * displaySize;
                for (int i = 0; i < 3; i++) {
                    float from = i / 3.0F;
                    float to = (i + 1) / 3.0F;
                    line(buffer, rotation, px, py, pz,
                            bend * from * from, -displaySize * from * 3,
                            bend * to * to, -displaySize * to * 3,
                            displaySize * (0.36F - from * 0.26F), color, opacity * (1 - from));
                }
                fan(buffer, rotation, px, py, pz, DIAMOND,
                        displaySize * 0.5F, displaySize, 0xCFD0FF, opacity, 0);
            }
            case ELECTRIC -> {
                float length = 0.3F + radius;
                float jitter = Mth.sin(age * 2.17F + phase) * 0.08F;
                line(buffer, rotation, px, py, pz, -length * 0.45F, 0,
                        -length * 0.1F, jitter, 0.007F, color, opacity);
                line(buffer, rotation, px, py, pz, -length * 0.1F, jitter,
                        length * 0.17F, -jitter * 0.6F, 0.006F, color, opacity);
                line(buffer, rotation, px, py, pz, length * 0.17F, -jitter * 0.6F,
                        length * 0.55F, jitter * 0.2F, 0.004F, 0x8CA0FF, opacity);
            }
            case SOLAR_SPARK, STAR -> fan(buffer, rotation, px, py, pz, STAR,
                    displaySize * 1.7F, displaySize * 1.7F, color, opacity, opacity * 0.3F);
            case PULSE -> {
                float height = 0.2F + Mth.sin(progress * Mth.PI) * 0.65F;
                fan(buffer, rotation, px, py, pz, DIAMOND,
                        displaySize, height, color, opacity * 0.65F, 0);
                fan(buffer, rotation, px, py, pz, DIAMOND,
                        displaySize * 0.32F, height * 0.8F, 0xF6E8FF, opacity, 0);
            }
            default -> {
                fan(buffer, rotation, px, py, pz, DIAMOND,
                        displaySize * 1.7F, displaySize * 2, color, opacity * 0.25F, 0);
                fan(buffer, rotation, px, py, pz, DIAMOND,
                        displaySize * 0.7F, displaySize, color, opacity, opacity * 0.1F);
            }
        }
    }

    private static void fan(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z,
            float[][] points, float width, float height, int color, float centerAlpha, float edgeAlpha) {
        for (int i = 0; i < points.length; i++) {
            float[] first = points[i];
            float[] second = points[(i + 1) % points.length];
            vertex(buffer, rotation, x, y, z, 0, 0, color, centerAlpha);
            vertex(buffer, rotation, x, y, z, first[0] * width, first[1] * height, color, edgeAlpha);
            vertex(buffer, rotation, x, y, z, second[0] * width, second[1] * height, color, edgeAlpha);
            vertex(buffer, rotation, x, y, z, second[0] * width, second[1] * height, color, edgeAlpha);
        }
    }

    private static void line(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z,
            float x1, float y1, float x2, float y2, float halfWidth, int color, float opacity) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Math.max(0.0001F, Mth.sqrt(dx * dx + dy * dy));
        float sideX = -dy / length * halfWidth;
        float sideY = dx / length * halfWidth;
        vertex(buffer, rotation, x, y, z, x1 - sideX, y1 - sideY, color, opacity);
        vertex(buffer, rotation, x, y, z, x2 - sideX, y2 - sideY, color, opacity);
        vertex(buffer, rotation, x, y, z, x2 + sideX, y2 + sideY, color, opacity);
        vertex(buffer, rotation, x, y, z, x1 + sideX, y1 + sideY, color, opacity);
    }

    private static void vertex(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z,
            float localX, float localY, int color, float opacity) {
        Vector3f point = new Vector3f(localX, localY, 0).rotate(rotation).add(x, y, z);
        buffer.addVertex(point.x, point.y, point.z).setColor((color >>> 16 & 255) / 255.0F,
                (color >>> 8 & 255) / 255.0F, (color & 255) / 255.0F, opacity);
    }

    @Override public ParticleRenderType getRenderType() { return RENDER_TYPE; }
    @Override public Optional<ParticleGroup> getParticleGroup() {
        return Optional.of(FlameClientEffects.PARTICLE_GROUP);
    }
}
