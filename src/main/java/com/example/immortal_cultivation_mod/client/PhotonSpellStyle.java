package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Spell-specific silhouettes with soft additive light and flowing animation. */
final class PhotonSpellStyle {
    enum Form { FLAME, BLADE, QI, WATER, RIPPLE, FROST, PRESSURE, BURST, BEAM, LEGACY }
    private PhotonSpellStyle() {}

    static Form form(String preset) {
        return switch (preset) {
            case "fireball" -> Form.FLAME;
            case "flying_sword" -> Form.BLADE;
            case "meditating" -> Form.QI;
            case "water1", "water2", "water3" -> Form.WATER;
            case "puddle" -> Form.RIPPLE;
            case "ice1", "ice2" -> Form.FROST;
            case "rage3" -> Form.PRESSURE;
            case "lingbeng" -> Form.BURST;
            case "beam" -> Form.BEAM;
            case "ball" -> Form.LEGACY;
            default -> throw new IllegalArgumentException("Unknown spell appearance: " + preset);
        };
    }

    static void particle(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera, Vec3 right, Vec3 up,
                         double width, double height, Vector4f tint, float age, float seed,
                         float life, Vec3 movement, Form form) {
        double size = Math.max(.012, Math.max(width, height));
        double phase = seed * Math.PI * 2 + age * .055;
        float fade = Mth.clamp((1 - life) * 5, 0, 1);
        Vector4f color = new Vector4f(tint.x, tint.y, tint.z, tint.w * fade);
        // These volume emitters overlap hundreds of motes in a small area.
        if (form == Form.FROST) { size *= .65; color.w *= .28F; }
        if (form == Form.RIPPLE) color.w *= .35F;
        Vec3 direction = right.scale(movement.dot(right)).add(up.scale(movement.dot(up)));
        direction = direction.lengthSqr() > .000001 ? direction.normalize() : up;
        Vec3 side = right.scale(direction.dot(up)).subtract(up.scale(direction.dot(right)));
        FilamentVfx.halo(out, matrix, center, camera, right, up, size * 1.4,
                color.x, color.y, color.z, color.w * .14F);
        switch (form) {
            case FLAME -> {
                // Broad curved tongues and compact hot embers follow particle motion.
                Vec3 bend = center.add(side.scale(Math.sin(phase * 2) * size * .3));
                facet(out, matrix, bend, camera, side, direction, size * .55, size * 1.8, color);
                facet(out, matrix, center, camera, side, direction, size * .2, size * .65, bright(color, .7F));
            }
            case BLADE, BURST, PRESSURE -> {
                double length = form == Form.BLADE ? 2.4 : form == Form.BURST ? 1.8 : 1.1;
                facet(out, matrix, center, camera, side, direction, size * .32, size * length, color);
                facet(out, matrix, center, camera, side, direction, size * .1, size * length * .7, bright(color, .8F));
            }
            case FROST -> {
                Vec3 axis = right.scale(Math.cos(phase)).add(up.scale(Math.sin(phase)));
                Vec3 cross = right.scale(-Math.sin(phase)).add(up.scale(Math.cos(phase)));
                facet(out, matrix, center, camera, cross, axis, size * .32, size, color);
                facet(out, matrix, center, camera, axis, cross, size * .13, size * .6, bright(color, .6F));
            }
            case RIPPLE -> {
                // Ground-aligned expanding rings stay flat when the camera pitches.
                // A stable subset carries the rings; the rest supplies water sheen.
                if (seed < .3F) arc(out, matrix, center, camera, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                        size * (.45 + life * 1.9), phase, Math.PI * 2, size * .11, color);
            }
            case WATER -> {
                arc(out, matrix, center, camera, right, up, size * .8, phase, Math.PI * 1.5, size * .14, color);
                facet(out, matrix, center, camera, right, up, size * .24, size * .6, bright(color, .5F));
            }
            case QI -> {
                Vec3 curl = right.scale(Math.cos(phase)).add(up.scale(Math.sin(phase)));
                Vec3 cross = right.scale(-Math.sin(phase)).add(up.scale(Math.cos(phase)));
                arc(out, matrix, center, camera, curl, cross, size * .85,
                        0, Math.PI * 1.15, size * .17, color);
                FilamentVfx.halo(out, matrix, center, camera, right, up, size * .35,
                        .6F + color.x * .4F, .6F + color.y * .4F, .6F + color.z * .4F, color.w * .55F);
            }
            default -> { }
        }
    }

