package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModKeyMappings {
    public static final String KEY_CATEGORY = "key.categories." + ImmortalCultivationMod.MODID;

    public static final Lazy<KeyMapping> OPEN_STAT_MENU = Lazy.of(() -> new KeyMapping(
            "key." + ImmortalCultivationMod.MODID + ".open_stat_menu",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KEY_CATEGORY));

    public static final Lazy<KeyMapping> OPEN_SPELL_WHEEL = Lazy.of(() -> new KeyMapping(
            "key." + ImmortalCultivationMod.MODID + ".open_spell_wheel",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, KEY_CATEGORY));

    public static final Lazy<KeyMapping> MEDITATE = Lazy.of(() -> new KeyMapping(
            "key." + ImmortalCultivationMod.MODID + ".meditate",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY));

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STAT_MENU.get());
        event.register(OPEN_SPELL_WHEEL.get());
        event.register(MEDITATE.get());
    }
}
