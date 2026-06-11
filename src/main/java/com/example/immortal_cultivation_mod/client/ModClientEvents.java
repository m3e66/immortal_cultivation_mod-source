package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
import com.example.immortal_cultivation_mod.client.renderer.EmptyEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.GeoRockBlockRenderer;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.client.particle.FireballTrailParticle;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.screen.StatMenuScreen;
import com.example.immortal_cultivation_mod.screen.SpellSelectionScreen;
import com.example.immortal_cultivation_mod.screen.SpellWheelScreen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    private static boolean wasSpellKeyDown = false;
    private static boolean wasAttackKeyDown = false;
    private static boolean wasJumpKeyDown = false;

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, QiBarOverlay.HUD_ID, QiBarOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIREBALL_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.IGNITE_FLARE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHT_BEAM_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LINGZHI_BULLET_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_BLADE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SMOKE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHENSHAN_PALM.get(), EmptyEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GEO_ROCK.get(), GeoRockBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FIREBALL_TRAIL.get(), FireballTrailParticle.Provider::new);
        event.registerSpriteSet(ModParticles.QI_ORB.get(), FireballTrailParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.QI_POUCH.get(),
                com.example.immortal_cultivation_mod.screen.QiPouchScreen::new);
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        applyEarthEscapeMovement(mc);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var key = ModKeyMappings.OPEN_SPELL_WHEEL.get();
        boolean isHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getKey().getValue());

        boolean typing = mc.screen instanceof ChatScreen;

        if (typing) {
            wasSpellKeyDown = false;
            wasAttackKeyDown = false;
            wasJumpKeyDown = false;
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

        if (isHeld && !wasSpellKeyDown) {
            mc.setScreen(new SpellWheelScreen());
        }

        if (!isHeld && wasSpellKeyDown) {
            if (mc.screen instanceof SpellWheelScreen sws) {
                String spell = sws.getSelectedSpell();
                if (spell != null && mc.getConnection() != null) {
                    PacketDistributor.sendToServer(
                            new ModPayloads.ServerboundCastSpellPayload(spell)
                    );
                }
            }

            if (mc.screen instanceof SpellWheelScreen) {
                mc.setScreen(null);
            }
        }

        wasSpellKeyDown = isHeld;

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

        while (ModKeyMappings.MEDITATE.get().consumeClick()) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundMeditatePayload());
        }

        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !wasJumpKeyDown && mc.player.hasEffect(ModEffects.WIND_STEP) && !mc.player.onGround()) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundWindStepJumpPayload());
            Vec3 movement = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(movement.x, 0.65D, movement.z);
            mc.player.fallDistance = 0.0F;
        }
        wasJumpKeyDown = jumpDown;

        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !wasAttackKeyDown && mc.screen == null
                && (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS)) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundLightBeamAirPunchPayload(mc.player.isShiftKeyDown()));
        }
        wasAttackKeyDown = attackDown;
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
}
