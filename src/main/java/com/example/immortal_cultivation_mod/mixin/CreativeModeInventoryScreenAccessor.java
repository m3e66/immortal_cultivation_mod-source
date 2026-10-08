package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only bridge used by the mod's nested creative inventory tabs. */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor("selectedTab")
    static CreativeModeTab immortalCultivation$getSelectedTab() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("scrollOffs")
    void immortalCultivation$setScrollOffset(float value);
}
