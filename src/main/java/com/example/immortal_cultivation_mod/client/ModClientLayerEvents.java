package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.renderer.ZhenshiSealHelmetLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModClientLayerEvents {
    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        addSealLayer(event, EntityType.ZOMBIE);
        addSealLayer(event, EntityType.ZOMBIE_VILLAGER);
        addSealLayer(event, EntityType.HUSK);
        addSealLayer(event, EntityType.DROWNED);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends LivingEntity, M extends HumanoidModel<T>> void addSealLayer(EntityRenderersEvent.AddLayers event, EntityType<T> type) {
        var renderer = event.getRenderer(type);
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            ((LivingEntityRenderer<T, M>) livingRenderer)
                    .addLayer(new ZhenshiSealHelmetLayer<>((RenderLayerParent<T, M>) livingRenderer));
        }
    }
}
