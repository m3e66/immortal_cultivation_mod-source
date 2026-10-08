package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.effect.SpellEmitterPresets;
import com.example.immortal_cultivation_mod.effect.SpellEmitterPresets.Definition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/** Native simulation for the features actually present in the authored spell presets. */
final class OriginalSpellEmitter {
    private final Definition definition;
    private final RandomSource random;
    private final boolean filamentStyle;
    private final PhotonSpellStyle.Form form;
    private final List<Mote> motes = new ArrayList<>();
    private Vec3 anchor = Vec3.ZERO, previousAnchor = Vec3.ZERO;
    private Quaternionf rotation = new Quaternionf(), previousRotation = new Quaternionf();
    private int age;
    private double distance;

    OriginalSpellEmitter(Definition definition, long seed, String preset) {
        this.definition = definition;
        this.random = RandomSource.create(seed);
        this.form = PhotonSpellStyle.form(preset);
        this.filamentStyle = form != PhotonSpellStyle.Form.LEGACY;
    }


    void tick(ClientLevel level, Vec3 root, Quaternionf rootRotation) {
        previousAnchor = anchor;
        previousRotation = rotation;
        anchor = root.add(SpellEmitterPresets.rotate(definition.offset(), rootRotation));
        rotation = new Quaternionf(rootRotation).mul(definition.rotation());
        if (age == 0) { previousAnchor = anchor; previousRotation = rotation; }
        else distance += anchor.distanceTo(previousAnchor);
        if (definition.beam()) { age++; return; }
        // Finished particles can retain their world-space trail until its last sample expires.
        for (Mote mote : motes) mote.tick(level);
        motes.removeIf(mote -> mote.age >= mote.lifetime && mote.trail.isEmpty());
        int living = 0;
        for (Mote mote : motes) {
            if (mote.age < mote.lifetime) living++;
            // Attached trails occupied their own slot in the original particle budget.
            if (definition.trail != null && (mote.age < mote.lifetime || !mote.trail.isEmpty())) living++;
        }
        int count = definition.looping || age < definition.duration
                ? definition.emissionCount(age, random) : 0;
        if (definition.distanceInterval > 0) {
            int extra = (int) (distance / definition.distanceInterval);
            distance -= extra * definition.distanceInterval;
            count += extra;
        }
        for (int i = 0; i < Math.min(count, definition.maxParticles - living); i++) motes.add(new Mote());
        age++;
    }

    void render(VertexConsumer vertices, Matrix4f matrix, Camera camera, float partial, boolean filamentStyle) {
        // Meditation now has one coherent body-centered effect; retain only its soft support motes here.
        if (filamentStyle && (form == PhotonSpellStyle.Form.LEGACY || form == PhotonSpellStyle.Form.QI)) return;
        Vec3 currentAnchor = previousAnchor.lerp(anchor, partial);
        Quaternionf currentRotation = new Quaternionf(previousRotation).slerp(rotation, partial);
        if (definition.beam()) {
            Vec3 delta = SpellEmitterPresets.rotate(SpellEmitterPresets.multiply(
                    new Vec3(definition.end[0], definition.end[1], definition.end[2]), definition.scale), currentRotation);
            Vector4f color = SpellEmitterPresets.color(definition.color, 0);
            if (filamentStyle) {
                PhotonSpellStyle.beam(vertices, matrix, camera.getPosition(), currentAnchor, delta,
                        definition.width, color, age + partial);
            } else strip(vertices, matrix, camera, currentAnchor, currentAnchor.add(delta),
                    definition.width, 0, 1, color, color, false);
            return;
        }
        Vec3 right = SpellEmitterPresets.rotate(new Vec3(1, 0, 0), camera.rotation());
        Vec3 up = SpellEmitterPresets.rotate(new Vec3(0, 1, 0), camera.rotation());
        var quality = net.minecraft.client.Minecraft.getInstance().options.particles().get();
        int budget = !filamentStyle ? 64 : switch (quality) {
            case MINIMAL -> 64;
            case DECREASED -> 160;
            default -> 320;
        };
        if (filamentStyle && currentAnchor.distanceToSqr(camera.getPosition()) > 32 * 32) budget = Math.min(budget, 96);
        for (int drawn = 0; drawn < Math.min(motes.size(), budget); drawn++) {
            Mote mote = motes.get(filamentStyle ? motes.size() - 1 - drawn : drawn);
            if (mote.age < mote.lifetime) {
                Vec3 pos = mote.world(mote.previous.lerp(mote.position, partial), currentAnchor, currentRotation);
                if (filamentStyle) {
                    PhotonSpellStyle.particle(vertices, matrix, pos, camera.getPosition(), right, up,
                            definition.size[0] * definition.scale[0], definition.size[1] * definition.scale[1],
                            mote.color, mote.age + partial, mote.trailRandom,
                            (mote.age + partial) / mote.lifetime,
                            pos.subtract(mote.world(mote.previous, previousAnchor, previousRotation)), form);
                } else {
                    Vec3 x = right.scale(definition.size[0] * definition.scale[0]);
                    Vec3 y = up.scale(definition.size[1] * definition.scale[1]);
                    float u = definition.builtinCircle ? 2 : 0;
                    Vec3 rel = pos.subtract(camera.getPosition());
                    vertex(vertices, matrix, rel.subtract(x).subtract(y), u, 1, mote.color);
                    vertex(vertices, matrix, rel.add(x).subtract(y), u + 1, 1, mote.color);
                    vertex(vertices, matrix, rel.add(x).add(y), u + 1, 0, mote.color);
                    vertex(vertices, matrix, rel.subtract(x).add(y), u, 0, mote.color);
                }
            }
            mote.renderTrail(vertices, matrix, camera, partial, currentAnchor, currentRotation, filamentStyle);
        }
    }

