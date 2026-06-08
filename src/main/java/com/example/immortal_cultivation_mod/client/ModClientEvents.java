package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
import com.example.immortal_cultivation_mod.client.particle.FireballTrailParticle;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.screen.StatMenuScreen;
import com.example.immortal_cultivation_mod.screen.SpellWheelScreen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
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

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, QiBarOverlay.HUD_ID, QiBarOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIREBALL_PROJECTILE.get(), ThrownItemRenderer::new);
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
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var key = ModKeyMappings.OPEN_SPELL_WHEEL.get();
        boolean isHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getKey().getValue());

        boolean typing = mc.screen instanceof ChatScreen;

        if (typing) {
            wasSpellKeyDown = false;
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

        while (ModKeyMappings.MEDITATE.get().consumeClick()) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundMeditatePayload());
        }
    }
}
