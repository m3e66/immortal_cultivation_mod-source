package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModScreens {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<QiPouchMenu>> QI_POUCH =
            MENUS.register("qi_pouch", () -> new MenuType<>(QiPouchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<DingMenu>> DING =
            MENUS.register("ding", () -> new MenuType<>(DingMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) { MENUS.register(bus); }
}
