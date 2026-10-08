package com.example.immortal_cultivation_mod.flame;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Deterministic local impact geometry; no world access, particles, or terrain mutation. */
public final class FlameImpactShape {
    public static final int EXPAND_END = 5, REVEAL_END = 30, AFTERMATH_END = 160, EXPIRE = 600;
    public static final int MAX_LINES = 1200, MAX_CORES = 96, MAX_LEAVES = 96;
    public static final float MAX_TARGET_RADIUS = 60;
    private static final double TAU = Math.PI * 2;
    private static final float HOLD_END = 14;

    public enum Phase { COMPRESS, EXPAND, REVEAL, IMMEDIATE_AFTERMATH, LONG_AFTERMATH, EXPIRED }

    private FlameImpactShape() { }

    public static Phase phase(float age) {
        if (!Float.isFinite(age) || age >= EXPIRE) return Phase.EXPIRED;
        if (age <= 1) return Phase.COMPRESS;
        if (age < EXPAND_END) return Phase.EXPAND;
        if (age < REVEAL_END) return Phase.REVEAL;
        return age < AFTERMATH_END ? Phase.IMMEDIATE_AFTERMATH : Phase.LONG_AFTERMATH;
    }

    /** Cooling envelope shared by geometry and the renderer's independently projected scars. */
    public static float intensity(float age) {
        if (!Float.isFinite(age) || age >= EXPIRE) return 0;
        float t = Math.max(0, age);
        if (t <= HOLD_END) return 1;
        if (t < REVEAL_END) return Mth.lerp(smooth((t - HOLD_END) / (REVEAL_END - HOLD_END)), 1, .64F);
        if (t < AFTERMATH_END)
            return Mth.lerp(smooth((t - REVEAL_END) / (AFTERMATH_END - REVEAL_END)), .64F, .34F);
        return .34F * (float)Math.pow(1 - (t - AFTERMATH_END) / (EXPIRE - AFTERMATH_END), 1.4);
    }

    /** The fire globe reaches the exact configured radius before the terrain is revealed. */
    public static float expansionRadius(float age, float initialRadius, float targetRadius) {
        float target = validRadius(targetRadius);
        if (!Float.isFinite(age) || target == 0 || age >= REVEAL_END) return 0;
        float initial = Float.isFinite(initialRadius)
                ? Mth.clamp(initialRadius, Math.min(.05F, target), target) : Math.min(1.15F, target);
        float t = Math.max(0, age);
        float compressed = initial * .35F;
        if (t <= 1) return Mth.lerp(smooth(t), initial, compressed);
        if (t < EXPAND_END) return Mth.lerp(smooth((t - 1) / (EXPAND_END - 1)), compressed, target);
        if (t <= HOLD_END) return target;
        return target * (1 - smooth((t - HOLD_END) / (REVEAL_END - HOLD_END)));
    }

    /** Fixed primitive limits apply at every radius and detail setting. Coordinates are relative to the hit. */
    public static void emit(FlameKind kind, float age, float targetRadius, long seed,
            int detail, ChargedFlameShape.Sink out) {
        float radius = validRadius(targetRadius);
        if (out == null || radius == 0 || phase(age) == Phase.EXPIRED) return;
        FlameKind element = kind == null ? FlameKind.CRIMSON : kind;
        float t = Math.max(0, age), glow = intensity(t);
        int quality = Mth.clamp(detail, 0, 2);
        var sink = new BoundedSink(out, radius);
        int color = element.color(), hot = ChargedFlameShape.coreColor(element, .9F);
        if (t < REVEAL_END) {
            explosion(element, t, radius, seed, quality, sink, color, hot);
            debris(t, radius, seed, quality, sink, glow);
        }
        dust(element, t, radius, seed, quality, sink, glow);
        smoke(element, t, radius, seed, quality, sink, glow);
        elemental(element, t, radius, seed, quality, sink, color, hot, glow);
    }

