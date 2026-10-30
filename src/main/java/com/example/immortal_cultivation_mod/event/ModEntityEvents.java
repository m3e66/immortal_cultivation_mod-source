package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.entity.AzureSpiritSilkwormEntity;
import com.example.immortal_cultivation_mod.entity.BloodApeDemonGeneralEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.entity.CultivatorSpawnRules;
import com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity;
import com.example.immortal_cultivation_mod.entity.FanrenNpcEntity;
import com.example.immortal_cultivation_mod.entity.HotelNpcEntity;
import com.example.immortal_cultivation_mod.entity.JindanCloneEntity;
import com.example.immortal_cultivation_mod.entity.JindanCultivatorEntity;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
import com.example.immortal_cultivation_mod.entity.KongmingBeetleEntity;
import com.example.immortal_cultivation_mod.entity.FrostspineCaveBearEntity;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxTier;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxCloneEntity;
import com.example.immortal_cultivation_mod.entity.ThunderhornRockBadgerEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.LianqiCultivatorEntity;
import com.example.immortal_cultivation_mod.entity.SoulEntity;
import com.example.immortal_cultivation_mod.entity.SystemManifestEntity;
import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import com.example.immortal_cultivation_mod.entity.ShihunfanGhostEntity;
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
        event.put(ModEntities.SILVER_MIST_WOLF.get(), com.example.immortal_cultivation_mod.entity.SilverMistWolfEntity.createAttributes().build());
        event.put(ModEntities.MOONSHADE_HORSE.get(), com.example.immortal_cultivation_mod.entity.MoonshadeHorseEntity.attributes().build());
        event.put(ModEntities.RUSSET_SPIRIT_DEER.get(), com.example.immortal_cultivation_mod.entity.MortalAnimalEntity.createAttributes(com.example.immortal_cultivation_mod.entity.MortalAnimalEntity.Kind.DEER).build());
        event.put(ModEntities.CHESTNUT_SPIRIT_COW.get(), com.example.immortal_cultivation_mod.entity.MortalAnimalEntity.createAttributes(com.example.immortal_cultivation_mod.entity.MortalAnimalEntity.Kind.COW).build());
        event.put(ModEntities.FIELD_MOUSE.get(), com.example.immortal_cultivation_mod.entity.MortalAnimalEntity.createAttributes(com.example.immortal_cultivation_mod.entity.MortalAnimalEntity.Kind.MOUSE).build());
        event.put(ModEntities.JADECOIL_SNAKE.get(), com.example.immortal_cultivation_mod.entity.JadecoilSnakeEntity.createAttributes().build());
        event.put(ModEntities.XUANJIN_ZHENG.get(), com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity.createAttributes().build());
        event.put(ModEntities.LIANQI_CULTIVATOR.get(), LianqiCultivatorEntity.createAttributes().build());
        event.put(ModEntities.ZHUJI_CULTIVATOR.get(), ZhujiCultivatorEntity.createAttributes().build());
        event.put(ModEntities.JINDAN_CULTIVATOR.get(), JindanCultivatorEntity.createAttributes().build());
        event.put(ModEntities.TRAINING_DUMMY.get(),com.example.immortal_cultivation_mod.entity.TrainingDummyEntity.createAttributes().build());
        event.put(ModEntities.FANREN_NPC.get(), FanrenNpcEntity.createAttributes().build());
        event.put(ModEntities.HOTEL_NPC.get(), HotelNpcEntity.createAttributes().build());
        event.put(ModEntities.RECIPE_MERCHANT.get(), ZhujiCultivatorEntity.createAttributes().build());
        event.put(ModEntities.SPELL_MERCHANT.get(), ZhujiCultivatorEntity.createAttributes().build());
        event.put(ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(),
                CrimsonScaleFlameLizardEntity.createAttributes().build());
        event.put(ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(),
                CrimsonSkyFlameFloodDragonEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_APE_DEMON_GENERAL.get(),
                BloodApeDemonGeneralEntity.createAttributes().build());
        event.put(ModEntities.VAULT_GUARDIAN.get(), VaultGuardianEntity.createAttributes().build());
        event.put(ModEntities.ESCAPED_YUANYING_CORE.get(),
                EscapedYuanyingCoreEntity.createAttributes().build());
        event.put(ModEntities.AZURE_SPIRIT_SILKWORM.get(),
                AzureSpiritSilkwormEntity.createAttributes().build());
        event.put(ModEntities.KONGMING_BEETLE.get(),
                KongmingBeetleEntity.createAttributes().build());
        event.put(ModEntities.THUNDERHORN_ROCK_BADGER.get(),
                ThunderhornRockBadgerEntity.createAttributes().build());
        event.put(ModEntities.FROSTSPINE_CAVE_BEAR.get(),
                FrostspineCaveBearEntity.createAttributes().build());
        event.put(ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(),
                com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity.ratAttributes().build());
        event.put(ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get(),
                com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity.scorpionAttributes().build());
        event.put(ModEntities.WIND_FEATHER_CLIFF_DEER.get(),
                com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity.deerAttributes().build());
        event.put(ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get(),
                com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity.tortoiseAttributes().build());
        event.put(ModEntities.EMBER_FEATHER_FIRE_OWL.get(),
                com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity.owlAttributes().build());
        event.put(ModEntities.PHANTOM_VEIL_FOX.get(),
                PhantomVeilFoxEntity.createAttributes(PhantomVeilFoxTier.YUANYING).build());
        event.put(ModEntities.PHANTOM_VEIL_FOX_CLONE.get(),
                PhantomVeilFoxCloneEntity.createAttributes().build());
        event.put(ModEntities.CULTIVATOR_CORPSE.get(), CultivatorCorpseEntity.createAttributes().build());
        event.put(ModEntities.XUYING_SHADOW.get(), XuyingShadowEntity.createAttributes().build());
        event.put(ModEntities.SOUL.get(), SoulEntity.createAttributes().build());
        event.put(ModEntities.SHIHUNFAN_GHOST.get(), ShihunfanGhostEntity.createAttributes().build());
        event.put(ModEntities.JINDAN_CLONE.get(), JindanCloneEntity.createAttributes().build());
        event.put(ModEntities.JINDAN.get(), JindanEntity.createAttributes().build());
        event.put(ModEntities.SYSTEM_MANIFEST.get(),
                SystemManifestEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.LIANQI_CULTIVATOR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CultivatorSpawnRules::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.ZHUJI_CULTIVATOR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CultivatorSpawnRules::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.JINDAN_CULTIVATOR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CultivatorSpawnRules::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.FANREN_NPC.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CultivatorSpawnRules::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.HOTEL_NPC.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> true,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        com.example.immortal_cultivation_mod.entity.BeastSpawnRules.register(event);
        event.register(ModEntities.RECIPE_MERCHANT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CultivatorSpawnRules::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SPELL_MERCHANT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CultivatorSpawnRules::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ModAttributes.GRUDGE);
        event.add(EntityType.ZOMBIE, ModAttributes.GRUDGE);
        event.add(EntityType.ZOMBIE_VILLAGER, ModAttributes.GRUDGE);
        event.add(EntityType.HUSK, ModAttributes.GRUDGE);
        event.add(EntityType.DROWNED, ModAttributes.GRUDGE);
    }
}
