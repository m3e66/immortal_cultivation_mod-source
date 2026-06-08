package com.example.immortal_cultivation_mod;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModTabs;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.sound.ModSounds;
import com.example.immortal_cultivation_mod.screen.ModScreens;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ImmortalCultivationMod.MODID)
public class ImmortalCultivationMod {
    public static final String MODID = "immortal_cultivation_mod";

    public ImmortalCultivationMod(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        ModEffects.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModScreens.MENUS.register(modEventBus);
        ModTabs.CREATIVE_TABS.register(modEventBus);
    }
}