    private static void explosion(FlameKind kind, float age, float target, long seed,
            int detail, ChargedFlameShape.Sink out, int color, int hot) {
        float r = expansionRadius(age, Math.min(1.15F, target), target);
        float reveal = 1 - smooth((age - HOLD_END) / (REVEAL_END - HOLD_END));
        // Multiple nearly opaque nested globes obscure the center while the shell separates into flames.
        out.core(Vec3.ZERO, r, color, reveal * .96F);
        out.core(Vec3.ZERO, r * .82F, hot, reveal * .92F);
        out.core(Vec3.ZERO, r * .54F, kind == FlameKind.NETHER ? 0x10051D : 0xFFF5DF, reveal);
        if (age < 3)
            out.core(Vec3.ZERO, Math.min(target, .6F + target * smooth(age / 3)), 0xFFFFFF, (1 - age / 3) * .95F);

        int strands = detail >= 2 ? 20 : detail == 1 ? 14 : 8;
        int steps = detail >= 2 ? 12 : 8;
        double spin = age * (kind == FlameKind.THUNDER ? .27 : .11) + unit(seed, 7) * TAU;
        for (int i = 0; i < strands; i++) {
            double latitude = Math.asin(-.91 + 1.82 * (i + .5) / strands);
            Vec3 previous = null;
            for (int j = 0; j <= steps; j++) {
                double u = j / (double)steps, angle = i * 2.39996 + spin + u * 3;
                double lat = latitude + Math.sin(angle * 2 + i) * .12;
                double reach = r * (.88 + .1 * Math.sin(u * Math.PI));
                Vec3 point = sphere(angle, lat, reach);
                if (previous != null) {
                    float opacity = reveal * (.35F + .55F * (float)Math.sin(u * Math.PI));
                    out.line(previous, point, Math.max(.018F, r * .045F * (1 - (float)u * .65F)), color, opacity);
                    out.line(previous, point, Math.max(.009F, r * .008F), hot, opacity * .7F);
                }
                previous = point;
            }
        }
        int ringSteps = detail >= 2 ? 40 : 24;
        for (int i = 0; i < 3; i++) {
            float wave = Mth.clamp((age - i * .65F) / 9, 0, 1);
            if (wave > 0 && wave < 1) {
                ring(out, new Vec3(0, .05 + i * .025, 0), target * (wave * 1.25 + .05),
                        spin * .1, .02, hot, Math.max(.025F, target * .008F), (1 - wave) * .78F, ringSteps);
            }
            if (r > .01)
                ring(out, Vec3.ZERO, r * .99, spin + i, i * Math.PI / 3,
                        color, Math.max(.014F, r * .01F), reveal * .5F, ringSteps);
        }
    }

    private static void debris(float age, float r, long seed, int detail, ChargedFlameShape.Sink out, float glow) {
        int count = detail >= 2 ? 18 : detail == 1 ? 12 : 6;
        for (int i = 0; i < count; i++) {
            float start = (float)unit(seed, 100 + i) * 2;
            float p = Mth.clamp((age - start) / 27, 0, 1);
            double angle = unit(seed, 200 + i) * TAU;
            double reach = r * (.12 + p * (.62 + unit(seed, 300 + i) * .5));
            double height = r * (.15 + unit(seed, 400 + i) * .7) * 4 * p * (1 - p);
            height += floor(r, reach / r) * p;
            Vec3 rock = ground(angle, reach, height);
            float size = Math.min(.85F, .08F + r * (.006F + .009F * (float)unit(seed, 500 + i)));
            float alpha = glow * (1 - smooth((age - 23) / 7));
            out.core(rock, size, i % 3 == 0 ? 0x574434 : 0x373332, alpha);
            Vec3 tumble = new Vec3(Math.cos(age * .5 + i), Math.sin(age * .37 + i), Math.sin(age * .5 + i)).scale(size);
            out.line(rock.subtract(tumble), rock.add(tumble), size * .3F, 0x8D6D50, alpha * .75F);
            out.line(rock, rock.add(0, -.12 - p * r * .12, 0), Math.max(.02F, size * .2F), 0xAD8968, alpha * .32F);
        }
    }

