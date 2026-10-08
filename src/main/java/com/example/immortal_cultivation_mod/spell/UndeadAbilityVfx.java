package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Layered, directional visuals for the ranked undead combat kit. */
final class UndeadAbilityVfx {
    private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static final DustParticleOptions CORPSE_GREEN =
            new DustParticleOptions(new Vector3f(0.10F, 0.42F, 0.13F), 0.82F);
    private static final DustParticleOptions DIRTY_GREEN =
            new DustParticleOptions(new Vector3f(0.20F, 0.31F, 0.09F), 0.72F);
    private static final DustParticleOptions CORPSE_BLACK =
            new DustParticleOptions(new Vector3f(0.018F, 0.024F, 0.021F), 1.05F);
    private static final DustParticleOptions CORPSE_GRAY =
            new DustParticleOptions(new Vector3f(0.20F, 0.22F, 0.20F), 0.76F);
    private static final DustParticleOptions BONE_WHITE =
            new DustParticleOptions(new Vector3f(0.86F, 0.82F, 0.67F), 0.92F);
    private static final DustParticleOptions BONE_GOLD =
            new DustParticleOptions(new Vector3f(0.94F, 0.82F, 0.48F), 0.88F);
    private static final DustParticleOptions PALE_RED =
            new DustParticleOptions(new Vector3f(0.62F, 0.17F, 0.13F), 0.70F);
    private static final DustParticleOptions COLD_CYAN =
            new DustParticleOptions(new Vector3f(0.20F, 0.76F, 0.69F), 0.66F);
    private static final DustParticleOptions DRY_DUST =
            new DustParticleOptions(new Vector3f(0.55F, 0.28F, 0.10F), 0.82F);
    private static final DustParticleOptions DROUGHT_RED =
            new DustParticleOptions(new Vector3f(0.78F, 0.09F, 0.025F), 1.05F);

    private UndeadAbilityVfx() {
    }

    static void castTelegraph(ServerLevel level, LivingEntity caster,
                              LivingEntity target, String ability, long ticksLeft) {
        double progress = Mth.clamp(1.0D - ticksLeft / (double) castDuration(ability), 0.0D, 1.0D);
        switch (ability) {
            case "shiqi_tuxi" -> breathCharge(level, caster, progress);
            case "shihou" -> roarCharge(level, caster, progress);
            case "shiying_pusha" -> pounceCharge(level, caster, target, progress);
            case "fudi_shiyu", "dimai_shihua" -> terrainCharge(level, caster, progress, false);
            case "tunshi_huisheng", "wanshi_guiqi" -> corpseDrainCharge(level, caster, progress, false);
            case "wangu_guiyi" -> corpseDrainCharge(level, caster, progress, true);
            case "shigu_baosheng" -> boneGrowthCharge(level, caster, progress);
            case "zhaoshi" -> summonCharge(level, caster, progress);
            case "shiyu_tianmu" -> corpseSkyCharge(level, caster, progress);
            case "shesheng_duoxue" -> lifeDrainCharge(level, caster, target, progress);
            case "guyu_fengshen" -> bonePrisonWarning(level, target, progress);
            case "shigu_zaishu" -> boneReshapeCharge(level, caster, progress);
            case "chidi_qianli", "fenxue_hanqi", "tianhan_lingyu" ->
                    droughtCharge(level, caster, progress);
            case "bahuo_linshi" -> baFireCharge(level, caster, progress);
            default -> inwardBodyCharge(level, caster, primary(ability), accent(ability), progress);
        }
    }

    static void corpseBreath(ServerLevel level, LivingEntity caster) {
        Basis basis = basis(caster.getLookAngle());
        Vec3 origin = caster.getEyePosition().add(basis.forward.scale(0.30D)).add(0.0D, -0.20D, 0.0D);
        Vec3 end = origin.add(basis.forward.scale(5.2D));
        jaggedLine(level, origin, end, CORPSE_BLACK, 34, 0.055D);
        for (int ribbon = 0; ribbon < 7; ribbon++) {
            double phase = ribbon * 0.91D;
            ParticleOptions particle = ribbon % 3 == 0 ? CORPSE_GREEN : DIRTY_GREEN;
            for (int step = 1; step <= 28; step++) {
                double t = step / 28.0D;
                double spread = (0.05D + t * 0.92D) * (0.45D + (ribbon % 3) * 0.18D);
                double angle = phase + t * (5.2D + ribbon * 0.22D);
                Vec3 point = origin.add(basis.forward.scale(t * 5.0D))
                        .add(basis.right.scale(Math.cos(angle) * spread))
                        .add(basis.up.scale(Math.sin(angle) * spread * 0.56D));
                Vec3 velocity = basis.forward.scale(0.12D + 0.06D * t).add(0.0D, -0.01D, 0.0D);
                directed(level, particle, point, velocity);
            }
        }
        for (int step = 2; step <= 18; step++) {
            double t = step / 18.0D;
            Vec3 floor = surfacePoint(level, origin.add(basis.forward.scale(t * 5.0D)));
            level.sendParticles(step % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    floor.x, floor.y + 0.08D, floor.z, 2 + step / 7,
                    0.18D + t * 0.36D, 0.035D, 0.18D + t * 0.36D, 0.006D);
            if ((step & 1) == 0) {
                directed(level, ModParticles.BLACK_FLAME_ASH.get(),
                        floor.add((level.random.nextDouble() - 0.5D) * 0.45D, 0.12D,
                                (level.random.nextDouble() - 0.5D) * 0.45D),
                        basis.forward.scale(0.025D).add(0.0D, -0.004D, 0.0D));
            }
        }
    }