    static void strip(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 start, Vec3 end,
                      double width, float u0, float u1, Vector4f c0, Vector4f c1) {
        // A single energy sheet follows the authored slash/burst path.
        float a0 = c0.w * Mth.clamp(u0 * 4, 0, 1), a1 = c1.w * Mth.clamp(u1 * 4, 0, 1);
        float r = (c0.x + c1.x) * .5F, g = (c0.y + c1.y) * .5F, b = (c0.z + c1.z) * .5F;
        double body = Math.max(.003, width * (.15 + .45 * (u0 + u1) * .5));
        FilamentVfx.ribbon(out, matrix, start, end, camera, body * 2.7, r, g, b, a0 * .11F, a1 * .11F);
        FilamentVfx.ribbon(out, matrix, start, end, camera, body * .99, r, g, b, a0 * .36F, a1 * .36F);
        FilamentVfx.ribbon(out, matrix, start, end, camera, body * .30,
                .4F + r * .6F, .4F + g * .6F, .4F + b * .6F, a0, a1);
    }

    static void beam(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 start, Vec3 delta,
                     double width, Vector4f tint, float age) {
        if (delta.lengthSqr() < .000001) return;
        Vec3 axis = delta.normalize();
        Vec3 reference = Math.abs(axis.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = axis.cross(reference).normalize(), up = axis.cross(right).normalize();
        Vec3 end = start.add(delta);
        double pulse = 1 + Math.sin(age * .16) * .06;
        FilamentVfx.ribbon(out, matrix, start, end, camera, width * 2.4 * pulse,
                tint.x, tint.y, tint.z, tint.w * .15F, tint.w * .15F);
        FilamentVfx.ribbon(out, matrix, start, end, camera, width * .8,
                tint.x, tint.y, tint.z, tint.w * .7F, tint.w * .7F);
        FilamentVfx.ribbon(out, matrix, start, end, camera, width * .2, 1, 1, 1, tint.w, tint.w);
        // Travelling annular pulses give the column direction without a flight wake.
        for (int i = 0; i < 5; i++) {
            double u = (i / 5.0 + age * .012) % 1;
            Vector4f color = new Vector4f(tint);
            color.w *= (float) (Math.sin(u * Math.PI) * .65);
            arc(out, matrix, start.add(delta.scale(u)), camera, right, up,
                    width * (1.5 + u * 2), 0, Math.PI * 2, width * .14, color);
        }
    }

    private static Vector4f bright(Vector4f color, float alpha) {
        return new Vector4f(.55F + color.x * .45F, .55F + color.y * .45F,
                .55F + color.z * .45F, color.w * alpha);
    }

    private static void arc(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera, Vec3 right, Vec3 up,
                            double radius, double angle, double sweep, double width, Vector4f color) {
        for (int i = 0; i < 12; i++) {
            double a = angle + sweep * i / 12, b = angle + sweep * (i + 1) / 12;
            Vec3 start = center.add(right.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));
            Vec3 end = center.add(right.scale(Math.cos(b) * radius)).add(up.scale(Math.sin(b) * radius));
            float alphaA = (float) Math.sin(Math.PI * i / 12) * color.w;
            float alphaB = (float) Math.sin(Math.PI * (i + 1) / 12) * color.w;
            FilamentVfx.ribbon(out, matrix, start, end, camera, width, color.x, color.y, color.z, alphaA, alphaB);
        }
    }

    private static void facet(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera,
                              Vec3 side, Vec3 axis, double width, double length, Vector4f color) {
        Vec3 c = center.subtract(camera);
        Vec3[] edge = {c.add(axis.scale(length)), c.add(side.scale(width)),
                c.subtract(axis.scale(length * .65)), c.subtract(side.scale(width))};
        for (int i = 0; i < 4; i++) {
            vertex(out, matrix, c, color, 1);
            vertex(out, matrix, edge[i], color, .04F);
            vertex(out, matrix, edge[(i + 1) % 4], color, .04F);
            vertex(out, matrix, c, color, 1);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, Vector4f color, float alpha) {
        out.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .setColor(color.x, color.y, color.z, Mth.clamp(color.w * alpha, 0, 1));
    }
}
