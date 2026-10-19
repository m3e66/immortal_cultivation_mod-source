package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JindanTribulation {
    private static final int OVERLAY_START = 0;
    private static final int OVERLAY_STRIKE_FLASH = 1;
    private static final int OVERLAY_FINAL_FLASH = 2;
    private static final int OVERLAY_GOLD_PULSE = 3;
    private static final int OVERLAY_CALM = 4;
    private static final int OVERLAY_END = 5;
    private static final int OVERLAY_PURPLE_FLASH = 6;
    private static final int TOTAL_TICKS = 1390;
    private static final int FINAL_STRIKE_TICK = 1160;
    private static final int SILENCE_TICK = 1200;
    private static final int GATHER_START_TICK = 1220;
    private static final int AURA_TICK = 1300;
    private static final int[] WARNING_TICKS = {55, 80, 105};
    private static final int[] STRIKE_TICKS = {
            125, 155, 185, 225,
            345, 380, 465, 500,
            615, 650, 745, 810,
            845, 940, 1040, FINAL_STRIKE_TICK
    };
    private static final float[] STRIKE_FIFTEEN_DAMAGE = {
            2.0F, 4.0F, 8.0F, 16.0F, 32.0F, 64.0F, 128.0F, 256.0F, 512.0F
    };
    private static final int[] PULSE_TICKS = {1240, 1262, 1282, AURA_TICK};
    private static final float[] PULSE_STRENGTH = {0.22F, 0.68F, 0.28F, 1.0F};

    private static final DustParticleOptions PALE_LIGHTNING = new DustParticleOptions(
            new Vector3f(0.72F, 0.90F, 1.0F), 1.25F);
    private static final DustParticleOptions GOLD_LIGHTNING = new DustParticleOptions(
            new Vector3f(1.0F, 0.72F, 0.12F), 1.45F);
    private static final DustParticleOptions PURPLE_LIGHTNING = new DustParticleOptions(
            new Vector3f(0.68F, 0.20F, 1.0F), 1.45F);
    private static final DustParticleOptions GOLD_QI = new DustParticleOptions(
            new Vector3f(1.0F, 0.82F, 0.28F), 1.15F);

    private static final Map<UUID, Sequence> ACTIVE = new HashMap<>();

    private JindanTribulation() {
    }

    public static void start(ServerPlayer player, String quality) {
        ACTIVE.put(player.getUUID(), new Sequence(quality));
        sendOverlay(player, OVERLAY_START, TOTAL_TICKS, 1.0F);
        player.sendSystemMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".jindan_tribulation_begin"));
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.WEATHER, 0.9F, 0.55F);
        }
    }

    public static void tick(ServerPlayer player) {
        Sequence sequence = ACTIVE.get(player.getUUID());
        if (sequence == null) {
            return;
        }
        if (!player.isAlive() || player.isRemoved()) {
            if (!player.isAlive()) failOnDeath(player);
            else clear(player);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            clear(player);
            return;
        }

        sequence.tick++;
        int tick = sequence.tick;

        if (tick < SILENCE_TICK) {
            spawnStormClouds(level, player, tick, false);
        } else if (tick >= AURA_TICK && tick < TOTAL_TICKS) {
            spawnStormClouds(level, player, tick, true);
        }

        if (tick < SILENCE_TICK && tick % 18 == 0) {
            flashInsideClouds(level, player, tick);
        }

        for (int warningTick : WARNING_TICKS) {
            if (tick == warningTick) {
                warningStrike(level, player, tick);
                sendOverlay(player, OVERLAY_STRIKE_FLASH, 5, 0.14F);
            }
        }

        tickStrikeSequence(level, player, tick);
        if (!player.isAlive()) {
            return;
        }

        if (tick > FINAL_STRIKE_TICK && tick <= FINAL_STRIKE_TICK + 28 && tick % 2 == 0) {
            spawnShockwave(level, player, tick - FINAL_STRIKE_TICK);
        }

        if (tick >= GATHER_START_TICK && tick <= AURA_TICK) {
            gatherGoldenQi(level, player, tick);
        }

        for (int pulse = 0; pulse < PULSE_TICKS.length; pulse++) {
            if (tick == PULSE_TICKS[pulse]) {
                pulseGoldenCore(level, player, pulse);
            }
        }

        if (tick == AURA_TICK) {
            sendOverlay(player, OVERLAY_CALM, 0, 1.0F);
            releaseGoldenAura(level, player);
        }
        if (tick > AURA_TICK && tick < TOTAL_TICKS && tick % 2 == 0) {
            goldenRain(level, player, tick);
        }

        if (tick >= TOTAL_TICKS) {
            finish(player, sequence.quality);
        }
    }

    public static void clear(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null) {
            sendOverlay(player, OVERLAY_END, 0, 0.0F);
        }
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** Failure changes the original attachment before NeoForge copies it on respawn. */
    public static boolean failOnDeath(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) == null) return false;
        var data = ModAttachments.getData(player);
        int ageLimit = CultivationLevels.getLevelDef(CultivationLevels.REALM_HALF_JINDAN).maxAge();
        // Leave at least one year after the normal ten-year death penalty; failure must not become a mortal reset.
        ModAttachments.setData(player, data.withCultivationLevel(CultivationLevels.REALM_HALF_JINDAN)
                .withCultivationProgress(0).withQi(0).withMeditating(false)
                .withAgePenalty(Math.min(data.agePenalty(), Math.max(0, ageLimit - 11))));
        player.getPersistentData().remove("ImmortalCultivationPendingJindanBreakthrough");
        ServerEvents.setJindanQuality(player, "liedan");
        sendOverlay(player, OVERLAY_END, 0, 0);
        player.sendSystemMessage(Component.translatable("message.immortal_cultivation_mod.jindan_trial_death",
                CultivationLevels.getTotalQiNeeded(CultivationLevels.REALM_HALF_JINDAN)));
        return true;
    }

    private static void tickStrikeSequence(ServerLevel level, ServerPlayer player, int tick) {
        if (tick == STRIKE_TICKS[0]) {
            strikePlayer(level, player, 50.0F, 0, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.30F);
        }
        if (tick == STRIKE_TICKS[1]) {
            spawnConvergingBolts(level, player, 2, 7.0D, PALE_LIGHTNING);
            strikePlayer(level, player, 100.0F, 1, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.42F);
        }

        int elapsed = tick - STRIKE_TICKS[2];
        if (elapsed >= 0 && elapsed <= 10 && elapsed % 5 == 0) {
            strikePlayer(level, player, 50.0F, 1 + elapsed / 5, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.38F + elapsed * 0.012F);
        }

        elapsed = tick - STRIKE_TICKS[3];
        if (elapsed == 0) {
            strikePlayer(level, player, 100.0F, 4, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.65F);
        }
        if (elapsed >= 0 && elapsed <= 24 && (elapsed & 1) == 0) {
            spawnElectricShockwave(level, player, elapsed, PALE_LIGHTNING);
        }
        if (elapsed > 0 && elapsed <= 100 && elapsed % 20 == 0) {
            arcDamageTick(level, player, 30.0F, PALE_LIGHTNING, 0.20F);
        }

        if (tick == STRIKE_TICKS[4] - 12) {
            spawnGroundStrikeRing(level, player, 8, 7.0D, PALE_LIGHTNING);
        }
        if (tick == STRIKE_TICKS[4]) {
            strikePlayer(level, player, 300.0F, 5, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.76F);
        }

        elapsed = tick - STRIKE_TICKS[5];
        if (elapsed >= 0 && elapsed <= 50 && elapsed % 10 == 0) {
            strikePlayer(level, player, 30.0F, 1, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.28F);
        }

        if (tick == STRIKE_TICKS[6]) {
            spawnConvergingBolts(level, player, 7, 12.0D, PALE_LIGHTNING);
            strikePlayer(level, player, 400.0F, 7, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.90F);
        }

        elapsed = tick - STRIKE_TICKS[7];
        if (elapsed == 0) {
            strikePlayer(level, player, 50.0F, 5, PURPLE_LIGHTNING,
                    OVERLAY_PURPLE_FLASH, 0.82F);
        } else if (elapsed > 0 && elapsed <= 80 && elapsed % 20 == 0) {
            arcDamageTick(level, player, 50.0F, PURPLE_LIGHTNING, 0.48F);
        }
        if (elapsed >= 0 && elapsed <= 100 && elapsed % 4 == 0) {
            spawnUnstableArcs(level, player, elapsed);
        }

        if (tick == STRIKE_TICKS[8] - 8) {
            spawnCardinalBolts(level, player, 6.0D, PALE_LIGHTNING);
        }
        if (tick == STRIKE_TICKS[8]) {
            strikePlayer(level, player, 400.0F, 6, PALE_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.92F);
        }

        elapsed = tick - STRIKE_TICKS[9];
        if (elapsed >= 0 && elapsed <= 60) {
            if ((elapsed & 1) == 0) {
                spawnContinuousPillarFrame(level, player, elapsed, PALE_LIGHTNING);
            }
            if (elapsed % 60 == 0) {
                damagePlayer(level, player, 100.0F);
                sendOverlay(player, OVERLAY_STRIKE_FLASH, 8, 0.62F);
            }
        }

        elapsed = tick - STRIKE_TICKS[10];
        if (elapsed >= 0 && elapsed <= 30 && elapsed % 6 == 0) {
            int hit = elapsed / 6;
            strikeFromCloudAngle(level, player, 80.0F, hit, 6,
                    GOLD_LIGHTNING, 0.58F + hit * 0.055F);
        }

        if (tick == STRIKE_TICKS[11]) {
            spawnConvergingBolts(level, player, 12, 17.0D, GOLD_LIGHTNING);
            strikePlayer(level, player, 300.0F, 7, GOLD_LIGHTNING,
                    OVERLAY_STRIKE_FLASH, 0.94F);
        }

        elapsed = tick - STRIKE_TICKS[12];
        if (elapsed >= 0 && elapsed <= 56 && elapsed % 8 == 0) {
            strikeFromCloudAngle(level, player, 50.0F, elapsed / 8, 8,
                    PALE_LIGHTNING, 0.46F);
        }

        elapsed = tick - STRIKE_TICKS[13];
        if (elapsed >= 0 && elapsed <= 60) {
            if (elapsed % 4 == 0) {
                spawnLightningRainFrame(level, player, elapsed);
            }
            if (elapsed % 60 == 0) {
                strikePlayer(level, player, 50.0F, 2, PALE_LIGHTNING,
                        OVERLAY_STRIKE_FLASH, 0.42F);
            }
        }

        elapsed = tick - STRIKE_TICKS[14];
        if (elapsed >= 0 && elapsed <= 64 && elapsed % 8 == 0) {
            int hit = elapsed / 8;
            strikePlayer(level, player, STRIKE_FIFTEEN_DAMAGE[hit], hit,
                    hit >= 5 ? GOLD_LIGHTNING : PALE_LIGHTNING,
                    hit == 8 ? OVERLAY_FINAL_FLASH : OVERLAY_STRIKE_FLASH,
                    0.22F + hit * 0.09F);
        }

        elapsed = tick - STRIKE_TICKS[15];
        if (elapsed == 0) {
            finalJudgment(level, player);
        }
        if (elapsed >= 0 && elapsed < 40 && (elapsed & 1) == 0) {
            spawnFinalSecondaryBolts(level, player, elapsed);
        }
    }

    private static void warningStrike(ServerLevel level, ServerPlayer player, int tick) {
        double angle = tick * 1.61803398875D;
        double radius = 5.0D + level.random.nextDouble() * 5.0D;
        Vec3 pos = player.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
        summonVisualBolt(level, pos.x, pos.y, pos.z);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.2D, pos.z,
                42, 0.65D, 0.35D, 0.65D, 0.18D);
        level.sendParticles(PALE_LIGHTNING, pos.x, pos.y + 0.15D, pos.z,
                28, 0.8D, 0.20D, 0.8D, 0.06D);
    }

    private static void summonVisualBolt(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private static void strikePlayer(ServerLevel level, ServerPlayer player, float damage,
                                     int power, DustParticleOptions accent,
                                     int overlayEvent, float flashStrength) {
        Vec3 impact = player.position();
        sendOverlay(player, overlayEvent, 7 + Math.min(13, power * 2), flashStrength);
        summonVisualBolt(level, impact.x, impact.y, impact.z);
        spawnLightningPillar(level, impact, power, accent);
        spawnImpactBurst(level, impact, power, accent);
        playThunder(level, player, power);
        damagePlayer(level, player, damage);
    }

    private static void damagePlayer(ServerLevel level, ServerPlayer player, float damage) {
        player.invulnerableTime = 0;
        player.hurt(level.damageSources().source(DamageTypes.LIGHTNING_BOLT), damage);
    }

    private static void playThunder(ServerLevel level, ServerPlayer player, int power) {
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, Math.min(10.0F, 2.8F + power * 0.75F),
                Math.max(0.42F, 0.96F - power * 0.045F));
    }

    private static void arcDamageTick(ServerLevel level, ServerPlayer player, float damage,
                                      DustParticleOptions accent, float flashStrength) {
        Vec3 core = player.position().add(0.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, core.x, core.y, core.z,
                70, 1.35D, 1.15D, 1.35D, 0.20D);
        level.sendParticles(accent, core.x, core.y, core.z,
                55, 1.45D, 1.25D, 1.45D, 0.08D);
        sendOverlay(player, accent == PURPLE_LIGHTNING
                        ? OVERLAY_PURPLE_FLASH : OVERLAY_STRIKE_FLASH,
                5, flashStrength);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.WEATHER, 1.8F, accent == PURPLE_LIGHTNING ? 0.62F : 0.88F);
        damagePlayer(level, player, damage);
    }

    private static void spawnConvergingBolts(ServerLevel level, ServerPlayer player,
                                             int count, double radius,
                                             DustParticleOptions accent) {
        Vec3 impact = player.position().add(0.0D, 0.35D, 0.0D);
        Vec3 convergence = impact.add(0.0D, 7.5D, 0.0D);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count + level.random.nextDouble() * 0.18D;
            Vec3 start = impact.add(Math.cos(angle) * radius,
                    17.0D + level.random.nextDouble() * 5.0D,
                    Math.sin(angle) * radius);
            spawnBranch(level, start, convergence, accent, 14);
        }
        spawnBranch(level, convergence, impact, accent, 16);
    }

    private static void spawnGroundStrikeRing(ServerLevel level, ServerPlayer player,
                                              int count, double radius,
                                              DustParticleOptions accent) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            summonVisualBolt(level, x, player.getY(), z);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, player.getY() + 0.25D, z,
                    42, 0.75D, 0.28D, 0.75D, 0.17D);
            level.sendParticles(accent, x, player.getY() + 0.2D, z,
                    30, 0.85D, 0.2D, 0.85D, 0.06D);
        }
    }

    private static void spawnCardinalBolts(ServerLevel level, ServerPlayer player,
                                           double radius, DustParticleOptions accent) {
        spawnGroundStrikeRing(level, player, 4, radius, accent);
        Vec3 center = player.position().add(0.0D, 8.0D, 0.0D);
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI * 0.5D;
            Vec3 start = player.position().add(Math.cos(angle) * radius,
                    18.0D, Math.sin(angle) * radius);
            spawnBranch(level, start, center, accent, 16);
        }
    }

    private static void spawnElectricShockwave(ServerLevel level, ServerPlayer player,
                                               int age, DustParticleOptions accent) {
        double radius = 0.8D + age * 0.42D;
        int points = 28;
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double x = player.getX() + dx * radius;
            double z = player.getZ() + dz * radius;
            level.sendParticles(accent, x, player.getY() + 0.2D, z,
                    0, dx * 0.24D, 0.015D, dz * 0.24D, 1.0D);
            if ((point & 1) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        x, player.getY() + 0.22D, z,
                        0, dx * 0.31D, 0.02D, dz * 0.31D, 1.0D);
            }
        }
    }

    private static void spawnUnstableArcs(ServerLevel level, ServerPlayer player, int age) {
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        for (int arc = 0; arc < 4; arc++) {
            double angle = age * 0.11D + arc * Math.PI * 0.5D;
            Vec3 start = center.add(Math.cos(angle) * 0.65D,
                    Math.sin(angle * 2.0D) * 0.8D,
                    Math.sin(angle) * 0.65D);
            Vec3 end = center.add(Math.cos(angle + 1.15D) * 3.2D,
                    level.random.nextDouble() * 2.2D - 0.5D,
                    Math.sin(angle + 1.15D) * 3.2D);
            spawnBranch(level, start, end, PURPLE_LIGHTNING, 8);
        }
    }

    private static void spawnContinuousPillarFrame(ServerLevel level, ServerPlayer player,
                                                   int elapsed,
                                                   DustParticleOptions accent) {
        double x = player.getX();
        double z = player.getZ();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                x, player.getY() + 9.0D, z,
                70, 0.42D, 8.7D, 0.42D, 0.14D);
        level.sendParticles(accent, x, player.getY() + 9.0D, z,
                60, 0.5D, 8.8D, 0.5D, 0.055D);
        if (elapsed % 20 == 0) {
            summonVisualBolt(level, x, player.getY(), z);
            playThunder(level, player, 5);
        }
    }

    private static void strikeFromCloudAngle(ServerLevel level, ServerPlayer player,
                                             float damage, int index, int total,
                                             DustParticleOptions accent,
                                             float flashStrength) {
        Vec3 impact = player.position().add(0.0D, 0.4D, 0.0D);
        double angle = Math.PI * 2.0D * index / total + index * 0.31D;
        Vec3 start = impact.add(Math.cos(angle) * 13.0D,
                15.0D + (index % 3) * 2.0D,
                Math.sin(angle) * 13.0D);
        spawnBranch(level, start, impact, accent, 24);
        spawnImpactBurst(level, impact, 3 + index / 2, accent);
        sendOverlay(player, accent == PURPLE_LIGHTNING
                        ? OVERLAY_PURPLE_FLASH : OVERLAY_STRIKE_FLASH,
                7, flashStrength);
        playThunder(level, player, 3 + index / 2);
        damagePlayer(level, player, damage);
    }

    private static void spawnLightningRainFrame(ServerLevel level, ServerPlayer player,
                                                int elapsed) {
        double ringY = player.getY() + 15.0D;
        int ringPoints = 18;
        for (int point = 0; point < ringPoints; point++) {
            double angle = Math.PI * 2.0D * point / ringPoints + elapsed * 0.035D;
            double x = player.getX() + Math.cos(angle) * 11.0D;
            double z = player.getZ() + Math.sin(angle) * 11.0D;
            level.sendParticles(point % 3 == 0 ? GOLD_LIGHTNING : PALE_LIGHTNING,
                    x, ringY, z, 1, 0.12D, 0.08D, 0.12D, 0.015D);
        }
        for (int bolt = 0; bolt < 3; bolt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 4.0D + level.random.nextDouble() * 17.0D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            summonVisualBolt(level, x, player.getY(), z);
        }
    }

    private static void finalJudgment(ServerLevel level, ServerPlayer player) {
        Vec3 impact = player.position();
        sendOverlay(player, OVERLAY_FINAL_FLASH, 30, 1.0F);
        for (int bolt = 0; bolt < 9; bolt++) {
            double angle = Math.PI * 2.0D * bolt / 9.0D;
            double radius = bolt == 0 ? 0.0D : 0.95D;
            summonVisualBolt(level, impact.x + Math.cos(angle) * radius,
                    impact.y, impact.z + Math.sin(angle) * radius);
        }
        spawnConvergingBolts(level, player, 24, 27.0D, GOLD_LIGHTNING);
        spawnLightningPillar(level, impact, 10, GOLD_LIGHTNING);
        spawnImpactBurst(level, impact, 10, GOLD_LIGHTNING);
        level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 1.0D, impact.z,
                8, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                impact.x, impact.y + 8.0D, impact.z,
                520, 3.0D, 9.0D, 3.0D, 0.24D);
        level.sendParticles(GOLD_LIGHTNING,
                impact.x, impact.y + 8.0D, impact.z,
                460, 2.8D, 9.0D, 2.8D, 0.11D);
        playThunder(level, player, 12);
        damagePlayer(level, player, 600.0F);
    }

    private static void spawnFinalSecondaryBolts(ServerLevel level, ServerPlayer player,
                                                 int elapsed) {
        for (int bolt = 0; bolt < 10; bolt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 5.0D + level.random.nextDouble() * 27.0D;
            summonVisualBolt(level,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY(),
                    player.getZ() + Math.sin(angle) * radius);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 9.0D, player.getZ(),
                120, 23.0D, 7.5D, 23.0D, 0.19D);
        level.sendParticles(GOLD_LIGHTNING,
                player.getX(), player.getY() + 9.0D, player.getZ(),
                90, 22.0D, 7.0D, 22.0D, 0.08D);
        spawnContinuousPillarFrame(level, player, elapsed, GOLD_LIGHTNING);
    }

    private static void spawnLightningPillar(ServerLevel level, Vec3 impact, int power,
                                             DustParticleOptions accent) {
        int strands = Math.min(6, 1 + power);
        int segments = 24 + Math.min(10, power) * 3;
        double height = 18.0D + Math.min(10, power) * 1.4D;
        for (int strand = 0; strand < strands; strand++) {
            double strandAngle = Math.PI * 2.0D * strand / strands;
            double strandRadius = strand == 0 ? 0.0D : 0.16D + power * 0.07D;
            double previousX = impact.x + Math.cos(strandAngle) * strandRadius;
            double previousZ = impact.z + Math.sin(strandAngle) * strandRadius;
            for (int segment = 0; segment <= segments; segment++) {
                double progress = segment / (double) segments;
                double taper = Math.sin(progress * Math.PI);
                double jitter = (0.12D + power * 0.035D) * taper;
                double x = impact.x + Math.cos(strandAngle) * strandRadius * (1.0D - progress)
                        + (level.random.nextDouble() - 0.5D) * jitter;
                double y = impact.y + 0.25D + height * (1.0D - progress);
                double z = impact.z + Math.sin(strandAngle) * strandRadius * (1.0D - progress)
                        + (level.random.nextDouble() - 0.5D) * jitter;
                double midX = (previousX + x) * 0.5D;
                double midZ = (previousZ + z) * 0.5D;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, midX, y, midZ,
                        power >= 4 ? 4 : 2, 0.08D, 0.12D, 0.08D,
                        0.10D + power * 0.025D);
                if ((segment & 1) == 0) {
                    level.sendParticles(accent, midX, y, midZ,
                            power >= 4 ? 3 : 1, 0.07D, 0.10D, 0.07D, 0.03D);
                }
                previousX = x;
                previousZ = z;
            }
        }

        int branches = Math.max(1, Math.min(20, power * 2));
        for (int branch = 0; branch < branches; branch++) {
            double angle = Math.PI * 2.0D * branch / branches + level.random.nextDouble() * 0.8D;
            double startY = impact.y + 5.0D + level.random.nextDouble() * (8.0D + power);
            Vec3 start = new Vec3(impact.x, startY, impact.z);
            double length = 2.5D + power * 0.8D + level.random.nextDouble() * 2.5D;
            Vec3 end = start.add(Math.cos(angle) * length,
                    -1.0D - level.random.nextDouble() * 3.0D,
                    Math.sin(angle) * length);
            spawnBranch(level, start, end, accent, 8 + power);
        }
    }

    private static void spawnBranch(ServerLevel level, Vec3 start, Vec3 end,
                                    DustParticleOptions accent, int segments) {
        for (int segment = 0; segment <= segments; segment++) {
            double progress = segment / (double) segments;
            Vec3 point = start.lerp(end, progress).add(
                    (level.random.nextDouble() - 0.5D) * 0.24D,
                    (level.random.nextDouble() - 0.5D) * 0.18D,
                    (level.random.nextDouble() - 0.5D) * 0.24D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
                    2, 0.04D, 0.04D, 0.04D, 0.10D);
            if ((segment & 1) == 0) {
                level.sendParticles(accent, point.x, point.y, point.z,
                        1, 0.02D, 0.02D, 0.02D, 0.01D);
            }
        }
    }

    private static void spawnImpactBurst(ServerLevel level, Vec3 impact, int power,
                                         DustParticleOptions accent) {
        int multiplier = power + 1;
        level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y + 0.35D, impact.z,
                power >= 4 ? 4 : 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y + 0.35D, impact.z,
                35 + multiplier * 22, 0.75D + power * 0.22D, 0.50D,
                0.75D + power * 0.22D, 0.18D + power * 0.02D);
        level.sendParticles(ParticleTypes.FIREWORK, impact.x, impact.y + 0.45D, impact.z,
                18 + multiplier * 12, 0.6D + power * 0.17D, 0.35D,
                0.6D + power * 0.17D, 0.12D);
        level.sendParticles(accent,
                impact.x, impact.y + 0.25D, impact.z,
                40 + multiplier * 20, 0.9D + power * 0.25D, 0.28D,
                0.9D + power * 0.25D, 0.08D);
    }

    private static void spawnStormClouds(ServerLevel level, ServerPlayer player, int tick,
                                         boolean separating) {
        int interval = separating ? 3 : 4;
        if (tick % interval != 0) {
            return;
        }
        double expansion = separating ? (tick - AURA_TICK) * 0.11D : 0.0D;
        for (int layer = 0; layer < 6; layer++) {
            double baseRadius = 4.5D + layer * 3.2D + expansion;
            for (int point = 0; point < 6; point++) {
                double angle = tick * (0.015D + layer * 0.0028D)
                        + point * Math.PI / 3.0D + layer * 0.47D;
                double radius = baseRadius + Math.sin(tick * 0.04D + point) * 1.35D;
                double x = player.getX() + Math.cos(angle) * radius;
                double y = player.getY() + 12.0D + layer * 1.25D
                        + Math.sin(angle * 2.0D) * 0.8D;
                double z = player.getZ() + Math.sin(angle) * radius;
                level.sendParticles(layer >= 1
                                ? ParticleTypes.LARGE_SMOKE
                                : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        x, y, z, separating ? 2 : 8,
                        1.85D, 0.78D, 1.85D, separating ? 0.04D : 0.009D);
            }
        }

        // Fill the entire ceiling so the storm reads as a heavy dark cloud deck, not a ring.
        if (!separating) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 15.0D, player.getZ(),
                    110, 8.5D, 1.35D, 8.5D, 0.006D);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 16.5D, player.getZ(),
                    75, 12.5D, 1.8D, 12.5D, 0.005D);
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 14.0D, player.getZ(),
                    60, 14.0D, 2.1D, 14.0D, 0.012D);
        }
    }

    private static void flashInsideClouds(ServerLevel level, ServerPlayer player, int tick) {
        double angle = tick * 0.31D;
        double radius = 4.0D + level.random.nextDouble() * 7.0D;
        double x = player.getX() + Math.cos(angle) * radius;
        double y = player.getY() + 13.0D + level.random.nextDouble() * 5.0D;
        double z = player.getZ() + Math.sin(angle) * radius;
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        DustParticleOptions accent = tick >= STRIKE_TICKS[10] - 20
                ? GOLD_LIGHTNING
                : tick >= STRIKE_TICKS[7] && tick < STRIKE_TICKS[8]
                        ? PURPLE_LIGHTNING
                        : PALE_LIGHTNING;
        level.sendParticles(accent,
                x, y, z, 34, 1.8D, 0.7D, 1.8D, 0.06D);
    }

    private static void spawnShockwave(ServerLevel level, ServerPlayer player, int age) {
        double radius = age * 0.72D;
        int points = 18;
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points + age * 0.025D;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double x = player.getX() + dx * radius;
            double z = player.getZ() + dz * radius;
            level.sendParticles(GOLD_QI, x, player.getY() + 0.25D, z,
                    0, dx * 0.36D, 0.045D, dz * 0.36D, 1.0D);
            if ((point & 1) == 0) {
                level.sendParticles(ParticleTypes.POOF, x, player.getY() + 0.12D, z,
                        1, 0.18D, 0.05D, 0.18D, 0.04D);
            }
        }
    }

    private static void gatherGoldenQi(ServerLevel level, ServerPlayer player, int tick) {
        if ((tick & 1) != 0) {
            return;
        }
        float progress = (tick - GATHER_START_TICK) / (float) (AURA_TICK - GATHER_START_TICK);
        double radius = 8.5D * (1.0D - progress) + 0.35D;
        Vec3 target = player.position().add(0.0D, 1.0D, 0.0D);
        for (int point = 0; point < 10; point++) {
            double angle = tick * 0.19D + point * (Math.PI * 2.0D / 10.0D);
            Vec3 origin = target.add(Math.cos(angle) * radius,
                    Math.sin(angle * 2.0D) * 1.4D,
                    Math.sin(angle) * radius);
            Vec3 velocity = target.subtract(origin).normalize().scale(0.24D + progress * 0.25D);
            level.sendParticles(GOLD_QI, origin.x, origin.y, origin.z,
                    0, velocity.x, velocity.y, velocity.z, 1.0D);
            if ((point & 3) == 0) {
                level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z,
                        0, velocity.x * 0.65D, velocity.y * 0.65D,
                        velocity.z * 0.65D, 1.0D);
            }
        }
    }

    private static void pulseGoldenCore(ServerLevel level, ServerPlayer player, int pulse) {
        float strength = PULSE_STRENGTH[pulse];
        Vec3 core = player.position().add(0.0D, 1.0D, 0.0D);
        int goldCount = switch (pulse) {
            case 0 -> 35;
            case 1 -> 105;
            case 2 -> 48;
            default -> 180;
        };
        int whiteCount = switch (pulse) {
            case 0 -> 14;
            case 1 -> 44;
            case 2 -> 18;
            default -> 72;
        };
        level.sendParticles(GOLD_QI, core.x, core.y, core.z,
                goldCount, 0.15D + strength * 0.42D,
                0.18D + strength * 0.34D, 0.15D + strength * 0.42D, 0.04D);
        level.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z,
                whiteCount, 0.10D + strength * 0.26D,
                0.14D + strength * 0.24D, 0.10D + strength * 0.26D, 0.035D);
        sendOverlay(player, OVERLAY_GOLD_PULSE,
                14 + pulse * 3, strength);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 0.55F + strength * 0.9F, 0.68F + strength * 0.42F);
    }

    private static void releaseGoldenAura(ServerLevel level, ServerPlayer player) {
        Vec3 core = player.position().add(0.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z,
                6, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, core.x, core.y, core.z,
                180, 2.8D, 2.2D, 2.8D, 0.22D);
        level.sendParticles(GOLD_QI, core.x, core.y, core.z,
                260, 3.5D, 2.1D, 3.5D, 0.20D);
        level.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z,
                130, 2.6D, 1.8D, 2.6D, 0.18D);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 2.4F, 0.72F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 2.0F, 0.55F);
    }

    private static void goldenRain(ServerLevel level, ServerPlayer player, int tick) {
        for (int i = 0; i < 7; i++) {
            double angle = tick * 0.11D + i * 2.399963D;
            double radius = 2.0D + level.random.nextDouble() * 11.0D;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 7.0D + level.random.nextDouble() * 9.0D;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(GOLD_QI, x, y, z,
                    0, 0.0D, -0.12D - level.random.nextDouble() * 0.08D, 0.0D, 1.0D);
        }
    }

    private static void finish(ServerPlayer player, String quality) {
        ACTIVE.remove(player.getUUID());
        sendOverlay(player, OVERLAY_END, 0, 0.0F);
        String level = CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
        player.sendSystemMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".breakthrough_success", CultivationLevels.displayName(level)));
        player.sendSystemMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".jindan_quality",
                Component.translatable("jindan_quality." + ImmortalCultivationMod.MODID + "." + quality)));
        ServerEvents.syncPlayerData(player);
    }

    private static void sendOverlay(ServerPlayer player, int event, int duration, float intensity) {
        PacketDistributor.sendToPlayer(player,
                new ModPayloads.ClientboundJindanTribulationPayload(event, duration, intensity));
    }

    private static final class Sequence {
        private final String quality;
        private int tick;

        private Sequence(String quality) {
            this.quality = quality;
        }
    }
}
