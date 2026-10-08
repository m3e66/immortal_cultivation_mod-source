package com.example.immortal_cultivation_mod.item;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HeavyIronBracerItemRenderer extends GeoItemRenderer<HeavyIronBracerItem> {
    private final HeavyIronBracerDebugItemModel debugModel = new HeavyIronBracerDebugItemModel();
    private final YaoyuanFistBoneItemModel yaoyuanFistBoneModel = new YaoyuanFistBoneItemModel();

    public HeavyIronBracerItemRenderer() {
        super(new HeavyIronBracerItemModel());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        if (WeaponInventorySprite.usesSprite(context)) {
            String icon = stack.is(ModItems.YAOYUAN_FIST_BONE.get()) ? "yaoyuan_fist_bone" : "heavy_iron_bracer";
            WeaponInventorySprite.render(icon, pose, buffers, light, overlay);
            return;
        }
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }

    @Override
    public GeoModel<HeavyIronBracerItem> getGeoModel() {
        if (getCurrentItemStack().is(ModItems.YAOYUAN_FIST_BONE.get())) {
            return yaoyuanFistBoneModel;
        }
        return HeavyIronBracerItem.isDebugStack(getCurrentItemStack()) ? debugModel : super.getGeoModel();
    }
}
