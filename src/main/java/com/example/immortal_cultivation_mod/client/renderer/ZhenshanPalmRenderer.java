package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.ZhenshanPalmModel;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmShape;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZhenshanPalmRenderer extends GeoEntityRenderer<ZhenshanPalmEntity> {
    public ZhenshanPalmRenderer(EntityRendererProvider.Context context) {
        super(context, new ZhenshanPalmModel());
        withScale(ZhenshanPalmShape.MODEL_SCALE);
        shadowRadius = 0;
    }

    @Override
    protected void applyRotations(ZhenshanPalmEntity palm, PoseStack poses, float age,
                                  float yaw, float partialTick, float nativeScale) {
        // This is a projectile: GeckoLib's living-body yaw is always zero for it.
        // Use the synchronized launch/reflection vector, not caster or camera rotation.
        poses.mulPose(ZhenshanPalmShape.rotation(palm.forward()));
        float scale = palm.visualScale();
        poses.scale(scale, scale, scale);
    }
}
