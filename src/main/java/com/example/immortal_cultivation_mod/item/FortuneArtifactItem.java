package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.fate.LuckSystem;
import com.example.immortal_cultivation_mod.fate.FateTechniques;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class FortuneArtifactItem extends Item implements ICurioItem {
    public enum Kind { MIRROR, PRECISE_MIRROR, COIN }
    private final Kind kind;
    public FortuneArtifactItem(Properties properties, Kind kind) { super(properties); this.kind = kind; }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return kind == Kind.COIN && "charm".equals(context.identifier());
    }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        // Return mirrors equipped by an older version without deleting or duplicating the item.
        if (kind == Kind.COIN || !(context.entity() instanceof ServerPlayer player)) return;
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            var slot = inventory.getCurios().get(context.identifier());
            if (slot == null) return;
            var stacks = context.cosmetic() ? slot.getCosmeticStacks() : slot.getStacks();
            if (context.index() < 0 || context.index() >= stacks.getSlots()) return;
            ItemStack equipped = stacks.getStackInSlot(context.index());
            if (equipped.getItem() != this) return;
            ItemStack returned = equipped.copy();
            stacks.setStackInSlot(context.index(), ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(returned);
        });
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == Kind.COIN) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) {
            var target = FateTechniques.lookedAt(serverPlayer, 16);
            LuckSystem.inspect(serverPlayer, target == null ? player : target, kind == Kind.PRECISE_MIRROR);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("luck.immortal_cultivation_mod.artifact." + kind.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
