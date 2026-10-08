package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.flame.FlameSeedItem;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** The same animated core, tongues and seals used by the placed fire block. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FlameSeedItemRenderer extends BlockEntityWithoutLevelRenderer {
    public FlameSeedItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private FlameSeedItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new FlameSeedItemRenderer();
                return renderer;
            }
        }, ModItems.CRIMSON_FLAME_SEED.get(), ModItems.EARTH_FLAME_SEED.get(),
                ModItems.WOOD_FLAME_HEART.get(), ModItems.SOLAR_FLAME_SEED.get(),
                ModItems.NETHER_FLAME_SEED.get(), ModItems.THUNDER_FLAME_SEED.get(),
                ModItems.DRAGON_FLAME_SEED.get(), ModItems.HEAVENLY_FLAME_SEED.get());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof FlameSeedItem seed)) return;
        renderSeed(seed.kind(), animationTime(), poses, buffers, .5, .43, .5, .54F);
    }

    static float animationTime() {
        var minecraft = Minecraft.getInstance();
        return minecraft.level == null ? (System.nanoTime() / 50_000_000L) % 240
                : minecraft.level.getGameTime() % 240 + minecraft.getTimer().getGameTimeDeltaPartialTick(true);
    }

    /** Both loose seeds and contained flames use this exact model and animation path. */
    static void renderSeed(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers,
            double x, double y, double z, float scale) {
        renderSeed(kind, time, poses, buffers, x, y, z, scale, false);
    }

    static void renderContainedSeed(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers,
            double x, double y, double z, float scale) {
        renderSeed(kind, time, poses, buffers, x, y, z, scale, true);
    }

    private static void renderSeed(FlameKind kind, float time, PoseStack poses, MultiBufferSource buffers,
            double x, double y, double z, float scale, boolean contained) {
        poses.pushPose();
        // Core, ground ring, bob and the longest lightning/heavenly pulse all stay in
        // the inventory square. No world particles or heat-haze sources are registered.
        poses.translate(x, y, z);
        poses.scale(scale, scale, scale);
        poses.mulPose(Axis.YP.rotationDegrees(25));
        poses.translate(0, FlameVfxRenderer.bob(kind, time) * .35F, 0);
        if (contained) FlameVfxRenderer.renderContainedItem(kind, time, poses, buffers);
        else FlameVfxRenderer.renderItem(kind, time, poses, buffers);
        poses.popPose();
    }
}
