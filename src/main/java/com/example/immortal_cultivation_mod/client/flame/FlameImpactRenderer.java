package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.CombatCameraVfx;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.flame.ChargedFlameShape;
import com.example.immortal_cultivation_mod.flame.FlameImpactShape;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.network.FlameImpactPayload;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Impact effects outlive their projectile and never modify terrain, entities, or chunk loading. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class FlameImpactRenderer {
    private static final int MAX_ACTIVE = 12, MAX_VISIBLE = 6, SURFACE_SAMPLES = 28;
    private static final Map<UUID, Impact> IMPACTS = new LinkedHashMap<>();
    private static final Map<UUID, Long> SEEN = new LinkedHashMap<>();
    private static ClientLevel world;

    private record Surface(Vec3 point, Vec3 normal) { }
    private record Fragment(Vec3 velocity, float size, float spin) { }

    private static final class Impact {
        final FlameImpactPayload data;
        final FlameKind kind;
        final BlockState debris;
        final long start;
        final List<Surface> surfaces = new ArrayList<>();
        final List<Fragment> fragments = new ArrayList<>();
        int probes;
        boolean resampled;

        Impact(FlameImpactPayload data, long start) {
            this.data = data;
            this.kind = FlameKind.fromId(data.flame());
            this.debris = Block.stateById(data.terrainState());
            this.start = start;
            var random = RandomSource.create(data.seed());
            for (int i = 0; i < 18; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = (.22 + data.radius() * .025) * (.5 + random.nextDouble() * .6);
                fragments.add(new Fragment(new Vec3(Math.cos(angle) * speed,
                        .22 + random.nextDouble() * (.3 + data.radius() * .02), Math.sin(angle) * speed),
                        .12F + random.nextFloat() * Math.min(.5F, data.radius() * .016F), random.nextFloat() * 6));
            }
        }

        float age(float partial) { return world.getGameTime() - start + partial; }
        double distance(Vec3 camera) { return data.center().distanceToSqr(camera); }
    }

    private FlameImpactRenderer() { }

    private static void use(ClientLevel level) {
        if (world == level) return;
        IMPACTS.clear();
        SEEN.clear();
        world = level;
    }

    public static void accept(FlameImpactPayload payload) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || !world.dimension().location().equals(payload.dimension())
                || SEEN.containsKey(payload.impactId())) return;
        long now = world.getGameTime();
        SEEN.put(payload.impactId(), now);
        while (SEEN.size() > 128) SEEN.remove(SEEN.keySet().iterator().next());
        while (IMPACTS.size() >= MAX_ACTIVE) IMPACTS.remove(IMPACTS.keySet().iterator().next());
        IMPACTS.put(payload.impactId(), new Impact(payload, now));
        CombatCameraVfx.flameImpact(payload.center(), payload.radius(), payload.charge());
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { use(null); }

    private static int detail() {
        return Minecraft.getInstance().options.particles().get() == ParticleStatus.ALL ? 2 : 1;
    }

    private static List<Impact> visible(Vec3 camera) {
        var result = new ArrayList<>(IMPACTS.values());
        result.removeIf(impact -> impact.distance(camera) > Math.pow(impact.data.radius() + 128, 2));
        result.sort(Comparator.<Impact>comparingInt(impact -> impact.age(0) < 30 ? 0 : 1)
                .thenComparingDouble(impact -> impact.distance(camera)));
        if (result.size() > MAX_VISIBLE) result.subList(MAX_VISIBLE, result.size()).clear();
        return result;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || mc.isPaused()) return;
        long now = world.getGameTime();
        IMPACTS.values().removeIf(impact -> impact.age(0) >= FlameImpactShape.EXPIRE);
        SEEN.values().removeIf(start -> now - start > FlameImpactShape.EXPIRE + 40);
        int probes = 24;
        for (Impact impact : visible(mc.gameRenderer.getMainCamera().getPosition())) {
            float age = impact.age(0);
            // Chunk updates arrive after the impact event. Sample after the flash, then refresh once
            // the reveal starts, instead of projecting scars onto the old pre-impact surface.
            if (impact.data.terrainDamaged() && age >= 6 && probes > 0) {
                if (age >= 24 && !impact.resampled) {
                    impact.surfaces.clear(); impact.probes = 0; impact.resampled = true;
                }
                int allowance = Math.min(6, probes);
                probes -= sample(impact, allowance);
            }
        }
    }

    private static int sample(Impact impact, int budget) {
        int used = 0;
        Vec3 center = impact.data.center();
        float radius = impact.data.radius();
        while (impact.probes < SURFACE_SAMPLES && used < budget) {
            int index = impact.probes++;
            used++;
            double angle = index * 2.399963;
            double y = index == 0 ? -1 : -.18 - .78 * ((index * .618034) % 1);
            double horizontal = Math.sqrt(1 - y * y);
            Vec3 direction = new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
            Vec3 from = center.add(direction.scale(radius * .35));
            Vec3 to = center.add(direction.scale(radius + 4));
            if (!world.hasChunkAt(BlockPos.containing(from)) || !world.hasChunkAt(BlockPos.containing(to))) continue;
            var hit = world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, Minecraft.getInstance().player));
            if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) continue;
            BlockState state = world.getBlockState(block.getBlockPos());
            if (state.hasBlockEntity() || state.getDestroySpeed(world, block.getBlockPos()) < 0) continue;
            Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
            impact.surfaces.add(new Surface(hit.getLocation().add(normal.scale(.018)).subtract(center), normal));
        }
        return used;
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ShaderPackCompatibility.isRenderingShadowPass()) return;
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || IMPACTS.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        var poses = event.getPoseStack();
        var out = buffers.getBuffer(FlameRenderTypes.energy());
        for (Impact impact : visible(camera)) {
            if (!event.getFrustum().isVisible(new AABB(impact.data.center(), impact.data.center())
                    .inflate(impact.data.radius() * 2 + 8))) continue;
            float age = impact.age(partial);
            if (age < 0 || age >= FlameImpactShape.EXPIRE) continue;
            poses.pushPose();
            Vec3 relative = impact.data.center().subtract(camera);
            poses.translate(relative.x, relative.y, relative.z);
            Matrix4f matrix = poses.last().pose();
            cover(impact, age, out, matrix);
            // Procedural smoke, embers, and fragments stay bounded instead of drifting freely past the blast.
            var bounded = FlameImpactShape.withinRadius(sink(out, matrix), impact.data.radius());
            FlameImpactShape.emit(impact.kind, age, impact.data.radius(), impact.data.seed(), detail(),
                    bounded);
            debris(impact, age, bounded);
            surfaces(impact, age, out, matrix, bounded);
            poses.popPose();
            if (age < 160 && impact.kind != FlameKind.NETHER)
                FlameHeatHaze.observeCharge(impact.kind, impact.data.center(),
                        .8F * (1 - age / 160), Math.min(24, impact.data.radius() * 2));
            if (age >= 30 && impact.kind != FlameKind.NETHER) {
                float warmth = .3F * (1 - age / FlameImpactShape.EXPIRE);
                for (int i = 0; i < Math.min(16, impact.surfaces.size()); i += 4)
                    FlameHeatHaze.observeCharge(impact.kind, impact.data.center().add(impact.surfaces.get(i).point),
                            warmth, 2 + impact.data.radius() * .08F);
            }
        }
        buffers.endBatch(FlameRenderTypes.energy());
    }

    private static ChargedFlameShape.Sink sink(VertexConsumer out, Matrix4f matrix) {
        return new ChargedFlameShape.Sink() {
            public void line(Vec3 a, Vec3 b, float width, int color, float alpha) {
                if (alpha > .002F) FlameVfxRenderer.beam(out, matrix, a, b, width, color, alpha);
            }
            public void core(Vec3 p, float radius, int color, float alpha) {
                if (alpha <= .002F) return;
                boolean globe = radius > 2 && p.lengthSqr() < .001;
                int sides = globe ? (detail() == 2 ? 32 : 20) : radius > .3 ? 8 : 6;
                FlameVfxRenderer.sphere(out, matrix, p, radius, color, alpha, sides, globe ? 12 : 4);
            }
            public void leaf(Vec3 p, Vec3 direction, float size, int color, float alpha) {
                if (alpha <= .002F) return;
                Vec3 forward = direction.lengthSqr() < .001 ? new Vec3(0, 1, 0) : direction.normalize();
                Vec3 side = forward.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < .001) side = new Vec3(1, 0, 0);
                side = side.normalize().scale(size * .38);
                FlameVfxRenderer.vertex(out, matrix, p.subtract(forward.scale(size)), color, alpha * .2F);
                FlameVfxRenderer.vertex(out, matrix, p.add(side), color, alpha);
                FlameVfxRenderer.vertex(out, matrix, p.add(forward.scale(size)), color, alpha * .7F);
                FlameVfxRenderer.vertex(out, matrix, p.subtract(side), color, alpha);
            }
        };
    }

    /** Full-radius dust immediately masks instant block updates while the fire globe expands. */
    private static void cover(Impact impact, float age, VertexConsumer out, Matrix4f matrix) {
        if (age >= 30 || !impact.data.terrainDamaged()) return;
        float t = Mth.clamp((age - 8) / 22, 0, 1);
        float fade = 1 - t * t * (3 - 2 * t);
        int color = impact.kind == FlameKind.NETHER ? 0x150B22 : ChargedFlameShape.mix(0x51443A, impact.kind.color(), .18F);
        for (int layer = 0; layer < 3; layer++) {
            float radius = impact.data.radius() * (1 - layer * .012F);
            FlameVfxRenderer.sphere(out, matrix, Vec3.ZERO, radius, color, fade * .72F,
                    detail() == 2 ? 32 : 20, 12);
        }
    }

    private static void debris(Impact impact, float age, ChargedFlameShape.Sink out) {
        if (age >= 55 || !impact.data.terrainDamaged() || impact.debris.isAir()
                || !impact.debris.getFluidState().isEmpty()) return;
        float opacity = Mth.clamp((55 - age) / 14, 0, 1);
        int count = detail() == 2 ? impact.fragments.size() : 8;
        for (int i = 0; i < count; i++) {
            Fragment rock = impact.fragments.get(i);
            Vec3 pos = rock.velocity.scale(age).add(0, -.023 * age * age, 0);
            float floor = -impact.data.radius() * .8F;
            for (Surface surface : impact.surfaces) {
                if (surface.normal.y > .5 && surface.point.subtract(pos).multiply(1, 0, 1).lengthSqr()
                        < Math.max(2, impact.data.radius() * .15) * Math.max(2, impact.data.radius() * .15))
                    floor = (float) surface.point.y;
            }
            if (pos.y < floor + rock.size) {
                double bounce = Math.abs(Mth.sin(age * .8F + rock.spin)) * .16 * opacity;
                pos = new Vec3(pos.x, floor + rock.size + bounce, pos.z);
            }
            int tint = ChargedFlameShape.mix(0x514A42, impact.kind.color(), Math.max(0, .45F - age / 50));
            out.core(pos, rock.size, tint, opacity);
        }
    }

    private static void surfaces(Impact impact, float age, VertexConsumer out, Matrix4f matrix,
            ChargedFlameShape.Sink bounded) {
        if (age < 10 || !impact.data.terrainDamaged() || impact.surfaces.isEmpty()) return;
        float fade = 1 - Mth.clamp((age - 160) / 440, 0, 1);
        fade *= fade;
        float heat = Mth.clamp(1 - age / 360, 0, 1);
        float size = Math.min(2.4F, .38F + impact.data.radius() * .035F);
        for (int index = 0; index < impact.surfaces.size(); index++) {
            Surface surface = impact.surfaces.get(index);
            Vec3 u = surface.normal.cross(new Vec3(0, 1, 0));
            if (u.lengthSqr() < .001) u = new Vec3(1, 0, 0);
            u = u.normalize();
            Vec3 v = surface.normal.cross(u).normalize();
            Vec3 p = surface.point;
            // Decals sit slightly above the actual crater faces; no block replacement or depth conflict.
            for (int segment = 0; segment < 8; segment++) {
                double a = segment * Math.PI / 4, b = (segment + 1) * Math.PI / 4;
                Vec3 first = p.add(u.scale(Math.cos(a) * size)).add(v.scale(Math.sin(a) * size * .65));
                Vec3 second = p.add(u.scale(Math.cos(b) * size)).add(v.scale(Math.sin(b) * size * .65));
                FlameVfxRenderer.vertex(out, matrix, p, 0x160F16, fade * .45F);
                FlameVfxRenderer.vertex(out, matrix, first, 0x160F16, fade * .12F);
                FlameVfxRenderer.vertex(out, matrix, second, 0x160F16, fade * .12F);
                FlameVfxRenderer.vertex(out, matrix, p, 0x160F16, fade * .45F);
            }
            Vec3 last = p.subtract(u.scale(size));
            int tint = ChargedFlameShape.mix(0x24162B, impact.kind.color(), heat);
            for (int segment = 1; segment <= 5; segment++) {
                Vec3 next = p.add(u.scale(size * (segment / 2.5 - 1)))
                        .add(v.scale(Math.sin(segment * 2.1 + index) * size * .21));
                bounded.line(last, next, .02F + size * .023F, tint, fade * (.25F + heat * .5F));
                last = next;
            }
            if (index % 4 == 0 && heat > .01F) {
                float flicker = .7F + .3F * Mth.sin(age * .24F + index);
                Vec3 tip = p.add(surface.normal.scale(size * (.5 + flicker)));
                bounded.line(p, tip, size * .12F,
                        impact.kind.color(), fade * heat * .48F);
                bounded.core(tip, size * .08F,
                        ChargedFlameShape.coreColor(impact.kind, heat), fade * heat * .6F);
            }
        }
    }

    @SubscribeEvent public static void flash(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || mc.player == null || mc.options.hideGui) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float option = mc.options.screenEffectScale().get().floatValue();
        if (option <= 0) return;
        float brightest = 0;
        int color = 0xFFF3DD;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        for (Impact impact : IMPACTS.values()) {
            float age = impact.age(partial);
            if (age < 0 || age >= 5) continue;
            float distance = (float) eye.distanceTo(impact.data.center());
            float weight = Mth.clamp(1 - Math.max(0, distance - impact.data.radius()) / 32, 0, 1);
            Vec3 incoming = impact.data.center().subtract(eye);
            if (distance > impact.data.radius() && incoming.lengthSqr() > .001
                    && incoming.normalize().dot(mc.player.getLookAngle()) < 0) weight *= .1F;
            float amount = (1 - age / 5) * weight * option;
            if (amount > brightest) {
                brightest = amount;
                color = ChargedFlameShape.mix(impact.kind.color(), 0xFFFFFF, .75F);
            }
        }
        if (brightest > .002F) {
            var graphics = event.getGuiGraphics();
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ((int) (brightest * 90) << 24) | color);
        }
    }
}