    private static void dust(FlameKind kind, float age, float r, long seed,
            int detail, ChargedFlameShape.Sink out, float glow) {
        int count = detail >= 2 ? 18 : detail == 1 ? 12 : 7;
        int color = kind == FlameKind.EARTH ? 0x68503E : kind == FlameKind.NETHER ? 0x30283D : 0xA1876D;
        for (int i = 0; i < count; i++) {
            double angle = unit(seed, 1000 + i) * TAU;
            double cycle = fraction(age / (age < REVEAL_END ? 34. : 95.) + unit(seed, 1100 + i));
            double reach = r * (age < REVEAL_END ? .25 + .85 * smooth(age / 30) : .38 + .5 * cycle);
            double drift = Math.sin(angle * 3 + age * .035) * r * .025;
            Vec3 cloud = ground(angle + drift / Math.max(1, r), reach,
                    floor(r, reach / r) + .1 + cycle * Math.min(1.8, r * .1));
            float opacity = glow * (float)Math.sin(cycle * Math.PI) * (age < REVEAL_END ? .32F : .11F);
            float size = Math.min(3.2F, .18F + r * .035F + (float)cycle * r * .018F);
            out.core(cloud, size, color, opacity);
            // Periodic wall slips create localized puffs rather than a permanent crater-sized cloud.
            double slip = fraction(age / 117 + unit(seed, 1200 + i));
            if (age > REVEAL_END && slip < .11) {
                Vec3 wall = ground(angle, r * .84, floor(r, .84) - slip * Math.min(3, r * .15));
                out.core(wall, size * .65F, color, glow * .18F * (float)Math.sin(slip / .11 * Math.PI));
            }
        }
    }

    private static void smoke(FlameKind kind, float age, float r, long seed,
            int detail, ChargedFlameShape.Sink out, float glow) {
        int plumes = detail >= 2 ? 9 : detail == 1 ? 6 : 3;
        int layers = detail >= 2 ? 3 : 2;
        float rate = switch (kind) { case EARTH -> .006F; case NETHER -> .003F; default -> .012F; };
        float opacity = switch (kind) { case EARTH -> .24F; case SOLAR, WOOD -> .025F; case NETHER -> .19F; default -> .14F; };
        int color = switch (kind) { case NETHER -> 0x130B20; case WOOD -> 0x44634C; case SOLAR -> 0xCDBA86; default -> 0x252329; };
        for (int i = 0; i < plumes; i++) {
            double angle = unit(seed, 1500 + i) * TAU;
            double reach = r * (.18 + unit(seed, 1600 + i) * .58);
            for (int j = 0; j < layers; j++) {
                double p = fraction(age * rate + unit(seed, 1700 + i) + j / (double)layers);
                double height = kind == FlameKind.NETHER ? .15 + Math.sin(p * Math.PI) * r * .05
                        : p * (.75 + r * .33);
                double drift = r * p * .08;
                Vec3 cloud = ground(angle, reach, floor(r, reach / r) + height)
                        .add(Math.sin(age * .018 + i) * drift, 0, Math.cos(age * .014 + i) * drift);
                float size = Math.min(3.5F, .22F + r * .025F + (float)p * r * .045F);
                out.core(cloud, size, color, glow * opacity * (float)Math.sin(p * Math.PI));
            }
        }
    }

