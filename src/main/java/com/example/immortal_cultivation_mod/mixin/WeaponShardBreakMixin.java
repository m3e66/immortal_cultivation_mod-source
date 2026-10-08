package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.item.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class WeaponShardBreakMixin {
    @WrapMethod(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V")
    private void immortalCultivation$shatter(int amount,ServerLevel level,LivingEntity holder,Consumer<Item> callback,Operation<Void> original){
        var stack=(ItemStack)(Object)this;
        var before=WeaponRestrictionSystem.isWeapon(stack)&&WeaponRestrictionSystem.tier(stack)>0?stack.copy():ItemStack.EMPTY;
        original.call(amount,level,holder,callback);
        if(!before.isEmpty()&&stack.getCount()<before.getCount())WeaponShardSystem.dropBroken(before,level,holder);
    }
}
