package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.RealmPressureClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Clips vanilla rain columns at pressure's upper dome, preserving rain above and outside it. */
@Mixin(LevelRenderer.class)
public abstract class RealmPressureWeatherMixin {
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int immortalCultivation$clipPressureRain(Level level, Heightmap.Types type, int x, int z) {
        int ground = level.getHeight(type, x, z);
        BlockPos surface = new BlockPos(x, ground, z);
        if (level.getBiome(surface).value().getPrecipitationAt(surface) != Biome.Precipitation.RAIN) return ground;
        double ceiling = RealmPressureClient.rainCeiling(x + .5, z + .5);
        return Double.isFinite(ceiling) ? Math.max(ground, (int) Math.ceil(ceiling)) : ground;
    }

    @Redirect(method = "tickRain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation immortalCultivation$noPressureSplashes(Biome biome, BlockPos pos) {
        return RealmPressureClient.suppressRainAt(Vec3.atCenterOf(pos))
                ? Biome.Precipitation.NONE : biome.getPrecipitationAt(pos);
    }
}
