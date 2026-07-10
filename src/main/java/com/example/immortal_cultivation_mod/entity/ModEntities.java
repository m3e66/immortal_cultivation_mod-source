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

    public static final DeferredHolder<EntityType<?>, EntityType<FireballProjectileEntity>> FIREBALL_PROJECTILE =
            ENTITY_TYPES.register("fireball_projectile",
                    () -> EntityType.Builder.<FireballProjectileEntity>of(
                            FireballProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build("fireball_projectile"));

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

    public static final DeferredHolder<EntityType<?>, EntityType<ZhenshanPalmEntity>> ZHENSHAN_PALM =
            ENTITY_TYPES.register("zhenshan_palm",
                    () -> EntityType.Builder.<ZhenshanPalmEntity>of(
                            ZhenshanPalmEntity::new, MobCategory.MISC)
                            .sized(2.5f, 2.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("zhenshan_palm"));

    public static final DeferredHolder<EntityType<?>, EntityType<GudiaoScratchEntity>> GUDIAO_SCRATCH =
            ENTITY_TYPES.register("gudiao_scratch",
                    () -> EntityType.Builder.<GudiaoScratchEntity>of(
                            GudiaoScratchEntity::new, MobCategory.MISC)
                            .sized(2.5f, 2.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("gudiao_scratch"));

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

    public static final DeferredHolder<EntityType<?>, EntityType<GudiaoEntity>> GUDIAO =
            ENTITY_TYPES.register("gudiao",
                    () -> EntityType.Builder.<GudiaoEntity>of(
                            GudiaoEntity::new, MobCategory.MONSTER)
                            .sized(1.2f, 1.7f)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("gudiao"));

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

    public static void register(IEventBus bus) { ENTITY_TYPES.register(bus); }
}
