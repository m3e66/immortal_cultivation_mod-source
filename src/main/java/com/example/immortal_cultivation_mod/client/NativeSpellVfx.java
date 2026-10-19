package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.SpellEmitterPresets;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.network.SpellVfxPayloads;
import com.example.immortal_cultivation_mod.network.SpellVfxPayloads.Kind;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

/** Replays the original spell presets through the mod's native particle renderer. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class NativeSpellVfx {
    private static final ResourceLocation PRESETS = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "vfx/spell_emitters.json");
    private static final Map<VisualKey, Visual> VISUALS = new HashMap<>();
    private static final Map<Integer, Long> BEAMS = new HashMap<>();
    private static final Map<EmitterKey, List<OriginalSpellEmitter>> EMITTERS = new HashMap<>();
    private static Map<String, List<SpellEmitterPresets.Definition>> presets;
    private static ClientLevel currentLevel;
    private NativeSpellVfx() {}

    private record VisualKey(Kind kind, long anchor) {}
    private record Visual(SpellVfxPayloads.Visual data, long expires) {}
    private record EmitterKey(String preset, long anchor, boolean entity) {}

    static void reload() { presets = null; EMITTERS.clear(); }

    private static boolean useLevel(ClientLevel level) {
        if (level != currentLevel) {
            VISUALS.clear();
            BEAMS.clear();
            EMITTERS.clear();
            currentLevel = level;
        }
        return level != null;
    }

    public static void accept(SpellVfxPayloads.Visual payload) {
        if (payload.kind() == Kind.PRESSURE) {
            RealmPressureClient.accept(payload);
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (!useLevel(level) || !level.dimension().location().equals(payload.dimension())) return;
        VisualKey key = new VisualKey(payload.kind(), payload.kind() == Kind.WATER_SHIELD
                ? payload.entityId() : BlockPos.containing(payload.position()).asLong());
        if (payload.duration() <= 0) {
            Visual old = VISUALS.remove(key);
            if (old != null) EMITTERS.remove(emitterKey(old.data));
            return;
        }
        if (VISUALS.size() >= 2048 && !VISUALS.containsKey(key)) return;
        if (payload.kind() == Kind.LINGBENG) EMITTERS.remove(emitterKey(payload));
        VISUALS.put(key, new Visual(payload, level.getGameTime() + Mth.clamp(payload.duration(), 1, 40)));
    }

    public static void accept(SpellVfxPayloads.BeamMarkers payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!useLevel(level) || !level.dimension().location().equals(payload.dimension())) return;
        BEAMS.clear();
        EMITTERS.keySet().removeIf(key -> key.preset.equals("beam"));
        long expires = level.getGameTime() + Mth.clamp(payload.duration(), 1, 72000);
        for (int id : payload.entityIds()) BEAMS.put(id, expires);
    }

    private static EmitterKey emitterKey(SpellVfxPayloads.Visual payload) {
        String name = switch (payload.kind()) {
            case WATER_SHIELD -> "water" + Mth.clamp(payload.strength(), 1, 3);
            case PUDDLE -> "puddle";
            case PRESSURE -> throw new IllegalArgumentException("Pressure uses RealmPressureClient");
            case LINGBENG -> "lingbeng";
        };
        boolean entity = payload.kind() == Kind.WATER_SHIELD;
        return new EmitterKey(name, entity ? payload.entityId() : BlockPos.containing(payload.position()).asLong(), entity);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || mc.player == null || mc.isPaused()) return;
        if (presets == null) {
            try (var reader = mc.getResourceManager().openAsReader(PRESETS)) {
                presets = SpellEmitterPresets.read(reader);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load native spell emitter presets", exception);
            }
        }
        long now = mc.level.getGameTime();
        VISUALS.entrySet().removeIf(entry -> entry.getValue().expires <= now);
        BEAMS.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (!mc.player.isAlive()) BEAMS.clear();
        Set<EmitterKey> active = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!entity.isAlive() || entity.distanceToSqr(mc.player) > 64.0D * 64.0D) continue;
            if (entity instanceof FireballProjectileEntity) keepEntity(active, "fireball", entity);
            else if (entity instanceof LightBeamProjectileEntity) keepEntity(active, "ball", entity);
            else if (entity instanceof LiuguangJianyingProjectileEntity
                    || entity instanceof FlyingSwordEntity sword && sword.isShot() && !sword.isStuck()) {
                keepEntity(active, "flying_sword", entity);
            } else if (entity instanceof IceFxAnchorEntity) keepEntity(active, "ice1", entity);
            if (!(entity instanceof LivingEntity living)) continue;
            if (isMeditating(living)) {
                keepEntity(active, "meditating", entity);
            }
            if (living.hasEffect(ModEffects.FROST_FLIGHT)
                    && !(entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson())) {
                keepEntity(active, "ice2", entity);
            }
        }
        for (Visual visual : VISUALS.values()) {
            var data = visual.data;
            EmitterKey key = emitterKey(data);
            Vec3 root;
            Quaternionf rotation = new Quaternionf();
            if (data.kind() == Kind.WATER_SHIELD) {
                Entity owner = mc.level.getEntity(data.entityId());
                if (owner == null || !owner.isAlive()) continue;
                root = owner.getEyePosition();
                // The old shield refreshed its emitter every two ticks.
                if (owner.tickCount % 2 == 0) EMITTERS.remove(key);
                float spin = (float) Math.toRadians((now * Math.PI / 10.0D) % (2.0D * Math.PI));
                rotation.rotationXYZ(0, spin, spin);
            } else root = Vec3.atCenterOf(BlockPos.containing(data.position()));
            if (root.distanceToSqr(mc.player.position()) <= 64.0D * 64.0D) keep(active, key, root, rotation);
        }
        for (int id : BEAMS.keySet()) {
            Entity entity = mc.level.getEntity(id);
            if (entity != null && entity.isAlive() && entity.distanceToSqr(mc.player) <= 128.0D * 128.0D) {
                keepEntity(active, "beam", entity);
            }
        }
        EMITTERS.keySet().retainAll(active);
    }

    static boolean isMeditating(LivingEntity entity) {
        return entity.isAlive() && (entity.hasEffect(ModEffects.QI_GATHERING)
                || entity instanceof Player player && (ClientData.isPlayerMeditating(entity.getUUID())
                        || ForgingVisualState.isForging(player))
                || entity instanceof CultivationNpc npc && npc.npcCultivationState()>0);
    }

    private static void keepEntity(Set<EmitterKey> active, String name, Entity entity) {
        // Entity-bound presets were authored relative to the eye, including their own Y offsets.
        Quaternionf rotation = "ice2".equals(name)
                ? new Quaternionf().rotationY((float) Math.toRadians(-0.85D)) : new Quaternionf();
        keep(active, new EmitterKey(name, entity.getId(), true), entity.getEyePosition(), rotation);
    }

    private static void keep(Set<EmitterKey> active, EmitterKey key, Vec3 root, Quaternionf rotation) {
        if (!active.add(key)) return;
        if (!key.preset.equals("meditating"))
            SpellParticleAccents.trail(key.preset, root, Vec3.ZERO, key.anchor, .65);
        var emitters = EMITTERS.computeIfAbsent(key, unused -> presets.get(key.preset).stream()
                .map(definition -> new OriginalSpellEmitter(definition, currentLevel.random.nextLong(),
                        key.preset)).toList());
        for (OriginalSpellEmitter emitter : emitters) emitter.tick(currentLevel, root, rotation);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ShaderPackCompatibility.isRenderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!useLevel(mc.level) || mc.player == null || EMITTERS.isEmpty()) return;
        // Screen closure can pause the game before the next client tick.
        EMITTERS.keySet().removeIf(key -> key.preset.equals("meditating")
                && (!(mc.level.getEntity((int) key.anchor) instanceof LivingEntity living)
                        || !isMeditating(living)));
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = mc.renderBuffers().bufferSource();
        for (boolean filaments : new boolean[]{false, true}) {
            var type = filaments ? FilamentVfx.TYPE : OriginalSpellVfxRender.TYPE;
            var vertices = buffers.getBuffer(type);
            for (var entry : EMITTERS.entrySet()) {
                var matrix = event.getPoseStack().last().pose();
                var key = entry.getKey();
                Entity anchor = key.entity ? mc.level.getEntity((int) key.anchor) : null;
                if (anchor instanceof net.minecraft.world.entity.projectile.Projectile) {
                    Vec3 pivot = anchor.getPosition(partial).add(0, anchor.getEyeHeight(), 0).subtract(event.getCamera().getPosition());
                    matrix = new org.joml.Matrix4f(matrix).translate((float)pivot.x, (float)pivot.y, (float)pivot.z)
                            .scale(SpellProjectileVisuals.SCALE).translate((float)-pivot.x, (float)-pivot.y, (float)-pivot.z);
                }
                // Scale projectile-local geometry without changing emission, movement, or world telegraphs.
                for (var emitter : entry.getValue()) emitter.render(vertices, matrix, event.getCamera(), partial, filaments);
            }
            buffers.endBatch(type);
        }
    }
}
