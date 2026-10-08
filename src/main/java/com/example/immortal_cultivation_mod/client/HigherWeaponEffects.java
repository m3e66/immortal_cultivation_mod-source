package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.SevenStarPattern;
import com.example.immortal_cultivation_mod.network.HigherWeaponPayloads;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid="immortal_cultivation_mod",value=Dist.CLIENT)
public final class HigherWeaponEffects {
    private static final ArrayList<HigherWeaponPayloads.Effect> ACTIVE=new ArrayList<>();
    private static final ArrayList<HigherWeaponPayloads.SkillEffect> SKILLS = new ArrayList<>();
    private static final ArrayList<HigherWeaponPayloads.SkillEffect> QI_TRAILS = new ArrayList<>();
    private static int quads;
    private static ClientLevel world;
    private static void use(ClientLevel level) {
        if (world != level) { ACTIVE.clear(); SKILLS.clear(); QI_TRAILS.clear(); world = level; }
    }
    public static void accept(HigherWeaponPayloads.Effect effect) {
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(effect.dimension()))return;
        use(level);
        if(ACTIVE.size()>=32)ACTIVE.removeFirst();
        ACTIVE.add(effect);
    }
    public static void accept(HigherWeaponPayloads.SkillEffect effect) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(effect.dimension())
                || !finite(effect.from()) || !finite(effect.to()) || !Float.isFinite(effect.width())) return;
        use(level);
        // One current VFX state per invisible particle batch. Never overlay consumed old fragments.
        if (effect.projectileId() >= 0) {
            for (var previous : SKILLS) if (effect.replaces(previous) && previous.start() < effect.start()) {
                if (QI_TRAILS.size() >= 96) QI_TRAILS.removeFirst();
                QI_TRAILS.add(previous);
            }
            // Every historical position inherits the newest consumed-section mask.
            QI_TRAILS.replaceAll(previous -> effect.replaces(previous)
                    ? new HigherWeaponPayloads.SkillEffect(previous.dimension(), previous.visual(), previous.from(),
                            previous.to(), previous.width(), previous.start(), previous.color(),
                            previous.bladeMask().and(effect.bladeMask()), previous.projectileId()) : previous);
            QI_TRAILS.removeIf(HigherWeaponPayloads.SkillEffect::emptyBlade);
        }
        SKILLS.removeIf(effect::replaces);
        if (effect.emptyBlade()) return;
        if (SKILLS.size() >= 96) SKILLS.removeFirst();
        SKILLS.add(effect);
    }
    private static boolean finite(Vec3 p) { return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z); }
    private static int lifetime(HigherWeaponPayloads.Visual visual) {
        return switch (visual) {
            case THUNDER_SEAL -> 18;
            case SILK -> 24;
            case THRUST, CLEAVE, SWEEP, GUST -> 10;
            case QI_RISING, QI_FALLING, QI_LEVEL, QI_CONE -> 3;
            case QI_IMPACT -> 7;
            default -> 5;
        };
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        use(Minecraft.getInstance().level);
        if(world!=null) {
            ACTIVE.removeIf(e->world.getGameTime()-e.start()>(e.starfall()?SevenStarPattern.LIFETIME:12));
            SKILLS.removeIf(e->world.getGameTime()-e.start()>lifetime(e.visual()));
            QI_TRAILS.removeIf(e -> world.getGameTime() - e.start() > 6);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world==null||ShaderPackCompatibility.isRenderingShadowPass())return;
        var mc=Minecraft.getInstance();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        Vec3 camera=event.getCamera().getPosition();
        double partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var sword=new ItemStack(ModItems.SEVEN_STAR_SWORD.get());
        for(var effect:ACTIVE)if(effect.starfall()) {
            double age=world.getGameTime()-effect.start()+partial;
            if(age<0 || age>SevenStarPattern.LIFETIME || effect.center().distanceToSqr(camera)>96*96)continue;
            for(int i=0;i<SevenStarPattern.COUNT;i++) {
                double travel=SevenStarPattern.progress(age,i);
                if(travel>=1)continue;
                Vec3 position=SevenStarPattern.point(effect.center(),i).add(0,1+(1-travel*travel)*6,0).subtract(camera);
                pose.pushPose();pose.translate(position.x,position.y,position.z);
                pose.mulPose(Axis.ZP.rotationDegrees(180));pose.scale(2,2,2);
                // Vanilla item rendering uses the world depth buffer, including through transparent terrain.
                mc.getItemRenderer().renderStatic(sword,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,world,i);
                pose.popPose();
            }
        }
        buffers.endBatch();
        // Reuse the existing depth-tested world material, including its Fabulous/shader target handling.
        VertexConsumer out = buffers.getBuffer(FilamentVfx.TYPE);
        Matrix4f matrix = pose.last().pose();
        quads = 0;
        for (var effect : SKILLS) {
            if (quads >= 10000) break;
            if (effect.from().distanceToSqr(camera) > 96 * 96 && effect.to().distanceToSqr(camera) > 96 * 96) continue;
            double age = world.getGameTime() - effect.start() + partial;
            if (age < 0 || age > lifetime(effect.visual())) continue;
            drawSkill(out, matrix, camera, effect, age);
        }
        for (var effect : QI_TRAILS) {
            double age = world.getGameTime() - effect.start() + partial;
            if (quads >= 10000) break;
            if (age < 0 || age >= 6 || effect.to().distanceToSqr(camera) > 96 * 96) continue;
            Vec3 forward = effect.to().subtract(effect.from()).normalize();
            if (forward.lengthSqr() < .001) continue;
            drawExternalQi(out, matrix, camera, effect, forward, effect.width(), 1,
                    (float) (.22 * Math.pow(1 - age / 6, 2)));
        }
        for (var effect : ACTIVE) {
            if (quads >= 10000) break;
            if (effect.center().distanceToSqr(camera) > 96 * 96) continue;
            double age = world.getGameTime() - effect.start() + partial;
            if (age < 0) continue;
            if (effect.starfall()) drawStarfall(out, matrix, camera, effect.center(), age);
            else drawSlam(out, matrix, camera, effect.center(), age);
        }
        buffers.endBatch(FilamentVfx.TYPE);
    }

    private static void drawSkill(VertexConsumer out, Matrix4f matrix, Vec3 camera,
                                  HigherWeaponPayloads.SkillEffect effect, double age) {
        Vec3 from = effect.from(), to = effect.to();
        Vec3 forward = to.subtract(from).normalize();
        if (forward.lengthSqr() < .001) forward = new Vec3(0, 0, 1);
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
        if (side.lengthSqr() < .001) side = new Vec3(1, 0, 0);
        Vec3 up = forward.cross(side).normalize();
        double width = HigherWeaponPayloads.clampWidth(effect.visual(), effect.width());
        double progress = age / lifetime(effect.visual());
        float fade = (float) Math.pow(1 - progress, .8);
        switch (effect.visual()) {
            case QI_RISING, QI_FALLING, QI_LEVEL, QI_CONE, QI_IMPACT ->
                    drawExternalQi(out, matrix, camera, effect, forward, width, age, fade);
            case THRUST -> {
                stroke(out, matrix, camera, from, to, .075, 0xb9dfed, fade * .65F);
                for (int i = -1; i <= 1; i += 2) {
                    Vec3 offset = side.scale(.11 * i);
                    stroke(out, matrix, camera, from.add(offset), to, .03, 0x809fad, fade * .45F);
                    stroke(out, matrix, camera, to.subtract(forward.scale(.45)).add(offset.scale(2)), to, .045, 0xe7eddd, fade * .7F);
                }
            }
            case CLEAVE -> {
                Vec3 center = from.add(0, .95, 0);
                Vec3 horizontal = forward.multiply(1, 0, 1).normalize();
                if (horizontal.lengthSqr() < .001) horizontal = new Vec3(0, 0, 1);
                for (int layer = 0; layer < 3; layer++) {
                    double r = width - layer * .16;
                    arc(out, matrix, camera, center, side, horizontal, r,
                            -.95 + progress * .25, .95 + progress * .25, .09 - layer * .018,
                            layer == 0 ? 0xe0ded0 : 0x8ea6a9, fade * (layer == 0 ? .6F : .25F), 20);
                }
            }
            case SWEEP -> {
                Vec3 center = from.add(0, .95, 0);
                arc(out, matrix, camera, center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                        width, progress * 2, progress * 2 + Math.PI * 1.8, .09, 0xb4d9e8, fade * .5F, 36);
                arc(out, matrix, camera, center.add(0, .10, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                        width * .9, progress * 2 - .15, progress * 2 + Math.PI * 1.55, .035, 0xe4eedb, fade * .5F, 28);
            }
            case SILK -> {
                // Three restrained threads connect the actual hand and bound target. No particle fog.
                for (int cord = 0; cord < 3; cord++) {
                    Vec3 last = from;
                    for (int i = 1; i <= 18; i++) {
                        double t = i / 18D, phase = t * Math.PI * 3 + cord * Math.PI * 2 / 3;
                        double spread = Math.sin(t * Math.PI) * .1;
                        Vec3 next = from.lerp(to, t).add(side.scale(Math.sin(phase) * spread))
                                .add(up.scale(Math.cos(phase) * spread));
                        stroke(out, matrix, camera, last, next, .021, cord == 0 ? 0xf2ead8 : 0x94c9cc, fade * .5F);
                        last = next;
                    }
                }
                for (int i = 0; i < 3; i++) ring(out, matrix, camera, to.add(0, (i - 1) * .3, 0),
                        Math.max(.3, width), .025, 0xd9e5dc, fade * .5F);
            }
            case HOOK -> {
                stroke(out, matrix, camera, from, to, .055, 0x5dbec2, fade * .45F);
                Vec3 center = to.subtract(up.scale(.2));
                arc(out, matrix, camera, center, side, up, .28, -.9, Math.PI * 1.25,
                        .035, 0xb9ded5, fade * .75F, 14);
                stroke(out, matrix, camera, to.subtract(forward.scale(.35)), to, .075, 0x749b99, fade * .5F);
            }
            case WIND, FLAME, FLAME_CHARGED -> {
                boolean fire = effect.visual() != HigherWeaponPayloads.Visual.WIND;
                int edge = fire ? 0xffa348 : 0xc3efe0;
                int tail = fire ? 0xb9512d : 0x4c9d9b;
                double r = Math.max(.45, width);
                // The crescent faces along its server movement segment, so VFX cannot drift from hits.
                for (int layer = 0; layer < 2; layer++) {
                    Vec3 center = to.subtract(forward.scale(.55 + layer * .14));
                    Vec3 last = null;
                    for (int i = 0; i <= 20; i++) {
                        double t = i / 20D, x = (t * 2 - 1) * r;
                        double bow = (1 - Math.pow(t * 2 - 1, 2)) * .55;
                        Vec3 next = center.add(side.scale(x)).add(forward.scale(bow))
                                .add(up.scale(Math.sin(t * Math.PI) * (fire ? .18 : .08)));
                        if (last != null) stroke(out, matrix, camera, last, next,
                                (layer == 0 ? .08 : .17) * Math.sin(t * Math.PI), layer == 0 ? edge : tail,
                                fade * (layer == 0 ? .6F : .2F));
                        last = next;
                    }
                }
                stroke(out, matrix, camera, from, to, fire ? .10 : .06, tail, fade * .3F);
            }
            case GUST -> {
                double r = width * (.25 + .75 * progress);
                ring(out, matrix, camera, from.add(0, .45, 0), r, .10, 0xb4e6d5, fade * .45F);
                ring(out, matrix, camera, from.add(0, .8, 0), r * .9, .05, 0x70aaa5, fade * .3F);
            }
            case THUNDER_SEAL -> {
                // Ground symbol only: no lightning arcs or aura surrounding the player.
                Vec3 center = from.add(0, .035, 0);
                double r = Math.min(1.4, Math.max(.7, width));
                ring(out, matrix, camera, center, r, .03, 0x91bbed, fade * .55F);
                ring(out, matrix, camera, center, r * .78, .02, 0xcfceeb, fade * .45F);
                for (int square = 0; square < 2; square++) {
                    Vec3 previous = null;
                    for (int i = 0; i <= 4; i++) {
                        double a = i * Math.PI / 2 + square * Math.PI / 4;
                        Vec3 next = center.add(Math.cos(a) * r * .68, 0, Math.sin(a) * r * .68);
                        if (previous != null) stroke(out, matrix, camera, previous, next, .025, 0xb2caf2, fade * .55F);
                        previous = next;
                    }
                }
            }
            case STAR -> {
                stroke(out, matrix, camera, from, to, .08, 0xf7dc96, fade * .6F);
                stroke(out, matrix, camera, from, to, .19, 0x728cd3, fade * .18F);
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    Vec3 tip = to.add(side.scale(Math.cos(a) * .23)).add(up.scale(Math.sin(a) * .23));
                    stroke(out, matrix, camera, to, tip, .055, 0xf7ead3, fade * .65F);
                }
            }
        }
    }

    private static void drawExternalQi(VertexConsumer out, Matrix4f matrix, Vec3 camera,
                                      HigherWeaponPayloads.SkillEffect effect, Vec3 forward,
                                      double radius, double age, float fade) {
        Vec3 side = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.right(forward);
        Vec3 up = forward.cross(side).normalize();
        int color = effect.color();
        // A restrained pale edge retains the element's hue without a screen-filling white bloom.
        int edge = (((color >> 16 & 255) + 510) / 3 << 16)
                | (((color >> 8 & 255) + 510) / 3 << 8) | ((color & 255) + 510) / 3;
        if (effect.visual() == HigherWeaponPayloads.Visual.QI_IMPACT) {
            Vec3 at = effect.from();
            double expansion = radius * (.15 + .85 * Math.min(1, age / 4));
            arc(out, matrix, camera, at, side, up, expansion, 0, Math.PI * 2,
                    .035, color, fade * .32F, 20);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4 + .18;
                Vec3 ray = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                stroke(out, matrix, camera, at.add(ray.scale(expansion * .35)),
                        at.add(ray.scale(expansion * (i % 2 == 0 ? 1.3 : .8))).subtract(forward.scale(age * .03)),
                        .025 + fade * .025, edge, fade * .65F);
            }
            return;
        }
        Vec3 center = effect.from().lerp(effect.to(), Math.clamp(age, 0, 1));
        if (effect.visual() == HigherWeaponPayloads.Visual.QI_CONE) {
            double thickness = Math.min(.22, .035 + radius * .014);
            // Hollow, tapered shock front; never a solid glowing disk hiding the target.
            for (int ring = 0; ring < 3; ring++) {
                Vec3 at = center.subtract(forward.scale(ring * .45));
                double size = radius * (1 - ring * .17);
                arc(out, matrix, camera, at, side, up, size, 0, Math.PI * 2,
                        thickness * (ring == 0 ? 1 : .65), ring == 0 ? edge : color,
                        fade * (ring == 0 ? .5F : .16F), 28);
            }
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3;
                Vec3 rim = center.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
                stroke(out, matrix, camera, center.add(forward.scale(.5)), rim,
                        thickness * .45, color, fade * .13F);
            }
            return;
        }
        int stage = effect.visual() == HigherWeaponPayloads.Visual.QI_RISING ? 0
                : effect.visual() == HigherWeaponPayloads.Visual.QI_FALLING ? 1 : 2;
        Vec3 axis = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.axis(forward, stage);
        Vec3 perpendicular = forward.cross(axis).normalize();
        double thickness = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.halfThickness(radius);
        for (int layer = 0; layer < 3; layer++) {
            double layerThickness = thickness * (layer == 0 ? .45 : layer == 1 ? 1 : .8);
            int layerColor = layer == 0 ? edge : color;
            float layerAlpha = fade * (layer == 0 ? .60F : .14F);
            int particles = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.sectionCount(radius);
            for (int particle = 0; particle < particles && quads < 10000; particle++) {
                if (!effect.bladeMask().active(particle)) continue;
                double start = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.particleStart(radius, particle);
                double end = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.particleEnd(radius, particle);
                double startTaper = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.bladeTaper(radius, start);
                double endTaper = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.bladeTaper(radius, end);
                Vec3 from = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.crescent(
                        center.subtract(forward.scale(layer * .20 * Math.max(0, 1 - start * start))), forward, axis, radius, start);
                Vec3 to = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.crescent(
                        center.subtract(forward.scale(layer * .20 * Math.max(0, 1 - end * end))), forward, axis, radius, end);
                quads += FilamentVfx.bladeStrip(out, matrix, from, to, camera, perpendicular,
                        layerThickness * startTaper, layerThickness * endTaper,
                        ((layerColor >> 16) & 255) / 255F, ((layerColor >> 8) & 255) / 255F,
                        (layerColor & 255) / 255F, layerAlpha * (.6F + .4F * (float)Math.sqrt(startTaper)),
                        layerAlpha * (.6F + .4F * (float)Math.sqrt(endTaper)));
            }
        }
        // Short swept filaments follow the confirmed movement segment, not the player's later look direction.
        for (int i = -2; i <= 2; i++) {
            double t = i * .3;
            if (!com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.sectionActive(effect.bladeMask(), radius, t)) continue;
            Vec3 tip = com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.crescent(center, forward, axis, radius, t);
            stroke(out, matrix, camera, tip.subtract(forward.scale(Math.min(1.4, effect.from().distanceTo(effect.to())))),
                    tip, thickness * .12, color, fade * .18F);
        }
    }

    private static void drawSlam(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 center, double age) {
        float fade = (float) Math.max(0, 1 - age / 12);
        if (fade <= 0) return;
        Vec3 ground = center.add(0, .035, 0);
        double radius = 5 * Math.min(1, age / 5);
        ring(out, matrix, camera, ground, radius, .09, 0xc2a173, fade * .5F);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Vec3 last = ground;
            for (int n = 1; n <= 3; n++) {
                double turn = angle + Math.sin(i * 3 + n) * .12;
                Vec3 next = ground.add(Math.cos(turn) * n * radius / 3, 0, Math.sin(turn) * n * radius / 3);
                stroke(out, matrix, camera, last, next, .035, 0xb49362, fade * .4F); last = next;
            }
        }
    }

    private static void drawStarfall(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 center, double age) {
        float fade = (float) Math.min(1, Math.max(0, (SevenStarPattern.LIFETIME - age) / 10));
        Vec3 ground = center.add(0, .04, 0);
        ring(out, matrix, camera, ground, 1.8, .035, 0x929ed9, fade * .25F);
        Vec3 last = null;
        for (int i = 0; i <= SevenStarPattern.COUNT; i++) {
            Vec3 next = SevenStarPattern.point(ground,i==SevenStarPattern.COUNT?3:i);
            if (last != null) stroke(out, matrix, camera, last, next, .025, 0xe5d59c, fade * .25F);
            last = next;
        }
        for (int i = 0; i < 7; i++) {
            Vec3 point = SevenStarPattern.point(center,i).add(0,.12,0);
            double flight = SevenStarPattern.progress(age,i);
            ring(out,matrix,camera,point,.12,.035,0xe9d49a,fade*.5F);
            if (flight > 0 && flight < 1) {
                Vec3 tip = point.add(0, (1 - flight * flight) * 6, 0);
                stroke(out, matrix, camera, tip, tip.add(0, 1.2, 0), .10, 0xe4d394, .45F);
            }
            double impactAge = age - SevenStarPattern.impactTick(i);
            if (impactAge >= 0 && impactAge < 6) ring(out, matrix, camera, point,
                    .2 + impactAge * .2, .06, 0xe9d49a, (float) (.5 * (1 - impactAge / 6)));
        }
        if (age >= SevenStarPattern.FINISH) ring(out, matrix, camera, ground, Math.min(5, (age - SevenStarPattern.FINISH) * 2),
                .12, 0xe9deb5, (float) Math.max(0, (SevenStarPattern.LIFETIME - age) / 20));
    }

    private static void ring(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 center,
                             double radius, double width, int color, float alpha) {
        arc(out, matrix, camera, center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                radius, 0, Math.PI * 2, width, color, alpha, 32);
    }
    private static void arc(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 center,
                            Vec3 x, Vec3 y, double radius, double start, double end,
                            double width, int color, float alpha, int segments) {
        Vec3 last = null;
        for (int i = 0; i <= segments; i++) {
            double angle = start + (end - start) * i / segments;
            Vec3 next = center.add(x.scale(Math.sin(angle) * radius)).add(y.scale(Math.cos(angle) * radius));
            if (last != null) stroke(out, matrix, camera, last, next, width, color, alpha);
            last = next;
        }
    }
    private static void stroke(VertexConsumer out, Matrix4f matrix, Vec3 camera, Vec3 a, Vec3 b,
                               double width, int color, float alpha) {
        if (quads >= 10000 || width <= .001 || alpha <= .001) return;
        quads += FilamentVfx.ribbon(out, matrix, a, b, camera, width,
                ((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, alpha, alpha);
    }
}
