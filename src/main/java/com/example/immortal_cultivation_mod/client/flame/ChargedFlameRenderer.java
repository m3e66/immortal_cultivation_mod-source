package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.flame.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Client-only charge, screen lighting and bounded world-space flame trails. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class ChargedFlameRenderer {
    private static final Map<UUID, ArrayDeque<Vec3>> TRAILS = new HashMap<>();
    private static ClientLevel world;
    private static int rendered;
    private ChargedFlameRenderer() { }

    public static boolean charging(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator() || !entity.isUsingItem()) return false;
        FlameKind kind = FlameVesselItem.flame(entity.getUseItem());
        var local = Minecraft.getInstance().player;
        // Other players' use state is server-authoritative; their Qi is not synchronized here.
        // Local prediction must also check the synchronized Qi before drawing any charge effects.
        return FlameVesselItem.chargeEffectsReady(kind, entity.getTicksUsingItem(),
                entity == local ? ClientData.cultivationData.qi() : 0,
                entity != local || local.isCreative());
    }
    private static int detail() { return Minecraft.getInstance().options.particles().get() == ParticleStatus.ALL ? 2 : 1; }
    private static void orient(PoseStack poses, Vec3 forward) {
        if (forward.lengthSqr() > .00001) poses.mulPose(new Quaternionf().rotationTo(new Vector3f(0,0,1),
                new Vector3f((float)forward.x,(float)forward.y,(float)forward.z).normalize()));
    }
    private static Vec3 anchor(LivingEntity caster, float partial, float held) {
        Vec3 point = FlameVesselItem.chargeOrigin(caster, held);
        Vec3 eye = caster.getEyePosition();
        var hit = caster.level().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.MISS) point = hit.getLocation().subtract(caster.getLookAngle().scale(.1));
        return point.add(caster.getPosition(partial).subtract(caster.position()));
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (world != mc.level) { TRAILS.clear(); world = mc.level; }
        if (world == null || mc.isPaused()) return;
        var alive = new HashSet<UUID>();
        int count = 0;
        for (var entity : world.entitiesForRendering()) {
            if (!(entity instanceof FlameShotEntity shot) || shot.charge() <= 0 || ++count > 32) continue;
            alive.add(shot.getUUID());
            var trail = TRAILS.computeIfAbsent(shot.getUUID(), id -> new ArrayDeque<>());
            trail.addFirst(shot.position().add(0, shot.getBbHeight()*.5, 0));
            while (trail.size() > 18) trail.removeLast();
        }
        TRAILS.keySet().retainAll(alive);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        TRAILS.clear(); world = null;
    }

    @SubscribeEvent public static void renderCharge(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) { rendered = 0; return; }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ShaderPackCompatibility.isRenderingShadowPass()) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = mc.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        int count = 0;
        for (var caster : mc.level.players()) {
            if (!charging(caster) || caster.isInvisible() || caster.distanceToSqr(camera) > 48*48 || ++count > 8) continue;
            float held = Math.min(90, caster.getTicksUsingItem() + partial);
            FlameKind kind = FlameVesselItem.flame(caster.getUseItem());
            Vec3 center = anchor(caster, partial, held);
            var poses = event.getPoseStack();
            poses.pushPose();
            Vec3 relative = center.subtract(camera);
            poses.translate(relative.x, relative.y, relative.z);
            orient(poses, caster.getLookAngle());
            draw(kind, held, held/90, false, poses, buffers);
            poses.popPose();
            FlameHeatHaze.observeCharge(kind, center, held/90, ChargedFlameShape.radius(held)*3);
        }
        buffers.endBatch(FlameRenderTypes.energy());
    }

    public static void projectile(FlameShotEntity shot, float partial, PoseStack poses, MultiBufferSource buffers) {
        if (++rendered > 24) return;
        poses.pushPose();
        orient(poses, shot.getDeltaMovement());
        draw(shot.kind(), shot.tickCount+partial, shot.charge(), true, poses, buffers);
        poses.popPose();
        FlameHeatHaze.observeCharge(shot.kind(), shot.getPosition(partial), shot.charge(), 2.8F);
    }

    private static void draw(FlameKind kind, float age, float strength, boolean flying,
            PoseStack poses, MultiBufferSource buffers) {
        VertexConsumer out = buffers.getBuffer(FlameRenderTypes.energy());
        Matrix4f matrix = poses.last().pose();
        ChargedFlameShape.emit(kind, age, strength, flying, detail(), new ChargedFlameShape.Sink() {
            public void line(Vec3 a, Vec3 b, float width, int color, float alpha) {
                FlameVfxRenderer.beam(out, matrix, a, b, width, color, alpha);
            }
            public void core(Vec3 p, float radius, int color, float alpha) {
                FlameVfxRenderer.sphere(out, matrix, p, radius, color, alpha, radius > .2 ? 16 : 6, radius > .2 ? 8 : 4);
            }
            public void leaf(Vec3 p, Vec3 direction, float size, int color, float alpha) {
                Vec3 forward = direction.lengthSqr() < .001 ? new Vec3(0,1,0) : direction.normalize();
                Vec3 side = forward.cross(new Vec3(0,1,0));
                if (side.lengthSqr() < .001) side = new Vec3(1,0,0);
                side = side.normalize().scale(size*.38);
                FlameVfxRenderer.vertex(out,matrix,p.subtract(forward.scale(size)),color,alpha*.2F);
                FlameVfxRenderer.vertex(out,matrix,p.add(side),color,alpha);
                FlameVfxRenderer.vertex(out,matrix,p.add(forward.scale(size)),color,alpha*.7F);
                FlameVfxRenderer.vertex(out,matrix,p.subtract(side),color,alpha);
                FlameVfxRenderer.beam(out,matrix,p.subtract(forward.scale(size)),p.add(forward.scale(size)),size*.03F,
                        ChargedFlameShape.mix(color,0xFFFFFF,.4F),alpha);
            }
        });
    }

    public static void trail(FlameShotEntity shot, float partial, PoseStack poses, MultiBufferSource buffers) {
        var points = TRAILS.get(shot.getUUID());
        if (points == null || points.size() < 2 || rendered > 24) return;
        var out = buffers.getBuffer(FlameRenderTypes.energy());
        var matrix = poses.last().pose();
        Vec3 origin = shot.getPosition(partial), previous = new Vec3(0,shot.getBbHeight()*.5,0);
        Vec3 forward = shot.getDeltaMovement().lengthSqr() > .001 ? shot.getDeltaMovement().normalize() : new Vec3(0,0,1);
        Vec3 side = forward.cross(new Vec3(0,1,0));
        if (side.lengthSqr() < .001) side = new Vec3(1,0,0);
        side = side.normalize(); Vec3 up = side.cross(forward).normalize();
        int index = 0;
        for (Vec3 position : points) {
            Vec3 point = position.subtract(origin);
            float remaining = 1-index/(float)points.size();
            int strands = detail() == 2 ? 9 : 5;
            for (int strand = 0; strand < strands; strand++) {
                double angle = strand*Math.PI*2/strands+(shot.tickCount+partial)*.45-index*.7;
                Vec3 offset = side.scale(Math.cos(angle)*.42).add(up.scale(Math.sin(angle)*.42)).scale(shot.charge()*remaining);
                // Positions follow the projectile's actual path; turbulence follows behind each sample.
                FlameVfxRenderer.beam(out,matrix,previous.add(offset),point.add(offset.scale(.8)),
                        (.055F+shot.charge()*.08F)*remaining, strand%3 == 0 ? ChargedFlameShape.coreColor(shot.kind(),.4F) : shot.kind().color(), remaining*.55F);
            }
            previous = point; index++;
        }
    }

    @SubscribeEvent public static void screenLight(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !charging(mc.player)) return;
        float held = mc.player.getTicksUsingItem()+event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var kind = FlameVesselItem.flame(mc.player.getUseItem());
        float flash = held>=87 && held<90 ? 18*(1-(held-87)/3)
                : held>=70 && held<75 ? 14*(1-(held-70)/5) : 0;
        int alpha = (int)(Math.min(held/90,1)*13 + flash);
        if (kind == FlameKind.NETHER) alpha /= 2;
        var gui = event.getGuiGraphics();
        gui.fillGradient(0,0,gui.guiWidth(),gui.guiHeight(), alpha<<24|kind.color(), (alpha/3)<<24|kind.color());
    }

}
