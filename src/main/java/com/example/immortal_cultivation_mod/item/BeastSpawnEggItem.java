package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.BeastRealms;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** One egg per species; normal placement chooses a realm, sneak-use selects a fixed one. */
public final class BeastSpawnEggItem extends SpawnEggItem {
    public BeastSpawnEggItem(EntityType<? extends Mob> type, int primary, int secondary, Properties properties) {
        super(type, primary, secondary, properties);
    }
    public static int selectedRealm(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(BeastRealms.NBT_KEY) ? tag.getInt(BeastRealms.NBT_KEY) : -1;
    }
    public static void selectRealm(ItemStack stack, int realm) {
        if (!(stack.getItem() instanceof BeastSpawnEggItem egg)) return;
        var tag = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY).copyTag();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(egg.getDefaultType()).toString());
        if (realm < 0) tag.remove(BeastRealms.NBT_KEY);
        else tag.putInt(BeastRealms.NBT_KEY, BeastRealms.clamp(egg.getDefaultType(), realm));
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
    }
    public void cycleRealm(ItemStack stack) {
        var levels = BeastRealms.variants(getDefaultType());
        int next = levels.indexOf(selectedRealm(stack)) + 1;
        selectRealm(stack, next == levels.size() ? -1 : levels.get(next));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) cycleRealm(context.getItemInHand());
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return super.use(level, player, hand);
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) cycleRealm(stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public Component getName(ItemStack stack) {
        int selected = selectedRealm(stack);
        return selected < 0 ? super.getName(stack) : BeastRealms.prefix(selected, super.getName(stack));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component realms = Component.empty();
        for (int realm : BeastRealms.variants(getDefaultType())) {
            if (!realms.getString().isEmpty()) realms = realms.copy().append(" / ");
            realms = realms.copy().append(BeastRealms.prefix(realm, Component.empty()));
        }
        tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.beast_egg_realms", realms));
        if (selectedRealm(stack) < 0) tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.beast_egg_random"));
    }
}
