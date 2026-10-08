package com.example.immortal_cultivation_mod.flame;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Forged seeds awaken a capturable essence instead of placing an ordinary disposable fire. */
public final class FlameSeedItem extends Item {
    private final FlameKind kind;
    public FlameSeedItem(Properties properties, FlameKind kind) { super(properties); this.kind = kind; }
    public FlameKind kind() { return kind; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (player == null || player.isSpectator() || !player.getAbilities().mayBuild
                || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!server.getEntitiesOfClass(SpiritFlameEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(.4)).isEmpty()) return InteractionResult.FAIL;
        if (SpiritFlameEntity.spawn(server, Vec3.atBottomCenterOf(pos).add(0, .15, 0), kind) == null) return InteractionResult.FAIL;
        if (!player.isCreative()) context.getItemInHand().shrink(1);
        server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .7F, .8F);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.flame_seed", kind.displayName()).withStyle(ChatFormatting.GOLD));
    }
}
