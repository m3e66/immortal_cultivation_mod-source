package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.worldgen.SpiritWoodBiomes;
import com.mojang.datafixers.util.Either;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only selected forest results while retaining the original climate search tree. */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class SpiritWoodBiomeSourceMixin {
    @Shadow @Final private Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;
    @Unique private Holder<Biome> immortalCultivation$grove;
    @Unique private boolean immortalCultivation$groveChecked;

    @Unique private Holder<Biome> immortalCultivation$grove() {
        if (!immortalCultivation$groveChecked) {
            var preset = parameters.right();
            if (preset.isPresent() && preset.get() instanceof Holder.Reference<MultiNoiseBiomeSourceParameterList> holder && !holder.isBound()) return null;
            immortalCultivation$grove = preset.map(holder -> ((SpiritWoodBiomes.GrovePreset)holder.value()).immortalCultivation$grove()).orElse(null);
            immortalCultivation$groveChecked = true;
        }
        return immortalCultivation$grove;
    }

    @Inject(method="collectPossibleBiomes", at=@At("RETURN"), cancellable=true)
    private void immortalCultivation$includeGrove(CallbackInfoReturnable<Stream<Holder<Biome>>> result) {
        var grove = immortalCultivation$grove();
        if (grove != null) result.setReturnValue(Stream.concat(result.getReturnValue(), Stream.of(grove)).distinct());
    }

    @Inject(method="getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;", at=@At("RETURN"), cancellable=true)
    private void immortalCultivation$sampleGrove(Climate.TargetPoint point, CallbackInfoReturnable<Holder<Biome>> result) {
        if (result.getReturnValue().is(Biomes.FOREST) && SpiritWoodBiomes.groveClimate(point)) {
            var grove = immortalCultivation$grove();
            if (grove != null) result.setReturnValue(grove);
        }
    }
}