    private final class Mote {
        private Vec3 position, previous, velocity;
        private final Vec3 birthAnchor = anchor;
        private final Quaternionf birthRotation = new Quaternionf(rotation);
        private final Vector4f color = SpellEmitterPresets.color(definition.color, random.nextFloat());
        private final int lifetime = Math.max(1, (int) SpellEmitterPresets.range(definition.lifetime, random.nextFloat()));
        private final float trailRandom = random.nextFloat();
        private final ArrayDeque<TrailPoint> trail = new ArrayDeque<>();
        private int age;
        private boolean collided;

        Mote() {
            var spawn = definition.spawn(random);
            position = spawn.position();
            previous = position;
            velocity = spawn.velocity();
            if (definition.trail != null) trail.add(new TrailPoint(world(position, anchor, rotation), 0));
        }

        Vec3 world(Vec3 local, Vec3 currentAnchor, Quaternionf currentRotation) {
            return (definition.local ? currentAnchor : birthAnchor).add(SpellEmitterPresets.rotate(
                    SpellEmitterPresets.multiply(local, definition.scale), definition.local ? currentRotation : birthRotation));
        }

        void tick(ClientLevel level) {
            previous = position;
            if (age < lifetime) {
                if (definition.physics == null) position = position.add(velocity);
                else moveWithPhysics(level);
            }
            age++;
            if (definition.trail == null) return;
            float keep = definition.trail.lifetime * lifetime;
            while (!trail.isEmpty() && age - trail.getFirst().tick >= keep) trail.removeFirst();
            if (age >= lifetime) {
                if (definition.trail.dieWithParticle) trail.clear();
                return;
            }
            Vec3 point = world(position, anchor, rotation);
            double spacing = definition.trail.minDistance;
            if (filamentStyle && !trail.isEmpty() && point.distanceToSqr(trail.getLast().position) > 100) trail.clear();
            if (trail.isEmpty() || point.distanceToSqr(trail.getLast().position) >= spacing * spacing) {
                trail.addLast(new TrailPoint(point, age));
            }
            if (filamentStyle) while (trail.size() > 24) trail.removeFirst();
        }

