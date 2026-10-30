package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Keep existing wolf habitats and spawn weights; change only their group size. */
public record WolfPackBiomeModifier() implements BiomeModifier {
    public static final MapCodec<WolfPackBiomeModifier> CODEC = MapCodec.unit(WolfPackBiomeModifier::new);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ImmortalCultivationMod.MODID);
    static { SERIALIZERS.register("wolf_packs", () -> CODEC); }
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC; }
    @Override public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY) return;
        for (MobCategory category : MobCategory.values()) {
            var spawns = builder.getMobSpawnSettings().getSpawner(category);
            int wolfWeight = 0;
            for (int i = 0; i < spawns.size(); i++) {
                var old = spawns.get(i);
                if (old.type == EntityType.WOLF) {
                    wolfWeight = Math.max(wolfWeight, old.getWeight().asInt());
                    spawns.set(i, new MobSpawnSettings.SpawnerData(old.type,
                            old.getWeight().asInt(), WolfPackBehavior.NATURAL_PACK_SIZE, WolfPackBehavior.NATURAL_PACK_SIZE));
                }
            }
            if (wolfWeight > 0 && spawns.stream().noneMatch(s -> s.type == ModEntities.SILVER_MIST_WOLF.get()))
                spawns.add(new MobSpawnSettings.SpawnerData(ModEntities.SILVER_MIST_WOLF.get(),
                        wolfWeight, 2, 5));
        }
    }
}
