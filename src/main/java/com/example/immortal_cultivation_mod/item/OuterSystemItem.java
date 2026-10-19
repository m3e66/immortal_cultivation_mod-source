package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.system.OuterSystemType;
import com.example.immortal_cultivation_mod.system.OuterSystemData;
import com.example.immortal_cultivation_mod.system.OuterSystemManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class OuterSystemItem extends Item implements ICurioItem {
    public static final String SLOT = "system";
    private static final String TYPE_TAG = "OuterSystemType";

    public OuterSystemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (OuterSystemData.equipped(player).isPresent()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player instanceof ServerPlayer serverPlayer
                && OuterSystemManager.bindFromItem(serverPlayer, stack)) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        return InteractionResultHolder.pass(stack);
    }

    public static ItemStack create(OuterSystemType type) {
        ItemStack stack = new ItemStack(ModItems.SYSTEM.get());
        setType(stack, type);
        return stack;
    }

    public static OuterSystemType getType(ItemStack stack) {
        CompoundTag custom = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return OuterSystemType.byId(custom.getString(TYPE_TAG));
    }

    public static void setType(ItemStack stack, OuterSystemType type) {
        CompoundTag custom = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        custom.putString(TYPE_TAG, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag custom = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!custom.contains(TYPE_TAG)) {
            return super.getName(stack);
        }
        return Component.translatable(
                "item.immortal_cultivation_mod.system.bound",
                Component.translatable(getType(stack).translationKey()));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return SLOT.equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player
                && player.getAbilities().instabuild;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // The System is soul-bound, not a physical item that can be tossed
        // with Q or dragged out of an inventory screen.
        return false;
    }

    @Override
    public ICurio.DropRule getDropRule(
            SlotContext slotContext,
            DamageSource source,
            boolean recentlyHit,
            ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