    private static void elemental(FlameKind kind, float age, float r, long seed, int detail,
            ChargedFlameShape.Sink out, int color, int hot, float glow) {
        int count = detail >= 2 ? 14 : detail == 1 ? 9 : 5;
        int steps = detail >= 2 ? 28 : 18;
        float patchSize = Math.min(1.35F, .07F + r * .018F);
        float after = smooth((age - 3) / 12);
        // Separate, small residual fire patches keep the crater readable after the main sphere clears.
        for (int i = 0; i < count; i++) {
            double angle = unit(seed, 2000 + i) * TAU;
            double reach = r * (.12 + unit(seed, 2100 + i) * .67);
            Vec3 patch = ground(angle, reach, floor(r, reach / r) + patchSize * .45);
            float flicker = .65F + .35F * Mth.sin(age * .18F + i * 2.7F);
            float pulse = glow * after * flicker;
            out.core(patch, patchSize * .46F, hot, pulse * .32F);
            out.line(patch, patch.add(Math.sin(angle + age * .07) * patchSize * .5,
                    patchSize * (1.3 + flicker), Math.cos(angle) * patchSize * .3), patchSize * .3F, color, pulse * .68F);
        }

        switch (kind) {
            case EARTH -> {
                cracks(out, r, seed, age, count, 0xFF7928, glow * .62F, true);
                for (int i = 0; i < count; i++) {
                    double p = fraction(age / 98. + unit(seed, 2500 + i)), angle = i * 2.39996;
                    double reach = r * (.1 + unit(seed, 2600 + i) * .34);
                    Vec3 molten = ground(angle, reach, floor(r, reach / r) + Math.sin(p * Math.PI) * patchSize * 1.2);
                    int cooling = ChargedFlameShape.mix(0xFFB23D, 0x2A1412, smooth(age / EXPIRE));
                    out.core(molten, patchSize * (.28F + .3F * (float)Math.sin(p * Math.PI)), cooling, glow * .7F);
                    if (p < .14) out.line(molten, molten.add(0, patchSize * 2 * p / .14, 0), patchSize * .15F,
                            0xFF6628, glow * (1 - (float)p / .14F));
                }
            }
            case WOOD -> {
                for (int i = 0; i < count; i++) {
                    double p = fraction(age / 126. + unit(seed, 3000 + i)), angle = i * 2.39996 + p * 3;
                    double reach = r * (.24 + unit(seed, 3100 + i) * .54);
                    Vec3 leaf = ground(angle, reach, floor(r, reach / r) + p * (1.2 + r * .18));
                    out.leaf(leaf, new Vec3(-Math.sin(angle), .7, Math.cos(angle)), patchSize * .7F,
                            i % 2 == 0 ? 0xB5F6A7 : color, glow * (float)Math.sin(p * Math.PI) * .7F);
                    if (age > REVEAL_END) {
                        Vec3 previous = null;
                        for (int j = 0; j <= 6; j++) {
                            double a = angle + j * .13, distance = reach + j * r * .013;
                            Vec3 vine = ground(a, distance, floor(r, distance / r) + .1);
                            if (previous != null) out.line(previous, vine, Math.max(.015F, r * .0015F), color,
                                    glow * .45F * smooth((age - REVEAL_END) / 50));
                            if (j % 3 == 0) out.leaf(vine, new Vec3(Math.cos(a), .25, Math.sin(a)), patchSize * .42F,
                                    0xC4FFAD, glow * .38F);
                            previous = vine;
                        }
                    }
                }
            }
            case SOLAR -> {
                Vec3 center = new Vec3(0, floor(r, 0) + patchSize, 0);
                out.core(center, Math.min(2, patchSize * 1.3F), 0xFFF2B8, glow * .78F);
                for (int i = 0; i < 3; i++)
                    ring(out, center, r * (.17 + i * .095), age * .018 * (i % 2 == 0 ? 1 : -1), .06,
                            0xFFDC83, Math.max(.015F, r * .0014F), glow * (.56F - i * .12F), steps);
                for (int i = 0; i < count; i++) {
                    double angle = i * TAU / count + unit(seed, 4000) * TAU;
                    Vec3 a = ground(angle, r * .18, floor(r, .18) + .1);
                    Vec3 b = ground(angle, r * .7, floor(r, .7) + .1);
                    out.line(a, b, Math.max(.015F, r * .0015F), hot, glow * .45F);
                    float ray = pulse(age, 83, i * 7, 9);
                    out.line(b, b.add(0, Math.min(18, r * .4), 0), Math.max(.018F, r * .0018F),
                            0xFFF8D2, glow * ray * .5F);
                }
            }
            case NETHER -> {
                cracks(out, r, seed, age, count, 0xA66AFF, glow * .48F, false);
                for (int i = 0; i < count; i++) {
                    double p = fraction(age / 145. + unit(seed, 4500 + i)), angle = i * 2.39996 - p * 4;
                    double reach = r * (.86 - .72 * p);
                    Vec3 wisp = ground(angle, reach, floor(r, reach / r) + Math.sin(p * Math.PI) * (1 + r * .13));
                    float alpha = glow * (float)Math.sin(p * Math.PI);
                    out.core(wisp, patchSize * .32F, 0xCCADEA, alpha * .35F);
                    out.line(wisp, wisp.add(Math.sin(angle) * patchSize, -patchSize * 1.5, Math.cos(angle) * patchSize),
                            patchSize * .2F, 0x7660AF, alpha * .48F);
                    out.line(wisp.add(0, -.2, 0), wisp.scale(.97).add(0, -.2, 0), .014F, 0xA391B3, alpha * .55F);
                }
            }
            case THUNDER -> {
                cracks(out, r, seed, age, count, 0x97B9FF, glow * .7F, true);
                for (int i = 0; i < count; i++) {
                    double angle = unit(seed, 5000 + i) * TAU;
                    double reach = r * (.22 + unit(seed, 5100 + i) * .56);
                    Vec3 start = ground(angle, reach, floor(r, reach / r) + .25);
                    Vec3 end = start.add(Math.cos(angle + 1) * patchSize * 3,
                            patchSize * (.3 + unit(seed, 5200 + i)), Math.sin(angle + 1) * patchSize * 3);
                    float flash = pulse(age, 47 + i % 4 * 11, i * 9, 7);
                    bolt(out, start, end, seed + i, age, Math.max(.012F, patchSize * .025F), hot, glow * flash, 7);
                    out.core(start.add(0, patchSize, 0), patchSize * .16F, hot, glow * flash * .6F);
                }
                float discharge = pulse(age, 104, unit(seed, 5300) * 104, 6);
                if (discharge > 0) {
                    Vec3 a = ground(.3, r * .72, floor(r, .72) + .5);
                    Vec3 b = ground(.3 + Math.PI, r * .72, floor(r, .72) + .5);
                    bolt(out, a, b, seed, age, Math.max(.018F, r * .002F), 0xDDEBFF, glow * discharge * .65F, 15);
                }
            }
            case DRAGON -> {
                float aura = glow * (1 - .65F * smooth((age - AFTERMATH_END) / (EXPIRE - AFTERMATH_END)));
                for (int dragon = 0; dragon < 2; dragon++) {
                    Vec3 previous = null, head = null;
                    for (int j = 0; j <= steps; j++) {
                        double u = j / (double)steps, angle = u * TAU * 1.2 + age * .035 + dragon * Math.PI;
                        double reach = r * (.28 + .37 * u);
                        Vec3 point = ground(angle, reach, floor(r, reach / r) + patchSize
                                + Math.sin(u * TAU + age * .04) * patchSize * 1.4);
                        if (previous != null) out.line(previous, point, patchSize * (.12F + .38F * (float)u),
                                j % 3 == 0 ? 0xFFD17A : color, aura * .73F);
                        if (j % 4 == 0) out.leaf(point, new Vec3(Math.cos(angle), .2, Math.sin(angle)), patchSize * .65F,
                                0xF9BF5A, aura * .65F);
                        previous = point;
                        head = point;
                    }
                    out.core(head, patchSize * .72F, 0xFFB651, aura * .55F);
                    for (int side = -1; side <= 1; side += 2) {
                        Vec3 horn = head.add(side * patchSize * .4, patchSize * .35, 0);
                        out.line(horn, horn.add(side * patchSize * .4, patchSize * .8, -patchSize * .5),
                                patchSize * .06F, hot, aura * .8F);
                        out.line(head, head.add(side * patchSize * 1.4, patchSize * .05, patchSize * .5),
                                patchSize * .025F, 0xFFD992, aura * .55F);
                    }
                }
                embers(out, age, r, seed, count, 0xFFD47A, color, glow, true);
            }
            case HEAVENLY -> {
                cracks(out, r, seed, age, count, 0xCB8FFF, glow * .55F, true);
                for (int i = 0; i < 3; i++) {
                    float wave = (float)fraction(age / 110. + i / 3.);
                    ring(out, new Vec3(0, floor(r, wave * .8) + .15, 0), r * (.13 + wave * .69),
                            age * .014 * (i % 2 == 0 ? 1 : -1), .025, i % 2 == 0 ? color : 0xEED49A,
                            Math.max(.012F, r * .0013F), glow * (1 - wave) * .4F, steps);
                }
                for (int i = 0; i < count; i++) {
                    double p = fraction(age / 153. + unit(seed, 6000 + i)), angle = i * 2.39996 + age * .007;
                    double reach = r * (.2 + unit(seed, 6100 + i) * .63);
                    Vec3 star = ground(angle, reach, floor(r, reach / r) + p * (1 + r * .28));
                    float size = patchSize * (.22F + .12F * Mth.sin(age * .08F + i));
                    float alpha = glow * (float)Math.sin(p * Math.PI) * .8F;
                    star(out, star, size, i % 2 == 0 ? 0xFFDEAA : hot, alpha);
                }
                float beam = pulse(age, 123, unit(seed, 6200) * 123, 9);
                Vec3 origin = new Vec3(0, floor(r, 0) + .2, 0);
                out.line(origin, origin.add(0, Math.min(35, r * .95), 0), Math.max(.025F, patchSize * .24F),
                        0xCD9AFF, glow * beam * .28F);
            }
            default -> {
                cracks(out, r, seed, age, count, 0xFF842B, glow * .64F, false);
                embers(out, age, r, seed, count, hot, color, glow, false);
            }
        }
    }