    static void corpseBreathHit(ServerLevel level, LivingEntity target, LivingEntity caster) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        radialBurst(level, center, CORPSE_GREEN, 18, 0.10D, 0.025D);
        Basis basis = basis(caster.getLookAngle());
        for (int tendril = 0; tendril < 6; tendril++) {
            double angle = Math.PI * 2.0D * tendril / 6.0D;
            Vec3 start = center.add(basis.right.scale(Math.cos(angle) * 0.58D))
                    .add(basis.up.scale(Math.sin(angle) * target.getBbHeight() * 0.34D));
            curvedDoubleStream(level, start, center, DIRTY_GREEN,
                    ModParticles.METHOD_QI_DARK.get(), 7, 0.08D, 0.12D);
        }
    }

    static void pounceStart(ServerLevel level, LivingEntity caster) {
        Vec3 feet = caster.position().add(0.0D, 0.08D, 0.0D);
        Vec3 back = caster.getLookAngle().multiply(-1.0D, 0.0D, -1.0D).normalize();
        inwardDisc(level, feet.add(back.scale(0.45D)), ModParticles.METHOD_QI_DARK.get(), 1.15D, 18);
        dragGroundLine(level, feet, feet.add(caster.getLookAngle().multiply(3.0D, 0.0D, 3.0D)), CORPSE_BLACK, 0.18D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, feet.x + back.x * 0.35D,
                feet.y + 0.08D, feet.z + back.z * 0.35D, 12, 0.30D, 0.04D, 0.30D, 0.025D);
    }

    static void pounceTrail(ServerLevel level, LivingEntity caster) {
        Vec3 movement = caster.getDeltaMovement();
        Vec3 direction = movement.horizontalDistanceSqr() > 0.01D
                ? new Vec3(movement.x, 0.0D, movement.z).normalize() : caster.getLookAngle();
        for (int copy = 1; copy <= 3; copy++) {
            Vec3 base = caster.position().subtract(direction.scale(copy * 0.62D));
            for (int point = 0; point < 9; point++) {
                double t = point / 8.0D;
                double y = base.y + 0.08D + caster.getBbHeight() * t;
                ParticleOptions particle = point < copy * 2
                        ? ModParticles.BLACK_FLAME_ASH.get() : CORPSE_BLACK;
                directed(level, particle, new Vec3(base.x, y, base.z),
                        direction.scale(0.035D).add(0.0D, 0.012D + t * 0.01D, 0.0D));
            }
        }
        Vec3 floor = surfacePoint(level, caster.position());
        dragGroundLine(level, floor.subtract(direction.scale(1.8D)), floor, CORPSE_BLACK, 0.16D);
        level.sendParticles(ParticleTypes.POOF, floor.x - direction.x * 0.7D,
                floor.y + 0.06D, floor.z - direction.z * 0.7D,
                5, 0.20D, 0.025D, 0.20D, 0.018D);
    }

    static void pounceImpact(ServerLevel level, Vec3 center) {
        Vec3 floor = surfacePoint(level, center);
        radialBurst(level, center.add(0.0D, 0.42D, 0.0D), CORPSE_BLACK, 26, 0.17D, 0.06D);
        irregularGroundFront(level, floor, CORPSE_GRAY, 2.2D, 34, 0.14D, 0.28D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.65D, center.z,
                18, 0.48D, 0.55D, 0.48D, 0.035D);
        level.sendParticles(ParticleTypes.POOF, floor.x, floor.y + 0.08D, floor.z,
                18, 0.75D, 0.08D, 0.75D, 0.045D);
    }

    static void roar(ServerLevel level, LivingEntity caster, double radius, boolean king) {
        Basis basis = basis(caster.getLookAngle());
        Vec3 mouth = caster.getEyePosition().add(basis.forward.scale(0.28D));
        jaggedLine(level, mouth, mouth.add(basis.forward.scale(radius)), CORPSE_BLACK,
                king ? 32 : 24, 0.035D);
        double[] heights = {0.10D, caster.getBbHeight() * 0.50D, caster.getBbHeight() * 0.92D};
        for (int front = 0; front < heights.length; front++) {
            double distance = radius * (0.68D + front * 0.12D);
            Vec3 frontCenter = caster.position().add(0.0D, heights[front], 0.0D)
                    .add(basis.forward.scale(distance * 0.42D));
            irregularArc(level, frontCenter, basis, front % 2 == 0 ? CORPSE_GRAY : DIRTY_GREEN,
                    distance, 30 + front * 6, 0.10D + front * 0.018D,
                    front == 0 ? 0.18D : 0.44D);
        }
        Vec3 floor = surfacePoint(level, caster.position());
        radialBurst(level, floor.add(0.0D, 0.08D, 0.0D), ParticleTypes.POOF,
                king ? 44 : 30, king ? 0.28D : 0.19D, 0.025D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z,
                king ? 20 : 12, 0.24D, 0.18D, 0.24D, 0.025D);
    }

    static void meridianClaw(ServerLevel level, LivingEntity caster, LivingEntity target) {
        Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        Basis basis = basis(caster.getLookAngle());
        for (int slash = -2; slash <= 2; slash++) {
            Vec3 start = hit.add(basis.right.scale(slash * 0.14D - 0.42D))
                    .add(basis.up.scale(0.58D));
            Vec3 end = hit.add(basis.right.scale(slash * 0.14D + 0.42D))
                    .add(basis.up.scale(-0.58D));
            jaggedLine(level, start, end, slash % 2 == 0
                    ? ModParticles.METHOD_QI_BLOOD.get() : CORPSE_GREEN, 7, 0.07D);
        }
        level.sendParticles(ModParticles.METHOD_QI_DARK.get(), hit.x, hit.y, hit.z,
                18, target.getBbWidth() * 0.32D, target.getBbHeight() * 0.24D,
                target.getBbWidth() * 0.32D, 0.04D);
    }

    static void shadowTrail(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.01D) {
            return;
        }
        Vec3 direction = delta.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).normalize();
        Vec3 oldFloor = surfacePoint(level, start);
        for (int layer = 0; layer < 11; layer++) {
            double t = layer / 10.0D;
            Vec3 sinking = start.add(0.0D, 0.08D + (1.0D - t) * 1.65D, 0.0D);
            level.sendParticles(layer < 4 ? CORPSE_GRAY : CORPSE_BLACK,
                    sinking.x, sinking.y, sinking.z, 1, 0.16D * (1.0D - t),
                    0.02D, 0.16D * (1.0D - t), 0.0D);
        }
        int steps = Math.max(8, Mth.ceil(length / 0.32D));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double wave = Math.sin(t * Math.PI * 4.0D) * 0.13D;
            Vec3 center = surfacePoint(level, start.add(delta.scale(t))).add(0.0D, 0.045D, 0.0D);
            for (int ribbon = -1; ribbon <= 1; ribbon += 2) {
                Vec3 point = center.add(side.scale(wave + ribbon * 0.16D));
                directed(level, ribbon < 0 ? CORPSE_BLACK : CORPSE_GRAY,
                        point, direction.scale(0.032D));
            }
            if (i % 5 == 2) {
                double faceOffset = 0.12D + (i % 3) * 0.06D;
                level.sendParticles(ModParticles.BLACK_FLAME_ASH.get(), center.x,
                        center.y + faceOffset, center.z, 2, 0.10D, 0.05D, 0.10D, 0.002D);
            }
        }
        Vec3 endFloor = surfacePoint(level, end);
        for (int layer = 0; layer < 11; layer++) {
            double t = layer / 10.0D;
            Vec3 emerging = endFloor.add(0.0D, 0.10D + t * 1.65D, 0.0D);
            level.sendParticles(layer > 7 ? CORPSE_GRAY : CORPSE_BLACK,
                    emerging.x, emerging.y, emerging.z, 1, 0.18D * t,
                    0.025D, 0.18D * t, 0.0D);
        }
        irregularGroundFront(level, endFloor, CORPSE_GRAY, 1.25D, 20, 0.055D, 0.08D);
        level.sendParticles(ModParticles.BLACK_FLAME_ASH.get(), oldFloor.x,
                oldFloor.y + 0.04D, oldFloor.z, 18, 0.28D, 0.012D, 0.28D, 0.003D);
    }

    static void boneGrowth(ServerLevel level, LivingEntity caster) {
        Basis body = bodyBasis(caster);
        double h = caster.getBbHeight();
        Vec3 root = caster.position();
        boneSegment(level, root.add(0.0D, h * 0.74D, 0.0D),
                root.add(body.right.scale(0.52D)).add(0.0D, h * 0.78D, 0.0D), 0);
        boneSegment(level, root.add(0.0D, h * 0.20D, 0.0D),
                root.add(0.0D, h * 0.83D, 0.0D), 1);
        boneSegment(level, root.add(body.right.scale(0.38D)).add(0.0D, h * 0.66D, 0.0D),
                root.add(body.right.scale(0.70D)).add(0.0D, h * 0.36D, 0.0D), 2);
        boneSegment(level, root.add(body.right.scale(-0.38D)).add(0.0D, h * 0.66D, 0.0D),
                root.add(body.right.scale(-0.70D)).add(0.0D, h * 0.36D, 0.0D), 3);
        level.sendParticles(ParticleTypes.ASH, caster.getX(), caster.getY() + h * 0.58D,
                caster.getZ(), 18, 0.42D, h * 0.34D, 0.42D, 0.012D);
        level.sendParticles(ModParticles.METHOD_QI_DARK.get(), caster.getX(),
                caster.getY() + h * 0.52D, caster.getZ(), 10,
                0.35D, h * 0.30D, 0.35D, 0.012D);
    }

    static void fireClaws(ServerLevel level, LivingEntity caster, boolean activation) {
        Basis basis = bodyBasis(caster);
        Vec3 chest = caster.position().add(0.0D, caster.getBbHeight() * 0.62D, 0.0D);
        for (int side : new int[]{-1, 1}) {
            Vec3 hand = chest.add(basis.right.scale(side * 0.42D))
                    .add(basis.forward.scale(0.28D)).add(0.0D, -0.34D, 0.0D);
            level.sendParticles(ModParticles.BLACK_FLAME_CORE.get(), hand.x, hand.y, hand.z,
                    activation ? 10 : 2, 0.11D, 0.12D, 0.11D, activation ? 0.025D : 0.005D);
            level.sendParticles(COLD_CYAN, hand.x, hand.y, hand.z,
                    activation ? 14 : 3, 0.16D, 0.15D, 0.16D, activation ? 0.020D : 0.004D);
            if (activation) {
                for (int finger = 0; finger < 4; finger++) {
                    Vec3 tip = hand.add(basis.forward.scale(0.10D + finger * 0.045D))
                            .add(basis.right.scale(side * (finger - 1.5D) * 0.035D));
                    directed(level, finger % 2 == 0 ? COLD_CYAN : CORPSE_BLACK,
                            tip, basis.forward.scale(-0.018D));
                }
            }
        }
    }

    static void fireClawHit(ServerLevel level, LivingEntity caster, LivingEntity target) {
        Basis basis = bodyBasis(caster);
        Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        for (int slash = -2; slash <= 2; slash++) {
            Vec3 start = hit.add(basis.right.scale(-0.42D + slash * 0.09D))
                    .add(basis.up.scale(0.36D));
            Vec3 end = hit.add(basis.right.scale(0.42D + slash * 0.09D))
                    .add(basis.up.scale(-0.32D));
            line(level, start, end, slash % 2 == 0 ? COLD_CYAN : CORPSE_BLACK, 0.11D);
        }
        level.sendParticles(ModParticles.BLACK_FLAME_CORE.get(), hit.x, hit.y, hit.z,
                8, target.getBbWidth() * 0.24D, target.getBbHeight() * 0.18D,
                target.getBbWidth() * 0.24D, 0.006D);
    }

    static void summonSeal(ServerLevel level, Vec3 center, int index) {
        Vec3 floor = surfacePoint(level, center).add(0.0D, 0.03D, 0.0D);
        irregularGroundFront(level, floor, ModParticles.METHOD_QI_BLOOD.get(),
                0.92D, 24, 0.0D, 0.08D);
        double rotation = index * Math.PI / 3.0D;
        for (int spoke = 0; spoke < 6; spoke++) {
            double angle = rotation + Math.PI * 2.0D * spoke / 6.0D;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            dragGroundLine(level, floor.add(radial.scale(0.18D)),
                    floor.add(radial.scale(0.78D)), spoke % 2 == 0
                            ? DIRTY_GREEN : ModParticles.METHOD_QI_BLOOD.get(), 0.14D);
        }
        surfaceBlockDust(level, floor, 16);
        for (int layer = 0; layer < 8; layer++) {
            double y = floor.y + 0.15D + layer * 0.20D;
            level.sendParticles(layer < 3 ? CORPSE_BLACK : DIRTY_GREEN,
                    floor.x, y, floor.z, 2, 0.20D, 0.02D, 0.20D, 0.0D);
        }
    }

    static void summonConnection(ServerLevel level, LivingEntity caster,
                                 List<? extends LivingEntity> summons) {
        Vec3 hand = caster.position().add(0.0D, caster.getBbHeight() * 0.62D, 0.0D);
        for (LivingEntity summon : summons) {
            jaggedLine(level, hand, summon.position().add(0.0D, summon.getBbHeight() * 0.52D, 0.0D),
                    ModParticles.METHOD_QI_BLOOD.get(), 11, 0.09D);
        }
    }

    static void bodyBurst(ServerLevel level, LivingEntity caster, double radius) {
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.48D, 0.0D);
        sphere(level, center, radius, ModParticles.METHOD_QI_DARK.get(), 90, true);
        sphere(level, center, radius * 0.72D, CORPSE_GREEN, 56, true);
        radialBurst(level, center, ModParticles.BLACK_FLAME_ASH.get(), 48, 0.28D, 0.18D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                70, radius * 0.34D, radius * 0.20D, radius * 0.34D, 0.075D);
    }

    static void corpseStreams(ServerLevel level, List<CultivatorCorpseEntity> corpses,
                              LivingEntity caster, boolean boneUnity) {
        Vec3 destination = caster.position().add(0.0D, caster.getBbHeight() * 0.62D, 0.0D);
        for (int index = 0; index < corpses.size(); index++) {
            CultivatorCorpseEntity corpse = corpses.get(index);
            Vec3 body = corpse.position();
            if (boneUnity) {
                boneOrbitStream(level, corpse, destination, index, corpses.size());
            } else {
                Vec3 chest = body.add(0.0D, corpse.getBbHeight() * 0.55D, 0.0D);
                curvedDoubleStream(level, chest, destination, CORPSE_BLACK,
                        CORPSE_GREEN, 19, 0.20D + index * 0.015D, 0.82D + index * 0.12D);
                curvedDoubleStream(level, body.add(0.0D, corpse.getBbHeight() * 0.82D, 0.0D),
                        destination, DIRTY_GREEN, ModParticles.BLACK_FLAME_ASH.get(),
                        13, 0.10D, 0.55D);
                for (int limb = 0; limb < 4; limb++) {
                    double x = (limb < 2 ? -1.0D : 1.0D) * corpse.getBbWidth() * 0.32D;
                    double y = corpse.getBbHeight() * (limb % 2 == 0 ? 0.28D : 0.68D);
                    curvedDoubleStream(level, body.add(x, y, 0.0D), destination,
                            CORPSE_GRAY, CORPSE_BLACK, 9, 0.08D, 0.35D);
                }
            }
            corpseCollapse(level, corpse, index);
        }
        int ringPoints = Math.min(72, 24 + corpses.size() * 5);
        orbit(level, destination.add(0.0D, 0.35D, 0.0D),
                boneUnity ? BONE_WHITE : CORPSE_BLACK,
                Math.min(2.6D, 0.75D + corpses.size() * 0.13D), ringPoints);
        inwardDisc(level, destination, boneUnity ? BONE_GOLD : CORPSE_GREEN,
                Math.min(1.8D, 0.65D + corpses.size() * 0.07D), 30);
        radialBurst(level, destination, boneUnity ? BONE_GOLD : DIRTY_GREEN, 22, 0.08D, 0.04D);
    }

    static void corpseChannel(ServerLevel level, List<CultivatorCorpseEntity> corpses,
                              LivingEntity caster, boolean boneUnity) {
        Vec3 destination = caster.position().add(0.0D, caster.getBbHeight() * 0.62D, 0.0D);
        int limit = Math.min(corpses.size(), boneUnity ? 12 : 8);
        for (int index = 0; index < limit; index++) {
            CultivatorCorpseEntity corpse = corpses.get(index);
            Vec3 chest = corpse.position().add(0.0D, corpse.getBbHeight() * 0.52D, 0.0D);
            level.sendParticles(boneUnity ? BONE_WHITE : CORPSE_BLACK,
                    chest.x, chest.y, chest.z, 4, corpse.getBbWidth() * 0.34D,
                    corpse.getBbHeight() * 0.26D, corpse.getBbWidth() * 0.34D, 0.0D);
            curvedDoubleStream(level, chest, destination,
                    boneUnity ? BONE_WHITE : CORPSE_BLACK,
                    boneUnity ? BONE_GOLD : DIRTY_GREEN,
                    10, 0.08D + index * 0.012D, 0.38D + index * 0.06D);
        }
    }

    static void drainStream(ServerLevel level, LivingEntity source, LivingEntity destination) {
        Vec3 start = source.position().add(0.0D, source.getBbHeight() * 0.62D, 0.0D);
        Vec3 end = destination.position().add(0.0D, destination.getBbHeight() * 0.62D, 0.0D);
        oppositeBodyRings(level, source, PALE_RED);
        for (int stream = 0; stream < 5; stream++) {
            Vec3 streamStart = start.add((source.getRandom().nextDouble() - 0.5D) * source.getBbWidth(),
                    (source.getRandom().nextDouble() - 0.5D) * source.getBbHeight() * 0.7D,
                    (source.getRandom().nextDouble() - 0.5D) * source.getBbWidth());
            curvedDoubleStream(level, streamStart, end,
                    stream < 3 ? PALE_RED : ModParticles.METHOD_QI_BLOOD.get(),
                    stream % 2 == 0 ? CORPSE_BLACK : DIRTY_GREEN,
                    17, 0.11D + stream * 0.025D, 0.32D + stream * 0.09D);
        }
        level.sendParticles(CORPSE_BLACK, destination.getX(),
                destination.getY() + destination.getBbHeight() * 0.58D, destination.getZ(),
                9, 0.24D, 0.32D, 0.24D, 0.012D);
    }

    static void shadowCopy(ServerLevel level, Vec3 position, double height) {
        Vec3 floor = surfacePoint(level, position);
        for (int layer = 0; layer < 14; layer++) {
            double t = layer / 13.0D;
            double width = 0.34D * Math.sin(Math.PI * Math.min(1.0D, t * 1.25D));
            Vec3 center = floor.add(0.0D, 0.03D + height * t, 0.0D);
            level.sendParticles(layer % 4 == 0 ? CORPSE_GRAY : CORPSE_BLACK,
                    center.x, center.y, center.z, 2, width, 0.015D, width, 0.0D);
        }
        dragGroundLine(level, floor.add(-0.65D, 0.0D, 0.0D), floor.add(0.65D, 0.0D, 0.0D),
                CORPSE_BLACK, 0.12D);
        level.sendParticles(ModParticles.BLACK_FLAME_ASH.get(), position.x,
                position.y + height * 0.52D, position.z, 9, 0.22D, height * 0.32D, 0.22D, 0.012D);
    }

    static void immortalBones(ServerLevel level, LivingEntity caster) {
        drawInternalSkeleton(level, caster, BONE_WHITE, BONE_GOLD, true);
        level.sendParticles(CORPSE_GRAY, caster.getX(), caster.getY() + caster.getBbHeight() * 0.50D,
                caster.getZ(), 8, 0.24D, caster.getBbHeight() * 0.32D, 0.24D, 0.006D);
    }

    static void immortalBoneImpact(ServerLevel level, LivingEntity target, Vec3 source) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 impact = source == null ? center.add(target.getLookAngle().scale(-target.getBbWidth()))
                : closestBodyPoint(target, source);
        jaggedLine(level, impact, center, BONE_GOLD, 7, 0.035D);
        drawInternalSkeleton(level, target, BONE_GOLD, BONE_WHITE, false);
        radialBurst(level, impact, BONE_GOLD, 8, 0.045D, 0.02D);
    }

    static void hardenedImpact(ServerLevel level, LivingEntity target, Vec3 source) {
        Vec3 impact = source == null
                ? target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D)
                : closestBodyPoint(target, source);
        irregularGroundFront(level, impact, CORPSE_GRAY, 0.55D, 12, 0.025D, 0.10D);
        for (int crack = 0; crack < 5; crack++) {
            Vec3 end = impact.add((level.random.nextDouble() - 0.5D) * 0.65D,
                    (level.random.nextDouble() - 0.5D) * 0.65D,
                    (level.random.nextDouble() - 0.5D) * 0.65D);
            jaggedLine(level, impact, end, CORPSE_BLACK, 4, 0.025D);
        }
        level.sendParticles(ModParticles.BLACK_FLAME_ASH.get(), impact.x, impact.y, impact.z,
                10, 0.16D, 0.16D, 0.16D, 0.018D);
    }

    static void boneGrowthImpact(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.56D, 0.0D);
        level.sendParticles(BONE_WHITE, center.x, center.y, center.z, 12,
                target.getBbWidth() * 0.34D, target.getBbHeight() * 0.24D,
                target.getBbWidth() * 0.34D, 0.018D);
        level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 8,
                0.25D, 0.28D, 0.25D, 0.012D);
    }

    static void bonePrison(ServerLevel level, Vec3 center) {
        Vec3 floor = surfacePoint(level, center).add(0.0D, 0.04D, 0.0D);
        for (int rib = 0; rib < 12; rib++) {
            double angle = Math.PI * 2.0D * rib / 12.0D;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            int points = rib % 3 == 0 ? 16 : 12;
            double height = rib % 3 == 0 ? 5.4D : 4.7D;
            for (int point = 0; point <= points; point++) {
                double t = point / (double) points;
                double radius = 4.7D * Math.pow(Math.cos(t * Math.PI * 0.5D), 0.82D);
                double y = Math.sin(t * Math.PI * 0.5D) * height;
                Vec3 position = floor.add(radial.scale(radius)).add(0.0D, y, 0.0D);
                level.sendParticles(point % 4 == 0 ? CORPSE_BLACK : BONE_WHITE,
                        position.x, position.y, position.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }
        }
        for (int point = 0; point <= 18; point++) {
            double t = point / 18.0D;
            Vec3 spine = floor.add(0.0D, t * 5.2D, -4.45D + Math.sin(t * Math.PI) * 0.45D);
            level.sendParticles(point % 2 == 0 ? BONE_WHITE : CORPSE_BLACK,
                    spine.x, spine.y, spine.z, 2, 0.06D, 0.03D, 0.06D, 0.0D);
        }
        level.sendParticles(ParticleTypes.ASH, center.x, center.y + 0.3D, center.z,
                42, 3.4D, 1.9D, 3.4D, 0.018D);
    }

    static void boneReshape(ServerLevel level, LivingEntity caster) {
        drawInternalSkeleton(level, caster, BONE_WHITE, BONE_GOLD, true);
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.52D, 0.0D);
        for (int section = 0; section < 6; section++) {
            double y = caster.getY() + caster.getBbHeight() * (0.16D + section * 0.14D);
            Vec3 sectionCenter = new Vec3(caster.getX(), y, caster.getZ());
            inwardDisc(level, sectionCenter, section < 3 ? CORPSE_BLACK : PALE_RED,
                    0.55D - section * 0.035D, 10);
        }
        irregularGroundFront(level, surfacePoint(level, caster.position()), CORPSE_GRAY,
                1.5D, 24, 0.055D, 0.12D);
        radialBurst(level, center, DIRTY_GREEN, 20, 0.07D, 0.035D);
    }

    static void baFire(ServerLevel level, Vec3 center, double radius) {
        Vec3 floor = surfacePoint(level, center);
        for (int ring = 0; ring < 6; ring++) {
            double ringRadius = radius * (ring + 1.0D) / 6.0D;
            irregularGroundFront(level, floor, ring % 2 == 0 ? DROUGHT_RED : CORPSE_BLACK,
                    ringRadius, 34 + ring * 6, 0.08D + ring * 0.015D, 0.24D);
        }
        level.sendParticles(ModParticles.SOLAR_EMBER.get(), center.x, center.y + 0.8D, center.z,
                85, radius * 0.42D, 0.85D, radius * 0.42D, 0.08D);
        level.sendParticles(ModParticles.BLACK_FLAME_CORE.get(), center.x, center.y + 0.32D, center.z,
                100, radius * 0.40D, 0.28D, radius * 0.40D, 0.035D);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, center.x, center.y + 0.18D, center.z,
                55, radius * 0.34D, 0.10D, radius * 0.34D, 0.045D);
    }

    static void domain(ServerLevel level, String ability, Vec3 center, double radius) {
        switch (ability) {
            case "fudi_shiyu" -> corruptField(level, center, radius);
            case "shiyu_tianmu" -> corpseSkyDomain(level, center, radius);
            case "dimai_shihua" -> earthMeridianDomain(level, center, radius);
            case "chidi_qianli" -> droughtDomain(level, center, radius, true);
            case "fenxue_hanqi" -> droughtDomain(level, center, radius, false);
            case "tianhan_lingyu" -> elementSuppressionDomain(level, center, radius);
            default -> corruptField(level, center, radius);
        }
    }

    static void domainVictim(ServerLevel level, String ability, LivingEntity target) {
        Vec3 feet = surfacePoint(level, target.position());
        if ("chidi_qianli".equals(ability) || "fenxue_hanqi".equals(ability)) {
            for (int vent = 0; vent < 7; vent++) {
                Vec3 start = target.position().add(
                        (level.random.nextDouble() - 0.5D) * target.getBbWidth(),
                        target.getBbHeight() * (0.12D + level.random.nextDouble() * 0.72D),
                        (level.random.nextDouble() - 0.5D) * target.getBbWidth());
                directed(level, vent % 3 == 0 ? PALE_RED : ParticleTypes.WHITE_SMOKE,
                        start, new Vec3(0.0D, 0.055D + level.random.nextDouble() * 0.035D, 0.0D));
            }
            irregularGroundFront(level, feet, DRY_DUST, 0.72D, 14, 0.02D, 0.12D);
        } else if ("shiyu_tianmu".equals(ability) || "dimai_shihua".equals(ability)
                || "fudi_shiyu".equals(ability)) {
            for (int wisp = 0; wisp < 7; wisp++) {
                Vec3 start = feet.add((level.random.nextDouble() - 0.5D) * 0.65D,
                        0.08D, (level.random.nextDouble() - 0.5D) * 0.65D);
                Vec3 ankle = target.position().add(0.0D,
                        Math.min(0.45D, target.getBbHeight() * 0.28D), 0.0D);
                curvedDoubleStream(level, start, ankle, DIRTY_GREEN, CORPSE_BLACK,
                        5, 0.05D, 0.08D);
            }
        }
    }

    static void domainAllyPulse(ServerLevel level, LivingEntity target) {
        Vec3 floor = surfacePoint(level, target.position());
        irregularGroundFront(level, floor, CORPSE_GREEN, 0.86D, 16, -0.025D, 0.08D);
        for (int thread = 0; thread < 6; thread++) {
            double angle = Math.PI * 2.0D * thread / 6.0D;
            Vec3 start = floor.add(Math.cos(angle) * 0.45D, 0.05D, Math.sin(angle) * 0.45D);
            Vec3 ankle = target.position().add(0.0D, Math.min(0.40D, target.getBbHeight() * 0.24D), 0.0D);
            curvedDoubleStream(level, start, ankle, CORPSE_BLACK, DIRTY_GREEN,
                    6, 0.045D, 0.06D);
        }
        level.sendParticles(CORPSE_GREEN, target.getX(),
                target.getY() + target.getEyeHeight() * 0.92D, target.getZ(),
                4, 0.08D, 0.05D, 0.08D, 0.0D);
    }

    static void healingPulse(ServerLevel level, LivingEntity entity) {
        Vec3 chest = entity.position().add(0.0D, entity.getBbHeight() * 0.58D, 0.0D);
        inwardDisc(level, chest, CORPSE_GREEN, 0.58D, 14);
        irregularGroundFront(level, surfacePoint(level, entity.position()), DIRTY_GREEN,
                1.1D, 18, 0.035D, 0.10D);
        level.sendParticles(CORPSE_GREEN, entity.getX(), entity.getEyeY() - 0.10D,
                entity.getZ(), 5, 0.09D, 0.04D, 0.09D, 0.0D);
    }

    static void ashDisappear(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(ModParticles.BLACK_FLAME_ASH.get(), center.x, center.y, center.z,
                28, 0.35D, entity.getBbHeight() * 0.40D, 0.35D, 0.04D);
        radialBurst(level, center, ModParticles.METHOD_QI_DARK.get(), 18, 0.09D, 0.12D);
    }

    static void hardenedShell(ServerLevel level, LivingEntity entity, boolean start) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        if (start) {
            for (int i = 0; i < 30; i++) {
                Vec3 direction = fibonacciDirection(i, 30);
                Vec3 point = center.add(direction.multiply(
                        entity.getBbWidth() * 0.72D, entity.getBbHeight() * 0.52D,
                        entity.getBbWidth() * 0.72D));
                directed(level, i % 4 == 0 ? CORPSE_GRAY : CORPSE_BLACK, point,
                        center.subtract(point).normalize().scale(0.085D));
            }
            for (int step = 0; step < 15; step++) {
                double t = step / 14.0D;
                Vec3 shimmer = entity.position().add(0.0D, 0.06D + t * entity.getBbHeight(), 0.0D);
                level.sendParticles(step % 3 == 0 ? CORPSE_GRAY : ModParticles.BLACK_FLAME_ASH.get(),
                        shimmer.x, shimmer.y, shimmer.z, 3,
                        entity.getBbWidth() * 0.38D, 0.02D,
                        entity.getBbWidth() * 0.38D, 0.0D);
            }
        } else {
            Basis body = bodyBasis(entity);
            double h = entity.getBbHeight();
            Vec3 root = entity.position();
            jaggedLine(level, root.add(body.right.scale(-0.34D)).add(0.0D, h * 0.68D, 0.0D),
                    root.add(body.right.scale(-0.46D)).add(0.0D, h * 0.40D, 0.0D),
                    CORPSE_BLACK, 5, 0.025D);
            jaggedLine(level, root.add(body.right.scale(0.34D)).add(0.0D, h * 0.68D, 0.0D),
                    root.add(body.right.scale(0.46D)).add(0.0D, h * 0.40D, 0.0D),
                    CORPSE_BLACK, 5, 0.025D);
            level.sendParticles(CORPSE_GRAY, center.x, center.y, center.z, 3,
                    entity.getBbWidth() * 0.34D, h * 0.27D,
                    entity.getBbWidth() * 0.34D, 0.0D);
        }
    }

    static void hardenedRelease(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D);
        radialBurst(level, center, CORPSE_GRAY, 30, 0.10D, 0.055D);
        level.sendParticles(ModParticles.BLACK_FLAME_ASH.get(), center.x, center.y, center.z,
                26, entity.getBbWidth() * 0.42D, entity.getBbHeight() * 0.35D,
                entity.getBbWidth() * 0.42D, 0.025D);
    }

    static void boneGrowthEnd(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
        level.sendParticles(BONE_WHITE, center.x, center.y, center.z, 24,
                entity.getBbWidth() * 0.52D, entity.getBbHeight() * 0.38D,
                entity.getBbWidth() * 0.52D, 0.025D);
        level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 34,
                entity.getBbWidth() * 0.60D, entity.getBbHeight() * 0.42D,
                entity.getBbWidth() * 0.60D, 0.022D);
        inwardDisc(level, center, CORPSE_BLACK, 0.72D, 18);
    }

    static void immortalBonesEnd(ServerLevel level, LivingEntity entity) {
        drawInternalSkeleton(level, entity, CORPSE_GRAY, CORPSE_BLACK, false);
        level.sendParticles(CORPSE_GRAY, entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.54D, entity.getZ(),
                18, 0.28D, entity.getBbHeight() * 0.34D, 0.28D, 0.016D);
    }

    private static int castDuration(String ability) {
        return switch (ability) {
            case "shiying_pusha" -> 7;
            case "shiqi_tuxi" -> 12;
            case "shihou" -> 16;
            case "fudi_shiyu", "shigu_baosheng", "dimai_shihua", "tianhan_lingyu" -> 20;
            case "guyu_fengshen" -> 24;
            case "tunshi_huisheng" -> 30;
            case "zhaoshi", "wanshi_guiqi", "shiyu_tianmu", "bahuo_linshi" -> 40;
            case "shigu_zaishu" -> 50;
            case "shesheng_duoxue", "chidi_qianli" -> 60;
            default -> 20;
        };
    }

    private static void breathCharge(ServerLevel level, LivingEntity caster, double progress) {
        Basis body = bodyBasis(caster);
        Vec3 throat = caster.position().add(0.0D, caster.getBbHeight() * 0.78D, 0.0D)
                .add(body.forward.scale(0.16D));
        Vec3 torso = caster.position().add(0.0D, caster.getBbHeight() * 0.48D, 0.0D);
        Vec3[] starts = {
                torso.add(body.right.scale(-0.42D)), torso.add(body.right.scale(0.42D)),
                torso.add(0.0D, caster.getBbHeight() * 0.18D, 0.0D),
                torso.add(body.forward.scale(-0.26D))
        };
        for (int i = 0; i < starts.length; i++) {
            curvedDoubleStream(level, starts[i], throat, i % 2 == 0 ? CORPSE_GRAY : CORPSE_BLACK,
                    DIRTY_GREEN, 6, 0.05D + progress * 0.04D, 0.10D);
        }
        jaggedLine(level, torso.add(0.0D, caster.getBbHeight() * 0.12D, 0.0D), throat,
                DIRTY_GREEN, 6, 0.025D);
        Vec3 mouth = caster.getEyePosition().add(body.forward.scale(0.26D)).add(0.0D, -0.11D, 0.0D);
        level.sendParticles(CORPSE_BLACK, mouth.x, mouth.y, mouth.z,
                3 + Mth.floor(progress * 4.0D), 0.06D, 0.05D, 0.06D, 0.0D);
    }

    private static void roarCharge(ServerLevel level, LivingEntity caster, double progress) {
        Basis body = bodyBasis(caster);
        Vec3 mouth = caster.getEyePosition().add(body.forward.scale(0.25D)).add(0.0D, -0.10D, 0.0D);
        Vec3 floor = surfacePoint(level, caster.position());
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2.0D * i / 10.0D + caster.tickCount * 0.04D;
            Vec3 start = floor.add(Math.cos(angle) * (1.4D + progress),
                    0.04D + (i % 3) * 0.10D, Math.sin(angle) * (1.4D + progress));
            directed(level, i % 3 == 0 ? ParticleTypes.ASH : CORPSE_GRAY,
                    start, mouth.subtract(start).normalize().scale(0.045D + progress * 0.035D));
        }
        level.sendParticles(CORPSE_BLACK, mouth.x, mouth.y, mouth.z,
                3 + Mth.floor(progress * 6.0D), 0.08D, 0.06D, 0.08D, 0.0D);
    }

    private static void pounceCharge(ServerLevel level, LivingEntity caster,
                                     LivingEntity target, double progress) {
        Basis body = bodyBasis(caster);
        Vec3 floor = surfacePoint(level, caster.position());
        Vec3 spine = caster.position().add(body.forward.scale(-0.20D))
                .add(0.0D, caster.getBbHeight() * 0.48D, 0.0D);
        for (int i = 0; i < 8; i++) {
            Vec3 start = spine.add(body.right.scale((i - 3.5D) * 0.11D))
                    .add(0.0D, (i % 3) * 0.14D - 0.18D, 0.0D);
            directed(level, i % 3 == 0 ? CORPSE_GRAY : CORPSE_BLACK,
                    start, body.forward.scale(-0.035D));
        }
        Vec3 end = target == null ? floor.add(body.forward.scale(3.0D + progress * 2.0D))
                : surfacePoint(level, target.position());
        dragGroundLine(level, floor, floor.add(end.subtract(floor).scale(0.55D + progress * 0.45D)),
                CORPSE_BLACK, 0.18D);
    }

    private static void terrainCharge(ServerLevel level, LivingEntity caster,
                                      double progress, boolean drought) {
        Vec3 floor = surfacePoint(level, caster.position());
        if (caster.tickCount % 2 == 0) {
            int branches = 3 + Mth.floor(progress * 4.0D);
            for (int branch = 0; branch < branches; branch++) {
                double angle = Math.PI * 2.0D * branch / branches + caster.getId() * 0.37D;
                double length = 0.7D + progress * (drought ? 5.0D : 3.8D);
                terrainVein(level, floor, angle, length,
                        drought ? DRY_DUST : CORPSE_BLACK,
                        drought ? DROUGHT_RED : DIRTY_GREEN, 0.18D);
            }
        }
        level.sendParticles(drought ? ParticleTypes.WHITE_SMOKE : ParticleTypes.LARGE_SMOKE,
                floor.x, floor.y + 0.08D, floor.z, 3, 0.35D, 0.03D, 0.35D, 0.004D);
    }

    private static void corpseDrainCharge(ServerLevel level, LivingEntity caster,
                                          double progress, boolean boneUnity) {
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.58D, 0.0D);
        double radius = 0.55D + progress * (boneUnity ? 1.35D : 0.72D);
        orbit(level, center.add(0.0D, progress * 0.45D, 0.0D),
                boneUnity ? BONE_WHITE : CORPSE_BLACK, radius,
                8 + Mth.floor(progress * 14.0D));
        if (progress > 0.55D) {
            inwardDisc(level, center, boneUnity ? BONE_GOLD : DIRTY_GREEN,
                    radius, 10 + Mth.floor(progress * 8.0D));
        }
    }

    private static void boneGrowthCharge(ServerLevel level, LivingEntity caster, double progress) {
        Basis body = bodyBasis(caster);
        double h = caster.getBbHeight();
        Vec3 root = caster.position();
        int stage = Math.min(3, Mth.floor(progress * 4.0D));
        if (stage >= 0) {
            boneSegment(level, root.add(0.0D, h * 0.73D, 0.0D),
                    root.add(body.right.scale(0.48D)).add(0.0D, h * 0.77D, 0.0D), 0);
        }
        if (stage >= 1) {
            boneSegment(level, root.add(0.0D, h * 0.22D, 0.0D),
                    root.add(0.0D, h * 0.82D, 0.0D), 1);
        }
        if (stage >= 2) {
            boneSegment(level, root.add(body.right.scale(-0.38D)).add(0.0D, h * 0.65D, 0.0D),
                    root.add(body.right.scale(-0.64D)).add(0.0D, h * 0.39D, 0.0D), 2);
        }
        if (stage >= 3) {
            boneSegment(level, root.add(body.right.scale(0.38D)).add(0.0D, h * 0.65D, 0.0D),
                    root.add(body.right.scale(0.64D)).add(0.0D, h * 0.39D, 0.0D), 3);
        }
    }

    private static void summonCharge(ServerLevel level, LivingEntity caster, double progress) {
        Basis body = bodyBasis(caster);
        Vec3 hand = caster.position().add(0.0D, caster.getBbHeight() * 0.56D, 0.0D)
                .add(body.forward.scale(0.18D));
        Vec3 floor = surfacePoint(level, caster.position());
        for (int thread = 0; thread < 8; thread++) {
            double angle = Math.PI * 2.0D * thread / 8.0D;
            Vec3 end = floor.add(Math.cos(angle) * (0.35D + progress * 1.8D),
                    0.02D, Math.sin(angle) * (0.35D + progress * 1.8D));
            curvedDoubleStream(level, hand.add(body.right.scale((thread - 3.5D) * 0.035D)), end,
                    CORPSE_BLACK, DIRTY_GREEN, 7, 0.045D, 0.10D);
        }
        if (progress > 0.25D && caster.tickCount % 2 == 0) {
            for (int index = 0; index < 3; index++) {
                double angle = Math.PI * 2.0D * index / 3.0D;
                Vec3 mark = surfacePoint(level, caster.position().add(
                        Math.cos(angle) * 2.2D, 0.0D, Math.sin(angle) * 2.2D));
                irregularGroundFront(level, mark, ModParticles.METHOD_QI_BLOOD.get(),
                        0.25D + 0.62D * progress, 10 + Mth.floor(progress * 10.0D), 0.0D, 0.06D);
                level.sendParticles(ParticleTypes.SMOKE, mark.x, mark.y + 0.05D, mark.z,
                        2, 0.20D, 0.02D, 0.20D, 0.002D);
            }
        }
    }

    private static void corpseSkyCharge(ServerLevel level, LivingEntity caster, double progress) {
        terrainCharge(level, caster, progress, false);
        Vec3 center = caster.position().add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 7; i++) {
            Vec3 start = center.add((level.random.nextDouble() - 0.5D) * 1.1D,
                    level.random.nextDouble() * caster.getBbHeight(),
                    (level.random.nextDouble() - 0.5D) * 1.1D);
            directed(level, i % 3 == 0 ? DIRTY_GREEN : CORPSE_BLACK,
                    start, new Vec3(0.0D, 0.07D + progress * 0.05D, 0.0D));
        }
        Vec3 cloud = center.add(0.0D, 7.0D + progress * 5.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, cloud.x, cloud.y, cloud.z,
                3 + Mth.floor(progress * 5.0D), 1.5D + progress * 3.5D,
                0.20D, 1.5D + progress * 3.5D, 0.002D);
    }

    private static void lifeDrainCharge(ServerLevel level, LivingEntity caster,
                                        LivingEntity target, double progress) {
        if (target == null) {
            return;
        }
        oppositeBodyRings(level, target, PALE_RED);
        Vec3 start = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        Vec3 end = caster.position().add(0.0D, caster.getBbHeight() * 0.60D, 0.0D);
        int streams = 1 + Mth.floor(progress * 4.0D);
        for (int i = 0; i < streams; i++) {
            Vec3 offset = start.add((level.random.nextDouble() - 0.5D) * target.getBbWidth(),
                    (level.random.nextDouble() - 0.5D) * target.getBbHeight() * 0.35D,
                    (level.random.nextDouble() - 0.5D) * target.getBbWidth());
            curvedDoubleStream(level, offset, end, PALE_RED,
                    i % 2 == 0 ? CORPSE_BLACK : DIRTY_GREEN, 11, 0.08D + i * 0.025D,
                    0.22D + progress * 0.38D);
        }
    }

    private static void bonePrisonWarning(ServerLevel level, LivingEntity target, double progress) {
        if (target == null) {
            return;
        }
        Vec3 floor = surfacePoint(level, target.position());
        int shadows = 4 + Mth.floor(progress * 4.0D);
        for (int rib = 0; rib < shadows; rib++) {
            double angle = Math.PI * 2.0D * rib / shadows + Math.sin(target.tickCount * 0.17D) * 0.08D;
            Vec3 start = floor.add(Math.cos(angle) * 1.1D, 0.02D, Math.sin(angle) * 1.1D);
            Vec3 end = floor.add(Math.cos(angle) * (4.3D - progress * 0.4D),
                    0.02D, Math.sin(angle) * (4.3D - progress * 0.4D));
            dragGroundLine(level, start, end, rib % 2 == 0 ? BONE_WHITE : CORPSE_GRAY, 0.24D);
        }
    }

    private static void boneReshapeCharge(ServerLevel level, LivingEntity caster, double progress) {
        drawInternalSkeleton(level, caster, progress < 0.5D ? CORPSE_GRAY : BONE_WHITE,
                progress < 0.7D ? CORPSE_BLACK : BONE_GOLD, false);
        int sections = 1 + Mth.floor(progress * 6.0D);
        for (int section = 0; section < sections; section++) {
            Vec3 point = caster.position().add(0.0D,
                    caster.getBbHeight() * (0.12D + section * 0.13D), 0.0D);
            inwardDisc(level, point, section % 2 == 0 ? CORPSE_BLACK : PALE_RED,
                    0.58D, 7);
        }
    }

    private static void droughtCharge(ServerLevel level, LivingEntity caster, double progress) {
        terrainCharge(level, caster, progress, true);
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.58D, 0.0D);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, center.x, center.y, center.z,
                2 + Mth.floor(progress * 5.0D), 0.35D + progress * 0.35D,
                0.32D, 0.35D + progress * 0.35D, 0.018D);
    }

    private static void baFireCharge(ServerLevel level, LivingEntity caster, double progress) {
        Basis body = bodyBasis(caster);
        Vec3 hand = caster.position().add(0.0D, caster.getBbHeight() * 0.68D, 0.0D)
                .add(body.forward.scale(0.44D)).add(body.right.scale(0.36D));
        double radius = 0.04D + progress * 0.58D;
        sphere(level, hand, radius, CORPSE_BLACK, 9 + Mth.floor(progress * 22.0D), false);
        if (progress > 0.2D) {
            sphere(level, hand, radius * 1.10D, DROUGHT_RED,
                    7 + Mth.floor(progress * 16.0D), false);
        }
        for (int i = 0; i < 4; i++) {
            Vec3 steam = surfacePoint(level, caster.position().add(
                    (level.random.nextDouble() - 0.5D) * 2.2D, 0.0D,
                    (level.random.nextDouble() - 0.5D) * 2.2D));
            directed(level, ParticleTypes.WHITE_SMOKE, steam,
                    new Vec3(0.0D, 0.035D + progress * 0.025D, 0.0D));
        }
    }

    private static void inwardBodyCharge(ServerLevel level, LivingEntity caster,
                                         ParticleOptions primary, ParticleOptions accent,
                                         double progress) {
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.54D, 0.0D);
        for (int i = 0; i < 10; i++) {
            Vec3 direction = fibonacciDirection(i, 10);
            Vec3 start = center.add(direction.multiply(0.55D + progress * 0.35D,
                    caster.getBbHeight() * 0.44D, 0.55D + progress * 0.35D));
            directed(level, i % 3 == 0 ? accent : primary,
                    start, center.subtract(start).normalize().scale(0.055D));
        }
    }

    private static void corruptField(ServerLevel level, Vec3 center, double radius) {
        for (int i = 0; i < 5; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(level.random.nextDouble()) * radius;
            Vec3 floor = surfacePoint(level, center.add(Math.cos(angle) * distance, 0.0D,
                    Math.sin(angle) * distance));
            level.sendParticles(i % 2 == 0 ? ParticleTypes.SMOKE : ModParticles.BLACK_FLAME_ASH.get(),
                    floor.x, floor.y + 0.05D, floor.z, 2, 0.18D, 0.035D, 0.18D, 0.004D);
        }
        if (level.getGameTime() % 20L == 0L) {
            Vec3 floor = surfacePoint(level, center);
            for (int branch = 0; branch < 7; branch++) {
                terrainVein(level, floor, branch * Math.PI * 2.0D / 7.0D + center.x * 0.03D,
                        radius * (0.62D + (branch % 3) * 0.16D), CORPSE_BLACK, DIRTY_GREEN, 0.24D);
            }
        }
    }

    private static void corpseSkyDomain(ServerLevel level, Vec3 center, double radius) {
        corruptField(level, center, radius);
        Vec3 origin = center.add(0.0D, 0.4D, 0.0D);
        for (int i = 0; i < 4; i++) {
            Vec3 start = origin.add((level.random.nextDouble() - 0.5D) * 1.4D,
                    level.random.nextDouble() * 2.0D,
                    (level.random.nextDouble() - 0.5D) * 1.4D);
            directed(level, i == 0 ? DIRTY_GREEN : CORPSE_BLACK, start,
                    new Vec3(0.0D, 0.075D, 0.0D));
        }
        Vec3 cloud = center.add(0.0D, 10.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, cloud.x, cloud.y, cloud.z,
                12, radius * 0.50D, 0.35D, radius * 0.50D, 0.003D);
        if (level.getGameTime() % 20L == 0L) {
            irregularGroundFront(level, cloud, DIRTY_GREEN, radius * 0.65D,
                    58, 0.008D, 0.75D);
        }
    }

    private static void earthMeridianDomain(ServerLevel level, Vec3 center, double radius) {
        int pulse = (int) (level.getGameTime() % 60L);
        double currentRadius = pulse < 20 ? radius * 0.25D : pulse < 40 ? radius * 0.50D : radius;
        Vec3 floor = surfacePoint(level, center);
        if (level.getGameTime() % 10L == 0L) {
            for (int branch = 0; branch < 9; branch++) {
                terrainVein(level, floor, branch * Math.PI * 2.0D / 9.0D + center.z * 0.025D,
                        currentRadius * (0.72D + (branch % 4) * 0.08D),
                        CORPSE_BLACK, DIRTY_GREEN, 0.30D);
            }
        }
        for (int i = 0; i < 4; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            Vec3 vent = surfacePoint(level, center.add(Math.cos(angle) * currentRadius * 0.8D,
                    0.0D, Math.sin(angle) * currentRadius * 0.8D));
            directed(level, i == 0 ? CORPSE_GREEN : ParticleTypes.SMOKE,
                    vent, new Vec3(0.0D, 0.028D, 0.0D));
        }
    }

    private static void droughtDomain(ServerLevel level, Vec3 center, double radius,
                                      boolean calamity) {
        for (int i = 0; i < (calamity ? 8 : 5); i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(level.random.nextDouble()) * radius;
            Vec3 floor = surfacePoint(level, center.add(Math.cos(angle) * distance,
                    0.0D, Math.sin(angle) * distance));
            directed(level, i % 3 == 0 ? DRY_DUST : ParticleTypes.WHITE_SMOKE, floor,
                    new Vec3((level.random.nextDouble() - 0.5D) * 0.008D,
                            0.032D + level.random.nextDouble() * 0.035D,
                            (level.random.nextDouble() - 0.5D) * 0.008D));
            if (i % 3 == 0) {
                surfaceBlockDust(level, floor, 2);
            }
        }
        if (calamity && level.getGameTime() % 20L == 0L) {
            irregularGroundFront(level, surfacePoint(level, center), DROUGHT_RED,
                    radius * 0.92D, 72, 0.045D, 0.55D);
        }
    }

    private static void elementSuppressionDomain(ServerLevel level, Vec3 center, double radius) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        Vec3 floor = surfacePoint(level, center);
        for (int point = 0; point < 28; point++) {
            double angle = Math.PI * 2.0D * point / 28.0D;
            Vec3 boundary = surfacePoint(level, floor.add(Math.cos(angle) * radius,
                    0.0D, Math.sin(angle) * radius));
            directed(level, point % 4 == 0 ? DROUGHT_RED : DRY_DUST,
                    boundary, new Vec3(0.0D, 0.018D, 0.0D));
        }
    }

    private static void terrainVein(ServerLevel level, Vec3 origin, double angle,
                                    double length, ParticleOptions core,
                                    ParticleOptions pulse, double spacing) {
        Vec3 previous = surfacePoint(level, origin);
        for (double distance = spacing; distance <= length; distance += spacing) {
            double bend = Math.sin(distance * 1.7D + angle * 2.0D) * 0.22D
                    + Math.sin(distance * 0.61D) * 0.12D;
            Vec3 sample = origin.add(Math.cos(angle + bend) * distance, 0.0D,
                    Math.sin(angle + bend) * distance);
            Vec3 point = surfacePoint(level, sample);
            line(level, previous.add(0.0D, 0.025D, 0.0D), point.add(0.0D, 0.025D, 0.0D),
                    ((int) Math.round(distance / spacing)) % 5 == 0 ? pulse : core,
                    Math.max(0.12D, spacing * 0.65D));
            previous = point;
        }
    }

    private static void boneSegment(ServerLevel level, Vec3 start, Vec3 end, int phase) {
        jaggedLine(level, start, end, phase % 3 == 0 ? BONE_WHITE : BONE_GOLD, 8, 0.025D);
        level.sendParticles(CORPSE_BLACK, start.x, start.y, start.z,
                3, 0.08D, 0.07D, 0.08D, 0.0D);
        level.sendParticles(ParticleTypes.ASH, end.x, end.y, end.z,
                2, 0.06D, 0.05D, 0.06D, 0.006D);
    }

    private static void drawInternalSkeleton(ServerLevel level, LivingEntity entity,
                                             ParticleOptions bone, ParticleOptions cracks,
                                             boolean dense) {
        Basis body = bodyBasis(entity);
        Vec3 root = entity.position();
        double h = entity.getBbHeight();
        Vec3 pelvis = root.add(0.0D, h * 0.34D, 0.0D);
        Vec3 chest = root.add(0.0D, h * 0.68D, 0.0D);
        Vec3 skull = root.add(0.0D, h * 0.88D, 0.0D);
        line(level, pelvis, skull, bone, dense ? 0.09D : 0.14D);
        orientedRing(level, chest, body.right, body.up, bone,
                entity.getBbWidth() * 0.40D, dense ? 18 : 12, 0.0D);
        for (int side : new int[]{-1, 1}) {
            Vec3 shoulder = chest.add(body.right.scale(side * entity.getBbWidth() * 0.43D));
            Vec3 hand = root.add(body.right.scale(side * entity.getBbWidth() * 0.72D))
                    .add(0.0D, h * 0.38D, 0.0D);
            Vec3 hip = pelvis.add(body.right.scale(side * entity.getBbWidth() * 0.24D));
            Vec3 foot = root.add(body.right.scale(side * entity.getBbWidth() * 0.22D))
                    .add(0.0D, h * 0.03D, 0.0D);
            line(level, shoulder, hand, bone, dense ? 0.10D : 0.15D);
            line(level, hip, foot, bone, dense ? 0.10D : 0.15D);
        }
        jaggedLine(level, pelvis, skull, cracks, dense ? 10 : 7, 0.035D);
    }

    private static void boneOrbitStream(ServerLevel level, CultivatorCorpseEntity corpse,
                                        Vec3 destination, int index, int total) {
        Vec3 center = corpse.position().add(0.0D, corpse.getBbHeight() * 0.48D, 0.0D);
        for (int bone = 0; bone < 8; bone++) {
            double angle = Math.PI * 2.0D * bone / 8.0D + index * 0.67D;
            Vec3 start = center.add(Math.cos(angle) * (0.35D + bone * 0.025D),
                    (bone % 4 - 1.5D) * 0.18D, Math.sin(angle) * (0.35D + bone * 0.025D));
            Vec3 orbit = destination.add(Math.cos(angle + total * 0.21D) * (1.3D + index * 0.10D),
                    0.30D + (bone % 3) * 0.28D,
                    Math.sin(angle + total * 0.21D) * (1.3D + index * 0.10D));
            curvedDoubleStream(level, start, orbit, BONE_WHITE, BONE_GOLD,
                    12, 0.10D + bone * 0.01D, 0.55D);
            curvedDoubleStream(level, orbit, destination, BONE_GOLD, CORPSE_BLACK,
                    8, 0.07D, 0.28D);
        }
    }

    private static void corpseCollapse(ServerLevel level, CultivatorCorpseEntity corpse, int index) {
        Vec3 center = corpse.position().add(0.0D, corpse.getBbHeight() * 0.46D, 0.0D);
        ParticleOptions remains = index % 3 == 0 ? ModParticles.BLACK_FLAME_ASH.get()
                : index % 3 == 1 ? BONE_WHITE : CORPSE_BLACK;
        level.sendParticles(remains, center.x, center.y, center.z,
                18 + index * 2, 0.34D, corpse.getBbHeight() * 0.34D, 0.34D, 0.018D);
        if (index % 3 == 1) {
            drawInternalSkeleton(level, corpse, BONE_WHITE, CORPSE_GRAY, false);
        } else if (index % 3 == 2) {
            irregularGroundFront(level, surfacePoint(level, corpse.position()), CORPSE_BLACK,
                    0.82D, 16, 0.01D, 0.08D);
        }
    }

    private static void oppositeBodyRings(ServerLevel level, LivingEntity entity,
                                          ParticleOptions particle) {
        Basis body = bodyBasis(entity);
        Vec3 floor = surfacePoint(level, entity.position()).add(0.0D, 0.04D, 0.0D);
        Vec3 chest = entity.position().add(0.0D, entity.getBbHeight() * 0.62D, 0.0D);
        double phase = level.getGameTime() * 0.16D;
        orientedRingPhase(level, floor, body.right, body.forward, particle,
                Math.max(0.48D, entity.getBbWidth() * 0.72D), 16, phase);
        orientedRingPhase(level, chest, body.right, body.up, particle,
                Math.max(0.34D, entity.getBbWidth() * 0.58D), 14, -phase);
    }

    private static void irregularArc(ServerLevel level, Vec3 center, Basis basis,
                                     ParticleOptions particle, double radius, int count,
                                     double outwardSpeed, double heightNoise) {
        for (int i = 0; i < count; i++) {
            double t = i / (double) Math.max(1, count - 1);
            double angle = -Math.PI * 0.72D + t * Math.PI * 1.44D;
            double uneven = radius * (0.86D + 0.12D * Math.sin(i * 1.73D));
            Vec3 radial = basis.forward.scale(Math.cos(angle))
                    .add(basis.right.scale(Math.sin(angle))).normalize();
            Vec3 point = center.add(radial.scale(uneven))
                    .add(0.0D, Math.sin(i * 2.11D) * heightNoise, 0.0D);
            directed(level, particle, point, radial.scale(outwardSpeed));
        }
    }

    private static void irregularGroundFront(ServerLevel level, Vec3 center,
                                             ParticleOptions particle, double radius,
                                             int count, double radialSpeed, double unevenness) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double localRadius = radius + Math.sin(i * 2.17D + center.x * 0.19D) * unevenness;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 point = surfacePoint(level, center.add(radial.scale(localRadius)))
                    .add(0.0D, 0.035D, 0.0D);
            directed(level, particle, point, radial.scale(radialSpeed));
        }
    }

    private static void dragGroundLine(ServerLevel level, Vec3 start, Vec3 end,
                                       ParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.horizontalDistance();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z).normalize();
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            Vec3 point = surfacePoint(level, start.add(horizontal.scale(distance)))
                    .add(0.0D, 0.025D, 0.0D);
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.006D, 0.006D, 0.006D, 0.0D);
        }
    }

    private static Vec3 surfacePoint(ServerLevel level, Vec3 sample) {
        int x = Mth.floor(sample.x);
        int z = Mth.floor(sample.z);
        int top = Mth.floor(sample.y) + 5;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y >= top - 18; y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && level.getBlockState(cursor.above()).isAir()) {
                return new Vec3(sample.x, y + 1.02D, sample.z);
            }
        }
        return new Vec3(sample.x, sample.y + 0.04D, sample.z);
    }

    private static void surfaceBlockDust(ServerLevel level, Vec3 surface, int count) {
        BlockPos block = BlockPos.containing(surface.x, surface.y - 0.08D, surface.z);
        BlockState state = level.getBlockState(block);
        if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    surface.x, surface.y + 0.04D, surface.z, count,
                    0.10D, 0.025D, 0.10D, 0.012D);
        }
    }

    private static Vec3 closestBodyPoint(LivingEntity entity, Vec3 source) {
        double x = Mth.clamp(source.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX);
        double y = Mth.clamp(source.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY);
        double z = Mth.clamp(source.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ);
        return new Vec3(x, y, z);
    }

    private static Basis bodyBasis(LivingEntity entity) {
        Vec3 forward = entity.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-6D) {
            double yaw = Math.toRadians(entity.getYRot());
            forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        return basis(forward.normalize());
    }

    private static void orientedRingPhase(ServerLevel level, Vec3 center, Vec3 axisA, Vec3 axisB,
                                          ParticleOptions particle, double radius, int count,
                                          double phase) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count + phase;
            Vec3 radial = axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
            level.sendParticles(particle, center.x + radial.x * radius,
                    center.y + radial.y * radius, center.z + radial.z * radius,
                    1, 0.004D, 0.004D, 0.004D, 0.0D);
        }
    }

    private static boolean isMouthCast(String ability) {
        return "shiqi_tuxi".equals(ability) || "shihou".equals(ability)
                || "shiwang_zhenhou".equals(ability);
    }

    private static boolean isBone(String ability) {
        return "shigu_baosheng".equals(ability) || "bumie_gushen".equals(ability)
                || "guyu_fengshen".equals(ability) || "shigu_zaishu".equals(ability)
                || "wangu_guiyi".equals(ability);
    }

    private static boolean isDrought(String ability) {
        return "chidi_qianli".equals(ability) || "fenxue_hanqi".equals(ability)
                || "tianhan_lingyu".equals(ability) || "bahuo_linshi".equals(ability);
    }

    private static ParticleOptions primary(String ability) {
        if ("fudi_shiyu".equals(ability)) {
            return ModParticles.METHOD_QI_WOOD.get();
        }
        if (isBone(ability)) {
            return ModParticles.METHOD_QI_NEUTRAL.get();
        }
        if (isDrought(ability)) {
            return ModParticles.METHOD_QI_FIRE.get();
        }
        return ModParticles.METHOD_QI_DARK.get();
    }

    private static ParticleOptions accent(String ability) {
        if ("fudi_shiyu".equals(ability)) {
            return CORPSE_GREEN;
        }
        if (isBone(ability)) {
            return BONE_WHITE;
        }
        if (isDrought(ability)) {
            return ModParticles.METHOD_QI_BLOOD.get();
        }
        return CORPSE_GREEN;
    }

    private static void mouthGather(ServerLevel level, LivingEntity caster, ParticleOptions primary) {
        Basis basis = basis(caster.getLookAngle());
        Vec3 mouth = caster.getEyePosition().add(basis.forward.scale(0.30D));
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D + caster.tickCount * 0.24D;
            Vec3 start = mouth.add(basis.right.scale(Math.cos(angle) * 0.38D))
                    .add(basis.up.scale(Math.sin(angle) * 0.28D))
                    .subtract(basis.forward.scale(0.22D));
            directed(level, i % 3 == 0 ? CORPSE_GREEN : primary,
                    start, mouth.subtract(start).normalize().scale(0.085D));
        }
    }

    private static void targetReticle(ServerLevel level, LivingEntity target,
                                      ParticleOptions particle) {
        Vec3 center = target.position().add(0.0D, 0.08D, 0.0D);
        groundRing(level, center, particle,
                Math.max(0.65D, target.getBbWidth() * 0.8D), 12, -0.025D);
    }

    private static void inwardDisc(ServerLevel level, Vec3 center, ParticleOptions particle,
                                   double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 start = center.add(Math.cos(angle) * radius, 0.0D,
                    Math.sin(angle) * radius);
            directed(level, particle, start,
                    center.subtract(start).normalize().scale(0.10D));
        }
    }

    private static void groundRing(ServerLevel level, Vec3 center, ParticleOptions particle,
                                   double radius, int count, double radialSpeed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            directed(level, particle, center.add(radial.scale(radius)),
                    radial.scale(radialSpeed));
        }
    }

    private static void orientedRing(ServerLevel level, Vec3 center, Vec3 axisA, Vec3 axisB,
                                     ParticleOptions particle, double radius, int count,
                                     double outwardSpeed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 radial = axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
            directed(level, particle, center.add(radial.scale(radius)),
                    radial.scale(outwardSpeed));
        }
    }

    private static void radialBurst(ServerLevel level, Vec3 center, ParticleOptions particle,
                                    int count, double horizontalSpeed, double verticalSpeed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double y = ((i % 5) - 2) * verticalSpeed * 0.35D;
            Vec3 velocity = new Vec3(Math.cos(angle) * horizontalSpeed, y + verticalSpeed,
                    Math.sin(angle) * horizontalSpeed);
            directed(level, particle, center, velocity);
        }
    }

    private static void orbit(ServerLevel level, Vec3 center, ParticleOptions particle,
                              double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count + level.getGameTime() * 0.08D;
            double y = Math.sin(angle * 2.0D) * radius * 0.28D;
            Vec3 point = center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.03D, Math.cos(angle)).scale(0.045D);
            directed(level, particle, point, tangent);
        }
    }

    private static void spiral(ServerLevel level, Vec3 base, double height, double radius,
                               ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double t = i / (double) Math.max(1, count - 1);
            double angle = t * Math.PI * 5.0D + level.getGameTime() * 0.12D;
            Vec3 point = base.add(Math.cos(angle) * radius, t * height,
                    Math.sin(angle) * radius);
            Vec3 tangent = new Vec3(-Math.sin(angle) * 0.035D, 0.035D,
                    Math.cos(angle) * 0.035D);
            directed(level, particle, point, tangent);
        }
    }

    private static void curvedDoubleStream(ServerLevel level, Vec3 start, Vec3 end,
                                           ParticleOptions first, ParticleOptions second,
                                           int steps, double twist, double arcHeight) {
        Vec3 delta = end.subtract(start);
        Vec3 direction = delta.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D)
                : delta.normalize();
        Vec3 side = direction.cross(UP);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 other = side.cross(direction).normalize();
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double angle = t * Math.PI * 5.0D;
            Vec3 center = start.add(delta.scale(t)).add(0.0D,
                    Math.sin(t * Math.PI) * arcHeight, 0.0D);
            Vec3 offset = side.scale(Math.cos(angle) * twist)
                    .add(other.scale(Math.sin(angle) * twist));
            directed(level, first, center.add(offset), direction.scale(0.035D));
            directed(level, second, center.subtract(offset), direction.scale(0.035D));
        }
    }

    private static void jaggedLine(ServerLevel level, Vec3 start, Vec3 end,
                                   ParticleOptions particle, int segments, double jitter) {
        Vec3 delta = end.subtract(start);
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 point = start.add(delta.scale(t));
            if (i > 0 && i < segments) {
                point = point.add((level.random.nextDouble() - 0.5D) * jitter,
                        (level.random.nextDouble() - 0.5D) * jitter,
                        (level.random.nextDouble() - 0.5D) * jitter);
            }
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.008D, 0.008D, 0.008D, 0.0D);
        }
    }

    private static void line(ServerLevel level, Vec3 start, Vec3 end,
                             ParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) {
            return;
        }
        Vec3 direction = delta.normalize();
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.006D, 0.006D, 0.006D, 0.0D);
        }
    }

    private static void sphere(ServerLevel level, Vec3 center, double radius,
                               ParticleOptions particle, int count, boolean outward) {
        for (int i = 0; i < count; i++) {
            Vec3 direction = fibonacciDirection(i, count);
            Vec3 point = center.add(direction.scale(radius));
            if (outward) {
                directed(level, particle, point, direction.scale(0.055D));
            } else {
                level.sendParticles(particle, point.x, point.y, point.z,
                        1, 0.006D, 0.006D, 0.006D, 0.0D);
            }
        }
    }

    private static Vec3 fibonacciDirection(int index, int count) {
        double y = 1.0D - 2.0D * (index + 0.5D) / count;
        double circle = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double angle = Math.PI * (3.0D - Math.sqrt(5.0D)) * index;
        return new Vec3(Math.cos(angle) * circle, y, Math.sin(angle) * circle);
    }

    private static void directed(ServerLevel level, ParticleOptions particle,
                                 Vec3 position, Vec3 velocity) {
        level.sendParticles(particle, position.x, position.y, position.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
    }

    private static Basis basis(Vec3 rawForward) {
        Vec3 forward = rawForward.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : rawForward.normalize();
        Vec3 right = forward.cross(UP);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        return new Basis(forward, right, up);
    }

    private record Basis(Vec3 forward, Vec3 right, Vec3 up) {
    }
}
