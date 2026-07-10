package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.entity.FanrenNpcEntity;
import com.example.immortal_cultivation_mod.entity.GudiaoEntity;
import com.example.immortal_cultivation_mod.entity.JindanCloneEntity;
import com.example.immortal_cultivation_mod.entity.JindanCultivatorEntity;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.XuyingShadowEntity;
import com.example.immortal_cultivation_mod.entity.ZhujiCultivatorEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ZHUJI_CULTIVATOR.get(), ZhujiCultivatorEntity.createAttributes().build());
        event.put(ModEntities.JINDAN_CULTIVATOR.get(), JindanCultivatorEntity.createAttributes().build());
        event.put(ModEntities.FANREN_NPC.get(), FanrenNpcEntity.createAttributes().build());
        event.put(ModEntities.GUDIAO.get(), GudiaoEntity.createAttributes().build());
        event.put(ModEntities.CULTIVATOR_CORPSE.get(), CultivatorCorpseEntity.createAttributes().build());
        event.put(ModEntities.XUYING_SHADOW.get(), XuyingShadowEntity.createAttributes().build());
        event.put(ModEntities.JINDAN_CLONE.get(), JindanCloneEntity.createAttributes().build());
        event.put(ModEntities.JINDAN.get(), JindanEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.FANREN_NPC.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> true,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.ZOMBIE, ModAttributes.GRUDGE);
        event.add(EntityType.ZOMBIE_VILLAGER, ModAttributes.GRUDGE);
        event.add(EntityType.HUSK, ModAttributes.GRUDGE);
        event.add(EntityType.DROWNED, ModAttributes.GRUDGE);
    }
}
