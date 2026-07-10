package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
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
import com.example.immortal_cultivation_mod.client.renderer.GudiaoRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCultivatorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhujiCultivatorRenderer;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.client.particle.FireballTrailParticle;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.screen.StatMenuScreen;
import com.example.immortal_cultivation_mod.screen.SpellComboMenuScreen;
import com.example.immortal_cultivation_mod.screen.SpellSelectionScreen;
import com.example.immortal_cultivation_mod.screen.SpellWheelScreen;
import com.example.immortal_cultivation_mod.spell.ModSpells;

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
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.Set;

public class ModClientEvents {
    private static boolean wasSpellKeyDown = false;
    private static boolean wasAttackKeyDown = false;
    private static boolean wasUseKeyDown = false;
    private static boolean wasJumpKeyDown = false;
    private static boolean wasPreparedCastKeyDown = false;
    private static int preparedChargeTicks = 0;
    private static boolean windStepDoubleJumpUsed = false;
    private static final int PREPARED_CHARGE_TICKS = 40;
    private static Boolean lastFrostFlightFirstPerson = null;
    private static final Set<String> R_CAST_SPELLS = Set.of(
            ModSpells.FIREBALL,
            ModSpells.YINLEI_JUE,
            ModSpells.WULEI_ZHENGFA,
            ModSpells.LIUGUANG_JIANYING,
            ModSpells.SLIDING_WATER,
            ModSpells.LINGZHI_BULLET,
            ModSpells.WIND_BLADE,
            ModSpells.SMOKE_ART,
            ModSpells.MICHEN_ZHANG,
            ModSpells.IGNITE_FLARE,
            ModSpells.ZHENSHAN_PALM,
            ModSpells.ABSORB_CULTIVATION,
            ModSpells.TUNTIAN,
            ModSpells.DINGSHEN,
            ModSpells.HANJING_SUOZHUA,
            ModSpells.SHUANGTIAN_QI,
            ModSpells.YUQI_SHU,
            ModSpells.KONGSHI_SHU,
            ModSpells.DUANLIU_KONGDUN,
            ModSpells.YIHEN_CI
    );
    private static final DustParticleOptions GONG_LASER =
            new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 0.8F);

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ModClientEvents::onRenderBlockScreenEffect);
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
        if (mc.player == null) return;
        ClientData.tickClientAnimations();

        var key = ModKeyMappings.OPEN_SPELL_WHEEL.get();
        boolean isHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getKey().getValue());

        boolean typing = mc.screen instanceof ChatScreen;

        if (typing) {
            wasSpellKeyDown = false;
            wasAttackKeyDown = false;
            wasUseKeyDown = false;
            wasJumpKeyDown = false;
            wasPreparedCastKeyDown = false;
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

        while (ModKeyMappings.TARGET_LOCK.get().consumeClick()) {
            ClientTargetLock.toggleLookingAt(mc);
        }

        while (ModKeyMappings.RELEASE_WEAPON.get().consumeClick()) {
            if (mc.screen == null) {
                PacketDistributor.sendToServer(new ModPayloads.ServerboundReleaseWeaponPayload());
            }
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
            PacketDistributor.sendToServer(new ModPayloads.ServerboundFlyingSwordPunchPayload(mc.player.isShiftKeyDown()));
            if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                PacketDistributor.sendToServer(new ModPayloads.ServerboundLightBeamAirPunchPayload(mc.player.isShiftKeyDown()));
            }
        }
        wasAttackKeyDown = attackDown;

        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !wasUseKeyDown && mc.screen == null) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundFlyingSwordInteractPayload(mc.player.isShiftKeyDown()));
        }
        wasUseKeyDown = useDown;
    }

    private static void tickPreparedSpellCharge(Minecraft mc) {
        var preparedCastKey = ModKeyMappings.CAST_PREPARED_SPELL.get();
        boolean preparedCastDown = InputConstants.isKeyDown(mc.getWindow().getWindow(), preparedCastKey.getKey().getValue());
        String preparedSpell = ClientData.preparedSpellId();
        boolean canUse = mc.screen == null && mc.getConnection() != null && !preparedSpell.isEmpty();
        if (preparedCastDown && canUse) {
            int nextTicks = wasPreparedCastKeyDown ? Math.min(PREPARED_CHARGE_TICKS, preparedChargeTicks + 1) : 0;
            float nextScale = 1.0F + nextTicks / (float) PREPARED_CHARGE_TICKS;
            if (canAffordPreparedCharge(preparedSpell, nextScale)) {
                preparedChargeTicks = nextTicks;
            }
            ClientData.updatePreparedCharge(1.0F + preparedChargeTicks / (float) PREPARED_CHARGE_TICKS, true);
        } else if (!preparedCastDown && wasPreparedCastKeyDown) {
            float chargeScale = ClientData.preparedChargeScale();
            if (canUse && canAffordPreparedCharge(preparedSpell, chargeScale)) {
                if (preparedSpell.startsWith(ClientSpellCombos.CAST_PREFIX)) {
                    ClientSpellCombos.SpellCombo combo = ClientSpellCombos.get(preparedSpell.substring(ClientSpellCombos.CAST_PREFIX.length()));
                    if (combo != null) {
                        PacketDistributor.sendToServer(new ModPayloads.ServerboundCastComboPayload(
                                combo.spellIds(), combo.castMode() == ClientSpellCombos.CastMode.SEQUENTIAL, chargeScale));
                    }
                } else {
                    PacketDistributor.sendToServer(new ModPayloads.ServerboundCastSpellPayload(preparedSpell, chargeScale));
                }
            }
            preparedChargeTicks = 0;
            ClientData.resetPreparedCharge();
        } else if (!preparedCastDown) {
            preparedChargeTicks = 0;
            ClientData.resetPreparedCharge();
        }
        wasPreparedCastKeyDown = preparedCastDown;
    }

    private static void cancelPreparedCharge() {
        preparedChargeTicks = 0;
        wasPreparedCastKeyDown = false;
        ClientData.resetPreparedCharge();
    }

    private static boolean canAffordPreparedCharge(String spellId, float chargeScale) {
        int baseCost = basePreparedSpellCost(spellId);
        if (baseCost <= 0) {
            return true;
        }
        return ClientData.cultivationData.qi() >= chargedCost(baseCost, chargeScale);
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
        return R_CAST_SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    private static boolean isRSelectableCombo(ClientSpellCombos.SpellCombo combo) {
        for (String spellId : combo.spellIds()) {
            if (isRCastSpell(spellId)) {
                return true;
            }
        }
        return false;
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
