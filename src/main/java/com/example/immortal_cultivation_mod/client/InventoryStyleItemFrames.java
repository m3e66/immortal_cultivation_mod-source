package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;

/** Frames use the same model, sprite selection and display transform as inventory slots. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class InventoryStyleItemFrames {
    private InventoryStyleItemFrames() { }

    private static final class Buffers {
        // Isolate GUI renderers: GeckoLib may flush its source and change diffuse lighting.
        // Reuse one buffer rather than allocating native memory per visible frame per tick.
        private static final MultiBufferSource.BufferSource ITEMS =
                MultiBufferSource.immediate(new ByteBufferBuilder(65536));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderItemInFrameEvent event) {
        var stack = event.getItemStack();
        var frame = event.getItemFrameEntity();
        if (stack.isEmpty()) return;

        var minecraft = Minecraft.getInstance();
        var renderer = minecraft.getItemRenderer();
        var model = renderer.getModel(stack, frame.level(), minecraft.player, frame.getId());
        var pose = event.getPoseStack();
        var buffers = Buffers.ITEMS;
        int light = frame.getType() == EntityType.GLOW_ITEM_FRAME ? LightTexture.FULL_BRIGHT : event.getPackedLight();
        pose.pushPose();
        try {
            // The frame has already applied its wall/floor/ceiling orientation and click rotation.
            // Turn the inventory's front toward the viewer, retaining normal frame item size.
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(.5F, .5F, .5F);
            if (model.usesBlockLight()) Lighting.setupFor3DItems();
            else Lighting.setupForFlatItems();
            renderer.render(stack, ItemDisplayContext.GUI, false, pose, buffers,
                    light, OverlayTexture.NO_OVERLAY, model);
            buffers.endBatch();
            event.setCanceled(true);
        } finally {
            pose.popPose();
            // Restore the world lighting before any queued terrain or entity batches are drawn.
            if (minecraft.level != null && minecraft.level.effects().constantAmbientLight()) Lighting.setupNetherLevel();
            else Lighting.setupLevel();
        }
    }
}
