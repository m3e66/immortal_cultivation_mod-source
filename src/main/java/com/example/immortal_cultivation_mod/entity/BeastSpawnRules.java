package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Natural ecology for every wild beast. The sealed Xuanjin companion is deliberately excluded. */
public final class BeastSpawnRules {
    public record SpawnRule(int realm, int chanceDenominator) {
        public double radius() { return separationRadius(realm); }
    }

    private static final Map<EntityType<?>, SpawnRule> RULES = Map.ofEntries(
            Map.entry(ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(), new SpawnRule(3, 1)),
            Map.entry(ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(), new SpawnRule(4, 1)),
            Map.entry(ModEntities.BLOOD_APE_DEMON_GENERAL.get(), new SpawnRule(4, 8)),
            Map.entry(ModEntities.AZURE_SPIRIT_SILKWORM.get(), new SpawnRule(1, 1)),
            Map.entry(ModEntities.KONGMING_BEETLE.get(), new SpawnRule(2, 1)),
            Map.entry(ModEntities.THUNDERHORN_ROCK_BADGER.get(), new SpawnRule(2, 1)),
            Map.entry(ModEntities.FROSTSPINE_CAVE_BEAR.get(), new SpawnRule(2, 1)),
            Map.entry(ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(), new SpawnRule(1, 1)),
            Map.entry(ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get(), new SpawnRule(1, 1)),
            Map.entry(ModEntities.WIND_FEATHER_CLIFF_DEER.get(), new SpawnRule(2, 1)),
            Map.entry(ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get(), new SpawnRule(2, 1)),
            Map.entry(ModEntities.EMBER_FEATHER_FIRE_OWL.get(), new SpawnRule(3, 1)),
            Map.entry(ModEntities.PHANTOM_VEIL_FOX.get(), new SpawnRule(4, 1)));

    private BeastSpawnRules() { }

    public static Map<EntityType<?>, SpawnRule> rules() { return RULES; }

    /** One radius per major realm; early/middle/late stages do not change spacing. */
    public static double separationRadius(int realm) {
        if (realm < 0 || realm > 9) throw new IllegalArgumentException("Invalid beast realm: " + realm);
        return Math.pow(3, realm);
    }

    public static boolean hasRealmSeparation(EntityType<?> type, ServerLevelAccessor level, BlockPos pos) {
        SpawnRule candidate = RULES.get(type);
        if (candidate == null) return false;
        return hasRealmSeparation(candidate.realm(), level, pos);
    }

    public static boolean hasRealmSeparation(int realm, ServerLevelAccessor level, BlockPos pos) {
        double radius = separationRadius(realm);
        Vec3 center = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        AABB area = new AABB(center, center).inflate(radius);
        return level.getLevel().getEntitiesOfClass(Mob.class, area, mob -> {
            SpawnRule existing = RULES.get(mob.getType());
            return mob.isAlive() && existing != null && mob instanceof RealmBeast beast && beast.beastRealm() == realm
                    && mob.distanceToSqr(center) <= radius * radius;
        }).isEmpty();
    }

