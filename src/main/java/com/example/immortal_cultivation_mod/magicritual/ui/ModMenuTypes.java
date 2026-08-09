package com.example.immortal_cultivation_mod.magicritual.ui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "immortal_cultivation_mod");
   public static final DeferredHolder<MenuType<?>, MenuType<CenterFuelMenu>> CENTER_FUEL = MENUS.register(
      "center_fuel", () -> IMenuTypeExtension.create(CenterFuelMenu::new)
   );

   public static void register(IEventBus bus) {
      MENUS.register(bus);
   }
}
