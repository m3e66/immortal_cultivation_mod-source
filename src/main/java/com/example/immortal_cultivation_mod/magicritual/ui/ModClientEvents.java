package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.Input;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ModClientEvents {
   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(ModClientEvents::registerScreens);
      modEventBus.addListener(ModClientEvents::onClientReload);
      NeoForge.EVENT_BUS.addListener(ModClientEvents::onInteractionKey);
      NeoForge.EVENT_BUS.addListener(ModClientEvents::onComputeFogColor);
      NeoForge.EVENT_BUS.addListener(ModClientEvents::onRenderFog);
      NeoForge.EVENT_BUS.addListener(ModClientEvents::onMovementInput);
   }

   public static void registerScreens(RegisterMenuScreensEvent event) {
      event.register((MenuType)ModMenuTypes.CENTER_FUEL.get(), CenterFuelScreen::new);
   }

   public static void onClientReload(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((barrier, manager, prepProfiler, applyProfiler, backgroundExecutor, gameExecutor) -> {
         MagicCenterBlockEntity.loadPatterns(manager);
         return barrier.wait(null);
      });
   }

   public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
      if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.player.getMainHandItem().is((Item)ModItems.RITUAL_DAGGER.get())) {
            event.setCanceled(true);
            if (Screen.hasShiftDown()) {
               mc.setScreen(new RadiusSelectionScreen());
            } else {
               mc.setScreen(new CircleSelectionScreen());
            }
         }
      }

   }

   public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
      Entity var2 = event.getCamera().getEntity();
      if (var2 instanceof LivingEntity living) {
         if (living.hasEffect(ModEffects.FOG)) {
            event.setRed(1.0F);
            event.setGreen(1.0F);
            event.setBlue(1.0F);
         }
      }

   }

   public static void onRenderFog(ViewportEvent.RenderFog event) {
      Entity var2 = event.getCamera().getEntity();
      if (var2 instanceof LivingEntity living) {
         if (living.hasEffect(ModEffects.FOG)) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(5.0F);
            event.setCanceled(true);
         }
      }

   }

   public static void onMovementInput(MovementInputUpdateEvent event) {
      if (event.getEntity().hasEffect(ModEffects.INVERTED_CONTROL)) {
         Input input = event.getInput();
         input.leftImpulse = -input.leftImpulse;
         input.forwardImpulse = -input.forwardImpulse;
      }

   }
}
