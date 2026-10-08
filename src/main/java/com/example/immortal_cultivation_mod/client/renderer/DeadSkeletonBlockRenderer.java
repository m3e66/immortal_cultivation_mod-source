package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.DeadSkeletonBlockEntity;
import com.example.immortal_cultivation_mod.client.model.DeadSkeletonModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class DeadSkeletonBlockRenderer extends GeoBlockRenderer<DeadSkeletonBlockEntity> {
   private static final float MODEL_SCALE = 0.9F;

   public DeadSkeletonBlockRenderer(BlockEntityRendererProvider.Context context) {
      super(new DeadSkeletonModel());
      this.withScale(0.9F);
   }

   public AABB getRenderBoundingBox(DeadSkeletonBlockEntity skeleton) {
      return (new AABB(skeleton.getBlockPos())).inflate((double)1.5F, (double)0.75F, (double)1.5F);
   }
}
