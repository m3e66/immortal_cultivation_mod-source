package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.flame.FlameVesselItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Hollow glass bottle containing the stored flame's existing animated seed model. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FlameVesselItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation BOTTLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "item/flame_vessel_bottle"));
    private static final float CONTAINED_SEED_SCALE = .50F;

    public FlameVesselItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @SubscribeEvent
    public static void registerModel(ModelEvent.RegisterAdditional event) {
        event.register(BOTTLE);
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private FlameVesselItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new FlameVesselItemRenderer();
                return renderer;
            }
        }, ModItems.FLAME_VESSEL.get());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof FlameVesselItem)) return;
        var minecraft = Minecraft.getInstance();
        // Resolve from the current model manager each time, so F3+T cannot retain a stale atlas/model.
        var bottle = minecraft.getModelManager().getModel(BOTTLE);
        var glass = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
        minecraft.getItemRenderer().renderModelLists(bottle, stack, light, overlay, poses, buffers.getBuffer(glass));
        // The opaque glass edges must write depth before the seed's translucent tongues.
        if (buffers instanceof MultiBufferSource.BufferSource immediate) immediate.endBatch(glass);
        var kind = FlameVesselItem.flame(stack);
        if (kind != null) {
            FlameSeedItemRenderer.renderContainedSeed(kind, FlameSeedItemRenderer.animationTime(), poses, buffers,
                    .5, 9.3 / 16.0, .5, CONTAINED_SEED_SCALE);
        }
    }
}
