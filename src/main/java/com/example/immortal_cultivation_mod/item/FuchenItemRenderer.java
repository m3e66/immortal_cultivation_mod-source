package com.example.immortal_cultivation_mod.item;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class FuchenItemRenderer extends GeoItemRenderer<FuchenItem> {
    @Override public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        if (WeaponInventorySprite.usesSprite(context)) {
            WeaponInventorySprite.render("fuchen", pose, buffers, light, overlay);
            return;
        }
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }
    public FuchenItemRenderer() {
        super(new FuchenItemModel());
    }
}
