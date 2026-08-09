package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
import com.example.immortal_cultivation_mod.client.hud.JindanTribulationOverlay;
import com.example.immortal_cultivation_mod.client.hud.MysteriousRingDialogueOverlay;
import com.example.immortal_cultivation_mod.client.particle.DragonVfxParticles;
import com.example.immortal_cultivation_mod.client.particle.MethodQiParticle;
import com.example.immortal_cultivation_mod.client.renderer.MagicRitualGlyphRenderer;
import com.example.immortal_cultivation_mod.client.renderer.AzureSpiritSilkwormRenderer;
import com.example.immortal_cultivation_mod.client.renderer.Blindfold2CurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlindfoldCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BloodApeDemonGeneralRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CoffinCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ClothSheathCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CultivatorCorpseRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CrimsonScaleFlameLizardRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CrimsonSkyFlameFloodDragonRenderer;
import com.example.immortal_cultivation_mod.client.renderer.DaofuCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.DisguiseMaskCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.EmptyEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SystemManifestRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FanrenNpcRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlagCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlyingSwordRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlyingNeedleRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FuchenBindingPlumeRenderer;
import com.example.immortal_cultivation_mod.client.renderer.GeoRockBlockRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JadePendantCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCloneRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCultivatorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.LiusanRenderer;
import com.example.immortal_cultivation_mod.client.renderer.NoobHatCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ProtectiveJadePendantCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ScaledFireballRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SoulRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ShihunfanRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ShihunfanGhostRenderer;
import com.example.immortal_cultivation_mod.client.renderer.StrawHatCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SwordCaseCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ThrowingKnifeRenderer;
import com.example.immortal_cultivation_mod.client.renderer.YinhunGongCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.XuyingShadowRenderer;
import com.example.immortal_cultivation_mod.client.renderer.XiujianCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhenhunBellCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhongyiCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhujiCultivatorRenderer;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
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
        event.registerAboveAll(
                MysteriousRingDialogueOverlay.HUD_ID,
                MysteriousRingDialogueOverlay.INSTANCE);
        event.registerAboveAll(JindanTribulationOverlay.HUD_ID, JindanTribulationOverlay.INSTANCE);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLOOD_DROP.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.FIREBALL_PROJECTILE.get(), ScaledFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.FIREBALL_TALISMAN_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_FLAME_FIREBALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_DRAGON_SCALE_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRIT_SILK_PROJECTILE.get(), ThrownItemRenderer::new);
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
        event.registerEntityRenderer(ModEntities.SUOKONG_PROJECTILE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.XINLIAO_FIREBALL.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.STARFALL_PROJECTILE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWING_KNIFE.get(), ThrowingKnifeRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_NEEDLE.get(), FlyingNeedleRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_FX_ANCHOR.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.DUANLIU_KONGDUN_DOME.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHENSHAN_PALM.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.XUYING_SHADOW.get(), XuyingShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL.get(), SoulRenderer::new);
        event.registerEntityRenderer(ModEntities.SHIHUNFAN.get(), ShihunfanRenderer::new);
        event.registerEntityRenderer(ModEntities.SHIHUNFAN_SOUL_WAVE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SHIHUNFAN_GHOST.get(), ShihunfanGhostRenderer::new);
        event.registerEntityRenderer(ModEntities.FUCHEN_BINDING_PLUME.get(), FuchenBindingPlumeRenderer::new);
        event.registerEntityRenderer(ModEntities.WEAPON_QI_SLASH.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHUJI_CULTIVATOR.get(), ZhujiCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_CULTIVATOR.get(), JindanCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.LIUSAN.get(), LiusanRenderer::new);
        event.registerEntityRenderer(ModEntities.FANREN_NPC.get(), FanrenNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(),
                CrimsonScaleFlameLizardRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(),
                CrimsonSkyFlameFloodDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_APE_DEMON_GENERAL.get(),
                BloodApeDemonGeneralRenderer::new);
        event.registerEntityRenderer(ModEntities.ESCAPED_YUANYING_CORE.get(),
                EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AZURE_SPIRIT_SILKWORM.get(),
                AzureSpiritSilkwormRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATOR_CORPSE.get(), CultivatorCorpseRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_CLONE.get(), JindanCloneRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN.get(), JindanEntityRenderer::new);
        event.registerEntityRenderer(
                ModEntities.SYSTEM_MANIFEST.get(),
                SystemManifestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GEO_ROCK.get(), GeoRockBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DING.get(), GeoRockBlockRenderer::new);
        event.registerBlockEntityRenderer(
                com.example.immortal_cultivation_mod.magicritual.block.custom.ModBlockEntities.MAGIC_CENTER_BE.get(),
                MagicRitualGlyphRenderer::new);
        event.registerBlockEntityRenderer(
                com.example.immortal_cultivation_mod.magicritual.block.custom.ModBlockEntities.MAGIC_CIRCLE_BE.get(),
                MagicRitualGlyphRenderer::new);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        registerMethodQiParticle(event, ModParticles.METHOD_QI_METAL.get(), 0xF4D35E);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_WOOD.get(), 0x55C96B);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_WATER.get(), 0x3D9CFF);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_FIRE.get(), 0xFF572E);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_EARTH.get(), 0xC58A3A);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_THUNDER.get(), 0xA967FF);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_ICE.get(), 0x9BEFFF);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_WIND.get(), 0x9CE7BD);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_LIGHT.get(), 0xFFF4B0);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_DARK.get(), 0x6D3A91);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_BLOOD.get(), 0xD71920);
        registerMethodQiParticle(event, ModParticles.METHOD_QI_NEUTRAL.get(), 0xFFD66B);
        event.registerSpriteSet(
                ModParticles.DRAGON_EMBER.get(), DragonVfxParticles.EmberProvider::new);
        event.registerSpriteSet(
                ModParticles.DRAGON_SCALE_SPARK.get(), DragonVfxParticles.ScaleSparkProvider::new);
        event.registerSpriteSet(
                ModParticles.DRAGON_FIRE_SIGIL.get(), DragonVfxParticles.FireSigilProvider::new);
    }

    private static void registerMethodQiParticle(
            RegisterParticleProvidersEvent event,
            net.minecraft.core.particles.SimpleParticleType type,
            int color) {
        event.registerSpriteSet(type, sprites -> new MethodQiParticle.Provider(sprites, color));
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.QI_POUCH.get(),
                com.example.immortal_cultivation_mod.screen.QiPouchScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.DING.get(),
                com.example.immortal_cultivation_mod.screen.DingScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.RESTRICTION_TABLE.get(),
                com.example.immortal_cultivation_mod.screen.RestrictionTableScreen::new);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIUJIN_SAND.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NUOMI_PLANT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LINGMU.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LINGMU_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NUOMI_DUST.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MODOU_LINE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LING_FIRE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STAR_MARK.get(), RenderType.cutout());
            CuriosRendererRegistry.register(ModItems.COFFIN.get(), CoffinCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.FLAG.get(), FlagCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.CLOTH_SHEATH.get(), ClothSheathCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.SWORD_CASE.get(), SwordCaseCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.XIUJIAN.get(), XiujianCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLINDFOLD.get(), BlindfoldCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLINDFOLD2.get(), Blindfold2CurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.STRAW_HAT.get(), StrawHatCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.NOOB_HAT.get(), NoobHatCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.JADE_PENDANT.get(), JadePendantCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DAOFU_CHESTPLATE.get(), DaofuCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ZHONGYI.get(), ZhongyiCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DISGUISE_MASK.get(), DisguiseMaskCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.PROTECTIVE_JADE_PENDANT.get(),
                    ProtectiveJadePendantCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ZHENHUN_BELL.get(), ZhenhunBellCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.YINHUN_GONG.get(), YinhunGongCurioRenderer::new);
        });
    }
}