    public static TagKey<Biome> habitatTag(EntityType<?> type) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "beast_spawns/" + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()));
    }

    public static void register(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.SILVER_MIST_WOLF.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> net.minecraft.world.entity.animal.Wolf.checkWolfSpawnRules(
                        EntityType.WOLF, level, reason, pos, random), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        register(event, ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(), BeastSpawnRules::checkLizard);
        register(event, ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(), CrimsonSkyFlameFloodDragonEntity::checkDragonSpawnRules);
        register(event, ModEntities.BLOOD_APE_DEMON_GENERAL.get(), BeastSpawnRules::checkBloodApe);
        register(event, ModEntities.AZURE_SPIRIT_SILKWORM.get(), AzureSpiritSilkwormEntity::checkAzureSpawnRules);
        register(event, ModEntities.KONGMING_BEETLE.get(), KongmingBeetleEntity::checkKongmingSpawnRules);
        register(event, ModEntities.THUNDERHORN_ROCK_BADGER.get(), ThunderhornRockBadgerEntity::checkSpawnRules);
        register(event, ModEntities.FROSTSPINE_CAVE_BEAR.get(), FrostspineCaveBearEntity::checkSpawnRules);
        register(event, ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(), CultivationBestiaryBeastEntity::checkSpawnRules);
        register(event, ModEntities.RED_BACKED_IRON_SCALE_SCORPION.get(), CultivationBestiaryBeastEntity::checkSpawnRules);
        register(event, ModEntities.WIND_FEATHER_CLIFF_DEER.get(), CultivationBestiaryBeastEntity::checkSpawnRules);
        register(event, ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get(), CultivationBestiaryBeastEntity::checkSpawnRules);
        register(event, ModEntities.EMBER_FEATHER_FIRE_OWL.get(), CultivationBestiaryBeastEntity::checkSpawnRules);
        register(event, ModEntities.PHANTOM_VEIL_FOX.get(), PhantomVeilFoxEntity::checkSpawnRules);
    }

    private static <T extends Mob> void register(RegisterSpawnPlacementsEvent event, EntityType<T> type,
            SpawnPlacements.SpawnPredicate<T> habitat) {
        event.register(type, type == ModEntities.EMBER_FEATHER_FIRE_OWL.get()
                        ? SpawnPlacementTypes.NO_RESTRICTIONS : SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, reason, pos, random) -> checkEnvironment(entityType, level, reason, pos, random)
                        && (!isNatural(reason) || habitat.test(entityType, level, reason, pos, random)),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static boolean isNatural(MobSpawnType reason) {
        return reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION;
    }

    public static boolean checkEnvironment(EntityType<?> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        SpawnRule population = RULES.get(type);
        if (population == null) return false;
        // Egg, command, spawner, and scripted encounters do not depend on natural ecology.
        if (!isNatural(reason)) return true;
        boolean netherFlame = type == ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get()
                || type == ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get()
                || type == ModEntities.EMBER_FEATHER_FIRE_OWL.get();
        if ((!level.getLevel().dimension().equals(Level.OVERWORLD)
                && !(netherFlame && level.getLevel().dimension().equals(Level.NETHER)))
                || (type.getCategory() == MobCategory.MONSTER && level.getDifficulty() == Difficulty.PEACEFUL)
                || !level.getBiome(pos).is(habitatTag(type))
                || !level.getWorldBorder().isWithinBounds(pos)
                || !hasClearVolume(type, level, pos)) return false;
        if (population.chanceDenominator() > 1 && random.nextInt(population.chanceDenominator()) != 0) return false;
        return hasRealmSeparation(BeastRealms.naturalRealm(type, level, pos), level, pos);
    }

    private static boolean hasClearVolume(EntityType<?> type, ServerLevelAccessor level, BlockPos pos) {
        AABB box = type.getDimensions().makeBoundingBox(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                || !level.noCollision(box)) return false;
        for (BlockPos cursor : BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY),
                (int) Math.floor(box.minZ), (int) Math.floor(box.maxX - 1e-6),
                (int) Math.floor(box.maxY - 1e-6), (int) Math.floor(box.maxZ - 1e-6))) {
            if (!level.getFluidState(cursor).isEmpty()) return false;
        }
        return true;
    }

    private static boolean checkLizard(EntityType<CrimsonScaleFlameLizardEntity> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        var floor = level.getBlockState(pos.below());
        if (floor.is(Blocks.NETHERRACK) || floor.is(Blocks.BASALT) || floor.is(Blocks.BLACKSTONE)
                || floor.is(Blocks.MAGMA_BLOCK) || floor.is(BlockTags.TERRACOTTA)
                || floor.is(Blocks.RED_SAND)) return true;
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 2, 4))) {
            if (level.getFluidState(cursor).is(FluidTags.LAVA)) return true;
        }
        return false;
    }

    private static boolean checkBloodApe(EntityType<BloodApeDemonGeneralEntity> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        var floorPos = pos.below();
        var floor = level.getBlockState(floorPos);
        return floor.isFaceSturdy(level, floorPos, Direction.UP)
                && (floor.is(BlockTags.DIRT) || floor.is(Blocks.MOSS_BLOCK) || floor.is(Blocks.STONE))
                && (level.getLevel().isNight() || !level.canSeeSky(pos));
    }
}
