package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** A real floating core surrounded by moving three-dimensional fire ribbons. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class FlameVfxRenderer {
    private static final float TAU = (float) (Math.PI * 2);
    private static final int MAX_SOURCES = 48;
    private static int rendered;

    private FlameVfxRenderer() { }

    @SubscribeEvent
    public static void frame(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) rendered = 0;
    }

    public static float bob(FlameKind kind, float time) {
        float phase = phase(time);
        return Mth.sin(phase * (kind == FlameKind.CRIMSON || kind == FlameKind.THUNDER ? 4 : 2))
                * (kind == FlameKind.EARTH ? .022F : .045F);
    }

    /** Time wraps at 240 ticks; integer harmonics keep all continuous flame loops seamless. */
    public static void render(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers,
            double distanceSquared, boolean placed) {
        render(kind, time, poses, buffers, distanceSquared, placed,
                coreColor(kind), shellColor(kind), tipColor(kind), kind.color(), true);
    }

    public static void renderQiFire(float time, PoseStack poses, MultiBufferSource buffers, double distanceSquared) {
        render(FlameKind.MORTAL, time, poses, buffers, distanceSquared, true,
                0xBDFBFF, 0x39BFCF, 0xE5FFFF, 0x73EDFF, true);
    }

    /** Item previews share block geometry, without consuming the world's source budget. */
    public static void renderItem(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers) {
        render(kind, time, poses, buffers, 0, true,
                coreColor(kind), shellColor(kind), tipColor(kind), kind.color(), false);
    }

    /** Enlarge the actual seed and tongues while keeping its orbiting effects inside the vessel. */
    public static void renderContainedItem(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers) {
        render(kind, time, poses, buffers, 0, true,
                coreColor(kind), shellColor(kind), tipColor(kind), kind.color(), false, true);
    }

    public static void renderFireball(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers) {
        if (++rendered > MAX_SOURCES) return;
        render(kind, time, poses, buffers, 0, false,
                coreColor(kind), shellColor(kind), tipColor(kind), kind.color(), false);
        sphere(buffers.getBuffer(FlameRenderTypes.core()), poses.last().pose(), Vec3.ZERO,
                .30F * pulse(kind, phase(time)), coreColor(kind), 1, 12, 8);
    }

    private static void render(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers,
            double distanceSquared, boolean placed, int coreTint, int shellTint, int tipTint, int groundTint,
            boolean worldBudget) {
        render(kind, time, poses, buffers, distanceSquared, placed,
                coreTint, shellTint, tipTint, groundTint, worldBudget, false);
    }

    private static void render(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers,
            double distanceSquared, boolean placed, int coreTint, int shellTint, int tipTint, int groundTint,
            boolean worldBudget, boolean contained) {
        if (worldBudget && (++rendered > MAX_SOURCES || distanceSquared > 48 * 48)) return;
        float phase = phase(time);
        boolean near = distanceSquared < 24 * 24;
        int sides = near ? 12 : 8;
        int bands = near ? 6 : 4;
        VertexConsumer core = buffers.getBuffer(FlameRenderTypes.core());
        Matrix4f matrix = poses.last().pose();
        // Fill most of the glass chamber with the actual core. Tongues remain tall and
        // surround it, while their height stays below the stopper throughout the animation.
        Matrix4f coreMatrix = contained ? new Matrix4f(matrix).scale(3.2F) : matrix;
        Matrix4f tongueMatrix = contained ? new Matrix4f(matrix).scale(2.0F, 1.7F, 2.0F) : matrix;
        float pulse = pulse(kind, phase);
        // 0.35-block nominal diameter; this is geometry, not an enlarged inventory sprite.
        sphere(core, coreMatrix, Vec3.ZERO, .175F * pulse, coreTint, 1, sides, bands);
        VertexConsumer shell = buffers.getBuffer(FlameRenderTypes.energy());
        int layers = kind == FlameKind.HEAVENLY ? 3 : 2;
        int tongues = near ? 6 : 4;
        for (int layer = 0; layer < layers; layer++) {
            float direction = kind == FlameKind.HEAVENLY && layer == 1 ? -1 : 1;
            float speed = switch (kind) {
                case EARTH -> 1;
                case WOOD, SOLAR, NETHER -> 2;
                case THUNDER -> 9;
                case HEAVENLY -> layer + 2;
                default -> 6;
            };
            for (int tongue = 0; tongue < tongues; tongue++) {
                float angle = tongue * TAU / tongues + layer * .6F;
                ribbon(shell, tongueMatrix, kind, phase, angle, direction * speed, layer,
                        placed, near ? 9 : 6, pulse, shellTint, tipTint);
            }
        }
        switch (kind) {
            case EARTH -> {
                float progress = fraction(phase / TAU * 3);
                ring(shell, matrix, .19F + progress * .35F, .012F,
                        0, .3F, phase, 0xC62E12, .38F * (1 - progress));
            }
            case SOLAR -> {
                ring(shell, matrix, .37F, .017F, .015F, .33F, phase, 0xFFE99B, .72F);
                ring(shell, matrix, .395F, .004F, .015F, .33F, -phase, 0xFFC64B, .34F);
            }
            case NETHER -> {
                ring(shell, matrix, .31F, .023F, -.03F, .6F, -phase, 0x502375, .16F);
                ring(shell, matrix, .24F, .012F, .08F, -.4F, phase, 0x1C092D, .28F);
            }
            case THUNDER -> {
                ring(shell, matrix, .37F, .009F, 0, .45F, phase * 4, 0xA2A3FF, .55F);
                lightning(shell, matrix, phase);
            }
            case DRAGON -> {
                dragon(shell, matrix, phase, near);
                float progress = fraction(phase / TAU * 3);
                ring(shell, matrix, .2F + progress * .45F, .024F, -.03F, .12F,
                        phase, 0xFFA636, .5F * (1 - progress) * (1 - progress));
            }
            case HEAVENLY -> {
                ring(shell, matrix, .30F, .009F, -.08F, .25F, phase * 2, 0xE0ABFF, .55F);
                ring(shell, matrix, .38F, .008F, .08F, -.35F, -phase * 3, 0xA357FF, .42F);
                float cycle = fraction(phase / TAU * 2);
                if (cycle < .16F) {
                    float flash = Mth.sin(cycle / .16F * Mth.PI);
                    beam(shell, matrix, new Vec3(0, -.48, 0), new Vec3(0, .86, 0),
                            .025F * flash, 0xEED6FF, .65F * flash);
                }
                lightning(shell, matrix, phase * 2, 0xD3ABFF, .48F);
            }
            default -> { }
        }
        if (placed) {
            // A breathing ground halo and shorter grounded tongues replace the static cross model.
            ring(shell, matrix, .29F + .015F * Mth.sin(phase * 3), .04F,
                    -.55F, 0, phase, groundTint, .2F);
        }
    }

    private static float phase(float time) {
        return (time % 240 + 240) % 240 / 240 * TAU;
    }

    private static float fraction(float value) { return value - Mth.floor(value); }

    private static float pulse(FlameKind kind, float phase) {
        return switch (kind) {
            case CRIMSON -> 1 + .045F * Mth.sin(phase * 11) + .025F * Mth.sin(phase * 23);
            case EARTH -> 1 + .045F * Mth.sin(phase * 2);
            case WOOD -> 1 + .06F * Mth.sin(phase);
            case SOLAR -> 1 + .04F * Mth.sin(phase * 2);
            case NETHER -> 1 + .025F * Mth.sin(phase);
            case THUNDER -> 1 + .06F * Mth.sin(phase * 19) * Mth.sin(phase * 31);
            case DRAGON -> 1 + .045F * Mth.sin(phase * 3);
            case HEAVENLY -> 1 + .035F * Mth.sin(phase * 2);
            default -> 1;
        };
    }

    private static int coreColor(FlameKind kind) {
        return switch (kind) {
            case CRIMSON -> 0xF63312;
            case EARTH -> 0x4A110A;
            case WOOD -> 0xCCFFB8;
            case SOLAR -> 0xFFF7D1;
            case NETHER -> 0x170721;
            case THUNDER -> 0xF0F1FF;
            case DRAGON -> 0xFFB32E;
            case HEAVENLY -> 0xB645F5;
            default -> 0xFF9F3A;
        };
    }

    private static int shellColor(FlameKind kind) {
        return switch (kind) {
            case CRIMSON -> 0xEF431B;
            case EARTH -> 0xFF7424;
            case WOOD -> 0x47CE86;
            case SOLAR -> 0xFFC84D;
            case NETHER -> 0x6150B9;
            case THUNDER -> 0x6678FF;
            case DRAGON -> 0xF84924;
            case HEAVENLY -> 0xA67AFF;
            default -> 0xFF973B;
        };
    }

    private static int tipColor(FlameKind kind) {
        return switch (kind) {
            case WOOD -> 0xD9FFD8;
            case NETHER -> 0xB697EF;
            case THUNDER -> 0xDBE8FF;
            case HEAVENLY -> 0xF1DBFF;
            default -> 0xFFF1B3;
        };
    }

    private static void ribbon(VertexConsumer out, Matrix4f matrix, FlameKind kind, float phase,
            float angle, float speed, int layer, boolean placed, int segments, float pulse, int shellTint, int tipTint) {
        for (int segment = 0; segment < segments; segment++) {
            float t0 = segment / (float) segments;
            float t1 = (segment + 1) / (float) segments;
            Vec3 a = tongue(kind, phase, angle, speed, layer, t0, placed, pulse);
            Vec3 b = tongue(kind, phase, angle, speed, layer, t1, placed, pulse);
            float widthA = .052F * (1 - t0) * (layer == 0 ? 1 : .72F);
            float widthB = .052F * (1 - t1) * (layer == 0 ? 1 : .72F);
            float opacityA = Mth.sin(t0 * Mth.PI) * (layer == 0 ? .62F : .40F);
            float opacityB = Mth.sin(t1 * Mth.PI) * (layer == 0 ? .62F : .40F);
            int color = mix(shellTint, tipTint, t0 * .6F);
            float tangent = angle + phase * speed + t0 * 2.4F;
            Vec3 side = new Vec3(-Mth.sin(tangent), .12, Mth.cos(tangent)).normalize();
            softRibbon(out, matrix, a, b, side, widthA, widthB, color, opacityA, opacityB);
            // A narrow luminous inner edge makes the shell read as fire rather than opaque cloth.
            softRibbon(out, matrix, a, b, side, widthA * .2F, widthB * .2F,
                    tipTint, opacityA * .5F, opacityB * .5F);
        }
    }

    private static Vec3 tongue(FlameKind kind, float phase, float angle, float speed,
            int layer, float progress, boolean placed, float pulse) {
        float sway = kind == FlameKind.EARTH ? .7F : kind == FlameKind.WOOD ? 1.3F : 2.4F;
        float turn = angle + phase * speed + progress * sway;
        float wobble = Mth.sin(phase * (kind == FlameKind.THUNDER ? 23 : 6)
                + angle * 2 + progress * 8) * .023F;
        float radius = (.15F + .16F * Mth.sin(progress * Mth.PI) + layer * .027F + wobble)
                * (1 - progress * .55F) * pulse;
        float start = placed ? -.52F : -.19F;
        float height = placed ? .99F : .65F;
        float y = start + progress * height;
        if (kind == FlameKind.NETHER) {
            y = .12F - progress * .47F;
            radius += progress * .07F;
        }
        float x = Mth.cos(turn) * radius + progress * progress * Mth.sin(phase * 2 + angle) * .08F;
        float z = Mth.sin(turn) * radius + progress * progress * Mth.cos(phase * 2 + angle) * .08F;
        return new Vec3(x, y, z);
    }

    static void sphere(VertexConsumer out, Matrix4f matrix, Vec3 center, float radius,
            int color, float alpha, int sides, int bands) {
        for (int band = 0; band < bands; band++) {
            float latitude0 = -Mth.HALF_PI + band * Mth.PI / bands;
            float latitude1 = -Mth.HALF_PI + (band + 1) * Mth.PI / bands;
            for (int side = 0; side < sides; side++) {
                float a0 = side * TAU / sides;
                float a1 = (side + 1) * TAU / sides;
                float shade = .70F + .30F * Math.max(0, Mth.cos(a0 - .7F) * Mth.cos(latitude0));
                int shaded = shade(color, shade);
                vertex(out, matrix, center.add(spherical(radius, latitude0, a0)), shaded, alpha);
                vertex(out, matrix, center.add(spherical(radius, latitude0, a1)), shaded, alpha);
                vertex(out, matrix, center.add(spherical(radius, latitude1, a1)), shaded, alpha);
                vertex(out, matrix, center.add(spherical(radius, latitude1, a0)), shaded, alpha);
            }
        }
    }

    private static Vec3 spherical(float radius, float latitude, float longitude) {
        float horizontal = radius * Mth.cos(latitude);
        return new Vec3(horizontal * Mth.cos(longitude), radius * Mth.sin(latitude),
                horizontal * Mth.sin(longitude));
    }

    private static void ring(VertexConsumer out, Matrix4f matrix, float radius, float width,
            float y, float tilt, float spin, int color, float alpha) {
        for (int segment = 0; segment < 32; segment++) {
            float a0 = segment * TAU / 32 + spin;
            float a1 = (segment + 1) * TAU / 32 + spin;
            float brightness = .5F + .5F * Mth.sin(segment * TAU / 32 * 3 + spin);
            float a = alpha * (.5F + brightness * .5F);
            vertex(out, matrix, ringPoint(a0, radius - width, y, tilt), color, 0);
            vertex(out, matrix, ringPoint(a0, radius, y, tilt), color, a);
            vertex(out, matrix, ringPoint(a1, radius, y, tilt), color, a);
            vertex(out, matrix, ringPoint(a1, radius - width, y, tilt), color, 0);
            vertex(out, matrix, ringPoint(a0, radius, y, tilt), color, a);
            vertex(out, matrix, ringPoint(a0, radius + width, y, tilt), color, 0);
            vertex(out, matrix, ringPoint(a1, radius + width, y, tilt), color, 0);
            vertex(out, matrix, ringPoint(a1, radius, y, tilt), color, a);
        }
    }

    private static Vec3 ringPoint(float angle, float radius, float y, float tilt) {
        float wave = Mth.sin(angle) * radius;
        return new Vec3(Mth.cos(angle) * radius, y + wave * Mth.sin(tilt), wave * Mth.cos(tilt));
    }

    private static void lightning(VertexConsumer out, Matrix4f matrix, float phase) {
        lightning(out, matrix, phase, 0xE5F0FF, .72F);
    }

    private static void lightning(VertexConsumer out, Matrix4f matrix, float phase, int color, float reach) {
        int frame = Mth.floor(phase / TAU * 120);
        for (int arc = 0; arc < 3; arc++) {
            int seed = frame / 3 * 31 + arc * 73;
            if (Math.floorMod(seed * 17, 11) > 5) continue;
            float angle = fraction(seed * .137F) * TAU;
            float end = .30F + fraction(seed * .311F) * (reach - .30F);
            Vec3 a = new Vec3(Mth.cos(angle) * .16, .025, Mth.sin(angle) * .16);
            for (int piece = 1; piece <= 4; piece++) {
                float radius = .16F + (end - .16F) * piece / 4;
                float jitter = Mth.sin(seed * 1.3F + piece * 9) * .075F;
                Vec3 b = new Vec3(Mth.cos(angle + jitter) * radius,
                        Mth.sin(seed + piece * 6) * .09, Mth.sin(angle + jitter) * radius);
                beam(out, matrix, a, b, .006F, color, .88F);
                a = b;
            }
        }
    }

    private static void dragon(VertexConsumer out, Matrix4f matrix, float phase, boolean near) {
        float cycle = fraction(phase / TAU * 2);
        if (cycle < .55F) return;
        float progress = (cycle - .55F) / .45F;
        float envelope = Mth.sin(progress * Mth.PI);
        int pieces = near ? 18 : 12;
        Vec3 previous = dragonPoint(progress * TAU - 2.3F, .27F * envelope, phase);
        for (int piece = 1; piece <= pieces; piece++) {
            float along = piece / (float) pieces;
            float angle = progress * TAU - (1 - along) * 2.3F;
            Vec3 point = dragonPoint(angle, .27F * envelope, phase);
            float width = (.004F + .025F * along) * envelope;
            beam(out, matrix, previous, point, width, 0xF55623, .78F * envelope);
            beam(out, matrix, previous, point, width * .35F, 0xFFE394, .86F * envelope);
            previous = point;
        }
        sphere(out, matrix, previous, .038F * envelope, 0xFFD571, .86F * envelope, 8, 4);
        Vec3 forward = new Vec3(-Mth.sin(progress * TAU), .08, Mth.cos(progress * TAU));
        beam(out, matrix, previous, previous.add(forward.scale(.065 * envelope)),
                .016F * envelope, 0xFFD571, .86F * envelope);
        for (int horn = -1; horn <= 1; horn += 2) {
            Vec3 tip = previous.add(Mth.cos(progress * TAU) * horn * .026 * envelope,
                    .073 * envelope, Mth.sin(progress * TAU) * horn * .026 * envelope);
            beam(out, matrix, previous, tip, .006F * envelope, 0xFFF1BB, .90F * envelope);
        }
    }

    private static Vec3 dragonPoint(float angle, float radius, float phase) {
        return new Vec3(Mth.cos(angle) * radius, .07 + Mth.sin(angle * 3 + phase * 2) * .07,
                Mth.sin(angle) * radius);
    }

    static void beam(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b,
            float width, int color, float alpha) {
        Vec3 delta = b.subtract(a);
        if (width < .0001F || delta.lengthSqr() < .000001) return;
        Vec3 side = delta.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < .000001) side = delta.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        softRibbon(out, matrix, a, b, side, width, width, color, alpha, alpha);
        softRibbon(out, matrix, a, b, side.cross(delta).normalize(), width, width, color, alpha, alpha);
    }

    private static void softRibbon(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 side,
            float widthA, float widthB, int color, float alphaA, float alphaB) {
        Vec3 sideA = side.scale(widthA);
        Vec3 sideB = side.scale(widthB);
        vertex(out, matrix, a.subtract(sideA), color, 0);
        vertex(out, matrix, a, color, alphaA);
        vertex(out, matrix, b, color, alphaB);
        vertex(out, matrix, b.subtract(sideB), color, 0);
        vertex(out, matrix, a, color, alphaA);
        vertex(out, matrix, a.add(sideA), color, 0);
        vertex(out, matrix, b.add(sideB), color, 0);
        vertex(out, matrix, b, color, alphaB);
    }

    private static int shade(int color, float value) {
        return (int) (((color >> 16) & 255) * value) << 16
                | (int) (((color >> 8) & 255) * value) << 8 | (int) ((color & 255) * value);
    }

    private static int mix(int a, int b, float amount) {
        return (int) Mth.lerp(amount, (a >> 16) & 255, (b >> 16) & 255) << 16
                | (int) Mth.lerp(amount, (a >> 8) & 255, (b >> 8) & 255) << 8
                | (int) Mth.lerp(amount, a & 255, b & 255);
    }

    static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 point, int color, float alpha) {
        out.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                        (color & 255) / 255F, Mth.clamp(alpha, 0, 1));
    }
}
