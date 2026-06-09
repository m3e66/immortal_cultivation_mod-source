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

    public static void register(IEventBus bus) { ENTITY_TYPES.register(bus); }
}
