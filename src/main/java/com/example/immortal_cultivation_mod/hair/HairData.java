package com.example.immortal_cultivation_mod.hair;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import java.util.*;

public final class HairData {
    public static final int DEFAULT_COLOR = 0x241713;
    public static IDynamicStackHandler slot(LivingEntity player, String id) {
        return CuriosApi.getCuriosInventory(player).flatMap(h -> h.getStacksHandler(id))
                .map(h -> h.getStacks()).filter(s -> s.getSlots() > 0).orElse(null);
    }
    public static ItemStack equipped(LivingEntity player, String id) {
        var slot = slot(player, id); return slot == null ? ItemStack.EMPTY : slot.getStackInSlot(0);
    }
    public static boolean visible(LivingEntity player, String id) {
        return CuriosApi.getCuriosInventory(player).flatMap(h -> h.getStacksHandler(id))
                .map(h -> !h.getRenders().isEmpty() && h.getRenders().get(0)).orElse(false);
    }
    public static int style(ItemStack item) { return item.getItem() instanceof HairItem h && !h.accessory ? h.style : -1; }
    public static int accessory(ItemStack item) { return item.getItem() instanceof HairItem h && h.accessory ? h.style : -1; }
    public static int color(ItemStack item) {
        CompoundTag tag = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("HairColor") ? tag.getInt("HairColor") & 0xFFFFFF : DEFAULT_COLOR;
    }
    /** 0: full hair, 1: compact hair, 2: hidden with a helmet. */
    public static int helmet(ItemStack item) {
        CompoundTag tag = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("HairHelmetMode") ? Math.clamp(tag.getInt("HairHelmetMode"), 0, 2) : 1;
    }
    public static ItemStack create(int style, int color, int helmet) {
        ItemStack stack = new ItemStack(HairContent.HAIR.get(style).get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("HairStyle", HairStyle.values()[style].id()); tag.putInt("HairColor", color & 0xFFFFFF);
            tag.putInt("HairHelmetMode", Math.clamp(helmet, 0, 2));
        });
        return stack;
    }
    private static CompoundTag preferences(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
    public static int[] colors(Player player, boolean saved) {
        return preferences(player).getIntArray(saved ? "HairSavedColors" : "HairRecentColors");
    }
    public static void remember(Player player, int color, boolean saved) {
        int[] prior = colors(player, saved);
        var values = new ArrayList<Integer>(); values.add(color & 0xFFFFFF);
        for (int v : prior) if (v != color && values.size() < 8) values.add(v & 0xFFFFFF);
        preferences(player).putIntArray(saved ? "HairSavedColors" : "HairRecentColors", values.stream().mapToInt(i -> i).toArray());
    }
}
