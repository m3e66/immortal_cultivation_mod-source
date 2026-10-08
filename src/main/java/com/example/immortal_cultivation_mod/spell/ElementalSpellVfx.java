package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-visible, low packet-count casting accents for the elemental techniques.
 * Persistent telegraphs and moving effects live on their synced spell entities.
 */
public final class ElementalSpellVfx {
    private ElementalSpellVfx() {
    }

    public static void cast(
            ServerLevel level, LivingEntity caster, LivingEntity target, String spellId) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 aim = target == null
                ? caster.getEyePosition().add(look.scale(8.0D))
                : target.position().add(0.0D, Math.min(target.getBbHeight() * 0.45D, 1.2D), 0.0D);
        castAtImpact(level, caster, aim, spellId);
    }

    public static void castAtImpact(
            ServerLevel level, LivingEntity caster, Vec3 aim, String spellId) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(look).normalize();
        Vec3 palm = caster.getEyePosition().add(look.scale(0.52D)).add(right.scale(-0.16D));

        switch (ModSpells.normalizeId(spellId)) {
            case ModSpells.JINFENG_ZHI -> metalNeedleCast(level, caster, palm, look, right, up);
            case ModSpells.WANREN_CHUANXIN -> bladeFormationCast(level, caster, look, right, up);
            case ModSpells.JINGJI_TUSHENG -> inwardGroundMotes(
                    level, aim, ModParticles.METHOD_QI_WOOD.get(), 1.4D, 12);
            case ModSpells.WANTENG_JIAOSHA -> inwardGroundMotes(
                    level, aim, ModParticles.METHOD_QI_WOOD.get(), 4.5D, 18);
            case ModSpells.YANBAO_DAN -> fireballCast(level, palm, look, right, up);
            case ModSpells.CHIYAN_LONGJUAN -> groundSpiral(
                    level, aim, ModParticles.METHOD_QI_FIRE.get(), 2.0D, 18);
            case ModSpells.DIYAN_QIANG -> inwardGroundMotes(
                    level, aim, ModParticles.METHOD_QI_EARTH.get(), 1.2D, 12);
            case ModSpells.SHANBENG_YIN -> groundSpiral(
                    level, aim, ModParticles.METHOD_QI_EARTH.get(), 5.8D, 24);
            case ModSpells.SHUIREN_LIANZHAN -> waterBladeCast(level, caster, look, right, up);
            case ModSpells.CANGLANG_CHONG -> waveCast(level, caster, look, right);
            case ModSpells.LEISHAN -> lightningGather(level, caster);
            case ModSpells.WULEI_HONGDING -> groundSpiral(
                    level, aim, ModParticles.METHOD_QI_THUNDER.get(), 1.8D, 18);
            case ModSpells.BINGZHUI_YU -> coldMist(level, aim.add(0.0D, 6.0D, 0.0D), 2.8D, 20);
            case ModSpells.HANYU_FENGDI -> groundSpiral(
                    level, caster.position(), ModParticles.METHOD_QI_ICE.get(), 6.8D, 28);
            case ModSpells.YAOGUANG_SHAN -> lightOrbCast(level, caster, look, right, up);
            case ModSpells.TIANGUANG_GUANSHA -> groundSpiral(
                    level, aim, ModParticles.METHOD_QI_LIGHT.get(), 1.9D, 20);
            case ModSpells.YINGXI -> shadowGather(level, caster);
            case ModSpells.TIANFENG_ZHUI -> windCompression(level, aim);
            default -> {
            }
        }
    }

    private static void metalNeedleCast(
            ServerLevel level, LivingEntity caster, Vec3 palm,
            Vec3 look, Vec3 right, Vec3 up) {
        Vec3 wrist = palm.subtract(look.scale(0.42D));
        line(level, wrist, palm.add(look.scale(0.25D)),
                ModParticles.METHOD_QI_METAL.get(), 0.09D);
        for (int i = 0; i < 9; i++) {
            double angle = Math.PI * 2.0D * i / 9.0D;
            Vec3 start = palm.add(right.scale(Math.cos(angle) * 0.24D))
                    .add(up.scale(Math.sin(angle) * 0.24D));
            directed(level, ModParticles.METHOD_QI_METAL.get(), start,
                    palm.subtract(start).normalize().scale(0.10D));
        }
        line(level, palm, palm.add(look.scale(0.42D)), ParticleTypes.END_ROD, 0.08D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                palm.x, palm.y, palm.z, 4, 0.06D, 0.06D, 0.06D, 0.01D);
    }

    private static void bladeFormationCast(
            ServerLevel level, LivingEntity caster, Vec3 look, Vec3 right, Vec3 up) {
        Vec3 center = caster.getEyePosition().add(look.scale(0.25D));
        Vec3[] points = new Vec3[6];
        for (int i = 0; i < points.length; i++) {
            double angle = Math.PI * 2.0D * i / points.length;
            points[i] = center.add(right.scale(Math.cos(angle) * 1.35D))
                    .add(up.scale(Math.sin(angle) * 1.35D));
            for (int mote = 0; mote < 3; mote++) {
                Vec3 offset = right.scale((level.random.nextDouble() - 0.5D) * 0.5D)
                        .add(up.scale((level.random.nextDouble() - 0.5D) * 0.5D));
                Vec3 start = points[i].add(offset);
                directed(level, ModParticles.METHOD_QI_METAL.get(), start,
                        points[i].subtract(start).scale(0.18D));
            }
        }
        for (int i = 0; i < points.length; i++) {
            line(level, points[i], points[(i + 1) % points.length],
                    ModParticles.METHOD_QI_METAL.get(), 0.28D);
        }
    }

    private static void fireballCast(
            ServerLevel level, Vec3 palm, Vec3 look, Vec3 right, Vec3 up) {
        Vec3 center = palm.add(look.scale(0.12D));
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0D * i / 18.0D;
            double radius = 0.42D - (i % 3) * 0.06D;
            Vec3 start = center.add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            Vec3 tangent = right.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle)));
            directed(level, i % 3 == 0 ? ParticleTypes.FLAME : ModParticles.METHOD_QI_FIRE.get(),
                    start, center.subtract(start).scale(0.16D).add(tangent.scale(0.035D)));
        }
        level.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void waterBladeCast(
            ServerLevel level, LivingEntity caster, Vec3 look, Vec3 right, Vec3 up) {
        Vec3 center = caster.getEyePosition().add(look.scale(0.38D));
        for (int blade = -1; blade <= 1; blade++) {
            Vec3 ringCenter = center.add(right.scale(blade * 0.48D));
            orientedRing(level, ringCenter, right, up,
                    ModParticles.METHOD_QI_WATER.get(), 0.26D, 10);
            level.sendParticles(ParticleTypes.FALLING_WATER,
                    ringCenter.x, ringCenter.y, ringCenter.z,
                    3, 0.12D, 0.12D, 0.12D, 0.015D);
        }
    }

    private static void waveCast(
            ServerLevel level, LivingEntity caster, Vec3 look, Vec3 right) {
        Vec3 base = caster.position().add(look.scale(0.9D));
        for (int lateral = -4; lateral <= 4; lateral++) {
            Vec3 point = base.add(right.scale(lateral * 0.34D));
            directed(level, ModParticles.METHOD_QI_WATER.get(), point,
                    look.scale(0.10D).add(0.0D, 0.12D + Math.abs(lateral) * 0.01D, 0.0D));
            if ((lateral & 1) == 0) {
                directed(level, ParticleTypes.SPLASH, point, look.scale(0.08D).add(0.0D, 0.09D, 0.0D));
            }
        }
    }

    private static void lightningGather(ServerLevel level, LivingEntity caster) {
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.52D, 0.0D);
        for (int i = 0; i < 18; i++) {
            Vec3 start = center.add(
                    (level.random.nextDouble() - 0.5D) * 1.4D,
                    (level.random.nextDouble() - 0.5D) * caster.getBbHeight() * 1.2D,
                    (level.random.nextDouble() - 0.5D) * 1.4D);
            directed(level, i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK
                            : ModParticles.METHOD_QI_THUNDER.get(),
                    start, center.subtract(start).normalize().scale(0.15D));
        }
        level.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void lightOrbCast(
            ServerLevel level, LivingEntity caster, Vec3 look, Vec3 right, Vec3 up) {
        Vec3 center = caster.getEyePosition().add(look.scale(0.55D));
        orientedRing(level, center, right, up, ModParticles.METHOD_QI_LIGHT.get(), 0.30D, 16);
        for (int ray = 0; ray < 8; ray++) {
            double angle = Math.PI * 2.0D * ray / 8.0D;
            Vec3 direction = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            line(level, center.add(direction.scale(0.12D)), center.add(direction.scale(0.58D)),
                    ParticleTypes.END_ROD, 0.12D);
        }
    }

    private static void shadowGather(ServerLevel level, LivingEntity caster) {
        Vec3 feet = caster.position().add(0.0D, 0.08D, 0.0D);
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0D * i / 18.0D;
            double radius = 0.35D + (i % 4) * 0.10D;
            Vec3 point = feet.add(Math.cos(angle) * radius,
                    i * caster.getBbHeight() / 18.0D, Math.sin(angle) * radius);
            directed(level, ModParticles.METHOD_QI_DARK.get(), point,
                    feet.add(0.0D, caster.getBbHeight() * 0.45D, 0.0D)
                            .subtract(point).scale(0.06D));
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                feet.x, feet.y + 0.25D, feet.z, 8, 0.35D, 0.10D, 0.35D, 0.015D);
    }

    private static void windCompression(ServerLevel level, Vec3 center) {
        for (int ring = 0; ring < 3; ring++) {
            Vec3 ringCenter = center.add(0.0D, 4.0D + ring * 1.25D, 0.0D);
            double radius = 2.7D - ring * 0.55D;
            groundRing(level, ringCenter, ModParticles.METHOD_QI_WIND.get(), radius, 16, -0.18D);
        }
        level.sendParticles(ParticleTypes.CLOUD,
                center.x, center.y + 5.0D, center.z,
                8, 1.8D, 1.2D, 1.8D, 0.025D);
    }

    private static void inwardGroundMotes(
            ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 start = center.add(Math.cos(angle) * radius, 0.18D,
                    Math.sin(angle) * radius);
            Vec3 velocity = center.add(0.0D, -0.12D, 0.0D).subtract(start)
                    .normalize().scale(0.07D);
            directed(level, particle, start, velocity);
        }
    }

    private static void groundSpiral(
            ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.12D,
                    Math.sin(angle) * radius);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.02D, Math.cos(angle)).scale(0.08D);
            directed(level, particle, point, tangent);
        }
    }

    private static void groundRing(
            ServerLevel level, Vec3 center, ParticleOptions particle,
            double radius, int count, double radialSpeed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            directed(level, particle, center.add(radial.scale(radius)), radial.scale(radialSpeed));
        }
    }

    private static void coldMist(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double r = radius * Math.sqrt(level.random.nextDouble());
            Vec3 point = center.add(Math.cos(angle) * r,
                    (level.random.nextDouble() - 0.5D) * 0.8D,
                    Math.sin(angle) * r);
            directed(level, i % 3 == 0 ? ParticleTypes.SNOWFLAKE
                            : ModParticles.METHOD_QI_ICE.get(),
                    point, new Vec3(0.0D, -0.035D, 0.0D));
        }
    }

    private static void orientedRing(
            ServerLevel level, Vec3 center, Vec3 axisA, Vec3 axisB,
            ParticleOptions particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 point = center.add(axisA.scale(Math.cos(angle) * radius))
                    .add(axisB.scale(Math.sin(angle) * radius));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.005D, 0.005D, 0.005D, 0.0D);
        }
    }

    public static void line(
            ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) {
            return;
        }
        Vec3 direction = delta.normalize();
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.005D, 0.005D, 0.005D, 0.0D);
        }
    }

    public static void directed(
            ServerLevel level, ParticleOptions particle, Vec3 position, Vec3 velocity) {
        level.sendParticles(particle, position.x, position.y, position.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
    }
}
