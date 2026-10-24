package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.spell.SpellProjectileCollision;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ProjectileUtil.class)
public abstract class SpellProjectileCollisionMixin {
    @ModifyVariable(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float immortalCultivation$widenSpellContact(float original, Level level,
            Entity projectile, Vec3 start, Vec3 end, AABB bounds, Predicate<Entity> filter, float margin) {
        return SpellProjectileCollision.margin(projectile, original);
    }
}
