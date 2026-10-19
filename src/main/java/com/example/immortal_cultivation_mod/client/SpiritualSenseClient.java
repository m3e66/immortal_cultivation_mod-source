package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.client.hud.HudLayout;
import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.renderer.SpiritualReconstructionRenderer;
import com.example.immortal_cultivation_mod.network.SpiritualSensePayloads;
import com.example.immortal_cultivation_mod.network.SpiritualSensePayloads.Signature;
import com.example.immortal_cultivation_mod.spiritual.SpiritualSenseSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Shader-like, non-outline presentation of server-provided spiritual signatures. */
public final class SpiritualSenseClient {
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType SENSE_RENDER_TYPE =
            createSenseRenderType();
    private static final RenderType AIR_FLOW_RENDER_TYPE =
            createAirFlowRenderType();
    private static final MultiBufferSource.BufferSource SENSE_BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(65536));
    private static final Color SENSE_CYAN = new Color(0.35F, 0.93F, 1.0F);
    private static final Color SENSE_VIOLET = new Color(0.64F, 0.48F, 1.0F);
    private static final Color SENSE_GOLD = new Color(1.0F, 0.88F, 0.52F);
    private static final Color AIR_FLOW_COLOR =
            new Color(0.76F, 0.94F, 1.0F);
    private static final int FEEDBACK_LIMIT = 10;
    private static final int FOCUSED_FEEDBACK_LIMIT = 6;
    private static boolean keyWasDown;
    private static boolean focusKeyWasDown;
    private static boolean holding;
    private static boolean active;
    private static boolean directionalFocus;
    private static boolean unavailableShown;
    private static float targetRadius;
    private static float renderedRadius;
    private static float maxRadius;
    private static float focus;
    private static float detectionPower;
    private static int ambientQi;
    private static float renderedAmbientQi;
    private static boolean selfUsingQi;
    private static float fade;
    private static float focusFade;
    private static int clientTicks;
    private static int activationTicks;
    private static int deactivationTicks;
    private static int counterPressureTicks;
    private static int clashTicks;
    private static int veinPatternEpoch = 0x4A17B93D;
    private static int focusedSignatureId = Integer.MIN_VALUE;
    private static List<Signature> signatures = List.of();
    private static final Map<Integer, SignatureVisual> VISUALS = new HashMap<>();
    private static final Matrix4f WORLD_TO_CLIP = new Matrix4f();
    private static Vec3 projectionCameraPosition = Vec3.ZERO;
    private static boolean projectionReady;
    private static final AtmosphericQiCells ATMOSPHERIC_CELLS = new AtmosphericQiCells();
    private static final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Vec3> ATMOSPHERIC_FLOW_NODES =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>(16);

    private SpiritualSenseClient() {
    }

    private static RenderType createSenseRenderType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setTransparencyState(
                        RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                .setCullState(RenderStateShard.NO_CULL)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create(
                ImmortalCultivationMod.MODID + ":spiritual_accents",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                state);
    }

    private static RenderType createAirFlowRenderType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setTransparencyState(
                        RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setCullState(RenderStateShard.NO_CULL)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create(
                ImmortalCultivationMod.MODID + ":spiritual_air_flow",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                2048,
                false,
                true,
                state);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SpiritualSenseClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SpiritualSenseClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(SpiritualSenseClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(
                SpiritualReconstructionRenderer::onGeoEntityPost);
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isSensingEntity(int entityId) {
        if (!active) {
            return false;
        }
        SignatureVisual visual = VISUALS.get(entityId);
        return visual != null
                && visual.missingTicks <= 8
                && visual.signature.tier()
                        >= SpiritualSenseSystem.TIER_PRESENCE;
    }

    public static void accept(
            SpiritualSensePayloads.ClientboundSpiritualSensePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.unavailable() && holding && !unavailableShown
                && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID
                            + ".spiritual_sense_requires_cultivation"), true);
            unavailableShown = true;
        }
        boolean wasActive = active;
        active = payload.active();
        if (!active) {
            holding = false;
            keyWasDown = false;
            focusKeyWasDown = false;
        }
        targetRadius = Math.max(0.0F, payload.radius());
        maxRadius = Math.max(0.0F, payload.maxRadius());
        focus = Mth.clamp(payload.focus(), 0.0F, 1.0F);
        directionalFocus = payload.directionalFocus();
        detectionPower = Math.max(0.0F, payload.detectionPower());
        ambientQi = Math.max(0, payload.ambientQi());
        selfUsingQi = payload.selfUsingQi();
        if (active && !wasActive) {
            activationTicks = 0;
            deactivationTicks = 0;
            // Change vein topology once per sensing session, not every frame.
            veinPatternEpoch = veinPatternEpoch * 1664525
                    + 1013904223 + clientTicks;
        }
        if (active) {
            signatures = List.copyOf(payload.signatures());
            acceptSignatures(signatures);
        }
        if (!active && !holding) {
            targetRadius = 0.0F;
            focus = 0.0F;
            directionalFocus = false;
            deactivationTicks = 0;
        }
    }

    /**
     * Returns the server-authorized Qi-body reconstruction for a real entity.
     * Every detected living presence receives an automatic silhouette;
     * identification makes it steadier, while 洞察 reveals the clear body.
     */
    public static Reconstruction reconstructionFor(int entityId) {
        if (fade <= 0.01F || ShaderPackCompatibility.isRenderingShadowPass()) return null;
        SignatureVisual visual = VISUALS.get(entityId);
        if (visual == null || visual.missingTicks > 8) return null;
        Signature signature = visual.signature;
        if (signature.tier() < SpiritualSenseSystem.TIER_PRESENCE
                || !isLivingKind(signature.kind())) {
            return null;
        }

        boolean presenceOnly = signature.tier()
                == SpiritualSenseSystem.TIER_PRESENCE;
        boolean identificationOnly = signature.tier()
                == SpiritualSenseSystem.TIER_IDENTIFICATION;
        boolean concealed = (signature.flags()
                & SpiritualSenseSystem.FLAG_CONCEALED) != 0;
        float reveal = visual.reveal(clientTicks, concealed);
        float distanceFraction = maxRadius <= 0.01F
                ? 0.0F
                : Mth.clamp(signature.distance() / maxRadius, 0.0F, 1.0F);
        float distanceClarity = Mth.lerp(distanceFraction, 1.0F, 0.28F);
        float interference = (signature.flags()
                & SpiritualSenseSystem.FLAG_DENSE_INTERFERENCE) != 0
                ? 0.95F
                : (signature.flags()
                        & SpiritualSenseSystem.FLAG_INTERFERENCE) != 0
                        ? 0.52F : 0.0F;
        if (presenceOnly) {
            interference = Math.max(interference, 0.68F);
        } else if (identificationOnly) {
            interference = Math.max(interference, 0.48F);
        }
        float concealment = concealed
                ? Mth.lerp(reveal, 0.92F, 0.15F) : 0.0F;
        float pressure = (signature.flags()
                & SpiritualSenseSystem.FLAG_PRESSURE) != 0
                ? Mth.clamp(0.55F + signature.intensity() * 0.20F,
                        0.55F, 1.0F)
                : 0.0F;
        float clarity = fade * reveal * distanceClarity
                * (1.0F - interference * 0.20F);
        if (presenceOnly) {
            clarity *= 0.46F;
        } else if (identificationOnly) {
            clarity *= 0.62F;
        }
        if (clarity <= 0.015F) return null;

        int alpha = Mth.clamp(Math.round(34.0F + clarity * 184.0F),
                20, presenceOnly ? 92 : identificationOnly ? 132 : 218);
        float signatureClass = switch (signature.kind()) {
            case SpiritualSenseSystem.KIND_HOSTILE -> 1.0F / 3.0F;
            case SpiritualSenseSystem.KIND_CULTIVATOR -> 2.0F / 3.0F;
            case SpiritualSenseSystem.KIND_SOUL -> 1.0F;
            default -> 0.0F;
        };
        return new Reconstruction(
                colorFor(signature).argb(alpha),
                interference,
                pressure,
                concealment,
                signatureClass);
    }

    public record Reconstruction(
            int argb,
            float interference,
            float pressure,
            float concealment,
            float signatureClass) {
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.getConnection() == null) {
            clear();
            return;
        }

        clientTicks++;

        boolean canToggle = minecraft.screen == null
                && !minecraft.player.isSpectator();
        while (ModKeyMappings.SPIRITUAL_SENSE.get().consumeClick()) {
            if (canToggle) {
                holding = !holding;
                unavailableShown = false;
            }
        }
        if (minecraft.player.isSpectator()) {
            holding = false;
        }
        boolean keyDown = holding;
        boolean focusKeyDown = keyDown && minecraft.player.isShiftKeyDown();
        if (keyDown != keyWasDown || focusKeyDown != focusKeyWasDown) {
            PacketDistributor.sendToServer(
                    new SpiritualSensePayloads.ServerboundSetSpiritualSensePayload(
                            keyDown, focusKeyDown));
            keyWasDown = keyDown;
            focusKeyWasDown = focusKeyDown;
            if (!keyDown) {
                active = false;
                targetRadius = 0.0F;
                focus = 0.0F;
                directionalFocus = false;
                deactivationTicks = 0;
                unavailableShown = false;
            }
        }

        renderedRadius = Mth.lerp(active ? 0.34F : 0.16F,
                renderedRadius, targetRadius);
        fade = Mth.clamp(fade + (active ? 0.12F : -0.10F), 0.0F, 1.0F);
        focusFade = Mth.lerp(0.18F, focusFade,
                active && directionalFocus ? 1.0F : 0.0F);
        renderedAmbientQi = Mth.lerp(active ? 0.08F : 0.16F,
                renderedAmbientQi, active ? ambientQi : 0.0F);
        if (active) {
            activationTicks = Math.min(200, activationTicks + 1);
            deactivationTicks = 0;
        } else if (fade > 0.0F) {
            deactivationTicks++;
        }
        if (counterPressureTicks > 0) counterPressureTicks--;
        if (clashTicks > 0) clashTicks--;

        tickSignatureVisuals();
        updateFocusedSignature(minecraft);
        if (!active && fade <= 0.001F) {
            signatures = List.of();
            VISUALS.clear();
            focusedSignatureId = Integer.MIN_VALUE;
            ATMOSPHERIC_CELLS.clear();
            ATMOSPHERIC_FLOW_NODES.clear();
        }
    }

    private static void clear() {
        ATMOSPHERIC_CELLS.clear();
        ATMOSPHERIC_FLOW_NODES.clear();
        keyWasDown = false;
        focusKeyWasDown = false;
        holding = false;
        active = false;
        directionalFocus = false;
        unavailableShown = false;
        targetRadius = 0.0F;
        renderedRadius = 0.0F;
        maxRadius = 0.0F;
        focus = 0.0F;
        detectionPower = 0.0F;
        ambientQi = 0;
        renderedAmbientQi = 0.0F;
        selfUsingQi = false;
        fade = 0.0F;
        focusFade = 0.0F;
        activationTicks = 0;
        deactivationTicks = 0;
        counterPressureTicks = 0;
        clashTicks = 0;
        focusedSignatureId = Integer.MIN_VALUE;
        signatures = List.of();
        VISUALS.clear();
    }

    private static void acceptSignatures(List<Signature> incoming) {
        Set<Integer> seen = new HashSet<>();
        for (Signature signature : incoming) {
            seen.add(signature.id());
            SignatureVisual visual = VISUALS.get(signature.id());
            boolean newlySensed = visual == null;
            if (visual == null) {
                visual = new SignatureVisual(signature, clientTicks);
                VISUALS.put(signature.id(), visual);
            }
            boolean counterWasVisible = !newlySensed && visual.hasFlag(
                    SpiritualSenseSystem.FLAG_COUNTER_PRESSURE);
            boolean clashWasVisible = !newlySensed && visual.hasFlag(
                    SpiritualSenseSystem.FLAG_SENSE_ACTIVE);
            visual.accept(signature, clientTicks);
            if (!counterWasVisible && visual.hasFlag(
                    SpiritualSenseSystem.FLAG_COUNTER_PRESSURE)) {
                counterPressureTicks = Math.max(counterPressureTicks, 26);
            }
            if (!clashWasVisible && visual.hasFlag(
                    SpiritualSenseSystem.FLAG_SENSE_ACTIVE)) {
                clashTicks = Math.max(clashTicks, 34);
            }
        }
        for (SignatureVisual visual : VISUALS.values()) {
            if (!seen.contains(visual.signature.id())) visual.missingTicks++;
        }
    }

    private static void tickSignatureVisuals() {
        for (SignatureVisual visual : VISUALS.values()) {
            visual.tick();
        }
        VISUALS.entrySet().removeIf(entry ->
                entry.getValue().missingTicks > 24
                        || clientTicks - entry.getValue().lastSeenTick > 40);
    }

    private static void updateFocusedSignature(Minecraft minecraft) {
        if (!active || !directionalFocus || minecraft.player == null) {
            focusedSignatureId = Integer.MIN_VALUE;
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle();
        double bestScore = 0.90D;
        int bestId = Integer.MIN_VALUE;
        for (Signature signature : signatures) {
            if (signature.tier() < SpiritualSenseSystem.TIER_IDENTIFICATION
                    || signature.kind()
                            == SpiritualSenseSystem.KIND_FORMATION_NODE) {
                continue;
            }
            Vec3 direction = new Vec3(signature.x(), signature.y(), signature.z())
                    .subtract(eye);
            if (direction.lengthSqr() < 1.0E-5D) continue;
            double alignment = look.dot(direction.normalize());
            double score = alignment - Math.min(0.035D,
                    signature.distance() * 0.00012D);
            if (score > bestScore) {
                bestScore = score;
                bestId = signature.id();
            }
        }
        focusedSignatureId = bestId;
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || fade <= 0.01F || renderedRadius <= 0.05F
                || ShaderPackCompatibility.isRenderingShadowPass()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        // Save the exact matrices Minecraft used for this world frame. HUD
        // markers are rendered later, after the world pass, and must use this
        // projection rather than an approximate yaw/FOV conversion.
        WORLD_TO_CLIP.set(event.getProjectionMatrix())
                .mul(event.getModelViewMatrix());
        projectionCameraPosition = cameraPosition;
        projectionReady = true;
        float partialTick = event.getPartialTick()
                .getGameTimeDeltaPartialTick(true);
        Vec3 playerPosition = new Vec3(
                Mth.lerp(partialTick, minecraft.player.xo, minecraft.player.getX()),
                Mth.lerp(partialTick, minecraft.player.yo, minecraft.player.getY())
                        + minecraft.player.getBbHeight() * 0.5D,
                Mth.lerp(partialTick, minecraft.player.zo, minecraft.player.getZ()));
        MultiBufferSource.BufferSource buffers = SENSE_BUFFERS;
        float time = minecraft.level.getGameTime() + partialTick;

        WorldRenderState previous = WorldRenderState.capture();
        try {
            if (ClientData.showSpiritualSenseQiCurrents()) {
                VertexConsumer airFlowVertices =
                        buffers.getBuffer(AIR_FLOW_RENDER_TYPE);
                renderAtmosphericQiFlow(poseStack, airFlowVertices,
                        playerPosition, cameraPosition, time);
                buffers.endBatch(AIR_FLOW_RENDER_TYPE);
            }

            VertexConsumer vertices = buffers.getBuffer(SENSE_RENDER_TYPE);
            renderExpansionField(poseStack, vertices, playerPosition,
                    cameraPosition, renderedRadius, time);
            renderFocusedPerception(poseStack, vertices, camera,
                    playerPosition, cameraPosition, time);
            renderSignatures(poseStack, vertices, camera,
                    playerPosition, cameraPosition, time);
            buffers.endBatch(SENSE_RENDER_TYPE);
        } finally {
            previous.restore();
        }
    }

    /**
     * Absolute world-space atmospheric qi. Entity positions never create
     * streaks; active qi users only redirect currents already in nearby cells.
     */
    private static void renderAtmosphericQiFlow(
            PoseStack poseStack, VertexConsumer vertices,
            Vec3 center, Vec3 cameraPosition, float time) {
        float radius = Math.min(19.0F,
                Math.max(7.0F, renderedRadius * 0.58F));
        float verticalRadius = Math.min(7.5F,
                Math.max(4.0F, radius * 0.45F));
        float rawRichness = Mth.clamp(renderedAmbientQi / 100.0F,
                0.0F, 1.0F);
        // Perceptual scaling keeps a real Qi value such as 8 visible without
        // making low-Qi air look remotely as dense as a spirit vein.
        float richness = Mth.sqrt(rawRichness);
        if (fade <= 0.01F) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        // Thousands of cells interpolate the same few 24-block flow nodes.
        // Recompute each animated node once for this frame, preserving live motion.
        ATMOSPHERIC_FLOW_NODES.clear();
        List<QiAttractor> attractors = new ArrayList<>();
        if (selfUsingQi) {
            attractors.add(new QiAttractor(center, 1.0F));
        }
        for (Signature signature : signatures) {
            if ((signature.flags() & (SpiritualSenseSystem.FLAG_ACTIVE
                    | SpiritualSenseSystem.FLAG_CASTING)) == 0) continue;
            if (signature.kind() == SpiritualSenseSystem.KIND_VEIN
                    || signature.kind()
                            == SpiritualSenseSystem.KIND_FORMATION_NODE) {
                continue;
            }
            Vec3 position = new Vec3(
                    signature.x(), signature.y(), signature.z());
            if (position.distanceToSqr(center)
                    <= (radius + 12.0F) * (radius + 12.0F)) {
                float strength = signature.kind()
                        == SpiritualSenseSystem.KIND_FORMATION ? 0.72F : 1.0F;
                if ((signature.flags() & SpiritualSenseSystem
                        .FLAG_SUPPRESSED_QI_PARTICLES) != 0) {
                    strength *= 1.45F;
                }
                attractors.add(new QiAttractor(position, strength));
                if (attractors.size() >= 7) break;
            }
        }

        // Minecraft quantizes the final alpha to 8 bits. The former 0.01
        // base opacity became effectively transparent after the lifetime and
        // boundary fades, while a 0.007-block ribbon was sub-pixel at normal
        // viewing distances. These values keep low-qi air subtle but legible.
        float visibility = fade * (0.230F + richness * 0.140F);
        float selection = 0.40F + richness * 0.18F;
        Matrix4f matrix = poseStack.last().pose();

        for (AtmosphericQiCells.Cell cell : ATMOSPHERIC_CELLS.samples(minecraft.level, center, radius, verticalRadius)) {
            int seed = cell.seed;
            Vec3 origin = cell.origin;
            double offsetX = origin.x - center.x;
            double offsetY = origin.y - center.y;
            double offsetZ = origin.z - center.z;
            double horizontalDistanceSqr = offsetX * offsetX + offsetZ * offsetZ;
            if (horizontalDistanceSqr > radius * radius
                    || Math.abs(offsetY) > verticalRadius) continue;

            // Every candidate is still an absolute world cell. Qi use
            // only reveals more of those pre-existing cells nearby.
            float localDensity = 0.0F;
            for (QiAttractor attractor : attractors) {
                double distance = attractor.position()
                        .distanceTo(origin);
                if (distance > 12.0D) continue;
                float local = (1.0F - (float) distance / 12.0F)
                        * attractor.strength();
                localDensity = Math.max(localDensity, local * local);
            }
            if (cell.selectionNoise
                    > selection + localDensity * 0.18F) continue;
            if (!cell.isOpen(minecraft.level, clientTicks)) continue;

            Vec3 direction = atmosphericDirection(origin, time, seed);
            float attraction = 0.0F;
            for (QiAttractor attractor : attractors) {
                Vec3 inward = attractor.position().subtract(origin);
                double distance = inward.length();
                if (distance < 0.15D || distance > 11.0D) continue;
                float local = (1.0F - (float) distance / 11.0F)
                        * attractor.strength();
                local *= local;
                direction = direction.scale(1.0F - local * 0.88F)
                        .add(inward.scale(1.0D / distance)
                                .scale(local * 0.88F))
                        .normalize();
                attraction = Math.max(attraction, local);
            }

            float phase = Mth.frac(time
                    * (0.0062F + richness * 0.0014F
                            + attraction * 0.0055F)
                    + cell.phaseNoise);
            float lifeFade = Mth.sin(phase * Mth.PI);
            lifeFade *= lifeFade;
            float edgeFade = Mth.clamp(
                    (radius - (float) Math.sqrt(horizontalDistanceSqr))
                            / 3.0F,
                    0.0F, 1.0F);
            float alpha = visibility * lifeFade * edgeFade
                    * (1.0F + attraction * 1.25F);
            // A current passing through the camera otherwise expands across the
            // screen and makes temporal exposure/antialiasing pulse in shader packs.
            alpha *= Mth.clamp((float) origin.distanceTo(cameraPosition) - 1.5F, 0.0F, 1.0F);
            if (alpha <= 0.001F) continue;

            Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
            if (side.lengthSqr() < 1.0E-5D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            float travel = (phase - 0.5F)
                    * (1.25F + richness * 0.55F
                            + attraction * 1.20F);
            Vec3 start = origin.add(direction.scale(travel));
            float length = 0.90F
                    + cell.lengthNoise * 0.60F
                    + richness * 0.24F
                    + attraction * 0.34F;
            float curve = (cell.curveNoise - 0.5F)
                    * (0.15F + attraction * 0.12F);
            float width = 0.044F + richness * 0.014F;
            renderCurvedFlowStreak(vertices, matrix,
                    start, direction, side, length, curve,
                    width, alpha, cameraPosition);
        }
    }

    /**
     * Samples a cubic curve so a current eases into its lateral heading. The
     * previous two quads met at one visible corner and looked like an instant
     * turn whenever the flow field changed direction.
     */
    private static void renderCurvedFlowStreak(
            VertexConsumer vertices, Matrix4f matrix,
            Vec3 start, Vec3 direction, Vec3 side,
            float length, float curve, float width, float alpha,
            Vec3 cameraPosition) {
        Vec3 controlOne = start.add(direction.scale(length * 0.34F));
        Vec3 controlTwo = start.add(direction.scale(length * 0.70F))
                .add(side.scale(curve * 0.55F));
        Vec3 end = start.add(direction.scale(length))
                .add(side.scale(curve * 1.55F));
        final int segments = 5;
        Vec3[] points = new Vec3[segments + 1];
        points[0] = start;
        for (int point = 1; point <= segments; point++) {
            points[point] = cubicFlowPoint(start, controlOne, controlTwo, end,
                    point / (float) segments);
        }
        // Both layers share their curve. Keep haze before core and all five
        // segments so density, taper and translucent draw order stay unchanged.
        for (int layer = 0; layer < 2; layer++) {
            float layerWidth = layer == 0 ? width * 2.8F : width;
            float layerAlpha = layer == 0 ? alpha * 0.30F : alpha;
            for (int segment = 0; segment < segments; segment++) {
                float startTaper = Mth.sin(segment / (float) segments * Mth.PI);
                float endTaper = Mth.sin((segment + 1) / (float) segments * Mth.PI);
                renderTaperedFlowSegment(vertices, matrix,
                        points[segment], points[segment + 1], cameraPosition,
                        Math.max(0.0005F, layerWidth * startTaper),
                        Math.max(0.0005F, layerWidth * endTaper),
                        layerAlpha * startTaper, layerAlpha * endTaper);
            }
        }
    }

    private static Vec3 cubicFlowPoint(
            Vec3 start, Vec3 controlOne, Vec3 controlTwo, Vec3 end,
            float progress) {
        double inverse = 1.0D - progress;
        double a = inverse * inverse * inverse;
        double b = 3.0D * inverse * inverse * progress;
        double c = 3.0D * inverse * progress * progress;
        double d = progress * progress * progress;
        return new Vec3(start.x * a + controlOne.x * b + controlTwo.x * c + end.x * d,
                start.y * a + controlOne.y * b + controlTwo.y * c + end.y * d,
                start.z * a + controlOne.z * b + controlTwo.z * c + end.z * d);
    }

    private static Vec3 atmosphericDirection(
            Vec3 position, float time, int cellSeed) {
        final double scale = 24.0D;
        double scaledX = position.x / scale;
        double scaledZ = position.z / scale;
        int gridX = Mth.floor(scaledX);
        int gridZ = Mth.floor(scaledZ);
        float blendX = smoothFlowBlend((float) (scaledX - gridX));
        float blendZ = smoothFlowBlend((float) (scaledZ - gridZ));
        Vec3 north = atmosphericFlowNode(gridX, gridZ, time).lerp(
                atmosphericFlowNode(gridX + 1, gridZ, time), blendX);
        Vec3 south = atmosphericFlowNode(gridX, gridZ + 1, time).lerp(
                atmosphericFlowNode(gridX + 1, gridZ + 1, time), blendX);
        Vec3 field = north.lerp(south, blendZ).normalize();
        float angle = (float) Math.atan2(field.z, field.x)
                + (veinNoise(cellSeed, 0, 8) - 0.5F) * 0.20F;
        return new Vec3(Mth.cos(angle), field.y, Mth.sin(angle)).normalize();
    }

    private static Vec3 atmosphericFlowNode(
            int gridX, int gridZ, float time) {
        long key = (long) gridX << 32 | (gridZ & 0xFFFFFFFFL);
        Vec3 cached = ATMOSPHERIC_FLOW_NODES.get(key);
        if (cached != null) return cached;
        int seed = mixVeinSeed(gridX * 0x27D4EB2D
                ^ gridZ * 0x165667B1);
        float angle = veinNoise(seed, 0, 7) * Mth.TWO_PI
                + Mth.sin(time * 0.0014F + seed * 0.000011F) * 0.12F;
        float vertical = (veinNoise(seed, 0, 9) - 0.5F) * 0.075F;
        Vec3 direction = new Vec3(Mth.cos(angle), vertical, Mth.sin(angle)).normalize();
        ATMOSPHERIC_FLOW_NODES.put(key, direction);
        return direction;
    }

    private static float smoothFlowBlend(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static void renderTaperedFlowSegment(
            VertexConsumer vertices, Matrix4f matrix,
            Vec3 startWorld, Vec3 endWorld, Vec3 cameraPosition,
            float startWidth, float endWidth,
            float startAlpha, float endAlpha) {
        double dx = endWorld.x - startWorld.x, dy = endWorld.y - startWorld.y, dz = endWorld.z - startWorld.z;
        if (dx * dx + dy * dy + dz * dz < 1.0E-6D) return;
        double mx = cameraPosition.x - (startWorld.x + endWorld.x) * .5;
        double my = cameraPosition.y - (startWorld.y + endWorld.y) * .5;
        double mz = cameraPosition.z - (startWorld.z + endWorld.z) * .5;
        double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        if (sx * sx + sy * sy + sz * sz < 1.0E-6D) { sx = -dz; sy = 0; sz = dx; }
        double lengthSquared = sx * sx + sy * sy + sz * sz;
        if (lengthSquared < 1.0E-6D) return;
        double length = Math.sqrt(lengthSquared);
        sx /= length; sy /= length; sz /= length;
        double ax = startWorld.x - cameraPosition.x, ay = startWorld.y - cameraPosition.y, az = startWorld.z - cameraPosition.z;
        double bx = endWorld.x - cameraPosition.x, by = endWorld.y - cameraPosition.y, bz = endWorld.z - cameraPosition.z;
        trailVertex(vertices, matrix, ax + sx * startWidth, ay + sy * startWidth, az + sz * startWidth,
                0, 0, AIR_FLOW_COLOR, startAlpha);
        trailVertex(vertices, matrix, ax - sx * startWidth, ay - sy * startWidth, az - sz * startWidth,
                1, 0, AIR_FLOW_COLOR, startAlpha);
        trailVertex(vertices, matrix, bx - sx * endWidth, by - sy * endWidth, bz - sz * endWidth,
                1, 1, AIR_FLOW_COLOR, endAlpha);
        trailVertex(vertices, matrix, bx + sx * endWidth, by + sy * endWidth, bz + sz * endWidth,
                0, 1, AIR_FLOW_COLOR, endAlpha);
    }

    private record QiAttractor(Vec3 position, float strength) {
    }


    private static void renderExpansionField(
            PoseStack poseStack, VertexConsumer vertices,
            Vec3 center, Vec3 cameraPosition, float radius, float time) {
        poseStack.pushPose();
        poseStack.translate(center.x - cameraPosition.x,
                center.y - cameraPosition.y,
                center.z - cameraPosition.z);
        float authority = Mth.clamp((maxRadius - 35.0F) / 115.0F,
                0.0F, 1.0F);
        float pulse = 0.72F + 0.28F * Mth.sin(time * 0.16F);
        // Keep the limit readable without turning it into an opaque wall. The
        // broken, slowly breathing wire sphere marks the CURRENT expanded
        // radius, so the player can also watch spiritual sense spread outward.
        float alpha = (0.105F + focus * 0.035F + authority * 0.025F)
                * fade * pulse;
        Matrix4f matrix = poseStack.last().pose();
        float gridWidth = Mth.clamp(radius * 0.0019F,
                0.050F, 0.20F);
        for (int latitude = -4; latitude <= 4; latitude++) {
            float latitudeAngle = latitude * Mth.PI / 10.0F;
            float y = Mth.sin(latitudeAngle) * radius;
            float ringRadius = Mth.cos(latitudeAngle) * radius;
            renderRing(vertices, matrix, ringRadius, y,
                    gridWidth, 96,
                    latitude % 2 == 0 ? SENSE_CYAN : SENSE_VIOLET,
                    alpha * (latitude == 0 ? 1.35F : 0.88F));
        }
        for (int meridian = 0; meridian < 6; meridian++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(meridian * 30.0F
                    + time * (0.10F + authority * 0.08F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            renderRing(vertices, poseStack.last().pose(), radius, 0.0F,
                    gridWidth * 0.90F, 96,
                    meridian % 2 == 0 ? SENSE_VIOLET : SENSE_CYAN,
                    alpha * 0.72F);
            poseStack.popPose();
        }

        float boundaryWidth = Mth.clamp(radius * 0.0028F,
                0.075F, 0.30F);
        float boundaryAlpha = (0.180F + focus * 0.045F
                + authority * 0.030F) * fade
                * (0.82F + 0.18F * Mth.sin(time * 0.11F));
        renderBrokenRing(vertices, matrix, radius, boundaryWidth,
                time * 0.18F, SENSE_CYAN.mix(SENSE_VIOLET, 0.28F),
                boundaryAlpha, 29);
        renderBrokenRing(vertices, matrix, radius * 0.992F,
                boundaryWidth * 0.52F, -time * 0.11F,
                SENSE_VIOLET.mix(SENSE_CYAN, 0.42F),
                boundaryAlpha * 0.62F, 43);
        for (int arc = 0; arc < 4; arc++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(20.0F + arc * 43.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(
                    arc % 2 == 0 ? 62.0F : 78.0F));
            renderBrokenRing(vertices, poseStack.last().pose(), radius,
                    boundaryWidth, -time * (0.13F + arc * 0.025F),
                    SENSE_VIOLET.mix(SENSE_CYAN, 0.55F),
                    boundaryAlpha * 0.82F, 31 + arc * 7);
            poseStack.popPose();
        }

        float wave = Mth.frac(time / (30.0F - authority * 8.0F));
        renderRing(vertices, matrix, radius * wave, 0.0F,
                0.055F + 0.10F * (1.0F - wave), 72,
                SENSE_CYAN.mix(SENSE_GOLD, authority * 0.28F),
                fade * (0.14F + authority * 0.08F) * (1.0F - wave));

        // Broken qi-script fragments ride just behind the wavefront instead of
        // making the pulse resemble a technological scan grid.
        float scriptRadius = Math.max(0.2F, radius * wave * 0.985F);
        renderBrokenRing(vertices, matrix, scriptRadius,
                0.025F + authority * 0.018F,
                time * 0.24F, SENSE_VIOLET.mix(SENSE_CYAN, 0.62F),
                fade * 0.08F * (1.0F - wave), 17);
        poseStack.popPose();
    }

    private static void renderFocusedPerception(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 center, Vec3 cameraPosition, float time) {
        if (focusFade <= 0.015F) return;
        Vec3 look = Minecraft.getInstance().player == null
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : Minecraft.getInstance().player.getLookAngle();
        float reach = Math.min(28.0F, Math.max(8.0F, renderedRadius * 0.42F));
        for (int ring = 1; ring <= 7; ring++) {
            float progress = ring / 7.0F;
            Vec3 position = center.add(look.scale(reach * progress));
            poseStack.pushPose();
            poseStack.translate(position.x - cameraPosition.x,
                    position.y - cameraPosition.y,
                    position.z - cameraPosition.z);
            poseStack.mulPose(camera.rotation());
            float radius = 0.30F + progress * 1.45F
                    + Mth.sin(time * 0.13F + ring) * 0.05F;
            renderBillboardRing(vertices, poseStack.last().pose(), radius,
                    0.025F, 32, SENSE_GOLD.mix(SENSE_CYAN, progress),
                    focusFade * fade * (0.12F - progress * 0.010F));
            poseStack.popPose();
        }
    }

    private static void renderSignatures(
            PoseStack poseStack, VertexConsumer vertices,
            Camera camera, Vec3 playerPosition,
            Vec3 cameraPosition, float time) {
        Map<Integer, Signature> formationCenters = new HashMap<>();
        for (Signature signature : signatures) {
            if (signature.kind() == SpiritualSenseSystem.KIND_FORMATION) {
                formationCenters.put(signature.groupId(), signature);
            }
        }

        for (Signature signature : signatures) {
            SignatureVisual visual = VISUALS.get(signature.id());
            if (visual == null) continue;
            Color color = colorFor(signature);
            Vec3 position = new Vec3(signature.x(), signature.y(), signature.z());
            if ((signature.flags()
                    & SpiritualSenseSystem.FLAG_TALISMAN_MARK) != 0) {
                renderTalismanMark(poseStack, vertices, camera,
                        playerPosition, position, cameraPosition,
                        signature, time);
            }
            if (signature.tier() < SpiritualSenseSystem.TIER_IDENTIFICATION) {
                if ((signature.flags() & SpiritualSenseSystem
                        .FLAG_SUPPRESSED_QI_PARTICLES) != 0) {
                    renderSuppressedQiPresence(poseStack, vertices, camera,
                            position, cameraPosition, color, signature, time);
                }
                continue;
            }
            renderResponseRipple(poseStack, vertices, camera, position,
                    cameraPosition, color, visual, time);
            if (signature.kind() == SpiritualSenseSystem.KIND_FORMATION) {
                renderFormation(poseStack, vertices, position,
                        cameraPosition, color, signature, visual, time);
            } else if (signature.kind() == SpiritualSenseSystem.KIND_FORMATION_NODE) {
                renderFormationNode(poseStack, vertices, camera,
                        position, cameraPosition, color, time,
                        signature.groupId());
                Signature center = formationCenters.get(signature.groupId());
                if (center != null) {
                    Vec3 centerPosition = new Vec3(
                            center.x(), center.y() + 0.08D, center.z());
                    Vec3 nodePosition = position.add(0.0D, 0.08D, 0.0D);
                    renderConnection(vertices, poseStack.last().pose(),
                            centerPosition, nodePosition,
                            cameraPosition, color, 0.09F * fade);
                    renderConnectionMote(poseStack, vertices, camera,
                            centerPosition, nodePosition, cameraPosition,
                            color, time, signature.id());
                }
            } else if (signature.kind() == SpiritualSenseSystem.KIND_VEIN) {
                renderVeinFlow(poseStack, vertices, position,
                        cameraPosition, color, signature, visual, time);
            } else if (isLivingKind(signature.kind())
                    && signature.tier()
                            >= SpiritualSenseSystem.TIER_INSIGHT) {
                renderReconstructionAccents(poseStack, vertices, camera,
                        position, cameraPosition, color, signature,
                        visual, time);
            } else {
                renderAura(poseStack, vertices, camera,
                        position, cameraPosition, color, signature,
                        visual, time);
            }
            if ((signature.flags()
                    & SpiritualSenseSystem.FLAG_SENSE_ACTIVE) != 0) {
                renderSenseClash(poseStack, vertices, camera,
                        playerPosition, position, cameraPosition,
                        color, signature, time);
            }
        }
    }

    private static void renderTalismanMark(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 playerPosition, Vec3 sensedPosition,
            Vec3 cameraPosition, Signature signature, float time) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity target = minecraft.level == null
                ? null : minecraft.level.getEntity(signature.id());
        double topOffset = target == null ? 1.05D
                : target.getBbHeight() * 0.5D + 0.34D;
        Vec3 markPosition = sensedPosition.add(0.0D, topOffset, 0.0D);
        boolean interference = (signature.flags()
                & SpiritualSenseSystem.FLAG_INTERFERENCE) != 0;
        boolean distant = signature.distance() > 250.0F;
        float unstable = interference || distant
                ? 0.48F + 0.52F * Math.abs(Mth.sin(
                        time * 0.47F + signature.id() * 0.37F))
                : 1.0F;
        float rangeFade = Mth.clamp(
                (305.0F - signature.distance()) / 55.0F,
                0.25F, 1.0F);
        float pulse = 0.84F + 0.16F * Mth.sin(
                time * 0.18F + signature.id() * 0.19F);
        float alpha = fade * unstable * rangeFade;
        Color gold = SENSE_GOLD.mix(SENSE_CYAN, 0.16F);

        // A barely-there directional thread becomes intermittent near the
        // range limit or through spiritual interference.
        if (!interference || unstable > 0.58F) {
            renderConnection(vertices, poseStack.last().pose(),
                    playerPosition.add(0.0D, 0.85D, 0.0D), markPosition,
                    cameraPosition, gold,
                    alpha * (distant ? 0.035F : 0.020F));
        }

        poseStack.pushPose();
        poseStack.translate(markPosition.x - cameraPosition.x,
                markPosition.y - cameraPosition.y,
                markPosition.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        Matrix4f matrix = poseStack.last().pose();
        float radius = 0.25F * pulse;
        renderBillboardRing(vertices, matrix,
                radius, 0.020F, 30,
                gold, alpha * 0.88F);
        renderBrokenBillboardRing(vertices, matrix,
                radius * 1.42F, 0.015F,
                -time * 0.22F, SENSE_CYAN.mix(gold, 0.52F),
                alpha * 0.54F, signature.id() + 131);

        // Four fine compass strokes and a contracting center form a 符印
        // without covering the target with a thick icon.
        float stroke = 0.014F;
        float inner = radius * 0.48F;
        float outer = radius * 1.28F;
        quad(vertices, matrix, -stroke, inner,
                stroke, outer, gold, alpha * 0.78F);
        quad(vertices, matrix, -stroke, -outer,
                stroke, -inner, gold, alpha * 0.78F);
        quad(vertices, matrix, inner, -stroke,
                outer, stroke, gold, alpha * 0.78F);
        quad(vertices, matrix, -outer, -stroke,
                -inner, stroke, gold, alpha * 0.78F);
        float core = 0.042F + 0.015F * pulse;
        quad(vertices, matrix, -core, -core,
                core, core, SENSE_CYAN.mix(Color.WHITE, 0.42F),
                alpha * 0.62F);
        poseStack.popPose();
    }

    /**
     * Geometry is supplied by the entity shader pass. These are only the
     * non-body cues that sell detection: dantian, reveal instability, savage
     * leakage, movement lag and oppressive realm pressure.
     */
    private static void renderReconstructionAccents(
            PoseStack poseStack, VertexConsumer vertices,
            Camera camera, Vec3 position, Vec3 cameraPosition,
            Color color, Signature signature,
            SignatureVisual visual, float time) {
        boolean hostile = signature.kind()
                == SpiritualSenseSystem.KIND_HOSTILE;
        boolean cultivator = signature.kind()
                == SpiritualSenseSystem.KIND_CULTIVATOR;
        boolean concealed = (signature.flags()
                & SpiritualSenseSystem.FLAG_CONCEALED) != 0;
        boolean pressure = (signature.flags()
                & SpiritualSenseSystem.FLAG_PRESSURE) != 0;
        boolean suppressedQiParticles = (signature.flags()
                & SpiritualSenseSystem.FLAG_SUPPRESSED_QI_PARTICLES) != 0;
        float reveal = visual.reveal(clientTicks, concealed);
        float size = Mth.clamp(0.50F + signature.intensity() * 0.22F,
                0.50F, 1.45F);
        float pulse = 0.72F + 0.28F * Mth.sin(
                time * (hostile ? 0.31F : 0.17F)
                        + signature.id() * 0.37F);
        float alpha = fade * reveal * pulse;

        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y,
                position.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        Matrix4f matrix = poseStack.last().pose();

        if (cultivator) {
            float coreRadius = size * (0.095F
                    + signature.intensity() * 0.018F);
            Color core = SENSE_GOLD.mix(color, 0.40F);
            renderBillboardRing(vertices, matrix,
                    coreRadius * (1.0F + pulse * 0.10F),
                    coreRadius * 0.34F, 28,
                    core.mix(Color.WHITE, 0.38F), alpha * 0.72F);
            quad(vertices, matrix,
                    -coreRadius * 0.34F, -coreRadius * 0.34F,
                    coreRadius * 0.34F, coreRadius * 0.34F,
                    Color.WHITE.mix(core, 0.40F), alpha * 0.46F);
            for (int channel = -1; channel <= 1; channel += 2) {
                float x = channel * size * 0.16F;
                quad(vertices, matrix,
                        x - size * 0.010F, -size * 0.26F,
                        x + size * 0.010F, size * 0.28F,
                        color.mix(SENSE_GOLD, 0.34F), alpha * 0.22F);
            }
            if (suppressedQiParticles) {
                renderSuppressedQiMotes(vertices, matrix, size, color,
                        alpha, time, signature.id());
            }
        } else if (hostile) {
            for (int leak = 0; leak < 3; leak++) {
                float radius = size * (0.64F + leak * 0.15F
                        + Mth.sin(time * 0.24F + leak * 2.1F) * 0.05F);
                renderBrokenBillboardRing(vertices, matrix,
                        radius, 0.025F, time * (1.0F + leak * 0.27F),
                        color.mix(SENSE_VIOLET, 0.28F),
                        alpha * (0.12F / (leak + 1.0F)),
                        signature.id() + leak * 19);
            }
        }
        if ((signature.flags() & SpiritualSenseSystem.FLAG_MOVING) != 0) {
            for (int echo = 1; echo <= 2; echo++) {
                float direction = visual.motionVector.lengthSqr() > 0.001D
                        ? (float) Math.atan2(
                                visual.motionVector.y,
                                Math.sqrt(visual.motionVector.x
                                        * visual.motionVector.x
                                        + visual.motionVector.z
                                                * visual.motionVector.z))
                        : 0.0F;
                renderBrokenBillboardRing(vertices, matrix,
                        size * (0.58F + echo * 0.17F),
                        0.018F, direction + time * 0.30F,
                        color, alpha * (0.075F / echo),
                        signature.id() + echo * 31);
            }
        }

        if (concealed && reveal < 0.96F) {
            renderBrokenBillboardRing(vertices, matrix,
                    size * (0.72F + (1.0F - reveal) * 0.25F),
                    0.032F, -time * 1.2F,
                    SENSE_VIOLET.mix(color, 0.42F),
                    alpha * (1.0F - reveal) * 0.50F,
                    signature.id() + 79);
        }
        if (pressure) {
            for (int halo = 1; halo <= 3; halo++) {
                renderBillboardRing(vertices, matrix,
                        size * (0.86F + halo * 0.28F
                                + Mth.sin(time * 0.075F + halo) * 0.05F),
                        0.030F + halo * 0.006F, 44,
                        color.mix(SENSE_VIOLET, 0.36F),
                        alpha * (0.17F / halo));
            }
        }
        poseStack.popPose();
    }

    /**
     * Presence-tier perception remains deliberately approximate. The motes
     * appear at the server-provided perceived position, not the entity's exact
     * coordinates, so hidden particles cannot become a free wallhack.
     */
    private static void renderSuppressedQiPresence(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 position, Vec3 cameraPosition, Color color,
            Signature signature, float time) {
        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y,
                position.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        Matrix4f matrix = poseStack.last().pose();
        float pulse = 0.72F + 0.28F * Mth.sin(
                time * 0.19F + signature.id() * 0.41F);
        renderSuppressedQiMotes(vertices, matrix, 0.82F, color,
                fade * pulse * 0.62F, time, signature.id());
        renderBrokenBillboardRing(vertices, matrix,
                0.40F + pulse * 0.08F, 0.020F,
                -time * 0.35F, color.mix(SENSE_GOLD, 0.38F),
                fade * pulse * 0.12F, signature.id() + 137);
        poseStack.popPose();
    }

    /** Spiritual-only traces of Qi whose ordinary particles were suppressed. */
    private static void renderSuppressedQiMotes(
            VertexConsumer vertices, Matrix4f matrix,
            float size, Color color, float alpha,
            float time, int seed) {
        Color qiColor = color.mix(SENSE_GOLD, 0.28F)
                .mix(Color.WHITE, 0.16F);
        for (int mote = 0; mote < 7; mote++) {
            float offset = Mth.frac(seed * 0.071F + mote * 0.143F);
            float phase = Mth.frac(time * 0.025F + offset);
            float angle = mote * (Mth.TWO_PI / 7.0F)
                    + seed * 0.17F + phase * 0.65F;
            float radius = size * Mth.lerp(phase, 0.72F, 0.12F);
            float x = Mth.cos(angle) * radius;
            float y = size * (-0.42F + mote * 0.14F)
                    + Mth.sin(angle * 1.7F) * size * 0.08F;
            float moteFade = Mth.sin(phase * Mth.PI);
            float moteSize = size * (0.018F + (1.0F - phase) * 0.010F);
            quad(vertices, matrix,
                    x - moteSize, y - moteSize,
                    x + moteSize, y + moteSize,
                    qiColor, alpha * moteFade * 0.78F);
        }
    }

    private static void renderAura(
            PoseStack poseStack, VertexConsumer vertices,
            Camera camera, Vec3 position, Vec3 cameraPosition,
            Color color, Signature signature,
            SignatureVisual visual, float time) {
        float size = Mth.clamp(0.48F + signature.intensity() * 0.22F,
                0.48F, 1.35F);
        boolean hostile = signature.kind() == SpiritualSenseSystem.KIND_HOSTILE;
        boolean cultivator = signature.kind()
                == SpiritualSenseSystem.KIND_CULTIVATOR;
        boolean concealed = (signature.flags()
                & SpiritualSenseSystem.FLAG_CONCEALED) != 0;
        boolean interfered = (signature.flags()
                & SpiritualSenseSystem.FLAG_INTERFERENCE) != 0;
        boolean denseInterference = (signature.flags()
                & SpiritualSenseSystem.FLAG_DENSE_INTERFERENCE) != 0;
        boolean pressure = (signature.flags()
                & SpiritualSenseSystem.FLAG_PRESSURE) != 0;
        boolean suppressedQiParticles = (signature.flags()
                & SpiritualSenseSystem.FLAG_SUPPRESSED_QI_PARTICLES) != 0;
        float reveal = visual.reveal(clientTicks, concealed);
        float rhythm = hostile ? 0.29F : 0.16F;
        float pulse = 0.74F + 0.26F
                * Mth.sin(time * rhythm + signature.id() * 0.37F);
        if (hostile) {
            pulse *= 0.88F + 0.12F * Mth.sin(
                    time * 0.71F + signature.id() * 1.91F);
        }
        float interferenceAlpha = denseInterference ? 0.52F
                : interfered ? 0.76F : 1.0F;
        if (signature.kind() == SpiritualSenseSystem.KIND_TREASURE) {
            renderTreasureAura(poseStack, vertices, camera, position,
                    cameraPosition, color, signature, visual,
                    size, reveal * interferenceAlpha, time);
            return;
        }

        int ghosts = signature.tier() >= SpiritualSenseSystem.TIER_INSIGHT ? 3 : 2;
        if ((signature.flags() & SpiritualSenseSystem.FLAG_MOVING) != 0) {
            ghosts++;
        }
        for (int ghost = ghosts - 1; ghost >= 0; ghost--) {
            poseStack.pushPose();
            float drift = ghost * (hostile ? 0.085F : 0.055F);
            Vec3 motionTrail = visual.motionVector.scale(-ghost * 0.32D);
            poseStack.translate(
                    position.x - cameraPosition.x
                            + motionTrail.x
                            + Mth.sin(time * 0.09F + signature.id()) * drift,
                    position.y - cameraPosition.y
                            + motionTrail.y
                            + Mth.sin(time * 0.13F + ghost) * 0.05F,
                    position.z - cameraPosition.z
                            + motionTrail.z
                            + Mth.cos(time * 0.08F + signature.id()) * drift);
            poseStack.mulPose(camera.rotation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    ghost == 0 ? 0.0F
                            : (ghost % 2 == 0 ? 4.0F : -4.0F)
                                    * (hostile ? 1.65F : 1.0F)));
            Matrix4f matrix = poseStack.last().pose();
            float alpha = fade * pulse * reveal * interferenceAlpha
                    * (ghost == 0 ? 0.22F : 0.070F / ghost);
            if (denseInterference) {
                alpha *= 0.72F + 0.28F * Mth.sin(
                        time * 0.88F + ghost * 2.4F + signature.id());
            }

            if (concealed && reveal < 0.34F) {
                renderBillboardRing(vertices, matrix,
                        size * (0.55F + reveal), 0.05F, 28,
                        color.mix(SENSE_VIOLET, 0.45F), alpha * 1.8F);
                quad(vertices, matrix, -size * 0.20F, -size * 0.48F,
                        size * 0.20F, size * 0.45F,
                        color, alpha * 0.42F);
                poseStack.popPose();
                continue;
            }

            // A soft humanoid soul-shape: head, torso, limbs and a denser
            // dantian point. It remains an abstract qi perception, never a
            // Minecraft glowing outline.
            quad(vertices, matrix, -size * 0.27F, -size * 0.34F,
                    size * 0.27F, size * 0.28F, color, alpha);
            quad(vertices, matrix, -size * 0.16F, size * 0.36F,
                    size * 0.16F, size * 0.68F,
                    color.mix(Color.WHITE, 0.38F), alpha * 1.18F);
            if (!denseInterference || Math.floorMod(
                    signature.id() + ghost + (int) time, 3) != 0) {
                quad(vertices, matrix, -size * 0.49F, -size * 0.28F,
                        -size * 0.31F, size * 0.20F,
                        color, alpha * 0.72F);
                quad(vertices, matrix, size * 0.31F, -size * 0.28F,
                        size * 0.49F, size * 0.20F,
                        color, alpha * 0.72F);
                quad(vertices, matrix, -size * 0.24F, -size * 0.72F,
                        -size * 0.055F, -size * 0.34F,
                        color, alpha * 0.74F);
                quad(vertices, matrix, size * 0.055F, -size * 0.72F,
                        size * 0.24F, -size * 0.34F,
                        color, alpha * 0.74F);
            }

            if (ghost == 0) {
                float auraRadius = size * (0.70F + pulse * 0.16F);
                renderBillboardRing(vertices, matrix, auraRadius,
                        0.025F + signature.intensity() * 0.008F, 38,
                        color, alpha * (hostile ? 0.72F : 0.48F));
                float coreRadius = size * (cultivator ? 0.13F : 0.075F);
                renderBillboardRing(vertices, matrix, coreRadius,
                        0.035F, 24,
                        cultivator ? SENSE_GOLD.mix(color, 0.45F) : color,
                        alpha * (cultivator ? 2.5F : 1.55F));
                if (cultivator) {
                    quad(vertices, matrix, -size * 0.025F, -size * 0.24F,
                            size * 0.025F, size * 0.43F,
                            color.mix(Color.WHITE, 0.55F), alpha * 1.2F);
                    for (int channel = -1; channel <= 1; channel += 2) {
                        quad(vertices, matrix,
                                channel * size * 0.19F - size * 0.018F,
                                -size * 0.27F,
                                channel * size * 0.19F + size * 0.018F,
                                size * 0.20F,
                                color.mix(SENSE_GOLD, 0.32F), alpha * 0.9F);
                    }
                    if (suppressedQiParticles) {
                        renderSuppressedQiMotes(vertices, matrix, size, color,
                                alpha * 2.1F, time, signature.id());
                    }
                }
                if (pressure) {
                    for (int halo = 1; halo <= 3; halo++) {
                        renderBillboardRing(vertices, matrix,
                                size * (0.88F + halo * 0.26F
                                        + Mth.sin(time * 0.08F + halo) * 0.04F),
                                0.035F, 42,
                                color.mix(SENSE_VIOLET, 0.35F),
                                alpha * (0.30F / halo));
                    }
                }
                if (visual.residueTicks > 0) {
                    float residue = visual.residueTicks / 32.0F;
                    renderBrokenBillboardRing(vertices, matrix,
                            size * (0.92F + (1.0F - residue) * 0.38F),
                            0.025F, time * 1.15F,
                            color.mix(SENSE_GOLD, 0.24F),
                            alpha * residue * 0.72F,
                            signature.id() + 31);
                }
                if (visual.spatialTraceTicks > 0) {
                    float trace = visual.spatialTraceTicks / 30.0F;
                    renderBrokenBillboardRing(vertices, matrix,
                            size * (0.55F + (1.0F - trace) * 0.85F),
                            0.022F, -time * 1.8F,
                            SENSE_VIOLET.mix(color, 0.28F),
                            alpha * trace * 0.95F,
                            signature.id() + 73);
                }
            }
            poseStack.popPose();
        }
    }

    private static void renderTreasureAura(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 position, Vec3 cameraPosition, Color color,
            Signature signature, SignatureVisual visual,
            float size, float reveal, float time) {
        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y
                        + Mth.sin(time * 0.12F + signature.id()) * 0.08F,
                position.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.55F
                + signature.id() * 7.0F));
        Matrix4f matrix = poseStack.last().pose();
        float alpha = fade * reveal * 0.30F;
        quad(vertices, matrix, -size * 0.25F, -size * 0.25F,
                size * 0.25F, size * 0.25F,
                color.mix(Color.WHITE, 0.45F), alpha);
        renderBillboardRing(vertices, matrix, size * 0.43F,
                0.035F, 32, color, alpha * 1.4F);
        renderBrokenBillboardRing(vertices, matrix, size * 0.68F,
                0.028F, -time * 0.9F,
                color.mix(SENSE_VIOLET, 0.25F), alpha * 0.75F,
                signature.id());
        if (signature.intensity() > 1.15F) {
            renderBillboardRing(vertices, matrix, size * 0.88F,
                    0.022F, 40, SENSE_GOLD, alpha * 0.45F);
        }
        poseStack.popPose();
    }

    private static void renderResponseRipple(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 position, Vec3 cameraPosition, Color color,
            SignatureVisual visual, float time) {
        if (visual.responseTicks <= 0) return;
        float progress = 1.0F - visual.responseTicks / 24.0F;
        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y,
                position.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        Matrix4f matrix = poseStack.last().pose();
        float alpha = fade * (1.0F - progress) * 0.38F;
        renderBillboardRing(vertices, matrix, 0.18F + progress * 1.9F,
                0.045F * (1.0F - progress) + 0.012F, 42,
                color.mix(Color.WHITE, 0.34F), alpha);
        renderBrokenBillboardRing(vertices, matrix,
                0.12F + progress * 1.35F, 0.022F,
                time * 0.7F, color, alpha * 0.55F,
                visual.signature.id());
        poseStack.popPose();
    }

    private static void renderSenseClash(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 playerPosition, Vec3 targetPosition, Vec3 cameraPosition,
            Color color, Signature signature, float time) {
        Vec3 midpoint = playerPosition.add(targetPosition).scale(0.5D);
        poseStack.pushPose();
        poseStack.translate(midpoint.x - cameraPosition.x,
                midpoint.y - cameraPosition.y,
                midpoint.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        Matrix4f matrix = poseStack.last().pose();
        float pulse = 0.58F + 0.42F * Mth.sin(
                time * 0.62F + signature.id());
        float alpha = fade * (0.08F + clashTicks / 34.0F * 0.18F) * pulse;
        for (int ring = 0; ring < 3; ring++) {
            renderBrokenBillboardRing(vertices, matrix,
                    0.65F + ring * 0.34F
                            + Mth.sin(time * 0.21F + ring) * 0.09F,
                    0.035F, (ring % 2 == 0 ? 1.0F : -1.0F)
                            * time * 1.25F,
                    ring == 1 ? SENSE_VIOLET : color.mix(Color.WHITE, 0.55F),
                    alpha / (ring + 1.0F), signature.id() + ring * 13);
        }
        poseStack.popPose();
    }

    private static void renderFormation(
            PoseStack poseStack, VertexConsumer vertices,
            Vec3 position, Vec3 cameraPosition, Color color,
            Signature signature, SignatureVisual visual, float time) {
        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y + 0.04D,
                position.z - cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();
        float scale = 1.7F + signature.intensity() * 0.75F;
        float reveal = visual.reveal(clientTicks, false);
        float nodeReveal = Mth.clamp(reveal * 2.4F, 0.0F, 1.0F);
        float lineReveal = Mth.clamp((reveal - 0.22F) * 1.9F, 0.0F, 1.0F);
        float geometryReveal = Mth.clamp((reveal - 0.48F) * 1.95F,
                0.0F, 1.0F);
        float interference = (signature.flags()
                & SpiritualSenseSystem.FLAG_DENSE_INTERFERENCE) != 0
                ? 0.50F : 1.0F;
        float alpha = fade * (signature.tier()
                >= SpiritualSenseSystem.TIER_INSIGHT ? 0.34F : 0.18F)
                * interference;
        renderBrokenRing(vertices, matrix, scale, 0.075F,
                time * 0.55F, color, alpha * lineReveal, signature.id());
        renderBrokenRing(vertices, matrix, scale * 0.62F, 0.052F,
                -time * 0.82F, color.mix(Color.WHITE, 0.35F),
                alpha * 0.72F * geometryReveal, signature.id() + 9);
        for (int node = 0; node < 6; node++) {
            float angle = time * 0.012F + node * Mth.TWO_PI / 6.0F;
            float x = Mth.cos(angle) * scale;
            float z = Mth.sin(angle) * scale;
            renderRing(vertices, matrix, 0.10F,
                    0.035F + node * 0.002F, 0.05F, 12,
                    color.mix(Color.WHITE, 0.5F),
                    alpha * 0.9F * nodeReveal,
                    x, z);
        }
        poseStack.popPose();
    }

    private static void renderFormationNode(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 position, Vec3 cameraPosition, Color color,
            float time, int seed) {
        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y
                        + Mth.sin(time * 0.18F + seed) * 0.08F,
                position.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        float size = 0.14F;
        quad(vertices, poseStack.last().pose(), -size, -size, size, size,
                color.mix(Color.WHITE, 0.42F), 0.42F * fade);
        poseStack.popPose();
    }

    private static void renderVeinFlow(
            PoseStack poseStack, VertexConsumer vertices,
            Vec3 position, Vec3 cameraPosition, Color color,
            Signature signature, SignatureVisual visual, float time) {
        Matrix4f matrix = poseStack.last().pose();
        float reveal = visual.reveal(clientTicks, false);
        int seed = mixVeinSeed(signature.groupId()
                ^ Integer.rotateLeft(signature.id(), 11)
                ^ veinPatternEpoch);
        int branchCount = 4 + Math.min(4,
                (int) (veinNoise(seed, -1, 0) * 5.0F));
        float rotation = veinNoise(seed, -2, 0) * Mth.TWO_PI;
        for (int branch = 0; branch < branchCount; branch++) {
            float spacing = Mth.TWO_PI / branchCount;
            float angleJitter = (veinNoise(seed, branch, 0) - 0.5F)
                    * spacing * 0.76F;
            float currentAngle = rotation + branch * spacing + angleJitter;
            int segmentCount = 6 + Math.min(6,
                    (int) (veinNoise(seed, branch, 1) * 7.0F));
            float baseStep = 0.58F + signature.intensity() * 0.18F
                    + veinNoise(seed, branch, 2) * 0.42F;
            float distance = 0.0F;
            Vec3 previous = position.add(0.0D, 0.08D, 0.0D);
            for (int segment = 1; segment <= segmentCount; segment++) {
                float stepVariation = Mth.lerp(
                        veinNoise(seed, branch, segment * 4),
                        0.68F, 1.34F);
                distance += baseStep * stepVariation;
                float randomTurn = (veinNoise(
                        seed, branch, segment * 4 + 1) - 0.5F) * 0.56F;
                float livingDrift = Mth.sin(time * 0.026F
                        + branch * 1.31F + segment * 0.67F
                        + veinNoise(seed, branch, 3) * 8.0F) * 0.035F;
                currentAngle += randomTurn + livingDrift;
                float height = (veinNoise(
                        seed, branch, segment * 4 + 2) - 0.5F) * 0.74F
                        + Mth.sin(segment * 0.61F
                        + veinNoise(seed, branch, 4) * Mth.TWO_PI)
                        * 0.10F;
                Vec3 next = position.add(
                        Mth.cos(currentAngle) * distance,
                        0.08F + height,
                        Mth.sin(currentAngle) * distance);
                float remaining = 1.0F
                        - segment / (float) (segmentCount + 2);
                renderConnection(vertices, matrix, previous, next,
                        cameraPosition, branch % 2 == 0
                                ? color : color.mix(Color.WHITE, 0.28F),
                        fade * reveal * (0.055F + remaining * 0.115F));

                // Some paths split into a short secondary root. Its location,
                // side, angle and length are all stable for this vein/session.
                boolean fork = segment >= 2 && segment < segmentCount - 1
                        && veinNoise(seed, branch, segment * 4 + 3) > 0.78F;
                if (fork) {
                    float side = veinNoise(seed, branch,
                            200 + segment) < 0.5F ? -1.0F : 1.0F;
                    float forkAngle = currentAngle + side * Mth.lerp(
                            veinNoise(seed, branch, 220 + segment),
                            0.48F, 1.10F);
                    int forkSegments = 2 + (int) (veinNoise(
                            seed, branch, 240 + segment) * 3.0F);
                    renderVeinFork(vertices, matrix, next, cameraPosition,
                            color.mix(Color.WHITE, 0.18F), seed, branch,
                            segment, forkAngle, baseStep * 0.68F,
                            forkSegments, fade * reveal * remaining * 0.105F);
                }
                previous = next;
            }
        }
    }

    private static void renderVeinFork(
            VertexConsumer vertices, Matrix4f matrix,
            Vec3 origin, Vec3 cameraPosition, Color color,
            int seed, int branch, int sourceSegment,
            float angle, float step, int segments, float alpha) {
        Vec3 previous = origin;
        float distance = 0.0F;
        for (int segment = 1; segment <= segments; segment++) {
            distance += step * Mth.lerp(veinNoise(seed,
                    branch + 31, sourceSegment * 17 + segment),
                    0.68F, 1.22F);
            angle += (veinNoise(seed, branch + 47,
                    sourceSegment * 19 + segment) - 0.5F) * 0.38F;
            double y = origin.y + (veinNoise(seed, branch + 59,
                    sourceSegment * 23 + segment) - 0.5F) * 0.38F;
            Vec3 next = new Vec3(
                    origin.x + Mth.cos(angle) * distance,
                    y,
                    origin.z + Mth.sin(angle) * distance);
            renderConnection(vertices, matrix, previous, next,
                    cameraPosition, color,
                    alpha * (1.0F - segment / (float) (segments + 1)));
            previous = next;
        }
    }

    static int mixVeinSeed(int value) {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ value >>> 16;
    }

    static float veinNoise(int seed, int branch, int segment) {
        int mixed = mixVeinSeed(seed
                ^ branch * 0x632BE5AB
                ^ segment * 0x85157AF5);
        return (mixed >>> 8 & 0x00FFFFFF) / 16777216.0F;
    }

    private static void renderConnection(
            VertexConsumer vertices, Matrix4f matrix,
            Vec3 startWorld, Vec3 endWorld, Vec3 cameraPosition,
            Color color, float alpha) {
        Vec3 direction = endWorld.subtract(startWorld);
        if (direction.lengthSqr() < 1.0E-6D) return;
        Vec3 midpoint = startWorld.add(endWorld).scale(0.5D);
        Vec3 side = direction.cross(cameraPosition.subtract(midpoint));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-6D) return;
        side = side.normalize().scale(0.045D);
        Vec3 start = startWorld.subtract(cameraPosition);
        Vec3 end = endWorld.subtract(cameraPosition);
        trailVertex(vertices, matrix, start.add(side), 0.0F, 0.0F,
                color, alpha);
        trailVertex(vertices, matrix, start.subtract(side), 1.0F, 0.0F,
                color, alpha);
        trailVertex(vertices, matrix, end.subtract(side), 1.0F, 1.0F,
                color, alpha * 0.72F);
        trailVertex(vertices, matrix, end.add(side), 0.0F, 1.0F,
                color, alpha * 0.72F);
    }

    private static void renderConnectionMote(
            PoseStack poseStack, VertexConsumer vertices, Camera camera,
            Vec3 start, Vec3 end, Vec3 cameraPosition,
            Color color, float time, int seed) {
        float progress = Mth.frac(time * 0.025F
                + Math.floorMod(seed, 97) / 97.0F);
        Vec3 position = start.lerp(end, progress);
        poseStack.pushPose();
        poseStack.translate(position.x - cameraPosition.x,
                position.y - cameraPosition.y,
                position.z - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        float size = 0.045F + 0.025F
                * Mth.sin(progress * Mth.PI);
        quad(vertices, poseStack.last().pose(), -size, -size, size, size,
                color.mix(Color.WHITE, 0.62F), 0.38F * fade);
        poseStack.popPose();
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        if (fade <= 0.01F) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        renderSpiritualScreen(graphics, minecraft, width, height);
        renderProjectedSignatures(graphics, minecraft, width, height);
        renderCompass(graphics, minecraft, width);
        renderNearestFeedback(graphics, minecraft, width, height);
    }

    private static void renderSpiritualScreen(
            GuiGraphics graphics, Minecraft minecraft,
            int width, int height) {
        float authority = Mth.clamp((maxRadius - 35.0F) / 115.0F,
                0.0F, 1.0F);
        int washAlpha = Mth.clamp(Math.round((8.0F + authority * 5.0F)
                * fade), 0, 18);
        graphics.fill(0, 0, width, height,
                washAlpha << 24 | 0x153142);

        // Layered soft vignette: mundane vision recedes without creating a
        // rectangular HUD frame.
        int layers = 9;
        for (int layer = 0; layer < layers; layer++) {
            float outward = 1.0F - layer / (float) layers;
            int alpha = Mth.clamp(Math.round((4.0F + outward * 9.0F)
                    * fade * (1.0F + focusFade * 0.55F)), 0, 24);
            int insetX = layer * Math.max(2, width / 220);
            int insetY = layer * Math.max(2, height / 160);
            int color = alpha << 24 | 0x050D18;
            graphics.fill(insetX, insetY,
                    width - insetX, insetY + 2, color);
            graphics.fill(insetX, height - insetY - 2,
                    width - insetX, height - insetY, color);
            graphics.fill(insetX, insetY + 2,
                    insetX + 2, height - insetY - 2, color);
            graphics.fill(width - insetX - 2, insetY + 2,
                    width - insetX, height - insetY - 2, color);
        }

        // Ambient qi fog follows slow curved paths. These sparse motes avoid
        // the straight scan-line language of a technological overlay.
        float time = minecraft.level.getGameTime();
        for (int stream = 0; stream < 7; stream++) {
            float phase = time * (0.010F + stream * 0.0013F)
                    + stream * 1.37F;
            for (int mote = 0; mote < 16; mote++) {
                float progress = mote / 15.0F;
                int x = Math.floorMod(Math.round(
                        progress * (width + 80)
                                + Mth.sin(phase + progress * 4.8F) * 28.0F
                                + stream * 71.0F), width + 80) - 40;
                int y = Math.round(height * (0.16F + stream * 0.115F)
                        + Mth.sin(phase * 0.73F + progress * 6.2F)
                                * (8.0F + stream));
                int alpha = Mth.clamp(Math.round(
                        (3.0F + 3.0F * Mth.sin(progress * Mth.PI)) * fade),
                        0, 8);
                graphics.fill(x, y, x + 2 + stream % 2, y + 1,
                        alpha << 24 | 0xA9F4FF);
            }
        }

        int centerX = width / 2;
        int centerY = height / 2;
        if (activationTicks < 30 && active) {
            float progress = activationTicks / 30.0F;
            float eased = smoothScreenStep(progress);
            int radius = Math.round(4.0F
                    + eased * Math.max(width, height) * 0.62F);
            int alpha = Mth.clamp(Math.round(
                    150.0F * fade * (1.0F - progress)), 0, 170);
            drawScreenRing(graphics, centerX, centerY, radius,
                    SENSE_CYAN.argb(alpha), 96, 0);
            drawScreenRing(graphics, centerX, centerY,
                    Math.max(2, Math.round(radius * 0.72F)),
                    SENSE_VIOLET.argb(Math.max(0, alpha / 3)), 72, 5);
        }
        if (!active && deactivationTicks > 0 && fade > 0.0F) {
            float progress = Mth.clamp(deactivationTicks / 12.0F,
                    0.0F, 1.0F);
            int radius = Math.round((1.0F - progress)
                    * Math.max(width, height) * 0.58F + 3.0F);
            drawScreenRing(graphics, centerX, centerY, radius,
                    SENSE_CYAN.argb(Mth.clamp(
                            Math.round(95.0F * fade), 0, 110)), 88, 3);
        }

        if (counterPressureTicks > 0 || clashTicks > 0) {
            float pressure = Math.max(counterPressureTicks / 26.0F,
                    clashTicks / 34.0F);
            int offset = Math.max(1, Math.round(pressure * 3.0F));
            int radius = Math.round(Math.min(width, height)
                    * (0.20F + pressure * 0.19F));
            drawScreenRing(graphics, centerX - offset, centerY,
                    radius, SENSE_VIOLET.argb(
                            Math.round(82.0F * pressure * fade)), 72, 7);
            drawScreenRing(graphics, centerX + offset, centerY,
                    radius + 5, SENSE_GOLD.argb(
                            Math.round(55.0F * pressure * fade)), 72, 11);
        }

        if (focusFade > 0.02F) {
            int radius = Math.round(Math.min(width, height)
                    * (0.095F + 0.015F * Mth.sin(time * 0.08F)));
            drawScreenRing(graphics, centerX, centerY, radius,
                    SENSE_GOLD.argb(Mth.clamp(
                            Math.round(72.0F * focusFade * fade), 0, 90)),
                    64, 13);
        }
    }

    /**
     * Abstract screen-space soul echoes remain perceptible behind terrain. They
     * are deliberately not entity outlines: tier two uses a server-jittered
     * position and several dim echoes, while tier three becomes steadier.
     */
    private static void renderProjectedSignatures(
            GuiGraphics graphics, Minecraft minecraft,
            int width, int height) {
        List<Signature> projected = signatures.stream()
                .filter(signature -> signature.kind()
                        != SpiritualSenseSystem.KIND_FORMATION_NODE)
                // Client-tracked living entities use their real animated Qi
                // body at every detection tier. Keep a projected echo only
                // when the server can sense an entity the client has not
                // received yet.
                .filter(signature -> !isLivingKind(signature.kind())
                        || minecraft.level == null
                        || minecraft.level.getEntity(signature.id()) == null)
                .sorted(Comparator.comparingDouble(Signature::distance))
                .limit(16)
                .toList();
        for (Signature signature : projected) {
            ProjectedPoint point = projectToScreen(signature, width, height);
            if (point == null) continue;
            int x = point.x();
            int y = point.y();
            Color color = colorFor(signature);
            SignatureVisual visual = VISUALS.get(signature.id());
            if (visual == null) continue;
            boolean concealed = (signature.flags()
                    & SpiritualSenseSystem.FLAG_CONCEALED) != 0;
            float reveal = visual.reveal(clientTicks, concealed);
            float pulse = 0.78F + 0.22F * Mth.sin(
                    (minecraft.level.getGameTime() + signature.id()) * 0.22F);
            int baseAlpha = signature.tier() >= SpiritualSenseSystem.TIER_INSIGHT
                    ? 148 : signature.tier()
                            >= SpiritualSenseSystem.TIER_IDENTIFICATION ? 92 : 46;
            if ((signature.flags()
                    & SpiritualSenseSystem.FLAG_DENSE_INTERFERENCE) != 0) {
                baseAlpha = Math.round(baseAlpha * 0.50F);
            } else if ((signature.flags()
                    & SpiritualSenseSystem.FLAG_INTERFERENCE) != 0) {
                baseAlpha = Math.round(baseAlpha * 0.76F);
            }
            int alpha = Mth.clamp(Math.round(
                    baseAlpha * fade * pulse * reveal), 0, 210);
            if (signature.tier() == SpiritualSenseSystem.TIER_PRESENCE) {
                for (int cloud = 0; cloud < 3; cloud++) {
                    drawScreenRing(graphics,
                            x + Math.round(Mth.sin(clientTicks * 0.08F
                                    + signature.id() + cloud) * (cloud + 1)),
                            y + Math.round(Mth.cos(clientTicks * 0.07F
                                    + signature.id() - cloud) * (cloud + 1)),
                            5 + cloud * 4,
                            color.argb(Math.max(3, alpha / (cloud + 2))),
                            24, signature.id() + cloud * 5);
                }
                continue;
            }

            if (signature.kind() == SpiritualSenseSystem.KIND_TREASURE) {
                drawTreasureEcho(graphics, x, y, color, alpha,
                        signature, minecraft.level.getGameTime());
            } else if (signature.kind() == SpiritualSenseSystem.KIND_FORMATION
                    || signature.kind() == SpiritualSenseSystem.KIND_VEIN) {
                drawScreenRing(graphics, x, y, 7, color.argb(alpha),
                        32, signature.id());
                drawScreenRing(graphics, x, y, 13,
                        color.argb(Math.max(4, alpha / 3)),
                        40, signature.id() + 7);
                graphics.drawCenteredString(minecraft.font,
                        marker(signature.kind()), x, y - 4,
                        color.argb(alpha));
            } else {
                drawLivingEcho(graphics, x, y, color, alpha,
                        signature, visual, reveal);
            }

            if ((signature.flags()
                    & SpiritualSenseSystem.FLAG_PRESSURE) != 0) {
                for (int halo = 0; halo < 3; halo++) {
                    drawScreenRing(graphics, x, y,
                            12 + halo * 7
                                    + Math.round(Mth.sin(clientTicks * 0.09F
                                            + halo) * 2.0F),
                            color.mix(SENSE_VIOLET, 0.45F)
                                    .argb(Math.max(3, alpha / (5 + halo))),
                            42, signature.id() + halo * 9);
                }
            }
        }
    }

    private static ProjectedPoint projectToScreen(
            Signature signature, int width, int height) {
        if (!projectionReady) return null;
        Vector4f clip = new Vector4f(
                (float) (signature.x() - projectionCameraPosition.x),
                (float) (signature.y() - projectionCameraPosition.y),
                (float) (signature.z() - projectionCameraPosition.z),
                1.0F).mul(WORLD_TO_CLIP);
        float w = clip.w();
        if (!Float.isFinite(w) || w <= 1.0E-4F) return null;
        float ndcX = clip.x() / w;
        float ndcY = clip.y() / w;
        float ndcZ = clip.z() / w;
        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)
                || !Float.isFinite(ndcZ)
                || ndcX < -1.0F || ndcX > 1.0F
                || ndcY < -1.0F || ndcY > 1.0F
                || ndcZ < -1.0F || ndcZ > 1.0F) {
            return null;
        }
        int x = Math.round((ndcX * 0.5F + 0.5F) * width);
        int y = Math.round((0.5F - ndcY * 0.5F) * height);
        return new ProjectedPoint(x, y);
    }

    private static void drawLivingEcho(
            GuiGraphics graphics, int x, int y, Color color, int alpha,
            Signature signature, SignatureVisual visual, float reveal) {
        boolean hostile = signature.kind() == SpiritualSenseSystem.KIND_HOSTILE;
        boolean cultivator = signature.kind()
                == SpiritualSenseSystem.KIND_CULTIVATOR;
        boolean dense = (signature.flags()
                & SpiritualSenseSystem.FLAG_DENSE_INTERFERENCE) != 0;
        int scale = signature.intensity() > 1.65F ? 2 : 1;
        int ghosts = (signature.flags() & SpiritualSenseSystem.FLAG_MOVING) != 0
                ? 3 : 2;
        for (int ghost = ghosts - 1; ghost >= 0; ghost--) {
            int jitter = ghost == 0 ? 0 : (hostile ? ghost * 2 : ghost);
            int gx = x + (ghost % 2 == 0 ? jitter : -jitter);
            int gy = y + ghost;
            int ghostAlpha = ghost == 0 ? alpha
                    : Math.max(2, alpha / (4 + ghost));
            if (reveal < 0.34F) {
                drawScreenRing(graphics, gx, gy, 5 + ghost * 3,
                        color.argb(ghostAlpha), 28,
                        signature.id() + ghost);
                continue;
            }
            int head = 2 * scale;
            graphics.fill(gx - head, gy - 9 * scale,
                    gx + head + 1, gy - 5 * scale,
                    color.mix(Color.WHITE, 0.32F).argb(ghostAlpha));
            graphics.fill(gx - 3 * scale, gy - 5 * scale,
                    gx + 3 * scale + 1, gy + 3 * scale,
                    color.argb(Math.max(2, Math.round(ghostAlpha * 0.78F))));
            if (!dense || Math.floorMod(
                    signature.id() + ghost + clientTicks / 3, 3) != 0) {
                graphics.fill(gx - 5 * scale, gy - 4 * scale,
                        gx - 3 * scale, gy + 2 * scale,
                        color.argb(Math.max(2, ghostAlpha / 2)));
                graphics.fill(gx + 4 * scale, gy - 4 * scale,
                        gx + 6 * scale, gy + 2 * scale,
                        color.argb(Math.max(2, ghostAlpha / 2)));
                graphics.fill(gx - 3 * scale, gy + 3 * scale,
                        gx - 1 * scale, gy + 8 * scale,
                        color.argb(Math.max(2, ghostAlpha / 2)));
                graphics.fill(gx + 1 * scale, gy + 3 * scale,
                        gx + 3 * scale, gy + 8 * scale,
                        color.argb(Math.max(2, ghostAlpha / 2)));
            }
        }
        drawScreenRing(graphics, x, y - scale,
                8 * scale + Math.round(Mth.sin(
                        clientTicks * (hostile ? 0.22F : 0.11F)
                                + signature.id()) * 2.0F),
                color.argb(Math.max(3, alpha / 4)), 38, signature.id());
        if (cultivator) {
            int coreColor = SENSE_GOLD.mix(color, 0.42F)
                    .argb(Math.max(7, Math.min(220, alpha + 42)));
            graphics.fill(x - scale, y - scale,
                    x + scale + 1, y + scale + 1, coreColor);
            graphics.fill(x, y - 6 * scale, x + 1, y + 4 * scale,
                    color.mix(Color.WHITE, 0.55F)
                            .argb(Math.max(4, alpha / 2)));
        }
    }

    private static void drawTreasureEcho(
            GuiGraphics graphics, int x, int y, Color color, int alpha,
            Signature signature, long time) {
        int radius = signature.intensity() > 1.2F ? 4 : 3;
        for (int row = -radius; row <= radius; row++) {
            int halfWidth = radius - Math.abs(row);
            graphics.fill(x - halfWidth, y + row,
                    x + halfWidth + 1, y + row + 1,
                    color.mix(Color.WHITE, 0.42F).argb(alpha));
        }
        drawScreenRing(graphics, x, y,
                8 + Math.round(Mth.sin(time * 0.10F + signature.id()) * 1.5F),
                color.argb(Math.max(4, alpha / 2)), 36, signature.id());
        drawScreenRing(graphics, x, y, 13,
                SENSE_GOLD.argb(Math.max(3, alpha / 5)),
                44, signature.id() + 17);
    }

    private static void drawScreenRing(
            GuiGraphics graphics, int centerX, int centerY,
            int radius, int color, int segments, int pattern) {
        if (radius <= 0 || (color >>> 24) == 0) return;
        int safeSegments = Math.max(12, segments);
        for (int segment = 0; segment < safeSegments; segment++) {
            if (pattern != 0
                    && Math.floorMod(segment * 7 + pattern * 3, 23) < 3) {
                continue;
            }
            float angle = Mth.TWO_PI * segment / safeSegments;
            int x = centerX + Math.round(Mth.cos(angle) * radius);
            int y = centerY + Math.round(Mth.sin(angle) * radius);
            graphics.fill(x, y, x + (segment % 3 == 0 ? 2 : 1), y + 1,
                    color);
        }
    }

    private static float smoothScreenStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static void renderCompass(
            GuiGraphics graphics, Minecraft minecraft, int width) {
        int centerX = width / 2;
        int y = 39;
        int halfWidth = Math.min(190, width / 3);
        var position = HudLayout.place(graphics, HudElement.SENSE_COMPASS, centerX - halfWidth - 5, y - 13, halfWidth * 2 + 10, 20);
        centerX = position.x() + halfWidth + 5; y = position.y() + 13;
        graphics.fill(centerX - halfWidth, y,
                centerX + halfWidth, y + 1, 0x446EEBFF);
        // Minecraft yaw: south 0, west 90, north 180, east -90.
        renderCompassCardinal(graphics, minecraft, centerX, y, halfWidth,
                180.0F, "n");
        renderCompassCardinal(graphics, minecraft, centerX, y, halfWidth,
                -90.0F, "e");
        renderCompassCardinal(graphics, minecraft, centerX, y, halfWidth,
                0.0F, "s");
        renderCompassCardinal(graphics, minecraft, centerX, y, halfWidth,
                90.0F, "w");
        List<Signature> markers = signatures.stream()
                .filter(signature -> signature.kind()
                        != SpiritualSenseSystem.KIND_FORMATION_NODE)
                .sorted(Comparator.comparingDouble(Signature::distance))
                .limit(14)
                .toList();
        for (Signature signature : markers) {
            double targetYaw = Math.toDegrees(Math.atan2(
                    -(signature.x() - minecraft.player.getX()),
                    signature.z() - minecraft.player.getZ()));
            float relative = Mth.wrapDegrees(
                    (float) targetYaw - minecraft.player.getYRot());
            float clamped = Mth.clamp(relative / 120.0F, -1.0F, 1.0F);
            int x = centerX + Math.round(clamped * halfWidth);
            Color color = colorFor(signature);
            int alpha = Mth.clamp(Math.round(235.0F * fade), 0, 235);
            graphics.drawCenteredString(minecraft.font,
                    marker(signature.kind()), x, y - 4,
                    color.argb(alpha));
        }
    }

    private static void renderCompassCardinal(
            GuiGraphics graphics, Minecraft minecraft,
            int centerX, int barY, int halfWidth,
            float worldYaw, String suffix) {
        float relative = Mth.wrapDegrees(
                worldYaw - minecraft.player.getYRot());
        if (Math.abs(relative) > 120.0F) return;
        int x = centerX + Math.round(relative / 120.0F * halfWidth);
        float edgeFade = Mth.clamp(
                (120.0F - Math.abs(relative)) / 16.0F,
                0.0F, 1.0F);
        int alpha = Mth.clamp(Math.round(
                230.0F * fade * edgeFade), 0, 230);
        if (alpha <= 0) return;
        Component label = Component.translatable(
                "hud." + ImmortalCultivationMod.MODID
                        + ".spiritual_sense.direction." + suffix);
        graphics.fill(x, barY - 2, x + 1, barY + 3,
                (alpha << 24) | 0x8FEAF2);
        graphics.drawCenteredString(minecraft.font, label,
                x, barY - 13, (alpha << 24) | 0xD8F7F5);
    }

    private static void renderNearestFeedback(
            GuiGraphics graphics, Minecraft minecraft,
            int width, int height) {
        List<Signature> nearest = signatures.stream()
                .filter(signature -> signature.kind()
                        != SpiritualSenseSystem.KIND_FORMATION_NODE)
                .sorted(Comparator.<Signature>comparingInt(signature ->
                        signature.id() == focusedSignatureId ? 0 : 1)
                        .thenComparingDouble(Signature::distance))
                .limit(directionalFocus
                        ? FOCUSED_FEEDBACK_LIMIT : FEEDBACK_LIMIT)
                .toList();
        if (nearest.isEmpty()) return;
        int x = 12;
        int y = height - 20 - nearest.size() * 16;
        int panelWidth = 1;
        int panelHeight = nearest.size() * 16;
        for (Signature signature : nearest) {
            panelWidth = Math.max(panelWidth, minecraft.font.width(primaryLine(minecraft, signature)));
            if (signature.id() == focusedSignatureId && !detailLine(signature).isEmpty()) {
                panelWidth = Math.max(panelWidth, minecraft.font.width(detailLine(signature)) + 8);
                panelHeight += 11;
            }
        }
        var position = HudLayout.place(graphics, HudElement.SENSE_DETAILS, x, y, panelWidth, panelHeight);
        x = position.x(); y = position.y();
        for (Signature signature : nearest) {
            Color color = colorFor(signature);
            Component primary = primaryLine(minecraft, signature);
            graphics.drawString(minecraft.font, primary, x, y,
                    color.argb(Mth.clamp(Math.round(
                            (signature.id() == focusedSignatureId ? 220.0F : 145.0F)
                                    * fade), 0, 230)),
                    true);
            if (signature.id() == focusedSignatureId) {
                String details = detailLine(signature);
                if (!details.isEmpty()) {
                    graphics.drawString(minecraft.font, details,
                            x + 8, y + 10, 0xB8E3EDF2, true);
                    y += 11;
                }
            }
            y += 16;
        }
    }

    private static Component primaryLine(
            Minecraft minecraft, Signature signature) {
        String direction = directionTo(minecraft, signature);
        String distance = "~" + Math.round(signature.distance()) + "m";
        if (signature.tier() == SpiritualSenseSystem.TIER_PRESENCE) {
            if ((signature.flags()
                    & SpiritualSenseSystem.FLAG_QI_FLOW) != 0) {
                return Component.translatable(
                        "hud." + ImmortalCultivationMod.MODID
                                + ".spiritual_sense.qi_flow",
                        direction, distance);
            }
            return Component.translatable(
                    "hud." + ImmortalCultivationMod.MODID
                            + ".spiritual_sense.presence",
                    direction, distance);
        }
        Component kind = kindName(signature);
        if (signature.tier() == SpiritualSenseSystem.TIER_IDENTIFICATION) {
            Component realm = realmName(signature.realm());
            return signature.realm().isBlank()
                    ? Component.literal("").append(kind)
                            .append("  " + direction + "  " + distance)
                    : Component.literal("").append(kind).append(" · ")
                            .append(realm).append("  " + distance);
        }
        Component name = signature.name().isBlank()
                ? kind : exactName(signature);
        return Component.literal("").append(name)
                .append(signature.realm().isBlank() ? "" : " · ")
                .append(signature.realm().isBlank()
                        ? Component.empty() : realmName(signature.realm()))
                .append("  " + distance);
    }

    private static String detailLine(Signature signature) {
        if (signature.tier() < SpiritualSenseSystem.TIER_INSIGHT) return "";
        List<String> details = new ArrayList<>();
        if (signature.healthPercent() >= 0) {
            details.add("HP ~" + signature.healthPercent() + "%");
        }
        if (signature.qiPercent() >= 0) {
            details.add("Qi ~" + signature.qiPercent() + "%");
        }
        if (!signature.element().isBlank()) {
            details.add(elementName(signature.element()));
        }
        if (!signature.method().isBlank()) {
            CultivationMethods.MethodDef method =
                    CultivationMethods.get(signature.method());
            details.add(method == null ? signature.method()
                    : Component.translatable(method.nameKey()).getString());
        }
        if (!signature.artifact().isBlank()) details.add(signature.artifact());
        return String.join(" · ", details);
    }

    private static Component exactName(Signature signature) {
        if (signature.kind() == SpiritualSenseSystem.KIND_FORMATION) {
            return Component.translatable("center_type."
                    + ImmortalCultivationMod.MODID + "." + signature.name());
        }
        if (signature.kind() == SpiritualSenseSystem.KIND_VEIN) {
            return Component.translatable("block."
                    + ImmortalCultivationMod.MODID + ".spirit_vein_center");
        }
        if (signature.name().startsWith(
                SpiritualSenseSystem.TRANSLATED_NAME_PREFIX)) {
            return Component.translatable(signature.name().substring(
                    SpiritualSenseSystem.TRANSLATED_NAME_PREFIX.length()));
        }
        return Component.literal(signature.name());
    }

    private static Component kindName(Signature signature) {
        String suffix = switch (signature.kind()) {
            case SpiritualSenseSystem.KIND_HOSTILE -> "hostile";
            case SpiritualSenseSystem.KIND_CULTIVATOR -> "cultivator";
            case SpiritualSenseSystem.KIND_TREASURE -> "treasure";
            case SpiritualSenseSystem.KIND_VEIN -> "vein";
            case SpiritualSenseSystem.KIND_FORMATION,
                 SpiritualSenseSystem.KIND_FORMATION_NODE -> "formation";
            case SpiritualSenseSystem.KIND_SOUL -> "soul";
            default -> "living";
        };
        return Component.translatable("hud." + ImmortalCultivationMod.MODID
                + ".spiritual_sense.kind." + suffix);
    }

    private static boolean isLivingKind(int kind) {
        return kind == SpiritualSenseSystem.KIND_LIVING
                || kind == SpiritualSenseSystem.KIND_HOSTILE
                || kind == SpiritualSenseSystem.KIND_CULTIVATOR
                || kind == SpiritualSenseSystem.KIND_SOUL;
    }

    private static Component realmName(String realm) {
        // Identification deliberately reports only the broad realm (for
        // example "练气") rather than a full stage ("练气前期").
        // A broad realm is already canonical and must not be reparsed as a
        // level, because getLevelDef quite correctly rejects stage-less input.
        CultivationLevels.RealmDef broadRealm = CultivationLevels.getRealm(realm);
        String base = broadRealm == null
                ? CultivationLevels.getLevelDef(realm).realm()
                : broadRealm.realm();
        String suffix = switch (base) {
            case CultivationLevels.REALM_LIANQI -> "lianqi";
            case CultivationLevels.REALM_ZHUJI -> "zhuji";
            case CultivationLevels.REALM_JINDAN -> "jindan";
            case CultivationLevels.REALM_YUANYING -> "yuanying";
            case CultivationLevels.REALM_HUASHEN -> "huashen";
            case CultivationLevels.REALM_LIANXU -> "lianxu";
            case CultivationLevels.REALM_HETI -> "heti";
            case CultivationLevels.REALM_DACHENG -> "dacheng";
            case CultivationLevels.REALM_DUJIE -> "dujie";
            default -> "mortal";
        };
        Component translated = Component.translatable("hud."
                + ImmortalCultivationMod.MODID
                + ".spiritual_sense.realm." + suffix);
        if (realm.equals(base)) return translated;
        String stage = realm.startsWith(base)
                ? realm.substring(base.length()) : "";
        return Component.literal("").append(translated)
                .append(stageName(stage));
    }

    private static Component stageName(String stage) {
        String suffix = switch (stage) {
            case CultivationLevels.STAGE_EARLY -> "early";
            case CultivationLevels.STAGE_MID -> "mid";
            case CultivationLevels.STAGE_LATE -> "late";
            default -> "";
        };
        return suffix.isBlank() ? Component.empty()
                : Component.translatable("hud." + ImmortalCultivationMod.MODID
                        + ".spiritual_sense.stage." + suffix);
    }

    private static String elementName(String roots) {
        List<String> translated = new ArrayList<>();
        for (String root : roots.split("[/,]")) {
            String suffix = switch (com.example.immortal_cultivation_mod.attachment.SpiritRoots.normalizeElement(root)) {
                case "金" -> "metal";
                case "木" -> "wood";
                case "水" -> "water";
                case "火" -> "fire";
                case "土" -> "earth";
                case "雷" -> "thunder";
                case "冰" -> "ice";
                case "风" -> "wind";
                case "阳" -> "yang";
                case "阴" -> "yin";
                default -> "";
            };
            if (!suffix.isBlank()) {
                translated.add(Component.translatable("screen."
                        + ImmortalCultivationMod.MODID
                        + ".trajectory.element." + suffix).getString());
            }
        }
        return String.join("/", translated);
    }

    private static String directionTo(
            Minecraft minecraft, Signature signature) {
        double angle = Math.toDegrees(Math.atan2(
                signature.z() - minecraft.player.getZ(),
                signature.x() - minecraft.player.getX()));
        int octant = Math.floorMod((int) Math.round(angle / 45.0D), 8);
        String suffix = switch (octant) {
            case 0 -> "e";
            case 1 -> "se";
            case 2 -> "s";
            case 3 -> "sw";
            case 4 -> "w";
            case 5 -> "nw";
            case 6 -> "n";
            default -> "ne";
        };
        return Component.translatable("hud." + ImmortalCultivationMod.MODID
                + ".spiritual_sense.direction." + suffix).getString();
    }

    private static String marker(int kind) {
        return switch (kind) {
            case SpiritualSenseSystem.KIND_HOSTILE -> "◆";
            case SpiritualSenseSystem.KIND_CULTIVATOR -> "人";
            case SpiritualSenseSystem.KIND_TREASURE -> "◇";
            case SpiritualSenseSystem.KIND_VEIN -> "≈";
            case SpiritualSenseSystem.KIND_FORMATION -> "阵";
            case SpiritualSenseSystem.KIND_SOUL -> "魂";
            default -> "·";
        };
    }

    private static Color colorFor(Signature signature) {
        if (!signature.element().isBlank()) {
            String first = com.example.immortal_cultivation_mod.attachment.SpiritRoots.normalizeElement(
                    signature.element().split("[/,]")[0]);
            return switch (first) {
                case "金" -> new Color(1.0F, 0.78F, 0.20F);
                case "木" -> new Color(0.28F, 0.92F, 0.38F);
                case "水" -> new Color(0.20F, 0.62F, 1.0F);
                case "火" -> new Color(1.0F, 0.22F, 0.08F);
                case "土" -> new Color(0.76F, 0.54F, 0.25F);
                case "雷" -> new Color(0.46F, 0.65F, 1.0F);
                case "冰" -> new Color(0.58F, 0.96F, 1.0F);
                case "风" -> new Color(0.50F, 1.0F, 0.72F);
                case "阳" -> new Color(1.0F, 0.96F, 0.66F);
                case "阴" -> new Color(0.52F, 0.25F, 0.72F);
                default -> SENSE_CYAN;
            };
        }
        return switch (signature.kind()) {
            case SpiritualSenseSystem.KIND_HOSTILE ->
                    new Color(0.92F, 0.12F, 0.18F);
            case SpiritualSenseSystem.KIND_CULTIVATOR -> SENSE_CYAN;
            case SpiritualSenseSystem.KIND_TREASURE ->
                    new Color(1.0F, 0.79F, 0.26F);
            case SpiritualSenseSystem.KIND_VEIN ->
                    new Color(0.18F, 1.0F, 0.93F);
            case SpiritualSenseSystem.KIND_FORMATION,
                 SpiritualSenseSystem.KIND_FORMATION_NODE -> SENSE_VIOLET;
            case SpiritualSenseSystem.KIND_SOUL ->
                    new Color(0.68F, 0.52F, 1.0F);
            default -> new Color(0.88F, 0.94F, 1.0F);
        };
    }

    private static void renderBillboardRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float width, int segments,
            Color color, float alpha) {
        float inner = Math.max(0.0F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        for (int segment = 0; segment < segments; segment++) {
            float angle0 = Mth.TWO_PI * segment / segments;
            float angle1 = Mth.TWO_PI * (segment + 1) / segments;
            billboardRingVertex(vertices, matrix, inner, angle0,
                    0.0F, 0.0F, color, alpha);
            billboardRingVertex(vertices, matrix, outer, angle0,
                    0.0F, 1.0F, color, alpha);
            billboardRingVertex(vertices, matrix, outer, angle1,
                    1.0F, 1.0F, color, alpha);
            billboardRingVertex(vertices, matrix, inner, angle1,
                    1.0F, 0.0F, color, alpha);
        }
    }

    private static void renderBrokenBillboardRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float width, float angleOffset,
            Color color, float alpha, int pattern) {
        int segments = 48;
        for (int segment = 0; segment < segments; segment++) {
            if (Math.floorMod(segment * 7 + pattern * 3, 17) < 4) continue;
            float angle0 = (angleOffset
                    + 360.0F * segment / segments) * Mth.DEG_TO_RAD;
            float angle1 = (angleOffset
                    + 360.0F * (segment + 0.74F) / segments)
                    * Mth.DEG_TO_RAD;
            float inner = Math.max(0.0F, radius - width * 0.5F);
            float outer = radius + width * 0.5F;
            billboardRingVertex(vertices, matrix, inner, angle0,
                    0.0F, 0.0F, color, alpha);
            billboardRingVertex(vertices, matrix, outer, angle0,
                    0.0F, 1.0F, color, alpha);
            billboardRingVertex(vertices, matrix, outer, angle1,
                    1.0F, 1.0F, color, alpha);
            billboardRingVertex(vertices, matrix, inner, angle1,
                    1.0F, 0.0F, color, alpha);
        }
    }

    private static void billboardRingVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float angle, float u, float v,
            Color color, float alpha) {
        billboardVertex(vertices, matrix,
                Mth.cos(angle) * radius,
                Mth.sin(angle) * radius,
                u, v, color, alpha);
    }

    private static void renderBrokenRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float width, float angleOffset,
            Color color, float alpha, int pattern) {
        int segments = 48;
        for (int segment = 0; segment < segments; segment++) {
            if (Math.floorMod(segment * 7 + pattern * 3, 17) < 4) continue;
            float start = angleOffset + 360.0F * segment / segments;
            float end = angleOffset + 360.0F * (segment + 0.76F) / segments;
            renderArc(vertices, matrix, radius, 0.0F, width,
                    start, end, color, alpha);
        }
    }

    private static void renderArc(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float y, float width,
            float startDegrees, float endDegrees,
            Color color, float alpha) {
        float inner = Math.max(0.0F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        float angle0 = startDegrees * Mth.DEG_TO_RAD;
        float angle1 = endDegrees * Mth.DEG_TO_RAD;
        ringVertex(vertices, matrix, inner, angle0, y,
                0.0F, 0.0F, color, alpha);
        ringVertex(vertices, matrix, outer, angle0, y,
                0.0F, 1.0F, color, alpha);
        ringVertex(vertices, matrix, outer, angle1, y,
                1.0F, 1.0F, color, alpha);
        ringVertex(vertices, matrix, inner, angle1, y,
                1.0F, 0.0F, color, alpha);
    }

    private static void renderRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float y, float width, int segments,
            Color color, float alpha) {
        renderRing(vertices, matrix, radius, y, width,
                segments, color, alpha, 0.0F, 0.0F);
    }

    private static void renderRing(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float y, float width, int segments,
            Color color, float alpha, float offsetX, float offsetZ) {
        float inner = Math.max(0.0F, radius - width * 0.5F);
        float outer = radius + width * 0.5F;
        for (int segment = 0; segment < segments; segment++) {
            float angle0 = Mth.TWO_PI * segment / segments;
            float angle1 = Mth.TWO_PI * (segment + 1) / segments;
            ringVertex(vertices, matrix, inner, angle0, y,
                    0.0F, 0.0F, color, alpha, offsetX, offsetZ);
            ringVertex(vertices, matrix, outer, angle0, y,
                    0.0F, 1.0F, color, alpha, offsetX, offsetZ);
            ringVertex(vertices, matrix, outer, angle1, y,
                    1.0F, 1.0F, color, alpha, offsetX, offsetZ);
            ringVertex(vertices, matrix, inner, angle1, y,
                    1.0F, 0.0F, color, alpha, offsetX, offsetZ);
        }
    }

    private static void ringVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float angle, float y, float u, float v,
            Color color, float alpha) {
        ringVertex(vertices, matrix, radius, angle, y, u, v,
                color, alpha, 0.0F, 0.0F);
    }

    private static void ringVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float radius, float angle, float y, float u, float v,
            Color color, float alpha, float offsetX, float offsetZ) {
        flatVertex(vertices, matrix,
                offsetX + Mth.cos(angle) * radius,
                y,
                offsetZ + Mth.sin(angle) * radius,
                u, v, color, alpha);
    }

    private static void quad(
            VertexConsumer vertices, Matrix4f matrix,
            float minX, float minY, float maxX, float maxY,
            Color color, float alpha) {
        billboardVertex(vertices, matrix, minX, minY,
                0.0F, 0.0F, color, alpha);
        billboardVertex(vertices, matrix, maxX, minY,
                1.0F, 0.0F, color, alpha);
        billboardVertex(vertices, matrix, maxX, maxY,
                1.0F, 1.0F, color, alpha);
        billboardVertex(vertices, matrix, minX, maxY,
                0.0F, 1.0F, color, alpha);
    }

    private static void billboardVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float u, float v,
            Color color, float alpha) {
        VfxVertexWriter.vertex(vertices, matrix, x, y, 0.0F)
                .setColor(color.red, color.green, color.blue, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 0, 1);
    }

    private static void flatVertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float z, float u, float v,
            Color color, float alpha) {
        VfxVertexWriter.vertex(vertices, matrix, x, y, z)
                .setColor(color.red, color.green, color.blue, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }

    private static void trailVertex(
            VertexConsumer vertices, Matrix4f matrix,
            Vec3 position, float u, float v,
            Color color, float alpha) {
        trailVertex(vertices, matrix, position.x, position.y, position.z, u, v, color, alpha);
    }

    private static void trailVertex(VertexConsumer vertices, Matrix4f matrix,
            double x, double y, double z, float u, float v, Color color, float alpha) {
        if (vertices.getClass() == com.mojang.blaze3d.vertex.BufferBuilder.class) {
            int packedColor = ((int) (alpha * 255F) & 255) << 24
                    | ((int) (color.red * 255F) & 255) << 16
                    | ((int) (color.green * 255F) & 255) << 8
                    | ((int) (color.blue * 255F) & 255);
            VfxVertexWriter.entity(vertices, matrix, (float) x, (float) y, (float) z,
                    packedColor, u, v, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0, 1, 0);
            return;
        }
        VfxVertexWriter.vertex(vertices, matrix, (float) x, (float) y, (float) z)
                .setColor(color.red, color.green, color.blue, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }

    private static final class SignatureVisual {
        private Signature signature;
        private Vec3 lastPosition;
        private Vec3 motionVector = Vec3.ZERO;
        private int firstSeenTick;
        private int lastSeenTick;
        private int tierChangedTick;
        private int responseTicks = 24;
        private int residueTicks;
        private int spatialTraceTicks;
        private int missingTicks;

        private SignatureVisual(Signature signature, int now) {
            this.signature = signature;
            this.lastPosition = positionOf(signature);
            this.firstSeenTick = now;
            this.lastSeenTick = now;
            this.tierChangedTick = now;
            if ((signature.flags() & SpiritualSenseSystem.FLAG_CASTING) != 0) {
                residueTicks = 32;
            }
        }

        private void accept(Signature next, int now) {
            Vec3 nextPosition = positionOf(next);
            Vec3 displacement = nextPosition.subtract(lastPosition);
            if (next.tier() >= SpiritualSenseSystem.TIER_INSIGHT
                    && displacement.lengthSqr() < 144.0D) {
                motionVector = motionVector.scale(0.58D)
                        .add(displacement.scale(0.42D));
            } else {
                motionVector = motionVector.scale(0.45D);
            }
            if (next.tier() >= SpiritualSenseSystem.TIER_INSIGHT
                    && displacement.lengthSqr() > 20.0D) {
                spatialTraceTicks = 30;
            }
            if (next.tier() > signature.tier()) {
                tierChangedTick = now;
                responseTicks = 24;
            }
            if ((next.flags() & SpiritualSenseSystem.FLAG_CASTING) != 0) {
                residueTicks = 32;
            }
            signature = next;
            lastPosition = nextPosition;
            lastSeenTick = now;
            missingTicks = 0;
        }

        private void tick() {
            if (responseTicks > 0) responseTicks--;
            if (residueTicks > 0) residueTicks--;
            if (spatialTraceTicks > 0) spatialTraceTicks--;
            motionVector = motionVector.scale(0.94D);
        }

        private boolean hasFlag(int flag) {
            return (signature.flags() & flag) != 0;
        }

        private float reveal(int now, boolean concealed) {
            int baseAge = Math.max(0, now - firstSeenTick);
            int tierAge = Math.max(0, now - tierChangedTick);
            float base = Mth.clamp(baseAge / (concealed ? 34.0F : 16.0F),
                    0.0F, 1.0F);
            float tier = Mth.clamp(tierAge / (concealed ? 24.0F : 10.0F),
                    0.0F, 1.0F);
            return smoothScreenStep(Math.min(base, 0.42F + tier * 0.58F));
        }

        private static Vec3 positionOf(Signature signature) {
            return new Vec3(signature.x(), signature.y(), signature.z());
        }
    }

    private record ProjectedPoint(int x, int y) {
    }

    private record Color(float red, float green, float blue) {
        private static final Color WHITE = new Color(1.0F, 1.0F, 1.0F);

        private Color mix(Color other, float amount) {
            float clamped = Mth.clamp(amount, 0.0F, 1.0F);
            return new Color(
                    Mth.lerp(clamped, red, other.red),
                    Mth.lerp(clamped, green, other.green),
                    Mth.lerp(clamped, blue, other.blue));
        }

        private int argb(int alpha) {
            return Mth.clamp(alpha, 0, 255) << 24
                    | Mth.clamp(Math.round(red * 255.0F), 0, 255) << 16
                    | Mth.clamp(Math.round(green * 255.0F), 0, 255) << 8
                    | Mth.clamp(Math.round(blue * 255.0F), 0, 255);
        }
    }
}
