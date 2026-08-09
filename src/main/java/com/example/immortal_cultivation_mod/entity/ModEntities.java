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

    public static final DeferredHolder<EntityType<?>, EntityType<LiusanEntity>> LIUSAN =
            ENTITY_TYPES.register("liusan",
                    () -> EntityType.Builder.<LiusanEntity>of(
                            LiusanEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("liusan"));

    public static final DeferredHolder<EntityType<?>, EntityType<FanrenNpcEntity>> FANREN_NPC =
            ENTITY_TYPES.register("fanren_npc",
                    () -> EntityType.Builder.<FanrenNpcEntity>of(
                            FanrenNpcEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("fanren_npc"));

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

    public static void register(IEventBus bus) { ENTITY_TYPES.register(bus); }
}
