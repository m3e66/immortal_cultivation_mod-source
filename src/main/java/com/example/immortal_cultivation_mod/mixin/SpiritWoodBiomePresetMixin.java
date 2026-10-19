package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.worldgen.SpiritWoodBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Resolves the grove after the playable biome registry has loaded. */
@Mixin(MultiNoiseBiomeSourceParameterList.class)
public abstract class SpiritWoodBiomePresetMixin implements SpiritWoodBiomes.GrovePreset {
    @Shadow @Final private Climate.ParameterList<Holder<Biome>> parameters;
    @Unique private Holder<Biome> immortalCultivation$grove;

    @Inject(method="<init>", at=@At("TAIL"))
    private void immortalCultivation$findGrove(MultiNoiseBiomeSourceParameterList.Preset preset,
            HolderGetter<Biome> biomes, CallbackInfo callback) {
        if (!preset.equals(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD)) return;
        // The vanilla registry bootstrap uses unbound holders and a creating getter.
        // Never request mod-only keys there; the actual data-pack load binds biomes first.
        if (parameters.values().stream().anyMatch(entry ->
                entry.getSecond() instanceof Holder.Reference<Biome> holder && !holder.isBound())) return;
        biomes.get(SpiritWoodBiomes.GROVE).filter(Holder.Reference::isBound).ifPresent(grove -> immortalCultivation$grove=grove);
    }

    @Override public Holder<Biome> immortalCultivation$grove() { return immortalCultivation$grove; }
}
