package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.renderer.ZhenshiSealHelmetLayer;
import com.example.immortal_cultivation_mod.client.renderer.XiujianHeldLayer;
import com.example.immortal_cultivation_mod.client.renderer.SpiritualReconstructionLayer;
import com.example.immortal_cultivation_mod.client.renderer.SpiritualReconstructionRenderer;
import com.example.immortal_cultivation_mod.client.renderer.BlackFlameDeathRenderer;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModClientLayerEvents {
    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        Set<LivingEntityRenderer<?, ?>> spiritualRenderers =
                Collections.newSetFromMap(new IdentityHashMap<>());
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new XiujianHeldLayer(renderer));
                addSpiritualLayer(renderer, spiritualRenderers);
            }
        }
        for (EntityType<?> entityType : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer((EntityType) entityType);
            addSpiritualLayer(renderer, spiritualRenderers);
        }
        addSealLayer(event, EntityType.ZOMBIE);
        addSealLayer(event, EntityType.ZOMBIE_VILLAGER);
        addSealLayer(event, EntityType.HUSK);
        addSealLayer(event, EntityType.DROWNED);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event)
            throws IOException {
        SpiritualReconstructionRenderer.registerShader(event);
        CosmicVoidSkyShaders.registerShaders(event);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addSpiritualLayer(
            EntityRenderer<?> renderer,
            Set<LivingEntityRenderer<?, ?>> attached) {
        if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)
                || !attached.add(livingRenderer)) {
            return;
        }
        ((LivingEntityRenderer) livingRenderer).addLayer(
                new SpiritualReconstructionLayer(livingRenderer));
        ((LivingEntityRenderer) livingRenderer).addLayer(
                new BlackFlameDeathRenderer.Layer(livingRenderer));
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
