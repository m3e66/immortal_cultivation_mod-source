package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.ShixuChiliu;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Client presentation for 时序迟流. Gameplay remains authoritative in {@link ShixuChiliu}. */
public final class TemporalSlowflowClient {
    private static final float RED = 1.0F;
    private static final float GREEN = 0.79F;
    private static final float BLUE = 0.34F;
    private static final float PALE_RED = 1.0F;
    private static final float PALE_GREEN = 0.93F;
    private static final float PALE_BLUE = 0.68F;
    private static final double RADIUS_SQUARED =
            ShixuChiliu.DOMAIN_RADIUS * ShixuChiliu.DOMAIN_RADIUS;
    private static final double VISIBLE_DISTANCE =
            ShixuChiliu.DOMAIN_RADIUS + 64.0D;
    private static final int CASTING_TICKS = 20;
    private static final int EXPANSION_TICKS = 18;
    private static final int MAX_TRAIL_SAMPLES = 7;
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "textures/misc/white.png");
    private static final RenderType DOMAIN_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(WHITE_TEXTURE);
    private static final DustParticleOptions PALE_GOLD_DUST =
            new DustParticleOptions(new Vector3f(
                    PALE_RED, PALE_GREEN, PALE_BLUE), 0.75F);
    private static final DustParticleOptions GOLD_DUST =
            new DustParticleOptions(new Vector3f(
                    RED, GREEN, BLUE), 0.55F);

    private static final Map<UUID, DomainState> DOMAINS = new HashMap<>();
    private static final List<Vec3> ACTIVE_CENTERS = new ArrayList<>();
    private static final Map<Integer, LivingTrail> LIVING_TRAILS = new HashMap<>();
    private static final Map<Integer, ProjectileTrail> PROJECTILE_TRAILS = new HashMap<>();

    private TemporalSlowflowClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TemporalSlowflowClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(TemporalSlowflowClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(TemporalSlowflowClient::onRenderGui);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            return;
        }
        syncDomains(minecraft.level);
        if (minecraft.isPaused()) return;
        updateDraggedEntities(minecraft.level);
        spawnAmbientTimeMotes(minecraft.level);
    }

    private static void clear() {
        DOMAINS.clear();
        ACTIVE_CENTERS.clear();
        LIVING_TRAILS.clear();
        PROJECTILE_TRAILS.clear();
    }

    private static void syncDomains(ClientLevel level) {
        Set<UUID> active = new HashSet<>();
        ACTIVE_CENTERS.clear();
        long now = level.getGameTime();
        for (Player caster : level.players()) {
            if (!caster.hasEffect(ModEffects.SHIXU_CHILIU)) continue;
            active.add(caster.getUUID());
            ACTIVE_CENTERS.add(caster.position());
            DOMAINS.computeIfAbsent(caster.getUUID(),
                    ignored -> new DomainState(now));
        }
        DOMAINS.keySet().removeIf(id -> !active.contains(id));
    }

    private static void updateDraggedEntities(ClientLevel level) {
        long now = level.getGameTime();
        Set<Integer> currentLiving = new HashSet<>();
        Set<Integer> currentProjectiles = new HashSet<>();

        for (Player caster : activeCasters(level)) {
            AABB bounds = caster.getBoundingBox()
                    .inflate(ShixuChiliu.DOMAIN_RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(
                    LivingEntity.class, bounds,
                    target -> target.isAlive()
                            && target.distanceToSqr(caster) <= RADIUS_SQUARED
                            && isAffectedEnemy(caster, target))) {
                currentLiving.add(target.getId());
                LIVING_TRAILS.computeIfAbsent(
                                target.getId(), ignored -> new LivingTrail())
                        .record(target, now);
                if (Math.floorMod((int) now + target.getId(), 9) == 0) {
                    spawnBodyTimeMotes(level, target);
                }
            }

            for (Projectile projectile : level.getEntitiesOfClass(
                    Projectile.class, bounds,
                    projectile -> !projectile.isRemoved()
                            && projectile.distanceToSqr(caster) <= RADIUS_SQUARED
                            && isAffectedProjectile(caster, projectile))) {
                currentProjectiles.add(projectile.getId());
                ProjectileTrail trail = PROJECTILE_TRAILS.computeIfAbsent(
                        projectile.getId(), ignored -> new ProjectileTrail());
                if (!trail.inside) {
                    spawnProjectileEntryRipple(level, projectile);
                }
                trail.record(projectile, now);
                trail.inside = true;
                spawnProjectileGhostTrail(level, projectile, trail);
            }
        }

        Iterator<Map.Entry<Integer, LivingTrail>> livingIterator =
                LIVING_TRAILS.entrySet().iterator();
        while (livingIterator.hasNext()) {
            Map.Entry<Integer, LivingTrail> entry = livingIterator.next();
            if (!currentLiving.contains(entry.getKey())
                    && now - entry.getValue().lastSeen > 8L) {
                livingIterator.remove();
            }
        }

        Iterator<Map.Entry<Integer, ProjectileTrail>> projectileIterator =
                PROJECTILE_TRAILS.entrySet().iterator();
        while (projectileIterator.hasNext()) {
            Map.Entry<Integer, ProjectileTrail> entry = projectileIterator.next();
            ProjectileTrail trail = entry.getValue();
            if (!currentProjectiles.contains(entry.getKey())) {
                trail.inside = false;
            }
            if (now - trail.lastSeen > 8L) {
                projectileIterator.remove();
            }
        }
    }

    private static List<Player> activeCasters(ClientLevel level) {
        List<Player> casters = new ArrayList<>();
        for (Player player : level.players()) {
            if (DOMAINS.containsKey(player.getUUID())
                    && player.hasEffect(ModEffects.SHIXU_CHILIU)) {
                casters.add(player);
            }
        }
        return casters;
    }

    private static boolean isAffectedEnemy(Player caster, LivingEntity target) {
        if (target == caster || caster.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal tamable
                && caster.getUUID().equals(tamable.getOwnerUUID())) {
            return false;
        }
        if (UndeadControl.isOwnedByPlayer(target, caster)) return false;
        if (target instanceof Player player) return !player.isSpectator();
        if (target instanceof Enemy) return true;
        if (target instanceof Mob mob && mob.getTarget() == caster) return true;
        return target.getLastHurtByMob() == caster
                || caster.getLastHurtMob() == target;
    }

    private static boolean isAffectedProjectile(Player caster, Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (owner == caster || owner != null && caster.isAlliedTo(owner)) return false;
        return !(owner instanceof LivingEntity livingOwner)
                || isAffectedEnemy(caster, livingOwner);
    }

    private static void spawnAmbientTimeMotes(ClientLevel level) {
        long now = level.getGameTime();
        RandomSource random = level.random;
        for (Player caster : activeCasters(level)) {
            DomainState state = DOMAINS.get(caster.getUUID());
            if (state == null) continue;
            float age = now - state.startTick;
            int count = age < CASTING_TICKS ? 7 : now % 2L == 0L ? 4 : 2;
            float fieldScale = fieldScale(age);
            double radius = ShixuChiliu.DOMAIN_RADIUS * fieldScale;
            for (int index = 0; index < count; index++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double radial = Math.sqrt(random.nextDouble()) * radius;
                double y = -0.25D + random.nextDouble() * 6.5D;
                double x = caster.getX() + Math.cos(angle) * radial;
                double z = caster.getZ() + Math.sin(angle) * radial;
                double backward = Math.floorMod(index + (int) now, 5) == 0
                        ? -0.008D : 0.002D;
                level.addParticle(index % 3 == 0 ? PALE_GOLD_DUST : GOLD_DUST,
                        x, caster.getY() + y, z,
                        Math.cos(angle) * backward,
                        random.nextDouble() * 0.003D,
                        Math.sin(angle) * backward);
            }

            if (age >= CASTING_TICKS - 4
                    && age <= CASTING_TICKS + EXPANSION_TICKS) {
                float progress = Mth.clamp(
                        (age - CASTING_TICKS + 4.0F)
                                / (EXPANSION_TICKS + 4.0F), 0.0F, 1.0F);
                double pulseRadius = ShixuChiliu.DOMAIN_RADIUS
                        * smoothStep(progress);
                for (int mark = 0; mark < 12; mark++) {
                    double angle = Mth.TWO_PI * mark / 12.0D
                            + now * 0.035D;
                    double x = caster.getX() + Math.cos(angle) * pulseRadius;
                    double z = caster.getZ() + Math.sin(angle) * pulseRadius;
                    level.addParticle(PALE_GOLD_DUST,
                            x, caster.getY() + 0.08D, z,
                            0.0D, 0.001D, 0.0D);
                }
            }
        }
    }

    private static void spawnBodyTimeMotes(
            ClientLevel level, LivingEntity target) {
        double phase = target.getId() * 1.618D
                + level.getGameTime() * 0.11D;
        double radius = Math.max(0.35D, target.getBbWidth() * 0.72D);
        for (int mote = 0; mote < 3; mote++) {
            double angle = phase + mote * Mth.TWO_PI / 3.0D;
            double y = target.getY() + target.getBbHeight()
                    * (mote == 0 ? 0.12D : 0.52D);
            level.addParticle(mote == 0 ? PALE_GOLD_DUST : GOLD_DUST,
                    target.getX() + Math.cos(angle) * radius,
                    y,
                    target.getZ() + Math.sin(angle) * radius,
                    0.0D, 0.001D, 0.0D);
        }
    }

    private static void spawnProjectileEntryRipple(
            ClientLevel level, Projectile projectile) {
        Vec3 direction = projectile.getDeltaMovement();
        if (direction.lengthSqr() < 1.0E-5D) direction = new Vec3(0.0D, 0.0D, 1.0D);
        direction = direction.normalize();
        Vec3 axisA = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (axisA.lengthSqr() < 1.0E-5D) {
            axisA = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        axisA = axisA.normalize();
        Vec3 axisB = direction.cross(axisA).normalize();
        Vec3 center = projectile.position();
        for (int mark = 0; mark < 16; mark++) {
            double angle = Mth.TWO_PI * mark / 16.0D;
            Vec3 offset = axisA.scale(Math.cos(angle) * 0.55D)
                    .add(axisB.scale(Math.sin(angle) * 0.55D));
            Vec3 point = center.add(offset);
            level.addParticle(PALE_GOLD_DUST,
                    point.x, point.y, point.z,
                    offset.x * 0.015D,
                    offset.y * 0.015D,
                    offset.z * 0.015D);
        }
    }

    private static void spawnProjectileGhostTrail(
            ClientLevel level, Projectile projectile, ProjectileTrail trail) {
        if (Math.floorMod(projectile.tickCount + projectile.getId(), 2) != 0) return;
        int index = 0;
        for (Vec3 sample : trail.samples) {
            if (++index > 3) break;
            level.addParticle(index == 1 ? PALE_GOLD_DUST : GOLD_DUST,
                    sample.x, sample.y + projectile.getBbHeight() * 0.5D, sample.z,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ShaderPackCompatibility.isRenderingShadowPass()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || DOMAINS.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        float partialTick = event.getPartialTick()
                .getGameTimeDeltaPartialTick(true);
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(DOMAIN_RENDER_TYPE);

        for (Player caster : activeCasters(minecraft.level)) {
            if (caster.distanceToSqr(minecraft.player)
                    > VISIBLE_DISTANCE * VISIBLE_DISTANCE) continue;
            DomainState state = DOMAINS.get(caster.getUUID());
            if (state == null) continue;
            Vec3 center = interpolatedPosition(caster, partialTick);
            float age = minecraft.level.getGameTime() + partialTick
                    - state.startTick;
            renderDomain(poseStack, vertices, center, cameraPosition, age);
        }

        renderLivingAfterimages(
                minecraft.level, poseStack, vertices,
                camera, cameraPosition, partialTick);
        renderProjectileAfterimages(
                minecraft.level, poseStack, vertices,
                camera, cameraPosition);
        buffers.endBatch(DOMAIN_RENDER_TYPE);
    }

    private static void renderDomain(
            PoseStack poseStack, VertexConsumer vertices,
            Vec3 center, Vec3 cameraPosition, float age) {
        float scale = fieldScale(age);
        float radius = (float) ShixuChiliu.DOMAIN_RADIUS * scale;
        float time = age;

        poseStack.pushPose();
        poseStack.translate(
                center.x - cameraPosition.x,
                center.y + 1.0D - cameraPosition.y,
                center.z - cameraPosition.z);
        float shellFade = Mth.clamp((age - 7.0F) / 14.0F, 0.0F, 1.0F);
        float shimmer = 0.82F + 0.18F
                * Mth.sin(Mth.floor(time / 4.0F) * 0.73F);
        renderSphereShell(poseStack, vertices, radius,
                PALE_RED, PALE_GREEN, PALE_BLUE,
                0.032F * shellFade * shimmer);
        renderSphereShell(poseStack, vertices, radius * 1.012F,
                RED, GREEN, BLUE,
                0.012F * shellFade * shimmer);
        renderTemporalMeridians(poseStack, vertices, radius,
                time, 0.115F * shellFade * shimmer);

        Matrix4f sphereMatrix = poseStack.last().pose();
        for (int echo = 0; echo < 3; echo++) {
            float correction = Mth.frac(Math.max(
                    0.0F, time - echo * 5.0F) / 74.0F);
            float correctionY = -radius + correction * radius * 2.0F;
            float correctionRadius = Mth.sqrt(Math.max(
                    0.0F, radius * radius - correctionY * correctionY));
            if (correctionRadius > 0.3F) {
                renderHorizontalRing(vertices, sphereMatrix,
                        correctionRadius, correctionY,
                        0.060F - echo * 0.012F,
                        irregularAngle(time - echo * 3.0F, 7 + echo), 48,
                        echo == 0 ? RED : PALE_RED,
                        echo == 0 ? GREEN : PALE_GREEN,
                        echo == 0 ? BLUE : PALE_BLUE,
                        (0.11F - echo * 0.028F) * shellFade);
            }
        }
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(
                center.x - cameraPosition.x,
                center.y + 0.055D - cameraPosition.y,
                center.z - cameraPosition.z);
        Matrix4f groundMatrix = poseStack.last().pose();
        float groundAlpha = Mth.clamp(age / 10.0F, 0.0F, 1.0F);
        renderBrokenRing(vertices, groundMatrix,
                radius * 0.30F, 0.055F,
                irregularAngle(time, 0), 48,
                RED, GREEN, BLUE, 0.22F * groundAlpha, 0);
        renderBrokenRing(vertices, groundMatrix,
                radius * 0.55F, 0.060F,
                irregularAngle(time, 1), 56,
                PALE_RED, PALE_GREEN, PALE_BLUE,
                0.17F * groundAlpha, 1);
        renderBrokenRing(vertices, groundMatrix,
                radius * 0.79F, 0.070F,
                irregularAngle(time, 2), 64,
                RED, GREEN, BLUE, 0.14F * groundAlpha, 2);
        renderBrokenRing(vertices, groundMatrix,
                radius, 0.095F,
                irregularAngle(time, 3), 72,
                PALE_RED, PALE_GREEN, PALE_BLUE,
                0.30F * groundAlpha, 3);
        renderClockMarks(vertices, groundMatrix,
                radius, irregularAngle(time, 4), groundAlpha);

        // Offset copies make adjacent moments look slightly out of alignment
        // without covering the terrain with an opaque floor.
        for (int echo = 1; echo <= 3; echo++) {
            poseStack.pushPose();
            float echoAngle = time * 0.013F + echo * 2.21F;
            float echoOffset = 0.045F + echo * 0.035F;
            poseStack.translate(
                    Mth.cos(echoAngle) * echoOffset,
                    echo * 0.0015F,
                    Mth.sin(echoAngle) * echoOffset);
            Matrix4f echoMatrix = poseStack.last().pose();
            renderBrokenRing(vertices, echoMatrix,
                    radius * (0.43F + echo * 0.115F),
                    0.045F,
                    irregularAngle(time - echo * 4.0F, 10 + echo),
                    52 + echo * 4,
                    PALE_RED, PALE_GREEN, PALE_BLUE,
                    (0.085F - echo * 0.015F) * groundAlpha,
                    8 + echo);
            poseStack.popPose();
        }

        if (age < CASTING_TICKS + 7.0F) {
            float gathering = 1.0F - Mth.clamp(
                    Math.abs(age - CASTING_TICKS) / (CASTING_TICKS + 1.0F),
                    0.0F, 1.0F);
            renderRadialMark(vertices, groundMatrix,
                    radius * 0.08F, radius * 0.48F,
                    irregularAngle(time, 18), 0.12F,
                    PALE_RED, PALE_GREEN, PALE_BLUE,
                    0.36F * gathering);
            renderRadialMark(vertices, groundMatrix,
                    radius * 0.10F, radius * 0.34F,
                    irregularAngle(time, 19) + 118.0F, 0.075F,
                    RED, GREEN, BLUE, 0.27F * gathering);
        }

        float expansion = (age - CASTING_TICKS + 4.0F)
                / (EXPANSION_TICKS + 4.0F);
        if (expansion >= 0.0F && expansion <= 1.0F) {
            float eased = smoothStep(expansion);
            renderHorizontalRing(vertices, groundMatrix,
                    (float) ShixuChiliu.DOMAIN_RADIUS * eased,
                    0.025F, 0.20F + 0.18F * (1.0F - eased),
                    0.0F, 80,
                    PALE_RED, PALE_GREEN, PALE_BLUE,
                    0.68F * (1.0F - eased));
            renderHorizontalRing(vertices, groundMatrix,
                    (float) ShixuChiliu.DOMAIN_RADIUS * eased * 0.965F,
                    0.040F, 0.07F,
                    0.0F, 72,
                    RED, GREEN, BLUE,
                    0.24F * (1.0F - eased));
        }
        poseStack.popPose();
    }

    private static void renderLivingAfterimages(
            ClientLevel level, PoseStack poseStack, VertexConsumer vertices,
            Camera camera, Vec3 cameraPosition, float partialTick) {
        long now = level.getGameTime();
        for (Map.Entry<Integer, LivingTrail> entry : LIVING_TRAILS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)
                    || entity.isRemoved()
                    || now - entry.getValue().lastSeen > 3L) continue;
            List<LivingSample> samples = new ArrayList<>(entry.getValue().samples);
            int rendered = 0;
            for (int index = 1; index < samples.size() && rendered < 3; index++) {
                LivingSample sample = samples.get(index);
                float alpha = switch (rendered) {
                    case 0 -> 0.075F;
                    case 1 -> 0.047F;
                    default -> 0.027F;
                };
                renderGhostSilhouette(
                        poseStack, vertices, camera,
                        sample.position.subtract(cameraPosition),
                        living.getBbWidth(), living.getBbHeight(),
                        sample.yaw, alpha);
                rendered++;
            }

            Vec3 current = interpolatedPosition(living, partialTick);
            poseStack.pushPose();
            poseStack.translate(
                    current.x - cameraPosition.x,
                    current.y + 0.025D - cameraPosition.y,
                    current.z - cameraPosition.z);
            Matrix4f matrix = poseStack.last().pose();
            float ringPulse = 0.04F + 0.025F
                    * Mth.sin((now + living.getId()) * 0.31F);
            renderBrokenRing(vertices, matrix,
                    Math.max(0.42F, living.getBbWidth() * 0.75F),
                    0.035F, irregularAngle(now, living.getId()),
                    24, RED, GREEN, BLUE, ringPulse, living.getId());
            if (living.getAttackAnim(partialTick) > 0.04F) {
                float attack = living.getAttackAnim(partialTick);
                renderAttackEcho(vertices, matrix, living, attack);
            }
            poseStack.popPose();
        }
    }

    private static void renderProjectileAfterimages(
            ClientLevel level, PoseStack poseStack, VertexConsumer vertices,
            Camera camera, Vec3 cameraPosition) {
        long now = level.getGameTime();
        for (Map.Entry<Integer, ProjectileTrail> entry : PROJECTILE_TRAILS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            ProjectileTrail trail = entry.getValue();
            if (!(entity instanceof Projectile projectile)
                    || entity.isRemoved() || now - trail.lastSeen > 4L) continue;
            List<Vec3> samples = new ArrayList<>(trail.samples);
            renderProjectileRibbon(
                    poseStack, vertices, samples, cameraPosition,
                    projectile.getBbHeight());
            int index = 0;
            for (Vec3 sample : samples) {
                if (index >= 3) break;
                float alpha = 0.16F - index * 0.042F;
                renderProjectileGhost(poseStack, vertices, camera,
                        sample.add(0.0D, projectile.getBbHeight() * 0.5D, 0.0D)
                                .subtract(cameraPosition),
                        0.11F + index * 0.025F, alpha);
                index++;
            }
        }
    }

    private static void renderGhostSilhouette(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 position, float width, float height,
            float yaw, float alpha) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        Matrix4f worldMatrix = poseStack.last().pose();
        renderArc(vertices, worldMatrix,
                Math.max(0.34F, width * 0.54F), 0.025F,
                0.030F, -yaw - 32.0F, -yaw + 32.0F,
                10, PALE_RED, PALE_GREEN, PALE_BLUE,
                alpha * 1.45F);
        poseStack.mulPose(camera.rotation());
        Matrix4f matrix = poseStack.last().pose();
        float bodyHalf = Math.max(0.12F, width * 0.34F);
        float legHalf = bodyHalf * 0.42F;
        float torsoBottom = height * 0.34F;
        float torsoTop = height * 0.79F;
        quad(vertices, matrix,
                -bodyHalf, torsoBottom, bodyHalf, torsoTop,
                PALE_RED, PALE_GREEN, PALE_BLUE, alpha);
        quad(vertices, matrix,
                -bodyHalf, height * 0.08F, -0.025F, torsoBottom,
                RED, GREEN, BLUE, alpha * 0.82F);
        quad(vertices, matrix,
                0.025F, height * 0.08F, bodyHalf, torsoBottom,
                RED, GREEN, BLUE, alpha * 0.82F);
        quad(vertices, matrix,
                -legHalf, height * 0.80F, legHalf, height,
                PALE_RED, PALE_GREEN, PALE_BLUE, alpha * 0.9F);
        poseStack.popPose();
    }

    private static void renderProjectileRibbon(
            PoseStack poseStack, VertexConsumer vertices,
            List<Vec3> samples, Vec3 cameraPosition, float height) {
        if (samples.size() < 2) return;
        Matrix4f matrix = poseStack.last().pose();
        int segmentCount = Math.min(5, samples.size() - 1);
        for (int index = 0; index < segmentCount; index++) {
            Vec3 startWorld = samples.get(index)
                    .add(0.0D, height * 0.5D, 0.0D);
            Vec3 endWorld = samples.get(index + 1)
                    .add(0.0D, height * 0.5D, 0.0D);
            Vec3 direction = endWorld.subtract(startWorld);
            if (direction.lengthSqr() < 1.0E-6D) continue;
            Vec3 midpoint = startWorld.add(endWorld).scale(0.5D);
            Vec3 side = direction.cross(cameraPosition.subtract(midpoint));
            if (side.lengthSqr() < 1.0E-6D) {
                side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-6D) continue;
            double ribbonWidth = 0.055D + index * 0.012D;
            side = side.normalize().scale(ribbonWidth);
            Vec3 start = startWorld.subtract(cameraPosition);
            Vec3 end = endWorld.subtract(cameraPosition);
            float alpha = 0.15F - index * 0.021F;
            trailVertex(vertices, matrix, start.add(side),
                    0.0F, 0.0F, alpha);
            trailVertex(vertices, matrix, start.subtract(side),
                    1.0F, 0.0F, alpha);
            trailVertex(vertices, matrix, end.subtract(side),
                    1.0F, 1.0F, alpha * 0.45F);
            trailVertex(vertices, matrix, end.add(side),
                    0.0F, 1.0F, alpha * 0.45F);
        }
    }

    private static void trailVertex(
            VertexConsumer vertices, Matrix4f matrix, Vec3 position,
            float u, float v, float alpha) {
        vertices.addVertex(matrix,
                        (float) position.x, (float) position.y,
                        (float) position.z)
                .setColor(PALE_RED, PALE_GREEN, PALE_BLUE, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void renderProjectileGhost(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 position, float radius, float alpha) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(camera.rotation());
        quad(vertices, poseStack.last().pose(),
                -radius, -radius, radius, radius,
                PALE_RED, PALE_GREEN, PALE_BLUE, alpha);
        poseStack.popPose();
    }

    private static void renderAttackEcho(
            VertexConsumer vertices, Matrix4f matrix,
            LivingEntity entity, float attack) {
        float radius = Math.max(0.7F, entity.getBbWidth() * 0.95F);
        float yaw = -entity.getYRot() + 90.0F;
        for (int echo = 0; echo < 3; echo++) {
            float delayed = Mth.clamp(attack - echo * 0.12F, 0.0F, 1.0F);
            float start = yaw - 70.0F + delayed * 95.0F - echo * 8.0F;
            renderArc(vertices, matrix, radius + echo * 0.06F,
                    entity.getBbHeight() * (0.50F + echo * 0.035F),
                    0.035F, start, start + 92.0F,
                    18, PALE_RED, PALE_GREEN, PALE_BLUE,
                    0.09F - echo * 0.02F);
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isLocalPlayerTimeDragged()) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        graphics.fill(0, 0, width, height, 0x071F1707);
        int heldBand = Math.floorMod((int) (tick * 17L), Math.max(1, height));
        graphics.fill(0, heldBand, width, Math.min(height, heldBand + 2),
                0x10F6D776);
        if (tick % 7L == 0L) {
            graphics.fill(0, 0, 3, height, 0x12F6D776);
            graphics.fill(width - 3, 0, width, height, 0x0CC49339);
        }
    }

    /** Keeps input and networking live while world interpolation updates in held steps. */
    public static float quantizePartialTick(float partialTick) {
        if (!isLocalPlayerTimeDragged()) return partialTick;
        float clamped = Mth.clamp(partialTick, 0.0F, 1.0F);
        Minecraft minecraft = Minecraft.getInstance();
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int phase = Math.floorMod((int) tick, 7);
        if (phase == 2 || phase == 6) {
            return 0.0F;
        }
        float releasePoint = switch (phase) {
            case 0, 4 -> 0.90F;
            case 1 -> 0.68F;
            default -> 0.79F;
        };
        return clamped < releasePoint ? 0.0F : releasePoint;
    }

    /** Different particles advance on different ticks, producing suspended and catching motes. */
    public static boolean shouldDelayParticle(Particle particle) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || DOMAINS.isEmpty()
                || !isInsideAnyDomain(particle.getPos())) {
            return false;
        }
        int identity = System.identityHashCode(particle);
        int divisor = 2 + Math.floorMod(identity, 4);
        int phase = Math.floorMod(identity >>> 3, divisor);
        return Math.floorMod((int) minecraft.level.getGameTime(), divisor) != phase;
    }

    public static boolean isLocalPlayerTimeDragged() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || minecraft.player.isSpectator()) return false;
        Player localPlayer = minecraft.player;
        for (Player caster : minecraft.level.players()) {
            if (caster == localPlayer
                    || !caster.hasEffect(ModEffects.SHIXU_CHILIU)
                    || caster.isAlliedTo(localPlayer)) continue;
            if (caster.distanceToSqr(localPlayer) <= RADIUS_SQUARED) return true;
        }
        return false;
    }

    private static boolean isInsideAnyDomain(Vec3 position) {
        for (Vec3 center : ACTIVE_CENTERS) {
            if (center.distanceToSqr(position) <= RADIUS_SQUARED) return true;
        }
        return false;
    }

    private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static float fieldScale(float age) {
        float progress = Mth.clamp(age / CASTING_TICKS, 0.0F, 1.0F);
        return 0.14F + 0.86F * smoothStep(progress);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float irregularAngle(float time, int ring) {
        int hold = 3 + Math.floorMod(ring * 5, 6);
        int step = Mth.floor((time + ring * 1.7F) / hold);
        float direction = Math.floorMod(step + ring, 9) == 0 ? -0.55F : 1.0F;
        return step * (2.4F + ring * 0.31F) * direction;
    }

    private static void renderSphereShell(
            PoseStack poseStack, VertexConsumer vertices,
            float radius, float red, float green, float blue, float alpha) {
        Matrix4f matrix = poseStack.last().pose();
        int longitudeSegments = 48;
        int latitudeSegments = 24;
        for (int latitudeIndex = 0;
             latitudeIndex < latitudeSegments; latitudeIndex++) {
            float latitude0 = (float) (-Math.PI / 2.0D
                    + Math.PI * latitudeIndex / latitudeSegments);
            float latitude1 = (float) (-Math.PI / 2.0D
                    + Math.PI * (latitudeIndex + 1) / latitudeSegments);
            float v0 = (float) latitudeIndex / latitudeSegments;
            float v1 = (float) (latitudeIndex + 1) / latitudeSegments;
            for (int longitudeIndex = 0;
                 longitudeIndex < longitudeSegments; longitudeIndex++) {
                float longitude0 = Mth.TWO_PI * longitudeIndex / longitudeSegments;
                float longitude1 = Mth.TWO_PI * (longitudeIndex + 1) / longitudeSegments;
                float u0 = (float) longitudeIndex / longitudeSegments;
                float u1 = (float) (longitudeIndex + 1) / longitudeSegments;
                sphereVertex(vertices, matrix, radius,
                        latitude0, longitude0, u0, v0,
                        red, green, blue, alpha);
                sphereVertex(vertices, matrix, radius,
                        latitude1, longitude0, u0, v1,
                        red, green, blue, alpha);
                sphereVertex(vertices, matrix, radius,
                        latitude1, longitude1, u1, v1,
                        red, green, blue, alpha);
                sphereVertex(vertices, matrix, radius,
                        latitude0, longitude1, u1, v0,
                        red, green, blue, alpha);
            }
        }
    }

    private static void renderTemporalMeridians(
            PoseStack poseStack, VertexConsumer vertices,
            float radius, float time, float alpha) {
        if (radius <= 0.01F || alpha <= 0.001F) return;
        Matrix4f matrix = poseStack.last().pose();
        int meridians = 12;
        int latitudeSegments = 24;
        int heldStep = Mth.floor(time / 4.0F);
        float rotation = irregularAngle(time, 21) * Mth.DEG_TO_RAD;
        float halfWidth = Math.min(0.012F, 0.075F / radius);
        for (int meridian = 0; meridian < meridians; meridian++) {
            float longitude = rotation
                    + Mth.TWO_PI * meridian / meridians;
            for (int latitude = 1;
                 latitude < latitudeSegments - 1; latitude++) {
                if (Math.floorMod(
                        latitude * 5 + meridian * 7 + heldStep, 13) < 5) {
                    continue;
                }
                float latitude0 = (float) (-Math.PI / 2.0D
                        + Math.PI * latitude / latitudeSegments);
                float latitude1 = (float) (-Math.PI / 2.0D
                        + Math.PI * (latitude + 0.72F) / latitudeSegments);
                float segmentAlpha = alpha * (0.48F + 0.34F
                        * Mth.sin(latitude * 1.73F + meridian * 0.91F));
                sphereVertex(vertices, matrix, radius,
                        latitude0, longitude - halfWidth,
                        0.0F, 0.0F,
                        PALE_RED, PALE_GREEN, PALE_BLUE, segmentAlpha);
                sphereVertex(vertices, matrix, radius,
                        latitude1, longitude - halfWidth,
                        0.0F, 1.0F,
                        PALE_RED, PALE_GREEN, PALE_BLUE, segmentAlpha);
                sphereVertex(vertices, matrix, radius,
                        latitude1, longitude + halfWidth,
                        1.0F, 1.0F,
                        RED, GREEN, BLUE, segmentAlpha * 0.72F);
                sphereVertex(vertices, matrix, radius,
                        latitude0, longitude + halfWidth,
                        1.0F, 0.0F,
                        RED, GREEN, BLUE, segmentAlpha * 0.72F);
            }
        }
    }

    private static void sphereVertex(
            VertexConsumer vertices, Matrix4f matrix, float radius,
            float latitude, float longitude, float u, float v,
            float red, float green, float blue, float alpha) {
        float horizontal = Mth.cos(latitude);
        float x = radius * horizontal * Mth.cos(longitude);
        float y = radius * Mth.sin(latitude);
        float z = radius * horizontal * Mth.sin(longitude);
        vertices.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(x / radius, y / radius, z / radius);
    }

    private static void renderBrokenRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float width, float angleOffset,
            int segments, float red, float green, float blue,
            float alpha, int pattern) {
        for (int segment = 0; segment < segments; segment++) {
            if (Math.floorMod(segment * 7 + pattern * 5, 17) < 3) continue;
            float start = angleOffset + 360.0F * segment / segments;
            float end = angleOffset + 360.0F * (segment + 0.78F) / segments;
            renderArc(vertices, matrix, radius, 0.0F, width,
                    start, end, 1, red, green, blue, alpha);
        }
    }

    private static void renderClockMarks(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float angleOffset, float alpha) {
        for (int mark = 0; mark < 24; mark++) {
            float angle = angleOffset + mark * 15.0F;
            float length = mark % 6 == 0 ? 0.95F
                    : mark % 3 == 0 ? 0.62F : 0.36F;
            renderRadialMark(vertices, matrix,
                    radius - length, radius + 0.10F,
                    angle, mark % 6 == 0 ? 0.095F : 0.055F,
                    PALE_RED, PALE_GREEN, PALE_BLUE,
                    alpha * (mark % 6 == 0 ? 0.34F : 0.20F));
        }
    }

    private static void renderHorizontalRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float y, float width,
            float angleOffset, int segments,
            float red, float green, float blue, float alpha) {
        renderArc(vertices, matrix, radius, y, width,
                angleOffset, angleOffset + 360.0F, segments,
                red, green, blue, alpha);
    }

    private static void renderArc(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float y, float width,
            float startDegrees, float endDegrees, int segments,
            float red, float green, float blue, float alpha) {
        float inner = Math.max(0.0F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        for (int segment = 0; segment < segments; segment++) {
            float progress0 = (float) segment / segments;
            float progress1 = (float) (segment + 1) / segments;
            float angle0 = Mth.DEG_TO_RAD
                    * Mth.lerp(progress0, startDegrees, endDegrees);
            float angle1 = Mth.DEG_TO_RAD
                    * Mth.lerp(progress1, startDegrees, endDegrees);
            ringVertex(vertices, matrix, inner, angle0, y,
                    0.0F, 0.0F, red, green, blue, alpha);
            ringVertex(vertices, matrix, outer, angle0, y,
                    0.0F, 1.0F, red, green, blue, alpha);
            ringVertex(vertices, matrix, outer, angle1, y,
                    1.0F, 1.0F, red, green, blue, alpha);
            ringVertex(vertices, matrix, inner, angle1, y,
                    1.0F, 0.0F, red, green, blue, alpha);
        }
    }

    private static void renderRadialMark(
            VertexConsumer vertices, Matrix4f matrix,
            float inner, float outer, float angleDegrees, float width,
            float red, float green, float blue, float alpha) {
        float angle = angleDegrees * Mth.DEG_TO_RAD;
        float sideX = -Mth.sin(angle) * width * 0.5F;
        float sideZ = Mth.cos(angle) * width * 0.5F;
        float innerX = Mth.cos(angle) * inner;
        float innerZ = Mth.sin(angle) * inner;
        float outerX = Mth.cos(angle) * outer;
        float outerZ = Mth.sin(angle) * outer;
        flatVertex(vertices, matrix,
                innerX + sideX, 0.002F, innerZ + sideZ,
                0.0F, 0.0F, red, green, blue, alpha);
        flatVertex(vertices, matrix,
                outerX + sideX, 0.002F, outerZ + sideZ,
                0.0F, 1.0F, red, green, blue, alpha);
        flatVertex(vertices, matrix,
                outerX - sideX, 0.002F, outerZ - sideZ,
                1.0F, 1.0F, red, green, blue, alpha);
        flatVertex(vertices, matrix,
                innerX - sideX, 0.002F, innerZ - sideZ,
                1.0F, 0.0F, red, green, blue, alpha);
    }

    private static void ringVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float angle, float y, float u, float v,
            float red, float green, float blue, float alpha) {
        flatVertex(vertices, matrix,
                Mth.cos(angle) * radius, y, Mth.sin(angle) * radius,
                u, v, red, green, blue, alpha);
    }

    private static void flatVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float z, float u, float v,
            float red, float green, float blue, float alpha) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void quad(
            VertexConsumer vertices, Matrix4f matrix,
            float minX, float minY, float maxX, float maxY,
            float red, float green, float blue, float alpha) {
        billboardVertex(vertices, matrix,
                minX, minY, 0.0F, 0.0F,
                red, green, blue, alpha);
        billboardVertex(vertices, matrix,
                maxX, minY, 1.0F, 0.0F,
                red, green, blue, alpha);
        billboardVertex(vertices, matrix,
                maxX, maxY, 1.0F, 1.0F,
                red, green, blue, alpha);
        billboardVertex(vertices, matrix,
                minX, maxY, 0.0F, 1.0F,
                red, green, blue, alpha);
    }

    private static void billboardVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float u, float v,
            float red, float green, float blue, float alpha) {
        vertices.addVertex(matrix, x, y, 0.0F)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    private static final class DomainState {
        private final long startTick;

        private DomainState(long startTick) {
            this.startTick = startTick;
        }
    }

    private static final class LivingTrail {
        private final ArrayDeque<LivingSample> samples = new ArrayDeque<>();
        private long lastSeen;

        private void record(LivingEntity entity, long now) {
            LivingSample newest = samples.peekFirst();
            Vec3 position = entity.position();
            boolean moved = newest == null
                    || newest.position.distanceToSqr(position) > 0.0025D;
            boolean turned = newest == null
                    || Math.abs(Mth.wrapDegrees(
                            entity.getYRot() - newest.yaw)) > 2.0F;
            if (moved || turned) {
                samples.addFirst(new LivingSample(
                        position, entity.getYRot()));
            }
            while (samples.size() > MAX_TRAIL_SAMPLES) samples.removeLast();
            lastSeen = now;
        }
    }

    private record LivingSample(Vec3 position, float yaw) {
    }

    private static final class ProjectileTrail {
        private final ArrayDeque<Vec3> samples = new ArrayDeque<>();
        private long lastSeen;
        private boolean inside;

        private void record(Projectile projectile, long now) {
            Vec3 position = projectile.position();
            if (samples.isEmpty()) {
                Vec3 travel = projectile.getDeltaMovement();
                if (travel.lengthSqr() < 1.0E-5D) {
                    travel = position.subtract(
                            projectile.xo, projectile.yo, projectile.zo);
                }
                if (travel.lengthSqr() < 1.0E-5D) {
                    travel = projectile.getLookAngle().scale(0.35D);
                }
                Vec3 step = travel.normalize().scale(
                        Mth.clamp(travel.length(), 0.28D, 0.72D));
                for (int sample = 0; sample < 5; sample++) {
                    samples.addLast(position.subtract(step.scale(sample)));
                }
            } else if (samples.peekFirst()
                    .distanceToSqr(position) > 0.0025D) {
                samples.addFirst(position);
            }
            while (samples.size() > MAX_TRAIL_SAMPLES) samples.removeLast();
            lastSeen = now;
        }
    }
}
