package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.block.ScaledBlockCollisionSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.function.BiFunction;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin<T> {
    @Shadow @Final private AABB box;
    @Shadow @Final private CollisionContext context;
    @Shadow @Final private BlockPos.MutableBlockPos pos;
    @Shadow @Final private CollisionGetter collisionGetter;
    @Shadow @Final private boolean onlySuffocatingBlocks;
    @Shadow @Final private BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider;

    @Unique
    private Iterator<ScaledBlockCollisionSupport.Collision>
            immortalCultivation$scaledCollisions;

    @Inject(
            method = "computeNext",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockCollisions;endOfData()Ljava/lang/Object;"),
            cancellable = true)
    private void immortalCultivation$includeDistantScaledBlockParts(
            CallbackInfoReturnable<T> cir) {
        if (immortalCultivation$scaledCollisions == null) {
            immortalCultivation$scaledCollisions =
                    ScaledBlockCollisionSupport.findMissingCollisions(
                            collisionGetter, box, context, onlySuffocatingBlocks).iterator();
        }
        if (!immortalCultivation$scaledCollisions.hasNext()) {
            return;
        }

        ScaledBlockCollisionSupport.Collision collision =
                immortalCultivation$scaledCollisions.next();
        pos.set(collision.anchor());
        cir.setReturnValue(resultProvider.apply(pos, collision.shape()));
    }
}
