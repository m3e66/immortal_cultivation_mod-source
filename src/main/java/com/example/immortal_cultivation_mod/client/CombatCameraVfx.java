package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.CombatFeedbackTiming;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Event-driven camera recoil. A mandatory quiet interval prevents rapid hits from sustaining shake. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class CombatCameraVfx {
    private static ClientLevel world;
    private static final CombatFeedbackTiming IMPULSE = new CombatFeedbackTiming();
    private static float strength, lateral, chargeZoom, motion;
    private static long dashStart = Long.MIN_VALUE, spatialStart = Long.MIN_VALUE;
    private static boolean massive;

    private CombatCameraVfx() {}

    private static void use(ClientLevel level) {
        if (world == level) return;
        world = level;
        IMPULSE.clear();
        strength = lateral = chargeZoom = motion = 0;
        dashStart = spatialStart = Long.MIN_VALUE;
        massive = false;
        CombatSceneEffects.release();
    }

    public static void accept(CombatFeedbackPayloads.Effect effect) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || mc.player == null || !world.dimension().location().equals(effect.dimension())) return;
        long now = world.getGameTime();
        boolean local = effect.subjectId() == mc.player.getId();
        if (effect.kind() == CombatFeedbackPayloads.Kind.DASH_START && local) {
            dashStart = now;
            if ((effect.injuryFlags() & 32) != 0) spatialStart = now;
            return;
        }
        if (effect.kind() == CombatFeedbackPayloads.Kind.DASH_STOP) return;
        float weight = CombatFeedbackTiming.distanceWeight(mc.player.getEyePosition().distanceTo(effect.position()),
                effect.intensity() >= .8F ? 24 : 12);
        if (!local && effect.intensity() < .45F) return;
        float amount = (local ? 1 : weight) * effect.intensity();
        if (amount < .025F || !IMPULSE.trigger(now)) return;
        Vec3 right = mc.player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        lateral = (float) Mth.clamp(effect.direction().dot(right), -1, 1);
        if (!Float.isFinite(lateral)) lateral = 0;
        strength = Math.min(1, amount);
        massive = effect.intensity() >= .8F && weight > .1F;
    }

    /** A single nearby fireball impact shares the existing short impulse and quiet interval. */
    public static void flameImpact(Vec3 center, float radius, float charge) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || mc.player == null || mc.player.isSpectator()) return;
        double distance = Math.max(0, mc.player.getEyePosition().distanceTo(center) - radius * .6);
        float amount = CombatFeedbackTiming.distanceWeight(distance, radius + 16) * (.55F + .45F * charge);
        if (amount < .025F || !IMPULSE.trigger(world.getGameTime())) return;
        Vec3 right = mc.player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        Vec3 incoming = center.subtract(mc.player.getEyePosition());
        lateral = incoming.lengthSqr() < .001 ? 0 : (float) incoming.normalize().dot(right);
        strength = Math.min(1, amount);
        massive = amount > .35F;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        use(mc.level);
        if (world == null || mc.player == null || mc.isPaused()) return;
        float charge = Math.max(ClientData.heavyAttackChargeProgress(),
                ClientData.preparedCharging() ? ClientData.preparedChargeScale() - 1 : 0);
        chargeZoom += (Mth.clamp(charge, 0, 1) * 2.5F - chargeZoom) * .3F;
        float speed = (float) mc.player.getDeltaMovement().length();
        float target = !mc.player.isSpectator() ? Mth.clamp((speed - 1.2F) / 2.5F, 0, .45F) : 0;
        motion += (target - motion) * .3F;
    }

    private static boolean active(Minecraft mc) {
        return mc.level != null && world == mc.level && mc.player != null && !mc.isPaused()
                && mc.screen == null && mc.getCameraEntity() == mc.player && !mc.player.isSpectator();
    }

    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        var mc = Minecraft.getInstance();
        if (!active(mc)) return;
        float pressurePulse = RealmPressureClient.pulseAt(mc.player.getEyePosition(), event.getPartialTick())
                * mc.options.damageTiltStrength().get().floatValue();
        event.setPitch(event.getPitch() + pressurePulse * .28F);
        event.setRoll(event.getRoll() + pressurePulse * .12F);
        double age = IMPULSE.age(world.getGameTime(), event.getPartialTick());
        float envelope = IMPULSE.value(world.getGameTime(), event.getPartialTick());
        if (envelope <= 0) return;
        float value = envelope * strength * mc.options.damageTiltStrength().get().floatValue();
        // At most 1.25 degrees. Pressure never drives this path.
        float oscillation = (float) Math.cos(age * 2.5) * value;
        event.setPitch(event.getPitch() + oscillation * .95F);
        event.setYaw(event.getYaw() + oscillation * lateral * .55F);
        event.setRoll(event.getRoll() - oscillation * lateral * 1.25F);
    }

    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov event) {
        var mc = Minecraft.getInstance();
        if (!active(mc) || !event.usedConfiguredFov()) return;
        float scale = mc.options.fovEffectScale().get().floatValue();
        float pulse = IMPULSE.value(world.getGameTime(), event.getPartialTick()) * strength;
        event.setFOV(Mth.clamp(event.getFOV() + (pulse * 1.7 - chargeZoom) * scale, 25, 150));
    }

    @SubscribeEvent public static void screen(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (!active(mc) || mc.options.hideGui) return;
        float scale = mc.options.screenEffectScale().get().floatValue();
        if (scale <= 0) return;
        long now = world.getGameTime();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float dash = dashStart == Long.MIN_VALUE ? 0 : CombatFeedbackTiming.decay(now - dashStart + partial, 10);
        float spatial = spatialStart == Long.MIN_VALUE ? 0 : CombatFeedbackTiming.decay(now - spatialStart + partial, 8);
        float pressure = RealmPressureClient.exposure() * scale;
        CombatSceneEffects.draw(event.getGuiGraphics(), Math.max(dash * .8F, motion) * scale,
                pressure, spatial * scale, now + partial);
        var graphics = event.getGuiGraphics();
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        if (pressure > .001F) {
            int depth = Math.max(10, Math.min(w, h) / 4);
            for (int i = 0; i < depth; i++) {
                float edge = 1 - i / (float) depth;
                int alpha = (int) (95 * pressure * edge * edge);
                if (alpha == 0) continue;
                int color = (alpha << 24) | 0x050810;
                graphics.fill(i, i, w - i, i + 1, color);
                graphics.fill(i, h - i - 1, w - i, h - i, color);
                graphics.fill(i, i + 1, i + 1, h - i - 1, color);
                graphics.fill(w - i - 1, i + 1, w - i, h - i - 1, color);
            }
        }
        float flash = massive ? CombatFeedbackTiming.decay(IMPULSE.age(now, partial), 2) * strength * scale : 0;
        if (flash > .001F) graphics.fill(0, 0, w, h, ((int) (19 * flash) << 24) | 0xffefd6);
    }
}
