package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.RealmPressureClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Clips vanilla rain columns at pressure's upper dome, preserving rain above and outside it. */
@Mixin(LevelRenderer.class)
public abstract class RealmPressureWeatherMixin {
    // Delegate through other weather mods rather than exclusively redirecting their call sites.
    // A completely replaced weather method may omit these calls; that must not crash startup.
    @WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"), require = 0)
    private int immortalCultivation$clipPressureRain(Level level, Heightmap.Types type, int x, int z, Operation<Integer> original) {
        int ground = original.call(level, type, x, z);
        double ceiling = RealmPressureClient.rainCeiling(x + .5, z + .5);
        if (!Double.isFinite(ceiling)) return ground;
        BlockPos surface = new BlockPos(x, ground, z);
        if (level.getBiome(surface).value().getPrecipitationAt(surface) != Biome.Precipitation.RAIN) return ground;
        return Math.max(ground, (int) Math.ceil(ceiling));
    }

    @WrapOperation(method = "tickRain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"), require = 0)
    private Biome.Precipitation immortalCultivation$noPressureSplashes(Biome biome, BlockPos pos, Operation<Biome.Precipitation> original) {
        Biome.Precipitation precipitation = original.call(biome, pos);
        return precipitation == Biome.Precipitation.RAIN && RealmPressureClient.suppressRainAt(Vec3.atCenterOf(pos))
                ? Biome.Precipitation.NONE : precipitation;
    }
}
