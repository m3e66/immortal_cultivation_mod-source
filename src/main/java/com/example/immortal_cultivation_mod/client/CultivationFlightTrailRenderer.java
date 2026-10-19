package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.network.CultivationFlightPayloads;
import com.example.immortal_cultivation_mod.physics.CultivationFlightTrail;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Video-reference flight wake: bright tapered filaments, soft leading aura and lingering curved paths. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID, value=Dist.CLIENT)
public final class CultivationFlightTrailRenderer {
    private static final int MAX_TRAILS = 64;
    private static final int MAX_VISIBLE_TRAILS = 24;
    private static final double DRAW_DISTANCE_SQUARED = 144 * 144;
    private static final Map<UUID, Wake> WAKES = new HashMap<>();
    private static ClientLevel world;
    private static int renderedQuads;
    private static final RenderType GLOW = FilamentVfx.TYPE;

    private static final class Wake {
        final CultivationFlightTrail path = new CultivationFlightTrail();
        final UUID uuid;
        int entityId, color;
        float strength;
        long until;
        boolean active;
        Wake(UUID uuid) { this.uuid = uuid; }
    }
    private CultivationFlightTrailRenderer() {}
    private static boolean useLevel(ClientLevel level) {
        if (world != level) { WAKES.clear(); world = level; }
        return level != null;
    }

