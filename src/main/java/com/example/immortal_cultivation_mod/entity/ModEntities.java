package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<com.example.immortal_cultivation_mod.flame.SpiritFlameEntity>> SPIRIT_FLAME =
            ENTITY_TYPES.register("spirit_flame", () -> EntityType.Builder.<com.example.immortal_cultivation_mod.flame.SpiritFlameEntity>of(
                    com.example.immortal_cultivation_mod.flame.SpiritFlameEntity::new, MobCategory.MISC)
                    .sized(.75F, 1.3F).clientTrackingRange(10).updateInterval(20).fireImmune().build("spirit_flame"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.example.immortal_cultivation_mod.flame.FlameShotEntity>> FLAME_SHOT =
            ENTITY_TYPES.register("flame_shot", () -> EntityType.Builder.<com.example.immortal_cultivation_mod.flame.FlameShotEntity>of(
                    com.example.immortal_cultivation_mod.flame.FlameShotEntity::new, MobCategory.MISC)
                    .sized(.3F, .3F).clientTrackingRange(8).updateInterval(1).fireImmune().build("flame_shot"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.example.immortal_cultivation_mod.pagoda.PagodaEntity>> PAGODA =
            ENTITY_TYPES.register("pagoda", () -> EntityType.Builder.<com.example.immortal_cultivation_mod.pagoda.PagodaEntity>of(
                    com.example.immortal_cultivation_mod.pagoda.PagodaEntity::new, MobCategory.MISC)
                    .sized(6,14).clientTrackingRange(32).updateInterval(1).build("pagoda"));

    public static final DeferredHolder<EntityType<?>, EntityType<XuanjinZhengEntity>> XUANJIN_ZHENG =
            ENTITY_TYPES.register("xuanjin_zheng", () -> EntityType.Builder.<XuanjinZhengEntity>of(
                    XuanjinZhengEntity::new, MobCategory.CREATURE).sized(2.2F, 3.2F)
                    .clientTrackingRange(16).updateInterval(1).build("xuanjin_zheng"));

    public static final DeferredHolder<EntityType<?>, EntityType<BloodDropEntity>> BLOOD_DROP =
            ENTITY_TYPES.register("blood_drop",
                    () -> EntityType.Builder.<BloodDropEntity>of(
                                    BloodDropEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("blood_drop"));

    public static final DeferredHolder<EntityType<?>, EntityType<FireballProjectileEntity>> FIREBALL_PROJECTILE =
            ENTITY_TYPES.register("fireball_projectile",
                    () -> EntityType.Builder.<FireballProjectileEntity>of(
                            FireballProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build("fireball_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<FireballTalismanProjectileEntity>> FIREBALL_TALISMAN_PROJECTILE =
            ENTITY_TYPES.register("fireball_talisman_projectile",
                    () -> EntityType.Builder.<FireballTalismanProjectileEntity>of(
                            FireballTalismanProjectileEntity::new, MobCategory.MISC)
                            .sized(0.45f, 0.45f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("fireball_talisman_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<TalismanActivationEntity>> TALISMAN_ACTIVATION =
            ENTITY_TYPES.register("talisman_activation",
                    () -> EntityType.Builder.<TalismanActivationEntity>of(
                                    TalismanActivationEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("talisman_activation"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackFlameDomainEntity>> BLACK_FLAME_DOMAIN =
            ENTITY_TYPES.register("black_flame_domain",
                    () -> EntityType.Builder.<BlackFlameDomainEntity>of(
                                    BlackFlameDomainEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(32)
                            .updateInterval(5)
                            .fireImmune()
                            .build("black_flame_domain"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritualLightEntity>> SPIRITUAL_LIGHT =
            ENTITY_TYPES.register("spiritual_light",
                    () -> EntityType.Builder.<SpiritualLightEntity>of(
                                    SpiritualLightEntity::new, MobCategory.MISC)
                            .sized(0.18F, 0.18F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("spiritual_light"));

    public static final DeferredHolder<EntityType<?>, EntityType<TalismanWardEntity>> TALISMAN_WARD =
            ENTITY_TYPES.register("talisman_ward",
                    () -> EntityType.Builder.<TalismanWardEntity>of(
                                    TalismanWardEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(20)
                            .updateInterval(1)
                            .build("talisman_ward"));

    public static final DeferredHolder<EntityType<?>, EntityType<CosmicVoidPortalEntity>> COSMIC_VOID_PORTAL =
            ENTITY_TYPES.register("cosmic_void_portal",
                    () -> EntityType.Builder.<CosmicVoidPortalEntity>of(
                                    CosmicVoidPortalEntity::new, MobCategory.MISC)
                            .sized(2.2F, 4.5F)
                            .clientTrackingRange(24)
                            .updateInterval(1)
                            .fireImmune()
                            .noSave()
                            .build("cosmic_void_portal"));

    public static final DeferredHolder<EntityType<?>, EntityType<TalismanMessageEntity>> TALISMAN_MESSAGE =
            ENTITY_TYPES.register("talisman_message",
                    () -> EntityType.Builder.<TalismanMessageEntity>of(
                                    TalismanMessageEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(24)
                            .updateInterval(1)
                            .build("talisman_message"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonFlameFireballEntity>> CRIMSON_FLAME_FIREBALL =
            ENTITY_TYPES.register("crimson_flame_fireball",
                    () -> EntityType.Builder.<CrimsonFlameFireballEntity>of(
                            CrimsonFlameFireballEntity::new, MobCategory.MISC)
                            .sized(0.45f, 0.45f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("crimson_flame_fireball"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonDragonScaleProjectileEntity>>
            CRIMSON_DRAGON_SCALE_PROJECTILE =
            ENTITY_TYPES.register("crimson_dragon_scale_projectile",
                    () -> EntityType.Builder.<CrimsonDragonScaleProjectileEntity>of(
                                    CrimsonDragonScaleProjectileEntity::new, MobCategory.MISC)
                            .sized(0.34F, 0.34F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("crimson_dragon_scale_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<VaultSpiritSpikeEntity>>
            VAULT_SPIRIT_SPIKE =
            ENTITY_TYPES.register("vault_spirit_spike",
                    () -> EntityType.Builder.<VaultSpiritSpikeEntity>of(
                                    VaultSpiritSpikeEntity::new, MobCategory.MISC)
                            .sized(0.22F, 0.22F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("vault_spirit_spike"));

    public static final DeferredHolder<EntityType<?>, EntityType<KongmingCrystalProjectileEntity>>
            KONGMING_CRYSTAL_PROJECTILE =
            ENTITY_TYPES.register("kongming_crystal_projectile",
                    () -> EntityType.Builder.<KongmingCrystalProjectileEntity>of(
                                    KongmingCrystalProjectileEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("kongming_crystal_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThunderRockProjectileEntity>>
            THUNDER_ROCK_PROJECTILE =
            ENTITY_TYPES.register("thunder_rock_projectile",
                    () -> EntityType.Builder.<ThunderRockProjectileEntity>of(
                                    ThunderRockProjectileEntity::new, MobCategory.MISC)
                            .sized(0.34F, 0.34F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("thunder_rock_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritSilkProjectileEntity>> SPIRIT_SILK_PROJECTILE =
            ENTITY_TYPES.register("spirit_silk_projectile",
                    () -> EntityType.Builder.<SpiritSilkProjectileEntity>of(
                            SpiritSilkProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("spirit_silk_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<IgniteFlareProjectileEntity>> IGNITE_FLARE_PROJECTILE =
            ENTITY_TYPES.register("ignite_flare_projectile",
                    () -> EntityType.Builder.<IgniteFlareProjectileEntity>of(
                            IgniteFlareProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("ignite_flare_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LightBeamProjectileEntity>> LIGHT_BEAM_PROJECTILE =
            ENTITY_TYPES.register("light_beam_projectile",
                    () -> EntityType.Builder.<LightBeamProjectileEntity>of(
                            LightBeamProjectileEntity::new, MobCategory.MISC)
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("light_beam_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LingzhiBulletProjectileEntity>> LINGZHI_BULLET_PROJECTILE =
            ENTITY_TYPES.register("lingzhi_bullet_projectile",
                    () -> EntityType.Builder.<LingzhiBulletProjectileEntity>of(
                            LingzhiBulletProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("lingzhi_bullet_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<JinmangChuanshaProjectileEntity>>
            JINMANG_CHUANSHA_PROJECTILE =
            ENTITY_TYPES.register("jinmang_chuansha_projectile",
                    () -> EntityType.Builder.<JinmangChuanshaProjectileEntity>of(
                                    JinmangChuanshaProjectileEntity::new, MobCategory.MISC)
                            .sized(0.12F, 0.12F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("jinmang_chuansha_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElementalTechniqueProjectileEntity>>
            ELEMENTAL_TECHNIQUE_PROJECTILE =
            ENTITY_TYPES.register("elemental_technique_projectile",
                    () -> EntityType.Builder.<ElementalTechniqueProjectileEntity>of(
                                    ElementalTechniqueProjectileEntity::new, MobCategory.MISC)
                            .sized(0.16F, 0.16F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("elemental_technique_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpellTriggerProjectileEntity>>
            SPELL_TRIGGER_PROJECTILE =
            ENTITY_TYPES.register("spell_trigger_projectile",
                    () -> EntityType.Builder.<SpellTriggerProjectileEntity>of(
                                    SpellTriggerProjectileEntity::new, MobCategory.MISC)
                            .sized(0.16F, 0.16F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("spell_trigger_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SolarTechniqueModelEntity>>
            SOLAR_TECHNIQUE_MODEL =
            ENTITY_TYPES.register("solar_technique_model",
                    () -> EntityType.Builder.<SolarTechniqueModelEntity>of(
                                    SolarTechniqueModelEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("solar_technique_model"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElementalTechniqueAreaEntity>>
            ELEMENTAL_TECHNIQUE_AREA =
            ENTITY_TYPES.register("elemental_technique_area",
                    () -> EntityType.Builder.<ElementalTechniqueAreaEntity>of(
                                    ElementalTechniqueAreaEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("elemental_technique_area"));

    public static final DeferredHolder<EntityType<?>, EntityType<MethodTechniqueAreaEntity>>
            METHOD_TECHNIQUE_AREA =
            ENTITY_TYPES.register("method_technique_area",
                    () -> EntityType.Builder.<MethodTechniqueAreaEntity>of(
                                    MethodTechniqueAreaEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("method_technique_area"));

    public static final DeferredHolder<EntityType<?>, EntityType<TemporaryTerrainEntity>>
            TEMPORARY_TERRAIN =
            ENTITY_TYPES.register("temporary_terrain",
                    () -> EntityType.Builder.<TemporaryTerrainEntity>of(
                                    TemporaryTerrainEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("temporary_terrain"));

    public static final DeferredHolder<EntityType<?>, EntityType<XuanyanShieldPlateEntity>>
            XUANYAN_SHIELD_PLATE =
            ENTITY_TYPES.register("xuanyan_shield_plate",
                    () -> EntityType.Builder.<XuanyanShieldPlateEntity>of(
                                    XuanyanShieldPlateEntity::new, MobCategory.MISC)
                            .sized(0.8F, 1.4F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("xuanyan_shield_plate"));

    public static final DeferredHolder<EntityType<?>, EntityType<YaoguangFlashEntity>>
            YAOGUANG_FLASH =
            ENTITY_TYPES.register("yaoguang_flash",
                    () -> EntityType.Builder.<YaoguangFlashEntity>of(
                                    YaoguangFlashEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("yaoguang_flash"));

    public static final DeferredHolder<EntityType<?>, EntityType<WindBladeProjectileEntity>> WIND_BLADE_PROJECTILE =
            ENTITY_TYPES.register("wind_blade_projectile",
                    () -> EntityType.Builder.<WindBladeProjectileEntity>of(
                            WindBladeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.45f, 0.45f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("wind_blade_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeProjectileEntity>> SMOKE_PROJECTILE =
            ENTITY_TYPES.register("smoke_projectile",
                    () -> EntityType.Builder.<SmokeProjectileEntity>of(
                            SmokeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.45f, 0.45f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("smoke_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SlidingWaterProjectileEntity>> SLIDING_WATER_PROJECTILE =
            ENTITY_TYPES.register("sliding_water_projectile",
                    () -> EntityType.Builder.<SlidingWaterProjectileEntity>of(
                            SlidingWaterProjectileEntity::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("sliding_water_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<AbsorbCultivationProjectileEntity>> ABSORB_CULTIVATION_PROJECTILE =
            ENTITY_TYPES.register("absorb_cultivation_projectile",
                    () -> EntityType.Builder.<AbsorbCultivationProjectileEntity>of(
                            AbsorbCultivationProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("absorb_cultivation_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<DingshenProjectileEntity>> DINGSHEN_PROJECTILE =
            ENTITY_TYPES.register("dingshen_projectile",
                    () -> EntityType.Builder.<DingshenProjectileEntity>of(
                            DingshenProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("dingshen_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<KongshiProjectileEntity>> KONGSHI_PROJECTILE =
            ENTITY_TYPES.register("kongshi_projectile",
                    () -> EntityType.Builder.<KongshiProjectileEntity>of(
                            KongshiProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("kongshi_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LightningProjectileEntity>> LIGHTNING_PROJECTILE =
            ENTITY_TYPES.register("lightning_projectile",
                    () -> EntityType.Builder.<LightningProjectileEntity>of(
                            LightningProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("lightning_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostCryProjectileEntity>> FROST_CRY_PROJECTILE =
            ENTITY_TYPES.register("frost_cry_projectile",
                    () -> EntityType.Builder.<FrostCryProjectileEntity>of(
                            FrostCryProjectileEntity::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("frost_cry_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LiuguangJianyingProjectileEntity>> LIUGUANG_JIANYING_PROJECTILE =
            ENTITY_TYPES.register("liuguang_jianying_projectile",
                    () -> EntityType.Builder.<LiuguangJianyingProjectileEntity>of(
                            LiuguangJianyingProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("liuguang_jianying_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<DuanliuKongdunProjectileEntity>> DUANLIU_KONGDUN_PROJECTILE =
            ENTITY_TYPES.register("duanliu_kongdun_projectile",
                    () -> EntityType.Builder.<DuanliuKongdunProjectileEntity>of(
                            DuanliuKongdunProjectileEntity::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("duanliu_kongdun_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<YihenCiProjectileEntity>> YIHEN_CI_PROJECTILE =
            ENTITY_TYPES.register("yihen_ci_projectile",
                    () -> EntityType.Builder.<YihenCiProjectileEntity>of(
                            YihenCiProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("yihen_ci_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SuokongProjectileEntity>> SUOKONG_PROJECTILE =
            ENTITY_TYPES.register("suokong_projectile",
                    () -> EntityType.Builder.<SuokongProjectileEntity>of(
                            SuokongProjectileEntity::new, MobCategory.MISC)
                            .sized(0.2f, 0.2f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("suokong_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<XinliaoFireballEntity>> XINLIAO_FIREBALL =
            ENTITY_TYPES.register("xinliao_fireball",
                    () -> EntityType.Builder.<XinliaoFireballEntity>of(
                            XinliaoFireballEntity::new, MobCategory.MISC)
                            .sized(0.7f, 0.7f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("xinliao_fireball"));

    public static final DeferredHolder<EntityType<?>, EntityType<StarfallProjectileEntity>> STARFALL_PROJECTILE =
            ENTITY_TYPES.register("starfall_projectile",
                    () -> EntityType.Builder.<StarfallProjectileEntity>of(
                            StarfallProjectileEntity::new, MobCategory.MISC)
                            .sized(0.7f, 0.7f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("starfall_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<TianyunFenchengMeteorEntity>>
            TIANYUN_FENCHENG_METEOR =
            ENTITY_TYPES.register("tianyun_fencheng_meteor",
                    () -> EntityType.Builder.<TianyunFenchengMeteorEntity>of(
                            TianyunFenchengMeteorEntity::new, MobCategory.MISC)
                            .sized(7.2F, 7.2F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("tianyun_fencheng_meteor"));

    public static final DeferredHolder<EntityType<?>, EntityType<PetalTechniqueEntity>>
            PETAL_TECHNIQUE =
            ENTITY_TYPES.register("petal_technique",
                    () -> EntityType.Builder.<PetalTechniqueEntity>of(
                            PetalTechniqueEntity::new, MobCategory.MISC)
                            .sized(0.55F, 0.55F)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("petal_technique"));

    public static final DeferredHolder<EntityType<?>, EntityType<SwordTechniqueProjectileEntity>>
            SWORD_TECHNIQUE_PROJECTILE =
            ENTITY_TYPES.register("sword_technique_projectile",
                    () -> EntityType.Builder.<SwordTechniqueProjectileEntity>of(
                            SwordTechniqueProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(24)
                            .updateInterval(1)
                            .build("sword_technique_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SwordTechniqueMarkEntity>>
            SWORD_TECHNIQUE_MARK =
            ENTITY_TYPES.register("sword_technique_mark",
                    () -> EntityType.Builder.<SwordTechniqueMarkEntity>of(
                            SwordTechniqueMarkEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(24)
                            .updateInterval(1)
                            .build("sword_technique_mark"));

    public static final DeferredHolder<EntityType<?>, EntityType<DuanliuKongdunDomeEntity>> DUANLIU_KONGDUN_DOME =
            ENTITY_TYPES.register("duanliu_kongdun_dome",
                    () -> EntityType.Builder.<DuanliuKongdunDomeEntity>of(
                            DuanliuKongdunDomeEntity::new, MobCategory.MISC)
                            .sized(20.0f, 10.0f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("duanliu_kongdun_dome"));

    public static final DeferredHolder<EntityType<?>, EntityType<IceFxAnchorEntity>> ICE_FX_ANCHOR =
            ENTITY_TYPES.register("ice_fx_anchor",
                    () -> EntityType.Builder.<IceFxAnchorEntity>of(
                            IceFxAnchorEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("ice_fx_anchor"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingSwordEntity>> FLYING_SWORD =
            ENTITY_TYPES.register("flying_sword",
                    () -> EntityType.Builder.<FlyingSwordEntity>of(
                            FlyingSwordEntity::new, MobCategory.MISC)
                            .sized(3.0f, 0.12f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("flying_sword"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrowingKnifeEntity>> THROWING_KNIFE =
            ENTITY_TYPES.register("throwing_knife",
                    () -> EntityType.Builder.<ThrowingKnifeEntity>of(
                            ThrowingKnifeEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.75f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("throwing_knife"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingNeedleEntity>> FLYING_NEEDLE =
            ENTITY_TYPES.register("flying_needle",
                    () -> EntityType.Builder.<FlyingNeedleEntity>of(
                            FlyingNeedleEntity::new, MobCategory.MISC)
                            .sized(0.12f, 0.45f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("flying_needle"));

    public static final DeferredHolder<EntityType<?>, EntityType<ZhenshanPalmEntity>> ZHENSHAN_PALM =
            ENTITY_TYPES.register("zhenshan_palm",
                    () -> EntityType.Builder.<ZhenshanPalmEntity>of(
                            ZhenshanPalmEntity::new, MobCategory.MISC)
                            .sized(2.5f, 2.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("zhenshan_palm"));

    public static final DeferredHolder<EntityType<?>, EntityType<LianqiCultivatorEntity>> LIANQI_CULTIVATOR =
            ENTITY_TYPES.register("lianqi_cultivator",
                    () -> EntityType.Builder.<LianqiCultivatorEntity>of(
                            LianqiCultivatorEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("lianqi_cultivator"));

    public static final DeferredHolder<EntityType<?>, EntityType<ZhujiCultivatorEntity>> ZHUJI_CULTIVATOR =
            ENTITY_TYPES.register("zhuji_cultivator",
                    () -> EntityType.Builder.<ZhujiCultivatorEntity>of(
                            ZhujiCultivatorEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("zhuji_cultivator"));

    public static final DeferredHolder<EntityType<?>, EntityType<JindanCultivatorEntity>> JINDAN_CULTIVATOR =
            ENTITY_TYPES.register("jindan_cultivator",
                    () -> EntityType.Builder.<JindanCultivatorEntity>of(
                            JindanCultivatorEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("jindan_cultivator"));

    public static final DeferredHolder<EntityType<?>, EntityType<FanrenNpcEntity>> FANREN_NPC =
            ENTITY_TYPES.register("fanren_npc",
                    () -> EntityType.Builder.<FanrenNpcEntity>of(
                            FanrenNpcEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("fanren_npc"));

    public static final DeferredHolder<EntityType<?>, EntityType<HotelNpcEntity>> HOTEL_NPC =
            ENTITY_TYPES.register("hotel_npc",
                    () -> EntityType.Builder.<HotelNpcEntity>of(
                            HotelNpcEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("hotel_npc"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonScaleFlameLizardEntity>> CRIMSON_SCALE_FLAME_LIZARD =
            ENTITY_TYPES.register("crimson_scale_flame_lizard",
                    () -> EntityType.Builder.<CrimsonScaleFlameLizardEntity>of(
                            CrimsonScaleFlameLizardEntity::new, MobCategory.MONSTER)
                            .sized(1.65f, 1.45f)
                            .fireImmune()
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("crimson_scale_flame_lizard"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonSkyFlameFloodDragonEntity>>
            CRIMSON_SKY_FLAME_FLOOD_DRAGON =
            ENTITY_TYPES.register("crimson_sky_flame_flood_dragon",
                    () -> EntityType.Builder.<CrimsonSkyFlameFloodDragonEntity>of(
                                    CrimsonSkyFlameFloodDragonEntity::new, MobCategory.MONSTER)
                            .sized(3.2F, 2.6F)
                            .fireImmune()
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("crimson_sky_flame_flood_dragon"));

    public static final DeferredHolder<EntityType<?>, EntityType<BloodApeDemonGeneralEntity>>
            BLOOD_APE_DEMON_GENERAL =
            ENTITY_TYPES.register("blood_ape_demon_general",
                    () -> EntityType.Builder.<BloodApeDemonGeneralEntity>of(
                                    BloodApeDemonGeneralEntity::new, MobCategory.MONSTER)
                            .sized(1.15F, 2.8F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("blood_ape_demon_general"));

    public static final DeferredHolder<EntityType<?>, EntityType<VaultGuardianEntity>>
            ZHUJI_VAULT_GUARDIAN =
            ENTITY_TYPES.register("zhuji_vault_guardian",
                    () -> EntityType.Builder.<VaultGuardianEntity>of(
                                    (type, level) -> new VaultGuardianEntity(type, level, false),
                                    MobCategory.MONSTER)
                            .sized(3.9F, 7.8F)
                            .fireImmune()
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("zhuji_vault_guardian"));

    public static final DeferredHolder<EntityType<?>, EntityType<VaultGuardianEntity>>
            JINDAN_VAULT_GUARDIAN =
            ENTITY_TYPES.register("jindan_vault_guardian",
                    () -> EntityType.Builder.<VaultGuardianEntity>of(
                                    (type, level) -> new VaultGuardianEntity(type, level, true),
                                    MobCategory.MONSTER)
                            .sized(3.9F, 7.8F)
                            .fireImmune()
                            .clientTrackingRange(18)
                            .updateInterval(1)
                            .build("jindan_vault_guardian"));

    public static final DeferredHolder<EntityType<?>, EntityType<EscapedYuanyingCoreEntity>>
            ESCAPED_YUANYING_CORE =
            ENTITY_TYPES.register("escaped_yuanying_core",
                    () -> EntityType.Builder.<EscapedYuanyingCoreEntity>of(
                                    EscapedYuanyingCoreEntity::new, MobCategory.MISC)
                            .sized(1.4F, 1.4F)
                            .fireImmune()
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("escaped_yuanying_core"));

    public static final DeferredHolder<EntityType<?>, EntityType<AzureSpiritSilkwormEntity>> AZURE_SPIRIT_SILKWORM =
            ENTITY_TYPES.register("azure_spirit_silkworm",
                    () -> EntityType.Builder.<AzureSpiritSilkwormEntity>of(
                            AzureSpiritSilkwormEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 0.45f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("azure_spirit_silkworm"));

    public static final DeferredHolder<EntityType<?>, EntityType<KongmingBeetleEntity>> KONGMING_BEETLE =
            ENTITY_TYPES.register("kongming_beetle",
                    () -> EntityType.Builder.<KongmingBeetleEntity>of(
                                    KongmingBeetleEntity::new, MobCategory.MONSTER)
                            .sized(1.75F, 1.1F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("kongming_beetle"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThunderhornRockBadgerEntity>>
            THUNDERHORN_ROCK_BADGER =
            ENTITY_TYPES.register("thunderhorn_rock_badger",
                    () -> EntityType.Builder.<ThunderhornRockBadgerEntity>of(
                                    ThunderhornRockBadgerEntity::new, MobCategory.MONSTER)
                            .sized(2.35F, 1.68F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("thunderhorn_rock_badger"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostspineCaveBearEntity>>
            FROSTSPINE_CAVE_BEAR =
            ENTITY_TYPES.register("frostspine_cave_bear",
                    () -> EntityType.Builder.<FrostspineCaveBearEntity>of(
                                    FrostspineCaveBearEntity::new, MobCategory.MONSTER)
                            .sized(2.8F, 2.2F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("frostspine_cave_bear"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBestiaryBeastEntity>>
            GREEN_PATTERNED_SPIRIT_DEVOURING_RAT =
            ENTITY_TYPES.register("green_patterned_spirit_devouring_rat",
                    () -> EntityType.Builder.<CultivationBestiaryBeastEntity>of(
                                    (type, level) -> new CultivationBestiaryBeastEntity(type, level,
                                            BestiaryBeastKind.SPIRIT_DEVOURING_RAT), MobCategory.MONSTER)
                            .sized(1.35F, 0.70F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("green_patterned_spirit_devouring_rat"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBestiaryBeastEntity>>
            RED_BACKED_IRON_SCALE_SCORPION =
            ENTITY_TYPES.register("red_backed_iron_scale_scorpion",
                    () -> EntityType.Builder.<CultivationBestiaryBeastEntity>of(
                                    (type, level) -> new CultivationBestiaryBeastEntity(type, level,
                                            BestiaryBeastKind.IRON_SCALE_SCORPION), MobCategory.MONSTER)
                            .sized(1.65F, 0.90F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("red_backed_iron_scale_scorpion"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBestiaryBeastEntity>>
            WIND_FEATHER_CLIFF_DEER =
            ENTITY_TYPES.register("wind_feather_cliff_deer",
                    () -> EntityType.Builder.<CultivationBestiaryBeastEntity>of(
                                    (type, level) -> new CultivationBestiaryBeastEntity(type, level,
                                            BestiaryBeastKind.WIND_FEATHER_CLIFF_DEER), MobCategory.MONSTER)
                            .sized(1.35F, 2.0F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("wind_feather_cliff_deer"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBestiaryBeastEntity>>
            INK_ARMORED_STONE_DEVOURING_TORTOISE =
            ENTITY_TYPES.register("ink_armored_stone_devouring_tortoise",
                    () -> EntityType.Builder.<CultivationBestiaryBeastEntity>of(
                                    (type, level) -> new CultivationBestiaryBeastEntity(type, level,
                                            BestiaryBeastKind.STONE_DEVOURING_TORTOISE), MobCategory.MONSTER)
                            .sized(2.65F, 1.30F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("ink_armored_stone_devouring_tortoise"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBestiaryBeastEntity>>
            EMBER_FEATHER_FIRE_OWL =
            ENTITY_TYPES.register("ember_feather_fire_owl",
                    () -> EntityType.Builder.<CultivationBestiaryBeastEntity>of(
                                    (type, level) -> new CultivationBestiaryBeastEntity(type, level,
                                            BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL), MobCategory.MONSTER)
                            .sized(1.55F, 1.75F)
                            .clientTrackingRange(18)
                            .updateInterval(1)
                            .fireImmune()
                            .build("ember_feather_fire_owl"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBeastProjectileEntity>>
            CULTIVATION_BEAST_PROJECTILE =
            ENTITY_TYPES.register("cultivation_beast_projectile",
                    () -> EntityType.Builder.<CultivationBeastProjectileEntity>of(
                                    CultivationBeastProjectileEntity::new, MobCategory.MISC)
                            .sized(0.28F, 0.28F)
                            .clientTrackingRange(14)
                            .updateInterval(1)
                            .build("cultivation_beast_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivationBeastFirePatchEntity>>
            CULTIVATION_BEAST_FIRE_PATCH =
            ENTITY_TYPES.register("cultivation_beast_fire_patch",
                    () -> EntityType.Builder.<CultivationBeastFirePatchEntity>of(
                                    CultivationBeastFirePatchEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune()
                            .build("cultivation_beast_fire_patch"));
    public static final DeferredHolder<EntityType<?>, EntityType<PhantomVeilFoxEntity>>
            ZHUJI_PHANTOM_VEIL_FOX =
            ENTITY_TYPES.register("zhuji_phantom_veil_fox",
                    () -> EntityType.Builder.<PhantomVeilFoxEntity>of(
                                    PhantomVeilFoxEntity::new, MobCategory.MONSTER)
                            .sized(0.95F, 1.05F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("zhuji_phantom_veil_fox"));

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomVeilFoxEntity>>
            JINDAN_PHANTOM_VEIL_FOX =
            ENTITY_TYPES.register("jindan_phantom_veil_fox",
                    () -> EntityType.Builder.<PhantomVeilFoxEntity>of(
                                    PhantomVeilFoxEntity::new, MobCategory.MONSTER)
                            .sized(1.10F, 1.25F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("jindan_phantom_veil_fox"));

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomVeilFoxEntity>>
            PHANTOM_VEIL_FOX =
            ENTITY_TYPES.register("phantom_veil_fox",
                    () -> EntityType.Builder.<PhantomVeilFoxEntity>of(
                                    PhantomVeilFoxEntity::new, MobCategory.MONSTER)
                            .sized(1.25F, 1.4F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("phantom_veil_fox"));

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomVeilFoxCloneEntity>>
            PHANTOM_VEIL_FOX_CLONE =
            ENTITY_TYPES.register("phantom_veil_fox_clone",
                    () -> EntityType.Builder.<PhantomVeilFoxCloneEntity>of(
                                    PhantomVeilFoxCloneEntity::new, MobCategory.MISC)
                            .sized(1.25F, 1.4F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("phantom_veil_fox_clone"));

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomFoxfireProjectileEntity>>
            PHANTOM_FOXFIRE =
            ENTITY_TYPES.register("phantom_foxfire",
                    () -> EntityType.Builder.<PhantomFoxfireProjectileEntity>of(
                                    PhantomFoxfireProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("phantom_foxfire"));

    public static final DeferredHolder<EntityType<?>, EntityType<CultivatorCorpseEntity>> CULTIVATOR_CORPSE =
            ENTITY_TYPES.register("cultivator_corpse",
                    () -> EntityType.Builder.<CultivatorCorpseEntity>of(
                            CultivatorCorpseEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("cultivator_corpse"));

    public static final DeferredHolder<EntityType<?>, EntityType<XuyingShadowEntity>> XUYING_SHADOW =
            ENTITY_TYPES.register("xuying_shadow",
                    () -> EntityType.Builder.<XuyingShadowEntity>of(
                            XuyingShadowEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("xuying_shadow"));

    public static final DeferredHolder<EntityType<?>, EntityType<SoulEntity>> SOUL =
            ENTITY_TYPES.register("soul",
                    () -> EntityType.Builder.<SoulEntity>of(
                            SoulEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("soul"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShihunfanEntity>> SHIHUNFAN =
            ENTITY_TYPES.register("shihunfan",
                    () -> EntityType.Builder.<ShihunfanEntity>of(
                            ShihunfanEntity::new, MobCategory.MISC)
                            .sized(1.35f, 2.4f)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("shihunfan"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShihunfanSoulWaveEntity>> SHIHUNFAN_SOUL_WAVE =
            ENTITY_TYPES.register("shihunfan_soul_wave",
                    () -> EntityType.Builder.<ShihunfanSoulWaveEntity>of(
                                    ShihunfanSoulWaveEntity::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("shihunfan_soul_wave"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShihunfanGhostEntity>> SHIHUNFAN_GHOST =
            ENTITY_TYPES.register("shihunfan_ghost",
                    () -> EntityType.Builder.<ShihunfanGhostEntity>of(
                                    ShihunfanGhostEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("shihunfan_ghost"));

    public static final DeferredHolder<EntityType<?>, EntityType<FuchenBindingPlumeEntity>> FUCHEN_BINDING_PLUME =
            ENTITY_TYPES.register("fuchen_binding_plume",
                    () -> EntityType.Builder.<FuchenBindingPlumeEntity>of(
                                    FuchenBindingPlumeEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("fuchen_binding_plume"));

    public static final DeferredHolder<EntityType<?>, EntityType<WantengVineEntity>> WANTENG_VINE =
            ENTITY_TYPES.register("wanteng_vine",
                    () -> EntityType.Builder.<WantengVineEntity>of(
                                    WantengVineEntity::new, MobCategory.MISC)
                            .sized(0.7F, 5.5F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("wanteng_vine"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShanbengSealEntity>> SHANBENG_SEAL =
            ENTITY_TYPES.register("shanbeng_seal",
                    () -> EntityType.Builder.<ShanbengSealEntity>of(
                                    ShanbengSealEntity::new, MobCategory.MISC)
                            .sized(5.0F, 4.0F)
                            .clientTrackingRange(20)
                            .updateInterval(1)
                            .build("shanbeng_seal"));

    public static final DeferredHolder<EntityType<?>, EntityType<WeaponQiSlashEntity>> WEAPON_QI_SLASH =
            ENTITY_TYPES.register("weapon_qi_slash",
                    () -> EntityType.Builder.<WeaponQiSlashEntity>of(
                                    WeaponQiSlashEntity::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("weapon_qi_slash"));

    public static final DeferredHolder<EntityType<?>, EntityType<JindanCloneEntity>> JINDAN_CLONE =
            ENTITY_TYPES.register("jindan_clone",
                    () -> EntityType.Builder.<JindanCloneEntity>of(
                            JindanCloneEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("jindan_clone"));

    public static final DeferredHolder<EntityType<?>, EntityType<JindanEntity>> JINDAN =
            ENTITY_TYPES.register("jindan",
                    () -> EntityType.Builder.<JindanEntity>of(
                            JindanEntity::new, MobCategory.MISC)
                            .sized(0.8f, 1.8f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("jindan"));

    public static final DeferredHolder<EntityType<?>, EntityType<SystemManifestEntity>>
            SYSTEM_MANIFEST =
            ENTITY_TYPES.register("system_manifest",
                    () -> EntityType.Builder.<SystemManifestEntity>of(
                            SystemManifestEntity::new, MobCategory.MISC)
                            .sized(2.3F, 1.45F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("system_manifest"));

    public static final DeferredHolder<EntityType<?>, EntityType<AsteroidEntity>> ASTEROID =
            ENTITY_TYPES.register("asteroid",
                    () -> EntityType.Builder.<AsteroidEntity>of(
                                    AsteroidEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .fireImmune()
                            .build("asteroid"));


    public static void register(IEventBus bus) { ENTITY_TYPES.register(bus); }
}