        private void moveWithPhysics(ClientLevel level) {
            var physics = definition.physics;
            Quaternionf basis = definition.local ? rotation : birthRotation;
            Vec3 worldVelocity = SpellEmitterPresets.rotate(SpellEmitterPresets.multiply(velocity, definition.scale), basis);
            Vec3 travel = worldVelocity;
            Vec3 point = world(position, anchor, rotation);
            if (physics.collision && travel.lengthSqr() < 10000) {
                travel = Entity.collideBoundingBox(null, travel,
                        new AABB(point, point).inflate(definition.size[0] / 2.0D,
                                definition.size[1] / 2.0D, definition.size[2] / 2.0D), level, List.of());
            }
            Quaternionf inverse = new Quaternionf(basis).conjugate();
            position = position.add(SpellEmitterPresets.rotate(travel, inverse).scale(1.0D / definition.scale[0]));
            int hitAxis = Math.abs(worldVelocity.x) / Math.abs(travel.x) > 1.001D ? 0
                    : Math.abs(worldVelocity.y) / Math.abs(travel.y) > 1.001D ? 1
                    : Math.abs(worldVelocity.z) / Math.abs(travel.z) > 1.001D ? 2 : -1;
            if (!collided && hitAxis >= 0) {
                if (physics.remove) { age = lifetime; return; }
                if (random.nextFloat() < physics.chance) {
                    double x = hitAxis == 0 ? -worldVelocity.x * physics.bounce : worldVelocity.x + spread(physics.spread);
                    double y = hitAxis == 1 ? -worldVelocity.y * physics.bounce : worldVelocity.y + spread(physics.spread);
                    double z = hitAxis == 2 ? -worldVelocity.z * physics.bounce : worldVelocity.z + spread(physics.spread);
                    worldVelocity = new Vec3(x, y, z);
                } else collided = true;
            }
            worldVelocity = worldVelocity.add(0, -0.04D * physics.gravity, 0)
                    .scale(physics.friction * (collided ? physics.collidedFriction : 1));
            velocity = SpellEmitterPresets.rotate(worldVelocity, inverse).scale(1.0D / definition.scale[0]);
        }

        private double spread(float amount) { return amount > 0 ? random.nextGaussian() * amount : 0; }

        private void renderTrail(VertexConsumer vertices, Matrix4f matrix, Camera camera, float partial,
                                  Vec3 currentAnchor, Quaternionf currentRotation, boolean filamentStyle) {
            if (trail.isEmpty()) return;
            var points = new ArrayList<>(trail);
            if (age < lifetime) points.add(new TrailPoint(world(previous.lerp(position, partial), currentAnchor, currentRotation), age));
            if (points.size() < 2) return;
            double total = 0;
            for (int i = 1; i < points.size(); i++) total += points.get(i - 1).position.distanceTo(points.get(i).position);
            if (total < 1.0E-6) return;
            float width = definition.trail == null ? Math.max(definition.size[0], definition.size[1]) * .65F
                    : definition.trail.width * (definition.trail.sizeAffectsWidth
                    ? Math.max(definition.size[0], Math.max(definition.size[1], definition.size[2])) : 1.0F);
            Vector4f inherited = definition.trail == null ? new Vector4f(color)
                    : SpellEmitterPresets.gradient(definition.trail.lifetimeColor,
                    Math.min(1, (age + partial) / lifetime), trailRandom);
            if (definition.trail != null && definition.trail.inheritColor) inherited.mul(color);
            if (filamentStyle) inherited.w *= Math.max(0, 1 - Math.max(0, age + partial - lifetime) / 8);
            double length = 0;
            for (int i = 1; i < points.size(); i++) {
                Vec3 start = points.get(i - 1).position, end = points.get(i).position;
                float u0 = (float) (length / total);
                length += start.distanceTo(end);
                float u1 = (float) (length / total);
                Vector4f c0 = definition.trail == null ? inherited : SpellEmitterPresets.gradient(definition.trail.color, u0, trailRandom).mul(inherited);
                Vector4f c1 = definition.trail == null ? inherited : SpellEmitterPresets.gradient(definition.trail.color, u1, trailRandom).mul(inherited);
                strip(vertices, matrix, camera, start, end, width, u0, u1, c0, c1, filamentStyle);
            }
        }
    }

    private record TrailPoint(Vec3 position, int tick) {}

    private static void strip(VertexConsumer vertices, Matrix4f matrix, Camera camera,
                              Vec3 start, Vec3 end, float width, float u0, float u1,
                              Vector4f c0, Vector4f c1, boolean filamentStyle) {
        if (filamentStyle) {
            PhotonSpellStyle.strip(vertices, matrix, camera.getPosition(), start, end, width, u0, u1, c0, c1);
            return;
        }
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-10D) return;
        Vec3 side = start.subtract(camera.getPosition()).cross(direction);
        if (side.lengthSqr() < 1.0E-10D) side = SpellEmitterPresets.rotate(new Vec3(1, 0, 0), camera.rotation());
        side = side.normalize().scale(width);
        start = start.subtract(camera.getPosition());
        end = end.subtract(camera.getPosition());
        vertex(vertices, matrix, start.subtract(side), u0, 0, c0);
        vertex(vertices, matrix, start.add(side), u0, 1, c0);
        vertex(vertices, matrix, end.add(side), u1, 1, c1);
        vertex(vertices, matrix, end.subtract(side), u1, 0, c1);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point, float u, float v, Vector4f color) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setUv(u, v).setColor(color.x, color.y, color.z, color.w * .20F);
    }
}
