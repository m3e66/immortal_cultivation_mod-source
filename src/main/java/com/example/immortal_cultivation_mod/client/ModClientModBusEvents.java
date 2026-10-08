package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.client.flame.FlameShotRenderer;
import com.example.immortal_cultivation_mod.client.flame.SpiritFlameRenderer;
import com.example.immortal_cultivation_mod.client.flame.SpiritFlameBlockRenderer;
import com.example.immortal_cultivation_mod.client.flame.VesselFlameParticle;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.client.hud.QiBarOverlay;
import com.example.immortal_cultivation_mod.client.hud.JindanTribulationOverlay;
import com.example.immortal_cultivation_mod.client.hud.DaoGuidanceOverlay;
import com.example.immortal_cultivation_mod.client.hud.MysteriousRingDialogueOverlay;
import com.example.immortal_cultivation_mod.client.particle.DragonVfxParticles;
import com.example.immortal_cultivation_mod.client.particle.BlackFlameVfxParticles;
import com.example.immortal_cultivation_mod.client.particle.JindanShockwaveParticles;
import com.example.immortal_cultivation_mod.client.particle.MethodQiParticle;
import com.example.immortal_cultivation_mod.client.particle.SolarVfxParticles;
import com.example.immortal_cultivation_mod.client.renderer.MagicRitualGlyphRenderer;
import com.example.immortal_cultivation_mod.client.renderer.AzureSpiritSilkwormRenderer;
import com.example.immortal_cultivation_mod.client.renderer.AsteroidRenderer;
import com.example.immortal_cultivation_mod.client.renderer.Blindfold2CurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlindfoldCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CoinMaskCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlackFlameDomainRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlackTortoiseHeartguardMirrorCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BloodApeDemonGeneralRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CoffinCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ClothSheathCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.RigidSheathCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CosmicVoidPortalRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CultivatorCorpseRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CultivationBestiaryBeastRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CrimsonScaleFlameLizardRenderer;
import com.example.immortal_cultivation_mod.client.renderer.CrimsonSkyFlameFloodDragonRenderer;
import com.example.immortal_cultivation_mod.client.renderer.DaofuCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.DeadSkeletonBlockRenderer;
import com.example.immortal_cultivation_mod.client.renderer.DisguiseMaskCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.EmptyEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ElementalTechniqueAreaRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ElementalTechniqueProjectileRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SolarTechniqueModelRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SpiritualLightRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SpellTriggerProjectileRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SystemManifestRenderer;
import com.example.immortal_cultivation_mod.client.renderer.TalismanActivationRenderer;
import com.example.immortal_cultivation_mod.client.renderer.TalismanDrawingTableRenderer;
import com.example.immortal_cultivation_mod.client.renderer.TalismanWardRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FanrenNpcRenderer;
import com.example.immortal_cultivation_mod.client.renderer.HotelNpcRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FireballTalismanProjectileRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlagCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlyingSwordRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FlyingNeedleRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FuchenBindingPlumeRenderer;
import com.example.immortal_cultivation_mod.client.renderer.GeoRockBlockRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JadePendantCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCloneRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanCultivatorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.JindanEntityRenderer;
import com.example.immortal_cultivation_mod.client.renderer.KongmingBeetleRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ThunderhornRockBadgerRenderer;
import com.example.immortal_cultivation_mod.client.renderer.FrostspineCaveBearRenderer;
import com.example.immortal_cultivation_mod.client.renderer.PhantomVeilFoxRenderer;
import com.example.immortal_cultivation_mod.client.renderer.PhantomVeilFoxCloneRenderer;
import com.example.immortal_cultivation_mod.client.renderer.LianqiCultivatorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.MethodTechniqueAreaRenderer;
import com.example.immortal_cultivation_mod.client.renderer.MedicineCabinetRenderer;
import com.example.immortal_cultivation_mod.client.renderer.NoobHatCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.PetalTechniqueRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ProtectiveJadePendantCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.RestrictionTableRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ScaledFireballRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ShanbengSealRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SoulRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ShihunfanRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ShihunfanGhostRenderer;
import com.example.immortal_cultivation_mod.client.renderer.StrawHatCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SwordCaseCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SwordTechniqueMarkRenderer;
import com.example.immortal_cultivation_mod.client.renderer.SwordTechniqueProjectileRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ThrowingKnifeRenderer;
import com.example.immortal_cultivation_mod.client.renderer.TianyunFenchengMeteorRenderer;
import com.example.immortal_cultivation_mod.client.renderer.YinhunGongCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.XuyingShadowRenderer;
import com.example.immortal_cultivation_mod.client.renderer.YaoguangFlashRenderer;
import com.example.immortal_cultivation_mod.client.renderer.WantengVineRenderer;
import com.example.immortal_cultivation_mod.client.renderer.VaultGuardianRenderer;
import com.example.immortal_cultivation_mod.client.renderer.XiujianCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhenhunBellCurioRenderer;
import com.example.immortal_cultivation_mod.client.renderer.ZhenshanPalmRenderer;
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
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
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
        eventBus.addListener(ModClientModBusEvents::registerDimensionSpecialEffects);
    }

    private static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "cosmic_void"),
                new CosmicVoidDimensionEffects());
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, QiBarOverlay.HUD_ID, QiBarOverlay.INSTANCE);
        event.registerAboveAll(
                MysteriousRingDialogueOverlay.HUD_ID,
                MysteriousRingDialogueOverlay.INSTANCE);
        event.registerAboveAll(JindanTribulationOverlay.HUD_ID, JindanTribulationOverlay.INSTANCE);
        event.registerAboveAll(DaoGuidanceOverlay.HUD_ID, DaoGuidanceOverlay.INSTANCE);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPIRIT_FLAME.get(), SpiritFlameRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SPIRIT_FLAME.get(), SpiritFlameBlockRenderer::new);
        event.registerEntityRenderer(ModEntities.FLAME_SHOT.get(), FlameShotRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_DROP.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTEROID.get(), AsteroidRenderer::new);

        event.registerEntityRenderer(ModEntities.FIREBALL_PROJECTILE.get(), ScaledFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.FIREBALL_TALISMAN_PROJECTILE.get(),
                FireballTalismanProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.TALISMAN_ACTIVATION.get(),
                TalismanActivationRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_FLAME_DOMAIN.get(),
                BlackFlameDomainRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRITUAL_LIGHT.get(),
                SpiritualLightRenderer::new);
        event.registerEntityRenderer(ModEntities.TALISMAN_WARD.get(),
                TalismanWardRenderer::new);
        event.registerEntityRenderer(ModEntities.COSMIC_VOID_PORTAL.get(),
                CosmicVoidPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.TALISMAN_MESSAGE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_FLAME_FIREBALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_DRAGON_SCALE_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRIT_SILK_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.IGNITE_FLARE_PROJECTILE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHT_BEAM_PROJECTILE.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.LINGZHI_BULLET_PROJECTILE.get(), context -> new ThrownItemRenderer<>(context, 0.32F, true));
        event.registerEntityRenderer(ModEntities.JINMANG_CHUANSHA_PROJECTILE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ELEMENTAL_TECHNIQUE_PROJECTILE.get(),
                ElementalTechniqueProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_TRIGGER_PROJECTILE.get(),
                SpellTriggerProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SOLAR_TECHNIQUE_MODEL.get(),
                SolarTechniqueModelRenderer::new);
        event.registerEntityRenderer(ModEntities.ELEMENTAL_TECHNIQUE_AREA.get(),
                ElementalTechniqueAreaRenderer::new);
        event.registerEntityRenderer(ModEntities.METHOD_TECHNIQUE_AREA.get(),
                MethodTechniqueAreaRenderer::new);
        event.registerEntityRenderer(ModEntities.TEMPORARY_TERRAIN.get(),
                EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.XUANYAN_SHIELD_PLATE.get(),
                EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.YAOGUANG_FLASH.get(), YaoguangFlashRenderer::new);
        event.registerEntityRenderer(ModEntities.WANTENG_VINE.get(), WantengVineRenderer::new);
        event.registerEntityRenderer(ModEntities.SHANBENG_SEAL.get(), ShanbengSealRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_BLADE_PROJECTILE.get(), context -> new ThrownItemRenderer<>(context, 0.32F, true));
        event.registerEntityRenderer(ModEntities.SMOKE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SLIDING_WATER_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ABSORB_CULTIVATION_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DINGSHEN_PROJECTILE.get(), context -> new ThrownItemRenderer<>(context, 0.32F, true));
        event.registerEntityRenderer(ModEntities.KONGSHI_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_PROJECTILE.get(), context -> new ThrownItemRenderer<>(context, 0.32F, true));
        event.registerEntityRenderer(ModEntities.FROST_CRY_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIUGUANG_JIANYING_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DUANLIU_KONGDUN_PROJECTILE.get(), context -> new ThrownItemRenderer<>(context, 0.32F, true));
        event.registerEntityRenderer(ModEntities.YIHEN_CI_PROJECTILE.get(), context -> new ThrownItemRenderer<>(context, 0.32F, true));
        event.registerEntityRenderer(ModEntities.SUOKONG_PROJECTILE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.XINLIAO_FIREBALL.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.STARFALL_PROJECTILE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.TIANYUN_FENCHENG_METEOR.get(),
                TianyunFenchengMeteorRenderer::new);
        event.registerEntityRenderer(ModEntities.PETAL_TECHNIQUE.get(),
                PetalTechniqueRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_TECHNIQUE_PROJECTILE.get(),
                SwordTechniqueProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_TECHNIQUE_MARK.get(),
                SwordTechniqueMarkRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWING_KNIFE.get(), ThrowingKnifeRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_NEEDLE.get(), FlyingNeedleRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_FX_ANCHOR.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.DUANLIU_KONGDUN_DOME.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHENSHAN_PALM.get(), ZhenshanPalmRenderer::new);
        event.registerEntityRenderer(ModEntities.XUYING_SHADOW.get(), XuyingShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL.get(), SoulRenderer::new);
        event.registerEntityRenderer(ModEntities.SHIHUNFAN.get(), ShihunfanRenderer::new);
        event.registerEntityRenderer(ModEntities.SHIHUNFAN_SOUL_WAVE.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SHIHUNFAN_GHOST.get(), ShihunfanGhostRenderer::new);
        event.registerEntityRenderer(ModEntities.FUCHEN_BINDING_PLUME.get(), FuchenBindingPlumeRenderer::new);
        event.registerEntityRenderer(ModEntities.WEAPON_QI_SLASH.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LIANQI_CULTIVATOR.get(), LianqiCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHUJI_CULTIVATOR.get(), ZhujiCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_CULTIVATOR.get(), JindanCultivatorRenderer::new);
        event.registerEntityRenderer(ModEntities.FANREN_NPC.get(), FanrenNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.HOTEL_NPC.get(), HotelNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(),
                CrimsonScaleFlameLizardRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(),
                CrimsonSkyFlameFloodDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_APE_DEMON_GENERAL.get(),
                BloodApeDemonGeneralRenderer::new);
        com.example.immortal_cultivation_mod.pagoda.PagodaClientAccess.tick = com.example.immortal_cultivation_mod.client.PagodaVisuals::tick;
        event.registerEntityRenderer(ModEntities.PAGODA.get(), com.example.immortal_cultivation_mod.client.renderer.PagodaRenderer::new);
        event.registerEntityRenderer(ModEntities.XUANJIN_ZHENG.get(),
                com.example.immortal_cultivation_mod.client.renderer.XuanjinZhengRenderer::new);
        event.registerEntityRenderer(ModEntities.ESCAPED_YUANYING_CORE.get(),
                EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AZURE_SPIRIT_SILKWORM.get(),
                AzureSpiritSilkwormRenderer::new);
        event.registerEntityRenderer(ModEntities.KONGMING_BEETLE.get(),
                KongmingBeetleRenderer::new);
        event.registerEntityRenderer(ModEntities.THUNDERHORN_ROCK_BADGER.get(),
                ThunderhornRockBadgerRenderer::new);
        event.registerEntityRenderer(ModEntities.FROSTSPINE_CAVE_BEAR.get(),
                FrostspineCaveBearRenderer::new);
        event.registerEntityRenderer(ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(),
                CultivationBestiaryBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get(),
                CultivationBestiaryBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_FEATHER_CLIFF_DEER.get(),
                CultivationBestiaryBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get(),
                CultivationBestiaryBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.EMBER_FEATHER_FIRE_OWL.get(),
                CultivationBestiaryBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATION_BEAST_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATION_BEAST_FIRE_PATCH.get(),
                EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHUJI_PHANTOM_VEIL_FOX.get(),
                context -> new PhantomVeilFoxRenderer(context, 0.78F));
        event.registerEntityRenderer(ModEntities.JINDAN_PHANTOM_VEIL_FOX.get(),
                context -> new PhantomVeilFoxRenderer(context, 0.90F));
        event.registerEntityRenderer(ModEntities.PHANTOM_VEIL_FOX.get(),
                PhantomVeilFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_VEIL_FOX_CLONE.get(),
                PhantomVeilFoxCloneRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_FOXFIRE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.KONGMING_CRYSTAL_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THUNDER_ROCK_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.VAULT_SPIRIT_SPIKE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ZHUJI_VAULT_GUARDIAN.get(),
                VaultGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_VAULT_GUARDIAN.get(),
                VaultGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATOR_CORPSE.get(), CultivatorCorpseRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN_CLONE.get(), JindanCloneRenderer::new);
        event.registerEntityRenderer(ModEntities.JINDAN.get(), JindanEntityRenderer::new);
        event.registerEntityRenderer(
                ModEntities.SYSTEM_MANIFEST.get(),
                SystemManifestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GEO_ROCK.get(), GeoRockBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DING.get(), GeoRockBlockRenderer::new);

        event.registerBlockEntityRenderer(
                ModBlockEntities.RESTRICTION_TABLE.get(),
                RestrictionTableRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.TALISMAN_DRAWING_TABLE.get(),
                TalismanDrawingTableRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.MEDICINE_CABINET.get(),
                MedicineCabinetRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.DEAD_SKELETON.get(),
                DeadSkeletonBlockRenderer::new);
        event.registerBlockEntityRenderer(
                com.example.immortal_cultivation_mod.magicritual.block.custom.ModBlockEntities.MAGIC_CENTER_BE.get(),
                MagicRitualGlyphRenderer::new);
        event.registerBlockEntityRenderer(
                com.example.immortal_cultivation_mod.magicritual.block.custom.ModBlockEntities.MAGIC_CIRCLE_BE.get(),
                MagicRitualGlyphRenderer::new);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_CRIMSON.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.CRIMSON));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_EARTH.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.EARTH));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_WOOD.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.WOOD));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_SOLAR.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.SOLAR));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_NETHER.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.NETHER));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_THUNDER.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.THUNDER));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_DRAGON.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.DRAGON));
        event.registerSpriteSet(ModParticles.VESSEL_FLAME_HEAVENLY.get(),
                sprites -> new VesselFlameParticle.Provider(sprites, FlameKind.HEAVENLY));
        com.example.immortal_cultivation_mod.client.particle.EnvironmentParticle.register(event);
        com.example.immortal_cultivation_mod.client.particle.DingGoldenSteamParticle.register(event);
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
        event.registerSpriteSet(
                ModParticles.SOLAR_EMBER.get(), SolarVfxParticles.SolarEmberProvider::new);
        event.registerSpriteSet(
                ModParticles.SOLAR_CORE.get(), SolarVfxParticles.SolarCoreProvider::new);
        event.registerSpriteSet(
                ModParticles.SOLAR_SIGIL.get(), SolarVfxParticles.SolarSigilProvider::new);
        event.registerSpriteSet(
                ModParticles.BLACK_FLAME_CORE.get(), BlackFlameVfxParticles.CoreProvider::new);
        event.registerSpriteSet(
                ModParticles.BLACK_FLAME_EDGE.get(), BlackFlameVfxParticles.EdgeProvider::new);
        event.registerSpriteSet(
                ModParticles.BLACK_FLAME_ASH.get(), BlackFlameVfxParticles.AshProvider::new);
        event.registerSpriteSet(
                ModParticles.BLACK_FLAME_SPARK.get(), BlackFlameVfxParticles.SparkProvider::new);
        event.registerSpriteSet(
                ModParticles.JINDAN_SONIC_RING.get(),
                JindanShockwaveParticles.SonicRingProvider::new);
        event.registerSpriteSet(
                ModParticles.JINDAN_SMASH_RING.get(),
                JindanShockwaveParticles.SmashRingProvider::new);
    }

    private static void registerMethodQiParticle(
            RegisterParticleProvidersEvent event,
            net.minecraft.core.particles.SimpleParticleType type,
            int color) {
        event.registerSpriteSet(type, sprites -> new MethodQiParticle.Provider(sprites, color));
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.DEBUG_WEAPON_EDITOR.get(),
                com.example.immortal_cultivation_mod.screen.DebugWeaponEditorScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.QI_POUCH.get(),
                com.example.immortal_cultivation_mod.screen.QiPouchScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.DING.get(),
                com.example.immortal_cultivation_mod.screen.DingScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.RESTRICTION_TABLE.get(),
                com.example.immortal_cultivation_mod.screen.RestrictionTableScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.TALISMAN_DRAWING.get(),
                com.example.immortal_cultivation_mod.screen.TalismanDrawingScreen::new);
        event.register(com.example.immortal_cultivation_mod.screen.ModScreens.MEDICINE_DRAWER.get(),
                com.example.immortal_cultivation_mod.screen.MedicineDrawerScreen::new);

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
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ZHEN_FIRE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.STAR_MARK.get(), RenderType.cutout());
            CuriosRendererRegistry.register(ModItems.COFFIN.get(), CoffinCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.FLAG.get(), FlagCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.CLOTH_SHEATH.get(), ClothSheathCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.SWORD_SHEATH.get(), RigidSheathCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DAO_SHEATH.get(), RigidSheathCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.SWORD_CASE.get(), SwordCaseCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.XIUJIAN.get(), XiujianCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.COIN_MASK.get(), CoinMaskCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLINDFOLD.get(), BlindfoldCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLINDFOLD2.get(), Blindfold2CurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.STRAW_HAT.get(), StrawHatCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.NOOB_HAT.get(), NoobHatCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.JADE_PENDANT.get(), JadePendantCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DAOFU_CHESTPLATE.get(), DaofuCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ZHONGYI.get(), ZhongyiCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DOUPENG.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("doupeng"));
            CuriosRendererRegistry.register(ModItems.SHENYI.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("shenyi"));
            CuriosRendererRegistry.register(ModItems.HECHANG.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("hechang"));
            CuriosRendererRegistry.register(ModItems.YUNJIAN.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("yunjian"));
            CuriosRendererRegistry.register(ModItems.CULTIVATION_FAPAO.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("cultivation_fapao"));
            CuriosRendererRegistry.register(ModItems.YUANLING_PAO.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("yuanling_pao"));
            CuriosRendererRegistry.register(ModItems.HUFU.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("hufu"));
            CuriosRendererRegistry.register(ModItems.JIANGPA.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("jiangpa"));
            CuriosRendererRegistry.register(ModItems.FUTOU.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("futou"));
            CuriosRendererRegistry.register(ModItems.WUSHA_HAT.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("wusha_hat"));
            CuriosRendererRegistry.register(ModItems.SONG_WUSHA_HAT.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("song_wusha_hat"));
            CuriosRendererRegistry.register(ModItems.CHANFU_JIN.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("chanfu_jin"));
            CuriosRendererRegistry.register(ModItems.EMBROIDERED_FOREHEAD_BAND.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("embroidered_forehead_band"));
            CuriosRendererRegistry.register(ModItems.VEILED_DOULI.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("veiled_douli"));
            CuriosRendererRegistry.register(ModItems.FANGKOU_QIAOJIAN_LV.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("fangkou_qiaojian_lv"));
            CuriosRendererRegistry.register(ModItems.MIANSHA.get(),
                    () -> new com.example.immortal_cultivation_mod.client.renderer.CultivationGarmentRenderer("miansha"));
            CuriosRendererRegistry.register(ModItems.DISGUISE_MASK.get(), DisguiseMaskCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.PROTECTIVE_JADE_PENDANT.get(),
                    ProtectiveJadePendantCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get(),
                    BlackTortoiseHeartguardMirrorCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.FORTUNE_GUARD_COIN.get(),
                    com.example.immortal_cultivation_mod.client.renderer.FortuneCharmCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.TRIBULATION_DOLL.get(),
                    com.example.immortal_cultivation_mod.client.renderer.FortuneCharmCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.PURIFIED_TRIBULATION_DOLL.get(),
                    com.example.immortal_cultivation_mod.client.renderer.FortuneCharmCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.WATER_AVOIDING_PEARL.get(),
                    com.example.immortal_cultivation_mod.client.renderer.FortuneCharmCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.SOUL_ANCHOR_PEARL.get(),
                    com.example.immortal_cultivation_mod.client.renderer.FortuneCharmCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ENLIGHTENMENT_LEAF.get(),
                    com.example.immortal_cultivation_mod.client.renderer.FortuneCharmCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.JIN_HULU.get(), com.example.immortal_cultivation_mod.client.renderer.GoldenGourdCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ZHENHUN_BELL.get(), ZhenhunBellCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.YINHUN_GONG.get(), YinhunGongCurioRenderer::new);
        });
    }
}
