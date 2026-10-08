package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.SpellVfxPayloads;
import com.example.immortal_cultivation_mod.spell.Weiya;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Steady oppressive pressure. Geometry is bounded; no explosions or terrain edits. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class RealmPressureClient {
    private static final int MAX_SOURCES = 32, MAX_VISIBLE = 8;
    private static final Map<Long, Field> FIELDS = new HashMap<>();
    private static final List<Field> VISIBLE = new ArrayList<>();
    private static ClientLevel world;
    private static int particleBudget;
    private static float exposure;
    private static final double[][] STONE_POINTS = {{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},
            {-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] STONE_FACES = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
    private static final RenderType MATTER = RenderType.create("cultivation_pressure_matter",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 32768, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(FilamentVfx.WORLD_TARGET).createCompositeState(false));

    private RealmPressureClient() {}
    private static final class Field {
        Vec3 center;
        int stage;
        long until, born, pulse;
        final int owner;
        final List<Vec3> ground = new ArrayList<>();
        Vec3 groundAnchor = Vec3.ZERO;
        Field(Vec3 center, int stage, long now, long until, int owner) {
            this.center = center; this.stage = stage; this.born = now; this.until = until; this.owner = owner; this.pulse = now;
        }
        void stage(int value, long now) {
            if (value > stage && now - pulse >= 20) pulse = now;
            stage = value;
        }
        double radius() { return Weiya.radiusForStage(stage); }
        float fade(double now) { return Mth.clamp((float) ((now - born) / 30.0), 0, 1); }
    }

    private static boolean useWorld() {
        ClientLevel level = Minecraft.getInstance().level;
        if (world != level) {
            FIELDS.clear(); VISIBLE.clear(); exposure = 0; particleBudget = 0; world = level;
        }
        return level != null;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        FIELDS.clear(); VISIBLE.clear(); exposure = 0; particleBudget = 0; world = null;
    }

    public static void accept(SpellVfxPayloads.Visual payload) {
        if (!useWorld() || !world.dimension().location().equals(payload.dimension())) return;
        long key = BlockPos.containing(payload.position()).asLong();
        if (payload.duration() <= 0) { FIELDS.remove(key); return; }
        if (FIELDS.size() >= MAX_SOURCES && !FIELDS.containsKey(key)) return;
        long now = world.getGameTime();
        Field field = FIELDS.computeIfAbsent(key, ignored -> new Field(payload.position(),
                Mth.clamp(payload.strength(), 1, 27), now, now + 30, -1));
        field.center = payload.position(); field.stage(Mth.clamp(payload.strength(), 1, 27), now);
        field.until = now + Mth.clamp(payload.duration(), 1, 40);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!useWorld()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        long now = world.getGameTime();
        var activeEntities = new HashSet<Integer>();
        for (Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || living.distanceToSqr(mc.player) > 128 * 128) continue;
            var pressure = living.getEffect(ModEffects.WEIYA);
            if (pressure == null) continue;
            activeEntities.add(entity.getId());
            long key = Long.MIN_VALUE + entity.getId();
            if (FIELDS.size() >= MAX_SOURCES && !FIELDS.containsKey(key)) continue;
            Field field = FIELDS.computeIfAbsent(key, ignored -> new Field(entity.position(),
                    Math.max(1, pressure.getAmplifier()), now, now + 2, entity.getId()));
            field.center = entity.position(); field.stage(Mth.clamp(pressure.getAmplifier(), 1, 27), now);
            field.until = now + 2;
        }
        FIELDS.values().removeIf(field -> field.until <= now
                || field.owner >= 0 && !activeEntities.contains(field.owner));
        VISIBLE.clear();
        FIELDS.values().stream().sorted(Comparator.comparingDouble(f -> f.center.distanceToSqr(mc.player.position())))
                .limit(MAX_VISIBLE).forEach(VISIBLE::add);
        particleBudget = 128;
        float target = strengthAt(mc.player.getEyePosition());
        exposure += (target - exposure) * .14F;
        for (Field field : VISIBLE) {
            if (field.ground.isEmpty() || field.center.distanceToSqr(field.groundAnchor) > 1) findGround(field);
            if ((now & 3) != 0) continue;
            // Two dust motes per field every four ticks; population never scales with radius.
            for (int i = 0; i < 1 + field.stage / 9 && !field.ground.isEmpty(); i++) {
                Vec3 p = field.ground.get(world.random.nextInt(field.ground.size()));
                Vec3 outward = p.subtract(field.center).multiply(1, 0, 1).normalize().scale(-.012 - field.stage * .0006);
                world.addParticle(com.example.immortal_cultivation_mod.particle.ModParticles.AMBIENT_ASH.get(),
                        p.x, p.y + .045, p.z, outward.x, -.025, outward.z);
            }
        }
    }

    private static void findGround(Field field) {
        field.ground.clear(); field.groundAnchor = field.center;
        for (int i = 0; i < 10 + field.stage / 4; i++) {
            double angle = i * 2.399963, distance = .35 + Math.sqrt(i) * .65;
            Vec3 p = field.center.add(Math.cos(angle) * distance, 1, Math.sin(angle) * distance);
            if (!world.hasChunkAt(BlockPos.containing(p))) continue;
            var hit = world.clip(new ClipContext(p, p.add(0, -5, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, Minecraft.getInstance().player));
            if (hit.getType() == HitResult.Type.BLOCK) field.ground.add(hit.getLocation().add(0, .014, 0));
        }
    }

    /** Camera effects consume this smoothed value, never continuous camera shaking. */
    public static float exposure() { return useWorld() ? exposure : 0; }

    /** A short impulse only on activation/increase, never sustained camera shake. */
    public static float pulseAt(Vec3 point, double partial) {
        if (!useWorld()) return 0;
        float value = 0;
        for (Field field : VISIBLE) {
            if (!Weiya.contains(field.center, field.radius(), point)) continue;
            double age = world.getGameTime() + partial - field.pulse;
            if (age >= 0 && age < 9)
                value = Math.max(value, (float)(Math.sin(age * 2.2) * Math.pow(1-age/9,2))
                        * (.15F + field.stage/36F));
        }
        return value;
    }


    public static float strengthAt(Vec3 point) {
        if (!useWorld()) return 0;
        float result = 0;
        for (Field field : FIELDS.values()) {
            double distance = field.center.distanceTo(point), radius = field.radius();
            if (distance <= radius) result = Math.max(result,
                    (float) ((.34 + field.stage * .024) * Math.min(1, (radius - distance) / 3)));
        }
        return Mth.clamp(result, 0, 1);
    }

    public static Vec3 windAt(Vec3 point) {
        if (!useWorld()) return Vec3.ZERO;
        Vec3 result = Vec3.ZERO;
        for (Field field : VISIBLE) {
            if (!Weiya.contains(field.center, field.radius(), point)) continue;
            Vec3 outward = point.subtract(field.center).multiply(1, 0, 1);
            if (outward.lengthSqr() > .0001) result = result.add(outward.normalize().scale(-.025 - field.stage * .0015));
        }
        return result.lengthSqr() > .015 ? result.normalize().scale(.12) : result;
    }

    public static double rainCeiling(double x, double z) {
        if (!useWorld()) return Double.NEGATIVE_INFINITY;
        double top = Double.NEGATIVE_INFINITY;
        for (Field field : FIELDS.values()) top = Math.max(top, Weiya.rainCeiling(field.center, field.radius(), x, z));
        return top;
    }

    public static boolean suppressRainAt(Vec3 point) {
        return point.y <= rainCeiling(point.x, point.z);
    }

    public static void pushParticle(Particle particle) {
        if (particleBudget <= 0 || VISIBLE.isEmpty()) return;
        // Bound existing-particle work even in dense spell fights; positions remain visual only.
        particleBudget--;
        Vec3 wind = windAt(particle.getPos());
        if (wind.lengthSqr() > .00001) particle.move(wind.x * .22, -.018 - strengthAt(particle.getPos()) * .022, wind.z * .22);
    }

    public static float suppression(LivingEntity entity) {
        var effect = entity.getEffect(ModEffects.WEIYA_SUPPRESSED);
        return effect == null || !entity.isAlive() ? 0 : Mth.clamp((effect.getAmplifier() + 1) / 3F, 0, 1);
    }

    private static boolean weakProjectile(Projectile projectile, Field field) {
        Entity owner = projectile.getOwner();
        if (owner == null || owner.getId() == field.owner) return false;
        // Remote NPC profiles are server-only. An unsynced player/NPC must never be guessed mortal.
        int stage = -1;
        if (owner == Minecraft.getInstance().player)
            stage = CultivationLevels.getStageIndex(ClientData.cultivationData.cultivationLevel());
        else if (owner instanceof LivingEntity living) {
            var pressure = living.getEffect(ModEffects.WEIYA);
            if (pressure != null && pressure.getAmplifier() > 0) stage = pressure.getAmplifier();
        }
        return stage >= 0 && stage < field.stage
                && Weiya.contains(field.center, field.radius(), projectile.position());
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ShaderPackCompatibility.isRenderingShadowPass() || !useWorld() || VISIBLE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        double now = world.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f matrix = event.getPoseStack().last().pose();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer air = buffers.getBuffer(FilamentVfx.TYPE);
        for (Field field : VISIBLE) {
            float fade = field.fade(now);
            // Broken, slow air contours. No complete bright bubble, flashing ring, or explosive expansion.
            for (int arc = 0; arc < 5; arc++) {
                double phase = ((now - field.born) * .012 + arc * .2) % 1;
                double radius = .45 + (1-phase) * (2.2 + field.stage * .065);
                double angle = arc * 1.73 + now * .006;
                for (int segment = 0; segment < 12; segment++) {
                    double a = angle + segment * .055, b = a + .055;
                    Vec3 p = field.center.add(Math.cos(a) * radius, .65 + arc * .36, Math.sin(a) * radius);
                    Vec3 q = field.center.add(Math.cos(b) * radius, .65 + arc * .36 + .015, Math.sin(b) * radius);
                    float alpha = fade * (.025F + field.stage * .002F) * (float) Math.sin(segment * Math.PI / 12);
                    FilamentVfx.ribbon(air, matrix, p, q, camera, .025, .63F, .68F, .76F, alpha, alpha);
                }
            }
            double pulseAge = now - field.pulse;
            if (pulseAge >= 0 && pulseAge < 10) {
                double descent = 1 - pulseAge / 10;
                double r = 1.2 + field.stage * .055;
                for (int segment = 0; segment < 32; segment++) {
                    double a = segment * Math.PI / 16, b = (segment+1) * Math.PI / 16;
                    Vec3 p = field.center.add(Math.cos(a)*r, descent*2.2+.06, Math.sin(a)*r);
                    Vec3 q = field.center.add(Math.cos(b)*r, descent*2.2+.06, Math.sin(b)*r);
                    float opacity = (float)Math.sin(pulseAge/10*Math.PI) * (.025F+field.stage*.0015F);
                    FilamentVfx.ribbon(air,matrix,p,q,camera,.035,.42F,.45F,.5F,opacity,opacity);
                }
            }
        }
        int unstable = 0;
        for (Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile) || entity.distanceToSqr(camera) > 48 * 48) continue;
            if (++unstable > 24) break;
            for (Field field : VISIBLE) if (weakProjectile(projectile, field)) {
                Vec3 p = projectile.getPosition((float) (now % 1));
                Vec3 velocity = projectile.getDeltaMovement().normalize();
                for (int i = 0; i < 3; i++) {
                    Vec3 q = p.subtract(velocity.scale(i * .12)).add(0, Math.sin(now * .8 + i) * .07, 0);
                    FilamentVfx.ribbon(air, matrix, q, q.subtract(velocity.scale(.07)), camera, .025,
                            .56F, .55F, .68F, .12F, 0);
                }
                break;
            }
        }
        buffers.endBatch(FilamentVfx.TYPE);
        VertexConsumer matter = buffers.getBuffer(MATTER);
        for (Field field : VISIBLE) {
            float fade = field.fade(now);
            for (int i = 0; i < field.ground.size(); i++) {
                Vec3 base = field.ground.get(i);
                Vec3 outward = base.subtract(field.center).multiply(1, 0, 1).normalize();
                Vec3 bend = new Vec3(-outward.z, 0, outward.x).scale((i % 2 == 0 ? 1 : -1) * .13);
                Vec3 tip = base.add(outward.scale((.22 + i * .035) * (1 + field.stage * .055) * fade));
                FilamentVfx.ribbon(matter, matrix, base, tip.add(bend), camera, .018,
                        .045F, .036F, .055F, fade * .55F, fade * .25F);
                FilamentVfx.ribbon(matter, matrix, tip.add(bend), tip.add(outward.scale(.16)), camera, .012,
                        .045F, .036F, .055F, fade * .25F, 0);
                double lift = .012;
                cube(matter, matrix, base.add(outward.scale(lift * .2)).add(0, lift, 0).subtract(camera),
                        .035 + (i % 3) * .018 + field.stage * .001, fade * .65F);
            }
        }
        buffers.endBatch(MATTER);
    }

    private static void cube(VertexConsumer out, Matrix4f matrix, Vec3 c, double size, float alpha) {
        for (int face = 0; face < STONE_FACES.length; face++) {
            float shade = face == 4 ? .43F : .20F + face * .024F;
            for (int point : STONE_FACES[face]) {
                double[] p = STONE_POINTS[point];
                out.addVertex(matrix, (float)(c.x+p[0]*size), (float)(c.y+p[1]*size*.12), (float)(c.z+p[2]*size))
                        .setColor(shade, shade * .95F, shade * .90F, alpha);
            }
        }
    }
}
