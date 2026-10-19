package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Brief, entity-attached wounds. Status refreshes stop as soon as the actual burn/freeze ends. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class InjuryVfx {
    private static final Map<Integer, Wound> WOUNDS = new LinkedHashMap<>();
    private static ClientLevel world;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(.48F, .025F, .018F), .55F);
    private static final RenderType MATTER = RenderType.create("cultivation_injury_marks",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 32768, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("injury_alpha", () -> {
                        RenderSystem.enableBlend(); RenderSystem.blendFuncSeparate(770, 771, 0, 1);
                    }, () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); }))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL).setOutputState(FilamentVfx.WORLD_TARGET).createCompositeState(false));

    private static final class Wound {
        final java.util.UUID identity;
        long burn, frost, lightning, aura, weak;
        boolean sawBurn, sawFrost;
        Wound(LivingEntity entity) { identity = entity.getUUID(); }
        long expiry() { return Math.max(Math.max(burn, frost), Math.max(lightning, Math.max(aura, weak))); }
    }
    private InjuryVfx() {}
    private static void use(ClientLevel level) {
        if (world != level) { WOUNDS.clear(); world = level; }
    }
    private static Wound wound(LivingEntity entity) {
        Wound state = WOUNDS.get(entity.getId());
        if (state != null && state.identity.equals(entity.getUUID())) return state;
        if (WOUNDS.size() >= 48) WOUNDS.remove(WOUNDS.keySet().iterator().next());
        state = new Wound(entity);
        WOUNDS.put(entity.getId(), state);
        return state;
    }
    public static void accept(CombatFeedbackPayloads.Effect effect) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || !world.dimension().location().equals(effect.dimension())
                || (effect.kind() != CombatFeedbackPayloads.Kind.HIT && effect.kind() != CombatFeedbackPayloads.Kind.SHIELD_BREAK)
                || !(world.getEntity(effect.subjectId()) instanceof LivingEntity living)) return;
        Wound state = wound(living);
        long now = world.getGameTime();
        int flags = effect.injuryFlags();
        if ((flags & CombatFeedbackPayloads.FIRE) != 0) state.burn = now + 50;
        if ((flags & CombatFeedbackPayloads.FROST) != 0) state.frost = now + 35;
        if ((flags & CombatFeedbackPayloads.LIGHTNING) != 0) state.lightning = now + 12;
        if ((flags & CombatFeedbackPayloads.LOW_QI) != 0) state.weak = now + 20;
        if ((flags & CombatFeedbackPayloads.AURA) != 0 || effect.kind() == CombatFeedbackPayloads.Kind.SHIELD_BREAK)
            state.aura = now + 14;
        if ((flags & CombatFeedbackPayloads.PHYSICAL) != 0 && effect.kind() == CombatFeedbackPayloads.Kind.HIT) {
            int count = mc.options.particles().get() == ParticleStatus.MINIMAL ? 1 : 3 + (int) (effect.intensity() * 4);
            var random = RandomSource.create(effect.seed());
            for (int i = 0; i < count; i++) {
                Vec3 velocity = effect.direction().scale(.03 + random.nextDouble() * .06)
                        .add((random.nextDouble() - .5) * .04, -.02 + random.nextDouble() * .04, (random.nextDouble() - .5) * .04);
                var p = mc.particleEngine.createParticle(BLOOD, effect.position().x, effect.position().y, effect.position().z,
                        velocity.x, velocity.y, velocity.z);
                if (p != null) p.setLifetime(7 + random.nextInt(5));
            }
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || mc.player == null || mc.isPaused()) return;
        long now = world.getGameTime();
        WOUNDS.entrySet().removeIf(entry -> {
            var entity = world.getEntity(entry.getKey());
            return !(entity instanceof LivingEntity living) || living.isRemoved()
                    || !entity.getUUID().equals(entry.getValue().identity) || entry.getValue().expiry() < now;
        });
        // Read synced statuses, including effects applied just after the original damage event.
        if (now % 4 == 0) {
            int checked = 0;
            for (var entity : world.entitiesForRendering()) {
                if (++checked > 256) break;
                if (!(entity instanceof LivingEntity living) || !living.isAlive()
                        || living.distanceToSqr(mc.player) > 32 * 32) continue;
                boolean burn = living.isOnFire() || living.hasEffect(ModEffects.EMBER_BURN) || living.hasEffect(ModEffects.QI_FIRE_BURN);
                boolean frost = living.getTicksFrozen() > 0 || living.hasEffect(ModEffects.FROZEN) || living.hasEffect(ModEffects.FROZEN_QI);
                Wound state = WOUNDS.get(entity.getId());
                if (burn || frost) state = wound(living);
                if (state == null) continue;
                if (burn) { state.burn = now + 8; state.sawBurn = true; }
                else if (state.sawBurn) state.burn = 0;
                if (frost) { state.frost = now + 8; state.sawFrost = true; }
                else if (state.sawFrost) state.frost = 0;
            }
        }
        var data = ClientData.cultivationData;
        long max = Math.max(1L, (long) data.maxQi());
        if (max > 1 && data.qi() < max * .15 && ClientData.combatActive()) wound(mc.player).weak = now + 6;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || world == null
                || world != Minecraft.getInstance().level || ShaderPackCompatibility.isRenderingShadowPass()) return;
        var mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        Matrix4f matrix = event.getPoseStack().last().pose();
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = world.getGameTime() + partial;
        int budget = mc.options.particles().get() == ParticleStatus.MINIMAL ? 4 : 16;
        var matter = buffers.getBuffer(MATTER);
        for (var entry : WOUNDS.entrySet()) {
            if (!(world.getEntity(entry.getKey()) instanceof LivingEntity entity) || !visible(entity, camera)
                    || !entity.getUUID().equals(entry.getValue().identity)) continue;
            float burn = fade(entry.getValue().burn, time, 15);
            if (burn <= 0) continue;
            if (--budget < 0) break;
            marks(matter, matrix, entity, camera, partial, burn);
        }
        buffers.endBatch(MATTER);
        var light = buffers.getBuffer(FilamentVfx.TYPE);
        budget = mc.options.particles().get() == ParticleStatus.MINIMAL ? 4 : 16;
        for (var entry : WOUNDS.entrySet()) {
            if (!(world.getEntity(entry.getKey()) instanceof LivingEntity entity) || !visible(entity, camera)
                    || !entity.getUUID().equals(entry.getValue().identity)) continue;
            Wound state = entry.getValue();
            float frost = fade(state.frost, time, 8), lightning = fade(state.lightning, time, 8);
            float aura = fade(state.aura, time, 14), weak = fade(state.weak, time, 6);
            if (frost <= 0 && lightning <= 0 && aura <= 0 && weak <= 0) continue;
            if (--budget < 0) break;
            if (frost > 0) frost(light, matrix, entity, camera, partial, frost);
            if (lightning > 0) electricity(light, matrix, entity, camera, partial, lightning, time);
            if (aura > 0 || weak > 0) aura(light, matrix, entity, camera, partial, aura, weak, time);
        }
        buffers.endBatch(FilamentVfx.TYPE);
    }
    private static boolean visible(LivingEntity entity, Vec3 camera) {
        var mc = Minecraft.getInstance();
        return !entity.isInvisible() && entity.position().distanceToSqr(camera) < 40 * 40
                && !(entity == mc.player && mc.options.getCameraType().isFirstPerson());
    }
    private static float fade(long end, double now, double tail) { return (float) Mth.clamp((end - now) / tail, 0, 1); }
    private static Vec3 body(LivingEntity entity, float partial, double x, double y, double z) {
        float yaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
        return entity.getPosition(partial).add(new Vec3(x * entity.getBbWidth(), y * entity.getBbHeight(), z * entity.getBbWidth()).yRot(-yaw));
    }
    private static void marks(VertexConsumer out, Matrix4f matrix, LivingEntity e, Vec3 camera, float partial, float alpha) {
        for (int face = -1; face <= 1; face += 2) for (int i = 0; i < 9; i++) {
            double x = Math.sin(i * 19 + e.getId()) * .24, y = .39 + (i % 4) * .066, size = .035 + (i % 3) * .012;
            Vec3 a = body(e, partial, x - size, y, face * .285), b = body(e, partial, x, y - size * .6, face * .285);
            Vec3 c = body(e, partial, x + size, y + size * .3, face * .285), d = body(e, partial, x, y + size, face * .285);
            vertex(out, matrix, a.subtract(camera), .07F, .048F, .04F, alpha * .65F);
            vertex(out, matrix, b.subtract(camera), .09F, .04F, .025F, alpha * .45F);
            vertex(out, matrix, c.subtract(camera), .07F, .048F, .04F, alpha * .65F);
            vertex(out, matrix, d.subtract(camera), .09F, .04F, .025F, alpha * .45F);
        }
    }
    private static void frost(VertexConsumer out, Matrix4f matrix, LivingEntity e, Vec3 camera, float partial, float alpha) {
        for (int face = -1; face <= 1; face += 2) for (int i = 0; i < 5; i++) {
            double x = -.2 + i * .075, y = .42 + (i % 3) * .11;
            Vec3 a = body(e, partial, x, y, face * .29), b = body(e, partial, x + .045, y + .1, face * .29);
            FilamentVfx.ribbon(out, matrix, a, b, camera, .011, .48F, .84F, 1F, alpha * .6F, alpha * .25F);
            FilamentVfx.ribbon(out, matrix, a.lerp(b, .6), body(e, partial, x - .055, y + .09, face * .29),
                    camera, .007, .72F, .94F, 1F, alpha * .45F, 0);
        }
    }
    private static void electricity(VertexConsumer out, Matrix4f matrix, LivingEntity e, Vec3 camera, float partial, float alpha, double time) {
        for (int strand = 0; strand < 3; strand++) {
            Vec3 last = null;
            for (int i = 0; i < 7; i++) {
                double angle = strand * Math.PI * 2 / 3 + i * .19 + Math.floor(time * 1.5) * .7;
                Vec3 p = body(e, partial, Math.cos(angle) * .56, .24 + i * .092,
                        Math.sin(angle) * .34 + Math.sin(i * 21 + Math.floor(time)) * .03);
                if (last != null) FilamentVfx.ribbon(out, matrix, last, p, camera, .008, .5F, .7F, 1F, alpha * .8F, alpha * .5F);
                last = p;
            }
        }
    }
    private static void aura(VertexConsumer out, Matrix4f matrix, LivingEntity e, Vec3 camera, float partial, float broken, float weak, double time) {
        for (int strand = 0; strand < 5; strand++) {
            if (weak > 0 && Math.sin(time * 1.6 + strand * 3.7) < .45) continue;
            Vec3 last = null;
            float alpha = broken > 0 ? broken * .45F : weak * .16F;
            for (int i = 0; i < 5; i++) {
                double angle = strand * Math.PI * 2 / 5 + (i % 2) * .12;
                Vec3 p = body(e, partial, Math.cos(angle) * (.68 + (1 - broken) * .12),
                        .2 + i * .14, Math.sin(angle) * .64);
                if (last != null && !(broken > 0 && i == 3)) FilamentVfx.ribbon(out, matrix, last, p, camera,
                        broken > 0 ? .018 : .008, .3F, .8F, 1F, alpha, alpha * .45F);
                last = p;
            }
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, float r, float g, float b, float a) {
        out.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }
}
