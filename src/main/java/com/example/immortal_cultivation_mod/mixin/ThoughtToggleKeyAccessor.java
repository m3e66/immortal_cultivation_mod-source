package com.example.immortal_cultivation_mod.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ToggleKeyMapping.class)
public interface ThoughtToggleKeyAccessor {
    @Accessor("needsToggle") BooleanSupplier immortalCultivation$needsToggle();
}
