package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.CombatImpactShapes;
import com.example.immortal_cultivation_mod.effect.CombatImpactShapes.Basis;
import com.example.immortal_cultivation_mod.effect.CombatImpactShapes.Stage;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Effect;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Kind;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Short, anchored combat feedback. Geometry is batched, depth tested and never creates world entities. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class CombatImpactVfx {
    private static final int MAX_IMPACTS = 48;
    private static final int MAX_QUADS = 12000;
    private static final List<Impact> IMPACTS = new ArrayList<>();
    private static final List<Particle> SMOKE = new ArrayList<>();
    private static ClientLevel world;
    private static int renderedQuads;
    private static long admittedTick = Long.MIN_VALUE;
    private static int admittedCount;
    private static int smokeThisTick;
    static final RenderType MATTER = RenderType.create("cultivation_impact_matter",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 65536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("impact_soft_alpha", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(770, 771, 0, 1);
                    }, () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(FilamentVfx.WORLD_TARGET)
                    .createCompositeState(false));

    private static final class Impact {
        final Effect data;
        final long born;
        final Basis basis;
        final Vec3 floor;
        final Vec3[] dust;
        final List<Vec3[]> cracks;
        final Vec3[][] groundRings;
        final float targetWidth;
        final int groundColor;

        Impact(Effect data, long born, int quality) {
            this.data = data;
            this.born = born;
            basis = CombatImpactShapes.basis(data.direction());
            var subject = world.getEntity(data.subjectId());
            targetWidth = subject == null ? .6F : Math.min(2, subject.getBbWidth());
            floor = findFloor(data.position(), 2.6);
            int color = floor == null ? 0x827866 : world.getBlockState(BlockPos.containing(floor.add(0, -.08, 0)))
                    .getMapColor(world, BlockPos.containing(floor.add(0, -.08, 0))).col;
            groundColor = color == 0 ? 0x827866 : color;
            int count = floor == null ? 0 : quality == 0 ? 3 : quality == 1 ? 6 : 10;
            dust = new Vec3[count];
            cracks = new ArrayList<>();
            groundRings = floor != null && data.kind() == Kind.LANDING ? new Vec3[6][24] : new Vec3[0][];
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2 * (i + random(i) * .3) / count;
                double radius = groundRadius() * (.55 + random(i + 30) * .45);
                dust[i] = findFloor(floor.add(Math.cos(angle) * radius, .8, Math.sin(angle) * radius), 2);
            }
            if (floor != null && (data.kind() == Kind.LANDING || data.intensity() >= .8F)) {
                int branches = quality == 0 ? 2 : quality == 1 ? 4 : 6;
                for (int i = 0; i < branches; i++) {
                    double angle = i * Math.PI * 2 / branches + random(i + 80) * .55;
                    Vec3[] path = new Vec3[4];
                    path[0] = floor;
                    for (int j = 1; j < path.length; j++) {
                        double turn = angle + (random(i * 7 + j + 140) - .5) * .48;
                        double reach = groundRadius() * j / 4.5;
                        path[j] = findFloor(floor.add(Math.cos(turn) * reach, .65, Math.sin(turn) * reach), 1.7);
                    }
                    cracks.add(path);
                }
            }
            // Terrain probes are done once on receipt, never once per rendered frame.
            for (int ring = 0; ring < groundRings.length; ring++) {
                double radius = .12 + groundRadius() * ring / (groundRings.length - 1D);
                for (int segment = 0; segment < 24; segment++) {
                    double angle = segment * Math.PI * 2 / 24;
                    groundRings[ring][segment] = findFloor(floor.add(Math.cos(angle) * radius, .25,
                            Math.sin(angle) * radius), .65);
                }
            }
        }

        double random(int index) { return CombatImpactShapes.variation(data.seed(), index); }
        double groundRadius() {
            return switch (data.kind()) {
                case DASH_START, DASH_STOP -> .65 + data.intensity() * .8;
                case LANDING -> .7 + data.intensity() * 4;
                default -> data.intensity() >= .45F ? 1.1 + data.intensity() * 3.1 : .45;
            };
        }
        int lifetime() {
            if (data.kind() == Kind.BLOCK) return 10;
            if (data.kind() == Kind.DASH_START || data.kind() == Kind.DASH_STOP) return 14;
            return data.intensity() >= .8F ? 34 : data.intensity() >= .45F ? 22 : 12;
        }
    }

    private CombatImpactVfx() {}

    private static boolean useLevel(ClientLevel level) {
        if (world != level) {
            IMPACTS.clear();
            SMOKE.forEach(Particle::remove);
            SMOKE.clear();
            world = level;
            admittedTick = Long.MIN_VALUE;
            admittedCount = 0;
            renderedQuads = 0;
        }
        return level != null;
    }

    public static void accept(Effect packet) {
        Minecraft mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || !world.dimension().location().equals(packet.dimension())
                || !CombatImpactShapes.finite(packet.position()) || !CombatImpactShapes.finite(packet.direction())
                || !Float.isFinite(packet.intensity()) || mc.player == null
                || packet.position().distanceToSqr(mc.player.position()) > 80 * 80) return;
        long now = world.getGameTime();
        if (admittedTick != now) { admittedTick = now; admittedCount = 0; smokeThisTick = 0; }
        int quality = quality();
        if (admittedCount >= (quality == 0 ? 3 : quality == 1 ? 6 : 10)) return;
        // Duplicate/overlapping packets must not turn a single contact into repeated bright flashes.
        for (Impact old : IMPACTS) {
            if (now - old.born < 2 && old.data.subjectId() == packet.subjectId()
                    && old.data.kind() == packet.kind()
                    && old.data.position().distanceToSqr(packet.position()) < .25) return;
        }
        if (IMPACTS.size() >= MAX_IMPACTS) IMPACTS.removeFirst();
        Impact impact = new Impact(packet, now, quality);
        IMPACTS.add(impact);
        if (packet.kind() == Kind.HIT) spawnCombatSmoke(impact, quality);
        admittedCount++;
    }

    private static void spawnCombatSmoke(Impact impact, int quality) {
        SMOKE.removeIf(particle -> !particle.isAlive());
        float strength = Mth.clamp(impact.data.intensity(), 0, 1);
        boolean heavy = com.example.immortal_cultivation_mod.combat.CombatFeedbackProfile.heavy(strength);
        double plumeScale = heavy ? 1.25 : 1;
        int liveLimit = quality == 0 ? 24 : quality == 1 ? 64 : 128;
        int tickLimit = quality == 0 ? 4 : quality == 1 ? 12 : 24;
        int count = quality == 0 ? 2 : (int) ((5 + strength * 11) * (quality == 1 ? .6F : 1));
        count = Math.min(count, Math.min(liveLimit - SMOKE.size(), tickLimit - smokeThisTick));
        Vec3 origin = impact.floor == null ? impact.data.position() : impact.floor.add(0, .15, 0);
        Vec3 push = impact.data.direction().multiply(.025, 0, .025);
        for (int i = 0; i < count; i++) {
            // Offset lobes form a rising column instead of one opaque cloud over the target.
            double angle = impact.random(700 + i) * Math.PI * 2;
            double radius = (.15 + impact.random(740 + i) * .35) * (.6 + strength) * plumeScale;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    origin.x + dx * radius, origin.y + impact.random(780 + i) * (.35 + strength * 2.3) * plumeScale,
                    origin.z + dz * radius, push.x + dx * .012,
                    .025 + strength * .035 + impact.random(820 + i) * .015, push.z + dz * .012);
            if (particle == null) continue;
            float gray = (float) (.55 + impact.random(860 + i) * .18);
            particle.setColor(gray, gray, gray);
            particle.scale((float) ((1 + strength * 1.2 + impact.random(900 + i) * .4) * (heavy ? 1.5 : 1)));
            particle.setLifetime(70 + (int) (strength * 20 + impact.random(940 + i) * 10) + (heavy ? 30 : 0));
            SMOKE.add(particle);
            smokeThisTick++;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || mc.isPaused()) return;
        long now = world.getGameTime();
        IMPACTS.removeIf(impact -> now - impact.born >= impact.lifetime());
        SMOKE.removeIf(particle -> !particle.isAlive());
    }

    public static int activeCount() { return IMPACTS.size(); }
    public static int renderedQuads() { return renderedQuads; }

    private static int quality() {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return status == ParticleStatus.MINIMAL ? 0 : status == ParticleStatus.DECREASED ? 1 : 2;
    }

    private static Vec3 findFloor(Vec3 point, double reach) {
        if (!world.hasChunkAt(BlockPos.containing(point))) return null;
        var hit = world.clip(new ClipContext(point.add(0, .08, 0), point.add(0, -reach, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection().getStepY() > 0
                ? hit.getLocation().add(0, .018, 0) : null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ShaderPackCompatibility.isRenderingShadowPass()) return;
        renderedQuads = 0;
        Minecraft mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || IMPACTS.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        double now = world.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int quality = quality();
        int limit = quality == 0 ? 6 : quality == 1 ? 12 : 20;
        List<Impact> visible = IMPACTS.stream()
                .filter(impact -> impact.data.position().distanceToSqr(camera) < 64 * 64)
                .sorted(Comparator.comparingDouble(impact -> impact.data.position().distanceToSqr(camera)))
                .limit(limit).toList();
        Vec3 right = new Vec3(new Vector3f(1, 0, 0).rotate(event.getCamera().rotation()));
        Vec3 up = new Vec3(new Vector3f(0, 1, 0).rotate(event.getCamera().rotation()));
        var buffers = mc.renderBuffers().bufferSource();
        Matrix4f matrix = event.getPoseStack().last().pose();
        Mesh matter = new Mesh(buffers.getBuffer(MATTER), matrix, camera, right, up);
        // Translucent dust renders back to front. Additive light follows both dust and solid debris.
        for (int i = visible.size() - 1; i >= 0; i--) drawMatter(visible.get(i), now - visible.get(i).born, matter, quality);
        buffers.endBatch(MATTER);
        Mesh glow = new Mesh(buffers.getBuffer(FilamentVfx.TYPE), matrix, camera, right, up);
        for (Impact impact : visible) drawGlow(impact, now - impact.born, glow, quality);
        buffers.endBatch(FilamentVfx.TYPE);
    }

    private static void drawGlow(Impact hit, double age, Mesh mesh, int quality) {
        if (age < 0 || age >= hit.lifetime()) return;
        Effect data = hit.data;
        float strength = data.intensity();
        Vec3 center = data.position();
        boolean block = data.kind() == Kind.BLOCK;
        if (data.kind() == Kind.DASH_START || data.kind() == Kind.DASH_STOP) return;
        if (data.kind() == Kind.LANDING) {
            double shockAge = CombatImpactShapes.elapsed(Stage.SHOCKWAVE, strength, age);
            if (hit.floor != null && shockAge >= 0 && shockAge < 10) {
                double radius = .12 + hit.groundRadius() * ease(shockAge / 10);
                groundRing(hit, mesh, radius, .055 + strength * .025, (float) (1 - shockAge / 10) * .24F);
            }
            return;
        }

        int color = color(data.injuryFlags(), block);
        if (age < 1.6) {
            double size = block ? .26 : .10 + strength * .27;
            mesh.halo(center, size, 0xfff5d5, (float) (1 - age / 1.6) * .78F);
        }
        double sparkLife = block ? 7 : 5;
        if (age < sparkLife) {
            int count = quality == 0 ? 2 : block ? 7 : 3 + Math.round(strength * 4);
            for (int i = 0; i < count; i++) {
                double angle = hit.random(i + 210) * Math.PI * 2;
                Vec3 sideways = hit.basis.right().scale(Math.cos(angle)).add(hit.basis.up().scale(Math.sin(angle)));
                Vec3 trajectory = sideways.scale(.10 + hit.random(i + 240) * .10)
                        .add(hit.basis.forward().scale(block ? -.025 : .05 + strength * .05));
                Vec3 head = center.add(trajectory.scale(age + .12)).add(0, -.006 * age * age, 0);
                Vec3 tail = head.subtract(trajectory.scale(.5 + strength));
                mesh.line(tail, head, .014, color, .65F * (float) (1 - age / sparkLife), 0);
            }
        }
        if (block) {
            if (age < 7) {
                // A semicircle, deliberately open on one side, distinguishes a block from an explosion.
                arc(mesh, center, hit.basis, .20 + age * .052, -.5 * Math.PI, .5 * Math.PI,
                        .033, 0xffdb83, (float) (1 - age / 7) * .65F, quality == 0 ? 8 : 16);
            }
            if (age < 4) {
                Vec3 weaponAxis = hit.basis.up().add(hit.basis.right().scale(.42)).normalize();
                mesh.line(center.subtract(weaponAxis.scale(.30)), center.add(weaponAxis.scale(.30)),
                        .045, 0xffdf92, (float) (1 - age / 4) * .35F, (float) (1 - age / 4) * .1F);
                Vec3 recoil = center.subtract(hit.basis.forward().scale(age * .035));
                mesh.line(center, recoil.add(hit.basis.right().scale(.035)), .025, color,
                        (float) (1 - age / 4) * .45F, 0);
            }
            return;
        }
        if (age < 4) {
            Vec3 slash = hit.basis.up().scale(.82).add(hit.basis.right().scale(.58));
            double length = .15 + strength * .35;
            mesh.line(center.subtract(slash.scale(length)), center.add(slash.scale(length)),
                    .018 + strength * .018, color, (float) (1 - age / 4) * .72F, 0);
        }
        if (strength < .45F && data.kind() != Kind.SHIELD_BREAK) return;
        double shockAge = CombatImpactShapes.elapsed(Stage.SHOCKWAVE, strength, age);
        if (shockAge >= 0 && shockAge < 12) {
            double radius = .2 + (1 + strength * 2.5) * ease(shockAge / 12);
            arc(mesh, center, hit.basis, radius, 0, Math.PI * 2,
                    .035 + strength * .04, color, (float) (1 - shockAge / 12) * .58F, quality == 0 ? 16 : 32);
            if (shockAge < 6) {
                int rays = quality == 0 ? 3 : 6;
                Vec3 rear = CombatImpactShapes.behind(center, hit.basis, hit.targetWidth, 0);
                for (int i = 0; i < rays; i++) {
                    double angle = i * Math.PI * 2 / rays + hit.random(i + 300) * .25;
                    Vec3 sideways = hit.basis.right().scale(Math.cos(angle)).add(hit.basis.up().scale(Math.sin(angle)));
                    Vec3 tip = CombatImpactShapes.behind(center, hit.basis, hit.targetWidth,
                            .3 + ease(shockAge / 6) * (1.4 + strength))
                            .add(sideways.scale(.15 + shockAge * .08));
                    mesh.line(rear, tip, .028 + strength * .025, color,
                            (float) (1 - shockAge / 6) * .42F, 0);
                }
            }
        }
        double fallingAge = CombatImpactShapes.elapsed(Stage.FALLING_ENERGY, strength, age);
        if (strength >= .8F && fallingAge >= 0 && fallingAge < 26 && quality > 0) {
            for (int i = 0; i < (quality == 1 ? 4 : 7); i++) {
                double angle = hit.random(i + 350) * Math.PI * 2;
                double radius = .5 + hit.random(i + 370) * 1.6;
                Vec3 spark = center.add(Math.cos(angle) * radius,
                        1.3 + hit.random(i + 390) * .8 - fallingAge * .055, Math.sin(angle) * radius);
                mesh.line(spark.add(0, .07, 0), spark, .013, color,
                        (float) (1 - fallingAge / 26) * .48F, 0);
            }
        }
    }

    private static void drawMatter(Impact hit, double age, Mesh mesh, int quality) {
        if (age < 0 || age >= hit.lifetime() || hit.data.kind() == Kind.BLOCK) return;
        float strength = hit.data.intensity();
        boolean dash = hit.data.kind() == Kind.DASH_START || hit.data.kind() == Kind.DASH_STOP;
        if (hit.floor == null) {
            if (hit.data.kind() == Kind.HIT && age < 6) {
                for (int i = 0; i < (quality == 0 ? 1 : 3); i++) {
                    Vec3 point = hit.data.position().add(hit.basis.right().scale((i - 1) * age * .023))
                            .add(hit.basis.forward().scale(age * .016)).add(0, age * .01, 0);
                    mesh.halo(point, .07 + age * .012, 0xb9ae96, (float) (1 - age / 6) * .14F);
                }
            }
            return;
        }
        double dustAge = dash ? age : CombatImpactShapes.elapsed(Stage.DUST, strength, age);
        double dustLife = strength >= .8F ? 24 : dash ? 12 : 13;
        if (dustAge >= 0 && dustAge < dustLife) {
            float alpha = (float) (Math.min(1, (dustAge + .5) / 2) * (1 - dustAge / dustLife));
            for (int i = 0; i < hit.dust.length; i++) {
                Vec3 ground = hit.dust[i];
                if (ground == null) continue;
                double progress = ease(Math.min(1, dustAge / (dash ? 5 : 9)));
                Vec3 center = hit.floor.lerp(ground, progress).add(0, .07 + dustAge * .014, 0);
                // Never bridge a cliff with a suspended dust bank.
                if (Math.abs(ground.y - hit.floor.y) > .65) continue;
                double size = (dash ? .16 : .10 + strength * .25) + dustAge * .018;
                mesh.halo(center, size, hit.groundColor, alpha * (strength >= .45F ? .24F : .18F));
            }
        }
        double crackAge = CombatImpactShapes.elapsed(Stage.CRACK, strength, age);
        if (crackAge >= 0 && crackAge < 24) {
            float fade = (float) Math.min(1, (hit.lifetime() - age) / 6);
            for (Vec3[] branch : hit.cracks) {
                double length = Math.min(3, crackAge * 1.5 + .4);
                for (int i = 0; i < 3 && i < length; i++) {
                    Vec3 a = branch[i], b = branch[i + 1];
                    if (a == null || b == null || Math.abs(a.y - b.y) > .4) continue;
                    groundStroke(mesh, a, a.lerp(b, Math.min(1, length - i)),
                            (.013 + strength * .021) * (1 - i * .2), 0x252124, fade * .82F);
                }
            }
        }
        double debrisAge = CombatImpactShapes.elapsed(Stage.DEBRIS, strength, age);
        if (dash || strength < .45F || debrisAge < 0 || debrisAge >= 18 || quality == 0) return;
        int count = quality == 1 ? 3 : 6;
        for (int i = 0; i < count; i++) {
            double angle = hit.random(i + 420) * Math.PI * 2;
            double horizontal = (.025 + hit.random(i + 440) * .040) * debrisAge;
            double height = (.12 + strength * .10 + hit.random(i + 460) * .07) * debrisAge
                    - .016 * debrisAge * debrisAge;
            if (height < 0) continue;
            Vec3 pos = hit.floor.add(Math.cos(angle) * horizontal, height + .06, Math.sin(angle) * horizontal);
            mesh.rock(pos, .035 + hit.random(i + 480) * .065, hit.groundColor,
                    (float) Math.min(1, (18 - debrisAge) / 4), debrisAge * .2 + angle);
        }
    }

    private static void groundRing(Impact hit, Mesh mesh, double radius, double width, float alpha) {
        // Ground rings are only drawn on the confirmed local surface; uneven edges are intentionally broken.
        if (hit.groundRings.length == 0) return;
        int segments = hit.groundRings[0].length;
        double sample = Mth.clamp((radius - .12) / hit.groundRadius(), 0, 1) * (hit.groundRings.length - 1);
        int low = (int) sample, high = Math.min(low + 1, hit.groundRings.length - 1);
        for (int i = 0; i < segments; i++) {
            Vec3 a = hit.groundRings[low][i], b = hit.groundRings[high][i];
            Vec3 c = hit.groundRings[low][(i + 1) % segments], d = hit.groundRings[high][(i + 1) % segments];
            if (a == null || b == null || c == null || d == null
                    || Math.abs(a.y - b.y) > .25 || Math.abs(c.y - d.y) > .25) continue;
            Vec3 p = a.lerp(b, sample - low), q = c.lerp(d, sample - low);
            if (p != null && q != null && Math.abs(p.y - q.y) < .25) {
                mesh.line(p.add(0, .01, 0), q.add(0, .01, 0), width, 0xc9c4b0, alpha, alpha);
            }
        }
    }

    private static void groundStroke(Mesh mesh, Vec3 a, Vec3 b, double width, int color, float alpha) {
        Vec3 delta = b.subtract(a);
        if (delta.lengthSqr() < .0001) return;
        Vec3 side = new Vec3(-delta.z, 0, delta.x).normalize().scale(width);
        mesh.quad(a.add(side), a.subtract(side), b.subtract(side), b.add(side), color, alpha);
    }

    private static void arc(Mesh mesh, Vec3 center, Basis basis, double radius, double from, double to,
                            double width, int color, float alpha, int segments) {
        if (alpha <= .001) return;
        for (int i = 0; i < segments; i++) {
            double a = Mth.lerp(i / (double) segments, from, to);
            double b = Mth.lerp((i + 1D) / segments, from, to);
            Vec3 p = CombatImpactShapes.ringPoint(center, basis, radius, a);
            Vec3 q = CombatImpactShapes.ringPoint(center, basis, radius, b);
            mesh.line(p, q, width, color, alpha, alpha);
        }
    }

    private static double ease(double value) { return 1 - Math.pow(1 - Mth.clamp(value, 0, 1), 2); }

    private static int color(int flags, boolean block) {
        if (block) return 0xffdf96;
        if ((flags & CombatFeedbackPayloads.FIRE) != 0) return 0xffb35b;
        if ((flags & CombatFeedbackPayloads.FROST) != 0) return 0x98dce9;
        if ((flags & CombatFeedbackPayloads.LIGHTNING) != 0) return 0xa8bcff;
        if ((flags & CombatFeedbackPayloads.AURA) != 0) return 0x87dcdd;
        return 0xe6d5aa;
    }

    private record Mesh(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 right, Vec3 up) {
        void line(Vec3 a, Vec3 b, double width, int color, float alphaA, float alphaB) {
            if (renderedQuads + 6 > MAX_QUADS) return;
            renderedQuads += FilamentVfx.ribbon(out, matrix, a, b, camera, width,
                    red(color), green(color), blue(color), alphaA, alphaB);
        }
        void halo(Vec3 center, double radius, int color, float alpha) {
            if (renderedQuads + 16 > MAX_QUADS) return;
            renderedQuads += FilamentVfx.halo(out, matrix, center, camera, right, up,
                    radius, red(color), green(color), blue(color), alpha);
        }
        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
            if (renderedQuads >= MAX_QUADS) return;
            vertex(a, color, alpha); vertex(b, color, alpha); vertex(c, color, alpha); vertex(d, color, alpha);
            renderedQuads++;
        }
        void vertex(Vec3 p, int color, float alpha) {
            VfxVertexWriter.vertex(out, matrix, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z))
                    .setColor(red(color), green(color), blue(color), Mth.clamp(alpha, 0, 1));
        }
        void rock(Vec3 center, double size, int color, float alpha, double rotation) {
            Vec3 x = new Vec3(Math.cos(rotation), .18 * Math.sin(rotation), Math.sin(rotation)).scale(size);
            Vec3 z = new Vec3(-Math.sin(rotation), 0, Math.cos(rotation)).scale(size * .8);
            Vec3 y = new Vec3(0, size, 0);
            Vec3 a = center.subtract(x).subtract(z), b = center.add(x).subtract(z);
            Vec3 c = center.add(x).add(z), d = center.subtract(x).add(z);
            quad(a.add(y), b.add(y), c.add(y), d.add(y), color, alpha);
            quad(a.subtract(y), a.add(y), b.add(y), b.subtract(y), shade(color, .65), alpha);
            quad(b.subtract(y), b.add(y), c.add(y), c.subtract(y), shade(color, .8), alpha);
            quad(c.subtract(y), c.add(y), d.add(y), d.subtract(y), shade(color, .6), alpha);
            quad(d.subtract(y), d.add(y), a.add(y), a.subtract(y), shade(color, .7), alpha);
        }
        private static float red(int color) { return (color >> 16 & 255) / 255F; }
        private static float green(int color) { return (color >> 8 & 255) / 255F; }
        private static float blue(int color) { return (color & 255) / 255F; }
        private static int shade(int color, double amount) {
            return (int) ((color >> 16 & 255) * amount) << 16
                    | (int) ((color >> 8 & 255) * amount) << 8 | (int) ((color & 255) * amount);
        }
    }
}
