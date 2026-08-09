package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
import com.example.immortal_cultivation_mod.client.hud.JindanTribulationOverlay;
import com.example.immortal_cultivation_mod.client.renderer.EmptyEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CultivatorCorpseRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlindfoldCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.Blindfold2CurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CoffinCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlagCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JadePendantCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.StrawHatCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhenhunBellCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.GeoRockBlockRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCultivatorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhujiCultivatorRenderer;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.client.particle.FireballTrailParticle;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.item.HeavyIronBracerItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.SwordCaseItem;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.screen.StatMenuScreen;
import com.example.immortal_cultivation_mod.screen.CultivationSettingsScreen;
import com.example.immortal_cultivation_mod.screen.SpellComboMenuScreen;
import com.example.immortal_cultivation_mod.screen.SpellSelectionScreen;
import com.example.immortal_cultivation_mod.screen.SpellWheelScreen;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.ShixuChiliu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.List;
import java.util.UUID;

public class ModClientEvents {
    private static boolean wasSpellKeyDown = false;
    private static boolean wasAttackKeyDown = false;
    private static boolean wasUseKeyDown = false;
    private static boolean wasJumpKeyDown = false;
    private static boolean wasPreparedCastKeyDown = false;
    private static boolean preparedStateSent = false;
    private static boolean preparedCastInterrupted = false;
    private static boolean wasReleaseWeaponKeyDown = false;
    private static boolean wasSwordCaseKeyDown = false;
    private static int preparedChargeTicks = 0;
    private static int swordCaseChargeTicks = 0;
    private static boolean swordCaseReleaseTriggered = false;
    private static boolean windStepDoubleJumpUsed = false;
    private static final int PREPARED_CHARGE_TICKS = 40;
    private static final int SWORD_CASE_CHARGE_TICKS = 40;
    private static final int JINDAN_SCROLL_STEPS_PER_PACKET = 8;
    private static final int MAX_JINDAN_SCROLL_STEPS_PER_EVENT = 64;
    private static Boolean lastFrostFlightFirstPerson = null;
    private static UUID autoChannelSyncedPlayer = null;
    private static Boolean autoChannelSyncedValue = null;
    private static UUID outwardQiSyncedPlayer = null;
    private static Boolean outwardQiSyncedValue = null;
    private static Boolean outwardQiBlockBreakingSyncedValue = null;
    private static UUID jindanFlightSyncedPlayer = null;
    private static Boolean jindanFlightSyncedValue = null;
    private static double jindanFlightScrollAccumulator = 0.0D;
    private static UUID combatSettingSyncedPlayer = null;
    private static Boolean combatSettingSyncedValue = null;
    private static UUID formationPlatePaymentSyncedPlayer = null;
    private static Boolean formationPlatePaymentSyncedValue = null;
    private static final DustParticleOptions GONG_LASER =
            new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 0.8F);
    private static final DustParticleOptions SHIXU_DOMAIN_DUST =
            new DustParticleOptions(new Vector3f(0.08F, 1.0F, 0.22F), 1.25F);
    private static final int SHIXU_SPHERE_POINTS = 480;
    private static final int SHIXU_PARTICLE_INTERVAL = 5;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));

    public static void register() {
        SwordComboAnimations.register();
        ClientCombatController.register();
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onMouseScrolling);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onRenderBlockScreenEffect);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onRenderNameTag);
        NeoForge.EVENT_BUS.addListener(InventorySystemTab::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ClientScreenFade::onOpening);
        NeoForge.EVENT_BUS.addListener(ClientScreenFade::onRenderPost);
    }

    private static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        double scrollDelta = event.getScrollDeltaY();
        if (mc.player == null
                || mc.getConnection() == null
                || scrollDelta == 0.0D
                || !ModClientConfig.enableJindanFlightMouseWheel()
                || !ClientData.jindanCreativeFlight()
                || !ClientData.jindanFlightControlActive()
                || CultivationLevels.getRealmIndex(
                        ClientData.cultivationData.cultivationLevel()) < 3
                || !mc.player.getAbilities().flying
                || mc.player.isCreative()
                || mc.player.isSpectator()
                || mc.player.hasEffect(ModEffects.YUFENG_JUE)
                || mc.player.getVehicle() instanceof FlyingSwordEntity) {
            jindanFlightScrollAccumulator = 0.0D;
            return;
        }

        event.setCanceled(true);
        jindanFlightScrollAccumulator += scrollDelta;
        int accumulatedSteps = (int) jindanFlightScrollAccumulator;
        if (accumulatedSteps == 0) {
            return;
        }

        jindanFlightScrollAccumulator -= accumulatedSteps;
        int remainingSteps = Math.max(-MAX_JINDAN_SCROLL_STEPS_PER_EVENT,
                Math.min(MAX_JINDAN_SCROLL_STEPS_PER_EVENT, accumulatedSteps));
        while (remainingSteps != 0) {
            int packetSteps = Math.max(-JINDAN_SCROLL_STEPS_PER_PACKET,
                    Math.min(JINDAN_SCROLL_STEPS_PER_PACKET, remainingSteps));
            PacketDistributor.sendToServer(
                    new ModPayloads.ServerboundAdjustJindanFlightSpeedPayload(packetSteps));
            remainingSteps -= packetSteps;
        }
    }

    private static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer player
                && com.example.immortal_cultivation_mod.item.DisguiseMaskItem.isWearing(player)
                && (Minecraft.getInstance().player == null
                    || !Minecraft.getInstance().player.hasEffect(ModEffects.SPIRIT_SIGHT)
                    || Minecraft.getInstance().player.distanceToSqr(player) > 64.0D)) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() != RenderBlockScreenEffectEvent.OverlayType.FIRE
                || !event.getPlayer().hasEffect(ModEffects.QI_FIRE_BURN)) {
            return;
        }
        event.setCanceled(true);
        renderBlueFireOverlay(event.getPoseStack());
    }

    private static void renderBlueFireOverlay(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        TextureAtlasSprite sprite = ModelBakery.FIRE_1.sprite();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();
        float alpha = 0.82F;

        for (int side = 0; side < 2; side++) {
            poseStack.pushPose();
            poseStack.translate(side == 0 ? -0.24F : 0.24F, -0.30F, -0.50F);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side == 0 ? 10.0F : -10.0F));
            Matrix4f sideMatrix = poseStack.last().pose();
            buffer.addVertex(sideMatrix, -0.65F, -0.50F, -0.50F).setUv(maxU, maxV).setColor(0.04F, 0.28F, 1.0F, alpha);
            buffer.addVertex(sideMatrix, 0.65F, -0.50F, -0.50F).setUv(minU, maxV).setColor(0.04F, 0.28F, 1.0F, alpha);
            buffer.addVertex(sideMatrix, 0.65F, 0.75F, -0.50F).setUv(minU, minV).setColor(0.04F, 0.28F, 1.0F, alpha);
            buffer.addVertex(sideMatrix, -0.65F, 0.75F, -0.50F).setUv(maxU, minV).setColor(0.04F, 0.28F, 1.0F, alpha);
            poseStack.popPose();
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        applyEarthEscapeMovement(mc);
        applyFrostFlightMovement(mc);
    }

    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            JindanTribulationOverlay.clear();
            ClientData.updateCombatState(false, 0.0F, 0.0F, 0, false);
            ClientCombatController.interruptHeavyAttack();
            autoChannelSyncedPlayer = null;
            autoChannelSyncedValue = null;
            outwardQiSyncedPlayer = null;
            outwardQiSyncedValue = null;
            outwardQiBlockBreakingSyncedValue = null;
            jindanFlightSyncedPlayer = null;
            jindanFlightSyncedValue = null;
            jindanFlightScrollAccumulator = 0.0D;
            ClientData.setJindanFlightControlActive(false);
            combatSettingSyncedPlayer = null;
            combatSettingSyncedValue = null;
            formationPlatePaymentSyncedPlayer = null;
            formationPlatePaymentSyncedValue = null;
            preparedChargeTicks = 0;
            wasPreparedCastKeyDown = false;
            preparedStateSent = false;
            preparedCastInterrupted = false;
            ClientData.resetPreparedCharge();
            return;
        }
        ClientData.tickClientAnimations();
        SwordComboAnimations.tickBindingPoses();
        ClientCombatController.tick(mc);
        JindanTribulationOverlay.tick();
        syncAutoChannelSetting(mc);
        syncOutwardQiSetting(mc);
        syncJindanFlightSetting(mc);
        syncCombatSetting(mc);
        syncFormationPlatePaymentSetting(mc);
        renderShixuDomainParticles(mc);

        var key = ModKeyMappings.OPEN_SPELL_WHEEL.get();
        boolean isHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getKey().getValue());

        boolean typing = mc.screen instanceof ChatScreen;

        if (typing) {
            wasSpellKeyDown = false;
            wasAttackKeyDown = false;
            wasUseKeyDown = false;
            wasJumpKeyDown = false;
            cancelPreparedCharge();
            wasReleaseWeaponKeyDown = false;
            wasSwordCaseKeyDown = false;
            swordCaseChargeTicks = 0;
            swordCaseReleaseTriggered = false;
            ClientData.resetSwordCaseCharge();
            windStepDoubleJumpUsed = false;
            return;
        }

        if (ClientData.cultivationData.isMeditating()) {
            mc.player.input.leftImpulse = 0;
            mc.player.input.forwardImpulse = 0;
            mc.player.input.jumping = false;
            mc.player.input.shiftKeyDown = false;
            Vec3 movement = mc.player.getDeltaMovement();
            if (movement.x != 0 || movement.z != 0) {
                mc.player.setDeltaMovement(0, movement.y, 0);
            }
            mc.player.setSprinting(false);
            mc.player.setPose(Pose.SITTING);
        }

        if (mc.player.hasEffect(ModEffects.EARTH_ESCAPE)) {
            applyEarthEscapeMovement(mc);
        } else if (mc.player.noPhysics && !mc.player.isSpectator()) {
            mc.player.noPhysics = false;
            mc.player.setNoGravity(false);
        }

        boolean spellWheelOpen = mc.screen instanceof SpellWheelScreen;
        boolean normalWorldView = mc.screen == null || spellWheelOpen;
        if (normalWorldView) {
            if (isHeld && !wasSpellKeyDown && mc.screen == null) {
                mc.setScreen(new SpellWheelScreen());
            }

            if (!isHeld && wasSpellKeyDown) {
                if (mc.screen instanceof SpellWheelScreen sws) {
                    String spell = sws.getSelectedSpell();
                    if (spell != null && mc.getConnection() != null) {
                        if (spell.startsWith(ClientSpellCombos.CAST_PREFIX)) {
                            var combo = ClientSpellCombos.get(spell.substring(ClientSpellCombos.CAST_PREFIX.length()));
                            if (combo != null) {
                                if (isRSelectableCombo(combo)) {
                                    ClientData.setPreparedSpellId(spell);
                                } else {
                                    cancelPreparedCharge();
                                    PacketDistributor.sendToServer(new ModPayloads.ServerboundCastComboPayload(
                                            combo.spellIds(), combo.castMode() == ClientSpellCombos.CastMode.SEQUENTIAL, 1.0F));
                                }
                            }
                        } else if (isRCastSpell(spell)) {
                            ClientData.setPreparedSpellId(spell);
                        } else {
                            cancelPreparedCharge();
                            PacketDistributor.sendToServer(
                                    new ModPayloads.ServerboundCastSpellPayload(spell, 1.0F)
                            );
                        }
                    }
                }

                if (mc.screen instanceof SpellWheelScreen) {
                    mc.setScreen(null);
                }
            }

            wasSpellKeyDown = isHeld;
        } else {
            wasSpellKeyDown = false;
        }

        while (ModKeyMappings.OPEN_STAT_MENU.get().consumeClick()) {
            if (mc.screen instanceof StatMenuScreen) {
                mc.setScreen(null);
            } else {
                mc.setScreen(new StatMenuScreen());
            }
        }

        while (ModKeyMappings.OPEN_SETTINGS.get().consumeClick()) {
            if (mc.screen instanceof CultivationSettingsScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new CultivationSettingsScreen());
            }
        }

        while (ModKeyMappings.OPEN_SPELL_SELECTION.get().consumeClick()) {
            if (mc.screen instanceof SpellSelectionScreen) {
                mc.setScreen(null);
            } else {
                mc.setScreen(new SpellSelectionScreen());
            }
        }

        while (ModKeyMappings.OPEN_COMBO_MENU.get().consumeClick()) {
            if (mc.screen instanceof SpellComboMenuScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new SpellComboMenuScreen());
            }
        }

        while (ModKeyMappings.MEDITATE.get().consumeClick()) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundMeditatePayload());
        }

        while (ModKeyMappings.INJECT_QI.get().consumeClick()) {
            if (mc.screen == null) {
                PacketDistributor.sendToServer(new ModPayloads.ServerboundInjectQiPayload());
            }
        }

        while (ModKeyMappings.TOGGLE_COMBAT_MODE.get().consumeClick()) {
            if (mc.screen == null) {
                PacketDistributor.sendToServer(
                        new CombatPayloads.ServerboundToggleManualCombatPayload());
            }
        }

        while (ModKeyMappings.TARGET_LOCK.get().consumeClick()) {
            ClientTargetLock.toggleLookingAt(mc);
        }

        tickReleaseWeaponKey(mc);

        while (ModKeyMappings.DEBUG_EYE.get().consumeClick()) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundToggleDebugEyePayload());
        }

        tickPreparedSpellCharge(mc);

        ClientTargetLock.tickCamera(mc);
        syncFrostFlightCameraMode(mc);
        spawnGongLaser(mc);

        if (mc.player.onGround() || !mc.player.hasEffect(ModEffects.WIND_STEP)) {
            windStepDoubleJumpUsed = false;
        }

        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !wasJumpKeyDown && mc.player.hasEffect(ModEffects.WIND_STEP) && !mc.player.onGround() && !windStepDoubleJumpUsed) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundWindStepJumpPayload());
            Vec3 movement = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(movement.x, 0.65D, movement.z);
            mc.player.fallDistance = 0.0F;
            windStepDoubleJumpUsed = true;
        }
        wasJumpKeyDown = jumpDown;

        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !wasAttackKeyDown && mc.screen == null) {
            if (ClientCombatController.ownsAttackInput(mc)) {
                // Combat sword/dao input is handled after its normal/heavy charge is known.
            } else if (mc.player.isShiftKeyDown() && mc.player.getMainHandItem().is(ModItems.SHIHUNFAN.get())) {
                PacketDistributor.sendToServer(new ModPayloads.ServerboundShihunfanSlashPayload());
            } else {
                PacketDistributor.sendToServer(new ModPayloads.ServerboundFlyingSwordPunchPayload(mc.player.isShiftKeyDown()));
                if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundLightBeamAirPunchPayload(mc.player.isShiftKeyDown()));
                }
            }
        }
        wasAttackKeyDown = attackDown;

        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !wasUseKeyDown && mc.screen == null) {
            if (HeavyIronBracerItem.isRealStack(mc.player.getMainHandItem())) {
                HeavyIronBracerItem.forceClientOffhandSwing(mc.player);
            }
            PacketDistributor.sendToServer(new ModPayloads.ServerboundFlyingSwordInteractPayload(mc.player.isShiftKeyDown()));
        }
        wasUseKeyDown = useDown;
    }

    private static void renderShixuDomainParticles(Minecraft mc) {
        if (mc.level == null || mc.player == null
                || mc.level.getGameTime() % SHIXU_PARTICLE_INTERVAL != 0L) {
            return;
        }

        double radius = ShixuChiliu.DOMAIN_RADIUS;
        double visibleDistance = radius + 48.0D;
        double phase = mc.level.getGameTime() * 0.075D;
        for (var caster : mc.level.players()) {
            if (!caster.hasEffect(ModEffects.SHIXU_CHILIU)
                    || caster.distanceToSqr(mc.player) > visibleDistance * visibleDistance) {
                continue;
            }
            double centerX = caster.getX();
            double centerY = caster.getY() + 1.0D;
            double centerZ = caster.getZ();
            for (int point = 0; point < SHIXU_SPHERE_POINTS; point++) {
                double normalizedY = 1.0D - 2.0D * (point + 0.5D) / SHIXU_SPHERE_POINTS;
                double horizontal = Math.sqrt(Math.max(0.0D,
                        1.0D - normalizedY * normalizedY));
                double angle = point * GOLDEN_ANGLE + phase;
                double x = centerX + Math.cos(angle) * horizontal * radius;
                double y = centerY + normalizedY * radius;
                double z = centerZ + Math.sin(angle) * horizontal * radius;
                mc.level.addParticle(SHIXU_DOMAIN_DUST, x, y, z,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void syncAutoChannelSetting(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        boolean enabled = ClientData.autoChannelWeaponQi();
        UUID playerId = mc.player.getUUID();
        if (!playerId.equals(autoChannelSyncedPlayer)
                || autoChannelSyncedValue == null
                || autoChannelSyncedValue != enabled) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundSetAutoChannelQiPayload(enabled));
            autoChannelSyncedPlayer = playerId;
            autoChannelSyncedValue = enabled;
        }
    }

    private static void syncOutwardQiSetting(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        boolean enabled = ClientData.outwardQiRelease();
        boolean blockBreaking = ClientData.outwardQiBlockBreaking();
        UUID playerId = mc.player.getUUID();
        if (!playerId.equals(outwardQiSyncedPlayer)
                || outwardQiSyncedValue == null
                || outwardQiSyncedValue != enabled
                || outwardQiBlockBreakingSyncedValue == null
                || outwardQiBlockBreakingSyncedValue != blockBreaking) {
            PacketDistributor.sendToServer(
                    new CombatPayloads.ServerboundSetOutwardQiPayload(enabled, blockBreaking));
            outwardQiSyncedPlayer = playerId;
            outwardQiSyncedValue = enabled;
            outwardQiBlockBreakingSyncedValue = blockBreaking;
        }
    }

    private static void syncJindanFlightSetting(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        boolean enabled = ClientData.jindanCreativeFlight();
        UUID playerId = mc.player.getUUID();
        if (!playerId.equals(jindanFlightSyncedPlayer)
                || jindanFlightSyncedValue == null
                || jindanFlightSyncedValue != enabled) {
            PacketDistributor.sendToServer(
                    new ModPayloads.ServerboundSetJindanFlightPayload(enabled));
            jindanFlightSyncedPlayer = playerId;
            jindanFlightSyncedValue = enabled;
        }
    }

    private static void syncCombatSetting(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        boolean enabled = ClientData.combatStateEnabled();
        UUID playerId = mc.player.getUUID();
        if (!playerId.equals(combatSettingSyncedPlayer)
                || combatSettingSyncedValue == null
                || combatSettingSyncedValue != enabled) {
            PacketDistributor.sendToServer(
                    new CombatPayloads.ServerboundSetCombatEnabledPayload(enabled));
            combatSettingSyncedPlayer = playerId;
            combatSettingSyncedValue = enabled;
        }
    }

    private static void syncFormationPlatePaymentSetting(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        boolean qiFirst = ClientData.formationPlateQiFirst();
        UUID playerId = mc.player.getUUID();
        if (!playerId.equals(formationPlatePaymentSyncedPlayer)
                || formationPlatePaymentSyncedValue == null
                || formationPlatePaymentSyncedValue != qiFirst) {
            PacketDistributor.sendToServer(
                    new ModPayloads.ServerboundSetFormationPlatePaymentOrderPayload(qiFirst));
            formationPlatePaymentSyncedPlayer = playerId;
            formationPlatePaymentSyncedValue = qiFirst;
        }
    }

    private static void tickReleaseWeaponKey(Minecraft mc) {
        boolean releaseKeyDown = ModKeyMappings.RELEASE_WEAPON.get().isDown();
        boolean swordCaseKeyDown = ModKeyMappings.SWORD_CASE.get().isDown();
        boolean worldView = mc.screen == null;
        boolean wearingSwordCase = worldView && SwordCaseItem.isEquipped(mc.player);

        if (wearingSwordCase) {
            if (swordCaseKeyDown) {
                if (!wasSwordCaseKeyDown) {
                    swordCaseChargeTicks = 0;
                    swordCaseReleaseTriggered = false;
                }
                if (!swordCaseReleaseTriggered) {
                    swordCaseChargeTicks = Math.min(SWORD_CASE_CHARGE_TICKS, swordCaseChargeTicks + 1);
                    ClientData.updateSwordCaseCharge(swordCaseChargeTicks, SWORD_CASE_CHARGE_TICKS);
                    if (swordCaseChargeTicks >= SWORD_CASE_CHARGE_TICKS) {
                        PacketDistributor.sendToServer(new ModPayloads.ServerboundSwordCaseActionPayload(true));
                        swordCaseReleaseTriggered = true;
                        ClientData.resetSwordCaseCharge();
                    }
                }
            } else if (wasSwordCaseKeyDown) {
                if (!swordCaseReleaseTriggered) {
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundSwordCaseActionPayload(false));
                }
                swordCaseChargeTicks = 0;
                swordCaseReleaseTriggered = false;
                ClientData.resetSwordCaseCharge();
            }
        } else {
            swordCaseChargeTicks = 0;
            swordCaseReleaseTriggered = false;
            ClientData.resetSwordCaseCharge();
            if (worldView && swordCaseKeyDown && !wasSwordCaseKeyDown) {
                PacketDistributor.sendToServer(new ModPayloads.ServerboundSwordCaseActionPayload(false));
            }
        }
        if (worldView && releaseKeyDown && !wasReleaseWeaponKeyDown) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundReleaseWeaponPayload());
        }
        wasReleaseWeaponKeyDown = releaseKeyDown;
        wasSwordCaseKeyDown = swordCaseKeyDown;
    }

    private static void tickPreparedSpellCharge(Minecraft mc) {
        var preparedCastKey = ModKeyMappings.CAST_PREPARED_SPELL.get();
        boolean preparedCastDown = InputConstants.isKeyDown(mc.getWindow().getWindow(), preparedCastKey.getKey().getValue());
        String preparedSpell = ClientData.preparedSpellId();
        boolean canUse = mc.screen == null
                && mc.getConnection() != null
                && !preparedSpell.isEmpty()
                && !ClientData.postureBroken();
        if (preparedCastInterrupted) {
            resetPreparedCharge(false);
            wasPreparedCastKeyDown = preparedCastDown;
            if (!preparedCastDown) {
                preparedCastInterrupted = false;
            }
            return;
        }
        if (preparedCastDown && canUse) {
            if (!wasPreparedCastKeyDown) {
                List<String> spellIds = preparedSpellIds(preparedSpell);
                if (!spellIds.isEmpty()) {
                    PacketDistributor.sendToServer(
                            new ModPayloads.ServerboundSpellPreparationPayload(
                                    spellIds, true, preparedSpellSequential(preparedSpell)));
                    preparedStateSent = true;
                }
            }
            int nextTicks = wasPreparedCastKeyDown ? Math.min(PREPARED_CHARGE_TICKS, preparedChargeTicks + 1) : 0;
            float nextScale = 1.0F + nextTicks / (float) PREPARED_CHARGE_TICKS;
            if (canAffordPreparedCharge(preparedSpell, nextScale)) {
                preparedChargeTicks = nextTicks;
            }
            ClientData.updatePreparedCharge(1.0F + preparedChargeTicks / (float) PREPARED_CHARGE_TICKS, true);
        } else if (!preparedCastDown && wasPreparedCastKeyDown) {
            float chargeScale = ClientData.preparedChargeScale();
            boolean castSent = false;
            if (canUse && canAffordPreparedCharge(preparedSpell, chargeScale)) {
                if (preparedSpell.startsWith(ClientSpellCombos.CAST_PREFIX)) {
                    ClientSpellCombos.SpellCombo combo = ClientSpellCombos.get(preparedSpell.substring(ClientSpellCombos.CAST_PREFIX.length()));
                    if (combo != null) {
                        PacketDistributor.sendToServer(new ModPayloads.ServerboundCastComboPayload(
                                combo.spellIds(), combo.castMode() == ClientSpellCombos.CastMode.SEQUENTIAL, chargeScale));
                        castSent = true;
                    }
                } else {
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundCastSpellPayload(preparedSpell, chargeScale));
                    castSent = true;
                }
            }
            if (castSent) {
                preparedStateSent = false;
            }
            resetPreparedCharge(!castSent);
        } else if (!preparedCastDown) {
            resetPreparedCharge(true);
        } else {
            preparedCastInterrupted = true;
            resetPreparedCharge(true);
        }
        wasPreparedCastKeyDown = preparedCastDown;
    }

    private static void cancelPreparedCharge() {
        resetPreparedCharge(true);
        preparedCastInterrupted = false;
    }

    private static void resetPreparedCharge(boolean notifyServer) {
        if (notifyServer && preparedStateSent) {
            PacketDistributor.sendToServer(
                    new ModPayloads.ServerboundSpellPreparationPayload(List.of(), false, false));
        }
        preparedStateSent = false;
        preparedChargeTicks = 0;
        wasPreparedCastKeyDown = false;
        ClientData.resetPreparedCharge();
    }

    public static void interruptPreparedCharge() {
        preparedCastInterrupted = true;
        resetPreparedCharge(false);
    }

    private static boolean canAffordPreparedCharge(String spellId, float chargeScale) {
        int baseCost = basePreparedSpellCost(spellId);
        if (baseCost <= 0) {
            return true;
        }
        var data = ClientData.cultivationData;
        long available = data.qi();
        if (com.example.immortal_cultivation_mod.attachment.CultivationMethods.isBloodDemon(
                data.activeCultivationMethod())) {
            available += data.blood();
        }
        return available >= chargedCost(baseCost, chargeScale);
    }

    private static int basePreparedSpellCost(String spellId) {
        if (spellId != null && spellId.startsWith(ClientSpellCombos.CAST_PREFIX)) {
            ClientSpellCombos.SpellCombo combo = ClientSpellCombos.get(spellId.substring(ClientSpellCombos.CAST_PREFIX.length()));
            if (combo == null) {
                return 0;
            }
            int total = 0;
            for (String comboSpell : combo.spellIds()) {
                total += basePreparedSpellCost(comboSpell);
            }
            return total;
        }
        String normalized = ModSpells.normalizeId(spellId);
        if (ModSpells.HANJING_SUOZHUA.equals(normalized)) {
            return Math.max(1, clientMaxQi() * 3 / 100);
        }
        if (ModSpells.SHUANGTIAN_QI.equals(normalized)) {
            return Math.max(1, clientMaxQi() * 5 / 100);
        }
        if (ModSpells.LIANSHEN_SHU.equals(normalized)) {
            return Math.max(1, clientMaxQi() * 75 / 100);
        }
        ModSpells.SpellDef spell = ModSpells.get(normalized);
        return spell == null ? 0 : Math.max(0, spell.qiCost());
    }

    private static int clientMaxQi() {
        var data = ClientData.cultivationData;
        var levelDef = com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(data.cultivationLevel());
        return Math.max(1, levelDef.maxQi() + data.maxQiBonus());
    }

    private static int chargedCost(int baseCost, float chargeScale) {
        float clamped = Math.max(1.0F, Math.min(2.0F, chargeScale));
        return Math.max(1, Math.round(baseCost * (1.0F + 4.0F * (clamped - 1.0F))));
    }

    private static boolean isRCastSpell(String spellId) {
        return ModSpells.requiresPreparation(spellId);
    }

    private static boolean isRSelectableCombo(ClientSpellCombos.SpellCombo combo) {
        for (String spellId : combo.spellIds()) {
            if (isRCastSpell(spellId)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> preparedSpellIds(String preparedSpell) {
        if (preparedSpell == null || preparedSpell.isEmpty()) {
            return List.of();
        }
        if (preparedSpell.startsWith(ClientSpellCombos.CAST_PREFIX)) {
            ClientSpellCombos.SpellCombo combo = ClientSpellCombos.get(
                    preparedSpell.substring(ClientSpellCombos.CAST_PREFIX.length()));
            return combo == null ? List.of() : combo.spellIds();
        }
        String normalized = ModSpells.normalizeId(preparedSpell);
        return ModSpells.get(normalized) == null ? List.of() : List.of(normalized);
    }

    private static boolean preparedSpellSequential(String preparedSpell) {
        if (preparedSpell == null || !preparedSpell.startsWith(ClientSpellCombos.CAST_PREFIX)) {
            return false;
        }
        ClientSpellCombos.SpellCombo combo = ClientSpellCombos.get(
                preparedSpell.substring(ClientSpellCombos.CAST_PREFIX.length()));
        return combo != null && combo.castMode() == ClientSpellCombos.CastMode.SEQUENTIAL;
    }

    private static void applyEarthEscapeMovement(Minecraft mc) {
        if (mc.player == null || !mc.player.hasEffect(ModEffects.EARTH_ESCAPE)) {
            return;
        }

        mc.player.noPhysics = true;
        mc.player.setNoGravity(true);
        mc.player.fallDistance = 0.0F;

        float forward = mc.player.input.forwardImpulse;
        float left = mc.player.input.leftImpulse;
        double yaw = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double speed = 0.16D;
        double dx = (left * cos - forward * sin) * speed;
        double dz = (forward * cos + left * sin) * speed;
        double dy = 0.0D;
        if (mc.player.input.jumping) {
            dy += speed * 0.75D;
        }
        if (mc.player.input.shiftKeyDown) {
            dy -= speed * 0.75D;
        }

        Vec3 movement = new Vec3(dx, dy, dz);
        mc.player.setDeltaMovement(movement);
        if (movement.lengthSqr() > 0.0D) {
            mc.player.move(MoverType.SELF, movement);
        }
    }

    private static void applyFrostFlightMovement(Minecraft mc) {
        if (mc.player == null || !mc.player.hasEffect(ModEffects.FROST_FLIGHT) || mc.screen != null) {
            return;
        }

        mc.player.fallDistance = 0.0F;
        mc.player.startFallFlying();
        mc.player.setPose(Pose.FALL_FLYING);
        Vec3 look = mc.player.getLookAngle().normalize();
        double forwardInput = Math.max(0.45D, mc.player.input.forwardImpulse);
        double horizontalLook = Math.max(0.22D, Math.sqrt(look.x * look.x + look.z * look.z));
        double speed = 1.85D * forwardInput;
        double dx = look.x / horizontalLook * speed;
        double dz = look.z / horizontalLook * speed;
        double dy = look.y * 1.05D - 0.035D;
        if (mc.player.input.jumping) {
            dy += 0.42D;
        }
        if (mc.player.input.shiftKeyDown) {
            dy -= 0.72D;
        }

        Vec3 current = mc.player.getDeltaMovement();
        Vec3 target = new Vec3(dx, dy, dz);
        mc.player.setDeltaMovement(current.lerp(target, 0.48D));
    }

    private static void syncFrostFlightCameraMode(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            lastFrostFlightFirstPerson = null;
            return;
        }
        if (!mc.player.hasEffect(ModEffects.FROST_FLIGHT)) {
            if (lastFrostFlightFirstPerson != null) {
                lastFrostFlightFirstPerson = null;
            }
            return;
        }
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        if (lastFrostFlightFirstPerson == null || lastFrostFlightFirstPerson.booleanValue() != firstPerson) {
            lastFrostFlightFirstPerson = firstPerson;
            PacketDistributor.sendToServer(new ModPayloads.ServerboundFrostFlightCameraPayload(firstPerson));
        }
    }

    private static void spawnGongLaser(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.screen != null || !holdingGong(mc.player.getMainHandItem(), mc.player.getOffhandItem())) {
            return;
        }
        Vec3 pos = mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS
                ? mc.hitResult.getLocation()
                : mc.player.getEyePosition().add(mc.player.getLookAngle().scale(mc.player.blockInteractionRange()));
        for (int i = 0; i < 4; i++) {
            mc.level.addParticle(GONG_LASER, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean holdingGong(ItemStack mainHand, ItemStack offHand) {
        return mainHand.is(ModItems.YINHUN_GONG.get()) || offHand.is(ModItems.YINHUN_GONG.get());
    }
}
