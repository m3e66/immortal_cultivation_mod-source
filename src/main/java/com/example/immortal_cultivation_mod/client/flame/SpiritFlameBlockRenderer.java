package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.flame.SpiritFlameBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Placed forge fires use the same volumetric energy language as capturable seeds. */
public final class SpiritFlameBlockRenderer implements BlockEntityRenderer<SpiritFlameBlockEntity> {
    public SpiritFlameBlockRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(SpiritFlameBlockEntity flame, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay) {
        if (flame.getLevel() == null || ShaderPackCompatibility.isRenderingShadowPass()) return;
        Vec3 center = Vec3.atLowerCornerOf(flame.getBlockPos()).add(.5, .6, .5);
        double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(center);
        if (distance > 48 * 48) return;
        float time = flame.getLevel().getGameTime() % 240 + partialTick
                + Math.floorMod(flame.getBlockPos().asLong(), 240);
        float bob = FlameVfxRenderer.bob(flame.kind(), time) * .35F;
        poses.pushPose();
        poses.translate(.5, .6 + bob, .5);
        if (flame.isQiFire()) FlameVfxRenderer.renderQiFire(time, poses, buffers, distance);
        else FlameVfxRenderer.render(flame.kind(), time, poses, buffers, distance, true);
        poses.popPose();
        FlameClientEffects.observeBlock(flame.getBlockPos(), flame.kind());
        FlameHeatHaze.observe(flame.kind(), center.add(0, bob, 0));
    }

    @Override public int getViewDistance() { return 48; }

    @Override public AABB getRenderBoundingBox(SpiritFlameBlockEntity flame) {
        return new AABB(flame.getBlockPos()).inflate(.55);
    }
}
