package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.flame.FlameVesselItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FlameClientItems {
    private FlameClientItems() { }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Retained for resource packs that still use the earlier binary predicate.
            ItemProperties.register(ModItems.FLAME_VESSEL.get(),
                    ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "filled"),
                    (stack, level, holder, seed) -> FlameVesselItem.flame(stack) == null ? 0.0F : 1.0F);
            // Use the unclamped function: eight stable kind values must not collapse to 1.
            ItemProperties.register(ModItems.FLAME_VESSEL.get(),
                    ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "flame_kind"),
                    (ItemPropertyFunction) (stack, level, holder, seed) -> {
                        var kind = FlameVesselItem.flame(stack);
                        return kind == null ? 0.0F : kind.modelIndex();
                    });
        });
    }
}
