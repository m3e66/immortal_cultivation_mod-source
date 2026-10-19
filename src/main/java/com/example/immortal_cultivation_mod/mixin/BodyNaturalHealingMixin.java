package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.attachment.BodyTypeEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class BodyNaturalHealingMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void immortalCultivation$naturalBodyHealing(Player player, float amount) {
        player.heal(BodyTypeEffects.naturalHealing(player, amount));
    }
}
