package com.example.immortal_cultivation_mod.sound;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_CAST = SOUND_EVENTS.register(
            "fireball_cast",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "fireball_cast")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QI_REGEN = SOUND_EVENTS.register(
            "qi_regen",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "qi_regen")));

    public static void register(IEventBus bus) { SOUND_EVENTS.register(bus); }
}