    public static void accept(CultivationFlightPayloads.Trail packet) {
        var level = Minecraft.getInstance().level;
        if (!useLevel(level) || !level.dimension().location().equals(packet.dimension()) || !Float.isFinite(packet.strength())) return;
        Entity entity = level.getEntity(packet.entityId());
        if (entity != null && entity.getUUID().equals(packet.uuid()) && suppressed(entity)) {
            WAKES.remove(packet.uuid());return;
        }
        Wake wake = WAKES.get(packet.uuid());
        if (wake == null) {
            if (packet.strength() <= 0 || WAKES.size() >= MAX_TRAILS) return;
            wake = new Wake(packet.uuid()); WAKES.put(packet.uuid(), wake);
        }
        wake.entityId = packet.entityId(); wake.color = packet.color() & 0xFFFFFF;
        wake.active = packet.strength() > 0;
        if (wake.active) wake.strength = Mth.clamp(packet.strength(), .4F, 1.7F);
        wake.until = level.getGameTime() + 12;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || mc.isPaused()) return;
        long now = world.getGameTime();
        WAKES.entrySet().removeIf(entry -> {
            Wake wake = entry.getValue();
            Entity entity = world.getEntity(wake.entityId);
            if (entity == null || !entity.getUUID().equals(wake.uuid) || !entity.isAlive() || entity.isInvisible() || suppressed(entity)) return true;
            if (wake.active && now < wake.until) wake.path.sample(anchor(entity, 1), now);
            else wake.active = false;
            wake.path.prune(now);
            return !wake.active && wake.path.opacity(now) <= 0;
        });
    }

    private static boolean suppressed(Entity entity) {
        return entity instanceof CultivationNpc
                || entity instanceof Player player && FlyingSwordEntity.flightSword(player) != null;
    }

    private static Vec3 anchor(Entity entity, float partial) {
        float pose = entity instanceof Player player ? JindanFlightEffects.fastFlightPoseProgress(player, partial) : 0;
        // The horizontal flight pose rotates around the feet, placing its head ahead of the entity origin.
        Vec3 movement = entity.getDeltaMovement();
        Vec3 ahead = movement.lengthSqr() > .00001 ? movement.normalize().scale(1.9 * pose) : Vec3.ZERO;
        return entity.getPosition(partial).add(0, Mth.lerp(pose, entity.getBbHeight() * .48, .25), 0).add(ahead);
    }
    public static int pointCount(UUID uuid) { Wake wake = WAKES.get(uuid); return wake == null ? 0 : wake.path.size(); }
    public static int trailCount() { return WAKES.size(); }
    public static int renderedQuads() { return renderedQuads; }
    public static int tint(UUID uuid) { Wake wake = WAKES.get(uuid); return wake == null ? 0 : wake.color; }

    /** Hide the complete player render only while an actual, drawable wake replaces it. */
    public static boolean shouldHidePlayer(Player player) {
        var mc = Minecraft.getInstance();
        if (world == null || world != mc.level || player.level() != world
                || !player.isAlive() || player.isInvisible() || player.isSpectator() || suppressed(player)) return false;
        Wake wake = WAKES.get(player.getUUID());
        long now = world.getGameTime();
        if (wake == null || wake.entityId != player.getId() || !wake.active || now >= wake.until) return false;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        double time = now + partial;
        if (!drawable(wake, camera, time, partial)) return false;
        // The same nearest-first budget governs both trail drawing and hiding. Distant
        // players must not disappear when their wake is omitted from the render budget.
        int closer = 0;
        for (Wake other : WAKES.values()) {
            if (other != wake && drawable(other, camera, time, partial) && compareDistance(other, wake, camera) < 0
                    && ++closer >= MAX_VISIBLE_TRAILS) return false;
        }
        return true;
    }

    private static boolean drawable(Wake wake, Vec3 camera, double time, float partial) {
        Entity entity = world.getEntity(wake.entityId);
        return entity != null && entity.getUUID().equals(wake.uuid) && entity.isAlive() && !entity.isInvisible() && !suppressed(entity)
                && entity.distanceToSqr(camera) < DRAW_DISTANCE_SQUARED
                && (wake.path.connectedTo(anchor(entity, 1))
                && wake.path.size() > 1 && wake.path.opacity(time) > 0
                && wake.path.hasVisibleSegment(wake.active ? anchor(entity, partial) : null));
    }

    private static int compareDistance(Wake first, Wake second, Vec3 camera) {
        int distance = Double.compare(world.getEntity(first.entityId).distanceToSqr(camera),
                world.getEntity(second.entityId).distanceToSqr(camera));
        return distance != 0 ? distance : first.uuid.compareTo(second.uuid);
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ShaderPackCompatibility.isRenderingShadowPass()) return;
        renderedQuads = 0;
        var mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || WAKES.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = world.getGameTime() + partial;
        var buffers = mc.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(GLOW);
        Matrix4f matrix = event.getPoseStack().last().pose();
        List<Wake> nearby = WAKES.values().stream().filter(w -> drawable(w, camera, time, partial))
                .sorted((a, b) -> compareDistance(a, b, camera)).limit(MAX_VISIBLE_TRAILS).toList();
        for (Wake wake : nearby) draw(wake, vertices, matrix, camera, partial, time, event);
        buffers.endBatch(GLOW);
    }

    private static void draw(Wake wake, VertexConsumer vertices, Matrix4f matrix, Vec3 camera, float partial,
                             double time, RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        Entity entity = world.getEntity(wake.entityId);
        List<Vec3> centers = wake.path.points(wake.active ? anchor(entity, partial) : null);
        float fade = wake.path.opacity(time);
        float r = ((wake.color >> 16) & 255) / 255F, g = ((wake.color >> 8) & 255) / 255F, b = (wake.color & 255) / 255F;
        if (centers.size() < 2 || fade <= 0) return;
        boolean firstPerson = entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        ParticleStatus quality = mc.options.particles().get();
        int strands = quality == ParticleStatus.MINIMAL ? 12 : quality == ParticleStatus.DECREASED ? 24 : 36;
        if (entity.distanceToSqr(camera) > 64 * 64) strands = Math.min(strands, 12);
        else if (entity.distanceToSqr(camera) > 32 * 32) strands = Math.min(strands, 24);
        int n = centers.size();
        Vec3[] forward = new Vec3[n], sides = new Vec3[n], ups = new Vec3[n];
        double[] distances = new double[n];
        for (int i = 0; i < n; i++) {
            forward[i] = centers.get(Math.max(0, i - 1)).subtract(centers.get(Math.min(n - 1, i + 1))).normalize();
            sides[i] = CultivationFlightTrail.side(forward[i], i == 0 ? null : sides[i - 1]);
            ups[i] = sides[i].cross(forward[i]).normalize();
            if (i > 0) distances[i] = distances[i - 1] + centers.get(i).distanceTo(centers.get(i - 1));
        }
        double total = Math.max(.1, distances[n - 1]);
        // A continuous luminous center fills the wake instead of leaving an empty wire cage.
        for (int i = 0; i < n - 1; i++) {
            double u0 = distances[i] / total, u1 = distances[i + 1] / total;
            Vec3 a = centers.get(i), end = centers.get(i + 1);
            float alpha0 = fade * tailFade(u0), alpha1 = fade * tailFade(u1);
            if (firstPerson) {
                alpha0 *= Mth.clamp((float) (a.distanceTo(camera) - 2.8) / 1.5F, 0, 1);
                alpha1 *= Mth.clamp((float) (end.distanceTo(camera) - 2.8) / 1.5F, 0, 1);
            }
            double width = (.24 + wake.strength * .09) * (1 - u0 * .82);
            ribbon(vertices, matrix, a, end, camera, width * 2.4, r, g, b, alpha0 * .13F, alpha1 * .13F);
            ribbon(vertices, matrix, a, end, camera, width, .3F + r * .7F, .3F + g * .7F, .3F + b * .7F,
                    alpha0 * .44F, alpha1 * .44F);
        }
        for (int strand = 0; strand < strands; strand++) {
            double angle = strand * Math.PI * 2 / strands;
            double lengthFraction = .62 + .38 * ((strand * 7 % 11) / 10D);
            List<Vec3> line = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double distance = distances[i], u = distance / total;
                double radius = (.45 + u * .70) * (1 - Math.exp(-distance * 1.4));
                radius += Math.sin(Math.min(1, distance / 2.5) * Math.PI) * .3;
                radius *= switch (strand % 3) { case 0 -> .48; case 1 -> .76; default -> 1; };
                double swirl = angle + Math.sin(distance * .3 - time * .065 + strand) * .12;
                double flutter = Math.sin(distance * .9 + time * .12 + strand * 2.1) * .035;
                line.add(centers.get(i).add(sides[i].scale(Math.cos(swirl) * (radius + flutter)))
                        .add(ups[i].scale(Math.sin(swirl) * (radius + flutter))));
            }
            for (int i = 0; i < n - 1; i++) {
                double u0 = distances[i] / total / lengthFraction, u1 = distances[i + 1] / total / lengthFraction;
                if (u0 >= 1) break;
                Vec3 a = line.get(i), end = line.get(i + 1);
                float alpha0 = fade * tailFade(u0), alpha1 = fade * tailFade(u1);
                if (firstPerson) {
                    alpha0 *= Mth.clamp((float) (a.distanceTo(camera) - 2.8) / 1.5F, 0, 1);
                    alpha1 *= Mth.clamp((float) (end.distanceTo(camera) - 2.8) / 1.5F, 0, 1);
                }
                double width = (strand % 3 == 0 ? .045 : .023) * (.8 + wake.strength * .4) * (1 - u0 * .78);
                ribbon(vertices, matrix, a, end, camera, width * 9, r, g, b, alpha0 * .14F, alpha1 * .14F);
                ribbon(vertices, matrix, a, end, camera, width * 3.3, r, g, b, alpha0 * .42F, alpha1 * .42F);
                ribbon(vertices, matrix, a, end, camera, width, .4F + r * .6F, .4F + g * .6F, .4F + b * .6F, alpha0, alpha1);
            }
        }
        if (!firstPerson) {
            Vec3 head = centers.getFirst().add(forward[0].scale(.35));
            Vector3f cameraRight = new Vector3f(1, 0, 0).rotate(event.getCamera().rotation());
            Vector3f cameraUp = new Vector3f(0, 1, 0).rotate(event.getCamera().rotation());
            Vec3 right = new Vec3(cameraRight), up = new Vec3(cameraUp);
            double pulse = 1 + Math.sin(time * .38) * .035;
            halo(vertices, matrix, head, camera, right, up, 1.35 * pulse, r, g, b, fade * .20F);
            halo(vertices, matrix, head, camera, right, up, .68 * pulse, r, g, b, fade * .55F);
            halo(vertices, matrix, head, camera, right, up, .24, 1, 1, 1, fade * .95F);
            // Small motes accent the denser continuous ribbons without turning them into particle fog.
            if (quality != ParticleStatus.MINIMAL) for (int i = 2; i < n; i += 3) {
                Vec3 mote = centers.get(i).add(sides[i].scale(Math.sin(i * 2.3) * .9)).add(ups[i].scale(Math.cos(i * 1.7) * .7));
                halo(vertices, matrix, mote, camera, right, up, .045, .8F + r * .2F, .8F + g * .2F, .8F + b * .2F, fade * .65F);
            }
        }
    }

    private static float tailFade(double u) { return (float) (Mth.clamp((1 - u) * 5, 0, 1) * Math.max(0, 1 - u * .28)); }

    private static void ribbon(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 camera, double width,
                               float r, float g, float blue, float alphaA, float alphaB) {
        renderedQuads += FilamentVfx.ribbon(out, matrix, a, b, camera, width, r, g, blue, alphaA, alphaB);
    }

    private static void halo(VertexConsumer out, Matrix4f matrix, Vec3 center, Vec3 camera, Vec3 right, Vec3 up,
                             double size, float r, float g, float b, float alpha) {
        renderedQuads += FilamentVfx.halo(out, matrix, center, camera, right, up, size, r, g, b, alpha);
    }
}