    private static void cracks(ChargedFlameShape.Sink out, float r, long seed, float age,
            int count, int color, float alpha, boolean branches) {
        for (int i = 0; i < count; i++) {
            double angle = unit(seed, 7000 + i) * TAU;
            Vec3 previous = null;
            float cooling = alpha * (.76F + .24F * Mth.sin(age * .025F + i));
            for (int j = 0; j <= 5; j++) {
                double reach = r * (.08 + j * .135);
                double a = angle + (unit(seed, 7100 + i * 7 + j) - .5) * .24;
                Vec3 point = ground(a, reach, floor(r, reach / r) + .07);
                if (previous != null) out.line(previous, point, Math.max(.01F, r * .0022F), color, cooling);
                if (branches && j > 1 && j % 2 == 0) {
                    double branch = a + .14 * (j % 3 == 0 ? 1 : -1);
                    Vec3 tip = ground(branch, reach + r * .11, floor(r, reach / r + .11) + .07);
                    out.line(point, tip, Math.max(.009F, r * .0014F), color, cooling * .6F);
                }
                previous = point;
            }
        }
    }

    private static void embers(ChargedFlameShape.Sink out, float age, float r, long seed,
            int count, int hot, int color, float glow, boolean scales) {
        for (int i = 0; i < count; i++) {
            double p = fraction(age / 88. + unit(seed, 8000 + i)), angle = i * 2.39996 + p * .5;
            double reach = r * (.2 + unit(seed, 8100 + i) * .57);
            Vec3 ember = ground(angle, reach, floor(r, reach / r) + p * (1.2 + r * .2));
            float alpha = glow * (float)Math.sin(p * Math.PI);
            float size = Math.min(.65F, .035F + r * .007F);
            if (scales) out.leaf(ember, new Vec3(Math.cos(angle), .45, Math.sin(angle)), size,
                    i % 2 == 0 ? hot : color, alpha * .72F);
            else out.line(ember, ember.add(.03 * Math.sin(angle), .1 + p * .16, .03 * Math.cos(angle)),
                    size * .22F, hot, alpha * .86F);
            float reignite = pulse(age, 139, i * 17, 8);
            Vec3 base = ground(angle, reach, floor(r, reach / r) + .1);
            out.line(base, base.add(0, size * 4, 0), size * .65F, color, glow * reignite * .6F);
        }
    }

