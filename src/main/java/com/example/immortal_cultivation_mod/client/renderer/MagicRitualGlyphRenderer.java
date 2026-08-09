package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.ui.CenterFuelMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

public class MagicRitualGlyphRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final double ORPHAN_RENDER_DISTANCE = 96.0D;
    private static final float ENTRY_EXPANSION_TICKS = 30.0F;
    private static final ResourceLocation CENTER_TEXTURE = texture("magic_center_glyph");
    private static final ResourceLocation ATTACK_TEXTURE = texture("magic_circle_glyph");
    private static final ResourceLocation DEFENSE_TEXTURE = texture("magic_circle_defense_glyph");
    private static final ResourceLocation REGENERATION_TEXTURE = texture("magic_circle_regeneration_glyph");
    private static final ResourceLocation FOG_TEXTURE_1 = texture("magic_center_fog_1");
    private static final ResourceLocation FOG_TEXTURE_2 = texture("magic_center_fog_2");
    private static final ResourceLocation INVERTED_TEXTURE_1 = texture("magic_center_inverted_1");
    private static final ResourceLocation INVERTED_TEXTURE_2 = texture("magic_center_inverted_2");
    private static final ResourceLocation FORCEFIELD_TEXTURE_1 = texture("magic_center_forcefield_1");
    private static final ResourceLocation FORCEFIELD_TEXTURE_2 = texture("magic_center_forcefield_2");
    private static final ResourceLocation SCORCHING_TEXTURE_1 = texture("magic_center_scorching_1");
    private static final ResourceLocation SCORCHING_TEXTURE_2 = texture("magic_center_scorching_2");
    private static final ResourceLocation SANCTUARY_TEXTURE_1 = texture("magic_center_sanctuary_1");
    private static final ResourceLocation SANCTUARY_TEXTURE_2 = texture("magic_center_sanctuary_2");
    private static final ResourceLocation FROST_TEXTURE_1 = texture("magic_center_frost_1");
    private static final ResourceLocation FROST_TEXTURE_2 = texture("magic_center_frost_2");
    private static final ResourceLocation DECAY_TEXTURE_1 = texture("magic_center_decay_1");
    private static final ResourceLocation DECAY_TEXTURE_2 = texture("magic_center_decay_2");
    private static final ResourceLocation THUNDER_TEXTURE_1 = texture("magic_center_thunder_1");
    private static final ResourceLocation THUNDER_TEXTURE_2 = texture("magic_center_thunder_2");
    private static final ResourceLocation DETECTION_TEXTURE_1 = texture("magic_center_detection_1");
    private static final ResourceLocation DETECTION_TEXTURE_2 = texture("magic_center_detection_2");
    private static final ResourceLocation REFORGE_BLADE_TEXTURE_1 = texture("magic_center_reforge_blade_1");
    private static final ResourceLocation REFORGE_BLADE_TEXTURE_2 = texture("magic_center_reforge_blade_2");
    private static final ResourceLocation TELEPORT_TEXTURE_1 = texture("magic_center_teleport_1");
    private static final ResourceLocation TELEPORT_TEXTURE_2 = texture("magic_center_teleport_2");
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/misc/white.png");
    private static final int FORCEFIELD_MIN_LONGITUDE_SEGMENTS = 32;
    private static final int FORCEFIELD_MAX_LONGITUDE_SEGMENTS = 64;
    private static final float FORMATION_FLAG_INNER_RADIUS_FACTOR = 0.34F;
    private static final float FORMATION_FLAG_OUTER_MARGIN = 0.75F;
    private static final float FORMATION_FLAG_MIN_INNER_RADIUS = 0.90F;
    private static final float FORMATION_FLAG_MIN_OUTER_RADIUS = 1.90F;
    private static final float FORMATION_FLAG_SCALE = 0.72F;
    private static final double FORMATION_FLAG_BASE_HEIGHT = 0.22D;
    private static final float FORMATION_FLAG_HOVER_HEIGHT = 0.20F;
    private static final float FORMATION_FLAG_SPIN_SPEED = 2.1F;
    private final ItemRenderer itemRenderer;

    public MagicRitualGlyphRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MagicCenterBlockEntity renderedCenter = blockEntity instanceof MagicCenterBlockEntity centerEntity
                ? centerEntity : null;
        boolean forcefieldCenter = renderedCenter != null
                && MagicCenterTypeRegistry.FORCEFIELD.equals(renderedCenter.getCenterTypeId());
        ItemStack reforgeWeapon = renderedCenter != null
                && MagicCenterTypeRegistry.REFORGE_BLADE.equals(renderedCenter.getCenterTypeId())
                ? renderedCenter.getReforgeWeapon() : ItemStack.EMPTY;
        boolean hasReforgeWeapon = !reforgeWeapon.isEmpty();
        boolean hasFormationFlags = renderedCenter != null
                && renderedCenter.hasFormationFlags();
        boolean hasTeleportChannel = renderedCenter != null
                && renderedCenter.hasTeleportChannel();
        boolean temporaryCenter = renderedCenter != null && renderedCenter.isTemporary();
        boolean glyphEnabled = renderedCenter == null || renderedCenter.isParticlesEnabled();
        boolean authorizedReveal = renderedCenter != null
                && renderedCenter.hasAuthorizedPlayerInside();
        boolean entryReveal = authorizedReveal && !glyphEnabled;
        if (!glyphEnabled && !forcefieldCenter && !authorizedReveal
                && !hasReforgeWeapon && !hasFormationFlags && !hasTeleportChannel
                && !temporaryCenter) {
            return;
        }
        MagicCenterBlockEntity owningCenter = findOwningCenter(blockEntity);
        if (blockEntity instanceof MagicCircleBlockEntity
                && owningCenter != null
                && !owningCenter.isParticlesEnabled()) {
            return;
        }
        boolean center = renderedCenter != null;
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 renderOrigin = owningCenter == null
                ? Vec3.atCenterOf(blockEntity.getBlockPos())
                : owningCenter.centerPoint();
        double renderDistance = getRenderDistance(owningCenter);
        double distanceSquared = renderOrigin.distanceToSqr(cameraPosition);
        if (distanceSquared > renderDistance * renderDistance) {
            return;
        }
        boolean active = owningCenter != null && owningCenter.isActive();
        float alpha = active ? 1.0F : distanceSquared > 48.0D * 48.0D ? 0.45F : 1.0F;
        if (owningCenter != null && !owningCenter.isActive()) {
            alpha *= 0.35F;
        }
        int color = owningCenter == null ? 0xFFFFFF : owningCenter.getGlyphColor();
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float halfSize = center
                ? ((MagicCenterBlockEntity) blockEntity).getRadius()
                : 0.78F;
        float pulse = 1.0F + 0.012F * (float) Math.sin(
                (blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime()) * 0.12D);
        halfSize *= pulse;
        long gameTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime();
        float time = gameTime + partialTick;
        if (center) {
            ResourceLocation[] textures = typedCenterTextures(renderedCenter);
            boolean basicPattern = CENTER_TEXTURE.equals(textures[0]) && CENTER_TEXTURE.equals(textures[1]);
            if (glyphEnabled || authorizedReveal) {
                float revealScale = entryReveal ? entryExpansionScale(renderedCenter, time) : 1.0F;
                float glyphSize = halfSize * revealScale;
                float glyphAlpha = entryReveal ? Math.max(alpha, 0.70F) : alpha;
                boolean blank = MagicCenterTypeRegistry.BLANK.equals(renderedCenter.getCenterTypeId());
                float firstSpin = entryReveal || !blank
                        ? -time * 0.65F : time * 0.45F;
                renderGlyph(poseStack, bufferSource, textures[0], glyphSize, firstSpin,
                        0.080F, red, green, blue, glyphAlpha, active);
                if (!basicPattern) {
                    renderGlyph(poseStack, bufferSource, textures[1], glyphSize, time * 0.52F,
                            0.086F, red, green, blue, glyphAlpha, active);
                }
            }
            if (forcefieldCenter) {
                renderForcefieldSphere(poseStack, bufferSource, halfSize,
                        time, red, green, blue, alpha * (active ? 1.0F : 0.28F));
            }
            if (hasTeleportChannel) {
                renderTeleportChannel(renderedCenter, time, poseStack, bufferSource,
                        red, green, blue, Math.max(0.82F, alpha));
            }
            if (hasReforgeWeapon) {
                renderReforgeWeapon(renderedCenter, reforgeWeapon, time, poseStack,
                        bufferSource, packedLight);
            }
            if (hasFormationFlags) {
                renderFormationFlags(renderedCenter, time, poseStack, bufferSource, packedLight);
            }
            if (temporaryCenter) {
                renderTemporaryCountdown(renderedCenter, partialTick, poseStack, bufferSource);
            }
            return;
        }
        renderGlyph(poseStack, bufferSource, circleTexture(blockEntity), halfSize,
                time * 1.8F, 0.09F, red, green, blue, alpha, active);
    }

    private void renderFormationFlags(MagicCenterBlockEntity center,
                                      float time,
                                      PoseStack poseStack,
                                      MultiBufferSource bufferSource,
                                      int packedLight) {
        float outerRadius = Math.max(FORMATION_FLAG_MIN_OUTER_RADIUS,
                center.getRadius() - FORMATION_FLAG_OUTER_MARGIN);
        float innerRadius = Math.max(FORMATION_FLAG_MIN_INNER_RADIUS,
                outerRadius * FORMATION_FLAG_INNER_RADIUS_FACTOR);
        float ringStep = (outerRadius - innerRadius) / 2.0F;

        for (int slot = 0; slot < CenterFuelMenu.OCTAGON_SLOT_COUNT; slot++) {
            ItemStack flag = center.getFormationFlagForRender(slot);
            if (flag.isEmpty()) {
                continue;
            }

            int layer = slot / CenterFuelMenu.OCTAGON_LAYER_SIZE;
            int point = slot % CenterFuelMenu.OCTAGON_LAYER_SIZE;
            double angleDegrees = -90.0D + point * 45.0D;
            double angleRadians = Math.toRadians(angleDegrees);
            double ringRadius = innerRadius + ringStep * layer;
            double offsetX = Math.cos(angleRadians) * ringRadius;
            double offsetZ = Math.sin(angleRadians) * ringRadius;
            float bob = FORMATION_FLAG_HOVER_HEIGHT
                    * (0.5F + 0.5F * (float)Math.sin(time * 0.12F + slot * 0.65F));
            float spin = (time * FORMATION_FLAG_SPIN_SPEED + slot * 11.25F) % 360.0F;

            poseStack.pushPose();
            poseStack.translate(0.5D + offsetX, FORMATION_FLAG_BASE_HEIGHT + bob,
                    0.5D + offsetZ);
            poseStack.mulPose(Axis.YP.rotationDegrees((float)-angleDegrees + spin));
            poseStack.scale(FORMATION_FLAG_SCALE, FORMATION_FLAG_SCALE,
                    FORMATION_FLAG_SCALE);
            itemRenderer.renderStatic(flag, ItemDisplayContext.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, center.getLevel(),
                    (int)(center.getBlockPos().asLong() ^ (long)slot * 31L));
            poseStack.popPose();
        }
    }

    private void renderReforgeWeapon(MagicCenterBlockEntity center, ItemStack weapon,
                                     float time, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight) {
        float bob = 0.04F * (float)Math.sin(time * 0.10F);
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.25D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.scale(1.35F, 1.35F, 1.35F);
        itemRenderer.renderStatic(weapon, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, center.getLevel(),
                (int)center.getBlockPos().asLong());
        poseStack.popPose();
    }

    private static void renderTemporaryCountdown(MagicCenterBlockEntity center,
                                                 float partialTick,
                                                 PoseStack poseStack,
                                                 MultiBufferSource bufferSource) {
        if (center.getLevel() == null || center.getTemporaryExpiresAt() < 0L) {
            return;
        }
        double remainingTicks = center.getTemporaryExpiresAt()
                - (center.getLevel().getGameTime() + partialTick);
        int remainingSeconds = Math.max(0, (int)Math.ceil(remainingTicks / 20.0D));
        if (remainingSeconds <= 0) {
            return;
        }

        Component text = Component.translatable(
                "label.immortal_cultivation_mod.temporary_center_countdown",
                remainingSeconds);
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.5D, 2.0D, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher()
                .cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        float textX = -font.width(text) / 2.0F;
        font.drawInBatch(text, textX, 0.0F, 0xFFF3D06A, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                0x70000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void renderTeleportChannel(MagicCenterBlockEntity center, float time,
                                              PoseStack poseStack,
                                              MultiBufferSource bufferSource,
                                              float red, float green, float blue,
                                              float alpha) {
        if (center.getLevel() == null || center.getTeleportChannelPlayer() == null) {
            return;
        }
        net.minecraft.world.entity.player.Player player = center.getLevel()
                .getPlayerByUUID(center.getTeleportChannelPlayer());
        if (player == null) {
            return;
        }
        Vec3 playerPos = player.getPosition(time - (float)Math.floor(time));
        Vec3 blockOrigin = Vec3.atLowerCornerOf(center.getBlockPos());
        Vec3 relative = playerPos.subtract(blockOrigin);
        float duration = Math.max(1.0F, center.getTeleportChannelDurationTicks());
        float progress = Math.max(0.0F, Math.min(1.0F,
                (time - center.getTeleportChannelStartTime()) / duration));
        float bodyHeight = player.getBbHeight();
        float travel = bodyHeight * 0.55F;
        float lowerHeight = 0.05F + progress * travel;
        float upperHeight = bodyHeight + 0.45F - progress * travel;
        float glyphRadius = Math.max(1.05F, player.getBbWidth() + 0.65F);

        poseStack.pushPose();
        poseStack.translate(relative.x - 0.5D, relative.y, relative.z - 0.5D);
        renderGlyph(poseStack, bufferSource, TELEPORT_TEXTURE_1, glyphRadius,
                -time * 3.2F, lowerHeight, red, green, blue, alpha, true);
        renderGlyph(poseStack, bufferSource, TELEPORT_TEXTURE_2, glyphRadius,
                time * 3.8F, upperHeight, red, green, blue, alpha, true);
        poseStack.popPose();
    }

    private static float entryExpansionScale(MagicCenterBlockEntity center, float time) {
        long entryTime = center.getAuthorizedEntryGameTime();
        if (entryTime < 0L) {
            return 1.0F;
        }
        float progress = Math.max(0.0F,
                Math.min(1.0F, (time - entryTime) / ENTRY_EXPANSION_TICKS));
        float remaining = 1.0F - progress;
        return 1.0F - remaining * remaining * remaining;
    }

    private static void renderGlyph(PoseStack poseStack, MultiBufferSource bufferSource,
                                    ResourceLocation texture, float halfSize, float spin,
                                    float height, float red, float green, float blue, float alpha,
                                    boolean active) {
        renderLayer(poseStack, bufferSource, texture, halfSize, spin, height,
                red, green, blue, alpha, active);
        if (!active) {
            return;
        }

        // Keep every aura layer on the selected RGB value. Mixing white into the
        // echoes made blue appear purple and shifted other chosen colors.
        float glowRed = red;
        float glowGreen = green;
        float glowBlue = blue;
        for (int layer = 1; layer <= 7; layer++) {
            float progress = layer / 7.0F;
            float glowSize = halfSize * (1.0F + progress * 0.055F);
            float glowHeight = height + layer * 0.026F;
            float glowAlpha = alpha * (0.19F * (1.0F - progress) + 0.025F);
            renderLayer(poseStack, bufferSource, texture, glowSize, spin, glowHeight,
                    glowRed, glowGreen, glowBlue, glowAlpha, false);
        }

        renderLayer(poseStack, bufferSource, texture, halfSize * 1.085F, spin,
                height + 0.008F, glowRed, glowGreen, glowBlue, alpha * 0.10F, false);
    }

    private static void renderLayer(PoseStack poseStack, MultiBufferSource bufferSource,
                                    ResourceLocation texture, float halfSize, float spin,
                                    float height, float red, float green, float blue, float alpha,
                                    boolean solid) {
        poseStack.pushPose();
        poseStack.translate(0.5D, height, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vertices = bufferSource.getBuffer(solid
                ? RenderType.entityCutoutNoCull(texture)
                : RenderType.entityTranslucent(texture));
        int light = LightTexture.FULL_BRIGHT;
        vertex(vertices, pose, -halfSize, 0.0F, -halfSize, 0.0F, 0.0F, light, red, green, blue, alpha);
        vertex(vertices, pose, -halfSize, 0.0F, halfSize, 0.0F, 1.0F, light, red, green, blue, alpha);
        vertex(vertices, pose, halfSize, 0.0F, halfSize, 1.0F, 1.0F, light, red, green, blue, alpha);
        vertex(vertices, pose, halfSize, 0.0F, -halfSize, 1.0F, 0.0F, light, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void renderForcefieldSphere(PoseStack poseStack,
                                               MultiBufferSource bufferSource,
                                               float radius, float time,
                                               float red, float green, float blue,
                                               float alpha) {
        float shimmer = 0.94F + 0.06F * (float)Math.sin(time * 0.09F);
        renderSphereShell(poseStack, bufferSource, radius,
                red, green, blue, alpha * 0.10F * shimmer);
        renderSphereShell(poseStack, bufferSource, radius * 1.025F,
                red, green, blue,
                alpha * 0.035F * shimmer);
    }

    private static void renderSphereShell(PoseStack poseStack,
                                          MultiBufferSource bufferSource,
                                          float radius, float red, float green,
                                          float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.08D, 0.5D);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vertices = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
        int light = LightTexture.FULL_BRIGHT;
        int longitudeSegments = Math.max(FORCEFIELD_MIN_LONGITUDE_SEGMENTS,
                Math.min(FORCEFIELD_MAX_LONGITUDE_SEGMENTS,
                        (int)Math.ceil(radius / 14.0F) * 8));
        int latitudeSegments = longitudeSegments / 2;
        for (int latitudeIndex = 0;
             latitudeIndex < latitudeSegments;
             latitudeIndex++) {
            float latitude0 = (float)(-Math.PI / 2.0D
                    + Math.PI * latitudeIndex / latitudeSegments);
            float latitude1 = (float)(-Math.PI / 2.0D
                    + Math.PI * (latitudeIndex + 1) / latitudeSegments);
            float v0 = (float)latitudeIndex / latitudeSegments;
            float v1 = (float)(latitudeIndex + 1) / latitudeSegments;
            for (int longitudeIndex = 0;
                 longitudeIndex < longitudeSegments;
                 longitudeIndex++) {
                float longitude0 = (float)(Math.PI * 2.0D
                        * longitudeIndex / longitudeSegments);
                float longitude1 = (float)(Math.PI * 2.0D
                        * (longitudeIndex + 1) / longitudeSegments);
                float u0 = (float)longitudeIndex / longitudeSegments;
                float u1 = (float)(longitudeIndex + 1) / longitudeSegments;
                sphereVertex(vertices, pose, radius, latitude0, longitude0,
                        u0, v0, light, red, green, blue, alpha);
                sphereVertex(vertices, pose, radius, latitude1, longitude0,
                        u0, v1, light, red, green, blue, alpha);
                sphereVertex(vertices, pose, radius, latitude1, longitude1,
                        u1, v1, light, red, green, blue, alpha);
                sphereVertex(vertices, pose, radius, latitude0, longitude1,
                        u1, v0, light, red, green, blue, alpha);
            }
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        // Ritual glyphs are world-area visuals. Keep them rendered while the
        // player is in range even when the center itself is outside the camera frustum.
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        // NeoForge applies this renderer-level frustum test even to global block
        // entities. Distance is limited separately by shouldRender(), so infinite
        // bounds only prevent camera direction from hiding a nearby ritual glyph.
        return AABB.INFINITE;
    }

    @Override
    public int getViewDistance() {
        return MagicCenterBlockEntity.MAX_RADIUS + MagicCenterBlockEntity.GLYPH_RENDER_MARGIN;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        MagicCenterBlockEntity owningCenter = findOwningCenter(blockEntity);
        Vec3 renderOrigin = owningCenter == null
                ? Vec3.atCenterOf(blockEntity.getBlockPos())
                : owningCenter.centerPoint();
        double renderDistance = getRenderDistance(owningCenter);
        return renderOrigin.distanceToSqr(cameraPosition) <= renderDistance * renderDistance;
    }

    private static double getRenderDistance(MagicCenterBlockEntity center) {
        return center == null
                ? ORPHAN_RENDER_DISTANCE
                : center.getRadius() + MagicCenterBlockEntity.GLYPH_RENDER_MARGIN;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/particle/" + name + ".png");
    }

    private static ResourceLocation circleTexture(BlockEntity blockEntity) {
        CircleType type = blockEntity.getBlockState().getValue(MagicCircleBlock.CIRCLE_TYPE);
        return switch (type) {
            case DEFENSE -> DEFENSE_TEXTURE;
            case REGENERATION -> REGENERATION_TEXTURE;
            default -> ATTACK_TEXTURE;
        };
    }

    private static ResourceLocation[] typedCenterTextures(MagicCenterBlockEntity center) {
        if (MagicCenterTypeRegistry.FOG.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{FOG_TEXTURE_1, FOG_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.INVERTED.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{INVERTED_TEXTURE_1, INVERTED_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{FORCEFIELD_TEXTURE_1, FORCEFIELD_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.SCORCHING.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{SCORCHING_TEXTURE_1, SCORCHING_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.SANCTUARY.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{SANCTUARY_TEXTURE_1, SANCTUARY_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.FROST.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{FROST_TEXTURE_1, FROST_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.DECAY.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{DECAY_TEXTURE_1, DECAY_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.THUNDER.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{THUNDER_TEXTURE_1, THUNDER_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.DETECTION.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{DETECTION_TEXTURE_1, DETECTION_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.REFORGE_BLADE.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{REFORGE_BLADE_TEXTURE_1, REFORGE_BLADE_TEXTURE_2};
        }
        if (MagicCenterTypeRegistry.TELEPORT.equals(center.getCenterTypeId())) {
            return new ResourceLocation[]{TELEPORT_TEXTURE_1, TELEPORT_TEXTURE_2};
        }
        return new ResourceLocation[]{CENTER_TEXTURE, CENTER_TEXTURE};
    }

    private static MagicCenterBlockEntity findOwningCenter(BlockEntity blockEntity) {
        if (blockEntity instanceof MagicCenterBlockEntity center) {
            return center;
        }
        if (blockEntity instanceof MagicCircleBlockEntity circle && circle.getLevel() != null) {
            BlockPos centerPos = circle.findOwningCenter();
            if (centerPos != null
                    && circle.getLevel().getBlockEntity(centerPos) instanceof MagicCenterBlockEntity center) {
                return center;
            }
        }
        return null;
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose,
                               float x, float y, float z, float u, float v, int light,
                               float red, float green, float blue, float alpha) {
        vertices.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void sphereVertex(VertexConsumer vertices, Matrix4f pose,
                                     float radius, float latitude, float longitude,
                                     float u, float v, int light,
                                     float red, float green, float blue, float alpha) {
        float cosLatitude = (float)Math.cos(latitude);
        float normalX = cosLatitude * (float)Math.cos(longitude);
        float normalY = (float)Math.sin(latitude);
        float normalZ = cosLatitude * (float)Math.sin(longitude);
        vertices.addVertex(pose, normalX * radius, normalY * radius, normalZ * radius)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }
}
