package com.example.immortal_cultivation_mod.flame;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Deterministic, texture-free geometry shared by charge and flight. Local forward is +Z. */
public final class ChargedFlameShape {
    public static final int GATHER_END = 30, GROW_END = 70, EXPOSE_CORE = 87, LAUNCH = 90;
    private static final double TAU = Math.PI * 2;
    public interface Sink {
        void line(Vec3 from, Vec3 to, float width, int color, float alpha);
        void core(Vec3 center, float radius, int color, float alpha);
        void leaf(Vec3 center, Vec3 direction, float size, int color, float alpha);
    }
    private ChargedFlameShape() { }

    public static float radius(float ticks) {
        if (ticks <= GATHER_END) return .125F + .075F * smooth(ticks / GATHER_END);
        if (ticks <= GROW_END) return .2F + 1.2F * smooth((ticks - GATHER_END) / 40);
        return 1.4F * (1 - .18F * smooth((ticks - GROW_END) / 17));
    }
    public static float shell(float ticks) {
        return ticks >= EXPOSE_CORE ? .065F : 1;
    }
    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t);
    }
    public static float flightSpeed(float charge, int age) {
        return Mth.lerp(smooth(age / 14F), .38F + charge * .12F, 1.4F + charge * .4F);
    }
    public static int coreColor(FlameKind kind, float strength) {
        int base = switch (kind) {
            case EARTH -> 0xFF962E;
            case WOOD -> 0xBCFFCD;
            case SOLAR -> 0xFFF5C5;
            case NETHER -> 0x11031F;
            case THUNDER -> 0xDAEAFF;
            case DRAGON -> 0xFFE089;
            case HEAVENLY -> 0xF1CFFF;
            default -> 0xFFD2A0;
        };
        return kind == FlameKind.NETHER ? mix(0x341044, 0x06020C, strength) : mix(base, 0xFFFFFF, strength * .75F);
    }
    public static void emit(FlameKind kind, float ticks, float charge, boolean flying, int detail, Sink out) {
        float held = flying ? LAUNCH : Mth.clamp(ticks, 0, LAUNCH);
        float strength = flying ? charge : held / LAUNCH;
        float radius = flying ? (.22F + .93F * charge) * (1 + .08F * Mth.sin(ticks * .65F)) : radius(held);
        float shell = flying ? 1 : shell(held);
        // Celestial particles briefly hang motionless before the final inward collapse.
        double time = kind == FlameKind.HEAVENLY && !flying && held >= 74 && held < 78 ? 74 : ticks;
        double spin = time * switch (kind) { case EARTH -> .045; case WOOD, NETHER -> .07; case THUNDER -> .31; default -> .17; };
        int color = kind.color(), hot = coreColor(kind, strength);
        if (kind != FlameKind.NETHER && ((!flying && held >= EXPOSE_CORE) || (flying && ticks < 3)))
            hot = mix(hot, 0xFFFFFF, .8F);
        float coreRatio = .34F + .66F * (1 - Mth.clamp(strength * 1.7F, 0, 1));
        out.core(Vec3.ZERO, radius * coreRatio, hot, 1);
        out.core(Vec3.ZERO, radius * .46F, kind == FlameKind.NETHER ? 0x4B1573 : hot, .14F);
        int strands = detail >= 2 ? 24 : 12;
        for (int i = 0; i < strands; i++) {
            double seed = i * 2.39996, latitude = -.95 + (i % 9) * .2375;
            Vec3 previous = null;
            for (int j = 0; j <= 9; j++) {
                double u = j / 9., angle = seed + spin + u * 2.6;
                double r = radius * (.56 + .37 * Math.sin(u * Math.PI) + .08 * Math.sin(time * .7 + seed + u * 12));
                double lat = latitude + Math.sin(angle + seed) * .22;
                Vec3 point = polar(angle, lat, r);
                if (flying) point = point.add(0, 0, -u * u * radius * 1.4);
                else if (kind == FlameKind.NETHER) point = point.add(Math.sin(seed) * u * .2, -u * radius * .5, 0);
                if (previous != null) {
                    float alpha = (float)Math.sin(u * Math.PI) * .65F * shell;
                    out.line(previous, point, radius * .075F * (1 - (float)u * .7F), mix(color, hot, (float)u * .3F), alpha);
                    out.line(previous, point, radius * .013F, hot, alpha * .7F);
                }
                previous = point;
            }
        }
        // Opposing inward streams continue while the flame shell flows outward.
        if (!flying) {
            int streams = detail >= 2 ? 18 : 9;
            double intakePhase = time * .023 + Math.max(0, time - GROW_END) * .047;
            for (int i = 0; i < streams; i++) {
                double progress = fraction(intakePhase + i * .61803);
                for (int j = 0; j < 5; j++) {
                    double p = Math.max(0, progress - j * .022);
                    double reach = (3 + i % 3) * (1 - p) + radius * .3;
                    double angle = i * 2.39996 + p * 5.5 + spin * .2;
                    Vec3 position = polar(angle, Math.sin(i * 1.7) * .85, reach);
                    float opacity = (float)Math.sin(p * Math.PI) * (1 - j / 6F) * .8F;
                    if (kind == FlameKind.WOOD || kind == FlameKind.DRAGON)
                        out.leaf(position, position.scale(-1).normalize(), .05F + strength * .08F, color, opacity);
                    else out.line(position, position.scale(.97), .018F + strength * .015F, j == 0 ? hot : color, opacity);
                }
            }
            ring(out, radius * 1.08, spin, .6, hot, .015F, .5F * shell);
            if (held >= GROW_END) {
                float wave = (held - GROW_END) / 20F;
                ring(out, radius + wave * 4, 0, 0, color, .035F, .5F * (1 - wave));
            } else if (held > GATHER_END) {
                float wave = (float)fraction(time / 20);
                ring(out, radius + wave * 1.5, 0, .1, color, .025F, .18F * (1 - wave));
            }
        } else if (ticks < 7) {
            ring(out, radius + ticks * .3, spin, Math.PI / 2, hot, .055F, .65F * (1 - ticks / 7));
        }
        accents(kind, out, time, spin, radius, strength, shell, flying, detail, color, hot);
    }

    private static void accents(FlameKind kind, Sink out, double time, double spin, float r,
            float strength, float shell, boolean flying, int detail, int color, int hot) {
        int count = detail >= 2 ? 12 : 6;
        switch (kind) {
            case CRIMSON -> {
                for (int i = 0; i < count; i++) {
                    double p = fraction(time * .09 + i * .618), a = i * 2.399 + spin;
                    Vec3 from = polar(a, Math.sin(i) * .9, r * (1 + p * .9));
                    out.line(from, from.add(0, .1 + p * .3, flying ? -.3 : 0), .025F, hot, (float)(1-p) * shell);
                }
            }
            case EARTH -> {
                for (int i = 0; i < count; i++) {
                    double a = i * TAU / count + spin * .6, p = fraction(time * .015 + i * .37);
                    double reach = r * 1.25 + (1-p) * (flying ? .3 : 1.7);
                    Vec3 rock = polar(a, Math.sin(i * 2.) * .55, reach);
                    out.core(rock, .07F + strength * .05F, 0x591D12, .9F * shell);
                    out.line(rock.add(-.05, 0, 0), rock.add(.06, .05, .02), .015F, 0xFFAA39, shell);
                    Vec3 crack = polar(a, .3 * Math.sin(i), r * .71);
                    out.line(crack, crack.add(.13 * Math.sin(i), .2, .09), .018F, 0xFF701C, .75F * shell);
                    Vec3 smoke = rock.add(0, p * 1.2, 0);
                    out.core(smoke, .11F + (float)p * .14F, 0x312725, .10F * shell);
                }
            }
            case WOOD -> {
                for (int i = 0; i < count; i++) {
                    double a = i * TAU / count + spin, p = fraction(time * .018 + i * .27);
                    Vec3 leaf = polar(a, Math.sin(a * .7) * .7, r * (1.05 + .3 * p));
                    out.leaf(leaf, new Vec3(-Math.sin(a), .3, Math.cos(a)), r * .23F, i % 2 == 0 ? color : hot, .55F * shell);
                }
                for (int i = 0; i < 3; i++) ring(out, r * (1.05 + i * .12), spin + i, .6 + i, color, .025F, .35F * shell);
            }
            case SOLAR -> {
                for (int i = 0; i < 3; i++) ring(out, r * (1.1 + i * .12), spin * (i % 2 == 0 ? 1 : -1), .5 + i * .9, 0xFFECA2, .025F, .8F * shell);
                for (int i = 0; i < count; i++) {
                    Vec3 p = polar(i * TAU / count + spin, Math.sin(i) * .4, r * .8);
                    out.line(p, p.scale(1.8 + .3 * Math.sin(time * .5)), .015F, hot, (.2F + strength * .3F) * shell);
                }
            }
            case NETHER -> {
                ring(out, r * .48, spin, 1.2, 0xD18BFF, .028F, .9F);
                for (int i = 0; i < count; i++) {
                    double a = i * TAU / count - spin, p = fraction(time * .017 + i * .33);
                    Vec3 ghost = polar(a, -.3 - p * .6, r * (1.5 - p * .8));
                    out.core(ghost, .045F + strength * .06F, 0xB58FE5, .25F * shell);
                    out.line(ghost, ghost.add(.15 * Math.sin(a), -.3, -.2), .035F, 0x6642BC, .45F * shell);
                    out.core(ghost.scale(1.1), r * .12F, 0x140621, .2F * shell);
                }
            }
            case THUNDER -> {
                for (int i = 0; i < 3; i++) ring(out, r * (1.15 + i * .13), spin, i * .9, color, .012F, .7F * shell);
                int arcs = 3 + (int)(strength * 7);
                for (int i = 0; i < arcs; i++) {
                    Vec3 start = polar(i * 2.4 + spin, Math.sin(i) * .6, r * .45);
                    Vec3 end = polar(i * 2.4 + spin + .8, Math.cos(i) * .8, r * (i % 3 == 0 ? 2.1 : 1.05));
                    bolt(out, start, end, time + i, hot, .65F * shell + .2F);
                }
            }
            case DRAGON -> {
                int dragons = strength < .78F ? 3 : 1;
                for (int dragon = 0; dragon < dragons; dragon++) {
                    Vec3 previous = null, head = null;
                    for (int j = 0; j <= 40; j++) {
                        double u = j / 40., a = u * TAU * 1.4 + spin + dragon * TAU / dragons;
                        Vec3 p = new Vec3(Math.cos(a) * r * 1.2, Math.sin(u * TAU) * r * .6, Math.sin(a) * r * 1.2);
                        if (previous != null) out.line(previous, p, r * .075F * (float)(.25+u), j % 3 == 0 ? 0xFFD27A : color, .7F * shell);
                        if (j % 5 == 0) out.leaf(p, p.normalize(), r * .09F, 0xFFD27A, .55F * shell);
                        previous = p; head = p;
                    }
                    if (head != null) {
                        out.core(head, r * .13F, 0xFFBC4A, .65F * shell);
                        for (int side : new int[]{-1,1}) {
                            Vec3 horn = head.add(side*r*.11,r*.08,-r*.03);
                            out.line(horn,horn.add(side*r*.08,r*.2,-r*.12),r*.018F,hot,.8F*shell);
                            out.line(head.add(side*r*.09,0,r*.08),head.add(side*r*.29,r*.03,r*.2),r*.009F,hot,.65F*shell);
                        }
                    }
                    if (head != null && !flying && strength > .86F) {
                        out.line(head, head.add(0, r * .16, r * .35), r * .07F, hot, shell);
                        out.line(head, head.add(0, -r * .16, r * .35), r * .07F, color, shell);
                    }
                }
            }
            case HEAVENLY -> {
                for (int i = 0; i < 4; i++) {
                    ring(out, r * (1.0 + i * .14), spin * (i % 2 == 0 ? 1 : -1) * (1 + i * .3), i * .72, i % 2 == 0 ? color : 0xF9D58A, .013F, .65F * shell);
                    if (i > 0) bolt(out, polar(spin+i, .2, r), polar(spin+i+.3, .5, r*1.4), time+i, 0xDEB3FF, .55F * shell);
                }
                if (time % 24 < 7) out.line(new Vec3(0, -r*2.5, 0), new Vec3(0, r*2.5, 0), .024F, hot, .55F);
                for (int i = 0; i < count; i++) {
                    Vec3 p = polar(i * 2.4 + spin * .4, Math.sin(i) * .8, r * 1.65);
                    out.line(p.add(-.065,0,0), p.add(.065,0,0), .01F, i % 2 == 0 ? 0xFFE199 : color, .75F * shell);
                    out.line(p.add(0,-.065,0), p.add(0,.065,0), .01F, hot, .75F * shell);
                }
            }
            default -> { }
        }
    }
    private static void bolt(Sink out, Vec3 a, Vec3 b, double time, int color, float alpha) {
        Vec3 previous = a;
        for (int j = 1; j <= 7; j++) {
            double u = j / 7., jitter = Math.sin(u * Math.PI) * .15;
            Vec3 p = a.lerp(b, u).add(Math.sin(j*37+Math.floor(time*1.7))*jitter, Math.cos(j*19+Math.floor(time*1.3))*jitter, 0);
            out.line(previous,p,.015F,color,alpha); previous=p;
        }
    }
    private static void ring(Sink out, double radius, double spin, double tilt, int color, float width, float alpha) {
        Vec3 previous = null;
        for (int j = 0; j <= 40; j++) {
            double a = j * TAU / 40 + spin;
            Vec3 point = new Vec3(Math.cos(a)*radius, Math.sin(a)*radius*Math.sin(tilt), Math.sin(a)*radius*Math.cos(tilt));
            float glow = .5F + .5F * (float)Math.pow(Math.sin(j * TAU / 40 * 3 + spin), 2);
            if (previous != null) out.line(previous,point,width,color,alpha * glow); previous=point;
        }
    }
    private static Vec3 polar(double angle, double latitude, double radius) {
        return new Vec3(Math.cos(angle)*Math.cos(latitude)*radius, Math.sin(latitude)*radius, Math.sin(angle)*Math.cos(latitude)*radius);
    }
    private static double fraction(double n) { return n-Math.floor(n); }
    public static int mix(int a, int b, float value) {
        float t=Mth.clamp(value,0,1);
        return (int)Mth.lerp(t,a>>16&255,b>>16&255)<<16 | (int)Mth.lerp(t,a>>8&255,b>>8&255)<<8 | (int)Mth.lerp(t,a&255,b&255);
    }
}
