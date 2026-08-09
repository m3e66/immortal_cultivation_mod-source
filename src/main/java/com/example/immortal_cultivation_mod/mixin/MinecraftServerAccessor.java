package com.example.immortal_cultivation_mod.mixin;

import java.util.concurrent.Executor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("executor")
    Executor immortalCultivation$getExecutor();

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess immortalCultivation$getStorageSource();
}