    private static void bolt(ChargedFlameShape.Sink out, Vec3 from, Vec3 to, long seed,
            float age, float width, int color, float alpha, int steps) {
        Vec3 direction = to.subtract(from);
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < .001) side = new Vec3(1, 0, 0);
        side = side.normalize();
        double jitter = Math.min(1.7, direction.length() * .08);
        Vec3 previous = from;
        for (int j = 1; j <= steps; j++) {
            double u = j / (double)steps;
            double wobble = (unit(seed, (long)Math.floor(age / 2) * 31 + j) - .5) * jitter * Math.sin(u * Math.PI);
            Vec3 point = from.lerp(to, u).add(side.scale(wobble)).add(0, wobble * .35, 0);
            out.line(previous, point, width, color, alpha);
            previous = point;
        }
    }

    private static void ring(ChargedFlameShape.Sink out, Vec3 center, double radius, double spin, double tilt,
            int color, float width, float alpha, int steps) {
        Vec3 previous = null;
        for (int j = 0; j <= steps; j++) {
            double a = spin + j * TAU / steps;
            Vec3 point = center.add(Math.cos(a) * radius, Math.sin(a) * radius * Math.sin(tilt),
                    Math.sin(a) * radius * Math.cos(tilt));
            if (previous != null) out.line(previous, point, width, color, alpha);
            previous = point;
        }
    }

    private static void star(ChargedFlameShape.Sink out, Vec3 center, float size, int color, float alpha) {
        out.line(center.add(-size, 0, 0), center.add(size, 0, 0), size * .12F, color, alpha);
        out.line(center.add(0, -size, 0), center.add(0, size, 0), size * .12F, color, alpha);
        out.line(center.add(0, 0, -size), center.add(0, 0, size), size * .1F, color, alpha * .65F);
    }

    private static Vec3 sphere(double angle, double latitude, double radius) {
        return new Vec3(Math.cos(angle) * Math.cos(latitude) * radius, Math.sin(latitude) * radius,
                Math.sin(angle) * Math.cos(latitude) * radius);
    }

    private static Vec3 ground(double angle, double radius, double height) {
        return new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    private static double floor(float radius, double normalizedReach) {
        double edge = Mth.clamp(normalizedReach, 0, 1);
        // The server excavates a sphere. Keep residual effects near its lower
        // surface, with a small allowance for block-sized voxel boundaries.
        return -radius * Math.sqrt(Math.max(0, 1 - edge * edge)) + .35;
    }

    private static float pulse(float age, double period, double offset, double duration) {
        double elapsed = fraction((age + offset) / period) * period;
        return elapsed >= duration ? 0 : (float)Math.sin(elapsed / duration * Math.PI);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double fraction(double value) { return value - Math.floor(value); }

    private static double unit(long seed, long salt) {
        long value = seed + salt * 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return ((value ^ (value >>> 31)) >>> 11) * 0x1.0p-53;
    }

    private static float validRadius(float radius) {
        return Float.isFinite(radius) && radius > 0 ? Math.min(MAX_TARGET_RADIUS, radius) : 0;
    }

    /** Fits an entire primitive, including its thickness, inside the impact sphere. */
    public static Vec3 fitWithinRadius(Vec3 point, float padding, float radius) {
        if (point == null || !Double.isFinite(point.lengthSqr()) || !Float.isFinite(padding)) return Vec3.ZERO;
        double limit = Math.max(0, validRadius(radius) - Math.max(0, padding));
        double length = point.length();
        return length > limit ? point.scale(limit / length) : point;
    }

    /** Core, orbiting details, and aftermath share one outer radius rather than adding to it. */
    public static ChargedFlameShape.Sink withinRadius(ChargedFlameShape.Sink target, float radius) {
        return new BoundedSink(target, validRadius(radius));
    }

    /** Reject malformed styles and keep the complete output inside the impact radius. */
    private static final class BoundedSink implements ChargedFlameShape.Sink {
        private final ChargedFlameShape.Sink target;
        private final double reachSquared;
        private final float outerRadius;
        private int lines, cores, leaves;

        BoundedSink(ChargedFlameShape.Sink target, float radius) {
            this.target = target;
            outerRadius = radius;
            reachSquared = Math.pow(radius * 2 + 4, 2);
        }

        private boolean finite(Vec3 point) {
            return point != null && Double.isFinite(point.lengthSqr()) && point.lengthSqr() <= reachSquared;
        }

        private float opacity(float alpha) {
            return outerRadius > 0 && Float.isFinite(alpha) ? Mth.clamp(alpha, 0, 1) : 0;
        }

        @Override public void line(Vec3 from, Vec3 to, float width, int color, float alpha) {
            float visible = opacity(alpha);
            if (lines >= MAX_LINES || !finite(from) || !finite(to) || !Float.isFinite(width) || width <= 0 || visible <= 0) return;
            lines++;
            float thickness = Math.min(outerRadius, Math.min(3, width));
            target.line(fitWithinRadius(from, thickness, outerRadius), fitWithinRadius(to, thickness, outerRadius),
                    thickness, color & 0xFFFFFF, visible);
        }

        @Override public void core(Vec3 center, float radius, int color, float alpha) {
            float visible = opacity(alpha);
            if (cores >= MAX_CORES || !finite(center) || !Float.isFinite(radius) || radius <= 0 || visible <= 0) return;
            cores++;
            float size = Math.min(outerRadius, radius);
            target.core(fitWithinRadius(center, size, outerRadius), size, color & 0xFFFFFF, visible);
        }

        @Override public void leaf(Vec3 center, Vec3 direction, float size, int color, float alpha) {
            float visible = opacity(alpha);
            if (leaves >= MAX_LEAVES || !finite(center) || !finite(direction)
                    || !Float.isFinite(size) || size <= 0 || visible <= 0) return;
            leaves++;
            float extent = Math.min(outerRadius, Math.min(3, size));
            target.leaf(fitWithinRadius(center, extent, outerRadius), direction, extent, color & 0xFFFFFF, visible);
        }
    }
}
