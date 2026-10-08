package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** A saved deadline also expires stationary, retargeting and reloaded spell projectiles. */
@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class SpellProjectileLifetime {
    private static final String DEADLINE="ImmortalSpellExpiry";
    private SpellProjectileLifetime() {}

    public static int maximumTicks(Entity entity) {
        if (!(entity instanceof Projectile)) return 0;
        var id=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!id.getNamespace().equals("immortal_cultivation_mod")) return 0;
        if (entity instanceof LightBeamProjectileEntity || entity instanceof XinliaoFireballEntity) return 600;
        // Recoverable weapons and messenger talismans have their own return/pickup lifecycle.
        return id.getPath().endsWith("_projectile") || entity instanceof CrimsonFlameFireballEntity
                || entity instanceof TianyunFenchengMeteorEntity || entity instanceof PetalTechniqueEntity ? 200 : 0;
    }

    public static boolean expire(Entity entity) {
        if (entity.level().isClientSide || entity.isRemoved()) return false;
        int duration=maximumTicks(entity);
        if(duration==0)return false;
        var data=entity.getPersistentData();long now=entity.level().getGameTime();
        if(!data.contains(DEADLINE))data.putLong(DEADLINE,now+Math.max(0,duration-entity.tickCount));
        if(now<data.getLong(DEADLINE))return false;
        entity.discard();
        return true;
    }

    @SubscribeEvent public static void beforeTick(EntityTickEvent.Pre event) {
        if(expire(event.getEntity()))event.setCanceled(true);
    }
}
