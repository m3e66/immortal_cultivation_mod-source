package com.example.immortal_cultivation_mod.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Inventory and dropped-item sprites; held models retain their original UV atlases. */
public final class WeaponInventorySprite {
    private WeaponInventorySprite() {}
    public static boolean usesSprite(ItemDisplayContext context) {
        return context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND;
    }
    public static void render(String name, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var texture = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/" + name + "_inventory.png");
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        var matrix = pose.last();
        out.addVertex(matrix.pose(), 0, 0, .5F).setColor(-1).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(matrix, 0, 0, 1);
        out.addVertex(matrix.pose(), 1, 0, .5F).setColor(-1).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(matrix, 0, 0, 1);
        out.addVertex(matrix.pose(), 1, 1, .5F).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(matrix, 0, 0, 1);
        out.addVertex(matrix.pose(), 0, 1, .5F).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(matrix, 0, 0, 1);
    }
}
