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

    public static final DeferredHolder<EntityType<?>, EntityType<ZhenshanPalmEntity>> ZHENSHAN_PALM =
            ENTITY_TYPES.register("zhenshan_palm",
                    () -> EntityType.Builder.<ZhenshanPalmEntity>of(
                            ZhenshanPalmEntity::new, MobCategory.MISC)
                            .sized(2.5f, 2.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("zhenshan_palm"));

    public static void register(IEventBus bus) { ENTITY_TYPES.register(bus); }
}
