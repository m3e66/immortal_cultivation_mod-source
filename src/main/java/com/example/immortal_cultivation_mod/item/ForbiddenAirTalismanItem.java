package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.TalismanWardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ancient 禁空符: attaches to aimed ground and creates a physical pressure field. */
public final class ForbiddenAirTalismanItem extends TalismanItem {
    private static final double TARGET_RANGE = 32.0D;

    public ForbiddenAirTalismanItem(Properties properties) {
        super(properties, Tier.ANCIENT);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(TARGET_RANGE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || blockHit.getDirection() != Direction.UP) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            Vec3 target = blockHit.getLocation().add(
                    Vec3.atLowerCornerOf(blockHit.getDirection().getNormal())
                            .scale(0.035D));
            TalismanWardEntity.spawnForbiddenAir(serverLevel, player, target);
            player.getCooldowns().addCooldown(this, 20);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
