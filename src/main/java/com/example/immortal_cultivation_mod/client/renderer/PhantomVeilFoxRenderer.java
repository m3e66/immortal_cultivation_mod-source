package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.PhantomVeilFoxModel;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class PhantomVeilFoxRenderer extends BeastGeoRenderer<PhantomVeilFoxEntity> {
    public PhantomVeilFoxRenderer(EntityRendererProvider.Context context) {
        this(context, 2.0F);
    }

    public PhantomVeilFoxRenderer(EntityRendererProvider.Context context, float scale) {
        super(context, new PhantomVeilFoxModel());
        withScale(scale);
        shadowRadius = 0.7F * scale;
    }

    @Override
    protected float getDeathMaxRotation(PhantomVeilFoxEntity animatable) {
        // The death clip already rolls and settles the complete rig on the ground.
        return animatable.deathTime > 0 ? 0.0F : super.getDeathMaxRotation(animatable);
    }

    @Override
    public RenderType getRenderType(PhantomVeilFoxEntity animatable,
                                    ResourceLocation texture,
                                    MultiBufferSource bufferSource,
                                    float partialTick) {
        // Binary pixel cutouts retain solid depth at intersecting fur surfaces.
        return RenderType.entityCutoutNoCull(texture);
    }
}
