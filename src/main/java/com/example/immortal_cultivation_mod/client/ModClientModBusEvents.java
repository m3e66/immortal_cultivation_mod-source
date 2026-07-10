package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
import com.example.immortal_cultivation_mod.client.renderer.Blindfold2CurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlindfoldCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CoffinCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CultivatorCorpseRenderer;
import com.example.immortal_cultivation_mod.client.renderer.EmptyEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FanrenNpcRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlagCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlyingSwordRenderer;
import com.example.immortal_cultivation_mod.client.renderer.GeoRockBlockRenderer;
import com.example.immortal_cultivation_mod.client.renderer.GudiaoRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JadePendantCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCloneRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCultivatorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ScaledFireballRenderer;
import com.example.immortal_cultivation_mod.client.renderer.StrawHatCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.YinhunGongCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.XuyingShadowRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhenhunBellCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhujiCultivatorRenderer;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class ModClientModBusEvents {
    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModClientModBusEvents::registerGuiLayers);
        eventBus.addListener(ModClientModBusEvents::registerRenderers);
        eventBus.addListener(ModClientModBusEvents::registerParticleProviders);
        eventBus.addListener(ModClientModBusEvents::registerMenuScreens);
        eventBus.addListener(ModClientModBusEvents::onClientSetup);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, QiBarOverlay.HUD_ID, QiBarOverlay.INSTANCE);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIREBALL_PROJECTILE.get(), ScaledFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.IGNITE_FLARE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHT_BEAM_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LINGZHI_BULLET_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_BLADE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SMOKE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SLIDING_WATER_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ABSORB_CULTIVATION_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DINGSHEN_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.KONGSHI_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_CRY_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIUGUANG_JIANYING_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DUANLIU_KONGDUN_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.YIHEN_CI_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_FX_ANCHOR.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.DUANLIU_KONGDUN_DOME.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHENSHAN_PALM.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.GUDIAO_SCRATCH.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.XUYING_SHADOW.get(), XuyingShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHUJI_CULTIVATOR.get(), ZhujiCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_CULTIVATOR.get(), JindanCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.FANREN_NPC.get(), FanrenNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.GUDIAO.get(), GudiaoRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATOR_CORPSE.get(), CultivatorCorpseRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_CLONE.get(), JindanCloneRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN.get(), JindanEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GEO_ROCK.get(), GeoRockBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DING.get(), GeoRockBlockRenderer::new);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.QI_POUCH.get(),
                com.example.immortal_cultivation_mod.screen.QiPouchScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.DING.get(),
                com.example.immortal_cultivation_mod.screen.DingScreen::new);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIUJIN_SAND.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NUOMI_PLANT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NUOMI_DUST.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MODOU_LINE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LING_FIRE.get(), RenderType.cutout());
            CuriosRendererRegistry.register(ModItems.COFFIN.get(), CoffinCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.FLAG.get(), FlagCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLINDFOLD.get(), BlindfoldCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLINDFOLD2.get(), Blindfold2CurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.STRAW_HAT.get(), StrawHatCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.JADE_PENDANT.get(), JadePendantCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ZHENHUN_BELL.get(), ZhenhunBellCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.YINHUN_GONG.get(), YinhunGongCurioRenderer::new);
        });
    }
}
